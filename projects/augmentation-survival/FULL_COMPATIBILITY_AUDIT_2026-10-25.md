# Augmentation Survival — Full Compatibility Audit

Date: 2026-10-25
Scope: full 146-augment runtime + pinned 23-artifact external pack + distributable local pack
Status: **FULL TECHNICAL AUDIT COMPLETE / BUILD 146 VERIFIED**

## Runtime roster

- FULL: **146 / 146**
- PARTIAL: **0 / 146**
- NO RUNTIME: **0 / 146**

This count means canonical gameplay runtime coverage. It does not claim human gameplay feel, final VFX/UI/audio polish, statistical balance validation, world-generation visual review, or multiplayer validation.

## Static audit coverage

Reviewed against current `main`, `AGENTS.md`, `docs/BUILD_STANDARD.md`, `docs/QUALITY_STANDARD.md`, the canonical augment roster, compatibility profiles, built JAR contents, and the pinned external pack.

Checked:

- Minecraft 26.2 / Java 25 / Fabric Loader 0.19.5 / Fabric API 0.160.0+26.2 / Gradle 9.5.1 / Loom 1.17.20 pins;
- `fabric.mod.json` dependency contract;
- configured Mixin source presence and side separation;
- optional Better Combat mixins remain soft-linked / `@Pseudo` where required;
- no hard compile-time imports from Chrono Dawn, World Bosses, or MCEndgame;
- canonical augment catalog contains 146 unique definitions;
- data/resource JSON parses cleanly;
- project block/model references are present;
- English/Korean project translation key sets match;
- player-facing project translations contain no TODO/debug/developer-stage wording;
- external pack top-level artifacts resolve without duplicate Fabric mod IDs;
- boss compatibility binding covers Vanilla, Chrono Dawn, World Bosses and dynamic MCEndgame identity;
- server-authoritative damage/drop/progression boundaries remain intact;
- player save codec retains optional/default fields used for backwards-compatible decoding;
- built project JAR contains runtime classes/resources and no Java source or internal design documents.

## Runtime corrections discovered by this audit

### Dexterity final-drop boundary

The earlier implementation re-evaluated `Block.getDrops` after a block break. That could re-roll randomized loot and create a +1 item that was not part of the actual emitted result.

Corrected behavior:

- roll Dexterity once for the actual `destroyBlock` context;
- observe the real `Block.popResource` producer output;
- add exactly one copy of the first eligible stack that actually exists;
- never re-roll the loot table;
- preserve nested/derived harvesting contexts;
- execute before Mythic `Abundance`, so compatible additions participate in the final-drop multiplier normally.

### Wall Slam knockback timing and boss policy

The earlier implementation checked target horizontal velocity inside `AFTER_DAMAGE`, which can execute before vanilla/compatible melee knockback has actually been applied.

Corrected behavior:

- qualifying root melee damage creates a pending collision window;
- external boss `allow_wall_impact` compatibility policy is enforced;
- a dedicated `LivingEntity.knockback` tail hook arms the window only after an actual same-tick knockback impulse exists and clears the configured minimum horizontal-motion threshold;
- wall collision consumes the armed window once;
- derived wall damage stays non-root and cannot recursively create another Wall Slam.

## Final full-pack verification

Final verified commit:

`c04a0273918aaecac57c7fce0a4085ff17c56a73`

GitHub Actions:

- Workflow: `Build Augmentation Survival Pack`
- Run number: **#146**
- Run ID: `40174073009`
- Artifact: `augmentation-survival-pack-smoke-146`
- Artifact ID: `12590903610`
- Artifact ZIP size: `1,892,151` bytes
- Artifact ZIP SHA-256: `f348f4f7833837db8bd67296b13f624935aca5df3f491ff0d8b190dbc885047b`

Results:

- toolchain verification: **PASS**
- clean build: **PASS**
- pinned 23-artifact inspection: **PASS**
- runtime classpath verification: **PASS**
- dedicated server smoke: **PASS**
- client boot smoke: **PASS**
- local `.mrpack` assembly/self-check: **PASS**
- verification artifact upload: **PASS**

Final project JAR SHA-256:

`3539651024a595251a07279cebc76d5ffdf3e36171edaf4cf7528d8c43e0f5d7`

Generated local `.mrpack`:

- file: `augmentation-survival-0.1.0-alpha.1.mrpack`
- bytes: `766907`
- SHA-256: `a7b0cd9454ff761674165b2b542b65133868d04e521fd7838424460ae85710db`
- external manifest files: 24, including Fabric API
- embedded third-party JARs: 0
- embedded project JAR: 1
- MCEndgame override: `sendAnalytics=false`

The `.mrpack` is intended for local/private import. Some pinned dependencies are referenced through Maven/CurseMaven download domains; Modrinth's public upload validator only permits its documented upload-domain allow-list, so this file is not claimed to be ready for public Modrinth project publication without converting those download references. A separate Windows fallback installer can download the same exact pins and verify SHA-512 values when a launcher restricts those local-pack URLs.

## External-pack compatibility observations

The final smoke still logs several non-fatal warnings. They are retained here instead of being hidden:

- Forge Config API Port, Friends&Foes and LambDynamicLights recommend Mod Menu; Friends&Foes also recommends YACL. These are recommendations, not required dependencies.
- Resourceful Lib and Farmer's Delight emit environment-specific client-target Mixin warnings during dedicated-server startup but do not prevent the server from reaching ready state.
- Illager Invasion includes a NeoForge-specific villager-distance data-map entry that Fabric ignores.
- World Bosses 2.0.2 reports unresolved/missing texture references for several Worldbreaker tools and the Varkhul trophy.
- first-run server repositories report missing persistence files before they are created.
- headless CI reports expected audio-device/sound-system warnings on the client.
- Gradle reports deprecation warnings relevant to a future Gradle 10 migration.

No final server/client log matched the workflow fatal signatures. The dedicated server reached ready state and the client reached its initialization marker.

## Distribution policy

Third-party JARs whose redistribution permission is not established are not copied into the deliverable. World Bosses is All Rights Reserved and remains an externally fetched exact dependency rather than an embedded file. The local pack contains the verified project JAR and project-owned configuration overrides only, while external dependencies are fetched from their pinned source repositories.

## Final validation labels

- CODE REVIEWED: **YES**
- TESTED: **NO — Gradle `test` is `NO-SOURCE`; no dedicated automated gameplay suite was executed**
- BUILD VERIFIED: **YES — `c04a0273918aaecac57c7fce0a4085ff17c56a73`**
- JAR PRODUCED: **YES**
- PACK DEPENDENCY INSPECTION: **PASS**
- DEDICATED SERVER SMOKE: **PASS**
- CLIENT BOOT SMOKE: **PASS**
- LOCAL MRPACK STRUCTURE/HASH VERIFY: **PASS**
- LIVE GAMEPLAY TESTED: **NO**
- WORLDGEN/VISUAL PLAYTESTED: **NO**
- BOSS GAMEPLAY TESTED: **NO**
- BETTER COMBAT LIVE INTERACTION TESTED: **NO**
- MULTIPLAYER TESTED: **NO**

Technical build/load compatibility is verified. Human gameplay, balance, visual/audio quality, worldgen feel, boss encounter quality and multiplayer behavior remain separate playtest gates and must not be represented as completed by this audit.
