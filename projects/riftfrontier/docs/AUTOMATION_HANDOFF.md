# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Canonical design documents and current GitHub `main` remain authoritative.

## Last recovered baseline

- Remote `main` was re-read from current GitHub state before implementation; unrelated projects may advance repository HEAD between Riftfrontier commits.
- The previous handoff was stale relative to current `main`: `feeacd1ca219b2bb63a9dc83c65f4f4c65d9622a` already connected `BossCombatController` to the Minecraft damage runtime, and `4abb10cc563b9ee1180773f5374e239b7c79274d` added the native entity-backed phase/damage regression.
- This batch therefore did not rebuild that adapter or repeat its GameTest.
- M2 field-play-gated balance values and all non-Riftfrontier project folders were intentionally left untouched.

## Completed in this batch

M3 authoritative boss presentation sampling boundary:

- Extended `MinecraftBossCombatAdapter` so boss lifecycle, Minecraft damage and presentation consumers are all derived from the same `AttackExecution.Snapshot` / `AttackTimeline` sample.
- Added immutable `PresentationFrame` with authoritative boss phase, pattern ID, attack phase, normalized phase progress, presentation cue, delivery semantic, counterplay set and hit-window state.
- Presentation frame construction copies semantic metadata from the authored `AttackPattern`; it adds no animation, VFX, sound or hitbox cadence constants.
- `TickResult` now carries an optional presentation frame. Idle/cancelled/phase-transition state exposes no stale frame.
- Runtime invariants fail closed if presentation phase/hit-window state diverges from the Minecraft damage adapter.
- Added JUnit coverage proving TELEGRAPH/ACTIVE/RECOVERY phase progress and cues are inherited from the authoritative snapshot and rejecting impossible presentation timing states.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftBossCombatAdapter.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/MinecraftBossPresentationContractTest.java`

## Verification

- Presentation implementation commit: `cbbaa99ae50c34a1e834b44a5e324edaa16d01e6`.
- Presentation contract test commit: `e681b41f962fda2ca19d43028209f3cdda89adff`.
- `Build Riftfrontier` run `34189760239` targets exactly `e681b41f962fda2ca19d43028209f3cdda89adff` and was `IN PROGRESS` when this handoff was written.
- Do not claim the new presentation contract fully CI-verified until that run finishes all clean/unit, required native GameTest, dedicated server, Xvfb client and executable-JAR gates.
- Existing entity-backed boss phase/damage GameTest is present on current main from `4abb10cc563b9ee1180773f5374e239b7c79274d`.
- Actual human Minecraft field play: NOT TESTED.
- Final model/animation/VFX/sound presentation: NOT DESIGNED / NOT TESTED by design gate.

## Do not repeat or revert

- Do not rebuild M2 extraction/restart/evidence infrastructure.
- Do not tune M2 pressure scaling or patrol bonus without human field-play evidence.
- Do not rename M2 Zombie/Skeleton/Ravager technical proxies into production creatures.
- Preserve `AttackPattern` as the single authoritative timing source; do not introduce separate boss, animation, hitbox, damage, VFX or sound cadence constants.
- Preserve `AttackStateMachine` / `MinecraftAttackAdapter` ACTIVE-only damage and per-execution target deduplication.
- Preserve `BossAttackSelectionPolicy` as the selection seam and server-owned boss lifecycle.
- Preserve `MinecraftBossCombatAdapter` phase-transition cancellation of both lifecycle and damage states.
- Presentation adapters must consume `PresentationFrame`/the same authoritative snapshot instead of starting independent timers.
- Do not invent final Region 01 boss art/VFX/sound before the reference/asset gate.

## Exact next start point

1. Check latest remote `main`, then inspect `Build Riftfrontier` run `34189760239`. If it failed, fix the first actual compiler/test/runtime failure without weakening the shared timing or fail-closed presentation invariants.
2. If the full gate is green, add the next production-facing presentation seam without final art: a server-to-client semantic sync/presentation resolver that carries `PresentationFrame` state or an equivalent stable payload, with no duplicated timing constants and no visual style invention.
3. Before choosing GeckoLib/model/VFX/audio assets, re-read `REFERENCE_TARGETS.md` and the M3 combat reference dossier and perform the required reference/asset compatibility gate.
4. Keep M2 numerical tuning and final Region 01 production boss assets gated on actual field-play/reference evidence.
