# Survival Ascension

- Mod version: `0.42.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Network protocol: `8`
- Existing-world compatibility: 0.42 adds no SavedData ID and no migration. Existing `infrastructure_v1`, `field_depots_v1`, `outpost_v1` and older data stay unchanged. A loaded freight Chest Minecart carries only owner/origin manifest fields on that entity's persistent NBT.

## Core direction
Progression enlarges physical player actions rather than mainly inflating percentages. Bigger actions create larger throughput; infrastructure, real storage, transport, bases, expeditions and behavior-driven enemies consume it again. Shift remains the precision/single-action safety override.

## 0.42 Physical Freight Relay / 물리 화물 중계
### Purpose
0.41 made large road/bridge construction possible, but logistics between distant bases still depended on the player manually carrying stacks. 0.42 adds one bounded transport bridge between existing systems: vanilla rails and a real Chest Minecart can move actual bulk inventory between two owned active outpost warehouse clusters.

This is not a virtual route network, automatic train system, remote storage API or delivery quest.

### Explicit action
`ProductionService.ACTION_FREIGHT = "physical_freight"` reuses the existing Industrial Works action payload and radial menu. No packet schema/protocol change.

Player path:
`M -> 인프라 -> 산업 가공소 -> 물리 화물 수레`.

Server prerequisites:
- survival/non-spectator;
- Industrial Works complete;
- Civil Works complete;
- active owned outpost within4;
- real `MinecartChest` within4;
- cart standing on already-loaded `BlockTags.RAILS` at cart position or one block below.

### Departure loading
The Chest Minecart must be completely empty and have no freight owner manifest.

The source endpoint is not every nearby Barrel. `FreightService` resolves the exact outpost anchor to its existing `FieldDepotData.DepotEntry`, then uses only:
- that physical registered Barrel anchor;
- that anchor's persisted linked warehouse Barrels.

Each Container must be in the current dimension, already loaded, still be a vanilla Barrel BlockEntity `Container`, and pass `level.mayInteract(player, pos)`.

Only `FieldDepotService.isBulkMaterial` items are eligible, so the 0.35 authored high-volume whitelist remains the single bulk classification contract.

Actual stacks move into actual cart slots. Source stacks shrink only by accepted count. On successful load the cart stores:
- `survivalascension_freight_owner`
- `survivalascension_freight_origin_dimension`
- `survivalascension_freight_origin_x/y/z`

No global freight SavedData exists.

### Physical transport and arrival
The same loaded entity must be moved by normal Minecraft mechanics. The mod adds no cart spawn, propulsion, pathfinding, route simulation, teleportation, chunk ticket or force-load.

At unloading time:
- manifest owner must equal the acting player;
- destination must be a different active owned outpost;
- origin and destination must be in the same dimension;
- cart must still be on loaded rail;
- destination resolves only its own physical anchor + linked warehouse Barrels.

Cargo insertion merges same item+components first, respects `Container.canPlaceItem` and real max stack/container limits, then uses empty slots. Partial unloading is valid. Anything not accepted stays inside the cart. When no eligible bulk cargo remains, the manifest is cleared.

### Why there is no delivery reward or supply cost
Freight itself creates no item or XP output, so a reward loop would encourage trivial short-route farming. The useful result is purely the inventory relocation. Likewise there is no supply-charge fee: rails, powered rail infrastructure, cart, route construction and physical travel already carry the cost.

Different outpost is required, but no artificial minimum distance is added because inventory duplication is impossible and the action has no generated payout.

### Safety boundaries
- no new SavedData ID;
- no new packet/protocol bump;
- no new custom block/item/entity;
- no automated minecart movement;
- no player/cart teleport;
- no cross-dimension freight;
- no `getChunk`, region ticket or force-load;
- no remote access to unloaded source/destination storage;
- no universal warehouse resolver during freight: endpoint inventory is intentionally tied to one physical outpost cluster.

### External-source boundary
Create remains design-reference-only. 0.42 studies the product-level value of physical stock movement but does not copy train, contraption, package, Stock Link, Stock Ticker, Requester, routing, GUI, asset, data or namespace implementation. FreightService is independent code using vanilla Chest Minecart/rail/Container behavior plus Survival Ascension's existing physical Barrel records.

## 0.41 Civil Works Causeways retained
`CIVIL_WORKS` is Stage1: Stone Bricks2048 + Cobblestone1536 + Gravel1536 + Iron256 + Copper256. Final funding needs owned registered Barrel within4 plus radius6 physical yard: Stone Bricks48, Scaffolding16, Iron Blocks4, Stonecutters2, Crafting Table1.

`CAUSEWAY("causeway", "도로/교량", 60)` uses the existing protected Construction queue. It lays the actual manually selected BlockItem as a flat 3-wide forward deck:17/33/49/65 length. Loaded-only, `mayInteract`, placement hooks, player/real-Barrel material, no terrain erasure or free supports.

## 0.40 Physical Siege Breachers retained
Only Bastion wave4 Ravager/Vindicator can damage qualifying fortification; normal three-wave defense remains non-destructive. Every break obeys loaded-only, mobGriefing, owner protection, entity-destroyability and NeoForge destroy event.

## 0.39 Physical Bastion Defense retained
Physical fortification uses radius6..12, Y-3..+4, four quadrants each12 unique x/z columns. Bastion remains supply2 /4 waves /6000 ticks and composition-driven rather than blanket-stat driven.

## 0.38 Defendable Physical Outposts retained
Normal defense remains supply1 /3 waves /4800 ticks with owner64, physical structure validation and breach radius6/limit200.

## 0.37 Physical Warehouse Clusters retained
`field_depots_v1` keeps max3 anchors/player, max8 satellite Barrels/anchor inside6. Real Container contents only, loaded-only, no virtual capacity.

## 0.36 Physical Commissioning retained
Civil Works, Industrial Works, Apex Tracking Post and Ascension Nexus use the same final-call real-world commissioning engine. It is one-time proof, not ongoing maintenance.

## 0.35 / 0.34 logistics retained
High-volume offload scans main inventory9..35. Stationary material sinks use inventory first, then nearest usable real Barrel storage. Apex/Trial admission stays player-carried.

## 0.33 / 0.32 expeditions retained
Active regional outpost -> supply1 -> cross range -> two real validated objectives -> exact-origin return. One bounded complication per new sortie. No force-load.

## 0.31 Field Recovery retained
One prepaid ordinary-death return within96; authored encounter deaths remain excluded. No ordinary fast travel.

## 0.30 Physical Outposts retained
Owned registered Barrel + Bed/Campfire/Crafting/Furnace-family within5. Owner-nearby activation64, logistics64, NATURAL-hostile safety24.

## Production retained
METALWORKS / TIMBERWORKS / PROVISIONS / PRECISION remain. One full cycle grants supply1, cap3. Dispatch remains a player-carried reward rather than freight/remote payment.

## Mastery / Field Mastery retained
Lv100 base: Mining11×11+vein192, Wood384, Harvest11×11, Construction49/11×11, combat6.5/16, air dash3.
After all nine regions: Quarry7×7×12, Wood448, Harvest13×13, Construction65/13×13 plus Causeway3×65, combat7.5/20, air dash4.
