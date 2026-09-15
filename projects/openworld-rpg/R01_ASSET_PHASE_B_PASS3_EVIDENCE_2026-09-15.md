# Open-World RPG — R01 Asset Intake Phase-B Pass 3 Evidence — 2026-09-15

> Canon: `GAME_DESIGN.md`  
> Intake: `R01_ASSET_INTAKE.md`  
> Previous evidence: `R01_ASSET_PHASE_B_EVIDENCE_2026-09-15.md`  
> Provenance: `EXTERNAL_SOURCES.md`

This pass narrows three remaining R01 presentation gates without pretending that file discovery equals visual acceptance.

The evidence classes used here are deliberately separated:

- **authoritative source/license evidence** — creator-controlled Quaternius itch/website pages or an OpenGameArt upload by `quaternius`;
- **artifact snapshot evidence** — an exact historical Standard package/license snapshot;
- **third-party file-tree/hash evidence** — useful for exact filenames and corroboration, but not a replacement for the author/source license;
- **accepted asset** — only after the project has the intended bytes, preserves license/hash evidence, and passes 3D + Minecraft review.

---

## 1. Quaternius source-specific CC0 artifacts are usable evidence

The central Quaternius site currently publishes QAL v1.0 while multiple creator-controlled pack pages still explicitly advertise CC0. The project therefore continues to avoid a blanket family-level license conclusion.

Pass 3 found stronger package-specific evidence that is directly relevant to R01:

### Fantasy Props MegaKit Standard — OpenGameArt

Author/uploader: `quaternius`  
OpenGameArt page: `https://opengameart.org/content/fantasy-props-megakit`  
Published: 2025-06-03  
Page license: **CC0**  
Exact file: `fantasy_props_megakitstandard.zip`  
Published size: 73.7 MB  
Published Standard contents: **94 models**

This is a creator-uploaded historical Standard artifact with explicit CC0 evidence. It can therefore be treated separately from a newly acquired package governed only by the current central QAL page.

A third-party asset register that consumed this exact OpenGameArt URL records:

```text
reported archive SHA-256 = 0bc1e5843c9f245c44bda82f97f738b09f88fe51bb34d2a69a7847a969f868e3
```

Important: this hash was **not independently recomputed in this environment** because the ZIP could not be downloaded through the available runtime. Record it as corroborating evidence only until the project obtains the ZIP and calculates SHA-256 locally.

### Medieval Village MegaKit Standard — OpenGameArt

Author/uploader: `quaternius`  
OpenGameArt page: `https://opengameart.org/content/medieval-village-megakit`  
Published: 2025-04-16  
Page license: **CC0**  
Exact file: `medieval_village_megakitstandard.zip`  
Published size: 100 MB  
Published Standard contents: **176 models**

A third-party asset register records this exact OpenGameArt artifact with:

```text
reported archive SHA-256 = 58c3e1b9713ee85749ade230c36e1195af5d96951435532b4411e993b46387bd
```

Again, this is a reported hash, not independently recomputed here.

### Current itch pages

Current Quaternius itch pages also explicitly label the relevant downloadable packs CC0, including:

- `Modular Character Outfits - Fantasy[Standard].zip` — 280 MB, CC0;
- `Universal Base Characters[Standard].zip` — 122 MB, CC0;
- `Fantasy Props MegaKit[Standard].zip` — 143 MB, CC0.

For any current itch acquisition, preserve the download-page/license evidence and calculate the actual downloaded archive SHA-256 before committing raw bytes.

---

## 2. Modular Character Outfits — exact Standard free-part evidence

A public repository snapshot contains an extracted `Modular Character Outfits - Fantasy[Standard]` directory and the package's own `License_Standard.txt`.

The embedded license file says the Standard version is the **free version**, contains only a portion of the models, and puts the Standard snapshot under CC0 1.0 Universal. It also states that the remaining outfits and separate parts are obtained through the Source version.

This is third-party snapshot evidence, so the project should still acquire the intended pack from a creator-controlled source. However, it establishes exact existing Standard part names without guessing from screenshots.

Observed exact Standard glTF parts:

