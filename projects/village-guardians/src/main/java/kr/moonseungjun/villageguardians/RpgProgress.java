package kr.moonseungjun.villageguardians;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RpgProgress(int level, int experience) {
    public static final int MAX_LEVEL = 30;

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
        return 120 + level * 72 + level * level * 7;
    }
}
