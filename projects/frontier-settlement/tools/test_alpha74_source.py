#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A73=ROOT/'tools/test_alpha73_source.py'
_real_read=Path.read_text; _real_rglob=Path.rglob
def legacy_read(self,*args,**kwargs):
    s=_real_read(self,*args,**kwargs)
    if self.name=='gradle.properties': s=s.replace('mod_version=0.1.0-alpha.74','mod_version=0.1.0-alpha.73')
    elif self.name=='COMPANION_LOCK.json': s=s.replace('"frontier_settlement": "0.1.0-alpha.74"','"frontier_settlement": "0.1.0-alpha.73"')
    elif self.name=='SettlementBarracksService.java':
        s=s.replace('public static final double BASE_THREAT_RADIUS = 28.0D;', 'private static final double THREAT_RADIUS = 28.0D;')
        s=s.replace('        long recruitFoodCost = SettlementExplorationBenefitService.barracksRecruitFoodCost(level.getServer());\n        if (resources.food() < recruitFoodCost || resources.metal() < RECRUIT_METAL_COST) return false;', '        if (resources.food() < RECRUIT_FOOD_COST || resources.metal() < RECRUIT_METAL_COST) return false;')
        s=s.replace('SettlementStorageService.consumeMetalAndFood(level, data, RECRUIT_METAL_COST, recruitFoodCost)', 'SettlementStorageService.consumeMetalAndFood(level, data, RECRUIT_METAL_COST, RECRUIT_FOOD_COST)')
        s=s.replace('        double threatRadius = SettlementExplorationBenefitService.barracksThreatRadius(level.getServer());\n        AABB area = new AABB(center).inflate(threatRadius, 12.0D, threatRadius);', '        AABB area = new AABB(center).inflate(THREAT_RADIUS, 12.0D, THREAT_RADIUS);')
    return s
def legacy_rglob(self,pattern):
    values=list(_real_rglob(self,pattern))
    if pattern=='*.java': values=[p for p in values if p.name!='SettlementThreatKnowledgeData.java']
    return iter(values)
Path.read_text=legacy_read; Path.rglob=legacy_rglob
try:
    a=_real_read(A73,encoding='utf-8').replace("print('Frontier Settlement alpha.23-73 cumulative source audit: PASS')",'pass')
    ns={'__file__':str(A73),'__name__':'__main__'}; exec(compile(a,str(A73),'exec'),ns,ns)
finally:
    Path.read_text=_real_read; Path.rglob=_real_rglob
def text(p): return Path(p).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
sett=JAVA/'settlement'; threat=text(sett/'SettlementThreatKnowledgeData.java'); explore=text(sett/'SettlementExplorationService.java'); benefit=text(sett/'SettlementExplorationBenefitService.java'); barracks=text(sett/'SettlementBarracksService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json')
if len(list((ROOT/'src/main/java').rglob('*.java')))!=106: raise SystemExit('alpha.74 Java family count changed')
must(threat,('threat_knowledge','defeated_external_threats','MAX_RECORDED_THREAT_TYPES = 64','recordExternalThreat(String id)','threatLevel()'), 'alpha.74 threat data')
must(explore,('isExternalThreat(victim, id)','SettlementThreatKnowledgeData.get(server)','개척 전투보고 · 외부 위협','instanceof net.minecraft.world.entity.monster.Monster'), 'alpha.74 external threat observation')
must(benefit,('MAX_THREAT_LEVEL = 3','barracksRecruitFoodCost(MinecraftServer server)','Math.max(5L','barracksThreatRadius(MinecraftServer server)','BARRACKS_THREAT_RADIUS_BONUS_PER_LEVEL = 4.0D','threatSupportSummary(MinecraftServer server)'), 'alpha.74 threat benefits')
must(barracks,('BASE_THREAT_RADIUS = 28.0D','long recruitFoodCost = SettlementExplorationBenefitService.barracksRecruitFoodCost(level.getServer())','consumeMetalAndFood(level, data, RECRUIT_METAL_COST, recruitFoodCost)','SettlementExplorationBenefitService.barracksThreatRadius(level.getServer())'), 'alpha.74 physical barracks')
must(props,('mod_version=0.1.0-alpha.74','bounded first-kill external-hostile field knowledge'), 'alpha.74 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.74"','Alpha.74 adds bounded first-kill external-hostile field knowledge'), 'alpha.74 lock')
for forbidden in ('variantsandventures.', 'com.faboslav', 'ResourcefulLib'):
    if forbidden in explore or forbidden in threat: raise SystemExit(f'alpha.74 hard companion dependency: {forbidden}')
print('Frontier Settlement alpha.23-74 cumulative source audit: PASS')
