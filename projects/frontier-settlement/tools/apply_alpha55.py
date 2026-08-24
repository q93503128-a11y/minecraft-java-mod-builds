#!/usr/bin/env python3
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'

def read(p): return p.read_text(encoding='utf-8')
def write(p,s): p.parent.mkdir(parents=True,exist_ok=True); p.write_text(s,encoding='utf-8')
def repl(p,old,new):
    s=read(p); c=s.count(old)
    if c!=1: raise SystemExit(f'{p}: expected one anchor, found {c}: {old[:100]!r}')
    write(p,s.replace(old,new,1))

# Pure computed bridge: no tick, inventory or separate saved authority.
write(JAVA/'settlement/SettlementExplorationBenefitService.java','''package kr.moonseungjun.frontiersettlement.settlement;

/**
 * Small, deterministic settlement benefits derived only from Alpha.45 first-time milestones.
 * No new currency, inventory, reward chest, world scan or saved authority is introduced.
 */
public final class SettlementExplorationBenefitService {
    public static final int MAX_SURVEY_LEVEL = 3;
    public static final int MAX_CONQUEST_LEVEL = 2;
    public static final long OUTPOST_WOOD_DISCOUNT_PER_CONQUEST = 4L;
    public static final long OUTPOST_STONE_DISCOUNT_PER_CONQUEST = 2L;

    private SettlementExplorationBenefitService() {}

    public static int surveyLevel(SettlementData data) {
        return Math.min(MAX_SURVEY_LEVEL, data.discoveredExternalStructures().size());
    }

    public static int conquestLevel(SettlementData data) {
        return Math.min(MAX_CONQUEST_LEVEL, data.defeatedExternalBosses().size());
    }

    public static long outpostWoodCost(SettlementData data) {
        return SettlementOutpostService.WOOD_COST - conquestLevel(data) * OUTPOST_WOOD_DISCOUNT_PER_CONQUEST;
    }

    public static long outpostStoneCost(SettlementData data) {
        return SettlementOutpostService.STONE_COST - conquestLevel(data) * OUTPOST_STONE_DISCOUNT_PER_CONQUEST;
    }

    public static int oreEvidenceBonus(SettlementData data) {
        return surveyLevel(data) >= 2 ? 1 : 0;
    }

    public static int logEvidenceBonus(SettlementData data) {
        return surveyLevel(data) * 2;
    }

    public static int fieldEvidenceBonus(SettlementData data) {
        return surveyLevel(data) * 8;
    }

    public static int stoneEvidenceBonus(SettlementData data) {
        return surveyLevel(data) * 2;
    }
}
''')

out=JAVA/'settlement/SettlementOutpostService.java'
repl(out,
'''        BlockPos center = outpostCenter(gate, road.directionX(), road.directionZ());
        String specialization = detectSpecialization(level, center);
        SettlementService.refreshResources(server, data);
        if (data.resources().wood() < WOOD_COST || data.resources().stone() < STONE_COST) {
            return new PlacementCheck(false, roadIndex, gate, road.directionX(), road.directionZ(), specialization,
                    "전초기지 필요 자원: 목재 " + WOOD_COST + " · 석재 " + STONE_COST);
        }''',
'''        BlockPos center = outpostCenter(gate, road.directionX(), road.directionZ());
        String specialization = detectSpecialization(level, center, data);
        long woodCost = SettlementExplorationBenefitService.outpostWoodCost(data);
        long stoneCost = SettlementExplorationBenefitService.outpostStoneCost(data);
        SettlementService.refreshResources(server, data);
        if (data.resources().wood() < woodCost || data.resources().stone() < stoneCost) {
            return new PlacementCheck(false, roadIndex, gate, road.directionX(), road.directionZ(), specialization,
                    "전초기지 필요 자원: 목재 " + woodCost + " · 석재 " + stoneCost);
        }''')
repl(out,
'''        SettlementService.refreshResources(server, data);
        if (data.resources().wood() < WOOD_COST || data.resources().stone() < STONE_COST) {
            return new StartResult(false, "전초기지 필요 자원: 목재 " + WOOD_COST + " · 석재 " + STONE_COST);
        }

        data.beginOutpostConstruction''',
'''        long woodCost = SettlementExplorationBenefitService.outpostWoodCost(data);
        long stoneCost = SettlementExplorationBenefitService.outpostStoneCost(data);
        SettlementService.refreshResources(server, data);
        if (data.resources().wood() < woodCost || data.resources().stone() < stoneCost) {
            return new StartResult(false, "전초기지 필요 자원: 목재 " + woodCost + " · 석재 " + stoneCost);
        }

        data.beginOutpostConstruction''')
