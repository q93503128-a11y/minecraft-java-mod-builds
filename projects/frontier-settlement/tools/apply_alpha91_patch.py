from pathlib import Path
import json

ROOT = Path("projects/frontier-settlement")
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"


def read(path):
    return path.read_text(encoding="utf-8")


def write(path, text):
    path.write_text(text, encoding="utf-8")


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


props_path = ROOT / "gradle.properties"
props = read(props_path)
props = replace_once(props, "mod_version=0.1.0-alpha.90", "mod_version=0.1.0-alpha.91", "gradle version")
props = props.replace("\n# Alpha.90 maintenance recovery:", "\n# Alpha.91 navigation/storage hardening: expanded bounded resource search, complete-path water-safe routing, jobsite barrels, and four-barrel public stockpile.\n\n# Alpha.90 maintenance recovery:", 1)
write(props_path, props)

lock_path = ROOT / "COMPANION_LOCK.json"
lock = json.loads(read(lock_path))
if lock.get("target", {}).get("frontier_settlement") != "0.1.0-alpha.90":
    raise SystemExit("COMPANION_LOCK target is not Alpha.90")
lock["target"]["frontier_settlement"] = "0.1.0-alpha.91"
note = (
    "Alpha.91 keeps every Alpha.90 companion binary pin unchanged. Local production search envelopes "
    "expand substantially while target caching and miss cooldowns bound repeated scans; ground movement "
    "accepts only complete reachable vanilla paths, water is impassable for Frontier workers, and stuck "
    "targets are temporarily rejected instead of being reissued forever. Farm crop coordinates are now "
    "rotation-aware. Lumber, farm, quarry and mine workers prefer their own physical worksite barrel, with "
    "safe shared-storage fallback. The starter public stockpile grows into a non-destructive four-barrel "
    "cluster (up to 108 vanilla slots) and legacy completed worksites safely receive missing managed barrels "
    "without overwriting solid blocks or block entities. No force-load, teleport, virtual cargo, custom "
    "container protocol, second resident ledger, companion code copy, or destructive inventory migration is introduced."
)
if not any(str(x).startswith("Alpha.91 ") for x in lock.get("notes", [])):
    lock.setdefault("notes", []).append(note)
write(lock_path, json.dumps(lock, ensure_ascii=False, indent=2) + "\n")

# ---------------------------------------------------------------------------
# Building blueprint: lumber camp gets its own physical barrel.
# ---------------------------------------------------------------------------
blueprint_path = JAVA / "settlement/BuildingBlueprints.java"
blueprint = read(blueprint_path)
needle = "        b.put(5, 1, 3, Blocks.CRAFTING_TABLE.defaultBlockState(), Phase.FINISH);\n        b.put(2, 1, 5, Blocks.STRIPPED_OAK_LOG.defaultBlockState(), Phase.FINISH);"
replacement = "        b.put(5, 1, 3, Blocks.CRAFTING_TABLE.defaultBlockState(), Phase.FINISH);\n        b.put(5, 1, 6, Blocks.BARREL.defaultBlockState(), Phase.FINISH);\n        b.put(2, 1, 5, Blocks.STRIPPED_OAK_LOG.defaultBlockState(), Phase.FINISH);"
blueprint = replace_once(blueprint, needle, replacement, "lumber barrel blueprint")
write(blueprint_path, blueprint)

# ---------------------------------------------------------------------------
# Frontier worker body: water must be an impossible path type, not a shortcut.
# ---------------------------------------------------------------------------
entity_path = JAVA / "content/FrontierWorkerEntity.java"
entity = read(entity_path)
entity = replace_once(
    entity,
    "import net.minecraft.world.level.Level;\n",
    "import net.minecraft.world.level.Level;\nimport net.minecraft.world.level.pathfinder.PathType;\n",
    "worker PathType import",
)
entity = replace_once(
    entity,
    "        super(type, level);\n        setPersistenceRequired();\n",
    "        super(type, level);\n        setPersistenceRequired();\n        setPathfindingMalus(PathType.WATER, -1.0F);\n",
    "worker water malus",
)
write(entity_path, entity)

