# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus `PROJECT.md`, `docs/CANONICAL.md`, project standards and the current source remain authoritative.

## Current stage

`M3 — PLAYER COMBAT BUILD VERIFIED / REGION 01 BOSS ROLE-SPECIFIC FIELD HARNESS + READABILITY OVERLAY BUILD VERIFIED / HUMAN FIELD PLAY NEXT`

Do not reopen M2 expedition/runtime/restart authority work without a demonstrated regression. The current priority is a genuinely playable vertical slice: human player-combat field play, Region 01 boss field evidence, and evidence-backed boss presentation/combat integration.

## Settled direction — do not redo

- M3 player combat already has two production weapon families, one technique module, server-owned ItemStack loadout state, authenticated move-id-only serverbound intent, authoritative attack lifecycle, client action-slot input, and supported provisioning commands.
- `RiftfrontierClientKeyMappings` exposes two configurable combat actions. They intentionally remain unbound by default because no final control layout has been approved. Do not invent final keys merely to look complete.
- Supported player field-play commands remain `/riftfrontier weapon mobile`, `/riftfrontier weapon mobile pivot`, `/riftfrontier weapon reach`, and `/riftfrontier weapon reach pivot`.
- The vanilla iron sword issued by those commands is only a temporary physical carrier for the registered loadout component. It is not final weapon art or balance.
- Player field impact is server-authoritative and currently uses provisional calibration geometry plus uniform `1.0F` diagnostic damage. Do not promote those values to final balance before human Minecraft evidence exists.
- `docs/M3_PLAYER_COMBAT_FIELD_PLAY.md` is the exact human player-combat validation contract. Human player-weapon field play and multiplayer field play are still NOT TESTED.
- Region 01 first-boss geometry/rig direction remains the selected Quaternius CC0 `Dragon Evolved` derivation source through Riftfrontier's custom skinned-mesh pipeline. Do not restart candidate search or add GeckoLib merely to duplicate the working custom path.
- The obsolete GeckoLib resource-id adapter and its dead dedicated test were removed and must not be restored unless a later selected asset genuinely changes the renderer/dependency decision.
- Final player damage/range/resource policy, final weapon presentation, final boss material/VFX/sound, Minecraft-scale readability and human boss play remain evidence-gated.

## Verified player combat checkpoint

- Player field-impact code checkpoint: `2d94c54cfddcb8d81d0ae7567a4d90adb0cd0dca`.
- `Build Riftfrontier` run `34739314980`: SUCCESS.
- That run completed toolchain verification, asset-intake tests, `clean test build`, all required native GameTests, dedicated-server smoke, Xvfb client initialization smoke, executable-JAR inspection and artifact upload.
- Verified executable JAR: `riftfrontier-0.1.0-alpha.1.jar`.
- Verified JAR SHA-256 for that checkpoint: `310257a14a6e8d7c990aa21fb2bff314dd0cfea3234825c5379a1912fb80b8d1`.
- Status: CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED. This does NOT mean PLAYTESTED or MULTIPLAYER TESTED.

## Region 01 boss combat semantics

- Production boss profile: `riftfrontier:boss/region_01_first_apex`.
- Authored attack roles:
  - `region_01_committed_strike`
  - `region_01_line_displacement`
  - `region_01_arena_pressure`
- `region_01_first_apex_semantics.json` and `Region01BossProductionSemantics` define the two-phase composition:
  - phase 1: committed strike + line displacement
  - phase 2: committed strike + line displacement + arena pressure
- Phase-policy CI run `34750881421`: SUCCESS.
- These attacks use the existing authoritative telegraph → ACTIVE → recovery timing model. Provisional ticks are not final balance.

## Dragon Evolved motion evidence boundary

- The selected Dragon source has direct visual motion review and exact reviewed source phase-window evidence for `Headbutt` and `Punch`.
- Source clip names and observed motion do NOT by themselves authorize gameplay-role assignment or server ACTIVE timing.
- Do not infer `Punch -> committed_strike`, `Headbutt -> line_displacement`, or an arena-pressure mapping merely from names.
- Any source binding requires explicit review of observed motion together with the intended gameplay role.
- Arena pressure currently has no approved source-motion mapping. Leave it unresolved rather than recycling an unrelated clip.

## Logical boss presentation profile

