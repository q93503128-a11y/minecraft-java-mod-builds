# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension turns progression into larger physical actions, then scales enemies, world stages and shared infrastructure back against that growth.

## 0.20.0-alpha.1 — Mastery VI
Lv.100 is now a distinct final mastery tier instead of ten mostly-numeric levels after Lv.90.

- Mining: 11x11 excavation, vein/extract cap 192, Quarry Network Tunnel grows from 5x5x8 to 7x7x10.
- Woodcutting: connected natural-tree cap grows from 256 to 384 logs while retaining smart-tree leaf safety and tick draining.
- Harvesting: mature-crop area grows from 9x9 to 11x11; irrigation still consumes real seeds/crops.
- Combat: cleave grows to 10 targets / 5-block radius / 70% scaled damage. Combat Academy sprint shockwave becomes 6.5 radius / 16 targets / 55% with a 50-tick cooldown.
- Construction: line 49, wall/floor 11x11; Builder Foundry Volume becomes 7x7x7. Placement still consumes real materials and uses the protected tick queue.
- Mobility: 2-block step height, 16-block safe fall, stronger dash with 16-tick cooldown. Stage-2 Ascension Nexus upgrades the Lv.100 air-dash allowance to three uses before landing.
- Skills UI displays mastery tier VI at Lv.100.

Large capstones do not increase synchronous server work budgets: Mining stays 12 blocks/player/tick and 64 globally; Construction stays 64 globally with material/protection validation.

## Existing world/endgame loop
- Stage 0 Awakening -> Wither: Stage 1 Legendary -> Ender Dragon: Stage 2 Endgame.
- Elite I / Ascended II / Mythic III traits and tactical Leader / Bruiser / Hunter / Support warbands scale with world stage.
- Stage 2 natural zombies/skeletons can gain Withered / Phase / Plague mutations; spawner mobs are excluded.
- Affix gear, reforge/salvage, Quarry Network, Irrigation Works, Builder Foundry, Combat Academy and Ascension Nexus remain active.

M main radial: Skills / Mining / Construction / Equipment / Infrastructure / Guide / Close. Shift remains the precision override for scaled work.