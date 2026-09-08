# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Canonical design documents and current GitHub `main` remain authoritative.

## Last recovered baseline

- This session started by re-reading remote `main` at `46c48db5a0d2969e5a1f6f8e091f6eb34c5b2b81`, then re-read the canonical project documents in the required order.
- The previous M3 presentation-frame CI, `Build Riftfrontier` run `34189760239` for `e681b41f962fda2ca19d43028209f3cdda89adff`, is now confirmed `SUCCESS`.
- The previous handoff's exact next boundary was server-to-client semantic presentation sync/resolution. This batch continued from that boundary and did not rebuild the authoritative boss timeline, Minecraft damage adapter, boss lifecycle, or PresentationFrame.
- M2 field-play-gated values and all non-Riftfrontier project folders were intentionally left untouched.

## Completed in this batch

M3 boss presentation semantic transport + ordering boundary:

- Added `BossPresentationSemanticState`, a Minecraft-API-free semantic snapshot derived directly from the authoritative `MinecraftBossCombatAdapter.PresentationFrame`.
- Added `BossPresentationPayload` as a thin NeoForge `CustomPacketPayload` transport wrapper around the semantic state. It carries no independent animation, VFX, sound, cooldown, hitbox or damage cadence.
- Added common-side `RiftfrontierNetworking` registration plus a tracking-player send seam that converts the exact authoritative boss tick result into semantic state or a canonical clear state.
- Added physical-client-only `RiftfrontierClientNetworking` for payload handling and disconnect cleanup, keeping client event types out of common/dedicated-server registration.
- Added `BossPresentationClientState` with per-entity monotonic `serverGameTick` ordering. Delayed older snapshots cannot rewind a newer presentation.
- Clear snapshots retain their ordering watermark so an older delayed ACTIVE snapshot cannot resurrect visuals after an authoritative clear.
- Disconnect cleanup clears both active semantics and tick watermarks so a new connection/world may start a new entity-id/game-time epoch safely.
- Added/updated JUnit contract coverage for authoritative frame-to-semantic copying, stale packet rejection, clear watermark behavior, reconnect epoch reset, and impossible ACTIVE/hit-window combinations.
- During validation, an initial test compile failure exposed that plain JUnit source sets cannot safely depend on Minecraft packet supertypes. The contract was improved instead of weakening the build: pure presentation semantics are now separated from NeoForge transport, so plain JVM tests exercise the semantic/client-ordering contract without requiring Minecraft classes.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/BossPresentationSemanticState.java`
- `src/main/java/kr/moonseungjun/riftfrontier/network/BossPresentationPayload.java`
- `src/main/java/kr/moonseungjun/riftfrontier/network/RiftfrontierNetworking.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/BossPresentationClientState.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/RiftfrontierClientNetworking.java`
- `src/main/java/kr/moonseungjun/riftfrontier/Riftfrontier.java`
- `src/test/java/kr/moonseungjun/riftfrontier/network/BossPresentationSyncContractTest.java`

## Verification

- Latest Riftfrontier code/test commit for this batch before this handoff: `6e3637ea5fe054986e90116278ac47fd420608c1`.
- Earlier workflow run `34193275569` failed at `compileTestJava` because `BossPresentationSyncContractTest` referenced `BossPresentationPayload`, whose `CustomPacketPayload` supertype is unavailable on the plain JUnit compile classpath. Production `compileJava` had succeeded. GameTest/server/client/JAR gates were therefore not run in that failed workflow.
- The failure was corrected architecturally by introducing transport-free `BossPresentationSemanticState` and moving the unit contract to that type rather than adding a fake API or weakening tests.
- `Build Riftfrontier` run `34193512087` targets exactly `6e3637ea5fe054986e90116278ac47fd420608c1`; it was still running when this handoff was written. Do not claim this batch fully CI-verified until clean/unit build, required native GameTest, dedicated server smoke, Xvfb client smoke and executable-JAR inspection all finish green.
- Actual multiplayer boss semantic packet reception/rendering: NOT TESTED.
- No final production boss entity currently consumes the send seam as a finished Region 01 boss integration: NOT IMPLEMENTED / NOT TESTED.
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
- Preserve `PresentationFrame` as the runtime presentation source and `BossPresentationSemanticState` as the transport-free semantic contract.
- Do not make packet transport itself an asset resolver or duplicate presentation timing in the client.
- Preserve monotonic per-entity server-tick ordering and the clear watermark; do not allow delayed packets to rewind/resurrect stale presentation.
- Keep client-only registration isolated from common/dedicated-server class loading.
- Do not invent final Region 01 boss art/VFX/sound before the reference/asset gate.

## Exact next start point

1. Re-check current remote `main`, then inspect `Build Riftfrontier` run `34193512087`. If it failed, fix the first actual compiler/test/runtime failure without weakening the semantic/timing/order invariants.
2. If the full gate is green, complete the Cobblemon-style presentation resolver boundary: map stable semantic inputs (`presentationCue`, delivery, attack phase, boss/variant context) to validated animation/VFX/sound/model asset keys without final visual invention and without client-side timing constants.
3. Keep that resolver data-driven and fail-closed for missing/invalid authored keys. The packet/client cache should remain transport/state only.
4. Before choosing actual GeckoLib/model/VFX/audio assets, re-read `REFERENCE_TARGETS.md`, `M3_COMBAT_REFERENCE_DOSSIER.md`, and `THIRD_PARTY_ASSETS.md`; perform the required reference/license/compatibility gate.
5. Wire `RiftfrontierNetworking.syncBossPresentation(...)` into a final production entity-backed Region 01 boss only when that production entity boundary is authored; do not misuse M2 technical proxies as the final boss.
6. Keep M2 numerical tuning and final Region 01 production assets gated on actual field-play/reference evidence.
