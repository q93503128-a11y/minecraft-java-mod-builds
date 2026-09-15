# Open-World RPG — R01 External Asset Intake Manifest

> Status: **ASSET INTAKE PHASE B / EVIDENCE PASS 3 COMPLETE — NOT ASSET READY**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Opening: `R01_VERTICAL_SLICE.md`  
> Equipment: `EQUIPMENT_BALANCE.md`, `LOOT_ECONOMY.md`  
> Appearance/recovery: `RECOVERY_PRODUCTION_APPEARANCE.md`  
> Provenance registry: `EXTERNAL_SOURCES.md`  
> Phase-B evidence: `R01_ASSET_PHASE_B_EVIDENCE_2026-09-15.md`  
> Pass-3 evidence: `R01_ASSET_PHASE_B_PASS3_EVIDENCE_2026-09-15.md`  
> Rule: gameplay canon still wins. For **R01 asset-admission/license decisions**, this file records the newest verified evidence and supersedes older subordinate notes whose license label has become stale.

This file converts the project's external-first rule from a pack-level intention into an intake manifest. It is deliberately conservative: an asset is not considered production-ready merely because a web page looks good or because a previous conversation called a pack CC0.

The target workflow is:

```text
canonical gameplay need
→ select external source family
→ verify current source/license
→ identify exact file/archive entry
→ record acquisition-time license + source hash
→ inspect model/clip in Blockbench/3D viewer
→ convert/retarget without changing gameplay truth
→ inspect inside actual Minecraft
→ accept
→ implement/bind in data
```

No player-facing subsystem may bypass an unresolved visual row by shipping a vanilla recolor, static pose, code-only dash, generic particle cloud or invented placeholder model.

---

# 1. Intake status vocabulary

Use these exact states in R01 source-binding data and review notes.

- `READY_PUBLIC` — current source/license permits the intended public-repository inclusion; exact source file or deterministic archive entry is identified.
- `READY_LOCAL_ONLY` — suitable for the private playable build, but raw source bytes are not committed to the public repository.
- `DEPENDENCY` — presentation comes from an installed mod/dependency; its source assets are not copied into this repo unless separately permitted.
- `ARCHIVE_INSPECTION_REQUIRED` — source family is accepted but exact downloaded filename/part and package license still need inspection.
- `NEEDS_EXTERNAL_CLIP` — gameplay behavior is locked, but no accepted motion clip has yet passed source/license/quality review.
- `REFERENCE_ONLY` — useful visual/behavior precedent, not an adopted asset.
- `BLOCKED` — do not implement player-facing presentation until a better/valid source is selected.

`ARCHIVE_INSPECTION_REQUIRED` is not permission to invent a replacement during coding.

---

# 2. Critical Quaternius license-drift correction

As of the 2026-09-15 intake review, Quaternius publishes conflicting-looking public metadata:

- individual pack pages such as `Modular Character Outfits - Fantasy`, `Universal Base Characters` and many older/newer packs still display **CC0**;
- the central `https://quaternius.com/license.html` page now publishes **Quaternius Asset License (QAL) v1.0**, last updated 2026-08-28;
- QAL permits use/copy/modify and distribution of a completed Product incorporating the assets, but forbids redistributing the assets themselves as assets, including modified asset files;
- QAL §7 states license changes are not retroactive: assets already obtained under an earlier license remain governed by the license in effect when obtained.

Therefore the public GitHub repository must not blindly treat newly downloaded Quaternius raw files as redistributable CC0 bytes.

## R01 conservative rule

Until the actual downloaded package/license-at-acquisition is inspected and recorded:

```text
Quaternius raw model/animation/texture file
status = ARCHIVE_INSPECTION_REQUIRED / READY_LOCAL_ONLY
raw_repo_allowed = false
```

This does **not** reject Quaternius as the preferred private-play visual family. Current QAL explicitly permits incorporation into a game/product. It only changes how source bytes are handled in the public repository.

If an exact package was demonstrably obtained earlier under CC0, preserve that evidence and classify that package according to the earlier license. Never backfill this conclusion from memory alone.

The older generic Quaternius `CC0 / direct-use` labels in `EXTERNAL_SOURCES.md`, `R01_VERTICAL_SLICE.md`, `RECOVERY_PRODUCTION_APPEARANCE.md` or `EQUIPMENT_BALANCE.md` must be read through this newer intake rule until the registry-wide provenance cleanup is completed.

Source-specific exception handling matters: if an exact Quaternius artifact is independently published by Quaternius on another source such as OpenGameArt or itch with explicit CC0 terms, preserve that artifact's own source/license evidence instead of silently replacing it with the central site's current default label.

Phase-B pass 3 confirms two directly useful creator-uploaded OpenGameArt snapshots:

```text
Fantasy Props MegaKit Standard
  file = fantasy_props_megakitstandard.zip
  license = CC0
  published Standard count = 94 models

Medieval Village MegaKit Standard
  file = medieval_village_megakitstandard.zip
  license = CC0
  published Standard count = 176 models
```

These source-specific CC0 snapshots are public-safe **candidate sources** when the project acquires and hashes those exact artifacts. A third-party recorded SHA-256 is evidence, not a substitute for locally hashing the bytes actually used.

