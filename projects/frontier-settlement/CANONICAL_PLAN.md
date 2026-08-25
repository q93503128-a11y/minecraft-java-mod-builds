# Frontier Settlement — Canonical Plan

This file is the repository-side implementation authority for Frontier Settlement. Read it together with `ORIGINAL_DESIGN_v0.2.md`, current `main` source, `COMPLETION_GAP_AUDIT.md`, `COMPANION_LOCK.json`, `EXTERNAL_CONTENT_REGISTER.md`, README and current CI result before every continuation.

`ORIGINAL_DESIGN_v0.2.md` is the scope foundation/ceiling. This file may make that design more concrete, but it must never silently shrink unfinished original requirements to match the current code.

Current canonical implementation: **0.1.0-alpha.69**.

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
- do not use `destroyBlock` / `dropResources` as free-drop construction or civil-work shortcuts;
- companion mods supply terrain/dungeon/combat/weapon/loot breadth while Frontier remains settlement/citizen/construction/logistics/territory/progression glue;
- optional companion absence must never become a Frontier boot failure unless a future dependency is explicitly reclassified.

## 2. Interaction budget and controls

The original v0.2 rule remains: backend depth may grow, but repeated direct management must not.

Primary interactions:

1. put/take physical items from shared settlement storage;
2. choose a functional building and its position/rotation;
3. choose an outpost location;
4. choose road endpoints/necessary route intent;
5. explore/fight and decide which rare physical loot to commit to trade/crafting/progression;
6. late-game civil work reuses the construction palette and must not become a separate management game.

Fixed controls:

- `B`: settlement/infrastructure palette;
- `R`: rotate ordinary building placement;
- `Enter`: confirm the current building/road/outpost/civil-work step;
- `Backspace`: reset road start or civil-work first corner.

Avoid E/Q/F/number/Shift/Ctrl/Space/chat/camera conflicts. Do not proliferate N/J/K or one new key per feature.

Physical intent remains preferred over menus:

- market: relic deliberately placed in market barrel;
- workshop: damaged external weapon deliberately placed in service barrel;
- advanced forge/reforge: weapon + relic deliberately placed in commission barrel, real metal fetched by specialist;
- waterfront trade: fish deliberately placed in the dedicated trade barrel;
- coast/river and dangerous-region roles are inferred from loaded world conditions;
- Alpha.51 civil work is `B palette -> first corner -> second corner -> server approval`, not a terraforming dashboard.

Do not add tax rates, dozens of happiness stats, family schedules, per-worker priority tables, manual hauling routes or giant research screens.

## 3. Multiplayer authority

One world/server has one shared settlement and one shared infrastructure/project state.

- resources/buildings/population/roads/outposts/progression/civil work are shared;
- clients submit bounded requests and render synchronized state;
- Alpha.58 explicitly keeps all play payload handlers on NeoForge `HandlerThread.MAIN`, so simultaneous player requests are serialized before mutation;
- the server revalidates building/road/outpost/civil-work requests before mutation;
- founded-world login refreshes common physical storage once and republishes the same authoritative snapshot to all connected players;
- client logout clears cached settlement/context/placement/notice state so another world/server cannot inherit UI state;
- only one building/road/outpost/civil construction project may occupy the shared construction authority at once;
- Alpha.59 makes that invariant service-local rather than UI-local: every building/road/outpost/civil preview/start path calls the same `SettlementProjectAuthority.anyActive` gate before mutation;
- Alpha.45 exploration metadata is shared, non-spendable and deduplicated;
- civil `earthBank` is project-local relocation accounting only and cannot be spent outside the active civil project;
- imported civil fill is never a number ledger: only real DIRT/COARSE_DIRT ItemStacks in actual storage/worker hand have authority;
- no per-player settlement or internal politics/tax layer in planned scope.

### Alpha.61 outpost grade-cell transaction hardening

- the existing outpost grading envelope and save phase remain unchanged;
- each grade cell snapshots all changed BlockStates;
- every clear/support/final-grade `setBlock` must succeed;
- an unloaded required cell or failed placement rolls back successful partial changes in reverse order;
- persisted outpost construction step advances only after the full grade cell succeeds;
- grading still creates no loose drops, virtual soil, resource refund or second construction authority;
- no save schema/key/building/worker/companion change.

### Alpha.60 ordinary construction transaction hardening

Alpha.60 aligns the oldest ordinary-building path with the physical transaction guarantees already used by later road/outpost/civil systems.

- site-crate wood/stone availability is checked before world mutation;
- for a missing blueprint block: successful `setBlock` -> physical crate consume -> construction state advance;
- failed `setBlock` means zero material loss and zero step advance;
- unexpected consume failure after a new placement restores the prior block state before pausing;
- already-present correct blueprint blocks still pay the normal step delta, preventing player pre-fill from becoming a free-build exploit;
- Alpha.44 grading captures reversible snapshots for every cleared/filled block in one grade cell;
- any failed grade placement rolls the cell back before material/state commit;
- retaining stone commits only after the complete grade-cell world mutation succeeds, and unexpected consume failure rolls the cell back;
- final validation repair remains non-double-charged because those blueprint step costs were already committed before repair;
- no drop-producing excavation, resource refund minting, new save state, builder, queue or UI is added.

### Alpha.59 centralized single-project authority hardening

Alpha.59 is a multiplayer correctness pass, not a new construction feature.

- `SettlementProjectAuthority` reads building, road, outpost and civil active state from the two existing SavedData authorities;
- it creates no fifth project state, queue, reservation ledger or new save field;
- building preview/start, road preview/start, outpost preview/start and civil preview/start all reuse the same gate;
- outer network/command checks remain convenience feedback only and are not trusted as the final invariant;
- with Alpha.58 `HandlerThread.MAIN`, simultaneous confirmations are serialized and the later request rechecks this shared gate after the earlier request mutates state;
- stale client previews therefore cannot authorize a second concurrent shared project;
- one shared construction worker, physical ItemStack hauling, B/R/Enter/Backspace controls and all existing save formats remain unchanged;
- actual long-survival/two-client/reconnect runtime acceptance remains unfinished.

