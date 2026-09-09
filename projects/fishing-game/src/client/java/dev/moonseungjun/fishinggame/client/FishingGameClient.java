package dev.moonseungjun.fishinggame.client;

import dev.moonseungjun.fishinggame.network.ReelInputPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class FishingGameClient implements ClientModInitializer {
    private static final int HEARTBEAT_TICKS = 5;

    private boolean lastHeld;
    private int heartbeatTicks;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean fishing = client.player != null && client.player.fishing != null;
            if (!fishing) {
                lastHeld = false;
                heartbeatTicks = 0;
                return;
            }

            boolean held = client.options.keyUse.isDown();
            heartbeatTicks++;

            if (held != lastHeld || heartbeatTicks >= HEARTBEAT_TICKS) {
                if (ClientPlayNetworking.canSend(ReelInputPayload.TYPE)) {
                    ClientPlayNetworking.send(new ReelInputPayload(held));
                }
                lastHeld = held;
                heartbeatTicks = 0;
            }
        });
    }
}
