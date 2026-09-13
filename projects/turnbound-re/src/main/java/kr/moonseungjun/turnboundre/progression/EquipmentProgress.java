package kr.moonseungjun.turnboundre.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Persisted ownership/upgrade state for one unique equipment piece. */
public record EquipmentProgress(String equipmentId, int level) {
    public static final Codec<EquipmentProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("equipmentId").forGetter(EquipmentProgress::equipmentId),
            Codec.INT.fieldOf("level").forGetter(EquipmentProgress::level)
    ).apply(instance, EquipmentProgress::new));

    public EquipmentProgress {
        if (equipmentId == null || equipmentId.isBlank()) throw new IllegalArgumentException("equipmentId must not be blank");
        if (level < 1) throw new IllegalArgumentException("equipment level must be >= 1");
    }
}
