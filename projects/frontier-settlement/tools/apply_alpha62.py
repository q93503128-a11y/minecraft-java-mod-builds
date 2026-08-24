#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement'


def read(path):
    return path.read_text(encoding='utf-8')


def write(path, text):
    path.write_text(text, encoding='utf-8')


def replace_once(text, old, new, label):
    if old not in text:
        raise SystemExit(f'alpha.62 patch missing {label}')
    if text.count(old) != 1:
        raise SystemExit(f'alpha.62 patch ambiguous {label}: {text.count(old)}')
    return text.replace(old, new, 1)

# ---------------------------------------------------------------------------
# Remote sentry local armament: only the final local stockpile -> sentry leg.
# Long-distance weapon movement remains owned by the existing outpost transporter.
# ---------------------------------------------------------------------------
armory_path = JAVA / 'SettlementMilitaryArmoryService.java'
armory = read(armory_path)
anchor = '''        soldier.getNavigation().stop();\n        return true;\n    }\n\n    private static BlockPos nearestWeaponSource'''
insert = '''        soldier.getNavigation().stop();\n        return true;\n    }\n\n    /**\n     * Local final leg for an already road-delivered outpost weapon. The sentry never reads town\n     * storage directly: the existing assigned transporter must first place the exact ItemStack in\n     * this outpost's physical stockpile.\n     */\n    public static boolean tickOutpostArmament(ServerLevel level, OutpostRecord outpost,\n                                              FrontierSoldierEntity soldier) {\n        if (soldier == null || !soldier.isAlive()) return false;\n        ItemStack carried = soldier.getMainHandItem();\n        if (SettlementExternalContentService.isExternalWeapon(carried)) return false;\n        if (!carried.isEmpty()) return false;\n\n        BlockPos source = outpost.stockpile();\n        if (!level.hasChunkAt(source)) return false;\n        if (!(level.getBlockEntity(source) instanceof Container container) || !containsExternalWeapon(container)) {\n            return false;\n        }\n        double distance = soldier.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D);\n        if (distance > STORAGE_INTERACTION_RANGE_SQR) {\n            soldier.getNavigation().moveTo(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D, ARMORY_WALK_SPEED);\n            return true;\n        }\n\n        ItemStack extracted = SettlementStorageService.extract(\n                level, source, SettlementExternalContentService::isExternalWeapon, 1);\n        if (extracted.isEmpty()) return false;\n        soldier.setItemSlot(EquipmentSlot.MAINHAND, extracted);\n        soldier.getNavigation().stop();\n        return true;\n    }\n\n    private static BlockPos nearestWeaponSource'''
armory = replace_once(armory, anchor, insert, 'outpost armament insertion')
write(armory_path, armory)

