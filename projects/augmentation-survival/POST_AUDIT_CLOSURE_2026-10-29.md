# Augmentation Survival — Post-Audit Closure

Date: 2026-10-29

This document records the code fixes and authoritative verification performed after `FINAL_AUDIT_2026-10-29.md`. For the latest verification checkpoint, this document supersedes the older CI checkpoint recorded inside the final-audit report while preserving that report as the full static-audit record.

## Scope closed after the full source audit

Starting audited checkpoint: `d3133cb38bd6af459930a614e65f4265c8aae2b9`.

Post-audit fixes:

- Companion Rescue no longer lets an expired Downed timeout be bypassed by logging out and returning after the timeout. An expired persisted Downed marker now enforces the timeout death when the player state is restored.
- Companion Rescue runtime and regression coverage were restored to the canonical **300-second successful rescue cooldown**. The prior 15-second runtime/test value was a regression against `AUGMENT_CANON_CORRECTION_2026-10-27.md` and the proposal history.
- Aerial Combo no longer relies on the vanilla `Enemy` marker alone. Its ROOT melee recharge uses the shared companion-aware hostile predicate, so dominated companions do not recharge it while compatible hostile/boss targets can participate through the shared hostility policy.
- Powered Flight restores temporary `mayfly` / `flying` / `noGravity` state during `SERVER_STOPPING` before the runtime map is discarded at stop completion.
- `끌어당기기` canon was synchronized with the already-intended runtime: it pulls nearby entities rather than being limited to hostile mobs, while compatibility policy can still deny/control boss pull behavior.

Code commits:

- `3c793ab966362c257bfd72c88c705226f8a07a48` — `fix(augmentation-survival): close final lifecycle regressions`
- `c970b9aebd6b3435da7782413be6896d5f81a0f8` — `fix(augmentation-survival): restore companion rescue cooldown canon`

Canon-only follow-up:

- `15fad4d78959f0a5023e8b93307c8d5d3ef99238` — `docs(augmentation-survival): sync pull target canon [skip ci]`

## Authoritative verification

Authoritative code checkpoint:

`c970b9aebd6b3435da7782413be6896d5f81a0f8`

Workflow:

- `Build Augmentation Survival Pack #444`
- run ID `40870393971`
- result: **SUCCESS**

Verified in #444:

- Java 25 / Gradle 9.5.1 project build: PASS
- required GameTests: **78 / 78 PASS**
- pack dependency/runtime-classpath checks: PASS
- external Korean localization audit: PASS
- dedicated-server smoke: PASS
- client boot smoke: PASS
- verified Modrinth pack generation: PASS
- production JAR structure / metadata / SHA-256 inspection: PASS
- build report and raw-log archive: PASS

Production JAR:

- `augmentation-survival-0.1.0-alpha.1.jar`
- bytes: `1,415,748`
- SHA-256: `862058aafb32dc1a9d601448fcd7ed905f8c228260e8c8a6388692cefa0aa31b`

Verified Modrinth pack:

- `augmentation-survival-0.1.0-alpha.1.mrpack`
- bytes: `1,285,657`
- SHA-256: `a678615a64d0c426c79922f25e45eb38fcab69b97c95e94c5a4d1483ba289ac9`

Verification artifact:

- `augmentation-survival-pack-smoke-444`
- artifact ID `12896400619`
- run `40870393971`

The current main after the canon-only pull documentation commit is a docs-only descendant of the verified code checkpoint. No runtime/source change was made after `c970b9ae` in that canon sync.

## Remaining completion gates

The automated/static gate is closed for the current code checkpoint, but game-completeness validation is not.

- CODE REVIEWED: YES — prior full static inventory plus focused post-audit review
- TESTED: YES — 78 required GameTests in #444
- BUILD VERIFIED: YES — #444 at `c970b9ae`
- JAR PRODUCED / VERIFIED: YES
- PACK PRODUCED / VERIFIED: YES
- DEDICATED SERVER SMOKE: YES
- CLIENT BOOT TESTED: YES
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO
- PROFILER: NOT RUN

Next gate is real Minecraft playtesting, with Rare/Hero regression and feel checks prioritized before broader Legendary/Mythic/manual-polish validation.
