package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Resolves and verifies the active Erden residence and civic custody cells. */
public final class SafeResidenceLocator {
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;
    private static final String STARTING_RESIDENTIAL_ROLE = "residential_middle_south_04";

    private SafeResidenceLocator() {
    }

    public static BlockPos preferredResidence(ServerLevel level, String homelandId, String residenceId) {
        requiredSite(level, homelandId);
        if (!PlayableOriginCatalog.DEFAULT_RESIDENCE.equals(residenceId)) {
            throw new IllegalArgumentException("Inactive residence: " + residenceId);
        }
        ExternalDistrictBuildingBuilder.BuildingEntrance entrance = ExternalDistrictBuildingBuilder.entrances().stream()
                .filter(candidate -> candidate.role().equals(STARTING_RESIDENTIAL_ROLE))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing authored Erden starting residence: " + STARTING_RESIDENTIAL_ROLE));

        int roadDx = entrance.x() - entrance.roadX();
        int roadDz = entrance.z() - entrance.roadZ();
        int inwardX = 0;
        int inwardZ = 0;
        if (Math.abs(roadDx) >= Math.abs(roadDz) && roadDx != 0) inwardX = Integer.signum(roadDx);
        else if (roadDz != 0) inwardZ = Integer.signum(roadDz);
        else throw new IllegalStateException("Starting residence entrance has no road separation");

        int x = entrance.x() + inwardX * 3;
        int z = entrance.z() + inwardZ * 3;
        int surfaceY = authoredSurfaceY(x, z);
        return new BlockPos(x, surfaceY + 1, z);
    }

    public static BlockPos residence(ServerLevel level, String homelandId, String residenceId) {
        BlockPos preferred = preferredResidence(level, homelandId, residenceId);
        if (!level.hasChunkAt(preferred)) return preferred;
        BlockPos existing = findExistingWalkable(level, preferred, 10, 16);
        return existing == null ? preferred : existing;
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
        BlockPos existing = findExistingWalkable(level, preferred, 8, 10);
        return existing == null ? secure(level, preferred, Blocks.STONE_BRICKS) : existing;
    }

    public static float yaw(String homelandId, String residenceId) {
        if (!PlayableOriginCatalog.DEFAULT_HOMELAND.equals(homelandId)
                || !PlayableOriginCatalog.DEFAULT_RESIDENCE.equals(residenceId)) {
            throw new IllegalArgumentException("Inactive origin residence");
        }
        return 180.0F;
    }

    public static boolean isWalkable(ServerLevel level, BlockPos feet) {
        if (!level.hasChunkAt(feet)) return false;
        return level.getBlockState(feet.below()).isSolid()
                && level.getBlockState(feet).isAir()
                && level.getBlockState(feet.above()).isAir()
                && level.getBlockState(feet.above(2)).isAir();
    }

    private static RealmSiteLayoutSavedData.RealmSite requiredSite(ServerLevel level, String homelandId) {
        RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(level, homelandId);
        if (site == null || !site.built() || site.revision() < RealmSitePlanner.LAYOUT_REVISION) {
            throw new IllegalStateException("Authored site is not ready: " + homelandId);
        }
        return site;
    }

    private static BlockPos findExistingWalkable(ServerLevel level, BlockPos preferred,
                                                 int horizontalRadius, int verticalRadius) {
        if (!level.hasChunkAt(preferred)) return null;
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
        return null;
    }

    private static BlockPos secure(ServerLevel level, BlockPos feet, Block floor) {
        if (!level.hasChunkAt(feet)) return feet;
        BlockPos floorPos = feet.below();
        level.setBlock(floorPos, floor.defaultBlockState(), UPDATE_FLAGS);
        for (int dy = 0; dy <= 2; dy++) {
            level.setBlock(feet.above(dy), Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
        }
        if (!isWalkable(level, feet)) {
            throw new IllegalStateException("Unable to create safe custody spawn at " + feet);
        }
        return feet;
    }

    private static int authoredSurfaceY(int x, int z) {
        return (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
    }
}
