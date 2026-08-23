# Changelog

## 0.28.0-alpha.1
- Added Stage-1 shared infrastructure `산업 가공소 / Industrial Works`: Stone Bricks1024 + Iron512 + Copper512 + Redstone256 + Amethyst128.
- Added a MineMenu-style nested `산업 생산망` radial instead of a generic machine/quest rectangle. The submenu contains facility funding, four production lines, supply dispatch, status and back navigation.
- Added four large-batch production lines that deliberately consume different survival output categories: `제련 배치` Raw Iron96 + Raw Copper96 + Coal64; `구조재 배치` any logs192 + Cobblestone384 + Iron32; `식량 배치` Wheat128 + Carrot64 + Potato64 + Beetroot32; `정밀 부품 배치` Redstone128 + Amethyst64 + Gold32 + Quartz64.
- Added per-player `production_v1` SavedData with four bounded line buffers, lifetime cycle count and stored supply charges. Each line buffer is capped at3 and supply charges are capped at3.
- A production cycle requires at least one completed batch from all four lines. The system consumes exactly one from each line and creates one supply charge; a single abundant resource line cannot complete cycles by itself.
- Added queued-cycle normalization: if supply storage is full while complete four-line sets remain buffered, dispatching a charge immediately assembles waiting sets until the charge cap is reached, preventing a permanent buffer deadlock.
- Batch execution is atomic: the server checks line capacity and every input amount before consuming anything. Failed/partial-material requests do not eat resources.
- Added tangible supply dispatch: one stored supply charge produces Gold Ingots32 + Amethyst Shards16 + Echo Shards2, delivered to inventory or dropped if full. This keeps the output usable across Apex Hunts, Ascension Trials and equipment sinks rather than hiding a special-case discount inside one subsystem.
- `/ascension stats` now reports industrial lifetime cycles and stored supply charges.
- Existing `infrastructure_v1` schema is unchanged; Industrial Works uses new project funding keys only. Existing saves therefore retain all old infrastructure progress. `production_v1` is a new independent per-player store.
- No new network payload was added; the existing string-based `InfrastructureActionPayload` routes production actions, so protocol remains8.
- Rechecked Create's current repository license split: code is MIT and files under `src/main/resources/assets/` are All Rights Reserved. 0.28 studies only the high-level multi-step/high-throughput processing and logistics progression; no Create assets, recipes, machines, data or namespaces are copied.
- Updated Guide, README/PROJECT canon, source audit and JAR verification for the new production runtime/persistence/UI classes and resource-safety contracts.

## 0.27.0-alpha.1
- Added Stage-1 shared infrastructure `정점 추적소 / Apex Tracking Post`: Iron512 + Gold256 + Amethyst256 + Echo32 + Nether Star1.
- Completed tracking posts can be re-selected inside an already completed expedition region to start a 90-second behavior-driven regional Apex Hunt for Echo8 + Amethyst32 + Gold32.
- Added nine regional Apex archetypes: Woodland Breaker, Arid Commander, Wetland Plagueheart, Highland Hunter, Ocean Tyrant, Deep Stalker, Frozen Warden, Nether Reaver and End Harbinger.
- Added distinct combat patterns instead of one blanket HP scale: telegraphed charge, health-threshold reinforcements, poison/heal field, skirmish repositioning, pull, leap, frost slow field, wither pulse and levitation/void pressure.
- Apex bosses use archetype-specific additive health/armor/attack tuning, tagged triggered vanilla spawns, boss bars and bounded escort compositions.
- Added owner lifecycle safety: 64-block/current-region validation, 10-second grace, 90-second timeout, 48-block escort recall, 96-block hunt separation, logout/failure cleanup, stale-server cleanup and tagged-orphan rejection.
- Apex Hunt cannot start during a field incident; hunt activation pushes field-incident readiness past the hunt window; Ascension Trial activation is refused while that player owns an active Apex Hunt.
- Added `apex_hunt_v1` per-player SavedData with nine first-defeat bits, total victories and one-time 9/9 mastery reward state.
- Stage1 hunt victory: Ascended II gear + Diamond2 + Echo4 + XP120. Stage2: Ascended II gear with 20% Mythic III chance + Diamond3 + Echo6 + Netherite Scrap1 + XP180. Nearby helpers get XP50 without duplicate owner loot.
- First defeats of all nine Apex archetypes grant guaranteed Mythic III + Netherite Scrap4 + Echo32 + Dragon Breath16 + XP500 once.
- `/ascension stats` now reports Apex first defeats x/9 and total hunt victories.
- Added the tracking post to the existing MineMenu-style Infrastructure radial instead of introducing another generic rectangular menu.
- Clarified Apotheosis licensing: the official `Shadows-of-Fire/Apotheosis` GitHub 26.1 code license is MIT; distribution-page/assets rights are treated separately and no Apotheosis assets are bundled.
- Silent Gear (MIT) is reference-only for the high-level long-lived gear/resource-sink philosophy; no Silent Gear source, material/part system, blueprints, data or assets are copied.
- Updated Guide, README/PROJECT canon, source audit and JAR verifier for the new hunt runtime and persistence classes.

