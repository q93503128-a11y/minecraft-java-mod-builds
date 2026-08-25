#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
PROJECT = ROOT / 'projects/frontier-settlement'
JAVA = PROJECT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement'

def read(path):
    return path.read_text(encoding='utf-8')

def write(path, value):
    path.write_text(value, encoding='utf-8')

def replace_once(value, old, new, label):
    count = value.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected one anchor, found {count}')
    return value.replace(old, new, 1)

def insert_before_once(value, anchor, block, marker, label):
    if marker in value:
        return value
    if value.count(anchor) != 1:
        raise SystemExit(f'{label}: expected one insertion anchor, found {value.count(anchor)}')
    return value.replace(anchor, block.rstrip() + '\n\n' + anchor, 1)

# -----------------------------------------------------------------------------
# 1) Local civilian lifecycle: use the same work/storage/housing bounds for
#    loaded evidence and authoritative worker lookup.
# -----------------------------------------------------------------------------
worker_path = JAVA / 'SettlementWorkerService.java'
worker = read(worker_path)
worker = replace_once(worker,
    'import java.util.Comparator;\nimport java.util.List;\n',
    'import java.util.Comparator;\nimport java.util.HashSet;\nimport java.util.List;\nimport java.util.Set;\n',
    'worker imports')
worker = replace_once(worker,
'''        List<Villager> lumber = workersByName(level, data.centerPos(), LUMBER_WORKER_NAME);
        List<Villager> farm = workersByName(level, data.centerPos(), FARM_WORKER_NAME);
        List<Villager> quarry = workersByName(level, data.centerPos(), QUARRY_WORKER_NAME);
        List<Villager> mine = workersByName(level, data.centerPos(), MINE_WORKER_NAME);''',
'''        List<Villager> lumber = workersByName(level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME);
        List<Villager> farm = workersByName(level, data, BuildingType.FARM, FARM_WORKER_NAME);
        List<Villager> quarry = workersByName(level, data, BuildingType.QUARRY, QUARRY_WORKER_NAME);
        List<Villager> mine = workersByName(level, data, BuildingType.MINE, MINE_WORKER_NAME);''',
    'worker lookup calls')
old_evidence = '''    /**
     * Loaded-only visibility proof for a local civilian assignment. It checks every chunk in the
     * bounded rectangle between the work site and every concrete settlement storage endpoint plus a
     * small work/path margin. It only calls hasChunkAt: this is never a force-load mechanism.
     */
    static boolean workerRouteEvidenceLoaded(ServerLevel level, SettlementData data,
                                             BlockPos workCenter, int margin) {
        if (!SettlementStorageService.storageAvailable(level, data)) return false;
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
        int minChunkX = Math.floorDiv(minX, 16);
        int maxChunkX = Math.floorDiv(maxX, 16);
        int minChunkZ = Math.floorDiv(minZ, 16);
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
'''
new_evidence = '''    /**
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
        for (BuildingRecord building : data.buildings()) {
            if (building.buildingType() != BuildingType.HOUSE) continue;
            minX = Math.min(minX, building.originX() - margin);
            maxX = Math.max(maxX, building.originX() + building.rotatedWidth() - 1 + margin);
            minZ = Math.min(minZ, building.originZ() - margin);
            maxZ = Math.max(maxZ, building.originZ() + building.rotatedDepth() - 1 + margin);
        }
        double minY = data.centerPos().getY() - 96.0D;
        double maxY = data.centerPos().getY() + 97.0D;
        return new AABB(minX, minY, minZ, maxX + 1.0D, maxY, maxZ + 1.0D);
    }

    private static boolean workerBoundsFullyLoaded(ServerLevel level, SettlementData data, AABB bounds) {
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
'''
worker = replace_once(worker, old_evidence, new_evidence, 'worker lifecycle evidence')
old_workers_by_name = '''    private static List<Villager> workersByName(ServerLevel level, BlockPos center, String name) {
        AABB search = new AABB(
                center.getX() - 256.0D, center.getY() - 96.0D, center.getZ() - 256.0D,
                center.getX() + 257.0D, center.getY() + 97.0D, center.getZ() + 257.0D);
        List<Villager> workers = level.getEntitiesOfClass(Villager.class, search,
                villager -> villager.getCustomName() != null && name.equals(villager.getCustomName().getString()));
        workers.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        return workers;
    }
'''
new_workers_by_name = '''    private static List<Villager> workersByName(ServerLevel level, SettlementData data,
                                                BuildingType type, String name) {
        List<Villager> workers = new ArrayList<>();
        Set<java.util.UUID> ids = new HashSet<>();
        for (BuildingRecord building : buildings(data, type)) {
            AABB search = workerRouteBounds(data, building.workCenter(), 24);
            for (Villager villager : level.getEntitiesOfClass(Villager.class, search,
                    candidate -> candidate.getCustomName() != null
                            && name.equals(candidate.getCustomName().getString()))) {
                if (ids.add(villager.getUUID())) workers.add(villager);
            }
        }
        workers.sort(Comparator.comparing(villager -> villager.getUUID().toString()));
        return workers;
    }
'''
worker = replace_once(worker, old_workers_by_name, new_workers_by_name, 'ordinary worker search')
write(worker_path, worker)