---

# 3. Public-safe baseline families

## 3.1 KayKit

Current KayKit pages used by R01 explicitly state CC0 and allow personal/commercial use without attribution.

Accepted source families:

- `KayKit Character Animations` — 161 humanoid animations; locomotion/dodging, melee, ranged/spellcasting, tool/work actions;
- legacy `KayKit Character Animations` — exact named fallback clips including `Roll`, `Dash Front`, `Dash Back`, `Dash Right`, `Dash Left`;
- `KayKit Fantasy Weapons Bits` — free tier 25+ fantasy weapon models, broader pack 40+; swords, axes, hammers, bows, staves, wands, shields, spears, etc.;
- `KayKit RPG Tools Bits` — free tier 45+ tool/production props; hammer, anvil, pickaxe, axe, hand tools, blueprints, etc.;
- `KayKit Restaurant Bits` — food/kitchen models, including ingredients and prepared-food states;
- `KayKit Character Pack: Adventurers 1.0` GitHub repository — exact public CC0 package used below for path-level bindings and fallback compatibility models.

The Adventurers repository itself contains a CC0 `LICENSE.txt`; repository main tree observed at commit `672074b73ba276876a19e8816ecdc5241817ab47` during this intake.

Phase-B evidence rule:

- authoritative KayKit pack pages establish license/family eligibility;
- official KayKit GitHub repositories may establish exact public file paths;
- third-party mirrors may be used only to inspect filename/tree evidence when the authoritative page does not expose internal filenames;
- a third-party mirror never replaces the authoritative KayKit license or acquisition source;
- archive/file SHA-256 is recorded only after the actual source bytes used by the project are obtained.

## 3.2 Kenney

Current Kenney asset pages used here state CC0.

Accepted R01 source families:

- `Particle Pack` — 80 VFX sprites;
- `Smoke Particles` — dust/smoke secondary pool;
- `RPG Audio` — footsteps/weapon/RPG foley baseline;
- `Impact Sounds` — 130 impact/foley sounds;
- `UI Audio` / `Interface Sounds` — menu/confirmation/error/notification baseline.

Kenney is a baseline reusable pool, not automatic permission to give every boss the same generic sound. Signature bosses/Mythics may require stronger dedicated external audio later.

## 3.3 Quaternius Universal Animation Library family

Quaternius `Universal Animation Library` and `Universal Animation Library 2` remain strong humanoid motion candidates because they use a universal humanoid rig and public source pages/source-specific mirrors have explicit CC0 evidence for relevant editions.

Phase-B pass 3 establishes exact candidate clip names:

- UAL2 `Consume` for meal eating;
- UAL1 `Drink` for potion drinking;
- UAL2 also covers chop/mine/fish/farm/carry/sleep and therefore is a useful cross-check against KayKit for motions that clip less with the selected Quaternius body/outfit family.

Because current central Quaternius licensing has drifted, actual project adoption still records the exact UAL/UAL2 source edition, acquisition-time license evidence and source hash. Do not infer raw-repo safety from the family name alone.

---

# 4. Exact public-safe KayKit file bindings

Repository:

`KayKit-Game-Assets/KayKit-Character-Pack-Adventures-1.0`

Observed license: CC0 1.0 Universal.

These are exact existing source locators, not guessed filenames.

| Intake ID | R01 / project use | Exact source locator | Status |
|---|---|---|---|
| `kk_adv_sword_1h` | Heartland Arming Sword conversion baseline / sword grip-scale test | `addons/kaykit_character_pack_adventures/Assets/gltf/sword_1handed.gltf` | `READY_PUBLIC` |
| `kk_adv_sword_2h` | greatsword scale/animation compatibility baseline | `addons/kaykit_character_pack_adventures/Assets/gltf/sword_2handed.gltf` | `READY_PUBLIC` |
| `kk_adv_dagger` | Wayfarer Daggers compatibility baseline | `addons/kaykit_character_pack_adventures/Assets/gltf/dagger.gltf` | `READY_PUBLIC` |
| `kk_adv_shield_round` | Watch Buckler compatibility baseline | `addons/kaykit_character_pack_adventures/Assets/gltf/shield_round.gltf` | `READY_PUBLIC` |
| `kk_adv_shield_square` | heavy/standard shield scale baseline | `addons/kaykit_character_pack_adventures/Assets/gltf/shield_square.gltf` | `READY_PUBLIC` |
| `kk_adv_shield_badge` | guard/crest shield alternate | `addons/kaykit_character_pack_adventures/Assets/gltf/shield_badge.gltf` | `READY_PUBLIC` |
| `kk_adv_staff` | Initiate Staff public-safe baseline | `addons/kaykit_character_pack_adventures/Assets/gltf/staff.gltf` | `READY_PUBLIC` |
| `kk_adv_wand` | Initiate Wand public-safe baseline | `addons/kaykit_character_pack_adventures/Assets/gltf/wand.gltf` | `READY_PUBLIC` |
| `kk_adv_spellbook_open` | Apprentice Focus / spellbook open-state baseline | `addons/kaykit_character_pack_adventures/Assets/gltf/spellbook_open.gltf` | `READY_PUBLIC` |
| `kk_adv_spellbook_closed` | Apprentice Focus closed-state baseline | `addons/kaykit_character_pack_adventures/Assets/gltf/spellbook_closed.gltf` | `READY_PUBLIC` |
| `kk_adv_crossbow_2h` | later Hunter crossbow technical/visual baseline | `addons/kaykit_character_pack_adventures/Assets/gltf/crossbow_2handed.gltf` | `READY_PUBLIC` |
| `kk_adv_arrow` | ranged projectile scale baseline | `addons/kaykit_character_pack_adventures/Assets/gltf/arrow.gltf` | `READY_PUBLIC` |
| `kk_adv_quiver` | Hunter/quiver attachment baseline | `addons/kaykit_character_pack_adventures/Assets/gltf/quiver.gltf` | `READY_PUBLIC` |
| `kk_adv_mug_full` | inn/tavern drink prop only | `addons/kaykit_character_pack_adventures/Assets/gltf/mug_full.gltf` | `READY_PUBLIC` |
| `kk_adv_mug_empty` | inn/tavern empty-mug state | `addons/kaykit_character_pack_adventures/Assets/gltf/mug_empty.gltf` | `READY_PUBLIC` |

