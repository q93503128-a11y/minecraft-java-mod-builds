package kr.moonseungjun.riftfrontier.client;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import kr.moonseungjun.riftfrontier.network.BossPresentationPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** Physical-client-only networking handlers. */
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

    /** Entity ids and level game-time epochs may be reused after disconnect; discard ordering watermarks with the connection. */
    @SubscribeEvent
    private static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        BossPresentationClientState.clearAll();
    }
}
