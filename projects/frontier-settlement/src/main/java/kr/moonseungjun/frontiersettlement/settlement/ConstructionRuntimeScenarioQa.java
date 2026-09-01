package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.content.FrontierContent;
import kr.moonseungjun.frontiersettlement.content.FrontierWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.lang.reflect.Method;

/** Temporary headless-server QA for construction pathing and physical-world edge cases. */
public final class ConstructionRuntimeScenarioQa {
    private static final int UPDATE = 2;
    private static int checks;
    private static int failures;

    private ConstructionRuntimeScenarioQa() {}

    public static void onServerStarted(ServerStartedEvent event) {
        System.out.println("FRONTIER_CONSTRUCTION_RUNTIME_QA_V2");
        try {
            run(event.getServer().overworld());
        } catch (Throwable t) {
            failures++;
            t.printStackTrace(System.out);
        } finally {
            System.out.printf("RUNTIME_QA_TOTAL checks=%d failures=%d%n", checks, failures);
            event.getServer().halt(false);
        }
    }

    private static void run(ServerLevel level) throws Exception {
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
        BlockPos base = new BlockPos(0, Math.max(80, surface + 8), 0);

        Method exactMove = SettlementConstructionService.class.getDeclaredMethod(
                "moveToReachable", FrontierWorkerEntity.class, BlockPos.class, double.class);
        exactMove.setAccessible(true);
        Method interactionMove = SettlementConstructionService.class.getDeclaredMethod(
                "moveTowardInteraction", ServerLevel.class, FrontierWorkerEntity.class, BlockPos.class, double.class);
        interactionMove.setAccessible(true);

        testBasicNavigation(level, base, exactMove);
        testExactScaffoldNavigation(level, base.offset(48, 0, 0), exactMove);
        testPartialPathRejected(level, base.offset(96, 0, 0), exactMove);
        testContainerApproachPathing(level, base.offset(144, 0, 0), interactionMove);
        testSafeConstructionReplacement(level, base.offset(192, 0, 0));
        testForeignContainerRejected(level, base.offset(224, 0, 0));
        testScaffoldCoverageRequiresEntry(level, base.offset(272, 0, 0));
    }

    private static void testBasicNavigation(ServerLevel level, BlockPos base, Method exactMove) throws Exception {
        resetFlatArena(level, base, 18, 8);
        FrontierWorkerEntity worker = spawnWorker(level, base.offset(-10, 0, 0));
        boolean path = worker != null && invokeBoolean(exactMove, worker, base.offset(10, 0, 0), 1.05D);
        check("exact_flat_path", path);
        if (worker != null) worker.discard();
    }

    private static void testExactScaffoldNavigation(ServerLevel level, BlockPos base, Method exactMove) throws Exception {
        int[][] starts = {{0, -10}, {10, 0}, {0, 10}, {-10, 0}};
        for (int i = 0; i < starts.length; i++) {
            resetFlatArena(level, base, 18, 12);
            BlockPos topWork = buildExactScaffold(level, base);
            FrontierWorkerEntity worker = spawnWorker(level, base.offset(starts[i][0], 0, starts[i][1]));
            boolean path = worker != null && invokeBoolean(exactMove, worker, topWork, 1.05D);
            check("exact_scaffold_path_cardinal_" + i, path);
            if (worker != null) worker.discard();
        }
    }

