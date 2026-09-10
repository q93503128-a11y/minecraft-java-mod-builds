package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.network.WorldEncounterAnchorNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Bridges fixed-world Interaction markers to validated RegionDefinition encounter locators. */
public final class WorldEncounterAnchorHooks {
    public void register(IEventBus bus) {
        if (bus == null) throw new IllegalArgumentException("bus required");
        bus.addListener(this::onEntityInteract);
    }

    private void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        String dimensionId = player.level().dimension().identifier().toString();
        WorldEncounterAnchorResolver.Resolved resolved = WorldEncounterAnchorResolver.resolveEntity(
                TurnboundRe.DEFINITIONS.snapshot().registry(), event.getTarget(), dimensionId).orElse(null);
        if (resolved == null) return;
        if (!WorldEncounterAnchorResolver.withinConfirmRange(player.distanceToSqr(event.getTarget()))) return;

        WorldEncounterAnchorNetwork.sendPreview(player, event.getTarget(), resolved);
    }
}
