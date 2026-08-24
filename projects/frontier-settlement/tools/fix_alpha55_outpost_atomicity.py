#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ROAD = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementOutpostService.java'
AUDIT = ROOT / 'tools/test_alpha55_source.py'
README = ROOT / 'README.md'
CANONICAL = ROOT / 'CANONICAL_PLAN.md'
GAP = ROOT / 'COMPLETION_GAP_AUDIT.md'


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding='utf-8')
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{path}: expected one patch anchor, found {count}: {old[:120]!r}')
    path.write_text(text.replace(old, new, 1), encoding='utf-8')

# Legacy prepaid placement must not advance on a failed world mutation.
replace_once(ROAD,
'''        level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE);
        builder.swing(InteractionHand.MAIN_HAND);
        data.advanceOutpostConstruction();
        return false;
    }

    private static boolean tickPhysicalBuilding''',
'''        if (!level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)) return false;
        builder.swing(InteractionHand.MAIN_HAND);
        data.advanceOutpostConstruction();
        return false;
    }

    private static boolean tickPhysicalBuilding''')

# Physical building: successful world placement first, then actual carried ItemStack consume, rollback on unexpected consume failure.
replace_once(ROAD,
'''        if (predicate != null && !ensureBuildMaterial(server, data, builder, predicate, requiredNow, remainingCost)) {
            return false;
        }
        if (requiredNow > 0L && !consumeCarried(builder, predicate, requiredNow)) return false;

        level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE);
        builder.swing(InteractionHand.MAIN_HAND);
        data.advanceOutpostConstruction();''',
'''        if (predicate != null && !ensureBuildMaterial(server, data, builder, predicate, requiredNow, remainingCost)) {
            return false;
        }

        if (!level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)) return false;
        if (requiredNow > 0L && !consumeCarried(builder, predicate, requiredNow)) {
            level.setBlock(placement.pos(), current, DIRECT_BLOCK_UPDATE);
            return false;
        }
        builder.swing(InteractionHand.MAIN_HAND);
        data.advanceOutpostConstruction();''')

# Final validation: Alpha.26+ physical projects pay real material for missing priced blocks; old prepaid saves are not charged twice.
replace_once(ROAD,
'''        ServerLevel level = server.overworld();
        for (OutpostBlueprints.Placement placement : plan) {
            BlockState current = level.getBlockState(placement.pos());
            if (current.is(placement.state().getBlock())) continue;
            if (!canReplaceForBlueprint(level, placement.pos(), current)) {
                builder.getNavigation().stop();
                return false;
            }
            if (!moveBuilderToCurrentSurface(level, builder, placement.pos())) return false;
            if (level.getGameTime() % WORK_INTERVAL_TICKS != 0L) return false;
            level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE);
            builder.swing(InteractionHand.MAIN_HAND);
            return false;
        }''',
'''        ServerLevel level = server.overworld();
        boolean legacyPrepaidRepair = state.legacyPrepaidBuilding();
        for (OutpostBlueprints.Placement placement : plan) {
            BlockState current = level.getBlockState(placement.pos());
            if (current.is(placement.state().getBlock())) continue;
            if (!canReplaceForBlueprint(level, placement.pos(), current)) {
                builder.getNavigation().stop();
                return false;
            }
            Predicate<ItemStack> repairPredicate = isWoodPlacement(placement.state()) ? SettlementInventory::isWood
                    : isStonePlacement(placement.state()) ? SettlementInventory::isStone : null;
            if (!legacyPrepaidRepair && repairPredicate != null
                    && !ensureBuildMaterial(server, data, builder, repairPredicate, 1L, 1L)) return false;
            if (!moveBuilderToCurrentSurface(level, builder, placement.pos())) return false;
            if (level.getGameTime() % WORK_INTERVAL_TICKS != 0L) return false;
            if (!level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)) return false;
            if (!legacyPrepaidRepair && repairPredicate != null && !consumeCarried(builder, repairPredicate, 1L)) {
                level.setBlock(placement.pos(), current, DIRECT_BLOCK_UPDATE);
                return false;
            }
            builder.swing(InteractionHand.MAIN_HAND);
            return false;
        }''')

