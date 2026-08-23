# Frontier Settlement

Minecraft Java 26.2 / NeoForge 26.2 cooperative settlement-growth mod.

Canonical direction: see `CANONICAL_PLAN.md`.

## Current version: 0.1.0-alpha.23

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

### New in Alpha.23 — physical construction logistics

Alpha.23 changes the builder from an almost-magical block placer into a visible logistics worker.

- approving a building validates the full physical wood/stone requirement but does not instantly delete the whole cost;
- a temporary physical construction supply barrel is created beside the work site;
- the builder walks to an actually loaded settlement storage container;
- the builder extracts a real matching item stack and visibly carries it in the main hand;
- carried resources are deposited into the site supply barrel in batches;
- construction waits until all currently required physical materials are staged;
- building cost is consumed gradually from the site barrel as blueprint steps are placed;
- if a player removes staged material, work pauses/re-hauls instead of creating resources from nothing;
- the active supply barrel and already-correct construction blocks are protected from accidental player breaking while the job is active, but wrong newly placed obstructions remain removable;
- building placement is paced to one blueprint step per 10 server ticks after the builder reaches a work position;
- final validation repairs unexpected missing air blocks without rewinding the whole cost transaction;
- leftover player-added items are returned to settlement storage when possible; an empty construction barrel is removed with a no-drop update.

Road/outpost construction still uses its existing stable transaction path; physical hauling is being proven on buildings first before being generalized.

### Validation

The canonical GitHub Actions workflow performs:

1. source audit;
2. Java 25 clean Gradle build;
3. runtime JAR verification;
4. deliverable artifact upload;
5. canonical CI-result recording back to `ci-results/frontier-settlement/`.

Real in-game visual quality, navigation behavior and pacing must still be judged through actual play/screenshots; CI cannot prove those presentation qualities.
