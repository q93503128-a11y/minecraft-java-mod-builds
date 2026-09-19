# TURNBOUND Encounter / Enemy Placement v1

> 역할: Drehmal 실제 공간 위에 TURNBOUND 적을 어떻게 만나는지 정의하는 정본.
> Source-backed geography와 TURNBOUND-authored gameplay는 반드시 구분한다.
> 정확한 spawn/battle 좌표는 26.2 migrated world에서 직접 확인하기 전까지 확정하지 않는다.

## 1. 핵심 목표

플레이어가 길을 걷는 동안 적을 실제로 보고 조우한다.

하지만:
- 모든 길을 몬스터 복도로 만들지 않는다.
- 일정 블록마다 기계적으로 적을 배치하지 않는다.
- 안전한 landmark까지 전투로 덮지 않는다.
- boss를 random spawn으로 뿌리지 않는다.

좋은 흐름:

```text
길을 걷는다
→ 멀리 순찰/야생 적이 보인다
→ 피할지 접근할지 판단
→ 길이 좁아지거나 폐허가 나오며 압박이 올라간다
→ 전투
→ 잠깐 안전
→ Elite/POI가 시야에 잡힌다
→ 선택적으로 도전
→ 거점/큰 landmark에서 호흡
```

## 2. Source-backed geography relevant to encounters

공식 Drehmal 자료에서 확인된 사실:

### Capital Valley
- 초반 지역은 비교적 평탄한 초원/숲/구릉.
- Stasis surface → Primal Caverns → Capital Valley Tower → Drabyel 도로가 초반 자연 동선.
- Capital Valley Tower 자체는 원본에서도 hostile entity가 지키지 않는다.
- Drabyel은 대부분의 플레이어가 처음 만나는 안전한 초기 town.

### Av'Sal
- Capital Valley 서남쪽의 거대한 폐허 도시.
- 현재 Mihkmari scavenger가 점거.
- outer ring과 central island로 공간이 나뉨.
- central island가 가장 적/콘텐츠 밀도가 높음.
- 북쪽 dock이 central island의 일반적인 접근점.
- capitol interior에 named Mihkmari Warrior 2명이 존재.

### Neighboring directions
- Palisades Heath 쪽에는 길을 따라 큰 pillager camp가 있다는 원본 안내가 존재.
- North Heartwood는 짙은 숲으로 인해 기본적으로 hostile pressure가 높은 지형.
- Hunter's Crypt는 실제 작은 dungeon으로, 원본 spawner가 존재.

이 source facts를 TURNBOUND encounter 설계의 지형 근거로 사용한다.

## 3. 적 위계

### Common
역할:
- 필드의 기본 압박
- 1~3마리 조합 중심
- 하나의 mechanic만 빠르게 읽힘

예:
- 단순 근접
- ranged
- charge
- support

### Veteran
Common의 stat inflation 버전이 아니라:
- 다른 weapon
- 다른 target priority
- 한 개 추가 mechanic
을 가진 강화형.

필드에서 실루엣으로 구분되어야 한다.

### Elite
- 고정 POI 또는 드문 field presence
- 일반 enemy group보다 확실히 강함
- unique silhouette 또는 큰 장비 차이
- optional인 경우 시야에서 미리 경고
- 첫 처치 보상 큼

### Midboss
- 길/폐허/던전의 구조적 관문
- 보통 1회 또는 느린 재전투
- 2~3개의 명확한 행동 pattern
- 전용 이름/모델 variation
- 작은 phase/전술 변화 가능

### Boss
- 장소의 핵심 갈등
- random road spawn 금지
- 최소 2 phase 또는 명확한 pattern shift
- 전용 model/animation/VFX/SFX
- 전투 전 시각적 예고
- first clear 후 월드/진행 변화

### World Boss
- 메인 route를 막지 않는 고난도 적
- 멀리서도 읽히는 실루엣
- 우회 가능
- 발견 자체가 이벤트
- 일반 respawn loop와 분리

## 4. 길 위 적 배치 규칙

