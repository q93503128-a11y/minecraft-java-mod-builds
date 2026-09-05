package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.content.FrontierContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import kr.moonseungjun.frontiersettlement.content.FrontierWorkerEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.pathfinder.Path;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SettlementWorkerService {
    public static final String RESOURCE_WORKER_TAG = "frontier_settlement_resource_worker";
    private static final String WORKSITE_EXPORT_TAG = "frontier_settlement_worksite_export";
    private static final String LUMBER_WORKER_NAME = "벌목 주민";
    private static final String FARM_WORKER_NAME = "농사 주민";
    private static final String QUARRY_WORKER_NAME = "채석 주민";
    private static final String MINE_WORKER_NAME = "광산 주민";
    private static final long ARRIVAL_FOOD_COST = 4L;
    private static final int LOCAL_RESOURCE_ROUTE_MARGIN = 56;
    private static final int TREE_SEARCH_RADIUS = 128;
    private static final int TREE_SEARCH_DOWN = 12;
    private static final int TREE_SEARCH_UP = 28;
    private static final int QUARRY_SEARCH_RADIUS = 96;
    private static final int QUARRY_SEARCH_DOWN = 16;
    private static final int QUARRY_SEARCH_UP = 12;
    private static final int MINE_HORIZONTAL_SEARCH_RADIUS = 48;
    private static final int MINE_SEARCH_DEPTH = 80;
    private static final long RESOURCE_TARGET_CACHE_TICKS = 600L;
    private static final long RESOURCE_SEARCH_RETRY_TICKS = 100L;
    private static final long BLOCKED_TARGET_RETRY_TICKS = 120L;
    private static final long STUCK_PROGRESS_TIMEOUT_TICKS = 80L;
    private static final int MAX_APPROACH_PATH_TRIES = 64;
    private static final int PRODUCTION_HAUL_STACK = 64;
    private static final int MAX_LOGS_PER_WORK = 16;
    private static final int MAX_STONE_PER_WORK = 16;
    private static final int LUMBER_WORK_PERIOD_TICKS = 100;
    private static final int FARM_WORK_PERIOD_TICKS = 120;
    private static final int QUARRY_WORK_PERIOD_TICKS = 80;
    private static final int MINING_WORK_PERIOD_TICKS = 160;

    private SettlementWorkerService() {}

    public static long arrivalFoodCost() { return ARRIVAL_FOOD_COST; }

    public record NormalizeResult(int removedProductionWorkers, int loadedProductionWorkers) {}
    private record TreeCandidate(BlockPos base, Item item, double distance, int availableLogs) {}
    private record CachedTarget(BlockPos pos, long expiresAt) {}
    private record MovementWatch(BlockPos target, double x, double y, double z, long lastProgressTick) {}

    private static final Map<java.util.UUID, CachedTarget> RESOURCE_TARGETS = new HashMap<>();
    private static final Map<java.util.UUID, Long> RESOURCE_SEARCH_RETRY_AFTER = new HashMap<>();
    private static final Map<java.util.UUID, Map<BlockPos, Long>> BLOCKED_TARGETS = new HashMap<>();
    private static final Map<java.util.UUID, MovementWatch> MOVEMENT_WATCHES = new HashMap<>();

    public static void onServerStopping(ServerStoppingEvent event) {
        RESOURCE_TARGETS.clear();
        RESOURCE_SEARCH_RETRY_AFTER.clear();
        BLOCKED_TARGETS.clear();
        MOVEMENT_WATCHES.clear();
    }

    public static void tick(MinecraftServer server, SettlementData data) {
        ServerLevel level = server.overworld();
        if (server.getTickCount() % 10 == 0) {
            SettlementOutpostLogisticsService.migrateLegacyWorkers(level, data);
            SettlementConstructionService.reconcileBuilderDuplicates(level, data);
            int removedDuplicates = reconcileProductionDuplicates(level, data);
            removedDuplicates += SettlementOutpostLogisticsService.reconcileLoadedAssignmentDuplicates(level, data);
            removedDuplicates += SettlementWorkshopService.reconcileLoadedAssignmentDuplicates(level, data);
            removedDuplicates += SettlementAdvancedWorkshopService.reconcileLoadedAssignmentDuplicates(level, data);
            if (removedDuplicates > 0) {
                // Recompute only after every civilian duplicate authority has been normalized.
                // If some assignment evidence is unloaded, repairPopulationAfterDuplicateCleanup()
                // deliberately keeps the saved count conservative until a complete view is available.
                repairPopulationAfterDuplicateCleanup(level, data);
                SettlementService.refreshResources(server, data);
                SettlementService.broadcast(server, data);
            }
        }
        // Duplicate reconciliation must run first on the same 600-tick boundary so an excess
        // historical worker can never be removed and immediately replaced from stale population state.
        if (server.getTickCount() % 600 == 0) tryAttractWorker(server, level, data);
        if (server.getTickCount() % 10 != 0) return;

        runBuildingWorkers(level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME, SettlementWorkerService::workLumber);
        runBuildingWorkers(level, data, BuildingType.FARM, FARM_WORKER_NAME, SettlementWorkerService::workFarm);
        runBuildingWorkers(level, data, BuildingType.QUARRY, QUARRY_WORKER_NAME, SettlementWorkerService::workQuarry);
        runBuildingWorkers(level, data, BuildingType.MINE, MINE_WORKER_NAME, SettlementWorkerService::workMine);
        SettlementOutpostLogisticsService.tick(level, data);
    }

    /**
     * Save-recovery cleanup for historical Alpha.84-88 local production duplicates.
     *
     * Cleanup is destructive only after the complete local production/storage evidence envelope is
     * loaded. UUID order is already deterministic in workersByName(), so exactly one physical worker
     * per completed production building remains authoritative. No unloaded resident is treated as dead.
     */
    private static int reconcileProductionDuplicates(ServerLevel level, SettlementData data) {
        // Seeing more loaded physical workers than completed jobs is already sufficient proof of an
        // excess entity. No unloaded resident can make N+1 loaded bodies legal for N completed jobs,
        // so duplicate removal itself must not be blocked by the much wider recruitment evidence gate.
        int removed = 0;
        removed += trimExcessProductionWorkers(level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME);
        removed += trimExcessProductionWorkers(level, data, BuildingType.FARM, FARM_WORKER_NAME);
        removed += trimExcessProductionWorkers(level, data, BuildingType.QUARRY, QUARRY_WORKER_NAME);
        removed += trimExcessProductionWorkers(level, data, BuildingType.MINE, MINE_WORKER_NAME);
        return removed;
    }

    /**
     * Explicit loaded-world maintenance entry point used by /frontier normalize.
     * The command never force-loads chunks and only deletes a worker when the loaded count alone
     * already exceeds the number of completed local jobs. Surviving workers have stale navigation
     * and historical invulnerability/NoAI state cleared once so the next work tick can retarget.
     */
    public static NormalizeResult normalizeLoadedWorkers(MinecraftServer server, SettlementData data) {
        ServerLevel level = server.overworld();
        int removed = 0;
        removed += trimExcessLoadedTownWorkers(level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME);
        removed += trimExcessLoadedTownWorkers(level, data, BuildingType.FARM, FARM_WORKER_NAME);
        removed += trimExcessLoadedTownWorkers(level, data, BuildingType.QUARRY, QUARRY_WORKER_NAME);
        removed += trimExcessLoadedTownWorkers(level, data, BuildingType.MINE, MINE_WORKER_NAME);

        int loaded = 0;
        loaded += resetLoadedTownWorkers(level, data, LUMBER_WORKER_NAME);
        loaded += resetLoadedTownWorkers(level, data, FARM_WORKER_NAME);
        loaded += resetLoadedTownWorkers(level, data, QUARRY_WORKER_NAME);
        loaded += resetLoadedTownWorkers(level, data, MINE_WORKER_NAME);

        if (removed > 0) repairPopulationAfterDuplicateCleanup(level, data);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
        return new NormalizeResult(removed, loaded);
    }

    private static int trimExcessLoadedTownWorkers(ServerLevel level, SettlementData data,
                                                   BuildingType type, String workerName) {
        int allowed = buildings(data, type).size();
        List<FrontierWorkerEntity> workers = loadedTownWorkersByName(level, data, workerName);
        if (workers.size() <= allowed) return 0;
        int removed = 0;
        for (int i = allowed; i < workers.size(); i++) {
            if (removeDuplicateWorkerPreservingCargo(level, workers.get(i))) removed++;
        }
        return removed;
    }

    private static int resetLoadedTownWorkers(ServerLevel level, SettlementData data, String workerName) {
        int count = 0;
        for (FrontierWorkerEntity worker : loadedTownWorkersByName(level, data, workerName)) {
            worker.setNoAi(false);
            worker.setInvulnerable(false);
            worker.getNavigation().stop();
            clearTransientWorkerState(worker);
            count++;
        }
        return count;
    }

    private static List<FrontierWorkerEntity> loadedTownWorkersByName(ServerLevel level, SettlementData data,
                                                                      String workerName) {
        BlockPos center = data.centerPos();
        AABB loadedTown = new AABB(
                center.getX() - 256.0D, center.getY() - 96.0D, center.getZ() - 256.0D,
                center.getX() + 257.0D, center.getY() + 97.0D, center.getZ() + 257.0D);
        List<FrontierWorkerEntity> workers = level.getEntitiesOfClass(FrontierWorkerEntity.class, loadedTown,
                candidate -> candidate.getCustomName() != null
                        && workerName.equals(candidate.getCustomName().getString()));
        workers.sort(Comparator.comparing(worker -> worker.getUUID().toString()));
        return workers;
    }

    private static int trimExcessProductionWorkers(ServerLevel level, SettlementData data,
                                                    BuildingType type, String workerName) {
        int allowed = buildings(data, type).size();
        List<FrontierWorkerEntity> workers = workersByName(level, data, type, workerName);
        if (workers.size() <= allowed) return 0;
        int removed = 0;
        for (int i = allowed; i < workers.size(); i++) {
            if (removeDuplicateWorkerPreservingCargo(level, workers.get(i))) removed++;
        }
        return removed;
    }

    static boolean removeDuplicateWorkerPreservingCargo(ServerLevel level, FrontierWorkerEntity worker) {
        worker.getNavigation().stop();
        worker.setNoAi(false);
        worker.setInvulnerable(false);
        ItemStack carried = worker.getMainHandItem();
        if (!carried.isEmpty()) {
            ItemEntity physical = new ItemEntity(level, worker.getX(), worker.getY(), worker.getZ(), carried.copy());
            if (!level.addFreshEntity(physical)) return false;
            worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        worker.discard();
        return true;
    }

    private static void repairPopulationAfterDuplicateCleanup(ServerLevel level, SettlementData data) {
        if (!SettlementOutpostLogisticsService.allRoutesLoaded(level, data)
                || !SettlementWorkshopService.allAssignmentsLoaded(level, data)
                || !SettlementAdvancedWorkshopService.allAssignmentsLoaded(level, data)) return;
        int transport = SettlementOutpostLogisticsService.loadedAssignedWorkerCount(level, data);
        int workshop = SettlementWorkshopService.loadedAssignedWorkerCount(level, data);
        int advanced = SettlementAdvancedWorkshopService.loadedAssignedWorkerCount(level, data);
        int actualPopulation = 1
                + workersByName(level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME).size()
                + workersByName(level, data, BuildingType.FARM, FARM_WORKER_NAME).size()
                + workersByName(level, data, BuildingType.QUARRY, QUARRY_WORKER_NAME).size()
                + workersByName(level, data, BuildingType.MINE, MINE_WORKER_NAME).size()
                + transport + workshop + advanced;
        if (data.population() != actualPopulation) data.setPopulation(actualPopulation);
    }

    @FunctionalInterface
    private interface BuildingWork {
        void run(ServerLevel level, SettlementData data, FrontierWorkerEntity worker, BuildingRecord building);
    }

    private static void runBuildingWorkers(ServerLevel level, SettlementData data, BuildingType type,
                                           String workerName, BuildingWork work) {
        List<BuildingRecord> buildings = buildings(data, type);
        List<FrontierWorkerEntity> workers = workersByName(level, data, type, workerName);
        int count = Math.min(buildings.size(), workers.size());
        for (int i = 0; i < count; i++) {
            BuildingRecord building = buildings.get(i);
            if (!level.hasChunkAt(building.workCenter())) continue;
            FrontierWorkerEntity worker = workers.get(i);
            // Frontier owns the work order, but workers remain ordinary damageable mobs.
            // Clear stale Alpha.84-87 quarantine/active-project flags on every ordinary work tick.
            worker.setNoAi(false);
            worker.setInvulnerable(false);
            work.run(level, data, worker, building);
        }
    }

    private static void tryAttractWorker(MinecraftServer server, ServerLevel level, SettlementData data) {
        List<FrontierWorkerEntity> lumber = workersByName(level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME);
        List<FrontierWorkerEntity> farm = workersByName(level, data, BuildingType.FARM, FARM_WORKER_NAME);
        List<FrontierWorkerEntity> quarry = workersByName(level, data, BuildingType.QUARRY, QUARRY_WORKER_NAME);
        List<FrontierWorkerEntity> mine = workersByName(level, data, BuildingType.MINE, MINE_WORKER_NAME);
        boolean localEvidenceLoaded = localProductionEvidenceLoaded(level, data);

        // Population is repaired downward/upward only when every civilian evidence corridor is visible.
        // An unloaded resident is not a dead resident, and must never free a housing slot or trigger a duplicate.
        if (localEvidenceLoaded
                && SettlementOutpostLogisticsService.allRoutesLoaded(level, data)
                && SettlementWorkshopService.allAssignmentsLoaded(level, data)
                && SettlementAdvancedWorkshopService.allAssignmentsLoaded(level, data)) {
            int transport = SettlementOutpostLogisticsService.loadedAssignedWorkerCount(level, data);
            int workshop = SettlementWorkshopService.loadedAssignedWorkerCount(level, data);
            int advanced = SettlementAdvancedWorkshopService.loadedAssignedWorkerCount(level, data);
            int actualPopulation = 1 + lumber.size() + farm.size() + quarry.size() + mine.size()
                    + transport + workshop + advanced;
            if (data.population() != actualPopulation) data.setPopulation(actualPopulation);
        }
        if (data.population() >= data.housingCapacity()) return;

        // Ordinary production workers have no per-worker manual assignment UI. Do not infer a vacancy
        // from a partial entity view; only recruit when their work<->storage envelope is fully loaded.
        if (localEvidenceLoaded) {
            if (tryFillJob(server, level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME, lumber.size())) return;
            if (tryFillJob(server, level, data, BuildingType.FARM, FARM_WORKER_NAME, farm.size())) return;
            if (tryFillJob(server, level, data, BuildingType.QUARRY, QUARRY_WORKER_NAME, quarry.size())) return;
            if (tryFillJob(server, level, data, BuildingType.MINE, MINE_WORKER_NAME, mine.size())) return;
        }

        BuildingRecord missingWorkshop = SettlementWorkshopService.firstMissingLoadedAssignment(level, data);
        if (missingWorkshop != null) {
            if (!arrivalFoodAvailable(level, data)) return;
            FrontierWorkerEntity arrival = SettlementWorkshopService.spawnAssignedWorker(level, data, missingWorkshop);
            commitArrival(server, level, data, arrival);
            return;
        }

        BuildingRecord missingAdvanced = SettlementAdvancedWorkshopService.firstMissingLoadedAssignment(level, data);
        if (missingAdvanced != null) {
            if (!arrivalFoodAvailable(level, data)) return;
            FrontierWorkerEntity arrival = SettlementAdvancedWorkshopService.spawnAssignedWorker(level, data, missingAdvanced);
            commitArrival(server, level, data, arrival);
            return;
        }

        OutpostRecord missing = SettlementOutpostLogisticsService.firstMissingLoadedAssignment(level, data);
        if (missing != null) {
            if (!arrivalFoodAvailable(level, data)) return;
            FrontierWorkerEntity arrival = SettlementOutpostLogisticsService.spawnAssignedWorker(level, data, missing);
            commitArrival(server, level, data, arrival);
        }
    }

    private static boolean localProductionEvidenceLoaded(ServerLevel level, SettlementData data) {
        if (!SettlementStorageService.storageAvailable(level, data)) return false;
        for (BuildingRecord building : data.buildings()) {
            BuildingType type = building.buildingType();
            if (type != BuildingType.LUMBER_CAMP && type != BuildingType.FARM
                    && type != BuildingType.QUARRY && type != BuildingType.MINE) continue;
            if (!workerRouteEvidenceLoaded(level, data, building.workCenter(), resourceRouteMargin(type))) return false;
        }
        return true;
    }

    /**
     * Loaded-only visibility proof for one local civilian lifecycle envelope.
     *
     * Alpha.68 deliberately includes every real place the town routine can send the worker:
     * work target, concrete settlement storage, and every completed HOUSE rest footprint. The exact
     * same AABB is also used by assignment/entity lookup, so a worker sleeping in an unloaded house
     * cannot become an "unloaded == dead" false negative. Only hasChunkAt is used; no chunk is loaded.
     */
    static boolean workerRouteEvidenceLoaded(ServerLevel level, SettlementData data,
                                             BlockPos workCenter, int margin) {
        if (!SettlementStorageService.storageAvailable(level, data)) return false;
        return workerBoundsFullyLoaded(level, data, workerRouteBounds(data, workCenter, margin));
    }

    static AABB workerRouteBounds(SettlementData data, BlockPos workCenter, int margin) {
        int minX = workCenter.getX() - margin;
        int maxX = workCenter.getX() + margin;
        int minZ = workCenter.getZ() - margin;
        int maxZ = workCenter.getZ() + margin;
        for (BlockPos storage : SettlementStorageService.storagePositions(data)) {
            minX = Math.min(minX, storage.getX() - margin);
            maxX = Math.max(maxX, storage.getX() + margin);
            minZ = Math.min(minZ, storage.getZ() - margin);
            maxZ = Math.max(maxZ, storage.getZ() + margin);
        }
        // Historical routines could leave a town worker at a completed house. Keep houses in the
        // legal lookup/evidence envelope so a sleeping/stranded loaded worker cannot disappear from
        // assignment accounting merely because it is no longer next to its production building.
        for (BuildingRecord building : data.buildings()) {
            if (building.buildingType() != BuildingType.HOUSE) continue;
            BlockPos rest = building.workCenter();
            minX = Math.min(minX, rest.getX() - margin);
            maxX = Math.max(maxX, rest.getX() + margin);
            minZ = Math.min(minZ, rest.getZ() - margin);
            maxZ = Math.max(maxZ, rest.getZ() + margin);
        }
        double minY = data.centerPos().getY() - 96.0D;
        double maxY = data.centerPos().getY() + 97.0D;
        return new AABB(minX, minY, minZ, maxX + 1.0D, maxY, maxZ + 1.0D);
    }

    private static boolean workerBoundsFullyLoaded(ServerLevel level, SettlementData data, AABB bounds) {
        int minX = (int) Math.floor(bounds.minX);
        int minChunkX = Math.floorDiv(minX, 16);
        int maxX = (int) Math.floor(Math.nextDown(bounds.maxX));
        int maxChunkX = Math.floorDiv(maxX, 16);
        int minZ = (int) Math.floor(bounds.minZ);
        int minChunkZ = Math.floorDiv(minZ, 16);
        int maxZ = (int) Math.floor(Math.nextDown(bounds.maxZ));
        int maxChunkZ = Math.floorDiv(maxZ, 16);
        int probeY = data.centerPos().getY();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                BlockPos probe = new BlockPos(chunkX * 16 + 8, probeY, chunkZ * 16 + 8);
                if (!level.hasChunkAt(probe)) return false;
            }
        }
        return true;
    }

    private static boolean tryFillJob(MinecraftServer server, ServerLevel level, SettlementData data,
                                      BuildingType type, String workerName, int existingWorkers) {
        List<BuildingRecord> available = buildings(data, type);
        if (existingWorkers >= available.size()) return false;
        BuildingRecord target = available.get(existingWorkers);
        if (!level.hasChunkAt(target.workCenter())) return true;
        if (!arrivalFoodAvailable(level, data)) return true;
        FrontierWorkerEntity arrival = spawnWorker(level, target.workCenter(), workerName);
        commitArrival(server, level, data, arrival);
        return true;
    }

    private static boolean arrivalFoodAvailable(ServerLevel level, SettlementData data) {
        if (!SettlementStorageService.storageAvailable(level, data)) return false;
        return SettlementStorageService.scan(level, data).food() >= ARRIVAL_FOOD_COST;
    }

    private static boolean consumeArrivalFood(ServerLevel level, SettlementData data) {
        return SettlementStorageService.consume(level, data, 0L, 0L, ARRIVAL_FOOD_COST);
    }

    private static boolean commitArrival(MinecraftServer server, ServerLevel level,
                                         SettlementData data, FrontierWorkerEntity arrival) {
        if (arrival == null) return false;
        if (!consumeArrivalFood(level, data)) {
            arrival.discard();
            return false;
        }
        finishArrival(server, data);
        return true;
    }

    private static void finishArrival(MinecraftServer server, SettlementData data) {
        data.addPopulation(1);
        SettlementService.refreshResources(server, data);
        SettlementService.broadcast(server, data);
    }

    private static FrontierWorkerEntity spawnWorker(ServerLevel level, BlockPos spawn, String name) {
        if (!level.hasChunkAt(spawn)) return null;
        FrontierWorkerEntity worker = new FrontierWorkerEntity(FrontierContent.FRONTIER_WORKER.get(), level);
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setCustomName(Component.literal(name));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        worker.addTag(RESOURCE_WORKER_TAG);
        if (!level.addFreshEntity(worker)) return null;
        return worker;
    }

    /**
     * Frontier-managed local civilians can carry the only physical copy of harvested/staged cargo.
     * Remove vanilla equipment-drop randomness and expose that exact MAINHAND stack once.
     * The road transporter keeps its separate Alpha.63 handler and is explicitly excluded here.
     */
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof FrontierWorkerEntity worker)) return;
        if (worker.entityTags().contains(SettlementOutpostLogisticsService.TRANSPORT_WORKER_TAG)) return;
        if (!isManagedCargoWorker(worker)) return;
        ItemStack carried = worker.getMainHandItem();
        event.getDrops().clear();
        if (carried.isEmpty()) return;
        event.getDrops().add(new ItemEntity(
                worker.level(), worker.getX(), worker.getY(), worker.getZ(), carried.copy()));
    }

    private static boolean isManagedCargoWorker(FrontierWorkerEntity worker) {
        if (worker.entityTags().contains(RESOURCE_WORKER_TAG)
                || worker.entityTags().contains(SettlementConstructionService.BUILDER_TAG)
                || worker.entityTags().contains(SettlementFishingOutpostService.FISHING_WORKER_TAG)
                || worker.entityTags().contains(SettlementConstructionOfficeService.SUPPLY_RUNNER_TAG)
                || worker.entityTags().contains(SettlementWorkshopService.WORKSHOP_WORKER_TAG)
                || worker.entityTags().contains(SettlementAdvancedWorkshopService.ADVANCED_WORKER_TAG)
                || worker.entityTags().contains(SettlementOutpostProductionService.PRODUCTION_WORKER_TAG)
                || worker.entityTags().contains(SettlementConstructionOfficeService.SUPPLY_RUNNER_TAG)) {
            return true;
        }
        // Save-compatible fallback for pre-Alpha.65 ordinary workers that did not yet carry a role tag.
        Component name = worker.getCustomName();
        if (name == null) return false;
        String value = name.getString();
        return LUMBER_WORKER_NAME.equals(value) || FARM_WORKER_NAME.equals(value)
                || QUARRY_WORKER_NAME.equals(value) || MINE_WORKER_NAME.equals(value)
                || value.startsWith("전초 벌목 주민 #") || value.startsWith("전초 채석 주민 #")
                || value.startsWith("전초 광산 주민 #") || value.startsWith("전초 농업 주민 #");
    }

    private static void workLumber(ServerLevel level, SettlementData data,
                                   FrontierWorkerEntity worker, BuildingRecord camp) {
        if (tryExportWorksiteBuffer(level, data, worker, camp)) return;
        ItemStack carried = worker.getMainHandItem();
        Item expected = carried.isEmpty() ? null : carried.getItem();
        if (!carried.isEmpty() && carried.getCount() >= cargoLimit(carried)) {
            deliverToWorksiteStorage(level, data, worker, camp, carried);
            return;
        }
        BlockPos target = findTreeForWorker(level, data, worker, camp.workCenter(), expected);
        if (target == null) {
            if (!carried.isEmpty()) deliverToWorksiteStorage(level, data, worker, camp, carried);
            else moveNear(level, worker, camp.workCenter(), 0.82D);
            return;
        }
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > 8.0D) {
            moveNear(level, worker, target, 0.92D);
            return;
        }
        if (!workDue(level, camp, LUMBER_WORK_PERIOD_TICKS)) return;
        Item item = level.getBlockState(target).getBlock().asItem();
        int room = cargoRoom(worker, item);
        if (room <= 0) {
            deliverToWorksiteStorage(level, data, worker, camp, carried);
            return;
        }
        ItemStack harvested = harvestVerticalTrunk(level, data, target, item, Math.min(MAX_LOGS_PER_WORK, room));
        if (!harvested.isEmpty() && appendCargo(worker, harvested)) worker.swing(InteractionHand.MAIN_HAND);
    }

    private static void workFarm(ServerLevel level, SettlementData data,
                                 FrontierWorkerEntity worker, BuildingRecord farm) {
        if (tryExportWorksiteBuffer(level, data, worker, farm)) return;
        ItemStack carried = worker.getMainHandItem();
        if (!carried.isEmpty() && !carried.is(Items.WHEAT)) {
            deliverToWorksiteStorage(level, data, worker, farm, carried);
            return;
        }
        if (!carried.isEmpty() && carried.getCount() >= cargoLimit(carried)) {
            deliverToWorksiteStorage(level, data, worker, farm, carried);
            return;
        }
        if (worker.distanceToSqr(farm.workCenter().getX() + 0.5D, farm.workCenter().getY(), farm.workCenter().getZ() + 0.5D) > 64.0D) {
            moveNear(level, worker, farm.workCenter(), 0.88D);
            return;
        }
        if (!workDue(level, farm, FARM_WORK_PERIOD_TICKS)) return;
        BuildingType type = farm.buildingType();
        if (type == null) return;
        int room = cargoRoom(worker, Items.WHEAT);
        int harvested = 0;
        int replanted = 0;
        for (int x = 0; x < type.width(); x++) {
            for (int z = 0; z < type.depth(); z++) {
                BlockPos crop = farm.localToWorld(x, 1, z);
                if (!level.hasChunkAt(crop) || !level.hasChunkAt(crop.below())) continue;
                BlockState state = level.getBlockState(crop);
                BlockState soil = level.getBlockState(crop.below());
                if (state.isAir() && soil.is(Blocks.FARMLAND)) {
                    if (level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3)) replanted++;
                    continue;
                }
                if (harvested >= room || !state.is(Blocks.WHEAT)
                        || !state.hasProperty(BlockStateProperties.AGE_7)
                        || state.getValue(BlockStateProperties.AGE_7) < 7) continue;
                if (level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3)) harvested++;
            }
        }
        if (harvested > 0) {
            if (appendCargo(worker, new ItemStack(Items.WHEAT, harvested))) worker.swing(InteractionHand.MAIN_HAND);
            return;
        }
        if (!worker.getMainHandItem().isEmpty() && replanted == 0) {
            deliverToWorksiteStorage(level, data, worker, farm, worker.getMainHandItem());
        }
    }

    private static void workQuarry(ServerLevel level, SettlementData data,
                                   FrontierWorkerEntity worker, BuildingRecord quarry) {
        if (tryExportWorksiteBuffer(level, data, worker, quarry)) return;
        ItemStack carried = worker.getMainHandItem();
        Item expected = carried.isEmpty() ? null : carried.getItem();
        if (!carried.isEmpty() && carried.getCount() >= cargoLimit(carried)) {
            deliverToWorksiteStorage(level, data, worker, quarry, carried);
            return;
        }
        BlockPos target = findQuarryTargetForWorker(level, data, worker, quarry.workCenter(), expected);
        if (target == null) {
            if (!carried.isEmpty()) deliverToWorksiteStorage(level, data, worker, quarry, carried);
            else moveNear(level, worker, quarry.workCenter(), 0.82D);
            return;
        }
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D) > 9.0D) {
            moveNear(level, worker, target, 0.90D);
            return;
        }
        if (!workDue(level, quarry, QUARRY_WORK_PERIOD_TICKS)) return;
        Item item = level.getBlockState(target).getBlock().asItem();
        int room = cargoRoom(worker, item);
        if (room <= 0) {
            deliverToWorksiteStorage(level, data, worker, quarry, carried);
            return;
        }
        ItemStack stone = harvestStoneCluster(level, data, target, item, Math.min(MAX_STONE_PER_WORK, room));
        if (!stone.isEmpty() && appendCargo(worker, stone)) worker.swing(InteractionHand.MAIN_HAND);
    }

    private static void workMine(ServerLevel level, SettlementData data,
                                 FrontierWorkerEntity worker, BuildingRecord mine) {
        if (tryExportWorksiteBuffer(level, data, worker, mine)) return;
        ItemStack carried = worker.getMainHandItem();
        if (!carried.isEmpty() && carried.getCount() >= cargoLimit(carried)) {
            deliverToWorksiteStorage(level, data, worker, mine, carried);
            return;
        }
        BlockPos work = mine.workCenter();
        if (worker.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D) > 16.0D) {
            moveNear(level, worker, work, 0.86D);
            return;
        }
        if (!workDue(level, mine, MINING_WORK_PERIOD_TICKS)) return;
        Item expected = carried.isEmpty() ? null : carried.getItem();
        BlockPos ore = findOreBelow(level, data, work, expected);
        if (ore == null) {
            if (!carried.isEmpty()) deliverToWorksiteStorage(level, data, worker, mine, carried);
            return;
        }
        ItemStack preview = previewMineDrop(level.getBlockState(ore));
        if (preview.isEmpty()) return;
        int room = cargoRoom(worker, preview.getItem());
        if (room <= 0) {
            deliverToWorksiteStorage(level, data, worker, mine, carried);
            return;
        }
        ItemStack mined = mineOre(level, ore, room);
        if (!mined.isEmpty() && appendCargo(worker, mined)) worker.swing(InteractionHand.MAIN_HAND);
    }

    /**
     * Profession barrels remain visible local buffers, but they are not dead-end economy silos.
     * Once ordinary harvesting has staged a stack there, that same worker takes the physical stack
     * and walks it to shared/general town storage. The export tag distinguishes this cargo from a
     * freshly harvested stack so it cannot accidentally resume harvesting on the trip to town.
     */
    private static boolean tryExportWorksiteBuffer(ServerLevel level, SettlementData data,
                                                   FrontierWorkerEntity worker, BuildingRecord building) {
        if (worker.entityTags().contains(WORKSITE_EXPORT_TAG)) {
            ItemStack exporting = worker.getMainHandItem();
            if (exporting.isEmpty()) {
                worker.removeTag(WORKSITE_EXPORT_TAG);
                return false;
            }
            deliverToTownStorage(level, data, worker, exporting);
            if (worker.getMainHandItem().isEmpty()) worker.removeTag(WORKSITE_EXPORT_TAG);
            return true;
        }
        if (!worker.getMainHandItem().isEmpty()) return false;

        BlockPos local = SettlementStorageService.worksiteStoragePosition(building);
        if (local == null || !level.hasChunkAt(local) || !level.getBlockState(local).is(Blocks.BARREL)) return false;
        if (!(level.getBlockEntity(local) instanceof Container container)) return false;
        if (!hasExportableWorksiteOutput(building.buildingType(), container)) return false;

        if (worker.distanceToSqr(local.getX() + 0.5D, local.getY() + 0.5D, local.getZ() + 0.5D) > 9.0D) {
            moveNear(level, worker, local, 0.86D);
            return true;
        }

        ItemStack staged = SettlementStorageService.extract(
                level, local, stack -> isExportableWorksiteOutput(building.buildingType(), stack), PRODUCTION_HAUL_STACK);
        if (staged.isEmpty()) return false;
        worker.setItemSlot(EquipmentSlot.MAINHAND, staged);
        worker.addTag(WORKSITE_EXPORT_TAG);
        worker.getNavigation().stop();
        return true;
    }

    private static boolean hasExportableWorksiteOutput(BuildingType type, Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (isExportableWorksiteOutput(type, container.getItem(slot))) return true;
        }
        return false;
    }

    private static boolean isExportableWorksiteOutput(BuildingType type, ItemStack stack) {
        if (stack == null || stack.isEmpty() || type == null) return false;
        return switch (type) {
            // Managed worksite barrels are visible physical buffers and players can interact with them.
            // Export only items that this profession can actually create; never vacuum arbitrary player
            // storage merely because it happens to sit in a managed barrel.
            case LUMBER_CAMP -> stack.is(ItemTags.LOGS);
            case FARM -> stack.is(Items.WHEAT);
            case QUARRY -> isQuarryOutputItem(stack);
            case MINE -> isMineOutputItem(stack);
            default -> false;
        };
    }

    private static boolean isQuarryOutputItem(ItemStack stack) {
        return stack.is(Items.STONE) || stack.is(Items.DEEPSLATE) || stack.is(Items.ANDESITE)
                || stack.is(Items.DIORITE) || stack.is(Items.GRANITE) || stack.is(Items.TUFF);
    }

    private static boolean isMineOutputItem(ItemStack stack) {
        if (stack.is(Items.RAW_IRON) || stack.is(Items.RAW_COPPER) || stack.is(Items.RAW_GOLD)
                || stack.is(Items.COAL) || stack.is(Items.DIAMOND) || stack.is(Items.EMERALD)
                || stack.is(Items.REDSTONE) || stack.is(Items.LAPIS_LAZULI)) return true;
        // Unknown companion ores are mined as their ore-block item by previewMineDrop(). Preserve that
        // compatibility without treating unrelated blocks, tools, food or equipment as mine output.
        return stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock().defaultBlockState().is(Tags.Blocks.ORES);
    }

    private static int cargoLimit(ItemStack stack) {
        return Math.min(PRODUCTION_HAUL_STACK, stack.getMaxStackSize());
    }

    private static int cargoRoom(FrontierWorkerEntity worker, Item item) {
        ItemStack carried = worker.getMainHandItem();
        if (carried.isEmpty()) {
            ItemStack probe = new ItemStack(item);
            return Math.min(PRODUCTION_HAUL_STACK, probe.getMaxStackSize());
        }
        if (carried.getItem() != item) return 0;
        return Math.max(0, cargoLimit(carried) - carried.getCount());
    }

    private static boolean appendCargo(FrontierWorkerEntity worker, ItemStack gained) {
        if (gained.isEmpty()) return false;
        ItemStack carried = worker.getMainHandItem();
        if (carried.isEmpty()) {
            int amount = Math.min(gained.getCount(), Math.min(PRODUCTION_HAUL_STACK, gained.getMaxStackSize()));
            if (amount <= 0) return false;
            worker.setItemSlot(EquipmentSlot.MAINHAND, gained.copyWithCount(amount));
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(carried, gained)) return false;
        int amount = Math.min(gained.getCount(), cargoLimit(carried) - carried.getCount());
        if (amount <= 0) return false;
        carried.grow(amount);
        worker.setItemSlot(EquipmentSlot.MAINHAND, carried);
        return true;
    }

    private static boolean workDue(ServerLevel level, BuildingRecord building, int periodTicks) {
        long periodSlots = Math.max(1L, periodTicks / 10L);
        long currentSlot = level.getGameTime() / 10L;
        long salt = (long) building.originX() * 31L + (long) building.originY() * 7L + (long) building.originZ() * 17L;
        return Math.floorMod(currentSlot + salt, periodSlots) == 0L;
    }

    private static void deliverToWorksiteStorage(ServerLevel level, SettlementData data,
                                                 FrontierWorkerEntity worker, BuildingRecord building,
                                                 ItemStack carried) {
        if (carried.isEmpty()) return;
        BlockPos local = SettlementStorageService.worksiteStoragePosition(building);
        if (local != null && level.hasChunkAt(local) && level.getBlockState(local).is(Blocks.BARREL)
                && SettlementStorageService.hasRoomAt(level, local, carried) && !isTargetBlocked(level, worker, local)) {
            if (worker.distanceToSqr(local.getX() + 0.5D, local.getY() + 0.5D, local.getZ() + 0.5D) > 9.0D) {
                if (moveNear(level, worker, local, 0.86D)) return;
            } else {
                ItemStack remaining = SettlementStorageService.insertAt(level, local, carried);
                worker.setItemSlot(EquipmentSlot.MAINHAND, remaining);
                clearTargetIfEmpty(worker);
                return;
            }
        }
        deliverToTownStorage(level, data, worker, carried);
    }

    private static void deliverToTownStorage(ServerLevel level, SettlementData data,
                                             FrontierWorkerEntity worker, ItemStack carried) {
        Set<BlockPos> excluded = new HashSet<>();
        Map<BlockPos, Long> blocked = BLOCKED_TARGETS.get(worker.getUUID());
        if (blocked != null) {
            long now = level.getGameTime();
            for (Map.Entry<BlockPos, Long> entry : blocked.entrySet()) {
                if (entry.getValue() > now) excluded.add(entry.getKey());
            }
        }
        BlockPos target = SettlementStorageService.findProductionDepositTargetExcluding(level, data, carried, excluded);
        if (target == null || !level.hasChunkAt(target) || !SettlementStorageService.hasRoomAt(level, target, carried)) {
            worker.getNavigation().stop();
            return;
        }
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D) > 9.0D) {
            moveNear(level, worker, target, 0.85D);
            return;
        }
        ItemStack remaining = SettlementStorageService.insertAt(level, target, carried);
        worker.setItemSlot(EquipmentSlot.MAINHAND, remaining);
        clearTargetIfEmpty(worker);
    }

    /**
     * PathfinderMob has no villager POI/interaction brain to turn a solid target block into a usable
     * standing position. Never path to a log, quarry block, barrel, or fenced work-center directly:
     * try nearby loaded walkable cells and let vanilla ground navigation choose a real path.
     */
    private static boolean moveNear(ServerLevel level, FrontierWorkerEntity worker, BlockPos target, double speed) {
        if (isTargetBlocked(level, worker, target)) {
            worker.getNavigation().stop();
            return false;
        }
        if (watchMovement(level, worker, target)) return false;
        if (!worker.getNavigation().isDone()) return true;

        int[] dyOrder = {0, 1, -1, 2, -2, 3, -3};
        List<BlockPos> approaches = new ArrayList<>();
        for (int radius = 1; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int dy : dyOrder) {
                        BlockPos approach = target.offset(dx, dy, dz);
                        if (isWalkableApproach(level, approach)) approaches.add(approach);
                    }
                }
            }
        }
        approaches.sort(Comparator.comparingDouble(approach ->
                worker.distanceToSqr(approach.getX() + 0.5D, approach.getY(), approach.getZ() + 0.5D)));
        int tried = 0;
        for (BlockPos approach : approaches) {
            if (++tried > MAX_APPROACH_PATH_TRIES) break;
            if (worker.distanceToSqr(approach.getX() + 0.5D, approach.getY(), approach.getZ() + 0.5D) <= 1.0D) {
                worker.getNavigation().stop();
                MOVEMENT_WATCHES.remove(worker.getUUID());
                return true;
            }
            Path path = worker.getNavigation().createPath(approach, 0);
            if (path == null || !path.canReach()) continue;
            if (worker.getNavigation().moveTo(path, speed)) {
                MOVEMENT_WATCHES.put(worker.getUUID(), new MovementWatch(
                        target.immutable(), worker.getX(), worker.getY(), worker.getZ(), level.getGameTime()));
                return true;
            }
        }
        blockTarget(level, worker, target);
        worker.getNavigation().stop();
        clearResourceTarget(worker);
        return false;
    }

    private static boolean watchMovement(ServerLevel level, FrontierWorkerEntity worker, BlockPos target) {
        MovementWatch watch = MOVEMENT_WATCHES.get(worker.getUUID());
        if (watch == null || !watch.target().equals(target)) return false;
        double dx = worker.getX() - watch.x();
        double dy = worker.getY() - watch.y();
        double dz = worker.getZ() - watch.z();
        if (dx * dx + dy * dy + dz * dz >= 0.0625D) {
            MOVEMENT_WATCHES.put(worker.getUUID(), new MovementWatch(
                    target.immutable(), worker.getX(), worker.getY(), worker.getZ(), level.getGameTime()));
            return false;
        }
        if (level.getGameTime() - watch.lastProgressTick() < STUCK_PROGRESS_TIMEOUT_TICKS) return false;
        blockTarget(level, worker, target);
        worker.getNavigation().stop();
        MOVEMENT_WATCHES.remove(worker.getUUID());
        clearResourceTarget(worker);
        return true;
    }

    private static void blockTarget(ServerLevel level, FrontierWorkerEntity worker, BlockPos target) {
        BLOCKED_TARGETS.computeIfAbsent(worker.getUUID(), ignored -> new HashMap<>())
                .put(target.immutable(), level.getGameTime() + BLOCKED_TARGET_RETRY_TICKS);
    }

    private static boolean isTargetBlocked(ServerLevel level, FrontierWorkerEntity worker, BlockPos target) {
        Map<BlockPos, Long> blocked = BLOCKED_TARGETS.get(worker.getUUID());
        if (blocked == null) return false;
        long now = level.getGameTime();
        blocked.entrySet().removeIf(entry -> entry.getValue() <= now);
        if (blocked.isEmpty()) {
            BLOCKED_TARGETS.remove(worker.getUUID());
            return false;
        }
        return blocked.getOrDefault(target, 0L) > now;
    }

    private static void clearResourceTarget(FrontierWorkerEntity worker) {
        RESOURCE_TARGETS.remove(worker.getUUID());
        RESOURCE_SEARCH_RETRY_AFTER.remove(worker.getUUID());
    }

    private static void clearTargetIfEmpty(FrontierWorkerEntity worker) {
        if (worker.getMainHandItem().isEmpty()) clearResourceTarget(worker);
        MOVEMENT_WATCHES.remove(worker.getUUID());
    }

    private static void clearTransientWorkerState(FrontierWorkerEntity worker) {
        clearResourceTarget(worker);
        MOVEMENT_WATCHES.remove(worker.getUUID());
        BLOCKED_TARGETS.remove(worker.getUUID());
    }

    private static boolean isWalkableApproach(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.above()) || !level.hasChunkAt(pos.below())) return false;
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());
        if (level.getBlockEntity(pos) != null || level.getBlockEntity(pos.above()) != null) return false;
        if (!feet.getFluidState().isEmpty() || !head.getFluidState().isEmpty() || !below.getFluidState().isEmpty()) return false;
        if ((!feet.isAir() && !feet.canBeReplaced()) || (!head.isAir() && !head.canBeReplaced())) return false;
        return !below.isAir() && !below.canBeReplaced();
    }

    private static BlockPos findTreeForWorker(ServerLevel level, SettlementData data,
                                                  FrontierWorkerEntity worker, BlockPos center, Item expected) {
        java.util.UUID id = worker.getUUID();
        long now = level.getGameTime();
        CachedTarget cached = RESOURCE_TARGETS.get(id);
        if (cached != null && cached.expiresAt() > now && isValidTreeTarget(level, data, worker, cached.pos(), expected)) {
            return cached.pos();
        }
        RESOURCE_TARGETS.remove(id);
        if (RESOURCE_SEARCH_RETRY_AFTER.getOrDefault(id, 0L) > now) return null;
        BlockPos target = findTree(level, data, worker, center, expected);
        if (target == null) {
            RESOURCE_SEARCH_RETRY_AFTER.put(id, now + RESOURCE_SEARCH_RETRY_TICKS);
            return null;
        }
        RESOURCE_SEARCH_RETRY_AFTER.remove(id);
        RESOURCE_TARGETS.put(id, new CachedTarget(target.immutable(), now + RESOURCE_TARGET_CACHE_TICKS));
        return target;
    }

    private static boolean isValidTreeTarget(ServerLevel level, SettlementData data, FrontierWorkerEntity worker,
                                             BlockPos pos, Item expected) {
        if (!level.hasChunkAt(pos) || isTargetBlocked(level, worker, pos) || isProtected(data, pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (!state.is(BlockTags.LOGS)) return false;
        Item item = state.getBlock().asItem();
        return item != Items.AIR && (expected == null || item == expected)
                && isNaturalTreeBase(level, pos) && hasWalkableApproach(level, pos);
    }

    private static BlockPos findTree(ServerLevel level, SettlementData data, FrontierWorkerEntity worker,
                                     BlockPos center, Item expected) {
        List<TreeCandidate> candidates = new ArrayList<>();
        Set<BlockPos> seenBases = new HashSet<>();
        Map<Item, Integer> availableByItem = new HashMap<>();
        Map<Item, Double> nearestByItem = new HashMap<>();
        for (int radius = 0; radius <= TREE_SEARCH_RADIUS; radius++) {
            TreeCandidate nearestExpected = null;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    int x = center.getX() + dx;
                    int z = center.getZ() + dz;
                    for (int y = center.getY() - TREE_SEARCH_DOWN; y <= center.getY() + TREE_SEARCH_UP; y++) {
                        BlockPos probe = new BlockPos(x, y, z);
                        if (!level.hasChunkAt(probe)) continue;
                        BlockState state = level.getBlockState(probe);
                        if (!state.is(BlockTags.LOGS) || isProtected(data, probe) || !hasLeavesAbove(level, probe)) continue;
                        Item item = state.getBlock().asItem();
                        if (item == Items.AIR || (expected != null && item != expected)) continue;
                        BlockPos base = descendToTrunkBase(level, data, probe, item);
                        if (!seenBases.add(base) || isTargetBlocked(level, worker, base)
                                || !isNaturalTreeBase(level, base) || !hasWalkableApproach(level, base)) continue;
                        int availableLogs = countVerticalTrunk(level, data, base, item);
                        if (availableLogs <= 0) continue;
                        TreeCandidate candidate = new TreeCandidate(base, item, base.distSqr(center), availableLogs);
                        if (expected != null) {
                            if (nearestExpected == null || candidate.distance() < nearestExpected.distance()) nearestExpected = candidate;
                        } else {
                            candidates.add(candidate);
                            availableByItem.merge(item, availableLogs, Integer::sum);
                            nearestByItem.merge(item, candidate.distance(), Math::min);
                        }
                        break;
                    }
                }
            }
            if (nearestExpected != null) return nearestExpected.base();
            if (expected == null) {
                Item ready = preferredTreeItem(availableByItem, nearestByItem, PRODUCTION_HAUL_STACK);
                if (ready != null) return nearestCandidate(candidates, ready);
            }
        }
        if (candidates.isEmpty()) return null;
        Item preferred = preferredTreeItem(availableByItem, nearestByItem, 0);
        return preferred == null ? null : nearestCandidate(candidates, preferred);
    }

    private static Item preferredTreeItem(Map<Item, Integer> availableByItem, Map<Item, Double> nearestByItem,
                                          int minimumLogs) {
        Item preferred = null;
        int bestLogs = -1;
        double bestNearest = Double.MAX_VALUE;
        for (Map.Entry<Item, Integer> entry : availableByItem.entrySet()) {
            if (entry.getValue() < minimumLogs) continue;
            double nearest = nearestByItem.getOrDefault(entry.getKey(), Double.MAX_VALUE);
            if (entry.getValue() > bestLogs || (entry.getValue() == bestLogs && nearest < bestNearest)) {
                preferred = entry.getKey();
                bestLogs = entry.getValue();
                bestNearest = nearest;
            }
        }
        return preferred;
    }

    private static BlockPos nearestCandidate(List<TreeCandidate> candidates, Item preferred) {
        TreeCandidate best = null;
        for (TreeCandidate candidate : candidates) {
            if (candidate.item() != preferred) continue;
            if (best == null || candidate.distance() < best.distance()) best = candidate;
        }
        return best == null ? null : best.base();
    }

    private static BlockPos descendToTrunkBase(ServerLevel level, SettlementData data, BlockPos start, Item item) {
        BlockPos base = start;
        for (int depth = 0; depth < 24; depth++) {
            BlockPos below = base.below();
            if (!level.hasChunkAt(below) || isProtected(data, below)) break;
            BlockState state = level.getBlockState(below);
            if (!state.is(BlockTags.LOGS) || state.getBlock().asItem() != item) break;
            base = below;
        }
        return base;
    }

    private static int countVerticalTrunk(ServerLevel level, SettlementData data, BlockPos base, Item item) {
        int count = 0;
        for (int y = 0; y < 24; y++) {
            BlockPos pos = base.above(y);
            if (!level.hasChunkAt(pos) || isProtected(data, pos)) break;
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LOGS) || state.getBlock().asItem() != item) break;
            count++;
        }
        return count;
    }

    private static boolean isNaturalTreeBase(ServerLevel level, BlockPos base) {
        if (!level.hasChunkAt(base.below())) return false;
        BlockState below = level.getBlockState(base.below());
        return below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.DIRT) || below.is(Blocks.COARSE_DIRT)
                || below.is(Blocks.PODZOL) || below.is(Blocks.ROOTED_DIRT) || below.is(Blocks.MOSS_BLOCK)
                || below.is(Blocks.MYCELIUM) || below.is(Blocks.MUD);
    }

    private static boolean hasWalkableApproach(ServerLevel level, BlockPos target) {
        int[] dyOrder = {0, 1, -1, 2, -2, 3, -3};
        for (int radius = 1; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int dy : dyOrder) {
                        if (isWalkableApproach(level, target.offset(dx, dy, dz))) return true;
                    }
                }
            }
        }
        return false;
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

    private static ItemStack harvestVerticalTrunk(ServerLevel level, SettlementData data, BlockPos base,
                                                  Item expected, int maxCount) {
        if (maxCount <= 0 || !level.hasChunkAt(base)) return ItemStack.EMPTY;
        BlockState first = level.getBlockState(base);
        if (!first.is(BlockTags.LOGS)) return ItemStack.EMPTY;
        Item item = first.getBlock().asItem();
        if (item == Items.AIR || (expected != null && item != expected)) return ItemStack.EMPTY;
        int count = 0;
        for (int y = 0; y < 16 && count < maxCount; y++) {
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
        return count == 0 ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static BlockPos findQuarryTargetForWorker(ServerLevel level, SettlementData data,
                                                           FrontierWorkerEntity worker, BlockPos center, Item expected) {
        java.util.UUID id = worker.getUUID();
        long now = level.getGameTime();
        CachedTarget cached = RESOURCE_TARGETS.get(id);
        if (cached != null && cached.expiresAt() > now && level.hasChunkAt(cached.pos())
                && !isTargetBlocked(level, worker, cached.pos()) && !isProtected(data, cached.pos())) {
            BlockState state = level.getBlockState(cached.pos());
            Item item = state.getBlock().asItem();
            if (isQuarryStone(state) && item != Items.AIR && (expected == null || item == expected)
                    && level.getBlockState(cached.pos().above()).isAir() && hasWalkableApproach(level, cached.pos())) {
                return cached.pos();
            }
        }
        RESOURCE_TARGETS.remove(id);
        if (RESOURCE_SEARCH_RETRY_AFTER.getOrDefault(id, 0L) > now) return null;
        BlockPos target = findExposedStone(level, data, worker, center, QUARRY_SEARCH_RADIUS, expected);
        if (target == null) {
            RESOURCE_SEARCH_RETRY_AFTER.put(id, now + RESOURCE_SEARCH_RETRY_TICKS);
            return null;
        }
        RESOURCE_SEARCH_RETRY_AFTER.remove(id);
        RESOURCE_TARGETS.put(id, new CachedTarget(target.immutable(), now + RESOURCE_TARGET_CACHE_TICKS));
        return target;
    }

    private static BlockPos findExposedStone(ServerLevel level, SettlementData data, FrontierWorkerEntity worker,
                                             BlockPos center, int radiusLimit, Item expected) {
        for (int radius = 0; radius <= radiusLimit; radius++) {
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int y = center.getY() - QUARRY_SEARCH_DOWN; y <= center.getY() + QUARRY_SEARCH_UP; y++) {
                        BlockPos pos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                        if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.above())) continue;
                        BlockState state = level.getBlockState(pos);
                        Item item = state.getBlock().asItem();
                        if (!isQuarryStone(state) || item == Items.AIR || (expected != null && item != expected)
                                || isProtected(data, pos) || isTargetBlocked(level, worker, pos)
                                || !level.getBlockState(pos.above()).isAir() || !hasWalkableApproach(level, pos)) continue;
                        double distance = pos.distSqr(center);
                        if (distance < bestDistance) { best = pos; bestDistance = distance; }
                    }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    private static ItemStack harvestStoneCluster(ServerLevel level, SettlementData data, BlockPos base,
                                                 Item expected, int maxCount) {
        if (maxCount <= 0 || !level.hasChunkAt(base)) return ItemStack.EMPTY;
        BlockState first = level.getBlockState(base);
        if (!isQuarryStone(first)) return ItemStack.EMPTY;
        Item item = first.getBlock().asItem();
        if (item == Items.AIR || (expected != null && item != expected)) return ItemStack.EMPTY;
        int count = 0;
        for (int dx = -2; dx <= 2 && count < maxCount; dx++) {
            for (int dz = -2; dz <= 2 && count < maxCount; dz++) {
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

    private static BlockPos findOreBelow(ServerLevel level, SettlementData data, BlockPos center, Item expected) {
        for (int depth = 2; depth <= MINE_SEARCH_DEPTH; depth++) {
            int y = center.getY() - depth;
            for (int radius = 0; radius <= MINE_HORIZONTAL_SEARCH_RADIUS; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                        BlockPos pos = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                        if (!level.hasChunkAt(pos)) continue;
                        BlockState state = level.getBlockState(pos);
                        if (!state.is(Tags.Blocks.ORES) || isProtected(data, pos)) continue;
                        ItemStack preview = previewMineDrop(state);
                        if (preview.isEmpty() || (expected != null && preview.getItem() != expected)) continue;
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private static ItemStack previewMineDrop(BlockState state) {
        if (!state.is(Tags.Blocks.ORES)) return ItemStack.EMPTY;
        if (state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE)) return new ItemStack(Items.RAW_IRON);
        if (state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE)) return new ItemStack(Items.RAW_COPPER, 2);
        if (state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE)) return new ItemStack(Items.RAW_GOLD);
        if (state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE)) return new ItemStack(Items.COAL);
        if (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)) return new ItemStack(Items.DIAMOND);
        if (state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE)) return new ItemStack(Items.EMERALD);
        if (state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_REDSTONE_ORE)) return new ItemStack(Items.REDSTONE, 4);
        if (state.is(Blocks.LAPIS_ORE) || state.is(Blocks.DEEPSLATE_LAPIS_ORE)) return new ItemStack(Items.LAPIS_LAZULI, 4);
        Item item = state.getBlock().asItem();
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static ItemStack mineOre(ServerLevel level, BlockPos pos, int maxCount) {
        if (maxCount <= 0 || !level.hasChunkAt(pos)) return ItemStack.EMPTY;
        BlockState state = level.getBlockState(pos);
        ItemStack result = previewMineDrop(state);
        if (result.isEmpty()) return ItemStack.EMPTY;
        if (result.getCount() > maxCount) result = result.copyWithCount(maxCount);
        if (!level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3)) return ItemStack.EMPTY;
        return result;
    }

    private static boolean isProtected(SettlementData data, BlockPos pos) {
        if (pos.closerThan(data.stockpilePos(), 3.0D)) return true;
        for (BuildingRecord building : data.buildings()) if (building.protectsXZ(pos, 1)) return true;
        for (RoadSegment road : data.roads()) if (road.containsXZ(pos)) return true;
        for (OutpostRecord outpost : data.outposts()) if (outpost.protectsXZ(pos, 1)) return true;
        return false;
    }

    private static List<BuildingRecord> buildings(SettlementData data, BuildingType type) {
        List<BuildingRecord> result = new ArrayList<>();
        for (BuildingRecord building : data.buildings()) if (type.id().equals(building.type())) result.add(building);
        return result;
    }

    private static int resourceRouteMargin(BuildingType type) {
        if (type == BuildingType.LUMBER_CAMP) return TREE_SEARCH_RADIUS + 16;
        if (type == BuildingType.QUARRY) return QUARRY_SEARCH_RADIUS + 16;
        return LOCAL_RESOURCE_ROUTE_MARGIN;
    }

    private static List<FrontierWorkerEntity> workersByName(ServerLevel level, SettlementData data,
                                                BuildingType type, String name) {
        List<FrontierWorkerEntity> workers = new ArrayList<>();
        Set<java.util.UUID> ids = new HashSet<>();
        for (BuildingRecord building : buildings(data, type)) {
            AABB search = workerRouteBounds(data, building.workCenter(), resourceRouteMargin(type));
            for (FrontierWorkerEntity villager : level.getEntitiesOfClass(FrontierWorkerEntity.class, search,
                    candidate -> candidate.getCustomName() != null
                            && name.equals(candidate.getCustomName().getString()))) {
                if (ids.add(villager.getUUID())) workers.add(villager);
            }
        }
        workers.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        return workers;
    }
}