repl(out,
'''        return new StartResult(true, "전초기지 착공. 건설 주민이 부지를 정리한 뒤 실제 목재·석재를 운반하며 시공합니다.");''',
'''        return new StartResult(true, "전초기지 착공. 건설 주민이 부지를 정리한 뒤 실제 목재·석재를 운반하며 시공합니다."
                + " (탐험 정복 반영 비용: 목재 " + woodCost + " · 석재 " + stoneCost + ")");''')
repl(out,
'''            requiredNow = materialCostDelta(plan, step, true, WOOD_COST);
            remainingCost = materialRemainingCost(plan, step, true, WOOD_COST);''',
'''            long totalWoodCost = SettlementExplorationBenefitService.outpostWoodCost(data);
            requiredNow = materialCostDelta(plan, step, true, totalWoodCost);
            remainingCost = materialRemainingCost(plan, step, true, totalWoodCost);''')
repl(out,
'''            requiredNow = materialCostDelta(plan, step, false, STONE_COST);
            remainingCost = materialRemainingCost(plan, step, false, STONE_COST);''',
'''            long totalStoneCost = SettlementExplorationBenefitService.outpostStoneCost(data);
            requiredNow = materialCostDelta(plan, step, false, totalStoneCost);
            remainingCost = materialRemainingCost(plan, step, false, totalStoneCost);''')
repl(out,
'''        String specialization = detectSpecialization(level, center);
        OutpostRecord outpost = new OutpostRecord(''',
'''        String specialization = detectSpecialization(level, center, data);
        OutpostRecord outpost = new OutpostRecord(''')
repl(out,
'''    private static String detectSpecialization(ServerLevel level, BlockPos center) {
        int ores = 0;''',
'''    private static String detectSpecialization(ServerLevel level, BlockPos center) {
        return detectSpecialization(level, center, null);
    }

    private static String detectSpecialization(ServerLevel level, BlockPos center, SettlementData data) {
        int ores = 0;''')
repl(out,
'''        if (ores >= 4) return "mining";
        if (logs >= 24) return "lumber";
        if (fieldGround >= 120) return "agriculture";
        if (exposedStone >= 24) return "quarry";''',
'''        if (data != null) {
            ores += SettlementExplorationBenefitService.oreEvidenceBonus(data);
            logs += SettlementExplorationBenefitService.logEvidenceBonus(data);
            fieldGround += SettlementExplorationBenefitService.fieldEvidenceBonus(data);
            exposedStone += SettlementExplorationBenefitService.stoneEvidenceBonus(data);
        }
        if (ores >= 4) return "mining";
        if (logs >= 24) return "lumber";
        if (fieldGround >= 120) return "agriculture";
        if (exposedStone >= 24) return "quarry";''')

# Compact status: visible benefit without another dashboard/key.
cmd=JAVA/'command/SettlementCommands.java'
anchor='player.sendSystemMessage(Component.literal("개척 진척 | 외부 구조물 "+data.discoveredExternalStructures().size()+"종 | 정복 강적 "+data.defeatedExternalBosses().size()+"종 | 진척 "+data.explorationScore()+" / 8 | 동일 종류 반복은 중복 없음"));'
repl(cmd,anchor,anchor+'\n        player.sendSystemMessage(Component.literal("개척 지식 | 정찰 "+SettlementExplorationBenefitService.surveyLevel(data)+" / 3 | 정복 "+SettlementExplorationBenefitService.conquestLevel(data)+" / 2 | 새 전초 실물 비용 목재 "+SettlementExplorationBenefitService.outpostWoodCost(data)+" · 석재 "+SettlementExplorationBenefitService.outpostStoneCost(data)+" | 반복 발견/정복 보너스 없음"));')

