# Frontier Settlement — Canonical Plan

This file is the repository-side implementation authority for Frontier Settlement. Read it together with `ORIGINAL_DESIGN_v0.2.md`, current `main` source, `COMPLETION_GAP_AUDIT.md`, companion lock/register, README and CI results before continuing development.

`ORIGINAL_DESIGN_v0.2.md` is the scope foundation/ceiling. This file may make that design more concrete, but it must not silently shrink unfinished original requirements to match whatever is currently implemented.

## 1. Product identity

Frontier Settlement is a Minecraft Java cooperative survival settlement / territory-growth mod.

Core loop:

`survival -> settlement growth -> better exploration -> external exploration / conquest -> NPCs / resources / technology -> settlement growth`

Player role:

`survivor -> pioneer -> village leader / lord-like role -> domain operator`

Settlement scale:

`pioneer camp -> hamlet -> village -> frontier town -> domain`

Hard identity rules:

- **One world/server has one shared settlement** and territory state;
- server authoritative;
- **Resources remain physical Minecraft items**; HUD/cache/context are presentation only;
- player-built structures are never scanned into functional Frontier buildings;
- repeated hauling, production and routine job assignment are automated;
- loaded areas should visibly move/work;
- no chunk force-load merely to keep simulation running;
- no teleport cargo merely to fake long-distance work;
- do not silently destroy player containers, fluids, valuable blocks or unrelated builds;
- companion mods supply terrain/dungeon/combat/weapon/loot breadth while Frontier remains settlement/citizen/construction/logistics/territory/progression glue.

## 2. Interaction budget and controls

Many backend systems are allowed; direct micromanagement stays small.

Primary player actions remain approximately:

1. place/take real resources in physical settlement storage;
2. choose building type, position and rotation;
3. choose outpost location;
4. choose road start/end and only necessary route guidance;
5. explore/fight and decide which rare loot is committed to trade/crafting/progression.

Controls remain fixed:

- `B`: settlement palette
- `R`: rotate current building placement
- `Enter`: confirm
- `Backspace`: reset/cancel

Avoid E/Q/F/number/Shift/Ctrl/Space/chat/camera conflicts. Do not proliferate N/J/K or one new key per feature.

Physical-intent rules:

- market sale = eligible relic deliberately placed in market barrel;
- workshop repair = eligible damaged external weapon deliberately placed in service barrel;
- Alpha.39 first advanced forge = unenchanted external weapon + relic in advanced commission barrel;
- Alpha.47 domain reforge = already-enchanted external weapon + two relics in the same commission barrel;
- Alpha.40 fishing role is inferred from loaded shoreline geography;
- Alpha.41 military outpost role is inferred from loaded threat evidence;
- Alpha.42 catch-up stores bounded elapsed work time, not resources;
- Alpha.43 status uses compact HUD/notices/Jade instead of a management dashboard;
- Alpha.44 medium terrain stays inside normal construction authority;
- Alpha.45 exploration/conquest progress is observed automatically;
- Alpha.46 waterfront works reuse the fishing worker and existing road transporter;
- Alpha.48 military presentation adds no key, troop screen, equipment screen or military currency.

Do not add tax-rate panels, dozens of happiness stats, family schedules, per-worker priority matrices, giant research trees or manual hauling routes.

## 3. Multiplayer authority

One world/server has one shared settlement. All players share:

- resources;
- buildings;
- population/housing;
- roads/outposts;
- progression;
- construction state;
- exploration/conquest milestones.

The server remains authoritative. Clients render synchronized state and submit bounded requests only.

No independent per-player settlements, player politics, tax-distribution layer or client-authoritative military equipment path belongs in the initial scope.

## 4. Founding and early loop

Founding establishes:

- the shared settlement;
- protected starter stockpile/civic anchor;
- one dedicated builder;
- first resource loop;
- physical construction flow.

The starter stockpile is progression-critical infrastructure and must not be casually destructible into a softlock.

