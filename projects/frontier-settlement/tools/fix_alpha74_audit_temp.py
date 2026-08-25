#!/usr/bin/env python3
from pathlib import Path
p=Path(__file__).resolve().parent/'test_alpha74_source.py'
s=p.read_text(encoding='utf-8')
old="""    elif self.name=='COMPANION_LOCK.json': s=s.replace('\"frontier_settlement\": \"0.1.0-alpha.74\"','\"frontier_settlement\": \"0.1.0-alpha.73\"')
    return s
"""
new="""    elif self.name=='COMPANION_LOCK.json': s=s.replace('\"frontier_settlement\": \"0.1.0-alpha.74\"','\"frontier_settlement\": \"0.1.0-alpha.73\"')
    elif self.name=='SettlementBarracksService.java':
        s=s.replace('public static final double BASE_THREAT_RADIUS = 28.0D;', 'private static final double THREAT_RADIUS = 28.0D;')
        s=s.replace('        long recruitFoodCost = SettlementExplorationBenefitService.barracksRecruitFoodCost(level.getServer());\\n        if (resources.food() < recruitFoodCost || resources.metal() < RECRUIT_METAL_COST) return false;', '        if (resources.food() < RECRUIT_FOOD_COST || resources.metal() < RECRUIT_METAL_COST) return false;')
        s=s.replace('SettlementStorageService.consumeMetalAndFood(level, data, RECRUIT_METAL_COST, recruitFoodCost)', 'SettlementStorageService.consumeMetalAndFood(level, data, RECRUIT_METAL_COST, RECRUIT_FOOD_COST)')
        s=s.replace('        double threatRadius = SettlementExplorationBenefitService.barracksThreatRadius(level.getServer());\\n        AABB area = new AABB(center).inflate(threatRadius, 12.0D, threatRadius);', '        AABB area = new AABB(center).inflate(THREAT_RADIUS, 12.0D, THREAT_RADIUS);')
    return s
"""
if s.count(old)!=1:
    raise SystemExit(f'expected alpha74 legacy insertion point once, got {s.count(old)}')
p.write_text(s.replace(old,new,1),encoding='utf-8')
print('Alpha.74 historical barracks audit view fixed')
