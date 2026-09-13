from pathlib import Path

ROOT = Path('projects/frontier-settlement')
WORKER = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementWorkerService.java'
PROPS = ROOT / 'gradle.properties'
README = ROOT / 'README.md'
TEST = ROOT / 'tools/test_current_source.py'
DOC = ROOT / 'PERFORMANCE_RETURN_ALPHA131.md'


def read(path: Path) -> str:
    return path.read_text(encoding='utf-8')


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding='utf-8')


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f'{label}: expected exactly one match, found {count}')
    return text.replace(old, new, 1)


worker = read(WORKER)
worker = replace_once(
    worker,
    '''    private static final long RESOURCE_TARGET_CACHE_TICKS = 600L;\n    private static final long RESOURCE_SEARCH_RETRY_TICKS = 100L;\n    private static final long BLOCKED_TARGET_RETRY_TICKS = 120L;\n    private static final long STUCK_PROGRESS_TIMEOUT_TICKS = 80L;\n''',
    '''    private static final long RESOURCE_TARGET_CACHE_TICKS = 600L;\n    // A miss is expensive (large bounded world scan). Back off longer and add a deterministic\n    // per-worker offset so lumber/quarry/mine misses cannot all burst on the same server tick.\n    private static final long RESOURCE_SEARCH_RETRY_TICKS = 200L;\n    private static final long RESOURCE_SEARCH_RETRY_JITTER_TICKS = 200L;\n    private static final long BLOCKED_TARGET_RETRY_TICKS = 120L;\n    private static final long STUCK_PROGRESS_TIMEOUT_TICKS = 80L;\n    // Deep quarry/mine routes can become physically valid but impractically long after excavation.\n    // Keep ordinary walking first, then recover only the cargo-return leg after twelve seconds.\n    private static final long DEEP_WORK_RETURN_TELEPORT_TICKS = 240L;\n''',
    'worker constants')
worker = replace_once(
    worker,
    '''    private static final Map<java.util.UUID, CachedTarget> RESOURCE_TARGETS = new HashMap<>();\n    private static final Map<java.util.UUID, Long> RESOURCE_SEARCH_RETRY_AFTER = new HashMap<>();\n    private static final Map<java.util.UUID, Map<BlockPos, Long>> BLOCKED_TARGETS = new HashMap<>();\n    private static final Map<java.util.UUID, MovementWatch> MOVEMENT_WATCHES = new HashMap<>();\n''',
    '''    private static final Map<java.util.UUID, CachedTarget> RESOURCE_TARGETS = new HashMap<>();\n    private static final Map<java.util.UUID, Long> RESOURCE_SEARCH_RETRY_AFTER = new HashMap<>();\n    private static final Map<java.util.UUID, Map<BlockPos, Long>> BLOCKED_TARGETS = new HashMap<>();\n    private static final Map<java.util.UUID, MovementWatch> MOVEMENT_WATCHES = new HashMap<>();\n    private static final Map<java.util.UUID, Long> CARGO_RETURN_STARTED_AT = new HashMap<>();\n''',
    'return timer map')
worker = replace_once(
    worker,
    '''        RESOURCE_SEARCH_RETRY_AFTER.clear();\n        BLOCKED_TARGETS.clear();\n        MOVEMENT_WATCHES.clear();\n''',
    '''        RESOURCE_SEARCH_RETRY_AFTER.clear();\n        BLOCKED_TARGETS.clear();\n        MOVEMENT_WATCHES.clear();\n        CARGO_RETURN_STARTED_AT.clear();\n''',
    'server-stop clear')

old_miss = 'RESOURCE_SEARCH_RETRY_AFTER.put(id, now + RESOURCE_SEARCH_RETRY_TICKS);'
if worker.count(old_miss) != 3:
    raise RuntimeError(f'resource miss retry: expected 3 matches, found {worker.count(old_miss)}')
worker = worker.replace(old_miss, 'RESOURCE_SEARCH_RETRY_AFTER.put(id, now + resourceSearchRetryTicks(worker));')