# ---------------------------------------------------------------------------
# Shared/jobsite storage: keep physical vanilla containers, grow capacity by
# a protected four-barrel public cluster, and expose rotation-aware job barrels.
# ---------------------------------------------------------------------------
storage_path = JAVA / "settlement/SettlementStorageService.java"
storage = read(storage_path)
storage = replace_once(
    storage,
    "import net.minecraft.world.item.Items;\n",
    "import net.minecraft.world.item.Items;\nimport net.minecraft.world.level.block.Blocks;\nimport net.minecraft.world.level.block.state.BlockState;\n",
    "storage block imports",
)
storage = replace_once(
    storage,
    "public final class SettlementStorageService {\n    private SettlementStorageService() {}\n",
    '''public final class SettlementStorageService {
    private static final int PUBLIC_STOCKPILE_TARGET_BARRELS = 4;
    private static final int[][] PUBLIC_STOCKPILE_OFFSETS = {
            {0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}
    };

    private SettlementStorageService() {}
''',
    "storage constants",
)
old_storage_positions = '''    public static List<BlockPos> storagePositions(SettlementData data) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.addAll(constructionOfficeSupplyPositions(data));
        positions.addAll(ordinaryStoragePositions(data));
        return new ArrayList<>(positions);
    }

    public static List<BlockPos> ordinaryStoragePositions(SettlementData data) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.add(data.stockpilePos());
        for (BuildingRecord building : data.buildings()) {
            if (building.buildingType() == BuildingType.WAREHOUSE) {
                positions.addAll(WarehouseLayout.storagePositions(building));
            } else if (building.buildingType() == BuildingType.CART_STATION) {
                positions.addAll(CartStationLayout.freightPositions(building));
            }
        }
        return new ArrayList<>(positions);
    }
'''
new_storage_positions = '''    public static List<BlockPos> storagePositions(SettlementData data) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.addAll(constructionOfficeSupplyPositions(data));
        positions.addAll(ordinaryStoragePositions(data));
        return new ArrayList<>(positions);
    }

    /**
     * Public storage remains ordinary vanilla barrels. The first barrel is the persisted founding
     * stockpile; up to three safe neighboring cells are maintained as physical capacity annexes.
     * One vanilla barrel still has 27 slots, so the cluster provides up to 108 slots without a
     * custom block entity/menu/network protocol or save migration.
     */
    public static List<BlockPos> publicStockpilePositions(SettlementData data) {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos origin = data.stockpilePos();
        for (int[] offset : PUBLIC_STOCKPILE_OFFSETS) {
            positions.add(origin.offset(offset[0], 0, offset[1]));
        }
        return positions;
    }

    public static BlockPos worksiteStoragePosition(BuildingRecord building) {
        BuildingType type = building.buildingType();
        if (type == BuildingType.LUMBER_CAMP) return building.localToWorld(5, 1, 6);
        if (type == BuildingType.FARM) return building.localToWorld(6, 1, 2);
        if (type == BuildingType.QUARRY || type == BuildingType.MINE) return building.localToWorld(5, 1, 2);
        return null;
    }

    public static List<BlockPos> worksiteStoragePositions(SettlementData data) {
        List<BlockPos> positions = new ArrayList<>();
        for (BuildingRecord building : data.buildings()) {
            BlockPos pos = worksiteStoragePosition(building);
            if (pos != null) positions.add(pos);
        }
        return positions;
    }

    public static boolean isManagedStoragePosition(SettlementData data, BlockPos pos) {
        for (BlockPos candidate : publicStockpilePositions(data)) {
            if (candidate.equals(pos)) return true;
        }
        for (BlockPos candidate : worksiteStoragePositions(data)) {
            if (candidate.equals(pos)) return true;
        }
        return false;
    }

    /**
     * Save-compatible backfill. Missing Frontier-owned barrels are created only in loaded, dry,
     * replaceable cells with solid dry support. Existing block entities and solid player blocks are
     * never overwritten. A blocked annex is simply skipped and retried on a later maintenance tick.
     */
    public static void ensureManagedStorage(ServerLevel level, SettlementData data) {
        int existingPublic = 0;
        for (BlockPos pos : publicStockpilePositions(data)) {
            if (level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.BARREL)
                    && level.getBlockEntity(pos) instanceof Container) existingPublic++;
        }
        if (existingPublic < PUBLIC_STOCKPILE_TARGET_BARRELS) {
            for (BlockPos pos : publicStockpilePositions(data)) {
                if (existingPublic >= PUBLIC_STOCKPILE_TARGET_BARRELS) break;
                if (!canSafelyCreateManagedBarrel(level, pos)) continue;
                if (level.setBlock(pos, Blocks.BARREL.defaultBlockState(), 3)
                        && level.getBlockEntity(pos) instanceof Container) existingPublic++;
            }
        }
        for (BlockPos pos : worksiteStoragePositions(data)) {
            if (!canSafelyCreateManagedBarrel(level, pos)) continue;
            level.setBlock(pos, Blocks.BARREL.defaultBlockState(), 3);
        }
    }

    private static boolean canSafelyCreateManagedBarrel(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.below())) return false;
        BlockState current = level.getBlockState(pos);
        BlockState below = level.getBlockState(pos.below());
        if (current.is(Blocks.BARREL) && level.getBlockEntity(pos) instanceof Container) return false;
        if (level.getBlockEntity(pos) != null) return false;
        if (!current.getFluidState().isEmpty() || !below.getFluidState().isEmpty()) return false;
        if (!current.isAir() && !current.canBeReplaced()) return false;
        return !below.isAir() && !below.canBeReplaced();
    }

    private static List<BlockPos> generalStoragePositions(SettlementData data) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.addAll(publicStockpilePositions(data));
        for (BuildingRecord building : data.buildings()) {
            if (building.buildingType() == BuildingType.WAREHOUSE) {
                positions.addAll(WarehouseLayout.storagePositions(building));
            } else if (building.buildingType() == BuildingType.CART_STATION) {
                positions.addAll(CartStationLayout.freightPositions(building));
            }
        }
        return new ArrayList<>(positions);
    }

    public static List<BlockPos> ordinaryStoragePositions(SettlementData data) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.addAll(generalStoragePositions(data));
        positions.addAll(worksiteStoragePositions(data));
        return new ArrayList<>(positions);
    }
'''
storage = replace_once(storage, old_storage_positions, new_storage_positions, "storage topology")
old_deposit = '''    private static List<BlockPos> depositPositions(SettlementData data, ItemStack stack) {
        // Wood/stone production can naturally feed the construction office. Food, metal and random
        // loot stay out of its dedicated material bays unless a player deliberately puts them there.
        if (SettlementInventory.isWood(stack) || SettlementInventory.isStone(stack)) return storagePositions(data);
        return ordinaryStoragePositions(data);
    }
'''
new_deposit = '''    private static List<BlockPos> depositPositions(SettlementData data, ItemStack stack) {
        // Generic delivery never steals another profession's local barrel. Local production workers
        // explicitly target their own worksite barrel first; shared overflow uses only public/warehouse/
        // cart storage (plus construction-office material bays for wood/stone).
        Set<BlockPos> positions = new LinkedHashSet<>();
        if (SettlementInventory.isWood(stack) || SettlementInventory.isStone(stack)) {
            positions.addAll(constructionOfficeSupplyPositions(data));
        }
        positions.addAll(generalStoragePositions(data));
        return new ArrayList<>(positions);
    }
'''
storage = replace_once(storage, old_deposit, new_deposit, "storage deposit topology")
write(storage_path, storage)

