# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension makes survival progression increase the scale of actions, not only numeric stats.

## 0.7.0-alpha.1

### Live progression
- Mining: 3x3 / 5x5 / 7x7 / 9x9 excavation and connected valuable-ore extraction.
- Woodcutting: connected-log felling up to 16 / 48 / 128 / 256.
- Harvesting: mature-only XP and 3x3 / 5x5 / 7x7 / 9x9 hoe harvesting.
- Combat: kill XP, damage growth to ~1.8x, hostile-only melee cleave 2 / 4 / 8 targets.
- Construction: placing blocks grants XP; M -> Construction selects Single / Line / Wall / Floor. Line grows 5 / 9 / 17 / 33, while wall/floor grows 3x3 / 5x5 / 9x9.

### Construction safety
- Secondary placements consume real inventory blocks unless creative.
- Shift always forces precision single placement.
- Multi-block vanilla placements such as doors/beds are not recursively expanded.
- Bulk placement checks player interaction permission and NeoForge placement hooks.
- Large jobs are spread over server ticks with a global work budget and per-player pending cap.

### M radial menu
- M is the integrated menu key.
- MineMenu MIT radial geometry/presentation is adapted for the main and construction wheels.
- Main entries: Skills, Construction, Guide, Unlocks, Stats, Controls, Close.
- The live world remains visible behind the radial wheel.
- Guide/skills screens use native Minecraft UI patterns adapted from Skill Proficiencies MIT.

## Third-party source policy

Permissively licensed source may be reused with preserved notices. Skill Proficiencies, Veinminer++, MineMenu and Building Gadgets 2 MIT notices are packaged in the runtime JAR. Restricted/ARR projects remain reference-only. See `THIRD_PARTY_NOTICES.md`.
