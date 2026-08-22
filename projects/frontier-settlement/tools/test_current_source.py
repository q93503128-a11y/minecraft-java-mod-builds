#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
required = [
    ROOT / 'build.gradle',
    ROOT / 'gradle.properties',
    ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/FrontierSettlement.java',
    ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementData.java',
    ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementService.java',
    ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/network/SettlementSnapshotPayload.java',
    ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/client/SettlementHudOverlay.java',
]
missing = [str(p.relative_to(ROOT)) for p in required if not p.is_file()]
if missing:
    raise SystemExit('missing required files: ' + ', '.join(missing))

props = (ROOT / 'gradle.properties').read_text(encoding='utf-8')
for token in ('minecraft_version=26.2', 'neo_version=26.2.0.38-beta', 'mod_id=frontier_settlement'):
    if token not in props:
        raise SystemExit(f'missing canonical property: {token}')

service = (ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementService.java').read_text(encoding='utf-8')
for token in ('Blocks.BARREL', 'frontier_settlement_builder', 'getTickCount() % 20', 'ItemTags.LOGS'):
    if token not in service:
        raise SystemExit(f'core vertical-slice invariant missing: {token}')

saved = (ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementData.java').read_text(encoding='utf-8')
if 'server.getDataStorage().computeIfAbsent(TYPE)' not in saved:
    raise SystemExit('settlement state is not server-global SavedData')

print('Frontier Settlement source audit: PASS')