Important boundaries:

- these exact assets establish public-safe geometry/scale/attachment baselines; they do not force the entire R01 art direction to become the Adventurers pack if a stronger accepted family exists;
- a crossbow is not substituted for the canonical `Riverthorn Bow` merely because its exact path is known;
- `mug_full` is a tavern prop, **not** the Healing Potion bottle.

## 4.1 Phase-B KayKit filename evidence

Detailed source evidence is recorded in `R01_ASSET_PHASE_B_EVIDENCE_2026-09-15.md`.

The official `KayKit-Restaurant-Bits-1.0` repository was observed at commit `153c8a7535b48237854cb54ff6890679f8c574d1`. Exact public-tree entries include:

```text
food_dinner.gltf
food_stew.gltf
stew_bowl.gltf
stew_pot.gltf
food_ingredient_steak.gltf
food_ingredient_steak_pieces.gltf
food_ingredient_ham_cooked.gltf
```

A third-party mirror of KayKit packs was used only as filename/tree inspection evidence for packs whose itch pages do not expose internal names. It exposed these exact filename families:

```text
Fantasy Weapons Bits:
  bow_A / bow_B / bow_C (+ withString variants)
  sword_A ... sword_G
  hammer_A ... hammer_D
  spear_A / spear_B
  staff_A ... staff_D
  shield_A ... shield_D
  wand_A / wand_B

RPG Tools Bits:
  anvil.gltf
  hammer.gltf
  pickaxe.gltf
  grindstone.gltf
  tongs.gltf
  mallet.gltf
  axe.gltf
  shovel.gltf
  knife.gltf
  saw.gltf
```

Those names are no longer guesses, but final A/B/C variant selection remains blocked until actual mesh/scale/grip/silhouette review. The mirror is **not** the license authority or preferred acquisition source.

---

# 5. R01 armor / apparel / NPC clothing bindings

## 5.1 Preferred visual family

Gameplay identities remain:

- `River Scholar Garb` — Light;
- `Wayfarer Leathers` — Medium;
- `Ironbound Guard` — Heavy.

Preferred visual family remains:

- Quaternius `Modular Character Outfits - Fantasy`;
- Quaternius `Universal Base Characters` for coherent settlement NPC bodies/head/hair combinations where used.

Current published pack properties are strong enough to keep the family selected: 12 outfits, 62 modular parts, 3 texture variants, humanoid rig, retargeting and UAL compatibility.

Phase-B pass 3 found an extracted Standard snapshot whose embedded `License_Standard.txt` identifies the Standard version as the free partial package under CC0 and says remaining outfits/separate parts belong to the Source version. Current Quaternius itch also lists `Modular Character Outfits - Fantasy[Standard].zip` as a free CC0 download. The exact snapshot is still treated as evidence until the project acquires the intended creator-controlled archive and hashes it.

Current intake state:

| Use | Source family | Status | Phase-B requirement |
|---|---|---|---|
| River Scholar Garb | Modular Character Outfits - Fantasy | `ARCHIVE_INSPECTION_REQUIRED` | Standard Ranger/Peasant are not a truthful scholar silhouette; inspect current Standard/Source or another coherent family, preserve source license/hash, choose exact parts |
| Wayfarer Leathers | Standard Ranger part family | exact candidate filenames pinned; `ARCHIVE_INSPECTION_REQUIRED` until source acquisition + rig/clip review | evaluate `*_Ranger_Body/Arms/Legs/Feet`, optional `Facewear`/`Pauldron`; verify dodge/roll and bow draw clipping |
| Ironbound Guard | Modular Character Outfits - Fantasy | `ARCHIVE_INSPECTION_REQUIRED` | Standard Ranger/Peasant are not accepted as heavy armor; inspect other legal parts/source family and verify guard/heavy-attack clipping |
| guild/class trainer | Outfits + Universal Base Characters | `ARCHIVE_INSPECTION_REQUIRED / READY_LOCAL_ONLY` | exact body/hair/outfit/prop IDs still needed |
| smith | Peasant/Ranger candidate + external tool prop | exact clothing candidate family exists; role composition pending | acquire/hash Standard, inspect silhouette, bind KayKit hammer/anvil and work animation |
| alchemist/healer | Peasant candidate + bottle/cauldron prop | clothing candidate exists; role-specific final still pending | exact body/head/hair + potion/cauldron selection |
| inn worker | Standard Peasant part family | exact candidate filenames pinned; visual role review pending | acquire/hash Standard and author 1–2 coherent combinations |
| merchant | Standard Peasant baseline + role props | candidate family exists; final silhouette pending | avoid cloning inn/civilian appearance |
| stable handler | Standard Peasant/Ranger baseline | candidate family exists; final silhouette + tack/brush prop pending | verify mount interaction poses |
| guards | Ranger candidate is not automatically guard armor | `ARCHIVE_INSPECTION_REQUIRED` | 2–3 authored variations with sufficiently defensive silhouette |
| civilians/travelers | Standard Peasant part family | exact candidate filenames pinned; variation authoring pending | create a small coherent variation library rather than clones |

