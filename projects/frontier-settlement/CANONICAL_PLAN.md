# Frontier Settlement — Canonical Plan

This file is the repository-side implementation authority for Frontier Settlement. Read it together with `ORIGINAL_DESIGN_v0.2.md`, current `main` source, `COMPLETION_GAP_AUDIT.md`, companion lock/register, README and CI results before continuing development.

`ORIGINAL_DESIGN_v0.2.md` is the scope ceiling/foundation. This file may make that design more concrete, but it must not silently shrink unfinished original requirements to match whatever is currently implemented.

## 1. Product identity

Frontier Settlement is a Minecraft Java cooperative survival settlement / territory-growth mod.

Core loop:

`survival -> settlement growth -> better exploration -> external exploration / conquest -> NPCs / resources / technology -> settlement growth`

Player role progression:

`survivor -> pioneer -> village leader / lord-like role -> domain operator`

Settlement scale progression:

`pioneer camp -> hamlet -> village -> frontier town -> domain`

The settlement becomes deeper while the player's direct management stays simple. Minecraft exploration, gathering, combat and choosing where the settlement expands remain foreground play.

## 2. Interaction budget

Hard rule: many backend systems are allowed; direct micromanagement stays small.

Primary direct interactions remain approximately:

1. put/take physical items from shared settlement storage;
2. choose a building and its position/rotation;
3. choose an outpost location;
4. choose road start/end and only necessary route guidance;
5. explore/fight and decide which rare/external loot to commit to settlement progression or trade.

Do not grow the project into tax rates, dozens of happiness stats, family schedules, per-worker priority tables, giant research menus or manual hauling routes.

Market sale intent is expressed by putting an eligible relic in the market barrel. Workshop maintenance intent is expressed by putting an eligible weapon in the workshop service barrel. Cart-station logistics, road terrain adaptation, watchtower defense and barracks recruitment require no separate player-management dashboard.

## 3. Multiplayer authority

One world/server has one shared settlement and territory state.

- settlement resources/buildings/population/roads/outposts/progression are shared;
- server is authoritative;
- clients render state and submit bounded requests;
- no independent per-player settlements in planned scope;
- no internal player politics/tax distribution layer in initial scope.

## 4. Founding and early loop

Intended start:

- player places/uses the pioneer marker at a chosen overworld site;
- one shared settlement is founded;
- a small authoritative physical stockpile is established;
- one dedicated builder exists from the start;
- players manually secure first resources;
- builder performs physical site preparation and construction.

The saved starter stockpile position is part of the authoritative early loop and must not be casually destroyed into a progression softlock.

## 5. Buildings

Functional settlement buildings use official blueprints. Player/vanilla buildings remain welcome visually but are not scanned/registered as functional settlement buildings.

Original target remains roughly **15–20 meaningful building families**.

Current Alpha.37 functional families: **13**.

- house;
- lumber camp;
- farm;
- quarry;
- mine;
- warehouse;
- blacksmith;
- workshop;
- guard post;
- watchtower;
- barracks;
- market;
- cart station.

Original families still to close include construction office and advanced workshop. Town center currently exists through the civic-core progression rather than a separate placement family. Additional late territory/production families remain allowed where they serve the original scope rather than padding the count.

Construction UX:

1. open compact palette;
2. choose building;
3. see completed 3D ghost preview;
4. choose position/rotation;
5. validate terrain/overlap/cost;
6. create a project transaction without instantly mutating the site;
7. builder physically clears/grades safe terrain;
8. builder/workers physically haul resources and build in readable phases.

Construction intention:

`site clearing -> hauling -> foundation -> frame -> walls -> roof -> interior -> completion`

Terrain rules:

- small differences may be graded by visible worker action;
- medium differences should eventually show explicit terrain-work intent rather than silently failing or flattening a mountain;
- extreme/unsafe terrain is rejected;
- no silent destruction of player containers, fluids, valuable blocks or unrelated infrastructure;
- no loose-drop terrain farming from project approval;
- project grading must not create recoverable free economic materials;
- roof before support and visually floating foundations are invalid;
- enclosed completed buildings need deliberate windows/lighting.

Tall/large structures such as watchtowers and barracks reuse the persisted scaffold/building workflow. They do not get an instant-placement exception.

