# Changelog

## 0.24.0-alpha.1
- Reworked Expeditions from instant biome discovery rewards into persistent `discovered -> field objective -> completed` progression. Entering a region now only discovers it; milestone rewards and Field Mastery require actual regional work.
- Added nine explicit field objectives: Woodland natural-tree bulk felling96, Arid scaled bulk placements128, Wetland mature harvests96, Highlands legitimate on-foot traversal600, Ocean swim/vessel travel800, Deep pickaxe blocks192, Frozen legitimate on-foot traversal600, Nether hostile kills24 and End hostile kills32.
- Woodland progress is credited only after successful smart-tree queued log destruction, so stockpiled logs or ordinary placed-log demolition cannot bypass the objective.
- Arid progress is credited only from successful secondary Construction queue placements after the existing real-material, survival and protection checks.
- Wetland and Deep objectives reuse normal block-break events, so every actual tick-queued crop harvest and every normal area/vein/extract/tunnel pickaxe break can contribute while the player remains in the correct expedition biome.
- Highlands/Frozen reuse Mobility's legitimate sprint-distance filter, excluding passengers, flight, elytra, swimming and teleport-like displacement.
- Ocean uses a separate water/vessel voyage tracker with a 24-block-per-second displacement sanity cap. Normal land Mobility progress is explicitly forbidden from advancing Ocean, preventing frozen-ocean sprint shortcuts.
- Nether/End count only player kills of hostile `Enemy` entities in the matching region; passive livestock kills do not contribute.
- `/ascension stats` now reports both discovered and completed counts and displays exact progress for each discovered-but-incomplete field objective.
- Extended the existing `expedition_v1` codec with optional `completed`, objective `progress` and `region_rewards` state while retaining the same SavedData ID for existing worlds.
- Added 0.23 migration safety: old discoveries remain discovered, old per-region skill XP is marked already paid to prevent duplicate XP, old milestone bits remain claimed, and players who already claimed the 0.23 master milestone migrate to all nine completed so Field Mastery is never lost.
- Expedition milestone rewards now trigger from completed field objectives rather than discovery counts; all player-specific one-time reward protections remain.
- Field Mastery scale remains the 0.23 final Lv.100 layer: Quarry7x7x12, Woodcut448, Harvest13x13, Academy shockwave7.5/20, Construction line65/plane13x13 and four Stage-2 Nexus air dashes.
- Bountiful and FTB Quests were studied only as objective/reward and persistent quest-stage design references. No source, assets, quest data, UI, namespace or runtime dependency from either project is bundled.
- Extended in-game Guide, README/PROJECT canon and source/JAR audit contracts for objective thresholds, in-region server validation, legacy migration, anti-shortcut rules and all prior 0.23/0.22 safety regressions.

