package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.content.FrontierContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import kr.moonseungjun.frontiersettlement.content.FrontierWorkerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Physical outpost-to-settlement logistics.
 *
 * Transport workers are permanently assigned to one outpost through entity tags. They follow the
 * persisted road network instead of being paired by UUID order, and the service never force-loads
 * remote chunks just to keep logistics simulation running. A completed cart station only changes
 * the town-side freight destination/capacity; it never becomes a second route-navigation authority.
 * Alpha.41 also lets this same transporter carry real food/metal back to an active military outpost;
 * there is still only one authority for long-distance outpost transport. Alpha.46 lets that same
 * worker reverse-supply real wood for an incomplete waterfront pier when no military supply job is active.
 *
 * Alpha.42 may redeem bounded unloaded logistics-time by raising the next real pickup batch. The
 * deferred number is never cargo: source extraction, carried ItemStack, road travel and destination
 * insertion all remain physical and owned by this same transporter.
 */
public final class SettlementOutpostLogisticsService {
    public static final String TRANSPORT_WORKER_TAG = "frontier_settlement_transport_worker";
    public static final String TRANSPORT_OUTPOST_TAG_PREFIX = "frontier_settlement_transport_outpost_";
    public static final String MILITARY_SUPPLY_TRIP_TAG = "frontier_settlement_military_supply_trip";
    public static final String MILITARY_RETURN_TRIP_TAG = "frontier_settlement_military_return_trip";
    public static final String WATERFRONT_SUPPLY_TRIP_TAG = "frontier_settlement_waterfront_supply_trip";
    public static final String WATERFRONT_RETURN_TRIP_TAG = "frontier_settlement_waterfront_return_trip";
    private static final String LEGACY_TRANSPORT_WORKER_NAME = "운송 주민";
    private static final int BASE_TRANSPORT_STACK = 16;
    private static final int CART_STATION_TRANSPORT_STACK = 32;
    private static final int TERRITORY_NETWORK_TRANSPORT_BONUS_PER_LEVEL = 4;
    private static final int MAX_PRODUCTIVE_TRANSPORT_STACK = 44;
    private static final int ROAD_WAYPOINT_STRIDE = 3;
    private static final double ROAD_JOIN_RANGE_SQR = 25.0D;
    private static final double ENDPOINT_RANGE_SQR = 16.0D;
    private static final double STORAGE_INTERACTION_RANGE_SQR = 9.0D;
    private static final double ROUTE_SEARCH_MARGIN = 32.0D;

    private SettlementOutpostLogisticsService() {}

    public static void migrateLegacyWorkers(ServerLevel level, SettlementData data) {
        for (OutpostRecord outpost : data.outposts()) {
            if (!routeFullyLoaded(level, data, outpost)) continue;
            FrontierWorkerEntity legacy = findLegacyWorker(level, data, outpost);
            if (legacy == null) continue;
            // Migration also changes assignment authority. Do not tag a visible legacy worker while a
            // legitimate assigned transporter could merely be hidden in an unloaded search-bound chunk.
            if (!assignmentEvidenceLoaded(level, data, outpost)) continue;
            if (findAssignedWorker(level, data, outpost) != null) continue;
            assignWorker(legacy, outpost);
        }
    }

    public static void tick(ServerLevel level, SettlementData data) {
        for (OutpostRecord outpost : data.outposts()) {
            List<BlockPos> route = routeFromTown(data, outpost);
            if (route.isEmpty()) continue;
            FrontierWorkerEntity worker = findAssignedWorker(level, data, outpost, route);
            if (worker == null) continue;
            SettlementDeferredOutpostService.observeTransportReady(level.getServer(), outpost);
            if (worker.isNoAi()) worker.setNoAi(false);
            workTransport(level, data, outpost, worker, route);
        }
    }

