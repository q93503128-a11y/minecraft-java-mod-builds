package io.github.q93503128.turnbound;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Receive world-owned presentation state; client never decides authoritative rules. */
public final class TurnboundClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(Turnbound.WORLD_MODE_PACKET,
                (client, handler, payload, sender) -> {
                    boolean worldEnabled = payload.readBoolean();
                    client.execute(() -> TurnboundClientWorldState.setEnabled(worldEnabled));
                });
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> TurnboundClientWorldState.setEnabled(false));
    }
}
