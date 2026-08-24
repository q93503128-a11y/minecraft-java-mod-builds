#!/usr/bin/env python3
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'

def read(p): return p.read_text(encoding='utf-8')
def write(p,s): p.parent.mkdir(parents=True,exist_ok=True); p.write_text(s,encoding='utf-8')
def repl(p,old,new):
    s=read(p); c=s.count(old)
    if c!=1: raise SystemExit(f'{p}: expected one anchor, found {c}: {old[:120]!r}')
    write(p,s.replace(old,new,1))

write(JAVA/'settlement/SettlementMilitaryArmoryService.java','''package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.content.FrontierSoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Automated loaded-only barracks armament using one real external weapon ItemStack per soldier.
 * Soldiers walk to a concrete shared-storage container themselves; no weapon is minted, teleported,
 * force-loaded or represented by a virtual armory balance.
 */
public final class SettlementMilitaryArmoryService {
    public static final double STORAGE_INTERACTION_RANGE_SQR = 9.0D;
    public static final double MAX_ARMORY_ROUTE_SQR = 160.0D * 160.0D;
    public static final double ARMORY_WALK_SPEED = 0.95D;

    private SettlementMilitaryArmoryService() {}

    /**
     * @return true while this soldier is actively handling an armament trip this tick.
     */
    public static boolean tickArmament(ServerLevel level, SettlementData data, FrontierSoldierEntity soldier) {
        if (soldier == null || !soldier.isAlive()) return false;
        ItemStack carried = soldier.getMainHandItem();
        if (SettlementExternalContentService.isExternalWeapon(carried)) return false;
        if (!carried.isEmpty()) return false;
        if (!SettlementStorageService.storageAvailable(level, data)) return false;

        BlockPos source = nearestWeaponSource(level, data, soldier);
        if (source == null) return false;
        double distance = soldier.distanceToSqr(source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D);
        if (distance > STORAGE_INTERACTION_RANGE_SQR) {
            soldier.getNavigation().moveTo(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D, ARMORY_WALK_SPEED);
            return true;
        }

        ItemStack extracted = SettlementStorageService.extract(
                level, source, SettlementExternalContentService::isExternalWeapon, 1);
        if (extracted.isEmpty()) return false;
        soldier.setItemSlot(EquipmentSlot.MAINHAND, extracted);
        soldier.getNavigation().stop();
        return true;
    }

    private static BlockPos nearestWeaponSource(ServerLevel level, SettlementData data, FrontierSoldierEntity soldier) {
        BlockPos best = null;
        double bestDistance = MAX_ARMORY_ROUTE_SQR + 1.0D;
        for (BlockPos pos : SettlementStorageService.storagePositions(data)) {
            if (!level.hasChunkAt(pos)) continue;
            if (!(level.getBlockEntity(pos) instanceof Container container) || !containsExternalWeapon(container)) continue;
            double distance = soldier.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            if (distance <= MAX_ARMORY_ROUTE_SQR && distance < bestDistance) {
                bestDistance = distance;
                best = pos;
            }
        }
        return best;
    }

    private static boolean containsExternalWeapon(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (SettlementExternalContentService.isExternalWeapon(container.getItem(slot))) return true;
        }
        return false;
    }
}
''')

entity=JAVA/'content/FrontierSoldierEntity.java'
repl(entity,
''' * owning a distinct entity type so the client can present it as a human soldier. It deliberately
 * carries no server-side weapon ItemStack; visible equipment is presentation-only in the renderer.
''',
''' * owning a distinct entity type so the client can present it as a human soldier. Alpha.48 deliberately
 * carries no server-side weapon ItemStack by default; Alpha.57 may assign one exact external weapon
 * from loaded shared storage to MAINHAND. Vanilla Mob equipment persistence/sync owns that ItemStack.
''')