# ---------------------------------------------------------------------------
# Tick/founding integration and protection of every Frontier managed barrel.
# ---------------------------------------------------------------------------
service_path = JAVA / "settlement/SettlementService.java"
service = read(service_path)
service = replace_once(
    service,
    "        SettlementCoreService.tick(server, data);\n        SettlementConstructionOfficeService.tick(server, data);",
    "        SettlementCoreService.tick(server, data);\n        if (tick % 40 == 0) SettlementStorageService.ensureManagedStorage(server.overworld(), data);\n        SettlementConstructionOfficeService.tick(server, data);",
    "managed storage tick",
)
service = replace_once(
    service,
    "        data.found(center, stockpile);\n        SettlementConstructionService.ensureBuilder(level, data);",
    "        data.found(center, stockpile);\n        SettlementStorageService.ensureManagedStorage(level, data);\n        SettlementConstructionService.ensureBuilder(level, data);",
    "managed storage founding",
)
write(service_path, service)

core_path = JAVA / "settlement/SettlementCoreService.java"
core = read(core_path)
core = replace_once(
    core,
    '''        if (pos.equals(data.stockpilePos()) && current.is(Blocks.BARREL)) {
            event.setCanceled(true);
            event.setNotifyClient(true);
            return;
        }
''',
    '''        if (SettlementStorageService.isManagedStoragePosition(data, pos) && current.is(Blocks.BARREL)) {
            event.setCanceled(true);
            event.setNotifyClient(true);
            return;
        }
''',
    "managed storage break protection",
)
write(core_path, core)

