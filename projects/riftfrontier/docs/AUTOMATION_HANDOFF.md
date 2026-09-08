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
- Inspected GeckoMesh's PolyMesh path: arbitrary faces are attached to a GeckoLib `GeoBone`, but the inspected path does not carry glTF per-vertex `JOINTS_0`/`WEIGHTS_0` linear-blend skinning data.
- Added a deterministic fail-closed glTF audit that decides whether a skinned mesh is exactly eligible for a rigid-bone PolyMesh renderer: all vertices must be single-weight and all triangles must remain within one dominant joint.
- Added regression coverage for eligible rigid geometry, blended skinning rejection, cross-bone triangle rejection, and missing skin attributes.
- Reacquired the exact creator-hosted `Dragon_Evolved.gltf`; 991,335 bytes and pinned SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` matched.
- Ran the new gate on those exact source bytes: 3,086 / 4,437 vertices use multiple positive joint weights and 1,192 / 7,440 triangles span dominant-joint boundaries. Result: `BOSS_RUNTIME_MESH_RIGID_POLYMESH_LOSSY`.
- Added an exact-source audit receipt and rejected the rigid GeckoMesh/GeoBone conversion path for this Dragon. GeckoMesh/GeckoLib were not added as dependencies.

## Changed systems/files

- `tools/audit_region01_boss_runtime_mesh.py` — glTF 2.0 skin-weight/triangle rigid-bone eligibility audit with stable decision/error codes.
- `tools/tests/test_audit_region01_boss_runtime_mesh.py` — four deterministic regression cases.
- `assets/sources/region_01_boss_dragon_evolved.runtime_mesh_audit.json` — exact-source rigid-bone rejection receipt.
- `docs/REGION_01_BOSS_RUNTIME_MESH_GATE.md` — renderer-format evidence, actual Dragon counts, rejection/selection rules.
- `docs/AUTOMATION_HANDOFF.md` — this recovery record.

## Verification

- Test-bearing implementation commit: `6f35a4abb901724309edf81e8091647a193919b7`.
- Evidence follow-up commit: `94a9a8c38e34a0d9aacc49c0053bc7e46245d899` (`[skip ci]`).
- `Build Riftfrontier` run `34289171362` for the test-bearing commit: full `SUCCESS`.
- CI toolchain: SUCCESS.
- CI asset intake tool tests: SUCCESS.
- CI clean tests/build: SUCCESS.
- CI required native GameTest: SUCCESS.
- CI dedicated server smoke: SUCCESS.
- CI Xvfb client smoke: SUCCESS.
- CI executable JAR inspection: SUCCESS.
- CI build report + deliverable/log artifact upload: SUCCESS.
- Local Python unittest for the new audit: 4/4 PASS.
- Exact creator-hosted Dragon source fingerprint: PASS.
- Exact source rigid-bone audit: EXECUTED / REJECTED AS LOSSY (3,086 blended vertices; 1,192 cross-bone triangles).
- Reproduction of the accepted sanitized derivation in this batch: NOT RUN; its previously pinned SHA remains `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- GeckoMesh runtime dependency for Dragon: REJECTED / NOT ADDED.
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
- Treat rigid GeckoMesh/GeoBone conversion for this Dragon as closed/rejected unless verified per-vertex blend-skinning support appears later.
- Do not add GeckoMesh/GeckoLib merely to force this source through a lossy path.
- Do not create placeholder production resources or a fake `presentation_assets` manifest.
- Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs and this handoff.
2. If the exact accepted sanitized artifact is reacquired/reproduced, run the new audit against it once to confirm the source-level rejection survives the existing art-stripping conversion; do not repin the accepted hash to make it pass.
3. Then investigate and qualify a Minecraft 26.2 renderer path that preserves arbitrary triangle geometry plus per-vertex linear-blend skinning and the existing 46-joint/8-clip contract. Prefer a bounded dependency or small project-owned renderer boundary over lossy conversion.
4. If no maintainable 26.2 renderer exists, define a separate production re-authoring gate that preserves boss silhouette and attack-bearing rig intent rather than silently collapsing weights or geometry.
5. Only with a concrete renderer + real physical model/animation resources: wire the boss entity/renderer through existing ResourceProbe, atomic reload and generation-match gates.
6. Only after every referenced physical resource exists and client loadability passes, create the first real `presentation_assets` manifest and continue to attack presentation, scale/hitbox/deformation, VFX/sound and human field-play gates.
