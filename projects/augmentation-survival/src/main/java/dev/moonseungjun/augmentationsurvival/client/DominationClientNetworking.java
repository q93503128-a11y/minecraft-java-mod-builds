package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.client.screen.DominationManagementScreen;
import dev.moonseungjun.augmentationsurvival.network.DominationManageRequestPayload;
import dev.moonseungjun.augmentationsurvival.network.DominationRosterPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Refreshes the small roster while its management screen is open. */
public final class DominationClientNetworking {
    private static int refreshTicks;
    private DominationClientNetworking() { }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(
                DominationRosterPayload.TYPE,
                (payload, context) -> ClientDominationState.apply(payload)
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientDominationState.clear();
            refreshTicks = 0;
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || !(client.gui.screen() instanceof DominationManagementScreen)) {
                refreshTicks = 0;
                return;
            }
            if (++refreshTicks >= 20) {
                refreshTicks = 0;
                ClientPlayNetworking.send(new DominationManageRequestPayload(
                        DominationManageRequestPayload.OPEN, ""));
            }
        });
    }
}
