# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Remote `main` was independently verified at run start and again immediately before writes as `d53e8636637b41f70ad6761ed8bbcddf266d0ced`.
- The preceding attack-clock implementation CI (`Build Riftfrontier` run `34474521611`, HEAD `87248f47b66462c960125284986fbf3099e87d48`) was recovered as completed SUCCESS.
- Baseline already contained the two production weapon-family graph, server-owned ItemStack loadout boundary, move-id-only serverbound intent, generation-aware runtime, dimension-scoped player weapon sessions/lifecycle cleanup, and completed boss presentation/network authority work.
- Region 01 still had no approved final boss material/profile/asset input in the recovered production state.
- No approved concrete production weapon ItemStack provisioning identity, client control mapping, shape-specific hit geometry, damage/range/resource policy, or final presentation input was available; none was invented.

## Completed in this batch

M3 authoritative attack-clock observation unification:

- Implementation descendants: `4d3b883a34983a618510cd8552bb3b9907ed751a`, `e5d1ffa513c45cc1556d1898addbd29575de4f6e`, `b3accf0ac8a0363d61ef2e771cd4ae4d4f8e1578`, final code/test HEAD `0486a7b3676717e433eaa80517e740bad8d9caf0`.
- Found a real monotonicity bypass: `PlayerWeaponCombatController.recoveryPivotAuthorized(...)` sampled the raw immutable `AttackExecution` directly, so that observation could see a later server tick without updating `AttackStateMachine`'s monotonic watermark.
- Added `AttackStateMachine.observeCurrent(gameTick)` as the read-only authoritative observation path. It shares rewind rejection/cancellation and same-tick semantics with `advance(...)` but intentionally does not consume phase-transition bookkeeping.
- `advance(...)` now uses that same observation primitive, keeping one clock-validation implementation.
- `recoveryPivotAuthorized(...)` now observes through `AttackStateMachine`, so module phase authorization cannot observe a future tick and then allow an older `advance`, nor can it query an older tick after a later authoritative advance.
- Rewind through either direction fails closed and cancels the execution; a fresh execution can start afterward.
- Added unit regressions for read-only observation monotonicity, same-tick observation + phase transition preservation, and both recovery/advance rewind orderings.
- No cadence values, damage, range, hit geometry, ItemStack/control UX, model, animation, VFX, sound, or boss content changed.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/AttackStateMachine.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/PlayerWeaponCombatController.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/AttackStateMachineTest.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/PlayerWeaponCombatControllerTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Push preflight found remote `main` unchanged at `d53e8636637b41f70ad6761ed8bbcddf266d0ced`; all writes were normal non-force descendants on `main`.
- Prior run `34474521611` was recovered as FULL SUCCESS.
- `Build Riftfrontier` run `34480792673` targets final code/test HEAD `0486a7b3676717e433eaa80517e740bad8d9caf0`.
- Verified SUCCESS so far in `34480792673`: setup/toolchain, asset-intake tests, JUnit + clean build, and required Riftfrontier native GameTest gate.
- Dedicated-server smoke was IN PROGRESS when this handoff was written.
- Xvfb client smoke, executable JAR inspection, build report, deliverable/log upload: NOT YET RUN at handoff-write time.
- Local Gradle execution: NOT RUN because this execution environment could not obtain a repository clone; GitHub Actions is the executable validation source for this run.
- Human field play, multiplayer latency, concrete player control transport, production hit geometry/damage/resource policy, and final player/boss presentation remain NOT TESTED / NOT APPROVED as applicable.

## Do not repeat or revert

- Boss source/asset provenance, reviewed animation preparation, semantic-animation/material/geometry publication provenance, UUID-qualified boss presentation/network ordering/lifecycle/cache/render work are DONE.
- `weapon_family` / `weapon_module` schema, decoder, graph validation, reference dossier, two-role lock, first production player-combat graph, server-owned ItemStack loadout component and move-id-only serverbound authority are DONE.
- Player weapon authoritative sessions are dimension-scoped and reset on login/logout/dimension change/clone. Do not restore cross-dimension attack-clock continuity.
- `AttackStateMachine` is the shared authoritative cadence state machine. Both state progression and read-only gameplay phase authorization now participate in its nondecreasing server-tick watermark. Do not restore direct raw `AttackExecution.sample(...)` use from module/runtime authorization paths or add a parallel timer.
- `AttackPattern` remains the sole authored `telegraph -> ACTIVE -> recovery` cadence primitive. `recovery_pivot` cannot shorten recovery, create another hit window, grant generic invulnerability, or survive equipment/world invalidation.
- Do not promote provisional production tick values to field-balanced values and do not invent blocked UX/art/combat values.

## Exact next start point

1. Re-check current remote `main` first, then recover the final conclusion of `Build Riftfrontier` run `34480792673` and any newer descendant Riftfrontier run. If any required gate failed, repair the first real failing cause before new features.
2. Re-check approved Region 01 boss inputs and approved player ItemStack/control inputs. Route them through existing gates only if legitimate new source material exists.
3. If those remain absent, inspect `PlayerWeaponServerRuntime.handleMoveIntent(...)`, `MinecraftPlayerWeaponCombatAdapter.beginMove(...)`, and the player lifecycle event wiring specifically for actor eligibility at death/respawn/spectator or otherwise non-combat-capable server states. If authority can currently be established or progressed by an ineligible actor, close that lifecycle/input boundary fail-closed with executable regression coverage; if it is already covered, move to the next objective M3 server-authority gap instead of duplicating it.
4. Shape-specific hit volumes, damage/range/resource policy, provisional timing tuning, final item/model/animation/VFX/sound, and human multiplayer feel stay blocked on evidence/approval.
