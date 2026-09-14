# Riftfrontier Automation Handoff

Recovery aid only. Reconstruct canonical truth from current GitHub `main`, `PROJECT.md`, `docs/CANONICAL.md`, the project standards, data and implementation before acting. This file records the latest useful checkpoint and direction; it is not permission to bypass code/data contracts.

## Current stage

`M3 — PLAYER COMBAT + NATIVE HIT READABILITY BUILD VERIFIED / COMMANDLESS TECHNICAL EXPEDITION LOOP + NATIVE ACTIONBAR READABILITY BUILD VERIFIED / REGION 01 BOSS FIELD ROLE-CONTRAST + HEALTH + REVIEWED ATTACK/REACTION MOTION BUILD VERIFIED / ARENA-PRESSURE MOTION CANDIDATE AWAITING HUMAN REVIEW / PRODUCTION BOSS PRESENTATION STILL MATERIAL + ARENA-PRESSURE ACCEPTANCE + VFX/SOUND GATED`

Prioritize a genuinely playable vertical slice and visible production quality. Do not reopen settled expedition authority/restart work or add speculative lifecycle/authority fences without a demonstrated regression.

## Settled direction — do not redo

- First vertical slice remains `prepare -> deploy -> fight/recover -> extract -> hub/provision -> redeploy` before region expansion.
- Player combat already has two production weapon families, one technique module and server-authoritative attack/loadout state. Human player-combat `PLAYTESTED: NO`, `MULTIPLAYER TESTED: NO`.
- Region 01 first-boss geometry/rig remains Quaternius `Dragon Evolved` through Riftfrontier's custom skinned-mesh importer/renderer. Do not restart model search or reintroduce GeckoLib merely to duplicate this working path.
- Accepted sanitized boss resource: `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`, SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- Canonical provenance/license registry is `docs/THIRD_PARTY_ASSETS.md`. The original Dragon Evolved `Atlas` is source provenance/reference only and is **not** approved final Region 01 art. Do not restore it, invent a final palette, create fake `presentation_assets`, or weaken exact-coverage gates.
- Diagnostic vanilla particles/sounds, technical hub/field blocks, native boss bar, stone-textured field-review body, expedition actionbar and current player-hit cues are temporary validation/readability affordances, not final Riftfrontier UI/art/VFX language.

## First expedition physical loop — latest verified checkpoint

The technical vertical slice no longer requires `/riftfrontier expedition start` to bootstrap the first run in a fresh world.

### Fresh-world hub bootstrap

`ExpeditionHubTerminal.bootstrapFreshWorld(...)` runs from the existing server login adapter only when there is no active authoritative expedition and the authoritative world has no expedition history. It prepares the same bounded technical hub fixture near `(0, 100, 0)`, moves the player there, exposes the temporary smithing-table provision station and lodestone deploy station, and introduces no new SavedData fields, lifecycle state, balance values or station-owned economy.

The hub lodestone delegates directly to `ExpeditionGameplayService.start(...)`; the smithing table delegates directly to `ExpeditionGameplayService.provision(...)`. Existing expedition history prevents fresh-world bootstrap from resetting established state. `/riftfrontier expedition start` remains diagnostic/fallback only.

Earlier bootstrap commits:

- `5f9d6b6bb889a5eab84135437df4150ec80c6494` — fresh-world technical hub bootstrap;
- `19a54bdf9ac5c620099688cbdeac4e1722d3b21d` — login adapter exposure.

Workflow `34808226019`: full SUCCESS. Deliverable digest `sha256:e52d0cae1e0ed30c88b5e945c446c2c739dea013d3ed0b1f60b207781d327c35`.

### Minecraft-native expedition readability checkpoint

Latest gameplay/readability batch adds **localized Minecraft actionbar feedback only**; it does not add a custom HUD, new client-owned state or a second expedition lifecycle.

Authoritative state remains in existing server services. The actionbar only reads and surfaces:

- fresh hub station affordances;
- Region 01 deployment objective progress and run pressure;
- successful salvage progress plus current live patrol threat count;
- field extraction relay availability;
- successful extraction stored salvage / pressure / next supply cost;
- successful provision supply / stored salvage / next deployment cost.

Detailed existing chat messages remain in place. Rejected actions do not emit false success feedback. English and Korean translation keys live in the normal Riftfrontier lang files.

Implementation commits:

- `b763751196bf5bda946c490fdb911bd5640cfbc0` — actionbar feedback integration;
- `fded37cbe91668af213af477ae0b0238ac455693` — correct Minecraft 26.2 server API (`ServerPlayer.sendSystemMessage(Component, true)`).

Verification history matters:

- workflow `34821121084` failed compile because the first implementation used unavailable `ServerPlayer.displayClientMessage(...)`; this was corrected, not hidden;
- workflow `34821369460` attempt 1: `clean test build` SUCCESS, but one existing `attack_hit_window` GameTest failed once;
- the same SHA was rerun without changing combat code because the actionbar batch does not touch that hit path; attempt 2 passed required GameTest, dedicated-server smoke, Xvfb client smoke, executable-JAR inspection, report and artifact uploads.

Successful deliverable:

- artifact: `riftfrontier-0.1.0-alpha.1-deliverables`
- artifact id: `10339290053`
- digest: `sha256:1ffbf1ef086a348052c697bfc983c404da283f3d2ce54db5d32d82063e607659`

Treat `fded37cbe91668af213af477ae0b0238ac455693` as `CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED`. Human actionbar/loop procedure is `docs/M2B_IN_WORLD_HUB_LOOP.md`. `PLAYTESTED: NO`, `MULTIPLAYER TESTED: NO` until humans actually perform those checks.

### Existing connected field/hub behavior preserved

`ExpeditionFieldExtractionRelay` keeps extraction on existing authoritative `ExpeditionGameplayService.extract(...)`; successful physical extraction restores the temporary hub stations. Earlier extraction-relay recovery workflow `34780169928` succeeded.

Post-extraction loop checkpoint `09109a4b8fc11b5c136ff8d329e380fd9fe65eaf`, workflow `34782718258`: full SUCCESS.

No new lifecycle or persistence semantics were added by the fresh-world bootstrap or actionbar batch. Death/logout/restart reconciliation, extraction gates, pressure-scaled preparation cost, storage, supply and evidence remain owned by their existing server-authoritative services.

## Player weapon impact readability — build verified, human field play pending

Checkpoint `24a15b2a0c8db3ade4c6ae730da1373c93e59e00` adds Minecraft-native hit confirmation to the existing player field-impact bridge without adding a second attack clock or trusting client hit claims.

`PlayerWeaponFieldImpactResolver.apply(...)` now emits feedback **only after** the authoritative `hurtServer(...)` call reports successful damage. A successful ACTIVE hit produces a small `DAMAGE_INDICATOR` burst at the target plus `PLAYER_ATTACK_STRONG` through the player sound source. A whiff, closed hit window, duplicate target in the same execution, ineligible target, or rejected damage produces no new success cue.

This follows the same broad readability pattern used by established Minecraft combat mods (server-side impact particles and native attack sounds) while deliberately reusing Minecraft assets rather than inventing Riftfrontier's final weapon VFX/audio language. No external code or art asset was copied into the project, so no new redistributable asset provenance entry is required.

Workflow `34825929915`: full SUCCESS. It passed asset-intake tests, `clean test build`, required GameTest, dedicated-server smoke, Xvfb client smoke, executable-JAR inspection, build report and artifact uploads.

Successful deliverable:

- artifact: `riftfrontier-0.1.0-alpha.1-deliverables`
- artifact id: `10340393261`
- digest: `sha256:58f5e65db6e7cba6ae2d932a8984e1acf738836b3b37a75445f4abe366d2cdbc`

Treat `24a15b2a0c8db3ade4c6ae730da1373c93e59e00` as `CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED`. `PLAYTESTED: NO`, `MULTIPLAYER TESTED: NO`.

