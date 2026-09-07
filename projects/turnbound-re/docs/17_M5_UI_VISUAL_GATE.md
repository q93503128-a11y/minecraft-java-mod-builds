# 17 — M5 UI VISUAL GATE

최종 갱신: 2026-09-07
상태: **PRE-IMPLEMENTATION VISUAL GATE PASS / BATTLE UI STRUCTURE AUTO GATE PASS / VISUAL QA PENDING**

이 문서는 TURNBOUND: RE의 M5 production UI를 구현하기 전후에 반드시 지켜야 하는 레퍼런스, 정보 계층, 디자인 토큰, 구조 목업, 구현 경계와 검증 상태를 정본화한다.

이 문서는 캐릭터 외형, 스킬 VFX, 월드/마을/던전 미술의 최종 디자인을 고정하지 않는다. 해당 영역은 각각 별도 visual gate를 거친다.

---

## 1. 최종 선택 방향

TURNBOUND: RE의 첫 production UI 방향은 **Minecraft-native tactical overlay**다.

의미:
- Minecraft의 3D 전투 장면을 화면의 주인공으로 남긴다.
- 전투 HUD는 화면을 덮는 대형 RPG 패널이 아니라 가장자리와 대상 주변에 필요한 정보만 배치한다.
- 턴 순서, Enemy Intent, HP, Poise, Energy, EXPOSED가 한눈에 읽혀야 한다.
- 세부 수치와 긴 설명은 hover/selection/tooltip로 단계적으로 드러낸다.
- 시각 언어는 Minecraft GUI sprite/nine-slice 및 동일 계열 픽셀 그래픽에 맞춘다.
- proprietary 게임 UI의 프레임/아이콘/화면을 그대로 복제하지 않는다.

이 방향은 `AGENT_RULES.md`와 공용 `QUALITY_STANDARD.md`의 "레퍼런스 → 정보계층 → 디자인 시스템 → 목업 → 구현" 순서를 따른다.

---

## 2. 레퍼런스 조사 세트

### 2.1 상용 게임

| ID | Reference | 주로 본 문제 | 채택 원리 | 그대로 가져오지 않는 것 |
|---|---|---|---|---|
| UI-R01 | Persona 5 Royal | 행동 선택의 즉시성 | 현재 actor 주변에서 선택 가능한 행동을 강하게 구분하고 버튼/키 입력을 바로 이해시키는 방식 | 비대칭 만화 컷, 고유 red/black 그래픽, 원형 command composition |
| UI-R02 | OCTOPATH TRAVELER II | 약점/Break/turn 정보의 동시 노출 | 적의 약점과 Break 계열 게이지, 다음 행동 순서를 서로 분리하되 한 시야에서 읽히게 함 | 원작 BP/Shield 아이콘·레이아웃·폰트 복제 |
| UI-R03 | Honkai: Star Rail | SPD 기반 turn order | 좌측 세로 Action Order가 전투 계획의 상시 기준점이 되는 구조. 현재 선택 action은 별도 영역에서 크게 읽힘 | 모바일/가챠식 거대 캐릭터 portrait, 원작 아이콘/색 |
| UI-R04 | Metaphor: ReFantazio | 행동 메뉴와 battlefield 공존 | 명령은 화면 한쪽에 강하게 모으되 적/캐릭터와 전장을 계속 볼 수 있게 함 | 고유 brush/typography/white-red visual identity |
| UI-R05 | Slay the Spire | Enemy Intent | 적이 다음에 무엇을 할지 아이콘+수치로 즉시 파악하게 하고 세부 설명은 hover로 넘김 | 카드 전투 레이아웃, 카드 프레임 |
| UI-R06 | Darkest Dungeon II | 4인 파티 상태와 action tray | party 상태, status, action availability를 하단에 응축하고 선택 캐릭터만 강하게 강조 | 고딕 프레임/장식/원작 토큰 |
| UI-R07 | Pokémon Scarlet/Violet | 짧은 battle command flow | Fight 계열 진입 후 move list가 간결하게 바뀌고 effectiveness를 action row에서 바로 확인 | 포켓몬 전용 메뉴 구조/테라스탈 연출 |
| UI-R08 | Into the Breach | 결과 예측/전술 정보 우선 | 장식보다 행동 결과를 읽는 정보가 우선이며, 보조 정보는 hover/inspection으로 제공 | 격자 전술게임의 보드 UI 자체 |
| UI-R09 | Clair Obscur: Expedition 33 | 턴제 전투의 현장감 | UI가 전장을 가리는 대신 3D 장면과 선택 타이밍을 보조하는 원리 | QTE/parry를 M5 핵심에 도입하지 않음 |

