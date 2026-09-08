from pathlib import Path
import re

ROOT = Path('projects/frontier-settlement')
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementWorkerService.java'
TEST = ROOT / 'tools/test_current_source.py'


def read(path):
    return path.read_text(encoding='utf-8')


def write(path, text):
    path.write_text(text, encoding='utf-8')


def replace_once(text, old, new, label):
    if text.count(old) != 1:
        raise SystemExit(f'{label}: expected one literal match, got {text.count(old)}')
    return text.replace(old, new, 1)


def sub_once(text, pattern, repl, label):
    out, n = re.subn(pattern, repl, text, count=1, flags=re.S)
    if n != 1:
        raise SystemExit(f'{label}: expected one regex match, got {n}')
    return out


worker = read(JAVA)
worker = replace_once(worker,
    'import net.minecraft.world.level.block.state.properties.BlockStateProperties;\nimport net.minecraft.world.phys.AABB;\n',
    'import net.minecraft.world.level.block.state.properties.BlockStateProperties;\nimport net.minecraft.world.level.levelgen.Heightmap;\nimport net.minecraft.world.phys.AABB;\n',
    'heightmap import')
worker = replace_once(worker,
    'import java.util.ArrayList;\n',
    'import java.util.ArrayDeque;\nimport java.util.ArrayList;\n',
    'array deque import')
worker = replace_once(worker,
'''    private static final int TREE_SEARCH_RADIUS = 128;
    private static final int TREE_SEARCH_DOWN = 12;
    private static final int TREE_SEARCH_UP = 28;
    private static final int QUARRY_SEARCH_RADIUS = 96;
    private static final int QUARRY_SEARCH_DOWN = 16;
    private static final int QUARRY_SEARCH_UP = 12;
    private static final int MANAGED_QUARRY_FACE_RADIUS = 28;
    private static final int MANAGED_QUARRY_MAX_OVERBURDEN = 4;
    private static final int MINE_HORIZONTAL_SEARCH_RADIUS = 48;
''',
'''    // Alpha.129 keeps production local enough to remain readable and loaded while removing the
    // multi-million-block miss scans that could spike the integrated-server tick and visibly hitch mobs.
    private static final int TREE_SEARCH_RADIUS = 64;
    private static final int TREE_TOP_SCAN_DEPTH = 12;
    private static final int TREE_FELL_HORIZONTAL_RADIUS = 7;
    private static final int TREE_FELL_MAX_HEIGHT = 32;
    private static final int TREE_FELL_MAX_LOGS = 192;
    private static final int QUARRY_SEARCH_RADIUS = 40;
    private static final int MANAGED_QUARRY_MAX_OVERBURDEN = 12;
    private static final int MINE_HORIZONTAL_SEARCH_RADIUS = 32;
''',
    'resource search constants')
worker = replace_once(worker, '    private static final int MAX_APPROACH_PATH_TRIES = 64;\n',
                      '    private static final int MAX_APPROACH_PATH_TRIES = 24;\n', 'path try cap')
worker = worker.replace('    private record TreeCandidate(BlockPos base, Item item, double distance, int availableLogs) {}\n', '')

# Per-profession evidence: one unloaded wide lumber route must not suppress every other vacancy.
worker = sub_once(worker,
    r'    private static void tryAttractWorker\(MinecraftServer server, ServerLevel level, SettlementData data\) \{.*?\n    private static boolean localProductionEvidenceLoaded',
'''    private static void tryAttractWorker(MinecraftServer server, ServerLevel level, SettlementData data) {
        List<FrontierWorkerEntity> lumber = workersByName(level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME);
        List<FrontierWorkerEntity> farm = workersByName(level, data, BuildingType.FARM, FARM_WORKER_NAME);
        List<FrontierWorkerEntity> quarry = workersByName(level, data, BuildingType.QUARRY, QUARRY_WORKER_NAME);
        List<FrontierWorkerEntity> mine = workersByName(level, data, BuildingType.MINE, MINE_WORKER_NAME);
        boolean allLocalEvidenceLoaded = localProductionEvidenceLoaded(level, data);

        // Population repair still needs complete evidence across every civilian lane. Vacancy authority does
        // not: each profession can safely recruit when its own work/storage envelope is complete. This keeps
        // a distant lumber camp from accidentally preventing a loaded farm, quarry or mine from staffing.
        if (allLocalEvidenceLoaded
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

        if (productionEvidenceLoaded(level, data, BuildingType.LUMBER_CAMP)
                && tryFillJob(server, level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME, lumber)) return;
        if (productionEvidenceLoaded(level, data, BuildingType.FARM)
                && tryFillJob(server, level, data, BuildingType.FARM, FARM_WORKER_NAME, farm)) return;
        if (productionEvidenceLoaded(level, data, BuildingType.QUARRY)
                && tryFillJob(server, level, data, BuildingType.QUARRY, QUARRY_WORKER_NAME, quarry)) return;
        if (productionEvidenceLoaded(level, data, BuildingType.MINE)
                && tryFillJob(server, level, data, BuildingType.MINE, MINE_WORKER_NAME, mine)) return;

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

    private static boolean localProductionEvidenceLoaded''',
    'attraction evidence split')
