# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension expands vanilla survival by making the *scale of actions* grow with the player instead of only adding small numeric upgrades.

## 0.2.0-alpha.1

The alpha.1 mining prototype has been converted into a generic skill engine and the first second profession, Woodcutting, is active.

### Mining
- persistent level 0-100 progression
- break-speed bonus applies only while using a pickaxe on pickaxe-mineable blocks
- Lv.10: 3x3
- Lv.30: 5x5
- Lv.60: 7x7
- sneak: precision 1x1
- area blocks use the normal player destroy controller for drops, durability and block-break hooks

### Woodcutting
- axe + log actions award Woodcutting XP
- Lv.10: up to 16 connected logs
- Lv.30: up to 48
- Lv.60: up to 128
- Lv.90: up to 256
- sneak: precision single-log mode
- every secondary log uses the normal player destroy controller

### Shared progression/UI
- per-player map of skill-id -> total XP
- alpha.1 `mining_xp` saves migrate into the shared skill map
- server -> client skill snapshots and XP updates
- recent-skill XP bar above the hotbar with level-up feedback
- `/ascension stats`
- `/ascension mining setlevel <0..100>`
- `/ascension woodcutting setlevel <0..100>`

## Third-party source policy

Permissively licensed source may be reused when its license is preserved. Restricted/ARR projects are reference-only. See `THIRD_PARTY_NOTICES.md`.