- Authored packaged profile: `data/riftfrontier/riftfrontier/presentation/region_01_first_apex.json`.
- Loader: `Region01BossProductionPresentation`.
- Coverage test: `ProductionRegion01BossPresentationTest`.
- The profile covers all nine semantic selectors: three boss attacks × TELEGRAPH/ACTIVE/RECOVERY.
- Its model/animation/VFX/sound IDs are logical contract keys only. They do NOT claim that final selected production resources exist.
- Do not publish a partial `BossAnimationSourceBinding` as though it covers the boss.

## Presentation staging regression and recovery — 2026-09-13

### What failed

`Build Riftfrontier` run `34753695677` for presentation-profile HEAD `7a6d5e239516ae3b0ea2586b36d0d0cf1d7697ec` failed specifically at the GameTest gate. Toolchain verification, asset-intake tests and `Tests and clean build` had already succeeded.

The concrete cause was a mismatch between two existing rules:

1. `ContentServerReloadListener` discovered the new authored logical presentation profile as live data-pack content and passed it directly into the production content snapshot candidate.
2. Production `ContentRuntime.installValidated(..., presentationProfiles, assetManifest)` correctly refuses to publish any boss presentation profile without a selected-asset manifest.

There is intentionally no production `presentation_assets` manifest yet because the final material/texture, source animation mapping, VFX and sound selections are still unresolved. Creating a fake placeholder manifest would have violated the provenance/selection boundary.

### Recovery implementation

Commit `1da70d4be1145500123c03394553a97c48ac3357` (`fix(riftfrontier): stage boss presentation until asset selection`) updates `ContentServerReloadListener` so:

- authored logical boss presentation profiles are still decoded and therefore remain syntax/schema-reviewable;
- if profiles exist but no selected-asset manifest exists, they remain staged and are NOT published into the authoritative runtime snapshot;
- no placeholder model/animation/VFX/sound selection is invented;
- once a real selected-asset manifest exists, the production publication path and its manifest validation remain unchanged and fail-closed.

This is a regression repair for the playable server path, not a new speculative lifecycle fence.

### Recovery verification

`Build Riftfrontier` run `34756155015`, HEAD `1da70d4be1145500123c03394553a97c48ac3357`: SUCCESS.

The run completed successfully through:

- toolchain verification;
- asset-intake tests;
- `clean test build`;
- required GameTest gate;
- dedicated-server smoke;
- Xvfb client initialization smoke;
- executable JAR inspection;
- build report;
- deliverable and log artifact upload.

Deliverable artifact exists as `riftfrontier-0.1.0-alpha.1-deliverables` for run `34756155015`. The code checkpoint is therefore CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED.

Do not treat Xvfb smoke as human play. PLAYTESTED remains NO and MULTIPLAYER TESTED remains NO.

## Region 01 boss field-play harness — 2026-09-13

A development-only path now exists to exercise the already-authored production boss semantics in a real Minecraft world without prematurely inserting the boss into Region 01 encounter composition.

Initial harness code checkpoint: `c2726fad1019a3af49ec7335a756574814b151bc`.

### Commands

- `/riftfrontier boss fieldtest spawn` — spawns an enabled Region 01 boss actor six blocks in front of the invoking player.
- `/riftfrontier boss fieldtest phase1` — switches the nearest enabled field-test boss within 64 blocks to phase 1.
- `/riftfrontier boss fieldtest phase2` — switches the nearest enabled field-test boss within 64 blocks to phase 2.

### Runtime behavior

- The harness reconstructs `ValidatedBossCombatSemantics` from the current published `ContentRuntime` snapshot, `Region01BossProductionSemantics`, and the packaged logical `Region01BossProductionPresentation`.
- Attack begin, phase transition, damage and presentation sampling stay server-authoritative through `MinecraftBossCombatAdapter.ValidatedRuntime`.
- The same validated tick result is sent through `RiftfrontierNetworking.syncBossPresentation`; there is no second client-authored hit clock.
- A content reload retires a stale published-generation capability; the enabled field actor rebuilds its diagnostic runtime from the new published snapshot rather than continuing stale damage authority.
- The harness is intentionally **not persisted** as production encounter state and remains excluded from natural spawning/Region 01 encounter composition.

### Attack-specific provisional physical policy

The original neutral `MinecraftAttackAdapter.AabbHitVolume(2.5D, 1.5D)` has been replaced in the development-only field harness by `Region01BossFieldImpactProfile` + `Region01BossFieldImpactResolver` so the three already-authored gameplay roles are physically distinguishable before final art exists:

