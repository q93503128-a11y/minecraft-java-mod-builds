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
    private static final long NANOS_PER_TICK = 50_000_000L;
    private static final long MAX_INTERPOLATION_TICKS = 20L;

    private static ResponsiveOriginSelectionScreen activeOriginScreen;
    private static RealmLoadingScreen activeLoadingScreen;
    private static RealmBuildProgressPayload latestBuildProgress;
    private static FantasyHudStatePayload latestHudState = new FantasyHudStatePayload(
            0L, 0, 0, "미등록", 0L, 100, 100, 100, 100
    );
    private static long realmTimeAnchor;
    private static long realmTimeAnchorNanos;
    private static long lastDisplayedRealmTime;

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

    /**
     * Monotonic display clock derived from one-second server-authoritative samples. Between samples
     * it interpolates at most one second, then waits for the server instead of drifting. Small packet
     * jitter can therefore never make the visible kingdom clock move backwards.
     */
    public static long hudRealmTime() {
        long now = System.nanoTime();
        long projected = projectedRealmTime(now);
        if (projected < lastDisplayedRealmTime) return lastDisplayedRealmTime;
        lastDisplayedRealmTime = projected;
        return projected;
    }

    private static long projectedRealmTime(long nowNanos) {
        if (realmTimeAnchorNanos == 0L) return Math.max(0L, latestHudState.realmTime());
        long elapsed = Math.max(0L, (nowNanos - realmTimeAnchorNanos) / NANOS_PER_TICK);
        return realmTimeAnchor + Math.min(MAX_INTERPOLATION_TICKS, elapsed);
    }

    private static void acceptRealmTime(long serverTime) {
        long now = System.nanoTime();
        long safeServerTime = Math.max(0L, serverTime);
        if (realmTimeAnchorNanos == 0L || safeServerTime + 24_000L < lastDisplayedRealmTime) {
            realmTimeAnchor = safeServerTime;
            realmTimeAnchorNanos = now;
            lastDisplayedRealmTime = safeServerTime;
            return;
        }
        long projected = projectedRealmTime(now);
        realmTimeAnchor = Math.max(safeServerTime, projected);
        realmTimeAnchorNanos = now;
        lastDisplayedRealmTime = Math.max(lastDisplayedRealmTime, realmTimeAnchor);
    }

    private static void resetRealmClock() {
        realmTimeAnchor = 0L;
        realmTimeAnchorNanos = 0L;
        lastDisplayedRealmTime = 0L;
    }

    private static void handleOpenOriginScreen(OpenOriginScreenPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (activeOriginScreen == null) {
                activeOriginScreen = new ResponsiveOriginSelectionScreen(payload.schemaVersion());
                activeLoadingScreen = null;
                latestBuildProgress = null;
                resetRealmClock();
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
        Minecraft.getInstance().execute(() -> {
            latestHudState = payload;
            acceptRealmTime(payload.realmTime());
        });
    }

    private static void handleOpenCodex(OpenCodexPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.gui.setScreen(
                new RealmCodexScreenV5(payload.page(), payload.snapshot())
        ));
    }
}
