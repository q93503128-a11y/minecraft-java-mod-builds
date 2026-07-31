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

        BlockState marker = surface(level, 0, 0);
        if (marker.is(Blocks.DANDELION)) {
            CountrysideDays.LOGGER.info(
                    "Countryside worldgen audit passed: deterministic spawn marker={}",
                    marker.getBlock()
            );
        } else {
            CountrysideDays.LOGGER.error(
                    "Countryside worldgen audit failed: expected dandelion marker at 0,0, found {}",
                    marker.getBlock()
            );
        }
    }

    private static BlockState surface(ServerLevel level, int x, int z) {
        BlockPos air = level.getHeightmapPos(
                Heightmap.Types.WORLD_SURFACE,
                new BlockPos(x, 0, z)
        );
        return level.getBlockState(air.below());
    }
}
