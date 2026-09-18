# Open-World RPG — Production Asset Binding Matrix

> Status: **ACTIVE CANON — production-facing visual/source binding triage before source bootstrap**  
> Date: 2026-09-18  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Provenance registry: `EXTERNAL_SOURCES.md`  
> R01 detailed intake: `R01_ASSET_INTAKE.md`  
> R05–R08 boss-selection contract: `BOSS_REFERENCE_DESIGN_PASS_R05_R08_2026-09-17.md`  
> R05/R06 targeted model intake: `R05_R06_GUARDIAN_MODEL_INTAKE_PASS1_2026-09-17.md`  
> R07/R08 targeted model intake: `R07_R08_GUARDIAN_MODEL_INTAKE_PASS1_2026-09-17.md`  
> R09–R12 targeted major-model intake: `R09_R12_MAJOR_MODEL_INTAKE_PASS1_2026-09-18.md`  
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

The R07/R08 intake attempted to inspect the exact current Fabric 26.2 JAR, but the current inspection environment could resolve the distribution/version and not retrieve the Java archive bytes. Therefore no claim is made that current Armor of Desert / Moon Priest model JSON, animation files or hitboxes were directly inspected. That work remains `DEPENDENCY_VALIDATE`, not accepted runtime proof.

---

# 4. Region-by-region major actor matrix

