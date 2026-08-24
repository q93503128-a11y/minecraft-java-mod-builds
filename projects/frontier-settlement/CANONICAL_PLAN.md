# Frontier Settlement — Canonical Plan

This file is the repository-side implementation authority for Frontier Settlement. Read it together with `ORIGINAL_DESIGN_v0.2.md`, current `main` source, `COMPLETION_GAP_AUDIT.md`, `COMPANION_LOCK.json`, `EXTERNAL_CONTENT_REGISTER.md`, README and current CI result before every continuation.

`ORIGINAL_DESIGN_v0.2.md` is the scope foundation/ceiling. This file may make that design more concrete, but it must never silently shrink unfinished original requirements to match the current code.

Current canonical implementation: **0.1.0-alpha.49**.

## 1. Product identity

Frontier Settlement is a Minecraft Java cooperative survival settlement / territory-growth mod.

Core loop:

`survival -> settlement growth -> better exploration -> external exploration / conquest -> NPCs / resources / technology -> settlement growth`

Player role:

`survivor -> pioneer -> village leader / lord-like role -> domain operator`

Settlement scale:

`pioneer camp -> hamlet -> village -> frontier town -> domain`

The settlement becomes deeper while direct management stays simple. Exploration, gathering, combat and choosing where the settlement expands remain foreground Minecraft play.

Hard identity rules:

- one shared settlement per world/server;
- server authoritative;
- real Minecraft ItemStacks remain resource authority; HUD/cache/context/progression score/project-local earth are not resource authority;
- player-made buildings are never scanned into Frontier functional buildings;
- repeated hauling/production/job assignment is automated;
- loaded areas visibly move/work;
- do not force-load chunks merely to continue simulation;
- do not silently destroy player containers, fluids, valuable blocks or unrelated builds;
- companion mods supply terrain/dungeon/combat/weapon/loot breadth while Frontier remains settlement/citizen/construction/logistics/territory/progression glue.

## 2. Interaction budget and controls

The original v0.2 rule remains: backend depth may grow, but repeated direct management must not.

Primary interactions:

1. put/take physical items from shared settlement storage;
2. choose a functional building and its position/rotation;
3. choose an outpost location;
4. choose road endpoints/necessary route intent;
5. explore/fight and decide which rare physical loot to commit to trade/crafting/progression;
6. late-game civil work may reuse the construction palette, but must not become a separate management game.

Fixed controls:

- `B`: settlement/infrastructure palette;
- `R`: rotate ordinary building placement;
- `Enter`: confirm the current building/road/outpost/civil-work step;
- `Backspace`: reset road start or Alpha.49 civil-work first corner.

Avoid E/Q/F/number/Shift/Ctrl/Space/chat/camera conflicts. Do not proliferate N/J/K or one new key per feature.

Physical intent remains preferred over menus:

- market: relic deliberately placed in market barrel;
- workshop: damaged external weapon deliberately placed in service barrel;
- advanced forge/reforge: weapon + relic deliberately placed in commission barrel, real metal fetched by specialist;
- waterfront trade: fish deliberately placed in the dedicated trade barrel;
- coast/river and dangerous-region roles are inferred from loaded world conditions;
- Alpha.49 civil work is `B palette -> first corner -> second corner -> server approval`, not a terraforming dashboard.

Do not add tax rates, dozens of happiness stats, family schedules, per-worker priority tables, manual hauling routes or giant research screens.

## 3. Multiplayer authority

One world/server has one shared settlement and one shared infrastructure/project state.

- resources/buildings/population/roads/outposts/progression/civil work are shared;
- clients submit bounded requests and render synchronized state;
- the server revalidates building/road/outpost/civil-work requests before mutation;
- only one building/road/outpost/civil construction project may occupy the shared construction authority at once;
- Alpha.45 exploration metadata is shared, non-spendable and deduplicated;
- Alpha.49 `earthBank` is project-local relocation accounting only and cannot be spent outside the active civil project;
- no per-player settlement or internal politics/tax layer in planned scope.

## 4. Founding and growth

Founding establishes:

- player-selected pioneer marker site;
- authoritative physical shared stockpile;
- one shared dedicated construction worker;
- settlement center/territory state;
- starter manual resource loop.

Growth tiers remain:

- pioneer camp;
- hamlet;
- village;
- frontier town;
- domain.

