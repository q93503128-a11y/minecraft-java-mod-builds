# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` verified at this run start: `fbf1b3b1b6ea200341e810d52cd382e68935bfd7`.
- Latest verified Riftfrontier implementation/test HEAD: `a5c31ae892249c0d1bac7399bcc6a8de3757686d`.
- `Dragon Evolved` remains the selected, sanitized and packaged Region 01 first-boss geometry/rig/unaltered-source-animation resource. Source `Atlas` art remains explicitly unapproved and excluded.
- Exact accepted runtime derivation SHA-256 remains `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.

## Completed in this batch

M3 Region 01 typed reviewed-animation preparation and renderer-publication provenance gate:

- Added `Region01BossAnimationPreparation`: a production animation capability can now be created only from the exact current `Region01BossGeometryPreparation.PreparedGeometry` and its non-forgeable `ValidatedReload`.
- Preparation requires `BossAnimationSourceBinding.requireReviewedPhaseWindows()` and resolves every logical key only against the exact `Region01BossRuntimeAsset.animations()` inventory imported with that prepared geometry.
- The capability retains the exact reviewed binding, immutable resolved source windows, exact `BossAnimationSampleBridge`, exact runtime asset, content generation and publication generation; stale reload/clear fails closed.
- `RiftfrontierClientResources` now stages and lifecycle-cleans prepared animation independently of geometry. It still does not invent a production logical binding because Region 01 server boss semantic keys/timing are not authored.
- `Region01BossClientRenderRuntime.publish(...)` now requires `PreparedAnimation`, not free-standing geometry. Publication verifies exact prepared mesh identity and exact prepared animation-bridge identity, preventing reviewed-window preparation followed by substitution of a separately assembled animation bridge.
- Published `SubmissionBinding` retains the reviewed animation binding and rechecks reviewed phase-window capability.
- Added API-free regression coverage for stale-generation rejection, unreviewed-window rejection, and exact geometry + animation-bridge identity publication.
- No server boss timing, attack pattern, logical key, material, texture, encounter, hitbox, VFX, sound or balance value was guessed.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/client/render/Region01BossAnimationPreparation.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/BossCustomGeometryRenderPipeline.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/render/Region01BossClientRenderRuntime.java`
- `src/main/java/kr/moonseungjun/riftfrontier/client/RiftfrontierClientResources.java`
- `src/test/java/kr/moonseungjun/riftfrontier/client/render/Region01BossAnimationPreparationTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Implementation/test HEAD `a5c31ae892249c0d1bac7399bcc6a8de3757686d`, `Build Riftfrontier` run `34404859623`: FULL SUCCESS.
- CI passed Java 25 toolchain verification, asset-intake tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb graphical client smoke, executable-JAR inspection, build report, deliverable upload and logs/report upload.
- Local container Java/GitHub clone validation: NOT RUN because the execution container could not resolve `github.com`; no local success was claimed.
- Exact production server-authoritative Region 01 boss `attack_pattern` / `boss_profile` / presentation logical keys: NOT AUTHORED.
- Production `BossAnimationSourceBinding.reviewed(...)` mapping into the new prepared-animation capability: NOT AUTHORED because server semantic keys/timing are not yet authored.
- Approved final material/texture/`RenderType`: NOT IMPLEMENTED / NOT APPROVED.
- Actual encounter attachment, final dimensions/hitbox/scale, spawned/deformed Dragon graphical capture, VFX/sound and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Exact Dragon source reacquisition, deterministic sanitizer acceptance, runtime packaging, eight-clip visual motion review and native-keyframe `Headbutt`/`Punch` source-window review are DONE.
- Preserve source SHA-256 `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`, accepted derivation SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`, and provenance SHA-256 `3e16877a0043cf980ac8de05bb518834c5e77bd72d96103b4984c65a2a5a4c6c`.
- Preserve `BossAnimationMotionReview`, `BossAnimationPhaseWindowReview`, exact source-window evidence, `Region01BossAnimationPreparation`, exact prepared `BossAnimationSampleBridge` identity publication and server-authoritative phase progress.
- Never infer gameplay semantics from clip names/durations/channel metrics and never treat source `ACTION` as Minecraft damage authorization by itself.
- Preserve exact-source/structure/inventory gates, staged exact-`ResourceManager` transaction, exact prepared `SkinnedMeshAsset` identity publication, UUID actor identity, monotonic server ticks, ACTIVE-only authoritative damage, four-influence LBS and arbitrary triangle topology.
- Do not allow source `Atlas` material/texture/image bytes into production. Do not restore GeckoLib/rigid-cube approximation as a shortcut.
- Do not attach the boss to encounter/natural spawning or invent final dimensions/hitbox/balance timing without authored server semantics and field/visual evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs, M3 combat/presentation code, `REFERENCE_TARGETS.md`, `THIRD_PARTY_ASSETS.md`, Dragon motion-review receipts, then this handoff.
2. Do not redo Dragon source/motion/window review or the typed geometry/animation preparation gates.
3. Inspect the current boss-presentation asset manifest/material path and determine whether there is already an objectively approved material/texture/`RenderType` contract. Source `Atlas` remains prohibited. If there is no approved visual treatment, do not invent one.
4. If a defensible approved material contract exists, implement a typed material-preparation capability tied to the same current `PreparedAnimation`/`ValidatedReload`, then make renderer publication require geometry + reviewed animation + approved material from one exact reload transaction.
5. If no approved material exists, inspect the M3 combat/content schemas and author only the server boss profile/selectors/logical animation keys that can be defined without guessing field-balance timing; construct production source bindings only via `BossAnimationSourceBinding.reviewed(...)` and feed them through `RiftfrontierClientResources.prepareBossAnimation(...)`.
6. After both reviewed animation and approved material are legitimately available in one current reload, perform actual spawned-boss graphical capture before encounter attachment, final hitbox/telegraph tuning, VFX/sound or balance work.
