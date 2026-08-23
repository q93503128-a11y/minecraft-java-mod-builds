# Frontier Settlement — Canonical Plan

This file is the repository-side source of truth for design direction. Read this together with the current `main` source, `PROJECT.md`, `README.md`, CI results and verifier output before continuing development.

## 1. Product identity

Frontier Settlement is a Minecraft Java survival settlement / territory-growth mod.

Core loop:

`survival -> settlement growth -> better exploration -> external exploration / conquest -> NPCs / resources / technology -> settlement growth`

Player role progression:

`survivor -> pioneer -> village leader / lord-like role -> domain operator`

Settlement scale progression:

`pioneer camp -> hamlet -> village -> frontier town -> domain`

The mod must keep feeling like Minecraft. Internal simulation may be deep, but the player should not operate a spreadsheet.

## 2. Interaction budget

Hard rule: many systems are allowed, but the number of things the player must directly micromanage stays small.

Primary direct interactions are limited to roughly:

1. shared storage;
2. choosing a building, position and rotation;
3. choosing an outpost location;
4. choosing a road start/end and optional route guidance;
5. exploring, fighting and obtaining rare resources.

Worker assignment, hauling, repetitive production, terrain preparation, ordinary logistics and most routine settlement labor should be simulated or automated.

Do not grow this into a tax spreadsheet, happiness spreadsheet, family simulator, giant research tree, or per-worker assignment UI unless later testing proves a real need.

## 3. Multiplayer authority

- One world/server has one shared settlement and territory state.
- Resources, buildings, population, roads, outposts and progression are shared.
- The server is authoritative.
- Do not create independent per-player towns or internal player politics in the initial scope.

## 4. Founding and early loop

The intended start is:

- player chooses a location;
- the pioneer marker/core establishes the shared settlement;
- a small shared physical stockpile is created;
- the settlement begins with the player group and one dedicated construction worker;
- players gather the first resources manually;
- the builder performs the first physical construction work.

Debug commands may remain during development, but normal survival interaction must use world-space interaction and the compact palette rather than commands.

## 5. Buildings

Functional settlement buildings use official mod blueprints. Vanilla/player-built structures remain allowed in the world, but player-built structures are not scanned and registered as functional settlement buildings in the planned scope.

Target building family count is roughly 15-20 meaningful families with upgrades, not hundreds of shallow building types.

Construction UX:

1. open the compact building palette;
2. choose a building;
3. see a transparent 3D completed preview;
4. choose position and rotation;
5. validate terrain / overlap / cost;
6. establish the work site;
7. workers haul resources and build it physically.

Construction phase intention:

`site clearing -> hauling -> foundation -> frame -> walls -> roof -> interior -> completion`

Terrain rules:

- small terrain differences may be automatically cleared / filled;
- medium differences should later require explicit approval;
- extreme terrain must be rejected;
- player containers, valuable blocks, fluids, existing structures and protected infrastructure must not be silently destroyed;
- construction preparation must not create loose item drops;
- roofs must never appear before their supports;
- foundations/floors must not visibly float;
- completed enclosed buildings require deliberate windows and sufficient lighting.

Building art must be checked in actual Minecraft. If a code-authored building looks generic or weak, study strong Minecraft colony/building references and improve the design rather than defending a placeholder. Public/licensed sources may be used as technical/design references, but repository assets must remain license-safe.

## 6. Citizens and jobs

Vanilla villager profession/trading is not the settlement progression system. Custom settlement roles replace it.

Initial role families:

- builder;
- logger;
- farmer;
- quarry worker;
- miner;
- blacksmith-related service;
- guard;
- transport/logistics worker.

Job slots should fill automatically when housing, food and relevant workplaces permit it.

Loaded areas should show physical work. Unloaded areas use coarse simulation where appropriate; do not force-load chunks just to keep worker animations running.

No family/children simulation in the planned scope.

## 7. Resources and logistics

Resources remain physical Minecraft items. The HUD ledger is a cached view, not the authority.

- workers deposit physical items into settlement storage;
- construction and infrastructure costs consume physical items;
- do not scan every chest every tick;
- use categorized/tag-based compatibility for wood, stone, food and similar families;
- warehouse expansion may add storage positions without turning the settlement into abstract currency.

Construction should visibly communicate logistics. The builder should walk to a real storage source, visibly carry actual item stacks, place them into a construction supply cache, and then build at a readable pace. Do not instantly delete all resources at approval and magically spawn the structure.

Distant outpost logistics should remain spatial and physical. Transport workers belong to a specific outpost, follow persisted road-network waypoints, carry actual output stacks, and pause at unloaded route boundaries rather than teleporting or force-loading the territory. Worker absence/replacement must never confuse an unloaded entity with a dead one.

Specialized outpost production follows the same physical rule. Loaded workers must visibly approach local work, real nearby resources are finite where appropriate, and renewable production should come from an actual renewable world process such as crop growth rather than a timer that creates abstract items. Production must not force-load remote chunks.

## 8. Roads, outposts and territory

Roads/outposts are the spatial-growth layer; do not endlessly enlarge a single flat base.

Road flow:

`choose start -> choose route/end -> preview -> approve -> workers grade/build`

Road planning should avoid destructive tunneling, cliffs and reckless terrain damage. Bridges/stairs may be added when route quality needs them.

No early teleport network. Roads and logistics must matter.

Outposts connect distant resources and exploration back to the shared settlement and should become specialized territory nodes rather than duplicate mini-towns. Lumber, quarry and mining sites may deplete locally and create pressure to expand or restore the landscape; agriculture is the first deliberately renewable specialization through physical vanilla crop growth.

