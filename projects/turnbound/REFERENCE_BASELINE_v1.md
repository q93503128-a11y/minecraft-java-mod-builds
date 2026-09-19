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
