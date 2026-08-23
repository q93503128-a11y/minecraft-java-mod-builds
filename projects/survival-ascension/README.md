# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension turns progression into larger physical actions, then scales enemies, world stages and shared infrastructure back against that growth.

## 0.22.0-alpha.1 — Tactical Trials + Awakened Mythic
The Stage-2 loop now varies by behavior and enemy composition instead of replaying the same four fixed waves, and Mythic III loot now has a costly final purpose beyond ordinary rerolling.

### Ascension Trial doctrines
- Every run randomly selects one tactical doctrine: `쇄도 / 추격 / 봉쇄`.
- `쇄도` emphasizes melee pressure with Husk, Vindicator, Wither Skeleton and Ravager-heavy compositions.
- `추격` uses Spider, Enderman, Husk, Wither Skeleton and Vindicator pressure; distant pursuit mobs are actively driven back toward the owner instead of idling at the edge of the arena.
- `봉쇄` emphasizes Skeleton/Stray/Pillager/Witch control and ranged lane pressure.
- Each wave triggers one bounded doctrine-specific reinforcement when the initial force falls to roughly half strength. The wave timer is not extended, so reinforcement changes the fight instead of becoming free extra loot time.
- No doctrine adds a new blanket HP multiplier. Existing Elite ranks, Stage-2 mutations and Warband roles can still layer onto trial mobs through the normal triggered spawn path.
- Evokers remain excluded from direct trial compositions so Vex cannot become untracked residual entities after success/failure.

### Awakened Mythic gear
- `M -> Equipment` now contains `신화 각성` inside the existing MineMenu-derived radial instead of adding a separate rectangular GUI.
- A normal Mythic III item has 3 affixes. One-time awakening preserves all existing affixes and adds one missing affix for a 4-affix Awakened Mythic item.
- Awakening cost: 256 Amethyst Shards + 24 Diamonds + 8 Netherite Scraps + 64 Echo Shards + 16 Dragon's Breath.
- Awakened Mythic rerolls preserve the four-affix state and cost 128 Amethyst Shards + 16 Diamonds + 4 Netherite Scraps + 16 Echo Shards per reroll.
- Affixes still cannot unlock scaled skill actions early; they only amplify actions already unlocked by the relevant skill level.
- Awakening uses the existing affix CustomData format, so no new custom item registry or texture dependency is introduced.

## Existing Stage-2 loop
- Complete the Ascension Nexus, then re-select it from `M -> Infrastructure` to open an Ascension Trial.
- Entry consumes 32 Echo Shards + 64 Amethyst Shards + 8 Dragon's Breath.
- Four waves, 60 seconds per wave, 5-second setup between waves, boss-bar wave/enemy/time state.
- Owner death/dimension/64-block departure has a 10-second grace window; active trials require 96-block separation and a 120-second start cooldown.
- Completion guarantees one Mythic III affix item, 2 Netherite Scraps, 4 Diamonds and 200 XP; nearby helpers receive XP without duplicating owner loot.
- Restart/stale-server guards reject orphan tagged trial mobs and clear old in-JVM trial references.

The timed-wave lifecycle, boss-bar encounter model and the idea of wave-changing modifiers are adapted at a high level from the MIT-licensed Gateways to Eternity project; runtime attribution remains packaged in the JAR.

## Existing Mastery VI scale
- Mining: 11x11 excavation, vein/extract cap 192, Quarry Network Tunnel 7x7x10.
- Woodcutting: connected natural-tree cap 384 logs with smart-tree leaf safety and tick draining.
- Harvesting: mature-crop area 11x11; irrigation still consumes real seeds/crops.
- Combat: cleave 10 targets / 5-block radius / 70%. Combat Academy sprint shockwave 6.5 radius / 16 targets / 55% with a 50-tick cooldown.
- Construction: line 49, wall/floor 11x11; Builder Foundry Volume 7x7x7. Placement still consumes real materials and uses the protected tick queue.
- Mobility: 2-block step height, 16-block safe fall, dash power 1.80 / 16 ticks. Stage-2 Ascension Nexus upgrades Lv.100 to three air dashes before landing.

Large capstones do not increase synchronous server work budgets: Mining stays 12 blocks/player/tick and 64 globally; Construction stays 64 globally with material/protection validation.

M main radial: Skills / Mining / Construction / Equipment / Infrastructure / Guide / Close. Shift remains the precision override for scaled work.