- committed strike: broad, shorter forward arc;
- line displacement: longer, narrower forward lane;
- arena pressure: facing-independent local area around the boss, available only through the phase-2 attack pool.

All three still use `1.0F` diagnostic damage, the existing authoritative ACTIVE-only timing gate, server target eligibility, and once-per-execution target dedupe. The numeric dimensions are calibration scaffolding only. They are not final reach, hitbox, encounter balance or production damage and must be revised only from actual Minecraft field evidence.

`Region01BossFieldImpactProfileTest` locks the intended relative contrast and verifies that the three profile IDs exactly belong to the canonical Region 01 production semantic phase sets. It does not approve the concrete numeric dimensions as final balance.

### Verification for the role-specific field-impact batch

Code checkpoint: `f65e6f98d3a986582117e33f773ccb3220a9faff`.

`Build Riftfrontier` run `34761913225`: SUCCESS.

That run completed successfully through toolchain verification, asset-intake tests, `clean test build`, required Riftfrontier GameTests, dedicated-server smoke, Xvfb client initialization smoke, executable-JAR inspection and artifact upload. The role-specific field-impact batch is therefore CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED. This still does not constitute human play or multiplayer play.

## Region 01 boss field readability overlay — 2026-09-14

The field harness now has a client-only diagnostic readability layer so a human tester can see the provisional server-authored hit geometry while probing the three boss roles in Minecraft. This is not final presentation art and does not replace the pending material/animation/VFX/sound work.

### Implementation

- `Region01BossFieldReadabilityOverlay` listens on the client and projects the current synchronized boss attack over the existing field-test actor.
- It obtains the current attack pattern from `BossPresentationClientState` and looks up the same `Region01BossFieldImpactProfile` used by the server resolver. It owns no independent reach/width/damage/timing values.
- `Region01BossFieldReadabilityProjection` is a Minecraft-client-free projection helper. It derives local outline samples directly from `Region01BossFieldImpactProfile.Profile`:
  - committed strike: samples remain inside the same provisional forward-arc envelope;
  - line displacement: two rails expose the exact provisional half-width through the authored reach;
  - arena pressure: a facing-independent ring uses the exact provisional radius.
- Diagnostic phase particles are intentionally vanilla and temporary: `CLOUD` for TELEGRAPH, `CRIT` for ACTIVE, `SMOKE` for RECOVERY.
- Those particles are instrumentation only. They are not final VFX language, asset selection, combat authority, or a promise that the final boss will use those colors/particles.
- The overlay auto-retires when `Region01BossClientRenderRuntime` has a reviewed production render binding, preventing this diagnostic layer from becoming accidental final presentation.
- No damage, hit admission, server attack clock, phase composition, target dedupe or production encounter composition was changed.

### Initial test-runtime failure and repair

The first readability test checkpoint `d2088accfe6bf5526da889ed18ce0b50557cc94f` produced `Build Riftfrontier` run `34764507625`, which failed at `:test` after Java compilation succeeded. The three new tests failed with `NoClassDefFoundError` because the pure JUnit runtime directly loaded the client overlay class annotated with `Dist.CLIENT`; this test runtime intentionally does not provide the NeoForge client distmarker class.

The repair did not weaken production client isolation or add client dependencies to pure tests. Instead:

- commit `1c6310f32b2dcd0f8628789d6d8edb70c33e33ae` extracted the geometry sampling into `Region01BossFieldReadabilityProjection`;
- commit `ff3b733f0132af708d7c4778eaaa5f5829e2ea0f` changed the client overlay to consume that projection;
- commit `c8530276486606e0a42c15b61c804bd7ed0a9e5d` added the client-free projection tests;
- commit `3ddcc5640689d84e8449c0845418b45fa6dc2121` removed the obsolete client-loaded test fixture.

The tests now verify that local-area samples stay on the exact profile radius, line samples expose both exact half-width boundaries across the full reach, and arc samples never advertise points outside the server profile envelope.

### Recovery verification

`Build Riftfrontier` run `34764647070`, HEAD `3ddcc5640689d84e8449c0845418b45fa6dc2121`: SUCCESS.

That run completed successfully through:

- toolchain verification;
- asset-intake tests;
- `clean test build`;
- required Riftfrontier GameTest gate;
- dedicated-server smoke;
- Xvfb client initialization smoke;
- executable JAR inspection;
- build report;
- deliverable and log artifact upload.

Deliverable artifact: `riftfrontier-0.1.0-alpha.1-deliverables`, artifact ID `10320190641` for run `34764647070`. Status for the readability checkpoint: CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED.

