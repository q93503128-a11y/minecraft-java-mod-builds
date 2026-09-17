# Open-World RPG — R07 / R08 Guardian Exact-Model Intake Pass 1

> Date: **2026-09-17**  
> Status: **TARGETED INTAKE COMPLETE / EXACT MODELS STILL OPEN**  
> Baseline immediately before write: `main` = `f172e2aa91c498ebbb180c320ef9b0d3bb1b5472`  
> Boss contract: `BOSS_REFERENCE_DESIGN_PASS_R05_R08_2026-09-17.md`  
> Binding matrix: `PRODUCTION_ASSET_BINDING_MATRIX.md`  
> R07 canon: `R07_IMPLEMENTATION_PACKAGE.md`  
> R08 canon: `R08_IMPLEMENTATION_PACKAGE.md`

This is an intake/rejection pass, **not** a new boss-design pass.

No exact boss model is accepted merely because it is desert-themed, crystal-themed, already present in a dependency, cheap, free, rigged or heavily animated. The selected actor must visibly support the already-closed encounter contract.

---

# 1. Intake vocabulary

- `REJECT_FOR_SLOT` — enough public evidence exists to show that the model/design conflicts with the closed boss contract.
- `DEPENDENCY_VALIDATE` — the actor already exists in a planned dependency, but current 26.2 in-game model/animation/hitbox quality still must be directly inspected before promotion.
- `NEEDS_DIRECT_3D_REVIEW` — public evidence justifies obtaining the legitimate artifact and inspecting it in Blender/Minecraft scale, but does not justify acceptance.
- `REFERENCE_ONLY` — useful production lesson, anatomy precedent or animation coverage reference; not the exact boss actor.
- `LICENSE_CONFLICT / CLARIFICATION REQUIRED` — source/storefront metadata is not consistent enough to authorize copying donor bytes.
- `ACCEPTED` — not used in this pass.

No overall numeric winner score is used.

---

# 2. Threateningly Mobs Continued — current dependency reality

R07 and R08 both had dependency candidates in their implementation packages, so the dependency was checked before doing another broad model search.

Current public distribution evidence:

- CurseForge project:
  `https://www.curseforge.com/minecraft/mc-mods/threateninglly-mobs-continued`
- CurseForge 26.2 Fabric fixed file:
  `https://www.curseforge.com/minecraft/mc-mods/threateninglly-mobs-continued/files/8804647`
- Modrinth project:
  `https://modrinth.com/mod/threateningly-mobs-continued`
- Modrinth Fabric 26.2 version:
  `https://modrinth.com/mod/threateningly-mobs-continued/version/1.1.1%2Bfabric.26.2`
- original project:
  `https://modrinth.com/mod/threateningly-mobs`

Verified public distribution facts at this pass:

```text
CURRENT 26.2 FABRIC DISTRIBUTION: YES
Modrinth version: 1.1.1+fabric.26.2
Modrinth version id: Bdd8lkUM
Modrinth project id: JXjyo7k6
CurseForge fixed Fabric filename: threateningly_mobs-1.1.1+fabric.26.2(fixed).jar
CurseForge fixed file id: 8804647
```

## 2.1 License metadata conflict

The continuation currently has conflicting public license labels:

- Modrinth labels the continuation **MIT** and its project description says the original mod and the port use MIT;
- CurseForge currently labels the continuation **All Rights Reserved**;
- the original Threateningly Mobs project is publicly labeled MIT on Modrinth/CurseForge.

Therefore:

```text
DEPENDENCY USE: may be evaluated as a dependency candidate
COPYING CONTINUATION SOURCE/ASSET BYTES INTO THIS PUBLIC REPO: NOT AUTHORIZED BY THIS PASS
LICENSE STATUS FOR RAW CONTINUATION BYTES: CONFLICT / CLARIFICATION REQUIRED
```

Do not flatten this into `Threateningly Continued = MIT` or `Threateningly Continued = ARR` until the maintainer/source-of-truth license is reconciled.

## 2.2 Actual 26.2 JAR inspection attempt

The exact current Modrinth download URL was resolved from the version page. This environment's web layer could resolve the Java archive URL but could not expose/download the binary into the inspection container because `application/java-archive` retrieval is blocked here.

