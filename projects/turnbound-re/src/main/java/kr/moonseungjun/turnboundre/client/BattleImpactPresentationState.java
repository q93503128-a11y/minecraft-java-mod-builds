package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Presentation-only projection of consecutive authoritative snapshots onto the action impact timeline.
 * Combat truth never changes here: only already-published previous/final HP, Poise, EXPOSED and alive values are staged.
 */
public final class BattleImpactPresentationState {
    private static final Object LOCK = new Object();
    private static final double BOOLEAN_COMMIT_PROGRESS = 0.55D;

    private static UUID battleId;
    private static long revision = Long.MIN_VALUE;
    private static long eventEpoch = Long.MIN_VALUE;
    private static Map<String, Entry> entries = Map.of();

    private BattleImpactPresentationState() {}

    public static void acceptSnapshot(
            BattleNetworkPayloads.DecodedSnapshot previous,
            BattleNetworkPayloads.DecodedSnapshot next,
            long currentEventEpoch
    ) {
        acceptSnapshot(previous, next, currentEventEpoch, System.nanoTime());
    }

    static void acceptSnapshot(
            BattleNetworkPayloads.DecodedSnapshot previous,
            BattleNetworkPayloads.DecodedSnapshot next,
            long currentEventEpoch,
            long nowNanos
    ) {
        if (next == null) {
            clear();
            return;
        }
        synchronized (LOCK) {
            if (previous == null || !previous.battleId().equals(next.battleId())) {
                resetTo(next.battleId(), next.revision(), currentEventEpoch);
                return;
            }
            if (next.revision() <= previous.revision()) {
                if (!next.battleId().equals(battleId)
                        || next.revision() != revision
                        || currentEventEpoch != eventEpoch) {
                    resetTo(next.battleId(), next.revision(), currentEventEpoch);
                }
                return;
            }

            battleId = next.battleId();
            revision = next.revision();
            eventEpoch = currentEventEpoch;
            Map<String, Entry> staged = new HashMap<>();

            if (BattleActionTimelineState.timelineRevision(next.battleId()) != next.revision()) {
                entries = Map.of();
                return;
            }

            Map<String, BattleNetworkPayloads.SnapshotParticipant> before = new HashMap<>();
            for (BattleNetworkPayloads.SnapshotParticipant participant : previous.participants()) {
                if (participant != null) before.put(participant.id(), participant);
            }

            for (BattleNetworkPayloads.SnapshotParticipant participant : next.participants()) {
                if (participant == null) continue;
                BattleNetworkPayloads.SnapshotParticipant old = before.get(participant.id());
                if (old == null || !displayStateChanged(old, participant)) continue;

                ImpactTiming timing = impactTiming(next.battleId(), participant.id(), nowNanos).orElse(null);
                if (timing == null) continue;
                staged.put(participant.id(), new Entry(
                        old.hp(), participant.hp(),
                        old.poise(), participant.poise(),
                        old.exposed(), participant.exposed(),
                        old.alive(), participant.alive(),
                        timing.impactStartNanos(), timing.impactEndNanos()));
            }
            entries = staged.isEmpty() ? Map.of() : Map.copyOf(staged);
        }
    }

    public static BattleNetworkPayloads.DecodedSnapshot project(
            BattleNetworkPayloads.DecodedSnapshot authoritative,
            long currentEventEpoch
    ) {
        return project(authoritative, currentEventEpoch, System.nanoTime());
    }

    static BattleNetworkPayloads.DecodedSnapshot project(
            BattleNetworkPayloads.DecodedSnapshot authoritative,
            long currentEventEpoch,
            long nowNanos
    ) {
        if (authoritative == null) return null;
        synchronized (LOCK) {
            if (!authoritative.battleId().equals(battleId)
                    || authoritative.revision() != revision
                    || currentEventEpoch != eventEpoch
                    || BattleActionTimelineState.timelineRevision(authoritative.battleId()) != revision) {
                entries = Map.of();
                return authoritative;
            }
            if (entries.isEmpty()) return authoritative;

            boolean changed = false;
            List<BattleNetworkPayloads.SnapshotParticipant> projected = new ArrayList<>(authoritative.participants().size());
            for (BattleNetworkPayloads.SnapshotParticipant participant : authoritative.participants()) {
                Entry entry = entries.get(participant.id());
                if (entry == null) {
                    projected.add(participant);
                    continue;
                }
                BattleNetworkPayloads.SnapshotParticipant display = projectParticipant(participant, entry, nowNanos);
                projected.add(display);
                changed |= display != participant;
            }
            if (!changed) return authoritative;
            return new BattleNetworkPayloads.DecodedSnapshot(
                    authoritative.battleId(),
                    authoritative.revision(),
                    authoritative.state(),
                    authoritative.cycle(),
                    authoritative.currentActorId(),
                    List.copyOf(projected),
                    authoritative.availableActions());
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            battleId = null;
            revision = Long.MIN_VALUE;
            eventEpoch = Long.MIN_VALUE;
            entries = Map.of();
        }
    }