주요 조사 링크:
- Persona 5 Royal: https://persona.atlus.com/p5r/
- OCTOPATH TRAVELER II: https://www.square-enix-games.com/games/octopath-traveler-ii
- Honkai: Star Rail UI catalog: https://interfaceingame.com/games/honkai-star-rail
- Honkai: Star Rail combat UI explanation: https://www.hoyolab.com/article/17984000
- Metaphor: ReFantazio: https://metaphor.atlus.com/
- Slay the Spire UI catalog: https://interfaceingame.com/games/slay-the-spire/
- Into the Breach UI catalog: https://interfaceingame.com/games/into-the-breach
- Clair Obscur: Expedition 33: https://www.expedition33.com/overview

### 2.2 Minecraft 구현 사례

| ID | Reference | 관찰 | 채택 원리 | 코드/자산 사용 여부 |
|---|---|---|---|---|
| MC-R01 | Cobblemon | Minecraft world 안에서 battle command와 entity 상태를 작은 overlay로 처리 | world-first HUD, compact command, party/summary 화면 분리 | 코드/자산 복사 없음 |
| MC-R02 | Cobblemon Extended Battle UI | battle log/info panel을 moveable/collapsible하게 하고, 확실히 알려진 정보만 보여줌 | progressive disclosure, revealed-information only, native texture 계열 유지 | MIT 확인. 이번 단계 실제 코드 복사 없음 |
| MC-R03 | TurnBasedMinecraftMod | `Screen` 기반 battle GUI, network와 client battle UI 책임 분리 | battle state와 presentation 분리 | MIT 저장소. 실제 코드 복사 없음 |
| MC-R04 | FTB Quests UI Overhaul / FTB-style quest UIs | vanilla 친화 texture, 간결한 navigation, hover 상태, resource-pack 교체 가능성 | pixel/nine-slice UI와 theme/resource 경계 | 자산 복사 없음 |
| MC-R05 | Questify/FTB 계열 graph UI | 큰 데이터 화면에서 zoom/filter/detail panel을 분리 | 복잡한 비전투 화면은 정보 영역을 역할별로 분리 | 코드/자산 복사 없음 |

주요 조사 링크:
- Cobblemon: https://www.cobblemon.com/
- Cobblemon Extended Battle UI: https://modrinth.com/mod/cobblemon-extended-battle-ui
- Cobblemon Extended Battle UI source: https://github.com/sveniik/CobblemonExtendedBattleUI
- TurnBasedMinecraftMod: https://github.com/Stephen-Seo/TurnBasedMinecraftMod
- FTB Quests UI Overhaul: https://modrinth.com/mod/ftb-quests-ui-overhaul
- Questify: https://modrinth.com/mod/questify

`Cobblemon Extended Battle UI`의 저장소 라이선스는 조사 시점에 MIT로 확인했다. 그래도 실제 코드를 가져오게 되는 경우 `THIRD_PARTY_ASSETS.md`에 파일 단위 기록을 먼저 남긴다.

---

## 3. 레퍼런스에서 얻은 공통 결론

### 3.1 채택
1. **전투 중 가장 중요한 것은 장식이 아니라 다음 결정이다.**
   - 현재 actor
   - 다음 turn order
   - 적 Intent
   - HP/Poise/Energy
   - 선택 action의 target/cost/효과 성격

2. **중앙 3D 전장을 최대한 비운다.**
   - 항상 켜진 큰 중앙 panel 금지.
   - HUD는 좌/우/하단 edge와 entity 근처 상태 표시에 집중.

