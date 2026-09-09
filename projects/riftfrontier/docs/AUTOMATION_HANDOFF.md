# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` recovered at this run start: `b52b05df4df5789bc831db88d85413ef2d78f61b`.
- Concurrent unrelated repository work was preserved. The Riftfrontier implementation was fast-forwarded on top of that baseline; later unrelated `turnbound-re` work advanced `main` to `24ef8661395e8f2c52f5cc07da6b1811339d21a4` before this handoff update.
- Previous accepted art-neutral glTF -> skinned-mesh/rig importer baseline: implementation `934ec45c4a02fcd3c5267103c031cb1b8fb4eb65`, `Build Riftfrontier` run `34297942339`: full SUCCESS.
- `Dragon Evolved` remains selected only as Region 01 first-boss geometry/rig derivation source. Source `Atlas` art remains unapproved.

## Completed in this batch

M3 renderer-neutral glTF animation-channel import + 46-joint pose sampling boundary:

- Added immutable `AnimationClip` / `Channel` contracts targeting the imported skin palette rather than Minecraft entity state.
- Added deterministic embedded-glTF animation import for translation / rotation / scale channels.
- Supports glTF `STEP`, `LINEAR`, and `CUBICSPLINE`; unknown interpolation fails closed rather than being approximated.
- Preserves glTF cubic Hermite tangent layout and normalizes quaternion results.
- Uses normalized shortest-path quaternion slerp for LINEAR rotation channels, including sign-equivalent quaternion endpoints.
- Samples only presentation pose. It does not own hit timing, attack selection, damage, or a second combat clock.
- Reconstructs per-joint local TRS from the imported rest affine transform with shear/reflection rejection, applies animated components, resolves the hierarchy, and returns skin matrices as `globalJoint * inverseBind`.
- Rejects animation channels targeting nodes outside the imported skin palette, sparse accessors, invalid strides/accessor ranges, non-finite values, duplicate joint/path channels, and degenerate quaternions.
- Added JUnit coverage for embedded accessor decoding, LINEAR translation, shortest-path quaternion interpolation, CUBICSPLINE Hermite layout, unsupported interpolation rejection, and duplicate-channel rejection.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/mesh/AnimationClip.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/mesh/GltfAnimationImporter.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/mesh/JointPoseSampler.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/mesh/GltfAnimationImporterTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Test-bearing implementation commit: `036cda32e6451b50af55a38ca19e579ab5b22628`.
- `Build Riftfrontier` run `34301699458`: full SUCCESS.
- CI toolchain: SUCCESS.
- CI asset intake tool tests: SUCCESS.
- CI JUnit + clean build: SUCCESS.
- CI required native GameTest: SUCCESS.
- CI dedicated server smoke: SUCCESS.
- CI Xvfb client smoke: SUCCESS.
- CI executable JAR inspection: SUCCESS.
- CI build report + deliverable/log artifact upload: SUCCESS.
- Exact accepted 681773-byte sanitized Dragon derivation -> animation importer over all eight real source clips: NOT RUN in this batch because that accepted binary is not committed and was not reacquired in this execution environment.
- Authoritative semantic/`AttackPattern` state -> animation sample-time bridge: NOT IMPLEMENTED / NOT TESTED.
- `LinearBlendSkinner` -> immutable `SkinnedMeshFrame` -> Minecraft 26.2 `SubmitNodeCollector.submitCustomGeometry` renderer bridge: NOT IMPLEMENTED / NOT TESTED.
- Real physical MODEL/ANIMATION resources and `presentation_assets` manifest: NOT IMPLEMENTED.
- Production texture/material, VFX/sound, scale/hitbox/deformation and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Do not repeat candidate selection/source fingerprint work unless the pinned immutable source check fails.
- Preserve source SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` and accepted derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` unless converter schema/version intentionally changes.
- Preserve `AttackPattern` as authoritative hit timing and ACTIVE-only damage semantics; animation sample time must be presentation-only.
- Preserve fail-closed presentation resolution, atomic client reload and exact content-generation matching.
- Do not permit source `Atlas` material/texture/image bytes into production resources.
- Do not restore legacy GeckoLib 4 paths or force GeckoMesh/GeoBone onto this asset.
- Do not approximate arbitrary Dragon triangles into cubes/bounding boxes or collapse multi-joint weights to a dominant bone.
- Preserve project-owned four-influence LBS deformation semantics; optimize only after profiler evidence.
- Do not weaken exact derivation SHA/count validation to make a changed asset load.
- Do not create placeholder production resources or a fake `presentation_assets` manifest.
- Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs and this handoff. Treat implementation `036cda32e6451b50af55a38ca19e579ab5b22628` / run `34301699458` as the green animation-import/sampling baseline.
2. Reacquire or otherwise make available the exact accepted sanitized Dragon derivation and run `GltfAnimationImporter` against all eight real source clips. Record exact clip/channel/interpolation coverage; if source data violates the implemented fail-closed contract, fix the first real mismatch without weakening semantics.
3. Implement the presentation-only semantic/`AttackPattern` -> clip/sample-time bridge. Damage and ACTIVE timing remain authoritative in combat state; no animation-owned clock.
4. Feed sampled matrices through existing `LinearBlendSkinner` to `SkinnedMeshFrame`, then wire immutable frame output to Minecraft 26.2 `SubmitNodeCollector.submitCustomGeometry`; validate client compile and Xvfb smoke.
5. Only after a real approved material/texture exists, perform scale/culling/UV/deformation/hitbox screen review and human field-play.
6. Only after physical model/animation/material/VFX/sound resources exist and existing atomic/generation gates pass, create the first real `presentation_assets` manifest.
