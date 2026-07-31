package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

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
        boolean placedTemplate = VillageVanillaTemplateBuilder.build(level, origin, groundY, spec);
        if (!placedTemplate) {
            VillageStructureShell.build(level, origin, groundY, spec);
        }
        VillageBuildingCatalog.furnish(level, origin, spec, building);
    }

    static void rebuild(ServerLevel level, BlockPos center, VillageProgressionSystem.Building building) {
        if (building == VillageProgressionSystem.Building.WALLS) {
            return;
        }
        build(level, center, building);
    }

    static void remove(ServerLevel level, BlockPos center, VillageProgressionSystem.Building building) {
        if (building == VillageProgressionSystem.Building.WALLS) {
            return;
        }
        VillageStructureShell.ruin(level, center, VillageBuildingCatalog.spec(building));
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

    static void applyUpgradeVisual(
            ServerLevel level,
            BlockPos center,
            VillageProgressionSystem.Building building,
            int upgradeLevel) {
        if (upgradeLevel <= 0 || building == VillageProgressionSystem.Building.WALLS) {
            return;
        }
        VillageBuildingCatalog.Spec spec = VillageBuildingCatalog.spec(building);
        BlockPos origin = center.offset(spec.dx(), 0, spec.dz());
        BlockPos entrance = VillageBuildingCatalog.entrance(level, origin, spec);
        Direction sideways = spec.entranceFacing().getClockWise();
        Block decoration = switch (building) {
            case SMITHY -> Blocks.IRON_BLOCK;
            case SKILL_HALL -> Blocks.BOOKSHELF;
            case INFIRMARY -> Blocks.QUARTZ_BLOCK;
            case STOREHOUSE -> Blocks.GOLD_BLOCK;
            case BARRACKS -> Blocks.BRICKS;
            case TOWN_HALL -> Blocks.CHISELED_STONE_BRICKS;
            case WALLS -> Blocks.STONE_BRICKS;
        };

        BlockPos plinth = entrance.relative(sideways, 6).below();
        for (int x = -1; x <= 1; x++) {
            VillageFortressTerrain.set(level, plinth.relative(sideways, x), Blocks.POLISHED_ANDESITE);
        }
        for (int i = 0; i < Math.min(5, upgradeLevel); i++) {
            VillageFortressTerrain.set(
                    level,
                    plinth.relative(sideways, i - 2).above(),
                    decoration);
        }
    }
}
