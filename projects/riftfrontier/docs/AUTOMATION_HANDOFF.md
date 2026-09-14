# Riftfrontier Automation Handoff

Recovery aid only. Reconstruct canonical truth from current GitHub `main`, `PROJECT.md`, `docs/CANONICAL.md`, project standards and the implementation before acting. Do not treat this file as permission to bypass code/data contracts.

## Current stage

`M3 — PLAYER COMBAT BUILD VERIFIED / REGION 01 BOSS FIELD ROLE-CONTRAST + NATIVE HEALTH READABILITY + VISIBLE REVIEWED ATTACK MOTION BUILD VERIFIED / FIRST EXPEDITION PHYSICAL LOOP BUILD VERIFIED / DRAGON ATTACK SOURCE MAPPING PARTIALLY REVIEWED`

Prioritize the playable vertical slice and visible production quality. Do not reopen settled expedition authority/restart work or add lifecycle/authority fences without a demonstrated regression.

## Settled direction — do not redo

- First vertical slice remains `prepare -> deploy -> fight/recover -> extract -> hub/provision -> redeploy` before region expansion.
- Player combat already has two production weapon families, one technique module and server-authoritative attack/loadout state. Human player-combat `PLAYTESTED: NO`, `MULTIPLAYER TESTED: NO`.
- Region 01 first-boss geometry/rig remains Quaternius `Dragon Evolved` using Riftfrontier's custom skinned-mesh importer/renderer. Do not restart candidate search or reintroduce GeckoLib merely to duplicate this path.
- Accepted sanitized boss resource: `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`, SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- Canonical third-party registry is `docs/THIRD_PARTY_ASSETS.md`. Original Dragon Evolved `Atlas` art is provenance/reference only and is not approved final production art. Do not restore it, invent a final palette, create fake `presentation_assets`, or weaken exact-coverage gates.
- Diagnostic vanilla particles, technical hub/field blocks, the native boss bar, and the visible field-review boss material are validation affordances, not final art/UI/VFX language.

## First expedition loop — verified automation checkpoints

`ExpeditionFieldExtractionRelay` keeps field extraction on the existing authoritative `ExpeditionGameplayService.extract(...)` path. Recovery workflow `34780169928` completed successfully.

`ExpeditionHubTerminal` closes the repeatable technical loop after extraction:

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
- `region_01_line_displacement` (`delivery: line_charge`) physically travels only during ACTIVE at provisional `0.5` blocks/tick along committed facing and uses normal Minecraft collision;
- `region_01_arena_pressure` applies its provisional horizontal `0.85` radial impulse once on authoritative ACTIVE entry, not every ACTIVE tick.

The arena-pressure ACTIVE entry also has one Minecraft-native diagnostic explosion sound/particle cue. Code commit `d47a0229e922b0f01a2258bd47c8db3c0878fffa`, workflow `34791308953`, completed SUCCESS through the full Riftfrontier workflow. Deliverable artifact digest: `sha256:665fdc874aef764ddb973de2a7f58cfd1a97204bcb062fc3fd1a2362829799e6`. This cue is readability scaffolding, not final VFX/sound design.

### Native boss-health readability — verified checkpoint

The field actor uses Minecraft's own `ServerBossEvent` progress UI as a platform-native readability baseline:

- tracking uses `startSeenByPlayer` / `stopSeenByPlayer`;
- progress is derived from real entity `getHealth()/getMaxHealth()`;
- the neutral field label `Region 01 Boss` / `01구역 보스` is not final lore naming;
- no final health/DPS/phase-duration balance was approved.

Verified code checkpoint `9081a074347d041f687ca5f84478a0b575bcb13b`, workflow `34788624295` SUCCESS through full build/runtime/JAR checks. Deliverable artifact digest: `sha256:e8f44b6e2774c6ca13b46cecc952cde12ae86ca64ca104bfd427df125a9a92ed`.

### Visible Dragon Evolved field-review body — verified checkpoint

Human field review was previously blocked by a presentation gap: the accepted Dragon Evolved geometry was resource-prepared, but `Region01BossClientRenderRuntime` correctly refused production renderer publication until both complete reviewed 9/9 logical animation coverage and a reviewed final material existed. `Region01BossRenderer` had no non-production visibility path, so a field-test boss could expose particles/boss bar without a body.

This has now been resolved without weakening production gates:

- commit `b9d00ae1b67969e59fe7cb11c85bcea8c3ece36b` adds `Region01BossFieldReviewRenderPreview`;
- the renderer always attempts `Region01BossClientRenderRuntime.submit(...)` first; if production submission succeeds, the preview is not reached;
- only while production publication is unavailable, the preview consumes the already integrity-checked `RiftfrontierClientResources.preparedBossGeometry()` capability;
- it renders the accepted sanitized Dragon Evolved skinned geometry rather than a cube/proxy;
- the preview material is Minecraft's own `minecraft:textures/block/stone.png`, intentionally chosen as an unmistakably temporary review surface. It is not Riftfrontier final material/palette work and adds no third-party binary asset;
- prepared geometry still fails closed on stale reload capability, and a future complete production publication takes absolute priority.

Two compile failures were retained as evidence rather than hidden:

1. workflow `34794592509` failed because Minecraft 26.2 has no `RenderType.entityCutoutNoCull(Identifier)` factory;
2. recovery commit `13d307a725ac060a09e979f022be954f94bd4517` / workflow `34794671046` still failed because the renamed factory is not on `RenderType` either;
3. final recovery commit `ad273f3bd17fb307994c672972349eb4e05426ef` uses the actual 26.2 `RenderTypes.entityCutout(Identifier)` API.

