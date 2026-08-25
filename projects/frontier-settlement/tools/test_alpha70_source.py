#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A69=ROOT/'tools/test_alpha69_source.py'
_real=Path.read_text
def legacy_view(self,*args,**kwargs):
    s=_real(self,*args,**kwargs)
    if self.name=='gradle.properties': s=s.replace('mod_version=0.1.0-alpha.70','mod_version=0.1.0-alpha.69')
    elif self.name=='COMPANION_LOCK.json': s=s.replace('"frontier_settlement": "0.1.0-alpha.70"','"frontier_settlement": "0.1.0-alpha.69"')
    return s
Path.read_text=legacy_view
try:
    a=_real(A69,encoding='utf-8').replace("print('Frontier Settlement alpha.23-69 cumulative source audit: PASS')",'pass')
    ns={'__file__':str(A69),'__name__':'__main__'}; exec(compile(a,str(A69),'exec'),ns,ns)
finally: Path.read_text=_real
def text(p): return Path(p).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
outpost=text(JAVA/'settlement/SettlementOutpostProductionService.java'); workers=text(JAVA/'settlement/SettlementWorkerService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json')
must(outpost,('private static List<Villager> findAssignedWorkers(ServerLevel level, OutpostRecord outpost)','assigned.sort(Comparator.comparing(villager -> villager.getUUID().toString()))','private static AABB assignmentBounds(OutpostRecord outpost)','private static boolean assignmentEvidenceLoaded(ServerLevel level, OutpostRecord outpost)','Math.floorDiv((int) Math.floor(bounds.minX), 16)','level.hasChunkAt(probe)','if (!assignmentEvidenceLoaded(level, outpost)) return null;','legacy.sort(Comparator.comparing(villager -> villager.getUUID().toString()))','if (!level.addFreshEntity(worker)) return null;','if (level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3)) harvested++;','if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) break;','if (level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) count++;','if (result.isEmpty() || !level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3)) return ItemStack.EMPTY;'),'alpha.70 outpost lifecycle/transactions')
must(workers,('SettlementOutpostProductionService.PRODUCTION_WORKER_TAG','value.startsWith("전초 벌목 주민 #")','value.startsWith("전초 농업 주민 #")','if (level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), 3)) harvested++;','if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) break;','if (level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) count++;','if (result.isEmpty() || !level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3)) return ItemStack.EMPTY;','event.getDrops().clear()','carried.copy()'),'alpha.70 managed production cargo/transactions')
ensure=outpost.index('private static Villager ensureWorker'); evidence=outpost.index('if (!assignmentEvidenceLoaded(level, outpost)) return null;',ensure); legacy=outpost.index('List<Villager> legacy =',evidence); create=outpost.index('Villager worker = new Villager',legacy)
if not ensure < evidence < legacy < create: raise SystemExit('alpha.70 absence evidence does not gate migration/spawn')
forbid(outpost+workers,('OUTPOST_PRODUCTION_WORKER_LEDGER','PRODUCTION_CARGO_LEDGER','RECOVERY_BALANCE','setChunkForced','forceChunk','teleportTo('),'alpha.70 no virtual/load authority')
must(props,('mod_version=0.1.0-alpha.70','fail-closed specialized-outpost production-worker absence evidence'),'alpha.70 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.70"','Alpha.70 hardens specialized outpost production'),'alpha.70 lock')
print('Frontier Settlement alpha.23-70 cumulative source audit: PASS')
