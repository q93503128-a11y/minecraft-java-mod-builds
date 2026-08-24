package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class SettlementOutpostService {
    public static final long WOOD_COST = 72L;
    public static final long STONE_COST = 48L;
    public static final int MAX_TARGET_DISTANCE_FROM_ROAD_END = 8;
    public static final int MAX_PLAYER_DISTANCE_FROM_ROAD_END = 48;
    private static final int MAX_FILL_DEPTH = 2;
    private static final int DIRECT_BLOCK_UPDATE = 2;
    private static final int NORMAL_BLOCK_UPDATE = 3;
    private static final double BUILDER_WORK_RANGE_SQR = 18.0D;
    private static final double STORAGE_INTERACTION_RANGE_SQR = 9.0D;
    private static final double OUTPOST_BUILDER_SEARCH_MARGIN = 96.0D;
    private static final int HAUL_BATCH_SIZE = 16;
    private static final int WORK_INTERVAL_TICKS = 8;

    private SettlementOutpostService() {}

    public record StartResult(boolean started, String message) {}
    public record PlacementCheck(boolean valid, int roadIndex, BlockPos gate,
                                 int directionX, int directionZ,
                                 String specialization, String message) {
        public static PlacementCheck invalid(String message) {
            return new PlacementCheck(false, -1, BlockPos.ZERO, 0, 0, "general", message);
        }
    }

    public static StartResult start(ServerPlayer player) {
        SettlementData data = SettlementData.get(player.level().getServer());
        int roadIndex = latestUnclaimedRoad(data);
        if (roadIndex < 0) return new StartResult(false, "연결되지 않은 완성 도로가 필요합니다.");
        return startAt(player, roadIndex);
    }

    public static PlacementCheck checkPlacement(ServerPlayer player, BlockPos selected) {
        MinecraftServer server = player.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return PlacementCheck.invalid("먼저 공동 마을을 시작해야 합니다.");
        if (player.level() != server.overworld()) return PlacementCheck.invalid("전초기지는 오버월드에만 건설할 수 있습니다.");
        if (data.construction().active() || data.roadConstruction().active() || data.outpostConstruction().active()) {
            return PlacementCheck.invalid("현재 공사가 끝난 뒤 전초기지를 배치해 주세요.");
        }

        int roadIndex = nearestUnclaimedRoad(data, selected);
        if (roadIndex < 0) return PlacementCheck.invalid("사용하지 않은 완성 도로 끝을 가리켜 주세요.");

        RoadSegment road = data.roads().get(roadIndex);
        BlockPos roadEnd = road.end();
        double playerDistance = player.blockPosition().distSqr(roadEnd);
        if (playerDistance > (double) MAX_PLAYER_DISTANCE_FROM_ROAD_END * MAX_PLAYER_DISTANCE_FROM_ROAD_END) {
            return new PlacementCheck(false, roadIndex, gateFor(road), road.directionX(), road.directionZ(),
                    "general", "전초기지를 세울 도로 끝에서 48블록 안으로 이동해 주세요.");
        }

        BlockPos gate = gateFor(road);
        ServerLevel level = server.overworld();
        if (!assessSite(level, data, roadIndex, gate, road.directionX(), road.directionZ())) {
            return new PlacementCheck(false, roadIndex, gate, road.directionX(), road.directionZ(),
                    "general", "도로 끝의 9×9 부지가 안전하지 않습니다. 물·기존 건축물·큰 경사를 피해주세요.");
        }

        BlockPos center = outpostCenter(gate, road.directionX(), road.directionZ());
        SettlementOutpostBiomeService.Bias biomeBias = SettlementOutpostBiomeService.bias(level, center);
        String specialization = detectSpecialization(level, center, data);
        long woodCost = SettlementExplorationBenefitService.outpostWoodCost(data);
        long stoneCost = SettlementExplorationBenefitService.outpostStoneCost(data);
        SettlementService.refreshResources(server, data);
        if (data.resources().wood() < woodCost || data.resources().stone() < stoneCost) {
            return new PlacementCheck(false, roadIndex, gate, road.directionX(), road.directionZ(), specialization,
                    "전초기지 필요 자원: 목재 " + woodCost + " · 석재 " + stoneCost);
        }

        return new PlacementCheck(true, roadIndex, gate, road.directionX(), road.directionZ(), specialization,
                "배치 가능 · " + specializationDisplayName(specialization) + " 후보 · 환경 " + biomeBias.label());
    }

    public static StartResult startAt(ServerPlayer player, int roadIndex) {
        MinecraftServer server = player.level().getServer();
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return new StartResult(false, "먼저 공동 마을을 시작해야 합니다.");
        if (player.level() != server.overworld()) return new StartResult(false, "전초기지는 오버월드에만 건설할 수 있습니다.");
        if (data.construction().active() || data.roadConstruction().active() || data.outpostConstruction().active()) {
            return new StartResult(false, "현재 공사가 끝난 뒤 전초기지를 시작해 주세요.");
        }
        if (roadIndex < 0 || roadIndex >= data.roads().size() || isRoadClaimed(data, roadIndex)) {
            return new StartResult(false, "선택한 도로는 전초기지에 연결할 수 없습니다.");
        }

        RoadSegment road = data.roads().get(roadIndex);
        BlockPos gate = gateFor(road);
        if (player.blockPosition().distSqr(road.end())
                > (double) MAX_PLAYER_DISTANCE_FROM_ROAD_END * MAX_PLAYER_DISTANCE_FROM_ROAD_END) {
            return new StartResult(false, "선택한 도로 끝에서 48블록 안으로 이동해 주세요.");
        }

        ServerLevel level = server.overworld();
        if (!assessSite(level, data, roadIndex, gate, road.directionX(), road.directionZ())) {
            return new StartResult(false, "도로 끝의 전초기지 부지가 더 이상 안전하지 않습니다.");
        }
        if (!SettlementStorageService.storageAvailable(level, data)) {
            return new StartResult(false, "공동 창고가 모두 로드된 상태에서 전초기지를 착공해 주세요. 자원은 차감되지 않았습니다.");
        }

        long woodCost = SettlementExplorationBenefitService.outpostWoodCost(data);
        long stoneCost = SettlementExplorationBenefitService.outpostStoneCost(data);
        SettlementService.refreshResources(server, data);
        if (data.resources().wood() < woodCost || data.resources().stone() < stoneCost) {
            return new StartResult(false, "전초기지 필요 자원: 목재 " + woodCost + " · 석재 " + stoneCost);
        }

        data.beginOutpostConstruction(roadIndex, gate, road.directionX(), road.directionZ());
        data.replaceOutpostConstructionStep(OutpostConstructionState.GRADE_STEP_OFFSET);
        SettlementConstructionService.ensureBuilder(level, data.centerPos());
        SettlementService.broadcast(server, data);
        return new StartResult(true, "전초기지 착공. 건설 주민이 부지를 정리한 뒤 실제 목재·석재를 운반하며 시공합니다."
                + " (탐험 정복 반영 비용: 목재 " + woodCost + " · 석재 " + stoneCost + ")");
    }

    public static boolean tick(MinecraftServer server, SettlementData data) {
        OutpostConstructionState state = data.outpostConstruction();
        if (!state.active()) return false;

        List<OutpostBlueprints.Placement> plan = OutpostBlueprints.create(state);
        if (plan.isEmpty()) {
            data.clearOutpostConstruction();
            return true;
        }

        ServerLevel level = server.overworld();
        Villager builder = findOutpostBuilder(level, data.centerPos(), state, plan);
        if (builder == null) return false;
        if (builder.isNoAi()) builder.setNoAi(false);
        builder.setInvulnerable(true);

        if (state.grading()) return tickGrading(server, data, state, builder);
        if (state.legacyPrepaidBuilding()) return tickLegacyPrepaid(server, data, state, plan, builder);

        int step = state.buildStep();
        if (step >= plan.size()) return finishIfValid(server, data, state, plan, builder);
        return tickPhysicalBuilding(server, data, state, plan, builder);
    }

    private static Villager findOutpostBuilder(ServerLevel level, BlockPos settlementCenter,
                                               OutpostConstructionState state,
                                               List<OutpostBlueprints.Placement> plan) {
        BlockPos hint;
        if (state.grading()) {
            List<BlockPos> footprint = footprint(state);
            int index = Math.max(0, Math.min(footprint.size() - 1, state.gradeStep()));
            hint = footprint.get(index);
        } else {
            int raw = state.physicalBuilding() ? state.buildStep() : state.legacyStep();
            int index = Math.max(0, Math.min(plan.size() - 1, raw));
            hint = plan.get(index).pos();
        }

        double minX = Math.min(settlementCenter.getX(), hint.getX()) - OUTPOST_BUILDER_SEARCH_MARGIN;
        double minY = Math.min(settlementCenter.getY(), hint.getY()) - 64.0D;
        double minZ = Math.min(settlementCenter.getZ(), hint.getZ()) - OUTPOST_BUILDER_SEARCH_MARGIN;
        double maxX = Math.max(settlementCenter.getX(), hint.getX()) + OUTPOST_BUILDER_SEARCH_MARGIN + 1.0D;
        double maxY = Math.max(settlementCenter.getY(), hint.getY()) + 65.0D;
        double maxZ = Math.max(settlementCenter.getZ(), hint.getZ()) + OUTPOST_BUILDER_SEARCH_MARGIN + 1.0D;
        AABB corridor = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        List<Villager> tagged = level.getEntitiesOfClass(Villager.class, corridor,
                villager -> villager.entityTags().contains(SettlementConstructionService.BUILDER_TAG));
        if (!tagged.isEmpty()) return tagged.getFirst();
        return SettlementConstructionService.ensureBuilder(level, settlementCenter);
    }

    private static boolean tickGrading(MinecraftServer server, SettlementData data,
                                       OutpostConstructionState state, Villager builder) {
        List<BlockPos> footprint = footprint(state);
        int step = state.gradeStep();
        if (step >= footprint.size()) {
            data.replaceOutpostConstructionStep(OutpostConstructionState.BUILD_STEP_OFFSET);
            SettlementService.broadcast(server, data);
            return false;
        }

        ServerLevel level = server.overworld();
        BlockPos target = footprint.get(step);
        if (!moveBuilderToCurrentSurface(level, builder, target)) return false;
        if (level.getGameTime() % WORK_INTERVAL_TICKS != 0L) return false;
        if (!canGradeCell(level, target)) {
            builder.getNavigation().stop();
            return false;
        }

        applyGradeCell(level, target);
        builder.swing(InteractionHand.MAIN_HAND);
        data.advanceOutpostConstruction();
        OutpostConstructionState next = data.outpostConstruction();
        if (next.grading() && next.gradeStep() >= footprint.size()) {
            data.replaceOutpostConstructionStep(OutpostConstructionState.BUILD_STEP_OFFSET);
            SettlementService.broadcast(server, data);
        }
        return false;
    }

    private static boolean tickLegacyPrepaid(MinecraftServer server, SettlementData data,
                                             OutpostConstructionState state,
                                             List<OutpostBlueprints.Placement> plan,
                                             Villager builder) {
        int step = state.legacyStep();
        if (step >= plan.size()) return finishIfValid(server, data, state, plan, builder);

        ServerLevel level = server.overworld();
        OutpostBlueprints.Placement placement = plan.get(step);
        if (!moveBuilderToCurrentSurface(level, builder, placement.pos())) return false;
        if (level.getGameTime() % WORK_INTERVAL_TICKS != 0L) return false;

        BlockState current = level.getBlockState(placement.pos());
        if (current.is(placement.state().getBlock())) {
            data.advanceOutpostConstruction();
            return false;
        }
        if (!canReplaceForBlueprint(level, placement.pos(), current)) {
            builder.getNavigation().stop();
            return false;
        }

        if (!level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)) return false;
        builder.swing(InteractionHand.MAIN_HAND);
        data.advanceOutpostConstruction();
        return false;
    }

    private static boolean tickPhysicalBuilding(MinecraftServer server, SettlementData data,
                                                OutpostConstructionState state,
                                                List<OutpostBlueprints.Placement> plan,
                                                Villager builder) {
        int step = state.buildStep();
        OutpostBlueprints.Placement placement = plan.get(step);
        ServerLevel level = server.overworld();

        if (!moveBuilderToCurrentSurface(level, builder, placement.pos())) return false;
        if (level.getGameTime() % WORK_INTERVAL_TICKS != 0L) return false;

        BlockState current = level.getBlockState(placement.pos());
        if (current.is(placement.state().getBlock())) {
            data.advanceOutpostConstruction();
            return false;
        }
        if (!canReplaceForBlueprint(level, placement.pos(), current)) {
            builder.getNavigation().stop();
            return false;
        }

        boolean woodStep = isWoodPlacement(placement.state());
        boolean stoneStep = isStonePlacement(placement.state());
        long requiredNow = 0L;
        long remainingCost = 0L;
        Predicate<ItemStack> predicate = null;
        if (woodStep) {
            long totalWoodCost = SettlementExplorationBenefitService.outpostWoodCost(data);
            requiredNow = materialCostDelta(plan, step, true, totalWoodCost);
            remainingCost = materialRemainingCost(plan, step, true, totalWoodCost);
            predicate = SettlementInventory::isWood;
        } else if (stoneStep) {
            long totalStoneCost = SettlementExplorationBenefitService.outpostStoneCost(data);
            requiredNow = materialCostDelta(plan, step, false, totalStoneCost);
            remainingCost = materialRemainingCost(plan, step, false, totalStoneCost);
            predicate = SettlementInventory::isStone;
        }

        if (predicate != null && !ensureBuildMaterial(server, data, builder, predicate, requiredNow, remainingCost)) {
            return false;
        }

        if (!level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)) return false;
        if (requiredNow > 0L && !consumeCarried(builder, predicate, requiredNow)) {
            level.setBlock(placement.pos(), current, DIRECT_BLOCK_UPDATE);
            return false;
        }
        builder.swing(InteractionHand.MAIN_HAND);
        data.advanceOutpostConstruction();
        if (data.outpostConstruction().buildStep() >= plan.size()) {
            return finishIfValid(server, data, data.outpostConstruction(), plan, builder);
        }
        return false;
    }

    private static boolean ensureBuildMaterial(MinecraftServer server, SettlementData data, Villager builder,
                                               Predicate<ItemStack> predicate,
                                               long requiredNow, long remainingCost) {
        ItemStack carried = builder.getMainHandItem();
        if (!carried.isEmpty() && !predicate.test(carried)) {
            return returnCarriedToStorage(server, data, builder);
        }
        if (requiredNow <= 0L) return true;
        if (!carried.isEmpty() && carried.getCount() >= requiredNow) return true;
        if (!carried.isEmpty()) return returnCarriedToStorage(server, data, builder);

        ServerLevel level = server.overworld();
        BlockPos source = SettlementStorageService.findExtractionTarget(level, data, predicate);
        if (source == null) return false;
        if (builder.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)
                > STORAGE_INTERACTION_RANGE_SQR) {
            builder.getNavigation().moveTo(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D, 0.9D);
            return false;
        }

        int amount = (int) Math.min((long) HAUL_BATCH_SIZE, Math.max(requiredNow, remainingCost));
        ItemStack extracted = SettlementStorageService.extract(level, source, predicate, amount);
        if (extracted.isEmpty()) return false;
        builder.setItemSlot(EquipmentSlot.MAINHAND, extracted);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        return false;
    }

    private static boolean consumeCarried(Villager builder, Predicate<ItemStack> predicate, long amount) {
        if (amount <= 0L) return true;
        ItemStack carried = builder.getMainHandItem();
        if (carried.isEmpty() || !predicate.test(carried) || carried.getCount() < amount) return false;
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

    private static long materialCostDelta(List<OutpostBlueprints.Placement> plan, int step,
                                          boolean wood, long totalCost) {
        if (!matchesMaterial(plan.get(step).state(), wood)) return 0L;
        int total = 0;
        int before = 0;
        for (int i = 0; i < plan.size(); i++) {
            if (!matchesMaterial(plan.get(i).state(), wood)) continue;
            total++;
            if (i < step) before++;
        }
        return costAtStep(totalCost, before + 1, total) - costAtStep(totalCost, before, total);
    }

    private static long materialRemainingCost(List<OutpostBlueprints.Placement> plan, int step,
                                              boolean wood, long totalCost) {
        int total = 0;
        int before = 0;
        for (int i = 0; i < plan.size(); i++) {
            if (!matchesMaterial(plan.get(i).state(), wood)) continue;
            total++;
            if (i < step) before++;
        }
        return Math.max(0L, totalCost - costAtStep(totalCost, before, total));
    }

    private static boolean matchesMaterial(BlockState state, boolean wood) {
        return wood ? isWoodPlacement(state) : isStonePlacement(state);
    }

    private static boolean isWoodPlacement(BlockState state) {
        return state.is(Blocks.OAK_FENCE)
                || state.is(Blocks.SPRUCE_PLANKS)
                || state.is(Blocks.STRIPPED_SPRUCE_LOG)
                || state.is(Blocks.SPRUCE_SLAB)
                || state.is(Blocks.BARREL)
                || state.is(Blocks.CRAFTING_TABLE);
    }

    private static boolean isStonePlacement(BlockState state) {
        return state.is(Blocks.COBBLESTONE);
    }

    private static long costAtStep(long totalCost, int step, int totalSteps) {
        if (totalCost <= 0L || step <= 0 || totalSteps <= 0) return 0L;
        if (step >= totalSteps) return totalCost;
        return totalCost * step / totalSteps;
    }

    private static boolean finishIfValid(MinecraftServer server, SettlementData data,
                                         OutpostConstructionState state,
                                         List<OutpostBlueprints.Placement> plan,
                                         Villager builder) {
        ServerLevel level = server.overworld();
        boolean legacyPrepaidRepair = state.legacyPrepaidBuilding();
        for (OutpostBlueprints.Placement placement : plan) {
            BlockState current = level.getBlockState(placement.pos());
            if (current.is(placement.state().getBlock())) continue;
            if (!canReplaceForBlueprint(level, placement.pos(), current)) {
                builder.getNavigation().stop();
                return false;
            }
            Predicate<ItemStack> repairPredicate = isWoodPlacement(placement.state()) ? SettlementInventory::isWood
                    : isStonePlacement(placement.state()) ? SettlementInventory::isStone : null;
            if (!legacyPrepaidRepair && repairPredicate != null
                    && !ensureBuildMaterial(server, data, builder, repairPredicate, 1L, 1L)) return false;
            if (!moveBuilderToCurrentSurface(level, builder, placement.pos())) return false;
            if (level.getGameTime() % WORK_INTERVAL_TICKS != 0L) return false;
            if (!level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)) return false;
            if (!legacyPrepaidRepair && repairPredicate != null && !consumeCarried(builder, repairPredicate, 1L)) {
                level.setBlock(placement.pos(), current, DIRECT_BLOCK_UPDATE);
                return false;
            }
            builder.swing(InteractionHand.MAIN_HAND);
            return false;
        }

        BlockPos stockpile = OutpostBlueprints.stockpile(state);
        if (!(level.getBlockEntity(stockpile) instanceof Container)) return false;
        if (!returnCarriedToStorage(server, data, builder)) return false;

        BlockPos center = OutpostBlueprints.center(state);
        String specialization = detectSpecialization(level, center, data);
        OutpostRecord outpost = new OutpostRecord(
                data.outposts().size() + 1,
                center.getX(), center.getY(), center.getZ(),
                stockpile.getX(), stockpile.getY(), stockpile.getZ(),
                state.roadIndex(), specialization);
        data.completeOutpost(outpost);
        builder.getNavigation().stop();
        builder.setInvulnerable(false);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        return true;
    }

    public static int totalSteps(OutpostConstructionState state) {
        return state.active() ? OutpostBlueprints.create(state).size() : 0;
    }

    public static String phaseLabel(OutpostConstructionState state) {
        if (state == null || !state.active()) return "전초기지 건설";
        if (state.grading()) return "전초기지 부지 정리";
        if (state.physicalBuilding()) return "전초기지 자재 운반·시공";
        if (state.legacyPrepaidBuilding()) return "전초기지 기존 공사 마무리";
        return "전초기지 마감 확인";
    }

    public static void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (level != server.overworld()) return;

        SettlementData data = SettlementData.get(server);
        OutpostConstructionState state = data.outpostConstruction();
        if (!state.active()) return;

        BlockPos pos = event.getPos();
        BlockState current = level.getBlockState(pos);
        List<OutpostBlueprints.Placement> plan = OutpostBlueprints.create(state);
        int builtCount;
        if (state.physicalBuilding()) builtCount = Math.max(0, Math.min(plan.size(), state.buildStep()));
        else if (state.legacyPrepaidBuilding()) builtCount = Math.max(0, Math.min(plan.size(), state.legacyStep()));
        else builtCount = 0;

        for (int i = 0; i < builtCount; i++) {
            OutpostBlueprints.Placement placement = plan.get(i);
            if (placement.pos().equals(pos) && current.is(placement.state().getBlock())) {
                event.setCanceled(true);
                event.setNotifyClient(true);
                return;
            }
        }

        List<BlockPos> footprint = footprint(state);
        int gradedCount = state.grading()
                ? Math.max(0, Math.min(footprint.size(), state.gradeStep()))
                : footprint.size();
        for (int i = 0; i < gradedCount; i++) {
            BlockPos floor = footprint.get(i);
            if (floor.getX() != pos.getX() || floor.getZ() != pos.getZ()) continue;
            if (pos.getY() <= floor.getY() && pos.getY() >= floor.getY() - MAX_FILL_DEPTH - 1
                    && current.is(Blocks.COARSE_DIRT)) {
                event.setCanceled(true);
                event.setNotifyClient(true);
                return;
            }
        }
    }

    private static List<BlockPos> footprint(OutpostConstructionState state) {
        List<BlockPos> positions = new ArrayList<>(OutpostBlueprints.LENGTH * OutpostBlueprints.WIDTH);
        for (int forward = 0; forward < OutpostBlueprints.LENGTH; forward++) {
            for (int side = -4; side <= 4; side++) {
                positions.add(OutpostBlueprints.local(state, forward, side, 0));
            }
        }
        return positions;
    }

    private static boolean moveBuilderToCurrentSurface(ServerLevel level, Villager builder, BlockPos target) {
        int workY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
        BlockPos work = new BlockPos(target.getX(), workY, target.getZ());
        double distance = builder.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D);
        if (distance <= BUILDER_WORK_RANGE_SQR) return true;
        builder.getNavigation().moveTo(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D, 0.85D);
        return false;
    }

    private static boolean canGradeCell(ServerLevel level, BlockPos target) {
        for (int y = target.getY(); y <= target.getY() + OutpostBlueprints.CLEAR_HEIGHT; y++) {
            BlockPos pos = new BlockPos(target.getX(), y, target.getZ());
            BlockState state = level.getBlockState(pos);
            if (level.getBlockEntity(pos) != null || !state.getFluidState().isEmpty() || state.is(BlockTags.LOGS)) {
                return false;
            }
            boolean naturalSlope = y <= target.getY() + 1 && isNaturalGround(state);
            if (!state.isAir() && !state.canBeReplaced() && !state.is(BlockTags.LEAVES) && !naturalSlope) {
                return false;
            }
        }
        return hasOrCanMakeSupport(level, target.below());
    }

    private static void applyGradeCell(ServerLevel level, BlockPos target) {
        for (int y = target.getY() + OutpostBlueprints.CLEAR_HEIGHT; y >= target.getY() + 1; y--) {
            BlockPos pos = new BlockPos(target.getX(), y, target.getZ());
            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE);
            }
        }

        BlockPos cursor = target.below();
        for (int depth = 0; depth <= MAX_FILL_DEPTH; depth++) {
            BlockState current = level.getBlockState(cursor);
            if (!current.isAir() && !current.canBeReplaced()) break;
            level.setBlock(cursor, Blocks.COARSE_DIRT.defaultBlockState(), DIRECT_BLOCK_UPDATE);
            cursor = cursor.below();
        }

        BlockState current = level.getBlockState(target);
        if (current.isAir() || current.canBeReplaced()) {
            level.setBlock(target, Blocks.COARSE_DIRT.defaultBlockState(), DIRECT_BLOCK_UPDATE);
        }
    }

    private static boolean hasOrCanMakeSupport(ServerLevel level, BlockPos support) {
        BlockPos cursor = support;
        for (int depth = 0; depth <= MAX_FILL_DEPTH; depth++) {
            BlockState state = level.getBlockState(cursor);
            if (level.getBlockEntity(cursor) != null || !state.getFluidState().isEmpty()) return false;
            if (!state.isAir() && !state.canBeReplaced()) return true;
            cursor = cursor.below();
        }
        return false;
    }

    private static boolean canReplaceForBlueprint(ServerLevel level, BlockPos pos, BlockState current) {
        if (level.getBlockEntity(pos) != null || !current.getFluidState().isEmpty()) return false;
        if (current.isAir() || current.canBeReplaced()) return true;
        return pos.getY() == level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) - 1
                && isNaturalGround(current);
    }

    private static BlockPos gateFor(RoadSegment road) {
        return road.end().offset(road.directionX(), 0, road.directionZ());
    }

    private static BlockPos outpostCenter(BlockPos gate, int directionX, int directionZ) {
        return gate.offset(directionX * 4, 0, directionZ * 4);
    }

    private static String detectSpecialization(ServerLevel level, BlockPos center) {
        return detectSpecialization(level, center, null);
    }

    private static String detectSpecialization(ServerLevel level, BlockPos center, SettlementData data) {
        int ores = 0;
        int logs = 0;
        int fieldGround = 0;
        int exposedStone = 0;
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                if (dx * dx + dz * dz > 144) continue;
                for (int dy = -12; dy <= 8; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Tags.Blocks.ORES)) ores++;
                    if (dy >= -2 && dy <= 7 && state.is(BlockTags.LOGS)) logs++;
                    if (dy >= -1 && dy <= 1 && (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT))) fieldGround++;
                    if (dy >= -3 && dy <= 3 && isStone(state) && level.getBlockState(pos.above()).isAir()) exposedStone++;
                }
            }
        }
        if (data != null) {
            ores += SettlementExplorationBenefitService.oreEvidenceBonus(data);
            logs += SettlementExplorationBenefitService.logEvidenceBonus(data);
            fieldGround += SettlementExplorationBenefitService.fieldEvidenceBonus(data);
            exposedStone += SettlementExplorationBenefitService.stoneEvidenceBonus(data);
        }
        SettlementOutpostBiomeService.Bias biomeBias = SettlementOutpostBiomeService.bias(level, center);
        ores += biomeBias.ore();
        logs += biomeBias.logs();
        fieldGround += biomeBias.field();
        exposedStone += biomeBias.stone();
        if (ores >= 4) return "mining";
        if (logs >= 24) return "lumber";
        if (fieldGround >= 120) return "agriculture";
        if (exposedStone >= 24) return "quarry";
        return "general";
    }

    public static String specializationDisplayName(String specialization) {
        return switch (specialization) {
            case "mining" -> "광업";
            case "lumber" -> "벌목";
            case "agriculture" -> "농업";
            case "quarry" -> "채석";
            default -> "일반";
        };
    }

    private static boolean isStone(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE) || state.is(Blocks.TUFF);
    }

    private static int latestUnclaimedRoad(SettlementData data) {
        for (int i = data.roads().size() - 1; i >= 0; i--) if (!isRoadClaimed(data, i)) return i;
        return -1;
    }

    private static int nearestUnclaimedRoad(SettlementData data, BlockPos selected) {
        double bestDistance = (double) MAX_TARGET_DISTANCE_FROM_ROAD_END * MAX_TARGET_DISTANCE_FROM_ROAD_END + 1.0D;
        int bestIndex = -1;
        for (int i = 0; i < data.roads().size(); i++) {
            if (isRoadClaimed(data, i)) continue;
            double distance = selected.distSqr(data.roads().get(i).end());
            if (distance <= (double) MAX_TARGET_DISTANCE_FROM_ROAD_END * MAX_TARGET_DISTANCE_FROM_ROAD_END
                    && distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static boolean isRoadClaimed(SettlementData data, int roadIndex) {
        return data.outposts().stream().anyMatch(outpost -> outpost.roadIndex() == roadIndex);
    }

    private static boolean assessSite(ServerLevel level, SettlementData data, int roadIndex,
                                      BlockPos gate, int directionX, int directionZ) {
        int roadY = gate.getY();
        for (int forward = 0; forward < OutpostBlueprints.LENGTH; forward++) {
            for (int side = -4; side <= 4; side++) {
                int x = gate.getX() + directionX * forward - directionZ * side;
                int z = gate.getZ() + directionZ * forward + directionX * side;
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                if (Math.abs(surfaceY - roadY) > 1) return false;
                BlockPos surface = new BlockPos(x, surfaceY, z);
                BlockState ground = level.getBlockState(surface);
                if (level.getBlockEntity(surface) != null || !ground.getFluidState().isEmpty() || !isNaturalGround(ground)) {
                    return false;
                }
                BlockPos footprint = new BlockPos(x, roadY, z);
                if (overlapsProtectedInfrastructure(data, roadIndex, footprint)) return false;
                for (int y = roadY; y <= roadY + OutpostBlueprints.CLEAR_HEIGHT; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockEntity(pos) != null) return false;
                    BlockState state = level.getBlockState(pos);
                    if (!state.getFluidState().isEmpty() || state.is(BlockTags.LOGS)) return false;
                    if (!state.isAir() && !state.canBeReplaced() && !state.is(BlockTags.LEAVES)
                            && !(y <= roadY + 1 && isNaturalGround(state))) return false;
                }
                if (!hasOrCanMakeSupport(level, footprint.below())) return false;
            }
        }
        return true;
    }

    private static boolean overlapsProtectedInfrastructure(SettlementData data, int connectedRoadIndex, BlockPos pos) {
        for (BuildingRecord building : data.buildings()) if (building.protectsXZ(pos, 1)) return true;
        for (OutpostRecord outpost : data.outposts()) if (outpost.protectsXZ(pos, 1)) return true;
        for (int i = 0; i < data.roads().size(); i++) {
            if (i == connectedRoadIndex) continue;
            if (data.roads().get(i).containsXZ(pos)) return true;
        }
        return false;
    }

    private static boolean isNaturalGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.STONE)
                || state.is(Blocks.ANDESITE) || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE)
                || state.is(Blocks.TUFF) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY) || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK);
    }
}
