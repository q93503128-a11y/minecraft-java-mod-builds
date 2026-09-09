# Region 01 Boss Skinned Renderer Qualification

Status: `ACTIVE — project-owned LBS path qualified at renderer-neutral boundary`

This document extends `REGION_01_BOSS_RUNTIME_MESH_GATE.md`. It does not approve final art, a production texture/material, boss entity registration, or a real `presentation_assets` manifest.

## Decision

The selected Dragon source cannot use the previously rejected rigid GeoBone/PolyMesh path because 3,086 / 4,437 source vertices use multiple positive joint weights and 1,192 / 7,440 triangles cross dominant-joint boundaries.

The project will therefore keep arbitrary indexed triangle geometry and four-influence linear-blend skinning intact. No cube/bounding-box approximation and no dominant-bone weight collapse is permitted.

## Why a project-owned renderer path is viable

NeoForge's current feature renderer exposes `SubmitNodeCollector.submitCustomGeometry(PoseStack, RenderType, CustomGeometryRenderer)`. The custom renderer receives the snapshotted pose plus a `VertexConsumer` and is specifically intended to define arbitrary uploaded vertices. This means the remaining blocker is not triangle support; it is faithful pose sampling and skinning before vertex submission.

Riftfrontier therefore uses this staged boundary:

```text
accepted art-neutral triangle mesh
+ sampled 46-joint pose
+ inverse-bind-aware skin matrices
→ project-owned four-weight CPU LBS
→ immutable SkinnedMeshFrame
→ NeoForge submitCustomGeometry / VertexConsumer
→ RenderType + approved production material
```

The first four stages are renderer-neutral and testable without a Minecraft client. The final submission stage remains client-only and must be wired only after a real physical model/animation resource loader exists.

## Implemented runtime-neutral contract

Package: `kr.moonseungjun.riftfrontier.combat.presentation.mesh`

- `Affine3x4` — immutable finite affine joint skin matrix.
- `SkinnedTriangleMesh` — immutable indexed triangles with position, normal, UV, `JOINTS_0`-style four joint lanes and normalized `WEIGHTS_0`-style four weight lanes.
- `LinearBlendSkinner` — CPU reference LBS preserving topology/UVs while blending positions and normals from four joint influences.
- `SkinnedMeshFrame` — immutable renderer-facing frame snapshot; no Minecraft client object can mutate authoritative mesh input.

Fail-closed rules:

- malformed stream lengths are rejected;
- non-finite geometry/weights/matrices are rejected;
- weights must be non-negative and sum to one within a narrow import tolerance;
- triangle indices must remain in range;
- a positive-weight joint outside the supplied matrix palette is rejected;
- degenerate/non-finite skinned normals are rejected.

## Performance boundary

The selected source is 4,437 vertices / 7,440 triangles / four weight lanes. A reference CPU pass is bounded by the mesh vertex count and four influences per vertex; it does not scan world/entity collections. The first renderer should reuse parsed immutable mesh data and allocate/publish one frame snapshot per rendered boss pose rather than reparsing glTF or rebuilding content graphs per frame.

Actual frame-time and allocation budgets are `NOT TESTED`; use profiler evidence before optimizing or moving skinning to a custom GPU shader.

## Still not approved

- actual glTF runtime loader / accepted derivation loader
- inverse bind + hierarchy pose sampler for the source 46-joint contract
- source 8-clip animation sampler and attack-specific authored clips
- NeoForge entity renderer and `submitCustomGeometry` bridge in Minecraft 26.2
- approved production texture/material / RenderType
- scale, culling bounds, hitbox/deformation alignment
- VFX/sound timing
- first real `presentation_assets` manifest
- human field-play / screenshot review

## Next gate

1. Add a deterministic importer from the accepted art-neutral derivation into `SkinnedTriangleMesh` plus joint hierarchy/inverse-bind data; reject source drift.
2. Add pose/animation sampling that produces the exact joint skin-matrix palette without inventing a second combat clock. `AttackPattern` remains authoritative for hit timing.
3. Wire `SkinnedMeshFrame` into a Minecraft 26.2 entity renderer through `submitCustomGeometry` and verify client compilation/smoke.
4. Only after a real approved material exists, validate texture coordinates, culling, scale and deformation on screen.
5. Only after every physical resource exists and the existing atomic/generation gates pass may the first real `presentation_assets` manifest be published.