## 4. Founding and growth

Founding establishes:

- player-selected pioneer marker site;
- authoritative physical shared stockpile;
- one shared dedicated construction worker;
- settlement center/territory state;
- starter manual resource loop.

Growth tiers remain pioneer camp -> hamlet -> village -> frontier town -> domain.

Alpha.45 alternate exploration-accelerated routes remain additive and do not invalidate legacy routes.

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

The original target was roughly 15–20 meaningful families. Alpha.40–62 deepen systems rather than adding fake families.

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

## 6. Selected-area civil works — Alpha.49/50 history and Alpha.51 current

### Alpha.49 historical first pass

Alpha.49 first implemented the original v0.2 late-game `선택 영역 평탄화/절토/성토` in a deliberately bounded form:

- `DOMAIN` + construction office;
- B palette, first-corner grade Y, opposite-corner bounds, Backspace reset;
- **9×9 / cut-fill ±4**;
- selected corners within28 blocks and project center within80 blocks;
- full area loaded;
- infrastructure/block-entity/fluid/ore/player-structure/non-natural protection;
- shared construction worker only;
- project-local `earthBank` credited only after real no-drop cut and debited by real fill;
- `fill > cut` rejected;
- no force-load/teleport/free resource path.

That 9×9 balanced-earth rule is historical, not the current Alpha.50 capacity.

### Alpha.50 physical imported-fill foundation

Unlock/interaction remains:

- shared settlement `DOMAIN`;
- at least one completed construction office;
- no new functional BuildingType;
- no new key/dashboard;
- choose `토목 평탄화` from existing B palette;
- first corner fixes final grade Y;
- second corner defines X/Z bounds and requests server validation;
- `Backspace` resets first corner.

Bounds:

- maximum **13×13** footprint;
- max cut **5** blocks/column;
- max fill **5** blocks/column;
- selected corners within36 blocks of player;
- project center within96 blocks of settlement center;
- entire selected area already loaded.

Protection:

- reject overlap with authoritative stockpile, completed functional buildings, roads or outposts;
- reject block entities and fluids;
- reject ores, structures/player blocks and other non-natural terrain in affected columns;
- revalidate the current target column while work proceeds so later unsafe external changes pause instead of being buried/removed;
- active civil-work volume is protected from ordinary player block breaking;
- do not globally scan, force-load or generate chunks.

Earth/resource invariant:

- each successful real cut changes one natural world block to air with no item drops, then and only then credits one project-local earth unit;
- fill uses `earthBank` first;
- initial imported requirement is `max(0, fillBlocks - cutBlocks)`;
- approval of a project needing imported fill requires all shared settlement storage chunks loaded and enough actual DIRT/COARSE_DIRT ItemStacks;
- project-local `earthBank` persists for save/reload correctness but is not a resource ledger, ItemStack, cargo, currency or transferable balance;
- no virtual dirt/stone count is created.

Physical imported-fill sequence:

`actual shared storage -> shared construction worker walks to exact container -> extracts max16 real dirt/coarse-dirt into MAINHAND -> walks to site -> successful setBlock -> shrink carried ItemStack by1`

Rules:

- if real dirt is removed from storage after approval, construction pauses until physical supply returns;
- remaining imported demand is recalculated from current physical selected-area height and remaining `earthBank`, so pre-filled/external changes do not create a stale last haul;
- a failed `setBlock` never consumes carried dirt or advances project state;
- if completion is reached with a carried stack, project enters a persisted return phase; the worker walks to a concrete loaded settlement container and inserts there before the construction authority is released;
- if return storage is unloaded/full, the worker keeps the real stack and the project waits; there is no deletion or broad storage teleport;
- save/reload therefore cannot convert carried fill into a virtual balance or strand dirt as a blocker for the next construction project.

Worker/authority invariant:

- Alpha.50 reuses `SettlementConstructionService.ensureBuilder` and the existing shared `건설 주민`;
- no second builder entity or independent civil economy;
- building/road/outpost UI/network/command starts reject while civil work is active;
- compact status/context only presents phase, on-site earth, remaining imported fill and actual storage availability;
- `가상 토사 0`.

Alpha.50 established physical imported fill but did not yet implement retaining-heavy terraces.

### Alpha.51 current retaining-heavy terrace expansion

Alpha.51 keeps the same DOMAIN construction-palette interaction and one shared construction authority while expanding the bounded envelope to **17×17 / ±7**. Selected corners remain player-local (44 blocks) and the project center remains settlement-local (112 blocks).

Retaining rule:

- validation reserves the selected rectangle plus a one-block outer protection ring;
- the ring must already be loaded and may not contain Frontier infrastructure, block entities, fluids, player structures or other non-natural obstruction;
- after cut and before fill, only fill-facing edges whose final grade stands at least3 blocks above natural exterior ground receive retaining wall cells;
- retaining height is capped at7; deeper ravine edges are rejected rather than silently bridged;
- required retaining count is persisted in the civil state and contributes to project progress;
- exact `COBBLESTONE` ItemStacks are the material authority for the wall;
- approval checks actual loaded settlement storage for the full initial retaining requirement;
- the existing shared construction worker walks to an exact storage container, extracts max16 cobblestone into MAINHAND, walks to the exact wall cell, successfully places cobblestone, then shrinks the carried stack by1;
- failed placement or mid-project cobblestone depletion pauses without item/step loss;
- carried leftovers use the same physical return-to-concrete-storage path before material mode changes or project authority clears;
- phase order is `cut -> retaining -> fill -> return`, with Alpha.50 phase numeric meanings preserved for save compatibility and `PHASE_RETAIN=3` added;
- no generic stone ledger may mint cobblestone, and no second builder/economy, force-load, teleport, `destroyBlock` or `dropResources` path exists.

Alpha.51 completes the first retaining-heavy large-terrace slice. **Ravine-scale crossings, long bridges, tunnels and monumental engineering remain unfinished.** Mountain deletion/unrestricted WorldEdit is outside product scope.

## 7. Citizens, production and military

Implemented/service roles include builder, logger, farmer, quarry worker, miner, fishing worker, waterfront trader, workshop artisan, advanced forging specialist, construction supply runner, guards, barracks soldiers, dangerous-region outpost sentry, market presentation and road-bound transporter.

Loaded areas should show actual movement/work. Family/children simulation is outside planned scope.

Defense separation:

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
- **위험지역 군사 역할이 우선**;
- danger loss causes stand-down/return rather than deletion.

### Alpha.48 humanoid military presentation

- `FrontierSoldierEntity` extends `IronGolem` and inherits proven server AI/combat attributes;
- client renders a humanoid player-shaped soldier;
- **visual service sword is never a server ItemStack**;
- old loaded tagged Iron Golem barracks soldiers/sentries migrate **1:1**, preserving assignment/name/tags/health without recruitment consume;
- food/metal costs, military slots and drop protection remain unchanged;
- no hard Better Combat/Weapons Expanded Java dependency.

At Alpha.48 the physical external-weapon armory/loadout loop was unfinished. Alpha.57 covers loaded town-barracks soldiers with actual MAINHAND ItemStacks and automation, and Alpha.62 extends that same physical rule to remote sentries through the existing road-bound reverse-supply transporter.

### Alpha.69 historical duplicate-assignment containment

Alpha.69 hardens saves that may already contain more than one physical resident with the same specialist/outpost assignment tag. Alpha.67/68 prevent the known new false-missing paths, but they did not erase historical duplicates and the previous specialist population counters observed only one worker per assignment.

- workshop, advanced-workshop and outpost assignment queries expose the complete loaded matching-worker set;
- shared population reconciliation counts every unique physical assigned resident under the existing full loaded-evidence gates;
- replacement remains authorized only by **zero** matching workers, so two-or-more residents never consume another food4;
- actual specialist/transport work still uses exactly one worker, chosen deterministically by UUID order;
- ordinary production residents remain pooled and unchanged;
- existing exact MAINHAND death recovery remains unchanged; Alpha.69 does not delete, discard, retag or refund a duplicate resident or its cargo;
- no duplicate ledger, UUID reservation authority, virtual population/cargo, force-load, teleport, new worker family, key, UI or second logistics authority is added.

This is non-destructive containment, not historical-save cleanup. Long two-player repeated-death/night-rest/save-reload/reconnect runtime acceptance remains unfinished.

### Alpha.68 rest-anchor-aware civilian lifecycle evidence

Alpha.68 closes a deterministic hole left between Alpha.66/67 lifecycle evidence and the already-existing resident night routine.

- `SettlementResidentRoutineService` intentionally sends lumber/farm/quarry/mine residents and normal workshop artisans to completed HOUSE rest slots at night; a town-side outpost transporter within the existing town-rest radius may also sleep at a HOUSE;
- before Alpha.68, local worker absence evidence covered work↔storage but not those real HOUSE destinations, while workshop/advanced lookup used an unrelated fixed 192-block AABB; therefore a legitimate resident sleeping in an unloaded house could disappear from the loaded entity query and be misread as dead/missing;
- `SettlementWorkerService.workerRouteBounds` is now the shared local-civilian lifecycle envelope: work center + every concrete settlement storage endpoint + every completed HOUSE footprint, all with the existing bounded work/path margin;
- `workerRouteEvidenceLoaded` checks every chunk intersecting that exact envelope with `hasChunkAt` only, and workshop/advanced-workshop assignment lookup now queries the same envelope rather than a separate 192-block radius;
- ordinary production workers remain pooled/automatic rather than manually assigned: their name lookup is the UUID-deduplicated union of the exact per-building lifecycle envelopes that must also be loaded before population reconciliation/replacement;
- normal workshop spawn now independently rechecks the same complete lifecycle evidence and service-crate chunk before entity insertion, preserving Alpha.64 `entity add -> real food4 -> population +1` commit ordering;
- `SettlementOutpostLogisticsService.routeBounds` now also includes completed HOUSE footprints before its existing 32-block search margin, so a transporter intentionally resting in town remains inside the same lookup/evidence authority as its persisted road;
- normal work navigation, transporter road waypoints, Alpha.42 debt pacing, Alpha.63 exact MAINHAND cargo death recovery and Alpha.67 fail-closed assignment replacement are unchanged;
- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, Alpha.27 remains the **single authority for outpost transport**, and **there is still only one authority for long-distance outpost transport**;
- no new SavedData, UUID reservation ledger, manual resident schedule/job UI, worker family, virtual resident/cargo balance, force-load, teleport or companion dependency is added.

This closes the deterministic `normal night rest -> house unload -> false missing -> food-funded duplicate replacement` path in static authority. **Long two-player repeated-death/night-rest/save-reload/reconnect runtime acceptance remains unfinished.**

### Alpha.67 fail-closed outpost transporter assignment evidence

Alpha.67 closes the same unloaded-entity false-absence class for road-bound outpost transporters that Alpha.66 closed for local civilians, without changing the physical transport authority itself.

- `findAssignedWorker` and legacy-worker discovery search the persisted road/stockpile `routeBounds` with the existing 32-block route-search margin;
- population reconciliation, loaded transporter counting, missing-assignment inference, legacy reassignment and replacement spawn now require the **exact transporter lookup envelope** to be loaded before absence is authoritative;
- the assignment proof checks every X/Z chunk intersecting that same `routeBounds` AABB using `hasChunkAt` only; it never generates or force-loads a chunk;
- if any lookup-envelope chunk is unloaded, Frontier freezes absence/replacement instead of treating a hidden tagged transporter as dead and consuming food4 for a duplicate assignment;
- normal transporter work deliberately continues to use the existing persisted-road `routeFullyLoaded`/waypoint `hasChunkAt` checks, so **Transport workers belong to a specific outpost** and **pause at unloaded route boundaries** exactly as before;
- Alpha.42 deferred logistics pacing is not widened into a force-loaded simulation and Alpha.63 exact MAINHAND cargo death recovery remains the sole transporter death-cargo authority;
- the dangerous-outpost sentry keeps its separate loaded-area proof whose 32-block evidence margin already matches its 32-block sentry lookup radius;
- Alpha.27 remains the **single authority for outpost transport** and **there is still only one authority for long-distance outpost transport**;
- no new SavedData field, transporter UUID ledger, reservation, route controller, worker family, key, UI, currency, virtual cargo, force-load, teleport or companion dependency is added.

