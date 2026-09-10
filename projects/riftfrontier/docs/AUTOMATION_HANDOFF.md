# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` verified at run start: `ca1063d45d9cd6971ffb6f22a82455142193afd3`.
- Previous handoff build `34453984278` for `ca1063d45d9cd6971ffb6f22a82455142193afd3` was recovered as FULL SUCCESS: toolchain, asset intake, JUnit/clean build, native GameTest, dedicated server, Xvfb client, executable JAR, report and artifact upload all passed.
- Production Region 01 still has no newly discovered approved final boss material treatment or legitimate production boss content in the checked production sources.

## Completed in this batch

M3 first production player-weapon content graph:

- Added `player_combat_01.json` as production data with exactly three `AttackPattern` moves, exactly two locked-role `WeaponFamily` definitions and exactly one shared `recovery_pivot` `WeaponModule`.
- `mobile_pressure` has an ordinary repositioning entry plus a separate committed finisher; `reach_commitment` has a deliberate line-commitment strike. Both expose the shared `technique` socket.
- Current attack tick counts are explicitly provisional engineering values required by the authoritative `AttackPattern` clock. They are data-driven and are NOT approved field balance/final cadence.
- No damage, reach distance, hitbox dimensions, stamina/resource cost, model, texture, animation, VFX or sound was authored.
- Added `ProductionPlayerWeaponContentTest` to load the packaged production pack through `ContentPackLoader`, require graph validity, lock the exact two-family/three-move/one-module scope, verify semantic contrast and reject fixture IDs.
- Updated `M3_PLAYER_COMBAT_KERNEL.md` so future work starts at real server-owned equipment/input binding rather than re-authoring the completed family graph.

## Changed systems/files

- `src/main/resources/data/riftfrontier/riftfrontier/content/player_combat_01.json`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/ProductionPlayerWeaponContentTest.java`
- `docs/M3_PLAYER_COMBAT_KERNEL.md`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Implementation/test HEAD `96ab7bd94f4a06932a1db9ad6836e3a027dd7474`.
- `Build Riftfrontier` run `34458685173`: FULL SUCCESS. Java/toolchain, asset-intake tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb client smoke, executable JAR inspection, build report and artifact/log uploads all passed.
- Local Gradle execution: NOT RUN successfully in this session; GitHub Actions is the authoritative executable validation recorded above.
- Real `ItemStack`/equipment decoder and serverbound move-input binding: NOT AUTHORED / NOT TESTED.
- Production shape-specific player hit volume, damage/range/resource costs, field tuning and final presentation: NOT AUTHORED / NOT TESTED.
- Human field-play of the two production families: NOT TESTED; provisional timings must not be promoted to final balance without it.
- Production final boss texture/material/`RenderType`, final boss `attack_pattern`/`boss_profile`, and encounter attachment: NOT APPROVED / NOT AUTHORED / NOT TESTED.

## Do not repeat or revert

- Boss source/asset provenance, reviewed animation preparation, semantic-animation/material/geometry publication provenance, UUID-qualified presentation/network ordering/lifecycle/cache/render work are DONE.
- `weapon_family` / `weapon_module` schema, decoder, graph validation, reference dossier and first-slice role lock are DONE.
- First production player-combat graph is DONE: exactly `mobile_pressure`, `reach_commitment`, three authored moves and one shared `recovery_pivot` module. Do not add a third family or replace this with weapon-specific Java execution subclasses.
- `PlayerWeaponRuntimeProfile` + `PlayerWeaponCombatController` remain the one reusable server execution capability. `MinecraftPlayerWeaponCombatAdapter` remains the Minecraft-facing authority boundary.
- `AttackPattern` remains the only authoritative `telegraph -> ACTIVE -> recovery` cadence primitive.
- `recovery_pivot` cannot shorten required recovery, open a second hit window, grant generic invulnerability or survive equipment invalidation.
- Do not treat current production tick counts as field-balanced. Do not invent final player/boss art or combat values without evidence.

## Exact next start point

1. Re-check current remote `main` and recover the newest `Build Riftfrontier` conclusion; if any descendant gate failed, fix its first actual failure before new feature work.
2. Re-check approved Region 01 boss material/data. If legitimate production boss inputs appeared, route them through the already completed boss gates.
3. If boss inputs remain absent, bind real server-owned `ItemStack`/equipment state to the existing `MinecraftPlayerWeaponCombatAdapter.LoadoutResolver`. Prefer a typed/data-component boundary that resolves only the two authored production families and optional `recovery_pivot`; do not create a second weapon registry or final art merely to identify the stack.
4. Add a serverbound move-intent payload that accepts only authored move IDs and resolves the sending `ServerPlayer` plus current loadout on the server before calling `beginMove(...)`. Client input must never send damage/hit confirmation/timing authority.
5. Add executable coverage for spoofed/wrong-family move IDs, equipment swap/unequip invalidation, actor isolation and lifecycle cleanup.
6. Leave shape-specific hit volumes, damage/range/resource costs, provisional timing tuning and final art/animation/VFX/sound for field-play/presentation evidence.
