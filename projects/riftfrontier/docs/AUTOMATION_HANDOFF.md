# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` verified at this run start: `9e5134330f41c8d05a487e30747af71805b734eb`.
- Implementation/test HEAD completed and fully validated in this batch: `df9102e5d595b88d540c25974f7a28f5feb107ae`.
- `Dragon Evolved` remains the selected sanitized Region 01 first-boss geometry/rig/unaltered-source-animation resource. Source `Atlas` art remains excluded and unapproved.
- Preserve source SHA-256 `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`, accepted derivation SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`, and provenance SHA-256 `3e16877a0043cf980ac8de05bb518834c5e77bd72d96103b4984c65a2a5a4c6c`.

## Completed in this batch

M3 server-to-client boss presentation ordering closure:

- Recovered current main and confirmed the previously requested `MinecraftBossCombatAdapter.validated(...)` production boundary and sealed `ValidatedTickResult` were already implemented before this run; they were not repeated.
- `BossPresentationClientState` now treats one `(entity UUID, serverGameTick)` as immutable after first acceptance. Older ticks and all equal-tick rewrites are rejected; exact replays are idempotent.
- Equal-tick active→clear, clear→active, progress/cue/delivery/phase substitution can no longer make packet arrival order rewrite one server-authoritative tick.
- Numeric entity-id reuse by a different UUID still starts a new actor epoch and is allowed at a lower level-time tick; production lookup remains UUID checked.
- Added regression coverage for stale rejection, equal-tick active rewrite rejection, equal-tick clear/reactivation rejection, actor-id reuse and clear watermark isolation.
- Repaired the inherited pure-Java structural runtime test without deleting it: it no longer reflectively loads Minecraft-typed `MinecraftBossCombatAdapter` on the API-free JUnit classpath and instead verifies the factory/sealed-constructor source contract without class loading.
- No combat timing/damage/hitbox values, final material, VFX, sound or encounter attachment were invented.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/client/BossPresentationClientState.java`
- `src/test/java/kr/moonseungjun/riftfrontier/client/BossPresentationClientStateTest.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/MinecraftBossValidatedRuntimeTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- `Build Riftfrontier` run `34429039879` for HEAD `df9102e5d595b88d540c25974f7a28f5feb107ae`: FULL SUCCESS.
- PASS: Java 25/toolchain, asset-intake tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb graphical client smoke, executable-JAR inspection, build report, deliverable upload, and logs/report upload.
- Earlier runs `34428811875` and `34428954257` failed and were repaired at their first real causes: inherited API-free JUnit class loading of Minecraft runtime types, then an exact source-signature assertion typo. Do not treat those failed runs as successful evidence.
- Local clone/build: NOT RUN successfully because this execution environment could not resolve `github.com`; GitHub Actions is the actual validation source.
- Production final boss texture/material/`RenderType`: NOT APPROVED / NOT AUTHORED. Source `Atlas` remains prohibited.
- Production Region 01 numeric `attack_pattern`/final `boss_profile`: NOT AUTHORED; fixture timings remain non-production.
- Actual encounter attachment, final dimensions/hitbox/scale, VFX/sound, spawned/deformed Dragon graphical capture, and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Dragon source reacquisition, sanitizer acceptance, runtime packaging, eight-clip motion review, `Headbutt`/`Punch` window review, exact reload/geometry provenance, reviewed-animation preparation, boss semantic schema/validator, material/publication provenance, server-semantic ↔ reviewed-animation join, and server-semantic presentation-resolver provenance closure are DONE.
- `MinecraftBossCombatAdapter.validated(...)`, internally derived boss id + `BossAttackSelectionPolicy.authoredPhases(semantics)`, sealed `ValidatedTickResult` network boundary, and client equal-tick presentation immutability are DONE.
- Do not restore raw `BossAnimationSourceBinding` as a production `prepareBossAnimation(...)` input. Production attack animation preparation must cross `BossAnimationSemanticBinding`.
- Do not allow a free-standing generic `BossPresentationResolver` into Region 01 production publication.
- Do not treat network cue/delivery or same-tick packet arrival order as independent authority.
- Never infer gameplay semantics/timing from Dragon clip names, durations, or source `ACTION` windows; never use source `Atlas` as production art.

## Exact next start point

1. Re-check current remote `main`, canonical docs, and this handoff; do not redo completed provenance/order gates.
2. Check for an actually approved Region 01 final boss texture/material treatment with immutable resource ID, SHA-256, and review evidence. If present, instantiate the production `MaterialReview` and complete same-reload material + semantic-animation + geometry publication.
3. Separately check for legitimate production Region 01 `attack_pattern` and `boss_profile` authoring. Only if real values exist, validate them into `ValidatedBossCombatSemantics` and wire them through the already completed validated boss runtime; never copy fixture timing/damage values.
4. If neither exists, close the next objective client-runtime lifecycle gap: add UUID-safe per-actor removal/pruning for `BossPresentationClientState` on client entity unload/removal so stale actor watermarks do not live until logout, while preserving disconnect `clearAll()`, numeric-id reuse semantics and equal-tick immutability. Cover the lifecycle with pure JUnit and client/native validation; do not add broad per-tick scans.
5. Once legitimate server semantics + semantic-reviewed animation + approved material share one current reload, perform actual spawned-boss graphical capture before encounter attachment, hitbox/telegraph tuning, VFX/sound, or balance work.