# -----------------------------------------------------------------------------
# 2) Workshop/advanced-workshop assignment lookup must use the same lifecycle
#    envelope as loaded evidence. Workshop spawn also rechecks complete evidence.
# -----------------------------------------------------------------------------
workshop_path = JAVA / 'SettlementWorkshopService.java'
workshop = read(workshop_path)
workshop = replace_once(workshop,
    '    private static final double ASSIGNMENT_SEARCH_RADIUS = 192.0D;\n', '',
    'workshop fixed assignment radius')
workshop = replace_once(workshop,
'''        if (workshop == null || workshop.buildingType() != BuildingType.WORKSHOP
                || !level.hasChunkAt(workshop.workCenter())
                || findAssignedWorker(level, data, workshop) != null) return null;''',
'''        if (workshop == null || workshop.buildingType() != BuildingType.WORKSHOP
                || !SettlementWorkerService.workerRouteEvidenceLoaded(level, data, workshop.workCenter(), 12)
                || !level.hasChunkAt(WorkshopLayout.serviceCrate(workshop))
                || findAssignedWorker(level, data, workshop) != null) return null;''',
    'workshop spawn evidence')
old_workshop_find = '''    private static Villager findAssignedWorker(ServerLevel level, SettlementData data, BuildingRecord workshop) {
        String assignment = assignmentTag(workshop);
        BlockPos center = data.centerPos();
        AABB area = new AABB(
                center.getX() - ASSIGNMENT_SEARCH_RADIUS, center.getY() - 96.0D, center.getZ() - ASSIGNMENT_SEARCH_RADIUS,
                center.getX() + ASSIGNMENT_SEARCH_RADIUS + 1.0D, center.getY() + 97.0D,
                center.getZ() + ASSIGNMENT_SEARCH_RADIUS + 1.0D);
        List<Villager> assigned = level.getEntitiesOfClass(Villager.class, area,
                villager -> villager.entityTags().contains(WORKSHOP_WORKER_TAG)
                        && villager.entityTags().contains(assignment));
        return assigned.isEmpty() ? null : assigned.getFirst();
    }
'''
new_workshop_find = '''    private static Villager findAssignedWorker(ServerLevel level, SettlementData data, BuildingRecord workshop) {
        String assignment = assignmentTag(workshop);
        AABB area = SettlementWorkerService.workerRouteBounds(data, workshop.workCenter(), 12);
        List<Villager> assigned = level.getEntitiesOfClass(Villager.class, area,
                villager -> villager.entityTags().contains(WORKSHOP_WORKER_TAG)
                        && villager.entityTags().contains(assignment));
        return assigned.isEmpty() ? null : assigned.getFirst();
    }
'''
workshop = replace_once(workshop, old_workshop_find, new_workshop_find, 'workshop assignment search')
write(workshop_path, workshop)

