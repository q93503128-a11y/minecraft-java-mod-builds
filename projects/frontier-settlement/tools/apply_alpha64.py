#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'


def replace_once(path: Path, old: str, new: str, label: str):
    s = path.read_text(encoding='utf-8')
    if new in s and old not in s:
        return
    if old not in s:
        raise SystemExit(f'alpha.64 patch target missing: {label}')
    path.write_text(s.replace(old, new, 1), encoding='utf-8')


def insert_before(path: Path, anchor: str, block: str, marker: str, label: str):
    s = path.read_text(encoding='utf-8')
    if marker in s:
        return
    if anchor not in s:
        raise SystemExit(f'alpha.64 insert anchor missing: {label}')
    path.write_text(s.replace(anchor, block + anchor, 1), encoding='utf-8')

# --- Worker spawn functions report actual entity-add success. ---
logistics = JAVA / 'settlement/SettlementOutpostLogisticsService.java'
replace_once(logistics,
'''    public static Villager spawnAssignedWorker(ServerLevel level, OutpostRecord outpost) {
        Villager worker = new Villager(EntityTypes.VILLAGER, level);
        BlockPos spawn = outpost.center().above();
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        assignWorker(worker, outpost);
        level.addFreshEntity(worker);
        return worker;
    }
''',
'''    public static Villager spawnAssignedWorker(ServerLevel level, SettlementData data, OutpostRecord outpost) {
        if (outpost == null || !routeFullyLoaded(level, data, outpost)
                || findAssignedWorker(level, data, outpost) != null) return null;
        Villager worker = new Villager(EntityTypes.VILLAGER, level);
        BlockPos spawn = outpost.center().above();
        if (!level.hasChunkAt(spawn)) return null;
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        assignWorker(worker, outpost);
        if (!level.addFreshEntity(worker)) return null;
        return worker;
    }
''', 'outpost assigned-worker spawn transaction')

workshop = JAVA / 'settlement/SettlementWorkshopService.java'
replace_once(workshop,
'''    public static void spawnAssignedWorker(ServerLevel level, BuildingRecord workshop) {
        if (workshop == null || workshop.buildingType() != BuildingType.WORKSHOP
                || !level.hasChunkAt(workshop.workCenter())) return;
        Villager worker = new Villager(EntityTypes.VILLAGER, level);
        BlockPos spawn = workshop.workCenter();
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setCustomName(Component.literal(WORKER_NAME));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        worker.addTag(WORKSHOP_WORKER_TAG);
        worker.addTag(assignmentTag(workshop));
        level.addFreshEntity(worker);
    }
''',
'''    public static Villager spawnAssignedWorker(ServerLevel level, SettlementData data, BuildingRecord workshop) {
        if (workshop == null || workshop.buildingType() != BuildingType.WORKSHOP
                || !level.hasChunkAt(workshop.workCenter())
                || findAssignedWorker(level, data, workshop) != null) return null;
        Villager worker = new Villager(EntityTypes.VILLAGER, level);
        BlockPos spawn = workshop.workCenter();
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setCustomName(Component.literal(WORKER_NAME));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        worker.addTag(WORKSHOP_WORKER_TAG);
        worker.addTag(assignmentTag(workshop));
        if (!level.addFreshEntity(worker)) return null;
        return worker;
    }
''', 'workshop assigned-worker spawn transaction')

worker = JAVA / 'settlement/SettlementWorkerService.java'
replace_once(worker,
'''        BuildingRecord missingWorkshop = SettlementWorkshopService.firstMissingLoadedAssignment(level, data);
        if (missingWorkshop != null) {
            if (!consumeArrivalFood(level, data)) return;
            SettlementWorkshopService.spawnAssignedWorker(level, missingWorkshop);
            finishArrival(server, data);
            return;
        }

        OutpostRecord missing = SettlementOutpostLogisticsService.firstMissingLoadedAssignment(level, data);
        if (missing != null) {
            if (!consumeArrivalFood(level, data)) return;
            SettlementOutpostLogisticsService.spawnAssignedWorker(level, missing);
            finishArrival(server, data);
        }
''',
'''        BuildingRecord missingWorkshop = SettlementWorkshopService.firstMissingLoadedAssignment(level, data);
        if (missingWorkshop != null) {
            if (!arrivalFoodAvailable(level, data)) return;
            Villager arrival = SettlementWorkshopService.spawnAssignedWorker(level, data, missingWorkshop);
            commitArrival(server, level, data, arrival);
            return;
        }

        OutpostRecord missing = SettlementOutpostLogisticsService.firstMissingLoadedAssignment(level, data);
        if (missing != null) {
            if (!arrivalFoodAvailable(level, data)) return;
            Villager arrival = SettlementOutpostLogisticsService.spawnAssignedWorker(level, data, missing);
            commitArrival(server, level, data, arrival);
        }
''', 'specialized worker arrival transaction')

