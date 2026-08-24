# Frontier Settlement — Canonical Plan

This file is the repository-side implementation authority for Frontier Settlement. Read it together with `ORIGINAL_DESIGN_v0.2.md`, current `main` source, `COMPLETION_GAP_AUDIT.md`, companion lock/register, README and CI results before continuing development.

`ORIGINAL_DESIGN_v0.2.md` is the scope foundation/ceiling. This file may make that design more concrete, but it must not silently shrink unfinished original requirements to match whatever is currently implemented.

## 1. Product identity

Frontier Settlement is a Minecraft Java cooperative survival settlement / territory-growth mod.

Core loop:

`survival -> settlement growth -> better exploration -> external exploration / conquest -> NPCs / resources / technology -> settlement growth`

Player role progression:

`survivor -> pioneer -> village leader / lord-like role -> domain operator`

Settlement scale progression:

`pioneer camp -> hamlet -> village -> frontier town -> domain`

The settlement becomes deeper while direct management stays simple. Minecraft exploration, gathering, combat and choosing where the settlement expands remain foreground play.

Hard identity rules:

- one shared settlement per world/server;
- server authoritative;
- real Minecraft ItemStacks are resource authority; HUD/cache/context are not authority;
- player-made buildings are never scanned into functional Frontier buildings;
- repeated hauling/production/job assignment is automated;
- loaded areas should visibly move/work;
- do not force-load chunks merely to keep simulation running;
- do not silently destroy player containers, fluids, valuable blocks or unrelated builds;
- companion mods supply worldgen/dungeon/combat/weapon/loot breadth while Frontier remains settlement/citizen/construction/logistics/territory/progression glue.

## 2. Interaction budget and controls

Hard rule: many backend systems are allowed; direct micromanagement stays small.

Primary direct interactions remain approximately:

1. put/take physical items from shared settlement storage;
2. choose a building and its position/rotation;
3. choose an outpost location;
4. choose road start/end and only necessary route guidance;
5. explore/fight and decide which rare/external loot to commit to progression, trade or crafting.

Normal gameplay controls are fixed:

- `B`: settlement palette;
- `R`: rotate current building placement;
- `Enter`: confirm the active building/road/outpost placement;
- `Backspace`: reset/cancel the road start selection step.

Avoid E/Q/F/number/Shift/Ctrl/Space/chat/camera conflicts. Do not proliferate N/J/K or one new key per feature.

Intent is expressed physically wherever possible:

- market sale: eligible relic in market barrel;
- normal workshop repair: eligible damaged external weapon in service barrel;
- advanced forging: eligible external weapon + expedition relic in advanced commission barrel;
- construction office, cart station, watchtower and barracks operate without separate management dashboards;
- Alpha.40 coast/river fishing is inferred from loaded world geography and needs no specialization menu;
- Alpha.41 dangerous-region military role is inferred from loaded threat/environment evidence and needs no troop-management menu;
- Alpha.42 unloaded catch-up is bounded bookkeeping of elapsed work time only;
- Alpha.43 status context surfaces through compact HUD/notifications/Jade instead of a new management screen;
- Alpha.44 medium terrain work stays inside ordinary placement/construction;
- Alpha.45 structure discovery/conquest milestones are observed automatically from normal exploration/combat;
- Alpha.46 waterfront works are inferred from an already-qualifying fishing outpost and are built/operated without a new key or waterfront management panel.

Do not grow the project into tax rates, dozens of happiness stats, family schedules, per-worker priority tables, giant research menus or manual hauling routes.

## 3. Multiplayer authority

One world/server has one shared settlement and territory state.

- settlement resources/buildings/population/roads/outposts/progression are shared;
- server is authoritative;
- clients render state and submit bounded requests;
- presentation snapshots/notifications/Jade context are not gameplay authority and cannot spend resources or mutate progression;
- Alpha.45 exploration records are shared non-spendable progression metadata, not per-player currency;
- no independent per-player settlements in planned scope;
- no internal player politics/tax-distribution layer in initial scope.