renderer=JAVA/'client/FrontierSoldierRenderer.java'
repl(renderer,
''' * The visible iron sword exists only as a render-state ItemStack. It is never inserted into the
 * server entity, settlement storage or loot tables, so Alpha.48 cannot mint an economic weapon or
 * alter the inherited Iron Golem combat attributes. Companion weapon visuals can be revisited later
 * only through an explicit physical armory contract.
''',
''' * Alpha.48's iron service sword remains a client-only fallback for an un-upgraded soldier. Alpha.57
 * renders the entity's real synced MAINHAND ItemStack when the automated barracks armory has physically
 * assigned one. The renderer itself never creates or inserts economic equipment.
''')
repl(renderer,
'''        // Presentation-only service sword. Never call entity.setItemSlot: server/economy state stays empty.
        state.rightHandItemStack = VISUAL_SERVICE_SWORD;
        state.rightArmPose = HumanoidModel.ArmPose.ITEM;
        this.itemModelResolver.updateForLiving(
                state.rightHandItemState,
                VISUAL_SERVICE_SWORD,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                entity);
''',
'''        // Renderer rule: Never call entity.setItemSlot here; the server armory owns real equipment.
        ItemStack physicalWeapon = entity.getMainHandItem();
        if (physicalWeapon.isEmpty()) {
            state.rightHandItemStack = VISUAL_SERVICE_SWORD;
            state.rightArmPose = HumanoidModel.ArmPose.ITEM;
            this.itemModelResolver.updateForLiving(
                    state.rightHandItemState,
                    VISUAL_SERVICE_SWORD,
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    entity);
        } else {
            state.rightHandItemStack = physicalWeapon;
            state.rightArmPose = HumanoidModel.ArmPose.ITEM;
            this.itemModelResolver.updateForLiving(
                    state.rightHandItemState,
                    physicalWeapon,
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    entity);
        }
''')

barracks=JAVA/'settlement/SettlementBarracksService.java'
repl(barracks,
'''import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
''',
'''import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
''')
repl(barracks,'private static final double SOLDIER_SEARCH_RADIUS = 40.0D;','private static final double SOLDIER_SEARCH_RADIUS = 176.0D;')
repl(barracks,
'''                FrontierSoldierEntity soldier = findSoldier(level, barracks, slot);
                if (soldier != null) patrol(level, barracks, slot, soldier);
''',
'''                FrontierSoldierEntity soldier = findSoldier(level, barracks, slot);
                if (soldier != null) patrol(level, data, barracks, slot, soldier);
''')
repl(barracks,
'''    public static boolean militaryStateLoaded(ServerLevel level, SettlementData data) {
''',
'''    public static int loadedArmedSoldierCount(ServerLevel level, SettlementData data) {
        int count = 0;
        for (BuildingRecord barracks : barracks(data)) {
            if (!patrolAreaLoaded(level, barracks)) continue;
            for (int slot = 0; slot < SOLDIERS_PER_BARRACKS; slot++) {
                FrontierSoldierEntity soldier = findSoldier(level, barracks, slot);
                if (soldier != null && SettlementExternalContentService.isExternalWeapon(soldier.getMainHandItem())) count++;
            }
        }
        return count;
    }

    public static boolean militaryStateLoaded(ServerLevel level, SettlementData data) {
''')
repl(barracks,
'''    /** Supplied soldiers are combat/service units and never an item/iron farm. */
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().entityTags().contains(SOLDIER_TAG)) event.getDrops().clear();
    }
''',
'''    /** Supplied soldiers never become an iron/body-drop farm; one physically assigned weapon is recoverable. */
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!event.getEntity().entityTags().contains(SOLDIER_TAG)) return;
        ItemStack weapon = event.getEntity().getMainHandItem();
        event.getDrops().clear();
        if (!SettlementExternalContentService.isExternalWeapon(weapon)) return;
        ItemStack recovered = weapon.copy();
        event.getDrops().add(new ItemEntity(
                event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), recovered));
    }
''')
repl(barracks,
'''    private static void patrol(ServerLevel level, BuildingRecord barracks, int slot, FrontierSoldierEntity soldier) {
        BlockPos home = soldierHome(barracks, slot);
        double homeDistance = soldier.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
        if (homeDistance > PATROL_LEASH_RADIUS_SQR) {
            if (soldier.getTarget() != null) soldier.setTarget(null);
            soldier.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 0.95D);
            return;
        }
        Monster threat = nearestThreat(level, barracks.workCenter());
        if (threat != null) { soldier.setTarget(threat); return; }
        if (soldier.getTarget() != null) soldier.setTarget(null);
        if (homeDistance > HOME_RADIUS_SQR) {
            soldier.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 0.9D);
        }
    }
''',
'''    private static void patrol(ServerLevel level, SettlementData data, BuildingRecord barracks, int slot, FrontierSoldierEntity soldier) {
        BlockPos home = soldierHome(barracks, slot);
        Monster threat = nearestThreat(level, barracks.workCenter());
        if (threat != null) { soldier.setTarget(threat); return; }
        if (soldier.getTarget() != null) soldier.setTarget(null);

        // Defense always wins. Only an idle, loaded garrison walks to real shared storage for one real weapon.
        if (SettlementMilitaryArmoryService.tickArmament(level, data, soldier)) return;

        double homeDistance = soldier.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
        if (homeDistance > PATROL_LEASH_RADIUS_SQR) {
            soldier.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 0.95D);
            return;
        }
        if (homeDistance > HOME_RADIUS_SQR) {
            soldier.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 0.9D);
        }
    }
''')

