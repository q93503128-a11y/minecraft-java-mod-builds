# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Canonical design documents and current GitHub `main` remain authoritative.

## Last recovered baseline

- Re-read current remote `main`, then `AGENTS.md` → `docs/BUILD_STANDARD.md` → `docs/QUALITY_STANDARD.md` → `PROJECT.md` → `CANONICAL.md` → `GAME_DESIGN_MASTER.md` → `CONTENT_ARCHITECTURE.md` → `ROADMAP.md`, plus M3/reference documents and this handoff.
- Atomic presentation publication commit `e43f39a052845b202c662e773e0cec751cd3f8a2` is confirmed by `Build Riftfrontier` run `34202374935` as full `SUCCESS`.
- Repository currently contains no selected production boss model/animation/VFX/sound resources. No fake asset was added to satisfy the next gate.

## Completed in this batch

M3 boss-presentation production-asset manifest gate:

- Added `BossPresentationAssetManifest` as the explicit boundary between logical presentation keys and selected physical resources.
- Every selected asset records logical key, expected role (`MODEL`, `ANIMATION`, `VFX`, `SOUND`), physical resource id, source/provenance and license/usage note.
- Duplicate logical-key selections fail immediately.
- `validateProfiles` fails closed when an authored boss presentation references an unselected logical key or a key selected for the wrong asset role.
- `validateResources` accepts a kind-aware loader/resource probe and fails closed when a selected physical resource does not exist.
- The manifest intentionally does not invent GeckoLib/particle/audio path conventions before the actual production stack/assets are selected; loader-specific physical path semantics remain outside the pure domain contract.
- Added JUnit coverage for complete logical-key coverage, wrong-kind rejection and missing physical-resource rejection.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationAssetManifest.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationResolverTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Previous atomic publication run `34202374935`: full Riftfrontier workflow `SUCCESS`.
- Latest test-bearing code commit: `60cf93e3d3ef4df142508fe7752d654a2ef0846a`.
- `Build Riftfrontier` run `34208092342`: IN PROGRESS at this handoff update; workflow setup is running and build/GameTest/server/client/JAR gates are not yet complete. Do not claim this batch fully verified until that exact run is checked.
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
- Do not create placeholder production resources just to make resource validation green.
- Do not tune M2 pressure/patrol values without field-play evidence or promote M2 technical proxies to production art/AI.

## Exact next start point

1. Re-check current remote `main` and finish verification of run `34208092342`; fix the first real compile/test/runtime failure if present.
2. If fully green, research/select the first real Region 01 boss presentation asset stack under `M3_COMBAT_REFERENCE_DOSSIER.md`, `REFERENCE_TARGETS.md` and the repository asset-license rules. Create/update `THIRD_PARTY_ASSETS.md` when an external asset is actually selected; do not fabricate an entry before selection.
3. Once real assets are selected, add the loader-specific `ResourceProbe` adapter for the chosen model/animation/VFX/sound technology and wire manifest validation into the same reload candidate before atomic publication.
4. Then author the first production manifest/presentation profile together and prove a missing physical resource keeps the previous snapshot intact.
5. Only after the production entity/reference boundary exists, wire resolver output to the Region 01 boss client presentation and verify visual-hit timing in Minecraft.
6. Keep M2 numerical tuning and final presentation completion gated on human field-play/reference evidence.
