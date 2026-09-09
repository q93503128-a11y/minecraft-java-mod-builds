# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` at this run start: `e52f3bc2887217cba501be792d3265440fc4b24b`.
- Latest verified Riftfrontier implementation/test HEAD in this run: `5a6a43ce99a5cf0dfbdd180733d9f291dfeec5aa`.
- `Dragon Evolved` remains selected only as Region 01 first-boss geometry/rig derivation source. Source `Atlas` art remains unapproved.

## Completed in this batch

M3 boss client resource-reload fail-closed hardening:

- `BossPresentationClientAssetRuntime.reload(...)` is now synchronized and rejects content-generation rollback while still allowing same-generation resource-pack revalidation.
- A selected production manifest that fails physical client-resource validation now publishes an explicit inactive snapshot for the attempted generation before throwing; a resource pack that removed required files can no longer leave the previous pack's `ready` presentation snapshot alive.
- The real NeoForge client resource listener now starts a new `Region01BossClientRenderRuntime` reload generation before validating the incoming pack. This immediately invalidates any custom-geometry/material binding derived from the previous pack.
- The generation ticket is intentionally not retained yet: until the accepted Dragon runtime bytes, explicit animation binding, and approved material/`RenderType` loader are all connected in this listener, a resource reload must remain renderer-fail-closed rather than republish partial state.
- Added JUnit coverage for failed-candidate deactivation, same-generation resource-pack revalidation, stale content-generation rejection, successful publication, and explicit inactive publication.
- Preserved UUID actor identity, server-authoritative animation timing, exact-source/structure/inventory gates, four-weight LBS, custom geometry, and generation-scoped render publication.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationClientAssetRuntime.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/RiftfrontierClientResources.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationClientAssetRuntimeTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Implementation commit: `5a6a43ce99a5cf0dfbdd180733d9f291dfeec5aa`.
- `Build Riftfrontier` run `34356034991`: FULL SUCCESS.
- Passed: toolchain, asset-intake tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb client smoke, executable JAR inspection, report generation, deliverables upload, logs/reports upload.
- A public GitHub mirror containing `Dragon_Evolved.gltf` was located, but the connector could not expose the complete ~991 KB file as hashable local bytes; the mirror was therefore NOT accepted as canonical evidence.
- Exact source `Dragon_Evolved.gltf` SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`: NOT REACQUIRED / NOT REVERIFIED in this execution environment.
- Exact accepted sanitized derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`: NOT REACQUIRED in this execution environment.
- Real eight-clip duration/channel/path/interpolation receipt: NOT PRODUCED.
- Direct visual motion inspection and explicit logical attack-to-source-clip mapping: NOT TESTED / NOT IMPLEMENTED.
- Approved material/texture/`RenderType` publication, actual Region 01 encounter attachment, final dimensions/hitbox/scale, spawned/deformed Dragon graphical capture, VFX/sound and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Preserve UUID-bound presentation identity end-to-end; production render lookup must never regress to numeric entity id alone.
- Preserve same-UUID monotonic server-tick rejection and fresh watermark behavior when a new UUID reuses a numeric entity id.
- Preserve generation-ticket publication: a stale reload completion must never publish after a newer `beginReload()` or `clear()`.
- Every client resource-pack reload must invalidate the previous Region 01 custom-geometry binding before inspecting the candidate pack.
- Physical presentation-resource validation failure must remain fail-closed/inactive for the attempted content generation; do not restore the old behavior that preserved a previous `ready` snapshot.
- Content-generation publication must not move backwards; same-generation resource-pack revalidation remains allowed.
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
2. Reacquire the exact original `Dragon_Evolved.gltf` SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` from recorded Quaternius provenance, then deterministically regenerate/verify accepted sanitized derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`; reject mirrors/substitutes unless their complete bytes independently match the recorded SHA.
3. Run `python tools/audit_region01_boss_animation_clips.py <accepted.gltf> assets/sources/region_01_boss_dragon_evolved.acceptance.json --output <receipt.json>` and load the exact same accepted bytes through `Region01BossRuntimeAsset.importAccepted(...)`.
4. Inspect actual clip motion and author explicit logical bindings only where semantics match; never infer mapping from clip names alone.
5. Extend the existing `RiftfrontierClientResources` listener to retain its `Region01BossClientRenderRuntime.beginReload()` ticket through preparation and generation-bound `publish(...)`, but only after it can construct the verified runtime asset, explicit binding, and approved material/`RenderType` from the same `ResourceManager` snapshot.
6. Attach the production boss encounter only after those gates, then perform spawned graphical capture before final hitbox/telegraph/VFX/sound tuning.
