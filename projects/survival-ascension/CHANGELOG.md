# Changelog

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
- Replaced one fixed field objective per expedition region with two persistent directive options per region, for 18 total directives across the nine expedition regions.
- New region discoveries randomly select one directive per player and persist the choice in `expedition_v1`; leaving/re-entering the biome or restarting the server does not reroll it.
- Standard directives preserve the 0.24 objectives. Mixed directives combine two real gameplay tasks and require both before the region becomes completed.
- Added `ExpeditionAction` vocabulary for actual smart-tree log breaks, protected bulk construction placements, mature crop harvests, legitimate travel, water/vessel voyage, pickaxe block breaks, hostile kills and successful R dash uses.
- Added mixed directives including Woodland logs64+travel240, Arid build96+travel240, Wetland crops64+hostile8, Highlands travel360+dash12, Ocean voyage500+hostile8, Deep mining128+hostile10, Frozen travel360+dash12, Nether hostile16+mining96 and End hostile20+travel360.
- Directive progress is only credited from existing authoritative gameplay hooks. Inventory stockpiles, manual single-block building, passive kills, invalid movement and rejected/cooldown dash requests do not satisfy the corresponding scaled-action tasks.
- `/ascension stats` now shows the assigned directive name and every active task's current/target value for discovered incomplete regions.
- Extended `expedition_v1` with optional per-region directive indices and per-action progress keys while retaining the same SavedData ID.
- Added 0.24 migration: old discovered regions receive the standard directive, legacy single-objective progress moves into the standard directive's first task, completed regions remain completed, and 0.23/0.24 reward bits still prevent duplicate XP/material/Mythic rewards.
- Existing 0.23 master-milestone owners remain fully completed and keep Field Mastery.
- Field Mastery itself is unchanged: Quarry7x7x12, Woodcut448, Harvest13x13, Academy shockwave7.5/20, Construction line65/plane13x13 and four Stage-2 Nexus air dashes.
- FTB Quests (All Rights Reserved) and Bountiful (GPL-3.0) are reference-only for multi-task quest and variable contract ideas. No source, UI, quest/bounty data, assets or namespaces are bundled.
- Extended in-game Guide, README/PROJECT canon, source audit and JAR verification for the new directive classes, migration rules, all-task completion and successful-dash accounting.

## 0.24.0-alpha.1
- Reworked Expeditions from instant biome discovery rewards into persistent `discovered -> field objective -> completed` progression. Entering a region now only discovers it; milestone rewards and Field Mastery require actual regional work.
- Added nine explicit field objectives: Woodland natural-tree bulk felling96, Arid scaled bulk placements128, Wetland mature harvests96, Highlands legitimate on-foot traversal600, Ocean swim/vessel travel800, Deep pickaxe blocks192, Frozen legitimate on-foot traversal600, Nether hostile kills24 and End hostile kills32.
- Woodland progress is credited only after successful smart-tree queued log destruction, so stockpiled logs or ordinary placed-log demolition cannot bypass the objective.
- Arid progress is credited only from successful secondary Construction queue placements after the existing real-material, survival and protection checks.
- Wetland and Deep objectives reuse normal block-break events, so every actual tick-queued crop harvest and every normal area/vein/extract/tunnel pickaxe break can contribute while the player remains in the correct expedition biome.
- Highlands/Frozen reuse Mobility's legitimate sprint-distance filter, excluding passengers, flight, elytra, swimming and teleport-like displacement.
- Ocean uses a separate water/vessel voyage tracker with a 24-block-per-second displacement sanity cap. Normal land Mobility progress is explicitly forbidden from advancing Ocean, preventing frozen-ocean sprint shortcuts.
- Nether/End count only player kills of hostile `Enemy` entities in the matching region; passive livestock kills do not contribute.
- `/ascension stats` reports discovered/completed counts and exact progress for each discovered-but-incomplete field objective.
- Extended `expedition_v1` with optional `completed`, objective `progress` and `region_rewards` state while retaining the same SavedData ID for existing worlds.
- Added 0.23 migration safety: old discoveries remain discovered, old per-region skill XP is marked already paid, old milestone bits remain claimed, and master-milestone owners migrate to all nine completed so Field Mastery is never lost.
- Field Mastery scale remains the final Lv.100 layer: Quarry7x7x12, Woodcut448, Harvest13x13, Academy shockwave7.5/20, Construction line65/plane13x13 and four Stage-2 Nexus air dashes.

## 0.23.0-alpha.1
- Added per-player `expedition_v1` SavedData with nine stage-gated vanilla-biome expedition regions: Woodland, Arid, Wetland, Highlands, Ocean, Deep, Frozen, Nether and End.
- Stage 0 exposes five Overworld regions, Stage 1 unlocks Deep/Frozen/Nether, and Stage 2 unlocks End.
- Added per-player milestone rewards and final Stage-2 nine-region completion.
- Completing all nine regions at Stage 2 unlocks `Field Mastery`, a final Lv.100 physical-action scale layer rather than another flat stat multiplier.
- Field Mastery Mining extends Quarry Network tunnel 7x7x10 -> 7x7x12 while retaining 12/player and 64/global destruction budgets.
- Woodcutting extends 384 -> 448 logs, Harvesting 11x11 -> 13x13, Combat Academy 6.5/16 -> 7.5/20, Construction line49 -> 65 and plane11x11 -> 13x13, Mobility Nexus air dashes3 -> 4.

## 0.22.0-alpha.1
- Added randomized Ascension Trial tactical doctrines `쇄도 / 추격 / 봉쇄` with different vanilla-mob role mixtures and one bounded mid-wave reinforcement.
- No doctrine adds blanket HP scaling; existing Stage-2 mutations, Elite ranks and Warband roles still layer through normal triggered spawns.
- Added `M -> Equipment -> 신화 각성`. Valid Mythic III gear preserves its three affixes and adds one missing fourth affix after a large endgame resource cost.
- Awakened Mythic rerolls preserve four affixes and remain expensive.
- Kept Evokers out of direct trial composition so untracked Vex cannot survive encounter cleanup.

## 0.21.0-alpha.1
- Added repeatable Stage-2 `Ascension Trial` behind the completed Ascension Nexus.
- Added four timed waves, 5-second setup, boss-bar state, owner/distance grace, active-trial separation, persistent cooldown, orphan mob rejection and stale-server cleanup.
- Completion guarantees one Mythic III affix item + Netherite Scraps + Diamonds + XP.
- Adapted timed sequential-wave / boss-bar lifecycle from Gateways to Eternity under MIT and packaged its notice.

## 0.20.0-alpha.1
- Added final Lv.100 `Mastery VI` tier across all six active skills.
- Mining: 11x11 excavation, 192 vein/extract cap, Quarry tunnel7x7x10.
- Woodcutting: 384 natural logs with leaf safety and tick drain.
- Harvesting: 11x11 mature crops with seed-backed irrigation replant.
- Combat: cleave10/5-block/70%; Combat Academy shockwave6.5/16/55%/50ticks.
- Construction: line49, plane11x11, Builder Foundry volume7x7x7.
- Mobility: step2.0, safe fall16, dash1.80/16ticks, Stage-2 Nexus air dash3.
