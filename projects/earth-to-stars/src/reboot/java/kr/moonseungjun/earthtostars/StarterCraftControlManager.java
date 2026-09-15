package kr.moonseungjun.earthtostars;

import g_mungus.zps.block.cableNetwork.core.Channels;
import g_mungus.zps.entity.OctoMountingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.TickEvent;

import java.util.List;

public final class StarterCraftControlManager {
    private static final int BUS_TRANSFER_PER_TICK = 2_000;

    private StarterCraftControlManager() {
    }

    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }

        level.players().forEach(player -> {
            if (!(player.getVehicle() instanceof OctoMountingEntity seat) || seat.blockEntity == null) {
                return;
            }

            BlockPos controller = seat.blockEntity.getBlockPos();
            if (!level.getBlockState(controller.offset(StarterCraftLayout.CORE)).is(EarthToStarsContent.STARTER_FLIGHT_CORE.get())) {
                return;
            }

            int forward = seat.blockEntity.getCurrentSuppliedSignal(Channels.OCT_A);      // W
            int yawLeft = seat.blockEntity.getCurrentSuppliedSignal(Channels.OCT_B);      // A
            int reverse = seat.blockEntity.getCurrentSuppliedSignal(Channels.OCT_C);      // S
            int yawRight = seat.blockEntity.getCurrentSuppliedSignal(Channels.OCT_D);     // D
            int ascend = seat.blockEntity.getCurrentSuppliedSignal(Channels.OCT_E);       // Up
            int strafeLeft = seat.blockEntity.getCurrentSuppliedSignal(Channels.OCT_F);   // Left
            int descend = seat.blockEntity.getCurrentSuppliedSignal(Channels.OCT_G);      // Down
            int strafeRight = seat.blockEntity.getCurrentSuppliedSignal(Channels.OCT_H);  // Right

            setNodes(level, controller, StarterCraftLayout.FORWARD_NODES, forward);
            setNodes(level, controller, StarterCraftLayout.REVERSE_NODES, reverse);
            setNodes(level, controller, StarterCraftLayout.ASCEND_NODES, ascend);
            setNodes(level, controller, StarterCraftLayout.DESCEND_NODES, descend);
            setNodes(level, controller, StarterCraftLayout.STRAFE_LEFT_NODES, strafeLeft);
            setNodes(level, controller, StarterCraftLayout.STRAFE_RIGHT_NODES, strafeRight);
            setNodes(level, controller, StarterCraftLayout.YAW_LEFT_NODES, yawLeft);
            setNodes(level, controller, StarterCraftLayout.YAW_RIGHT_NODES, yawRight);

            distributeBatteryPower(level, controller);
        });
    }

    private static void setNodes(ServerLevel level, BlockPos controller, List<BlockPos> offsets, int power) {
        int clamped = Math.max(0, Math.min(15, power));
        for (BlockPos offset : offsets) {
            BlockPos pos = controller.offset(offset);
            BlockState state = level.getBlockState(pos);
            if (!state.is(EarthToStarsContent.STARTER_CONTROL_NODE.get())) {
                continue;
            }
            if (state.getValue(StarterControlNodeBlock.POWER) != clamped) {
                level.setBlock(pos, state.setValue(StarterControlNodeBlock.POWER, clamped), Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
            }
        }
    }

    private static void distributeBatteryPower(ServerLevel level, BlockPos controller) {
        BlockEntity batteryEntity = level.getBlockEntity(controller.offset(StarterCraftLayout.BATTERY));
        if (batteryEntity == null) {
            return;
        }
        IEnergyStorage battery = batteryEntity.getCapability(ForgeCapabilities.ENERGY).resolve().orElse(null);
        if (battery == null || battery.getEnergyStored() <= 0) {
            return;
        }

        for (BlockPos offset : StarterCraftLayout.ALL_MODULATORS) {
            if (battery.getEnergyStored() <= 0) {
                break;
            }
            BlockEntity targetEntity = level.getBlockEntity(controller.offset(offset));
            if (targetEntity == null) {
                continue;
            }
            IEnergyStorage target = targetEntity.getCapability(ForgeCapabilities.ENERGY).resolve().orElse(null);
            if (target == null || !target.canReceive()) {
                continue;
            }

            int accepted = target.receiveEnergy(BUS_TRANSFER_PER_TICK, true);
            if (accepted <= 0) {
                continue;
            }
            int available = battery.extractEnergy(accepted, true);
            if (available <= 0) {
                continue;
            }
            int extracted = battery.extractEnergy(available, false);
            int received = target.receiveEnergy(extracted, false);
            if (received < extracted && battery.canReceive()) {
                battery.receiveEnergy(extracted - received, false);
            }
        }
    }
}
