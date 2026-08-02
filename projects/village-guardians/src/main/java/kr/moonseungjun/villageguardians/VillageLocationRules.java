package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.StairBlock;

public final class VillageLocationRules {
    private static final long TERMINAL_DISTANCE_SQUARED = 16L * 16L;

    private VillageLocationRules() {}

    public static boolean isNearTownHall(ServerPlayer player) {
        return isNear(player, VillageProgressionSystem.Building.TOWN_HALL);
    }

    public static boolean isNearSkillHall(ServerPlayer player) {
        return isNear(player, VillageProgressionSystem.Building.SKILL_HALL);
    }

    public static boolean isEnemyIgnoredElevation(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        BlockPos pos = player.blockPosition();
        if (level.getBlockState(pos).getBlock() instanceof StairBlock
                || level.getBlockState(pos.below()).getBlock() instanceof StairBlock) return true;
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null || !VillageCouncilState.isInsideVillage(player)) return false;
        int dx = Math.abs(pos.getX() - center.getX());
        int dz = Math.abs(pos.getZ() - center.getZ());
        int relativeY = pos.getY() - center.getY();
        boolean wallBand = Math.abs(dx - VillageWorldSystem.FORTRESS_RADIUS) <= 8
                || Math.abs(dz - VillageWorldSystem.FORTRESS_RADIUS) <= 8;
        return wallBand && relativeY >= 6;
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