    public static int transportBatchSize(SettlementData data) {
        return data.buildingCount(BuildingType.CART_STATION) > 0
                ? CART_STATION_TRANSPORT_STACK
                : BASE_TRANSPORT_STACK;
    }

    /**
     * Normal productive outpost -> town freight only. A completed cart station is still required,
     * while diversified DOMAIN territory raises only this physical pickup cap from 32 to at most 44.
     * Military/waterfront reverse supply deliberately remains on transportBatchSize(data).
     */
    public static int productiveTransportBatchSize(SettlementData data) {
        if (data.buildingCount(BuildingType.CART_STATION) <= 0) return BASE_TRANSPORT_STACK;
        int networkLevel = SettlementExplorationBenefitService.territoryNetworkLevel(data);
        return Math.min(MAX_PRODUCTIVE_TRANSPORT_STACK, CART_STATION_TRANSPORT_STACK
                + networkLevel * TERRITORY_NETWORK_TRANSPORT_BONUS_PER_LEVEL);
    }

    public static int loadedAssignedWorkerCount(ServerLevel level, SettlementData data) {
        Set<java.util.UUID> ids = new HashSet<>();
        for (OutpostRecord outpost : data.outposts()) {
            if (!assignmentEvidenceLoaded(level, data, outpost)) continue;
            for (FrontierWorkerEntity worker : findAssignedWorkers(level, data, outpost)) ids.add(worker.getUUID());
        }
        return ids.size();
    }

    /**
     * Population reconciliation treats this as an absence-evidence gate, not merely a movement gate.
     * The name is retained because this is the existing single authority consumed by the civilian
     * lifecycle service, but Alpha.67 deliberately requires the complete transporter lookup envelope.
     */
    public static boolean allRoutesLoaded(ServerLevel level, SettlementData data) {
        for (OutpostRecord outpost : data.outposts()) {
            if (!assignmentEvidenceLoaded(level, data, outpost)) return false;
        }
        return true;
    }

    public static OutpostRecord firstMissingLoadedAssignment(ServerLevel level, SettlementData data) {
        for (OutpostRecord outpost : data.outposts()) {
            if (!assignmentEvidenceLoaded(level, data, outpost)) continue;
            if (findAssignedWorkers(level, data, outpost).isEmpty()) return outpost;
        }
        return null;
    }

