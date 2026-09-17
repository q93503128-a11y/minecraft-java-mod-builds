# Open-World RPG — R01 Asset Intake Phase-B Pass 4 Evidence — 2026-09-17

> Canon: `GAME_DESIGN.md`  
> Intake: `R01_ASSET_INTAKE.md`  
> Previous evidence: `R01_ASSET_PHASE_B_PASS3_EVIDENCE_2026-09-15.md`  
> Provenance: `EXTERNAL_SOURCES.md`

This pass continues the pre-code R01 external-asset closure without reopening already locked gameplay. The purpose is to remove remaining “unknown exact visual source” gaps while keeping the distinction between **candidate evidence** and **accepted in-game asset**.

Evidence classes used here:

- **authoritative source/license evidence** — creator-controlled asset page or creator upload;
- **third-party extracted-tree evidence** — proves exact source filenames/parts exist, but is not the preferred acquisition source or sole license authority;
- **candidate binding** — exact family/file choice is narrow enough to carry into acquisition/3D review;
- **accepted asset** — only after project-controlled source acquisition, provenance/hash capture, Blockbench/3D review and actual Minecraft review.

`R01 ASSET READY = NO` remains correct after this pass.

---

## 1. Modular Character Outfits — current authoritative family evidence

Current Quaternius `Modular Character Outfits - Fantasy` page remains a strong R01 family source:

- 12 outfits;
- 62 modular parts;
- 3 texture variants per outfit;
- shared humanoid rig / retarget support;
- compatibility with Universal Base Characters and Universal Animation Library;
- the current creator-controlled itch page explicitly labels the pack CC0;
- the 2026-07-05 v2.1 changelog explicitly mentions fixes for `Male_Noble` and `Male_Wizard` glTF exports, which is direct creator evidence that the Wizard identity exists in the current package.

The central Quaternius QAL drift documented in earlier intake passes still applies. This pass does **not** flatten that conflict into a blanket raw-repository rule. The project must preserve the exact acquisition page/package license and project-local source hash for the archive actually used.

Authoritative source:

`https://quaternius.itch.io/modular-character-outfits-fantasy`

---

## 2. River Scholar Garb — exact Wizard modular candidate is now pinned

`River Scholar Garb` previously had no honest exact silhouette candidate inside the selected family. Pass 4 found extracted-tree evidence for the Wizard modular pieces, while the creator's current changelog independently confirms the Wizard export family exists.

Observed exact source part names:

```text
Male_Wizard_Body.gltf
Male_Wizard_Arms.gltf
Male_Wizard_Legs.gltf
Male_Wizard_Feet.gltf

Female_Wizard_Body.gltf
Female_Wizard_Arms.gltf
Female_Wizard_Legs.gltf
Female_Wizard_Feet.gltf
```

Third-party extracted-tree corroboration includes repositories that imported these exact Quaternius source paths. Those repositories are filename evidence only; the project should acquire the intended archive from Quaternius/another creator-controlled artifact source.

### R01 candidate binding

Use the Wizard `Body + Arms + Legs + Feet` family as the **first exact River Scholar Garb intake candidate**, with Universal Base Character head/hair rather than automatically forcing a stereotypical giant wizard hat.

Reason:

- the body/robe silhouette is much closer to a light scholarly/caster garment than Standard Ranger or Peasant;
- omitting an exaggerated hat keeps `River Scholar Garb` readable as an adventuring scholar garment rather than a costume-only mage uniform;
- male/female matching parts exist on the same shared rig family;
- it preserves R01's one-family character language instead of introducing an unrelated robe pack solely for one starter outfit.

Current state:

```text
River Scholar Garb exact candidate family: PINNED
source family: Quaternius Modular Character Outfits - Fantasy / Wizard
status: ARCHIVE_INSPECTION_REQUIRED
visual acceptance: NOT DONE
Minecraft acceptance: NOT DONE
```

Before acceptance:

- acquire the exact intended archive and preserve license-at-acquisition evidence + SHA-256;
- inspect all three texture variants;
- verify sprint/dodge/roll/cast/drink/eat/revive/mount clipping;
- verify robe lower-body motion does not visually collapse during fast combat movement;
- reject or alter the combination if it reads as generic “wizard costume” instead of grounded R01 light gear.

---