3. **정보는 progressive disclosure한다.**
   - 항상 표시: 생존/자원/Intent/turn/핵심 status.
   - 선택 시 표시: action 설명, HP/Poise 성격, Energy cost, known affinity.
   - hover/inspect: 긴 status 설명, 상세 수치, event history.

4. **색만으로 의미를 전달하지 않는다.**
   - WEAK/RESIST/IMMUNE는 텍스트 또는 glyph를 같이 사용.
   - Intent 위험도는 shape/icon/label을 병행.
   - ally/enemy도 위치와 표식으로 구분.

5. **선택 상태는 움직임보다 형태 변화가 우선이다.**
   - border/marker/offset/label의 조합으로 확실히 보임.
   - 과한 glow, pulse, blur를 기본 피드백으로 쓰지 않음.

### 3.2 금지
- Persona/Metaphor의 proprietary visual identity 복제.
- 검은 반투명 사각형을 화면 전체에 여러 장 겹치는 generic RPG UI.
- 모든 정보를 항상 노출하는 MMO식 HUD.
- 모든 요소를 독립 card로 만드는 dashboard UI.
- 화면마다 다른 border/padding/button height.
- AI가 임의로 그린 최종 아이콘/프레임/폰트.
- 개발 편의 때문에 action/target을 텍스트 버튼 목록으로 영구 고정.

---

## 4. Battle HUD information hierarchy

| Priority | 정보 | 표시 규칙 |
|---|---|---|
| P0 | 현재 actor / 입력 가능 여부 | 항상. actor가 바뀌면 즉시 강조 위치가 이동 |
| P0 | player 4인 HP / Energy | 항상. 생존과 Burst 판단의 핵심 |
| P0 | enemy HP / Poise / Intent | 활성 enemy는 항상. Poise와 Intent를 HP보다 작은 보조 정보로 숨기지 않음 |
| P0 | turn order | 항상. 좌측 고정 rail |
| P0 | 선택 가능한 Basic / Skill / Guard / Burst | player actor turn에 항상 |
| P0 | 현재 target | world highlight + HUD marker 병행 |
| P1 | selected action Energy cost / target rule / HP·Poise 성격 | action hover/selection 시 |
| P1 | known WEAK / RESIST / IMMUNE | 해당 정보가 발견된 target에만 |
| P1 | EXPOSED 남은 창 | EXPOSED 중 항상 |
| P1 | 핵심 timed status | participant bar 근처 icon + remaining turns |
| P2 | status 전체 설명 / action 상세 수치 | tooltip/inspect |
| P2 | battle log | 기본 collapsed. 필요 시 확장 |
| P3 | battle id / seed / revision | production 기본 숨김. debug overlay에서만 |

---

## 5. Party / Character / Growth information hierarchy

### Party Formation

| Priority | 정보 |
|---|---|
| P0 | 4 active slots, 현재 squad cost / capacity |
| P0 | 선택 character의 origin/current star, level, role |
| P0 | slot 교체 결과와 cost 초과 여부 |
| P1 | HP/ATK/DEF/SPD/Poise 계열 핵심 stat |
| P1 | affinity/kit summary |
| P1 | 현재 party에서 역할 중복/빈 slot |
| P2 | passive 세부 설명, 성장 preview |
| P3 | lore/codex text |

### Character Detail / Growth

| Priority | 정보 |
|---|---|
| P0 | 이름, current/origin star, level/cap, role |
| P0 | 현재 stat과 다음 성장 결과 |
| P0 | 필요한 Coin/Essence/Shard와 부족 여부 |
| P1 | Basic/Skill/Burst/Passive summary |
| P1 | affinity와 주요 status/Poise 상호작용 |
| P2 | 상세 성장 수치, 다음 승급 preview |
| P3 | 수집/도감/설정 설명 |

---

## 6. Design tokens

### 6.1 Grid / spacing

모든 수치는 Minecraft GUI logical coordinate 기준으로 사용한다.