# Cumulative audit adds explicit atomicity/legacy compatibility checks while retaining all Alpha.23-54 checks.
replace_once(AUDIT,
'''must(out,('WOOD_COST = 72L','STONE_COST = 48L','detectSpecialization(ServerLevel level, BlockPos center, SettlementData data)','SettlementExplorationBenefitService.outpostWoodCost(data)','SettlementExplorationBenefitService.outpostStoneCost(data)','ores += SettlementExplorationBenefitService.oreEvidenceBonus(data)','logs += SettlementExplorationBenefitService.logEvidenceBonus(data)','fieldGround += SettlementExplorationBenefitService.fieldEvidenceBonus(data)','exposedStone += SettlementExplorationBenefitService.stoneEvidenceBonus(data)','materialCostDelta(plan, step, true, totalWoodCost)','materialCostDelta(plan, step, false, totalStoneCost)'), 'alpha.55 existing outpost integration')''',
'''must(out,('WOOD_COST = 72L','STONE_COST = 48L','detectSpecialization(ServerLevel level, BlockPos center, SettlementData data)','SettlementExplorationBenefitService.outpostWoodCost(data)','SettlementExplorationBenefitService.outpostStoneCost(data)','ores += SettlementExplorationBenefitService.oreEvidenceBonus(data)','logs += SettlementExplorationBenefitService.logEvidenceBonus(data)','fieldGround += SettlementExplorationBenefitService.fieldEvidenceBonus(data)','exposedStone += SettlementExplorationBenefitService.stoneEvidenceBonus(data)','materialCostDelta(plan, step, true, totalWoodCost)','materialCostDelta(plan, step, false, totalStoneCost)',
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
    raise SystemExit('alpha.55 physical outpost repair must fetch/place/consume; legacy prepaid stays exempt')''')

replace_once(README,
'''- each conquest level reduces a **new** outpost's physical construction total by only 4 wood + 2 stone, capped at 8 wood + 4 stone; the builder still walks from real loaded settlement storage and consumes actual ItemStacks through the existing outpost construction authority;''',
'''- each conquest level reduces a **new** outpost's physical construction total by only 4 wood + 2 stone, capped at 8 wood + 4 stone; the builder still walks from real loaded settlement storage and consumes actual ItemStacks through the existing outpost construction authority;
- Alpha.55 also closes a physical-authority hole in that existing outpost builder: world placement must succeed before carried wood/stone is consumed and the step advances, with rollback on an unexpected consume failure; final repair of missing priced blocks uses real material for Alpha.26+ physical projects, while historical prepaid saves keep their already-paid repair semantics;''')

replace_once(CANONICAL,
'''- those effective totals are used by placement approval and by the existing builder's actual ItemStack extraction/consumption math, so the discount cannot become a virtual refund or free construction;''',
'''- those effective totals are used by placement approval and by the existing builder's actual ItemStack extraction/consumption math, so the discount cannot become a virtual refund or free construction;
- outpost physical placement is atomic: successful world `setBlock` precedes carried wood/stone consumption and state advance, with rollback on unexpected consume failure;
- Alpha.26+ physical outpost final repair fetches and consumes a real wood/stone item for missing priced blueprint cells; historical prepaid saves remain repair-cost exempt to avoid double charging;''')

replace_once(GAP,
'''- base72/48, 최저64/44이며 placement 승인과 actual builder ItemStack consume가 같은 effective cost를 사용;''',
'''- base72/48, 최저64/44이며 placement 승인과 actual builder ItemStack consume가 같은 effective cost를 사용;
- physical outpost는 successful setBlock → carried material consume → state advance, consume 실패 rollback;
- Alpha.26+ missing priced blueprint final repair는 실제 wood/stone 1개를 fetch/place/consume하고 historical prepaid save는 이중과금하지 않음;''')

print('Applied Alpha.55 outpost physical-material atomicity and legacy-prepaid repair compatibility fix.')
