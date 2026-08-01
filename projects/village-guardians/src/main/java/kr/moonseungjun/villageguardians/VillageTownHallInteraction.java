package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
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
                || player.level().isClientSide()
                || !player.level().getBlockState(event.getPos()).is(Blocks.LECTERN)) {
            return false;
        }
        BlockPos hallCenter = VillageWorldSystem.buildingCenter(VillageProgressionSystem.Building.TOWN_HALL);
        if (event.getPos().distSqr(hallCenter) > 36.0 * 36.0) {
            return false;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        VillageUiService.openDashboard(player);
        return true;
    }
}