replace_once(worker,
'''        BuildingRecord target = available.get(existingWorkers);
        if (!level.hasChunkAt(target.workCenter())) return true;
        if (!consumeArrivalFood(level, data)) return true;
        spawnWorker(level, target.workCenter(), workerName);
        finishArrival(server, data);
        return true;
    }

    private static boolean consumeArrivalFood(ServerLevel level, SettlementData data) {
        return SettlementStorageService.consume(level, data, 0L, 0L, ARRIVAL_FOOD_COST);
    }

    private static void finishArrival(MinecraftServer server, SettlementData data) {
''',
'''        BuildingRecord target = available.get(existingWorkers);
        if (!level.hasChunkAt(target.workCenter())) return true;
        if (!arrivalFoodAvailable(level, data)) return true;
        Villager arrival = spawnWorker(level, target.workCenter(), workerName);
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
                                         SettlementData data, Villager arrival) {
        if (arrival == null) return false;
        if (!consumeArrivalFood(level, data)) {
            arrival.discard();
            return false;
        }
        finishArrival(server, data);
        return true;
    }

    private static void finishArrival(MinecraftServer server, SettlementData data) {
''', 'ordinary arrival commit helper')

replace_once(worker,
'''    private static void spawnWorker(ServerLevel level, BlockPos spawn, String name) {
        if (!level.hasChunkAt(spawn)) return;
        Villager worker = new Villager(EntityTypes.VILLAGER, level);
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setCustomName(Component.literal(name));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        level.addFreshEntity(worker);
    }
''',
'''    private static Villager spawnWorker(ServerLevel level, BlockPos spawn, String name) {
        if (!level.hasChunkAt(spawn)) return null;
        Villager worker = new Villager(EntityTypes.VILLAGER, level);
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setCustomName(Component.literal(name));
        worker.setCustomNameVisible(true);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        if (!level.addFreshEntity(worker)) return null;
        return worker;
    }
''', 'ordinary worker spawn result')

# --- Version / docs. ---
props = ROOT / 'gradle.properties'
replace_once(props, 'mod_version=0.1.0-alpha.63', 'mod_version=0.1.0-alpha.64', 'version')
replace_once(props,
'in-flight stale-demand return and exact transporter-cargo death recovery.',
'in-flight stale-demand return and exact transporter-cargo death recovery, plus atomic food-funded worker arrival commits that never charge population/resources for a failed entity spawn.',
'description')

readme = ROOT / 'README.md'
replace_once(readme, '## Current version: 0.1.0-alpha.63', '## Current version: 0.1.0-alpha.64', 'README version')
alpha64_readme = '''## Alpha.64 — atomic food-funded worker arrivals\n\nAlpha.64 hardens resident replacement/recruitment before long two-player acceptance; it adds no new job or management UI.\n\n- ordinary production workers, workshop artisans and outpost-assigned transporters now expose real `addFreshEntity` success before arrival food/population can commit;\n- each path first requires loaded shared storage with at least the existing 4-food arrival cost, then creates the candidate entity, then consumes the same real food, and only then increments shared population;\n- failed entity insertion consumes no food and adds no population;\n- if physical food consumption unexpectedly fails after a successful entity insertion, that just-created worker is discarded and population remains unchanged;\n- workshop/outpost assigned-worker spawn also rechecks the current loaded assignment immediately before entity insertion, so a stale missing-worker observation cannot knowingly create a second assignment;\n- outpost transport replacement remains the same resident-attraction path and the same road-bound logistics authority; no instant cargo restoration or virtual replacement inventory exists;\n- Alpha.63 exact MAINHAND cargo recovery on transporter death remains unchanged;\n- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, Alpha.27 remains the **single authority for outpost transport**, and **there is still only one authority for long-distance outpost transport**;\n- no new save field, worker type, route controller, key, UI, currency, force-load or teleport.\n\nThis closes deterministic spawn/food/population transaction gaps, but repeated death/replacement, unload/reload and two-client runtime acceptance remain real-play items.\n\n'''
insert_before(readme, '## Alpha.63 — transport transaction hardening', alpha64_readme,
              '## Alpha.64 — atomic food-funded worker arrivals', 'README alpha64 section')

