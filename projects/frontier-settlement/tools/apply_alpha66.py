#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'


def read(rel):
    return (ROOT / rel).read_text(encoding='utf-8')


def write(rel, value):
    (ROOT / rel).write_text(value, encoding='utf-8')


def replace(rel, old, new, count=1):
    value = read(rel)
    if old not in value:
        raise SystemExit(f'{rel}: replacement marker missing: {old[:160]!r}')
    value = value.replace(old, new, count)
    write(rel, value)


# -----------------------------------------------------------------------------
# Product: civilian population evidence + advanced artisan lifecycle.
# -----------------------------------------------------------------------------
worker_rel = 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementWorkerService.java'
workers = read(worker_rel)
workers = workers.replace(
'''        List<Villager> mine = workersByName(level, data.centerPos(), MINE_WORKER_NAME);

        // Remote entities are legitimately unloaded. Reconcile population only while both road-bound
        // transport assignments and local workshop assignment evidence are completely visible.
        if (SettlementOutpostLogisticsService.allRoutesLoaded(level, data)
                && SettlementWorkshopService.allAssignmentsLoaded(level, data)) {
            int transport = SettlementOutpostLogisticsService.loadedAssignedWorkerCount(level, data);
            int workshop = SettlementWorkshopService.loadedAssignedWorkerCount(level, data);
            int actualPopulation = 1 + lumber.size() + farm.size() + quarry.size() + mine.size() + transport + workshop;
            if (data.population() != actualPopulation) data.setPopulation(actualPopulation);
        }
        if (data.population() >= data.housingCapacity()) return;

        if (tryFillJob(server, level, data, BuildingType.LUMBER_CAMP, LUMBER_WORKER_NAME, lumber.size())) return;
        if (tryFillJob(server, level, data, BuildingType.FARM, FARM_WORKER_NAME, farm.size())) return;
        if (tryFillJob(server, level, data, BuildingType.QUARRY, QUARRY_WORKER_NAME, quarry.size())) return;
        if (tryFillJob(server, level, data, BuildingType.MINE, MINE_WORKER_NAME, mine.size())) return;

        BuildingRecord missingWorkshop = SettlementWorkshopService.firstMissingLoadedAssignment(level, data);
''',
'''        List<Villager> mine = workersByName(level, data.centerPos(), MINE_WORKER_NAME);
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
''', 1)
if workers == read(worker_rel):
    raise SystemExit('SettlementWorkerService population block replacement failed')
write(worker_rel, workers)

replace(worker_rel,
'''        BuildingRecord missingWorkshop = SettlementWorkshopService.firstMissingLoadedAssignment(level, data);
        if (missingWorkshop != null) {
            if (!arrivalFoodAvailable(level, data)) return;
            Villager arrival = SettlementWorkshopService.spawnAssignedWorker(level, data, missingWorkshop);
            commitArrival(server, level, data, arrival);
            return;
        }

        OutpostRecord missing = SettlementOutpostLogisticsService.firstMissingLoadedAssignment(level, data);
''',
'''        BuildingRecord missingWorkshop = SettlementWorkshopService.firstMissingLoadedAssignment(level, data);
        if (missingWorkshop != null) {
            if (!arrivalFoodAvailable(level, data)) return;
            Villager arrival = SettlementWorkshopService.spawnAssignedWorker(level, data, missingWorkshop);
            commitArrival(server, level, data, arrival);
            return;
        }

        BuildingRecord missingAdvanced = SettlementAdvancedWorkshopService.firstMissingLoadedAssignment(level, data);
        if (missingAdvanced != null) {
            if (!arrivalFoodAvailable(level, data)) return;
            Villager arrival = SettlementAdvancedWorkshopService.spawnAssignedWorker(level, data, missingAdvanced);
            commitArrival(server, level, data, arrival);
            return;
        }

        OutpostRecord missing = SettlementOutpostLogisticsService.firstMissingLoadedAssignment(level, data);
''')