worker = replace_once(worker,
'''    private static boolean localProductionEvidenceLoaded(ServerLevel level, SettlementData data) {
        if (!SettlementStorageService.storageAvailable(level, data)) return false;
        for (BuildingRecord building : data.buildings()) {
            BuildingType type = building.buildingType();
            if (type != BuildingType.LUMBER_CAMP && type != BuildingType.FARM
                    && type != BuildingType.QUARRY && type != BuildingType.MINE) continue;
            if (!workerRouteEvidenceLoaded(level, data, building.workCenter(), resourceRouteMargin(type))) return false;
        }
        return true;
    }
''',
'''    private static boolean localProductionEvidenceLoaded(ServerLevel level, SettlementData data) {
        return productionEvidenceLoaded(level, data, BuildingType.LUMBER_CAMP)
                && productionEvidenceLoaded(level, data, BuildingType.FARM)
                && productionEvidenceLoaded(level, data, BuildingType.QUARRY)
                && productionEvidenceLoaded(level, data, BuildingType.MINE);
    }

    private static boolean productionEvidenceLoaded(ServerLevel level, SettlementData data, BuildingType type) {
        if (!SettlementStorageService.storageAvailable(level, data)) return false;
        for (BuildingRecord building : buildings(data, type)) {
            if (!workerRouteEvidenceLoaded(level, data, building.workCenter(), resourceRouteMargin(type))) return false;
        }
        return true;
    }
''',
    'production evidence helper')

# Repair/relocate workers that were historically spawned inside a solid work-center block.
worker = replace_once(worker,
'''            worker.setNoAi(false);
            worker.setInvulnerable(false);
            SettlementProductionStatusService.mark(level, building, "정상 작업 중");
''',
'''            worker.setNoAi(false);
            worker.setInvulnerable(false);
            recoverBlockedWorker(level, worker, building.workCenter());
            SettlementProductionStatusService.mark(level, building, "정상 작업 중");
''',
    'runtime blocked worker recovery')

worker = sub_once(worker,
    r'    private static FrontierWorkerEntity spawnWorker\(ServerLevel level, BlockPos spawn, String name\) \{.*?\n    \}\n\n    /\*\*\n     \* Frontier-managed local civilians',
'''    private static FrontierWorkerEntity spawnWorker(ServerLevel level, BlockPos spawn, String name) {
        BlockPos safe = safeWorkerSpawn(level, spawn);
        if (safe == null) return null;
        FrontierWorkerEntity worker = new FrontierWorkerEntity(FrontierContent.FRONTIER_WORKER.get(), level);
        worker.setPos(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
        worker.setCustomName(Component.literal(name));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        worker.addTag(RESOURCE_WORKER_TAG);
        if (!level.addFreshEntity(worker)) return null;
        return worker;
    }

    private static BlockPos safeWorkerSpawn(ServerLevel level, BlockPos preferred) {
        if (isWalkableApproach(level, preferred)) return preferred;
        int[] dyOrder = {0, 1, -1, 2, -2, 3, -3};
        for (int radius = 1; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int dy : dyOrder) {
                        BlockPos candidate = preferred.offset(dx, dy, dz);
                        if (isWalkableApproach(level, candidate)) return candidate;
                    }
                }
            }
        }
        return null;
    }

    private static void recoverBlockedWorker(ServerLevel level, FrontierWorkerEntity worker, BlockPos workplace) {
        BlockPos feet = worker.blockPosition();
        if (!level.hasChunkAt(feet) || !level.hasChunkAt(feet.above())) return;
        boolean blocked = !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
        if (!blocked) return;
        BlockPos safe = safeWorkerSpawn(level, workplace);
        if (safe == null) return;
        worker.getNavigation().stop();
        worker.setPos(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
        clearTransientWorkerState(worker);
    }

    /**
     * Frontier-managed local civilians''',
    'safe worker spawn')