## 6. Citizens, jobs and defense roles

Vanilla villager trading/professions are not the settlement progression authority.

Implemented role families include builder, logger, farmer, quarry worker, miner, workshop artisan, guards/service behavior, market merchant presentation, road-bound transport and a supplied barracks garrison.

Defense role separation is authoritative:

- guard post: close local routine defense;
- watchtower: loaded-area longer-range threat observation/response with one tower-assigned response guard;
- barracks: formal multi-slot garrison whose replacements cost physical settlement supplies.

Watch guards and barracks soldiers are defense capacity, not civilian population. They must not inflate population-based tier thresholds. Barracks bunks are military capacity and must not be consumed by ordinary civilian worker assignment.

### Alpha.37 barracks rule

- unlock at `FRONTIER_TOWN` after at least one watchtower and one blacksmith;
- one barracks has 3 military slots;
- each missing loaded slot may be replenished automatically at a bounded cadence;
- one recruit costs 8 physical food + 2 physical metal from loaded shared settlement storage;
- food and metal availability is validated together before mutation;
- a soldier is assigned by persistent barracks coordinates + stable slot tag, not UUID ordering;
- unloaded patrol areas are not treated as dead/missing soldiers;
- no chunk force-loading for patrol or recruitment;
- garrison death drops are cleared so the combat proxy cannot become a resource farm;
- current combat body is an Iron Golem proxy, not final humanoid soldier presentation;
- final equipment/loadout/class/formation depth remains later work and should reuse external combat/weapon content where useful.

The former tier-based free frontier-town/domain reinforcement backend is removed. `SettlementTierInfrastructureService` owns public works only; barracks is the formal high-tier garrison authority.

Still planned from original scope: stronger blacksmith presentation, merchant depth and specialist crafter/high-tier crafting.

Job slots should fill automatically from housing, food and workplaces. Loaded areas show physical movement/work. Do not force-load chunks solely to keep animations running.

Night routines must preserve daytime authority. Transport/night behavior must not introduce a second route controller.

No family/children simulation in planned scope.

## 7. Resources and physical logistics

Resources remain physical Minecraft items. HUD numbers are a cached view, not authority.

- workers deposit real ItemStacks;
- construction consumes real ItemStacks;
- barracks recruitment consumes real food/metal ItemStacks;
- avoid every-tick scanning of arbitrary player chests;
- use tags/categories so compatible external materials can participate;
- warehouses and cart-station freight bays add physical storage positions rather than abstract capacity currency.

Construction presentation invariant: the builder walks from actual settlement storage carrying real wood/stone stacks and visibly stages/uses them at the site.

Distant logistics remains spatial. Transport workers belong to a specific outpost, follow persisted road-network waypoints, carry actual cargo and pause at unloaded route boundaries rather than teleporting or force-loading.

Alpha.27 tagged road logistics remains the single authority for outpost transport at every tier. Do not reintroduce generic-name/UUID pairing or a second transport navigation controller.

### Alpha.34 cart-station rule

The cart station is a town-side physical freight hub, not a new logistics simulator.

- unlock: village tier + at least one road and road-connected outpost;
- placement must be within 12 blocks of an existing road;
- four rotation-aware physical freight barrels join normal settlement storage;
- existing assigned outpost transporter still owns the whole route;
- incoming outpost cargo prefers cart-station freight bays before ordinary town storage;
- station increases per-trip outpost cargo from 16 to 32;
- a full station safely falls back to another valid settlement storage target;
- no teleport cargo, no force-load and no abstract freight currency;
- no `SettlementCartStationService.tick` or second navigation authority.

A future moving wagon must remain presentation/vehicle behavior driven by the existing route authority rather than a duplicate economic simulation.

## 8. Roads, outposts and territory

Roads/outposts are the spatial-growth layer. Do not make progression mean endlessly enlarging one flat central base.

Road intent:

`choose start -> choose route/end -> preview -> approve -> physical grading/build`

Roads should avoid destructive tunneling and reckless cliff modification. Alpha.35 implements the original small-stair/small-bridge requirement for bounded ordinary terrain:

- a one-block longitudinal rise is represented by actual cobblestone stair road pieces on the low side;
- short water runs between dry banks may become a 3-wide stone-brick deck;
- automatic bridge centerline span is capped at 6 blocks;
- both banks must exist and differ by no more than one block;
- bridge water is left in place instead of being filled;
- bridge grading creates no free log/cobblestone support columns;
- stair/bridge profiles add real stone cost to the normal physical hauling transaction;
- optional bridge profile data is persisted only on active `RoadConstructionState`; old saved paths decode as ordinary road;
- completed roads still persist the ordinary centerline `RoadSegment`, so logistics authority remains unchanged;
- deep/large/unsafe crossings, dangerous fluids, 2+ block abrupt grades, protected blocks and unsafe headroom remain rejected.

Large ravine bridges, tunnels, retaining-wall roads and monumental bridge architecture remain later civil-engineering breadth rather than something to generate silently.

No early teleport network; roads/logistics must retain meaning.

Implemented outpost specializations: lumber, quarry, mining, agriculture.

Still required by original scope:

- coast/river fishing/trade specialization;
- dangerous-region military outpost;
- better biome-aware specialization with companion worldgen;
- coarse unloaded simulation that does not violate physical item authority.

tier-visible public works may make established territory more readable, but only in loaded safe locations and without overwriting player work or generating farmable free blocks.

## 9. Combat and exploration

This is not a constant wave-defense game.

- guard posts handle close routine local threats;
- watchtowers extend loaded-area observation/response;
- barracks provide supplied regular garrison capacity at frontier-town scale;
- occasional meaningful threats are allowed;
- do not spam mandatory waves.

Watchtower rule:

- one tagged response guard per loaded tower;
- roughly 40-block loaded threat response at 100-tick cadence;
- creepers excluded from forced pursuit;
- no global radar or chunk force-load.

Barracks rule:

- three barracks-owned stable slots;
- replenishment is supply-backed, not free tier magic;
- patrol remains local and loaded;
- creepers are excluded from forced pursuit;
- military capacity is visible separately from civilian population;
- Iron Golem combat proxy is temporary presentation, not the final soldier-art claim.

Exploration content should include ruins, mines, camps, nests, structures, dungeons, caravans, rare resources, bosses and rare NPCs primarily through the external content stack where strong implementations already exist.

Frontier's job is to connect exploration outcomes back into settlement growth rather than reimplement all adventure content.

## 10. External content is a core development accelerator

External mods are not merely optional visual recommendations. The intended complete play experience uses them to supply content breadth while Frontier remains the settlement/progression glue.

Current candidate 26.2 stack is locked in `COMPANION_LOCK.json` and includes Terralith + Lithostitched, Dungeons and Taverns, Repurposed Structures, Better Combat + required libraries, Weapons Expanded, Lootr, Sophisticated Backpacks + Core, Jade and Xaero's Minimap.

Rules:

- Frontier must still boot without companions unless a future dependency is explicitly justified;
- world/dungeon/combat/weapon systems companions already implement well should not be redundantly rebuilt;
- common/additive item tags are preferred over hard-coded mod classes;
- MIT/LGPL/clear public-license material may be reused only within license obligations and attribution;
- ARR/ND/restricted content stays official dependency/reference-only;
- public source visibility alone is not reuse permission;
- `EXTERNAL_CONTENT_REGISTER.md` records the boundary.

The lock stays `candidate_runtime_lock` until all entries are launched together in the target client/server environment.

## 11. External-content / settlement bridges implemented

### Alpha.31
- additive settlement material/relic tags and external weapon recognition.

### Alpha.32 market
- deliberately deposited expedition relics become physical emerald value without auto-selling shared storage.

### Alpha.33 workshop
- external weapons deliberately deposited for service are repaired using physically fetched settlement metal.

### Alpha.34 cart station
- road-adjacent physical freight depot, preferred outpost delivery and trip capacity 16→32 with one logistics authority.

### Alpha.35 road terrain adaptation
- one-block road stairs and bounded 3-wide short-water stone bridges with real stone cost.

### Alpha.36 watchtower defense
- physical climbable tower, persistent loaded response guard, bounded threat radius and no forced creeper pursuit.

