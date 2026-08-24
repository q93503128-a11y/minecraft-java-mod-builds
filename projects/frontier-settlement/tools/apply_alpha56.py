#!/usr/bin/env python3
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'

def read(p): return p.read_text(encoding='utf-8')
def write(p,s): p.parent.mkdir(parents=True,exist_ok=True); p.write_text(s,encoding='utf-8')
def repl(p,old,new):
    s=read(p); c=s.count(old)
    if c!=1: raise SystemExit(f'{p}: expected one anchor, found {c}: {old[:110]!r}')
    write(p,s.replace(old,new,1))

write(JAVA/'settlement/SettlementOutpostBiomeService.java','''package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.Tags;

/**
 * Soft biome-tag context for existing outpost specialization. No companion class or biome id is required.
 * The physical local survey remains primary; biome tags only provide bounded evidence bias.
 */
public final class SettlementOutpostBiomeService {
    public static final int FOREST_LOG_BONUS = 8;
    public static final int OPEN_FIELD_BONUS = 24;
    public static final int MOUNTAIN_STONE_BONUS = 8;
    public static final int MOUNTAIN_ORE_BONUS = 1;
    public static final int DRY_STONE_BONUS = 6;

    public record Bias(int ore, int logs, int field, int stone, String label) {
        static final Bias NONE = new Bias(0, 0, 0, 0, "중립");
    }

    private SettlementOutpostBiomeService() {}

    public static Bias bias(ServerLevel level, BlockPos center) {
        if (!level.hasChunkAt(center)) return Bias.NONE;
        var biome = level.getBiome(center);
        int ore = 0;
        int logs = 0;
        int field = 0;
        int stone = 0;
        String label = "중립";

        if (biome.is(Tags.Biomes.IS_FOREST) || biome.is(Tags.Biomes.IS_DENSE_VEGETATION)) {
            logs += FOREST_LOG_BONUS;
            label = "삼림";
        }
        if (biome.is(Tags.Biomes.IS_PLAINS) || biome.is(Tags.Biomes.IS_SAVANNA)) {
            field += OPEN_FIELD_BONUS;
            label = "개활지";
        }
        if (biome.is(Tags.Biomes.IS_MOUNTAIN) || biome.is(Tags.Biomes.IS_HILL)) {
            stone += MOUNTAIN_STONE_BONUS;
            ore += MOUNTAIN_ORE_BONUS;
            label = "산악";
        } else if (biome.is(Tags.Biomes.IS_BADLANDS) || biome.is(Tags.Biomes.IS_SANDY)) {
            stone += DRY_STONE_BONUS;
            label = "건조 암지";
        }
        return new Bias(ore, logs, field, stone, label);
    }
}
''')

out=JAVA/'settlement/SettlementOutpostService.java'
repl(out,
'''        BlockPos center = outpostCenter(gate, road.directionX(), road.directionZ());
        String specialization = detectSpecialization(level, center, data);
        long woodCost = SettlementExplorationBenefitService.outpostWoodCost(data);''',
'''        BlockPos center = outpostCenter(gate, road.directionX(), road.directionZ());
        SettlementOutpostBiomeService.Bias biomeBias = SettlementOutpostBiomeService.bias(level, center);
        String specialization = detectSpecialization(level, center, data);
        long woodCost = SettlementExplorationBenefitService.outpostWoodCost(data);''')
repl(out,
'''        return new PlacementCheck(true, roadIndex, gate, road.directionX(), road.directionZ(), specialization,
                "배치 가능 · " + specializationDisplayName(specialization) + " 후보");''',
'''        return new PlacementCheck(true, roadIndex, gate, road.directionX(), road.directionZ(), specialization,
                "배치 가능 · " + specializationDisplayName(specialization) + " 후보 · 환경 " + biomeBias.label());''')
