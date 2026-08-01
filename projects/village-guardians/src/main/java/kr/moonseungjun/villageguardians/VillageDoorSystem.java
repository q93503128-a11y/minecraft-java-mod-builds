package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

final class VillageDoorSystem {
    private VillageDoorSystem() {
    }

    static boolean handle(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)
                || !VillageWorldSystem.isInsideVillageArea(event.getPos())) {
            return false;
        }

        BlockState clickedState = level.getBlockState(event.getPos());
        if (!clickedState.is(Blocks.DARK_OAK_DOOR)) {
            return false;
        }

        BlockPos lower = clickedState.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                ? event.getPos()
                : event.getPos().below();
        BlockState lowerState = level.getBlockState(lower);
        if (!lowerState.is(Blocks.DARK_OAK_DOOR)) {
            return false;
        }

        Direction sideways = lowerState.getValue(DoorBlock.FACING).getClockWise();
        BlockPos partner = findPartner(level, lower, sideways);
        if (partner != null) {
            normalizeHinges(level, lower, partner);
            lowerState = level.getBlockState(lower);
        }

        boolean open = !lowerState.getValue(DoorBlock.OPEN);
        setDoorOpen(level, lower, open);
        if (partner != null) {
            setDoorOpen(level, partner, open);
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        return true;
    }

    private static BlockPos findPartner(ServerLevel level, BlockPos lower, Direction sideways) {
        BlockState source = level.getBlockState(lower);
        for (Direction direction : new Direction[]{sideways, sideways.getOpposite()}) {
            BlockPos candidate = lower.relative(direction);
            BlockState state = level.getBlockState(candidate);
            if (state.is(Blocks.DARK_OAK_DOOR)
                    && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                    && state.getValue(DoorBlock.FACING) == source.getValue(DoorBlock.FACING)) {
                return candidate;
            }
        }
        return null;
    }

    private static void normalizeHinges(ServerLevel level, BlockPos first, BlockPos second) {
        BlockState firstState = level.getBlockState(first);
        Direction facing = firstState.getValue(DoorBlock.FACING);
        BlockPos westOrNorth;
        BlockPos eastOrSouth;
        if (first.getX() != second.getX()) {
            westOrNorth = first.getX() < second.getX() ? first : second;
            eastOrSouth = first.getX() < second.getX() ? second : first;
            if (facing == Direction.NORTH) {
                setHinge(level, westOrNorth, DoorHingeSide.LEFT);
                setHinge(level, eastOrSouth, DoorHingeSide.RIGHT);
            } else {
                setHinge(level, westOrNorth, DoorHingeSide.RIGHT);
                setHinge(level, eastOrSouth, DoorHingeSide.LEFT);
            }
            return;
        }

        westOrNorth = first.getZ() < second.getZ() ? first : second;
        eastOrSouth = first.getZ() < second.getZ() ? second : first;
        if (facing == Direction.WEST) {
            setHinge(level, westOrNorth, DoorHingeSide.RIGHT);
            setHinge(level, eastOrSouth, DoorHingeSide.LEFT);
        } else {
            setHinge(level, westOrNorth, DoorHingeSide.LEFT);
            setHinge(level, eastOrSouth, DoorHingeSide.RIGHT);
        }
    }

    private static void setHinge(ServerLevel level, BlockPos lower, DoorHingeSide hinge) {
        BlockState lowerState = level.getBlockState(lower);
        BlockState upperState = level.getBlockState(lower.above());
        if (!lowerState.is(Blocks.DARK_OAK_DOOR) || !upperState.is(Blocks.DARK_OAK_DOOR)) {
            return;
        }
        level.setBlockAndUpdate(lower, lowerState.setValue(DoorBlock.HINGE, hinge));
        level.setBlockAndUpdate(lower.above(), upperState.setValue(DoorBlock.HINGE, hinge));
    }

    private static void setDoorOpen(ServerLevel level, BlockPos lower, boolean open) {
        BlockState lowerState = level.getBlockState(lower);
        BlockState upperState = level.getBlockState(lower.above());
        if (!lowerState.is(Blocks.DARK_OAK_DOOR) || !upperState.is(Blocks.DARK_OAK_DOOR)) {
            return;
        }
        level.setBlockAndUpdate(lower, lowerState.setValue(DoorBlock.OPEN, open));
        level.setBlockAndUpdate(lower.above(), upperState.setValue(DoorBlock.OPEN, open));
    }
}