### Alpha.37 barracks garrison
- physical 15×11 barracks and drill yard;
- three separate military slots per barracks;
- bounded automatic recruitment at 8 food + 2 metal per replacement;
- persistent barracks/slot assignment;
- loaded patrol and no force-load;
- garrison does not alter civilian population/housing or tier progression;
- death drops suppressed to prevent iron/resource farming;
- old free tier-garrison backend removed.

## 12. UI and controls

Reference hierarchy from original design remains:

1. Against the Storm — compact resource/status hierarchy;
2. Manor Lords — world-space building/road placement;
3. MineColonies — Minecraft-native blueprint/material/construction presentation;
4. Frostpunk 2 — secondary territory-overview concepts.

Do not invent giant generic rectangle dashboards when a proven interaction reference exists.

Normal gameplay controls remain:

- `B`: settlement palette;
- `R`: rotate current building placement;
- `Enter`: confirm the active building/road/outpost placement;
- `Backspace`: reset/cancel the road start selection step.

Alpha.37 adds barracks below guard post/watchtower in the existing compact defense section. No new gameplay key or military-management screen is introduced. `/frontier status` may expose military capacity/recruitment state for debugging/readability without becoming required micromanagement.

Avoid essential vanilla conflicts. Do not proliferate N/J/K or one new key per feature.

Still missing from original UI scope: stronger building status panel, clearer physical material/progress view and compact side notifications.

## 13. Engineering rules

Target:

- Minecraft Java 26.2;
- NeoForge 26.2.0.38-beta;
- Java 25;
- Gradle 9.2.1.

Development sequence:

`read current GitHub main -> inspect ORIGINAL_DESIGN + CANONICAL_PLAN + gap audit + source + CI -> implement -> manual code/gameplay audit -> Java25 clean build -> JAR verify -> direct main update -> deliver test JAR when useful`

Shared repository rule:

- re-read remote `main` immediately before writes;
- never force-push over concurrent work;
- Frontier path changes only;
- CI result bot may advance main;
- final accepted result must point at the intended Frontier source/docs SHA.

## 14. Current playable slice after Alpha.37

The playable slice now includes:

- one shared authoritative settlement;
- protected founding stockpile and civic core;
- compact B/R/Enter/Backspace interaction;
- **13 functional building families**;
- physical site grading and construction hauling;
- paced loaded town production;
- physical roads with one-block stair adaptation and bounded short-water bridges;
- physical specialized outposts;
- finite/renewable specialization rules;
- persisted road-bound transport;
- loaded-only remote production/logistics;
- tier growth and safe public works;
- Alpha.31 external physical material/relic/weapon recognition;
- Alpha.32 physical village market;
- Alpha.33 staffed physical workshop;
- Alpha.34 road-adjacent physical cart-station freight hub with 32-item transport trips;
- Alpha.35 persisted road bridge profile + real-stone stair/bridge paving;
- Alpha.36 physical watchtower + loaded long-range response guard;
- Alpha.37 physical barracks + separate three-slot supply-backed regular garrison.

This is **not** equivalent to original v0.2 completion. `COMPLETION_GAP_AUDIT.md` remains authoritative for unfinished breadth.

## 15. Next priorities after Alpha.37

Unless real-play regression overrides them:

1. require final Alpha.37 source audit + Java25 build + JAR verify on the final docs/source SHA;
2. add construction office and advanced workshop/high-tier crafting while reusing external rare materials/content;
3. add river/coast fishing/trade and dangerous-region military outpost specializations;
4. assemble and launch the full `COMPANION_LOCK.json` stack in a fresh 26.2 NeoForge world before declaring external runtime compatibility;
5. design coarse unloaded simulation without breaking physical item authority;
6. close UI status/progress/notification gaps;
7. real-play audit barracks construction, three-slot replacement cadence, garrison patrol/return behavior, watchtower overlap and Alpha.35 road navigation;
8. later replace/improve the Iron Golem military proxy only when doing so materially improves soldier presentation and works with the external combat/weapon stack;
9. perform full survival + multiplayer acceptance across founding -> settlement -> roads/outposts -> freight station -> external dungeon loot -> market/workshop -> defense -> higher tiers.

Real-play observations override assumptions. Fix root causes before adding more breadth when testing exposes a regression.
