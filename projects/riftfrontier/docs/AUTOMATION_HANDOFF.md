# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Canonical design documents and current GitHub `main` remain authoritative.

## Last recovered baseline

- Re-read current remote `main`, then `AGENTS.md` → `docs/BUILD_STANDARD.md` → `docs/QUALITY_STANDARD.md` → `PROJECT.md` → `CANONICAL.md` → `GAME_DESIGN_MASTER.md` → `CONTENT_ARCHITECTURE.md` → `ROADMAP.md`, then M3 reference docs and this handoff.
- Recovered remote `main` before this batch: `3d663354ca05c986ec8a677533be7df5f086264d`.
- Previous atomic boss selected-asset publication commit `3e75fd9ca273456cd8814206c7e9f6fb40540ac7` is confirmed by `Build Riftfrontier` run `34218517847` as full `SUCCESS`.
- Repository still contains no selected production Region 01 boss model/animation/VFX/sound resources. `THIRD_PARTY_ASSETS.md` is not present. No fake production asset or manifest was added.

## Completed in this batch

M3 physical presentation promotion gate:

- Added read-only logical-key lookup to `BossPresentationAssetManifest`.
- Added `BossPresentationAssetSelection`, a client/rendering-side promotion boundary that is constructed only when every selected physical resource passes a kind-aware `ResourceProbe`.
- Physical selection maps validated logical model/animation/VFX/sound keys to selected physical resource IDs without owning or recreating combat timing.
- `phase`, `phaseProgress`, and ACTIVE-only `hitWindowOpen` pass through unchanged from `BossPresentationResolver.ResolvedPresentation`.
- Missing physical resources prevent creation of a renderable selection for the entire manifest; there is no guessed fallback resource.
- Even after whole-manifest validation, resolving an unexpected logical key or wrong kind still fails closed.
- Added JUnit coverage for missing-resource rejection, successful logical→physical promotion, timing preservation, and unexpected-key fail-closed behavior.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationAssetManifest.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationAssetSelection.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationAssetSelectionTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Previous publication run `34218517847`: full Riftfrontier workflow `SUCCESS`.
- Latest test-bearing commit before this handoff: `5b958f046030cb19b3c0c5345eaab7ebee15f213`.
- `Build Riftfrontier` run `34224017933` exists for that exact commit and was `queued` at last check. Do not claim clean build/GameTest/server/client/JAR success until it finishes.
- Actual Minecraft client `ResourceManager` adapter for MODEL/ANIMATION/VFX/SOUND: NOT IMPLEMENTED / NOT TESTED.
- Actual production resource existence: NOT TESTED because no production boss assets are selected.
- Actual multiplayer semantic packet → renderer path: NOT TESTED.
- Final production Region 01 boss entity integration: NOT IMPLEMENTED / NOT TESTED.
- Actual human Minecraft field play: NOT TESTED.
- Final model/animation/VFX/sound assets: NOT SELECTED / NOT TESTED by design gate.

## Do not repeat or revert

- Preserve `AttackPattern` as the single authoritative combat timing source.
- Preserve ACTIVE-only damage, per-execution target deduplication, server-owned boss phase cancellation and deterministic attack-selection seam.
- Preserve `PresentationFrame` → `BossPresentationSemanticState` as the timing-free semantic contract and monotonic client ordering/clear watermark.
- Preserve presentation resolver fail-closed behavior; never guess missing asset keys or invent client timing.
- Preserve atomic publication: invalid core, presentation profile, or selected-asset manifest candidates must leave the complete previous snapshot untouched.
- Preserve the logical-key → selected-resource manifest boundary; manifest selection and physical resource existence are separate gates.
- Preserve `BossPresentationAssetSelection` as a fail-closed promotion gate: no renderable physical presentation until the complete selected manifest passes the client resource probe.
- Preserve the strict/versioned manifest JSON contract; provenance and license notes are mandatory selection data.
- Do not create placeholder production resources or a fake production manifest to make future resource validation green.
- Do not tune M2 pressure/patrol values without field-play evidence or promote M2 technical proxies to production art/AI.

## Exact next start point

1. Re-check current remote `main` and `Build Riftfrontier` run `34224017933`; fix the first real failure if present.
2. If green, implement the actual Minecraft-client kind-aware `ResourceProbe` adapter against the client resource boundary, keeping resource path rules explicit per MODEL/ANIMATION/VFX/SOUND and not using the server datapack `ResourceManager` as proof.
3. Keep that adapter inactive/fail-closed when no production manifest/assets exist; do not invent placeholders.
4. Perform actual Region 01 boss identity/presentation reference selection under `M3_COMBAT_REFERENCE_DOSSIER.md`, `REFERENCE_TARGETS.md`, repository license rules, and create `THIRD_PARTY_ASSETS.md` when the first bundled external asset is actually selected.
5. When the first actual animated production entity is ready, re-verify GeckoLib/current alternative compatibility for Minecraft 26.2 and adopt exactly one animation technology only if justified by the real asset/runtime need.
6. Author the first production `boss_presentation_asset_manifest` only after real assets are selected, then connect validated semantic packet → logical resolver → physical selection → animation/VFX/sound renderer.
7. Verify in Minecraft that telegraph/ACTIVE/recovery presentation and actual hit windows remain aligned; keep final presentation completion gated on human field-play/reference evidence.
