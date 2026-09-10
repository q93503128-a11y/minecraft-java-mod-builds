# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` verified at run start: `eb9f5a6ee8027e991bb5fe32bf09d736795619db`.
- Production Region 01 still has no newly discovered approved final boss material treatment or legitimate production boss content in the checked production sources.
- Prior completed player runtime capability remains `PlayerWeaponRuntimeProfile` + `PlayerWeaponCombatController`; do not replace it with a second weapon clock.

## Completed in this batch

M3 Minecraft-facing player weapon server-authority adapter:

- Added `MinecraftPlayerWeaponCombatAdapter` as the bridge from server-resolved equipment loadout to `CombatRuntimeCatalog.playerWeaponController(...)` and the existing authoritative `AttackStateMachine`.
- Input is accepted only as a move intent after resolving the actor's current server loadout; family/module validity remains fail-closed through the published catalog/runtime profile.
- Per-actor state is UUID-keyed. Equipment/module swap or unequip observed by the server cancels and discards the old execution instead of letting stale family/module semantics survive.
- Hit-volume candidates are exposed only while the same authoritative attack clock is `ACTIVE`; candidates are deduplicated per execution. No guessed damage, range, hitbox size, cooldown, art, VFX, animation, or sound was introduced.
- `recovery_pivot` delegates to the existing controller and is therefore available only during authoritative `RECOVERY`; loadout invalidation closes it immediately.
- Added native `player_weapon_authority` GameTest covering TELEGRAPH/ACTIVE/RECOVERY authority, ACTIVE-only target resolution, target deduplication, recovery-only pivot, equipment swap invalidation, fresh-family replacement, and lifecycle cleanup.
- Registered the new native GameTest and updated `M3_PLAYER_COMBAT_KERNEL.md` to mark the reusable Minecraft authority adapter boundary complete while keeping concrete item/input/damage production bindings pending.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftPlayerWeaponCombatAdapter.java`
- `src/main/java/kr/moonseungjun/riftfrontier/gametest/PlayerWeaponGameTests.java`
- `src/main/java/kr/moonseungjun/riftfrontier/Riftfrontier.java`
- `docs/M3_PLAYER_COMBAT_KERNEL.md`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- `Build Riftfrontier` run `34453799810` for implementation/test HEAD `e9bf6665fdcf805678315744a03b306556d9e097`: IN PROGRESS when superseded by the documentation follow-up; Java setup/toolchain and asset-intake steps passed, tests/clean build had started. Do not report this run as success unless later recovery confirms its conclusion.
- Current follow-up `Build Riftfrontier` run `34453918498` for documentation HEAD `10863d1716312ab4ee102750f29212275f44678a`: QUEUED at handoff update time. Treat all remaining gates as NOT YET VERIFIED until a later recovery checks the actual conclusion.
- Local clone/build: NOT RUN successfully in this automation environment; GitHub Actions is the validation source.
- Concrete production item-stack/equipment decoder and client-input packet binding: NOT AUTHORED / NOT TESTED.
- Production `attack_pattern` / exactly two `weapon_family` / one `weapon_module`: NOT AUTHORED / NOT TESTED.
- Production shape-specific player hit volume, damage/range/resource costs, final cadence tuning, art, animation/VFX/sound, and human field-play: NOT AUTHORED / NOT TESTED.
- Production final boss texture/material/`RenderType`, final boss `attack_pattern`/`boss_profile`, and encounter attachment: NOT APPROVED / NOT AUTHORED / NOT TESTED.

## Do not repeat or revert

- Boss source/asset provenance, reviewed animation preparation, semantic-animation/material/geometry publication provenance, UUID-qualified presentation/network ordering/lifecycle/cache/render work are DONE.
- `weapon_family` / `weapon_module` schema, decoder, graph validation, reference dossier, and first-slice role lock are DONE.
- `PlayerWeaponRuntimeProfile` + `PlayerWeaponCombatController` are the one reusable server execution capability. `MinecraftPlayerWeaponCombatAdapter` is now the Minecraft-facing authority boundary. Do not add a parallel player attack clock or weapon-specific execution subclasses.
- `AttackPattern` remains the only authoritative `telegraph -> ACTIVE -> recovery` cadence primitive.
- `recovery_pivot` cannot shorten recovery, open a second hit window, grant generic invulnerability, or survive an equipment invalidation.
- Never promote fixture IDs/timings, infer balance from animation clips, or invent final player/boss art or field-balance values without evidence.

## Exact next start point

1. Re-check current remote `main` and recover the final conclusions of `Build Riftfrontier` runs `34453799810` / `34453918498` or their newest descendant. If any gate failed, fix the first actual failure before new feature work.
2. Re-check approved Region 01 boss material/data. If legitimate production boss inputs appeared, route them through the already completed boss gates.
3. If boss inputs remain absent, author the smallest production `attack_pattern` set expressing only the locked `mobile_pressure` and `reach_commitment` roles, keeping timings explicitly provisional/data-driven rather than claiming field balance.
4. Author exactly two production `weapon_family` definitions sharing `technique` and exactly one `recovery_pivot` `weapon_module`, then run schema/graph checks.
5. Bind real server-owned item/equipment state to `MinecraftPlayerWeaponCombatAdapter.LoadoutResolver` and real move input intents to `beginMove(...)`; reuse the adapter rather than creating another execution path.
6. Do not add production damage/range/cadence tuning or final presentation until field-play/presentation evidence exists.