advanced_path = JAVA / 'SettlementAdvancedWorkshopService.java'
advanced = read(advanced_path)
advanced = replace_once(advanced,
    '    private static final double ASSIGNMENT_SEARCH_RADIUS = 192.0D;\n', '',
    'advanced fixed assignment radius')
old_advanced_find = '''    private static Villager findAssignedWorker(ServerLevel level, SettlementData data, BuildingRecord workshop) {
        String assignment = assignmentTag(workshop);
        BlockPos center = data.centerPos();
        AABB area = new AABB(center.getX() - ASSIGNMENT_SEARCH_RADIUS, center.getY() - 96.0D,
                center.getZ() - ASSIGNMENT_SEARCH_RADIUS, center.getX() + ASSIGNMENT_SEARCH_RADIUS + 1.0D,
                center.getY() + 97.0D, center.getZ() + ASSIGNMENT_SEARCH_RADIUS + 1.0D);
        List<Villager> assigned = level.getEntitiesOfClass(Villager.class, area,
                villager -> villager.entityTags().contains(ADVANCED_WORKER_TAG)
                        && villager.entityTags().contains(assignment));
        return assigned.isEmpty() ? null : assigned.getFirst();
    }
'''
new_advanced_find = '''    private static Villager findAssignedWorker(ServerLevel level, SettlementData data, BuildingRecord workshop) {
        String assignment = assignmentTag(workshop);
        AABB area = SettlementWorkerService.workerRouteBounds(data, workshop.workCenter(), 12);
        List<Villager> assigned = level.getEntitiesOfClass(Villager.class, area,
                villager -> villager.entityTags().contains(ADVANCED_WORKER_TAG)
                        && villager.entityTags().contains(assignment));
        return assigned.isEmpty() ? null : assigned.getFirst();
    }
'''
advanced = replace_once(advanced, old_advanced_find, new_advanced_find, 'advanced assignment search')
write(advanced_path, advanced)

# -----------------------------------------------------------------------------
# 3) A town-side transporter can intentionally sleep in a HOUSE. Make house
#    footprints part of its existing lookup/evidence routeBounds authority.
# -----------------------------------------------------------------------------
outpost_path = JAVA / 'SettlementOutpostLogisticsService.java'
outpost = read(outpost_path)
outpost = replace_once(outpost,
'''        for (BlockPos pos : route) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return new AABB(minX - ROUTE_SEARCH_MARGIN, minY - 48.0D, minZ - ROUTE_SEARCH_MARGIN,''',
'''        for (BlockPos pos : route) {
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
        return new AABB(minX - ROUTE_SEARCH_MARGIN, minY - 48.0D, minZ - ROUTE_SEARCH_MARGIN,''',
    'transporter house rest bounds')
write(outpost_path, outpost)

# -----------------------------------------------------------------------------
# 4) Version + candidate lock.
# -----------------------------------------------------------------------------
props_path = PROJECT / 'gradle.properties'
props = read(props_path)
props = replace_once(props, 'mod_version=0.1.0-alpha.67', 'mod_version=0.1.0-alpha.68', 'props version')
props = replace_once(props,
    'and fail-closed outpost transporter assignment evidence that prevents unloaded route-bound residents from being mistaken for dead/missing replacements.',
    'and fail-closed outpost transporter assignment evidence that prevents unloaded route-bound residents from being mistaken for dead/missing replacements, plus rest-anchor-aware civilian lifecycle evidence that uses the same work/storage/housing bounds for lookup and absence authority.',
    'props description')
write(props_path, props)

lock_path = PROJECT / 'COMPANION_LOCK.json'
lock = json.loads(read(lock_path))
lock['generated_at'] = '2026-08-25'
lock['target']['frontier_settlement'] = '0.1.0-alpha.68'
note = ('Alpha.68 closes the normal-rest false-missing lifecycle edge: local production/workshop lookup evidence now includes '
        'the same work, concrete storage and HOUSE rest envelope used by worker search, while town-side outpost transporter '
        'routeBounds also include HOUSE rest footprints. Normal work/road movement remains physical and unloaded boundaries '
        'still pause; no force-load, teleport, UUID reservation ledger, virtual resident/cargo authority or companion dependency is added.')
