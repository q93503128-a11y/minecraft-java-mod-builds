package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Exact server timing for Regalhart Antler Sweep, including deterministic mirrored follow-up. */
public final class R01RegalhartSweepExecutionState {
    private final R01RegalhartEncounterData data;
    private CommittedSweep current;

    public R01RegalhartSweepExecutionState(R01RegalhartEncounterData data) {
        this.data = Objects.requireNonNull(data, "data");
        R01RegalhartEncounterDataLoader.validate(data);
    }

    public boolean begin(R01RegalhartActionController.Decision decision, long nowTick) {
        Objects.requireNonNull(decision, "decision");
        requireTick(nowTick);
        refresh(nowTick);
        if (current != null
                || decision.mode() != R01RegalhartActionController.Mode.ATTACK
                || decision.action().orElse(null)
                        != R01RegalhartEncounterData.ActionId.ANTLER_SWEEP) {
            return false;
        }
        var rule = data.rulesById().get(R01RegalhartEncounterData.ActionId.ANTLER_SWEEP);
        if (rule == null || !rule.impactContractClosed()) {
            return false;
        }
        long firstImpactTick = Math.addExact(nowTick, rule.tellTicks());
        long secondImpactTick = decision.mirroredSweepFollowUp()
                ? Math.addExact(firstImpactTick, decision.mirroredSweepFollowUpDelayTicks())
                : 0L;
        long finalImpactTick = secondImpactTick > 0L ? secondImpactTick : firstImpactTick;
        long recoveryEndTick = Math.addExact(finalImpactTick, rule.recoveryTicks());
        current = new CommittedSweep(
                decision.actionCounter(),
                nowTick,
                firstImpactTick,
                secondImpactTick,
                recoveryEndTick,
                new HashSet<>(),
                new HashSet<>()
        );
        return true;
    }

    public Optional<Snapshot> snapshot(long nowTick) {
        requireTick(nowTick);
        refresh(nowTick);
        if (current == null) return Optional.empty();
        Phase phase;
        if (nowTick < current.firstImpactTick()) {
            phase = Phase.WIND_UP;
        } else if (nowTick == current.firstImpactTick()
                || (current.secondImpactTick() > 0L && nowTick == current.secondImpactTick())) {
            phase = Phase.IMPACT_FRAME;
        } else if (current.secondImpactTick() > 0L && nowTick < current.secondImpactTick()) {
            phase = Phase.FOLLOW_UP_GAP;
        } else {
            phase = Phase.RECOVERY;
        }
        return Optional.of(new Snapshot(
                current.actionCounter(),
                phase,
                current.startedAtTick(),
                current.firstImpactTick(),
                current.secondImpactTick(),
                current.recoveryEndTick()
        ));
    }

    public Optional<R01RegalhartEncounterData.ActionId> confirmContact(
            long actionCounter,
            UUID targetId,
            long nowTick
    ) {
        Objects.requireNonNull(targetId, "targetId");
        requireTick(nowTick);
        refresh(nowTick);
        if (current == null || current.actionCounter() != actionCounter) return Optional.empty();
        Set<UUID> hitTargets;
        if (nowTick == current.firstImpactTick()) {
            hitTargets = current.firstHitTargets();
        } else if (current.secondImpactTick() > 0L && nowTick == current.secondImpactTick()) {
            hitTargets = current.secondHitTargets();
        } else {
            return Optional.empty();
        }
        if (!hitTargets.add(targetId)) return Optional.empty();
        return Optional.of(R01RegalhartEncounterData.ActionId.ANTLER_SWEEP);
    }

    private void refresh(long nowTick) {
        if (current == null) return;
        if (nowTick < current.startedAtTick()) {
            throw new IllegalArgumentException("Regalhart sweep execution time must be monotonic.");
        }
        if (nowTick >= current.recoveryEndTick()) current = null;
    }

    private static void requireTick(long nowTick) {
        if (nowTick < 0L) throw new IllegalArgumentException(
                "Regalhart execution tick must be non-negative."
        );
    }

    public enum Phase { WIND_UP, IMPACT_FRAME, FOLLOW_UP_GAP, RECOVERY }

    public record Snapshot(
            long actionCounter,
            Phase phase,
            long startedAtTick,
            long impactTick,
            long secondImpactTick,
            long recoveryEndTick
    ) {
        public Snapshot {
            Objects.requireNonNull(phase, "phase");
            if (secondImpactTick < 0L) throw new IllegalArgumentException(
                    "Regalhart second impact tick cannot be negative."
            );
        }
        public boolean mirroredFollowUp() { return secondImpactTick > 0L; }
    }

    private record CommittedSweep(
            long actionCounter,
            long startedAtTick,
            long firstImpactTick,
            long secondImpactTick,
            long recoveryEndTick,
            Set<UUID> firstHitTargets,
            Set<UUID> secondHitTargets
    ) {
        private CommittedSweep {
            Objects.requireNonNull(firstHitTargets, "firstHitTargets");
            Objects.requireNonNull(secondHitTargets, "secondHitTargets");
        }
    }
}
