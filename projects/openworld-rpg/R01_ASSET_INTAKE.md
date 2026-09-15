# Open-World RPG — R01 External Asset Intake Manifest

> Status: **ASSET INTAKE PHASE A / SOURCE BINDINGS + LICENSE BOUNDARIES LOCKED**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Opening: `R01_VERTICAL_SLICE.md`  
> Equipment: `EQUIPMENT_BALANCE.md`, `LOOT_ECONOMY.md`  
> Appearance/recovery: `RECOVERY_PRODUCTION_APPEARANCE.md`  
> Provenance registry: `EXTERNAL_SOURCES.md`  
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

---

# 3. Public-safe baseline families

## 3.1 KayKit

Current KayKit pages used by R01 explicitly state CC0 and allow personal/commercial use without attribution.

Accepted source families:

- `KayKit Character Animations` — 161 humanoid animations; locomotion/dodging, melee, ranged/spellcasting, tool/work actions;
- legacy `KayKit Character Animations` — exact named fallback clips including `Roll`, `Dash Front`, `Dash Back`, `Dash Right`, `Dash Left`;
- `KayKit Fantasy Weapons Bits` — free tier 25+ fantasy weapon models, broader pack 40+; swords, axes, hammers, bows, staves, wands, shields, spears, etc.;
- `KayKit RPG Tools Bits` — free tier 45+ tool/production props; hammer, anvil, pickaxe, axe, hand tools, blueprints, etc.;
- `KayKit Restaurant Bits` — 140+ food/kitchen models, including ingredients and raw/cooked/chopped states;
- `KayKit Character Pack: Adventurers 1.0` GitHub repository — exact public CC0 package used below for path-level bindings and fallback compatibility models.

The Adventurers repository itself contains a CC0 `LICENSE.txt`; repository main tree observed at commit `672074b73ba276876a19e8816ecdc5241817ab47` during this intake.

## 3.2 Kenney

Current Kenney asset pages used here state CC0.

Accepted R01 source families:

- `Particle Pack` — 80 VFX sprites;
- `Smoke Particles` — dust/smoke secondary pool;
- `RPG Audio` — footsteps/weapon/RPG foley baseline;
- `Impact Sounds` — 130 impact/foley sounds;
- `UI Audio` / `Interface Sounds` — menu/confirmation/error/notification baseline.

Kenney is a baseline reusable pool, not automatic permission to give every boss the same generic sound. Signature bosses/Mythics may require stronger dedicated external audio later.

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
- `Riverthorn Bow` remains tied to `Fantasy Weapons Bits` and is `ARCHIVE_INSPECTION_REQUIRED` until its actual bow filename is recorded;
- `mug_full` is a tavern prop, **not** the Healing Potion bottle.

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

However exact R01 part names/paths are **not invented from screenshots**.

Current intake state:

| Use | Source family | Status | Phase-B requirement |
|---|---|---|---|
| River Scholar Garb | Modular Character Outfits - Fantasy | `ARCHIVE_INSPECTION_REQUIRED / READY_LOCAL_ONLY` | download legal package, save package license, choose exact head/chest/legs/gloves/boots parts, hash files |
| Wayfarer Leathers | same | `ARCHIVE_INSPECTION_REQUIRED / READY_LOCAL_ONLY` | same; verify dodge/roll and bow draw clipping |
| Ironbound Guard | same | `ARCHIVE_INSPECTION_REQUIRED / READY_LOCAL_ONLY` | same; verify guard/perfect-guard/heavy attack clipping |
| guild/class trainer | Outfits + Universal Base Characters | `ARCHIVE_INSPECTION_REQUIRED / READY_LOCAL_ONLY` | exact body/hair/outfit/prop IDs |
| smith | same + external tool prop | `ARCHIVE_INSPECTION_REQUIRED / READY_LOCAL_ONLY` | exact outfit + KayKit hammer/anvil binding |
| alchemist/healer | same + bottle/cauldron prop | `ARCHIVE_INSPECTION_REQUIRED / READY_LOCAL_ONLY` | exact outfit/prop IDs |
| inn worker | same | `ARCHIVE_INSPECTION_REQUIRED / READY_LOCAL_ONLY` | exact civilian/tavern combination |
| merchant | same | `ARCHIVE_INSPECTION_REQUIRED / READY_LOCAL_ONLY` | exact merchant silhouette |
| stable handler | same | `ARCHIVE_INSPECTION_REQUIRED / READY_LOCAL_ONLY` | exact handler outfit + tack/brush prop |
| guards | same | `ARCHIVE_INSPECTION_REQUIRED / READY_LOCAL_ONLY` | 2–3 authored variations, not clones |
| civilians/travelers | same | `ARCHIVE_INSPECTION_REQUIRED / READY_LOCAL_ONLY` | small coherent variation library |

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
| Quarry Maul | KayKit Fantasy Weapons Bits or accepted Quaternius hammer | `ARCHIVE_INSPECTION_REQUIRED` |
| River Pike | Fantasy Weapons Bits / accepted Quaternius spear | `ARCHIVE_INSPECTION_REQUIRED` |
| Riverwood Bow | KayKit Fantasy Weapons Bits grounded bow | `ARCHIVE_INSPECTION_REQUIRED` |
| Initiate Staff | KayKit Fantasy Weapons Bits preferred; exact Adventurers `staff.gltf` available as public-safe baseline | `READY_PUBLIC` baseline |
| Initiate Wand | KayKit Fantasy Weapons Bits preferred; exact Adventurers `wand.gltf` available | `READY_PUBLIC` baseline |
| Watch Buckler | preferred external round shield; exact KayKit `shield_round.gltf` exists | `READY_PUBLIC` baseline |
| Apprentice Focus | exact KayKit `spellbook_open.gltf` / `spellbook_closed.gltf` is accepted public-safe book-focus baseline | `READY_PUBLIC` |

## 6.2 First dungeon deterministic rewards

- `Ironroot Longsword`: use the best grounded straight-sword source accepted from the above family; exact model must be named in Phase B before implementation.
- `Riverthorn Bow`: Fantasy Weapons Bits bow; exact archive file required.
- `Lumenwood Staff`: Fantasy Weapons Bits staff; exact archive file required even though `kk_adv_staff` can serve as a technical fallback.

The canonical stats/effects do not authorize using whatever model happens to be easiest during coding.

## 6.3 Mythic/signature models

`Hartcrown Spear`, `Rootquake Maul`, `Earthscale Ward` remain **asset-blocked for final presentation** until the chosen external model actually supports their silhouette/identity.

Do not call a recolored ordinary spear/hammer/shield Mythic merely because mechanics are complete.

---

# 7. Potions, meals and workstation props

## 7.1 Potions

Canonical items:

- Healing Potion;
- Focus Draught;
- Cleansing Tonic.

Preferred prop family:

- accepted Quaternius fantasy potion props if acquisition license evidence supports the intended handling; otherwise another coherent CC0 bottle set must be selected.

Current status for all three exact bottles/icons: `ARCHIVE_INSPECTION_REQUIRED`.

Requirements:

- distinct silhouette/contents enough to distinguish the three without color alone;
- same accepted 3D model is the basis for world/hand presentation and inventory icon render;
- bottle hand pivot must align with the final drink animation;
- no vanilla potion bottle as final presentation.

## 7.2 Food

Canonical R01 meals:

- Herbed Louxia Roast;
- Trail Skewers;
- Glow Broth.

Source family: `KayKit Restaurant Bits` — current page states 140+ CC0 food/kitchen models.

Status: `ARCHIVE_INSPECTION_REQUIRED` for exact filenames, but **license family accepted**.

Phase B chooses the exact cooked-meat/skewer/bowl/soup models from the real archive; do not invent filenames.

## 7.3 Forge / alchemy / cooking tools

Source family: `KayKit RPG Tools Bits` — current free page states 45+ CC0 models including hammer, anvil, axe, pickaxe, tools, blueprints and related production props.