`docs/M3_REGION01_BOSS_FIELD_PLAY.md` now includes the diagnostic-boundary agreement checks. Human field play remains NOT TESTED and multiplayer field play remains NOT TESTED.

## Exact human test contract

`docs/M3_REGION01_BOSS_FIELD_PLAY.md` is the canonical manual procedure. It now covers:

- explicit spawn/cleanup;
- ACTIVE-only damage and one-hit-per-execution dedupe;
- physical role contrast for broad committed strike vs narrow long line displacement vs local arena pressure;
- visible diagnostic boundary agreement for the arc/lane/local-area profiles;
- phase 1/phase 2 composition switching;
- `/reload` generation retirement/rebuild;
- interaction with the existing M3 player weapons;
- required evidence fields and the exact boundary for `PLAYTESTED` / `MULTIPLAYER TESTED` labels.

Human field play remains NOT TESTED until a person actually runs that document.

## Asset/provenance rules that remain locked

- Do not invent a palette or restore the stripped source Atlas merely to make the boss textured.
- Do not create a fake selected-asset manifest containing placeholder paths or logical IDs masquerading as reviewed production resources.
- A real selected resource must have source/provenance and license notes appropriate to its kind.
- Reuse the selected Dragon Evolved rig/geometry direction unless evidence requires changing it.
- Use strong commercial-game/major-mod references for presentation decisions, but do not copy protected assets or derive Riftfrontier balance numbers from those games.
- Final VFX/sound/material choices require documented reference/asset decisions and Minecraft readability review.

## Do not repeat or revert

- Do not recreate a second client move-intent sender or key-mapping layer.
- Do not bind arbitrary final/default keys before a control-layout decision.
- Do not reintroduce the removed GeckoLib adapter/test without a concrete selected-asset need.
- Do not replace Dragon Evolved simply because another asset is easier to integrate.
- Do not add new reconnect/owner/exact-instance/target-admission/attack-clock/content-generation fences without an observed regression.
- Do not promote the vanilla sword carrier or `PlayerWeaponFieldImpactProfile` calibration into final design.
- Do not restore the old one-size-fits-all boss field-test AABB; its verified replacement exists specifically so the three authored combat roles can be tested physically.
- Do not promote `Region01BossFieldImpactProfile` dimensions or `1.0F` damage into production geometry/balance without human evidence.
- Do not promote the diagnostic `CLOUD` / `CRIT` / `SMOKE` readability overlay into final VFX or art direction.
- Do not add the field-test boss to production Region 01 encounter composition merely because the command path works.
- Do not claim successful automated client/server smoke as human play.
- Do not confuse logical boss presentation keys, authored profiles, reviewed source motion and selected production assets; they are distinct gates.
- Do not remove the presentation staging behavior by forcing incomplete boss presentation into the authoritative runtime snapshot.

## Exact next development boundary

1. Human player-combat field play is still required via `docs/M3_PLAYER_COMBAT_FIELD_PLAY.md`. Do not tune the provisional player geometry/damage before an observed symptom exists.
2. Human Region 01 boss field play should use the role-specific geometry plus diagnostic-boundary checks in `docs/M3_REGION01_BOSS_FIELD_PLAY.md`; record exact JAR hash, hit/miss positions and observations instead of inferring success from CI.
3. Boss-side automated work may continue independently on visible production quality. The diagnostic overlay is now sufficient for field-harness readability; do not add more backend or diagnostic fences unless a real test exposes a need.
4. Review whether observed Dragon Evolved `Punch` and/or `Headbutt` motion genuinely fits committed-strike or line-displacement gameplay semantics before authoring a source binding. Unsupported mappings remain unresolved.
5. Arena pressure still needs either a legally usable reviewed motion compatible with the selected rig direction or an authored/derived motion under documented reference constraints.
6. Select/review a real boss material/texture direction with provenance/license records and Minecraft readability in mind. Do not invent an arbitrary final palette and do not restore stripped Atlas art by default.
7. Select/review VFX and sound assets or authored directions under the same evidence/provenance rule.
8. Only when a coherent set of real production resources is selected should `presentation_assets` be authored so the logical profile can move from staged to authoritative published presentation.
9. Do not add the boss to production Region 01 encounter composition until presentation, Minecraft scale/hit geometry and authoritative combat/damage policy have evidence-backed inputs.
10. PLAYTESTED and MULTIPLAYER TESTED remain human-evidence labels only.