replace(worker_rel,
'''    private static boolean tryFillJob(MinecraftServer server, ServerLevel level, SettlementData data,
''',
'''    private static boolean localProductionEvidenceLoaded(ServerLevel level, SettlementData data) {
        if (!SettlementStorageService.storageAvailable(level, data)) return false;
        for (BuildingRecord building : data.buildings()) {
            BuildingType type = building.buildingType();
            if (type != BuildingType.LUMBER_CAMP && type != BuildingType.FARM
                    && type != BuildingType.QUARRY && type != BuildingType.MINE) continue;
            if (!workerRouteEvidenceLoaded(level, data, building.workCenter(), 24)) return false;
        }
        return true;
    }

    /**
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

    private static boolean tryFillJob(MinecraftServer server, ServerLevel level, SettlementData data,
''')

replace(worker_rel,
'''        if (worker.entityTags().contains(RESOURCE_WORKER_TAG)
                || worker.entityTags().contains(SettlementWorkshopService.WORKSHOP_WORKER_TAG)) {
''',
'''        if (worker.entityTags().contains(RESOURCE_WORKER_TAG)
                || worker.entityTags().contains(SettlementWorkshopService.WORKSHOP_WORKER_TAG)
                || worker.entityTags().contains(SettlementAdvancedWorkshopService.ADVANCED_WORKER_TAG)) {
''')

# Workshop/advanced assignment absence is authoritative only while the whole work<->storage envelope is loaded.
workshop_rel = 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementWorkshopService.java'
replace(workshop_rel,
'''            if (workshop.buildingType() != BuildingType.WORKSHOP) continue;
            if (!level.hasChunkAt(workshop.workCenter()) || !level.hasChunkAt(WorkshopLayout.serviceCrate(workshop))) {
                return false;
            }
''',
'''            if (workshop.buildingType() != BuildingType.WORKSHOP) continue;
            if (!SettlementWorkerService.workerRouteEvidenceLoaded(level, data, workshop.workCenter(), 12)
                    || !level.hasChunkAt(WorkshopLayout.serviceCrate(workshop))) {
                return false;
            }
''')

advanced_rel = 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementAdvancedWorkshopService.java'
replace(advanced_rel,
'''            if (workshop.buildingType() != BuildingType.ADVANCED_WORKSHOP) continue;
            if (!level.hasChunkAt(workshop.workCenter())
                    || !level.hasChunkAt(AdvancedWorkshopLayout.commissionCrate(workshop))) return false;
''',
'''            if (workshop.buildingType() != BuildingType.ADVANCED_WORKSHOP) continue;
            if (!SettlementWorkerService.workerRouteEvidenceLoaded(level, data, workshop.workCenter(), 12)
                    || !level.hasChunkAt(AdvancedWorkshopLayout.commissionCrate(workshop))
                    || !level.hasChunkAt(AdvancedWorkshopLayout.artisanHome(workshop))) return false;
''')

replace(advanced_rel,
'''    public static void spawnAssignedWorker(ServerLevel level, BuildingRecord workshop) {
        if (workshop == null || workshop.buildingType() != BuildingType.ADVANCED_WORKSHOP
                || !level.hasChunkAt(workshop.workCenter())) return;
        Villager worker = new Villager(EntityTypes.VILLAGER, level);
        BlockPos spawn = AdvancedWorkshopLayout.artisanHome(workshop);
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setCustomName(Component.literal(WORKER_NAME));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        worker.addTag(ADVANCED_WORKER_TAG);
        worker.addTag(assignmentTag(workshop));
        level.addFreshEntity(worker);
    }
''',
'''    public static Villager spawnAssignedWorker(ServerLevel level, SettlementData data, BuildingRecord workshop) {
        if (workshop == null || workshop.buildingType() != BuildingType.ADVANCED_WORKSHOP
                || !SettlementWorkerService.workerRouteEvidenceLoaded(level, data, workshop.workCenter(), 12)
                || !level.hasChunkAt(AdvancedWorkshopLayout.artisanHome(workshop))
                || findAssignedWorker(level, data, workshop) != null) return null;
        Villager worker = new Villager(EntityTypes.VILLAGER, level);
        BlockPos spawn = AdvancedWorkshopLayout.artisanHome(workshop);
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setCustomName(Component.literal(WORKER_NAME));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        worker.addTag(ADVANCED_WORKER_TAG);
        worker.addTag(assignmentTag(workshop));
        if (!level.addFreshEntity(worker)) return null;
        return worker;
    }
''')