## 9. Combat and exploration

This is not a constant wave-defense loop.

- guards handle routine local danger;
- occasional major threats are allowed;
- do not spam constant raids/waves.

External exploration can include ruins, mines, camps, nests, settlements, rare resources, bosses, dungeons, caravans and rare NPCs. This layer should feed settlement growth rather than exist as a disconnected adventure mode.

## 10. UI and controls

Reference hierarchy:

1. Against the Storm — compact resource strip, categories, readable overlays;
2. Manor Lords — freeform world placement and roads;
3. MineColonies — Minecraft-native 3D blueprint placement/rotation/material progress;
4. Frostpunk 2 — secondary territory overview ideas.

Do not invent large generic rectangle dashboards when a proven interaction reference exists.

Persistent HUD target is compact, roughly:

`[Village Name] wood 234 | stone 181 | metal 46 | food 317 | population 8/12`

Normal gameplay controls are currently unified around:

- `B`: settlement palette;
- `R`: rotate current building placement;
- `Enter`: confirm the active building/road/outpost placement;
- `Backspace`: reset/cancel the road start selection step.

Control policy:

- avoid important vanilla keys whenever possible;
- if more keys are truly needed, first reuse/reassign low-value vanilla actions that do not matter much to this mod;
- do not scatter one system across many separate hotkeys;
- never silently conflict with essential inventory, movement, combat, hotbar, chat or camera controls.

## 11. External mod compatibility

A minimal intended companion stack is:

- Terralith;
- Dungeons and Taverns;
- Better Combat;
- Sophisticated Backpacks;
- Jade;
- Xaero's Minimap.

Integrations should be optional and tag/data-driven where possible. Do not reimplement every world/combat/dungeon/item feature that companion mods already provide well.

Avoid early Waystones-style teleportation because it weakens roads/logistics. Avoid duplicative village-overhaul mods unless later compatibility work specifically calls for them.

## 12. Engineering rules

Target environment:

- Minecraft Java 26.2;
- NeoForge 26.2.0.38-beta;
- Java 25;
- Gradle 9.2.1.

Development sequence:

`read current GitHub main -> inspect current canonical docs/source/CI -> implement -> manual code/gameplay audit -> Java 25 clean build -> JAR verify -> direct main update -> deliver test JAR when useful`

Do not force-push over concurrent work. Other work shares the repository and CI result bots may advance `main`; always re-read `main` before committing and rebase only the Frontier Settlement changes.

## 13. Current playable slice after Alpha.28

Alpha.28 preserves the shared-settlement loop and extends the physical-work model through buildings, roads, outpost construction, road-connected logistics and distinct local production:

- one shared server settlement and territory state;
- physical shared storage with cached HUD ledger;
- pioneer marker founding;
- compact B-key palette and world-space building/road/outpost placement;
- B / R / Enter / Backspace unified controls;
- one-line server-authored next-goal and active construction-phase guidance;
- starter and advanced functional buildings;
- worker jobs and housing/population progression;
- roads, productive specialized outposts and tier progression toward a domain;
- safe building blueprints with no-drop terrain preparation;
- building construction uses real storage extraction, visible carried stacks, an on-site supply barrel and gradual material consumption;
- road approval validates cost without instant deletion and roads physically progress through grading, hauling and 3-wide paving;
- outposts physically progress through 9×9 grading, actual wood/stone hauling and gradual blueprint construction;
- finished outposts detect lumber / quarry / mining / agriculture specialization;
- each transport worker is persistently tagged to one outpost and follows persisted road-center waypoints;
- transport pauses at unloaded route boundaries and carries only appropriate specialization output;
- each specialized production worker is persistently tagged to its own outpost and legacy visible-name workers are adopted rather than duplicated;
- outpost production only examines already-loaded chunks and never force-loads remote resource areas;
- lumber workers physically approach nearby natural trees and harvest at a throttled cadence, so forests visibly deplete instead of producing abstract wood every half-second;
- quarry workers physically approach exposed stone and only remove exposed cells, avoiding hidden hollowing beneath intact terrain;
- mining workers perform a readable minehead cycle and consume finite actual underground ore at a much slower cadence;
- agriculture is renewable through vanilla wheat growth, with a one-time outpost plot instead of continuous magical replant/repair;
- full local stockpiles naturally stall workers carrying undelivered output, preserving storage pressure;
- active building/road/outpost transaction blocks remain protected from break-and-rebuild resource exploits.

## 14. Near-term priorities

After Alpha.28 automated validation is stable, keep the sequence narrow:

1. inspect real screenshots/gameplay for scaffold navigation, road turns, outpost grading/build pacing, transport adherence, production-worker movement, quarry holes, forest depletion, crop readability, unload/reload pauses and stockpile pressure;
2. fix real-play root causes before adding breadth if those tests expose navigation, terrain-damage, duplication, migration or visual-quality problems;
3. decide from play whether depleted lumber/quarry sites need explicit recovery mechanics or whether depletion should intentionally push further territorial expansion; do not add abstract regeneration merely to keep a node infinite;
4. tune production/trip rates only from actual play pacing, keeping specialization differences obvious and physical;
5. after the building/road/outpost/logistics/production spatial loop feels good, expand exploration/conquest inputs and additional meaningful building families;
6. keep UI/controls compact while expanding simulation depth behind the scenes.

Real-play observations override assumptions in this file. If screenshots or symptoms expose a problem, fix the root cause before adding more content.
