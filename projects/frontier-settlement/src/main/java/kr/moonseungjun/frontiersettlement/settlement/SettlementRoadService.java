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
import kr.moonseungjun.frontiersettlement.content.FrontierWorkerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SettlementRoadService {
    public static final int LEGACY_ROAD_LENGTH = 16;
    public static final int ROAD_WIDTH = 3;
    public static final int MIN_ROUTE_LENGTH = 4;
    public static final int MAX_ROUTE_LENGTH = 96;
    private static final int MAX_STEP_HEIGHT = 1;
    private static final int MAX_CROSS_SLOPE = 1;
    private static final int MAX_FILL_DEPTH = 2;
    private static final int MAX_SHORT_BRIDGE_SPAN = 6;
    private static final int MAX_LONG_BRIDGE_SPAN = 24;
    private static final int MIN_RAVINE_DEPTH = 4;
    private static final int MAX_LONG_BRIDGE_PIER_DEPTH = 12;
    private static final int LONG_BRIDGE_PIER_INTERVAL = 6;
    private static final int MAX_TUNNEL_SPAN = 24;
    private static final int MIN_TUNNEL_SPAN = 3;
    private static final int MAX_TUNNEL_BENDS = 1;
    private static final int MIN_BENT_TUNNEL_LEG = 3;
    private static final int MIN_TUNNEL_COVER = 4;
    private static final int TUNNEL_CLEAR_HEIGHT = 3;
    private static final int TUNNEL_PORTAL_HALF_WIDTH = 2;
    private static final int TUNNEL_PORTAL_HEIGHT = 4;
    private static final int TUNNEL_PORTAL_FRAME_BLOCKS = 22;
    private static final int TUNNEL_SURCHARGE_PER_CENTER = 1;
    private static final int BRIDGE_SURCHARGE_PER_CENTER = 2;
    private static final int BRIDGE_SUPPORT_SURCHARGE = 1;
    private static final int STAIR_SURCHARGE_PER_CENTER = 1;
    private static final int DIRECT_BLOCK_UPDATE = 2;
    private static final int NORMAL_BLOCK_UPDATE = 3;
    private static final double BUILDER_WORK_RANGE_SQR = 18.0D;
    private static final double BRIDGE_WORK_RANGE_SQR = 16.0D;
    private static final double BRIDGE_SUPPORT_WORK_RANGE_SQR = 196.0D;
    private static final double STORAGE_INTERACTION_RANGE_SQR = 9.0D;
    private static final int HAUL_BATCH_SIZE = 16;
    private static final double ROAD_BUILDER_SEARCH_MARGIN = 96.0D;
    private static final long PLAYER_ENDPOINT_RANGE_SQR = 16L * 16L;

    private SettlementRoadService() {}

    public record StartResult(boolean started, String message) {}
    public record RouteCheck(boolean valid, List<BlockPos> centers, int stoneCost, String message) {}
    private record RouteCandidate(boolean valid, List<BlockPos> centers, List<Integer> profile,
                                  List<BlockPos> supports, int score, String message) {}
    private record Placement(BlockPos pos, BlockState state, boolean bridge, boolean support,
                             boolean tunnel, boolean portal) {}
    private record FootprintSpec(boolean centerline, boolean bridge, boolean tunnel, Direction stairFacing) {}
    private record SurfaceSample(int y, BlockState state, boolean water) {}
    private record BlockSnapshot(BlockPos pos, BlockState state) {}
    private record TunnelCell(BlockPos target, BlockPos work) {}
    private record SupportPlan(boolean valid, List<BlockPos> positions, String message) {
        static SupportPlan invalid(String message) { return new SupportPlan(false, List.of(), message); }
    }
    private record PierColumn(boolean valid, List<BlockPos> positions, String message) {
        static PierColumn invalid(String message) { return new PierColumn(false, List.of(), message); }
    }

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
        String projectBlock = SettlementProjectAuthority.startBlockReason(server, data, SettlementProjectAuthority.ProjectLane.ROAD);
        if (projectBlock != null) return invalid(projectBlock);
        if (data.houseCount() < 1 || data.lumberCampCount() < 1) {
            return invalid("첫 도로는 주택 1채와 벌목소 1곳을 완성한 뒤 열립니다.");
        }

        BlockPos startXZ = new BlockPos(selectedStart.getX(), 0, selectedStart.getZ());
        BlockPos endXZ = new BlockPos(selectedEnd.getX(), 0, selectedEnd.getZ());
        long dx = Math.abs((long) endXZ.getX() - startXZ.getX());
        long dz = Math.abs((long) endXZ.getZ() - startXZ.getZ());
        long manhattan = dx + dz + 1L;
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
        if (!SettlementProjectAuthority.routeSeparatedFromOtherActive(data, SettlementProjectAuthority.ProjectLane.ROAD, chosen.centers())) {
            return invalid("동시 공사 도로는 다른 활성 공사 현장에서 " + SettlementProjectAuthority.MIN_PARALLEL_SEPARATION + "블록 이상 떨어져야 합니다.");
        }

        if (!chosen.supports().isEmpty() && SettlementTier.current(data).ordinal() < SettlementTier.VILLAGE.ordinal()) {
            return invalid("교각이 필요한 장교량·협곡 횡단은 마을 단계부터 건설할 수 있습니다.");
        }
        int tunnels = tunnelCenterCount(chosen.profile());
        int tunnelBends = tunnelBendCount(chosen.centers(), chosen.profile());
        int tunnelRuns = tunnelRunCount(chosen.profile());
        if (tunnels > 0 && (SettlementTier.current(data).ordinal() < SettlementTier.FRONTIER_TOWN.ordinal()
                || data.buildingCount(BuildingType.CONSTRUCTION_OFFICE) < 1)) {
            return invalid("자동 터널은 개척 도시 + 건설소 단계부터 사용할 수 있습니다.");
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
        String tunnelDetail = tunnels == 0 ? "" : " · 터널 " + tunnels
                + (tunnelBends == 0 ? "" : " · 굴곡 " + tunnelBends)
                + " · 석재 포털 " + (tunnelRuns * 2);
        String terrain = (stairs == 0 && bridges == 0 && tunnels == 0) ? ""
                : " | 계단 " + stairs + bridgeDetail + tunnelDetail;
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

        data.beginRoadConstruction(chosen.centers(), chosen.profile(), chosen.supports());
        if (SettlementConstructionService.infrastructureProjectBuilder(level, data, SettlementProjectAuthority.ProjectLane.ROAD) == null) {
            data.clearRoadConstruction();
            SettlementService.broadcast(server, data);
            return new StartResult(false, "건설 작업자를 안전하게 확보할 수 없어 도로 착공을 취소했습니다. 주변 마을·공동 창고 청크를 로드한 뒤 다시 시도해 주세요. 자원은 차감되지 않았습니다.");
        }
        SettlementService.broadcast(server, data);
        String bridge = chosen.supports().isEmpty() ? ""
                : " 장교량/협곡 교각 " + chosen.supports().size() + "블록 포함.";
        int tunnels = tunnelCenterCount(chosen.profile());
        int tunnelBends = tunnelBendCount(chosen.centers(), chosen.profile());
        int tunnelRuns = tunnelRunCount(chosen.profile());
        String tunnel = tunnels == 0 ? "" : " 터널 " + tunnels + "블록"
                + (tunnelBends == 0 ? "" : "(90도 굴곡 " + tunnelBends + "회)")
                + " + 실제 석재 포털 " + (tunnelRuns * 2) + "개 포함.";
        return new StartResult(true, "개척 도로 착공: " + chosen.centers().size()
                + "블록 경로, 3칸 폭." + bridge + tunnel + " 건설 주민이 지반·계단·교량·터널을 정리한 뒤 공동 창고의 실제 석재 "
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
        FrontierWorkerEntity builder = SettlementConstructionService.infrastructureProjectBuilder(
                level, data, SettlementProjectAuthority.ProjectLane.ROAD);
        if (builder == null) return false;
        if (builder.isNoAi()) builder.setNoAi(false);
        builder.setInvulnerable(false);

        if (road.grading()) return tickGrading(server, data, road, plan, builder);
        if (road.tunneling()) return tickTunneling(server, data, road, builder);
        if (road.step() >= plan.size()) return finishIfValid(server, data, road, plan, builder);
        return tickPaving(server, data, road, plan, builder);
    }

    private static FrontierWorkerEntity findRoadBuilder(ServerLevel level, SettlementData data, BlockPos settlementCenter,
                                            RoadConstructionState road, List<Placement> plan) {
        BlockPos hint;
        if (road.tunneling()) {
            List<TunnelCell> tunnel = tunnelExcavationPlan(road);
            int index = Math.max(0, Math.min(tunnel.size() - 1, road.tunnelStep()));
            hint = tunnel.isEmpty() ? road.start() : tunnel.get(index).work();
        } else {
            int rawIndex = road.grading() ? road.gradeStep() : road.step();
            int index = Math.max(0, Math.min(plan.size() - 1, rawIndex));
            hint = plan.get(index).pos();
        }
        double minX = Math.min(settlementCenter.getX(), hint.getX()) - ROAD_BUILDER_SEARCH_MARGIN;
        double minY = Math.min(settlementCenter.getY(), hint.getY()) - 64.0D;
        double minZ = Math.min(settlementCenter.getZ(), hint.getZ()) - ROAD_BUILDER_SEARCH_MARGIN;
        double maxX = Math.max(settlementCenter.getX(), hint.getX()) + ROAD_BUILDER_SEARCH_MARGIN + 1.0D;
        double maxY = Math.max(settlementCenter.getY(), hint.getY()) + 65.0D;
        double maxZ = Math.max(settlementCenter.getZ(), hint.getZ()) + ROAD_BUILDER_SEARCH_MARGIN + 1.0D;
        AABB corridor = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        List<FrontierWorkerEntity> tagged = level.getEntitiesOfClass(FrontierWorkerEntity.class, corridor,
                villager -> villager.entityTags().contains(SettlementConstructionService.BUILDER_TAG));
        tagged.sort(java.util.Comparator.comparing(villager -> villager.getUUID().toString()));
        if (!tagged.isEmpty()) return tagged.getFirst();
        return SettlementConstructionService.ensureBuilder(level, data);
    }

    private static boolean tickGrading(MinecraftServer server, SettlementData data, RoadConstructionState road,
                                       List<Placement> plan, FrontierWorkerEntity builder) {
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
        // Bridge/tunnel structural cells are validated here; physical tunnel excavation has its own persisted phase.
        if (!placement.bridge() && !placement.tunnel() && !moveBuilderToPlacement(level, builder, placement)) return false;
        if (!applyGradePlacement(level, placement)) return false;
        builder.swing(InteractionHand.MAIN_HAND);
        data.advanceRoadConstruction();
        RoadConstructionState next = data.roadConstruction();
        if (next.grading() && next.gradeStep() >= plan.size()) {
            data.replaceRoadConstructionStep(0);
            SettlementService.broadcast(server, data);
        }
        return false;
    }

    private static boolean tickTunneling(MinecraftServer server, SettlementData data,
                                         RoadConstructionState road, FrontierWorkerEntity builder) {
        List<TunnelCell> cells = tunnelExcavationPlan(road);
        int step = road.tunnelStep();
        if (step < 0) return false;
        if (step >= cells.size()) {
            data.replaceRoadConstructionStep(0);
            SettlementService.broadcast(server, data);
            return false;
        }
        ServerLevel level = server.overworld();
        TunnelCell cell = cells.get(step);
        if (!level.hasChunkAt(cell.target()) || !level.hasChunkAt(cell.work())) {
            builder.getNavigation().stop();
            return false;
        }
        BlockState current = level.getBlockState(cell.target());
        if (current.isAir() || current.canBeReplaced()) {
            data.advanceRoadConstruction();
            return false;
        }
        if (level.getBlockEntity(cell.target()) != null || !current.getFluidState().isEmpty()
                || (!isNaturalTunnelExcavation(current) && !isNaturalTunnelPortalBlock(current))) {
            builder.getNavigation().stop();
            return false;
        }
        if (!moveBuilderToTunnelWork(builder, cell.work())) return false;
        if (!level.setBlock(cell.target(), Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE)) return false;
        builder.swing(InteractionHand.MAIN_HAND);
        data.advanceRoadConstruction();
        return false;
    }

    private static boolean moveBuilderToTunnelWork(FrontierWorkerEntity builder, BlockPos floor) {
        double x = floor.getX() + 0.5D;
        double y = floor.getY() + 1.0D;
        double z = floor.getZ() + 0.5D;
        if (builder.distanceToSqr(x, y, z) <= BUILDER_WORK_RANGE_SQR) return true;
        builder.getNavigation().moveTo(x, y, z, 0.82D);
        return false;
    }

    private static boolean tickPaving(MinecraftServer server, SettlementData data, RoadConstructionState road,
                                      List<Placement> plan, FrontierWorkerEntity builder) {
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
            if (!placement.portal()) {
                BlockState support = level.getBlockState(target.below());
                if (support.isAir() || support.canBeReplaced() || !support.getFluidState().isEmpty()) {
                    builder.getNavigation().stop();
                    return false;
                }
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
        if (placement.portal()) return current.isAir() || current.canBeReplaced();
        if (placement.support()) {
            return current.isAir() || current.canBeReplaced() || current.getFluidState().is(FluidTags.WATER);
        }
        return current.isAir() || isRoadGround(current);
    }

    private static boolean ensurePavingMaterial(MinecraftServer server, SettlementData data, FrontierWorkerEntity builder,
                                                long requiredNow, long remainingCost) {
        ItemStack carried = builder.getMainHandItem();
        if (!carried.isEmpty() && !SettlementInventory.isStone(carried)) {
            return returnCarriedToStorage(server, data, builder);
        }
        if (requiredNow <= 0L) return true;
        if (!carried.isEmpty() && carried.getCount() >= requiredNow) return true;
        if (!carried.isEmpty()) return returnCarriedToStorage(server, data, builder);

        ServerLevel level = server.overworld();
        BlockPos source = SettlementWorkerStorageNavigation.findReachableExtractionTarget(
                level, data, builder, SettlementInventory::isStone, STORAGE_INTERACTION_RANGE_SQR);
        if (source == null) {
            builder.getNavigation().stop();
            return false;
        }
        if (builder.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)
                > STORAGE_INTERACTION_RANGE_SQR) {
            SettlementWorkerStorageNavigation.moveToInteraction(
                    level, builder, source, 0.9D, STORAGE_INTERACTION_RANGE_SQR);
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

    private static boolean consumeCarriedStone(FrontierWorkerEntity builder, long amount) {
        if (amount <= 0L) return true;
        ItemStack carried = builder.getMainHandItem();
        if (carried.isEmpty() || !SettlementInventory.isStone(carried) || carried.getCount() < amount) return false;
        carried.shrink((int) amount);
        if (carried.isEmpty()) builder.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        return true;
    }

    private static boolean returnCarriedToStorage(MinecraftServer server, SettlementData data, FrontierWorkerEntity builder) {
        ItemStack carried = builder.getMainHandItem();
        if (carried.isEmpty()) return true;
        ServerLevel level = server.overworld();
        BlockPos target = SettlementWorkerStorageNavigation.findReachableDepositTarget(
                level, data, builder, carried, STORAGE_INTERACTION_RANGE_SQR);
        if (target == null) {
            builder.getNavigation().stop();
            return false;
        }
        if (builder.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                > STORAGE_INTERACTION_RANGE_SQR) {
            SettlementWorkerStorageNavigation.moveToInteraction(
                    level, builder, target, 0.9D, STORAGE_INTERACTION_RANGE_SQR);
            return false;
        }
        int before = carried.getCount();
        ItemStack remaining = SettlementStorageService.insertAt(level, target, carried);
        builder.setItemSlot(EquipmentSlot.MAINHAND, remaining);
        if (remaining.getCount() < before) {
            SettlementService.refreshResources(server, data);
            SettlementService.broadcast(server, data);
        }
        return remaining.isEmpty();
    }

    private static boolean moveBuilderToPlacement(ServerLevel level, FrontierWorkerEntity builder, Placement placement) {
        if (!level.hasChunkAt(placement.pos())) {
            builder.getNavigation().stop();
            return false;
        }
        if (placement.portal()) {
            BlockPos approach = tunnelPortalApproach(level, placement.pos());
            if (approach == null) return false;
            double distance = builder.distanceToSqr(approach.getX() + 0.5D, approach.getY() + 1.0D, approach.getZ() + 0.5D);
            if (distance <= BUILDER_WORK_RANGE_SQR) return true;
            builder.getNavigation().moveTo(approach.getX() + 0.5D, approach.getY() + 1.0D, approach.getZ() + 0.5D, 0.82D);
            return false;
        }
        if (placement.tunnel()) {
            double x = placement.pos().getX() + 0.5D;
            double y = placement.pos().getY() + 1.0D;
            double z = placement.pos().getZ() + 0.5D;
            if (builder.distanceToSqr(x, y, z) <= BUILDER_WORK_RANGE_SQR) return true;
            builder.getNavigation().moveTo(x, y, z, 0.82D);
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

    private static BlockPos tunnelPortalApproach(ServerLevel level, BlockPos target) {
        for (int down = 1; down <= TUNNEL_PORTAL_HEIGHT; down++) {
            int y = target.getY() - down;
            for (int dx = -TUNNEL_PORTAL_HALF_WIDTH; dx <= TUNNEL_PORTAL_HALF_WIDTH; dx++) {
                for (int dz = -TUNNEL_PORTAL_HALF_WIDTH; dz <= TUNNEL_PORTAL_HALF_WIDTH; dz++) {
                    BlockPos candidate = new BlockPos(target.getX() + dx, y, target.getZ() + dz);
                    if (!level.hasChunkAt(candidate)) continue;
                    if (isRoadPavingBlock(level.getBlockState(candidate))) return candidate;
                }
            }
        }
        return null;
    }

    private static BlockPos bridgeApproach(ServerLevel level, BlockPos target) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = target.relative(direction);
            if (level.hasChunkAt(neighbor) && isRoadPavingBlock(level.getBlockState(neighbor))) return neighbor;
        }
        return null;
    }

    private static BlockPos findBridgeDeckAbove(ServerLevel level, BlockPos support) {
        for (int dy = 1; dy <= MAX_LONG_BRIDGE_PIER_DEPTH; dy++) {
            BlockPos candidate = support.above(dy);
            if (!level.hasChunkAt(candidate)) return null;
            if (level.getBlockState(candidate).is(Blocks.STONE_BRICKS)) return candidate;
        }
        return null;
    }

    private static boolean isRoadPavingBlock(BlockState state) {
        return state.is(Blocks.GRAVEL) || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.COBBLESTONE_STAIRS) || state.is(Blocks.STONE_BRICKS);
    }

    private static boolean moveBuilderToCurrentSurface(ServerLevel level, FrontierWorkerEntity builder, BlockPos target) {
        if (!level.hasChunkAt(target)) return false;
        int workY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
        BlockPos work = new BlockPos(target.getX(), workY, target.getZ());
        double distance = builder.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D);
        if (distance <= BUILDER_WORK_RANGE_SQR) return true;
        builder.getNavigation().moveTo(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D, 0.85D);
        return false;
    }

    private static boolean canGradePlacement(ServerLevel level, Placement placement) {
        BlockPos target = placement.pos();
        if (!level.hasChunkAt(target)) return false;
        BlockState current = level.getBlockState(target);
        if (level.getBlockEntity(target) != null) return false;
        if (placement.portal()) return tunnelPortalCellSafe(level, target);
        if (placement.support()) {
            if (!current.getFluidState().isEmpty() && !current.getFluidState().is(FluidTags.WATER)) return false;
            return current.is(placement.state().getBlock()) || current.isAir() || current.canBeReplaced()
                    || current.getFluidState().is(FluidTags.WATER);
        }
        if (placement.tunnel()) return tunnelFootprintSafe(level, target);
        if (!current.getFluidState().isEmpty()) return false;
        if (!current.isAir() && !current.canBeReplaced() && !isRoadGround(current)) return false;

        for (int y = target.getY() + 1; y <= target.getY() + 2; y++) {
            BlockPos pos = new BlockPos(target.getX(), y, target.getZ());
            if (!level.hasChunkAt(pos)) return false;
            BlockState state = level.getBlockState(pos);
            if (level.getBlockEntity(pos) != null || !state.getFluidState().isEmpty()) return false;
            if (!isClearableForRoad(state)) return false;
        }
        return placement.bridge() || hasOrCanMakeSupport(level, target.below());
    }

    private static boolean applyGradePlacement(ServerLevel level, Placement placement) {
        if (placement.support() || placement.tunnel() || placement.portal()) return true;
        List<BlockSnapshot> changed = new ArrayList<>();
        BlockPos target = placement.pos();
        for (int y = target.getY() + 2; y >= target.getY() + 1; y--) {
            BlockPos pos = new BlockPos(target.getX(), y, target.getZ());
            if (!level.hasChunkAt(pos)) { rollbackGradeMutation(level, changed); return false; }
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && isClearableForRoad(state)
                    && !setGradeBlock(level, pos, Blocks.AIR.defaultBlockState(), changed)) {
                rollbackGradeMutation(level, changed);
                return false;
            }
        }
        if (placement.bridge()) {
            BlockState targetState = level.getBlockState(target);
            if (targetState.canBeReplaced() && targetState.getFluidState().isEmpty()
                    && !setGradeBlock(level, target, Blocks.AIR.defaultBlockState(), changed)) {
                rollbackGradeMutation(level, changed);
                return false;
            }
            return true;
        }

        BlockPos cursor = target.below();
        for (int depth = 0; depth <= MAX_FILL_DEPTH; depth++) {
            if (!level.hasChunkAt(cursor)) { rollbackGradeMutation(level, changed); return false; }
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir() && !state.canBeReplaced()) break;
            if (!setGradeBlock(level, cursor, Blocks.COARSE_DIRT.defaultBlockState(), changed)) {
                rollbackGradeMutation(level, changed);
                return false;
            }
            cursor = cursor.below();
        }
        BlockState targetState = level.getBlockState(target);
        if ((targetState.isAir() || targetState.canBeReplaced())
                && !setGradeBlock(level, target, Blocks.COARSE_DIRT.defaultBlockState(), changed)) {
            rollbackGradeMutation(level, changed);
            return false;
        }
        return true;
    }

    private static boolean setGradeBlock(ServerLevel level, BlockPos pos, BlockState next,
                                         List<BlockSnapshot> changed) {
        BlockState current = level.getBlockState(pos);
        if (current.equals(next)) return true;
        if (!level.setBlock(pos, next, DIRECT_BLOCK_UPDATE)) return false;
        changed.add(new BlockSnapshot(pos, current));
        return true;
    }

    private static void rollbackGradeMutation(ServerLevel level, List<BlockSnapshot> changed) {
        for (int i = changed.size() - 1; i >= 0; i--) {
            BlockSnapshot snapshot = changed.get(i);
            if (level.hasChunkAt(snapshot.pos())) {
                level.setBlock(snapshot.pos(), snapshot.state(), DIRECT_BLOCK_UPDATE);
            }
        }
    }

    public static int totalSteps(RoadConstructionState road) {
        if (road == null || !road.active()) return 0;
        return road.tunneling() ? tunnelExcavationPlan(road).size() : createPlan(road).size();
    }

    public static int stoneCost(int centerlineLength) {
        return Math.max(6, (centerlineLength * 3 + 1) / 2);
    }

    private static int stoneCost(RouteCandidate candidate) {
        return stoneCost(candidate.centers().size())
                + bridgeCenterCount(candidate.profile()) * BRIDGE_SURCHARGE_PER_CENTER
                + candidate.supports().size() * BRIDGE_SUPPORT_SURCHARGE
                + tunnelCenterCount(candidate.profile()) * TUNNEL_SURCHARGE_PER_CENTER
                + tunnelRunCount(candidate.profile()) * TUNNEL_PORTAL_FRAME_BLOCKS
                + stairCenterCount(candidate.centers(), candidate.profile()) * STAIR_SURCHARGE_PER_CENTER;
    }

    private static int stoneCost(RoadConstructionState road) {
        return stoneCost(road.centers().size())
                + road.bridgeCenterCount() * BRIDGE_SURCHARGE_PER_CENTER
                + road.bridgeSupportCount() * BRIDGE_SUPPORT_SURCHARGE
                + road.tunnelCenterCount() * TUNNEL_SURCHARGE_PER_CENTER
                + tunnelRunCount(road.profile()) * TUNNEL_PORTAL_FRAME_BLOCKS
                + stairCenterCount(road.centers(), road.profile()) * STAIR_SURCHARGE_PER_CENTER;
    }

    public static String phaseLabel(RoadConstructionState road) {
        if (road == null || !road.active()) return "도로 공사";
        List<Placement> plan = createPlan(road);
        if (road.grading()) return road.hasTunnel() ? "도로 터널·지반 안전 검사"
                : road.bridgeSupportCount() > 0 ? "도로 장교량·교각 자리 검사"
                : road.bridgeCenterCount() > 0 ? "도로 지반·교량 자리 정리" : "도로 지반 정리";
        if (road.tunneling()) return "도로 터널 굴착 " + road.tunnelStep() + "/" + tunnelExcavationPlan(road).size();
        if (road.step() < plan.size()) return road.hasTunnel() ? "도로 터널 포장·석재 포털 운반·시공"
                : road.bridgeSupportCount() > 0 ? "도로 장교량·교각 석재 운반·시공"
                : road.bridgeCenterCount() > 0 ? "도로 계단·교량 석재 운반·포설" : "도로 석재 운반·포설";
        return "도로 마감 확인";
    }

    private static long costAtStep(long totalCost, int step, int totalSteps) {
        if (totalCost <= 0L || step <= 0 || totalSteps <= 0) return 0L;
        if (step >= totalSteps) return totalCost;
        return totalCost * step / totalSteps;
    }

    private static boolean finishIfValid(MinecraftServer server, SettlementData data,
                                         RoadConstructionState road, List<Placement> plan, FrontierWorkerEntity builder) {
        ServerLevel level = server.overworld();
        // Alpha.24-and-earlier roads already paid their full stone cost before construction state was saved.
        // New physical roads must pay for every repair, but legacy prepaid saves must never be charged twice.
        boolean legacyPrepaidRepair = road.legacyPrepaidPaving();
        for (Placement placement : plan) {
            if (!level.hasChunkAt(placement.pos())) return false;
            BlockState current = level.getBlockState(placement.pos());
            if (current.is(placement.state().getBlock())) continue;
            if (!canReplaceForPlacement(current, placement)) {
                builder.getNavigation().stop();
                return false;
            }
            if (!placement.bridge()) {
                if (!placement.portal()) {
                    BlockState support = level.getBlockState(placement.pos().below());
                    if (support.isAir() || support.canBeReplaced() || !support.getFluidState().isEmpty()) {
                        builder.getNavigation().stop();
                        return false;
                    }
                }
            }
            if (!legacyPrepaidRepair && !ensurePavingMaterial(server, data, builder, 1L, 1L)) return false;
            if (!moveBuilderToPlacement(level, builder, placement)) return false;
            if (!level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)) return false;
            if (!legacyPrepaidRepair && !consumeCarriedStone(builder, 1L)) {
                level.setBlock(placement.pos(), current, DIRECT_BLOCK_UPDATE);
                return false;
            }
            builder.swing(InteractionHand.MAIN_HAND);
            return false;
        }

        if (!returnCarriedToStorage(server, data, builder)) return false;
        if (!SettlementConstructionService.returnBuilderHome(level, data, builder)) return false;
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
        for (TunnelCell cell : tunnelExcavationPlan(road)) {
            if (cell.target().equals(pos) && !current.isAir()) {
                event.setCanceled(true);
                event.setNotifyClient(true);
                return;
            }
        }
        List<Placement> plan = createPlan(road);
        int gradedCount = road.grading() ? Math.max(0, Math.min(plan.size(), road.gradeStep())) : plan.size();
        for (int i = 0; i < plan.size(); i++) {
            Placement placement = plan.get(i);
            BlockPos surface = placement.pos();
            // The natural tunnel floor is future paving/support and must not be removed mid-project.
            if (placement.tunnel() && surface.equals(pos)) {
                event.setCanceled(true);
                event.setNotifyClient(true);
                return;
            }
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
        for (BlockPos flatPos : flat) {
            SurfaceSample sample = sampleSurface(level, flatPos.getX(), flatPos.getZ());
            if (sample == null) return invalidCandidate("도로 예정 경로 전체가 로드된 상태에서 계획해 주세요.");
            surfaces.add(sample);
        }
        if (surfaces.getFirst().water() || surfaces.getLast().water()) {
            return invalidCandidate("도로 시작점과 끝점은 물 밖의 단단한 지면에 두어 주세요.");
        }

        List<Integer> profile = new ArrayList<>(java.util.Collections.nCopies(flat.size(), RoadConstructionState.PROFILE_NORMAL));
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

        int[] tunnelY = new int[flat.size()];
        java.util.Arrays.fill(tunnelY, Integer.MIN_VALUE);
        for (int i = 1; i < flat.size() - 1; i++) {
            if (profile.get(i) != RoadConstructionState.PROFILE_NORMAL || surfaces.get(i).water()) continue;
            SurfaceSample entry = surfaces.get(i - 1);
            if (entry.water() || surfaces.get(i).y() - entry.y() < MIN_TUNNEL_COVER) continue;
            int bestEnd = -1;
            int bestY = Integer.MIN_VALUE;
            int maxEnd = Math.min(flat.size() - 2, i + MAX_TUNNEL_SPAN - 1);
            for (int tunnelEnd = i; tunnelEnd <= maxEnd; tunnelEnd++) {
                if (profile.get(tunnelEnd) != RoadConstructionState.PROFILE_NORMAL || surfaces.get(tunnelEnd).water()) break;
                SurfaceSample exit = surfaces.get(tunnelEnd + 1);
                if (exit.water() || Math.abs(entry.y() - exit.y()) > MAX_STEP_HEIGHT) continue;
                int grade = Math.max(entry.y(), exit.y());
                boolean covered = true;
                for (int j = i; j <= tunnelEnd; j++) {
                    if (surfaces.get(j).y() < grade + MIN_TUNNEL_COVER) { covered = false; break; }
                }
                if (!covered) continue;
                int span = tunnelEnd - i + 1;
                if (span < MIN_TUNNEL_SPAN) continue;
                int turns = tunnelTurnCount(flat, i, tunnelEnd);
                if (turns > MAX_TUNNEL_BENDS) continue;
                if (turns == 1 && !bentTunnelLegsLongEnough(flat, i, tunnelEnd)) continue;
                bestEnd = tunnelEnd; bestY = grade; break;
            }
            if (bestEnd < 0) continue;
            for (int j = i; j <= bestEnd; j++) {
                profile.set(j, RoadConstructionState.PROFILE_TUNNEL);
                tunnelY[j] = bestY;
            }
            i = bestEnd;
        }

        List<BlockPos> centers = new ArrayList<>(flat.size());
        int score = 0;
        int previousY = Integer.MIN_VALUE;
        for (int i = 0; i < flat.size(); i++) {
            BlockPos flatPos = flat.get(i);
            int roadY = profile.get(i) == RoadConstructionState.PROFILE_BRIDGE ? bridgeY[i]
                    : profile.get(i) == RoadConstructionState.PROFILE_TUNNEL ? tunnelY[i] : surfaces.get(i).y();
            if (previousY != Integer.MIN_VALUE && Math.abs(roadY - previousY) > MAX_STEP_HEIGHT) {
                return invalidCandidate("자동 경로에 계단으로 처리할 수 없는 2블록 이상의 급경사가 있습니다.");
            }
            score += previousY == Integer.MIN_VALUE ? 0 : Math.abs(roadY - previousY) * 3;
            if (profile.get(i) == RoadConstructionState.PROFILE_BRIDGE) score += 8;
            if (profile.get(i) == RoadConstructionState.PROFILE_TUNNEL) score += 10;
            centers.add(new BlockPos(flatPos.getX(), roadY, flatPos.getZ()));
            previousY = roadY;
        }

        for (int i = 0; i < centers.size(); i++) {
            BlockPos center = centers.get(i);
            boolean bridge = profile.get(i) == RoadConstructionState.PROFILE_BRIDGE;
            boolean tunnel = profile.get(i) == RoadConstructionState.PROFILE_TUNNEL;
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
                        return invalidCandidate("교량 폭 안에 보호 블록·높은 장애물·위험한 유체가 있습니다.");
                    }
                    continue;
                }
                if (tunnel) {
                    if (!tunnelFootprintSafe(level, footprint)) {
                        return invalidCandidate("터널 굴착 범위에 광석·유체·컨테이너·플레이어/비자연 블록 또는 빈 공동이 있습니다.");
                    }
                    continue;
                }

                SurfaceSample natural = sampleSurface(level, x, z);
                if (natural == null) return invalidCandidate("도로 3칸 폭 전체가 로드된 상태에서 계획해 주세요.");
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
        String tunnelPortalError = validateTunnelPortals(level, data, centers, profile);
        if (!tunnelPortalError.isBlank()) return invalidCandidate(tunnelPortalError);
        score += tunnelBendCount(centers, profile) * 12;
        SupportPlan supports = planBridgeSupports(level, centers, profile);
        if (!supports.valid()) return invalidCandidate(supports.message());
        score += supports.positions().size() * 2;
        return new RouteCandidate(true, List.copyOf(centers), List.copyOf(profile), supports.positions(), score, "");
    }

    private static SurfaceSample sampleSurface(ServerLevel level, int x, int z) {
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) return null;
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
        if (natural == null) return false;
        if (!natural.water() && natural.y() > target.getY()) return false;
        return natural.water() || target.getY() - natural.y() <= MAX_LONG_BRIDGE_PIER_DEPTH;
    }

    private static boolean tunnelFootprintSafe(ServerLevel level, BlockPos floor) {
        if (!level.hasChunkAt(floor) || level.getBlockEntity(floor) != null) return false;
        BlockState floorState = level.getBlockState(floor);
        if (!floorState.getFluidState().isEmpty() || !isNaturalSupportGround(floorState)) return false;
        for (int y = 1; y <= TUNNEL_CLEAR_HEIGHT; y++) {
            BlockPos pos = floor.above(y);
            if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) != null) return false;
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty() || !isNaturalTunnelExcavation(state)) return false;
        }
        return true;
    }

    private static boolean isNaturalTunnelExcavation(BlockState state) {
        return state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL)
                || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.ANDESITE) || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE)
                || state.is(Blocks.TUFF) || state.is(Blocks.CLAY) || state.is(BlockTags.DIRT);
    }

    private static boolean isNaturalTunnelPortalBlock(BlockState state) {
        return isNaturalTunnelExcavation(state) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK);
    }

    private static int tunnelTurnCount(List<BlockPos> centers, int runStart, int runEnd) {
        int turns = 0;
        int previousDx = 0;
        int previousDz = 0;
        int firstEdge = Math.max(1, runStart);
        int lastEdge = Math.min(centers.size() - 1, runEnd + 1);
        for (int edge = firstEdge; edge <= lastEdge; edge++) {
            int dx = Integer.signum(centers.get(edge).getX() - centers.get(edge - 1).getX());
            int dz = Integer.signum(centers.get(edge).getZ() - centers.get(edge - 1).getZ());
            if (Math.abs(dx) + Math.abs(dz) != 1) return Integer.MAX_VALUE;
            if (previousDx != 0 || previousDz != 0) {
                if (dx != previousDx || dz != previousDz) turns++;
            }
            previousDx = dx;
            previousDz = dz;
        }
        return turns;
    }

    private static boolean bentTunnelLegsLongEnough(List<BlockPos> centers, int runStart, int runEnd) {
        int firstDx = Integer.signum(centers.get(runStart).getX() - centers.get(runStart - 1).getX());
        int firstDz = Integer.signum(centers.get(runStart).getZ() - centers.get(runStart - 1).getZ());
        for (int edge = runStart + 1; edge <= runEnd + 1 && edge < centers.size(); edge++) {
            int dx = Integer.signum(centers.get(edge).getX() - centers.get(edge - 1).getX());
            int dz = Integer.signum(centers.get(edge).getZ() - centers.get(edge - 1).getZ());
            if (dx == firstDx && dz == firstDz) continue;
            int beforeTurn = edge - runStart;
            int afterTurn = runEnd - edge + 2;
            return beforeTurn >= MIN_BENT_TUNNEL_LEG && afterTurn >= MIN_BENT_TUNNEL_LEG;
        }
        return true;
    }

    private static int tunnelRunCount(List<Integer> profile) {
        int runs = 0;
        boolean inside = false;
        for (int value : profile) {
            boolean tunnel = value == RoadConstructionState.PROFILE_TUNNEL;
            if (tunnel && !inside) runs++;
            inside = tunnel;
        }
        return runs;
    }

    private static int tunnelBendCount(List<BlockPos> centers, List<Integer> profile) {
        int bends = 0;
        int runStart = 0;
        while (runStart < centers.size()) {
            if (runStart >= profile.size() || profile.get(runStart) != RoadConstructionState.PROFILE_TUNNEL) {
                runStart++;
                continue;
            }
            int runEnd = runStart;
            while (runEnd + 1 < centers.size() && runEnd + 1 < profile.size()
                    && profile.get(runEnd + 1) == RoadConstructionState.PROFILE_TUNNEL) runEnd++;
            int count = tunnelTurnCount(centers, runStart, runEnd);
            if (count != Integer.MAX_VALUE) bends += count;
            runStart = runEnd + 1;
        }
        return bends;
    }

    private static List<BlockPos> tunnelPortalFrameAt(List<BlockPos> centers, int index) {
        if (index < 0 || index >= centers.size()) return List.of();
        BlockPos center = centers.get(index);
        int[] direction = directionAt(centers, index);
        List<BlockPos> frame = new ArrayList<>(11);
        for (int side : new int[] {-TUNNEL_PORTAL_HALF_WIDTH, TUNNEL_PORTAL_HALF_WIDTH}) {
            int x = center.getX() - direction[1] * side;
            int z = center.getZ() + direction[0] * side;
            for (int y = 1; y < TUNNEL_PORTAL_HEIGHT; y++) {
                frame.add(new BlockPos(x, center.getY() + y, z));
            }
        }
        for (int side = -TUNNEL_PORTAL_HALF_WIDTH; side <= TUNNEL_PORTAL_HALF_WIDTH; side++) {
            int x = center.getX() - direction[1] * side;
            int z = center.getZ() + direction[0] * side;
            frame.add(new BlockPos(x, center.getY() + TUNNEL_PORTAL_HEIGHT, z));
        }
        return List.copyOf(frame);
    }

    private static List<BlockPos> tunnelPortalFramePositions(List<BlockPos> centers, List<Integer> profile) {
        Set<BlockPos> frames = new LinkedHashSet<>();
        int runStart = 0;
        while (runStart < centers.size()) {
            if (runStart >= profile.size() || profile.get(runStart) != RoadConstructionState.PROFILE_TUNNEL) {
                runStart++;
                continue;
            }
            int runEnd = runStart;
            while (runEnd + 1 < centers.size() && runEnd + 1 < profile.size()
                    && profile.get(runEnd + 1) == RoadConstructionState.PROFILE_TUNNEL) runEnd++;
            frames.addAll(tunnelPortalFrameAt(centers, runStart));
            frames.addAll(tunnelPortalFrameAt(centers, runEnd));
            runStart = runEnd + 1;
        }
        return List.copyOf(frames);
    }

    private static boolean tunnelPortalCellSafe(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) != null) return false;
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().isEmpty()) return false;
        return state.isAir() || state.canBeReplaced() || state.is(Blocks.STONE_BRICKS)
                || isNaturalTunnelPortalBlock(state);
    }

    private static String validateTunnelPortals(ServerLevel level, SettlementData data,
                                                List<BlockPos> centers, List<Integer> profile) {
        for (BlockPos pos : tunnelPortalFramePositions(centers, profile)) {
            if (overlapsBuildingOrOutpost(data, pos)) return "터널 석재 포털이 기존 건물이나 전초기지와 겹칩니다.";
            if (overlapsExistingRoad(data.roads(), pos)) return "터널 석재 포털이 기존 도로와 겹칩니다.";
            if (!tunnelPortalCellSafe(level, pos)) {
                return "터널 석재 포털 범위에 광석·유체·컨테이너·플레이어/비자연 블록이 있습니다.";
            }
        }
        return "";
    }

    private static List<TunnelCell> tunnelExcavationPlan(RoadConstructionState road) {
        List<BlockPos> centers = road.centers();
        List<Integer> profile = road.profile();
        Map<BlockPos, BlockPos> ordered = new LinkedHashMap<>();
        int runStart = 0;
        while (runStart < centers.size()) {
            if (runStart >= profile.size() || profile.get(runStart) != RoadConstructionState.PROFILE_TUNNEL) {
                runStart++;
                continue;
            }
            int runEnd = runStart;
            while (runEnd + 1 < centers.size() && runEnd + 1 < profile.size()
                    && profile.get(runEnd + 1) == RoadConstructionState.PROFILE_TUNNEL) runEnd++;
            BlockPos startWork = runStart > 0 ? centers.get(runStart - 1) : centers.get(runStart);
            for (BlockPos frame : tunnelPortalFrameAt(centers, runStart)) ordered.putIfAbsent(frame, startWork);
            for (int i = runStart; i <= runEnd; i++) {
                BlockPos center = centers.get(i);
                BlockPos work = i > runStart ? centers.get(i - 1) : startWork;
                int[] direction = directionAt(centers, i);
                for (int side : new int[] {0, -1, 1}) {
                    int x = center.getX() - direction[1] * side;
                    int z = center.getZ() + direction[0] * side;
                    for (int y = 1; y <= TUNNEL_CLEAR_HEIGHT; y++) {
                        ordered.putIfAbsent(new BlockPos(x, center.getY() + y, z), work);
                    }
                }
            }
            BlockPos endWork = centers.get(runEnd);
            for (BlockPos frame : tunnelPortalFrameAt(centers, runEnd)) ordered.putIfAbsent(frame, endWork);
            runStart = runEnd + 1;
        }
        List<TunnelCell> cells = new ArrayList<>(ordered.size());
        for (Map.Entry<BlockPos, BlockPos> entry : ordered.entrySet()) {
            cells.add(new TunnelCell(entry.getKey(), entry.getValue()));
        }
        return List.copyOf(cells);
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
                if (natural == null) return SupportPlan.invalid("장교량 지형이 모두 로드된 상태에서 계획해 주세요.");
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
        try {
            return Math.addExact(Math.multiplyExact(dx, dx), Math.multiplyExact(dz, dz));
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
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
        List<BlockPos> tunnelPortals = tunnelPortalFramePositions(centers, road.profile());
        List<Placement> placements = new ArrayList<>(footprints.size() + road.bridgeSupportCount() + tunnelPortals.size());
        for (Map.Entry<BlockPos, FootprintSpec> entry : footprints.entrySet()) {
            FootprintSpec spec = entry.getValue();
            BlockState state;
            if (spec.bridge()) state = Blocks.STONE_BRICKS.defaultBlockState();
            else if (spec.stairFacing() != null) {
                state = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, spec.stairFacing());
            } else state = spec.centerline() ? Blocks.GRAVEL.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState();
            placements.add(new Placement(entry.getKey(), state, spec.bridge(), false, spec.tunnel(), false));
        }
        for (BlockPos support : road.bridgeSupportPositions()) {
            placements.add(new Placement(support, Blocks.STONE_BRICKS.defaultBlockState(), true, true, false, false));
        }
        for (BlockPos portal : tunnelPortals) {
            placements.add(new Placement(portal, Blocks.STONE_BRICKS.defaultBlockState(), false, false, true, true));
        }
        return placements;
    }

    private static Map<BlockPos, FootprintSpec> footprintMap(List<BlockPos> centers, List<Integer> profile) {
        Map<BlockPos, FootprintSpec> footprints = new LinkedHashMap<>();
        for (int i = 0; i < centers.size(); i++) {
            BlockPos center = centers.get(i);
            int[] direction = directionAt(centers, i);
            boolean bridge = i < profile.size() && profile.get(i) == RoadConstructionState.PROFILE_BRIDGE;
            boolean tunnel = i < profile.size() && profile.get(i) == RoadConstructionState.PROFILE_TUNNEL;
            Direction stairFacing = (bridge || tunnel) ? null : stairFacingAt(centers, i);
            for (int side : new int[] {0, -1, 1}) {
                BlockPos pos = new BlockPos(center.getX() - direction[1] * side, center.getY(), center.getZ() + direction[0] * side);
                FootprintSpec incoming = new FootprintSpec(side == 0, bridge, tunnel, stairFacing);
                FootprintSpec existing = footprints.get(pos);
                if (existing == null) footprints.put(pos, incoming);
                else footprints.put(pos, new FootprintSpec(existing.centerline() || incoming.centerline(),
                        existing.bridge() || incoming.bridge(), existing.tunnel() || incoming.tunnel(),
                        existing.stairFacing() != null ? existing.stairFacing() : incoming.stairFacing()));
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

    private static int tunnelCenterCount(List<Integer> profile) {
        int count = 0;
        for (int value : profile) if (value == RoadConstructionState.PROFILE_TUNNEL) count++;
        return count;
    }

    private static int stairCenterCount(List<BlockPos> centers, List<Integer> profile) {
        int count = 0;
        for (int i = 0; i < centers.size(); i++) {
            boolean bridge = i < profile.size() && profile.get(i) == RoadConstructionState.PROFILE_BRIDGE;
            boolean tunnel = i < profile.size() && profile.get(i) == RoadConstructionState.PROFILE_TUNNEL;
            if (!bridge && !tunnel && stairFacingAt(centers, i) != null) count++;
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
