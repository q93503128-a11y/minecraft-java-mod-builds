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

The current Character Animations page still describes eating/drinking as planned future work. Therefore KayKit 1.1 itself is **not** evidence for R01 eat/drink. Phase-B pass 2 located separate Quaternius UAL candidates in §8.

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

## 7. Kenney VFX/audio filename evidence — pass 2

Authoritative Kenney pages remain the license authority and state CC0 for the relevant packs:

- `Particle Pack` — 80 sprites;
- `Smoke Particles`;
- `Impact Sounds` — 130 impact/foley sounds;
- `RPG Audio`;
- `UI Audio` / interface families.

The environment did **not** successfully acquire the original Kenney ZIPs during this pass. Therefore no project acquisition SHA-256 is claimed.

Public package indexes/repackaging trees were used only to pin real filename families for later audition/visual review.

### Particle Pack candidate names

A public Godot packaging of Kenney Particle Pack exposes the expected source filenames, including:

```text
circle_01.png ... circle_05.png
dirt_01.png ... dirt_03.png
fire_01.png
fire_02.png
flame_01.png ... flame_06.png
flare_01.png
light_01.png ... light_03.png
magic_01.png ... magic_05.png
```

R01 use remains candidate-only until the sprites are visually inspected. Likely evaluation pools:

- dodge/quarry dust → `dirt_*`;
- perfect-guard/heal/focus → `flare_*`, `light_*`, `magic_*` as components;
- never select by filename alone.

### Impact Sounds candidate names

A public Kenney data index exposes real Impact Sounds paths including:

```text
Audio/impactMetal_heavy_000.ogg ... impactMetal_heavy_004.ogg
Audio/impactMetal_medium_000.ogg ... impactMetal_medium_004.ogg
Audio/impactMetal_light_000.ogg ... impactMetal_light_004.ogg
Audio/impactMining_000.ogg ... impactMining_004.ogg
```

These are candidate families for guard/perfect-guard, weapon/armor contact, quarry/mining and smithing. No exact final clip is pinned because this pass did not audition the audio.

### UI Audio candidate names

A public Godot packaging exposes exact filenames including:

```text
click1.wav ... click5.wav
mouseclick1.wav
mouserelease1.wav
rollover1.wav ... rollover6.wav
switch1.wav ...
```

Again, exact confirm/cancel/hover/error assignments remain audition-gated.

## 8. Quaternius Universal Animation Library / UAL2 — eat and drink evidence

This pass located a stronger coherent motion source than inventing temporary eat/drink clips.

### Source/licensing evidence

Observed public source pages:

- Quaternius `Universal Animation Library` — 120+ animations, universal humanoid rig, page marked CC0;
- Quaternius `Universal Animation Library 2` — 130+ animations covering farming/fishing/parkour/civilian actions, page marked CC0;
- Quaternius itch pages for UAL/UAL2 also advertise CC0 versions;
- OpenGameArt uploads by Quaternius expose source-specific CC0 downloads for Standard editions.

Because the central Quaternius site now also publishes QAL v1.0, R01 must preserve **the exact source/acquisition evidence actually used**. An OpenGameArt/itch artifact explicitly offered as CC0 is stronger package-specific evidence than assuming all current Quaternius downloads are CC0.

The original binary package was not successfully downloaded in this environment, so source ZIP SHA-256 remains pending.

### Exact eat clip

A real downstream project that imported the Quaternius vendor libraries reports:

```text
UAL2 clip: Consume
mapped use: eat
```

The same audit identifies `UAL2.glb` as a 134-clip library with chop/mine/fish/farm/carry/eat/sleep and confirms the native UAL naming convention.

Therefore R01 `meal eat` is no longer “no external candidate found.” The exact evidence-backed candidate is:

```text
Quaternius Universal Animation Library 2
clip = Consume
status = ARCHIVE_INSPECTION_REQUIRED
```

It still requires acquisition-time license capture, confirmation that the acquired edition includes `Consume`, retarget/prop alignment and actual Minecraft review before binding.

### Drink family

The same downstream audit reports that its 120-clip `UAL1.glb` contains and wires a drink action. This is enough to establish **UAL1 as a real drink candidate family**, but the audit sample does not publish the exact raw drink clip string.

Therefore:

```text
R01 potion drink
source candidate = Quaternius UAL1
exact clip name = NOT YET PINNED
status = ARCHIVE_INSPECTION_REQUIRED
```

Do not guess `Drink`, `Drinking`, `Drink_Loop` or any other filename until the source archive/clip list is directly inspected.

## 9. Revive/help-up and Trail Stag mount/dismount candidates — pass 2

These rows are still not accepted, but they now have real external sources to evaluate.

### Teammate revive/help-up

A Fab `Revive & Downed Animation Pack` listing exposes paired reviver/reviving clips, including names such as:

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

This is functionally much closer to the required two-character help-up than a generic self-`Revive` animation.

Current R01 state: `NEEDS_EXTERNAL_CLIP`, with this pack retained as `VERIFY / LOCAL_ONLY CANDIDATE` until exact Fab license/price/acquisition, skeleton compatibility, pair alignment and Minecraft quality are checked.

### Trail Stag mount/dismount

A Fab `Modular Classic Horse` listing advertises 36 horse and 38 rider unique animations and explicitly includes rider **mount** and **dismount** animations, including root-motion variants.

It is a useful motion reference/candidate, but a horse saddle/torso transition is not automatically correct for Trail Stag proportions. Antler/body collision, rider hip height, hand target and server mount snap timing must be tested.

Current R01 state: `NEEDS_EXTERNAL_CLIP`, with this pack retained as `VERIFY / LOCAL_ONLY CANDIDATE`, not adopted.

## 10. Still unresolved after pass 2

Do not fill these with placeholders:

- Quaternius package-specific acquisition license files and project-owned source SHA-256;
- exact River Scholar / Wayfarer / Ironbound modular outfit parts and settlement NPC outfits;
- exact starting-settlement Medieval Village modules;
- final KayKit A/B/C weapon variant after mesh review;
- Trail Skewers model;
- potion bottle models;
- UAL1 potion-drink exact raw clip name and acquired-edition confirmation;
- acquired/verified UAL2 `Consume` source package + retarget/visual acceptance;
- accepted teammate revive/help-up clip;
- accepted Trail Stag mount/dismount clips;
- final Kenney VFX sprite choices after visual review;
- final Kenney audio choices after actual audition;
- Blockbench/3D-viewer and in-Minecraft visual acceptance.

Phase B advanced materially through pass 2, but `R01 ASSET READY = NO`.
