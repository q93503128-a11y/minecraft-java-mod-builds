package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
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
    private static final int MAX_BRIDGE_SPAN = 6;
    private static final int BRIDGE_SURCHARGE_PER_CENTER = 2;
    private static final int STAIR_SURCHARGE_PER_CENTER = 1;
    private static final int DIRECT_BLOCK_UPDATE = 2;
    private static final int NORMAL_BLOCK_UPDATE = 3;
    private static final double BUILDER_WORK_RANGE_SQR = 18.0D;
    private static final double BRIDGE_WORK_RANGE_SQR = 64.0D;
    private static final double STORAGE_INTERACTION_RANGE_SQR = 9.0D;
    private static final int HAUL_BATCH_SIZE = 16;
    private static final double ROAD_BUILDER_SEARCH_MARGIN = 96.0D;
    private static final long PLAYER_ENDPOINT_RANGE_SQR = 16L * 16L;

    private SettlementRoadService() {}

    public record StartResult(boolean started, String message) {}
    public record RouteCheck(boolean valid, List<BlockPos> centers, int stoneCost, String message) {}
    private record RouteCandidate(boolean valid, List<BlockPos> centers, List<Integer> profile, int score, String message) {}
    private record Placement(BlockPos pos, BlockState state, boolean bridge) {}
    private record FootprintSpec(boolean centerline, boolean bridge, Direction stairFacing) {}
    private record SurfaceSample(int y, BlockState state, boolean water) {}

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
        RouteCandidate chosen = chooseCandidate(level, data, startXZ, endXZ);
        if (!chosen.valid()) return invalid(chosen.message().isBlank()
                ? "두 자동 경로 모두 안전한 3칸 폭 도로를 만들 수 없습니다." : chosen.message());

        int cost = stoneCost(chosen);
        SettlementService.refreshResources(server, data);
        String resource = data.resources().stone() < cost
                ? " | 석재 부족: 필요 " + cost + " / 현재 " + data.resources().stone()
                : " | 석재 " + cost;
        int bridges = bridgeCenterCount(chosen.profile());
        int stairs = stairCenterCount(chosen.centers(), chosen.profile());
        String terrain = (stairs == 0 && bridges == 0) ? "" : " | 계단 " + stairs + " · 소교량 " + bridges;
        return new RouteCheck(true, chosen.centers(), cost,
                "경로 " + chosen.centers().size() + "블록" + terrain + resource);
    }

    public static StartResult startAt(ServerPlayer player, BlockPos selectedStart, BlockPos selectedEnd) {
        MinecraftServer server = player.level().getServer();
        SettlementData data = SettlementData.get(server);
        RouteCheck check = checkRoute(player, selectedStart, selectedEnd);
        if (!check.valid()) return new StartResult(false, check.message());

        BlockPos startXZ = new BlockPos(selectedStart.getX(), 0, selectedStart.getZ());
        BlockPos endXZ = new BlockPos(selectedEnd.getX(), 0, selectedEnd.getZ());
        RouteCandidate chosen = chooseCandidate(server.overworld(), data, startXZ, endXZ);
        if (!chosen.valid()) return new StartResult(false, "지형이 바뀌어 도로 경로를 다시 계산해야 합니다.");

        ServerLevel level = server.overworld();
        if (!SettlementStorageService.storageAvailable(level, data)) {
            return new StartResult(false, "공동 창고가 모두 로드된 상태에서 도로를 착공해 주세요. 자원은 차감되지 않았습니다.");
        }
        SettlementService.refreshResources(server, data);
        int requiredStone = stoneCost(chosen);
        if (data.resources().stone() < requiredStone) {
            return new StartResult(false, "도로 필요 석재 " + requiredStone + " | 현재 석재 " + data.resources().stone());
        }

        data.beginRoadConstruction(chosen.centers(), chosen.profile());
        SettlementConstructionService.ensureBuilder(level, data.centerPos());
        SettlementService.broadcast(server, data);
        return new StartResult(true, "개척 도로 착공: " + chosen.centers().size()
                + "블록 경로, 3칸 폭. 건설 주민이 지반·계단·짧은 수로 교량을 정리한 뒤 공동 창고의 실제 석재 "
                + requiredStone + "개를 운반하며 포설합니다.");
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
    }

    private static boolean tickPaving(MinecraftServer server, SettlementData data, RoadConstructionState road,
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
        if (!level.hasChunkAt(target)) {
            builder.getNavigation().stop();
            return false;
        }
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

    private static boolean moveBuilderToPlacement(ServerLevel level, Villager builder, Placement placement) {
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
    }

    private static boolean moveBuilderToCurrentSurface(ServerLevel level, Villager builder, BlockPos target) {
        int workY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
        BlockPos work = new BlockPos(target.getX(), workY, target.getZ());
        double distance = builder.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D);
        if (distance <= BUILDER_WORK_RANGE_SQR) return true;
        builder.getNavigation().moveTo(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D, 0.85D);
        return false;
    }

    private static boolean canGradePlacement(ServerLevel level, Placement placement) {
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

    private static int stoneCost(RouteCandidate candidate) {
        return stoneCost(candidate.centers().size())
                + bridgeCenterCount(candidate.profile()) * BRIDGE_SURCHARGE_PER_CENTER
                + stairCenterCount(candidate.centers(), candidate.profile()) * STAIR_SURCHARGE_PER_CENTER;
    }

    private static int stoneCost(RoadConstructionState road) {
        return stoneCost(road.centers().size())
                + road.bridgeCenterCount() * BRIDGE_SURCHARGE_PER_CENTER
                + stairCenterCount(road.centers(), road.profile()) * STAIR_SURCHARGE_PER_CENTER;
    }

    public static String phaseLabel(RoadConstructionState road) {
        if (road == null || !road.active()) return "도로 공사";
        List<Placement> plan = createPlan(road);
        if (road.grading()) return road.bridgeCenterCount() > 0 ? "도로 지반·교량 자리 정리" : "도로 지반 정리";
        if (road.step() < plan.size()) return road.bridgeCenterCount() > 0 ? "도로 계단·교량 석재 운반·포설" : "도로 석재 운반·포설";
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
        int gradedCount = road.grading() ? Math.max(0, Math.min(plan.size(), road.gradeStep())) : plan.size();
        for (int i = 0; i < plan.size(); i++) {
            Placement placement = plan.get(i);
            BlockPos surface = placement.pos();
            if (surface.equals(pos) && current.is(placement.state().getBlock())) {
                event.setCanceled(true);
                event.setNotifyClient(true);
                return;
            }
            if (i >= gradedCount || placement.bridge()) continue;
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

    private static RouteCandidate chooseCandidate(ServerLevel level, SettlementData data, BlockPos start, BlockPos end) {
        RouteCandidate xThenZ = assessCandidate(level, data, start, end, true);
        RouteCandidate zThenX = assessCandidate(level, data, start, end, false);
        return choose(xThenZ, zThenX);
    }

    private static RouteCandidate assessCandidate(ServerLevel level, SettlementData data,
                                                  BlockPos start, BlockPos end, boolean xFirst) {
        List<BlockPos> flat = manhattanPath(start, end, xFirst);
        if (flat.size() < MIN_ROUTE_LENGTH || flat.size() > MAX_ROUTE_LENGTH) {
            return invalidCandidate("도로 길이가 허용 범위를 벗어납니다.");
        }

        List<SurfaceSample> surfaces = new ArrayList<>(flat.size());
        for (BlockPos flatPos : flat) surfaces.add(sampleSurface(level, flatPos.getX(), flatPos.getZ()));
        if (surfaces.getFirst().water() || surfaces.getLast().water()) {
            return invalidCandidate("도로 시작점과 끝점은 물 밖의 단단한 지면에 두어 주세요.");
        }

        List<Integer> profile = new ArrayList<>(java.util.Collections.nCopies(flat.size(), RoadConstructionState.PROFILE_NORMAL));
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

        List<BlockPos> centers = new ArrayList<>(flat.size());
        int score = 0;
        int previousY = Integer.MIN_VALUE;
        for (int i = 0; i < flat.size(); i++) {
            BlockPos flatPos = flat.get(i);
            int roadY = profile.get(i) == RoadConstructionState.PROFILE_BRIDGE ? bridgeY[i] : surfaces.get(i).y();
            if (previousY != Integer.MIN_VALUE && Math.abs(roadY - previousY) > MAX_STEP_HEIGHT) {
                return invalidCandidate("자동 경로에 계단으로 처리할 수 없는 2블록 이상의 급경사가 있습니다.");
            }
            score += previousY == Integer.MIN_VALUE ? 0 : Math.abs(roadY - previousY) * 3;
            if (profile.get(i) == RoadConstructionState.PROFILE_BRIDGE) score += 8;
            centers.add(new BlockPos(flatPos.getX(), roadY, flatPos.getZ()));
            previousY = roadY;
        }

        for (int i = 0; i < centers.size(); i++) {
            BlockPos center = centers.get(i);
            boolean bridge = profile.get(i) == RoadConstructionState.PROFILE_BRIDGE;
            int[] direction = directionAt(centers, i);
            for (int side = -1; side <= 1; side++) {
                int x = center.getX() - direction[1] * side;
                int z = center.getZ() + direction[0] * side;
                BlockPos footprint = new BlockPos(x, center.getY(), z);
                if (overlapsBuildingOrOutpost(data, footprint)) {
                    return invalidCandidate("경로가 기존 건물이나 전초기지와 겹칩니다.");
                }
                if (overlapsExistingRoad(data.roads(), footprint) && i > 1 && i < centers.size() - 2) {
                    return invalidCandidate("경로 중간이 기존 도로와 겹칩니다. 시작·끝 접속만 허용됩니다.");
                }

                if (bridge) {
                    if (!bridgeColumnSafe(level, footprint)) {
                        return invalidCandidate("소교량 폭 안에 보호 블록·높은 장애물·위험한 유체가 있습니다.");
                    }
                    continue;
                }

                SurfaceSample natural = sampleSurface(level, x, z);
                if (natural.water() || !natural.state().getFluidState().isEmpty()) {
                    return invalidCandidate("3칸 폭 전체가 짧은 수로가 아니어서 안전한 교량을 만들 수 없습니다.");
                }
                if (Math.abs(natural.y() - center.getY()) > MAX_CROSS_SLOPE) {
                    return invalidCandidate("3칸 폭을 만들기엔 옆 경사가 너무 큽니다.");
                }
                score += Math.abs(natural.y() - center.getY());
                if (level.getBlockEntity(new BlockPos(x, natural.y(), z)) != null || !isRoadGround(natural.state())) {
                    return invalidCandidate("경로에 컨테이너·도로로 정리할 수 없는 지면이 있습니다.");
                }
                for (int y = center.getY(); y <= center.getY() + 2; y++) {
                    BlockPos check = new BlockPos(x, y, z);
                    if (level.getBlockEntity(check) != null) return invalidCandidate("경로 위에 보호해야 할 블록이 있습니다.");
                    BlockState state = level.getBlockState(check);
                    if (!state.getFluidState().isEmpty() || !isClearableForRoad(state)) {
                        return invalidCandidate("경로 위 공간을 안전하게 정리할 수 없습니다.");
                    }
                }
                if (!hasOrCanMakeSupport(level, footprint.below())) {
                    return invalidCandidate("도로 아래 지반이 너무 깊게 비어 있습니다.");
                }
            }
        }
        return new RouteCandidate(true, List.copyOf(centers), List.copyOf(profile), score, "");
    }

    private static SurfaceSample sampleSurface(ServerLevel level, int x, int z) {
        int worldY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        BlockState world = level.getBlockState(new BlockPos(x, worldY, z));
        if (world.getFluidState().is(FluidTags.WATER)) return new SurfaceSample(worldY, world, true);
        int roadY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        BlockState state = level.getBlockState(new BlockPos(x, roadY, z));
        return new SurfaceSample(roadY, state, state.getFluidState().is(FluidTags.WATER));
    }

    private static boolean bridgeColumnSafe(ServerLevel level, BlockPos target) {
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
    }

    private static Map<BlockPos, FootprintSpec> footprintMap(List<BlockPos> centers, List<Integer> profile) {
        Map<BlockPos, FootprintSpec> footprints = new LinkedHashMap<>();
        for (int i = 0; i < centers.size(); i++) {
            BlockPos center = centers.get(i);
            int[] direction = directionAt(centers, i);
            boolean bridge = i < profile.size() && profile.get(i) == RoadConstructionState.PROFILE_BRIDGE;
            Direction stairFacing = bridge ? null : stairFacingAt(centers, i);
            for (int side = -1; side <= 1; side++) {
                BlockPos pos = new BlockPos(center.getX() - direction[1] * side, center.getY(), center.getZ() + direction[0] * side);
                FootprintSpec incoming = new FootprintSpec(side == 0, bridge, stairFacing);
                FootprintSpec existing = footprints.get(pos);
                if (existing == null) footprints.put(pos, incoming);
                else footprints.put(pos, new FootprintSpec(existing.centerline() || incoming.centerline(),
                        existing.bridge() || incoming.bridge(), existing.stairFacing() != null ? existing.stairFacing() : incoming.stairFacing()));
            }
        }
        return footprints;
    }

    private static Direction stairFacingAt(List<BlockPos> centers, int index) {
        BlockPos current = centers.get(index);
        if (index + 1 < centers.size() && centers.get(index + 1).getY() > current.getY()) {
            return directionFromTo(current, centers.get(index + 1));
        }
        if (index > 0 && centers.get(index - 1).getY() > current.getY()) {
            return directionFromTo(current, centers.get(index - 1));
        }
        return null;
    }

    private static Direction directionFromTo(BlockPos from, BlockPos to) {
        int dx = Integer.signum(to.getX() - from.getX());
        int dz = Integer.signum(to.getZ() - from.getZ());
        if (dx > 0) return Direction.EAST;
        if (dx < 0) return Direction.WEST;
        if (dz > 0) return Direction.SOUTH;
        if (dz < 0) return Direction.NORTH;
        return Direction.NORTH;
    }

    private static int bridgeCenterCount(List<Integer> profile) {
        int count = 0;
        for (int value : profile) if (value == RoadConstructionState.PROFILE_BRIDGE) count++;
        return count;
    }

    private static int stairCenterCount(List<BlockPos> centers, List<Integer> profile) {
        int count = 0;
        for (int i = 0; i < centers.size(); i++) {
            boolean bridge = i < profile.size() && profile.get(i) == RoadConstructionState.PROFILE_BRIDGE;
            if (!bridge && stairFacingAt(centers, i) != null) count++;
        }
        return count;
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
        return state.isAir() || state.canBeReplaced() || state.is(BlockTags.LEAVES) || isRoadGround(state);
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
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.COBBLESTONE_STAIRS)
                || state.is(Blocks.STONE_BRICKS);
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
