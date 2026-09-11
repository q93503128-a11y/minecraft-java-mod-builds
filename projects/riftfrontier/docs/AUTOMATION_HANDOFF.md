# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Run-start remote `main`: `8d8fea0c24ec2b932cc2c92e9760597847ecaa81`.
- Previous player lifecycle run `34549242359`, code HEAD `27fe822de323c56a9875f56ab6f68cbb2b8899ee`: final `SUCCESS`.
- No newly approved Region 01 final boss source/profile/assets, production player ItemStack provisioning/control mapping, shape-specific hit geometry, damage/range/resource policy, or final presentation input was found. None was invented.

## Completed in this batch

M3 boss combat authoritative server-lifetime retirement:

- Code/test HEAD before handoff: `ea55e9d795f1a0f2a2cf6d359d1351972b690ef3`.
- Found a real process-lifetime divergence: validated boss capabilities were exact entity/dimension bound and retired on `EntityLeaveLevelEvent`, but the static `VALIDATED_RUNTIMES_BY_OWNER` index had no explicit `ServerStoppedEvent` fence.
- `MinecraftBossCombatAdapter.serverStopped` now snapshots exact owner/runtime pairs, clears the process-local owner index first, then irreversibly invalidates every detached runtime.
- Production bootstrap registers the boss server-stop boundary on the NeoForge event bus.
- Existing live-owner singleton, generation retirement, exact actor/dimension binding, owner-context mutation and entity-leave invalidation remain unchanged.
- `BossRuntimeLifecycleContractTest` now locks both entity-lifetime and server-lifetime cleanup wiring.
- No balance, controls, ItemStack provisioning, hit geometry, damage/range/resource, model, animation, VFX or sound was invented.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/MinecraftBossCombatAdapter.java`
- `src/main/java/kr/moonseungjun/riftfrontier/Riftfrontier.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/BossRuntimeLifecycleContractTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Previous code run `34549242359`: final `SUCCESS` across the Riftfrontier workflow.
- Current code/test `Build Riftfrontier` run `34553110431`, HEAD `ea55e9d795f1a0f2a2cf6d359d1351972b690ef3`: `IN PROGRESS` at handoff update. Do not claim full success until its final conclusion is observed.
- Local Gradle: NOT RUN in this automation environment; executable validation is through GitHub Actions.
- Human field play, multiplayer latency/feel, concrete player controls, production boss/player hit geometry/damage/resource policy and final player/boss presentation: NOT TESTED / NOT APPROVED.

## Do not repeat or revert

- M2 persistence/restart/Region 01 technical-proxy lifecycle work is DONE.
- Boss presentation/network semantic capability plumbing is DONE; do not add more without approved real assets.
- Player `weapon_family` / `weapon_module` schema, two-family production graph, server-owned ItemStack loadout component and move-id-only serverbound intent are DONE.
- Player sessions remain published-generation + exact actor-instance + dimension + loadout scoped and combat-eligibility gated; player process-local combat state is cleared at server stop.
- Boss validated runtimes remain generation-bound, exact entity-instance + dimension lifetime-bound, singleton per live owner, owner-context mutation gated, and now explicitly server-lifetime bound.
- Never restore a boss process-local owner index across `ServerStoppedEvent`.
- Shared actor/target admission and attack-clock boundaries are already closed.
- Provisional attack ticks are not final balance. Do not invent blocked geometry/damage/range/resource/control/art values.

## Exact next start point

1. Re-check current remote `main` and final status of code run `34553110431` plus the handoff descendant CI. If either failed, fix the first actual failing gate without weakening authority.
2. Re-check approved Region 01 boss/profile/assets and player ItemStack provisioning/control input. Use existing gates only if legitimate new input exists.
3. If still absent and CI is green, do not repeat boss/player generation, lifecycle, singleton, actor-instance, eligibility, target-admission, dimension, loadout, server-stop or attack-clock work. Audit the next objectively testable M3 authoritative-state divergence.
4. Prioritize event-driven lifecycle/state transitions or cross-capability ownership boundaries that can be verified without inventing presentation or balance.
5. Keep production hit geometry, damage/range/resource, timing tuning, final controls/ItemStack provisioning/model/animation/VFX/sound and multiplayer feel blocked on evidence/approval.
