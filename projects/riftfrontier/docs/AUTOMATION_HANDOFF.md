# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Run-start remote `main`: `5a11cc5fbaf0ec2fd56f2de24a5ecfef0e491bee`.
- Previous code `Build Riftfrontier` run `34568200760`, HEAD `bad9579ed7110d6351d1c1781b53deda13b1b10a`: final `SUCCESS`.
- Previous handoff descendant `Build Riftfrontier` run `34568262027`, HEAD `5a11cc5fbaf0ec2fd56f2de24a5ecfef0e491bee`: final `SUCCESS`.
- No newly approved Region 01 final boss source/profile/assets, production player ItemStack provisioning/control mapping, shape-specific hit geometry, damage/range/resource policy, or final presentation input was found. None was invented.

## Completed in this batch

M3 client retained boss-presentation actor-leave tombstone fence:

- Code/test HEAD before handoff: `99df3a8acc48df4b22ed5110e84ae0283a61b120`.
- Audited the next retained-result lifecycle handoff instead of repeating the completed server owner/world/tick/content-generation fences.
- Found a real client-side replay gap: `BossPresentationClientState.forgetActor(...)` removed both active presentation and its ordering watermark on `EntityLeaveLevelEvent`. A delayed pre-leave packet for the same entity id/UUID could therefore be accepted afterward and recreate stale active presentation.
- Actor leave now converts the exact actor entry into a tombstone that preserves UUID + latest authoritative server tick while removing active semantics.
- Same/older tick packets for that UUID cannot resurrect presentation after leave. A later same-UUID presentation must advance authoritative server time before it can become active again.
- Numeric entity-id reuse by a different UUID remains allowed and still cannot expose the previous actor's state.
- `clearAll()` remains the explicit connection/world-epoch reset, so a reconnect may legitimately start from a lower server-tick epoch after the old tombstones are discarded.
- Expanded pure-Java client cache regression coverage for delayed pre-leave replay, existing clear tombstones, UUID-safe id reuse, later authoritative reactivation, and disconnect epoch reset.
- No balance, hit geometry, damage/range/resource policy, controls, ItemStack provisioning, model, animation, VFX or sound was invented.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/client/BossPresentationClientState.java`
- `src/test/java/kr/moonseungjun/riftfrontier/client/BossPresentationClientStateTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Previous baseline runs `34568200760` and `34568262027`: final `SUCCESS`.
- Current code/test `Build Riftfrontier` run `34572661455`, HEAD `99df3a8acc48df4b22ed5110e84ae0283a61b120`: `IN PROGRESS` at handoff write. Toolchain verification, asset intake tests, and `Tests and clean build` are confirmed `SUCCESS`; required native GameTest was in progress. Dedicated-server smoke, Xvfb client smoke, executable JAR inspection, report and artifact gates were not yet complete and must not be claimed successful until the run or a descendant finishes.
- An unrelated `.github/workflows/cd-a15-export-exact3.yml` run failed on the same repository commit; it is not the Riftfrontier validation workflow and does not substitute for `Build Riftfrontier`.
- Local Gradle: NOT RUN in this automation environment because the sandbox has no usable GitHub/network checkout path; executable validation is delegated to repository CI.
- Human field play, multiplayer latency/feel, concrete player controls, production boss/player hit geometry/damage/resource policy and final player/boss presentation: NOT TESTED / NOT APPROVED.

## Do not repeat or revert

- M2 persistence/restart/Region 01 technical-proxy lifecycle and owner attribution work is DONE.
- Boss presentation semantic capability and network payload plumbing are DONE; do not expand presentation without legitimate approved input.
- Boss presentation delivery revalidates exact live server entity/world/current-tick context immediately before fan-out; preserve it.
- Retained boss presentation semantics are published-generation-bound; never allow a pre-reload semantic capability to fan out after a newer atomic content generation becomes authoritative.
- Client boss presentation actor leave now preserves an ordering-watermark tombstone; delayed same/older pre-leave packets must never resurrect retired presentation. Only connection/world epoch reset may discard those tombstones wholesale.
- Player `weapon_family` / `weapon_module` schema, two-family production graph, server-owned ItemStack loadout component and move-id-only serverbound intent are DONE.
- Production player combat is authenticated `ServerPlayer`-only by reachability; player sessions remain published-generation + exact actor-instance + exact server-level instance + dimension + loadout scoped and combat-eligibility gated; process-local state is cleared at server stop.
- Delayed stale player callbacks must never replace or UUID-wide-clear a successor session.
- Boss validated runtimes remain generation-bound, exact entity-instance + dimension lifetime-bound, singleton per live owner, owner-context mutation gated, entity-leave/server-stop retired, and explicitly reject `ServerPlayer` ownership.
- Shared target admission and attack-clock boundaries are already closed.
- Provisional attack ticks are not final balance. Do not invent blocked geometry/damage/range/resource/control/art values.

## Exact next start point

1. Re-check current remote `main` and final status of `Build Riftfrontier` run `34572661455` plus any handoff descendant CI. If failed, fix the first actual failing gate without weakening the client tombstone or existing server authority fences.
2. Re-check approved Region 01 boss/profile/assets and player ItemStack provisioning/control input. Use existing production gates only if legitimate new input exists.
3. If still absent and CI is green, do not repeat player production reachability/generation/lifecycle/reconnect/exact-instance/loadout/target/attack-clock, boss owner/generation/server-lifetime/presentation world/tick/content-generation, or client actor-leave ordering-watermark work.
4. Audit the next demonstrable M3 retained capability/state handoff with real production reachability. Prefer an existing client render/resource-state or server→client lifecycle boundary only if a concrete stale-authority path is demonstrated; do not add speculative protocol infrastructure.
5. Keep production hit geometry, damage/range/resource, timing tuning, final controls/ItemStack provisioning/model/animation/VFX/sound and multiplayer feel blocked on evidence/approval.
