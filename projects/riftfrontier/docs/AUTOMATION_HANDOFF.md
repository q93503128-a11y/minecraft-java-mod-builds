# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` verified at this run start: `ac44c6f7b67c5f1d04a0d77d8350b35ec8e8e520`.
- Implementation HEAD completed in this batch: `17b4456c0302f7f169c1f9c4590ae0f5e58f3936`.
- Immediately before this handoff write, remote `main` was still exactly `17b4456c0302f7f169c1f9c4590ae0f5e58f3936`; no concurrent change required reconciliation.
- `Dragon Evolved` remains the selected sanitized Region 01 first-boss geometry/rig/unaltered-source-animation resource. Source `Atlas` art remains excluded and unapproved.
- Preserve source SHA-256 `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`, accepted derivation SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`, and provenance SHA-256 `3e16877a0043cf980ac8de05bb518834c5e77bd72d96103b4984c65a2a5a4c6c`.

## Completed in this batch

M3 server-semantic presentation-resolver provenance closure:

- `ValidatedBossCombatSemantics` now retains/exposes its exact validated `BossPresentationProfile` and validates active presentation state by authoritative `patternId` before resolving assets.
- Presentation `cue`, `delivery`, and phase are cross-checked against the exact validated attack pattern; a valid cue belonging to another attack can no longer redirect rendering.
- Added `BossPresentationResolver.validated(...)`, a fail-closed resolver bound by capability identity to one `ValidatedBossCombatSemantics` instance and its exact presentation profile.
- `Region01BossAnimationPreparation` now prepares and retains the exact validated presentation resolver alongside the reviewed source-window animation bridge.
- `Region01BossClientRenderRuntime.publish(...)` now requires the pipeline to consume the exact prepared resolver, exact validated boss profile, exact validated variant, exact animation bridge, and exact prepared mesh before publication.
- Added regression coverage proving a generic resolver can reproduce the former spoof seam while the validated resolver rejects mismatched cue/delivery, out-of-pool attack ids, wrong boss context, wrong variant, and equal-looking-but-distinct semantic capability substitution.
- No production combat timing/damage/hitbox values, final texture/material/`RenderType`, VFX, sound, or encounter attachment were invented.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/ValidatedBossCombatSemantics.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationResolver.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/Region01BossAnimationPreparation.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/BossCustomGeometryRenderPipeline.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/Region01BossClientRenderRuntime.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationSemanticResolverTest.java`
- `src/test/java/kr/moonseungjun/riftfrontier/client/render/Region01BossAnimationPreparationTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Implementation HEAD `17b4456c0302f7f169c1f9c4590ae0f5e58f3936`, `Build Riftfrontier` run `34424260310`: FULL SUCCESS.
- PASS: Java 25/toolchain, asset-intake tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb graphical client smoke, executable-JAR inspection, build report, deliverable upload, and logs/report upload.
- Local clone/build: NOT RUN successfully because this execution environment could not resolve `github.com`; GitHub Actions is the actual validation source.
- Production final boss texture/material/`RenderType`: NOT APPROVED / NOT AUTHORED. Source `Atlas` remains prohibited.
- Production Region 01 numeric `attack_pattern`/final `boss_profile`: NOT AUTHORED; fixture timings remain non-production.
- Actual encounter attachment, final dimensions/hitbox/scale, VFX/sound, spawned/deformed Dragon graphical capture, and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Dragon source reacquisition, sanitizer acceptance, runtime packaging, eight-clip motion review, `Headbutt`/`Punch` window review, exact reload/geometry provenance, reviewed-animation preparation, boss semantic schema/validator, material/publication provenance, server-semantic ↔ reviewed-animation join, and server-semantic presentation-resolver provenance closure are DONE.
- Do not restore raw `BossAnimationSourceBinding` as a production `prepareBossAnimation(...)` input. Production attack animation preparation must cross `BossAnimationSemanticBinding`.
- Do not allow a free-standing generic `BossPresentationResolver` into Region 01 production publication. The pipeline must retain the exact resolver created by `PreparedAnimation` from the same validated semantic capability.
- Do not treat network `presentationCue` or `delivery` as independent authority: both must match the authoritative `patternId` resolved through validated server combat semantics.
- Never infer gameplay semantics/timing from Dragon clip names, durations, or source `ACTION` windows; never use source `Atlas` as production art.

## Exact next start point

1. Re-check current remote `main`, canonical docs, and this handoff; do not redo completed provenance gates.
2. Check for an actually approved Region 01 final boss texture/material treatment with immutable resource ID, SHA-256, and review evidence. If present, instantiate the production `MaterialReview` and complete same-reload material + semantic-animation + geometry publication.
3. Separately check for legitimate production Region 01 `attack_pattern` and `boss_profile` authoring. Only if real values exist, validate them into `ValidatedBossCombatSemantics`, join them to reviewed source windows with `BossAnimationSemanticBinding`, and wire the authoritative boss runtime. Never copy fixture timing/damage values.
4. If neither exists, next objective M3 server-authority gap is the production construction boundary around `MinecraftBossCombatAdapter`: add a validated-semantics factory/path that derives the boss id and `BossAttackSelectionPolicy.authoredPhases(semantics)` internally and keeps outgoing presentation state tied to that same capability. Do not invent numeric combat values while doing so.
5. Once legitimate server semantics + semantic-reviewed animation + approved material share one current reload, perform actual spawned-boss graphical capture before encounter attachment, hitbox/telegraph tuning, VFX/sound, or balance work.
