package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

final class VillageBuildingCatalog {
    private VillageBuildingCatalog() {}

    static Spec spec(VillageProgressionSystem.Building building) {
        return switch (building) {
            case TOWN_HALL -> new Spec(-16, 28, 33, 23, 8, Blocks.OAK_PLANKS, Blocks.DEEPSLATE_TILES);
            case BARRACKS -> new Spec(-14, -45, 29, 19, 7, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS);
            case SMITHY -> new Spec(-54, -20, 25, 20, 7, Blocks.BRICKS, Blocks.DEEPSLATE_TILES);
            case SKILL_HALL -> new Spec(30, -20, 25, 20, 7, Blocks.OAK_PLANKS, Blocks.DARK_OAK_PLANKS);
            case STOREHOUSE -> new Spec(-54, 10, 25, 20, 7, Blocks.SPRUCE_PLANKS, Blocks.BRICKS);
            case INFIRMARY -> new Spec(30, 10, 25, 20, 7, Blocks.QUARTZ_BLOCK, Blocks.STONE_BRICKS);
            case WALLS -> new Spec(0, -58, 1, 1, 1, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS);
        };
    }

    static void furnish(ServerLevel level, BlockPos origin, Spec spec,
                        VillageProgressionSystem.Building building) {
        BlockPos middle = origin.offset(spec.width / 2, 2, spec.depth / 2);
        switch (building) {
            case TOWN_HALL -> {
                put(level, middle, Blocks.CRAFTING_TABLE);
                shelf(level, middle.offset(-5, 0, 2));
                shelf(level, middle.offset(5, 0, 2));
            }
            case SMITHY -> {
                put(level, middle, Blocks.SMITHING_TABLE);
                put(level, middle.offset(-5, 0, 2), Blocks.BLAST_FURNACE);
                put(level, middle.offset(5, 0, 2), Blocks.ANVIL);
                put(level, middle.offset(0, 0, 4), Blocks.FLETCHING_TABLE);
            }
            case SKILL_HALL -> {
                put(level, middle, Blocks.ENCHANTING_TABLE);
                for (int x : new int[]{-4, -3, 3, 4}) shelf(level, middle.offset(x, 0, 3));
            }
            case INFIRMARY -> {
                put(level, middle, Blocks.BREWING_STAND);
                put(level, middle.offset(-5, 0, 2), Blocks.CAULDRON);
                shelf(level, middle.offset(5, 0, 2));
                line(level, middle.offset(-6, 0, 5), 4, 4, Blocks.QUARTZ_BLOCK);
            }
            case STOREHOUSE -> {
                put(level, middle, Blocks.BARREL);
                line(level, middle.offset(-7, 0, 4), 5, 3, Blocks.BARREL);
                line(level, middle.offset(-7, 0, -4), 5, 3, Blocks.CHEST);
            }
            case BARRACKS -> {
                put(level, middle, Blocks.TARGET);
                line(level, middle.offset(-8, 0, 4), 6, 3, Blocks.SPRUCE_PLANKS);
            }
            case WALLS -> {}
        }
    }

    private static void shelf(ServerLevel level, BlockPos pos) {
        put(level, pos, Blocks.BOOKSHELF);
        put(level, pos.above(), Blocks.BOOKSHELF);
    }

    private static void line(ServerLevel level, BlockPos start, int count, int spacing, Block block) {
        for (int i = 0; i < count; i++) put(level, start.offset(i * spacing, 0, 0), block);
    }

    private static void put(ServerLevel level, BlockPos pos, Block block) {
        VillageFortressTerrain.set(level, pos, block);
    }

    record Spec(int dx, int dz, int width, int depth, int height, Block panel, Block roof) {}
}
