#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'


def replace_once(path: Path, old: str, new: str, label: str):
    s = path.read_text(encoding='utf-8')
    if new in s and old not in s:
        return
    if old not in s:
        raise SystemExit(f'alpha.65 patch target missing: {label}')
    path.write_text(s.replace(old, new, 1), encoding='utf-8')


def insert_before(path: Path, anchor: str, block: str, marker: str, label: str):
    s = path.read_text(encoding='utf-8')
    if marker in s:
        return
    if anchor not in s:
        raise SystemExit(f'alpha.65 insert anchor missing: {label}')
    path.write_text(s.replace(anchor, block + anchor, 1), encoding='utf-8')

# ---------------------------------------------------------------------------
# Alpha.65: exact carried-resource death recovery for local Frontier civilians.
# ---------------------------------------------------------------------------
worker = JAVA / 'settlement/SettlementWorkerService.java'
replace_once(worker,
'''import net.minecraft.world.entity.EquipmentSlot;\nimport net.minecraft.world.entity.npc.villager.Villager;\n''',
'''import net.minecraft.world.entity.EquipmentSlot;\nimport net.minecraft.world.entity.item.ItemEntity;\nimport net.minecraft.world.entity.npc.villager.Villager;\n''', 'worker ItemEntity import')
replace_once(worker,
'''import net.neoforged.neoforge.common.Tags;\n''',
'''import net.neoforged.neoforge.common.Tags;\nimport net.neoforged.neoforge.event.entity.living.LivingDropsEvent;\n''', 'worker LivingDropsEvent import')
replace_once(worker,
'''public final class SettlementWorkerService {\n    private static final String LUMBER_WORKER_NAME = "벌목 주민";\n''',
'''public final class SettlementWorkerService {\n    public static final String RESOURCE_WORKER_TAG = "frontier_settlement_resource_worker";\n    private static final String LUMBER_WORKER_NAME = "벌목 주민";\n''', 'resource worker tag')
replace_once(worker,
'''        worker.setPersistenceRequired();\n        worker.setNoAi(false);\n        if (!level.addFreshEntity(worker)) return null;\n''',
'''        worker.setPersistenceRequired();\n        worker.setNoAi(false);\n        worker.addTag(RESOURCE_WORKER_TAG);\n        if (!level.addFreshEntity(worker)) return null;\n''', 'tag new ordinary workers')

alpha65_worker = '''    /**\n     * Frontier-managed local civilians can carry the only physical copy of harvested/staged cargo.\n     * Remove vanilla equipment-drop randomness and expose that exact MAINHAND stack once.\n     * The road transporter keeps its separate Alpha.63 handler and is explicitly excluded here.\n     */\n    public static void onLivingDrops(LivingDropsEvent event) {\n        if (!(event.getEntity() instanceof Villager worker)) return;\n        if (worker.entityTags().contains(SettlementOutpostLogisticsService.TRANSPORT_WORKER_TAG)) return;\n        if (!isManagedCargoWorker(worker)) return;\n        ItemStack carried = worker.getMainHandItem();\n        event.getDrops().clear();\n        if (carried.isEmpty()) return;\n        event.getDrops().add(new ItemEntity(\n                worker.level(), worker.getX(), worker.getY(), worker.getZ(), carried.copy()));\n    }\n\n    private static boolean isManagedCargoWorker(Villager worker) {\n        if (worker.entityTags().contains(RESOURCE_WORKER_TAG)\n                || worker.entityTags().contains(SettlementWorkshopService.WORKSHOP_WORKER_TAG)) {\n            return true;\n        }\n        // Save-compatible fallback for pre-Alpha.65 ordinary workers that did not yet carry a role tag.\n        Component name = worker.getCustomName();\n        if (name == null) return false;\n        String value = name.getString();\n        return LUMBER_WORKER_NAME.equals(value) || FARM_WORKER_NAME.equals(value)\n                || QUARRY_WORKER_NAME.equals(value) || MINE_WORKER_NAME.equals(value);\n    }\n\n'''
insert_before(worker, '    private static void workLumber(', alpha65_worker,
              'public static void onLivingDrops(LivingDropsEvent event)', 'worker cargo death recovery')

