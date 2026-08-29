package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.network.BattleCommandPayload;
import io.github.q93503128.turnbound.network.BattleSnapshotPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class BattleNetwork {
    public static final String PROTOCOL = "turnbound-alpha5";

    private BattleNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(BattleSnapshotPayload.TYPE, BattleSnapshotPayload.STREAM_CODEC);
        registrar.playToServer(BattleCommandPayload.TYPE, BattleCommandPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        BattleSessionManager.command(player, payload.command());
                    }
                }));
    }

    static void sync(ServerPlayer player, BattleSession session) {
        PacketDistributor.sendToPlayer(player, new BattleSnapshotPayload(BattleSnapshotCodec.encode(session)));
    }

    static void close(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new BattleSnapshotPayload("H|0|0|1|RUNNING||1\n"));
    }
}
