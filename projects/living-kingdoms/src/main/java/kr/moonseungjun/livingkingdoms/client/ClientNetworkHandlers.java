package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.network.OpenOriginScreenPayload;
import kr.moonseungjun.livingkingdoms.network.OriginSubmissionResultPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientNetworkHandlers {
    private ClientNetworkHandlers() {
    }

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(OpenOriginScreenPayload.TYPE, ClientNetworkHandlers::handleOpenOriginScreen);
        event.register(OriginSubmissionResultPayload.TYPE, ClientNetworkHandlers::handleSubmissionResult);
    }

    private static void handleOpenOriginScreen(OpenOriginScreenPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof OriginSelectionScreen)) {
            minecraft.setScreen(new OriginSelectionScreen(payload.schemaVersion()));
        }
    }

    private static void handleSubmissionResult(OriginSubmissionResultPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof OriginSelectionScreen screen) {
            screen.handleServerResult(payload.accepted(), payload.message());
        }
    }
}