Alpha.45 adds alternate exploration-accelerated tier routes but does not invalidate legacy routes.

Legacy frontier-town route:
- population >=8;
- outposts >=2;
- mine >=1;
- quarry >=1.

Exploration frontier-town route:
- population >=7;
- outposts >=2;
- mine >=1;
- quarry >=1;
- explorationScore >=2.

Legacy domain route:
- population >=16;
- outposts >=4;
- mine >=1;
- farms >=2.

Exploration domain route:
- population >=14;
- outposts >=3;
- mine >=1;
- farms >=2;
- explorationScore >=5.

## 5. Functional buildings and ordinary construction

Functional buildings use Frontier blueprints. Player/vanilla buildings may coexist visually but are not registered as Frontier functional buildings.

Current functional families are exactly **15**:

1. house;
2. lumber camp;
3. farm;
4. quarry;
5. mine;
6. warehouse;
7. construction office;
8. blacksmith;
9. workshop;
10. advanced workshop;
11. guard post;
12. watchtower;
13. barracks;
14. market;
15. cart station.

The original target was roughly 15–20 meaningful families. Alpha.40–49 deepen systems rather than adding fake 16th–20th families.

Ordinary construction:

`palette -> completed ghost -> position/rotation -> terrain/overlap/cost validation -> project transaction -> physical grading -> hauling -> phased construction -> completion`

Physical sequence intention:

`site clearing -> hauling -> foundation -> frame -> walls -> roof -> interior -> completion`

Construction presentation invariant: **builder walks from actual settlement storage carrying real wood/stone stacks** and visibly stages/uses them at the site.

### Alpha.38 construction office

- unlock village + warehouse;
- four physical material bays join the same ItemStack ledger;
- construction extraction prefers staged office bays;
- one office supply runner works only while a building project is active;
- runner walks to loaded ordinary storage, extracts real wood/stone, carries bounded stacks and returns before insertion;
- no abstract build-speed points, second building authority, teleport inventory or force-load.

### Alpha.44 bounded building terrain

- footprint height span0–2: established small grading;
- span3–4: accepted with `지형 공사 포함`;
- span>4/unsafe block entities/fluids/unsupported terrain: reject;
- natural cut max3 relative to grade;
- support/fill depth max3;
- deep exposed edge requires cobblestone retaining/foundation treatment;
- retaining stone is physically hauled/staged/consumed, max extra96/project;
- no free cobblestone, loose drops, `destroyBlock`, force-load or teleport.

`SettlementConstructionService` remains the ordinary building terrain/build authority.

## 6. Alpha.49 selected-area civil works

Alpha.49 implements the original v0.2 late-game `선택 영역 평탄화/절토/성토` as a bounded first pass without turning Frontier into WorldEdit.

Unlock and interaction:

- shared settlement must be `DOMAIN`;
- at least one construction office must be completed;
- no new functional BuildingType;
- no new key/dashboard;
- choose `토목 평탄화` from existing B palette;
- first corner fixes final grade Y;
- second corner defines X/Z bounds and requests authoritative validation;
- `Backspace` resets first corner.

Bounds:

- maximum 9×9 footprint;
- max cut4 blocks/column;
- max fill4 blocks/column;
- selected corners within28 blocks of player;
- project center within80 blocks of settlement center;
- entire selected area already loaded.

Protection:

- reject overlap with authoritative stockpile, completed functional buildings, roads or outposts;
- reject block entities and fluids;
- reject ores, structures/player blocks and other non-natural terrain in the affected column;
- do not globally scan, force-load or generate chunks;
- active civil-work volume is protected from ordinary player block breaking.

Balanced-earth invariant:

- server computes initial cut and fill counts before approval;
- **fill volume must be <= cut volume** in Alpha.49;
- each successful real cut changes one natural world block to air with no item drops, then credits one project-local earth unit;
- each real coarse-dirt fill requires earthBank>0 and consumes one project-local earth unit;
- `earthBank` persists for save/reload correctness but is not a resource ledger, ItemStack, cargo, currency or transferable balance;
- leftover project-local earth disappears when the civil project completes; it never becomes free dirt/stone loot.

Worker/authority invariant:

- Alpha.49 reuses `SettlementConstructionService.ensureBuilder` and the existing shared `건설 주민`;
- no second builder entity or independent construction economy;
- building/road/outpost normal UI/network/command starts reject while civil work is active;
- compact project context exposes phase/progress/cut/fill/earthBank only as presentation.

