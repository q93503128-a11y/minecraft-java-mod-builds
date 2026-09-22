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
- Lightning Furrow ↔ `woodlizard_skill1`: technical candidate only;
- Root Breaker ↔ `woodlizard_roar`: technical candidate only;
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
RUNTIME SMOKE CHUNK LIFETIME: VERIFIED — CI harness now force-loads the verification chunk; prior failure was `UNLOADED_TO_CHUNK` with full HP/tickCount 0, not a combat regression
LATEST FULL OPENWORLD CI: 35689727920 — SUCCESS
REAL CLIENT VISUAL ATTACK REVIEW: NO
HITBOX-TO-ANIMATION ACCEPTANCE: NO
PLAYER-FACING EARTHLOONG COMBAT PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
