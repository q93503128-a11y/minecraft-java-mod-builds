# Open-World RPG — Production Asset Binding Matrix

> Status: **ACTIVE CANON — production-facing visual/source binding triage before source bootstrap**  
> Date: 2026-09-17  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Provenance registry: `EXTERNAL_SOURCES.md`  
> R01 detailed intake: `R01_ASSET_INTAKE.md`  
> R05–R08 boss-selection contract: `BOSS_REFERENCE_DESIGN_PASS_R05_R08_2026-09-17.md`  
> Rule: this matrix does not redesign gameplay. It only records whether a player-facing visual/content slot already has a concrete external source, needs runtime acceptance, or still needs model selection.

---

# 1. Production visual rule

Player-facing visual design is **external-first from the first real implementation**.

For UI, models, outfits, structures, important props, animations, VFX and audio:

```text
canonical gameplay need
→ accepted external design / asset family / dependency
→ provenance + license boundary
→ real scale / animation / readability review
→ production binding
→ implementation
```

Do **not** implement a temporary player-facing visual and plan to replace it later.

In particular, do not ship or build production screens/content around:

- improvised black translucent UI panels;
- arbitrary AI-made frames, icons or decorative borders;
- placeholder vanilla entities for important custom actors;
- temporary particle-only boss attacks;
- generic developer-art structures;
- temporary `TODO` / `prototype` / `debug` visual treatment.

If an exact player-facing source is unresolved, keep that visible slot gated and continue other work. Do not let a placeholder silently become the final design.

Gameplay/system design remains project-owned. **Visual design is not delegated to ad-hoc implementation taste.** External assets/references may be adapted/composed to fit the project, but the first production-facing version must already use the accepted external visual language.

---

# 2. Binding status vocabulary

- `DEPENDENCY_VALIDATE` — final actor family comes from an installed dependency. No donor raw bytes are copied into the public repo; current 26.2 model/animation/hitbox/behavior must still pass real integration review.
- `EXTERNAL_CANDIDATE_VALIDATE` — one concrete external model/artifact direction is already selected; acquire/hash/convert/inspect it before final binding.
- `OPEN_MODEL_SELECTION` — gameplay/encounter role is locked but no final visual has been accepted. This is a real pre-code gate for that visible content.
- `ASSET_INTAKE_ACTIVE` — exact family/files are partly pinned but acquisition/hash/visual acceptance is incomplete.
- `BOUND_DIRECTION` — external design family is already canon; exact individual file/variant binding may remain.
- `REJECTED` — inspected candidate is not allowed to become the finished presentation.

No status in this file means `PLAYTESTED`.

---

# 3. License / dependency correction — Threateningly Mobs Continued

As of 2026-09-17, storefront metadata conflicts:

- Modrinth project/version pages display **MIT**;
- CurseForge project/license pages display **All Rights Reserved**;
- current 26.2 Fabric files exist on both storefronts.

References:

- `https://modrinth.com/mod/threateningly-mobs-continued`
- `https://modrinth.com/mod/threateningly-mobs-continued/version/1.1.1%2Bfabric.26.2`
- `https://www.curseforge.com/minecraft/mc-mods/threateninglly-mobs-continued/license`

Until the exact canonical upstream license for the installed artifact is resolved:

```text
Threateningly Mobs Continued = DEPENDENCY-ONLY for production planning
raw source/model/texture/audio bytes copied into public repo = FORBIDDEN BY DEFAULT
project-owned stats/spawn/rewards/state = allowed through normal integration boundary
```

Any older live wording that flattens the continuation to `MIT` is stale for raw-byte reuse decisions. Runtime dependency use remains distinct from copying donor assets/code.

---

# 4. Region-by-region major actor matrix

| Region | Player-facing slot | Current external direction | Status | What actually remains |
|---|---|---|---|---|
| R01 | Earthloong dungeon boss | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | current 26.2 model/animation/hit volume, project attack/VFX alignment |
| R01 | Regalhart / major field identity | Threateningly lineage/dependency direction already in R01 canon | `DEPENDENCY_VALIDATE` | current presentation and hitbox acceptance |
| R01 | Trail Stag | Quaternius animated Stag source direction | `EXTERNAL_CANDIDATE_VALIDATE` | exact artifact/license-at-acquisition, conversion, mount/dismount rider motion, Minecraft review |
| R01 | player outfits / weapons / tools / food / settlement / potions | Quaternius/KayKit/Kenney families pinned in `R01_ASSET_INTAKE.md` | `ASSET_INTAKE_ACTIVE` | acquisition hashes, exact variant choice, 3D/Minecraft acceptance |
| R02 | Grovebound Warden | Quaternius `Goleling Evolved` direction | `EXTERNAL_CANDIDATE_VALIDATE` | exact source artifact, clips, scale/silhouette, final binding |
| R02 | Edras, Last Curator | Threateningly Lich dependency direction | `DEPENDENCY_VALIDATE` | current 26.2 presentation, reinforcement animation/readability |
| R03 | Whitecrest Griffin | VitSh `Griffin Animated` | `EXTERNAL_CANDIDATE_VALIDATE` | downloadable artifact, attribution/hash, flight/landing clips, weak-point viability |
| R03 | Forgewake Colossus | Dm3d `Rock Golem` OpenGameArt candidate | `EXTERNAL_CANDIDATE_VALIDATE` | exact license path, Blender conversion, fracture presentation, animation quality |
| R04 | Ferox Iceworm | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | burrow/surface animation, hit volume, camera/readability |
| R04 | Icebroodmother + Ice Weaver | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | boss/minion current-model quality, add readability, VFX/hitbox |
| R05 | mature Earthloong field boss | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | current 26.2 quality + distinct mature encounter presentation |
| R05 | root-vault / regulator dungeon guardian | ancient regulator construct with readable core + articulated control components | `OPEN_MODEL_SELECTION` | design/reference contract closed in `BOSS_REFERENCE_DESIGN_PASS_R05_R08_2026-09-17.md`; targeted exact-model search + 3D/license review only |
| R06 | Hydra field boss | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | multi-head hit/readability, water combat behavior, authored basin integration |
| R06 | Sunken Observatory dungeon guardian | hydromechanical/ceremonial flow guardian with directional components | `OPEN_MODEL_SELECTION` | design/reference contract closed; targeted exact-model search must support visible flow lanes and shallow-water arena readability |
| R07 | Ferox Deathworm field boss | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | sand/burrow presentation, melee access, hitbox |
| R07 | buried fortress/cistern dungeon guardian | armored infrastructure sentinel; Armor of Desert only if it passes direct review | `OPEN_MODEL_SELECTION` | design/reference contract closed; accept/reject Armor of Desert or choose replacement against visible armor/seam/pressure-state criteria |
| R08 | Titan Rabbit ritual boss | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | current model/animation/hitbox, stomp/leap honesty, signature material visual |
| R08 | Moonpriest / Knowledge Fairy authored identities | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | current presentation, density/readability, no donor progression leakage |
| R08 | Glass-Root Archive final guardian | magical archive/regulator guardian with visible movable lens/ring/root/crystal components | `OPEN_MODEL_SELECTION` | design/reference contract closed; targeted exact-model search must support field redirection and visible reconfiguration |
| R09 | Caravan Elephant | Alex's Mobs Continued | `DEPENDENCY_VALIDATE` | current 26.2 riding/charge/body-width behavior and project registration integration |
| R09 | Executioner dungeon boss | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` with replace-if-fail rule | current animation/reach/poise quality; replace if boss bar exceeds presentation quality |
| R09 | optional Lv49 dryland field boss | distinct heavy external creature not yet accepted | `OPEN_MODEL_SELECTION` | exact model/name/anatomy/signature material; no scaled Rhino placeholder |
| R10 | Basalt Wyvern | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | flight uptime/melee access, dive tell, camera/hitbox |
| R10 | Scorch Golem | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | current model/attack quality and forge-space readability |
| R10 | Inferno / final forge guardian | Threateningly Inferno is candidate, not automatically accepted | `OPEN_MODEL_SELECTION` | direct current review → accept Inferno or bind stronger volcanic guardian; then exact boss name/attack sheet |
| R11 | Jungle/sea transition mount: Laviathan | Alex's Mobs Continued | `DEPENDENCY_VALIDATE` | current multipart stability, seats/controller, project registration and R11 handling course |
| R11 | Riptooth | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | fair surface/open-water uptime, charge/breach readability |
| R11 | Abyss Fang | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | giant multipart/hitbox/camera, Charge/Devour fairness, 3D arena telegraphs |
| R11 | Giant Squid / Cachalot and other large aquatic actors | Alex's Mobs Continued | `DEPENDENCY_VALIDATE` | current 26.2 multipart regression/smoke test before regional admission |
| R11 | Sea-Fort / Freebooter Keep final boss | corsair/armored humanoid or distinct fort guardian | `OPEN_MODEL_SELECTION` | exact final external model + weapon/reach animations |
| R12 | Terradragon optional world boss | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | current ultra-boss model/animation/hitbox/airtime and project-authored no-grief behavior |
| R12 | Sky Drake | Quaternius animated Dragon direction | `EXTERNAL_CANDIDATE_VALIDATE` | exact artifact/license evidence, conversion, flight/mount animation review |
| R12 | final systemic guardian | current inspected Magic Construct / Steel Guardian candidates rejected | `OPEN_MODEL_SELECTION` | select a genuinely systemic/segmented final visual; only then final name/anatomy/attack binding |
| R12 | low-density anomaly creatures | candidate Farseer/Murmur/Reaper identities not verified | `OPEN_MODEL_SELECTION` | verify current dependency visuals or choose stronger external actors; keep roster sparse |

---

# 5. Slots that are **not** a new-model-search problem

Do not waste another research pass looking for replacement models merely because these rows are not yet `PLAYTESTED`:

- R01 Earthloong / Regalhart direction;
- R02 Edras/Lich;
- R04 Ferox Iceworm / Icebroodmother / Ice Weaver;
- R05 mature Earthloong;
- R06 Hydra;
- R07 Ferox Deathworm;
- R08 Titan Rabbit / Moonpriest / Knowledge Fairy;
- R09 Elephant and provisional Executioner;
- R10 Basalt Wyvern / Scorch Golem;
- R11 Laviathan / Riptooth / Abyss Fang and selected Alex aquatic ecology;
- R12 Terradragon.

Their next step is **actual current-version runtime/visual acceptance**, not another broad web search.

---

# 6. True major model-selection queue

The current large visible search queue is intentionally narrow:

1. R05 root-vault dungeon guardian — reference/design contract closed; exact model open;
2. R06 Sunken Observatory dungeon guardian — reference/design contract closed; exact model open;
3. R07 buried fortress/cistern dungeon guardian — reference/design contract closed; exact model open;
4. R08 Glass-Root Archive final guardian — reference/design contract closed; exact model open;
5. R09 optional dryland field boss;
6. R10 Inferno accept/replace decision;
7. R11 Sea-Fort boss;
8. R12 final systemic guardian;
9. R12 low-density anomaly roster only where current dependency candidates fail.

R02/R03 already have concrete external candidates and therefore belong to acquisition/conversion review, not broad discovery.

For R05–R08, do not restart generic boss ideation. Candidate selection must use `BOSS_REFERENCE_DESIGN_PASS_R05_R08_2026-09-17.md`.

---

# 7. Cross-cutting presentation binding

## UI

Canon visual family remains:

- Foozle `Lucifer - RPG UI` primary;
- `Lucifer - Equipment` for inventory/equipment continuation;
- Kenney `Fantasy UI Borders` / `UI Pack - Adventure` only as support primitives.

Status: `BOUND_DIRECTION`.

Implementation rule: the first player-facing HUD/inventory/skill/service screen must already use the accepted Lucifer-family visual language. Do not first build a generic Minecraft/black-panel screen and promise to skin it later.

## Player equipment / NPC outfits

R01 intake has already pinned Wizard/Scholar, Ranger/Wayfarer and Knight/Ironbound directions plus KayKit weapon/tool families.

Status: `ASSET_INTAKE_ACTIVE`.

Expand region-specific wardrobe only from coherent accepted families. Do not invent one-off armor design language per region.

## Structures

Macro terrain and silhouettes use Minecraft/Azari world composition. External modular architecture/props provide the authored visual language for settlements, services, dungeons and Anchor machinery.

Status: mixed `BOUND_DIRECTION` / `ASSET_INTAKE_ACTIVE`.

No temporary vanilla-village shell is accepted as a production settlement.

## VFX / audio

Kenney public-safe families supply reusable baseline components. Major bosses, Mythic drops, class ultimates and finale states still require distinctive exact bindings.

Status: `ASSET_INTAKE_ACTIVE`.

A generic particle/sound is not an acceptable finished signature effect merely because it makes the code path visible.

---

# 8. What this closes

This matrix closes one planning ambiguity:

> `external asset gate` no longer means `search for everything again`.

It now means one of three concrete jobs:

```text
DEPENDENCY_VALIDATE
or
EXTERNAL_CANDIDATE_VALIDATE
or
OPEN_MODEL_SELECTION
```

The next implementation-prep work should therefore prioritize:

1. true `OPEN_MODEL_SELECTION` slots using their already-closed design/reference criteria;
2. actual Azari spatial closure in parallel;
3. acquisition/conversion/hash work for already-selected external candidates;
4. runtime visual/hitbox acceptance for dependency actors;
5. final stale-document cleanup after accepted bindings propagate.

Brand/title exploration is not required to block these jobs.

---

# 9. Verification state

```text
DESIGN/CANON REVIEWED: YES
EXTERNAL SOURCE REVIEWED: YES for classifications already supported by project evidence
LICENSE/PROVENANCE REVIEWED: PARTIAL — exact acquisition artifacts/hashes remain incomplete
CODE REVIEWED: N/A
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

Docs-only classification work does not justify a build/CI run.