repl(out,
'''        if (data != null) {
            ores += SettlementExplorationBenefitService.oreEvidenceBonus(data);
            logs += SettlementExplorationBenefitService.logEvidenceBonus(data);
            fieldGround += SettlementExplorationBenefitService.fieldEvidenceBonus(data);
            exposedStone += SettlementExplorationBenefitService.stoneEvidenceBonus(data);
        }
        if (ores >= 4) return "mining";''',
'''        if (data != null) {
            ores += SettlementExplorationBenefitService.oreEvidenceBonus(data);
            logs += SettlementExplorationBenefitService.logEvidenceBonus(data);
            fieldGround += SettlementExplorationBenefitService.fieldEvidenceBonus(data);
            exposedStone += SettlementExplorationBenefitService.stoneEvidenceBonus(data);
        }
        SettlementOutpostBiomeService.Bias biomeBias = SettlementOutpostBiomeService.bias(level, center);
        ores += biomeBias.ore();
        logs += biomeBias.logs();
        fieldGround += biomeBias.field();
        exposedStone += biomeBias.stone();
        if (ores >= 4) return "mining";''')

props=ROOT/'gradle.properties'
repl(props,'mod_version=0.1.0-alpha.55','mod_version=0.1.0-alpha.56')
repl(props,'capped non-farmable exploration knowledge that improves existing outpost surveying and physical construction efficiency.','capped non-farmable exploration knowledge that improves existing outpost surveying and physical construction efficiency, plus soft common-biome-tag evidence that improves outpost specialization without hard worldgen dependencies.')

lock=ROOT/'COMPANION_LOCK.json'
repl(lock,'"frontier_settlement": "0.1.0-alpha.55"','"frontier_settlement": "0.1.0-alpha.56"')
repl(lock,
'    "Alpha.55 derives a capped survey level from unique external structure discoveries and a capped conquest level from unique boss types: survey knowledge only biases the existing local outpost-specialization evidence, while conquest reduces new outpost physical material totals by at most 8 wood and 4 stone; repeated IDs remain non-farmable and no new currency, loot minting, save authority or logistics controller is created.",\n',
'    "Alpha.55 derives a capped survey level from unique external structure discoveries and a capped conquest level from unique boss types: survey knowledge only biases the existing local outpost-specialization evidence, while conquest reduces new outpost physical material totals by at most 8 wood and 4 stone; repeated IDs remain non-farmable and no new currency, loot minting, save authority or logistics controller is created.",\n    "Alpha.56 reads only NeoForge common biome tags from the already-loaded outpost center and adds bounded evidence bias to the existing physical specialization survey: forest/dense vegetation helps lumber, plains/savanna helps agriculture, mountain/hill helps quarry/mining, and badlands/sandy terrain slightly helps quarry; no Terralith class/id hard dependency or biome-generated resource minting is introduced.",\n')
repl(lock,'so Alpha.55 keeps only HUD collision avoidance','so Alpha.56 keeps only HUD collision avoidance')

readme=ROOT/'README.md'
repl(readme,'## Current version: 0.1.0-alpha.55','## Current version: 0.1.0-alpha.56')
repl(readme,'No new Alpha.55 key was added.','No new Alpha.56 key was added.')
repl(readme,'Alpha.40–55 deepen existing systems','Alpha.40–56 deepen existing systems')
sec='''## Alpha.56 — soft biome-aware outpost specialization

Alpha.56 uses the stable soft seam that was left open after Alpha.55: common NeoForge biome tags at the **already-loaded outpost center**. It never calls Terralith or another worldgen mod directly.

- the existing 12-block physical ore/log/field/exposed-stone survey remains the primary specialization authority;
- `IS_FOREST` / `IS_DENSE_VEGETATION` adds only **+8 log evidence**;
- `IS_PLAINS` / `IS_SAVANNA` adds only **+24 field evidence**;
- `IS_MOUNTAIN` / `IS_HILL` adds **+8 exposed-stone +1 ore evidence**;
- `IS_BADLANDS` / `IS_SANDY` adds only **+6 exposed-stone evidence** when the stronger mountain rule does not apply;
- these bonuses are deliberately below the existing specialization thresholds (ore4, logs24, field120, stone24), so a biome tag alone does not magically create a mine/farm/quarry/lumber outpost;
- datapacks/worldgen companions that correctly participate in common biome tags can influence the same survey automatically; missing Terralith or any other companion still boots and behaves normally;
- no biome id string allowlist, reflection, class reference, chunk generation, locator, force-load, virtual resource, new specialization family or new saved field is added;
- placement preview exposes only a compact environment label (`삼림/개활지/산악/건조 암지/중립`) beside the existing specialization candidate; no new dashboard/key;
- Alpha.55 survey/conquest knowledge stacks only as bounded evidence/cost context; actual outpost materials and construction remain physical ItemStacks;
- `Transport workers belong to a specific outpost`, `pause at unloaded route boundaries`, and Alpha.27 remains the **single authority for outpost transport**; **there is still only one authority for long-distance outpost transport**.

This closes the generic companion-biome-aware specialization gap through a common tag seam. Rare-NPC-specific value remains optional only if a similarly stable soft data seam appears.

'''
repl(readme,'## Alpha.55 — non-farmable exploration knowledge feeds existing outposts\n',sec+'## Alpha.55 — non-farmable exploration knowledge feeds existing outposts\n')

