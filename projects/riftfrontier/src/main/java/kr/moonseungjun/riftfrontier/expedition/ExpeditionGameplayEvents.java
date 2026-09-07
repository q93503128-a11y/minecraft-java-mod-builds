package kr.moonseungjun.riftfrontier.expedition;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Server-side event adapters for field recovery, restart cleanup and terminal failure policy. */
public final class ExpeditionGameplayEvents {
    private ExpeditionGameplayEvents() {}

    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide() != LogicalSide.SERVER || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (ExpeditionGameplayService.tryRecover(player, event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /**
     * Persisted M2 proxy mobs may be loaded long after the restart that invalidated their expedition.
     * Reject them when the entity itself loads instead of scanning every loaded entity or chunk.
     */
    public static void entityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Region01EncounterRuntime.discardIfOrphaned(event.getEntity());
    }

    public static void playerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            ExpeditionGameplayService.failActive(player, ExpeditionRun.EndReason.PLAYER_DEATH, "player death");
        }
    }

    public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ExpeditionGameplayService.failActive(player, ExpeditionRun.EndReason.PLAYER_LOGOUT, "player left during expedition");
        }
    }

    /**
     * Re-entry is evaluated only when the server has a concrete player login event. This keeps the
     * restart/logout UX event-driven and avoids broad world scans or proximity-based ownership guesses.
     */
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ExpeditionGameplayService.reconcilePlayerFieldReentry(player);
        }
    }
}