# ---------------------------------------------------------------------------
# Production worker search/path/storage hardening.
# ---------------------------------------------------------------------------
worker_path = JAVA / "settlement/SettlementWorkerService.java"
worker = read(worker_path)
worker = replace_once(
    worker,
    "import net.minecraft.world.phys.AABB;\n",
    "import net.minecraft.world.phys.AABB;\nimport net.minecraft.world.level.pathfinder.Path;\n",
    "worker Path import",
)
worker = replace_once(
    worker,
    '''    private static final int LOCAL_RESOURCE_ROUTE_MARGIN = 56;
    private static final int TREE_SEARCH_RADIUS = 48;
    private static final int QUARRY_SEARCH_RADIUS = 40;
    private static final int MINE_HORIZONTAL_SEARCH_RADIUS = 24;
    private static final int MINE_SEARCH_DEPTH = 48;
''',
    '''    private static final int LOCAL_RESOURCE_ROUTE_MARGIN = 56;
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
''',
    "worker search constants",
)
worker = replace_once(
    worker,
    '''    public record NormalizeResult(int removedProductionWorkers, int loadedProductionWorkers) {}
    private record TreeCandidate(BlockPos base, Item item, double distance, int availableLogs) {}
''',
    '''    public record NormalizeResult(int removedProductionWorkers, int loadedProductionWorkers) {}
    private record TreeCandidate(BlockPos base, Item item, double distance, int availableLogs) {}
    private record CachedTarget(BlockPos pos, long expiresAt) {}
    private record MovementWatch(BlockPos target, double x, double y, double z, long lastProgressTick) {}

    private static final Map<java.util.UUID, CachedTarget> RESOURCE_TARGETS = new HashMap<>();
    private static final Map<java.util.UUID, Long> RESOURCE_SEARCH_RETRY_AFTER = new HashMap<>();
    private static final Map<java.util.UUID, Map<BlockPos, Long>> BLOCKED_TARGETS = new HashMap<>();
    private static final Map<java.util.UUID, MovementWatch> MOVEMENT_WATCHES = new HashMap<>();
''',
    "worker runtime cache records",
)
# Maintenance should also clear transient navigation memory.
worker = replace_once(
    worker,
    '''            worker.getNavigation().stop();
            count++;
''',
    '''            worker.getNavigation().stop();
            clearTransientWorkerState(worker);
            count++;
''',
    "normalize transient reset",
)
# Dynamic evidence/search envelope so far workers are never declared absent just because search widened.
worker = replace_once(
    worker,
    '''            if (!workerRouteEvidenceLoaded(level, data, building.workCenter(), LOCAL_RESOURCE_ROUTE_MARGIN)) return false;
''',
    '''            if (!workerRouteEvidenceLoaded(level, data, building.workCenter(), resourceRouteMargin(type))) return false;
''',
    "dynamic route evidence margin",
)
worker = replace_once(
    worker,
    '''            AABB search = workerRouteBounds(data, building.workCenter(), LOCAL_RESOURCE_ROUTE_MARGIN);
''',
    '''            AABB search = workerRouteBounds(data, building.workCenter(), resourceRouteMargin(type));
''',
    "dynamic worker lookup margin",
)
# Local work methods: jobsite barrels and rotation-safe farms.
worker = worker.replace("deliverToTownStorage(level, data, worker, carried);", "deliverToWorksiteStorage(level, data, worker, camp, carried);", 4)
# The blanket replacement above only touches the first four lumber occurrences in source order. Replace exact
# farm/quarry/mine method bodies deliberately to avoid accidental changes elsewhere.
start = worker.index("    private static void workFarm(")
end = worker.index("    private static void workQuarry(", start)
farm = worker[start:end]
farm = farm.replace("deliverToTownStorage(level, data, worker, carried);", "deliverToWorksiteStorage(level, data, worker, farm, carried);")
farm = farm.replace("deliverToTownStorage(level, data, worker, worker.getMainHandItem());", "deliverToWorksiteStorage(level, data, worker, farm, worker.getMainHandItem());")
farm = farm.replace("farm.origin().offset(x, 1, z)", "farm.localToWorld(x, 1, z)")
worker = worker[:start] + farm + worker[end:]
start = worker.index("    private static void workQuarry(")
end = worker.index("    private static void workMine(", start)
quarry = worker[start:end]
quarry = quarry.replace("deliverToTownStorage(level, data, worker, carried);", "deliverToWorksiteStorage(level, data, worker, quarry, carried);")
quarry = quarry.replace("findExposedStone(level, data, quarry.workCenter(), QUARRY_SEARCH_RADIUS, expected)", "findQuarryTargetForWorker(level, data, worker, quarry.workCenter(), expected)")
worker = worker[:start] + quarry + worker[end:]
start = worker.index("    private static void workMine(")
end = worker.index("    private static int cargoLimit(", start)
mine = worker[start:end]
mine = mine.replace("deliverToTownStorage(level, data, worker, carried);", "deliverToWorksiteStorage(level, data, worker, mine, carried);")
worker = worker[:start] + mine + worker[end:]
# Lumber now uses cached worker-aware tree target.
worker = worker.replace("findTree(level, data, camp.workCenter(), expected)", "findTreeForWorker(level, data, worker, camp.workCenter(), expected)", 1)

