package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Rotation;

final class VillageBuildingCatalog {
    private VillageBuildingCatalog() {
    }

    static Spec spec(VillageProgressionSystem.Building building) {
        return switch (building) {
            case TOWN_HALL -> new Spec(
                    -18, 40, 37, 27, 18,
                    Blocks.OAK_PLANKS, Blocks.DEEPSLATE_TILES,
                    "villageguardians:external/town_hall",
                    "minecraft:village/plains/houses/plains_big_house_1",
                    Rotation.CLOCKWISE_180, Direction.NORTH,
                    "마을 회관", "공동 운영");
            case BARRACKS -> new Spec(
                    -71, -54, 31, 25, 18,
                    Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS,
                    "villageguardians:external/barracks",
                    "minecraft:village/plains/houses/plains_armorer_1",
                    Rotation.COUNTERCLOCKWISE_90, Direction.EAST,
                    "병영·훈련장", "훈련 / 역할 강화");
            case SMITHY -> new Spec(
                    -71, -18, 31, 25, 18,
                    Blocks.BRICKS, Blocks.DEEPSLATE_TILES,
                    "villageguardians:external/smithy",
                    "minecraft:village/plains/houses/plains_weaponsmith_1",
                    Rotation.COUNTERCLOCKWISE_90, Direction.EAST,
                    "대장간", "장비 강화");
            case SKILL_HALL -> new Spec(
                    41, -18, 31, 25, 18,
                    Blocks.OAK_PLANKS, Blocks.DARK_OAK_PLANKS,
                    "villageguardians:external/skill_hall",
                    "minecraft:village/plains/houses/plains_library_1",
                    Rotation.CLOCKWISE_90, Direction.WEST,
                    "기술·마법 연구소", "능력 습득");
            case STOREHOUSE -> new Spec(
                    -71, 18, 31, 25, 18,
                    Blocks.SPRUCE_PLANKS, Blocks.BRICKS,
                    "villageguardians:external/storehouse",
                    "minecraft:village/plains/houses/plains_butcher_shop_1",
                    Rotation.COUNTERCLOCKWISE_90, Direction.EAST,
                    "상점·보급소", "구매 / 일일 식량");
            case INFIRMARY -> new Spec(
                    41, 18, 31, 25, 18,
                    Blocks.QUARTZ_BLOCK, Blocks.STONE_BRICKS,
                    "villageguardians:external/infirmary",
                    "minecraft:village/plains/houses/plains_temple_3",
                    Rotation.CLOCKWISE_90, Direction.WEST,
                    "의무소", "치료 / 회복");
            case WALLS -> new Spec(
                    0, -76, 1, 1, 1,
                    Blocks.STONE_BRICKS, Blocks.STONE_BRICKS,
                    "", "", Rotation.NONE, Direction.SOUTH,
                    "북문", "성벽");
        };
    }

    static void furnish(
            ServerLevel level,
            BlockPos origin,
            Spec spec,
            VillageProgressionSystem.Building building) {
        if (building == VillageProgressionSystem.Building.WALLS) {
            return;
        }
        placeEntrancePath(level, origin, spec);
        placeTerminal(level, origin, spec, terminalBlock(building));
        placeEntrancePlaque(level, origin, spec);
    }

    static BlockPos entrance(ServerLevel level, BlockPos origin, Spec spec) {
        BlockPos front = switch (spec.entranceFacing()) {
            case NORTH -> origin.offset(spec.width() / 2, 0, 0);
            case SOUTH -> origin.offset(spec.width() / 2, 0, spec.depth() - 1);
            case WEST -> origin.offset(0, 0, spec.depth() / 2);
            case EAST -> origin.offset(spec.width() - 1, 0, spec.depth() / 2);
            default -> origin.offset(spec.width() / 2, 0, spec.depth() / 2);
        };
        return front.relative(spec.entranceFacing(), 2);
    }

    private static Block terminalBlock(VillageProgressionSystem.Building building) {
        return switch (building) {
            case TOWN_HALL -> Blocks.BELL;
            case BARRACKS -> Blocks.TARGET;
            case SMITHY -> Blocks.SMITHING_TABLE;
            case SKILL_HALL -> Blocks.ENCHANTING_TABLE;
            case STOREHOUSE -> Blocks.BARREL;
            case INFIRMARY -> Blocks.BREWING_STAND;
            case WALLS -> Blocks.STONE_BRICKS;
        };
    }

    private static void placeEntrancePath(ServerLevel level, BlockPos origin, Spec spec) {
        BlockPos door = entrance(level, origin, spec);
        Direction sideways = spec.entranceFacing().getClockWise();
        for (int forward = -1; forward <= 5; forward++) {
            BlockPos row = door.relative(spec.entranceFacing(), forward);
            for (int side = -2; side <= 2; side++) {
                BlockPos floor = row.relative(sideways, side).below();
                set(level, floor, Math.abs(side) == 2 ? Blocks.STONE_BRICKS : Blocks.PACKED_MUD);
                for (int y = 0; y <= 3; y++) {
                    set(level, floor.above(1 + y), Blocks.AIR);
                }
            }
        }
    }

    private static void placeTerminal(ServerLevel level, BlockPos origin, Spec spec, Block terminal) {
        Direction sideways = spec.entranceFacing().getClockWise();
        BlockPos door = entrance(level, origin, spec);
        BlockPos terminalPos = door.relative(sideways, 4);
        set(level, terminalPos.below(), Blocks.CHISELED_STONE_BRICKS);
        set(level, terminalPos, terminal);
        set(level, terminalPos.above(), Blocks.LANTERN);
    }

    private static void placeEntrancePlaque(ServerLevel level, BlockPos origin, Spec spec) {
        Direction sideways = spec.entranceFacing().getClockWise();
        BlockPos door = entrance(level, origin, spec);
        BlockPos signPos = door.relative(sideways.getOpposite(), 4).above(2);
        BlockPos backing = signPos.relative(spec.entranceFacing().getOpposite());
        set(level, backing.below(2), Blocks.STONE_BRICKS);
        set(level, backing.below(), Blocks.STRIPPED_DARK_OAK_WOOD);
        set(level, backing, Blocks.STRIPPED_DARK_OAK_WOOD);

        BlockState signState = Blocks.OAK_WALL_SIGN.defaultBlockState()
                .setValue(WallSignBlock.FACING, spec.entranceFacing());
        level.setBlockAndUpdate(signPos, signState);
        if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
            SignText text = new SignText()
                    .setMessage(0, Component.literal("§6" + spec.labelLine1()))
                    .setMessage(1, Component.literal("§0" + spec.labelLine2()));
            sign.setText(text, true);
            sign.setText(text, false);
            sign.setWaxed(true);
            sign.setChanged();
        }
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
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
            String fallbackTemplateId,
            Rotation rotation,
            Direction entranceFacing,
            String labelLine1,
            String labelLine2) {
    }
}
