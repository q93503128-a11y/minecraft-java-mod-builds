package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.client.ExpeditionJournalClientState;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.progression.PlayerProgress;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Server-authored Expedition Journal snapshot flow.
 * The journal is informational only: production encounters start from validated in-world anchors.
 */
public final class ExpeditionNetwork {
    private ExpeditionNetwork() {}

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(
                ExpeditionNetworkPayloads.RequestJournalC2S.TYPE,
                ExpeditionNetworkPayloads.RequestJournalC2S.STREAM_CODEC,
                ExpeditionNetwork::handleRequest);
        registrar.playToClient(
                ExpeditionNetworkPayloads.JournalSnapshotS2C.TYPE,
                ExpeditionNetworkPayloads.JournalSnapshotS2C.STREAM_CODEC,
                (payload, context) -> ExpeditionJournalClientState.accept(payload));
    }

    private static void handleRequest(ExpeditionNetworkPayloads.RequestJournalC2S payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        replyJournal(context, player);
    }

    static void replyJournal(IPayloadContext context, ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        PlayerProgress progress = TurnboundRe.PROGRESS.getOrCreate(server, player.getUUID());
        DefinitionRegistry definitions = TurnboundRe.DEFINITIONS.snapshot().registry();
        context.reply(ExpeditionNetworkPayloads.JournalSnapshotS2C.of(
                new ExpeditionNetworkPayloads.JournalView(
                        progress.party(),
                        ExpeditionJournalProjection.encounters(definitions),
                        "",
                        "")));
    }
}
