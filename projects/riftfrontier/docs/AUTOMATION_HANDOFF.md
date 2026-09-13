# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main`, `PROJECT.md`, `docs/CANONICAL.md`, project standards, production data and source are authoritative.

## Current stage

`M3 — PLAYER COMBAT BUILD VERIFIED / REGION 01 BOSS FIELD LINE-CHARGE BUILD VERIFIED / FIRST EXPEDITION PHYSICAL EXTRACTION VERIFIED / POST-EXTRACTION HUB LOOP IMPLEMENTED / DRAGON ATTACK SOURCE MAPPING PARTIALLY REVIEWED`

Prioritize the playable vertical slice and visible production quality. Do not reopen settled expedition authority/restart work or add lifecycle fences without a demonstrated regression.

## Settled direction — do not redo

- First vertical slice must close a real `prepare -> deploy -> fight/recover -> extract -> hub/provision -> redeploy` loop before expanding regions.
- Player combat already has two production weapon families, one technique module and server-authoritative attack/loadout state. Human player-combat PLAYTESTED and MULTIPLAYER TESTED remain NO.
- Region 01 first-boss geometry/rig remains Quaternius `Dragon Evolved`, using Riftfrontier's custom skinned-mesh importer/renderer. Do not restart candidate search or reintroduce GeckoLib merely to duplicate this path.
- Accepted sanitized boss resource: `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`, SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- Canonical third-party registry is `docs/THIRD_PARTY_ASSETS.md`. Original Dragon Evolved `Atlas` art is provenance/reference only and is not approved final production art. Do not restore it, invent a final palette, create fake `presentation_assets`, or weaken exact-coverage gates.

## First expedition loop

### In-world field extraction — verified automation checkpoint

Commit `2ed3db36a7d374952d2e0cf209044fdd68aeded5` introduced `ExpeditionFieldExtractionRelay`: an owned deployed Region 01 run can materialize a temporary lodestone at `technicalRegionCenter + (0, 0, 5)`, and right-clicking it routes into the existing authoritative `ExpeditionGameplayService.extract(...)` path.

The initial pure-JUnit fixture mistake was removed by `5d4614eb933c127f7e8d6409a3e26810805f8463` rather than widening the unit-test classpath. Recovery workflow `34780169928` is now observed **completed / SUCCESS**. Treat that extraction-relay checkpoint as `CODE REVIEWED / TESTED / BUILD VERIFIED`; the workflow also reached its executable-JAR/deliverable stages, but human play and multiplayer play remain NO.

Human extraction checklist: `docs/M2B_IN_WORLD_EXTRACTION_RELAY.md`.

### Post-extraction hub loop — current implementation batch

`ExpeditionHubTerminal` closes the visible loop after an authoritative field extraction without duplicating resource/lifecycle authority:

- successful extraction still runs through `ExpeditionGameplayService.extract(...)`, including objective gate, retention, patrol bonus, pressure/world response and terminal evidence;
- after the service returns the player to the bounded technical hub, a temporary **smithing table** is restored two blocks west of hub center and a temporary **lodestone** two blocks east;
- smithing-table interaction delegates to the existing `ExpeditionGameplayService.provision(...)` operation;
- hub-lodestone interaction delegates to the existing `ExpeditionGameplayService.start(...)` operation;
- authoritative rejection is surfaced to the player instead of being reimplemented by the station layer;
- the first run may still be bootstrapped with `/riftfrontier expedition start`; after the first successful extraction the player can repeat `hub provision -> deploy -> field recover/fight -> extract -> hub` through block interaction;
- the vanilla station blocks are technical vertical-slice affordances only, not final hub art/UI/station language.

Human checklist: `docs/M2B_IN_WORLD_HUB_LOOP.md`.

Do not add a second hub economy/lifecycle state machine. The physical stations must remain thin adapters over the existing authoritative services.

## Region 01 boss field harness

