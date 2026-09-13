# 23 — M6 MINIMAL EQUIPMENT CONTRACT

이 문서는 Minecraft 채광/제작 재료를 TURNBOUND: RE의 장기 성장에 연결하는 첫 장비 계약을 정본화한다.

## 1. 목적

Battle Preparation은 매 Encounter마다 소비하는 단기 선택이고, 장비는 여러 전투에 걸쳐 유지되는 장기 선택이다.

```text
실제 채광
→ Minecraft 광물
→ 장비 제작/강화
→ 캐릭터 1명에게 장착
→ 전투 스탯의 작은 방향성 보정
→ 더 어려운 Encounter 도전
```

장비 때문에 별/레벨/kit/역할보다 장비 파밍이 더 중요해지는 구조는 사용하지 않는다.

## 2. Vertical Slice 슬롯 규칙

초기 장비 슬롯은 **캐릭터당 1개**다.

- 하나의 제작된 equipment id는 현재 Vertical Slice에서 하나의 고유 장비 조각으로 취급한다.
- 같은 장비 하나를 두 캐릭터가 동시에 장착할 수 없다.
- 장착/교체/해제 자체에는 비용을 받지 않는다.
- 새 장비로 교체해도 기존 장비는 보유 상태로 남는다.

초기에는 다음을 추가하지 않는다.

- 무기/갑옷/반지처럼 여러 슬롯.
- 희귀도.
- 랜덤 부옵션.
- 세트 효과.
- 내구도/수리 노동.
- 장비 전용 통화.
- SPD 장비.

필요성이 실제 플레이에서 확인되기 전에는 복잡도를 늘리지 않는다.

## 3. Data contract

`EquipmentDefinition`:

```text
id
ingredientItem
tiers[]
  level
  coinCost
  materialCount
  bonus
    hpPercent
    atkPercent
    defPercent
    poisePercent
```

규칙:

- tier는 1부터 연속이어야 한다.
- 최대 5 tier까지 허용한다. 현재 production 대표 장비는 3 tier만 사용한다.
- Coin/material 비용은 상위 tier에서 감소하지 않는다.
- 각 stat bonus는 0~20% 범위다.
- bonus는 tier가 오를 때 감소하지 않는다.
- SPD는 data field 자체가 없다.

Tier 1의 비용은 최초 제작 비용이며 Tier 2+는 강화 비용이다.

## 4. 현재 representative equipment

현재 수치는 Vertical Slice 초기 tuning이며 최종 밸런스가 아니다.

### Iron Bulwark
재료: `minecraft:iron_ingot`

- Lv1: Coin 60 + Iron 6 → DEF +4%, POISE +4%
- Lv2: Coin 100 + Iron 10 → DEF +6%, POISE +6%
- Lv3: Coin 160 + Iron 16 → DEF +8%, POISE +8%

### Copper Edge
재료: `minecraft:copper_ingot`

- Lv1: Coin 60 + Copper 6 → ATK +4%
- Lv2: Coin 100 + Copper 10 → ATK +6%
- Lv3: Coin 160 + Copper 16 → ATK +8%

### Golden Heart
재료: `minecraft:gold_ingot`

- Lv1: Coin 80 + Gold 4 → Max HP +5%
- Lv2: Coin 130 + Gold 6 → Max HP +7%
- Lv3: Coin 200 + Gold 10 → Max HP +10%

세 장비는 공격/방어/생존 방향의 최소 선택만 검증한다. 캐릭터 역할 자체를 새로 만들어내는 장비가 아니다.

## 5. Save contract

`PlayerProgress` schema 3은 다음을 분리한다.

```text
equipment: equipmentId -> EquipmentProgress(equipmentId, level)
equippedEquipment: characterId -> equipmentId
```

따라서:

- 장비 보유와 장착 대상을 분리한다.
- 기존 CharacterProgress에 장비 스탯을 영구 합산하지 않는다.
- schema 1/2 save는 equipment field가 없어도 빈 장비 상태로 decode된다.
- level/ascension/reward/encounter completion 같은 기존 progression mutation은 장비 상태를 보존해야 한다.

## 6. Battle contract

