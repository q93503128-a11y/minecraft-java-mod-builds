package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = VillageGuardians.MOD_ID)
public final class VillageTownHallSystem {
    private static final long TERMINAL_RANGE_SQUARED = 34L * 34L;

    private VillageTownHallSystem() {
    }

    @SubscribeEvent
    public static void onTownHallInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || player.level().isClientSide()
                || !player.level().getBlockState(event.getPos()).is(Blocks.LECTERN)) {
            return;
        }

        BlockPos hallCenter = VillageWorldSystem.buildingCenter(
                VillageProgressionSystem.Building.TOWN_HALL);
        if (distanceSquared(event.getPos(), hallCenter) > TERMINAL_RANGE_SQUARED) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        VillageUiService.openBuilding(player, VillageProgressionSystem.Building.TOWN_HALL);
    }

    private static long distanceSquared(BlockPos first, BlockPos second) {
        long dx = (long) first.getX() - second.getX();
        long dy = (long) first.getY() - second.getY();
        long dz = (long) first.getZ() - second.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
