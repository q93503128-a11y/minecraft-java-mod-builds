package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;

final class VillageBuildingCatalog {
    private VillageBuildingCatalog() {
    }

    static Spec spec(VillageProgressionSystem.Building building) {
        return switch (building) {
            case TOWN_HALL -> new Spec(
                    -25, 34, 51, 31, 14,
                    Blocks.CALCITE, Blocks.DEEPSLATE_TILES,
                    "", "", Rotation.NONE, Direction.NORTH,
                    "마을 회관", "회의 / 마을 관리");
            case BARRACKS -> new Spec(
                    -70, -52, 27, 21, 9,
                    Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS,
                    "", "", Rotation.NONE, Direction.EAST,
                    "병영·훈련장", "훈련 / 역할 강화");
            case SMITHY -> new Spec(
                    -70, -18, 27, 21, 9,
                    Blocks.BRICKS, Blocks.DEEPSLATE_TILES,
                    "", "", Rotation.NONE, Direction.EAST,
                    "대장간", "장비 강화");
            case SKILL_HALL -> new Spec(
                    44, -18, 27, 21, 9,
                    Blocks.OAK_PLANKS, Blocks.DARK_OAK_PLANKS,
                    "", "", Rotation.NONE, Direction.WEST,
                    "기술 연구소", "스킬 트리 / 능력");
            case STOREHOUSE -> new Spec(
                    -70, 18, 27, 21, 9,
                    Blocks.SPRUCE_PLANKS, Blocks.BRICKS,
                    "", "", Rotation.NONE, Direction.EAST,
                    "상점·보급소", "구매 / 판매 / 식량");
            case INFIRMARY -> new Spec(
                    44, 18, 27, 21, 9,
                    Blocks.QUARTZ_BLOCK, Blocks.STONE_BRICKS,
                    "", "", Rotation.NONE, Direction.WEST,
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
        BlockPos entrance = entrance(level, origin, spec);
        placeEntrancePath(level, entrance, spec);
        placeEntrancePlaque(level, entrance, spec);
        placeInterior(level, origin, entrance, spec, building);
    }

    static BlockPos entrance(ServerLevel level, BlockPos origin, Spec spec) {
        Direction front = spec.entranceFacing();
        Direction sideways = front.getClockWise();
        int footprintCenterX2 = origin.getX() * 2 + spec.width() - 1;
        int footprintCenterZ2 = origin.getZ() * 2 + spec.depth() - 1;
        BlockPos selectedDoor = null;
        int selectedY = Integer.MAX_VALUE;
        int selectedProjection = Integer.MIN_VALUE;
        int selectedLateral = Integer.MAX_VALUE;

        for (int x = origin.getX(); x < origin.getX() + spec.width(); x++) {
            for (int z = origin.getZ(); z < origin.getZ() + spec.depth(); z++) {
                for (int y = origin.getY(); y <= origin.getY() + spec.height(); y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).is(BlockTags.DOORS)) {
                        continue;
                    }
                    int relativeX2 = x * 2 - footprintCenterX2;
                    int relativeZ2 = z * 2 - footprintCenterZ2;
                    int projection = relativeX2 * front.getStepX()
                            + relativeZ2 * front.getStepZ();
                    int lateral = Math.abs(relativeX2 * sideways.getStepX()
                            + relativeZ2 * sideways.getStepZ());
                    boolean betterFloor = y < selectedY;
                    boolean sameFloorBetterProjection = y == selectedY && projection > selectedProjection;
                    boolean sameProjectionMoreCentral = y == selectedY
                            && projection == selectedProjection
                            && lateral < selectedLateral;
                    if (betterFloor || sameFloorBetterProjection || sameProjectionMoreCentral) {
                        selectedDoor = pos;
                        selectedY = y;
                        selectedProjection = projection;
                        selectedLateral = lateral;
                    }
                }
            }
        }
        if (selectedDoor != null) {
            return selectedDoor.relative(front);
        }

        return switch (front) {
            case NORTH -> origin.offset(spec.width() / 2, 1, -1);
            case SOUTH -> origin.offset(spec.width() / 2, 1, spec.depth());
            case WEST -> origin.offset(-1, 1, spec.depth() / 2);
            case EAST -> origin.offset(spec.width(), 1, spec.depth() / 2);
            default -> origin.offset(spec.width() / 2, 1, spec.depth() / 2);
        };
    }

