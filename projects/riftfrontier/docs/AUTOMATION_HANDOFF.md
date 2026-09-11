# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Run-start remote `main`: `58d3f133d7f549a6baad59d33c604bfef859da2d`.
- Previous player-generation run `34544929333`, HEAD `3e987dbc8eab31763deb74f0e7735fdbe8ab186c`: final `SUCCESS`.
- No newly approved Region 01 final boss source/profile/assets, production player ItemStack provisioning/control mapping, shape-specific hit geometry, damage/range/resource policy, or final presentation input was found. None was invented.

## Completed in this batch

M3 player-combat exact-instance lifecycle retirement:

- Code/test HEAD before handoff: `27fe822de323c56a9875f56ab6f68cbb2b8899ee`.
- Found a real authority divergence: combat sessions were exact `LivingEntity` instance-bound, but lifecycle cleanup accepted only UUID. A delayed logout/clone/dimension/remove event from an old same-UUID player instance could therefore cancel a fresh successor instance's valid session.
- `MinecraftPlayerWeaponCombatAdapter` now exposes exact-instance `clearActor(LivingEntity)` and invalidates only when the stored owner is that exact object.
- UUID-wide cleanup remains only as an explicit identity fence for reconnect/content/server boundaries.
- Logout, dimension handoff and clone retirement now use exact-instance cleanup. Login deliberately retains UUID-wide cleanup before new authority is established.
- `EntityLeaveLevelEvent` now retires player combat state through the exact-instance path.
- `ServerStoppedEvent` clears the process-local player combat runtime so no capability survives authoritative server lifetime.
- Added a pure-Java structural lifecycle contract test locking exact-instance cleanup, reconnect fencing and event wiring.
- No balance, controls, ItemStack provisioning, hit geometry, damage/range/resource, model, animation, VFX or sound was invented.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftPlayerWeaponCombatAdapter.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/PlayerWeaponServerRuntime.java`
- `src/main/java/kr/moonseungjun/riftfrontier/Riftfrontier.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/MinecraftPlayerWeaponLifecycleContractTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Code/test `Build Riftfrontier` run `34549242359`, HEAD `27fe822de323c56a9875f56ab6f68cbb2b8899ee`: still `IN PROGRESS` at handoff update.
- Confirmed `SUCCESS`: toolchain, asset-intake tests, JUnit + clean build, required native GameTest gate, dedicated-server smoke.
- Xvfb client smoke was `IN PROGRESS`; executable JAR inspection, report and artifact upload had not yet run. Do not claim those stages successful until their final conclusion is observed.
- Local Gradle: NOT RUN in this automation environment; executable validation is through GitHub Actions.
- Human field play, multiplayer latency/feel, concrete player controls, production boss/player hit geometry/damage/resource policy, final player/boss presentation and real reconnect race timing: NOT TESTED / NOT APPROVED.

## Do not repeat or revert

- M2 persistence/restart/Region 01 technical-proxy lifecycle work is DONE.
- Boss presentation/network semantic capability plumbing is DONE; do not add more without approved real assets.
- Player `weapon_family` / `weapon_module` schema, reference role lock, two-family production graph, server-owned ItemStack loadout component and move-id-only serverbound intent are DONE.
- Player sessions remain published-generation + exact actor-instance + dimension + loadout scoped and combat-eligibility gated.
- Player lifecycle cleanup is now exact-instance for logout/dimension/clone/entity-leave. Never restore UUID-only cleanup for those events; a stale predecessor event must not cancel a same-UUID successor session.
- Login UUID-wide cleanup is intentional as a reconnect identity fence before the new player instance establishes combat authority.
- Process-local player combat state must be cleared at server stop.
- Shared actor/target admission, boss generation retirement, boss owner lifetime/singleton claim, owner-context mutation, attack clock and stale-content fail-closed boundaries are already closed.
- Provisional attack ticks are not final balance. Do not invent blocked geometry/damage/range/resource/control/art values.

## Exact next start point

1. Re-check current remote `main` and final status of code run `34549242359` plus the handoff descendant CI. If either failed, fix the first actual failing gate without weakening authority.
2. Re-check approved Region 01 boss/profile/assets and player ItemStack provisioning/control input. Use existing gates only if legitimate new input exists.
3. If still absent and CI is green, do not repeat player lifecycle/generation, boss generation/owner lifetime/singleton, actor-instance, eligibility, target-admission, dimension, loadout or attack-clock work. Audit the next objectively testable M3 authoritative-state divergence.
4. Prefer the next audit at process/server lifecycle boundaries (including any boss-side process-local owner index that can outlive a server instance) before inventing presentation or balance work.
5. Keep production hit geometry, damage/range/resource, timing tuning, final controls/ItemStack provisioning/model/animation/VFX/sound and multiplayer feel blocked on evidence/approval.