if note not in lock['notes']:
    lock['notes'].append(note)
write(lock_path, json.dumps(lock, ensure_ascii=False, indent=2) + '\n')

# -----------------------------------------------------------------------------
# 5) Canonical docs. Preserve all old content; insert Alpha.68 and adjust only
#    current-version/range anchors.
# -----------------------------------------------------------------------------
canonical_path = PROJECT / 'CANONICAL_PLAN.md'
canonical = read(canonical_path)
canonical = replace_once(canonical,
    'Current canonical implementation: **0.1.0-alpha.67**.',
    'Current canonical implementation: **0.1.0-alpha.68**.',
    'canonical version')
alpha68_canonical = r'''### Alpha.68 rest-anchor-aware civilian lifecycle evidence

Alpha.68 closes a deterministic hole left between Alpha.66/67 lifecycle evidence and the already-existing resident night routine.

- `SettlementResidentRoutineService` intentionally sends lumber/farm/quarry/mine residents and normal workshop artisans to completed HOUSE rest slots at night; a town-side outpost transporter within the existing town-rest radius may also sleep at a HOUSE;
- before Alpha.68, local worker absence evidence covered work↔storage but not those real HOUSE destinations, while workshop/advanced lookup used an unrelated fixed 192-block AABB; therefore a legitimate resident sleeping in an unloaded house could disappear from the loaded entity query and be misread as dead/missing;
- `SettlementWorkerService.workerRouteBounds` is now the shared local-civilian lifecycle envelope: work center + every concrete settlement storage endpoint + every completed HOUSE footprint, all with the existing bounded work/path margin;
- `workerRouteEvidenceLoaded` checks every chunk intersecting that exact envelope with `hasChunkAt` only, and workshop/advanced-workshop assignment lookup now queries the same envelope rather than a separate 192-block radius;
- ordinary production workers remain pooled/automatic rather than manually assigned: their name lookup is the UUID-deduplicated union of the exact per-building lifecycle envelopes that must also be loaded before population reconciliation/replacement;
- normal workshop spawn now independently rechecks the same complete lifecycle evidence and service-crate chunk before entity insertion, preserving Alpha.64 `entity add -> real food4 -> population +1` commit ordering;
- `SettlementOutpostLogisticsService.routeBounds` now also includes completed HOUSE footprints before its existing 32-block search margin, so a transporter intentionally resting in town remains inside the same lookup/evidence authority as its persisted road;
- normal work navigation, transporter road waypoints, Alpha.42 debt pacing, Alpha.63 exact MAINHAND cargo death recovery and Alpha.67 fail-closed assignment replacement are unchanged;
- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, Alpha.27 remains the **single authority for outpost transport**, and **there is still only one authority for long-distance outpost transport**;
- no new SavedData, UUID reservation ledger, manual resident schedule/job UI, worker family, virtual resident/cargo balance, force-load, teleport or companion dependency is added.

This closes the deterministic `normal night rest -> house unload -> false missing -> food-funded duplicate replacement` path in static authority. **Long two-player repeated-death/night-rest/save-reload/reconnect runtime acceptance remains unfinished.**'''
canonical = insert_before_once(canonical,
    '### Alpha.67 fail-closed outpost transporter assignment evidence',
    alpha68_canonical,
    '### Alpha.68 rest-anchor-aware civilian lifecycle evidence',
    'canonical Alpha.68 section')
canonical = replace_once(canonical,
    '## 14. Current playable slice after Alpha.67',
    '## 14. Current playable slice after Alpha.68',
    'canonical current slice heading')
canonical = replace_once(canonical,
    '## 15. Unfinished original-scope priorities after Alpha.67',
    '## 15. Unfinished original-scope priorities after Alpha.68',
    'canonical unfinished heading')
