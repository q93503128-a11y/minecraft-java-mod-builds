# Frontier Settlement

Minecraft Java 26.2 / NeoForge 26.2 cooperative survival settlement-growth mod.

Canonical direction: `ORIGINAL_DESIGN_v0.2.md` + `CANONICAL_PLAN.md`. Remaining original-scope gaps are tracked in `COMPLETION_GAP_AUDIT.md`.

## Current version: 0.1.0-alpha.32

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

No extra market hotkey or separate shop dashboard was added in Alpha.32.

## Functional building families

Current functional families: **9**.

- house;
- lumber camp;
- farm;
- quarry;
- mine;
- warehouse;
- blacksmith;
- guard post;
- **market**.

The original v0.2 target remains roughly 15–20 meaningful families, with workshop, construction office, cart station, watchtower, barracks and advanced workshop still on the completion track.

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

- builder, logger, farmer, quarry worker, miner, guard and transport roles are implemented;
- loaded town production is paced and bounded rather than instant;
- remote outpost production is specialization-specific and loaded-chunk only;
- lumber/quarry/mining consume actual finite nearby world resources where appropriate;
- agriculture uses actual vanilla crop growth;
- transport workers are persistently assigned to one outpost;
- transport follows persisted road-center waypoints;
- unloaded route boundaries pause transport instead of force-loading or teleporting;
- higher tiers add non-destructive tier-visible road public works and garrison benefits.

Alpha.27 tagged road logistics remains the only outpost-transport authority.

## Alpha.31 — external content becomes a Frontier input

Alpha.31 changed external mods from a passive recommendation list into a data-driven content source.

Frontier now exposes additive item tags for:

- settlement wood;
- settlement stone;
- settlement metal;
- settlement food;
- expedition relics.

It also understands conventional `c:` material tags where applicable, so compatible external materials can enter the same physical settlement economy without Java hard dependencies.

`/frontier status` can report physical expedition relics and recognized external weapons found in loaded settlement storage. The first recognized external weapon namespace is Weapons Expanded.

No companion loader class is required merely to boot Frontier or classify a physical item: missing companions degrade to no matching content rather than a startup failure.

## Alpha.32 — physical market / exploration-to-settlement bridge

Alpha.32 adds the original-design **market** as the first real consumer of external exploration content.

- market unlocks at the `VILLAGE` settlement tier;
- cost: wood 96 / stone 48;
- physical 11×11 open market blueprint with covered stalls and civic pavilion;
- one dedicated protected trade barrel is part of the completed market;
- one visible tagged `방문 상인` works at each loaded market during the daytime;
- **ordinary shared settlement storage is never auto-sold**;
- only items the player deliberately puts in the market trade barrel can be sold;
- only `frontier_settlement:expedition_relics` are eligible;
- one relic is processed per readable trade cycle;
- payment is a real emerald ItemStack inserted into the same barrel;
- if the barrel cannot accept the payout, the transaction does not consume the relic;
- no abstract trade currency, teleport delivery or new shop UI was added.

Baseline relic values currently include Echo Shard, Heart of the Sea, Enchanted Golden Apple, Heavy Core, Trial Key, Ominous Trial Key and Ominous Bottle. Future external dungeon loot can join through the additive relic tag rather than a new Java branch for every mod.

This closes only the first market/merchant loop. Broader high-tier trading and specialist crafting remain future original-scope work.

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

Alpha.32 code checkpoint `62ece48b90d4f36575194fd3dea7863edf0f61bd` passed source audit, Java25 build and JAR verification in run `32631924968` before this documentation update.

Automated validation proves source/build/JAR consistency, not hands-on navigation, visual quality or full companion-stack runtime compatibility. Those still require real Minecraft play.
