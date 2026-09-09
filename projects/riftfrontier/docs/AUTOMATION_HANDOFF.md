# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` at this run start: `81464606485e511fd41dd4615eada63e918a6dfb`.
- Latest verified Riftfrontier implementation/test HEAD in this run: `8b4a3ee7a5c14eb6fb654b7a3aa1e757411b007f`.
- `Dragon Evolved` remains selected only as Region 01 first-boss geometry/rig derivation source. Source `Atlas` art remains unapproved.

## Completed in this batch

M3 boss renderer resource-publication generation hardening:

- Added a lock-free `GenerationPublicationSlot<T>` for complete client-resource publication.
- Every preparation cycle now obtains an opaque generation ticket; starting a newer cycle immediately clears the current binding and makes all older tickets stale.
- `Region01BossClientRenderRuntime` no longer permits unscoped direct publication. Callers must `beginReload()` and publish with that exact ticket.
- `clear()` now increments the publication generation as well as clearing the active binding, preventing an in-flight older reload from resurrecting resources after lifecycle invalidation.
- Foreign-slot tickets are rejected and generation exhaustion fails closed.
- Preserved current UUID-bound actor lookup, authoritative presentation timing, exact-source gates, no-fallback renderer behavior, and custom-geometry submission pipeline.
- Added deterministic JUnit coverage for newer-reload invalidation, stale async completion rejection, lifecycle clear invalidation, foreign-ticket rejection, and same-generation atomic replacement.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/client/render/GenerationPublicationSlot.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/Region01BossClientRenderRuntime.java`
- `src/test/java/kr/moonseungjun/riftfrontier/client/render/GenerationPublicationSlotTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Implementation commit: `8b4a3ee7a5c14eb6fb654b7a3aa1e757411b007f`.
- `Build Riftfrontier` run `34349638133`: FULL SUCCESS.
- Passed: toolchain, asset-intake tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb client smoke, executable JAR inspection, report generation, deliverables upload, logs/reports upload.
- Exact source `Dragon_Evolved.gltf` SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`: NOT REACQUIRED in this execution environment.
- Exact accepted sanitized derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`: NOT REACQUIRED in this execution environment.
- Real eight-clip duration/channel/path/interpolation receipt: NOT PRODUCED.
- Direct visual motion inspection and explicit logical attack-to-source-clip mapping: NOT TESTED / NOT IMPLEMENTED.
- Approved material/texture/`RenderType` publication, actual Region 01 encounter attachment, final dimensions/hitbox/scale, spawned/deformed Dragon graphical capture, VFX/sound and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Preserve UUID-bound presentation identity end-to-end; production render lookup must never regress to numeric entity id alone.
- Preserve same-UUID monotonic server-tick rejection and fresh watermark behavior when a new UUID reuses a numeric entity id.
- Preserve generation-ticket publication: a stale reload completion must never publish after a newer `beginReload()` or `clear()`.
- Do not reintroduce direct unscoped `Region01BossClientRenderRuntime.publish(...)` publication.
- Preserve source SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` and accepted derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` unless the converter schema/version intentionally changes.
- Preserve exact source clip inventory names unless direct accepted-source inspection proves a source revision changed.
- Do not hard-code guessed clip durations/channel metrics or infer logical mappings from clip names.
- Do not bypass exact-SHA/exact-structure/exact-inventory gates or construct the production runtime asset from independently substituted bytes.
- Preserve `Region01BossRuntimeAsset` as the composed production import seam.
- Preserve `AttackPattern`/server semantic state as authoritative timing and ACTIVE-only damage semantics. No presentation layer may become a free-running combat clock.
- Preserve monotonic client semantic snapshot ordering and fail-closed presentation resolution.
- Preserve the registered `region_01_boss` actor/render-state/renderer bridge and `BossCustomGeometryRenderPipeline`; do not replace them with fallback art or a parallel renderer clock.
- Do not add the boss to encounter/natural spawning or invent final dimensions/hitbox/combat tuning before visual/source evidence and authored content exist.
- Do not permit source `Atlas` material/texture/image bytes into production resources.
- Do not restore GeckoLib 4 paths or rigid GeoBone/cube approximation for this asset.
- Preserve arbitrary triangle topology, four-influence LBS, imported rig/animations, pose sampler, semantic sample bridge, frame sampler, exact inventory gate and custom-geometry adapter.
- Keep Minecraft 26.2 `net.minecraft.client.renderer.rendertype.RenderType` and `net.minecraft.client.renderer.state.level.CameraRenderState` boundaries.
- Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs and this handoff.
2. Reacquire the exact original `Dragon_Evolved.gltf` SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` from recorded Quaternius provenance, then deterministically regenerate/verify the accepted sanitized derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`; reject substitutes.
3. Run `python tools/audit_region01_boss_animation_clips.py <accepted.gltf> assets/sources/region_01_boss_dragon_evolved.acceptance.json --output <receipt.json>` and load the exact same accepted bytes through `Region01BossRuntimeAsset.importAccepted(...)`.
4. Inspect actual clip motion and author explicit logical bindings only where semantics match; never infer mapping from clip names alone.
5. Implement the real resource-reload preparation path using `Region01BossClientRenderRuntime.beginReload()` and generation-bound `publish(...)`, then publish only the verified runtime asset plus approved material/`RenderType`.
6. Attach the production boss encounter only after those gates, then perform spawned graphical capture before final hitbox/telegraph/VFX/sound tuning.
