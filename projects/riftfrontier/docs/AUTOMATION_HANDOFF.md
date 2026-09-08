# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Canonical design documents and current GitHub `main` remain authoritative.

## Last recovered baseline

- Re-read current remote `main`, then `AGENTS.md` → `docs/BUILD_STANDARD.md` → `docs/QUALITY_STANDARD.md` → `PROJECT.md` → `CANONICAL.md` → `GAME_DESIGN_MASTER.md` → `CONTENT_ARCHITECTURE.md` → `ROADMAP.md`, plus M3/reference documents and this handoff.
- Current recovered baseline before this batch was remote `main` `2ab7761fb9a21c331240d1f1eb7376b488bbb6b5`.
- Atomic presentation publication commit `e43f39a052845b202c662e773e0cec751cd3f8a2` is confirmed by `Build Riftfrontier` run `34202374935` as full `SUCCESS`.
- Presentation asset-manifest gate commit `60cf93e3d3ef4df142508fe7752d654a2ef0846a` is confirmed by `Build Riftfrontier` run `34208092342` as full `SUCCESS`.
- Repository still contains no selected production boss model/animation/VFX/sound resources. No fake asset was added to satisfy the next gate.

## Completed in this batch

M3 selected-asset manifest serialization boundary:

- Added `BossPresentationAssetManifestCodec` as the strict/versioned authored JSON decoder for production asset selections.
- Manifest document contract is `kind=boss_presentation_asset_manifest`, `schema_version=1`, `assets[]`.
- Every authored asset must contain `logical_key`, `asset_kind`, `resource_id`, nonblank `source`, and nonblank `license_note`.
- Unknown root/asset fields fail closed rather than being silently ignored; unsupported schema versions fail explicitly.
- Unknown asset kinds fail explicitly; duplicate logical keys remain rejected by `BossPresentationAssetManifest`.
- Added JUnit coverage for successful decode, schema rejection, unknown-field rejection, unknown-kind rejection, duplicate logical keys and blank provenance.
- This batch intentionally does not publish a production manifest file because no production boss assets have been selected yet.
- Fresh reference check on 2026-09-08 confirms GeckoLib currently lists Minecraft 26.2 support (GeckoLib 5.5.1), but the project has **not** adopted/locked GeckoLib in this batch. Do not add the dependency solely to make a future path look decided; adoption should happen together with the first real production entity/animation asset need.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationAssetManifestCodec.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationAssetManifestCodecTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Previous presentation manifest gate run `34208092342`: full Riftfrontier workflow `SUCCESS`.
- New code commit: `de89707023a03c3d4249baf113989e14a1dc5341`.
- `Build Riftfrontier` run `34213636342`: IN PROGRESS at this handoff update. Checkout has completed and Gradle setup is running; do not claim this batch fully verified until that exact run is checked.
- Actual production resource existence: NOT TESTED because no production boss assets have been selected yet.
- Actual multiplayer boss semantic packet reception/rendering: NOT TESTED.
- Final production Region 01 boss entity integration: NOT IMPLEMENTED / NOT TESTED.
- Actual human Minecraft field play: NOT TESTED.
- Final model/animation/VFX/sound assets: NOT SELECTED / NOT TESTED by design gate.

## Do not repeat or revert

- Preserve `AttackPattern` as the single authoritative combat timing source.
- Preserve ACTIVE-only damage, per-execution target deduplication, server-owned boss phase cancellation, and deterministic attack-selection seam.
- Preserve `PresentationFrame` → `BossPresentationSemanticState` as the timing-free semantic contract and monotonic client ordering/clear watermark.
- Preserve presentation resolver fail-closed behavior; never guess missing asset keys or invent client timing.
- Preserve atomic publication: invalid core OR presentation candidates must leave the complete previous snapshot untouched.
- Preserve the logical-key → selected-resource manifest boundary; a valid logical key alone is never proof that a production resource exists.
- Preserve the strict/versioned manifest JSON contract; provenance and license notes are mandatory selection data, not optional comments.
- Do not create placeholder production resources or a fake manifest just to make resource validation green.
- Do not tune M2 pressure/patrol values without field-play evidence or promote M2 technical proxies to production art/AI.

## Exact next start point

1. Re-check current remote `main` and finish verification of run `34213636342`; fix the first real compile/test/runtime failure if present.
2. If fully green, perform actual Region 01 boss identity/presentation reference selection under `M3_COMBAT_REFERENCE_DOSSIER.md`, `REFERENCE_TARGETS.md` and repository license rules. Select or create a real model/animation/VFX/sound stack before authoring a production manifest.
3. When the first actual animated production entity is ready to be implemented, re-verify GeckoLib's current 26.2 release/API and adopt it only if it remains the best single animation technology; record the dependency/version decision in project docs.
4. With real assets selected, author the first `boss_presentation_asset_manifest` JSON and update `THIRD_PARTY_ASSETS.md` for every external bundled asset. Do not fabricate third-party ledger entries for project-owned or nonexistent assets.
5. Add the loader/client-specific kind-aware `ResourceProbe` and validate model/animation/VFX/sound physical resources on the correct client resource boundary. Do not incorrectly treat the server data-pack `ResourceManager` as proof that client-only assets exist.
6. Then connect the validated client presentation snapshot to the Region 01 production boss entity and verify semantic packet → animation/VFX/sound → actual hit-window alignment in Minecraft.
7. Keep M2 numerical tuning and final presentation completion gated on human field-play/reference evidence.