main = JAVA / 'FrontierSettlement.java'
replace_once(main,
'''import kr.moonseungjun.frontiersettlement.settlement.SettlementWaterfrontService;\nimport kr.moonseungjun.frontiersettlement.settlement.SettlementWorkshopService;\n''',
'''import kr.moonseungjun.frontiersettlement.settlement.SettlementWaterfrontService;\nimport kr.moonseungjun.frontiersettlement.settlement.SettlementWorkerService;\nimport kr.moonseungjun.frontiersettlement.settlement.SettlementWorkshopService;\n''', 'main worker service import')
replace_once(main,
'''        NeoForge.EVENT_BUS.addListener(SettlementOutpostLogisticsService::onLivingDrops);\n''',
'''        NeoForge.EVENT_BUS.addListener(SettlementOutpostLogisticsService::onLivingDrops);\n        NeoForge.EVENT_BUS.addListener(SettlementWorkerService::onLivingDrops);\n''', 'worker drops event registration')

# Version.
props = ROOT / 'gradle.properties'
replace_once(props, 'mod_version=0.1.0-alpha.64', 'mod_version=0.1.0-alpha.65', 'version')
replace_once(props,
'plus atomic food-funded worker arrival commits that never charge population/resources for a failed entity spawn.',
'plus atomic food-funded worker arrival commits that never charge population/resources for a failed entity spawn, and exact death recovery for real MAINHAND cargo carried by local production/workshop civilians.',
'description')

# README.
readme = ROOT / 'README.md'
replace_once(readme, '## Current version: 0.1.0-alpha.64', '## Current version: 0.1.0-alpha.65', 'README version')
replace_once(readme, 'No new Alpha.63 key was added.', 'No new Alpha.65 key was added.', 'README control version')
replace_once(readme, 'Alpha.40–63 deepen existing systems', 'Alpha.40–65 deepen existing systems', 'README breadth version')
alpha65_readme = '''## Alpha.65 — exact local civilian cargo death recovery\n\nAlpha.65 closes the remaining deterministic death-loss gap for Frontier civilians that physically carry settlement resources. It adds no new job, inventory or recovery ledger.\n\n- lumber, farm, quarry and mine workers now receive a persistent Frontier resource-worker entity tag when newly recruited; pre-Alpha.65 saves remain recognized by their existing exact Frontier worker names;\n- workshop artisans are recognized through their existing workshop assignment tag;\n- when one of those managed civilians dies, vanilla equipment-drop randomness is cleared and its current MAINHAND ItemStack is emitted exactly once as a physical world drop;\n- an empty MAINHAND creates no item; death never refunds the worker's food recruitment cost or mints the resources it had already deposited;\n- the rule covers physically harvested logs/wheat/stone/ore and workshop metal that has actually left shared storage;\n- road-bound outpost transporters are explicitly excluded from this handler and retain the dedicated Alpha.63 exact-cargo recovery path, preventing double recovery;\n- active building/road public-works builders remain governed by their existing invulnerable active-project lifecycle rather than a second cargo-death system;\n- no new SavedData field, virtual cargo, refund currency, force-load, teleport, worker-management UI or logistics authority is introduced.\n\nThis closes the statically reproducible local civilian MAINHAND loss boundary. Repeated death/replacement, save/reload, route unload and two-client runtime acceptance remain real-play work rather than being claimed complete.\n\n'''
insert_before(readme, '## Alpha.64 — atomic food-funded worker arrivals', alpha65_readme,
              '## Alpha.65 — exact local civilian cargo death recovery', 'README alpha65 section')

