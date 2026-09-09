# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` recovered at this run start: `030c2b022c8b3adb8ead54c5a0c444b8b3ae7b9a`.
- Concurrent unrelated repository work was preserved. Immediately before the implementation push, remote `main` was `2ddb8ae5e0a6cc0eb5c41ef17f063d6d060f3194`; the Riftfrontier commit was fast-forwarded on top of that exact state.
- Previous renderer-neutral LBS baseline: implementation `a4322c82b55c1c98b4525d5452f5b0a3a3f088e7`, `Build Riftfrontier` run `34293588299`: full SUCCESS.
- `Dragon Evolved` remains selected only as Region 01 first-boss geometry/rig derivation source. Source `Atlas` art remains unapproved.

## Completed in this batch

M3 accepted art-neutral glTF -> runtime skinned-mesh importer:

- Added a deterministic embedded-glTF importer that reads actual POSITION, NORMAL, TEXCOORD_0, JOINTS_0, WEIGHTS_0 and triangle index accessors into `SkinnedTriangleMesh` without collapsing blend weights or topology.
- Added immutable `JointRig` / `SkinnedMeshAsset` runtime boundaries containing joint-node palette, nearest-joint hierarchy, rest-local transforms and inverse-bind matrices.
- Added glTF column-major MAT4 -> project `Affine3x4` conversion and TRS rest-pose conversion with finite/quaternion/hierarchy validation.
- Kept the accepted derivation fail-closed: Region 01 production import requires derivation SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` plus 4437 vertices / 7440 triangles / 46 joints.
- Explicitly reject non-empty materials/textures/images/samplers, primitive material bindings, external buffers, morph targets, sparse accessors and unsupported primitive/accessor forms.
- Added JUnit coverage using a real embedded binary glTF fixture for accessor/bufferView decoding, weights/joints, inverse-bind matrix, rest pose, forbidden material rejection and unpinned derivation rejection.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/mesh/GltfSkinnedMeshImporter.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/mesh/JointRig.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/mesh/SkinnedMeshAsset.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/mesh/Region01BossDerivationContract.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/mesh/GltfSkinnedMeshImporterTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Test-bearing implementation commit: `934ec45c4a02fcd3c5267103c031cb1b8fb4eb65`.
- `Build Riftfrontier` run `34297942339`: full `SUCCESS`.
- CI toolchain: SUCCESS.
- CI asset intake tool tests: SUCCESS.
- CI JUnit + clean build: SUCCESS.
- CI required native GameTest: SUCCESS.
- CI dedicated server smoke: SUCCESS.
- CI Xvfb client smoke: SUCCESS.
- CI executable JAR inspection: SUCCESS.
- CI build report + deliverable/log artifact upload: SUCCESS.
- Exact accepted 681773-byte sanitized Dragon derivation -> `Region01BossDerivationContract.importAccepted`: NOT RUN in this batch because the accepted glTF binary is not committed as a runtime resource and was not reacquired. The implementation is regression-tested against a synthetic embedded glTF fixture and pinned receipt metadata remains authoritative.
- Source clip accessor decoding / animation pose sampler: NOT IMPLEMENTED / NOT TESTED.
- Authoritative semantic/`AttackPattern` state -> animation sampling bridge: NOT IMPLEMENTED / NOT TESTED.
- Minecraft 26.2 `submitCustomGeometry` entity renderer bridge: NOT IMPLEMENTED / NOT TESTED.
- Real physical MODEL/ANIMATION resources and `presentation_assets` manifest: NOT IMPLEMENTED.
- Production texture/material, VFX/sound, scale/hitbox/deformation and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Do not repeat candidate selection/source fingerprint work unless the pinned immutable source check fails.
- Preserve source SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` and accepted derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` unless converter schema/version intentionally changes.
- Preserve `AttackPattern` as authoritative hit timing and ACTIVE-only damage semantics; do not create a second animation-owned combat clock.
- Preserve fail-closed presentation resolution, atomic client reload and exact content-generation matching.
- Do not permit source `Atlas` material/texture/image bytes into production resources. The importer intentionally rejects them.
- Do not restore legacy GeckoLib 4 paths or force GeckoMesh/GeoBone onto this asset.
- Do not approximate arbitrary Dragon triangles into cubes/bounding boxes or collapse multi-joint weights to a dominant bone.
- Preserve project-owned four-influence LBS deformation semantics; optimize only after profiler evidence.
- Do not weaken exact derivation SHA/count validation to make a changed asset load.
- Do not create placeholder production resources or a fake `presentation_assets` manifest.
- Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs and this handoff. Treat run `34297942339` as the green validation baseline for the accepted glTF -> skinned-mesh/rig importer.
2. Implement deterministic animation-channel import and pose sampling for the existing eight source clips over the same 46-joint hierarchy, preserving glTF interpolation semantics and normalized shortest-path quaternion interpolation where applicable.
3. Drive presentation sample time from authoritative semantic/`AttackPattern` state; do not add a second combat clock or move damage timing into animation data.
4. Feed sampled joint matrices through the existing `LinearBlendSkinner`, then wire immutable `SkinnedMeshFrame` output to Minecraft 26.2 `SubmitNodeCollector.submitCustomGeometry`; validate client compile and Xvfb smoke.
5. Only after a real approved material/texture exists, perform actual scale/culling/UV/deformation/hitbox screen review and human field-play.
6. Only after physical model/animation/material/VFX/sound resources exist and existing atomic/generation gates pass, create the first real `presentation_assets` manifest.
