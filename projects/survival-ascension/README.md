# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension turns survival progression into larger physical actions, then scales enemies and loot back against that growth.

## 0.11.0-alpha.1

### Six live skills
- Mining: 3x3 / 5x5 / 7x7 / 9x9 excavation + connected valuable-ore extraction.
- Woodcutting: connected-log felling 16 / 48 / 128 / 256.
- Harvesting: mature-crop 3x3 / 5x5 / 7x7 / 9x9 harvesting.
- Combat: damage growth + hostile melee cleave 2 / 4 / 8 targets.
- Construction: M -> Construction -> Single / Line / Wall / Floor; line 5 / 9 / 17 / 33 and planes 3x3 / 5x5 / 9x9.
- Mobility: sprint-distance XP, Lv.30 R dash, Lv.60 air dash, Lv.90 endgame traversal.

### Reactive elite world
- Elite I / Ascended II / Mythic III scale from nearby players' average six-skill progression.
- Swift evades, Bulwark counter-pushes, Vampiric heals from real damage, Berserker lunges and gains low-health damage.
- Spawner-origin mobs are excluded from elite assignment.
- Elite kills retain rank XP/material rewards.

### Affix equipment
Apotheosis MIT-inspired rarity/category/affix separation is adapted to Survival Ascension's own 26.2 CustomData system.

- Elite I: 25% affix gear chance, iron base, 1 affix.
- Ascended II: 65%, diamond base, 2 affixes.
- Mythic III: 100%, netherite base, 3 affixes.
- Weapon: Destruction (damage), Cleave (already-unlocked cleave scale), Mastery (Combat XP).
- Pickaxe: Haste, Excavation/vein scale, Mining Mastery.
- Axe: Haste, connected-log scale, Woodcutting Mastery.
- Hoe: Haste, harvest-area scale, Harvesting Mastery.
- Scale affixes do not unlock 3x3/cleave/chain systems early. The matching skill must unlock the scaled action first.
- Affix rarity and names are visible directly in the item's custom name.

### Controls
- M: integrated radial menu.
- R: Mobility dash/action.
- Shift: precision override for scaled work.

## Third-party policy
MIT code/patterns may be adapted with notices preserved. Runtime notices currently cover Skill Proficiencies, Veinminer++, MineMenu, Building Gadgets 2, Mob Champions and Apotheosis. Project MMO, ParCool, Majrusz's Progressive Difficulty and other non-permissive/unadopted sources remain reference-only. See `THIRD_PARTY_NOTICES.md`.
