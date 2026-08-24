#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'


def text(path):
    return path.read_text(encoding='utf-8')


def write(path, content):
    path.write_text(content, encoding='utf-8')


def repl(path, old, new):
    source = text(path)
    if old not in source:
        raise SystemExit(f'missing replacement anchor in {path}: {old[:140]!r}')
    write(path, source.replace(old, new, 1))

service = JAVA / 'settlement/SettlementConstructionService.java'

# Ordinary building placement: verify material availability, mutate world, then commit ItemStack cost/state.
repl(service,
'''        long woodDelta = costAtStep(type.woodCost(), buildStep + 1, plan.size())
                - costAtStep(type.woodCost(), buildStep, plan.size());
        long stoneDelta = costAtStep(type.stoneCost(), buildStep + 1, plan.size())
                - costAtStep(type.stoneCost(), buildStep, plan.size());
        if (!SettlementInventory.consume(crate, woodDelta, stoneDelta, 0L)) return false;

        if (current.isAir()) {
            level.setBlock(target, placement.state(), NORMAL_BLOCK_UPDATE);
            builder.swing(InteractionHand.MAIN_HAND);
        }
        data.advanceConstruction();''',
'''        long woodDelta = costAtStep(type.woodCost(), buildStep + 1, plan.size())
                - costAtStep(type.woodCost(), buildStep, plan.size());
        long stoneDelta = costAtStep(type.stoneCost(), buildStep + 1, plan.size())
                - costAtStep(type.stoneCost(), buildStep, plan.size());
        if (SettlementInventory.countWood(crate) < woodDelta || SettlementInventory.countStone(crate) < stoneDelta) {
            return false;
        }

        boolean placedNow = false;
        if (current.isAir()) {
            if (!level.setBlock(target, placement.state(), NORMAL_BLOCK_UPDATE)) return false;
            placedNow = true;
        }
        if (!SettlementInventory.consume(crate, woodDelta, stoneDelta, 0L)) {
            if (placedNow) level.setBlock(target, current, NORMAL_BLOCK_UPDATE);
            return false;
        }
        if (placedNow) builder.swing(InteractionHand.MAIN_HAND);
        data.advanceConstruction();''')

# Grading: material availability is checked before mutation, but actual stone is committed only after
# the full grade-cell mutation succeeds. Any failed placement or unexpected consume failure rolls back.
repl(service,
'''        if (cell.retainingStone() > 0) {
            BlockPos supply = supplyPosition(construction.origin(), type, construction.buildingRotation());
            Container crate = ensureSupplyCrate(level, supply);
            if (crate == null || !SettlementInventory.consume(crate, 0L, cell.retainingStone(), 0L)) return false;
            SettlementService.refreshResources(server, data);
            SettlementService.broadcast(server, data);
        }
        applyGradeCell(level, construction, type, cell);
        builder.swing(InteractionHand.MAIN_HAND);
        data.advanceConstruction();''',
'''        Container terrainCrate = null;
        if (cell.retainingStone() > 0) {
            BlockPos supply = supplyPosition(construction.origin(), type, construction.buildingRotation());
            terrainCrate = ensureSupplyCrate(level, supply);
            if (terrainCrate == null || SettlementInventory.countStone(terrainCrate) < cell.retainingStone()) return false;
        }

        List<BlockSnapshot> gradeMutation = applyGradeCellTransactional(level, construction, type, cell);
        if (gradeMutation == null) return false;
        if (cell.retainingStone() > 0 && !SettlementInventory.consume(terrainCrate, 0L, cell.retainingStone(), 0L)) {
            rollbackGradeMutation(level, gradeMutation);
            return false;
        }
        if (cell.retainingStone() > 0) {
            SettlementService.refreshResources(server, data);
            SettlementService.broadcast(server, data);
        }
        builder.swing(InteractionHand.MAIN_HAND);
        data.advanceConstruction();''')

repl(service,
'''    private record GradeCell(BlockPos floor, boolean foundation, int retainingStone) {}
    private record ScaffoldPiece(BlockPos pos, BlockState state) {}''',
'''    private record GradeCell(BlockPos floor, boolean foundation, int retainingStone) {}
    private record BlockSnapshot(BlockPos pos, BlockState state) {}
    private record ScaffoldPiece(BlockPos pos, BlockState state) {}''')