# Whole-tree felling: no orphaned top/branch logs after the base is removed.
worker = sub_once(worker,
    r'    private static void workLumber\(ServerLevel level, SettlementData data,\n                                   FrontierWorkerEntity worker, BuildingRecord camp\) \{.*?\n    \}\n\n    private static void workFarm',
'''    private static void workLumber(ServerLevel level, SettlementData data,
                                   FrontierWorkerEntity worker, BuildingRecord camp) {
        ItemStack carried = worker.getMainHandItem();
        Item expected = carried.isEmpty() ? null : carried.getItem();
        if (!carried.isEmpty() && carried.getCount() >= cargoLimit(carried)) {
            deliverToWorksiteStorage(level, data, worker, camp, carried);
            return;
        }
        BlockPos target = findTreeForWorker(level, data, worker, camp.workCenter(), expected);
        if (target == null) {
            if (!carried.isEmpty()) { SettlementProductionStatusService.mark(level, camp, "생산물 보관 중"); deliverToWorksiteStorage(level, data, worker, camp, carried); }
            else { SettlementProductionStatusService.mark(level, camp, "주변 벌목 대상 없음"); moveNear(level, worker, camp.workCenter(), 0.82D); }
            return;
        }
        if (!withinResourceWorkReach(worker, target, LUMBER_REMOTE_WORK_REACH_SQR)) {
            SettlementProductionStatusService.mark(level, camp, "벌목지 이동 중");
            moveNear(level, worker, target, 0.92D);
            return;
        }
        worker.getNavigation().stop();
        MOVEMENT_WATCHES.remove(worker.getUUID());
        int efficiencyGrade = SettlementProductionEfficiencyService.grade(data, camp);
        if (!workDue(level, camp, SettlementProductionEfficiencyService.lumberWorkPeriod(efficiencyGrade))) return;
        Item item = level.getBlockState(target).getBlock().asItem();
        int room = cargoRoom(worker, item);
        if (room <= 0) {
            deliverToWorksiteStorage(level, data, worker, camp, carried);
            return;
        }
        // One production pass fells one complete verified tree. Usable timber recovery stays bounded by
        // the paid production grade/cargo room, but branches and the upper trunk are never orphaned.
        int recoveryCap = Math.min(SettlementProductionEfficiencyService.lumberBatch(efficiencyGrade), room);
        ItemStack harvested = fellWholeTree(level, data, target, item, recoveryCap);
        clearResourceTarget(worker);
        if (!harvested.isEmpty() && appendCargo(worker, harvested)) {
            worker.swing(InteractionHand.MAIN_HAND);
            deliverIfCargoFull(level, data, worker, camp);
        }
    }

    private static void workFarm''',
    'whole tree work loop')

# Minehead workers need a legal nearby standing cell, and ore acquisition must reuse veins/cache.
worker = sub_once(worker,
    r'    private static void workMine\(ServerLevel level, SettlementData data,\n                                 FrontierWorkerEntity worker, BuildingRecord mine\) \{.*?\n    \}\n\n    // Profession barrels',
'''    private static void workMine(ServerLevel level, SettlementData data,
                                 FrontierWorkerEntity worker, BuildingRecord mine) {
        ItemStack carried = worker.getMainHandItem();
        if (!carried.isEmpty() && carried.getCount() >= cargoLimit(carried)) {
            deliverToWorksiteStorage(level, data, worker, mine, carried);
            return;
        }
        BlockPos work = mine.workCenter();
        if (worker.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D) > 64.0D) {
            SettlementProductionStatusService.mark(level, mine, "광산 복귀 중");
            moveNear(level, worker, work, 0.86D);
            return;
        }
        int efficiencyGrade = SettlementProductionEfficiencyService.grade(data, mine);
        if (!workDue(level, mine, SettlementProductionEfficiencyService.mineWorkPeriod(efficiencyGrade))) return;
        Item expected = carried.isEmpty() ? null : carried.getItem();
        BlockPos ore = findOreForWorker(level, data, worker, work, expected);
        if (ore == null) {
            SettlementProductionStatusService.mark(level, mine, "광맥 탐색 중");
            if (!carried.isEmpty()) deliverToWorksiteStorage(level, data, worker, mine, carried);
            return;
        }
        ItemStack preview = previewMineDrop(level.getBlockState(ore));
        if (preview.isEmpty()) {
            clearResourceTarget(worker);
            return;
        }
        int room = cargoRoom(worker, preview.getItem());
        if (room <= 0) {
            deliverToWorksiteStorage(level, data, worker, mine, carried);
            return;
        }
        ItemStack mined = mineOre(level, ore, room);
        if (!mined.isEmpty() && appendCargo(worker, mined)) {
            worker.swing(InteractionHand.MAIN_HAND);
            deliverIfCargoFull(level, data, worker, mine);
        }
    }

    // Profession barrels''',
    'mine worker cache loop')

