# 17B — M5 HUD READABILITY GATE

최종 갱신: 2026-09-08
상태: **AUTO GATE PASS / VISUAL QA PENDING**

이 문서는 `17_M5_UI_VISUAL_GATE.md`와 `17A_M5_COMMAND_OVERLAY_LIFECYCLE_GATE.md` 이후의 Battle HUD 가독성 보강을 정본화한다.

이번 배치는 새로운 production art를 만드는 단계가 아니다. 기존 Minecraft GUI sprite 계열을 유지한 채, 실제 screenshot audit에 들어가기 전에 자동으로 잡을 수 있는 정보 밀도·현지화·최소 logical canvas 문제만 수정했다.

## 1. 변경 원칙

유지:
- 중앙 Minecraft 3D 전장이 화면의 주인공이다.
- 신규 임의 palette/frame/icon을 만들지 않는다.
- combat 결과, target legality, ownership을 client에서 재계산하지 않는다.
- `BattleHud`는 authoritative S2C snapshot을 표시만 한다.
- command/target chooser와 world marker의 기존 server-authoritative 계약은 그대로 유지한다.

이번에 변경:
- HUD 내부 개발자식 영문 상태 문자열을 player-facing translation으로 이동.
- 최소 지원 logical canvas에서 4인 party 정보를 읽을 수 있도록 compact layout을 추가.
- EN/KO translation key와 현재 Enemy Intent/core status vocabulary를 자동 계약으로 잠금.

## 2. Compact party layout

기존 문제:
- 480×270 최소 지원 canvas에서 party 영역 폭이 약 256 logical px다.
- 4명을 무조건 한 줄에 배치하면 각 멤버가 약 64 px만 받아 이름/HP/Energy/status가 지나치게 압축된다.

현재 계약:
- `UiLayoutMetrics.partyGrid(...)`가 party 영역 폭과 인원 수로 표시 구조를 정한다.
- party 영역 폭 `< 360`이고 표시 인원이 3~4명이면 `2×2 compact`로 전환한다.
- 그 외 normal layout에서는 기존 한 줄 party를 유지한다.

480×270 / 4인 기준:
- columns: 2
- rows: 2
- cell width: 약 128
- cell height: 약 41
- 각 cell: slot number / name / HP+Energy compact text / HP bar / Energy bar / optional status
- reserved world viewport는 최소 약 300×96 이상을 유지한다.

640×360 / 4인 기준:
- columns: 4
- rows: 1
- 각 cell 폭 90 이상
- 기존 regular party presentation 유지.

## 3. HUD localization boundary

새 `BattleHudPresentation`은 authoritative snapshot fact를 player-facing copy로 변환한다.

다루는 정보:
- 현재 turn 표시
- defeated / exposed / guard / poise guard
- Enemy Intent risk/type/targeting
- break-cancel 가능 여부
- status 이름
- status stack / remaining turn 표현

현재 Enemy Intent enum 전부를 명시적으로 매핑한다.

Risk:
- NORMAL
- DANGEROUS
- ULTIMATE

Type:
- ATTACK
- DEFEND
- BUFF
- DEBUFF
- SPECIAL

Targeting:
- SINGLE
- ALL
- SELF
- RANDOM

현재 authored core status 12종도 EN/KO translation key에 연결한다.

- guard
- exposed
- poise_guard
- burn
- slow
- atk_up
- def_down
- venom
- webbed
- evasion
- ward
- volatile

미래에 새 status가 추가되었는데 translation mapping이 아직 없다면 전투 규칙을 추측하지 않고 humanized id로 fallback한다.

## 4. 자동 계약

`M5UiLayoutMetricsTest`:
- 480×270에서 4인 party가 반드시 2×2 compact.
- compact cell width >= 120.
- compact cell height >= 40.
- minimum viewport width >= 300.
- minimum viewport height >= 96.
- party/command region이 world viewport를 침범하지 않음.
- 640×360에서 4인 party는 다시 한 줄.
- 기존 480/640/1280/1920 target chooser non-modal 계약 유지.

`M5LanguageContractsTest`:
- `en_us` / `ko_kr` key set 정확히 동일.
- 기존 command tooltip/disabled reason key 유지.
- Enemy Intent Risk/Type/Targeting 전 enum이 실제 translation key로 연결됨.
- authored core status 12종 모두 translation key 존재.
- unknown future status는 잘못된 의미를 발명하지 않고 fallback 경로 사용.

## 5. 자동 검증

GitHub Actions:
- workflow: `Build turnbound-re`
- Run: `34171544704`
- validated commit: `a367e3e1c8678b987c1a0714a807f579bf577135`
- Java: Temurin 25.0.4+1
- Gradle: 9.2.1
- NeoForge: 26.2.0.38-beta
- dependency resolution + clean build: **PASS**
- compileJava / compileTestJava / JUnit: **PASS**
- production JAR verify: **PASS**
- artifact upload: **PASS**
- JAR: `turnbound_re-0.1.0-alpha.1.jar`
- JAR SHA-256: `7b78790ce0825d9b55055e91ac9168068badcfb2e7e9e1a7661faa088b45c6ba`

## 6. 현재 판정

### BATTLE HUD READABILITY AUTO GATE: PASS

완료:
- 최소 logical canvas 4인 party 밀도 문제 자동 해결.
- normal layout 회귀 보존.
- HUD의 Intent/status player-facing localization.
- EN/KO parity와 current combat vocabulary 자동 검증.
- 기존 world-first / server-authoritative UI 구조 보존.
- clean build/JUnit/JAR verify 통과.

### 아직 PASS가 아닌 것

- 실제 Minecraft screenshot에서의 최종 가독성.
- 실제 GUI Scale 옵션별 clipping/시선 이동 체감.
- final production sprite/icon/frame asset quality.
- animation/transition timing.
- Party Formation / Character / Skills / Growth production UI.

사용자 방침상 이 단계에서 중간 JAR 테스트를 요구하지 않는다. 자동으로 닫을 수 있는 Battle HUD 구조/interaction/readability gate는 닫혔으므로 다음 실제 개발은 `Party Formation` screen skeleton으로 이동하고, 최종 integrated client test 때 Battle screenshot audit을 함께 수행한다.
