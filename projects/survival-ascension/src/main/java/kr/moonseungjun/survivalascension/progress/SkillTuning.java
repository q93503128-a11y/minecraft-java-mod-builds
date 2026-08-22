package kr.moonseungjun.survivalascension.progress;

public final class SkillTuning {
    public static final int MAX_LEVEL = 100;

    private SkillTuning() {}

    public static long xpForNextLevel(int currentLevel) {
        if (currentLevel >= MAX_LEVEL) return 0L;
        int level = Math.max(0, currentLevel);
        return 40L + 8L * level + Math.round(1.5D * level * level);
    }

    public static long xpAtLevel(int targetLevel) {
        int clamped = Math.max(0, Math.min(MAX_LEVEL, targetLevel));
        long total = 0L;
        for (int level = 0; level < clamped; level++) total += xpForNextLevel(level);
        return total;
    }

    public static int levelFromXp(long xp) {
        long safeXp = Math.max(0L, xp);
        int level = 0;
        long threshold = 0L;
        while (level < MAX_LEVEL) {
            long next = xpForNextLevel(level);
            if (safeXp < threshold + next) break;
            threshold += next;
            level++;
        }
        return level;
    }

    public static long xpIntoLevel(long totalXp) {
        int level = levelFromXp(totalXp);
        return Math.max(0L, totalXp - xpAtLevel(level));
    }

    public static double miningSpeedMultiplier(int level) {
        int clamped = clamp(level);
        return 1.0D + 0.03D * clamped + 0.0004D * clamped * clamped;
    }

    public static int miningAreaSize(int level) {
        if (level >= 60) return 7;
        if (level >= 30) return 5;
        if (level >= 10) return 3;
        return 1;
    }

    public static double woodcuttingSpeedMultiplier(int level) {
        int clamped = clamp(level);
        return 1.0D + 0.025D * clamped + 0.00025D * clamped * clamped;
    }

    public static int woodcuttingLogLimit(int level) {
        if (level >= 90) return 256;
        if (level >= 60) return 128;
        if (level >= 30) return 48;
        if (level >= 10) return 16;
        return 1;
    }

    public static double harvestingSpeedMultiplier(int level) {
        int clamped = clamp(level);
        return 1.0D + 0.02D * clamped + 0.0002D * clamped * clamped;
    }

    public static int harvestingAreaSize(int level) {
        if (level >= 90) return 9;
        if (level >= 60) return 7;
        if (level >= 30) return 5;
        if (level >= 10) return 3;
        return 1;
    }

    public static int masteryTier(int level) {
        if (level >= 90) return 5;
        if (level >= 60) return 4;
        if (level >= 30) return 3;
        if (level >= 10) return 2;
        return 1;
    }

    private static int clamp(int level) {
        return Math.max(0, Math.min(MAX_LEVEL, level));
    }
}