전투 시작 시 서버 저장 상태에서 현재 파티의 장비만 읽는다.

```text
CharacterDefinition + CharacterProgress
→ base progression stats
→ persistent equipment bonus
→ one-battle preparation bonus
→ immutable BattleParticipant
```

- 적은 플레이어 장비의 영향을 받지 않는다.
- 클라이언트가 equipment id나 bonus를 전투 시작 요청으로 보내지 않는다.
- 현재 definition에서 사라졌거나 tier가 유효하지 않은 저장 장비는 전투 시작을 거부한다.
- SPD는 장비로 변경하지 않는다.
- 전투 종료 후 장비가 사라지거나 소비되지 않는다.

## 7. 제작/강화 서버 transaction 경계

현재 `EquipmentProgressionService`는 순수 규칙 계층이다.

그 자체는 Minecraft inventory를 변경하지 않는다. 따라서 **이 서비스만 네트워크에 직접 노출해서는 안 된다.**

production 제작/강화 adapter는 반드시 서버에서 한 요청 안에 다음을 수행해야 한다.

1. current PlayerProgress와 현재 장비 definition을 다시 읽는다.
2. 실제 Minecraft inventory에서 요구 material 수량을 확인한다.
3. Coin과 material이 모두 충분한지 확인한다.
4. inventory material을 서버에서 정확한 수량만큼 소비한다.
5. 장비 제작/강화된 PlayerProgress를 저장한다.
6. 어느 단계든 실패하면 Coin/재료/장비 중 일부만 변경된 상태를 남기지 않는다.

실제 inventory transaction이 연결되기 전에는 player-facing 제작 버튼/패킷을 열지 않는다.

## 8. Minecraft / 외부 사례에서 가져오는 원리

### Pokémon held item
한 캐릭터가 한 개의 장비성 아이템을 선택하고, 장착 선택이 캐릭터 자체 성장과 분리되는 단순한 구조를 참고한다.

TURNBOUND: RE는 Pokémon 아이템/수치/UI를 복제하지 않고 **한 슬롯이 만드는 읽기 쉬운 선택**만 채택한다.

참고:
`https://diamondpearl.pokemon.com/en-gb/trainersguide/fundamentals/battling/`

### Minecraft Smithing Table
실제 월드의 물리 workstation에서 장비와 소재를 결합하는 흐름을 참고한다.

TURNBOUND: RE는 별도 장비 통화를 만들기보다 Minecraft material을 실제 장기 성장 sink로 사용한다.

참고:
`https://www.minecraft.net/en-us/article/minecraft-snapshot-23w04a`

## 9. 금지

- 장비 한 개가 캐릭터 originStar/level/kit 차이를 압도하는 수치.
- random affix reroll 같은 반복 관리 노동을 검증 없이 추가.
- 장비 강화 재료를 별도 TURNBOUND 광석 통화로 다시 변환.
- 장착할 때마다 Coin을 소모시켜 조합 실험을 막기.
- 클라이언트가 보낸 material count/equipment bonus를 신뢰.
- 순수 progression service만 호출해 Minecraft material을 소비하지 않고 장비를 제작.
- equipment state를 CharacterProgress에 합쳐 save migration을 불필요하게 결합.

## 10. 자동 검증 범위

- production equipment definition decode/validation.
- legacy definition bundle의 equipment 필드 부재 호환.
- tier 연속성/비용 단조성/bonus 상한.
- schema 2 → schema 3 backward decode.
- schema 3 equipment round-trip.
- 제작/강화의 Coin/material affordability와 immutable rejection.
- 한 unique 장비의 중복 장착 차단.
- 장착 교체가 무료이며 기존 장비 보유 유지.
- SPD 보존.
- 실제 authored Encounter에서 플레이어 participant에만 장비 bonus 적용.

자동 검증으로 증명하지 않는 것:

- 광물 채집 속도 대비 강화 속도의 재미.
- 장비 4~10% 수치의 최종 체감 밸런스.
- 장비 UI의 시각 품질.
- Smithing Table/Hub forge의 실제 조작감.
- multiplayer inventory race.

이 항목은 inventory transaction + production access가 연결된 뒤 통합 playtest에서 검증한다.
