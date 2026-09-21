package kr.moonseungjun.villageguardians;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RpgProgress(int level, int experience) {
    public static final int MAX_LEVEL = 300;

    public static final Codec<RpgProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("level", 1).forGetter(RpgProgress::level),
            Codec.INT.optionalFieldOf("experience", 0).forGetter(RpgProgress::experience)
    ).apply(instance, RpgProgress::new));

    public RpgProgress {
        level = Math.max(1, Math.min(MAX_LEVEL, level));
        experience = Math.max(0, experience);
        if (level >= MAX_LEVEL) {
            experience = 0;
        }
    }

    public static RpgProgress initial() {
        return new RpgProgress(1, 0);
    }

    public int experienceToNextLevel() {
        if (level >= MAX_LEVEL) {
            return 0;
        }
        return experienceRequiredAtLevel(level);
    }

    public static int experienceRequiredAtLevel(int level) {
        int safe = Math.max(1, Math.min(MAX_LEVEL - 1, level));
        return 120 + safe * 72 + safe * safe * 7;
    }

    /**
     * Visible progression continues to Lv.300, while raw combat formulas preserve the old
     * Lv.1-100 curve and only gain one legacy-equivalent scaling level per four mastery levels.
     */
    public static int combatScalingLevel(int level) {
        int safe = Math.max(1, Math.min(MAX_LEVEL, level));
        if (safe <= 100) return safe;
        return 100 + (safe - 100) / 4;
    }
}
