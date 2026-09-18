# Open-World RPG — R01 Asset Phase E Apparel / Potion / Motion Exact-File Review — 2026-09-18

> Canon: `GAME_DESIGN.md`  
> Intake: `R01_ASSET_INTAKE.md`  
> Binding matrix: `PRODUCTION_ASSET_BINDING_MATRIX.md`  
> Previous passes: `R01_ASSET_PHASE_B_PASS4_EVIDENCE_2026-09-17.md`, `R01_ASSET_PHASE_D_FISH_VFX_SOURCE_REVIEW_2026-09-18.md`  
> Base remote `main` observed before this pass: `e12cd31a789a74170105b78a3e6ed43dbcf17122`

This is a production-binding evidence pass. It does not reopen R01 gameplay/content design and it does not treat third-party extracted files as a substitute for creator-controlled acquisition.

Evidence classes used here:

- **creator source/license evidence** — current Quaternius / creator-controlled source page;
- **downstream exact-file corroboration** — an exact public Git repository commit containing extracted Quaternius files, useful for byte identity, file tree and glTF structure;
- **technical conversion evidence** — direct parsing of glTF metadata such as vertex/index counts, bounds, skin joints, texture references and external BIN size;
- **accepted production asset** — only after the project obtains the intended creator/source artifact, records project-local provenance + SHA-256, performs visual conversion and Minecraft review.

No row in this pass is promoted to `FINAL_ACCEPTED` solely because a downstream copy exists.

---

## 1. Fish acquisition retry outcome

The Phase-D fish queue remains unchanged:

- Common A: Small Fish — Common Minnow;
- Common B: CDmir Fish;
- Uncommon: Quaternius Armored Catfish;
- Rare: CDmir Esox.

The two creator OpenGameArt CDmir direct ZIP locators remain the intended acquisition paths:

- `https://opengameart.org/sites/default/files/fish.zip`
- `https://opengameart.org/sites/default/files/esox.zip`

Binary materialization still failed in the current environment. The project did **not** repeat the same failing download loop and did **not** reopen broad fish scouting.

Common Minnow remains confirmed at the package-discovery level as a released CC0 5 MB Small Fish asset, but the exact individual package/download locator is still unresolved.

Therefore:

```text
R01 FISH FOUR-ROLE QUEUE: UNCHANGED
CDMIR PROJECT-LOCAL ZIP SHA-256: NOT AVAILABLE
COMMON MINNOW EXACT PACKAGE LOCATOR: NOT PINNED
BROAD FISH SEARCH: STOPPED
```

---

## 2. Current creator-source state for the apparel family

Current creator page:

`https://quaternius.itch.io/modular-character-outfits-fantasy`

Observed 2026-09-18:

- Status: Released;
- Asset license: CC0 1.0;
- 12 outfits;
- 62 modular parts;
- 3 texture variations per outfit;
- Humanoid rig / retarget support;
- Universal Base Characters compatibility;
- Universal Animation Library compatibility;
- current Standard archive: `Modular Character Outfits - Fantasy[Standard].zip`;
- current page reports v2.1 (2026-07-05) fixes including `Male_Wizard` glTF export correction.

The creator archive itself was not binary-materialized by this environment, so project-local archive SHA-256 is still pending.

---

## 3. Pinned downstream technical snapshot

To remove uncertainty about the exact file structures without pretending that a downstream repository is the authoritative acquisition source, this pass directly parsed extracted Quaternius glTF files from:

```text
repository: dustinc555/mygame
commit: 6f12ffb2f924af86d910ade13e6e2ba3df8cd3df
path: assets/vendor/quaternius/modular_character_outfits_fantasy/modular_parts/
```

This snapshot is used only for exact-file / conversion corroboration.

### 3.1 River Scholar — Wizard body

| File | Git blob SHA-1 | Vertices | Triangles | Bounds size | Skin joints |
|---|---|---:|---:|---|---:|
| `Male_Wizard_Body.gltf` | `1e845f5b91b89cb53dd73155ed5d1a82165d377d` | 4,865 | 5,254 | 0.513 × 0.698 × 0.349 | 65 |
| `Female_Wizard_Body.gltf` | `a574d22979f47f748d3933dce8c594523fca3ce6` | 2,651 | 2,858 | 0.469 × 0.651 × 0.358 | 65 |

