# 31 — M6 EQUIPMENT VISUAL ASSET GATE

## 1. 목적

TURNBOUND: RE의 장비는 gameplay 수치만 존재하고 플레이어-facing 외형을 AI가 임의로 그리는 상태로 남겨두지 않는다.

이 문서는 첫 Vertical Slice 장비 3종의 **production visual source**를 고정한다.

핵심 원칙:
- TURNBOUND가 장비 아이콘/모델/텍스처를 새로 디자인하지 않는다.
- 외부 이미지를 보고 비슷하게 다시 그리지 않는다.
- 현재 3종은 Minecraft Java 26.2에 이미 존재하는 Mojang runtime item model/texture를 직접 사용한다.
- 원본 PNG를 TURNBOUND namespace로 복사하거나 재배포하지 않는다.
- 화면에서는 vanilla `ItemStack` / item renderer가 실제 runtime asset을 그린다.
- 장비 gameplay id와 시각용 vanilla item id는 분리한다. vanilla item의 원래 소비/전투 효과를 TURNBOUND 장비가 상속하지 않는다.

## 2. Production mapping

| TURNBOUND equipment | gameplay role | production visual item | 사용 이유 |
|---|---|---|---|
| `turnbound_re:iron_bulwark` | DEF + POISE | `minecraft:shield` | 방어/버티기 역할이 즉시 읽히며 Mojang의 실제 shield model을 직접 사용 |
| `turnbound_re:copper_edge` | ATK | `minecraft:copper_sword` | Copper Age의 실제 copper weapon design을 직접 사용하여 재료와 공격 역할이 일치 |
| `turnbound_re:golden_heart` | Max HP | `minecraft:golden_apple` | 금 재료 + 생존/회복 의미가 이미 Minecraft에서 강하게 읽히는 실제 item design |

이 mapping은 **visual identity only**다.

예:
- Golden Heart를 장착했다고 Golden Apple의 vanilla food/effect를 부여하지 않는다.
- Copper Edge를 장착했다고 실제 Copper Sword durability/attack component를 BattleParticipant에 복사하지 않는다.
- Iron Bulwark의 TURNBOUND bonus는 `vertical_equipment.json`의 기존 data가 계속 정본이다.

## 3. 렌더링 계약

Equipment UI에서는 새 PNG를 추가하지 않고 Minecraft 26.2의 정식 GUI item renderer를 사용한다.

목표 경로:

```text
EquipmentDefinition visual item id
→ server-authored equipment snapshot
→ client Item registry resolve
→ ItemStack
→ GuiGraphicsExtractor.item(...)
```

26.x GUI에서는 `GuiGraphics`가 `GuiGraphicsExtractor`로 바뀌었고 item rendering entrypoint는 `item(ItemStack, x, y)`다.

구현 규칙:
- 장비 목록 행 왼쪽에 16×16 실제 item render.
- text/button hitbox와 item icon이 겹치지 않게 기존 row layout 안에서 spacing만 조정.
- selected/disabled/focus frame은 이미 채택한 Kenney UI family를 유지.
- 재료 비용을 시각화할 때도 `iron_ingot`, `copper_ingot`, `gold_ingot`의 vanilla item render를 우선한다.
- unknown/invalid visual id는 AI placeholder icon으로 대체하지 않고 fail-closed 한다.

## 4. 데이터 방향

장비 visual source는 반복 조정 가능한 콘텐츠 값이므로 최종 구현에서는 Java switch보다 definition data에 둔다.

권장 field:

```json
{
  "id": "turnbound_re:copper_edge",
  "ingredientItem": "minecraft:copper_ingot",
  "visualItem": "minecraft:copper_sword",
  "tiers": []
}
```

호환 규칙:
- 기존 definition에 `visualItem`이 없을 때는 legacy decode가 깨지지 않아야 한다.
- production 3종은 explicit `visualItem`을 반드시 선언한다.
- server snapshot이 visual item id를 제공하고 client가 임의로 equipment id → icon을 재설계하지 않는다.
- visual item id는 namespaced item id 형식 검증을 거친다.

## 5. 외부 자산 조사 결과

추가 custom pixel icon family도 조사했다.

### Kettoman — Pixel Art Icons: RPG Essentials 16×16
- URL: https://kettoman.itch.io/pixel-art-icons-rpg-essentials-16x16
- 64 icons / 16×16 / PNG.
- CC0.
- no generative AI 표기.
- weapons / food / materials / potions 포함.
- 판정: **fallback candidate**, 현재 3종 production에는 불필요.

### Shade — Free 16×16 Assorted RPG Icons
- URL: https://merchant-shade.itch.io/16x16-mixed-rpg-icons
- weapon / armour / consumable / chest 등 대규모 sprite sheet.
- CC0 1.0 Universal.
- no generative AI 표기.
- 판정: **future custom-equipment candidate**, 현재 3종 production에는 불필요.

현재는 Minecraft-native readability와 화풍 일관성, 별도 binary vendoring 불필요성 때문에 Mojang runtime item을 우선한다.

향후 전용 장비가 vanilla item으로 역할을 충분히 표현하지 못할 때만 위와 같은 실제 외부 asset family를 직접 채택한다. 그 경우 원본 파일/버전/라이선스/사용 sprite 위치를 `THIRD_PARTY_ASSETS.md`에 먼저 고정한다.

## 6. 금지

- TURNBOUND 전용 검/방패/하트 아이콘을 AI가 새로 그리기.
- 외부 RPG icon을 보고 닮은 16×16 sprite를 수동 제작.
- `minecraft:shield`를 참고해서 새 shield texture를 만드는 방식.
- 시각 item의 vanilla gameplay 수치를 TURNBOUND 장비 수치로 몰래 사용.
- 여러 외부 icon pack을 화면마다 제멋대로 혼합.
- visual source가 없는 장비를 텍스트만으로 production-final 처리.

## 7. 완료 상태

2026-09-15 기준:
- VISUAL SOURCE LOCKED: YES.
- LICENSE / SOURCE REGISTERED: YES — Mojang runtime direct use, no vendored copy.
- CUSTOM TURNBOUND ITEM ART: NONE.
- DATA `visualItem` FIELD: NOT IMPLEMENTED YET.
- EQUIPMENT UI ITEM RENDER: NOT IMPLEMENTED YET.
- SCREENSHOT VISUAL AUDIT: NOT RUN.
- PLAYTESTED: NO.

다음 코드 단위는 `visualItem` data contract → snapshot → `GuiGraphicsExtractor.item(...)` 연결과 관련 unit/contract test다.
