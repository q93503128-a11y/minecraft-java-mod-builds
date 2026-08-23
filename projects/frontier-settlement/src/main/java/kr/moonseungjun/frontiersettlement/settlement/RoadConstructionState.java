package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public record RoadConstructionState(int startX, int startY, int startZ,
                                    int directionX, int directionZ,
                                    int length, int step,
                                    List<Integer> path) {
    /**
     * Phase markers live inside the existing persisted step field so older saves decode unchanged.
     * Small steps are Alpha.24-or-earlier prepaid paving, 1M+ is grading, and 2M+ is Alpha.25 physical paving.
     */
    public static final int GRADE_STEP_OFFSET = 1_000_000;
    public static final int PAVE_STEP_OFFSET = 2_000_000;
    public static final RoadConstructionState EMPTY = new RoadConstructionState(0, 0, 0, 0, 0, 0, 0, List.of());

    public static final Codec<RoadConstructionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("start_x", 0).forGetter(RoadConstructionState::startX),
            Codec.INT.optionalFieldOf("start_y", 0).forGetter(RoadConstructionState::startY),
            Codec.INT.optionalFieldOf("start_z", 0).forGetter(RoadConstructionState::startZ),
            Codec.INT.optionalFieldOf("direction_x", 0).forGetter(RoadConstructionState::directionX),
            Codec.INT.optionalFieldOf("direction_z", 0).forGetter(RoadConstructionState::directionZ),
            Codec.INT.optionalFieldOf("length", 0).forGetter(RoadConstructionState::length),
            Codec.INT.optionalFieldOf("step", 0).forGetter(RoadConstructionState::encodedStep),
            Codec.INT.listOf().optionalFieldOf("path", List.of()).forGetter(RoadConstructionState::path)
    ).apply(instance, RoadConstructionState::new));

    public RoadConstructionState(int startX, int startY, int startZ,
                                 int directionX, int directionZ, int length, int step) {
        this(startX, startY, startZ, directionX, directionZ, length, step, List.of());
    }

    public static RoadConstructionState fromPath(List<BlockPos> centers) {
        if (centers == null || centers.size() < 2) return EMPTY;
        BlockPos first = centers.get(0);
        BlockPos last = centers.get(centers.size() - 1);
        BlockPos beforeLast = centers.get(centers.size() - 2);
        int directionX = Integer.signum(last.getX() - beforeLast.getX());
        int directionZ = Integer.signum(last.getZ() - beforeLast.getZ());
        List<Integer> encoded = new ArrayList<>(centers.size() * 3);
        for (BlockPos center : centers) {
            encoded.add(center.getX());
            encoded.add(center.getY());
            encoded.add(center.getZ());
        }
        return new RoadConstructionState(first.getX(), first.getY(), first.getZ(),
                directionX, directionZ, centers.size(), GRADE_STEP_OFFSET, List.copyOf(encoded));
    }

    public boolean active() {
        if (hasPath()) return centers().size() >= 2;
        return length > 0 && Math.abs(directionX) + Math.abs(directionZ) == 1;
    }

    public boolean hasPath() {
        return path != null && path.size() >= 6 && path.size() % 3 == 0;
    }

    public boolean grading() {
        return hasPath() && step >= GRADE_STEP_OFFSET && step < PAVE_STEP_OFFSET;
    }

    public boolean physicalPaving() {
        return hasPath() && step >= PAVE_STEP_OFFSET;
    }

    public boolean legacyPrepaidPaving() {
        return active() && step >= 0 && step < GRADE_STEP_OFFSET;
    }

    public int gradeStep() {
        return grading() ? step - GRADE_STEP_OFFSET : -1;
    }

    /**
     * Runtime callers historically use step() as the paving cursor. New physical paving exposes the
     * logical cursor; legacy prepaid roads intentionally report completion so SettlementRoadService
     * enters its cost-free final validation/repair path instead of charging their stone a second time.
     */
    public int step() {
        if (physicalPaving()) return step - PAVE_STEP_OFFSET;
        if (legacyPrepaidPaving()) return Integer.MAX_VALUE;
        return step;
    }

    private int encodedStep() {
        return step;
    }

    public BlockPos start() {
        List<BlockPos> centers = centers();
        return centers.isEmpty() ? new BlockPos(startX, startY, startZ) : centers.get(0);
    }

    public List<BlockPos> centers() {
        if (hasPath()) {
            List<BlockPos> centers = new ArrayList<>(path.size() / 3);
            for (int i = 0; i + 2 < path.size(); i += 3) {
                centers.add(new BlockPos(path.get(i), path.get(i + 1), path.get(i + 2)));
            }
            return List.copyOf(centers);
        }
        if (length <= 0 || Math.abs(directionX) + Math.abs(directionZ) != 1) return List.of();
        List<BlockPos> legacy = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            legacy.add(new BlockPos(startX + directionX * i, startY, startZ + directionZ * i));
        }
        return List.copyOf(legacy);
    }

    public RoadConstructionState advance() {
        return new RoadConstructionState(startX, startY, startZ, directionX, directionZ, length, step + 1, path);
    }

    public RoadConstructionState withStep(int nextStep) {
        int encoded = grading() && nextStep == 0 ? PAVE_STEP_OFFSET : Math.max(0, nextStep);
        return new RoadConstructionState(startX, startY, startZ, directionX, directionZ, length, encoded, path);
    }
}
