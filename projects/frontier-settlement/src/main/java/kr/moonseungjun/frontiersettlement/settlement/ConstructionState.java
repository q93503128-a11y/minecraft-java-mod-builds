package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record ConstructionState(String type, int originX, int originY, int originZ, int rotation, int step) {
    public static final ConstructionState EMPTY = new ConstructionState("", 0, 0, 0, 0, 0);

    public static final Codec<ConstructionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("type", "").forGetter(ConstructionState::type),
            Codec.INT.optionalFieldOf("origin_x", 0).forGetter(ConstructionState::originX),
            Codec.INT.optionalFieldOf("origin_y", 0).forGetter(ConstructionState::originY),
            Codec.INT.optionalFieldOf("origin_z", 0).forGetter(ConstructionState::originZ),
            Codec.INT.optionalFieldOf("rotation", 0).forGetter(ConstructionState::rotation),
            Codec.INT.optionalFieldOf("step", 0).forGetter(ConstructionState::step)
    ).apply(instance, ConstructionState::new));

    public ConstructionState(String type, int originX, int originY, int originZ, int step) {
        this(type, originX, originY, originZ, 0, step);
    }

    public boolean active() {
        return !type.isBlank();
    }

    public BlockPos origin() {
        return new BlockPos(originX, originY, originZ);
    }

    public BuildingRotation buildingRotation() {
        return BuildingRotation.fromId(rotation);
    }

    public ConstructionState advance() {
        return new ConstructionState(type, originX, originY, originZ, rotation, step + 1);
    }
}