worker = replace_once(
    worker,
    '''    private static void deliverToWorksiteStorage(ServerLevel level, SettlementData data,\n                                                 FrontierWorkerEntity worker, BuildingRecord building,\n                                                 ItemStack carried) {\n        if (carried.isEmpty()) return;\n        for (BlockPos local : SettlementStorageService.desiredWorksiteStoragePositions(building, data)) {\n            if (!level.hasChunkAt(local) || !level.getBlockState(local).is(Blocks.BARREL)\n                    || !SettlementStorageService.hasRoomAt(level, local, carried)) continue;\n            double distance = worker.distanceToSqr(\n                    local.getX() + 0.5D, local.getY() + 0.5D, local.getZ() + 0.5D);\n            if (distance <= WORKSITE_STORAGE_INTERACTION_REACH_SQR) {\n                worker.getNavigation().stop();\n                SettlementProductionStatusService.mark(level, building, "현장 저장고 적재 중");\n                ItemStack remaining = SettlementStorageService.insertAt(level, local, carried);\n                worker.setItemSlot(EquipmentSlot.MAINHAND, remaining);\n                clearTargetIfEmpty(worker);\n                return;\n            }\n            if (!isTargetBlocked(level, worker, local) && moveNear(level, worker, local, 0.86D)) { SettlementProductionStatusService.mark(level, building, "현장 저장고 운반 중"); return; }\n        }\n        deliverToTownStorage(level, data, worker, building, carried);\n    }\n''',
    '''    private static void deliverToWorksiteStorage(ServerLevel level, SettlementData data,\n                                                 FrontierWorkerEntity worker, BuildingRecord building,\n                                                 ItemStack carried) {\n        java.util.UUID workerId = worker.getUUID();\n        if (carried.isEmpty()) {\n            CARGO_RETURN_STARTED_AT.remove(workerId);\n            return;\n        }\n        boolean localDestination = false;\n        for (BlockPos local : SettlementStorageService.desiredWorksiteStoragePositions(building, data)) {\n            if (!level.hasChunkAt(local) || !level.getBlockState(local).is(Blocks.BARREL)\n                    || !SettlementStorageService.hasRoomAt(level, local, carried)) continue;\n            localDestination = true;\n            double distance = worker.distanceToSqr(\n                    local.getX() + 0.5D, local.getY() + 0.5D, local.getZ() + 0.5D);\n            if (distance <= WORKSITE_STORAGE_INTERACTION_REACH_SQR) {\n                worker.getNavigation().stop();\n                CARGO_RETURN_STARTED_AT.remove(workerId);\n                SettlementProductionStatusService.mark(level, building, "현장 저장고 적재 중");\n                ItemStack remaining = SettlementStorageService.insertAt(level, local, carried);\n                worker.setItemSlot(EquipmentSlot.MAINHAND, remaining);\n                clearTargetIfEmpty(worker);\n                return;\n            }\n            if (rescueLongDeepWorkReturn(level, worker, building)) {\n                SettlementProductionStatusService.mark(level, building, "귀환 경로 재정비");\n                return;\n            }\n            if (!isTargetBlocked(level, worker, local) && moveNear(level, worker, local, 0.86D)) {\n                SettlementProductionStatusService.mark(level, building, "현장 저장고 운반 중");\n                return;\n            }\n        }\n        if (!localDestination) CARGO_RETURN_STARTED_AT.remove(workerId);\n        deliverToTownStorage(level, data, worker, building, carried);\n    }\n\n    private static boolean rescueLongDeepWorkReturn(ServerLevel level, FrontierWorkerEntity worker,\n                                                    BuildingRecord building) {\n        BuildingType type = building.buildingType();\n        java.util.UUID workerId = worker.getUUID();\n        if (type != BuildingType.QUARRY && type != BuildingType.MINE) {\n            CARGO_RETURN_STARTED_AT.remove(workerId);\n            return false;\n        }\n        long now = level.getGameTime();\n        long started = CARGO_RETURN_STARTED_AT.computeIfAbsent(workerId, ignored -> now);\n        if (now - started < DEEP_WORK_RETURN_TELEPORT_TICKS) return false;\n        BlockPos safe = safeWorkerSpawn(level, building.workCenter());\n        if (safe == null) return false;\n        worker.getNavigation().stop();\n        worker.setPos(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);\n        clearResourceTarget(worker);\n        MOVEMENT_WATCHES.remove(workerId);\n        BLOCKED_TARGETS.remove(workerId);\n        CARGO_RETURN_STARTED_AT.remove(workerId);\n        return true;\n    }\n''',
    'worksite delivery return rescue')

