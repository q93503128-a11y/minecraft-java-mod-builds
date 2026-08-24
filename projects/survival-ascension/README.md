# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25. Network protocol `8`.

Survival Ascension makes progression increase the physical scale of player actions, then makes infrastructure, logistics, expeditions and combat consume that larger output again.

## 0.44.0-alpha.1 — External Gear Ascension Imprint / 외부 장비 승천 각인
The content pack now has additional equipment sources, so compatible external gear can be pulled into the same Survival Ascension rarity/affix/mastery loop instead of living beside it as disconnected loot.

Use `M -> 장비 -> 승천 각인` while holding an affix-free item in the main hand. The item must be single-stack equipment and use one of Minecraft's standard item tags: sword, pickaxe, axe or hoe. This intentionally avoids direct Java dependencies on optional content mods.

The current World Ascension stage decides the initial rarity:
- Stage0 `각성` -> 정예 I / 1 affix;
- Stage1 `전설` -> 승천 II / 2 affixes;
- Stage2 `종말` -> 신화 III / 3 affixes.

Physical-material imprint costs:
- Stage0: 자수정24 + 철12;
- Stage1: 자수정48 + 다이아4 + 금16;
- Stage2: 자수정96 + 다이아8 + 네더라이트 파편2 + 메아리8.

The same storage-first logistics resolver is used: nearby usable logistics `통` are consumed first and only the shortfall comes from player inventory.

Imprint does not replace an external item with a vanilla item. Existing item components stay on the same stack. Survival Ascension adds only its nested `survivalascension_affix` CustomData and display-name layer, while retaining a stored pre-affix base name so rerolls/awakening do not stack prefixes.

Once imprinted, the existing reforge/salvage/Mythic-awakening path and activity affixes work automatically: mastery XP, tool speed, mining area/vein, woodcutting chain, harvest area, weapon damage and cleave all read the same affix data.

This supports correctly tagged current and future mod gear without hardcoding `biomesoplenty`, `tbos`, `amethyst_resonance` or other optional namespaces. Armor is deliberately not imprinted yet because the current affix vocabulary is action/tool focused rather than passive armor-stat focused.

## 0.43.0-alpha.1 — Physical Freight Railheads / 물리 화물 하역장
0.42 made real Chest Minecarts carry actual outpost stock, but a single arbitrary rail beside an outpost was enough to use the system. 0.43 makes both endpoints into small real loading yards without turning freight into an automatic train network.

Use the existing `M -> Infrastructure -> 산업 가공소 -> 물리 화물 수레` action. At both departure and arrival, the exact active outpost `통(Barrel)` anchor must have a loaded radius6 railhead containing:
- at least6 blocks in `BlockTags.RAILS`;
- at least1 Powered Rail;
- at least1 Hopper;
- at least1 Lever or Redstone Block.

The Chest Minecart's actual rail must be inside that same railhead. A Powered Rail and Hopper must be within squared distance9 of the cart rail, and a control block within squared distance16, so scattered checklist blocks do not count as a working platform.

The yard is not registered or saved. Every freight action reads the actual blocks with loaded-only and `mayInteract` checks. Break the yard and freight stops; rebuild it and freight immediately works again.

Everything important from 0.42 remains: Industrial Works + Civil Works, active owned outposts, same physical Chest Minecart, same dimension, different destination, exact source/destination `통` cluster, existing bulk whitelist, real Container capacity and partial unloading. There is still no generated freight reward or supply fee.

No auto-driving, pathfinding, virtual route, station SavedData, new packet, custom train/block/item/entity, teleport or force-load is added.

## 0.42.1-alpha.1 — Guide / Early Mastery / Logistics Priority
The in-game guide scrolls when its content is taller than the screen and explains current gameplay instead of patch history.

World progression:
- Stage0 `각성 단계` at world start;
- first Wither kill -> Stage1 `전설 단계`;
- first Ender Dragon kill -> Stage2 `종말 단계`.

Early mastery remains accelerated through Lv59 and converges to the late quadratic curve at Lv60. Approximate cumulative XP landmarks: Lv10 453, Lv30 11,610, Lv60 107,010, Lv90 379,230, Lv100 521,270.

Mining XP remains material-tiered rather than a flat valuable-ore value: Copper7/8, Iron9/10, Gold12/13, Diamond18/20, Emerald20/22, Ancient Debris24, Obsidian16 / Crying Obsidian18.