canonical = replace_once(canonical,
    '2. Alpha.62–67 physical military/transporter/local-civilian cargo recovery and replacement boundaries are statically hardened; Alpha.67 additionally fails closed when transporter lookup-envelope chunks are unloaded; save-reload, route-unload, repeated death/replacement and no-dup/no-loss acceptance remain;',
    '2. Alpha.62–68 physical military/transporter/local-civilian cargo recovery and replacement boundaries are statically hardened; Alpha.67 fails closed on transporter lookup-envelope unload and Alpha.68 includes real HOUSE night-rest anchors in civilian/transporter absence evidence; save-reload, route-unload, night-rest, repeated death/replacement and no-dup/no-loss acceptance remain;',
    'canonical priority range')
canonical = insert_before_once(canonical,
    '- Alpha.67 fail-closed outpost transporter assignment evidence matching the exact transporter lookup envelope, without changing normal route-bound physical movement;',
    '- Alpha.68 rest-anchor-aware local civilian/transporter lifecycle evidence matching actual work/storage/HOUSE routine destinations;',
    '- Alpha.68 rest-anchor-aware local civilian/transporter lifecycle evidence',
    'canonical playable slice')
write(canonical_path, canonical)

gap_path = PROJECT / 'COMPLETION_GAP_AUDIT.md'
gap = read(gap_path)
gap = replace_once(gap, '현재 구현 기준: `0.1.0-alpha.67`', '현재 구현 기준: `0.1.0-alpha.68`', 'gap version')
alpha68_gap = r'''### Alpha.68 야간 휴식 anchor / 민간 assignment evidence 감사

- 실제 `SettlementResidentRoutineService`는 벌목/농사/채석/광산/일반 작업장 주민을 야간에 completed HOUSE slot으로 이동시키며, 본진 반경 안의 전초 운송 주민도 HOUSE에서 휴식할 수 있음;
- Alpha.66의 work↔storage evidence에는 HOUSE가 없고 workshop/advanced lookup은 별도 192블록 AABB였으므로, 정상 휴식 주민의 house chunk만 unloaded인 상태에서 false missing→food4 중복 replacement가 가능했음;
- Alpha.68 `workerRouteBounds`는 work center + 모든 concrete shared storage + 모든 completed HOUSE footprint를 기존 margin과 함께 하나의 lifecycle envelope로 정의;
- `workerRouteEvidenceLoaded`와 workshop/advanced assigned-worker query가 같은 envelope를 사용하므로 lookup 범위와 loaded absence proof가 다시 분리되지 않음;
- 일반 생산 주민은 개별 수동 assignment를 추가하지 않고 기존 type/name pool을 유지하며, 각 해당 building lifecycle envelope의 union에서 UUID dedupe 검색; replacement/population reconciliation은 같은 envelope들이 전부 loaded일 때만 수행;
- 일반 workshop `spawnAssignedWorker`도 mutation 직전 same lifecycle evidence + service crate loaded를 재검사해 public/direct caller가 partial evidence를 우회하지 못함;
- transporter `routeBounds`에 completed HOUSE footprint를 포함하고 기존 32-block margin을 적용해 town-rest 중인 운송 주민도 Alpha.67 assignment evidence/lookup 안에 유지;
- 실제 work/haul/road 이동, route waypoint `hasChunkAt`, Alpha.42 debt, Alpha.63 cargo recovery, food4 atomic arrival 순서는 변경 없음;
- 새 SavedData/UUID reservation ledger/수동 주민 관리/가상 resident/cargo/force-load/teleport 없음;
- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, `single authority for outpost transport`, **there is still only one authority for long-distance outpost transport** 유지.

따라서 정적으로 연결되는 `정상 야간 집 이동 -> house unload -> false missing -> duplicate resident/transporter` 경계를 닫았다. **실제 2인 야간 반복 death/replacement/save-reload/reconnect acceptance는 계속 남는다.**'''
gap = insert_before_once(gap,
    '### Alpha.67 전초 운송 주민 assignment evidence 감사',
    alpha68_gap,
    '### Alpha.68 야간 휴식 anchor / 민간 assignment evidence 감사',
    'gap Alpha.68 section')
