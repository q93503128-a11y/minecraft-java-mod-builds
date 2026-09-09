# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` verified at this run start: `06008b744cde23fbafa286123aba75ed172014dc`.
- Latest verified Riftfrontier implementation/test HEAD: `b38ac52559538df75c2331833b5a08d0ff32f180`.
- `Dragon Evolved` remains the selected, sanitized and packaged Region 01 first-boss geometry/rig/unaltered-source-animation resource. Source `Atlas` art remains explicitly unapproved and excluded.
- Exact accepted runtime derivation SHA-256 remains `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.

## Completed in this batch

M3 Region 01 boss fine source-motion phase-window evidence gate:

- Recovered the exact accepted runtime glTF from the previously verified executable-JAR artifact and independently rechecked its SHA-256 against the accepted derivation before inspection.
- Re-sampled `Headbutt` and `Punch` at their native animation-key cadence using the accepted skinned mesh plus articulated skeleton rather than clip-name inference.
- Added `assets/sources/region_01_boss_dragon_evolved.phase_window_review.json` with source-motion-only boundaries at actual source keyframes.
- Reviewed `Headbutt`: ANTICIPATION `0..7/45`, ACTION `7/45..9/45`, RECOVERY `9/45..22/45`. Frame 8 is the peak reviewed head/torso displacement and frame 9 starts recovery.
- Reviewed `Punch`: ANTICIPATION `0..8/40`, ACTION `8/40..11/40`, RECOVERY `11/40..1`. Frame 10 is the peak reviewed distal-forelimb extension and frame 11 starts sustained recovery.
- These are source-motion segments only. `ACTION` explicitly does NOT authorize a Minecraft ACTIVE damage window or define `attack_pattern` tick balance.
- Added `BossAnimationPhaseWindowReview`: exact clip + exact `ClipWindow` evidence is required; overlapping, numerically altered, wrong-clip or duplicate source-motion segment claims fail closed.
- Added `BossAnimationSourceBinding.reviewed(...)` and `requireReviewedPhaseWindows()`. Production phase-partitioned bindings can now require both whole-clip `BossAnimationMotionReview` and exact fine-window review. Legacy explicit-window constructors remain non-production/general compatibility paths and expose no reviewed-window capability.
- Added Java regression coverage and Python asset-intake validation pinning the review receipt to the exact accepted runtime resource/hash.
- No server boss timing, hit volume, logical animation key, material, texture, encounter, hitbox, VFX, sound or balance value was guessed.

## Changed systems/files

- `assets/sources/region_01_boss_dragon_evolved.phase_window_review.json`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossAnimationPhaseWindowReview.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossAnimationSourceBinding.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/BossAnimationPhaseWindowReviewTest.java`
- `tools/tests/test_region01_boss_phase_window_review.py`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Local Python receipt test: 3/3 PASS against exact accepted runtime glTF extracted from the verified prior deliverable.
- Local Java compile: NOT RUN successfully because this execution environment only has Java 21 while the project/JAR requires Java 25; no success was claimed from that environment.
- Implementation/test HEAD `b38ac52559538df75c2331833b5a08d0ff32f180`, `Build Riftfrontier` run `34399727213`: FULL SUCCESS.
- CI passed toolchain verification, asset-intake tests, Java 25 JUnit + clean build, required native GameTest, dedicated-server smoke, Xvfb client smoke, executable-JAR inspection, report generation, deliverable upload and logs/report upload.
- Exact production server-authoritative Region 01 boss `attack_pattern` / `boss_profile` / presentation logical keys: NOT AUTHORED.
- Production reviewed logical animation binding constructed from the new fine windows: NOT AUTHORED because the server semantic keys/timing are not yet authored.
- Same-`ResourceManager` staging/publication of reviewed animation binding: NOT IMPLEMENTED.
- Approved final material/texture/`RenderType`, actual encounter attachment, final dimensions/hitbox/scale, spawned/deformed Dragon graphical capture, VFX/sound and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Exact Dragon source reacquisition, deterministic sanitizer acceptance, runtime packaging, eight-clip visual motion review and the new native-keyframe `Headbutt`/`Punch` source-window review are DONE.
- Preserve source SHA-256 `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`, accepted derivation SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`, and provenance SHA-256 `3e16877a0043cf980ac8de05bb518834c5e77bd72d96103b4984c65a2a5a4c6c`.
- Preserve `BossAnimationMotionReview`, `BossAnimationPhaseWindowReview`, exact source-window evidence and server-authoritative phase progress. Never infer gameplay semantics from clip names/durations/channel metrics and never treat source `ACTION` as damage authorization by itself.
- Preserve exact-source/structure/inventory gates, staged exact-`ResourceManager` transaction, `Region01BossGeometryPreparation`, exact prepared `SkinnedMeshAsset` identity publication, UUID actor identity, monotonic server ticks, ACTIVE-only authoritative damage, four-influence LBS and arbitrary triangle topology.
- Do not allow source `Atlas` material/texture/image bytes into production. Do not restore GeckoLib/rigid-cube approximation as a shortcut.
- Do not attach the boss to encounter/natural spawning or invent final dimensions/hitbox/balance timing without authored server semantics and field/visual evidence.

## Exact next start point

1. Re-check remote `main`, canonical docs, M3 combat/presentation code, `REFERENCE_TARGETS.md`, `THIRD_PARTY_ASSETS.md`, both Dragon motion-review receipts, then this handoff.
2. Do not redo source animation review. Treat the reviewed `Headbutt` and `Punch` windows above as source-motion evidence, not gameplay timing.
3. Inspect the existing M3 combat/content schema and determine whether a defensible Region 01 server boss semantic contract can be authored without inventing balance timing. If timing still requires field-play evidence, do not guess it.
4. In that case, take the next objective implementation seam instead: add a typed production animation-preparation capability tied to the current `Region01BossGeometryPreparation.PreparedGeometry` / `ValidatedReload`, and require `BossAnimationSourceBinding.requireReviewedPhaseWindows()` before a windowed binding can enter renderer publication.
5. Once server-authoritative boss selectors/logical animation keys are legitimately authored, construct bindings only through `BossAnimationSourceBinding.reviewed(...)`, map authoritative phase progress through the reviewed source windows, and keep material/texture/`RenderType` as an independent fail-closed prerequisite.
6. Publish geometry + reviewed windowed animation binding + approved material atomically from one current resource reload, then perform actual spawned-boss graphical capture before encounter attachment, final hitbox/telegraph tuning, VFX/sound or balance work.
