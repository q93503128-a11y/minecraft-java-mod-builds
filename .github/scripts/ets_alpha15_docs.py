from pathlib import Path

root = Path('projects/earth-to-stars')


def replace_once(path: Path, old: str, new: str) -> None:
    body = path.read_text(encoding='utf-8')
    count = body.count(old)
    if count != 1:
        raise SystemExit(f'{path}: expected one replacement, found {count}: {old[:80]!r}')
    path.write_text(body.replace(old, new, 1), encoding='utf-8')


project = root / 'PROJECT.md'
replace_once(project, '- Mod version: `0.1.0-alpha.13`', '- Mod version: `0.1.0-alpha.15`')
replace_once(project, '- Final JAR: `earth_to_stars-0.1.0-alpha.13.jar`', '- Final JAR: `earth_to_stars-0.1.0-alpha.15.jar`')
replace_once(
    project,
    '- Client resource/model smoke: `PASS` — Xvfb `runClient` on Minecraft 26.2 / NeoForge 26.2.0.76, run `34328160894`; live visual quality remains `NOT PLAYTESTED`',
    '- Client resource/model smoke: `PASS` — Xvfb `runClient` on Minecraft 26.2 / NeoForge 26.2.0.76, run `34424914782`; alpha.15 changed boarding/runtime state only, not visual resources\n- Boarding EntityType dedicated-server contract: `PASS` — run `34731266964`; `serializable=true`, runtime exterior `shouldBeSaved=false`; live client boarding remains `NOT PLAYTESTED`'
)
replace_once(
    project,
    '# Current implementation baseline\n\n## 0.1.0-alpha.12 — Live-Acceptance Rescue',
    '''# Current implementation baseline\n\n## 0.1.0-alpha.15 — Boarding Root-Cause Rescue\n\nVerified source commit: `893eb4e244411c2bc93694c9e21d7209a09610ee`\n\nGitHub Actions `EARTH TO STARS alpha15 boarding rescue` run `34731266964`: **PASS**\n\nVerified JAR SHA-256: `c7d1adb1939b9108803b0e02e4f18ba3f309a726c24a0443641fb8144fa4485f`\n\nalpha.14 live playtest proved that the visible starter craft could not be boarded. Root cause was structural: `SHIP_EXTERIOR` used `EntityType.noSave()`, while Minecraft 26.2 server-side `Entity.startRiding()` rejects non-serializable vehicle types before force/canRide checks. alpha.15 removes that EntityType-level blocker while keeping the runtime exterior non-persistent through `ShipExteriorEntity.shouldBeSaved() == false`, so authoritative persistence remains in ShipState/SavedData.\n\nAlso verified in alpha.15:\n\n- authorized boarding uses the actual visible `ShipExteriorEntity` passenger relationship\n- authorized mount calls use the forced server path after ownership/range/seat checks\n- deployment no longer reports success if boarding fails; failed deployment rolls back repository/save/system/interior/runtime state\n- Earth↔Orbit reboarding/rollback uses the same corrected riding path\n- alpha.14 5-block-class hull, replaceable-vegetation deployment, hull-directed supply, and atlas-safe runtime OBJ contracts remain preserved\n- static acceptance, `clean test build`, production JAR verify: `PASS`\n- dedicated server boarding EntityType contract: `PASS` (`serializable=true`, exterior instance persisted=`false`)\n\nNot yet verified:\n\n- real client right-click boarding / seat position / camera and handling feel\n- live Earth→Orbit→salvage→combat→return loop\n- live multiplayer\n\n---\n\n## 0.1.0-alpha.12 — Live-Acceptance Rescue'''
)
replace_once(project, '2. 3×3×3 clearance', '2. 5×5×3 solid-obstacle clearance; replaceable grass/flowers and similar harmless vegetation are cleared automatically')
replace_once(project, '5. custom `ShipExteriorEntity` + model-backed spacecraft visual placement', '5. one visible `ShipExteriorEntity` that is simultaneously spacecraft visual, interaction target and actual passenger vehicle')
replace_once(project, 'The current use-on-block interaction is temporary M1 UX, not final refueling design.', 'Supply items now target the exact visible ship hull directly; arbitrary-block proximity refueling is forbidden. The cartridge interaction is still an early M1 service UX and may later become a richer service-port/refueling interaction.')
replace_once(project, 'alpha.12 removed the live-acceptance blockers that used ArmorStand/fake tether presentation.', 'alpha.15 keeps the ArmorStand/fake-tether removal and additionally fixes the server-side vehicle registration contract that made alpha.14 boarding impossible.')
replace_once(project, '- temporary use-on-block supply/upgrade UX', '- early cartridge-on-hull supply UX and temporary upgrade UX')
replace_once(
    project,
    '`ALPHA.12 LIVE-ACCEPTANCE RESCUE BUILD + DEDICATED RESOURCE LOAD VERIFIED / LIVE CLIENT ACCEPTANCE NEXT / LIVE MULTIPLAYER NOT TESTED`',
    '`ALPHA.15 BOARDING ROOT-CAUSE RESCUE BUILD + DEDICATED BOARDING CONTRACT VERIFIED / LIVE BOARDING & HANDLING ACCEPTANCE NEXT / LIVE MULTIPLAYER NOT TESTED`'
)

