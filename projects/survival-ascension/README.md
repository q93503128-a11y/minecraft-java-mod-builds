# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25. Network protocol `8`.

Survival Ascension makes progression increase the physical scale of player actions, then makes infrastructure, logistics, expeditions and endgame consume that larger output again.

## 0.37.0-alpha.1 — Physical Warehouse Clusters / 물리 창고군
0.35 made high-volume gathering easy to offload, but each registered field depot was still only one Barrel. 0.37 scales storage by making the player physically build more storage instead of adding a virtual warehouse.

### Physical cluster rule
Each existing registered depot Barrel remains the anchor. A player may explicitly link up to `8` additional real vanilla Barrels to that anchor while each target is within radius 6 of the anchor.

Use `M -> Infrastructure -> 산업 가공소 -> 창고 배럴 연결` while within4 blocks of the target Barrel.
- the target must be a real loaded Barrel with a `Container` block entity;
- `mayInteract` must pass;
- the server resolves the nearby owned depot anchor; no client coordinate is trusted;
- one physical Barrel cannot be another registered anchor or already belong to another warehouse cluster;
- selecting one of your already-linked Barrels unlinks it;
- linking consumes no supply charge: the actual Barrel and its world footprint are the capacity cost;
- removing an anchor removes all of its stored links, while the actual Barrel blocks/items remain in the world.

### Persistence / migration
The existing `field_depots_v1` identifier remains. 0.37 adds only an optional `warehouse_links` list to its codec. Older saves therefore decode with an empty link list and preserve all existing depot/outpost coordinates.

Save sanitation rechecks:
- max3 anchors/player;
- max8 linked Barrels/anchor;
- link distance <= radius 6;
- linked position cannot equal the anchor;
- no physical position may be double-claimed by another anchor/link.

### Shared real-stock resolver
All systems that already used the 0.34 logistics backbone now see the anchor plus its currently usable linked Barrels:
1. player inventory first for consumption;
2. all usable physical Barrel containers are sorted nearest-first;
3. ordinary anchor radius32 / active outpost anchor radius64;
4. same dimension only;
5. anchor and linked Barrel chunks must already be loaded;
6. real `Blocks.BARREL` + `Container` + `mayInteract` required;
7. an unloaded linked Barrel is simply skipped and its link is preserved;
8. a loaded linked position that is no longer a valid Barrel is pruned from `warehouse_links` only.

The same ordered set is used by 0.35 High-volume Field Offload. Slots `9..35` remain the only scanned player slots; Hotbar slots `0..8`, equipment and offhand are preserved. Matching existing stacks are filled before empty slots, `ItemStack.isSameItemSameComponents`, `Container.canPlaceItem`, real stack caps and source-remainder preservation remain mandatory.

There is no global storage balance, remote dimension access, automatic sorting, pickup interception, background scan or chunk force-load.

## 0.36.0-alpha.1 — Physical Commissioning Sites / 물리 준공 현장
The three largest infrastructure projects must prove a real built site before a finalizable funding call may cross the completion line. Validation occurs before any material is consumed in that final call, and already-completed worlds remain compatible.

- Industrial Works: real Barrel within4; radius6 Stone Bricks48, Iron Blocks4, Blast Furnaces2, Stonecutter1, Hoppers2.
- Apex Tracking Post: own registered Barrel within4; radius6 Stone Bricks32, Gold Blocks4, Lodestone1, Cartography Table1, Targets4.
- Ascension Nexus: own registered Barrel within4; radius6 Obsidian32, Crying Obsidian8, Beacon1, Enchanting Table1, Ender Chest1.

No maintenance tick, force-load, new packet or new building entity is added.

## Retained logistics / field loop
- 0.34 Integrated Logistics Backbone: stationary industrial/infrastructure/reforge sinks consume inventory first, then nearest usable real logistics Barrels.
- 0.35 High-volume Field Offload: explicit main-inventory bulk output -> nearest physical logistics Barrel capacity.
- 0.30 Physical Outposts: owned depot + Bed/Campfire/Crafting/Furnace; logistics64 and NATURAL-hostile safety24 while active.
- 0.31 Field Recovery: prepaid one-use ordinary-death return within96; no normal fast travel.
- 0.32/0.33 Expedition Operations: physical out-and-back regional sorties with exactly one bounded complication per new sortie.

## Production
Industrial Works retains four lines:
- METALWORKS: Raw Iron96 + Raw Copper96 + Coal64
- TIMBERWORKS: logs192 + Cobblestone384 + Iron32
- PROVISIONS: Wheat128 + Carrot64 + Potato64 + Beetroot32
- PRECISION: Redstone128 + Amethyst64 + Gold32 + Quartz64

One completed set grants one field-supply charge. Buffers and charges remain capped at3. Dispatch remains player-carried Gold32 + Amethyst16 + Echo2.

## Combat / endgame boundaries
Apex Hunt entry remains player-carried Echo8 + Amethyst32 + Gold32. Ascension Trial entry remains player-carried Echo32 + Amethyst64 + Dragon Breath8. Warehouse clusters do not turn field-combat admission into remote payment.

## Final action scale
After Lv100 + all nine expedition regions:
- Quarry tunnel 7×7×12
- Woodcutting 448 logs
- Harvest 13×13
- Combat shockwave 7.5 radius / 20 targets
- Construction line65 / plane13×13
- Mobility air dash4

Large mining/wood/farm/construction work remains tick-budgeted and uses normal protection/material paths. Shift remains the precision override.

## External references
0.37 adds no new third-party source, data, UI or assets. Existing notices and reference-only boundaries remain in `THIRD_PARTY_NOTICES.md`.
