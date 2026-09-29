package dev.moonseungjun.openworldrpg.death;

import dev.moonseungjun.openworldrpg.progression.ProjectProgressionRules;

/** Canonical automatic death-penalty math. */
public final class ProjectDeathPenaltyRules {
    private ProjectDeathPenaltyRules() {
    }

    public static long currentLevelXpLoss(
            int combatLevel,
            long currentLevelXp
    ) {
        if (combatLevel < 1
                || combatLevel > ProjectProgressionRules.MAX_COMBAT_LEVEL) {
            throw new IllegalArgumentException(
                    "Death penalty requires combat Lv 1..80."
            );
        }
        if (currentLevelXp < 0L) {
            throw new IllegalArgumentException(
                    "Current-Lv EXP cannot be negative."
            );
        }
        if (currentLevelXp == 0L) {
            return 0L;
        }
        if (combatLevel == ProjectProgressionRules.MAX_COMBAT_LEVEL) {
            throw new IllegalArgumentException(
                    "Lv 80 cannot carry current-Lv EXP."
            );
        }

        long requirement = ProjectProgressionRules.combatXpToNext(
                combatLevel
        );
        long targetLoss = Math.max(
                1L,
                Math.round(requirement * 0.04)
        );
        return Math.min(currentLevelXp, targetLoss);
    }

    public static long goldFallbackCost(int combatLevel) {
        if (combatLevel < 1
                || combatLevel > ProjectProgressionRules.MAX_COMBAT_LEVEL) {
            throw new IllegalArgumentException(
                    "Death penalty requires combat Lv 1..80."
            );
        }
        double raw = 30.0
                + 6.0 * combatLevel
                + 0.12 * combatLevel * combatLevel;
        return Math.round(Math.min(1200.0, raw) / 10.0) * 10L;
    }
}
