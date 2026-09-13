# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main`, `PROJECT.md`, `docs/CANONICAL.md`, project standards, production data and source are authoritative.

## Current stage

`M3 — PLAYER COMBAT BUILD VERIFIED / REGION 01 BOSS FIELD LINE-CHARGE BUILD VERIFIED / DRAGON ATTACK SOURCE MAPPING PARTIALLY REVIEWED`

Prioritize the playable vertical slice and visible production quality. Do not reopen M2 expedition/runtime/restart authority work or add new lifecycle fences without a demonstrated regression.

## Settled direction — do not redo

- Player combat already has two production weapon families, one technique module, server-owned ItemStack loadout state, authenticated move-id-only intent, authoritative attack lifecycle and client action slots. Final combat keys remain intentionally unbound pending a control-layout decision.
- Human player-combat PLAYTESTED and MULTIPLAYER TESTED remain NO. Use `docs/M3_PLAYER_COMBAT_FIELD_PLAY.md` before tuning provisional geometry or damage.
- Region 01 first-boss geometry/rig remains the selected Dragon Evolved derivation using Riftfrontier's custom skinned-mesh importer/renderer. Do not restart candidate search or reintroduce GeckoLib merely to duplicate this path.
- Accepted sanitized resource: `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`, SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- Canonical third-party registry is `docs/THIRD_PARTY_ASSETS.md`; `assets/sources/region_01_boss_dragon_evolved.source.json` points to its Dragon Evolved entry and records the exact original SHA/license contract. The obsolete root `THIRD_PARTY_ASSETS.md` that incorrectly said no third-party asset was bundled was removed at `16e19be21a9f6ff358c57da4390ef1125b2b8421` after this verified replacement was confirmed.
- Do not restore the stripped source Atlas, invent a final palette, or create fake placeholder `presentation_assets`.

## Verified field-harness checkpoint

Development-only commands remain:

- `/riftfrontier boss fieldtest spawn`
- `/riftfrontier boss fieldtest phase1`
- `/riftfrontier boss fieldtest phase2`

The actor is excluded from natural spawning and production Region 01 encounter composition.

The harness retains role-specific provisional geometry, `1.0F` diagnostic damage, ACTIVE-only server damage, once-per-execution dedupe, temporary vanilla-particle readability instrumentation, and attack-start target commitment. It acquires the nearest alive non-spectator player within a provisional 24-block diagnostic radius only when beginning a new attack, commits facing before TELEGRAPH, and does not home through TELEGRAPH/ACTIVE/RECOVERY.

Production content authors `region_01_line_displacement` as `delivery: line_charge` with `read_travel_lane` / `move_laterally` / `punish_recovery`. The field harness now exercises that authored role physically instead of leaving the boss stationary:

- committed strike: `activeForwardStep = 0.0`
- line displacement: `activeForwardStep = 0.5` blocks per authoritative ACTIVE tick
- arena pressure: `activeForwardStep = 0.0`

`0.5` is field-test calibration, not final boss speed or charge distance. `Region01BossEntity` moves only after an authoritative ACTIVE tick for the line attack, along the already-committed facing, using `MoverType.SELF` so normal Minecraft collision handling remains involved. No generic combat authority, save format, network payload or production encounter composition changed.

Implementation commits:

- `1349b5603eb5dc98925aa2ca142bd78bd371be1c` — field profile movement calibration
- `542769f3b4f0a995e65ef68f8de0640719fca693` — server field actor executes ACTIVE line-charge travel
- `495ee7fd6bd8e9d2e31fedb4cccb378422d954c3` — role/calibration regression coverage
- `c461c65f67fb95c1b4c0808ff68b9a585234ea9e` — preserve geometry-only 4-argument profile construction as explicit zero-travel compatibility

Initial run `34776642466` failed `Tests and clean build` because existing readability-projection tests intentionally constructed geometry-only profiles with the old 4-argument constructor. That was fixed without editing unrelated call sites: geometry-only construction now delegates to `activeForwardStep = 0.0`.

Recovery `Build Riftfrontier` run `34776775338` for checkpoint `c461c65f67fb95c1b4c0808ff68b9a585234ea9e`: **SUCCESS through the complete workflow** — toolchain, asset intake, clean tests/build, required GameTest, dedicated-server smoke, Xvfb client smoke, executable-JAR inspection, reports and deliverable upload all passed. This charge batch is **CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED**. It is not human play.

