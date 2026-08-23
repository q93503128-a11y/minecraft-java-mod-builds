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

The starter stockpile is an authoritative physical container whose saved position drives the early resource loop. It must not be casually destroyed into a progression softlock.

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
6. establish the work transaction without mutating the world immediately;
7. the builder physically clears/grades the safe site;
8. workers haul resources and build it physically.

Construction phase intention:

`site clearing -> hauling -> foundation -> frame -> walls -> roof -> interior -> completion`

Terrain rules:

- small terrain differences may be automatically cleared / filled by visible worker action, not at approval time;
- medium differences should later require explicit approval;
- extreme terrain must be rejected;
- player containers, valuable blocks, fluids, existing structures and protected infrastructure must not be silently destroyed;
- construction preparation must not create loose item drops;
- grading/fill must not create recoverable free economic materials merely because a project was approved;
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

Loaded areas should show physical work. Unloaded areas use coarse simulation where appropriate; do not force-load chunks just to keep worker animations running. Ordinary town production should also respect loaded-chunk boundaries and readable work cadence rather than running much faster than distant specialized production simply because it lives near the center.

Night routines should preserve the same authority model as daytime work. Town workers may return to housing, outpost workers may return to their local shelter, and remote haulers may stop between anchors; night behavior must not introduce a second navigation system that overrides road logistics.

No family/children simulation in the planned scope.

## 7. Resources and logistics

Resources remain physical Minecraft items. The HUD ledger is a cached view, not the authority.

- workers deposit physical items into settlement storage;
- construction and infrastructure costs consume physical items;
- do not scan every chest every tick;
- use categorized/tag-based compatibility for wood, stone, food and similar families;
- warehouse expansion may add storage positions without turning the settlement into abstract currency.

Construction should visibly communicate logistics. The builder walks from actual settlement storage carrying real wood/stone stacks, stages or carries them at the work site, and then builds at a readable pace. Do not instantly delete all resources at approval and magically spawn the structure.

Distant outpost logistics should remain spatial and physical. Transport workers belong to a specific outpost, follow persisted road-network waypoints, carry actual output stacks, and pause at unloaded route boundaries rather than teleporting or force-loading the territory. Worker absence/replacement must never confuse an unloaded entity with a dead one.

Alpha.27 tagged road logistics is the single authority for outpost transport. Higher settlement tiers may improve the surrounding infrastructure or capacity later, but must not reintroduce generic-name/UUID pairing or a second navigation controller for transport workers. This remains the single authority for outpost transport.

Specialized outpost production follows the same physical rule. Loaded workers must visibly approach local work, real nearby resources are finite where appropriate, and renewable production should come from an actual renewable world process such as crop growth rather than a timer that creates abstract items. Production must not force-load remote chunks.

## 8. Roads, outposts and territory

Roads/outposts are the spatial-growth layer; do not endlessly enlarge a single flat base.

Road flow:

`choose start -> choose route/end -> preview -> approve -> workers grade/build`

Road planning should avoid destructive tunneling, cliffs and reckless terrain damage. Bridges/stairs may be added when route quality needs them.

No early teleport network. Roads and logistics must matter.

Outposts connect distant resources and exploration back to the shared settlement and should become specialized territory nodes rather than duplicate mini-towns. Lumber, quarry and mining sites may deplete locally and create pressure to expand or restore the landscape; agriculture is the first deliberately renewable specialization through physical vanilla crop growth.

tier-visible public works may make established territory more readable, but they must stay non-destructive: loaded chunks only, clear road shoulders only, no overwriting player containers/fluids/buildings/outposts, and no recoverable free-block farming from automatically maintained infrastructure.

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

## 13. Current playable slice after Alpha.30

Alpha.30 preserves the complete shared-settlement spatial loop and closes the largest code-visible inconsistencies before the next bundled hands-on test:

- one shared server settlement and territory state;
- physical shared storage with cached HUD ledger;
- pioneer marker founding and a protected authoritative starter stockpile;
- compact B-key palette and world-space building/road/outpost placement;
- B / R / Enter / Backspace unified controls;
- one-line server-authored next-goal and active construction-phase guidance;
- starter and advanced functional buildings;
- worker jobs and housing/population progression;
- roads, productive specialized outposts and tier progression toward a domain;
- new building approval records the transaction without clearing terrain or generating a free foundation immediately;
- new buildings physically progress through a persisted worker grading phase before their existing hauling/building transaction;
- building grading visits the footprint and one-block apron, refuses block entities/fluids/unsafe obstructions/unloaded work cells and uses shallow coarse-dirt support rather than recoverable cobblestone;
- old pre-Alpha.30 active building saves remain on their already-prepared small-step path so migration does not re-grade or double-charge them;
- building construction then uses real storage extraction, visible carried stacks, an on-site supply barrel and gradual wood/stone consumption;
- road approval validates cost without instant deletion and roads physically progress through grading, hauling and 3-wide paving;
- outposts physically progress through 9×9 grading, actual wood/stone hauling and gradual blueprint construction;
- finished outposts detect lumber / quarry / mining / agriculture specialization;
- each transport worker is persistently tagged to one outpost and follows persisted road-center waypoints;
- transport pauses at unloaded route boundaries and carries only appropriate specialization output;
- Alpha.27 road logistics remains the single authority for outpost transport at every tier;
- each specialized production worker is persistently tagged to its own outpost and production only examines already-loaded chunks;
- lumber workers physically approach nearby natural trees and harvest at a throttled cadence;
- quarry workers physically approach exposed stone and only remove exposed cells;
- mining workers perform a readable minehead cycle and consume finite actual underground ore;
- agriculture is renewable through vanilla wheat growth, with a one-time outpost plot instead of continuous magical repair;
- main-settlement logger / farmer / quarry / miner production is also throttled to readable roughly 5s / 6s / 4s / 8s cycles, uses loaded cells only and swings visibly on successful work;
- main-settlement quarry cluster work only removes individually exposed stone cells rather than hidden adjacent material;
- full local stockpiles naturally stall workers carrying undelivered output, preserving storage pressure;
- frontier-town and domain tiers add deterministic settlement-owned lighting on safe loaded road shoulders, with aligned denser spacing at domain tier;
- civic-core and tier-public-work blocks are protected from break-and-respawn drop exploits;
- ordinary guard and blacksmith maintenance ignore unloaded work centers rather than treating unloaded infrastructure as an active missing target;
- town, transport and outpost-production villagers retain tag-aware night routines while remote haulers between safe rest anchors stop rather than forcing cross-territory night paths;
- active building/road/outpost transaction blocks remain protected from break-and-rebuild resource exploits.

## 14. Near-term priorities

Alpha.30 is the code-level test-readiness checkpoint. The next sequence should stay narrow and hands-on:

1. run the real survival loop from founding through first house / lumber camp / farm, then road -> outpost -> specialized production -> transport -> tier growth, with particular attention to the new building grading phase;
2. test save/reload during building grading and during physical hauling/building, and if an older active-build world is available verify that the pre-Alpha.30 prepared-site migration resumes without regrading or extra material charge;
3. inspect builder navigation around the one-block grading apron, scaffold approaches, road turns, outpost grading/build pacing, transport adherence, stockpile pressure, production cadence, night routines and tier road lamps;
4. verify the starter stockpile and automated civic/public-work blocks remain safe under ordinary player interaction; explosion behavior is not yet claimed as a tested protection path;
5. one lower-severity manual-audit item remains: same-role town workers are still pooled by visible role name / UUID order rather than permanently tagged to one town workplace. If real play shows noticeable workplace swapping after reload/death, make per-building town-worker identity the next stabilization fix instead of adding content breadth;
6. fix any real-play root cause before adding breadth; only after the building/road/outpost/logistics/production/tier loop feels stable should exploration/conquest inputs and additional meaningful building families expand;
7. keep UI/controls compact while simulation depth expands behind the scenes.

Real-play observations override assumptions in this file. If screenshots or symptoms expose a problem, fix the root cause before adding more content.
