package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Comparator;

/** Resolves the active Erden player residence exclusively from verified authored urban interiors. */
public final class SafeResidenceLocator {
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;
    private static final int DESIRED_RESIDENCE_X_OFFSET = 320;
    private static final int DESIRED_RESIDENCE_Z_OFFSET = 180;

    private SafeResidenceLocator() {
    }

    /**
     * Chooses one deterministic real tenement near the intended citizen-quarter address. The
     * entrance metadata is authored placement truth; no roof/surface scan participates in selection.
     */
    public static ExternalUrbanFabricBuilder.UrbanEntrance starterResidenceEntrance(
            ServerLevel level, String homelandId, String residenceId) {
        RealmSiteLayoutSavedData.RealmSite site = requiredSite(level, homelandId);
        requireActiveResidence(homelandId, residenceId);
        int desiredX = site.centerX() + DESIRED_RESIDENCE_X_OFFSET;
        int desiredZ = site.centerZ() + DESIRED_RESIDENCE_Z_OFFSET;
        return ExternalUrbanFabricBuilder.entrances().stream()
                .filter(entrance -> "tenement".equals(entrance.role()))
                .min(Comparator
                        .comparingLong((ExternalUrbanFabricBuilder.UrbanEntrance entrance) ->
                                squaredDistance(entrance.x(), entrance.z(), desiredX, desiredZ))
                        .thenComparingInt(ExternalUrbanFabricBuilder.UrbanEntrance::z)
                        .thenComparingInt(ExternalUrbanFabricBuilder.UrbanEntrance::x))
                .orElseThrow(() -> new IllegalStateException("No authored Erden tenement exists for player residence"));
    }

    public static ChunkPos residenceChunk(ServerLevel level, String homelandId, String residenceId) {
        ExternalUrbanFabricBuilder.UrbanEntrance entrance = starterResidenceEntrance(level, homelandId, residenceId);
        return new ChunkPos(entrance.x() >> 4, entrance.z() >> 4);
    }

    /**
     * Returns the real authored room target only after its ground/upper topology is physically ready.
     * Returning null means callers must wait; this method never manufactures a floor or accepts a roof.
     */
    public static BlockPos residence(ServerLevel level, String homelandId, String residenceId) {
        ExternalUrbanFabricBuilder.UrbanEntrance entrance = starterResidenceEntrance(level, homelandId, residenceId);
        if (!level.hasChunk(entrance.x() >> 4, entrance.z() >> 4)) return null;
        if (!ErdenUrbanResidenceResolver.isResidenceReady(level, entrance)) return null;
        BlockPos target = ErdenUrbanResidenceResolver.resolveHomeTarget(level, entrance, 0);
        if (target == null) return null;
        ErdenUrbanResidenceResolver.verifyTargetOrThrow(level, target, "player_starter_rental");
        return target;
    }

    public static boolean isVerifiedResidence(ServerLevel level, String homelandId,
                                              String residenceId, BlockPos target) {
        BlockPos verified = residence(level, homelandId, residenceId);
        return verified != null && verified.equals(target);
    }

    public static BlockPos preferredJail(ServerLevel level, String jurisdiction) {
        RealmSiteLayoutSavedData.RealmSite site = requiredSite(level, jurisdiction);
        int x = site.centerX() - 360;
        int z = site.centerZ() - 80;
        int surfaceY = authoredSurfaceY(x, z);
        return new BlockPos(x, surfaceY + 1, z);
    }

    public static BlockPos jail(ServerLevel level, String jurisdiction) {
        BlockPos preferred = preferredJail(level, jurisdiction);
        if (!level.hasChunkAt(preferred)) return preferred;
        return findOrCreateCustodyCell(level, preferred, Blocks.STONE_BRICKS, 8, 10);
    }

    public static float yaw(String homelandId, String residenceId) {
        requireActiveResidence(homelandId, residenceId);
        return 180.0F;
    }

    public static boolean isWalkable(ServerLevel level, BlockPos feet) {
        if (!level.hasChunkAt(feet)) return false;
        return level.getBlockState(feet.below()).isSolid()
                && level.getBlockState(feet).isAir()
                && level.getBlockState(feet.above()).isAir();
    }

    private static RealmSiteLayoutSavedData.RealmSite requiredSite(ServerLevel level, String homelandId) {
        RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(level, homelandId);
        if (site == null || !site.built() || site.revision() < RealmSitePlanner.LAYOUT_REVISION) {
            throw new IllegalStateException("Authored site is not ready: " + homelandId);
        }
        return site;
    }

    private static void requireActiveResidence(String homelandId, String residenceId) {
        if (!"erden_kingdom".equals(homelandId) || !"erden_city_room".equals(residenceId)) {
            throw new IllegalArgumentException("Inactive origin residence");
        }
    }

    /** Custody remains self-securing; player residence assignment never calls this fallback. */
    private static BlockPos findOrCreateCustodyCell(ServerLevel level, BlockPos preferred, Block floor,
                                                     int horizontalRadius, int verticalRadius) {
        if (!level.hasChunkAt(preferred)) return preferred;
        for (int dy = 0; dy <= verticalRadius; dy++) {
            for (int sign : new int[]{1, -1}) {
                if (dy == 0 && sign < 0) continue;
                int y = preferred.getY() + dy * sign;
                for (int radius = 0; radius <= horizontalRadius; radius++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        BlockPos north = new BlockPos(preferred.getX() + dx, y, preferred.getZ() - radius);
                        if (isWalkable(level, north)) return north;
                        BlockPos south = new BlockPos(preferred.getX() + dx, y, preferred.getZ() + radius);
                        if (isWalkable(level, south)) return south;
                    }
                    for (int dz = -radius + 1; dz < radius; dz++) {
                        BlockPos west = new BlockPos(preferred.getX() - radius, y, preferred.getZ() + dz);
                        if (isWalkable(level, west)) return west;
                        BlockPos east = new BlockPos(preferred.getX() + radius, y, preferred.getZ() + dz);
                        if (isWalkable(level, east)) return east;
                    }
                }
            }
        }
        return secureCustody(level, preferred, floor);
    }

    private static BlockPos secureCustody(ServerLevel level, BlockPos feet, Block floor) {
        if (!level.hasChunkAt(feet)) return feet;
        BlockPos floorPos = feet.below();
        level.setBlock(floorPos, floor.defaultBlockState(), UPDATE_FLAGS);
        for (int dy = 0; dy <= 2; dy++) {
            level.setBlock(feet.above(dy), Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
        }
        if (!isWalkable(level, feet)) {
            throw new IllegalStateException("Unable to create safe custody cell at " + feet);
        }
        return feet;
    }

    private static long squaredDistance(int x, int z, int targetX, int targetZ) {
        long dx = (long) x - targetX;
        long dz = (long) z - targetZ;
        return dx * dx + dz * dz;
    }

    private static int authoredSurfaceY(int x, int z) {
        return (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
    }
}
