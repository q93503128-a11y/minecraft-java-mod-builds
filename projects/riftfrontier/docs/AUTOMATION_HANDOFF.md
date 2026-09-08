# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Canonical design documents and current GitHub `main` remain authoritative.

## Last recovered baseline

- Remote `main` inspected before implementation: `654ee1c9b7ef7bc46d06e4ccfaf313bec731a1e4` (unrelated earth-to-stars documentation commit at repository head).
- Prior Minecraft damage adapter commit `08fba9a415b4204949d832e2e126763bcf604105` was rechecked: `Build Riftfrontier` run `34182335245` completed with overall `success`.
- This batch preserved M2 field-play-gated balance values and did not modify other project folders.

## Completed in this batch

M3 server-authoritative boss phase / attack-selection runtime:

- Added `BossAttackSelectionPolicy` as an explicit deterministic policy boundary rather than hard-coding selection into boss lifecycle logic.
- Added baseline `deterministicRoundRobin()` policy over the boss profile's stable sorted attack IDs. Later phase weighting/cooldown logic can replace the policy without duplicating attack cadence.
- Selection context now carries boss ID, current phase, phase count, candidate attack IDs, completed attack count and last completed attack.
- Added `BossCombatController`, which resolves one validated `BossProfile` through `CombatRuntimeCatalog` and fails closed if its attack set cannot resolve.
- Boss lifecycle remains server-owned: begin, advance/completion accounting, explicit cancel and phase transition are owned by the controller.
- Phase transition always cancels an executing old-phase attack before changing phase, so an old ACTIVE hit window cannot survive the transition.
- Interrupted attacks do not increment completed-attack count or become `lastAttack`.
- Selection policies are restricted to the boss profile's validated candidate attack set; returning a foreign content ID fails closed.
- Added JUnit coverage for stable alpha→beta round-robin selection, overlap rejection, completion accounting, transition interruption, no reopened old attack after transition, cancel idempotence and invalid policy output.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/BossAttackSelectionPolicy.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/BossCombatController.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/BossCombatControllerTest.java`

## Verification

- Implementation commit: `dc52da5b50d942e46feadf4f2ec68779dbb2e82f`.
- `Build Riftfrontier` run `34185565033` targets exactly that implementation commit.
- At handoff write time the run is still `IN PROGRESS`; setup has started and clean build/unit/GameTest/server/client/JAR gates are not yet all complete.
- Therefore this batch does **not** claim the new boss controller is fully CI-verified yet.
- Previous Minecraft damage adapter run `34182335245`: overall `SUCCESS`.
- Actual human Minecraft field play: NOT TESTED.
- Final model/animation/VFX/sound presentation: NOT DESIGNED / NOT TESTED by design gate.

## Do not repeat or revert

- Do not rebuild M2 extraction/restart/evidence infrastructure.
- Do not tune M2 pressure scaling or patrol bonus without human field-play evidence.
- Do not rename M2 Zombie/Skeleton/Ravager technical proxies into production creatures.
- Preserve `AttackPattern` as the single authoritative timing source; do not introduce separate boss, animation, hitbox, damage or VFX cadence constants.
- Preserve `AttackStateMachine` / `MinecraftAttackAdapter` ACTIVE-only damage and per-execution target deduplication.
- Preserve fail-closed runtime content lookup and server-owned attack lifecycle.
- Preserve `BossAttackSelectionPolicy` as the selection seam; do not bury future weighting/cooldown rules inside `BossCombatController`.
- Phase transitions must cancel any executing old-phase attack before the new phase becomes authoritative.
- Do not invent final Region 01 boss art/VFX/sound before the presentation asset/reference gate.

## Exact next start point

1. Check latest remote `main`, then inspect `Build Riftfrontier` run `34185565033`. If it failed, fix the first actual compiler/test/runtime failure without weakening deterministic selection, profile membership checks, phase-transition cancellation or shared attack timing.
2. If the full gate is green, connect `BossCombatController` to a Minecraft server-owned boss adapter/lifecycle fixture. The adapter must use the existing `MinecraftAttackAdapter`/AttackStateMachine timing contract rather than create a second damage cadence.
3. Add a native GameTest proving a real entity-backed boss lifecycle cannot damage during telegraph/recovery, cannot preserve an old ACTIVE window across phase transition, and resumes attack selection under the new authoritative phase.
4. Presentation adapters must consume the same sampled phase/progress/presentation cue; do not create animation/VFX timers yet.
5. Keep M2 numerical tuning and final Region 01 production boss assets gated on actual field-play/reference evidence.