# ---------------------------------------------------------------------------
# Military overlay: request one weapon only after food/metal readiness, equip locally
# only when combat no longer needs the sentry, and recover the exact equipped stack.
# ---------------------------------------------------------------------------
military_path = JAVA / 'SettlementMilitaryOutpostService.java'
military = read(military_path)
military = replace_once(military,
'''import net.minecraft.world.entity.animal.golem.IronGolem;\nimport net.minecraft.world.entity.monster.Creeper;''',
'''import net.minecraft.world.entity.animal.golem.IronGolem;\nimport net.minecraft.world.entity.item.ItemEntity;\nimport net.minecraft.world.entity.monster.Creeper;''',
'military ItemEntity import')
military = replace_once(military,
'''            if (!evidence.dangerous()) {\n                if (sentry != null && tick % PATROL_INTERVAL_TICKS == 0) standDown(outpost, sentry);\n                continue;\n            }''',
'''            if (!evidence.dangerous()) {\n                if (sentry != null && tick % PATROL_INTERVAL_TICKS == 0) {\n                    // Combat has ended. Only now may a sentry walk to its local stockpile for a\n                    // weapon that the existing road transporter already delivered physically.\n                    if (!SettlementMilitaryArmoryService.tickOutpostArmament(level, outpost, sentry)) {\n                        standDown(outpost, sentry);\n                    }\n                }\n                continue;\n            }''',
'idle local armament')
anchor = '''    public static int metalSupplyShortage(ServerLevel level, OutpostRecord outpost) {\n        if (!isActiveMilitaryOutpost(level, outpost)) return 0;\n        if (!(level.getBlockEntity(outpost.stockpile()) instanceof Container container)) return 0;\n        long present = countMatching(container, SettlementStorageService::isMetalStack);\n        return Math.max(0, TARGET_METAL_RESERVE - (int) Math.min(Integer.MAX_VALUE, present));\n    }\n\n    public static DangerEvidence dangerEvidence'''
insert = '''    public static int metalSupplyShortage(ServerLevel level, OutpostRecord outpost) {\n        if (!isActiveMilitaryOutpost(level, outpost)) return 0;\n        if (!(level.getBlockEntity(outpost.stockpile()) instanceof Container container)) return 0;\n        long present = countMatching(container, SettlementStorageService::isMetalStack);\n        return Math.max(0, TARGET_METAL_RESERVE - (int) Math.min(Integer.MAX_VALUE, present));\n    }\n\n    /**\n     * One real weapon is enough for the one remote sentry. A weapon already in MAINHAND or already\n     * staged in this outpost stockpile closes demand so the road transporter cannot over-supply it.\n     */\n    public static int weaponSupplyShortage(ServerLevel level, OutpostRecord outpost) {\n        if (!isActiveMilitaryOutpost(level, outpost)) return 0;\n        FrontierSoldierEntity sentry = findSentry(level, outpost);\n        if (sentry == null || !sentry.getMainHandItem().isEmpty()) return 0;\n        if (!(level.getBlockEntity(outpost.stockpile()) instanceof Container container)) return 0;\n        for (int slot = 0; slot < container.getContainerSize(); slot++) {\n            if (SettlementExternalContentService.isExternalWeapon(container.getItem(slot))) return 0;\n        }\n        return 1;\n    }\n\n    public static DangerEvidence dangerEvidence'''
military = replace_once(military, anchor, insert, 'weapon shortage helper')
military = replace_once(military,
'''    /** Military sentries are combat/service units and never item/iron farms. */\n    public static void onLivingDrops(LivingDropsEvent event) {\n        if (event.getEntity().entityTags().contains(MILITARY_SENTRY_TAG)) event.getDrops().clear();\n    }''',
'''    /** Military sentries are never body/iron farms; one physically supplied weapon is recoverable. */\n    public static void onLivingDrops(LivingDropsEvent event) {\n        if (!event.getEntity().entityTags().contains(MILITARY_SENTRY_TAG)) return;\n        ItemStack weapon = event.getEntity().getMainHandItem();\n        event.getDrops().clear();\n        if (!SettlementExternalContentService.isExternalWeapon(weapon)) return;\n        event.getDrops().add(new ItemEntity(\n                event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(),\n                event.getEntity().getZ(), weapon.copy()));\n    }''',
'remote weapon death recovery')
write(military_path, military)

# ---------------------------------------------------------------------------
# Existing route-bound military reverse supply: food -> metal -> weapon exactly one.
# No new worker, trip tag, route or save authority.
# ---------------------------------------------------------------------------
logistics_path = JAVA / 'SettlementOutpostLogisticsService.java'
logistics = read(logistics_path)
logistics = replace_once(logistics,
'''        } else if (metalShortage > 0) {\n            predicate = SettlementStorageService::isMetalStack;\n            amount = Math.min(metalShortage, transportBatchSize(data));\n        } else {\n            worker.getNavigation().stop();\n            return;\n        }''',
'''        } else if (metalShortage > 0) {\n            predicate = SettlementStorageService::isMetalStack;\n            amount = Math.min(metalShortage, transportBatchSize(data));\n        } else if (SettlementMilitaryOutpostService.weaponSupplyShortage(level, outpost) > 0) {\n            // Third priority only: the same assigned transporter carries one exact external weapon\n            // after the outpost's survival food/metal reserves are already satisfied.\n            predicate = SettlementExternalContentService::isExternalWeapon;\n            amount = 1;\n        } else {\n            worker.getNavigation().stop();\n            return;\n        }''',
'food metal weapon priority')
write(logistics_path, logistics)

