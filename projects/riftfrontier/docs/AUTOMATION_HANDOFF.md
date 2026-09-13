# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus `PROJECT.md`, `docs/CANONICAL.md`, project standards and the current source remain authoritative.

## Current stage

`M3 — PLAYER COMBAT BUILD VERIFIED / REGION 01 BOSS FIELD HARNESS TARGET-COMMITTED + ROLE-READABLE / HUMAN FIELD PLAY NEXT`

Do not reopen M2 expedition/runtime/restart authority work without a demonstrated regression. The current priority is a genuinely playable vertical slice: human player-combat field play, Region 01 boss field evidence, and evidence-backed boss presentation/combat integration.

## Settled direction — do not redo

- M3 player combat already has two production weapon families, one technique module, server-owned ItemStack loadout state, authenticated move-id-only serverbound intent, authoritative attack lifecycle, client action-slot input, and supported provisioning commands.
- `RiftfrontierClientKeyMappings` intentionally leaves final combat keys unbound until a control-layout decision exists. Do not invent final defaults merely to look complete.
- Supported player field-play commands remain `/riftfrontier weapon mobile`, `/riftfrontier weapon mobile pivot`, `/riftfrontier weapon reach`, and `/riftfrontier weapon reach pivot`.
- The issued vanilla iron sword is only a temporary carrier for registered loadout state. It is not final weapon art or balance.
- Player field impact remains provisional and server-authoritative with uniform `1.0F` diagnostic damage. `docs/M3_PLAYER_COMBAT_FIELD_PLAY.md` is the human validation contract. Player weapon PLAYTESTED and MULTIPLAYER TESTED remain NO.
- Region 01 first-boss geometry/rig direction remains the selected Quaternius CC0 `Dragon Evolved` derivation through Riftfrontier's custom skinned-mesh importer/renderer. Do not restart candidate search or reintroduce GeckoLib merely to duplicate the working path.
- Final boss material/texture, VFX, sound, Minecraft-scale readability, source-animation gameplay binding, production encounter integration and human play remain evidence-gated.

## Verified player-combat checkpoint

- Code checkpoint: `2d94c54cfddcb8d81d0ae7567a4d90adb0cd0dca`.
- `Build Riftfrontier` run `34739314980`: SUCCESS.
- Toolchain, asset-intake tests, `clean test build`, required native GameTests, dedicated-server smoke, Xvfb client initialization smoke, executable-JAR inspection and artifact upload succeeded.
- Verified JAR: `riftfrontier-0.1.0-alpha.1.jar`, SHA-256 `310257a14a6e8d7c990aa21fb2bff314dd0cfea3234825c5379a1912fb80b8d1`.
- Status: CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED. This is not human play.

## Region 01 boss production semantics

Production boss profile: `riftfrontier:boss/region_01_first_apex`.

Authored roles:

- `region_01_committed_strike` — broad committed melee role;
- `region_01_line_displacement` — longer narrow lane/displacement role;
- `region_01_arena_pressure` — local area-denial role.

`region_01_first_apex_semantics.json` + `Region01BossProductionSemantics` define:

- phase 1: committed strike + line displacement;
- phase 2: committed strike + line displacement + arena pressure.

Phase-policy CI `34750881421`: SUCCESS. Existing telegraph → ACTIVE → recovery timing remains server-authoritative. Current ticks are not final balance.

## Dragon Evolved presentation evidence boundary

- The accepted sanitized derivation is `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`, SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- Direct visual motion review exists for all carried source clips.
- Exact reviewed phase windows exist for `Headbutt` and `Punch`.
- Source clip names and observed motion do **not** authorize gameplay-role assignment or server ACTIVE timing.
- Do not infer `Punch -> committed_strike`, `Headbutt -> line_displacement`, or an arena-pressure mapping from names.
- Arena pressure currently has no approved source-motion mapping. Leave it unresolved rather than recycling an unrelated clip.
- The packaged logical presentation profile covers all three attacks × TELEGRAPH/ACTIVE/RECOVERY, but model/animation/VFX/sound IDs remain logical contract keys until real selected resources exist.
- Do not publish a partial `BossAnimationSourceBinding` as if it covers the production boss.

