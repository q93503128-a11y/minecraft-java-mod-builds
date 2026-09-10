# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Run-start remote `main`: `daf803758a48f113d8d8b2a0947b499daca996f4`.
- That HEAD was newer than the previous handoff and already bound generic `MinecraftAttackAdapter` executions to the exact attacker instance + authoritative dimension instead of UUID/first-candidate continuity.
- No approved Region 01 final boss profile/source/asset input was found in the recovered canonical/runtime state or repository search.
- No approved production weapon ItemStack provisioning identity, concrete client control mapping, shape-specific hit geometry, damage/range/resource policy, or final presentation input was found; none was invented.

## Completed in this batch

M3 Minecraft boss attack-start actor authority boundary:

- Runtime commit: `ab5bf7e98bcda9f95f5bc20106709abec4ef24a7`.
- Added Minecraft-facing `MinecraftBossCombatAdapter.beginNextAttack(ServerLevel, LivingEntity, long)` so an attack is admitted and bound to one eligible server actor + dimension before its first authoritative tick.
- Dead/removed/spectator/wrong-level boss actors cannot create a lifecycle/damage execution through the Minecraft-facing begin path; failed admission leaves both clocks closed.
- The validated boss runtime exposes the same actor-bound begin and still checks the selected attack against the validated semantic phase pool.
- Existing low-level `beginNextAttack(long)` remains only as an API-free/test adapter boundary; Minecraft-facing runtime should not use it to defer actor ownership until first tick.
- Extended required `combat_authority_boundary` native GameTest coverage for invalid boss rejection at begin, actor-bound TELEGRAPH start, and replacement-actor rejection before ACTIVE damage resolution.
- No production boss identity, geometry, damage/range values, cadence tuning, controls, ItemStack provisioning, model, animation, VFX, or sound was added.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftBossCombatAdapter.java`
- `src/main/java/kr/moonseungjun/riftfrontier/gametest/CombatAuthorityGameTests.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Push preflight verified remote `main` still at `daf803758a48f113d8d8b2a0947b499daca996f4`; runtime commit was a normal non-force descendant.
- `Build Riftfrontier` run `34506852195`, HEAD `ab5bf7e98bcda9f95f5bc20106709abec4ef24a7`: completed `SUCCESS`.
- Verified successful steps: Java/Gradle setup + toolchain, asset-intake tests, JUnit + clean build, required native GameTest gate, dedicated-server smoke, Xvfb client smoke, executable JAR inspection, build report, deliverable/log uploads.
- Local Gradle: NOT RUN; executable validation source was GitHub Actions.
- Human field play, multiplayer latency/feel, concrete player controls, production boss/player hit geometry/damage/resource policy, and final player/boss presentation: NOT TESTED / NOT APPROVED.

## Do not repeat or revert

- M2 persistence/restart/Region 01 technical-proxy lifecycle work is DONE.
- Boss presentation/network semantic capability plumbing is DONE; do not add more presentation plumbing without approved real assets.
- Player `weapon_family` / `weapon_module` schema, reference role lock, two-family production graph, server-owned ItemStack loadout component, move-id-only serverbound intent, generation-aware runtime are DONE.
- Player sessions are exact-actor-instance + dimension + loadout scoped and alive/non-removed/non-spectator gated. Do not restore UUID-only or cross-dimension continuity.
- Shared Minecraft combat actor/target admission rejects wrong-level/dead/removed/spectator authority before hit resolution. Do not let future shape resolvers bypass it.
- Generic Minecraft attack executions are exact-actor-instance + dimension bound. Minecraft-facing boss attacks are now also actor-bound at begin; do not restore a path where the first later tick chooses the boss execution owner.
- `AttackStateMachine` remains the single monotonic authoritative `telegraph -> ACTIVE -> recovery` clock. Do not add parallel timers or raw gameplay sampling bypasses.
- Provisional attack ticks are not final balance. Do not invent blocked damage/range/resource/geometry/control/art values.

## Exact next start point

1. Re-check current remote `main`, then recover the final CI conclusion for this handoff descendant and any newer Riftfrontier run; repair the first actual failing gate if needed.
2. Re-check approved Region 01 boss source/profile/asset input and approved player ItemStack provisioning/control mapping. Use existing gates only if legitimate new input exists.
3. If still absent, inspect boss combat content-generation ownership/stale-runtime invalidation: determine whether a validated boss runtime can continue after the published combat catalog/semantic generation changes. If an actual stale-generation authority gap exists, bind boss runtime lifetime to generation and fail closed with executable regression coverage; if existing code already closes it, select the next objective M3 runtime integration gap instead.
4. Do not repeat actor identity/start, combat eligibility, target admission, dimension, player loadout, or attack-clock monotonicity work.
5. Keep shape-specific production hit volumes, damage/range/resource policy, timing tuning, final ItemStack/model/animation/VFX/sound and multiplayer feel blocked on evidence/approval.
