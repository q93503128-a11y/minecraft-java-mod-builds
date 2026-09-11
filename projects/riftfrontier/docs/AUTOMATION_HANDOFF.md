# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Run-start remote `main`: `f98ae05f5a9512530a22ad5c654fe47dbc569470`.
- Latest recovered `Build Riftfrontier` run before this batch: `34540928892`, HEAD `f98ae05f5a9512530a22ad5c654fe47dbc569470`: final `SUCCESS`.
- No approved Region 01 final boss profile/source/asset input was found in the recovered canonical/runtime state.
- No approved production weapon ItemStack provisioning identity, concrete client control mapping, shape-specific hit geometry, damage/range/resource policy, or final presentation input was found; none was invented.

## Completed in this batch

M3 player-weapon published-generation retirement boundary:

- Code/test HEAD before handoff: `3e987dbc8eab31763deb74f0e7735fdbe8ab186c`.
- Found a real authority divergence: `MinecraftPlayerWeaponCombatAdapter` retained immutable player weapon controllers from its original `CombatRuntimeCatalog`, but unlike validated boss runtime it had no published-generation guard. A successful content reload could therefore leave the same actor/loadout ID executing an attack timeline/profile from the previous authoritative snapshot.
- The adapter now inherits `PublishedContentGenerationGuard` from a published catalog.
- New move intents fail closed on a stale catalog and clear any retained actor execution.
- Active player sessions cancel/discard before attack-clock advancement when publication generation changes.
- `recovery_pivot` authorization is removed immediately for stale publication state.
- Session creation re-checks publication after assembling the immutable weapon profile so a reload cannot publish a stale replacement session between intent admission and session installation.
- Detached test/fixture catalogs still have no generation guard and preserve their existing technical-test behavior.
- Added a pure-Java structural contract test locking these production invariants without loading Minecraft runtime classes.
- No weapon balance, controls, ItemStack identity, hit geometry, damage/range/resource values, model, animation, VFX or sound was invented.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftPlayerWeaponCombatAdapter.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/MinecraftPlayerWeaponGenerationContractTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Pre-batch descendant `Build Riftfrontier` run `34540928892`, HEAD `f98ae05f5a9512530a22ad5c654fe47dbc569470`: final `SUCCESS`.
- Current code/test `Build Riftfrontier` run `34544929333`, HEAD `3e987dbc8eab31763deb74f0e7735fdbe8ab186c`: `IN PROGRESS` at handoff update; do not claim success until final conclusion is observed.
- Local Gradle: NOT RUN; this environment has no direct GitHub network checkout, so executable validation remains GitHub Actions.
- Human field play, multiplayer latency/feel, concrete player controls, production boss/player hit geometry/damage/resource policy, final player/boss presentation and real production reload during an active player attack: NOT TESTED / NOT APPROVED.

## Do not repeat or revert

- M2 persistence/restart/Region 01 technical-proxy lifecycle work is DONE.
- Boss presentation/network semantic capability plumbing is DONE; do not add more presentation plumbing without approved real assets.
- Player `weapon_family` / `weapon_module` schema, reference role lock, two-family production graph, server-owned ItemStack loadout component and move-id-only serverbound intent are DONE.
- Player sessions are exact-actor-instance + dimension + loadout scoped, alive/non-removed/non-spectator gated, and now published-generation scoped. Do not restore stale-catalog continuity after content publication changes.
- Shared Minecraft combat actor/target admission rejects wrong-level/dead/removed/spectator authority before hit resolution. Do not let future shape resolvers bypass it.
- Generic Minecraft attack executions and Minecraft-facing boss attack starts remain exact-actor-instance + dimension bound.
- `AttackStateMachine` remains the single monotonic authoritative `telegraph -> ACTIVE -> recovery` clock. Do not add parallel timers or raw gameplay sampling bypasses.
- Validated boss runtimes derived from a published snapshot remain generation-bound. Stale generations relinquish their exact process-local owner claim, become irreversibly retired, and cannot resume or rebind.
- Validated Minecraft boss capabilities remain exact entity-instance + dimension lifetime-bound and irreversibly invalidated by `EntityLeaveLevelEvent`.
- One live boss entity may own only one current-generation independently mutable validated Minecraft boss combat runtime.
- Once that runtime is Minecraft-bound, attack-start and phase mutations must not use ownerless APIs; phase mutation requires the exact authoritative `ServerLevel + LivingEntity` context.
- Provisional attack ticks are not final balance. Do not invent blocked damage/range/resource/geometry/control/art values.

## Exact next start point

1. Re-check current remote `main` and final status of run `34544929333` plus any newer Riftfrontier descendant CI. If it failed, fix the first actual failing gate without weakening production authority.
2. Re-check approved Region 01 boss source/profile/asset input and approved player ItemStack provisioning/control mapping. Use existing gates only if legitimate new input exists.
3. If still absent and CI is green, do not repeat boss/player generation retirement, owner lifetime, singleton claim, owner-context mutation, actor-instance, eligibility, target-admission, dimension, loadout or attack-clock work. Audit the next objectively testable M3 authority/state divergence, prioritizing event-driven lifecycle edges where process-local player/boss combat state can survive authoritative Minecraft state changes.
4. Keep shape-specific production hit volumes, damage/range/resource policy, timing tuning, final ItemStack/model/animation/VFX/sound and multiplayer feel blocked on evidence/approval.
