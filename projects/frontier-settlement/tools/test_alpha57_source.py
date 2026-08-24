#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A56=ROOT/'tools/test_alpha56_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
a=text(A56).replace("print('Frontier Settlement alpha.23-56 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.56','0.1.0-alpha.57'); ns={'__file__':str(A56),'__name__':'__main__'}; exec(compile(a,str(A56),'exec'),ns,ns)
arm=text(JAVA/'settlement/SettlementMilitaryArmoryService.java'); barracks=text(JAVA/'settlement/SettlementBarracksService.java'); entity=text(JAVA/'content/FrontierSoldierEntity.java'); renderer=text(JAVA/'client/FrontierSoldierRenderer.java'); military=text(JAVA/'settlement/SettlementMilitaryOutpostService.java'); commands=text(JAVA/'command/SettlementCommands.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json'); building=text(JAVA/'settlement/BuildingType.java')
must(arm,('STORAGE_INTERACTION_RANGE_SQR = 9.0D','MAX_ARMORY_ROUTE_SQR = 160.0D * 160.0D','SettlementStorageService.storageAvailable(level, data)','SettlementStorageService.storagePositions(data)','SettlementExternalContentService.isExternalWeapon(carried)','SettlementStorageService.extract(','SettlementExternalContentService::isExternalWeapon, 1','soldier.setItemSlot(EquipmentSlot.MAINHAND, extracted)','soldier.getNavigation().moveTo(','containsExternalWeapon(container)'),'alpha.57 physical armory authority')
forbid(arm,('new ItemStack(','weaponsexpanded.','bettercombat.','ModList','forceChunk','setChunkForced','getChunk(','teleportTo(','data.updateResources(','data.addPopulation('),'alpha.57 armory may move one real item only')
must(barracks,('SOLDIER_SEARCH_RADIUS = 176.0D','patrol(level, data, barracks, slot, soldier)','SettlementMilitaryArmoryService.tickArmament(level, data, soldier)','loadedArmedSoldierCount','SettlementExternalContentService.isExternalWeapon(soldier.getMainHandItem())','ItemStack weapon = event.getEntity().getMainHandItem()','event.getDrops().clear()','ItemStack recovered = weapon.copy()','event.getDrops().add(new ItemEntity(','Defense always wins'),'alpha.57 barracks integration/recovery')
must(entity,('carries no server-side weapon ItemStack by default','Alpha.57 may assign one exact external weapon','Vanilla Mob equipment persistence/sync owns that ItemStack'),'alpha.57 soldier equipment persistence contract')
must(renderer,('state.rightHandItemStack = VISUAL_SERVICE_SWORD','ItemStack physicalWeapon = entity.getMainHandItem()','state.rightHandItemStack = physicalWeapon','physicalWeapon,','Never call entity.setItemSlot'),'alpha.57 physical weapon renderer')
forbid(renderer,('weaponsexpanded.','bettercombat.','ModList','entity.setItemSlot('),'alpha.57 renderer remains presentation-only')
# Alpha.62 supersedes the Alpha.57 remote-armory absence check; current transport authority is enforced by test_alpha62_source.py.
must(commands,('실물 외부무기 무장 "+SettlementBarracksService.loadedArmedSoldierCount'),'alpha.57 compact status')
# Exact 15 functional families remain unchanged.
enum_block=building.split('public enum BuildingType {',1)[1].split(';',1)[0]
actual=[line.strip().split('(',1)[0] for line in enum_block.splitlines() if '(' in line]
expected=['HOUSE','LUMBER_CAMP','FARM','QUARRY','MINE','WAREHOUSE','CONSTRUCTION_OFFICE','BLACKSMITH','WORKSHOP','ADVANCED_WORKSHOP','GUARD_POST','WATCHTOWER','BARRACKS','MARKET','CART_STATION']
if actual != expected: raise SystemExit(f'alpha.57 expected exact 15 functional building families, got: {actual}')
must(props,('mod_version=0.1.0-alpha.57','automated physical barracks armament','without per-soldier micromanagement'),'alpha.57 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.57"','Alpha.57 upgrades only loaded town barracks soldiers','extracts exactly one real Frontier-recognized external weapon ItemStack','remote military-outpost weapon supply remains deferred','"status": "candidate_runtime_lock"'),'alpha.57 lock')
print('Frontier Settlement alpha.23-57 cumulative source audit: PASS')
