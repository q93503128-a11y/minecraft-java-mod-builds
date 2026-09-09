# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` at this run start: `10c9f4b36f456cedae0c4463f09089a9da395559`.
- Previous custom-geometry adapter baseline remains `f208c713e5627fc9a800c904b936c0867553c0f7`, run `34313426477` FULL SUCCESS.
- `Dragon Evolved` remains selected only as Region 01 first-boss geometry/rig derivation source. Source `Atlas` art remains unapproved.

## Completed in this batch

M3 verified source-animation inventory and explicit logical binding boundary:

- Added `AnimationClipInventory`: immutable imported-clip catalog, duplicate-name rejection, exact-name inventory gate, fail-closed lookup and deterministic per-clip metrics for duration/channel/key/path/interpolation coverage.
- Added `Region01BossDragonEvolvedAnimationContract` preserving pinned source SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`, accepted derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`, and the eight source clip names already established by direct source inspection: `Death`, `Fast_Flying`, `Flying_Idle`, `Headbutt`, `HitReact`, `No`, `Punch`, `Yes`.
- Added `BossAnimationSourceBinding`: logical animation keys must explicitly name a source clip that exists in a verified inventory. No fuzzy matching, semantic-name inference, fallback or invented production mapping is allowed.
- Added production-safe `BossAnimationSampleBridge(BossAnimationSourceBinding, AnimationClipInventory)` constructor so the authoritative semantic sample bridge can consume only explicitly resolved verified source clips.
- Added JUnit coverage for exact eight-clip acceptance, missing/unexpected/duplicate source clips, metric extraction, explicit logical binding, missing-source rejection and blank-name rejection.
- No source clip durations/channel counts/interpolation counts were guessed because the accepted binary was not available in this execution environment.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/mesh/AnimationClipInventory.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/Region01BossDragonEvolvedAnimationContract.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossAnimationSourceBinding.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossAnimationSampleBridge.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/mesh/AnimationClipInventoryTest.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/BossAnimationSourceBindingTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Implementation commit: `0c4d7ef1d2a2b53f1806909ba56cd5efa3426d37`.
- `Build Riftfrontier` run `34317521853`: FULL SUCCESS — toolchain, asset-intake tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb client smoke, executable JAR inspection, report and artifact upload all SUCCESS.
- Local build/test: NOT RUN; GitHub Actions was the executed validation path.
- Exact accepted sanitized Dragon derivation -> real per-clip duration/channel/interpolation metrics: NOT RUN. Generic network access in this execution environment could not reacquire the external binary; the existing verified source-name facts were used without inventing metrics.
- Logical production mapping from current attack animation keys to `Headbutt`/`Punch`/other source clips: NOT IMPLEMENTED. The new binding boundary exists specifically to prevent guessing this mapping.
- Actual production boss entity/renderer calling `SkinnedMeshCustomGeometryAdapter.submit(...)`: NOT IMPLEMENTED / NOT TESTED.
- Actual deformed Dragon geometry visible on screen: NOT TESTED.
- Real physical MODEL/ANIMATION resources and `presentation_assets` manifest: NOT IMPLEMENTED.
- Production texture/material, VFX/sound, scale/culling/UV/deformation/hitbox review and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Do not repeat candidate selection/source fingerprint work unless the pinned immutable source check fails.
- Preserve source SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` and accepted derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` unless converter schema/version intentionally changes.
- Preserve exact source clip inventory names unless direct accepted-source inspection proves a source revision changed.
- Do not hard-code guessed clip durations/channel metrics or infer logical mappings from clip names.
- Preserve `AttackPattern`/server semantic state as authoritative timing and ACTIVE-only damage semantics. `BossAnimationSampleBridge` and `BossSkinnedMeshFrameSampler` must never become free-running clocks.
- Preserve fail-closed presentation resolution, atomic client reload and exact content-generation matching.
- Do not permit source `Atlas` material/texture/image bytes into production resources.
- Do not restore legacy GeckoLib 4 paths or force GeckoMesh/GeoBone onto this asset.
- Do not approximate arbitrary Dragon triangles into cubes/bounding boxes or collapse multi-joint weights to a dominant bone.
- Preserve project-owned four-influence LBS deformation semantics and completed mesh/rig importer, animation importer, pose sampler, semantic sample bridge, frame sampler, verified source inventory/binding gate and custom-geometry adapter.
- Keep Minecraft 26.2 `net.minecraft.client.renderer.rendertype.RenderType` boundary.
- Do not weaken exact derivation SHA/count validation to make a changed asset load.
- Do not create placeholder production resources or a fake `presentation_assets` manifest.
- Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs and this handoff. Treat implementation commit `0c4d7ef1d2a2b53f1806909ba56cd5efa3426d37`, run `34317521853` as the green verified-inventory/binding baseline.
2. Reacquire the exact accepted sanitized Dragon derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`; run `GltfAnimationImporter`, then `Region01BossDragonEvolvedAnimationContract.verifyImportedClips(...)`. Record `AnimationClipInventory.metrics()` for all eight real clips: exact duration, channel count, key count, path coverage and interpolation coverage.
3. Compare those measured clips to the current logical animation keys/attack semantics and author an explicit `BossAnimationSourceBinding` only where the motion actually matches. Do not infer mappings from names alone.
4. Add the minimal production boss client render-state/entity-renderer call site that feeds authoritative `BossSkinnedMeshFrameSampler.FrameSample` into `SkinnedMeshCustomGeometryAdapter`. Keep `RenderType`/material supplied only by an explicitly approved presentation resource rather than source Atlas fallback.
5. Validate an actually spawned boss under graphical client capture. Check topology, normals, UV orientation, culling, scale and deformation before claiming visual success.
6. Only after approved material/texture exists, perform hitbox/telegraph/VFX/sound and human field-play review; only after all physical presentation assets exist may the first real `presentation_assets` manifest be created.
