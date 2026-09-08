# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Canonical design documents and current GitHub `main` remain authoritative.

## Last recovered baseline

- Re-read current remote `main`, then `AGENTS.md` → `docs/BUILD_STANDARD.md` → `docs/QUALITY_STANDARD.md` → `PROJECT.md` → `CANONICAL.md` → `GAME_DESIGN_MASTER.md` → `CONTENT_ARCHITECTURE.md` → `ROADMAP.md`, current M3 reference/runtime files, then this handoff.
- Recovered remote `main` before this batch: `1c201e66089db0b94dc59b0249078a40b5cedc16`.
- Previous render-facing generation consistency run `34242151623` for code commit `a3b9d19b61689b847582223808978b3e1e18f05e` is confirmed full `SUCCESS`.
- Repository still has no selected production Region 01 boss model/animation/VFX/sound assets and no fake production manifest.

## Completed in this batch

M3 Region 01 boss presentation/reference and asset-selection gate:

- Added `REGION_01_BOSS_PRESENTATION_GATE.md` to stop architecture-only presentation work and define the production acceptance matrix before any model/animation/VFX/sound is bundled.
- Locked first boss combat communication requirements around position control / commitment punishment: readable committed strike, line/displacement pressure, arena-pressure telegraph, explicit recovery/counter window, and non-numeric phase change.
- Locked model acceptance criteria: facing readability, attack-bearing silhouette, telegraph rig headroom, Minecraft scale/style fit, variant stability, and performance fit.
- Locked animation/VFX/sound contract to the authoritative `AttackPattern` clock; presentation may emphasize timing but may not create a second hit clock.
- Researched Quaternius `Ultimate Monsters`: 50 animated monster models, FBX/OBJ/Blend/glTF, source page declares CC0 and commercial use. Classified source family as license-eligible `CANDIDATE`, but selected no individual model because the visual/rig/Minecraft-fit gate has not been performed.
- Inspected Poly Pizza public-domain/CC0 mirrors for individual Quaternius examples only as discovery/provenance cross-check; no model selected.
- Re-verified GeckoLib current support table: Minecraft 26.2 → GeckoLib 5.5.1. Kept it as technology candidate only; dependency not added without a real accepted animated asset.
- Created missing project `THIRD_PARTY_ASSETS.md` provenance registry referenced by canonical project guidance.
- Updated `M3_COMBAT_REFERENCE_DOSSIER.md` to point to the new production presentation gate and exact next boundary.

## Changed systems/files

- `docs/REGION_01_BOSS_PRESENTATION_GATE.md` — new
- `docs/THIRD_PARTY_ASSETS.md` — new
- `docs/M3_COMBAT_REFERENCE_DOSSIER.md` — updated
- `docs/AUTOMATION_HANDOFF.md` — updated

## Verification

- Previous render resolver run `34242151623`: full Riftfrontier workflow `SUCCESS`.
- This batch is documentation/reference-only; no Java, Gradle, resource bytes, registry IDs, persistence schema, network payloads, combat timing, or production assets changed.
- New workflow run triggered by this project-doc change must be checked after push; do not claim it green while pending.
- Actual candidate model geometry/rig inspection in Blockbench: NOT RUN.
- Actual production resource existence: NOT TESTED because no production Region 01 boss asset is selected.
- Actual renderer/animation/VFX/sound activation: NOT IMPLEMENTED / NOT TESTED.
- Final production Region 01 boss entity integration: NOT IMPLEMENTED / NOT TESTED.
- Human Minecraft field play: NOT TESTED.

## Do not repeat or revert

- Preserve `AttackPattern` as the single authoritative combat timing source and ACTIVE-only damage semantics.
- Preserve server-owned boss phase cancellation, deterministic attack selection, and `PresentationFrame` → semantic packet timing contract.
- Preserve monotonic client ordering/clear watermark; do not revive stale ACTIVE presentation.
- Preserve fail-closed logical resolver, whole-manifest physical promotion, atomic client resource reload, and exact content-generation matching before render resolution.
- Preserve separation between server datapack `ResourceManager` and client physical resource existence.
- Do not create placeholder production resources or a fake production manifest.
- Do not add GeckoLib only because it is compatible; add it when a selected real animated asset requires it and re-verify coordinates at that moment.
- Do not mark Quaternius or Poly Pizza candidates as selected without visual/rig/scale/performance inspection.
- Do not tune M2 pressure/patrol values without field-play evidence or promote M2 technical proxies to production art/AI.

## Exact next start point

1. Re-check current remote `main` and the CI run triggered by this batch. If any gate fails, fix the first real failure without weakening existing presentation/runtime semantics.
2. If green, inspect a bounded real candidate set from license-eligible source families in Blockbench or an equivalent geometry/animation viewer.
3. For each candidate record exact name/source/license/formats, skeleton and animation inventory, scale/bounds, geometry/material cost, attack-bearing silhouette and whether the required committed-strike / displacement / area-pressure semantic clips can be authored cleanly.
4. Select exactly one only if it passes `REGION_01_BOSS_PRESENTATION_GATE.md`; otherwise record explicit rejection reasons and continue to a new bounded source set rather than lowering the gate.
5. After selection, mark the exact entry `SELECTED` in `THIRD_PARTY_ASSETS.md`, add only permitted asset bytes, re-verify GeckoLib 5.5.1 coordinates if needed, author the first real `presentation_assets` manifest, then wire the existing render resolver to the chosen renderer.
6. Validate actual telegraph/ACTIVE/recovery animation/VFX/sound against authoritative hit windows in Minecraft before declaring production presentation complete.
