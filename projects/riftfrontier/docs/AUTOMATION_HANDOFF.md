# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main`, `PROJECT.md`, `docs/CANONICAL.md`, project standards, production data and source are authoritative.

## Current stage

`M3 — PLAYER COMBAT BUILD VERIFIED / REGION 01 BOSS FIELD LINE-CHARGE BUILD VERIFIED / FIRST EXPEDITION IN-WORLD EXTRACTION RELAY IMPLEMENTED / DRAGON ATTACK SOURCE MAPPING PARTIALLY REVIEWED`

Prioritize the playable vertical slice and visible production quality. Do not reopen settled expedition authority/restart work or add new lifecycle fences without a demonstrated regression.

## Settled direction — do not redo

- Riftfrontier's first vertical slice must close a real prepare -> deploy -> fight/recover -> extract -> hub/provision loop before expanding regions.
- Player combat already has two production weapon families, one technique module and server-authoritative attack/loadout state. Human player-combat PLAYTESTED and MULTIPLAYER TESTED remain NO.
- Region 01 first-boss geometry/rig remains the selected Quaternius Dragon Evolved derivation using Riftfrontier's custom skinned-mesh importer/renderer. Do not restart candidate search or reintroduce GeckoLib just to duplicate this path.
- Accepted sanitized resource: `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`, SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- Canonical third-party registry is `docs/THIRD_PARTY_ASSETS.md`. The source Atlas is reference/provenance only and is explicitly not approved final production art. Do not restore it, invent a final palette, create fake `presentation_assets`, or weaken exact-coverage gates.

## First expedition loop — new in-world extraction checkpoint

The M2-B gameplay adapter already had physical salvage recovery but extraction was still command-only. Commit `2ed3db36a7d374952d2e0cf209044fdd68aeded5` adds `ExpeditionFieldExtractionRelay` and connects it through the existing server-side `PlayerInteractEvent.RightClickBlock` adapter.

Behavior:

- after an owned Region 01 run is DEPLOYED and the player begins field block interaction, a **temporary lodestone relay** materializes at `technicalRegionCenter + (0, 0, 5)`;
- the relay is inside the existing 11x11 technical cell and does not overlap any salvage node;
- right-clicking the relay calls the existing authoritative `ExpeditionGameplayService.extract(...)` path;
- objective satisfaction, ownership, retention, patrol bonus, region pressure, evidence and terminal state remain owned by the existing lifecycle/service;
- early relay use is caught as an authoritative rejection and does not create a fake extraction state;
- `/riftfrontier expedition extract` remains available as a diagnostic/fallback command;
- the lodestone is technical validation presentation only, not final Region 01 extraction-device art or UI.

Human checklist: `docs/M2B_IN_WORLD_EXTRACTION_RELAY.md`.

Initial CI run `34780037763` failed only at `compileTestJava`: the added JUnit fixture imported `net.minecraft.core.BlockPos`, but this project's pure `src/test` classpath intentionally does not expose Minecraft classes. The failure artifact proved `package net.minecraft.core does not exist`; production/main source was not the reported failure. Commit `5d4614eb933c127f7e8d6409a3e26810805f8463` removes that invalid fixture rather than widening the pure unit-test classpath.

Recovery CI run `34780169928` for `5d4614eb...` has already passed toolchain, asset-intake, clean test/build and required GameTest. At this handoff update the dedicated-server smoke was still running, so do not upgrade the relay batch to full BUILD VERIFIED/JAR PRODUCED until the run is observed complete and successful.

`PLAYTESTED: NO`. `MULTIPLAYER TESTED: NO`. CI/GameTest/Xvfb never count as either.

## Region 01 boss field harness

Development-only commands remain:

- `/riftfrontier boss fieldtest spawn`
- `/riftfrontier boss fieldtest phase1`
- `/riftfrontier boss fieldtest phase2`

The actor remains excluded from natural spawning and production encounter composition. It uses role-specific provisional geometry, `1.0F` diagnostic damage, ACTIVE-only server damage, once-per-execution dedupe, attack-start target commitment and temporary vanilla-particle readability instrumentation.

Production `region_01_line_displacement` is a `line_charge`; the field harness now physically advances only during authoritative ACTIVE at provisional `activeForwardStep = 0.5` blocks/tick along committed facing. TELEGRAPH/recovery do not travel and the attack does not home after start. This value is not final balance.

Checkpoint `c461c65f67fb95c1b4c0808ff68b9a585234ea9e`, CI `34776775338`: full workflow SUCCESS through tests/build, required GameTest, dedicated-server smoke, Xvfb client smoke, executable-JAR inspection and deliverable upload. That charge batch is **CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED**; human and multiplayer play remain NO.

Human boss checklist: `docs/M3_REGION01_BOSS_FIELD_PLAY.md`.

## Production boss semantics and source animation evidence

Production profile: `riftfrontier:boss/region_01_first_apex`.

- phase 1: `region_01_committed_strike` + `region_01_line_displacement`
- phase 2: both above + `region_01_arena_pressure`

Reviewed Dragon Evolved mappings are motion-evidence-based, not clip-name guesses:

- `region_01_committed_strike` -> `Punch`: TELEGRAPH `0.0–0.2`, presentational ACTIVE `0.2–0.275`, RECOVERY `0.275–1.0`
- `region_01_line_displacement` -> `Headbutt`: TELEGRAPH `0.0–0.15555555555555556`, presentational ACTIVE `0.15555555555555556–0.2`, RECOVERY `0.2–0.4888888888888889`

These windows never authorize server damage timing, hit geometry, movement or target admission.

`region_01_arena_pressure` still has no accepted source motion with exact reviewed phase windows. Do not recycle `Fast_Flying`, `Flying_Idle`, `Yes`, `No`, `HitReact`, or `Death` simply to reach 9/9 coverage. The staged binding therefore remains intentionally incomplete at six logical animation keys.

## Presentation/provenance locks

The packaged Region 01 boss presentation currently contains the sanitized GLTF but no approved final texture resource. `Region01BossMaterialPreparation` requires a real material texture through the existing resource/atlas contract. The original Dragon Evolved Atlas is not an approved final treatment, so do not restore it as a shortcut.

- Reuse the selected Dragon Evolved rig/geometry unless evidence requires changing it.
- External assets require redistribution-compatible licensing, immutable source/revision/hash where practical, modification notes and actual usage records.
- Use strong commercial-game/major-mod references for presentation decisions, but do not copy protected assets, UI, balance values or proprietary code.
- Diagnostic `CLOUD` / `CRIT` / `SMOKE` particles are instrumentation, not final VFX.
- Only a coherent physical resource set may create `presentation_assets` and publish the staged logical boss profile.

## Exact next development boundary

1. First check recovery CI `34780169928`. If fully green, record the relay batch as CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED and use its deliverable JAR for the exact human steps in `docs/M2B_IN_WORLD_EXTRACTION_RELAY.md`.
2. Do not add more expedition authority/lifecycle fences unless the relay field test exposes a real regression. The next automatic implementation should return to visible Region 01 quality: resolve an evidence-backed arena-pressure motion **or** advance the approved material/texture/VFX/sound set with provenance and Minecraft-scale readability.
3. Once all nine animation logical keys have evidence, assemble the full production `BossAnimationSourceBinding` without weakening exact coverage.
4. Human field play remains required before changing provisional boss charge travel or calling the in-world relay polished/final.
5. Never claim PLAYTESTED or MULTIPLAYER TESTED from CI, GameTest, dedicated-server smoke or Xvfb client smoke.