For Survival Ascension systems using shared physical logistics, consumption order is nearest usable registered/linked physical `통` first, then player inventory for the remaining amount. Vanilla crafting-table grids and Apex/Ascension Trial carried admissions remain separate.

## 0.42.0-alpha.1 — Physical Freight Relay / 물리 화물 중계
A real vanilla Chest Minecart physically carries the existing bulk-material whitelist from one owned active outpost warehouse cluster to another. Source stacks shrink only by accepted cart capacity and cart stacks shrink only by accepted destination capacity. Partial unload leaves remainder in the same cart.

The cart stores only owner UUID and origin outpost dimension/x/y/z on its own persistent entity NBT. No route SavedData, virtual cargo account, generated freight reward, auto-driving, teleport or force-load exists.

## 0.41.0-alpha.1 — Civil Works Causeways / 토목 공사소·도로 교량 시공
Stage1 `CIVIL_WORKS / 토목 공사소` consumes Stone Bricks2048 + Cobblestone1536 + Gravel1536 + Iron256 + Copper256 through nearby real logistics `통` first, then player inventory.

Its finalizable funding call requires an owned registered `통` within4 and a loaded radius6 commissioning yard containing Stone Bricks48, Scaffolding16, Iron Blocks4, Stonecutters2 and Crafting Table1.

After completion, Construction Lv60 gains `도로/교량`. The manually placed BlockItem becomes a flat three-wide deck extending forward: Lv60 3×17, Lv90 3×33, Lv100 3×49, Field Mastery 3×65. Loaded-only, real materials, protection hooks, no terrain erase or free supports.

## 0.40.0-alpha.1 — Physical Siege Breachers / 물리 공성 파괴자
Only Bastion wave4 Ravagers/Vindicators may physically break qualifying fortification. Every break requires loaded terrain, `mobGriefing`, owner `mayInteract`, entity-destroy permission and NeoForge destroy-event approval. Normal three-wave defense remains non-destructive.

## 0.39.0-alpha.1 — Physical Bastion Defense / 물리 요새 방어
A real outpost fortification ring uses radius6..12, Y-3..+4, vanilla WALLS/Iron Bars/Nether Brick Fence and at least12 unique fortified x/z columns in each quadrant. Bastion costs supply2, has4 waves/6000 ticks and revalidates the physical ring between waves.

## 0.38.0-alpha.1 — Defendable Physical Outposts / 전초 방어전
Active real outposts can host a supply1 three-wave defense. Attackers advance toward the actual anchor; enemies inside6 generate breach pressure and200 fails the defense. Owner must remain within64 and keep the physical outpost structure operational.

## 0.37.0-alpha.1 — Physical Warehouse Clusters / 물리 창고군
Each registered `통` remains an anchor and may explicitly link up to8 additional real `통` inside radius6. Real Container contents only; unloaded storage is not accessed.

## 0.36.0-alpha.1 — Physical Commissioning Sites / 물리 준공 현장
Major late projects require a bounded real-world commissioning site before a finalizable funding call can cross completion. Existing completed projects remain compatible.

## Retained field loop
- Integrated Logistics Backbone: stationary logistics-backed sinks consume nearest usable real logistics `통` first, then player inventory. Apex/Trial entry stays player-carried.
- High-volume Field Offload: main inventory slots9..35 -> nearest real `통` capacity; hotbar/equipment remain carried.
- Physical Outposts: owned depot + Bed/Campfire/Crafting/Furnace; logistics64 and NATURAL-hostile safety24 while active.
- Field Recovery: prepaid one-use ordinary-death return within96; authored encounter deaths stay excluded.
- Expedition Operations: physical out-and-back regional sorties with one bounded complication per sortie.

## Combat / endgame boundaries
Apex entry remains player-carried Echo8 + Amethyst32 + Gold32. Ascension Trial entry remains player-carried Echo32 + Amethyst64 + Dragon Breath8. Physical logistics never become remote encounter payment.

## Final action scale
After Lv100 + all nine expedition regions:
- Quarry tunnel 7×7×12
- Woodcutting 448 logs
- Harvest 13×13
- Combat shockwave 7.5 radius / 20 targets
- Construction line65 / plane13×13 / road-bridge deck3×65
- Mobility air dash4

Large work remains tick-budgeted and uses normal protection/material paths. Shift remains the precision override.

## External references
Permissive-code and reference-only boundaries are documented in `THIRD_PARTY_NOTICES.md`. Content-pack mods remain external pack dependencies; Survival Ascension does not embed their assets into its own JAR.