# Version/lock.
props=ROOT/'gradle.properties'
repl(props,'mod_version=0.1.0-alpha.54','mod_version=0.1.0-alpha.55')
repl(props,'bounded one-bend tunnel public works with physically hauled stone-brick portals.','bounded one-bend tunnel public works with physically hauled stone-brick portals, and capped non-farmable exploration knowledge that improves existing outpost surveying and physical construction efficiency.')
lock=ROOT/'COMPANION_LOCK.json'
repl(lock,'"frontier_settlement": "0.1.0-alpha.54"','"frontier_settlement": "0.1.0-alpha.55"')
repl(lock,
'    "Alpha.54 keeps the same 24-cell tunnel ceiling but permits one bounded 90-degree bend with at least three tunnel centers on each leg; two deterministic 5-wide by 4-high STONE_BRICKS portal frames are excavated safely and then built from the same physically hauled road-stone authority, with no new logistics or companion dependency.",\n',
'    "Alpha.54 keeps the same 24-cell tunnel ceiling but permits one bounded 90-degree bend with at least three tunnel centers on each leg; two deterministic 5-wide by 4-high STONE_BRICKS portal frames are excavated safely and then built from the same physically hauled road-stone authority, with no new logistics or companion dependency.",\n    "Alpha.55 derives a capped survey level from unique external structure discoveries and a capped conquest level from unique boss types: survey knowledge only biases the existing local outpost-specialization evidence, while conquest reduces new outpost physical material totals by at most 8 wood and 4 stone; repeated IDs remain non-farmable and no new currency, loot minting, save authority or logistics controller is created.",\n')
repl(lock,'so Alpha.54 keeps only HUD collision avoidance','so Alpha.55 keeps only HUD collision avoidance')

# README.
readme=ROOT/'README.md'
repl(readme,'## Current version: 0.1.0-alpha.54','## Current version: 0.1.0-alpha.55')
repl(readme,'No new Alpha.54 key was added.','No new Alpha.55 key was added.')
repl(readme,'Alpha.40–54 deepen existing systems','Alpha.40–55 deepen existing systems')
section='''## Alpha.55 — non-farmable exploration knowledge feeds existing outposts

Alpha.55 makes the already-persisted Alpha.45 discovery/conquest milestones matter after tier acceleration without creating another progression tree.

- unique external structure IDs yield a capped **survey level 0–3**; repeated copies of the same structure type still add nothing;
- survey knowledge does not create ores/logs/food. It only adds a small bounded evidence bias to the existing loaded 12-block outpost-specialization survey, making mining/lumber/agriculture/quarry roles slightly easier to recognize after real exploration;
- unique conquest target IDs yield a capped **conquest level 0–2**; repeated kills of the same boss type add nothing;
- each conquest level reduces a **new** outpost's physical construction total by only 4 wood + 2 stone, capped at 8 wood + 4 stone; the builder still walks from real loaded settlement storage and consumes actual ItemStacks through the existing outpost construction authority;
- base outpost costs remain 72 wood + 48 stone and the minimum Alpha.55 explored cost is 64 wood + 44 stone;
- the benefit is computed directly from existing unique milestone lists, so old saves gain the correct value automatically and there is no new saved currency/claim flag;
- no structure locator, force-load, companion class reference, generated reward chest, free item, population grant, second economy or second logistics authority is added;
- compact `/frontier status` shows survey/conquest levels and the current physical new-outpost cost; no new dashboard or key is introduced;
- `Transport workers belong to a specific outpost`, `pause at unloaded route boundaries`, and Alpha.27 remains the **single authority for outpost transport**; **there is still only one authority for long-distance outpost transport**.

This is intentionally a small settlement-value bridge, not a generic RPG skill tree. Companion-specific biome/NPC seams remain a later optional depth pass only where a stable API/data seam exists.

'''
repl(readme,'## Alpha.54 — bounded one-bend tunnels and physical portals\n',section+'## Alpha.54 — bounded one-bend tunnels and physical portals\n')

