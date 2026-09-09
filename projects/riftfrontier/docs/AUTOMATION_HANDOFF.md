# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` verified at this run start: `3add650a72db4f66031a64f3ce4136485a61046d`.
- Latest verified Riftfrontier implementation/test HEAD: `a21e574473411cdb3210f2614c3896efaece5401` (`feat(riftfrontier): retain boss reload resource snapshot`).
- `Dragon Evolved` remains selected only as the Region 01 first-boss geometry/rig/animation derivation source. Source `Atlas` art remains unapproved.

## Completed in this batch

M3 Region 01 boss client-resource reload transaction:

- `RiftfrontierClientResources` now begins the renderer reload against the exact `ResourceManager` instance being inspected instead of discarding an unbound generation ticket.
- After `BossPresentationClientAssetRuntime` physically validates the selected manifest from that same resource manager, the result is retained as a non-forgeable `Region01BossClientRenderRuntime.ValidatedReload`.
- The staged capability carries the exact `ResourceManager`, validated asset snapshot/selection, content generation and publication generation together.
- Final renderer publication no longer accepts an arbitrary ready snapshot. It accepts only the currently staged capability, preventing geometry/material preparation from swapping in a separately validated resource selection at publish time.
- Starting a newer reload or `clear()` invalidates both the published binding and staged preparation. A race between validation and a newer reload is rechecked and fails closed.
- `GenerationPublicationSlot.isCurrent(...)` was added with wrong-slot rejection and stale-ticket regression coverage.
- No fallback model/material, guessed animation binding, source Atlas material, encounter spawn, hitbox/scale, or balance change was introduced.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/client/RiftfrontierClientResources.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/Region01BossClientRenderRuntime.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/GenerationPublicationSlot.java`
- `src/test/java/kr/moonseungjun/riftfrontier/client/render/GenerationPublicationSlotTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Prior baseline commit `3add650a72db4f66031a64f3ce4136485a61046d`, `Build Riftfrontier` run `34369764689`: FULL SUCCESS (confirmed this run).
- Implementation commit `a21e574473411cdb3210f2614c3896efaece5401`, `Build Riftfrontier` run `34375241614`: FULL SUCCESS. Passed toolchain, asset intake tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb client smoke, executable JAR inspection, report generation and artifact uploads.
- Unrelated `.github/workflows/cd-a15-export-exact3.yml` run `34375240260` failed immediately for this same push; it is not the Riftfrontier build gate and was not used as Riftfrontier validation evidence.
- Direct visual inspection of the actual eight Dragon clip motions: NOT TESTED.
- Explicit approved logical attack-to-source-clip mappings: NOT AUTHORED.
- Loading the accepted sanitized Dragon derivation from the staged `ResourceManager` into `Region01BossRuntimeAsset`: NOT IMPLEMENTED.
- Approved material/texture/`RenderType` publication, actual Region 01 encounter attachment, final dimensions/hitbox/scale, spawned/deformed Dragon graphical capture, VFX/sound and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Exact Dragon source reacquisition is DONE. Pinned immutable mirror: `laoniutoushx/TD-demo-2024-04-03@87051774343f2a0df215639e8674178437228b71`, `Asserts/Models/ulimate monster/glTF/Dragon_Evolved.gltf`.
- Preserve source SHA-256 `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`, accepted derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`, provenance SHA `3e16877a0043cf980ac8de05bb518834c5e77bd72d96103b4984c65a2a5a4c6c`, and `assets/sources/region_01_boss_dragon_evolved.animation_audit.json` unless the source/converter contract intentionally changes.
- Preserve `BossAnimationMotionReview`; never infer semantics from `Headbutt`, `Punch`, other clip names, durations or channel metrics.
- Preserve UUID-bound actor identity, monotonic server ticks, server-authoritative attack timing/ACTIVE-only damage, exact source/structure/inventory gates, four-influence LBS, arbitrary triangle topology, custom geometry renderer, actor/render bridge, and the new exact-`ResourceManager` staged reload capability.
- Do not allow source `Atlas` material/texture/image bytes into production; do not restore GeckoLib/rigid cube approximation.
- Do not attach the boss to encounter/natural spawning or invent final dimensions/hitbox/combat tuning before visual/source evidence and authored content exist. Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs, relevant M3 render/resource code, then this handoff.
2. If direct visual animation inspection is available, inspect all eight accepted Dragon clips and create `BossAnimationMotionReview` approvals only from concrete observed motion; author logical bindings only where observed semantics match server-authoritative states.
3. If visual inspection is still unavailable, do not guess. Continue the objective M3 resource transaction from `Region01BossClientRenderRuntime.staged()`: inspect the selected production MODEL physical resource and packaging contract, then add a typed preparation stage that reads the accepted sanitized derivation bytes only through `ValidatedReload.resourceManager()` and requires `Region01BossRuntimeAsset.importAccepted(...)` to pass before retaining geometry/rig/animation runtime data.
4. Keep approved animation binding and approved material/`RenderType` as independent fail-closed prerequisites. Do not publish a renderer binding until all three products (exact runtime asset, reviewed binding, approved material) derive from the currently staged reload.
5. After atomic publication is complete, attach the authored Region 01 encounter and perform an actual spawned graphical capture before final hitbox/telegraph/VFX/sound tuning.
