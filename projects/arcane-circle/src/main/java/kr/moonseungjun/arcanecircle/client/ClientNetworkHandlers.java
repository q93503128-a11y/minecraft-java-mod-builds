package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.MeteorBarragePattern;
import kr.moonseungjun.arcanecircle.network.GrimoireSnapshotPayload;
import kr.moonseungjun.arcanecircle.network.WorldMagicPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientNetworkHandlers {
    private ClientNetworkHandlers() {}

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(GrimoireSnapshotPayload.TYPE, ClientNetworkHandlers::handleSnapshot);
        event.register(WorldMagicPayload.TYPE, ClientNetworkHandlers::handleWorldMagic);
    }

    private static void handleWorldMagic(WorldMagicPayload payload, IPayloadContext context) {
        MeteorBarragePattern.rememberPayload(payload.state());
        WorldMagicTracker.accept(payload);
    }

    private static void handleSnapshot(GrimoireSnapshotPayload payload, IPayloadContext context) {
        ArcaneClientState.update(payload.snapshot());
        if ("sync".equals(payload.page())) return;
        if ("atlas".equals(payload.page()) || "academy".equals(payload.page())) {
            Minecraft.getInstance().gui.setScreen(new PrimaryGrimoireScreen(payload.page()));
        } else {
            Minecraft.getInstance().gui.setScreen(new GrimoireScreen(payload.page()));
        }
    }
}
