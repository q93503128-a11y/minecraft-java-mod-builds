package io.github.q93503128.turnbound.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Bounded single-actor field encounter claim machine.
 * Does not choose a monster, roster, reward, spawn rate or any game-balance timer.
 * Times and radii come from trusted authored content; not from client packets.
 */
public final class FieldEncounterClaims {
    public enum Phase { IDLE, WARNING, PURSUIT, CLAIMED, COOLDOWN }
    public record Tuning(double noticeRadius, double contactRadius, double leashRadius,
                         int warningTicks, int releaseTicks) {
        public Tuning {
            if (!Double.isFinite(noticeRadius) || !Double.isFinite(contactRadius)
                    || !Double.isFinite(leashRadius)
                    || contactRadius <= 0 || noticeRadius <= contactRadius
                    || leashRadius < noticeRadius || leashRadius > 128
                    || warningTicks < 1 || warningTicks > 1200
                    || releaseTicks < 1 || releaseTicks > 24000) {
                throw new IllegalArgumentException("Invalid authored encounter tuning");
            }
        }
    }
    public record View(Phase phase, UUID target) {}
    public record Step(Phase phase, UUID target, boolean warned, boolean contactClaim) {}

    private static final int MAX_ACTORS = 128;
    private static final class Entry {
        final Tuning tuning;
        Phase phase = Phase.IDLE;
        UUID target;
        long phaseTick;
        Entry(Tuning tuning) { this.tuning = tuning; }
    }
    private final Map<UUID, Entry> actors = new HashMap<>();
    private final Map<UUID, UUID> claimedPlayers = new HashMap<>();

    public void register(UUID actorId, Tuning tuning) {
        Objects.requireNonNull(actorId);
        Objects.requireNonNull(tuning);
        if (actors.size() >= MAX_ACTORS) throw new IllegalStateException("Field actor capacity exceeded");
        if (actors.putIfAbsent(actorId, new Entry(tuning)) != null)
            throw new IllegalStateException("Duplicate encounter representative");
    }

    public boolean registered(UUID actorId) { return actors.containsKey(actorId); }
    public int size() { return actors.size(); }
    public View view(UUID actorId) {
        Entry e = actors.get(actorId);
        return e == null ? null : new View(e.phase, e.target);
    }

    /**
     * Engine/world provides validated distance and visibility; this state machine
     * never queries player position itself. Only a visible, eligible, unclaimed
     * target may begin warning; that exact target must remain valid for contact.
     */
    public Step observe(UUID actorId, UUID candidate, boolean eligible,
                        boolean visible, double distanceSquared, long tick) {
        Entry e = actors.get(actorId);
        if (e == null) throw new IllegalArgumentException("Unregistered field representative");
        if (tick < 0 || !Double.isFinite(distanceSquared) || distanceSquared < 0) {
            throw new IllegalArgumentException("Invalid field observation");
        }
        if (e.phase == Phase.CLAIMED) return new Step(e.phase, e.target, false, false);
        if (e.phase == Phase.COOLDOWN) {
            if (tick >= e.phaseTick && tick-e.phaseTick >= e.tuning.releaseTicks()) reset(e);
            return new Step(e.phase, e.target, false, false);
        }
        boolean available = eligible && candidate != null
                && !claimedPlayers.containsKey(candidate);
        if (e.phase == Phase.IDLE) {
            if (available && visible && distanceSquared <= square(e.tuning.noticeRadius())) {
                e.phase = Phase.WARNING;
                e.target = candidate;
                e.phaseTick = tick;
                return new Step(e.phase, candidate, true, false);
            }
            return new Step(e.phase, null, false, false);
        }
        if (!available || !candidate.equals(e.target)
                || distanceSquared > square(e.tuning.leashRadius())) {
            reset(e);
            return new Step(e.phase, null, false, false);
        }
        if (e.phase == Phase.WARNING) {
            if (tick >= e.phaseTick && tick-e.phaseTick >= e.tuning.warningTicks())
                e.phase = Phase.PURSUIT;
        }
        if (e.phase == Phase.PURSUIT && distanceSquared <= square(e.tuning.contactRadius())) {
            UUID holder = claimedPlayers.putIfAbsent(candidate, actorId);
            if (holder == null) {
                e.phase = Phase.CLAIMED;
                return new Step(e.phase, candidate, false, true);
            }
        }
        return new Step(e.phase, e.target, false, false);
    }

    /** Failed/aborted encounter; not a victory and does not grant anything. */
    public void release(UUID actorId, long tick) {
        Entry e = actors.get(actorId);
        if (e == null) return;
        if (e.target != null) claimedPlayers.remove(e.target, actorId);
        e.phase = Phase.COOLDOWN;
        e.target = null;
        e.phaseTick = Math.max(0, tick);
    }

    public void remove(UUID actorId) {
        Entry e = actors.remove(actorId);
        if (e != null && e.target != null) claimedPlayers.remove(e.target, actorId);
    }

    public void clear() {
        claimedPlayers.clear();
        actors.clear();
    }

    private static double square(double d) { return d*d; }
    private static void reset(Entry e) {
        e.phase = Phase.IDLE;
        e.target = null;
        e.phaseTick = 0;
    }
}
