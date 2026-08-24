package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public record RoadConstructionState(int startX, int startY, int startZ,
                                    int directionX, int directionZ,
                                    int length, int step,
                                    List<Integer> path,
                                    List<Integer> profile,
                                    List<Integer> bridgeSupports) {
    /**
     * Phase markers live inside the existing persisted step field so older saves decode unchanged.
     * Small steps are Alpha.24-or-earlier prepaid paving, 1M+ is grading, and 2M+ is Alpha.25 physical paving.
     * Alpha.35 adds an optional per-center profile. Alpha.52 adds optional exact long-bridge support cells.
     */
    public static final int GRADE_STEP_OFFSET = 1_000_000;
    public static final int PAVE_STEP_OFFSET = 2_000_000;
    public static final int PROFILE_NORMAL = 0;
    public static final int PROFILE_BRIDGE = 1;
    public static final RoadConstructionState EMPTY = new RoadConstructionState(0, 0, 0, 0, 0, 0, 0,
            List.of(), List.of(), List.of());

    public static final Codec<RoadConstructionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("start_x", 0).forGetter(RoadConstructionState::startX),
            Codec.INT.optionalFieldOf("start_y", 0).forGetter(RoadConstructionState::startY),
            Codec.INT.optionalFieldOf("start_z", 0).forGetter(RoadConstructionState::startZ),
            Codec.INT.optionalFieldOf("direction_x", 0).forGetter(RoadConstructionState::directionX),
            Codec.INT.optionalFieldOf("direction_z", 0).forGetter(RoadConstructionState::directionZ),
            Codec.INT.optionalFieldOf("length", 0).forGetter(RoadConstructionState::length),
            Codec.INT.optionalFieldOf("step", 0).forGetter(RoadConstructionState::encodedStep),
            Codec.INT.listOf().optionalFieldOf("path", List.of()).forGetter(RoadConstructionState::path),
            Codec.INT.listOf().optionalFieldOf("profile", List.of()).forGetter(RoadConstructionState::profile),
            Codec.INT.listOf().optionalFieldOf("bridge_supports", List.of()).forGetter(RoadConstructionState::bridgeSupports)
    ).apply(instance, RoadConstructionState::new));

    public RoadConstructionState(int startX, int startY, int startZ,
                                 int directionX, int directionZ, int length, int step) {
        this(startX, startY, startZ, directionX, directionZ, length, step, List.of(), List.of(), List.of());
    }

    public RoadConstructionState(int startX, int startY, int startZ,
                                 int directionX, int directionZ, int length, int step, List<Integer> path) {
        this(startX, startY, startZ, directionX, directionZ, length, step, path, List.of(), List.of());
    }

    public RoadConstructionState(int startX, int startY, int startZ,
                                 int directionX, int directionZ, int length, int step,
                                 List<Integer> path, List<Integer> profile) {
        this(startX, startY, startZ, directionX, directionZ, length, step, path, profile, List.of());
    }

    public static RoadConstructionState fromPath(List<BlockPos> centers) {
        return fromPath(centers, List.of(), List.of());
    }

    public static RoadConstructionState fromPath(List<BlockPos> centers, List<Integer> profile) {
        return fromPath(centers, profile, List.of());
    }

    public static RoadConstructionState fromPath(List<BlockPos> centers, List<Integer> profile,
                                                 List<BlockPos> bridgeSupports) {
        if (centers == null || centers.size() < 2) return EMPTY;
        BlockPos first = centers.get(0);
        BlockPos last = centers.get(centers.size() - 1);
        BlockPos beforeLast = centers.get(centers.size() - 2);
        int directionX = Integer.signum(last.getX() - beforeLast.getX());
        int directionZ = Integer.signum(last.getZ() - beforeLast.getZ());
        List<Integer> encoded = encodePositions(centers);
        List<Integer> normalizedProfile = new ArrayList<>(centers.size());
        for (int i = 0; i < centers.size(); i++) {
            int value = profile != null && i < profile.size() ? profile.get(i) : PROFILE_NORMAL;
            normalizedProfile.add(value == PROFILE_BRIDGE ? PROFILE_BRIDGE : PROFILE_NORMAL);
        }
        List<Integer> encodedSupports = encodePositions(bridgeSupports == null ? List.of() : bridgeSupports);
        return new RoadConstructionState(first.getX(), first.getY(), first.getZ(),
                directionX, directionZ, centers.size(), GRADE_STEP_OFFSET,
                List.copyOf(encoded), List.copyOf(normalizedProfile), List.copyOf(encodedSupports));
    }

    private static List<Integer> encodePositions(List<BlockPos> positions) {
        if (positions == null || positions.isEmpty()) return List.of();
        List<Integer> encoded = new ArrayList<>(positions.size() * 3);
        for (BlockPos pos : positions) {
            encoded.add(pos.getX());
            encoded.add(pos.getY());
            encoded.add(pos.getZ());
        }
        return encoded;
    }

    private static List<BlockPos> decodePositions(List<Integer> encoded) {
        if (encoded == null || encoded.size() < 3 || encoded.size() % 3 != 0) return List.of();
        List<BlockPos> positions = new ArrayList<>(encoded.size() / 3);
        for (int i = 0; i + 2 < encoded.size(); i += 3) {
            positions.add(new BlockPos(encoded.get(i), encoded.get(i + 1), encoded.get(i + 2)));
        }
        return List.copyOf(positions);
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
        if (hasPath()) return decodePositions(path);
        if (length <= 0 || Math.abs(directionX) + Math.abs(directionZ) != 1) return List.of();
        List<BlockPos> legacy = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            legacy.add(new BlockPos(startX + directionX * i, startY, startZ + directionZ * i));
        }
        return List.copyOf(legacy);
    }

    public List<BlockPos> bridgeSupportPositions() {
        return decodePositions(bridgeSupports);
    }

    public boolean bridgeAt(int centerIndex) {
        return centerIndex >= 0 && profile != null && centerIndex < profile.size()
                && profile.get(centerIndex) == PROFILE_BRIDGE;
    }

    public int bridgeCenterCount() {
        int count = 0;
        for (int i = 0; i < centers().size(); i++) if (bridgeAt(i)) count++;
        return count;
    }

    public int bridgeSupportCount() {
        return bridgeSupportPositions().size();
    }

    public RoadConstructionState advance() {
        return new RoadConstructionState(startX, startY, startZ, directionX, directionZ, length,
                step + 1, path, profile, bridgeSupports);
    }

    public RoadConstructionState withStep(int nextStep) {
        int encoded = grading() && nextStep == 0 ? PAVE_STEP_OFFSET : Math.max(0, nextStep);
        return new RoadConstructionState(startX, startY, startZ, directionX, directionZ, length,
                encoded, path, profile, bridgeSupports);
    }
}
