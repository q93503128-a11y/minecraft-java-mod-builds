package kr.moonseungjun.turnboundre.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Persisted per-character progression. originStar is copied from CharacterDefinition and never mutated. */
public record CharacterProgress(String characterId, int originStar, int currentStar, int level) {
    public static final Codec<CharacterProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("characterId").forGetter(CharacterProgress::characterId),
            Codec.INT.fieldOf("originStar").forGetter(CharacterProgress::originStar),
            Codec.INT.fieldOf("currentStar").forGetter(CharacterProgress::currentStar),
            Codec.INT.fieldOf("level").forGetter(CharacterProgress::level)
    ).apply(instance, CharacterProgress::new));

    public CharacterProgress {
        if (characterId == null || characterId.isBlank()) throw new IllegalArgumentException("characterId must not be blank");
        if (originStar < 1 || originStar > 5) throw new IllegalArgumentException("originStar must be 1..5");
        if (currentStar < originStar || currentStar > 6) throw new IllegalArgumentException("currentStar must be originStar..6");
        if (level < 1 || level > ProgressionRules.levelCap(currentStar)) {
            throw new IllegalArgumentException("level outside currentStar cap");
        }
    }
}
