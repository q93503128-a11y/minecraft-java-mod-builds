# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Canonical design documents and current GitHub `main` remain authoritative.

## Last recovered baseline

- Baseline main inspected before this batch: `799ace297ba75a1133d2d50c539b8dbc0b6ac645`
- Concurrent repository activity occurred during writeback; Riftfrontier files were therefore applied through the GitHub contents API on top of moving `main` without force push.

## Completed in this batch

M3 production-combat content contract foundation:

- Added typed `attack_pattern` content definition.
- Added typed `boss_profile` composition definition.
- Added codec support for both content kinds.
- Added validator errors for missing counterplay, missing boss attack patterns, dangling attack-pattern references and missing arena/presentation contracts.
- Added JUnit coverage for cadence decoding, counterplay validation and boss-to-pattern reference resolution.
- Added `M3_COMBAT_REFERENCE_DOSSIER.md` separating architectural combat rules from still-unapproved final art/animation/VFX/sound.

## Changed systems/files

- `content/CoreDefinition.java`
- `content/ContentDocumentCodec.java`
- `content/ContentValidator.java`
- `src/test/.../content/CombatDefinitionTest.java`
- `docs/M3_COMBAT_REFERENCE_DOSSIER.md`

## Verification

- GitHub write: SUCCESS.
- Unit/clean build/GameTest/server/client/JAR CI for this exact batch: PENDING at handoff creation time; do not call this batch verified until the workflow reaches success.
- Actual Minecraft field play: NOT TESTED.
- Final production combat presentation: NOT DESIGNED / NOT TESTED by design gate.

## Do not repeat or revert

- Do not rebuild M2 extraction/restart/evidence infrastructure.
- Do not rename M2 Zombie/Skeleton/Ravager technical proxies into production creatures.
- Do not invent final Region 01 boss art or VFX before presentation references/assets are approved.
- Preserve `telegraph -> active -> recovery` as one authoritative attack-pattern timing source for future animation/hit-volume integration.

## Exact next start point

1. Check the latest `main` and the newest `Build Riftfrontier` workflow result caused by this batch.
2. If CI fails, fix the first real compilation/test failure without reducing the new schema contract.
3. If CI succeeds, keep M2 balance changes gated on human field-play evidence and continue M3 by designing the runtime attack state machine that consumes `AttackPattern` timing; do not lock final assets yet.
4. Author Region 01 production boss/attack data only after the runtime contract is stable and M2 field-play evidence is available.
