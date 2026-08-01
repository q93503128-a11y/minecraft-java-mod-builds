package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class VillageLocationRules {
    private static final long TERMINAL_DISTANCE_SQUARED = 16L * 16L;

    private VillageLocationRules() {}

    public static boolean isNearTownHall(ServerPlayer player) {
        return isNear(player, VillageProgressionSystem.Building.TOWN_HALL);
    }

    public static boolean isNearSkillHall(ServerPlayer player) {
        return isNear(player, VillageProgressionSystem.Building.SKILL_HALL);
    }

    public static boolean isNear(ServerPlayer player, VillageProgressionSystem.Building building) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) {
            return false;
        }
        BlockPos terminal = VillageFortressBuildings.terminalPosition(level, center, building);
        if (terminal == null) {
            return false;
        }
        BlockPos current = player.blockPosition();
        long dx = (long) current.getX() - terminal.getX();
        long dy = (long) current.getY() - terminal.getY();
        long dz = (long) current.getZ() - terminal.getZ();
        return dx * dx + dy * dy + dz * dz <= TERMINAL_DISTANCE_SQUARED;
    }
}
