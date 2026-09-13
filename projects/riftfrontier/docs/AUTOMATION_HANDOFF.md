# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus `PROJECT.md`, `docs/CANONICAL.md`, project standards and the current source remain authoritative.

## Current stage

`M3 — PLAYER COMBAT BUILD VERIFIED / HUMAN FIELD PLAY + BOSS PRESENTATION NEXT`

Do not reopen M2 expedition/runtime/restart authority work without a demonstrated regression. The current priority is a genuinely playable vertical slice: human player-combat field play and evidence-backed Region 01 boss presentation/combat integration.

## Settled direction — do not redo

- M3 player combat already has two production weapon families, one technique module, server-owned ItemStack loadout state, authenticated move-id-only serverbound intent, authoritative attack lifecycle, client action-slot input, and supported provisioning commands.
- `RiftfrontierClientKeyMappings` exposes two configurable combat actions. They intentionally remain unbound by default because no final control layout has been approved. Do not invent final keys merely to look complete.
- Supported field-play commands remain `/riftfrontier weapon mobile`, `/riftfrontier weapon mobile pivot`, `/riftfrontier weapon reach`, and `/riftfrontier weapon reach pivot`.
- The vanilla iron sword issued by those commands is only a temporary physical carrier for the registered loadout component. It is not final weapon art or balance.
- Player field impact is server-authoritative and currently uses provisional calibration geometry plus uniform `1.0F` diagnostic damage. Do not promote those values to final balance before human Minecraft evidence exists.
- `docs/M3_PLAYER_COMBAT_FIELD_PLAY.md` is the exact human validation contract. Human player-weapon field play and multiplayer field play are still NOT TESTED.
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
- Do not claim successful automated client/server smoke as human play.
- Do not confuse logical boss presentation keys, authored profiles, reviewed source motion and selected production assets; they are distinct gates.
- Do not remove the new staging behavior by forcing incomplete boss presentation into the authoritative runtime snapshot.

## Exact next development boundary

1. Human player-combat field play is still required via `docs/M3_PLAYER_COMBAT_FIELD_PLAY.md`. Do not tune the provisional geometry/damage before an observed symptom exists.
2. Boss-side work may continue independently on visible quality. The highest-value next work is actual evidence-backed production presentation, not more lifecycle plumbing.
3. Review whether observed Dragon Evolved `Punch` and/or `Headbutt` motion genuinely fits committed-strike or line-displacement gameplay semantics before authoring a source binding. Unsupported mappings remain unresolved.
4. Arena pressure still needs either a legally usable reviewed motion compatible with the selected rig direction or an authored/derived motion under documented reference constraints.
5. Select/review a real boss material/texture direction with provenance/license records and Minecraft readability in mind. Do not invent an arbitrary final palette and do not restore stripped Atlas art by default.
6. Select/review VFX and sound assets or authored directions under the same evidence/provenance rule.
7. Only when a coherent set of real production resources is selected should `presentation_assets` be authored so the logical profile can move from staged to authoritative published presentation.
8. Do not add the boss to production Region 01 encounter composition until presentation, Minecraft scale/hit geometry and authoritative combat/damage policy have evidence-backed inputs.
9. PLAYTESTED and MULTIPLAYER TESTED remain human-evidence labels only.
