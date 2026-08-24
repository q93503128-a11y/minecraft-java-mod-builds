package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.network.FantasyHudStatePayload;
import kr.moonseungjun.livingkingdoms.network.OpenCodexPayload;
import kr.moonseungjun.livingkingdoms.network.OpenOriginScreenPayload;
import kr.moonseungjun.livingkingdoms.network.OriginSubmissionResultPayload;
import kr.moonseungjun.livingkingdoms.network.RealmBuildProgressPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientNetworkHandlers {
    private static ResponsiveOriginSelectionScreen activeOriginScreen;
    private static RealmLoadingScreen activeLoadingScreen;
    private static RealmBuildProgressPayload latestBuildProgress;
    private static FantasyHudStatePayload latestHudState = new FantasyHudStatePayload(
            0L, 0, 0, "미등록", 100, 100, 100, 100, 0L
    );
    private static ClientLevel clockLevel;
    private static long latestServerRealmTime;
    private static long stableRealmTime;
    private static boolean clockReady;

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
     * Returns a monotonic client presentation of the server kingdom clock. It may wait briefly for
     * a lagging server, but it never rewinds because a delayed level-time packet arrived.
     */
    public static long realmTime() {
        Minecraft minecraft = Minecraft.getInstance();
        if (clockReady && minecraft.level == clockLevel) return stableRealmTime;
        return minecraft.level == null ? 0L : Math.max(0L, minecraft.level.getGameTime());
    }

    /** Extrapolates at most one second beyond the latest authoritative server sample. */
    public static void tickRealmClock() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != clockLevel) {
            clockLevel = minecraft.level;
            latestServerRealmTime = 0L;
            stableRealmTime = 0L;
            clockReady = false;
            return;
        }
        if (!clockReady) return;
        stableRealmTime = advancePresentedTime(stableRealmTime, latestServerRealmTime);
    }

    static long mergeServerTime(long presented, long serverSample) {
        return Math.max(Math.max(0L, presented), Math.max(0L, serverSample));
    }

    static long advancePresentedTime(long presented, long latestServerSample) {
        long safePresented = Math.max(0L, presented);
        long ceiling = Math.max(0L, latestServerSample) + 20L;
        return safePresented < ceiling ? safePresented + 1L : safePresented;
    }

    static boolean kingdomClockRegressionPassForTest() {
        long presented = 10_000L;
        presented = mergeServerTime(presented, 8_800L);
        if (presented != 10_000L) return false;
        presented = mergeServerTime(presented, 10_100L);
        if (presented != 10_100L) return false;
        for (int i = 0; i < 80; i++) presented = advancePresentedTime(presented, 10_100L);
        return presented == 10_120L
                && mergeServerTime(presented, 9_000L) == 10_120L;
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
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            latestHudState = payload;
            if (minecraft.level != clockLevel) {
                clockLevel = minecraft.level;
                clockReady = false;
            }
            latestServerRealmTime = payload.realmGameTime();
            if (!clockReady) {
                stableRealmTime = latestServerRealmTime;
                clockReady = true;
            } else {
                stableRealmTime = mergeServerTime(stableRealmTime, latestServerRealmTime);
            }
        });
    }

    private static void handleOpenCodex(OpenCodexPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.gui.setScreen(
                new RealmCodexScreenV5(payload.page(), payload.snapshot())
        ));
    }
}