    private static Optional<ImpactTiming> impactTiming(UUID expectedBattleId, String participantId, long nowNanos) {
        long delay = BattleActionTimelineState.firstImpactDelayNanos(expectedBattleId, participantId, nowNanos);
        if (delay > 0L) {
            long start = safeAdd(nowNanos, delay);
            return Optional.of(new ImpactTiming(start, safeAdd(start, BattleActionTimelineState.IMPACT_NANOS)));
        }

        BattleActionTimelineState.Cue current = BattleActionTimelineState.cue(expectedBattleId, nowNanos).orElse(null);
        if (current == null || !current.targetIds().contains(participantId)) return Optional.empty();
        if (current.phase() == BattleActionTimelineState.Phase.RECOVERY) return Optional.empty();

        long start;
        if (current.phase() == BattleActionTimelineState.Phase.WINDUP) {
            long remaining = Math.max(0L, Math.round((1.0D - current.phaseProgress()) * BattleActionTimelineState.WINDUP_NANOS));
            start = safeAdd(nowNanos, remaining);
        } else {
            long elapsedImpact = Math.max(0L, Math.round(current.phaseProgress() * BattleActionTimelineState.IMPACT_NANOS));
            start = safeSubtract(nowNanos, elapsedImpact);
        }
        return Optional.of(new ImpactTiming(start, safeAdd(start, BattleActionTimelineState.IMPACT_NANOS)));
    }

    private static BattleNetworkPayloads.SnapshotParticipant projectParticipant(
            BattleNetworkPayloads.SnapshotParticipant authoritative,
            Entry entry,
            long nowNanos
    ) {
        if (nowNanos >= entry.impactEndNanos()) return authoritative;

        double progress = nowNanos <= entry.impactStartNanos()
                ? 0.0D
                : clamp01((nowNanos - entry.impactStartNanos())
                        / (double) Math.max(1L, entry.impactEndNanos() - entry.impactStartNanos()));
        double eased = smoothStep(progress);
        int hp = interpolate(entry.previousHp(), entry.finalHp(), eased);
        int poise = interpolate(entry.previousPoise(), entry.finalPoise(), eased);
        boolean committed = progress >= BOOLEAN_COMMIT_PROGRESS;
        boolean exposed = committed ? entry.finalExposed() : entry.previousExposed();
        boolean alive = committed ? entry.finalAlive() : entry.previousAlive();

        if (hp == authoritative.hp()
                && poise == authoritative.poise()
                && exposed == authoritative.exposed()
                && alive == authoritative.alive()) {
            return authoritative;
        }
        return new BattleNetworkPayloads.SnapshotParticipant(
                authoritative.id(),
                hp,
                authoritative.maxHp(),
                poise,
                authoritative.poiseMax(),
                authoritative.energy(),
                authoritative.guard(),
                exposed,
                authoritative.poiseGuard(),
                alive,
                authoritative.team(),
                authoritative.participantOrdinal(),
                authoritative.characterId(),
                authoritative.statuses(),
                authoritative.intent(),
                authoritative.entityId());
    }

    private static boolean displayStateChanged(
            BattleNetworkPayloads.SnapshotParticipant previous,
            BattleNetworkPayloads.SnapshotParticipant next
    ) {
        return previous.hp() != next.hp()
                || previous.poise() != next.poise()
                || previous.exposed() != next.exposed()
                || previous.alive() != next.alive();
    }

    private static int interpolate(int from, int to, double progress) {
        return (int) Math.round(from + (to - from) * progress);
    }

    private static double smoothStep(double value) {
        double t = clamp01(value);
        return t * t * (3.0D - 2.0D * t);
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) return 0.0D;
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static long safeAdd(long value, long delta) {
        if (delta > 0L && value > Long.MAX_VALUE - delta) return Long.MAX_VALUE;
        return value + delta;
    }

    private static long safeSubtract(long value, long delta) {
        if (delta > 0L && value < Long.MIN_VALUE + delta) return Long.MIN_VALUE;
        return value - delta;
    }

    private static void resetTo(UUID nextBattleId, long nextRevision, long nextEventEpoch) {
        battleId = nextBattleId;
        revision = nextRevision;
        eventEpoch = nextEventEpoch;
        entries = Map.of();
    }

    private record ImpactTiming(long impactStartNanos, long impactEndNanos) {}

    private record Entry(
            int previousHp,
            int finalHp,
            int previousPoise,
            int finalPoise,
            boolean previousExposed,
            boolean finalExposed,
            boolean previousAlive,
            boolean finalAlive,
            long impactStartNanos,
            long impactEndNanos
    ) {}
}
