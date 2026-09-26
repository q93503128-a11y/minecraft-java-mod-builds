package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Routes authored interactions without turning the external Drehmal world into a corridor.
 *
 * <p>Legacy session content still blocks survival interactions inside its authored shell. The production external
 * world stays freely traversable: only TURNBOUND-owned physical service NPCs consume right-click/attack events.</p>
 */
public final class FieldInteractionGuard {
    private FieldInteractionGuard(){}

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e){
        if(e.getEntity() instanceof ServerPlayer player
                && ExternalWorldBootstrap.active(player)
                && DrehmalFirstRouteRuntime.insideHub(player)
                && player.level().getBlockEntity(e.getPos()) instanceof Container){
            e.setCancellationResult(InteractionResult.SUCCESS);
            e.setCanceled(true);
            return;
        }
        if(legacyActive(e.getEntity())){e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);}
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem e){
        if(legacyActive(e.getEntity())){e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);}
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract e){
        if(!(e.getEntity() instanceof ServerPlayer player))return;
        if(ExternalWorldBootstrap.active(player)&&ExternalWorldBootstrap.interactEntity(player,e.getTarget())){
            e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);return;
        }
        if(ExternalWorldBootstrap.active(player)
                && DrehmalFirstRouteRuntime.insideHub(player)
                && e.getTarget() instanceof AbstractVillager){
            e.setCancellationResult(InteractionResult.SUCCESS);
            e.setCanceled(true);
            return;
        }
        if(WorldSessionRouter.active(player)){
            WorldSessionRouter.interactEntity(player,e.getTarget());
            e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);
        }
    }

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock e){
        if(legacyActive(e.getEntity()))e.setCanceled(true);
    }

    public static void onAttackEntity(AttackEntityEvent e){
        if(e.getEntity() instanceof ServerPlayer player
                && ExternalWorldBootstrap.active(player)
                && ExternalWorldBootstrap.serviceActor(e.getTarget())){
            e.setCanceled(true);return;
        }
        if(legacyActive(e.getEntity()))e.setCanceled(true);
    }

    private static boolean legacyActive(net.minecraft.world.entity.player.Player p){
        return p instanceof ServerPlayer s&&WorldSessionRouter.active(s);
    }
}