# Canonical.
can=ROOT/'CANONICAL_PLAN.md'
repl(can,'Current canonical implementation: **0.1.0-alpha.54**.','Current canonical implementation: **0.1.0-alpha.55**.')
repl(can,'Alpha.40–54 deepen systems','Alpha.40–55 deepen systems')
sec='''### Alpha.55 exploration knowledge -> existing outpost value

Alpha.55 extends Alpha.45 without creating a second progression or reward authority.

- `surveyLevel = min(3, unique external structure types)`;
- `conquestLevel = min(2, unique defeated conquest target types)`;
- survey level only biases the existing loaded local specialization evidence: ore +0/0/1/1, logs +2/level, field ground +8/level, exposed stone +2/level; it never spawns or credits resources;
- conquest level reduces only the physical material total for newly built outposts: wood `72 - 4*level`, stone `48 - 2*level`, minimum64/44;
- those effective totals are used by placement approval and by the existing builder's actual ItemStack extraction/consumption math, so the discount cannot become a virtual refund or free construction;
- benefits are deterministic from Alpha.45 persisted unique-ID lists; old saves need no migration field and repeated IDs remain non-farmable;
- exploration observation remains loaded-only and never locates/generates external content;
- no free loot, population, abstract survey currency, new UI tree, second economy or second transport authority;
- **builder walks from actual settlement storage carrying real wood/stone stacks** remains true;
- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, and Alpha.27 is the **single authority for outpost transport**; **there is still only one authority for long-distance outpost transport**.

'''
repl(can,'## 8. Resources and logistics\n',sec+'## 8. Resources and logistics\n')
repl(can,'Alpha.45 exploration score and civil `earthBank` are not spendable resources.','Alpha.45 exploration score, Alpha.55 survey/conquest knowledge and civil `earthBank` are not spendable resources.')
repl(can,'## 14. Current playable slice after Alpha.54','## 14. Current playable slice after Alpha.55')
repl(can,'## 15. Unfinished original-scope priorities after Alpha.54','## 15. Unfinished original-scope priorities after Alpha.55')
repl(can,
'''1. deeper exploration bridges — rare NPC/structure/boss-specific settlement value only where soft, non-farmable and meaningful;
2. better companion-biome-aware outpost specialization where a stable data seam exists;
3. physical military armory/loadout only if it can stay automated and ItemStack-authoritative without per-soldier micromanagement;''',
'''1. better companion-biome-aware / rare-NPC outpost specialization only where a stable soft data seam exists; Alpha.55 already supplies the generic non-farmable exploration-value bridge;
2. physical military armory/loadout only if it can stay automated and ItemStack-authoritative without per-soldier micromanagement;
3. long survival + two-player multiplayer acceptance;''')

# Gap audit.
gap=ROOT/'COMPLETION_GAP_AUDIT.md'
repl(gap,'현재 구현 기준: `0.1.0-alpha.54`','현재 구현 기준: `0.1.0-alpha.55`')
repl(gap,
'이 문서는 현재 구현에 맞춰 원본 v0.2 범위를 축소하지 않는다. Alpha.54에서 bounded 단일굴곡 터널과 실제 석재 포털까지 추가되어도 실물 군사 armory, 일부 탐험/전초 breadth, 장시간 multiplayer 및 full companion runtime이 남아 있는 동안 완성이라고 부르지 않는다.',
'이 문서는 현재 구현에 맞춰 원본 v0.2 범위를 축소하지 않는다. Alpha.55에서 비농사형 탐험 지식이 기존 전초 운영에 실제 효과를 주어도 실물 군사 armory, companion-biome/NPC 특화 breadth, 장시간 multiplayer 및 full companion runtime이 남아 있는 동안 완성이라고 부르지 않는다.')
secgap='''### Alpha.55 탐험 지식 / 전초 가치 감사

- Alpha.45 unique structure/boss persistence 그대로 사용, 새 save field 없음;
- 외부 구조물 unique type 최대3단계 survey knowledge, 동일 ID 반복 0;
- 강적 unique type 최대2단계 conquest knowledge, 동일 ID 반복 0;
- survey는 기존 loaded 12-block 전초 특화 증거에 작은 bounded bias만 추가, 자원 생성/광석 생성 없음;
- conquest는 신규 전초 physical total만 level당 wood4/stone2 절감, max wood8/stone4;
- base72/48, 최저64/44이며 placement 승인과 actual builder ItemStack consume가 같은 effective cost를 사용;
- free loot/refund/population/virtual currency 없음;
- loaded-only exploration observation/companion soft dependency 유지;
- `builder walks from actual settlement storage carrying real wood/stone stacks` 유지;
- `single authority for outpost transport` / `there is still only one authority for long-distance outpost transport` 유지;
- **Transport workers belong to a specific outpost** / **pause at unloaded route boundaries** 유지.

따라서 generic exploration-to-settlement value는 **완료/부분**으로 전진했다. 다음 탐험 breadth는 companion biome/NPC의 안정적인 soft seam이 실제로 있을 때만 추가한다.

'''
repl(gap,'## 4. 주민 / 생산 / 방어\n',secgap+'## 4. 주민 / 생산 / 방어\n')
repl(gap,
'''1. deeper exploration bridges — rare NPC/structure/boss별 정착 가치;
2. stable seam이 있을 때 companion-biome-aware outpost specialization;
3. per-soldier micromanagement 없이 가능한 physical military armory/loadout;''',
'''1. stable seam이 있을 때 companion-biome/rare-NPC-aware outpost specialization; generic exploration value는 Alpha.55에서 1차 연결됨;
2. per-soldier micromanagement 없이 가능한 physical military armory/loadout;
3. long survival + two-player multiplayer acceptance;''')