Exact human field check for this checkpoint:

1. Use a disposable client world and bind both Riftfrontier combat actions in Minecraft Controls; no final default key layout has been approved.
2. Enter the normal commandless Region 01 loop through the hub lodestone so real patrol targets are present.
3. Issue `/riftfrontier weapon mobile`, hold the issued iron-sword carrier, and exercise both combat actions against a patrol target. Repeat with `/riftfrontier weapon reach`.
4. Confirm a successful server-authoritative hit produces one concise damage-particle burst and one native strong-attack sound at the struck target.
5. Deliberately whiff outside the target volume and confirm there is no false hit-confirmation cue.
6. Confirm the cue does not repeat on the same target throughout one ACTIVE execution and does not appear during TELEGRAPH or RECOVERY.
7. Repeat with `/riftfrontier weapon mobile pivot` and `/riftfrontier weapon reach pivot`; recovery-pivot behavior must remain unchanged and must not itself create a hit cue.
8. Do not approve particle count, sound, pitch, damage, geometry or key layout as final balance/presentation from automation alone. Record whether the cue makes the existing two weapon roles easier to read without masking spacing/recovery differences.

## Region 01 boss field harness — verified technical behavior

Development-only commands:

```text
/riftfrontier boss fieldtest spawn
/riftfrontier boss fieldtest phase1
/riftfrontier boss fieldtest phase2
```

The actor remains excluded from natural spawning and production Region 01 encounter composition.

Existing server-owned field behavior includes production semantic profile `riftfrontier:boss/region_01_first_apex`, phase 1 committed strike + line displacement, phase 2 arena pressure, authoritative TELEGRAPH -> ACTIVE -> RECOVERY, ACTIVE-only diagnostic damage (`1.0F`) with once-per-execution target dedupe, attack-start target commitment, provisional role-specific hit geometry, ACTIVE-only line travel, one-shot arena radial impulse, diagnostic explosion cue, and native health-backed boss bar. Diagnostic values/cues are not final production tuning/art.

Arena-pressure cue checkpoint `d47a0229e922b0f01a2258bd47c8db3c0878fffa`, workflow `34791308953`: full SUCCESS.
Boss-health checkpoint `9081a074347d041f687ca5f84478a0b575bcb13b`, workflow `34788624295`: full SUCCESS.

## Dragon Evolved field-review presentation — verified, still temporary

The production renderer still fails closed until complete reviewed semantic animation coverage plus a reviewed final material exist. While production publication is unavailable, `Region01BossFieldReviewRenderPreview` exposes accepted sanitized Dragon geometry with Minecraft stone as an intentionally temporary material. Production submission always has priority and prevents duplicate preview drawing.

Visible-body checkpoint `ad273f3bd17fb307994c672972349eb4e05426ef`, workflow `34794765872`: full SUCCESS.

### Reviewed source attack motion

Only evidence-backed source attack roles are sampled from server-synced semantic phase progress:

- `region_01_committed_strike -> Punch`: TELEGRAPH `0.0–0.2`, presentational ACTIVE `0.2–0.275`, RECOVERY `0.275–1.0`;
- `region_01_line_displacement -> Headbutt`: TELEGRAPH `0.0–0.15555555555555556`, presentational ACTIVE `0.15555555555555556–0.2`, RECOVERY `0.2–0.4888888888888889`.

These windows do not authorize damage timing, hit geometry, movement or target admission.

Attack-motion checkpoint `1683cc426891ce2907e66fece520ffc903b5c2aa`, workflow `34797829558`: full SUCCESS.

### Reviewed reaction/death motion

Checkpoint `304fc8317ba1af42463a34b2b1ad1871ff4b57b9` adds field-review sampling for directly observed Dragon Evolved `HitReact` and `Death` without assigning either to an attack role.

Presentation priority remains `terminal Death > reviewed authoritative attack sample (Punch / Headbutt) > HitReact > neutral Flying_Idle`.