# Remove the old free specialist spawn. SettlementWorkerService now owns food/housing/population commit.
service_rel = 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementService.java'
replace(service_rel,
'''        if (tick % 600 == 0) {
            BuildingRecord missingAdvanced = SettlementAdvancedWorkshopService.firstMissingLoadedAssignment(server.overworld(), data);
            if (missingAdvanced != null) SettlementAdvancedWorkshopService.spawnAssignedWorker(server.overworld(), missingAdvanced);
        }
''', '')

# Alpha.39's historical audit used the old direct/free spawn as runtime-integration evidence.
# Alpha.66 deliberately supersedes only that evidence: the advanced service still ticks in SettlementService,
# while the actual spawn is now under the food-funded civilian arrival authority in SettlementWorkerService.
a39_rel = 'tools/test_alpha39_source.py'
a39 = read(a39_rel)
old = '''service = text(JAVA / 'settlement/SettlementService.java')
if service.count('SettlementAdvancedWorkshopService.tick(server, data)') != 1:
    raise SystemExit('alpha.39 advanced workshop service must have exactly one server tick call')
must(service, ('SettlementAdvancedWorkshopService.firstMissingLoadedAssignment(server.overworld(), data)',
               'SettlementAdvancedWorkshopService.spawnAssignedWorker(server.overworld(), missingAdvanced)',
               'type == BuildingType.ADVANCED_WORKSHOP',
               'SettlementAdvancedWorkshopService.lockedReason(data)'),
     'alpha.39 advanced workshop runtime/unlock integration')
'''
new = '''service = text(JAVA / 'settlement/SettlementService.java')
if service.count('SettlementAdvancedWorkshopService.tick(server, data)') != 1:
    raise SystemExit('alpha.39 advanced workshop service must have exactly one server tick call')
must(service, ('type == BuildingType.ADVANCED_WORKSHOP',
               'SettlementAdvancedWorkshopService.lockedReason(data)'),
     'alpha.39 advanced workshop runtime/unlock integration')
# Alpha.66 supersedes the historical free-spawn wiring with the shared civilian arrival authority.
worker_service = text(JAVA / 'settlement/SettlementWorkerService.java')
must(worker_service, ('SettlementAdvancedWorkshopService.firstMissingLoadedAssignment(level, data)',
                      'SettlementAdvancedWorkshopService.spawnAssignedWorker(level, data, missingAdvanced)'),
     'alpha.39/66 advanced workshop civilian-arrival integration')
'''
if old not in a39:
    raise SystemExit('test_alpha39_source.py integration marker missing')
write(a39_rel, a39.replace(old, new, 1))

# Version and companion lock.
replace('gradle.properties', 'mod_version=0.1.0-alpha.65', 'mod_version=0.1.0-alpha.66')
props = read('gradle.properties')
props = props.replace('plus exact death recovery for real MAINHAND cargo carried by local production/workshop civilians.',
                      'plus exact death recovery for real MAINHAND cargo carried by local production/workshop civilians, and loaded-evidence-safe civilian population/replacement authority with food-funded advanced artisans.')
if 'loaded-evidence-safe civilian population/replacement authority' not in props:
    # Fallback for the current long description wording.
    props = props.replace('plus atomic food-funded worker arrival commits that never charge population/resources for a failed entity spawn.',
                          'plus atomic food-funded worker arrival commits that never charge population/resources for a failed entity spawn, plus exact local civilian cargo death recovery and loaded-evidence-safe civilian population/replacement authority with food-funded advanced artisans.')
write('gradle.properties', props)

lock = json.loads(read('COMPANION_LOCK.json'))
lock['target']['frontier_settlement'] = '0.1.0-alpha.66'
note = ('Alpha.66 unifies the civilian lifecycle boundary: population reconciliation and ordinary-worker replacement require fully loaded work-to-storage evidence, advanced-forging artisans join the existing atomic food4/housing/population arrival path, and their physical MAINHAND cargo uses the same local exact-death-recovery rule; old loaded artisans are preserved without retroactive food charging.')
if note not in lock['notes']:
    lock['notes'].append(note)
write('COMPANION_LOCK.json', json.dumps(lock, ensure_ascii=False, indent=2) + '\n')

