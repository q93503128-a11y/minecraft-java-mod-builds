# TURNBOUND World Canon — Drehmal Binding v1

## 1. 현재 production world

- Base world: Drehmal: APOTHEOSIS v2.2.2f
- Public target: Minecraft Java 1.20.1
- TURNBOUND target: Minecraft Java 26.2 / NeoForge
- TURNBOUND profile: `turnbound:drehmal_apotheosis_2_2_2f`
- 원본 world/resource-pack bytes는 TURNBOUND repository에 vendoring하지 않는다.
- 공식 배포본을 별도로 설치하고 TURNBOUND는 자체 marker, semantic anchors, gameplay state만 관리한다.

TURNBOUND: RE 프로젝트는 현재 정본이 아니며 의존하지 않는다. 과거에 확인한 구현이 좋은 기술적 참고였더라도 앞으로는 TURNBOUND 자체 요구사항과 현재 코드로 판단한다.

## 2. 가장 중요한 규칙

Drehmal 위에 Aster March를 다시 만들지 않는다.

금지:
- 대형 terrain flatten
- TURNBOUND 전용 긴 인공 도로 생성
- 옛 Aster 구조물 복원
- 임시 평지 arena를 map에 permanent 생성
- map을 “새 좌표가 있는 선형 corridor”로 재해석

허용:
- 기존 지형을 읽고 semantic role 부여
- gameplay entity/NPC/encounter 배치
- 비파괴 marker/interaction
- 안전한 runtime battle formation
- TURNBOUND 자체 save data

## 3. binding 안전성

- arbitrary save 자동 변환 금지
- 정확한 profile marker 없으면 gameplay shell fail-closed
- marker는 TURNBOUND metadata만 기록
- binding이 terrain/entity/resource pack을 임의 수정하지 않음
- 자동 설치/마이그레이션을 추가한다면 official source/hash/version을 검증한 뒤에만 marker 생성

현재 integration seeds:
- New Drabyel: 502 67 1801
- Stasis Facility: 778 31 668

이 값은 **map 조사 시작점**이지 즉시 player-facing safe anchor가 아니다.

## 4. Map Survey가 콘텐츠보다 먼저

NPC/적/퀘스트/fast travel을 넣기 전에 실제 migrated 26.2 world를 조사한다.

각 route/settlement/POI는 `data/turnbound/world/locations/*.json` 형태의 semantic record로 정리할 수 있다.

권장 필드:
- id
- sourceLocationName
- type
- center
- entrances
- roadLinks
- safeNpcSpots
- encounterZones
- battleCandidateZones
- cameraRisks
- nearbyPoi
- discoveryRule
- notes
- verifiedIn26_2

`verifiedIn26_2=false`인 anchor는 자동 teleport/quest critical route에 사용하지 않는다.

## 5. 배치 순서

1. 실제 지도/위키로 큰 지역과 POI 후보 파악
2. Minecraft 26.2 migrated world에서 직접 지형 확인
3. 길을 실제로 걸어서 travel time/시야/분기 확인
4. NPC 역할 배치
5. encounter zone 배치
6. battle candidate footprint 검사
7. quest objective 연결
8. minimap/worldmap discovery 연결
9. 실제 플레이로 이동 리듬 검사

좌표 표를 먼저 채우고 나중에 지형을 보는 순서를 금지한다.

## 6. NPC

NPC spawn point는:
- 발판 안정성
- 머리 공간
- 문/계단/울타리 충돌
- 플레이어 접근
- 원본 NPC/오브젝트와 겹침
- 주변 동선
을 확인한다.

NPC는 실제 역할에 맞는 위치를 가져야 한다.

## 7. Encounter

Encounter는 점 하나가 아니라 zone/route를 가진다.

- patrol polyline
- alert radius
- disengage boundary
- engage point
- optional ambush trigger
- nearby safe battle candidate set

필드 모델과 실제 BattleDefinition은 같은 적 구성을 가리킨다.

## 8. Terrain-aware battle selection

조우 위치를 그대로 arena center로 쓰지 않는다.

후보 공간마다:
- 4 ally + 최대5 enemy formation
- ground height variance
- water/lava
- wall/tree obstruction
- ceiling
- cliff/drop
- camera rear/side arc
를 검사한다.

가장 가까운 적합 후보를 선택하고, 적합 공간이 없으면 encounter를 강제로 시작하지 않는다.

전투가 끝나면 실제 필드의 안전한 원래 위치/세션으로 복귀한다.

## 9. 기존 Aster code 처리

production tick에서 이미 끊긴 old Aster builders/map/sanitizer/spawn guard는 gameplay dependency를 추출한 뒤 삭제한다.

남겨야 할 것은:
- 전투 결과
- 진행/보상
- save/network authority
- encounter lifecycle

버릴 것은:
- Aster 좌표
- block gate write
- terrain shell/build
- fixed relay route
- old minimap image/data
- old quest guide coordinates

## 10. 지도/미니맵

Drehmal 원본 지도를 무단 복제하지 않는다.

선택지:
- runtime terrain 기반 local map
- 사용 허가가 확인된 map asset
- TURNBOUND가 직접 기록한 discovered road/landmark overlay

실제 source/license가 확인되기 전에는 외부 wiki 지도 image를 게임 asset으로 넣지 않는다.

## 11. 현재 검증 상태

기존 코드 checkpoint:
- CODE REVIEWED: YES
- TESTED: YES — Gradle tests + NeoForge server smoke, Build TURNBOUND #754
- BUILD VERIFIED: YES — code checkpoint 47cd25027fe26ea27f1ce5688372ab7102f7da18
- JAR PRODUCED: YES
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

이번 v1 문서 대격변은 문서 작업이며 위 code verification 상태를 새 gameplay 검증으로 간주하지 않는다.
