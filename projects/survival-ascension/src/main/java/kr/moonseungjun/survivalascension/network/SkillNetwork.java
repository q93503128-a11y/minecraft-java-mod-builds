package kr.moonseungjun.survivalascension.network;

/* Client receiver seam adapted from Skill Proficiencies, Copyright (c) 2026 balovich-matje, MIT. */

import kr.moonseungjun.survivalascension.construction.ConstructionMode;
import kr.moonseungjun.survivalascension.construction.ConstructionProgression;
import kr.moonseungjun.survivalascension.equipment.EquipmentReforgeService;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureService;
import kr.moonseungjun.survivalascension.mining.MiningMode;
import kr.moonseungjun.survivalascension.mining.MiningProgression;
import kr.moonseungjun.survivalascension.mobility.MobilityProgression;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import java.util.Objects;
import java.util.function.Consumer;

public final class SkillNetwork {
    private static final String PROTOCOL = "8";
    private static volatile Consumer<SkillUpdatePayload> updateSink = payload -> {};
    private static volatile Consumer<SkillSnapshotPayload> snapshotSink = payload -> {};
    private SkillNetwork() {}

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(SkillUpdatePayload.TYPE, SkillUpdatePayload.CODEC, (payload, context) -> updateSink.accept(payload));
        registrar.playToClient(SkillSnapshotPayload.TYPE, SkillSnapshotPayload.CODEC, (payload, context) -> snapshotSink.accept(payload));
        registrar.playToServer(ConstructionModePayload.TYPE, ConstructionModePayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) ConstructionProgression.setMode(player, ConstructionMode.fromId(payload.modeId()));
                }));
        registrar.playToServer(MiningModePayload.TYPE, MiningModePayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) MiningProgression.setMode(player, MiningMode.fromId(payload.modeId()));
                }));
        registrar.playToServer(MobilityActionPayload.TYPE, MobilityActionPayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) MobilityProgression.performAction(player);
                }));
        registrar.playToServer(EquipmentActionPayload.TYPE, EquipmentActionPayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) EquipmentReforgeService.perform(player, payload.action());
                }));
        registrar.playToServer(InfrastructureActionPayload.TYPE, InfrastructureActionPayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) InfrastructureService.perform(player, payload.projectId(), payload.action());
                }));
    }

    public static void installClientReceivers(Consumer<SkillUpdatePayload> updates, Consumer<SkillSnapshotPayload> snapshots) {
        updateSink = Objects.requireNonNull(updates);
        snapshotSink = Objects.requireNonNull(snapshots);
    }
    public static void sendUpdate(ServerPlayer player, SkillUpdatePayload payload) { PacketDistributor.sendToPlayer(player, payload); }
    public static void sendSnapshot(ServerPlayer player, SkillSnapshotPayload payload) { PacketDistributor.sendToPlayer(player, payload); }
}
