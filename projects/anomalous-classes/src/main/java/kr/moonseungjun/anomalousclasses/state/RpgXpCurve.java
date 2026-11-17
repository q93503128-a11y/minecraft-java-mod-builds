package kr.moonseungjun.anomalousclasses.state;

/** The provisional P30 curve, independent of vanilla enchanting experience. */
public final class RpgXpCurve {
    public static final int SKILL_POINTS_PER_LEVEL = 3;
    private RpgXpCurve() {}

    public static long required(int level) {
        if (level < 1 || level == Integer.MAX_VALUE) return Long.MAX_VALUE;
        return (long) Math.ceil(100.0 + 9.0 * level + 0.45 * level * Math.sqrt(level));
    }

    public static long cumulativeTo(int level) {
        long total = 0L;
        for (int current = 1; current < level; current++) {
            long next = required(current);
            if (Long.MAX_VALUE - total < next) return Long.MAX_VALUE;
            total += next;
        }
        return total;
    }

    public static Progress advance(int level, long progressXp, int skillPoints, long awarded) {
        int nextLevel = Math.max(1, level);
        long remaining = Math.max(0L, progressXp);
        int points = Math.max(0, skillPoints);
        if (awarded > 0L) remaining = Long.MAX_VALUE - remaining < awarded
                ? Long.MAX_VALUE : remaining + awarded;
        int levels = 0;
        while (nextLevel < Integer.MAX_VALUE
                && points <= Integer.MAX_VALUE - SKILL_POINTS_PER_LEVEL
                && remaining >= required(nextLevel) && levels < 20_000) {
            remaining -= required(nextLevel);
            nextLevel++;
            points += SKILL_POINTS_PER_LEVEL;
            levels++;
        }
        return new Progress(nextLevel, remaining, points, levels);
    }

    public record Progress(int level, long experience, int skillPoints, int levelsGained) {}
}
