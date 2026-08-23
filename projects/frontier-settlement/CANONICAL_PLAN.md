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

The settlement should become deeper while the player's direct management remains simple. Minecraft exploration, gathering, building around the settlement and combat must remain the foreground activity.

## 2. Interaction budget

Hard rule: many backend systems are allowed; direct micromanagement stays small.

Primary direct interactions remain approximately:

1. put/take physical items from shared settlement storage;
2. choose a building and its position/rotation;
3. choose an outpost location;
4. choose road start/end and only necessary route guidance;
5. explore/fight and decide which rare/external loot to commit to settlement progression or trade.

Do not grow the project into tax rates, dozens of happiness stats, family schedules, per-worker priority tables, giant research menus or manual hauling routes.

The Alpha.32 market follows the same rule: the player signals sale intent by putting an eligible relic into one dedicated physical trade barrel; there is no extra market hotkey or shop spreadsheet.

## 3. Multiplayer authority

One world/server has one shared settlement and territory state.

- settlement resources/buildings/population/roads/outposts/progression are shared;
- server is authoritative;
- clients render state and submit bounded requests;
- no independent per-player settlements in the planned scope;
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

Debug commands may exist during development, but normal survival play uses world interaction and the compact palette.

## 5. Buildings

Functional settlement buildings use official blueprints. Player/vanilla buildings are welcome visually but are not scanned/registered as functional settlement buildings.

Original target remains roughly **15–20 meaningful building families**, not hundreds of shallow types.

Current Alpha.32 functional families:

- house;
- lumber camp;
- farm;
- quarry;
- mine;
- warehouse;
- blacksmith;
- guard post;
- market.

Original families still to close include construction office, workshop, cart station, watchtower, barracks and advanced workshop; infrastructure also still needs small bridges/stairs.

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

## 6. Citizens and jobs

Vanilla villager trading/professions are not the settlement progression authority.

Implemented role families include builder, logger, farmer, quarry worker, miner, guard/service behavior and road-bound transport. Market uses a visible tagged visiting merchant rather than silently converting items in the background.

Still planned from original scope: stronger dedicated blacksmith presentation, merchant depth, specialist crafter and soldier/barracks role.

Job slots should fill automatically from housing, food and workplaces. Loaded areas show physical movement/work. Do not force-load chunks solely to keep animations running.

Night routines must preserve daytime authority. Transport/night behavior must not introduce a second route controller.

No family/children simulation in planned scope.

## 7. Resources and physical logistics

Resources remain physical Minecraft items. HUD numbers are a cached view, not authority.

- workers deposit real ItemStacks;
- construction consumes real ItemStacks;
- avoid every-tick scanning of arbitrary player chests;
- use tags/categories so compatible external materials can participate;
- warehouses add physical storage positions rather than abstract capacity currency.

Construction presentation invariant: the builder walks from actual settlement storage carrying real wood/stone stacks and visibly stages/uses them at the site.

Distant logistics remains spatial. Transport workers belong to a specific outpost, follow persisted road-network waypoints, carry actual cargo and pause at unloaded route boundaries rather than teleporting or force-loading.

Alpha.27 tagged road logistics remains the single authority for outpost transport at every tier. Do not reintroduce generic-name/UUID pairing or a second transport navigation controller.

## 8. Roads, outposts and territory

Roads/outposts are the spatial-growth layer. Do not make progression mean endlessly enlarging one flat central base.

Road intent:

`choose start -> choose route/end -> preview -> approve -> physical grading/build`

Roads should avoid destructive tunneling and reckless cliff modification. Original v0.2 still requires automatic small stairs/bridges where route quality needs them.

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

- guards handle routine local threats;
- occasional meaningful threats are allowed;
- do not spam mandatory waves.

Exploration content should include ruins, mines, camps, nests, structures, dungeons, caravans, rare resources, bosses and rare NPCs primarily through the external content stack where strong implementations already exist.

Frontier's job is to connect exploration outcomes back into settlement growth rather than reimplement all adventure content.

## 10. External content is a core development accelerator

External mods are not merely optional visual recommendations. The intended complete play experience uses them to supply content breadth while Frontier remains the settlement/progression glue.

Current candidate 26.2 stack is locked in `COMPANION_LOCK.json` and includes:

- Terralith + Lithostitched;
- Dungeons and Taverns;
- Repurposed Structures;
- Better Combat + required libraries;
- Weapons Expanded;
- Lootr;
- Sophisticated Backpacks + Core;
- Jade;
- Xaero's Minimap.

Rules:

- Frontier must still boot without companions unless a future dependency is explicitly justified;
- world/dungeon/combat/weapon systems that companions already implement well should not be redundantly rebuilt;
- common/additive item tags are preferred over hard-coded mod classes;
- MIT/LGPL/clear public-license material may be reused only within license obligations and attribution;
- ARR/ND/restricted content stays official dependency/reference-only;
- public source visibility alone is not reuse permission;
- `EXTERNAL_CONTENT_REGISTER.md` records the boundary.

The lock stays `candidate_runtime_lock` until all entries are launched together in the target client/server environment.

## 11. Alpha.31 external-content bridge

Alpha.31 established a soft, data-driven bridge:

- additive settlement wood/stone/metal/food item tags;
- conventional `c:` material compatibility where applicable;
- additive `frontier_settlement:expedition_relics` tag;
- physical storage scanning for expedition relics;
- recognized external weapon namespace support, initially Weapons Expanded;
- no companion `ModList` requirement merely to classify physical content.

This is the compatibility substrate for later market, workshop, military and progression integrations.

## 12. Alpha.32 physical market

Market is the first original-scope building that directly converts external exploration content into settlement value.

Market rules:

- unlock at `VILLAGE` tier;
- cost wood 96 / stone 48;
- physical 11×11 blueprint;
- dedicated protected trade barrel at a stable rotation-aware local position;
- ordinary shared settlement storage is never auto-sold;
- only `expedition_relics` deliberately placed in the trade barrel are eligible;
- loaded daytime visiting merchant physically approaches the crate;
- one relic is processed per readable trade cycle;
- payout is a physical emerald ItemStack returned to the same barrel;
- insufficient output capacity stalls/rolls back safely rather than deleting goods;
- no abstract trade points or separate shop dashboard.

Current baseline values are intentionally simple and should be adjusted from actual play, especially after real DnT/Repurposed Structures/Lootr loot density is known.

The market is not the final trading system: high-tier trade, external-item recipes and specialist crafting remain original-scope work.

## 13. UI and controls

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

Avoid essential vanilla conflicts. Do not proliferate N/J/K or one new key per feature.

Still missing from original UI scope: stronger building status panel, clearer physical material/progress view and compact side notifications.

## 14. Engineering rules

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

## 15. Current playable slice after Alpha.32

The playable slice now includes:

- one shared authoritative settlement;
- protected founding stockpile and civic core;
- compact B/R/Enter/Backspace interaction;
- 9 functional building families;
- physical site grading and construction hauling;
- paced loaded town production;
- physical roads;
- physical specialized outposts;
- finite/renewable specialization rules;
- persisted road-bound transport;
- loaded-only remote production/logistics;
- tier growth and safe public works;
- Alpha.31 external physical material/relic/weapon recognition;
- Alpha.32 physical village market converting deliberately deposited expedition relics into real emerald proceeds.

This is **not** equivalent to original v0.2 completion. `COMPLETION_GAP_AUDIT.md` remains authoritative for unfinished breadth.

## 16. Next priorities after Alpha.32

Unless real-play regression overrides them:

1. finish documentation/CI consistency for Alpha.32;
2. assemble and launch the full `COMPANION_LOCK.json` stack in a fresh 26.2 NeoForge world before declaring external runtime compatibility;
3. continue missing building breadth without new controls: workshop, cart station, watchtower are strong next candidates;
4. connect external weapons/loot to workshop/advanced crafting rather than merely counting them;
5. add small road stairs/bridges and stronger steep-route handling;
6. add river/coast trade/fishing and military outpost specializations;
7. design coarse unloaded simulation without breaking physical item authority;
8. close UI status/progress/notification gaps;
9. perform full survival + multiplayer acceptance pass across founding -> settlement -> roads/outposts -> external dungeon loot -> market/trade -> higher tiers.

Real-play observations override assumptions. Fix root causes before adding more breadth when testing exposes a regression.