# Version / lock.
props_path = ROOT / 'gradle.properties'
props = read(props_path)
props = replace_once(props, 'mod_version=0.1.0-alpha.61', 'mod_version=0.1.0-alpha.62', 'mod version')
props = replace_once(props, 'and rollback-safe outpost grading cells.',
                     'and rollback-safe outpost grading cells, plus road-bound physical remote-sentry external-weapon reverse supply through the existing transporter.',
                     'mod description')
write(props_path, props)

lock_path = ROOT / 'COMPANION_LOCK.json'
lock = read(lock_path)
lock = replace_once(lock, '"frontier_settlement": "0.1.0-alpha.61"', '"frontier_settlement": "0.1.0-alpha.62"', 'lock target')
alpha61_note = '    "Alpha.61 makes outpost grading rollback-safe: each loaded grade cell snapshots every changed BlockState, checks every setBlock result, restores successful partial mutations in reverse order on failure, and advances the persisted outpost step only after the complete grade cell succeeds. No new save field, resource, worker, key or companion dependency is added.",\n'
alpha62_note = alpha61_note + '    "Alpha.62 extends the existing military reverse-supply transporter with a third-priority exact external weapon cargo after food/metal reserves: the same assigned road worker physically carries one recognized weapon to the outpost stockpile, the idle sentry equips that exact stack locally, and death returns the exact equipped weapon once. No new transport authority, worker, save field, hard weapon dependency, force-load or teleport is added.",\n'
lock = replace_once(lock, alpha61_note, alpha62_note, 'Alpha.62 lock note')
lock = lock.replace('so Alpha.61 keeps only HUD collision avoidance', 'so Alpha.62 keeps only HUD collision avoidance')
write(lock_path, lock)

# README.
readme_path = ROOT / 'README.md'
readme = read(readme_path)
readme = replace_once(readme, '## Current version: 0.1.0-alpha.61', '## Current version: 0.1.0-alpha.62', 'README version')
readme = readme.replace('No new Alpha.61 key was added.', 'No new Alpha.62 key was added.')
readme = readme.replace('Alpha.40–61 deepen existing systems', 'Alpha.40–62 deepen existing systems')
anchor = '## Alpha.61 — rollback-safe outpost grading\n'
section = '''## Alpha.62 — road-bound remote sentry physical armament\n\nAlpha.62 closes the remote half of the physical military armory without creating a second logistics system.\n\n- only an active dangerous **general** outpost with an existing unarmed sentry can request a weapon;\n- the existing military reverse-supply order remains **food reserve first -> metal reserve second -> weapon third**;\n- weapon demand is exactly one and becomes zero if the sentry is already armed or an external weapon is already waiting in the outpost stockpile;\n- the same outpost-assigned transporter walks the existing persisted road, extracts one real Frontier-recognized external weapon from loaded shared settlement storage, carries that exact MAINHAND stack, and inserts it into the exact outpost stockpile;\n- no direct town-storage -> sentry transfer exists. After combat ends, the sentry walks locally to its own stockpile and extracts exactly one real weapon into MAINHAND;\n- **위험지역 군사 역할이 우선**: an active threat always wins over an equipment walk, and food/metal survival supply wins over the weapon cargo;\n- sentry death still clears body/iron drops but now restores the exact physically equipped external weapon once for recovery instead of deleting it;\n- if the military overlay ends during a supply trip, existing transporter behavior keeps the carried ItemStack physical and returns it through the same route rather than teleporting or minting a replacement;\n- **군사 전초도 같은 도로 운송자가 역방향 보급**, **Transport workers belong to a specific outpost**, and they **pause at unloaded route boundaries**;\n- Alpha.27 remains the **single authority for outpost transport** and **there is still only one authority for long-distance outpost transport**;\n- no new trip tag, save field, worker, route controller, building, key, UI, currency, force-load, teleport or hard Weapons Expanded dependency is introduced.\n\nThis implements the planned remote physical-weapon supply slice, but save/reload, route-unload and no-dup behavior still require real-play acceptance.\n\n'''
readme = replace_once(readme, anchor, section + anchor, 'README Alpha.62 section')
write(readme_path, readme)