### 4.1 Road Patrol
실제 길을 왕복한다.

필수:
- patrol polyline
- 시야 거리
- aggro 거리
- disengage boundary
- battle candidate set

플레이어가 적을 먼저 볼 가능성이 높아야 한다.

### 4.2 Roadside Threat
길 바로 위가 아니라:
- 수풀 가장자리
- 바위 뒤
- 언덕 아래
- 폐허 옆
에 있다가 플레이어가 접근하면 드러남.

길 한가운데 정지한 몹 여러 마리를 반복 배치하지 않는다.

### 4.3 Roadblock
의도적으로 길을 막는 encounter.

사용:
- 첫 tutorial 이후
- bandit/scavenger camp
- broken wagon
- barricade
- narrow bridge

남발 금지.
main route에서 연속 두 개 roadblock 금지.

### 4.4 Ambush
특수 encounter.

조건:
- 지형상 시야가 실제로 끊김
- 사전 단서가 약하게 존재
- 첫 tutorial에 사용하지 않음

보이지 않는 trigger battle 남발 금지.

## 5. 안전 반경

초기 target. 실제 지형에 따라 조정.

### Player start
- 첫 30~45초 동안 강제 aggro 없음
- spawn 중심 약 40~60 block에 hostile patrol spawn 금지
- 멀리 적 실루엣이 보이는 것은 허용

### Capital Valley Tower
- 약 30~40 block breathing ring
- original no-hostile landmark 성격 유지
- roaming enemy가 ring 안까지 들어오지 않게 patrol boundary 설정

### Explorer's Guide camp
- 약 20~30 block small rest ring
- 바로 옆에서 적이 respawn하지 않음

### Drabyel
- town boundary + 약 50~80 block safety buffer
- hostile patrol은 gate/외곽에서 멈춤
- town 내부 accidental battle 금지

안전 반경은 무적 bubble이 아니라 encounter spawn/aggro policy다.

## 6. Capital Valley first-route enemy family

현재 v0.4 enemy IDs/모델은 migration source일 뿐 production visual canon이 아니다.

### CV-A · Mossback Boar
기반 후보: old E006 concept.

역할:
- 첫 wildlife melee
- 단순 charge
- readable wind-up
- 낮은 complexity

전투:
- Basic bite/body hit
- 2턴마다 charge
- charge는 소형 Gauge delay

배치:
- 길 위보다는 meadow/road shoulder
- 1~2마리
- 첫 encounter 후보

### CV-B · Road Cutthroat
기반 후보: old E004 concept.

역할:
- 인간형/약탈자 근접
- HP 낮은 대상 노림
- player에게 target priority를 가르침

배치:
- chapel 이후
- broken cart/roadside camp
- Drabyel→Av'Sal route에서 빈도 증가

### CV-C · Hill Marksman
old E002의 gameplay role만 재사용 가능.

역할:
- ranged
- 후열
- 낮은 HP
- 조준 후 강한 shot

production visual은 기존 skeleton-looking 자산을 자동 유지하지 않는다.

배치:
- 언덕/바위/폐허 옆
- melee 1~2명과 조합

### CV-D · Field Stitcher
old E005 support role 후보.

역할:
- heal/support
- 낮은 공격력
- player가 “먼저 잡아야 할 적”을 학습

첫 tutorial 전투에는 등장하지 않는다.
Drabyel 이후부터 사용.

## 7. First route exact encounter beats

### Zone 0 — TURNBOUND roadhead
Enemy:
- 없음

목적:
- world reveal
- movement
- first NPC
- landmark reading

### Zone 1 — chapel 이후 road/meadow opening
First mandatory visible encounter.

Composition:
- CV-A ×2
or
- terrain/visual quality가 더 좋은 경우 동일 archetype 2마리

규칙:
- 멀리서 보임
- bypass는 가능해도 tutorial objective 때문에 접근 유도 가능
- 4v2 footprint
- no support/ranged/DoT/revive

### Zone 2 — Capital Valley Tower approach
Enemy:
- 없음

