# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main`, `PROJECT.md`, `docs/CANONICAL.md`, project standards, production data and source are authoritative.

## Current stage

`M3 — PLAYER COMBAT BUILD VERIFIED / REGION 01 BOSS FIELD HARNESS CHARGE BATCH IN VERIFICATION / DRAGON ATTACK SOURCE MAPPING PARTIALLY REVIEWED`

Prioritize the playable vertical slice and visible production quality. Do not reopen M2 expedition/runtime/restart authority work or add new lifecycle fences without a demonstrated regression.

## Settled direction — do not redo

- Player combat already has two production weapon families, one technique module, server-owned ItemStack loadout state, authenticated move-id-only intent, authoritative attack lifecycle and client action slots. Final combat keys remain intentionally unbound pending a control-layout decision.
- Human player-combat PLAYTESTED and MULTIPLAYER TESTED remain NO. Use `docs/M3_PLAYER_COMBAT_FIELD_PLAY.md` before tuning provisional geometry or damage.
- Region 01 first-boss geometry/rig remains the selected Dragon Evolved derivation using Riftfrontier's custom skinned-mesh importer/renderer. Do not restart candidate search or reintroduce GeckoLib merely to duplicate this path.
- Accepted sanitized resource: `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`, SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- Do not restore the stripped source Atlas, invent a final palette, or create fake placeholder `presentation_assets`.
- The root `THIRD_PARTY_ASSETS.md` currently says there are no bundled third-party assets even though the sanitized Dragon resource is present. Do not invent provenance to paper over this discrepancy; reconcile it from the existing source receipts/immutable upstream evidence before the next external-asset promotion.

## Verified field-harness baseline

Development-only commands remain:

- `/riftfrontier boss fieldtest spawn`
- `/riftfrontier boss fieldtest phase1`
- `/riftfrontier boss fieldtest phase2`

The actor is excluded from natural spawning and production Region 01 encounter composition.

The harness retains role-specific provisional geometry, `1.0F` diagnostic damage, ACTIVE-only server damage, once-per-execution dedupe, and temporary vanilla-particle readability instrumentation. It acquires the nearest alive non-spectator player within a provisional 24-block diagnostic radius only when beginning a new attack, commits facing before TELEGRAPH, and deliberately does not home through TELEGRAPH/ACTIVE/RECOVERY.

Target-commitment code checkpoint `cc657fc06101d76fdc89fafb86de27110b0ec531`, `Build Riftfrontier` run `34770458284`: **SUCCESS** through the complete workflow. That baseline is CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED. It is not human play.

The later Dragon staged-source-binding checkpoint `5e7c96f4f4cca13246e58352617a629ab7c3755e`, `Build Riftfrontier` run `34773453745`, also completed **SUCCESS** through the whole workflow. That batch is CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED.

Human boss field play remains NOT TESTED; multiplayer field play remains NOT TESTED. Follow `docs/M3_REGION01_BOSS_FIELD_PLAY.md` exactly.

## Current field-play charge batch

Production content already authors `region_01_line_displacement` as `delivery: line_charge` with counterplay `read_travel_lane` + `move_laterally` + `punish_recovery`. The earlier field harness represented the long narrow lane but left the boss physically stationary, so the authored travel-lane meaning was not actually exercised in Minecraft.

Current `main` now calibrates field-only ACTIVE travel in the same `Region01BossFieldImpactProfile` that owns provisional hit geometry:

- committed strike: `activeForwardStep = 0.0`
- line displacement: `activeForwardStep = 0.5` blocks per authoritative ACTIVE tick
- arena pressure: `activeForwardStep = 0.0`

`0.5` is a provisional human-test calibration, not final boss speed or balance. It remains below the six-block line threat reach and must not be tuned from CI feel guesses.

`Region01BossEntity` applies that movement only after an authoritative tick reports an ACTIVE presentation frame for the line attack. It moves along the already-committed facing with `MoverType.SELF`, so normal Minecraft collision handling remains involved. The boss still does not rotate toward the target after TELEGRAPH starts. No generic combat authority, save format, network payload or production encounter composition was changed.

Batch commits:

- `1349b5603eb5dc98925aa2ca142bd78bd371be1c` — field profile movement calibration
- `542769f3b4f0a995e65ef68f8de0640719fca693` — server field actor executes ACTIVE line-charge travel
- `495ee7fd6bd8e9d2e31fedb4cccb378422d954c3` — role/calibration regression coverage

`Build Riftfrontier` run `34776642466` is the verification run for code checkpoint `495ee7fd6bd8e9d2e31fedb4cccb378422d954c3`. At handoff-writing time it is **IN PROGRESS**. Do not call this new charge batch BUILD VERIFIED or JAR PRODUCED until the complete workflow is green.

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

- `region_01_committed_strike` → `Punch`
  - TELEGRAPH source window `0.0–0.2`
  - ACTIVE-presentational source window `0.2–0.275`
  - RECOVERY source window `0.275–1.0`
- `region_01_line_displacement` → `Headbutt`
  - TELEGRAPH source window `0.0–0.15555555555555556`
  - ACTIVE-presentational source window `0.15555555555555556–0.2`
  - RECOVERY source window `0.2–0.4888888888888889`

These source ACTION windows do **not** authorize server damage timing, target admission, hit geometry, movement distance, lane shape, encounter placement, VFX or sound.

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

1. Check `34776642466`; only label the new charge batch TESTED / BUILD VERIFIED / JAR PRODUCED to the extent its gates actually finish successfully.
2. Update the human boss field-play contract so line displacement explicitly checks physical ACTIVE travel, no pre-telegraph travel, no recovery travel, committed-facing movement and wall/collision behavior. Human evidence, not automation, decides whether `0.5` needs revision.
3. Then return to production-visible presentation work: resolve arena-pressure animation without changing the selected rig merely for convenience, or advance the real material/texture/VFX/sound set with verified provenance.
4. Once all nine logical animation keys have evidence, assemble the full production `BossAnimationSourceBinding` and pass the existing exact-coverage validation; never weaken it.
5. Reconcile the root third-party ledger against the existing Dragon source receipts before claiming the bundled asset ledger is complete.
6. Only a coherent physical-resource set may create `presentation_assets` and move the staged logical profile toward renderer-visible production publication.
7. Human player/boss field play is still required. CI, GameTest, dedicated-server smoke and Xvfb client smoke never count as PLAYTESTED or MULTIPLAYER TESTED.
