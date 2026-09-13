# 22 — M6 BATTLE PREPARATION MATERIAL SINK

이 문서는 Resource Anchor에서 얻는 Minecraft 재료가 TURNBOUND 전투 준비에 실제로 쓰이는 첫 production sink를 정본화한다.

## 1. 목적

생활 활동을 별도 통화나 독립 미니게임으로 분리하지 않고 Minecraft 인벤토리와 TURNBOUND 전투 사이에 직접 선택을 만든다.

플레이어는 Encounter를 확인하기 전에 **보조손 한 칸**으로 이번 전투의 준비물을 명시적으로 선택한다.

- 일반 인벤토리를 자동 검색하지 않는다.
- 보조손에 지원하지 않는 아이템이 있으면 준비 없음으로 처리한다.
- 준비물은 서버가 Encounter preview를 만들 때 판정한다.
- Challenge 확정 순간 서버가 보조손을 다시 확인한다.
- preview 이후 준비물이 바뀌었으면 전투를 시작하지 않는다.
- 전투 등록이 성공한 뒤 선택한 준비물 1개만 소비한다.

즉, UI는 Minecraft 인벤토리를 대체하지 않고 월드 상호작용이 기존 플레이 흐름을 그대로 사용한다.

## 2. 현재 대표 준비물

초기 tuning 값이며 최종 밸런스 수치가 아니다.

| Minecraft 재료 | preparation id | 전투 효과 | 연결 활동 |
|---|---|---|---|
| Iron Ingot | `iron_reinforcement` | 파티 DEF +8%, POISE +8% | Mining |
| Golden Carrot | `golden_provision` | 파티 Max HP +10% | Farming + Crafting |
| Cooked Cod | `cooked_cod_ration` | 파티 ATK +8% | Fishing + Cooking |
| Cooked Salmon | `cooked_salmon_ration` | 파티 ATK +8% | Fishing + Cooking |

효과는 `BattleParticipant` 생성 직전에만 적용된다.

- `PlayerProgress`에는 저장하지 않는다.
- 캐릭터 레벨/별/성장 스탯을 변경하지 않는다.
- 적에게 적용하지 않는다.
- SPD를 변경하지 않아 턴 순서를 준비물 하나가 쉽게 뒤집지 않게 한다.
- 전투가 끝나면 별도 정리 없이 자연스럽게 사라지는 battle-local 값이다.

## 3. 서버 권한 경계

클라이언트가 보내는 것은 preview에서 본 `expectedPreparationId`뿐이다.

서버는 전투 시작 직전에 다시 다음을 검증한다.

1. world anchor locator/dimension/entity/range/encounter identity.
2. 현재 party와 progression.
3. 현재 보조손 아이템에서 preparation을 다시 해석.
4. preview의 expected preparation과 현재 preparation이 동일한지 확인.
5. authored battle 생성/등록.
6. 보조손에서 정확히 1개 소비.
7. 예상치 못한 소비 실패 시 방금 등록한 battle을 cleanup하고 시작 거부.

클라이언트가 preparation id를 조작해도 서버 인벤토리와 일치하지 않으면 효과를 얻을 수 없다.

## 4. UI 원칙

Encounter preview에는 현재 서버가 판정한 준비물을 한 줄로 표시한다.

새 preparation 메뉴는 만들지 않는다. 아이템 선택은 보조손에서 이미 끝난다.

표시는 vanilla item localization을 사용하고, 현재 효과와 소비량을 함께 보여준다. 실제 screenshot 품질은 M5/M6 통합 visual gate에서 확인한다.

## 5. 이 연결이 만드는 루프

```text
Region 탐험
→ 실제 채광/농사/낚시
→ Minecraft 재료 획득/조리/제작
→ 보조손으로 이번 Encounter 준비 선택
→ 서버 확정 및 1개 소비
→ TURNBOUND 전투
→ Coin/Essence/Shard 및 다음 성장
```

생활 활동이 Coin/Essence 환전소로 평탄화되지 않고, 어떤 재료를 준비할지에 따라 전투 전 선택이 달라진다.

## 6. 다음 확장

이 시스템 하나로 장비 시스템을 대체하지 않는다.

다음 M6 sink는 Mining/Crafting의 지속 성장 가치를 위해 장비 제작/강화 쪽을 연결한다. Battle Preparation은 반복 소비와 전투 전 선택을 담당하고, 장비는 장기 빌드 선택을 담당해야 한다.

## 7. 금지

- 일반 인벤토리에서 가장 좋은 준비물을 자동 소비.
- 여러 preparation을 동시에 중첩해 관리 노동 증가.
- 준비물마다 별도 통화/레벨/메뉴 추가.
- preparation 효과를 `PlayerProgress` 영구 스탯에 섞기.
- preparation 하나로 캐릭터 성장/역할 차이를 압도하는 수치 적용.
- 클라이언트가 보낸 preparation id만 믿고 효과 지급.

## 8. 검증 경계

자동 검증:

- preparation bonus의 deterministic integer scaling.
- SPD 보존.
- player participant만 보정되고 enemy participant는 원래 authored stat 유지.
- vanilla material → preparation mapping.
- unsupported item/empty selection → no preparation.
- preview/start payload가 preparation identity를 보존.
- preview identity와 confirm identity 비교 계약.

통합 playtest에서 확인:

- 보조손 선택이 실제 플레이에서 직관적인가.
- 8~10% 초기 수치가 의미는 있지만 필수 강제가 되지는 않는가.
- 실제 채광/농사/낚시 시간 대비 소비 속도가 적절한가.
- Encounter preview 한 줄이 GUI scale별로 읽히는가.
- 재료 소비가 실제 Minecraft 인벤토리에서 정확히 체감되는가.
