# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25 / network protocol `8`.

Survival Ascension turns progression into **larger physical actions**, then makes world stages, expeditions, infrastructure, behavior-driven enemies, production, logistics and physical field bases consume that larger output again. Shift remains the precision/single-action safety override.

## 0.36.0-alpha.1 — Physical Commissioning Sites / 물리 준공 현장
0.34 and 0.35 completed the physical stock loop, but the largest infrastructure projects could still become complete by filling counters while leaving no structure in the world. 0.36 makes the last step physical without introducing a second auto-builder, a virtual settlement, or a maintenance simulator.

### Finalizable funding gate
The existing shared `infrastructure_v1` material progress remains authoritative. Players can partially fund projects exactly as before. When a single funding action has enough inventory + currently usable linked-Barrel stock to satisfy **every remaining material requirement**, the server treats it as a commissioning attempt.

For the three large late projects the server calls `InfrastructureSiteService.validateForFinalFunding` **before any material from that finalizable funding call is consumed**. If the site is incomplete, that call consumes zero project material and reports the missing physical blocks.

Already complete worlds are deliberately grandfathered: the existing completed-project branch runs before the 0.36 site gate, so **existing completed** Industrial Works, Apex Tracking Post and Ascension Nexus remain complete after updating.

### Shared physical rules
All commissioning scans are bounded and server-side:
- `ANCHOR_RADIUS = 4` from the player to the site Barrel;
- `SITE_RADIUS = 6` around that Barrel;
- only already-loaded blocks count;
- each counted block must pass `mayInteract`;
- the anchor must still be a real vanilla Barrel with a Container block entity;
- no client coordinate is trusted;
- no chunk ticket, remote dimension scan or force-load is introduced.

The structure blocks remain placed in the world. They are commissioning proof, not another item cost silently deleted from storage.

### Industrial Works
Final commissioning uses any real interactable Barrel within4 because a new world cannot register field depots until Industrial Works itself is complete.

Within radius6 of that Barrel:
- Stone Bricks 48
- Iron Blocks 4
- Blast Furnaces 2
- Stonecutter 1
- Hoppers 2

The original project funding remains Stone Bricks1024 + Iron512 + Copper512 + Redstone256 + Amethyst128.

### Apex Tracking Post
Final commissioning requires one of the player's **owned registered logistics Barrels** within4, tying the late combat facility to the physical network created after Industrial Works.

Within radius6:
- Stone Bricks 32
- Gold Blocks 4
- Lodestone 1
- Cartography Table 1
- Targets 4

Original funding remains Iron512 + Gold256 + Amethyst256 + Echo32 + Nether Star1.

### Ascension Nexus
Final commissioning also requires the player's owned registered Barrel within4.

Within radius6:
- Obsidian 32
- Crying Obsidian 8
- Beacon 1
- Enchanting Table 1
- Ender Chest 1

Original funding remains Nether Star4 + Dragon Breath64 + Obsidian512 + Amethyst512 + Echo64.

0.36 adds **No new SavedData**, packet type, protocol bump, background scanner or permanent stat layer. The commissioning check is explicit, bounded and only relevant when the final funding line can actually be crossed.

## 0.35.0-alpha.1 — High-volume Field Offload / 현장 일괄 적재
`M -> Infrastructure -> 산업 가공소 -> 현장 일괄 적재` explicitly moves authored bulk progression materials from main inventory slots `9..35` into currently usable linked real Barrels, nearest-first. Hotbar slots `0..8`, equipment and offhand are preserved.

Matching existing stacks are filled before empty slots, item+components must match, `Container.canPlaceItem` and actual stack capacity are respected, and any unaccepted remainder stays in the original inventory stack. No automatic pickup routing or virtual warehouse exists.

## 0.34.0-alpha.1 — Integrated Logistics Backbone / 통합 물류 백본
`FieldDepotService.countMatching` / `consumeMatching` provide one physical inventory-first resolver for exact items and tag-style inputs. Industrial batches, incomplete infrastructure funding and equipment reforge/awakening can consume inventory first and then same-dimension usable linked Barrels nearest-first.

Ordinary depot radius is32; an active physical outpost extends only its own depot to64. The target chunk must already be loaded, the Barrel must be real and interactable, and stale loaded links are pruned. Apex Hunt / Ascension Trial entry costs deliberately remain player-carried and industrial dispatch output remains physical player output.

