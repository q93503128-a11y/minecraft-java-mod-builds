# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` at this run start: `4cd02762c3ea77f63325b38037e42c43c51392bc`.
- Latest verified Riftfrontier implementation/test HEAD in this run: `793901a6f813703966cc365a305fcdbc1d8ba40a`.
- `Dragon Evolved` remains selected only as Region 01 first-boss geometry/rig derivation source. Source `Atlas` art remains unapproved.

## Completed in this batch

M3 boss presentation actor-identity hardening:

- Bound each server-authoritative boss presentation semantic snapshot to both the Minecraft numeric entity id and stable entity UUID.
- Extended `BossPresentationPayload` to carry the UUID over the wire and changed production sync to source it directly from `LivingEntity.getUUID()`.
- Hardened `BossPresentationClientState` so monotonic server-tick ordering is scoped to one UUID. Reuse of the same numeric entity id by a new UUID starts a fresh watermark instead of inheriting the old actor state.
- Added UUID-checked client lookup. A renderer querying the right numeric id with the wrong UUID receives no presentation state, preventing a newly spawned/reused id from briefly displaying stale pose state before its first packet.
- Propagated UUID through `Region01BossRenderIdentity`, `Region01BossRenderState`, `Region01BossRenderer`, `Region01BossClientRenderRuntime`, and `BossCustomGeometryRenderPipeline` so production custom-geometry submission is actor-identity checked end-to-end.
- Added regression tests for stale same-actor ticks, numeric-id reuse across two UUIDs, actor-scoped clear watermarks, and render identity requiring id + UUID.
- First CI exposed one existing API-free test fixture still calling the old `fromFrame(int,long,frame)` helper. Preserved that test contract with a deterministic fixture-only compatibility factory; production networking remains UUID-mandatory.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/BossPresentationSemanticState.java`
- `src/main/java/kr/moonseungjun/riftfrontier/network/BossPresentationPayload.java`
- `src/main/java/kr/moonseungjun/riftfrontier/network/RiftfrontierNetworking.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/BossPresentationClientState.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/Region01BossRenderIdentity.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/Region01BossRenderState.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/Region01BossRenderer.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/Region01BossClientRenderRuntime.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/BossCustomGeometryRenderPipeline.java`
- `src/test/java/kr/moonseungjun/riftfrontier/client/BossPresentationClientStateTest.java`
- `src/test/java/kr/moonseungjun/riftfrontier/client/render/Region01BossRenderStateTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Main implementation commit: `9ce0152a6507d7d02369db57b1b0b30fe0af2157`.
- Fixture-compatibility correction: `793901a6f813703966cc365a305fcdbc1d8ba40a`.
- `Build Riftfrontier` run `34344045811`: FULL SUCCESS. Toolchain, 25 asset-intake tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb client smoke, executable JAR inspection, report generation, deliverables upload, and logs/reports upload all succeeded.
- Earlier run `34343872241`: FAILED only at `compileTestJava` because `BossPresentationSyncContractTest` still used the API-free legacy `fromFrame(int,long,frame)` fixture signature; fixed without deleting or weakening the test.
- Exact accepted sanitized Dragon derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`: NOT reacquired in this execution environment.
- Real eight-clip duration/channel/path/interpolation receipt: NOT PRODUCED.
- Explicit logical production mapping from attack animation keys to real source clips: NOT IMPLEMENTED pending direct accepted-source motion inspection.
- Approved material/texture/`RenderType` publication into `Region01BossClientRenderRuntime`: NOT IMPLEMENTED; runtime remains intentionally inactive/fail-closed.
- Region 01 encounter spawn/combat attachment, final dimensions/hitbox/scale, actual spawned/deformed Dragon graphical capture, VFX/sound and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Preserve the UUID-bound presentation identity from server semantic state through wire payload, client cache, render state and custom-geometry submission. Production render lookup must never regress to numeric entity id alone.
- Preserve same-UUID monotonic server-tick rejection and allow a different UUID reusing the numeric id to establish a fresh watermark.
- Preserve source SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` and accepted derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` unless converter schema/version intentionally changes.
- Preserve exact source clip inventory names unless direct accepted-source inspection proves a source revision changed.
- Do not hard-code guessed clip durations/channel metrics or infer logical mappings from clip names.
- Do not bypass exact-SHA/exact-structure/exact-inventory gates or construct the Region 01 production runtime asset from independently substituted bytes.
- Preserve `Region01BossRuntimeAsset` as the composed production import seam.
- Preserve `AttackPattern`/server semantic state as authoritative timing and ACTIVE-only damage semantics. No presentation layer may become a free-running combat clock.
- Preserve monotonic client semantic snapshot ordering and fail-closed presentation resolution.
- Preserve the registered `region_01_boss` actor/render-state/renderer bridge and `Region01BossClientRenderRuntime`; do not replace it with fallback art or a parallel renderer clock.
- Do not add the boss to natural spawning/encounter composition or invent final dimensions/hitbox/combat tuning before visual/source evidence and authored content exist.
- Do not permit source `Atlas` material/texture/image bytes into production resources.
- Do not restore legacy GeckoLib 4 paths or rigid GeoBone/cube approximation for this asset.
- Preserve arbitrary triangle topology, four-influence LBS, imported rig/animations, pose sampler, semantic sample bridge, frame sampler, verified inventory/binding gate, custom-geometry adapter and production-facing render pipeline.
- Keep Minecraft 26.2 `net.minecraft.client.renderer.rendertype.RenderType` and `net.minecraft.client.renderer.state.level.CameraRenderState` boundaries.
- Do not weaken exact derivation SHA/count validation or create fake physical presentation assets/manifests.
- Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs and this handoff.
2. Reacquire the exact accepted sanitized Dragon derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` from the recorded original Quaternius/Drive provenance; reject substitutes whose bytes do not match.
3. Run `python tools/audit_region01_boss_animation_clips.py <accepted.gltf> assets/sources/region_01_boss_dragon_evolved.acceptance.json --output <receipt.json>` and load the same bytes through `Region01BossRuntimeAsset.importAccepted(...)`; commit real clip metrics only if both exact gates pass.
4. Inspect actual clip motion and author explicit logical source bindings only where motion semantics match; never infer from names alone.
5. Only then publish the verified runtime asset + approved `RenderType` through `Region01BossClientRenderRuntime`, attach the server encounter/combat actor, and perform a spawned graphical capture before hitbox/telegraph/VFX/sound tuning.
