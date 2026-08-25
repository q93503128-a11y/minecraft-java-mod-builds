from pathlib import Path
import json

root=Path('projects/frontier-settlement')
sett=root/'src/main/java/kr/moonseungjun/frontiersettlement/settlement'

def read(p): return p.read_text(encoding='utf-8')
def write(p,s): p.write_text(s,encoding='utf-8')
def repl(p,a,b):
    s=read(p)
    if a not in s: raise SystemExit(f'missing replacement token in {p}: {a[:120]}')
    write(p,s.replace(a,b))

benefit=sett/'SettlementExplorationBenefitService.java'
write(benefit, '''package kr.moonseungjun.frontiersettlement.settlement;

/**
 * Deterministic exploration-to-settlement feedback.
 * First-time survey/conquest metadata never becomes currency or item authority: it only improves
 * existing physical market, maintenance, forging and outpost systems.
 */
public final class SettlementExplorationBenefitService {
    public static final int MAX_SURVEY_LEVEL = 3;
    public static final int MAX_CONQUEST_LEVEL = 2;
    public static final long OUTPOST_WOOD_DISCOUNT_PER_CONQUEST = 4L;
    public static final long OUTPOST_STONE_DISCOUNT_PER_CONQUEST = 2L;
    public static final int MARKET_EMERALD_BONUS_PER_SURVEY = 1;
    public static final int MARKET_EMERALD_BONUS_PER_CONQUEST = 2;
    public static final int REPAIR_BONUS_PER_SURVEY = 16;
    public static final int REPAIR_BONUS_PER_CONQUEST = 8;
    public static final int FORGE_POWER_BONUS_PER_SURVEY = 2;
    public static final int FORGE_POWER_BONUS_PER_CONQUEST = 2;

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

    public static int marketPayoutBonus(SettlementData data) {
        return surveyLevel(data) * MARKET_EMERALD_BONUS_PER_SURVEY
                + conquestLevel(data) * MARKET_EMERALD_BONUS_PER_CONQUEST;
    }

    public static int marketPayout(SettlementData data, int basePayout) {
        return Math.max(0, basePayout) + marketPayoutBonus(data);
    }

    public static int repairPerMetal(SettlementData data) {
        return SettlementWorkshopService.BASE_REPAIR_PER_METAL
                + surveyLevel(data) * REPAIR_BONUS_PER_SURVEY
                + conquestLevel(data) * REPAIR_BONUS_PER_CONQUEST;
    }

    public static int forgePower(SettlementData data) {
        return SettlementAdvancedWorkshopService.ENCHANTMENT_POWER
                + surveyLevel(data) * FORGE_POWER_BONUS_PER_SURVEY
                + conquestLevel(data) * FORGE_POWER_BONUS_PER_CONQUEST;
    }

    public static int reforgePower(SettlementData data) {
        return SettlementAdvancedWorkshopService.REFORGE_POWER
                + surveyLevel(data) * FORGE_POWER_BONUS_PER_SURVEY
                + conquestLevel(data) * FORGE_POWER_BONUS_PER_CONQUEST;
    }

    public static String supportSummary(SettlementData data) {
        return "조사 " + surveyLevel(data) + "/" + MAX_SURVEY_LEVEL
                + " · 정복 " + conquestLevel(data) + "/" + MAX_CONQUEST_LEVEL
                + " · 시장 +" + marketPayoutBonus(data)
                + " · 수리 " + repairPerMetal(data) + "/금속"
                + " · 제작 " + forgePower(data) + "/" + reforgePower(data);
    }

    public static int oreEvidenceBonus(SettlementData data) { return surveyLevel(data) >= 2 ? 1 : 0; }
    public static int logEvidenceBonus(SettlementData data) { return surveyLevel(data) * 2; }
    public static int fieldEvidenceBonus(SettlementData data) { return surveyLevel(data) * 8; }
    public static int stoneEvidenceBonus(SettlementData data) { return surveyLevel(data) * 2; }
}
''')

p=sett/'SettlementMarketService.java'
repl(p,'tradeOne(container, trader);','tradeOne(container, trader, data);')
repl(p,'private static void tradeOne(Container container, Villager trader) {','private static void tradeOne(Container container, Villager trader, SettlementData data) {')
repl(p,'int payout = tradeValue(goods);','int payout = SettlementExplorationBenefitService.marketPayout(data, tradeValue(goods));')

p=sett/'SettlementWorkshopService.java'
repl(p,'private static final int REPAIR_PER_METAL = 64;','public static final int BASE_REPAIR_PER_METAL = 64;')
repl(p,'weapon.setDamageValue(Math.max(0, weapon.getDamageValue() - REPAIR_PER_METAL));','weapon.setDamageValue(Math.max(0, weapon.getDamageValue() - SettlementExplorationBenefitService.repairPerMetal(data)));')