Exact Standard modular part families observed:

```text
Female_Peasant_Arms / Body / Feet / Legs
Male_Peasant_Arms / Body / Feet / Legs

Female_Ranger_Arms / Body / Facewear / Feet / Legs / Pauldron
Male_Ranger_Arms / Body / Facewear / Feet / Legs / Pauldron
```

Matching whole-outfit glTFs also exist for Female/Male Peasant and Ranger, including `NoBody` variants. Full paths and evidence are recorded in `R01_ASSET_PHASE_B_PASS3_EVIDENCE_2026-09-15.md`.

Runtime conversion target remains Fabric 26.2 `Armor Model API` for project-owned worn geometry unless asset testing proves a hard blocker.

## 5.2 Apparel acceptance

One full armor family must pass all of these before the family becomes `READY_*`:

- idle/walk/sprint;
- directional dodge/roll/dash;
- jump/landing;
- guard/perfect-guard;
- representative 1H and heavy attack;
- spell cast;
- drink/eat;
- down/revive;
- Trail Stag mount/dismount.

Catastrophic skirt/robe/pauldron clipping is an asset/rig problem to fix or reject; it is not a reason to fall back to vanilla armor.

---

# 6. R01 weapon and equipment model intake

## 6.1 Grounded ordinary R01 bases

| Canonical base | Preferred external source | Current intake status |
|---|---|---|
| Heartland Arming Sword | Quaternius Modular Weapons if acquisition license is preserved/verified; KayKit `sword_1handed.gltf` is public-safe fallback/baseline | `READY_PUBLIC` fallback, preferred Quaternius `ARCHIVE_INSPECTION_REQUIRED` |
| Wayfarer Daggers | Quaternius paired daggers preferred; KayKit `dagger.gltf` public-safe baseline | same |
| Quarry Maul | KayKit Fantasy Weapons Bits `hammer_A..D` candidate family or accepted Quaternius hammer | exact candidate filenames pinned; final model `ARCHIVE_INSPECTION_REQUIRED` pending mesh review/source SHA-256 |
| River Pike | Fantasy Weapons Bits `spear_A/B` / accepted Quaternius spear | exact candidate filenames pinned; final model pending visual review |
| Riverwood Bow | KayKit Fantasy Weapons Bits `bow_A/B/C_withString` family | exact candidate filenames pinned; final variant pending visual review |
| Initiate Staff | Fantasy Weapons Bits `staff_A..D` preferred; exact Adventurers `staff.gltf` available as public-safe baseline | exact candidate filenames pinned; `READY_PUBLIC` baseline |
| Initiate Wand | Fantasy Weapons Bits `wand_A/B` preferred; exact Adventurers `wand.gltf` available | exact candidate filenames pinned; `READY_PUBLIC` baseline |
| Watch Buckler | preferred external round shield; exact KayKit `shield_round.gltf` exists; Fantasy Weapons Bits `shield_A..D` candidate family also known | `READY_PUBLIC` baseline; alternate variant review pending |
| Apprentice Focus | exact KayKit `spellbook_open.gltf` / `spellbook_closed.gltf` is accepted public-safe book-focus baseline | `READY_PUBLIC` |

## 6.2 First dungeon deterministic rewards

- `Ironroot Longsword`: exact candidate universe is now evidence-backed as `sword_A.gltf` through `sword_G.gltf`; final letter is **not** selected until the meshes are visually inspected for a grounded straight-sword silhouette, grip and Minecraft scale.
- `Riverthorn Bow`: exact candidate universe is `bow_A_withString.gltf`, `bow_B_withString.gltf`, `bow_C_withString.gltf`; final letter waits for draw/silhouette review.
- `Lumenwood Staff`: exact candidate universe is `staff_A.gltf` through `staff_D.gltf`; final letter waits for caster silhouette, grip and spell-VFX origin review.

The canonical stats/effects do not authorize using whatever model happens to be easiest during coding. A filename-tree pass narrows the choice but does not replace quality review.

## 6.3 Mythic/signature models

`Hartcrown Spear`, `Rootquake Maul`, `Earthscale Ward` remain **asset-blocked for final presentation** until the chosen external model actually supports their silhouette/identity.

