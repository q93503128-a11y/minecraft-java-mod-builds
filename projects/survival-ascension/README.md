# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension turns progression into larger physical actions, then scales enemies, world stages and shared infrastructure back against that growth.

## 0.21.0-alpha.1 — Ascension Trial
Stage 2 now has a repeatable endgame combat loop instead of ending after the Ascension Nexus is completed.

- Complete the Stage-2 Ascension Nexus, then select the completed Nexus again from `M -> Infrastructure` to open an Ascension Trial.
- Entry consumes real resources every run: 32 Echo Shards + 64 Amethyst Shards + 8 Dragon's Breath.
- Four timed waves use mixed vanilla combat roles rather than another blanket HP-sponge layer: undead pressure, ranged slow/support, illager/wither pressure, then a Ravager/Evoker/Vindicator end wave.
- Each wave has a 60-second limit and a 5-second inter-wave setup. A vanilla boss bar shows wave, remaining enemies and time.
- The owner gets a 10-second grace window for death/dimension/64-block arena departure. Active trials require 96 blocks of separation and have a 120-second start cooldown to prevent spam.
- Successful completion guarantees one Mythic III affix item, 2 Netherite Scraps, 4 Diamonds and 200 XP; nearby helpers receive XP without duplicating the owner loot package.
- Triggered trial mobs still pass through normal NeoForge spawn finalization, so Stage-2 mutations, Elite ranks and existing tactical systems may layer onto the encounter naturally.
- Fixed stale infrastructure benefit text so Lv.100 Quarry / Builder Foundry / Ascension Nexus descriptions match the actual Mastery VI rules.

The timed-wave lifecycle and boss-bar encounter information model are adapted from the MIT-licensed Gateways to Eternity project; runtime attribution is packaged in the JAR.

## Existing Mastery VI scale
- Mining: 11x11 excavation, vein/extract cap 192, Quarry Network Tunnel 7x7x10.
- Woodcutting: connected natural-tree cap 384 logs with smart-tree leaf safety and tick draining.
- Harvesting: mature-crop area 11x11; irrigation still consumes real seeds/crops.
- Combat: cleave 10 targets / 5-block radius / 70%. Combat Academy sprint shockwave 6.5 radius / 16 targets / 55% with a 50-tick cooldown.
- Construction: line 49, wall/floor 11x11; Builder Foundry Volume 7x7x7. Placement still consumes real materials and uses the protected tick queue.
- Mobility: 2-block step height, 16-block safe fall, dash power 1.80 / 16 ticks. Stage-2 Ascension Nexus upgrades Lv.100 to three air dashes before landing.

Large capstones do not increase synchronous server work budgets: Mining stays 12 blocks/player/tick and 64 globally; Construction stays 64 globally with material/protection validation.

M main radial: Skills / Mining / Construction / Equipment / Infrastructure / Guide / Close. Shift remains the precision override for scaled work.
