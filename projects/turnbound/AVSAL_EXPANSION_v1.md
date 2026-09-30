# TURNBOUND Av'Sal Expansion v1

> 역할: New Drabyel 이후 첫 본격 확장 챕터의 story / quest / encounter / outcome / character-production 정본.
> 정확한 원본 lore·지형은 Drehmal source와 현재 26.2 world가 우선하며, TURNBOUND는 원본을 덮어쓰지 않고 gameplay layer를 얹는다.

## 1. 목표

Av'Sal 확장은 단순히 적 수를 늘리는 패치가 아니다.

완성해야 하는 플레이 흐름:

```text
New Drabyel에서 다음 목적 확인
→ 실제 도로를 따라 Av'Sal 접근
→ 순찰 / 사건 / 선택 Elite
→ 폐허 외곽에서 상황 파악
→ 북쪽 dock Midboss
→ central island 진입
→ named encounter / 지역 핵심 문제
→ 선택에 따른 챕터 결말
→ 새로운 캐릭터 / 상점 / 탐험 / 고난도 content 해금
```

목표 플레이타임:
- 직선 메인 진행: 약 45~70분
- side/elite/탐험 포함: 약 70~110분

숫자를 맞추기 위해 전투를 끼워 넣지 않는다.

## 2. Story framing

Av'Sal의 원본 도시/폐허/Mihkmari 점거 상태는 Drehmal source fact로 취급한다.
TURNBOUND는 그 위에 **최근 다시 작동하기 시작한 비정상적인 relay/echo 현상**을 별도 사건으로 얹는다.

중요:
- TURNBOUND 사건을 원본 Drehmal 고대사나 정사인 것처럼 쓰지 않는다.
- 원본 named NPC / interactive landmark를 임의로 죽이거나 제거하지 않는다.
- 핵심 갈등은 “누가 절대적으로 선/악인가”보다 폐허를 어떻게 다룰지에 둔다.

챕터 질문:
**도시 깊은 곳의 불안정한 연결을 봉인할 것인가, 복구할 것인가, 핵심만 회수할 것인가?**

## 3. Main quest beats

### MQ_AV01 — 무너진 길의 끝
- New Drabyel에서 Av'Sal 방향 출발
- road patrol 1~2개
- 길가 사건 1개
- Av'Sal 외곽 시야 확보
- objective type: REACH + OPTIONAL_INTERACT

### MQ_AV02 — 폐허를 쓰는 사람들
- 외곽 scavenger / 생존자 / 기록 중 2개 이상 조사
- 순서 자유
- 모든 marker를 강제로 찍지 않는다
- objective type: INTERACT_SET_ANY

### MQ_AV03 — 끊어진 선
- TURNBOUND-authored relay/echo 이상 3곳 중 2곳 안정화
- 한 곳은 전투, 한 곳은 환경 상호작용, 한 곳은 우회 가능한 선택형
- objective type: MULTI_ROUTE_2_OF_3

### MQ_AV04 — 북쪽 선착장
- central island 접근을 막는 Midboss
- 2~3개 pattern
- 단순 HP sponge 금지
- objective type: MIDBOSS_WIN

### MQ_AV05 — 섬의 두 칼
- source-backed named-warrior 위치를 훼손하지 않는 범위에서 TURNBOUND encounter로 해석
- 동시에 둘을 상대하거나, 선행 side choice에 따라 순차적으로 상대할 수 있음
- objective type: MULTI_ENCOUNTER

### MQ_AV06 — 남겨 둘 것
지역 핵심 encounter 후 세 가지 해결안을 제시한다.

1. **봉인**
   - relay/echo를 닫음
   - post-clear 필드 압박 감소
   - 기록/character-story 쪽 후속이 빨리 열림

2. **복구**
   - 제한적으로 안정화해 연결 기능을 살림
   - Av'Sal fast-travel / service utility 쪽 후속이 빨리 열림
   - 반복 전투에서는 불안정 pattern이 일부 남음

3. **회수**
   - 핵심 부품을 Drabyel 쪽으로 가져감
   - 즉시 장비/Gold/Crystal 보상이 가장 큼
   - Av'Sal 일부 위험 encounter가 그대로 남아 후속 side quest가 생김

이 선택은 **캐릭터 영구 미획득, 지역 영구 봉쇄, save softlock**을 만들지 않는다.
차이는 후속 NPC/route/repeat encounter/보상 순서에 두고, 장기적으로 핵심 콘텐츠는 모두 접근 가능해야 한다.

## 4. Side quest / objective variety

Av'Sal에서 같은 “N마리 처치”를 반복하지 않는다.