old_apply = '''    private static void applyGradeCell(ServerLevel level, ConstructionState construction,
                                       BuildingType type, GradeCell cell) {
        BlockPos column = cell.floor().above();
        for (int y = type.clearHeight(); y >= 0; y--) {
            BlockPos pos = column.above(y);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), DIRECT_BLOCK_UPDATE);
        }
        if (!cell.foundation()) return;

        BlockState fill = cell.retainingStone() > 0
                ? Blocks.COBBLESTONE.defaultBlockState()
                : Blocks.COARSE_DIRT.defaultBlockState();
        level.setBlock(cell.floor(), fill, DIRECT_BLOCK_UPDATE);
        for (int depth = 1; depth <= MAX_GRADE_FILL_DEPTH; depth++) {
            BlockPos support = cell.floor().below(depth);
            BlockState state = level.getBlockState(support);
            if (!state.isAir() && !state.canBeReplaced()) break;
            level.setBlock(support, fill, DIRECT_BLOCK_UPDATE);
        }
    }
'''
new_apply = '''    /**
     * Applies one grade cell as a reversible world transaction. The caller commits retaining stone
     * only after this returns a non-null snapshot list. A null result means every successful partial
     * mutation was rolled back and neither material nor construction step may advance.
     */
    private static List<BlockSnapshot> applyGradeCellTransactional(ServerLevel level, ConstructionState construction,
                                                                    BuildingType type, GradeCell cell) {
        List<BlockSnapshot> changed = new ArrayList<>();
        BlockPos column = cell.floor().above();
        for (int y = type.clearHeight(); y >= 0; y--) {
            BlockPos pos = column.above(y);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            if (!setGradeBlock(level, pos, Blocks.AIR.defaultBlockState(), changed)) {
                rollbackGradeMutation(level, changed);
                return null;
            }
        }
        if (!cell.foundation()) return List.copyOf(changed);

        BlockState fill = cell.retainingStone() > 0
                ? Blocks.COBBLESTONE.defaultBlockState()
                : Blocks.COARSE_DIRT.defaultBlockState();
        if (!setGradeBlock(level, cell.floor(), fill, changed)) {
            rollbackGradeMutation(level, changed);
            return null;
        }
        for (int depth = 1; depth <= MAX_GRADE_FILL_DEPTH; depth++) {
            BlockPos support = cell.floor().below(depth);
            BlockState state = level.getBlockState(support);
            if (!state.isAir() && !state.canBeReplaced()) break;
            if (!setGradeBlock(level, support, fill, changed)) {
                rollbackGradeMutation(level, changed);
                return null;
            }
        }
        return List.copyOf(changed);
    }

    private static boolean setGradeBlock(ServerLevel level, BlockPos pos, BlockState next,
                                         List<BlockSnapshot> changed) {
        BlockState current = level.getBlockState(pos);
        if (current.equals(next)) return true;
        if (!level.setBlock(pos, next, DIRECT_BLOCK_UPDATE)) return false;
        changed.add(new BlockSnapshot(pos, current));
        return true;
    }

    private static void rollbackGradeMutation(ServerLevel level, List<BlockSnapshot> changed) {
        for (int i = changed.size() - 1; i >= 0; i--) {
            BlockSnapshot snapshot = changed.get(i);
            level.setBlock(snapshot.pos(), snapshot.state(), DIRECT_BLOCK_UPDATE);
        }
    }
'''
repl(service, old_apply, new_apply)

# Version and lock.
props = ROOT / 'gradle.properties'
repl(props, 'mod_version=0.1.0-alpha.59', 'mod_version=0.1.0-alpha.60')
repl(props,
     'and one centralized service-level shared-project authority gate.',
     'and one centralized service-level shared-project authority gate, plus rollback-safe ordinary building and terrain material transactions.')

lock = ROOT / 'COMPANION_LOCK.json'
repl(lock, '"frontier_settlement": "0.1.0-alpha.59"', '"frontier_settlement": "0.1.0-alpha.60"')
repl(lock,
'''    "Alpha.59 centralizes building, road, outpost and civil-work exclusivity in one server-side SettlementProjectAuthority gate reused by every preview/start service path. UI, commands and stale client previews are not trusted as the final exclusivity authority; no new save field, worker, currency, key or companion dependency is added.",''',
'''    "Alpha.59 centralizes building, road, outpost and civil-work exclusivity in one server-side SettlementProjectAuthority gate reused by every preview/start service path. UI, commands and stale client previews are not trusted as the final exclusivity authority; no new save field, worker, currency, key or companion dependency is added.",
    "Alpha.60 makes ordinary building placement and Alpha.44 terrain grading transaction-safe: required crate material is verified before mutation, successful world placement precedes ItemStack consumption/state advance, failed placement consumes nothing, and unexpected post-placement consumption failure rolls the newly changed blocks back. Existing already-paid final repair semantics are retained to avoid double charging.",''')
repl(lock, 'so Alpha.59 keeps only HUD collision avoidance', 'so Alpha.60 keeps only HUD collision avoidance')

