package kr.moonseungjun.livingkingdoms.network;

import kr.moonseungjun.livingkingdoms.profile.OriginProfileManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class LivingKingdomsNetwork {
    public static final String PROTOCOL_VERSION = "realm-codex-2";

    private LivingKingdomsNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(OpenOriginScreenPayload.TYPE, OpenOriginScreenPayload.STREAM_CODEC);
        registrar.playToClient(OriginSubmissionResultPayload.TYPE, OriginSubmissionResultPayload.STREAM_CODEC);
        registrar.playToClient(OpenCodexPayload.TYPE, OpenCodexPayload.STREAM_CODEC);
        registrar.playToServer(SubmitOriginPayload.TYPE, SubmitOriginPayload.STREAM_CODEC, LivingKingdomsNetwork::handleSubmit);
        registrar.playToServer(RequestCodexPayload.TYPE, RequestCodexPayload.STREAM_CODEC, LivingKingdomsNetwork::handleCodex);
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
        String page = payload.page();
        if (!"map".equals(page) && !"status".equals(page)) {
            page = "map";
        }
        context.reply(RealmCodexSnapshotBuilder.build(player, page));
    }
}