## 3. Ironbound Guard — exact Knight armor parts are now pinned

Pass 4 also found exact Knight modular-part evidence from extracted Quaternius package trees and consumers.

Observed exact male heavy components include:

```text
Male_Knight_Body_Armor.gltf
Male_Knight_Feet_Armor.gltf
Male_Knight_Legs_Armor.gltf
Male_Knight_Head_Armet.gltf
Male_Knight_Acc_Pauldron_Round.gltf
Male_Knight_Acc_Pauldron_Spike.gltf
```

Observed exact female Knight family evidence includes:

```text
Female_Knight_Body_Armor.gltf
Female_Knight_Acc_Pauldrons_Round.gltf
Female_Knight_Acc_Pauldrons_Spike.gltf
Female_Knight_Feet.gltf
Female_Knight_Legs.gltf
```

The female lower-part filenames above are recorded exactly as observed; do not silently rename them to `_Armor` to make the male/female naming symmetrical.

### R01 candidate binding

For the baseline `Ironbound Guard` silhouette, intake the following first:

```text
heavy torso: Knight Body Armor
shoulder identity: Round Pauldron(s)
legs/feet: matching Knight family lower pieces
head: open-head by default for starter readability; Armet remains the accepted-family helmet candidate for the Head appearance
```

Keep the spike pauldron variant as a higher-threat/high-grade visual option rather than putting the most aggressive silhouette on every starter Heavy set.

Current state:

```text
Ironbound Guard exact candidate family: PINNED
source family: Quaternius Modular Character Outfits - Fantasy / Knight
status: ARCHIVE_INSPECTION_REQUIRED
visual acceptance: NOT DONE
Minecraft acceptance: NOT DONE
```

Acceptance must check:

- shoulder clipping during guard, perfect guard, two-handed attacks and bow/cast edge cases;
- camera readability of the heavy silhouette at Minecraft scale;
- mount hip/leg clearance;
- revive/help-up arm clearance;
- whether round pauldrons preserve a grounded first-region look better than spike pauldrons;
- whether female/male combinations feel like the same armor identity rather than two unrelated sets.

This closes the old “Ranger/Peasant are not real heavy armor” problem at **exact candidate** level without pretending 3D review already happened.

---

## 4. Trail Skewers — exact CC0 external candidate found outside Restaurant Bits

The official KayKit Restaurant Bits tree still does not provide an honest `skewer`/`kebab` file match. Pass 4 therefore widened the search instead of mislabeling a steak/ham/dinner plate.

A better public-safe candidate exists in **Kenney Food Kit**.

Authoritative/source-family evidence:

- Kenney Food Kit is creator-published CC0;
- creator/OpenGameArt publication describes separate FBX/OBJ/glTF files and a large low-poly food/kitchen set;
- Poly Pizza's Kenney Food Kit index explicitly lists **`Skewer Vegetables`** as a CC0 model.

Exact filename/tree evidence from a Kenney asset mirror:

```text
foodKit_v1.2/Models/FBX format/skewer.fbx
foodKit_v1.2/Models/OBJ format/skewer.obj
foodKit_v1.2/Models/DAE format/skewer.dae

foodKit_v1.2/Models/FBX format/skewerVegetables.fbx
foodKit_v1.2/Models/OBJ format/skewerVegetables.obj
foodKit_v1.2/Models/DAE format/skewerVegetables.dae
```

The mirror is filename evidence, not the preferred acquisition source. Acquire the official Kenney archive/creator upload for project use.

### R01 candidate binding

`skewerVegetables` is the first exact **editable-base** candidate for `Trail Skewers` because its silhouette already communicates “food on a skewer.” Kenney CC0 permits modification, so after acquisition it can be adapted to the canonical R01 ingredient identity instead of pretending vegetables are Louxia meat.

Required adaptation boundary:

- preserve the readable skewer silhouette;
- alter food pieces/material treatment as needed so the finished item reads as the actual R01 recipe rather than a vegetable-only dish;
- keep texture/poly density compatible with the selected KayKit/Quaternius low-poly food/prop language;
- inventory icon must render from the same accepted 3D result.

Current state:

```text
Trail Skewers exact external candidate: FOUND
source family: Kenney Food Kit / skewerVegetables
license family: CC0
status: READY_PUBLIC CANDIDATE / VISUAL ADAPTATION REQUIRED
final accepted model: NO
```

