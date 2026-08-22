package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record RoadConstructionState(int startX, int startY, int startZ,
                                    int directionX, int directionZ,
                                    int length, int step) {
    public static final RoadConstructionState EMPTY = new RoadConstructionState(0, 0, 0, 0, 0, 0, 0);

    public static final Codec<RoadConstructionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("start_x", 0).forGetter(RoadConstructionState::startX),
            Codec.INT.optionalFieldOf("start_y", 0).forGetter(RoadConstructionState::startY),
            Codec.INT.optionalFieldOf("start_z", 0).forGetter(RoadConstructionState::startZ),
            Codec.INT.optionalFieldOf("direction_x", 0).forGetter(RoadConstructionState::directionX),
            Codec.INT.optionalFieldOf("direction_z", 0).forGetter(RoadConstructionState::directionZ),
            Codec.INT.optionalFieldOf("length", 0).forGetter(RoadConstructionState::length),
            Codec.INT.optionalFieldOf("step", 0).forGetter(RoadConstructionState::step)
    ).apply(instance, RoadConstructionState::new));

    public boolean active() {
        return length > 0 && Math.abs(directionX) + Math.abs(directionZ) == 1;
    }

    public BlockPos start() {
        return new BlockPos(startX, startY, startZ);
    }

    public RoadConstructionState advance() {
        return new RoadConstructionState(startX, startY, startZ, directionX, directionZ, length, step + 1);
    }

    public RoadConstructionState withStep(int nextStep) {
        return new RoadConstructionState(startX, startY, startZ, directionX, directionZ, length, Math.max(0, nextStep));
    }
}
