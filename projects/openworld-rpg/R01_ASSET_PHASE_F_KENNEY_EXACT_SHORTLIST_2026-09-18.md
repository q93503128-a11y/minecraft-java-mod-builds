# Open-World RPG — R01 Asset Phase F Kenney VFX / SFX Exact Shortlist — 2026-09-18

> Canon: `GAME_DESIGN.md`  
> R01 presentation canon: `R01_VERTICAL_SLICE.md`, `STATUS_AND_R01_ENCOUNTERS.md`, `R01_UI_PRODUCTION_SPEC.md`  
> Intake: `R01_ASSET_INTAKE.md`  
> Binding matrix: `PRODUCTION_ASSET_BINDING_MATRIX.md`  
> Previous pass: `R01_ASSET_PHASE_E_APPAREL_POTION_MOTION_EXACT_REVIEW_2026-09-18.md`  
> Remote `main` observed before this pass: `e18ad2deeb37f81e7ee58fafac358fc87b8e82b5`

This pass does **not** reopen R01 combat, UI, status, boss or reward design.

Its purpose is narrower:

1. stop referring to Kenney only at pack-family level;
2. pin a small exact-file shortlist for ordinary R01 baseline presentation;
3. record actual binary identity where the environment can read the file bytes;
4. explicitly preserve the rule that Earthloong / signature boss presentation and shaped status VFX cannot be reduced to one generic Kenney file.

The official Kenney pack pages remain the intended acquisition source and licensing authority already recorded in project intake:

- Particle Pack — CC0;
- Smoke Particles — CC0;
- RPG Audio — CC0;
- Impact Sounds — CC0;
- UI Audio — CC0;
- Interface Sounds — CC0.

Public Git mirrors in this pass are used only to corroborate exact filenames, byte identity, dimensions and clip duration. They are not promoted above the official Kenney source.

---

## 1. Pinned corroboration snapshots

### 1.1 Kenney image library mirror

```text
repository: shorepine/kenney
commit: 3694c6879e487c108f55677be7dd2ca75b07cc3b
```

The repository describes itself as a path-stable Kenney all-in-one dump and preserves original-style pack paths. Phase F directly read the candidate PNG bytes below from this pinned revision.

### 1.2 Kenney Impact Sounds mirror

```text
repository: Boyquotes/kenney-impact-sounds-for-godot
commit: 999dd1684873f8b020a3aa5b26e713da21688924
```

Phase F directly read the Ogg Vorbis bytes for the selected impact candidates.

### 1.3 Kenney RPG Audio mirror

```text
repository: Boyquotes/kenney-rpg-audio-for-godot
commit: 22eb79bb843bbcadcaa6ed119353a33265ffad11
```

Phase F directly read the Ogg Vorbis bytes for the selected interaction/environment candidates.

### 1.4 Kenney Interface Sounds mirror

```text
repository: Calinou/kenney-interface-sounds
commit: 4596a49eaf5a533948d49a47467f606bcdea70ff
```

Its README states that the package is Kenney Interface Sounds, contains 100 interface sounds, links the official Kenney pack, and converts the original Ogg files to WAV for Godot. Therefore the WAV hashes below are **mirror-binary hashes**, not official-original-package hashes.

---

# 2. Exact VFX shortlist

No VFX row below is final merely because the PNG exists. These are compositing inputs.

## 2.1 Generic ground ring / telegraph mask

```text
2d/Particle Pack/PNG (Transparent)/circle_03.png
```

- Git blob SHA-1: `eb771ecf9e515a5fdb1da9dc4a9bfbd18f0d5e63`
- SHA-256: `ed6c6f082e666c16bcaca89f15aefb55df9bf69450090518bcae1d3d61f82936`
- size: 72,952 bytes
- dimensions: 512 × 512

Candidate use:

- generic authored ground-ring material/mask;
- ordinary readable AoE ring composition;
- possible subordinate mask for Root Breaker / authored ground effects.

Restriction:

- it is **not** by itself an accepted Earthloong telegraph;
- visible ring dimensions still must be generated to match the exact server hit area;
- boss-specific material motion, root/stone treatment and lightning layer remain separate.

## 2.2 Spark support layer

```text
2d/Particle Pack/PNG (Transparent)/spark_04.png
```

- Git blob SHA-1: `98d0f0a74ec53711d5e6ce693b53782c9ac41fd7`
- SHA-256: `3cba35b80a97c12283a8ecd41780a0412194de14562cbffc7f04e9157183f772`
- size: 83,811 bytes
- dimensions: 512 × 512

Candidate use:

- weapon/material contact spark;
- restrained support layer for Shocked/Conductive;
- small high-energy impact accent.

Restriction:

- cannot replace body-anchored electrical arcs;
- cannot become Earthloong's signature lightning by tinting and scaling one sprite.

## 2.3 Melee slash accent

```text
2d/Particle Pack/PNG (Transparent)/slash_01.png
```

- Git blob SHA-1: `c04fa2d3938827da63105172748851db86735aa6`
- SHA-256: `cb4787978122bb863866a1681af22a7dec39a4566f08c400f233740eb1d3730c`
- size: 25,465 bytes
- dimensions: 512 × 512

Candidate use:

- short-lived contact accent for accepted melee animations;
- never the attack itself.

Restriction:

- the visible weapon arc still comes from actual weapon/body motion;
- no particle-only sword swing.

## 2.4 Soft light support layer

```text
2d/Particle Pack/PNG (Transparent)/light_03.png
```

- Git blob SHA-1: `2ca26ca733eb4d2ff70b9e04aacf606a9e0222c0`
- SHA-256: `b68627734d08aab3eaf3f94718521553628c75aace1de6c74de28501c1a4c147`
- size: 100,114 bytes
- dimensions: 512 × 512

Candidate use:

- heal/cast readability support;
- shrine or reward highlight component after real world geometry is already present.

Restriction:

- not a complete shrine visual family;
- not a substitute for authored spell shape.

## 2.5 Generic magic accent

```text
2d/Particle Pack/PNG (Transparent)/magic_03.png
```

- Git blob SHA-1: `65f69d649a290e5c4e292efb288f457f8323d6b3`
- SHA-256: `c2ef5fe86cd2fd08e5b9768389ab6580b346c391db3c7c09fc9b0a63e00ec4ce`
- size: 58,093 bytes
- dimensions: 512 × 512

Candidate use:

- generic low-priority cast/interaction accent where canon does not demand a unique signature shape.

Restriction:

- no boss ultimate / Mythic / signature spell is closed by this asset.

## 2.6 Dust / hoof / landing puff

```text
2d/Smoke Particles/White puff/whitePuff06.png
```

- Git blob SHA-1: `fa9e94b8c420498e3f7f11372e5b265f94f4b90e`
- SHA-256: `055e78cfd8b476eedf0764b614efa1267abf266c187f62338fe47f30c898e6a0`
- size: 57,119 bytes
- dimensions: 400 × 383

Candidate use:

- Trail Stag hoof/landing dust;
- heavy creature landing support;
- quarry ground impact dust.

Restriction:

- dust is a support cue, not the entire landing or charge presentation.

## 2.7 Poison gas support layer

```text
2d/Smoke Particles/Gas/gas04.png
```

- Git blob SHA-1: `c54a826ac976705eaeff54d6484a0fec5cb24190`
- SHA-256: `fbb4366be98bc2e39f91889593d98d1aa10b94cc5cb1ddc1ec10be32c53d6938`
- size: 119,105 bytes
- dimensions: 586 × 629

Candidate use:

- compact Poison trail/aura support layer.

Restriction:

- must remain compact enough not to hide telegraphs or weak points;
- final Poison presentation still needs icon/body anchoring/composition review.

## 2.8 Impact flash support layer

```text
2d/Smoke Particles/Flash/flash04.png
```

