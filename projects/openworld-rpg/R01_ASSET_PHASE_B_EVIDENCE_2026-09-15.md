# Open-World RPG — R01 Asset Intake Phase-B Evidence — 2026-09-15

> Canon: `GAME_DESIGN.md`  
> Intake: `R01_ASSET_INTAKE.md`  
> Provenance: `EXTERNAL_SOURCES.md`

This records only evidence actually observed. A filename existing does not mean final acceptance: source-package acquisition where applicable, SHA-256, 3D/rig review and in-Minecraft review still gate production use.

## 1. Quaternius license drift

Current central page `https://quaternius.com/license.html` publishes `Quaternius Asset License (QAL) v1.0`, updated 2026-08-28. It permits use/modification and incorporation into a completed Product, but prohibits redistributing the raw Assets themselves as assets. §7 states later license changes do not retroactively change assets already obtained under an earlier license.

At the same time, current individual pages such as:

- `https://quaternius.com/packs/modularcharacteroutfitsfantasy.html`
- `https://quaternius.com/packs/medievalvillagemegakit.html`

still display `CC0`.

Therefore the project keeps package-specific acquisition evidence as authoritative and does not normalize Quaternius to a blanket `CC0` statement. New/current packages without preserved acquisition evidence remain local/private Product candidates; raw public-repo inclusion is not assumed.

## 2. KayKit authoritative pages

Authoritative current pages observed:

- `https://kaylousberg.itch.io/fantasy-weapons-bits` — CC0; free tier 25+ models; sword/axe/hammer/bow/staff/wand/shield/spear families.
- `https://kaylousberg.itch.io/rpg-tools-bits` — CC0; free tier 45+ models; pickaxe/hammer/axe/mallet/anvil/hand-tools/etc.
- `https://kaylousberg.itch.io/kaykit-character-animations` — CC0; 161 humanoid animations; current free label `Free 1.1`.
- `https://kaylousberg.itch.io/kaykit-animations` — legacy CC0 fallback containing named `Roll` and directional `Dash` clips.

The current Character Animations page still describes eating/drinking as planned future animation work. R01 `meal eat` and `potion drink` therefore remain `NEEDS_EXTERNAL_CLIP`.

## 3. Character Animations 1.1 exact work/fishing names

Authoritative devlog:

`https://kaylousberg.itch.io/kaykit-character-animations/devlog/1139588/character-animations-update-11`

Exact published 1.1 tool names:

```text
Chop
Chopping
Dig
Digging
Fishing_Bite
Fishing_Cast
Fishing_Catch
Fishing_Idle
Fishing_Reeling
Fishing_Struggling
Fishing_Tug
Hammer
Hammering
Holding_A
Holding_B
Holding_C
Lockpick
Lockpicking
Pickaxe
Pickaxing
Saw
Sawing
Work_A
Work_B
Work_C
Working_A
Working_B
Working_C
```

This pins the exact source clip families for smithing, mining, generic work and fishing. Retarget quality and source-archive SHA-256 remain pending.

## 4. Restaurant Bits — official GitHub tree

Official repository:

`https://github.com/KayKit-Game-Assets/KayKit-Restaurant-Bits-1.0`

Observed main commit: `153c8a7535b48237854cb54ff6890679f8c574d1`.

Relevant exact files under `addons/kaykit_restaurant_bits/Assets/gltf/`:

```text
food_dinner.gltf
food_stew.gltf
stew_bowl.gltf
stew_pot.gltf
food_ingredient_steak.gltf
food_ingredient_steak_pieces.gltf
food_ingredient_ham_cooked.gltf
```

Observed tree search found no filename containing `skewer` and no dedicated filename containing `soup`.

Candidate mapping, pending 3D review:

- Herbed Louxia Roast → `food_dinner.gltf` candidate.
- Glow Broth → `food_stew.gltf` candidate; compare with `stew_bowl.gltf` in 3D.
- Trail Skewers → unresolved; do not fabricate a filename.

## 5. Fantasy Weapons Bits filename inspection

A public third-party mirror was used **only for tree/filename inspection**, not as license authority or acquisition source:

`https://github.com/GeorgeQLe/assets-kaykit-3d-props`

Mirror path: `assets/kaykit/fantasy-weapons-bits-1.0/Assets/gltf/`.

Observed exact names:

```text
bow_A.gltf
bow_A_withString.gltf
bow_B.gltf
bow_B_withString.gltf
bow_C.gltf
bow_C_withString.gltf

sword_A.gltf ... sword_G.gltf
hammer_A.gltf ... hammer_D.gltf
spear_A.gltf
spear_B.gltf
staff_A.gltf ... staff_D.gltf
shield_A.gltf ... shield_D.gltf
wand_A.gltf
wand_B.gltf
halberd.gltf
scythe.gltf
```

Consequences:

- Ironroot Longsword candidate universe: `sword_A`–`sword_G`.
- Riverthorn Bow: `bow_A/B/C_withString`.
- Lumenwood Staff: `staff_A`–`staff_D`.
- Hartcrown Spear: `spear_A/B` candidate universe, still Mythic-quality blocked.
- Rootquake Maul: `hammer_A`–`hammer_D`, still Mythic-quality blocked.
- Earthscale Ward: `shield_A`–`shield_D`, still Mythic-quality blocked.

Names are evidence-backed, but final letter selection is still blocked on actual mesh/silhouette/grip/scale review. KayKit's itch page remains authoritative for the CC0 license; the mirror is not substituted for provenance.

## 6. RPG Tools Bits filename inspection

The same mirror was used only to inspect the pack tree. Observed exact glTF names include:

```text
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

R01 candidates now have evidence-backed filenames for smith hammer/anvil, mining pickaxe and forge support props. Authoritative package acquisition, SHA-256 and model review remain pending.

## 7. Still unresolved

Do not fill these with placeholders:

- Quaternius package-specific acquisition license files and SHA-256;
- exact River Scholar / Wayfarer / Ironbound modular outfit parts and settlement NPC outfits;
- exact starting-settlement Medieval Village modules;
- final KayKit A/B/C weapon variant after mesh review;
- Trail Skewers model;
- potion bottle models;
- drink/eat, revive/help-up, Trail Stag mount/dismount clips;
- exact Kenney VFX/audio files after archive inspection/audition;
- Blockbench/3D-viewer and in-Minecraft visual acceptance.

Phase B advanced materially, but `R01 ASSET READY = NO`.