worker = replace_once(
    worker,
    '''    private static void clearResourceTarget(FrontierWorkerEntity worker) {\n        RESOURCE_TARGETS.remove(worker.getUUID());\n        RESOURCE_SEARCH_RETRY_AFTER.remove(worker.getUUID());\n    }\n''',
    '''    private static void clearResourceTarget(FrontierWorkerEntity worker) {\n        RESOURCE_TARGETS.remove(worker.getUUID());\n        RESOURCE_SEARCH_RETRY_AFTER.remove(worker.getUUID());\n    }\n\n    private static long resourceSearchRetryTicks(FrontierWorkerEntity worker) {\n        long jitterSlots = Math.max(1L, RESOURCE_SEARCH_RETRY_JITTER_TICKS / 10L);\n        long jitter = Math.floorMod(worker.getUUID().getLeastSignificantBits(), jitterSlots) * 10L;\n        return RESOURCE_SEARCH_RETRY_TICKS + jitter;\n    }\n''',
    'resource retry jitter helper')
worker = replace_once(
    worker,
    '''        clearResourceTarget(worker);\n        MOVEMENT_WATCHES.remove(worker.getUUID());\n        BLOCKED_TARGETS.remove(worker.getUUID());\n        worker.removeTag(LEGACY_WORKSITE_EXPORT_TAG);\n''',
    '''        clearResourceTarget(worker);\n        MOVEMENT_WATCHES.remove(worker.getUUID());\n        BLOCKED_TARGETS.remove(worker.getUUID());\n        CARGO_RETURN_STARTED_AT.remove(worker.getUUID());\n        worker.removeTag(LEGACY_WORKSITE_EXPORT_TAG);\n''',
    'transient-state clear')
write(WORKER, worker)

props = read(PROPS)
props = replace_once(props, 'mod_version=0.1.0-alpha.130', 'mod_version=0.1.0-alpha.131', 'mod version')
props = replace_once(
    props,
    '# Alpha.130 construction worker routing: distinct idle/recovery slots and ground-only local work approaches.\n',
    '# Alpha.130 construction worker routing: distinct idle/recovery slots and ground-only local work approaches.\n# Alpha.131 deep-work return recovery and staggered expensive-search backoff.\n',
    'version history')
write(PROPS, props)

readme = read(README)
readme = replace_once(
    readme,
    '## Current version: 0.1.0-alpha.130\n\n## Alpha.130 construction-worker routing recovery\n',
    '''## Current version: 0.1.0-alpha.131\n\n## Alpha.131 deep-work return recovery and performance smoothing\n\nQuarry and mine residents still walk normally, but a resident carrying real production cargo no longer spends an unlimited amount of time trying to climb out of an excavated work face. If the return to an available local profession barrel lasts twelve seconds, the server moves that same resident and the same carried ItemStack to a safe walkable cell at its own workplace; ordinary outbound work, short returns and central-storage overflow still use physical navigation. Expensive empty lumber/quarry/mine world searches now back off for 10-20 seconds with a deterministic per-worker offset instead of every worker repeating a full miss scan together every five seconds. This smooths known burst work without changing production radius, resource authority or chunk-loading rules.\n\nStatic review still does not prove which subsystem owns the remaining real-play hitch. A JFR/spark capture from the affected world remains the acceptance path for deeper performance work.\n\n## Alpha.130 construction-worker routing recovery\n''',
    'README Alpha.131 header')