## 4. Founding and early loop

Intended start:

- player places/uses the pioneer marker at a chosen overworld site;
- one shared settlement is founded;
- a small authoritative physical stockpile is established;
- one dedicated builder exists from the start;
- players manually secure first resources;
- builder performs physical site preparation and construction.

The saved starter stockpile position is authoritative and must not be casually destroyed into a progression softlock.

## 5. Buildings and physical construction

Functional settlement buildings use Frontier blueprints. Player/vanilla buildings remain welcome visually but are not scanned or registered as functional buildings.

Original target remains roughly **15–20 meaningful building families**. Alpha.39 reached **15 functional families**; Alpha.40–46 keep that number while deepening territory, simulation, terrain, status, exploration and waterfront systems. Do not add meaningless 16th–20th buildings merely to inflate the count.

Current families are exactly:

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

Town center currently exists through civic-core progression rather than a separate placement family.

Construction UX:

`palette -> completed ghost preview -> position/rotation -> terrain/overlap/cost validation -> project transaction -> physical grading -> hauling -> phased construction -> completion`

Construction intention:

`site clearing -> hauling -> foundation -> frame -> walls -> roof -> interior -> completion`

Terrain rules after Alpha.44:

- footprint height span 0–2 uses the established small visible grading path;
- span 3–4 is bounded medium terrain and placement feedback explicitly says `지형 공사 포함`;
- span above 4 or unsafe block-entity/fluid/unsupported terrain is rejected rather than flattening a mountain;
- medium cut is natural-ground only and bounded to at most 3 blocks relative to project grade;
- support/fill remains bounded to 3-block support depth;
- exposed outer-edge support depth >=2 gets visible cobblestone retaining/foundation treatment;
- retaining cobblestone is never free: real settlement stone is physically hauled/staged/consumed before each retaining cell;
- additional retaining stone is bounded to 96/project and included in approval/start resource checks;
- no loose-drop terrain farming from project approval;
- project grading must not create recoverable free economic materials;
- roof-before-support and visually floating foundations are invalid;
- enclosed completed buildings need deliberate windows/lighting;
- arbitrary selected-area cut/fill, tunnels, large ravine civil works and monumental terraforming remain unfinished.

Construction presentation invariant: **builder walks from actual settlement storage carrying real wood/stone stacks** and visibly stages/uses them at the site.

Tall structures reuse persisted scaffold/building workflow; no instant-placement exception.

### Alpha.38 construction-office rule

The construction office is a physical logistics accelerator, not an abstract build-speed buff and not a second builder authority.

- unlock: village tier + one warehouse;
- 13×9, wood112, stone64;
- four protected material barrels in the normal physical settlement ItemStack ledger;
- construction wood/stone extraction prefers office bays before ordinary storage;
- one persistent office-assigned supply runner works only while a building project is active;
- runner physically walks to loaded ordinary storage, extracts real wood/stone, carries max32 and returns before insertion;
- target staging reserve is wood96 + stone96;
- source radius is bounded to24 and corridor samples must already be loaded;
- no force-load, teleport inventory, virtual construction points or duplicate construction tick;
- `SettlementConstructionService` remains the only authority for grading and blueprint placement.

### Alpha.44 bounded medium-terrain rule

- the ordinary `SettlementConstructionService` remains the single terrain/building authority;
- medium terrain is a project property, not a second terraforming simulation;
- no full project cost is silently deleted at approval;
- retaining stone follows the physical storage -> carried stack -> site barrel -> consume path;
- unsafe terrain stalls/rejects instead of using `destroyBlock`, free drops, force-load or teleport;
- deep project foundations are protected during active construction;
- selected-area terraforming remains explicitly unfinished.

## 6. Citizens, jobs and defense roles

Vanilla villager trading/professions are not settlement-progression authority.

