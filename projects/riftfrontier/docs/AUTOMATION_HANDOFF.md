# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Canonical design documents and current GitHub `main` remain authoritative.

## Last recovered baseline

- Re-read current remote `main`, then `AGENTS.md` → `docs/BUILD_STANDARD.md` → `docs/QUALITY_STANDARD.md` → `PROJECT.md` → `CANONICAL.md` → `GAME_DESIGN_MASTER.md` → `CONTENT_ARCHITECTURE.md` → `ROADMAP.md`, current M3 reference/runtime files, then this handoff.
- Recovered remote `main` before this batch: `be7eaab6ef27a6f44509ce971e6a58ca63a7cd0e`.
- Previous Region 01 presentation-gate workflow run `34248910052` is confirmed full `SUCCESS`.
- Repository still has no selected production Region 01 boss model/animation/VFX/sound assets and no fake production manifest.

## Completed in this batch

M3 real-candidate bounded audit + deterministic glTF technical intake gate:

- Audited a bounded CC0 candidate set from Quaternius Ultimate Monsters against `REGION_01_BOSS_PRESENTATION_GATE.md`.
- Rejected Blue Demon, Goleling Evolved and Mushroom King for the first Region 01 boss instead of lowering the combat/presentation gate.
- Kept Dragon Evolved as the only conditional technical front-runner, **NOT SELECTED**. Public evidence shows a useful multi-channel silhouette and manageable reported geometry, but exact rig hierarchy, clip names, materials, deformation and Minecraft-scale presentation remain unverified.
- Researched the newer 2026 Quaternius Bestiary kit and QAL v1.0. It is not approved for raw public source-repository bundling under current evidence because QAL permits completed-Product distribution while restricting redistribution of the assets themselves.
- Added `tools/inspect_gltf.py`, a standard-library GLB/glTF 2.0 inspector that reports geometry, material, skin/bone, animation and bounds metadata and can fail requested technical budgets with stable issue codes.
- Added deterministic unit tests using synthetic non-production GLB/glTF fixtures. The fixture exists only in a temporary test directory and is never bundled as a production resource.
- Added `REGION_01_BOSS_CANDIDATE_AUDIT.md` as the bounded decision record.
- Updated `THIRD_PARTY_ASSETS.md` with exact candidate decisions and the QAL repository-distribution boundary.

## Changed systems/files

- `tools/inspect_gltf.py` — new
- `tools/tests/test_inspect_gltf.py` — new
- `docs/REGION_01_BOSS_CANDIDATE_AUDIT.md` — new
- `docs/THIRD_PARTY_ASSETS.md` — updated
- `docs/AUTOMATION_HANDOFF.md` — updated

## Verification

- Previous Region 01 presentation-gate run `34248910052`: full Riftfrontier workflow `SUCCESS`.
- `python3 -m unittest discover -s tools/tests -p 'test_*.py'`: 3 tests, `OK` in the automation working environment.
- Tests cover valid GLB metadata/bounds, stable technical gate issue codes, and malformed GLB length rejection.
- Current project workflow triggered by this batch must still be checked after push; do not claim it green while pending.
- Original Dragon Evolved binary downloaded from original creator source: NOT RUN in this environment.
- Exact Dragon Evolved skeleton hierarchy / clip names / material count / deformation: NOT TESTED.
- Actual production resource existence: NOT TESTED because no production Region 01 boss asset is selected.
- Actual renderer/animation/VFX/sound activation: NOT IMPLEMENTED / NOT TESTED.
- Human Minecraft combat readability/field play: NOT TESTED.

## Do not repeat or revert

- Preserve `AttackPattern` as the single authoritative combat timing source and ACTIVE-only damage semantics.
- Preserve fail-closed logical resolver, whole-manifest physical promotion, atomic client resource reload, and exact content-generation matching before render resolution.
- Do not create placeholder production resources or a fake production manifest.
- Do not add GeckoLib only because it is compatible.
- Do not mark Dragon Evolved `SELECTED` from public previews/metadata alone.
- Keep Blue Demon, Goleling Evolved and Mushroom King rejected for the first Region 01 boss unless the canonical boss contract itself is intentionally changed for a separately documented reason.
- Do not raw-bundle QAL Bestiary assets into this public source repository without clearer redistribution permission.
- `tools/inspect_gltf.py` is a technical metadata gate only; never treat its PASS as visual/license/production approval.
- Do not tune M2 pressure/patrol values without field-play evidence or promote M2 technical proxies to production art/AI.

## Exact next start point

1. Re-check current remote `main` and the `Build Riftfrontier` run triggered by this batch. Fix the first real failure if any.
2. Obtain the original CC0 Dragon Evolved GLB in an environment that can inspect binary assets locally.
3. Run `tools/inspect_gltf.py --json`, then inspect exact skeleton hierarchy, animation clip names, materials, axes/bounds and deformation in Blockbench/Blender or equivalent.
4. Verify that distinct committed-strike telegraph/ACTIVE/recovery, line/displacement telegraph and arena-pressure telegraph poses can be authored cleanly from the rig.
5. Only if those checks pass, mark Dragon Evolved `SELECTED`, add permitted resource bytes, re-verify the chosen animation runtime coordinates, author the first real `presentation_assets` manifest and connect the renderer.
6. If it fails, start a new bounded redistribution-safe candidate set; do not lower the gate.
