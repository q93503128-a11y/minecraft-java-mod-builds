# Riftfrontier Automation Handoff

Recovery aid only. Reconstruct canonical truth from current GitHub `main`, `PROJECT.md`, `docs/CANONICAL.md`, the project standards, data and implementation before acting. This file records the latest useful checkpoint and direction; it is not permission to bypass code/data contracts.

## Current stage

`M3 — PLAYER COMBAT BUILD VERIFIED / FIRST EXPEDITION PHYSICAL LOOP BUILD VERIFIED / REGION 01 BOSS FIELD ROLE-CONTRAST + HEALTH + REVIEWED ATTACK/REACTION MOTION BUILD VERIFIED / PRODUCTION BOSS PRESENTATION STILL MATERIAL + ARENA-PRESSURE + VFX/SOUND GATED`

Prioritize a genuinely playable vertical slice and visible production quality. Do not reopen settled expedition authority/restart work or add speculative lifecycle/authority fences without a demonstrated regression.

## Settled direction — do not redo

- First vertical slice remains `prepare -> deploy -> fight/recover -> extract -> hub/provision -> redeploy` before region expansion.
- Player combat already has two production weapon families, one technique module and server-authoritative attack/loadout state. Human player-combat `PLAYTESTED: NO`, `MULTIPLAYER TESTED: NO`.
- Region 01 first-boss geometry/rig remains Quaternius `Dragon Evolved` through Riftfrontier's custom skinned-mesh importer/renderer. Do not restart model search or reintroduce GeckoLib merely to duplicate this working path.
- Accepted sanitized boss resource: `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`, SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- Canonical provenance/license registry is `docs/THIRD_PARTY_ASSETS.md`. The original Dragon Evolved `Atlas` is source provenance/reference only and is **not** approved final Region 01 art. Do not restore it, invent a final palette, create fake `presentation_assets`, or weaken exact-coverage gates.
- Diagnostic vanilla particles/sounds, technical hub/field blocks, the native boss bar and the stone-textured field-review body are temporary validation affordances, not final Riftfrontier UI/art/VFX language.

## First expedition physical loop — verified

`ExpeditionFieldExtractionRelay` keeps extraction on existing authoritative `ExpeditionGameplayService.extract(...)`. Recovery workflow `34780169928` succeeded.

`ExpeditionHubTerminal` closes the repeatable technical loop after extraction:

- smithing table -> existing `provision(...)`;
- hub lodestone -> existing `start(...)`;
- no second hub economy/lifecycle state;
- initial run may still be bootstrapped by `/riftfrontier expedition start` until final reference-reviewed hub UX exists.

Checkpoint `09109a4b8fc11b5c136ff8d329e380fd9fe65eaf`, workflow `34782718258`: full Riftfrontier workflow SUCCESS. Treat this loop as `CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED` only. Human checklists: `M2B_IN_WORLD_EXTRACTION_RELAY.md`, `M2B_IN_WORLD_HUB_LOOP.md`.

## Region 01 boss field harness — verified technical behavior

Development-only commands:

```text
/riftfrontier boss fieldtest spawn
/riftfrontier boss fieldtest phase1
/riftfrontier boss fieldtest phase2
```

The actor remains excluded from natural spawning and production Region 01 encounter composition.

Existing server-owned field behavior:

- production semantic profile `riftfrontier:boss/region_01_first_apex`;
- phase 1: committed strike + line displacement;
- phase 2 adds arena pressure;
- authoritative TELEGRAPH -> ACTIVE -> RECOVERY clock;
- ACTIVE-only diagnostic damage (`1.0F`) with once-per-execution target dedupe;
- attack-start target commitment inside provisional 24 blocks; no mid-attack homing;
- role-specific provisional hit geometry and client diagnostic boundary overlay;
- line displacement physically travels only during ACTIVE at provisional `0.5` blocks/tick with normal Minecraft collision;
- arena pressure applies provisional horizontal `0.85` radial impulse once on authoritative ACTIVE entry;
- arena-pressure ACTIVE entry has one Minecraft-native diagnostic explosion sound/particle cue; this is readability scaffolding, not final VFX/sound.

Arena-pressure cue checkpoint `d47a0229e922b0f01a2258bd47c8db3c0878fffa`, workflow `34791308953` full SUCCESS, deliverable digest `sha256:665fdc874aef764ddb973de2a7f58cfd1a97204bcb062fc3fd1a2362829799e6`.

### Native boss health

Field actor uses Minecraft `ServerBossEvent`, tracks players through `startSeenByPlayer` / `stopSeenByPlayer`, and derives progress from real entity health. Neutral `Region 01 Boss` label is not final lore naming. Checkpoint `9081a074347d041f687ca5f84478a0b575bcb13b`, workflow `34788624295` full SUCCESS, deliverable digest `sha256:e8f44b6e2774c6ca13b46cecc952cde12ae86ca64ca104bfd427df125a9a92ed`.

## Dragon Evolved field-review presentation — verified, still temporary

The production renderer still fails closed until complete reviewed semantic animation coverage plus a reviewed final material exist. While production publication is unavailable, `Region01BossFieldReviewRenderPreview` exposes the accepted sanitized Dragon geometry with Minecraft stone as an intentionally temporary material. Production submission always has priority and prevents duplicate preview drawing.

