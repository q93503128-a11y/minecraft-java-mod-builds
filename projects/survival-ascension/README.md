# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension makes survival progression increase the scale of actions, not only numeric stats.

## 0.8.0-alpha.1

### Six live skills
- Mining: 3x3 / 5x5 / 7x7 / 9x9 excavation and connected valuable-ore extraction.
- Woodcutting: connected-log felling up to 16 / 48 / 128 / 256.
- Harvesting: mature-only XP and 3x3 / 5x5 / 7x7 / 9x9 hoe harvesting.
- Combat: kill XP, damage growth to ~1.8x, hostile-only melee cleave 2 / 4 / 8 targets.
- Construction: M -> Construction selects Single / Line / Wall / Floor; line grows 5 / 9 / 17 / 33 and wall/floor grows 3x3 / 5x5 / 9x9.
- Mobility: real on-foot sprint-distance XP, traversal attributes, Lv.30 R ground dash, Lv.60 one air dash before landing, stronger endgame traversal at Lv.90.

### Mobility
- Passive movement speed growth is deliberately modest; the large power spikes are new movement actions.
- Lv.10: step height 1.0 and safer falls.
- Lv.30: R ground dash, 3 second base cooldown.
- Lv.60: R can be used once in the air before touching ground again; ground/air dash are stronger and cooldown drops to 2 seconds.
- Lv.90: larger impulse, 1.2 second cooldown, 1.5-block step height and 12-block safe fall distance.
- Server validates level, cooldown, grounded/air state and forbidden states before applying movement.

### UI / controls
- M: MineMenu MIT-derived integrated radial menu.
- M -> Construction: Single / Line / Wall / Floor radial.
- R: Mobility action / dash.
- Shift: precision single-work override for scaled destructive/building actions.
- Skill/guide screens follow Skill Proficiencies MIT information architecture.

## Third-party source policy

Permissively licensed source may be reused with preserved notices. Skill Proficiencies, Veinminer++, MineMenu and Building Gadgets 2 MIT notices are packaged in the runtime JAR. Restricted/copyleft/custom-license projects such as Project MMO and ParCool remain reference-only unless their license obligations are deliberately adopted. See `THIRD_PARTY_NOTICES.md`.