- `SPACE_2 = 2`
- `SPACE_4 = 4`
- `SPACE_8 = 8`
- `SPACE_12 = 12`
- `SPACE_16 = 16`
- `SPACE_24 = 24`

임의의 5/7/11/13px spacing을 새로 만들지 않는다.

### 6.2 Frame / surface

- production panel은 **GUI sprite + nine-slice**가 기본.
- border thickness는 texture가 정의하며 코드가 임의 색 rect border를 반복 생성하지 않는다.
- battle HUD에는 불투명 대형 background를 두지 않는다.
- 긴 목록/tooltip/party screen 같이 실제 surface가 필요한 곳에서만 panel을 사용한다.
- hover/selected 상태는 동일 sprite family의 state sprite를 사용한다.

Semantic tokens:
- `SURFACE_BASE`
- `SURFACE_ELEVATED`
- `SURFACE_TOOLTIP`
- `FRAME_NORMAL`
- `FRAME_SELECTED`
- `FRAME_DISABLED`
- `TEXT_PRIMARY`
- `TEXT_SECONDARY`
- `TEXT_DISABLED`
- `STATE_SUCCESS`
- `STATE_WARNING`
- `STATE_DANGER`
- `ALLY_MARKER`
- `ENEMY_MARKER`
- `WEAK_MARKER`
- `RESIST_MARKER`
- `IMMUNE_MARKER`
- `POISE_BAR`
- `ENERGY_BAR`

**중요:** 첫 구현에서 위 semantic token을 임의 hex palette로 결정하지 않는다. vanilla Minecraft/Cobblemon 계열 픽셀 UI를 참고해 만든 승인된 sprite asset의 실제 색을 token source로 사용한다. gameplay 의미가 있는 색은 icon/label과 항상 병행한다.

### 6.3 Typography

첫 production pass는 Minecraft가 이미 제공하는 font 계열을 사용하고, 별도 폰트 dependency를 추가하지 않는다.

- `TYPE_TITLE`: 화면 제목. 기본 font, 1.25x 또는 공간이 작으면 1.0x + weight/contrast.
- `TYPE_SECTION`: section label. 1.0x.
- `TYPE_BODY`: 기본 1.0x.
- `TYPE_AUX`: 보조 설명. 0.75~1.0x, 최소 가독성 유지.
- `TYPE_NUMBER`: HP/Energy/turn 수치. 숫자 폭이 안정적인 Minecraft 제공 font를 우선 검토.

규칙:
- 한글/영문에서 같은 정보 계층을 유지.
- 1280×720 + 작은 GUI scale에서도 P0 텍스트를 축약하지 않는다.
- 긴 이름은 먼저 panel width/line wrap을 조정하고 무조건 작은 폰트로 해결하지 않는다.

### 6.4 Iconography

- 한 UI 안에서 pixel icon과 vector/photoreal icon을 혼합하지 않는다.
- 가능한 경우 vanilla item/entity visual 또는 허용된 단일 pixel icon family를 사용한다.
- mob portrait가 필요할 경우 먼저 Minecraft entity render/head capture 방식의 가독성/성능을 검토한다.
- final icon art를 AI 임의 생성으로 채우지 않는다.

### 6.5 Motion

- server state가 먼저고 animation은 그 결과를 따라간다.
- hover/select transition은 짧고 interruptible.
- turn queue reorder는 새 server snapshot을 가리지 않음.
- resolve 중 UI가 다음 결과를 미리 확정한 것처럼 보이지 않음.
- 지속 pulse/blink는 Danger에서도 기본값으로 사용하지 않음.

---

## 7. Component contract

### `TurnQueueCell`
- actor portrait/glyph
- ally/enemy marker
- current-turn marker
- optional delay/intent indicator
- hover details

### `ParticipantBar`
- name or short identity
- HP
- secondary resource: ally=Energy, enemy=Poise
- status icons
- EXPOSED state marker

### `IntentBadge`
- intent icon/glyph
- risk label
- optional target marker
- hover description

