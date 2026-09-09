package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Presentation-only short-lived feedback derived from consecutive authoritative battle snapshots.
 * This class never predicts combat results: it only visualizes deltas the server has already published.
 */
public final class BattleStageFeedbackState {
    static final long HP_FEEDBACK_NANOS = 520_000_000L;
    static final long POISE_FEEDBACK_NANOS = 360_000_000L;
    static final long EXPOSED_FEEDBACK_NANOS = 720_000_000L;
    static final long DEFEAT_FEEDBACK_NANOS = 820_000_000L;

    private static final Object LOCK = new Object();
    private static final Map<String, Entry> ENTRIES = new HashMap<>();
    private static UUID battleId;

    private BattleStageFeedbackState() {}

    public record Cue(
            int hpDelta,
            int poiseDelta,
            double hpStrength,
            double poiseStrength,
            double exposedStrength,
            double defeatStrength
    ) {
        public Cue {
            hpStrength = clampStrength(hpStrength);
            poiseStrength = clampStrength(poiseStrength);
            exposedStrength = clampStrength(exposedStrength);
            defeatStrength = clampStrength(defeatStrength);
        }

        /** Hurt/Poise/EXPOSED recoil strength. Healing deliberately does not shake the model. */
        public double impactStrength() {
            double hpImpact = hpDelta < 0 ? hpStrength : 0.0D;
            return Math.max(Math.max(hpImpact, poiseStrength), exposedStrength);
        }

        public boolean active() {
            return hpStrength > 0.0D || poiseStrength > 0.0D || exposedStrength > 0.0D || defeatStrength > 0.0D;
        }
    }

    public static void acceptSnapshot(
            BattleNetworkPayloads.DecodedSnapshot previous,
            BattleNetworkPayloads.DecodedSnapshot next
    ) {
        acceptSnapshot(previous, next, System.nanoTime());
    }

    static void acceptSnapshot(
            BattleNetworkPayloads.DecodedSnapshot previous,
            BattleNetworkPayloads.DecodedSnapshot next,
            long nowNanos
    ) {
        if (next == null) {
            clear();
            return;
        }
        synchronized (LOCK) {
            if (previous == null || !previous.battleId().equals(next.battleId())) {
                battleId = next.battleId();
                ENTRIES.clear();
                return;
            }
            if (!next.battleId().equals(battleId)) {
                battleId = next.battleId();
                ENTRIES.clear();
            }

            Map<String, BattleNetworkPayloads.SnapshotParticipant> before = new HashMap<>();
            for (BattleNetworkPayloads.SnapshotParticipant participant : previous.participants()) {
                if (participant != null) before.put(participant.id(), participant);
            }

            for (BattleNetworkPayloads.SnapshotParticipant participant : next.participants()) {
                if (participant == null) continue;
                BattleNetworkPayloads.SnapshotParticipant old = before.get(participant.id());
                if (old == null) continue;

                int hpDelta = participant.hp() - old.hp();
                int poiseDelta = participant.poise() - old.poise();
                boolean exposedTriggered = !old.exposed() && participant.exposed();
                boolean defeatedTriggered = old.alive() && !participant.alive();
                if (hpDelta == 0 && poiseDelta == 0 && !exposedTriggered && !defeatedTriggered) continue;

                long impactDelay = BattleActionTimelineState.firstImpactDelayNanos(next.battleId(), participant.id(), nowNanos);
                long feedbackStart = safeAdd(nowNanos, impactDelay);
                Entry entry = ENTRIES.computeIfAbsent(participant.id(), ignored -> new Entry());
                if (hpDelta != 0) {
                    entry.hpDelta = hpDelta;
                    entry.hpStarted = feedbackStart;
                    entry.hpUntil = safeAdd(feedbackStart, HP_FEEDBACK_NANOS);
                }
                if (poiseDelta != 0) {
                    entry.poiseDelta = poiseDelta;
                    entry.poiseStarted = feedbackStart;
                    entry.poiseUntil = safeAdd(feedbackStart, POISE_FEEDBACK_NANOS);
                }
                if (exposedTriggered) {
                    entry.exposedStarted = feedbackStart;
                    entry.exposedUntil = safeAdd(feedbackStart, EXPOSED_FEEDBACK_NANOS);
                }
                if (defeatedTriggered) {
                    entry.defeatStarted = feedbackStart;
                    entry.defeatUntil = safeAdd(feedbackStart, DEFEAT_FEEDBACK_NANOS);
                }
            }
        }
    }

    public static Optional<Cue> cue(UUID expectedBattleId, String participantId) {
        return cue(expectedBattleId, participantId, System.nanoTime());
    }

    static Optional<Cue> cue(UUID expectedBattleId, String participantId, long nowNanos) {
        if (expectedBattleId == null || participantId == null || participantId.isBlank()) return Optional.empty();
        synchronized (LOCK) {
            if (!expectedBattleId.equals(battleId)) return Optional.empty();
            Entry entry = ENTRIES.get(participantId);
            if (entry == null) return Optional.empty();

            double hpStrength = strength(entry.hpStarted, entry.hpUntil, nowNanos);
            double poiseStrength = strength(entry.poiseStarted, entry.poiseUntil, nowNanos);
            double exposedStrength = strength(entry.exposedStarted, entry.exposedUntil, nowNanos);
            double defeatStrength = strength(entry.defeatStarted, entry.defeatUntil, nowNanos);
            Cue cue = new Cue(
                    hpStrength > 0.0D ? entry.hpDelta : 0,
                    poiseStrength > 0.0D ? entry.poiseDelta : 0,
                    hpStrength,
                    poiseStrength,
                    exposedStrength,
                    defeatStrength);
            if (!cue.active()) {
                if (entry.hasFutureCue(nowNanos)) return Optional.empty();
                ENTRIES.remove(participantId);
                return Optional.empty();
            }
            return Optional.of(cue);
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            battleId = null;
            ENTRIES.clear();
        }
    }

    private static double strength(long started, long until, long now) {
        if (until <= started || now < started || now >= until) return 0.0D;
        return clampStrength((until - now) / (double) (until - started));
    }

    private static double clampStrength(double value) {
        if (!Double.isFinite(value)) return 0.0D;
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static long safeAdd(long value, long delta) {
        if (delta > 0L && value > Long.MAX_VALUE - delta) return Long.MAX_VALUE;
        return value + delta;
    }

    private static final class Entry {
        int hpDelta;
        long hpStarted;
        long hpUntil;
        int poiseDelta;
        long poiseStarted;
        long poiseUntil;
        long exposedStarted;
        long exposedUntil;
        long defeatStarted;
        long defeatUntil;

        boolean hasFutureCue(long nowNanos) {
            return (hpStarted > nowNanos && hpUntil > hpStarted)
                    || (poiseStarted > nowNanos && poiseUntil > poiseStarted)
                    || (exposedStarted > nowNanos && exposedUntil > exposedStarted)
                    || (defeatStarted > nowNanos && defeatUntil > defeatStarted);
        }
    }
}
