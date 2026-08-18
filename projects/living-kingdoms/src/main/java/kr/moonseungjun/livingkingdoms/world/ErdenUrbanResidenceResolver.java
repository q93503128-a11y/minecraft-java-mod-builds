package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Resolves Erden home/work targets exclusively from verified authored ground and upper topology. */
public final class ErdenUrbanResidenceResolver {
    public static final int EXPECTED_GROUND_ONLY_BUILDINGS = 0;

    private ErdenUrbanResidenceResolver() {}

    public static int groundOnlyBuildingCount() {
        int count = 0;
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : ExternalUrbanFabricBuilder.entrances()) {
            if (!ErdenUrbanAuthoredUpperRouteManager.isEligible(entrance)) count++;
        }
        if (count != EXPECTED_GROUND_ONLY_BUILDINGS) {
            throw new IllegalStateException("Erden ground-only building count drifted: " + count);
        }
        return count;
    }

    public static int groundOnlyHomeCount() {
        int count = 0;
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : ExternalUrbanFabricBuilder.entrances()) {
            if (entrance.role().equals("tenement") && !ErdenUrbanAuthoredUpperRouteManager.isEligible(entrance)) count++;
        }
        if (count != 0) throw new IllegalStateException("Erden ground-only home count drifted: " + count);
        return count;
    }

    public static boolean isGroundOnly(ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        return !ErdenUrbanAuthoredUpperRouteManager.isEligible(entrance);
    }

    public static boolean isResidenceReady(
            ServerLevel level,
            ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        return groundReady(level, entrance)
                && ErdenUrbanAuthoredUpperRouteManager.isEligible(entrance)
                && ErdenUrbanAuthoredUpperRouteManager.isCompleted(level, entrance)
                && ErdenUrbanAuthoredUpperRouteManager.verifiedUpperTarget(level, entrance) != null;
    }

    public static BlockPos resolveHomeTarget(
            ServerLevel level,
            ExternalUrbanFabricBuilder.UrbanEntrance entrance,
            int bedSlot) {
        BlockPos upper = resolveUpperActivityTarget(level, entrance, bedSlot);
        if (upper != null) return upper;
        if (!groundReady(level, entrance)) return null;
        BlockPos ground = ErdenUrbanAuthoredGroundPlanCatalog.homeTarget(entrance, bedSlot);
        return ground != null && walkable(level, ground) ? ground : null;
    }

    /** Returns a verified point on an authored upper level, never a synthetic fallback. */
    public static BlockPos resolveUpperActivityTarget(
            ServerLevel level,
            ExternalUrbanFabricBuilder.UrbanEntrance entrance,
            int slot) {
        if (!ErdenUrbanAuthoredUpperRouteManager.isEligible(entrance)
                || !ErdenUrbanAuthoredUpperRouteManager.isCompleted(level, entrance)) return null;
        BlockPos authored = ErdenUrbanAuthoredUpperRouteManager.verifiedUpperTarget(level, entrance);
        return authored == null ? null : nearbyWalkable(level, authored, slot);
    }

    public static BlockPos resolveWorkTarget(
            ServerLevel level,
            ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        if (!groundReady(level, entrance)) return null;
        BlockPos target = ErdenUrbanAuthoredGroundPlanCatalog.workTarget(entrance);
        return target != null && walkable(level, target) ? target : null;
    }

    public static void verifyTargetOrThrow(ServerLevel level, BlockPos target, String label) {
        if (target == null || !walkable(level, target)) {
            throw new IllegalStateException("Erden residence target is not walkable label="
                    + label + " target=" + target);
        }
    }

    private static boolean groundReady(
            ServerLevel level,
            ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        long key = key(entrance.x(), entrance.z());
        return ErdenUrbanAuthoredGroundPlanCatalog.plan(entrance) != null
                && level.getDataStorage().computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE)
                .isComplete(key, ErdenUrbanInteriorBuilder.INTERIOR_REVISION);
    }

    private static BlockPos nearbyWalkable(ServerLevel level, BlockPos target, int slot) {
        int[][] offsets = {{0,0},{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,-1},{1,-1},{-1,1}};
        int start = Math.floorMod(slot, offsets.length);
        for (int i = 0; i < offsets.length; i++) {
            int[] offset = offsets[(start + i) % offsets.length];
            BlockPos pos = target.offset(offset[0], 0, offset[1]);
            if (walkable(level, pos)) return pos;
        }
        return walkable(level, target) ? target : null;
    }

    private static boolean walkable(ServerLevel level, BlockPos pos) {
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return false;
        BlockState floor = level.getBlockState(pos.below());
        return !floor.isAir() && floor.getFluidState().isEmpty()
                && level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir();
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}
