package kr.countrysidedays.worldgen;

import kr.countrysidedays.CountrysideDays;
import kr.countrysidedays.world.CountrysideRegionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Logs a non-blocking proof that the loaded superflat spawn used countryside worldgen. */
public final class CountrysideWorldgenAudit {
    private CountrysideWorldgenAudit() {
    }

    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel level = event.getServer().overworld();
        if (!CountrysideRegionManager.isFlatWorld(level)) {
            return;
        }
        if (!level.hasChunk(0, 1)) {
            CountrysideDays.LOGGER.warn("Countryside worldgen audit skipped because spawn-adjacent chunk 0,1 is not loaded");
            return;
        }

        BlockState road = surface(level, 0, 18);
        boolean roadReady = road.is(Blocks.PACKED_MUD) || road.is(Blocks.GRAVEL);
        if (roadReady) {
            CountrysideDays.LOGGER.info("Countryside worldgen audit passed: generated road={}", road.getBlock());
        } else {
            CountrysideDays.LOGGER.error(
                    "Countryside worldgen audit failed: expected generated road at 0,18, found {}",
                    road.getBlock()
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
