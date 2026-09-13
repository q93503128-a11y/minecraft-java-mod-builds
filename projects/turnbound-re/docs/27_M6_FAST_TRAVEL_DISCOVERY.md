# 27 — M6 FAST TRAVEL DISCOVERY CONTRACT

이 문서는 `HUB_01 <-> REGION_01` 첫 이동 지점의 탐험 발견/빠른 이동 계약을 정본화한다.

목표는 탐험을 건너뛰는 메뉴 순간이동이 아니라, **직접 찾아간 장소만 이후 이동 편의로 전환**하는 것이다.

## 1. 첫 Vertical Slice

현재 두 지점만 사용한다.

- `turnbound_re:hub_01/waypoint`
- `turnbound_re:region_01/waypoint`

두 지점은 서로 한 방향 목적지를 가진다. 목적지 선택 UI가 필요한 3개 이상 네트워크는 아직 열지 않는다.

## 2. 발견 규칙

1. 플레이어가 월드의 실제 이동석 Interaction에 접근한다.
2. 서버가 Interaction tag → 현재 `RegionDefinition.fastTravelAnchors`를 다시 resolve한다.
3. 서버가 거리와 현재 등록된 실제 월드 위치를 확인한다.
4. 첫 상호작용은 **그 지점만 발견**하고 이동하지 않는다.
5. 반대쪽 지점도 직접 찾아가 첫 상호작용해야 한다.
6. 두 지점을 모두 발견한 뒤 같은 이동석을 다시 사용하면 연결된 지점으로 이동한다.

따라서 Hub에서 REGION_01 이동석을 아직 발견하지 않은 상태로 원격 해금하거나 목적지 좌표를 클라이언트가 제출할 수 없다.

## 3. 상태 소유권

`FastTravelSavedData`는 두 종류의 상태를 의도적으로 분리한다.

### 월드 공용
- locator별 실제 등록 dimension / arrival position.
- structure/prototype가 배치 위치를 소유한다.
- `RegionDefinition`에는 production 좌표를 저장하지 않는다.

### 플레이어 개인
- 플레이어 UUID별 discovered locator set.
- 한 플레이어의 발견이 다른 플레이어에게 자동 해금되지 않는다.

이 분리는 추후 dedicated server/multiplayer에서도 월드 geometry와 개인 탐험 진행을 섞지 않기 위한 계약이다.

## 4. 서버 권한 검증

이동 시 서버가 모두 다시 확인한다.

- 대상이 실제 `minecraft:interaction` entity인가.
- 정확히 하나의 `turnbound_re:travel=<locator>` tag를 가지는가.
- locator가 현재 definition에 존재하는가.
- 현재 dimension이 definition과 일치하는가.
- 플레이어가 6블록 이내인가.
- Interaction 위치가 서버에 현재 등록된 waypoint 위치와 일치하는가.
- 플레이어가 source를 이미 발견했는가.
- destination도 직접 발견했는가.
- destination이 source의 authored destination인가.
- destination waypoint가 현재 월드에 등록되어 있는가.
- 플레이어가 현재 TURNBOUND 전투를 제어 중이지 않은가.

클라이언트가 좌표, unlock bool, destination 위치를 권위적으로 보내는 네트워크 경로는 만들지 않는다.

## 5. 물리 표현

첫 prototype은 작은 3×3 발판 + Lodestone + lantern pair를 사용한다.

- Hub waypoint는 forge camp 내부에서 찾기 쉬운 위치에 둔다.
- REGION_01 waypoint는 주도로 가장자리에서 보이되 광산/농장/강/Encounter landmark footprint와 겹치지 않는다.
- 실제 도착점은 Lodestone 바로 앞의 발판 안쪽이다.
- 외형은 첫 production-facing prototype이며 최종 visual PASS는 아니다.

향후 외형을 교체해도 locator가 유지되면 개인 discovery state는 유지된다.

## 6. 반복 prototype 안전성

operator가 prototype을 다른 origin에 다시 만들 수 있으므로 옛 Interaction entity가 월드에 남을 수 있다.

서버는 현재 SavedData에 등록된 waypoint 위치와 Interaction 위치를 다시 비교한다. 현재 waypoint와 맞지 않는 낡은 marker는 `UNAVAILABLE`로 거부한다.

## 7. 현재 제한

- 첫 slice는 Overworld 동일 dimension 이동만 지원한다.
- 목적지가 정확히 1개일 때만 즉시 이동한다.
- 2개 이상 발견 가능한 목적지가 생기면 현재 service는 `SELECT_REQUIRED`로 멈추며, 그때 목적지 선택 UX를 별도 설계한다.
- 전투 중 빠른 이동은 금지한다.
- 전투/퀘스트/스토리 gate를 우회하는 목적지는 data validation과 후속 access policy에서 별도 잠근다.

## 8. Prototype 설치

`/turnbound_re_world_slice prototype`

현재 operator prototype은 production-facing 월드 구조를 만든 뒤 같은 origin 기준으로 두 waypoint를 배치하고 실제 위치를 `FastTravelSavedData`에 등록한다.

이 명령은 개발/통합 검수용이며 일반 플레이어에게 제공하는 이동 메뉴가 아니다.

## 9. 자동 gate

- production definitions에서 두 waypoint가 정확히 resolve.
- 두 지점의 link가 현재 두-point slice에서 대칭.
- wrong dimension resolve 차단.
- ambiguous/malformed travel tag 차단.
- 6블록 interaction range 계약.
- world anchor registration과 player discovery 상태 분리.
- 다른 플레이어의 발견 상태 독립.
- 반복 discover idempotent.
- world anchor 위치 갱신이 discovery set을 지우지 않음.
- unresolved fast-travel destination definition validation 실패.
- production waypoint footprint가 주요 landmark를 침범하지 않음.
- clean build / JUnit / production JAR verify.

## 10. 다음 단계

다음 M6 작업은 **production non-repeatable encounter + 첫 quest hook**이다.

빠른 이동을 별도 수집 게임으로 확장하지 않는다. 첫 quest/일회성 Encounter를 현재 `Hub -> 탐험/채집 -> patrol -> elite -> reward -> Hub` 루프와 연결한 뒤, 장비/준비물/발견/이동을 포함한 통합 screenshot/playtest에서 검증한다.