Tower 자체는 breathing landmark로 유지.

### Zone 3 — Warning Cave
Optional Elite site.

Candidate Elite:
**Cavehorn Ravager**

Mechanic:
- heavy single hit telegraph
- charge
- repeated same-target pressure
- second pattern under 50% HP

배치:
- cave interior가 camera에 충분하면 interior
- 아니면 cave mouth/exterior clearing

보상:
- guaranteed early Heroic-equivalent equipment candidate
- Gold
- Crystal first-clear
- discovery marker

main progression 필수 아님.

### Zone 4 — Explorer's Guide camp
Enemy:
- 없음

휴식/정보/장비 decision beat.

### Zone 5 — camp → Drabyel road
Second authored road encounter.

Composition baseline:
- CV-B ×2
- CV-C ×1

목적:
- melee + ranged target priority
- 첫 encounter보다 한 단계 복잡

variation:
- 나중 repeat spawn에서는 2~3 unit pool variation 허용

### Zone 6 — Drabyel safety ring
Enemy:
- 없음

Town entrance가 전투 trigger와 겹치지 않음.

## 8. Capital Valley optional field encounters

Main road만 살아 있고 주변 초원이 죽어 있으면 안 된다.

Off-road:
- boar pair
- single veteran beast
- small raider camp
- abandoned wagon event
- roaming 2-unit patrol

단:
- map에 모든 encounter를 icon으로 미리 찍지 않음
- 플레이어가 exploration으로 발견

## 9. Capital Valley World Boss candidate

### 들이받는 왕 그라울
old B01 concept는 **optional Capital Valley field boss 후보**로 재해석한다.

위치:
- main tutorial road에서 벗어난 넓은 meadow/open hill
- Drabyel에서 너무 가깝지 않음
- 멀리서 큰 실루엣 확인 가능
- 우회 가능

금지:
- first route를 막음
- 좁은 숲/절벽 arena
- random spawn

Battle identity:
- charge lane telegraph
- phase마다 charge frequency/angle 변화
- 고정 4v1만 고집하지 않고 phase add 여부 검토
- visible charge와 hit area 일치

정확한 장소는 Capital Valley 26.2 meadow survey 후 결정.

## 10. Drabyel → Av'Sal road

공식 early-game flow가 Drabyel에서 Av'Sal을 자연스러운 다음 탐험축으로 제시하므로 이 길은 첫 본격 combat route 후보로 사용한다.

### Start
Drabyel safety buffer를 벗어난 뒤 45~75초는 scenery/readability 우선.

### Road patrol A
- CV-B ×2
- CV-C ×1

avoidable.

### Scavenger sign
Av'Sal에 가까워질수록:
- salvaged barricade
- broken camp
- wooden additions on ruins
- source-backed Mihkmari occupation을 시각적으로 읽게 함

### First Mihkmari patrol
- AV-A Scavenger Blade ×2
- AV-B Scrap Slinger ×1

여기부터 enemy family를 명확히 전환.

## 11. Av'Sal enemy family

Source-backed faction:
Mihkmari scavengers.

TURNBOUND-authored combat archetypes:

### AV-A · Mihkmari Scavenger Blade
Common melee.

- fast basic strike
- low defense
- ally가 쓰러지면 small Gauge gain

### AV-B · Mihkmari Scrap Slinger
Ranged.

- low HP
- high-Gauge target 또는 low-HP target 압박
- telegraphed aimed shot

### AV-C · Mihkmari Patchworker
Support.

- single heal
- small barrier/repair
- player target-priority enemy

### AV-D · Mihkmari Salvage Brute
Heavy common/veteran.

- slow
- high DEF
- shove/Gauge delay
- broken shield/large salvaged weapon silhouette

### AV-E · Relic Tinkerer
Veteran utility.

- old Avsohmic scrap를 사용하는 theme
- ally SPD/ATK temporary boost 또는 player Gauge interference
- raw damage는 낮음

