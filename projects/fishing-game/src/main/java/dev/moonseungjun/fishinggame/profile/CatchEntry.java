package dev.moonseungjun.fishinggame.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CatchEntry(String speciesId, int weightGrams, int lengthMm, int value) {
    public static final Codec<CatchEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("species").forGetter(CatchEntry::speciesId),
            Codec.INT.fieldOf("weight_g").forGetter(CatchEntry::weightGrams),
            Codec.INT.fieldOf("length_mm").forGetter(CatchEntry::lengthMm),
            Codec.INT.fieldOf("value").forGetter(CatchEntry::value)
    ).apply(instance, CatchEntry::new));

    public CatchEntry {
        if (speciesId == null || speciesId.isBlank()) throw new IllegalArgumentException("speciesId must not be blank");
        if (weightGrams <= 0) throw new IllegalArgumentException("weightGrams must be positive");
        if (lengthMm <= 0) throw new IllegalArgumentException("lengthMm must be positive");
        if (value < 0) throw new IllegalArgumentException("value must not be negative");
    }

    public double weightKg() {
        return weightGrams / 1000.0;
    }

    public double lengthCm() {
        return lengthMm / 10.0;
    }
}
