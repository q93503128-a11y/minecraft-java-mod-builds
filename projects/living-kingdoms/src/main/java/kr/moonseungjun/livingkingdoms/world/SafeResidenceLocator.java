package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Resolves and verifies authored interior spawn cells after every construction pass. */
public final class SafeResidenceLocator {
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;

    private SafeResidenceLocator() {
    }

    public static BlockPos residence(ServerLevel level, String homelandId, String residenceId) {
        RealmSiteLayoutSavedData.RealmSite site = requiredSite(level, homelandId);
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = site.baseY();
        BlockPos preferred = switch (residenceId) {
            case "erden_city_room" -> new BlockPos(cx + 26, y + 1, cz + 36);
            case "erden_farm_home" -> new BlockPos(cx + 132, y + 1, cz + 98);
            case "river_fishing_hut" -> new BlockPos(cx - 146, y + 1, cz + 91);
            case "forest_camp" -> new BlockPos(cx + 116, y + 1, cz - 132);
            case "silvana_tree_home" -> new BlockPos(cx - 45, y + 17, cz - 28);
            case "silvana_moonwell_lodge" -> new BlockPos(cx + 73, y + 2, cz + 87);
            case "kardum_gate_lodge" -> new BlockPos(cx - 4, y + 2, cz - 72);
            case "kardum_worker_quarters" -> new BlockPos(cx - 72, y + 2, cz + 43);
            default -> new BlockPos(cx + 26, y + 1, cz + 36);
        };
        return findOrCreateWalkable(level, preferred, floorFor(residenceId), 6, 8);
    }

    public static BlockPos jail(ServerLevel level, String jurisdiction) {
        RealmSiteLayoutSavedData.RealmSite site = requiredSite(level, jurisdiction);
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = site.baseY();
        BlockPos preferred = switch (jurisdiction) {
            case "silvana_forest" -> new BlockPos(cx - 67, y + 1, cz + 58);
            case "kardum_league" -> new BlockPos(cx + 69, y + 11, cz - 64);
            default -> new BlockPos(cx - 93, y + 1, cz - 31);
        };
        return findOrCreateWalkable(level, preferred, Blocks.STONE_BRICKS, 5, 6);
    }

    public static float yaw(String homelandId, String residenceId) {
        if ("silvana_forest".equals(homelandId)) return 180.0F;
        if ("kardum_league".equals(homelandId)) return 0.0F;
        return switch (residenceId) {
            case "river_fishing_hut" -> 0.0F;
            case "forest_camp" -> 180.0F;
            default -> 180.0F;
        };
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

    private static Block floorFor(String residenceId) {
        return switch (residenceId) {
            case "forest_camp" -> Blocks.COARSE_DIRT;
            case "silvana_tree_home", "silvana_moonwell_lodge" -> Blocks.STRIPPED_BIRCH_WOOD;
            case "kardum_gate_lodge", "kardum_worker_quarters" -> Blocks.POLISHED_DEEPSLATE;
            default -> Blocks.SPRUCE_PLANKS;
        };
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