    private static Block terminalBlock(VillageProgressionSystem.Building building) {
        return switch (building) {
            case TOWN_HALL -> Blocks.LECTERN;
            case BARRACKS -> Blocks.TARGET;
            case SMITHY -> Blocks.SMITHING_TABLE;
            case SKILL_HALL -> Blocks.ENCHANTING_TABLE;
            case STOREHOUSE -> Blocks.BARREL;
            case INFIRMARY -> Blocks.BREWING_STAND;
            case WALLS -> Blocks.STONE_BRICKS;
        };
    }

    private static void placeInterior(
            ServerLevel level,
            BlockPos origin,
            BlockPos entrance,
            Spec spec,
            VillageProgressionSystem.Building building) {
        Direction inward = spec.entranceFacing().getOpposite();
        Direction sideways = spec.entranceFacing().getClockWise();
        BlockPos terminalPos = building == VillageProgressionSystem.Building.TOWN_HALL
                ? origin.offset(spec.width() / 2, 1, spec.depth() / 2 - 2)
                : entrance.relative(inward, 5).relative(sideways, 4);
        set(level, terminalPos, terminalBlock(building));

        switch (building) {
            case TOWN_HALL -> {
                int tableZ = origin.getZ() + spec.depth() / 2 + 4;
                for (int x = origin.getX() + 13; x <= origin.getX() + spec.width() - 14; x++) {
                    set(level, new BlockPos(x, origin.getY() + 1, tableZ), Blocks.DARK_OAK_PLANKS);
                }
                set(level, origin.offset(8, 1, 8), Blocks.CARTOGRAPHY_TABLE);
                set(level, origin.offset(spec.width() - 9, 1, 8), Blocks.CRAFTING_TABLE);
            }
            case SMITHY -> {
                set(level, terminalPos.relative(sideways, 2), Blocks.ANVIL);
                set(level, terminalPos.relative(sideways.getOpposite(), 2), Blocks.BLAST_FURNACE);
                set(level, terminalPos.relative(inward, 2), Blocks.GRINDSTONE);
            }
            case SKILL_HALL -> {
                for (int side : new int[]{-2, 2}) {
                    set(level, terminalPos.relative(sideways, side), Blocks.BOOKSHELF);
                    set(level, terminalPos.relative(sideways, side).relative(inward), Blocks.BOOKSHELF);
                }
            }
            case INFIRMARY -> {
                set(level, terminalPos.relative(sideways, 2), Blocks.CAULDRON);
                set(level, terminalPos.relative(sideways.getOpposite(), 2), Blocks.QUARTZ_BLOCK);
            }
            case STOREHOUSE -> set(level, terminalPos.relative(inward, 2), Blocks.CRAFTING_TABLE);
            case BARRACKS -> {
                set(level, terminalPos.relative(sideways, 3), Blocks.IRON_BLOCK);
                set(level, terminalPos.relative(sideways.getOpposite(), 3), Blocks.IRON_BLOCK);
            }
            case WALLS -> {
            }
        }
    }

    private static void placeEntrancePath(ServerLevel level, BlockPos entrance, Spec spec) {
        Direction sideways = spec.entranceFacing().getClockWise();
        for (int forward = 0; forward <= 7; forward++) {
            BlockPos row = entrance.relative(spec.entranceFacing(), forward);
            for (int side = -2; side <= 2; side++) {
                BlockPos floor = row.relative(sideways, side).below();
                set(level, floor, Math.abs(side) == 2 ? Blocks.STONE_BRICKS : Blocks.PACKED_MUD);
                for (int y = 1; y <= 4; y++) {
                    set(level, floor.above(y), Blocks.AIR);
                }
            }
        }
    }

    private static void placeEntrancePlaque(ServerLevel level, BlockPos entrance, Spec spec) {
        Direction sideways = spec.entranceFacing().getClockWise();
        BlockPos signPos = entrance.relative(sideways.getOpposite(), 3).above(2);
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
