# Survival Ascension

- Mod version: `0.36.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Network protocol: `8`
- Existing-world compatibility: `mining_progress_v1`, `infrastructure_v1`, `world_ascension_v1`, `expedition_v1`, `apex_hunt_v1`, `production_v1`, `field_depots_v1`, `outpost_v1`, `field_recovery_v1`, `expedition_operations_v1`, Elite/Warband/mutation persistent NBT, affix CustomData and mining modes remain intact. 0.36 adds no SavedData field, ID or packet. The existing completed-project branch runs before commissioning, so existing completed Industrial Works/Apex Tracking Post/Ascension Nexus stay complete. 0.35 offload and all earlier persistence contracts remain unchanged.

## Core direction
Progression must enlarge physical actions rather than only percentages. Larger actions create larger material throughput; world stages, behavior-rich enemies, exploration goals, infrastructure, production, logistics and field bases must consume that throughput again. Shift remains the precision/single-action safety override.

## 0.36 Physical Commissioning Sites / 물리 준공 현장
### Purpose
0.34 connected large stationary inputs to physical Barrel stock and 0.35 connected high-volume gathering outputs back into that stock. The remaining contradiction was infrastructure itself: a player could fund thousands of materials into a project counter and gain a major facility without constructing a facility in the world.

0.36 does not create another blueprint/autobuilder subsystem. Existing Construction already provides protected, material-backed, tick-budgeted physical building. Instead, late infrastructure gains a bounded **commissioning proof** at the final funding line.

### Finalization order
The original `infrastructure_v1` funding state remains the source of truth.
1. Existing `data.isComplete(project)` is checked first. This is the explicit existing completed compatibility boundary.
2. Incomplete projects may receive partial material funding exactly as before.
3. `canFullyFundNow(player, data, project)` asks whether current inventory + usable local logistics stock can satisfy every remaining project material.
4. Only when the current action is finalizable and the project is one of the three late facilities does `InfrastructureSiteService.validateForFinalFunding` run.
5. Site validation runs before `int consumed = 0` and therefore before any project material is removed by that finalizable call.
6. An incomplete site aborts the call and consumes zero project material.
7. A valid site allows the existing inventory-first + nearest-Barrel funding loop to finish and the original completion behavior fires.

The site is a commissioning gate rather than ongoing maintenance state. No new persistence or per-tick structure monitor is introduced; protocol remains `8`.

### Physical scan contract
`InfrastructureSiteService` defines:
- `ANCHOR_RADIUS = 4`
- `SITE_RADIUS = 6`

All anchors and components are server-resolved:
- current dimension only;
- site anchor must be within4 of the player;
- target chunks must already be loaded through `level.hasChunkAt`;
- every counted position must pass `level.mayInteract(player, pos)`;
- Barrel anchor must still be vanilla `Blocks.BARREL` with a `Container` block entity;
- site scan is a radius6 sphere around the anchor rather than an unlimited cube;
- no `getChunk`, chunk ticket, force-load or client coordinate is used.

The authored site blocks remain in the world after commissioning. They are not an additional hidden item sink.

### Industrial Works profile
Because `field_depots_v1` cannot normally exist before Industrial Works is complete, its first commissioning anchor is any real interactable Barrel within4. Inside radius6:
- `STONE_BRICKS` 48
- `IRON_BLOCK` 4
- `BLAST_FURNACE` 2
- `STONECUTTER` 1
- `HOPPER` 2

Original funding remains Stone Bricks1024 + Iron512 + Copper512 + Redstone256 + Amethyst128.

### Apex Tracking Post profile
Requires one of the player's own `field_depots_v1` registered Barrels within4. Inside radius6:
- `STONE_BRICKS` 32
- `GOLD_BLOCK` 4
- `LODESTONE` 1
- `CARTOGRAPHY_TABLE` 1
- `TARGET` 4

Original funding remains Iron512 + Gold256 + Amethyst256 + Echo32 + Nether Star1.

### Ascension Nexus profile
Requires one of the player's own registered Barrels within4. Inside radius6:
- `OBSIDIAN` 32
- `CRYING_OBSIDIAN` 8
- `BEACON` 1
- `ENCHANTING_TABLE` 1
- `ENDER_CHEST` 1

Original funding remains Nether Star4 + Dragon Breath64 + Obsidian512 + Amethyst512 + Echo64.

### Presentation and compatibility
- `InfrastructureService.sendStatus` reports the commissioning anchor and per-block site counts while a gated project is incomplete.
- Industrial and Infrastructure radial details explicitly warn that late final funding needs a physical site.
- Guide lists all three profiles and explains that failed commissioning consumes no final-call project materials.
- completed facilities do not receive retroactive site checks;
- Stage0 Quarry/Irrigation/Builder/Combat projects retain their old material-only completion behavior;
- no new packet, SavedData, background event listener, force-load or generic management GUI.

## 0.35 High-volume Field Offload / 현장 일괄 적재
`ProductionService.ACTION_BULK_OFFLOAD = "bulk_offload"` explicitly moves authored bulk materials from main inventory slots9..35 into already-usable linked Barrels. `MAIN_INVENTORY_FIRST_SLOT = 9`; `MAIN_INVENTORY_END_EXCLUSIVE = 36`. Hotbar0..8, equipment and offhand remain carried.

`ItemStack.isSameItemSameComponents`, `Container.canPlaceItem`, actual container/item stack caps, merge-before-empty ordering and source-remainder preservation are mandatory. No pickup hook/tick automation/virtual inventory is introduced.

## 0.34 Integrated Logistics Backbone / 통합 물류 백본
`FieldDepotService.countMatching` and `consumeMatching` are the shared inventory-first + nearest usable Barrel resolver. Industrial batches, incomplete infrastructure funding and equipment reforge/awakening use it; Apex/Trial admission remains carried inventory.

Resolver boundary: same dimension, ordinary radius32 / active-outpost radius64, loaded real Barrel Container, `mayInteract`, stale-link pruning, no force-load.

## 0.33 Sortie Complications / 원정 작전 변수
New sorties get exactly one of `DEEP_FRONT`, `FORWARD_SHIFT`, `HOT_EXTRACTION`; existing 0.32 active runs decode as `NONE`. Emergency extraction remains Stage0 4800 / Stage1 3600 / Stage2 3000 ticks and never extends the original deadline.

## 0.32 Out-and-back Expedition Operations
One active operation/player. Start at nearest active owned outpost within4 after that region's directive is complete; supply1. Cross authored range, perform two validated real actions in the matching region and outside48 from origin, then return within8 to the same operational physical outpost. Death/dimension/game-mode/deadline failure gives no refund.

Profiles remain:
- WOODLAND range96, logs128 + travel240, 20m
- ARID range96, build96 + travel240, 20m
- WETLAND range96, crops80 + kills8, 20m
- HIGHLANDS range128, travel600 + dashes12, 20m
- OCEAN range128, voyage900 + kills8, 20m
- DEEP range128, mine192 + kills10, 25m
- FROZEN range128, travel600 + kills10, 25m
- NETHER range160, kills24 + mine96, 25m
- END range160, kills28 + travel360, 30m

## 0.31 Field Recovery retained
Active outpost within4, first arm supply1, one prepaid token. Same-dimension ordinary death within96 only. Incident/Apex/Trial excluded. Safe teleport happens after real loaded outpost + safe-floor validation; token consumes only after successful move. No ordinary fast travel/force-load.

## 0.30 Physical Field Outposts retained
Nearest owned registered Barrel within4; structure within5 must include Bed + Campfire/Soul + Crafting Table + Furnace/Blast/Smoker. Cost supply2 + Iron32 + Gold8 + Coal32. Active same-dim owner-nearby outpost extends its depot to64 and NATURAL-only hostile safe radius24. TRIGGERED combat remains unaffected.

## 0.29 Physical Field Depots retained
`field_depots_v1`, max3/player, one owner/physical Barrel. Same-dim loaded real Barrel, `mayInteract`, ordinary32 / active outpost64. Player inventory first for sinks, linked Barrels nearest-first. 0.35 offload reverses direction only for explicitly authored main-inventory bulk stacks.

## 0.28 Industrial production retained
Four lines:
1. METALWORKS Raw Iron96 + Raw Copper96 + Coal64
2. TIMBERWORKS logs192 + Cobblestone384 + Iron32
3. PROVISIONS Wheat128 + Carrot64 + Potato64 + Beetroot32
4. PRECISION Redstone128 + Amethyst64 + Gold32 + Quartz64

Buffers0..3, supply charges0..3. One full four-line set creates supply1. Dispatch gives Gold32 + Amethyst16 + Echo2 physically.

## Apex / Trial retained
- Apex: nine behavior identities CHARGE / REINFORCE / PLAGUE / SKIRMISH / PULL / LEAP / FROST / WITHER / VOID, 90s. Entry inventory-only Echo8 + Amethyst32 + Gold32.
- Trial: Stage2 Nexus, four waves, Evoker excluded. Entry inventory-only Echo32 + Amethyst64 + Dragon Breath8.
- Operation manual starts are mutually exclusive with Apex/Trial; Regional Incidents may coexist.

## Mastery / Field Mastery retained
Lv100 base final scale remains Mining11×11 + vein192, Wood384, Harvest11×11, Construction49/11×11, combat6.5/16, air dashes3. Nine-region Field Mastery expands to Quarry7×7×12, Wood448, Harvest13×13, Construction65/13×13, combat7.5/20, air dash4.

## Safety contracts
- large destruction/build/harvest is tick-budgeted;
- secondary destruction uses normal destroy paths;
- Construction and irrigation use material/protection hooks;
- logistics/outpost/recovery/operation/commissioning scans never force-load chunks;
- no client coordinate drives a physical anchor;
- no generic quest GUI, blanket enemy HP layer or permanent flat-stat reward is introduced;
- Apex/Trial admission remains carried inventory;
- protocol remains `8`.

## External-source policy
0.36 adds no new external source/assets. Deep Rock Galactic and Warframe remain product references only for0.33; Heracles is design reference only for0.32; Bountiful, Waystones, Corpse, MineColonies, Create, Building Gadgets2 and packaged permissive notices retain the boundaries in `THIRD_PARTY_NOTICES.md`.
