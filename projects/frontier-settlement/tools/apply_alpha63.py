#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'


def replace_once(path: Path, old: str, new: str, label: str):
    s = path.read_text(encoding='utf-8')
    if new in s and old not in s:
        return
    if old not in s:
        raise SystemExit(f'alpha.63 patch target missing: {label}')
    path.write_text(s.replace(old, new, 1), encoding='utf-8')


def insert_once(path: Path, anchor: str, block: str, marker: str, label: str):
    s = path.read_text(encoding='utf-8')
    if marker in s:
        return
    if anchor not in s:
        raise SystemExit(f'alpha.63 insert anchor missing: {label}')
    path.write_text(s.replace(anchor, block + anchor, 1), encoding='utf-8')


# 1) Physical transport worker death recovery + stale military weapon demand return.
logistics = JAVA / 'settlement/SettlementOutpostLogisticsService.java'
replace_once(
    logistics,
    'import net.minecraft.world.entity.EquipmentSlot;\nimport net.minecraft.world.entity.npc.villager.Villager;\nimport net.minecraft.world.item.ItemStack;',
    'import net.minecraft.world.entity.EquipmentSlot;\nimport net.minecraft.world.entity.item.ItemEntity;\nimport net.minecraft.world.entity.npc.villager.Villager;\nimport net.minecraft.world.item.ItemStack;',
    'logistics ItemEntity import')
replace_once(
    logistics,
    'import net.minecraft.world.phys.AABB;\n',
    'import net.minecraft.world.phys.AABB;\nimport net.neoforged.neoforge.event.entity.living.LivingDropsEvent;\n',
    'logistics LivingDropsEvent import')

spawn_anchor = '''    public static Villager spawnAssignedWorker(ServerLevel level, OutpostRecord outpost) {
        Villager worker = new Villager(EntityTypes.VILLAGER, level);
        BlockPos spawn = outpost.center().above();
        worker.setPos(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
        worker.setPersistenceRequired();
        worker.setNoAi(false);
        assignWorker(worker, outpost);
        level.addFreshEntity(worker);
        return worker;
    }

'''
death_block = '''    /**
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

'''
insert_once(logistics, spawn_anchor, spawn_anchor + death_block, 'public static void onLivingDrops(LivingDropsEvent event)', 'transport death recovery')

replace_once(
    logistics,
    '''        if (!(level.getBlockEntity(stock) instanceof Container container)) return;
        ItemStack remaining = SettlementInventory.insert(container, carried);
''',
    '''        if (!(level.getBlockEntity(stock) instanceof Container container)) return;
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
''',
    'delivery stale weapon demand guard')

frontier = JAVA / 'FrontierSettlement.java'
replace_once(
    frontier,
    'import kr.moonseungjun.frontiersettlement.settlement.SettlementMilitaryOutpostService;\nimport kr.moonseungjun.frontiersettlement.settlement.SettlementOutpostService;',
    'import kr.moonseungjun.frontiersettlement.settlement.SettlementMilitaryOutpostService;\nimport kr.moonseungjun.frontiersettlement.settlement.SettlementOutpostLogisticsService;\nimport kr.moonseungjun.frontiersettlement.settlement.SettlementOutpostService;',
    'Frontier logistics import')
replace_once(
    frontier,
    '        NeoForge.EVENT_BUS.addListener(SettlementBarracksService::onLivingDrops);\n        NeoForge.EVENT_BUS.addListener(SettlementMilitaryOutpostService::onLivingDrops);',
    '        NeoForge.EVENT_BUS.addListener(SettlementBarracksService::onLivingDrops);\n        NeoForge.EVENT_BUS.addListener(SettlementMilitaryOutpostService::onLivingDrops);\n        NeoForge.EVENT_BUS.addListener(SettlementOutpostLogisticsService::onLivingDrops);',
    'Frontier logistics death listener')

# 2) Version / canonical docs.
props = ROOT / 'gradle.properties'
replace_once(props, 'mod_version=0.1.0-alpha.62', 'mod_version=0.1.0-alpha.63', 'gradle version')
replace_once(
    props,
    'plus road-bound physical remote-sentry external-weapon reverse supply through the existing transporter.',
    'plus road-bound physical remote-sentry external-weapon reverse supply through the existing transporter, with in-flight stale-demand return and exact transporter-cargo death recovery.',
    'gradle description')

