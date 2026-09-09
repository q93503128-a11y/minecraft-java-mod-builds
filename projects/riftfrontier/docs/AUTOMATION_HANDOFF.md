# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` at this run start: `ae5aea754874b9eb0e8bdaeab0b6271e995dec9d`.
- Latest verified Riftfrontier implementation/test HEAD now on `main`: `99cfd0c3a7fb73c699fcc62dacee72b5b3e6ffd7`.
- Exact source reacquisition/test implementation in this run: `72eb6bbf9481e0b247685d6c9b3b76592d5b3421`.
- `Dragon Evolved` remains selected only as Region 01 first-boss geometry/rig/animation derivation source. Source `Atlas` art remains unapproved.

## Completed in this batch

M3 Region 01 boss exact-source recovery and animation evidence gate:

- Reacquired a complete byte-identical `Dragon_Evolved.gltf` from immutable mirror `laoniutoushx/TD-demo-2024-04-03` commit `87051774343f2a0df215639e8674178437228b71`, path `Asserts/Models/ulimate monster/glTF/Dragon_Evolved.gltf`.
- Complete bytes independently match the pre-existing selected-source SHA-256 `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`.
- Explicitly rejected another immutable mirror (`flawlesshappiness/EmotionCreatures@215ae451ba7690a3f765b01eea5295f37c120e5a`) because its complete bytes hash to `36bb52a2de81571389cfe9957346d43d25e75687b6f766b8e792a50df90ee2f3` rather than the selected source SHA.
- Deterministically reproduced accepted sanitized derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`, 681773 bytes, and provenance SHA `3e16877a0043cf980ac8de05bb518834c5e77bd72d96103b4984c65a2a5a4c6c`.
- Committed `assets/sources/region_01_boss_dragon_evolved.animation_audit.json` as the permanent receipt and replaced network-dependent proof with an offline regression gate cross-checking source/acceptance contracts.
- Measured all eight accepted clips. Every clip has 90 channels, 90 samplers, 45 animated nodes, 45 rotation + 45 translation targets, and 90 LINEAR interpolations. Durations: Death 0.6666666865s; Fast_Flying 0.8333333135s; Flying_Idle 1.5s; Headbutt 1.5s; HitReact 0.6666666865s; No 1.1666666269s; Punch 1.3333333731s; Yes 1.1666666269s.
- Preserved the distinction between numerical/source evidence and motion semantics: clip names/metrics alone still cannot authorize logical attack binding.
- Concurrent follow-on commit `99cfd0c3a7fb73c699fcc62dacee72b5b3e6ffd7` adds `BossAnimationMotionReview` and makes `BossAnimationSourceBinding` require explicit approved visual-review evidence before a source clip can be bound. This is retained and verified; it does not fabricate any review approval.

## Changed systems/files

- `assets/sources/region_01_boss_dragon_evolved.animation_audit.json`
- `tools/tests/test_region01_boss_remote_provenance.py`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossAnimationMotionReview.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossAnimationSourceBinding.java`
- corresponding boss animation binding tests
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Exact-source reacquisition/audit proof commit `14d1587ce85227e0a80983abe0dd65a6709769f2`, `Build Riftfrontier` run `34362397167`: FULL SUCCESS. Passed toolchain, source/sanitizer/audit tests, JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb client smoke, executable JAR inspection, report generation and artifact uploads.
- Permanent offline receipt commit corrected at `72eb6bbf9481e0b247685d6c9b3b76592d5b3421`, `Build Riftfrontier` run `34363812956`: FULL SUCCESS with the same complete gate set.
- Current implementation HEAD `99cfd0c3a7fb73c699fcc62dacee72b5b3e6ffd7`, `Build Riftfrontier` run `34364304659`: FULL SUCCESS with asset tests, JUnit/clean build, required GameTest, dedicated server, Xvfb client, executable JAR, report and artifact gates all successful.
- Earlier run `34361908051` hit the existing `attack_hit_window` GameTest twice while the unchanged combat code passed subsequent runs `34362397167`, `34363812956`, and `34364304659`; no production combat behavior or test was weakened to address that transient failure.
- Direct visual inspection of the actual eight clip motions: NOT TESTED.
- Explicit approved logical attack-to-source-clip mappings: NOT AUTHORED.
- Approved material/texture/`RenderType` publication, actual Region 01 encounter attachment, final dimensions/hitbox/scale, spawned/deformed Dragon graphical capture, VFX/sound and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Exact source reacquisition is DONE. Do not spend another session searching mirrors unless the selected source contract intentionally changes; the immutable byte-identical mirror and permanent receipt are now recorded.
- Preserve selected source SHA `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`, accepted derivation SHA `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`, provenance SHA `3e16877a0043cf980ac8de05bb518834c5e77bd72d96103b4984c65a2a5a4c6c`, and measured receipt unless converter/source contract intentionally changes.
- Do not infer gameplay semantics from `Headbutt`, `Punch`, or any other source clip name or from duration/channel metrics. Preserve `BossAnimationMotionReview` as a fail-closed prerequisite for production binding.
- Preserve UUID-bound presentation identity, monotonic server-tick handling, generation-ticket resource publication, fail-closed resource reload, server-authoritative `AttackPattern` timing and ACTIVE-only damage.
- Preserve exact-source/structure/inventory gates, `Region01BossRuntimeAsset`, four-influence LBS, arbitrary triangle topology, custom-geometry renderer pipeline and registered boss actor/render bridge.
- Do not permit source `Atlas` material/texture/image bytes into production resources; do not restore GeckoLib/rigid cube approximation.
- Do not attach the boss to natural/encounter spawning or invent final dimensions/hitbox/combat tuning before visual/source evidence and authored content exist.
- Do not tune M2 pressure/patrol values without field-play evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs and this handoff.
2. Use the committed animation audit receipt as factual numerical evidence; reacquire the accepted source immediately from the now-pinned immutable byte-identical mirror when actual source bytes are needed.
3. Perform direct visual motion inspection of all eight accepted clips and record concrete evidence/observed motion. Author `BossAnimationMotionReview` approvals only for clips actually inspected; then create logical bindings only where observed semantics match the intended server-authoritative states.
4. If direct visual animation inspection is unavailable in the execution environment, do not guess. Move to the other objective M3 bundle: extend `RiftfrontierClientResources` so its existing `beginReload()` ticket survives preparation through exact `Region01BossRuntimeAsset` + approved binding + approved material/`RenderType` publication from the same `ResourceManager` snapshot.
5. After those gates, attach the production boss encounter and perform spawned graphical capture before final hitbox/telegraph/VFX/sound tuning.