write(README, readme)

test = read(TEST)
test = replace_once(test, 'require("mod_version=0.1.0-alpha.130" in gradle, "current verifier/version drift")',
                    'require("mod_version=0.1.0-alpha.131" in gradle, "current verifier/version drift")',
                    'source verifier version')
needle = '''require("DUPLICATE_MAINTENANCE_INTERVAL_TICKS = 200" in worker, "maintenance duplicate scans regressed to hot-path cadence")\n'''
insert = needle + '''require("DEEP_WORK_RETURN_TELEPORT_TICKS = 240L" in worker\n        and "CARGO_RETURN_STARTED_AT" in worker and "rescueLongDeepWorkReturn" in worker,\n        "deep quarry/mine cargo-return recovery missing")\nrequire("RESOURCE_SEARCH_RETRY_TICKS = 200L" in worker\n        and "RESOURCE_SEARCH_RETRY_JITTER_TICKS = 200L" in worker\n        and worker.count("resourceSearchRetryTicks(worker)") >= 3,\n        "expensive empty resource searches can again synchronize every five seconds")\n'''
test = replace_once(test, needle, insert, 'Alpha.131 source assertions')
write(TEST, test)

DOC.write_text('''# Alpha.131 — Deep-work return recovery and performance smoothing\n\n## Real-play symptom\n\nQuarry residents can progressively work below the original terrain surface, then spend too long or fail to return to their local profession barrel. The same play session also reports recurring integrated-server hitches despite the settlement game being smaller in feature count than many commercial games/modpacks.\n\n## Bounded gameplay fix\n\n- Quarry and mine residents always attempt ordinary physical return navigation first.\n- Only while carrying real cargo toward an available local profession barrel, a 240-tick (12-second) return timer runs.\n- When that timer expires, the same physical resident and unchanged MAINHAND ItemStack move to a safe walkable cell at that resident's own workplace.\n- The timer does not apply to outbound gathering, farms/lumber, or central-storage overflow.\n- No resource is duplicated, deleted, virtualized or force-loaded.\n\n## Static performance findings\n\nThis patch intentionally separates code evidence from profiling proof. Current worker code has several burst-shaped costs:\n\n- a lumber miss can inspect up to 129 x 129 surface columns and several vertical candidates;\n- a quarry miss searches the bounded radius-40 surface/overburden envelope;\n- a mine miss can inspect a radius-32 horizontal envelope through up to 80 blocks of depth;\n- candidate validation repeatedly checks settlement protection state;\n- route acquisition can test multiple vanilla paths around one target.\n\nAlpha.129 already bounded/cached these searches, but a no-target result retried after exactly 100 ticks for every worker. Multiple residents could therefore align their large miss scans on the same integrated-server tick. Alpha.131 changes only the miss cadence: 200 ticks plus a deterministic 0-190 tick per-worker offset. Average no-target rescanning drops and the remaining work is spread across time.\n\n## What this does not claim\n\nThis is not a profiler result. It removes one confirmed burst pattern and one observed return-path failure mode, but the dominant remaining hitch in the user's real world must be measured. If real play still hitches, capture Java Flight Recorder or spark data during the symptom and compare `SettlementWorkerService`, vanilla pathfinding/navigation, entity queries, storage scans and world/block updates before changing more systems.\n\n## Acceptance\n\n1. Let a quarry resident excavate below its workplace and fill/part-fill cargo.\n2. Confirm a normal short return still walks.\n3. If the resident cannot finish the local return within about 12 seconds, confirm it reappears on a safe workplace cell with the exact same carried stack, then deposits normally.\n4. With an exhausted/no-target production site, observe that large target reacquisition does not recur every five seconds in lockstep across workers.\n5. For deeper performance conclusions, collect a profiler capture from the same world.\n''', encoding='utf-8')

print('Alpha.131 patch prepared')
