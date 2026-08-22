# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension makes survival progression increase the *scale of actions*, not only their numeric stats.

## 0.5.0-alpha.1

### Mining
- 3x3 / 5x5 / 7x7 / 9x9 terrain excavation at Lv.10 / 30 / 60 / 90
- connected valuable-ore extraction: 24 / 64 / 128 blocks at Lv.30 / 60 / 90
- sneak keeps precision 1x1

### Woodcutting
- connected logs: 16 / 48 / 128 / 256 at Lv.10 / 30 / 60 / 90

### Harvesting
- mature-only XP and 3x3 / 5x5 / 7x7 / 9x9 hoe harvesting

### Combat
- kills award Combat XP; hostile mobs are weighted much higher than passive mobs
- outgoing player damage scales smoothly to about 1.8x at Lv.100
- melee cleave unlocks at Lv.30 / 60 / 90
- cleave hits up to 2 / 4 / 8 nearby hostile enemies with 25% / 42% / 60% propagated damage
- cleave radius grows to 1.75 / 2.75 / 4.0 blocks
- ranged attacks receive the damage multiplier but never trigger melee cleave
- recursion guard prevents cleave damage from creating another cleave chain

### Shared progression/UI
- Mining, Woodcutting, Harvesting and Combat are active
- Construction and Mobility remain reserved
- K opens the synchronized six-skill overview
- `/ascension stats`
- `/ascension mining|woodcutting|harvesting|combat setlevel <0..100>`

## Third-party source policy

Permissively licensed source may be reused when its license is preserved. Skill Proficiencies and Veinminer++ MIT notices are packaged in the runtime JAR. Restricted/ARR projects remain reference-only. See `THIRD_PARTY_NOTICES.md`.
