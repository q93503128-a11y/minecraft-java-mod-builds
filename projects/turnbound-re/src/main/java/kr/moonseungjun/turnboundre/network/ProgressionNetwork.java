package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.client.ProgressionClientState;
import kr.moonseungjun.turnboundre.progression.PlayerProgress;
import kr.moonseungjun.turnboundre.progression.ProgressionService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Party/progression presentation network. Reads and writes always terminate at the server-owned progress store. */
public final class ProgressionNetwork {
    private ProgressionNetwork() {}

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(
                ProgressionNetworkPayloads.RequestProgressC2S.TYPE,
                ProgressionNetworkPayloads.RequestProgressC2S.STREAM_CODEC,
                ProgressionNetwork::handleRequest);
        registrar.playToServer(
                ProgressionNetworkPayloads.SetPartyC2S.TYPE,
                ProgressionNetworkPayloads.SetPartyC2S.STREAM_CODEC,
                ProgressionNetwork::handleSetParty);
        registrar.playToClient(
                ProgressionNetworkPayloads.ProgressSnapshotS2C.TYPE,
                ProgressionNetworkPayloads.ProgressSnapshotS2C.STREAM_CODEC,
                (payload, context) -> ProgressionClientState.accept(payload));
    }

    private static void handleRequest(ProgressionNetworkPayloads.RequestProgressC2S payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        context.reply(snapshot(player, "", ""));
    }

    private static void handleSetParty(ProgressionNetworkPayloads.SetPartyC2S payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        final ProgressionNetworkPayloads.DecodedSetParty decoded;
        try {
            decoded = payload.decode();
        } catch (RuntimeException invalidWire) {
            TurnboundRe.LOGGER.warn("Rejected malformed TURNBOUND party command from {}: {}",
                    player.getUUID(), invalidWire.toString());
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return;
        PlayerProgress current = TurnboundRe.PROGRESS.getOrCreate(server, player.getUUID());
        if (!current.party().equals(decoded.expectedParty())) {
            context.reply(snapshot(player, "STALE_PARTY", ""));
            return;
        }

        ProgressionService.Result result = TurnboundRe.PROGRESS.setParty(server, player.getUUID(), decoded.requestedParty());
        context.reply(snapshot(player, result.code().name(), result.detail()));
    }

    private static ProgressionNetworkPayloads.ProgressSnapshotS2C snapshot(
            ServerPlayer player,
            String resultCode,
            String resultDetail
    ) {
        MinecraftServer server = player.getServer();
        if (server == null) throw new IllegalStateException("server unavailable for progression snapshot");
        PlayerProgress progress = TurnboundRe.PROGRESS.getOrCreate(server, player.getUUID());
        return ProgressionNetworkPayloads.ProgressSnapshotS2C.from(
                progress,
                TurnboundRe.DEFINITIONS.snapshot().registry(),
                resultCode,
                resultDetail);
    }
}
