# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Canonical design documents and current GitHub `main` remain authoritative.

## Last recovered baseline

- Remote `main` inspected at the start of this batch: `f8863fb338735f8a60487b98ba205872fa391793` (unrelated TURNBOUND: RE documentation commit at the repository head).
- Prior Riftfrontier content-contract verification was completed successfully by `Build Riftfrontier` run `34175769123` for commit `19237976701c1a3db36a443efbe54e56cd1976f0`.
- This batch preserved M2 field-play-gated balance values and did not modify other projects.

## Completed in this batch

M3 authoritative attack-runtime foundation:

- Added `AttackTimeline`, which samples `TELEGRAPH -> ACTIVE -> RECOVERY -> COMPLETE` directly from one `CoreDefinition.AttackPattern` cadence.
- Added immutable `AttackExecution`, binding runtime start tick to that shared timeline without copying phase durations.
- Hit-volume permission and presentation phase now derive from the same runtime sample (`mayApplyHit()` / `presentationPhase()`).
- Added `CombatRuntimeCatalog`, a read-only bridge from published `ContentLookup` snapshots to attack execution and boss attack timelines.
- Boss attack-pattern references resolve in stable `ContentId` order, while actual authored attack-selection policy remains intentionally separate.
- Added server-thread-owned `AttackStateMachine` with overlap rejection, phase-boundary reporting, one-shot completion, and explicit cancel/interruption boundary for future stun/death/despawn policy.
- Added JUnit coverage for exact phase boundaries, zero-recovery cadence, hit-window alignment, time-travel rejection, fail-closed missing content, deterministic boss pattern resolution, overlap rejection, cancellation, and one-shot completion.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/AttackTimeline.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/AttackExecution.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/CombatRuntimeCatalog.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/AttackStateMachine.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/AttackExecutionTest.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/CombatRuntimeCatalogTest.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/AttackStateMachineTest.java`

## Verification

- Current code commit before this handoff document: `ed740cc304c6b2cf8c99d61d9d630ef09a0476d5`.
- `Build Riftfrontier` run `34178875657` targets exactly that commit.
- `Verify toolchain`: SUCCESS.
- `Tests and clean build`: SUCCESS.
- Required `Riftfrontier GameTest gate`: SUCCESS.
- At the last observation during this handoff write, `Dedicated server smoke` was still IN PROGRESS; client smoke, executable JAR inspection, build report and artifact upload were still pending. Full workflow SUCCESS is therefore NOT YET VERIFIED here.
- Actual Minecraft field play: NOT TESTED.
- Minecraft-side production hit-volume/damage adapter: NOT IMPLEMENTED / NOT TESTED.
- Final model/animation/VFX/sound presentation: NOT DESIGNED / NOT TESTED by design gate.

## Do not repeat or revert

- Do not rebuild M2 extraction/restart/evidence infrastructure.
- Do not tune M2 pressure scaling or patrol bonus without human field-play evidence.
- Do not rename M2 Zombie/Skeleton/Ravager technical proxies into production creatures.
- Preserve `AttackPattern` as the single authoritative timing source; do not introduce separate animation, hitbox, damage or VFX timing constants.
- Preserve fail-closed runtime content lookup and server-owned attack lifecycle.
- Do not invent final Region 01 boss art/VFX/sound before the presentation asset/reference gate.
- Do not move Gson back to `testRuntimeOnly` while tests compile against the codec API.

## Exact next start point

1. Check latest remote `main`, then finish verification of `Build Riftfrontier` run `34178875657` or the latest exact equivalent if repository activity superseded it. If any remaining gate fails, fix the first actual failure without weakening the attack runtime.
2. Once the full build gate is green, connect `AttackStateMachine` to a Minecraft-side server combat adapter with an explicit delivery/hit-volume contract. Actual damage must be possible only while the shared timeline reports `ACTIVE`.
3. Add native GameTest coverage for a minimal technical attack adapter proving: telegraph cannot damage, active can damage, recovery cannot damage, cancellation closes the hit window, and completion cannot apply a late hit. This is runtime verification, not final boss art.
4. After hit-volume semantics are stable, add boss phase/attack-selection runtime that consumes `BossProfile` references without duplicating attack cadence. Presentation adapters must consume the same sampled phase/progress/presentation cue.
5. Keep M2 numerical tuning and final Region 01 production boss assets gated on actual field-play/reference evidence.