Workflow `34794765872` for `ad273f3bd17fb307994c672972349eb4e05426ef` completed **SUCCESS** through toolchain, asset intake, clean tests/build, required GameTest, dedicated-server smoke, Xvfb client smoke, executable-JAR inspection, build report and both artifact uploads. Deliverable artifact `riftfrontier-0.1.0-alpha.1-deliverables` digest: `sha256:28e77e878c96a7886d198325e86be1fdec2e99639ed71983a76014f43c0243f6`.

### Reviewed field attack motion — verified checkpoint

The temporary visible-body path now consumes already-reviewed attack motion instead of showing neutral hover through every attack. This does **not** complete or weaken the production 9/9 animation gate:

- `Region01BossReviewedFieldAnimationPreview` maps only authoritative synced semantic states for the two already-reviewed roles;
- `region_01_committed_strike` samples the accepted `Punch` TELEGRAPH / ACTIVE / RECOVERY source windows;
- `region_01_line_displacement` samples the accepted `Headbutt` TELEGRAPH / ACTIVE / RECOVERY source windows;
- source time comes from the server-synced semantic `phaseProgress`; no second attack clock, hit window, movement authority or damage timing exists on the client;
- `region_01_arena_pressure` remains unresolved and therefore gets no recycled attack mapping. While production publication is unavailable it continues to show the neutral reviewed `Flying_Idle` visible-body fallback only;
- production `Region01BossClientRenderRuntime.submit(...)` still has absolute priority, and exact 9/9 semantic animation coverage plus reviewed material remain required before production publication.

Checkpoint `1683cc426891ce2907e66fece520ffc903b5c2aa`, workflow `34797829558`: full `Build Riftfrontier` workflow **SUCCESS** through toolchain, asset intake, clean tests/build, required GameTest, dedicated-server smoke, Xvfb client smoke, executable-JAR inspection, build report and both artifact uploads. Deliverable artifact `riftfrontier-0.1.0-alpha.1-deliverables` digest: `sha256:ebc27ce3c8fff49ba66c8eeae9c7eacec4137d64b0f7c47ce1cc7765a16f8f20`.

Treat this batch as `CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED`. It is **not** human play evidence: `PLAYTESTED: NO`, `MULTIPLAYER TESTED: NO`.

Human field procedure: `docs/M3_REGION01_BOSS_VISIBLE_FIELD_PREVIEW.md`. It now requires checking the accepted silhouette/material scaffold, reviewed `Punch` committed-strike progression, reviewed `Headbutt` line-displacement progression plus lateral dodge/charge travel, neutral fallback for unresolved arena pressure, combat-authority independence, production/preview exclusivity and resource-reload behavior.

General boss checklist: `docs/M3_REGION01_BOSS_FIELD_PLAY.md`.
Focused arena-pressure displacement check: `docs/M3_REGION01_ARENA_PRESSURE_FIELD_CHECK.md`.
Focused native boss-health/tracking check: `docs/M3_REGION01_BOSS_HUD_FIELD_CHECK.md`.

## Dragon Evolved animation/presentation state

Reviewed source mappings remain evidence-based rather than clip-name guesses:

- `region_01_committed_strike -> Punch`
  - TELEGRAPH `0.0–0.2`
  - presentational ACTIVE `0.2–0.275`
  - RECOVERY `0.275–1.0`
- `region_01_line_displacement -> Headbutt`
  - TELEGRAPH `0.0–0.15555555555555556`
  - presentational ACTIVE `0.15555555555555556–0.2`
  - RECOVERY `0.2–0.4888888888888889`

These windows never authorize server damage timing, hit geometry, movement or target admission. The field-review path may now visibly sample them, but that does not transform the six-key staged source binding into complete production coverage.

`region_01_arena_pressure` still has no accepted source motion with exact reviewed phase windows. Do not recycle `Fast_Flying`, `Flying_Idle`, `Yes`, `No`, `HitReact`, or `Death` merely to reach 9/9 coverage. Staged source binding intentionally remains incomplete at six logical animation keys.

The packaged boss contains the sanitized GLTF but no approved final texture resource. `Region01BossMaterialPreparation` still requires a real reviewed material texture through the existing resource/atlas contract.

## Exact next development boundary

1. Do not add more expedition authority/lifecycle hardening unless human/automated evidence exposes a real regression.
2. Do not auto-tune provisional field combat numbers without human field evidence.
3. Do not embellish the stone-textured field preview into a pseudo-final boss. Its reviewed strike/line motion exists only to make current human combat-readability review more representative while production gates remain closed.
4. Automatic implementation should now target **real production-visible Region 01 quality**: either an evidence-backed `arena_pressure` source motion with exact reviewed phase windows, or a legally usable/reference-reviewed final material/texture direction (then VFX/sound) with provenance and Minecraft-scale evaluation.
5. Once all nine logical animation keys have evidence, assemble the complete production `BossAnimationSourceBinding` without weakening exact coverage.
6. Once a verified production renderer/material replaces the field-preview scaffold, remove `Region01BossFieldReviewRenderPreview`, `Region01BossReviewedFieldAnimationPreview` and the stone-material assumption as superseded temporary code rather than maintaining two long-term render systems.
7. Do not restore the Dragon source Atlas or promote diagnostic particles, native field cues, technical blocks, boss bar, or stone preview material as final Riftfrontier art language.
8. `PLAYTESTED: NO`. `MULTIPLAYER TESTED: NO`. Unit tests, GameTest, dedicated-server smoke and Xvfb client smoke never count as either.
