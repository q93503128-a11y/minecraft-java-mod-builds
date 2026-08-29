package io.github.q93503128.turnbound.world;

/** Pure encounter state rules kept separate from entity presentation for deterministic testing. */
final class FieldEncounterRules {
    enum Phase { PATROL, ALERT }

    static final double ALERT_RADIUS = 10.0;
    static final double DISENGAGE_RADIUS = 15.0;
    static final double ENGAGE_RADIUS = 2.6;

    private FieldEncounterRules() {}

    static boolean shouldEngage(double distance, int graceTicks) {
        return graceTicks <= 0 && distance <= ENGAGE_RADIUS;
    }

    static Phase nextPhase(Phase phase, double distance, int graceTicks) {
        if (graceTicks > 0) return Phase.PATROL;
        if (phase == Phase.PATROL && distance <= ALERT_RADIUS) return Phase.ALERT;
        if (phase == Phase.ALERT && distance >= DISENGAGE_RADIUS) return Phase.PATROL;
        return phase;
    }
}
