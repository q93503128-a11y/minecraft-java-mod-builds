package io.github.q93503128.turnbound.world;

/** Pure repath throttling and blocked-route recovery rules for shared free-roam encounter actors. */
final class FieldNavigationRules {
    static final long NEVER = Long.MIN_VALUE;
    static final double ARRIVAL_DISTANCE_SQ = 0.64D;
    static final double PHYSICAL_PROGRESS_DISTANCE_SQ = 0.000025D;
    static final int WALKING_GRACE_TICKS = 2;
    static final int ALERT_REPATH_INTERVAL_TICKS = 4;
    static final int STATIC_RETRY_INTERVAL_TICKS = 20;
    static final int STALLED_ALERT_RECOVERY_TICKS = 30;
    static final int STALLED_RETURN_RECOVERY_TICKS = 40;
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

    static boolean madePhysicalProgress(double horizontalDistanceSq) {
        return horizontalDistanceSq > PHYSICAL_PROGRESS_DISTANCE_SQ;
    }

    static boolean walkingFromRecentProgress(long now, long lastProgressTick, boolean navigationDone) {
        return !navigationDone
                && lastProgressTick != NEVER
                && now - lastProgressTick <= WALKING_GRACE_TICKS;
    }

    static boolean shouldRecoverStalledNavigation(
            FieldEncounterRules.Phase phase,
            long now,
            long stalledSince,
            double targetDistanceSq
    ) {
        if (stalledSince == NEVER || targetDistanceSq <= ARRIVAL_DISTANCE_SQ) return false;
        int limit = switch (phase) {
            case ALERT -> STALLED_ALERT_RECOVERY_TICKS;
            case RETURN -> STALLED_RETURN_RECOVERY_TICKS;
            case PATROL -> BLOCKED_PATROL_RETARGET_TICKS;
        };
        return now - stalledSince >= limit;
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
                && shouldRecoverStalledNavigation(phase, now, stalledSince, targetDistanceSq);
    }
}