Alpha.49 completion claim is intentionally narrow: bounded 9×9 balanced flatten/cut/fill is **implemented/partial**. Still unfinished are imported physical fill, retaining-heavy terraces, larger area projects, ravine-scale bridges, tunnels and monumental civil engineering. Mountain-scale deletion remains prohibited.

## 7. Citizens, production and military

Implemented/service roles include builder, logger, farmer, quarry worker, miner, fishing worker, waterfront trader, workshop artisan, advanced forging specialist, construction supply runner, guards, barracks soldiers, dangerous-region outpost sentry, market presentation and road-bound transporter.

Loaded areas should show actual movement/work. Family/children simulation is outside planned scope.

### Defense separation

- guard post: routine close defense;
- watchtower: loaded longer-range observation/response;
- barracks: formal supplied town garrison;
- dangerous-region outpost: one supplied remote foothold sentry while loaded evidence justifies the role.

Barracks invariant:

- frontier-town + watchtower + blacksmith;
- 3 persistent military slots/barracks;
- each refill costs real food8 + metal2;
- military capacity stays separate from civilian population/housing;
- creepers excluded from forced pursuit;
- military drops cleared.

Dangerous-outpost invariant:

- general outposts only;
- loaded Monster pressure + close pressure + threat diversity + enclosed darkness evidence;
- max one sentry/outpost;
- replacement local stockpile food6 + metal2;
- target reserve food12 + metal4;
- military role overrides fishing;
- danger loss causes stand-down/return rather than deletion.

### Alpha.48 humanoid military presentation

- `FrontierSoldierEntity` extends `IronGolem` and inherits the proven server AI/combat attributes;
- client renders a humanoid player-shaped soldier;
- **visual service sword is never a server ItemStack**;
- old loaded tagged Iron Golem barracks soldiers/sentries migrate **1:1**, preserving assignment/name/tags/health without recruitment consume;
- existing food/metal costs, military slots and drop protection remain unchanged;
- no hard Better Combat/Weapons Expanded Java dependency.

A physical external-weapon armory/loadout loop remains unfinished. If added, it must use actual ItemStacks and automation and must not require manually opening every soldier.

## 8. Resources and logistics

Resources remain physical Minecraft items.

- workers deposit real ItemStacks;
- construction/crafting/trade consumes real ItemStacks where those systems require resources;
- avoid every-tick arbitrary player chest scanning;
- common/additive tags let compatible external materials participate;
- service/commission/trade barrels are not generic storage unless explicitly specified;
- Alpha.45 exploration score and Alpha.49 earthBank are not spendable resources.

**Transport workers belong to a specific outpost**, follow persisted road-network waypoints, carry actual cargo and **pause at unloaded route boundaries** instead of teleporting or force-loading.

Alpha.27 tagged road logistics remains the **single authority for outpost transport** at every tier.

### Cart station

- road-adjacent physical freight hub;
- four freight barrels;
- same outpost transporter owns route;
- batch16→32;
- no second route/economy authority.

### Fishing/waterfront

- loaded shoreline fishing produces real cod/salmon into outpost stockpile;
- existing road logistics moves it;
- Alpha.46 physical landing is built from real local wood;
- local wood shortage may be reverse-supplied by same transporter;
- dedicated trade barrel consumes 16 real cod/salmon and outputs1 real emerald;
- ordinary outpost stock never auto-sells;
- no boat logistics/virtual trade balance.

### Military reverse supply

- same transporter can physically return to town and bring food/metal to active military outpost;
- military food/metal has precedence over waterfront wood reverse supply;
- no abstract supply points or duplicate transporter.

### Alpha.42 unloaded catch-up

- stores bounded elapsed work-time debt only;
- production/logistics each cap24,000 ticks/outpost;
- no resources/cargo generated while unloaded;
- credit is consumed only after later real loaded harvest/catch/extraction;
- logistics may raise one real pickup up to2× normal, absolute64;
- no server-off real-world catch-up.

## 9. Roads, outposts and territory

Roads/outposts are the spatial-growth layer. The central base should not simply expand forever.

Road interaction:

`choose start -> choose route/end -> preview -> approve -> physical grading/build`

Implemented:

- physical road construction;
- one-block longitudinal stair adaptation;
- bounded short-water stone bridge/deck max6 centerline blocks;
- persisted outpost construction;
- lumber/quarry/mining/agriculture specialization;
- loaded fishing overlay;
- loaded dangerous-region military overlay;
- bounded unloaded catch-up;
- Alpha.46 physical waterfront landing/trade.

**tier-visible public works** may improve territory readability only when safe, loaded, non-farmable and non-destructive to player work.

Still partial: deeper companion-biome specialization and larger road/civil engineering.

## 10. Exploration, crafting and settlement feedback

Frontier does not own the full adventure-content layer. External structures/dungeons/mobs/bosses/loot remain companion authority.

### Alpha.45 exploration/conquest progression

- every100 server ticks inspect only online players' current already-loaded positions;
- use dynamic structure registry/StructureManager;
- external namespaces except minecraft/frontier_settlement/neoforge;
- unique structure type counts once, not every instance;
- direct player Dragon/Wither kills count;
- external Mob max health>=80 may count as generic strong-enemy milestone;
- unique entity type counts once;
- discovered structures max64, conquest types max32;
- score=`min(8, structures + conquest*3)`;
- score is non-spendable metadata;
- no locate/global scan/chunk generation/loot minting.

### Market/workshop/advanced crafting role split

`market = expedition relic -> physical trade value`

`normal workshop = real metal -> damaged recognized external weapon repair`

`advanced forge = unenchanted external weapon + relic1 + metal4 -> validated power30 compatible forge`

`domain reforge = already-enchanted external weapon + relic2 + metal8 -> validated power40 compatible additions`

Alpha.47 reforge validates improvement and preservation before consuming resources. Existing enchantments may not be removed/downgraded. External weapon support remains soft/tag/type recognition, not a hard Weapons Expanded dependency.

## 11. External content stack

Candidate lock stays in `COMPANION_LOCK.json`:

- Terralith + Lithostitched;
- Dungeons and Taverns;
- Repurposed Structures;
- Better Combat + Cloth Config + Player Animation Library;
- Weapons Expanded;
- Lootr;
- Sophisticated Backpacks + Core;
- Jade;
- Xaero's Minimap.

Rules:

- Frontier must boot without optional companions unless a future hard dependency is explicitly justified;
- do not duplicate worldgen/dungeon/combat/weapon systems already supplied by companions;
- prefer generic registry/tag/data observation;
- keep optional API references isolated;
- lock remains `candidate_runtime_lock` until full fresh-world client/server launch succeeds.

Jade:

- exact candidate 26.2.2/HLYMycSr compileOnly;
- API references quarantined under compat/jade;
- absence must not affect core simulation.

Xaero:

- Frontier only shifts its HUD below expected minimap area;
- locked Xaero 26.4.2 lacks the historical public `WaypointsManager` API;
- do not fake completion through internal waypoint sets/reflection/mixins;
- true settlement/outpost/road marker synchronization remains deferred until a stable supported seam exists.

Alpha.49 civil work reads only already-loaded ordinary block state/heightmap information and adds no Terralith/worldgen hard dependency.

## 12. UI and information hierarchy

Reference hierarchy:

1. Against the Storm — compact resource/status hierarchy;
2. Manor Lords — world-space building/road placement;
3. MineColonies — Minecraft-native blueprint/material/construction presentation;
4. Frostpunk 2 — secondary territory-overview concepts.

Current hierarchy:

- always-on compact resource/tier/next-goal HUD;
- one compact active-project label/progress line;
- right-side notices max3/6 seconds;
- Jade crosshair-local infrastructure details when installed;
- Xaero-aware HUD offset;
- rare exploration messages + one `/frontier status` line, not quest dashboard;
- physical barrels encode market/workshop/waterfront intent;
- Alpha.49 grade-plane outline is world-space placement feedback, not a new screen.

Do not invent giant generic dashboards when a physical/world-space interaction can carry the same intent.

## 13. Engineering rules

Target:

- Minecraft Java26.2;
- NeoForge26.2.0.38-beta;
- Java25;
- Gradle9.2.1.

Development sequence:

`read current GitHub main -> inspect ORIGINAL_DESIGN + CANONICAL_PLAN + gap audit + source + CI -> implement -> manual code/gameplay audit -> Java25 clean build -> JAR verify -> direct main update -> canonical exact-SHA CI`

Shared repo:

- re-read remote main before writes;
- no force push;
- do not revert unrelated concurrent-project commits;
- Frontier path changes only except its workflow/result files;
- CI result bot may advance main;
- final accepted result must identify the exact intended Frontier source/docs SHA.

## 14. Current playable slice after Alpha.49

Current implemented slice includes:

- one shared authoritative settlement;
- founding stockpile/builder/civic progression;
- compact B/R/Enter/Backspace interaction;
- exactly15 functional buildings;
- physical construction/hauling and bounded Alpha.44 building terrain;
- construction-office staging;
- loaded production and resident work;
- physical roads/stairs/short bridges;
- specialized outposts, fishing, dangerous military overlay;
- same-authority physical road logistics and reverse supply;
- bounded unloaded work debt without virtual resources/cargo;
- waterfront real-wood landing and opt-in physical fish trade;
- compact HUD/notices/Jade context and Xaero collision avoidance;
- unique external structure/conquest progression bridge;
- market, repair, first advanced forge and DOMAIN reforge;
- supplied humanoid military presentation with unchanged physical recruitment economics;
- **Alpha.49 DOMAIN 9×9 balanced selected-area cut/fill** using the shared construction worker and project-local non-economic earth accounting.

This is not original v0.2 completion.

## 15. Unfinished original-scope priorities after Alpha.49

Unless real-play regression overrides them:

1. **larger civil-engineering second pass** — physical imported fill/retaining-heavy terraces and carefully bounded larger works without player-build/resource exploits;
2. deeper exploration bridges — rare NPC/structure/boss-specific settlement value only where soft, non-farmable and meaningful;
3. better companion-biome-aware outpost specialization where a stable data seam exists;
4. physical military armory/loadout only if it can stay automated and ItemStack-authoritative without per-soldier micromanagement;
5. long survival + two-player multiplayer acceptance;
6. Alpha.42 catch-up pacing/save-reload/exploit acceptance;
7. Alpha.43 Jade/Xaero/HUD visual/runtime acceptance;
8. Alpha.46 waterfront pathing/site/reverse-supply/trade-balance acceptance;
9. Alpha.48 humanoid render/attack-animation + legacy migration acceptance;
10. Alpha.49 civil-work pathing/save-reload/terrain-safety/earth-balance acceptance;
11. full companion lock fresh-world client/server runtime;
12. true Xaero markers only if a stable supported API appears;
13. moving boat/waterborne merchant only if presentation value justifies it and it never becomes a second logistics authority.

Large mountain deletion, unrestricted WorldEdit-style terraforming, family simulation, giant research trees, tax/economic micromanagement and manual per-soldier management remain outside the intended product.

## 16. Real-play acceptance focus

At the final/test-worthy point verify at least:

- two players see one shared stockpile/build/project/progression state;
- founding and early vertical slice;
- ordinary small and Alpha.44 span3–4 construction terrain;
- unsafe/>4 building terrain rejection and no free retaining materials;
- construction office runner and builder physical hauling;
- road stairs/short bridge and outpost route movement;
- cart station freight fallback;
- fishing shoreline qualification and invalid-puddle rejection;
- dangerous-region military activation/supply/stand-down;
- unloaded debt cap/reload/no-offline-mint;
- Jade installed/absent boot and compact context;
- Xaero installed/absent HUD readability with marker feature still honestly absent;
- external structure/conquest unique-type dedupe and legacy tier routes;
- waterfront real wood build, fish cargo return, same-transporter wood supply, military supply precedence, break protection and 16→1 dedicated trade;
- advanced forge and DOMAIN reforge no-loss compatibility;
- Alpha.48 humanoid render, attacks, drops and 1:1 legacy migration;
- Alpha.49 B-palette unlock requires DOMAIN + construction office at server authority;
- Alpha.49 first-corner grade, second-corner area, max9×9, ±4 bounds;
- Alpha.49 rejects fluids/block entities/ores/player structures/existing infrastructure;
- Alpha.49 fill>cut rejected;
- real cut creates no drops and only then credits earthBank;
- real fill requires/debits earthBank;
- save/reload preserves phase/progress/earthBank without duplication;
- unloaded area pauses instead of force-loading;
- shared builder visibly walks to civil cells;
- no building/road/outpost project starts concurrently;
- completed project leaves no spendable/transferable virtual earth;
- full companion-stack fresh world.

Real-play observations override assumptions. Fix root causes before adding more breadth when testing exposes a regression.
