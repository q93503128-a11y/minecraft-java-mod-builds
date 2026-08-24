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

## 2. Interaction budget

Hard rule: many backend systems are allowed; direct micromanagement stays small.

Primary direct interactions remain approximately:

1. put/take physical items from shared settlement storage;
2. choose a building and its position/rotation;
3. choose an outpost location;
4. choose road start/end and only necessary route guidance;
5. explore/fight and decide which rare/external loot to commit to settlement progression, trade or crafting.

Do not grow the project into tax rates, dozens of happiness stats, family schedules, per-worker priority tables, giant research menus or manual hauling routes.

Intent is expressed physically wherever possible:
- market sale: eligible relic in market barrel;
- normal workshop repair: eligible damaged external weapon in service barrel;
- advanced forging: eligible external weapon + expedition relic in advanced commission barrel;
- construction office, cart station, watchtower and barracks operate without separate management dashboards;
- Alpha.40 coast/river fishing is inferred from loaded world geography and needs no specialization menu;
- Alpha.41 dangerous-region military role is inferred from loaded threat/environment evidence and needs no specialization or troop-management menu;
- Alpha.42 unloaded catch-up is automatic bookkeeping of elapsed work time only and needs no player-facing management screen;
- Alpha.43 status context is derived automatically from authoritative settlement state and surfaces through compact HUD/notifications/Jade rather than a new management screen;
- Alpha.44 medium terrain work stays inside the ordinary placement/construction flow and adds no terraforming dashboard;
- Alpha.45 structure discovery/conquest milestones are observed automatically from normal exploration/combat and add no quest log, research tree or spendable point screen.

## 3. Multiplayer authority

One world/server has one shared settlement and territory state.

- settlement resources/buildings/population/roads/outposts/progression are shared;
- server is authoritative;
- clients render state and submit bounded requests;
- presentation snapshots/notifications are not gameplay authority and cannot spend resources or mutate progression;
- Alpha.45 exploration records are shared settlement progression metadata, not per-player currency;
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

The saved starter stockpile position is authoritative and must not be casually destroyed into a progression softlock.

## 5. Buildings and physical construction

Functional settlement buildings use official blueprints. Player/vanilla buildings remain welcome visually but are not scanned or registered as functional settlement buildings.

Original target remains roughly **15–20 meaningful building families**. Alpha.39 reached **15 functional families**; Alpha.40–45 keep that number while deepening territory, simulation, terrain, status and exploration systems. The number alone is not completion because original trade, selected-area civil engineering, exploration breadth and final presentation/runtime acceptance remain unfinished.

Current families:

- house;
- lumber camp;
- farm;
- quarry;
- mine;
- warehouse;
- construction office;
- blacksmith;
- workshop;
- advanced workshop;
- guard post;
- watchtower;
- barracks;
- market;
- cart station.

Town center currently exists through civic-core progression rather than a separate placement family. Do not add meaningless building families merely to inflate the count.

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

Terrain rules after Alpha.44:

- footprint height span0–2 uses the established small visible grading path;
- footprint height span3–4 is accepted only as bounded medium terrain and placement feedback explicitly says `지형 공사 포함`;
- span above4 or unsafe block-entity/fluid/unsupported terrain is rejected rather than silently flattening a mountain;
- medium cut is natural-ground only and bounded to at most3 blocks relative to project grade;
- support/fill remains bounded to the established3-block support depth;
- exposed outer-edge support depth>=2 gets visible cobblestone retaining/foundation treatment;
- retaining cobblestone is never free: real settlement stone is physically hauled/staged/consumed before each retaining cell;
- additional retaining stone is bounded to96/project and included in approval/start resource checks;
- no silent destruction of player containers, fluids, valuable blocks or unrelated infrastructure;
- no loose-drop terrain farming from project approval;
- project grading must not create recoverable free economic materials;
- roof before support and visually floating foundations are invalid;
- enclosed completed buildings need deliberate windows/lighting;
- arbitrary selected-area cut/fill, tunnels, large ravine civil works and monumental terraforming remain later breadth.

Construction presentation invariant: builder walks from actual settlement storage carrying real wood/stone stacks and visibly stages/uses them at the site.

Tall structures reuse persisted scaffold/building workflow; no instant-placement exception.

### Alpha.38 construction-office rule

The construction office is a physical logistics accelerator, not an abstract build-speed buff and not a second builder authority.

- unlock: village tier + one warehouse;
- footprint/cost: 13×9, wood112, stone64;
- four protected rotation-aware material barrels;
- those barrels participate in the normal physical settlement ItemStack ledger;
- wood/stone extraction orders office bays before ordinary storage so the existing builder naturally benefits from staged stock;
- automated non-wood/stone deposits avoid dedicated construction bays;
- one persistent office-assigned construction supply runner services each loaded office;
- runner works only with a building project active, physically walks to bounded loaded ordinary storage, extracts real wood/stone, carries max32 and physically returns before insertion;
- target staging reserve is96 wood +96 stone;
- source radius is bounded to24 and sampled corridor positions must already be loaded;
- night/no-project behavior returns the runner home;
- construction supply runners are service units, not civilian population/housing;
- no force-load, teleport inventory transfer, virtual construction points or duplicate construction tick;
- `SettlementConstructionService` remains the only authority for grading and blueprint placement.

Actual time saved depends on town layout and must be evaluated in real play rather than claimed as a fixed percentage.

### Alpha.44 bounded medium-terrain rule

- the ordinary `SettlementConstructionService` remains the single terrain/building authority;
- medium terrain is a project property, not a new independent terraforming simulation;
- no full project cost is silently deleted at approval;
- retaining stone follows the same physical storage/extraction/carry/site-barrel path as construction material;
- retaining material is consumed before the corresponding cobblestone support appears;
- unsafe terrain stalls/rejects instead of using `destroyBlock`, free drops, force-load or teleport;
- deep project foundations are protected during active construction;
- selected-area terraforming is explicitly still unfinished.

## 6. Citizens, jobs and defense roles

Vanilla villager trading/professions are not settlement-progression authority.

Implemented role families include builder, logger, farmer, quarry worker, miner, coast/river fishing worker, normal workshop artisan, construction supply runner, advanced forging specialist, guards/service behavior, dangerous-region outpost sentry, market merchant presentation and road-bound transport.

Defense role separation:

- guard post: close routine local defense;
- watchtower: loaded-area longer-range threat observation/response with one tower-assigned response guard;
- barracks: formal supplied multi-slot town garrison;
- dangerous-region military outpost: one supplied remote sentry that secures a loaded risky territorial foothold and stands down when risk evidence disappears.

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

Alpha.41 remote military rules are intentionally different from barracks:

- only otherwise-general outposts may dynamically gain the military role;
- bounded area must already be loaded before danger or missing-sentry state is trusted;
- danger combines total Monster pressure, close pressure, hostile-type diversity and enclosed low-light samples rather than a single count or menu toggle;
- one sentry maximum per qualifying outpost;
- local replacement cost is food6 + metal2 from the physical outpost stockpile;
- target local reserve is food12 + metal4;
- tagged sentry drops no iron/resources;
- danger loss clears forced target and returns the sentry home instead of despawning it;
- external hostiles using the standard Monster hierarchy are soft-compatible without hard companion code.

Current soldier/sentry bodies are Iron Golem proxies. Humanoid soldier visuals, external weapon loadouts, formations and class variety remain later presentation/combat-depth work.

Normal production jobs should fill automatically from appropriate settlement conditions. Loaded areas show physical movement/work. Do not force-load chunks solely to keep animations running.

Construction-office and Alpha.39 advanced-forging specialists are currently building-bound service NPCs and do not inflate civilian population/housing. The normal workshop artisan remains a civilian job. Alpha.40 fishing workers and Alpha.41 military-outpost sentries follow the existing outpost/service-unit convention and likewise do not inflate civilian population. A future citizen-assignment cleanup may unify these distinctions, but must not accidentally double-count population or create free tier progression.

No family/children simulation in planned scope.

## 7. Resources and physical logistics

Resources remain physical Minecraft items. HUD numbers are a cached view, not authority.