The previously surfaced Sketchfab kebab lead is **not adopted** in this pass because its indexed page exposes conflicting-looking license metadata and its scanned/high-detail style is a worse fit for the established R01 low-poly language.

---

## 5. Revive and Trail Stag mount/dismount remain honest motion gates

Pass 4 did not find a stronger public-safe clip that justifies closing either motion gate.

### Teammate revive/help-up

Keep the existing paired Fab revive/downed candidate as discovery evidence only. It still needs:

- actual marketplace/license verification for intended use;
- source/skeleton accessibility;
- two-character relative-position review;
- downed/reviver hand alignment;
- server revive-completion frame alignment.

State remains:

```text
NEEDS_EXTERNAL_CLIP
```

### Trail Stag mount / dismount

A recently published horse-riding example exposes animation-driven mount/dismount behavior, but its repository states that redistribution is not allowed and its animation/horse geometry is not proven compatible with Trail Stag. It is therefore at most a **LOCAL_ONLY / REFERENCE** lead, not a public-repo production binding.

Quaternius' older CC0 animated horse family provides ordinary animal locomotion but does not by itself close rider mount/dismount transitions.

State remains:

```text
Trail Stag mount: NEEDS_EXTERNAL_CLIP
Trail Stag dismount: NEEDS_EXTERNAL_CLIP
```

Do not replace these with an instant rider snap or two-keyframe placeholder merely to unblock coding.

---

## 6. Pass-4 intake delta

This pass changes the evidence state as follows:

| R01 gate | Before pass 4 | After pass 4 |
|---|---|---|
| River Scholar Garb | no truthful exact Scholar/light-robe candidate | exact Wizard Body/Arms/Legs/Feet candidate family pinned for male + female; 3D acceptance pending |
| Ironbound Guard | heavy set unresolved | exact Knight armor/pauldron component family pinned; 3D acceptance pending |
| Trail Skewers | no exact Restaurant Bits model | exact Kenney Food Kit `skewerVegetables` editable-base candidate found; final adaptation/review pending |
| revive/help-up | external lead only | unchanged; still `NEEDS_EXTERNAL_CLIP` |
| Trail Stag mount/dismount | external lead only | unchanged; still `NEEDS_EXTERNAL_CLIP` |

This is intentionally a **candidate-evidence closure**, not a false production acceptance.

---

## 7. Immediate next intake actions

Do not restart broad source scouting. The next efficient batch is now:

1. acquire/hash the intended Quaternius Modular Character Outfits archive and Kenney Food Kit archive from creator-controlled sources;
2. perform 3D/Blockbench side-by-side review of Wizard vs Ranger/Peasant and Knight round-vs-spike pauldron combinations;
3. choose River Scholar texture/part composition and Ironbound Guard texture/part composition;
4. inspect/adapt the Kenney skewer candidate into the actual Trail Skewers recipe visual;
5. continue the still-open revive and Trail Stag transition search only until a source passes license + rig + quality requirements;
6. then move to potion assignment, KayKit weapon final A/B/C selections, Kenney VFX/audio visual/audition acceptance and actual Minecraft presentation review.

No build/CI is warranted for this docs/evidence-only pass.

---

## 8. Verification state after pass 4

- `DESIGN REVIEWED`: YES — current master canon reread before this pass
- `EXTERNAL SOURCE REVIEWED`: YES for the sources recorded here
- `LICENSE METADATA REVIEWED`: YES at source-page/family level; project-local archive provenance/hashes still pending where stated
- `RIVER SCHOLAR EXACT CANDIDATE PART FAMILY PINNED`: YES
- `IRONBOUND GUARD EXACT CANDIDATE PART FAMILY PINNED`: YES
- `TRAIL SKEWERS EXACT EXTERNAL CANDIDATE PINNED`: YES
- `REVIVE EXTERNAL CLIP ACCEPTED`: NO
- `TRAIL STAG MOUNT/DISMOUNT CLIP ACCEPTED`: NO
- `PROJECT-LOCAL SOURCE SHA-256 COMPLETE`: NO
- `BLOCKBENCH / 3D REVIEWED`: NO
- `R01 ASSET READY`: NO
- `BUILD VERIFIED`: NO
- `JAR PRODUCED`: NO
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO
