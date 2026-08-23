package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Physical outpost-to-settlement logistics.
 *
 * Transport workers are permanently assigned to one outpost through entity tags. They follow the
 * persisted road network instead of being paired by UUID order, and the service never force-loads
 * remote chunks just to keep logistics simulation running.
 */
public final class SettlementOutpostLogisticsService {
    public static final String TRANSPORT_WORKER_TAG = "frontier_settlement_transport_worker";
    public static final String TRANSPORT_OUTPOST_TAG_PREFIX = "frontier_settlement_transport_outpost_";
    private static final String LEGACY_TRANSPORT_WORKER_NAME = "운송 주민";
    private static final int MAX_TRANSPORT_STACK = 16;
    private static final int ROAD_WAYPOINT_STRIDE = 3;
    private static final double ROAD_JOIN_RANGE_SQR = 25.0D;
    private static final double ENDPOINT_RANGE_SQR = 16.0D;
    private static final double STORAGE_INTERACTION_RANGE_SQR = 9.0D;
    private static final double ROUTE_SEARCH_MARGIN = 32.0D;

    private SettlementOutpostLogisticsService() {}

    /** Assign old pre-Alpha.27 generic transport villagers without charging arrival food again. */
    public static void migrateLegacyWorkers(ServerLevel level, SettlementData data) {
        for (OutpostRecord outpost : data.outposts()) {
            if (!routeLoaded(level, data, outpost)) continue;
            if (findAssignedWorker(level, data, outpost) != null) continue;
            Villager legacy = findLegacyWorker(level, data, outpost);
            if (legacy != null) assignWorker(legacy, outpost);
        }
    }

    public static void tick(ServerLevel level, SettlementData data) {
        for (OutpostRecord outpost : data.outposts()) {
            if (!routeLoaded(level, data, outpost)) continue;
            Villager worker = findAssignedWorker(level, data, outpost);
            if (worker == null) continue;
            if (worker.isNoAi()) worker.setNoAi(false);
            workTransport(level, data, outpost, worker);
        }
    }

    /** Only safe to use for authoritative population reconciliation when all routes are loaded. */
    public static int loadedAssignedWorkerCount(ServerLevel level, SettlementData data) {
        int count = 0;
        for (OutpostRecord outpost : data.outposts()) {
            if (!routeLoaded(level, data, outpost)) continue;
            if (findAssignedWorker(level, data, outpost) != null) count++;
        }
        return count;
    }

    public static boolean allRoutesLoaded(ServerLevel level, SettlementData data) {
        for (OutpostRecord outpost : data.outposts()) {
            if (!routeLoaded(level, data, outpost)) return false;
        }
        return true;
    }

    /** Returns one loaded outpost that truly lacks its assigned transporter. */
    public static OutpostRecord firstMissingLoadedAssignment(ServerLevel level, SettlementData data) {
        for (OutpostRecord outpost : data.outposts()) {
            if (!routeLoaded(level, data, outpost)) continue;
            if (findAssignedWorker(level, data, outpost) == null) return outpost;
        }
        return null;
    }

    public static Villager spawnAssignedWorker(ServerLevel level, OutpostRecord outpost) {
        Villager worker = new Villager(EntityTypes.VILLAGER, level);
        BlockPos spawn = outpost.center().above();
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        assignWorker(worker, outpost);
        level.addFreshEntity(worker);
        return worker;
    }

