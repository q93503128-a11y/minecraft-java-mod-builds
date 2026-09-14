package kr.moonseungjun.riftfrontier.expedition;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Server-side event adapters for field recovery, extraction, restart cleanup and terminal failure policy. */
public final class ExpeditionGameplayEvents {
    private ExpeditionGameplayEvents() {}

    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide() != LogicalSide.SERVER || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Close the post-extraction hub loop through world interaction while preserving the existing
        // authoritative provision/start services as the only mutation paths.
        if (ExpeditionHubTerminal.tryUse(player, event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        // Keep the first vertical slice playable in-world: recovery interaction materializes a temporary
        // technical extraction relay, while the existing lifecycle remains the sole extraction authority.
        ExpeditionFieldExtractionRelay.ensurePresent(player);
        if (ExpeditionGameplayService.tryRecover(player, event.getPos())
            || ExpeditionFieldExtractionRelay.tryUse(player, event.getPos())) {
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
     * A genuinely fresh world is then routed into the same technical hub interaction path used after extraction,
     * removing the remaining command-only bootstrap without creating a second expedition lifecycle.
     */
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ExpeditionGameplayService.reconcilePlayerFieldReentry(player);
            ExpeditionHubTerminal.bootstrapFreshWorld(player);
        }
    }
}