This closes the deterministic `route centers loaded -> transporter in an unloaded lookup-margin chunk -> false missing -> duplicate replacement` path. **Long two-player repeated-death/save-reload runtime acceptance remains unfinished.**

### Alpha.66 loaded-evidence-safe civilian lifecycle authority

Alpha.66 makes population/replacement decisions depend on complete physical visibility rather than interpreting unloaded entities as deaths.

- ordinary lumber/farm/quarry/mine replacement and population reconciliation require a bounded loaded chunk envelope from each work site to every concrete settlement-storage endpoint; the check uses `hasChunkAt` only and never loads a chunk;
- workshop and advanced-workshop assignment evidence uses the same loaded work-to-storage rule, while outpost transport keeps its persisted-road `allRoutesLoaded` authority;
- incomplete evidence freezes reconciliation/replacement instead of decrementing population or deliberately spawning a second worker for a merely unloaded resident;
- advanced-forging specialists move under the existing civilian housing/food authority: a genuinely missing loaded assignment is added successfully first, then real food4 is consumed, then shared population increments;
- old advanced artisans remain valid physical residents with their existing entity/assignment tags and are counted when evidence is complete; Alpha.66 never retroactively charges their original spawn;
- the old `SettlementService` free advanced-artisan spawn is removed, so there is one civilian arrival transaction path rather than a special zero-food bypass;
- Alpha.65 local death recovery now recognizes `ADVANCED_WORKER_TAG`, preserving the exact real metal stack in MAINHAND once on death and minting nothing for an empty hand;
- no new SavedData, virtual resident reservation, family simulation, direct-management UI, force-load, teleport or second logistics authority.

This closes deterministic unloaded-resident false-death/duplicate-replacement and advanced-artisan lifecycle gaps. Long two-player repeated-death/save-reload runtime acceptance remains unfinished.

### Alpha.65 local civilian physical-cargo death boundary

Alpha.65 extends the existing no-loss physical ItemStack rule to local production/workshop civilians without creating a new resource authority.

- new lumber/farm/quarry/mine workers carry `frontier_settlement_resource_worker`; legacy workers remain recognized through their exact pre-existing Frontier custom names;
- workshop artisans reuse `frontier_settlement_workshop_worker`; no new workshop assignment state exists;
- a managed local civilian death clears ambiguous vanilla equipment drops and emits exactly one copy of its current MAINHAND stack; empty hand emits zero;
- this preserves only cargo that physically existed in that worker's hand: harvested resources and already-extracted workshop metal; recruitment food is not refunded and already-deposited resources are not recreated;
- outpost transport workers are excluded and remain solely under Alpha.63's transporter recovery handler, so the same entity cannot be recovered by two Frontier authorities;
- active public-works builders keep their existing invulnerable project lifecycle and are not turned into another death/recovery subsystem;
- no SavedData field, recovery ledger, virtual inventory, currency, force-load, teleport, new worker family or management UI is added.

The physical rule is therefore consistent across Frontier entities that can own the only live copy of a carried settlement ItemStack, while repeated-death/reconnect runtime acceptance remains unfinished.

### Alpha.64 atomic worker-arrival transaction

Alpha.64 applies the same physical commit discipline to the existing food-funded civilian arrival paths.

- the 4-food arrival cost is unchanged and still comes only from loaded concrete shared storage;
- ordinary building workers, workshop artisans and road-bound outpost transporters must be successfully added to the server world before food/population commit;
- failed `addFreshEntity` means no food loss and no population increment;
- successful spawn followed by an unexpected food-consume failure discards only that new worker and leaves population unchanged;
- workshop/outpost assignment spawners recheck a current assignment immediately before spawn, closing stale missing-assignment observations without introducing a reservation ledger;
- transport cargo is not recreated by replacement; Alpha.63's physical world recovery drop remains the only failure-boundary cargo recovery;
- save format, job families and route state are unchanged;
- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, and Alpha.27 remains the **single authority for outpost transport**; **there is still only one authority for long-distance outpost transport**.

This is pre-acceptance correctness hardening, not proof of repeated-death/reconnect runtime acceptance.

### Alpha.63 transporter transaction hardening

Alpha.63 closes two deterministic acceptance-edge gaps inside the existing long-distance authority.

- a military external weapon carried under `MILITARY_SUPPLY_TRIP_TAG` is revalidated at the destination immediately before insertion;
- if `weaponSupplyShortage(...)` is already zero, the exact carried weapon remains in transporter MAINHAND and only the supply trip tag is removed; the next existing normal freight step returns it to concrete town storage;
- therefore a player/manual stock change or sentry armament during a long trip cannot create an unintended second remote reserve weapon;
- tagged transport-worker death clears ambiguous vanilla equipment drops and emits exactly one copy of the currently carried MAINHAND ItemStack for physical recovery;
- empty-handed transporter death emits no cargo and cannot mint items;
- the death handler is registered once on the common NeoForge event bus and does not alter ordinary villagers;
- entity persistence continues to own transporter assignment tags and MAINHAND cargo; no new save field, virtual cargo, refund balance or recovery ledger exists;
- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, **군사 전초도 같은 도로 운송자가 역방향 보급**, and **위험지역 군사 역할이 우선** remain unchanged;
- Alpha.27 remains the **single authority for outpost transport** and **there is still only one authority for long-distance outpost transport**;
- no new worker/trip family/route controller/building/key/UI/currency, no force-load/teleport, and no hard companion dependency.

This is static transaction hardening, not a substitute for route-unload/save-reload/two-player real-play acceptance.

### Alpha.62 road-bound remote-sentry physical armament

Alpha.62 implements that remaining remote slice through the existing Alpha.27/41 transport authority rather than adding a remote armory or second carrier system.