## 5. Functional buildings and construction

Original target remains approximately **15–20 meaningful building families**. Alpha.39 reached 15; Alpha.40–48 deepen systems without adding fake count-padding families.

Current families are exactly:

1. house
2. lumber camp
3. farm
4. quarry
5. mine
6. warehouse
7. construction office
8. blacksmith
9. workshop
10. advanced workshop
11. guard post
12. watchtower
13. barracks
14. market
15. cart station

Town-center value is currently represented through civic-core/tier infrastructure rather than a separate placement family.

Construction UX:

`palette -> completed ghost preview -> position/rotation -> terrain/overlap/cost validation -> project -> grading -> hauling -> phased building -> completion`

Construction phases:

`site clearing -> hauling -> foundation -> frame -> walls -> roof -> interior -> completion`

Presentation invariant: **builder walks from actual settlement storage carrying real wood/stone stacks** and visibly stages/uses them at the site.

### Terrain rules

- footprint span0–2 = established small grading;
- span3–4 = bounded medium terrain, explicitly `지형 공사 포함`;
- span>4 or unsafe fluid/block-entity/support geometry = reject;
- natural-ground cut is bounded to three blocks relative to project grade;
- fill/support depth remains bounded to three;
- deep exposed outer support uses cobblestone retaining/foundation only after real stone is hauled/staged/consumed;
- extra retaining stone is capped at96/project and included in resource approval;
- no free cobble, loose-drop excavation or silent mountain deletion;
- `SettlementConstructionService` remains the single building/terrain authority;
- arbitrary selected-area cut/fill, large retaining works, ravine civil engineering and tunnels remain unfinished.

### Construction office

The construction office is a physical staging accelerator, not a second builder.

- village + warehouse unlock;
- four physical material barrels;
- one office-assigned runner while a project is active;
- runner physically extracts/hauls real wood/stone from loaded ordinary storage;
- no force-load/teleport inventory/virtual construction points;
- existing builder remains the blueprint/terrain authority.

## 6. Citizens, jobs and military roles

Vanilla villager profession/trading is not settlement-progression authority.

Implemented visible/service roles include builder, logger, farmer, quarry worker, miner, fishing worker, waterfront trader, workshop artisan, construction supply runner, advanced forging specialist, guards, barracks soldiers, military-outpost sentry, market merchant presentation and road transporter.

Military role separation:

- guard post = routine local defense;
- watchtower = loaded-area observation/response;
- barracks = formal supplied three-slot town garrison;
- dangerous-region outpost = one supplied remote sentry while loaded danger evidence qualifies.

### Barracks economic authority

- frontier-town + watchtower + blacksmith prerequisite;
- exactly three persistent military slots/barracks;
- each replacement costs real food8 + metal2;
- military capacity is separate from civilian population/housing;
- unloaded patrol areas are not interpreted as dead soldiers;
- tagged military bodies do not drop farmable resources;
- creepers are excluded from forced pursuit;
- old free reinforcement backend remains removed.

### Military outpost economic authority

- only otherwise-general outposts can dynamically qualify;
- loaded danger combines hostile count, close pressure, hostile diversity and enclosed darkness;
- at most one sentry/outpost;
- local replacement cost real food6 + metal2;
- reserve target food12 + metal4;
- same Alpha.27 road transporter reverse-supplies food/metal;
- military overlay takes precedence over fishing;
- stand-down clears forced combat/returns home rather than deleting the sentry.

### Alpha.48 supplied humanoid military presentation

Alpha.48 separates **server combat authority** from **client visual body** instead of changing to a naturally hostile humanoid mob.

