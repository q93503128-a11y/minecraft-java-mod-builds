# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main`, `PROJECT.md`, `docs/CANONICAL.md`, project standards, production data and source are authoritative.

## Current stage

`M3 — PLAYER COMBAT BUILD VERIFIED / REGION 01 BOSS FIELD HARNESS BUILD VERIFIED / DRAGON ATTACK SOURCE MAPPING PARTIALLY REVIEWED`

Prioritize the playable vertical slice and visible production quality. Do not reopen M2 expedition/runtime/restart authority work or add new lifecycle fences without a demonstrated regression.

## Settled direction — do not redo

- Player combat already has two production weapon families, one technique module, server-owned ItemStack loadout state, authenticated move-id-only intent, authoritative attack lifecycle and client action slots. Final combat keys remain intentionally unbound pending a control-layout decision.
- Human player-combat PLAYTESTED and MULTIPLAYER TESTED remain NO. Use `docs/M3_PLAYER_COMBAT_FIELD_PLAY.md` before tuning provisional geometry or damage.
- Region 01 first-boss geometry/rig remains the selected Quaternius CC0 `Dragon Evolved` derivation using Riftfrontier's custom skinned-mesh importer/renderer. Do not restart candidate search or reintroduce GeckoLib merely to duplicate this path.
- Accepted sanitized resource: `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`, SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- Do not restore the stripped source Atlas, invent a final palette, or create fake placeholder `presentation_assets`.

## Verified field-harness checkpoint

Development-only commands remain:

- `/riftfrontier boss fieldtest spawn`
- `/riftfrontier boss fieldtest phase1`
- `/riftfrontier boss fieldtest phase2`

The actor is excluded from natural spawning and production Region 01 encounter composition.

The harness retains role-specific provisional geometry, `1.0F` diagnostic damage, ACTIVE-only server damage, once-per-execution dedupe, and temporary vanilla-particle readability instrumentation. It acquires the nearest alive non-spectator player within a provisional 24-block diagnostic radius only when beginning a new attack, commits facing before TELEGRAPH, and deliberately does not home through TELEGRAPH/ACTIVE/RECOVERY.

Target-commitment code checkpoint `cc657fc06101d76fdc89fafb86de27110b0ec531`, `Build Riftfrontier` run `34770458284`: **SUCCESS** through the complete workflow. This batch is CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED. It is not human play.

Human boss field play remains NOT TESTED; multiplayer field play remains NOT TESTED. Follow `docs/M3_REGION01_BOSS_FIELD_PLAY.md` exactly.

## Production boss semantics

Production boss profile: `riftfrontier:boss/region_01_first_apex`.

- phase 1: `region_01_committed_strike` + `region_01_line_displacement`
- phase 2: both above + `region_01_arena_pressure`

The server-authoritative telegraph → ACTIVE → recovery timing remains independent from source animation windows. Current timing/geometry numbers are not final balance.

## Dragon Evolved source-animation evidence

Direct visual motion review exists for every carried source clip. Exact source-motion phase windows exist for `Punch` and `Headbutt`.

### Newly reviewed staged role mapping

Canonical receipt: `assets/sources/region_01_boss_dragon_evolved.semantic_role_review.json`.
Detailed rationale: `docs/M3_REGION01_BOSS_ANIMATION_ROLE_REVIEW.md`.
Runtime-ready staged mapping: `Region01BossDragonEvolvedStagedAttackBinding`.

Accepted from observed motion, not clip-name similarity:

- `region_01_committed_strike` → `Punch`
  - TELEGRAPH source window `0.0–0.2`
  - ACTIVE-presentational source window `0.2–0.275`
  - RECOVERY source window `0.275–1.0`
- `region_01_line_displacement` → `Headbutt`
  - TELEGRAPH source window `0.0–0.15555555555555556`
  - ACTIVE-presentational source window `0.15555555555555556–0.2`
  - RECOVERY source window `0.2–0.4888888888888889`

These source ACTION windows do **not** authorize server damage timing, target admission, hit geometry, movement distance, lane shape, encounter placement, VFX or sound.

### Intentionally unresolved

`region_01_arena_pressure` still has no accepted source motion with exact reviewed phase windows. Do not recycle `Fast_Flying`, `Flying_Idle`, `Yes`, `No`, `HitReact`, or `Death` merely to satisfy coverage.

The staged binding therefore covers exactly six logical animation keys and is intentionally **not** a complete production `BossAnimationSemanticBinding`. The existing exact-coverage gate must continue to prevent renderer publication until arena-pressure motion is reviewed too. Do not weaken that gate or publish a partial source binding as complete production coverage.

## Presentation staging

The logical boss presentation profile already covers all three attacks × TELEGRAPH/ACTIVE/RECOVERY, but logical model/animation/VFX/sound IDs are contracts rather than proof that physical resources exist.

A previous GameTest regression established that the logical profile must remain staged when no real selected asset manifest exists. Do not create fake manifests or remove staging. Real publication requires a coherent selected set: complete reviewed animation source binding + reviewed material/texture + VFX/sound resources/provenance as required by the manifest and client render pipeline.

## Asset/provenance locks

- Reuse the selected Dragon Evolved rig/geometry unless evidence requires changing it.
- External assets require redistribution-compatible licensing, immutable source/revision/hash where practical, modification notes and actual usage records.
- Use strong commercial-game/major-mod references for presentation decisions, but do not copy protected assets, UI, balance values or proprietary code.
- Final material/texture, VFX and sound decisions must be evidence-backed and checked at Minecraft scale.
- Diagnostic `CLOUD` / `CRIT` / `SMOKE` particles are instrumentation, not final VFX.

## Exact next development boundary

1. Verify the CI for the staged source-binding checkpoint created after this handoff update; only call it BUILD VERIFIED/JAR PRODUCED if the whole workflow succeeds.
2. Resolve arena-pressure animation without changing the selected rig merely for convenience: obtain/review a legally usable compatible motion or author a documented derivative motion, then perform direct visual review and exact phase-window review.
3. Once all nine logical animation keys have evidence, assemble the full production `BossAnimationSourceBinding` and pass the existing `BossAnimationSemanticBinding.validate(...)`; do not weaken exact coverage.
4. In parallel, select/review a real boss material/texture direction with provenance/license records and Minecraft readability. Never restore the stripped Atlas by default or invent an arbitrary palette.
5. Select/review VFX and sound under the same evidence/provenance rule.
6. Only a coherent physical-resource set may create `presentation_assets` and move the staged logical profile toward renderer-visible production publication.
7. Human player/boss field play is still required. CI, GameTest, dedicated-server smoke and Xvfb client smoke never count as PLAYTESTED or MULTIPLAYER TESTED.