# Heightmap-first tree search replaces the full 3-D 128-block cube scan.
worker = sub_once(worker,
    r'    private static BlockPos findTree\(ServerLevel level, SettlementData data, FrontierWorkerEntity worker,\n                                     BlockPos center, Item expected\) \{.*?\n    private static BlockPos descendToTrunkBase',
'''    private static BlockPos findTree(ServerLevel level, SettlementData data, FrontierWorkerEntity worker,
                                     BlockPos center, Item expected) {
        Set<BlockPos> seenBases = new HashSet<>();
        for (int radius = 0; radius <= TREE_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    int x = center.getX() + dx;
                    int z = center.getZ() + dz;
                    BlockPos columnProbe = new BlockPos(x, center.getY(), z);
                    if (!level.hasChunkAt(columnProbe)) continue;
                    int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                    for (int drop = 0; drop <= TREE_TOP_SCAN_DEPTH; drop++) {
                        BlockPos probe = new BlockPos(x, topY - drop, z);
                        if (!level.hasChunkAt(probe) || isProtected(data, probe)) continue;
                        BlockState state = level.getBlockState(probe);
                        if (!state.is(BlockTags.LOGS)) continue;
                        Item item = state.getBlock().asItem();
                        if (item == Items.AIR || (expected != null && item != expected)) continue;
                        BlockPos base = descendToTrunkBase(level, data, probe, item);
                        if (!seenBases.add(base) || !isNaturalTreeBase(level, base)
                                || isBlockedOutsideWorkReach(level, worker, base, LUMBER_REMOTE_WORK_REACH_SQR)
                                || !canWorkOrApproach(level, worker, base, LUMBER_REMOTE_WORK_REACH_SQR)) continue;
                        List<BlockPos> logs = connectedTreeLogs(level, data, base, item);
                        if (!logs.isEmpty() && hasLeafEvidenceForTree(level, logs)) return base;
                    }
                }
            }
        }
        return null;
    }

    private static BlockPos descendToTrunkBase''',
    'heightmap tree search')

# Remove old vertical-only count and replace natural-ground authority.
worker = sub_once(worker,
    r'    private static int countVerticalTrunk\(ServerLevel level, SettlementData data, BlockPos base, Item item\) \{.*?\n    \}\n\n',
    '', 'remove vertical trunk counter')
worker = replace_once(worker,
'''    private static boolean isNaturalTreeBase(ServerLevel level, BlockPos base) {
        if (!level.hasChunkAt(base.below())) return false;
        BlockState below = level.getBlockState(base.below());
        return below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.DIRT) || below.is(Blocks.COARSE_DIRT)
                || below.is(Blocks.PODZOL) || below.is(Blocks.ROOTED_DIRT) || below.is(Blocks.MOSS_BLOCK)
                || below.is(Blocks.MYCELIUM) || below.is(Blocks.MUD);
    }
''',
'''    private static boolean isNaturalTreeBase(ServerLevel level, BlockPos base) {
        if (!level.hasChunkAt(base.below())) return false;
        BlockState below = level.getBlockState(base.below());
        return below.is(BlockTags.DIRT) || below.is(Blocks.MOSS_BLOCK) || below.is(Blocks.MUD);
    }
''',
    'tag-aware tree ground')