readme = ROOT / 'README.md'
replace_once(readme, '## Current version: 0.1.0-alpha.62', '## Current version: 0.1.0-alpha.63', 'README version')
replace_once(readme, 'No new Alpha.62 key was added.', 'No new Alpha.63 key was added.', 'README key')
replace_once(readme, 'Alpha.40–62 deepen existing systems', 'Alpha.40–63 deepen existing systems', 'README family range')
alpha63_readme = '''## Alpha.63 — transport transaction hardening\n\nAlpha.63 hardens the existing Alpha.27/41/62 physical outpost transporter for long-session failure edges without adding another logistics system.\n\n- military weapon demand is rechecked at the **actual outpost delivery point**, not trusted from the town departure decision;\n- if the sentry became armed or another recognized weapon reached the outpost while one was in flight, the transporter keeps that exact weapon in MAINHAND, clears only the military-supply trip state, and returns it through the existing road/town-deposit path;\n- the stale weapon is never inserted as a second reserve copy, deleted, converted to a number or teleported;\n- a tagged outpost transporter death clears vanilla equipment/drop-chance ambiguity and re-adds its exact carried MAINHAND ItemStack once as a recoverable world drop;\n- this death rule applies equally to normal outpost cargo and food/metal/wood/weapon reverse-supply cargo, preventing silent physical cargo loss;\n- worker tags, MAINHAND equipment and the persisted road remain the authority across normal entity save/reload; no new SavedData field or weapon-specific trip tag is introduced;\n- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, and **군사 전초도 같은 도로 운송자가 역방향 보급** remains true;\n- Alpha.27 remains the **single authority for outpost transport** and **there is still only one authority for long-distance outpost transport**;\n- no new worker, route controller, building, key, UI, currency, force-load, teleport or hard companion dependency is added.\n\nThis closes two statically reproducible no-loss/no-dup edges. Long save/reload, route-unload and two-player runtime acceptance is still not claimed.\n\n'''
insert_once(readme, '## Alpha.62 — road-bound remote sentry physical armament\n', alpha63_readme, '## Alpha.63 — transport transaction hardening', 'README alpha63 section')

can = ROOT / 'CANONICAL_PLAN.md'
replace_once(can, 'Current canonical implementation: **0.1.0-alpha.62**.', 'Current canonical implementation: **0.1.0-alpha.63**.', 'canonical version')
replace_once(
    can,
    'At Alpha.48 the physical external-weapon armory/loadout loop was unfinished. Alpha.57 now covers loaded town-barracks soldiers with actual MAINHAND ItemStacks and automation. The remaining remote-sentry extension must reuse the existing road-bound reverse-supply transporter and must not require manually opening every soldier.',
    'At Alpha.48 the physical external-weapon armory/loadout loop was unfinished. Alpha.57 covers loaded town-barracks soldiers with actual MAINHAND ItemStacks and automation, and Alpha.62 extends that same physical rule to remote sentries through the existing road-bound reverse-supply transporter.',
    'canonical historical armory wording')
