package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SettlementRoadService {
    public static final int LEGACY_ROAD_LENGTH = 16;
    public static final int ROAD_WIDTH = 3;
    public static final int MIN_ROUTE_LENGTH = 4;
    public static final int MAX_ROUTE_LENGTH = 96;
    private static final int MAX_STEP_HEIGHT = 1;
    private static final int MAX_CROSS_SLOPE = 1;
    private static final int MAX_FILL_DEPTH = 2;
    private static final int DIRECT_BLOCK_UPDATE = 2;
    private static final int NORMAL_BLOCK_UPDATE = 3;
    private static final double BUILDER_WORK_RANGE_SQR = 18.0D;
    private static final double STORAGE_INTERACTION_RANGE_SQR = 9.0D;
    private static final int HAUL_BATCH_SIZE = 16;
    private static final double ROAD_BUILDER_SEARCH_MARGIN = 96.0D;
    private static final long PLAYER_ENDPOINT_RANGE_SQR = 16L * 16L;

    private SettlementRoadService() {}

    public record StartResult(boolean started, String message) {}
    public record RouteCheck(boolean valid, List<BlockPos> centers, int stoneCost, String message) {}
    private record RouteCandidate(boolean valid, List<BlockPos> centers, int score, String message) {}
    private record Placement(BlockPos pos, BlockState state) {}

    public static StartResult start(ServerPlayer player) {
        int[] direction = horizontalDirection(player.getYRot());
        BlockPos start = player.blockPosition();
        BlockPos end = start.offset(direction[0] * (LEGACY_ROAD_LENGTH - 1), 0,
                direction[1] * (LEGACY_ROAD_LENGTH - 1));
        return startAt(player, start, end);
    }

    public static RouteCheck checkRoute(ServerPlayer player, BlockPos selectedStart, BlockPos selectedEnd) {
        MinecraftServer server = player.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return invalid("먼저 공동 마을을 시작해야 합니다.");
        if (player.level() != server.overworld()) return invalid("도로는 현재 오버월드 공동 마을에서만 건설할 수 있습니다.");
        if (data.construction().active() || data.roadConstruction().active() || data.outpostConstruction().active()) {
            return invalid("현재 공사가 끝난 뒤 새 도로를 계획해 주세요.");
        }
        if (data.houseCount() < 1 || data.lumberCampCount() < 1) {
            return invalid("첫 도로는 주택 1채와 벌목소 1곳을 완성한 뒤 열립니다.");
        }

        BlockPos startXZ = new BlockPos(selectedStart.getX(), 0, selectedStart.getZ());
        BlockPos endXZ = new BlockPos(selectedEnd.getX(), 0, selectedEnd.getZ());
        int manhattan = Math.abs(endXZ.getX() - startXZ.getX()) + Math.abs(endXZ.getZ() - startXZ.getZ()) + 1;
        if (manhattan < MIN_ROUTE_LENGTH) return invalid("도로 끝점을 시작점에서 최소 3블록 이상 떨어뜨려 주세요.");
        if (manhattan > MAX_ROUTE_LENGTH) return invalid("한 번에 계획할 수 있는 도로는 최대 " + MAX_ROUTE_LENGTH + "블록입니다.");
        if (!nearEitherEndpoint(player.blockPosition(), startXZ, endXZ)) {
            return invalid("도로 시작점이나 끝점 가까이에서 계획해 주세요.");
        }
        if (!connectedToNetwork(data, startXZ)) {
            return invalid("시작점은 마을 중심, 기존 도로 끝, 또는 전초기지와 이어지는 곳이어야 합니다.");
        }

        ServerLevel level = server.overworld();
        RouteCandidate xThenZ = assessCandidate(level, data, startXZ, endXZ, true);
        RouteCandidate zThenX = assessCandidate(level, data, startXZ, endXZ, false);
        RouteCandidate chosen = choose(xThenZ, zThenX);
        if (!chosen.valid()) {
            String reason = !xThenZ.message().isBlank() ? xThenZ.message() : zThenX.message();
            return invalid(reason.isBlank() ? "두 자동 경로 모두 안전한 3칸 폭 도로를 만들 수 없습니다." : reason);
        }

        int cost = stoneCost(chosen.centers().size());
        SettlementService.refreshResources(server, data);
        String resource = data.resources().stone() < cost
                ? " | 석재 부족: 필요 " + cost + " / 현재 " + data.resources().stone()
                : " | 석재 " + cost;
        return new RouteCheck(true, chosen.centers(), cost,
                "경로 " + chosen.centers().size() + "블록" + resource);
    }

    public static StartResult startAt(ServerPlayer player, BlockPos selectedStart, BlockPos selectedEnd) {
        MinecraftServer server = player.level().getServer();
        SettlementData data = SettlementData.get(server);
        RouteCheck check = checkRoute(player, selectedStart, selectedEnd);
        if (!check.valid()) return new StartResult(false, check.message());

        ServerLevel level = server.overworld();
        if (!SettlementStorageService.storageAvailable(level, data)) {
            return new StartResult(false, "공동 창고가 모두 로드된 상태에서 도로를 착공해 주세요. 자원은 차감되지 않았습니다.");
        }
        SettlementService.refreshResources(server, data);
        if (data.resources().stone() < check.stoneCost()) {
            return new StartResult(false, "도로 필요 석재 " + check.stoneCost() + " | 현재 석재 " + data.resources().stone());
        }

        data.beginRoadConstruction(check.centers());
        SettlementConstructionService.ensureBuilder(level, data.centerPos());
        SettlementService.broadcast(server, data);
        return new StartResult(true, "개척 도로 착공: " + check.centers().size()
                + "블록 경로, 3칸 폭. 건설 주민이 지반을 정리한 뒤 공동 창고의 실제 석재 "
                + check.stoneCost() + "개를 운반하며 포설합니다.");
    }

    public static boolean tick(MinecraftServer server, SettlementData data) {
        RoadConstructionState road = data.roadConstruction();
        if (!road.active()) return false;

        List<Placement> plan = createPlan(road);
        if (plan.isEmpty()) {
            data.clearRoadConstruction();
            return true;
        }

        ServerLevel level = server.overworld();
        Villager builder = findRoadBuilder(level, data.centerPos(), road, plan);
        if (builder == null) return false;
        if (builder.isNoAi()) builder.setNoAi(false);
        builder.setInvulnerable(true);

        if (road.grading()) return tickGrading(server, data, road, plan, builder);
        if (road.step() >= plan.size()) return finishIfValid(server, data, road, plan, builder);
        return tickPaving(server, data, road, plan, builder);
    }

    private static Villager findRoadBuilder(ServerLevel level, BlockPos settlementCenter,
                                            RoadConstructionState road, List<Placement> plan) {
        int rawIndex = road.grading() ? road.gradeStep() : road.step();
        int index = Math.max(0, Math.min(plan.size() - 1, rawIndex));
        BlockPos hint = plan.get(index).pos();
        double minX = Math.min(settlementCenter.getX(), hint.getX()) - ROAD_BUILDER_SEARCH_MARGIN;
        double minY = Math.min(settlementCenter.getY(), hint.getY()) - 64.0D;
        double minZ = Math.min(settlementCenter.getZ(), hint.getZ()) - ROAD_BUILDER_SEARCH_MARGIN;
        double maxX = Math.max(settlementCenter.getX(), hint.getX()) + ROAD_BUILDER_SEARCH_MARGIN + 1.0D;
        double maxY = Math.max(settlementCenter.getY(), hint.getY()) + 65.0D;
        double maxZ = Math.max(settlementCenter.getZ(), hint.getZ()) + ROAD_BUILDER_SEARCH_MARGIN + 1.0D;
        AABB corridor = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        List<Villager> tagged = level.getEntitiesOfClass(Villager.class, corridor,
                villager -> villager.entityTags().contains(SettlementConstructionService.BUILDER_TAG));
        if (!tagged.isEmpty()) return tagged.getFirst();
        return SettlementConstructionService.ensureBuilder(level, settlementCenter);
    }

    private static boolean tickGrading(MinecraftServer server, SettlementData data, RoadConstructionState road,
                                       List<Placement> plan, Villager builder) {
        int gradeStep = road.gradeStep();
        if (gradeStep >= plan.size()) {
            data.replaceRoadConstructionStep(0);
            SettlementService.broadcast(server, data);
            return false;
        }

        ServerLevel level = server.overworld();
        Placement placement = plan.get(gradeStep);
        if (!moveBuilderToCurrentSurface(level, builder, placement.pos())) return false;
        if (!canGradePlacement(level, placement.pos())) {
            builder.getNavigation().stop();
            return false;
        }

        applyGradePlacement(level, placement.pos());
        builder.swing(InteractionHand.MAIN_HAND);
        data.advanceRoadConstruction();
        RoadConstructionState next = data.roadConstruction();
        if (next.grading() && next.gradeStep() >= plan.size()) {
            data.replaceRoadConstructionStep(0);
            SettlementService.broadcast(server, data);
        }
        return false;
    }

    private static boolean tickPaving(MinecraftServer server, SettlementData data, RoadConstructionState road,
                                      List<Placement> plan, Villager builder) {
        int step = road.step();
        int totalCost = stoneCost(road.centers().size());
        long spentBefore = costAtStep(totalCost, step, plan.size());
        long spentAfter = costAtStep(totalCost, step + 1, plan.size());
        long stoneDelta = spentAfter - spentBefore;
        long remainingCost = Math.max(0L, totalCost - spentBefore);
        if (!ensurePavingMaterial(server, data, builder, stoneDelta, remainingCost)) return false;

        ServerLevel level = server.overworld();
        Placement placement = plan.get(step);
        if (!moveBuilderToCurrentSurface(level, builder, placement.pos())) return false;

        BlockPos target = placement.pos();
        BlockState current = level.getBlockState(target);
        if (!current.is(placement.state().getBlock()) && !current.isAir() && !isRoadGround(current)) {
            builder.getNavigation().stop();
            return false;
        }
        BlockState support = level.getBlockState(target.below());
        if (support.isAir() || support.canBeReplaced() || !support.getFluidState().isEmpty()) {
            builder.getNavigation().stop();
            return false;
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
    }

    private static boolean ensurePavingMaterial(MinecraftServer server, SettlementData data, Villager builder,
                                                long requiredNow, long remainingCost) {
        ItemStack carried = builder.getMainHandItem();
        if (!carried.isEmpty() && !SettlementInventory.isStone(carried)) {
            return returnCarriedToStorage(server, data, builder);
        }
        if (requiredNow <= 0L) return true;
        if (!carried.isEmpty() && carried.getCount() >= requiredNow) return true;
        if (!carried.isEmpty()) return returnCarriedToStorage(server, data, builder);

        ServerLevel level = server.overworld();
        BlockPos source = SettlementStorageService.findExtractionTarget(level, data, SettlementInventory::isStone);
        if (source == null) return false;
        if (builder.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)
                > STORAGE_INTERACTION_RANGE_SQR) {
            builder.getNavigation().moveTo(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D, 0.9D);
            return false;
        }

        int amount = (int) Math.min((long) HAUL_BATCH_SIZE, Math.max(requiredNow, remainingCost));
        ItemStack extracted = SettlementStorageService.extract(level, source, SettlementInventory::isStone, amount);
        if (extracted.isEmpty()) return false;
        builder.setItemSlot(EquipmentSlot.MAINHAND, extracted);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        return false;
    }

    private static boolean consumeCarriedStone(Villager builder, long amount) {
        if (amount <= 0L) return true;
        ItemStack carried = builder.getMainHandItem();
        if (carried.isEmpty() || !SettlementInventory.isStone(carried) || carried.getCount() < amount) return false;
        carried.shrink((int) amount);
        if (carried.isEmpty()) builder.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        return true;
    }

    private static boolean returnCarriedToStorage(MinecraftServer server, SettlementData data, Villager builder) {
        ItemStack carried = builder.getMainHandItem();
        if (carried.isEmpty()) return true;
        ServerLevel level = server.overworld();
        BlockPos target = SettlementStorageService.findDepositTarget(level, data, carried);
        if (builder.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                > STORAGE_INTERACTION_RANGE_SQR) {
            builder.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 0.9D);
            return false;
        }
        int before = carried.getCount();
        ItemStack remaining = SettlementStorageService.insert(level, data, carried);
        builder.setItemSlot(EquipmentSlot.MAINHAND, remaining);
        if (remaining.getCount() < before) {
            SettlementService.refreshResources(server, data);
            SettlementService.broadcast(server, data);
        }
        return remaining.isEmpty();
    }

    private static boolean moveBuilderToCurrentSurface(ServerLevel level, Villager builder, BlockPos target) {
        int workY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
        BlockPos work = new BlockPos(target.getX(), workY, target.getZ());
        double distance = builder.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D);
        if (distance <= BUILDER_WORK_RANGE_SQR) return true;
        builder.getNavigation().moveTo(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D, 0.85D);
        return false;
    }

    private static boolean canGradePlacement(ServerLevel level, BlockPos target) {
        BlockState current = level.getBlockState(target);
        if (level.getBlockEntity(target) != null || !current.getFluidState().isEmpty()) return false;
        if (!current.isAir() && !current.canBeReplaced() && !isRoadGround(current)) return false;

        for (int y = target.getY() + 1; y <= target.getY() + 2; y++) {
            BlockPos pos = new BlockPos(target.getX(), y, target.getZ());
            BlockState state = level.getBlockState(pos);
            if (level.getBlockEntity(pos) != null || !state.getFluidState().isEmpty()) return false;
            if (!isClearableForRoad(state)) return false;
        }
        return hasOrCanMakeSupport(level, target.below());
    }

    private static void applyGradePlacement(ServerLevel level, BlockPos target) {
        for (int y = target.getY() + 2; y >= target.getY() + 1; y--) {
            BlockPos pos = new BlockPos(target.getX(), y, target.getZ());
            if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE);
        }
        BlockPos cursor = target.below();
        for (int depth = 0; depth <= MAX_FILL_DEPTH; depth++) {
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir() && !state.canBeReplaced()) break;
            level.setBlock(cursor, Blocks.COARSE_DIRT.defaultBlockState(), DIRECT_BLOCK_UPDATE);
            cursor = cursor.below();
        }
        BlockState targetState = level.getBlockState(target);
        if (targetState.isAir() || targetState.canBeReplaced()) {
            level.setBlock(target, Blocks.COARSE_DIRT.defaultBlockState(), DIRECT_BLOCK_UPDATE);
        }
    }

    public static int totalSteps(RoadConstructionState road) {
        return road.active() ? createPlan(road).size() : 0;
    }

    public static int stoneCost(int centerlineLength) {
        return Math.max(6, (centerlineLength * 3 + 1) / 2);
    }

    public static String phaseLabel(RoadConstructionState road) {
        if (road == null || !road.active()) return "도로 공사";
        List<Placement> plan = createPlan(road);
        if (road.grading()) return "도로 지반 정리";
        if (road.step() < plan.size()) return "도로 석재 운반·포설";
        return "도로 마감 확인";
    }

    private static long costAtStep(long totalCost, int step, int totalSteps) {
        if (totalCost <= 0L || step <= 0 || totalSteps <= 0) return 0L;
        if (step >= totalSteps) return totalCost;
        return totalCost * step / totalSteps;
    }

    private static boolean finishIfValid(MinecraftServer server, SettlementData data,
                                         RoadConstructionState road, List<Placement> plan, Villager builder) {
        ServerLevel level = server.overworld();
        for (Placement placement : plan) {
            BlockState current = level.getBlockState(placement.pos());
            if (current.is(placement.state().getBlock())) continue;
            if (!current.isAir() && !isRoadGround(current)) {
                builder.getNavigation().stop();
                return false;
            }
            BlockState support = level.getBlockState(placement.pos().below());
            if (support.isAir() || support.canBeReplaced() || !support.getFluidState().isEmpty()) {
                builder.getNavigation().stop();
                return false;
            }
            if (!moveBuilderToCurrentSurface(level, builder, placement.pos())) return false;
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
    }

    public static void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (level != server.overworld()) return;
        SettlementData data = SettlementData.get(server);
        RoadConstructionState road = data.roadConstruction();
        if (!road.active()) return;
        BlockPos pos = event.getPos();
        BlockState current = level.getBlockState(pos);
        List<Placement> plan = createPlan(road);
        int gradedCount = road.grading()
                ? Math.max(0, Math.min(plan.size(), road.gradeStep()))
                : plan.size();
        for (int i = 0; i < plan.size(); i++) {
            Placement placement = plan.get(i);
            BlockPos surface = placement.pos();
            if (surface.equals(pos) && current.is(placement.state().getBlock())) {
                event.setCanceled(true);
                event.setNotifyClient(true);
                return;
            }
            if (i >= gradedCount) continue;
            if (surface.equals(pos) && current.is(Blocks.COARSE_DIRT)) {
                event.setCanceled(true);
                event.setNotifyClient(true);
                return;
            }
            if (surface.getX() == pos.getX() && surface.getZ() == pos.getZ()
                    && pos.getY() < surface.getY() && pos.getY() >= surface.getY() - MAX_FILL_DEPTH - 1
                    && current.is(Blocks.COARSE_DIRT)) {
                event.setCanceled(true);
                event.setNotifyClient(true);
                return;
            }
        }
    }

    private static RouteCandidate assessCandidate(ServerLevel level, SettlementData data,
                                                  BlockPos start, BlockPos end, boolean xFirst) {
        List<BlockPos> flat = manhattanPath(start, end, xFirst);
        if (flat.size() < MIN_ROUTE_LENGTH || flat.size() > MAX_ROUTE_LENGTH) {
            return new RouteCandidate(false, List.of(), Integer.MAX_VALUE, "도로 길이가 허용 범위를 벗어납니다.");
        }

        List<BlockPos> centers = new ArrayList<>(flat.size());
        int score = 0;
        int previousY = Integer.MIN_VALUE;
        for (int i = 0; i < flat.size(); i++) {
            BlockPos flatPos = flat.get(i);
            int roadY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, flatPos.getX(), flatPos.getZ()) - 1;
            if (previousY != Integer.MIN_VALUE && Math.abs(roadY - previousY) > MAX_STEP_HEIGHT) {
                return new RouteCandidate(false, centers, Integer.MAX_VALUE,
                        "자동 경로에 2블록 이상의 급경사가 있습니다. 끝점을 옮겨 주세요.");
            }
            score += previousY == Integer.MIN_VALUE ? 0 : Math.abs(roadY - previousY) * 3;
            centers.add(new BlockPos(flatPos.getX(), roadY, flatPos.getZ()));
            previousY = roadY;
        }

        for (int i = 0; i < centers.size(); i++) {
            BlockPos center = centers.get(i);
            int[] direction = directionAt(centers, i);
            for (int side = -1; side <= 1; side++) {
                int x = center.getX() - direction[1] * side;
                int z = center.getZ() + direction[0] * side;
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                if (Math.abs(surfaceY - center.getY()) > MAX_CROSS_SLOPE) {
                    return new RouteCandidate(false, centers, Integer.MAX_VALUE,
                            "3칸 폭을 만들기엔 옆 경사가 너무 큽니다.");
                }
                score += Math.abs(surfaceY - center.getY());

                BlockPos naturalSurface = new BlockPos(x, surfaceY, z);
                BlockState natural = level.getBlockState(naturalSurface);
                if (level.getBlockEntity(naturalSurface) != null || !natural.getFluidState().isEmpty() || !isRoadGround(natural)) {
                    return new RouteCandidate(false, centers, Integer.MAX_VALUE,
                            "경로에 물·컨테이너·도로로 정리할 수 없는 지면이 있습니다.");
                }

                BlockPos footprint = new BlockPos(x, center.getY(), z);
                if (overlapsBuildingOrOutpost(data, footprint)) {
                    return new RouteCandidate(false, centers, Integer.MAX_VALUE,
                            "경로가 기존 건물이나 전초기지와 겹칩니다.");
                }
                if (overlapsExistingRoad(data.roads(), footprint) && i > 1 && i < centers.size() - 2) {
                    return new RouteCandidate(false, centers, Integer.MAX_VALUE,
                            "경로 중간이 기존 도로와 겹칩니다. 시작·끝 접속만 허용됩니다.");
                }

                for (int y = center.getY(); y <= center.getY() + 2; y++) {
                    BlockPos check = new BlockPos(x, y, z);
                    if (level.getBlockEntity(check) != null) {
                        return new RouteCandidate(false, centers, Integer.MAX_VALUE, "경로 위에 보호해야 할 블록이 있습니다.");
                    }
                    BlockState state = level.getBlockState(check);
                    if (!state.getFluidState().isEmpty() || !isClearableForRoad(state)) {
                        return new RouteCandidate(false, centers, Integer.MAX_VALUE, "경로 위 공간을 안전하게 정리할 수 없습니다.");
                    }
                }
                if (!hasOrCanMakeSupport(level, footprint.below())) {
                    return new RouteCandidate(false, centers, Integer.MAX_VALUE, "도로 아래 지반이 너무 깊게 비어 있습니다.");
                }
            }
        }
        return new RouteCandidate(true, List.copyOf(centers), score, "");
    }

    private static RouteCandidate choose(RouteCandidate a, RouteCandidate b) {
        if (a.valid() && b.valid()) return a.score() <= b.score() ? a : b;
        if (a.valid()) return a;
        if (b.valid()) return b;
        return a;
    }

    private static List<BlockPos> manhattanPath(BlockPos start, BlockPos end, boolean xFirst) {
        List<BlockPos> out = new ArrayList<>();
        int x = start.getX();
        int z = start.getZ();
        out.add(new BlockPos(x, 0, z));
        if (xFirst) {
            while (x != end.getX()) { x += Integer.signum(end.getX() - x); out.add(new BlockPos(x, 0, z)); }
            while (z != end.getZ()) { z += Integer.signum(end.getZ() - z); out.add(new BlockPos(x, 0, z)); }
        } else {
            while (z != end.getZ()) { z += Integer.signum(end.getZ() - z); out.add(new BlockPos(x, 0, z)); }
            while (x != end.getX()) { x += Integer.signum(end.getX() - x); out.add(new BlockPos(x, 0, z)); }
        }
        return out;
    }

    private static boolean connectedToNetwork(SettlementData data, BlockPos start) {
        if (horizontalDistanceSqr(start, data.centerPos()) <= 24L * 24L) return true;
        for (RoadSegment road : data.roads()) if (road.containsXZ(start)) return true;
        for (OutpostRecord outpost : data.outposts()) {
            BlockPos center = new BlockPos(outpost.centerX(), 0, outpost.centerZ());
            if (horizontalDistanceSqr(start, center) <= 6L * 6L) return true;
        }
        return false;
    }

    private static boolean nearEitherEndpoint(BlockPos player, BlockPos start, BlockPos end) {
        return horizontalDistanceSqr(player, start) <= PLAYER_ENDPOINT_RANGE_SQR
                || horizontalDistanceSqr(player, end) <= PLAYER_ENDPOINT_RANGE_SQR;
    }

    private static long horizontalDistanceSqr(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dz = (long) a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    private static boolean overlapsBuildingOrOutpost(SettlementData data, BlockPos pos) {
        for (BuildingRecord building : data.buildings()) if (building.protectsXZ(pos, 1)) return true;
        for (OutpostRecord outpost : data.outposts()) if (outpost.protectsXZ(pos, 1)) return true;
        return false;
    }

    private static boolean overlapsExistingRoad(List<RoadSegment> roads, BlockPos pos) {
        for (RoadSegment segment : roads) if (segment.containsXZ(pos)) return true;
        return false;
    }

    private static boolean hasOrCanMakeSupport(ServerLevel level, BlockPos support) {
        BlockPos cursor = support;
        for (int depth = 0; depth <= MAX_FILL_DEPTH; depth++) {
            BlockState state = level.getBlockState(cursor);
            if (!state.getFluidState().isEmpty() || level.getBlockEntity(cursor) != null) return false;
            if (!state.isAir() && !state.canBeReplaced()) return true;
            cursor = cursor.below();
        }
        return false;
    }

    private static List<Placement> createPlan(RoadConstructionState road) {
        List<BlockPos> centers = road.centers();
        Map<BlockPos, Boolean> footprints = footprintMap(centers);
        List<Placement> placements = new ArrayList<>(footprints.size());
        for (Map.Entry<BlockPos, Boolean> entry : footprints.entrySet()) {
            placements.add(new Placement(entry.getKey(), entry.getValue()
                    ? Blocks.GRAVEL.defaultBlockState()
                    : Blocks.COBBLESTONE.defaultBlockState()));
        }
        return placements;
    }

    private static Map<BlockPos, Boolean> footprintMap(List<BlockPos> centers) {
        Map<BlockPos, Boolean> footprints = new LinkedHashMap<>();
        for (int i = 0; i < centers.size(); i++) {
            BlockPos center = centers.get(i);
            int[] direction = directionAt(centers, i);
            for (int side = -1; side <= 1; side++) {
                BlockPos pos = new BlockPos(
                        center.getX() - direction[1] * side,
                        center.getY(),
                        center.getZ() + direction[0] * side);
                if (side == 0) footprints.put(pos, true);
                else footprints.putIfAbsent(pos, false);
            }
        }
        return footprints;
    }

    private static int[] directionAt(List<BlockPos> centers, int index) {
        if (centers.size() < 2) return new int[] {1, 0};
        BlockPos from;
        BlockPos to;
        if (index < centers.size() - 1) {
            from = centers.get(index);
            to = centers.get(index + 1);
        } else {
            from = centers.get(index - 1);
            to = centers.get(index);
        }
        int dx = Integer.signum(to.getX() - from.getX());
        int dz = Integer.signum(to.getZ() - from.getZ());
        return Math.abs(dx) + Math.abs(dz) == 1 ? new int[] {dx, dz} : new int[] {1, 0};
    }

    private static RouteCheck invalid(String message) {
        return new RouteCheck(false, List.of(), 0, message);
    }

    private static boolean isClearableForRoad(BlockState state) {
        return state.isAir()
                || state.canBeReplaced()
                || state.is(BlockTags.LEAVES)
                || isRoadGround(state);
    }

    private static boolean isRoadGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK);
    }

    private static int[] horizontalDirection(float yaw) {
        int quadrant = Math.floorMod((int) Math.floor(yaw / 90.0F + 0.5F), 4);
        return switch (quadrant) {
            case 0 -> new int[] {0, 1};
            case 1 -> new int[] {-1, 0};
            case 2 -> new int[] {0, -1};
            default -> new int[] {1, 0};
        };
    }
}
