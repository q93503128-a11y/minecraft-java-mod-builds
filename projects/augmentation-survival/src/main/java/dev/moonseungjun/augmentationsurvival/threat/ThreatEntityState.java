package dev.moonseungjun.augmentationsurvival.threat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Persistent per-boss threat metadata used to freeze the encounter's threat stage. */
public record ThreatEntityState(int bossSnapshotStage) {
    public static final Codec<ThreatEntityState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("boss_snapshot_stage", -1).forGetter(ThreatEntityState::bossSnapshotStage)
    ).apply(instance, ThreatEntityState::new));

    public ThreatEntityState {
        if (bossSnapshotStage < -1 || bossSnapshotStage > ThreatCurve.maxStage()) {
            throw new IllegalArgumentException("bossSnapshotStage out of range: " + bossSnapshotStage);
        }
    }

    public static ThreatEntityState initial() {
        return new ThreatEntityState(-1);
    }
}
