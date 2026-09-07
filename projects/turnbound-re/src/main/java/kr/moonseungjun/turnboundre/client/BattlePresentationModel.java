package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Immutable, rendering-friendly projection of one authoritative server snapshot. */
public record BattlePresentationModel(
        UUID battleId,
        long revision,
        String state,
        int cycle,
        String currentActorId,
        List<BattleNetworkPayloads.SnapshotParticipant> turnOrder,
        List<BattleNetworkPayloads.SnapshotParticipant> playerParty,
        List<BattleNetworkPayloads.SnapshotParticipant> enemies
) {
    public BattlePresentationModel {
        if (battleId == null) throw new IllegalArgumentException("battleId must not be null");
        if (state == null || state.isBlank()) throw new IllegalArgumentException("state must not be blank");
        if (currentActorId == null) currentActorId = "";
        turnOrder = List.copyOf(turnOrder);
        playerParty = List.copyOf(playerParty);
        enemies = List.copyOf(enemies);
    }

    public static BattlePresentationModel from(BattleNetworkPayloads.DecodedSnapshot snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("snapshot must not be null");
        List<BattleNetworkPayloads.SnapshotParticipant> turnOrder = List.copyOf(snapshot.participants());
        List<BattleNetworkPayloads.SnapshotParticipant> players = turnOrder.stream()
                .filter(participant -> "PLAYER".equals(participant.team()))
                .toList();
        List<BattleNetworkPayloads.SnapshotParticipant> enemies = turnOrder.stream()
                .filter(participant -> "ENEMY".equals(participant.team()))
                .toList();
        return new BattlePresentationModel(
                snapshot.battleId(),
                snapshot.revision(),
                snapshot.state(),
                snapshot.cycle(),
                snapshot.currentActorId(),
                turnOrder,
                players,
                enemies);
    }

    public Optional<BattleNetworkPayloads.SnapshotParticipant> currentActor() {
        return turnOrder.stream().filter(participant -> participant.id().equals(currentActorId)).findFirst();
    }

    public boolean awaitingPlayerCommand() {
        return "AWAIT_COMMAND".equals(state)
                && currentActor().map(participant -> "PLAYER".equals(participant.team())).orElse(false);
    }
}
