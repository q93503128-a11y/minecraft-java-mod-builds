#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
P = ROOT / 'projects/frontier-settlement'
JAVA = P / 'src/main/java/kr/moonseungjun/frontiersettlement'


def read(path: Path) -> str:
    return path.read_text(encoding='utf-8')


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding='utf-8')


def replace_once(path: Path, old: str, new: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{path}: expected exactly one old occurrence, found {count}: {old[:120]!r}')
    write(path, text.replace(old, new, 1))


# ---------------------------------------------------------------------------
# Persist exact long-bridge pier cells without changing old save semantics.
# ---------------------------------------------------------------------------
write(JAVA / 'settlement/RoadConstructionState.java', '''package kr.moonseungjun.frontiersettlement.settlement;

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
''')

# SettlementData adds a save-compatible overload; all older callers continue to work.
data_path = JAVA / 'settlement/SettlementData.java'
replace_once(data_path,
'''    public void beginRoadConstruction(List<BlockPos> centers) { beginRoadConstruction(centers, List.of()); }

    public void beginRoadConstruction(List<BlockPos> centers, List<Integer> profile) {
        RoadConstructionState next = RoadConstructionState.fromPath(centers, profile);
        if (!next.active()) return;
        infrastructure = new SettlementInfrastructureState(buildings(), roads(), next, outposts(), outpostConstruction());
        setDirty();
    }''',
'''    public void beginRoadConstruction(List<BlockPos> centers) { beginRoadConstruction(centers, List.of(), List.of()); }

    public void beginRoadConstruction(List<BlockPos> centers, List<Integer> profile) {
        beginRoadConstruction(centers, profile, List.of());
    }

    public void beginRoadConstruction(List<BlockPos> centers, List<Integer> profile, List<BlockPos> bridgeSupports) {
        RoadConstructionState next = RoadConstructionState.fromPath(centers, profile, bridgeSupports);
        if (!next.active()) return;
        infrastructure = new SettlementInfrastructureState(buildings(), roads(), next, outposts(), outpostConstruction());
        setDirty();
    }''')

road = JAVA / 'settlement/SettlementRoadService.java'
replace_once(road,
'''import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;''',
'''import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;''')
replace_once(road,
'''    private static final int MAX_FILL_DEPTH = 2;
    private static final int MAX_BRIDGE_SPAN = 6;
    private static final int BRIDGE_SURCHARGE_PER_CENTER = 2;
    private static final int STAIR_SURCHARGE_PER_CENTER = 1;
    private static final int DIRECT_BLOCK_UPDATE = 2;
    private static final int NORMAL_BLOCK_UPDATE = 3;
    private static final double BUILDER_WORK_RANGE_SQR = 18.0D;
    private static final double BRIDGE_WORK_RANGE_SQR = 64.0D;''',
'''    private static final int MAX_FILL_DEPTH = 2;
    private static final int MAX_SHORT_BRIDGE_SPAN = 6;
    private static final int MAX_LONG_BRIDGE_SPAN = 24;
    private static final int MIN_RAVINE_DEPTH = 4;
    private static final int MAX_LONG_BRIDGE_PIER_DEPTH = 12;
    private static final int LONG_BRIDGE_PIER_INTERVAL = 6;
    private static final int BRIDGE_SURCHARGE_PER_CENTER = 2;
    private static final int BRIDGE_SUPPORT_SURCHARGE = 1;
    private static final int STAIR_SURCHARGE_PER_CENTER = 1;
    private static final int DIRECT_BLOCK_UPDATE = 2;
    private static final int NORMAL_BLOCK_UPDATE = 3;
    private static final double BUILDER_WORK_RANGE_SQR = 18.0D;
    private static final double BRIDGE_WORK_RANGE_SQR = 16.0D;
    private static final double BRIDGE_SUPPORT_WORK_RANGE_SQR = 196.0D;''')
replace_once(road,
'''    private record RouteCandidate(boolean valid, List<BlockPos> centers, List<Integer> profile, int score, String message) {}
    private record Placement(BlockPos pos, BlockState state, boolean bridge) {}
    private record FootprintSpec(boolean centerline, boolean bridge, Direction stairFacing) {}
    private record SurfaceSample(int y, BlockState state, boolean water) {}''',
'''    private record RouteCandidate(boolean valid, List<BlockPos> centers, List<Integer> profile,
                                  List<BlockPos> supports, int score, String message) {}
    private record Placement(BlockPos pos, BlockState state, boolean bridge, boolean support) {}
    private record FootprintSpec(boolean centerline, boolean bridge, Direction stairFacing) {}
    private record SurfaceSample(int y, BlockState state, boolean water) {}
    private record SupportPlan(boolean valid, List<BlockPos> positions, String message) {
        static SupportPlan invalid(String message) { return new SupportPlan(false, List.of(), message); }
    }
    private record PierColumn(boolean valid, List<BlockPos> positions, String message) {
        static PierColumn invalid(String message) { return new PierColumn(false, List.of(), message); }
    }''')

replace_once(road,
'''        int cost = stoneCost(chosen);
        SettlementService.refreshResources(server, data);
        String resource = data.resources().stone() < cost
                ? " | 석재 부족: 필요 " + cost + " / 현재 " + data.resources().stone()
                : " | 석재 " + cost;
        int bridges = bridgeCenterCount(chosen.profile());
        int stairs = stairCenterCount(chosen.centers(), chosen.profile());
        String terrain = (stairs == 0 && bridges == 0) ? "" : " | 계단 " + stairs + " · 소교량 " + bridges;
        return new RouteCheck(true, chosen.centers(), cost,
                "경로 " + chosen.centers().size() + "블록" + terrain + resource);''',
'''        if (!chosen.supports().isEmpty() && SettlementTier.current(data).ordinal() < SettlementTier.VILLAGE.ordinal()) {
            return invalid("교각이 필요한 장교량·협곡 횡단은 마을 단계부터 건설할 수 있습니다.");
        }
        int cost = stoneCost(chosen);
        SettlementService.refreshResources(server, data);
        String resource = data.resources().stone() < cost
                ? " | 석재 부족: 필요 " + cost + " / 현재 " + data.resources().stone()
                : " | 석재 " + cost;
        int bridges = bridgeCenterCount(chosen.profile());
        int stairs = stairCenterCount(chosen.centers(), chosen.profile());
        String bridgeDetail = bridges == 0 ? ""
                : chosen.supports().isEmpty() ? " · 소교량 " + bridges
                : " · 장교량 " + bridges + " · 교각 " + chosen.supports().size();
        String terrain = (stairs == 0 && bridges == 0) ? "" : " | 계단 " + stairs + bridgeDetail;
        return new RouteCheck(true, chosen.centers(), cost,
                "경로 " + chosen.centers().size() + "블록" + terrain + resource);''')
replace_once(road,
'''        data.beginRoadConstruction(chosen.centers(), chosen.profile());
        SettlementConstructionService.ensureBuilder(level, data.centerPos());
        SettlementService.broadcast(server, data);
        return new StartResult(true, "개척 도로 착공: " + chosen.centers().size()
                + "블록 경로, 3칸 폭. 건설 주민이 지반·계단·짧은 수로 교량을 정리한 뒤 공동 창고의 실제 석재 "
                + requiredStone + "개를 운반하며 포설합니다.");''',
'''        data.beginRoadConstruction(chosen.centers(), chosen.profile(), chosen.supports());
        SettlementConstructionService.ensureBuilder(level, data.centerPos());
        SettlementService.broadcast(server, data);
        String bridge = chosen.supports().isEmpty() ? ""
                : " 장교량/협곡 교각 " + chosen.supports().size() + "블록 포함.";
        return new StartResult(true, "개척 도로 착공: " + chosen.centers().size()
                + "블록 경로, 3칸 폭." + bridge + " 건설 주민이 지반·계단·교량을 정리한 뒤 공동 창고의 실제 석재 "
                + requiredStone + "개를 운반하며 포설합니다.");''')

replace_once(road,
'''    private static boolean tickGrading(MinecraftServer server, SettlementData data, RoadConstructionState road,
                                       List<Placement> plan, Villager builder) {
        int gradeStep = road.gradeStep();
        if (gradeStep >= plan.size()) {
            data.replaceRoadConstructionStep(0);
            SettlementService.broadcast(server, data);
            return false;
        }

        ServerLevel level = server.overworld();
        Placement placement = plan.get(gradeStep);
        if (!moveBuilderToPlacement(level, builder, placement)) return false;
        if (!canGradePlacement(level, placement)) {
            builder.getNavigation().stop();
            return false;
        }

        applyGradePlacement(level, placement);
        builder.swing(InteractionHand.MAIN_HAND);
        data.advanceRoadConstruction();
        RoadConstructionState next = data.roadConstruction();
        if (next.grading() && next.gradeStep() >= plan.size()) {
            data.replaceRoadConstructionStep(0);
            SettlementService.broadcast(server, data);
        }
        return false;
    }''',
'''    private static boolean tickGrading(MinecraftServer server, SettlementData data, RoadConstructionState road,
                                       List<Placement> plan, Villager builder) {
        int gradeStep = road.gradeStep();
        if (gradeStep >= plan.size()) {
            data.replaceRoadConstructionStep(0);
            SettlementService.broadcast(server, data);
            return false;
        }

        ServerLevel level = server.overworld();
        Placement placement = plan.get(gradeStep);
        if (!canGradePlacement(level, placement)) {
            builder.getNavigation().stop();
            return false;
        }
        // Bridge decks/supports occupy empty/water columns. Validation is enough here; physical stone work is paving.
        if (!placement.bridge() && !moveBuilderToPlacement(level, builder, placement)) return false;
        applyGradePlacement(level, placement);
        builder.swing(InteractionHand.MAIN_HAND);
        data.advanceRoadConstruction();
        RoadConstructionState next = data.roadConstruction();
        if (next.grading() && next.gradeStep() >= plan.size()) {
            data.replaceRoadConstructionStep(0);
            SettlementService.broadcast(server, data);
        }
        return false;
    }''')

replace_once(road,
'''    private static boolean tickPaving(MinecraftServer server, SettlementData data, RoadConstructionState road,
                                      List<Placement> plan, Villager builder) {
        int step = road.step();
        int totalCost = stoneCost(road);
        long spentBefore = costAtStep(totalCost, step, plan.size());
        long spentAfter = costAtStep(totalCost, step + 1, plan.size());
        long stoneDelta = spentAfter - spentBefore;
        long remainingCost = Math.max(0L, totalCost - spentBefore);
        if (!ensurePavingMaterial(server, data, builder, stoneDelta, remainingCost)) return false;

        ServerLevel level = server.overworld();
        Placement placement = plan.get(step);
        if (!moveBuilderToPlacement(level, builder, placement)) return false;

        BlockPos target = placement.pos();
        BlockState current = level.getBlockState(target);
        if (!current.is(placement.state().getBlock()) && !current.isAir() && !isRoadGround(current)) {
            builder.getNavigation().stop();
            return false;
        }
        if (!placement.bridge()) {
            BlockState support = level.getBlockState(target.below());
            if (support.isAir() || support.canBeReplaced() || !support.getFluidState().isEmpty()) {
                builder.getNavigation().stop();
                return false;
            }
        }
        if (!consumeCarriedStone(builder, stoneDelta)) return false;

        if (!current.is(placement.state().getBlock())) {
            level.setBlock(target, placement.state(), NORMAL_BLOCK_UPDATE);
            builder.swing(InteractionHand.MAIN_HAND);
        }
        data.advanceRoadConstruction();
        if (data.roadConstruction().step() >= plan.size()) {
            return finishIfValid(server, data, data.roadConstruction(), plan, builder);
        }
        return false;
    }''',
'''    private static boolean tickPaving(MinecraftServer server, SettlementData data, RoadConstructionState road,
                                      List<Placement> plan, Villager builder) {
        int step = road.step();
        int totalCost = stoneCost(road);
        long spentBefore = costAtStep(totalCost, step, plan.size());
        long spentAfter = costAtStep(totalCost, step + 1, plan.size());
        long stoneDelta = spentAfter - spentBefore;
        long remainingCost = Math.max(0L, totalCost - spentBefore);
        if (!ensurePavingMaterial(server, data, builder, stoneDelta, remainingCost)) return false;

        ServerLevel level = server.overworld();
        Placement placement = plan.get(step);
        if (!moveBuilderToPlacement(level, builder, placement)) return false;

        BlockPos target = placement.pos();
        BlockState current = level.getBlockState(target);
        if (!current.is(placement.state().getBlock()) && !canReplaceForPlacement(current, placement)) {
            builder.getNavigation().stop();
            return false;
        }
        if (!placement.bridge()) {
            BlockState support = level.getBlockState(target.below());
            if (support.isAir() || support.canBeReplaced() || !support.getFluidState().isEmpty()) {
                builder.getNavigation().stop();
                return false;
            }
        }

        boolean changed = false;
        if (!current.is(placement.state().getBlock())) {
            if (!level.setBlock(target, placement.state(), NORMAL_BLOCK_UPDATE)) return false;
            changed = true;
        }
        if (!consumeCarriedStone(builder, stoneDelta)) {
            if (changed) level.setBlock(target, current, DIRECT_BLOCK_UPDATE);
            return false;
        }
        if (changed) builder.swing(InteractionHand.MAIN_HAND);
        data.advanceRoadConstruction();
        if (data.roadConstruction().step() >= plan.size()) {
            return finishIfValid(server, data, data.roadConstruction(), plan, builder);
        }
        return false;
    }

    private static boolean canReplaceForPlacement(BlockState current, Placement placement) {
        if (placement.support()) {
            return current.isAir() || current.canBeReplaced() || current.getFluidState().is(FluidTags.WATER);
        }
        return current.isAir() || isRoadGround(current);
    }''')

replace_once(road,
'''    private static boolean moveBuilderToPlacement(ServerLevel level, Villager builder, Placement placement) {
        if (!level.hasChunkAt(placement.pos())) {
            builder.getNavigation().stop();
            return false;
        }
        if (placement.bridge()) {
            double distance = builder.distanceToSqr(placement.pos().getX() + 0.5D,
                    placement.pos().getY(), placement.pos().getZ() + 0.5D);
            if (distance <= BRIDGE_WORK_RANGE_SQR) return true;
        }
        return moveBuilderToCurrentSurface(level, builder, placement.pos());
    }''',
'''    private static boolean moveBuilderToPlacement(ServerLevel level, Villager builder, Placement placement) {
        if (!level.hasChunkAt(placement.pos())) {
            builder.getNavigation().stop();
            return false;
        }
        if (placement.support()) {
            BlockPos deck = findBridgeDeckAbove(level, placement.pos());
            if (deck == null) return false;
            double distance = builder.distanceToSqr(deck.getX() + 0.5D, deck.getY(), deck.getZ() + 0.5D);
            if (distance <= BRIDGE_SUPPORT_WORK_RANGE_SQR) return true;
            builder.getNavigation().moveTo(deck.getX() + 0.5D, deck.getY(), deck.getZ() + 0.5D, 0.82D);
            return false;
        }
        if (placement.bridge()) {
            double distance = builder.distanceToSqr(placement.pos().getX() + 0.5D,
                    placement.pos().getY(), placement.pos().getZ() + 0.5D);
            if (distance <= BRIDGE_WORK_RANGE_SQR) return true;
            BlockPos approach = bridgeApproach(level, placement.pos());
            if (approach != null) {
                builder.getNavigation().moveTo(approach.getX() + 0.5D, approach.getY(), approach.getZ() + 0.5D, 0.82D);
                return false;
            }
        }
        return moveBuilderToCurrentSurface(level, builder, placement.pos());
    }

    private static BlockPos bridgeApproach(ServerLevel level, BlockPos target) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = target.relative(direction);
            if (isRoadPavingBlock(level.getBlockState(neighbor))) return neighbor;
        }
        return null;
    }

    private static BlockPos findBridgeDeckAbove(ServerLevel level, BlockPos support) {
        for (int dy = 1; dy <= MAX_LONG_BRIDGE_PIER_DEPTH; dy++) {
            BlockPos candidate = support.above(dy);
            if (level.getBlockState(candidate).is(Blocks.STONE_BRICKS)) return candidate;
        }
        return null;
    }

    private static boolean isRoadPavingBlock(BlockState state) {
        return state.is(Blocks.GRAVEL) || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.COBBLESTONE_STAIRS) || state.is(Blocks.STONE_BRICKS);
    }''')

replace_once(road,
'''    private static boolean canGradePlacement(ServerLevel level, Placement placement) {
        BlockPos target = placement.pos();
        BlockState current = level.getBlockState(target);
        if (level.getBlockEntity(target) != null || !current.getFluidState().isEmpty()) return false;
        if (!current.isAir() && !current.canBeReplaced() && !isRoadGround(current)) return false;

        for (int y = target.getY() + 1; y <= target.getY() + 2; y++) {
            BlockPos pos = new BlockPos(target.getX(), y, target.getZ());
            BlockState state = level.getBlockState(pos);
            if (level.getBlockEntity(pos) != null || !state.getFluidState().isEmpty()) return false;
            if (!isClearableForRoad(state)) return false;
        }
        return placement.bridge() || hasOrCanMakeSupport(level, target.below());
    }

    private static void applyGradePlacement(ServerLevel level, Placement placement) {
        BlockPos target = placement.pos();
        for (int y = target.getY() + 2; y >= target.getY() + 1; y--) {
            BlockPos pos = new BlockPos(target.getX(), y, target.getZ());
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && isClearableForRoad(state)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE);
            }
        }
        if (placement.bridge()) {
            BlockState targetState = level.getBlockState(target);
            if (targetState.canBeReplaced() && targetState.getFluidState().isEmpty()) {
                level.setBlock(target, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE);
            }
            return;
        }''',
'''    private static boolean canGradePlacement(ServerLevel level, Placement placement) {
        BlockPos target = placement.pos();
        BlockState current = level.getBlockState(target);
        if (level.getBlockEntity(target) != null) return false;
        if (placement.support()) {
            if (!current.getFluidState().isEmpty() && !current.getFluidState().is(FluidTags.WATER)) return false;
            return current.is(placement.state().getBlock()) || current.isAir() || current.canBeReplaced()
                    || current.getFluidState().is(FluidTags.WATER);
        }
        if (!current.getFluidState().isEmpty()) return false;
        if (!current.isAir() && !current.canBeReplaced() && !isRoadGround(current)) return false;

        for (int y = target.getY() + 1; y <= target.getY() + 2; y++) {
            BlockPos pos = new BlockPos(target.getX(), y, target.getZ());
            BlockState state = level.getBlockState(pos);
            if (level.getBlockEntity(pos) != null || !state.getFluidState().isEmpty()) return false;
            if (!isClearableForRoad(state)) return false;
        }
        return placement.bridge() || hasOrCanMakeSupport(level, target.below());
    }

    private static void applyGradePlacement(ServerLevel level, Placement placement) {
        if (placement.support()) return;
        BlockPos target = placement.pos();
        for (int y = target.getY() + 2; y >= target.getY() + 1; y--) {
            BlockPos pos = new BlockPos(target.getX(), y, target.getZ());
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && isClearableForRoad(state)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE);
            }
        }
        if (placement.bridge()) {
            BlockState targetState = level.getBlockState(target);
            if (targetState.canBeReplaced() && targetState.getFluidState().isEmpty()) {
                level.setBlock(target, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE);
            }
            return;
        }''')

replace_once(road,
'''    private static int stoneCost(RouteCandidate candidate) {
        return stoneCost(candidate.centers().size())
                + bridgeCenterCount(candidate.profile()) * BRIDGE_SURCHARGE_PER_CENTER
                + stairCenterCount(candidate.centers(), candidate.profile()) * STAIR_SURCHARGE_PER_CENTER;
    }

    private static int stoneCost(RoadConstructionState road) {
        return stoneCost(road.centers().size())
                + road.bridgeCenterCount() * BRIDGE_SURCHARGE_PER_CENTER
                + stairCenterCount(road.centers(), road.profile()) * STAIR_SURCHARGE_PER_CENTER;
    }''',
'''    private static int stoneCost(RouteCandidate candidate) {
        return stoneCost(candidate.centers().size())
                + bridgeCenterCount(candidate.profile()) * BRIDGE_SURCHARGE_PER_CENTER
                + candidate.supports().size() * BRIDGE_SUPPORT_SURCHARGE
                + stairCenterCount(candidate.centers(), candidate.profile()) * STAIR_SURCHARGE_PER_CENTER;
    }

    private static int stoneCost(RoadConstructionState road) {
        return stoneCost(road.centers().size())
                + road.bridgeCenterCount() * BRIDGE_SURCHARGE_PER_CENTER
                + road.bridgeSupportCount() * BRIDGE_SUPPORT_SURCHARGE
                + stairCenterCount(road.centers(), road.profile()) * STAIR_SURCHARGE_PER_CENTER;
    }''')
replace_once(road,
'''        if (road.grading()) return road.bridgeCenterCount() > 0 ? "도로 지반·교량 자리 정리" : "도로 지반 정리";
        if (road.step() < plan.size()) return road.bridgeCenterCount() > 0 ? "도로 계단·교량 석재 운반·포설" : "도로 석재 운반·포설";''',
'''        if (road.grading()) return road.bridgeSupportCount() > 0 ? "도로 장교량·교각 자리 검사"
                : road.bridgeCenterCount() > 0 ? "도로 지반·교량 자리 정리" : "도로 지반 정리";
        if (road.step() < plan.size()) return road.bridgeSupportCount() > 0 ? "도로 장교량·교각 석재 운반·시공"
                : road.bridgeCenterCount() > 0 ? "도로 계단·교량 석재 운반·포설" : "도로 석재 운반·포설";''')

replace_once(road,
'''    private static boolean finishIfValid(MinecraftServer server, SettlementData data,
                                         RoadConstructionState road, List<Placement> plan, Villager builder) {
        ServerLevel level = server.overworld();
        for (Placement placement : plan) {
            BlockState current = level.getBlockState(placement.pos());
            if (current.is(placement.state().getBlock())) continue;
            if (!current.isAir() && !isRoadGround(current)) {
                builder.getNavigation().stop();
                return false;
            }
            if (!placement.bridge()) {
                BlockState support = level.getBlockState(placement.pos().below());
                if (support.isAir() || support.canBeReplaced() || !support.getFluidState().isEmpty()) {
                    builder.getNavigation().stop();
                    return false;
                }
            }
            if (!moveBuilderToPlacement(level, builder, placement)) return false;
            level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE);
            builder.swing(InteractionHand.MAIN_HAND);
            return false;
        }

        if (!returnCarriedToStorage(server, data, builder)) return false;
        data.completeRoad(RoadSegment.fromPath(road.centers()));
        builder.getNavigation().stop();
        builder.setInvulnerable(false);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        return true;
    }''',
'''    private static boolean finishIfValid(MinecraftServer server, SettlementData data,
                                         RoadConstructionState road, List<Placement> plan, Villager builder) {
        ServerLevel level = server.overworld();
        for (Placement placement : plan) {
            BlockState current = level.getBlockState(placement.pos());
            if (current.is(placement.state().getBlock())) continue;
            if (!canReplaceForPlacement(current, placement)) {
                builder.getNavigation().stop();
                return false;
            }
            if (!placement.bridge()) {
                BlockState support = level.getBlockState(placement.pos().below());
                if (support.isAir() || support.canBeReplaced() || !support.getFluidState().isEmpty()) {
                    builder.getNavigation().stop();
                    return false;
                }
            }
            if (!ensurePavingMaterial(server, data, builder, 1L, 1L)) return false;
            if (!moveBuilderToPlacement(level, builder, placement)) return false;
            if (!level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)) return false;
            if (!consumeCarriedStone(builder, 1L)) {
                level.setBlock(placement.pos(), current, DIRECT_BLOCK_UPDATE);
                return false;
            }
            builder.swing(InteractionHand.MAIN_HAND);
            return false;
        }

        if (!returnCarriedToStorage(server, data, builder)) return false;
        data.completeRoad(RoadSegment.fromPath(road.centers()));
        builder.getNavigation().stop();
        builder.setInvulnerable(false);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        return true;
    }''')

replace_once(road,
'''        List<Integer> profile = new ArrayList<>(java.util.Collections.nCopies(flat.size(), RoadConstructionState.PROFILE_NORMAL));
        int[] bridgeY = new int[flat.size()];
        java.util.Arrays.fill(bridgeY, Integer.MIN_VALUE);
        for (int i = 1; i < flat.size() - 1; i++) {
            if (!surfaces.get(i).water()) continue;
            int startWater = i;
            int endWater = i;
            while (endWater + 1 < flat.size() && surfaces.get(endWater + 1).water()) endWater++;
            int span = endWater - startWater + 1;
            if (endWater >= flat.size() - 1 || span > MAX_BRIDGE_SPAN) {
                return invalidCandidate("자동 소교량은 양쪽 둑이 있는 최대 " + MAX_BRIDGE_SPAN + "칸 수로만 건널 수 있습니다.");
            }
            SurfaceSample before = surfaces.get(startWater - 1);
            SurfaceSample after = surfaces.get(endWater + 1);
            if (before.water() || after.water() || Math.abs(before.y() - after.y()) > MAX_STEP_HEIGHT) {
                return invalidCandidate("수로 양쪽 둑의 높이 차가 커서 작은 교량을 안전하게 놓을 수 없습니다.");
            }
            int waterTop = Integer.MIN_VALUE;
            for (int j = startWater; j <= endWater; j++) waterTop = Math.max(waterTop, surfaces.get(j).y());
            int deckY = Math.max(Math.max(before.y(), after.y()), waterTop + 1);
            if (Math.abs(deckY - before.y()) > MAX_STEP_HEIGHT || Math.abs(deckY - after.y()) > MAX_STEP_HEIGHT) {
                return invalidCandidate("수면과 둑 높이가 맞지 않아 작은 교량 접근 계단을 만들 수 없습니다.");
            }
            for (int j = startWater; j <= endWater; j++) {
                profile.set(j, RoadConstructionState.PROFILE_BRIDGE);
                bridgeY[j] = deckY;
            }
            i = endWater;
        }

        List<BlockPos> centers = new ArrayList<>(flat.size());''',
'''        List<Integer> profile = new ArrayList<>(java.util.Collections.nCopies(flat.size(), RoadConstructionState.PROFILE_NORMAL));
        int[] bridgeY = new int[flat.size()];
        java.util.Arrays.fill(bridgeY, Integer.MIN_VALUE);

        // Water crossing: Alpha.35 short bridges remain, Alpha.52 extends bounded straight spans to 24.
        for (int i = 1; i < flat.size() - 1; i++) {
            if (!surfaces.get(i).water()) continue;
            int startWater = i;
            int endWater = i;
            while (endWater + 1 < flat.size() && surfaces.get(endWater + 1).water()) endWater++;
            int span = endWater - startWater + 1;
            if (endWater >= flat.size() - 1 || span > MAX_LONG_BRIDGE_SPAN) {
                return invalidCandidate("자동 교량은 양쪽 둑이 있는 최대 " + MAX_LONG_BRIDGE_SPAN + "칸 횡단까지만 지원합니다.");
            }
            SurfaceSample before = surfaces.get(startWater - 1);
            SurfaceSample after = surfaces.get(endWater + 1);
            if (before.water() || after.water() || Math.abs(before.y() - after.y()) > MAX_STEP_HEIGHT) {
                return invalidCandidate("교량 양쪽 접속 지면의 높이 차가 커서 안전하게 연결할 수 없습니다.");
            }
            int waterTop = Integer.MIN_VALUE;
            for (int j = startWater; j <= endWater; j++) waterTop = Math.max(waterTop, surfaces.get(j).y());
            int deckY = Math.max(Math.max(before.y(), after.y()), waterTop + 1);
            if (Math.abs(deckY - before.y()) > MAX_STEP_HEIGHT || Math.abs(deckY - after.y()) > MAX_STEP_HEIGHT) {
                return invalidCandidate("수면과 접속 지면 높이가 맞지 않아 교량 접근부를 만들 수 없습니다.");
            }
            for (int j = startWater; j <= endWater; j++) {
                profile.set(j, RoadConstructionState.PROFILE_BRIDGE);
                bridgeY[j] = deckY;
            }
            i = endWater;
        }

        // Dry ravine crossing: only abrupt, bounded depressions with nearly level shoulders are bridged.
        for (int i = 1; i < flat.size() - 1; i++) {
            if (profile.get(i) == RoadConstructionState.PROFILE_BRIDGE || surfaces.get(i).water()) continue;
            SurfaceSample before = surfaces.get(i - 1);
            if (before.water() || before.y() - surfaces.get(i).y() < MIN_RAVINE_DEPTH) continue;
            int startGap = i;
            int endGap = i;
            while (endGap + 1 < flat.size() - 1
                    && profile.get(endGap + 1) != RoadConstructionState.PROFILE_BRIDGE
                    && !surfaces.get(endGap + 1).water()
                    && surfaces.get(endGap + 1).y() <= before.y() - MIN_RAVINE_DEPTH) {
                endGap++;
            }
            SurfaceSample after = surfaces.get(endGap + 1);
            if (after.water() || Math.abs(before.y() - after.y()) > MAX_STEP_HEIGHT) {
                return invalidCandidate("협곡 양쪽 접속 지면의 높이 차가 커서 장교량으로 연결할 수 없습니다.");
            }
            int span = endGap - startGap + 1;
            if (span > MAX_LONG_BRIDGE_SPAN) {
                return invalidCandidate("협곡 장교량은 최대 " + MAX_LONG_BRIDGE_SPAN + "칸 횡단까지만 지원합니다.");
            }
            int deckY = Math.max(before.y(), after.y());
            for (int j = startGap; j <= endGap; j++) {
                profile.set(j, RoadConstructionState.PROFILE_BRIDGE);
                bridgeY[j] = deckY;
            }
            i = endGap;
        }

        List<BlockPos> centers = new ArrayList<>(flat.size());''')

replace_once(road,
'''        return new RouteCandidate(true, List.copyOf(centers), List.copyOf(profile), score, "");
    }

    private static SurfaceSample sampleSurface''',
'''        SupportPlan supports = planBridgeSupports(level, centers, profile);
        if (!supports.valid()) return invalidCandidate(supports.message());
        score += supports.positions().size() * 2;
        return new RouteCandidate(true, List.copyOf(centers), List.copyOf(profile), supports.positions(), score, "");
    }

    private static SurfaceSample sampleSurface''')

replace_once(road,
'''    private static boolean bridgeColumnSafe(ServerLevel level, BlockPos target) {
        if (!level.hasChunkAt(target)) return false;
        for (int y = target.getY(); y <= target.getY() + 2; y++) {
            BlockPos pos = new BlockPos(target.getX(), y, target.getZ());
            if (level.getBlockEntity(pos) != null) return false;
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty()) return false;
            if (!isClearableForRoad(state)) return false;
        }
        SurfaceSample natural = sampleSurface(level, target.getX(), target.getZ());
        if (!natural.water() && natural.y() > target.getY()) return false;
        return natural.water() || target.getY() - natural.y() <= MAX_FILL_DEPTH + 1;
    }

    private static RouteCandidate invalidCandidate(String message) {
        return new RouteCandidate(false, List.of(), List.of(), Integer.MAX_VALUE, message);
    }''',
'''    private static boolean bridgeColumnSafe(ServerLevel level, BlockPos target) {
        if (!level.hasChunkAt(target)) return false;
        for (int y = target.getY(); y <= target.getY() + 2; y++) {
            BlockPos pos = new BlockPos(target.getX(), y, target.getZ());
            if (level.getBlockEntity(pos) != null) return false;
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty()) return false;
            if (!isClearableForRoad(state)) return false;
        }
        SurfaceSample natural = sampleSurface(level, target.getX(), target.getZ());
        if (!natural.water() && natural.y() > target.getY()) return false;
        return natural.water() || target.getY() - natural.y() <= MAX_LONG_BRIDGE_PIER_DEPTH;
    }

    private static SupportPlan planBridgeSupports(ServerLevel level, List<BlockPos> centers, List<Integer> profile) {
        Set<BlockPos> supports = new LinkedHashSet<>();
        int runStart = 0;
        while (runStart < centers.size()) {
            if (runStart >= profile.size() || profile.get(runStart) != RoadConstructionState.PROFILE_BRIDGE) {
                runStart++;
                continue;
            }
            int runEnd = runStart;
            while (runEnd + 1 < centers.size() && runEnd + 1 < profile.size()
                    && profile.get(runEnd + 1) == RoadConstructionState.PROFILE_BRIDGE) runEnd++;
            int span = runEnd - runStart + 1;
            boolean needsPiers = span > MAX_SHORT_BRIDGE_SPAN;
            for (int i = runStart; i <= runEnd && !needsPiers; i++) {
                SurfaceSample natural = sampleSurface(level, centers.get(i).getX(), centers.get(i).getZ());
                if (!natural.water() && centers.get(i).getY() - natural.y() >= MIN_RAVINE_DEPTH) needsPiers = true;
            }
            if (needsPiers) {
                int[] straight = directionAt(centers, runStart);
                for (int i = runStart + 1; i <= runEnd; i++) {
                    int[] direction = directionAt(centers, i);
                    if (direction[0] != straight[0] || direction[1] != straight[1]) {
                        return SupportPlan.invalid("교각이 필요한 장교량은 현재 직선 구간에서만 시공할 수 있습니다.");
                    }
                }
                List<Integer> stations = new ArrayList<>();
                if (span <= MAX_SHORT_BRIDGE_SPAN) stations.add((runStart + runEnd) / 2);
                else for (int station = runStart + LONG_BRIDGE_PIER_INTERVAL - 1;
                          station < runEnd; station += LONG_BRIDGE_PIER_INTERVAL) stations.add(station);
                for (int station : stations) {
                    BlockPos center = centers.get(station);
                    int[] direction = directionAt(centers, station);
                    for (int side : new int[] {-1, 1}) {
                        BlockPos deckEdge = new BlockPos(center.getX() - direction[1] * side,
                                center.getY(), center.getZ() + direction[0] * side);
                        PierColumn column = planPierColumn(level, deckEdge);
                        if (!column.valid()) return SupportPlan.invalid(column.message());
                        supports.addAll(column.positions());
                    }
                }
            }
            runStart = runEnd + 1;
        }
        return new SupportPlan(true, List.copyOf(supports), "");
    }

    private static PierColumn planPierColumn(ServerLevel level, BlockPos deckEdge) {
        List<BlockPos> positions = new ArrayList<>();
        for (int depth = 1; depth <= MAX_LONG_BRIDGE_PIER_DEPTH; depth++) {
            BlockPos pos = deckEdge.below(depth);
            if (!level.hasChunkAt(pos)) return PierColumn.invalid("장교량 교각 예정 열이 로드되지 않았습니다.");
            if (level.getBlockEntity(pos) != null) return PierColumn.invalid("장교량 교각 열에 보호해야 할 컨테이너가 있습니다.");
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty()) {
                if (!state.getFluidState().is(FluidTags.WATER)) return PierColumn.invalid("장교량 교각 열에 물이 아닌 유체가 있습니다.");
                positions.add(pos);
                continue;
            }
            if (state.isAir() || state.canBeReplaced()) {
                positions.add(pos);
                continue;
            }
            if (isNaturalSupportGround(state)) return new PierColumn(true, List.copyOf(positions), "");
            return PierColumn.invalid("장교량 교각 열에 플레이어 블록·구조물·비자연 지반이 있습니다.");
        }
        return PierColumn.invalid("장교량 교각 지지 지면은 최대 " + MAX_LONG_BRIDGE_PIER_DEPTH + "블록 아래까지만 허용합니다.");
    }

    private static boolean isNaturalSupportGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE) || state.is(Blocks.ANDESITE) || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE) || state.is(Blocks.TUFF) || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY)
                || state.is(Blocks.SNOW_BLOCK) || state.is(BlockTags.DIRT);
    }

    private static RouteCandidate invalidCandidate(String message) {
        return new RouteCandidate(false, List.of(), List.of(), List.of(), Integer.MAX_VALUE, message);
    }''')

replace_once(road,
'''    private static List<Placement> createPlan(RoadConstructionState road) {
        List<BlockPos> centers = road.centers();
        Map<BlockPos, FootprintSpec> footprints = footprintMap(centers, road.profile());
        List<Placement> placements = new ArrayList<>(footprints.size());
        for (Map.Entry<BlockPos, FootprintSpec> entry : footprints.entrySet()) {
            FootprintSpec spec = entry.getValue();
            BlockState state;
            if (spec.bridge()) state = Blocks.STONE_BRICKS.defaultBlockState();
            else if (spec.stairFacing() != null) {
                state = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, spec.stairFacing());
            } else state = spec.centerline() ? Blocks.GRAVEL.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
            placements.add(new Placement(entry.getKey(), state, spec.bridge()));
        }
        return placements;
    }''',
'''    private static List<Placement> createPlan(RoadConstructionState road) {
        List<BlockPos> centers = road.centers();
        Map<BlockPos, FootprintSpec> footprints = footprintMap(centers, road.profile());
        List<Placement> placements = new ArrayList<>(footprints.size() + road.bridgeSupportCount());
        for (Map.Entry<BlockPos, FootprintSpec> entry : footprints.entrySet()) {
            FootprintSpec spec = entry.getValue();
            BlockState state;
            if (spec.bridge()) state = Blocks.STONE_BRICKS.defaultBlockState();
            else if (spec.stairFacing() != null) {
                state = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, spec.stairFacing());
            } else state = spec.centerline() ? Blocks.GRAVEL.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
            placements.add(new Placement(entry.getKey(), state, spec.bridge(), false));
        }
        for (BlockPos support : road.bridgeSupportPositions()) {
            placements.add(new Placement(support, Blocks.STONE_BRICKS.defaultBlockState(), true, true));
        }
        return placements;
    }''')
replace_once(road,
'''            for (int side = -1; side <= 1; side++) {
                BlockPos pos = new BlockPos(center.getX() - direction[1] * side, center.getY(), center.getZ() + direction[0] * side);
                FootprintSpec incoming = new FootprintSpec(side == 0, bridge, stairFacing);''',
'''            for (int side : new int[] {0, -1, 1}) {
                BlockPos pos = new BlockPos(center.getX() - direction[1] * side, center.getY(), center.getZ() + direction[0] * side);
                FootprintSpec incoming = new FootprintSpec(side == 0, bridge, stairFacing);''')

# ---------------------------------------------------------------------------
# Version / companion lock.
# ---------------------------------------------------------------------------
props = P / 'gradle.properties'
replace_once(props, 'mod_version=0.1.0-alpha.51', 'mod_version=0.1.0-alpha.52')
replace_once(props,
             'and retaining-heavy 17x17 terraces with exact cobblestone hauling.',
             'retaining-heavy 17x17 terraces with exact cobblestone hauling, and bounded long-bridge/ravine crossings with persisted physical stone piers.')

lock = P / 'COMPANION_LOCK.json'
replace_once(lock, '"frontier_settlement": "0.1.0-alpha.51"', '"frontier_settlement": "0.1.0-alpha.52"')
replace_once(lock,
             '    "Alpha.51 expands the same civil tool to 17x17 / plus-or-minus-7 and adds a bounded one-block outer retaining ring when fill-facing edges stand at least three blocks above natural exterior ground; exact COBBLESTONE ItemStacks are physically hauled and consumed only after successful wall placement, with a seven-block retaining-height ceiling and no ravine bridge/tunnel claim.",\n    "Xaero 26.4.2 remains candidate-only for Frontier marker synchronization: the historical public WaypointsManager API is absent, so Alpha.51 keeps only HUD collision avoidance rather than internal/mixin waypoint injection."',
             '    "Alpha.51 expands the same civil tool to 17x17 / plus-or-minus-7 and adds a bounded one-block outer retaining ring when fill-facing edges stand at least three blocks above natural exterior ground; exact COBBLESTONE ItemStacks are physically hauled and consumed only after successful wall placement, with a seven-block retaining-height ceiling and no ravine bridge/tunnel claim.",\n    "Alpha.52 extends the existing road authority with bounded straight long-bridge and dry-ravine crossings up to 24 centerline cells; exact support cells are persisted, stone piers must reach natural support within 12 blocks, and the same road builder physically hauls stone with no second transport or construction authority.",\n    "Xaero 26.4.2 remains candidate-only for Frontier marker synchronization: the historical public WaypointsManager API is absent, so Alpha.52 keeps only HUD collision avoidance rather than internal/mixin waypoint injection."')

# ---------------------------------------------------------------------------
# Documentation: complete long-bridge/ravine first pass only; tunnel remains.
# ---------------------------------------------------------------------------
readme = P / 'README.md'
replace_once(readme, '## Current version: 0.1.0-alpha.51', '## Current version: 0.1.0-alpha.52')
replace_once(readme, 'No new Alpha.51 key was added.', 'No new Alpha.52 key was added.')
replace_once(readme, 'Alpha.40–51 deepen existing systems', 'Alpha.40–52 deepen existing systems')
replace_once(readme,
             '- Alpha.35 adds one-block road stairs and bounded short-water bridges using real stone.',
             '- Alpha.35 adds one-block road stairs and bounded short-water bridges using real stone. Alpha.52 extends that same road authority to bounded 24-cell long-water/dry-ravine bridge runs with persisted physical stone piers.')
alpha52_readme = '''## Alpha.52 — bounded long bridges and ravine crossings\n\nAlpha.52 advances the first item left after Alpha.51 without inventing a new road system. The existing road endpoint flow, shared builder and real settlement stone authority are reused.\n\n- ordinary Alpha.35 short-water bridges up to 6 centerline cells remain supported;\n- a straight water or abrupt dry-ravine bridge run may now span at most **24 centerline cells**;\n- dry ravine detection requires a bounded depression at least **4 blocks** below compatible shoulders;\n- bridge approaches still require bank/shoulder height difference of at most 1 block;\n- bridge runs needing structural support receive two stone pier columns at bounded stations rather than becoming floating decks;\n- every planned pier cell is persisted in `RoadConstructionState.bridge_supports`, so save/reload keeps the exact same support plan;\n- each pier must reach natural support ground within **12 blocks** below the deck. Unloaded cells, containers, lava/other fluids, player structures and non-natural support reject the route;\n- pier-required long bridges unlock at the existing village stage; no new key, building family, currency or dashboard is added;\n- the same road builder walks to real settlement storage, extracts real stone ItemStacks, and physically builds deck/support placements;\n- paving now treats world placement and ItemStack consumption atomically: successful `setBlock` precedes carried-stone shrink and state advance, with rollback if consumption unexpectedly fails;\n- final road repair no longer places missing road/bridge blocks for free; each repair fetches and consumes a real stone ItemStack;\n- no force-load, teleport logistics, second builder, second road authority or second outpost transport authority is introduced.\n\nAlpha.52 completes the **first bounded long-bridge/ravine-crossing slice**. Tunnels, more complex curved/deeper monumental crossings and final real-play acceptance remain unfinished.\n\n'''
replace_once(readme, '## Alpha.51 — 17×17 retaining-heavy terraces\n', alpha52_readme + '## Alpha.51 — 17×17 retaining-heavy terraces\n')
replace_once(readme,
             'Alpha.51 completes the first **retaining-heavy large-terrace** slice. Ravine-scale crossings, long bridges, tunnels and monumental civil engineering remain unfinished; unrestricted WorldEdit and mountain deletion remain outside scope.',
             'Alpha.51 completed the first **retaining-heavy large-terrace** slice. Alpha.52 now adds a bounded first long-bridge/ravine-crossing pass; tunnels and more complex monumental civil engineering remain unfinished. Unrestricted WorldEdit and mountain deletion remain outside scope.')
replace_once(readme, 'Frontier must still boot without optional companions. Alpha.51 reads already-loaded ordinary terrain and physical storage only; it adds no Terralith/worldgen Java dependency.',
             'Frontier must still boot without optional companions. Alpha.52 reads already-loaded ordinary terrain and physical storage only; it adds no Terralith/worldgen Java dependency.')
replace_once(readme, 'Canonical Alpha.51 CI order:', 'Canonical Alpha.52 CI order:')
replace_once(readme, 'cumulative Alpha.23–51 source/runtime audit', 'cumulative Alpha.23–52 source/runtime audit')
replace_once(readme, 'Alpha.51 canonical README/plan/gap docs audit', 'Alpha.52 canonical README/plan/gap docs audit')

canonical = P / 'CANONICAL_PLAN.md'
replace_once(canonical, 'Current canonical implementation: **0.1.0-alpha.51**.', 'Current canonical implementation: **0.1.0-alpha.52**.')
replace_once(canonical, 'Alpha.40–51 deepen systems', 'Alpha.40–52 deepen systems')
replace_once(canonical,
             '- bounded short-water stone bridge/deck max6 centerline blocks;',
             '- bounded short-water stone bridge/deck max6 centerline blocks;\n- Alpha.52 bounded straight long-water/dry-ravine bridge runs max24 with persisted physical stone pier cells;')
alpha52_plan = '''\n### Alpha.52 bounded long-bridge / ravine crossing\n\nAlpha.52 remains inside `SettlementRoadService` and the existing road construction state. It does not create a civil-work duplicate or another logistics controller.\n\n- Alpha.35 short-water bridges remain max6 centerline cells without new pier state;\n- straight water crossings and abrupt dry ravines may use bridge profile for at most24 centerline cells;\n- dry ravines require at least4 blocks of bounded depression and nearly level shoulders;\n- runs needing structural support persist exact pier cells in optional `bridge_supports`, default empty for old saves;\n- pier stations use two edge columns and each column must reach natural support within12 blocks;\n- loaded block entities, non-water fluid, player/non-natural obstruction or excessive depth reject the route;\n- pier-required bridges are village-stage public works;\n- deck and pier stone remain real ItemStacks hauled by the same road builder from actual settlement storage;\n- placement is atomic with resource authority: successful world `setBlock` happens before carried-stone shrink/state advance, and a failed consume rolls the placed block back;\n- final validation/repair also requires physical stone instead of free repair placement;\n- completed roads still become the same `RoadSegment`, so Alpha.27 remains the **single authority for outpost transport** and there is still only one authority for long-distance outpost transport;\n- no force-load, teleport, virtual stone, second builder or second route authority.\n\nThis is the first long-bridge/ravine slice only. **Tunnels and more complex/deeper monumental crossings remain unfinished.**\n'''
replace_once(canonical, '## 10. Exploration, crafting and settlement feedback\n', alpha52_plan + '\n## 10. Exploration, crafting and settlement feedback\n')
replace_once(canonical, 'Alpha.51 civil work reads already-loaded block state/heightmap and loaded physical storage only. It adds no Terralith/worldgen hard dependency.',
             'Alpha.52 road/civil work reads already-loaded block state/heightmap and loaded physical storage only. It adds no Terralith/worldgen hard dependency.')
replace_once(canonical, '## 14. Current playable slice after Alpha.51', '## 14. Current playable slice after Alpha.52')
replace_once(canonical,
             '- physical roads/stairs/short bridges;',
             '- physical roads/stairs/short bridges plus Alpha.52 bounded 24-cell long-water/dry-ravine bridges with persisted physical stone piers;')
replace_once(canonical, 'This is not original v0.2 completion.\n\n## 15. Unfinished original-scope priorities after Alpha.51',
             'This is not original v0.2 completion.\n\n## 15. Unfinished original-scope priorities after Alpha.52')
replace_once(canonical,
             '1. **ravine-scale / long bridge / tunnel civil-engineering pass** — extend beyond Alpha.51 retaining terraces without becoming WorldEdit, force-loading or minting resources;',
             '1. **tunnel / deeper monumental crossing civil-engineering pass** — extend beyond Alpha.52 bounded long bridges without becoming WorldEdit, force-loading or minting resources;')
replace_once(canonical,
             '10. Alpha.51 civil-work pathing/save-reload/retaining-cobble depletion-resupply/return-cargo/terrain-safety acceptance;',
             '10. Alpha.51 civil-work pathing/save-reload/retaining-cobble depletion-resupply/return-cargo/terrain-safety acceptance;\n11. Alpha.52 long-bridge pier planning/save-reload/stone depletion/physical repair/pathing acceptance;')
replace_once(canonical, '11. full companion lock fresh-world client/server runtime;', '12. full companion lock fresh-world client/server runtime;')
replace_once(canonical, '12. true Xaero markers only if a stable supported API appears;', '13. true Xaero markers only if a stable supported API appears;')
replace_once(canonical, '13. moving boat/waterborne merchant only if presentation value justifies it and it never becomes a second logistics authority.',
             '14. moving boat/waterborne merchant only if presentation value justifies it and it never becomes a second logistics authority.')
replace_once(canonical,
             '- full companion-stack fresh world.',
             '- Alpha.52 long bridge: 7–24-cell bound, dry ravine depth trigger, straight support run, pier natural-support depth<=12, save/reload support stability, real-stone depletion/resupply and no-free-repair behavior;\n- full companion-stack fresh world.')

gap = P / 'COMPLETION_GAP_AUDIT.md'
replace_once(gap, '현재 구현 기준: `0.1.0-alpha.51`', '현재 구현 기준: `0.1.0-alpha.52`')
replace_once(gap,
             'Alpha.51에서 retaining-heavy 대형 테라스 1차가 추가되어도 ravine-scale/장교량/터널 토목, 실물 군사 armory, 일부 탐험/전초 breadth, 장시간 multiplayer 및 full companion runtime이 남아 있는 동안 완성이라고 부르지 않는다.',
             'Alpha.52에서 bounded 장교량/협곡 횡단 1차가 추가되어도 터널/더 깊은 기념비급 토목, 실물 군사 armory, 일부 탐험/전초 breadth, 장시간 multiplayer 및 full companion runtime이 남아 있는 동안 완성이라고 부르지 않는다.')
replace_once(gap,
             '| 대형 협곡 다리/터널/기념비급 토목 | **미구현** | 작은 road bridge + 13×13 토목까지만 |',
             '| 대형 협곡/장교량 | **완료/부분** | Alpha.52 max24 straight crossing + persisted physical stone piers, real-play breadth 남음 |\n| 터널/더 깊은 기념비급 토목 | **미구현/부분** | Alpha.52 범위 밖, 다음 civil-engineering slice |')
replace_once(gap,
             '| 대형 협곡/장교량/터널 | 미구현 | larger civil engineering priority |',
             '| 대형 협곡/장교량 | 완료/부분 | Alpha.52 max24 + physical persisted piers |\n| 터널/더 깊은 대형 횡단 | 미구현/부분 | larger civil engineering next priority |')
replace_once(gap,
             '1. **ravine-scale / long bridge / tunnel civil-engineering pass** — Alpha.51 terrace보다 큰 crossing breadth를 실물 자원·player protection 안에서 구현;',
             '1. **tunnel / deeper monumental crossing civil-engineering pass** — Alpha.52 long bridge보다 큰/복잡한 crossing breadth를 실물 자원·player protection 안에서 구현;')
alpha52_gap = '''\n### Alpha.52 long-bridge / ravine crossing 감사\n\n- 기존 road endpoint/preview/approval 흐름과 같은 건설 주민 재사용, 새 key/building/currency 없음;\n- Alpha.35 short bridge max6 유지, Alpha.52 straight bridge run max24;\n- dry ravine은 shoulder 대비 최소4블록 깊이의 bounded depression만 자동 횡단;\n- pier-required bridge는 village 단계부터;\n- exact pier block positions를 optional `bridge_supports`에 저장, old saves default empty;\n- 장교량 교각은 양쪽 edge column으로 계획되고 자연 지반을 최대12블록 안에서 찾아야 함;\n- unloaded / block entity / non-water fluid / non-natural-player obstruction / too-deep support 거부;\n- same shared road builder + actual settlement stone ItemStacks만 사용;\n- world setBlock 성공 → carried stone consume → road state advance 순서, consume 실패 시 placed block rollback;\n- final validation missing block도 physical stone1개를 가져와 성공 배치 후 소비하며 free repair 없음;\n- completed road는 기존 RoadSegment/Alpha.27 transport authority로 귀결, second logistics authority 없음;\n- force-load/teleport/virtual stone 없음.\n\n따라서 **bounded long bridge/ravine crossing은 완료/부분**으로 전진했다. 터널과 더 복잡하고 깊은 기념비급 횡단은 여전히 미구현/부분이다.\n'''
replace_once(gap, '## 4. 주민 / 생산 / 방어\n', alpha52_gap + '\n## 4. 주민 / 생산 / 방어\n')
replace_once(gap, '## 10. Alpha.51 추가 실플레이 acceptance', '## 10. Alpha.51/52 추가 실플레이 acceptance')
replace_once(gap,
             '- 두 플레이어가 같은 civil project/progress/context를 봄.',
             '- 두 플레이어가 같은 civil project/progress/context를 봄.\n- Alpha.52 short6/long24 경계, dry-ravine 최소4 깊이, straight-only pier rule 정확성;\n- pier support depth12 허용/13 거부, water 허용/non-water fluid·container·player block 거부;\n- `bridge_supports` save/reload가 같은 exact support cells를 유지;\n- deck/support 모두 real stone depletion에서 pause/resupply resume;\n- road placement 성공 전에는 stone consume/state advance가 없고, failed/rollback 경로에서 free block이 남지 않음;\n- final missing road/bridge repair도 actual stone을 소비해 free repair가 없음.')
replace_once(gap,
             '- ravine-scale / long bridge / tunnel larger civil engineering breadth;',
             '- tunnel / deeper or more complex monumental crossing civil engineering breadth;')

# ---------------------------------------------------------------------------
# Alpha.52 audits.
# ---------------------------------------------------------------------------
write(P / 'tools/test_alpha52_source.py', '''#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
ALPHA51 = ROOT / 'tools/test_alpha51_source.py'


def text(path): return path.read_text(encoding='utf-8')
def must(source, tokens, label):
    for token in tokens:
        if token not in source: raise SystemExit(f'{label} missing: {token}')
def forbid(source, tokens, label):
    for token in tokens:
        if token in source: raise SystemExit(f'{label}: {token}')

if not ALPHA51.exists(): raise SystemExit('historical Alpha.51 source audit must remain present')
alpha51 = text(ALPHA51).replace("print('Frontier Settlement alpha.23-51 cumulative source audit: PASS')", 'pass')
alpha51 = alpha51.replace('0.1.0-alpha.51', '0.1.0-alpha.52')
namespace = {'__file__': str(ALPHA51), '__name__': '__main__'}
exec(compile(alpha51, str(ALPHA51), 'exec'), namespace, namespace)

road_state = text(JAVA / 'settlement/RoadConstructionState.java')
road = text(JAVA / 'settlement/SettlementRoadService.java')
data = text(JAVA / 'settlement/SettlementData.java')
props = text(ROOT / 'gradle.properties')
lock = text(ROOT / 'COMPANION_LOCK.json')

must(road_state, ('optionalFieldOf("bridge_supports", List.of())', 'bridgeSupportPositions()',
                  'bridgeSupportCount()', 'fromPath(List<BlockPos> centers, List<Integer> profile,',
                  'step + 1, path, profile, bridgeSupports', 'encoded, path, profile, bridgeSupports'),
     'alpha.52 persisted bridge supports')
must(data, ('beginRoadConstruction(List<BlockPos> centers, List<Integer> profile, List<BlockPos> bridgeSupports)',
            'RoadConstructionState.fromPath(centers, profile, bridgeSupports)'), 'alpha.52 road state authority')
must(road, ('MAX_SHORT_BRIDGE_SPAN = 6', 'MAX_LONG_BRIDGE_SPAN = 24', 'MIN_RAVINE_DEPTH = 4',
            'MAX_LONG_BRIDGE_PIER_DEPTH = 12', 'LONG_BRIDGE_PIER_INTERVAL = 6',
            'planBridgeSupports(', 'planPierColumn(', 'isNaturalSupportGround(',
            'chosen.supports()', 'road.bridgeSupportCount()', 'bridgeSupportPositions()',
            '교각이 필요한 장교량은 현재 직선 구간에서만',
            'level.setBlock(target, placement.state(), NORMAL_BLOCK_UPDATE)',
            'consumeCarriedStone(builder, stoneDelta)', 'data.advanceRoadConstruction()',
            'if (changed) level.setBlock(target, current, DIRECT_BLOCK_UPDATE)',
            'ensurePavingMaterial(server, data, builder, 1L, 1L)',
            'consumeCarriedStone(builder, 1L)'), 'alpha.52 long bridge physical authority')
place = road.find('if (!level.setBlock(target, placement.state(), NORMAL_BLOCK_UPDATE)) return false;')
consume = road.find('if (!consumeCarriedStone(builder, stoneDelta))', place)
advance = road.find('data.advanceRoadConstruction();', consume)
if min(place, consume, advance) < 0 or not (place < consume < advance):
    raise SystemExit('alpha.52 road paving must place successfully before stone consume/state advance')
finish = road.find('private static boolean finishIfValid')
repair_material = road.find('ensurePavingMaterial(server, data, builder, 1L, 1L)', finish)
repair_place = road.find('level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)', repair_material)
repair_consume = road.find('consumeCarriedStone(builder, 1L)', repair_place)
if min(finish, repair_material, repair_place, repair_consume) < 0 or not (finish < repair_material < repair_place < repair_consume):
    raise SystemExit('alpha.52 final repair must physically fetch/place/consume stone; no free repair')
forbid(road, ('forceChunk', 'setChunkForced', 'teleportTo(', 'destroyBlock(', 'dropResources('),
       'alpha.52 road safety')
must(props, ('mod_version=0.1.0-alpha.52', 'bounded long-bridge/ravine crossings with persisted physical stone piers'),
     'alpha.52 build properties')
must(lock, ('"frontier_settlement": "0.1.0-alpha.52"', 'Alpha.52 extends the existing road authority',
            'up to 24 centerline cells', 'support within 12 blocks', 'same road builder physically hauls stone',
            '"status": "candidate_runtime_lock"'), 'alpha.52 companion lock')

print('Frontier Settlement alpha.23-52 cumulative source audit: PASS')
''')

write(P / 'tools/test_alpha52_docs.py', '''#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def text(name): return (ROOT / name).read_text(encoding='utf-8')
def must(source, tokens, label):
    for token in tokens:
        if token not in source: raise SystemExit(f'{label} missing: {token}')
def forbid(source, tokens, label):
    for token in tokens:
        if token in source: raise SystemExit(f'{label} stale/invalid: {token}')

original = text('ORIGINAL_DESIGN_v0.2.md')
readme = text('README.md')
canonical = text('CANONICAL_PLAN.md')
gap = text('COMPLETION_GAP_AUDIT.md')

must(original, ('도로는 시작점·끝점·필요 시 중간점만 지정한다.',
                '경로는 급경사와 건물을 피하고, 작은 계단·교량을 자동으로 포함한다.',
                '평탄화를 위해 산 하나를 통째로 삭제하는 식의 과도한 월드 수정은 금지한다.'),
     'original design scope')
must(readme, ('## Current version: 0.1.0-alpha.52',
              '## Alpha.52 — bounded long bridges and ravine crossings', '**24 centerline cells**',
              '**4 blocks**', '**12 blocks**', '`RoadConstructionState.bridge_supports`',
              'successful `setBlock` precedes carried-stone shrink and state advance',
              'final road repair no longer places missing road/bridge blocks for free',
              'Tunnels, more complex curved/deeper monumental crossings',
              'single authority for outpost transport', 'Transport workers belong to a specific outpost',
              'pause at unloaded route boundaries'), 'alpha.52 README')
forbid(readme, ('## Current version: 0.1.0-alpha.51', 'Canonical Alpha.51 CI order:'), 'alpha.52 README stale')
must(canonical, ('Current canonical implementation: **0.1.0-alpha.52**',
                 '### Alpha.52 bounded long-bridge / ravine crossing', 'at most24 centerline cells',
                 'optional `bridge_supports`', 'within12 blocks', 'successful world `setBlock` happens before carried-stone shrink/state advance',
                 'final validation/repair also requires physical stone',
                 'single authority for outpost transport', 'there is still only one authority for long-distance outpost transport',
                 '## 14. Current playable slice after Alpha.52', '## 15. Unfinished original-scope priorities after Alpha.52',
                 '**tunnel / deeper monumental crossing civil-engineering pass**'), 'alpha.52 canonical')
must(gap, ('현재 구현 기준: `0.1.0-alpha.52`', '### Alpha.52 long-bridge / ravine crossing 감사',
           '| 대형 협곡/장교량 | **완료/부분** | Alpha.52 max24',
           '| 터널/더 깊은 기념비급 토목 | **미구현/부분** |',
           'old saves default empty', '자연 지반을 최대12블록',
           'world setBlock 성공 → carried stone consume → road state advance',
           'free repair 없음', '## 11. 완료 판정 금지선'), 'alpha.52 gap')
forbid(gap, ('현재 구현 기준: `0.1.0-alpha.51`',
             '- ravine-scale / long bridge / tunnel larger civil engineering breadth;'), 'alpha.52 gap stale')

print('Frontier Settlement alpha.52 canonical docs audit: PASS')
''')

print('Applied Frontier Settlement 0.1.0-alpha.52 long-bridge/ravine crossing slice.')
