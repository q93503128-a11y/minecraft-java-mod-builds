# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` at this run start: `39c416434ed32e5cc8d59723c002d467f122c086`.
- Latest verified Riftfrontier implementation/test HEAD before this handoff update: `fd10ad377d2dce0ad3c5bcc02258c9d1920a95c2`.
- `Dragon Evolved` remains selected only as Region 01 first-boss geometry/rig derivation source. Source `Atlas` art remains unapproved.

## Completed in this batch

M3 production boss actor/render bridge:

- Registered Riftfrontier-owned `region_01_boss` `EntityType` and base living attributes without inventing final health, dimensions, hitbox, AI or encounter tuning.
- Added `Region01BossEntity` as the stable Minecraft actor identity. It is deliberately not inserted into natural spawning or Region 01 encounter content yet.
- Added a dedicated `Region01BossRenderState` that carries the exact Minecraft entity id used to resolve the server-authoritative presentation cache.
- Added `Region01BossRenderer` and client-only `EntityRenderersEvent.RegisterRenderers` registration using the actual Minecraft 26.2 entity-render submission API.
- Added `Region01BossClientRenderRuntime`, an atomic fail-closed publication seam. Until an approved runtime asset + inspected animation binding + approved `RenderType` are published together, the renderer submits no fallback model/material/texture. Once published it calls the existing `BossCustomGeometryRenderPipeline.submitCurrent(...)` directly.
- Added a pure `Region01BossRenderIdentity` invariant so the ordinary JVM test source set can verify non-negative entity ids without crossing the isolated client-only Minecraft superclass boundary.
- First CI exposed an incorrect 26.2 `CameraRenderState` package; corrected to `net.minecraft.client.renderer.state.level.CameraRenderState` without reducing functionality. Second CI showed the ordinary test source set cannot load client-only `EntityRenderState`; the test was preserved by extracting the shared pure identity invariant rather than deleting or weakening it.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/Riftfrontier.java`
- `src/main/java/kr/moonseungjun/riftfrontier/entity/RiftfrontierEntityTypes.java`
- `src/main/java/kr/moonseungjun/riftfrontier/entity/Region01BossEntity.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/Region01BossRenderIdentity.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/Region01BossRenderState.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/Region01BossClientRenderRuntime.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/Region01BossRenderer.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/RiftfrontierClientRenderers.java`
- `src/test/java/kr/moonseungjun/riftfrontier/client/render/Region01BossRenderStateTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Initial actor/render implementation commit: `5f9c77a7faba9b2cb6507e9f0915e935bd204b14`.
- Minecraft 26.2 camera-state package correction: `f3844f49d3a5b37c4e018da954a1c3573d741f66`.
- Final identity/source-set regression-test HEAD: `fd10ad377d2dce0ad3c5bcc02258c9d1920a95c2`.
- `Build Riftfrontier` run `34339273157`: FULL SUCCESS. Toolchain, 25 asset-intake tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb client smoke, executable JAR inspection, report generation, deliverables upload, and logs/reports upload all succeeded.
- Earlier run `34338815434`: FAILED at `compileJava` only because `CameraRenderState` was imported from the pre-26.2 package; fixed.
- Earlier run `34339046839`: production `compileJava` succeeded but `compileTestJava` failed because default tests cannot load client-only `EntityRenderState`; fixed by a shared pure identity invariant while retaining regression coverage.
- Prior runtime-asset run `34332734136` remains FULL SUCCESS.
- Previous CI artifacts were inspected in this run to try to reacquire the accepted Dragon source bytes. Logs and deliverable JAR artifacts contain no accepted glTF source binary.
- Exact accepted sanitized Dragon derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`: NOT reacquired in this execution environment.
- Real eight-clip duration/channel/path/interpolation receipt: NOT PRODUCED.
- Explicit logical production mapping from attack animation keys to real source clips: NOT IMPLEMENTED pending direct accepted-source motion inspection.
- Approved material/texture/`RenderType` publication into `Region01BossClientRenderRuntime`: NOT IMPLEMENTED; the runtime therefore remains intentionally inactive/fail-closed.
- Region 01 encounter spawn/combat attachment, final dimensions/hitbox/scale, actual spawned/deformed Dragon graphical capture, VFX/sound and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Preserve source SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` and accepted derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` unless converter schema/version intentionally changes.
- Preserve exact source clip inventory names unless direct accepted-source inspection proves a source revision changed.
- Do not hard-code guessed clip durations/channel metrics or infer logical mappings from clip names.
- Do not bypass exact-SHA/exact-structure/exact-inventory gates or construct the Region 01 production runtime asset from independently substituted bytes.
- Preserve `Region01BossRuntimeAsset` as the composed production import seam.
- Preserve `AttackPattern`/server semantic state as authoritative timing and ACTIVE-only damage semantics. No presentation layer may become a free-running combat clock.
- Preserve monotonic client semantic snapshot ordering and fail-closed presentation resolution.
- Preserve the newly registered `region_01_boss` actor/render-state/renderer bridge and `Region01BossClientRenderRuntime`; do not replace it with fallback art or a parallel renderer clock.
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
