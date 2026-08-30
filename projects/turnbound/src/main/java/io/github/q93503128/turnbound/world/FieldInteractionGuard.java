package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Prevents vanilla survival interaction from becoming the field progression loop while allowing TURNBOUND NPC interaction. */
public final class FieldInteractionGuard {
    private FieldInteractionGuard() {}

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (inField(event.getEntity())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (inField(event.getEntity())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && FieldSessionManager.active(player)) {
            FieldSessionManager.interactEntity(player, event.getTarget());
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (inField(event.getEntity())) event.setCanceled(true);
    }

    public static void onAttackEntity(AttackEntityEvent event) {
        if (inField(event.getEntity())) event.setCanceled(true);
    }

    private static boolean inField(net.minecraft.world.entity.player.Player player) {
        return player instanceof ServerPlayer serverPlayer && FieldSessionManager.active(serverPlayer);
    }
}
