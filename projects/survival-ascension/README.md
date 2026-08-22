# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension expands vanilla survival by making the *scale of actions* grow with the player instead of only adding small numeric upgrades.

## 0.3.0-alpha.1

### Mining
- persistent level 0-100 progression
- pickaxe-scoped speed growth
- Lv.10: 3x3, Lv.30: 5x5, Lv.60: 7x7
- sneak: precision 1x1

### Woodcutting
- axe + log actions award Woodcutting XP
- connected-log limits: 16 / 48 / 128 / 256 at Lv.10 / 30 / 60 / 90
- sneak: precision single-log mode

### Harvesting
- only mature crops, mature nether wart, melons and pumpkins award Harvesting XP
- hoe harvest speed scales with Harvesting level
- hoe area harvest: 3x3 @10, 5x5 @30, 7x7 @60, 9x9 @90
- hand harvesting remains vanilla-sized; sneak forces precision harvesting
- every secondary crop uses the normal player destroy controller

### Shared progression/UI
- six skill slots: Mining, Woodcutting, Harvesting, Combat, Construction, Mobility
- K opens a synchronized six-skill overview screen
- active professions show their current action scale, speed and XP progress
- five generic mastery tiers provide a shared progression foundation for later tool/enchant unlocks
- recent-skill XP bar and level-up feedback remain active
- `/ascension stats`
- `/ascension mining|woodcutting|harvesting setlevel <0..100>`

## Third-party source policy

Permissively licensed source may be reused when its license is preserved. Restricted/ARR projects are reference-only. See `THIRD_PARTY_NOTICES.md`.