Both reference the same authored Wizard texture family:

```text
T_Wizard_BaseColor.png
T_Wizard_Normal.png
T_Wizard_ORM.png
```

External buffers:

```text
Male_Wizard_Body.bin   370,636 bytes
Female_Wizard_Body.bin 243,992 bytes
```

The male body contains multiple authored mesh pieces, including belt submeshes. The files contain no embedded animations; animation is expected to come from the shared humanoid animation family.

### 3.2 River Scholar — Wizard arms

| File | Git blob SHA-1 | Vertices | Triangles | Skin joints |
|---|---|---:|---:|---:|
| `Male_Wizard_Arms.gltf` | `cc4aaf187b0f84e3e3c8cb542f18a408eb95d9c6` | 3,400 | 5,184 | 65 |
| `Female_Wizard_Arms.gltf` | `6e51c7fb87845245a62ef188d9a3be77c6233f3f` | 3,342 | 5,152 | 65 |

The arm files reference both Wizard outfit textures and the appropriate regular humanoid skin texture family. This is useful evidence that the separate-part export is intended to combine authored outfit geometry with the shared base-character body.

### 3.3 Wayfarer — Ranger body

| File | Git blob SHA-1 | Vertices | Triangles | Bounds size | Skin joints |
|---|---|---:|---:|---|---:|
| `Male_Ranger_Body.gltf` | `a8ef1f96f83d3a4d453260976beb3f03630046cc` | 4,681 | 4,606 | 0.429 × 0.691 × 0.343 | 65 |
| `Female_Ranger_Body.gltf` | `ab33294f155cc9fd811210dfd8478aecff17b160` | 4,641 | 4,570 | 0.380 × 0.622 × 0.345 | 65 |

Shared texture family:

```text
T_Ranger_BaseColor.png
T_Ranger_Normal.png
T_Ranger_ORM.png
```

Exact accessory evidence also exists in the same snapshot:

| File | Git blob SHA-1 | Vertices | Triangles | Skin joints |
|---|---|---:|---:|---:|
| `Male_Ranger_Acc_Pauldron.gltf` | `6387d88a73c70e95861946cf968be3a2839416cb` | 1,092 | 1,376 | 65 |
| `Female_Ranger_Acc_Pauldrons.gltf` | `d45fad296ffa46c0ab89b6d675dcb8d51c6d1603` | 1,092 | 1,376 | 65 |

Note the real male/female filename difference: singular `Pauldron` vs plural `Pauldrons`. Do not normalize this away in acquisition tooling.

### 3.4 Ironbound — Knight body and round pauldrons

| File | Git blob SHA-1 | Vertices | Triangles | Bounds size | Skin joints |
|---|---|---:|---:|---|---:|
| `Male_Knight_Body_Armor.gltf` | `9fc5e259cfb86142164810b14d852ea2bee2064b` | 5,336 | 6,042 | 0.372 × 0.654 × 0.358 | 65 |
| `Female_Knight_Body_Armor.gltf` | `94987514e0f1d829910f86ea2d83d6f335581a1f` | 3,188 | 3,450 | 0.341 × 0.628 × 0.354 | 65 |

Shared texture family:

```text
T_Knight_BaseColor.png
T_Knight_Normal.png
T_Knight_ORM.png
```

Round shoulder identity:

| File | Git blob SHA-1 | Vertices | Triangles | Skin joints |
|---|---|---:|---:|---:|
| `Male_Knight_Acc_Pauldron_Round.gltf` | `43120b671456dbb608847ec67e416134f8566ad5` | 2,540 | 2,976 | 65 |
| `Female_Knight_Acc_Pauldrons_Round.gltf` | `a6a06eb420aaa20eeb946648a8b41a554c146287` | 2,541 | 2,976 | 65 |

The exact male body directly parsed in this pass uses Khronos glTF Blender I/O v4.3.47, references a 381,916-byte external BIN, and exposes the same detailed 65-joint humanoid hierarchy used by the other selected outfit components.

### 3.5 Apparel production consequence

The three starter appearance families are no longer only filename candidates. Their exact extracted glTF structures show:

- a common 65-joint skinned humanoid basis;
- external BIN payloads rather than broken empty shells;
- coherent BaseColor / Normal / ORM texture families;
- low-thousands triangle counts suitable for a Minecraft conversion pipeline;
- separate modular components that can be tested for clipping instead of baking unrelated static costumes.

This raises confidence in **conversion viability**, not final visual acceptance.

Still required before acceptance:

- acquire the creator-controlled current archive;
- calculate project-local archive/file SHA-256;
- preserve acquisition/license evidence;
- inspect all current texture variants;
- perform actual 3D side-by-side appearance review;
- verify sprint/dodge/cast/guard/drink/eat/revive/mount clipping;
- convert to the chosen Minecraft rendering/model pipeline;
- verify in actual Minecraft camera, lighting and multiplayer-relevant states.

---

## 4. Potion_1..4 exact structure review

Creator/source family:

- Quaternius Fantasy Props MegaKit;
- current official page: `https://quaternius.com/packs/fantasypropsmegakit.html`;
- creator OpenGameArt Standard snapshot: `https://opengameart.org/content/fantasy-props-megakit`;
- Standard snapshot file: `fantasy_props_megakitstandard.zip`;
- creator page / OpenGameArt source identify the family as CC0.

Exact downstream technical snapshot:

```text
repository: dustinc555/mygame
commit: 6f12ffb2f924af86d910ade13e6e2ba3df8cd3df
path: assets/vendor/quaternius/fantasy_props_megakit/gltf/
```

| Candidate | glTF Git blob SHA-1 | BIN bytes | Vertices | Triangles | Bounds size |
|---|---|---:|---:|---:|---|
| `Potion_1.gltf` | `29eba07b7189bdd9d9fcad7dfd8667a32ae7abad` | 22,144 | 501 | 500 | 0.116 × 0.141 × 0.116 |
| `Potion_2.gltf` | `1edb06afe2de9b1ef40d0f83ad327739b5fa7c76` | 22,496 | 501 | 516 | 0.135 × 0.263 × 0.135 |
| `Potion_3.gltf` | `ef45231036718d5547f0c7af594722ce7bba46f3` | 36,792 | 817 | 792 | 0.160 × 0.274 × 0.160 |
| `Potion_4.gltf` | `16be5eb1e00660fdee3818b93651f59031467918` | 26,480 | 595 | 596 | 0.151 × 0.201 × 0.151 |

All four are real distinct geometry payloads rather than four filenames pointing to an identical mesh. `Potion_3` also references the trim-metal material family in addition to the common prop trim material.

This is enough to close the old uncertainty that the four candidates might be color-only clones. It is **not** enough to assign the canonical Healing Potion / Focus Draught / Cleansing Tonic yet.

Final assignment remains blocked on:

- direct visual 3D comparison;
- hand-scale / pivot review against the selected `Drink` motion;
- Minecraft inventory/world readability;
- ensuring the three canonical items remain distinguishable without relying on hue alone.

State:

```text
POTION_1..4 EXACT GEOMETRY CONFIRMED: YES
POTION THREE-ITEM CANONICAL ASSIGNMENT: NOT YET
POTION MINECRAFT ACCEPTANCE: NOT DONE
```

---

## 5. UAL Drink / Consume version-sensitive review

Current creator pages observed 2026-09-18:

- UAL1: `https://quaternius.itch.io/universal-animation-library`
  - current page advertises 120+ animations;
  - CC0;
  - current Standard ZIP is 15 MB;
  - v3.0 (2026-06-16) added root motion to locomotion/movement exports;
  - v2.1 corrected an accidental 24 fps export back to 30 fps.
- UAL2: `https://quaternius.itch.io/universal-animation-library-2`
  - 130+ animations;
  - CC0;
  - current Standard ZIP is 17 MB;
  - v2.0 (2026-06-16) adds root-motion variants;
  - v2.1 (2026-07-05) includes a Godot duplicate-name fix.

### 5.1 Historical UAL1 mirror is not sufficient proof for Drink

Pinned mirror:

```text
repository: J-Ponzo/gltf-universal-animation-library
distribution statement: Quaternius Standard free version as distributed 2025-06-10
glTF blob: d9e132ad1d41089f8f96488775829d220a4beb05
BIN blob: 481652b8b1571b15c254b44f4d9b9f702498f948
```

The glTF was parsed directly:

```text
animation count: 46
exact "Drink": absent
```