p=sett/'SettlementAdvancedWorkshopService.java'
repl(p,'? reforgeOne(level, crate, weaponSlot)\n                    : forgeOne(level, crate, weaponSlot, relicSlot);','? reforgeOne(level, crate, weaponSlot, data)\n                    : forgeOne(level, crate, weaponSlot, relicSlot, data);')
repl(p,'private static boolean forgeOne(ServerLevel level, Container crate, int weaponSlot, int relicSlot) {','private static boolean forgeOne(ServerLevel level, Container crate, int weaponSlot, int relicSlot, SettlementData data) {')
repl(p,'EnchantmentHelper.enchantItem(level.getRandom(), forged, ENCHANTMENT_POWER,','EnchantmentHelper.enchantItem(level.getRandom(), forged, SettlementExplorationBenefitService.forgePower(data),')
repl(p,'private static boolean reforgeOne(ServerLevel level, Container crate, int weaponSlot) {','private static boolean reforgeOne(ServerLevel level, Container crate, int weaponSlot, SettlementData data) {')
repl(p,'EnchantmentHelper.selectEnchantment(level.getRandom(), reforged, REFORGE_POWER,','EnchantmentHelper.selectEnchantment(level.getRandom(), reforged, SettlementExplorationBenefitService.reforgePower(data),')

p=sett/'SettlementExplorationService.java'
repl(p,'player.sendSystemMessage(Component.literal("개척 발견 · 외부 구조물 " + id + " | 탐험 진척 " + data.explorationScore()));','player.sendSystemMessage(Component.literal("개척 발견 · 외부 구조물 " + id + " | " + SettlementExplorationBenefitService.supportSummary(data)));')
repl(p,'player.sendSystemMessage(Component.literal("개척 정복 · 강적 " + id + " | 탐험 진척 " + data.explorationScore()));','player.sendSystemMessage(Component.literal("개척 정복 · 강적 " + id + " | " + SettlementExplorationBenefitService.supportSummary(data)\n                + " · 신규 전초 " + SettlementExplorationBenefitService.outpostWoodCost(data) + "목재/"\n                + SettlementExplorationBenefitService.outpostStoneCost(data) + "석재"));')

p=sett/'SettlementContextService.java'
repl(p,'type.displayName(), buildingDetail(type), -1));','type.displayName(), buildingDetail(type, data), -1));')
repl(p,'private static String buildingDetail(BuildingType type) {','private static String buildingDetail(BuildingType type, SettlementData data) {')
repl(p,'case WORKSHOP -> "완공 · 금속 → 무기 수리";','case WORKSHOP -> "완공 · 금속 1 → 외부무기 내구 +" + SettlementExplorationBenefitService.repairPerMetal(data);')
repl(p,'case ADVANCED_WORKSHOP -> "완공 · 무기 + 유물 + 금속 → 고급 제작";','case ADVANCED_WORKSHOP -> "완공 · 고급 제작 위력 " + SettlementExplorationBenefitService.forgePower(data)\n                    + " · 영지 재련 " + SettlementExplorationBenefitService.reforgePower(data);')
repl(p,'case MARKET -> "완공 · 유물 → 실물 교역";','case MARKET -> "완공 · 유물 → 실물 교역 · 개척 보너스 +" + SettlementExplorationBenefitService.marketPayoutBonus(data);')

props=root/'gradle.properties'
repl(props,'mod_version=0.1.0-alpha.72','mod_version=0.1.0-alpha.73')
s=read(props).replace('and no-drop renewable civic guards.','and no-drop renewable civic guards, plus exploration-feedback progression where unique surveys/conquests improve physical relic market payouts, metal-based weapon repair efficiency, and advanced weapon forging power without adding a currency, menu, or virtual resource authority.')
write(props,s)

canonical=root/'CANONICAL_PLAN.md'
s=read(canonical).replace('Current canonical implementation: **0.1.0-alpha.72**.','Current canonical implementation: **0.1.0-alpha.73**.')
marker='### Alpha.72 full-project authority / transaction hardening'
section='''### Alpha.73 expedition feedback gameplay pass

Alpha.73 returns priority from pure hardening to the original v0.2 gameplay loop: **explore/fight -> settlement becomes more useful -> better prepared exploration**. It adds no new menu, key, currency, building family or resource authority.

- first-time external structure survey still caps at level3 and first-time conquest still caps at level2; repeated IDs remain non-farmable;
- survey/conquest metadata now improves the existing dedicated market-barrel relic payout by a bounded flat premium (`+1` emerald per survey level, `+2` per conquest level); only a real relic ItemStack deliberately placed in the market is consumed and payout remains a real emerald ItemStack;
- the normal workshop still consumes exactly one real metal item per repair operation, but settlement expedition knowledge raises repaired durability from base64 up to128 at survey3/conquest2;
- advanced forge/re-forge physical costs remain relic1+metal4 and relic2+metal8, while validated enchantment selection power scales from30/40 to at most40/50; no-cost/no-improvement behavior is unchanged;
- unique discovery/conquest notices show the currently active compact survey/conquest support benefits, and building context for market/workshop/advanced workshop exposes the same numbers without adding a dashboard;
- existing conquest outpost-cost reduction and survey specialization evidence remain active, so exploration knowledge now affects territory expansion, trade, maintenance and expedition equipment preparation rather than only a small hidden score;
- companion content remains optional/soft-linked and full fresh-world runtime acceptance is still user-playtest work, not claimed by this content pass.

'''
if marker not in s: raise SystemExit('canonical alpha72 marker missing')
write(canonical,s.replace(marker,section+marker))

