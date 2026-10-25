package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.client.screen.SpatialLinkScreen;
import dev.moonseungjun.augmentationsurvival.network.SpatialLinkSnapshotPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Opens or refreshes the spatial-link screen from a server-authoritative snapshot. */
public final class SpatialLinkClientNetworking {
    private SpatialLinkClientNetworking() { }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(SpatialLinkSnapshotPayload.TYPE, (payload, context) -> {
            var client = context.client();
            if (client.gui.screen() instanceof SpatialLinkScreen screen) {
                screen.applySnapshot(payload.gates());
            } else {
                client.gui.setScreen(new SpatialLinkScreen(payload.gates()));
            }
        });
    }
}
