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

- Region 01 boss final name and species/lore identity
- model geometry / texture palette
- final selected model/rig asset
- sound library and VFX language
- player weapon art
- exact damage, range and cooldown balance

Those require actual reference/asset work and Minecraft-screen validation.

## Region 01 presentation gate — 2026-09-09

The architecture-only phase is now closed. `REGION_01_BOSS_PRESENTATION_GATE.md` locks the next production criteria:

- first boss combat communication is position-control / commitment-punish oriented rather than a high-HP proxy;
- broad committed strike, line/displacement pressure and visible arena-pressure roles must each have readable telegraph/ACTIVE/recovery presentation;
- model candidates must pass facing, attack-bearing silhouette, rig headroom, Minecraft scale/style, variant stability and performance checks;
- VFX/sound reinforce the authoritative `AttackPattern` clock and may not invent a parallel hit clock;
- production asset bundling is fail-closed until a real candidate passes the visual/rig/license gate.

External research identified Quaternius `Ultimate Monsters` as a license-eligible CC0 animated source family, but no individual model is selected merely because it is free. GeckoLib 5.5.1 remains the current Minecraft 26.2 animation technology candidate and is not added as a dependency until an accepted real animated asset justifies it.

`THIRD_PARTY_ASSETS.md` now exists as the project asset/provenance registry.

## Next implementation boundary

Do not add more presentation plumbing. Inspect a bounded set of real license-eligible candidate models/rigs against `REGION_01_BOSS_PRESENTATION_GATE.md`. Only after one candidate is visually and technically accepted should the repository:

1. mark the exact asset `SELECTED` in `THIRD_PARTY_ASSETS.md`;
2. add permitted source/derived asset bytes;
3. re-verify and, if justified, add the chosen animation technology;
4. author the first real `presentation_assets` manifest;
5. connect the existing render-facing resolver to the chosen renderer;
6. verify telegraph/ACTIVE/recovery presentation against real hit windows in Minecraft.

Do not migrate M2 Zombie/Skeleton/Ravager technical proxies into production art by renaming them.

## Sources consulted

- Mowzie's Mobs project overview: https://github.com/MiniGuardRetrieve/mowzies-mobs
- Alex's Caves official repository: https://github.com/AlexModGuy/AlexsCaves
- Advent of Ascension official repository: https://github.com/Tslat/Advent-Of-Ascension
- Supergiant Games, Hades II updates: https://www.supergiantgames.com/blog/hades2-unseen-update/ and https://www.supergiantgames.com/blog/hades2-warsong-update/
- Quaternius Ultimate Monsters: https://quaternius.com/packs/ultimatemonsters.html
- GeckoLib 5 support table: https://wiki.geckolib.com/docs/geckolib5/
