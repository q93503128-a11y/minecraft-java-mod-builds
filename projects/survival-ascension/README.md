# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25. Network protocol `8`.

Survival Ascension makes progression increase the physical scale of player actions, then makes infrastructure, logistics, expeditions and combat consume that larger output again.

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
This pass fixes first-play readability and pacing without adding another subsystem.

### Guide is a guide again
The in-game guide now scrolls when its content is taller than the screen. It no longer uses patch-note wording such as `0.42부터`; version history stays in `CHANGELOG.md` instead.

The overview/unlock pages now explain the actual world progression explicitly:
- Stage0 `각성 단계` at world start;
- first Wither kill -> Stage1 `전설 단계`;
- first Ender Dragon kill -> Stage2 `종말 단계`.

Stage1/Stage2 consequences, Field Mastery, logistics, Civil Works, freight and outpost/bastion rules are described as current gameplay rules rather than update history.

### Early mastery pacing
The original single quadratic requirement made the 1×1 opening disproportionately slow even though Lv10 immediately unlocks scale multipliers. `SkillTuning.xpForNextLevel` now applies a smooth early discount that fades out by Lv60.

Cumulative XP landmarks change approximately as follows:
- Lv10: 1,190 -> 453;
- Lv30: 17,520 -> 11,610;
- Lv60: 121,890 -> 107,010;
- Lv90: 394,110 -> 379,230;
- Lv100: 536,150 -> 521,270.

The intent is to shorten the repetitive 1×1 opening while retaining most of the long-term progression. Existing saves keep their stored total XP, so a player may resolve to a slightly higher level under the new thresholds.

### Mining XP tiers
Mining no longer treats every ore in `valuable_ores` as a flat 20 XP while capping non-ore hardness XP at6. The old rule made common copper worth more than obsidian.

Mining XP now uses explicit material tiers. Important examples:
- Copper Ore7 / Deepslate Copper8;
- Iron9/10;
- Gold12/13;
- Diamond18/20;
- Emerald20/22;
- Ancient Debris24;
- Obsidian16 / Crying Obsidian18.

The `valuable_ores` tag still controls vein/extract eligibility; only XP valuation changed.

### Logistics storage-first spending
The user-facing `배럴` wording is clarified as Minecraft's normal `통 (Barrel)` block.

For Survival Ascension systems that use the shared physical-logistics material resolver, consumption order is now:
1. nearest usable registered/linked physical logistics `통` Containers;
2. player inventory only for the remaining amount.

This applies to logistics-backed mod costs such as bulk Construction, Infrastructure funding, replant material use and other systems already using `FieldDepotService.consume*`. Apex/Ascension Trial admission remains deliberately carried-only because those systems do not use the logistics resolver.

Vanilla crafting-table recipe grids remain vanilla; this change does not make ordinary crafting grids remotely read storage.

## 0.42.0-alpha.1 — Physical Freight Relay / 물리 화물 중계
0.41 made it practical to build long roads and bridges. 0.42 makes transport infrastructure matter to actual inventory location: a real vanilla Chest Minecart can physically carry bulk stock from one owned active outpost warehouse cluster to another.

### Player flow
Use `M -> Infrastructure -> 산업 가공소 -> 물리 화물 수레`.

Requirements:
- Industrial Works complete;
- Civil Works complete;
- survival/non-spectator player;
- an active owned outpost within4;
- a real Chest Minecart within4 standing on already-loaded vanilla rail;
- the current endpoint's 0.43 physical railhead complete.

At the departure outpost the cart must be completely empty. The action takes only the same authored bulk-material whitelist used by 0.35 High-volume Field Offload from that exact outpost's registered `통(Barrel)` anchor and its linked warehouse `통`, then inserts the actual stacks into the actual Chest Minecart inventory.

After loading, the same cart stores only a small manifest on its own persistent entity NBT: owner UUID and origin outpost dimension/x/y/z. There is no route SavedData, virtual cargo account or generated reward.

Move that physical cart over the player's real rail network to a different active owned outpost in the same dimension and select the same action again. Cargo is inserted into that destination outpost's real `통` anchor and linked warehouse `통`. If destination capacity is insufficient, only accepted items move and the remainder stays inside the cart. When no bulk cargo remains, the manifest is removed.

### Inventory truth and safety
Freight never teleports stock. Loading shrinks source `통` stacks only by the quantity accepted by the cart. Unloading shrinks cart stacks only by the quantity accepted by destination Containers.

Insertion keeps the existing logistics rules:
- merge only `ItemStack.isSameItemSameComponents` stacks first;
- respect `Container.canPlaceItem`;
- respect stack and Container limits;
- then use valid empty slots;
- preserve every remainder physically.

