# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Canonical design documents and current GitHub `main` remain authoritative.

## Last recovered baseline

- Re-read current remote `main`, then `AGENTS.md` → `docs/BUILD_STANDARD.md` → `docs/QUALITY_STANDARD.md` → `PROJECT.md` → `CANONICAL.md` → `GAME_DESIGN_MASTER.md` → `CONTENT_ARCHITECTURE.md` → `ROADMAP.md` and the current automation handoff.
- Resolver commit `7b5fb9f11c303b3be4cc4d06698db545fefe8a48` is confirmed by `Build Riftfrontier` run `34197669794` as full `SUCCESS`.
- This batch continued from the exact next boundary and did not rebuild attack timing, damage, boss lifecycle, semantic networking, resolver behavior, M2 expedition runtime, or field-play-gated balance values.

## Completed in this batch

M3 atomic boss-presentation publication boundary:

- `ContentRuntime` now validates core content and `BossPresentationProfile` candidates before generation advancement/publication.
- `ContentRuntimeSnapshot` now owns the immutable validated presentation-profile list and its `BossPresentationResolver` in the same generation as the core `ContentRegistry`/catalog.
- Existing two-argument `installValidated` remains compatible and publishes an empty presentation set for bootstrap paths that do not yet author final boss presentation data.
- `ContentServerReloadListener` now loads deterministic `data/riftfrontier/riftfrontier/presentation/*.json` candidates with `BossPresentationProfileCodec`, then publishes core content + presentation profiles through one atomic snapshot.
- Presentation JSON is optional until authored production profiles exist; invalid authored profiles fail the reload and preserve the last-known-good generation.
- Added JUnit coverage proving presentation profiles resolve from the same published generation and that an invalid presentation candidate does not replace the previous snapshot or advance generation.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/content/ContentRuntime.java`
- `src/main/java/kr/moonseungjun/riftfrontier/content/ContentRuntimeSnapshot.java`
- `src/main/java/kr/moonseungjun/riftfrontier/content/ContentServerReloadListener.java`
- `src/test/java/kr/moonseungjun/riftfrontier/content/ContentRuntimeTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Previous resolver run `34197669794`: full Riftfrontier workflow `SUCCESS`.
- Atomic publication code commit: `e43f39a052845b202c662e773e0cec751cd3f8a2`.
- `Build Riftfrontier` run `34202374935`: IN PROGRESS at handoff update; setup reached the build job, but clean build/GameTest/server/client/JAR gates were not yet complete. Do not claim this batch fully verified until that exact run is checked.
- Actual multiplayer boss semantic packet reception/rendering: NOT TESTED.
- Final production Region 01 boss entity integration: NOT IMPLEMENTED / NOT TESTED.
- Actual human Minecraft field play: NOT TESTED.
- Final model/animation/VFX/sound assets: NOT DESIGNED / NOT TESTED by design gate.

## Do not repeat or revert

- Preserve `AttackPattern` as the single authoritative combat timing source.
- Preserve ACTIVE-only damage, per-execution target deduplication, server-owned boss phase cancellation, and deterministic attack-selection seam.
- Preserve `PresentationFrame` → `BossPresentationSemanticState` as the timing-free semantic contract and monotonic client ordering/clear watermark.
- Preserve presentation resolver fail-closed behavior; never guess missing asset keys or invent client timing.
- Preserve atomic publication: invalid core OR presentation candidates must leave the complete previous snapshot untouched.
- Do not tune M2 pressure/patrol values without field-play evidence or promote M2 technical proxies to production art/AI.
- Logical asset keys are not proof that final assets exist.

## Exact next start point

1. Re-check current remote `main` and finish verification of run `34202374935`; fix the first real failure if present.
2. If fully green, add the next meaningful resource-boundary gate: validate actual selected model/animation/VFX/sound resource existence + expected namespace/type against the authored presentation snapshot while retaining last-known-good atomic behavior. Do not fabricate final assets merely to satisfy the validator.
3. Before selecting real assets, re-read `M3_COMBAT_REFERENCE_DOSSIER.md`, `REFERENCE_TARGETS.md`, and `THIRD_PARTY_ASSETS.md`; record provenance/license/26.2 compatibility.
4. After real presentation assets and production boss entity boundaries exist, wire snapshot resolver output to that entity/client presentation path and verify visual-hit timing in Minecraft.
5. Keep M2 numerical tuning and final presentation completion gated on human field-play/reference evidence.
