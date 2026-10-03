package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Exact server timing for a non-combo Antler Sweep.
 *
 * <p>A sweep whose controller decision carries a mirrored follow-up is rejected here because current
 * canon closes the follow-up cadence/legality but not the second hit's exact timing. That prevents a
 * partial combo implementation from becoming production behavior.</p>
 */
public final class R01RegalhartSweepExecutionState {
    private final R01RegalhartEncounterData data;
    private CommittedSweep current;

    public R01RegalhartSweepExecutionState(
            R01RegalhartEncounterData data
    ) {
        this.data = Objects.requireNonNull(data, "data");
        R01RegalhartEncounterDataLoader.validate(data);
    }

    public boolean begin(
            R01RegalhartActionController.Decision decision,
            long nowTick
    ) {
        Objects.requireNonNull(decision, "decision");
        requireTick(nowTick);
        refresh(nowTick);

        if (current != null
                || decision.mode() != R01RegalhartActionController.Mode.ATTACK
                || decision.mirroredSweepFollowUp()
                || decision.action().orElse(null)
                        != R01RegalhartEncounterData.ActionId.ANTLER_SWEEP) {
            return false;
        }

        var rule = data.rulesById().get(
                R01RegalhartEncounterData.ActionId.ANTLER_SWEEP
        );
        if (rule == null || !rule.impactContractClosed()) {
            return false;
        }

        long impactTick = Math.addExact(nowTick, rule.tellTicks());
        long recoveryEndTick = Math.addExact(
                impactTick,
                rule.recoveryTicks()
        );
        current = new CommittedSweep(
                decision.actionCounter(),
                nowTick,
                impactTick,
                recoveryEndTick,
                new HashSet<>()
        );
        return true;
    }

    public Optional<Snapshot> snapshot(long nowTick) {
        requireTick(nowTick);
        refresh(nowTick);
        if (current == null) {
            return Optional.empty();
        }
        Phase phase = nowTick < current.impactTick()
                ? Phase.WIND_UP
                : nowTick == current.impactTick()
                        ? Phase.IMPACT_FRAME
                        : Phase.RECOVERY;
        return Optional.of(
                new Snapshot(
                        current.actionCounter(),
                        phase,
                        current.startedAtTick(),
                        current.impactTick(),
                        current.recoveryEndTick()
                )
        );
    }

    public Optional<R01RegalhartEncounterData.ActionId> confirmContact(
            long actionCounter,
            UUID targetId,
            long nowTick
    ) {
        Objects.requireNonNull(targetId, "targetId");
        requireTick(nowTick);
        refresh(nowTick);
        if (current == null
                || current.actionCounter() != actionCounter
                || nowTick != current.impactTick()
                || !current.hitTargets().add(targetId)) {
            return Optional.empty();
        }
        return Optional.of(
                R01RegalhartEncounterData.ActionId.ANTLER_SWEEP
        );
    }

    private void refresh(long nowTick) {
        if (current == null) {
            return;
        }
        if (nowTick < current.startedAtTick()) {
            throw new IllegalArgumentException(
                    "Regalhart sweep execution time must be monotonic."
            );
        }
        if (nowTick >= current.recoveryEndTick()) {
            current = null;
        }
    }

    private static void requireTick(long nowTick) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Regalhart execution tick must be non-negative."
            );
        }
    }

    public enum Phase {
        WIND_UP,
        IMPACT_FRAME,
        RECOVERY
    }

    public record Snapshot(
            long actionCounter,
            Phase phase,
            long startedAtTick,
            long impactTick,
            long recoveryEndTick
    ) {
        public Snapshot {
            Objects.requireNonNull(phase, "phase");
        }
    }

    private record CommittedSweep(
            long actionCounter,
            long startedAtTick,
            long impactTick,
            long recoveryEndTick,
            Set<UUID> hitTargets
    ) {
        private CommittedSweep {
            Objects.requireNonNull(hitTargets, "hitTargets");
        }
    }
}