can=ROOT/'CANONICAL_PLAN.md'
repl(can,'Current canonical implementation: **0.1.0-alpha.55**.','Current canonical implementation: **0.1.0-alpha.56**.')
repl(can,'Alpha.40–55 deepen systems','Alpha.40–56 deepen systems')
sec2='''### Alpha.56 common-biome-tag outpost specialization

Alpha.56 uses only `net.neoforged.neoforge.common.Tags.Biomes` against the already-loaded center biome. There is no Terralith/worldgen Java dependency or biome-ID allowlist.

- physical local evidence remains primary: mining threshold4 ore, lumber24 logs, agriculture120 field-ground, quarry24 exposed stone;
- forest/dense vegetation adds8 log evidence;
- plains/savanna adds24 field evidence;
- mountain/hill adds8 exposed-stone +1 ore evidence;
- badlands/sandy adds6 exposed-stone evidence if not already mountain/hill;
- all biome biases stay below their thresholds, so tags only resolve plausible borderline sites and never create resources or guarantee specialization by themselves;
- the common tag seam lets compatible datapacks/worldgen participate without Frontier importing their class, registry ID or assets;
- unloaded center means zero biome bias; no chunk generation or force-load;
- no new specialization family/save state/economy/worker/logistics authority;
- Alpha.55 knowledge and Alpha.56 biome context are both computed helpers, not spendable resources;
- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, Alpha.27 remains the **single authority for outpost transport**, and **there is still only one authority for long-distance outpost transport**.

'''
repl(can,'## 8. Resources and logistics\n',sec2+'## 8. Resources and logistics\n')
repl(can,'Alpha.45 exploration score, Alpha.55 survey/conquest knowledge and civil `earthBank` are not spendable resources.','Alpha.45 exploration score, Alpha.55 survey/conquest knowledge, Alpha.56 biome context and civil `earthBank` are not spendable resources.')
repl(can,'## 14. Current playable slice after Alpha.55','## 14. Current playable slice after Alpha.56')
repl(can,'## 15. Unfinished original-scope priorities after Alpha.55','## 15. Unfinished original-scope priorities after Alpha.56')
repl(can,
'''1. better companion-biome-aware / rare-NPC outpost specialization only where a stable soft data seam exists; Alpha.55 already supplies the generic non-farmable exploration-value bridge;
2. physical military armory/loadout only if it can stay automated and ItemStack-authoritative without per-soldier micromanagement;
3. long survival + two-player multiplayer acceptance;
4. long survival + two-player multiplayer acceptance;
5. optional deeper monumental crossings only if real play shows Alpha.52–54 breadth is insufficient; never expand by default into WorldEdit-scale civil works;''',
'''1. physical military armory/loadout only if it can stay automated and ItemStack-authoritative without per-soldier micromanagement;
2. long survival + two-player multiplayer acceptance;
3. rare-NPC-specific settlement value only if a stable soft data seam appears; generic biome-aware specialization is covered by Alpha.56;
4. optional deeper monumental crossings only if real play shows Alpha.52–54 breadth is insufficient; never expand by default into WorldEdit-scale civil works;''')
repl(can,'13. Alpha.54 one-bend detection/corner clearance/portal excavation/22-stone physical portal/save-reload acceptance;','13. Alpha.54 one-bend detection/corner clearance/portal excavation/22-stone physical portal/save-reload acceptance;\n14. Alpha.56 common-biome-tag borderline specialization / companion-installed-and-absent acceptance;')
# Renumber following acceptance lines shifted by one.
repl(can,'14. full companion lock fresh-world client/server runtime;\n15. true Xaero markers only if a stable supported API appears;\n16. moving boat/waterborne merchant only if presentation value justifies it and it never becomes a second logistics authority.','15. full companion lock fresh-world client/server runtime;\n16. true Xaero markers only if a stable supported API appears;\n17. moving boat/waterborne merchant only if presentation value justifies it and it never becomes a second logistics authority.')

