package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.TurnboundRe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Bridges physical fast-travel Interaction entities to the server-authoritative travel service. */
public final class WorldFastTravelHooks {
    private final WorldFastTravelService service;
    private final FirstExpeditionQuestService firstExpedition;

    public WorldFastTravelHooks(WorldFastTravelService service) {
        this(service, null);
    }

    public WorldFastTravelHooks(WorldFastTravelService service, FirstExpeditionQuestService firstExpedition) {
        if (service == null) throw new IllegalArgumentException("service required");
        this.service = service;
        this.firstExpedition = firstExpedition;
    }

    public void register(IEventBus bus) {
        if (bus == null) throw new IllegalArgumentException("bus required");
        bus.addListener(this::onEntityInteract);
    }

    private void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        String dimensionId = player.level().dimension().identifier().toString();
        if (WorldFastTravelResolver.resolveEntity(
                TurnboundRe.DEFINITIONS.snapshot().registry(), event.getTarget(), dimensionId).isEmpty()) return;
        WorldFastTravelService.Result result = service.use(player, event.getTarget());
        if (firstExpedition != null) firstExpedition.onFastTravelResult(player, result);
    }
}
