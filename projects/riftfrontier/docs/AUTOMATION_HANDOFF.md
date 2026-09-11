# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Run-start remote `main`: `5f8b539f3285f5a88dd2db65bbafd514c6d0ecc9`.
- Previous handoff descendant `Build Riftfrontier` run `34558376670`, HEAD `5f8b539f3285f5a88dd2db65bbafd514c6d0ecc9`: final `SUCCESS`.
- No newly approved Region 01 final boss source/profile/assets, production player ItemStack provisioning/control mapping, shape-specific hit geometry, damage/range/resource policy, or final presentation input was found. None was invented.

## Completed in this batch

M3 authenticated player exact-instance authority fence:

- Code/test HEAD before handoff: `2aa47a0a55b9da3ea75e6797a389e1e911aa8c87`.
- Audited production reachability first. Production player-combat session creation is reachable only through `RiftfrontierNetworking` play-to-server handling, whose authenticated payload actor is a `ServerPlayer`, and production ticking is likewise `ServerPlayer`-typed. Direct generic `LivingEntity` adapter use remains fixture/capability code, so no redundant non-player role fence was added there.
- Found the next real authority divergence at the reconnect/clone boundary: a delayed callback retaining an old `ServerPlayer` object with the same UUID could reach the UUID-keyed adapter and replace a successor instance's session unless production revalidated the exact currently registered server-level entity.
- `PlayerWeaponServerRuntime` now verifies `level.getEntity(player.getUUID()) == player` before both move-intent session creation and per-player ticking.
- A rejected stale move intent performs only exact-instance `clearPlayer(player)` cleanup and returns `REJECTED`; it never performs UUID-wide cleanup that could erase a successor session.
- A stale tick callback similarly retires only an exact stale session, returning invalidated/idle without touching a successor instance.
- The check uses the server level's keyed entity lookup, not a broad player/entity scan.
- Existing login UUID-wide reconnect fence remains intentional; logout/dimension/clone/entity-leave cleanup remains exact-instance; generation/loadout/dimension/attack-clock semantics remain unchanged.
- Added `PlayerWeaponAuthenticatedInstanceContractTest` to lock the exact lookup and exact-only stale cleanup on both production entry paths.
- No balance, ItemStack provisioning, controls, hit geometry, damage/range/resource, model, animation, VFX or sound was invented.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/PlayerWeaponServerRuntime.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/PlayerWeaponAuthenticatedInstanceContractTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- `Build Riftfrontier` run `34561082894`, code/test HEAD `2aa47a0a55b9da3ea75e6797a389e1e911aa8c87`: final `SUCCESS`.
- Successful workflow gates: Java/Gradle setup and toolchain verification; asset intake tool tests; JUnit + clean build; required native GameTest gate; dedicated-server smoke; Xvfb client smoke; executable JAR inspection; build report; deliverable upload; logs/report upload.
- Existing native `player_weapon_input_authority` production bridge test remained green, proving its mock `ServerPlayer` is admitted by the new exact server-level lookup while the source contract seals the stale-instance branch.
- A separate unrelated repository workflow `.github/workflows/cd-a15-export-exact3.yml` run `34561079736` failed immediately with no jobs; it is not the Riftfrontier build/validation workflow and was not used as evidence for this batch.
- Local Gradle: NOT RUN in this automation environment; executable validation was performed by GitHub Actions.
- Human field play, multiplayer latency/feel, concrete player controls, production boss/player hit geometry/damage/resource policy and final player/boss presentation: NOT TESTED / NOT APPROVED.

## Do not repeat or revert

- M2 persistence/restart/Region 01 technical-proxy lifecycle and owner attribution work is DONE.
- Boss presentation/network semantic capability plumbing is DONE; do not expand presentation without legitimate approved input.
- Player `weapon_family` / `weapon_module` schema, two-family production graph, server-owned ItemStack loadout component and move-id-only serverbound intent are DONE.
- Production player combat is authenticated `ServerPlayer`-only by reachability; do not add a redundant player-role gate to the generic fixture/capability adapter without a new production path.
- Player sessions remain published-generation + exact actor-instance + exact currently registered server-level instance + dimension + loadout scoped and combat-eligibility gated; player process-local combat state is cleared at server stop.
- Delayed stale player callbacks must never replace or UUID-wide-clear a successor session; preserve `level.getEntity(player.getUUID()) == player` admission and exact-instance stale cleanup.
- Boss validated runtimes remain generation-bound, exact entity-instance + dimension lifetime-bound, singleton per live owner, owner-context mutation gated, entity-leave/server-stop retired, and explicitly reject `ServerPlayer` ownership.
- Shared target admission and attack-clock boundaries are already closed.
- Provisional attack ticks are not final balance. Do not invent blocked geometry/damage/range/resource/control/art values.

## Exact next start point

1. Re-check current remote `main` and the final status of code run `34561082894` plus any handoff descendant CI. If a descendant failed, fix the first actual failing gate without weakening authority.
2. Re-check approved Region 01 boss/profile/assets and player ItemStack provisioning/control input. Use existing production gates only if legitimate new input exists.
3. If still absent and CI is green, do not repeat player production reachability, generation, lifecycle, reconnect/exact-instance admission, loadout, target-admission, attack-clock or boss owner/generation/server-lifetime work.
4. Audit the next objective M3 server-authority divergence at the production network/runtime boundary. Prioritize whether any retained result/capability can be applied after its authoritative actor/world context has changed, or whether malformed/reordered transport can mutate a newer authoritative state. Distinguish actual production reachability from fixture-only APIs and only patch a demonstrated path.
5. Keep production hit geometry, damage/range/resource, timing tuning, final controls/ItemStack provisioning/model/animation/VFX/sound and multiplayer feel blocked on evidence/approval.
