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

## External world

TURNBOUND repository는 Drehmal 원본 world/resource pack을 재배포하지 않는다.
공식 배포본을 별도로 설치하고 TURNBOUND가 자체 gameplay metadata를 bind한다.

Operator diagnostics:
- `/turnbound world status`
- `/turnbound world bind_drehmal`

현재 integration seed는 실제 26.2 지형 조사 전까지 player-facing teleport anchor가 아니다.

## Validation

Current verified combat-overhaul checkpoint:
- Build TURNBOUND #764
- code commit: `fb54674f290521c6ae765bef8f00147b6eccd5fd`
- fixed-point TurnScheduler tests: PASS
- Kyren v1 duel-loop tests: PASS
- Lumea v1 tempo-control tests: PASS
- Gradle tests: PASS
- NeoForge server smoke: PASS
- JAR verify: PASS
- artifact: `turnbound-v04-workbranch` (id `10552255748`, SHA-256 `4d92b7c02f6b233a2fe02476184d4810079d92160522cf3d332eebf9faaf1734`)
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO
