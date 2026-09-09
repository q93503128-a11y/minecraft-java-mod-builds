# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` at this run start: `6731864f3b8d4fc32f6884dced17e109f3ae9105`.
- Remote advanced concurrently to `cb02ebfb1dcbf9a1a40c1bc11c06217d030a1524` through other-project work; the Riftfrontier implementation was based on that newer tree and pushed by non-force fast-forward.
- Previous verified production-facing boss render-pipeline baseline: implementation `0ae647b7bc76650baf0aca60f3978003eed935bf`, run `34321821176` FULL SUCCESS.
- `Dragon Evolved` remains selected only as Region 01 first-boss geometry/rig derivation source. Source `Atlas` art remains unapproved.

## Completed in this batch

M3 exact-source animation-metrics audit gate:

- Added `tools/audit_region01_boss_animation_clips.py`.
- The tool accepts only a glTF whose SHA-256 exactly matches `assets/sources/region_01_boss_dragon_evolved.acceptance.json` and whose animation inventory/order exactly matches the accepted eight source clips.
- It reads actual embedded FLOAT/SCALAR animation input accessors and records, per clip, duration, channel count, sampler count, animated-node count, target-path counts, and interpolation counts.
- STEP / LINEAR / CUBICSPLINE plus translation / rotation / scale are explicit accepted contracts; sparse/bad accessor layout, invalid nodes/samplers, non-finite/non-monotonic times, unsupported paths/interpolation, SHA drift, and inventory drift fail closed.
- Added focused regression tests for exact metrics, SHA rejection, and inventory-order rejection.
- No source clip was bound to a logical attack and no duration/channel metric was guessed without the accepted binary.

## Changed systems/files

- `tools/audit_region01_boss_animation_clips.py`
- `tools/tests/test_audit_region01_boss_animation_clips.py`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Implementation commit: `4053119fb9a607053b321de0cb050a3b252a3a59`.
- Focused local Python regression: 3/3 PASS for the new audit tool tests.
- `Build Riftfrontier` run `34327050109`: IN PROGRESS at handoff update. Confirmed SUCCESS so far: toolchain, asset-intake tests, JUnit + clean build, required native GameTest. Dedicated-server smoke was still running; client smoke, JAR inspection, report/artifact upload were NOT YET COMPLETE. Do not promote this run to FULL SUCCESS until GitHub reports completion.
- Exact accepted sanitized Dragon derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` was NOT reacquired in this execution environment. The recorded Quaternius/Drive source remains the provenance, but the binary download path was unavailable here.
- Real eight-clip duration/channel/path/interpolation receipt: NOT PRODUCED because the exact accepted binary was unavailable. No values were inferred from clip names.
- Explicit logical production mapping from attack animation keys to real source clips: NOT IMPLEMENTED pending actual accepted-source motion inspection.
- Concrete production boss entity/render-state/entity-renderer calling `BossCustomGeometryRenderPipeline.submitCurrent(...)`: NOT IMPLEMENTED.
- Actual spawned/deformed Dragon graphical capture, approved material/texture, `presentation_assets` manifest, scale/culling/UV/deformation/hitbox/VFX/sound/human field-play review: NOT TESTED / NOT IMPLEMENTED.

## Do not repeat or revert

- Preserve source SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` and accepted derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` unless converter schema/version intentionally changes.
- Preserve exact source clip inventory names unless direct accepted-source inspection proves a source revision changed.
- Do not hard-code guessed clip durations/channel metrics or infer logical mappings from clip names.
- Do not bypass the new exact-SHA/exact-inventory audit gate or create an animation-metrics receipt from any non-accepted derivation.
- Preserve `AttackPattern`/server semantic state as authoritative timing and ACTIVE-only damage semantics. No presentation layer may become a free-running combat clock.
- Preserve monotonic client semantic snapshot ordering and fail-closed presentation resolution.
- Do not permit source `Atlas` material/texture/image bytes into production resources.
- Do not restore legacy GeckoLib 4 paths or rigid GeoBone/cube approximation for this asset.
- Preserve arbitrary triangle topology, four-influence LBS, imported rig/animations, pose sampler, semantic sample bridge, frame sampler, verified inventory/binding gate, custom-geometry adapter and production-facing render pipeline.
- Keep Minecraft 26.2 `net.minecraft.client.renderer.rendertype.RenderType` boundary.
- Do not weaken exact derivation SHA/count validation or create fake physical presentation assets/manifests.
- Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs and this handoff. First close the final status of `Build Riftfrontier` run `34327050109`; if it failed, fix the first real failure without reducing scope.
2. Reacquire the exact accepted sanitized derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
3. Run `python tools/audit_region01_boss_animation_clips.py <accepted.gltf> assets/sources/region_01_boss_dragon_evolved.acceptance.json --output <receipt.json>` and commit the resulting real eight-clip metrics receipt only if the SHA/inventory gates pass.
4. Inspect actual clip motion and author explicit logical source bindings only where motion semantics match; never infer from names alone.
5. Introduce/register the actual Region 01 boss entity/render-state/entity-renderer and call `BossCustomGeometryRenderPipeline.submitCurrent(...)`. Keep final material selection blocked until an approved material/texture exists.
6. Spawn the boss under graphical capture and inspect topology, normals, UV orientation, culling, scale and deformation before proceeding to hitbox/telegraph, VFX/sound and human field-play review.
