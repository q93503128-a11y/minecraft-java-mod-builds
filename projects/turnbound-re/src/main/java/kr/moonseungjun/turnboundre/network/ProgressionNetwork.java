package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.client.ProgressionClientState;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.progression.CharacterProgress;
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
        registrar.playToServer(
                ProgressionNetworkPayloads.GrowthC2S.TYPE,
                ProgressionNetworkPayloads.GrowthC2S.STREAM_CODEC,
                ProgressionNetwork::handleGrowth);
        registrar.playToClient(
                ProgressionNetworkPayloads.ProgressSnapshotS2C.TYPE,
                ProgressionNetworkPayloads.ProgressSnapshotS2C.STREAM_CODEC,
                (payload, context) -> ProgressionClientState.accept(payload));
        registrar.playToClient(
                CharacterPresentationNetworkPayloads.CatalogS2C.TYPE,
                CharacterPresentationNetworkPayloads.CatalogS2C.STREAM_CODEC,
                (payload, context) -> ProgressionClientState.accept(payload));
    }

    private static void handleRequest(ProgressionNetworkPayloads.RequestProgressC2S payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        replyState(context, player, "", "");
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

        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        PlayerProgress current = TurnboundRe.PROGRESS.getOrCreate(server, player.getUUID());
        if (!current.party().equals(decoded.expectedParty())) {
            replyState(context, player, "STALE_PARTY", "");
            return;
        }

        ProgressionService.Result result = TurnboundRe.PROGRESS.setParty(server, player.getUUID(), decoded.requestedParty());
        replyState(context, player, result.code().name(), result.detail());
    }

    private static void handleGrowth(ProgressionNetworkPayloads.GrowthC2S payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        final ProgressionNetworkPayloads.DecodedGrowth decoded;
        try {
            decoded = payload.decode();
        } catch (RuntimeException invalidWire) {
            TurnboundRe.LOGGER.warn("Rejected malformed TURNBOUND growth command from {}: {}",
                    player.getUUID(), invalidWire.toString());
            return;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        PlayerProgress current = TurnboundRe.PROGRESS.getOrCreate(server, player.getUUID());
        CharacterProgress character = current.characters().get(decoded.characterId());
        if (character == null) {
            replyState(context, player, "GROWTH_NOT_OWNED", decoded.characterId());
            return;
        }
        if (character.currentStar() != decoded.expectedStar() || character.level() != decoded.expectedLevel()) {
            replyState(context, player, "GROWTH_STALE", decoded.characterId());
            return;
        }

        ProgressionService.Result result = switch (decoded.operation()) {
            case "LEVEL_UP" -> TurnboundRe.PROGRESS.levelUp(server, player.getUUID(), decoded.characterId());
            case "ASCEND" -> TurnboundRe.PROGRESS.ascend(server, player.getUUID(), decoded.characterId());
            default -> null;
        };
        if (result == null) {
            replyState(context, player, "GROWTH_INVALID_OPERATION", decoded.operation());
            return;
        }
        replyState(context, player, "GROWTH_" + result.code().name(), result.detail());
    }

    private static void replyState(
            IPayloadContext context,
            ServerPlayer player,
            String resultCode,
            String resultDetail
    ) {
        MinecraftServer server = player.level().getServer();
        if (server == null) throw new IllegalStateException("server unavailable for progression snapshot");
        PlayerProgress progress = TurnboundRe.PROGRESS.getOrCreate(server, player.getUUID());
        DefinitionRegistry definitions = TurnboundRe.DEFINITIONS.snapshot().registry();
        context.reply(CharacterPresentationNetworkPayloads.CatalogS2C.from(definitions));
        context.reply(ProgressionNetworkPayloads.ProgressSnapshotS2C.from(
                progress,
                definitions,
                resultCode,
                resultDetail));
    }
}