commands=JAVA/'command/SettlementCommands.java'
repl(commands,
'''        if(SettlementBarracksService.militaryStateLoaded(server.overworld(),data)) player.sendSystemMessage(Component.literal("군사 | 주둔병 "+SettlementBarracksService.loadedSoldierCount(server.overworld(),data)+" / "+SettlementBarracksService.militaryCapacity(data)+" | 충원비 1명당 식량 "+SettlementBarracksService.RECRUIT_FOOD_COST+" 금속 "+SettlementBarracksService.RECRUIT_METAL_COST));
''',
'''        if(SettlementBarracksService.militaryStateLoaded(server.overworld(),data)) player.sendSystemMessage(Component.literal("군사 | 주둔병 "+SettlementBarracksService.loadedSoldierCount(server.overworld(),data)+" / "+SettlementBarracksService.militaryCapacity(data)+" | 실물 외부무기 무장 "+SettlementBarracksService.loadedArmedSoldierCount(server.overworld(),data)+" | 충원비 1명당 식량 "+SettlementBarracksService.RECRUIT_FOOD_COST+" 금속 "+SettlementBarracksService.RECRUIT_METAL_COST));
''')

props=ROOT/'gradle.properties'
repl(props,'mod_version=0.1.0-alpha.56','mod_version=0.1.0-alpha.57')
repl(props,'plus soft common-biome-tag evidence that improves outpost specialization without hard worldgen dependencies.','plus soft common-biome-tag evidence that improves outpost specialization without hard worldgen dependencies, and automated physical barracks armament from real external-weapon ItemStacks without per-soldier micromanagement.')

