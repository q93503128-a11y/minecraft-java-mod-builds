package io.github.q93503128.turnbound.world;

/** Pure repath throttling rules for shared free-roam encounter actors. */
final class FieldNavigationRules {
    static final long NEVER = Long.MIN_VALUE;

    private FieldNavigationRules() {}

    static boolean shouldIssue(FieldEncounterRules.Phase phase, long now, long lastIssued,
                               double targetShiftSq, boolean navigationDone) {
        if (navigationDone || lastIssued == NEVER) return true;
        double shiftThresholdSq = phase == FieldEncounterRules.Phase.ALERT ? 1.0D : 0.25D;
        if (targetShiftSq > shiftThresholdSq) return true;
        int interval = phase == FieldEncounterRules.Phase.ALERT ? 4 : 12;
        return now - lastIssued >= interval;
    }
}
