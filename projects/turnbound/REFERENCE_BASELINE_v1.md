# TURNBOUND Reference Baseline v1

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