- only a loaded active dangerous general outpost with an existing empty-MAINHAND sentry can have weapon demand;
- an external weapon already in sentry MAINHAND or already staged in that exact outpost stockpile makes weapon demand zero;
- the existing military reverse-supply choice is ordered food reserve -> metal reserve -> one external weapon, so **위험지역 군사 역할이 우선** includes survival provisioning before upgrade cargo;
- `SettlementOutpostLogisticsService` reuses the same `MILITARY_RETURN_TRIP_TAG` / `MILITARY_SUPPLY_TRIP_TAG`, assigned transporter, persisted road and physical MAINHAND cargo; no weapon-specific long-distance state exists;
- the transporter extracts exactly one recognized external weapon from concrete loaded shared settlement storage, walks the road, and inserts the same ItemStack into the outpost stockpile;
- the sentry never reads town storage. When there is no current combat pressure it walks only the local final leg to its own stockpile and extracts exactly one real weapon into vanilla MAINHAND;
- active combat preempts local armament movement;
- sentry death clears service/body drops but re-adds the exact equipped external weapon once, matching the no-mint recovery rule used by town barracks;
- role loss or route unload never converts the weapon to a number: existing physical worker MAINHAND and route-pause/return behavior retains authority;
- **군사 전초도 같은 도로 운송자가 역방향 보급**, **Transport workers belong to a specific outpost**, and they **pause at unloaded route boundaries**;
- Alpha.27 stays the **single authority for outpost transport** and **there is still only one authority for long-distance outpost transport**;
- no new save field/trip tag/worker/building/key/UI/currency, no force-load/teleport, and no hard Weapons Expanded or Better Combat Java dependency.


### Alpha.55 exploration knowledge -> existing outpost value

Alpha.55 extends Alpha.45 without creating a second progression or reward authority.

- `surveyLevel = min(3, unique external structure types)`;
- `conquestLevel = min(2, unique defeated conquest target types)`;
- survey level only biases the existing loaded local specialization evidence: ore +0/0/1/1, logs +2/level, field ground +8/level, exposed stone +2/level; it never spawns or credits resources;
- conquest level reduces only the physical material total for newly built outposts: wood `72 - 4*level`, stone `48 - 2*level`, minimum64/44;
- those effective totals are used by placement approval and by the existing builder's actual ItemStack extraction/consumption math, so the discount cannot become a virtual refund or free construction;
- outpost physical placement is atomic: successful world `setBlock` precedes carried wood/stone consumption and state advance, with rollback on unexpected consume failure;
- Alpha.26+ physical outpost final repair fetches and consumes a real wood/stone item for missing priced blueprint cells; historical prepaid saves remain repair-cost exempt to avoid double charging;
- benefits are deterministic from Alpha.45 persisted unique-ID lists; old saves need no migration field and repeated IDs remain non-farmable;
- exploration observation remains loaded-only and never locates/generates external content;
- no free loot, population, abstract survey currency, new UI tree, second economy or second transport authority;
- **builder walks from actual settlement storage carrying real wood/stone stacks** remains true;
- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, and Alpha.27 is the **single authority for outpost transport**; **there is still only one authority for long-distance outpost transport**.

### Alpha.58 multiplayer pre-acceptance hardening

Alpha.58 is a bounded correctness pass before real two-player acceptance, not a claim that multiplayer acceptance is finished.

- `SettlementData` and civil SavedData remain world/server shared, never keyed per player;
- `PayloadRegistrar` explicitly executes play handlers on `HandlerThread.MAIN`; building/road/outpost/civil confirms therefore serialize through one game-thread authority and every start path still revalidates current state;
- founded-world login runs one physical storage refresh then broadcasts one current snapshot to **all** connected players, avoiding a join-triggered ledger update that only the joiner sees;
- client `LoggingOut` resets settlement snapshot/context initialization, cancels all construction placement modes and clears transient settlement notices;
- reconnect/server-switch cannot generate notices by comparing unrelated old/new settlement contexts;
- no new protocol payload, per-player resource cache authority, settlement duplication or async world mutation is introduced.

Remaining acceptance is intentionally real-play: two clients, one shared settlement, simultaneous requests, long hauling/construction, disconnect/reconnect and save/reload under the full candidate companion stack.

### Alpha.57 automated physical barracks armory

Alpha.57 deliberately uses the existing barracks + shared-storage authorities. It does not create a new armory BuildingType, equipment currency, soldier-management UI or remote logistics path.

- loaded town barracks soldiers only in the first slice;
- no active barracks threat -> unarmed soldier may seek equipment; defense always has priority;
- shared settlement storage must be fully loaded and must physically contain a recognized external weapon;
- soldier itself walks to the nearest concrete weapon-containing storage within160 blocks and only extracts at <=3-block interaction range;
- extraction count is exactly1; no copy/mint/free fallback server ItemStack;
- actual weapon becomes the soldier's vanilla MAINHAND equipment, preserving the original ItemStack components/damage/enchantments through vanilla entity persistence/sync;
- client renderer shows that synced physical weapon; Alpha.48 service sword remains only the client fallback when no physical upgrade exists;
- barracks death drops are still cleared except the exact assigned external weapon, which is re-added once for recovery;
- loaded armed count is compact status only;
- remote military sentries remain generic until actual weapon cargo can reuse the same road-bound transporter. **군사 전초도 같은 도로 운송자가 역방향 보급** remains the only acceptable remote extension;
- no hard Weapons Expanded/Better Combat class dependency, force-load, teleport, new worker or second logistics authority;
- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, Alpha.27 stays the **single authority for outpost transport**, and **there is still only one authority for long-distance outpost transport**.

### Alpha.56 common-biome-tag outpost specialization

Alpha.56 uses only `net.neoforged.neoforge.common.Tags.Biomes` against the already-loaded center biome. There is no Terralith/worldgen Java dependency or biome-ID allowlist.