| Region | Player-facing slot | Current external direction | Status | What actually remains |
|---|---|---|---|---|
| R01 | Earthloong dungeon boss | Threateningly Mobs Continued — exact Fabric 26.2 target `1.1.1+fabric.26.2`, Version ID `Bdd8lkUM` | `DEPENDENCY_VALIDATE` | artifact identity is pinned; materialize current JAR, inspect model/animation/hit volume, then align project attacks/VFX |
| R01 | Regalhart / major field identity | Threateningly lineage/dependency direction; same pinned Fabric 26.2 artifact `Bdd8lkUM` | `DEPENDENCY_VALIDATE` | materialize current JAR, inspect current presentation/animation/hitbox, then accept/reject integration |
| R01 | Trail Stag | Quaternius animated Stag source direction | `EXTERNAL_CANDIDATE_VALIDATE` | exact artifact/license-at-acquisition, conversion, mount/dismount rider motion, Minecraft review |
| R01 | player outfits / weapons / tools / food / settlement / potions | Quaternius/KayKit/Kenney families pinned in `R01_ASSET_INTAKE.md`; selected KayKit starter/equipment + Restaurant rows now exact-byte/SHA/geometry audited in Phase C | `ASSET_INTAKE_ACTIVE` | **do not rediscover audited KayKit filenames**; remaining work is visual conversion/pivot/Minecraft acceptance plus unresolved apparel/potion/settlement/variant artifacts |
| R01 | four Heartland catchable fish identities + Fish Codex icons | four-role direct-review set: Small Fish Common Minnow / CDmir Fish / Quaternius Armored Catfish / CDmir Esox; gfroad Catfish + joyfulsquirrel Fish alternates only | `ASSET_INTAKE_ACTIVE` | Phase D pins exact CDmir direct ZIP URLs and passes Armored Catfish's first visible role/silhouette review; next is Common Minnow exact package + binary/hash/3D/Minecraft comparison, then species names/icons |
| R02 | Grovebound Warden | Quaternius `Goleling Evolved` direction | `EXTERNAL_CANDIDATE_VALIDATE` | exact source artifact, clips, scale/silhouette, final binding |
| R02 | Edras, Last Curator | Threateningly Lich dependency direction | `DEPENDENCY_VALIDATE` | current 26.2 presentation, reinforcement animation/readability |
| R03 | Whitecrest Griffin | VitSh `Griffin Animated` | `EXTERNAL_CANDIDATE_VALIDATE` | downloadable artifact, attribution/hash, flight/landing clips, weak-point viability |
| R03 | Forgewake Colossus | Dm3d `Rock Golem` OpenGameArt candidate | `EXTERNAL_CANDIDATE_VALIDATE` | exact license path, Blender conversion, fracture presentation, animation quality |
| R04 | Ferox Iceworm | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | burrow/surface animation, hit volume, camera/readability |
| R04 | Icebroodmother + Ice Weaver | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | boss/minion current-model quality, add readability, VFX/hitbox |
| R05 | mature Earthloong field boss | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | current 26.2 quality + distinct mature encounter presentation |
| R05 | root-vault / regulator dungeon guardian | ancient regulator construct with readable core + articulated control components | `OPEN_MODEL_SELECTION` | Pass 1 leaves only `Pok` and OnlyPro `Forest Golem` worth direct 3D rejection/acceptance review; neither is accepted; see R05/R06 intake |
| R06 | Hydra field boss | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | multi-head hit/readability, water combat behavior, authored basin integration |
| R06 | Sunken Observatory dungeon guardian | hydromechanical/ceremonial flow guardian with directional components | `OPEN_MODEL_SELECTION` | Pass 1 rejected water-elemental/generic caster/mecha shortcuts; no exact candidate survives; search by directional hydromechanical anatomy |
| R07 | Ferox Deathworm field boss | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | sand/burrow presentation, melee access, hitbox |
| R07 | buried fortress/cistern dungeon guardian | armored infrastructure sentinel; Armor of Desert only if it passes direct review | `OPEN_MODEL_SELECTION` | Armor of Desert remains `DEPENDENCY_VALIDATE` as a candidate only; current 26.2 binary/model evidence still required; screened generic desert/golem alternatives rejected |
| R08 | Titan Rabbit ritual boss | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | current model/animation/hitbox, stomp/leap honesty, signature material visual |
| R08 | Moonpriest / Knowledge Fairy authored identities | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | current presentation, density/readability, no donor progression leakage; Moonpriest remains normal/elite caster role only |
| R08 | Glass-Root Archive final guardian | magical archive/regulator guardian with visible movable lens/ring/root/crystal components | `OPEN_MODEL_SELECTION` | Pass 1 rejects Moonpriest as final boss and generic crystal golem/automaton shortcuts; no exact final candidate survives |
| R09 | Caravan Elephant | Alex's Mobs Continued | `DEPENDENCY_VALIDATE` | current 26.2 riding/charge/body-width behavior and project registration integration |
| R09 | Executioner dungeon boss | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` with replace-if-fail rule | current animation/reach/poise quality; replace if boss bar exceeds presentation quality |
| R09 | optional Lv49 dryland field boss | direct-review shortlist: Fab `Brimstone Behemoth` / ArtStation `Fantasy Creature - Combat Rhino` | `OPEN_MODEL_SELECTION` | compare both in 3D/animation first; accept only if silhouette is more than a scaled common animal and Southstone art direction survives |
| R10 | Basalt Wyvern | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | flight uptime/melee access, dive tell, camera/hitbox |
| R10 | Scorch Golem | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | current model/attack quality and forge-space readability |
| R10 | Inferno / final forge guardian | Threateningly Inferno is candidate, not automatically accepted | `OPEN_MODEL_SELECTION` | **current 26.2 Inferno runtime review first**; reopen replacement search only if model/animation/melee-readability/cascade presentation fails |
| R11 | Jungle/sea transition mount: Laviathan | Alex's Mobs Continued | `DEPENDENCY_VALIDATE` | current multipart stability, seats/controller, project registration and R11 handling course |
| R11 | Riptooth | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | fair surface/open-water uptime, charge/breach readability |
| R11 | Abyss Fang | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | giant multipart/hitbox/camera, Charge/Devour fairness, 3D arena telegraphs |
| R11 | Giant Squid / Cachalot and other large aquatic actors | Alex's Mobs Continued | `DEPENDENCY_VALIDATE` | current 26.2 multipart regression/smoke test before regional admission |
| R11 | Sea-Fort / Freebooter Keep final boss | strong direct-review prospect: CGTrader `Pirate Captain Gameready with animations` (22 animations, sword+pistol, paid/local-only intake) | `OPEN_MODEL_SELECTION` | preview/acquire only if paid local-only candidate is acceptable; verify grounded fort-commander tone, melee reach, projectile tells and Minecraft retarget quality |
| R12 | Terradragon optional world boss | Threateningly Mobs Continued | `DEPENDENCY_VALIDATE` | current ultra-boss model/animation/hitbox/airtime and project-authored no-grief behavior |
| R12 | Sky Drake | Quaternius animated Dragon direction | `EXTERNAL_CANDIDATE_VALIDATE` | exact artifact/license evidence, conversion, flight/mount animation review |
| R12 | final systemic guardian | generic arcane/crystal/steampunk/humanoid guardian candidates rejected; no surviving Pass-1 direct-review model | `OPEN_MODEL_SELECTION` | search only segmented/reconfigurable/multi-part topology able to express Network Link / Cascade / Partition / Reconfiguration physically |
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

R01 fish are also **not a broad-discovery problem anymore**: `R01_ASSET_INTAKE.md` §3.4 now owns a source-specific CC0 direct-review shortlist. The next action is binary acquisition + 3D comparison, not another generic fish search.

---

# 6. True major model-selection queue

The current large visible search queue is intentionally narrow:

1. R05 root-vault dungeon guardian — design contract closed; Pass 1 has two direct-review prospects but no accepted model;
2. R06 Sunken Observatory dungeon guardian — design contract closed; Pass 1 found no surviving exact candidate;
3. R07 buried fortress/cistern dungeon guardian — design contract closed; Armor of Desert is candidate-only pending current dependency inspection;
4. R08 Glass-Root Archive final guardian — design contract closed; Moonpriest/generic crystal shortcuts rejected and no accepted model exists;
5. R09 optional dryland field boss — direct-review shortlist now exists; compare `Brimstone Behemoth` vs `Fantasy Creature - Combat Rhino` before new search;
6. R10 Inferno accept/replace decision — inspect current 26.2 Inferno first, no broad replacement search yet;
7. R11 Sea-Fort boss — animated Pirate Captain is current strong paid/local-only prospect pending preview/acquisition decision;
8. R12 final systemic guardian — no accepted prospect; continue only segmented/reconfigurable topology search;
9. R12 low-density anomaly roster only where current dependency candidates fail.

R02/R03 already have concrete external candidates and therefore belong to acquisition/conversion review, not broad discovery.

For R05–R08, do not restart generic boss ideation or repeat the rejected broad material/biome searches. Apply, in order:

1. `BOSS_REFERENCE_DESIGN_PASS_R05_R08_2026-09-17.md`;
2. `R05_R06_GUARDIAN_MODEL_INTAKE_PASS1_2026-09-17.md`;
3. `R07_R08_GUARDIAN_MODEL_INTAKE_PASS1_2026-09-17.md`.

The first intake pass established several reusable rejection rules:

- `water creature` is not automatically a flow-control guardian;
- `desert enemy` is not automatically a pressure sentinel;
- `crystal creature` is not automatically a magical regulator;
- high animation count does not compensate for the wrong body topology;
- dependency presence does not prove boss-quality presentation;
- material/color fit does not replace a real open/reconfiguration/phase state.

---

# 7. Cross-cutting presentation binding

## UI

Canon visual family remains:

- Foozle `Lucifer - RPG UI` primary;
- `Lucifer - Equipment` for inventory/equipment continuation;
- Kenney `Fantasy UI Borders` / `UI Pack - Adventure` only as support primitives.

Status: `BOUND_DIRECTION`.

Exact official acquisition targets pinned on 2026-09-18:

```text
Lucifer RPG UI: Foozle_UI_0002_Lucifer_RPG_UI_Pixel_Art.zip — 29 MB — CC0
Lucifer Equipment: Foozle_2DS0005_Lucifer_Equipment_Pixel_Art.zip — 81 kB — CC0
```

Raw ZIP bytes/SHA-256 and real Minecraft GUI-scale screenshot acceptance remain open; UI-family selection does not.

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

1. actual Azari spatial closure immediately if the world archive becomes available;
2. direct artifact/3D rejection-or-acceptance review for the surviving R05 prospects;
3. current-version binary/model/animation/hitbox acceptance for dependency candidates such as Armor of Desert and **R10 Inferno** where the exact actor matters;
4. direct-review the new R09 and R11 shortlisted candidates before any broader search;
5. shape/function-based R06/R07/R08 and R12 search without repeating material/biome-only candidates already rejected;
6. acquisition/conversion/hash work for already-selected external candidates;
7. final stale-document cleanup after accepted bindings propagate.

Brand/title exploration is not required to block these jobs.

---


## R01 Phase E exact-file delta — 2026-09-18

The R01 rows below have stronger **technical exact-file evidence** but are not production-accepted yet:

| R01 binding | Phase E evidence | Binding state after Phase E |
|---|---|---|
| River Scholar Garb | Wizard male/female body glTF parsed; 65-joint skin; 2.9k–5.3k tris body range; Wizard BaseColor/Normal/ORM family | `EXACT_CANDIDATE_TECHNICALLY_CORROBORATED`; creator archive + visual/Minecraft acceptance pending |
| Wayfarer Leathers | Ranger male/female body + pauldron glTF parsed; shared 65-joint skin; coherent Ranger texture family | `EXACT_CANDIDATE_TECHNICALLY_CORROBORATED`; visual/Minecraft acceptance pending |
| Ironbound Guard | Knight male/female body armor + round pauldrons parsed; shared 65-joint skin; coherent Knight texture family | `EXACT_CANDIDATE_TECHNICALLY_CORROBORATED`; visual/Minecraft acceptance pending |
| Potion family | `Potion_1..4` all parsed as distinct geometry payloads, about 500–800 tris each | `EXACT_CANDIDATE_TECHNICALLY_CORROBORATED`; Healing/Focus/Cleansing assignment still blocked on direct visual + hand-pivot review |
| potion drink | historical 2025 UAL1 Standard parsed: 46 clips and no `Drink`; newer pinned integration maps exact `Drink` | `EXACT_CLIP_NAME_CORROBORATED`; current creator archive/timing/retarget/Minecraft acceptance pending |
| meal eat | newer pinned integration maps exact `Consume` and explicitly treats consume as UAL2-resolved | `EXACT_CLIP_NAME_CORROBORATED`; current creator archive/timing/retarget/Minecraft acceptance pending |
| revive/help-up | no stronger accepted source in Phase E | `NEEDS_EXTERNAL_CLIP` |
| Trail Stag mount/dismount | no stronger accepted source in Phase E | `NEEDS_EXTERNAL_CLIP` |
| four-role fish queue | binary acquisition remained blocked; no broad search reopened | unchanged; final model roster still not accepted |

Evidence document: `R01_ASSET_PHASE_E_APPAREL_POTION_MOTION_EXACT_REVIEW_2026-09-18.md`.

The downstream Git repositories used here are **corroboration snapshots**, not automatic raw-asset admission sources. Creator-controlled acquisition + project-local SHA-256 remain mandatory before production admission.

# 9. Verification state

```text
DESIGN/CANON REVIEWED: YES
EXTERNAL SOURCE REVIEWED: YES for existing classifications/targeted R05–R08 screening plus R01 Phase-E apparel/potion/motion exact-file corroboration
LICENSE/PROVENANCE REVIEWED: PARTIAL — exact acquisition artifacts/hashes remain incomplete; Threateningly continuation storefront conflict remains unresolved
CODE REVIEWED: N/A
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

Docs-only classification/research work does not justify a build/CI run.
