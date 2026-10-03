package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Server-owned timing gate between a committed Nature Spirit decision and presentation-confirmed
 * contacts.
 *
 * <p>The encounter data already closes wind-up, recovery, committed movement distance and Bloom
 * Quake radius. This state owns those values and the exact impact frame. It deliberately does not
 * invent the missing animation, frontal-arc shape, ram path interpolation or ground-ring VFX.</p>
 */
public final class R01NatureSpiritActionExecutionState {
    private final R01SecondaryCreatureEncounterData data;
    private CommittedAction current;

    public R01NatureSpiritActionExecutionState(
            R01SecondaryCreatureEncounterData data
    ) {
        this.data = Objects.requireNonNull(data, "data");
        R01SecondaryCreatureEncounterDataLoader.validate(data);
        if (data.kind()
                != R01SecondaryCreatureEncounterData.CreatureKind.NATURE_SPIRIT) {
            throw new IllegalArgumentException(
                    "Expected Nature Spirit encounter data."
            );
        }
    }

    public boolean begin(
            R01NatureSpiritActionController.Decision decision,
            long nowTick
    ) {
        Objects.requireNonNull(decision, "decision");
        requireTick(nowTick);
        refresh(nowTick);

        if (current != null
                || decision.mode()
                        != R01NatureSpiritActionController.Mode.ATTACK) {
            return false;
        }

        R01SecondaryCreatureEncounterData.ActionId action =
                decision.action().orElseThrow();
        R01SecondaryCreatureEncounterData.ActionRule rule =
                data.rulesById().get(action);
        if (rule == null) {
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
        current = new CommittedAction(
                action,
                decision.actionCounter(),
                decision.forcedAfterShell(),
                nowTick,
                impactTick,
                recoveryEndTick,
                rule.committedMovementBlocks(),
                rule.areaRadius(),
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
                        current.forcedAfterShell(),
                        phase(current, nowTick),
                        current.startedAtTick(),
                        current.impactTick(),
                        current.recoveryEndTick(),
                        current.committedMovementBlocks(),
                        current.areaRadius()
                )
        );
    }

    /**
     * Consumes one presentation-confirmed target contact at the exact authored impact frame.
     *
     * <p>Each target can be consumed once per committed action. Bloom Quake may therefore confirm
     * multiple different players on the same impact tick while duplicate callbacks remain harmless.</p>
     */
    public Optional<R01SecondaryCreatureEncounterData.ActionId> confirmContact(
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
                    "Nature Spirit execution time must be monotonic."
            );
        }

        boolean recoveryComplete =
                current.recoveryEndTick() > current.impactTick()
                        ? nowTick >= current.recoveryEndTick()
                        : nowTick > current.impactTick();
        if (recoveryComplete) {
            current = null;
        }
    }

    private static Phase phase(
            CommittedAction action,
            long nowTick
    ) {
        if (nowTick < action.impactTick()) {
            return Phase.WIND_UP;
        }
        if (nowTick == action.impactTick()) {
            return Phase.IMPACT_FRAME;
        }
        return Phase.RECOVERY;
    }

    private static void requireTick(long gameTick) {
        if (gameTick < 0L) {
            throw new IllegalArgumentException(
                    "Nature Spirit execution tick must be non-negative."
            );
        }
    }

    public enum Phase {
        WIND_UP,
        IMPACT_FRAME,
        RECOVERY
    }

    public record Snapshot(
            R01SecondaryCreatureEncounterData.ActionId action,
            long actionCounter,
            boolean forcedAfterShell,
            Phase phase,
            long startedAtTick,
            long impactTick,
            long recoveryEndTick,
            double committedMovementBlocks,
            double areaRadius
    ) {
        public Snapshot {
            Objects.requireNonNull(action, "action");
        }
    }

    private record CommittedAction(
            R01SecondaryCreatureEncounterData.ActionId action,
            long actionCounter,
            boolean forcedAfterShell,
            long startedAtTick,
            long impactTick,
            long recoveryEndTick,
            double committedMovementBlocks,
            double areaRadius,
            Set<UUID> hitTargets
    ) {
        private CommittedAction {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(hitTargets, "hitTargets");
        }
    }
}
