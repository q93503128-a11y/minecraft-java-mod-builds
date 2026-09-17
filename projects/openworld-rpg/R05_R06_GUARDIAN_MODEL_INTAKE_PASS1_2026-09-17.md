# Open-World RPG — R05 / R06 Guardian Exact-Model Intake Pass 1

> Date: **2026-09-17**  
> Status: **TARGETED INTAKE COMPLETE / EXACT MODELS STILL OPEN**  
> Repository baseline checked at start and immediately before write: `main` = `25249caf193b6ef4b776f0478496b67b7c580f3d`  
> Boss contract: `BOSS_REFERENCE_DESIGN_PASS_R05_R08_2026-09-17.md`  
> Binding matrix: `PRODUCTION_ASSET_BINDING_MATRIX.md`  
> R05 canon: `R05_IMPLEMENTATION_PACKAGE.md`  
> R06 canon: `R06_IMPLEMENTATION_PACKAGE.md`  
> Rule: this pass may reject or narrow candidates. It does **not** authorize a placeholder guardian or anatomy-specific final encounter sheet before a model actually passes direct 3D intake.

---

# 1. Why this pass happened now

The current priority order says to continue actual Azari spatial closure first if the real world archive is available.

Repository inspection found no usable Azari world payload (`level.dat`, region `.mca` data or an Azari world archive) in the current public `main`. `AZARI_SPATIAL_CLOSURE_PASS1.md` also still correctly records:

```text
ACTUAL AZARI WORLD LOADED: NO
EXACT COORDINATES VERIFIED: NO
```

Therefore this pass moved to the next approved pre-code gate: **targeted R05 / R06 guardian model intake**.

This is not a new boss-design pass. The encounter contracts were already closed. The only question here is whether a concrete external actor can honestly support those contracts.

---

# 2. Intake decision vocabulary used here

- `REJECT_FOR_SLOT` — enough public model/animation/visual evidence exists to show that the candidate conflicts with the already-closed R05/R06 contract. Do not spend acquisition time on it for this slot.
- `NEEDS_DIRECT_3D_REVIEW` — public evidence is promising enough to justify obtaining the legitimate artifact and inspecting mesh/rig/clips/materials in Blender/Minecraft scale, but not enough to accept it.
- `SOURCE_POOL_ONLY` — the pack/source is still useful generally, but no exact model in the published evidence is strong enough to become the guardian candidate yet.
- `LICENSE_CLARIFICATION_REQUIRED` — do not put raw bytes into the public repo until exact usage/redistribution terms for the acquired artifact are captured.
- `ACCEPTED` — **not used in this pass**. No candidate reached the direct-artifact standard required by the project.

The project intentionally does not assign numeric winner scores.

---

# 3. R05 Root-Vault Guardian — targeted results

Locked visual need:

> ancient regulator construct reclaimed by roots/stone; readable central control volume; articulated plates/limbs/rings/control arms; a broad exposed state; visible reconfiguration; clearly distinct from R03's rock-golem identity.

Locked combat relationship:

> visible root/channel routing → committed control action → generous open-core/control-region punish window.

## 3.1 OpenGameArt — `Pok` by Teh_Bucket

Source:

`https://opengameart.org/content/pok`

Published evidence:

- CC0;
- stone golem with carved surfaces;
- rigged;
- original sculpt included;
- 32k triangles;
- Unity scene/materials included;
- downloadable FBX/Blend/textures archive;
- included textures are also recorded as CC0-derived on the source page.

Disposition:

```text
R05: NEEDS_DIRECT_3D_REVIEW — EDITABLE-BASE PROSPECT ONLY
R06: REJECT_FOR_SLOT
PUBLIC RAW-BYTE LICENSE SIGNAL: STRONG (CC0), HASH/ARTIFACT STILL NOT CAPTURED
```

Why it remains alive for R05:

- the carved ancient-stone language can belong in an old regulator/root-vault space;
- the rigged source and original sculpt make legitimate adaptation materially easier than a closed/static mesh;
- polygon scale is not automatically unreasonable for one authored boss after Minecraft conversion/LOD decisions.

Why it is **not accepted**:

- the public page does not establish the required boss animation family;
- no published evidence proves a broad opening control core, rotating regulator component or real phase reconfiguration;
- a normal stone-golem silhouette would collide with the explicit requirement to remain distinct from R03's rock-golem presentation;
- project-owned additions must not become so extensive that `external-first model selection` silently turns into designing a different boss from scratch.

Direct review must answer:

1. Is the silhouette already distinct enough from R03 after Minecraft-scale conversion?
2. Are chest/torso/shoulder pieces separable enough to support a **large readable exposed state** without destructive remodeling?
3. Does the rig permit heavy committed control motions, or would every useful tell require a new skeleton?
4. Can root reclamation be added as material/secondary geometry without hiding the carved-regulator read?
5. Can a real phase change be created by legitimate part separation/reconfiguration rather than particle-only effects?

If the answer to 2–5 is mostly `no`, reject instead of forcing the contract onto the mesh.

## 3.2 PROTOFACTOR Inc. — `Golem`

Sources:

- `https://www.fab.com/listings/c74b579c-a793-49f6-8113-7b4312e58eab`
- storefront preview lineage also points to Sketchfab model id `54ce1483a81d42dc90ad647d731c9658`.

Published evidence:

- about 11.2k triangles;
- 34-bone rig;
- 25 animations on the current Fab listing;
- PBR material support;
- includes an **animated split-into-pieces** variant for construction/explosion style state changes;
- non-AI storefront metadata.

Disposition:

```text
R05: REJECT_FOR_SLOT
R06: REJECT_FOR_SLOT
GENERAL ANIMATION/DESTRUCTION REFERENCE: RETAIN
LICENSE: MARKETPLACE / ACQUIRED-ARTIFACT TERMS MUST BE RECORDED BEFORE ANY LOCAL INTAKE
```

Reason:

The asset is technically much stronger than a generic static golem, and the split-body variant proves a useful production lesson: a boss model can ship with an authored structural state change rather than faking one with VFX.

However, the visible design is still a conventional muscular rock golem. R05 explicitly needs an **ancient regulator first**, not another large rock creature, and must remain visually distinct from the already-selected R03 rock-golem direction. The model would require the project to invent most regulator identity, control components and core language itself. That fails the purpose of exact external model selection.

Keep the **structural-state-change precedent**, not the exact R05 actor.

## 3.3 CGTrader — `Forest Golem` by OnlyPro

Source:

`https://www.cgtrader.com/3d-models/character/fantasy-character/forest-golem`

Published evidence:

- Royalty Free License storefront listing, marked no-AI;
- rigged and animated;
- Epic-skeleton-based rig with IK bones;
- 9 in-place animations;
- roughly 46k triangles in the description;
- Maya/FBX/Unity/Unreal delivery directions;
- multiple 4K texture sets.

Disposition:

```text
R05: NEEDS_DIRECT_3D_REVIEW — VISUAL FIT CHECK ONLY
R06: REJECT_FOR_SLOT
PUBLIC REPO BYTES: DO NOT COMMIT; MARKETPLACE LICENSE/ACQUISITION BOUNDARY FIRST
```

Why it earns one direct-review slot:

- it is a real rigged/animated production asset rather than a thumbnail-only concept;
- the forest/stone material family is closer to the R05 root-vault environment than generic metal/sci-fi constructs;
- it already supplies enough animation infrastructure to test real Minecraft conversion rather than starting from a static statue.

Why it is still high-risk:

- `forest golem` can easily read as **nature creature first**, which is the wrong R05 story implication;
- nine generic animations do not prove open-core/recovery/reconfiguration coverage;
- no current evidence proves movable regulator/control components;
- marketplace source bytes must remain outside the public repo unless the exact acquired license permits the chosen handling.

Direct review is worthwhile only to decide whether the source mesh contains a genuine mechanical/armored substructure that the storefront text does not expose. If it is simply moss + stone + humanoid rig, reject it.

## 3.4 Standout 7 — `LOWPO: Fantasy Low Poly Golem Pack`

Source:

`https://standout7.itch.io/lowpo-golem`

Published evidence:

- free Rock/Earth/Iron golems; additional Crystal/Bone/Ice/Arcane variants in paid tiers;
- humanoid rig, Mixamo compatible;
- **no animations included**;
- low-poly/shared-atlas production direction;
- source `.blend` available in the full tier;
- itch metadata reports `Creative Commons Zero v1.0 Universal`;
- the creator's page separately says `No Reselling or Redistributing asset files`.

Disposition:

```text
R05: REJECT_FOR_SLOT
R06: REJECT_FOR_SLOT
LICENSE_CLARIFICATION_REQUIRED: YES — page metadata and written redistribution term are not flattened into one claim
```

Reason:

The visible Rock/Earth/Iron bodies are straightforward low-poly humanoid golems. They do not provide the central regulator, articulated control anatomy or authored phase state needed by R05, and they are even weaker for R06. The absence of bundled animations also makes this the wrong place to spend boss-intake time.

The license presentation is intentionally recorded as a conflict rather than casually calling the files `CC0` for public-repo redistribution.

## 3.5 Quaternius source pools

Sources:

- `https://quaternius.com/packs/ultimatemonsters.html`
- current Bestiary / Dungeon Monsters pack pages under Quaternius' current license system.

Published evidence reviewed in this pass:

- `Ultimate Monsters` advertises 50 fully animated monsters and older CC0 handling;
- current Bestiary / Dungeon Monsters material provides newer rigged/retargetable humanoid monster directions but does not bundle its own animation set and is under the current Quaternius Asset License path.

Disposition:

```text
R05: SOURCE_POOL_ONLY
R06: SOURCE_POOL_ONLY
```

No exact published model inspected in this pass demonstrated the required regulator/flow anatomy. Do not convert `Quaternius is a trusted source` into `therefore one of its generic monsters must be the boss`.

---

# 4. R06 Sunken Observatory Flow Guardian — targeted results

Locked visual need:

> hydromechanical or ceremonial **flow guardian** with visibly directional components — fins, vanes, broad orientable arms, gate-like plates, rings or another body mechanism whose motion honestly indicates current/lane direction.

Locked combat relationship:

> body changes visible flow lanes → player reads direction on stable/shallow footing → short earned opening isolates/disables one dangerous flow path.

R06 must **not** become `find a water-colored monster and add current particles`.

## 4.1 Fab / Cavity — `Water Golem`

Source:

`https://www.fab.com/listings/216f4d06-6831-4f77-a6f1-39a89b2a8c9c`

Published evidence:

- non-AI storefront metadata;
- rigged and skinned;
- about 32.6k polygons;
- 4K PBR/refraction-oriented materials;
- FBX/OBJ/3ds Max/Blend directions;
- visual identity is a water-elemental body whose look relies heavily on translucent/refraction treatment.

Disposition:

```text
R06: REJECT_FOR_SLOT
```

Reason:

It is a **water creature**, not infrastructure. The model does not visibly supply gates, vanes, rings or another directional mechanism that can own R06's flow lanes. Its signature presentation also depends strongly on refraction/translucent-water rendering, which would shift attack readability back toward materials/VFX rather than body-driven topology.

Using this because it is literally made of water would violate the closed R06 contract.

## 4.2 N-Hance Studio — `Stylized Magic Construct`

Source:

`https://www.fab.com/listings/b86c16b7-fa0f-449f-9dfd-2ca4ef725982`

Published evidence:

- non-AI storefront metadata;
- modular components;
- 37 animations;
- unarmed combat, stun, spell start/loop/end, locomotion and full swim directional animations;
- multiple color/material variants;
- FBX and converted GLB/glTF directions.

Disposition:

```text
R06: REJECT_FOR_SLOT
R05: REJECT_FOR_SLOT
ANIMATION-COVERAGE REFERENCE: RETAIN
```

Reason:

The animation coverage is excellent, but the visual family is a floating/humanoid magical armored construct. The published body does not make water-flow direction legible through hydromechanical anatomy, and generic spell animations would push R06 toward `caster + water VFX`, exactly what the contract forbids.

This is a useful example of why **animation count is not the same thing as encounter fit**.

## 4.3 N-Hance Studio — `Stylized Steel Guardian`

Source:

`https://www.fab.com/listings/4c1b6ce9-1b9d-4585-b831-97a5dd0306b6`

Published evidence:

- non-AI storefront metadata;
- 40 animations;
- ground and flight locomotion;
- gun attack families plus unarmed combat;
- stylized mechanical/steampunk identity.

Disposition:

```text
R06: REJECT_FOR_SLOT
R05: REJECT_FOR_SLOT
```

Reason:

The body is technological, but the authored motion language is gun/flight/mecha combat rather than a ceremonial water-control mechanism. Adapting it would mean fighting the source design instead of benefiting from it. R06 needs directional **flow hardware**, not a generic flying robot recolored blue/green.

## 4.4 Generic water-elemental / elemental-golem packs

