# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` verified at run start: `d2c35cd9c1f14582459ff1e3dc7abd2644132d78`.
- Prior handoff CI run `34443381052` was recovered as FAILED at native GameTest, not left as pending: `riftfrontiertest.region_01_encounter_runtime` assumed the level entity index already exposed a freshly spawned proxy on tick 0.
- Fully validated implementation/test HEAD completed in this batch: `69f4d2c678d0745a4c4bfa9905213cff1494eb33` (`Build Riftfrontier` run `34448910763`, FULL SUCCESS).
- Documentation HEAD immediately before this handoff commit: `e4e8c971ce289c681d5ee4392ee04191dbc70770`.
- Production Region 01 still has no newly discovered approved final boss material treatment or legitimate production boss content in the recovered production content pack.

## Completed in this batch

M3 player weapon server execution capability + GameTest race repair:

- Added `PlayerWeaponRuntimeProfile` as an immutable fail-closed assembly of one published `WeaponFamily`, its exact resolved `AttackPattern` move set, and an optional compatible `WeaponModule`.
- Runtime assembly rechecks family/module compatibility and socket ownership even after graph validation; resolver ID substitution and missing family moves fail closed.
- Extended `CombatRuntimeCatalog` with typed weapon-family/module resolution and `playerWeaponProfile(...)` / `playerWeaponController(...)` construction.
- Added `PlayerWeaponCombatController`; only family-authored moves can start, overlapping moves are rejected, and timing remains owned exclusively by `AttackStateMachine` / `AttackExecution` / `AttackTimeline`.
- `recovery_pivot` is exposed only as a server authorization while that same authoritative clock is in `RECOVERY`; it cannot open during TELEGRAPH/ACTIVE/COMPLETE and does not shorten recovery or create a second hit clock.
- Added JUnit coverage for exact move-set assembly, incompatible modules, missing move resolution, illegal-family moves, overlap rejection, and recovery-only pivot authorization.
- Repaired the existing Region 01 GameTest without weakening lure/cleanup assertions: only the asynchronous Minecraft level-entity indexing boundary now waits via `succeedWhen`; the authoritative tracker assertions remain immediate and strict.
- Updated `docs/M3_PLAYER_COMBAT_KERNEL.md` to mark the reusable API-free server execution capability complete and keep production data/Minecraft adapter work separate.
- No production player attack timings, damage/range/resource values, hitboxes, item IDs, art, animation, VFX, or sound were invented.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/PlayerWeaponRuntimeProfile.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/PlayerWeaponCombatController.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/CombatRuntimeCatalog.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/PlayerWeaponCombatControllerTest.java`
- `src/main/java/kr/moonseungjun/riftfrontier/gametest/RiftfrontierGameTests.java`
- `docs/M3_PLAYER_COMBAT_KERNEL.md`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Recovered prior run `34443381052`: FAILED at native GameTest with `Encounter must expose at least one live proxy for lure-boundary regression coverage on tick 0`; Java/toolchain, asset intake, JUnit and clean build had passed before that failure.
- `Build Riftfrontier` run `34448910763` for HEAD `69f4d2c678d0745a4c4bfa9905213cff1494eb33`: FULL SUCCESS.
- PASS: Java 25/toolchain, asset-intake tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb graphical client smoke, executable-JAR inspection, build report, deliverable upload, and logs/report upload.
- Local clone/build: NOT RUN successfully in this automation environment; GitHub Actions is the validation source.
- Production `attack_pattern` / exactly two `weapon_family` / one `weapon_module`: NOT AUTHORED / NOT TESTED.
- Minecraft player equipment/input adapter, authoritative player hit-volume attachment, save/network integration, and production native GameTest for the player adapter: NOT AUTHORED / NOT TESTED.
- Player field balance, damage/range/resource costs, final art, animation/VFX/sound, and human field-play: NOT AUTHORED / NOT TESTED.
- Production final boss texture/material/`RenderType`: NOT APPROVED / NOT AUTHORED.
- Production Region 01 final boss `attack_pattern`/`boss_profile` and actual boss encounter attachment: NOT AUTHORED / NOT TESTED.

## Do not repeat or revert

- Boss source/asset provenance, reviewed animation preparation, semantic-animation/material/geometry publication provenance, UUID-qualified presentation/network ordering/lifecycle/cache/render work are DONE. Do not recreate or weaken those gates.
- `weapon_family` / `weapon_module` schema, decoder, graph validation, reference dossier, and the first-slice role lock are DONE.
- The reusable `PlayerWeaponRuntimeProfile` + `PlayerWeaponCombatController` server execution capability is DONE. Do not add a parallel player attack clock or bypass it with weapon-specific subclasses.
- `AttackPattern` remains the only authoritative `telegraph -> ACTIVE -> recovery` cadence primitive for player and boss authored moves.
- `recovery_pivot` may only authorize an eligible recovery transition; do not turn it into timing bypass, generic invulnerability, ACTIVE extension, or an independent hit window.
- The Region 01 GameTest must continue to validate luring outside the technical cell and terminal cleanup; do not restore the tick-0 entity-index assumption or delete the regression.
- Never promote fixture IDs/timings, copy third-party moves/assets, infer balance from animation clips, or invent final player/boss art or field-balance values without evidence.

## Exact next start point

1. Re-check current remote `main`, canon, roadmap, `M3_PLAYER_COMBAT_KERNEL.md`, `M3_PLAYER_WEAPON_REFERENCE_DOSSIER.md`, and this handoff; confirm any CI triggered by this documentation-only tail commit before calling it successful.
2. Re-check whether approved final Region 01 boss material or legitimate production boss data appeared. If so, use the existing completed boss gates rather than replacing them.
3. If boss inputs remain absent, author the smallest production `attack_pattern` set that expresses exactly the locked `mobile_pressure` and `reach_commitment` roles. Keep all timing windows explicitly data-driven and provisional; do not claim field balance without play evidence.
4. Author exactly two production `weapon_family` definitions sharing the `technique` socket and exactly one production `weapon_module` for `recovery_pivot`; run the existing schema/graph validation and add tests only for real ambiguities discovered.
5. Assemble those production definitions through `CombatRuntimeCatalog.playerWeaponController(...)`, then implement the Minecraft server-owned player equipment/input + authoritative hit-volume adapter. Do not build a second execution path.
6. Add native GameTest coverage for the actual player adapter before any damage/range/cadence tuning or presentation approval.