## 0.23.0-alpha.1
- Added per-player `expedition_v1` SavedData with nine stage-gated vanilla-biome expedition regions: Woodland, Arid, Wetland, Highlands, Ocean, Deep, Frozen, Nether and End.
- Stage 0 exposes five Overworld regions, Stage 1 unlocks Deep/Frozen/Nether, and Stage 2 unlocks End. Creative and spectator movement cannot claim discoveries or rewards.
- Each region grants one-time skill XP appropriate to its terrain instead of being a disconnected checklist: Woodcutting/Construction/Harvesting/Mobility/Mining/Combat all participate.
- Added per-player milestone rewards: four Stage-0 regions, a Stage-1 seven-region milestone requiring Deep+Nether, and a final Stage-2 nine-region completion. Persistent milestone bits prevent re-entry or multiplayer duplication.
- Completing all nine regions at Stage 2 unlocks `Field Mastery`, a final Lv.100 physical-action scale layer rather than another flat stat multiplier.
- Field Mastery Mining extends the Quarry Network tunnel from 7x7x10 to 7x7x12. Pending capacity rises to 640 only to hold one job; destruction remains 12 blocks/player/tick and 64 globally.
- Field Mastery Woodcutting extends the natural-tree limit from 384 to 448 logs while preserving smart-tree foliage safety and the existing 12/player, 64/global tick drain.
- Field Mastery Harvesting extends mature-crop harvesting from 11x11 to 13x13. Bulk harvesting was converted from synchronous area destruction to a bounded 12/player, 64/global tick-drained queue with a 384 pending cap.
- Field Mastery Combat extends the Combat Academy sprint shockwave from 6.5 radius/16 targets to 7.5/20 while retaining the existing 55% damage fraction and 50-tick cooldown.
- Field Mastery Construction extends line 49->65 and wall/floor 11x11->13x13. Builder Foundry volume stays 7x7x7 and retains real-material, protection-hook, 512-pending and 64-global placement contracts.
- Field Mastery Mobility extends Stage-2 Ascension Nexus Lv.100 air dashes from three to four per airtime; landing reset and the existing dash cooldown remain unchanged.
- `/ascension stats` now reports expedition progress and the discovered-region summary; login reports existing expedition progress without adding another generic rectangular GUI.
- Existing Mastery VI remains the normal Lv.100 baseline. Field Mastery does not unlock actions before Lv.100 and Shift remains the precision override.
- Studied Lootr's per-player exploration-reward fairness as a design reference. Repurposed Structures and Explorer's/Nature's Compass are reference-only for vanilla-world exploration motivation; no LGPL/CC-BY-NC-SA source or assets are bundled.
- Extended source/JAR audits for the new SavedData, stage gates, one-time rewards, six Field Mastery action upgrades and tick-budget safety while retaining the 0.22 doctrine-trial/awakened-Mythic regressions.

## 0.22.0-alpha.1
- Added randomized Ascension Trial tactical doctrines: `쇄도 / 추격 / 봉쇄`. Each doctrine uses different vanilla-mob role mixtures instead of replaying the same four fixed waves.
- 쇄도 emphasizes Husk/Vindicator/Wither Skeleton/Ravager melee pressure, 추격 mixes Spider/Enderman/mobile melee pressure and actively drives distant pursuit mobs back toward the owner, and 봉쇄 emphasizes Skeleton/Stray/Pillager/Witch ranged-control pressure.
- Each wave can trigger exactly one doctrine-specific reinforcement when roughly half of the initial force remains. Reinforcements do not extend the 60-second wave timer, preventing a free reward/time loop.
- Doctrine variation adds no blanket HP multiplier; existing Stage-2 mutations, Elite ranks and Warband roles remain the systems that can stack additional behaviors and roles on normal triggered trial spawns.
- Kept Evokers out of trial composition so summon-produced Vex cannot escape the tracked encounter lifecycle.
- Added `M -> Equipment -> 신화 각성` inside the existing MineMenu-derived radial. No new rectangular equipment GUI was introduced.
- Normal Mythic III equipment remains 3-affix. One-time awakening preserves those three affixes and adds one missing affix, producing a 4-affix `각성 신화` item.
- Mythic awakening consumes 256 Amethyst Shards + 24 Diamonds + 8 Netherite Scraps + 64 Echo Shards + 16 Dragon's Breath, turning large resource throughput and Stage-2 combat resources into a final equipment sink.
- Awakened Mythic rerolls preserve four affixes and cost 128 Amethyst Shards + 16 Diamonds + 4 Netherite Scraps + 16 Echo Shards per reroll.
- Added strict awakening validation: only a valid non-awakened Mythic III item containing exactly three known affixes can consume awakening materials, preventing malformed CustomData from eating the full resource cost.
- Existing skill unlock guards remain unchanged, so the fourth affix cannot unlock excavation, cleave, harvest, chain-felling or other scaled actions before their skill level unlocks them.
- Awakening reuses the existing affix CustomData and vanilla base equipment; no new custom item registry, texture or model dependency was added.
- Extended in-game Guide, source audit, JAR verification, README/PROJECT canon and Gateways to Eternity MIT attribution for doctrines, bounded reinforcement and awakened Mythic contracts.

