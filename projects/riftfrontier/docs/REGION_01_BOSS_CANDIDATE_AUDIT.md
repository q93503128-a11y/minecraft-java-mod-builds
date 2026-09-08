# Region 01 Boss Candidate Audit

Status: **BOUNDED CANDIDATE AUDIT COMPLETE — NO PRODUCTION ASSET SELECTED**

Date: 2026-09-09

This audit follows `REGION_01_BOSS_PRESENTATION_GATE.md`. It is a bounded review of real redistribution-eligible candidates, not a license-only shortlist and not a substitute for final Blockbench/Minecraft inspection.

## Evidence boundary

The current automation environment can inspect public previews, source metadata, repository file metadata and license pages, but cannot decode the remote binary GLB inside Blockbench. Therefore:

- public preview silhouette and creator/source metadata were inspected;
- exact GLB metadata was available for `Dragon Evolved` from an independent parser and a public GitHub mirror;
- pack-level animation claims were verified from the original Quaternius source;
- exact animation clip names, bone hierarchy, material layout and deformation quality remain unverified until the original GLB is locally inspected;
- no candidate is promoted to `SELECTED` on incomplete rig evidence.

## Candidate set

### 1. Quaternius — Dragon Evolved

Source:
- original family: https://quaternius.com/packs/ultimatemonsters.html
- individual discovery page: https://poly.pizza/m/LlwD0QNUPj

License: CC0 / public domain.

Observed technical evidence:
- GLB/FBX family is fully animated;
- independent GLB parser reports 4,350 vertices, 7,438 triangles, 8 animations, 70 nodes;
- reported bounds: 5.48 × 2.86 × 2.42;
- public GitHub mirror at commit `371f69f03e295c509faa65f5a4bd20ad32834b8f` exposes `dragon_evolved.glb` blob SHA `114c2311759fd037d01ffd74c1ca4a1199f874eb`, 436,000 bytes;
- preview has a strongly readable frontal face, wings, horns and oversized forelimbs/claws.

Gate assessment:
- readable facing: **PASS at public-preview level**;
- attack-bearing mass: **PASS candidate** — forelimbs/claws, head/horns, wings and body provide multiple readable attack channels;
- committed-strike headroom: **PROMISING, NOT VERIFIED** — forelimbs support a broad strike silhouette but exact rig control is not inspected;
- line/displacement role: **PROMISING, NOT VERIFIED** — body axis and wings can plausibly communicate a charge/displacement action;
- arena-pressure role: **PROMISING, NOT VERIFIED** — wings/head/body give enough distinct channels to author an area-pressure telegraph without reusing the same limb;
- Minecraft scale/performance: **TECHNICALLY PLAUSIBLE** — 7.4k triangles is not itself disqualifying for one boss, but material/draw-call cost and in-game scale still need measurement;
- texture/style fit: **NOT PASSED** — the source is a bright rounded low-poly style and must not be imported unchanged merely because the rig is convenient;
- exact animation inventory/rig: **NOT VERIFIED**.

Decision: **CONDITIONAL FRONT-RUNNER / NOT SELECTED.**

This is the only candidate in this bounded set worth direct binary rig inspection next. It cannot become production art until exact clip names, skeleton hierarchy, deformation, material count, Minecraft scale and required telegraph/recovery pose headroom are inspected.

### 2. Quaternius — Blue Demon

Source: https://poly.pizza/m/S7jYW6Amye

License: CC0 / public domain.

Observed preview:
- readable biped facing;
- one handheld club provides a clear attack-bearing object;
- proportions are rounded and compact.

Gate assessment:
- readable facing: pass;
- committed strike: possible through club arm;
- line/displacement and arena-pressure vocabulary: weak without substantial new animation/VFX invention;
- silhouette: too generic/compact for the first Region 01 major boss;
- style adaptation cost: high relative to the amount of reusable boss identity retained.

Decision: **REJECTED for Region 01 first boss.**

Reason: the asset could serve as a lower-tier creature/elite study, but making it satisfy all three required boss communication roles would require redesigning most of its presentation identity.

### 3. Quaternius — Goleling Evolved

Source: https://poly.pizza/m/iHEuXiH6Aj

License: CC0 / public domain.

Observed preview/source metadata:
- animated flying creature;
- strong wing/head silhouette but limited obvious grounded attack-bearing mass.

Decision: **REJECTED for Region 01 first boss.**

Reason: the silhouette is readable, but the current boss contract needs a broad committed impact action plus line/displacement and visible arena pressure. The candidate's body plan would push too much readability burden onto VFX or whole-body translation before exact rig evidence justifies that choice.

### 4. Quaternius — Mushroom King

Source: https://poly.pizza/m/grnFTziU8u

License: CC0 / public domain.

Observed preview:
- very strong cap/head silhouette;
- small limbs and a handheld round object;
- compact, upright body.

Decision: **REJECTED for Region 01 first boss.**

Reason: silhouette recognition is good, but the attack-bearing geometry is too weak for the required commitment/displacement language without replacing the model's core visual identity.

## Newer Quaternius Bestiary kit

Source:
- https://quaternius.com/packs/bestiarydungeonmonsterskit.html
- https://quaternius.itch.io/bestiary-dungeon-monsters-kit
- license: https://quaternius.com/license.html

Technical merit:
- 7 game-ready monsters;
- 3 color variants;
- optimized low-poly topology;
- humanoid retargetable rig;
- no bundled animations; intended to use the Universal Animation Library.

License gate:
- these 2026 assets use QAL rather than CC0;
- QAL permits use/modification and distribution of a completed Product;
- QAL prohibits redistributing the Assets themselves as standalone assets, including modified assets.

Decision: **NOT APPROVED FOR RAW PUBLIC-SOURCE-REPOSITORY BUNDLING.**

Riftfrontier's public repository exposes files in `src/main/resources` directly. Until the license holder clarifies that this source-repository distribution is permitted, do not commit raw QAL model/texture/source files. This is a conservative repository-distribution decision, not a claim that the kit cannot be used in a completed game product.

## Technical intake tool

`tools/inspect_gltf.py` is added with this audit so the next real local GLB can be inspected deterministically before any manifest/renderer work.

It reports:
- glTF/GLB 2.0 validity;
- file size;
- node/mesh/primitive counts;
- material/texture/image counts;
- skin count and maximum joints per skin;
- animation count and exact names;
- estimated vertices/triangles from accessors;
- POSITION accessor bounds where available.

Optional fail-closed gates use stable issue codes for:
- missing skin;
- too few animations;
- triangle/material/bone budget overflow;
- missing required animation-name tokens;
- unnamed animation clips.

This tool is **not** allowed to auto-approve visual quality, licensing, hitbox alignment or production selection.

## Exact next boundary

1. Obtain the original CC0 `Dragon Evolved` GLB from the creator/source family in a local environment capable of binary inspection.
2. Run `tools/inspect_gltf.py --json` and archive the output with the asset review.
3. Inspect skeleton hierarchy, exact clip names, material count, axes/bounds and deformation in Blockbench/Blender or an equivalent viewer.
4. Test whether distinct committed-strike telegraph/ACTIVE/recovery, line/displacement telegraph and area-pressure telegraph poses can be authored without fighting the rig.
5. Only if those checks pass, mark the exact asset `SELECTED` in `THIRD_PARTY_ASSETS.md` and then add permitted bytes / renderer integration.
6. If Dragon Evolved fails, begin a new bounded CC0/redistribution-safe source set; do not lower the gate.
