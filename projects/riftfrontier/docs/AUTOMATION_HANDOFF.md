# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` at this run start: `917b5b808c52c57cba1744807220c0553401418a`.
- Previous verified source-animation inventory/binding baseline: implementation `0c4d7ef1d2a2b53f1806909ba56cd5efa3426d37`, run `34317521853` FULL SUCCESS.
- `Dragon Evolved` remains selected only as Region 01 first-boss geometry/rig derivation source. Source `Atlas` art remains unapproved.

## Completed in this batch

M3 authoritative semantic-state -> skinned custom-geometry render pipeline boundary:

- Added `BossCustomGeometryRenderPipeline` as the production-facing client bridge from `BossPresentationSemanticState` to `BossPresentationResolver` -> `BossAnimationSampleBridge` -> `BossSkinnedMeshFrameSampler` -> `SkinnedMeshCustomGeometryAdapter`.
- The pipeline owns no animation clock, damage timing, material/texture lookup or fallback. Sample time remains derived only from server-authored semantic phase progress.
- Added `prepareCurrent(entityId)` using the monotonic `BossPresentationClientState` cache, so stale server snapshots cannot rewind the rendered frame.
- Added `submitCurrent(...)` as the minimal entity-renderer call boundary. The caller must still provide an explicitly approved Minecraft 26.2 `RenderType`; source Atlas material cannot enter implicitly.
- Added topology consistency checks between the authoritative skinned frame and immutable prepared custom geometry.
- Added JUnit coverage for end-to-end semantic resolution -> authoritative sample time -> LBS deformation -> prepared triangle geometry, monotonic cache behavior, and fail-closed missing/inactive presentation.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/client/render/BossCustomGeometryRenderPipeline.java`
- `src/test/java/kr/moonseungjun/riftfrontier/client/render/BossCustomGeometryRenderPipelineTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Implementation commit: `0ae647b7bc76650baf0aca60f3978003eed935bf`.
- `Build Riftfrontier` run `34321821176`: FULL SUCCESS — toolchain, asset-intake tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb client smoke, executable JAR inspection, report and artifact upload all SUCCESS.
- Local build/test: NOT RUN; GitHub Actions was the executed validation path.
- Exact accepted sanitized Dragon derivation -> real per-clip duration/channel/interpolation metrics: NOT RUN. External binary was not reacquired; no metrics or clip mappings were guessed.
- Explicit logical production mapping from attack animation keys to real source clips: NOT IMPLEMENTED pending real accepted-source motion inspection.
- Concrete production boss entity renderer registered against an actual boss entity type and calling `submitCurrent(...)`: NOT IMPLEMENTED. The production-facing call boundary now exists, but there is not yet a final boss entity/renderer registration.
- Actual deformed Dragon geometry visible on screen: NOT TESTED. Xvfb smoke proves the client loads the pipeline, not that a spawned boss rendered through it.
- Real approved material/texture, physical presentation resources and `presentation_assets` manifest: NOT IMPLEMENTED.
- Scale/culling/UV/deformation/hitbox/VFX/sound/human field-play review: NOT TESTED.

## Do not repeat or revert

- Preserve source SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` and accepted derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` unless converter schema/version intentionally changes.
- Preserve exact source clip inventory names unless direct accepted-source inspection proves a source revision changed.
- Do not hard-code guessed clip durations/channel metrics or infer logical mappings from clip names.
- Preserve `AttackPattern`/server semantic state as authoritative timing and ACTIVE-only damage semantics. No presentation layer may become a free-running combat clock.
- Preserve monotonic client semantic snapshot ordering and fail-closed presentation resolution.
- Do not permit source `Atlas` material/texture/image bytes into production resources.
- Do not restore legacy GeckoLib 4 paths or rigid GeoBone/cube approximation for this asset.
- Preserve arbitrary triangle topology, four-influence LBS, imported rig/animations, pose sampler, semantic sample bridge, frame sampler, verified inventory/binding gate, custom-geometry adapter and the new render pipeline.
- Keep Minecraft 26.2 `net.minecraft.client.renderer.rendertype.RenderType` boundary.
- Do not weaken exact derivation SHA/count validation or create fake physical presentation assets/manifests.
- Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs and this handoff. Treat implementation `0ae647b7bc76650baf0aca60f3978003eed935bf`, run `34321821176` as the green authoritative-render-pipeline baseline.
2. Reacquire accepted derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`; run `GltfAnimationImporter` + exact inventory gate and record real metrics for all eight clips.
3. Inspect real motion and author explicit logical source bindings only where motion semantics actually match; do not infer from names.
4. Introduce/register the actual Region 01 boss entity/render-state/entity-renderer and call `BossCustomGeometryRenderPipeline.submitCurrent(...)` from that renderer. Keep material selection blocked until an approved texture/material resource exists.
5. Spawn the real boss under graphical capture and inspect topology, normals, UV orientation, culling, scale and deformation. Only then proceed to approved material/texture, hitbox/telegraph, VFX/sound and human field-play review.
