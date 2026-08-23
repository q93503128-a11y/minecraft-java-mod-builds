# Frontier Settlement

Minecraft Java 26.2 / NeoForge 26.2 cooperative survival settlement-growth mod.

Canonical direction: `ORIGINAL_DESIGN_v0.2.md` + `CANONICAL_PLAN.md`. Remaining original-scope gaps are tracked in `COMPLETION_GAP_AUDIT.md`.

## Current version: 0.1.0-alpha.36

Frontier Settlement owns the shared settlement, physical construction, residents, production, roads, outposts, logistics, defense infrastructure and territory progression. It deliberately uses a locked external-content stack for biome, dungeon, structure, combat, weapon, loot and exploration breadth instead of rebuilding all of that from scratch.

The current implementation is a broad playable alpha, not the final 1.0 scope. Do not call it complete while original v0.2 `부분/미구현` items remain.

## Core loop

`survival -> settlement growth -> better exploration -> external exploration / conquest -> NPCs / resources / technology -> settlement growth`

- one shared settlement per world/server;
- server-authoritative resources/buildings/population/roads/outposts;
- actual Minecraft ItemStacks remain resource authority;
- repeated hauling/production/job assignment is automated;
- players keep exploring, fighting and choosing where the settlement expands.

## Controls

Normal play remains compact:

- `B` — settlement building/infrastructure palette;
- `R` — rotate active building placement;
- `Enter` — confirm active building/road/outpost placement;
- `Backspace` — reset/cancel the current road-start step.

Alpha.36 adds no new gameplay key or separate defense dashboard.

## Functional building families

Current functional families: **12**.

- house;
- lumber camp;
- farm;
- quarry;
- mine;
- warehouse;
- blacksmith;
- workshop;
- guard post;
- **watchtower**;
- market;
- cart station.

The original v0.2 target remains roughly 15–20 meaningful families. Construction office, barracks and advanced workshop remain unfinished, together with later territory specializations.

## Physical construction

Building approval does not instantly mutate the world or delete the full project cost.

`approval -> physical grading -> real material hauling -> foundation/frame/walls/roof/finish -> completion`

- builder visits loaded work cells and grades only validated terrain;
- shallow support uses coarse dirt rather than free recoverable economic material;
- real wood/stone stacks are extracted from loaded settlement storage in bounded batches;
- a protected physical site barrel stages materials;
- tall buildings such as the watchtower reuse the persisted construction-scaffold system rather than appearing instantly;
- unsafe obstructions pause work instead of being silently destroyed;
- no `destroyBlock` / loose-drop construction path;
- save migration preserves older active projects.

Roads and outposts likewise use physical grading and real hauled resources.

## Residents, production and road logistics

- builder, logger, farmer, quarry worker, miner, workshop artisan, guards/service behavior and transport roles are implemented;
- loaded town production is paced and bounded;
- outpost production is specialization-specific and loaded-chunk only;
- transport workers are persistently assigned to one outpost;
- transport follows persisted road-center waypoints;
- unloaded route boundaries pause transport instead of force-loading or teleporting;
- Alpha.27 tagged road logistics remains the **single authority for outpost transport**.

## Alpha.31 — external content becomes a Frontier input

Frontier exposes additive physical-item tags for settlement wood/stone/metal/food and expedition relics, understands compatible `c:` material tags, and recognizes external weapons such as Weapons Expanded without requiring companion loader classes merely to boot.

`/frontier status` can report recognized physical expedition relics and external weapons in loaded settlement storage.

## Alpha.32 — physical market

The market connects exploration loot back into settlement value:

- dedicated protected trade barrel;
- ordinary shared storage is never auto-sold;
- only deliberately deposited `frontier_settlement:expedition_relics` are eligible;
- loaded daytime visiting merchant approaches the barrel;
- payment is a real emerald ItemStack;
- full output safely stalls instead of deleting goods.

## Alpha.33 — staffed workshop

The workshop connects external weapons to settlement support:

- workshop unlocks after the blacksmith;
- dedicated protected service barrel;
- only damaged recognized external weapons deliberately put there are maintained;
- one assigned workshop artisan follows housing/food/population rules;
- artisan physically walks to loaded settlement storage, extracts one real metal item and carries it back;
- one metal repairs 64 durability;
- unused carried material is physically returned rather than teleported.

## Alpha.34 — cart station / physical freight hub

- `CART_STATION` unlocks after village tier + road-connected outpost;
- cost: wood 104 / stone 56;
- 13×9 depot with four physical freight barrels;
- placement must be within 12 blocks of an existing road;
- incoming outpost cargo prefers freight barrels before ordinary town storage;
- per-trip transport capacity becomes 32 instead of 16;
- full station falls back to other valid settlement storage;
- no second logistics AI, teleport cargo or chunk force-load.

The current station is a physical freight depot, not yet a moving wagon entity.

## Alpha.35 — automatic road stairs and small bridges

- one-block longitudinal height changes use actual cobblestone stair road pieces;
- bounded water runs between dry banks may become 3-wide stone-brick decks;
- automatic water span is capped at 6 centerline blocks;
- both banks must exist and differ by at most one block;
- water remains in place;
- bridge grading creates no free support materials;
- stairs/bridges add real stone cost to physical hauling;
- optional active-road bridge profile remains save-compatible with older roads;
- completed road centerlines remain ordinary `RoadSegment` paths, preserving transport compatibility.

This does not claim large ravine bridges, tunnels, retaining walls or arbitrary mountain-road terraforming.

## Alpha.36 — watchtower / loaded threat response

Alpha.36 closes the first original-design watchtower slice while keeping barracks as a later military layer.

- new `WATCHTOWER` functional building;
- unlocks after one completed guard post;
- cost: wood 96 / stone 72;
- compact 7×7 footprint with a tall physical timber/stone tower, ladder, observation deck, lamps and bell;
- existing builder grading, real-material hauling and construction scaffolds build the tower normally;
- every completed, loaded tower maintains one persistently tagged response iron golem assigned to that tower;
- the tower checks a 40-block horizontal threat radius every 100 ticks;
- nearby loaded `Monster` entities can become response targets;
- creepers are intentionally excluded from forced watchtower targeting so the tower does not drag explosion risk toward settlement infrastructure;
- when no eligible threat remains, the response guard clears its forced target and returns toward the tower;
- watchtower behavior does not force-load chunks;
- watch guards are defense units, not settlement population and not the future barracks soldier system.

Current watchtower detection is **loaded-world defense**, not a global radar. A player-facing warning/notification layer and true barracks soldiers remain later scope.

## External content stack

`COMPANION_LOCK.json` is the exact candidate lock for the next fresh-world compatibility test. `COMPANION_MODS.md` explains the strategy and `EXTERNAL_CONTENT_REGISTER.md` records reuse/license boundaries.

Current candidate stack includes Terralith + Lithostitched, Dungeons and Taverns, Repurposed Structures, Better Combat + its libraries, Weapons Expanded, Lootr, Sophisticated Backpacks + Core, Jade and Xaero's Minimap.

The lock deliberately remains `candidate_runtime_lock` until the full client/server set is actually launched together. World-generation entries must be installed before creating that test world.

## Validation

Canonical CI performs:

1. source audit;
2. Java 25 clean Gradle build;
3. runtime JAR verification;
4. artifact upload;
5. result recording to `ci-results/frontier-settlement/`.

Automated validation proves source/build/JAR consistency, not hands-on pathfinding, watchtower combat behavior, stair/bridge appearance, balance or full companion-stack runtime compatibility. Those still require real Minecraft play.