lock=ROOT/'COMPANION_LOCK.json'
repl(lock,'"frontier_settlement": "0.1.0-alpha.56"','"frontier_settlement": "0.1.0-alpha.57"')
repl(lock,
'    "Alpha.56 reads only NeoForge common biome tags from the already-loaded outpost center and adds bounded evidence bias to the existing physical specialization survey: forest/dense vegetation helps lumber, plains/savanna helps agriculture, mountain/hill helps quarry/mining, and badlands/sandy terrain slightly helps quarry; no Terralith class/id hard dependency or biome-generated resource minting is introduced.",\n',
'    "Alpha.56 reads only NeoForge common biome tags from the already-loaded outpost center and adds bounded evidence bias to the existing physical specialization survey: forest/dense vegetation helps lumber, plains/savanna helps agriculture, mountain/hill helps quarry/mining, and badlands/sandy terrain slightly helps quarry; no Terralith class/id hard dependency or biome-generated resource minting is introduced.",\n    "Alpha.57 upgrades only loaded town barracks soldiers: an idle soldier with an empty MAINHAND walks to the nearest loaded shared-storage container, extracts exactly one real Frontier-recognized external weapon ItemStack, equips it through vanilla Mob equipment persistence/sync, and returns that exact weapon as the sole recoverable military drop on death; remote military-outpost weapon supply remains deferred to the existing road transporter authority.",\n')
repl(lock,'so Alpha.56 keeps only HUD collision avoidance','so Alpha.57 keeps only HUD collision avoidance')

readme=ROOT/'README.md'
repl(readme,'## Current version: 0.1.0-alpha.56','## Current version: 0.1.0-alpha.57')
repl(readme,'No new Alpha.56 key was added.','No new Alpha.57 key was added.')
repl(readme,'Alpha.40–56 deepen existing systems','Alpha.40–57 deepen existing systems')
section='''## Alpha.57 — automated physical barracks armament

Alpha.57 closes the first physical military armory/loadout slice without adding soldier-by-soldier menus or a 16th building.

- only loaded **town barracks** soldiers participate in this first slice; dangerous-region remote sentries remain unchanged until weapons can travel through the existing road transporter authority;
- an idle barracks soldier with an empty MAINHAND checks real shared settlement storage only when the ordinary storage authority is fully loaded;
- if a Frontier-recognized external weapon exists, the soldier walks to the **nearest concrete storage container within 160 blocks**; no teleport, force-load, virtual armory inventory or instant remote transfer;
- after reaching normal 3-block interaction range, the soldier extracts **exactly one real external weapon ItemStack** and equips it in vanilla `EquipmentSlot.MAINHAND`; damage/enchantments/components stay on that exact stack and vanilla Mob equipment persistence/sync owns save/reload/client state;
- hostile defense has priority over an armament trip, so soldiers do not abandon an active barracks threat to fetch gear;
- the humanoid renderer shows the real synced MAINHAND weapon when present and keeps Alpha.48's client-only iron service sword only as the un-upgraded fallback;
- soldier death still clears ordinary body/iron drops, but if a real external weapon was assigned, exactly that one stack is re-added as the sole recoverable military drop. The source stack previously left settlement storage, so this is recovery rather than minting;
- `/frontier status` adds only the loaded physically armed garrison count; no new screen/key/manual assignment list;
- `SettlementExternalContentService.isExternalWeapon` remains the soft registry recognizer, so no Weapons Expanded Java class or Better Combat class becomes a hard dependency;
- remote sentry weapon supply is **not** faked. When/if implemented, **군사 전초도 같은 도로 운송자가 역방향 보급** and **위험지역 군사 역할이 우선** must still hold;
- `Transport workers belong to a specific outpost`, `pause at unloaded route boundaries`, Alpha.27 remains the **single authority for outpost transport**, and **there is still only one authority for long-distance outpost transport**.

This is automated physical loadout rather than per-soldier micromanagement: players only put useful weapons into the same shared storage they already use.

'''
repl(readme,'## Alpha.56 — soft biome-aware outpost specialization\n',section+'## Alpha.56 — soft biome-aware outpost specialization\n')

