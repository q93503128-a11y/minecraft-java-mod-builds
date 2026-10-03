package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Exact timing gate for the two Steelboar melee attacks whose impact contracts are fully closed.
 *
 * <p>Iron Rush and Furious Route are rejected here until the missing ordinary guardability and
 * movement/contact presentation contracts are closed.</p>
 */
public final class R01SteelboarMeleeExecutionState {
    private final R01SteelboarEncounterData data;
    private CommittedMelee current;

    public R01SteelboarMeleeExecutionState(
            R01SteelboarEncounterData data
    ) {
        this.data = Objects.requireNonNull(data, "data");
        R01SteelboarEncounterDataLoader.validate(data);
    }

    public boolean begin(
            R01SteelboarActionController.Decision decision,
            long nowTick
    ) {
        Objects.requireNonNull(decision, "decision");
        requireTick(nowTick);
        refresh(nowTick);

        if (current != null
                || decision.reposition()
                || decision.furiousRoute()
                || decision.action().isEmpty()) {
            return false;
        }

        R01SteelboarEncounterData.ActionId action =
                decision.action().orElseThrow();
        if (action == R01SteelboarEncounterData.ActionId.IRON_RUSH) {
            return false;
        }

        R01SteelboarEncounterData.AttackRule rule =
                data.rulesById().get(action);
        if (rule == null || !rule.impactContractClosed()) {
            return false;
        }

        long impactTick = Math.addExact(
                nowTick,
                rule.tellTicks()
        );
        long recoveryEndTick = Math.addExact(
                impactTick,
                rule.recoveryTicks()
        );
        current = new CommittedMelee(
                action,
                decision.actionCounter(),
                nowTick,
                impactTick,
                recoveryEndTick,
                new HashSet<>()
        );
        return true;
    }

    public boolean busy(long nowTick) {
        requireTick(nowTick);
        refresh(nowTick);
        return current != null;
    }

    public Optional<Snapshot> snapshot(long nowTick) {
        requireTick(nowTick);
        refresh(nowTick);
        if (current == null) {
            return Optional.empty();
        }
        return Optional.of(
                new Snapshot(
                        current.action(),
                        current.actionCounter(),
                        phase(current, nowTick),
                        current.startedAtTick(),
                        current.impactTick(),
                        current.recoveryEndTick()
                )
        );
    }

    public Optional<R01SteelboarEncounterData.ActionId> confirmContact(
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
        return Optional.of(current.action());
    }

    private void refresh(long nowTick) {
        if (current == null) {
            return;
        }
        if (nowTick < current.startedAtTick()) {
            throw new IllegalArgumentException(
                    "Steelboar execution time must be monotonic."
            );
        }
        if (nowTick >= current.recoveryEndTick()) {
            current = null;
        }
    }

    private static Phase phase(
            CommittedMelee current,
            long nowTick
    ) {
        if (nowTick < current.impactTick()) {
            return Phase.WIND_UP;
        }
        if (nowTick == current.impactTick()) {
            return Phase.IMPACT_FRAME;
        }
        return Phase.RECOVERY;
    }

    private static void requireTick(long nowTick) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Steelboar execution tick must be non-negative."
            );
        }
    }

    public enum Phase {
        WIND_UP,
        IMPACT_FRAME,
        RECOVERY
    }

    public record Snapshot(
            R01SteelboarEncounterData.ActionId action,
            long actionCounter,
            Phase phase,
            long startedAtTick,
            long impactTick,
            long recoveryEndTick
    ) {
        public Snapshot {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(phase, "phase");
        }
    }

    private record CommittedMelee(
            R01SteelboarEncounterData.ActionId action,
            long actionCounter,
            long startedAtTick,
            long impactTick,
            long recoveryEndTick,
            Set<UUID> hitTargets
    ) {
        private CommittedMelee {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(hitTargets, "hitTargets");
        }
    }
}