    /** The old moveTo(double,double,double) accepted this as a partial path. Exact-path authority must reject it. */
    private static void testPartialPathRejected(ServerLevel level, BlockPos base, Method exactMove) throws Exception {
        resetFlatArena(level, base, 24, 8);
        BlockPos blocked = base.offset(-7, 0, 0);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (Math.abs(dx) != 2 && Math.abs(dz) != 2) continue;
            for (int y = 0; y <= 2; y++) {
                level.setBlock(blocked.offset(dx, y, dz), Blocks.COBBLESTONE.defaultBlockState(), UPDATE);
            }
        }
        FrontierWorkerEntity worker = spawnWorker(level, base.offset(0, 0, -10));
        boolean blockedExact = worker != null && invokeBoolean(exactMove, worker, blocked, 1.05D);
        if (worker != null) worker.getNavigation().stop();
        boolean alternate = worker != null && invokeBoolean(exactMove, worker, base.offset(8, 0, 0), 1.05D);
        check("partial_path_rejected", !blockedExact);
        check("alternate_exact_path_selected", alternate);
        if (worker != null) worker.discard();
    }

    private static void testContainerApproachPathing(ServerLevel level, BlockPos base, Method interactionMove) throws Exception {
        resetFlatArena(level, base, 24, 8);
        BlockPos barrel = base.offset(8, 0, 0);
        level.setBlock(barrel, Blocks.BARREL.defaultBlockState(), UPDATE);
        FrontierWorkerEntity worker = spawnWorker(level, base.offset(-10, 0, 0));
        boolean accessible = worker != null && invokeBoolean(interactionMove, level, worker, barrel, 1.10D);
        check("container_uses_reachable_adjacent_cell", accessible);
        if (worker != null) worker.discard();

        resetFlatArena(level, base, 24, 8);
        level.setBlock(barrel, Blocks.BARREL.defaultBlockState(), UPDATE);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (Math.abs(dx) != 2 && Math.abs(dz) != 2) continue;
            for (int y = 0; y <= 3; y++) {
                level.setBlock(barrel.offset(dx, y, dz), Blocks.COBBLESTONE.defaultBlockState(), UPDATE);
            }
        }
        worker = spawnWorker(level, base.offset(-10, 0, 0));
        boolean sealed = worker != null && invokeBoolean(interactionMove, level, worker, barrel, 1.10D);
        check("sealed_container_rejected", !sealed);
        if (worker != null) worker.discard();
    }

    private static void testSafeConstructionReplacement(ServerLevel level, BlockPos base) throws Exception {
        resetFlatArena(level, base, 8, 5);
        Method replace = SettlementConstructionService.class.getDeclaredMethod(
                "canReplaceConstructionTarget", ServerLevel.class, BlockPos.class, BlockState.class);
        replace.setAccessible(true);

        BlockPos air = base;
        check("replace_air", invokeBoolean(replace, level, air, level.getBlockState(air)));
        BlockPos snow = base.offset(2, 0, 0);
        level.setBlock(snow, Blocks.SNOW.defaultBlockState(), UPDATE);
        check("replace_snow", invokeBoolean(replace, level, snow, level.getBlockState(snow)));
        BlockPos stone = base.offset(4, 0, 0);
        level.setBlock(stone, Blocks.STONE.defaultBlockState(), UPDATE);
        check("protect_solid", !invokeBoolean(replace, level, stone, level.getBlockState(stone)));
        BlockPos water = base.offset(0, 0, 3);
        level.setBlock(water, Blocks.WATER.defaultBlockState(), UPDATE);
        check("protect_fluid", !invokeBoolean(replace, level, water, level.getBlockState(water)));
        BlockPos chest = base.offset(3, 0, 3);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), UPDATE);
        check("protect_block_entity", !invokeBoolean(replace, level, chest, level.getBlockState(chest)));
    }

    private static void testForeignContainerRejected(ServerLevel level, BlockPos base) throws Exception {
        resetFlatArena(level, base, 8, 5);
        Method ensure = SettlementConstructionService.class.getDeclaredMethod(
                "ensureSupplyCrate", ServerLevel.class, BlockPos.class);
        ensure.setAccessible(true);

        BlockPos foreign = base;
        level.setBlock(foreign, Blocks.CHEST.defaultBlockState(), UPDATE);
        Object result = ensure.invoke(null, level, foreign);
        check("foreign_container_not_adopted", result == null && level.getBlockState(foreign).is(Blocks.CHEST));
        BlockPos empty = base.offset(3, 0, 0);
        Object created = ensure.invoke(null, level, empty);
        check("empty_site_creates_barrel", created instanceof Container && level.getBlockState(empty).is(Blocks.BARREL));
    }

    private static void testScaffoldCoverageRequiresEntry(ServerLevel level, BlockPos base) throws Exception {
        Method coverage = SettlementConstructionService.class.getDeclaredMethod(
                "hasFreshScaffoldCoverage", ServerLevel.class, BuildingType.class, BlockPos.class, BuildingRotation.class);
        coverage.setAccessible(true);

        for (BuildingType type : BuildingType.values()) {
            for (BuildingRotation rotation : BuildingRotation.values()) {
                int width = rotation.rotatedWidth(type);
                int depth = rotation.rotatedDepth(type);
                BlockPos origin = base;
                int radius = Math.max(28, Math.max(width, depth) + 12);

                resetFlatArena(level, origin.offset(width / 2, 0, depth / 2), radius, 18);
                boolean flat = invokeBoolean(coverage, level, type, origin, rotation);
                check("coverage_flat_" + type.id() + "_" + rotation.id(), flat);

                clearArena(level, origin.offset(width / 2, 0, depth / 2), radius, 18);
                boolean hasHigh = false;
                for (BuildingBlueprints.Placement placement : RotatedBlueprints.create(type, origin, rotation.id())) {
                    if (placement.pos().getY() - origin.getY() > 3) { hasHigh = true; break; }
                }
                boolean unsupported = invokeBoolean(coverage, level, type, origin, rotation);
                check("coverage_void_semantics_" + type.id() + "_" + rotation.id(), hasHigh ? !unsupported : unsupported);
            }
        }
    }

    private static boolean invokeBoolean(Method method, Object... args) throws Exception {
        return Boolean.TRUE.equals(method.invoke(null, args));
    }

    private static FrontierWorkerEntity spawnWorker(ServerLevel level, BlockPos pos) {
        FrontierWorkerEntity worker = new FrontierWorkerEntity(FrontierContent.FRONTIER_WORKER.get(), level);
        worker.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        worker.setNoAi(false);
        worker.setOnGround(true);
        return level.addFreshEntity(worker) ? worker : null;
    }

    private static BlockPos buildExactScaffold(ServerLevel level, BlockPos center) {
        int[][] ring = {{0,-1},{1,-1},{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1}};
        for (int y = 0; y <= 7; y++) level.setBlock(center.above(y), Blocks.OAK_FENCE.defaultBlockState(), UPDATE);
        BlockPos top = center;
        int step = 0;
        for (int[] offset : ring) {
            BlockPos column = center.offset(offset[0], 0, offset[1]);
            for (int y = 0; y < step; y++) level.setBlock(column.above(y), Blocks.OAK_FENCE.defaultBlockState(), UPDATE);
            BlockPos tread = column.above(step);
            level.setBlock(tread, Blocks.OAK_PLANKS.defaultBlockState(), UPDATE);
            top = tread.above();
            step++;
        }
        return top;
    }

    private static void resetFlatArena(ServerLevel level, BlockPos center, int radius, int height) {
        clearArena(level, center, radius, height);
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            level.setBlock(center.offset(x, -1, z), Blocks.STONE.defaultBlockState(), UPDATE);
        }
    }

    private static void clearArena(ServerLevel level, BlockPos center, int radius, int height) {
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            for (int y = -2; y <= height; y++) level.setBlock(center.offset(x, y, z), Blocks.AIR.defaultBlockState(), UPDATE);
        }
    }

    private static void check(String name, boolean ok) {
        checks++;
        if (!ok) failures++;
        System.out.printf("RUNTIME_CASE name=%s OK=%s%n", name, ok);
    }
}
