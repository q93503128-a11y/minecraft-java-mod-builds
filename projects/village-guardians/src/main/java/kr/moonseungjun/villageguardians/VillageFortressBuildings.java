package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
        removeEmbeddedBells(level, center);
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
        clearLegacyUpgradePile(level, origin, spec);
    }

    static void rebuild(ServerLevel level, BlockPos center, VillageProgressionSystem.Building building) {
        if (building == VillageProgressionSystem.Building.WALLS) {
            return;
        }
        build(level, center, building);
        removeEmbeddedBells(level, center);
        VillageFortressTerrain.restoreCentralBell(level, center);
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
        if (building == VillageProgressionSystem.Building.WALLS) {
            return;
        }
        VillageBuildingCatalog.Spec spec = VillageBuildingCatalog.spec(building);
        BlockPos origin = center.offset(spec.dx(), 0, spec.dz());
        clearLegacyUpgradePile(level, origin, spec);
    }

    static void removeEmbeddedBells(ServerLevel level, BlockPos center) {
        for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
            if (building == VillageProgressionSystem.Building.WALLS) {
                continue;
            }
            VillageBuildingCatalog.Spec spec = VillageBuildingCatalog.spec(building);
            BlockPos origin = center.offset(spec.dx(), 0, spec.dz());
            for (int x = 0; x < spec.width(); x++) {
                for (int z = 0; z < spec.depth(); z++) {
                    for (int y = 0; y <= spec.height(); y++) {
                        BlockPos pos = origin.offset(x, y, z);
                        if (level.getBlockState(pos).is(Blocks.BELL)) {
                            VillageFortressTerrain.set(level, pos, Blocks.AIR);
                        }
                    }
                }
            }
        }
    }

    private static void clearLegacyUpgradePile(
            ServerLevel level,
            BlockPos origin,
            VillageBuildingCatalog.Spec spec) {
        BlockPos entrance = VillageBuildingCatalog.entrance(level, origin, spec);
        Direction sideways = spec.entranceFacing().getClockWise();
        BlockPos oldPlinth = entrance.relative(sideways, 6).below();
        for (int side = -3; side <= 3; side++) {
            for (int y = 0; y <= 3; y++) {
                BlockPos pos = oldPlinth.relative(sideways, side).above(y);
                if (level.getBlockState(pos).is(Blocks.IRON_BLOCK)
                        || level.getBlockState(pos).is(Blocks.GOLD_BLOCK)
                        || level.getBlockState(pos).is(Blocks.QUARTZ_BLOCK)
                        || level.getBlockState(pos).is(Blocks.BOOKSHELF)
                        || level.getBlockState(pos).is(Blocks.BRICKS)
                        || level.getBlockState(pos).is(Blocks.POLISHED_ANDESITE)
                        || level.getBlockState(pos).is(Blocks.CHISELED_STONE_BRICKS)) {
                    VillageFortressTerrain.set(level, pos, Blocks.AIR);
                }
            }
        }
    }
}
