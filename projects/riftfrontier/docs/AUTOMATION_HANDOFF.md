# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Remote `main` was independently verified at run start as `d1bcfec8e08bab97ac67a9277a830d115b22465c` and again immediately before documentation write as `4f605f6069dfe8cde4741293d3c97808f88b1978`.
- The preceding attack-observation CI (`Build Riftfrontier` run `34480792673`, HEAD `0486a7b3676717e433eaa80517e740bad8d9caf0`) was recovered as completed SUCCESS.
- Baseline already contained the two production weapon-family graph, server-owned ItemStack loadout boundary, move-id-only serverbound intent, generation-aware runtime, dimension-scoped sessions/lifecycle cleanup, shared monotonic attack clock, and completed boss presentation/network authority work.
- Region 01 still had no approved final boss material/profile/asset input.
- No approved concrete production weapon ItemStack provisioning identity, client control mapping, shape-specific hit geometry, damage/range/resource policy, or final presentation input was available; none was invented.

## Completed in this batch

M3 player-weapon actor eligibility authority:

- Implementation commit `bdd1afe7e6a07979c7b371a0d356f20847c49ac8`; final executable regression descendant `4f605f6069dfe8cde4741293d3c97808f88b1978`.
- Found a real authority gap: a valid server-owned loadout could establish or continue a weapon session even when the actor was dead, removed, or spectator because eligibility was not part of the adapter boundary.
- `MinecraftPlayerWeaponCombatAdapter` now requires `isAlive() && !isRemoved() && !isSpectator()` before beginning a move.
- An actor becoming ineligible while a session exists invalidates/cancels that session before attack-clock advancement or hit-candidate resolution.
- `recoveryPivotAuthorized(...)` applies the same eligibility gate and cannot preserve module authority for an ineligible actor.
- Rejected ineligible input clears any stale session and creates no replacement session.
- Native `player_weapon_authority` GameTest now covers death invalidation, rejected fresh input while dead, spectator rejection, and isolation of another eligible actor.
- An intermediate spectator test using a mock `ServerPlayer` caused `Build Riftfrontier` run `34486758020` to fail at the GameTest gate after clean build had succeeded. The test was repaired without weakening production authority by exercising the adapter with a technical spectator actor; final descendant GameTest passes.
- No cadence, damage, range, hit geometry, ItemStack/control UX, model, animation, VFX, sound, or boss content changed.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftPlayerWeaponCombatAdapter.java`
- `src/main/java/kr/moonseungjun/riftfrontier/gametest/PlayerWeaponGameTests.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Push preflight found remote `main` unchanged at `4f605f6069dfe8cde4741293d3c97808f88b1978`; writes remained normal non-force descendants on `main`.
- Prior run `34480792673` was recovered as FULL SUCCESS.
- Failed intermediate run `34486758020`, HEAD `7c756f867ccdb9e323967cb4205cc2a982df18f8`: setup/toolchain, asset-intake, JUnit + clean build SUCCESS; GameTest FAILED; downstream server/client/JAR gates skipped. The brittle mock-spectator test was replaced rather than production behavior being weakened.
- Repaired run `34487087927`, HEAD `4f605f6069dfe8cde4741293d3c97808f88b1978`: setup/toolchain, asset-intake, JUnit + clean build, and required native Riftfrontier GameTest gate verified SUCCESS. Dedicated-server smoke was IN PROGRESS when this handoff was written.
- Xvfb client smoke, executable JAR inspection, build report/deliverables for run `34487087927`: NOT YET VERIFIED at handoff-write time.
- Local Gradle execution: NOT RUN; GitHub Actions is the executable validation source for this session.
- Human field play, multiplayer latency, concrete player control transport, production hit geometry/damage/resource policy, and final player/boss presentation remain NOT TESTED / NOT APPROVED as applicable.

## Do not repeat or revert

- Boss source/asset provenance, reviewed animation preparation, semantic-animation/material/geometry publication provenance, UUID-qualified boss presentation/network ordering/lifecycle/cache/render work are DONE.
- `weapon_family` / `weapon_module` schema, decoder, graph validation, reference dossier, two-role lock, first production player-combat graph, server-owned ItemStack loadout component and move-id-only serverbound authority are DONE.
- Player weapon authoritative sessions are dimension-scoped and reset on login/logout/dimension change/clone. Do not restore cross-dimension attack-clock continuity.
- Player weapon authority additionally requires an alive, non-removed, non-spectator actor at admission and while progressing/authorizing recovery. Do not rely only on lifecycle events or allow an ineligible actor to retain a session.
- `AttackStateMachine` remains the single shared authoritative cadence state machine; state progression and read-only phase authorization share its nondecreasing server-tick watermark. Do not restore raw gameplay `AttackExecution.sample(...)` bypasses or add a parallel timer.
- `AttackPattern` remains the sole authored `telegraph -> ACTIVE -> recovery` cadence primitive. `recovery_pivot` cannot shorten recovery, create another hit window, grant generic invulnerability, or survive equipment/world/actor invalidation.
- Do not promote provisional production tick values to field-balanced values and do not invent blocked UX/art/combat values.

## Exact next start point

1. Re-check current remote `main` first, then recover the final conclusion of `Build Riftfrontier` run `34487087927` and any newer descendant Riftfrontier run. If any required gate failed, repair the first real failing cause before new features.
2. Re-check approved Region 01 boss inputs and approved player ItemStack/control inputs. Route them through existing gates only if legitimate new source material exists.
3. If those remain absent, inspect the next objective M3 server-authority/runtime gap that connects existing production systems. Do not repeat death/spectator eligibility, dimension lifecycle, loadout authority, or attack-clock monotonicity work.
4. Shape-specific hit volumes, damage/range/resource policy, provisional timing tuning, final item/model/animation/VFX/sound, and human multiplayer feel stay blocked on evidence/approval.
