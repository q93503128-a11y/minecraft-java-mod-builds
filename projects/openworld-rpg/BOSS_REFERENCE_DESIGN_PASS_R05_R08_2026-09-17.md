# Open-World RPG — R05–R08 Boss Reference Design Pass

> Date: 2026-09-17  
> Status: **DESIGN/REFERENCE CONTRACT CLOSED — EXACT MODELS STILL OPEN**  
> Project contract: `PROJECT.md`  
> Binding matrix: `PRODUCTION_ASSET_BINDING_MATRIX.md`  
> Combat canon: `COMBAT_BALANCE.md`  
> Region canon: R05–R08 implementation packages + content bibles  
> Rule: this pass narrows external model selection and encounter presentation. It does not authorize placeholder actors, copied boss mechanics, or anatomy-specific final attacks before an exact model is accepted.

---

# 1. Why this pass exists

R05–R08 each already has a locked regional story/dungeon role, but the final dungeon guardian visual remains `OPEN_MODEL_SELECTION`.

The project must not solve that by:

- inventing a generic golem/caster in code;
- scaling a normal regional mob and adding a boss bar;
- shipping particle-only identity while waiting for art;
- choosing a model first and forcing unrelated mechanics onto it;
- copying a memorable boss from another mod because its fight already works.

Instead this pass studies successful Minecraft boss structures and extracts reusable **design principles**, then turns them into exact acceptance contracts for the four open guardian slots.

The result is:

```text
regional gameplay thesis
→ boss hook / arena relationship
→ required visible body language
→ model-search acceptance criteria
→ exact external model intake
→ anatomy-specific final encounter sheet
```

Final player-facing names, weak points, signature materials and anatomy-specific moves still wait for the accepted model.

---

# 2. External Minecraft boss precedents reviewed

## 2.1 Bosses of Mass Destruction — explicit attack families, escalation and cue ownership

Sources reviewed:

- official/public repository: `barribob/bosses-of-mass-destruction`;
- repository license: LGPL-3.0;
- `VoidBlossomAttacks.kt`;
- `VoidBlossomMoveLogic.kt`;
- Void Blossom action classes and subtitle strings;
- public mod listing / encounter descriptions.

Useful observed implementation structure:

- attacks are separate explicit actions rather than one giant undifferentiated AI routine;
- the Void Blossom controller exposes distinct spike, spike-wave, spore, blade and blossom action families;
- attack selection changes with health state and target distance;
- multiplayer target switching is part of the boss logic;
- major effects also receive readable audio/subtitle cues such as ground trembling / spikes gathering / poison bubbling;
- boss encounter structure and repeatability are tied to authored world presentation rather than a random high-HP spawn.

Project adoption:

- bosses own a small readable family of attacks with explicit states/cooldowns;
- phase escalation unlocks or recombines known attack families rather than merely multiplying stats;
- target selection is multiplayer-aware;
- strong tells receive both visual/body and subtitle/non-audio support;
- encounter controller/server state remains authoritative.

Not adopted:

- exact Void Blossom moves;
- exact timings, status effects or boss geometry;
- code copied mechanically across incompatible Minecraft/API generations.

Status here: **REFERENCE / CODE-ARCHITECTURE PRECEDENT**, not a runtime dependency decision.

## 2.2 Mowzie's Mobs — boss hook, counterplay and learned vulnerability

Source reviewed:

- public Mowzie's Mobs material / design discussion;
- current public repository license file.

Useful design precedent:

- a strong boss begins with a clear hook or relationship the player can understand;
- attacks should create counter-strategy rather than only damage avoidance;
- an encounter can expose a limited resource/state that the player can influence;
- a vulnerability can be earned through a boss commitment rather than being permanently available;
- the encounter must remain enjoyable once the player already knows the trick.

The Ferrous Wroughtnaut is a useful high-level precedent for **committed action → readable punish opportunity**, but also illustrates a risk: a vulnerability that is too small, too positional or too strict can become camera/hitbox frustration rather than mastery.

Project adoption:

- every R05–R08 guardian must have a one-sentence combat relationship;
- important punish windows come from visible commitment/state change;
- weak/vulnerable regions must be generous enough to read at Minecraft camera scale;
- knowing the solution should improve mastery, not delete the fight.

Not adopted:

- rear-pixel weak-point geometry;
- near-total immunity as a default boss template;
- specific Mowzie boss moves, names, art, code or lore.

License boundary:

- the public Mowzie repository license is All Rights Reserved unless otherwise stated and contains special private/public-use conditions;
- because this repository is public, Mowzie material is **REFERENCE_ONLY** here. Do not copy code/assets or closely reproduce signature boss content.

## 2.3 L_Ender's Cataclysm — boss and arena authored as one encounter

Source reviewed:

- official/current public mod listing and structure/boss presentation.

Useful precedent:

- major bosses are supported by purpose-built structures/arenas rather than dropped into generic rooms;
- arena geometry helps establish attack readability, scale and phase identity;
- high-tier body-driven attacks can communicate direction and threat through the creature itself before effects land;
- large fights benefit from a small number of clear environmental relationships rather than random hazard spam.

Project adoption:

- the arena is part of each R05–R08 guardian's design contract;
- room topology may change the usefulness of attacks, cover, routes and interaction opportunities;
- model selection must consider the actual dungeon room it will occupy.

Not adopted:

- Cataclysm's specific bosses, attacks, assets, structures or progression;
- custom-license source/assets as a basis for this public repository.

Status: **REFERENCE_ONLY**.

## 2.4 AdventureZ — authored activation and encounter ownership

Sources reviewed:

- official GitHub repository `Globox1997/AdventureZ`;
- GPL-3.0 repository license;
- public boss/creature presentation.

Useful precedent:

- a major boss can be intentionally activated through authored world interaction instead of appearing as a random mob;
- boss ownership can be tied to a specific structure/ritual/context.

Project adoption:

- dungeon guardians activate through the dungeon's authored state, not generic natural spawning;
- restart/reconnect rules remain server-authoritative and deterministic.

Not adopted:

- grindy ritual/key economies for inline regional dungeon bosses;
- AdventureZ-specific assets/content;
- GPL source copied into the project without a deliberate licensing/integration decision.

Status: **REFERENCE / POSSIBLE CODE STUDY ONLY**, not baseline runtime.

---

# 3. Cross-boss production rules extracted from the references

These rules apply to all four R05–R08 open guardian slots.

## 3.1 One-sentence combat hook

Before accepting a model, the fight must be explainable as one gameplay relationship.

Bad:

> Big guardian with melee, ranged attack, AoE and phase two.

Good shape:

> The guardian routes danger through visible lanes, then exposes itself when it overcommits to changing those lanes.

The hook does not need to appear as tutorial text. It is a design test.

## 3.2 Body first, VFX second

Important attacks begin in the accepted model's readable posture, limb/component motion or state change.

VFX confirms:

- direction;
- radius/volume;
- timing;
- impact.

VFX must not invent an attack the body cannot plausibly communicate.

## 3.3 Arena is part of the boss

Boss selection is invalid if the actor only works in an empty flat test room.

The final model must fit:

- camera distance;
- melee uptime;
- collision width;
- route/cover/channel geometry;
- multiplayer spacing;
- authored interaction points.

## 3.4 Phase changes alter rules/geometry, not only numbers

A phase transition should visibly change at least one of:

- active limbs/components;
- open/closed armor/core state;
- lane origin;
- arena route/field geometry;
- attack-family sequencing.

Do not use `+30% speed / +30% damage` as the phase identity.

## 3.5 Solved mechanics must remain fun

Once the player understands the counterplay, execution still matters through:

- spacing;
- commitment timing;
- stamina/guard/dodge economy;
- target priorities;
- multiplayer positioning;
- changing attack combinations.

No boss becomes a scripted waiting game after its gimmick is learned.

## 3.6 Weak-point generosity

Do not accept a model whose intended vulnerability would require a tiny, camera-hostile or animation-clipping hitbox.

A vulnerability must be:

- visually exposed;
- physically reachable by relevant weapon families;
- represented by an honest server hit volume;
- available long enough for a normal human reaction + approach + attack;
- readable from more than one perfect camera angle where possible.

## 3.7 Multiplayer-aware ownership

Boss logic cannot assume one stationary target.

Required later runtime behavior:

- server-owned target selection;
- bounded target switching where appropriate;
- no attack repeatedly snapping between players during its committed tell;
- support/guard/revive/objective contribution counts under project participation rules;
- encounter state/reward claim is idempotent.

## 3.8 Multi-channel telegraphs

Major tells should have at least two readable channels among:

- body animation;
- shape-readable VFX;
- positional sound;
- subtitle/caption cue;
- arena device/state change.

Accessibility does not mean duplicating every small swing with text. It means catastrophic/novel mechanics remain understandable when one sensory channel is limited.

## 3.9 Server truth matches visible truth

Server owns:

- hit acceptance;
- hazard/lane volume;
- phase state;
- interaction success;
- vulnerable state;
- damage/poise/status results.

The visible danger and server danger must agree closely.

## 3.10 Reference principles only

The project may reuse permissively licensed code later when an exact integration is intentionally approved. This document, however, adopts **principles**, not signature attacks or recognizable boss copies.

---

# 4. R05 — Root-Vault / Regulator Guardian selection contract

Regional thesis:

- modern jungle/river ecology adapted after ancient fixed control weakened;
- the dungeon guardian belongs to the old regulator/root-vault interface;
- it must not imply that Earthloong or jungle wildlife caused the infrastructure problem.

## 4.1 Required visual direction

Search for an **ancient regulator construct visibly reclaimed by roots/stone**, not another natural beast.

Preferred visible anatomy:

- clear central regulator/core volume;
- large articulated plates, limbs, rings or control arms;
- one or more components that can visibly open/close/rotate/reconfigure;
- silhouette distinct from R03 Rock Golem and R01/R02 nature actors;
- enough body mass for a dungeon climax without filling the whole camera.

Roots may reclaim/entangle the construct, but the model must still read as old infrastructure first.

## 4.2 Combat hook

> **The guardian routes pressure through visible root/channel lanes, then creates a punish window when a control action overcommits.**

Functional fight grammar:

1. body/component selects or points toward one lane family;
2. lane telegraph becomes visible in arena/root-channel geometry;
3. player repositions/guards/dodges through the committed action;
4. major control action leaves the core/plate structure visibly open;
5. punish window rewards correct read;
6. later phase recombines lanes/components instead of merely attacking faster.

## 4.3 Weak-point rule

No tiny rear weak spot.

The accepted model should support a **broad front/side core opening or large exposed control region** after a committed action.

If the candidate only supports an invisible center hitbox or a tiny decorative gem, reject it or redesign before acceptance.

## 4.4 Phase requirement

Around the middle of the encounter, the guardian must visibly fracture, unlock or reconfigure enough that:

- lane origins change;
- one familiar attack gains a different geometry;
- the player can still recognize old tells;
- no long invulnerable cinematic pause is required.

## 4.5 Minimum animation/state coverage before acceptance

At minimum the actual source/retarget plan must convincingly cover:

- idle/active loop;
- locomotion or rooted turning/reposition if applicable;
- at least two distinct committed attack bodies;
- open/exposed or stagger/recovery state;
- visible reconfiguration/phase state;
- death/shutdown.

Final exact attacks wait for anatomy.

---

# 5. R06 — Sunken Observatory Flow Guardian selection contract

Regional thesis:

- useful local water infrastructure can be retained while dangerous remote authority is isolated;
- Hydra remains the major natural predator identity;
- the dungeon guardian must therefore read as an **infrastructure/observatory actor**, not another swamp monster.

## 5.1 Required visual direction

Search for a **hydromechanical or ceremonial flow guardian** with visibly directional components.

Useful anatomy may include:

- fins/vanes;
- broad orientable arms;
- gate-like plates;
- rotating ring/flow-control pieces;
- a central body/core whose facing clearly matters.

The model must work in shallow/flooded space without requiring 35 minutes of fully underwater combat.

## 5.2 Combat hook

> **The guardian changes current/flow lanes through moving body components; the player reads direction and uses earned openings to isolate or disable one dangerous flow path.**

Arena grammar:

- stable footing remains available;
- shallow/flow zones change positioning;
- moving components tell the player where a lane will become dangerous;
- interaction opportunities are short combat punctuation, not a lever minigame while the boss free-hits the player.

## 5.3 Phase requirement

A later phase changes the topology of active flows, for example:

- different component pair active;
- lane crossing pattern changes;
- central safe route shifts;
- old tell remains readable through the same moving anatomy.

## 5.4 Hard rejection

Reject a model if the encounter would need:

- invisible spherical water damage;
- arbitrary currents with no visible origin;
- long unreachable swimming periods;
- Hydra-like animal anatomy that duplicates the field boss identity;
- particle rings as the only directional information.

---

# 6. R07 — Buried Fortress / Cistern Guardian selection contract

Regional thesis:

- scarce-water systems expose the cost of distant optimization;
- Ferox Deathworm owns the natural burrowing apex role;
- the dungeon guardian should embody old allocation/pressure infrastructure.

`Armor of Desert` remains a candidate only if current 26.2 presentation actually passes this contract.

## 6.1 Required visual direction

Target identity:

> **armored infrastructure sentinel**

not:

> large desert animal with more HP.

Useful visible anatomy:

- thick but segmented armor;
- pressure/seam/vent elements;
- a heavy committed limb/body mechanism;
- a readable exposed state after armor/pressure movement;
- silhouette that belongs in a buried cistern/fortress system.

## 6.2 Combat hook

> **Heavy armor and pressure plates make frontal pressure inefficient until a committed release/impact action opens a clearly readable vulnerable seam.**

This uses the broad precedent of `commitment creates vulnerability`, but does **not** copy the Wroughtnaut rear-weakness pattern.

## 6.3 Damage rule

Default preference:

- boss remains meaningfully damageable outside the best window;
- the exposed window improves damage/poise opportunity rather than turning all other contact into `0`;
- hard immunity is allowed only if the final accepted model communicates it extraordinarily clearly and playtest proves it fun.

The project should prefer mastery over waiting.

## 6.4 Arena relationship

Cistern/fortress pressure lanes or valves may communicate attack preparation, but:

- no long switch puzzle during active melee pressure;
- no requirement to click three identical devices every cycle;
- arena information supports reading the boss instead of replacing fighting the boss.

## 6.5 Phase requirement

Visible armor damage/reconfiguration changes:

- reachable angles;
- attack reach/arc;
- pressure-lane origin;

rather than merely increasing movement speed.

---

# 7. R08 — Glass-Root Archive Guardian selection contract

Regional thesis:

- historical stability suppressed some valuable modern magical ecology;
- the player preserves bounded safety while rejecting over-stabilization;
- normal Moonpriest/Nature Spirit actors cannot simply be scaled into the dungeon climax.

## 7.1 Required visual direction

Search for a **magical archive/regulator guardian** with actual moving structures capable of communicating field manipulation.

Strong anatomy candidates contain some combination of:

- lenses;
- rings;
- root/crystal extensions;
- multiple casting appendages;
- articulated plates/focus structures;
- a visible central magical reservoir/core.

A generic robed mage with a larger health bar is insufficient unless the animation/model quality provides a genuinely unique regulator identity.

## 7.2 Combat hook

> **The guardian accumulates visible stabilization/archive charge into bounded fields; the player can redirect or interrupt field geometry through positioning and concise combat interactions, trading safety against boss pressure.**

This adopts the high-level `boss-owned resource/state the player can influence` principle without copying another mod's resource or puzzle.

## 7.3 Field rules

- field boundaries must be shape-readable;
- field server volume matches presentation;
- field manipulation never becomes projectile spam filling the entire screen;
- one cleanse/support opportunity is valuable but Cleric/Guardian are never mandatory;
- melee always receives a practical route to uptime.

## 7.4 Phase requirement

Lens/ring/root configuration changes field shapes while preserving learned tells.

Examples of acceptable functional change after final model intake:

- line field becomes forked line from a visibly split focus;
- circular bound shifts to two smaller visible emitters;
- one defensive lens opens a new punish angle.

These are **functional examples, not locked final attacks**.

---

# 8. Cross-model hard rejection checklist

Reject a candidate before implementation if any of the following is true and cannot be fixed through a legitimate adaptation/retarget plan:

- static mesh with only generic locomotion + `attack1`;
- vanilla-shaped reskin as the main identity;
- boss concept depends on particle clouds because the body has no useful tells;
- no visually meaningful state/phase change is possible;
- proposed server hitboxes would not correspond to visible anatomy;
- vulnerability is tiny, camera-hostile or unreachable by common melee families;
- constant flight/swimming makes normal melee uptime unreasonable;
- silhouette duplicates an important field boss in the same region;
- model only works by adding unrelated donor lore/progression;
- license/provenance cannot support the chosen `DEPENDENCY / LOCAL_ONLY / PUBLIC` handling;
- the only reason to accept it is `we already found it`.

---

# 9. Model-intake scorecard for the next search pass

Do not use an overall numeric winner score. A candidate either satisfies required gates or remains rejected/open.

Record each candidate against:

```text
source / author
license or dependency boundary
exact artifact / version
Minecraft-scale silhouette
region identity fit
camera fit
melee accessibility
animation coverage
body-language telegraph support
open/vulnerable-state support
phase/reconfiguration support
arena-geometry fit
multiplayer readability
hitbox honesty
VFX dependence level
conversion/retarget cost
public-repo safety
accept / reject / needs direct 3D review
```

Do not call a candidate accepted from a storefront thumbnail alone.

---

# 10. Reference-source handling summary

| Reference | License/status observed | Project use in this pass |
|---|---|---|
| Bosses of Mass Destruction | LGPL-3.0 repository | design + code-architecture precedent; no current-runtime assumption |
| Mowzie's Mobs | custom / All Rights Reserved unless otherwise stated | high-level design precedent only; no copied signature mechanics/assets/code |
| L_Ender's Cataclysm | custom-license/current mod reference | arena/boss co-design precedent only |
| AdventureZ | GPL-3.0 repository | authored activation/structure precedent; code study only with deliberate GPL decision |

The purpose of this table is to prevent `reference` from silently becoming `copy source/assets` later.

---

# 11. Closure result

After this pass:

```text
R05 ROOT-VAULT GUARDIAN DESIGN/REFERENCE CONTRACT: CLOSED
R06 FLOW GUARDIAN DESIGN/REFERENCE CONTRACT: CLOSED
R07 CISTERN GUARDIAN DESIGN/REFERENCE CONTRACT: CLOSED
R08 GLASS-ROOT GUARDIAN DESIGN/REFERENCE CONTRACT: CLOSED

EXACT R05 MODEL: OPEN
EXACT R06 MODEL: OPEN
EXACT R07 MODEL: OPEN
EXACT R08 MODEL: OPEN
```

Therefore the four rows remain `OPEN_MODEL_SELECTION`, but the next search is no longer free-form. Every candidate is accepted/rejected against this document.

The next pass should either:

1. perform targeted exact-model search/preview/license review for R05/R06 first, then R07/R08; or
2. if actual Azari world bytes are available sooner, continue real coordinate/spatial closure in parallel.

Do not implement temporary guardian actors while these exact visual rows remain open.

---

# 12. Verification state

```text
DESIGN REVIEWED: YES
EXTERNAL SOURCE REVIEWED: YES
LICENSE/PROVENANCE REVIEWED: YES for reference-use boundaries; final production model provenance remains open
CODE REVIEWED: N/A
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

Docs/reference work only; no build or CI is warranted for this pass.
