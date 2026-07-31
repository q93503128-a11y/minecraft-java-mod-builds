package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

final class VillageStructureShell {
    private VillageStructureShell() {}

    static void build(ServerLevel level, BlockPos origin, int groundY, VillageBuildingCatalog.Spec spec) {
        VillageSimpleBuildingBuilder.build(level, origin, groundY, spec);
    }

    static void clear(ServerLevel level, BlockPos center, VillageBuildingCatalog.Spec spec) {
        VillageSimpleBuildingBuilder.clear(level, center, spec);
    }
}
