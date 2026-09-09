# Minecraft Fishing Game — Preproduction

> 이 문서는 아직 정식 프로젝트명/mod id가 확정되기 전의 개발 준비 정본이다.
> 정식 프로젝트가 생성되면 내용을 `projects/<slug>/`의 기획서/PROJECT.md로 옮기고 이 문서는 인수인계 역할만 남긴다.

## 1. 게임 한 줄 정의

**낚고 → 팔고 → 더 좋은 낚싯대를 얻고 → 더 크고 희귀한 물고기를 낚으며 도감을 채우는 Minecraft 낚시 성장 게임.**

핵심은 복잡한 생활 시뮬레이션이 아니라 Roblox식으로 즉시 이해되는 반복 루프와 강한 수집/성장 체감이다.

## 2. 핵심 기둥

1. **Fishing feel** — 캐스팅, 입질, 릴링, 물고기가 실제로 끌려오는 장면이 재미있어야 한다.
2. **Collection** — 많은 어종, 크기/무게/희귀도 변형, 도감 완성 욕구.
3. **Progression** — 판매 수익으로 낚싯대가 좋아지고 이전에 힘들던 어종을 쉽게 잡게 된다.
4. **Exploration** — 바이옴/시간/날씨/수심에 따라 잡히는 물고기가 바뀐다.

초기 개발에서 이 네 축에 연결되지 않는 시스템은 추가하지 않는다.

## 3. 기본 플레이 루프

```text
낚시터 탐색
→ 캐스팅
→ 입질
→ 한 버튼 중심 릴링 미니게임
→ 물고기 포획
→ 크기/무게/희귀도 결과 표시
→ 도감 등록
→ 판매
→ 낚싯대 업그레이드
→ 더 어려운 어종/낚시 환경 도전
→ 반복
```

### 초기에는 넣지 않는 것

- 여러 종류의 재화
- 펫/알
- 가챠
- 복잡한 제작
- 룬/각성/강화 중첩
- 여러 종류의 배틀패스성 메뉴
- 반복적으로 가방을 비워야 하는 강제 인벤토리 노동

필요성이 실제 플레이에서 확인된 뒤에만 추가한다.

## 4. 낚시 조작

### Cast

- 사용 버튼을 누르고 있는 동안 캐스팅 파워가 움직인다.
- 놓으면 해당 세기로 찌를 던진다.
- 좋은 캐스트는 주로 거리/정확도에 영향을 주고 희귀도 자체를 공짜로 올려주지는 않는다.

### Bite

- 위치, 시간, 날씨, 수심, 낚싯대/추후 미끼 조건으로 서버가 유효 어종 풀을 계산한다.
- 입질 대기시간은 장비 성장으로 줄일 수 있다.

### Reel

초기안은 **한 버튼 hold/release** 방식이다.

- 버튼을 누르면 장력이 올라가고, 놓으면 내려간다.
- 물고기의 저항 패턴에 맞춰 안전 구간을 유지하면 포획 진행도가 오른다.
- 장력이 너무 높으면 줄이 끊어지고, 너무 낮으면 진행이 후퇴한다.
- 고급 낚싯대는 단순 공격력 대신 control/strength 등의 체감 차이를 만든다.

PC 조작은 마우스 한 버튼만으로 기본 낚시 사이클을 수행할 수 있게 한다.

## 5. 낚싯대 성장

초기 낚싯대 스탯은 최대 4개로 제한한다.

- **Strength** — 감당 가능한 물고기 무게/저항
- **Control** — 안전 장력 구간과 조작 안정성
- **Luck** — 희귀 어종/특수 변형 확률
- **Lure Speed** — 평균 입질 대기시간

낚싯대는 모든 수치가 조금씩 높은 단순 사다리만 만들지 않는다.
예를 들어 같은 가격대에서도 희귀어 수집용, 대형어용, 빠른 파밍용처럼 체감 차이를 줄 수 있다.

초기 vertical slice에서는 3~5개 낚싯대만 구현한다.

## 6. 물고기 콘텐츠 생산 방식

많은 어종을 전부 별도 고비용 모델로 만들지 않는다.

### 일반 어종

- 6~10개 정도의 기본 체형/rig
- texture variant
- scale/proportion variant
- 지느러미/머리/꼬리 파츠 변형
- 간단한 공용 수영 애니메이션

을 조합해 대량 생산한다.

### 희귀/전설 어종

- 전용 silhouette
- 고유 texture
- 필요 시 전용 animation/VFX/sound
- 일반 물고기와 확실히 다른 reel resistance

을 준다.

