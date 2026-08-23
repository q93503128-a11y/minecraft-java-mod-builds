# Survival Ascension

- Mod version: `0.37.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Network protocol: `8`
- Existing-world compatibility: `mining_progress_v1`, `infrastructure_v1`, `world_ascension_v1`, `expedition_v1`, `apex_hunt_v1`, `production_v1`, `field_depots_v1`, `outpost_v1`, `field_recovery_v1`, `expedition_operations_v1`, Elite/Warband/mutation persistent NBT, affix CustomData and mining modes remain intact. 0.37 keeps the same `field_depots_v1` ID and adds optional `warehouse_links`; older worlds load with no linked Barrels.

## Core direction
Progression enlarges physical player actions rather than mainly inflating percentages. Bigger actions create larger throughput; infrastructure, logistics, physical bases, expeditions, behavior-driven enemies and endgame consume it again. Shift remains precision/single-action safety override.

## 0.37 Physical Warehouse Clusters / 물리 창고군
### Purpose
0.35 solved manual output shuffling, but a depot still represented one physical Barrel. With 11×11 mining, 448-log felling or 13×13 harvest, storage capacity should scale through **building more actual storage**, not by adding an abstract warehouse screen or infinite virtual slots.

### Persistence
`field_depots_v1` remains the source of truth. `FieldDepotData` adds:
- `MAX_LINKED_BARRELS_PER_DEPOT = 8`
- `MAX_LINK_RADIUS = 6`
- optional `warehouse_links` codec field
- persisted owner + anchor xyz + linked Barrel xyz for each link.

Migration is bounded. Missing `warehouse_links` decodes as an empty list. On load, links are kept only if:
- the saved owner still has that exact anchor depot;
- linked position differs from the anchor;
- link lies within radius6 of anchor;
- no anchor has more than8 links;
- the linked physical position is not already claimed as an anchor or another link.

### Explicit link action
`ProductionService.ACTION_WAREHOUSE_TOGGLE = "toggle_warehouse_barrel"` is routed through the existing Industrial Works packet.

`M -> Infrastructure -> 산업 가공소 -> 창고 배럴 연결`:
1. survival/non-spectator and Industrial Works complete;
2. server resolves nearest real Barrel within4 of the player;
3. target must pass `mayInteract`;
4. a registered anchor itself cannot be used as a satellite link;
5. selecting the player's already-linked target removes that link;
6. another player's linked Barrel is rejected;
7. otherwise server finds a real loaded/interactable owned depot anchor within6 of the target;
8. link is stored only if the anchor remains owned and under the 8-link cap.

There is **no supply charge** for a satellite link. The crafted Barrel block and world footprint are the storage cost. Removing a link does not delete its inventory; it simply stops exposing that Barrel to the logistics resolver.

### Shared real-stock resolver
`FieldDepotService.usableContainers` now resolves a bounded cluster instead of one Container/depot.
- anchor must still satisfy existing same-dimension,32/64 range, loaded chunk, real Barrel Container and `mayInteract` rules;
- an invalid loaded anchor prunes the full depot and its outpost, and `FieldDepotData.remove` also drops that anchor's link records;
- after a valid anchor, its persisted linked Barrels are considered;
- unloaded link chunks are skipped without deleting the link;
- a loaded link that is missing/non-Barrel/non-Container is pruned individually;
- `mayInteract` failure skips the Barrel without deleting the link;
- resolved physical Barrels are globally sorted nearest-first by their own positions before count/consume/offload use.

`activeDepotCount` remains a count of usable anchors. `activeStorageBarrelCount` reports actual usable anchor+satellite Containers.

### Systems using the cluster
The existing shared resolver automatically expands:
- industrial production inputs;
- unfinished infrastructure funding;
- equipment reforge / Mythic III awakening;
- large Construction material consumption;
- irrigation seed consumption;
- 0.35 explicit bulk offload.

Player inventory remains first for sinks. Physical Barrels remain nearest-first afterward. Offload remains the reverse direction only for authored main-inventory bulk stacks.

### Safety / anti-overlap
- one physical Barrel cannot simultaneously be a registered anchor and satellite;
- one satellite cannot belong to multiple owners/clusters;
- max3 anchors/player and max8 satellites/anchor;
- same dimension only;
- no client coordinate trust;
- no automatic item routing or pickup interception;
- no cross-dimension inventory;
- no virtual capacity;
- no chunk ticket / force-load / `getChunk`;
- protocol remains8.

## 0.36 Physical Commissioning Sites
Late facility final funding still requires actual loaded commissioning sites and runs validation before final-call material consumption. Existing completed projects remain compatible.

Profiles remain:
- Industrial: real Barrel within4; radius6 Stone Bricks48, Iron Blocks4, Blast Furnaces2, Stonecutter1, Hoppers2.
- Apex: own registered Barrel within4; radius6 Stone Bricks32, Gold Blocks4, Lodestone1, Cartography Table1, Targets4.
- Nexus: own registered Barrel within4; radius6 Obsidian32, Crying Obsidian8, Beacon1, Enchanting Table1, Ender Chest1.

## 0.35 High-volume Field Offload
`MAIN_INVENTORY_FIRST_SLOT = 9`, `MAIN_INVENTORY_END_EXCLUSIVE = 36`. Authored bulk materials move explicitly into nearest usable physical Barrels. Hotbar0..8, equipment/offhand remain untouched. `ItemStack.isSameItemSameComponents`, `Container.canPlaceItem`, real stack caps and source remainder preservation remain required.

## 0.34 Integrated Logistics Backbone
`countMatching` / `consumeMatching` remain inventory-first + nearest physical Barrel. Stationary industrial/infrastructure/reforge sinks use this shared path. Apex/Trial entry remains carried inventory.

## 0.33 Sortie Complications
New operations receive exactly one `DEEP_FRONT`, `FORWARD_SHIFT` or `HOT_EXTRACTION`. Existing 0.32 operations may remain `NONE`. Extraction windows stay Stage0 4800 / Stage1 3600 / Stage2 3000 ticks.

## 0.32 Out-and-back Expedition Operations
One active operation/player. Start from completed-region active outpost within4, supply1, cross authored range, complete two validated regional actions outside48, and return within8 to exact operational origin. Death/dimension/game-mode/deadline fails with no refund.

## 0.31 Field Recovery
One prepaid token at active outpost. Same-dimension ordinary death within96 only; Incident/Apex/Trial deaths excluded. Safe return consumes token only after successful teleport. No normal fast travel.

## 0.30 Physical Outposts
Owned registered Barrel + Bed + Campfire + Crafting Table + Furnace-family within5. Cost supply2 + Iron32 + Gold8 + Coal32. Active owner-nearby outpost extends logistics32 ->64 and suppresses NATURAL hostiles within24 only.

## Production retained
Four lines remain METALWORKS / TIMBERWORKS / PROVISIONS / PRECISION. One complete four-line cycle produces supply1. Line buffers and supply charges cap at3. Dispatch remains player-carried Gold32 + Amethyst16 + Echo2.

## Apex / Trial retained
- Apex entry: carried Echo8 + Amethyst32 + Gold32; nine behavior patterns;90s.
- Trial entry: carried Echo32 + Amethyst64 + Dragon Breath8;4 waves; Evoker excluded.
- Operation manual starts remain mutually exclusive with Apex/Trial; Regional Incidents may coexist.

## Mastery / Field Mastery retained
Lv100 base: Mining11×11+vein192, Wood384, Harvest11×11, Construction49/11×11, combat6.5/16, air dash3.
After all nine regions: Quarry7×7×12, Wood448, Harvest13×13, Construction65/13×13, combat7.5/20, air dash4.

## External-source policy
0.37 adds no new external implementation or assets. Existing reference-only and packaged-license boundaries remain in `THIRD_PARTY_NOTICES.md`.