# Canonical plan.
can = ROOT / 'CANONICAL_PLAN.md'
replace_once(can, 'Current canonical implementation: **0.1.0-alpha.64**.', 'Current canonical implementation: **0.1.0-alpha.65**.', 'canonical version')
replace_once(can, '## 14. Current playable slice after Alpha.64', '## 14. Current playable slice after Alpha.65', 'canonical playable header')
replace_once(can, '## 15. Unfinished original-scope priorities after Alpha.64', '## 15. Unfinished original-scope priorities after Alpha.65', 'canonical priorities header')
alpha65_can = '''### Alpha.65 local civilian physical-cargo death boundary\n\nAlpha.65 extends the existing no-loss physical ItemStack rule to local production/workshop civilians without creating a new resource authority.\n\n- new lumber/farm/quarry/mine workers carry `frontier_settlement_resource_worker`; legacy workers remain recognized through their exact pre-existing Frontier custom names;\n- workshop artisans reuse `frontier_settlement_workshop_worker`; no new workshop assignment state exists;\n- a managed local civilian death clears ambiguous vanilla equipment drops and emits exactly one copy of its current MAINHAND stack; empty hand emits zero;\n- this preserves only cargo that physically existed in that worker's hand: harvested resources and already-extracted workshop metal; recruitment food is not refunded and already-deposited resources are not recreated;\n- outpost transport workers are excluded and remain solely under Alpha.63's transporter recovery handler, so the same entity cannot be recovered by two Frontier authorities;\n- active public-works builders keep their existing invulnerable project lifecycle and are not turned into another death/recovery subsystem;\n- no SavedData field, recovery ledger, virtual inventory, currency, force-load, teleport, new worker family or management UI is added.\n\nThe physical rule is therefore consistent across Frontier entities that can own the only live copy of a carried settlement ItemStack, while repeated-death/reconnect runtime acceptance remains unfinished.\n\n'''
insert_before(can, '### Alpha.64 atomic worker-arrival transaction', alpha65_can,
              '### Alpha.65 local civilian physical-cargo death boundary', 'canonical alpha65 section')
replace_once(can,
'- Alpha.64 atomic food-funded ordinary/workshop/transporter arrival commit with assignment recheck and no failed-spawn charge;\n',
'- Alpha.64 atomic food-funded ordinary/workshop/transporter arrival commit with assignment recheck and no failed-spawn charge;\n- Alpha.65 exact local production/workshop civilian MAINHAND death recovery with legacy-name compatibility and transporter double-recovery exclusion;\n',
'canonical playable alpha65')
replace_once(can,
'2. Alpha.62–64 remote military weapon road-haul/local-equip, in-flight stale-demand return, transporter-cargo recovery and transporter replacement arrival commit; save-reload, route-unload, repeated death/replacement and no-dup acceptance remain;',
'2. Alpha.62–65 physical military/transporter/local-civilian cargo recovery and replacement boundaries are statically hardened; save-reload, route-unload, repeated death/replacement and no-dup/no-loss acceptance remain;',
'canonical priority alpha65')

# Completion gap audit.
gap = ROOT / 'COMPLETION_GAP_AUDIT.md'
replace_once(gap, '현재 구현 기준: `0.1.0-alpha.64`', '현재 구현 기준: `0.1.0-alpha.65`', 'gap version')
alpha65_gap = '''### Alpha.65 로컬 주민 실물 화물 사망 경계 감사\n\n- 신규 벌목/농사/채석/광산 주민은 persistent resource-worker entity tag 보유;\n- Alpha.65 이전 저장의 일반 생산 주민은 기존 exact custom name으로 계속 식별해 save migration 없이 보호;\n- 작업장 주민은 기존 workshop worker tag 재사용;\n- 관리 주민 사망 시 vanilla equipment-drop 확률을 신뢰하지 않고 현재 MAINHAND exact ItemStack copy1을 world recovery drop으로 복원;\n- empty MAINHAND이면 drop0, 주민 유입 food4는 환불하지 않으며 이미 창고에 넣은 자원도 재생성하지 않음;\n- 벌목 log / 농사 wheat / 채석 stone / 광산 ore / 공동창고에서 실제 추출된 workshop metal의 in-flight 물리 copy만 보호;\n- outpost transporter는 이 handler에서 명시적으로 제외되어 Alpha.63 transporter handler만 사용하므로 double recovery 없음;\n- 활성 건설/도로 공사 builder는 기존 project invulnerable lifecycle 유지;\n- 새 SavedData/recovery ledger/virtual cargo/refund currency/worker family/UI/force-load/teleport/logistics authority 없음.\n\n따라서 로컬 생산/작업장 주민이 들고 있던 실물 ItemStack의 정적 silent-loss 경계는 닫혔다. 실제 반복 사망/대체, save/reload, reconnect, 2인 no-loss/no-dup acceptance는 계속 남는다.\n\n'''
insert_before(gap, '### Alpha.64 주민 유입/운송자 대체 원자성 감사', alpha65_gap,
              '### Alpha.65 로컬 주민 실물 화물 사망 경계 감사', 'gap alpha65 section')