목표는 최종적으로 많은 어종을 확보하되, 첫 테스트는 적은 수의 어종으로 낚시 한 사이클의 재미부터 검증하는 것이다.

## 7. 실제 물고기 표현과 성능

모든 수백 종 어종을 항상 월드 엔티티로 스폰시키지 않는다.

**Hybrid encounter 구조**를 기본안으로 한다.

- 바닐라/일부 대표 물고기는 실제 ambient entity로 존재할 수 있다.
- 낚시 판정용 전체 어종은 data-driven species pool에서 서버가 선택한다.
- 입질 시 선택된 어종의 실제 모델/Hooked Fish 표현을 찌 주변에 생성해 물고기가 걸리고 끌려오는 장면을 보여준다.
- 포획/실패 후 임시 encounter entity는 정리한다.

이렇게 하면 물고기가 실제로 보이는 낚시 감각을 유지하면서 수백 종을 상시 AI entity로 유지하는 비용을 피할 수 있다.

## 8. 도감과 변형

각 catch는 최소 다음 정보를 가진다.

- species id
- rarity
- length
- weight
- quality/size class
- optional mutation/variant
- catch location/biome
- catch timestamp는 통계가 필요할 때만 저장

도감은 종을 처음 잡았을 때 등록되고, 개인 최고 무게/크기를 갱신한다.

초기에는 mutation을 과도하게 늘리지 않는다. 기본 수집이 충분히 재미있는지 확인한 뒤 확장한다.

## 9. 경제

초기 재화는 **돈 한 종류**만 사용한다.

```text
물고기 판매
→ 돈
→ 낚싯대/기본 낚시 장비
```

판매가는 기본 종 가치 × 무게 × 희귀/품질 계수로 계산한다.

인벤토리를 희귀어 개별 NBT 아이템으로 가득 채우는 구조는 피한다.
정식 구현에서는 server-authoritative catch storage/creel을 검토하며, 상점에서 `Sell All`과 선택 판매를 지원한다.

## 10. 지역/진행

커스텀 선형 맵을 먼저 만들지 않는다.
Minecraft Overworld의 실제 환경을 활용한다.

초기 분류 예:

- pond / river
- swamp
- warm coast / ocean
- cold water
- deep ocean
- 특수/이벤트성 환경

시간, 날씨, 수심을 일부 어종의 조건으로 사용하되 지나치게 까다로운 조합은 피한다.

접근 가능한 지역을 메뉴로 잠그기보다 더 강한 어종이 높은 Strength/Control을 요구하도록 하여 자연스럽게 장비 진행과 연결한다.

## 11. UI/UX

초기 핵심 UI는 세 종류만 둔다.

1. **Fishing HUD** — 입질/릴링 중에만 표시
2. **Catch Result** — 잡은 직후 짧게 표시
3. **Bestiary / Sell UI** — 수집 확인과 판매

검은 반투명 카드 나열식 RPG UI를 즉흥적으로 만들지 않는다.
실제 낚시 게임의 정보 계층/동선과 Minecraft 화면 가독성을 먼저 참고하고 구현한다.

## 12. Multiplayer / Essential

Essential은 **친구가 월드에 접속하기 쉽게 만드는 선택 외부 모드**로 취급한다.
게임 로직 자체는 Essential API에 의존하지 않는다.

서버가 최종 결정:

- 유효 어종 풀
- species/weight/rarity roll
- minigame 성공 여부 검증
- catch 저장
- 돈
- 구매
- 도감/개인 기록

클라이언트 담당:

- 입력 전달
- HUD
- 낚싯줄/낚싯대/물고기 시각 연출
- 안전한 예측/보간

따라서 LAN, Essential-hosted world, dedicated server에서 같은 게임 규칙을 사용한다.

## 13. 기술 스택 선택

### 목표 환경

- Minecraft Java: **26.2**
- Java: **25**
- Mod Loader: **Fabric**
- Multiplayer convenience: **Essential 26.2 Fabric (optional external mod)**

### Fabric 선택 이유

- 현재 Essential이 Minecraft 26.2 Fabric을 지원한다.
- Fabric 공식 26.2 개발 문서와 API가 준비되어 있다.
- 낚시 관련 26.2 Fabric 구현과 공개 소스 활용 후보가 있다.
- GeckoLib도 Fabric 26.2를 지원한다.

### 프로젝트 생성 시 우선 검토할 현재 버전

2026-09-09 조사 기준:

- Fabric Loader: `0.19.5` 후보
- Fabric Loom: `1.17.x` 안정 버전 계열 (`1.17.20` 후보)
- Fabric API: `0.158.0+26.2` 후보
- Gradle: `9.5.1` (Fabric 26.2 공식 안내 기준)
- Java: `25`

