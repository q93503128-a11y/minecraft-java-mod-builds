# TURNBOUND: RE

Minecraft Java에서 동작하는 파티 기반 턴제 RPG 프로젝트의 새 정본이다.

> **상태: CORE IMPLEMENTATION READY / M5 BATTLE UI IMPLEMENTATION IN PROGRESS / FINAL VISUALS GATED**

`projects/turnbound/`의 구 TURNBOUND는 폐기 프로젝트이며 TURNBOUND: RE의 설계 근거로 사용하지 않는다. 이름이 비슷하더라도 자동 승계되는 규칙은 없다.

## 읽는 순서
1. `AGENTS.md`
2. `AGENT_RULES.md`
3. `docs/CANON.md`
4. 작업 분야에 해당하는 세부 문서
5. 저장소 공용 `../../docs/BUILD_STANDARD.md`, `../../docs/QUALITY_STANDARD.md`

## 정본 문서
- `PROJECT.md` — 목표·범위·개발 준비 상태
- `docs/CANON.md` — 변경 시 명시적 합의가 필요한 핵심 결정
- `docs/00_PRODUCTION_PRINCIPLES.md` — 제작 원칙·완료 기준
- `docs/01_GAME_LOOP_WORLD.md` — 월드/탐험/생활/조우
- `docs/02_COMBAT_SYSTEM.md` — 전투 규칙·공식·상태기계
- `docs/03_PROGRESSION_ECONOMY.md` — 별/레벨/승급/재화
- `docs/04_CHARACTER_SYSTEM.md` — 캐릭터 규격·역할·스킬 제작 규칙
- `docs/05_VANILLA_ROSTER_CATALOG.md` — 바닐라 엔티티 전수 등록 규칙
- `docs/06_UI_UX_PRESENTATION.md` — UI/연출 요구사항과 시각 디자인 게이트
- `docs/07_TECHNICAL_ARCHITECTURE.md` — 서버 권한·네트워크·저장·코드 구조
- `docs/08_REFERENCE_CATALOG.md` — 턴제 게임/오픈소스 참고 근거
- `docs/09_WORLD_ASSET_GATE.md` — 맵·건축·외부 자산 도입 절차
- `docs/10_VERTICAL_SLICE_IMPLEMENTATION.md` — 첫 실게임 구현 범위
- `docs/11_CONTENT_PIPELINE_TEST_PLAN.md` — 데이터/CI/게임테스트
- `docs/12_BALANCE_PLAYTEST_PROTOCOL.md` — 밸런스 측정 방법
- `docs/13_DATA_SCHEMA.md` — JSON/Codec 계약
- `docs/14_IMPLEMENTATION_BACKLOG.md` — 개발 순서·완료 조건
- `docs/15_READINESS_AUDIT.md` — 지금 바로 개발 가능한 범위와 아직 콘텐츠 완성 전인 범위

## 핵심 한 줄
Minecraft의 탐험·채집·제작을 버리지 않으면서, 보이는 조우를 통해 4인 파티가 적의 의도를 읽고 약점/Poise를 공략해 `EXPOSED` 창을 만드는 빠른 턴제 전투를 반복한다.

## 현재 구현 경계
M0~M4 전투 코어·네트워크·조우·대표 캐릭터·성장/보상 기반은 구현 정본을 유지한다. M5에서는 승인된 `Minecraft-native tactical overlay` 방향에 따라 battle HUD/command interaction을 실제 production 코드로 옮기는 중이다.

2026-09-07 기준 M5 battle interaction에는 다음이 구현되어 있다.
- 서버 snapshot 기반 action affordance와 hover tooltip
- Energy/target 부족 등 비활성 사유의 한·영 사용자 문구
- 실제 Controls key를 반영하는 command HUD
- protocol v5의 authoritative `participant -> entity UUID` presentation binding
- action 선택 시 서버가 허용한 대상만 표시하는 world target nameplate marker
- `TARGET / FOCUS / SELECTED`에 해당하는 텍스트+색 병행 피드백
- battleId+revision이 달라지면 stale marker를 무효화하고 command screen 종료 시 marker state 제거
- DEBUG_ONLY encounter도 production과 동일한 정의/행동/binding snapshot 경로 사용

이 milestone의 자동 검증 기준은 commit `8cbb255d58fa96bcf58fb800552e1dabc7b18484`, GitHub Actions `Build turnbound-re` Run `34114679756`이다. Java 25 + NeoForge 26.2.0.38-beta clean build/JUnit와 production JAR verify가 성공했으며 검증 JAR `turnbound_re-0.1.0-alpha.1.jar`의 SHA-256은 `e98c7c5309e058563fdafdd3b2c609545a814f3ffb2826634d9d8d4b4241611e`이다.

단, 이 성공은 **코드/자동 게이트 통과**를 뜻한다. 실제 Minecraft 클라이언트에서의 HUD 가독성, world marker 겹침, 거리별 readability, target popup의 시야 점유는 아직 visual/field-play gate로 남아 있다. 캐릭터 외형, VFX, 월드 미술도 기존 visual gate를 유지한다.

전체 완제품의 캐릭터/지역/아이템/드롭표까지 모두 채워진 `CONTENT COMPLETE` 상태는 아니며, 정확한 경계는 `docs/15_READINESS_AUDIT.md`를 따른다.