- `FrontierSoldierEntity` is a Frontier-owned entity type that **extends `IronGolem`**;
- its server attributes are created from `IronGolem.createAttributes()`;
- existing proven targeting/navigation/combat economics remain authoritative;
- collision/body dimensions are human-scaled for the dedicated type;
- client renderer uses a humanoid/player model;
- a service sword is injected only into the client render state;
- **visual service sword is never a server ItemStack**;
- server barracks/military services never call `setItemSlot` to create that sword;
- no weapon is inserted into settlement storage or military loot;
- the visual sword provides no ItemStack attribute modifiers and cannot be acquired by killing a soldier;
- older loaded tagged Iron Golem soldiers/sentries migrate **1:1** to `FrontierSoldierEntity`;
- migration preserves assignment tags, name, rotation and bounded current health;
- migration does not call recruitment resource consumption and does not add a military slot;
- recruitment costs remain food8+metal2 for barracks and food6+metal2 for remote sentries;
- no civilian population/housing change occurs;
- no Better Combat or Weapons Expanded Java class is required for core boot.

Alpha.48 closes the **humanoid visible body** gap only. A physical external-weapon armory/loadout loop remains unfinished. If implemented later, it must consume/hold actual ItemStacks and may not mint free companion weapons or create duplicate military capacity.

## 7. Resources and physical logistics

Resources remain real ItemStacks.

- workers deposit real items;
- construction consumes real items;
- service barrels are explicit-intent infrastructure, not hidden generic storage;
- warehouses/cart-station/office bays extend physical storage rather than abstract capacity points;
- avoid scanning arbitrary player chests every tick;
- external-compatible tags/categories are preferred to hard Java dependencies;
- Alpha.45 exploration score is non-spendable and can never satisfy an ItemStack cost.

**Transport workers belong to a specific outpost**, follow persisted road-network waypoints, carry actual cargo and **pause at unloaded route boundaries** rather than teleporting or force-loading.

Alpha.27 tagged road logistics remains the **single authority for outpost transport** at every tier.

### Cart station

- village + road-connected outpost;
- four physical freight barrels;
- incoming outpost cargo prefers them;
- batch16→32;
- existing transporter still owns the route;
- full station falls back to another valid storage target.

### Fishing/waterfront

- loaded qualifying general outpost can gain fishing role;
- catches are real cod/salmon in physical outpost stockpile;
- existing transporter moves them through normal food cargo;
- Alpha.46 landing consumes real local wood block-by-block;
- local wood shortage may use the same transporter for reverse supply;
- military reverse supply has precedence;
- dedicated trade barrel only: **16 real cod/salmon -> 1 real emerald**;
- ordinary stockpile is never auto-sold;
- no second water logistics economy.

### Bounded unloaded work

`SettlementDeferredOutpostData` stores time debt only.

- production/logistics debt cap24,000 ticks/outpost;
- credit makes later real loaded actions due sooner;
- production credit is consumed only after a physical output succeeds;
- logistics credit can raise a real pickup to at most2× normal, absolute64;
- no virtual cargo, server-offline real-time simulation or teleport inventory.

## 8. Roads, outposts and territory

Road flow:

`choose start -> choose end/limited guidance -> preview -> approve -> physical grading/build`

Current road terrain handling includes:

- one-block longitudinal stairs;
- bounded short water bridge max6 centerline blocks;
- real stone surcharge;
- water remains in place;
- persisted ordinary road centerline still drives logistics.

Large ravine bridges, tunnels and cliff-scale road engineering remain later work.

Current outpost breadth:

- lumber;
- quarry;
- mining;
- agriculture;
- dynamic coast/river fishing-trade;
- dynamic dangerous-region military;
- bounded unloaded catch-up;
- persisted physical waterfront landing/trade.

Better companion-biome specialization remains partial.

**tier-visible public works** may make established territory more readable, but only in loaded safe locations without overwriting player work or generating farmable free blocks.

## 9. Exploration, combat and high-tier crafting

Frontier does not own companion dungeons/bosses/worldgen. It observes/consumes outcomes and connects them back to settlement progression.

### Alpha.45 progression bridge

