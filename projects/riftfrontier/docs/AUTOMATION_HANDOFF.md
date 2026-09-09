# Riftfrontier Automation Handoff

This file is a recovery aid for scheduled development sessions. Current GitHub `main` and canonical design documents remain authoritative.

## Last recovered baseline

- Remote `main` verified at this run start: `485767e310ef472728dbb972cd7d68570d1ddb28`.
- Current implementation HEAD for this batch: `d8d9b5b6717067227d6e62f6678250a5f50dba9f`.
- `Dragon Evolved` remains the selected, sanitized and packaged Region 01 first-boss geometry/rig/unaltered-source-animation resource. Source `Atlas` art remains explicitly unapproved and excluded.
- Exact accepted runtime derivation SHA-256 remains `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.

## Completed in this batch

M3 Region 01 server boss semantic authoring gate:

- Added `BossCombatSemanticProfile`, a strict non-numeric authoring boundary that scopes already-authored `attack_pattern` IDs to explicit boss phases. It owns no damage, tick timing, hit volume, cooldown, weight or visual asset path.
- Added strict JSON decoding for `boss_profile` + `phase_attack_patterns`; missing/empty/duplicate/malformed phase pools fail closed instead of receiving defaults.
- Added `ValidatedBossCombatSemantics`, a non-forgeable runtime validation capability that requires exact `1..phaseCount` coverage, rejects attacks outside the existing `BossProfile`, resolves every referenced `AttackPattern`, and requires TELEGRAPH/ACTIVE/RECOVERY logical animation-key coverage from the existing `BossPresentationProfile`.
- Added `BossAttackSelectionPolicy.authoredPhases(...)`. Only a validated semantic capability can phase-scope server attack selection; the existing controller candidate set remains a second fail-closed boundary.
- Existing `BossProfile`, `AttackPattern`, their timing values, deterministic baseline selector, server authority, and presentation/source-binding gates were preserved unchanged.
- No Region 01 production attack timing/damage, encounter attachment, final material, hitbox, VFX/sound, or field-balance value was invented.

## Changed systems/files

- `src/main/java/kr/moonseungjun/riftfrontier/combat/BossCombatSemanticProfile.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/ValidatedBossCombatSemantics.java`
- `src/main/java/kr/moonseungjun/riftfrontier/combat/BossAttackSelectionPolicy.java`
- `src/test/java/kr/moonseungjun/riftfrontier/combat/BossCombatSemanticProfileTest.java`
- `docs/AUTOMATION_HANDOFF.md`

## Verification

- Implementation HEAD `d8d9b5b6717067227d6e62f6678250a5f50dba9f`, `Build Riftfrontier` run `34410900884`.
- PASS so far: Java 25 toolchain verification, asset-intake tests, JUnit + clean build, required native GameTest, dedicated-server smoke.
- Xvfb graphical client smoke: IN PROGRESS at handoff update time. Executable-JAR inspection, report and artifact upload: PENDING. Do not call the run FULL SUCCESS unless a later session confirms completion.
- Local clone/build: NOT RUN successfully in this session because the container could not resolve `github.com`; CI is the actual validation source.
- Production Region 01 `attack_pattern` numeric cadence/damage values and final `boss_profile`: NOT AUTHORED; `region_01.json` currently contains no production boss/attack definitions.
- Production `BossCombatSemanticProfile` data: NOT AUTHORED because there are no legitimate production attack IDs/timings to bind yet.
- Production reviewed `BossAnimationSourceBinding.reviewed(...)`: NOT AUTHORED until legitimate server semantic keys are present.
- Approved final material/texture/`RenderType`: NOT IMPLEMENTED / NOT APPROVED.
- Actual encounter attachment, final dimensions/hitbox/scale, spawned/deformed Dragon graphical capture, VFX/sound and human field-play: NOT IMPLEMENTED / NOT TESTED.

## Do not repeat or revert

- Exact Dragon source reacquisition, sanitizer acceptance, runtime packaging, eight-clip motion review, `Headbutt`/`Punch` source-window review, typed geometry preparation, typed reviewed-animation preparation and renderer publication provenance gates are DONE.
- Preserve `BossCombatSemanticProfile` as a non-numeric phase-selection layer; do not smuggle damage/timing/weights/cooldowns into it or collapse it back into visual source-clip inference.
- Preserve `ValidatedBossCombatSemantics` logical-key coverage against `BossPresentationProfile` and the controller's independent candidate-set check.
- Preserve source SHA-256 `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`, accepted derivation SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`, and provenance SHA-256 `3e16877a0043cf980ac8de05bb518834c5e77bd72d96103b4984c65a2a5a4c6c`.
- Never infer gameplay semantics from Dragon clip names/durations/channel metrics; source `ACTION` is not Minecraft damage authorization.
- Preserve exact `ResourceManager` staging, exact prepared mesh/animation identity, UUID actor identity, monotonic server ticks, ACTIVE-only authoritative damage, four-influence LBS and arbitrary triangle topology.
- Do not allow source `Atlas` bytes into production, restore GeckoLib/rigid-cube approximation, attach natural/encounter spawning, or invent final dimensions/hitbox/balance timing without evidence.

## Exact next start point

1. Re-check remote `main` and first resolve `Build Riftfrontier` run `34410900884`; if any remaining gate failed, fix the first real failure before new feature work.
2. Re-read canonical docs and this handoff. Do not redo Dragon art/motion/preparation gates or the new boss semantic schema/validator.
3. Inspect current production content authoring sources for a legitimately approved Region 01 `attack_pattern`/`boss_profile`. If none exist, do not copy fixture timing values or invent combat cadence.
4. If legitimate production attack IDs/timings now exist, author the production `BossCombatSemanticProfile`, validate phase pools + logical animation keys through `ValidatedBossCombatSemantics`, wire the controller to `BossAttackSelectionPolicy.authoredPhases(...)`, then construct reviewed source bindings only through existing motion/window evidence gates.
5. If production combat timing remains evidence-blocked, move to the next objective seam: approved material/texture/`RenderType` provenance. Source `Atlas` remains prohibited; do not improvise visual treatment.
6. Only after legitimate server semantics + reviewed animation + approved material share one current reload should actual spawned-boss graphical capture precede encounter attachment, hitbox/telegraph tuning, VFX/sound or balance work.
