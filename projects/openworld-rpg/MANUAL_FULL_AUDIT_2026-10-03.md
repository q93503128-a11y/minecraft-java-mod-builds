# Open-World RPG — Manual Full Audit — 2026-10-03

> Audit basis: current GitHub `main` at `b6fb1bd9aaf14e7176b79a082310e3c79c06c6ea`
>
> Latest gameplay code checkpoint inspected: `f7c1b69622bd062140841fc47d60458caa87f1e3`
>
> Build Openworld RPG #322 / run `37117434623`: SUCCESS
>
> Scope: manual source/canon/runtime-artifact cross-check. This is **not** a playtest and does not change gameplay source.

## 1. Audit conclusion

The project has a large amount of server-authoritative backend work and the latest Openworld workflow is healthy at compile/startup level, but it is **not yet safe to call R01 source-ready or player-facing implemented**.

The most important result of this audit is that several green-CI paths are not actually end-to-end closed. In particular, current Regalhart reward documentation overstates heal/barrier participation wiring, disengage can preserve stale reward participation, Hartcrown has scope/ordering/design-closure problems, and current client startup logs contain project-owned missing-texture warnings.

The existing fail-closed production gates are therefore still necessary and should remain closed.

## 2. Confirmed defects

### BLOCKER — Regalhart support reward participation is only partially wired

Pass 10 / PROJECT text says a real encounter-linked heal or barrier can establish personal Regalhart reward eligibility.

Actual source does not match that statement.

`ProjectHealingRuntime.applyRegalhartSkillHeal(...)` delegates to the common encounter-heal path, but that path records only:

- `R01EarthloongEncounterService.recordValidatedSupportContribution(...)`
- `R01NatureSpiritRewardService.recordValidatedSupportContribution(...)`

There is no `R01RegalhartRewardService.recordValidatedSupportContribution(...)` call and the returned `Application` does not even carry a Regalhart participation field.

`ProjectBarrierRuntime.applyRegalhartSkillBarrier(...)` has the same problem: it reaches the common encounter-barrier path, which records only Earthloong and Nature Spirit support.

Result:

- damage contribution: connected;
- successful control/debuff contribution: connected;
- heal contribution: **not connected**;
- barrier contribution: **not connected**.

Current tests do not cover a Regalhart healing/barrier participation result, which is why CI remains green.

### BLOCKER — Regalhart disengage/reset preserves stale reward participants

`R01RegalhartMaterializationRuntime.resetExistingToControllerStart(...)` correctly:

- clears mob target/navigation;
- returns the same boss entity to the cycle start;
- clears velocity;
- resets Regalhart combat execution state;
- restores canonical HP/poise;
- clears project hostile statuses;
- acknowledges disengage.

It does **not** clear the matching entry in
`R01RegalhartRewardState.activeEncounters`.

Regalhart reward encounter identity is based on the boss entity UUID. Disengage reuses the same entity/UUID. Therefore a player who qualified in an abandoned fight can remain in the participant snapshot for the later restarted fight, including the class frozen on the old first contribution.

This is both a reward-authority error and a persistent-state lifecycle leak.

A dedicated abort/reset operation should remove only the abandoned active encounter snapshot without touching committed first-defeat or pending reward history.

### HIGH — Hart's Momentum only binds to project-owned hostile targets

The only production call to
`R01RegalhartMythicRuntime.onAcceptedMeleeBasic(...)`
is inside `PlayerAttackAuthorityMixin` after the code has already required
`ExternalActorBindingRuntime.ownsDamageAuthority(target)`.

For vanilla mobs or another valid melee target that is not a project-bound external actor, the mixin returns to ordinary Better Combat/vanilla handling before Hart's Momentum is reached.

The item canon says “the next melee basic attack” and does not restrict the unique effect to project-owned enemies.

Physical Hartcrown delivery is still gated, so this is currently latent, but it must be corrected before the item becomes obtainable.

### HIGH — Hart's Momentum movement occurs after HP damage

Current melee order is effectively:

```text
validate Better Combat/project hit
-> apply project HP damage
-> consume Hart's Momentum
-> move/lunge player
-> apply project poise damage
```

That is not the natural reading of “the next melee basic attack lunges toward its valid target”.
A real client may perceive this as a hit followed by a pull/slide toward the target.

A lethal HP hit may also consume the effect before the later movement step because the Hartcrown runtime does not require the target to remain alive at that point.

The correct production transaction should preserve the already-validated target/reach while making the movement/contact ordering visually coherent.

### HIGH — Hart's Momentum introduced an undocumented re-arm rule

The canonical item rule specifies:

- sprint-speed movement for >=1.25 s;
- next melee basic within 2.0 s;
- 5.0 s internal cooldown.

Current `R01RegalhartMythicEffectState` additionally requires the player to break the sprint/movement segment before another qualification can occur after an expired opportunity or previous trigger.

That interruption requirement is not present in `EQUIPMENT_BALANCE.md`. Pass 11 then documents the implementation choice as if it were canon.

This violates the project's own “implementation-time gameplay choices remain: NO” rule.

Either canon must explicitly adopt that interruption behavior, or runtime should be changed to the direct authored interpretation.

### HIGH — current R01 “design closed” status is overstated

`PROJECT.md` currently says:

```text
R01 GAMEPLAY / CONTENT DESIGN CLOSED: YES
R01 IMPLEMENTATION-TIME GAMEPLAY CHOICES REMAIN: NO
```

However current live Regalhart canon deliberately leaves gameplay values unresolved:

- exact 7.0-block weighted-selection endpoint ownership;
- exact 12.0-block weighted-selection endpoint ownership;
- Crown Charge ordinary `guardable` value;
- Rear Kick guard-pressure value;
- Rear Kick player-poise pressure;
- mirrored Antler Sweep second-hit exact timing.

These are gameplay contracts, not merely model/VFX asset bindings.

`STATUS_AND_R01_ENCOUNTERS.md` §27 also claims “full signature attack/tell/guard/status behavior” and exact action-selection rules are closed, which conflicts with the same file's explicit fail-closed omissions.

The closure table should not claim zero implementation-time gameplay choices until these values are actually decided.

### HIGH — stale Regalhart first-clear reward canon

The live Regalhart reward section in
`STATUS_AND_R01_ENCOUNTERS.md` still says:

```text
first EXP 40%
first Class XP 30%
```

Latest implementation and later project-level reward pass use:

```text
Combat EXP 20%
Class XP 15%
```

`GAME_DESIGN.md` also gives the global first field/world-boss EXP target as about 20%.

Under current authority order, the later PROJECT/reward implementation is the newer correction, so 20% / 15% is the effective current value. The old 40% / 30% live wording should be removed or superseded explicitly. Leaving both violates the current-canon hygiene rule.

## 3. Runtime / presentation findings

### HIGH — project-owned spell-effect models log missing textures during client startup

Build #322 is green, but `client-gameplay.log` contains Minecraft “Missing textures in model” warnings for project resources including:

- `warrior_driving_slash`
- `warrior_earthshatter`
- `guardian_warding_strike`
- `warrior_cyclone_cut`
- `consecrated_ground`
- `guardian_bulwark_rush`
- `guardian_counterwall`
- `sanctuary_ward`
- `warrior_breaker_slam`
- `guardian_aegis_field`
- `guardian_unbroken_line`
- `rebuke_burst`
- `warrior_iron_counter`

The referenced PNG bytes do exist in the produced JAR, so this looks like a model/atlas/resource-binding issue rather than missing files on disk.

Because CI only reaches client startup/render-thread smoke and **does not join a world**, the actual in-game appearance is not proven. Treat this as a real presentation defect until a joined-client render confirms otherwise.

### MEDIUM — Hartcrown sprint qualification checks “sprinting + any horizontal velocity”, not actual sprint-speed motion

Current runtime considers the sprint requirement satisfied by:

```text
player.isSprinting()
AND horizontal delta movement > 0
```

That is weaker than literal “moving at sprint speed”. Slow/collision-limited movement can still count.

This needs either a canon wording normalization to “actively sprinting while moving” or a more precise movement threshold.

### MEDIUM — Hartcrown adds another permanent all-player server-tick loop

`R01RegalhartMythicRuntime.tick(server)` scans all connected players every tick, even though almost all players will not have Hartcrown equipped.

This is small at current scale and is not evidence of a measured performance problem, but the project already owns several per-player tick systems. Prefer a tracked equipped-player set or fold the state into an existing movement/equipment runtime before permanent tick costs accumulate.

## 4. Spawn / external-actor authority audit

### HIGH GATE — spawn override metadata is not visibly enforced by project source

The Earthloong actor overlay declares:

```text
spawn: OVERRIDE
openworld_rpg:actors/no_random_spawn
```

but the project integration runtime currently uses the overlay primarily as validated metadata. No project source path found in this audit actively removes/denies a donor natural spawn from that policy.

Moreover, `ExternalActorBindingRuntime.ENTITY_LOAD` applies project combat stats to every matching registered external actor, regardless of `authored_spawn`, and
`R01EarthloongPhysicalEncounterRuntime.ENTITY_LOAD` creates runtime state for every Earthloong it sees.

The pinned donor artifact surface also contains
`data/threateningly_mobs/forge/biome_modifier/the_earthloong_biome_modifier.json`.

This audit did **not** perform a fresh real-world natural-spawn reproduction, so it does not claim an observed rogue Earthloong spawn. It does establish that project-level spawn-override enforcement is not proven by current source.

Before R01 ecology is called production-ready, verify one of:

1. the exact Fabric donor cannot naturally spawn the actor in the admitted runtime; or
2. an accepted external filter/config demonstrably suppresses it; or
3. project code explicitly enforces the spawn override.

The same principle should be checked for every dependency actor tagged `no_random_spawn`.

### Correct fail-closed behavior retained

`R01ExternalActorCatalog.productionSpawnReady(REGALHART)` is still false.

Regalhart materialization therefore cannot create a live production boss today. This is correct and should remain so until the blockers in this report and the already-known encounter/presentation gates are closed.

## 5. Reward / persistence paths that looked correct

Manual review did not find an issue in these specific Regalhart behaviors:

- first valid contribution freezes reward class;
- per-player first-defeat history is persisted before delivery;
- reward RNG is rolled/persisted before delivery;
- reconnect cannot reroll base/grade/Item Lv/armor slot/signature result;
- progression/Gold grants use the existing idempotent reward transaction;
- Antler delivery is receipt-backed and capacity-blocked delivery remains pending;
- damage contribution is wired after actual project damage;
- successful project control/debuff contribution is wired;
- defeat advances shared territory cycle only after personal plans are durable;
- production spawn remains fail-closed.

Pending Regalhart finalizations remaining resident while physical equipment delivery is blocked is intentional in the current pre-production state. It becomes a cleanup requirement when complete physical delivery is implemented.

## 6. Current intentional Regalhart gates — not defects

The following are still deliberately unimplemented/fail-closed and should not be “fixed” by guessing:

- Crown Charge ordinary guardability;
- Rear Kick guard-pressure/player-poise contract;
- mirrored Antler Sweep second-hit exact timing;
- exact 7.0 / 12.0 weighted-selection endpoint ownership;
- head/antler weak-point physical geometry;
- accepted physical Crown Charge / Royal Bound presentation;
- real camera/occlusion caller;
- ordinary boss-equipment physical materialization/value;
- Hartcrown physical item presentation/materialization.

The code correctly refuses to open Regalhart production spawning while these remain.

## 7. What the latest green CI actually proves

Build Openworld RPG #322 / run `37117434623` proves:

- dependency verification: PASS;
- unit tests: PASS;
- clean build: PASS;
- bootstrap JAR verification: PASS;
- core-profile dedicated server boot: PASS;
- gameplay-profile dependency server boot: PASS;
- gameplay-profile client startup/render-thread smoke: PASS;
- both verification JAR builds: PASS;
- Modrinth playtest pack packaging: PASS.

Its own generated report still states:

```text
client world join: NOT RUN
exact real-player Better Combat network hit: NOT TESTED
real joined-player crossbow bolt: NOT TESTED
joined-player dodge feel: NOT TESTED
player-facing integrated R01 content: NOT IMPLEMENTED
playtest: NOT RUN
multiplayer test: NOT RUN
```

