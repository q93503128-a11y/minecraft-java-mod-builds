# 21 — M6 RESOURCE NODE CONTRACT

이 문서는 TURNBOUND: RE의 생활 활동을 별도 미니게임/재화 메뉴로 분리하지 않고 Minecraft world loop에 연결하기 위한 resource anchor 계약을 정본화한다.

## 1. 핵심 원칙

Resource Anchor는 **위치와 활동 종류를 식별하는 authored-world metadata**다.

Resource Anchor 자체가 아이템을 지급하지 않는다.

실제 산출은 Minecraft의 실제 상호작용이 결정한다.

- `MINING`: 실제 블록 채굴/loot.
- `FARMING`: 실제 작물 재배·수확.
- `FISHING`: 실제 물과 낚시 상호작용/loot.

따라서 `resourceAnchors`에 `reward`, `coin`, `essence`, 임의 수량 같은 필드를 넣지 않는다.

이 구조의 목적은 Minecraft를 메뉴형 채집 게임으로 대체하는 것이 아니라, 고정 RPG 월드 안에서 **어디가 어떤 생활 활동을 위해 설계된 장소인지** 안정적으로 식별하는 것이다.

## 2. Data contract

`RegionDefinition.ResourceAnchor`:

```text
id
activity
locator
```

허용 activity:

- `MINING`
- `FARMING`
- `FISHING`

`CRAFTING`은 resource node가 아니다. HUB 기능/제작 station 또는 Minecraft crafting flow가 담당한다.

Production 좌표는 data definition에 넣지 않는다. Encounter Anchor와 마찬가지로 실제 structure/world placement가 위치를 소유한다.

## 3. Identity / server boundary

Resource locator tag namespace:

```text
turnbound_re:resource=<locator>
```

resolver는 다음을 확인할 수 있어야 한다.

- locator가 production `RegionDefinition`에 실제 존재하는가.
- 현재 dimension이 authored region dimension과 일치하는가.
- locator tag가 정확히 하나인가.
- 상호작용 확인이 필요한 경로는 6-block server range boundary를 사용한다.

Encounter locator와 Resource locator는 authored world 전체에서 중복시키지 않는다. 서로 다른 시스템 prefix가 있어도 동일 logical locator를 두 기능에 겹쳐 배정하지 않는다.

## 4. 현재 representative region

`turnbound_re:region_01`은 생활 루프 기능 검증을 위해 좌표/미술을 고정하지 않은 locator 세 개를 가진다.

- `turnbound_re:region_01/ore_outcrop` — `MINING`
- `turnbound_re:region_01/riverside_plot` — `FARMING`
- `turnbound_re:region_01/river_pool` — `FISHING`

이 이름들은 gameplay locator이며 production 건축/텍스처/배치 승인을 의미하지 않는다.

## 5. 다음 연결

Resource Anchor 계약 다음 단계는 각 활동 자체를 새 통화로 변환하는 것이 아니다.

Minecraft에서 얻은 material을 기존 core loop의 sink에 연결한다.

```text
Mining material -> equipment craft / upgrade
Farming output -> food / battle preparation
Fishing output -> food / support / special crafting
Crafting -> equipment / support item
```

Coin / Essence / Character Shard의 기존 역할은 유지한다. 생활 활동마다 별도 currency를 추가하지 않는다.

## 6. 금지

- resource anchor 우클릭만으로 광물/작물/물고기를 직접 지급.
- 채광/농사/낚시를 같은 progress bar 메뉴로 평탄화.
- 모든 Minecraft material을 Coin/Essence로 환전하여 활동 차이를 제거.
- region JSON에 production 좌표를 박아 World Asset Gate와 결합.
- resource node 하나를 위해 per-tick 전역 entity/block scan 추가.
- 아직 정하지 않은 production art를 locator 구현과 함께 확정.

## 7. 검증 경계

자동 검증:

- codec backward compatibility: `resourceAnchors` 누락 시 빈 목록.
- activity allowlist.
- locator/id validation 및 중복 차단.
- Encounter/Resource locator collision 차단.
- dimension-aware resolution.
- tag ambiguity rejection.
- explicit interaction range boundary.
- production REGION_01에 MINING/FARMING/FISHING representative anchor 존재.

자동 검증으로 증명하지 않는 것:

- 실제 광산/농장/강의 시각 품질.
- 채굴/농사/낚시 동선의 재미.
- material drop 양과 최종 경제 밸런스.
- 멀티플레이 실제 체감.

위 항목은 authored world prototype과 material sink가 연결된 뒤 통합 playtest에서 검증한다.
