#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A60=ROOT/'tools/test_alpha60_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
a=text(A60).replace("print('Frontier Settlement alpha.23-60 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.60','0.1.0-alpha.61'); ns={'__file__':str(A60),'__name__':'__main__'}; exec(compile(a,str(A60),'exec'),ns,ns)
outpost=text(JAVA/'settlement/SettlementOutpostService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json'); building=text(JAVA/'settlement/BuildingType.java')
must(outpost,('private record BlockSnapshot(BlockPos pos, BlockState state)','if (!applyGradeCell(level, target)) return false;','private static boolean applyGradeCell(ServerLevel level, BlockPos target)','List<BlockSnapshot> changed = new ArrayList<>()','private static boolean setGradeBlock','if (!level.setBlock(pos, next, DIRECT_BLOCK_UPDATE)) return false;','private static void rollbackGradeMutation','for (int i = changed.size() - 1; i >= 0; i--)','if (level.hasChunkAt(snapshot.pos()))'),'alpha.61 outpost grade transaction')
apply=outpost.find('if (!applyGradeCell(level, target)) return false;'); advance=outpost.find('data.advanceOutpostConstruction();',apply)
if min(apply,advance)<0 or not apply < advance: raise SystemExit('alpha.61 outpost grade must succeed before step advance')
forbid(outpost,('destroyBlock(','dropResources(','forceChunk','setChunkForced','teleportTo('),'alpha.61 outpost grading keeps no-drop/no-force-load authority')
enum_block=building.split('public enum BuildingType {',1)[1].split(';',1)[0]; actual=[line.strip().split('(',1)[0] for line in enum_block.splitlines() if '(' in line]; expected=['HOUSE','LUMBER_CAMP','FARM','QUARRY','MINE','WAREHOUSE','CONSTRUCTION_OFFICE','BLACKSMITH','WORKSHOP','ADVANCED_WORKSHOP','GUARD_POST','WATCHTOWER','BARRACKS','MARKET','CART_STATION']
if actual!=expected: raise SystemExit(f'alpha.61 expected exact 15 functional building families, got: {actual}')
must(props,('mod_version=0.1.0-alpha.61','rollback-safe outpost grading cells'),'alpha.61 props')
must(lock,('\"frontier_settlement\": \"0.1.0-alpha.61\"','Alpha.61 makes outpost grading rollback-safe','advances the persisted outpost step only after the complete grade cell succeeds','\"status\": \"candidate_runtime_lock\"'),'alpha.61 lock')
print('Frontier Settlement alpha.23-61 cumulative source audit: PASS')
