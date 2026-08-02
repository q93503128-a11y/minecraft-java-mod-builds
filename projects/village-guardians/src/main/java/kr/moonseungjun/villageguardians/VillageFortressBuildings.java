package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

final class VillageFortressBuildings {
    private VillageFortressBuildings() {}

    static void buildAll(ServerLevel level, BlockPos center) {
        for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
            if (building != VillageProgressionSystem.Building.WALLS) build(level, center, building);
        }
    }

    static void build(ServerLevel level, BlockPos center, VillageProgressionSystem.Building building) {
        if (building == VillageProgressionSystem.Building.WALLS) return;
        VillageBuildingCatalog.Spec spec = VillageBuildingCatalog.spec(building);
        BlockPos origin = center.offset(spec.dx(), 0, spec.dz());
        int groundY = center.getY() - 1;
        VillageStructureShell.clear(level, center, spec);
        VillageStructureShell.build(level, origin, groundY, spec);
        VillageBuildingCatalog.furnish(level, origin, spec, building);
        VillageBuildingEnhancements.apply(level, center, building);
        if (building == VillageProgressionSystem.Building.TOWN_HALL) {
            VillageTownHallAccessFix.apply(level, center);
        }
        VillageBuildingFacadeFix.apply(level, origin, spec, building);
    }

    static void rebuild(ServerLevel level, BlockPos center, VillageProgressionSystem.Building building) {
        if (building != VillageProgressionSystem.Building.WALLS) build(level, center, building);
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
        return villageCenter.offset(spec.dx() + spec.width() / 2, 1, spec.dz() + spec.depth() / 2);
    }

    static BlockPos attackPoint(BlockPos villageCenter, VillageProgressionSystem.Building building, BlockPos enemy) {
        if (building == VillageProgressionSystem.Building.WALLS) {
            int x = clamp(enemy.getX(), villageCenter.getX() - 13, villageCenter.getX() + 13);
            return new BlockPos(x, villageCenter.getY(), villageCenter.getZ() - VillageWorldSystem.FORTRESS_RADIUS + 5);
        }
        Bounds bounds = bounds(villageCenter, building);
        int targetX = clamp(enemy.getX(), bounds.x0(), bounds.x1());
        int targetZ = clamp(enemy.getZ(), bounds.z0(), bounds.z1());
        if (enemy.getX() < bounds.x0()) targetX = bounds.x0() - 1;
        else if (enemy.getX() > bounds.x1()) targetX = bounds.x1() + 1;
        if (enemy.getZ() < bounds.z0()) targetZ = bounds.z0() - 1;
        else if (enemy.getZ() > bounds.z1()) targetZ = bounds.z1() + 1;
        if (enemy.getX() >= bounds.x0() && enemy.getX() <= bounds.x1()
                && enemy.getZ() >= bounds.z0() && enemy.getZ() <= bounds.z1()) {
            int west = enemy.getX() - bounds.x0();
            int east = bounds.x1() - enemy.getX();
            int north = enemy.getZ() - bounds.z0();
            int south = bounds.z1() - enemy.getZ();
            int nearest = Math.min(Math.min(west, east), Math.min(north, south));
            if (nearest == west) targetX = bounds.x0() - 1;
            else if (nearest == east) targetX = bounds.x1() + 1;
            else if (nearest == north) targetZ = bounds.z0() - 1;
            else targetZ = bounds.z1() + 1;
        }
        return new BlockPos(targetX, villageCenter.getY(), targetZ);
    }

    static boolean isTouchingStructure(BlockPos villageCenter, VillageProgressionSystem.Building building, BlockPos enemy) {
        if (building == VillageProgressionSystem.Building.WALLS) {
            return Math.abs(enemy.getZ() - (villageCenter.getZ() - VillageWorldSystem.FORTRESS_RADIUS + 3)) <= 3
                    && Math.abs(enemy.getX() - villageCenter.getX()) <= 16;
        }
        Bounds bounds = bounds(villageCenter, building);
        return enemy.getX() >= bounds.x0() - 2 && enemy.getX() <= bounds.x1() + 2
                && enemy.getZ() >= bounds.z0() - 2 && enemy.getZ() <= bounds.z1() + 2
                && enemy.getY() >= villageCenter.getY() - 2
                && enemy.getY() <= villageCenter.getY() + bounds.height() + 3;
    }

    static long distanceSquaredToStructure(BlockPos villageCenter, VillageProgressionSystem.Building building, BlockPos pos) {
        if (building == VillageProgressionSystem.Building.WALLS) {
            return horizontalDistanceSquared(pos, attackPoint(villageCenter, building, pos));
        }
        Bounds bounds = bounds(villageCenter, building);
        int x = clamp(pos.getX(), bounds.x0(), bounds.x1());
        int z = clamp(pos.getZ(), bounds.z0(), bounds.z1());
        long dx = (long) pos.getX() - x;
        long dz = (long) pos.getZ() - z;
        return dx * dx + dz * dz;
    }

    static BlockPos terminalPosition(ServerLevel level, BlockPos villageCenter, VillageProgressionSystem.Building building) {
        return VillageBuildingEnhancements.terminalPosition(level, villageCenter, building);
    }

    static VillageProgressionSystem.Building buildingAtTerminal(ServerLevel level, BlockPos villageCenter, BlockPos clicked) {
        return VillageBuildingEnhancements.buildingAtTerminal(level, villageCenter, clicked);
    }

    static void applyUpgradeVisual(ServerLevel level, BlockPos center,
                                   VillageProgressionSystem.Building building, int upgradeLevel) {
        if (building == VillageProgressionSystem.Building.WALLS) VillageDefenseTowerBuilder.build(level, center);
    }

    private static Bounds bounds(BlockPos center, VillageProgressionSystem.Building building) {
        VillageBuildingCatalog.Spec spec = VillageBuildingCatalog.spec(building);
        int x0 = center.getX() + spec.dx();
        int z0 = center.getZ() + spec.dz();
        return new Bounds(x0, z0, x0 + spec.width() - 1, z0 + spec.depth() - 1, spec.height());
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static long horizontalDistanceSquared(BlockPos first, BlockPos second) {
        long dx = (long) first.getX() - second.getX();
        long dz = (long) first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    private record Bounds(int x0, int z0, int x1, int z1, int height) {}
}
