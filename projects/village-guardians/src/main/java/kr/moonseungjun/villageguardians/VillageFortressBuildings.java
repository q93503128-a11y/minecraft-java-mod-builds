package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

final class VillageFortressBuildings {
    private VillageFortressBuildings() {
    }

    static void buildAll(ServerLevel level, BlockPos center) {
        for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
            if (building != VillageProgressionSystem.Building.WALLS) {
                build(level, center, building);
            }
        }
    }

    static void build(ServerLevel level, BlockPos center, VillageProgressionSystem.Building building) {
        if (building == VillageProgressionSystem.Building.WALLS) {
            return;
        }
        VillageBuildingCatalog.Spec spec = VillageBuildingCatalog.spec(building);
        BlockPos origin = center.offset(spec.dx(), 0, spec.dz());
        int groundY = center.getY() - 1;

        VillageStructureShell.clear(level, center, spec);
        VillageStructureShell.build(level, origin, groundY, spec);
        VillageBuildingCatalog.furnish(level, origin, spec, building);
        VillageBuildingEnhancements.apply(level, center, building);
    }

    static void rebuild(ServerLevel level, BlockPos center, VillageProgressionSystem.Building building) {
        if (building != VillageProgressionSystem.Building.WALLS) {
            build(level, center, building);
        }
    }

    static void remove(ServerLevel level, BlockPos center, VillageProgressionSystem.Building building) {
        if (building != VillageProgressionSystem.Building.WALLS) {
            VillageStructureShell.ruin(level, center, VillageBuildingCatalog.spec(building));
        }
    }

    static BlockPos center(BlockPos villageCenter, VillageProgressionSystem.Building building) {
        if (building == VillageProgressionSystem.Building.WALLS) {
            return villageCenter.offset(0, 0, -VillageWorldSystem.FORTRESS_RADIUS + 3);
        }
        VillageBuildingCatalog.Spec spec = VillageBuildingCatalog.spec(building);
        return villageCenter.offset(
                spec.dx() + spec.width() / 2,
                1,
                spec.dz() + spec.depth() / 2);
    }

    static BlockPos terminalPosition(
            ServerLevel level,
            BlockPos villageCenter,
            VillageProgressionSystem.Building building) {
        return VillageBuildingEnhancements.terminalPosition(level, villageCenter, building);
    }

    static VillageProgressionSystem.Building buildingAtTerminal(
            ServerLevel level,
            BlockPos villageCenter,
            BlockPos clicked) {
        return VillageBuildingEnhancements.buildingAtTerminal(level, villageCenter, clicked);
    }

    static void applyUpgradeVisual(
            ServerLevel level,
            BlockPos center,
            VillageProgressionSystem.Building building,
            int upgradeLevel) {
        // Upgrades are represented in the management HUD instead of loose block piles.
    }
}