```text
Separate Parts[Gltf]/Female/Female_Peasant_Arms.gltf
Separate Parts[Gltf]/Female/Female_Peasant_Body.gltf
Separate Parts[Gltf]/Female/Female_Peasant_Feet.gltf
Separate Parts[Gltf]/Female/Female_Peasant_Legs.gltf

Separate Parts[Gltf]/Female/Female_Ranger_Arms.gltf
Separate Parts[Gltf]/Female/Female_Ranger_Body.gltf
Separate Parts[Gltf]/Female/Female_Ranger_Facewear.gltf
Separate Parts[Gltf]/Female/Female_Ranger_Feet.gltf
Separate Parts[Gltf]/Female/Female_Ranger_Legs.gltf
Separate Parts[Gltf]/Female/Female_Ranger_Pauldron.gltf

Separate Parts[Gltf]/Male/Male_Peasant_Arms.gltf
Separate Parts[Gltf]/Male/Male_Peasant_Body.gltf
Separate Parts[Gltf]/Male/Male_Peasant_Feet.gltf
Separate Parts[Gltf]/Male/Male_Peasant_Legs.gltf

Separate Parts[Gltf]/Male/Male_Ranger_Arms.gltf
Separate Parts[Gltf]/Male/Male_Ranger_Body.gltf
Separate Parts[Gltf]/Male/Male_Ranger_Facewear.gltf
Separate Parts[Gltf]/Male/Male_Ranger_Feet.gltf
Separate Parts[Gltf]/Male/Male_Ranger_Legs.gltf
Separate Parts[Gltf]/Male/Male_Ranger_Pauldron.gltf
```

Observed whole-outfit glTFs:

```text
Outfits[Gltf]/Female_Peasant.gltf
Outfits[Gltf]/Female_Ranger.gltf
Outfits[Gltf]/Male_Peasant.gltf
Outfits[Gltf]/Male_Ranger.gltf
Outfits[Gltf]/NoBody/Female_Peasant_NoBody.gltf
Outfits[Gltf]/NoBody/Female_Ranger_NoBody.gltf
Outfits[Gltf]/NoBody/Male_Peasant_NoBody.gltf
Outfits[Gltf]/NoBody/Male_Ranger_NoBody.gltf
```

### R01 consequences

- `Wayfarer Leathers` now has a concrete **Ranger** candidate family with exact part names.
- civilian/inn-worker/ordinary settlement clothing now has a concrete **Peasant** candidate family.
- Ranger `Facewear` and `Pauldron` give useful authored variation knobs without inventing new pieces.
- `River Scholar Garb` and `Ironbound Guard` are **not** force-mapped to Ranger/Peasant merely to close a checklist row. Their intended silhouettes still need other Standard parts, Source-package parts, or another accepted external family.
- Even Wayfarer/Ranger remains only a candidate until dodge, bow draw, guard, drink/eat, revive and mount clipping are visually checked.

---

## 3. Fantasy Props potion filenames — exact candidate universe expanded

The canonical R01 consumables are:

- Healing Potion;
- Focus Draught;
- Cleansing Tonic.

Pass 3 found independent downstream package trees that identify multiple Quaternius Fantasy Props potion files:

```text
Potion_1.gltf
Potion_2.gltf
Potion_3.gltf
Potion_4.gltf
SmallBottle.gltf   # observed in another selected-asset manifest
```

`Potion_1.gltf` is especially well corroborated:

- a downstream loader references the creator-domain path `https://quaternius.com/assets/fantasypropsmegakit/GLTF/Potion_1.gltf`;
- a curated OpenGameArt-Standard asset register records source path `Exports/glTF/Potion_1.gltf`;
- that register reports source-model SHA-256 `fc53caca616741a21790b54a742a4fe3b4b3dfc88f4d50716fa8f65eece5c2ef` and `Potion_1.bin` SHA-256 `6838b7e7a740a91f3bbdce0c79482638f095a79f90e2e1a59158c9750171665c`.

Those hashes are third-party recorded values and are **not** promoted to project acquisition hashes until locally recomputed from the actual chosen source artifact.

`Potion_2.gltf`, `Potion_3.gltf` and `Potion_4.gltf` are independently observed in downstream extracted Fantasy Props trees/manifests. This closes the previous uncertainty about whether more than one exact potion filename exists.

