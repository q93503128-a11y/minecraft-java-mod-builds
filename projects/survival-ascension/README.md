# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension turns survival progression into larger physical actions, then scales enemies, loot and resource sinks back against that growth.

## 0.13.0-alpha.1

### Six live skills
- Mining: 3x3 / 5x5 / 7x7 / 9x9 excavation, connected veins, and a Lv.90 target-filtered Extract mode.
- Woodcutting: connected-log felling 16 / 48 / 128 / 256.
- Harvesting: mature-crop 3x3 / 5x5 / 7x7 / 9x9 harvesting.
- Combat: damage growth + hostile melee cleave 2 / 4 / 8 targets.
- Construction: M -> Construction -> Single / Line / Wall / Floor; line 5 / 9 / 17 / 33 and planes 3x3 / 5x5 / 9x9.
- Mobility: sprint-distance XP, Lv.30 R dash, Lv.60 air dash, Lv.90 endgame traversal.

### Mining modes
M -> Mining opens a MineMenu-derived radial.
- Auto: valuable ore uses connected vein extraction; normal blocks use excavation.
- Plane (Lv.10): always use the current view-aligned excavation plane.
- Vein (Lv.30): only connected same-family valuable ore is expanded.
- Extract (Lv.90): searches a bounded 12-block horizontal/vertical radius for the same valuable ore family even when deposits are not connected.
- Extract only searches already loaded chunks, excludes block entities, requires the held pickaxe to harvest every target, honors the normal destroy controller, and stops at the normal skill/affix vein limit.
- Shift always overrides every mining mode back to one block.
- The target-filtered bounded-search idea is adapted from Mekanism's MIT-licensed Digital Miner design; no Mekanism assets or machine systems are bundled.

### Reactive elites + affix equipment economy
- Elite I / Ascended II / Mythic III scale from player progression and use Swift/Bulwark/Vampiric/Berserker reactive patterns.
- Elite gear rolls 1 / 2 / 3 affixes from category-specific five-affix pools.
- M -> Equipment provides server-authoritative Reforge / Salvage / Gear Info.
- Mythic reforge consumes 64 amethyst + 12 diamonds + 2 netherite scraps, keeping late resource production relevant.

### Controls
- M: integrated radial menu (skills / mining / construction / equipment / guide / unlocks / stats / controls).
- R: Mobility dash/action.
- Shift: precision override for scaled work.

## Third-party policy
MIT code/patterns may be adapted with notices preserved. Runtime notices cover Skill Proficiencies, Veinminer++, MineMenu, Building Gadgets 2, Mob Champions, Apotheosis and Mekanism. Create code is MIT but its assets are All Rights Reserved; no Create assets are bundled. Restricted/unadopted projects remain reference-only. See `THIRD_PARTY_NOTICES.md`.
