# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Canonical design documents and current GitHub `main` remain authoritative.

## Last recovered baseline

- This session re-read current remote `main`, then `AGENTS.md` → `docs/BUILD_STANDARD.md` → `docs/QUALITY_STANDARD.md` → `PROJECT.md` → `CANONICAL.md` → `GAME_DESIGN_MASTER.md` → `CONTENT_ARCHITECTURE.md` → `ROADMAP.md` → `REFERENCE_TARGETS.md` before this handoff.
- Previous semantic presentation sync run `34193512087` for `6e3637ea5fe054986e90116278ac47fd420608c1` is confirmed `SUCCESS`.
- This batch continued from the exact next boundary: Cobblemon-style data-driven boss presentation resolution. It did not rebuild authoritative attack timing, Minecraft damage, boss lifecycle, network ordering or client cache semantics.
- M2 field-play-gated values and all non-Riftfrontier project folders remain untouched.

## Completed in this batch

M3 logical boss presentation profile + codec + validator + resolver boundary:

- Added immutable `BossPresentationProfile` keyed by stable `bossProfile + variant` context.
- Presentation bindings are selected only by authoritative semantic tuple `presentationCue + delivery + attack phase`.
- Logical model/animation/VFX/sound keys use validated `namespace:path` identifiers. No final Region 01 art asset is claimed or invented by this layer.
- Added strict JSON `BossPresentationProfileCodec` for `boss_presentation_profile` documents and duplicate-selector rejection.
- Added `BossPresentationProfileValidator` that checks boss linkage, duplicate boss/variant contexts and full TELEGRAPH/ACTIVE/(non-zero) RECOVERY binding coverage for every referenced attack pattern.
- Added immutable `BossPresentationResolver`. Missing variant/profile/selector fails closed with no resolved presentation; it never falls back to guessed assets.
- Resolver passes through authoritative phase progress and hit-window semantics and owns no independent cadence/timer.
- Added JUnit coverage for exact semantic resolution, fail-closed missing contexts/selectors, required authored coverage, duplicate selectors/contexts and logical key parsing.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationProfile.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationProfileCodec.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationProfileValidator.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationResolver.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/presentation/BossPresentationResolverTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Previous run `34193512087`: full Riftfrontier workflow `SUCCESS`.
- New presentation resolver batch: CI NOT YET VERIFIED at the time this handoff blob was prepared; inspect the workflow for the exact code commit before claiming success.
- Actual multiplayer boss semantic packet reception/rendering: NOT TESTED.
- Final production Region 01 boss entity integration: NOT IMPLEMENTED / NOT TESTED.
- Actual human Minecraft field play: NOT TESTED.
- Final model/animation/VFX/sound assets: NOT DESIGNED / NOT TESTED by design gate.

## Do not repeat or revert

- Do not rebuild M2 extraction/restart/evidence infrastructure or tune M2 pressure/patrol values without field-play evidence.
- Preserve `AttackPattern` as the single authoritative combat timing source.
- Preserve ACTIVE-only damage and per-execution target deduplication.
- Preserve server-owned `BossCombatController`, phase-transition cancellation and deterministic attack-selection seam.
- Preserve `PresentationFrame` → `BossPresentationSemanticState` as the timing-free server/client semantic contract.
- Preserve monotonic per-entity server tick ordering and clear watermark on the client.
- Keep client-only registration isolated from dedicated-server class loading.
- Preserve presentation resolver fail-closed behavior; never guess a missing asset key or invent client timing.
- Logical asset keys are not proof that final assets exist. Do not claim model/VFX/audio completion until reference/license/compatibility and actual in-game visual gates pass.

## Exact next start point

1. Re-check current remote `main`, identify the workflow run targeting this presentation-resolver code commit and fix the first real compile/test/runtime failure if any.
2. Once fully green, integrate authored boss presentation profiles into the existing atomic ResourceManager/content publication path rather than loading ad-hoc client JSON. Preserve last-known-good/fail-closed reload behavior.
3. Add asset-existence/namespace validation at the resource boundary when actual model/animation/VFX/sound files are selected; logical-key validity alone is not the final asset gate.
4. Re-read `M3_COMBAT_REFERENCE_DOSSIER.md` and `THIRD_PARTY_ASSETS.md` before selecting actual GeckoLib/model/VFX/audio assets.
5. Wire final presentation resolution to a production Region 01 boss entity only after the production entity/reference boundary is authored. Do not promote M2 technical proxies.
6. Keep M2 numerical tuning and actual presentation completion gated on human field-play/reference evidence.
