#!/usr/bin/env python3
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
