package kr.countrysidedays.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Optional;

/** Creates the shared village at the centre of a generated superflat countryside. */
public final class FlatCountrysideBootstrap {
    private FlatCountrysideBootstrap() {
    }

    public static Optional<BlockPos> ensureGenerated(ServerLevel level, BlockPos requestedCenter) {
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());
        if (data.homesteadOrigin().isPresent()) {
            return data.homesteadOrigin();
        }
        if (!CountrysideRegionManager.isFlatWorld(level)) {
            return Optional.empty();
        }

        BlockPos origin = level.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(0, 0, 0)
        );
        StarterHomesteadGenerator.buildCompleteVillage(level, origin);
        data.claimHomesteadOrigin(origin);
        data.claimRestaurantAnchor(StarterHomesteadGenerator.kitchenCounterPos(origin));
        return Optional.of(origin);
    }
}
