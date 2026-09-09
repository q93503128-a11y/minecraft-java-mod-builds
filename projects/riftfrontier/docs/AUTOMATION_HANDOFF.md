# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` recovered at this run start: `5bc78e58c6b58405faf7bacb49a1f8e110a33380`.
- Previous test-bearing correctness commit `6f35a4abb901724309edf81e8091647a193919b7`, `Build Riftfrontier` run `34289171362`: full SUCCESS.
- `Dragon Evolved` remains selected only as Region 01 first-boss geometry/rig derivation source. Source `Atlas` art remains unapproved.

## Completed in this batch

M3 project-owned linear-blend skinning runtime boundary:

- Re-read canonical/build/quality/project/roadmap/runtime-mesh handoff state from current remote `main`.
- Re-checked current NeoForge rendering direction: feature submission supports arbitrary custom geometry through `SubmitNodeCollector.submitCustomGeometry` and a `VertexConsumer` callback, so triangle topology is not itself a blocker.
- Kept the previous rigid GeckoMesh/GeoBone rejection closed; no weight collapse or cube approximation was restored.
- Added a renderer-neutral immutable skinned-triangle format with four joint/weight lanes per vertex.
- Added a deterministic CPU four-influence linear-blend skinning reference implementation preserving indexed triangle topology and UVs.
- Added fail-closed validation for malformed streams, non-finite values, bad weight sums, out-of-range indices/joints and degenerate skinned normals.
- Added JUnit regression coverage for two-joint blended deformation, identity pose preservation, invalid joint palette rejection and malformed weight rejection.
- Added the explicit renderer qualification document and staged custom-geometry path.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/mesh/Affine3x4.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/mesh/SkinnedTriangleMesh.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/mesh/LinearBlendSkinner.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/mesh/SkinnedMeshFrame.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/mesh/LinearBlendSkinnerTest.java`
- `docs/REGION_01_BOSS_SKINNED_RENDERER_GATE.md`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Pre-push static contract review: COMPLETE.
- New JUnit regression cases: committed; CI result must be checked before claiming SUCCESS.
- Clean Gradle build: PENDING CI after push.
- Required native GameTest: PENDING CI after push.
- Dedicated server smoke: PENDING CI after push.
- Xvfb client smoke: PENDING CI after push.
- Executable JAR inspection/report/artifact: PENDING CI after push.
- Exact Dragon source was not reacquired in this batch: NOT RUN; pinned source SHA remains `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`.
- Accepted sanitized derivation reproduction: NOT RUN; pinned derivation SHA remains `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- Actual derivation -> `SkinnedTriangleMesh` importer: NOT IMPLEMENTED / NOT TESTED.
- 46-joint inverse-bind/hierarchy animation sampler: NOT IMPLEMENTED / NOT TESTED.
- Minecraft 26.2 `submitCustomGeometry` entity renderer bridge: NOT IMPLEMENTED / NOT TESTED.
- Real physical MODEL/ANIMATION resources and `presentation_assets` manifest: NOT IMPLEMENTED.
- Production texture/material, VFX/sound, scale/hitbox/deformation and field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Do not repeat candidate selection/source fingerprint work unless the pinned immutable source check fails.
- Preserve source SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` and accepted derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` unless converter schema/version intentionally changes.
- Preserve `AttackPattern` as authoritative hit timing and ACTIVE-only damage semantics.
- Preserve fail-closed presentation resolution, atomic client reload and exact content-generation matching.
- Do not permit source `Atlas` material/texture/image bytes into production resources.
- Do not restore legacy GeckoLib 4 paths.
- Do not silently approximate arbitrary Dragon triangles into cubes/bounding boxes or collapse multi-joint weights to a dominant bone.
- Keep the rigid GeckoMesh/GeoBone path closed for this Dragon unless verified per-vertex blend-skinning support appears later.
- Do not add GeckoLib/GeckoMesh just to force a lossy conversion.
- Do not create placeholder production resources or a fake `presentation_assets` manifest.
- Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs, this handoff and the CI run triggered by this batch; fix the first real failure if any.
2. Implement a deterministic accepted-derivation importer that produces `SkinnedTriangleMesh` plus 46-joint hierarchy/inverse-bind data and validates the pinned source/derivation contract.
3. Implement animation pose sampling for the existing source clips/joint hierarchy without creating a second combat clock; presentation sampling follows authoritative semantic/`AttackPattern` state.
4. Wire immutable `SkinnedMeshFrame` output to a Minecraft 26.2 entity renderer through `SubmitNodeCollector.submitCustomGeometry` and validate actual client compile/smoke.
5. Only after a real approved material/texture exists, perform scale/culling/UV/deformation/hitbox screen review.
6. Only after every physical resource exists and the existing atomic/generation gates pass, create the first real `presentation_assets` manifest.