old_delivery = '''    private static void deliverToTownStorage(ServerLevel level, SettlementData data,
                                             FrontierWorkerEntity worker, ItemStack carried) {
        BlockPos target = SettlementStorageService.findDepositTarget(level, data, carried);
        if (!level.hasChunkAt(target)) {
            worker.getNavigation().stop();
            return;
        }
        if (worker.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D) > 9.0D) {
            moveNear(level, worker, target, 0.85D);
            return;
        }
        ItemStack remaining = SettlementStorageService.insertAt(level, target, carried);
        worker.setItemSlot(EquipmentSlot.MAINHAND, remaining);
    }
'''
new_delivery = '''    private static void deliverToWorksiteStorage(ServerLevel level, SettlementData data,
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
        BlockPos target = SettlementStorageService.findDepositTarget(level, data, carried);
        if (!level.hasChunkAt(target) || !SettlementStorageService.hasRoomAt(level, target, carried)) {
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
'''
worker = replace_once(worker, old_delivery, new_delivery, "worksite delivery")

old_move = '''    private static boolean moveNear(ServerLevel level, FrontierWorkerEntity worker, BlockPos target, double speed) {
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
        for (BlockPos approach : approaches) {
            if (worker.distanceToSqr(approach.getX() + 0.5D, approach.getY(), approach.getZ() + 0.5D) <= 1.0D) {
                worker.getNavigation().stop();
                return true;
            }
            if (worker.getNavigation().moveTo(
                    approach.getX() + 0.5D, approach.getY(), approach.getZ() + 0.5D, speed)) {
                return true;
            }
        }
        worker.getNavigation().stop();
        return false;
    }
'''
new_move = '''    private static boolean moveNear(ServerLevel level, FrontierWorkerEntity worker, BlockPos target, double speed) {
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
'''
worker = replace_once(worker, old_move, new_move, "complete path movement")

