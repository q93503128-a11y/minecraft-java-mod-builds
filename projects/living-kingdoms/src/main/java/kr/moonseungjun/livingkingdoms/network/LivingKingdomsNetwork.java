package kr.moonseungjun.livingkingdoms.network;

import kr.moonseungjun.livingkingdoms.profile.OriginProfileManager;
import kr.moonseungjun.livingkingdoms.skill.SkillProgressionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Set;

public final class LivingKingdomsNetwork {
    public static final String PROTOCOL_VERSION = "realm-codex-5";
    private static final Set<String> CODEX_PAGES = Set.of("overview", "equipment", "map", "skills", "status");

    private LivingKingdomsNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(OpenOriginScreenPayload.TYPE, OpenOriginScreenPayload.STREAM_CODEC);
        registrar.playToClient(OriginSubmissionResultPayload.TYPE, OriginSubmissionResultPayload.STREAM_CODEC);
        registrar.playToClient(RealmBuildProgressPayload.TYPE, RealmBuildProgressPayload.STREAM_CODEC);
        registrar.playToClient(FantasyHudStatePayload.TYPE, FantasyHudStatePayload.STREAM_CODEC);
        registrar.playToClient(OpenCodexPayload.TYPE, OpenCodexPayload.STREAM_CODEC);
        registrar.playToServer(SubmitOriginPayload.TYPE, SubmitOriginPayload.STREAM_CODEC, LivingKingdomsNetwork::handleSubmit);
        registrar.playToServer(RequestCodexPayload.TYPE, RequestCodexPayload.STREAM_CODEC, LivingKingdomsNetwork::handleCodex);
        registrar.playToServer(UnlockSkillPayload.TYPE, UnlockSkillPayload.STREAM_CODEC, LivingKingdomsNetwork::handleUnlockSkill);
    }

    private static void handleSubmit(SubmitOriginPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            context.disconnect(Component.literal("잘못된 출신 선택 요청입니다."));
            return;
        }
        context.reply(OriginProfileManager.submit(player, payload));
    }

    private static void handleCodex(RequestCodexPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            context.disconnect(Component.literal("잘못된 왕국 수첩 요청입니다."));
            return;
        }
        String page = CODEX_PAGES.contains(payload.page()) ? payload.page() : "overview";
        if ("status".equals(page)) page = "overview";
        context.reply(RealmCodexSnapshotBuilder.build(player, page));
    }

    private static void handleUnlockSkill(UnlockSkillPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            context.disconnect(Component.literal("잘못된 기술 해금 요청입니다."));
            return;
        }
        if (payload.skillId() == null || !payload.skillId().matches("[a-z0-9_]{1,64}")) {
            context.disconnect(Component.literal("허용되지 않은 기술 ID입니다."));
            return;
        }
        var result = SkillProgressionManager.unlock(player, payload.skillId());
        player.sendSystemMessage(Component.literal((result.accepted() ? "§a[기술 해금] §f" : "§c[기술 해금 실패] §f")
                + result.message()));
        context.reply(RealmCodexSnapshotBuilder.build(player, "skills"));
    }
}
