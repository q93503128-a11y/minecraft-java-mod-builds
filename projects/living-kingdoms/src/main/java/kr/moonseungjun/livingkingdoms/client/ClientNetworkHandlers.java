package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.network.OpenOriginScreenPayload;
import kr.moonseungjun.livingkingdoms.network.OriginSubmissionResultPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientNetworkHandlers {
    private static OriginSelectionScreen activeOriginScreen;

    private ClientNetworkHandlers() {
    }

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(OpenOriginScreenPayload.TYPE, ClientNetworkHandlers::handleOpenOriginScreen);
        event.register(OriginSubmissionResultPayload.TYPE, ClientNetworkHandlers::handleSubmissionResult);
    }

    private static void handleOpenOriginScreen(OpenOriginScreenPayload payload, IPayloadContext context) {
        activeOriginScreen = new OriginSelectionScreen(payload.schemaVersion());
        Minecraft.getInstance().gui.setScreen(activeOriginScreen);
    }

    private static void handleSubmissionResult(OriginSubmissionResultPayload payload, IPayloadContext context) {
        if (activeOriginScreen != null) {
            activeOriginScreen.handleServerResult(payload.accepted(), payload.message());
            if (payload.accepted()) {
                activeOriginScreen = null;
            }
        }
    }
}
