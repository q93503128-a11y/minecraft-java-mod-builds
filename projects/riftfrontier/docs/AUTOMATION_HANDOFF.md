# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` verified at this run start: `f6ada6a1f9a55314b01cedeba7eef4ae89053149`.
- Current implementation HEAD for this batch: `377979168c6f38763b9d3f1a6e63d188def004f8`.
- `Dragon Evolved` remains the selected, sanitized and packaged Region 01 first-boss geometry/rig/unaltered-source-animation resource. Source `Atlas` art remains explicitly unapproved and excluded.
- Exact accepted runtime derivation SHA-256 remains `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.

## Completed in this batch

M3 Region 01 boss material/publication provenance gate:

- Confirmed the previous boss-semantic implementation run `34410900884` completed with `success`; its previously pending client/JAR/report gates are closed.
- Added `Region01BossMaterialPreparation`, a typed capability that accepts only an explicit reviewed material receipt plus reviewed `RenderType`, reads the reviewed texture from the exact `ResourceManager` retained by the current validated reload, verifies exact SHA-256, and remains invalid after any newer reload.
- `MaterialReview` records nonblank review evidence ID, exact texture resource ID, exact 64-hex SHA-256 and separate render-treatment evidence ID. The type does not approve or invent art by itself.
- Replaced the renderer's old free-standing `RenderType`/overlay/color publication seam. `Region01BossClientRenderRuntime.publish(...)` now accepts only a `PreparedMaterial` plus a pipeline and verifies exact material -> animation -> geometry -> reload provenance before atomic publication.
- Renderer-visible `SubmissionBinding` now retains `MaterialReview` provenance alongside reviewed animation, validated asset selection and render state.
- Extended `RiftfrontierClientResources` with material preparation/current-state lifecycle. Replacing animation, reloading resources, or any preparation failure clears stale material state.
- Added API-free regression tests for stale-before-read, exact resource-owner/hash, hash mismatch fail-closed, and reload-during-I/O rejection.
- No production texture, color treatment, `RenderType`, Dragon source `Atlas`, combat timing, hitbox, VFX or balance value was authored or inferred.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/client/render/Region01BossMaterialPreparation.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/Region01BossClientRenderRuntime.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/RiftfrontierClientResources.java`
- `src/test/java/kr/moonseungjun/riftfrontier/client/render/Region01BossMaterialPreparationTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Prior semantic implementation HEAD `d8d9b5b6717067227d6e62f6678250a5f50dba9f`, `Build Riftfrontier` run `34410900884`: FULL SUCCESS confirmed this run.
- Material-provenance implementation HEAD `377979168c6f38763b9d3f1a6e63d188def004f8`, `Build Riftfrontier` run `34415043369`.
- PASS at this handoff update: Java 25 toolchain verification, asset-intake tests, JUnit + clean build, required native GameTest.
- Dedicated-server smoke: IN PROGRESS at handoff update time. Xvfb graphical client smoke, executable-JAR inspection, report and artifact upload: PENDING. Do not call run `34415043369` FULL SUCCESS unless a later session confirms completion.
- Local clone/build: NOT RUN successfully in this session because the container could not resolve `github.com`; GitHub Actions is the actual validation source.
- Production final boss texture/material/`RenderType`: NOT APPROVED / NOT AUTHORED. Source `Atlas` remains prohibited.
- Production Region 01 `attack_pattern` numeric cadence/damage values and final `boss_profile`: NOT AUTHORED.
- Production `BossCombatSemanticProfile`, final reviewed source bindings connected to legitimate server attack keys, actual encounter attachment, final dimensions/hitbox/scale, spawned/deformed Dragon graphical capture, VFX/sound and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Exact Dragon source reacquisition, sanitizer acceptance, runtime packaging, eight-clip motion review, `Headbutt`/`Punch` source-window review, typed geometry preparation, typed reviewed-animation preparation, boss semantic schema/validator and the new typed material/publication provenance gate are DONE.
- Do not restore the old `publish(preparedAnimation, pipeline, renderType, overlay, color)` seam. Renderer publication must require `PreparedMaterial` tied to the same exact reload as reviewed animation and geometry.
- Do not treat `MaterialReview` construction as automatic approval. It must correspond to a recorded external/production art review with exact texture ID/hash and render-treatment evidence.
- Preserve source SHA-256 `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`, accepted derivation SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`, and provenance SHA-256 `3e16877a0043cf980ac8de05bb518834c5e77bd72d96103b4984c65a2a5a4c6c`.
- Never infer gameplay semantics from Dragon clip names/durations/channel metrics; source `ACTION` is not Minecraft damage authorization.
- Preserve exact `ResourceManager` staging, exact prepared mesh/animation identity, UUID actor identity, monotonic server ticks, ACTIVE-only authoritative damage, four-influence LBS and arbitrary triangle topology.
- Do not allow source `Atlas` bytes into production, restore GeckoLib/rigid-cube approximation, attach natural/encounter spawning, or invent final dimensions/hitbox/balance timing without evidence.

## Exact next start point

1. Re-check remote `main` and resolve `Build Riftfrontier` run `34415043369`; if any remaining gate failed, fix the first real failure before new feature work.
2. Re-read canonical docs and this handoff. Do not redo Dragon geometry/animation, boss semantic, or material-provenance gates.
3. Check whether an actually reviewed Region 01 boss final texture/material treatment has been added with immutable resource ID, SHA-256 and review evidence. Source `Atlas` is not a fallback.
4. If an approved material exists, instantiate the production `MaterialReview`, prepare it only through `RiftfrontierClientResources.prepareBossMaterial(...)`, then atomically publish the exact prepared material + exact pipeline through `Region01BossClientRenderRuntime.publish(preparedMaterial, pipeline)`.
5. If no approved material exists, do not invent one. Re-check production combat authoring; only if legitimate `attack_pattern`/`boss_profile` values now exist should the existing semantic/animation gates be connected. Never copy fixture timings.
6. Only after legitimate server semantics + reviewed animation + approved material share one current reload should spawned-boss graphical capture precede encounter attachment, final hitbox/telegraph tuning, VFX/sound or balance work.
