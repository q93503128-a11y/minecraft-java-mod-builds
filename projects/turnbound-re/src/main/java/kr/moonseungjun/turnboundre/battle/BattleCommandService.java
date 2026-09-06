package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.ActionDefinition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Strict server-side M1 command gate.
 * Validation failures are mutation-free: BattleInstance is touched only after every strict dimension passes.
 */
public final class BattleCommandService {
    public enum Result {
        ACCEPTED,
        DUPLICATE_COMMAND,
        ACTION_NOT_OWNED,
        ACTION_ON_COOLDOWN,
        STATUS_BLOCKED,
        INVALID_TARGET_COUNT,
        INVALID_TARGET,
        WRONG_PHASE,
        STALE_REVISION,
        WRONG_ACTOR,
        ACTION_MISMATCH,
        INVALID_ACTION,
        INSUFFICIENT_ENERGY
    }

    private final BattleInstance battle;
    private final Map<String, BattleParticipant> participants;
    private final Set<String> consumedCommandIds = new HashSet<>();

    public BattleCommandService(BattleInstance battle, List<BattleParticipant> participants) {
        if (battle == null) throw new IllegalArgumentException("battle must not be null");
        if (participants == null || participants.isEmpty()) throw new IllegalArgumentException("participants must not be empty");
        this.battle = battle;
        Map<String, BattleParticipant> byId = new HashMap<>();
        for (BattleParticipant participant : participants) {
            if (participant == null) throw new IllegalArgumentException("participant must not be null");
            if (byId.putIfAbsent(participant.id(), participant) != null) {
                throw new IllegalArgumentException("duplicate participant id: " + participant.id());
            }
        }
        this.participants = Map.copyOf(byId);
    }

    public Result submit(BattleCommand command, ActionDefinition action, ActionUsePolicy policy) {
        if (command == null) throw new IllegalArgumentException("command must not be null");
        if (policy == null) throw new IllegalArgumentException("policy must not be null");

        if (consumedCommandIds.contains(command.commandId())) return Result.DUPLICATE_COMMAND;
        if (!policy.owned()) return Result.ACTION_NOT_OWNED;
        if (!policy.cooldownReady()) return Result.ACTION_ON_COOLDOWN;
        if (!policy.statusEligible()) return Result.STATUS_BLOCKED;

        Result targetResult = validateTargets(command, policy);
        if (targetResult != null) return targetResult;

        BattleInstance.CommandResult core = battle.submit(command, action);
        Result mapped = map(core);
        if (mapped == Result.ACCEPTED) consumedCommandIds.add(command.commandId());
        return mapped;
    }

    private Result validateTargets(BattleCommand command, ActionUsePolicy policy) {
        List<String> targets = command.targetIds();
        if (targets.size() < policy.minTargets() || targets.size() > policy.maxTargets()) {
            return Result.INVALID_TARGET_COUNT;
        }
        if (new HashSet<>(targets).size() != targets.size()) return Result.INVALID_TARGET;

        BattleParticipant actor = participants.get(command.actorId());
        if (actor == null) return Result.WRONG_ACTOR;

        for (String targetId : targets) {
            BattleParticipant target = participants.get(targetId);
            if (target == null || !battle.combatState(targetId).alive()) return Result.INVALID_TARGET;
            boolean allowed = switch (policy.targetRule()) {
                case SELF -> target.id().equals(actor.id());
                case ALLY -> target.team() == actor.team();
                case ENEMY -> target.team() != actor.team();
                case ANY -> true;
            };
            if (!allowed) return Result.INVALID_TARGET;
        }
        return null;
    }

    private static Result map(BattleInstance.CommandResult result) {
        return switch (result) {
            case ACCEPTED -> Result.ACCEPTED;
            case WRONG_PHASE -> Result.WRONG_PHASE;
            case STALE_REVISION -> Result.STALE_REVISION;
            case WRONG_ACTOR -> Result.WRONG_ACTOR;
            case ACTION_MISMATCH -> Result.ACTION_MISMATCH;
            case INVALID_ACTION -> Result.INVALID_ACTION;
            case INSUFFICIENT_ENERGY -> Result.INSUFFICIENT_ENERGY;
        };
    }
}