### `ActionButton`
- Basic / Skill / Guard / Burst family
- icon/glyph
- keybinding from actual configured key
- Energy cost when applicable
- enabled / disabled / selected states

### `ActionTooltip`
- action name
- target rule
- HP/Poise effect summary
- Energy cost
- known affinity result for current target
- long description only after hover/inspect

### `RosterRow`
- entity identity visual
- name, current star, level
- role
- squad cost
- selected/party-slot marker

### `GrowthAction`
- current → next value
- required currency
- missing amount
- disabled reason

---

## 8. Structural mockup A — Battle HUD

이 목업은 art mockup이 아니라 **레이아웃/정보 우선순위 정본**이다.

```text
┌ TURN ORDER ┐
│ > Ally A   │                                        Enemy A  HP ━━━━━━━
│   Enemy A  │                                                POISE ━━━  [Intent]
│   Ally B   │
│   Enemy B  │                         [ Minecraft battle world ]
│   Ally C   │                    target outline / weak marker in-world
└────────────┘


 Ally A   HP ━━━━━  EN ━━━   [status]      Ally B   HP ━━━━━  EN ━━━
 Ally C   HP ━━━━━  EN ━━━   [status]      Ally D   HP ━━━━━  EN ━━━

                                             [Basic] [Skill] [Guard] [Burst]
                                             selected action summary / cost
```

Target selection mode:

```text
                         [ Minecraft battle world ]
                    Zombie · #1 TARGET    Skeleton · #2 FOCUS

 Ally A   HP ━━━━━  EN ━━━                    [취소] Skill · 대상 0/1
 Ally C   HP ━━━━━  EN ━━━                    [#1 Zombie] [#2 Skeleton] [#3 Spider]
```

Layout rules:
- turn order rail은 좌상단 edge에 고정하고 폭을 작게 유지.
- enemy 상태는 가능한 한 해당 enemy와 시각적으로 연관되는 상단/월드 인접 위치에 둔다.
- 하단 좌측은 party status, 하단 우측은 현재 actor command.
- 중앙 3D 전투 공간에 항상 켜진 큰 panel을 두지 않는다.
- target 선택 중에는 list popup보다 world entity outline/marker를 우선한다.
- **target 선택은 중앙 modal을 열지 않고 기존 하단 우측 command strip을 그대로 target chooser로 전환한다.**
- command-strip target slot의 `#N`과 같은 authoritative 대상의 world marker `#N`을 일치시킨다.
- action을 선택하면 command 영역 안에서 action 요약/선택 수/취소/확정/페이지 이동을 처리하고, 취소하면 다시 action command로 돌아간다.
- 대상 후보와 번호 순서는 현재 server snapshot의 eligible target 순서를 사용하며 client가 주변 엔티티를 검색해 추측하지 않는다.

---

## 9. Structural mockup B — Party Formation / Character

```text
┌ Party Formation ─────────────── Squad Cost 9 / 12 ────────────────┐
│ [Overview] [Skills] [Growth]                                      │
├ ROSTER ───────────────┬ ACTIVE PARTY ──────────┬ SELECTED ─────────┤
│ Zombie    ★2 Lv30 C2  │ [1] Zombie     C2      │  [entity preview] │
│ Skeleton  ★3 Lv40 C3  │ [2] Skeleton   C3      │  Zombie           │
│ Spider    ★2 Lv28 C2  │ [3] Spider     C2      │  ★★→★★★  Lv30/40 │
│ Creeper   ★4 Lv50 C4  │ [4] Witch      C2      │  Role / Affinity  │
│ Witch     ★2 Lv30 C2  │                        │  HP ATK DEF SPD    │
│ ... scroll ...        │ drag/click replace     │  Basic/Skill/Burst │
│                       │                        │  [Growth Preview]  │
└───────────────────────┴────────────────────────┴───────────────────┘
```

