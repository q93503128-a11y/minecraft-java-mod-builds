package kr.moonseungjun.turnboundre.battle;

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
        WRONG_ACTOR
    }

    private final UUID battleId;
    private final long seed;
    private final Map<String, BattleParticipant> participants;
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

    public CommandResult submit(BattleCommand command) {
        if (state != BattleState.AWAIT_COMMAND) return CommandResult.WRONG_PHASE;
        if (command.expectedRevision() != revision) return CommandResult.STALE_REVISION;
        if (!currentActorId().equals(command.actorId())) return CommandResult.WRONG_ACTOR;
        BattleParticipant actor = participants.get(command.actorId());
        if (actor.team() != BattleTeam.PLAYER) return CommandResult.WRONG_ACTOR;
        revision++;
        state = BattleState.RESOLVING;
        eventLog.add(new BattleEvent(revision, "COMMAND_ACCEPTED", actor.id(), command.actionId()));
        return CommandResult.ACCEPTED;
    }

    public void resolveEnemyStub() {
        if (state != BattleState.RESOLVING) throw new IllegalStateException("enemy resolution requires RESOLVING");
        BattleParticipant actor = participants.get(currentActorId());
        if (actor.team() != BattleTeam.ENEMY) throw new IllegalStateException("current actor is not ENEMY");
        revision++;
        eventLog.add(new BattleEvent(revision, "AI_COMMAND", actor.id(), "basic"));
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

    public UUID battleId() { return battleId; }
    public long seed() { return seed; }
    public long revision() { return revision; }
    public int cycle() { return cycle; }
    public BattleState state() { return state; }
    public String currentActorId() { return actorOrder.get(actorIndex); }
    public List<String> actorOrder() { return List.copyOf(actorOrder); }
    public List<BattleEvent> eventLog() { return List.copyOf(eventLog); }
}
