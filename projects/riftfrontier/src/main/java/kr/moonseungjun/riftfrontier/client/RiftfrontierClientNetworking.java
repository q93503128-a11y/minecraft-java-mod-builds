package kr.moonseungjun.riftfrontier.client;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import kr.moonseungjun.riftfrontier.network.BossPresentationPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/** Physical-client-only networking and presentation lifecycle handlers. */
@EventBusSubscriber(modid = Riftfrontier.MOD_ID, value = Dist.CLIENT)
public final class RiftfrontierClientNetworking {
    private RiftfrontierClientNetworking() {}

    @SubscribeEvent
    private static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(
            BossPresentationPayload.TYPE,
            (payload, context) -> BossPresentationClientState.accept(payload.state())
        );
    }

    /**
     * Retire one exact logical actor as soon as client tracking ends instead of retaining its watermark until logout.
     * Dist.CLIENT also exists for an integrated server process, so the logical-side guard is required.
     */
    @SubscribeEvent
    private static void entityLeavingLevel(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) return;
        BossPresentationClientState.forgetActor(event.getEntity().getId(), event.getEntity().getUUID());
    }

    /** Entity ids and level game-time epochs may be reused after disconnect; discard ordering watermarks with the connection. */
    @SubscribeEvent
    private static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        BossPresentationClientState.clearAll();
    }
}