Current evidence-backed candidate universes are:

- Hartcrown Spear: `spear_A.gltf`, `spear_B.gltf`;
- Rootquake Maul: `hammer_A.gltf` through `hammer_D.gltf`;
- Earthscale Ward: `shield_A.gltf` through `shield_D.gltf`.

These are not automatically Mythic-quality. Do not call a recolored ordinary spear/hammer/shield Mythic merely because mechanics are complete.

---

# 7. Potions, meals and workstation props

## 7.1 Potions

Canonical items:

- Healing Potion;
- Focus Draught;
- Cleansing Tonic.

Preferred prop family:

- Quaternius `Fantasy Props MegaKit Standard` CC0 snapshot when the exact creator-uploaded/source artifact is acquired and preserved; otherwise another coherent accepted bottle family.

Phase-B pass 3 pins the real Fantasy Props candidate filename universe:

```text
Potion_1.gltf
Potion_2.gltf
Potion_3.gltf
Potion_4.gltf
```

`Potion_1.gltf` is independently corroborated by a creator-domain path reference and by a consumer of the OpenGameArt Standard archive. `Potion_2`, `Potion_3` and `Potion_4` are also observed in downstream extracted Fantasy Props trees/manifests.

Current status: **exact candidate filenames pinned; final three-item assignment still `ARCHIVE_INSPECTION_REQUIRED`.** Do not map Healing/Focus/Cleansing to 1/2/3 solely from filenames or color.

Requirements:

- inspect all candidates in 3D and choose three with sufficiently distinct silhouette/contents/readability;
- same accepted 3D model is the basis for world/hand presentation and inventory icon render;
- bottle hand pivot must align with UAL1 `Drink` or whichever final drink clip wins visual review;
- no vanilla potion bottle as final presentation;
- preserve source-specific CC0 evidence and calculate project-local SHA-256 from the exact archive/files actually used.

## 7.2 Food

Canonical R01 meals:

- Herbed Louxia Roast;
- Trail Skewers;
- Glow Broth.

Source family: `KayKit Restaurant Bits`, CC0. The official public GitHub repository gives path-level evidence rather than guessed names.

Current Phase-B mapping:

| Meal | Exact observed candidate | Status |
|---|---|---|
| Herbed Louxia Roast | `addons/kaykit_restaurant_bits/Assets/gltf/food_dinner.gltf` | candidate filename pinned; 3D appearance review pending |
| Trail Skewers | no `skewer` filename established in observed official free tree | `ARCHIVE_INSPECTION_REQUIRED`; do not invent filename |
| Glow Broth | `addons/kaykit_restaurant_bits/Assets/gltf/food_stew.gltf`; compare `stew_bowl.gltf` | candidate filename pinned; final visual selection pending |

The observed tree also includes ingredient/cooking support such as `food_ingredient_steak.gltf`, `food_ingredient_steak_pieces.gltf`, `food_ingredient_ham_cooked.gltf` and `stew_pot.gltf`.

## 7.3 Forge / alchemy / cooking tools

Source family: `KayKit RPG Tools Bits` — current free page states 45+ CC0 models including hammer, anvil, axe, pickaxe, tools, blueprints and related production props.

Filename inspection evidence now pins:

```text
hammer.gltf
anvil.gltf
pickaxe.gltf
grindstone.gltf
tongs.gltf
mallet.gltf
axe.gltf
shovel.gltf
knife.gltf
saw.gltf
```

Status: license/family `READY_PUBLIC`; exact names are evidence-backed, but authoritative archive acquisition/source SHA-256 and actual model inspection remain pending.

Visible workstation actions must bind the prop to an accepted work animation, not leave the tool floating while the vanilla arm swings.

---

# 8. Player / NPC animation intake

## 8.1 Locomotion and universal dodge

Primary source: current `KayKit Character Animations` 1.1/current release family, 161 CC0 humanoid animations.

Current public description explicitly includes movement/dodging plus melee, ranged/spellcasting and tool/work categories.

Binding rule:

```text
canonical dodge duration = 0.45 s
canonical movement target = ~3.2 blocks
canonical i-frame = 0.30 s
canonical Stamina cost = 30
```

The selected external clip is retimed/retargeted to these gameplay values; gameplay is not changed just to preserve the source clip's raw displacement.

Legacy CC0 named fallback clips are verified by their source page:

- `Roll`
- `Dash Front`
- `Dash Back`
- `Dash Right`
- `Dash Left`

Current status:

- current-pack directional dodge exact archive filenames: `ARCHIVE_INSPECTION_REQUIRED`;
- legacy named clips: `REFERENCE/READY_PUBLIC SOURCE`, but they still need modern-rig retarget and actual Minecraft visual review before binding.

## 8.2 Combat / work motions

Accepted source families:

- Better Combat dependency for weapon attack runtime/cadence where applicable;
- current KayKit Character Animations for melee/ranged/spellcasting/blocking/tool actions;
- Quaternius UAL/UAL2 only after source-specific acquisition-time license is recorded under the Quaternius rule in §2.

KayKit 1.1's authoritative devlog now pins exact R01 work/fishing clip names:

```text
Hammer / Hammering
Pickaxe / Pickaxing
Chop / Chopping
Dig / Digging
Saw / Sawing
Work_A / Work_B / Work_C
Working_A / Working_B / Working_C
Fishing_Bite
Fishing_Cast
Fishing_Catch
Fishing_Idle
Fishing_Reeling
Fishing_Struggling
Fishing_Tug
```

R01 visible work minimum therefore has an exact clip family for smithing, generic work, mining and fishing. These clips still require retarget/visual acceptance; the devlog name is not equivalent to `PLAYTESTED`.

UAL2 is retained as a coherent alternate/cross-check where a Quaternius-rig motion clips materially better with the selected body/outfit family. A downstream imported 134-clip UAL2 library confirms real clips such as `TreeChopping_Loop`, `Mining_Loop`, `Fish_Cast_Idle_Loop`, `Farm_Harvest` and `Farm_PlantSeed`.

No production action is accepted with only a vanilla arm bob if a full-body external animation is player-visible.

## 8.3 Eat / drink / revive / mount gaps

KayKit 1.1 still does **not** supply evidence for eating/drinking; its current page describes those as planned future actions. Other accepted/verified external candidates are therefore used instead of inventing placeholders.

| Action | Current evidence | Current state |
|---|---|---|
| meal eat | Quaternius UAL2 exact clip `Consume` | `ARCHIVE_INSPECTION_REQUIRED`; exact candidate identified, source acquisition/license/hash + retarget/prop/Minecraft review pending |
| potion drink | Quaternius UAL1 exact clip `Drink` | `ARCHIVE_INSPECTION_REQUIRED`; exact candidate identified, source acquisition/license/hash + bottle alignment/Minecraft review pending |
| teammate revive/help-up | Fab `Revive & Downed Animation Pack` exposes paired reviver/reviving clips | `NEEDS_EXTERNAL_CLIP`; `VERIFY / LOCAL_ONLY` candidate only until license/acquisition + two-character alignment review |
| Trail Stag mount | Fab `Modular Classic Horse` exposes rider mount animations/root-motion variants | `NEEDS_EXTERNAL_CLIP`; `VERIFY / LOCAL_ONLY` candidate; horse-to-stag retarget/clearance not proven |
| Trail Stag dismount | same pack explicitly exposes rider dismount animations/root-motion variants | `NEEDS_EXTERNAL_CLIP`; same gate |

Evidence-backed paired revive candidate names include:

```text
anim_Knocked_Reviver
anim_Knocked_Reviving
anim_Reviver_Without_Medic_L
anim_Reviver_Without_Medic_R
anim_Reviver_With_Medic_L2
anim_Reviver_With_Medic_R2
anim_Downed_Revive_With_Medic_L2
anim_Downed_Revive_With_Medic_R2
```

These are discovery candidates, not project assets. Marketplace terms, source format, skeleton compatibility and quality must be checked before acquisition/use.

For Trail Stag, a horse rider transition is only useful if it can be retargeted without implausible hip height, hand target, antler/body collision or a visible mismatch with the server mount snap. If not, reject it and keep searching.

Do not close any row with a two-keyframe hand-to-face placeholder or instant teleport onto the mount.

---

# 9. Settlement / structure intake

Preferred design language remains Quaternius `Medieval Village MegaKit` because the existing R01 canon was built around its coherent modular medieval/fantasy architecture.

Phase-B pass 3 identifies a direct creator-uploaded OpenGameArt Standard snapshot:

```text
medieval_village_megakitstandard.zip
license = CC0
published Standard count = 176 models
```

A curated consumer of that exact Standard family records real modular source paths including:

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

This materially improves provenance and gives the starting settlement a public-safe candidate modular vocabulary. It does **not** mean service buildings are already designed.

The starting settlement still requires authored composition/visual acceptance for:

- gate;
- shrine silhouette/details;
- inn/tavern;
- guild/class hall;
- forge/smith;
- bank/storage;
- healer/alchemy;
- stable/paddock;
- starter homes;
- market/quest-board props.

Acquisition rule:

- if the exact OpenGameArt CC0 Standard snapshot is used, preserve that page/archive identity and calculate the project-local archive/file SHA-256;
- if a newer Quaternius package is used instead, preserve that exact package's source/license evidence rather than transferring the historical snapshot's status automatically;
- service-specific layout quality still requires 3D/Minecraft review.

Fallback/replacement architecture must come from one coherent external family such as an accepted Kenney CC0 set, not a collage of unrelated schematics.

---

# 10. VFX intake

Baseline public-safe source: Kenney `Particle Pack`, 80 CC0 VFX sprites.
Secondary: Kenney `Smoke Particles` for dust/smoke treatment.

Phase-A bindings:

| R01 event | External source family | Status | Acceptance note |
|---|---|---|---|
| dodge foot dust | Kenney particles/smoke | `READY_PUBLIC` family | small and directional; never obscures i-frame timing |
| perfect-guard spark | Kenney particles + external impact audio | `READY_PUBLIC` family | frame must coincide with server success |
| heal/focus/cleanse resolve | Kenney particle basis | `READY_PUBLIC` family | three readable treatments, not recolor-only |
| quarry dust / collapse | Kenney smoke/particles | `READY_PUBLIC` family | world feedback, not hitbox clutter |
| Earthloong lightning lane support | Kenney particles only as component | `READY_PUBLIC` component | actual ground warning geometry must match canonical lane |
| loot grade highlight | Kenney component pool | `READY_PUBLIC` component | obey Lucifer/loot presentation language |

