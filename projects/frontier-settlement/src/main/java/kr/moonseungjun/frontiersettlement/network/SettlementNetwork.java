package kr.moonseungjun.frontiersettlement.network;

import kr.moonseungjun.frontiersettlement.settlement.BuildingType;
import kr.moonseungjun.frontiersettlement.settlement.SettlementConstructionService;
import kr.moonseungjun.frontiersettlement.settlement.SettlementData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.Consumer;

public final class SettlementNetwork {
    private static final String PROTOCOL = "2";
    private static Consumer<SettlementSnapshotPayload> snapshotSink = payload -> {};
    private static Consumer<PlacementPreviewPayload> placementPreviewSink = payload -> {};

    private SettlementNetwork() {}

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(SettlementSnapshotPayload.TYPE, SettlementSnapshotPayload.CODEC,
                (payload, context) -> snapshotSink.accept(payload));
        registrar.playToClient(PlacementPreviewPayload.TYPE, PlacementPreviewPayload.CODEC,
                (payload, context) -> placementPreviewSink.accept(payload));
        registrar.playToServer(PlacementRequestPayload.TYPE, PlacementRequestPayload.CODEC,
                SettlementNetwork::handlePlacementRequest);
    }

    private static void handlePlacementRequest(PlacementRequestPayload payload,
                                               net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        BuildingType type = BuildingType.fromId(payload.buildingType());
        if (type == null) {
            context.reply(new PlacementPreviewPayload(
                    payload.nonce(), payload.buildingType(), false, false,
                    0, 0, 0, payload.rotation(), "알 수 없는 건물입니다."));
            return;
        }

        SettlementData data = SettlementData.get(player.level().getServer());
        if (data.construction().active() || data.roadConstruction().active() || data.outpostConstruction().active()) {
            context.reply(new PlacementPreviewPayload(
                    payload.nonce(), type.id(), false, false,
                    0, 0, 0, payload.rotation(), "현재 공사가 끝난 뒤 새 건물을 배치해 주세요."));
            return;
        }

        BlockPos selectedCenter = new BlockPos(payload.centerX(), payload.centerY(), payload.centerZ());
        SettlementConstructionService.PlacementCheck check =
                SettlementConstructionService.checkPlacement(player, type, selectedCenter, payload.rotation());
        if (!check.valid()) {
            context.reply(new PlacementPreviewPayload(
                    payload.nonce(), type.id(), false, false,
                    0, 0, 0, payload.rotation(), check.message()));
            return;
        }

        if (!payload.confirm()) {
            context.reply(new PlacementPreviewPayload(
                    payload.nonce(), type.id(), true, false,
                    check.origin().getX(), check.origin().getY(), check.origin().getZ(),
                    payload.rotation(), check.message()));
            return;
        }

        SettlementConstructionService.StartResult result =
                SettlementConstructionService.startAt(player, type, selectedCenter, payload.rotation());
        player.sendSystemMessage(Component.literal(result.message()));
        context.reply(new PlacementPreviewPayload(
                payload.nonce(), type.id(), result.started(), result.started(),
                check.origin().getX(), check.origin().getY(), check.origin().getZ(),
                payload.rotation(), result.message()));
    }

    public static void setSnapshotSink(Consumer<SettlementSnapshotPayload> sink) {
        snapshotSink = sink == null ? payload -> {} : sink;
    }

    public static void setPlacementPreviewSink(Consumer<PlacementPreviewPayload> sink) {
        placementPreviewSink = sink == null ? payload -> {} : sink;
    }

    public static void sendSnapshot(ServerPlayer player, SettlementSnapshotPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