# README.
readme = ROOT / 'README.md'
repl(readme, '## Current version: 0.1.0-alpha.59', '## Current version: 0.1.0-alpha.60')
repl(readme, 'No new Alpha.59 key was added.', 'No new Alpha.60 key was added.')
repl(readme, 'Alpha.40–59 deepen existing systems rather than inventing meaningless 16th–20th buildings.',
             'Alpha.40–60 deepen existing systems rather than inventing meaningless 16th–20th buildings.')
alpha60_readme = '''## Alpha.60 — rollback-safe ordinary construction transactions

Alpha.60 closes a physical-authority gap found during long-play audit. Roads, outposts and civil work already commit resources after successful world mutation; ordinary buildings now follow the same rule.

- a build step first verifies that the physical site crate contains its exact wood/stone delta;
- when the blueprint target is empty, `setBlock` must succeed **before** the crate ItemStacks are consumed and the construction step advances;
- an unexpected consume failure after successful placement rolls that newly placed block back to its previous state and leaves the step unchanged;
- if the correct building block is already present, the step still consumes its normal material delta before advancing, so player pre-fill cannot bypass construction cost;
- Alpha.44 grade cells now record the original block states they change; every clear/foundation/support placement checks `setBlock` success;
- a failed grade mutation restores all successful partial changes and consumes no retaining stone;
- retaining/foundation stone is consumed only after the full grade-cell mutation succeeds; an unexpected consume failure rolls the complete grade cell back and does not advance the project;
- already-paid `finishIfValid` replacement remains a repair of a step whose material was previously committed, so Alpha.60 does not double-charge historical/current prepaid repair;
- no `destroyBlock`, loose drops, virtual refund, new worker, save field, key, UI or currency is introduced.

This improves deterministic long-play/save safety but still does **not** replace the required real two-player runtime acceptance.

'''
repl(readme, '## Alpha.59 — centralized single-project authority hardening\n', alpha60_readme + '## Alpha.59 — centralized single-project authority hardening\n')

# Canonical plan.
canonical = ROOT / 'CANONICAL_PLAN.md'
repl(canonical, 'Current canonical implementation: **0.1.0-alpha.59**.', 'Current canonical implementation: **0.1.0-alpha.60**.')
repl(canonical, 'The original target was roughly 15–20 meaningful families. Alpha.40–59 deepen systems rather than adding fake families.',
                'The original target was roughly 15–20 meaningful families. Alpha.40–60 deepen systems rather than adding fake families.')
alpha60_can = '''### Alpha.60 ordinary construction transaction hardening

Alpha.60 aligns the oldest ordinary-building path with the physical transaction guarantees already used by later road/outpost/civil systems.

- site-crate wood/stone availability is checked before world mutation;
- for a missing blueprint block: successful `setBlock` -> physical crate consume -> construction state advance;
- failed `setBlock` means zero material loss and zero step advance;
- unexpected consume failure after a new placement restores the prior block state before pausing;
- already-present correct blueprint blocks still pay the normal step delta, preventing player pre-fill from becoming a free-build exploit;
- Alpha.44 grading captures reversible snapshots for every cleared/filled block in one grade cell;
- any failed grade placement rolls the cell back before material/state commit;
- retaining stone commits only after the complete grade-cell world mutation succeeds, and unexpected consume failure rolls the cell back;
- final validation repair remains non-double-charged because those blueprint step costs were already committed before repair;
- no drop-producing excavation, resource refund minting, new save state, builder, queue or UI is added.

'''
repl(canonical, '### Alpha.59 centralized single-project authority hardening\n', alpha60_can + '### Alpha.59 centralized single-project authority hardening\n')
repl(canonical, '## 14. Current playable slice after Alpha.59', '## 14. Current playable slice after Alpha.60')
repl(canonical,
'''- Alpha.59 centralized service-level single-project authority for building/road/outpost/civil preview and start;''',
'''- Alpha.59 centralized service-level single-project authority for building/road/outpost/civil preview and start;
- Alpha.60 rollback-safe ordinary building placement + Alpha.44 grade-cell physical material transactions;''')
repl(canonical, '## 15. Unfinished original-scope priorities after Alpha.59', '## 15. Unfinished original-scope priorities after Alpha.60')
repl(canonical,
'''17. Alpha.59 simultaneous building/road/outpost/civil confirmation exclusivity acceptance;
18. full companion lock fresh-world client/server runtime;''',
'''17. Alpha.59 simultaneous building/road/outpost/civil confirmation exclusivity acceptance;
18. Alpha.60 building setBlock failure/rollback + terrain retaining rollback/pre-fill-cost acceptance;
19. full companion lock fresh-world client/server runtime;''')
repl(canonical,
'''19. true Xaero markers only if a stable supported API appears;
20. moving boat/waterborne merchant only if presentation value justifies it and it never becomes a second logistics authority.''',
'''20. true Xaero markers only if a stable supported API appears;
21. moving boat/waterborne merchant only if presentation value justifies it and it never becomes a second logistics authority.''')
repl(canonical,
'''- ordinary small and Alpha.44 span3–4 construction terrain;''',
'''- ordinary small and Alpha.44 span3–4 construction terrain, including Alpha.60 no-loss failed placement and rollback-safe retaining transactions;''')

