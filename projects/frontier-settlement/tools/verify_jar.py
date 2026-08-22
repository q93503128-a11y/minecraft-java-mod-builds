#!/usr/bin/env python3
import hashlib
import sys
import zipfile
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit('usage: verify_jar.py <jar>')
jar = Path(sys.argv[1]).resolve()
if not jar.is_file():
    raise SystemExit(f'JAR not found: {jar}')

required = {
    'kr/moonseungjun/frontiersettlement/FrontierSettlement.class',
    'kr/moonseungjun/frontiersettlement/settlement/SettlementData.class',
    'kr/moonseungjun/frontiersettlement/settlement/SettlementService.class',
    'kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.class',
    'kr/moonseungjun/frontiersettlement/settlement/BuildingRotation.class',
    'kr/moonseungjun/frontiersettlement/settlement/RotatedBlueprints.class',
    'kr/moonseungjun/frontiersettlement/settlement/SettlementOutpostService.class',
    'kr/moonseungjun/frontiersettlement/settlement/SettlementWorkerService.class',
    'kr/moonseungjun/frontiersettlement/settlement/SettlementStorageService.class',
    'kr/moonseungjun/frontiersettlement/settlement/WarehouseLayout.class',
    'kr/moonseungjun/frontiersettlement/settlement/SettlementTier.class',
    'kr/moonseungjun/frontiersettlement/network/SettlementSnapshotPayload.class',
    'kr/moonseungjun/frontiersettlement/network/PlacementRequestPayload.class',
    'kr/moonseungjun/frontiersettlement/network/PlacementPreviewPayload.class',
    'kr/moonseungjun/frontiersettlement/client/BuildingPlacementClient.class',
    'kr/moonseungjun/frontiersettlement/client/PlacementGhostRenderer.class',
    'kr/moonseungjun/frontiersettlement/client/SettlementHudOverlay.class',
}
with zipfile.ZipFile(jar) as zf:
    names = set(zf.namelist())
    missing = sorted(required - names)
    if missing:
        raise SystemExit('JAR missing classes: ' + ', '.join(missing))
    if any(name.endswith('.java') for name in names):
        raise SystemExit('runtime JAR unexpectedly contains Java source')

sha = hashlib.sha256(jar.read_bytes()).hexdigest()
sha_file = jar.with_suffix(jar.suffix + '.sha256')
sha_file.write_text(f'{sha}  {jar.name}\n', encoding='utf-8')
print(f'Frontier Settlement JAR verify: PASS\nSHA-256: {sha}')
