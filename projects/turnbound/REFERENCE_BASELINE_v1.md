# TURNBOUND Reference Baseline v1

## 0. Primary feel reference — Roblox R_PG / R_PG X

Reference only.

- https://www.roblox.com/games/10145990490/R-PG
- https://www.roblox.com/games/15205405381/R-PG-X
- Developer: 갓 스튜디오

공식 설명에서 확인되는 중심 구조:
- 마을에서 출발
- 정령/캐릭터 수집
- 오버월드 전투
- 강력한 보스 전투
- 시공간의 균열/무한 모드
- PvP / 랭크전 / 보스레이드
- 일일/서브 퀘스트와 이벤트 모드

프로젝트에서 이전에 제공된 실제 플레이 스크린샷으로 고정한 presentation reference:
- 화면 대부분을 3D 전장에 남김
- 4인 파티와 적이 실제 공간에 배치됨
- 얇은 HP/status 정보만 전장에 붙임
- 선택 적은 즉시 알아보는 target marker를 사용
- 스킬 선택은 상황형 compact menu
- AUTO/도주 같은 보조 조작은 작게 유지
- 전투 중 third-person orbit/zoom으로 캐릭터와 적을 직접 읽음

TURNBOUND 적용:
- overworld exploration → visible encounter → 4-person turn battle의 1차 감각 참고 게임
- 전투 중앙 3D 장면을 UI가 덮지 않도록 유지
- party/target/skill 정보를 필요한 순간에만 노출
- UI sprite, 캐릭터, world asset은 복제하지 않음
- 세부 navigation은 Pokémon / Honkai: Star Rail / OCTOPATH의 편의성 원칙을 함께 사용

### 2026-09-19 direct gameplay capture

User-provided 4m53s R_PG gameplay capture was reviewed frame-by-frame.

Observed:
- one controllable avatar freely traverses village, forest, snowfield, mine and desert spaces;
- ordinary management windows stay compact enough that the 3D world remains visible behind them;
- NPC interaction prompt appears only at close range;
- a single visible field enemy can represent a battle that expands into a larger enemy composition;
- enemy awareness is shown before battle with a concise exclamation-style alert;
- encounter transition is short: field contact → battle party deployment, without a preparation-menu detour;
- four allied battle characters occupy the 3D scene and the combat UI remains secondary;
- after combat the player returns to free traversal;
- major biome/area transitions use short location-name banners.

TURNBOUND consequence:
- common encounters default to one strong field representative unless a group silhouette materially improves readability;
- battle composition and field visual count are separate data;
- patrol movement should include pauses and varied destinations instead of mechanical point-by-point conveyor motion;
- service NPCs belong in the physical town and use proximity interaction;
- exploration remains the default screen state.

### Free-roam field rule

R_PG 공식 설명은 플레이어가 마을에서 출발해 여행하며 오버월드에서 적/보스를 만나는 구조를 명시한다.
공개 웹 검색에서는 R_PG gameplay clip이 Medal에 색인되어 있음을 확인했지만, 검색 결과만으로는 영상의 프레임 단위 세부 동작을 신뢰성 있게 검증할 수 없었다.
따라서 보이지 않은 세부 시스템은 추측하지 않고, 공식 구조 + 프로젝트에서 이미 제공된 실제 플레이 스크린샷 + 사용자 피드백을 기준으로 한다.

TURNBOUND 필드 원칙:
- 필드 이동은 자유 이동이 기본이다. 메뉴/챕터 전환을 위해 플레이어를 보이지 않는 복도나 강제 레일에 가두지 않는다.
- 마을의 상점/대장간/이동/소환은 가능한 한 실제 공간의 NPC/시설에 걸어가 상호작용한다.
- 적은 월드에서 먼저 보이고, 접근/경계/추격 뒤 전투로 전환한다.
- 전투 종료 후에는 전투 직전 필드 위치와 시선을 복원해 탐험 흐름을 끊지 않는다.
- 월드 탐험 중에는 TURNBOUND 소유 NPC가 아닌 Drehmal 원본 상호작용을 일괄 차단하지 않는다.

이 문서는 현재 대격변에서 재사용할 외부 설계/자산 참고를 한 곳에만 기록한다.
옛 alpha별 reference/delta 문서를 계속 누적하지 않는다.

