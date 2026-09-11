# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Last recovered baseline

- Run-start remote `main`: `0c46050d7eb8f720baeacda67fcc5f6307a5b1e0`.
- Previous code `Build Riftfrontier` run `34572661455`, HEAD `99df3a8acc48df4b22ed5110e84ae0283a61b120`: final `SUCCESS`.
- Previous handoff descendant `Build Riftfrontier` run `34572773683`, HEAD `0c46050d7eb8f720baeacda67fcc5f6307a5b1e0`: final `SUCCESS`.
- No newly approved Region 01 final boss source/profile/assets, production player ItemStack provisioning/control mapping, shape-specific hit geometry, damage/range/resource policy, or final presentation input was found. None was invented.

## Completed in this batch

M3 client boss-render connection-epoch retirement fence:

- Code/test HEAD before handoff: `53183214b210357dc7536258cad74fd0aedeced2`.
- Audited the next retained client capability after the actor-leave tombstone work.
- Found a concrete production lifecycle gap: logout cleared `BossPresentationClientState`, but the immutable prepared geometry/animation/material and `Region01BossClientRenderRuntime` publication could survive because a client resource reload is not guaranteed between server connections.
- A later connection could therefore accept fresh server semantics and feed them through a renderer capability reviewed/prepared against the previous connection epoch.
- `ClientPlayerNetworkEvent.LoggingOut` now retires both semantic ordering state and the full boss-presentation render/resource capability via `RiftfrontierClientResources.retireBossPresentationConnectionEpoch()`.
- Connection retirement clears prepared material, animation and geometry and invalidates `Region01BossClientRenderRuntime`; the next connection cannot reuse a previous publication unless a legitimate client resource preparation path establishes a new one.
- Entity-leave UUID/tick tombstones remain unchanged and are still actor-local; only disconnect retires the whole connection epoch.
- Expanded pure-Java lifecycle source contract so disconnect must clear both semantic cache and renderer-visible publication.
- No balance, hit geometry, damage/range/resource policy, controls, ItemStack provisioning, model, animation, VFX or sound was invented.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/client/RiftfrontierClientNetworking.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/RiftfrontierClientResources.java`
- `src/test/java/kr/moonseungjun/riftfrontier/client/RiftfrontierClientPresentationLifecycleTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Previous runs `34572661455` and `34572773683`: final `SUCCESS`.
- Current code/test `Build Riftfrontier` run `34577042551`, HEAD `53183214b210357dc7536258cad74fd0aedeced2`: `IN PROGRESS` at handoff write. Checkout and Java setup are confirmed `SUCCESS`; Gradle setup is in progress. Toolchain, asset tests, JUnit/clean build, native GameTest, dedicated-server smoke, Xvfb client smoke, executable JAR inspection, reports and artifacts are not yet complete and must not be claimed successful until the run or a descendant finishes.
- Local Gradle: NOT RUN in this automation environment; executable validation is delegated to repository CI.
- Human field play, multiplayer reconnect/latency feel, concrete player controls, production boss/player hit geometry/damage/resource policy and final player/boss presentation: NOT TESTED / NOT APPROVED.

## Do not repeat or revert

- M2 persistence/restart/Region 01 technical-proxy lifecycle and owner attribution work is DONE.
- Boss presentation semantic capability and network payload plumbing are DONE; do not expand presentation without legitimate approved input.
- Server boss presentation delivery remains exact-live-entity/world/current-tick and published-content-generation fenced.
- Client actor leave preserves UUID + ordering-watermark tombstones; delayed same/older pre-leave packets must never resurrect retired presentation.
- Client disconnect now also retires prepared/published boss-render capabilities; never let renderer publication cross a server connection epoch without fresh legitimate preparation.
- Player `weapon_family` / `weapon_module` schema, two-family production graph, server-owned ItemStack loadout component and move-id-only serverbound intent are DONE.
- Production player combat remains authenticated `ServerPlayer`-only, published-generation + exact actor-instance + exact server-level/dimension/loadout scoped, process-local state cleared at server stop, and stale callbacks cannot replace/UUID-wide-clear a successor session.
- Boss validated runtimes remain generation-bound, exact entity-instance + dimension lifetime-bound, singleton per live owner, entity-leave/server-stop retired, and reject `ServerPlayer` ownership.
- Shared target admission and attack-clock boundaries are already closed.
- Provisional attack ticks are not final balance. Do not invent blocked geometry/damage/range/resource/control/art values.

## Exact next start point

1. Re-check current remote `main` and final status of `Build Riftfrontier` run `34577042551` plus the handoff descendant CI. If failed, fix the first actual failing gate without weakening connection-epoch retirement or earlier authority fences.
2. Re-check approved Region 01 boss/profile/assets and player ItemStack provisioning/control input. Use existing production gates only if legitimate new input exists.
3. If still absent and CI is green, do not repeat player reachability/generation/lifecycle/reconnect/exact-instance/loadout/target/attack-clock, boss owner/generation/server-lifetime/presentation world/tick/content-generation, client actor-leave tombstone, or client connection-epoch render retirement work.
4. Audit the next demonstrable M3 retained capability/state handoff with real production reachability. Prefer a concrete renderer/resource publication or server→client lifecycle transition only if stale authority can actually cross it; do not add speculative protocol infrastructure.
5. Keep production hit geometry, damage/range/resource, timing tuning, final controls/ItemStack provisioning/model/animation/VFX/sound and multiplayer feel blocked on evidence/approval.
