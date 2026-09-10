# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Remote `main` verified at run start and again immediately before writes: `582754f8344a11a3a5a168739917a40ca76b33c1`.
- Baseline already contained the two production weapon-family graph, server-owned ItemStack loadout boundary, move-id-only serverbound intent, generation-aware runtime, dimension-scoped player weapon sessions, lifecycle cleanup, and completed boss presentation/network authority work.
- Region 01 still has no approved final boss material/profile/asset input in the recovered production state.
- No approved concrete production weapon ItemStack provisioning identity, client control mapping, shape-specific hit geometry, damage/range/resource policy, or final presentation input was available; none was invented.

## Completed in this batch

M3 shared authoritative attack-clock monotonicity:

- Implementation commits: `5254d714d2432182417ce163ed62568b2f2d3fb7` and regression-test descendant `87248f47b66462c960125284986fbf3099e87d48`.
- `AttackStateMachine` now records the most recently sampled server game tick for an active execution.
- Same-tick repeat sampling remains legal, but an older tick can no longer rewind a live `telegraph -> ACTIVE -> recovery` execution.
- On rewind, the execution is fail-closed and cancelled before `IllegalArgumentException` is raised, preventing later continuation from an already-observed future phase.
- Negative attack start ticks are rejected before an execution is created.
- Completion and explicit cancellation clear the monotonic watermark so a fresh execution starts a fresh clock epoch.
- Because `AttackStateMachine` is shared by player-weapon and boss controllers, this closes the same temporal-authority invariant without creating a second timer or duplicating policy.
- No cadence values, damage, range, hit geometry, item/control UX, model, animation, VFX, sound, or boss content changed.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/AttackStateMachine.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/AttackStateMachineTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Push preflight found remote `main` unchanged at `582754f8344a11a3a5a168739917a40ca76b33c1`; writes were applied as normal non-force descendants.
- `Build Riftfrontier` run `34474521611` targets implementation/test HEAD `87248f47b66462c960125284986fbf3099e87d48`.
- Verified SUCCESS in that run so far: Java/setup, Gradle setup, toolchain verification, asset-intake tests, JUnit + clean build, and required Riftfrontier native GameTest gate.
- Dedicated-server smoke was IN PROGRESS when this handoff was written.
- Xvfb client smoke, executable JAR inspection, build report, deliverable/log upload: NOT YET RUN at handoff-write time.
- Local Gradle execution: NOT RUN because this execution environment could not resolve github.com for a repository clone; GitHub Actions is the executable validation source.
- Human field play, multiplayer latency, concrete player control transport, production hit geometry/damage/resource policy, and final player/boss presentation remain NOT TESTED / NOT APPROVED as applicable.

## Do not repeat or revert

- Boss source/asset provenance, reviewed animation preparation, semantic-animation/material/geometry publication provenance, UUID-qualified boss presentation/network ordering/lifecycle/cache/render work are DONE.
- `weapon_family` / `weapon_module` schema, decoder, graph validation, reference dossier, two-role lock, first production player-combat graph, server-owned ItemStack loadout component and move-id-only serverbound authority are DONE.
- Player weapon authoritative sessions are dimension-scoped and reset on login/logout/dimension change/clone. Do not restore cross-dimension attack-clock continuity.
- `AttackStateMachine` is the shared authoritative cadence state machine and now enforces nondecreasing sampled server ticks during each execution. Do not reintroduce rewind-capable sampling or a parallel timer.
- `AttackPattern` remains the sole authored `telegraph -> ACTIVE -> recovery` cadence primitive. `recovery_pivot` cannot shorten recovery, create another hit window, grant generic invulnerability, or survive equipment/world invalidation.
- Do not promote provisional production tick values to field-balanced values and do not invent blocked UX/art/combat values.

## Exact next start point

1. Re-check current remote `main` first, then recover the final conclusion of `Build Riftfrontier` run `34474521611` and any newer descendant Riftfrontier run. If any required gate failed, repair the first real failing cause before new features.
2. Re-check approved Region 01 boss inputs and approved player ItemStack/control inputs. Route them through existing gates only if legitimate new source material exists.
3. If those remain absent, inspect the next objective M3 server-authority gap without inventing art/UX/balance. Prefer a complete runtime invariant or transport/lifecycle boundary that connects existing systems rather than another schema or observation-only helper.
4. Shape-specific hit volumes, damage/range/resource policy, provisional timing tuning, final item/model/animation/VFX/sound, and human multiplayer feel stay blocked on evidence/approval.
