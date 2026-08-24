#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'
A52=ROOT/'tools/test_alpha52_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
if not A52.exists(): raise SystemExit('historical Alpha.52 audit missing')
a=text(A52).replace("print('Frontier Settlement alpha.23-52 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.52','0.1.0-alpha.53')
ns={'__file__':str(A52),'__name__':'__main__'}
exec(compile(a,str(A52),'exec'),ns,ns)
state=text(JAVA/'settlement/RoadConstructionState.java')
road=text(JAVA/'settlement/SettlementRoadService.java')
props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json')
must(state,('TUNNEL_STEP_OFFSET = 1_500_000','PROFILE_TUNNEL = 2','step >= GRADE_STEP_OFFSET && step < TUNNEL_STEP_OFFSET','step >= TUNNEL_STEP_OFFSET && step < PAVE_STEP_OFFSET','tunnelStep()','tunnelCenterCount()','hasTunnel()','hasTunnel() ? TUNNEL_STEP_OFFSET : PAVE_STEP_OFFSET','tunneling() && nextStep == 0'), 'alpha.53 tunnel save/phase')
must(road,('MAX_TUNNEL_SPAN = 24','MIN_TUNNEL_COVER = 4','TUNNEL_CLEAR_HEIGHT = 3','TUNNEL_SURCHARGE_PER_CENTER = 1','SettlementTier.FRONTIER_TOWN','BuildingType.CONSTRUCTION_OFFICE','tickTunneling(','tunnelExcavationPlan(','moveBuilderToTunnelWork(','isNaturalTunnelExcavation(','PROFILE_TUNNEL','setBlock(cell.target(), Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE)','data.advanceRoadConstruction()','event.setCanceled(true)','placement.tunnel() && surface.equals(pos)','road.tunneling() ? tunnelExcavationPlan(road).size()','road.tunnelCenterCount() * TUNNEL_SURCHARGE_PER_CENTER'), 'alpha.53 physical tunnel authority')
cut=road.find('setBlock(cell.target(), Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE)')
advance=road.find('data.advanceRoadConstruction()',cut)
if cut<0 or advance<0 or cut>=advance: raise SystemExit('alpha.53 tunnel must mutate world before state advance')
forbid(road,('destroyBlock(','dropResources(','forceChunk','setChunkForced','teleportTo('),'alpha.53 tunnel safety')
must(props,('mod_version=0.1.0-alpha.53','physical save-compatible straight tunnel excavation'),'alpha.53 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.53"','Alpha.53 adds bounded automatic straight tunnel excavation','no worldgen/companion hard dependency','"status": "candidate_runtime_lock"'),'alpha.53 lock')
print('Frontier Settlement alpha.23-53 cumulative source audit: PASS')