# Replace vertical partial harvesting plus old leaf scan with bounded connected-tree felling.
worker = sub_once(worker,
    r'    private static boolean hasLeavesAbove\(ServerLevel level, BlockPos trunk\) \{.*?\n    private static void tryReplantHarvestedTree',
'''    private static List<BlockPos> connectedTreeLogs(ServerLevel level, SettlementData data, BlockPos base, Item item) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> logs = new ArrayList<>();
        queue.add(base.immutable());
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos)) continue;
            int dyFromBase = pos.getY() - base.getY();
            if (dyFromBase < 0 || dyFromBase > TREE_FELL_MAX_HEIGHT
                    || Math.abs(pos.getX() - base.getX()) > TREE_FELL_HORIZONTAL_RADIUS
                    || Math.abs(pos.getZ() - base.getZ()) > TREE_FELL_HORIZONTAL_RADIUS
                    || !level.hasChunkAt(pos) || isProtected(data, pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LOGS) || state.getBlock().asItem() != item) continue;
            logs.add(pos.immutable());
            if (logs.size() > TREE_FELL_MAX_LOGS) return List.of();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        queue.add(pos.offset(dx, dy, dz));
                    }
                }
            }
        }
        return List.copyOf(logs);
    }

    private static boolean hasLeafEvidenceForTree(ServerLevel level, List<BlockPos> logs) {
        if (logs.isEmpty()) return false;
        int topY = Integer.MIN_VALUE;
        for (BlockPos log : logs) topY = Math.max(topY, log.getY());
        for (BlockPos log : logs) {
            if (log.getY() < topY - 4) continue;
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -3; dy <= 5; dy++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        BlockPos probe = log.offset(dx, dy, dz);
                        if (level.hasChunkAt(probe) && level.getBlockState(probe).is(BlockTags.LEAVES)) return true;
                    }
                }
            }
        }
        return false;
    }

    private static ItemStack fellWholeTree(ServerLevel level, SettlementData data, BlockPos base,
                                           Item expected, int recoveryCap) {
        if (recoveryCap <= 0 || !level.hasChunkAt(base)) return ItemStack.EMPTY;
        BlockState first = level.getBlockState(base);
        if (!first.is(BlockTags.LOGS)) return ItemStack.EMPTY;
        Item item = first.getBlock().asItem();
        if (item == Items.AIR || (expected != null && item != expected)) return ItemStack.EMPTY;
        List<BlockPos> logs = connectedTreeLogs(level, data, base, item);
        if (logs.isEmpty() || !hasLeafEvidenceForTree(level, logs)) return ItemStack.EMPTY;
        List<BlockPos> roots = new ArrayList<>();
        Map<BlockPos, BlockState> original = new HashMap<>();
        for (BlockPos pos : logs) {
            original.put(pos, level.getBlockState(pos));
            if (isNaturalTreeBase(level, pos)) roots.add(pos);
        }
        List<BlockPos> removed = new ArrayList<>();
        for (BlockPos pos : logs) {
            if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) {
                for (BlockPos rollback : removed) level.setBlock(rollback, original.get(rollback), 3);
                return ItemStack.EMPTY;
            }
            removed.add(pos);
        }
        for (BlockPos root : roots) tryReplantHarvestedTree(level, data, root, original.get(root), item);
        int recovered = Math.min(recoveryCap, removed.size());
        return recovered <= 0 ? ItemStack.EMPTY : new ItemStack(item, recovered);
    }

    private static void tryReplantHarvestedTree''',
    'connected tree felling')

# Managed quarry now starts from nearby real surface columns instead of a huge exposed-stone volume scan.
worker = sub_once(worker,
    r'    private static BlockPos findQuarryTargetForWorker\(ServerLevel level, SettlementData data,\n                                                           FrontierWorkerEntity worker, BlockPos center, Item expected\) \{.*?\n    private static BlockPos findManagedQuarryStone',
'''    private static BlockPos findQuarryTargetForWorker(ServerLevel level, SettlementData data,
                                                           FrontierWorkerEntity worker, BlockPos center, Item expected) {
        java.util.UUID id = worker.getUUID();
        long now = level.getGameTime();
        CachedTarget cached = RESOURCE_TARGETS.get(id);
        if (cached != null && cached.expiresAt() > now && level.hasChunkAt(cached.pos())
                && !isProtected(data, cached.pos())
                && !isBlockedOutsideWorkReach(level, worker, cached.pos(), QUARRY_REMOTE_WORK_REACH_SQR)) {
            BlockState state = level.getBlockState(cached.pos());
            Item item = state.getBlock().asItem();
            BlockPos approach = quarryApproach(level, data, cached.pos());
            if (isQuarryStone(state) && item != Items.AIR && (expected == null || item == expected)
                    && approach != null && canWorkOrApproach(level, worker, approach, QUARRY_REMOTE_WORK_REACH_SQR)) {
                return cached.pos();
            }
        }
        RESOURCE_TARGETS.remove(id);
        if (RESOURCE_SEARCH_RETRY_AFTER.getOrDefault(id, 0L) > now) return null;
        BlockPos target = findManagedQuarryStone(level, data, worker, center, expected);
        if (target == null) {
            RESOURCE_SEARCH_RETRY_AFTER.put(id, now + RESOURCE_SEARCH_RETRY_TICKS);
            return null;
        }
        RESOURCE_SEARCH_RETRY_AFTER.remove(id);
        RESOURCE_TARGETS.put(id, new CachedTarget(target.immutable(), now + RESOURCE_TARGET_CACHE_TICKS));
        return target;
    }

    private static BlockPos findManagedQuarryStone''',
    'quarry target resolver')