# Canonical plan.
can_path = ROOT / 'CANONICAL_PLAN.md'
can = read(can_path)
can = replace_once(can, 'Current canonical implementation: **0.1.0-alpha.61**.', 'Current canonical implementation: **0.1.0-alpha.62**.', 'canonical version')
can = can.replace('Alpha.40–61 deepen systems', 'Alpha.40–62 deepen systems')
alpha57_tail = '''At Alpha.48 the physical external-weapon armory/loadout loop was unfinished. Alpha.57 now covers loaded town-barracks soldiers with actual MAINHAND ItemStacks and automation. The remaining remote-sentry extension must reuse the existing road-bound reverse-supply transporter and must not require manually opening every soldier.\n'''
alpha62_can = alpha57_tail + '''\n### Alpha.62 road-bound remote-sentry physical armament\n\nAlpha.62 implements that remaining remote slice through the existing Alpha.27/41 transport authority rather than adding a remote armory or second carrier system.\n\n- only a loaded active dangerous general outpost with an existing empty-MAINHAND sentry can have weapon demand;\n- an external weapon already in sentry MAINHAND or already staged in that exact outpost stockpile makes weapon demand zero;\n- the existing military reverse-supply choice is ordered food reserve -> metal reserve -> one external weapon, so **위험지역 군사 역할이 우선** includes survival provisioning before upgrade cargo;\n- `SettlementOutpostLogisticsService` reuses the same `MILITARY_RETURN_TRIP_TAG` / `MILITARY_SUPPLY_TRIP_TAG`, assigned transporter, persisted road and physical MAINHAND cargo; no weapon-specific long-distance state exists;\n- the transporter extracts exactly one recognized external weapon from concrete loaded shared settlement storage, walks the road, and inserts the same ItemStack into the outpost stockpile;\n- the sentry never reads town storage. When there is no current combat pressure it walks only the local final leg to its own stockpile and extracts exactly one real weapon into vanilla MAINHAND;\n- active combat preempts local armament movement;\n- sentry death clears service/body drops but re-adds the exact equipped external weapon once, matching the no-mint recovery rule used by town barracks;\n- role loss or route unload never converts the weapon to a number: existing physical worker MAINHAND and route-pause/return behavior retains authority;\n- **군사 전초도 같은 도로 운송자가 역방향 보급**, **Transport workers belong to a specific outpost**, and they **pause at unloaded route boundaries**;\n- Alpha.27 stays the **single authority for outpost transport** and **there is still only one authority for long-distance outpost transport**;\n- no new save field/trip tag/worker/building/key/UI/currency, no force-load/teleport, and no hard Weapons Expanded or Better Combat Java dependency.\n\n'''
can = replace_once(can, alpha57_tail, alpha62_can, 'canonical Alpha.62 section')
can = can.replace('## 14. Current playable slice after Alpha.61', '## 14. Current playable slice after Alpha.62')
can = can.replace('## 15. Unfinished original-scope priorities after Alpha.61', '## 15. Unfinished original-scope priorities after Alpha.62')
can = replace_once(can,
'- Alpha.61 rollback-safe outpost grade-cell terrain mutation before persisted step advance;\n',
'- Alpha.61 rollback-safe outpost grade-cell terrain mutation before persisted step advance;\n- Alpha.62 same-road-transporter remote military external-weapon delivery -> local sentry MAINHAND equip -> exact death recovery;\n',
'canonical playable Alpha.62 bullet')
can = replace_once(can,
'2. remote military external-weapon supply only if the actual weapon ItemStack can ride the existing road-bound reverse-supply transporter; town barracks physical armament is covered by Alpha.57;',
'2. Alpha.62 remote military weapon road-haul/local-equip/death-recovery save-reload, route-unload and no-dup acceptance; implementation now reuses the existing road-bound reverse-supply transporter;',
'canonical remaining priority')
can = replace_once(can,
'- dangerous-region military activation/supply/stand-down;\n',
'- dangerous-region military activation/supply/stand-down, including Alpha.62 food/metal-before-weapon priority, physical road weapon cargo, local equip and exact death recovery;\n',
'canonical acceptance Alpha.62')
write(can_path, can)

