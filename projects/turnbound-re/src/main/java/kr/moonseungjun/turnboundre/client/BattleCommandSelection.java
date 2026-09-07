package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.battle.BattleCommand;
import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Pure client-side command selection contract over one authoritative server snapshot.
 * It never invents target eligibility: selectable participant ids come only from SnapshotAction.eligibleTargetIds.
 */
public final class BattleCommandSelection {
    public enum ValidationCode {
        READY,
        NOT_AWAITING_PLAYER,
        ACTION_NOT_PUBLISHED,
        ACTION_DISABLED,
        INVALID_TARGET_COUNT,
        TARGET_NOT_ELIGIBLE
    }

    public record Validation(ValidationCode code, String detail) {
        public Validation {
            if (code == null) throw new IllegalArgumentException("code must not be null");
            if (detail == null) detail = "";
        }

        public boolean ready() {
            return code == ValidationCode.READY;
        }
    }

    private BattleCommandSelection() {}

    public static Optional<BattleNetworkPayloads.SnapshotAction> publishedAction(
            BattlePresentationModel model,
            BattleNetworkPayloads.SnapshotAction requested
    ) {
        if (model == null || requested == null) return Optional.empty();
        return model.availableActions().stream()
                .filter(action -> action.id().equals(requested.id()) && action.slot().equals(requested.slot()))
                .findFirst();
    }

    /** Returns target participants in the exact server-published candidate order. */
    public static List<BattleNetworkPayloads.SnapshotParticipant> eligibleTargets(
            BattlePresentationModel model,
            BattleNetworkPayloads.SnapshotAction requested
    ) {
        BattleNetworkPayloads.SnapshotAction action = publishedAction(model, requested).orElse(null);
        if (action == null) return List.of();

        List<BattleNetworkPayloads.SnapshotParticipant> result = new ArrayList<>();
        for (String targetId : action.eligibleTargetIds()) {
            model.turnOrder().stream()
                    .filter(participant -> participant.id().equals(targetId) && participant.alive())
                    .findFirst()
                    .ifPresent(result::add);
        }
        return List.copyOf(result);
    }

    /**
     * Returns the only possible target set when the server candidate count exactly equals the required count.
     * An empty list means the player must make a choice.
     */
    public static List<String> forcedTargetsIfUnambiguous(
            BattlePresentationModel model,
            BattleNetworkPayloads.SnapshotAction requested
    ) {
        BattleNetworkPayloads.SnapshotAction action = publishedAction(model, requested).orElse(null);
        if (action == null || !action.usable()) return List.of();
        List<BattleNetworkPayloads.SnapshotParticipant> candidates = eligibleTargets(model, action);
        if (candidates.size() != action.targetCount()) return List.of();
        return candidates.stream().map(BattleNetworkPayloads.SnapshotParticipant::id).toList();
    }

    public static Validation validate(
            BattlePresentationModel model,
            BattleNetworkPayloads.SnapshotAction requested,
            List<String> selectedTargetIds
    ) {
        if (model == null || !model.awaitingPlayerCommand()) {
            return new Validation(ValidationCode.NOT_AWAITING_PLAYER, "battle is not waiting for a player command");
        }

        BattleNetworkPayloads.SnapshotAction action = publishedAction(model, requested).orElse(null);
        if (action == null) {
            return new Validation(ValidationCode.ACTION_NOT_PUBLISHED, "action is not present in the current server snapshot");
        }
        if (!action.usable()) {
            return new Validation(ValidationCode.ACTION_DISABLED, action.disabledReason());
        }

        List<String> targets = selectedTargetIds == null ? List.of() : List.copyOf(selectedTargetIds);
        if (targets.size() != action.targetCount()) {
            return new Validation(ValidationCode.INVALID_TARGET_COUNT,
                    "expected=" + action.targetCount() + " actual=" + targets.size());
        }
        Set<String> unique = new HashSet<>(targets);
        if (unique.size() != targets.size()) {
            return new Validation(ValidationCode.TARGET_NOT_ELIGIBLE, "duplicate target id");
        }

        Set<String> publishedCandidates = new HashSet<>(action.eligibleTargetIds());
        Set<String> liveSnapshotParticipants = new HashSet<>();
        for (BattleNetworkPayloads.SnapshotParticipant participant : model.turnOrder()) {
            if (participant.alive()) liveSnapshotParticipants.add(participant.id());
        }
        for (String targetId : targets) {
            if (!publishedCandidates.contains(targetId) || !liveSnapshotParticipants.contains(targetId)) {
                return new Validation(ValidationCode.TARGET_NOT_ELIGIBLE, targetId);
            }
        }
        return new Validation(ValidationCode.READY, "");
    }

    public static BattleCommand buildCommand(
            BattlePresentationModel model,
            BattleNetworkPayloads.SnapshotAction requested,
            List<String> selectedTargetIds,
            String commandId
    ) {
        Validation validation = validate(model, requested, selectedTargetIds);
        if (!validation.ready()) {
            throw new IllegalArgumentException("invalid battle command selection: " + validation.code() + " " + validation.detail());
        }
        if (commandId == null || commandId.isBlank()) throw new IllegalArgumentException("commandId must not be blank");

        BattleNetworkPayloads.SnapshotAction action = publishedAction(model, requested).orElseThrow();
        BattleNetworkPayloads.SnapshotParticipant actor = model.currentActor().orElseThrow();
        return new BattleCommand(
                model.revision(), actor.id(), action.id(), commandId, List.copyOf(selectedTargetIds));
    }
}