## 0.33.0-alpha.1 — Sortie Complications / 원정 작전 변수
Every new repeatable sortie gets exactly one server-authored rule:
- `DEEP_FRONT / 전선 고착`: work remains beyond the authored outbound line.
- `FORWARD_SHIFT / 전선 재전개`: first objective completion requires a second push `range + 48` before remaining progress resumes.
- `HOT_EXTRACTION / 긴급 철수`: after both objectives, return window Stage0 4:00 / Stage1 3:00 / Stage2 2:30, never later than the original deadline.

Existing 0.32 active sorties migrate as `NONE`; no paid active run receives a surprise modifier.

## 0.32 — Out-and-back Expedition Operations
A completed-region active outpost launches one repeatable operation for supply1. The player physically crosses the region's outbound line, completes two validated real-action tasks away from base, then returns within8 of the exact same operational outpost before deadline.

Nine profiles remain locked:
- Woodland: range96, logs128 + travel240, 20m
- Arid: range96, build96 + travel240, 20m
- Wetland: range96, crops80 + hostile kills8, 20m
- Highlands: range128, travel600 + dashes12, 20m
- Ocean: range128, ocean voyage900 + hostile kills8, 20m
- Deep: range128, mine192 + hostile kills10, 25m
- Frozen: range128, travel600 + hostile kills10, 25m
- Nether: range160, hostile kills24 + mine96, 25m
- End: range160, hostile kills28 + travel360, 30m

Death, dimension exit, creative/spectator or timeout fails with no refund. Regional Incidents may coexist; Apex Hunt / Ascension Trial manual starts are mutually exclusive.

## 0.31 — Death-bound Field Recovery
An active physical outpost can prepay one recovery token for supply1. Ordinary same-dimension death within96 may return after revalidating the loaded/interactable Barrel/camp and a safe destination. Incident/Apex/Trial deaths do not consume it. There is no ordinary living-player fast travel or chunk force-load.

## 0.30 — Physical Field Outposts
Upgrade an owned linked Barrel within4. The camp within5 needs Bed + Campfire/Soul Campfire + Crafting Table + Furnace/Blast Furnace/Smoker. Cost supply2 + Iron32 + Gold8 + Coal32. Active owner-nearby outposts extend logistics to64 and suppress only NATURAL hostile spawns within24; TRIGGERED combat remains unaffected.

## 0.29 — Physical Field Depots
Register real vanilla Barrels within4 for supply1, max3/player and one owner per physical position. Same-dimension loaded stock is usable in radius32, or64 when that depot is an active outpost. No force-loaded chunks.

## 0.28 — Industrial Works / production
Four bounded large-batch lines feed a stored supply-charge loop:
- METALWORKS: Raw Iron96 + Raw Copper96 + Coal64
- TIMBERWORKS: logs192 + Cobblestone384 + Iron32
- PROVISIONS: Wheat128 + Carrot64 + Potato64 + Beetroot32
- PRECISION: Redstone128 + Amethyst64 + Gold32 + Quartz64

One completed set adds supply1; buffers and supply charges remain capped at3. Dispatch remains Gold32 + Amethyst16 + Echo2 physically to player/drop.

## Apex / Trial / endgame
- Stage1 Apex Tracking Post opens nine region-specific behavior identities: CHARGE, REINFORCE, PLAGUE, SKIRMISH, PULL, LEAP, FROST, WITHER, VOID. Entry remains player-carried Echo8 + Amethyst32 + Gold32.
- Stage2 Ascension Nexus opens the four-wave Ascension Trial. Evoker remains excluded. Entry remains player-carried Echo32 + Amethyst64 + Dragon Breath8.
- Valid Mythic III 3-affix gear can awaken once to4 affixes for Amethyst256 + Diamond24 + Netherite Scrap8 + Echo64 + Dragon Breath16.

## Mastery / Field Mastery
Lv100 remains the normal maximum. Completing all nine expedition regions unlocks Field Mastery expansion on already-scaled actions: Quarry7×7×12, Wood448, Harvest13×13, Combat shockwave7.5/20, Construction line65/plane13×13, Mobility air dash4.

Large mining, woodcutting, harvesting, construction and irrigation work remain tick-budgeted and use normal destruction/protection/material hooks.

## External references
0.36 introduces no new third-party implementation or asset source; it composes existing vanilla blocks, the existing Survival Ascension infrastructure funding path and the already-authored physical Barrel model. Deep Rock Galactic / Warframe remain 0.33 product-level references only; Heracles remains 0.32 design reference only; Bountiful and all other projects retain the restrictions documented in `THIRD_PARTY_NOTICES.md`.

Main radial: Skills / Mining / Construction / Equipment / Infrastructure / Guide / Close. Shift remains the precision override.