can = ROOT / 'CANONICAL_PLAN.md'
replace_once(can, 'Current canonical implementation: **0.1.0-alpha.63**.', 'Current canonical implementation: **0.1.0-alpha.64**.', 'canonical version')
replace_once(can, '## 14. Current playable slice after Alpha.63', '## 14. Current playable slice after Alpha.64', 'canonical playable header')
replace_once(can, '## 15. Unfinished original-scope priorities after Alpha.63', '## 15. Unfinished original-scope priorities after Alpha.64', 'canonical priorities header')
alpha64_can = '''### Alpha.64 atomic worker-arrival transaction\n\nAlpha.64 applies the same physical commit discipline to the existing food-funded civilian arrival paths.\n\n- the 4-food arrival cost is unchanged and still comes only from loaded concrete shared storage;\n- ordinary building workers, workshop artisans and road-bound outpost transporters must be successfully added to the server world before food/population commit;\n- failed `addFreshEntity` means no food loss and no population increment;\n- successful spawn followed by an unexpected food-consume failure discards only that new worker and leaves population unchanged;\n- workshop/outpost assignment spawners recheck a current assignment immediately before spawn, closing stale missing-assignment observations without introducing a reservation ledger;\n- transport cargo is not recreated by replacement; Alpha.63's physical world recovery drop remains the only failure-boundary cargo recovery;\n- save format, job families and route state are unchanged;\n- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, and Alpha.27 remains the **single authority for outpost transport**; **there is still only one authority for long-distance outpost transport**.\n\nThis is pre-acceptance correctness hardening, not proof of repeated-death/reconnect runtime acceptance.\n\n'''
insert_before(can, '### Alpha.63 transporter transaction hardening', alpha64_can,
              '### Alpha.64 atomic worker-arrival transaction', 'canonical alpha64 section')
replace_once(can,
'- Alpha.63 in-flight military weapon demand revalidation + exact transport-worker carried-ItemStack death recovery;\n',
'- Alpha.63 in-flight military weapon demand revalidation + exact transport-worker carried-ItemStack death recovery;\n- Alpha.64 atomic food-funded ordinary/workshop/transporter arrival commit with assignment recheck and no failed-spawn charge;\n',
'canonical playable alpha64')
replace_once(can,
'2. Alpha.62–63 remote military weapon road-haul/local-equip, in-flight stale-demand return, transporter-cargo recovery, save-reload, route-unload and no-dup acceptance; static failure edges are hardened but runtime acceptance remains;',
'2. Alpha.62–64 remote military weapon road-haul/local-equip, in-flight stale-demand return, transporter-cargo recovery and transporter replacement arrival commit; save-reload, route-unload, repeated death/replacement and no-dup acceptance remain;',
'canonical priority alpha64')

