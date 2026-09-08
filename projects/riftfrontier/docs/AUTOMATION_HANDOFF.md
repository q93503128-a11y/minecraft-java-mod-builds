# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Required docs were re-read from remote `main` in canonical order.
- Remote `main` recovered at run start: `1209e9b4ffab2e63beabb6c2ae9e5d8670187c3c`.
- Previous Region 01 source-selection workflow `34260793930`: full `SUCCESS`.
- `Dragon Evolved` remains the selected Region 01 first-boss geometry/rig derivation source only; original `Atlas` art is not production-approved.

## Completed in this batch

M3 deterministic selected-source derivation boundary:

- Added `assets/sources/region_01_boss_dragon_evolved.source.json` as the machine-readable exact source contract.
- Contract pins `Dragon_Evolved.gltf` SHA-256 `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`, glTF 2.0, 1 mesh, 1 skin, 46 joints, 8 exact source clips, and the third-party license record.
- Added `tools/derive_region01_boss_source.py`.
- Tool fail-closes on source hash/filename/version/mesh/skin/joint/clip drift and invalid node/accessor/channel references.
- Tool emits deterministic canonical JSON containing only node hierarchy, transforms, skin joint order/binding, and animation channel/duration inventory.
- Original materials, textures, images, buffers, mesh payloads, final-art approval, and hit timing are explicitly excluded from the derived binding-plan contract.
- No GeckoLib dependency, production renderer, placeholder model, fake production manifest, or speculative final texture was added.

## Changed systems/files

- `assets/sources/region_01_boss_dragon_evolved.source.json` — exact selected-source contract.
- `tools/derive_region01_boss_source.py` — deterministic source verification + rig/animation binding-plan derivation.
- `tools/tests/test_derive_region01_boss_source.py` — deterministic/fail-closed regression coverage.
- `docs/AUTOMATION_HANDOFF.md` — this recovery update.

## Verification

- Local new-tool unit tests: 5/5 PASS.
- Test-bearing commit: `3fcbf01d0b30ff119e9ad9888f08a76dcddeeb26`.
- `Build Riftfrontier` run `34267542813`: full `SUCCESS`.
- CI asset-intake Python tests: SUCCESS.
- CI clean test/build: SUCCESS.
- Required native GameTest gate: SUCCESS.
- Dedicated server smoke: SUCCESS.
- Xvfb client smoke: SUCCESS.
- Executable JAR inspection/SHA + deliverable/report upload: SUCCESS.
- Exact selected creator-source file re-run through the new contract in this batch: NOT RUN; the prior inspected source bytes were not persisted in this execution environment.
- Production derived Minecraft model/animation resources: NOT IMPLEMENTED / NOT TESTED.
- GeckoLib runtime integration and renderer: NOT IMPLEMENTED / NOT TESTED.
- Production material/texture treatment: NOT IMPLEMENTED / NOT TESTED.
- In-Minecraft boss scale/hitbox/deformation alignment: NOT TESTED.
- Production VFX/sound timing and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Preserve `AttackPattern` as the only authoritative hit-timing source and ACTIVE-only damage semantics.
- Preserve fail-closed logical/physical asset resolution, atomic client resource reload, and exact content-generation matching.
- Do not restart Region 01 boss candidate search unless the selected rig hits a non-fixable deterministic conversion/runtime blocker.
- Do not treat source `Atlas` material/texture as approved final art.
- Do not weaken or bypass the selected-source SHA/structure contract to accept a drifting download.
- Do not copy material/texture/buffer payloads through the rig binding-plan intermediate.
- Do not add GeckoLib merely because it is compatible; choose the currently verified 26.2-compatible coordinate only as part of a concrete renderer integration.
- Do not create placeholder production resources or a fake `presentation_assets` manifest.
- Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs, and this handoff.
2. Re-obtain the exact creator-hosted `Dragon_Evolved.gltf`; require SHA-256 `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` and run `tools/derive_region01_boss_source.py` against `assets/sources/region_01_boss_dragon_evolved.source.json`.
3. Inspect the real emitted binding plan and implement the deterministic production geometry/skeleton conversion that preserves the selected joint hierarchy; do not copy the unapproved `Atlas` look.
4. At the moment renderer work begins, freshly verify the current GeckoLib 5.x coordinate for Minecraft 26.2 + NeoForge and add it only if the chosen production resource path needs it.
5. Produce actual derived model/animation resources and validate them through the existing client `ResourceProbe`/reload generation gates.
6. Only after physical resources exist, create the first real `presentation_assets` manifest and connect `BossPresentationRenderResolver` to the renderer.
7. Then author distinct commitment/displacement/arena-pressure/phase-transition presentation clips around authoritative server timing and perform in-Minecraft scale, hitbox, deformation, VFX/sound, performance, and human readability gates.
