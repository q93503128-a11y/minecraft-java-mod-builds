package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class R01EarthloongThreatTable {
    private static final double INITIAL_THREAT = 10.0;
    private static final long DECAY_GRACE_TICKS = 120L;
    private static final long DECAY_STEP_TICKS = 20L;
    private static final double DECAY_FACTOR_PER_SECOND = 0.90;
    private static final double ENGAGED_THREAT_FLOOR = 10.0;
    private static final double SWITCH_RATIO = 1.25;

    private final Map<UUID, Entry> entries = new HashMap<>();
    private UUID currentTarget;

    public void engageInitial(UUID playerId, long gameTick) {
        requireTick(gameTick);
        Objects.requireNonNull(playerId, "playerId");
        entries.putIfAbsent(playerId, new Entry(INITIAL_THREAT, gameTick));
    }

    public void addThreat(UUID playerId, double amount, long gameTick) {
        requireTick(gameTick);
        Objects.requireNonNull(playerId, "playerId");
        if (!Double.isFinite(amount) || amount < 0.0) {
            throw new IllegalArgumentException("Threat amount must be finite and non-negative.");
        }
        engageInitial(playerId, gameTick);
        Entry previous = entries.get(playerId);
        entries.put(playerId, new Entry(effectiveThreat(previous, gameTick) + amount, gameTick));
    }

    public Optional<UUID> selectTarget(Collection<UUID> validPlayerIds, long gameTick) {
        requireTick(gameTick);
        Set<UUID> valid = new HashSet<>(Objects.requireNonNull(validPlayerIds, "validPlayerIds"));
        valid.retainAll(entries.keySet());
        if (valid.isEmpty()) {
            currentTarget = null;
            return Optional.empty();
        }

        UUID best = bestThreat(valid, gameTick);
        if (currentTarget == null || !valid.contains(currentTarget)) {
            currentTarget = best;
            return Optional.of(currentTarget);
        }

        if (!currentTarget.equals(best)
                && threatOf(best, gameTick) >= threatOf(currentTarget, gameTick) * SWITCH_RATIO) {
            currentTarget = best;
        }
        return Optional.of(currentTarget);
    }

    public List<UUID> rankedPlayers(Collection<UUID> validPlayerIds, long gameTick) {
        requireTick(gameTick);
        Set<UUID> valid = new HashSet<>(Objects.requireNonNull(validPlayerIds, "validPlayerIds"));
        valid.retainAll(entries.keySet());
        List<UUID> ranked = new ArrayList<>(valid);
        ranked.sort(
                Comparator.<UUID>comparingDouble(id -> threatOf(id, gameTick))
                        .reversed()
                        .thenComparing(UUID::toString)
        );
        return List.copyOf(ranked);
    }

    public double threatOf(UUID playerId, long gameTick) {
        requireTick(gameTick);
        Entry entry = entries.get(Objects.requireNonNull(playerId, "playerId"));
        return entry == null ? 0.0 : effectiveThreat(entry, gameTick);
    }

    public Set<UUID> engagedPlayerIds() {
        return Set.copyOf(entries.keySet());
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public boolean contains(UUID playerId) {
        return entries.containsKey(Objects.requireNonNull(playerId, "playerId"));
    }

    public void remove(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        entries.remove(playerId);
        if (playerId.equals(currentTarget)) {
            currentTarget = null;
        }
    }

    private UUID bestThreat(Set<UUID> valid, long gameTick) {
        UUID best = null;
        double bestValue = Double.NEGATIVE_INFINITY;
        for (UUID candidate : valid) {
            double value = threatOf(candidate, gameTick);
            if (best == null || value > bestValue + 1.0e-9
                    || (Math.abs(value - bestValue) <= 1.0e-9
                    && candidate.toString().compareTo(best.toString()) < 0)) {
                best = candidate;
                bestValue = value;
            }
        }
        return Objects.requireNonNull(best, "best");
    }

    private static double effectiveThreat(Entry entry, long gameTick) {
        long decayStart = entry.lastGeneratedTick() + DECAY_GRACE_TICKS;
        if (gameTick < decayStart + DECAY_STEP_TICKS) {
            return Math.max(ENGAGED_THREAT_FLOOR, entry.storedThreat());
        }
        long steps = (gameTick - decayStart) / DECAY_STEP_TICKS;
        return Math.max(
                ENGAGED_THREAT_FLOOR,
                entry.storedThreat() * Math.pow(DECAY_FACTOR_PER_SECOND, steps));
    }

    private static void requireTick(long gameTick) {
        if (gameTick < 0L) {
            throw new IllegalArgumentException("gameTick must be non-negative.");
        }
    }

    private record Entry(double storedThreat, long lastGeneratedTick) {
    }
}
