package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.network.FieldCommandPayload;
import io.github.q93503128.turnbound.network.FieldSnapshotPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class FieldNetwork {
    public static final String PROTOCOL = "turnbound-field-alpha12";

    private FieldNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(FieldSnapshotPayload.TYPE, FieldSnapshotPayload.STREAM_CODEC);
        registrar.playToServer(FieldCommandPayload.TYPE, FieldCommandPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        FieldSessionManager.command(player, payload.command());
                    }
                }));
    }

    public static void sync(ServerPlayer player, FieldUiSnapshot snapshot) {
        PacketDistributor.sendToPlayer(player, new FieldSnapshotPayload(FieldUiCodec.encode(snapshot)));
    }

    public static void close(ServerPlayer player) {
        sync(player, FieldUiSnapshot.inactive());
    }
}