- only current already-loaded online-player position is examined on the bounded cadence;
- external structure registry namespaces are observed generically;
- no `/locate`, remote scan or chunk generation;
- unique structure type counts once;
- direct-player-attributed dragon/wither and qualifying external strong-enemy types count once/type;
- score is capped8 and non-spendable;
- legacy frontier-town/domain routes remain valid;
- alternate lower-pop/outpost routes require exploration score plus the ordinary production infrastructure.

### High-tier crafting split

`market = relic -> trade value`

`normal workshop = metal -> external weapon repair`

`Alpha.39 first forge = unenchanted external weapon + relic1 + metal4 -> validated power30 compatible forge`

`Alpha.47 domain reforge = already-enchanted external weapon + relic2 + metal8 -> compatible new additions only`

Alpha.47 rules:

- DOMAIN only;
- same protected advanced commission barrel;
- same specialist physically hauls real metal;
- candidates must be table-eligible, item-supported, absent from existing set and compatible with the full existing set;
- apply additions to copy first;
- result must improve;
- existing enchant levels may never decrease;
- no improvement = no relic/metal consumption;
- successful weapon is fully repaired;
- no hard Weapons Expanded class/item link;
- no crafting currency or second crafting building.

Additional high-tier recipes should be added only for genuinely distinct exploration materials/use-cases, not to inflate content counts.

## 10. External content stack

`COMPANION_LOCK.json` is the candidate target stack:

- Terralith + Lithostitched;
- Dungeons and Taverns;
- Repurposed Structures;
- Better Combat + libraries;
- Weapons Expanded;
- Lootr;
- Sophisticated Backpacks + Core;
- Jade;
- Xaero's Minimap.

Rules:

- optional companions must not become core boot requirements without explicit justification;
- common/additive tags are preferred to hard-coded companion classes;
- Frontier should not duplicate entire worldgen/combat/weapon systems;
- reuse/license boundaries stay in `EXTERNAL_CONTENT_REGISTER.md`;
- lock stays `candidate_runtime_lock` until full client/server fresh-world launch.

Jade remains compile-only and isolated under its compat package.

Xaero integration remains HUD collision avoidance only. Locked Xaero 26.4.2 does not expose the historical public `WaypointsManager` API; true Xaero settlement/outpost markers only if a stable supported API/seam appears.

## 11. Milestone summary

- Alpha.31 external material/relic/weapon recognition
- Alpha.32 physical relic market
- Alpha.33 staffed external-weapon repair
- Alpha.34 cart-station freight hub
- Alpha.35 stairs/short bridges
- Alpha.36 watchtower
- Alpha.37 supplied barracks
- Alpha.38 construction office physical staging
- Alpha.39 first advanced relic forge
- Alpha.40 shoreline fishing
- Alpha.41 dangerous-region military outpost
- Alpha.42 bounded unloaded work-time debt
- Alpha.43 compact HUD/notices/Jade + Xaero HUD offset
- Alpha.44 bounded medium terrain / real retaining stone
- Alpha.45 external exploration/conquest progression
- Alpha.46 real-wood waterfront + opt-in fish trade
- Alpha.47 DOMAIN relic reforge preserving old enchantments
- Alpha.48 supplied humanoid military entity/render presentation with no server-side weapon minting

## 12. UI and information hierarchy

Reference hierarchy remains:

1. Against the Storm — compact resource/status hierarchy;
2. Manor Lords — world-space placement;
3. MineColonies — Minecraft-native blueprint/material/construction presentation;
4. Frostpunk 2 — secondary territory-overview concepts.

Do not invent giant generic rectangle dashboards when a proven interaction reference exists.

Current presentation:

- compact always-on resource/tier/goal HUD;
- active-project progress line;
- bounded side notice queue;
- optional Jade crosshair context;
- Xaero-aware top-left offset;
- physical barrels represent market/repair/forge/waterfront trade intent;
- Alpha.48 soldier equipment is visual-only and requires no equipment UI.

