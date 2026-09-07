package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;

import java.util.ArrayList;
import java.util.Comparator;
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
        List<BattleNetworkPayloads.SnapshotParticipant> enemies,
        List<BattleNetworkPayloads.SnapshotAction> availableActions
) {
    public BattlePresentationModel {
        if (battleId == null) throw new IllegalArgumentException("battleId must not be null");
        if (state == null || state.isBlank()) throw new IllegalArgumentException("state must not be blank");
        if (currentActorId == null) currentActorId = "";
        turnOrder = List.copyOf(turnOrder);
        playerParty = List.copyOf(playerParty);
        enemies = List.copyOf(enemies);
        availableActions = List.copyOf(availableActions);
    }

    public static BattlePresentationModel from(BattleNetworkPayloads.DecodedSnapshot snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("snapshot must not be null");
        List<BattleNetworkPayloads.SnapshotParticipant> stable = List.copyOf(snapshot.participants());
        List<BattleNetworkPayloads.SnapshotParticipant> turnOrder = rotateFromCurrent(stable, snapshot.currentActorId());
        Comparator<BattleNetworkPayloads.SnapshotParticipant> bySlot =
                Comparator.comparingInt(BattleNetworkPayloads.SnapshotParticipant::participantOrdinal);
        List<BattleNetworkPayloads.SnapshotParticipant> players = stable.stream()
                .filter(participant -> "PLAYER".equals(participant.team()))
                .sorted(bySlot)
                .toList();
        List<BattleNetworkPayloads.SnapshotParticipant> enemies = stable.stream()
                .filter(participant -> "ENEMY".equals(participant.team()))
                .sorted(bySlot)
                .toList();
        return new BattlePresentationModel(
                snapshot.battleId(), snapshot.revision(), snapshot.state(), snapshot.cycle(), snapshot.currentActorId(),
                turnOrder, players, enemies, snapshot.availableActions());
    }

    public Optional<BattleNetworkPayloads.SnapshotParticipant> currentActor() {
        return turnOrder.stream().filter(participant -> participant.id().equals(currentActorId)).findFirst();
    }

    public Optional<BattleNetworkPayloads.SnapshotParticipant> focusEnemy() {
        Optional<BattleNetworkPayloads.SnapshotParticipant> currentEnemy = currentActor()
                .filter(participant -> "ENEMY".equals(participant.team()));
        if (currentEnemy.isPresent()) return currentEnemy;
        return enemies.stream().filter(BattleNetworkPayloads.SnapshotParticipant::alive).findFirst()
                .or(() -> enemies.stream().findFirst());
    }

    public boolean awaitingPlayerCommand() {
        return "AWAIT_COMMAND".equals(state)
                && currentActor().map(participant -> "PLAYER".equals(participant.team())).orElse(false);
    }

    private static List<BattleNetworkPayloads.SnapshotParticipant> rotateFromCurrent(
            List<BattleNetworkPayloads.SnapshotParticipant> participants,
            String currentActorId
    ) {
        if (participants.isEmpty() || currentActorId == null || currentActorId.isBlank()) return participants;
        int currentIndex = -1;
        for (int i = 0; i < participants.size(); i++) {
            if (participants.get(i).id().equals(currentActorId)) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex <= 0) return participants;
        List<BattleNetworkPayloads.SnapshotParticipant> rotated = new ArrayList<>(participants.size());
        rotated.addAll(participants.subList(currentIndex, participants.size()));
        rotated.addAll(participants.subList(0, currentIndex));
        return List.copyOf(rotated);
    }
}