    private static void assignWorker(Villager worker, OutpostRecord outpost) {
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

    private static Villager findAssignedWorker(ServerLevel level, SettlementData data, OutpostRecord outpost) {
        List<BlockPos> route = routeFromTown(data, outpost);
        if (route.isEmpty()) return null;
        String assignment = outpostTag(outpost);
        List<Villager> found = level.getEntitiesOfClass(Villager.class, routeBounds(data, outpost, route),
                villager -> villager.entityTags().contains(TRANSPORT_WORKER_TAG)
                        && villager.entityTags().contains(assignment));
        if (found.isEmpty()) return null;
        found.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        return found.getFirst();
    }

    private static Villager findLegacyWorker(ServerLevel level, SettlementData data, OutpostRecord outpost) {
        List<BlockPos> route = routeFromTown(data, outpost);
        if (route.isEmpty()) return null;
        List<Villager> candidates = level.getEntitiesOfClass(Villager.class, routeBounds(data, outpost, route),
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
        return new AABB(minX - ROUTE_SEARCH_MARGIN, minY - 48.0D, minZ - ROUTE_SEARCH_MARGIN,
                maxX + ROUTE_SEARCH_MARGIN + 1.0D, maxY + 49.0D, maxZ + ROUTE_SEARCH_MARGIN + 1.0D);
    }

    private static void workTransport(ServerLevel level, SettlementData data,
                                      OutpostRecord outpost, Villager worker) {
        List<BlockPos> route = routeFromTown(data, outpost);
        if (route.isEmpty()) {
            worker.getNavigation().stop();
            return;
        }

        ItemStack carried = worker.getMainHandItem();
        if (carried.isEmpty()) {
            if (!moveAlongRoute(worker, route, true)) return;
            BlockPos stock = outpost.stockpile();
            if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D)
                    > STORAGE_INTERACTION_RANGE_SQR) {
                move(worker, stock, 0.82D);
                return;
            }
            if (!(level.getBlockEntity(stock) instanceof Container container)) return;
            ItemStack picked = takeFirstTransportStack(container, outpost, MAX_TRANSPORT_STACK);
            if (!picked.isEmpty()) worker.setItemSlot(EquipmentSlot.MAINHAND, picked);
            else move(worker, outpost.center().above(), 0.6D);
            return;
        }

        // A pre-Alpha.27 worker may be migrated while carrying a legacy stack. Preserve it and
        // return it safely rather than deleting it, even if it is not normal specialized cargo.
        if (!moveAlongRoute(worker, route, false)) return;
        deliverToTownStorage(level, data, worker, carried);
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
                                             Villager worker, ItemStack carried) {
        BlockPos target = SettlementStorageService.findDepositTarget(level, data, carried);
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                > STORAGE_INTERACTION_RANGE_SQR) {
            move(worker, target, 0.85D);
            return;
        }
        worker.setItemSlot(EquipmentSlot.MAINHAND, SettlementStorageService.insert(level, data, carried));
    }

    /** Follow persisted road centers in short strides so L-corners and chained roads remain visible. */
    private static boolean moveAlongRoute(Villager worker, List<BlockPos> route, boolean towardOutpost) {
        if (route.isEmpty()) return false;
        int nearest = nearestRouteIndex(worker, route);
        int endpoint = towardOutpost ? route.size() - 1 : 0;
        BlockPos nearestPos = route.get(nearest).above();
        double nearestDistance = worker.distanceToSqr(
                nearestPos.getX() + 0.5D, nearestPos.getY(), nearestPos.getZ() + 0.5D);
        if (nearestDistance > ROAD_JOIN_RANGE_SQR) {
            move(worker, nearestPos, 0.88D);
            return false;
        }

        if (nearest == endpoint) {
            BlockPos end = route.get(endpoint).above();
            double endDistance = worker.distanceToSqr(end.getX() + 0.5D, end.getY(), end.getZ() + 0.5D);
            if (endDistance <= ENDPOINT_RANGE_SQR) return true;
            move(worker, end, 0.88D);
            return false;
        }

        int next = towardOutpost
                ? Math.min(endpoint, nearest + ROAD_WAYPOINT_STRIDE)
                : Math.max(endpoint, nearest - ROAD_WAYPOINT_STRIDE);
        move(worker, route.get(next).above(), 0.9D);
        return false;
    }

    private static int nearestRouteIndex(Villager worker, List<BlockPos> route) {
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

    private static void move(Villager worker, BlockPos target, double speed) {
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

    private static boolean routeLoaded(ServerLevel level, SettlementData data, OutpostRecord outpost) {
        List<BlockPos> route = routeFromTown(data, outpost);
        if (route.isEmpty()) return false;
        if (!level.hasChunkAt(data.stockpilePos()) || !level.hasChunkAt(outpost.stockpile())) return false;
        for (BlockPos pos : route) {
            if (!level.hasChunkAt(pos)) return false;
        }
        return true;
    }
}
