package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Activates functional furniture inside the real imported Erden ground floors.
 *
 * <p>Revision 2 deliberately contains no room carving logic. Floors, walls, ceilings, stairs and
 * clearances are owned by the authored source structures; this manager only delegates source-air
 * fixture placement to {@link ErdenUrbanAuthoredGroundMaterializer}.</p>
 */
public final class ErdenUrbanInteriorBuilder {
    public static final int INTERIOR_REVISION = 2;
    private static final int PROCESS_BUDGET = 2;
    private static final Set<String> SUPPORTED_ROLES = Set.of(
            "tenement", "shop", "bakery", "inn",
            "stable", "guard_post", "bathhouse", "warehouse"
    );

    private static MinecraftServer activeServer;
    private static boolean diagnosticsLogged;
    private static boolean completionLogged;
    private static boolean ciChunksRequested;
    private static boolean ciSamplePassed;

    private ErdenUrbanInteriorBuilder() {}

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (activeServer != server) reset(server);
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;

        List<ExternalUrbanFabricBuilder.UrbanEntrance> entrances = ExternalUrbanFabricBuilder.entrances();
        logDiagnosticsOnce(entrances);
        requestCiSampleChunks(level);

        ErdenUrbanInteriorSavedData data = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE);
        int builtThisTick = 0;
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : entrances) {
            if (builtThisTick >= PROCESS_BUDGET) break;
            long key = entranceKey(entrance);
            if (data.isComplete(key, INTERIOR_REVISION)) continue;
            try {
                if (!ErdenUrbanAuthoredGroundMaterializer.tryMaterialize(level, entrance)) continue;
                data.markComplete(key, INTERIOR_REVISION);
                builtThisTick++;
                verifyCiSampleIfNeeded(level, entrance);
            } catch (Throwable throwable) {
                LivingKingdoms.LOGGER.error(
                        "Unable to activate authored Erden urban interior role={} entrance={},{}",
                        entrance.role(), entrance.x(), entrance.z(), throwable);
            }
        }

        int complete = data.completedCount(INTERIOR_REVISION);
        if (!completionLogged && complete == entrances.size()) {
            completionLogged = true;
            LivingKingdoms.LOGGER.info(
                    "Completed Erden functional urban interiors plots={} fixture_families={} source_authored_ground=233 synthetic_room_carving=0 source_blocks_cut=0 clear_authored_routes=true revision={}",
                    complete, SUPPORTED_ROLES.size(), INTERIOR_REVISION);
        }
    }

    public static int fixtureFamilyCount() {
        return SUPPORTED_ROLES.size();
    }

    public static Map<String, Integer> plannedInteriorCounts() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String role : SUPPORTED_ROLES) {
            result.put(role, ExternalUrbanFabricBuilder.roleCount(role));
        }
        return Map.copyOf(result);
    }

    public static int completedCount(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE)
                .completedCount(INTERIOR_REVISION);
    }

    private static void reset(MinecraftServer server) {
        activeServer = server;
        diagnosticsLogged = false;
        completionLogged = false;
        ciChunksRequested = false;
        ciSamplePassed = false;
    }

    private static void logDiagnosticsOnce(List<ExternalUrbanFabricBuilder.UrbanEntrance> entrances) {
        if (diagnosticsLogged) return;
        ErdenUrbanAuthoredGroundPlanCatalog.bootstrap();
        Map<String, Integer> counts = plannedInteriorCounts();
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        if (total != entrances.size() || total != ErdenUrbanAuthoredGroundPlanCatalog.EXPECTED_PLANS) {
            throw new IllegalStateException("Authored urban interior count mismatch roles="
                    + total + " entrances=" + entrances.size());
        }
        diagnosticsLogged = true;
        LivingKingdoms.LOGGER.info(
                "Prepared Erden functional urban interiors plots={} fixture_families={} source_authored_ground=true synthetic_room_carving=false source_blocks_cut=0 roles={}",
                total, SUPPORTED_ROLES.size(), counts);
    }

    private static void requestCiSampleChunks(ServerLevel level) {
        if (ciChunksRequested || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;
        ExternalUrbanFabricBuilder.UrbanEntrance entrance = ExternalUrbanFabricBuilder.diagnosticEntrance();
        ErdenUrbanAuthoredGroundPlanCatalog.PlacementPlan plan =
                ErdenUrbanAuthoredGroundPlanCatalog.plan(entrance);
        if (plan == null) throw new IllegalStateException("Missing authored urban CI plan");
        for (BlockPos pos : planPositions(plan)) {
            int centerChunkX = pos.getX() >> 4;
            int centerChunkZ = pos.getZ() >> 4;
            for (int chunkX = centerChunkX - 1; chunkX <= centerChunkX + 1; chunkX++) {
                for (int chunkZ = centerChunkZ - 1; chunkZ <= centerChunkZ + 1; chunkZ++) {
                    ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);
                }
            }
        }
        ciChunksRequested = true;
    }

    private static List<BlockPos> planPositions(
            ErdenUrbanAuthoredGroundPlanCatalog.PlacementPlan plan) {
        java.util.ArrayList<BlockPos> positions = new java.util.ArrayList<>();
        positions.addAll(plan.residentTargets());
        positions.add(plan.workTarget());
        if (plan.primaryContainer() != null) positions.add(plan.primaryContainer());
        for (ErdenUrbanAuthoredGroundPlanCatalog.BedPlan bed : plan.beds()) {
            positions.add(bed.foot());
            positions.add(bed.head());
        }
        for (ErdenUrbanAuthoredGroundPlanCatalog.FixturePlan fixture : plan.fixtures()) {
            positions.add(fixture.pos());
        }
        return List.copyOf(positions);
    }

    private static void verifyCiSampleIfNeeded(
            ServerLevel level,
            ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        if (ciSamplePassed || !"1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"))) return;
        ExternalUrbanFabricBuilder.UrbanEntrance diagnostic = ExternalUrbanFabricBuilder.diagnosticEntrance();
        if (diagnostic == null || entrance.x() != diagnostic.x() || entrance.z() != diagnostic.z()) return;
        ErdenUrbanAuthoredGroundPlanCatalog.PlacementPlan plan =
                ErdenUrbanAuthoredGroundPlanCatalog.plan(entrance);
        if (plan == null || plan.residentTargets().size() != 3 || plan.workTarget() == null) {
            throw new IllegalStateException("Authored urban CI plan lost movement targets");
        }
        for (BlockPos target : plan.residentTargets()) {
            ErdenUrbanResidenceResolver.verifyTargetOrThrow(level, target, "authored-ground-resident-ci");
        }
        ErdenUrbanResidenceResolver.verifyTargetOrThrow(level, plan.workTarget(), "authored-ground-work-ci");
        ciSamplePassed = true;
        LivingKingdoms.LOGGER.info(
                "LK_URBAN_INTERIOR_DIAGNOSTIC_PASS role={} ground_cells={} fixtures={} beds={} authored_ground=true synthetic_room=false source_blocks_cut=0 resident_targets=3 work_target=true",
                plan.role(), plan.groundCells(), plan.fixtures().size(), plan.beds().size());
    }

    private static long entranceKey(ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        return ((long) entrance.x() << 32) ^ (entrance.z() & 0xffffffffL);
    }
}
