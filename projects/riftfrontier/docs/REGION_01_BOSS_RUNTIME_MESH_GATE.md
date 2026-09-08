# Region 01 Boss Runtime Mesh Gate

Status: `ACTIVE — renderer strategy qualification`

This document records the renderer-format decision boundary for the selected Region 01 first-boss source, Quaternius `Dragon Evolved`. It does not approve final art, a renderer dependency, or a production `presentation_assets` manifest.

## Locked source facts

The existing source/derivation gates remain authoritative:

- source SHA-256: `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`
- accepted art-neutral derivation SHA-256: `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`
- 1 mesh, 1 skin, 46 joints, 4,437 vertices, 7,440 triangles, 8 source clips
- original `Atlas` material/texture remains forbidden as production art

Do not repin these values merely to make a renderer conversion pass.

## Why ordinary GeckoLib cube conversion is rejected

The selected source is a general triangle mesh with a glTF skin. A generic glTF -> GeckoLib/Bedrock cube importer is not a lossless representation contract. Public importer implementations explicitly approximate arbitrary polygon geometry with cubes/bounding boxes when the source is not already box-authored. Riftfrontier therefore does not treat successful import as proof of visual or deformation fidelity.

`geckolib-model-importer` and CubePort remain research references only. They are not approved as the Dragon production conversion path.

## GeckoMesh investigation

GeckoMesh 1.2.0 is currently published for Minecraft 26.2 NeoForge and is MIT-licensed. Its source shows that it accepts GeckoLib `PolyMesh` position/UV/polygon data and appends the resulting faces to a `GeoBone` during model baking. This is materially better than bounding-box cube approximation for static polygon geometry.

However, the inspected implementation attaches a `PolyMesh` to one GeckoLib bone and transforms that geometry with the bone. The `PolyMesh` schema consumed by GeckoMesh contains positions, normals, UVs and polygon indices, but no glTF `JOINTS_0`/`WEIGHTS_0` linear-blend skinning data. Therefore it must not be assumed to preserve a skinned glTF mesh exactly.

Decision: `REJECTED FOR DRAGON RIGID-SKIN PATH`. The exact pinned Dragon source fails the rigid-bone eligibility audit below. GeckoMesh may remain a research reference for genuinely rigid-bone PolyMesh assets, but it must not be used to convert this selected Dragon by discarding or collapsing skin weights. Do not add GeckoMesh or GeckoLib for the Dragon path solely because the library can render arbitrary faces.

## Deterministic rigid-bone eligibility gate

`tools/audit_region01_boss_runtime_mesh.py` inspects a glTF 2.0 skinned primitive and answers one narrow question:

> Can every rendered vertex and triangle be represented by assigning the entire triangle mesh pieces rigidly to GeckoLib bones without changing deformation?

The audit fails closed unless:

1. every skinned primitive has `JOINTS_0` and `WEIGHTS_0`;
2. skin weights are valid and sum to 1;
3. every vertex has exactly one positive joint weight;
4. every triangle's vertices resolve to the same dominant joint.

Any multi-joint blended vertex or triangle spanning dominant joints produces `BOSS_RUNTIME_MESH_RIGID_POLYMESH_LOSSY` and rejects the rigid-bone PolyMesh path. This is a format-capability gate, not a subjective visual threshold.

## Current verification state

- synthetic rigid single-bone fixture: PASS
- synthetic linear-blend fixture: correctly REJECTED
- synthetic cross-bone triangle fixture: correctly REJECTED
- malformed/missing skin attributes: fail-closed PASS
- exact creator-hosted `Dragon_Evolved.gltf` reacquired: PASS, 991,335 bytes, pinned SHA-256 matched
- exact Dragon rigid-bone audit: REJECTED as lossy — 3,086 / 4,437 vertices use multiple positive joint weights; 1,192 / 7,440 triangles span dominant-joint boundaries; 45 dominant joints are represented
- deterministic receipt: `assets/sources/region_01_boss_dragon_evolved.runtime_mesh_audit.json`
- in-Minecraft PolyMesh rendering: `NOT RUN` because the capability gate already rejects this renderer contract for Dragon
- Dragon renderer dependency selection: `NOT SELECTED`; next path must preserve triangle geometry plus linear-blend skinning

## Next decision

The exact pinned source already exits with `BOSS_RUNTIME_MESH_RIGID_POLYMESH_LOSSY`. Do not repeat the GeckoMesh rigid-bone experiment unless that library gains verified per-vertex blend-skinning support.

The next renderer investigation must support the accepted triangle mesh plus glTF-style linear-blend skinning without collapsing weights or approximating geometry. A deliberately re-authored Minecraft-native production model remains a fallback only under a separate art/rig review; it is not permission to silently downgrade the selected source.
