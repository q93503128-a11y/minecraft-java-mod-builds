# TURNBOUND World Canon — Drehmal v1

## 1. Production world

- Base: Drehmal: APOTHEOSIS v2.2.2f
- Original target: Minecraft Java 1.20.1
- TURNBOUND target: Minecraft Java 26.2 / NeoForge
- Profile: `turnbound:drehmal_apotheosis_2_2_2f`
- 원본 world/resource pack은 TURNBOUND repository에 vendoring하지 않는다.
- 별도 설치 후 TURNBOUND gameplay metadata만 bind한다.

TURNBOUND: RE는 현재 정본이 아니다.

## 2. 기본 원칙

Drehmal을 배경으로 쓰는 것이 아니라 **실제 장소를 플레이 규칙에 연결**한다.

금지:
- Aster March 재건
- 대규모 terrain flatten
- 임시 arena permanent 생성
- 좌표 먼저, 장소 확인 나중
- 동일 간격 NPC/적 배치
- 원본 map image 무단 복제

## 3. binding 안전성

- arbitrary save auto-convert 금지
- 정확한 profile marker 없으면 fail-closed
- marker는 TURNBOUND metadata만
- binding이 지형/원본 resource를 임의 수정하지 않음
- installer/migrator가 생기면 official source/hash/version 확인 후 marker

현재 seed:
- New Drabyel: 502 67 1801
- Stasis Facility: 778 31 668

이 좌표는 조사 시작점일 뿐 gameplay anchor로 자동 확정하지 않는다.

## 4. Map Survey가 모든 배치보다 먼저

각 후보 장소를 실제 migrated 26.2 world에서 확인한다.

기록:
- sourceLocationName
- center
- entrances
- exits
- roadLinks
- height range
- visibility
- safeNpcSpots
- encounterZones
- battleCandidateZones
- cameraRisks
- water/lava/cliff
- nearbyPoi
- travelTimeToNeighbors
- discoveryRule
- originalContentConflict
- verifiedIn26_2
- notes

`verifiedIn26_2=false`이면:
- 메인 퀘스트 목적지
- 자동 teleport
- boss arena
- 필수 NPC
로 사용하지 않는다.

## 5. 실제 조사 절차

1. wiki/map으로 큰 후보만 찾음
2. 26.2 월드 직접 진입
3. 입구→출구를 실제로 걸음
4. 화면 녹화/스크린샷 또는 좌표 메모
5. 길의 시야 변화 확인
6. NPC가 자연스러운 지점 표시
7. 적이 자연스러운 지점 표시
8. 전투 candidate 2~4곳 확인
9. 카메라 후방/측면 충돌 확인
10. 주변 POI 연결
11. 역할 부여
12. quest/minimap 연결

## 6. Route sheet

각 주요 길은 하나의 route sheet를 가진다.

필드:
- route id
- 시작/끝
- 실제 이동시간
- 안전/위험 구간
- 갈림길
- landmark
- npc beat
- encounter beat
- optional detour
- rest beat
- destination payoff

목표는 길을 몬스터 복도로 만들지 않는 것.

## 7. Settlement sheet

거점마다:
- 입구
- 중심 landmark
- service cluster
- narrative NPC
- ambient NPC zone
- summon access 여부
- equipment/shop
- rest
- fast travel
- 연결 route
를 기록한다.

기능 NPC를 같은 광장에 메뉴처럼 일렬 배치하지 않는다.

## 8. NPC placement

### 서비스
상인/여관/장비:
- 플레이어가 찾기 쉬움
- 실제 장소 기능과 맞음
- 너무 멀리 분산하지 않음

### 스토리
- landmark 근처
- 시야/동선에서 놓치지 않음
- 그러나 문 앞을 막지 않음

### 캐릭터 사건
- 해당 캐릭터 테마와 장소가 연결
- 개인 quest는 generic marker보다 실제 장소 의미를 우선

### ambient
- 분위기를 만들되 interaction spam 금지
- 같은 대사 반복 NPC 대량 배치 금지

## 9. NPC 움직임

중요 NPC는 필요하면:
- 작은 patrol
- 근처 object interaction
- 낮/밤 위치 변화
를 가진다.

단, quest-critical NPC가 pathfinding 때문에 사라지거나 절벽에서 떨어지지 않도록 anchor/range 제한.

## 10. Encounter zone

조우는 point가 아니라 zone/route.

필드:
- spawn set
- patrol polyline
- alert radius
- disengage boundary
- engage point
- ambush condition
- respawn policy
- nearby battle candidate
- encounter composition
- elite chance/variant 여부

필드 모델과 BattleDefinition은 같은 composition을 가리킨다.

## 11. 조우 archetype

### 순찰
도로 일부를 왕복.
멀리서 실루엣 읽힘.

### 매복
시야가 좁아지는 지형.
사전 흔적/소리 사용.

### 점거
폐허/캠프/건물.
처치 후 장소가 안전해질 수 있음.

