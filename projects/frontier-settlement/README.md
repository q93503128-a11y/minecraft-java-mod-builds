# Frontier Settlement

Minecraft Java 26.2 / NeoForge 26.2 cooperative survival settlement-growth mod.

Canonical direction: `ORIGINAL_DESIGN_v0.2.md` + `CANONICAL_PLAN.md`. Remaining original-scope gaps are tracked in `COMPLETION_GAP_AUDIT.md`.

## Current version: 0.1.0-alpha.40

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

Alpha.40 adds no new gameplay key, fishing dashboard or manual job-priority UI.

## Functional building families

Current functional families: **15**.

- house;
- lumber camp;
- farm;
- quarry;
- mine;
- warehouse;
- construction office;
- blacksmith;
- workshop;
- **advanced workshop**;
- guard post;
- watchtower;
- barracks;
- market;
- cart station.

The original v0.2 target remains roughly 15–20 meaningful families. The headline count is now inside that range, but this is not scope completion: Alpha.40 adds the first coast/river fishing-trade specialization, while dangerous-region military specialization, richer port/trade presentation, unloaded simulation and companion/UI integration remain unfinished.

## Physical construction

Building approval does not instantly mutate the world or delete the full project cost.

`approval -> physical grading -> real material hauling -> foundation/frame/walls/roof/finish -> completion`

- builder visits loaded work cells and grades only validated terrain;
- shallow support uses coarse dirt rather than free recoverable economic material;
- real wood/stone stacks are extracted from loaded settlement storage in bounded batches;
- a protected physical site barrel stages materials;
- tall/large buildings reuse the persisted construction-scaffold system rather than appearing instantly;
- unsafe obstructions pause work instead of being silently destroyed;
- no `destroyBlock` / loose-drop construction path;
- save migration preserves older active projects.

Roads and outposts likewise use physical grading and real hauled resources.

## Residents, production and road logistics

- builder, logger, farmer, quarry worker, miner, fishing outpost worker, workshop artisan, construction supply runner, advanced forging specialist, guards/service behavior and transport roles are implemented;
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

- dedicated protected trade barrel;
- ordinary shared storage is never auto-sold;
- only deliberately deposited `frontier_settlement:expedition_relics` are eligible;
- loaded daytime visiting merchant approaches the barrel;
- payment is a real emerald ItemStack;
- full output safely stalls instead of deleting goods.

## Alpha.33 — staffed workshop

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

- new `WATCHTOWER` functional building;
- unlocks after one completed guard post;
- cost: wood 96 / stone 72;
- compact 7×7 footprint with a tall physical timber/stone tower, ladder, observation deck, lamps and bell;
- every completed, loaded tower maintains one persistently tagged response iron golem assigned to that tower;
- 40-block loaded threat radius, with creepers excluded from forced pursuit;
- when no eligible threat remains the guard returns toward the tower;
- no chunk force-load;
- watch guards are baseline defense units, not civilian population.

## Alpha.37 — supplied barracks / regular garrison

- new `BARRACKS` functional building;
- unlock: **frontier-town tier + one watchtower + one blacksmith**;
- cost: wood 144 / stone 112;
- 15×11 physical barracks with enclosed quarters/armory, three visible bunk stations and a rear drill yard;
- one barracks owns **3 persistent military slots**;
- military capacity is separate from civilian population/housing;
- each recruited unit costs **8 real food + 2 real metal**;
- unloaded barracks/patrol areas are not interpreted as missing soldiers and no chunks are force-loaded;
- tagged barracks combat proxies drop no iron/resources on death;
- old free frontier-town/domain reinforcement backend is removed.

The current regular soldier combat body is an **Iron Golem proxy**. Humanoid soldier visuals/equipment/formations remain later presentation/combat-depth work.

## Alpha.38 — construction office / physical material staging

Alpha.38 closes the original construction-office family without adding an abstract construction-speed percentage or a second builder authority.

- new `CONSTRUCTION_OFFICE` functional building;
- unlock: **village tier + one warehouse**;
- cost: wood 112 / stone 64;
- 13×9 physical office/work yard with four protected material barrels, work tables, tools and visible construction supplies;
- office material barrels join the same physical settlement ItemStack ledger;
- for construction wood/stone they are ordered ahead of ordinary town storage, so the existing builder naturally prefers nearby staged stock;
- automated non-construction cargo such as food/metal/random loot is kept out of the dedicated material bays;
- one persistent office-assigned **construction supply runner** keeps the bays stocked only while a building project is active;
- the runner uses loaded ordinary settlement storage as source, physically walks to it, extracts a real wood/stone stack, carries up to 32 items in hand and walks back before depositing;
- target staged reserve is 96 wood + 96 stone per office;
- source search is bounded to 24 blocks and checked along loaded corridor points; no chunk force-load or teleport inventory transfer is introduced;
- at night or when no building project exists the runner returns to its office instead of performing background logistics;
- the runner is a construction service unit like the original dedicated builder and does **not** inflate civilian population/housing;
- `/frontier status` reports staged wood/stone and loaded supply-runner count;
- the existing `SettlementConstructionService` remains the only authority that grades terrain, consumes staged project cost and places blueprint blocks.

