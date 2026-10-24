package dev.moonseungjun.augmentationsurvival.threat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Persistent per-entity threat application state, mainly for boss encounter stage snapshots. */
public record ThreatEntityState(
        int appliedStage,
        int bossSnapshotStage
) {
    public static final Codec<ThreatEntityState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("applied_stage", -1).forGetter(ThreatEntityState::appliedStage),
            Codec.INT.optionalFieldOf("boss_snapshot_stage", -1).forGetter(ThreatEntityState::bossSnapshotStage)
    ).apply(instance, ThreatEntityState::new));

    public ThreatEntityState {
        validateStage(appliedStage, "appliedStage");
        validateStage(bossSnapshotStage, "bossSnapshotStage");
    }

    public static ThreatEntityState initial() {
        return new ThreatEntityState(-1, -1);
    }

    public ThreatEntityState withAppliedStage(int stage) {
        return new ThreatEntityState(stage, bossSnapshotStage);
    }

    public ThreatEntityState withBossSnapshotStage(int stage) {
        return new ThreatEntityState(appliedStage, stage);
    }

    private static void validateStage(int stage, String name) {
        if (stage < -1 || stage > ThreatCurve.maxStage()) {
            throw new IllegalArgumentException(name + " out of range: " + stage);
        }
    }
}
