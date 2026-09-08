# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Canonical documents were re-read from remote `main` in the required order.
- Remote `main` at run start: `aebf0dd9b68a48fdadd2b2fdc5e86cd93e708b51`.
- Previous correctness commit `638e942a77f4d20858094770e89beeea0a4072d2`, `Build Riftfrontier` run `34285487373`: full SUCCESS.
- `Dragon Evolved` remains selected only as Region 01 first-boss geometry/rig derivation source. Source `Atlas` art remains unapproved.

## Completed in this batch

M3 runtime mesh / skinning qualification gate:

- Inspected current GeckoLib/Blockbench conversion options instead of assuming arbitrary glTF triangle meshes can become GeckoLib cubes losslessly.
- Confirmed common glTF -> GeckoLib cube import routes are approximation paths for arbitrary polygon meshes and rejected them as production proof for the selected Dragon.
- Inspected GeckoMesh 1.2.0 source. It is published for Minecraft 26.2 NeoForge and can feed arbitrary PolyMesh faces into GeckoLib `GeoBone`, but the inspected PolyMesh path carries positions/normals/UVs/polygons attached to a bone and does not carry glTF per-vertex `JOINTS_0`/`WEIGHTS_0` blend weights.
- Added a deterministic fail-closed glTF audit that decides whether a skinned mesh is exactly eligible for a rigid-bone PolyMesh renderer: all vertices must be single-weight and all triangles must remain within one dominant joint.
- Added regression coverage for eligible rigid geometry, blended skinning rejection, cross-bone triangle rejection, and missing skin attributes.
- Added a decision record preventing GeckoMesh/GeckoLib dependency addition or production resource generation until the exact accepted Dragon derivation passes this gate.

## Changed systems/files

- `tools/audit_region01_boss_runtime_mesh.py` — glTF 2.0 skin-weight/triangle rigid-bone eligibility audit with stable decision/error codes.
- `tools/tests/test_audit_region01_boss_runtime_mesh.py` — four deterministic regression cases.
- `docs/REGION_01_BOSS_RUNTIME_MESH_GATE.md` — renderer-format evidence, rejection/selection rules, exact next decision.
- `docs/AUTOMATION_HANDOFF.md` — this recovery record.

## Verification

- Local Python unittest for new audit: 4/4 PASS.
- Full repository CI after push: see current run status; do not infer SUCCESS from local tests.
- Exact accepted Dragon derivation through new rigid-bone audit: NOT RUN; source/accepted derivation bytes are intentionally not committed to the public repository.
- GeckoMesh runtime dependency: NOT ADDED / NOT TESTED.
- GeckoLib runtime dependency + boss entity/renderer: NOT IMPLEMENTED / NOT TESTED.
- Real renderer-consumable Region 01 boss MODEL/ANIMATION resources: NOT IMPLEMENTED.
- Real `presentation_assets` manifest: NOT IMPLEMENTED.
- Production texture/material, VFX/sound, scale/hitbox/deformation and field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Do not repeat candidate selection or source fingerprint work unless the pinned source fails an immutable-source check.
- Preserve source SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` and accepted derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` unless converter schema/version intentionally changes.
- Preserve `AttackPattern` as authoritative hit timing and ACTIVE-only damage semantics.
- Preserve fail-closed presentation resolution, atomic client reload and exact content-generation matching.
- Do not permit source `Atlas` material/texture/image bytes into production resources.
- Do not restore legacy GeckoLib 4 `geo/` or unscoped `animations/` roots.
- Do not silently approximate the selected Dragon triangle mesh into bounding-box/cube geometry.
- Do not treat arbitrary-face PolyMesh support as proof that glTF linear-blend skinning is preserved.
- Do not add GeckoMesh/GeckoLib until a concrete real renderer/resource path has passed the exact-source capability gate.
- Do not create placeholder production resources or a fake `presentation_assets` manifest.
- Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs and this handoff.
2. Reacquire exact `Dragon_Evolved.gltf` with SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` and reproduce the accepted sanitized derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` using existing gates.
3. Run `tools/audit_region01_boss_runtime_mesh.py <accepted.gltf> --require-lossless-rigid-bone` and record the actual blended-vertex / cross-bone-triangle counts.
4. If `BOSS_RUNTIME_MESH_RIGID_POLYMESH_LOSSY`, reject rigid GeckoMesh conversion for this source and implement/qualify a renderer path that preserves triangle geometry plus linear-blend skinning, or separately re-author the production model under the art/rig gate. Do not reduce weights or geometry merely to pass.
5. If eligible, freshly verify GeckoMesh + GeckoLib 26.2 coordinates, add them only with the first concrete boss entity/renderer and real `geckolib/models/...` / `geckolib/animations/...` resources.
6. Only after every referenced physical resource exists and client loadability passes, create the first real `presentation_assets` manifest and continue to attack presentation, scale/hitbox/deformation, VFX/sound and human field-play gates.
