from pathlib import Path
root=Path('projects/frontier-settlement')
tools=root/'tools'
source=r'''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A72=ROOT/'tools/test_alpha72_source.py'
_real=Path.read_text
def legacy_view(self,*args,**kwargs):
    s=_real(self,*args,**kwargs)
    if self.name=='gradle.properties':
        s=s.replace('mod_version=0.1.0-alpha.73','mod_version=0.1.0-alpha.72')
    elif self.name=='COMPANION_LOCK.json':
        s=s.replace('"frontier_settlement": "0.1.0-alpha.73"','"frontier_settlement": "0.1.0-alpha.72"')
    elif self.name=='SettlementMarketService.java':
        s=s.replace('SettlementExplorationBenefitService.marketPayout(data, tradeValue(goods))','tradeValue(goods)')
    elif self.name=='SettlementWorkshopService.java':
        s=s.replace('public static final int BASE_REPAIR_PER_METAL = 64;','private static final int REPAIR_PER_METAL = 64;')
        s=s.replace('SettlementExplorationBenefitService.repairPerMetal(data)','REPAIR_PER_METAL')
    elif self.name=='SettlementAdvancedWorkshopService.java':
        s=s.replace('? reforgeOne(level, crate, weaponSlot, data)', '? reforgeOne(level, crate, weaponSlot)')
        s=s.replace(': forgeOne(level, crate, weaponSlot, relicSlot, data)', ': forgeOne(level, crate, weaponSlot, relicSlot)')
        s=s.replace('private static boolean forgeOne(ServerLevel level, Container crate, int weaponSlot, int relicSlot, SettlementData data) {','private static boolean forgeOne(ServerLevel level, Container crate, int weaponSlot, int relicSlot) {')
        s=s.replace('private static boolean reforgeOne(ServerLevel level, Container crate, int weaponSlot, SettlementData data) {','private static boolean reforgeOne(ServerLevel level, Container crate, int weaponSlot) {')
        s=s.replace('SettlementExplorationBenefitService.forgePower(data)','ENCHANTMENT_POWER')
        s=s.replace('SettlementExplorationBenefitService.reforgePower(data)','REFORGE_POWER')
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
'''
(tools/'test_alpha73_source.py').write_text(source,encoding='utf-8')
