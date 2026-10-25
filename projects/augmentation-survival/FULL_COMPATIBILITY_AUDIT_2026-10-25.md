# Augmentation Survival — Full Compatibility Audit

Date: 2026-10-25
Scope: full 146-augment runtime + official 23-artifact external pack candidate
Status: FINAL VERIFICATION CHECKPOINT STARTED

## Runtime roster

- FULL: 146 / 146
- PARTIAL: 0 / 146
- NO RUNTIME: 0 / 146

This count means canonical gameplay runtime coverage. It does not claim live playtesting, visual-polish completion, statistical balance validation, or multiplayer validation.

## Static audit coverage

Reviewed against current `main`, repository build/quality standards, the canonical augment roster, compatibility profiles, the built JAR, and the pinned external pack.

Checked:

- Fabric/Minecraft/Java/Gradle/Loom pins
- `fabric.mod.json` dependency contract
- all configured Mixin source presence
- optional Better Combat mixins remain `@Pseudo`/soft-linked
- no hard compile-time imports from Chrono Dawn, World Bosses, or MCEndgame
- canonical augment catalog contains 146 unique definitions
- data/resource JSON parses cleanly
- player-facing translations contain no development-stage/TODO/debug wording
- own block/model references are present
- external pack top-level artifacts resolve without duplicate Fabric mod IDs
- boss compatibility binding covers Vanilla, Chrono Dawn, World Bosses and dynamic MCEndgame identity
- server-authoritative damage/drop/progression boundaries retained
- save codec keeps optional/default fields for backwards-compatible state decoding

## Runtime corrections discovered by this audit

### Dexterity final-drop boundary

The first implementation re-evaluated `Block.getDrops` after a break. That could re-roll random loot and select a bonus item that was not part of the actual emitted result.

Corrected behavior:

- roll Dexterity once for the actual `destroyBlock` context;
- observe the real `Block.popResource` output;
- add exactly one copy of the first eligible stack that actually exists;
- never re-roll the loot table;
- preserve nested/derived harvesting contexts;
- execute before Mythic `Abundance`, allowing the final-drop multiplier to multiply compatible additions consistently.

### Wall Slam knockback timing and boss policy

The first implementation checked target horizontal velocity inside `AFTER_DAMAGE`, which can run before vanilla/compatible melee knockback is applied.

Corrected behavior:

- qualifying root melee damage creates a short pending collision window;
- external boss `allow_wall_impact` compatibility policy is enforced;
- a dedicated `LivingEntity.knockback` tail hook arms that window only after an actual same-tick knockback impulse exists and passes the configured minimum horizontal-motion threshold;
- wall collision consumes the armed window once;
- derived wall damage remains non-root and cannot recurse.

## External-pack compatibility observations

Known non-fatal warnings from the prior full-pack smoke are tracked rather than hidden:

- optional Mod Menu/YACL recommendations are absent;
- Naturalist optional Field Guide mixin targets are absent;
- Illager Invasion ships a NeoForge-specific villager-distance data map that Fabric ignores;
- World Bosses 2.0.2 logs several particle/texture-reference warnings for worldbreaker tools and the Varkhul trophy;
- CI has expected unauthenticated Realms/auth, narrator/flite and audio-device warnings;
- Gradle reports deprecation warnings relevant to future Gradle 10 migration.

These did not prevent the previous 23-artifact server/client smoke from reaching ready/initialized state. The current corrected checkpoint is being reverified rather than relying on that older result.

## Distribution policy

The final user-facing pack must not bundle third-party JARs whose redistribution permission is not established. In particular, World Bosses is treated as an external pinned dependency rather than copied into our distributable archive. The deliverable will therefore use a launcher-resolvable modpack manifest plus our own verified JAR and project-owned configuration overrides.

MCEndgame analytics is disabled in the pack override (`sendAnalytics=false`).

## Validation state before final run

- CODE REVIEWED: YES
- TESTED: NO — no dedicated automated gameplay test suite
- BUILD VERIFIED: PENDING CURRENT AUDIT CHECKPOINT
- JAR PRODUCED: PENDING CURRENT AUDIT CHECKPOINT
- PACK DEPENDENCY INSPECTION: PENDING CURRENT AUDIT CHECKPOINT
- DEDICATED SERVER SMOKE: PENDING CURRENT AUDIT CHECKPOINT
- CLIENT BOOT SMOKE: PENDING CURRENT AUDIT CHECKPOINT
- LIVE GAMEPLAY TESTED: NO
- WORLDGEN/VISUAL PLAYTESTED: NO
- BOSS GAMEPLAY TESTED: NO
- BETTER COMBAT LIVE INTERACTION TESTED: NO
- MULTIPLAYER TESTED: NO
