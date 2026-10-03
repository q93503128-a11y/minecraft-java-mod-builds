package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import java.util.Objects;

/**
 * Pure server-owned timing state for Regalhart's one-time Sovereign transition.
 *
 * <p>The transition itself reduces ordinary incoming damage only for the exact authored 30-tick
 * window. The +10% movement multiplier becomes eligible only after that window ends. This class
 * deliberately owns no entity movement or presentation.</p>
 */
public final class R01RegalhartSovereignExecutionState {
    private final R01RegalhartEncounterData.SovereignRule rule;
    private boolean begun;
    private long transitionStartTick = Long.MIN_VALUE;
    private long transitionEndTick = Long.MIN_VALUE;

    public R01RegalhartSovereignExecutionState(
            R01RegalhartEncounterData.SovereignRule rule
    ) {
        this.rule = Objects.requireNonNull(rule, "rule");
    }

    public boolean begin(
            R01RegalhartActionController.Decision decision,
            long gameTick
    ) {
        Objects.requireNonNull(decision, "decision");
        requireTick(gameTick);

        if (begun
                || decision.mode()
                        != R01RegalhartActionController.Mode.SOVEREIGN_TRANSITION_START) {
            return false;
        }

        long expectedEndTick = Math.addExact(
                gameTick,
                rule.transitionTicks()
        );
        if (decision.sovereignTransitionEndTick() != expectedEndTick) {
            return false;
        }

        begun = true;
        transitionStartTick = gameTick;
        transitionEndTick = expectedEndTick;
        return true;
    }

    public double incomingDamageMultiplier(long gameTick) {
        requireTick(gameTick);
        return begun
                        && gameTick >= transitionStartTick
                        && gameTick < transitionEndTick
                ? rule.transitionDamageTakenMultiplier()
                : 1.0;
    }

    public double movementSpeedMultiplier(long gameTick) {
        requireTick(gameTick);
        return begun && gameTick >= transitionEndTick
                ? rule.movementSpeedMultiplier()
                : 1.0;
    }

    public boolean begun() {
        return begun;
    }

    public long transitionEndTick() {
        return begun ? transitionEndTick : 0L;
    }

    private static void requireTick(long gameTick) {
        if (gameTick < 0L) {
            throw new IllegalArgumentException(
                    "Regalhart Sovereign gameTick must be non-negative."
            );
        }
    }
}