# Replace old tree search with bounded ring search and per-worker cache.
start = worker.index("    private static BlockPos findTree(")
end = worker.index("    private static BlockPos descendToTrunkBase(", start)
new_tree = '''    private static BlockPos findTreeForWorker(ServerLevel level, SettlementData data,
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

'''
worker = worker[:start] + new_tree + worker[end:]

# Quarry cached wrapper + wider vertical envelope + blocked target rejection.
quarry_marker = "    private static BlockPos findExposedStone(ServerLevel level, SettlementData data, BlockPos center,\n"
idx = worker.index(quarry_marker)
quarry_wrapper = '''    private static BlockPos findQuarryTargetForWorker(ServerLevel level, SettlementData data,
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

'''
worker = worker[:idx] + quarry_wrapper + worker[idx:]
worker = worker.replace(
    "private static BlockPos findExposedStone(ServerLevel level, SettlementData data, BlockPos center,\n                                             int radiusLimit, Item expected)",
    "private static BlockPos findExposedStone(ServerLevel level, SettlementData data, FrontierWorkerEntity worker,\n                                             BlockPos center, int radiusLimit, Item expected)",
    1,
)
worker = worker.replace(
    "for (int y = center.getY() - 8; y <= center.getY() + 6; y++)",
    "for (int y = center.getY() - QUARRY_SEARCH_DOWN; y <= center.getY() + QUARRY_SEARCH_UP; y++)",
    1,
)
worker = worker.replace(
    "|| isProtected(data, pos) || !level.getBlockState(pos.above()).isAir()\n                                || !hasWalkableApproach(level, pos)) continue;",
    "|| isProtected(data, pos) || isTargetBlocked(level, worker, pos)\n                                || !level.getBlockState(pos.above()).isAir() || !hasWalkableApproach(level, pos)) continue;",
    1,
)
# Resource margin helper before worker lookup.
helper = '''    private static int resourceRouteMargin(BuildingType type) {
        if (type == BuildingType.LUMBER_CAMP) return TREE_SEARCH_RADIUS + 16;
        if (type == BuildingType.QUARRY) return QUARRY_SEARCH_RADIUS + 16;
        return LOCAL_RESOURCE_ROUTE_MARGIN;
    }

'''
insert_at = worker.index("    private static List<FrontierWorkerEntity> workersByName(")
worker = worker[:insert_at] + helper + worker[insert_at:]
write(worker_path, worker)

# ---------------------------------------------------------------------------
# Alpha.91 source acceptance checks.
# ---------------------------------------------------------------------------
test = r'''from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement"

def text(path): return path.read_text(encoding="utf-8")
def must(src, tokens, label):
    for token in tokens:
        if token not in src: raise SystemExit(f"{label} missing: {token}")
def forbid(src, tokens, label):
    for token in tokens:
        if token in src: raise SystemExit(f"{label} forbidden: {token}")

props = text(ROOT / "gradle.properties")
lock = json.loads(text(ROOT / "COMPANION_LOCK.json"))
worker = text(JAVA / "settlement/SettlementWorkerService.java")
storage = text(JAVA / "settlement/SettlementStorageService.java")
entity = text(JAVA / "content/FrontierWorkerEntity.java")
blueprints = text(JAVA / "settlement/BuildingBlueprints.java")
service = text(JAVA / "settlement/SettlementService.java")
core = text(JAVA / "settlement/SettlementCoreService.java")

must(props, ("mod_version=0.1.0-alpha.91", "Alpha.91 navigation/storage hardening"), "alpha91 props")
if lock["target"]["frontier_settlement"] != "0.1.0-alpha.91": raise SystemExit("alpha91 lock target")
if not any(str(x).startswith("Alpha.91 ") for x in lock.get("notes", [])): raise SystemExit("alpha91 lock note")
must(worker, (
    "TREE_SEARCH_RADIUS = 128", "QUARRY_SEARCH_RADIUS = 96", "MINE_HORIZONTAL_SEARCH_RADIUS = 48",
    "MINE_SEARCH_DEPTH = 80", "RESOURCE_TARGET_CACHE_TICKS", "RESOURCE_SEARCH_RETRY_TICKS",
    "STUCK_PROGRESS_TIMEOUT_TICKS", "path.canReach()", "createPath(approach, 0)",
    "deliverToWorksiteStorage", "worksiteStoragePosition", "farm.localToWorld(x, 1, z)",
    "resourceRouteMargin(type)", "findTreeForWorker", "findQuarryTargetForWorker"
), "worker hardening")
must(storage, (
    "PUBLIC_STOCKPILE_TARGET_BARRELS = 4", "publicStockpilePositions", "worksiteStoragePosition",
    "ensureManagedStorage", "generalStoragePositions", "positions.addAll(worksiteStoragePositions(data))"
), "storage hardening")
must(entity, ("PathType.WATER", "setPathfindingMalus(PathType.WATER, -1.0F)"), "water avoidance")
must(blueprints, ("b.put(5, 1, 6, Blocks.BARREL.defaultBlockState(), Phase.FINISH);",), "lumber barrel")
must(service, ("SettlementStorageService.ensureManagedStorage(server.overworld(), data)", "SettlementStorageService.ensureManagedStorage(level, data)"), "storage maintenance")
must(core, ("SettlementStorageService.isManagedStoragePosition(data, pos)",), "managed storage protection")
forbid(worker, ("farm.origin().offset(x, 1, z)",), "rotated farm bug")
forbid(worker, ("getNavigation().moveTo(\n                    approach.getX() + 0.5D",), "coordinate-only partial path")
print("Alpha.91 source audit: PASS")
'''
write(ROOT / "tools/test_alpha91_source.py", test)