Development-only commands remain:

- `/riftfrontier boss fieldtest spawn`
- `/riftfrontier boss fieldtest phase1`
- `/riftfrontier boss fieldtest phase2`

The actor remains excluded from natural spawning and production encounter composition. It uses role-specific provisional geometry, `1.0F` diagnostic damage, ACTIVE-only server damage, once-per-execution dedupe, attack-start target commitment and temporary vanilla-particle readability instrumentation.

Production `region_01_line_displacement` is a `line_charge`; the field harness advances only during authoritative ACTIVE at provisional `activeForwardStep = 0.5` blocks/tick along committed facing. TELEGRAPH/recovery do not travel and the attack does not home after start. This value is not final balance.

Checkpoint `c461c65f67fb95c1b4c0808ff68b9a585234ea9e`, CI `34776775338`: full workflow SUCCESS through tests/build, required GameTest, dedicated-server smoke, Xvfb client smoke, executable-JAR inspection and deliverable upload. That batch is `CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED`; human and multiplayer play remain NO.

Human boss checklist: `docs/M3_REGION01_BOSS_FIELD_PLAY.md`.

## Production boss semantics and source animation evidence

Production profile: `riftfrontier:boss/region_01_first_apex`.

- phase 1: `region_01_committed_strike` + `region_01_line_displacement`
- phase 2: both above + `region_01_arena_pressure`

Reviewed Dragon Evolved mappings are motion-evidence-based, not clip-name guesses:

- `region_01_committed_strike` -> `Punch`: TELEGRAPH `0.0–0.2`, presentational ACTIVE `0.2–0.275`, RECOVERY `0.275–1.0`
- `region_01_line_displacement` -> `Headbutt`: TELEGRAPH `0.0–0.15555555555555556`, presentational ACTIVE `0.15555555555555556–0.2`, RECOVERY `0.2–0.4888888888888889`

These windows never authorize server damage timing, hit geometry, movement or target admission.

`region_01_arena_pressure` still has no accepted source motion with exact reviewed phase windows. Do not recycle `Fast_Flying`, `Flying_Idle`, `Yes`, `No`, `HitReact`, or `Death` merely to reach 9/9 coverage. Staged source binding intentionally remains incomplete at six logical animation keys.

## Presentation/provenance locks

The packaged Region 01 boss contains the sanitized GLTF but no approved final texture resource. `Region01BossMaterialPreparation` requires a real reviewed material texture through the existing resource/atlas contract.

- Reuse selected Dragon Evolved rig/geometry unless evidence requires changing it.
- External assets require redistribution-compatible licensing, immutable source/revision/hash where practical, modification notes and actual usage records.
- Use strong commercial-game/major-mod references for presentation decisions, but do not copy protected assets, UI, balance values or proprietary code.
- Diagnostic `CLOUD` / `CRIT` / `SMOKE` particles are instrumentation, not final VFX.
- Only a coherent physical resource set may create `presentation_assets` and publish the staged logical boss profile.

## Exact next development boundary

1. Verify the current post-extraction hub-loop commit with the normal Riftfrontier workflow. If green, record `CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED`; never infer human play from CI.
2. Use `docs/M2B_IN_WORLD_HUB_LOOP.md` for human field play. Do not rebalance or redesign the temporary hub stations before evidence from that run.
3. After this connected loop checkpoint, automatic work should return to **visible Region 01 production quality**: evidence-backed arena-pressure motion or the approved material/texture/VFX/sound set with provenance and Minecraft-scale readability.
4. Once all nine animation logical keys have evidence, assemble the full production `BossAnimationSourceBinding` without weakening exact coverage.
5. Do not add more expedition authority/lifecycle fences unless a real regression is demonstrated.
6. `PLAYTESTED: NO`. `MULTIPLAYER TESTED: NO`. CI, GameTest, dedicated-server smoke and Xvfb client smoke never count as either.