Therefore build success must not be interpreted as gameplay completion.

## 8. Overall project-state assessment

### Backend / authority

A substantial server-authoritative foundation exists:

- combat/progression/loadout/resource state;
- project damage/poise/guard/dodge boundaries;
- class skill runtimes;
- inventory/storage/material/reward transaction foundations;
- R01 gathering/fishing/economy/service backends;
- Earthloong authority work;
- several elite/field-boss controllers;
- reconnect/idempotency infrastructure.

The codebase is not a hollow prototype.

### Player-facing game

The project is still much earlier than the backend commit count suggests.

Current PROJECT status remains materially accurate here:

```text
R01 ASSET_BINDING COMPLETE: NO
R01 SPATIAL_BINDING COMPLETE: NO
R01 SOURCE READY: NO
R01 IMPLEMENTED: NO
R01 PLAYTESTED: NO
R01 MULTIPLAYER TESTED: NO
```

The immediate quality risk is no longer “can the backend compile?”.
It is **integration truth**:

- canon vs source;
- API seam vs actual caller;
- persistence lifecycle;
- donor spawn containment;
- actual joined-client visuals;
- actual playable world flow.

## 9. Recommended repair order

1. Fix Regalhart heal/barrier participation and add regression tests.
2. Clear Regalhart active reward participation on disengage/reset and test abandoned-fight -> refight behavior.
3. Resolve the live 40/30 vs 20/15 Regalhart reward canon conflict.
4. Remove the false “zero implementation-time gameplay choices” claim or close the remaining Regalhart numeric/timing choices.
5. Decide Hart's Momentum continuous-sprint re-arm canon; then fix scope + ordering + tests before item delivery.
6. Repair the project spell-effect texture/model/atlas warnings and verify in a joined world.
7. Prove/enforce dependency natural-spawn suppression before declaring R01 ecology contained.
8. Only then resume Regalhart production-spawn admission.
9. After core R01 player-facing paths are materially connected, run a real single-player full-flow playtest.
10. Run multiplayer only after the single-player flow is stable; current multiplayer status remains untested.

## 10. Validation state after this audit

```text
CURRENT MAIN REVIEWED: YES
LATEST CODE/CI ARTIFACT MANUALLY INSPECTED: YES
CANON/SOURCE CROSS-CHECKED: YES
REGALHART REWARD/RESET/HARTCROWN PATH MANUALLY REVIEWED: YES
CLIENT STARTUP LOG MANUALLY REVIEWED: YES

NEW SOURCE FIXES APPLIED BY THIS AUDIT: NO
BUILD RE-RUN BY THIS AUDIT: NO
CLIENT WORLD JOIN: NOT RUN
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```


---

## 11. Resolution update — 2026-10-03

Code commit `66df0af2b2837bf7473159abde169b0035ca4c56` resolves the two BLOCKER findings from §2:

- Regalhart effective-heal support participation now reaches `R01RegalhartRewardService`;
- Regalhart effective-barrier support participation now reaches `R01RegalhartRewardService`;
- the heal application result explicitly carries Regalhart participation and rejects impossible cross-encounter double qualification;
- successful Regalhart disengage/reset now aborts only the uncommitted active encounter snapshot;
- committed first-defeat history and pending durable rewards remain intact.

Regression coverage was added for abandoned-fight cleanup and Regalhart healing-result invariants.

Build Openworld RPG #323 / run `37119716754`: **SUCCESS** across tests/clean build, dependency inspection, core/gameplay dedicated-server smoke, gameplay client startup, both verification JARs, Modrinth pack packaging and artifact upload.

The former BLOCKER findings are therefore **RESOLVED IN SOURCE + BUILD VERIFIED**. They remain **NOT PLAYTESTED / NOT MULTIPLAYER TESTED**.

The stale 40%/30% Regalhart first-defeat wording and overstated R01 full-design-closure claims were corrected in the following canon-hygiene documentation pass. Remaining Hartcrown, texture/resource, spawn-containment and unresolved Regalhart gameplay-contract findings from this audit remain open.