- workers deposit real ItemStacks;
- construction consumes real ItemStacks;
- avoid every-tick scanning of arbitrary player chests;
- use tags/categories so compatible external materials can participate;
- warehouses, cart-station freight bays and construction-office material bays add real physical storage positions rather than abstract capacity currency;
- opt-in service/commission barrels are not generic shared storage unless their specific system explicitly says so;
- Alpha.45 exploration score is not a resource and can never satisfy an ItemStack cost.

Distant logistics remains spatial. Transport workers belong to a specific outpost, follow persisted road-network waypoints, carry actual cargo and pause at unloaded route boundaries rather than teleporting or force-loading.

Alpha.27 tagged road logistics remains the single authority for outpost transport at every tier. Do not reintroduce generic-name/UUID pairing or a second transport navigation controller.

### Alpha.34 cart-station rule

- village tier + road-connected outpost;
- station must be within12 blocks of an existing road;
- four physical freight barrels join settlement storage;
- existing outpost transporter still owns the route;
- incoming cargo prefers station bays;
- per-trip outpost cargo rises 16→32;
- full station falls back to another valid storage target;
- no teleport cargo or new route authority.

A future wagon entity may be presentation/vehicle behavior only and must not become a duplicate economy simulation.

### Alpha.40 fishing-trade cargo rule

- only an otherwise-general outpost may gain the current dynamic fishing overlay;
- qualification is evaluated from already-loaded local world state: radius12, at least24 open surface-water columns and a safe dry bank;
- fishing produces ordinary cod/salmon ItemStacks into the existing outpost stockpile;
- because fish are food items, existing Alpha.27 logistics transports them without a new route controller;
- `수변교역` currently means this catch participates in the existing physical outpost→road→town economy, not that emeralds or trade points are generated remotely;
- no forced chunks or abstract fish/trade currency.

### Alpha.41 military reverse-supply rule

- dangerous-region military role never gets a separate long-distance transporter;
- the existing outpost-assigned Alpha.27 worker remains the single route/navigation authority;
- while military role is active, that worker can return empty to town, physically extract needed food/metal from fully loaded settlement storage and carry the stack back along the same persisted route;
- supply trip and empty-return state are entity tags on that same worker, not a second economy simulation;
- physical outpost stockpile is the authority for sentry recruitment;
- target reserve is food12 + metal4; replacement consumes food6 + metal2;
- when danger stops qualifying, military trip state is cleared and ordinary outpost→town cargo behavior resumes;
- no teleport cargo, abstract supply points, forced chunks or offline replenishment.

### Alpha.42 bounded unloaded-work rule

Physical ItemStack/block/container state remains authority even when players explore far away.

- `SettlementDeferredOutpostData` is a separate auxiliary SavedData and is **not** the settlement resource ledger;
- it stores only bounded `productionTicks`, `logisticsTicks`, last verified general-outpost overlay and whether a real assigned transporter has previously been observed;
- sampling is every200 server ticks and occurs only in the normal daytime/work branch;
- production and logistics debt are each capped at24,000 ticks per outpost;
- persisted lumber/quarry/mining/agriculture specializations may bank production time while their local outpost/stockpile is unloaded;
- a general outpost may bank fishing time only when its last loaded verified overlay was fishing; general/military observations do not bank production;
- deferred production never creates an ItemStack. It only lets a later loaded physical work action become due immediately while credit remains;
- credit is consumed only after actual physical output succeeds: real tree/stone/ore/crop/water conditions still gate the harvest/catch;
- if the real resource is exhausted or invalid, no item is produced and no production credit is consumed;
- logistics debt may accumulate only after the real assigned transporter has been observed and its persisted route is later not fully loaded;
- logistics debt is never cargo. Each1,200 ticks of credit can only raise the next **real physical pickup** to at most2× its normal16/32 batch, with an absolute64-item cap;
- logistics credit is consumed only when actual container extraction exceeds the normal batch;
- the same existing worker still carries that actual ItemStack along persisted road waypoints and physically inserts it at town/cart-station storage;
- no real-world elapsed-time catch-up while the server is stopped;
- no force-load, teleport inventory, virtual stockpile, virtual wagon, virtual resource/cargo or second transport controller;
- `/frontier status` may expose deferred work ticks as diagnostic/status information, but those numbers are never spendable resources.