Phase-B pass 2 pinned real candidate filename families from a public packaging of Kenney Particle Pack:

```text
circle_01.png ... circle_05.png
dirt_01.png ... dirt_03.png
fire_01.png ... fire_02.png
flame_01.png ... flame_06.png
flare_01.png
light_01.png ... light_03.png
magic_01.png ... magic_05.png
```

These are **candidate names**, not final assignments. Filename alone does not establish that `dirt_01` is the best dodge dust or that `magic_01` is the best heal VFX. Actual sprite inspection and in-game blend/scale/timing review remain required.

Boss signature VFX may require additional external sources. Kenney is a reusable component pool, not an excuse for generic particles on every important attack.

---

# 11. Audio intake

Public-safe baseline:

- Kenney `RPG Audio` — 50 CC0 RPG/foley files;
- Kenney `Impact Sounds` — 130 CC0 impact/foley files;
- Kenney `UI Audio` / Interface sound families — CC0 UI feedback.

Phase-A intended bindings:

- basic weapon impact family;
- guard/perfect-guard/guard-break layers;
- footsteps/ordinary movement where superior to vanilla and compatible with surface logic;
- pickup/loot confirmation;
- inventory/service UI confirm/cancel/error;
- quarry stone/impact baseline;
- smith/workstation baseline.

Phase-B pass 2 pinned real candidate families from public indexes/repackaging trees:

```text
Impact Sounds:
  Audio/impactMetal_heavy_000.ogg ... impactMetal_heavy_004.ogg
  Audio/impactMetal_medium_000.ogg ... impactMetal_medium_004.ogg
  Audio/impactMetal_light_000.ogg ... impactMetal_light_004.ogg
  Audio/impactMining_000.ogg ... impactMining_004.ogg

UI Audio:
  click1.wav ... click5.wav
  mouseclick1.wav
  mouserelease1.wav
  rollover1.wav ... rollover6.wav
  switch1.wav ...
```

The authoritative Kenney pages remain the license authority. These indexes are filename evidence only. The original archive was not successfully acquired in this environment, and audio was not auditioned, so no final clip assignment or project acquisition SHA-256 is claimed.

Regalhart and Earthloong attacks, Mythic drops and major class ultimates require distinctive accepted audio; they are not considered complete merely because a generic Kenney impact can be played.

---

# 12. Intake record schema

Every production source binding eventually records:

```text
asset_id
player_facing_use
source_pack
source_page
source_version_or_commit
source_locator_or_archive_path
acquired_at
license_at_acquisition
license_evidence_path_or_url
adoption_status
raw_repo_allowed
source_sha256
conversion_pipeline
converted_output_path
scale_and_pivot_notes
animation_bindings[]
icon_render_source
vfx_or_sound_bindings[]
accepted_in_minecraft
accepted_date
review_notes
```

Rules:

- `source_sha256` hashes the actual downloaded source/archive or exact source file used, not a web page;
- generated/converted project files also receive hashes in the normal build/art pipeline where useful;
- if a source is `LOCAL_ONLY`, Git stores the manifest/path/integration instructions, not the forbidden raw bytes;
- screenshots/renders are evidence of acceptance, not license evidence;
- a SHA-256 copied from a third-party manifest is recorded as **reported evidence**, not as the project's own acquisition hash.

---

# 13. Conversion / implementation acceptance

## 13.1 3D item/armor

Before binding an external model:

1. inspect actual source file and license;
2. normalize scale while preserving silhouette;
3. set truthful grip/pivot/handedness;
4. verify visible reach against gameplay family;
5. convert through the selected Blockbench/Armor Model API/GeckoLib-compatible path as appropriate;
6. render the inventory icon from the same accepted model family;
7. test in actual Minecraft camera/FOV;
8. test required animation poses;
9. reject or repair catastrophic clipping/foot sliding/hand misalignment.

## 13.2 Motion

- server position/collision/cost/i-frame remain authoritative;
- retarget/retime source clips to canonical timing;
- align important event frames to within roughly one server tick where practical;
- collision-shortened dash/lunge must not leave a fake full-distance animation slide;
- root motion may guide the curve but does not override server movement.

## 13.3 VFX

- visible area must represent actual server hitbox/range;
- telegraph, impact and recovery state each have distinct timing;
- low-value ambient particles cannot hide a dangerous boss tell.

---

# 14. Phase-B progress and remaining gates before R01 is asset-ready

Phase-B evidence pass 1 completed:

1. re-verified current Quaternius QAL v1.0 against still-CC0-labeled individual pages;
2. preserved package-specific acquisition rules instead of flattening all Quaternius files to CC0;
3. verified authoritative KayKit CC0 pages for Fantasy Weapons Bits, RPG Tools Bits and Character Animations;
4. pinned exact KayKit Character Animations 1.1 tool/fishing clip names;
5. inspected the official Restaurant Bits GitHub tree and pinned roast/stew candidates while keeping Trail Skewers unresolved;
6. pinned evidence-backed Fantasy Weapons Bits filename families;
7. pinned evidence-backed RPG Tools Bits filenames.

