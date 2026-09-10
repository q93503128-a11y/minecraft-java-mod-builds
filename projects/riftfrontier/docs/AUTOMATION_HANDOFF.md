# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` verified at this run start: `602f653c616615252487eef203e8786f0d676b33`.
- Implementation HEAD completed in this batch: `2089cb4d342e8c2ff1aabef097a45a598893370f`.
- Before this handoff write, remote `main` had advanced to `f0d6fa4739f064e62d0925065e3589e0463031b3`; compare confirmed it is six commits ahead of `2089cb4d...`, zero behind, with only fishing-game/turnbound-re changes. Riftfrontier implementation remains in ancestry and was not overwritten.
- `Dragon Evolved` remains the selected sanitized Region 01 first-boss geometry/rig/unaltered-source-animation resource. Source `Atlas` art remains excluded and unapproved.
- Accepted runtime derivation SHA-256 remains `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.

## Completed in this batch

M3 server-semantic ↔ reviewed-animation provenance join:

- Added `ValidatedBossCombatSemantics.requiredLogicalAnimationKeys()` so validated server-authoritative attack semantics expose the exact logical attack-animation keys they require.
- Added evidence-preserving `BossAnimationSourceBinding.subset(...)`; missing required keys fail closed while original motion/window reviews and exact source windows are retained.
- Added `BossAnimationSemanticBinding`, which joins `ValidatedBossCombatSemantics` to a phase-window-reviewed source binding and narrows it to exactly the server-required logical key set.
- `Region01BossAnimationPreparation` now accepts only `BossAnimationSemanticBinding`, resolves only its exact required keys against the accepted runtime animation inventory, and retains that semantic capability through preparation.
- `RiftfrontierClientResources.prepareBossAnimation(...)` no longer accepts a free-standing reviewed source binding.
- `Region01BossClientRenderRuntime.publish(...)` and renderer-visible `SubmissionBinding` now retain and re-check the semantic animation capability, preventing a reviewed-but-unrelated clip binding from entering publication.
- Added regression coverage for exact-key restriction, missing required keys, extra reviewed keys, and unpartitioned/no-fine-window review rejection.
- No production combat timing/damage/hitbox values, final material/texture/RenderType, VFX, sound or encounter attachment were invented.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/ValidatedBossCombatSemantics.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossAnimationSourceBinding.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossAnimationSemanticBinding.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/Region01BossAnimationPreparation.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/Region01BossClientRenderRuntime.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/RiftfrontierClientResources.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/BossAnimationSemanticBindingTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Previous material-provenance run `34415043369`: FULL SUCCESS independently confirmed this run.
- Semantic-animation implementation HEAD `2089cb4d342e8c2ff1aabef097a45a598893370f`, `Build Riftfrontier` run `34419743260`: FULL SUCCESS.
- PASS: Java 25/toolchain, asset-intake tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb graphical client smoke, executable-JAR inspection, build report and deliverable/log uploads.
- Local clone/build: NOT RUN successfully because this execution environment could not resolve `github.com`; GitHub Actions is the actual validation source.
- Production final boss texture/material/`RenderType`: NOT APPROVED / NOT AUTHORED. Source `Atlas` remains prohibited.
- Production Region 01 numeric `attack_pattern`/final `boss_profile`: NOT AUTHORED; fixture timings remain non-production.
- Actual encounter attachment, final dimensions/hitbox/scale, VFX/sound, spawned/deformed Dragon graphical capture and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Dragon source reacquisition, sanitizer acceptance, runtime packaging, eight-clip motion review, `Headbutt`/`Punch` window review, exact reload/geometry provenance, reviewed-animation preparation, boss semantic schema/validator, material/publication provenance, and the new server-semantic ↔ reviewed-animation join are DONE.
- Do not restore raw `BossAnimationSourceBinding` as a production `prepareBossAnimation(...)` input. Production attack animation preparation must cross `BossAnimationSemanticBinding`.
- Do not restore the old free-standing renderer publication seam or permit reviewed clips unrelated to validated server logical keys.
- Preserve source SHA-256 `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`, accepted derivation SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`, and provenance SHA-256 `3e16877a0043cf980ac8de05bb518834c5e77bd72d96103b4984c65a2a5a4c6c`.
- Never infer gameplay semantics/timing from Dragon clip names, durations or source `ACTION` windows; never use source `Atlas` as production art.

## Exact next start point

1. Re-check current remote `main`, canonical docs, and this handoff; do not redo completed provenance gates.
2. Check for an actually approved Region 01 final boss texture/material treatment with immutable resource ID, SHA-256 and review evidence. If present, instantiate the production `MaterialReview` and complete same-reload material + semantic-animation + geometry publication.
3. Separately check for legitimate production Region 01 `attack_pattern` and `boss_profile` authoring. Only if real values exist, validate them into `ValidatedBossCombatSemantics`, join them to reviewed source windows with `BossAnimationSemanticBinding`, and wire the authoritative boss selection/runtime path. Never copy fixture timing/damage values.
4. If neither approved material nor legitimate combat authoring exists, do not invent either. Move to the next objective M3 task that does not require field-play/art assumptions; prefer a substantial server-authority/runtime integration gap over another observation-only helper.
5. Once legitimate server semantics + semantic-reviewed animation + approved material share one current reload, perform actual spawned-boss graphical capture before encounter attachment, hitbox/telegraph tuning, VFX/sound or balance work.
