# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Canonical design documents and current GitHub `main` remain authoritative.

## Last recovered baseline

- Re-read current remote `main`, then `AGENTS.md` → `docs/BUILD_STANDARD.md` → `docs/QUALITY_STANDARD.md` → `PROJECT.md` → `CANONICAL.md` → `GAME_DESIGN_MASTER.md` → `CONTENT_ARCHITECTURE.md` → `ROADMAP.md`, then current M3 presentation/runtime files and this handoff.
- Recovered remote `main` before this batch: `1e03eb094b245098c05c49bc99e228403b2573a2`.
- Previous atomic client resource-reload publication run `34236056618` for test-bearing head `976b8f7d6edcbaa4863b2ae3e9459749f52cb642` is confirmed full `SUCCESS`.
- Repository still has no selected production Region 01 boss model/animation/VFX/sound assets and no fake production manifest was added.

## Completed in this batch

M3 final render-facing generation consistency gate:

- Added `BossPresentationRenderResolver` as the final fail-closed bridge from authoritative `BossPresentationSemanticState` to physically validated `PhysicalPresentation` resource IDs.
- The bridge consumes the current logical `BossPresentationResolver` and atomically published `BossPresentationClientAssetRuntime.Snapshot` only when their `contentGeneration` values are exactly equal.
- A new logical content generation can therefore never resolve against stale physical assets from an older client resource reload; mismatched generations return no render presentation.
- Inactive client assets, inactive semantic state, unknown boss/variant contexts, missing logical bindings, and missing physical logical keys remain fail-closed with no guessed fallback.
- No animation/VFX/sound timer or damage cadence was introduced; telegraph/ACTIVE/recovery phase progress and ACTIVE-only hit-window semantics remain inherited from the authoritative semantic state.
- Added JUnit coverage for matched-generation semantic→logical→physical resolution, stale physical generation rejection, inactive asset rejection, unknown-context rejection, and canonical clear-state rejection.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationRenderResolver.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationRenderResolverTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Previous client resource reload run `34236056618`: full Riftfrontier workflow `SUCCESS`.
- Current implementation commit: `a3b9d19b61689b847582223808978b3e1e18f05e`.
- `Build Riftfrontier` run `34242151623`: toolchain `SUCCESS`; tests + clean build `SUCCESS`; required native GameTest currently `IN PROGRESS` at last check.
- Dedicated server smoke: NOT RUN yet in current run.
- Xvfb client smoke: NOT RUN yet in current run.
- Executable JAR inspection/report/artifact: NOT RUN yet in current run.
- Actual production resource existence: NOT TESTED because production Region 01 boss assets are not selected.
- Actual renderer/animation/VFX/sound activation: NOT IMPLEMENTED / NOT TESTED.
- Final production Region 01 boss entity integration: NOT IMPLEMENTED / NOT TESTED.
- Human Minecraft field play: NOT TESTED.
- Final model/animation/VFX/sound assets: NOT SELECTED / NOT TESTED by design gate.

## Do not repeat or revert

- Preserve `AttackPattern` as the single authoritative combat timing source and ACTIVE-only damage semantics.
- Preserve server-owned boss phase cancellation, deterministic attack selection, and `PresentationFrame` → semantic packet timing contract.
- Preserve monotonic client ordering/clear watermark; do not revive stale ACTIVE presentation.
- Preserve fail-closed logical resolver and whole-manifest physical promotion; never guess missing asset keys/resources or invent client combat timing.
- Preserve atomic core + presentation profile + selected-manifest server publication and last-known-good fallback.
- Preserve strict/versioned selected-asset manifest provenance/license requirements.
- Preserve separation between server datapack `ResourceManager` and client physical resource existence.
- Preserve client resource reload rule: validate the whole candidate manifest first, publish with one atomic swap, and retain the previous client selection if candidate physical validation fails.
- Preserve the new render rule: logical content generation must exactly match the physically validated client asset generation before semantic→physical resolution.
- Do not create placeholder production resources or a fake production manifest to make the physical probe green.
- Do not tune M2 pressure/patrol values without field-play evidence or promote M2 technical proxies to production art/AI.

## Exact next start point

1. Re-check current remote `main` and finish `Build Riftfrontier` run `34242151623`; if any remaining gate fails, fix the first real failure without weakening generation matching or fail-closed semantics.
2. If the run is fully green, move from architecture-only presentation plumbing to the Region 01 boss identity/presentation reference and asset-selection gate under `M3_COMBAT_REFERENCE_DOSSIER.md`, `REFERENCE_TARGETS.md`, repository license rules, and `THIRD_PARTY_ASSETS.md` only when a real external bundled asset is selected.
3. Keep actual renderer/animation/VFX/sound activation absent until real Region 01 boss assets exist.
4. Re-verify one animation technology for Minecraft 26.2 only when real animated production assets require it.
5. Author the first production selected-asset manifest only after real assets exist, then connect the render-facing resolver to the chosen renderer and verify telegraph/ACTIVE/recovery presentation against actual hit windows in Minecraft.
