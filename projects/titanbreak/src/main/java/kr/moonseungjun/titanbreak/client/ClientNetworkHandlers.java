package kr.moonseungjun.titanbreak.client;

import kr.moonseungjun.titanbreak.network.StationOpenPayload;
import kr.moonseungjun.titanbreak.network.StatusPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientNetworkHandlers {
    private ClientNetworkHandlers() {}

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(StatusPayload.TYPE, ClientNetworkHandlers::handleStatus);
        event.register(StationOpenPayload.TYPE, ClientNetworkHandlers::handleStationOpen);
    }

    private static void handleStatus(StatusPayload payload, IPayloadContext context) {
        TitanClientState.update(payload.snapshot());
    }

    private static void handleStationOpen(StationOpenPayload payload, IPayloadContext context) {
        String[] parts = payload.station().split("\\|", 2);
        if (parts.length < 2) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (parts[0].equals("fabricator")) {
            minecraft.gui.setScreen(new FabricatorScreen(payload.station()));
        } else if (parts[0].equals("surgery")) {
            minecraft.gui.setScreen(new SurgeryScreen(payload.station()));
        }
    }
}
