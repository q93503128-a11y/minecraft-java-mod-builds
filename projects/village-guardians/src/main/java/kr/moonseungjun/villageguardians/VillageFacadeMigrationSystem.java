package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** One-time in-place facade migration; it does not reset durability, items or player builds. */
public final class VillageFacadeMigrationSystem {
    private VillageFacadeMigrationSystem() {}

    public static void ensure(ServerLevel level) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null || level.getBlockState(center.below(7)).is(Blocks.EMERALD_BLOCK)) return;
        for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
            if (building == VillageProgressionSystem.Building.WALLS) continue;
            VillageBuildingCatalog.Spec spec = VillageBuildingCatalog.spec(building);
            BlockPos origin = center.offset(spec.dx(), 0, spec.dz());
            VillageBuildingSignatures.remove(level, center, building);
            if (VillageProgressionSystem.isOperational(building)) {
                VillageBuildingFacadeFix.apply(level, origin, spec, building);
                VillageBuildingSignatures.build(level, center, building);
            }
        }
        VillageFortressTerrain.set(level, center.below(7), Blocks.EMERALD_BLOCK);
    }
}
