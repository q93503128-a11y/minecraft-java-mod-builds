package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record ConstructionState(String type, int originX, int originY, int originZ, int rotation, int step, int scaffoldMask) {
    /**
     * Alpha.30 phase encoding keeps the persisted schema stable:
     * - small non-negative steps are pre-Alpha.30 already-prepared active builds;
     * - 1,000,000+ is physical site grading;
     * - 2,000,000+ is the normal material-hauling/blueprint phase.
     */
    public static final int GRADE_STEP_OFFSET = 1_000_000;
    public static final int BUILD_STEP_OFFSET = 2_000_000;

    public static final ConstructionState EMPTY = new ConstructionState("", 0, 0, 0, 0, 0, 0);

    public static final Codec<ConstructionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("type", "").forGetter(ConstructionState::type),
            Codec.INT.optionalFieldOf("origin_x", 0).forGetter(ConstructionState::originX),
            Codec.INT.optionalFieldOf("origin_y", 0).forGetter(ConstructionState::originY),
            Codec.INT.optionalFieldOf("origin_z", 0).forGetter(ConstructionState::originZ),
            Codec.INT.optionalFieldOf("rotation", 0).forGetter(ConstructionState::rotation),
            Codec.INT.optionalFieldOf("step", 0).forGetter(ConstructionState::step),
            Codec.INT.optionalFieldOf("scaffold_mask", 0).forGetter(ConstructionState::scaffoldMask)
    ).apply(instance, ConstructionState::new));

    public ConstructionState(String type, int originX, int originY, int originZ, int rotation, int step) {
        this(type, originX, originY, originZ, rotation, step, 0);
    }

    public ConstructionState(String type, int originX, int originY, int originZ, int step) {
        this(type, originX, originY, originZ, 0, step, 0);
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

    public boolean grading() {
        return active() && step >= GRADE_STEP_OFFSET && step < BUILD_STEP_OFFSET;
    }

    public boolean physicalBuilding() {
        return active() && step >= BUILD_STEP_OFFSET;
    }

    public boolean legacyPreparedBuilding() {
        return active() && step >= 0 && step < GRADE_STEP_OFFSET;
    }

    public int gradeStep() {
        return grading() ? step - GRADE_STEP_OFFSET : 0;
    }

    public int buildStep() {
        return physicalBuilding() ? step - BUILD_STEP_OFFSET : Math.max(0, step);
    }

    public boolean ownsScaffold(int index) {
        return index >= 0 && index < Integer.SIZE && (scaffoldMask & (1 << index)) != 0;
    }

    public ConstructionState advance() {
        return new ConstructionState(type, originX, originY, originZ, rotation, step + 1, scaffoldMask);
    }

    public ConstructionState withStep(int nextStep) {
        return new ConstructionState(type, originX, originY, originZ, rotation, Math.max(0, nextStep), scaffoldMask);
    }

    public ConstructionState withScaffoldMask(int nextMask) {
        return new ConstructionState(type, originX, originY, originZ, rotation, step, Math.max(0, nextMask));
    }
}