Layout rules:
- roster는 작은 독립 card grid가 아니라 빠르게 scan 가능한 row/list가 기본.
- active party 4 slot과 squad cost를 한 시야에서 본다.
- 선택 character detail은 오른쪽 고정 pane에서 갱신한다.
- Overview/Skills/Growth는 같은 character context를 유지한 채 detail pane 내용만 바꾼다.
- 성장 버튼은 결과와 비용을 같은 영역에 보여준 뒤 실행한다.
- squad cost 초과는 저장 시점이 아니라 조합 시점에 바로 이유를 표시한다.

---

## 10. Required states

최소 아래 상태를 구현/스크린샷 검증한다.

Battle:
- idle/non-player turn
- player actor ready
- action hover
- action selected
- target selection
- disabled action / insufficient Energy
- resolving / input locked
- known WEAK
- RESIST/IMMUNE
- enemy EXPOSED
- dangerous Intent
- multiple statuses
- server stale/rejected command feedback
- battle reward/result transition

Party/Growth:
- empty slot
- full party
- squad cost exactly cap
- squad cost over cap attempt
- locked character
- unlocked level 1
- max level for current star
- ascend available
- ascend blocked by currency/shard
- long Korean name/description
- long English name/description

---

## 11. Minecraft 26.2 / NeoForge implementation choice

첫 production pass는 **Vanilla/NeoForge Screen + GUI sprite atlas**를 사용한다.

근거:
- NeoForge 공식 Screen API는 GUI scale에 따른 상대 좌표를 전제로 한다.
- `blitSprite`와 GUI sprite scaling metadata의 `nine_slice`를 사용할 수 있다.
- scissor, tooltip, input listener를 기본 제공한다.
- 현재 요구사항은 graph editor나 고급 data binding이 아니므로 LDLib2 같은 대형 UI dependency를 넣을 이유가 없다.

공식 문서:
- https://docs.neoforged.net/docs/rendering/screens/

초기 구현 경계:
- battle HUD: Screen/overlay presentation layer
- party/character/growth: standalone Screen
- layout 계산은 pure helper로 분리해 해상도/GUI-scale test 가능하게 함
- client는 snapshot/event를 표시하고 command intent만 server에 보냄
- reward/progression 결과를 client가 계산하지 않음
- target world marker는 protocol v5 snapshot의 `participant → entity UUID` binding만 사용한다.
- target legality/eligible list/order는 server snapshot이 권위이며 client proximity scan으로 보완하지 않는다.
- marker cache는 `battleId + revision`에 귀속시키고 stale snapshot에서는 표시하지 않는다.

예상 resource 경로:
```text
assets/turnbound_re/textures/gui/sprites/
assets/turnbound_re/textures/gui/sprites/battle/
assets/turnbound_re/textures/gui/sprites/party/
assets/turnbound_re/textures/gui/sprites/common/
```

실제 sprite를 추가할 때 texture/license/source를 별도 기록한다.

---

## 12. 첫 구현 순서

현재 진행 상태:

1. `UiLayoutMetrics` / semantic token contract. — **구조 구현 완료**
2. read-only `BattlePresentationModel`. — **구현 완료**
3. turn queue + participant bars. — **첫 구조 구현 완료**
4. Intent/Poise/EXPOSED 표시. — **첫 구조 구현 완료**
5. Basic/Skill/Guard/Burst command strip. — **첫 구조 구현 완료**
6. action tooltip / target highlight / disabled reason. — **첫 구조 구현 완료**
   - protocol v5 participant entity UUID binding
   - world `TARGET / FOCUS / SELECTED` marker
   - world marker와 command-strip target slot의 동일 `#N` 번호
   - 중앙 target modal 제거
   - multi-target confirm/cancel/pagination 유지
7. Party Formation screen skeleton. — **미착수**
8. Character Overview / Skills / Growth detail pane. — **미착수**
9. resource sprite/nine-slice 적용. — **최종 품질 미확정**
10. 실제 Minecraft screenshot audit. — **미실행**

처음부터 예쁜 texture를 만들기보다 **정본 layout과 interaction이 실제 Screen에서 맞는지 먼저 확인**하고, 그 다음 승인된 sprite asset을 입힌다.

---

## 13. Visual QA gate after implementation