우선 구현 후보:
- 호위: NPC가 실제 route를 이동하고 길목에서 1~2회 사건 발생
- 방어: 일정 enemy action 수 동안 대상 보호
- 추적: 발자국/물품/시야 단서를 순서대로 찾음
- 선택 구조: 적을 잡기 전에 포로/생존자를 먼저 해방하면 다른 종료
- 제한 전투: revive 없이 Elite 승리, 특정 적을 마지막에 처치
- 회수: 전투 전/후 어느 쪽으로 접근했는지에 따라 reward beat 변화
- 생존: 일정 행동 수 버틴 뒤 탈출 point 활성
- 조사: 여러 clue 중 필요한 수만 모으면 다음 단계로 진행
- character quest: 신규 캐릭터의 combat mechanic을 실제 월드 사건으로 먼저 보여줌

실패 조건은 즉시 save 파괴가 아니라:
- 보상 축소
- 다른 follow-up
- NPC 상태 변화
- 재도전 가능한 combat reset
중 하나를 기본으로 한다.

## 5. Quest / achievement UI contract

현재 E 메뉴의 `퀘스트` 화면은:
- 왼쪽: 지역/퀘스트 진행
- 오른쪽: 전투/탐험 업적
을 함께 보여주는 구조를 유지한다.

용어:
- `업적` = No Death, 제한 행동 수, 특정 mechanic 달성 같은 장기/조건형 기록
- `고난도` = Hard Boss / Rift처럼 실제로 출전하는 endgame content

따라서 홈 메뉴의 별도 `고난도` 버튼은 업적 목록이 아니다.

확장 목표:
- 퀘스트 화면 내부 category를 `메인 / 지역 / 캐릭터 / 업적`으로 정리
- 단, 새 메뉴를 하나 더 만드는 방식은 사용하지 않는다
- 추적 중 objective는 최대 3개
- 완료/분기 결과는 journal에 남지만 평상시 HUD를 차지하지 않는다

## 6. New playable character batch

첫 확장에서는 **P09~P12 네 명**을 목표로 한다.
이 네 명은 현재 P01~P08의 변형판을 만들지 않는다.

중요 제작 순서:
1. 사용 가능한 외부 visual asset을 먼저 선택
2. silhouette / weapon / animation capability 확인
3. 그 asset이 자연스럽게 소화하는 combat fantasy를 확정
4. kit / 이름 / story를 최종 고정

즉, AI가 캐릭터 의상을 새로 발명한 뒤 모델을 억지로 맞추지 않는다.

### P09 — Channel / Delayed Cast
- provisional role: DPS / delayed payoff
- 핵심: 현재 행동을 투자해 주문을 저장하고, 지정된 다른 regular action 수가 지난 뒤 자동 해결
- 중간에 down/interrupt되면 손실 가능
- 즉시 피해형 caster와 완전히 다른 “미리 걸어 두는 턴” 플레이
- reference lesson: Reverse: 1999의 channel/memorize 계열처럼 **지금 행동을 미래 효과로 바꾸는 구조**
- 직접 복제하지 않고 TURNBOUND scheduler 기준으로 재설계
- external visual base 후보: Quaternius RPG Character Pack wizard 또는 Ultimate Modular Women의 fantasy caster

### P10 — Linked Targets
- provisional role: multi-target DPS / tactical debuff
- 적 두 명을 Link
- 한쪽에 들어간 single-target direct damage 일부를 다른 쪽에 echo
- AoE/echo가 다시 echo하지 않게 recursion 금지
- target swap이 손해인 P01과 반대로, 두 목표를 계속 관리하는 캐릭터
- external visual base 후보: Quaternius RPG Character Pack rogue/assassin

### P11 — Ordered Reagents
- provisional role: support / combo setup
- 두 종류의 reagent mark를 사용
- 같은 두 mark라도 **적용 순서**에 따라 결과가 달라짐
  - A→B: 공격적 reaction
  - B→A: 방어/지원 reaction
- UI는 두 작은 glyph만 사용하고 별도 대형 meter 금지
- reference lesson: card/order-combination RPG에서 “같은 자원도 순서가 선택”이 되는 구조
- external visual base 후보: Quaternius modular adventurer/worker + CC0 fantasy prop

### P12 — Alternating Form
- provisional role: hybrid DPS / utility
- regular action 종료마다 두 Form이 자동 교대
- 같은 skill이 Form에 따라 다른 두 번째 효과를 가짐
- 별도 stack farm 없이 **현재 Form을 읽고 다음 행동을 계획**하는 캐릭터
- P08처럼 자원을 채우는 캐릭터가 아니며 P02처럼 Gauge를 직접 지휘하는 support도 아님
- external visual base 후보: Quaternius monk / distinctive modular warrior

