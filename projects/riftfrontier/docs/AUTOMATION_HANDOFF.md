# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Canonical design documents and current GitHub `main` remain authoritative.

## Last recovered baseline

- Re-read current remote `main`, then `AGENTS.md` → `docs/BUILD_STANDARD.md` → `docs/QUALITY_STANDARD.md` → `PROJECT.md` → `CANONICAL.md` → `GAME_DESIGN_MASTER.md` → `CONTENT_ARCHITECTURE.md` → `ROADMAP.md`, plus M3 reference/runtime documents and this handoff.
- Recovered remote `main` before this batch: `d32742eef1db57e8d50a2b5fc5db63ec7757a28b`.
- Strict asset-manifest codec commit `de89707023a03c3d4249baf113989e14a1dc5341` is confirmed by `Build Riftfrontier` run `34213636342` as full `SUCCESS`.
- Repository still contains no selected production Region 01 boss model/animation/VFX/sound resources. No fake production asset or manifest was added.

## Completed in this batch

M3 atomic boss selected-asset publication boundary:

- `ContentRuntimeSnapshot` now carries the optional validated `BossPresentationAssetManifest` in the same immutable generation as core content and presentation profiles.
- Added a production `ContentRuntime.installValidated(..., presentationProfiles, Optional<assetManifest>)` path.
- On that production path, any non-empty boss presentation profile set without a selected-asset manifest fails closed.
- A supplied manifest is validated against every profile logical key and asset kind before generation is advanced.
- Invalid/missing manifest candidates preserve the complete last-known-good snapshot and generation.
- `ContentServerReloadListener` now loads at most one manifest from `data/riftfrontier/riftfrontier/presentation_assets/*.json` and publishes core + profiles + selected-asset metadata atomically.
- Multiple authored manifest documents fail closed rather than creating merge-order ambiguity.
- Physical client resource existence is still intentionally outside the server datapack publication boundary; this batch does not misuse the server `ResourceManager` as proof that client assets exist.
- Added JUnit coverage for manifest-required production publication, successful atomic manifest publication and rollback on missing logical-key coverage.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/content/ContentRuntimeSnapshot.java`
- `src/main/java/kr/moonseungjun/riftfrontier/content/ContentRuntime.java`
- `src/main/java/kr/moonseungjun/riftfrontier/content/ContentServerReloadListener.java`
- `src/test/java/kr/moonseungjun/riftfrontier/content/ContentRuntimeTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Previous codec run `34213636342`: full Riftfrontier workflow `SUCCESS`.
- This batch: commit/run must be checked after push; do not claim clean build/GameTest/server/client/JAR success until the exact new workflow run is complete.
- Actual production resource existence: NOT TESTED because no production boss assets are selected.
- Actual multiplayer boss semantic packet reception/rendering: NOT TESTED.
- Final production Region 01 boss entity integration: NOT IMPLEMENTED / NOT TESTED.
- Actual human Minecraft field play: NOT TESTED.
- Final model/animation/VFX/sound assets: NOT SELECTED / NOT TESTED by design gate.

## Do not repeat or revert

- Preserve `AttackPattern` as the single authoritative combat timing source.
- Preserve ACTIVE-only damage, per-execution target deduplication, server-owned boss phase cancellation and deterministic attack-selection seam.
- Preserve `PresentationFrame` → `BossPresentationSemanticState` as the timing-free semantic contract and monotonic client ordering/clear watermark.
- Preserve presentation resolver fail-closed behavior; never guess missing asset keys or invent client timing.
- Preserve atomic publication: invalid core, presentation profile, or selected-asset manifest candidates must leave the complete previous snapshot untouched.
- Preserve the logical-key → selected-resource manifest boundary; a logical key or manifest entry alone is never proof that a client physical resource exists.
- Preserve the strict/versioned manifest JSON contract; provenance and license notes are mandatory selection data.
- Do not create placeholder production resources or a fake production manifest to make future resource validation green.
- Do not tune M2 pressure/patrol values without field-play evidence or promote M2 technical proxies to production art/AI.

## Exact next start point

1. Re-check current remote `main` and the exact `Build Riftfrontier` run created by this batch; fix the first real compile/test/runtime failure if present.
2. If fully green, perform actual Region 01 boss identity/presentation reference selection under `M3_COMBAT_REFERENCE_DOSSIER.md`, `REFERENCE_TARGETS.md` and repository license rules.
3. When the first actual animated production entity is ready, re-verify GeckoLib/current alternative compatibility for Minecraft 26.2 and adopt exactly one animation technology only if justified by the real asset/runtime need.
4. With real assets selected, author the first production `boss_presentation_asset_manifest` JSON under the new `riftfrontier/presentation_assets` datapack boundary and update `THIRD_PARTY_ASSETS.md` for every bundled external asset.
5. Add a physical-client kind-aware `ResourceProbe` using the correct client resource boundary, then require successful physical resource validation before the client presentation selection becomes renderable. Do not use the server datapack `ResourceManager` for this proof.
6. Connect the validated client presentation snapshot to the Region 01 production boss entity and verify semantic packet → animation/VFX/sound → actual hit-window alignment in Minecraft.
7. Keep M2 numerical tuning and final presentation completion gated on human field-play/reference evidence.