gap = replace_once(gap,
    '2. Alpha.62–67 remote weapon/transporter/local-civilian physical cargo recovery + replacement의 route-unload/save-reload/reconnect/repeated-death no-loss/no-dup 실플레이 acceptance; Alpha.67 transporter lookup-envelope evidence도 실제 반복 unload에서 검증 필요;',
    '2. Alpha.62–68 remote weapon/transporter/local-civilian physical cargo recovery + replacement의 route-unload/night-rest/save-reload/reconnect/repeated-death no-loss/no-dup 실플레이 acceptance; Alpha.67 transporter lookup-envelope와 Alpha.68 HOUSE rest-anchor evidence도 실제 반복 unload에서 검증 필요;',
    'gap priority range')
write(gap_path, gap)

readme_path = PROJECT / 'README.md'
readme = read(readme_path)
readme = replace_once(readme, '## Current version: 0.1.0-alpha.67', '## Current version: 0.1.0-alpha.68', 'README version')
readme = replace_once(readme, 'No new Alpha.67 key was added.', 'No new Alpha.68 key was added.', 'README controls')
readme = replace_once(readme,
    'Alpha.40–67 deepen existing systems rather than inventing meaningless 16th–20th buildings.',
    'Alpha.40–68 deepen existing systems rather than inventing meaningless 16th–20th buildings.',
    'README family range')
alpha68_readme = r'''## Alpha.68 — rest-anchor-aware civilian lifecycle evidence

Alpha.68 fixes a deterministic unload/replacement edge created by the interaction between the existing night routine and Alpha.66/67 absence evidence.

- ordinary production residents and the normal workshop artisan really walk to completed houses at night; town-side outpost transporters can also use those house rest slots;
- the old work↔storage evidence did not include houses, so a legitimate sleeping resident could be hidden by an unloaded house chunk and look dead/missing to replacement logic;
- local civilian lifecycle bounds now include the real work target, every concrete shared-storage endpoint and every completed HOUSE footprint with the existing bounded margin;
- loaded evidence and workshop/advanced assignment lookup use that same bound instead of unrelated broad fixed-radius queries;
- ordinary production-worker lookup remains automatic and pooled by role, but searches the UUID-deduplicated union of the same per-building lifecycle bounds used for absence proof;
- normal workshop spawning rechecks complete lifecycle evidence immediately before entity insertion;
- outpost transporter `routeBounds` also includes completed HOUSE footprints before the existing 32-block margin, so a transporter sleeping near town remains visible to the same Alpha.67 assignment authority;
- physical work/hauling and road movement are unchanged, and unloaded road boundaries still pause instead of force-loading;
- **Transport workers belong to a specific outpost**, **군사 전초도 같은 도로 운송자가 역방향 보급**, **위험지역 군사 역할이 우선**, the **single authority for outpost transport**, and **there is still only one authority for long-distance outpost transport** remain intact;
- no resident-management screen, UUID reservation ledger, virtual cargo/population resource, force-load, teleport, new key or hard companion dependency was added.

This is deterministic pre-acceptance hardening, not a claim that two-player night/rest/death/save-reload acceptance is complete.'''
readme = insert_before_once(readme,
    '## Alpha.67 — fail-closed outpost transporter assignment evidence',
    alpha68_readme,
    '## Alpha.68 — rest-anchor-aware civilian lifecycle evidence',
    'README Alpha.68 section')
write(readme_path, readme)