### 둥지
동굴/숲.
지역 생태와 연결.

### 이동 강적
넓은 route.
회피 가능.

### 사건
NPC/수레/보급대 등 실제 상황과 연결.

## 12. Respawn

모든 적을 무한 즉시 respawn시키지 않는다.

초기 원칙:
- 일반 patrol: 지역 이탈/시간 후 재생성 가능
- quest encounter: 상태 저장, 완료 후 변경
- elite: 느린 respawn 또는 완료 flag
- boss: first clear 후 repeat 방식 별도

플레이어가 뒤돌면 같은 적이 바로 다시 생기는 느낌 금지.

## 13. Terrain-aware battle candidate

조우 위치 = 전투 center가 아니다.

후보 검사:
- ally4 + enemy5 formation 가능
- ground slope
- 높이 차
- water/lava
- foliage/wall
- ceiling
- cliff/drop
- camera rear arc
- camera side arc
- escape/return point
- 원본 interactive object 충돌

적합 후보가 없으면:
- 다른 candidate
- 전투 시작 유예
- 조우 자체 위치 수정
중 하나.

강제로 벽 속 전투를 시작하지 않는다.

## 14. battle center

선택된 candidate에서:
- ally centroid
- enemy centroid
- midpoint
을 battle center로 사용.

카메라/formation/UI는 이 center를 기준으로 한다.

## 15. 전투 종료 복귀

보존:
- 원 필드 position
- facing
- dimension
- encounter id
- multiplayer party state

종료 후:
- 안전한 nearby return point
- encounter defeated state
- reward
- quest update

옛 Aster fixed-return 좌표 사용 금지.

## 16. Route pacing baseline

일반 route:
- 의미 있는 visual/interaction 변화: 45~90초
- 필수 전투: 2~3분당 1회 이하
- 선택 전투/POI는 더 자주 가능

danger route:
- 1~2분 단위 압박 가능

safe connector:
- 2~4분 정도 전투 없는 구간 가능
- 대신 landmark/환경 변화 필요

숫자는 geography를 무시하고 맞추는 목표가 아니다.

## 17. 첫 플레이 구간 — Capital Valley → Drabyel

공식 Drehmal 지형 조사로 첫 route topology는 확정한다.

큰 흐름:
1. Stasis Facility 지상 출구 직후의 기존 roadhead에서 TURNBOUND 시작
2. Primal Caverns를 첫 world landmark로 읽음
3. 버려진 예배당을 지나 첫 visible encounter
4. Capital Valley Tower를 중간 landmark로 사용
5. Tower 남서쪽 warning cave를 optional Elite 후보로 사용
6. Explorer's Guide camp를 rest/information beat로 사용
7. New Drabyel을 첫 safe hub로 사용
8. 이후 Av'Sal을 첫 대형 위험 목적지 후보로 사용

정확한 block/NPC/enemy/battle coordinates는 26.2 world 직접 검수 전까지 고정하지 않는다.

상세 정본: `FIRST_ROUTE_CAPITAL_VALLEY_v1.md`.

## 18. 미니맵

원본 Drehmal map image 무단 복제 금지.

우선순위:
1. runtime terrain local map
2. TURNBOUND discovered road/landmark overlay
3. 허가된 external map asset

marker:
- settlement
- service
- quest
- dungeon
- boss
- danger
- fast travel
- character event

미발견 marker 기본 숨김.

## 19. Fast travel

unlock 조건:
- 실제 발견
- landmark activation 또는 거점 도달

배치:
- 큰 settlement
- 주요 transit landmark
- 일부 dungeon entrance

모든 2분 거리를 teleport로 줄이지 않는다.

## 20. World cleanup

대체 후 삭제 대상:
- Aster terrain builders
- block gate write
- fixed relay route
- old minimap image/data
- old quest coordinate tables
- Aster-specific camera assumptions
- 더 이상 참조되지 않는 sanitizer/spawn guard

보존:
- encounter lifecycle
- reward/save
- server authority
- generic battle transition
- reusable NPC/quest infrastructure

## 21. 적 배치 authority

적 위계/길 순찰/Capital Valley/Av'Sal encounter 정본은 `ENCOUNTER_ENEMY_PLACEMENT_v1.md`를 따른다.

핵심:
- road에도 enemy patrol을 둔다.
- Tower/camp/town은 breathing zone을 보존한다.
- Elite는 지형/POI와 연결한다.
- Midboss는 route/dungeon 관문에 둔다.
- Boss는 random spawn하지 않는다.
- original Drehmal hostile entity와 TURNBOUND encounter를 중첩하지 않는다.

## 22. 완료 기준

월드 한 구간은:
- 실제 지형 확인
- NPC 위치 검수
- encounter route 검수
- battle candidate 검수
- camera risk 검수
- minimap marker
- quest flow
- 복귀 위치
가 연결되어야 완료다.

좌표 JSON만 채운 상태는 완료가 아니다.
