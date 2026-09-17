# Open-World RPG — R01 Asset Intake Phase-B Pass 5 / Acquisition-Source Evidence — 2026-09-17

> Canon: `GAME_DESIGN.md`  
> Intake authority: `R01_ASSET_INTAKE.md`  
> Previous evidence: `R01_ASSET_PHASE_B_PASS4_EVIDENCE_2026-09-17.md`  
> Provenance registry: `EXTERNAL_SOURCES.md`

This pass does **not** claim production acceptance. It converts several R01 candidates from `web-page lead` into an **acquisition-ready source decision**, records one important free-vs-paid package correction, and makes explicit which facts still require reading the actual downloaded archive.

The intended next transition remains:

```text
verified creator-controlled archive
→ project-local download
→ project-local SHA-256
→ exact archive member inspection
→ 3D / Blockbench review
→ conversion / retarget
→ actual Minecraft review
→ ACCEPTED
```

No project-local source hash is claimed in this pass because the available execution environment could verify direct ZIP URLs but could not fetch binary ZIP bytes: the local container has no working external DNS and the web text reader rejects `application/zip`. This is an execution-environment limitation, not an asset-availability claim. Never substitute a third-party checksum and label it project-local.

---

## 1. Modular Character Outfits — important Standard / Source correction

Current creator-controlled source:

`https://quaternius.itch.io/modular-character-outfits-fantasy`

Current page facts observed on 2026-09-17:

- 12 outfits;
- 62 modular parts;
- 3 texture variations per outfit;
- compatible with Universal Base Characters;
- humanoid rig / retarget support;
- compatible with Universal Animation Library;
- creator-controlled page labels the asset **CC0 1.0 Universal**;
- current v2.1 changelog explicitly mentions repaired `Male_Noble` and `Male_Wizard` glTF exports.

Current itch download listing:

```text
Modular Character Outfits - Fantasy[Standard].zip — 280 MB — free
Modular Character Outfits - Fantasy[Source].zip   — 724 MB — $20 USD
```

The creator gallery matters here:

- the **Standard / free** gallery explicitly shows Ranger + Peasant as the complete free model set;
- the **Source** gallery shows the wider outfit roster, including the heavier knight/plate silhouettes and wizard/noble-style outfits.

### Consequence for Pass 4

Pass 4 correctly identified real Wizard and Knight source-part names, but the project must **not** imply those pieces are known to be inside the free Standard archive.

Current preferred bindings are therefore:

```text
River Scholar Garb
  preferred family = Modular Character Outfits - Fantasy / Wizard
  current acquisition class = SOURCE-EDITION CONTENT
  status = ARCHIVE_INSPECTION_REQUIRED

Ironbound Guard
  preferred family = Modular Character Outfits - Fantasy / Knight
  current acquisition class = SOURCE-EDITION CONTENT
  status = ARCHIVE_INSPECTION_REQUIRED
```

The Source edition is currently sold for $20 on itch. If it is acquired, preserve the exact acquisition page, archive filename, page license at acquisition and project-local SHA-256 before accepting any raw part.

The public repository must still follow `PROJECT.md` / `EXTERNAL_SOURCES.md` provenance policy. `CC0` on the current creator-controlled itch page is strong license evidence for that listed package, but the project still records the exact archive it actually uses.

### Visual-direction check

The current creator gallery supports keeping both families as preferred intake targets:

- the Knight silhouettes are materially more truthful for `Ironbound Guard` than the free Ranger outfit;
- the Wizard/noble/caster silhouettes are materially closer to `River Scholar Garb` than Peasant/Ranger;
- both remain in the same visual/rig family as the selected R01 character direction.

This is **gallery-level visual-direction approval**, not mesh/clipping/Minecraft acceptance.

---

## 2. Free CC0 fallbacks for Scholar / Heavy if Source acquisition is rejected

Broad scouting is not restarted. Two creator-uploaded OpenGameArt artifacts are recorded only as bounded fallbacks because the preferred modern modular family is now known to require the Source edition for the relevant parts.

### 2.1 LowPoly RPG Characters

Creator-controlled OpenGameArt upload:

`https://opengameart.org/content/lowpoly-rpg-characters`

Observed facts:

```text
author = quaternius
license = CC0
archive = rpg_characters_-_nov_2020.zip
size = 12.9 MB
content = 6 rigged / animated / textured fantasy characters
formats = FBX / OBJ / Blend
page tags explicitly include wizard / warrior / rogue / monk / ranger
```

Use classification:

```text
River Scholar fallback/reference = AVAILABLE
production preference = NO, not without 3D comparison
```

Reason: this is an older whole-character pack, not the modern shared modular outfit family. It can prevent a dead end, but a lower-quality/mismatched whole wizard is not promoted simply because it is free.

### 2.2 LowPoly Animated Knight

