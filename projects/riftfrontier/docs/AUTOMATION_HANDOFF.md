# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Run-start remote `main`: `3c26fd89b2e3aaf28f3e2b06b7e11bd99930e7c7`.
- Baseline `Build Riftfrontier` run `34507851538`, HEAD `3c26fd89b2e3aaf28f3e2b06b7e11bd99930e7c7`: completed `SUCCESS`.
- No approved Region 01 final boss profile/source/asset input was found in the recovered canonical/runtime state.
- No approved production weapon ItemStack provisioning identity, concrete client control mapping, shape-specific hit geometry, damage/range/resource policy, or final presentation input was found; none was invented.

## Completed in this batch

M3 published-content generation ownership for validated boss combat runtime:

- Runtime commit: `12f6e4e5adf9dd76606a8086791505590833ef9f`.
- `CombatRuntimeCatalog` now preserves the generation when it is built directly from an immutable published `ContentRuntimeSnapshot`; detached fixture registries deliberately carry no published-generation claim.
- Added `PublishedContentGenerationGuard`, an immutable authority token that compares the source generation against the currently published `ContentRuntime` generation.
- `MinecraftBossCombatAdapter.ValidatedRuntime` inherits that token. Before validated semantic/runtime state is read or authoritative begin/tick/phase-transition work occurs, the runtime verifies that its published generation is still current.
- A successful content reload to another generation or loss of the published runtime makes the old validated boss capability fail closed. Stale detection cancels both boss lifecycle and Minecraft damage execution before the guarded operation can proceed.
- Explicit cancellation remains available after invalidation so cleanup can never be blocked.
- Detached unit/native GameTest catalogs remain usable as technical fixtures but are not marked as published generation owners.
- Added executable pure-Java regression coverage for same-generation acceptance, changed/missing-generation rejection, detached-catalog behavior and invalid generation construction.
- Extended the production runtime structural contract so future edits cannot silently remove generation inheritance or fail-closed cancellation.
- No boss identity, attack geometry, damage/range/resource values, cadence tuning, controls, ItemStack provisioning, model, animation, VFX or sound was added.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/CombatRuntimeCatalog.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/PublishedContentGenerationGuard.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftBossCombatAdapter.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/PublishedContentGenerationGuardTest.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/MinecraftBossValidatedRuntimeTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Push preflight verified remote `main` was still `3c26fd89b2e3aaf28f3e2b06b7e11bd99930e7c7`; runtime commit was a normal non-force descendant.
- `Build Riftfrontier` run `34512442809`, HEAD `12f6e4e5adf9dd76606a8086791505590833ef9f`: completed `SUCCESS`.
- Verified successful steps: Java/Gradle setup + toolchain, asset-intake tests, JUnit + clean build, required native GameTest gate, dedicated-server smoke, Xvfb client smoke, executable JAR inspection, build report, deliverable upload and log/report upload.
- Local Gradle: NOT RUN; executable validation source was GitHub Actions.
- Human field play, multiplayer latency/feel, concrete player controls, production boss/player hit geometry/damage/resource policy, final player/boss presentation and reload behavior during a live production boss encounter: NOT TESTED / NOT APPROVED.

## Do not repeat or revert

- M2 persistence/restart/Region 01 technical-proxy lifecycle work is DONE.
- Boss presentation/network semantic capability plumbing is DONE; do not add more presentation plumbing without approved real assets.
- Player `weapon_family` / `weapon_module` schema, reference role lock, two-family production graph, server-owned ItemStack loadout component, move-id-only serverbound intent and generation-aware runtime are DONE.
- Player sessions are exact-actor-instance + dimension + loadout scoped and alive/non-removed/non-spectator gated. Do not restore UUID-only or cross-dimension continuity.
- Shared Minecraft combat actor/target admission rejects wrong-level/dead/removed/spectator authority before hit resolution. Do not let future shape resolvers bypass it.
- Generic Minecraft attack executions and Minecraft-facing boss attack starts remain exact-actor-instance + dimension bound.
- `AttackStateMachine` remains the single monotonic authoritative `telegraph -> ACTIVE -> recovery` clock. Do not add parallel timers or raw gameplay sampling bypasses.
- Validated boss runtimes derived from a published snapshot are now generation-bound. Do not permit them to continue across successful content publication changes, and do not treat detached fixture catalogs as published runtime state.
- Provisional attack ticks are not final balance. Do not invent blocked damage/range/resource/geometry/control/art values.

## Exact next start point

1. Re-check current remote `main`, then recover the final CI conclusion for this handoff descendant and any newer Riftfrontier run; repair the first actual failing gate if needed.
2. Re-check approved Region 01 boss source/profile/asset input and approved player ItemStack provisioning/control mapping. Use existing gates only if legitimate new input exists.
3. If still absent, audit the next objective M3 server-runtime boundary without repeating actor identity, combat eligibility, target admission, dimension, player loadout, attack-clock monotonicity or content-generation invalidation. Prioritize whether authoritative boss lifecycle ownership is safely closed when its owning entity is unloaded/removed between event-driven updates and whether any retained process-local boss capability can survive that lifecycle boundary.
4. If that boundary is already closed, select the next objectively testable M3 server-authority integration gap rather than adding observation-only infrastructure.
5. Keep shape-specific production hit volumes, damage/range/resource policy, timing tuning, final ItemStack/model/animation/VFX/sound and multiplayer feel blocked on evidence/approval.