can=ROOT/'CANONICAL_PLAN.md'
repl(can,'Current canonical implementation: **0.1.0-alpha.56**.','Current canonical implementation: **0.1.0-alpha.57**.')
repl(can,'Alpha.40–56 deepen systems','Alpha.40–57 deepen systems')
section2='''### Alpha.57 automated physical barracks armory

Alpha.57 deliberately uses the existing barracks + shared-storage authorities. It does not create a new armory BuildingType, equipment currency, soldier-management UI or remote logistics path.

- loaded town barracks soldiers only in the first slice;
- no active barracks threat -> unarmed soldier may seek equipment; defense always has priority;
- shared settlement storage must be fully loaded and must physically contain a recognized external weapon;
- soldier itself walks to the nearest concrete weapon-containing storage within160 blocks and only extracts at <=3-block interaction range;
- extraction count is exactly1; no copy/mint/free fallback server ItemStack;
- actual weapon becomes the soldier's vanilla MAINHAND equipment, preserving the original ItemStack components/damage/enchantments through vanilla entity persistence/sync;
- client renderer shows that synced physical weapon; Alpha.48 service sword remains only the client fallback when no physical upgrade exists;
- barracks death drops are still cleared except the exact assigned external weapon, which is re-added once for recovery;
- loaded armed count is compact status only;
- remote military sentries remain generic until actual weapon cargo can reuse the same road-bound transporter. **군사 전초도 같은 도로 운송자가 역방향 보급** remains the only acceptable remote extension;
- no hard Weapons Expanded/Better Combat class dependency, force-load, teleport, new worker or second logistics authority;
- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, Alpha.27 stays the **single authority for outpost transport**, and **there is still only one authority for long-distance outpost transport**.

'''
repl(can,'### Alpha.56 common-biome-tag outpost specialization\n',section2+'### Alpha.56 common-biome-tag outpost specialization\n')
repl(can,'## 14. Current playable slice after Alpha.56','## 14. Current playable slice after Alpha.57')
repl(can,'- supplied humanoid military presentation with unchanged physical recruitment economics;','- supplied humanoid military presentation with unchanged physical recruitment economics;\n- Alpha.57 loaded town-garrison physical external-weapon armament from shared storage, with exact weapon recovery;')
repl(can,'## 15. Unfinished original-scope priorities after Alpha.56','## 15. Unfinished original-scope priorities after Alpha.57')
repl(can,
'''1. physical military armory/loadout only if it can stay automated and ItemStack-authoritative without per-soldier micromanagement;
2. long survival + two-player multiplayer acceptance;
3. rare-NPC-specific settlement value only if a stable soft data seam appears; generic biome-aware specialization is covered by Alpha.56;
4. optional deeper monumental crossings only if real play shows Alpha.52–54 breadth is insufficient; never expand by default into WorldEdit-scale civil works;''',
'''1. long survival + two-player multiplayer acceptance;
2. remote military external-weapon supply only if the actual weapon ItemStack can ride the existing road-bound reverse-supply transporter; town barracks physical armament is covered by Alpha.57;
3. rare-NPC-specific settlement value only if a stable soft data seam appears; generic biome-aware specialization is covered by Alpha.56;
4. optional deeper monumental crossings only if real play shows Alpha.52–54 breadth is insufficient; never expand by default into WorldEdit-scale civil works;''')
repl(can,'14. Alpha.56 common-biome-tag borderline specialization / companion-installed-and-absent acceptance;','14. Alpha.56 common-biome-tag borderline specialization / companion-installed-and-absent acceptance;\n15. Alpha.57 weapon storage→soldier walk/extract/save-reload/render/death-recovery/no-dup acceptance;')
repl(can,'15. full companion lock fresh-world client/server runtime;\n16. true Xaero markers only if a stable supported API appears;\n17. moving boat/waterborne merchant only if presentation value justifies it and it never becomes a second logistics authority.','16. full companion lock fresh-world client/server runtime;\n17. true Xaero markers only if a stable supported API appears;\n18. moving boat/waterborne merchant only if presentation value justifies it and it never becomes a second logistics authority.')

