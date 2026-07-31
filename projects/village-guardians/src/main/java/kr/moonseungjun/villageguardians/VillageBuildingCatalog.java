package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

final class VillageBuildingCatalog {
    private VillageBuildingCatalog() {
    }

    static Spec spec(VillageProgressionSystem.Building building) {
        return switch (building) {
            case TOWN_HALL -> new Spec(
                    -18, 40, 37, 27, 9,
                    Blocks.OAK_PLANKS, Blocks.DEEPSLATE_TILES,
                    "minecraft:village/plains/houses/plains_big_house_1",
                    "마을", "회관");
            case BARRACKS -> new Spec(
                    -71, -54, 31, 25, 8,
                    Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS,
                    "minecraft:village/plains/houses/plains_armorer_1",
                    "병영", "훈련장");
            case SMITHY -> new Spec(
                    -71, -18, 31, 25, 8,
                    Blocks.BRICKS, Blocks.DEEPSLATE_TILES,
                    "minecraft:village/plains/houses/plains_weaponsmith_1",
                    "대장간", "장비 강화");
            case SKILL_HALL -> new Spec(
                    41, -18, 31, 25, 8,
                    Blocks.OAK_PLANKS, Blocks.DARK_OAK_PLANKS,
                    "minecraft:village/plains/houses/plains_library_1",
                    "기술·마법", "연구소");
            case STOREHOUSE -> new Spec(
                    -71, 18, 31, 25, 8,
                    Blocks.SPRUCE_PLANKS, Blocks.BRICKS,
                    "minecraft:village/plains/houses/plains_butcher_shop_1",
                    "상점", "보급소");
            case INFIRMARY -> new Spec(
                    41, 18, 31, 25, 8,
                    Blocks.QUARTZ_BLOCK, Blocks.STONE_BRICKS,
                    "minecraft:village/plains/houses/plains_temple_3",
                    "의무소", "치료 시설");
            case WALLS -> new Spec(
                    0, -76, 1, 1, 1,
                    Blocks.STONE_BRICKS, Blocks.STONE_BRICKS,
                    "", "북문", "성벽");
        };
    }

    static void furnish(
            ServerLevel level,
            BlockPos origin,
            Spec spec,
            VillageProgressionSystem.Building building) {
        BlockPos middle = origin.offset(spec.width() / 2, 1, spec.depth() / 2);
        switch (building) {
            case TOWN_HALL -> {
                put(level, middle, Blocks.BELL);
                put(level, middle.offset(-5, 0, 2), Blocks.CRAFTING_TABLE);
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
                for (int x : new int[]{-4, -3, 3, 4}) {
                    shelf(level, middle.offset(x, 0, 3));
                }
            }
            case INFIRMARY -> {
                put(level, middle, Blocks.BREWING_STAND);
                put(level, middle.offset(-5, 0, 2), Blocks.CAULDRON);
                shelf(level, middle.offset(5, 0, 2));
                line(level, middle.offset(-6, 0, 5), 4, 4, Blocks.WHITE_WOOL);
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
            case WALLS -> {
            }
        }
        placeLabel(level, origin, spec);
    }

    private static void placeLabel(ServerLevel level, BlockPos origin, Spec spec) {
        BlockPos signPos = origin.offset(spec.width() / 2, 1, -2);
        put(level, signPos.below(), Blocks.STONE_BRICKS);
        put(level, signPos, Blocks.OAK_SIGN);
        if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
            SignText text = new SignText()
                    .setMessage(1, Component.literal(spec.labelLine1()))
                    .setMessage(2, Component.literal(spec.labelLine2()));
            sign.setText(text, true);
            sign.setText(text, false);
            sign.setWaxed(true);
        }
    }

    private static void shelf(ServerLevel level, BlockPos pos) {
        put(level, pos, Blocks.BOOKSHELF);
        put(level, pos.above(), Blocks.BOOKSHELF);
    }

    private static void line(ServerLevel level, BlockPos start, int count, int spacing, Block block) {
        for (int i = 0; i < count; i++) {
            put(level, start.offset(i * spacing, 0, 0), block);
        }
    }

    private static void put(ServerLevel level, BlockPos pos, Block block) {
        VillageFortressTerrain.set(level, pos, block);
    }

    record Spec(
            int dx,
            int dz,
            int width,
            int depth,
            int height,
            Block panel,
            Block roof,
            String templateId,
            String labelLine1,
            String labelLine2) {
    }
}
