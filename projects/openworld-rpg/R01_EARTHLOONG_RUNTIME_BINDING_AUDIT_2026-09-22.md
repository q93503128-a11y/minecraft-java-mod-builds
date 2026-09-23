# R01 Earthloong — Pinned 26.2 Runtime/Animation Binding Audit

> Date: 2026-09-22  
> Scope: exact pinned dependency surface only; this is not a visual-playtest acceptance record.  
> Target: `threateningly_mobs:the_earthloong`  
> Artifact: `threateninglly-mobs-continued-1637799-8804647.jar`  
> SHA-256: `ed9278eddd99e37ae78d5ab70648abfdbf7a5b0d2f367a7b4ac1f8668e2461a0`

## Result

The exact admitted Fabric 26.2 dependency was byte-verified and inspected with `javap -p -c` in Openworld CI.

The donor Earthloong is usable as an external model/animation/presentation source, but its own combat state machine is **not** the R01 authored boss controller and must not become gameplay authority by accident.

Observed donor combat surface:

- entity: `net.mcreator.threateninglymobs.entity.TheEarthloongEntity`
- synced state:
  - `DATA_ChargeLevel`
  - `DATA_SkillNumber`
  - `DATA_alpha`
- procedures:
  - `EarthloongNormalAttackProcedure`
  - `EarthloongChargingProcedure`
  - `EarthloongSkillsProcedure`
  - `EarthloongStormRoarProcedure`
  - `EarthloongA1ConditionProcedure` .. `A4ConditionProcedure`
- available named animation definitions:
  - `woodlizard_attack` — 0.50 s
  - `woodlizard_charge` — 2.00 s
  - `woodlizard_skill1` — 1.25 s
  - `woodlizard_roar` — 2.50 s

## Donor combat semantics

`EarthloongSkillsProcedure` rolls donor `skills=1..6`.

Observed branches:

- donor skills 1–3: the same `EarthloongNormalAttackProcedure` when the current target is inside a 3.5-block local query;
- donor skill 4: `EarthloongChargingProcedure` while ChargeLevel < 3;
- donor skill 5: `LightingSummonProcedure` when ChargeLevel > 0;
- donor skill 6: `EarthloongStormRoarProcedure` when ChargeLevel > 0 and the current target is within the donor 6-block query.

Therefore donor skill numbers are **not** equivalent to project Claw Sweep / Tail Scythe / Quarry Rush / Lightning Furrow / Root Breaker IDs.

`EarthloongNormalAttackProcedure`:

- sets SkillNumber 1;
- plays the donor normal attack state;
- queues donor hit work after 5 ticks;
- queries a 3.5-block local AABB;
- damages only the mob's current target;
- uses donor `ATTACK_DAMAGE` through `DamageTypes.MOB_ATTACK`.

`EarthloongChargingProcedure`:

- sets SkillNumber 2;
- uses a 35-tick charge-up before adding Speed and incrementing ChargeLevel;
- is a donor charge-resource step, not the authored R01 Quarry Rush damage transaction.

`EarthloongStormRoarProcedure`:

- sets SkillNumber 4;
- stores current ChargeLevel as donor `power`;
- queues resolution after 27 ticks;
- uses a 7-block local AABB and visual-only vanilla lightning;
- applies donor lightning damage as `ATTACK_DAMAGE * power`;
- resets ChargeLevel under donor alpha rules.

Current Openworld damage-authority firewall already prevents those donor-origin HP values from directly damaging players.

## Binding decision

The dependency remains:

- presentation/model source: **ADAPT / dependency-only**
- donor combat AI: **not authoritative**
- donor damage values: **rejected**
- project server action selection: **OVERRIDE**
- project damage/guard/dodge/status result: **OVERRIDE**

The R01 Earthloong controller must own the locked action-selection rules from `STATUS_AND_R01_ENCOUNTERS.md`:

- 2-tick decision delay;
- exact phase weights;
- exact shared cooldowns;
- Root Breaker legality supplied by encounter/spatial state;
- max two consecutive space-control actions;
- forced physical action or reposition after that limit;
- phase transition does not reset cooldown/history;
- Lightning Furrow phase-2 lanes: first 4, then 3/4 alternating.

The spatial binder, not this controller, owns whether an action is currently legal after validating actual target position, model facing/anatomy, arena geometry and line/path constraints.

## Presentation compatibility status

Static bytecode establishes available animation identities and lengths, but **does not prove visual hitbox agreement**.

Current technical binding status:

- Claw Sweep ↔ donor `SkillNumber=1` / `woodlizard_attack`: dependency-only presentation-state bridge is runtime verified; project timing/hit authority remains separate;
- Quarry Rush ↔ donor `SkillNumber=2` / `woodlizard_charge`: dependency-only presentation-state bridge is runtime verified; donor charging procedure semantics and damage are not used;
- Lightning Furrow ↔ `woodlizard_skill1`: technical candidate only; timing/width/length/phase lane counts/Shock values plus exact 3/4-lane center offsets, committed axis, obstruction and vertical ground-projection rules are now canon-closed; runtime hit geometry/VFX binding remains implementation/presentation work;
- Root Breaker ↔ `woodlizard_roar`: technical candidate; project-owned 20-tick tell / 4.5-block horizontal radius / impact / 20-tick recovery / tagged-prop break runtime is implemented and startup verified, but final VFX/hitbox presentation acceptance remains open;
- Tail Scythe: no dedicated donor tail-scythe animation was proven by the pinned class surface, so presentation remains deliberately unbound.

No candidate above is promoted to final player-facing binding until real Minecraft visual review confirms readable body motion, attack direction, model contact, camera scale and server hit area within the project acceptance rules.

## Verification

```text
PINNED JAR BYTE VERIFIED: YES
EARTHLOONG ENTITY/PROCEDURE SURFACE INSPECTED: YES
DONOR SKILL STATE MAPPED: YES
ANIMATION IDS/LENGTHS INSPECTED: YES
PROJECT ACTION-SELECTION CONTROLLER: IMPLEMENTED + UNIT/BUILD/STARTUP VERIFIED
PHASE-1 CLAW/TAIL/QUARRY CANONICAL IMPACT DATA/AUTHORITY: IMPLEMENTED + UNIT/BUILD/STARTUP VERIFIED
PHASE-1 PHYSICAL START GEOMETRY: IMPLEMENTED + UNIT VERIFIED — Claw 0°..120° <=3.5, Tail 60°..180° <=4.5 with intentional flank overlap, Quarry 5.0..9.0 + clear committed line
CLAW/QUARRY DONOR PRESENTATION STATE BRIDGE: IMPLEMENTED + RUNTIME VERIFIED — SkillNumber 1/2 set + reset; donor damage/procedures remain non-authoritative
TAIL SCYTHE FINAL PRESENTATION: UNBOUND — no accepted dedicated donor animation exists on the pinned surface
ROOT BREAKER PROJECT RUNTIME: IMPLEMENTED + BUILD/SERVER/CLIENT-STARTUP VERIFIED — exact 40-tick proximity legality tracking, 20-tick tell, horizontal 4.5-block radius, direct impact, 20-tick recovery, tagged arena-prop break
ROOT BREAKER PLAYER-POISE 75: IMPLEMENTED IN SOURCE — partial-armor ArmorPoise, END contribution, +50% poise-resistance gear cap, 1 s recovery delay, 45/s recovery and 0.35 s post-break immunity are project-owned; final hit-stagger presentation remains separate
ROOT BREAKER FINAL PRESENTATION: NOT ACCEPTED — current rooted-dirt ring is a technical test telegraph, not production VFX
LIGHTNING FURROW DATA BINDING: IMPLEMENTED — 24-tick tell, 20-tick recovery, phase lane counts, 1.4 width, 12 length, Shock 35, donor SkillNumber 3 technical candidate
LIGHTNING FURROW PROJECT RUNTIME: IMPLEMENTED IN SOURCE — committed target axis, exact 3/4-lane offsets, local-ground projection, wall/step clipping, one-hit-per-wave, 26% benchmark magic impact and Shock 35 application are bound; final VFX/readability acceptance remains separate
RUNTIME SMOKE CHUNK LIFETIME: VERIFIED — CI harness now force-loads the verification chunk; prior failure was `UNLOADED_TO_CHUNK` with full HP/tickCount 0, not a combat regression
LATEST FULL OPENWORLD CI: 35698061263 — SUCCESS — commit 40961bc2c40373725108f31c32c1e18039d73415
REAL CLIENT VISUAL ATTACK REVIEW: NO
HITBOX-TO-ANIMATION ACCEPTANCE: NO
PLAYER-FACING EARTHLOONG COMBAT PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```


## 2026-09-22 design-precision follow-up

The implementation pass exposed several values that were previously described as design-closed but were not precise enough for deterministic source behavior. Live canon now closes:

- exact partial-armor ArmorPoise slot shares in COMBAT_BALANCE.md;
- player StatusPower / status-poise reference and enemy-origin fixed-budget rule in STATUS_AND_R01_ENCOUNTERS.md;
- enemy magic benchmark-share -> raw-damage authoring bridge in COMBAT_BALANCE.md;
- Lightning Furrow 3-lane offsets -2.50/0/+2.50 and 4-lane offsets -3.75/-1.25/+1.25/+3.75, committed target axis, wall termination and local ground-projection vertical tolerance;
- Earthloong-origin Shock proc fixed Lv8 source budget and player-poise pressure;
- exact 55% HP transition ownership: current committed action/recovery finishes, then 28-tick Stormshed, then Phase 2 eligibility.

These are canon corrections/precision bindings, not evidence that the final Lightning Furrow VFX or real-client hitbox readability has been accepted.


## 2026-09-22 Phase-1 space-control source binding

Source now additionally contains:

- shared same-Lv Medium-MR enemy magic authoring bridge;
- persistent-loadout-derived player max-poise publication and transient player-poise recovery/break state;
- shared player Shock threshold/decay/Conductive state;
- Earthloong Shock proc fixed source budget and poise pressure;
- Lightning Furrow commit-time lane geometry with local ground projection;
- exact Stormshed pending/28-tick transition state and 50% project incoming-damage multiplier.

This source state is not a final presentation acceptance claim. Tail Scythe remains presentation-unbound, and Lightning Furrow/Stormshed technical particles are not the accepted signature VFX layer.


## 2026-09-23 Phase-2 precision/backend pass

Phase-2 implementation choices are now closed and source-bound:

- Forked Heaven: 1.8-block markers, creation offsets 0/8/16, impacts 22/30/38, 20-tick recovery, exact 1P/2P/3+ SelectionThreat distribution, later-marker re-hit rule and 1.25-block vertical tolerance;
- Earthline Surge: 16-tick committed tell, 1.8 x 9.0 line, physical impact at tick 16, lightning at tick 27, 18-tick recovery, same ground-projected/obstruction-clipped line, both hits unguardable/perfect-unguardable and separately dodge-resolved;
- pure deterministic Phase-2 pattern authority and SelectionThreat participant ranking are implemented and unit-covered;
- runtime legality evaluates the exact Phase-2 range/LOS requirements but remains **presentation-gated** for Forked Heaven and Earthline Surge.

`runtimePresentationReady=false` is intentional. No accepted signature animation/VFX binding exists yet for these two attacks, and project production rules forbid promoting a temporary particle-only boss attack into normal player-facing runtime.


## 2026-09-23 verification-only presentation preview bridge

A new explicit M0 player-verification path now exercises the three still-gated Earthloong actions without promoting temporary presentation into normal gameplay:

- `/owr_earthloong_preview_tail` — forces canonical Tail Scythe geometry/timing with donor `SkillNumber=1` only as a visual candidate; normal Tail legality remains closed.
- `/owr_earthloong_preview_forked` — executes the exact 0/8/16 marker creation and 22/30/38 impact backend, shared same-cast re-hit rule, project damage and Shock using technical marker/strike particles.
- `/owr_earthloong_preview_earthline` — executes the canonical projected line, physical hit at tick 16, lightning follow-up at tick 27, separate dodge resolution and Shock-on-lightning-contact using technical ground/electric particles.

These commands are registered only by the existing M0 player-verification bootstrap. Production action selection is unchanged: Tail Scythe is still illegal in normal selection, and Forked Heaven / Earthline Surge still require their `runtimePresentationReady` gates, which remain `false`.

The temporary preview visuals are geometry/timing evidence only. They are not accepted Earthloong signature VFX and must not be used as the reason to flip production presentation gates.


## 2026-09-23 manual video finding — preview isolation defect

The first real-client preview recording showed that the M0 preview fixture was not isolated enough to produce trustworthy visual evidence.

Observed symptom:

- Quarry-Rush-like straight movement and Lightning-Furrow-like ground spark lanes continued even when no preview command was being evaluated.
- Tail / Forked Heaven / Earthline Surge commands therefore did not have a visually unique before/after state and could be rejected while the normal R01 controller already had a committed action.
- The runtime previously cleared the donor mob target only at START/END level tick. Donor/vanilla AI may reacquire a target during the entity tick between those callbacks, and the project itself used that transient target in `seedInitialThreatFromMobTarget()`. This leaves a presentation/state contamination risk even though donor-origin HP damage remains blocked.

Correction:

- `/owr_spawn_earthloong` in the M0 verification build now arms the spawned Earthloong as a persistent verification fixture.
- The fixture is held `NoAI=true`, targetless and horizontally stationary while idle.
- The normal R01 action selector is bypassed for the fixture; only explicit Tail / Forked / Earthline preview commands may commit actions.
- Preview completion returns to isolated idle rather than immediately scheduling a normal R01 action.
- Commands now print explicit accepted/rejected feedback so a failed distance/LOS/busy gate cannot be mistaken for a visual no-op.

This correction is verification-only. It does **not** claim the production donor-AI containment problem is fully closed; production still needs a dedicated authority pass so donor combat procedures cannot alter presentation/resource/movement state between project-owned ticks.


## 2026-09-23 automatic preview pass

Manual command-by-command activation is no longer the default player verification path.

For the M0 playtest artifact, `/owr_spawn_earthloong` now:

1. spawns the isolated Earthloong fixture about 7 blocks in front of the invoking player;
2. holds donor AI and normal R01 action selection paused;
3. waits 2 seconds;
4. previews `Tail Scythe`;
5. waits 2 seconds after the committed action finishes;
6. previews `Forked Heaven`;
7. waits 2 seconds after that action finishes;
8. previews `Earthline Surge`;
9. returns to isolated idle after the one-pass sequence.

The three explicit preview commands remain available as fallback developer verification controls, but they are no longer required for the normal visual-review workflow.

The automatic pass deliberately does not promote any candidate presentation into production. Tail remains an unaccepted donor-animation candidate, while Forked Heaven and Earthline Surge remain presentation-gated until real-client footage confirms readable signature presentation and visual/hit geometry agreement.


## 2026-09-23 real-client follow-up — fixture locomotion and chat cleanup

A second real-client recording exposed two verification-harness artifacts rather than production encounter behavior:

- persistent `[M0]` chat messages were visible to the player;
- the fixture was held `NoAI=true` while idle, which suppressed normal donor locomotion/idle behavior and left Earthloong in an unnatural frozen presentation between preview actions.

Correction:

- the M0 verification harness no longer emits player-facing chat messages;
- idle verification state now leaves donor AI enabled while the project continues to clear combat targets;
- each explicit preview action may still temporarily take movement control for its authored committed timeline, then returns AI to its prior enabled state.

The recording was in Creative mode. Project damage intentionally preserves Minecraft Creative/invulnerability protection, so lack of HP loss in that recording is not evidence that authored contact is absent. Damage/guard/dodge/ailment acceptance still requires a Survival-mode playtest.


## 2026-09-23 animation-first combat redesign gate

Real-client review invalidated the old Tail Scythe presentation assumption. The pinned `woodlizard_attack` state did not read as a tail-scythe motion, and no dedicated tail-swing animation is proven on the pinned dependency surface.

The Earthloong combat-presentation workflow is therefore changed:

```text
pinned donor model/animation motion
-> observed body direction/contact/weight
-> gameplay attack role
-> server hit geometry/timeline
-> signature VFX/SFX/camera/arena reaction
-> new animation only when the accepted donor motion set is genuinely insufficient
```

Consequences:

- `woodlizard_attack` is **not** accepted as Tail Scythe;
- the existing seven project attack names are not preservation requirements;
- existing project-owned geometry/timeline/damage code remains reusable implementation material, but does not dictate animation meaning;
- Forked Heaven / Earthline Surge backend logic stays preserved while their production presentation gates remain closed;
- the M0 Earthloong fixture now previews raw pinned donor `SkillNumber 1 -> 2 -> 3 -> 4` states in order, without Tail/Forked/Earthline hit geometry, damage or temporary signature VFX layered over the motion;
- individual raw states can be replayed with verification-only `/owr_earthloong_motion_1` through `/owr_earthloong_motion_4`;
- idle intervals restore donor locomotion so the review captures the model's normal stance/movement context as well as the isolated action motion;
- no production action-role remap is accepted until real Minecraft footage is reviewed.

The four pinned raw review durations remain:

| SkillNumber | donor animation | duration |
|---:|---|---:|
| 1 | `woodlizard_attack` | 10 ticks / 0.50 s |
| 2 | `woodlizard_charge` | 40 ticks / 2.00 s |
| 3 | `woodlizard_skill1` | 25 ticks / 1.25 s |
| 4 | `woodlizard_roar` | 50 ticks / 2.50 s |

This is a verification workflow correction, **not** a new finalized Earthloong kit.
