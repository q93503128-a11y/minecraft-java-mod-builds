package dev.moonseungjun.fishinggame.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record FishRecord(String speciesId, int caughtCount, int bestWeightGrams, int bestLengthMm) {
    public static final Codec<FishRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("species").forGetter(FishRecord::speciesId),
            Codec.INT.optionalFieldOf("caught_count", 1).forGetter(FishRecord::caughtCount),
            Codec.INT.fieldOf("best_weight_g").forGetter(FishRecord::bestWeightGrams),
            Codec.INT.fieldOf("best_length_mm").forGetter(FishRecord::bestLengthMm)
    ).apply(instance, FishRecord::new));

    public FishRecord {
        if (speciesId == null || speciesId.isBlank()) {
            throw new IllegalArgumentException("speciesId must not be blank");
        }
        caughtCount = Math.max(1, caughtCount);
        bestWeightGrams = Math.max(1, bestWeightGrams);
        bestLengthMm = Math.max(1, bestLengthMm);
    }

    public static FishRecord fromCatch(CatchEntry entry) {
        return new FishRecord(entry.speciesId(), 1, entry.weightGrams(), entry.lengthMm());
    }

    public FishRecord add(CatchEntry entry) {
        if (!speciesId.equals(entry.speciesId())) {
            throw new IllegalArgumentException("catch species does not match record");
        }
        return new FishRecord(
                speciesId,
                caughtCount + 1,
                Math.max(bestWeightGrams, entry.weightGrams()),
                Math.max(bestLengthMm, entry.lengthMm())
        );
    }

    public double bestWeightKg() {
        return bestWeightGrams / 1000.0;
    }

    public double bestLengthCm() {
        return bestLengthMm / 10.0;
    }
}