- physical local evidence remains primary: mining threshold4 ore, lumber24 logs, agriculture120 field-ground, quarry24 exposed stone;
- forest/dense vegetation adds8 log evidence;
- plains/savanna adds24 field evidence;
- mountain/hill adds8 exposed-stone +1 ore evidence;
- badlands/sandy adds6 exposed-stone evidence if not already mountain/hill;
- all biome biases stay below their thresholds, so tags only resolve plausible borderline sites and never create resources or guarantee specialization by themselves;
- the common tag seam lets compatible datapacks/worldgen participate without Frontier importing their class, registry ID or assets;
- unloaded center means zero biome bias; no chunk generation or force-load;
- no new specialization family/save state/economy/worker/logistics authority;
- Alpha.55 knowledge and Alpha.56 biome context are both computed helpers, not spendable resources;
- **Transport workers belong to a specific outpost**, **pause at unloaded route boundaries**, Alpha.27 remains the **single authority for outpost transport**, and **there is still only one authority for long-distance outpost transport**.

## 8. Resources and logistics

Resources remain physical Minecraft items.

- workers deposit real ItemStacks;
- construction/crafting/trade consumes real ItemStacks where those systems require resources;
- avoid every-tick arbitrary player chest scanning;
- common/additive tags let compatible external materials participate;
- service/commission/trade barrels are not generic storage unless explicitly specified;
- Alpha.45 exploration score, Alpha.55 survey/conquest knowledge, Alpha.56 biome context and civil `earthBank` are not spendable resources.

**Transport workers belong to a specific outpost**, follow persisted road-network waypoints, carry actual cargo and **pause at unloaded route boundaries** instead of teleporting or force-loading.

Alpha.27 tagged road logistics remains the **single authority for outpost transport** at every tier. **There is still only one authority for long-distance outpost transport.**

The invariant remains: **there is still only one authority for long-distance outpost transport**.

Cart station:
- road-adjacent physical freight hub;
- four freight barrels;
- same outpost transporter owns route;
- batch16→32;
- no second route/economy authority.

Fishing/waterfront:
- loaded shoreline fishing produces real cod/salmon into outpost stockpile;
- existing road logistics moves it;
- Alpha.46 physical landing is built from real local wood;
- local wood shortage may be reverse-supplied by same transporter;
- dedicated trade barrel consumes16 real cod/salmon and outputs1 real emerald;
- ordinary outpost stock never auto-sells;
- no boat logistics/virtual trade balance.

Military reverse supply:
- **군사 전초도 같은 도로 운송자가 역방향 보급**한다;
- same transporter returns to town and carries food/metal to an active military outpost;
- military food/metal has precedence over waterfront wood reverse supply;
- no abstract supply points or duplicate transporter.

Alpha.42 unloaded catch-up:
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
- Alpha.52 bounded straight long-water/dry-ravine bridge runs max24 with persisted physical stone pier cells;
- persisted outpost construction;
- lumber/quarry/mining/agriculture specialization;
- loaded fishing overlay;
- loaded dangerous-region military overlay;
- bounded unloaded catch-up;
- Alpha.46 physical waterfront landing/trade.

**tier-visible public works** may improve territory readability only when safe, loaded, non-farmable and non-destructive to player work.

Still partial: deeper companion-biome specialization and larger road/civil engineering.


### Alpha.52 bounded long-bridge / ravine crossing

Alpha.52 remains inside `SettlementRoadService` and the existing road construction state. It does not create a civil-work duplicate or another logistics controller.

- Alpha.35 short-water bridges remain max6 centerline cells without new pier state;
- straight water crossings and abrupt dry ravines may use bridge profile for at most24 centerline cells;
- dry ravines require at least4 blocks of bounded depression and nearly level shoulders;
- runs needing structural support persist exact pier cells in optional `bridge_supports`, default empty for old saves;
- pier stations use two edge columns and each column must reach natural support within12 blocks;
- loaded block entities, non-water fluid, player/non-natural obstruction or excessive depth reject the route;
- pier-required bridges are village-stage public works;
- deck and pier stone remain real ItemStacks hauled by the same road builder from actual settlement storage;
- placement is atomic with resource authority: successful world `setBlock` happens before carried-stone shrink/state advance, and a failed consume rolls the placed block back;
- final validation/repair for Alpha.25+ physical roads also requires physical stone instead of free repair placement; historical prepaid road saves remain cost-free at repair because their stone was already charged before persistence;
- completed roads still become the same `RoadSegment`, so Alpha.27 remains the **single authority for outpost transport** and there is still only one authority for long-distance outpost transport;
- no force-load, teleport, virtual stone, second builder or second route authority.

This is the first long-bridge/ravine slice only. **Tunnels and more complex/deeper monumental crossings remain unfinished.**

### Alpha.53 bounded straight road tunnels

Alpha.53 stays inside the same road endpoint/preview/approval authority. It does not add a tunnel UI, new key or a second civil-work controller.

- a loaded straight ridge/cliff may become a tunnel run of at most24 centerline cells when entry/exit shoulders differ by at most1 and every covered center remains at least4 blocks above final tunnel floor;
- tunnel profile value `PROFILE_TUNNEL=2` is saved in the existing optional profile list; legacy profile values and bridge support saves remain compatible;
- phase encoding preserves old meanings: prepaid<1M, grading1M..<1.5M, tunnel excavation1.5M..<2M, physical paving2M+;
- exact excavation geometry is deterministic from persisted centers/profile: width3, clear height3 above floor;
- server validation rejects unloaded cells, block entities, all fluids, ores, caves/air pockets and non-natural/player blocks in affected tunnel volume;
- the shared road builder advances from the previous opened floor and removes one validated natural cell at a time with no item drop; successful world mutation precedes state advance;
- excavated stone is never credited to storage, earthBank, HUD or any virtual resource;
- active excavation volume is project-protected; unsafe mid-project edits pause;
- final road surface still uses the established physical ItemStack hauling/placement/consume path, with one bounded stone surcharge per tunnel center;
- tunnel automatic public works require frontier-town + construction office;
- completed tunnel road remains an ordinary RoadSegment, so Alpha.27 remains the **single authority for outpost transport** and **there is still only one authority for long-distance outpost transport**;
- no force-load, teleport, `destroyBlock`, `dropResources`, second builder/economy/logistics authority or hard companion dependency.