alpha63_can = '''### Alpha.63 transporter transaction hardening\n\nAlpha.63 closes two deterministic acceptance-edge gaps inside the existing long-distance authority.\n\n- a military external weapon carried under `MILITARY_SUPPLY_TRIP_TAG` is revalidated at the destination immediately before insertion;\n- if `weaponSupplyShortage(...)` is already zero, the exact carried weapon remains in transporter MAINHAND and only the supply trip tag is removed; the next existing normal freight step returns it to concrete town storage;\n- therefore a player/manual stock change or sentry armament during a long trip cannot create an unintended second remote reserve weapon;\n- tagged transport-worker death clears ambiguous vanilla equipment drops and emits exactly one copy of the currently carried MAINHAND ItemStack for physical recovery;\n- empty-handed transporter death emits no cargo and cannot mint items;\n- the death handler is registered once on the common NeoForge event bus and does not alter ordinary villagers;\n- entity persistence continues to own transporter assignment tags and MAINHAND cargo; no new save field, virtual cargo, refund balance or recovery ledger exists;\n- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, **군사 전초도 같은 도로 운송자가 역방향 보급**, and **위험지역 군사 역할이 우선** remain unchanged;\n- Alpha.27 remains the **single authority for outpost transport** and **there is still only one authority for long-distance outpost transport**;\n- no new worker/trip family/route controller/building/key/UI/currency, no force-load/teleport, and no hard companion dependency.\n\nThis is static transaction hardening, not a substitute for route-unload/save-reload/two-player real-play acceptance.\n\n'''
insert_once(can, '### Alpha.62 road-bound remote-sentry physical armament\n', alpha63_can, '### Alpha.63 transporter transaction hardening', 'canonical alpha63 section')
replace_once(can, '## 14. Current playable slice after Alpha.62', '## 14. Current playable slice after Alpha.63', 'canonical playable heading')
replace_once(can, '- Alpha.62 same-road-transporter remote military external-weapon delivery -> local sentry MAINHAND equip -> exact death recovery;\n', '- Alpha.62 same-road-transporter remote military external-weapon delivery -> local sentry MAINHAND equip -> exact death recovery;\n- Alpha.63 in-flight military weapon demand revalidation + exact transport-worker carried-ItemStack death recovery;\n', 'canonical playable bullet')
replace_once(can, '## 15. Unfinished original-scope priorities after Alpha.62', '## 15. Unfinished original-scope priorities after Alpha.63', 'canonical unfinished heading')
replace_once(
    can,
    '2. Alpha.62 remote military weapon road-haul/local-equip/death-recovery save-reload, route-unload and no-dup acceptance; implementation now reuses the existing road-bound reverse-supply transporter;',
    '2. Alpha.62–63 remote military weapon road-haul/local-equip, in-flight stale-demand return, transporter-cargo recovery, save-reload, route-unload and no-dup acceptance; static failure edges are hardened but runtime acceptance remains;',
    'canonical priority2')

# Gap audit: fix stale Alpha.57-era wording, add Alpha.63 acceptance hardening.
gap = ROOT / 'COMPLETION_GAP_AUDIT.md'
replace_once(gap, '현재 구현 기준: `0.1.0-alpha.62`', '현재 구현 기준: `0.1.0-alpha.63`', 'gap version')
replace_once(
    gap,
    '이 문서는 현재 구현에 맞춰 원본 v0.2 범위를 축소하지 않는다. Alpha.57에서 본진 병영 실물 외부무기 armament까지 들어가도 원격 군사 무기 보급, rare-NPC breadth, 장시간 multiplayer 및 full companion runtime이 남아 있는 동안 완성이라고 부르지 않는다.',
    '이 문서는 현재 구현에 맞춰 원본 v0.2 범위를 축소하지 않는다. Alpha.57 본진 병영과 Alpha.62 원격 군사 실물 외부무기 armament가 구현됐어도, 해당 물류의 장시간 acceptance, rare-NPC breadth, 장시간 multiplayer 및 full companion runtime이 남아 있는 동안 완성이라고 부르지 않는다.',
    'gap intro stale remote wording')
replace_once(
    gap,
    '| 본진 병영 실물 외부무기 armory/loadout | **완료/부분** | Alpha.57 shared storage→soldier physical walk→exact MAINHAND ItemStack; remote sentry weapon supply는 미구현/부분 |',
    '| 실물 외부무기 군사 armory/loadout | **완료/부분** | Alpha.57 본진 병영 + Alpha.62 원격 전초 exact MAINHAND 물리 보급; Alpha.63 stale-demand/운송자 화물 회수 하드닝, 장시간 acceptance 남음 |',
    'gap resource table stale remote wording')
replace_once(
    gap,
    '- Alpha.48 시점에는 actual external-weapon physical armory가 미완료였고, Alpha.57에서 loaded 본진 병영은 real MAINHAND ItemStack 무장으로 구현됨; 원격 위험지역 전초 무기 역보급은 기존 road transporter authority를 재사용할 수 있을 때만 남은 범위.',
    '- Alpha.48 시점에는 actual external-weapon physical armory가 미완료였고, Alpha.57에서 loaded 본진 병영, Alpha.62에서 기존 road transporter authority를 재사용한 원격 위험지역 전초 real MAINHAND 무장이 구현됨.',
    'gap alpha48 stale remote wording')
