package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.network.MetaCommandPayload;
import io.github.q93503128.turnbound.network.MetaSnapshotPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MetaNetwork {
    public static final String PROTOCOL = "turnbound-meta-v04";
    private MetaNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(MetaSnapshotPayload.TYPE, MetaSnapshotPayload.STREAM_CODEC);
        registrar.playToServer(MetaCommandPayload.TYPE, MetaCommandPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) MetaMenuService.command(player, payload.command());
                }));
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new MetaSnapshotPayload(MetaUiCodec.encode(MetaMenuService.snapshot(player))));
    }
}
