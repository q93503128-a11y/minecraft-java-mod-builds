# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Canonical design documents and current GitHub `main` remain authoritative.

## Last recovered baseline

- Re-read current remote `main`, then `AGENTS.md` → `docs/BUILD_STANDARD.md` → `docs/QUALITY_STANDARD.md` → `PROJECT.md` → `CANONICAL.md` → `GAME_DESIGN_MASTER.md` → `CONTENT_ARCHITECTURE.md` → `ROADMAP.md`, then current M3 presentation/runtime/reference files and this handoff.
- Recovered remote `main` before this batch: `fb2f7142e61c6133369dbfb9aded3497e495dbf7`.
- Previous physical presentation promotion commit `5b958f046030cb19b3c0c5345eaab7ebee15f213` is confirmed by `Build Riftfrontier` run `34224017933` as full `SUCCESS`.
- Repository still has no selected production Region 01 boss model/animation/VFX/sound assets and no fake production manifest was added.

## Completed in this batch

M3 Minecraft client physical-resource probe adapter:

- Added pure `BossPresentationClientResourcePath` policy that resolves selected manifest resource IDs to exact client-resource-pack file paths by asset kind.
- MODEL accepts `models/` JSON and `geo/` `.geo.json`; ANIMATION accepts `animations/` `.animation.json`; VFX accepts `vfx/` or `particles/` JSON; SOUND accepts `sounds/` OGG.
- Invalid cross-kind/unscoped paths fail closed instead of guessing a fallback.
- Added `MinecraftClientBossPresentationResourceProbe`, backed only by a caller-supplied Minecraft client `ResourceManager` and `Identifier`; it never uses server datapack reload state as proof of client renderability.
- Existing `BossPresentationAssetSelection` remains the whole-manifest promotion gate; the new adapter only supplies its kind-aware physical existence check.
- Added JUnit coverage for exact per-kind path resolution, cross-kind rejection, and explicit suffix preservation.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationClientResourcePath.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/MinecraftClientBossPresentationResourceProbe.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationClientResourcePathTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Previous physical promotion run `34224017933`: full Riftfrontier workflow `SUCCESS`.
- This batch compile/unit/GameTest/server/client/JAR CI: PENDING until the new commit run is observed.
- Actual production resource existence: NOT TESTED because production Region 01 boss assets are not selected.
- Actual multiplayer semantic packet → physical renderer path: NOT TESTED.
- Final production Region 01 boss entity integration: NOT IMPLEMENTED / NOT TESTED.
- Human Minecraft field play: NOT TESTED.
- Final model/animation/VFX/sound assets: NOT SELECTED / NOT TESTED by design gate.

## Do not repeat or revert

- Preserve `AttackPattern` as the single authoritative combat timing source and ACTIVE-only damage semantics.
- Preserve server-owned boss phase cancellation, deterministic attack selection, and `PresentationFrame` → semantic packet timing contract.
- Preserve monotonic client ordering/clear watermark; do not revive stale ACTIVE presentation.
- Preserve fail-closed logical resolver and whole-manifest physical promotion; never guess missing asset keys/resources or invent client combat timing.
- Preserve atomic core + presentation profile + selected-manifest publication and last-known-good fallback.
- Preserve strict/versioned selected-asset manifest provenance/license requirements.
- Preserve separation between server datapack `ResourceManager` and client physical resource existence.
- Do not create placeholder production resources or a fake production manifest to make the physical probe green.
- Do not tune M2 pressure/patrol values without field-play evidence or promote M2 technical proxies to production art/AI.

## Exact next start point

1. Re-check current remote `main` and the `Build Riftfrontier` run for this batch; fix the first real failure if present.
2. If green, connect the client resource probe to the client reload lifecycle so a selected manifest can be revalidated atomically when client resource packs reload, while remaining inactive when no production manifest exists.
3. Keep renderer activation fail-closed: semantic packet → logical resolver → validated physical selection only; no resource or timing fallback.
4. Perform actual Region 01 boss identity/presentation reference and asset selection under `M3_COMBAT_REFERENCE_DOSSIER.md`, `REFERENCE_TARGETS.md`, repository license rules; create `THIRD_PARTY_ASSETS.md` only when an external bundled asset is actually selected.
5. Re-verify one animation technology for Minecraft 26.2 only when real animated production assets require it.
6. Author the first production selected-asset manifest only after real assets exist, then verify telegraph/ACTIVE/recovery presentation against actual hit windows in Minecraft.
