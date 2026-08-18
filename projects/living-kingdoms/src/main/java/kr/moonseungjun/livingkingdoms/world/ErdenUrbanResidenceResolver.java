package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Resolves a real Erden residence without assuming that every building must have a synthetic upper floor. */
public final class ErdenUrbanResidenceResolver {
    public static final int EXPECTED_GROUND_ONLY_BUILDINGS = 77;

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
        return count;
    }

    public static boolean isGroundOnly(ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        return !ErdenUrbanAuthoredUpperRouteManager.isEligible(entrance);
    }

    public static boolean isResidenceReady(ServerLevel level, ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        if (ErdenUrbanAuthoredUpperRouteManager.isEligible(entrance)) {
            return ErdenUrbanAuthoredUpperRouteManager.isCompleted(level, entrance)
                    && ErdenUrbanAuthoredUpperRouteManager.verifiedUpperTarget(level, entrance) != null;
        }
        long key = key(entrance.x(), entrance.z());
        return level.getDataStorage().computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE)
                .isComplete(key, ErdenUrbanInteriorBuilder.INTERIOR_REVISION);
    }

    public static BlockPos resolveHomeTarget(ServerLevel level, ExternalUrbanFabricBuilder.UrbanEntrance entrance, int bedSlot) {
        BlockPos upper = resolveUpperActivityTarget(level, entrance, bedSlot);
        if (upper != null) return upper;
        if (ErdenUrbanAuthoredUpperRouteManager.isEligible(entrance)) return null;
        if (!isResidenceReady(level, entrance)) return null;
        return groundTarget(level, entrance, 4 + Math.floorMod(bedSlot, 3));
    }

    /**
     * Resolves a walkable point on a verified authored upper level. This never synthesizes a floor and
     * never falls back to the ground floor, so callers can safely use it for optional multi-storey activity.
     */
    public static BlockPos resolveUpperActivityTarget(
            ServerLevel level,
            ExternalUrbanFabricBuilder.UrbanEntrance entrance,
            int slot) {
        if (!ErdenUrbanAuthoredUpperRouteManager.isEligible(entrance)
                || !ErdenUrbanAuthoredUpperRouteManager.isCompleted(level, entrance)) return null;
        BlockPos authored = ErdenUrbanAuthoredUpperRouteManager.verifiedUpperTarget(level, entrance);
        return authored == null ? null : nearbyWalkable(level, authored, slot);
    }

    public static BlockPos resolveWorkTarget(ServerLevel level, ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        long key = key(entrance.x(), entrance.z());
        if (!level.getDataStorage().computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE)
                .isComplete(key, ErdenUrbanInteriorBuilder.INTERIOR_REVISION)) return null;
        return groundTarget(level, entrance, 3);
    }

    public static void verifyTargetOrThrow(ServerLevel level, BlockPos target, String label) {
        if (target == null || !walkable(level, target)) {
            throw new IllegalStateException("Erden residence target is not walkable label=" + label + " target=" + target);
        }
    }

    private static BlockPos groundTarget(ServerLevel level, ExternalUrbanFabricBuilder.UrbanEntrance entrance, int preferredDepth) {
        int doorY = findLowestDoorY(level, entrance.x(), entrance.z());
        if (doorY == Integer.MIN_VALUE) return null;
        int dx = entrance.roadX() - entrance.x();
        int dz = entrance.roadZ() - entrance.z();
        int inwardX, inwardZ;
        if (Math.abs(dx) >= Math.abs(dz)) { inwardX = dx >= 0 ? -1 : 1; inwardZ = 0; }
        else { inwardX = 0; inwardZ = dz >= 0 ? -1 : 1; }
        int[] depths = {preferredDepth, 4, 5, 6, 3, 7, 2, 8};
        for (int depth : depths) {
            BlockPos pos = new BlockPos(entrance.x()+inwardX*depth, doorY, entrance.z()+inwardZ*depth);
            if (walkable(level,pos)) return pos;
        }
        return null;
    }

    private static BlockPos nearbyWalkable(ServerLevel level, BlockPos target, int slot) {
        int[][] offsets = {{0,0},{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,-1},{1,-1},{-1,1}};
        int start=Math.floorMod(slot, offsets.length);
        for (int i=0;i<offsets.length;i++) {
            int[] o=offsets[(start+i)%offsets.length];
            BlockPos pos=target.offset(o[0],0,o[1]);
            if (walkable(level,pos)) return pos;
        }
        return walkable(level,target) ? target : null;
    }

    private static boolean walkable(ServerLevel level, BlockPos pos) {
        if (!level.hasChunk(pos.getX()>>4,pos.getZ()>>4)) return false;
        BlockState floor=level.getBlockState(pos.below());
        return !floor.isAir() && floor.getFluidState().isEmpty()
                && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }

    private static int findLowestDoorY(ServerLevel level,int x,int z) {
        if (!level.hasChunk(x>>4,z>>4)) return Integer.MIN_VALUE;
        int designed=(int)Math.round(AuthoredContinentDensity.surfaceHeight(x,z));
        int min=Math.max(level.getMinY(),designed-8), max=Math.min(level.getMaxY()-1,designed+64);
        BlockPos.MutableBlockPos p=new BlockPos.MutableBlockPos();
        for(int y=min;y<=max;y++){p.set(x,y,z); if(level.getBlockState(p).getBlock() instanceof DoorBlock) return y;}
        return Integer.MIN_VALUE;
    }

    private static long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
}