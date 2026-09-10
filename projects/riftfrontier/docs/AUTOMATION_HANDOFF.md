# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Remote `main` was independently verified at run start as `b0de5c55f7d7a0c8df537f66d1663fe6c0aec19c`.
- `Build Riftfrontier` run `34487087927`, HEAD `4f605f6069dfe8cde4741293d3c97808f88b1978`, was recovered as completed `SUCCESS`.
- Baseline already contained the two production weapon-family graph, server-owned ItemStack loadout boundary, move-id-only serverbound intent, generation-aware runtime, dimension-scoped sessions, alive/non-removed/non-spectator eligibility, shared monotonic attack clock, and completed boss presentation/network authority work.
- Region 01 still had no approved final boss material/profile/asset input.
- No approved concrete production weapon ItemStack provisioning identity, client control mapping, shape-specific hit geometry, damage/range/resource policy, or final presentation input was available; none was invented.

## Completed in this batch

M3 player-weapon actor-instance ownership authority:

- Implementation commit `47b05502f1f0bae65d18698509da01bda6361503`; regression descendant `7aea71ece239db83fa1d216eb2c3264b25616dd9`.
- Found a real lifecycle authority gap: sessions were keyed by UUID and scoped by loadout/dimension but did not remember the exact `LivingEntity` instance that established them. A replacement/respawn entity with the same UUID, same dimension and same loadout could therefore reach the stale session if lifecycle cleanup ordering was missed.
- `MinecraftPlayerWeaponCombatAdapter.Session` now owns the exact actor instance in addition to UUID-keyed lookup.
- `tick(...)` invalidates the stale session before attack-clock advancement or hit-candidate resolution when the current entity instance differs, even if UUID/loadout/dimension all match.
- `recoveryPivotAuthorized(...)` applies the same actor-instance gate, so replacement entities cannot inherit recovery-module authority.
- `synchronizeSession(...)` reuses a session only when exact actor instance + loadout + dimension all match; a fresh eligible replacement can establish a new execution only after the stale session is cancelled.
- Native `player_weapon_authority` GameTest now creates a distinct technical actor with the same UUID and verifies stale execution invalidation and recovery-authority rejection.
- No cadence, damage, range, hit geometry, ItemStack/control UX, model, animation, VFX, sound, or boss content changed.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftPlayerWeaponCombatAdapter.java`
- `src/main/java/kr/moonseungjun/riftfrontier/gametest/PlayerWeaponGameTests.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Previous required workflow run `34487087927` was recovered as FULL SUCCESS before new work.
- Local Gradle execution: NOT RUN; this environment cannot resolve GitHub from the container, so GitHub Actions remains the executable validation source.
- New code/test commits are pushed to `main`; their fresh Riftfrontier CI result is NOT YET VERIFIED at handoff-write time and must not be called successful until the workflow concludes.
- Human field play, multiplayer latency, concrete player control transport, production hit geometry/damage/resource policy, and final player/boss presentation remain NOT TESTED / NOT APPROVED as applicable.

## Do not repeat or revert

- Boss source/asset provenance, reviewed animation preparation, semantic-animation/material/geometry publication provenance, UUID-qualified boss presentation/network ordering/lifecycle/cache/render work are DONE.
- `weapon_family` / `weapon_module` schema, decoder, graph validation, reference dossier, two-role lock, first production player-combat graph, server-owned ItemStack loadout component and move-id-only serverbound authority are DONE.
- Player weapon authoritative sessions are dimension-scoped, combat-eligibility-gated, and now bound to the exact `LivingEntity` instance that established them. Do not restore UUID-only session continuity across respawn/replacement.
- Lifecycle events remain useful cleanup hooks but are not the sole authority boundary for death/spectator/dimension/clone transitions.
- `AttackStateMachine` remains the single shared authoritative cadence state machine; state progression and read-only phase authorization share its nondecreasing server-tick watermark. Do not restore raw gameplay `AttackExecution.sample(...)` bypasses or add a parallel timer.
- `AttackPattern` remains the sole authored `telegraph -> ACTIVE -> recovery` cadence primitive. `recovery_pivot` cannot shorten recovery, create another hit window, grant generic invulnerability, or survive equipment/world/actor invalidation.
- Do not promote provisional production tick values to field-balanced values and do not invent blocked UX/art/combat values.

## Exact next start point

1. Re-check current remote `main` first and recover the final Riftfrontier CI conclusion for `7aea71ece239db83fa1d216eb2c3264b25616dd9` and any newer handoff descendant. If any required gate failed, repair the first real failing cause before new features.
2. Re-check approved Region 01 boss inputs and approved player ItemStack/control inputs. Route them through existing gates only if legitimate new source material exists.
3. If those remain absent, inspect the next objective M3 server-authority/runtime gap connecting existing production systems; do not repeat UUID/actor-instance, death/spectator, dimension, loadout, or attack-clock monotonicity boundaries.
4. Shape-specific hit volumes, damage/range/resource policy, provisional timing tuning, final item/model/animation/VFX/sound, and human multiplayer feel stay blocked on evidence/approval.