# Gap audit.
gap_path = ROOT / 'COMPLETION_GAP_AUDIT.md'
gap = read(gap_path)
gap = replace_once(gap, '현재 구현 기준: `0.1.0-alpha.61`', '현재 구현 기준: `0.1.0-alpha.62`', 'gap version')
gap = replace_once(gap,
'| 실물 외부무기 군사 armory/loadout | **완료/부분** | Alpha.57 본진 병영은 real external-weapon MAINHAND loadout 완료/부분; 원격 위험지역 전초 실물 무기 역보급은 남음 |',
'| 실물 외부무기 군사 armory/loadout | **완료/부분** | Alpha.57 본진 병영 + Alpha.62 원격 위험지역 전초 real external-weapon MAINHAND 물리 보급; 장시간/save-reload acceptance 남음 |',
'gap armory row')
gap = replace_once(gap,
'따라서 physical military armory/loadout은 **본진 병영 기준 완료/부분**으로 전진했다. 원격 수비대 무기 ItemStack 역보급은 별도 남은 범위다.\n',
'''따라서 Alpha.57 시점 physical military armory/loadout은 **본진 병영 기준 완료/부분**으로 전진했고, 그 당시 **원격 위험지역 전초 실물 무기 역보급은 남음** 상태였다.\n\n### Alpha.62 원격 군사 실물 무기 역보급 감사\n\n- active dangerous general outpost + existing unarmed sentry에서만 weapon demand1;\n- sentry MAINHAND 또는 outpost stockpile에 recognized external weapon이 이미 있으면 demand0으로 과잉 보급 방지;\n- 같은 military reverse-supply 선택에서 food shortage -> metal shortage -> external weapon1 순서;\n- 본진의 concrete loaded shared storage에서 exact weapon1 실제 extraction;\n- 기존 outpost-assigned transporter, 기존 `MILITARY_RETURN_TRIP_TAG` / `MILITARY_SUPPLY_TRIP_TAG`, 기존 persisted road를 그대로 사용;\n- transporter MAINHAND의 exact ItemStack이 도로를 따라 outpost stockpile로 실제 이동·삽입;\n- sentry는 town storage를 직접 읽지 않고 전투가 끝난 뒤 local stockpile까지 걸어가 exact1을 MAINHAND로 추출;\n- sentry death는 body/service drops를 clear한 뒤 실제 장착 external weapon exact copy1만 recovery drop;\n- **위험지역 군사 역할이 우선**, food/metal survival reserve가 weapon보다 우선;\n- **군사 전초도 같은 도로 운송자가 역방향 보급**;\n- `single authority for outpost transport` / `there is still only one authority for long-distance outpost transport` 유지;\n- **Transport workers belong to a specific outpost** / **pause at unloaded route boundaries** 유지;\n- 새 save field/trip tag/worker/building/key/UI/currency/force-load/teleport/hard weapon class dependency 없음.\n\n따라서 Alpha.62에서 원격 수비대 무기 ItemStack 역보급도 구현 **완료/부분**으로 전진했다. 실제 route unload, save/reload, sentry death/recruit 반복 no-dup acceptance는 남는다.\n''',
'gap Alpha.62 audit')
gap = replace_once(gap,
'| 군사 역보급 | 완료/부분 | **군사 전초도 같은 도로 운송자가 역방향 보급** |',
'| 군사 역보급 | 완료/부분 | food/metal + Alpha.62 external weapon1을 **군사 전초도 같은 도로 운송자가 역방향 보급** |',
'gap military reverse supply row')
gap = replace_once(gap,
'2. remote military external-weapon supply는 기존 road-bound reverse-supply transporter가 실제 ItemStack을 운반할 수 있을 때만; town barracks armory는 Alpha.57 완료/부분;',
'2. Alpha.62 remote weapon road-haul/local-equip/death-recovery의 route-unload/save-reload/no-dup 실플레이 acceptance;',
'gap priority Alpha.62')
write(gap_path, gap)