Workflow `34804887061`: full SUCCESS. Deliverable digest `sha256:6d2edd95f5ece1edf46996a3eda552d55bc0739c338b1ca2db26cf8bf8ad48c3`.
Human procedure: `docs/M3_REGION01_BOSS_VISIBLE_FIELD_PREVIEW.md`.

### Arena-pressure authored field-motion candidate — automated build verified, human acceptance pending

The latest candidate deliberately does **not** relabel an unrelated Dragon Evolved source clip as arena pressure. `Region01BossArenaPressureFieldMotionCandidate` applies a presentation-only whole-body silhouette compression/burst/recovery driven by the server-synced `patternId / attackPhase / phaseProgress`; gameplay movement, collision, damage, targeting and hit geometry remain unchanged.

Candidate shape:

- TELEGRAPH: horizontal `1.00 -> 0.94`, vertical `1.00 -> 1.04`;
- ACTIVE entry: broad/low burst around horizontal `1.12`, vertical `0.94`, then ease toward `1.08 / 0.97`;
- RECOVERY: return from about `1.08 / 0.97` to `1.00 / 1.00`.

This remains field-review-only. It does **not** close production 9/9 animation coverage and must not be promoted automatically.

Checkpoint `bca0816381c09b28d613bc12d2e395eb62368761`, workflow `34812925676`: full SUCCESS. Deliverable digest `sha256:9ab57cfec3a13f69c37b6d6732f126b9371eaf0b05d5e5fb32f7e39a5bf772f0`.

Human decision procedure: `docs/M3_REGION01_ARENA_PRESSURE_MOTION_REVIEW.md` using:

```text
/riftfrontier boss fieldtest spawn
/riftfrontier boss fieldtest phase2
```

Accept only if it reads as controlled radial-pressure anticipation -> one burst -> recovery at Minecraft scale. Reject rubbery/comic deformation or misleading range. Until that human decision exists, production binding remains incomplete.

## Production presentation state and exact next boundary

Do not grow the technical hub/actionbar/backend merely because it is now easier to play. The main automatic development boundary returns to **production-visible Region 01 quality**.

The packaged boss still has no approved final texture/material. `Region01BossMaterialPreparation` requires a real reviewed resource through the existing integrity/review contract. The original Dragon Evolved Atlas is forbidden as a final shortcut.

Production staged attack source binding remains intentionally incomplete because the arena-pressure candidate has not been human-accepted/authored into a reviewed production source-motion contract.

Next automatic development should target work that does not fake that acceptance:

1. Establish a legally usable, commercial-game/major-mod-reference-reviewed final material/texture direction and physical resource, preserving provenance/license records; or obtain actual human acceptance/rejection evidence for the arena-pressure candidate when available.
2. Only when all nine attack logical keys have evidence, assemble complete production `BossAnimationSourceBinding` without weakening exact coverage.
3. Select/author VFX and sound under the same readability/provenance rules; presentation channels may emphasize but never create a second gameplay hit clock. The current Minecraft-native player hit cue is only a field-readability baseline, not approval of final Riftfrontier combat audiovisual language.
4. Author the first real `presentation_assets` manifest only after every referenced physical resource exists and is reviewed.
5. Publish through the existing reload/render pipeline, then verify Minecraft scale/hit-volume readability and resource reload.
6. Only after verified production presentation replaces the temporary path, remove field-review render/motion/reaction scaffolding and stone-material assumption as superseded code.
7. Only after presentation/field evidence clears the gate should production Region 01 encounter insertion and final damage/geometry tuning be considered.

Do **not** auto-tune provisional combat numbers without human field evidence. Do **not** promote native diagnostic cues, stone material, bossbar, actionbar, lodestone, smithing table or technical hub platform as final art/UI.

## Verification vocabulary — latest gameplay/readability code checkpoint

For `24a15b2a0c8db3ade4c6ae730da1373c93e59e00` after workflow `34825929915`:

- `CODE REVIEWED`: YES
- `TESTED`: YES
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO

Unit tests, GameTest, dedicated-server smoke and Xvfb client smoke never count as human play or multiplayer field testing.