Implemented role families include builder, logger, farmer, quarry worker, miner, coast/river fishing worker, waterfront trader, normal workshop artisan, construction supply runner, advanced forging specialist, guards/service behavior, dangerous-region outpost sentry, market merchant presentation and road-bound transport.

Defense role separation:

- guard post: close routine local defense;
- watchtower: loaded-area longer-range threat observation/response;
- barracks: formal supplied multi-slot town garrison;
- dangerous-region military outpost: one supplied remote sentry that secures a loaded risky foothold and stands down when danger evidence disappears.

Alpha.37 barracks rules:

- frontier-town + watchtower + blacksmith prerequisite;
- 3 persistent military slots per barracks;
- each refill costs real food8 + metal2;
- military capacity is separate from civilian population/housing;
- soldier assignment uses barracks+slot tags, not UUID ordering;
- unloaded barracks are not interpreted as dead soldiers;
- forced creeper pursuit is excluded;
- tagged Iron Golem combat proxies drop no iron/resources;
- old free high-tier guard-post reinforcement backend stays removed.

Alpha.41 remote military rules:

- only otherwise-general outposts may dynamically gain the military role;
- bounded area must already be loaded before danger/missing-sentry state is trusted;
- danger combines total Monster pressure, close pressure, hostile-type diversity and enclosed low-light samples;
- one sentry maximum per qualifying outpost;
- local replacement cost food6 + metal2 from the physical outpost stockpile;
- target reserve food12 + metal4;
- tagged sentry drops no iron/resources;
- danger loss clears forced target and returns the sentry home rather than despawning it;
- external hostiles using the normal `Monster` hierarchy are soft-compatible without hard companion code.

Current regular soldier/sentry bodies are Iron Golem proxies. Humanoid soldier visuals, external weapon loadouts, formations and class variety remain later presentation/combat-depth work.

Construction-office and advanced-forging specialists are building-bound service NPCs and do not inflate civilian population/housing. Fishing workers, waterfront traders and military-outpost sentries follow the same service-unit convention. Future citizen-assignment cleanup must not double-count population or create free tier progression.

No family/children simulation in planned scope.

## 7. Resources and physical logistics

Resources remain physical Minecraft items. HUD numbers are a cached view, not authority.

- workers deposit real ItemStacks;
- construction consumes real ItemStacks;
- avoid every-tick scanning of arbitrary player chests;
- use tags/categories so compatible external materials can participate;
- warehouses, cart-station freight bays and construction-office bays add physical storage positions rather than abstract capacity currency;
- service/commission/trade barrels are not generic shared storage unless their system explicitly says so;
- Alpha.45 exploration score is not a resource and can never satisfy an ItemStack cost.

Distant logistics remains spatial. Transport workers belong to a specific outpost, follow persisted road-network waypoints, carry actual cargo and pause at unloaded route boundaries rather than teleporting or force-loading.

Alpha.27 tagged road logistics remains the **single authority for outpost transport** at every tier. Do not reintroduce generic-name/UUID pairing or a second transport navigation/economy controller.

### Alpha.34 cart-station rule

- village tier + road-connected outpost;
- station within12 blocks of an existing road;
- four physical freight barrels join settlement storage;
- existing outpost transporter still owns the route;
- incoming cargo prefers station bays;
- per-trip outpost cargo rises16→32;
- full station falls back to another valid storage target;
- no teleport cargo or new route authority.

A future wagon entity may be presentation/vehicle behavior only and must not become duplicate economy simulation.

### Alpha.40 fishing-trade cargo rule

- only an otherwise-general outpost may gain the dynamic fishing overlay;
- qualification uses already-loaded local world state: radius12, at least24 open surface-water columns and a safe dry bank;
- fishing produces ordinary cod/salmon ItemStacks into the existing outpost stockpile;
- because fish are food items, Alpha.27 logistics transports them without a new route controller;
- at Alpha.40, `수변교역` meant catch participation in the physical outpost→road→town economy;
- no forced chunks or abstract fish/trade currency.