Alpha.54 adds one bounded 90-degree bend and physical portals. >24-cell bores, underground stations and unrestricted mountain deletion remain outside the intended product.

### Alpha.54 bounded one-bend tunnel / physical portal pass

Alpha.54 is deliberately qualitative rather than a larger WorldEdit envelope. The total tunnel run remains max24 cells, while one persisted Manhattan bend and physical portal presentation are added inside the same road authority.

- max tunnel run remains24 centerline cells; no larger excavation cap;
- at most one90-degree centerline bend is accepted, with at least3 tunnel centers on both legs around the turn;
- bend geometry is reconstructed only from persisted road centers + `PROFILE_TUNNEL=2`, so old saves and Alpha.53 phase encoding remain compatible;
- tunnel interior remains width3 / clear height3 and keeps the same loaded natural non-ore/non-fluid/no-cave/no-player-block safety contract;
- each tunnel run deterministically owns two 5-wide × 4-high stone-brick portal frames;
- portal cells are prevalidated for loaded/block-entity/fluid/non-natural/infrastructure overlap and are included in the no-drop tunnel excavation plan;
- the existing road builder physically advances through excavation, then hauls real settlement stone and places `STONE_BRICKS` portal cells through the same paving authority;
- each run adds22 real-stone portal units; excavation never mints replacement stone, earthBank or currency;
- active tunnel interior, floor and portal cells are project-protected;
- successful road/portal placement precedes carried-stone consumption/state advance, retaining Alpha.52/53 physical authority;
- completed tunnel is still one ordinary `RoadSegment`: Alpha.27 remains the **single authority for outpost transport** and **there is still only one authority for long-distance outpost transport**;
- **Transport workers belong to a specific outpost** and **pause at unloaded route boundaries**;
- no new key/building/currency/dashboard, second builder/economy/logistics authority, force-load, teleport or hard companion dependency.

This closes the first bounded straight + single-bend tunnel breadth. Very-long bores, underground stations and unrestricted mountain deletion are not required to call the original road/civil loop functionally broad; real-play acceptance still governs release readiness.

## 10. Exploration, crafting and settlement feedback

Frontier does not own the full adventure-content layer. External structures/dungeons/mobs/bosses/loot remain companion authority.

Alpha.45 exploration/conquest progression:
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

Market/workshop/advanced crafting role split:

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
- locked Xaero26.4.2 lacks the historical public `WaypointsManager` API;
- do not fake completion through internal waypoint sets/reflection/mixins;
- true settlement/outpost/road marker synchronization remains deferred until a stable supported seam exists.

Alpha.54 road/civil work reads already-loaded block state/heightmap and loaded physical storage only. It adds no Terralith/worldgen hard dependency.

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
- rare exploration messages + `/frontier status`, not a quest dashboard;
- physical barrels encode market/workshop/waterfront intent;
- civil grade-plane outline is world-space placement feedback, not a new screen;
- Alpha.51 civil detail adds only `현장 토사 / 외부 흙 필요 / 실제 창고 흙 / 옹벽 잔여 / 창고 조약돌` presentation.

Do not invent giant generic dashboards when a physical/world-space interaction can carry the same intent.

## 13. Engineering rules

Target:

- Minecraft Java26.2;
- NeoForge26.2.0.38-beta;
- Java25;
- Gradle9.2.1.

Development sequence:

`read current GitHub main -> inspect ORIGINAL_DESIGN + CANONICAL_PLAN + gap audit + source + CI -> implement -> manual code/gameplay audit -> cumulative source audit -> canonical docs audit -> Java25 clean build -> JAR verify -> direct main update -> canonical exact-SHA CI`

Shared repo:

- re-read remote main before writes;
- no force push;
- do not revert unrelated concurrent-project commits;
- Frontier path changes only except its workflow/result files;
- CI result bot may advance main;
- final accepted result must identify exact intended Frontier **source/docs SHA**, result commit, run ID and JAR SHA-256.

## 14. Current playable slice after Alpha.68

Current implemented slice includes:

- one shared authoritative settlement;
- founding stockpile/builder/civic progression;
- compact B/R/Enter/Backspace interaction;
- exactly15 functional buildings;
- physical construction/hauling and bounded Alpha.44 building terrain;
- construction-office staging;
- loaded production and resident work;
- physical roads/stairs/short bridges, Alpha.52 bounded long bridges, Alpha.53 straight tunnels, and Alpha.54 bounded one-bend tunnels with physically built stone-brick portals;
- specialized outposts, fishing, dangerous military overlay;
- same-authority physical road logistics and reverse supply;
- bounded unloaded work debt without virtual resources/cargo;
- waterfront real-wood landing and opt-in physical fish trade;
- compact HUD/notices/Jade context and Xaero collision avoidance;
- unique external structure/conquest progression bridge;
- market, repair, first advanced forge and DOMAIN reforge;
- supplied humanoid military presentation with unchanged physical recruitment economics;
- Alpha.57 loaded town-garrison physical external-weapon armament from shared storage, with exact weapon recovery;
- Alpha.58 shared-login snapshot rebroadcast + explicit MAIN-thread request serialization + client session reset pre-hardening;
- Alpha.59 centralized service-level single-project authority for building/road/outpost/civil preview and start;
- Alpha.60 rollback-safe ordinary building placement + Alpha.44 grade-cell physical material transactions;
- Alpha.61 rollback-safe outpost grade-cell terrain mutation before persisted step advance;
- Alpha.62 same-road-transporter remote military external-weapon delivery -> local sentry MAINHAND equip -> exact death recovery;
- Alpha.63 in-flight military weapon demand revalidation + exact transport-worker carried-ItemStack death recovery;
- Alpha.64 atomic food-funded ordinary/workshop/transporter arrival commit with assignment recheck and no failed-spawn charge;
- Alpha.65 exact local production/workshop civilian MAINHAND death recovery with legacy-name compatibility and transporter double-recovery exclusion;
- Alpha.66 loaded-evidence-safe local civilian population/replacement authority + food-funded advanced-artisan lifecycle;
- Alpha.68 rest-anchor-aware local civilian/transporter lifecycle evidence matching actual work/storage/HOUSE routine destinations;