That means this pass **did not** inspect current 26.2 model JSON, textures, animation clips, class behavior or hitbox resources directly.

This limitation is intentional in the acceptance result below. No candidate is promoted from dependency metadata alone.

Required next local/direct inspection when the JAR is available:

```text
SHA-256 of exact Fabric 26.2 artifact
Armor of Desert model/texture paths
Armor of Desert skeleton/model part structure
Armor of Desert animation/state names
Armor of Desert attack/recovery coverage
Armor of Desert actual bounding/hitbox behavior
Moon Priest model/texture paths
Moon Priest animation/state names
Moon Priest attack telegraph coverage
Minecraft normal-FOV camera occupancy
2-player body overlap / target-switch readability
```

---

# 3. R07 Cistern Guardian — closed contract reminder

The exact actor must read as an **armored infrastructure sentinel**, not merely `a hard desert enemy`.

Required visible body language:

- heavy armor or shell with readable layered structure;
- broad pressure seam / vent / release state or another equally clear infrastructure mechanism;
- large committed impact/release motion;
- a wide, front/side-readable vulnerability after that commitment;
- body/armor state must explain the gameplay state;
- must remain distinct from R03's rock-golem identity and R07's own Ferox Deathworm field boss.

Reject:

- whole-body immunity plus tiny rear weak point;
- generic mummy/warrior;
- generic rock golem with a desert recolor;
- another worm/beetle/insect merely because it belongs in sand;
- VFX-only `venting` painted onto a body with no matching anatomy.

---

# 4. R07 dependency candidate — Armor of Desert

Sources:

- original project distribution:
  `https://www.curseforge.com/minecraft/mc-mods/threateningly-mobs`
- original 1.0.9.6 file notes:
  `https://www.curseforge.com/minecraft/mc-mods/threateningly-mobs/files/6653723`
- current continuation:
  `https://www.curseforge.com/minecraft/mc-mods/threateninglly-mobs-continued`
- independent Japanese reference inventory used only as secondary evidence:
  `https://www.mine-blog.tech/threateningly-mobs/`

Publicly supported facts:

- it is a heavy/high-tier desert/badlands enemy;
- source descriptions associate it with high defense / `guardian` identity;
- the original `1.0.9.6` changelog explicitly says `armor of desert has remake`;
- it remains present in the current continuation's 26.2 roster.

Disposition:

```text
R07 EXACT DUNGEON BOSS: DEPENDENCY_VALIDATE
PROMOTED TO EXACT MODEL: NO
RAW CONTINUATION ASSET COPYING: NO — LICENSE CONFLICT + ARTIFACT NOT INSPECTED
```

Why it remains alive:

- unlike a random external desert monster, it already belongs to the dependency ecology and to the correct regional material family;
- the remake note means stale screenshots of an older form should not be trusted as current truth;
- its stated defensive identity is directionally compatible with an armored sentinel.

Why it is **not** accepted:

- `high defense` is a stat/role claim, not proof of readable armor-state gameplay;
- no public evidence inspected in this pass proves broad pressure seams, vents, armor opening, or a wide vulnerability state;
- no current 26.2 animation list/hitbox evidence was available through the blocked JAR;
- being a normal overworld heavy enemy risks making the authored dungeon climax feel like a dependency mob promoted by HP multiplication.

Acceptance requires direct 26.2 evidence that the **body itself** can support the cistern contract. If the model is simply a durable creature with attack animations, keep it as an elite/miniboss and choose another dungeon guardian.

---

# 5. R07 external candidates screened

## 5.1 `Ancient Undead Egyptian Warrior Rigged Animated` — Sketchfab

Source:

`https://sketchfab.com/3d-models/ancient-undead-egyptian-warrior-rigged-animated-250d1e5c12ae413884813bb25860ef9d`

Public evidence:

- downloadable;
- CC Attribution;
- about 10k triangles;
- presented as rigged/animated;
- desert/ancient visual family.

Disposition:

```text
R07: REJECT_FOR_SLOT
```

Reason:

This is an undead humanoid warrior. Its anatomy communicates weapons/armor/character combat, not cistern pressure hardware or a mechanical infrastructure state. Accepting it would make the R07 mechanic something the project paints on top of a mummy rather than something the actor naturally explains.

## 5.2 `Armored Golem` — CGTrader / daelon

Source:

`https://www.cgtrader.com/3d-models/character/sci-fi-character/armored-golem`

Public evidence:

- ~1.6k polygons;
- rigged/animated;
- 12 animations including three attacks, hit reactions, jump and death;
- multiple texture variants;
- marketplace listing currently marked Editorial License / no AI.

Disposition:

```text
R07: REJECT_FOR_SLOT
```

Reason:

Animation coverage is usable but the identity is a generic/sci-fi armored golem. No public evidence shows a pressure system, vent state or water/cistern infrastructure anatomy, and the editorial license direction is also a poor production fit. It does not justify acquisition for this role.

## 5.3 `Stone golem low-poly game engine ready 3D model` — CGTrader / san3dart

Source:

`https://www.cgtrader.com/3d-models/character/fantasy-character/stone-golem-low-poly-game-engine-ready-3d-model`

Public evidence:

- Royalty Free License, no-AI listing;
- 23,296 polygons / 13,190 vertices;
- rigged and animated;
- 30 clips including four attacks, stomp, thrown sequence, cast sequence, stun, movement and death;
- FBX plus engine packages.

Disposition:

```text
R07: REJECT_FOR_SLOT
R03/GENERAL COMBAT-ANIMATION REFERENCE: RETAIN ONLY IF NEEDED
```

Reason:

This is technically stronger than many golem listings, but the published identity remains a generic stone golem. R07 needs the body to expose **pressure/armor infrastructure state**, and R03 already occupies the stronger conventional rock-golem silhouette space. Thirty generic combat clips cannot solve the wrong anatomy.

## 5.4 `Goliath Sentinel | RPG Enemy` — Sketchfab

Source:

`https://sketchfab.com/3d-models/goliath-sentinel-rpg-enemy-616ee51c8eea40caa1b82c151ece99b2`

Public evidence:

- downloadable CC Attribution;
- stone/steel armored guardian concept;
- 581.9k triangles / 290.9k vertices;
- description says `rig-ready`; tags also mention animated/rigged, but this pass did not obtain artifact proof of a usable clip set.

Disposition:

```text
R07: REJECT_FOR_SLOT
```

Reason:

The source is dramatically over-dense for a Minecraft boss conversion starting point, and the public page does not establish an authored pressure/release animation family. Retopology plus animation work would become a major redesign project rather than external-first exact-model intake.

## 5.5 `Ancient Scorpionaur Golem construct` — Cults

Source:

`https://cults3d.com/en/3d-model/game/ancient-scorpionaur-golem-construct-rigged-animated-game-ready-pbr`

Public evidence:

- recent desert-empire construct/scorpion hybrid presentation;
- seller claims rigged/animated/game-ready PBR direction;
- bronze armor + chitin/construct language.

Disposition:

```text
R07: REJECT_FOR_SLOT
```

Reason:

The unusual silhouette is attractive, but it moves R07 back toward `desert creature boss` and competes with the region's beetle/deathworm arthropod identity. It does not visibly solve the pressure-seam / armored infrastructure contract strongly enough to justify introducing another creature-hybrid boss family.

---

# 6. R07 Pass-1 closure

```text
Armor of Desert: DEPENDENCY_VALIDATE
Other screened exact candidates: REJECTED
EXACT R07 MODEL: STILL OPEN
```

Next discovery must stop using broad queries such as `desert boss`, `sand golem`, or `mummy guardian` as the primary search.

Use shape/function terms instead:

```text
animated fantasy pressure sentinel
armored cistern automaton
ancient hydraulic guardian
armored construct opening chest / vent state
fantasy boiler / pressure golem without sci-fi gun language
stone-metal infrastructure sentinel with articulated armor plates
```

A correct candidate can be desert-neutral if the cistern/dungeon art direction carries the regional context. **Mechanical readability matters more than sand-colored material.**

---

# 7. R08 Glass-Root Guardian — closed contract reminder

The exact actor must read as a **magical archive/regulator guardian**.

Required body support includes at least one meaningful family of actual moving structure such as:

- lens;
- ring;
- crystal assembly;
- root-like regulator limb;
- casting appendage whose orientation changes the field;
- another explicit mechanical-magical topology that can visibly redirect a zone.

The phase transition must visibly change the actor/field geometry. A humanoid mage that simply casts more projectiles at 50% HP is not acceptable.

---

# 8. R08 dependency candidate — Moon Priest / Moonpriest

Sources:

- original 1.0.9 version:
  `https://modrinth.com/mod/threateningly-mobs/version/K8PaW2nO`
- original/current project:
  `https://modrinth.com/mod/threateningly-mobs`
- current continuation:
  `https://modrinth.com/mod/threateningly-mobs-continued`

Publicly supported facts:

- Moon Priest was introduced in original version 1.0.9;
- the documented combat identity is curse usage + an explosive moon projectile;
- secondary inventory/reference material categorizes it below the 200+ heavy class in ordinary use;
- it remains part of the broader Threateningly roster/lineage.

Disposition:

```text
R08 NORMAL/ELITE AUTHORED CASTER ROLE: RETAIN
R08 FINAL GLASS-ROOT GUARDIAN: REJECT_FOR_SLOT
```

Reason:

Moon Priest is useful precisely because it already has a recognizable caster role. That same role makes it a poor final guardian: the public attack identity is spell/projectile centric, while R08 requires a body-owned field-redirection mechanism and phase geometry. Promoting the normal caster into a boss by increasing health/projectile count would violate both the R08 contract and the project's anti-placeholder rule.

The implementation package's ordinary Moonpriest ruin/archive pressure can remain. Do not clone it into the dungeon climax.

---

# 9. R08 external candidates screened

## 9.1 `Crystal Warden — Elemental 3D Character for Games Animations` — CGTrader / aligol3dart

Source:

`https://www.cgtrader.com/3d-models/character/fantasy-character/crystal-warden-elemental-3d-character-for-games-animations`

Public evidence:

- Royalty Free License;
- CGTrader technical verification on FBX;
- rigged and animated;
- 9,999 polygons / 9,997 vertices;
- FBX/Blend/glTF and other formats;
- translucent/crystal elemental presentation.

Disposition:

```text
R08: REJECT_FOR_SLOT
```

Reason:

The model solves `magical crystal creature`, not `archive/regulator`. No public evidence establishes a rotating lens/ring assembly, field-facing appendage, opened regulator state or phase reconfiguration. Crystal material alone is insufficient, just as water material was insufficient for R06.

## 9.2 `Crystal golem low-poly game engine ready 3D model` — CGTrader

Source:

`https://www.cgtrader.com/3d-models/character/fantasy-character/crystal-golem-low-poly-game-engine-ready-3d-model`

Public evidence:

- rigged/animated;
- 40,431 polygons / 21,680 vertices;
- 30 animations including attack, stomp, throw, cast, stun, locomotion and death;
- game-engine delivery direction.

Disposition:

```text
R08: REJECT_FOR_SLOT
```

Reason:

This is a generic animated crystal golem with broad combat coverage. The contract needs body geometry that **explains field redirection**, not just a creature able to play cast animations. It also risks making R08 another elemental golem after earlier regions.

## 9.3 `Arcane Guardian` — Sketchfab / bertanorgun

Source:

`https://sketchfab.com/3d-models/arcane-guardian-9607eb31bc9244eabff495971392a60b`

Public evidence:

- about 36.6k triangles;
- listed as an armored sentinel / heavy enemy;
- page says it includes many animations;
- downloadable under Sketchfab `Free Standard` terms rather than an open CC attribution license.

Disposition:

```text
R08: REJECT_FOR_SLOT
```

Reason:

The public actor evidence is armored-heavy-enemy first. No lens/ring/root/crystal regulator topology is established, and its download license is less suitable for a public mod repository than clean CC0/CC-BY material. There is not enough encounter fit to justify deeper acquisition work.

## 9.4 `Guardian Automaton` — Sketchfab / Mama

Source:

`https://sketchfab.com/3d-models/guardian-automaton-78e6db1b7ece44d68d8aeaa0683fc0a7`

Public evidence:

- downloadable CC Attribution;
- bronze/mechanical guardian identity;
- approximately 481.4k triangles / 240.7k vertices;
- no useful public animation/rig evidence captured by this pass.

Disposition:

```text
R08: REJECT_FOR_SLOT
```

Reason:

It is far too dense for a sensible Minecraft boss-conversion starting point and does not prove the required magical field topology or authored animation states.

## 9.5 `Magic Crystal Animated` / animated ring-crystal props

Representative sources:

- `https://sketchfab.com/3d-models/magic-crystal-animated-092e9eb2f5f448ba93727fb1cdf3a085`
- `https://www.cgtrader.com/3d-models/space/other/magic-crystal-animated`
- `https://sketchfab.com/3d-models/stargate-lik-ish-ring-portal-b9dac9c49f46455294eb47eca8015052`

Public evidence:

- actual animated crystal/ring props exist;
- some are modest polygon counts and CC Attribution; marketplace versions use their marketplace terms;
- these prove that the R08 regulator language does not need to be represented by particles alone.

Disposition:

```text
R08 EXACT ACTOR: REJECT / INSUFFICIENT
R08 MOVING-REGULATOR COMPONENT REFERENCE: RETAIN
```

Reason:

A ring/crystal prop is not a complete boss actor. However, these are useful evidence for what the **selected actor itself must visibly contain or integrate**: a real moving assembly that changes field direction.

Do not take a generic humanoid caster and bolt on an arbitrary spinning VFX ring after the fact. A composite solution is only acceptable if the selected external actor and external mechanism form one coherent rigged boss with believable attachment, animation ownership, hit volumes and licensing.

---

# 10. R08 Pass-1 closure

```text
Moon Priest as final boss: REJECTED; retain only normal/elite caster role
Generic crystal golem/warden: REJECTED
Generic automaton: REJECTED
Animated ring/crystal assets: COMPONENT REFERENCE ONLY
EXACT R08 MODEL: STILL OPEN
```

Next search shape:

```text
animated arcane regulator guardian
fantasy observatory sentinel rotating rings
magical archive automaton articulated lens
animated crystal mechanism guardian
construct with rotating orbit / lens / core assemblies
fantasy boss modular floating parts with authored animations
```

The search should prioritize **part topology and animation ownership** before color/material style.

---

# 11. Cross-pass conclusions for R05–R08

After the first targeted intake passes:

```text
R05 — two direct-review prospects survive: Pok / Forest Golem
R06 — no exact candidate survives
R07 — Armor of Desert remains DEPENDENCY_VALIDATE only; no exact external candidate survives
R08 — no exact final-boss candidate survives; Moon Priest remains an elite caster, not the climax
```

This is a better pre-code state than four arbitrary selected models. It removes several tempting but structurally wrong shortcuts:

- water creature != flow-control guardian;
- desert monster != pressure sentinel;
- crystal creature != magical regulator;
- high animation count != correct boss anatomy;
- dependency presence != automatic boss-quality approval;
- strong material theme != phase/reconfiguration support.

No temporary boss entity should be written for these open slots.

---

# 12. Next production action

Priority remains unchanged:

1. if actual Azari world bytes become available, suspend model research and perform real spatial calibration/closure;
2. otherwise perform legitimate direct artifact/3D inspection of the surviving R05 prospects;
3. obtain the exact current Threateningly 26.2 Fabric JAR in a local environment that permits binary inspection and validate Armor of Desert/Moon Priest model + animation + hitbox reality;
4. continue R06/R07/R08 search using **mechanical anatomy terms**, not biome/material keywords;
5. only when an exact model passes, write its anatomy-specific final encounter sheet and binding/provenance row;
6. after R05–R08 close, continue R09–R12 open-model selection.

---

# 13. Verification state

```text
DESIGN REVIEWED: YES — existing R07/R08 contracts applied, not redesigned
EXTERNAL SOURCE REVIEWED: YES — dependency/storefront/open-model targeted pass
LICENSE/PROVENANCE REVIEWED: PARTIAL — public license metadata compared; continuation conflict recorded; no final acquired artifact/hash
CODE REVIEWED: N/A
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

Docs/research-only change. Build/CI is intentionally not run.
