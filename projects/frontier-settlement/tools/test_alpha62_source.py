#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A61=ROOT/'tools/test_alpha61_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
a=text(A61).replace("print('Frontier Settlement alpha.23-61 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.61','0.1.0-alpha.62'); ns={'__file__':str(A61),'__name__':'__main__'}; exec(compile(a,str(A61),'exec'),ns,ns)
armory=text(JAVA/'settlement/SettlementMilitaryArmoryService.java'); military=text(JAVA/'settlement/SettlementMilitaryOutpostService.java'); logistics=text(JAVA/'settlement/SettlementOutpostLogisticsService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json'); building=text(JAVA/'settlement/BuildingType.java')
must(armory,('public static boolean tickOutpostArmament','BlockPos source = outpost.stockpile()','SettlementExternalContentService::isExternalWeapon, 1','soldier.setItemSlot(EquipmentSlot.MAINHAND, extracted)','level.hasChunkAt(source)'),'alpha.62 local sentry armament')
must(military,('public static int weaponSupplyShortage','if (sentry == null || !sentry.getMainHandItem().isEmpty()) return 0','SettlementExternalContentService.isExternalWeapon(container.getItem(slot))','SettlementMilitaryArmoryService.tickOutpostArmament(level, outpost, sentry)','new ItemEntity(','weapon.copy()'),'alpha.62 military demand/recovery')
must(logistics,('int foodShortage = SettlementMilitaryOutpostService.foodSupplyShortage','int metalShortage = SettlementMilitaryOutpostService.metalSupplyShortage','SettlementMilitaryOutpostService.weaponSupplyShortage(level, outpost) > 0','predicate = SettlementExternalContentService::isExternalWeapon','amount = 1','MILITARY_RETURN_TRIP_TAG','MILITARY_SUPPLY_TRIP_TAG'),'alpha.62 same-authority weapon supply')
food=logistics.find('if (foodShortage > 0)'); metal=logistics.find('else if (metalShortage > 0)',food); weapon=logistics.find('else if (SettlementMilitaryOutpostService.weaponSupplyShortage',metal)
if min(food,metal,weapon)<0 or not (food < metal < weapon): raise SystemExit('alpha.62 military supply priority must be food -> metal -> weapon')
forbid(logistics,('MILITARY_WEAPON_SUPPLY_TRIP_TAG','MILITARY_WEAPON_RETURN_TRIP_TAG','teleportTo(','forceChunk','setChunkForced'),'alpha.62 no second weapon logistics authority')
forbid(military,('teleportTo(','forceChunk','setChunkForced'),'alpha.62 sentry no teleport/force-load')
enum_block=building.split('public enum BuildingType {',1)[1].split(';',1)[0]; actual=[line.strip().split('(',1)[0] for line in enum_block.splitlines() if '(' in line]; expected=['HOUSE','LUMBER_CAMP','FARM','QUARRY','MINE','WAREHOUSE','CONSTRUCTION_OFFICE','BLACKSMITH','WORKSHOP','ADVANCED_WORKSHOP','GUARD_POST','WATCHTOWER','BARRACKS','MARKET','CART_STATION']
if actual!=expected: raise SystemExit(f'alpha.62 expected exact 15 functional building families, got: {actual}')
must(props,('mod_version=0.1.0-alpha.62','road-bound physical remote-sentry external-weapon reverse supply'),'alpha.62 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.62"','Alpha.62 extends the existing military reverse-supply transporter','third-priority exact external weapon cargo after food/metal reserves','No new transport authority, worker, save field, hard weapon dependency, force-load or teleport','"status": "candidate_runtime_lock"'),'alpha.62 lock')
patrol=military.find('private static void patrol('); target=military.find('Monster threat = nearestCombatThreat',patrol); arm_idle=military.find('if (SettlementMilitaryArmoryService.tickOutpostArmament(level, outpost, sentry)) return;',target); stand=military.find('standDown(outpost, sentry);',arm_idle)
if min(patrol,target,arm_idle,stand)<0 or not (patrol < target < arm_idle < stand): raise SystemExit('alpha.62 immediate combat must precede local sentry armament and stand-down')
print('Frontier Settlement alpha.23-62 cumulative source audit: PASS')
