package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.content.FrontierContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import kr.moonseungjun.frontiersettlement.content.FrontierWorkerEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Physical local production for specialized outposts.
 *
 * Alpha.28 keeps the specializations deliberately different:
 * - lumber and quarry consume nearby visible world resources;
 * - mining consumes finite underground ore while the worker performs readable work at the minehead;
 * - agriculture is renewable through vanilla crop growth, but its plot is established only once;
 * - unloaded outposts and resource chunks are never force-loaded for simulation.
 *
 * Alpha.42 may let a reloaded worker redeem bounded deferred work-time, but only after the same
 * physical harvest succeeds. Deferred state is never wood/stone/ore/food authority.
 */
public final class SettlementOutpostProductionService {
    public static final String PRODUCTION_WORKER_TAG = "frontier_settlement_outpost_production_worker";
    public static final String PRODUCTION_OUTPOST_TAG_PREFIX = "frontier_settlement_outpost_production_";

    private static final int TREE_RADIUS = 18;
    private static final int QUARRY_RADIUS = 16;
    private static final int MANAGED_QUARRY_FACE_RADIUS = 14;
    private static final int MANAGED_QUARRY_MAX_OVERBURDEN = 4;
    private static final int MAX_LOGS = 8;
    private static final int MAX_STONE = 8;
    private static final int MAX_CROPS = 4;
    private static final int LUMBER_WORK_PERIOD_TICKS = 100;
    private static final int QUARRY_WORK_PERIOD_TICKS = 80;
    private static final int MINING_WORK_PERIOD_TICKS = 160;
    private static final int AGRICULTURE_WORK_PERIOD_TICKS = 120;
    // Specialized workers are stable persistent entities. Resolve the known UUID on the hot path and
    // rescan the wide 97x65x97 assignment envelope only for cache misses/deaths or periodic duplicate
    // maintenance. Work cadence remains 10 ticks, so production and navigation responsiveness do not change.
    private static final int WORKER_MAINTENANCE_INTERVAL_TICKS = 200;
    private static final int TARGET_RESCAN_DELAY_TICKS = 100;
    private static final Map<Integer, UUID> WORKER_UUID_CACHE = new HashMap<>();
    // Lumber/quarry targets are physical world coordinates, not resource authority. Reuse a still-valid
    // target while a worker walks or waits for its production cadence instead of rescanning thousands of
    // blocks every ten ticks. Empty searches back off for five seconds; invalidated cached targets retry
    // immediately so player/world changes remain responsive without shrinking the authored work radius.
    private static final Map<Integer, BlockPos> LUMBER_TARGET_CACHE = new HashMap<>();
    private static final Map<Integer, BlockPos> QUARRY_TARGET_CACHE = new HashMap<>();
    private static final Map<Integer, Long> LUMBER_TARGET_RETRY_AFTER = new HashMap<>();
    private static final Map<Integer, Long> QUARRY_TARGET_RETRY_AFTER = new HashMap<>();

    private SettlementOutpostProductionService() {}

    public static void tick(MinecraftServer server, SettlementData data) {
        int tick = server.getTickCount();
        if (tick % 10 != 0) return;
        ServerLevel level = server.overworld();
        boolean maintenance = tick % WORKER_MAINTENANCE_INTERVAL_TICKS == 0;
        Set<Integer> liveSpecializedOutposts = maintenance ? new HashSet<>() : Set.of();
        Set<Integer> liveLumberOutposts = maintenance ? new HashSet<>() : Set.of();
        Set<Integer> liveQuarryOutposts = maintenance ? new HashSet<>() : Set.of();
        for (OutpostRecord outpost : data.outposts()) {
            if ("general".equals(outpost.specialization())) continue;
            if (maintenance) {
                liveSpecializedOutposts.add(outpost.id());
                if ("lumber".equals(outpost.specialization())) liveLumberOutposts.add(outpost.id());
                if ("quarry".equals(outpost.specialization())) liveQuarryOutposts.add(outpost.id());
            }
            if (!outpostLoaded(level, outpost)) continue;
            if ("agriculture".equals(outpost.specialization()) && pristineLegacyAgriculturePlot(level, data, outpost)) {
                initializeSpecializationSite(level, data, outpost);
            }
            FrontierWorkerEntity worker = resolveCachedWorker(level, outpost);
            if (worker == null || maintenance) worker = ensureWorker(level, outpost);
            if (worker != null) work(level, data, outpost, worker);
        }
        if (maintenance) {
            WORKER_UUID_CACHE.keySet().removeIf(id -> !liveSpecializedOutposts.contains(id));
            LUMBER_TARGET_CACHE.keySet().removeIf(id -> !liveLumberOutposts.contains(id));
            LUMBER_TARGET_RETRY_AFTER.keySet().removeIf(id -> !liveLumberOutposts.contains(id));
            QUARRY_TARGET_CACHE.keySet().removeIf(id -> !liveQuarryOutposts.contains(id));
            QUARRY_TARGET_RETRY_AFTER.keySet().removeIf(id -> !liveQuarryOutposts.contains(id));
        }
    }

