# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Canonical design documents and current GitHub `main` remain authoritative.

## Last recovered baseline

- Re-read current remote `main`, then `AGENTS.md` → `docs/BUILD_STANDARD.md` → `docs/QUALITY_STANDARD.md` → `PROJECT.md` → `CANONICAL.md` → `GAME_DESIGN_MASTER.md` → `CONTENT_ARCHITECTURE.md` → `ROADMAP.md`, current M3 reference/runtime files, then this handoff.
- Recovered remote `main` before this batch: `6217e52e4726229957f062faa9f041bc8afe325b`.
- Previous asset-intake workflow run `34255526319` is confirmed full `SUCCESS`.
- No placeholder production resources, fake production manifest, or speculative animation dependency were added.

## Completed in this batch

M3 original-source rig inspection + Region 01 first-boss source selection:

- Obtained the original creator-hosted Quaternius `Dragon_Evolved.gltf` directly from the Ultimate Monsters Google Drive distribution rather than relying on mirror metadata.
- Exact source file: 991,335 bytes; SHA-256 `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`.
- Direct glTF inspection: 1 mesh, 1 material (`Atlas`), 1 skin (`CharacterArmature`), 46 joints, about 4,437 vertices / 7,440 triangles, local extent about 5.4752 × 2.8579 × 2.4156.
- Exact source clips: `Death`, `Fast_Flying`, `Flying_Idle`, `Headbutt`, `HitReact`, `No`, `Punch`, `Yes`.
- Verified independent head/neck, bilateral forelimb/finger, bilateral wing and body-chain rig branches. Source `Punch` and `Headbutt` use articulated motion rather than only whole-body translation.
- Sampled source skinning across `Punch` and `Headbutt`; poses stayed finite/bounded with no catastrophic exploded-vertex deformation in this technical inspection.
- Promoted `Dragon Evolved` from `CANDIDATE` to `SELECTED` specifically as the Region 01 first-boss **geometry/rig derivation source**.
- The original bright low-poly `Atlas` texture/style is explicitly not approved unchanged as final Region 01 art. Production material/texture treatment, authored attack clips, renderer, VFX/sound, hitbox alignment and field-play remain separate gates.
- Blue Demon, Goleling Evolved and Mushroom King remain rejected for this boss role. QAL Bestiary raw source-repository bundling remains unapproved.

## Changed systems/files

- `docs/REGION_01_BOSS_CANDIDATE_AUDIT.md` — source-rig selection decision and exact direct-source evidence.
- `docs/THIRD_PARTY_ASSETS.md` — exact selected asset provenance/hash/use boundary.
- `docs/AUTOMATION_HANDOFF.md` — this recovery update.

## Verification

- Previous asset-intake run `34255526319`: full Riftfrontier workflow `SUCCESS`.
- Original `Dragon_Evolved.gltf`: directly downloaded from creator distribution and parsed locally.
- Direct source SHA-256: `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`.
- Structural glTF/rig/animation inspection: PASS for source-rig selection.
- Bounded sampled deformation inspection of existing `Punch` / `Headbutt`: PASS for catastrophic-deformation rejection only; this is not a final animation-quality approval.
- Current docs-selection CI run `34260793930` for `d3406d44ae6ae17bfe336d120e0fe38ab25cbe0d`: `IN PROGRESS` at last check. Do not claim its clean build/GameTest/server/client/JAR gates succeeded until the run is completed successfully.
- Physical selected model/derived runtime resource committed to `src/main/resources`: NOT YET IMPLEMENTED.
- GeckoLib conversion/runtime integration: NOT IMPLEMENTED / NOT TESTED.
- Production Region 01 material/texture treatment: NOT IMPLEMENTED / NOT TESTED.
- In-Minecraft boss scale/hitbox/animation alignment: NOT TESTED.
- Production VFX/sound timing: NOT IMPLEMENTED / NOT TESTED.
- Human Minecraft combat readability/field play: NOT TESTED.

## Do not repeat or revert

- Preserve `AttackPattern` as the single authoritative combat timing source and ACTIVE-only damage semantics.
- Preserve fail-closed logical resolver, whole-manifest physical promotion, atomic client resource reload, and exact content-generation matching before render resolution.
- `Dragon Evolved` is now the selected geometry/rig derivation source. Do not restart the previous candidate search unless a later deterministic conversion or actual Minecraft quality gate finds a non-fixable blocker.
- Do not treat the original `Atlas` material/texture style as approved final art.
- Do not create placeholder production resources or a fake production manifest.
- Do not add GeckoLib merely because it is compatible; re-verify exact 26.2 coordinates and add it only as part of the concrete selected-rig integration.
- Keep Blue Demon, Goleling Evolved and Mushroom King rejected for the Region 01 first boss unless the canonical boss contract is intentionally changed with a separate decision record.
- Do not raw-bundle QAL Bestiary assets into this public source repository without clearer redistribution permission.
- Do not tune M2 pressure/patrol values without field-play evidence or promote M2 technical proxies to production art/AI.

## Exact next start point

1. Re-check current remote `main` and finish CI run `34260793930`; if any gate fails, fix the first real failure without weakening the source-selection or presentation gates.
2. Re-verify the exact GeckoLib 5.5.1 coordinates/support for Minecraft 26.2 + NeoForge at the moment of integration.
3. Establish the smallest deterministic conversion/import path from the selected `Dragon_Evolved.gltf` skeleton into the chosen Minecraft/GeckoLib resource format. Preserve the selected rig semantics instead of inventing an unrelated replacement skeleton.
4. Add only the exact CC0 selected source/derived runtime files that are actually needed, record their repository paths and hashes, and avoid bundling unrelated pack content.
5. Author distinct committed-strike, line/displacement, arena-pressure and phase-transition presentation clips around authoritative `AttackPattern` telegraph/ACTIVE/recovery timing.
6. Create the first real `presentation_assets` manifest only after physical derived resources exist, then connect `BossPresentationRenderResolver` to the renderer.
7. Validate resource reload, model scale/hitbox alignment, deformation, VFX/sound timing and worst-case boss encounter behavior in Minecraft; human field-play remains mandatory before M3 presentation completion.
