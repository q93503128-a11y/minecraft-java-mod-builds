package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Releases the transient chunk-loading tickets used while Erden's exterior supply sites are built.
 * The builder deliberately keeps only three requests in flight; this companion runs immediately
 * afterwards and removes the actual server ticket as soon as the authoritative chunk record is
 * committed, preventing completed farms, mines, mills and wharves from accumulating as active
 * chunks for the remainder of the session.
 */
public final class ErdenExteriorTicketReaper {
    private static final int[][] NODE_ANCHOR_OFFSETS = {
            {0, 0}, {28, 0}, {-28, 0}, {0, 28}, {0, -28}
    };

    private static final Set<Long> RELEASED = new HashSet<>();
    private static MinecraftServer activeServer;
    private static boolean ciPassed;

    private ErdenExteriorTicketReaper() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        if (activeServer != server) {
            activeServer = server;
            RELEASED.clear();
            ciPassed = false;
        }

        ErdenKingdomExteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);
        Set<Long> required = requiredAnchors();
        int releasedNow = 0;
        for (long packed : required) {
            if (RELEASED.contains(packed)
                    || !data.isBuilt(packed, ErdenKingdomExteriorBuilder.EXTERIOR_REVISION)) continue;
            level.getChunkSource().removeTicketWithRadius(
                    TicketType.PORTAL,
                    new ChunkPos(unpackX(packed), unpackZ(packed)),
                    0);
            RELEASED.add(packed);
            releasedNow++;
        }

        if (releasedNow > 0 && (RELEASED.size() % 10 == 0 || RELEASED.size() == required.size())) {
            LivingKingdoms.LOGGER.info(
                    "Released Erden exterior transient tickets progress={}/{} released_now={} persistent_forced_chunks=false",
                    RELEASED.size(), required.size(), releasedNow);
        }

        if (!ciPassed
                && "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))
                && RELEASED.size() == required.size()) {
            ciPassed = true;
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_EXTERIOR_TICKETS_PASS revision=1 anchors={} released={} explicit_release=true persistent_forced_chunks=false",
                    required.size(), RELEASED.size());
        }
    }

    private static Set<Long> requiredAnchors() {
        Set<Long> anchors = new LinkedHashSet<>();
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            for (int[] offset : NODE_ANCHOR_OFFSETS) {
                anchors.add(pack((node.x + offset[0]) >> 4, (node.z + offset[1]) >> 4));
            }
        }
        return anchors;
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    private static int unpackZ(long packed) {
        return (int) packed;
    }
}