alpha63_gap = '''### Alpha.63 운송 트랜잭션 하드닝 감사\n\n- Alpha.62 weapon demand는 출발 시뿐 아니라 실제 전초 창고 삽입 직전 다시 검사;\n- 이동 중 sentry가 무장되거나 outpost stockpile에 다른 recognized weapon이 생겨 demand0이면 carried weapon을 두 번째 재고로 삽입하지 않음;\n- exact carried weapon은 transporter MAINHAND에 그대로 남고 `MILITARY_SUPPLY_TRIP_TAG`만 해제되어 기존 일반 반환 경로로 본진 concrete storage에 돌아감;\n- stale cargo 삭제/teleport/virtual refund/새 return ledger 없음;\n- tagged transporter 사망 시 vanilla equipment drop ambiguity를 clear하고 현재 MAINHAND ItemStack exact copy1만 world recovery drop으로 복원;\n- empty MAINHAND 사망은 cargo0이라 아이템 생성 없음;\n- normal outpost cargo와 military/waterfront reverse-supply cargo 모두 같은 death-recovery 경계를 사용;\n- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, **군사 전초도 같은 도로 운송자가 역방향 보급** 유지;\n- `single authority for outpost transport` / `there is still only one authority for long-distance outpost transport` 유지;\n- 새 save field/trip family/worker/building/key/UI/currency/force-load/teleport/hard companion dependency 없음.\n\n따라서 정적으로 재현 가능한 in-flight 과잉 weapon 보급과 운송자 사망 silent cargo loss 경계는 닫혔다. 실제 route unload/save-reload/reconnect/반복 사망 no-dup acceptance는 계속 남는다.\n\n'''
insert_once(gap, '## 7. 도로 / 전초 / 영토\n', alpha63_gap, '### Alpha.63 운송 트랜잭션 하드닝 감사', 'gap alpha63 section')
replace_once(
    gap,
    '2. Alpha.62 remote weapon road-haul/local-equip/death-recovery의 route-unload/save-reload/no-dup 실플레이 acceptance;',
    '2. Alpha.62–63 remote weapon road-haul/local-equip/stale-demand return/transporter-cargo recovery의 route-unload/save-reload/reconnect/no-dup 실플레이 acceptance;',
    'gap priority2')

lock = ROOT / 'COMPANION_LOCK.json'
replace_once(lock, '"frontier_settlement": "0.1.0-alpha.62"', '"frontier_settlement": "0.1.0-alpha.63"', 'lock target')
lock_note = '    "Alpha.63 hardens the same road transporter transaction boundary: stale in-flight external-weapon demand is rechecked at the outpost before insertion and the exact carried stack returns through the existing road path, while a tagged transporter death recovers its exact MAINHAND cargo once. No new route authority, save field, virtual cargo, force-load, teleport or hard companion dependency is added.",\n'
insert_once(lock, '    "Xaero 26.4.2 remains candidate-only', lock_note, 'Alpha.63 hardens the same road transporter transaction boundary', 'lock alpha63 note')
replace_once(lock, 'so Alpha.62 keeps only HUD collision avoidance', 'so Alpha.63 keeps only HUD collision avoidance', 'lock Xaero version')

