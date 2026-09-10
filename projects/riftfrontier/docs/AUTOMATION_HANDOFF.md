# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Run-start remote `main`: `43734abb5fab058f50865f498c7f054ac74f9122`.
- Previous HEAD workflow `34493261055`: completed `SUCCESS`.
- No approved Region 01 final boss material/profile/asset input was found.
- No approved production weapon ItemStack provisioning identity, concrete client control mapping, shape-specific hit geometry, damage/range/resource policy, or final presentation input was found; none was invented.

## Completed in this batch

M3 shared Minecraft combat actor/target authority boundary:

- Runtime commit: `78eb2849fbcfa7464f70b9bc3524f112f05241ff`.
- Added `MinecraftCombatAuthority` as the shared Minecraft-side admission policy.
- Generic `MinecraftAttackAdapter` now cancels before clock progression/hit resolution when the attacker is not an alive, non-removed, non-spectator entity in the supplied authoritative `ServerLevel`.
- Generic AABB and custom hit-resolver output now centrally reject null/self, foreign-level, dead, removed, and spectator targets before candidate/damage authority.
- `MinecraftPlayerWeaponCombatAdapter` now uses the same server-actor and target admission boundary; its existing actor-instance/loadout/dimension/session rules remain intact.
- `MinecraftBossCombatAdapter` now closes lifecycle + damage + presentation together before progression when the boss actor is no longer eligible for the supplied authoritative level.
- Added required native `combat_authority_boundary` GameTest covering resolver-supplied spectator/removed targets, dead generic attacker cancellation, dead boss lifecycle/presentation cancellation, and player candidate filtering.
- No production geometry, damage/range values, cadence tuning, control mapping, ItemStack provisioning, model, animation, VFX, sound, or boss identity was added.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftCombatAuthority.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftAttackAdapter.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftPlayerWeaponCombatAdapter.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftBossCombatAdapter.java`
- `src/main/java/kr/moonseungjun/riftfrontier/gametest/CombatAuthorityGameTests.java`
- `src/main/java/kr/moonseungjun/riftfrontier/Riftfrontier.java`
- `src/main/resources/data/riftfrontier/test_instance/combat_authority_boundary.json`

## Verification

- Push preflight verified remote `main` still at `43734abb5fab058f50865f498c7f054ac74f9122`; runtime commit was a normal non-force descendant.
- `Build Riftfrontier` run `34500143447`, HEAD `78eb2849fbcfa7464f70b9bc3524f112f05241ff`: completed `SUCCESS`.
- Verified successful steps: Java/Gradle setup + toolchain, asset-intake tests, JUnit + clean build, required native GameTest gate, dedicated-server smoke, Xvfb client smoke, executable JAR inspection, build report, deliverable/log uploads.
- Local Gradle: NOT RUN; executable validation source was GitHub Actions.
- Human field play, multiplayer latency/feel, concrete player controls, production hit geometry/damage/resource policy, final player/boss presentation: NOT TESTED / NOT APPROVED.

## Do not repeat or revert

- M2 persistence/restart/Region 01 technical-proxy lifecycle work is DONE.
- Boss presentation/network semantic capability plumbing is DONE; do not add more presentation plumbing without approved real assets.
- Player `weapon_family` / `weapon_module` schema, reference role lock, two-family production graph, server-owned ItemStack loadout component, move-id-only serverbound intent, generation-aware runtime are DONE.
- Player sessions are exact-actor-instance + dimension + loadout scoped and alive/non-removed/non-spectator gated. Do not restore UUID-only or cross-dimension continuity.
- Shared Minecraft combat actor/target admission now rejects wrong-level/dead/removed/spectator authority before hit resolution. Do not let future shape resolvers bypass it.
- `AttackStateMachine` remains the single monotonic authoritative `telegraph -> ACTIVE -> recovery` clock. Do not add parallel timers or raw gameplay sampling bypasses.
- Provisional attack ticks are not final balance. Do not invent blocked damage/range/resource/geometry/control/art values.

## Exact next start point

1. Re-check current remote `main`, then recover the final CI conclusion for the handoff descendant and any newer Riftfrontier run; repair the first actual failing gate if needed.
2. Re-check approved Region 01 boss source/asset input and approved player ItemStack provisioning/control mapping. Use existing gates only if legitimate new input exists.
3. If still absent, inspect the next objective M3 server-authority/runtime integration gap. Do not repeat actor-instance, combat-eligibility, target-admission, dimension, loadout, or attack-clock monotonicity work.
4. Keep shape-specific production hit volumes, damage/range/resource policy, timing tuning, final ItemStack/model/animation/VFX/sound and multiplayer feel blocked on evidence/approval.
