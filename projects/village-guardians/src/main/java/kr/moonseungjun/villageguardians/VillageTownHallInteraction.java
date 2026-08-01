package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class VillageTownHallInteraction {
    private VillageTownHallInteraction() {
    }

    public static boolean handle(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)
                || !level.getBlockState(event.getPos()).is(Blocks.LECTERN)) {
            return false;
        }
        BlockPos villageCenter = VillageCouncilState.villageCenter().orElse(null);
        if (villageCenter == null) {
            return false;
        }
        BlockPos expected = VillageFortressBuildings.terminalPosition(
                level,
                villageCenter,
                VillageProgressionSystem.Building.TOWN_HALL);
        if (!expected.equals(event.getPos())) {
            return false;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        VillageUiService.openDashboard(player);
        return true;
    }
}