This is intentionally a bounded catch-up model rather than pretending unloaded Minecraft blocks/entities were simulated continuously.

## 8. Roads, outposts and territory

Roads/outposts are the spatial-growth layer. Progression must not become endlessly enlarging one flat central base.

Road intent:

`choose start -> choose route/end -> preview -> approve -> physical grading/build`

Alpha.35 bounded terrain adaptation:

- one-block longitudinal rise becomes actual cobblestone stair road pieces;
- short water runs between dry banks may become 3-wide stone-brick deck;
- max automatic water span6 centerline blocks;
- bank height difference max1;
- water stays in place;
- no free log/cobblestone support generation;
- stairs/bridges add real stone cost;
- optional bridge profile is active-construction metadata compatible with older saves;
- completed logistics path remains ordinary `RoadSegment` centerline.

Alpha.44 retaining work currently applies to functional building sites, not a claim that large roads gained arbitrary cliff engineering. Large ravine bridges, tunnels and monumental civil engineering remain later breadth.

No early teleport network; roads/logistics must retain meaning.

Implemented outpost specializations / overlays:

- lumber;
- quarry;
- mining;
- agriculture;
- Alpha.40 coast/river fishing-trade overlay for qualifying general outposts;
- Alpha.41 dangerous-region military overlay for qualifying general outposts;
- Alpha.42 bounded unloaded-work catch-up over already-established specialization/route evidence.

Still required/partial by original scope:

- better biome-aware specialization with companion worldgen;
- richer coast/river presentation such as dedicated pier/harbor/waterborne merchant behavior if it adds value without duplicate economy authority;
- Alpha.42 is the first safe coarse unloaded-work layer, but real-play pacing/reload/exploit acceptance is still required before treating unloaded simulation as fully closed.

tier-visible public works may make established territory more readable, but only in loaded safe locations and without overwriting player work or generating farmable free blocks.

## 9. Combat and exploration

This is not a constant wave-defense game.

- guard posts handle routine local threats;
- watchtowers extend loaded-area observation/response;
- barracks provide supplied formal town garrison;
- dangerous-region outposts provide one supplied remote foothold sentry only while loaded world evidence justifies the role;
- occasional meaningful threats are allowed;
- do not spam mandatory waves.

Watchtower rules:

- guard-post prerequisite;
- 7×7 physical tower, wood96/stone72, clear height14;
- normal grading/hauling/scaffold build;
- one tagged response guard per loaded tower;
- roughly40-block loaded Monster response;
- creepers excluded from forced targeting;
- no global radar or chunk force-load.

Exploration content should include ruins, mines, camps, nests, structures, dungeons, caravans, rare resources, bosses and rare NPCs primarily through the external content stack where strong implementations already exist.

Frontier connects exploration outcomes back into settlement growth instead of reimplementing all adventure content.

### Alpha.45 exploration/conquest progression rule

Alpha.45 is the first direct implementation of that bridge.

- once every100 server ticks, only online players' current already-loaded positions are checked;
- Frontier asks the normal dynamic structure registry/StructureManager whether the player is actually inside a structure piece; it does not `/locate`, search distant chunks or generate anything;
- external structure namespaces are accepted generically, excluding `minecraft`, `frontier_settlement` and `neoforge`, so Dungeons and Taverns/Repurposed Structures can participate without hard Java imports;
- progress is by unique structure **type**, not each generated instance; repeated copies of one structure cannot be farmed;
- conquest requires direct player attribution;
- Ender Dragon and Wither count as explicit vanilla conquest milestones;
- external `Mob` types with max health>=80 may count as soft-compatible strong-enemy/boss milestones;
- each conquest entity type counts once;
- shared SavedData bounds discovered structure types to64 and conquest types to32;
- derived exploration score is `min(8, unique structures + unique conquest types*3)`;
- score is non-spendable metadata, never ItemStack/resource authority;
- `/frontier status` exposes structure-type count, conquest-type count and score for transparency;
- legacy settlement-tier routes remain valid: no old world is downgraded because its new exploration lists start empty;
- alternate accelerator route for frontier town: population7 +2 outposts + mine + quarry + score2;
- legacy frontier-town route remains population8 +2 outposts + mine + quarry;
- alternate accelerator route for domain: population14 +3 outposts + mine +2 farms + score5;
- legacy domain route remains population16 +4 outposts + mine +2 farms;
- no loot generation, resource minting, companion content mutation, teleport, chunk force-load or global radar.