Endpoints use only the outpost's own anchor and stored warehouse links. Every `통` position must already be loaded, still be a real vanilla Barrel Container and pass owner `mayInteract`.

The cart itself must be on `BlockTags.RAILS` at its position or immediately below and inside a complete physical railhead. The system never spawns a cart, drives it, teleports it, registers an abstract route, force-loads chunks or reaches across dimensions.

There is no supply-charge cost because this action creates no reward or duplicate output; the investment is the railhead, route, Chest Minecart, physical warehouse capacity and travel itself.

### External reference boundary
Create remains design-reference-only for this feature. Survival Ascension studies the high-level value of making logistics physically move stock, but does not copy Create trains, contraptions, packages, stock links, routing code, assets, models, sounds or data. The freight implementation uses only vanilla Chest Minecart/rail/redstone/Container behavior and Survival Ascension's existing physical-`통` data.

## 0.41.0-alpha.1 — Civil Works Causeways / 토목 공사소·도로 교량 시공
Stage1 `CIVIL_WORKS / 토목 공사소` consumes Stone Bricks2048 + Cobblestone1536 + Gravel1536 + Iron256 + Copper256 through nearby real logistics `통` first, then player inventory.

Its finalizable funding call requires an owned registered `통` within4 and a loaded radius6 commissioning yard containing Stone Bricks48, Scaffolding16, Iron Blocks4, Stonecutters2 and Crafting Table1. Validation occurs before final-call material consumption; commissioning is one-time proof, not maintenance.

After completion, Construction Lv60 gains `도로/교량`. The manually placed BlockItem becomes a flat three-wide deck extending forward: Lv60 3×17, Lv90 3×33, Lv100 3×49, Field Mastery 3×65. It uses the existing Construction queue, loaded-only targets, player/real-`통` materials, `mayInteract`, NeoForge placement hooks and Shift precision. It does not auto-level terrain, delete obstacles or create free supports.

## 0.40.0-alpha.1 — Physical Siege Breachers / 물리 공성 파괴자
Only Bastion wave4 Ravagers/Vindicators may physically break qualifying wall blocks. Ravager cooldown30 ticks, Vindicator60. Every break requires loaded terrain, `mobGriefing`, owner `mayInteract`, entity-destroy permission and NeoForge destroy-event approval. Normal three-wave defense remains non-destructive.

## 0.39.0-alpha.1 — Physical Bastion Defense / 물리 요새 방어
A real outpost fortification ring uses radius6..12, Y-3..+4, vanilla WALLS/Iron Bars/Nether Brick Fence and at least12 unique fortified x/z columns in each quadrant. Bastion costs supply2, has4 waves/6000 ticks and revalidates the physical ring between waves. No passive defense percentage or blanket combat stat multiplier.

## 0.38.0-alpha.1 — Defendable Physical Outposts / 전초 방어전
Active real outposts can host a supply1 three-wave defense. Attackers advance toward the actual anchor; enemies inside6 generate breach pressure and200 fails the defense. Owner must remain within64 and keep the physical outpost structure operational.

## 0.37.0-alpha.1 — Physical Warehouse Clusters / 물리 창고군
Each registered `통` remains an anchor and may explicitly link up to8 additional real `통` inside radius6. `field_depots_v1` keeps optional `warehouse_links`; unloaded satellites are skipped/preserved and loaded-invalid links prune individually.

## 0.36.0-alpha.1 — Physical Commissioning Sites / 물리 준공 현장
Major late projects require a bounded real commissioning site before a finalizable funding call can cross completion. Existing completed projects remain compatible.

## Retained field loop
- 0.34 Integrated Logistics Backbone: stationary logistics-backed sinks consume nearest usable real logistics `통` first, then player inventory. Apex/Trial entry stays player-carried.
- 0.35 High-volume Field Offload: main inventory slots9..35 -> nearest real `통` capacity; hotbar/equipment remain carried.
- 0.30 Physical Outposts: owned depot + Bed/Campfire/Crafting/Furnace; logistics64 and NATURAL-hostile safety24 while active.
- 0.31 Field Recovery: prepaid one-use ordinary-death return within96; authored encounter deaths stay excluded.
- 0.32/0.33 Expedition Operations: physical out-and-back regional sorties with one bounded complication per sortie.

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
Permissive-code and reference-only boundaries are documented in `THIRD_PARTY_NOTICES.md`. 0.43 adds no third-party asset or dependency and introduces no new SavedData ID or packet.