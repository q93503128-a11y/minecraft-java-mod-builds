package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record OutpostConstructionState(int roadIndex,
                                       int gateX, int gateY, int gateZ,
                                       int directionX, int directionZ,
                                       int step) {
    /**
     * Phase markers stay inside the existing persisted step field so old saves decode unchanged.
     * Small values are Alpha.25-or-earlier prepaid building, 1M+ is grading, and 2M+ is Alpha.26 physical building.
     */
    public static final int GRADE_STEP_OFFSET = 1_000_000;
    public static final int BUILD_STEP_OFFSET = 2_000_000;
    public static final OutpostConstructionState EMPTY =
            new OutpostConstructionState(-1, 0, 0, 0, 0, 0, 0);

    public static final Codec<OutpostConstructionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("road_index", -1).forGetter(OutpostConstructionState::roadIndex),
            Codec.INT.optionalFieldOf("gate_x", 0).forGetter(OutpostConstructionState::gateX),
            Codec.INT.optionalFieldOf("gate_y", 0).forGetter(OutpostConstructionState::gateY),
            Codec.INT.optionalFieldOf("gate_z", 0).forGetter(OutpostConstructionState::gateZ),
            Codec.INT.optionalFieldOf("direction_x", 0).forGetter(OutpostConstructionState::directionX),
            Codec.INT.optionalFieldOf("direction_z", 0).forGetter(OutpostConstructionState::directionZ),
            Codec.INT.optionalFieldOf("step", 0).forGetter(OutpostConstructionState::step)
    ).apply(instance, OutpostConstructionState::new));

    public boolean active() {
        return roadIndex >= 0 && (directionX != 0 || directionZ != 0);
    }

    public boolean grading() {
        return active() && step >= GRADE_STEP_OFFSET && step < BUILD_STEP_OFFSET;
    }

    public boolean physicalBuilding() {
        return active() && step >= BUILD_STEP_OFFSET;
    }

    public boolean legacyPrepaidBuilding() {
        return active() && step >= 0 && step < GRADE_STEP_OFFSET;
    }

    public int gradeStep() {
        return grading() ? step - GRADE_STEP_OFFSET : -1;
    }

    public int buildStep() {
        return physicalBuilding() ? step - BUILD_STEP_OFFSET : -1;
    }

    public int legacyStep() {
        return legacyPrepaidBuilding() ? step : -1;
    }

    public BlockPos gate() {
        return new BlockPos(gateX, gateY, gateZ);
    }

    public OutpostConstructionState advance() {
        return new OutpostConstructionState(
                roadIndex, gateX, gateY, gateZ, directionX, directionZ, step + 1);
    }

    public OutpostConstructionState withStep(int nextStep) {
        return new OutpostConstructionState(
                roadIndex, gateX, gateY, gateZ, directionX, directionZ, Math.max(0, nextStep));
    }
}
