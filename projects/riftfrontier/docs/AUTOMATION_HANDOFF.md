# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Required docs were re-read from remote `main` in canonical order.
- Remote `main` recovered at run start: `9b5cf4c33af41c784d15deba39c628665447dc02`.
- Previous deterministic source-derivation workflow `34267542813`: full `SUCCESS`.
- `Dragon Evolved` remains selected only as the Region 01 first-boss geometry/rig derivation source; source `Atlas` art is not production-approved.

## Completed in this batch

M3 art-neutral production geometry/skeleton conversion boundary:

- Added `tools/convert_region01_boss_geometry.py`.
- Conversion first requires the existing exact source SHA/structure gate, then rebuilds a self-contained deterministic glTF from only geometry, indices, allowed vertex attributes, skin/joint hierarchy, and source animation channels.
- Primitive material bindings, `materials`, `textures`, `images`, texture `samplers`, and image-only/unreferenced buffer payloads are removed rather than copied into the derived resource.
- Relative local buffers are re-embedded deterministically; remote/absolute buffer URIs and source-directory path escapes fail closed.
- Required glTF extensions, morph targets, unsupported primitive extensions/attributes, and unsupported accessor/bufferView/node fields fail closed with stable issue codes.
- Added synthetic regression coverage for art stripping, deterministic output, image-byte pruning, safe external-buffer re-embedding, path escape rejection, unsupported attribute rejection, required-extension rejection, and provenance.
- Source animation channels remain derivation data only. `AttackPattern` remains the only authoritative combat hit-timing source.
- No GeckoLib dependency, renderer, placeholder production model, final material, fake manifest, or speculative attack timing was added.

## Changed systems/files

- `tools/convert_region01_boss_geometry.py` — deterministic art-neutral geometry/skin/animation sanitizer/converter.
- `tools/tests/test_convert_region01_boss_geometry.py` — conversion regression/fail-closed tests.
- `docs/AUTOMATION_HANDOFF.md` — this recovery update.

## Verification

- Test-bearing commit: `bcf16d312de90a67053dde5b05fba77222c24b5b`.
- `Build Riftfrontier` run `34273047515`: full `SUCCESS`.
- CI Python asset-tool tests: SUCCESS.
- CI clean test/build: SUCCESS.
- Required native GameTest gate: SUCCESS.
- Dedicated server smoke: SUCCESS.
- Xvfb client smoke: SUCCESS.
- Executable JAR inspection/SHA, report, deliverable and logs upload: SUCCESS.
- Exact creator-hosted `Dragon_Evolved.gltf` re-run through source derivation + new geometry converter in this batch: NOT RUN; the exact selected source bytes were not available in this execution environment.
- Actual sanitized production Dragon Evolved glTF committed to runtime resources: NOT IMPLEMENTED / NOT TESTED.
- GeckoLib runtime integration and renderer: NOT IMPLEMENTED / NOT TESTED.
- Production material/texture treatment: NOT IMPLEMENTED / NOT TESTED.
- In-Minecraft boss scale/hitbox/deformation alignment: NOT TESTED.
- Production VFX/sound timing and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Preserve `AttackPattern` as the only authoritative hit-timing source and ACTIVE-only damage semantics.
- Preserve fail-closed logical/physical asset resolution, atomic client resource reload, and exact content-generation matching.
- Do not restart Region 01 boss candidate search unless the selected rig hits a non-fixable deterministic conversion/runtime blocker.
- Do not weaken the selected-source SHA/structure contract.
- Do not permit source material bindings, `Atlas` material/texture/image data, or image-only buffer bytes into the sanitized production geometry derivation.
- Do not weaken remote/absolute URI, path-escape, extension, morph-target, or unsupported-field fail-closed gates merely to accept a different export.
- Do not treat retained source animation channels as authoritative attack timing or as final authored boss presentation.
- Do not add GeckoLib merely because it is compatible; verify the current 26.2 coordinate as part of a concrete renderer integration.
- Do not create placeholder production resources or a fake `presentation_assets` manifest.
- Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs, and this handoff.
2. Re-obtain the exact creator-hosted `Dragon_Evolved.gltf`; require SHA-256 `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`.
3. Run `tools/derive_region01_boss_source.py` against `assets/sources/region_01_boss_dragon_evolved.source.json`, then run `tools/convert_region01_boss_geometry.py` on the same verified bytes.
4. Inspect the real sanitized glTF/provenance: exact output SHA, mesh/skin/joint/animation preservation, absence of materials/textures/images and source-art-only buffer payloads, and stable scale/bounds. Commit a physical derived runtime resource only if this exact-source conversion passes.
5. When renderer work actually begins, freshly verify the current GeckoLib 5.x coordinate for Minecraft 26.2 + NeoForge and add it only if needed by the chosen production resource path.
6. Validate the physical resource through the existing client `ResourceProbe`/reload generation gates; only then create the first real `presentation_assets` manifest and connect `BossPresentationRenderResolver` to the renderer.
7. Author final commitment/displacement/arena-pressure/phase-transition presentation around authoritative server timing, then perform in-Minecraft scale, hitbox, deformation, VFX/sound, performance, screenshot/video and human readability gates.
