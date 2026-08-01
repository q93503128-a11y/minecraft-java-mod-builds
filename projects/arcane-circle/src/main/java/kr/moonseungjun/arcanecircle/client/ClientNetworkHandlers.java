package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.network.GrimoireSnapshotPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientNetworkHandlers {
    private ClientNetworkHandlers() {}

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(GrimoireSnapshotPayload.TYPE, ClientNetworkHandlers::handleSnapshot);
    }

    private static void handleSnapshot(GrimoireSnapshotPayload payload, IPayloadContext context) {
        ArcaneClientState.update(payload.snapshot());
        if (!"sync".equals(payload.page())) {
            Minecraft.getInstance().gui.setScreen(new GrimoireScreen(payload.page()));
        }
    }
}
