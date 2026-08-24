#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

# The legacy Alpha.37 cumulative audit used the old helper name as evidence that physical grading
# existed. Alpha.60 replaces that helper with the stronger reversible transaction implementation.
legacy_path = ROOT / 'tools/test_current_source.py'
legacy = legacy_path.read_text(encoding='utf-8')
old_legacy = "'tickGrading(', 'createGradePlan(', 'canGradeCell(', 'applyGradeCell(', 'ConstructionState.BUILD_STEP_OFFSET',"
new_legacy = "'tickGrading(', 'createGradePlan(', 'canGradeCell(', 'applyGradeCellTransactional(', 'ConstructionState.BUILD_STEP_OFFSET',"
if old_legacy not in legacy:
    raise SystemExit('legacy physical construction helper-name anchor missing')
legacy = legacy.replace(old_legacy, new_legacy, 1)
legacy_path.write_text(legacy, encoding='utf-8')

# Alpha.44 historically required retaining stone consumption before grade placement. Alpha.60
# intentionally reverses commit order while preserving physical staging/consumption and adding rollback.
path = ROOT / 'tools/test_alpha44_source.py'
source = path.read_text(encoding='utf-8')
old_token = "    'SettlementInventory.consume(crate, 0L, cell.retainingStone(), 0L)',\n"
new_token = "    'cell.retainingStone()',\n"
if old_token not in source:
    raise SystemExit('alpha.44 retaining consume token anchor missing')
source = source.replace(old_token, new_token, 1)

old_order = '''# Cobblestone retaining/foundation blocks must never be free: every retaining cell stages and consumes real stone first.\nconsume_pos = construction.find('SettlementInventory.consume(crate, 0L, cell.retainingStone(), 0L)')\napply_pos = construction.find('applyGradeCell(level, construction, type, cell)')\nif consume_pos < 0 or apply_pos < 0 or consume_pos > apply_pos:\n    raise SystemExit('alpha.44 retaining stone is not consumed before physical grade placement')\n'''
new_order = '''# Alpha.60 intentionally supersedes Alpha.44's historical consume-before-placement ordering.\n# The historical invariant that retaining/foundation stone is real remains, while the current\n# transaction is stronger: reversible world mutation succeeds before retaining ItemStack commit.\nif 'stageTerrainStone(server, data, builder, crate, supply, cell.retainingStone())' not in construction:\n    raise SystemExit('alpha.44 retaining stone is no longer physically staged')\nif 'SettlementInventory.consume(terrainCrate, 0L, cell.retainingStone(), 0L)' not in construction:\n    raise SystemExit('alpha.44 retaining stone physical consume disappeared')\nif 'applyGradeCellTransactional(level, construction, type, cell)' not in construction:\n    raise SystemExit('alpha.60 transactional grade placement missing while superseding Alpha.44 ordering')\n'''
if old_order not in source:
    raise SystemExit('alpha.44 retaining ordering anchor missing')
source = source.replace(old_order, new_order, 1)
path.write_text(source, encoding='utf-8')
print('Updated legacy/Alpha.44 cumulative audits for Alpha.60 transaction supersession.')
