package io.github.q93503128.turnbound.world;

/** Pure encounter state rules kept separate from entity presentation for deterministic testing. */
final class FieldEncounterRules {
    enum Phase { PATROL, ALERT, RETURN }

    static final double ALERT_RADIUS = 10.0;
    static final double DISENGAGE_RADIUS = 15.0;
    static final double HOME_LEASH_RADIUS = 18.0;
    static final double ENGAGE_RADIUS = 2.6;
    static final double RETURN_HOME_EPSILON = 0.65;
    static final int RETURN_REAGGRO_GRACE_TICKS = 60;

    private FieldEncounterRules() {}

    static boolean shouldEngage(Phase phase, double distance, int graceTicks) {
        return phase == Phase.ALERT && graceTicks <= 0 && distance <= ENGAGE_RADIUS;
    }

    static Phase nextPhase(Phase phase, double playerDistance, double homeDistance, int graceTicks) {
        if (phase == Phase.RETURN) {
            return homeDistance <= RETURN_HOME_EPSILON ? Phase.PATROL : Phase.RETURN;
        }
        if (graceTicks > 0) return Phase.PATROL;
        if (phase == Phase.ALERT && (playerDistance >= DISENGAGE_RADIUS || homeDistance >= HOME_LEASH_RADIUS)) {
            return Phase.RETURN;
        }
        if (phase == Phase.PATROL && playerDistance <= ALERT_RADIUS && homeDistance < HOME_LEASH_RADIUS) {
            return Phase.ALERT;
        }
        return phase;
    }
}
