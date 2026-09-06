# 06 — UI / UX / PRESENTATION

> **현재 최종 시각 디자인 상태: GATED**

이 문서는 화면의 기능/정보/입력 계약을 정의한다. 색/모양/장식은 외부 레퍼런스 조사 전에 확정하지 않는다.

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

## 2. 전투 입력 흐름
기본 keyboard/mouse 흐름:
`Actor ready -> Action list -> Action 선택 -> Target 선택 -> 확인/즉시 실행 -> Resolve`

- Esc/뒤로가기는 resolve 전 한 단계 취소.
- target이 하나뿐이고 오입력 위험이 없을 때 빠른 실행을 허용할 수 있다.
- resolve 중 중복 입력 차단.
- 서버 거부 시 명확한 오류 피드백.
- UI 애니메이션이 실제 server state보다 앞서 결과를 확정해 보이지 않게 한다.

## 3. 주요 비전투 화면
- Party Formation: 4 slot, squad cost, 역할/태그 가시성.
- Character Detail: 별/레벨/스탯/affinity/kit/passive/승급 미리보기.
- Growth: 필요한 재화와 승급 후 변화를 사전 표시.
- Inventory/Equipment: 비교 가능한 수치 변화.
- Encounter Preview: 난이도/적 계열/보상/반복.
- Codex: 발견 정보와 미발견 정보 분리.

## 4. Accessibility/가독성
- 색만으로 WEAK/RESIST/위험도를 구분하지 않는다.
- 1280x720을 최소 기준선 중 하나로 검증하고 공용 QUALITY_STANDARD의 해상도 검증을 따른다.
- 작은 Minecraft GUI scale에서도 핵심 텍스트가 잘리지 않아야 한다.
- 지속적으로 깜박이는 위험 표시를 남발하지 않는다.
- 화면에 표시되는 keybinding은 실제 설정값에서 읽는다.

## 5. DEBUG_ONLY UI
M0~M4 허용 정보:
- battle id/revision/seed.
- turn order.
- HP/Energy/Poise/Intent.
- action/target 버튼.
- event log.
기본 위젯 수준으로 충분하다. 최종 프레임/아이콘/색 체계를 만들지 않는다.

## 6. Production Visual Gate
M5 시작 전 반드시:
1. 전투 HUD 레퍼런스 3개 이상.
2. 편성/캐릭터 화면 레퍼런스 3개 이상.
3. Minecraft 화면 안에서의 적용 가능성 분석.
4. 정보 우선순위 표.
5. 디자인 토큰 문서.
6. 최소 2개 핵심 화면 목업.
7. 사용자 승인 또는 명시적 정본 선택.

구현 후:
- 동일 해상도 실제 screenshot.
- mockup/reference와 side-by-side audit.
- hierarchy, spacing, scale, state, readability 차이 기록.
- 수정 후 PASS.

## 7. 금지 패턴
- '판타지 RPG니까' 근거 없이 금색/갈색/검정 조합.
- 화면 대부분을 큰 장식 패널로 가리기.
- 작은 글씨로 정보량을 억지로 늘리기.
- 기능별 버튼 스타일이 제각각.
- 코드 구현이 쉬운 배치가 UX 결정을 대신하기.
