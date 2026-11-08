package kr.moonseungjun.anomalousclasses.network;

import kr.moonseungjun.anomalousclasses.network.payload.RequestClassMenuPayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestClassStatusPayload;
import kr.moonseungjun.anomalousclasses.network.payload.SelectClassPayload;
import kr.moonseungjun.anomalousclasses.network.payload.ClassSelectionStatePayload;
import kr.moonseungjun.anomalousclasses.state.ClassSelectionService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ClassNetwork {
    private ClassNetwork() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ClassNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(RequestClassMenuPayload.TYPE, RequestClassMenuPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) ClassSelectionService.openMenu(player);
                });
        registrar.playToServer(RequestClassStatusPayload.TYPE, RequestClassStatusPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) ClassSelectionService.sendStatus(player);
                });
        registrar.playToServer(SelectClassPayload.TYPE, SelectClassPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) ClassSelectionService.select(player, payload);
                });
    }

    public static void sendToPlayer(ServerPlayer player, ClassSelectionStatePayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
