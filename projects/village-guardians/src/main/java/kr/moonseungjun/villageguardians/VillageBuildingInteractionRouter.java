package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Opens only the local function owned by a facility; repair and upgrade remain in the town hall. */
public final class VillageBuildingInteractionRouter {
    private VillageBuildingInteractionRouter() {}

    public static boolean handle(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)
                || !VillageCouncilState.isInsideVillage(player)) return false;
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return false;
        VillageProgressionSystem.Building building = VillageFortressBuildings.buildingAtTerminal(
                level, center, event.getPos());
        if (building == null || building == VillageProgressionSystem.Building.TOWN_HALL) return false;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        VillageUiController.openBuilding(player, building);
        return true;
    }
}
