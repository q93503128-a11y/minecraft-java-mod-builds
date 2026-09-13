# Riftfrontier Automation Handoff

Recovery aid only. Reconstruct canonical truth from current GitHub `main`, `PROJECT.md`, `docs/CANONICAL.md`, project standards and the implementation before acting. Do not treat this file as permission to bypass code/data contracts.

## Current stage

`M3 — PLAYER COMBAT BUILD VERIFIED / REGION 01 BOSS FIELD ROLE-CONTRAST + NATIVE HEALTH READABILITY BUILD VERIFIED / FIRST EXPEDITION PHYSICAL LOOP BUILD VERIFIED / DRAGON ATTACK SOURCE MAPPING PARTIALLY REVIEWED`

Prioritize the playable vertical slice and visible production quality. Do not reopen settled expedition authority/restart work or add lifecycle/authority fences without a demonstrated regression.

## Settled direction — do not redo

- First vertical slice remains `prepare -> deploy -> fight/recover -> extract -> hub/provision -> redeploy` before region expansion.
- Player combat already has two production weapon families, one technique module and server-authoritative attack/loadout state. Human player-combat `PLAYTESTED: NO`, `MULTIPLAYER TESTED: NO`.
- Region 01 first-boss geometry/rig remains Quaternius `Dragon Evolved` using Riftfrontier's custom skinned-mesh importer/renderer. Do not restart candidate search or reintroduce GeckoLib merely to duplicate this path.
- Accepted sanitized boss resource: `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`, SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- Canonical third-party registry is `docs/THIRD_PARTY_ASSETS.md`. Original Dragon Evolved `Atlas` art is provenance/reference only and is not approved final production art. Do not restore it, invent a final palette, create fake `presentation_assets`, or weaken exact-coverage gates.
- Diagnostic vanilla particles and technical hub/field blocks are validation affordances, not final art/UI/VFX language.
- The new native boss bar is an intentional Minecraft-platform readability baseline for the field actor, not approval to invent Riftfrontier's final custom HUD language.

## First expedition loop — verified automation checkpoints

`ExpeditionFieldExtractionRelay` keeps field extraction on the existing authoritative `ExpeditionGameplayService.extract(...)` path. Recovery workflow `34780169928` completed successfully.

`ExpeditionHubTerminal` then closes the repeatable technical loop after extraction:

- smithing-table interaction delegates to existing `provision(...)`;
- hub lodestone delegates to existing `start(...)`;
- no second hub economy/lifecycle state exists;
- first run may still be bootstrapped by `/riftfrontier expedition start` until final reference-reviewed hub UX exists.

Checkpoint `09109a4b8fc11b5c136ff8d329e380fd9fe65eaf`, workflow `34782718258`: full `Build Riftfrontier` workflow SUCCESS through toolchain, asset intake, clean tests/build, required GameTest, dedicated-server smoke, Xvfb client smoke, executable-JAR inspection and artifact upload. Treat the hub-loop code as `CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED`.

Human checklists remain `docs/M2B_IN_WORLD_EXTRACTION_RELAY.md` and `docs/M2B_IN_WORLD_HUB_LOOP.md`. CI does not count as human play.

## Region 01 boss field harness — current verified state

Development-only commands remain:

```text
/riftfrontier boss fieldtest spawn
/riftfrontier boss fieldtest phase1
/riftfrontier boss fieldtest phase2
```

The actor remains excluded from natural spawning and production encounter composition. Existing server-owned behavior remains:

- production semantic boss profile `riftfrontier:boss/region_01_first_apex`;
- phase 1: committed strike + line displacement;
- phase 2 adds arena pressure;
- authoritative TELEGRAPH -> ACTIVE -> RECOVERY clock;
- ACTIVE-only diagnostic damage (`1.0F`) with once-per-execution target dedupe;
- attack-start target commitment inside the provisional 24-block acquisition radius, with no mid-attack homing;
- role-specific provisional geometry and client diagnostic boundary overlay;
- `region_01_line_displacement` (`delivery: line_charge`) physically travels only during ACTIVE at provisional `0.5` blocks/tick along committed facing and uses normal Minecraft collision.

### Arena-pressure physical role contrast — verified checkpoint

Commits `a31253c53f86e17122a11250a1851b49dcd8f3a4` and `2cd121da0f96e3ae40ae72e69c8a426e0c5b60da` add a field-play-only physical distinction for `region_01_arena_pressure` without adding another attack clock:

- `Region01BossFieldImpactProfile` carries `activeEntryRadialImpulse` alongside geometry/line travel;
- committed strike and line displacement author zero radial impulse;
- arena pressure authors provisional horizontal strength `0.85`;
- on the authoritative transition into ACTIVE, eligible targets inside the existing `LOCAL_AREA` field profile are pushed horizontally away from the boss once;
- later ACTIVE ticks do not reapply the impulse;
- no vertical launch is authored;
- a target mathematically coincident with the boss center receives no invented arbitrary push direction;
- the resolver reuses existing `MinecraftCombatAuthority.isEligibleTarget(...)` and the same local-area spatial predicate used by field damage;
- beginning a new execution explicitly resets the prior phase/pattern observation so future zero-length telegraph/recovery data cannot suppress the next ACTIVE-entry impulse.