gap=root/'COMPLETION_GAP_AUDIT.md'
s=read(gap).replace('현재 구현 기준: `0.1.0-alpha.72`','현재 구현 기준: `0.1.0-alpha.73`')
marker='### Alpha.72 전체 소스 authority / transaction 감사'
section='''### Alpha.73 탐험-정착 되먹임 게임성 패스

- unique 외부 구조물 조사3 / 강적 정복2 상한과 중복 방지는 유지한다.
- 조사/정복은 기존 전초 비용/특화 보너스에 더해 시장 유물 판매, 외부무기 수리, 고급 제작/재련 성능을 자동 강화한다.
- 시장은 전용 barrel에 플레이어가 넣은 실제 expedition relic만 소비하고 실제 emerald를 지급하며 최대 추가 보너스는 +7이다.
- 작업장은 여전히 실제 금속1을 소비하지만 수리량은 base64에서 최대128까지 상승한다.
- 고급 제작 물리 비용 relic1+metal4 / 재련 relic2+metal8은 바뀌지 않고 enchant selection power만 최대40/50까지 상승한다.
- 새 연구 UI/탐험 화폐/save field/building/key/물류 권위는 없다. 탐험 성과가 반복 생존 노동과 다음 원정 준비를 직접 줄이는 원본 게임 루프 강화다.
- 밸런스 체감은 사용자 실플레이에서 조정하되, 개발측 구현 갭으로는 기존 hidden exploration score 의존도가 줄었다.

'''
if marker not in s: raise SystemExit('gap alpha72 marker missing')
write(gap,s.replace(marker,section+marker))

readme=root/'README.md'
s=read(readme).replace('## Current version: 0.1.0-alpha.72','## Current version: 0.1.0-alpha.73')
marker='## Alpha.72 — full-project authority / transaction hardening'
section='''## Alpha.73 — expedition feedback gameplay pass

Exploration now feeds back into the settlement more visibly without adding management. Unique companion/vanilla milestone surveys and conquests keep their capped non-farmable metadata, but that knowledge now automatically raises dedicated-market relic payouts, metal-per-repair efficiency, and advanced external-weapon forge/re-forge power. Existing outpost construction discounts and specialization evidence remain.

All transactions remain physical: market relics become real emerald ItemStacks, workshop service still consumes one real metal item, forge costs remain relic1+metal4 and DOMAIN reforge remains relic2+metal8. Max knowledge changes market payout by +7, repair efficiency from64 to128 durability per metal, forge power30→40 and reforge power40→50. Discovery/conquest notices and compact building context expose the active benefit values; there is no research screen, exploration currency, new key or new building.

'''
if marker not in s: raise SystemExit('readme alpha72 marker missing')
write(readme,s.replace(marker,section+marker))

lockp=root/'COMPANION_LOCK.json'
lock=json.loads(read(lockp))
lock['target']['frontier_settlement']='0.1.0-alpha.73'
lock['notes'].append('Alpha.73 is a gameplay feedback pass: capped first-time survey/conquest metadata now raises existing physical market relic payouts, one-metal weapon repair efficiency and advanced forge/reforge selection power while preserving exact ItemStack costs, no new currency/menu/save authority, optional companion boot, and the existing outpost-cost/specialization benefits.')
write(lockp,json.dumps(lock,ensure_ascii=False,indent=2)+'\n')

