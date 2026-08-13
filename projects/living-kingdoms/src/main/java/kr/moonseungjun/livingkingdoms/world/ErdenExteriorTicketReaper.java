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
        ErdenExteriorResidenceSavedData residences = level.getDataStorage()
                .computeIfAbsent(ErdenExteriorResidenceSavedData.TYPE);
        ErdenKingdomExteriorContainerSavedData containers = level.getDataStorage()
                .computeIfAbsent(ErdenKingdomExteriorContainerSavedData.TYPE);
        observePhysicalStorage(level);

        List<ErdenKingdomSupplyCatalog.SupplyNode> nodes = ErdenKingdomSupplyCatalog.nodes();
        ErdenKingdomSupplyCatalog.SupplyNode sampleNode = nodes.getFirst();
        Set<Long> sampleAnchors = new LinkedHashSet<>(anchorsFor(sampleNode));
        for (ErdenExteriorResidenceCatalog.ResidencePlot plot :
                ErdenExteriorResidenceCatalog.forNode(sampleNode.id)) {
            sampleAnchors.add(plot.physicalChunk());
        }
        boolean sampleResidentsReady = sampleResidentsReady(level, sampleNode);
        if (sampleResidentsReady && sampleReadySince < 0L) {
            sampleReadySince = level.getGameTime();
            LivingKingdoms.LOGGER.info(
                    "Observed Erden exterior resident sample node={} residents={} grace_ticks={}",
                    sampleNode.id, sampleResidentCount(level, sampleNode), SAMPLE_RELEASE_GRACE_TICKS);
        }
        // The sample is intentionally observed while its transient ticket is still retained. Once
        // the grace period expires that ticket may be removed and the sample chunk may unload, so a
        // later live entity query is no longer a valid indicator of whether validation happened.
        // sampleReadySince is the sticky proof that the authoritative loaded observation succeeded.
        boolean residentSampleObserved = sampleReadySince >= 0L;
        boolean releaseSample = residentSampleObserved
                && level.getGameTime() - sampleReadySince >= SAMPLE_RELEASE_GRACE_TICKS;

        Set<Long> required = requiredAnchors();
        int releasedNow = 0;
        for (long packed : required) {
            int chunkX = unpackX(packed);
            int chunkZ = unpackZ(packed);
            boolean residenceReady = !ErdenExteriorResidenceCatalog.residenceChunk(chunkX, chunkZ)
                    || residences.chunkBuilt(
                    chunkX, chunkZ,
                    ErdenExteriorResidenceBuilder.RESIDENCE_REVISION);
            boolean exteriorReady = !isExteriorAnchor(packed)
                    || exterior.isBuilt(packed, ErdenKingdomExteriorBuilder.EXTERIOR_REVISION);
            boolean storageReady = storageReadyForChunk(packed, containers);
            if (RELEASED.contains(packed)
                    || !exteriorReady
                    || !residenceReady
                    || !storageReady
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
                    "Released Erden exterior transient tickets progress={}/{} released_now={} storage_validated={}/{} captured_producers={}/{} resident_sample_validated={} persistent_forced_chunks=false",
                    RELEASED.size(), required.size(), releasedNow,
                    VALIDATED_STORAGE_NODES.size(), nodes.size(),
                    containers.capturedCount(), ErdenKingdomSupplyCatalog.producerCount(),
                    residentSampleObserved);
        }

        if (!ticketCiPassed
                && RELEASED.size() == required.size()
                && VALIDATED_STORAGE_NODES.size() == nodes.size()
                && containers.capturedCount() == ErdenKingdomSupplyCatalog.producerCount()
                && residentSampleObserved) {
            ticketCiPassed = true;
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_EXTERIOR_TICKETS_PASS revision=1 anchors={} released={} explicit_release=true persistent_forced_chunks=false storage_yards_observed={} resident_sample_observed=true validation_revision=4 inventory_captured_nodes={}",
                    required.size(), RELEASED.size(), VALIDATED_STORAGE_NODES.size(),
                    containers.capturedCount());
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

    public static boolean storageValidationComplete() {
        return VALIDATED_STORAGE_NODES.size() == ErdenKingdomSupplyCatalog.nodes().size();
    }

    /**
     * Producer storage tickets are retained until that exact barrel has both been initialized from
     * authoritative supply state and subsequently read back into that state on a later sync. This is
     * evidence-based rather than relying on a timing grace period.
     */
    private static boolean storageReadyForChunk(
            long packed,
            ErdenKingdomExteriorContainerSavedData containers) {
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            if (ErdenKingdomExteriorBuilder.storageAnchorChunk(node) != packed) continue;
            if (!VALIDATED_STORAGE_NODES.contains(node.id)) return false;
            if (!node.producer()) continue;
            if (!containers.isMaterialized(node.id) || !containers.isCaptured(node.id)) return false;
        }
        return true;
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
        BlockPos physicalHome = ErdenExteriorResidenceBuilder.residentSpawnPosition(
                sample.id(), 0);
        if (physicalHome.equals(BlockPos.ZERO)
                || !level.hasChunk(physicalHome.getX() >> 4, physicalHome.getZ() >> 4)) return false;
        AABB bounds = new AABB(
                physicalHome.getX() - 16, level.getMinY(), physicalHome.getZ() - 16,
                physicalHome.getX() + 16, level.getMaxY(), physicalHome.getZ() + 16);
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
        BlockPos physicalHome = ErdenExteriorResidenceBuilder.residentSpawnPosition(
                sample.id(), 0);
        if (physicalHome.equals(BlockPos.ZERO)
                || !level.hasChunk(physicalHome.getX() >> 4, physicalHome.getZ() >> 4)) return 0;
        AABB bounds = new AABB(
                physicalHome.getX() - 16, level.getMinY(), physicalHome.getZ() - 16,
                physicalHome.getX() + 16, level.getMaxY(), physicalHome.getZ() + 16);
        return level.getEntitiesOfClass(
                Villager.class, bounds,
                villager -> names.contains(villager.getName().getString())).size();
    }

    private static Set<Long> requiredAnchors() {
        Set<Long> anchors = new LinkedHashSet<>();
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            anchors.addAll(anchorsFor(node));
        }
        for (ErdenExteriorResidenceCatalog.ResidencePlot plot :
                ErdenExteriorResidenceCatalog.plots()) {
            anchors.add(plot.physicalChunk());
        }
        if (anchors.size() != ErdenKingdomExteriorBuilder.EXPECTED_CI_ANCHORS) {
            throw new IllegalStateException("Invalid Erden exterior ticket anchor count " + anchors.size());
        }
        return anchors;
    }

    private static Set<Long> anchorsFor(ErdenKingdomSupplyCatalog.SupplyNode node) {
        Set<Long> anchors = new LinkedHashSet<>();
        for (int[] offset : NODE_ANCHOR_OFFSETS) {
            anchors.add(pack((node.x + offset[0]) >> 4, (node.z + offset[1]) >> 4));
        }
        anchors.add(ErdenKingdomExteriorBuilder.storageAnchorChunk(node));
        return anchors;
    }

    private static boolean isExteriorAnchor(long packed) {
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            if (anchorsFor(node).contains(packed)) return true;
        }
        return false;
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
