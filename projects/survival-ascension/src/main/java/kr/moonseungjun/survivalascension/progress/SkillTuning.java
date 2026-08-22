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
        if (level >= 90) return 9;
        if (level >= 60) return 7;
        if (level >= 30) return 5;
        if (level >= 10) return 3;
        return 1;
    }
    public static int miningVeinLimit(int level) {
        if (level >= 90) return 128;
        if (level >= 60) return 64;
        if (level >= 30) return 24;
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

    public static double combatDamageMultiplier(int level) {
        int clamped = clamp(level);
        return 1.0D + 0.006D * clamped + 0.00002D * clamped * clamped;
    }
    public static double combatCleaveRadius(int level) {
        if (level >= 90) return 4.0D;
        if (level >= 60) return 2.75D;
        if (level >= 30) return 1.75D;
        return 0.0D;
    }
    public static int combatCleaveTargetLimit(int level) {
        if (level >= 90) return 8;
        if (level >= 60) return 4;
        if (level >= 30) return 2;
        return 0;
    }
    public static double combatCleaveFraction(int level) {
        if (level >= 90) return 0.60D;
        if (level >= 60) return 0.42D;
        if (level >= 30) return 0.25D;
        return 0.0D;
    }

    public static int constructionLineLength(int level) {
        if (level >= 90) return 33;
        if (level >= 60) return 17;
        if (level >= 30) return 9;
        if (level >= 10) return 5;
        return 1;
    }
    public static int constructionPlaneSize(int level) {
        if (level >= 90) return 9;
        if (level >= 60) return 5;
        if (level >= 30) return 3;
        return 1;
    }

    public static double mobilitySpeedMultiplier(int level) {
        int clamped = clamp(level);
        return 1.0D + 0.0015D * clamped + 0.000005D * clamped * clamped;
    }
    public static double mobilityStepHeight(int level) {
        if (level >= 90) return 1.5D;
        if (level >= 60) return 1.25D;
        if (level >= 10) return 1.0D;
        return 0.6D;
    }
    public static double mobilitySafeFallDistance(int level) {
        if (level >= 90) return 12.0D;
        if (level >= 60) return 8.0D;
        if (level >= 30) return 6.0D;
        if (level >= 10) return 4.0D;
        return 3.0D;
    }
    public static double mobilityDashPower(int level) {
        if (level >= 90) return 1.55D;
        if (level >= 60) return 1.25D;
        if (level >= 30) return 0.95D;
        return 0.0D;
    }
    public static int mobilityDashCooldownTicks(int level) {
        if (level >= 90) return 24;
        if (level >= 60) return 40;
        if (level >= 30) return 60;
        return Integer.MAX_VALUE;
    }

    public static int masteryTier(int level) {
        if (level >= 90) return 5;
        if (level >= 60) return 4;
        if (level >= 30) return 3;
        if (level >= 10) return 2;
        return 1;
    }

    private static int clamp(int level) { return Math.max(0, Math.min(MAX_LEVEL, level)); }
}