# Documentation.
readme = read('README.md').replace('## Current version: 0.1.0-alpha.65', '## Current version: 0.1.0-alpha.66', 1)
section = '''## Alpha.66 — loaded-evidence-safe civilian lifecycle authority

Alpha.66 closes the next repeated-death/save-reload boundary without adding a resident-management screen or a new population ledger.

- population reconciliation no longer trusts a partial loaded-entity view: ordinary production-worker evidence must include every loaded chunk in the bounded work-site to concrete-storage envelope, while workshop/advanced-workshop assignments and outpost routes must also be fully visible;
- if that evidence is incomplete, an unloaded resident is **not** treated as dead, population is not repaired downward, and ordinary production replacement is paused instead of creating a possible duplicate;
- ordinary production workers still have no per-worker assignment UI; recruitment resumes automatically when the bounded local evidence becomes loaded again;
- the advanced-forging specialist is now a normal civilian for arrival economics: a missing advanced artisan uses the existing housing gate and the same atomic real-food4 path (`entity add -> food consume -> population +1`) as other civilian specialists;
- pre-Alpha.66 advanced artisans that already physically exist are preserved and are simply counted when complete evidence is visible; no historical food is retroactively charged;
- advanced artisans already carry real metal in MAINHAND, so Alpha.65 local civilian death recovery now includes `ADVANCED_WORKER_TAG`: the exact currently carried stack is recoverable once, empty hand emits zero;
- workshop and advanced-workshop assignment checks use the same loaded work-to-storage corridor evidence before declaring a worker missing, reducing unload/reload duplicate-spawn risk;
- no new SavedData field, family system, manual priority table, resident UI, recovery balance, force-load or teleport.

This is deterministic pre-acceptance hardening. Repeated kill/reload/reconnect with two real clients is still a real-play acceptance item.

'''
marker = '## Alpha.65 — exact local civilian cargo death recovery\n'
if marker not in readme or '## Alpha.66 — loaded-evidence-safe civilian lifecycle authority' in readme:
    if '## Alpha.66 — loaded-evidence-safe civilian lifecycle authority' not in readme:
        raise SystemExit('README Alpha.65 marker missing')
else:
    readme = readme.replace(marker, section + marker, 1)
readme = readme.replace('Alpha.40–65 deepen existing systems', 'Alpha.40–66 deepen existing systems')
write('README.md', readme)

can = read('CANONICAL_PLAN.md')
can = can.replace('Current canonical implementation: **0.1.0-alpha.65**.', 'Current canonical implementation: **0.1.0-alpha.66**.', 1)
can_section = '''### Alpha.66 loaded-evidence-safe civilian lifecycle authority

Alpha.66 makes population/replacement decisions depend on complete physical visibility rather than interpreting unloaded entities as deaths.

- ordinary lumber/farm/quarry/mine replacement and population reconciliation require a bounded loaded chunk envelope from each work site to every concrete settlement-storage endpoint; the check uses `hasChunkAt` only and never loads a chunk;
- workshop and advanced-workshop assignment evidence uses the same loaded work-to-storage rule, while outpost transport keeps its persisted-road `allRoutesLoaded` authority;
- incomplete evidence freezes reconciliation/replacement instead of decrementing population or deliberately spawning a second worker for a merely unloaded resident;
- advanced-forging specialists move under the existing civilian housing/food authority: a genuinely missing loaded assignment is added successfully first, then real food4 is consumed, then shared population increments;
- old advanced artisans remain valid physical residents with their existing entity/assignment tags and are counted when evidence is complete; Alpha.66 never retroactively charges their original spawn;
- the old `SettlementService` free advanced-artisan spawn is removed, so there is one civilian arrival transaction path rather than a special zero-food bypass;
- Alpha.65 local death recovery now recognizes `ADVANCED_WORKER_TAG`, preserving the exact real metal stack in MAINHAND once on death and minting nothing for an empty hand;
- no new SavedData, virtual resident reservation, family simulation, direct-management UI, force-load, teleport or second logistics authority.

This closes deterministic unloaded-resident false-death/duplicate-replacement and advanced-artisan lifecycle gaps. Long two-player repeated-death/save-reload runtime acceptance remains unfinished.

'''
marker = '### Alpha.65 local civilian physical-cargo death boundary\n'
if marker not in can or '### Alpha.66 loaded-evidence-safe civilian lifecycle authority' in can:
    if '### Alpha.66 loaded-evidence-safe civilian lifecycle authority' not in can:
        raise SystemExit('CANONICAL Alpha.65 marker missing')
else:
    can = can.replace(marker, can_section + marker, 1)
