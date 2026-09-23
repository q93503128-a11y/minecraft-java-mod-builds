package dev.moonseungjun.openworldrpg.progression;

/** Canonical global Lv and root-class Rank XP curves. */
public final class ProjectProgressionRules {
    public static final int MAX_COMBAT_LEVEL = 80;
    public static final int MAX_CLASS_RANK = 50;

    private ProjectProgressionRules() {
    }

    public static long combatXpToNext(int level) {
        if (level < 1 || level >= MAX_COMBAT_LEVEL) {
            throw new IllegalArgumentException(
                    "combat XP requirement exists only for current Lv 1..79."
            );
        }
        long raw = 100L + 50L * level + 4L * level * level;
        return roundToTen(raw);
    }

    public static long classXpToNext(int rank) {
        if (rank < 1 || rank >= MAX_CLASS_RANK) {
            throw new IllegalArgumentException(
                    "class XP requirement exists only for current Rank 1..49."
            );
        }
        long raw = 100L + 20L * rank + 2L * rank * rank;
        return roundToTen(raw);
    }

    private static long roundToTen(long value) {
        return Math.round(value / 10.0) * 10L;
    }
}
