# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Run-start remote `main`: `f4a5d976678b8d2708b2c3394bac969419cd7f45`.
- Previous `Build Riftfrontier` run `34530007501`, HEAD `157fd390f091cd3be603ac63a9a1c49fbbc83308`: re-checked and completed `SUCCESS`.
- No approved Region 01 final boss profile/source/asset input was found in the recovered canonical/runtime state.
- No approved production weapon ItemStack provisioning identity, concrete client control mapping, shape-specific hit geometry, damage/range/resource policy, or final presentation input was found; none was invented.

## Completed in this batch

M3 stale-generation validated boss owner-claim retirement:

- Code/test HEAD before handoff: `db959f6b32e40eef19b3a8c7bcc7a8674aef3651`.
- Confirmed a real generation/owner-index conflict: after successful content publication, an older generation-bound `ValidatedRuntime` could remain in `VALIDATED_RUNTIMES_BY_OWNER`; although its own API failed closed as stale, the retained owner slot could reject a fresh-generation runtime for the same still-live boss.
- Fresh owner claims now prune only stale-generation validated runtimes before enforcing singleton ownership. Current-generation duplicate runtimes are still rejected.
- A stale runtime that discovers publication mismatch through its own API now cancels combat, becomes irreversibly generation-retired, and releases only its own exact owner-map entry.
- Retired stale capabilities cannot rebind or resume later, while a fresh-generation runtime may claim the same live actor.
- Added non-throwing generation-current probing to `PublishedContentGenerationGuard` and a pure-Java executable regression for the generation boundary.
- Expanded the owner-lifecycle source contract to lock stale pruning, exact-entry release, irreversible retirement, and preserved singleton behavior.
- No boss identity, art, controls, ItemStack provisioning, production geometry, damage/range/resource values, cadence tuning, model, animation, VFX or sound was added.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftBossCombatAdapter.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/PublishedContentGenerationGuard.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/BossRuntimeLifecycleContractTest.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/PublishedContentGenerationGuardTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Previous `Build Riftfrontier` run `34530007501`: final `SUCCESS`.
- New `Build Riftfrontier` run `34535359597`, HEAD `db959f6b32e40eef19b3a8c7bcc7a8674aef3651`: `IN PROGRESS` at handoff update time. Do not claim clean build/GameTest/server/client/JAR success until its final result is re-checked.
- Local Gradle: NOT RUN because the execution container could not resolve `github.com`; executable validation source is GitHub Actions.
- Human field play, multiplayer latency/feel, concrete player controls, production boss/player hit geometry/damage/resource policy, final player/boss presentation and real production boss reload during an encounter: NOT TESTED / NOT APPROVED.

## Do not repeat or revert

- M2 persistence/restart/Region 01 technical-proxy lifecycle work is DONE.
- Boss presentation/network semantic capability plumbing is DONE; do not add more presentation plumbing without approved real assets.
- Player `weapon_family` / `weapon_module` schema, reference role lock, two-family production graph, server-owned ItemStack loadout component, move-id-only serverbound intent and generation-aware runtime are DONE.
- Player sessions are exact-actor-instance + dimension + loadout scoped and alive/non-removed/non-spectator gated. Do not restore UUID-only or cross-dimension continuity.
- Shared Minecraft combat actor/target admission rejects wrong-level/dead/removed/spectator authority before hit resolution. Do not let future shape resolvers bypass it.
- Generic Minecraft attack executions and Minecraft-facing boss attack starts remain exact-actor-instance + dimension bound.
- `AttackStateMachine` remains the single monotonic authoritative `telegraph -> ACTIVE -> recovery` clock. Do not add parallel timers or raw gameplay sampling bypasses.
- Validated boss runtimes derived from a published snapshot remain generation-bound and stale generations now relinquish their exact process-local owner claim. Do not restore stale owner-slot blocking.
- Validated Minecraft boss capabilities remain exact entity-instance + dimension lifetime-bound and irreversibly invalidated by `EntityLeaveLevelEvent`.
- One live boss entity may own only one current-generation independently mutable validated Minecraft boss combat runtime.
- Once that runtime is Minecraft-bound, attack-start and phase mutations must not use ownerless APIs; phase mutation requires the exact authoritative `ServerLevel + LivingEntity` context.
- Provisional attack ticks are not final balance. Do not invent blocked damage/range/resource/geometry/control/art values.

## Exact next start point

1. Re-check current remote `main`, then recover the final conclusion of `Build Riftfrontier` run `34535359597` and any newer Riftfrontier descendant; repair the first actual failing gate if needed.
2. Re-check approved Region 01 boss source/profile/asset input and approved player ItemStack provisioning/control mapping. Use existing gates only if legitimate new input exists.
3. If still absent, do not repeat generation retirement, owner lifetime, singleton claim, owner-context mutation, actor-instance, eligibility, target-admission, dimension, loadout or attack-clock work. Audit the next objectively testable M3 authority/state divergence, prioritizing lifecycle edges where process-local combat capability state can outlive or diverge from authoritative Minecraft/content state.
4. Keep shape-specific production hit volumes, damage/range/resource policy, timing tuning, final ItemStack/model/animation/VFX/sound and multiplayer feel blocked on evidence/approval.
