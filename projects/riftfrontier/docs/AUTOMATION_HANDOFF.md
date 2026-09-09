# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` recovered at this run start: `e985913a97544d6811198dfcd38fbd4a1a48b06f`.
- Previous `BossAnimationSampleBridge` validation run `34305584752` was re-checked this run and is fully SUCCESS.
- Concurrent unrelated `turnbound-re` work advanced `main` after the first Riftfrontier implementation commit; it was preserved and the Riftfrontier follow-up was rebuilt on top without force-push or overwrite.
- `Dragon Evolved` remains selected only as Region 01 first-boss geometry/rig derivation source. Source `Atlas` art remains unapproved.

## Completed in this batch

M3 authoritative animation sample -> deformed renderer-neutral frame composition:

- Added `BossSkinnedMeshFrameSampler` as the final renderer-neutral composition boundary.
- It accepts only an already-authoritative `BossAnimationSampleBridge.Sample`; it owns no animation clock and performs no duplicate logical-clip resolution.
- It feeds the authoritative sample time through `JointPoseSampler`, then through project-owned four-influence `LinearBlendSkinner`, returning immutable `SkinnedMeshFrame` geometry together with the exact source sample metadata.
- It fail-closes if the pose palette joint count is inconsistent or skinning changes accepted vertex/triangle topology.
- Added JUnit coverage for authoritative-time deformation, different authoritative times without any internal clock, topology/UV/index preservation, and animation channels outside the imported rig.
- Initial run `34309560622` exposed two new test-fixture expectation errors: the fixture treated normalized progress values as seconds. Production code compiled; no feature was weakened. The fixture was corrected in follow-up commit `7d7e5defd54c3a95012621b85ff61013db73d192`.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossSkinnedMeshFrameSampler.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/BossSkinnedMeshFrameSamplerTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Implementation commit: `c40a726fbf3351e1204a3c012d6fe15d7b0a87b4`.
- Corrected test-bearing commit: `7d7e5defd54c3a95012621b85ff61013db73d192`.
- Initial `Build Riftfrontier` run `34309560622`: asset-tool tests SUCCESS; compile SUCCESS; JUnit failed only in the two new incorrect fixture expectations; later runtime gates were skipped. This failure was corrected rather than bypassed.
- Corrected `Build Riftfrontier` run `34309678765`: toolchain SUCCESS, asset-intake tests SUCCESS, JUnit + clean build SUCCESS, required GameTest SUCCESS; dedicated-server smoke was still IN PROGRESS at this handoff write. Client/Xvfb, executable JAR inspection, reports/artifacts therefore remain PENDING until that run finishes. Do not call the full run green until its conclusion is SUCCESS.
- Local build/test: NOT RUN because this execution environment has no usable local repository/dependency checkout; GitHub Actions is the executed validation path.
- Exact accepted sanitized Dragon derivation -> all eight real source clips: NOT RUN in this batch because the accepted binary was not available in conversation/library/runtime. Production clip names/mappings were not guessed.
- Minecraft 26.2 `SubmitNodeCollector.submitCustomGeometry` renderer adapter and actual rendered boss review: NOT IMPLEMENTED / NOT TESTED.
- Real physical MODEL/ANIMATION resources and `presentation_assets` manifest: NOT IMPLEMENTED.
- Production texture/material, VFX/sound, scale/hitbox/deformation and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Do not repeat candidate selection/source fingerprint work unless the pinned immutable source check fails.
- Preserve source SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c` and accepted derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` unless converter schema/version intentionally changes.
- Preserve `AttackPattern`/server semantic state as authoritative timing and ACTIVE-only damage semantics. `BossAnimationSampleBridge` and `BossSkinnedMeshFrameSampler` must never become free-running clocks.
- Preserve fail-closed presentation resolution, atomic client reload and exact content-generation matching.
- Do not permit source `Atlas` material/texture/image bytes into production resources.
- Do not restore legacy GeckoLib 4 paths or force GeckoMesh/GeoBone onto this asset.
- Do not approximate arbitrary Dragon triangles into cubes/bounding boxes or collapse multi-joint weights to a dominant bone.
- Preserve project-owned four-influence LBS deformation semantics and the completed glTF mesh/rig importer, animation importer, pose sampler, semantic sample bridge and frame sampler; optimize only after profiler evidence.
- Do not weaken exact derivation SHA/count validation to make a changed asset load.
- Do not create placeholder production resources or a fake `presentation_assets` manifest.
- Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs and this handoff. First close `Build Riftfrontier` run `34309678765`; if it fails after the already-green GameTest stage, fix its first real server/client/JAR failure without shrinking features.
2. Reacquire the exact accepted sanitized Dragon derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac` and run `GltfAnimationImporter` over all eight real source clips. Record exact clip names, durations, channel counts and interpolation coverage. Keep source/derivation SHA gates.
3. Bind only that verified real clip inventory to logical animation keys consumed by `BossAnimationSampleBridge`; do not guess production mappings.
4. Connect `BossSkinnedMeshFrameSampler.FrameSample.frame()` to a Minecraft 26.2 `SubmitNodeCollector.submitCustomGeometry` renderer adapter, preserving immutable topology/UV/index data and server-authored sample timing, then validate under Xvfb/client smoke.
5. Only after approved material/texture exists, perform scale/culling/UV/deformation/hitbox screen review and human field-play; only after all physical presentation assets exist may the first real `presentation_assets` manifest be created.