gap=ROOT/'COMPLETION_GAP_AUDIT.md'
repl(gap,'현재 구현 기준: `0.1.0-alpha.56`','현재 구현 기준: `0.1.0-alpha.57`')
repl(gap,
'이 문서는 현재 구현에 맞춰 원본 v0.2 범위를 축소하지 않는다. Alpha.55에서 비농사형 탐험 지식이 기존 전초 운영에 실제 효과를 주어도 실물 군사 armory, companion-biome/NPC 특화 breadth, 장시간 multiplayer 및 full companion runtime이 남아 있는 동안 완성이라고 부르지 않는다.',
'이 문서는 현재 구현에 맞춰 원본 v0.2 범위를 축소하지 않는다. Alpha.57에서 본진 병영 실물 외부무기 armament까지 들어가도 원격 군사 무기 보급, rare-NPC breadth, 장시간 multiplayer 및 full companion runtime이 남아 있는 동안 완성이라고 부르지 않는다.')
repl(gap,
'| 군사 전초 충원 실제 자원 | 완료/부분 | local food6 + metal2 + reverse supply |',
'| 군사 전초 충원 실제 자원 | 완료/부분 | local food6 + metal2 + reverse supply |\n| 본진 병영 실물 외부무기 armory/loadout | **완료/부분** | Alpha.57 shared storage→soldier physical walk→exact MAINHAND ItemStack; remote sentry weapon supply는 미구현/부분 |')
section3='''### Alpha.57 본진 병영 실물 무장 감사

- 새 BuildingType/armory UI/장비 재화/개별 병사 메뉴 없음;
- loaded town barracks soldier만 1차 대상, remote military sentry는 의도적으로 제외;
- shared storage 전체 loaded + 실제 recognized external weapon 존재가 전제;
- idle/unarmed soldier가 nearest concrete weapon storage까지 최대160블록 물리 이동;
- 3블록 interaction range 도달 뒤 exact external weapon 1개만 실제 extraction;
- vanilla MAINHAND ItemStack으로 장착되어 damage/enchantment/components + entity save/sync 유지;
- active barracks threat가 armory trip보다 우선;
- renderer는 physical MAINHAND가 있으면 실제 무기를 표시하고 없을 때만 Alpha.48 client service sword fallback;
- death에서 일반 soldier/body drops는 계속 제거하되 실제 장착 external weapon 1개만 recovery drop으로 복원;
- weapon은 사전에 shared storage에서 제거된 동일 stack이므로 free mint/duplication 아님;
- no hard Weapons Expanded/Better Combat class dependency, force-load, teleport, second worker/economy/logistics authority 없음;
- remote weapon supply는 **군사 전초도 같은 도로 운송자가 역방향 보급**할 수 있을 때만 후속 허용;
- **위험지역 군사 역할이 우선**, `single authority for outpost transport`, `there is still only one authority for long-distance outpost transport` 유지;
- **Transport workers belong to a specific outpost** / **pause at unloaded route boundaries** 유지.

따라서 physical military armory/loadout은 **본진 병영 기준 완료/부분**으로 전진했다. 원격 수비대 무기 ItemStack 역보급은 별도 남은 범위다.

'''
repl(gap,'## 7. 도로 / 전초 / 영토\n',section3+'## 7. 도로 / 전초 / 영토\n')
repl(gap,
'''1. per-soldier micromanagement 없이 가능한 physical military armory/loadout;
2. long survival + two-player multiplayer acceptance;
3. rare-NPC-specific settlement value는 stable soft seam이 실제 확인될 때만; generic biome-aware specialization은 Alpha.56에서 1차 완료/부분;
4. optional deeper monumental crossing은 Alpha.52–54 실플레이에서 실제 부족이 확인될 때만;''',
'''1. long survival + two-player multiplayer acceptance;
2. remote military external-weapon supply는 기존 road-bound reverse-supply transporter가 실제 ItemStack을 운반할 수 있을 때만; town barracks armory는 Alpha.57 완료/부분;
3. rare-NPC-specific settlement value는 stable soft seam이 실제 확인될 때만; generic biome-aware specialization은 Alpha.56에서 1차 완료/부분;
4. optional deeper monumental crossing은 Alpha.52–54 실플레이에서 실제 부족이 확인될 때만;''')
repl(gap,'13. Alpha.56 common-biome-tag borderline specialization + companion installed/absent acceptance;','13. Alpha.56 common-biome-tag borderline specialization + companion installed/absent acceptance;\n14. Alpha.57 shared-storage weapon walk/extract/persistence/render/death-recovery/no-dup acceptance;')
repl(gap,'14. full companion lock fresh-world client/server runtime;\n15. true Xaero marker는 stable supported API가 생길 때만;\n16. moving boat/waterborne merchant는 두 번째 logistics authority가 되지 않는 경우에만 선택적 presentation.','15. full companion lock fresh-world client/server runtime;\n16. true Xaero marker는 stable supported API가 생길 때만;\n17. moving boat/waterborne merchant는 두 번째 logistics authority가 되지 않는 경우에만 선택적 presentation.')

