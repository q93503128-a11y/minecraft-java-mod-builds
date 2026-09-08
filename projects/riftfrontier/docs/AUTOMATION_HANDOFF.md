# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Canonical design documents and current GitHub `main` remain authoritative.

## Last recovered baseline

- Re-read current remote `main`, then `AGENTS.md` → `docs/BUILD_STANDARD.md` → `docs/QUALITY_STANDARD.md` → `PROJECT.md` → `CANONICAL.md` → `GAME_DESIGN_MASTER.md` → `CONTENT_ARCHITECTURE.md` → `ROADMAP.md`, then current M3 presentation/runtime files and this handoff.
- Recovered remote `main` before this batch: `fddb105f585486d9622d847af065426b641b1931`.
- Previous client physical-resource probe commit `ce8b2f709f99c955e97274a581c8ae2917d06608` is confirmed by `Build Riftfrontier` run `34230099784` as full `SUCCESS`.
- Repository still has no selected production Region 01 boss model/animation/VFX/sound assets and no fake production manifest was added.

## Completed in this batch

M3 atomic client resource-reload publication gate:

- Added `BossPresentationClientAssetRuntime` as the single atomic client-side publication boundary for physically validated boss presentation assets.
- A resource reload validates the complete selected manifest against the candidate client resource manager before any new selection is published.
- If any selected MODEL / ANIMATION / VFX / SOUND resource is missing, validation throws and the previously published client selection remains untouched; no partially promoted manifest can reach a renderer.
- A content snapshot with no selected production asset manifest is an explicit valid inactive state and does not invent placeholder resources.
- Added `RiftfrontierClientResources` using NeoForge 26.2 `AddClientReloadListenersEvent` and an apply-only `ResourceManagerReloadListener` backed by `MinecraftClientBossPresentationResourceProbe`.
- Client resource validation continues to use only the physical client resource manager, never the server datapack resource manager.
- Added JUnit coverage for successful whole-manifest publication, failed-candidate preservation of the previous selection/generation, and transition to explicit inactive state when no production manifest exists.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationClientAssetRuntime.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/RiftfrontierClientResources.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationClientAssetRuntimeTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Previous physical client probe run `34230099784`: full Riftfrontier workflow `SUCCESS`.
- Batch implementation commits: `8fcc317924462e559674d9d52255c6638ef4914c`, `223e539c6f264b0694f9f5931c874a78854f8e92`, test-bearing head `976b8f7d6edcbaa4863b2ae3e9459749f52cb642`.
- `Build Riftfrontier` run `34236056618`: toolchain `SUCCESS`; tests + clean build `SUCCESS`; required native GameTest currently `IN PROGRESS` at last check.
- Dedicated server smoke: NOT RUN yet in current run.
- Xvfb client smoke: NOT RUN yet in current run.
- Executable JAR inspection/report/artifact: NOT RUN yet in current run.
- Actual production resource existence: NOT TESTED because production Region 01 boss assets are not selected.
- Actual multiplayer semantic packet → logical resolver → physical renderer path: NOT TESTED.
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
- Preserve the new client reload rule: validate the whole candidate manifest first, publish with one atomic swap, and retain the previous client selection if candidate physical validation fails.
- Do not create placeholder production resources or a fake production manifest to make the physical probe green.
- Do not tune M2 pressure/patrol values without field-play evidence or promote M2 technical proxies to production art/AI.

## Exact next start point

1. Re-check current remote `main` and finish `Build Riftfrontier` run `34236056618`; if any remaining gate fails, fix the first real failure without weakening the client reload contract.
2. If the run is fully green, add the final fail-closed render-facing resolution boundary: semantic packet → current logical resolver → client asset selection, requiring matching content/asset generation so a server/content reload cannot mix a new logical profile with stale physical assets.
3. Keep actual renderer/animation/VFX/sound activation absent until real Region 01 boss assets exist.
4. Perform Region 01 boss identity/presentation reference and asset selection under `M3_COMBAT_REFERENCE_DOSSIER.md`, `REFERENCE_TARGETS.md`, repository license rules; create `THIRD_PARTY_ASSETS.md` only when an external bundled asset is actually selected.
5. Re-verify one animation technology for Minecraft 26.2 only when real animated production assets require it.
6. Author the first production selected-asset manifest only after real assets exist, then verify telegraph/ACTIVE/recovery presentation against actual hit windows in Minecraft.
