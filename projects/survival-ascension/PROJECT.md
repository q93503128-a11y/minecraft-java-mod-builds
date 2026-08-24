# Survival Ascension

- Mod version: `0.43.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Network protocol: `8`
- Existing-world compatibility: no new SavedData ID or migration. Existing skill XP totals, `infrastructure_v1`, `field_depots_v1`, `outpost_v1` and older data stay unchanged. Physical freight railheads are validated from current world blocks and store no completion flag.

## Core direction
Progression enlarges physical player actions rather than mainly inflating percentages. Bigger actions create larger throughput; infrastructure, real storage, transport, bases, expeditions and behavior-driven enemies consume it again. Shift remains the precision/single-action safety override.

## 0.43 Physical Freight Railheads / 물리 화물 하역장
### Purpose
0.42 proved that actual stock can move between real outpost warehouse clusters in a real vanilla Chest Minecart. Its weak point was the endpoint: a single arbitrary rail beside an active outpost was enough to trigger loading or unloading.

0.43 makes the endpoint itself physical. Freight now works only when the exact active outpost has a small real railhead around its registered `통(Barrel)` anchor. This gives Civil Works/rails a visible persistent job without introducing a background logistics simulator.

### Physical endpoint contract
Before any freight mutation, `FreightRailheadService` inspects only already-loaded blocks around the exact active outpost anchor.

Radius: `6` blocks around the outpost anchor.
Minimum visible yard:
- rail blocks using `BlockTags.RAILS`: `6+`;
- `Powered Rail`: `1+`;
- `Hopper`: `1+`;
- control: `Lever` or `Redstone Block`: `1+`.

The actual Chest Minecart rail must also be inside the same radius. To stop scattered checklist blocks from counting as a station, the active cart rail additionally needs:
- a Powered Rail within squared distance `9`;
- a Hopper within squared distance `9`;
- a Lever/Redstone Block within squared distance `16`.

Every scanned block must be in an already-loaded chunk and pass `level.mayInteract(player, pos)`.

### Runtime behavior
The railhead is not registered and has no SavedData flag. Every load/unload action checks the current physical blocks again. If the yard is broken, freight stops; rebuild it and freight works again.

The 0.42 freight semantics remain:
- Industrial Works + Civil Works complete;
- survival/non-spectator;
- active owned outpost within4;
- real Chest Minecart within4;
- cart standing on loaded rail;
- empty cart required for departure loading;
- source/destination inventory is only that exact outpost anchor + its persisted linked warehouse `통`;
- same-dimension, different owned active destination;
- only `FieldDepotService.isBulkMaterial` moves;
- partial unloading leaves remainder in the same physical cart;
- manifest remains only owner UUID + origin dimension/x/y/z on the cart entity.

### Deliberate exclusions
- no station SavedData or registration menu;
- no route SavedData;
- no automatic minecart driving or pathfinding;
- no generated freight reward or supply-charge fee;
- no item/cart/player teleport;
- no custom train/block/item/entity;
- no `getChunk`, region ticket or force-load;
- no cross-dimension freight;
- no passive maintenance timer.

## 0.42.1 Guide / Early Mastery / Logistics Priority
### Guide information architecture
`GuideScreen` is a gameplay reference, not a patch-note surface.

The body now has a bounded viewport with scissor clipping, mouse-wheel scrolling and a visible scrollbar. Content no longer stops rendering when it reaches the old bottom cutoff.

The guide now explains the persistent world state directly:
- Stage0 `각성 단계` at world start;
- first Wither death -> Stage1 `전설 단계`;
- first Ender Dragon death -> Stage2 `종말 단계`.

World Ascension, skill tiers, Field Mastery, physical `통(Barrel)` logistics, Civil Works, freight and defenses are described as current rules. Version-history phrases such as `0.42부터` are excluded from the guide; release history belongs in `CHANGELOG.md`.

### Early skill pacing
The base quadratic XP curve remains the late-game reference, but a smooth level-dependent factor accelerates the opening and fades out by Lv60:
- Lv0..9: factor `0.20 + 0.03 * level`;
- Lv10..29: factor `0.50 + 0.015 * (level - 10)`;
- Lv30..59: factor `0.80 + 0.0065 * (level - 30)`;
- Lv60+: full base curve.

Approximate cumulative thresholds:
- Lv10 `453` instead of `1,190`;
- Lv30 `11,610` instead of `17,520`;
- Lv60 `107,010` instead of `121,890`;
- Lv90 `379,230` instead of `394,110`;
- Lv100 `521,270` instead of `536,150`.

This targets the repetitive 1×1 opening. After Lv10/30, area/chain actions already accelerate validated actions naturally, so the later curve is intentionally much closer to the old total.

### Mining XP material tiers
The previous `VALUABLE_ORES -> 20 XP` rule made common copper20 while obsidian fell through to hardness-capped6. Vein/extract eligibility still uses `valuable_ores`, but XP is now independent and material-tiered.

Key values:
- Coal6 / Deepslate Coal7;
- Copper7 / Deepslate Copper8;
- Iron9 / Deepslate Iron10;
- Nether Quartz8 / Nether Gold8;
- Gold12 / Deepslate Gold13;
- Redstone10/11;
- Lapis11/12;
- Diamond18/20;
- Emerald20/22;
- Ancient Debris24;
- Obsidian16 / Crying Obsidian18.

Ordinary pickaxe-mineable blocks still use hardness with a bounded1..8 fallback.

### Physical logistics spend order
User-facing `배럴` terminology is clarified to Minecraft's normal `통 (Barrel)` block.

`FieldDepotService.consumeMatching` now resolves usable physical logistics Containers first and consumes them nearest-first before touching the player's carried inventory. Only a shortfall is taken from the player.

This automatically changes every existing logistics-backed sink using `FieldDepotService.consume*`, including Construction and Infrastructure material use, without adding another storage abstraction.

Boundary retained:
- normal vanilla crafting-table grids stay vanilla and do not remotely pull storage;
- Apex and Ascension Trial admissions stay carried-only because they deliberately do not use `FieldDepotService`;
- no unloaded storage, chunk tickets, force-load or virtual stock.

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
- cart standing on already-loaded `BlockTags.RAILS` at cart position or one block below;
- 0.43 physical railhead complete at the current endpoint.

### Departure loading
The Chest Minecart must be completely empty and have no freight owner manifest.

The source endpoint is not every nearby `통`. `FreightService` resolves the exact outpost anchor to its existing `FieldDepotData.DepotEntry`, then uses only:
- that physical registered `통(Barrel)` anchor;
- that anchor's persisted linked warehouse `통`.

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
- cart must still be on loaded rail inside a complete physical railhead;
- destination resolves only its own physical anchor + linked warehouse `통`.

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
Create remains design-reference-only. Survival Ascension studies the product-level value of physical stock movement but does not copy train, contraption, package, Stock Link, Stock Ticker, Requester, routing, GUI, asset, data or namespace implementation. FreightService and FreightRailheadService are independent code using vanilla Chest Minecart/rail/redstone/Container behavior plus Survival Ascension's existing physical `통` records.

## 0.41 Civil Works Causeways retained
`CIVIL_WORKS` is Stage1: Stone Bricks2048 + Cobblestone1536 + Gravel1536 + Iron256 + Copper256. Final funding needs owned registered `통` within4 plus radius6 physical yard: Stone Bricks48, Scaffolding16, Iron Blocks4, Stonecutters2, Crafting Table1.

`CAUSEWAY("causeway", "도로/교량", 60)` uses the existing protected Construction queue. It lays the actual manually selected BlockItem as a flat 3-wide forward deck:17/33/49/65 length. Loaded-only, `mayInteract`, placement hooks, physical-logistics-first/player material, no terrain erasure or free supports.

## 0.40 Physical Siege Breachers retained
Only Bastion wave4 Ravager/Vindicator can damage qualifying fortification; normal three-wave defense remains non-destructive. Every break obeys loaded-only, mobGriefing, owner protection, entity-destroyability and NeoForge destroy event.

## 0.39 Physical Bastion Defense retained
Physical fortification uses radius6..12, Y-3..+4, four quadrants each12 unique x/z columns. Bastion remains supply2 /4 waves /6000 ticks and composition-driven rather than blanket-stat driven.

## 0.38 Defendable Physical Outposts retained
Normal defense remains supply1 /3 waves /4800 ticks with owner64, physical structure validation and breach radius6/limit200.

## 0.37 Physical Warehouse Clusters retained
`field_depots_v1` keeps max3 anchors/player, max8 satellite `통`/anchor inside6. Real Container contents only, loaded-only, no virtual capacity.

## 0.36 Physical Commissioning retained
Civil Works, Industrial Works, Apex Tracking Post and Ascension Nexus use the same final-call real-world commissioning engine. It is one-time proof, not ongoing maintenance.

## 0.35 / 0.34 logistics retained
High-volume offload scans main inventory9..35. Stationary logistics-backed material sinks use nearest usable real `통` storage first, then player inventory. Apex/Trial admission stays player-carried.

## 0.33 / 0.32 expeditions retained
Active regional outpost -> supply1 -> cross range -> two real validated objectives -> exact-origin return. One bounded complication per new sortie. No force-load.

## 0.31 Field Recovery retained
One prepaid ordinary-death return within96; authored encounter deaths remain excluded. No ordinary fast travel.

## 0.30 Physical Outposts retained
Owned registered `통` + Bed/Campfire/Crafting/Furnace-family within5. Owner-nearby activation64, logistics64, NATURAL-hostile safety24.

## Production retained
METALWORKS / TIMBERWORKS / PROVISIONS / PRECISION remain. One full cycle grants supply1, cap3. Dispatch remains a player-carried reward rather than freight/remote payment.

## Mastery / Field Mastery retained
Lv100 base: Mining11×11+vein192, Wood384, Harvest11×11, Construction49/11×11, combat6.5/16, air dash3.
After all nine regions: Quarry7×7×12, Wood448, Harvest13×13, Construction65/13×13 plus Causeway3×65, combat7.5/20, air dash4.