write(ROOT/'tools/test_alpha57_source.py','''#!/usr/bin/env python3
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
forbid(military,('SettlementMilitaryArmoryService','setItemSlot(EquipmentSlot.MAINHAND'),'alpha.57 must not fake remote sentry armory')
must(commands,('실물 외부무기 무장 "+SettlementBarracksService.loadedArmedSoldierCount'),'alpha.57 compact status')
# Exact 15 functional families remain unchanged.
enum_block=building.split('public enum BuildingType {',1)[1].split(';',1)[0]
actual=[line.strip().split('(',1)[0] for line in enum_block.splitlines() if '(' in line]
expected=['HOUSE','LUMBER_CAMP','FARM','QUARRY','MINE','WAREHOUSE','CONSTRUCTION_OFFICE','BLACKSMITH','WORKSHOP','ADVANCED_WORKSHOP','GUARD_POST','WATCHTOWER','BARRACKS','MARKET','CART_STATION']
if actual != expected: raise SystemExit(f'alpha.57 expected exact 15 functional building families, got: {actual}')
must(props,('mod_version=0.1.0-alpha.57','automated physical barracks armament','without per-soldier micromanagement'),'alpha.57 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.57"','Alpha.57 upgrades only loaded town barracks soldiers','extracts exactly one real Frontier-recognized external weapon ItemStack','remote military-outpost weapon supply remains deferred','"status": "candidate_runtime_lock"'),'alpha.57 lock')
print('Frontier Settlement alpha.23-57 cumulative source audit: PASS')
''')

write(ROOT/'tools/test_alpha57_docs.py','''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
readme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md')
must(readme,('## Current version: 0.1.0-alpha.57','## Alpha.57 — automated physical barracks armament','nearest concrete storage container within 160 blocks','exactly one real external weapon ItemStack','EquipmentSlot.MAINHAND','sole recoverable military drop','군사 전초도 같은 도로 운송자가 역방향 보급','there is still only one authority for long-distance outpost transport'),'alpha.57 README')
must(can,('Current canonical implementation: **0.1.0-alpha.57**','### Alpha.57 automated physical barracks armory','shared settlement storage must be fully loaded','actual weapon becomes the soldier\'s vanilla MAINHAND equipment','remote military sentries remain generic','## 15. Unfinished original-scope priorities after Alpha.57','long survival + two-player multiplayer acceptance'),'alpha.57 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.57`','### Alpha.57 본진 병영 실물 무장 감사','본진 병영 실물 외부무기 armory/loadout | **완료/부분**','remote weapon supply는 **군사 전초도 같은 도로 운송자가 역방향 보급**','physical military armory/loadout은 **본진 병영 기준 완료/부분**','## 11. 완료 판정 금지선'),'alpha.57 gap')
print('Frontier Settlement alpha.57 canonical docs audit: PASS')
''')

print('Applied Frontier Settlement 0.1.0-alpha.57 automated physical barracks armament.')
