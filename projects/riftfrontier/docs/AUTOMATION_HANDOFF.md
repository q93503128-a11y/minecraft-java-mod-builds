# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` verified at this run start: `3d21c3b7f9b9de013854f521152df5dceeed7688`.
- Implementation/test HEAD completed and fully validated in this batch: `86e262a5c9c99ec998103ee561d92d89ca65596b`.
- `Dragon Evolved` remains the selected sanitized Region 01 first-boss geometry/rig/unaltered-source-animation resource. Source `Atlas` art remains excluded and unapproved.
- Preserve source SHA-256 `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`, accepted derivation SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`, and provenance SHA-256 `3e16877a0043cf980ac8de05bb518834c5e77bd72d96103b4984c65a2a5a4c6c`.

## Completed in this batch

M3 boss-presentation actor-identity closure:

- Reconfirmed that no approved final Region 01 boss material treatment or legitimate production numeric `attack_pattern` / final `boss_profile` was available; fixture values and source `Atlas` were not promoted.
- Audited the production client render/cache chain and found a real numeric-id-only compatibility surface rather than manufacturing new plumbing.
- Removed `BossPresentationClientState.current(int)`; current presentation lookup now requires `(entityId, UUID)` and fails closed on numeric-id reuse with a mismatched actor UUID.
- Removed `BossCustomGeometryRenderPipeline.prepareCurrent(int)`; the production render preparation boundary now requires `(entityId, UUID)` before it can consume cached authoritative semantics.
- Migrated the network sync contract tests to UUID-qualified lookup without weakening server tick ordering or clear-state semantics.
- Added regression coverage that prevents restoring numeric-id-only cache/render lookup. The render-pipeline API contract is checked source-structurally so API-free JUnit does not class-load Minecraft renderer types.
- No combat timing/damage/hitbox values, final material, VFX, sound, or encounter attachment were invented.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/client/BossPresentationClientState.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/BossCustomGeometryRenderPipeline.java`
- `src/test/java/kr/moonseungjun/riftfrontier/client/BossPresentationClientStateTest.java`
- `src/test/java/kr/moonseungjun/riftfrontier/client/render/BossCustomGeometryRenderPipelineTest.java`
- `src/test/java/kr/moonseungjun/riftfrontier/network/BossPresentationSyncContractTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- `Build Riftfrontier` run `34436087656`, attempt 2, for HEAD `86e262a5c9c99ec998103ee561d92d89ca65596b`: FULL SUCCESS.
- PASS: Java 25/toolchain, asset-intake tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb graphical client smoke, executable-JAR inspection, build report, deliverable upload, and logs/report upload.
- Attempt 1 of the same HEAD passed clean test/build but `riftfrontier:attack_hit_window` transiently failed its native GameTest damage-event assertion. No server combat code or test expectation was changed; an unchanged-HEAD rerun passed the full six-test required GameTest suite and every later gate.
- Local clone/build: NOT RUN successfully in this automation environment; GitHub Actions is the validation source.
- Production final boss texture/material/`RenderType`: NOT APPROVED / NOT AUTHORED. Source `Atlas` remains prohibited.
- Production Region 01 numeric `attack_pattern`/final `boss_profile`: NOT AUTHORED; fixture timings remain non-production.
- Actual encounter attachment, final dimensions/hitbox/scale, VFX/sound, spawned/deformed Dragon graphical capture, and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Dragon source reacquisition, sanitizer acceptance, runtime packaging, eight-clip motion review, `Headbutt`/`Punch` window review, exact reload/geometry provenance, reviewed-animation preparation, boss semantic schema/validator, material/publication provenance, server-semantic ↔ reviewed-animation join, server-semantic presentation-resolver provenance closure, and UUID-qualified cache/render lookup are DONE.
- `MinecraftBossCombatAdapter.validated(...)`, internally derived boss id + `BossAttackSelectionPolicy.authoredPhases(semantics)`, sealed `ValidatedTickResult` network boundary, client equal-tick presentation immutability, and UUID-safe per-actor client lifecycle pruning are DONE.
- Do not restore `BossPresentationClientState.current(int)` or `BossCustomGeometryRenderPipeline.prepareCurrent(int)`. Numeric Minecraft entity ids alone are never sufficient actor authority.
- Do not restore raw `BossAnimationSourceBinding` as a production `prepareBossAnimation(...)` input. Production attack animation preparation must cross `BossAnimationSemanticBinding`.
- Do not allow a free-standing generic `BossPresentationResolver` into Region 01 production publication.
- Never infer gameplay semantics/timing from Dragon clip names, durations, or source `ACTION` windows; never use source `Atlas` as production art.

## Exact next start point

1. Re-check current remote `main`, canonical docs, and this handoff; do not redo completed provenance/order/lifecycle/actor-identity gates.
2. Check for an actually approved Region 01 final boss texture/material treatment with immutable resource ID, SHA-256, and review evidence. If present, instantiate the production `MaterialReview` and complete same-reload material + semantic-animation + geometry publication.
3. Separately check for legitimate production Region 01 `attack_pattern` and `boss_profile` authoring. Only if real values exist, validate them into `ValidatedBossCombatSemantics` and wire them through the already completed validated boss runtime; never copy fixture timing/damage values.
4. If neither exists, audit production `BossPresentationSemanticState` creation/clear call sites for any UUID-less compatibility constructor/factory escaping tests. Do not modify test-only fixture convenience APIs merely for uniformity; remove/restrict a UUID-less path only if an actual production consumer exists.
5. If production semantic creation is already UUID-safe, end this actor-identity subthread and move to the next objective M3 runtime integration bundle from the current roadmap. Do not manufacture boss attachment without a legitimate production boss profile, and do not invent art, hitbox, VFX/sound, or balance values before their evidence/field-play gates.
