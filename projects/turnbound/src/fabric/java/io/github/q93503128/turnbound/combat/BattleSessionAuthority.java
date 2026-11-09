package io.github.q93503128.turnbound.combat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * One server-owned battle and its authenticated actor assignment.
 * No Bukkit/Fabric/world objects enter deterministic combat code.
 */
public final class BattleSessionAuthority {
    public enum Status {
        ACCEPTED, NO_SESSION, SESSION_CLOSED, WRONG_WORLD, NOT_PARTICIPANT,
        STALE_REVISION, ACTOR_NOT_OWNED, WRONG_TURN, INVALID_ACTION
    }

    public record Action(long sessionId, long expectedRevision,
                         String actorId, String skillId, List<String> targetIds) {
        public Action {
            if (actorId == null || skillId == null || targetIds == null
                    || actorId.isBlank() || skillId.isBlank()
                    || actorId.length() > 128 || skillId.length() > 128
                    || targetIds.size() > 8) {
                throw new IllegalArgumentException("Malformed battle action");
            }
            targetIds = List.copyOf(targetIds);
            if (targetIds.stream().anyMatch(t -> t == null || t.isBlank() || t.length() > 128)) {
                throw new IllegalArgumentException("Malformed target ID");
            }
        }
    }

    public record SkillView(String id, String name, int cooldown) {}
    public record UnitView(String id, String name, CombatantSide side, boolean summon,
                           int hp, int maxHp, int barrier, long gauge, boolean downed,
                           List<SkillView> ownedSkills) {}
    public record View(long sessionId, long revision, String currentActorId,
                       BattleOutcome outcome, List<UnitView> combatants) {}

    private final long sessionId;
    private final String worldId;
    private final BattleState state;
    private final GenericBattleResolver resolver;
    private final Map<UUID, Set<String>> ownership;
    private long revision = 1L;
    private boolean closed;

    public BattleSessionAuthority(long sessionId, String worldId, BattleState state,
                                  Map<UUID, Set<String>> ownership) {
        if (sessionId <= 0 || worldId == null || worldId.isBlank() || state == null
                || ownership == null || ownership.isEmpty()
                || state.outcome() != BattleOutcome.RUNNING) {
            throw new IllegalArgumentException("Invalid battle authority setup");
        }
        this.sessionId = sessionId;
        this.worldId = worldId;
        this.state = state;
        this.resolver = new GenericBattleResolver(state);

        Map<UUID, Set<String>> checked = new HashMap<>();
        Set<String> assigned = new HashSet<>();
        for (var entry : ownership.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                throw new IllegalArgumentException("Invalid owner");
            }
            Set<String> ids = Set.copyOf(entry.getValue());
            for (String id : ids) {
                CombatantState unit = state.combatant(id);
                if (unit.side() != CombatantSide.ALLY || unit.definition().summon()
                        || !assigned.add(id)) {
                    throw new IllegalArgumentException("Invalid or duplicate assigned combatant");
                }
            }
            checked.put(entry.getKey(), ids);
        }
        for (CombatantState unit : state.combatants()) {
            if (unit.side() == CombatantSide.ALLY && !unit.definition().summon()
                    && !assigned.contains(unit.instanceId())) {
                throw new IllegalArgumentException("All ordinary allies require an owner");
            }
        }
        this.ownership = Map.copyOf(checked);
        resolver.beginTurn();
    }

    public long id() { return sessionId; }
    public String worldId() { return worldId; }
    public long revision() { return revision; }
    public Set<UUID> participants() { return ownership.keySet(); }
    public boolean closed() { return closed; }

    public View viewFor(UUID viewer) {
        if (closed || !ownership.containsKey(viewer)) {
            throw new IllegalArgumentException("Viewer is not a session participant");
        }
        Set<String> own = ownership.get(viewer);
        List<UnitView> units = state.combatants().stream().map(unit -> {
            List<SkillView> skills = own.contains(unit.instanceId())
                    ? unit.definition().skills().stream()
                        .map(skill -> new SkillView(skill.id(), skill.name(), unit.cooldown(skill.id())))
                        .toList()
                    : List.<SkillView>of();
            return new UnitView(unit.instanceId(), unit.definition().name(), unit.side(),
                    unit.definition().summon(), unit.hp(), unit.maxHp(), unit.barrier(),
                    unit.gauge(), unit.downed(), skills);
        }).toList();
        return new View(sessionId, revision, Objects.toString(state.currentActorId(), ""),
                state.outcome(), units);
    }

    /** Must be called by the game server thread, never the networking thread. */
    public Status submit(UUID sender, String senderWorld, Action action) {
        if (closed) return Status.SESSION_CLOSED;
        if (!ownership.containsKey(sender)) return Status.NOT_PARTICIPANT;
        if (!worldId.equals(senderWorld)) return Status.WRONG_WORLD;
        if (action == null || action.sessionId() != sessionId || action.expectedRevision() != revision) {
            return Status.STALE_REVISION;
        }
        if (!ownership.get(sender).contains(action.actorId())) return Status.ACTOR_NOT_OWNED;
        if (!action.actorId().equals(state.currentActorId())) return Status.WRONG_TURN;
        try {
            resolver.resolve(action.actorId(), action.skillId(),
                    action.targetIds().toArray(String[]::new));
        } catch (IllegalArgumentException exception) {
            return Status.INVALID_ACTION;
        }
        revision = Math.incrementExact(revision);
        if (state.outcome() == BattleOutcome.RUNNING) {
            resolver.beginTurn();
        }
        return Status.ACCEPTED;
    }

    /**
     * At most one enemy turn per call. Called from server tick; player decision
     * turns remain paused. An invalid enemy kit is an explicit error, never a
     * silent pass or a simulated attack.
     */
    public boolean advanceEnemyTurn() {
        if (closed || state.outcome() != BattleOutcome.RUNNING) return false;
        if (state.currentActorId() == null) resolver.beginTurn();
        if (!EnemyTurnDirector.actOne(state, resolver)) return false;
        revision = Math.incrementExact(revision);
        if (state.outcome() == BattleOutcome.RUNNING) resolver.beginTurn();
        return true;
    }

    public boolean finished() { return state.outcome() != BattleOutcome.RUNNING; }

    public void close() { closed = true; }
}
