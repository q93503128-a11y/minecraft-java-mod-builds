package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Keeps vanilla survival interactions out of both Radia and authored field progression. */
public final class FieldInteractionGuard {
    private FieldInteractionGuard(){}
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e){if(active(e.getEntity())){e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);}}
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem e){if(active(e.getEntity())){e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);}}
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract e){if(e.getEntity() instanceof ServerPlayer p&&WorldSessionRouter.active(p)){WorldSessionRouter.interactEntity(p,e.getTarget());e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);}}
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock e){if(active(e.getEntity()))e.setCanceled(true);}
    public static void onAttackEntity(AttackEntityEvent e){if(active(e.getEntity()))e.setCanceled(true);}
    private static boolean active(net.minecraft.world.entity.player.Player p){return p instanceof ServerPlayer s&&WorldSessionRouter.active(s);}
}
