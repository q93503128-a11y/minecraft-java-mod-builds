package kr.moonseungjun.villageguardians;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class VillageNetwork {
    private static final Map<UUID, ActionStamp> LAST_MUTATION = new LinkedHashMap<>();

    private VillageNetwork() {}

    public static synchronized void resetTransientState() {
        LAST_MUTATION.clear();
    }

    public static synchronized void forgetPlayer(UUID playerId) {
        if (playerId != null) LAST_MUTATION.remove(playerId);
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("5");
        registrar.playToClient(OpenVillageUiPayload.TYPE, OpenVillageUiPayload.STREAM_CODEC);
        registrar.playToClient(PlayerStatusPayload.TYPE, PlayerStatusPayload.STREAM_CODEC);
        registrar.playToClient(SkillMotionPayload.TYPE, SkillMotionPayload.STREAM_CODEC);
        registrar.playToClient(SkillHudPayload.TYPE, SkillHudPayload.STREAM_CODEC);
        registrar.playToClient(MainHudPayload.TYPE, MainHudPayload.STREAM_CODEC);
        registrar.playToServer(VillageUiActionPayload.TYPE, VillageUiActionPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        String action = payload.action();
                        if (!acceptAction(player, action)) return;
                        if (!VillageLocalActionSystem.handle(player, action)
                                && !VillageUiController.handleAction(player, action)) {
                            VillageUiService.handleAction(player, action);
                        }
                    }
                });
        registrar.playToServer(RequestPlayerStatusPayload.TYPE, RequestPlayerStatusPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) sendPlayerStatus(player);
                });
    }

    private static synchronized boolean acceptAction(ServerPlayer player, String action) {
        if (player == null || action == null || action.isBlank() || !isMutationAction(action)) return true;
        long now = player.level().getGameTime();
        ActionStamp previous = LAST_MUTATION.get(player.getUUID());
        if (previous != null && previous.action().equals(action)
                && now >= previous.gameTime() && now - previous.gameTime() <= 4L) {
            return false;
        }
        LAST_MUTATION.put(player.getUUID(), new ActionStamp(action, now));
        return true;
    }

    private static boolean isMutationAction(String action) {
        return action.equals("vote_yes") || action.equals("vote_no")
                || action.equals("buy_arrows") || action.equals("claim_bread")
                || action.equals("sell_loot") || action.equals("exchange_supplies")
                || action.startsWith("repair:") || action.startsWith("upgrade:")
                || action.startsWith("gear:") || action.startsWith("consumable:")
                || action.startsWith("sell_item:") || action.startsWith("forge_enhance:")
                || action.startsWith("fusion_combine:")
                || action.startsWith("role_node:") || action.startsWith("skill_node:")
                || action.startsWith("research_skill_unlock:")
                || action.startsWith("defense_research:")
                || action.startsWith("merc_hire:") || action.startsWith("retire_mercenary:")
                || action.startsWith("merc_deploy:")
                || action.startsWith("siege_segment_repair:")
                || action.startsWith("siege_segment_upgrade:")
                || action.startsWith("siege_turret_repair:")
                || action.startsWith("siege_turret_upgrade:")
                || action.startsWith("siege_turret_dismantle:")
                || action.equals("siege_turret_repair_all")
                || action.startsWith("restart_");
    }

    public static void open(ServerPlayer player, OpenVillageUiPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendSkillMotion(
            net.minecraft.server.level.ServerLevel level,
            ServerPlayer owner,
            String motion,
            int durationTicks) {
        if (level == null || owner == null || motion == null || durationTicks <= 0) return;
        SkillMotionPayload payload = new SkillMotionPayload(owner.getId(), motion, durationTicks);
        for (ServerPlayer viewer : level.players()) {
            PacketDistributor.sendToPlayer(viewer, payload);
        }
    }

    public static void sendSkillHud(ServerPlayer player, String text) {
        if (player == null) return;
        PacketDistributor.sendToPlayer(player, new SkillHudPayload(text == null ? "" : text));
    }

    public static void sendMainHud(ServerPlayer player, String text) {
        if (player == null) return;
        PacketDistributor.sendToPlayer(player, new MainHudPayload(text == null ? "" : text));
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
                        + " · 장비 최고 +" + VillageEquipmentRaritySystem.bestEquippedEnhancement(player),
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

    public record SkillMotionPayload(
            int entityId,
            String motion,
            int durationTicks) implements CustomPacketPayload {
        public static final Type<SkillMotionPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "skill_motion"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SkillMotionPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SkillMotionPayload::entityId,
                ByteBufCodecs.STRING_UTF8, SkillMotionPayload::motion,
                ByteBufCodecs.VAR_INT, SkillMotionPayload::durationTicks,
                SkillMotionPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SkillHudPayload(String text) implements CustomPacketPayload {
        public static final Type<SkillHudPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "skill_hud"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SkillHudPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, SkillHudPayload::text,
                SkillHudPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record MainHudPayload(String text) implements CustomPacketPayload {
        public static final Type<MainHudPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "main_hud"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MainHudPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, MainHudPayload::text,
                MainHudPayload::new);

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

    private record ActionStamp(String action, long gameTime) {}

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