# Source audit.
source_audit = '''#!/usr/bin/env python3\nfrom pathlib import Path\nROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A61=ROOT/'tools/test_alpha61_source.py'\ndef text(p): return p.read_text(encoding='utf-8')\ndef must(s,tokens,label):\n    for t in tokens:\n        if t not in s: raise SystemExit(f'{label} missing: {t}')\ndef forbid(s,tokens,label):\n    for t in tokens:\n        if t in s: raise SystemExit(f'{label}: {t}')\na=text(A61).replace("print('Frontier Settlement alpha.23-61 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.61','0.1.0-alpha.62'); ns={'__file__':str(A61),'__name__':'__main__'}; exec(compile(a,str(A61),'exec'),ns,ns)\narmory=text(JAVA/'settlement/SettlementMilitaryArmoryService.java'); military=text(JAVA/'settlement/SettlementMilitaryOutpostService.java'); logistics=text(JAVA/'settlement/SettlementOutpostLogisticsService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json'); building=text(JAVA/'settlement/BuildingType.java')\nmust(armory,('public static boolean tickOutpostArmament','BlockPos source = outpost.stockpile()','SettlementExternalContentService::isExternalWeapon, 1','soldier.setItemSlot(EquipmentSlot.MAINHAND, extracted)','level.hasChunkAt(source)'),'alpha.62 local sentry armament')\nmust(military,('public static int weaponSupplyShortage','if (sentry == null || !sentry.getMainHandItem().isEmpty()) return 0','SettlementExternalContentService.isExternalWeapon(container.getItem(slot))','SettlementMilitaryArmoryService.tickOutpostArmament(level, outpost, sentry)','new ItemEntity(','weapon.copy()'),'alpha.62 military demand/recovery')\nmust(logistics,('int foodShortage = SettlementMilitaryOutpostService.foodSupplyShortage','int metalShortage = SettlementMilitaryOutpostService.metalSupplyShortage','SettlementMilitaryOutpostService.weaponSupplyShortage(level, outpost) > 0','predicate = SettlementExternalContentService::isExternalWeapon','amount = 1','MILITARY_RETURN_TRIP_TAG','MILITARY_SUPPLY_TRIP_TAG'),'alpha.62 same-authority weapon supply')\nfood=logistics.find('if (foodShortage > 0)'); metal=logistics.find('else if (metalShortage > 0)',food); weapon=logistics.find('else if (SettlementMilitaryOutpostService.weaponSupplyShortage',metal)\nif min(food,metal,weapon)<0 or not (food < metal < weapon): raise SystemExit('alpha.62 military supply priority must be food -> metal -> weapon')\nforbid(logistics,('MILITARY_WEAPON_SUPPLY_TRIP_TAG','MILITARY_WEAPON_RETURN_TRIP_TAG','teleportTo(','forceChunk','setChunkForced'),'alpha.62 no second weapon logistics authority')\nforbid(military,('teleportTo(','forceChunk','setChunkForced'),'alpha.62 sentry no teleport/force-load')\nenum_block=building.split('public enum BuildingType {',1)[1].split(';',1)[0]; actual=[line.strip().split('(',1)[0] for line in enum_block.splitlines() if '(' in line]; expected=['HOUSE','LUMBER_CAMP','FARM','QUARRY','MINE','WAREHOUSE','CONSTRUCTION_OFFICE','BLACKSMITH','WORKSHOP','ADVANCED_WORKSHOP','GUARD_POST','WATCHTOWER','BARRACKS','MARKET','CART_STATION']\nif actual!=expected: raise SystemExit(f'alpha.62 expected exact 15 functional building families, got: {actual}')\nmust(props,('mod_version=0.1.0-alpha.62','road-bound physical remote-sentry external-weapon reverse supply'),'alpha.62 props')\nmust(lock,('"frontier_settlement": "0.1.0-alpha.62"','Alpha.62 extends the existing military reverse-supply transporter','third-priority exact external weapon cargo after food/metal reserves','No new transport authority, worker, save field, hard weapon dependency, force-load or teleport','"status": "candidate_runtime_lock"'),'alpha.62 lock')\nprint('Frontier Settlement alpha.23-62 cumulative source audit: PASS')\n'''
write(ROOT / 'tools/test_alpha62_source.py', source_audit)

