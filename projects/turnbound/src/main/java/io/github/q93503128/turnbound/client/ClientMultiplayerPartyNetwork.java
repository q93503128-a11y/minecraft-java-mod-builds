package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.PartySnapshotPayload;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Receives the server-authoritative player-party projection. */
public final class ClientMultiplayerPartyNetwork {
    private ClientMultiplayerPartyNetwork() {}

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(PartySnapshotPayload.TYPE, ClientMultiplayerPartyNetwork::handle);
    }

    private static void handle(PartySnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientMultiplayerPartyState.update(payload.snapshot()));
    }
}
