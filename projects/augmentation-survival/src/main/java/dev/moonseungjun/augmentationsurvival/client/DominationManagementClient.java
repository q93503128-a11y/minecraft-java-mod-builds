package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.client.screen.DominationManagementScreen;
import dev.moonseungjun.augmentationsurvival.network.DominationManagementSnapshotPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Opens or refreshes the server-owned dominated companion roster. */
public final class DominationManagementClient {
    private DominationManagementClient() { }
    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(DominationManagementSnapshotPayload.TYPE, (payload, context) -> {
            var client = context.client();
            if (client.gui.screen() instanceof DominationManagementScreen screen) {
                screen.applySnapshot(payload);
            } else if (payload.openScreen() && client.player != null) {
                client.gui.setScreen(new DominationManagementScreen(payload));
            }
        });
    }
}
