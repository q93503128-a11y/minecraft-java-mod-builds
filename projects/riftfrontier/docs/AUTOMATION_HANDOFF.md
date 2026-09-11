# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Run-start remote `main`: `25e9097d81e1fec34e5ff967af46c25b1ebba283`.
- Previous handoff descendant `Build Riftfrontier` run `34561558794`, HEAD `25e9097d81e1fec34e5ff967af46c25b1ebba283`: final `SUCCESS`.
- No newly approved Region 01 final boss source/profile/assets, production player ItemStack provisioning/control mapping, shape-specific hit geometry, damage/range/resource policy, or final presentation input was found. None was invented.

## Completed in this batch

M3 retained boss-presentation delivery-context authority fence:

- Code/test HEAD before handoff: `f88efea168a97cefc118c2540e1fea9a54933bd3`.
- Audited the requested production network/runtime boundary rather than repeating player reconnect/generation or boss owner-lifetime work.
- Player move transport remains authenticated `ServerPlayer` + move-id-only intent; no demonstrated malformed/reordered production mutation path justified inventing a sequence protocol without approved client control/provisioning input.
- Found a real retained-result divergence in boss presentation delivery: `ValidatedTickResult` is immutable and can outlive the Minecraft actor/world context that produced it, while the previous network bridge only compared entity id/UUID and a caller-supplied tick before fan-out.
- Added `BossPresentationDeliveryGuard` as the final server-side admission fence before packet distribution.
- Delivery now requires an authoritative `ServerLevel`, a combat-eligible non-player boss, exact keyed entity identity (`level.getEntity(uuid) == boss`), the level's current game tick matching the sampled tick, and semantic state entity id/UUID/tick matching the same actor/sample.
- `RiftfrontierNetworking.syncBossPresentation` invokes that guard immediately before `PacketDistributor.sendToPlayersTrackingEntityAndSelf`.
- Extended native `boss_runtime_owner_lifecycle`: it retains a presentation result produced while the owner is valid, removes the exact owner, then proves the production network bridge rejects the stale retained result before fan-out.
- Existing boss generation/owner singleton/dimension/entity-leave/server-stop rules and player authority rules were not weakened or duplicated.
- No balance, hit geometry, damage/range/resource policy, controls, ItemStack provisioning, model, animation, VFX or sound was invented.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/BossPresentationDeliveryGuard.java`
- `src/main/java/kr/moonseungjun/riftfrontier/network/RiftfrontierNetworking.java`
- `src/main/java/kr/moonseungjun/riftfrontier/gametest/BossRuntimeLifecycleGameTests.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- `Build Riftfrontier` run `34564677672`, code/test HEAD `f88efea168a97cefc118c2540e1fea9a54933bd3`: final `SUCCESS`.
- Successful workflow gates: Java/Gradle setup and toolchain verification; asset intake tool tests; JUnit + clean build; required native GameTest gate; dedicated-server smoke; Xvfb client smoke; executable JAR inspection; build report; deliverable upload; logs/report upload.
- Native `boss_runtime_owner_lifecycle` remained green with the new production-network retained-result rejection regression.
- Local Gradle: NOT RUN in this automation environment; executable validation was performed by GitHub Actions.
- Human field play, multiplayer latency/feel, concrete player controls, production boss/player hit geometry/damage/resource policy and final player/boss presentation: NOT TESTED / NOT APPROVED.

## Do not repeat or revert

- M2 persistence/restart/Region 01 technical-proxy lifecycle and owner attribution work is DONE.
- Boss presentation semantic capability and network payload plumbing are DONE; do not expand presentation without legitimate approved input.
- Boss presentation delivery now revalidates exact live server entity/world/current-tick context immediately before fan-out; do not return to id/UUID/caller-tick-only admission.
- Player `weapon_family` / `weapon_module` schema, two-family production graph, server-owned ItemStack loadout component and move-id-only serverbound intent are DONE.
- Production player combat is authenticated `ServerPlayer`-only by reachability; do not add a redundant player-role gate to the generic fixture/capability adapter without a new production path.
- Player sessions remain published-generation + exact actor-instance + exact currently registered server-level instance + dimension + loadout scoped and combat-eligibility gated; player process-local combat state is cleared at server stop.
- Delayed stale player callbacks must never replace or UUID-wide-clear a successor session; preserve `level.getEntity(player.getUUID()) == player` admission and exact-instance stale cleanup.
- Boss validated runtimes remain generation-bound, exact entity-instance + dimension lifetime-bound, singleton per live owner, owner-context mutation gated, entity-leave/server-stop retired, and explicitly reject `ServerPlayer` ownership.
- Shared target admission and attack-clock boundaries are already closed.
- Provisional attack ticks are not final balance. Do not invent blocked geometry/damage/range/resource/control/art values.

## Exact next start point

1. Re-check current remote `main` and the final status of code run `34564677672` plus any handoff descendant CI. If a descendant failed, fix the first actual failing gate without weakening authority.
2. Re-check approved Region 01 boss/profile/assets and player ItemStack provisioning/control input. Use existing production gates only if legitimate new input exists.
3. If still absent and CI is green, do not repeat player production reachability/generation/lifecycle/reconnect/exact-instance/loadout/target/attack-clock or boss owner/generation/server-lifetime/presentation-world-context work.
4. Audit the next objective M3 server-authority divergence. A priority candidate is whether a boss presentation result produced under a now-stale published content generation can still be delivered within an otherwise live actor/world context after atomic content publication; patch only if production reachability demonstrates the gap, and preserve the new exact-world/current-tick delivery fence.
5. If that boundary is already sealed by existing capability semantics, move to the next demonstrable production state handoff rather than adding speculative protocol infrastructure.
6. Keep production hit geometry, damage/range/resource, timing tuning, final controls/ItemStack provisioning/model/animation/VFX/sound and multiplayer feel blocked on evidence/approval.
