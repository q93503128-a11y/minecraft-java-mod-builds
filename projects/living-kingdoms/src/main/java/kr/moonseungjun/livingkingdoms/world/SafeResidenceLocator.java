package kr.moonseungjun.livingkingdoms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Resolves authored interior spawn points.
 *
 * <p>World-surface height cannot be used for a residence after construction because the highest
 * motion-blocking block is usually its roof. These positions deliberately target the walkable
 * interior floor of each authored residence and jail.</p>
 */
public final class SafeResidenceLocator {
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;

    private SafeResidenceLocator() {
    }

    public static BlockPos residence(ServerLevel level, String homelandId, String residenceId) {
        RealmSiteLayoutSavedData.RealmSite site = requiredSite(level, homelandId);
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = site.baseY();

        BlockPos feet = switch (residenceId) {
            case "erden_city_room" -> new BlockPos(cx + 26, y + 1, cz + 34);
            case "erden_farm_home" -> new BlockPos(cx + 175, y + 1, cz + 105);
            case "river_fishing_hut" -> new BlockPos(cx - 171, y + 1, cz + 115);
            case "forest_camp" -> new BlockPos(cx + 133, y + 1, cz - 159);
            case "silvana_tree_home" -> new BlockPos(cx - 58, y + 17, cz - 30);
            case "silvana_moonwell_lodge" -> new BlockPos(cx + 87, y + 1, cz + 87);
            case "kardum_gate_lodge" -> new BlockPos(cx - 4, y + 2, cz - 72);
            case "kardum_worker_quarters" -> new BlockPos(cx - 72, y + 2, cz + 43);
            default -> new BlockPos(cx + 26, y + 1, cz + 34);
        };
        return secure(level, feet, floorFor(residenceId));
    }

    public static BlockPos jail(ServerLevel level, String jurisdiction) {
        RealmSiteLayoutSavedData.RealmSite site = requiredSite(level, jurisdiction);
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = site.baseY();
        BlockPos feet = switch (jurisdiction) {
            case "silvana_forest" -> new BlockPos(cx - 84, y + 1, cz + 74);
            case "kardum_league" -> new BlockPos(cx + 64, y + 12, cz - 66);
            default -> new BlockPos(cx - 83, y + 1, cz - 30);
        };
        return secure(level, feet, Blocks.STONE_BRICKS);
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
            case "silvana_tree_home" -> Blocks.STRIPPED_BIRCH_WOOD;
            case "kardum_gate_lodge", "kardum_worker_quarters" -> Blocks.POLISHED_DEEPSLATE;
            default -> Blocks.SPRUCE_PLANKS;
        };
    }

    private static BlockPos secure(ServerLevel level, BlockPos feet, Block floor) {
        BlockPos floorPos = feet.below();
        if (!level.getBlockState(floorPos).isSolid()) {
            level.setBlock(floorPos, floor.defaultBlockState(), UPDATE_FLAGS);
        }
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos clear = feet.above(dy);
            if (!level.getBlockState(clear).isAir()) {
                level.setBlock(clear, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
            }
        }
        return feet;
    }
}