# 3) Alpha.63 cumulative source audit.
source_test = ROOT / 'tools/test_alpha63_source.py'
source_test.write_text(r'''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A62=ROOT/'tools/test_alpha62_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
a=text(A62).replace("print('Frontier Settlement alpha.23-62 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.62','0.1.0-alpha.63'); ns={'__file__':str(A62),'__name__':'__main__'}; exec(compile(a,str(A62),'exec'),ns,ns)
logistics=text(JAVA/'settlement/SettlementOutpostLogisticsService.java'); frontier=text(JAVA/'FrontierSettlement.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json'); building=text(JAVA/'settlement/BuildingType.java')
must(logistics,('public static void onLivingDrops(LivingDropsEvent event)','entityTags().contains(TRANSPORT_WORKER_TAG)','ItemStack carried = event.getEntity().getMainHandItem()','event.getDrops().clear()','carried.copy()','SettlementExternalContentService.isExternalWeapon(carried)','SettlementMilitaryOutpostService.weaponSupplyShortage(level, outpost) <= 0','worker.removeTag(MILITARY_SUPPLY_TRIP_TAG)','ItemStack remaining = SettlementInventory.insert(container, carried)'),'alpha.63 logistics transaction hardening')
must(frontier,('import kr.moonseungjun.frontiersettlement.settlement.SettlementOutpostLogisticsService;','NeoForge.EVENT_BUS.addListener(SettlementOutpostLogisticsService::onLivingDrops);'),'alpha.63 death handler registration')
if frontier.count('SettlementOutpostLogisticsService::onLivingDrops') != 1: raise SystemExit('alpha.63 transporter death listener must be registered exactly once')
delivery=logistics.find('private static void deliverMilitarySupply('); stale=logistics.find('SettlementMilitaryOutpostService.weaponSupplyShortage(level, outpost) <= 0',delivery); insert=logistics.find('SettlementInventory.insert(container, carried)',delivery)
if min(delivery,stale,insert)<0 or not (delivery < stale < insert): raise SystemExit('alpha.63 stale weapon demand must be checked immediately before destination insertion')
death=logistics.find('public static void onLivingDrops(LivingDropsEvent event)'); clear=logistics.find('event.getDrops().clear()',death); empty=logistics.find('if (carried.isEmpty()) return;',clear); recover=logistics.find('carried.copy()',empty)
if min(death,clear,empty,recover)<0 or not (death < clear < empty < recover): raise SystemExit('alpha.63 exact cargo recovery order invalid')
forbid(logistics,('MILITARY_WEAPON_SUPPLY_TRIP_TAG','MILITARY_WEAPON_RETURN_TRIP_TAG','TRANSPORT_RECOVERY_LEDGER','teleportTo(','forceChunk','setChunkForced'),'alpha.63 no second logistics/recovery authority')
enum_block=building.split('public enum BuildingType {',1)[1].split(';',1)[0]; actual=[line.strip().split('(',1)[0] for line in enum_block.splitlines() if '(' in line]; expected=['HOUSE','LUMBER_CAMP','FARM','QUARRY','MINE','WAREHOUSE','CONSTRUCTION_OFFICE','BLACKSMITH','WORKSHOP','ADVANCED_WORKSHOP','GUARD_POST','WATCHTOWER','BARRACKS','MARKET','CART_STATION']
if actual!=expected: raise SystemExit(f'alpha.63 expected exact 15 functional building families, got: {actual}')
must(props,('mod_version=0.1.0-alpha.63','in-flight stale-demand return and exact transporter-cargo death recovery'),'alpha.63 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.63"','Alpha.63 hardens the same road transporter transaction boundary','No new route authority, save field, virtual cargo, force-load, teleport or hard companion dependency'),'alpha.63 lock')
print('Frontier Settlement alpha.23-63 cumulative source audit: PASS')
''', encoding='utf-8')

# 4) Alpha.63 canonical docs audit.
docs_test = ROOT / 'tools/test_alpha63_docs.py'
docs_test.write_text(r'''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
readme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); lock=text('COMPANION_LOCK.json')
must(readme,('## Current version: 0.1.0-alpha.63','## Alpha.63 — transport transaction hardening','actual outpost delivery point','returns it through the existing road/town-deposit path','exact carried MAINHAND ItemStack once','Transport workers belong to a specific outpost','pause at unloaded route boundaries','there is still only one authority for long-distance outpost transport'),'alpha.63 README')
must(can,('Current canonical implementation: **0.1.0-alpha.63**','### Alpha.63 transporter transaction hardening','weaponSupplyShortage(...)','exact carried weapon remains in transporter MAINHAND','tagged transport-worker death','## 14. Current playable slice after Alpha.63','## 15. Unfinished original-scope priorities after Alpha.63','there is still only one authority for long-distance outpost transport'),'alpha.63 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.63`','Alpha.62 원격 전초 exact MAINHAND 물리 보급','### Alpha.63 운송 트랜잭션 하드닝 감사','실제 전초 창고 삽입 직전 다시 검사','MAINHAND ItemStack exact copy1','route unload/save-reload/reconnect/반복 사망 no-dup acceptance','there is still only one authority for long-distance outpost transport'),'alpha.63 gap')
must(lock,('"frontier_settlement": "0.1.0-alpha.63"','Alpha.63 hardens the same road transporter transaction boundary'),'alpha.63 lock')
print('Frontier Settlement alpha.63 canonical docs audit: PASS')
''', encoding='utf-8')

print('Applied Frontier Settlement alpha.63 transport transaction hardening.')
