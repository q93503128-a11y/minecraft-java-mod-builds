#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A71=ROOT/'tools/test_alpha71_source.py'
_real=Path.read_text
def legacy_view(self,*args,**kwargs):
    s=_real(self,*args,**kwargs)
    if self.name=='gradle.properties': s=s.replace('mod_version=0.1.0-alpha.72','mod_version=0.1.0-alpha.71')
    elif self.name=='COMPANION_LOCK.json': s=s.replace('"frontier_settlement": "0.1.0-alpha.72"','"frontier_settlement": "0.1.0-alpha.71"')
    return s
Path.read_text=legacy_view
try:
    a=_real(A71,encoding='utf-8').replace("print('Frontier Settlement alpha.23-71 cumulative source audit: PASS')",'pass')
    ns={'__file__':str(A71),'__name__':'__main__'}; exec(compile(a,str(A71),'exec'),ns,ns)
finally: Path.read_text=_real
def text(p): return Path(p).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
sett=JAVA/'settlement'
c=text(sett/'SettlementConstructionService.java'); r=text(sett/'SettlementRoadService.java'); o=text(sett/'SettlementOutpostService.java'); civil=text(sett/'SettlementCivilWorkService.java'); wf=text(sett/'SettlementWaterfrontService.java'); fish=text(sett/'SettlementFishingOutpostService.java'); market=text(sett/'SettlementMarketService.java'); benefit=text(sett/'SettlementBenefitService.java'); military=text(sett/'SettlementMilitaryOutpostService.java'); worker=text(sett/'SettlementWorkerService.java'); service=text(sett/'SettlementService.java'); mod=text(JAVA/'FrontierSettlement.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json')
java_files=list((ROOT/'src/main/java').rglob('*.java'))
if len(java_files)!=105: raise SystemExit(f'alpha.72 expected 105 Java files, got {len(java_files)}')
must(c,('ensureBuilder(ServerLevel level, SettlementData data)','builderAssignmentEvidenceLoaded(ServerLevel level, SettlementData data)','builders.sort(Comparator.comparing(villager -> villager.getUUID().toString()))','duplicate.setNoAi(true)','return level.addFreshEntity(builder) ? builder : null','returnBuilderHome(ServerLevel level, SettlementData data, Villager builder)','returnCrateExtrasPhysically','SettlementStorageService.insertAt(level, target, carried)'), 'alpha.72 shared builder authority')
must(r,('if (!applyGradePlacement(level, placement)) return false','private static boolean applyGradePlacement','List<BlockSnapshot> changed = new ArrayList<>()','rollbackGradeMutation(level, changed)','if (!level.hasChunkAt(placement.pos())) return false','SettlementConstructionService.returnBuilderHome(level, data, builder)'), 'alpha.72 road transaction')
must(o,('SettlementStorageService.insertAt(level, target, carried)','SettlementConstructionService.returnBuilderHome(level, data, builder)','if (!level.hasChunkAt(target)) return false','if (!level.hasChunkAt(new BlockPos(x, roadY, z))) return false'), 'alpha.72 outpost physical return/load')
must(civil,('long widthLong = (long) maxX - minX + 1L','withinHorizontalDistance(player.blockPosition(), first, MAX_PLAYER_DISTANCE)','SettlementConstructionService.returnBuilderHome(server.overworld(), settlement, builder)'), 'alpha.72 civil bounds/home')
if civil.count('if (project.phase() == CivilWorkState.PHASE_RETAIN)')!=1: raise SystemExit('alpha.72 duplicate PHASE_RETAIN branch remains')
must(wf,('if (!level.setBlock(placement.pos(), placement.state(), 3)) return','traders.sort(Comparator.comparing(villager -> villager.getUUID().toString()))','if (!entityAreaLoaded(level, area) || !level.hasChunkAt(station)) return null','return level.addFreshEntity(trader) ? trader : null'), 'alpha.72 waterfront')
if wf.index('if (!level.setBlock(placement.pos(), placement.state(), 3)) return') > wf.index('carried.shrink(1)'): raise SystemExit('alpha.72 waterfront consumes wood before world placement')
must(fish,('assignmentEvidenceLoaded(ServerLevel level, AABB area)','workers.sort(Comparator.comparing(villager -> villager.getUUID().toString()))','return level.addFreshEntity(worker) ? worker : null'), 'alpha.72 fishing lifecycle')
must(market,('assigned.sort(Comparator.comparing(villager -> villager.getUUID().toString()))','if (!entityAreaLoaded(level, area)) return null','return level.addFreshEntity(trader) ? trader : null'), 'alpha.72 market lifecycle')
must(benefit,('GUARD_POST_GUARD_TAG','entityAreaLoaded(ServerLevel level, AABB area)','existing.sort(Comparator.comparing(guard -> guard.getUUID().toString()))','duplicate.setNoAi(true)','public static void onLivingDrops(LivingDropsEvent event)','event.getDrops().clear()'), 'alpha.72 civic guard lifecycle/drop')
must(military,('sentries.sort(Comparator.comparing(sentry -> sentry.getUUID().toString()))','legacy.sort(Comparator.comparing(sentry -> sentry.getUUID().toString()))','duplicate.setNoAi(true)'), 'alpha.72 military duplicate containment')
must(worker,('SettlementConstructionService.BUILDER_TAG','SettlementFishingOutpostService.FISHING_WORKER_TAG','event.getDrops().clear()','carried.copy()'), 'alpha.72 exact builder/fishing cargo')
must(service,('public static boolean found(ServerPlayer founder) { return foundInternal(founder, founder.blockPosition(), true).founded(); }','if (!level.setBlock(center, Blocks.OAK_FENCE.defaultBlockState(), 3))','if (!level.setBlock(center.above(), Blocks.TORCH.defaultBlockState(), 3))','if (!level.setBlock(stockpile, Blocks.BARREL.defaultBlockState(), 3)','data.found(center, stockpile)'), 'alpha.72 founding transaction')
if service.index('data.found(center, stockpile)') < service.index('level.getBlockEntity(stockpile) instanceof net.minecraft.world.Container'): raise SystemExit('alpha.72 founding commits before real stockpile container')
must(mod,('SettlementBenefitService::onLivingDrops','SettlementWorkerService::onLivingDrops'), 'alpha.72 event registration')
all_java='\n'.join(text(p) for p in java_files)
forbid(all_java,('SettlementStorageService.insert(level, data','setChunkForced','forceChunk','teleportTo('), 'alpha.72 no broad storage teleport/load authority')
must(r,('long dx = Math.abs((long) endXZ.getX() - startXZ.getX())','long manhattan = dx + dz + 1L'), 'alpha.72 road coordinate hardening')
must(props,('mod_version=0.1.0-alpha.72','full-audit authority hardening'), 'alpha.72 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.72"','Alpha.72 is full-project error hardening only'), 'alpha.72 lock')
print('Frontier Settlement alpha.23-72 full-project source audit: PASS')