tools=root/'tools'
write(tools/'test_alpha73_source.py', '''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A72=ROOT/'tools/test_alpha72_source.py'
_real=Path.read_text
def legacy_view(self,*args,**kwargs):
    s=_real(self,*args,**kwargs)
    if self.name=='gradle.properties': s=s.replace('mod_version=0.1.0-alpha.73','mod_version=0.1.0-alpha.72')
    elif self.name=='COMPANION_LOCK.json': s=s.replace('"frontier_settlement": "0.1.0-alpha.73"','"frontier_settlement": "0.1.0-alpha.72"')
    return s
Path.read_text=legacy_view
try:
    a=_real(A72,encoding='utf-8').replace("print('Frontier Settlement alpha.23-72 full-project source audit: PASS')",'pass')
    ns={'__file__':str(A72),'__name__':'__main__'}; exec(compile(a,str(A72),'exec'),ns,ns)
finally: Path.read_text=_real
def text(p): return Path(p).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
sett=JAVA/'settlement'; benefit=text(sett/'SettlementExplorationBenefitService.java'); market=text(sett/'SettlementMarketService.java'); workshop=text(sett/'SettlementWorkshopService.java'); advanced=text(sett/'SettlementAdvancedWorkshopService.java'); explore=text(sett/'SettlementExplorationService.java'); context=text(sett/'SettlementContextService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json')
if len(list((ROOT/'src/main/java').rglob('*.java')))!=105: raise SystemExit('alpha.73 Java family count changed')
must(benefit,('marketPayoutBonus(SettlementData data)','marketPayout(SettlementData data, int basePayout)','repairPerMetal(SettlementData data)','forgePower(SettlementData data)','reforgePower(SettlementData data)','supportSummary(SettlementData data)','MAX_SURVEY_LEVEL = 3','MAX_CONQUEST_LEVEL = 2'), 'alpha.73 benefits')
must(market,('tradeOne(container, trader, data)','SettlementExplorationBenefitService.marketPayout(data, tradeValue(goods))'), 'alpha.73 physical market')
must(workshop,('BASE_REPAIR_PER_METAL = 64','SettlementExplorationBenefitService.repairPerMetal(data)'), 'alpha.73 repair feedback')
must(advanced,('forgeOne(level, crate, weaponSlot, relicSlot, data)','reforgeOne(level, crate, weaponSlot, data)','SettlementExplorationBenefitService.forgePower(data)','SettlementExplorationBenefitService.reforgePower(data)','METAL_COST = 4','REFORGE_METAL_COST = 8','RELIC_COST = 1','REFORGE_RELIC_COST = 2'), 'alpha.73 forge feedback')
must(explore,('SettlementExplorationBenefitService.supportSummary(data)','SettlementExplorationBenefitService.outpostWoodCost(data)','SettlementExplorationBenefitService.outpostStoneCost(data)'), 'alpha.73 discovery feedback')
must(context,('buildingDetail(type, data)','개척 보너스 +','repairPerMetal(data)','forgePower(data)','reforgePower(data)'), 'alpha.73 compact context')
must(props,('mod_version=0.1.0-alpha.73','exploration-feedback progression'), 'alpha.73 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.73"','Alpha.73 is a gameplay feedback pass'), 'alpha.73 lock')
print('Frontier Settlement alpha.23-73 cumulative source audit: PASS')
''')

write(tools/'test_alpha73_docs.py', '''#!/usr/bin/env python3
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; A72=ROOT/'tools/test_alpha72_docs.py'
_real=Path.read_text
def legacy_view(self,*args,**kwargs):
    s=_real(self,*args,**kwargs)
    if self.name in ('CANONICAL_PLAN.md','COMPLETION_GAP_AUDIT.md','README.md','COMPANION_LOCK.json'):
        s=s.replace('0.1.0-alpha.73','0.1.0-alpha.72').replace('Alpha.73','Alpha.72')
    return s
Path.read_text=legacy_view
try:
    a=_real(A72,encoding='utf-8').replace("print('Frontier Settlement alpha.72 canonical docs audit: PASS')",'pass')
    ns={'__file__':str(A72),'__name__':'__main__'}; exec(compile(a,str(A72),'exec'),ns,ns)
finally: Path.read_text=_real
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
canonical=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); readme=text('README.md'); lock=json.loads(text('COMPANION_LOCK.json'))
must(canonical,('Current canonical implementation: **0.1.0-alpha.73**','### Alpha.73 expedition feedback gameplay pass','explore/fight -> settlement becomes more useful','market-barrel relic payout','base64 up to128','at most40/50'), 'alpha.73 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.73`','### Alpha.73 탐험-정착 되먹임 게임성 패스','최대 추가 보너스는 +7','base64에서 최대128','최대40/50'), 'alpha.73 gap')
must(readme,('## Current version: 0.1.0-alpha.73','## Alpha.73 — expedition feedback gameplay pass','market payout by +7','repair efficiency from64 to128','forge power30→40','reforge power40→50'), 'alpha.73 readme')
if lock.get('status')!='candidate_runtime_lock': raise SystemExit('alpha.73 companion lock overclaim')
if lock.get('target',{}).get('frontier_settlement')!='0.1.0-alpha.73': raise SystemExit('alpha.73 lock target mismatch')
notes='\n'.join(lock.get('notes',[])); must(notes,('Alpha.73 is a gameplay feedback pass','no new currency/menu/save authority'), 'alpha.73 lock note')
print('Frontier Settlement alpha.73 canonical docs audit: PASS')
''')
