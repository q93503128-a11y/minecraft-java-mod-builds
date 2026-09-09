# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` recovered at this run start: `eac322de871fe52c77c9557d3d10346a7c578ed1`.
- Concurrent unrelated `turnbound-re` work advanced `main` to `94ce1513d7bbbd16a20975aee2c982e0b0d8dff5` while this batch was being prepared. The first fast-forward attempt was rejected; the Riftfrontier change was rebuilt on that newer tree without overwriting the unrelated commit.
- Previous green animation import/pose-sampling baseline: implementation `036cda32e6451b50af55a38ca19e579ab5b22628`, `Build Riftfrontier` run `34301699458`: full SUCCESS.
- `Dragon Evolved` remains selected only as Region 01 first-boss geometry/rig derivation source. Source `Atlas` art remains unapproved.

## Completed in this batch

M3 authoritative semantic-state -> logical clip -> sample-time bridge:

- Added immutable `BossAnimationSampleBridge` between `BossPresentationSemanticState` / resolved presentation and renderer-neutral `AnimationClip`.
- Logical animation keys resolve through an immutable catalog; missing keys return no sample instead of guessing a fallback.
- Animation sample time is derived only from server-authored `phaseProgress * clip.durationSeconds` and never advances an independent animation/combat clock.
- The bridge verifies that semantic phase/progress/hit-window still exactly match the resolved presentation; stale resolver state fails closed.
- ACTIVE remains the only hit-window phase. COMPLETE and inactive semantic state cannot produce active animation samples.
- Zero-duration pose clips deterministically sample at zero.
- Added JUnit coverage for authoritative phase-progress sampling, inactive state, missing logical clip, stale resolved-state rejection and zero-duration clips.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossAnimationSampleBridge.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/BossAnimationSampleBridgeTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Test-bearing implementation commit: `720655333797ccfd2f49f210b77b3db85175e2ba`.
- `Build Riftfrontier` run `34305584752`: IN PROGRESS at handoff update; do not treat as green until the run conclusion is SUCCESS.
- Local build/test: NOT RUN because this execution container cannot resolve github.com and no repository checkout/dependency cache is available locally.
- Exact accepted 681773-byte sanitized Dragon derivation -> all eight real source clips: NOT RUN in this batch; the binary was not available in this execution environment.
- `LinearBlendSkinner` -> immutable `SkinnedMeshFrame` -> Minecraft 26.2 `SubmitNodeCollector.submitCustomGeometry` renderer bridge: NOT IMPLEMENTED / NOT TESTED.
- Real physical MODEL/ANIMATION resources and `presentation_assets` manifest: NOT IMPLEMENTED.
- Production texture/material, VFX/sound, scale/hitbox/deformation and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Do not repeat candidate selection/source fingerprint work unless the pinned immutable source check fails.
- Preserve source SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` and accepted derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` unless converter schema/version intentionally changes.
- Preserve `AttackPattern`/server semantic state as authoritative timing and ACTIVE-only damage semantics. `BossAnimationSampleBridge` must never become a free-running clock.
- Preserve fail-closed presentation resolution, atomic client reload and exact content-generation matching.
- Do not permit source `Atlas` material/texture/image bytes into production resources.
- Do not restore legacy GeckoLib 4 paths or force GeckoMesh/GeoBone onto this asset.
- Do not approximate arbitrary Dragon triangles into cubes/bounding boxes or collapse multi-joint weights to a dominant bone.
- Preserve project-owned four-influence LBS deformation semantics; optimize only after profiler evidence.
- Do not weaken exact derivation SHA/count validation to make a changed asset load.
- Do not create placeholder production resources or a fake `presentation_assets` manifest.
- Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs and this handoff. First close `Build Riftfrontier` run `34305584752`; if it fails, fix the first real Riftfrontier failure without shrinking features.
2. Reacquire the exact accepted sanitized Dragon derivation and run `GltfAnimationImporter` over all eight real source clips. Record exact clip names, durations, channel counts and interpolation coverage. Keep the pinned source/derivation SHA gates.
3. Bind the verified real clip inventory to logical animation keys consumed by `BossAnimationSampleBridge`; do not hardcode guessed production mappings before the source inventory is measured.
4. Feed each bridge sample through `JointPoseSampler` -> `LinearBlendSkinner` -> immutable `SkinnedMeshFrame`, then implement the Minecraft 26.2 `SubmitNodeCollector.submitCustomGeometry` client renderer bridge and validate Xvfb/client smoke.
5. Only after approved material/texture exists, perform scale/culling/UV/deformation/hitbox screen review and human field-play; only after all physical presentation assets exist may the first real `presentation_assets` manifest be created.
