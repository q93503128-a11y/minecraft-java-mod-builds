# 33 — M5 BATTLE ACTION VISUAL ASSET GATE

Date: 2026-09-15

## 1. 목적

Battle Command와 action timeline에서 action identity를 텍스트/tooltip만으로 전달하지 않고, 현재 Minecraft Java 26.2가 이미 보유한 Mojang runtime item model/texture를 presentation-only visual identity로 사용한다.

이 문서는 gameplay 규칙 문서가 아니다. damage, targeting, energy, status, priority, legality는 `vertical_actions.json` 및 server-authoritative snapshot이 정본이다.

## 2. source contract

Source: Mojang Minecraft Java 26.2 runtime item models/textures.

Rules:
- TURNBOUND namespace에 skill icon PNG를 새로 만들지 않는다.
- vanilla item renderer를 직접 사용한다.
- item을 실제 지급하지 않는다.
- vanilla item의 gameplay effect/component를 action에 상속하지 않는다.
- unknown/new action은 generic fallback icon을 만들지 않고 icon 없이 fail-closed한다.
- 의미가 억지인 매핑은 추가하지 말고 외부 visual asset gate를 다시 연다.

Implementation:
- `BattleActionRuntimeVisuals`
- `BattleActionIdentityHud`
- shared item resolution: `RuntimeItemVisualResolver`

## 3. current active-action mapping

| character | action | runtime item |
|---|---|---|
| Zombie | Rotten Swing | `minecraft:rotten_flesh` |
| Zombie | Undead Grit | `minecraft:shield` |
| Zombie | Gravebreaker | `minecraft:iron_shovel` |
| Zombie | Relentless Horde | `minecraft:zombie_head` |
| Skeleton | Bone Arrow | `minecraft:arrow` |
| Skeleton | Pinning Shot | `minecraft:spectral_arrow` |
| Skeleton | Deadeye | `minecraft:crossbow` |
| Skeleton | Arrow Storm | `minecraft:bow` |
| Spider | Fang | `minecraft:spider_eye` |
| Spider | Venom Bite | `minecraft:fermented_spider_eye` |
| Spider | Binding Web | `minecraft:cobweb` |
| Spider | Brood Pounce | `minecraft:string` |
| Creeper | Fuse Bash | `minecraft:gunpowder` |
| Creeper | Volatile Charge | `minecraft:fire_charge` |
| Creeper | Blast Wave | `minecraft:tnt` |
| Creeper | Catastrophe | `minecraft:creeper_head` |
| Blaze | Ember Bolt | `minecraft:fire_charge` |
| Blaze | Searing Volley | `minecraft:blaze_powder` |
| Blaze | Heat Up | `minecraft:blaze_rod` |
| Blaze | Inferno Burst | `minecraft:magma_cream` |
| Witch | Splash Hex | `minecraft:splash_potion` |
| Witch | Weakening Brew | `minecraft:fermented_spider_eye` |
| Witch | Restorative Draught | `minecraft:potion` |
| Witch | Cauldron Overflow | `minecraft:cauldron` |
| Enderman | Rift Strike | `minecraft:ender_pearl` |
| Enderman | Phase Step | `minecraft:chorus_fruit` |
| Enderman | Void Rend | `minecraft:obsidian` |
| Enderman | Horizon Break | `minecraft:end_crystal` |
| Iron Golem | Iron Fist | `minecraft:iron_ingot` |
| Iron Golem | Guardian Plate | `minecraft:shield` |
| Iron Golem | Ground Slam | `minecraft:iron_block` |
| Iron Golem | Village Judgment | `minecraft:anvil` |

Passive actions are not command-picker actions and are deliberately not assigned action-picker icons in this gate.

## 4. rendering contract

### Action picker

When `BattleCommandScreen` is showing the action-selection state, `BattleActionIdentityHud` calculates the same authoritative action order and command-strip geometry and renders one 16x16 runtime item inside the existing Kenney frame above each action button.

It does not render while the target-selection state is published, so action identity icons do not collide with target UI.

### Action timeline

While `BattleActionTimelineState.Cue` is active, the same action id resolves to the same runtime item and renders as a compact framed icon at the top-center of the reserved battle viewport.

This is only visual emphasis. Timeline phase, target, hit result and damage are not inferred from the item.

## 5. fail-closed rules

The following cases show no action icon rather than inventing one:
- unknown action id,
- invalid runtime item id,
- item registry resolution failure,
- AIR resolution.

Text/action facts remain available through existing server-authored UI even when the icon path fails.

## 6. 검증 상태

- SOURCE CONTRACT REVIEWED: YES.
- CURRENT 32 ACTIVE ACTION IDS COVERED: CODE REVIEWED.
- SERVER-AUTHORITY BOUNDARY PRESERVED: CODE REVIEWED.
- BUILD VERIFIED after this batch: NO.
- CI RUN: NO (`[skip ci]`).
- SCREENSHOT AUDIT: NOT RUN.
- PLAYTESTED: NO.
- MULTIPLAYER TESTED: NO.

실제 화면 PASS 조건은 480x270 포함 GUI-scale screenshot에서 item silhouette가 action name과 함께 충분히 구분되고, stage/target marker를 가리지 않는지 확인하는 것이다.
