package kr.moonseungjun.villageguardians;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class VillageNetwork {
    private VillageNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("4");
        registrar.playToClient(OpenVillageUiPayload.TYPE, OpenVillageUiPayload.STREAM_CODEC);
        registrar.playToClient(PlayerStatusPayload.TYPE, PlayerStatusPayload.STREAM_CODEC);
        registrar.playToServer(VillageUiActionPayload.TYPE, VillageUiActionPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player
                            && !VillageLocalActionSystem.handle(player, payload.action())
                            && !VillageUiController.handleAction(player, payload.action())) {
                        VillageUiService.handleAction(player, payload.action());
                    }
                });
        registrar.playToServer(RequestPlayerStatusPayload.TYPE, RequestPlayerStatusPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) sendPlayerStatus(player);
                });
    }

    public static void open(ServerPlayer player, OpenVillageUiPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendPlayerStatus(ServerPlayer player) {
        RpgProgress progress = VillageCouncilState.progressOf(player.getUUID());
        String xp = progress.level() >= RpgProgress.MAX_LEVEL
                ? "최고 레벨"
                : progress.experience() + "/" + progress.experienceToNextLevel() + " XP";
        String role = VillageCouncilState.roleOf(player.getUUID())
                .map(VillageRole::displayName).orElse("미선택");
        PacketDistributor.sendToPlayer(player, new PlayerStatusPayload(
                "레벨 " + progress.level() + " · " + xp,
                role,
                "주화 " + VillageProgressionSystem.coins(player)
                        + " · 장비 +" + VillageProgressionSystem.forgeRank(player),
                "개인 연구 +" + VillageProgressionSystem.skillRank(player)
                        + " · 유물 " + VillageRelicSystem.summary(player)
                        + " · " + VillageCouncilState.currentDay() + "일 "
                        + VillageCouncilState.currentPhase().koreanName()));
    }

    public record OpenVillageUiPayload(
            String screenId,
            String title,
            String body,
            String actions,
            String labels) implements CustomPacketPayload {
        public static final Type<OpenVillageUiPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "open_village_ui"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenVillageUiPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, OpenVillageUiPayload::screenId,
                ByteBufCodecs.STRING_UTF8, OpenVillageUiPayload::title,
                ByteBufCodecs.STRING_UTF8, OpenVillageUiPayload::body,
                ByteBufCodecs.STRING_UTF8, OpenVillageUiPayload::actions,
                ByteBufCodecs.STRING_UTF8, OpenVillageUiPayload::labels,
                OpenVillageUiPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record VillageUiActionPayload(String action) implements CustomPacketPayload {
        public static final Type<VillageUiActionPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "village_ui_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, VillageUiActionPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, VillageUiActionPayload::action,
                VillageUiActionPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record RequestPlayerStatusPayload(String source) implements CustomPacketPayload {
        public static final Type<RequestPlayerStatusPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "request_player_status"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestPlayerStatusPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, RequestPlayerStatusPayload::source,
                RequestPlayerStatusPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record PlayerStatusPayload(
            String progress,
            String role,
            String economy,
            String village) implements CustomPacketPayload {
        public static final Type<PlayerStatusPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "player_status"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PlayerStatusPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, PlayerStatusPayload::progress,
                ByteBufCodecs.STRING_UTF8, PlayerStatusPayload::role,
                ByteBufCodecs.STRING_UTF8, PlayerStatusPayload::economy,
                ByteBufCodecs.STRING_UTF8, PlayerStatusPayload::village,
                PlayerStatusPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