### Alpha.41 military reverse-supply rule

- dangerous-region military role never gets a separate long-distance transporter;
- the existing outpost-assigned worker remains the route/navigation authority;
- while military role is active, that worker can return empty to town, physically extract food/metal and carry it back along the same road;
- physical outpost stockpile is authority for sentry recruitment;
- military supply trip state is cleared when danger stops qualifying;
- no teleport cargo, abstract supply points, forced chunks or offline replenishment.

### Alpha.42 bounded unloaded-work rule

`SettlementDeferredOutpostData` stores bounded elapsed work-time debt, never items or cargo.

- sampling occurs only during normal server-running work time;
- production/logistics debt each cap at24,000 ticks/outpost;
- persisted lumber/quarry/mining/agriculture may bank production time while unloaded;
- a general outpost may bank fishing time only from its last loaded verified fishing overlay;
- deferred production only makes later real loaded work due sooner and is consumed only after real physical output succeeds;
- logistics debt is never cargo; each1,200 ticks may raise the next actual pickup to at most2× normal, absolute64;
- logistics credit is consumed only if actual extraction exceeds normal batch;
- the same physical worker still carries the ItemStack along the persisted road;
- no server-off real-world downtime catch-up, virtual stockpile, virtual wagon, virtual resources or second transport controller.

### Alpha.46 waterfront reverse-supply and trade rule

Alpha.46 is a physical/local extension of Alpha.40, not a water logistics replacement.

- only a qualifying loaded fishing shoreline may gain a waterfront landing;
- persisted `WaterfrontState`/`SettlementWaterfrontData` stores only anchor/direction/build-step metadata and is not a resource ledger;
- the fishing worker builds the bounded landing one block at a time using **real wood from the outpost stockpile**;
- if the worker already carries fish, that cargo is physically returned to the stockpile before construction pauses ordinary fishing;
- if local construction wood is short, the **same existing Alpha.27 transporter** may reverse-supply real wood from loaded town storage;
- military food/metal reverse supply has precedence over waterfront wood reverse supply;
- the landing uses spruce slabs, three fence posts and one dedicated trade barrel while leaving the water in place;
- completed waterfront blocks are protected from normal breaking so consumed construction wood is not converted into a free-resource loop;
- the local persistent `수변 상인 #ID` is presentation/operation for the dedicated barrel, not a second long-distance merchant/logistics authority;
- only cod/salmon deliberately put in that dedicated barrel are sold;
- ordinary outpost stockpile is never auto-sold;
- trade recipe is **16 real cod/salmon -> 1 real emerald**, inserted into the same barrel and stalled if output has no room;
- no boat logistics, teleport inventory, force-load, virtual trade points or remote abstract emerald balance.

## 8. Roads, outposts and territory

Roads/outposts are the spatial-growth layer. Progression must not become endlessly enlarging one flat central base.

Road intent:

`choose start -> choose route/end -> preview -> approve -> physical grading/build`

Alpha.35 bounded terrain adaptation:

- one-block longitudinal rise becomes cobblestone stair road pieces;
- short water runs between dry banks may become a 3-wide stone-brick deck;
- max automatic water span6 centerline blocks;
- bank height difference max1;
- water stays in place;
- stairs/bridges add real stone cost;
- completed logistics path remains ordinary `RoadSegment` centerline.

Alpha.44 retaining work currently applies to functional building sites, not arbitrary road cliff engineering. Large ravine bridges, tunnels and monumental civil engineering remain later breadth.

No early teleport network; roads/logistics must retain meaning.

Implemented outpost specializations/overlays:

- lumber;
- quarry;
- mining;
- agriculture;
- Alpha.40 coast/river fishing-trade overlay for qualifying general outposts;
- Alpha.41 dangerous-region military overlay for qualifying general outposts;
- Alpha.42 bounded unloaded-work catch-up over established specialization/route evidence;
- Alpha.46 persisted physical waterfront landing + dedicated local fish trade for qualifying fishing outposts.

