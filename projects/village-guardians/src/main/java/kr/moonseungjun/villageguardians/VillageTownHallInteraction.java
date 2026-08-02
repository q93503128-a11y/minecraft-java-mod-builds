package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class VillageTownHallInteraction {
    private static final long ROLE_MANAGEMENT_DISTANCE_SQUARED = 16L * 16L;

    private VillageTownHallInteraction() {}

    public static boolean handle(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)
                || !level.getBlockState(event.getPos()).is(Blocks.LECTERN)) return false;
        BlockPos expected = terminalPosition(level);
        if (expected == null || !expected.equals(event.getPos())) return false;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        VillageFacadeMigrationSystem.ensure(level);
        VillageUiController.openDashboard(player);
        return true;
    }

    public static boolean isNearTownHall(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        BlockPos expected = terminalPosition(level);
        if (expected == null) return false;
        BlockPos current = player.blockPosition();
        long dx = (long) current.getX() - expected.getX();
        long dy = (long) current.getY() - expected.getY();
        long dz = (long) current.getZ() - expected.getZ();
        return dx * dx + dy * dy + dz * dz <= ROLE_MANAGEMENT_DISTANCE_SQUARED;
    }

    private static BlockPos terminalPosition(ServerLevel level) {
        BlockPos villageCenter = VillageCouncilState.villageCenter().orElse(null);
        if (villageCenter == null) return null;
        return VillageFortressBuildings.terminalPosition(level, villageCenter,
                VillageProgressionSystem.Building.TOWN_HALL);
    }
}
