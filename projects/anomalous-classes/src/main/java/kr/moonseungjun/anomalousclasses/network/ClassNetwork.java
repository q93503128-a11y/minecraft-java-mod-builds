package kr.moonseungjun.anomalousclasses.network;

import kr.moonseungjun.anomalousclasses.network.payload.RequestClassManagementPayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestClassMenuPayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestClassStatusPayload;
import kr.moonseungjun.anomalousclasses.network.payload.SelectClassPayload;
import kr.moonseungjun.anomalousclasses.network.payload.ClassSelectionStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.ProfileStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestProfilePayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestSkillTreePayload;
import kr.moonseungjun.anomalousclasses.network.payload.SkillTreeActionPayload;
import kr.moonseungjun.anomalousclasses.network.payload.SkillTreeStatePayload;
import kr.moonseungjun.anomalousclasses.state.ClassSelectionService;
import kr.moonseungjun.anomalousclasses.state.ProfileService;
import kr.moonseungjun.anomalousclasses.state.SkillTreeService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToServer(RequestClassMenuPayload.TYPE, RequestClassMenuPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) ClassSelectionService.openMenu(player);
                }));
        registrar.playToServer(RequestClassStatusPayload.TYPE, RequestClassStatusPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) ClassSelectionService.sendStatus(player);
                }));
        registrar.playToServer(SelectClassPayload.TYPE, SelectClassPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) ClassSelectionService.select(player, payload);
                }));
        registrar.playToServer(RequestProfilePayload.TYPE, RequestProfilePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) ProfileService.open(player);
                }));
        registrar.playToServer(RequestClassManagementPayload.TYPE, RequestClassManagementPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) ProfileService.openManagement(player);
                }));
        registrar.playToServer(RequestSkillTreePayload.TYPE, RequestSkillTreePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) SkillTreeService.open(player);
                }));
        registrar.playToServer(SkillTreeActionPayload.TYPE, SkillTreeActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) SkillTreeService.handle(player, payload);
                }));
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