## 0.21.0-alpha.1
- Added the repeatable Stage-2 `Ascension Trial` endgame loop behind the completed Ascension Nexus instead of ending progression after infrastructure completion.
- Re-selecting a completed Nexus consumes 32 Echo Shards + 64 Amethyst Shards + 8 Dragon's Breath from the real survival inventory and opens four timed combat waves.
- Added mixed-role wave compositions built from vanilla mobs: zombie/skeleton pressure, husk/stray/witch control, wither-skeleton/illager pressure, then a Ravager/Vindicator/Pillager/Witch final wave. Summon-producing Evokers are deliberately excluded so untracked Vex cannot survive a failed or completed trial.
- Each wave has a 60-second limit, a 5-second inter-wave setup, and a server boss bar showing wave, remaining enemies and time.
- Added 10-second owner death/dimension/64-block departure grace, 96-block active-trial separation, one active trial per owner, and a 120-second persistent start cooldown.
- Trial mobs use the normal triggered spawn path, allowing existing Stage-2 mutation, Elite and Warband systems to interact with the encounter rather than adding a separate HP-sponge stat layer.
- Completion guarantees one Mythic III affix item + 2 Netherite Scraps + 4 Diamonds + 200 XP to the owner; nearby helpers receive XP without duplicating the owner loot bundle.
- Trial state itself is runtime-only. Persisted tagged trial mobs are rejected on entity join after a restart unless they still belong to the active in-memory trial, preventing orphan encounter mobs.
- Added stale-server cleanup so a new integrated/dedicated server instance immediately drops old in-JVM trial references and boss-bar viewers instead of temporarily blocking a new run.
- Fixed stale infrastructure benefit text so Quarry Network, Builder Foundry, Combat Academy and Ascension Nexus descriptions match their Lv.100 Mastery VI behavior.
- Adapted the timed sequential-wave / boss-bar encounter lifecycle from the MIT-licensed Gateways to Eternity project and packaged its full MIT notice.
- Extended source/JAR audits to require the new encounter, 26.2-compatible entity lookup, restart/stale-server cleanup, summon-residual prevention, attribution and all pre-existing Mastery VI/world/economy safety contracts.

## 0.20.0-alpha.1
- Added final Lv.100 `Mastery VI` tier across all six active skills instead of leaving Lv.91-100 as mostly numeric progression.
- Mining Lv.100: 11x11 excavation, 192 connected/extract ore cap, and Quarry Network tunnel grows from 5x5x8 to 7x7x10.
- Woodcutting Lv.100: natural-tree chain cap grows from 256 to 384 logs while retaining leaf safety and tick-drained work.
- Harvesting Lv.100: mature-crop area grows from 9x9 to 11x11 while irrigation still consumes real seeds/crops.
- Combat Lv.100: cleave grows to 10 targets / 5-block radius / 70%; Combat Academy sprint shockwave becomes 6.5 radius / 16 targets / 55% with a 50-tick cooldown.
- Construction Lv.100: line 49, wall/floor 11x11, Builder Foundry Volume 7x7x7. Pending cap raised to 512 but global placement remains 64 blocks/tick with the same material and protection checks.
- Mobility Lv.100: step height 2.0, safe fall 16, dash power 1.80 and 16-tick cooldown. Stage-2 Ascension Nexus raises the Lv.100 air-dash allowance to three uses per airtime.
- Skills screen now renders Mastery VI and Guide documents all Lv.100 capstones.
- Large capstones keep the existing synchronous safety model: Mining 12/player + 64/global blocks/tick; Construction remains protected and tick-distributed.
- Source audit now enforces exact Mastery VI scale values while preserving Stage-2 mutations, world ascension, warbands, elites, equipment economy and infrastructure regressions.