can = can.replace('## 14. Current playable slice after Alpha.65', '## 14. Current playable slice after Alpha.66')
can = can.replace('## 15. Unfinished original-scope priorities after Alpha.65', '## 15. Unfinished original-scope priorities after Alpha.66')
can = can.replace('- Alpha.65 exact local civilian MAINHAND cargo death recovery for production/workshop workers, with transporter exclusion;',
                  '- Alpha.65 exact local civilian MAINHAND cargo death recovery for production/workshop workers, with transporter exclusion;\n- Alpha.66 loaded-evidence-safe population/replacement reconciliation + food-funded advanced-artisan lifecycle and cargo recovery;')
can = can.replace('Alpha.62–65', 'Alpha.62–66')
write('CANONICAL_PLAN.md', can)

gap = read('COMPLETION_GAP_AUDIT.md')
gap = gap.replace('현재 구현 기준: `0.1.0-alpha.65`', '현재 구현 기준: `0.1.0-alpha.66`', 1)
gap_section = '''### Alpha.66 민간 주민 lifecycle / 언로드 증거 감사

- 생산 주민 population 재계산과 신규 대체는 작업지↔실제 공동 저장소 사이 bounded chunk envelope가 전부 loaded일 때만 수행;
- `hasChunkAt` 확인만 사용하며 chunk generation/force-load 없음;
- evidence가 불완전하면 unloaded 주민을 사망자로 간주하지 않고 population 감소/일반 생산 주민 대체를 보류;
- 일반 작업장과 고급 제작소 assignment도 work↔storage loaded evidence가 완전할 때만 missing 판정;
- 고급 제작 주민의 과거 무료 직생성 경로 제거, 앞으로 missing replacement는 기존 housing + real food4 + `addFreshEntity 성공 -> food consume -> population +1` 경로 사용;
- pre-Alpha.66에 이미 존재하는 고급 제작 주민은 삭제/재생성/소급 food 청구 없이 그대로 유지하고 complete evidence 시 population에 포함;
- 고급 제작 주민의 실제 MAINHAND metal도 Alpha.65 local civilian exact recovery 대상에 포함, empty hand는 drop0;
- 새 SavedData/reservation ledger/가상 주민/가상 화물/수동 주민 UI/force-load/teleport 없음.

따라서 정적으로 재현 가능한 `unload -> population 허위 감소 -> 중복 대체`와 고급 제작 주민의 zero-food replacement/cargo-loss 경계를 닫았다. 실제 2인 반복 사망/재접속/save-reload acceptance는 계속 남는다.

'''
marker = '### Alpha.65 로컬 주민 실물 화물 사망 경계 감사\n'
if marker not in gap or '### Alpha.66 민간 주민 lifecycle / 언로드 증거 감사' in gap:
    if '### Alpha.66 민간 주민 lifecycle / 언로드 증거 감사' not in gap:
        raise SystemExit('GAP Alpha.65 marker missing')
else:
    gap = gap.replace(marker, gap_section + marker, 1)
gap = gap.replace('Alpha.62–65', 'Alpha.62–66')
write('COMPLETION_GAP_AUDIT.md', gap)