세부 수치/이름/희귀도는 external visual을 실제로 고른 뒤 확정한다.

## 7. External visual sources

우선 조사/사용 pool:

### Quaternius — RPG Character Pack
- license: CC0
- 6 rigged, animated, textured fantasy characters
- FBX / OBJ / Blend
- direct visual base 후보

### Quaternius — Ultimate Modular Women Pack
- license: CC0
- 10 characters / 24 animations
- modular parts
- FBX / OBJ / glTF / Blend

### Quaternius — Ultimate Modular Men Pack
- license: CC0
- 11 characters / 24 animations
- modular parts
- FBX / OBJ / glTF / Blend

### Kenney — Blocky Characters
- license: CC0
- 18 skins / 27 animations
- low-rarity characters, NPC, enemy variants에 특히 적합

자산 사용 시 프로젝트에:
- source URL
- author
- license
- direct / editable-base / reference 분류
를 기록한다.

## 8. Turn-RPG reference lessons

가져올 것은 개별 캐릭터 복제가 아니라 **mechanic 설계 원칙**이다.

- Epic Seven: Speed와 Combat Readiness를 분리하고, counter/dual/revive처럼 반응형 kit가 캐릭터 정체성이 됨
- Honkai: Star Rail: follow-up / action advance / extra action이 일반 turn과 다른 action economy로 취급됨
- Reverse: 1999: AP/Moxie 외에도 character-specific resource, channel, card/order 조건이 플레이 방식 자체를 바꿈
- Octopath Traveler: Break/Boost처럼 “지금 저장하고 나중에 크게 쓰는 선택”이 단순 stat 증가보다 강한 전술성을 만듦
- Limbus Company: clash/counter/coin처럼 공격 자체의 처리 규칙을 캐릭터와 encounter가 다르게 활용

TURNBOUND는 이 시스템들을 통째로 가져오지 않는다.
기존 Gauge 1000 / cooldown / reaction / 4인 파티 골격 안에서 캐릭터 하나가 다른 선택을 만들도록 축약한다.

## 9. Completion gate

Av'Sal expansion을 “콘텐츠 추가 완료”라고 부르려면:
- main quest chain 실제 진행 가능
- 최소 3종 이상의 objective pattern
- 1개 이상 branch outcome
- 외곽 common encounter family
- optional Elite
- north dock Midboss
- central island major encounter
- P09~P12 중 최소 2명은 model/animation/VFX/SFX까지 실제 플레이 가능
- 퀘스트/업적 journal 표시
- save persistence
- server authority
가 연결되어야 한다.

문서/데이터만 추가한 상태는 완료가 아니다.


## 10. First production slice binding — 2026-09-30

The first runtime slice is now implemented as:

```text
New Drabyel story briefing
→ source-backed west-road navigation
→ roadside echo observation
→ visible common patrol
→ optional off-route Elite
→ Av'Sal outskirts reveal
→ MQ_AV01 complete
→ MQ_AV02 active
```

Route source:
- `zachaa/DrehmalMap@72d82180cbe3f950f068cf2d8e8668c6b09d5c58`
- `data/paths.geojson`
- `data/locations.json`
- reference landmarks: New Drabyel and Av'Sal Scavengers House
- source X/Z values are route seeds only; the live 26.2 world decides Y, collision safety, source-content clearance and battle footprint acceptance

Encounter direction:
- the common patrol uses existing production visuals but a new mixed tactical composition: opportunist frontliner + telegraphed marksman + field support
- the optional Elite is not a main-quest gate
- one field encounter still uses one physical representative
- no final P09~P12 visual was invented; character production remains external-asset-first

Build TURNBOUND #954 verified code commit `bf228f7de380896c8cc9c070d8c996a236f021bc`:
- tests/build: PASS
- dedicated-server smoke: PASS
- JAR/mrpack verification: PASS
- client runtime/playtest/multiplayer: NOT TESTED


## Travel pacing and map readability

- New Drabyel → Av'Sal is a first-time journey, not a route the player should be forced to walk end-to-end repeatedly.
- Discovery adds a return waypoint at 끊긴 가도 and another at 아브살 외곽.
- Active Av'Sal quests may coexist with Capital Valley/hidden objectives and project their exact known target zones onto the map.
- MQ_AV02 remains an outskirts investigation objective until its individual clue interactions are implemented; do not invent false exact clue coordinates before live-world placement.
- Future Av'Sal NPCs, enemies, midbosses and playable additions remain external-model-first and should broaden silhouettes/species rather than defaulting to humanoid-only or giant-monster-only content.