doc = '''# Frontier Settlement Alpha.91 — worker navigation / local storage hardening

Version: `0.1.0-alpha.91`

## Larger physical resource search
- Lumber: 48 -> 128 horizontal blocks, vertical evidence from work Y -12 to +28.
- Quarry: 40 -> 96 horizontal blocks, vertical evidence from work Y -16 to +12.
- Mine scan: 24 -> 48 horizontal blocks and 48 -> 80 blocks downward.
- Search is still loaded-only: no chunk force-load or teleport.
- Tree/quarry targets are cached for 30 seconds and misses cool down for 5 seconds so the larger envelope is not rescanned every work tick.
- Worker lifecycle lookup/evidence bounds expand with the real lumber/quarry roaming envelope so far workers are not falsely treated as missing.

## No fence/water stupidity
- Work movement accepts only a vanilla path whose `Path.canReach()` is true.
- Coordinate-only partial-path acceptance is removed from local production movement.
- Frontier workers give water a negative path malus; standing cells and support blocks must also be dry.
- A target with no complete path is temporarily blacklisted.
- Less than 0.25-block progress for about four seconds triggers stuck recovery: navigation stops, the target is rejected for six seconds, and resource targeting is recalculated.

## Per-job physical barrels
- Lumber, farm, quarry and mine output first returns to that completed building's own barrel.
- Farm/quarry/mine use their existing blueprint barrels. Lumber receives a new barrel at local `(5,1,6)`.
- Old completed work buildings receive a missing managed barrel only if that exact loaded cell is dry, replaceable and has solid support; player blocks and block entities are never overwritten.
- Full, blocked, missing or unreachable local barrels fall back to shared physical storage. Cargo is never virtualized.
- Farm crop iteration now uses rotation-aware `BuildingRecord.localToWorld`, fixing rotated farm harvesting/replanting.

## Public storage capacity
A vanilla barrel is fixed at 27 slots. Making one barrel genuinely larger would require a custom block entity, menu/screen, inventory synchronization and save migration.
Alpha.91 instead keeps vanilla containers and manages a protected four-barrel public cluster around the original saved stockpile for up to **108 slots**.
The persisted original barrel remains authoritative; safe neighboring annex cells are backfilled without deleting solid blocks or block entities.

## Authority / safety
- No custom container protocol.
- No virtual resource ledger or item minting.
- No force-load or teleport.
- No second resident/logistics authority.
- Companion binary pins are unchanged.
'''
write(ROOT / "WORKER_NAVIGATION_STORAGE_ALPHA91.md", doc)

print("Alpha.91 patch applied")