Future exploration work may deepen rare-resource/NPC/boss-specific rewards, but must preserve the rule that Frontier is progression glue and companions own their adventure content.

## 10. External content is a core development accelerator

External mods are not merely visual recommendations. The intended full play experience uses them to supply content breadth while Frontier remains settlement/progression glue.

Current candidate 26.2 stack is locked in `COMPANION_LOCK.json`:

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
- do not redundantly rebuild strong world/dungeon/combat/weapon systems supplied by companions;
- common/additive tags are preferred over hard-coded companion classes;
- optional companion API code must be isolated so absence/version drift cannot become core settlement authority;
- soft registry observation such as Alpha.45 must not cause companion structure generation or remote scanning;
- MIT/LGPL/clear public-license material may be reused only within license obligations and attribution;
- ARR/ND/restricted content stays official dependency/reference-only;
- public source visibility alone is not reuse permission;
- `EXTERNAL_CONTENT_REGISTER.md` records the boundary.

The lock stays `candidate_runtime_lock` until all entries launch together in the target client/server environment.

Exact Alpha.44 investigation against locked Xaero's Minimap 26.4.2 established that the historical public `WaypointsManager` class/API is absent. Do not fake completed marker support by reaching into version-internal waypoint sets with reflection/mixins. Alpha.43 HUD collision avoidance remains the safe Xaero integration until a stable seam exists.

## 11. External-content / settlement bridges implemented

### Alpha.31
- additive wood/stone/metal/food tags and compatible `c:` material categories;
- expedition relic tag;
- physical relic storage scan;
- external weapon namespace recognition, initially Weapons Expanded;
- no companion `ModList` boot requirement.

### Alpha.32 market
- protected trade barrel;
- shared storage never auto-sold;
- deliberately deposited relics only;
- visiting merchant + real emerald payout;
- full output stalls safely.

### Alpha.33 workshop
- blacksmith prerequisite;
- dedicated service barrel;
- recognized damaged external weapon maintenance;
- assigned artisan physically collects one real metal;
- one metal repairs64 durability;
- unused metal physically returned.

### Alpha.34 cart station
- road-adjacent physical freight depot;
- four freight barrels;
- existing road transporter deposits there preferentially;
- transport batch16→32;
- route authority unchanged.

### Alpha.35 road terrain adaptation
- real stair pieces and bounded short stone-brick bridges;
- extra real stone cost;
- no water deletion/free economic support materials.

### Alpha.36 watchtower
- tall climbable physical tower;
- tower-assigned loaded response guard;
- no new key/global radar/force-load.

### Alpha.37 barracks
- supplied 3-slot regular garrison;
- real recruitment food+metal;
- separate military capacity;
- no iron-drop exploit or free high-tier reinforcement backend.

### Alpha.38 construction office
- four physical construction material bays;
- office-first wood/stone staging in same ledger;
- one loaded physical supply runner per office;
- max32 carried stack, target96/96 reserve, source radius24;
- existing builder remains sole construction authority.

### Alpha.39 advanced workshop

The first high-tier crafting loop is deliberately separate from market sale and ordinary repair.

- `ADVANCED_WORKSHOP`: 15×11, wood168/stone120, clear height14;
- unlock: frontier-town + one normal workshop + one market;
- protected dedicated commission barrel stays outside generic shared storage;
- commission input is explicit player intent: one recognized, currently unenchanted external damageable weapon + one expedition relic;
- one building-bound advanced forging specialist physically fetches real metal from loaded shared settlement storage;
- forge cost: relic1 + metal4;
- compatible enchanting-table result uses power30;
- valid enchant output is constructed/checked **before** metal or relic mutation;
- failed/incompatible enchant attempt consumes nothing;
- success fully repairs the same weapon and applies the generated compatible enchantments;
- no hard Weapons Expanded item/class reference, no auto-scan of shared storage for commissions, no teleport inventory or chunk force-load.

