package kr.moonseungjun.titanbreak.client;

import kr.moonseungjun.titanbreak.network.StatusPayload;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientNetworkHandlers {
    private ClientNetworkHandlers() {}

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(StatusPayload.TYPE, ClientNetworkHandlers::handleStatus);
    }

    private static void handleStatus(StatusPayload payload, IPayloadContext context) {
        TitanClientState.update(payload.snapshot());
    }
}