Visible-body checkpoint `ad273f3bd17fb307994c672972349eb4e05426ef`, workflow `34794765872` full SUCCESS, deliverable digest `sha256:28e77e878c96a7886d198325e86be1fdec2e99639ed71983a76014f43c0243f6`.

### Reviewed attack motion

Only evidence-backed attack roles are sampled from server-synced semantic phase progress:

- `region_01_committed_strike -> Punch`
  - TELEGRAPH `0.0–0.2`
  - presentational ACTIVE `0.2–0.275`
  - RECOVERY `0.275–1.0`
- `region_01_line_displacement -> Headbutt`
  - TELEGRAPH `0.0–0.15555555555555556`
  - presentational ACTIVE `0.15555555555555556–0.2`
  - RECOVERY `0.2–0.4888888888888889`

These windows do not authorize damage timing, hit geometry, movement or target admission. `region_01_arena_pressure` still has no accepted source motion/exact phase windows; do not recycle `Fast_Flying`, `Flying_Idle`, `Yes`, `No`, `HitReact` or `Death` to fake 9/9 coverage.

Attack-motion checkpoint `1683cc426891ce2907e66fece520ffc903b5c2aa`, workflow `34797829558` full SUCCESS, deliverable digest `sha256:ebc27ce3c8fff49ba66c8eeae9c7eacec4137d64b0f7c47ce1cc7765a16f8f20`.

### Reviewed reaction/death motion — latest verified checkpoint

Commit `304fc8317ba1af42463a34b2b1ad1871ff4b57b9` adds field-review sampling for the already directly observed Dragon Evolved `HitReact` and `Death` clips without assigning either to an attack role.

Presentation priority is deliberately:

```text
terminal Death
> reviewed authoritative attack sample (Punch / Headbutt)
> HitReact
> neutral Flying_Idle
```

Consequences:

- a dying field actor samples the reviewed non-looping `Death` motion and clamps at the clip end;
- ordinary damage may show reviewed `HitReact` when no reviewed attack sample is active;
- damage during committed strike or line displacement does **not** visually cancel the authoritative attack motion merely because Minecraft `hurtTime` is non-zero;
- `hurtTime`, `hurtDuration` and `deathTime` are client-visible presentation sampling inputs only; they do not own server attack, health, movement or terminal state;
- arena pressure remains unresolved and does not gain an attack mapping from this work.

`Region01BossReviewedReactionPreviewTest` covers terminal priority inputs, non-looping sample clamp, elapsed hurt-time sampling and no-reaction fail-closed behavior.

Workflow `34804887061`: **SUCCESS** through toolchain, asset intake, clean tests/build, required GameTest, dedicated-server smoke, Xvfb client smoke, executable-JAR inspection, report and artifact uploads. Deliverable `riftfrontier-0.1.0-alpha.1-deliverables` digest: `sha256:6d2edd95f5ece1edf46996a3eda552d55bc0739c338b1ca2db26cf8bf8ad48c3`.

Treat this latest code batch as `CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED`.

Human procedure is `docs/M3_REGION01_BOSS_VISIBLE_FIELD_PREVIEW.md`, now including checks for non-attacking HitReact, damage-during-attack priority, terminal Death, non-looping behavior, and authority independence.

## Production presentation state and exact next boundary

The packaged boss still has no approved final texture/material. `Region01BossMaterialPreparation` requires a real reviewed resource through the existing integrity/review contract.

Production staged attack source binding remains intentionally incomplete at six attack-phase keys because arena pressure has no reviewed source motion with exact windows.

Next automatic development should target **real production-visible Region 01 quality**, not more field/backend scaffolding:

1. Resolve an evidence-backed legal/authored `arena_pressure` motion with exact reviewed phase windows **or** establish a legally usable, commercial-game/major-mod-reference-reviewed final material/texture direction and physical resource.
2. When all nine attack logical keys have evidence, assemble complete production `BossAnimationSourceBinding` without weakening exact coverage.
3. Select/author VFX and sound under the same readability/provenance rules; presentation channels may emphasize but never create a second gameplay hit clock.
4. Author the first real `presentation_assets` manifest only after every referenced physical resource exists and is reviewed.
5. Publish through the existing reload/render pipeline, then verify Minecraft scale/hit-volume readability and resource reload.
6. Only after verified production presentation replaces the temporary path, remove `Region01BossFieldReviewRenderPreview`, `Region01BossReviewedFieldAnimationPreview`, `Region01BossReviewedReactionPreview` and the stone-material assumption as superseded scaffolding.
7. Only after presentation/field evidence clears the gate should production Region 01 encounter insertion and final damage/geometry tuning be considered.

Do **not** auto-tune provisional combat numbers without human field evidence. Do **not** promote native diagnostic cues, stone material, bossbar or technical blocks as final art.

## Verification vocabulary

Current latest reaction-motion code checkpoint:

- `CODE REVIEWED`: YES
- `TESTED`: YES
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES
- `PLAYTESTED`: NO
- `MULTIPLAYER TESTED`: NO

Unit tests, GameTest, dedicated-server smoke and Xvfb client smoke never count as human play or multiplayer field testing.