## 0.26.0-alpha.1
- Added 18 rare regional field incidents: one bounded hostile ambush and one action-rush incident for each of the nine expedition regions.
- Eligible players inside a discovered region receive a 10% incident roll every 30 seconds. Incidents last 45–60 seconds and expose remaining enemies/action progress/time through a boss bar.
- Added ambush variants for Woodland, Arid, Wetland, Highlands, Ocean, Deep, Frozen, Nether and End using bounded `EntitySpawnReason.TRIGGERED` vanilla spawns. Ocean ambushes require water spawn slots.
- Added action-rush variants that reuse authoritative Survival Ascension action hooks: natural smart-tree logs, protected/material-backed scaled Construction, mature crops, successful dashes, water/vessel voyage, valid pickaxe mining and legitimate traversal.
- Incidents fail after 10 seconds outside the matching expedition region or 48-block event radius, but never erase existing directive progress.
- Failed/logged-out incidents clean tracked mobs and boss-bar viewers; stale in-JVM incident state is removed when a different server instance is detected.
- Added an Ascension Trial exclusion window based on the existing persisted trial-ready tick so regional incidents cannot overlap the main Stage-2 combat encounter.
- Extended `expedition_v1` with optional `incident_rewards` bits while keeping the same SavedData ID and all existing 0.23–0.25 migration semantics.
- A region incident pays its success bundle once per player: Stage0 skill XP100 + Emerald4 + Amethyst8; Stage1 XP150 + Diamond2 + Echo4; Stage2 XP200 + Diamond4 + Echo8.
- First incident resolution in an incomplete region also grants at most 20% progress to its first unfinished directive task. This is one-time and follows the normal directive-completion/milestone path rather than bypassing it.
- `/ascension stats` now reports resolved incidents x/9 in addition to discoveries, completed directives and active task progress.
- Studied Enhanced Celestials Tweaks (MIT) only for the high-level temporary-event modifier/lifecycle idea; the regional incident catalog, spawn rules, boss bars, rewards and persistence are independent and no source/assets/config are bundled.
- Majrusz's Progressive Difficulty is reference-only for rare forced-encounter pacing because the current public repository root does not expose a license file; no source/assets/data are copied.
- Extended Guide, README/PROJECT canon, source audit and JAR verification for incident cadence, cleanup, one-time rewards, trial separation, 20% bonus cap and the new runtime classes.

## 0.25.0-alpha.1
- Replaced one fixed field objective per expedition region with two persistent directive options per region, for18 total directives across nine expedition regions.
- New region discoveries randomly select one directive per player and persist it in `expedition_v1`; standard directives preserve 0.24 behavior and legacy progress/reward migration.
- Mixed directives combine two actual gameplay tasks and require every task before region completion.
- Field Mastery remains unchanged: Quarry7x7x12, Woodcut448, Harvest13x13, Academy shockwave7.5/20, Construction line65/plane13x13 and four Stage-2 Nexus air dashes.

## 0.24.0-alpha.1
- Reworked expeditions from instant biome discovery rewards into persistent `discovered -> field objective -> completed` progression with nine explicit regional objectives and legacy reward migration.

## 0.23.0-alpha.1
- Added `expedition_v1`, nine stage-gated vanilla-biome expedition regions, milestone rewards and final Stage-2 Field Mastery.

## 0.22.0-alpha.1
- Added randomized Ascension Trial doctrines `쇄도 / 추격 / 봉쇄` and 4-affix Awakened Mythic progression.

## 0.21.0-alpha.1
- Added repeatable Stage-2 four-wave Ascension Trial behind the completed Ascension Nexus.

## 0.20.0-alpha.1
- Added final Lv.100 Mastery VI across all six active skills.
