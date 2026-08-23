# Frontier Settlement

Minecraft Java 26.2 / NeoForge 26.2 cooperative settlement-growth mod.

Canonical direction: see `CANONICAL_PLAN.md`.

## Current version: 0.1.0-alpha.28

Frontier Settlement currently provides a server-authoritative shared settlement vertical slice covering founding, building, population/workers, roads, outposts, tier growth and compact world-space controls.

### Shared settlement

- one shared settlement per world/server;
- pioneer marker survival founding;
- physical shared storage with cached wood / stone / metal / food HUD ledger;
- shared population, housing, buildings, roads and outposts;
- server-authored one-line next-goal guidance rather than a separate quest screen.

### Compact interaction model

Normal gameplay is unified around:

- `B` — settlement palette;
- `R` — rotate building placement;
- `Enter` — confirm active building/road/outpost placement;
- `Backspace` — reset the current road start selection.

Old fragmented N/J/K gameplay keys are not used. Important vanilla controls remain untouched.

### Buildings and workers

Functional building families currently include house, lumber camp, farm, quarry, mine, warehouse, blacksmith and guard post.

- building placement uses world-space validation and rotation;
- building unlocks follow settlement growth rather than one giant menu;
- custom worker roles handle logging, farming, quarrying, mining and transport;
- housing/food/workplaces drive population growth;
- vanilla villager profession trading is not the progression system.

### Safe construction rules

Construction rejects unsafe overlaps and unsuitable terrain instead of silently destroying player work.

- footprint height variation is limited;
- fluids, block entities, tree trunks and protected/non-natural obstructions reject the site;
- terrain preparation uses direct no-drop block updates rather than drop-producing destruction;
- foundations are supported;
- roof blocks are ordered after structural support;
- completed enclosed buildings include deliberate windows and lighting;
- a newly introduced wrong obstruction pauses construction instead of being overwritten.

### Alpha.23 — physical building construction logistics

- approving a building validates the full physical wood/stone requirement but does not instantly delete the whole cost;
- a temporary physical construction supply barrel is created beside the work site;
- the builder walks to an actually loaded settlement storage container;
- the builder extracts a real matching item stack and visibly carries it in the main hand;
- carried resources are deposited into the site supply barrel in batches;
- construction waits until all currently required physical materials are staged;
- building cost is consumed gradually from the site barrel as blueprint steps are placed;
- if staged material disappears, work pauses/re-hauls instead of creating resources from nothing;
- active construction blocks and the supply barrel are protected from accidental breaking;
- leftover items are returned to settlement storage when possible.

### Alpha.24 — phase-readable building work

- construction guidance reports hauling, foundation, frame/walls, roof, interior/finish or final validation;
- the active builder is protected from ordinary damage while carrying/placing settlement construction resources, then returns to normal vulnerability when the job finishes;
- temporary wooden work towers provide closer high-wall/roof work positions when the surrounding site is safe;
- scaffold pieces are only claimed where every required position is replaceable and free of fluids/block entities;
- scaffold ownership is persisted in `ConstructionState`, so restart/reconnect recovery and cleanup do not infer ownership from visual shape;
- if all scaffold approaches are blocked, construction falls back safely rather than deleting an obstruction or deadlocking;
- the builder performs a visible main-hand swing when a blueprint block is placed;
- construction-barrel pressure handling returns removable surplus to settlement storage.

### Alpha.25 — physical road grading and stone hauling

- road approval validates the full stone requirement but no longer deletes it instantly;
- newly approved routed roads begin with a persisted worker grading phase before surface paving;
- the construction worker walks the planned 3-wide footprint, clears only route-safe material with no-drop updates, fills shallow validated support gaps, and establishes a walkable graded base;
- old active road saves from Alpha.24 remain compatible;
- after grading, the worker extracts actual stone-family stacks from loaded settlement storage in batches and visibly carries them to the active work front;
- the project stone cost is consumed gradually from the carried physical stack as road placements advance;
- surplus carried stone is returned to physical settlement storage before completion;
- active road transaction blocks are protected from break/rebuild exploits.

### Alpha.26 — physical outpost construction

- approval validates wood 72 / stone 48 without deleting it up front;
- a persisted grading phase walks the 9×9 site and performs only validated shallow earthwork;
- the builder then extracts actual wood/stone-family stacks from loaded settlement storage and carries them to the work front;
- blueprint material cost is consumed gradually from the carried physical stack;
- work pauses on shortages or new obstructions rather than generating resources or overwriting player changes;
- old pre-Alpha.26 active outpost saves remain on their prepaid construction path;
- completion detects lumber / quarry / mining / agriculture specialization and activates local production.

### Alpha.27 — road-bound outpost logistics

- every transport villager is permanently assigned to one outpost with settlement-owned entity tags and a visible `운송 주민 #<id>` identity;
- old generic transport villagers migrate without charging arrival food again;
- transport no longer pairs generic villagers with `outposts[i]` through UUID sorting;
- routes reconstruct from persisted `RoadSegment.centers()` and follow chained/branched roads toward the settlement;
- outbound and inbound trips advance through short road waypoints so L-corners remain meaningful;
- an unloaded next segment pauses the worker without force-loading it;
- only fully loaded routes are used for migration, missing-worker replacement and authoritative population reconciliation;
- stockpile extraction is specialization-filtered so unrelated player junk stays in the outpost chest.

### Alpha.28 — distinct physical outpost production

Alpha.28 makes specialization affect actual work cadence and local resource behavior instead of only changing the output item category.

- production only runs when the outpost and its required local chunks are already loaded; no production path force-loads remote territory;
- each production worker is persistently tagged to its own outpost, with legacy visible-name workers adopted into the tag identity instead of duplicated;
- lumber work is limited to nearby natural trees with leaf canopies, requires the worker to walk to the trunk, performs a visible swing, and removes at most 4 logs per roughly 5-second work cycle;
- quarry work is limited to exposed nearby stone, requires physical approach and a swing, removes at most 3 exposed blocks per roughly 4-second cycle, and no longer hollows hidden adjacent stone beneath intact ground;
- mining work now has a readable minehead cycle: the worker returns to the outpost work point and performs one finite underground ore extraction roughly every 8 seconds instead of mining continuously every half-second;
- mined ore remains physically depleted in the world by replacement with ordinary stone, so a mining site is not an infinite abstract generator;
- agriculture uses vanilla crop growth as the renewable specialization and harvests at most 4 mature wheat per roughly 6-second work cycle;
- the agriculture plot is initialized only from the pristine completed coarse-dirt floor; once established, missing crops/farmland are not magically recreated every production tick;
- already-created Alpha.27 agriculture plots remain valid, while a pristine legacy plot can be initialized once on load;
- full stockpiles naturally stall local production because workers keep carrying undelivered output instead of deleting or abstracting it.

The interaction budget remains B/R/Enter/Backspace. Alpha.28 changes simulation quality behind those controls, not player micromanagement.

### Validation

The canonical GitHub Actions workflow performs:

1. source audit;
2. Java 25 clean Gradle build;
3. runtime JAR verification;
4. deliverable artifact upload;
5. canonical CI-result recording back to `ci-results/frontier-settlement/`.

Real in-game visual quality, navigation behavior and pacing must still be judged through actual play/screenshots; CI cannot prove those presentation qualities.