gap = ROOT / 'COMPLETION_GAP_AUDIT.md'
replace_once(gap, '현재 구현 기준: `0.1.0-alpha.63`', '현재 구현 기준: `0.1.0-alpha.64`', 'gap version')
alpha64_gap = '''### Alpha.64 주민 유입/운송자 대체 원자성 감사\n\n- 기존 주민 유입 비용 food4와 housing/population 규칙은 유지;\n- 일반 생산 주민, 작업장 주민, 전초 전담 운송 주민 모두 실제 entity add 성공을 반환;\n- loaded shared storage의 실제 food4 존재 확인 -> entity add 성공 -> 실제 food4 consume -> population +1 순서;\n- entity add 실패면 food/population 변경 0;\n- add 성공 뒤 예상 밖 consume 실패면 방금 생성한 주민을 discard하고 population 변경 0;\n- workshop/outpost assigned spawn은 mutation 직전 현재 assignment를 다시 검사해 stale missing 관측으로 의도적인 중복 assignment를 만들지 않음;\n- 운송자 사망 화물은 Alpha.63 exact MAINHAND recovery가 담당하며 replacement가 cargo를 복사/재생성하지 않음;\n- save field/reservation ledger/virtual food/refund currency/new worker type 없음;\n- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, `single authority for outpost transport`, **there is still only one authority for long-distance outpost transport** 유지.\n\n따라서 코드상 재현 가능한 failed-spawn 식량 손실·허위 population 증가 경계는 닫혔다. 실제 반복 사망/대체, route unload/reload, save/reconnect no-dup acceptance는 계속 남는다.\n\n'''
insert_before(gap, '### Alpha.63 운송 트랜잭션 하드닝 감사', alpha64_gap,
              '### Alpha.64 주민 유입/운송자 대체 원자성 감사', 'gap alpha64 section')
replace_once(gap,
'2. Alpha.62–63 remote weapon road-haul/local-equip/stale-demand return/transporter-cargo recovery의 route-unload/save-reload/reconnect/no-dup 실플레이 acceptance;',
'2. Alpha.62–64 remote weapon road-haul/local-equip/stale-demand return/transporter-cargo recovery + transporter replacement의 route-unload/save-reload/reconnect/repeated-death no-dup 실플레이 acceptance;',
'gap priority alpha64')

lock = ROOT / 'COMPANION_LOCK.json'
replace_once(lock, '"frontier_settlement": "0.1.0-alpha.63"', '"frontier_settlement": "0.1.0-alpha.64"', 'lock version')
replace_once(lock,
'    "Alpha.63 hardens the same road transporter transaction boundary: stale in-flight external-weapon demand is rechecked at the outpost before insertion and the exact carried stack returns through the existing road path, while a tagged transporter death recovers its exact MAINHAND cargo once. No new route authority, save field, virtual cargo, force-load, teleport or hard companion dependency is added.",',
'    "Alpha.63 hardens the same road transporter transaction boundary: stale in-flight external-weapon demand is rechecked at the outpost before insertion and the exact carried stack returns through the existing road path, while a tagged transporter death recovers its exact MAINHAND cargo once. No new route authority, save field, virtual cargo, force-load, teleport or hard companion dependency is added.",\n    "Alpha.64 makes existing food-funded resident arrivals atomic: ordinary workers, workshop artisans and outpost transporters must successfully enter the server world before real food and population commit, and an unexpected post-spawn food failure discards only that new worker. Assigned-worker spawn rechecks current loaded assignment and adds no reservation/save/logistics authority.",',
'lock alpha64 note')
replace_once(lock, 'so Alpha.63 keeps only HUD collision avoidance', 'so Alpha.64 keeps only HUD collision avoidance', 'lock Xaero version wording')