`0.85` is calibration scaffolding, not final knockback balance. Do not tune it from automation alone.

Workflow `34785968882` for code HEAD `2cd121da0f96e3ae40ae72e69c8a426e0c5b60da` completed SUCCESS through clean tests/build, required GameTest, dedicated-server smoke, Xvfb client smoke, executable-JAR inspection, report and artifacts. Deliverable artifact digest: `sha256:11dc7424197c629c6bac876f195b3e085b8c7993bd9dc3fbfeec755452cdd97e`.

### Native boss-health readability — verified checkpoint

The field actor now uses Minecraft's own `ServerBossEvent` progress UI as a deliberate platform-native readability baseline rather than inventing a new final HUD style:

- the boss event exists only for the explicit field-test actor path;
- `startSeenByPlayer` / `stopSeenByPlayer` attach and detach tracked players instead of maintaining a second client-local visibility model;
- progress is derived every server tick from the real entity `getHealth()/getMaxHealth()` value, so the bar owns no duplicate combat-health state;
- the neutral field label is localized as `Region 01 Boss` / `01구역 보스`; it is not final lore naming;
- no boss max-health, DPS, phase threshold or encounter-duration balance was changed in this batch.

The first implementation commit `326828f5b5711bf98d136eaac4997e324e44c47f` exposed a Minecraft 26.2 API difference: `ServerBossEvent` requires `(UUID, Component, BossBarColor, BossBarOverlay)`. CI failed at compile time rather than being misreported as verified. Recovery commit `9081a074347d041f687ca5f84478a0b575bcb13b` uses the entity UUID and is the verified code checkpoint.

Workflow `34788624295` for `9081a074347d041f687ca5f84478a0b575bcb13b` completed **SUCCESS** through toolchain, asset intake, clean tests/build, required GameTest, dedicated-server smoke, Xvfb client smoke, executable-JAR inspection, reports and artifact upload. Deliverable artifact `riftfrontier-0.1.0-alpha.1-deliverables` has artifact digest `sha256:e8f44b6e2774c6ca13b46cecc952cde12ae86ca64ca104bfd427df125a9a92ed`.

Therefore this HUD/readability code batch is `CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED`. `PLAYTESTED: NO`. `MULTIPLAYER TESTED: NO`.

General boss checklist: `docs/M3_REGION01_BOSS_FIELD_PLAY.md`.
Focused arena-pressure displacement check: `docs/M3_REGION01_ARENA_PRESSURE_FIELD_CHECK.md`.
Focused native boss-health/tracking check: `docs/M3_REGION01_BOSS_HUD_FIELD_CHECK.md`.

## Dragon Evolved animation/presentation state

Reviewed source mappings are evidence-based rather than clip-name guesses:

- `region_01_committed_strike -> Punch`
  - TELEGRAPH `0.0–0.2`
  - presentational ACTIVE `0.2–0.275`
  - RECOVERY `0.275–1.0`
- `region_01_line_displacement -> Headbutt`
  - TELEGRAPH `0.0–0.15555555555555556`
  - presentational ACTIVE `0.15555555555555556–0.2`
  - RECOVERY `0.2–0.4888888888888889`

These windows never authorize server damage timing, hit geometry, movement or target admission.

`region_01_arena_pressure` still has no accepted source motion with exact reviewed phase windows. Do not recycle `Fast_Flying`, `Flying_Idle`, `Yes`, `No`, `HitReact`, or `Death` merely to reach 9/9 coverage. Staged source binding intentionally remains incomplete at six logical animation keys.

The packaged boss contains the sanitized GLTF but no approved final texture resource. `Region01BossMaterialPreparation` still requires a real reviewed material texture through the existing resource/atlas contract.

## Exact next development boundary

1. Do not add more expedition authority/lifecycle hardening unless human/automated evidence exposes a real regression.
2. The field harness now gives all three authored boss roles distinct physical behavior plus native authoritative health readability suitable for human evaluation. Further numerical tuning requires field evidence; do not churn these calibration values automatically.
3. Do not turn the native field boss bar into speculative custom HUD art. Final custom HUD work still requires reference study/review.
4. Automatic implementation should return to **production-visible Region 01 presentation quality**: evidence-backed arena-pressure source motion or a legally usable/reviewed material-texture/VFX/sound set with provenance and Minecraft-scale readability.
5. Once all nine logical animation keys have evidence, assemble the complete production `BossAnimationSourceBinding` without weakening exact coverage.
6. Do not restore the source Atlas or promote diagnostic particles/technical blocks as final art.
7. `PLAYTESTED: NO`. `MULTIPLAYER TESTED: NO`. Unit tests, GameTest, dedicated-server smoke and Xvfb client smoke never count as either.
