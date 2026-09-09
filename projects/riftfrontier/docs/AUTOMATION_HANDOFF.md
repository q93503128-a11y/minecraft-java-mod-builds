# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` recovered at this run start: `2477e0be65cc702cf1ab50786b6c86300e77ed9c`.
- Previous `BossSkinnedMeshFrameSampler` validation run `34309678765` was re-checked this run and is fully SUCCESS.
- Concurrent unrelated `turnbound-re` commits advanced `main` during this run; they were preserved without force-push or overwrite.
- `Dragon Evolved` remains selected only as Region 01 first-boss geometry/rig derivation source. Source `Atlas` art remains unapproved.

## Completed in this batch

M3 renderer-neutral skinned frame -> Minecraft 26.2 custom-geometry submission boundary:

- Added `SkinnedMeshCustomGeometryAdapter` using the official 26.2 `SubmitNodeCollector.submitCustomGeometry` path.
- The adapter consumes the exact immutable `BossSkinnedMeshFrameSampler.FrameSample`/`SkinnedMeshFrame`; it owns no animation clock, hit timing, clip fallback, material selection or texture lookup.
- The caller must supply `RenderType`, so the unapproved source Atlas material cannot enter through this adapter implicitly.
- Custom geometry preserves accepted triangle indices, positions, UVs and normals and does not convert the Dragon into cubes, bounding boxes or rigid-bone geometry.
- Renderer data is defensively snapshotted before deferred feature submission.
- Hardened `SkinnedMeshFrame` to fail closed on non-finite position/normal/UV data and out-of-range indices before renderer submission.
- Added JUnit coverage for immutable prepared geometry, topology preservation, invalid indices and non-finite streams.
- Initial implementation used the pre-26.2 `net.minecraft.client.renderer.RenderType` package. CI exposed this exact compile failure; the feature was not weakened. It was corrected to 26.2 `net.minecraft.client.renderer.rendertype.RenderType` after current NeoForge 26.2 API verification.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/client/render/SkinnedMeshCustomGeometryAdapter.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/mesh/SkinnedMeshFrame.java`
- `src/test/java/kr/moonseungjun/riftfrontier/client/render/SkinnedMeshCustomGeometryAdapterTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Initial implementation commit: `ed4df9cd0611abb8df6a4db5ac492eb3831ceb25`.
- Initial `Build Riftfrontier` run `34313244740`: toolchain SUCCESS; asset-intake tests SUCCESS; clean build FAILED at the first real compile error because `RenderType` moved packages in 26.2. GameTest/server/client/JAR gates were skipped. No feature/test was removed to pass.
- Corrected code commit: `f208c713e5627fc9a800c904b936c0867553c0f7`.
- Corrected `Build Riftfrontier` run `34313426477`: FULL SUCCESS — toolchain, asset-intake tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb client smoke, executable JAR inspection, report and artifact upload all SUCCESS.
- Local build/test: NOT RUN; GitHub Actions was the executed validation path.
- Actual production boss entity calling `SkinnedMeshCustomGeometryAdapter.submit(...)`: NOT IMPLEMENTED / NOT TESTED.
- Actual deformed Dragon geometry visible on screen: NOT TESTED. Xvfb client smoke proves the project/client loads with the adapter, not that a boss instance rendered through it.
- Exact accepted sanitized Dragon derivation -> all eight real source clips: NOT RUN in this batch because the accepted binary was not available in this execution environment. Production clip names/mappings were not guessed.
- Real physical MODEL/ANIMATION resources and `presentation_assets` manifest: NOT IMPLEMENTED.
- Production texture/material, VFX/sound, scale/culling/UV/deformation/hitbox review and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Do not repeat candidate selection/source fingerprint work unless the pinned immutable source check fails.
- Preserve source SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` and accepted derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` unless converter schema/version intentionally changes.
- Preserve `AttackPattern`/server semantic state as authoritative timing and ACTIVE-only damage semantics. `BossAnimationSampleBridge` and `BossSkinnedMeshFrameSampler` must never become free-running clocks.
- Preserve fail-closed presentation resolution, atomic client reload and exact content-generation matching.
- Do not permit source `Atlas` material/texture/image bytes into production resources.
- Do not restore legacy GeckoLib 4 paths or force GeckoMesh/GeoBone onto this asset.
- Do not approximate arbitrary Dragon triangles into cubes/bounding boxes or collapse multi-joint weights to a dominant bone.
- Preserve project-owned four-influence LBS deformation semantics and completed mesh/rig importer, animation importer, pose sampler, semantic sample bridge, frame sampler and custom-geometry adapter; optimize only after profiler evidence.
- Keep the Minecraft 26.2 `net.minecraft.client.renderer.rendertype.RenderType` boundary; do not regress to the old package.
- Do not weaken exact derivation SHA/count validation to make a changed asset load.
- Do not create placeholder production resources or a fake `presentation_assets` manifest.
- Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs and this handoff. Treat corrected run `34313426477` as the green custom-geometry-adapter baseline.
2. Reacquire the exact accepted sanitized Dragon derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` and run `GltfAnimationImporter` over all eight real source clips. Record exact clip names, durations, channel counts and interpolation coverage. Keep source/derivation SHA gates.
3. Bind only that verified real clip inventory to logical animation keys consumed by `BossAnimationSampleBridge`; do not guess production mappings.
4. Add the minimal production boss client render-state/entity-renderer call site that feeds authoritative `BossSkinnedMeshFrameSampler.FrameSample` into the completed `SkinnedMeshCustomGeometryAdapter`. Keep `RenderType`/material supplied by an explicitly approved presentation resource rather than source Atlas fallback.
5. Validate an actually spawned boss under graphical client capture. Check topology, normals, UV orientation, culling, scale and deformation before claiming visual success.
6. Only after approved material/texture exists, perform hitbox/telegraph/VFX/sound and human field-play review; only after all physical presentation assets exist may the first real `presentation_assets` manifest be created.