# New cumulative source audit.
source_audit = r'''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A65=ROOT/'tools/test_alpha65_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
a=text(A65).replace("print('Frontier Settlement alpha.23-65 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.65','0.1.0-alpha.66'); ns={'__file__':str(A65),'__name__':'__main__'}; exec(compile(a,str(A65),'exec'),ns,ns)
workers=text(JAVA/'settlement/SettlementWorkerService.java'); workshop=text(JAVA/'settlement/SettlementWorkshopService.java'); advanced=text(JAVA/'settlement/SettlementAdvancedWorkshopService.java'); service=text(JAVA/'settlement/SettlementService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json')
must(workers,('boolean localEvidenceLoaded = localProductionEvidenceLoaded(level, data)','workerRouteEvidenceLoaded(ServerLevel level, SettlementData data','SettlementStorageService.storagePositions(data)','Math.floorDiv(minX, 16)','level.hasChunkAt(probe)','SettlementAdvancedWorkshopService.allAssignmentsLoaded(level, data)','SettlementAdvancedWorkshopService.loadedAssignedWorkerCount(level, data)','+ transport + workshop + advanced','if (localEvidenceLoaded) {','SettlementAdvancedWorkshopService.firstMissingLoadedAssignment(level, data)','SettlementAdvancedWorkshopService.spawnAssignedWorker(level, data, missingAdvanced)','SettlementAdvancedWorkshopService.ADVANCED_WORKER_TAG'),'alpha.66 civilian evidence/lifecycle')
must(workshop,('SettlementWorkerService.workerRouteEvidenceLoaded(level, data, workshop.workCenter(), 12)','WorkshopLayout.serviceCrate(workshop)'),'alpha.66 workshop loaded assignment evidence')
must(advanced,('public static Villager spawnAssignedWorker(ServerLevel level, SettlementData data, BuildingRecord workshop)','SettlementWorkerService.workerRouteEvidenceLoaded(level, data, workshop.workCenter(), 12)','AdvancedWorkshopLayout.artisanHome(workshop)','findAssignedWorker(level, data, workshop) != null','if (!level.addFreshEntity(worker)) return null','return worker;'),'alpha.66 advanced artisan atomic spawn')
if 'SettlementAdvancedWorkshopService.spawnAssignedWorker(server.overworld(), missingAdvanced)' in service or 'BuildingRecord missingAdvanced =' in service:
    raise SystemExit('alpha.66 old free advanced-artisan spawn remains in SettlementService')
# Complete evidence must precede destructive population reconciliation.
evidence=workers.index('if (localEvidenceLoaded'); reconcile=workers.index('if (data.population() != actualPopulation) data.setPopulation(actualPopulation)', evidence)
if not evidence < reconcile: raise SystemExit('alpha.66 population reconciliation is not evidence-gated')
# Advanced replacement must use the same commitArrival path; addFreshEntity -> food -> population ordering stays owned by Alpha.64.
missing=workers.index('BuildingRecord missingAdvanced'); spawn=workers.index('SettlementAdvancedWorkshopService.spawnAssignedWorker',missing); commit=workers.index('commitArrival(server, level, data, arrival)',spawn)
if not missing < spawn < commit: raise SystemExit('alpha.66 advanced replacement does not use civilian arrival commit')
forbid(workers,('CIVILIAN_RESERVATION_LEDGER','UNLOADED_RESIDENT_COUNT','forceChunk','setChunkForced','teleportTo('),'alpha.66 no virtual resident/load authority')
must(props,('mod_version=0.1.0-alpha.66','loaded-evidence-safe civilian population/replacement authority'),'alpha.66 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.66"','Alpha.66 unifies the civilian lifecycle boundary'),'alpha.66 lock')
print('Frontier Settlement alpha.23-66 cumulative source audit: PASS')
'''
write('tools/test_alpha66_source.py', source_audit)

docs_audit = r'''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
readme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); lock=text('COMPANION_LOCK.json')
must(readme,('## Current version: 0.1.0-alpha.66','## Alpha.66 — loaded-evidence-safe civilian lifecycle authority','unloaded resident is **not** treated as dead','atomic real-food4 path','pre-Alpha.66 advanced artisans','ADVANCED_WORKER_TAG','no new SavedData field'),'alpha.66 README')
must(can,('Current canonical implementation: **0.1.0-alpha.66**','### Alpha.66 loaded-evidence-safe civilian lifecycle authority','incomplete evidence freezes reconciliation/replacement','existing civilian housing/food authority','old `SettlementService` free advanced-artisan spawn is removed','## 14. Current playable slice after Alpha.66','## 15. Unfinished original-scope priorities after Alpha.66'),'alpha.66 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.66`','### Alpha.66 민간 주민 lifecycle / 언로드 증거 감사','unloaded 주민을 사망자로 간주하지 않고','housing + real food4','소급 food 청구 없이','실제 2인 반복 사망/재접속/save-reload acceptance'),'alpha.66 gap')
must(lock,('"frontier_settlement": "0.1.0-alpha.66"','Alpha.66 unifies the civilian lifecycle boundary'),'alpha.66 lock')
# Alpha.65 physical death boundary remains explicit.
must(readme,('## Alpha.65 — exact local civilian cargo death recovery','road-bound outpost transporters are explicitly excluded'),'alpha.65 retained README')
print('Frontier Settlement alpha.66 canonical docs audit: PASS')
'''
write('tools/test_alpha66_docs.py', docs_audit)

print('Frontier Settlement alpha.66 applicator: DONE')
