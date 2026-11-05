package io.github.q93503128.turnbound.session;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class BattleInteractionGuard {
    private BattleInteractionGuard() {}

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (inBattle(event.getEntity())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (inBattle(event.getEntity())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (inBattle(event.getEntity())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (inBattle(event.getEntity())) event.setCanceled(true);
    }

    public static void onAttackEntity(AttackEntityEvent event) {
        if (inBattle(event.getEntity())) event.setCanceled(true);
    }

    private static boolean inBattle(net.minecraft.world.entity.player.Player player) {
        return player instanceof ServerPlayer serverPlayer && BattleSessionManager.active(serverPlayer);
    }
}