# Cumulative audits.
write(ROOT/'tools/test_alpha55_source.py','''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A54=ROOT/'tools/test_alpha54_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
a=text(A54).replace("print('Frontier Settlement alpha.23-54 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.54','0.1.0-alpha.55'); ns={'__file__':str(A54),'__name__':'__main__'}; exec(compile(a,str(A54),'exec'),ns,ns)
benefit=text(JAVA/'settlement/SettlementExplorationBenefitService.java'); out=text(JAVA/'settlement/SettlementOutpostService.java'); cmd=text(JAVA/'command/SettlementCommands.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json')
must(benefit,('MAX_SURVEY_LEVEL = 3','MAX_CONQUEST_LEVEL = 2','OUTPOST_WOOD_DISCOUNT_PER_CONQUEST = 4L','OUTPOST_STONE_DISCOUNT_PER_CONQUEST = 2L','data.discoveredExternalStructures().size()','data.defeatedExternalBosses().size()','SettlementOutpostService.WOOD_COST - conquestLevel(data)','SettlementOutpostService.STONE_COST - conquestLevel(data)'), 'alpha.55 deterministic exploration benefit')
forbid(benefit,('ItemStack','setBlock','addPopulation','updateResources','forceChunk','teleportTo'),'alpha.55 benefit must not become authority')
must(out,('WOOD_COST = 72L','STONE_COST = 48L','detectSpecialization(ServerLevel level, BlockPos center, SettlementData data)','SettlementExplorationBenefitService.outpostWoodCost(data)','SettlementExplorationBenefitService.outpostStoneCost(data)','ores += SettlementExplorationBenefitService.oreEvidenceBonus(data)','logs += SettlementExplorationBenefitService.logEvidenceBonus(data)','fieldGround += SettlementExplorationBenefitService.fieldEvidenceBonus(data)','exposedStone += SettlementExplorationBenefitService.stoneEvidenceBonus(data)','materialCostDelta(plan, step, true, totalWoodCost)','materialCostDelta(plan, step, false, totalStoneCost)'), 'alpha.55 existing outpost integration')
must(cmd,('개척 지식 | 정찰 ','SettlementExplorationBenefitService.surveyLevel(data)','SettlementExplorationBenefitService.conquestLevel(data)','반복 발견/정복 보너스 없음'),'alpha.55 compact status')
must(props,('mod_version=0.1.0-alpha.55','capped non-farmable exploration knowledge'),'alpha.55 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.55"','Alpha.55 derives a capped survey level','at most 8 wood and 4 stone','no new currency, loot minting, save authority or logistics controller','"status": "candidate_runtime_lock"'),'alpha.55 lock')
print('Frontier Settlement alpha.23-55 cumulative source audit: PASS')
''')
write(ROOT/'tools/test_alpha55_docs.py','''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
readme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); original=text('ORIGINAL_DESIGN_v0.2.md')
must(original,('탐험','전초'),'original exploration/outpost scope')
must(readme,('## Current version: 0.1.0-alpha.55','## Alpha.55 — non-farmable exploration knowledge feeds existing outposts','survey level 0–3','conquest level 0–2','64 wood + 44 stone','Transport workers belong to a specific outpost','pause at unloaded route boundaries','there is still only one authority for long-distance outpost transport'),'alpha.55 README')
must(can,('Current canonical implementation: **0.1.0-alpha.55**','### Alpha.55 exploration knowledge -> existing outpost value','surveyLevel = min(3','conquestLevel = min(2','minimum64/44','builder walks from actual settlement storage carrying real wood/stone stacks','single authority for outpost transport','there is still only one authority for long-distance outpost transport','## 15. Unfinished original-scope priorities after Alpha.55'),'alpha.55 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.55`','### Alpha.55 탐험 지식 / 전초 가치 감사','최저64/44','generic exploration-to-settlement value는 **완료/부분**','## 11. 완료 판정 금지선'),'alpha.55 gap')
print('Frontier Settlement alpha.55 canonical docs audit: PASS')
''')
print('Applied Frontier Settlement 0.1.0-alpha.55 exploration knowledge bridge.')
