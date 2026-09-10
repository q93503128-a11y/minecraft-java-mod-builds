# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` verified at this run start: `25d09aba21dc0997f97bec15b560a66a461f1b6b`.
- Push preflight observed an unrelated Turnbound descendant at `742c6d3514db41bf4fb96809b2183faf2b992b16`; Riftfrontier paths did not conflict, so this batch was rebuilt on that latest tree before the non-force main update.
- A later unrelated Turnbound descendant `7a9c2512608b8945bdb243808f32b053bd1cafcd` has implementation HEAD `3eddf08cfa58410a63a62b0e47ae369bb5b48ca2` as its direct parent, preserving the Riftfrontier batch in main ancestry.
- Production Region 01 was rechecked from the current production pack/runtime publication: no approved final boss material treatment, production boss presentation/profile or boss asset manifest is published; runtime reports `bossPresentations=0` and `bossAssetManifest=false`.

## Completed in this batch

M3 server-owned player equipment + authenticated move-intent authority:

- Added persistent typed ItemStack data component `riftfrontier:player_weapon_loadout` carrying stable family/module content IDs only. It contains no damage, range, hit, timing or target authority.
- Added `PlayerWeaponItemStackLoadoutResolver` for the server-owned main hand. It fail-closes to exactly the locked production families `mobile_pressure` / `reach_commitment` and optional `recovery_pivot`, then reassembles through the current `CombatRuntimeCatalog` so ItemStack metadata cannot become a second weapon registry.
- Added `PlayerWeaponMoveIntentPayload`. Its serverbound wire contract carries only an authored `moveId`; sender identity comes from the authenticated NeoForge payload context and server game time/loadout are resolved on the server.
- Added generation-aware `PlayerWeaponServerRuntime` that routes accepted intent into the existing `MinecraftPlayerWeaponCombatAdapter`, advances active per-player sessions from server `PlayerTickEvent.Post`, and clears UUID state on logout/clone lifecycle boundaries.
- Expanded native GameTest coverage. `player_weapon_input_authority` verifies wrong-family/spoofed intent rejection, valid current-family intent, server-observed swap invalidation, fresh family replacement, unequip rejection and cleanup. `player_weapon_authority` now also proves two actors own isolated sessions.
- Added required GameTest instances for both player-weapon tests, raising the executed required suite from six to eight tests in the implementation CI.
- No production damage, range, resource cost, shape-specific hit volume, finished item/model, client input UX, animation, VFX or sound was invented.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/PlayerWeaponLoadoutComponent.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/RiftfrontierCombatDataComponents.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/PlayerWeaponItemStackLoadoutResolver.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/PlayerWeaponServerRuntime.java`
- `src/main/java/kr/moonseungjun/riftfrontier/network/PlayerWeaponMoveIntentPayload.java`
- `src/main/java/kr/moonseungjun/riftfrontier/network/RiftfrontierNetworking.java`
- `src/main/java/kr/moonseungjun/riftfrontier/Riftfrontier.java`
- `src/main/java/kr/moonseungjun/riftfrontier/gametest/PlayerWeaponGameTests.java`
- `src/main/resources/data/riftfrontier/test_instance/player_weapon_authority.json`
- `src/main/resources/data/riftfrontier/test_instance/player_weapon_input_authority.json`
- `docs/M3_PLAYER_COMBAT_KERNEL.md`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Implementation/test HEAD: `3eddf08cfa58410a63a62b0e47ae369bb5b48ca2`.
- `Build Riftfrontier` run `34464663645`: FULL SUCCESS. Java 25/toolchain, 32 asset-intake tool tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb client smoke, executable JAR inspection, build report and artifact/log uploads all passed.
- Native GameTest log: `8 tests are now running` and `All 8 required tests passed`; mock server-player login occurred in the new input-authority test.
- Executable JAR contains the new loadout component/resolver/server runtime/payload and both player-weapon required test instances. JAR SHA-256: `b23b057e49708a9ee3be6c5e5f936f0ecf44dd6587b3f70a0698ad7baf1f4b4c`.
- Local Gradle execution: NOT RUN successfully in this session; GitHub Actions is the authoritative executable validation recorded above.
- Real human client key/button/action emission of `PlayerWeaponMoveIntentPayload`: NOT AUTHORED / NOT TESTED. The serverbound payload registration/handler exists, but the native test enters the same authoritative server runtime directly rather than simulating a real network client.
- Production weapon ItemStack provisioning/model identity: NOT APPROVED / NOT AUTHORED / NOT TESTED. The typed component is proven on a technical vanilla test stack only.
- Production shape-specific hit volume and damage/range/resource policy: NOT APPROVED / NOT AUTHORED / NOT TESTED. `PlayerWeaponServerRuntime` deliberately exposes no production hit candidates until that separate evidence gate exists.
- Human multiplayer/latency/field-play of the two production families: NOT TESTED; provisional timings must not be promoted to final balance without it.
- Final weapon and boss art/animation/VFX/sound/material treatment: NOT APPROVED / NOT AUTHORED / NOT TESTED.

## Do not repeat or revert

- Boss source/asset provenance, reviewed animation preparation, semantic-animation/material/geometry publication provenance, UUID-qualified presentation/network ordering/lifecycle/cache/render work are DONE.
- `weapon_family` / `weapon_module` schema, decoder, graph validation, reference dossier, two-role lock and first production player-combat graph are DONE.
- Server-owned ItemStack loadout data-component boundary and serverbound move-id-only authority are DONE. Do not replace them with client-provided loadout, damage, target, phase or timing fields, and do not introduce a second weapon registry.
- `PlayerWeaponRuntimeProfile` + `PlayerWeaponCombatController` remain the reusable server execution capability. `MinecraftPlayerWeaponCombatAdapter` remains the Minecraft-facing authority boundary.
- `AttackPattern` remains the only authoritative `telegraph -> ACTIVE -> recovery` cadence primitive.
- `recovery_pivot` cannot shorten required recovery, open a second hit window, grant generic invulnerability or survive equipment invalidation.
- Do not treat current production tick counts as field-balanced. Do not invent final player/boss art, control UX or combat values without evidence.

## Exact next start point

1. Re-check current remote `main` and recover the newest `Build Riftfrontier` conclusion; if any descendant gate failed, fix its first actual failure before new feature work.
2. Re-check approved Region 01 boss material/data. If legitimate production boss inputs appeared, route them through the already completed boss gates.
3. If boss inputs remain absent, check whether canonical/reference work now approves a concrete production player ItemStack identity/provisioning path or client action/control mapping. Do not invent either merely to make the new server path reachable.
4. If an approved client control mapping exists, bind it to emit only `PlayerWeaponMoveIntentPayload(moveId)` and add executable transport/spoof/lifecycle coverage without moving hit authority client-side.
5. If no approved control/item input exists, leave this completed server authority boundary intact and select the next objective M3 runtime integration that does not require guessed UX, art, hit-volume geometry or balance.
6. Shape-specific hit volumes, damage/range/resource costs, provisional timing tuning and final item/model/animation/VFX/sound remain blocked on field-play/presentation evidence.