# Drop now-unused exhaustive exposed-stone method if it survived before managed method.
worker = sub_once(worker,
    r'    private static BlockPos findExposedStone\(ServerLevel level, SettlementData data, FrontierWorkerEntity worker,\n                                             BlockPos center, int radiusLimit, Item expected\) \{.*?\n    \}\n\n    /\*\*\n     \* A quarry is a physical excavation',
'''    /**
     * A quarry is a physical excavation''',
    'remove exhaustive exposed quarry scan')
worker = sub_once(worker,
    r'    private static BlockPos findManagedQuarryStone\(ServerLevel level, SettlementData data,\n                                                   FrontierWorkerEntity worker, BlockPos center, Item expected\) \{.*?\n    \}\n\n    private static BlockPos quarryApproach',
'''    private static BlockPos findManagedQuarryStone(ServerLevel level, SettlementData data,
                                                   FrontierWorkerEntity worker, BlockPos center, Item expected) {
        for (int radius = 6; radius <= QUARRY_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    int x = center.getX() + dx;
                    int z = center.getZ() + dz;
                    BlockPos columnProbe = new BlockPos(x, center.getY(), z);
                    if (!level.hasChunkAt(columnProbe)) continue;
                    int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                    for (int depth = 0; depth <= MANAGED_QUARRY_MAX_OVERBURDEN; depth++) {
                        BlockPos pos = new BlockPos(x, surfaceY - depth, z);
                        if (!level.hasChunkAt(pos) || isProtected(data, pos)) break;
                        BlockState state = level.getBlockState(pos);
                        Item item = state.getBlock().asItem();
                        if (isQuarryStone(state) && item != Items.AIR && (expected == null || item == expected)) {
                            int cover = quarryOverburdenDepth(level, data, pos);
                            if (cover < 0) break;
                            BlockPos approach = cover == 0 ? pos : pos.above(cover + 1);
                            if (!isBlockedOutsideWorkReach(level, worker, approach, QUARRY_REMOTE_WORK_REACH_SQR)
                                    && canWorkOrApproach(level, worker, approach, QUARRY_REMOTE_WORK_REACH_SQR)) return pos;
                            break;
                        }
                        if (!isSafeQuarryOverburden(state)) break;
                    }
                }
            }
        }
        return null;
    }

    private static BlockPos quarryApproach''',
    'surface quarry search')
worker = replace_once(worker,
'''    private static boolean isSafeQuarryOverburden(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.MUD) || state.is(Blocks.GRAVEL) || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND) || state.is(Blocks.CLAY) || state.is(Blocks.SNOW_BLOCK);
    }
''',
'''    private static boolean isSafeQuarryOverburden(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.MUD)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)
                || state.is(Blocks.CLAY) || state.is(Blocks.SNOW_BLOCK);
    }
''',
    'tag-aware quarry overburden')

