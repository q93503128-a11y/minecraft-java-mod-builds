#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A59=ROOT/'tools/test_alpha59_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
a=text(A59).replace("print('Frontier Settlement alpha.23-59 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.59','0.1.0-alpha.60'); ns={'__file__':str(A59),'__name__':'__main__'}; exec(compile(a,str(A59),'exec'),ns,ns)
service=text(JAVA/'settlement/SettlementConstructionService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json'); building=text(JAVA/'settlement/BuildingType.java')
must(service,('private record BlockSnapshot(BlockPos pos, BlockState state)','SettlementInventory.countWood(crate) < woodDelta','SettlementInventory.countStone(crate) < stoneDelta','boolean placedNow = false','if (!level.setBlock(target, placement.state(), NORMAL_BLOCK_UPDATE)) return false;','if (!SettlementInventory.consume(crate, woodDelta, stoneDelta, 0L)) {','if (placedNow) level.setBlock(target, current, NORMAL_BLOCK_UPDATE);','if (placedNow) builder.swing(InteractionHand.MAIN_HAND);'),'alpha.60 ordinary block transaction')
place=service.find('if (!level.setBlock(target, placement.state(), NORMAL_BLOCK_UPDATE)) return false;'); consume=service.find('if (!SettlementInventory.consume(crate, woodDelta, stoneDelta, 0L)) {',place); advance=service.find('data.advanceConstruction();',consume)
if min(place,consume,advance)<0 or not (place < consume < advance): raise SystemExit('alpha.60 new building block must place -> consume -> advance')
must(service,('List<BlockSnapshot> gradeMutation = applyGradeCellTransactional','SettlementInventory.countStone(terrainCrate) < cell.retainingStone()','if (gradeMutation == null) return false;','rollbackGradeMutation(level, gradeMutation);','private static List<BlockSnapshot> applyGradeCellTransactional','private static boolean setGradeBlock','if (!level.setBlock(pos, next, DIRECT_BLOCK_UPDATE)) return false;','private static void rollbackGradeMutation'),'alpha.60 grade transaction')
grade_apply=service.find('List<BlockSnapshot> gradeMutation = applyGradeCellTransactional'); grade_consume=service.find('SettlementInventory.consume(terrainCrate',grade_apply); grade_advance=service.find('data.advanceConstruction();',grade_consume)
if min(grade_apply,grade_consume,grade_advance)<0 or not (grade_apply < grade_consume < grade_advance): raise SystemExit('alpha.60 grade must mutate -> retaining consume -> advance')
forbid(service,('destroyBlock(','dropResources(','forceChunk','setChunkForced','teleportTo('),'alpha.60 construction keeps no-drop/no-force-load authority')
enum_block=building.split('public enum BuildingType {',1)[1].split(';',1)[0]; actual=[line.strip().split('(',1)[0] for line in enum_block.splitlines() if '(' in line]; expected=['HOUSE','LUMBER_CAMP','FARM','QUARRY','MINE','WAREHOUSE','CONSTRUCTION_OFFICE','BLACKSMITH','WORKSHOP','ADVANCED_WORKSHOP','GUARD_POST','WATCHTOWER','BARRACKS','MARKET','CART_STATION']
if actual!=expected: raise SystemExit(f'alpha.60 expected exact 15 functional building families, got: {actual}')
must(props,('mod_version=0.1.0-alpha.60','rollback-safe ordinary building and terrain material transactions'),'alpha.60 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.60"','Alpha.60 makes ordinary building placement and Alpha.44 terrain grading transaction-safe','unexpected post-placement consumption failure rolls the newly changed blocks back','"status": "candidate_runtime_lock"'),'alpha.60 lock')
print('Frontier Settlement alpha.23-60 cumulative source audit: PASS')
