# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension makes survival progression increase the scale of actions, not only numeric stats. The world now begins scaling back through progression-coupled elite enemies.

## 0.9.0-alpha.1

### Six live skills
- Mining: 3x3 / 5x5 / 7x7 / 9x9 excavation and connected valuable-ore extraction.
- Woodcutting: connected-log felling up to 16 / 48 / 128 / 256.
- Harvesting: mature-only XP and 3x3 / 5x5 / 7x7 / 9x9 hoe harvesting.
- Combat: kill XP, damage growth to ~1.8x, hostile-only melee cleave 2 / 4 / 8 targets.
- Construction: M -> Construction selects Single / Line / Wall / Floor; line grows 5 / 9 / 17 / 33 and wall/floor grows 3x3 / 5x5 / 9x9.
- Mobility: real on-foot sprint-distance XP, traversal attributes, Lv.30 R ground dash, Lv.60 one air dash before landing, stronger endgame traversal at Lv.90.

### Progression-scaled elite world
- Hostile mobs near players can become Elite I, Ascended II or Mythic III.
- Spawn chance and higher-rank odds scale from the nearby group's average level across all six skills.
- Rank bonuses affect health, armor, attack, movement and knockback resistance together.
- Every elite receives one trait: Swift, Bulwark, Vampiric or Berserker.
- Vampiric enemies heal from real player health damage; Berserkers become more dangerous below half health.
- Mythic enemies announce their appearance nearby and award extra vanilla XP when killed.
- Spawner-origin mobs are excluded from elite assignment to block repeatable reward farming.
- Rank and trait persist in entity NBT; rank attributes use permanent modifiers.

### UI / controls
- M: MineMenu MIT-derived integrated radial menu.
- M -> Construction: Single / Line / Wall / Floor radial.
- R: Mobility action / dash.
- Shift: precision single-work override for scaled destructive/building actions.
- Skill/guide screens follow Skill Proficiencies MIT information architecture.

## Third-party source policy

Permissively licensed source may be reused with preserved notices. Skill Proficiencies, Veinminer++, MineMenu, Building Gadgets 2 and Mob Champions MIT notices are packaged in the runtime JAR. Restricted/copyleft/custom-license projects such as Project MMO and ParCool remain reference-only unless their license obligations are deliberately adopted. See `THIRD_PARTY_NOTICES.md`.
