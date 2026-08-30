# TURNBOUND alpha.14 — Aster March 정본 좌표 전환

alpha.13에서 전투/조우/보스 규칙을 v0.4 정본에 재정렬한 뒤, alpha.14는 임시 64×64 제작 셀을 실제 Aster March 좌표 체계에 연결한다.

## 정본 좌표 카탈로그
`AsterMarchRegionCatalog`를 추가해 v0.4의 지역 범위와 주요 Anchor를 코드 정본으로 고정한다.

주요 Fast Travel:
- FT_RADIA `(0, 66, 20)`
- FT_MEADOW `(190, 67, 230)`
- FT_GLOAM `(-40, 70, -300)`
- FT_AQUEDUCT `(-320, 67, 20)`
- FT_QUARRY `(20, 70, 405)`
- FT_RELAY `(365, 68, -305)`

Boss Gate:
- B01 `(355, 68, 245)` / yaw 90°
- B02 `(-35, 72, -440)` / yaw 180°
- B03 `(-430, 64, 35)` / yaw -90°
- B04 `(65, 63, 455)` / yaw 0°
- B05 `(430, 66, -350)` / yaw 90°

## Southgate 제작 공간
Southgate 전체 510×240을 평평하게 밀어버리지 않는다. P2에서는 정본 범위 안에 플레이에 필요한 도로·광장·전투 클리어링을 authored ribbon 형태로 만든다.

현재 연결:
`FT_RADIA → Radia South Gate → A01/A02 → FT_MEADOW → M04/M05 clearings → B01 gate → B01 arena`

A01/A02는 계속 Southgate Meadow 내부 제작 셀이다. 별도 Chapter가 아니다.

## 퀘스트/빠른 이동
- 세션 시작 위치를 FT_RADIA로 이동.
- MQ_C01_01(M01+M02) 완료 즉시 정본 보상인 FT_MEADOW를 해금한다.
- FT_MEADOW는 실제 `(190,67,230)`으로 이동한다.
- M04/M05는 확장된 초원 공간에 배치한다.
- MQ_C01_02(M04) 완료 전 B01 통로는 물리적으로 닫혀 있다.

## 고정 BattleAnchor
M04/M05/B01은 authored battle anchor를 사용한다.
특히 B01은 정본 `(355,68,245), yaw 90°`를 정확히 사용한다.

고정 Anchor의 formation/camera footprint가 막혀 있으면 주변의 다른 위치를 임의로 찾아 전투를 시작하지 않는다. 전투 시작을 취소하고 40틱(2초) grace 후 필드 조우 상태로 복귀한다. 이는 v0.4의 `BattleAnchor를 찾지 못하면 전투 시작 금지` 규칙을 따른다.

## 전투 컨트롤 잠금 표시
서버에서 이미 강제하던 캠페인 잠금을 HUD에도 명확히 반영한다.
- B01 첫 클리어 전: `AUTO 잠금`, `×2 잠금`
- B01: `도주 불가`
- 일반 필드전: `도주`
- 전투 종료 후: `복귀`

키 입력도 같은 snapshot 권한을 확인하므로 잠긴 A/X/R 입력이 의미 없는 패킷을 보내지 않는다.

## 다음 개발
alpha.14의 Southgate canonical coordinate spine이 안정화되면 다음 P2는 Chapter 2 `Gloamwood`를 정본 범위/FT_GLOAM/B02 좌표에 맞춰 시작한다. Gloamwood가 구현되었다고 이번 버전에서 간주하지 않는다.
