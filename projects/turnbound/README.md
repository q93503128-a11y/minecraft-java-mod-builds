# TURNBOUND

Minecraft Java 26.2 / NeoForge 기반 3D 파티 턴제 RPG.

## Current direction

TURNBOUND는 현재 대격변 설계 단계다.

핵심:
- Drehmal: APOTHEOSIS v2.2.2f를 별도 설치해 production world로 사용
- 4인 파티 / Turn Gauge 전투 골격 유지
- SPD/Gauge scheduler 근본 수정
- battle-center 기반 terrain-aware 카메라
- 캐릭터 skill/passive/concept 재설계
- UI/폰트/가독성/미니맵 전면 개편
- 실제 Drehmal 지형을 읽은 NPC/조우/퀘스트 배치
- 3D 소환 연출 우선
- obsolete Aster world/code/docs 정리

## Canon

1. `PROJECT.md`
2. `01_GAME_DESIGN_v1.md`
3. `02_BALANCE_RULES_v1.md`
4. `03_CHARACTER_DESIGN_v1.md`
5. `UI_DESIGN_SYSTEM.md`
6. `WORLD_OVERHAUL_DREHMAL.md`
7. `REFERENCE_BASELINE_v1.md`


## One-click test install

권장 테스트 방식은 CI가 생성하는 `TURNBOUND-oneclick-*.mrpack`을 **한 번만 새 인스턴스로 가져오는 것**이다.

첫 실행:
- Minecraft 26.2 / NeoForge 26.2.0.62 / GeckoLib 5.5.3 구성은 모드팩이 담당한다.
- TURNBOUND가 Drehmal 2.2.2f 공식 GitHub release에서 map shards + resource pack을 직접 다운로드한다.
- 다운로드 약 4.18 GB, 설치 중 여유 공간 약 10 GB 권장.
- 다운로드/압축해제/공식 map hash 검증 후에만 TURNBOUND world marker가 생성된다.
- 수동 `/turnbound world bind_drehmal`은 one-click 설치 월드에는 필요 없다.

이후 업데이트:
- **새 모드팩 인스턴스를 만들지 않는다.**
- 같은 인스턴스와 `saves/`를 유지하고 `mods/turnbound-*.jar`만 새 JAR로 교체한다.
- 설치된 Drehmal map/resource pack은 다시 다운로드하지 않는다.
- base Minecraft/NeoForge 또는 pinned Drehmal version이 바뀌는 큰 migration 때만 새 pack이 필요할 수 있다.

Drehmal 원본 map/resource bytes는 TURNBOUND 배포물에 포함하지 않는다. first-run bootstrap은 공식 upstream 파일을 직접 받는다.

## External world

TURNBOUND repository는 Drehmal 원본 world/resource pack을 재배포하지 않는다.
공식 배포본을 별도로 설치하고 TURNBOUND가 자체 gameplay metadata를 bind한다.

Operator diagnostics:
- `/turnbound world status`
- `/turnbound world bind_drehmal`

현재 integration seed는 실제 26.2 지형 조사 전까지 player-facing teleport anchor가 아니다.

## Validation

Latest verified checkpoint:
- Build TURNBOUND #847
- code commit: `dc566d62a2db420a99f8cb7147df6059f8abd850`
- Gradle tests/build: PASS
- NeoForge server smoke: PASS — Done (4.337s)
- JAR verify: PASS — SHA-256 `9a85a7ee4ce1779fb4b54ed4eb95833e468da92ea311a0ba20dd9d337538960e`
- one-click mrpack verify: PASS — SHA-256 `2c29fa7a7296b181830251c07e334f8e9d31771b27627a9e7ec8fda2a2182efd`
- artifact: `turnbound-v04-workbranch` (id `10726407574`)
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO
