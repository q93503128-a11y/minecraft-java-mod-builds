# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Run-start remote `main`: `991175056682b09ceb2887980db598d0bf17e5f9`.
- Baseline `Build Riftfrontier` run `34513223693`, HEAD `991175056682b09ceb2887980db598d0bf17e5f9`: completed `SUCCESS`.
- No approved Region 01 final boss profile/source/asset input was found in the recovered canonical/runtime state.
- No approved production weapon ItemStack provisioning identity, concrete client control mapping, shape-specific hit geometry, damage/range/resource policy, or final presentation input was found; none was invented.

## Completed in this batch

M3 validated boss capability ownership across entity unload/remove:

- Runtime commit: `12235932769004a8a6ef72758abe28ca1142c23a`.
- `MinecraftBossCombatAdapter.ValidatedRuntime` is now permanently bound, on its first Minecraft-facing begin/tick, to the exact eligible `LivingEntity` instance and server dimension that owns it.
- A different entity instance cannot adopt the retained capability after the first attack finishes or is cancelled; wrong-owner access fails closed without rebinding ownership.
- Added an identity-keyed process-local owner index only for event-driven lifecycle retirement. It is not gameplay state and does not replace server/world authority.
- `EntityLeaveLevelEvent` on the main NeoForge event bus removes the exact owner index entry and irreversibly invalidates every validated runtime bound to that entity instance; any active boss lifecycle/damage execution is cancelled.
- Once invalidated by owner leave, semantic/runtime reads, attack begin/tick and phase transitions fail closed even if a caller still retains the Java object or presents the former owner instance.
- Explicit cancellation remains callable after invalidation so cleanup cannot be blocked.
- Added required native `boss_runtime_owner_lifecycle` GameTest covering first-owner binding, replacement rejection after cancellation, owner-leave invalidation, non-resurrection, and clean creation for a replacement actor.
- Added a pure-Java structural contract guarding exact-owner fields, owner gate, leave-event invalidation and production event-bus registration.
- No boss identity, art, controls, ItemStack provisioning, production geometry, damage/range/resource values, cadence tuning, model, animation, VFX or sound was added.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftBossCombatAdapter.java`
- `src/main/java/kr/moonseungjun/riftfrontier/Riftfrontier.java`
- `src/main/java/kr/moonseungjun/riftfrontier/gametest/BossRuntimeLifecycleGameTests.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/BossRuntimeLifecycleContractTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Push preflight verified remote `main` was still `991175056682b09ceb2887980db598d0bf17e5f9`; runtime commit was a normal non-force descendant.
- `Build Riftfrontier` run `34518190556`, HEAD `12235932769004a8a6ef72758abe28ca1142c23a`: completed `SUCCESS`.
- Verified successful steps: Java/Gradle setup + toolchain, asset-intake tests, JUnit + clean build, required native GameTest gate, dedicated-server smoke, Xvfb client smoke, executable JAR inspection, build report, deliverable upload and log/report upload.
- Local Gradle: NOT RUN; executable validation source was GitHub Actions.
- Human field play, multiplayer latency/feel, concrete player controls, production boss/player hit geometry/damage/resource policy, final player/boss presentation and real production boss unload/reload during an encounter: NOT TESTED / NOT APPROVED.

## Do not repeat or revert

- M2 persistence/restart/Region 01 technical-proxy lifecycle work is DONE.
- Boss presentation/network semantic capability plumbing is DONE; do not add more presentation plumbing without approved real assets.
- Player `weapon_family` / `weapon_module` schema, reference role lock, two-family production graph, server-owned ItemStack loadout component, move-id-only serverbound intent and generation-aware runtime are DONE.
- Player sessions are exact-actor-instance + dimension + loadout scoped and alive/non-removed/non-spectator gated. Do not restore UUID-only or cross-dimension continuity.
- Shared Minecraft combat actor/target admission rejects wrong-level/dead/removed/spectator authority before hit resolution. Do not let future shape resolvers bypass it.
- Generic Minecraft attack executions and Minecraft-facing boss attack starts remain exact-actor-instance + dimension bound.
- `AttackStateMachine` remains the single monotonic authoritative `telegraph -> ACTIVE -> recovery` clock. Do not add parallel timers or raw gameplay sampling bypasses.
- Validated boss runtimes derived from a published snapshot remain generation-bound. Do not permit them to continue across successful content publication changes.
- Validated Minecraft boss capabilities are now exact entity-instance + dimension lifetime-bound and are irreversibly invalidated by `EntityLeaveLevelEvent`; do not restore post-unload/replacement adoption.
- Provisional attack ticks are not final balance. Do not invent blocked damage/range/resource/geometry/control/art values.

## Exact next start point

1. Re-check current remote `main`, then recover the final CI conclusion for this handoff descendant and any newer Riftfrontier run; repair the first actual failing gate if needed.
2. Re-check approved Region 01 boss source/profile/asset input and approved player ItemStack provisioning/control mapping. Use existing gates only if legitimate new input exists.
3. If still absent, do not repeat generation, owner-lifetime, actor-instance, eligibility, target-admission, dimension, loadout or attack-clock work. Audit whether more than one independently mutable validated boss capability can be bound to the same still-live boss entity and thereby create divergent parallel phase/attack state. If the production architecture already prevents that, document the evidence and select the next objectively testable M3 server-authority integration gap instead.
4. Keep shape-specific production hit volumes, damage/range/resource policy, timing tuning, final ItemStack/model/animation/VFX/sound and multiplayer feel blocked on evidence/approval.