# Gap cleanup + Alpha56 audit.
gap=ROOT/'COMPLETION_GAP_AUDIT.md'
repl(gap,'현재 구현 기준: `0.1.0-alpha.55`','현재 구현 기준: `0.1.0-alpha.56`')
repl(gap,'| 터널/더 깊은 대형 횡단 | 미구현/부분 | larger civil engineering next priority |','| 터널/더 깊은 대형 횡단 | 완료/부분 | Alpha.53 straight + Alpha.54 one-bend/physical portals; 더 거대한 토목은 선택적 |')
repl(gap,'| biome-aware companion specialization | 부분/미구현 | stable data seam 필요 |','| biome-aware companion specialization | **완료/부분** | Alpha.56 NeoForge common biome tags + local physical evidence, no hard worldgen dependency |')
secgap='''### Alpha.56 common-biome-tag 전초 특화 감사

- already-loaded outpost center의 NeoForge common biome tags만 읽음;
- forest/dense vegetation +log8, plains/savanna +field24, mountain/hill +stone8/+ore1, badlands/sandy +stone6;
- 기존 threshold ore4/log24/field120/stone24보다 단독 bias가 작아 biome만으로 특화 확정 불가;
- 기존 12-block physical local survey + Alpha.55 bounded survey knowledge가 계속 주권;
- unloaded center는 biome bias0, chunk generation/force-load 없음;
- Terralith class/id string, reflection, hard dependency 없음;
- biome이 자원/광석/식량을 생성하지 않음;
- 새 specialization family/save field/currency/worker/logistics authority 없음;
- `single authority for outpost transport` / `there is still only one authority for long-distance outpost transport` 유지;
- **Transport workers belong to a specific outpost** / **pause at unloaded route boundaries** 유지.

따라서 generic companion-biome-aware specialization은 **완료/부분**으로 전진했다. rare-NPC 연결은 안정적 soft seam이 실제 확인될 때만 남긴다.

'''
repl(gap,'## 8. 외부 콘텐츠 / companion\n',secgap+'## 8. 외부 콘텐츠 / companion\n')
repl(gap,
'''1. stable seam이 있을 때 companion-biome/rare-NPC-aware outpost specialization; generic exploration value는 Alpha.55에서 1차 연결됨;
2. per-soldier micromanagement 없이 가능한 physical military armory/loadout;
3. long survival + two-player multiplayer acceptance;
4. long survival + two-player multiplayer acceptance;
5. optional deeper monumental crossing은 Alpha.52–54 실플레이에서 실제 부족이 확인될 때만;''',
'''1. per-soldier micromanagement 없이 가능한 physical military armory/loadout;
2. long survival + two-player multiplayer acceptance;
3. rare-NPC-specific settlement value는 stable soft seam이 실제 확인될 때만; generic biome-aware specialization은 Alpha.56에서 1차 완료/부분;
4. optional deeper monumental crossing은 Alpha.52–54 실플레이에서 실제 부족이 확인될 때만;''')
repl(gap,'12. Alpha.54 one-bend/corner clearance/portal excavation/physical stone22/save-reload acceptance;','12. Alpha.54 one-bend/corner clearance/portal excavation/physical stone22/save-reload acceptance;\n13. Alpha.56 common-biome-tag borderline specialization + companion installed/absent acceptance;')
repl(gap,'13. full companion lock fresh-world client/server runtime;\n14. true Xaero marker는 stable supported API가 생길 때만;\n15. moving boat/waterborne merchant는 두 번째 logistics authority가 되지 않는 경우에만 선택적 presentation.','14. full companion lock fresh-world client/server runtime;\n15. true Xaero marker는 stable supported API가 생길 때만;\n16. moving boat/waterborne merchant는 두 번째 logistics authority가 되지 않는 경우에만 선택적 presentation.')

