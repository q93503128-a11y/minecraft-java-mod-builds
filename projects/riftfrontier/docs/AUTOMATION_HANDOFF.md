# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Canonical design documents and current GitHub `main` remain authoritative.

## Last recovered baseline

- Remote `main` inspected at the start of this batch: `451ef995b9bb592f0773a5fe85320b2623c9d3ac` (unrelated earth-to-stars documentation commit at repository head).
- Prior M3 attack-runtime foundation commit `ed740cc304c6b2cf8c99d61d9d630ef09a0476d5` was rechecked: `Build Riftfrontier` run `34178875657` completed with overall `success`.
- This batch preserved M2 field-play-gated balance values and did not modify other project folders.

## Completed in this batch

M3 Minecraft server hit-volume/damage integration:

- Added `MinecraftAttackAdapter`, which owns one `AttackStateMachine` and never copies telegraph/active/recovery durations.
- Actual Minecraft `LivingEntity` damage is resolved only when the shared state-machine sample reports `ACTIVE`.
- Added explicit `HitVolume` contract so delivery geometry can vary independently from timing while still being gated by one authoritative cadence.
- Added bounded `AabbHitVolume` implementation using a local entity query rather than a broad world scan.
- Added per-execution target UUID deduplication: one attack execution cannot damage the same target once per server tick merely because ACTIVE spans multiple ticks.
- Explicit cancel (future stun/death/despawn boundary) clears the execution hit set and permanently closes the cancelled attack window.
- Added required native GameTest `attack_hit_window` with real Zombie attacker/target entities. It verifies telegraph no-damage, ACTIVE real health loss, no duplicate same-execution tick hit, recovery no-damage, completion no late hit, and cancellation no reopened hit.
- Registered the new combat GameTest alongside the existing Riftfrontier required test suite.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftAttackAdapter.java`
- `src/main/java/kr/moonseungjun/riftfrontier/gametest/CombatGameTests.java`
- `src/main/java/kr/moonseungjun/riftfrontier/Riftfrontier.java`
- `src/main/resources/data/riftfrontier/test_instance/attack_hit_window.json`

## Verification

- Implementation commit: `08fba9a415b4204949d832e2e126763bcf604105`.
- `Build Riftfrontier` run `34182335245` targets exactly that implementation commit.
- At handoff write time the run was `IN PROGRESS`; checkout/setup had begun and the full build/GameTest/server/client/JAR gate was not yet complete.
- Therefore this batch does **not** claim the new Minecraft damage adapter is fully CI-verified yet.
- Previous attack-runtime foundation run `34178875657`: overall `SUCCESS`.
- Actual human Minecraft field play: NOT TESTED.
- Final model/animation/VFX/sound presentation: NOT DESIGNED / NOT TESTED by design gate.

## Do not repeat or revert

- Do not rebuild M2 extraction/restart/evidence infrastructure.
- Do not tune M2 pressure scaling or patrol bonus without human field-play evidence.
- Do not rename M2 Zombie/Skeleton/Ravager technical proxies into production creatures.
- Preserve `AttackPattern` as the single authoritative timing source; do not introduce separate animation, hitbox, damage or VFX timing constants.
- Preserve fail-closed runtime content lookup and server-owned attack lifecycle.
- Preserve per-execution hit deduplication unless a future authored attack explicitly adds multi-hit semantics through a separate validated policy.
- Do not replace bounded hit-volume resolution with per-tick broad entity scans.
- Do not invent final Region 01 boss art/VFX/sound before the presentation asset/reference gate.

## Exact next start point

1. Check latest remote `main`, then inspect `Build Riftfrontier` run `34182335245`. If it failed, fix the first actual compiler/GameTest/runtime failure without weakening the ACTIVE-only damage invariant, bounded hit-volume contract, or one-hit-per-target execution invariant.
2. If the full gate is green, add boss phase/attack-selection runtime that consumes `BossProfile` references and the existing `CombatRuntimeCatalog`/`AttackStateMachine` without duplicating attack cadence.
3. Boss selection must have an explicit deterministic policy boundary suitable for later authored weighting/cooldown rules; phase transition, cancellation and attack completion must remain server-owned.
4. Presentation adapters must consume the same sampled phase/progress/presentation cue; do not create animation/VFX timers yet.
5. Keep M2 numerical tuning and final Region 01 production boss assets gated on actual field-play/reference evidence.
