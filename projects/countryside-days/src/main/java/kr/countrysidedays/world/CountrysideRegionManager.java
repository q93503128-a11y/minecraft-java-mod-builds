package kr.countrysidedays.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.FlatLevelSource;

/** Shared predicates for the generation-time countryside world. */
public final class CountrysideRegionManager {
    private CountrysideRegionManager() {
    }

    public static boolean isFlatWorld(ServerLevel level) {
        return level.getChunkSource().getGenerator() instanceof FlatLevelSource;
    }

    /**
     * The countryside feature is injected into every plains superflat chunk,
     * so the complete flat overworld is treated as the peaceful rural region.
     */
    public static boolean isInsideCountryside(ServerLevel level, BlockPos pos) {
        return level.dimension() == Level.OVERWORLD && isFlatWorld(level);
    }
}
