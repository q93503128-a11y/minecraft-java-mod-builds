# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Remote `main` verified at run start: `278250b75c36d463e3f8653a13445e9ca4c1df22`.
- That baseline already contained the completed M3 server-owned ItemStack loadout boundary, move-id-only serverbound intent, generation-aware player weapon runtime, two production weapon-family graph, and eight required native GameTests.
- Latest baseline Riftfrontier CI was green before new work.
- Region 01 was rechecked: no approved final boss material treatment, legitimate production boss profile/presentation, or boss asset manifest appeared.
- No approved concrete production player ItemStack identity/provisioning path or client control mapping appeared; do not invent either.

## Completed in this batch

M3 player-weapon world/lifecycle session integrity:

- Implementation commit: `093c51c6edab42ab5d1735766859a879536c2954`.
- `MinecraftPlayerWeaponCombatAdapter` now binds each authoritative execution session to the server dimension in which it was created.
- `tick(...)` fail-closes and cancels the session when the supplied server level/dimension no longer matches the actor/session, before advancing the attack clock or resolving any hit candidates.
- `recoveryPivotAuthorized(...)` also rejects and clears a session that crossed a dimension boundary, so module movement authority cannot leak across world handoff.
- Re-establishing the same UUID/loadout in another dimension creates a fresh controller/session rather than reusing the previous attack clock.
- Session invalidation now clears remembered hit-target UUIDs as well as cancelling the controller.
- Added explicit login and `PlayerChangedDimensionEvent` cleanup alongside existing logout/clone cleanup. Login is treated as a fresh process-local combat epoch even if an earlier disconnect path failed to clean up.
- No damage/range/cadence, hit-volume geometry, client key/UI, ItemStack identity, model, animation, VFX, sound, or boss content was invented.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftPlayerWeaponCombatAdapter.java`
- `src/main/java/kr/moonseungjun/riftfrontier/Riftfrontier.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Push preflight rechecked remote `main` immediately before the implementation fast-forward; it was still `278250b75c36d463e3f8653a13445e9ca4c1df22`, so no concurrent changes were overwritten.
- `Build Riftfrontier` run `34469568071` for implementation HEAD `093c51c6edab42ab5d1735766859a879536c2954`:
  - Java/toolchain setup: SUCCESS.
  - asset-intake tool tests: SUCCESS.
  - tests + clean build: SUCCESS.
  - required Riftfrontier native GameTest gate: SUCCESS.
  - dedicated-server smoke: IN PROGRESS when this handoff was written.
  - Xvfb client smoke, executable JAR inspection, report/artifact upload: NOT YET RUN at handoff-write time.
- Local Gradle execution: NOT RUN; GitHub Actions is the executable validation source for this session.
- Real dimension-transfer field play / reconnect under human multiplayer latency: NOT TESTED.
- Production player controls, ItemStack provisioning, hit geometry/damage/resource policy and final player/boss presentation remain NOT APPROVED / NOT AUTHORED / NOT TESTED.

## Do not repeat or revert

- Boss source/asset provenance, reviewed animation preparation, semantic-animation/material/geometry publication provenance, UUID-qualified boss presentation/network ordering/lifecycle/cache/render work are DONE.
- `weapon_family` / `weapon_module` schema, decoder, graph validation, reference dossier, two-role lock, first production player-combat graph, server-owned ItemStack loadout component and move-id-only serverbound authority are DONE.
- Player weapon authoritative sessions are now dimension-scoped and explicitly reset on login/logout/dimension change/clone. Do not restore cross-dimension attack-clock continuity.
- `PlayerWeaponRuntimeProfile` + `PlayerWeaponCombatController` remain the reusable execution capability; `MinecraftPlayerWeaponCombatAdapter` remains the Minecraft-facing server authority boundary.
- `AttackPattern` remains the sole `telegraph -> ACTIVE -> recovery` cadence primitive. `recovery_pivot` cannot shorten recovery, create another hit window, grant generic invulnerability, or survive equipment/world invalidation.
- Do not promote current production tick values to field-balanced values and do not invent blocked UX/art/combat values.

## Exact next start point

1. Re-check current remote `main` first, then recover final conclusion of implementation CI run `34469568071` (and any newer descendant Riftfrontier run). If failed, repair the first real failing gate before new features.
2. Re-check approved Region 01 boss inputs; if legitimate production material/profile inputs appeared, route them through the completed boss gates.
3. Re-check whether a canonical production player ItemStack provisioning identity or client control mapping has been approved. If yes, wire only `PlayerWeaponMoveIntentPayload(moveId)` from that approved control surface and add executable transport/spoof/lifecycle coverage.
4. If those inputs are still absent, do not invent them. Move to the next objective M3 runtime integration that can be implemented without guessed UX, art, shape-specific hit geometry, damage/range/resource values, or balance tuning.
5. Shape-specific hit volumes, damage/range/resource policy, timing tuning, final item/model/animation/VFX/sound, and real multiplayer feel remain blocked on evidence/approval.