Several currently surfaced marketplace packs contain water golems or water-elemental warriors. AI-generated packs were rejected immediately as production candidates for this pass, and conventional water humanoids were rejected on the same encounter-fit basis as the Cavity Water Golem.

Disposition:

```text
R06: DO NOT CONTINUE BROAD `WATER GOLEM` SEARCH AS THE MAIN QUERY
```

Next discovery must search for **mechanical / ceremonial directional anatomy**, not elemental material alone.

---

# 5. What this pass actually closes

The search space is now smaller.

## R05

Still worth legitimate direct-artifact review:

1. `Pok` — CC0 editable-base prospect; direct mesh/rig/reconfiguration feasibility check required.
2. `Forest Golem` by OnlyPro — marketplace visual-fit check only; reject immediately if it reads as nature creature rather than infrastructure.

Explicitly not the answer:

- generic low-poly humanoid golem packs;
- PROTOFACTOR rock golem despite its strong split-body animation system;
- a Quaternius model chosen only because the source is familiar;
- any solution whose regulator identity exists mostly in particles or newly invented project geometry.

## R06

**No exact candidate survives this pass.**

That is an acceptable result.

Rejected directions now include:

- pure water-elemental body;
- generic magic construct/caster;
- generic steel/flying/gun guardian;
- generic golem recolor;
- `water` as a search keyword without visible control anatomy.

The next R06 query should be closer to:

```text
ancient hydromechanical sentinel
ceremonial floodgate guardian
animated fantasy automaton with rotating/orientable body components
mechanical aquatic sentinel with fins/vanes/gates/rings
```

A static miniature/STL is insufficient unless a credible rig/retarget plan exists and the visual is unusually strong.

---

# 6. Direct acquisition / review checklist for the surviving R05 prospects

When a legitimate archive is available, capture before acceptance:

```text
source URL
creator
purchase/download date
exact license text / store license selected
artifact filename
SHA-256
raw formats
triangle/material/texture counts
skeleton/bone names
animation clip names + durations
separate mesh/object list
root-motion assumptions
Minecraft target scale
camera occupancy at melee range
broad weak/open-state candidate volume
phase/reconfiguration feasibility
hitbox correspondence
conversion/retarget work estimate
public-repo / local-only handling
accept / reject
```

Required visual checks:

- front, side, rear silhouette;
- 1× and intended boss scale beside a player;
- normal FOV and common GUI scales;
- melee approach from multiple angles;
- large recovery/open-state readability;
- animation wind-up vs intended server hit timing;
- phase state without relying on a particle cloud;
- multiplayer camera overlap with two or more players nearby.

No final boss name, weak-point rule or signature material is locked until that review passes.

---

# 7. Provenance / repository boundary decisions

This pass separates `can inspect/use locally` from `can commit donor bytes publicly`.

- OpenGameArt `Pok`: source page says CC0, but the exact downloaded archive still needs hash/version capture before use.
- Standout 7 LOWPO: itch license metadata and creator-written redistribution restriction are both preserved; do not simplify the conflict.
- Fab / CGTrader marketplace candidates: source/model facts may be recorded publicly, but donor asset bytes remain **LOCAL_ONLY by default** until the actually selected/purchased license is captured and confirmed for the intended packaging.
- AI-generated marketplace candidates inspected during discovery are not promoted into the production shortlist.

No paid asset was bypassed or downloaded in this pass.

---

# 8. Next production action

The next meaningful step is one of:

1. if the actual Azari world archive becomes available, stop model research and perform real spatial calibration/closure first;
2. otherwise obtain and directly inspect the two surviving R05 artifacts where legally accessible, rejecting them quickly if the regulator anatomy is not real;
3. in parallel or immediately after, run a **new-shape R06 search** centered on hydromechanical/floodgate/ceremonial directional anatomy rather than `water golem`;
4. only after R05/R06 exact models close, author their anatomy-specific final encounter sheets;
5. then continue the same targeted model process for R07/R08.

Do not write temporary guardian entity code while these rows remain open.

---

# 9. Verification state

```text
DESIGN REVIEWED: YES — existing R05/R06 contract applied, not redesigned
EXTERNAL SOURCE REVIEWED: YES — targeted source/storefront evidence pass
LICENSE/PROVENANCE REVIEWED: PARTIAL — source-level terms captured; no final acquired artifact/hash yet
CODE REVIEWED: N/A
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

Docs/research-only change. Build/CI is intentionally not run.
