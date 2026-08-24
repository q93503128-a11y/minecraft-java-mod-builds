#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A54=ROOT/'tools/test_alpha54_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
a=text(A54).replace("print('Frontier Settlement alpha.23-54 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.54','0.1.0-alpha.55'); ns={'__file__':str(A54),'__name__':'__main__'}; exec(compile(a,str(A54),'exec'),ns,ns)
benefit=text(JAVA/'settlement/SettlementExplorationBenefitService.java'); out=text(JAVA/'settlement/SettlementOutpostService.java'); cmd=text(JAVA/'command/SettlementCommands.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json')
must(benefit,('MAX_SURVEY_LEVEL = 3','MAX_CONQUEST_LEVEL = 2','OUTPOST_WOOD_DISCOUNT_PER_CONQUEST = 4L','OUTPOST_STONE_DISCOUNT_PER_CONQUEST = 2L','data.discoveredExternalStructures().size()','data.defeatedExternalBosses().size()','SettlementOutpostService.WOOD_COST - conquestLevel(data)','SettlementOutpostService.STONE_COST - conquestLevel(data)'), 'alpha.55 deterministic exploration benefit')
forbid(benefit,('ItemStack','setBlock','addPopulation','updateResources','forceChunk','teleportTo'),'alpha.55 benefit must not become authority')
must(out,('WOOD_COST = 72L','STONE_COST = 48L','detectSpecialization(ServerLevel level, BlockPos center, SettlementData data)','SettlementExplorationBenefitService.outpostWoodCost(data)','SettlementExplorationBenefitService.outpostStoneCost(data)','ores += SettlementExplorationBenefitService.oreEvidenceBonus(data)','logs += SettlementExplorationBenefitService.logEvidenceBonus(data)','fieldGround += SettlementExplorationBenefitService.fieldEvidenceBonus(data)','exposedStone += SettlementExplorationBenefitService.stoneEvidenceBonus(data)','materialCostDelta(plan, step, true, totalWoodCost)','materialCostDelta(plan, step, false, totalStoneCost)',
          'if (!level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)) return false;',
          'level.setBlock(placement.pos(), current, DIRECT_BLOCK_UPDATE);',
          'boolean legacyPrepaidRepair = state.legacyPrepaidBuilding()',
          '!legacyPrepaidRepair && repairPredicate != null',
          'ensureBuildMaterial(server, data, builder, repairPredicate, 1L, 1L)',
          'consumeCarried(builder, repairPredicate, 1L)'), 'alpha.55 existing outpost integration')
physical = out.find('private static boolean tickPhysicalBuilding')
place = out.find('if (!level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)) return false;', physical)
consume = out.find('if (requiredNow > 0L && !consumeCarried(builder, predicate, requiredNow))', place)
advance = out.find('data.advanceOutpostConstruction();', consume)
if min(physical, place, consume, advance) < 0 or not (physical < place < consume < advance):
    raise SystemExit('alpha.55 physical outpost must place successfully before carried consume/state advance')
finish = out.find('private static boolean finishIfValid')
repair_fetch = out.find('ensureBuildMaterial(server, data, builder, repairPredicate, 1L, 1L)', finish)
repair_place = out.find('if (!level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)) return false;', repair_fetch)
repair_consume = out.find('consumeCarried(builder, repairPredicate, 1L)', repair_place)
if min(finish, repair_fetch, repair_place, repair_consume) < 0 or not (finish < repair_fetch < repair_place < repair_consume):
    raise SystemExit('alpha.55 physical outpost repair must fetch/place/consume; legacy prepaid stays exempt')
must(cmd,('개척 지식 | 정찰 ','SettlementExplorationBenefitService.surveyLevel(data)','SettlementExplorationBenefitService.conquestLevel(data)','반복 발견/정복 보너스 없음'),'alpha.55 compact status')
must(props,('mod_version=0.1.0-alpha.55','capped non-farmable exploration knowledge'),'alpha.55 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.55"','Alpha.55 derives a capped survey level','at most 8 wood and 4 stone','no new currency, loot minting, save authority or logistics controller','"status": "candidate_runtime_lock"'),'alpha.55 lock')
print('Frontier Settlement alpha.23-55 cumulative source audit: PASS')
