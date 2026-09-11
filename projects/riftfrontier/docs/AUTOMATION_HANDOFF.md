# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Run-start remote `main`: `00da5320a136cba804ba14b21eefd90f48b0775a`.
- Previous boss server-lifetime code run `34553110431`, HEAD `ea55e9d795f1a0f2a2cf6d359d1351972b690ef3`: final `SUCCESS`.
- No newly approved Region 01 final boss source/profile/assets, production player ItemStack provisioning/control mapping, shape-specific hit geometry, damage/range/resource policy, or final presentation input was found. None was invented.

## Completed in this batch

M3 boss/player cross-capability role authority separation:

- Code/test HEAD before handoff: `4b4c7c7e12acea0a60dfcf339ec107f26cf1c4c4`.
- Found a real authority divergence: boss Minecraft admission used the generic live server `LivingEntity` predicate, so an authenticated `ServerPlayer` could also satisfy boss-owner admission even though production player weapon authority is separately owned by `ServerPlayer`.
- Added `MinecraftCombatAuthority.isEligibleBossActor(ServerLevel, LivingEntity)` as a role boundary. It preserves all generic server-actor eligibility checks but explicitly rejects `ServerPlayer`.
- Boss Minecraft-facing begin, boss tick, and validated owner binding now all use the boss-specific role gate.
- The change does not lock a concrete production boss entity class; any eligible non-player `LivingEntity` can still back the validated boss capability until an approved Region 01 boss entity/profile exists.
- Existing generation retirement, exact entity/dimension ownership, singleton-per-live-owner, owner-context mutation, entity-leave invalidation, server-stop invalidation, attack clock, target admission, player lifecycle and player loadout authority remain unchanged.
- `BossRuntimeLifecycleContractTest` now locks the dedicated boss-role gate and prevents fallback to the generic server-actor predicate on boss paths.
- No balance, ItemStack provisioning, controls, hit geometry, damage/range/resource, model, animation, VFX or sound was invented.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftCombatAuthority.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftBossCombatAdapter.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/BossRuntimeLifecycleContractTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- `Build Riftfrontier` run `34557859011`, code/test HEAD `4b4c7c7e12acea0a60dfcf339ec107f26cf1c4c4`: final `SUCCESS`.
- Successful workflow gates: Java/Gradle setup and toolchain verification; asset intake tool tests; JUnit + clean build; required native GameTest gate; dedicated-server smoke; Xvfb client smoke; executable JAR inspection; build report; deliverable upload; logs/report upload.
- Git diff from run-start baseline contains only the intended three code/test files before this handoff: boss adapter 4-line admission substitution/comment adjustment, combat authority role predicate, and lifecycle source-contract expansion.
- Local Gradle: NOT RUN in this automation environment; executable validation was performed by GitHub Actions.
- Human field play, multiplayer latency/feel, concrete player controls, production boss/player hit geometry/damage/resource policy and final player/boss presentation: NOT TESTED / NOT APPROVED.

## Do not repeat or revert

- M2 persistence/restart/Region 01 technical-proxy lifecycle and owner attribution work is DONE.
- Boss presentation/network semantic capability plumbing is DONE; do not expand presentation without legitimate approved input.
- Player `weapon_family` / `weapon_module` schema, two-family production graph, server-owned ItemStack loadout component and move-id-only serverbound intent are DONE.
- Player sessions remain published-generation + exact actor-instance + dimension + loadout scoped and combat-eligibility gated; player process-local combat state is cleared at server stop.
- Boss validated runtimes remain generation-bound, exact entity-instance + dimension lifetime-bound, singleton per live owner, owner-context mutation gated, entity-leave/server-stop retired, and now explicitly reject `ServerPlayer` ownership.
- Do not restore the generic `isEligibleServerActor(level, boss)` gate on boss begin/tick/owner-binding paths.
- Shared target admission and attack-clock boundaries are already closed.
- Provisional attack ticks are not final balance. Do not invent blocked geometry/damage/range/resource/control/art values.

## Exact next start point

1. Re-check current remote `main` and the final status of code run `34557859011` plus any handoff descendant CI. If a descendant failed, fix the first actual failing gate without weakening authority.
2. Re-check approved Region 01 boss/profile/assets and player ItemStack provisioning/control input. Use existing production gates only if legitimate new input exists.
3. If still absent and CI is green, do not repeat generation, lifecycle, singleton, exact actor/dimension, server-stop, boss/player role separation, loadout, target-admission or attack-clock work.
4. Audit the production reachability of the player-combat capability boundary next: verify that production move-intent/session creation cannot bypass `PlayerWeaponServerRuntime`'s authenticated `ServerPlayer` bridge and establish player-weapon authority for a non-player actor. Distinguish fixture/test-only direct adapter construction from production reachability. If a real production path exists, close it with an explicit typed role fence and executable regression coverage; if not, record the evidence and move to the next objective M3 server-authority divergence rather than adding redundant guards.
5. Keep production hit geometry, damage/range/resource, timing tuning, final controls/ItemStack provisioning/model/animation/VFX/sound and multiplayer feel blocked on evidence/approval.