### R01 consequence

The potion family is no longer filename-blocked. Candidate universe:

```text
Potion_1.gltf
Potion_2.gltf
Potion_3.gltf
Potion_4.gltf
```

However the three canonical consumables are **not yet assigned** to 1/2/3/4. R01 requires them to be readable by silhouette/contents rather than color alone. Final assignment waits for 3D inspection of all candidates and hand-pivot compatibility with the final drink animation.

---

## 4. UAL exact eat + drink clip names

Pass 2 had exact UAL2 `Consume` but only a drink-family observation for UAL1. Pass 3 closes the exact drink name through a real downstream Quaternius-library importer:

```text
UAL1 exact clip candidate = Drink
UAL2 exact clip candidate = Consume
```

Another public project packaging the Quaternius Standard GLBs records:

```text
UAL1_Standard.glb
Git blob SHA-1 = 410b30c617cc3097f7d82e36d4c28ef130f66388
size = 8,114,364 bytes

UAL2_Standard.glb
Git blob SHA-1 = dc684c2a664927964307e8eb7b27b0000ebf6a18
size = 8,061,600 bytes
```

These are **Git blob SHA-1 identities for a downstream copy**, not Quaternius source-archive SHA-256 values.

R01 motion status becomes:

```text
meal eat     -> Quaternius UAL2 / Consume
potion drink -> Quaternius UAL1 / Drink
```

Both remain `ARCHIVE_INSPECTION_REQUIRED` until the exact source artifact used by this project is acquired, its license evidence/hash recorded, the clip is retargeted, prop alignment is checked, and the result passes actual Minecraft visual review.

---

## 5. Medieval Village Standard — exact module evidence

A curated consumer of the OpenGameArt Standard archive records exact Medieval Village glTF modules and per-model SHA-256 values. Useful R01 base modules include:

```text
glTF/Wall_Plaster_Straight.gltf
glTF/Wall_Plaster_Window_Wide_Round.gltf
glTF/Wall_Plaster_Door_Round.gltf
glTF/Wall_UnevenBrick_Straight.gltf
glTF/Roof_RoundTiles_6x8.gltf
glTF/Roof_Front_Brick6.gltf
glTF/Prop_Chimney.gltf
glTF/Balcony_Cross_Straight.gltf
glTF/Prop_Vine1.gltf
glTF/Prop_Vine4.gltf
```

These establish a real modular vocabulary for settlement shells; they do **not** mean one single building module is already selected for every R01 service.

R01 can now treat `Medieval Village MegaKit Standard / OpenGameArt CC0 snapshot` as a public-safe candidate source for shared architectural modules, while service-specific composition for shrine/inn/guild/forge/bank/healer/stable/homes/market remains a design + in-game review task.

---

## 6. What pass 3 resolves vs. what remains gated

### Resolved to exact evidence-backed candidate level

- Wayfarer clothing: Ranger exact Standard part family exists.
- civilian/inn-worker baseline: Peasant exact Standard part family exists.
- potion filename universe: `Potion_1` / `Potion_2` / `Potion_3` / `Potion_4` exists.
- meal eat clip: UAL2 `Consume`.
- potion drink clip: UAL1 `Drink`.
- Medieval Village Standard has a creator-uploaded CC0 snapshot and exact modular wall/roof/door/window/prop filenames.
- Fantasy Props Standard has a creator-uploaded CC0 snapshot containing 94 models.

### Still not accepted / still blocked

- River Scholar Garb final external parts;
- Ironbound Guard final external parts;
- final Ranger part combination/color for Wayfarer after rig/clipping review;
- final Peasant combinations for each settlement role;
- exact potion-to-item assignment after 3D silhouette review;
- Trail Skewers model;
- accepted revive/help-up source;
- accepted Trail Stag mount/dismount source;
- final KayKit weapon A/B/C selections after 3D review;
- final Kenney VFX choices after visual inspection;
- final Kenney audio choices after audition;
- actual project-local SHA-256 for every chosen archive/file;
- Blockbench/3D-viewer acceptance;
- actual Minecraft visual/play acceptance.

`R01 ASSET READY = NO` remains correct.
