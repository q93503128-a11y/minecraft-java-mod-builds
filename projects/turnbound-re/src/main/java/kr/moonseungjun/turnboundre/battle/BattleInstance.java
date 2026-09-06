package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.ActionDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BattleInstance {
    public enum CommandResult {
        ACCEPTED,
        WRONG_PHASE,
        STALE_REVISION,
        WRONG_ACTOR,
        ACTION_MISMATCH,
        INVALID_ACTION,
        INSUFFICIENT_ENERGY
    }

    private final UUID battleId;
    private final long seed;
    private final Map<String, BattleParticipant> participants;
    private final Map<String, ParticipantCombatState> combatStates;
    private final DeterministicBattleRng rng;
    private final List<BattleEvent> eventLog = new ArrayList<>();
    private List<String> actorOrder;
    private BattleState state = BattleState.ENCOUNTER_OPEN;
    private long revision;
    private int cycle = 1;
    private int actorIndex;

    public BattleInstance(UUID battleId, long seed, List<BattleParticipant> participants) {
        if (battleId == null) throw new IllegalArgumentException("battleId must not be null");
        if (participants == null || participants.isEmpty()) throw new IllegalArgumentException("participants must not be empty");
        this.battleId = battleId;
        this.seed = seed;
        this.participants = validatedParticipants(participants);
        Map<String, ParticipantCombatState> mutable = new HashMap<>();
        this.participants.forEach((id, participant) -> mutable.put(id, new ParticipantCombatState(participant)));
        this.combatStates = mutable;
        this.rng = new DeterministicBattleRng(seed);
        this.actorOrder = InitiativeService.order(participants);
        emit("BATTLE_OPEN", "", "seed=" + seed);
    }

    private static Map<String, BattleParticipant> validatedParticipants(List<BattleParticipant> participants) {
        Map<String, BattleParticipant> byId = new HashMap<>();
        Set<Integer> ordinals = new HashSet<>();
        boolean player = false;
        boolean enemy = false;
        for (BattleParticipant participant : participants) {
            if (participant == null) throw new IllegalArgumentException("participant must not be null");
            if (byId.putIfAbsent(participant.id(), participant) != null) {
                throw new IllegalArgumentException("duplicate participant id: " + participant.id());
            }
            if (!ordinals.add(participant.participantOrdinal())) {
                throw new IllegalArgumentException("duplicate participantOrdinal: " + participant.participantOrdinal());
            }
            player |= participant.team() == BattleTeam.PLAYER;
            enemy |= participant.team() == BattleTeam.ENEMY;
        }
        if (!player || !enemy) throw new IllegalArgumentException("battle requires both PLAYER and ENEMY participants");
        return Map.copyOf(byId);
    }

    public void start() {
        requireState(BattleState.ENCOUNTER_OPEN);
        transition(BattleState.INTRO);
        prepareCurrentActor();
    }

    /** Convenience path for the two universal built-in commands. Data-defined Skill/Burst uses submit(command, action). */
    public CommandResult submit(BattleCommand command) {
        ActionDefinition action = switch (command.actionId()) {
            case "basic" -> new ActionDefinition("basic", "BASIC", 0, 0, 0);
            case "guard" -> new ActionDefinition("guard", "GUARD", 0, 0, 0);
            default -> null;
        };
        if (action == null) return CommandResult.INVALID_ACTION;
        return submit(command, action);
    }

    /**
     * Server-authoritative command acceptance for current M1 action semantics.
     * Failed validation never changes revision, event log, Energy, Guard or phase.
     */
    public CommandResult submit(BattleCommand command, ActionDefinition action) {
        if (state != BattleState.AWAIT_COMMAND) return CommandResult.WRONG_PHASE;
        if (command.expectedRevision() != revision) return CommandResult.STALE_REVISION;
        if (!currentActorId().equals(command.actorId())) return CommandResult.WRONG_ACTOR;
        BattleParticipant actor = participants.get(command.actorId());
        if (actor.team() != BattleTeam.PLAYER) return CommandResult.WRONG_ACTOR;
        if (action == null || !command.actionId().equals(action.id())) return CommandResult.ACTION_MISMATCH;

        String kind = action.kind();
        if (!(kind.equals("BASIC") || kind.equals("SKILL") || kind.equals("GUARD") || kind.equals("BURST"))) {
            return CommandResult.INVALID_ACTION;
        }
        if (action.energyCost() < 0) return CommandResult.INVALID_ACTION;
        if ((kind.equals("BASIC") || kind.equals("GUARD")) && action.energyCost() != 0) {
            return CommandResult.INVALID_ACTION;
        }

        ParticipantCombatState actorState = combatState(actor.id());
        if ((kind.equals("SKILL") || kind.equals("BURST")) && actorState.energy() < action.energyCost()) {
            return CommandResult.INSUFFICIENT_ENERGY;
        }

        revision++;
        state = BattleState.RESOLVING;
        eventLog.add(new BattleEvent(revision, "COMMAND_ACCEPTED", actor.id(), command.actionId()));
        applyActionResourceSemantics(actor.id(), actorState, action);
        return CommandResult.ACCEPTED;
    }

    private void applyActionResourceSemantics(String actorId, ParticipantCombatState actorState, ActionDefinition action) {
        switch (action.kind()) {
            case "BASIC" -> {
                int before = actorState.energy();
                actorState.gainEnergy(10);
                emit("ENERGY_CHANGED", actorId, before + "->" + actorState.energy() + " reason=BASIC");
            }
            case "GUARD" -> {
                int before = actorState.energy();
                actorState.setGuard(true);
                actorState.gainEnergy(15);
                emit("GUARD_APPLIED", actorId, "finalDamage=0.5 until=next_turn_start");
                emit("ENERGY_CHANGED", actorId, before + "->" + actorState.energy() + " reason=GUARD");
            }
            case "SKILL", "BURST" -> {
                int before = actorState.energy();
                if (!actorState.spendEnergy(action.energyCost())) {
                    throw new IllegalStateException("energy changed after command validation");
                }
                emit("ENERGY_CHANGED", actorId,
                        before + "->" + actorState.energy() + " reason=" + action.kind() + " cost=" + action.energyCost());
            }
            default -> throw new IllegalStateException("unsupported action kind after validation: " + action.kind());
        }
    }

    public void resolveEnemyStub() {
        if (state != BattleState.RESOLVING) throw new IllegalStateException("enemy resolution requires RESOLVING");
        BattleParticipant actor = participants.get(currentActorId());
        if (actor.team() != BattleTeam.ENEMY) throw new IllegalStateException("current actor is not ENEMY");
        revision++;
        eventLog.add(new BattleEvent(revision, "AI_COMMAND", actor.id(), "basic"));
    }

    /** Applies canonical deterministic damage using this battle's single RNG stream and mutable target state. */
    public DamageService.DamageResult resolveDamage(String actorId, String targetId, DamageService.DamageRequest request) {
        requireState(BattleState.RESOLVING);
        if (!participants.containsKey(actorId)) throw new IllegalArgumentException("unknown actor: " + actorId);
        ParticipantCombatState target = combatState(targetId);
        if (!target.alive()) throw new IllegalArgumentException("target is not alive: " + targetId);

        DamageService.DamageRequest canonicalRequest = new DamageService.DamageRequest(
                request.tag(), request.affinity(), request.hpPower(), request.poisePower(), request.attack(), request.defense(),
                target.exposed(), request.criticalChance(), request.criticalMultiplier(), request.otherMultiplier());
        DamageService.DamageResult result = DamageService.resolve(canonicalRequest, rng);

        int hpDamage = target.guard() ? (int) Math.floor(result.finalDamage() * 0.5D) : result.finalDamage();
        target.applyHpDamage(hpDamage);
        boolean brokePoise = target.applyPoiseDamage(result.poiseDamage());

        revision++;
        emit("DAMAGE", actorId, "target=" + targetId + " hp=" + hpDamage + " " + result.breakdown());
        if (brokePoise) emit("EXPOSED_APPLIED", targetId, "poise=0");
        if (!target.alive()) emit("PARTICIPANT_DEFEATED", targetId, "hp=0");
        return result;
    }

    public void finishResolution() {
        requireState(BattleState.RESOLVING);
        transition(BattleState.CHECK_END);
        actorIndex++;
        if (actorIndex >= actorOrder.size()) {
            cycle++;
            actorIndex = 0;
            actorOrder = InitiativeService.order(List.copyOf(participants.values()));
            emit("CYCLE_STARTED", "", Integer.toString(cycle));
        }
        prepareCurrentActor();
    }

    private void prepareCurrentActor() {
        transition(BattleState.ACTOR_READY);
        ParticipantCombatState actorState = combatState(currentActorId());
        if (actorState.exposed()) {
            actorState.recoverAtTurnStartIfExposed();
            emit("EXPOSED_RECOVERED", currentActorId(), "poise=" + actorState.poise());
            emit("POISE_GUARD_APPLIED", currentActorId(), "mult=0.5");
        } else if (actorState.poiseGuard()) {
            actorState.expirePoiseGuardAtTurnStart();
            emit("POISE_GUARD_EXPIRED", currentActorId(), "");
        }
        if (actorState.guard()) emit("GUARD_EXPIRED", currentActorId(), "at=turn_start");
        actorState.setGuard(false);
        BattleParticipant actor = participants.get(currentActorId());
        if (actor.team() == BattleTeam.PLAYER) transition(BattleState.AWAIT_COMMAND);
        else transition(BattleState.RESOLVING);
    }

    private void requireState(BattleState required) {
        if (state != required) throw new IllegalStateException("required state " + required + " but was " + state);
    }

    private void transition(BattleState next) {
        state = next;
        emit("STATE_CHANGED", currentActorIdOrBlank(), next.name());
    }

    private void emit(String type, String actorId, String detail) {
        eventLog.add(new BattleEvent(revision, type, actorId, detail));
    }

    private String currentActorIdOrBlank() {
        return actorOrder.isEmpty() ? "" : actorOrder.get(actorIndex);
    }

    public ParticipantCombatState combatState(String participantId) {
        ParticipantCombatState state = combatStates.get(participantId);
        if (state == null) throw new IllegalArgumentException("unknown participant: " + participantId);
        return state;
    }

    public UUID battleId() { return battleId; }
    public long seed() { return seed; }
    public long revision() { return revision; }
    public int cycle() { return cycle; }
    public BattleState state() { return state; }
    public String currentActorId() { return actorOrder.get(actorIndex); }
    public List<String> actorOrder() { return List.copyOf(actorOrder); }
    public List<BattleEvent> eventLog() { return List.copyOf(eventLog); }
    public long rngDraws() { return rng.draws(); }
}
