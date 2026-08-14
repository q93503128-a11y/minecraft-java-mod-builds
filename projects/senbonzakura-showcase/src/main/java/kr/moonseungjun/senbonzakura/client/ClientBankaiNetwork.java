package kr.moonseungjun.senbonzakura.client;

import kr.moonseungjun.senbonzakura.network.BankaiVisualPayload;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientBankaiNetwork {
    private ClientBankaiNetwork() {}

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(BankaiVisualPayload.TYPE, ClientBankaiNetwork::handleVisual);
    }

    private static void handleVisual(BankaiVisualPayload payload, IPayloadContext context) {
        BankaiWorldRenderer.accept(payload);
        ExternalShockwaveVfx.accept(payload);
    }
}
