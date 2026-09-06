package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.battle.BattleEvent;
import kr.moonseungjun.turnboundre.battle.BattleInstance;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

/** M2 play-phase network registration. Common code contains no client rendering/UI references. */
public final class BattleNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final BattleNetworkGateway GATEWAY = new BattleNetworkGateway(TurnboundRe.BATTLES);

    private BattleNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(BattleNetworkPayloads.BattleCommandC2S.TYPE,
                BattleNetworkPayloads.BattleCommandC2S.STREAM_CODEC,
                BattleNetwork::handleCommand);
        registrar.playToClient(BattleNetworkPayloads.BattleSnapshotS2C.TYPE,
                BattleNetworkPayloads.BattleSnapshotS2C.STREAM_CODEC,
                (payload, context) -> {});
        registrar.playToClient(BattleNetworkPayloads.BattleEventsS2C.TYPE,
                BattleNetworkPayloads.BattleEventsS2C.STREAM_CODEC,
                (payload, context) -> {});
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

        BattleNetworkGateway.Result result = GATEWAY.authorize(player.getUUID(), decoded);
        BattleInstance battle = result.battle();
        if (battle == null) return;

        if (result.authorized()) {
            // This slice deliberately stops before mutation: the next adapter step resolves definitions/policy and then
            // invokes the existing strict BattleCommandService. No network path may bypass that M1 gate.
            BattleEvent authorized = new BattleEvent(battle.revision(), "COMMAND_AUTHORIZED", decoded.actorId(), decoded.commandId());
            context.reply(BattleNetworkPayloads.BattleEventsS2C.from(
                    battle.battleId(), battle.revision(), battle.eventLog().size(), List.of(authorized)));
        } else {
            context.reply(BattleNetworkPayloads.BattleEventsS2C.rejection(
                    battle.battleId(), battle.revision(), result.code().name() + ":" + result.detail()));
        }
        // Always return the authoritative state after command authorization/rejection for deterministic resync.
        context.reply(BattleNetworkPayloads.BattleSnapshotS2C.from(battle));
    }
}