Creator-controlled OpenGameArt upload:

`https://opengameart.org/content/lowpoly-animated-knight`

Observed facts:

```text
author = quaternius
license = CC0
archive = Knight Character by @Quaternius.zip
size = 2.5 MB
animations = idle / walk / run / roll / death + additional motions
attachment guidance = weapon on Palm.R; helmet on Head; shoulder pads on neck/shoulder bones
```

Use classification:

```text
Ironbound Guard fallback/reference = AVAILABLE
production preference = NO, not without 3D/rig comparison
```

Reason: it is an older whole-character/rig solution and may not match the selected Universal Base Characters + Modular Outfit pipeline, gender variation, modern UAL retargeting or final R01 art density.

These fallbacks reduce acquisition risk; they do **not** justify downgrading visual quality to avoid a $20 Source archive if the preferred family proves materially better.

---

## 3. Kenney Food Kit — creator archive locked, current-version drift recorded

Current creator-controlled sources:

- `https://kenney.nl/assets/food-kit`
- `https://kenney-assets.itch.io/food-kit`
- creator-uploaded OpenGameArt mirror: `https://opengameart.org/content/food-kit`

Current official facts:

```text
license = CC0 1.0 Universal
current kit = 200+ models
formats = OBJ / FBX / glTF
itch archive = kenney_food-kit.zip (~4.3 MB)
OpenGameArt creator-upload = kenney_food-kit.zip (~4.6 MB)
```

Creator-controlled direct ZIP URLs were resolved during this pass:

```text
Kenney current site:
https://kenney.nl/media/pages/assets/food-kit/83086fa91c-1719418518/kenney_food-kit.zip

OpenGameArt creator upload:
https://opengameart.org/sites/default/files/kenney_food-kit.zip
```

### Current-version correction

The current Kenney site describes Food Kit **2.0** as completely remade. Earlier Pass-4 exact filename evidence came from an older/v1.x-style extracted tree.

Therefore:

- `Skewer Vegetables` remains independently corroborated as a real Kenney CC0 Food Kit model by Poly Pizza's Kenney Food Kit index;
- the current official 200-model family remains the preferred Trail Skewers source;
- the project must **not** claim that the old `foodKit_v1.2/.../skewerVegetables.*` path is necessarily the exact path inside the current 2.0 ZIP until the actual archive is inspected.

Updated Trail Skewers state:

```text
source family = Kenney Food Kit
model identity = Skewer Vegetables
license family = CC0
creator archive locator = LOCKED
current archive member path = INSPECTION REQUIRED
visual role = editable base for canonical Trail Skewers
production acceptance = NO
```

This is stronger and more accurate than either inventing a KayKit skewer or silently treating a historical mirror path as the current official archive tree.

---

## 4. Quaternius CC0 creator-uploaded archives — direct acquisition locators

These OpenGameArt pages are uploader `quaternius`, list CC0, and expose direct archive URLs. They are preferable public-safe acquisition candidates where their Standard content is sufficient.

### 4.1 Fantasy Props MegaKit Standard

Page:

`https://opengameart.org/content/fantasy-props-megakit`

Observed:

```text
archive = fantasy_props_megakitstandard.zip
size = 73.7 MB
Standard count = 94 models
license = CC0
```

Direct archive:

`https://opengameart.org/sites/default/files/fantasy_props_megakitstandard.zip`

R01 use:

- `Potion_1..4` candidate family;
- settlement / market / alchemy / forge props where present;
- source-specific CC0 path avoids transferring the current central Quaternius QAL to this historical creator-uploaded artifact.

### 4.2 Medieval Village MegaKit Standard

Page:

`https://opengameart.org/content/medieval-village-megakit`

Observed:

```text
archive = medieval_village_megakitstandard.zip
size = 100 MB
Standard count = 176 models
license = CC0
```

Direct archive:

`https://opengameart.org/sites/default/files/medieval_village_megakitstandard.zip`

R01 use:

- Alderford modular architecture vocabulary;
- gate / wall / door / roof / balcony / chimney / vine modules already evidenced in earlier passes;
- authored service-building composition still required after archive inspection.

---

## 5. UAL1 / UAL2 — public CC0 Standard archives locked, exact clip-edition membership still gated

### UAL1

Creator-uploaded OpenGameArt page:

`https://opengameart.org/content/universal-animation-library`

Observed:

```text
license = CC0
archive = universal_animation_librarystandard.zip
size = 14.5 MB
full family = 120+ animations
Standard edition = 45 animations
```

Direct archive:

`https://opengameart.org/sites/default/files/universal_animation_librarystandard.zip`

### UAL2

Creator-uploaded OpenGameArt page:

`https://opengameart.org/content/universal-animation-library-2`

Observed:

```text
license = CC0
archive = universal_animation_library_2standard.zip
size = 10.3 MB
family = 130+ animations
purpose = melee / armed combos / parkour / farming / fishing / zombie locomotion + more
```

Direct archive:

`https://opengameart.org/sites/default/files/universal_animation_library_2standard.zip`

### Important boundary for `Drink` / `Consume`

Earlier passes correctly established `Drink` and `Consume` as real family-level clip identities from extracted/source evidence. This pass did **not** establish that those exact clips are members of the free OpenGameArt **Standard** ZIPs.

In particular UAL1's OpenGameArt page explicitly says the Standard package contains only 45 of the 120+ family animations.

Therefore do not write:

```text
UAL1 Standard definitely contains Drink
UAL2 Standard definitely contains Consume
```

until the downloaded Standard archive is inspected.

Current truthful state:

```text
UAL1 / UAL2 creator-controlled CC0 Standard acquisition source = LOCKED
Drink / Consume family clip identity = PINNED
exact edition/archive that contains each adopted clip = ARCHIVE INSPECTION REQUIRED
retarget / prop alignment / Minecraft acceptance = NOT DONE
```

If a required clip is Source/Pro-only, either acquire that edition under its actual terms or select a stronger free/public-safe motion; do not fake the action with a two-keyframe placeholder.

---

## 6. Binary acquisition attempt / environment result

The following direct archives were suitable for actual byte acquisition during this pass:

```text
kenney_food-kit.zip
fantasy_props_megakitstandard.zip
medieval_village_megakitstandard.zip
universal_animation_librarystandard.zip
universal_animation_library_2standard.zip
```

The project attempted to advance to byte-level hashing in the available execution environment. The environment could not complete it:

- the local/container runtime could not resolve public internet hosts;
- the web reader could resolve the ZIP links but intentionally refused to parse `application/zip` as text.

As a result:

```text
PROJECT-LOCAL SOURCE SHA-256: NOT PRODUCED
ARCHIVE CONTENT INSPECTION: NOT PERFORMED FROM THE ZIP BYTES
BLOCKBENCH / 3D REVIEW: NOT PERFORMED
```

Do not interpret this as a source failure. The direct creator-controlled URLs are now recorded so a local asset-intake run can download/hash them without repeating web research.

---

## 7. Pass-5 delta

| Gate | Before Pass 5 | After Pass 5 |
|---|---|---|
| River Scholar | Wizard parts pinned, free/source boundary unclear in canon | Wizard remains preferred; current evidence says relevant broader outfit content belongs to paid Source edition; free CC0 fallback recorded |
| Ironbound Guard | Knight parts pinned, free/source boundary unclear in canon | Knight remains preferred; current evidence says broader Knight content belongs to paid Source edition; free CC0 Animated Knight fallback recorded |
| Trail Skewers | exact Kenney candidate pinned from older tree evidence | creator-controlled current Food Kit archive locked; v2.0 remade-package path drift explicitly guarded against; `Skewer Vegetables` identity retained |
| Fantasy Props | creator-uploaded CC0 artifact known | exact creator-controlled direct ZIP locator locked |
| Medieval Village | creator-uploaded CC0 artifact known | exact creator-controlled direct ZIP locator locked |
| UAL1 / UAL2 | source family and candidate clips known | creator-controlled CC0 Standard ZIP locators locked; exact adopted clip edition membership kept honestly gated |
| project-local hashes | pending | still pending; no fabricated hash |

---

## 8. Next efficient intake action

Do **not** restart broad research.

The next asset-intake machine/session that has binary internet access should execute this batch in one go:

1. download the five public CC0 ZIPs in §6 from the recorded creator-controlled locators;
2. calculate SHA-256 on the exact bytes actually used;
3. inventory archive members for `Potion_1..4`, current `Skewer Vegetables` path, Medieval Village service modules, UAL `Drink` / `Consume` membership and relevant motion clips;
4. inspect the resulting models/clips side-by-side in Blender/Blockbench/another capable 3D viewer;
5. if the preferred Wizard/Knight outfits are retained, acquire the current `Modular Character Outfits - Fantasy[Source].zip` deliberately rather than assuming Standard contains them;
6. choose the final River Scholar / Ironbound / Wayfarer compositions;
7. adapt and accept Trail Skewers;
8. then perform actual Minecraft camera/FOV/animation/clipping review before promoting any row to `READY_* / accepted_in_minecraft=true`.

Until then:

```text
R01 ASSET READY = NO
DESIGN REVIEWED = YES
EXTERNAL SOURCE REVIEWED = YES
LICENSE METADATA REVIEWED = YES at the recorded source level
PROJECT-LOCAL SOURCE SHA-256 COMPLETE = NO
BLOCKBENCH / 3D REVIEWED = NO
BUILD VERIFIED = NO
PLAYTESTED = NO
MULTIPLAYER TESTED = NO
```