- Alpha.67 fail-closed outpost transporter assignment evidence matching the exact transporter lookup envelope, without changing normal route-bound physical movement;

- **Alpha.51 DOMAIN 17×17 / ±7 selected-area cut/fill with Alpha.50 earth/imported-dirt authority plus bounded 3–7 block exposed-edge retaining walls made from exact physically hauled COBBLESTONE**.

This is not original v0.2 completion.

## 15. Unfinished original-scope priorities after Alpha.68

Unless real-play regression overrides them:

1. long survival + two-player multiplayer acceptance; Alpha.58–59 close deterministic state/exclusivity holes but do not satisfy this runtime item;
2. Alpha.62–68 physical military/transporter/local-civilian cargo recovery and replacement boundaries are statically hardened; Alpha.67 fails closed on transporter lookup-envelope unload and Alpha.68 includes real HOUSE night-rest anchors in civilian/transporter absence evidence; save-reload, route-unload, night-rest, repeated death/replacement and no-dup/no-loss acceptance remain;
3. rare-NPC-specific settlement value only if a stable soft data seam appears; generic biome-aware specialization is covered by Alpha.56;
4. optional deeper monumental crossings only if real play shows Alpha.52–54 breadth is insufficient; never expand by default into WorldEdit-scale civil works;
6. Alpha.42 catch-up pacing/save-reload/exploit acceptance;
7. Alpha.43 Jade/Xaero/HUD visual/runtime acceptance;
8. Alpha.46 waterfront pathing/site/reverse-supply/trade-balance acceptance;
9. Alpha.48 humanoid render/attack-animation + legacy migration acceptance;
10. Alpha.51 civil-work pathing/save-reload/retaining-cobble depletion-resupply/return-cargo/terrain-safety acceptance;
11. Alpha.52 long-bridge pier planning/save-reload/stone depletion/physical repair/pathing acceptance;
12. Alpha.53 tunnel detection/excavation/save-reload/pathing/no-drop/player-protection acceptance;
13. Alpha.54 one-bend detection/corner clearance/portal excavation/22-stone physical portal/save-reload acceptance;
14. Alpha.56 common-biome-tag borderline specialization / companion-installed-and-absent acceptance;
15. Alpha.57 weapon storage→soldier walk/extract/save-reload/render/death-recovery/no-dup acceptance;
16. Alpha.58 two-client shared-login refresh, logout/server-switch reset and reconnect acceptance;
17. Alpha.59 simultaneous building/road/outpost/civil confirmation exclusivity acceptance;
18. Alpha.60 building setBlock failure/rollback + terrain retaining rollback/pre-fill-cost acceptance;
19. Alpha.61 outpost grade-cell failure/rollback + unload/save-reload acceptance;
20. full companion lock fresh-world client/server runtime;
21. true Xaero markers only if a stable supported API appears;
22. moving boat/waterborne merchant only if presentation value justifies it and it never becomes a second logistics authority.

Large mountain deletion, unrestricted WorldEdit-style terraforming, family simulation, giant research trees, tax/economic micromanagement and manual per-soldier management remain outside the intended product.

## 16. Real-play acceptance focus

At the final/test-worthy point verify at least:

- two players see one shared stockpile/build/project/progression state;
- founding and early vertical slice;
- ordinary small and Alpha.44 span3–4 construction terrain, including Alpha.60 no-loss failed placement and rollback-safe retaining transactions;
- unsafe/>4 building terrain rejection and no free retaining materials;
- construction office runner and builder physical hauling;
- road stairs/short bridge and outpost route movement, including Alpha.61 rollback-safe outpost grading;
- cart station freight fallback;
- fishing shoreline qualification and invalid-puddle rejection;
- dangerous-region military activation/supply/stand-down, including Alpha.62 food/metal-before-weapon priority, physical road weapon cargo, local equip and exact death recovery;
- unloaded debt cap/reload/no-offline-mint;
- Jade installed/absent boot and compact context;
- Xaero installed/absent HUD readability with marker feature still honestly absent;
- external structure/conquest unique-type dedupe and legacy tier routes;
- waterfront real wood build, fish cargo return, same-transporter wood supply, military supply precedence, break protection and16→1 dedicated trade;
- advanced forge and DOMAIN reforge no-loss compatibility;
- Alpha.48 humanoid render, attacks, drops and1:1 legacy migration;
- Alpha.51 B-palette unlock requires DOMAIN + construction office at server authority;
- first-corner grade, second-corner area, max17×17 and ±7 bounds;
- fluids/block entities/ores/player structures/existing infrastructure rejection;
- real cut creates no drops and only successful setBlock credits earthBank;
- local earth fills first;
- fill>cut succeeds only when the loaded common storage actually contains enough DIRT/COARSE_DIRT;
- builder visibly walks storage→site carrying real dirt/coarse dirt;
- mid-project dirt depletion pauses and later physical resupply resumes;
- failed placement does not consume carried dirt;
- final haul is limited to current remaining imported demand;
- early/pre-filled completion returns carried dirt physically before project clear;
- save/reload preserves phase/progress/earthBank/carry without duplication or loss;
- unloaded selected area/storage pauses instead of force-loading;
- no building/road/outpost/civil project starts concurrently, including simultaneous confirmations from two clients or direct command/service entry paths;
- retaining plan reserves one loaded outer ring, rejects non-natural/player obstruction, and rejects required wall height >7;
- exact cobblestone is checked at approval, physically hauled max16, consumed only after successful wall placement, and shortage pauses;
- save/reload preserves PHASE_RETAIN + initial retaining count without duplicating cobblestone;
- completed project leaves no spendable/transferable virtual earth or virtual stone;
- Alpha.52 long bridge: 7–24-cell bound, dry ravine depth trigger, straight support run, pier natural-support depth<=12, save/reload support stability, real-stone depletion/resupply and no-free-repair behavior;
- full companion-stack fresh world.

Real-play observations override assumptions. Fix root causes before adding more breadth when testing exposes a regression.
