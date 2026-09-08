# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Required docs were re-read from remote `main` in canonical order.
- Remote `main` recovered at run start: `b27f873c14348c1ea965be0020bf8084a3eba295`.
- Previous geometry-sanitizer workflow `34273047515`: full `SUCCESS`.
- `Dragon Evolved` remains selected only as the Region 01 first-boss geometry/rig derivation source; source `Atlas` art is not production-approved.

## Completed in this batch

M3 exact-source derivation acceptance/fingerprint gate:

- Re-obtained creator-hosted `Dragon_Evolved.gltf` from the recorded Quaternius Google Drive file ID and verified exact SHA-256 `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` and 991,335-byte size.
- Ran the deterministic geometry sanitizer semantics against the exact selected bytes and inspected the real output rather than a synthetic fixture.
- Exact accepted sanitizer result: 681,773-byte canonical glTF, SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- Preserved: 1 mesh, 1 skin, 46 joints, 48 nodes, 4,437 vertices, 7,440 triangles, 737 accessors, 737 bufferViews and all 8 named source clips.
- Source-space bounds verified: min `[-2.7376062870025635, 0.30027779936790466, -1.4310814142227173]`, max `[2.7376062870025635, 3.1581382751464844, 0.984516441822052]`.
- Verified the source PNG image bufferView is excluded and primitive material binding plus top-level `materials`/`textures`/`images`/texture `samplers` are absent. The embedded retained payload is 381,212 bytes.
- Added `assets/sources/region_01_boss_dragon_evolved.acceptance.json` to pin the exact source/output fingerprints and structural bounds.
- Added `tools/verify_region01_boss_derivation.py`, a fail-closed acceptance verifier for SHA/provenance/structure/bounds/art stripping.
- Added regression tests for accepted derivation, hash drift and re-pinned forbidden material payload.
- First CI attempt `34279350520` failed at the asset-tool step because the new test used pytest while this repository intentionally runs `unittest discover`; the test was fixed without changing CI or weakening coverage.
- Updated `THIRD_PARTY_ASSETS.md` with the exact verified conversion fingerprint and clarified that this sanitized glTF is still a pre-runtime derivation, not a renderable MODEL resource.

## Changed systems/files

- `assets/sources/region_01_boss_dragon_evolved.acceptance.json` — exact production derivation receipt.
- `tools/verify_region01_boss_derivation.py` — deterministic acceptance/fail-closed verifier.
- `tools/tests/test_verify_region01_boss_derivation.py` — verifier regression tests under the repository's standard `unittest` gate.
- `docs/THIRD_PARTY_ASSETS.md` — exact source/derivation fingerprint and current distribution state.
- `docs/AUTOMATION_HANDOFF.md` — this recovery update.

## Verification

- Exact creator source SHA/size: VERIFIED.
- Exact sanitized output SHA/size/geometry topology/bounds/art stripping: VERIFIED locally from actual selected bytes.
- Local acceptance verifier against actual sanitized output/provenance: PASS.
- Local verifier regression tests: 3 PASS.
- Test-bearing fix commit: `2cf3c6daea3d4a496ca9bdcbaf7bb38bd0ceb3c5`.
- `Build Riftfrontier` run `34279483680`: IN PROGRESS at last check.
- CI toolchain: SUCCESS.
- CI asset intake tool tests: SUCCESS.
- CI clean tests/build: SUCCESS.
- CI required native GameTest: SUCCESS.
- CI dedicated server smoke: IN PROGRESS at last check.
- CI Xvfb client smoke: NOT RUN yet.
- CI executable JAR/report/artifact gate: NOT RUN yet.
- Accepted sanitized glTF bytes committed as a runtime MODEL resource: NOT IMPLEMENTED. Only its exact acceptance receipt is committed because a renderer-consumable production format/path has not yet been selected.
- GeckoLib/runtime renderer integration: NOT IMPLEMENTED / NOT TESTED.
- Production material/texture treatment: NOT IMPLEMENTED / NOT TESTED.
- In-Minecraft boss scale/hitbox/deformation alignment: NOT TESTED.
- Production VFX/sound timing and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Do not repeat Region 01 boss candidate selection or exact Dragon Evolved source fingerprint work unless the pinned source fails a future immutable-source check.
- Preserve source SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` and accepted sanitizer output SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` unless converter schema/version intentionally changes.
- Preserve `AttackPattern` as the only authoritative hit-timing source and ACTIVE-only damage semantics.
- Preserve fail-closed logical/physical asset resolution, atomic client resource reload and exact content-generation matching.
- Do not permit source `Atlas` material/texture/image bytes into production resources.
- Do not weaken URI/path/extension/morph/unsupported-field or derivation acceptance gates to accept another export.
- Do not add GeckoLib merely because it is compatible; verify the current Minecraft 26.2 + NeoForge coordinate as part of concrete renderer integration.
- Do not create placeholder production resources or a fake `presentation_assets` manifest.
- Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs and this handoff.
2. Close `Build Riftfrontier` run `34279483680` through dedicated server, Xvfb client, executable JAR/report/artifact. Do not call it successful before completion.
3. Inspect the current client presentation MODEL resource path/resolver and choose the renderer-consumable physical format for the accepted geometry/skin derivation. Do not commit the 681,773-byte glTF into `src/main/resources` merely because it passed derivation acceptance if the runtime cannot consume that format directly.
4. At actual renderer implementation time, freshly verify the current GeckoLib 5.x coordinate for Minecraft 26.2 + NeoForge. Add it only if it is the selected runtime path.
5. Produce the first renderer-consumable physical model/animation resource from the accepted receipt, then validate it through existing `ResourceProbe`/reload generation gates.
6. Only after physical loadability is proven, create the first real `presentation_assets` manifest and connect `BossPresentationRenderResolver` to the renderer.
7. Author commitment/displacement/arena-pressure/phase-transition presentation around authoritative server timing, then perform in-Minecraft scale, hitbox, deformation, VFX/sound, performance and human readability gates.
