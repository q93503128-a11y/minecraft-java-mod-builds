package kr.countrysidedays.worldgen;

import kr.countrysidedays.CountrysideDays;
import kr.countrysidedays.world.CountrysideRegionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Logs a deterministic proof that the standard superflat spawn used countryside worldgen. */
public final class CountrysideWorldgenAudit {
    private CountrysideWorldgenAudit() {
    }

    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel level = event.getServer().overworld();
        if (!CountrysideRegionManager.isFlatWorld(level)) {
            return;
        }

        BlockState road = surface(level, 0, 18);
        BlockState river = surface(level, 0, 118);
        boolean roadReady = road.is(Blocks.PACKED_MUD) || road.is(Blocks.GRAVEL);
        boolean riverReady = river.is(Blocks.WATER) || !river.getFluidState().isEmpty();

        if (roadReady && riverReady) {
            CountrysideDays.LOGGER.info(
                    "Countryside worldgen audit passed: road={} river={}",
                    road.getBlock(),
                    river.getBlock()
            );
        } else {
            CountrysideDays.LOGGER.error(
                    "Countryside worldgen audit failed: expected generated road and river, found road={} river={}",
                    road.getBlock(),
                    river.getBlock()
            );
        }
    }

    private static BlockState surface(ServerLevel level, int x, int z) {
        BlockPos air = level.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(x, 0, z)
        );
        return level.getBlockState(air.below());
    }
}
