package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.battle.BattleDefinitionContext;
import kr.moonseungjun.turnboundre.battle.BattleEvent;
import kr.moonseungjun.turnboundre.battle.BattleInstance;
import kr.moonseungjun.turnboundre.client.BattleClientState;
import kr.moonseungjun.turnboundre.client.BattleResultClientState;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

/** Play-phase network registration. Battle and progression truth remain server-authoritative. */
public final class BattleNetwork {
    private static final String PROTOCOL_VERSION = "9";
    private static final BattleNetworkGateway GATEWAY = new BattleNetworkGateway(TurnboundRe.BATTLES);

    private BattleNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(BattleNetworkPayloads.BattleCommandC2S.TYPE,
                BattleNetworkPayloads.BattleCommandC2S.STREAM_CODEC,
                BattleNetwork::handleCommand);
        registrar.playToClient(BattleNetworkPayloads.BattleSnapshotS2C.TYPE,
                BattleNetworkPayloads.BattleSnapshotS2C.STREAM_CODEC,
                (payload, context) -> BattleClientState.accept(payload));
        registrar.playToClient(BattleNetworkPayloads.BattleEventsS2C.TYPE,
                BattleNetworkPayloads.BattleEventsS2C.STREAM_CODEC,
                (payload, context) -> BattleClientState.accept(payload));
        registrar.playToClient(BattleResultNetworkPayloads.ResultS2C.TYPE,
                BattleResultNetworkPayloads.ResultS2C.STREAM_CODEC,
                (payload, context) -> BattleResultClientState.accept(payload));
        registrar.playToServer(BattleResultNetworkPayloads.AcknowledgeResultC2S.TYPE,
                BattleResultNetworkPayloads.AcknowledgeResultC2S.STREAM_CODEC,
                BattleNetwork::handleResultAcknowledgement);
        registrar.playToClient(BattleResultNetworkPayloads.ResultClosedS2C.TYPE,
                BattleResultNetworkPayloads.ResultClosedS2C.STREAM_CODEC,
                (payload, context) -> BattleResultClientState.accept(payload));
        ProgressionNetwork.register(registrar);
    }

    private static void handleCommand(BattleNetworkPayloads.BattleCommandC2S payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;

        final BattleNetworkPayloads.DecodedCommand decoded;
        try {
            decoded = payload.decode();
        } catch (RuntimeException invalidWire) {
            TurnboundRe.LOGGER.warn("Rejected malformed TURNBOUND battle command from {}: {}", player.getUUID(), invalidWire.toString());
            return;
        }

        BattleNetworkGateway.Result result = GATEWAY.submit(player.getUUID(), decoded);
        BattleInstance battle = result.battle();
        if (battle == null) return;

        if (result.accepted()) {
            List<BattleEvent> events = BattleNetworkGateway.eventsSince(result);
            context.reply(BattleNetworkPayloads.BattleEventsS2C.from(
                    battle.battleId(), battle.revision(), result.eventStartIndex(), events));
        } else {
            context.reply(BattleNetworkPayloads.BattleEventsS2C.rejection(
                    battle.battleId(), battle.revision(), result.code().name() + ":" + result.detail()));
        }
        BattleDefinitionContext definitions = TurnboundRe.BATTLES.definitionContext(battle.battleId()).orElse(null);
        context.reply(BattleNetworkPayloads.BattleSnapshotS2C.from(battle, definitions, TurnboundRe.BATTLES));
    }

    private static void handleResultAcknowledgement(
            BattleResultNetworkPayloads.AcknowledgeResultC2S payload,
            IPayloadContext context
    ) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        final BattleResultNetworkPayloads.DecodedAcknowledgement decoded;
        try {
            decoded = payload.decode();
        } catch (RuntimeException invalidWire) {
            TurnboundRe.LOGGER.warn("Rejected malformed TURNBOUND result acknowledgement from {}: {}",
                    player.getUUID(), invalidWire.toString());
            return;
        }
        var server = player.level().getServer();
        if (server == null) return;
        BattleResultPresentationService.Acknowledgement result = TurnboundRe.RESULT_PRESENTATION.acknowledge(
                server, player.getUUID(), decoded);
        context.reply(BattleResultNetworkPayloads.ResultClosedS2C.of(decoded.battleId(), result.accepted(), result.code()));
    }
}