## 13. Engineering rules

Target:

- Minecraft Java26.2;
- NeoForge26.2.0.38-beta;
- Java25;
- Gradle9.2.1.

Sequence:

`read current GitHub main -> inspect original/canonical/gap/source/CI -> implement -> manual source/gameplay audit -> Java25 clean build -> JAR verify -> direct main update`

Shared-repo rules:

- re-read remote `main` before every write;
- never force-push over concurrent work;
- CI result commits may interleave with unrelated projects;
- after result commits, confirm intended Frontier source/docs remain in current main;
- final accepted status must point at the intended Frontier source/docs SHA.

## 14. Current playable slice after Alpha.48

The playable slice includes:

- one shared authoritative settlement;
- exactly15 functional building families;
- physical storage/production/construction/logistics;
- bounded small/medium building terrain work;
- physical roads/stairs/short bridges;
- specialized outposts and one road-transport authority;
- fishing/military overlays;
- bounded unloaded-work catch-up without virtual resources;
- compact HUD/notices/Jade context;
- external exploration/conquest tier accelerators;
- physical relic market, repair, Alpha.39 first forge and Alpha.47 domain reforge;
- physical waterfront landing/trade;
- supplied barracks and dangerous-region military roles;
- Alpha.48 `FrontierSoldierEntity` humanoid client presentation while preserving IronGolem-derived server combat/economics;
- loaded old tagged military Iron Golem -> Frontier soldier 1:1 migration without re-charging recruitment.

This is not v0.2 completion.

## 15. Unfinished original-scope priorities

Unless a real-play regression overrides them:

1. **selected-area cut/fill and larger civil engineering** with strict player-build/resource-exploit protection;
2. **physical soldier armory/external-weapon loadout breadth** only if real ItemStacks can be supplied without free-weapon minting or hard companion dependency;
3. deeper rare NPC/structure/boss-specific settlement rewards that remain soft and non-farmable;
4. better biome-aware outpost specialization using stable data seams;
5. additional specialized crafting only when a genuinely distinct exploration material/use-case exists;
6. long survival + two-player multiplayer acceptance;
7. Alpha.42 catch-up pacing/save-reload/exploit acceptance;
8. Alpha.43 Jade/Xaero/HUD visual/runtime acceptance;
9. Alpha.46 waterfront pathing/site/trade acceptance;
10. Alpha.47 external-weapon reforge breadth/no-loss acceptance;
11. **Alpha.48 humanoid render/attack-animation + legacy migration acceptance**;
12. full `COMPANION_LOCK.json` fresh-world client/server runtime;
13. Xaero markers only if a stable supported seam appears;
14. moving wagon/boat presentation only if useful and never as duplicate logistics authority.

## 16. Real-play acceptance focus

Final/test-worthy acceptance must eventually cover:

- founding through early growth;
- construction grading/hauling and reload;
- construction-office staging;
- roads/stairs/short bridges/outpost logistics;
- shoreline fishing and waterfront wood/trade;
- dangerous-region activation/supply/stand-down;
- deferred-work cap/resource gating/no server-offline minting;
- exploration milestone dedupe and legacy tier routes;
- market vs repair vs first forge vs domain reforge intent separation;
- reforge no-loss/no-improvement behavior;
- barracks slot count and recruitment cost;
- Alpha.48 new soldiers visibly render humanoid and attack animation is acceptable;
- Alpha.48 server damage/health/targeting remains equivalent to the previous supplied combat body;
- Alpha.48 visual sword never appears in inventory/storage/drop and has no server attribute effect;
- loaded old barracks/sentry golems migrate once, preserve tags/name/health and do not consume recruitment resources;
- migration never creates duplicate soldiers/sentries or civilian population;
- optional companions absent/present boot behavior;
- two-player shared state;
- full companion-stack fresh world.

Real-play observations override assumptions. Fix root causes before adding more breadth when testing exposes a regression.
