# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Canonical design documents and current GitHub `main` remain authoritative.

## Last recovered baseline

- Baseline main inspected before this batch: `799ace297ba75a1133d2d50c539b8dbc0b6ac645`
- Concurrent repository activity occurred during writeback; Riftfrontier files were applied through the GitHub contents API without force-pushing or discarding other project commits.

## Completed in this batch

M3 production-combat content contract foundation:

- Added typed `attack_pattern` content definition with authoritative `telegraph -> active -> recovery` cadence, delivery, counterplay and presentation-cue policy.
- Added typed `boss_profile` composition definition with phase count, attack-pattern references and arena rule.
- Added strict JSON codec support for both content kinds.
- Added validator errors for missing counterplay, missing boss attack patterns, dangling attack-pattern references and missing arena/presentation contracts.
- Added JUnit coverage for cadence decoding, invalid timing, counterplay validation and boss-to-pattern reference resolution.
- Added `M3_COMBAT_REFERENCE_DOSSIER.md`, separating architectural combat rules from still-unapproved final art/animation/VFX/sound.
- Fixed the JVM-test dependency boundary exposed by CI: Gson is now `testImplementation`, because test source compiles against the codec API that publicly mentions Gson types.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/content/CoreDefinition.java`
- `src/main/java/kr/moonseungjun/riftfrontier/content/ContentDocumentCodec.java`
- `src/main/java/kr/moonseungjun/riftfrontier/content/ContentValidator.java`
- `src/test/java/kr/moonseungjun/riftfrontier/content/CombatDefinitionTest.java`
- `build.gradle`
- `docs/M3_COMBAT_REFERENCE_DOSSIER.md`

## Verification

- GitHub main write: SUCCESS.
- First `Build Riftfrontier` run `34175660269`: FAILED at `compileTestJava`; production `compileJava` succeeded. Root cause was Gson being runtime-only while the new test compiled against `ContentDocumentCodec`'s Gson-visible API. No schema/runtime feature was removed to pass the build.
- Fix commit: `19237976701c1a3db36a443efbe54e56cd1976f0`.
- Follow-up `Build Riftfrontier` run `34175769123`: at last observation, `Tests and clean build` SUCCESS and required `Riftfrontier GameTest gate` SUCCESS; dedicated server smoke was still IN PROGRESS. Full workflow conclusion, client smoke and JAR inspection are therefore NOT YET VERIFIED in this handoff revision.
- Actual Minecraft field play: NOT TESTED.
- Final production combat presentation: NOT DESIGNED / NOT TESTED by design gate.

## Do not repeat or revert

- Do not rebuild M2 extraction/restart/evidence infrastructure.
- Do not rename M2 Zombie/Skeleton/Ravager technical proxies into production creatures.
- Do not invent final Region 01 boss art or VFX before presentation references/assets are approved.
- Preserve `telegraph -> active -> recovery` as one authoritative attack-pattern timing source for future animation and hit-volume integration.
- Do not move Gson back to `testRuntimeOnly` while tests compile against the codec API.

## Exact next start point

1. Check latest `main` and finish verification of `Build Riftfrontier` run `34175769123` (or the latest equivalent run if repository activity superseded it).
2. If the remaining gate fails, fix the first actual failure without weakening the new combat schema.
3. If the full gate succeeds, keep M2 pressure/patrol numerical tuning gated on human field-play evidence and continue M3 with a pure/runtime attack state machine that consumes `AttackPattern` timing; runtime animation/hit-volume adapters must read the same cadence rather than duplicating constants.
4. Do not author final Region 01 boss art/assets yet. Production boss/attack data should follow once runtime semantics are stable; numerical combat tuning must be reconciled with M2 field-play evidence.
