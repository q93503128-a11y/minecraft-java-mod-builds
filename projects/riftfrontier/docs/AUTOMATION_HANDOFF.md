# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Run-start remote `main`: `cf0575e1a857c41a408f2c863b76d8f6623ab222`.
- Recovered `Build Riftfrontier` run `34535359597`, HEAD `db959f6b32e40eef19b3a8c7bcc7a8674aef3651`: final `FAILURE`.
- First actual failing gate: `MinecraftBossValidatedRuntimeTest.productionFactoryRequiresValidatedSemanticsAndReturnsSealedRuntime()` line 36. Production compiled, but an older source-contract assertion still required direct `delegate.cancelAttack(); throw stale;` after production had intentionally moved stale handling to `retireStaleGenerationOwnerClaim(); throw stale;`.
- No approved Region 01 final boss profile/source/asset input was found in the recovered canonical/runtime state.
- No approved production weapon ItemStack provisioning identity, concrete client control mapping, shape-specific hit geometry, damage/range/resource policy, or final presentation input was found; none was invented.

## Completed in this batch

M3 stale-generation boss retirement verification recovery:

- Code/test HEAD: `9812c95e49150b9a7bc30b2891a639d45b418264`.
- Preserved the production stale-generation owner-retirement implementation; no authority behavior was weakened to satisfy the test.
- Updated `MinecraftBossValidatedRuntimeTest` so the structural contract now requires `retireStaleGenerationOwnerClaim(); throw stale;`, explicit `retireIfGenerationStale()`, and cancellation inside generation retirement.
- This closes the false-negative test regression that blocked verification of the prior stale-generation owner-claim retirement batch.
- No boss identity, art, controls, ItemStack provisioning, production geometry, damage/range/resource values, cadence tuning, model, animation, VFX or sound was added.

## Changed systems/files

- `src/test/java/kr/moonseungjun/riftfrontier/combat/MinecraftBossValidatedRuntimeTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Failed predecessor `Build Riftfrontier` run `34535359597`: `208 tests completed, 1 failed`; the sole failure was the stale structural assertion described above. GameTest/server/client/JAR stages were skipped after that JUnit failure.
- Recovery `Build Riftfrontier` run `34540367548`, HEAD `9812c95e49150b9a7bc30b2891a639d45b418264`: final `SUCCESS`.
- Verified successful gates in run `34540367548`: toolchain, asset-intake tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb client smoke, executable JAR inspection, build report, deliverables upload, logs/report upload.
- Local Gradle: NOT RUN; executable validation source was GitHub Actions.
- Human field play, multiplayer latency/feel, concrete player controls, production boss/player hit geometry/damage/resource policy, final player/boss presentation and real production boss reload during an encounter: NOT TESTED / NOT APPROVED.

## Do not repeat or revert

- M2 persistence/restart/Region 01 technical-proxy lifecycle work is DONE.
- Boss presentation/network semantic capability plumbing is DONE; do not add more presentation plumbing without approved real assets.
- Player `weapon_family` / `weapon_module` schema, reference role lock, two-family production graph, server-owned ItemStack loadout component, move-id-only serverbound intent and generation-aware runtime are DONE.
- Player sessions are exact-actor-instance + dimension + loadout scoped and alive/non-removed/non-spectator gated. Do not restore UUID-only or cross-dimension continuity.
- Shared Minecraft combat actor/target admission rejects wrong-level/dead/removed/spectator authority before hit resolution. Do not let future shape resolvers bypass it.
- Generic Minecraft attack executions and Minecraft-facing boss attack starts remain exact-actor-instance + dimension bound.
- `AttackStateMachine` remains the single monotonic authoritative `telegraph -> ACTIVE -> recovery` clock. Do not add parallel timers or raw gameplay sampling bypasses.
- Validated boss runtimes derived from a published snapshot remain generation-bound. Stale generations relinquish their exact process-local owner claim, become irreversibly retired, and cannot resume or rebind.
- Keep the aligned stale-generation structural contract. Do not restore the obsolete direct-cancel assertion that caused run `34535359597` to fail.
- Validated Minecraft boss capabilities remain exact entity-instance + dimension lifetime-bound and irreversibly invalidated by `EntityLeaveLevelEvent`.
- One live boss entity may own only one current-generation independently mutable validated Minecraft boss combat runtime.
- Once that runtime is Minecraft-bound, attack-start and phase mutations must not use ownerless APIs; phase mutation requires the exact authoritative `ServerLevel + LivingEntity` context.
- Provisional attack ticks are not final balance. Do not invent blocked damage/range/resource/geometry/control/art values.

## Exact next start point

1. Re-check current remote `main` and final status of the latest Riftfrontier descendant CI before making changes.
2. Re-check approved Region 01 boss source/profile/asset input and approved player ItemStack provisioning/control mapping. Use existing gates only if legitimate new input exists.
3. If still absent, do not repeat generation retirement, owner lifetime, singleton claim, owner-context mutation, actor-instance, eligibility, target-admission, dimension, loadout or attack-clock work. Audit the next objectively testable M3 authority/state divergence, prioritizing lifecycle edges where process-local combat capability state can outlive or diverge from authoritative Minecraft/content state.
4. Keep shape-specific production hit volumes, damage/range/resource policy, timing tuning, final ItemStack/model/animation/VFX/sound and multiplayer feel blocked on evidence/approval.