Phase-B evidence pass 2 additionally completed:

8. verified source-specific Quaternius UAL/UAL2 CC0 publication evidence separately from the central QAL drift;
9. identified UAL2 `Consume` as an exact real eat-animation candidate;
10. identified UAL1 as a real drink-animation family without inventing a clip string;
11. found paired external teammate-revive and rider mount/dismount candidates for later license/rig/quality evaluation;
12. pinned Kenney Particle Pack filename families for R01 VFX evaluation;
13. pinned Kenney Impact Sounds and UI Audio filename families without falsely claiming audition or archive SHA-256.

Phase-B evidence pass 3 additionally completed:

14. pinned UAL1's exact drink candidate clip as `Drink` and retained UAL2 `Consume` for eat;
15. found creator-uploaded OpenGameArt CC0 Standard snapshots for Fantasy Props (94 models) and Medieval Village (176 models), so those exact historical artifacts no longer inherit a blanket current-QAL assumption;
16. pinned exact Standard Ranger and Peasant modular outfit part families and kept Scholar/Heavy armor unresolved instead of misusing them;
17. expanded the Fantasy Props potion universe to exact `Potion_1` / `Potion_2` / `Potion_3` / `Potion_4` candidates while keeping final three-potion visual assignment gated;
18. pinned real Medieval Village Standard wall/window/door/roof/prop module filenames for settlement-shell intake;
19. recorded all pass-3 evidence and boundaries in `R01_ASSET_PHASE_B_PASS3_EVIDENCE_2026-09-15.md`.

Still required:

1. actually acquire the exact source archives/files chosen by the project and calculate project-local SHA-256 values;
2. inspect and accept exact River Scholar Garb and Ironbound Guard parts/families;
3. visually select and accept the Wayfarer Ranger combination plus authored Peasant settlement-role combinations;
4. compose and visually accept each starting-settlement service building from the now-pinned modular vocabulary;
5. acquire the authoritative KayKit weapon/tools package bytes used by the project, record SHA-256, and visually choose final A/B/C variants rather than promoting mirror tree names directly;
6. finish Trail Skewers exact food source and inspect the Restaurant Bits candidate meshes;
7. visually inspect `Potion_1..4`, select three sufficiently distinct potion models, and verify hand pivots/icons;
8. acquire/inspect chosen UAL editions and visually accept UAL1 `Drink` + UAL2 `Consume` after retarget/prop alignment;
9. resolve licensing/acquisition/retarget quality for teammate revive/help-up and Trail Stag mount/dismount, or replace those candidates;
10. visually inspect and select exact Kenney VFX sprites;
11. actually audition and select exact Kenney audio clips;
12. run Blockbench/3D-viewer intake, then actual Minecraft visual review for the accepted set.

Until those rows are resolved, `R01 ASSET READY = NO`. Phase B is active and materially advanced, but not complete.

---

# 15. Verification state

Current state after Phase-B evidence pass 3:

- `DESIGN REVIEWED`: YES
- `EXTERNAL SOURCE REVIEWED`: YES
- `LICENSE METADATA REVIEWED`: YES, with Quaternius handled source/package-specifically rather than by blanket family assumption
- `PHASE-B PASS-3 EVIDENCE DOC RECORDED`: YES
- `EXACT PUBLIC-SAFE KAYKIT PATHS PINNED`: YES for listed Adventurers + Restaurant Bits evidence
- `KAYKIT WORK/FISHING CLIP NAMES PINNED`: YES
- `KAYKIT WEAPON/TOOLS CANDIDATE FILENAMES PINNED`: YES as tree evidence; authoritative archive SHA-256/final visual selection pending
- `QUATERNIUS STANDARD CC0 SNAPSHOT SOURCES IDENTIFIED`: YES for Fantasy Props + Medieval Village; project-local archive hashing pending
- `WAYFARER RANGER / PEASANT STANDARD PART NAMES PINNED`: YES; visual acceptance pending
- `POTION MODEL CANDIDATE FILENAMES PINNED`: YES — `Potion_1..4`; final three-model assignment pending
- `EAT EXTERNAL CLIP CANDIDATE PINNED`: YES — UAL2 `Consume`; source acquisition/visual acceptance pending
- `DRINK EXTERNAL CLIP CANDIDATE PINNED`: YES — UAL1 `Drink`; source acquisition/visual acceptance pending
- `REVIVE/MOUNT EXTERNAL CANDIDATES IDENTIFIED`: YES; not accepted
- `MEDIEVAL VILLAGE MODULE CANDIDATE NAMES PINNED`: YES for a useful Standard subset; service compositions not accepted
- `KENNEY VFX/AUDIO CANDIDATE FILENAME FAMILIES PINNED`: YES; visual/audition acceptance pending
- `ALL R01 EXACT ASSET FILENAMES PINNED`: NO
- `R01 ASSET READY`: NO
- `BLOCKBENCH / CONVERSION TESTED`: NO
- `BUILD VERIFIED`: NO
- `JAR PRODUCED`: NO
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO

Do not upgrade these labels merely because a pack page, filename, candidate clip or source model exists.