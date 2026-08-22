# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension expands vanilla survival by making the *scale of actions* grow with the player instead of only adding small numeric upgrades.

## 0.4.0-alpha.1

### Mining
- persistent level 0-100 progression
- pickaxe-scoped speed growth
- terrain excavation: Lv.10 3x3, Lv.30 5x5, Lv.60 7x7, Lv.90 9x9
- valuable ores switch from flat excavation to connected-vein extraction at Lv.30+
- connected ore caps: 24 / 64 / 128 at Lv.30 / 60 / 90
- common stone/deepslate ore variants are treated as one ore family
- sneak: precision 1x1 and disables automatic vein extraction
- all secondary breaks remain on the normal player destroy controller

### Woodcutting
- axe + log actions award Woodcutting XP
- connected-log limits: 16 / 48 / 128 / 256 at Lv.10 / 30 / 60 / 90
- sneak: precision single-log mode

### Harvesting
- only mature crops, mature nether wart, melons and pumpkins award Harvesting XP
- hoe harvest speed scales with Harvesting level
- hoe area harvest: 3x3 @10, 5x5 @30, 7x7 @60, 9x9 @90
- hand harvesting remains vanilla-sized; sneak forces precision harvesting

### Shared progression/UI
- six skill slots: Mining, Woodcutting, Harvesting, Combat, Construction, Mobility
- K opens a synchronized six-skill overview screen
- Mining now shows excavation scale, vein cap, speed and XP progress together
- five generic mastery tiers provide the shared foundation for later true tool-tier, enchantment and content unlocks
- recent-skill XP bar and level-up feedback remain active
- `/ascension stats`
- `/ascension mining|woodcutting|harvesting setlevel <0..100>`

## Third-party source policy

Permissively licensed source may be reused when its license is preserved. Skill Proficiencies and Veinminer++ MIT notices are packaged in the runtime JAR. Restricted/ARR projects remain reference-only. See `THIRD_PARTY_NOTICES.md`.
