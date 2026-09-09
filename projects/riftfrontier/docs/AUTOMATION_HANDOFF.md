# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` at this run start: `e61b70108789e9ad9de03bb5f783fe7b4ec2ca6a`.
- Remote advanced concurrently through unrelated project work to `34d53e75422917c75318fd3fd598e1ed05aab12e` before Riftfrontier writes; those changes were preserved and the Riftfrontier commits were appended without force push.
- Previous deterministic animation-audit implementation `4053119fb9a607053b321de0cb050a3b252a3a59`, run `34327050109`, was re-checked this session and is FULL SUCCESS.
- `Dragon Evolved` remains selected only as Region 01 first-boss geometry/rig derivation source. Source `Atlas` art remains unapproved.

## Completed in this batch

M3 composed accepted boss runtime-asset gate:

- Added `Region01BossRuntimeAsset` as the single renderer-neutral production import seam joining the exact accepted mesh/rig derivation with its imported source animation channels.
- `importAccepted(...)` first enforces the accepted derivation SHA/4437-vertex/7440-triangle/46-joint contract, then imports animation channels against that exact rig and enforces the independently verified eight-source-clip inventory.
- The bundle deliberately performs no logical attack mapping, material/texture selection, presentation clock, combat timing, or art fallback.
- Construction also fails closed on structural or source-clip inventory drift, preventing independently valid-looking pieces from being recombined into an unverified production runtime asset.
- Added JUnit regression coverage for non-accepted SHA rejection, accepted structural/inventory composition, and inventory drift rejection.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/mesh/Region01BossRuntimeAsset.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/mesh/Region01BossRuntimeAssetTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Runtime-asset implementation commit: `10fafe0dd1d8b05a5f27f6b93eb6cb83a8a01425`.
- Regression-test commit: `d761313aabd20d676a44851da8f8921fe961e04c`.
- `Build Riftfrontier` run `34332734136`: IN PROGRESS at handoff update. Confirmed SUCCESS so far: toolchain, asset-intake tests, JUnit + clean build, required native GameTest. Dedicated-server smoke was still running; client smoke, executable JAR inspection, report/artifact upload were NOT YET COMPLETE. Do not promote this run to FULL SUCCESS until GitHub reports completion.
- Prior run `34327050109`: FULL SUCCESS after fresh re-check this session.
- Exact accepted sanitized Dragon derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` was NOT reacquired in this execution environment. Recorded Quaternius/Drive provenance remains authoritative; no substitute mirror/file was treated as equivalent.
- Real eight-clip duration/channel/path/interpolation receipt: NOT PRODUCED because the exact accepted binary remains unavailable here.
- Explicit logical production mapping from attack animation keys to real source clips: NOT IMPLEMENTED pending actual accepted-source motion inspection.
- Concrete production boss entity/render-state/entity-renderer calling `BossCustomGeometryRenderPipeline.submitCurrent(...)`: NOT IMPLEMENTED.
- Actual spawned/deformed Dragon graphical capture, approved material/texture, `presentation_assets` manifest, scale/culling/UV/deformation/hitbox/VFX/sound/human field-play review: NOT TESTED / NOT IMPLEMENTED.

## Do not repeat or revert

- Preserve source SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` and accepted derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` unless converter schema/version intentionally changes.
- Preserve exact source clip inventory names unless direct accepted-source inspection proves a source revision changed.
- Do not hard-code guessed clip durations/channel metrics or infer logical mappings from clip names.
- Do not bypass exact-SHA/exact-structure/exact-inventory gates or construct the Region 01 production runtime asset from independently substituted bytes.
- Preserve `Region01BossRuntimeAsset` as the composed production import seam; renderer integration should consume its verified mesh/rig + animation inventory rather than re-importing pieces ad hoc.
- Preserve `AttackPattern`/server semantic state as authoritative timing and ACTIVE-only damage semantics. No presentation layer may become a free-running combat clock.
- Preserve monotonic client semantic snapshot ordering and fail-closed presentation resolution.
- Do not permit source `Atlas` material/texture/image bytes into production resources.
- Do not restore legacy GeckoLib 4 paths or rigid GeoBone/cube approximation for this asset.
- Preserve arbitrary triangle topology, four-influence LBS, imported rig/animations, pose sampler, semantic sample bridge, frame sampler, verified inventory/binding gate, custom-geometry adapter and production-facing render pipeline.
- Keep Minecraft 26.2 `net.minecraft.client.renderer.rendertype.RenderType` boundary.
- Do not weaken exact derivation SHA/count validation or create fake physical presentation assets/manifests.
- Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs and this handoff. First close the final status of `Build Riftfrontier` run `34332734136`; if it failed, fix the first real failure without reducing scope.
2. Reacquire the exact accepted sanitized derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` from the recorded original Quaternius/Drive provenance; reject substitutes whose bytes do not match.
3. Run `python tools/audit_region01_boss_animation_clips.py <accepted.gltf> assets/sources/region_01_boss_dragon_evolved.acceptance.json --output <receipt.json>` and also load the same bytes through `Region01BossRuntimeAsset.importAccepted(...)`; commit real metrics only if both exact gates pass.
4. Inspect actual clip motion and author explicit logical source bindings only where motion semantics match; never infer from names alone.
5. Introduce/register the actual Region 01 boss entity/render-state/entity-renderer and feed the verified `Region01BossRuntimeAsset` into the existing `BossCustomGeometryRenderPipeline.submitCurrent(...)`. Keep final material selection blocked until an approved material/texture exists.
6. Spawn the boss under graphical capture and inspect topology, normals, UV orientation, culling, scale and deformation before proceeding to hitbox/telegraph, VFX/sound and human field-play review.