## 1. Turn Gauge / SPD

### Honkai: Star Rail — Speed / Action Value
Reference only.

- https://www.hoyolab.com/article/27952530
- https://www.hoyolab.com/article/19822028

관찰:
- Speed가 높으면 단순 선공뿐 아니라 장기적으로 행동 횟수가 증가한다.
- Action Value는 대략 `10000 / Speed` 관점으로 설명된다.
- action advance는 Speed 자체와 다른 방식으로 남은 행동 거리를 조작한다.

TURNBOUND 적용:
- SPD를 premium action-economy stat으로 취급
- Turn Gauge push와 SPD buff를 같은 값으로 취급하지 않음
- timeline에 실제 future order를 보여줌
- integer pulse quantization 제거

### Epic Seven — Combat Readiness
Reference only.

- https://epic7x.com/combat-readiness-tutorial/

관찰:
- turn-order gauge 조작은 전투 흐름을 크게 바꾸므로 raw stat buff보다 높은 전략 가치를 가진다.

TURNBOUND 적용:
- Gauge push/delay의 power budget을 별도로 관리
- 보스는 blanket immunity보다 감소 상한/규칙으로 대응

## 2. Character kit / action economy

### Reverse: 1999
Reference only.

- https://www.prydwen.gg/re1999/guides/introduction-to-the-game
- https://www.prydwen.gg/re1999/guides/beginner-guide

관찰:
- 캐릭터마다 action economy를 얼마나 소비하는지 자체가 팀 구성 요소가 된다.
- 저희귀도도 niche utility가 존재할 수 있다.
- 중복이 캐릭터의 핵심 기능을 열어야만 하는 구조가 필수는 아니다.

TURNBOUND 적용:
- 캐릭터를 단순 등급별 상위호환으로 만들지 않음
- Reaction/Summon/Gauge support는 “무료 행동”의 실제 팀 가치까지 계산
- ★1~2 fodder 폐기
- duplicate는 핵심 기능 잠금 대신 collection currency로 환원

## 3. Gacha / collection economy

### Reverse: 1999
Reference only.

- https://www.prydwen.gg/re1999/guides/introduction-to-the-game

해당 게임은 1.5% 6★ base, 70 hard pity와 10-pull 고등급 보장을 사용하는 사례다.

TURNBOUND 적용:
- 정확한 수치를 복사하지 않음
- offline/non-monetized 게임이므로 더 관대한 baseline 사용
- hard pity / 10-pull guarantee / pity carryover라는 anti-frustration 구조만 참고
- 초기 v1 simulator baseline은 ★5 2%, soft45/hard60

## 4. UI asset candidates

### Foozle RPG UI Set 1
Directly usable candidate.
- https://foozlecc.itch.io/rpg-ui-set-1
- License: CC0

용도 후보:
- meta menu
- framed detail panel
- inventory/party visual base

### Kenney Fantasy UI Borders
Directly usable candidate.
- https://kenney.nl/assets/fantasy-ui-borders
- License: CC0

용도 후보:
- 9-slice frame
- compact battle controls
- utility popup

### Kenney UI Pack / RPG Expansion
Directly usable candidate.
- https://kenney.nl/assets/ui-pack
- https://kenney.nl/assets/ui-pack-rpg-expansion
- License: CC0

기존 repository의 Kenney 조각을 무조건 유지한다는 의미가 아니다. 새 mockup에서 맞는 경우만 사용한다.

## 5. Korean font candidate

### Pretendard
Directly usable candidate after Minecraft runtime/font-provider verification.
- https://github.com/orioncactus/pretendard
- License: SIL Open Font License 1.1

목적:
- 한국어 본문 가독성
- 더 안정적인 weight hierarchy
- Minecraft 기본 font 의존 축소

font 파일을 실제로 import할 때 license text와 third-party 기록을 함께 유지한다.

## 6. World

### Drehmal: APOTHEOSIS
External authored world / separate install.
- https://www.drehmal.net/downloads
- https://wiki.drehmal.cyou/World/

공식 다운로드 페이지는 v2.2.2f와 world/resource pack 설치 경로를 제공한다.

Wiki는:
- settlements
- regions
- POIs
- story locations
을 좌표/설명과 함께 조사할 수 있는 source다.

