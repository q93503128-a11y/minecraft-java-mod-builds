# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Required docs were re-read from remote `main` in canonical order.
- Remote `main` recovered at run start: `0ecc719bccb96ee7eaec857c5df8cd30fb7b9d88`.
- Previous exact-source derivation workflow `34279483680`: full `SUCCESS`.
- `Dragon Evolved` remains selected only as the Region 01 first-boss geometry/rig derivation source; source `Atlas` art is not production-approved.

## Completed in this batch

M3 GeckoLib 5 render-resource contract boundary:

- Re-checked the current Minecraft 26.2 GeckoLib distribution rather than trusting the stale local note. Official distribution feeds currently expose GeckoLib **5.5.5** for Minecraft 26.2 NeoForge; exact observed Maven coordinate: `com.geckolib:geckolib-neoforge-26.2:5.5.5`.
- Did **not** add GeckoLib as a dependency yet because Riftfrontier still has no concrete Region 01 boss entity/renderer consuming a real converted model. Re-verify the coordinate again at the dependency-adding commit.
- Corrected the animated boss physical resource contract from legacy GeckoLib 4-style `geo/` + unscoped `animations/` to GeckoLib 5 `geckolib/models/` + `geckolib/animations/` roots.
- Preserved vanilla `models/` support for static vanilla model JSON; VFX and sound resource policies remain unchanged.
- Added `BossPresentationGeckoLibResourceId`, the fail-closed bridge from already physically validated client resource paths to GeckoLib 5 GeoModel-relative identifiers without JSON suffixes.
- Legacy roots, cross-kind paths, missing suffixes and unscoped paths are rejected instead of guessing fallbacks.
- During review, found and fixed a real short-JSON edge case: GeckoLib 5 permits the `.geo` / `.animation` name portions to be omitted, so explicit `geckolib/.../*.json` resources must be preserved instead of becoming `*.json.geo.json` or `*.json.animation.json`.
- Updated the third-party/runtime registry so it no longer claims 5.5.1 is current and records the selected GeckoLib 5 resource layout without falsely claiming a runtime dependency or model exists.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationClientResourcePath.java` — GeckoLib 5 client resource-pack physical path policy; legacy animated roots fail closed.
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationGeckoLibResourceId.java` — physical-resource → GeoModel-relative ID bridge.
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationClientResourcePathTest.java` — canonical/short JSON, legacy and cross-kind path coverage.
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationGeckoLibResourceIdTest.java` — relative-ID and rejection coverage.
- `docs/THIRD_PARTY_ASSETS.md` — current GeckoLib 26.2 runtime/library note and resource layout.
- `docs/AUTOMATION_HANDOFF.md` — this recovery update.

## Verification

- Implementation commit: `f69febf0f4b87ed32789c9f80f27c52ff0ecccf8`.
- Correctness follow-up commit: `638e942a77f4d20858094770e89beeea0a4072d2`.
- `Build Riftfrontier` run `34284912972` for the implementation commit: full `SUCCESS`.
- `Build Riftfrontier` run `34285487373` for the final correctness commit: full `SUCCESS`.
- CI toolchain: SUCCESS.
- CI asset intake tool tests: SUCCESS.
- CI clean tests/build: SUCCESS.
- CI required native GameTest: SUCCESS.
- CI dedicated server smoke: SUCCESS.
- CI Xvfb client smoke: SUCCESS.
- CI executable JAR inspection: SUCCESS.
- CI build report + deliverable/log artifact upload: SUCCESS.
- Accepted sanitized Dragon glTF converted to a renderer-consumable GeckoLib model/animation resource: NOT IMPLEMENTED.
- GeckoLib runtime dependency + boss entity/renderer integration: NOT IMPLEMENTED / NOT TESTED.
- Real `presentation_assets` production manifest: NOT IMPLEMENTED.
- Production material/texture treatment: NOT IMPLEMENTED / NOT TESTED.
- In-Minecraft Region 01 boss model scale/hitbox/deformation/readability: NOT TESTED.
- Production VFX/sound timing and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Do not repeat Region 01 boss candidate selection or exact Dragon Evolved source fingerprint work unless the pinned source fails a future immutable-source check.
- Preserve source SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` and accepted sanitizer output SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` unless converter schema/version intentionally changes.
- Preserve `AttackPattern` as the only authoritative hit-timing source and ACTIVE-only damage semantics.
- Preserve fail-closed logical/physical asset resolution, atomic client resource reload and exact content-generation matching.
- Do not permit source `Atlas` material/texture/image bytes into production resources.
- For animated boss resources, do not restore legacy `geo/` or unscoped `animations/` physical roots. Current selected target contract is GeckoLib 5 `geckolib/models/` + `geckolib/animations/`.
- Preserve support for both canonical `.geo.json` / `.animation.json` files and GeckoLib 5 short `.json` variants without double-appending suffixes.
- Do not add GeckoLib merely because the current coordinate is known; add it only with concrete renderer/entity integration and re-verify the current 26.2 coordinate at that time.
- Do not create placeholder production model/animation/VFX/sound files or a fake `presentation_assets` manifest.
- Do not assume the accepted triangle-mesh glTF can be losslessly or trivially converted to GeckoLib/Bedrock cube geometry. Prove a valid Blockbench/GeckoLib conversion/import path first.
- Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs and this handoff.
2. Inspect current GeckoLib 5 / Blockbench model-format capabilities for a valid, deterministic path from the accepted Dragon Evolved glTF geometry/skin/animation data to a renderer-consumable resource. Treat arbitrary triangle-mesh → Bedrock/GeckoLib cube conversion as unproven until demonstrated; do not invent lossy geometry silently.
3. If a valid conversion/import route exists, acquire the exact pinned Dragon source bytes again, pass the existing source/derivation acceptance gates, generate the first real renderer-consumable `geckolib/models/...` and `geckolib/animations/...` resources, and add deterministic validation for bone hierarchy, clip inventory and resource loadability.
4. If GeckoLib cannot represent the selected mesh faithfully, document that evidence and select/implement a renderer path that can consume the accepted geometry without lowering the art or rig contract.
5. Only when real physical resources and a concrete boss entity/renderer exist: freshly verify the current Minecraft 26.2 GeckoLib coordinate, add the dependency if still selected, register the renderer, and validate through existing `ResourceProbe` / atomic reload / generation-match gates.
6. Then create the first real `presentation_assets` manifest; do not create it before all referenced physical resources exist.
7. After renderer loadability is proven, author commitment/displacement/arena-pressure/phase-transition presentation around authoritative server timing and perform Minecraft scale, hitbox, deformation, VFX/sound, performance and human readability field-play gates.