This is a logistics/readability improvement, not a claimed fixed percentage speed buff. Actual travel-time gains depend on settlement layout and require real-play pacing validation.

## Alpha.39 — advanced workshop / rare-material forging

Alpha.39 adds the first real high-tier crafting loop instead of turning rare exploration loot into another currency number.

- new `ADVANCED_WORKSHOP` functional building;
- unlock: **frontier-town tier + one workshop + one market**;
- cost: wood 168 / stone 120;
- 15×11 physical stone/timber forging hall with protected commission barrel, smithing table, anvil, enchanting table, grindstone and blast furnace;
- the commission barrel is deliberately **not** generic settlement storage: the player explicitly chooses what to commission by placing an eligible weapon and relic there;
- a forgeable commission requires one recognized, currently unenchanted external damageable weapon plus **1 expedition relic**;
- one building-bound advanced forging specialist physically walks to loaded shared settlement storage and carries real metal back to the commission barrel;
- one forge consumes **4 real metal + 1 real relic**;
- the system first builds and validates a compatible level-30 enchanting-table result; if the external weapon supports no valid enchantment, no metal or relic is consumed;
- on success the same weapon is fully repaired and receives the generated compatible enchantments;
- ordinary shared storage is never scanned for a weapon/relic commission, so market sale intent and advanced-forging intent cannot silently steal from each other;
- no hard companion class/item dependency is introduced; eligible external weapons still come through the existing soft recognition seam;
- no force-load, teleport inventory transfer or abstract crafting points;
- `/frontier status` reports ready commissions and the 1 relic / 4 metal recipe.

Role separation is intentional: **market = relic→trade value, workshop = metal→repair, advanced workshop = external weapon + relic + metal→high-tier forge**. The current advanced specialist is a building-bound visible service NPC rather than a civilian population slot; broader citizen-job reconciliation remains later cleanup. Alpha.39 is the first high-tier forge path, not a claim that every future specialized recipe/crafting family is complete.

## Alpha.40 — coast/river fishing-trade outpost

Alpha.40 makes water geography matter without adding a new management screen or a second long-distance logistics controller.

- an otherwise-`general` completed outpost is checked only while its local chunks are loaded;
- within a 12-block radius it must have at least 24 open surface-water columns plus a safe dry bank;
- qualifying outposts expose the effective role `어업·수변교역` in `/frontier status`;
- one outpost-assigned fishing villager appears with a fishing rod in the off hand;
- the worker physically walks from the outpost to the detected bank and fishes only while the water position remains valid and loaded;
- work cadence is 140 ticks, producing 1–3 real cod/salmon ItemStacks per successful catch;
- caught fish are carried in the worker's main hand back to the **existing physical outpost stockpile**;
- fish are ordinary edible ItemStacks, so the existing Alpha.27 outpost transporter already accepts them through the normal food cargo rule and follows the persisted road network to town/cart-station storage;
- no new route authority, teleport cargo, global water scan, forced chunk loading, emerald generation or abstract trade points are introduced;
- if the shoreline is not loaded or no longer qualifies, production pauses instead of simulating catches remotely.

The `수변교역` part currently means the fishing commodity participates in the established physical outpost→road→town economy. Alpha.40 does **not** claim a dedicated harbor, boats, waterborne merchant NPCs or a separate fish-for-emerald market. Those remain possible later presentation/economic breadth.

## External content stack

`COMPANION_LOCK.json` is the exact candidate lock for the next fresh-world compatibility test. `COMPANION_MODS.md` explains the strategy and `EXTERNAL_CONTENT_REGISTER.md` records reuse/license boundaries.

Current candidate stack includes Terralith + Lithostitched, Dungeons and Taverns, Repurposed Structures, Better Combat + its libraries, Weapons Expanded, Lootr, Sophisticated Backpacks + Core, Jade and Xaero's Minimap.

The lock deliberately remains `candidate_runtime_lock` until the full client/server set is actually launched together. World-generation entries must be installed before creating that test world.

## Validation

Canonical CI performs:

1. the complete established Alpha.23–39 source audit plus Alpha.40 waterborne-outpost extension;
2. Java 25 clean Gradle build;
3. runtime JAR verification;
4. artifact upload;
5. result recording to `ci-results/frontier-settlement/`.

Automated validation proves source/build/JAR consistency, not hands-on shoreline pathfinding, fishing cadence/balance, advanced-forging balance, external-weapon enchant compatibility breadth, construction-supply pathfinding, garrison combat or full companion-stack runtime compatibility. Those still require real Minecraft play.
