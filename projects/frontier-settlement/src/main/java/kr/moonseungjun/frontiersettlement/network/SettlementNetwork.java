package kr.moonseungjun.frontiersettlement.network;

import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class SettlementNetwork {
    private static final String PROTOCOL = "1";
    private static Consumer<SettlementSnapshotPayload> snapshotSink = payload -> {};

    private SettlementNetwork() {}

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(SettlementSnapshotPayload.TYPE, SettlementSnapshotPayload.CODEC,
                (payload, context) -> snapshotSink.accept(payload));
    }

    public static void setSnapshotSink(Consumer<SettlementSnapshotPayload> sink) {
        snapshotSink = sink == null ? payload -> {} : sink;
    }

    public static void sendSnapshot(ServerPlayer player, SettlementSnapshotPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