TURNBOUND 적용:
- wiki로 후보를 찾고
- 최종 배치는 반드시 실제 migrated 26.2 world에서 검증
- wiki map/image 자체를 license 확인 없이 game asset으로 복제하지 않음

## 7. External-use rule

모든 외부 자료는 다음 중 하나로 분류한다.

- reference
- editable base
- directly usable asset
- code library
- unknown-license

reference-only 자료는 디자인 원리/정보 구조/밸런스 원리만 참고한다.
directly usable asset은 실제 license를 `EXTERNAL_ASSETS.md`에 기록한 뒤 사용한다.


## 8. UI navigation / menu ergonomics

### Pokémon Scarlet / Violet
Reference only.
- https://bulbapedia.bulbagarden.net/wiki/Menu

관찰:
- 메인 메뉴와 현재 파티가 동시에 보인다.
- 파티 정보를 별도 깊은 화면으로 숨기지 않는다.
- 자주 쓰는 행동은 짧은 메뉴와 직접 shortcut으로 접근한다.

TURNBOUND 적용:
- Root quick menu에서 현재 4인 파티를 항상 같이 표시.
- party portrait 선택 → Character Detail 직접 진입.
- 별도 Characters 루트 메뉴를 추가하지 않고 전체 roster는 Party에서 관리.
- routine action을 여러 계층으로 나누지 않음.

### Honkai: Star Rail
Reference only.
- https://www.hoyolab.com/article/18161178

관찰:
- Character 화면에서 선택한 캐릭터의 문맥을 유지한 채 상세/장비/성장 계열 정보를 이동한다.
- 캐릭터마다 반복해서 루트 메뉴로 돌아가는 구조를 피한다.

TURNBOUND 적용:
- Character Detail / Equipment / Growth에서 현재 CharacterId 유지.
- 같은 화면 계열 안에서 캐릭터를 바로 전환.
- Back 시 선택 캐릭터/tab/scroll 상태 복원.

### OCTOPATH TRAVELER II
Reference only.
- https://note.com/nekono_miru/n/n7bb394ff7438

관찰:
- 장비 선택 시 stat 변화량을 같은 화면에서 비교할 수 있다.
- equipment decision을 위해 status 화면을 반복 왕복할 필요를 줄인다.

TURNBOUND 적용:
- Equipment item highlight 시 현재 수치 → 장착 후 수치 delta를 즉시 표시.
- 장착 자체는 reversible action이므로 불필요한 확인창을 만들지 않음.
- Gold를 실제 소비하는 강화 등 irreversible/spend action만 final confirmation 사용.

### TURNBOUND navigation rule

위 UI를 시각적으로 복제하지 않는다.
공통 원칙만 가져온다:

1. party/context를 숨기지 않는다.
2. 같은 대상(Character/Quest/Item)을 보면서 화면이 바뀌어도 context를 유지한다.
3. 비교 정보는 같은 화면에 둔다.
4. routine action은 1~3단계 안에 끝낸다.
5. 시스템 코드 구조를 메뉴 구조로 그대로 노출하지 않는다.

상세 path budget과 screen relation은 `UI_DESIGN_SYSTEM.md`가 정본이다.


## 9. External implementation reference — field roaming

### GuardVillagersFabric — field patrol implementation

Direct code reference / editable algorithm base.

- https://github.com/ffggyyuufamily-tech/GuardVillagersFabric
- inspected commit: `46f6893a7c97f3b1f49b5068bbd8ee12be4760c3`
- license: CC0-1.0

Relevant source:
- `PerimeterPatrolGoal.java`: patrol destinations are varied instead of consumed in a rigid 0→1→2 loop, tiny next hops are rejected, and patrol state periodically changes.
- `GuardNavigation.java`: move requests are not blindly reissued every tick; stalled movement has explicit recovery.
- `GuardMovementSlotResolver.java`: multiple actors receive separated movement slots instead of collapsing onto one anchor.

TURNBOUND use:
- directly adapt the safe CC0 idea of varied patrol-point choice + minimum readable movement distance;
- add authored idle dwell between patrol moves so field enemies look present in the world rather than conveyor-belted;
- keep shared encounter movement server-authored and deterministic for multiplayer;
- do not import the guard command/economy/tactics systems because they do not serve TURNBOUND's loop.
