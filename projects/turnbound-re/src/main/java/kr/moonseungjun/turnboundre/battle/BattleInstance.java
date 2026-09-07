package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.ActionDefinition;
import kr.moonseungjun.turnboundre.data.StatusDefinition;

import java.util.ArrayList;
import java.util.Comparator;
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

    public enum Outcome {
        ONGOING,
        VICTORY,
        DEFEAT
    }

    private final UUID battleId;
    private final long seed;
    private final Map<String, BattleParticipant> participants;
    private final Map<String, ParticipantCombatState> combatStates;
    private final Map<String, EnemyIntent> enemyIntents = new HashMap<>();
    private final DeterministicBattleRng rng;
    private final List<BattleEvent> eventLog = new ArrayList<>();
    private List<String> actorOrder;
    private BattleState state = BattleState.ENCOUNTER_OPEN;
    private Outcome outcome = Outcome.ONGOING;
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
        for (BattleParticipant participant : participants.values()) {
            if (participant.team() == BattleTeam.ENEMY && combatState(participant.id()).alive()) {
                revealEnemyIntent(participant.id(), EnemyIntent.basic(), false, "battle_start");
            }
        }
        prepareCurrentActor();
    }

    /** Convenience path for the two universal built-in commands. Data-defined actions use submit(command, action). */
    public CommandResult submit(BattleCommand command) {
        ActionDefinition action = switch (command.actionId()) {
            case "basic" -> new ActionDefinition("basic", "BASIC", 0, 0, 0);
            case "guard" -> new ActionDefinition("guard", "GUARD", 0, 0, 0);
            default -> null;
        };
        if (action == null) return CommandResult.INVALID_ACTION;
        return submit(command, action);
    }

    /** Failed validation never changes revision, event log, Energy, Guard or phase. */
    public CommandResult submit(BattleCommand command, ActionDefinition action) {
        if (state != BattleState.AWAIT_COMMAND) return CommandResult.WRONG_PHASE;
        if (command.expectedRevision() != revision) return CommandResult.STALE_REVISION;
        if (!currentActorId().equals(command.actorId())) return CommandResult.WRONG_ACTOR;
        BattleParticipant actor = participants.get(command.actorId());
        if (actor.team() != BattleTeam.PLAYER || !combatState(actor.id()).alive()) return CommandResult.WRONG_ACTOR;
        if (action == null || !command.actionId().equals(action.id())) return CommandResult.ACTION_MISMATCH;

        String kind = action.kind();
        if (!(kind.equals("BASIC") || kind.equals("SKILL") || kind.equals("GUARD") || kind.equals("BURST"))) {
            return CommandResult.INVALID_ACTION;
        }
        if (action.energyCost() < 0) return CommandResult.INVALID_ACTION;
        if ((kind.equals("BASIC") || kind.equals("GUARD")) && action.energyCost() != 0) return CommandResult.INVALID_ACTION;

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
        if (!combatState(actor.id()).alive()) throw new IllegalStateException("defeated enemy cannot resolve: " + actor.id());
        EnemyIntent intent = enemyIntents.get(actor.id());
        if (intent == null) throw new IllegalStateException("enemy has no published intent: " + actor.id());
        revision++;
        eventLog.add(new BattleEvent(revision, "AI_COMMAND", actor.id(), intent.actionId()));
        if ("recover".equals(intent.actionId())) emit("RECOVER", actor.id(), "poise=" + combatState(actor.id()).poise());
    }

    public void setEnemyIntent(String enemyId, EnemyIntent intent) {
        BattleParticipant participant = participants.get(enemyId);
        if (participant == null || participant.team() != BattleTeam.ENEMY) {
            throw new IllegalArgumentException("intent target must be an enemy participant: " + enemyId);
        }
        if (intent == null) throw new IllegalArgumentException("intent must not be null");
        if (!combatState(enemyId).alive()) throw new IllegalArgumentException("defeated enemy cannot receive intent: " + enemyId);
        revealEnemyIntent(enemyId, intent, enemyIntents.containsKey(enemyId), "script");
    }

    public EnemyIntent enemyIntent(String enemyId) {
        EnemyIntent intent = enemyIntents.get(enemyId);
        if (intent == null) throw new IllegalArgumentException("enemy has no published intent: " + enemyId);
        return intent;
    }

    private void revealEnemyIntent(String enemyId, EnemyIntent intent, boolean changed, String reason) {
        EnemyIntent previous = enemyIntents.put(enemyId, intent);
        if (changed && previous != null) {
            emit("INTENT_CHANGED", enemyId, previous.actionId() + "->" + intent.actionId() + " reason=" + reason);
        } else {
            emit("INTENT_REVEALED", enemyId, intent.actionId() + " type=" + intent.type() + " target=" + intent.targeting()
                    + " risk=" + intent.risk() + " breakCancelable=" + intent.breakCancelable());
        }
    }

    public DamageService.DamageResult resolveDamage(String actorId, String targetId, DamageService.DamageRequest request) {
        requireState(BattleState.RESOLVING);
        if (!participants.containsKey(actorId)) throw new IllegalArgumentException("unknown actor: " + actorId);
        if (!combatState(actorId).alive()) throw new IllegalArgumentException("actor is not alive: " + actorId);
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
        if (brokePoise) {
            emit("EXPOSED_APPLIED", targetId, "poise=0");
            applyPoiseBreakToIntent(targetId);
        }
        markDefeatedIfNeeded(targetId);
        return result;
    }

    void resolvePoiseOnly(String actorId, String targetId, AffinityGrade affinity, int poisePower, String actionId) {
        requireState(BattleState.RESOLVING);
        if (affinity == null) throw new IllegalArgumentException("affinity must not be null");
        ParticipantCombatState target = combatState(targetId);
        if (!target.alive()) throw new IllegalArgumentException("target is not alive: " + targetId);
        int damage = affinity == AffinityGrade.IMMUNE
                ? 0 : Math.max(0, (int) Math.floor(poisePower * affinity.poiseMultiplier()));
        boolean broke = target.applyPoiseDamage(damage);
        revision++;
        emit("POISE_DAMAGE", actorId, "target=" + targetId + " amount=" + damage + " action=" + actionId);
        if (broke) {
            emit("EXPOSED_APPLIED", targetId, "poise=0");
            applyPoiseBreakToIntent(targetId);
        }
    }

    void resolveHeal(String actorId, String targetId, int amount, String actionId) {
        requireState(BattleState.RESOLVING);
        ParticipantCombatState target = combatState(targetId);
        if (!target.alive()) throw new IllegalArgumentException("cannot heal defeated target: " + targetId);
        int restored = target.heal(amount);
        revision++;
        emit("HEAL", actorId, "target=" + targetId + " amount=" + restored + " action=" + actionId);
    }

    void applyDataStatus(String actorId, String targetId, StatusDefinition definition, int durationOverride) {
        requireState(BattleState.RESOLVING);
        ParticipantCombatState target = combatState(targetId);
        if (!target.alive()) throw new IllegalArgumentException("cannot apply status to defeated target: " + targetId);
        StatusService.apply(target.statuses(), definition, 1, durationOverride);
        revision++;
        emit("STATUS_APPLIED", actorId, "target=" + targetId + " status=" + definition.id()
                + " stacks=" + target.statuses().stacks(definition.id()) + " remaining=" + target.statuses().remaining(definition.id()));
        processStatusHooks(targetId, "ON_APPLY");
    }

    void removeDataStatus(String actorId, String targetId, String statusId) {
        requireState(BattleState.RESOLVING);
        boolean removed = StatusService.remove(combatState(targetId).statuses(), statusId);
        revision++;
        emit("STATUS_REMOVED", actorId, "target=" + targetId + " status=" + statusId + " removed=" + removed);
    }

    void adjustEnergy(String actorId, String targetId, int delta, String actionId) {
        requireState(BattleState.RESOLVING);
        ParticipantCombatState target = combatState(targetId);
        int before = target.energy();
        target.adjustEnergy(delta);
        revision++;
        emit("ENERGY_CHANGED", actorId, "target=" + targetId + " " + before + "->" + target.energy()
                + " reason=EFFECT action=" + actionId);
    }

    void delayEnemyIntent(String actorId, String targetId, String actionId) {
        requireState(BattleState.RESOLVING);
        BattleParticipant target = participant(targetId);
        if (target.team() != BattleTeam.ENEMY || !combatState(targetId).alive()) {
            throw new IllegalArgumentException("INTENT_DELAY target must be a living enemy: " + targetId);
        }
        EnemyIntent current = enemyIntents.get(targetId);
        if (current == null) throw new IllegalStateException("enemy has no published intent: " + targetId);
        revision++;
        if ("recover".equals(current.actionId())) {
            emit("INTENT_DELAY_NOOP", actorId, "target=" + targetId + " action=" + actionId);
        } else {
            revealEnemyIntent(targetId, EnemyIntent.recover(), true, "action_delay:" + actionId);
        }
    }

    boolean rollEffectChance(double chance) {
        if (!Double.isFinite(chance) || chance < 0.0D || chance > 1.0D) {
            throw new IllegalArgumentException("chance must be finite in [0,1]");
        }
        if (chance <= 0.0D) return false;
        if (chance >= 1.0D) return true;
        return rng.nextUnitDouble() < chance;
    }

    void recordEffectEvent(String type, String actorId, String detail) {
        emit(type, actorId, detail);
    }

    private void applyPoiseBreakToIntent(String targetId) {
        BattleParticipant target = participants.get(targetId);
        if (target == null || target.team() != BattleTeam.ENEMY) return;
        EnemyIntent current = enemyIntents.get(targetId);
        if (current == null) return;
        if (current.breakCancelable()) {
            revealEnemyIntent(targetId, EnemyIntent.recover(), true, "poise_break_cancel");
        } else if (current.breakDowngradeAction() != null) {
            revealEnemyIntent(targetId, current.withAction(current.breakDowngradeAction(), EnemyIntent.Risk.NORMAL),
                    true, "poise_break_downgrade");
        }
    }

    public void finishResolution() {
        requireState(BattleState.RESOLVING);
        String resolvedActorId = currentActorId();
        BattleParticipant resolvedActor = participants.get(resolvedActorId);

        processStatusHooks(resolvedActorId, "TURN_END");
        expireStatuses(resolvedActorId, "TURN");
        transition(BattleState.CHECK_END);

        Outcome checked = checkOutcome();
        if (checked != Outcome.ONGOING) {
            settleOutcome(checked);
            return;
        }

        if (resolvedActor.team() == BattleTeam.ENEMY && combatState(resolvedActorId).alive()) {
            enemyIntents.remove(resolvedActorId);
            revealEnemyIntent(resolvedActorId, EnemyIntent.basic(), false, "next_turn");
        }

        advanceToNextLivingActor();
        prepareCurrentActor();
    }

    private void processStatusHooks(String participantId, String when) {
        ParticipantCombatState target = combatState(participantId);
        List<StatusRuntime.TriggeredHook> hooks = target.statuses().hooks(when);
        for (StatusRuntime.TriggeredHook triggered : hooks) {
            String type = triggered.hook().effect().type();
            double value = triggered.hook().effect().value();
            int stacks = triggered.stacks();
            switch (type) {
                case "DAMAGE_MAX_HP_PERCENT" -> {
                    int amount = Math.max(0, (int) Math.floor(target.maxHp() * value * stacks));
                    target.applyHpDamage(amount);
                    revision++;
                    emit("STATUS_DAMAGE", participantId, "status=" + triggered.statusId() + " amount=" + amount + " timing=" + when);
                    markDefeatedIfNeeded(participantId);
                }
                case "DAMAGE_FLAT" -> {
                    int amount = Math.max(0, (int) Math.floor(value * stacks));
                    target.applyHpDamage(amount);
                    revision++;
                    emit("STATUS_DAMAGE", participantId, "status=" + triggered.statusId() + " amount=" + amount + " timing=" + when);
                    markDefeatedIfNeeded(participantId);
                }
                case "HEAL_MAX_HP_PERCENT" -> {
                    if (!target.alive()) break;
                    int amount = Math.max(0, (int) Math.floor(target.maxHp() * value * stacks));
                    int restored = target.heal(amount);
                    revision++;
                    emit("STATUS_HEAL", participantId, "status=" + triggered.statusId() + " amount=" + restored + " timing=" + when);
                }
                case "ATK_MULTIPLIER", "DEF_MULTIPLIER", "SPD_MULTIPLIER", "DAMAGE_TAKEN_MULTIPLIER", "POISE_TAKEN_MULTIPLIER", "NONE" -> {
                    // Continuous modifiers are queried by the action/initiative resolver while the status is active.
                }
                default -> throw new IllegalStateException("validated status has unsupported hook effect " + type);
            }
        }
    }

    private void expireStatuses(String participantId, String durationUnit) {
        for (String expired : combatState(participantId).statuses().tick(durationUnit)) {
            emit("STATUS_EXPIRED", participantId, expired + " unit=" + durationUnit);
        }
    }

    private void processCycleStatuses() {
        for (BattleParticipant participant : participants.values()) {
            if (!combatState(participant.id()).alive()) continue;
            processStatusHooks(participant.id(), "CYCLE_START");
            expireStatuses(participant.id(), "CYCLE");
        }
    }

    private void markDefeatedIfNeeded(String participantId) {
        ParticipantCombatState target = combatState(participantId);
        if (target.alive()) return;
        enemyIntents.remove(participantId);
        boolean alreadyLogged = eventLog.stream().anyMatch(event ->
                "PARTICIPANT_DEFEATED".equals(event.type()) && participantId.equals(event.actorId()));
        if (!alreadyLogged) emit("PARTICIPANT_DEFEATED", participantId, "hp=0");
    }

    private Outcome checkOutcome() {
        boolean playerAlive = hasLiving(BattleTeam.PLAYER);
        boolean enemyAlive = hasLiving(BattleTeam.ENEMY);
        if (!enemyAlive) return Outcome.VICTORY;
        if (!playerAlive) return Outcome.DEFEAT;
        return Outcome.ONGOING;
    }

    private boolean hasLiving(BattleTeam team) {
        for (BattleParticipant participant : participants.values()) {
            if (participant.team() == team && combatState(participant.id()).alive()) return true;
        }
        return false;
    }

    private void settleOutcome(Outcome checked) {
        outcome = checked;
        BattleState resultState = checked == Outcome.VICTORY ? BattleState.VICTORY : BattleState.DEFEAT;
        transition(resultState);
        emit("BATTLE_RESULT", "", checked.name());
        transition(BattleState.REWARD);
        emit("REWARD_READY", "", "outcome=" + checked.name());
    }

    public void cleanup() {
        requireState(BattleState.REWARD);
        transition(BattleState.CLEANUP);
        combatStates.values().forEach(ParticipantCombatState::resetBattleResources);
        enemyIntents.clear();
        emit("CLEANUP_COMPLETE", "", "energy=0 statuses=cleared");
        transition(BattleState.NOT_IN_BATTLE);
    }

    private void advanceToNextLivingActor() {
        int inspected = 0;
        while (inspected < actorOrder.size()) {
            actorIndex++;
            if (actorIndex >= actorOrder.size()) {
                cycle++;
                actorIndex = 0;
                processCycleStatuses();
                actorOrder = effectiveInitiativeOrder();
                emit("CYCLE_STARTED", "", Integer.toString(cycle));
            }
            inspected++;
            if (combatState(currentActorId()).alive()) return;
            emit("DEFEATED_ACTOR_SKIPPED", currentActorId(), "cycle=" + cycle);
        }
        throw new IllegalStateException("no living actor found while battle outcome is ongoing");
    }

    private List<String> effectiveInitiativeOrder() {
        return participants.values().stream()
                .sorted(Comparator
                        .comparingDouble((BattleParticipant participant) -> effectiveSpeed(participant)).reversed()
                        .thenComparingInt(BattleParticipant::participantOrdinal))
                .map(BattleParticipant::id)
                .toList();
    }

    private double effectiveSpeed(BattleParticipant participant) {
        return participant.speed() * combatState(participant.id()).statuses().multiplier("SPD_MULTIPLIER");
    }

    private void prepareCurrentActor() {
        if (!combatState(currentActorId()).alive()) throw new IllegalStateException("cannot prepare defeated actor: " + currentActorId());
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

    public BattleParticipant participant(String participantId) {
        BattleParticipant participant = participants.get(participantId);
        if (participant == null) throw new IllegalArgumentException("unknown participant: " + participantId);
        return participant;
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
    public Outcome outcome() { return outcome; }
    public String currentActorId() { return actorOrder.get(actorIndex); }
    public List<String> actorOrder() { return List.copyOf(actorOrder); }
    public List<BattleEvent> eventLog() { return List.copyOf(eventLog); }
    public long rngDraws() { return rng.draws(); }
}
