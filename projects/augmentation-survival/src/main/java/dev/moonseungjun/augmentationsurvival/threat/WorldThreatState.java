package dev.moonseungjun.augmentationsurvival.threat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Save-wide threat high-water mark. Stored on the server Overworld. */
public record WorldThreatState(
        double highWaterPowerScore,
        int stage
) {
    public static final Codec<WorldThreatState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("high_water_power_score", 0.0)
                    .forGetter(WorldThreatState::highWaterPowerScore),
            Codec.intRange(0, ThreatCurve.maxStage())
                    .optionalFieldOf("stage", 0)
                    .forGetter(WorldThreatState::stage)
    ).apply(instance, WorldThreatState::new));

    public WorldThreatState {
        if (!Double.isFinite(highWaterPowerScore) || highWaterPowerScore < 0.0) {
            throw new IllegalArgumentException("highWaterPowerScore must be finite and non-negative");
        }
        int expected = ThreatCurve.stageForScore(highWaterPowerScore);
        if (stage != expected) {
            stage = expected;
        }
    }

    public static WorldThreatState initial() {
        return new WorldThreatState(0.0, 0);
    }

    public WorldThreatState observe(double playerPowerScore) {
        if (playerPowerScore <= highWaterPowerScore) {
            return this;
        }
        return new WorldThreatState(playerPowerScore, ThreatCurve.stageForScore(playerPowerScore));
    }
}