자동:
- 480×270 최소 supported logical canvas.
- 640×360 narrow layout.
- 1280×720 logical layout bounds.
- 1920×1080 logical layout bounds.
- target chooser가 `commandStrip`과 정확히 같은 영역을 재사용하는지 검사.
- target chooser가 center `reservedWorldViewport`를 침범하지 않는지 검사.
- authoritative eligible target 순서와 world marker `#N` 번호 일치 계약.
- null entity binding은 world marker를 생성하지 않음.
- battleId/revision stale marker 거부.
- selected marker가 hovered marker보다 우선함.
- long Korean/English strings.
- action disabled/selected state conflict.

2026-09-07 자동 게이트:
- commit: `327721b7f9030da3fc2b115b58192ba411561047`
- workflow: `Build turnbound-re` Run `34115850056`
- Java 25 / Gradle 9.2.1 / NeoForge 26.2.0.38-beta
- dependency resolution + clean build + JUnit: **PASS**
- production JAR verify: **PASS**
- JAR: `turnbound_re-0.1.0-alpha.1.jar`
- JAR SHA-256: `7afee7168c920c5fcfa28fab29ac40ff8b012e3385aa52321b876a5976f892c3`

수동 screenshot:
- GUI scale 여러 단계.
- 1280×720 최소 기준.
- 1920×1080.
- battle ready / action hover / target / EXPOSED / dangerous Intent.
- command strip → target chooser 전환 시 기존 HUD와 시각 중복/겹침 여부.
- world `#N` marker와 하단 target slot의 실제 시선 이동/가독성.
- Party Formation / Growth.

각 screenshot에서 기록:
- reference와 비교한 정보계층.
- mockup과 실제 bounds 차이.
- center world visibility.
- text clipping.
- selection/disabled 가시성.
- icon/style consistency.

실제 화면 비교 전에 M5를 production visual PASS로 선언하지 않는다.

---

## 14. 현재 판정

### PRE-IMPLEMENTATION VISUAL GATE: PASS
완료:
- battle HUD 상용 reference 8개 이상 비교.
- party/character reference 포함.
- Minecraft 실제 UI/mod 사례 비교.
- 채택/금지 원리 정리.
- Battle/Party information hierarchy.
- spacing/typography/surface/component token contract.
- Battle HUD structural mockup.
- Party Formation structural mockup.
- NeoForge implementation feasibility 확인.

### BATTLE UI STRUCTURE AUTO GATE: PASS
현재 자동 검증까지 완료된 첫 production 구조:
- world-first battle HUD 영역 분리.
- server-authoritative action/target presentation.
- protocol v5 participant entity UUID 전달.
- 실제 server binding이 있는 엔티티만 world target marker로 사용.
- `TARGET / FOCUS / SELECTED` 텍스트+색 상태.
- eligible target 순서 기반 `#N` world marker.
- 하단 target slot과 world marker의 동일 번호 대응.
- 중앙 target-selection popup 제거.
- 하단 우측 `commandStrip`을 action 선택 후 target chooser로 재사용.
- single-target 즉시 실행, multi-target 선택/확정, 취소, pagination 유지.
- target chooser의 reserved world viewport 비침범 자동 계약.
- stale battle/revision marker 차단.
- Java 25 clean build/JUnit/JAR verify 통과.

### 아직 PASS가 아닌 것
- 실제 production sprite/icon asset quality.
- 실제 Minecraft 구현 screenshot quality.
- GUI scale별 실제 체감과 text clipping.
- command picker/target chooser와 항상 렌더되는 HUD의 실제 시각적 중복 여부.
- animation timing 체감.
- Party Formation / Character / Growth production UI.
- 캐릭터/VFX/world visual gate.

따라서 다음 작업은 구조를 다시 설계하는 것이 아니라 **현재 Battle HUD/command chooser를 screenshot-ready로 정리하고 실제 Minecraft 화면에서 시각 감사**하는 것이다. 특히 target chooser가 활성화될 때 기존 command HUD와 중복 렌더되는 부분이 있는지 우선 확인·정리한 뒤, GUI scale별 screenshot gate로 넘어간다.
