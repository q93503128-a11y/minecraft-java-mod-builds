#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding='utf-8')
    if text.count(old) != 1:
        raise SystemExit(f'{path}: expected one patch anchor, found {text.count(old)}')
    path.write_text(text.replace(old, new, 1), encoding='utf-8')

road = JAVA / 'settlement/SettlementRoadService.java'
replace_once(road,
'''    private static boolean finishIfValid(MinecraftServer server, SettlementData data,
                                         RoadConstructionState road, List<Placement> plan, Villager builder) {
        ServerLevel level = server.overworld();
        for (Placement placement : plan) {''',
'''    private static boolean finishIfValid(MinecraftServer server, SettlementData data,
                                         RoadConstructionState road, List<Placement> plan, Villager builder) {
        ServerLevel level = server.overworld();
        // Alpha.24-and-earlier roads already paid their full stone cost before construction state was saved.
        // New physical roads must pay for every repair, but legacy prepaid saves must never be charged twice.
        boolean legacyPrepaidRepair = road.legacyPrepaidPaving();
        for (Placement placement : plan) {''')
replace_once(road,
'''            if (!ensurePavingMaterial(server, data, builder, 1L, 1L)) return false;
            if (!moveBuilderToPlacement(level, builder, placement)) return false;
            if (!level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)) return false;
            if (!consumeCarriedStone(builder, 1L)) {
                level.setBlock(placement.pos(), current, DIRECT_BLOCK_UPDATE);
                return false;
            }''',
'''            if (!legacyPrepaidRepair && !ensurePavingMaterial(server, data, builder, 1L, 1L)) return false;
            if (!moveBuilderToPlacement(level, builder, placement)) return false;
            if (!level.setBlock(placement.pos(), placement.state(), NORMAL_BLOCK_UPDATE)) return false;
            if (!legacyPrepaidRepair && !consumeCarriedStone(builder, 1L)) {
                level.setBlock(placement.pos(), current, DIRECT_BLOCK_UPDATE);
                return false;
            }''')

source_audit = ROOT / 'tools/test_alpha52_source.py'
replace_once(source_audit,
'''must(road, ('MAX_SHORT_BRIDGE_SPAN = 6', 'MAX_LONG_BRIDGE_SPAN = 24', 'MIN_RAVINE_DEPTH = 4',
            'MAX_LONG_BRIDGE_PIER_DEPTH = 12', 'LONG_BRIDGE_PIER_INTERVAL = 6',''',
'''must(road, ('MAX_SHORT_BRIDGE_SPAN = 6', 'MAX_LONG_BRIDGE_SPAN = 24', 'MIN_RAVINE_DEPTH = 4',
            'MAX_LONG_BRIDGE_PIER_DEPTH = 12', 'LONG_BRIDGE_PIER_INTERVAL = 6',
            'boolean legacyPrepaidRepair = road.legacyPrepaidPaving()',
            '!legacyPrepaidRepair && !ensurePavingMaterial(server, data, builder, 1L, 1L)',
            '!legacyPrepaidRepair && !consumeCarriedStone(builder, 1L)',''')

readme = ROOT / 'README.md'
replace_once(readme,
'- final road repair no longer places missing road/bridge blocks for free; each repair fetches and consumes a real stone ItemStack;',
'- final repair for Alpha.25+ physical roads no longer places missing road/bridge blocks for free; each such repair fetches and consumes a real stone ItemStack, while historical prepaid road saves keep their already-paid repair semantics to avoid double charging;')

canonical = ROOT / 'CANONICAL_PLAN.md'
replace_once(canonical,
'- final validation/repair also requires physical stone instead of free repair placement;',
'- final validation/repair for Alpha.25+ physical roads also requires physical stone instead of free repair placement; historical prepaid road saves remain cost-free at repair because their stone was already charged before persistence;')

gap = ROOT / 'COMPLETION_GAP_AUDIT.md'
replace_once(gap,
'- final validation missing block도 physical stone1개를 가져와 성공 배치 후 소비하며 free repair 없음;',
'- Alpha.25+ physical road의 final validation missing block도 physical stone1개를 가져와 성공 배치 후 소비하며 free repair 없음. 단, 이미 선결제된 historical road save는 이중과금 방지를 위해 기존 prepaid semantics를 유지;')

docs_audit = ROOT / 'tools/test_alpha52_docs.py'
replace_once(docs_audit,
"              'final road repair no longer places missing road/bridge blocks for free',",
"              'final repair for Alpha.25+ physical roads no longer places missing road/bridge blocks for free',\n              'historical prepaid road saves keep their already-paid repair semantics to avoid double charging',")
replace_once(docs_audit,
"                 'final validation/repair also requires physical stone',",
"                 'final validation/repair for Alpha.25+ physical roads also requires physical stone',\n                 'historical prepaid road saves remain cost-free at repair',")
replace_once(docs_audit,
"           'free repair 없음', '## 11. 완료 판정 금지선'), 'alpha.52 gap')",
"           'free repair 없음', 'historical road save는 이중과금 방지',\n           '## 11. 완료 판정 금지선'), 'alpha.52 gap')")

print('Applied Alpha.52 legacy-prepaid repair compatibility fix.')
