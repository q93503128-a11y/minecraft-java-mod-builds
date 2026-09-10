# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Run-start remote `main`: `3ce2f980fbc38191fb05732001ebc5402f041125`.
- Baseline `Build Riftfrontier` run `34518993464`, HEAD `3ce2f980fbc38191fb05732001ebc5402f041125`: completed `SUCCESS`.
- No approved Region 01 final boss profile/source/asset input was found in the recovered canonical/runtime state.
- No approved production weapon ItemStack provisioning identity, concrete client control mapping, shape-specific hit geometry, damage/range/resource policy, or final presentation input was found; none was invented.

## Completed in this batch

M3 single authoritative validated boss runtime per live entity:

- Runtime commit: `98b7bb4464320385f3be113f9475375184ba02db`.
- Audited the owner index and confirmed the previous `LivingEntity -> Set<ValidatedRuntime>` admission path allowed more than one independently mutable validated boss capability to bind to the same still-live boss entity.
- `bindValidatedRuntimeOwner(...)` now enforces a single claimed validated runtime per exact live `LivingEntity` instance. A second distinct runtime fails closed before it records the owner binding or starts Minecraft-facing combat.
- Repeated use by the already-authoritative runtime remains valid.
- A rejected duplicate runtime stays unbound, so it may later be legitimately bound to a different eligible actor; duplicate rejection does not poison the candidate capability.
- Owner-leave invalidation remains event-driven and removes the exact owner entry before invalidating the authoritative runtime. No global or per-tick world/entity scan was introduced.
- Expanded required native `boss_runtime_owner_lifecycle` GameTest to prove duplicate-owner rejection, preservation of the existing authoritative runtime, and clean later binding of the rejected candidate to a different actor.
- Expanded pure-Java source contract to lock the single-runtime owner admission and claim-before-bind ordering.
- No boss identity, art, controls, ItemStack provisioning, production geometry, damage/range/resource values, cadence tuning, model, animation, VFX or sound was added.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftBossCombatAdapter.java`
- `src/main/java/kr/moonseungjun/riftfrontier/gametest/BossRuntimeLifecycleGameTests.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/BossRuntimeLifecycleContractTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Push preflight verified remote `main` was still `3ce2f980fbc38191fb05732001ebc5402f041125`; runtime commit was a normal non-force descendant.
- `Build Riftfrontier` run `34524065826`, HEAD `98b7bb4464320385f3be113f9475375184ba02db`: at handoff update time, Java/Gradle setup + toolchain, asset-intake tests, JUnit + clean build, and required native GameTest gate were all `SUCCESS`.
- The same run's dedicated-server smoke was still `IN PROGRESS`; client smoke, executable JAR inspection, report and artifact upload were therefore `NOT YET VERIFIED` at handoff update time. Do not promote this run to full success without re-checking its final conclusion.
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
- One live boss entity may now own only one independently mutable validated Minecraft boss combat runtime. Do not restore multi-capability parallel phase/attack state for the same actor.
- Provisional attack ticks are not final balance. Do not invent blocked damage/range/resource/geometry/control/art values.

## Exact next start point

1. Re-check current remote `main`, then recover the final conclusion of `Build Riftfrontier` run `34524065826` and any newer Riftfrontier run; repair the first actual failing gate if needed.
2. Re-check approved Region 01 boss source/profile/asset input and approved player ItemStack provisioning/control mapping. Use existing gates only if legitimate new input exists.
3. If still absent, do not repeat generation, owner-lifetime, single-owner-capability, actor-instance, eligibility, target-admission, dimension, loadout or attack-clock work. Audit the next objectively testable M3 server-authority integration gap, prioritizing lifecycle/state divergence that can create real gameplay authority conflicts without inventing blocked production balance or presentation.
4. Keep shape-specific production hit volumes, damage/range/resource policy, timing tuning, final ItemStack/model/animation/VFX/sound and multiplayer feel blocked on evidence/approval.