# Ore search: cached veins first, then radius-first vertical columns. No full horizontal plane per depth.
worker = sub_once(worker,
    r'    private static BlockPos findOreBelow\(ServerLevel level, SettlementData data, BlockPos center, Item expected\) \{.*?\n    \}\n\n    private static ItemStack previewMineDrop',
'''    private static BlockPos findOreForWorker(ServerLevel level, SettlementData data, FrontierWorkerEntity worker,
                                               BlockPos center, Item expected) {
        java.util.UUID id = worker.getUUID();
        long now = level.getGameTime();
        CachedTarget cached = RESOURCE_TARGETS.get(id);
        if (cached != null) {
            if (cached.expiresAt() > now && validOreTarget(level, data, cached.pos(), expected)) return cached.pos();
            BlockPos nearby = findNearbyOre(level, data, cached.pos(), expected);
            if (nearby != null) {
                RESOURCE_TARGETS.put(id, new CachedTarget(nearby.immutable(), now + RESOURCE_TARGET_CACHE_TICKS));
                RESOURCE_SEARCH_RETRY_AFTER.remove(id);
                return nearby;
            }
        }
        RESOURCE_TARGETS.remove(id);
        if (RESOURCE_SEARCH_RETRY_AFTER.getOrDefault(id, 0L) > now) return null;
        BlockPos target = findOreBelow(level, data, center, expected);
        if (target == null) {
            RESOURCE_SEARCH_RETRY_AFTER.put(id, now + RESOURCE_SEARCH_RETRY_TICKS);
            return null;
        }
        RESOURCE_SEARCH_RETRY_AFTER.remove(id);
        RESOURCE_TARGETS.put(id, new CachedTarget(target.immutable(), now + RESOURCE_TARGET_CACHE_TICKS));
        return target;
    }

    private static boolean validOreTarget(ServerLevel level, SettlementData data, BlockPos pos, Item expected) {
        if (!level.hasChunkAt(pos) || isProtected(data, pos)) return false;
        ItemStack preview = previewMineDrop(level.getBlockState(pos));
        return !preview.isEmpty() && (expected == null || preview.getItem() == expected);
    }

    private static BlockPos findNearbyOre(ServerLevel level, SettlementData data, BlockPos oldTarget, Item expected) {
        for (int radius = 1; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                        BlockPos pos = oldTarget.offset(dx, dy, dz);
                        if (validOreTarget(level, data, pos, expected)) return pos;
                    }
                }
            }
        }
        return null;
    }

    private static BlockPos findOreBelow(ServerLevel level, SettlementData data, BlockPos center, Item expected) {
        // Radius-first ordering walks a handful of vertical columns near the minehead instead of scanning
        // every X/Z position on one Y plane before descending. Common ore is therefore found after a tiny
        // fraction of the old worst-case probes, while the same finite physical search envelope remains.
        for (int radius = 0; radius <= MINE_HORIZONTAL_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int depth = 2; depth <= MINE_SEARCH_DEPTH; depth++) {
                        BlockPos pos = new BlockPos(center.getX() + dx, center.getY() - depth, center.getZ() + dz);
                        if (validOreTarget(level, data, pos, expected)) return pos;
                    }
                }
            }
        }
        return null;
    }

    private static ItemStack previewMineDrop''',
    'ore search rewrite')

# One entity query per profession rather than one huge AABB query per building every ten ticks.
worker = sub_once(worker,
    r'    private static List<FrontierWorkerEntity> workersByName\(ServerLevel level, SettlementData data,\n                                                BuildingType type, String name\) \{.*?\n    \}\n\}',
'''    private static List<FrontierWorkerEntity> workersByName(ServerLevel level, SettlementData data,
                                                BuildingType type, String name) {
        List<BuildingRecord> jobs = buildings(data, type);
        if (jobs.isEmpty()) return List.of();
        AABB search = null;
        for (BuildingRecord building : jobs) {
            AABB bounds = workerRouteBounds(data, building.workCenter(), resourceRouteMargin(type));
            search = search == null ? bounds : new AABB(
                    Math.min(search.minX, bounds.minX), Math.min(search.minY, bounds.minY), Math.min(search.minZ, bounds.minZ),
                    Math.max(search.maxX, bounds.maxX), Math.max(search.maxY, bounds.maxY), Math.max(search.maxZ, bounds.maxZ));
        }
        List<FrontierWorkerEntity> workers = level.getEntitiesOfClass(FrontierWorkerEntity.class, search,
                candidate -> candidate.getCustomName() != null && name.equals(candidate.getCustomName().getString()));
        workers.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        return List.copyOf(workers);
    }
}''',
    'single profession entity query')

write(JAVA, worker)

# Version + docs/manifests.
props = read(ROOT / 'gradle.properties')
props = replace_once(props, 'mod_version=0.1.0-alpha.128', 'mod_version=0.1.0-alpha.129', 'gradle version')
props += '\n# Alpha.129 worker runtime: whole-tree felling, safe worker spawn/recovery, per-profession staffing evidence, and bounded resource-search hot paths.\n'
write(ROOT / 'gradle.properties', props)

