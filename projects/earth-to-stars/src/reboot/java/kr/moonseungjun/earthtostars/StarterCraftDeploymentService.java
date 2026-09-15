package kr.moonseungjun.earthtostars;

import g_mungus.zpl.block.gyro.GyroscopeBlock;
import g_mungus.zpl.block.thruster.IonModulatorBlock;
import g_mungus.zpl.block.thruster.ThrusterExhaustBlock;
import g_mungus.zps.block.cableNetwork.OctoControllerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.registries.ForgeRegistries;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.assembly.ShipAssembler;

import java.util.LinkedHashMap;
import java.util.Map;

public final class StarterCraftDeploymentService {
    private StarterCraftDeploymentService() {
    }

    public static ServerShip deploy(ServerLevel level, BlockPos floorCenter) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            throw new IllegalStateException("starter craft can only be assembled on Earth");
        }

        Map<BlockPos, BlockState> template = buildTemplate(floorCenter);
        for (BlockPos pos : template.keySet()) {
            if (!level.getBlockState(pos).canBeReplaced()) {
                throw new IllegalStateException("starter craft deployment area is blocked at " + pos.toShortString());
            }
        }

        for (Map.Entry<BlockPos, BlockState> entry : template.entrySet()) {
            level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_ALL);
        }

        BlockPos controllerPos = StarterCraftLayout.controllerFromFloor(floorCenter);
        fillBattery(level, controllerPos.offset(StarterCraftLayout.BATTERY));

        try {
            // This is the @JvmStatic assembly API shipped by the pinned VS 2.4.10 line and
            // used by Genesis' own 1.20.1 source. VS owns relocation, collision and physics.
            return ShipAssembler.assembleToShipFull(level, template.keySet(), 1.0D).getShip();
        } catch (RuntimeException | AssertionError error) {
            template.keySet().forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
            throw error;
        }
    }

    private static Map<BlockPos, BlockState> buildTemplate(BlockPos floorCenter) {
        LinkedHashMap<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        BlockState hull = Blocks.IRON_BLOCK.defaultBlockState();
        BlockState accent = Blocks.WAXED_CUT_COPPER.defaultBlockState();
        BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();

        // Low, compact shuttle silhouette: tapered nose/rear, walkable center and visible external propulsion hardware.
        for (int z = -3; z <= 3; z++) {
            int halfWidth = Math.abs(z) == 3 ? 1 : 2;
            for (int x = -halfWidth; x <= halfWidth; x++) {
                put(blocks, floorCenter, x, 0, z, ((x + z) & 1) == 0 ? hull : accent);
            }
        }
        put(blocks, floorCenter, 0, 0, -4, accent);
        put(blocks, floorCenter, -1, 0, 4, accent);
        put(blocks, floorCenter, 1, 0, 4, accent);
        put(blocks, floorCenter, -3, 0, 1, accent);
        put(blocks, floorCenter, 3, 0, 1, accent);

        for (int z = -1; z <= 2; z++) {
            put(blocks, floorCenter, -2, 1, z, z <= 0 ? glass : accent);
            put(blocks, floorCenter, 2, 1, z, z <= 0 ? glass : accent);
        }
        for (int x = -1; x <= 1; x++) {
            put(blocks, floorCenter, x, 2, -2, glass);
            put(blocks, floorCenter, x, 2, -1, glass);
        }
        put(blocks, floorCenter, -2, 2, 0, accent);
        put(blocks, floorCenter, 2, 2, 0, accent);
        put(blocks, floorCenter, -2, 2, 1, accent);
        put(blocks, floorCenter, 2, 2, 1, accent);

        BlockPos controller = StarterCraftLayout.controllerFromFloor(floorCenter);
        putRelative(blocks, controller, StarterCraftLayout.CORE, EarthToStarsContent.STARTER_FLIGHT_CORE.get().defaultBlockState());
        putRelative(blocks, controller, BlockPos.ZERO,
                g_mungus.zps.block.ModBlocks.OCTO_CONTROLLER.get().defaultBlockState()
                        .setValue(OctoControllerBlock.FACING, Direction.NORTH));
        // The finite Power Cell exists in the pinned ZPS 2.5.1 runtime. Resolve by stable
        // registry id so the integration does not couple to an implementation field name.
        putRelative(blocks, controller, StarterCraftLayout.BATTERY,
                requireExternalBlock("zps", "power_cell").defaultBlockState());

        // Forward pair.
        putIonPair(blocks, controller, new BlockPos(-1, 0, 4), new BlockPos(-1, 0, 5), Direction.SOUTH);
        putIonPair(blocks, controller, new BlockPos(1, 0, 4), new BlockPos(1, 0, 5), Direction.SOUTH);
        // Reverse.
        putIonPair(blocks, controller, new BlockPos(0, 0, -2), new BlockPos(0, 0, -3), Direction.NORTH);
        // Vertical lift and descent.
        putIonPair(blocks, controller, new BlockPos(-1, 0, 2), new BlockPos(-1, -1, 2), Direction.DOWN);
        putIonPair(blocks, controller, new BlockPos(1, 0, 2), new BlockPos(1, -1, 2), Direction.DOWN);
        putIonPair(blocks, controller, new BlockPos(0, 1, 2), new BlockPos(0, 2, 2), Direction.UP);
        // Lateral translation.
        putIonPair(blocks, controller, new BlockPos(2, 0, 2), new BlockPos(3, 0, 2), Direction.EAST);
        putIonPair(blocks, controller, new BlockPos(-2, 0, 2), new BlockPos(-3, 0, 2), Direction.WEST);

        // Opposed vertical-axis gyros provide yaw in both directions.
        putRelative(blocks, controller, new BlockPos(-1, 0, 1),
                g_mungus.zpl.block.ModBlocks.GYROSCOPE_BLOCK.get().defaultBlockState()
                        .setValue(GyroscopeBlock.FACING, Direction.DOWN));
        putRelative(blocks, controller, new BlockPos(1, 0, 1),
                g_mungus.zpl.block.ModBlocks.GYROSCOPE_BLOCK.get().defaultBlockState()
                        .setValue(GyroscopeBlock.FACING, Direction.UP));

        StarterCraftLayout.FORWARD_NODES.forEach(offset -> putControlNode(blocks, controller, offset));
        StarterCraftLayout.REVERSE_NODES.forEach(offset -> putControlNode(blocks, controller, offset));
        StarterCraftLayout.ASCEND_NODES.forEach(offset -> putControlNode(blocks, controller, offset));
        StarterCraftLayout.DESCEND_NODES.forEach(offset -> putControlNode(blocks, controller, offset));
        StarterCraftLayout.STRAFE_LEFT_NODES.forEach(offset -> putControlNode(blocks, controller, offset));
        StarterCraftLayout.STRAFE_RIGHT_NODES.forEach(offset -> putControlNode(blocks, controller, offset));
        StarterCraftLayout.YAW_LEFT_NODES.forEach(offset -> putControlNode(blocks, controller, offset));
        StarterCraftLayout.YAW_RIGHT_NODES.forEach(offset -> putControlNode(blocks, controller, offset));

        return blocks;
    }

    private static void putIonPair(Map<BlockPos, BlockState> blocks, BlockPos controller, BlockPos modulatorOffset,
                                   BlockPos exhaustOffset, Direction facing) {
        putRelative(blocks, controller, modulatorOffset,
                g_mungus.zpl.block.ModBlocks.ION_MODULATOR_BLOCK.get().defaultBlockState()
                        .setValue(IonModulatorBlock.FACING, facing));
        putRelative(blocks, controller, exhaustOffset,
                g_mungus.zpl.block.ModBlocks.THRUSTER_EXHAUST_BLOCK.get().defaultBlockState()
                        .setValue(ThrusterExhaustBlock.FACING, facing));
    }

    private static void putControlNode(Map<BlockPos, BlockState> blocks, BlockPos controller, BlockPos offset) {
        putRelative(blocks, controller, offset, EarthToStarsContent.STARTER_CONTROL_NODE.get().defaultBlockState());
    }

    private static void put(Map<BlockPos, BlockState> blocks, BlockPos origin, int x, int y, int z, BlockState state) {
        blocks.put(origin.offset(x, y, z), state);
    }

    private static void putRelative(Map<BlockPos, BlockState> blocks, BlockPos origin, BlockPos offset, BlockState state) {
        blocks.put(origin.offset(offset), state);
    }

    private static Block requireExternalBlock(String namespace, String path) {
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(namespace, path));
        if (block == null || block == Blocks.AIR) {
            throw new IllegalStateException("required external block is absent: " + namespace + ":" + path);
        }
        return block;
    }

    private static void fillBattery(ServerLevel level, BlockPos batteryPos) {
        BlockEntity battery = level.getBlockEntity(batteryPos);
        if (battery == null) {
            throw new IllegalStateException("starter craft battery block entity did not initialize");
        }

        IEnergyStorage energy = battery.getCapability(ForgeCapabilities.ENERGY).resolve()
                .orElseThrow(() -> new IllegalStateException("starter craft battery exposes no Forge energy capability"));
        int guard = 0;
        while (energy.getEnergyStored() < energy.getMaxEnergyStored() && guard++ < 512) {
            int received = energy.receiveEnergy(energy.getMaxEnergyStored() - energy.getEnergyStored(), false);
            if (received <= 0) {
                break;
            }
        }
        if (energy.getEnergyStored() <= 0) {
            throw new IllegalStateException("starter craft battery could not be charged");
        }
    }
}
