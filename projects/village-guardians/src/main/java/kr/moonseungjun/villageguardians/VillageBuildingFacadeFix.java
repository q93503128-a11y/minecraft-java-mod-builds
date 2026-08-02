package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Repairs the generated facade after all furnishing passes. */
final class VillageBuildingFacadeFix {
    private VillageBuildingFacadeFix() {}

    static void apply(ServerLevel level, BlockPos origin, VillageBuildingCatalog.Spec spec,
                      VillageProgressionSystem.Building building) {
        BlockPos entrance = VillageBuildingCatalog.entrance(level, origin, spec);
        Direction front = spec.entranceFacing();
        Direction inward = front.getOpposite();
        Direction sideways = front.getClockWise();
        BlockPos doorway = entrance.relative(inward);

        closeDoorLintel(level, doorway, sideways);
        movePlaque(level, entrance, inward, sideways, front, building);
    }

    private static void closeDoorLintel(ServerLevel level, BlockPos doorway, Direction sideways) {
        for (int lateral = -2; lateral <= 2; lateral++) {
            for (int vertical = -1; vertical <= 1; vertical++) {
                BlockPos candidate = doorway.relative(sideways, lateral).above(vertical);
                BlockState state = level.getBlockState(candidate);
                if (!state.is(Blocks.DARK_OAK_DOOR)
                        || !state.hasProperty(DoorBlock.HALF)
                        || state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) continue;
                BlockPos lintel = candidate.above(2);
                if (level.getBlockState(lintel).isAir()) {
                    level.setBlockAndUpdate(lintel, Blocks.STRIPPED_DARK_OAK_WOOD.defaultBlockState());
                }
            }
        }
    }

    private static void movePlaque(ServerLevel level, BlockPos entrance, Direction inward,
                                   Direction sideways, Direction front,
                                   VillageProgressionSystem.Building building) {
        BlockPos legacySign = entrance.relative(sideways.getOpposite(), 3).above(2);
        if (level.getBlockState(legacySign).is(Blocks.OAK_WALL_SIGN)) {
            level.setBlockAndUpdate(legacySign, Blocks.AIR.defaultBlockState());
        }

        BlockPos oldColumn = legacySign.relative(inward);
        BlockPos mirrorColumn = entrance.relative(sideways, 3).above(2).relative(inward);
        for (int down = 0; down <= 2; down++) {
            BlockState mirrored = level.getBlockState(mirrorColumn.below(down));
            if (!mirrored.isAir()) level.setBlockAndUpdate(oldColumn.below(down), mirrored);
        }

        BlockPos signPos = entrance.relative(sideways.getOpposite(), 2).above(1);
        BlockPos backing = signPos.relative(inward);
        if (level.getBlockState(backing).isAir()) {
            level.setBlockAndUpdate(backing, Blocks.STRIPPED_DARK_OAK_WOOD.defaultBlockState());
        }
        level.setBlockAndUpdate(signPos,
                Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, front));
        if (level.getBlockEntity(signPos) instanceof SignBlockEntity sign) {
            sign.setText(sign.getFrontText()
                    .setMessage(0, Component.literal(building.displayName()))
                    .setMessage(1, Component.literal("기능 단말기"))
                    .setColor(DyeColor.BLACK), true);
            sign.setChanged();
        }
    }
}