replace_once(gap,
'2. Alpha.62–64 remote weapon road-haul/local-equip/stale-demand return/transporter-cargo recovery + transporter replacement의 route-unload/save-reload/reconnect/repeated-death no-dup 실플레이 acceptance;',
'2. Alpha.62–65 remote weapon/transporter/local-civilian physical cargo recovery + replacement의 route-unload/save-reload/reconnect/repeated-death no-loss/no-dup 실플레이 acceptance;',
'gap priority alpha65')

# Companion lock version/note only; no companion dependency changes.
lock = ROOT / 'COMPANION_LOCK.json'
replace_once(lock, '"frontier_settlement": "0.1.0-alpha.64"', '"frontier_settlement": "0.1.0-alpha.65"', 'lock version')
replace_once(lock,
'''    "Alpha.64 makes existing food-funded resident arrivals atomic: ordinary workers, workshop artisans and outpost transporters must successfully enter the server world before real food and population commit, and an unexpected post-spawn food failure discards only that new worker. Assigned-worker spawn rechecks current loaded assignment and adds no reservation/save/logistics authority.",\n    "Xaero 26.4.2 remains candidate-only for Frontier marker synchronization: the historical public WaypointsManager API is absent, so Alpha.64 keeps only HUD collision avoidance rather than internal/mixin waypoint injection."\n''',
'''    "Alpha.64 makes existing food-funded resident arrivals atomic: ordinary workers, workshop artisans and outpost transporters must successfully enter the server world before real food and population commit, and an unexpected post-spawn food failure discards only that new worker. Assigned-worker spawn rechecks current loaded assignment and adds no reservation/save/logistics authority.",\n    "Alpha.65 gives local production/workshop civilians exact physical MAINHAND cargo recovery on death, with legacy-name compatibility and an explicit exclusion for road transporters so Alpha.63 remains their only recovery handler. No companion dependency, save ledger, virtual inventory or logistics authority is added.",\n    "Xaero 26.4.2 remains candidate-only for Frontier marker synchronization: the historical public WaypointsManager API is absent, so Alpha.65 keeps only HUD collision avoidance rather than internal/mixin waypoint injection."\n''', 'lock alpha65 note')

