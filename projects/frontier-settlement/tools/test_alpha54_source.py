#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'
A53=ROOT/'tools/test_alpha53_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
if not A53.exists(): raise SystemExit('historical Alpha.53 audit missing')
a=text(A53).replace("print('Frontier Settlement alpha.23-53 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.53','0.1.0-alpha.54')
ns={'__file__':str(A53),'__name__':'__main__'}
exec(compile(a,str(A53),'exec'),ns,ns)
road=text(JAVA/'settlement/SettlementRoadService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json')
must(road,('MAX_TUNNEL_SPAN = 24','MIN_TUNNEL_SPAN = 3','MAX_TUNNEL_BENDS = 1','MIN_BENT_TUNNEL_LEG = 3',
           'TUNNEL_PORTAL_HALF_WIDTH = 2','TUNNEL_PORTAL_HEIGHT = 4','TUNNEL_PORTAL_FRAME_BLOCKS = 22',
           'boolean tunnel, boolean portal','tunnelTurnCount(','bentTunnelLegsLongEnough(','tunnelBendCount(',
           'tunnelRunCount(','tunnelPortalFrameAt(','tunnelPortalFramePositions(','validateTunnelPortals(',
           'tunnelPortalCellSafe(','placement.portal()','tunnelPortalApproach(',
           'tunnelRunCount(candidate.profile()) * TUNNEL_PORTAL_FRAME_BLOCKS',
           'tunnelRunCount(road.profile()) * TUNNEL_PORTAL_FRAME_BLOCKS',
           'new Placement(portal, Blocks.STONE_BRICKS.defaultBlockState(), false, false, true, true)',
           'turns > MAX_TUNNEL_BENDS','!bentTunnelLegsLongEnough(flat, i, tunnelEnd)',
           '터널 석재 포털 범위에 광석·유체·컨테이너·플레이어/비자연 블록이 있습니다.'), 'alpha.54 bent tunnel/portal authority')
forbid(road,('destroyBlock(','dropResources(','forceChunk','setChunkForced','teleportTo('),'alpha.54 safety')
must(props,('mod_version=0.1.0-alpha.54','physical save-compatible straight tunnel excavation','bounded one-bend tunnel public works'),'alpha.54 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.54"','Alpha.54 keeps the same 24-cell tunnel ceiling','STONE_BRICKS portal frames','no new logistics or companion dependency','"status": "candidate_runtime_lock"'),'alpha.54 lock')
print('Frontier Settlement alpha.23-54 cumulative source audit: PASS')