## Presentation staging rule

A previous GameTest regression proved that an authored logical presentation profile cannot be published authoritatively before selected physical assets exist. Commit `1da70d4be1145500123c03394553a97c48ac3357` repaired this by keeping logical profiles decoded/reviewable but staged when no real selected-asset manifest exists.

Recovery CI `34756155015`: SUCCESS through build, GameTest, server/client smoke, executable-JAR inspection and artifacts.

Do not create fake placeholder `presentation_assets`, and do not remove staging merely to make incomplete presentation look production-ready.

## Region 01 boss field harness

Development-only commands:

- `/riftfrontier boss fieldtest spawn`
- `/riftfrontier boss fieldtest phase1`
- `/riftfrontier boss fieldtest phase2`

The actor is deliberately excluded from natural spawning and production Region 01 encounter composition.

### Role-specific physical policy

`Region01BossFieldImpactProfile` + `Region01BossFieldImpactResolver` replaced the old one-size-fits-all AABB in the field harness:

- committed strike: broad shorter forward arc;
- line displacement: longer narrower forward lane;
- arena pressure: facing-independent local area, phase 2 only.

All use provisional geometry and `1.0F` diagnostic damage while retaining ACTIVE-only server damage, target eligibility and once-per-execution dedupe.

Role-specific field-impact checkpoint `f65e6f98d3a986582117e33f773ccb3220a9faff`, CI `34761913225`: SUCCESS through full build/smoke/JAR/artifact workflow.

### Diagnostic readability overlay

`Region01BossFieldReadabilityOverlay` projects the same provisional server profile with temporary vanilla particles while no reviewed production render binding exists:

- TELEGRAPH: `CLOUD`;
- ACTIVE: `CRIT`;
- RECOVERY: `SMOKE`.

This is instrumentation, not final VFX or art direction. `Region01BossFieldReadabilityProjection` is client-free and tested against the exact field profile. The overlay retires when a reviewed production render binding is available.

Readability recovery checkpoint `3ddcc5640689d84e8449c0845418b45fa6dc2121`, CI `34764647070`: SUCCESS. The later documentation HEAD `4012a2b6b3a1b1c509d49b8fac890b80e1aec7d4`, CI `34765042128`, also completed SUCCESS.

### Target commitment improvement — 2026-09-14

A concrete field-playability defect was identified from code review: the field actor could continuously attack with no real target and otherwise kept using its spawn-facing direction, making the authored `read_travel_lane` / lateral-dodge counterplay difficult to test meaningfully.

Current implementation:

- `Region01BossFieldAimPolicy` defines a **provisional 24-block field-test acquisition radius**. This is not final boss aggro, leash, arena, perception or AI balance.
- When no alive non-spectator player is within that radius, an idle field-test actor waits instead of starting a new attack into empty space.
- Immediately before a new authoritative attack starts, the server selects the nearest eligible player and commits the boss yaw toward that player.
- Once TELEGRAPH begins, facing is deliberately not retargeted through TELEGRAPH/ACTIVE/RECOVERY. The player can therefore read the committed lane/arc and sidestep without the diagnostic attack homing mid-execution.
- A later attack may select a new facing from the then-nearest eligible player.
- Existing attack clock, hit resolver, damage authority, phase composition, target dedupe, reload-generation handling and production encounter exclusion were not changed.

Commits:

- `a6435d0f95b64c92312efd79db67d4154bceed75` — field aim policy;
- `a0a24393223652ead349a149ab8a75c16655d17c` — pure unit tests for cardinal Minecraft yaw, radius boundary and invalid aim;
- `cc657fc06101d76fdc89fafb86de27110b0ec531` — field actor target acquisition + attack-start committed facing;
- `cedd3dd02c873e33fd75e1a1957d3af419d8dbcb` — canonical manual field-play procedure updated for acquisition/no-homing checks.

CI `34770458284` for code HEAD `cc657fc06101d76fdc89fafb86de27110b0ec531` has already passed toolchain verification, asset-intake tests, `Tests and clean build`, and the required GameTest gate. Dedicated-server smoke was still running at the time of this handoff update. Do not upgrade this new batch to BUILD VERIFIED / JAR PRODUCED until the whole workflow finishes successfully.

