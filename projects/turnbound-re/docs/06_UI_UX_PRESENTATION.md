# 06 — UI / UX / PRESENTATION

> **현재 UI 상태: M5 PRE-IMPLEMENTATION VISUAL GATE PASS / IMPLEMENTATION SCREENSHOT GATE PENDING**

이 문서는 화면의 기능/정보/입력 계약을 정의한다.
M5의 상세 레퍼런스, 정보계층, design tokens, structural mockup 정본은 `17_M5_UI_VISUAL_GATE.md`를 따른다.
실제 sprite/icon/최종 screenshot 품질은 구현 후 별도 gate로 남는다.

## 1. 전투 HUD가 반드시 전달할 정보
- 현재 행동 actor.
- 4인 파티 HP/Energy/status.
- 적 HP/Poise/status.
- 현재 turn/cycle order.
- 각 적의 다음 Intent와 위험도.
- 선택 action의 HP/Poise 성격, Energy cost, target rule.
- 대상 affinity가 이미 발견된 경우 WEAK/RESIST/IMMUNE.
- EXPOSED 남은 창.
- 입력 가능/불가 상태와 이유.

우선순위는 `17_M5_UI_VISUAL_GATE.md`의 P0/P1/P2/P3 표를 사용한다.

## 2. 전투 입력 흐름
기본 keyboard/mouse 흐름:
`Actor ready -> Action list -> Action 선택 -> Target 선택 -> 확인/즉시 실행 -> Resolve`

- Esc/뒤로가기는 resolve 전 한 단계 취소.
- target이 하나뿐이고 오입력 위험이 없을 때 빠른 실행을 허용할 수 있다.
- resolve 중 중복 입력 차단.
- 서버 거부 시 명확한 오류 피드백.
- UI 애니메이션이 실제 server state보다 앞서 결과를 확정해 보이지 않게 한다.
- target 선택은 가능하면 world entity highlight/marker를 우선하고 별도 대형 target list popup을 기본값으로 쓰지 않는다.
- battle log는 기본 collapsed이며 필요할 때만 확장한다.

## 3. 주요 비전투 화면
- Party Formation: 4 slot, squad cost, 역할/태그 가시성.
- Character Detail: 별/레벨/스탯/affinity/kit/passive/승급 미리보기.
- Growth: 필요한 재화와 승급 후 변화를 사전 표시.
- Inventory/Equipment: 비교 가능한 수치 변화.
- Encounter Preview: 난이도/적 계열/보상/반복.
- Codex: 발견 정보와 미발견 정보 분리.

첫 M5 비전투 구현은 Party Formation + Character Overview/Skills/Growth를 같은 selected-character context 안에서 구성한다.
Roster / Active Party / Selected Detail의 3영역 구조를 기본으로 한다.

## 4. Accessibility/가독성
- 색만으로 WEAK/RESIST/위험도를 구분하지 않는다.
- 1280x720을 최소 기준선 중 하나로 검증하고 공용 QUALITY_STANDARD의 해상도 검증을 따른다.
- 작은 Minecraft GUI scale에서도 핵심 텍스트가 잘리지 않아야 한다.
- 지속적으로 깜박이는 위험 표시를 남발하지 않는다.
- 화면에 표시되는 keybinding은 실제 설정값에서 읽는다.
- 긴 한국어/영어 이름, 긴 설명, 큰 수치 상태를 테스트한다.
- P0 정보는 작은 폰트로 억지 축소해 해결하지 않는다.

## 5. DEBUG_ONLY UI
M0~M4에서 사용한 DEBUG_ONLY UI는 production design 선례가 아니다.

허용 정보:
- battle id/revision/seed.
- turn order.
- HP/Energy/Poise/Intent.
- action/target 버튼.
- event log.

M5 production HUD가 들어오면 debug 정보는 별도 debug overlay로 격리하며, 옛 debug 버튼/레이아웃을 production UI 옆에 남기지 않는다.

## 6. M5 Production Visual Gate

### 구현 시작 전 — PASS
완료:
1. 전투 HUD 상용 레퍼런스 8개 이상 비교.
2. 편성/캐릭터 화면 레퍼런스 3개 이상 비교.
3. Cobblemon/TurnBasedMinecraftMod/FTB 계열 등 Minecraft 실제 구현 사례 조사.
4. `08_REFERENCE_CATALOG.md` 갱신.
5. 화면별 information hierarchy 확정.
6. design token/component contract 확정.
7. Battle HUD structural mockup.
8. Party Formation structural mockup.
9. Minecraft 26.2/NeoForge `Screen` + GUI sprite/nine-slice feasibility 확인.

상세 정본: `17_M5_UI_VISUAL_GATE.md`.

현재 선택 방향:
**Minecraft-native tactical overlay**
- 중앙 3D 전장을 최대한 보존.
- 좌측 turn rail.
- enemy HP/Poise/Intent는 적과 시각적으로 가까운 위치.
- 하단 party status + 현재 actor command.
- 세부 정보는 hover/selection으로 progressive disclosure.
- 화면 전체를 generic dark card dashboard로 만들지 않음.

### 구현 후 — 아직 PENDING
반드시:
- 동일 해상도 실제 screenshot.
- mockup/reference와 side-by-side audit.
- hierarchy, spacing, scale, state, readability 차이 기록.
- 1280×720 / 1920×1080 / 여러 GUI scale 확인.
- 긴 한국어/영어, disabled/selected/error/empty 상태 확인.
- 수정 후 PASS.

실제 screenshot 비교 전에는 **M5 production visual PASS**라고 쓰지 않는다.

## 7. 구현 기술 기본값
첫 production pass는 Vanilla/NeoForge `Screen`/overlay + GUI sprite atlas를 사용한다.

- GUI scale 상대좌표.
- `blitSprite`.
- `nine_slice` scaling metadata.
- scissor/tooltip/input listener.
- layout 계산은 pure helper로 분리하여 자동 bounds test 가능하게 한다.
- 현재 UI 복잡도만으로 LDLib2 같은 대형 dependency를 추가하지 않는다.
- client는 presentation과 command intent만 담당하고 전투/성장 결과를 계산하지 않는다.

## 8. 금지 패턴
- '판타지 RPG니까' 근거 없이 금색/갈색/검정 조합.
- 화면 대부분을 큰 장식 패널로 가리기.
- 작은 글씨로 정보량을 억지로 늘리기.
- 기능별 버튼 스타일이 제각각.
- 코드 구현이 쉬운 배치가 UX 결정을 대신하기.
- 검은 반투명 사각형 + 컬러 테두리 + 독립 카드 여러 개의 generic RPG dashboard.
- Persona/Metaphor/Cobblemon 등의 proprietary UI art를 그대로 복제.
- AI가 임의로 만든 최종 아이콘/프레임을 reference 검토 없이 production에 넣기.