Human boss field play remains **NOT TESTED**; multiplayer field play remains **NOT TESTED**. `docs/M3_REGION01_BOSS_FIELD_PLAY.md` now explicitly checks no TELEGRAPH travel, ACTIVE-only charge travel, no recovery travel, committed-facing movement, terrain collision, role isolation, damage dedupe and lane-dodge readability. Human evidence, not automation, decides whether the provisional `0.5` needs revision.

## Production boss semantics

Production boss profile: `riftfrontier:boss/region_01_first_apex`.

- phase 1: `region_01_committed_strike` + `region_01_line_displacement`
- phase 2: both above + `region_01_arena_pressure`

The server-authoritative telegraph → ACTIVE → recovery timing remains independent from source animation windows. Current field timing/geometry/travel numbers are not final balance.

## Dragon Evolved source-animation evidence

Direct visual motion review exists for every carried source clip. Exact source-motion phase windows exist for `Punch` and `Headbutt`.

Canonical receipt: `assets/sources/region_01_boss_dragon_evolved.semantic_role_review.json`.
Detailed rationale: `docs/M3_REGION01_BOSS_ANIMATION_ROLE_REVIEW.md`.
Runtime-ready staged mapping: `Region01BossDragonEvolvedStagedAttackBinding`.

Accepted from observed motion, not clip-name similarity:

- `region_01_committed_strike` → `Punch`: TELEGRAPH `0.0–0.2`, ACTIVE-presentational `0.2–0.275`, RECOVERY `0.275–1.0`
- `region_01_line_displacement` → `Headbutt`: TELEGRAPH `0.0–0.15555555555555556`, ACTIVE-presentational `0.15555555555555556–0.2`, RECOVERY `0.2–0.4888888888888889`

These source ACTION windows do **not** authorize server damage timing, target admission, hit geometry, movement distance, lane shape, encounter placement, VFX or sound.

`region_01_arena_pressure` still has no accepted source motion with exact reviewed phase windows. Do not recycle `Fast_Flying`, `Flying_Idle`, `Yes`, `No`, `HitReact`, or `Death` merely to satisfy coverage.

The staged binding therefore covers exactly six logical animation keys and is intentionally **not** a complete production `BossAnimationSemanticBinding`. The exact-coverage gate must continue to prevent renderer publication until arena-pressure motion is reviewed too.

## Presentation and provenance locks

The logical boss presentation profile covers all three attacks × TELEGRAPH/ACTIVE/RECOVERY, but logical model/animation/VFX/sound IDs are contracts rather than proof that physical resources exist. A coherent selected set is required before production publication: complete reviewed animation source binding + reviewed material/texture + VFX/sound resources/provenance as required by the manifest and client render pipeline.

- Reuse the selected Dragon Evolved rig/geometry unless evidence requires changing it.
- External assets require redistribution-compatible licensing, immutable source/revision/hash where practical, modification notes and actual usage records.
- Use strong commercial-game/major-mod references for presentation decisions, but do not copy protected assets, UI, balance values or proprietary code.
- Final material/texture, VFX and sound decisions must be evidence-backed and checked at Minecraft scale.
- Diagnostic `CLOUD` / `CRIT` / `SMOKE` particles are instrumentation, not final VFX.
- Do not create fake manifests or weaken exact-coverage gates to make staged work look complete.

## Exact next development boundary

1. Return to production-visible presentation work rather than adding more field/backend fences: resolve the arena-pressure motion on the selected rig, or advance the real material/texture/VFX/sound set with verified provenance and Minecraft-scale readability.
2. Once all nine logical animation keys have evidence, assemble the full production `BossAnimationSourceBinding` and pass existing exact-coverage validation without weakening it.
3. Human field play of the current charge JAR remains required before changing the provisional `0.5` travel calibration. Record TELEGRAPH/ACTIVE/recovery movement, committed-facing behavior, wall collision and lateral-dodge fairness.
4. Only a coherent physical-resource set may create `presentation_assets` and move the staged logical profile toward renderer-visible production publication.
5. CI, GameTest, dedicated-server smoke and Xvfb client smoke never count as PLAYTESTED or MULTIPLAYER TESTED.