    /** Called when a new specialized outpost becomes authoritative. Only agriculture needs a fixed worksite. */
    public static void initializeSpecializationSite(ServerLevel level, SettlementData data, OutpostRecord outpost) {
        if (!"agriculture".equals(outpost.specialization()) || !outpostLoaded(level, outpost)) return;
        initializeAgriculturePlot(level, data, outpost);
    }

    private static boolean outpostLoaded(ServerLevel level, OutpostRecord outpost) {
        return level.hasChunkAt(outpost.center()) && level.hasChunkAt(outpost.stockpile());
    }

    private static FrontierWorkerEntity resolveCachedWorker(ServerLevel level, OutpostRecord outpost) {
        UUID uuid = WORKER_UUID_CACHE.get(outpost.id());
        if (uuid == null) return null;
        if (!(level.getEntity(uuid) instanceof FrontierWorkerEntity worker)
                || !worker.isAlive()
                || !worker.entityTags().contains(PRODUCTION_WORKER_TAG)
                || !worker.entityTags().contains(productionTag(outpost.id()))
                || !assignmentBounds(outpost).contains(worker.getX(), worker.getY(), worker.getZ())) {
            WORKER_UUID_CACHE.remove(outpost.id());
            return null;
        }
        worker.setNoAi(false);
        worker.setInvulnerable(false);
        return worker;
    }

    private static FrontierWorkerEntity rememberWorker(OutpostRecord outpost, FrontierWorkerEntity worker) {
        WORKER_UUID_CACHE.put(outpost.id(), worker.getUUID());
        return worker;
    }