readme = root / 'README.md'
replace_once(
    readme,
    '> **상태: ALPHA.13 CLIENT MODEL/RESOURCE RESCUE BUILD + ACTUAL CLIENT RESOURCE LOAD VERIFIED / LIVE VISUAL ACCEPTANCE NEXT / LIVE MULTIPLAYER NOT TESTED**',
    '> **상태: ALPHA.15 BOARDING ROOT-CAUSE RESCUE BUILD + DEDICATED BOARDING CONTRACT VERIFIED / LIVE BOARDING & HANDLING ACCEPTANCE NEXT / LIVE MULTIPLAYER NOT TESTED**'
)
replace_once(readme, '# 현재 구현 — 0.1.0-alpha.12', '# 현재 구현 — 0.1.0-alpha.15')
replace_once(
    readme,
    '- 성공했을 때만 item 소비',
    '- 성공했을 때만 item 소비\n- 추진제/산소 보급은 임의 블록 근처 검색이 아니라 보이는 함선 본체를 직접 대상으로 한다'
)

changelog = root / 'CHANGELOG.md'
body = changelog.read_text(encoding='utf-8')
anchor = '이 문서는 실제 정본 변경을 기록한다.\n\n'
if body.count(anchor) != 1:
    raise SystemExit('CHANGELOG anchor mismatch')
entry = '''## 2026-09-13 — alpha.15 Boarding Root-Cause Rescue\n\n### Why alpha.15 exists\n\nalpha.14에서 starter craft 외형/크기/배치/보급 UX를 수술했지만 실제 플레이에서 함선 탑승이 여전히 불가능했다. Minecraft 26.2의 `Entity.startRiding()` 소스를 확인한 결과 차량 EntityType이 직렬화 불가능하면 서버가 force/canRide 검사보다 먼저 승차를 거부한다. alpha.14의 `SHIP_EXTERIOR`에는 `.noSave()`가 붙어 있어 승차가 구조적으로 항상 실패했다.\n\n### Fixed / Changed\n\n- mod version `0.1.0-alpha.15`\n- `SHIP_EXTERIOR` EntityType의 `.noSave()` 제거 → Minecraft 26.2 승차 가능한 serializable EntityType\n- runtime exterior 인스턴스는 `shouldBeSaved() == false`로 유지 → ShipState/SavedData와 이중 저장 금지\n- ownership/range/seat 검증 뒤 실제 visible hull에 server-authoritative forced mounting 사용\n- Earth↔Orbit transition 및 rollback 재탑승도 동일 경로 사용\n- 배치 직후 boarding 실패를 성공으로 숨기지 않고 deployment 전체를 rollback\n- alpha.14의 5-block급 visible hull / replaceable vegetation 배치 / hull-directed supply / runtime OBJ UV 교정 유지\n\n### Verification\n\nSource commit: `893eb4e244411c2bc93694c9e21d7209a09610ee`\n\nGitHub Actions `EARTH TO STARS alpha15 boarding rescue` run `34731266964`: `PASS`\n\n- alpha.15 static acceptance: `PASS`\n- existing progression/M1 regression: `PASS`\n- `clean test build`: `PASS`\n- production JAR verify: `PASS`\n- dedicated server boarding EntityType probe: `PASS` (`serializable=true`, runtime instance saved=`false`)\n- JAR: `earth_to_stars-0.1.0-alpha.15.jar`\n- SHA-256: `c7d1adb1939b9108803b0e02e4f18ba3f309a726c24a0443641fb8144fa4485f`\n- live client boarding: `NOT PLAYTESTED`\n- live multiplayer: `NOT TESTED`\n\n### Status\n\n`ALPHA.15 BOARDING ROOT-CAUSE RESCUE BUILD + DEDICATED BOARDING CONTRACT VERIFIED / LIVE BOARDING & HANDLING ACCEPTANCE NEXT / LIVE MULTIPLAYER NOT TESTED`\n\n---\n\n## 2026-09-10 — alpha.14 Starter Craft Core Rescue\n\nalpha.13의 live screenshot에서 starter craft가 atlas UV가 뒤엉킨 작은 물체로 보였고 탑승/배치/보급 UX도 게임 품질 기준을 충족하지 못했다. alpha.14는 보이는 hull을 실제 interaction/passenger entity와 통합하고, 약 5블록급 hitbox/seat, replaceable vegetation 허용 배치, hull-directed supply, atlas-safe runtime OBJ adaptation을 도입했다. Build run `34423576015` 및 client resource smoke run `34424914782`는 `PASS`였으나, 이후 실제 플레이에서 탑승 실패가 확인되어 live acceptance는 `FAIL`; alpha.15에서 근본 원인을 수정했다.\n\n---\n\n'''
changelog.write_text(body.replace(anchor, anchor + entry, 1), encoding='utf-8')

print('EARTH_TO_STARS_ALPHA15_DOCS_SYNC_READY')