원본 Mihkmari 외형과 충돌하면 새 TURNBOUND model을 같은 faction처럼 읽히게 디자인하되 원본 asset을 무단 복제하지 않는다.

## 12. Av'Sal spatial escalation

### Outer approach
밀도:
- 낮음~중간

Encounter:
- 2~3 unit patrol
- 플레이어가 폐허 규모를 먼저 볼 시간 확보

### Outer-ring streets
밀도:
- 중간

Encounter:
- 2~4 unit
- corner patrol
- rooftop/rubble ranged
- 1개 optional side-building encounter

모든 건물마다 적을 넣지 않는다.

### Salvage Camp Elite
고정 Elite:
**Mihkmari Salvage Captain**

Composition:
- Captain
- AV-A 또는 AV-B 1~2

Mechanic:
- subordinate command
- ally defeat 시 행동 가속
- player가 add를 먼저 정리할지 captain을 밀지 선택

공간:
- outer ring의 넓은 camp/plaza 후보

### North Dock Midboss
central island 일반 접근로의 관문.

Candidate:
**Dock Breaker**

Composition:
- Midboss 1
- support 1
- ranged 1

Mechanic:
- heavy shove/Gauge delay
- support 제거 여부가 난이도에 큰 영향
- 50% 이하에서 dock-side reinforcement 1회

전투는 실제 dock가 너무 좁으면 dock 바로 전/후 넓은 공간으로 이동.

### Central Island
Source-backed high-density zone.

Encounter pacing:
- 2~3개의 의미 있는 group
- 중간에 clear breathing pocket
- wooden Mihkmari structures 주변 patrol
- capitol staircase에 압박 증가

“건물 하나 = 전투 하나” 금지.

## 13. Named Warrior pair

Source map에는 capitol interior에 named Mihkmari Warrior 2명이 존재한다.

TURNBOUND 원칙:
1. 26.2에서 원본 entity가 실제로 어떻게 존재하는지 먼저 확인.
2. 가능하면 삭제하고 복제하는 대신 **TURNBOUND special encounter로 wrap/rebind**.
3. original loot/story trigger를 깨지 않음.
4. wrapping이 안전하지 않으면 그 방을 TURNBOUND 별도 battle로 덮지 않고 source encounter를 보존.

TURNBOUND 전투로 사용할 수 있을 경우:
- Twin Midboss encounter
- 서로 다른 역할: breaker + duelist
- 한 명이 쓰러지면 다른 한 명 pattern 변화
- boss보다 짧지만 일반 Elite보다 확실히 강함

## 14. Av'Sal regional boss

TURNBOUND-authored candidate:
**Av'Sal Salvage Overseer**

정확한 lore/name은 final visual/source integration 후 확정.

Placement candidate:
- central island의 열린 camp/plaza
- capitol/repository critical interaction을 직접 막지 않는 공간
- 4v5 camera footprint 확보 가능

Boss role:
Phase 1:
- melee command
- AV-A/AV-B small add
- player target marking

Phase 2:
- salvaged Avsohmic device 활성
- Turn Order pressure
- Gauge push/delay mechanic

Phase 3:
- add 생산 중단
- 직접 전투 강화
- clearer but faster telegraphs

First clear:
- 큰 Crystal
- guaranteed equipment
- route safety state 일부 변화
- Av'Sal fast-travel/discovery upgrade 후보
- next-region objective

Boss가 원본 Drehmal Repository 진행을 파괴하지 않도록 별도 TURNBOUND progression state로 관리.

## 15. Boss / Midboss respawn

Common:
- route/world policy에 따라 재생성

Elite:
- 10~20분 또는 area-reset 수준의 느린 재생성 후보
- first-clear bonus 1회

Midboss:
- first-clear 저장
- world에 즉시 respawn하지 않음
- repeat challenge가 필요하면 별도 interaction

Boss:
- first-clear 저장
- 세계 진행상 패배 상태 반영
- repeat는 명시적 challenge/replay로만
- 길 걷다가 다시 살아나 있는 연출 금지

## 16. Encounter density