docs_audit = '''#!/usr/bin/env python3\nfrom pathlib import Path\nROOT=Path(__file__).resolve().parents[1]\ndef text(n): return (ROOT/n).read_text(encoding='utf-8')\ndef must(s,tokens,label):\n    for t in tokens:\n        if t not in s: raise SystemExit(f'{label} missing: {t}')\nreadme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); lock=text('COMPANION_LOCK.json')\nmust(readme,('## Current version: 0.1.0-alpha.62','## Alpha.62 — road-bound remote sentry physical armament','food reserve first -> metal reserve second -> weapon third','same outpost-assigned transporter','exact physically equipped external weapon once','there is still only one authority for long-distance outpost transport'),'alpha.62 README')\nmust(can,('Current canonical implementation: **0.1.0-alpha.62**','### Alpha.62 road-bound remote-sentry physical armament','food reserve -> metal reserve -> one external weapon','existing `MILITARY_RETURN_TRIP_TAG` / `MILITARY_SUPPLY_TRIP_TAG`','## 14. Current playable slice after Alpha.62','## 15. Unfinished original-scope priorities after Alpha.62','Alpha.62 remote military weapon road-haul/local-equip/death-recovery'),'alpha.62 canonical')\nmust(gap,('현재 구현 기준: `0.1.0-alpha.62`','Alpha.57 본진 병영 + Alpha.62 원격 위험지역 전초 real external-weapon MAINHAND 물리 보급','원격 위험지역 전초 실물 무기 역보급은 남음','### Alpha.62 원격 군사 실물 무기 역보급 감사','food shortage -> metal shortage -> external weapon1','군사 전초도 같은 도로 운송자가 역방향 보급','there is still only one authority for long-distance outpost transport','route unload, save/reload, sentry death/recruit 반복 no-dup acceptance'),'alpha.62 gap')\nmust(lock,('"frontier_settlement": "0.1.0-alpha.62"','Alpha.62 extends the existing military reverse-supply transporter'),'alpha.62 lock')\nprint('Frontier Settlement alpha.62 canonical docs audit: PASS')\n'''
write(ROOT / 'tools/test_alpha62_docs.py', docs_audit)

print('Applied Frontier Settlement alpha.62 remote sentry physical armament.')
