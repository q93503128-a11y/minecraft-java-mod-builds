# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Run-start remote `main`: `b64b2ac5ff23aa0cc6b26ccd9516defe3c861b5f`.
- Previous runtime `Build Riftfrontier` run `34524065826`, HEAD `98b7bb4464320385f3be113f9475375184ba02db`: re-checked and completed `SUCCESS`.
- No approved Region 01 final boss profile/source/asset input was found in the recovered canonical/runtime state.
- No approved production weapon ItemStack provisioning identity, concrete client control mapping, shape-specific hit geometry, damage/range/resource policy, or final presentation input was found; none was invented.

## Completed in this batch

M3 Minecraft-bound validated boss owner-context mutation sealing:

- Code/test descendant HEAD before handoff: `157fd390f091cd3be603ac63a9a1c49fbbc83308`.
- Audited `ValidatedRuntime` after exact actor/dimension binding and found two ownerless mutation paths: `beginNextAttack(long)` could restart attack state and `transitionToPhase(int)` could change authoritative boss phase without re-validating the bound actor/level.
- Once a validated runtime is Minecraft-bound, both detached mutation paths now fail closed. Ownerless begin/phase mutation cannot bypass exact-actor, dimension, or current combat-eligibility checks.
- Added `transitionToPhase(ServerLevel, LivingEntity, int)` as the Minecraft-authoritative phase mutation path; it crosses the same `requireMinecraftOwner(...)` gate as Minecraft-facing begin/tick.
- Detached/runtime-test APIs remain usable before Minecraft ownership is established, preserving API-free controller tests without allowing them to mutate a live bound capability.
- Rejected ownerless/wrong-owner phase transitions preserve the authoritative phase. Wrong-owner rejection does not transfer ownership.
- Expanded required native `boss_runtime_owner_lifecycle` GameTest to cover ownerless attack restart rejection, owner-aware phase transition, ownerless phase mutation rejection, wrong-owner phase mutation rejection, and continued authority of the original owner.
- Expanded pure-Java source contract to lock the owner-context mutation boundary.
- No boss identity, art, controls, ItemStack provisioning, production geometry, damage/range/resource values, cadence tuning, model, animation, VFX or sound was added.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftBossCombatAdapter.java`
- `src/main/java/kr/moonseungjun/riftfrontier/gametest/BossRuntimeLifecycleGameTests.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/BossRuntimeLifecycleContractTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Previous `Build Riftfrontier` run `34524065826` was re-checked as final `SUCCESS` before this batch.
- New `Build Riftfrontier` run `34530007501`, HEAD `157fd390f091cd3be603ac63a9a1c49fbbc83308`: `IN PROGRESS` at handoff update time. Do not claim any new clean-build/GameTest/server/client/JAR gate as successful until its final result is re-checked.
- Local Gradle: NOT RUN because the execution container could not resolve `github.com`; executable validation source is GitHub Actions.
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
- Validated Minecraft boss capabilities remain exact entity-instance + dimension lifetime-bound and irreversibly invalidated by `EntityLeaveLevelEvent`.
- One live boss entity may own only one independently mutable validated Minecraft boss combat runtime.
- Once that runtime is Minecraft-bound, attack-start and phase mutations must not use ownerless APIs; phase mutation requires the exact authoritative `ServerLevel + LivingEntity` context.
- Provisional attack ticks are not final balance. Do not invent blocked damage/range/resource/geometry/control/art values.

## Exact next start point

1. Re-check current remote `main`, then recover the final conclusion of `Build Riftfrontier` run `34530007501` and any newer Riftfrontier descendant; repair the first actual failing gate if needed.
2. Re-check approved Region 01 boss source/profile/asset input and approved player ItemStack provisioning/control mapping. Use existing gates only if legitimate new input exists.
3. If still absent, do not repeat generation, owner-lifetime, single-owner-capability, owner-context mutation, actor-instance, eligibility, target-admission, dimension, loadout or attack-clock work. Audit whether a stale generation-bound `ValidatedRuntime` can remain in `VALIDATED_RUNTIMES_BY_OWNER` after successful content publication and block a fresh-generation runtime from claiming the same still-live entity. If confirmed, close that generation/owner-index retirement conflict with executable regression coverage rather than weakening singleton ownership.
4. If that path is already safe, move to the next objectively testable M3 authority/state divergence.
5. Keep shape-specific production hit volumes, damage/range/resource policy, timing tuning, final ItemStack/model/animation/VFX/sound and multiplayer feel blocked on evidence/approval.
