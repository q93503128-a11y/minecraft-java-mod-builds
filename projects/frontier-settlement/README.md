# Frontier Settlement

Minecraft Java 26.2 / NeoForge 26.2 cooperative survival settlement-growth mod.

Canonical direction: `ORIGINAL_DESIGN_v0.2.md` + `CANONICAL_PLAN.md`. Remaining original-scope gaps are tracked in `COMPLETION_GAP_AUDIT.md`.

## Current version: 0.1.0-alpha.34

Frontier Settlement owns the shared settlement, physical construction, residents, production, roads, outposts, logistics and territory progression. It deliberately uses a locked external-content stack for biome, dungeon, structure, combat, weapon, loot and exploration breadth instead of rebuilding all of that from scratch.

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

Alpha.34 adds no new gameplay key or separate logistics dashboard.

## Functional building families

Current functional families: **11**.

- house;
- lumber camp;
- farm;
- quarry;
- mine;
- warehouse;
- blacksmith;
- workshop;
- guard post;
- market;
- **cart station**.

The original v0.2 target remains roughly 15–20 meaningful families. Construction office, watchtower, barracks and advanced workshop remain unfinished, together with small road stairs/bridges and later territory specializations.

## Physical construction

Building approval does not instantly mutate the world or delete the full project cost.

`approval -> physical grading -> real material hauling -> foundation/frame/walls/roof/finish -> completion`

- builder visits loaded work cells and grades only validated terrain;
- shallow support uses coarse dirt rather than free recoverable economic material;
- real wood/stone stacks are extracted from loaded settlement storage in bounded batches;
- a protected physical site barrel stages materials;
- unsafe obstructions pause work instead of being silently destroyed;
- no `destroyBlock` / loose-drop construction path;
- save migration preserves older active projects.

Roads and outposts likewise use physical grading and real hauled resources.

## Residents, production and road logistics

- builder, logger, farmer, quarry worker, miner, workshop artisan, guard and transport roles are implemented;
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

Alpha.34 closes the first original-design cart-station slice without introducing a second transport controller.

- new `CART_STATION` building family;
- unlocks at village tier after a road-connected outpost exists;
- cost: wood 104 / stone 56;
- 13×9 depot blueprint with covered loading platforms, a short visible rail/loading lane and four physical freight barrels;
- placement must be within 12 blocks of an existing road so the last-mile delivery remains spatially readable;
- freight barrels are protected functional infrastructure and also participate in the ordinary shared physical-storage ledger;
- existing outpost transport workers still own all route navigation;
- when a station exists, incoming outpost cargo prefers its freight barrels before ordinary town storage;
- per-trip outpost cargo capacity increases from **16 to 32**;
- if station barrels are full, delivery falls back to other valid settlement storage rather than deleting cargo;
- no teleport cargo, chunk force-load, abstract freight points or second logistics AI was added.

The current station is a **physical freight depot**, not yet a moving wagon entity. A future visual wagon must remain presentation layered on top of the single road-logistics authority rather than becoming another simulation backend.

## External content stack

`COMPANION_LOCK.json` is the exact candidate lock for the next fresh-world compatibility test. `COMPANION_MODS.md` explains the strategy and `EXTERNAL_CONTENT_REGISTER.md` records reuse/license boundaries.

Current candidate stack includes:

- Terralith + Lithostitched;
- Dungeons and Taverns;
- Repurposed Structures;
- Better Combat + Cloth Config + Player Animation Library;
- Weapons Expanded;
- Lootr;
- Sophisticated Backpacks + Sophisticated Core;
- Jade;
- Xaero's Minimap.

The lock deliberately remains `candidate_runtime_lock` until the full client/server set is actually launched together. World-generation entries must be installed before creating that test world.

## Validation

Canonical CI performs:

1. source audit;
2. Java 25 clean Gradle build;
3. runtime JAR verification;
4. artifact upload;
5. result recording to `ci-results/frontier-settlement/`.

Automated validation proves source/build/JAR consistency, not hands-on pathfinding, visual quality, balance or full companion-stack runtime compatibility. Those still require real Minecraft play.