Therefore this older Standard snapshot must **not** be used as the source proof for the current UAL1 `Drink` candidate.

### 5.2 Newer downstream UAL snapshot corroborates exact names

Pinned downstream project:

```text
repository: DyingStar-game/DyingStar
commit: f8a783b1f6a5387652e99ec12823bb2ae7600f30
```

Exact GLB identities visible in its Quaternius animation folder include:

```text
UAL1_Standard.glb
Git blob SHA-1: 473e59080288428d0b6da826ba19324d07b191f0
size: 7,618,436 bytes

UAL1.glb
Git blob SHA-1: df3d91e3ec69cd2ac61a91f83c8cf81f1bd44c22
size: 21,378,992 bytes

UAL2.glb
Git blob SHA-1: bb3d392ebbc07363eca57e76ef4f4e6853bb37b9
size: 20,717,364 bytes
```

Its animation mapping source explicitly records:

```text
emote_drink   -> "Drink"
emote_consume -> "Consume"
```

and its emote catalog explicitly describes `consume` among the UAL2 emotes that resolve after UAL2 is merged.

This is strong downstream corroboration that the exact motion names remain valid in a newer Quaternius-library integration.

The connector could enumerate these binary GLBs and their Git blob identities, but could not return usable binary bytes for local GLB chunk parsing. Exact clip duration, key count and root-motion curve were therefore **not** measured in this pass.

Production state remains:

```text
potion drink -> UAL1 / Drink
meal eat     -> UAL2 / Consume

exact clip names: CORROBORATED
current creator ZIP acquired by project: NO
clip duration/key/root curve directly measured: NO
retarget/prop-hand alignment reviewed: NO
Minecraft acceptance: NO
```

---

## 6. Revive / Trail Stag transitions

No stronger production-safe evidence was found during this exact-file pass.

Do not broaden the search merely to fill rows.

```text
teammate revive/help-up: NEEDS_EXTERNAL_CLIP
Trail Stag mount:        NEEDS_EXTERNAL_CLIP
Trail Stag dismount:     NEEDS_EXTERNAL_CLIP
```

Instant snap / two-keyframe placeholder remains disallowed for player-facing production.

---

## 7. Binding delta after Phase E

| Gate | Before | After this pass |
|---|---|---|
| River Scholar | exact Wizard filenames pinned | exact Wizard glTF/BIN/texture/65-joint structure and geometry metrics corroborated |
| Wayfarer | Ranger family pinned | exact Ranger body + pauldron structure and geometry metrics corroborated |
| Ironbound | Knight family pinned | exact heavy body + round pauldron structure and geometry metrics corroborated |
| Potion family | Potion_1..4 filenames pinned | all four exact geometry payloads measured; color-only-clone uncertainty removed |
| UAL1 Drink | exact name candidate | older 2025 Standard proven insufficient; newer integration corroborates exact `Drink` mapping |
| UAL2 Consume | exact name candidate | newer integration corroborates exact `Consume`; UAL2 ownership explicitly documented downstream |
| Fish | four-role queue, binary acquisition blocked | unchanged; no repeated broad search |
| Revive / mount | open motion gates | unchanged |

---

## 8. Verification state

```text
DESIGN/CANON REVIEWED: YES
REMOTE MAIN RECHECKED BEFORE PASS: YES
CREATOR SOURCE/LICENSE PAGES REVIEWED: YES
DOWNSTREAM EXACT APPAREL GLTF STRUCTURE READ: YES
DOWNSTREAM EXACT POTION GLTF STRUCTURE READ: YES
UAL 2025 STANDARD GLTF ANIMATION LIST PARSED: YES
NEWER UAL GLB BINARY CONTENT LOCALLY PARSED: NO
CREATOR APPAREL ARCHIVE ACQUIRED: NO
CREATOR UAL CURRENT ARCHIVES ACQUIRED: NO
PROJECT-LOCAL SHA-256 FOR THESE ARCHIVES: NO
BLOCKBENCH / 3D VISUAL ACCEPTANCE: NO
MINECRAFT CONVERSION ACCEPTANCE: NO
R01 ASSET READY: NO
R01 SOURCE READY: NO
CODE REVIEWED: N/A
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

No build or CI run is justified for this docs/research-only pass.
