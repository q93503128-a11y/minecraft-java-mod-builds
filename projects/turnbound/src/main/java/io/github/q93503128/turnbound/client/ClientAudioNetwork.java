package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.AudioCuePayload;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientAudioNetwork {
    private ClientAudioNetwork() {}

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(AudioCuePayload.TYPE, ClientAudioNetwork::handle);
    }

    private static void handle(AudioCuePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientAudioDirector.acceptBatch(payload.cues()));
    }
}