# -----------------------------------------------------------------------------
# 6) Alpha.68 cumulative audit files.
# -----------------------------------------------------------------------------
source_audit = r'''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A67=ROOT/'tools/test_alpha67_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
# Preserve every Alpha.23-67 invariant while evaluating the current Alpha.68 version/lock.
a=text(A67).replace("print('Frontier Settlement alpha.23-67 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.67','0.1.0-alpha.68'); ns={'__file__':str(A67),'__name__':'__main__'}; exec(compile(a,str(A67),'exec'),ns,ns)
workers=text(JAVA/'settlement/SettlementWorkerService.java'); workshop=text(JAVA/'settlement/SettlementWorkshopService.java'); advanced=text(JAVA/'settlement/SettlementAdvancedWorkshopService.java'); outpost=text(JAVA/'settlement/SettlementOutpostLogisticsService.java'); routine=text(JAVA/'settlement/SettlementResidentRoutineService.java'); props=text(ROOT/'gradle.properties')
must(routine,('TOWN_WORKER_NAMES = Set.of(','"벌목 주민", "농사 주민", "채석 주민", "광산 주민", "작업장 주민"','moveToHouseSlot(residents.get(i), houses, i)','moveToHouseSlot(villager, houses, slot)'),'alpha.68 real house-rest call paths')
must(workers,(
    'static AABB workerRouteBounds(SettlementData data, BlockPos workCenter, int margin)',
    'if (building.buildingType() != BuildingType.HOUSE) continue',
    'building.originX() + building.rotatedWidth() - 1 + margin',
    'building.originZ() + building.rotatedDepth() - 1 + margin',
    'return workerBoundsFullyLoaded(level, data, workerRouteBounds(data, workCenter, margin))',
    'Math.floor(Math.nextDown(bounds.maxX))',
    'workersByName(level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME)',
    'Set<java.util.UUID> ids = new HashSet<>()',
    'AABB search = workerRouteBounds(data, building.workCenter(), 24)',
),'alpha.68 local civilian lookup/evidence envelope')
for fixed in ('center.getX() - 256.0D','center.getX() + 257.0D'):
    if fixed in workers: raise SystemExit(f'alpha.68 old ordinary fixed-radius authority remains: {fixed}')
must(workshop,(
    'SettlementWorkerService.workerRouteEvidenceLoaded(level, data, workshop.workCenter(), 12)',
    'SettlementWorkerService.workerRouteBounds(data, workshop.workCenter(), 12)',
    '!level.hasChunkAt(WorkshopLayout.serviceCrate(workshop))',
),'alpha.68 workshop evidence/search/spawn')
forbid(workshop,('ASSIGNMENT_SEARCH_RADIUS = 192.0D',),'alpha.68 workshop fixed-radius mismatch')
must(advanced,('SettlementWorkerService.workerRouteBounds(data, workshop.workCenter(), 12)',),'alpha.68 advanced search envelope')
forbid(advanced,('ASSIGNMENT_SEARCH_RADIUS = 192.0D',),'alpha.68 advanced fixed-radius mismatch')
must(outpost,(
    'Town-side transporters may intentionally sleep in completed houses',
    'if (building.buildingType() != BuildingType.HOUSE) continue',
    'building.originX() + building.rotatedWidth() - 1',
    'building.originZ() + building.rotatedDepth() - 1',
    'return new AABB(minX - ROUTE_SEARCH_MARGIN',
),'alpha.68 transporter house-rest routeBounds')
# Do not solve lifecycle visibility by loading/teleporting or adding a reservation authority.
for label,s in (('workers',workers),('workshop',workshop),('advanced',advanced),('outpost',outpost)):
    forbid(s,('setChunkForced','forceChunk','teleportTo(','CIVILIAN_RESERVATION_LEDGER','WORKER_UUID_LEDGER'),f'alpha.68 {label} no force/virtual lifecycle authority')
must(props,('mod_version=0.1.0-alpha.68','rest-anchor-aware civilian lifecycle evidence'),'alpha.68 props')
print('Frontier Settlement alpha.23-68 cumulative source audit: PASS')
'''
write(PROJECT / 'tools/test_alpha68_source.py', source_audit)

