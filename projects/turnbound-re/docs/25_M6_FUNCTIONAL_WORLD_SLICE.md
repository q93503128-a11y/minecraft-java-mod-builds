# 25 — M6 FUNCTIONAL WORLD SLICE

이 문서는 `HUB_01 -> REGION_01`의 첫 실제 Minecraft 공간 연결을 정본화한다.

이번 단계의 목적은 production 건축을 확정하는 것이 아니라, 이미 자동 검증된 전투·채집·준비·장비 시스템이 **한 월드 동선에서 실제로 이어질 수 있는지**를 닫는 것이다.

## 1. 유지하는 정본 경계

`RegionDefinition`에는 좌표를 추가하지 않는다.

- data는 `HUB_01`, `REGION_01`, exit, encounter locator, resource locator의 의미를 소유한다.
- 실제 block/entity 배치는 world/structure 쪽이 소유한다.
- 이번 `FunctionalWorldSliceLayout`의 offset은 기능 검증 harness일 뿐 production 좌표가 아니다.
- production structure로 교체할 때 locator가 유지되면 전투/보상/장비 data는 다시 작성하지 않는다.

## 2. 한 사이클 동선

현재 기능 slice는 플레이어 위치를 Hub origin으로 삼아 동쪽으로 전개된다.

```text
HUB_01
  Smithing Table + Furnace + Crafting Table
      |
      | 동쪽 길
      |
REGION_01 생활 분기
  북쪽: ore outcrop
  남쪽: riverside farm
  남동쪽: river pool
      |
  overworld patrol
      |
  rift elite
```

핵심은 메뉴 순회가 아니라 실제 이동이다.

## 3. Hub 기능

Hub에는 최소 기능 workstation만 둔다.

- Smithing Table: 기존 `EquipmentForgeService`의 물리 gate.
- Furnace: 광석 -> 실제 ingot 변환.
- Crafting Table: Minecraft-native 제작.
- Chest: 공간 기능 확인용 기본 storage 위치.

별도 장비 통화나 Hub 전용 가짜 제작 자원은 없다.

## 4. Mining 연결

`turnbound_re:region_01/ore_outcrop` 위치에는 실제 block ore를 둔다.

- Coal Ore
- Copper Ore
- Iron Ore
- Gold Ore

광석은 직접 파괴하고 vanilla loot를 얻는다.

장비 제작에 필요한 Iron/Copper/Gold는 실제 제련을 거쳐 ingot이 되어야 한다. ResourceAnchor를 클릭해 재료를 지급하는 코드는 추가하지 않는다.

## 5. Farming 연결

`turnbound_re:region_01/riverside_plot`은 실제 hydrated farmland + crop 구조다.

첫 기능 slice에서는 즉시 루프를 확인할 수 있도록 성숙 Carrot/Wheat를 배치한다.

- 수확은 vanilla crop interaction/breaking.
- Carrot은 Golden Carrot 전투 준비물 경로로 연결 가능.
- 별도 농사 경험치/통화/UI를 만들지 않는다.

## 6. Fishing 연결

`turnbound_re:region_01/river_pool`은 실제 물 블록 수역이다.

- 별도 fishing minigame을 만들지 않는다.
- vanilla fishing loot가 실제 식재료 획득 경로다.
- Cooked Cod / Cooked Salmon은 기존 Battle Preparation으로 연결된다.

## 7. Encounter 연결

동쪽 주동선에는 두 Interaction anchor를 실제로 spawn한다.

1. `turnbound_re:region_01/overworld_patrol`
2. `turnbound_re:region_01/rift_elite`

각 entity는 기존 `WorldEncounterAnchorResolver.tagFor(locator)` tag를 사용한다.

따라서 오른쪽 클릭 이후:

`Interaction entity -> server locator resolve -> preview -> confirm 재검증 -> authored battle`

기존 world-first 전투 진입 경로를 그대로 사용한다.

## 8. 미술 상태

현재 블록은 기능 구분만 위한 vanilla palette다.

- Stone Brick Hub
- Cobblestone route
- exposed quarry
- simple field
- simple water pool
- campfire/deepslate encounter landmark

이 외형은 **production visual PASS가 아니다**.

`09_WORLD_ASSET_GATE.md`에 따라 외부 환경 reference, palette, 규모, screenshot 비교를 통과하기 전에는 최종 Hub/Region 외형으로 취급하지 않는다.

## 9. Operator harness

등록 명령:

- `/turnbound_re_world_slice validate`
- `/turnbound_re_world_slice build`

`validate`는 현재 data의 Hub/Region exit와 모든 Resource/Encounter locator가 slice 계약과 맞는지 먼저 검사한다.

`build`는 검증 성공 후 현재 플레이어 발밑을 origin으로 기능 slice를 생성한다.

이 명령은 개발/통합 검수용이며 일반 플레이어 progression 진입점이 아니다.

## 10. 자동 gate

자동 검증 대상:

- production definition에서 Hub와 Region이 상호 exit.
- Mining/Farming/Fishing site가 정확한 ResourceAnchor activity로 resolve.
- 두 Encounter site가 current authored Encounter로 resolve.
- physical site offset collision 없음.
- locator collision 없음.
- Overworld 외 dimension은 world mutation 전에 거부.
- 전체 clean build/JUnit/JAR verify.

## 11. 아직 남은 것

- 실제 Minecraft screenshot으로 동선 가독성 확인.
- 광산/농장/강 규모와 실제 채집 시간 측정.
- 장비 Lv1~3 재료 요구량과 field yield 비교.
- 전투 준비물 획득 시간 비교.
- Hub/Region production 건축 reference 조사 및 visual gate.
- production non-repeatable encounter placement.
- fast travel / exploration discovery / quest hook.

사용자 방침에 따라 이 중간 기능 slice만 따로 수동 테스트를 요청하지 않는다. 월드 루프가 더 닫힌 뒤 통합 playtest에서 함께 확인한다.