# Gap audit.
gap = ROOT / 'COMPLETION_GAP_AUDIT.md'
repl(gap, '현재 구현 기준: `0.1.0-alpha.59`', '현재 구현 기준: `0.1.0-alpha.60`')
repl(gap,
'''| 물리 단계 건설 | 완료 | grading→haul→foundation/frame/walls/roof/finish |''',
'''| 물리 단계 건설 | 완료 | grading→haul→foundation/frame/walls/roof/finish |
| 일반 건물 world/item 거래 원자성 | **완료/부분** | Alpha.60 placement/grade rollback transaction; 실제 실패주입·save/reload acceptance 남음 |''')
alpha60_gap = '''### Alpha.60 일반 건설 transaction 감사

- blueprint step delta는 site crate의 real wood/stone에서만 지불;
- empty target은 material availability 확인 -> successful world `setBlock` -> crate consume -> step advance 순서;
- failed `setBlock`은 item/step 손실 0;
- placement 뒤 consume의 예상 밖 실패는 새 block을 이전 state로 rollback하고 step 유지;
- 이미 올바른 blueprint block이 있는 경우 정상 step 비용은 계속 내므로 player pre-fill 무료건설 exploit 없음;
- Alpha.44 grade cell은 clear/floor/support 변경 전 원본 BlockState를 snapshot;
- grade 중 하나라도 `setBlock` 실패하면 앞서 성공한 변경을 역순 rollback;
- retaining stone은 전체 grade cell 성공 뒤에만 consume;
- retaining consume이 예상 밖 실패하면 complete grade mutation rollback + step 유지;
- historical/current final validation repair는 이미 step 비용이 납부된 block 복구이므로 이중과금하지 않음;
- destroyBlock/dropResources/free refund/virtual resource/new worker/save authority 없음.

따라서 ordinary construction도 later road/outpost/civil과 동일한 **world 성공 이후 physical material/state commit** 원칙으로 정렬됐다. 실제 실패주입 및 장시간 save/reload 검증은 남는다.

'''
repl(gap, '### Alpha.44 감사 유지\n', alpha60_gap + '### Alpha.44 감사 유지\n')
repl(gap,
'''16. Alpha.59 simultaneous building/road/outpost/civil confirm exclusivity acceptance;
17. full companion lock fresh-world client/server runtime;''',
'''16. Alpha.59 simultaneous building/road/outpost/civil confirm exclusivity acceptance;
17. Alpha.60 building placement failure/rollback + grade retaining consume rollback/pre-fill-cost acceptance;
18. full companion lock fresh-world client/server runtime;''')
repl(gap,
'''18. true Xaero marker는 stable supported API가 생길 때만;
19. moving boat/waterborne merchant는 두 번째 logistics authority가 되지 않는 경우에만 선택적 presentation.''',
'''19. true Xaero marker는 stable supported API가 생길 때만;
20. moving boat/waterborne merchant는 두 번째 logistics authority가 되지 않는 경우에만 선택적 presentation.''')

# Cumulative audits.
write(ROOT / 'tools/test_alpha60_source.py', '''#!/usr/bin/env python3
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
''')

write(ROOT / 'tools/test_alpha60_docs.py', '''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
readme=text('README.md'); can=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md')
must(readme,('## Current version: 0.1.0-alpha.60','## Alpha.60 — rollback-safe ordinary construction transactions','setBlock` must succeed **before** the crate ItemStacks are consumed','player pre-fill cannot bypass construction cost','failed grade mutation restores all successful partial changes','already-paid `finishIfValid` replacement','real two-player runtime acceptance'),'alpha.60 README')
must(can,('Current canonical implementation: **0.1.0-alpha.60**','### Alpha.60 ordinary construction transaction hardening','successful `setBlock` -> physical crate consume -> construction state advance','unexpected consume failure after a new placement restores the prior block state','Alpha.44 grading captures reversible snapshots','## 14. Current playable slice after Alpha.60','## 15. Unfinished original-scope priorities after Alpha.60'),'alpha.60 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.60`','일반 건물 world/item 거래 원자성 | **완료/부분**','### Alpha.60 일반 건설 transaction 감사','player pre-fill 무료건설 exploit 없음','complete grade mutation rollback + step 유지','실제 실패주입 및 장시간 save/reload 검증은 남는다'),'alpha.60 gap')
print('Frontier Settlement alpha.60 canonical docs audit: PASS')
''')

print('Applied Frontier Settlement 0.1.0-alpha.60 rollback-safe ordinary construction transactions.')
