# Frontier Settlement

Minecraft Java 26.2 / NeoForge 26.2 cooperative survival settlement-growth mod.

Canonical direction: `ORIGINAL_DESIGN_v0.2.md` + `CANONICAL_PLAN.md`. Remaining original-scope gaps are tracked in `COMPLETION_GAP_AUDIT.md`.

## Current version: 0.1.0-alpha.33

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

No extra market/workshop hotkey or separate management dashboard was added.

## Functional building families

Current functional families: **10**.

- house;
- lumber camp;
- farm;
- quarry;
- mine;
- warehouse;
- blacksmith;
- **workshop**;
- guard post;
- market.

The original v0.2 target remains roughly 15–20 meaningful families. Construction office, small bridge, cart station, watchtower, barracks and advanced crafting remain on the completion track. **Workshop is a normal production/maintenance family; advanced crafting remains a separate later-game family.**

## Physical building / road / outpost construction

Building approval does not instantly mutate the world or delete the full project cost.

New building flow:

`approval -> physical grading -> real material hauling -> foundation/frame/walls/roof/finish -> completion`

- builder visits loaded work cells and grades only validated natural/replaceable terrain;
- shallow support uses coarse dirt rather than free recoverable economic material;
- real wood/stone stacks are extracted from loaded settlement storage in bounded batches;
- a protected physical site barrel stages materials;
- cost is consumed gradually as blueprint work proceeds;
- unsafe new obstructions pause work instead of being silently destroyed;
- no `destroyBlock` / loose-drop construction path;
- save migration preserves older active projects.

Roads use physical grading, stone hauling and paving. Outposts use physical grading, wood/stone hauling and blueprint construction.

## Residents, production and logistics

- builder, logger, farmer, quarry worker, miner, workshop artisan, guard and transport roles are implemented;
- loaded town production is paced and bounded rather than instant;
- residents require housing and real settlement food when joining;
- remote outpost production is specialization-specific and loaded-chunk only;
- lumber/quarry/mining consume actual finite nearby world resources where appropriate;
- agriculture uses actual vanilla crop growth;
- transport workers are persistently assigned to one outpost;
- workshop artisans are persistently assigned to one workshop;
- transport follows persisted road-center waypoints;
- unloaded route/assignment evidence pauses reconciliation instead of force-loading or duplicating workers;
- higher tiers add non-destructive tier-visible road public works and garrison benefits.

Alpha.27 tagged road logistics remains the only outpost-transport authority.

## Alpha.31 — external content becomes a Frontier input

Alpha.31 changed external mods from a passive recommendation list into a data-driven content source.

Frontier exposes additive item tags for settlement wood, stone, metal, food and expedition relics. It also understands conventional `c:` material tags where applicable, so compatible external materials can enter the same physical settlement economy without Java hard dependencies.

`/frontier status` can report physical expedition relics and recognized external weapons found in loaded settlement storage. The first recognized external weapon namespace is Weapons Expanded.

No companion loader class is required merely to boot Frontier or classify a physical item: missing companions degrade to no matching content rather than a startup failure.

## Alpha.32 — physical market / exploration-to-settlement bridge

Alpha.32 added the original-design **market** as the first real consumer of external exploration content.

- market unlocks at the `VILLAGE` settlement tier;
- cost: wood 96 / stone 48;
- physical 11×11 open market blueprint;
- dedicated protected trade barrel;
- visible tagged `방문 상인` during daytime;
- **ordinary shared settlement storage is never auto-sold**;
- only deliberately deposited `frontier_settlement:expedition_relics` are sold;
- payment is a real emerald ItemStack in the same barrel;
- full payout inventory stops the transaction without consuming the relic;
- no abstract trade currency or new shop UI.

## Alpha.33 — physical workshop / external-weapon maintenance

Alpha.33 adds the original-design **workshop** as a production/maintenance building and gives external weapons a direct settlement utility loop.

- workshop unlocks after one blacksmith is complete;
- cost: wood 88 / stone 44;
- physical 11×9 workshop with grindstone, smithing table, anvil, blast furnace and a dedicated service barrel;
- the service barrel is the player's explicit opt-in queue: **weapons sitting in ordinary shared storage are never auto-selected or moved**;
- current maintenance target is a damaged external weapon recognized by the existing companion bridge, initially Weapons Expanded;
- one artisan is persistently assigned to each workshop;
- the artisan joins through the ordinary housing/population path and consumes real food 4 from loaded settlement storage;
- the artisan walks to actual loaded settlement storage, extracts **one real metal item**, visibly carries it in the main hand, walks back to the workshop, then consumes it to repair 64 durability;
- maintenance cadence is approximately 5 seconds per completed service cycle;
- if the queued weapon disappears while metal is in hand, the artisan physically walks the material back to storage before depositing it;
- workshop work and population reconciliation stop conservatively when required workshop/storage assignment evidence is not loaded;
- the service barrel is protected infrastructure;
- no force-loaded simulation, teleport material return, abstract repair currency or new key/UI was added;
- workshop artisans join the normal night house routine.

This is intentionally **not** the later `고급 제작소`. The workshop handles routine equipment maintenance and ordinary production support; advanced crafting remains a frontier-town/late-game family for special recipes and progression.

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

External content policy:

- use official dependency JARs/packs where they already solve a problem well;
- reuse MIT/LGPL/clear public-license code/data only with required attribution and compatibility review;
- ARR/ND/custom-restricted assets stay dependency/reference-only;
- public GitHub visibility by itself is not permission to copy assets or code;
- Frontier should spend development time connecting strong external content to settlement growth, not recreating dozens of dungeons, mobs and weapons.

## Validation

Canonical CI performs:

1. source audit;
2. Java 25 clean Gradle build;
3. runtime JAR verification;
4. artifact upload;
5. result recording to `ci-results/frontier-settlement/`.

Automated validation proves source/build/JAR consistency, not hands-on navigation, visual quality or full companion-stack runtime compatibility. Those still require real Minecraft play.
