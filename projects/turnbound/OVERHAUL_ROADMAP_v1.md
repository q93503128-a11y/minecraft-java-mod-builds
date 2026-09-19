# TURNBOUND Overhaul Roadmap v1

> 이 문서는 대격변 작업 순서 정본이다.
> 설계 단계에서는 build/CI/JAR 검증을 돌리지 않는다.
> 문서-only commit은 `[skip ci]`를 사용한다.

## 1. Design Freeze

한 번에 확정할 것:
- 게임 정체성/루프
- P01~P08 kit
- SPD/Gauge 규칙
- 성장/장비/재화/소환
- UI 정보 구조
- Drehmal survey/배치 규칙
- camera contract
- tutorial
- cleanup contract

완료 조건:
- 구현자가 추가 기획 없이 개발을 시작할 수 있음
- 서로 모순되는 옛 정본 없음

## 2. Map Survey

첫 route topology는 `FIRST_ROUTE_CAPITAL_VALLEY_v1.md`에서 다음처럼 고정됨:
- Stasis surface roadhead
- Primal Caverns
- abandoned chapel
- Capital Valley Tower
- warning cave optional danger
- Explorer's Guide camp
- New Drabyel first hub
- Av'Sal next-major-route candidate

다음 survey는 코드보다 먼저 실제 26.2 world에서:
- exact player spawn block
- first 2 mandatory encounter footprints
- warning cave
- Drabyel entrance/service positions
- first Av'Sal approach
를 확인한다.

산출:
- semantic location data
- route sheet
- battle candidate
- camera risk
- NPC placement
- encounter placement
- `verifiedIn26_2` state

## 3. Combat Foundation

이미 시작된 영역:
- fixed-point TurnScheduler
- P01/P02 v1 runtime

남은:
- P03~P08 runtime
- boss Gauge resistance
- reaction cap review
- Auto behavior
- battle result consistency

검증:
- 관련 unit test
- 의미 있는 combat chunk 완료 후 1회 build

## 4. Battle Camera

- battle center
- terrain candidate
- dynamic distance
- wall/cliff fallback
- skill camera
- restore path

client runtime 확인 필수.

## 5. Battle UI

- primary external skin 확정
- font
- portrait pipeline
- Turn Order rail
- Party HUD
- Action buttons
- target marker
- Auto/speed strip

visual 수정 중 매번 build하지 않는다.
실제 client screenshot 검수를 우선.

## 6. Character Production

각 P01~P08:
- final/usable external visual base 또는 design reference 확정
- model
- texture
- animations
- VFX
- SFX
- portrait
- runtime kit
- Auto rule

캐릭터 하나를 “스킬 코드만 완료”로 종료하지 않는다.

## 7. World Content

Enemy placement 정본: `ENCOUNTER_ENEMY_PLACEMENT_v1.md`.

survey된 장소부터:
- road patrol
- common encounter
- NPC
- optional Elite
- Midboss
- dungeon
- regional Boss / optional World Boss
- quest
- map marker
를 한 route 단위로 완성.

첫 production survey 우선순위:
1. Capital Valley roadhead → Drabyel
2. Warning Cave Elite footprint
3. Capital Valley optional world-boss meadow 후보
4. Drabyel → Av'Sal road patrol anchors
5. Av'Sal outer-ring Elite
6. north-dock Midboss footprint
7. central-island source named-warrior handling
8. regional-boss arena candidate

## 8. Economy / Summon

- Gold flow
- equipment
- Crystal rewards
- pity
- duplicate Essence
- 3D summon presentation
- permanent exchange

server authority 필수.

## 9. Tutorial

실제 첫 route에서:
- 이동
- NPC
- 첫 전투
- target
- skill/CD
- Turn Order
- party
- equipment
- map
- summon
순으로 contextual onboarding.

## 10. Cleanup

교체가 끝나는 순간:
- old Aster code
- dead UI
- duplicate helpers
- old currencies
- old filler characters
- obsolete data
를 제거.

“나중에” 폴더에 쌓아두지 않는다.

## 11. Final Validation

마지막 단계에서 구분:
- CODE REVIEWED
- TESTED
- BUILD VERIFIED
- JAR PRODUCED
- CLIENT RUNTIME TESTED
- PLAYTESTED
- MULTIPLAYER TESTED

build 성공을 gameplay 성공으로 표현하지 않는다.