docs_audit = r'''#!/usr/bin/env python3
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; A67=ROOT/'tools/test_alpha67_docs.py'
def text(name): return (ROOT/name).read_text(encoding='utf-8')
def must(s,tokens,label):
    for token in tokens:
        if token not in s: raise SystemExit(f'{label} missing: {token}')
# Preserve Alpha.67 canonical constraints while evaluating Alpha.68 current-version metadata.
a=text('tools/test_alpha67_docs.py').replace("print('Frontier Settlement alpha.67 canonical docs audit: PASS')",'pass').replace('0.1.0-alpha.67','0.1.0-alpha.68'); ns={'__file__':str(A67),'__name__':'__main__'}; exec(compile(a,str(A67),'exec'),ns,ns)
canonical=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); readme=text('README.md'); lock=json.loads(text('COMPANION_LOCK.json'))
must(canonical,(
    'Current canonical implementation: **0.1.0-alpha.68**',
    '### Alpha.68 rest-anchor-aware civilian lifecycle evidence',
    'work center + every concrete settlement storage endpoint + every completed HOUSE footprint',
    'normal night rest -> house unload -> false missing -> food-funded duplicate replacement',
    'Long two-player repeated-death/night-rest/save-reload/reconnect runtime acceptance remains unfinished',
),'alpha.68 canonical')
must(gap,(
    '현재 구현 기준: `0.1.0-alpha.68`',
    '### Alpha.68 야간 휴식 anchor / 민간 assignment evidence 감사',
    '정상 야간 집 이동 -> house unload -> false missing -> duplicate resident/transporter',
    '실제 2인 야간 반복 death/replacement/save-reload/reconnect acceptance는 계속 남는다',
),'alpha.68 gap')
must(readme,('## Current version: 0.1.0-alpha.68','## Alpha.68 — rest-anchor-aware civilian lifecycle evidence','No new Alpha.68 key was added.'),'alpha.68 README')
if lock.get('status') != 'candidate_runtime_lock': raise SystemExit('alpha.68 companion lock overclaimed runtime status')
if lock.get('target',{}).get('frontier_settlement') != '0.1.0-alpha.68': raise SystemExit('alpha.68 companion lock target mismatch')
notes='\n'.join(lock.get('notes',[]))
must(notes,('Alpha.68 closes the normal-rest false-missing lifecycle edge','no force-load, teleport, UUID reservation ledger'),'alpha.68 companion note')
for forbidden in ('v0.2 complete','실플레이 검증 완료','full companion runtime: PASS'):
    if forbidden in readme: raise SystemExit(f'alpha.68 README overclaim: {forbidden}')
print('Frontier Settlement alpha.68 canonical docs audit: PASS')
'''
write(PROJECT / 'tools/test_alpha68_docs.py', docs_audit)

# -----------------------------------------------------------------------------
# 7) Canonical CI now runs Alpha.68 audits.
# -----------------------------------------------------------------------------
workflow_path = ROOT / '.github/workflows/build-frontier-settlement.yml'
workflow = read(workflow_path)
workflow = replace_once(workflow, 'Alpha.67 cumulative source audit', 'Alpha.68 cumulative source audit', 'workflow source label')
workflow = replace_once(workflow, 'python3 tools/test_alpha67_source.py', 'python3 tools/test_alpha68_source.py', 'workflow source tool')
workflow = replace_once(workflow, 'Alpha.67 canonical docs audit', 'Alpha.68 canonical docs audit', 'workflow docs label')
workflow = replace_once(workflow, 'python3 tools/test_alpha67_docs.py', 'python3 tools/test_alpha68_docs.py', 'workflow docs tool')
write(workflow_path, workflow)

# Hard fail on stale current-version anchors.
for path, stale in (
    (canonical_path, 'Current canonical implementation: **0.1.0-alpha.67**.'),
    (gap_path, '현재 구현 기준: `0.1.0-alpha.67`'),
    (readme_path, '## Current version: 0.1.0-alpha.67'),
    (props_path, 'mod_version=0.1.0-alpha.67'),
):
    if stale in read(path): raise SystemExit(f'stale Alpha.67 current-version anchor in {path}')

print('Frontier Settlement alpha.68 patch prepared')