Role split is fixed:

`market = relic -> trade value`

`normal workshop = metal -> external weapon repair`

`advanced workshop = external weapon + relic + metal -> high-tier forge`

This closes the first original high-tier-crafting placement family, not every future recipe/specialized-production possibility.

### Alpha.40 coast/river fishing-trade outpost

- no new building family or specialization-management screen;
- qualifying otherwise-general outposts are recognized dynamically from loaded shoreline geometry;
- radius12 scan requires at least24 open surface-water columns and a safe dry bank;
- one outpost-assigned fishing villager carries a visible fishing rod in the off hand;
- worker walks to the bank, waits on a140-tick work cadence, then produces1–3 real cod/salmon ItemStacks only if the water remains valid/loaded;
- catch is carried back to the existing physical outpost stockpile;
- existing food-cargo rule and the single road-transport authority move fish to town/cart-station storage;
- status reports loaded fishing-trade outpost count and effective recent-outpost role;
- no remote emerald generation, abstract trade points, teleport cargo, global water scan or force-load.

The current waterborne trade identity comes from the catch entering the established physical logistics economy. Dedicated docks, boats, water merchants and direct fish-market contracts remain later optional breadth.

### Alpha.41 dangerous-region military outpost

- no new building family, key, troop screen or manual specialization selector;
- qualifying otherwise-general outposts are recognized dynamically from bounded already-loaded danger evidence;
- evidence is multi-factor: total Monster pressure + close pressure + hostile-type diversity and/or enclosed low-light samples, not one arbitrary mob count;
- one persistent outpost-assigned sentry maximum;
- one replacement requires local physical food6 + metal2, with target reserve food12 + metal4;
- unloaded patrol/search areas are never interpreted as missing troops;
- sentry forced pursuit excludes Creepers and its Iron Golem proxy drops are cleared;
- when the danger condition is lost the sentry stands down and returns home rather than disappearing;
- the same Alpha.27 transporter performs reverse food/metal supply along persisted roads, preserving single transport authority;
- military role has precedence over Alpha.40 fishing while active, and fishing/general behavior may resume after danger clears;
- normal `Monster` subclasses from external content participate without a hard mod/class dependency;
- no teleport, fast travel, force-load, free troop points, free iron, virtual supply or offline combat simulation.

### Alpha.42 bounded unloaded-work catch-up

- no new building, key, worker type or route authority;
- separate persisted debt tracks elapsed server-running work time only;
- debt is bounded to one Minecraft day per production/logistics channel;
- no virtual item/cargo balance exists;
- loaded production redeems time only through the existing real harvesting methods and only after successful ItemStack creation;
- loaded logistics redeems time only by allowing the existing transporter to extract a larger real batch from the existing physical container;
- maximum catch-up pickup is64 items and still follows the same persisted road;
- last verified fishing overlay can remain eligible for catch-up while unloaded, but military/general state does not fabricate fishing production;
- `/frontier status` reports deferred time for transparency, not as a spendable resource;
- no force-load, teleport, real-world offline generation or second simulation economy.

### Alpha.43 compact context / Jade presentation

The first companion-facing status integration is deliberately presentation-only.

- `SettlementContextService` derives bounded display context from the same server-authoritative stockpile/building/outpost/construction state; it is not another SavedData ledger or simulation authority;
- context targets include the shared physical stockpile, completed functional buildings, completed outposts and the active building construction footprint;
- active building/road/outpost project name and percentage are sent through the existing settlement snapshot and shown in the compact HUD while work is active;
- client transition notices are intentionally narrow: settlement tier growth, project start, completed functional building and new outpost;
- side notices render on the right, expire after6 seconds and keep at most3 entries; no modal popup and no new control;
- Jade target version is exactly the candidate-lock `26.2.2+neoforge` / version ID `HLYMycSr`;
- Gradle uses `compileOnly` against that exact artifact. Distributed Frontier therefore does not turn Jade into a declared runtime implementation dependency;
- all `snownee.jade` references stay quarantined under `compat/jade`; core settlement/network/client logic has no Jade import;
- the Jade provider is client-side presentation: crosshair position is matched against the synchronized authoritative context and shows at most a title plus one compact detail/progress line;
- Jade absence must not affect the settlement data model, resources, jobs, construction, logistics or progression;
- Xaero integration is layout safety only: if `xaerominimap` is present, the normal Frontier resource HUD moves below the expected top-left minimap region.