실제 첫 build 직전에 공식 Maven/문서에서 다시 확인해 pin한다.

### Dependency

**우선 후보**

- Fabric API
- GeckoLib — 중요한 물고기/낚싯대 연출 및 애니메이션에 필요성이 확인되면 사용

**필수 dependency로 만들지 않을 것**

- Essential — 사용자가 별도 설치하는 멀티 편의 모드

기능 하나를 위해 추가 라이브러리를 계속 늘리지 않는다.

## 14. 외부 구현/레퍼런스 사용 계획

### Simple Fishing Overhaul

- Source: https://github.com/pajicadvance/simple-fishing-overhaul
- License: MIT
- Role: **code reference / editable base candidate**
- 참고 가치:
  - charged casting
  - nearby fish interaction
  - actual fish hook concept
  - first/third-person cast animation 접근
- 그대로 프로젝트 정체성을 복제하지 않고 필요한 기술 부분만 검토한다.

### Better Fishing

- Source page: https://modrinth.com/mod/better-fishing-system
- License: ARR
- Role: **reference only**
- 참고 가치:
  - line tension HUD
  - physical fish rendering
  - rod bending/reeling feel
  - fishing-line presentation
- 코드/자산을 복제하거나 재배포하지 않는다.

### Roblox Fisch / Fish It

- Role: **gameplay/progression reference only**
- 참고 가치:
  - 한 버튼 중심 fishing interaction
  - catch → sell → rod upgrade loop
  - bestiary/rare fish hunting
  - location-driven collection
- 펫/가챠/여러 재화 등 부가 수익화 구조는 핵심 게임에 필요하지 않으면 가져오지 않는다.

외부 코드/자산을 실제 포함할 때 `THIRD_PARTY_ASSETS.md`에 원천, 라이선스, 수정 여부, 사용 위치를 기록한다.

## 15. 첫 vertical slice

첫 테스트 빌드의 목표는 콘텐츠 양이 아니라 **한 번의 낚시가 재미있는지 확인하는 것**이다.

최소 범위:

- 1개의 완전한 cast → bite → reel → catch 사이클
- 3개 낚싯대
- 12~20개 어종
- 3개 기본 fish body family
- rarity/weight/length
- 물고기가 실제로 걸려 끌려오는 표현
- 돈 1종
- 판매
- 간단한 도감
- 3개 이상의 환경별 species pool
- multiplayer-safe server-authoritative state 구조
- 즉시 테스트 가능한 명령/테스트 아이템

### 완료 판단

- 낚시 입력이 불편하지 않은가
- 기다리는 시간이 지루하지 않은가
- 물고기별 저항 차이가 느껴지는가
- 잡았을 때 시각/사운드 보상이 충분한가
- 더 좋은 낚싯대를 사고 싶은 이유가 있는가
- 희귀어를 다시 찾고 싶은가
- 멀티에서 한 플레이어의 catch가 다른 플레이어 상태를 오염시키지 않는 구조인가

첫 vertical slice가 재미없다면 어종 수를 늘리기 전에 낚시 feel과 성장 루프부터 수정한다.

## 16. 첫 구현 순서

```text
Fabric 26.2 project skeleton
→ server-authoritative fishing session state
→ charged casting
→ bite/species selection
→ reel minigame
→ hooked-fish visual
→ catch data/value
→ sell loop
→ rod progression
→ bestiary
→ small species batch
→ actual client playtest
→ 수정
```

UI와 모델은 기능 마지막에 한꺼번에 붙이지 않는다. 릴링 HUD, hook visual, catch feedback은 핵심 낚시 사이클과 함께 검증한다.

## 17. 조사 출처

- Fabric 26.2 announcement: https://www.fabricmc.net/2026/06/15/262.html
- Fabric docs: https://docs.fabricmc.net/
- Fabric API 26.2 releases: https://modrinth.com/mod/fabric-api/versions?g=26.2
- Essential 26.2 support/changelog: https://essential.gg/changelog
- Essential supported versions: https://essential.gg/wiki/retiring-unpopular-versions
- GeckoLib: https://modrinth.com/mod/geckolib
- Simple Fishing Overhaul: https://modrinth.com/mod/simple-fishing-overhaul
- Simple Fishing Overhaul source: https://github.com/pajicadvance/simple-fishing-overhaul
- Better Fishing: https://modrinth.com/mod/better-fishing-system
- Fisch Roblox listing: https://www.roblox.com/games/9050835756/Fisch
- Fish It Roblox listing/data used only as external gameplay reference
