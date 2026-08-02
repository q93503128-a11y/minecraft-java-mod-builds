package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Resolves and verifies the active Erden residence and civic custody cells. */
public final class SafeResidenceLocator {
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;

    private SafeResidenceLocator() {
    }

    public static BlockPos residence(ServerLevel level, String homelandId, String residenceId) {
        RealmSiteLayoutSavedData.RealmSite site = requiredSite(level, homelandId);
        if (!"erden_city_room".equals(residenceId)) {
            throw new IllegalArgumentException("Inactive residence: " + residenceId);
        }
        int surfaceY = RealmSitePlanner.surfaceY(level, site.centerX() + 320, site.centerZ() + 180);
        BlockPos preferred = new BlockPos(site.centerX() + 320, surfaceY + 1, site.centerZ() + 180);
        return findOrCreateWalkable(level, preferred, Blocks.SPRUCE_PLANKS, 12, 12);
    }

    public static BlockPos jail(ServerLevel level, String jurisdiction) {
        RealmSiteLayoutSavedData.RealmSite site = requiredSite(level, jurisdiction);
        int surfaceY = RealmSitePlanner.surfaceY(level, site.centerX() - 360, site.centerZ() - 80);
        BlockPos preferred = new BlockPos(site.centerX() - 360, surfaceY + 1, site.centerZ() - 80);
        return findOrCreateWalkable(level, preferred, Blocks.STONE_BRICKS, 8, 10);
    }

    public static float yaw(String homelandId, String residenceId) {
        if (!"erden_kingdom".equals(homelandId) || !"erden_city_room".equals(residenceId)) {
            throw new IllegalArgumentException("Inactive origin residence");
        }
        return 180.0F;
    }

    public static boolean isWalkable(ServerLevel level, BlockPos feet) {
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

    private static BlockPos findOrCreateWalkable(ServerLevel level, BlockPos preferred, Block floor,
                                                  int horizontalRadius, int verticalRadius) {
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
        return secure(level, preferred, floor);
    }

    private static BlockPos secure(ServerLevel level, BlockPos feet, Block floor) {
        BlockPos floorPos = feet.below();
        level.setBlock(floorPos, floor.defaultBlockState(), UPDATE_FLAGS);
        for (int dy = 0; dy <= 2; dy++) {
            level.setBlock(feet.above(dy), Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
        }
        if (!isWalkable(level, feet)) {
            throw new IllegalStateException("Unable to create safe residence spawn at " + feet);
        }
        return feet;
    }
}