- Git blob SHA-1: `8c688b579b0da62c3f78607d0eebaf9ef4f64adf`
- SHA-256: `a085badfe56e49f1cfa36d71975813a7ee84ee1c1e0eb12a7db0fa53765e3402`
- size: 87,508 bytes
- dimensions: 483 × 502

Candidate use:

- short hit-confirm flash;
- heavy impact accent when layered with the accepted model/animation.

Restriction:

- no repeated full-screen strobe;
- low-VFX mode must retain required spatial tells.

---

# 3. Exact ordinary SFX shortlist

Duration below was calculated directly from the Ogg Vorbis granule positions or WAV data chunk. Duration measurement does **not** mean the clip was auditioned.

## 3.1 Quarry / stone / root-impact baseline candidate

```text
impact_mining_002.ogg
```

Pinned mirror binary:

- Git blob SHA-1: `5bd4f3582815fe023c5ab347f03f66cedab3c46b`
- SHA-256: `059558ec751115ca6b1441e00f0adc837c1a3eeb0a066b109e0fd879fc5d0b0c`
- bytes: 11,235
- sample rate: 44,100 Hz
- duration: ~0.805 s

Candidate use:

- quarry stone strike;
- Root Breaker / stone-root impact **support layer**;
- mining/resource impact where appropriate.

Not accepted as the entire Earthloong signature impact stack.

## 3.2 Heavy metal / guard / armor impact candidate

```text
impact_metal_heavy_001.ogg
```

- Git blob SHA-1: `e510d3eb929c89f58946f9773ae9f0197cd1334f`
- SHA-256: `83554049f81f4db9209379e103c30bfa63f65c42189a03f300b045c2c82e23ae`
- bytes: 7,279
- sample rate: 44,100 Hz
- duration: ~0.359 s

Candidate use:

- shield/armor/metal-heavy contact;
- guard/parry material layer after audition.

## 3.3 Creature/body impact candidate

```text
impact_punch_medium_000.ogg
```

- Git blob SHA-1: `d8e0b890ae019533ec190dd2d1decce249ea556e`
- SHA-256: `486988aa2d6440ffc4c62a0e8ccf3c23673ba84424bd4723378d451b7255eb5c`
- bytes: 8,800
- sample rate: 44,100 Hz
- duration: ~0.431 s

Candidate use:

- ordinary blunt/body contact layer;
- creature physical hit where it survives audition.

## 3.4 Heavy soft/body landing candidate

```text
impact_soft_heavy_000.ogg
```

- Git blob SHA-1: `1c321ef698c367144d0d55b52d7a680a4b8e5958`
- SHA-256: `49e7ca88743fca974bb8676ea138b751cfd8f9033b5e7af8736c2a215d6edbc1`
- bytes: 6,570
- sample rate: 44,100 Hz
- duration: ~0.505 s

Candidate use:

- heavy body/hoof/soft-material impact support;
- large creature landing composition if the actual audition fits.

## 3.5 Small metal interaction candidate

```text
metal_click.ogg
```

- Git blob SHA-1: `1a9ee4bd7384ab493b89df0ff10ee142959490be`
- SHA-256: `9851a69d0c613e13bceef08060ecc4148f098ef487927cbebe270d642398a3b3`
- bytes: 13,615
- sample rate: 48,000 Hz
- duration: ~0.446 s

Candidate use:

- small equipment/tack/lock/latch interaction;
- Trail Stag registration physical-prop layer if audition fits.

## 3.6 Quarry timber / structure creak candidate

```text
creak_2.ogg
```

- Git blob SHA-1: `bfa3045a0acf7c5961f50e3dc68cb74b65eaac7b`
- SHA-256: `8a990afdc03aebb91d528f5385e2f95582dbfa8e2c12c71098ab01be9142294a`
- bytes: 19,012
- sample rate: 48,000 Hz
- duration: ~0.830 s

Candidate use:

- quarry wood/old support structure local one-shot;
- not a continuous ambience loop.

---

# 4. Exact UI SFX shortlist

The following hashes identify the pinned Calinou WAV conversion, not the original Kenney Ogg archive bytes.

