# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` verified at this run start: `2e100500f2a4abbdca932f6df5507fe9da308986`.
- Latest verified Riftfrontier implementation/test HEAD: `5bd87fd4bb8ec3b1bece834912964e342838fbc9`.
- `Dragon Evolved` is selected, sanitized and packaged as the Region 01 first-boss geometry/rig/unaltered-source-animation runtime resource. Source `Atlas` art remains explicitly unapproved and excluded.
- Direct visual motion review of all eight accepted source clips is complete and recorded in `assets/sources/region_01_boss_dragon_evolved.motion_review.json`; gameplay/logical bindings remain separately gated.

## Completed in this batch

M3 Region 01 boss reviewed source-window sampling:

- Recovered that exact Dragon source acquisition, deterministic art-neutral sanitizer acceptance, runtime resource packaging, same-`ResourceManager` geometry preparation, prepared-mesh provenance publication and direct visual review of all eight source clips are already complete on current `main`.
- Identified a production integration defect before authoring logical mappings: the previous `BossAnimationSampleBridge` replayed an entire source clip from 0% to 100% for every logical presentation phase. A real source action such as the reviewed `Headbutt` or `Punch` contains anticipation/action/recovery in one clip, so reusing it for TELEGRAPH/ACTIVE/RECOVERY would restart the action at each phase boundary.
- `BossAnimationSourceBinding` now supports an explicit normalized `ClipWindow[start,end]` per logical animation key. The explicit window map must exactly cover the binding key set; missing/extra windows fail closed.
- Every explicit window still requires concrete `BossAnimationMotionReview` approval and a verified imported source clip. Name similarity, duration and channel metrics remain insufficient evidence.
- `BossAnimationSampleBridge` now maps only the authoritative server phase progress into the selected source window. It still owns no independent animation clock.
- Zero-width windows are intentionally valid for a reviewed held pose; invalid/non-finite/out-of-range/reversed windows are rejected.
- The previous two-argument full-clip binding and direct clip-map bridge constructors remain for compatibility/general fixtures. Production phase mappings should use explicit windows once their boundaries are reviewed and authored.
- No Region 01 production boss attack timing, logical animation key, phase boundary, material, texture, encounter, hitbox, VFX, sound or balance value was guessed or authored in this batch.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossAnimationSourceBinding.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossAnimationSampleBridge.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/BossAnimationSourceBindingTest.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/BossAnimationSampleBridgeTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Implementation/test HEAD `5bd87fd4bb8ec3b1bece834912964e342838fbc9`, `Build Riftfrontier` run `34393651418`: FULL SUCCESS. Passed toolchain verification, asset intake tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb client smoke, executable JAR inspection, report generation, deliverable upload and logs/report upload.
- Direct visual inspection of the actual eight accepted Dragon source clips: DONE; receipt `assets/sources/region_01_boss_dragon_evolved.motion_review.json` is separately validated on `main`.
- Exact production server-authoritative Region 01 boss `attack_pattern` / `boss_profile` / presentation logical keys: NOT AUTHORED.
- Reviewed normalized anticipation/ACTIVE/recovery source-window boundaries for `Headbutt`, `Punch` or other action clips: NOT AUTHORED / NOT TESTED. The current coarse five-sample motion receipt proves clip motion identity, not exact combat phase cut points.
- Approved final material/texture/`RenderType` publication, actual Region 01 encounter attachment, final dimensions/hitbox/scale, spawned/deformed Dragon graphical capture, VFX/sound and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Exact Dragon source reacquisition, deterministic sanitizer acceptance, runtime packaging and direct eight-clip visual review are DONE. Pinned source SHA-256: `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`; accepted derivation SHA-256: `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`; provenance SHA-256: `3e16877a0043cf980ac8de05bb518834c5e77bd72d96103b4984c65a2a5a4c6c`.
- Preserve the exact eight source clips, animation audit receipt, visual motion-review receipt and `BossAnimationMotionReview` requirement. Never infer semantics from clip names, durations or channel metrics.
- Preserve the new source-window contract. Do not regress production phase mappings to restarting a whole source action clip for every TELEGRAPH/ACTIVE/RECOVERY phase.
- Preserve exact-source/structure/inventory gates, staged exact-`ResourceManager` transaction, `Region01BossGeometryPreparation`, prepared `SkinnedMeshAsset` identity publication, UUID-bound actor identity, monotonic server ticks, server-authoritative ACTIVE-only damage, four-influence LBS and arbitrary triangle topology.
- Do not allow source `Atlas` material/texture/image bytes into production. Do not restore GeckoLib/rigid-cube approximation as a shortcut.
- Do not attach the boss to encounter/natural spawning or invent final dimensions/hitbox/combat/phase-window tuning before authored server semantics and review evidence exist. Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs, M3 resource/render code, `REFERENCE_TARGETS.md`, `THIRD_PARTY_ASSETS.md`, the current Dragon motion-review receipt, then this handoff.
2. Do not fabricate Region 01 boss balance/timing: production `region_01` still lacks authored server-authoritative boss attack patterns/profile/presentation logical keys.
3. Perform finer deterministic frame/pose review of the accepted `Headbutt` and `Punch` clips to identify defensible normalized anticipation/action/recovery boundaries. Record those cut points in a separate review receipt; the existing coarse 0/25/50/75/100% motion receipt alone is not sufficient to claim exact phase boundaries.
4. Once server-authoritative boss selectors/logical animation keys are authored from the M3 combat contract, create `BossAnimationSourceBinding` entries only for semantics supported by observed motion and pair each with the reviewed `ClipWindow` rather than whole-clip replay.
5. Stage that reviewed binding capability in the same current `RiftfrontierClientResources` reload transaction as `Region01BossGeometryPreparation`, and construct `BossCustomGeometryRenderPipeline` only from the current prepared mesh.
6. Keep approved material/texture/`RenderType` as an independent fail-closed prerequisite. When geometry + reviewed windowed binding + approved material all derive from one current reload, publish atomically and perform actual spawned-boss graphical capture before encounter attachment, final hitbox/telegraph, VFX/sound or tuning.