write(ROOT/'tools/test_alpha56_source.py','''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A55=ROOT/'tools/test_alpha55_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
a=text(A55).replace("print('Frontier Settlement alpha.23-55 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.55','0.1.0-alpha.56'); ns={'__file__':str(A55),'__name__':'__main__'}; exec(compile(a,str(A55),'exec'),ns,ns)
biome=text(JAVA/'settlement/SettlementOutpostBiomeService.java'); out=text(JAVA/'settlement/SettlementOutpostService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json')
must(biome,('FOREST_LOG_BONUS = 8','OPEN_FIELD_BONUS = 24','MOUNTAIN_STONE_BONUS = 8','MOUNTAIN_ORE_BONUS = 1','DRY_STONE_BONUS = 6','level.hasChunkAt(center)','level.getBiome(center)','Tags.Biomes.IS_FOREST','Tags.Biomes.IS_DENSE_VEGETATION','Tags.Biomes.IS_PLAINS','Tags.Biomes.IS_SAVANNA','Tags.Biomes.IS_MOUNTAIN','Tags.Biomes.IS_HILL','Tags.Biomes.IS_BADLANDS','Tags.Biomes.IS_SANDY'),'alpha.56 common biome soft seam')
forbid(biome,('terralith','Terralith','getChunk(','forceChunk','setChunkForced','ItemStack','setBlock(','Identifier.fromNamespaceAndPath'),'alpha.56 biome helper authority')
must(out,('SettlementOutpostBiomeService.bias(level, center)','ores += biomeBias.ore()','logs += biomeBias.logs()','fieldGround += biomeBias.field()','exposedStone += biomeBias.stone()','후보 · 환경 " + biomeBias.label()'),'alpha.56 existing specialization integration')
must(props,('mod_version=0.1.0-alpha.56','soft common-biome-tag evidence'),'alpha.56 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.56"','Alpha.56 reads only NeoForge common biome tags','no Terralith class/id hard dependency','"status": "candidate_runtime_lock"'),'alpha.56 lock')
print('Frontier Settlement alpha.23-56 cumulative source audit: PASS')
''')
write(ROOT/'tools/test_alpha56_docs.py','''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
readme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md')
must(readme,('## Current version: 0.1.0-alpha.56','## Alpha.56 — soft biome-aware outpost specialization','+8 log evidence','+24 field evidence','+8 exposed-stone +1 ore evidence','+6 exposed-stone evidence','common biome tags','Transport workers belong to a specific outpost','pause at unloaded route boundaries'),'alpha.56 README')
must(can,('Current canonical implementation: **0.1.0-alpha.56**','### Alpha.56 common-biome-tag outpost specialization','physical local evidence remains primary','forest/dense vegetation adds8','plains/savanna adds24','mountain/hill adds8','badlands/sandy adds6','there is still only one authority for long-distance outpost transport','## 15. Unfinished original-scope priorities after Alpha.56'),'alpha.56 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.56`','### Alpha.56 common-biome-tag 전초 특화 감사','biome-aware companion specialization | **완료/부분**','generic companion-biome-aware specialization은 **완료/부분**','## 11. 완료 판정 금지선'),'alpha.56 gap')
print('Frontier Settlement alpha.56 canonical docs audit: PASS')
''')

print('Applied Frontier Settlement 0.1.0-alpha.56 soft biome-aware outpost specialization.')