## 4.1 Commit / success

```text
confirmation_001.wav
```

- Git blob SHA-1: `57b0731148815280947db27a3015c483c3fa24e7`
- SHA-256: `f9d0ef5a5c740c2ac250ac3876db9e4340669044a5bf78ae09192918db3e2ebc`
- bytes: 26,062
- sample rate: 44,100 Hz
- duration: ~0.295 s

Candidate use:

- compact successful transaction/selection commit;
- quest/service confirmation where the Lucifer UI does not already provide a stronger authored cue.

## 4.2 Failure / unavailable action

```text
error_001.wav
```

- Git blob SHA-1: `edcf344ee212d13797f250eb5649ce55c655d825`
- SHA-256: `d77546b0baa89f37eca6b86f59b54a654ce5afe3d5a73492d0963168cc4ab5bf`
- bytes: 30,542
- sample rate: 44,100 Hz
- duration: ~0.173 s

Candidate use:

- short non-punitive failure/unavailable state;
- must not become a harsh repeated error beep during ordinary navigation.

## 4.3 Focus / select

```text
select_001.wav
```

- Git blob SHA-1: `9b87d500823213ccb556b286bef7164a44fcabe1`
- SHA-256: `3d9e86003f8ebd1a3f6f3d8af65b6b682562a46d532ccd204dcba21305b950ff`
- bytes: 11,086
- sample rate: 44,100 Hz
- duration: ~0.062 s

Candidate use:

- restrained list/card selection;
- not every hover frame.

---

# 5. Intentionally unresolved signature layers

This pass **does not** pretend that the baseline Kenney packs close every R01 effect.

## 5.1 Earthloong electrical charge / strike

The directly inspected baseline files expose useful impact, ring, flash, smoke and spark components, but no exact selected Kenney clip in this pass is strong evidence for a dedicated:

- electrical charge rise;
- electrical crack/strike;
- body-anchored arc sequence.

Therefore:

```text
EARTHLOONG ELECTRICAL CHARGE SFX: OPEN_SIGNATURE_LAYER
EARTHLOONG ELECTRICAL STRIKE SFX: OPEN_SIGNATURE_LAYER
EARTHLOONG BODY-ANCHORED ARC VFX: OPEN_SIGNATURE_LAYER
```

Do not use `spark_04.png` plus a generic impact sound and call the boss finished.

## 5.2 Burning

Canon requires attached shaped flame/heat treatment, not generic particle spam.

No exact Phase-F Kenney PNG is promoted as the complete Burning look.

```text
BURNING FINAL SHAPED VFX: OPEN_SIGNATURE_LAYER
```

## 5.3 Chilled / Frostbite

Canon requires frost rim/crystal or ground-contact treatment.

No exact Phase-F Kenney PNG is promoted as the complete Frostbite look.

```text
CHILLED/FROSTBITE FINAL SHAPED VFX: OPEN_SIGNATURE_LAYER
```

## 5.4 Shocked / Conductive

`spark_04.png` is retained only as a support layer. Final presentation still requires short body/model-anchored electrical arcs.

```text
SHOCKED/CONDUCTIVE SUPPORT SPRITE: SHORTLISTED
SHOCKED/CONDUCTIVE BODY-ANCHOR ARC: OPEN_SIGNATURE_LAYER
```

## 5.5 Poison

`gas04.png` is a useful compact aura/trail candidate, but final opacity, body attachment, icon and overlap behavior still require Minecraft review.

```text
POISON GAS SUPPORT SPRITE: SHORTLISTED
POISON FINAL COMPOSITE: NOT ACCEPTED
```

---

# 6. What is now closed vs still open

## Closed enough to stop filename discovery

- exact baseline candidate filenames for:
  - ground ring;
  - spark;
  - slash accent;
  - soft light;
  - generic magic accent;
  - dust/landing puff;
  - poison-gas support;
  - hit flash;
  - quarry/stone impact;
  - heavy metal impact;
  - body impact;
  - heavy soft impact;
  - small metal interaction;
  - quarry creak;
  - UI confirmation;
  - UI error;
  - UI select.
