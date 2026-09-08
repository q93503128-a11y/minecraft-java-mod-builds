# M3 Combat Reference Dossier

Status: architecture/reference gate only. This document does **not** approve final creature art, animations, VFX, sounds, or UI.

## Why this exists

M3 must replace M2 technical proxy combat with production combat without collapsing into HP multipliers or opaque animation logic. The production boundary is therefore authored as reusable `attack_pattern` data plus `boss_profile` composition before final assets are chosen.

## Reference lessons

### Mowzie's Mobs
Small creature count, high encounter craft: bespoke animation, recognizable attack patterns, and rewards tied to encounter identity. Riftfrontier adopts the principle that a boss needs readable mechanics and a reward/progression reason, not the assets or code.

### Alex's Caves
Region identity is built as a dense package rather than isolated mobs. Combat presentation must belong to the Region Pack's environment, sound and traversal identity. Riftfrontier therefore keeps boss arena rules in the boss policy and final presentation in a separate resolver/gate.

### Advent of Ascension
Useful as an architecture study for long-lived RPG boss/state/attack separation, but its repository license is restrictive. Riftfrontier uses only the design lesson; no source is copied.

### Hades II
Supergiant repeatedly differentiates weapons and Guardians through fighting style and encounter-specific surprises. Riftfrontier adopts the principle that timing/counterplay and role differentiation matter more than adding many superficially distinct attacks.

## Production combat contract

Every production attack pattern must define:

- stable content ID
- delivery semantics (`arc_melee`, `line_charge`, `projectile_burst`, `area_denial`, etc.)
- positive telegraph window
- positive active window
- non-negative recovery window
- at least one explicit player counterplay token
- presentation cue contract describing what animation/VFX/sound must communicate before the hit becomes active

`telegraph -> active -> recovery` is data policy. Runtime animation and hit volume must follow the same timing source instead of maintaining independent magic numbers.

Every production boss profile must define:

- stable boss content ID
- phase count
- references to reusable attack patterns
- arena rule that affects combat readability/positioning

A boss profile with no attack patterns, dangling attack references, or no arena rule is a validator ERROR.

## What is intentionally not locked yet

- Region 01 boss name and visual theme
- model geometry / texture palette
- GeckoLib adoption and exact animation set
- sound library and VFX language
- player weapon art
- exact damage, range and cooldown balance

Those require actual reference/asset work and Minecraft-screen validation. This dossier only locks the data/runtime contract needed to build them safely.

## Next implementation boundary

After M2 field-play evidence is available, use the observed combat pacing to author the first Region 01 production `attack_pattern` definitions and boss phase/state runtime. Do not migrate M2 Zombie/Skeleton/Ravager technical proxies into production art by renaming them.

## Sources consulted

- Mowzie's Mobs project overview: https://github.com/MiniGuardRetrieve/mowzies-mobs
- Alex's Caves official repository: https://github.com/AlexModGuy/AlexsCaves
- Advent of Ascension official repository: https://github.com/Tslat/Advent-Of-Ascension
- Supergiant Games, Hades II updates: https://www.supergiantgames.com/blog/hades2-unseen-update/ and https://www.supergiantgames.com/blog/hades2-warsong-update/