    private static FrontierWorkerEntity ensureWorker(ServerLevel level, OutpostRecord outpost) {
        if (!outpostLoaded(level, outpost)) return null;
        String assignmentTag = productionTag(outpost.id());
        String name = workerName(outpost);
        List<FrontierWorkerEntity> assigned = findAssignedWorkers(level, outpost);
        List<FrontierWorkerEntity> legacy = findLegacyWorkers(level, outpost, name, assignmentTag);

        if (!assigned.isEmpty()) {
            FrontierWorkerEntity active = assigned.getFirst();
            active.setNoAi(false);
            active.setInvulnerable(false);
            // One specialized outpost owns one local production authority. More than one loaded body
            // with the same assignment is conclusive duplicate evidence even when the wider envelope
            // is not fully loaded. Preserve physical cargo, then discard every excess body.
            for (int i = 1; i < assigned.size(); i++) {
                SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i));
            }
            // Pre-tag migration remnants use a unique name containing this outpost id. Once a tagged
            // authority is visible, any loaded same-name unassigned body is also definitively excess.
            for (FrontierWorkerEntity duplicate : legacy) {
                SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, duplicate);
            }
            return rememberWorker(outpost, active);
        }

        // Missing is authority. Do not migrate or spawn from a partial entity view.
        if (!assignmentEvidenceLoaded(level, outpost)) return null;

        if (!legacy.isEmpty()) {
            FrontierWorkerEntity active = legacy.getFirst();
            active.setNoAi(false);
            active.setInvulnerable(false);
            active.addTag(PRODUCTION_WORKER_TAG);
            active.addTag(assignmentTag);
            for (int i = 1; i < legacy.size(); i++) {
                SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, legacy.get(i));
            }
            return rememberWorker(outpost, active);
        }

        FrontierWorkerEntity worker = new FrontierWorkerEntity(FrontierContent.FRONTIER_WORKER.get(), level);
        BlockPos spawn = outpost.center().above();
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setCustomName(Component.literal(name));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        worker.setInvulnerable(false);
        worker.addTag(PRODUCTION_WORKER_TAG);
        worker.addTag(assignmentTag);
        if (!level.addFreshEntity(worker)) return null;
        return rememberWorker(outpost, worker);
    }

    private static List<FrontierWorkerEntity> findLegacyWorkers(ServerLevel level, OutpostRecord outpost,
                                                                 String name, String assignmentTag) {
        List<FrontierWorkerEntity> legacy = level.getEntitiesOfClass(FrontierWorkerEntity.class, assignmentBounds(outpost),
                villager -> villager.getCustomName() != null
                        && name.equals(villager.getCustomName().getString())
                        && !villager.entityTags().contains(assignmentTag));
        legacy.sort(Comparator
                .comparingInt((FrontierWorkerEntity worker) -> worker.getMainHandItem().isEmpty() ? 1 : 0)
                .thenComparing(worker -> worker.getUUID().toString()));
        return legacy;
    }

    private static List<FrontierWorkerEntity> findAssignedWorkers(ServerLevel level, OutpostRecord outpost) {
        String assignmentTag = productionTag(outpost.id());
        List<FrontierWorkerEntity> assigned = level.getEntitiesOfClass(FrontierWorkerEntity.class, assignmentBounds(outpost),
                villager -> villager.entityTags().contains(PRODUCTION_WORKER_TAG)
                        && villager.entityTags().contains(assignmentTag));
        assigned.sort(Comparator
                .comparingInt((FrontierWorkerEntity worker) -> worker.getMainHandItem().isEmpty() ? 1 : 0)
                .thenComparing(worker -> worker.getUUID().toString()));
        return assigned;
    }

    private static AABB assignmentBounds(OutpostRecord outpost) {
        return new AABB(
                outpost.centerX() - 48.0D, outpost.centerY() - 32.0D, outpost.centerZ() - 48.0D,
                outpost.centerX() + 49.0D, outpost.centerY() + 33.0D, outpost.centerZ() + 49.0D);
    }

    private static boolean assignmentEvidenceLoaded(ServerLevel level, OutpostRecord outpost) {
        if (!outpostLoaded(level, outpost)) return false;
        AABB bounds = assignmentBounds(outpost);
        int minChunkX = Math.floorDiv((int) Math.floor(bounds.minX), 16);
        int maxChunkX = Math.floorDiv((int) Math.floor(Math.nextDown(bounds.maxX)), 16);
        int minChunkZ = Math.floorDiv((int) Math.floor(bounds.minZ), 16);
        int maxChunkZ = Math.floorDiv((int) Math.floor(Math.nextDown(bounds.maxZ)), 16);
        int probeY = outpost.centerY();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                BlockPos probe = new BlockPos(chunkX * 16 + 8, probeY, chunkZ * 16 + 8);
                if (!level.hasChunkAt(probe)) return false;
            }
        }
        return true;
    }

    private static String productionTag(int outpostId) {
        return PRODUCTION_OUTPOST_TAG_PREFIX + outpostId;
    }

    private static String workerName(OutpostRecord outpost) {
        String role = switch (outpost.specialization()) {
            case "lumber" -> "벌목";
            case "quarry" -> "채석";
            case "mining" -> "광산";
            case "agriculture" -> "농업";
            default -> "작업";
        };
        return "전초 " + role + " 주민 #" + outpost.id();
    }

    private static void work(ServerLevel level, SettlementData data, OutpostRecord outpost, FrontierWorkerEntity worker) {
        ItemStack carried = worker.getMainHandItem();
        if (!carried.isEmpty()) {
            deliver(level, outpost, worker, carried);
            return;
        }

        switch (outpost.specialization()) {
            case "lumber" -> workLumber(level, data, outpost, worker);
            case "quarry" -> workQuarry(level, data, outpost, worker);
            case "mining" -> workMine(level, data, outpost, worker);
            case "agriculture" -> workAgriculture(level, data, outpost, worker);
            default -> move(worker, outpost.center().above(), 0.65D);
        }
    }

    private static void deliver(ServerLevel level, OutpostRecord outpost, FrontierWorkerEntity worker, ItemStack carried) {
        BlockPos stock = outpost.stockpile();
        if (!level.hasChunkAt(stock)) {
            worker.getNavigation().stop();
            return;
        }
        if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D) > 9.0D) {
            SettlementWorkerStorageNavigation.moveToInteraction(level, worker, stock, 0.82D, 9.0D);
            return;
        }
        if (!(level.getBlockEntity(stock) instanceof Container container)) return;
        worker.setItemSlot(EquipmentSlot.MAINHAND, SettlementInventory.insert(container, carried));
    }

    private static void workLumber(ServerLevel level, SettlementData data, OutpostRecord outpost, FrontierWorkerEntity worker) {
        BlockPos target = resolveLumberTarget(level, data, outpost);
        if (target == null) {
            move(worker, outpost.center().above(), 0.65D);
            return;
        }
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > 8.0D) {
            SettlementWorkerStorageNavigation.moveToInteraction(level, worker, target, 0.8D, 8.0D);
            return;
        }
        if (!workDue(level, outpost, LUMBER_WORK_PERIOD_TICKS)) return;
        // Natural-leaf evidence is expensive, so confirm it only when a harvest is actually due rather
        // than on every navigation tick. The full search required the same evidence when caching target.
        if (!hasLeavesAbove(level, target)) {
            invalidateLumberTarget(outpost.id());
            return;
        }
        ItemStack harvested = harvestVerticalTrunk(level, data, target);
        invalidateLumberTarget(outpost.id());
        if (!harvested.isEmpty()) {
            worker.swing(InteractionHand.MAIN_HAND);
            worker.setItemSlot(EquipmentSlot.MAINHAND, harvested);
            SettlementDeferredOutpostService.consumeProductionCredit(level.getServer(), outpost, LUMBER_WORK_PERIOD_TICKS);
        }
    }

    private static void workQuarry(ServerLevel level, SettlementData data, OutpostRecord outpost, FrontierWorkerEntity worker) {
        BlockPos target = resolveQuarryTarget(level, data, outpost);
        if (target == null) {
            move(worker, outpost.center().above(), 0.65D);
            return;
        }
        BlockPos approach = quarryApproach(level, data, target);
        if (approach == null) {
            invalidateQuarryTarget(outpost.id());
            return;
        }
        if (worker.distanceToSqr(approach.getX() + 0.5D, approach.getY(), approach.getZ() + 0.5D) > 9.0D) {
            SettlementWorkerStorageNavigation.moveToInteraction(level, worker, approach, 0.78D, 9.0D);
            return;
        }
        if (!workDue(level, outpost, QUARRY_WORK_PERIOD_TICKS)) return;
        if (!level.getBlockState(target.above()).isAir()) {
            if (clearTopQuarryOverburden(level, data, target)) {
                worker.swing(InteractionHand.MAIN_HAND);
            } else {
                invalidateQuarryTarget(outpost.id());
            }
            return;
        }
        ItemStack harvested = harvestStoneCluster(level, data, target);
        invalidateQuarryTarget(outpost.id());
        if (!harvested.isEmpty()) {
            worker.swing(InteractionHand.MAIN_HAND);
            worker.setItemSlot(EquipmentSlot.MAINHAND, harvested);
            SettlementDeferredOutpostService.consumeProductionCredit(level.getServer(), outpost, QUARRY_WORK_PERIOD_TICKS);
        }
    }

    private static BlockPos resolveLumberTarget(ServerLevel level, SettlementData data, OutpostRecord outpost) {
        int id = outpost.id();
        BlockPos cached = LUMBER_TARGET_CACHE.get(id);
        if (cached != null) {
            if (validCachedLumberTarget(level, data, outpost, cached)) return cached;
            invalidateLumberTarget(id);
        }
        long now = level.getGameTime();
        Long retryAfter = LUMBER_TARGET_RETRY_AFTER.get(id);
        if (retryAfter != null && now < retryAfter) return null;
        BlockPos found = findTree(level, data, outpost.center(), TREE_RADIUS);
        if (found == null) {
            LUMBER_TARGET_RETRY_AFTER.put(id, now + TARGET_RESCAN_DELAY_TICKS);
            return null;
        }
        LUMBER_TARGET_CACHE.put(id, found);
        LUMBER_TARGET_RETRY_AFTER.remove(id);
        return found;
    }

    private static boolean validCachedLumberTarget(ServerLevel level, SettlementData data,
                                                    OutpostRecord outpost, BlockPos target) {
        if (!withinTargetEnvelope(outpost.center(), target, TREE_RADIUS, -4, 10) || !level.hasChunkAt(target)) return false;
        return level.getBlockState(target).is(BlockTags.LOGS) && !isProtected(data, target);
    }

    private static void invalidateLumberTarget(int outpostId) {
        LUMBER_TARGET_CACHE.remove(outpostId);
        LUMBER_TARGET_RETRY_AFTER.remove(outpostId);
    }

    private static BlockPos resolveQuarryTarget(ServerLevel level, SettlementData data, OutpostRecord outpost) {
        int id = outpost.id();
        BlockPos cached = QUARRY_TARGET_CACHE.get(id);
        if (cached != null) {
            if (validCachedQuarryTarget(level, data, outpost, cached)) return cached;
            invalidateQuarryTarget(id);
        }
        long now = level.getGameTime();
        Long retryAfter = QUARRY_TARGET_RETRY_AFTER.get(id);
        if (retryAfter != null && now < retryAfter) return null;
        BlockPos found = findExposedStone(level, data, outpost.center(), QUARRY_RADIUS);
        if (found == null) found = findManagedQuarryStone(level, data, outpost.center());
        if (found == null) {
            QUARRY_TARGET_RETRY_AFTER.put(id, now + TARGET_RESCAN_DELAY_TICKS);
            return null;
        }
        QUARRY_TARGET_CACHE.put(id, found);
        QUARRY_TARGET_RETRY_AFTER.remove(id);
        return found;
    }

    private static boolean validCachedQuarryTarget(ServerLevel level, SettlementData data,
                                                    OutpostRecord outpost, BlockPos target) {
        if (!withinTargetEnvelope(outpost.center(), target, QUARRY_RADIUS, -7, 4) || !level.hasChunkAt(target)) return false;
        return isQuarryStone(level.getBlockState(target)) && !isProtected(data, target);
    }

    private static void invalidateQuarryTarget(int outpostId) {
        QUARRY_TARGET_CACHE.remove(outpostId);
        QUARRY_TARGET_RETRY_AFTER.remove(outpostId);
    }

    private static boolean withinTargetEnvelope(BlockPos center, BlockPos target, int radius, int minY, int maxY) {
        int dx = target.getX() - center.getX();
        int dz = target.getZ() - center.getZ();
        int dy = target.getY() - center.getY();
        return dx * dx + dz * dz <= radius * radius && dy >= minY && dy <= maxY;
    }

    private static void workMine(ServerLevel level, SettlementData data, OutpostRecord outpost, FrontierWorkerEntity worker) {
        BlockPos minehead = outpost.center().above();
        if (worker.distanceToSqr(minehead.getX() + 0.5D, minehead.getY(), minehead.getZ() + 0.5D) > 9.0D) {
            move(worker, minehead, 0.72D);
            return;
        }
        if (!workDue(level, outpost, MINING_WORK_PERIOD_TICKS)) return;
        BlockPos ore = findOreBelow(level, data, outpost.center());
        if (ore == null) return;
        ItemStack mined = mineOre(level, ore);
        if (!mined.isEmpty()) {
            worker.swing(InteractionHand.MAIN_HAND);
            worker.setItemSlot(EquipmentSlot.MAINHAND, mined);
            SettlementDeferredOutpostService.consumeProductionCredit(level.getServer(), outpost, MINING_WORK_PERIOD_TICKS);
        }
    }

    private static void workAgriculture(ServerLevel level, SettlementData data, OutpostRecord outpost, FrontierWorkerEntity worker) {
        BlockPos firstMature = findMatureCrop(level, data, outpost);
        if (firstMature == null) {
            move(worker, outpost.center().above(), 0.6D);
            return;
        }
        if (worker.distanceToSqr(firstMature.getX() + 0.5D, firstMature.getY(), firstMature.getZ() + 0.5D) > 9.0D) {
            move(worker, firstMature, 0.72D);
            return;
        }
        if (!workDue(level, outpost, AGRICULTURE_WORK_PERIOD_TICKS)) return;

        int harvested = 0;
        for (int forward = 1; forward <= 3 && harvested < MAX_CROPS; forward++) {
            for (int side : new int[] {-2, -1, 1, 2}) {
                BlockPos crop = local(data, outpost, forward, side, 1);
                if (!level.hasChunkAt(crop)) continue;
                BlockState state = level.getBlockState(crop);
                if (!isMatureWheat(state)) continue;
                if (worker.distanceToSqr(crop.getX() + 0.5D, crop.getY(), crop.getZ() + 0.5D) > 9.0D) continue;
                if (level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3)) harvested++;
                if (harvested >= MAX_CROPS) break;
            }
        }
        if (harvested > 0) {
            worker.swing(InteractionHand.MAIN_HAND);
            worker.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WHEAT, harvested));
            SettlementDeferredOutpostService.consumeProductionCredit(level.getServer(), outpost, AGRICULTURE_WORK_PERIOD_TICKS);
        }
    }

    private static BlockPos findMatureCrop(ServerLevel level, SettlementData data, OutpostRecord outpost) {
        for (int forward = 1; forward <= 3; forward++) {
            for (int side : new int[] {-2, -1, 1, 2}) {
                BlockPos crop = local(data, outpost, forward, side, 1);
                if (level.hasChunkAt(crop) && isMatureWheat(level.getBlockState(crop))) return crop;
            }
        }
        return null;
    }

    private static boolean isMatureWheat(BlockState state) {
        return state.is(Blocks.WHEAT) && state.hasProperty(BlockStateProperties.AGE_7)
                && state.getValue(BlockStateProperties.AGE_7) >= 7;
    }

    private static boolean workDue(ServerLevel level, OutpostRecord outpost, int periodTicks) {
        if (SettlementDeferredOutpostService.hasProductionCredit(level.getServer(), outpost, periodTicks)) return true;
        long periodSlots = Math.max(1L, periodTicks / 10L);
        long currentSlot = level.getGameTime() / 10L;
        return Math.floorMod(currentSlot + outpost.id() * 3L, periodSlots) == 0L;
    }

    private static boolean pristineLegacyAgriculturePlot(ServerLevel level, SettlementData data, OutpostRecord outpost) {
        BlockPos water = local(data, outpost, 3, 0, 0);
        if (!level.hasChunkAt(water)) return false;
        BlockState waterBase = level.getBlockState(water);
        if (!waterBase.is(Blocks.COARSE_DIRT)) return false;
        for (int forward = 1; forward <= 3; forward++) {
            for (int side : new int[] {-2, -1, 1, 2}) {
                BlockPos soil = local(data, outpost, forward, side, 0);
                BlockPos crop = soil.above();
                if (!level.hasChunkAt(soil) || !level.hasChunkAt(crop)) return false;
                if (!level.getBlockState(soil).is(Blocks.COARSE_DIRT) || !level.getBlockState(crop).isAir()) return false;
            }
        }
        return true;
    }

    private static void initializeAgriculturePlot(ServerLevel level, SettlementData data, OutpostRecord outpost) {
        BlockPos water = local(data, outpost, 3, 0, 0);
        if (!level.hasChunkAt(water)) return;
        BlockState waterBase = level.getBlockState(water);
        if (waterBase.is(Blocks.WATER)) return;
        if (!waterBase.is(Blocks.COARSE_DIRT) && !waterBase.is(Blocks.DIRT)
                && !waterBase.is(Blocks.GRASS_BLOCK) && !waterBase.is(Blocks.FARMLAND)) return;
        if (level.getBlockEntity(water) != null || !waterBase.getFluidState().isEmpty()) return;

        level.setBlock(water, Blocks.WATER.defaultBlockState(), 3);
        for (int forward = 1; forward <= 3; forward++) {
            for (int side : new int[] {-2, -1, 1, 2}) {
                BlockPos soil = local(data, outpost, forward, side, 0);
                BlockPos crop = soil.above();
                if (!level.hasChunkAt(soil) || !level.hasChunkAt(crop)) continue;
                BlockState existing = level.getBlockState(soil);
                if (level.getBlockEntity(soil) != null || !existing.getFluidState().isEmpty()) continue;
                if (existing.is(Blocks.COARSE_DIRT) || existing.is(Blocks.DIRT) || existing.is(Blocks.GRASS_BLOCK)) {
                    level.setBlock(soil, Blocks.FARMLAND.defaultBlockState(), 3);
                }
                if (level.getBlockState(soil).is(Blocks.FARMLAND) && level.getBlockState(crop).isAir()) {
                    level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3);
                }
            }
        }
    }

    private static BlockPos local(SettlementData data, OutpostRecord outpost, int forward, int side, int y) {
        if (outpost.roadIndex() < 0 || outpost.roadIndex() >= data.roads().size()) {
            return outpost.center().offset(side, y, forward - 4);
        }
        RoadSegment road = data.roads().get(outpost.roadIndex());
        BlockPos gate = road.end().offset(road.directionX(), 0, road.directionZ());
        int x = gate.getX() + road.directionX() * forward - road.directionZ() * side;
        int z = gate.getZ() + road.directionZ() * forward + road.directionX() * side;
        return new BlockPos(x, gate.getY() + y, z);
    }

    private static BlockPos findTree(ServerLevel level, SettlementData data, BlockPos center, int radius) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                for (int y = center.getY() - 4; y <= center.getY() + 10; y++) {
                    BlockPos pos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                    if (!level.hasChunkAt(pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    if (!state.is(BlockTags.LOGS) || isProtected(data, pos) || !hasLeavesAbove(level, pos)) continue;
                    double distance = pos.distSqr(center);
                    if (distance < bestDistance) { best = pos; bestDistance = distance; }
                    break;
                }
            }
        }
        return best;
    }

    private static boolean hasLeavesAbove(ServerLevel level, BlockPos trunk) {
        for (int y = 1; y <= 8; y++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = trunk.offset(dx, y, dz);
                    if (level.hasChunkAt(pos) && level.getBlockState(pos).is(BlockTags.LEAVES)) return true;
                }
            }
        }
        return false;
    }

    private static ItemStack harvestVerticalTrunk(ServerLevel level, SettlementData data, BlockPos base) {
        BlockState first = level.getBlockState(base);
        if (!first.is(BlockTags.LOGS)) return ItemStack.EMPTY;
        Item item = first.getBlock().asItem();
        if (item == Items.AIR) return ItemStack.EMPTY;
        BlockState originalTrunk = first;
        int count = 0;
        for (int y = 0; y < 32 && count < MAX_LOGS; y++) {
            BlockPos pos = base.above(y);
            if (!level.hasChunkAt(pos)) break;
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LOGS) || state.getBlock().asItem() != item || isProtected(data, pos)) {
                if (count > 0) break;
                continue;
            }
            if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) break;
            count++;
        }
        if (count > 0) tryReplantHarvestedTree(level, data, base, originalTrunk, item);
        return count == 0 ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static void tryReplantHarvestedTree(ServerLevel level, SettlementData data, BlockPos base,
                                                BlockState originalTrunk, Item item) {
        if (!level.hasChunkAt(base) || isProtected(data, base) || !level.getBlockState(base).isAir()) return;
        for (int y = 1; y <= 31; y++) {
            BlockPos above = base.above(y);
            if (!level.hasChunkAt(above)) return;
            BlockState state = level.getBlockState(above);
            if (state.is(BlockTags.LOGS) && state.getBlock().asItem() == item) return;
        }
        if (!isNaturalTreeGround(level.getBlockState(base.below()))) return;
        BlockState sapling = saplingForNaturalLog(originalTrunk);
        if (sapling != null) level.setBlock(base, sapling, 3);
    }

    private static boolean isNaturalTreeGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.MYCELIUM) || state.is(Blocks.MUD);
    }

    private static BlockState saplingForNaturalLog(BlockState trunk) {
        if (trunk.is(Blocks.OAK_LOG)) return Blocks.OAK_SAPLING.defaultBlockState();
        if (trunk.is(Blocks.SPRUCE_LOG)) return Blocks.SPRUCE_SAPLING.defaultBlockState();
        if (trunk.is(Blocks.BIRCH_LOG)) return Blocks.BIRCH_SAPLING.defaultBlockState();
        if (trunk.is(Blocks.JUNGLE_LOG)) return Blocks.JUNGLE_SAPLING.defaultBlockState();
        if (trunk.is(Blocks.ACACIA_LOG)) return Blocks.ACACIA_SAPLING.defaultBlockState();
        if (trunk.is(Blocks.DARK_OAK_LOG)) return Blocks.DARK_OAK_SAPLING.defaultBlockState();
        if (trunk.is(Blocks.MANGROVE_LOG)) return Blocks.MANGROVE_PROPAGULE.defaultBlockState();
        if (trunk.is(Blocks.CHERRY_LOG)) return Blocks.CHERRY_SAPLING.defaultBlockState();
        return null;
    }

    private static BlockPos findExposedStone(ServerLevel level, SettlementData data, BlockPos center, int radius) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                for (int y = center.getY() - 5; y <= center.getY() + 4; y++) {
                    BlockPos pos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                    if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.above())) continue;
                    BlockState state = level.getBlockState(pos);
                    if (!isQuarryStone(state) || isProtected(data, pos) || !level.getBlockState(pos.above()).isAir()) continue;
                    double distance = pos.distSqr(center);
                    if (distance < bestDistance) { best = pos; bestDistance = distance; }
                }
            }
        }
        return best;
    }

    private static BlockPos findManagedQuarryStone(ServerLevel level, SettlementData data, BlockPos center) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int radius = 7; radius <= MANAGED_QUARRY_FACE_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int y = center.getY() - 7; y <= center.getY() + 2; y++) {
                        BlockPos pos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                        if (!level.hasChunkAt(pos) || isProtected(data, pos) || !isQuarryStone(level.getBlockState(pos))) continue;
                        int cover = quarryOverburdenDepth(level, data, pos);
                        if (cover <= 0) continue;
                        double distance = pos.distSqr(center);
                        if (distance < bestDistance) { best = pos; bestDistance = distance; }
                    }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    private static BlockPos quarryApproach(ServerLevel level, SettlementData data, BlockPos target) {
        int cover = quarryOverburdenDepth(level, data, target);
        if (cover < 0) return null;
        return cover == 0 ? target : target.above(cover + 1);
    }

    private static int quarryOverburdenDepth(ServerLevel level, SettlementData data, BlockPos stone) {
        if (!level.hasChunkAt(stone) || !isQuarryStone(level.getBlockState(stone)) || isProtected(data, stone)) return -1;
        BlockPos first = stone.above();
        if (!level.hasChunkAt(first)) return -1;
        if (level.getBlockState(first).isAir()) return 0;
        for (int depth = 1; depth <= MANAGED_QUARRY_MAX_OVERBURDEN; depth++) {
            BlockPos pos = stone.above(depth);
            if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) != null || isProtected(data, pos)) return -1;
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty() || !isSafeQuarryOverburden(state)) return -1;
            BlockPos above = pos.above();
            if (!level.hasChunkAt(above)) return -1;
            if (level.getBlockState(above).isAir()) return depth;
        }
        return -1;
    }

    private static boolean clearTopQuarryOverburden(ServerLevel level, SettlementData data, BlockPos stone) {
        int depth = quarryOverburdenDepth(level, data, stone);
        if (depth <= 0) return false;
        BlockPos top = stone.above(depth);
        if (!level.hasChunkAt(top) || level.getBlockEntity(top) != null || isProtected(data, top)) return false;
        BlockState state = level.getBlockState(top);
        if (!state.getFluidState().isEmpty() || !isSafeQuarryOverburden(state)) return false;
        return level.setBlock(top, Blocks.AIR.defaultBlockState(), 3);
    }

    private static boolean isSafeQuarryOverburden(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.MUD) || state.is(Blocks.GRAVEL) || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND) || state.is(Blocks.CLAY) || state.is(Blocks.SNOW_BLOCK);
    }

    private static ItemStack harvestStoneCluster(ServerLevel level, SettlementData data, BlockPos base) {
        BlockState first = level.getBlockState(base);
        if (!isQuarryStone(first)) return ItemStack.EMPTY;
        Item item = first.getBlock().asItem();
        if (item == Items.AIR) return ItemStack.EMPTY;
        int count = 0;
        for (int dx = -1; dx <= 1 && count < MAX_STONE; dx++) {
            for (int dz = -1; dz <= 1 && count < MAX_STONE; dz++) {
                BlockPos pos = base.offset(dx, 0, dz);
                if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.above())) continue;
                BlockState state = level.getBlockState(pos);
                if (state.getBlock().asItem() != item || !isQuarryStone(state) || isProtected(data, pos)
                        || !level.getBlockState(pos.above()).isAir()) continue;
                if (level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) count++;
            }
        }
        return count == 0 ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static boolean isQuarryStone(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE) || state.is(Blocks.TUFF);
    }

    private static BlockPos findOreBelow(ServerLevel level, SettlementData data, BlockPos center) {
        for (int depth = 2; depth <= 28; depth++) {
            int y = center.getY() - depth;
            for (int radius = 0; radius <= 14; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                        BlockPos pos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                        if (!level.hasChunkAt(pos)) continue;
                        if (level.getBlockState(pos).is(Tags.Blocks.ORES) && !isProtected(data, pos)) return pos;
                    }
                }
            }
        }
        return null;
    }

    private static ItemStack mineOre(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return ItemStack.EMPTY;
        BlockState state = level.getBlockState(pos);
        if (!state.is(Tags.Blocks.ORES)) return ItemStack.EMPTY;
        ItemStack result;
        if (state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE)) result = new ItemStack(Items.RAW_IRON);
        else if (state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE)) result = new ItemStack(Items.RAW_COPPER, 2);
        else if (state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE)) result = new ItemStack(Items.RAW_GOLD);
        else if (state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE)) result = new ItemStack(Items.COAL);
        else if (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)) result = new ItemStack(Items.DIAMOND);
        else if (state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE)) result = new ItemStack(Items.EMERALD);
        else if (state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_REDSTONE_ORE)) result = new ItemStack(Items.REDSTONE, 4);
        else if (state.is(Blocks.LAPIS_ORE) || state.is(Blocks.DEEPSLATE_LAPIS_ORE)) result = new ItemStack(Items.LAPIS_LAZULI, 4);
        else {
            Item item = state.getBlock().asItem();
            result = item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        }
        if (result.isEmpty() || !level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3)) return ItemStack.EMPTY;
        return result;
    }

    private static boolean isProtected(SettlementData data, BlockPos pos) {
        if (pos.closerThan(data.stockpilePos(), 3.0D)) return true;
        for (BuildingRecord building : data.buildings()) if (building.protectsXZ(pos, 1)) return true;
        for (RoadSegment road : data.roads()) if (road.containsXZ(pos)) return true;
        for (OutpostRecord outpost : data.outposts()) if (outpost.protectsXZ(pos, 1)) return true;
        return false;
    }

    private static void move(FrontierWorkerEntity worker, BlockPos target, double speed) {
        worker.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, speed);
    }
}