- pinned mirror revision for each directly inspected binary;
- direct SHA-256 for every selected candidate above;
- dimensions for selected PNGs;
- sample rate + duration for selected clips.

## Still open

- actual audio audition and loudness matching;
- official Kenney archive project-local acquisition/hash;
- final event-to-clip assignment after audition;
- Minecraft spatial attenuation;
- client mix priority;
- Low VFX / accessibility behavior;
- actual VFX composition and timing;
- Earthloong signature electrical layer;
- Burning final shaped layer;
- Frostbite final shaped layer;
- body-anchored Shock arcs;
- exact ambience clips for Alderford / meadow / riverwood / quarry / root sections;
- real Minecraft screenshots/video and hitbox-to-visual acceptance.

---

# 7. Production mapping shortlist

| R01 presentation event | Phase F exact candidate | State |
|---|---|---|
| ordinary authored AoE ring | `circle_03.png` | SHORTLISTED, composite review pending |
| melee contact accent | `slash_01.png` | SHORTLISTED |
| generic contact spark | `spark_04.png` | SHORTLISTED |
| generic cast/heal support | `light_03.png`, `magic_03.png` | SHORTLISTED |
| Trail Stag / heavy landing dust | `whitePuff06.png` | SHORTLISTED |
| Poison trail/aura support | `gas04.png` | SHORTLISTED |
| hit flash | `flash04.png` | SHORTLISTED |
| quarry/stone/root support impact | `impact_mining_002.ogg` | SHORTLISTED, audition pending |
| heavy metal/guard contact | `impact_metal_heavy_001.ogg` | SHORTLISTED, audition pending |
| ordinary creature/body hit | `impact_punch_medium_000.ogg` | SHORTLISTED, audition pending |
| heavy body/landing layer | `impact_soft_heavy_000.ogg` | SHORTLISTED, audition pending |
| tack/equipment small metal | `metal_click.ogg` | SHORTLISTED, audition pending |
| quarry wood creak | `creak_2.ogg` | SHORTLISTED, audition pending |
| UI success commit | `confirmation_001` family | SHORTLISTED, audition pending |
| UI unavailable/error | `error_001` family | SHORTLISTED, audition pending |
| UI selection | `select_001` family | SHORTLISTED, audition pending |
| Earthloong electrical charge | none | OPEN_SIGNATURE_LAYER |
| Earthloong electrical strike | none | OPEN_SIGNATURE_LAYER |
| Earthloong body arc | none | OPEN_SIGNATURE_LAYER |
| Burning shaped effect | none | OPEN_SIGNATURE_LAYER |
| Frostbite shaped effect | none | OPEN_SIGNATURE_LAYER |

---

# 8. Verification state

```text
R01 GAMEPLAY / CONTENT DESIGN REOPENED: NO
OFFICIAL KENNEY FAMILY LICENSE/SOURCE STATE REUSED: YES
EXACT BASELINE VFX FILES PINNED: YES
EXACT BASELINE SFX FILES PINNED: YES
DIRECT CANDIDATE BINARY SHA-256 CALCULATED: YES
PNG DIMENSIONS READ FROM FILE BYTES: YES
AUDIO DURATION READ FROM FILE BYTES: YES
AUDIO AUDITIONED: NO
LOUDNESS MATCHED: NO
MINECRAFT VFX COMPOSITE TESTED: NO
MINECRAFT SPATIAL AUDIO TESTED: NO
EARTHLOONG SIGNATURE ELECTRICAL LAYER CLOSED: NO
BURNING SHAPED VFX CLOSED: NO
FROSTBITE SHAPED VFX CLOSED: NO
R01 ASSET_BINDING COMPLETE: NO
R01 SPATIAL_BINDING COMPLETE: NO
R01 SOURCE READY: NO
CODE REVIEWED: N/A
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

This is docs/research-only work. No build or CI run is justified.