Still required/partial by original scope:

- better biome-aware specialization with companion worldgen;
- moving boats/waterborne merchant motion may be added only as presentation/value breadth and must not replace road logistics;
- Alpha.42 catch-up still needs real-play pacing/reload/exploit acceptance.

**tier-visible public works** may make established territory more readable, but only in loaded safe locations and without overwriting player work or generating farmable free blocks.

## 9. Combat and exploration

This is not a constant wave-defense game.

- guard posts handle routine local threats;
- watchtowers extend loaded-area observation/response;
- barracks provide supplied formal town garrison;
- dangerous-region outposts provide one supplied remote foothold sentry only while loaded world evidence justifies the role;
- occasional meaningful threats are allowed;
- do not spam mandatory waves.

Exploration content should include ruins, mines, camps, nests, structures, dungeons, caravans, rare resources, bosses and rare NPCs primarily through the external content stack where strong implementations already exist.

Frontier connects exploration outcomes back into settlement growth instead of reimplementing all adventure content.

### Alpha.45 exploration/conquest progression rule

- once every100 server ticks, only online players' current already-loaded positions are checked;
- Frontier queries the normal dynamic structure registry/StructureManager and never `/locate`s distant content or generates chunks;
- external structure namespaces are accepted generically except `minecraft`, `frontier_settlement`, `neoforge`;
- progress is by unique structure type, not generated instance;
- conquest requires direct player attribution;
- Ender Dragon and Wither count explicitly;
- external `Mob` types with max health>=80 may count as soft-compatible strong-enemy/boss milestones;
- each conquest entity type counts once;
- shared SavedData bounds discovered structure types to64 and conquest types to32;
- score is `min(8, unique structures + unique conquest types*3)`;
- score is non-spendable metadata, never ItemStack/resource authority;
- frontier-town alternate route: population7 +2 outposts + mine + quarry + score2;
- legacy frontier-town route remains population8 +2 outposts + mine + quarry;
- domain alternate route: population14 +3 outposts + mine +2 farms + score5;
- legacy domain route remains population16 +4 outposts + mine +2 farms;
- no loot generation, resource minting, companion mutation, teleport, chunk force-load or global radar.

Future exploration work may deepen rare-resource/NPC/boss-specific rewards, but Frontier must remain progression glue and companions must remain adventure-content authority.

## 10. External content stack

The candidate 26.2 stack is locked in `COMPANION_LOCK.json`:

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

- Frontier should boot without companions unless a future hard dependency is explicitly justified;
- do not redundantly rebuild world/dungeon/combat/weapon systems supplied by companions;
- common/additive tags are preferred over hard-coded companion classes;
- optional companion API code must stay isolated;
- Alpha.45 soft registry observation must not generate or scan remote companion structures;
- reuse/license boundaries stay recorded in `EXTERNAL_CONTENT_REGISTER.md`;
- the lock remains `candidate_runtime_lock` until all entries launch together in the target client/server environment.

Jade:

- exact candidate artifact 26.2.2 / version ID `HLYMycSr` is `compileOnly`;
- all `snownee.jade` references stay quarantined under `compat/jade`;
- Jade absence must not affect core resources/jobs/construction/logistics/progression.

Xaero:

- Alpha.43 only shifts the Frontier HUD below the expected top-left minimap area;
- exact locked Xaero's Minimap 26.4.2 compile investigation proved the historical public `WaypointsManager` API is absent;
- do not fake completion by using version-internal waypoint sets, reflection or mixin injection;
- settlement/outpost/road marker synchronization remains deferred until a stable supported seam exists.

## 11. Implemented bridges by milestone

### Alpha.31–39

