package kr.countrysidedays.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Optional;

/** Creates the protected public village once; private estates are allocated per player. */
public final class FlatCountrysideBootstrap {
    private FlatCountrysideBootstrap() {
    }

    public static Optional<BlockPos> ensureGenerated(ServerLevel level, BlockPos requestedCenter) {
        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());
        if (data.homesteadOrigin().isPresent()) {
            BlockPos origin = data.homesteadOrigin().orElseThrow();
            PublicVillageExpansionBuilder.ensureExpanded(level, origin);
            return Optional.of(origin);
        }
        if (!CountrysideRegionManager.isFlatWorld(level)) return Optional.empty();

        BlockPos origin = level.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(0, 0, 0)
        );
        StarterHomesteadGenerator.buildPublicVillage(level, origin);
        PublicVillageExpansionBuilder.ensureExpanded(level, origin);
        data.claimHomesteadOrigin(origin);
        return Optional.of(origin);
    }
}
