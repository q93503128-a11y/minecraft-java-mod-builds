package io.github.q93503128.turnbound.world;

/** Pure repath throttling and blocked-route recovery rules for shared free-roam encounter actors. */
final class FieldNavigationRules {
    static final long NEVER = Long.MIN_VALUE;
    static final double ARRIVAL_DISTANCE_SQ = 0.64D;
    static final int ALERT_REPATH_INTERVAL_TICKS = 4;
    static final int STATIC_RETRY_INTERVAL_TICKS = 20;
    static final int BLOCKED_PATROL_RETARGET_TICKS = 60;

    private FieldNavigationRules() {}

    static boolean shouldIssue(FieldEncounterRules.Phase phase, long now, long lastIssued,
                               double targetShiftSq, boolean navigationDone) {
        if (lastIssued == NEVER) return true;

        double shiftThresholdSq = phase == FieldEncounterRules.Phase.ALERT ? 1.0D : 0.25D;
        if (targetShiftSq > shiftThresholdSq) return true;

        long elapsed = now - lastIssued;
        if (phase == FieldEncounterRules.Phase.ALERT) {
            return elapsed >= ALERT_REPATH_INTERVAL_TICKS;
        }
        return navigationDone && elapsed >= STATIC_RETRY_INTERVAL_TICKS;
    }

    static boolean shouldSkipBlockedPatrolTarget(
            FieldEncounterRules.Phase phase,
            long now,
            long stalledSince,
            boolean navigationDone,
            double targetDistanceSq
    ) {
        return phase == FieldEncounterRules.Phase.PATROL
                && navigationDone
                && targetDistanceSq > ARRIVAL_DISTANCE_SQ
                && stalledSince != NEVER
                && now - stalledSince >= BLOCKED_PATROL_RETARGET_TICKS;
    }
}