- Alpha.31: additive material/relic tags and external weapon recognition;
- Alpha.32: protected physical relic market, real emerald output, no shared-storage auto-sale;
- Alpha.33: staffed external-weapon repair with real metal;
- Alpha.34: road-adjacent cart station, four freight barrels, batch16→32, route authority unchanged;
- Alpha.35: road stairs and bounded short stone bridges with real stone;
- Alpha.36: watchtower with loaded response guard;
- Alpha.37: supplied 3-slot barracks garrison;
- Alpha.38: construction office with physical staging runner;
- Alpha.39: advanced workshop commission, relic1 + metal4 + external weapon -> validated power30 compatible forge.

Role split remains fixed:

`market = relic -> trade value`

`normal workshop = metal -> external weapon repair`

`advanced workshop = external weapon + relic + metal -> high-tier forge`

### Alpha.40–46

- Alpha.40: loaded shoreline fishing overlay, real cod/salmon to physical outpost stockpile and existing road economy;
- Alpha.41: loaded dangerous-region military overlay, one supplied sentry and reverse food/metal on the same transporter;
- Alpha.42: bounded unloaded work-time debt only, no virtual resources/cargo;
- Alpha.43: compact project/status UX + optional Jade context + Xaero HUD collision avoidance;
- Alpha.44: bounded span3–4 medium terrain with real retaining stone;
- Alpha.45: unique external structure/conquest milestones feeding alternate tier-growth routes;
- Alpha.46: persisted real-wood waterfront landing, same-transporter wood reverse supply, dedicated opt-in 16 fish -> 1 emerald local trade and waterfront Jade/status context.

## 12. UI and information hierarchy

Reference hierarchy remains:

1. Against the Storm — compact resource/status hierarchy;
2. Manor Lords — world-space building/road placement;
3. MineColonies — Minecraft-native blueprint/material/construction presentation;
4. Frostpunk 2 — secondary territory-overview concepts.

Do not invent giant generic rectangle dashboards when a proven interaction reference exists.

Current hierarchy:

- always-on: resource/tier/next-goal HUD;
- active project: one compact project/progress line;
- important transitions: bounded right-side notice queue, max3 / 6 seconds;
- Jade present: crosshair-local infrastructure title + one detail/progress line;
- Xaero present: Frontier HUD moves below minimap region;
- exploration milestones: rare event messages + `/frontier status`, not a quest dashboard;
- Alpha.46 waterfront trade: physical barrel + Jade/status text, not a separate trade screen.

Still partial: richer material/progress detail for some service states and true Xaero map synchronization.

## 13. Engineering rules

Target:

- Minecraft Java 26.2;
- NeoForge 26.2.0.38-beta;
- Java 25;
- Gradle 9.2.1.

Development sequence:

`read current GitHub main -> inspect ORIGINAL_DESIGN + CANONICAL_PLAN + gap audit + source + CI -> implement -> manual code/gameplay audit -> Java25 clean build -> JAR verify -> direct main update -> deliver test JAR when useful`

Shared repository rules:

- re-read remote `main` immediately before writes;
- never force-push over concurrent work;
- Frontier path changes only except its canonical workflow/result files;
- CI result bot may advance main;
- final accepted result must point at the intended Frontier source/docs SHA.

Optional companion rule:

- missing companion must not turn Frontier core into a boot failure;
- use documented/stable APIs where available;
- isolate optional API references under compat boundaries;
- if no stable API exists, prefer honest partial integration over brittle reflection/mixins that risk core boot failure.

## 14. Current playable slice after Alpha.46

The playable slice now includes:

- one shared authoritative settlement;
- protected founding stockpile/civic core;
- compact B/R/Enter/Backspace interaction;
- exactly 15 functional building families;
- physical site grading/construction hauling;
- Alpha.44 bounded medium terrain with real retaining stone;
- construction material staging/supply runner;
- paced loaded town production;
- physical roads with one-block stairs and bounded short-water bridges;
- specialized outposts plus Alpha.40 fishing and Alpha.41 military overlays;
- Alpha.46 real-wood waterfront landing and dedicated opt-in fish trade;
- persisted road-bound transport with physical reverse military and waterfront supply while preserving one long-distance authority;
- Alpha.42 bounded unloaded-work catch-up with no virtual resources/cargo;
- Alpha.43 project HUD/notices/Jade context;
- Xaero-aware HUD collision avoidance without marker claim;
- Alpha.45 unique external-structure/conquest milestones feeding alternate shared tier routes;
- tier growth and safe public works;
- external material/relic/weapon recognition;
- physical market, staffed repair workshop and explicit rare-material advanced forging;
- cart-station freight hub;
- climbable watchtower response;
- supplied barracks regular garrison.

This is **not** original v0.2 completion. `COMPLETION_GAP_AUDIT.md` remains authoritative for unfinished breadth.

## 15. Unfinished original-scope priorities

Unless real-play regression overrides them:

1. broader high-tier recipe/specialized crafting only where exploration materials justify it;
2. humanoid/weaponized soldier presentation where it improves the Better Combat / Weapons Expanded stack;
3. selected-area cut/fill and larger civil engineering with strict player-build/resource-exploit protection;
4. deeper companion exploration rewards/rare NPC or boss-specific bridges only when soft, non-farmable and meaningful;
5. better biome-aware outpost specialization with companion terrain where a stable data seam exists;
6. long survival + two-player multiplayer acceptance;
7. Alpha.42 catch-up pacing/save-reload/exploit acceptance;
8. Alpha.43 Jade/Xaero/HUD visual/runtime acceptance;
9. Alpha.46 waterfront pathing/site quality/reverse-supply/trade-balance acceptance;
10. full `COMPANION_LOCK.json` fresh-world client/server runtime at the final chosen test point;
11. true Xaero settlement/outpost markers only if a stable supported API/seam appears;
12. moving boat/waterborne merchant behavior only if it adds visible value while remaining presentation/local behavior, never a second logistics authority.

## 16. Real-play acceptance focus

Automated CI is not a substitute for Minecraft play. At the planned final/test-worthy point verify:

- founding -> early core buildings;
- building small grading and a real span3–4 Alpha.44 site;
- >4/unsafe-site rejection and no recoverable free material;
- building hauling and save/reload mid-project;
- construction-office runner staging/builder preference;
- road stairs/short bridge navigation;
- road -> outpost -> production -> transporter -> cart station;
- Xaero installed/absent HUD readability;
- Jade installed/absent boot and compact context;
- project/tier/build/outpost notices once, side-only, max3, expiry;
- actual companion external structure discovery, same-type dedupe and no remote scan;
- dragon/wither/external strong-enemy conquest attribution and dedupe;
- exploration score persistence/cap8/non-spendability and legacy tier routes;
- Alpha.40 shoreline qualification, fishing movement/catch and invalid-puddle rejection;
- Alpha.46 qualifying shoreline builds one landing from real wood, save/reload preserves build step, blocked/unsafe shoreline does not overwrite terrain;
- fishing worker returns pre-existing fish cargo before construction and does not deadlock;
- local wood shortage makes the same road transporter bring town wood physically, while active military food/metal supply remains higher priority;
- completed waterfront blocks cannot be broken into a free construction-material loop;
- only the dedicated trade barrel sells fish; normal outpost stock never auto-sells;
- exactly 16 real cod/salmon become one real emerald only when barrel output has room;
- waterfront trader does not duplicate across reload and Jade/status text matches state;
- deferred production/logistics debt cap24,000, real-resource gating, max64 catch-up pickup and no server-off downtime catch-up;
- dangerous-region military activation, one sentry, no iron drop, reverse supply and stand-down;
- market vs workshop vs advanced-forge intent separation;
- advanced-workshop compatibility/no-loss failure;
- watchtower/barracks replacement costs;
- two-player shared state/storage/construction/logistics/exploration/waterfront behavior;
- full companion-stack fresh world.

Real-play observations override assumptions. Fix root causes before adding more breadth when testing exposes a regression.