## Exact human test contract

`docs/M3_REGION01_BOSS_FIELD_PLAY.md` is authoritative for the manual boss harness procedure. It now checks:

- explicit spawn/cleanup;
- target acquisition only when an alive non-spectator player is within the provisional 24-block diagnostic radius;
- attack-start facing toward the nearest eligible player;
- no mid-attack homing after TELEGRAPH starts;
- ACTIVE-only damage and one-hit-per-execution dedupe;
- broad committed strike vs narrow/long line displacement vs local arena pressure;
- diagnostic particle/server boundary agreement;
- phase 1/phase 2 composition switching;
- `/reload` runtime generation retirement/rebuild;
- interaction with the M3 player weapons;
- exact evidence boundaries for PLAYTESTED and MULTIPLAYER TESTED.

Human boss field play remains NOT TESTED. Multiplayer field play remains NOT TESTED.

## Asset/provenance rules that remain locked

- Do not invent a final palette or restore the stripped source Atlas merely to make the boss textured.
- Do not create a fake selected-asset manifest with placeholder paths or logical IDs masquerading as reviewed resources.
- Real selected resources require source/provenance and license records appropriate to their kind.
- Reuse the selected Dragon Evolved rig/geometry direction unless evidence requires changing it.
- Use strong commercial-game/major-mod references for presentation decisions, but do not copy protected assets or lift their balance numbers.
- Final material/VFX/sound choices require documented reference/asset decisions and Minecraft readability review.

## Do not repeat or revert

- Do not recreate a second client move-intent sender or key-mapping layer.
- Do not bind arbitrary final/default combat keys before a control-layout decision.
- Do not reintroduce the removed GeckoLib adapter/test without a selected-asset need.
- Do not replace Dragon Evolved merely because another asset is easier to integrate.
- Do not add reconnect/owner/exact-instance/target-admission/attack-clock/content-generation fences without an observed regression.
- Do not restore the old one-size-fits-all boss field AABB.
- Do not promote `Region01BossFieldImpactProfile`, the 24-block acquisition radius, or `1.0F` diagnostic damage into final balance without human evidence.
- Do not continuously rotate the field actor toward the player during an executing attack; attack-start direction commitment is intentional counterplay instrumentation.
- Do not promote `CLOUD` / `CRIT` / `SMOKE` into final VFX.
- Do not add the field-test boss to production Region 01 encounter composition merely because the command path works.
- Do not claim CI client/server smoke as human play.
- Do not confuse logical presentation keys, reviewed source motion, selected physical assets and production encounter binding; they are distinct gates.

## Exact next development boundary

1. Check CI `34770458284`; if fully green, record the target-commitment batch as BUILD VERIFIED / JAR PRODUCED and use its deliverable for human testing. If it fails, inspect the concrete failed stage and repair only that regression.
2. Human player-combat field play is still required via `docs/M3_PLAYER_COMBAT_FIELD_PLAY.md`; do not tune provisional player geometry/damage without observed evidence.
3. Human boss field play should use `docs/M3_REGION01_BOSS_FIELD_PLAY.md`, especially the new acquisition/no-homing checks. Record exact JAR hash, hit/miss positions and video where useful.
4. Automated boss-side work should move back toward visible production quality, not more diagnostic/backend fences.
5. Explicitly review whether observed Dragon Evolved `Punch` and/or `Headbutt` motion genuinely fits committed-strike or line-displacement semantics before authoring any source binding.
6. Arena pressure still needs a legally usable reviewed/derived motion compatible with the selected rig direction and documented reference constraints.
7. Select/review a real boss material/texture direction with provenance/license records and Minecraft readability in mind. Do not invent an arbitrary final palette or restore stripped Atlas art by default.
8. Select/review VFX and sound assets/directions under the same evidence/provenance rule.
9. Only when a coherent set of real production resources exists should `presentation_assets` be authored and the staged logical profile move toward authoritative production presentation.
10. PLAYTESTED and MULTIPLAYER TESTED remain human-evidence labels only.
