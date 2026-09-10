# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` verified at this run start: `ecad93b277e3ca3434cc12c092f90c5c5b952ee5`.
- Implementation/test HEAD completed and fully validated in this batch: `1734d329b39da8ad006434973321cf60d6489cf8`.
- `Dragon Evolved` remains the selected sanitized Region 01 first-boss geometry/rig/unaltered-source-animation resource. Source `Atlas` art remains excluded and unapproved.
- Preserve source SHA-256 `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`, accepted derivation SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`, and provenance SHA-256 `3e16877a0043cf980ac8de05bb518834c5e77bd72d96103b4984c65a2a5a4c6c`.

## Completed in this batch

M3 client boss-presentation actor lifecycle closure:

- Confirmed no approved final Region 01 boss material treatment or legitimate production numeric `attack_pattern` / final `boss_profile` was available; fixture values and source `Atlas` were not promoted.
- Added `BossPresentationClientState.forgetActor(entityId, uuid)` to retire one exact logical actor's active semantics and hidden ordering watermark when client tracking ends.
- Per-actor removal is UUID-safe: a delayed leave callback for an old actor cannot erase a newer actor that already reused the same numeric entity id.
- Removing a clear-only watermark allows a later lifecycle of the same UUID/entity id to begin at a fresh level-time epoch without weakening equal-tick immutability while the actor is tracked.
- `RiftfrontierClientNetworking` now handles NeoForge `EntityLeaveLevelEvent` only for the logical client side; integrated-server logical-server leave events cannot mutate the client cache.
- Disconnect `clearAll()` remains intact as the whole-connection fallback. No broad per-tick entity scan was added.
- Added pure JUnit regression coverage for exact actor pruning, stale old-UUID leave events, clear-only watermark retirement, and a source-structural client lifecycle contract.
- No combat timing/damage/hitbox values, final material, VFX, sound, or encounter attachment were invented.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/client/BossPresentationClientState.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/RiftfrontierClientNetworking.java`
- `src/test/java/kr/moonseungjun/riftfrontier/client/BossPresentationClientStateTest.java`
- `src/test/java/kr/moonseungjun/riftfrontier/client/RiftfrontierClientPresentationLifecycleTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- `Build Riftfrontier` run `34432161369` for HEAD `1734d329b39da8ad006434973321cf60d6489cf8`: FULL SUCCESS.
- PASS: Java 25/toolchain, asset-intake tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb graphical client smoke, executable-JAR inspection, build report, deliverable upload, and logs/report upload.
- Local clone/build: NOT RUN successfully in this automation environment; GitHub Actions is the validation source.
- Production final boss texture/material/`RenderType`: NOT APPROVED / NOT AUTHORED. Source `Atlas` remains prohibited.
- Production Region 01 numeric `attack_pattern`/final `boss_profile`: NOT AUTHORED; fixture timings remain non-production.
- Actual encounter attachment, final dimensions/hitbox/scale, VFX/sound, spawned/deformed Dragon graphical capture, and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Dragon source reacquisition, sanitizer acceptance, runtime packaging, eight-clip motion review, `Headbutt`/`Punch` window review, exact reload/geometry provenance, reviewed-animation preparation, boss semantic schema/validator, material/publication provenance, server-semantic ↔ reviewed-animation join, and server-semantic presentation-resolver provenance closure are DONE.
- `MinecraftBossCombatAdapter.validated(...)`, internally derived boss id + `BossAttackSelectionPolicy.authoredPhases(semantics)`, sealed `ValidatedTickResult` network boundary, client equal-tick presentation immutability, and UUID-safe per-actor client lifecycle pruning are DONE.
- Do not restore raw `BossAnimationSourceBinding` as a production `prepareBossAnimation(...)` input. Production attack animation preparation must cross `BossAnimationSemanticBinding`.
- Do not allow a free-standing generic `BossPresentationResolver` into Region 01 production publication.
- Do not treat network cue/delivery, same-tick packet arrival order, or numeric entity id alone as independent authority.
- Never infer gameplay semantics/timing from Dragon clip names, durations, or source `ACTION` windows; never use source `Atlas` as production art.

## Exact next start point

1. Re-check current remote `main`, canonical docs, and this handoff; do not redo completed provenance/order/lifecycle gates.
2. Check for an actually approved Region 01 final boss texture/material treatment with immutable resource ID, SHA-256, and review evidence. If present, instantiate the production `MaterialReview` and complete same-reload material + semantic-animation + geometry publication.
3. Separately check for legitimate production Region 01 `attack_pattern` and `boss_profile` authoring. Only if real values exist, validate them into `ValidatedBossCombatSemantics` and wire them through the already completed validated boss runtime; never copy fixture timing/damage values.
4. If neither exists, audit the production render call chain for any remaining numeric-id-only `BossPresentationClientState.current(int)` use. The registered Region 01 renderer currently submits `(entityId, UUID)` through `Region01BossClientRenderRuntime` and `BossCustomGeometryRenderPipeline.prepareCurrent(int, UUID)`; preserve that fail-closed path. Remove/restrict a numeric-only production consumer only if one actually exists; do not manufacture plumbing for pure-test compatibility.
5. If that audit is already clean, move to the next objective M3 runtime integration gap from the current roadmap rather than inventing art or balance. Once legitimate server semantics + semantic-reviewed animation + approved material share one current reload, perform actual spawned-boss graphical capture before encounter attachment, hitbox/telegraph tuning, VFX/sound, or balance work.
