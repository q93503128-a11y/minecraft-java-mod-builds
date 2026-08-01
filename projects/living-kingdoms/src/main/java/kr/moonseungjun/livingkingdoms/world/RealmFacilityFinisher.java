package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Applies small support-sensitive finishing details once after a capital has finished building. */
public final class RealmFacilityFinisher {
    private static final Set<MinecraftServer> FINISHED_SERVERS =
            Collections.newSetFromMap(new WeakHashMap<>());

    private RealmFacilityFinisher() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        synchronized (FINISHED_SERVERS) {
            if (FINISHED_SERVERS.contains(server)) return;
        }
        ServerLevel realm = server.getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) return;
        RealmSiteLayoutSavedData.RealmSite erden = RealmSitePlanner.site(realm, "erden_kingdom");
        if (erden == null || !erden.built()) return;
        ensureCriticalFacilities(realm, erden);
        synchronized (FINISHED_SERVERS) {
            FINISHED_SERVERS.add(server);
        }
    }

    public static void ensureCriticalFacilities(ServerLevel realm,
                                                RealmSiteLayoutSavedData.RealmSite erden) {
        int y = Math.max(68, Math.min(112, erden.baseY()));
        BlockPos lantern = new BlockPos(erden.centerX() + 32, y + 3, erden.centerZ() - 67);

        // The original decorative lantern had no support and could pop into an item during neighbour updates.
        realm.setBlock(lantern.below(2), Blocks.SPRUCE_FENCE.defaultBlockState(), 3);
        realm.setBlock(lantern.below(), Blocks.SPRUCE_FENCE.defaultBlockState(), 3);
        realm.setBlock(lantern, Blocks.LANTERN.defaultBlockState(), 3);
    }
}
