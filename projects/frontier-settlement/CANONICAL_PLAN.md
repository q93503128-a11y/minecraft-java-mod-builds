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

## 8. Roads, outposts and territory

Roads/outposts are the spatial-growth layer; do not endlessly enlarge a single flat base.

Road flow:

`choose start -> choose route/end -> preview -> approve -> workers grade/build`

Road planning should avoid destructive tunneling, cliffs and reckless terrain damage. Bridges/stairs may be added when route quality needs them.

No early teleport network. Roads and logistics must matter.

Outposts connect distant resources and exploration back to the shared settlement and should become specialized territory nodes rather than duplicate mini-towns.

## 9. Combat and exploration

This is not Village Guardians-style wave defense.

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

Do not force-push over concurrent work. Other projects share the repository and CI result bots may advance `main`; always re-read `main` before committing and rebase only the Frontier Settlement changes.

## 13. Current playable slice after Alpha.23

Alpha.23 is intended to preserve the existing Alpha.22 systems and add physical construction logistics:

- one shared server settlement;
- physical shared storage and cached HUD ledger;
- pioneer marker founding;
- compact B-key palette;
- world-space building/road/outpost placement;
- B / R / Enter / Backspace unified controls;
- one-line server-authored next-goal guidance;
- starter and advanced functional buildings;
- worker jobs and housing/population progression;
- roads and productive outposts;
- tier progression toward a domain;
- safe building blueprints with no-drop terrain preparation;
- builder walks from actual settlement storage carrying real wood/stone stacks;
- active construction has a physical on-site supply barrel;
- materials are consumed gradually from the site supply as blocks are placed;
- building placement pace is deliberately slower/readable instead of two-block bursts every 5 ticks.

## 14. Near-term priorities

After Alpha.23 is stable in real play:

1. inspect real screenshots/gameplay for builder pathing, construction timing, site crate placement and building visual quality;
2. improve construction phase readability and scaffolding/work positions if the builder still looks magical;
3. continue road construction presentation and terrain-quality work;
4. expand outpost specialization and logistics only after the base loop feels good;
5. keep UI/controls compact while expanding depth behind the scenes.

Real-play observations override assumptions in this file. If screenshots or symptoms expose a problem, fix the root cause before adding more content.