readme = read(ROOT / 'README.md')
readme = replace_once(readme, '## Current version: 0.1.0-alpha.128', '## Current version: 0.1.0-alpha.129', 'readme version')
marker = '## Current version: 0.1.0-alpha.129\n'
section = '''\n## Alpha.129 worker runtime recovery\n\nReal-play feedback exposed a shared failure pattern behind partial forestry, idle non-farm workers and integrated-server tick hitches. Lumber now fells a complete verified connected tree instead of orphaning upper/branch logs; production workers spawn/recover on real walkable cells; staffing evidence is isolated per profession; and lumber/quarry/mine target acquisition avoids the previous huge 3-D/plane rescans. Physical resource/storage authority and loaded-only simulation remain unchanged.\n'''
readme = replace_once(readme, marker, marker + section, 'readme alpha129 section')
write(ROOT / 'README.md', readme)

for rel in ('COMPANION_LOCK.json', 'companion-testpack/resolved-lock.client.json', 'companion-testpack/resolved-lock.server.json'):
    path = ROOT / rel
    text = read(path)
    text = text.replace('"frontier_settlement": "0.1.0-alpha.128"', '"frontier_settlement": "0.1.0-alpha.129"')
    if rel == 'COMPANION_LOCK.json':
        text = replace_once(text,
            '    "Frontier Alpha.128 hardens real-play construction recovery: builders no longer choose arbitrary roofs/log pillars as home positions, grading uses bounded ground-aware approach candidates, and the HUD exposes the immediate stall category. Third-party companion pins/hashes remain unchanged."\n',
            '    "Frontier Alpha.128 hardens real-play construction recovery: builders no longer choose arbitrary roofs/log pillars as home positions, grading uses bounded ground-aware approach candidates, and the HUD exposes the immediate stall category. Third-party companion pins/hashes remain unchanged.",\n    "Frontier Alpha.129 hardens real-play worker runtime: whole-tree felling, safe production-worker spawn/recovery, per-profession staffing evidence and bounded lumber/quarry/mine search hot paths reduce server tick spikes. Third-party companion pins/hashes remain unchanged."\n',
            'companion alpha129 note')
    write(path, text)

# Current source verifier is the regression contract for this bug batch.
test = read(TEST)
test = replace_once(test, 'mod_version=0.1.0-alpha.128', 'mod_version=0.1.0-alpha.129', 'test version')
test = replace_once(test,
'''require("findManagedQuarryStone" in worker and "MANAGED_QUARRY_MAX_OVERBURDEN = 4" in worker
        and "clearTopQuarryOverburden" in worker,
        "town quarry still requires player-pre-exposed stone")
''',
'''require("findManagedQuarryStone" in worker and "MANAGED_QUARRY_MAX_OVERBURDEN = 12" in worker
        and "Heightmap.Types.MOTION_BLOCKING_NO_LEAVES" in worker and "clearTopQuarryOverburden" in worker,
        "town quarry search/managed-face recovery regressed")
require("TREE_SEARCH_RADIUS = 64" in worker and "TREE_FELL_MAX_LOGS = 192" in worker
        and "fellWholeTree" in worker and "connectedTreeLogs" in worker
        and "harvestVerticalTrunk" not in worker,
        "whole-tree bounded lumber recovery missing")
require("safeWorkerSpawn" in worker and "recoverBlockedWorker" in worker,
        "non-farm workers can regress to solid work-center spawn cells")
require("productionEvidenceLoaded" in worker
        and "productionEvidenceLoaded(level, data, BuildingType.LUMBER_CAMP)" in worker
        and "productionEvidenceLoaded(level, data, BuildingType.FARM)" in worker,
        "one unloaded production lane can suppress every other profession again")
require("MAX_APPROACH_PATH_TRIES = 24" in worker and "findOreForWorker" in worker
        and "findNearbyOre" in worker and "for (int radius = 0; radius <= MINE_HORIZONTAL_SEARCH_RADIUS; radius++)" in worker,
        "bounded worker path/ore hot-path recovery missing")
require("List<BuildingRecord> jobs = buildings(data, type);" in worker and "AABB search = null;" in worker,
        "production worker lookup returned to one wide entity scan per building")
''',
    'worker regression assertions')
write(TEST, test)

print('Alpha.129 worker runtime patch applied')