# --- New cumulative audits. ---
source_test = ROOT / 'tools/test_alpha64_source.py'
source_test.write_text(r'''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A63=ROOT/'tools/test_alpha63_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
a=text(A63).replace("print('Frontier Settlement alpha.23-63 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.63','0.1.0-alpha.64'); ns={'__file__':str(A63),'__name__':'__main__'}; exec(compile(a,str(A63),'exec'),ns,ns)
workers=text(JAVA/'settlement/SettlementWorkerService.java'); logistics=text(JAVA/'settlement/SettlementOutpostLogisticsService.java'); workshop=text(JAVA/'settlement/SettlementWorkshopService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json'); building=text(JAVA/'settlement/BuildingType.java')
must(workers,('arrivalFoodAvailable(ServerLevel level, SettlementData data)','SettlementStorageService.scan(level, data).food() >= ARRIVAL_FOOD_COST','commitArrival(MinecraftServer server, ServerLevel level','if (arrival == null) return false','if (!consumeArrivalFood(level, data))','arrival.discard()','finishArrival(server, data)','Villager arrival = SettlementWorkshopService.spawnAssignedWorker(level, data, missingWorkshop)','Villager arrival = SettlementOutpostLogisticsService.spawnAssignedWorker(level, data, missing)','private static Villager spawnWorker','if (!level.addFreshEntity(worker)) return null'),'alpha.64 atomic worker arrivals')
spawn=workers.find('private static boolean commitArrival('); consume=workers.find('if (!consumeArrivalFood(level, data))',spawn); discard=workers.find('arrival.discard()',consume); finish=workers.find('finishArrival(server, data)',discard)
if min(spawn,consume,discard,finish)<0 or not (spawn < consume < discard < finish): raise SystemExit('alpha.64 arrival commit order invalid')
must(logistics,('public static Villager spawnAssignedWorker(ServerLevel level, SettlementData data, OutpostRecord outpost)','routeFullyLoaded(level, data, outpost)','findAssignedWorker(level, data, outpost) != null','if (!level.addFreshEntity(worker)) return null','return worker;'),'alpha.64 transporter assignment spawn')
must(workshop,('public static Villager spawnAssignedWorker(ServerLevel level, SettlementData data, BuildingRecord workshop)','findAssignedWorker(level, data, workshop) != null','if (!level.addFreshEntity(worker)) return null','return worker;'),'alpha.64 workshop assignment spawn')
forbid(workers,('consumeArrivalFood(level, data)) return;\n            SettlementWorkshopService.spawnAssignedWorker','consumeArrivalFood(level, data)) return;\n            SettlementOutpostLogisticsService.spawnAssignedWorker'),'alpha.64 no precharge specialist arrivals')
forbid(logistics,('TRANSPORT_REPLACEMENT_LEDGER','forceChunk','setChunkForced','teleportTo('),'alpha.64 transporter replacement authority')
enum_block=building.split('public enum BuildingType {',1)[1].split(';',1)[0]; actual=[line.strip().split('(',1)[0] for line in enum_block.splitlines() if '(' in line]; expected=['HOUSE','LUMBER_CAMP','FARM','QUARRY','MINE','WAREHOUSE','CONSTRUCTION_OFFICE','BLACKSMITH','WORKSHOP','ADVANCED_WORKSHOP','GUARD_POST','WATCHTOWER','BARRACKS','MARKET','CART_STATION']
if actual!=expected: raise SystemExit(f'alpha.64 expected exact 15 functional building families, got: {actual}')
must(props,('mod_version=0.1.0-alpha.64','atomic food-funded worker arrival commits'),'alpha.64 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.64"','Alpha.64 makes existing food-funded resident arrivals atomic'),'alpha.64 lock')
print('Frontier Settlement alpha.23-64 cumulative source audit: PASS')
''', encoding='utf-8')

docs_test = ROOT / 'tools/test_alpha64_docs.py'
docs_test.write_text(r'''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
readme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); lock=text('COMPANION_LOCK.json')
must(readme,('## Current version: 0.1.0-alpha.64','## Alpha.64 — atomic food-funded worker arrivals','failed entity insertion consumes no food and adds no population','unexpected food consumption','Transport workers belong to a specific outpost','there is still only one authority for long-distance outpost transport'),'alpha.64 README')
must(can,('Current canonical implementation: **0.1.0-alpha.64**','### Alpha.64 atomic worker-arrival transaction','failed `addFreshEntity` means no food loss and no population increment','unexpected food-consume failure discards only that new worker','## 14. Current playable slice after Alpha.64','## 15. Unfinished original-scope priorities after Alpha.64','there is still only one authority for long-distance outpost transport'),'alpha.64 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.64`','### Alpha.64 주민 유입/운송자 대체 원자성 감사','entity add 실패면 food/population 변경 0','방금 생성한 주민을 discard','repeated-death no-dup 실플레이 acceptance','there is still only one authority for long-distance outpost transport'),'alpha.64 gap')
must(lock,('"frontier_settlement": "0.1.0-alpha.64"','Alpha.64 makes existing food-funded resident arrivals atomic'),'alpha.64 lock')
print('Frontier Settlement alpha.64 canonical docs audit: PASS')
''', encoding='utf-8')

print('Applied Frontier Settlement alpha.64 atomic worker arrivals.')
