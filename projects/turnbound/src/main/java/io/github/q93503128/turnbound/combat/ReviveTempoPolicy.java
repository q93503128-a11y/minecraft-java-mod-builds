package io.github.q93503128.turnbound.combat;

/**
 * Server-authoritative post-revive Turn Gauge policy.
 *
 * Character data may author a normal reviveStartGauge and a selfReviveGauge.
 * A one-time passive that must act immediately uses REVIVE_IMMEDIATE_TURN and starts at the ready threshold.
 */
final class ReviveTempoPolicy {
    private ReviveTempoPolicy() {}

    static int manualStartGauge(CombatantState target) {
        return clamp(target.definition().intParam("reviveStartGauge", 0));
    }

    static int selfReviveStartGauge(CombatantState target) {
        if (target.definition().hasRule("REVIVE_IMMEDIATE_TURN")) return (int)TurnScheduler.TURN_THRESHOLD;
        int authored = target.definition().intParam("selfReviveGauge", -1);
        return authored >= 0 ? clamp(authored) : manualStartGauge(target);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min((int)TurnScheduler.TURN_THRESHOLD, value));
    }
}