    /**
     * A dedicated transporter may be carrying the settlement's only physical copy of a cargo stack.
     * Clear vanilla equipment/drop-chance ambiguity and recover that exact MAINHAND stack once.
     */
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!event.getEntity().entityTags().contains(TRANSPORT_WORKER_TAG)) return;
        ItemStack carried = event.getEntity().getMainHandItem();
        event.getDrops().clear();
        if (carried.isEmpty()) return;
        event.getDrops().add(new ItemEntity(
                event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(),
                event.getEntity().getZ(), carried.copy()));
    }

    public static FrontierWorkerEntity spawnAssignedWorker(ServerLevel level, SettlementData data, OutpostRecord outpost) {
        if (outpost == null || !assignmentEvidenceLoaded(level, data, outpost)
                || !findAssignedWorkers(level, data, outpost).isEmpty()) return null;
        FrontierWorkerEntity worker = new FrontierWorkerEntity(FrontierContent.FRONTIER_WORKER.get(), level);
        BlockPos spawn = outpost.center().above();
        if (!level.hasChunkAt(spawn)) return null;
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        assignWorker(worker, outpost);
        if (!level.addFreshEntity(worker)) return null;
        return worker;
    }

    private static void assignWorker(FrontierWorkerEntity worker, OutpostRecord outpost) {
        worker.addTag(TRANSPORT_WORKER_TAG);
        worker.addTag(outpostTag(outpost));
        worker.setCustomName(Component.literal(workerName(outpost)));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
    }

    private static String outpostTag(OutpostRecord outpost) {
        return TRANSPORT_OUTPOST_TAG_PREFIX + outpost.id();
    }

    private static String workerName(OutpostRecord outpost) {
        return "운송 주민 #" + outpost.id();
    }

    private static FrontierWorkerEntity findAssignedWorker(ServerLevel level, SettlementData data, OutpostRecord outpost) {
        List<FrontierWorkerEntity> found = findAssignedWorkers(level, data, outpost);
        return found.isEmpty() ? null : found.getFirst();
    }

    private static List<FrontierWorkerEntity> findAssignedWorkers(ServerLevel level, SettlementData data, OutpostRecord outpost) {
        List<BlockPos> route = routeFromTown(data, outpost);
        return findAssignedWorkers(level, data, outpost, route);
    }

    private static FrontierWorkerEntity findAssignedWorker(ServerLevel level, SettlementData data,
                                               OutpostRecord outpost, List<BlockPos> route) {
        List<FrontierWorkerEntity> found = findAssignedWorkers(level, data, outpost, route);
        return found.isEmpty() ? null : found.getFirst();
    }

    private static List<FrontierWorkerEntity> findAssignedWorkers(ServerLevel level, SettlementData data,
                                                      OutpostRecord outpost, List<BlockPos> route) {
        if (route.isEmpty()) return List.of();
        String assignment = outpostTag(outpost);
        List<FrontierWorkerEntity> found = level.getEntitiesOfClass(FrontierWorkerEntity.class, routeBounds(data, outpost, route),
                villager -> villager.entityTags().contains(TRANSPORT_WORKER_TAG)
                        && villager.entityTags().contains(assignment));
        found.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        return found;
    }

    private static FrontierWorkerEntity findLegacyWorker(ServerLevel level, SettlementData data, OutpostRecord outpost) {
        List<BlockPos> route = routeFromTown(data, outpost);
        if (route.isEmpty()) return null;
        List<FrontierWorkerEntity> candidates = level.getEntitiesOfClass(FrontierWorkerEntity.class, routeBounds(data, outpost, route),
                villager -> !villager.entityTags().contains(TRANSPORT_WORKER_TAG)
                        && villager.getCustomName() != null
                        && LEGACY_TRANSPORT_WORKER_NAME.equals(villager.getCustomName().getString()));
        if (candidates.isEmpty()) return null;
        BlockPos center = outpost.center();
        candidates.sort(Comparator.comparingDouble(villager -> villager.distanceToSqr(
                center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D)));
        return candidates.getFirst();
    }

    private static AABB routeBounds(SettlementData data, OutpostRecord outpost, List<BlockPos> route) {
        double minX = Math.min(data.stockpilePos().getX(), outpost.stockX());
        double minY = Math.min(data.stockpilePos().getY(), outpost.stockY());
        double minZ = Math.min(data.stockpilePos().getZ(), outpost.stockZ());
        double maxX = Math.max(data.stockpilePos().getX(), outpost.stockX());
        double maxY = Math.max(data.stockpilePos().getY(), outpost.stockY());
        double maxZ = Math.max(data.stockpilePos().getZ(), outpost.stockZ());
        for (BlockPos pos : route) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        // Town-side transporters may intentionally sleep in completed houses. Those rest anchors
        // therefore belong to the same lookup/evidence authority as the persisted route, otherwise
        // a sleeping unloaded transporter could be mistaken for a dead/missing assignment.
        for (BuildingRecord building : data.buildings()) {
            if (building.buildingType() != BuildingType.HOUSE) continue;
            minX = Math.min(minX, building.originX());
            minZ = Math.min(minZ, building.originZ());
            maxX = Math.max(maxX, building.originX() + building.rotatedWidth() - 1);
            maxZ = Math.max(maxZ, building.originZ() + building.rotatedDepth() - 1);
        }
        return new AABB(minX - ROUTE_SEARCH_MARGIN, minY - 48.0D, minZ - ROUTE_SEARCH_MARGIN,
                maxX + ROUTE_SEARCH_MARGIN + 1.0D, maxY + 49.0D, maxZ + ROUTE_SEARCH_MARGIN + 1.0D);
    }

    /**
     * Strong loaded-only proof used exclusively when code is about to infer that an assignment is
     * absent, reconcile population from that absence, migrate authority, or create a replacement.
     *
     * The proof covers the exact X/Z AABB used by findAssignedWorker/findLegacyWorker, including the
     * 32-block route search margin. A transporter in any chunk that the authoritative lookup could
     * legitimately return therefore cannot become an "unloaded == dead" false negative. This calls
     * hasChunkAt only and never force-loads a chunk. Normal transport movement intentionally keeps
     * using routeFullyLoaded so Alpha.42 pacing and pause-at-unloaded-route-boundaries semantics stay
     * unchanged.
     */
    private static boolean assignmentEvidenceLoaded(ServerLevel level, SettlementData data,
                                                    OutpostRecord outpost) {
        List<BlockPos> route = routeFromTown(data, outpost);
        if (route.isEmpty() || !routeFullyLoaded(level, data, outpost)) return false;
        AABB bounds = routeBounds(data, outpost, route);
        int minChunkX = Math.floorDiv((int) Math.floor(bounds.minX), 16);
        int maxChunkX = Math.floorDiv((int) Math.floor(Math.nextDown(bounds.maxX)), 16);
        int minChunkZ = Math.floorDiv((int) Math.floor(bounds.minZ), 16);
        int maxChunkZ = Math.floorDiv((int) Math.floor(Math.nextDown(bounds.maxZ)), 16);
        int probeY = data.centerPos().getY();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                BlockPos probe = new BlockPos(chunkX * 16 + 8, probeY, chunkZ * 16 + 8);
                if (!level.hasChunkAt(probe)) return false;
            }
        }
        return true;
    }

    private static void workTransport(ServerLevel level, SettlementData data,
                                      OutpostRecord outpost, FrontierWorkerEntity worker, List<BlockPos> route) {
        boolean military = SettlementMilitaryOutpostService.isActiveMilitaryOutpost(level, outpost);
        boolean waterfrontSupply = !military && SettlementWaterfrontService.woodSupplyShortage(level, outpost) > 0;
        if (!military) {
            worker.removeTag(MILITARY_SUPPLY_TRIP_TAG);
            worker.removeTag(MILITARY_RETURN_TRIP_TAG);
        } else {
            worker.removeTag(WATERFRONT_SUPPLY_TRIP_TAG);
            worker.removeTag(WATERFRONT_RETURN_TRIP_TAG);
        }
        if (!waterfrontSupply) {
            worker.removeTag(WATERFRONT_SUPPLY_TRIP_TAG);
            worker.removeTag(WATERFRONT_RETURN_TRIP_TAG);
        }

        ItemStack carried = worker.getMainHandItem();
        if (military) {
            if (!carried.isEmpty() && worker.entityTags().contains(MILITARY_SUPPLY_TRIP_TAG)) {
                if (!moveAlongRoute(level, worker, route, true)) return;
                deliverMilitarySupply(level, outpost, worker, carried);
                return;
            }
            if (carried.isEmpty() && worker.entityTags().contains(MILITARY_RETURN_TRIP_TAG)) {
                if (!moveAlongRoute(level, worker, route, false)) return;
                loadMilitarySupply(level, data, outpost, worker);
                return;
            }
            if (carried.isEmpty()) {
                worker.addTag(MILITARY_RETURN_TRIP_TAG);
                return;
            }
        }

        if (waterfrontSupply) {
            if (!carried.isEmpty() && worker.entityTags().contains(WATERFRONT_SUPPLY_TRIP_TAG)) {
                if (!moveAlongRoute(level, worker, route, true)) return;
                deliverWaterfrontSupply(level, outpost, worker, carried);
                return;
            }
            if (carried.isEmpty() && worker.entityTags().contains(WATERFRONT_RETURN_TRIP_TAG)) {
                if (!moveAlongRoute(level, worker, route, false)) return;
                loadWaterfrontSupply(level, data, outpost, worker);
                return;
            }
            if (carried.isEmpty()) {
                worker.addTag(WATERFRONT_RETURN_TRIP_TAG);
                return;
            }
        }

        if (carried.isEmpty()) {
            if (!moveAlongRoute(level, worker, route, true)) return;
            BlockPos stock = outpost.stockpile();
            if (!level.hasChunkAt(stock)) {
                worker.getNavigation().stop();
                return;
            }
            if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D)
                    > STORAGE_INTERACTION_RANGE_SQR) {
                move(worker, stock, 0.82D);
                return;
            }
            if (!(level.getBlockEntity(stock) instanceof Container container)) return;
            int normalBatch = productiveTransportBatchSize(data);
            int adjustedBatch = SettlementDeferredOutpostService.adjustedTransportBatch(
                    level.getServer(), outpost, normalBatch);
            ItemStack picked = takeFirstTransportStack(container, outpost, adjustedBatch);
            if (!picked.isEmpty()) {
                worker.setItemSlot(EquipmentSlot.MAINHAND, picked);
                if (picked.getCount() > normalBatch) {
                    SettlementDeferredOutpostService.consumeLogisticsCredit(level.getServer(), outpost);
                }
            } else move(worker, outpost.center().above(), 0.6D);
            return;
        }

        if (!moveAlongRoute(level, worker, route, false)) return;
        deliverToTownStorage(level, data, worker, carried);
    }

    private static void loadMilitarySupply(ServerLevel level, SettlementData data,
                                           OutpostRecord outpost, FrontierWorkerEntity worker) {
        if (!SettlementStorageService.storageAvailable(level, data)) {
            worker.getNavigation().stop();
            return;
        }
        int foodShortage = SettlementMilitaryOutpostService.foodSupplyShortage(level, outpost);
        int metalShortage = SettlementMilitaryOutpostService.metalSupplyShortage(level, outpost);
        Predicate<ItemStack> predicate;
        int amount;
        if (foodShortage > 0) {
            predicate = SettlementInventory::isFood;
            amount = Math.min(foodShortage, transportBatchSize(data));
        } else if (metalShortage > 0) {
            predicate = SettlementStorageService::isMetalStack;
            amount = Math.min(metalShortage, transportBatchSize(data));
        } else if (SettlementMilitaryOutpostService.weaponSupplyShortage(level, outpost) > 0) {
            // Third priority only: the same assigned transporter carries one exact external weapon
            // after the outpost's survival food/metal reserves are already satisfied.
            predicate = SettlementExternalContentService::isExternalWeapon;
            amount = 1;
        } else {
            worker.getNavigation().stop();
            return;
        }

        BlockPos source = SettlementStorageService.findExtractionTarget(level, data, predicate);
        if (source == null) {
            worker.getNavigation().stop();
            return;
        }
        if (worker.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)
                > STORAGE_INTERACTION_RANGE_SQR) {
            move(worker, source, 0.84D);
            return;
        }
        ItemStack extracted = SettlementStorageService.extract(level, source, predicate, amount);
        if (extracted.isEmpty()) return;
        worker.setItemSlot(EquipmentSlot.MAINHAND, extracted);
        worker.removeTag(MILITARY_RETURN_TRIP_TAG);
        worker.addTag(MILITARY_SUPPLY_TRIP_TAG);
        SettlementService.refreshResources(level.getServer(), data);
        SettlementService.broadcast(level.getServer(), data);
    }

    private static void deliverMilitarySupply(ServerLevel level, OutpostRecord outpost,
                                               FrontierWorkerEntity worker, ItemStack carried) {
        BlockPos stock = outpost.stockpile();
        if (!level.hasChunkAt(stock)) {
            worker.getNavigation().stop();
            return;
        }
        if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D)
                > STORAGE_INTERACTION_RANGE_SQR) {
            move(worker, stock, 0.84D);
            return;
        }
        if (!(level.getBlockEntity(stock) instanceof Container container)) return;
        // Demand can become stale while the real weapon is physically in flight. If another weapon
        // appeared or the sentry became armed, keep this exact stack in MAINHAND, drop only the
        // military-supply state, and let the existing normal return path carry it back to town.
        if (SettlementExternalContentService.isExternalWeapon(carried)
                && SettlementMilitaryOutpostService.weaponSupplyShortage(level, outpost) <= 0) {
            worker.removeTag(MILITARY_SUPPLY_TRIP_TAG);
            worker.getNavigation().stop();
            return;
        }
        ItemStack remaining = SettlementInventory.insert(container, carried);
        worker.setItemSlot(EquipmentSlot.MAINHAND, remaining);
        if (remaining.isEmpty()) {
            worker.removeTag(MILITARY_SUPPLY_TRIP_TAG);
            worker.addTag(MILITARY_RETURN_TRIP_TAG);
        }
    }

    private static void loadWaterfrontSupply(ServerLevel level, SettlementData data,
                                             OutpostRecord outpost, FrontierWorkerEntity worker) {
        if (!SettlementStorageService.storageAvailable(level, data)) {
            worker.getNavigation().stop();
            return;
        }
        int shortage = SettlementWaterfrontService.woodSupplyShortage(level, outpost);
        if (shortage <= 0) {
            worker.removeTag(WATERFRONT_RETURN_TRIP_TAG);
            return;
        }
        BlockPos source = SettlementStorageService.findExtractionTarget(level, data, SettlementInventory::isWood);
        if (source == null) {
            worker.getNavigation().stop();
            return;
        }
        if (worker.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D)
                > STORAGE_INTERACTION_RANGE_SQR) {
            move(worker, source, 0.84D);
            return;
        }
        int amount = Math.min(shortage, transportBatchSize(data));
        ItemStack extracted = SettlementStorageService.extract(level, source, SettlementInventory::isWood, amount);
        if (extracted.isEmpty()) return;
        worker.setItemSlot(EquipmentSlot.MAINHAND, extracted);
        worker.removeTag(WATERFRONT_RETURN_TRIP_TAG);
        worker.addTag(WATERFRONT_SUPPLY_TRIP_TAG);
        SettlementService.refreshResources(level.getServer(), data);
        SettlementService.broadcast(level.getServer(), data);
    }

    private static void deliverWaterfrontSupply(ServerLevel level, OutpostRecord outpost,
                                                FrontierWorkerEntity worker, ItemStack carried) {
        BlockPos stock = outpost.stockpile();
        if (!level.hasChunkAt(stock)) {
            worker.getNavigation().stop();
            return;
        }
        if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D)
                > STORAGE_INTERACTION_RANGE_SQR) {
            move(worker, stock, 0.84D);
            return;
        }
        if (!(level.getBlockEntity(stock) instanceof Container container)) return;
        ItemStack remaining = SettlementInventory.insert(container, carried);
        worker.setItemSlot(EquipmentSlot.MAINHAND, remaining);
        if (remaining.isEmpty()) worker.removeTag(WATERFRONT_SUPPLY_TRIP_TAG);
    }

    private static ItemStack takeFirstTransportStack(Container container, OutpostRecord outpost, int maxCount) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack current = container.getItem(slot);
            if (current.isEmpty() || !isOutpostCargo(outpost, current)) continue;
            int take = Math.min(maxCount, current.getCount());
            ItemStack result = current.copyWithCount(take);
            current.shrink(take);
            container.setChanged();
            return result;
        }
        return ItemStack.EMPTY;
    }

    private static boolean isOutpostCargo(OutpostRecord outpost, ItemStack stack) {
        return switch (outpost.specialization()) {
            case "lumber" -> SettlementInventory.isWood(stack);
            case "quarry" -> SettlementInventory.isStone(stack);
            case "agriculture" -> SettlementInventory.isFood(stack);
            case "mining" -> isMiningCargo(stack);
            default -> SettlementInventory.isWood(stack) || SettlementInventory.isStone(stack)
                    || SettlementInventory.isFood(stack) || isMiningCargo(stack);
        };
    }

    private static boolean isMiningCargo(ItemStack stack) {
        return stack.is(Items.RAW_IRON) || stack.is(Items.RAW_COPPER) || stack.is(Items.RAW_GOLD)
                || stack.is(Items.COAL) || stack.is(Items.DIAMOND) || stack.is(Items.EMERALD)
                || stack.is(Items.REDSTONE) || stack.is(Items.LAPIS_LAZULI);
    }

    private static void deliverToTownStorage(ServerLevel level, SettlementData data,
                                             FrontierWorkerEntity worker, ItemStack carried) {
        BlockPos target = SettlementStorageService.findLogisticsDepositTarget(level, data, carried);
        if (!level.hasChunkAt(target)) {
            worker.getNavigation().stop();
            return;
        }
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                > STORAGE_INTERACTION_RANGE_SQR) {
            move(worker, target, 0.85D);
            return;
        }
        worker.setItemSlot(EquipmentSlot.MAINHAND, SettlementStorageService.insertAt(level, target, carried));
    }

    private static boolean moveAlongRoute(ServerLevel level, FrontierWorkerEntity worker,
                                          List<BlockPos> route, boolean towardOutpost) {
        if (route.isEmpty()) return false;
        int nearest = nearestRouteIndex(worker, route);
        int endpoint = towardOutpost ? route.size() - 1 : 0;
        BlockPos nearestPos = route.get(nearest).above();
        if (!level.hasChunkAt(nearestPos)) {
            worker.getNavigation().stop();
            return false;
        }
        double nearestDistance = worker.distanceToSqr(
                nearestPos.getX() + 0.5D, nearestPos.getY(), nearestPos.getZ() + 0.5D);
        if (nearestDistance > ROAD_JOIN_RANGE_SQR) {
            move(worker, nearestPos, 0.88D);
            return false;
        }

        if (nearest == endpoint) {
            BlockPos end = route.get(endpoint).above();
            if (!level.hasChunkAt(end)) {
                worker.getNavigation().stop();
                return false;
            }
            double endDistance = worker.distanceToSqr(end.getX() + 0.5D, end.getY(), end.getZ() + 0.5D);
            if (endDistance <= ENDPOINT_RANGE_SQR) return true;
            move(worker, end, 0.88D);
            return false;
        }

        int next = towardOutpost
                ? Math.min(endpoint, nearest + ROAD_WAYPOINT_STRIDE)
                : Math.max(endpoint, nearest - ROAD_WAYPOINT_STRIDE);
        BlockPos nextPos = route.get(next).above();
        if (!level.hasChunkAt(nextPos)) {
            worker.getNavigation().stop();
            return false;
        }
        move(worker, nextPos, 0.9D);
        return false;
    }

    private static int nearestRouteIndex(FrontierWorkerEntity worker, List<BlockPos> route) {
        int bestIndex = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < route.size(); i++) {
            BlockPos pos = route.get(i).above();
            double distance = worker.distanceToSqr(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static void move(FrontierWorkerEntity worker, BlockPos target, double speed) {
        worker.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, speed);
    }

    static List<BlockPos> routeFromTown(SettlementData data, OutpostRecord outpost) {
        if (outpost.roadIndex() < 0 || outpost.roadIndex() >= data.roads().size()) return List.of();
        List<BlockPos> route = new ArrayList<>();
        RoadSegment attached = data.roads().get(outpost.roadIndex());
        List<BlockPos> attachedCenters = attached.centers();
        if (attachedCenters.isEmpty()) return List.of();
        if (!appendRoadPrefixFromTown(data, outpost.roadIndex(), attachedCenters.size() - 1,
                route, new HashSet<>())) return List.of();
        return List.copyOf(route);
    }

    private static boolean appendRoadPrefixFromTown(SettlementData data, int roadIndex, int endIndex,
                                                    List<BlockPos> route, Set<Integer> visiting) {
        if (roadIndex < 0 || roadIndex >= data.roads().size() || !visiting.add(roadIndex)) return false;
        RoadSegment road = data.roads().get(roadIndex);
        List<BlockPos> centers = road.centers();
        if (centers.isEmpty()) {
            visiting.remove(roadIndex);
            return false;
        }
        int last = Math.max(0, Math.min(endIndex, centers.size() - 1));
        BlockPos start = centers.getFirst();

        boolean connected = horizontalDistanceSqr(start, data.centerPos()) <= 24L * 24L;
        if (!connected) {
            ParentRoad parent = findParentRoad(data, roadIndex, start);
            if (parent != null) {
                connected = appendRoadPrefixFromTown(data, parent.roadIndex(), parent.centerIndex(), route, visiting);
            } else {
                OutpostRecord parentOutpost = findParentOutpost(data, roadIndex, start);
                if (parentOutpost != null && parentOutpost.roadIndex() >= 0
                        && parentOutpost.roadIndex() < data.roads().size()) {
                    List<BlockPos> parentCenters = data.roads().get(parentOutpost.roadIndex()).centers();
                    connected = !parentCenters.isEmpty()
                            && appendRoadPrefixFromTown(data, parentOutpost.roadIndex(), parentCenters.size() - 1,
                            route, visiting);
                    if (connected) appendUnique(route, parentOutpost.center());
                }
            }
        }

        if (connected) {
            for (int i = 0; i <= last; i++) appendUnique(route, centers.get(i));
        }
        visiting.remove(roadIndex);
        return connected;
    }

    private record ParentRoad(int roadIndex, int centerIndex, long distanceSqr) {}

    private static ParentRoad findParentRoad(SettlementData data, int roadIndex, BlockPos start) {
        ParentRoad best = null;
        for (int i = 0; i < roadIndex; i++) {
            RoadSegment candidate = data.roads().get(i);
            if (!candidate.containsXZ(start)) continue;
            List<BlockPos> centers = candidate.centers();
            for (int j = 0; j < centers.size(); j++) {
                long distance = horizontalDistanceSqr(start, centers.get(j));
                if (best == null || distance < best.distanceSqr()) best = new ParentRoad(i, j, distance);
            }
        }
        return best;
    }

    private static OutpostRecord findParentOutpost(SettlementData data, int roadIndex, BlockPos start) {
        OutpostRecord best = null;
        long bestDistance = Long.MAX_VALUE;
        for (OutpostRecord outpost : data.outposts()) {
            if (outpost.roadIndex() < 0 || outpost.roadIndex() >= roadIndex) continue;
            long distance = horizontalDistanceSqr(start, outpost.center());
            if (distance <= 6L * 6L && distance < bestDistance) {
                best = outpost;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static void appendUnique(List<BlockPos> route, BlockPos pos) {
        if (route.isEmpty() || !route.getLast().equals(pos)) route.add(pos.immutable());
    }

    private static long horizontalDistanceSqr(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dz = (long) a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    public static boolean routeFullyLoaded(ServerLevel level, SettlementData data, OutpostRecord outpost) {
        List<BlockPos> route = routeFromTown(data, outpost);
        if (route.isEmpty()) return false;
        if (!level.hasChunkAt(data.stockpilePos()) || !level.hasChunkAt(outpost.stockpile())) return false;
        for (BlockPos pos : route) {
            if (!level.hasChunkAt(pos)) return false;
        }
        return true;
    }
}
