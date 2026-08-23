# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension turns progression into larger physical actions, then scales enemies, world stages, exploration goals and shared infrastructure back against that growth.

## 0.23.0-alpha.1 — Nine-region Expeditions + Field Mastery
Exploration now advances the same physical-scale progression instead of existing as a disconnected biome checklist.

### Nine expedition regions
- Per-player `expedition_v1` SavedData records first discovery of nine broad vanilla-world expedition regions.
- Stage 0: `삼림권 / 건조권 / 습지권 / 고산권 / 대양권`.
- Stage 1 adds `심층권 / 빙설권 / 네더권`.
- Stage 2 adds `엔드권`.
- Discovery is checked server-side once per second from the player's actual biome; creative/spectator movement cannot claim discoveries or rewards.
- Each first discovery grants skill XP tied to the terrain: forests train Woodcutting, deep caves train Mining, Nether/End train Combat, highlands/oceans/frozen regions train Mobility, wetlands train Harvesting and arid regions train Construction.
- Progress and the colored region list are visible through `/ascension stats`; login also reports the survey count.

### Per-player expedition milestones
- 4 of the five Stage-0 regions: 4 Diamonds + 16 Emeralds + 32 Amethyst Shards.
- Stage 1, seven total including both Deep and Nether: 2 Netherite Scraps + 16 Diamonds + 32 Echo Shards.
- Stage 2, all nine: guaranteed Mythic III + 4 Netherite Scraps + 64 Echo Shards + 16 Dragon's Breath + 500 XP.
- Milestone claim bits are persisted per player, so multiplayer explorers do not consume one another's rewards and re-entering a biome cannot duplicate them.

### Field Mastery
Completing all nine regions at Stage 2 unlocks a final Lv.100 physical-scale layer without removing the normal Mastery VI capstones.

- Mining: Quarry Network tunnel `7x7x10 -> 7x7x12`. The queue cap grows only enough to hold one tunnel; destruction remains 12 blocks/player/tick and 64 globally.
- Woodcutting: natural-tree chain `384 -> 448` logs; the existing 12/player and 64/global tick drain remains.
- Harvesting: `11x11 -> 13x13`; bulk harvesting is now tick-drained at 12/player and 64/global instead of synchronously breaking the whole field.
- Combat: Combat Academy sprint shockwave `6.5 radius / 16 targets -> 7.5 / 20`; damage fraction and cooldown do not increase.
- Construction: line `49 -> 65`, wall/floor `11x11 -> 13x13`; Builder Foundry volume remains `7x7x7`, real-material/protection checks and 64 global placements/tick remain.
- Mobility: Stage-2 Ascension Nexus Lv.100 air dashes `3 -> 4`; landing reset and the existing dash cooldown remain.

The exploration design deliberately reuses vanilla biomes rather than shipping low-quality filler structures. Lootr's per-player exploration-reward fairness is used as a design reference; Repurposed Structures and the Compass mods are reference-only for exploration motivation. No source/assets from those reference-only projects are bundled.

## 0.22.0-alpha.1 — Tactical Trials + Awakened Mythic
The Stage-2 loop varies by behavior and enemy composition instead of replaying the same four fixed waves, and Mythic III loot has a costly final purpose beyond ordinary rerolling.

### Ascension Trial doctrines
- Every run randomly selects one tactical doctrine: `쇄도 / 추격 / 봉쇄`.
- `쇄도` emphasizes melee pressure with Husk, Vindicator, Wither Skeleton and Ravager-heavy compositions.
- `추격` uses Spider, Enderman, Husk, Wither Skeleton and Vindicator pressure; distant pursuit mobs are actively driven back toward the owner instead of idling at the edge of the arena.
- `봉쇄` emphasizes Skeleton/Stray/Pillager/Witch control and ranged lane pressure.
- Each wave triggers one bounded doctrine-specific reinforcement when the initial force falls to roughly half strength. The wave timer is not extended.
- No doctrine adds a new blanket HP multiplier. Existing Elite ranks, Stage-2 mutations and Warband roles can still layer onto trial mobs through the normal triggered spawn path.
- Evokers remain excluded from direct trial compositions so Vex cannot become untracked residual entities after success/failure.

### Awakened Mythic gear
- `M -> Equipment` contains `신화 각성` inside the existing MineMenu-derived radial.
- A valid normal Mythic III item has exactly 3 affixes. One-time awakening preserves all three and adds one missing affix for a 4-affix Awakened Mythic item.
- Awakening validates the item before consuming resources.
- Awakening cost: 256 Amethyst Shards + 24 Diamonds + 8 Netherite Scraps + 64 Echo Shards + 16 Dragon's Breath.
- Awakened Mythic rerolls preserve four affixes and cost 128 Amethyst Shards + 16 Diamonds + 4 Netherite Scraps + 16 Echo Shards.
- Affixes still cannot unlock scaled skill actions early; they only amplify actions already unlocked by the relevant skill level.

## Existing Stage-2 loop
- Complete the Ascension Nexus, then re-select it from `M -> Infrastructure` to open an Ascension Trial.
- Entry consumes 32 Echo Shards + 64 Amethyst Shards + 8 Dragon's Breath.
- Four waves, 60 seconds per wave, 5-second setup between waves, boss-bar wave/enemy/time state.
- Owner death/dimension/64-block departure has a 10-second grace window; active trials require 96-block separation and a 120-second start cooldown.
- Completion guarantees one Mythic III affix item, 2 Netherite Scraps, 4 Diamonds and 200 XP; nearby helpers receive XP without duplicating owner loot.
- Restart/stale-server guards reject orphan tagged trial mobs and clear old in-JVM trial references.

## Base Mastery VI scale before Field Mastery
- Mining: 11x11 excavation, vein/extract cap 192, Quarry Network Tunnel 7x7x10.
- Woodcutting: connected natural-tree cap 384 logs.
- Harvesting: mature-crop area 11x11; irrigation still consumes real seeds/crops.
- Combat: cleave 10 targets / 5-block radius / 70%; Combat Academy shockwave 6.5 / 16 / 55% / 50 ticks.
- Construction: line 49, wall/floor 11x11; Builder Foundry Volume 7x7x7.
- Mobility: 2-block step height, 16-block safe fall, dash power 1.80 / 16 ticks; Stage-2 Nexus gives three Lv.100 air dashes.

M main radial: Skills / Mining / Construction / Equipment / Infrastructure / Guide / Close. Shift remains the precision override for scaled work.