# Source audit.
source_audit = ROOT / 'tools/test_alpha65_source.py'
source_audit.write_text(r'''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A64=ROOT/'tools/test_alpha64_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
a=text(A64).replace("print('Frontier Settlement alpha.23-64 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.64','0.1.0-alpha.65'); ns={'__file__':str(A64),'__name__':'__main__'}; exec(compile(a,str(A64),'exec'),ns,ns)
workers=text(JAVA/'settlement/SettlementWorkerService.java'); main=text(JAVA/'FrontierSettlement.java'); logistics=text(JAVA/'settlement/SettlementOutpostLogisticsService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json')
must(workers,('RESOURCE_WORKER_TAG = "frontier_settlement_resource_worker"','worker.addTag(RESOURCE_WORKER_TAG)','public static void onLivingDrops(LivingDropsEvent event)','instanceof Villager worker','SettlementOutpostLogisticsService.TRANSPORT_WORKER_TAG','if (!isManagedCargoWorker(worker)) return','ItemStack carried = worker.getMainHandItem()','event.getDrops().clear()','if (carried.isEmpty()) return','carried.copy()','SettlementWorkshopService.WORKSHOP_WORKER_TAG','Save-compatible fallback for pre-Alpha.65 ordinary workers','LUMBER_WORKER_NAME.equals(value)','FARM_WORKER_NAME.equals(value)','QUARRY_WORKER_NAME.equals(value)','MINE_WORKER_NAME.equals(value)'),'alpha.65 local civilian cargo recovery')
must(main,('SettlementWorkerService','NeoForge.EVENT_BUS.addListener(SettlementWorkerService::onLivingDrops)'),'alpha.65 event registration')
must(logistics,('public static void onLivingDrops(LivingDropsEvent event)','TRANSPORT_WORKER_TAG','event.getDrops().clear()','carried.copy()'),'alpha.63 transporter recovery retained')
# The local handler must reject transporters before clearing drops to avoid two recovery authorities.
start=workers.index('public static void onLivingDrops(LivingDropsEvent event)'); exclude=workers.index('SettlementOutpostLogisticsService.TRANSPORT_WORKER_TAG',start); clear=workers.index('event.getDrops().clear()',exclude)
if not start < exclude < clear: raise SystemExit('alpha.65 transporter exclusion must precede local drop mutation')
# New ordinary workers must be tagged before addFreshEntity; legacy names remain fallback only.
spawn=workers.index('private static Villager spawnWorker'); tag=workers.index('worker.addTag(RESOURCE_WORKER_TAG)',spawn); add=workers.index('level.addFreshEntity(worker)',tag)
if not spawn < tag < add: raise SystemExit('alpha.65 resource worker tag must be applied before entity add')
forbid(workers,('LOCAL_CARGO_LEDGER','RECOVERY_BALANCE','forceChunk','setChunkForced','teleportTo('),'alpha.65 no virtual recovery authority')
must(props,('mod_version=0.1.0-alpha.65','exact death recovery for real MAINHAND cargo carried by local production/workshop civilians'),'alpha.65 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.65"','Alpha.65 gives local production/workshop civilians exact physical MAINHAND cargo recovery on death'),'alpha.65 lock')
print('Frontier Settlement alpha.23-65 cumulative source audit: PASS')
''', encoding='utf-8')

# Docs audit.
docs_audit = ROOT / 'tools/test_alpha65_docs.py'
docs_audit.write_text(r'''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
readme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); lock=text('COMPANION_LOCK.json')
must(readme,('## Current version: 0.1.0-alpha.65','## Alpha.65 — exact local civilian cargo death recovery','vanilla equipment-drop randomness is cleared','current MAINHAND ItemStack is emitted exactly once','pre-Alpha.65 saves','road-bound outpost transporters are explicitly excluded','no new SavedData field'),'alpha.65 README')
must(can,('Current canonical implementation: **0.1.0-alpha.65**','### Alpha.65 local civilian physical-cargo death boundary','exactly one copy of its current MAINHAND stack','outpost transport workers are excluded','## 14. Current playable slice after Alpha.65','## 15. Unfinished original-scope priorities after Alpha.65','no-dup/no-loss acceptance remain'),'alpha.65 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.65`','### Alpha.65 로컬 주민 실물 화물 사망 경계 감사','MAINHAND exact ItemStack copy1','Alpha.63 transporter handler만 사용','no-loss/no-dup 실플레이 acceptance'),'alpha.65 gap')
must(lock,('"frontier_settlement": "0.1.0-alpha.65"','Alpha.65 gives local production/workshop civilians exact physical MAINHAND cargo recovery on death'),'alpha.65 lock')
# Previous Alpha.64 contract remains explicitly documented.
must(readme,('## Alpha.64 — atomic food-funded worker arrivals','failed entity insertion consumes no food and adds no population','there is still only one authority for long-distance outpost transport'),'alpha.64 retained README')
must(can,('### Alpha.64 atomic worker-arrival transaction','failed `addFreshEntity` means no food loss and no population increment','there is still only one authority for long-distance outpost transport'),'alpha.64 retained canonical')
print('Frontier Settlement alpha.65 canonical docs audit: PASS')
''', encoding='utf-8')

print('Prepared Frontier Settlement alpha.65 civilian cargo recovery patch.')
