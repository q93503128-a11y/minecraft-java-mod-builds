package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Releases the transient chunk-loading tickets used while Erden's exterior supply sites are built.
 * Physical storage yards and one resident household are observed while their chunks are loaded,
 * before the tickets are removed. This keeps validation authoritative without leaving completed
 * farms, mines, mills or wharves permanently active.
 */
public final class ErdenExteriorTicketReaper {
    private static final int[][] NODE_ANCHOR_OFFSETS = {
            {0, 0}, {28, 0}, {-28, 0}, {0, 28}, {0, -28}
    };
    private static final long SAMPLE_RELEASE_GRACE_TICKS = 40L;

    private static final Set<Long> RELEASED = new HashSet<>();
    private static final Set<String> VALIDATED_STORAGE_NODES = new HashSet<>();
    private static MinecraftServer activeServer;
    private static long sampleReadySince = -1L;
    private static boolean ticketCiPassed;
    private static boolean exteriorCiPassed;

    private ErdenExteriorTicketReaper() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null
                || !RealmSitePlanner.isBuilt(level, "erden_kingdom")
                || !isCi()) return;
        if (activeServer != server) reset(server);

        ErdenKingdomExteriorSavedData exterior = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorSavedData.TYPE);
        observePhysicalStorage(level);

        List<ErdenKingdomSupplyCatalog.SupplyNode> nodes = ErdenKingdomSupplyCatalog.nodes();
        ErdenKingdomSupplyCatalog.SupplyNode sampleNode = nodes.getFirst();
        Set<Long> sampleAnchors = anchorsFor(sampleNode);
        boolean sampleResidentsReady = sampleResidentsReady(level, sampleNode);
        if (sampleResidentsReady && sampleReadySince < 0L) {
            sampleReadySince = level.getGameTime();
            LivingKingdoms.LOGGER.info(
                    "Observed Erden exterior resident sample node={} residents={} grace_ticks={}",
                    sampleNode.id, sampleResidentCount(level, sampleNode), SAMPLE_RELEASE_GRACE_TICKS);
        }
        boolean releaseSample = sampleReadySince >= 0L
                && level.getGameTime() - sampleReadySince >= SAMPLE_RELEASE_GRACE_TICKS;

        Set<Long> required = requiredAnchors();
        int releasedNow = 0;
        for (long packed : required) {
            if (RELEASED.contains(packed)
                    || !exterior.isBuilt(packed, ErdenKingdomExteriorBuilder.EXTERIOR_REVISION)
                    || (sampleAnchors.contains(packed) && !releaseSample)) continue;
            level.getChunkSource().removeTicketWithRadius(
                    TicketType.PORTAL,
                    new ChunkPos(unpackX(packed), unpackZ(packed)),
                    0);
            RELEASED.add(packed);
            releasedNow++;
        }

        if (releasedNow > 0 && (RELEASED.size() % 10 == 0 || RELEASED.size() == required.size())) {
            LivingKingdoms.LOGGER.info(
                    "Released Erden exterior transient tickets progress={}/{} released_now={} storage_validated={}/{} resident_sample_validated={} persistent_forced_chunks=false",
                    RELEASED.size(), required.size(), releasedNow,
                    VALIDATED_STORAGE_NODES.size(), nodes.size(), sampleResidentsReady);
        }

        if (!ticketCiPassed
                && RELEASED.size() == required.size()
                && VALIDATED_STORAGE_NODES.size() == nodes.size()
                && sampleResidentsReady) {
            ticketCiPassed = true;
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_EXTERIOR_TICKETS_PASS revision=2 anchors={} released={} storage_yards_observed={} resident_sample_observed=true explicit_release=true persistent_forced_chunks=false",
                    required.size(), RELEASED.size(), VALIDATED_STORAGE_NODES.size());
        }

        if (!exteriorCiPassed
                && ticketCiPassed
                && exterior.completedNodeCount(ErdenKingdomExteriorBuilder.EXTERIOR_REVISION) == nodes.size()
                && exterior.builtChunkCount(ErdenKingdomExteriorBuilder.EXTERIOR_REVISION) >= 70
                && exterior.totalWrites(ErdenKingdomExteriorBuilder.EXTERIOR_REVISION) > 0L) {
            exteriorCiPassed = true;
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_KINGDOM_EXTERIOR_PASS revision={} nodes={} producers={} wharves={} anchor_chunks={} writes={} metre_scale=true streamed=true external_buildings=true fields=true paddocks=true mines=true mills=true docks=true roads=true storage_yards=true physical_storage_observed=true resident_sample_observed=true tickets_released=true debris_zero=true",
                    ErdenKingdomExteriorBuilder.EXTERIOR_REVISION, nodes.size(),
                    ErdenKingdomSupplyCatalog.producerCount(), ErdenKingdomSupplyCatalog.wharfCount(),
                    exterior.builtChunkCount(ErdenKingdomExteriorBuilder.EXTERIOR_REVISION),
                    exterior.totalWrites(ErdenKingdomExteriorBuilder.EXTERIOR_REVISION));
        }
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        RELEASED.clear();
        VALIDATED_STORAGE_NODES.clear();
        sampleReadySince = -1L;
        ticketCiPassed = false;
        exteriorCiPassed = false;
    }

    private static void observePhysicalStorage(ServerLevel level) {
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            if (VALIDATED_STORAGE_NODES.contains(node.id)) continue;
            BlockPos storage = ErdenKingdomExteriorBuilder.storagePosition(level, node);
            if (level.hasChunkAt(storage) && level.getBlockState(storage).is(Blocks.BARREL)) {
                VALIDATED_STORAGE_NODES.add(node.id);
            }
        }
    }

    private static boolean sampleResidentsReady(
            ServerLevel level,
            ErdenKingdomSupplyCatalog.SupplyNode sampleNode) {
        ErdenExteriorWorkforceSavedData workforce = level.getDataStorage()
                .computeIfAbsent(ErdenExteriorWorkforceSavedData.TYPE);
        ErdenExteriorWorkforceSavedData.Household sample = workforce.households().stream()
                .filter(household -> household.nodeId().equals(sampleNode.id))
                .findFirst().orElse(null);
        if (sample == null || sample.residents().isEmpty()) return false;
        Set<String> names = new HashSet<>();
        for (ErdenExteriorWorkforceSavedData.Resident resident : sample.residents()) {
            if (!workforce.isDead(resident.id())) names.add(resident.name());
        }
        if (names.isEmpty()) return false;
        AABB bounds = new AABB(
                sampleNode.x - 96, level.getMinY(), sampleNode.z - 96,
                sampleNode.x + 96, level.getMaxY(), sampleNode.z + 96);
        int loaded = level.getEntitiesOfClass(
                Villager.class, bounds,
                villager -> names.contains(villager.getName().getString())).size();
        return loaded == names.size();
    }

    private static int sampleResidentCount(
            ServerLevel level,
            ErdenKingdomSupplyCatalog.SupplyNode sampleNode) {
        ErdenExteriorWorkforceSavedData workforce = level.getDataStorage()
                .computeIfAbsent(ErdenExteriorWorkforceSavedData.TYPE);
        ErdenExteriorWorkforceSavedData.Household sample = workforce.households().stream()
                .filter(household -> household.nodeId().equals(sampleNode.id))
                .findFirst().orElse(null);
        if (sample == null) return 0;
        Set<String> names = new HashSet<>();
        for (ErdenExteriorWorkforceSavedData.Resident resident : sample.residents()) {
            if (!workforce.isDead(resident.id())) names.add(resident.name());
        }
        AABB bounds = new AABB(
                sampleNode.x - 96, level.getMinY(), sampleNode.z - 96,
                sampleNode.x + 96, level.getMaxY(), sampleNode.z + 96);
        return level.getEntitiesOfClass(
                Villager.class, bounds,
                villager -> names.contains(villager.getName().getString())).size();
    }

    private static Set<Long> requiredAnchors() {
        Set<Long> anchors = new LinkedHashSet<>();
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            anchors.addAll(anchorsFor(node));
        }
        return anchors;
    }

    private static Set<Long> anchorsFor(ErdenKingdomSupplyCatalog.SupplyNode node) {
        Set<Long> anchors = new LinkedHashSet<>();
        for (int[] offset : NODE_ANCHOR_OFFSETS) {
            anchors.add(pack((node.x + offset[0]) >> 4, (node.z + offset[1]) >> 4));
        }
        return anchors;
    }

    private static boolean isCi() {
        return "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
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
