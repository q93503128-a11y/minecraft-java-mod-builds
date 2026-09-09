# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` verified at this run start: `9e3e704d1de302167b68466419099c74f4e5a688`.
- Latest verified Riftfrontier implementation/test HEAD: `3a9b8b2d5032216df3e9a56be617dcd8ab65e900`.
- `Dragon Evolved` remains selected as the Region 01 first-boss geometry/rig derivation source. Source `Atlas` art remains explicitly unapproved.

## Completed in this batch

M3 Region 01 boss exact prepared-geometry publication provenance:

- Recovered that `Region01BossGeometryPreparation` is already implemented on current `main`: accepted derivation bytes are read only from the exact staged `ValidatedReload.resourceManager()`, passed through `Region01BossRuntimeAsset.importAccepted(...)`, and invalidated by newer reload/clear races.
- `BossSkinnedMeshFrameSampler` now exposes the exact immutable `SkinnedMeshAsset` it deforms; `BossCustomGeometryRenderPipeline` exposes that same identity without copying/substituting it.
- `Region01BossClientRenderRuntime.publish(...)` no longer accepts a free-standing `ValidatedReload` plus an arbitrary geometry pipeline. Publication now requires the current `Region01BossGeometryPreparation.PreparedGeometry` capability.
- Publication rechecks stale reload state and content/publication generations, then requires the pipeline to consume the exact `runtimeAsset.skinnedMesh()` object imported from that prepared geometry. A separately imported, substituted, or stale mesh cannot be mixed into the validated reload at publish time.
- Existing server-authoritative animation timing, UUID actor identity, custom-geometry submission, four-influence LBS, and fail-closed reload behavior are preserved.
- Test fixture geometry weights were explicitly preserved after adding mesh-identity regression coverage; no unrelated fixture semantics were changed.
- No animation semantics were guessed, no source Atlas material was accepted, no fallback model/material was added, and no encounter/hitbox/balance tuning was introduced.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossSkinnedMeshFrameSampler.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/BossCustomGeometryRenderPipeline.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/Region01BossClientRenderRuntime.java`
- `src/test/java/kr/moonseungjun/riftfrontier/client/render/BossCustomGeometryRenderPipelineTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Baseline `9e3e704d1de302167b68466419099c74f4e5a688`, `Build Riftfrontier` run `34383211575`: FULL SUCCESS.
- Implementation/test HEAD `3a9b8b2d5032216df3e9a56be617dcd8ab65e900`, `Build Riftfrontier` run `34384850090`: FULL SUCCESS. Passed toolchain verification, asset intake tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb client smoke, executable JAR inspection, report generation, deliverable upload and logs/report upload.
- Direct visual inspection of the actual eight Dragon clip motions: NOT TESTED.
- Explicit approved logical attack-to-source-clip mappings: NOT AUTHORED.
- Accepted sanitized Dragon bytes are not yet packaged as the final renderer-consumable production MODEL resource. `THIRD_PARTY_ASSETS.md` explicitly keeps runtime-format conversion, `Used in` path, final material/texture and JAR inclusion pending.
- Approved material/texture/`RenderType` publication, actual Region 01 encounter attachment, final dimensions/hitbox/scale, spawned/deformed Dragon graphical capture, VFX/sound and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Exact Dragon source reacquisition and deterministic sanitizer acceptance are DONE. Pinned source SHA-256: `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`; accepted derivation SHA-256: `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`; provenance SHA-256: `3e16877a0043cf980ac8de05bb518834c5e77bd72d96103b4984c65a2a5a4c6c`.
- Preserve the exact eight source clips and `assets/sources/region_01_boss_dragon_evolved.animation_audit.json`; preserve `BossAnimationMotionReview` and never infer semantics from clip names, durations or channel metrics.
- Preserve exact-source/structure/inventory gates, staged exact-`ResourceManager` transaction, `Region01BossGeometryPreparation`, and the new rule that renderer publication must consume the exact prepared `SkinnedMeshAsset` identity.
- Preserve UUID-bound actor identity, monotonic server ticks, server-authoritative attack timing/ACTIVE-only damage, four-influence LBS, arbitrary triangle topology, custom geometry renderer and actor/render bridge.
- Do not allow source `Atlas` material/texture/image bytes into production. Do not restore GeckoLib/rigid-cube approximation as a shortcut.
- Do not attach the boss to encounter/natural spawning or invent final dimensions/hitbox/combat tuning before visual/source evidence and authored presentation exist. Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs, M3 resource/render code, `REFERENCE_TARGETS.md`, `THIRD_PARTY_ASSETS.md`, then this handoff.
2. If direct visual animation inspection is available, inspect all eight accepted Dragon clips and create `BossAnimationMotionReview` approvals only from concrete observed motion; author logical bindings only where observed semantics match server-authoritative states.
3. Otherwise do not guess. Advance the objective resource transaction: define the approved runtime packaging path for the already-selected CC0 sanitized art-neutral derivation, record the exact `Used in` resource/content IDs in `THIRD_PARTY_ASSETS.md`, and wire `RiftfrontierClientResources` to call `Region01BossGeometryPreparation.prepare(...)` from the currently staged `ValidatedReload` only after that physical resource exists in the same `ResourceManager` snapshot.
4. Construct `BossCustomGeometryRenderPipeline` only from that `PreparedGeometry.runtimeAsset().skinnedMesh()` plus a visually reviewed `BossAnimationSourceBinding`; keep approved material/texture/`RenderType` as an independent fail-closed prerequisite.
5. Once all three prerequisites derive from one current reload, publish atomically through the prepared-geometry-only `Region01BossClientRenderRuntime.publish(...)` gate. Only then attach the authored Region 01 encounter and perform actual spawned graphical capture before final hitbox/telegraph/VFX/sound tuning.