Status: license/family `READY_PUBLIC`; exact filenames `ARCHIVE_INSPECTION_REQUIRED`.

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
- Quaternius UAL/UAL2 only after acquisition-time license is recorded under the Quaternius rule in §2.

R01 visible work minimum:

- smith hammering;
- generic crafting/work interaction;
- pickaxe/mining;
- fishing when fishing is introduced;
- pickup/interact;
- appropriate spellcast/channel motions.

No production action is accepted with only a vanilla arm bob if a full-body external animation is player-visible.

## 8.3 Still unresolved clips

The current KayKit Character Animations page does **not** guarantee eating/drinking; those are described as planned rather than present.

Therefore:

| Action | Current state |
|---|---|
| potion drink | `NEEDS_EXTERNAL_CLIP` |
| meal eat | `NEEDS_EXTERNAL_CLIP` |
| teammate revive/help-up | `NEEDS_EXTERNAL_CLIP` |
| Trail Stag mount | `NEEDS_EXTERNAL_CLIP` unless the accepted animal/humanoid package supplies a proven transition |
| Trail Stag dismount | `NEEDS_EXTERNAL_CLIP` |

Do not close these rows with a two-keyframe hand-to-face placeholder or instant teleport onto the mount.

---

# 9. Settlement / structure intake

Preferred design language remains Quaternius `Medieval Village MegaKit` because the existing R01 canon was built around its coherent modular medieval/fantasy architecture.

Due to §2, new raw package handling is:

```text
private playable use: allowed subject to current QAL/package terms
public repo raw asset bytes: false until acquisition-time license is recorded
exact module IDs: ARCHIVE_INSPECTION_REQUIRED
```

The starting settlement still requires exact Phase-B intake for:

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

A Minecraft block translation inspired by the external architecture must still be reviewed for source-license implications before it is packaged publicly. The private playable build may use local integration where the terms permit it.

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

Exact filenames remain `ARCHIVE_INSPECTION_REQUIRED` until real archives are downloaded and auditioned.

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
- screenshots/renders are evidence of acceptance, not license evidence.

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

# 14. Phase-B work still required before R01 is asset-ready

Phase A deliberately does **not** claim these are finished:

1. download/inspect the actual current Quaternius packages legally used by the project;
2. preserve acquisition-time license files/screens/evidence and calculate archive/file SHA-256 values;
3. resolve exact modular part filenames for River Scholar Garb / Wayfarer Leathers / Ironbound Guard and settlement NPC roles;
4. resolve exact Medieval Village/Fantasy Props module filenames for all starting-settlement services;
5. inspect KayKit `Fantasy Weapons Bits`, `Restaurant Bits`, `RPG Tools Bits` archives and record exact filenames for bow/maul/pike/staff/meal/tool models;
6. choose exact external potion bottle files;
7. choose actual external drink/eat clips;
8. choose actual external revive/help-up and mount/dismount clips;
9. audition and pin exact Kenney VFX/audio files;
10. run Blockbench/3D-viewer intake, then actual Minecraft visual review for the accepted set;
11. reconcile stale generic Quaternius CC0 classifications in `EXTERNAL_SOURCES.md` against package-specific acquisition evidence.

Until those rows are resolved, `R01 ASSET INTAKE = PHASE A COMPLETE`, **not** full asset-ready/implemented/playtested.

---

# 15. Verification state

Current state after Phase A:

- `DESIGN REVIEWED`: YES
- `EXTERNAL SOURCE REVIEWED`: YES
- `LICENSE METADATA REVIEWED`: YES, with Quaternius drift explicitly unresolved per package
- `EXACT PUBLIC-SAFE KAYKIT PATHS PINNED`: YES for the listed Adventurers assets
- `ALL R01 EXACT ASSET FILENAMES PINNED`: NO
- `BLOCKBENCH / CONVERSION TESTED`: NO
- `BUILD VERIFIED`: NO
- `JAR PRODUCED`: NO
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO

Do not upgrade these labels merely because a pack page or source model exists.