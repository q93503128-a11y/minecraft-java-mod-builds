# Frontier Settlement

Minecraft Java 26.2 / NeoForge 26.2 cooperative settlement-growth mod.

Canonical direction: see `CANONICAL_PLAN.md`.

## Current version: 0.1.0-alpha.27

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

Alpha.25 brings roads onto the same physical-work direction as buildings without adding new player micromanagement.

- road approval validates the full stone requirement but no longer deletes it instantly;
- newly approved routed roads begin with a persisted worker grading phase before surface paving;
- the construction worker walks the planned 3-wide footprint, clears only route-safe material with no-drop updates, fills shallow validated support gaps, and establishes a walkable graded base;
- old active road saves from Alpha.24 remain compatible: their normal non-encoded `step` resumes the already-prepared paving path instead of re-grading the route;
- after grading, the worker extracts actual stone-family stacks from loaded settlement storage in batches and visibly carries them to the active work front;
- the project stone cost is consumed gradually from the carried physical stack as road placements advance;
- removing settlement stone while work is underway causes resupply to pause naturally instead of creating abstract reserved resources;
- surplus carried stone is returned to physical settlement storage before the road is registered complete;
- the active builder is protected during the road job and restored to normal vulnerability when the transaction finishes;
- already-correct road surface blocks are protected from player breaking while the road is active, preventing drop/rebuild cost exploits;
- final validation repairs missing safe surface blocks from the already-paid project transaction without rewinding the stone-cost progression;
- HUD guidance distinguishes `도로 지반 정리`, `도로 석재 운반·포설`, and `도로 마감 확인`.

### Alpha.26 — physical outpost construction

Alpha.26 moves outposts onto the same physical transaction model as buildings and roads.

- approval validates the full wood 72 / stone 48 requirement without deleting it up front;
- a persisted grading phase walks the 9×9 site and performs only validated shallow earthwork;
- grading uses soil-like support fill instead of generating free recoverable stone;
- the builder then extracts actual wood/stone-family stacks from loaded settlement storage and carries them to the work front;
- blueprint material cost is consumed gradually from the builder's carried physical stack according to the current placement material family;
- work pauses on shortages or new obstructions rather than generating resources or overwriting player changes;
- active outpost transaction blocks and graded ground are protected while construction is in progress;
- old pre-Alpha.26 active outpost saves remain on their prepaid construction path and are not charged wood/stone a second time;
- surplus carried construction material is returned to settlement storage before the finished outpost is registered;
- completion then detects the site's lumber / quarry / mining / agriculture specialization and activates local production.

### Alpha.27 — road-bound outpost logistics

Alpha.27 makes the connection between specialized outposts and the shared settlement persistent and physically readable.

- every transport villager is permanently assigned to one outpost with settlement-owned entity tags and a visible `운송 주민 #<id>` identity;
- old generic `운송 주민` entities are migrated to nearby existing outposts without charging arrival food again;
- transport no longer pairs generic villagers with `outposts[i]` through UUID sorting, preventing silent reassignment after death/reload/order changes;
- a route is reconstructed from persisted `RoadSegment.centers()` and follows chained/branched roads back through older network connections toward the settlement;
- empty outbound and loaded inbound trips advance through short road waypoints so L-corners and chained roads remain meaningful instead of collapsing into one direct navigation target;
- normal travel needs only the worker's current/next route chunks to be loaded; an unloaded next segment pauses the worker without force-loading it;
- only fully loaded routes are used for legacy migration, missing-worker replacement and authoritative population reconciliation, preventing an unloaded transporter from being mistaken for a dead one;
- stockpile extraction is specialization-filtered: lumber carries wood, quarry carries stone, agriculture carries food staples and mining carries mined outputs; unrelated player junk is left in the outpost chest;
- a migrated legacy worker already carrying an item returns that item safely to town storage rather than deleting it.

Road/outpost route planning and the B/R/Enter/Backspace interaction budget remain unchanged. Alpha.27 deepens the simulation behind the same controls.

### Validation

The canonical GitHub Actions workflow performs:

1. source audit;
2. Java 25 clean Gradle build;
3. runtime JAR verification;
4. deliverable artifact upload;
5. canonical CI-result recording back to `ci-results/frontier-settlement/`.

Real in-game visual quality, navigation behavior and pacing must still be judged through actual play/screenshots; CI cannot prove those presentation qualities.
