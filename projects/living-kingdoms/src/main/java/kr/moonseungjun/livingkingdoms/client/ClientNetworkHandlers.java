package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.network.FantasyHudStatePayload;
import kr.moonseungjun.livingkingdoms.network.OpenCodexPayload;
import kr.moonseungjun.livingkingdoms.network.OpenOriginScreenPayload;
import kr.moonseungjun.livingkingdoms.network.OriginSubmissionResultPayload;
import kr.moonseungjun.livingkingdoms.network.RealmBuildProgressPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientNetworkHandlers {
    private static ResponsiveOriginSelectionScreen activeOriginScreen;
    private static RealmLoadingScreen activeLoadingScreen;
    private static RealmBuildProgressPayload latestBuildProgress;
    private static FantasyHudStatePayload latestHudState = new FantasyHudStatePayload(
            0L, 0, 0, "미등록", 100, 100, 100, 100
    );

    private ClientNetworkHandlers() {
    }

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(OpenOriginScreenPayload.TYPE, ClientNetworkHandlers::handleOpenOriginScreen);
        event.register(OriginSubmissionResultPayload.TYPE, ClientNetworkHandlers::handleSubmissionResult);
        event.register(RealmBuildProgressPayload.TYPE, ClientNetworkHandlers::handleBuildProgress);
        event.register(FantasyHudStatePayload.TYPE, ClientNetworkHandlers::handleHudState);
        event.register(OpenCodexPayload.TYPE, ClientNetworkHandlers::handleOpenCodex);
    }

    public static FantasyHudStatePayload hudState() {
        return latestHudState;
    }

    private static void handleOpenOriginScreen(OpenOriginScreenPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (activeOriginScreen == null) {
                activeOriginScreen = new ResponsiveOriginSelectionScreen(payload.schemaVersion());
                activeLoadingScreen = null;
                latestBuildProgress = null;
            }
            minecraft.gui.setScreen(activeOriginScreen);
        });
    }

    private static void handleSubmissionResult(OriginSubmissionResultPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (!payload.accepted()) {
                if (activeOriginScreen != null) activeOriginScreen.handleServerResult(false, payload.message());
                return;
            }

            activeOriginScreen = null;
            activeLoadingScreen = new RealmLoadingScreen(payload.message());
            if (latestBuildProgress != null) activeLoadingScreen.update(latestBuildProgress);
            minecraft.gui.setScreen(activeLoadingScreen);
        });
    }

    private static void handleBuildProgress(RealmBuildProgressPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            latestBuildProgress = payload;
            if (activeLoadingScreen == null) activeLoadingScreen = new RealmLoadingScreen(payload.message());
            activeLoadingScreen.update(payload);
            minecraft.gui.setScreen(activeLoadingScreen);
        });
    }

    private static void handleHudState(FantasyHudStatePayload payload, IPayloadContext context) {
        Minecraft.getInstance().execute(() -> latestHudState = payload);
    }

    private static void handleOpenCodex(OpenCodexPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.gui.setScreen(new RealmCodexScreenV4(payload.page(), payload.snapshot())));
    }
}