### Capital Valley first route
- mandatory combat: 약 2개
- optional Elite: 1개
- off-road optional encounters: 2~4 후보
- world boss: 1개 후보, main path 밖

### Drabyel → Av'Sal
- road patrol: 1~2
- approach patrol: 1
- outer ring: 2~3
- fixed Elite: 1
- dock Midboss: 1
- central island: 2~3
- named pair: source integration이 안전할 때 1
- regional boss: 1

실제 travel time이 짧으면 encounter 수를 줄인다.
숫자를 맞추기 위해 전투를 추가하지 않는다.

## 17. Aggro / disengage

Field enemy:
- line-of-sight + distance
- patrol이 player를 발견하면 alert animation
- 즉시 battle screen 전환보다 짧은 alert window 허용
- player가 boundary 밖으로 충분히 이탈하면 disengage

Ambush만 예외적으로 짧은 warning.

멀티:
- server가 encounter claim 결정
- 같은 party가 같은 enemy를 두 번 lock하지 않음
- encounter에 참여할 party radius 정의
- 멀리 떨어진 party member를 강제 teleport하지 않는 방향 우선

## 18. Battle candidate selection

각 field encounter는 spawn point와 별개로 2개 이상 battle candidate를 갖는다.

선택 기준:
- ally4 + enemy5
- slope
- water/lava
- wall/tree
- ceiling
- cliff
- camera arcs
- return point
- original NPC/entity collision

적합 candidate 없음:
- battle 시작하지 않음
- encounter 위치/route를 조정

terrain flatten으로 해결하지 않는다.

## 19. Original hostile entities

Drehmal 원본 적과 TURNBOUND 적을 같은 자리에서 겹쳐 두지 않는다.

TURNBOUND-bound zone에서 선택:
- preserve source entity as ambient/source encounter
- wrap into TURNBOUND encounter
- suppress replacement only when safe/necessary

무조건 전부 삭제하는 sanitizer는 금지.

특히:
- Av'Sal Mihkmari
- named warriors
- dungeon spawners
- story-critical hostiles
는 source behavior 확인 후 결정.

## 20. Future-region placement seeds

### Palisades Heath
Source-backed:
- steep fjords/cliffs
- 큰 pillager camp가 road에 존재한다고 early guide가 경고

TURNBOUND:
- road camp를 major hostile complex / Midboss 또는 Boss 후보로 우선 조사
- cliff camera 위험 때문에 battle candidate survey 중요

### North Heartwood
Source-backed:
- 매우 짙은 숲
- 낮에도 hostile spawn pressure가 높은 지역

TURNBOUND:
- 길 위 고정 patrol보다 숲 가장자리/시야 짧은 encounter
- ambush 비중 증가
- existing P06~P08 nature/fungal enemy concepts 재검토

### Hunter's Crypt
Source-backed:
- small dungeon
- original zombie/skeleton spawners

TURNBOUND:
- optional early-mid dungeon 후보
- 원본 spawner를 그대로 두고 TURNBOUND battle까지 중첩하지 않음
- dungeon conversion 가능성은 실제 survey 후 결정

## 21. Enemy visual production

중요 enemy는 vanilla mob + particle로 끝내지 않는다.

Common:
- 최소 distinctive silhouette / texture / weapon

Elite:
- model variation 또는 별도 model
- unique idle/attack tell

Midboss/Boss:
- 전용 model
- attack animation
- telegraph
- hit reaction
- defeat
- VFX/SFX
- battle camera consideration

기존 E001~E014/B01~B05 자산은 자동 production canon이 아니다.
좋은 concept/animation/code만 선별 재사용하고 visual quality가 부족하면 교체한다.

## 22. 완료 조건

한 encounter는:
- 실제 장소 이유
- field model
- patrol/aggro
- battle candidate
- enemy composition
- telegraph
- reward
- respawn policy
- map/quest 관계
- multiplayer authority
가 연결되어야 완료다.

“몹 spawn 좌표가 있다”는 완료가 아니다.
