package kr.moonseungjun.anomalousclasses.state;

import java.util.UUID;
import kr.moonseungjun.anomalousclasses.network.ClassNetwork;
import kr.moonseungjun.anomalousclasses.network.payload.RequestSwarmTacticsPayload;
import kr.moonseungjun.anomalousclasses.network.payload.SwarmTacticActionPayload;
import kr.moonseungjun.anomalousclasses.network.payload.SwarmTacticStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.SwarmTacticStatePayload.TacticEntry;
import net.minecraft.server.level.ServerPlayer;

/** One server-authoritative editing path shared by U, G and existing commands. */
public final class SwarmTacticService {
    private SwarmTacticService() {}

    public static void open(ServerPlayer player, int view) {
        if (!SwarmCasterService.isSwarm(player) || !player.isAlive()) return;
        if (view != RequestSwarmTacticsPayload.QUICK
                && view != RequestSwarmTacticsPayload.MANAGEMENT) return;
        send(player, view, SwarmTacticStatePayload.OPEN);
    }

    public static void act(ServerPlayer player, SwarmTacticActionPayload packet) {
        if (!SwarmCasterService.isSwarm(player) || !player.isAlive()) return;
        if (packet.view() != RequestSwarmTacticsPayload.QUICK
                && packet.view() != RequestSwarmTacticsPayload.MANAGEMENT) return;
        SwarmSavedData data = SwarmSavedData.get(player.getServer());
        SwarmSavedData.State state = data.player(player.getUUID());
        if (packet.expectedRevision() != state.tacticRevision()) {
            send(player, packet.view(), SwarmTacticStatePayload.STALE);
            return;
        }
        boolean accepted;
        UUID id = packet.id();
        switch (packet.action()) {
            case SwarmTacticActionPayload.SELECT -> accepted = state.activate(id);
            case SwarmTacticActionPayload.CREATE ->
                    accepted = state.createTactic(packet.name(), packet.attackPercent()) != null;
            case SwarmTacticActionPayload.UPDATE ->
                    accepted = state.updateTactic(id, packet.name(), packet.attackPercent());
            case SwarmTacticActionPayload.DELETE -> accepted = state.removeTactic(id);
            case SwarmTacticActionPayload.MOVE ->
                    accepted = state.moveTactic(id, packet.destination());
            case SwarmTacticActionPayload.TEMPORARY ->
                    accepted = state.applyTemporary(packet.attackPercent());
            case SwarmTacticActionPayload.RESTORE -> accepted = state.clearTemporary();
            default -> accepted = false;
        }
        if (accepted) data.changed();
        send(player, packet.view(), accepted
                ? SwarmTacticStatePayload.UPDATED : SwarmTacticStatePayload.REJECTED);
    }

    private static void send(ServerPlayer player, int view, int result) {
        PlayerClassProfile profile = ClassProfileSavedData.get(player.getServer())
                .profile(player.getUUID());
        SwarmSavedData.State state = SwarmSavedData.get(player.getServer())
                .player(player.getUUID());
        int level = profile.progress(ClassId.SWARM_CASTER).level();
        ClassNetwork.sendToPlayer(player, new SwarmTacticStatePayload(
                view, result, state.tacticRevision(), state.alive(),
                SwarmRules.maximum(level), state.attack(), state.guard(),
                state.lift(), state.regroup(),
                state.activeId() == null ? SwarmTacticActionPayload.NONE : state.activeId(),
                state.attackPercent(), state.isTemporary(),
                state.orderedTactics().stream().map(t ->
                        new TacticEntry(t.id(), t.name(), t.attackPercent())).toList()));
    }
}