### Alpha.44 bounded medium terrain works

- normal building footprint span3–4 gains explicit terrain-work acceptance;
- >4 is rejected;
- cut/support depth stays bounded;
- exposed deep edge foundation uses real physically hauled/consumed stone before visible cobblestone support;
- no free-drop terrain farming or duplicate terrain authority.

### Alpha.45 exploration/conquest progression

- already-loaded player-position external structure pieces feed unique shared discovery milestones;
- direct player conquest of dragon/wither or external max-health>=80 Mob types feeds unique conquest milestones;
- score cap8, structure+1, conquest+3;
- no duplicate same-type farming;
- score is non-spendable and only supplies alternate tier accelerator conditions;
- legacy tier conditions remain accepted.

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

Avoid essential vanilla conflicts. Do not proliferate N/J/K or one new key per feature.

Current status hierarchy:

- always-on: existing resource/tier/next-goal HUD;
- while a project is active: one extra compact project/progress line;
- important transitions: bounded right-side notice queue, max3 / 6 seconds;
- when Jade is present: crosshair-local infrastructure title + one status/role line, not a management panel;
- when Xaero is present: Frontier top-left HUD moves below the minimap area to avoid immediate overlap;
- exploration structure/conquest milestones use rare event messages plus one existing `/frontier status` line, not a new quest screen.

Still missing/partial from original UI scope: richer material/progress detail for some building/service states and true Xaero settlement/outpost/road-map synchronization. Do not solve those by adding a giant Frontier dashboard.

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
- Frontier path changes only except its canonical workflow/result files;
- CI result bot may advance main;
- final accepted result must point at the intended Frontier source/docs SHA.

Optional companion integration rule:

- missing companion must not turn Frontier core into a boot failure;
- use documented/stable APIs where available;
- isolate optional API references under compat boundaries;
- if a companion exposes no stable API, prefer honest partial integration over brittle version-internal reflection that risks corrupting or boot-breaking the core mod.

## 14. Current playable slice after Alpha.45

The playable slice now includes:

- one shared authoritative settlement;
- protected founding stockpile and civic core;
- compact B/R/Enter/Backspace interaction;
- **15 functional building families**;
- physical site grading and construction hauling;
- Alpha.44 bounded medium terrain works with real retaining stone;
- physical construction material staging and supply runner;
- paced loaded town production;
- physical roads with one-block stair adaptation and bounded short-water bridges;
- physical specialized outposts plus Alpha.40 coast/river fishing-trade overlay;
- Alpha.41 dangerous-region military overlay with one supplied remote sentry;
- persisted road-bound transport including physical reverse food/metal military supply;
- Alpha.42 bounded unloaded-work catch-up that never turns deferred numbers into resources/cargo;
- Alpha.43 project-progress HUD line, bounded compact side notifications and optional Jade infrastructure context;
- Xaero-aware HUD collision avoidance without claiming marker synchronization;
- Alpha.45 unique external-structure/conquest milestones feeding alternate shared tier-growth routes;
- tier growth and safe public works;
- external material/relic/weapon recognition;
- physical market and normal staffed repair workshop;
- explicit rare-material advanced forging;
- road-adjacent cart-station freight hub;
- climbable watchtower loaded response;
- supplied barracks regular garrison.

This is **not** equivalent to original v0.2 completion. `COMPLETION_GAP_AUDIT.md` remains authoritative for unfinished breadth.

## 15. Unfinished original-scope priorities

Unless real-play regression overrides them:

1. full `COMPANION_LOCK.json` fresh-world client/server runtime and multiplayer test when the build reaches the user’s chosen test point;
2. richer coast/river presentation/trade only if it remains physical and does not duplicate road logistics;
3. broader high-tier recipe/specialized crafting only where exploration materials justify it;
4. humanoid/weaponized soldier presentation if it improves the Better Combat / Weapons Expanded stack;
5. selected-area cut/fill and larger civil engineering only with strong safety/player-build protection;
6. deeper companion exploration rewards/rare NPC or boss-specific bridges only if they remain soft and non-farmable;
7. long survival/multiplayer acceptance and balance/pathfinding audit;
8. Alpha.42 unloaded catch-up pacing/reload/exploit validation before declaring that original gap fully closed;
9. Alpha.43 Jade/Xaero/HUD visual/runtime acceptance before declaring the UI-companion gap fully closed;
10. true Xaero settlement/outpost markers only if a stable supported API/seam becomes available; do not use brittle internal waypoint injection merely to check a box.

Do not add meaningless building families merely to raise the count above15.

## 16. Real-play acceptance focus

Automated CI is not a substitute for Minecraft play. Test, in order when the user reaches the planned test point:

- founding -> house/lumber/farm/quarry/warehouse;
- building small grading plus a real span3–4 Alpha.44 medium-terrain site; verify explicit terrain-work feedback, bounded cut/fill and visible real-stone retaining foundation;
- verify >4 span/unsafe site rejection and no recoverable free material path;
- building hauling and save/reload mid-project;
- construction-office runner source selection, physical carrying, office staging and builder preference;
- road stairs/short bridge navigation;
- road -> outpost -> specialization production -> transport -> cart station;
- verify resource HUD remains readable both with and without Xaero's Minimap installed and does not cover the minimap default region;
- with Jade installed, look at shared stockpile, several completed building blocks, active construction and an outpost; confirm no more than compact title/detail/progress context appears and vanilla/Jade information stays readable;
- verify Jade absent does not prevent Frontier from loading or alter settlement behavior;
- verify project-start/build-complete/outpost/tier notices appear once, stay on the side, cap at3 and disappear without requiring dismissal;
- enter multiple actual external Dungeons and Taverns/Repurposed Structures structure types in a fresh companion world; verify only current loaded structure pieces count and repeat copies do not increase the type count;
- directly kill Ender Dragon/Wither and any qualifying external strong enemy available in the final pack; verify same entity type only counts once and non-player deaths do not count;
- verify exploration score persists save/reload, caps at8 and cannot be spent as resources;
- verify a legacy no-exploration world can still reach frontier town/domain with the old population/outpost requirements;
- verify exploration accelerator routes trigger only with their remaining infrastructure/population requirements;
- place an otherwise-general outpost near a real river/coast, verify `어업·수변교역`, visible rod/bank movement, fish stockpile deposit and road transport;
- verify small puddles/invalid shorelines do not produce fish;
- leave a previously verified fishing/specialized outpost unloaded during server-running work time, return, and verify deferred work speeds only real physical actions rather than minting resources;
- exhaust nearby logs/stone/ore or remove mature crops/water, then verify deferred production remains pending and cannot create substitute items;
- verify production debt never exceeds24,000 ticks and survives save/reload without duplication;
- create logistics backlog with an observed assigned transporter, reload the route, and verify the next actual pickup may be larger but never exceeds64 and still physically travels the road;
- verify no logistics credit is consumed when the outpost container cannot provide more than the normal batch;
- stop/restart the server and verify real-world downtime does not create debt;
- place an otherwise-general outpost in a genuinely hostile loaded area and verify multi-factor `위험지역 군사거점` activation, one sentry only, Creeper non-pursuit and no iron drop;
- verify the existing transporter returns to town, physically carries food/metal down the persisted road, fills the outpost reserve, and a killed sentry cannot respawn without local food6+metal2;
- clear the danger and verify the sentry stands down, military supply state clears and normal/fishing role can resume without duplication;
- external dungeon/loot -> choose market sale vs normal repair vs advanced-forging commission;
- advanced workshop external-weapon compatibility, relic/metal no-loss failure behavior and completed enchanted weapon retrieval;
- guard post -> watchtower -> barracks response and replacement cost;
- day/night routines and loaded/unloaded boundaries;
- two-player shared state/storage/construction/logistics/exploration progression;
- full companion-stack fresh world.

Real-play observations override assumptions. Fix root causes before adding more breadth when testing exposes a regression.
