# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Current recovered state

- The M2 expedition/runtime/restart authority work is already closed enough for this stage; do not repeat it without a demonstrated regression.
- M3 player combat already has the two production weapon-family graph, server-owned ItemStack loadout component, authenticated move-id-only serverbound intent, server runtime authority/lifecycle fencing, client-side action-slot sender, and a supported command provisioning path.
- `RiftfrontierClientCombatInput` resolves authored move slots from the locally published graph and sends `PlayerWeaponMoveIntentPayload(moveId)` only.
- `RiftfrontierClientKeyMappings` exposes two configurable combat actions. They intentionally remain unbound by default because no final control layout has been approved; do not invent a final key layout merely to make the controls look complete.
- Region 01 first-boss geometry/rig direction remains the selected Quaternius CC0 `Dragon Evolved` derivation source feeding Riftfrontier's custom skinned-mesh rendering path. Do not restart candidate search or add GeckoLib merely to duplicate the working custom path.
- Final damage/range/resource policy, final weapon art/presentation, final boss material/VFX/sound, and human field balance/readability remain evidence-gated.

### Canon drift cleanup — 2026-09-13

- `PROJECT.md` had fallen behind actual `main`: it still advertised `M2-B — ... FIELD PLAY NEXT`, an old M2 CI/JAR baseline, and GeckoLib as the animation direction even though the project has already advanced into M3 player combat and the selected Dragon Evolved custom skinned-mesh pipeline.
- `PROJECT.md` is now aligned with the real current stage: `M3 — PLAYER COMBAT BUILD VERIFIED / HUMAN FIELD PLAY + BOSS PRESENTATION NEXT`.
- The optional-dependency note now records that GeckoLib is not on the Region 01 first-boss critical path; do not reintroduce it unless a later concrete asset requirement justifies a new dependency decision.
- The current verified player field-impact checkpoint is now pinned in `PROJECT.md` to run `34739314980` and executable JAR SHA-256 `310257a14a6e8d7c990aa21fb2bff314dd0cfea3234825c5379a1912fb80b8d1`.
- The next visible-quality boundary in both PROJECT and this handoff is the same: human player-combat field play plus the independent selected-boss material/animation/VFX/sound/readability path. Do not regress into M2 authority plumbing or speculative presentation fences.
- This was documentation-only canonical cleanup. No build/CI was triggered for it, and it does not change the already verified code/JAR checkpoint.

## Latest playable-slice work

### Recovery and cleanup

- Recovery `Build Riftfrontier` run `34736865658`, HEAD `0ae324112f93d77c743df6cd4695d0b091c586be`, finished `SUCCESS`. The obsolete GeckoLib resource-id adapter and its dead dedicated test remain deleted; do not restore them.
- The supported field-play loadout commands remain `/riftfrontier weapon mobile`, `/riftfrontier weapon mobile pivot`, `/riftfrontier weapon reach`, and `/riftfrontier weapon reach pivot`.
- The issued vanilla iron sword remains only a temporary physical carrier for the registered server-owned loadout component. It is not final weapon art or balance.

### Player field-impact checkpoint

- Closed the concrete gap where `PlayerWeaponServerRuntime` advanced the authoritative attack clock but constructed its adapter with an empty `List.of()` hit resolver, meaning a human could press the combat action but no production target could ever be admitted.
- Added `PlayerWeaponFieldImpactProfile` as an explicitly provisional field-play calibration boundary for the three existing production moves. It preserves only the already-approved role contrast: mobile-pressure entry is shortest, its committed finisher is slightly longer and lane-shaped, and reach-commitment is longest/narrowest.
- The exact calibration reaches/widths are NOT final balance and must not be promoted without Minecraft field evidence. All three impacts deliberately deal only `1.0F` diagnostic damage so this checkpoint proves server-authoritative delivery instead of smuggling in an unreviewed damage hierarchy.
- Added `PlayerWeaponFieldImpactResolver` with bounded, facing-relative `FORWARD_ARC` / `FORWARD_LANE` admission. It searches only a local AABB around the attacking player and then filters candidates by horizontal forward/lateral geometry and vertical allowance; it does not scan the world.
- Extended `MinecraftPlayerWeaponCombatAdapter` with a separate `ImpactPolicy`. Impact is called only after the existing ACTIVE-only hit-volume gate, server target eligibility, and once-per-execution UUID deduplication have accepted a candidate. The previous three-argument constructor remains available with a no-op impact policy so fixture/test users are not silently converted to damage behavior.
- `PlayerWeaponServerRuntime` now wires one `PlayerWeaponFieldImpactResolver` as both hit-volume and impact policy. Damage remains server authoritative through `ServerPlayer` + server `DamageSources.playerAttack` and never comes from the client payload.
- Added `PlayerWeaponFieldImpactProfileTest` to lock the intended relative reach/shape contrast and the intentionally uniform diagnostic damage while ensuring unrelated boss attacks do not acquire this player calibration accidentally.
- This is a field-play bridge, not the final data/balance layer. After human evidence exists, migrate repeated tuning into the appropriate data-driven production policy instead of letting this calibration class become permanent magic-number design.

### Human field-play gate prepared

- `docs/M3_PLAYER_COMBAT_FIELD_PLAY.md` is now the exact manual validation contract for this checkpoint.
- It pins the verified CI run, code SHA, executable-JAR SHA-256, provisioning commands, temporary control setup, controlled target placement, health inspection, facing/reach/lane/deduplication/telegraph/loadout-invalidation checks, expected outcomes and evidence-reporting format.
- This gate was added instead of inventing extra hit feedback or tuning numbers before observing the actual Minecraft result.
- If mechanics pass but feel visually disconnected, proceed to reference-backed player attack presentation/hit feedback. If geometry fails, fix the observed symptom first. Do not hide a geometry/readability failure by inflating range or removing telegraph.

### Region 01 boss combat/presentation progression — 2026-09-13

- Region 01 production boss combat now has the three authored roles `region_01_committed_strike`, `region_01_line_displacement`, and `region_01_arena_pressure` under `riftfrontier:boss/region_01_first_apex`.
- `region_01_first_apex_semantics.json` and `Region01BossProductionSemantics` author the two-phase composition: phase 1 uses committed strike + line displacement; phase 2 adds arena pressure. The phase-policy CI run `34750881421` finished SUCCESS.
- The selected Dragon Evolved source has direct visual motion review and exact reviewed phase-window evidence for `Headbutt` and `Punch`, but those source clip names do NOT by themselves authorize gameplay-role assignment or ACTIVE hit timing.
- Added the production logical profile `data/riftfrontier/riftfrontier/presentation/region_01_first_apex.json`. It covers all nine server semantic selectors: three attacks × TELEGRAPH/ACTIVE/RECOVERY.
- Added `Region01BossProductionPresentation` so the packaged profile is decoded through the existing strict `BossPresentationProfileCodec` instead of being recreated in Java.
- Added `ProductionRegion01BossPresentationTest` to lock the production boss/model context and exact nine-selector coverage.
- The animation/VFX/sound IDs in that profile are logical contract keys only. They do NOT claim that a reviewed source clip, final VFX, or final sound asset exists for those keys yet.
- Do not infer `Punch -> committed_strike`, `Headbutt -> line_displacement`, or any arena-pressure clip merely from source names. A source binding is allowed only when the observed motion evidence and intended gameplay role are explicitly reviewed together. Arena pressure currently has no approved source-motion mapping.
- Because the complete production profile requires nine logical animation keys, do not publish a partial `BossAnimationSourceBinding` as if it covered the boss. Keep missing source motion fail-closed while material/VFX/sound/animation evidence is authored.

## Verification status

- Canon/source reviewed against current main: YES.
- Recovery run `34736865658`: BUILD VERIFIED / SUCCESS for the pre-impact baseline.
- Field-impact code commit: `2d94c54cfddcb8d81d0ae7567a4d90adb0cd0dca`.
- `Build Riftfrontier` run `34739314980`: **SUCCESS**.
- Run `34739314980` completed toolchain verification, asset-intake tests, `clean test build`, all 9 required native GameTests, dedicated-server smoke, Xvfb client initialization smoke, executable-JAR inspection, build report and deliverable/log uploads.
- Verified executable JAR: `riftfrontier-0.1.0-alpha.1.jar`.
- Verified JAR SHA-256: `310257a14a6e8d7c990aa21fb2bff314dd0cfea3234825c5379a1912fb80b8d1`.
- GitHub Actions deliverable artifact: `riftfrontier-0.1.0-alpha.1-deliverables` from run `34739314980`.
- Current status for the player field-impact checkpoint: CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED.
- Region 01 boss phase-policy run `34750881421`: **SUCCESS**.
- The new production logical presentation profile commits are not yet promoted to BUILD VERIFIED until their own CI run completes successfully.
- Local Gradle/build in the automation environment: NOT RUN; executable verification came from repository CI.
- Human player-weapon field play with the new impact geometry: **NOT TESTED**.
- Multiplayer field play: **NOT TESTED**.
- Final hitbox/visual alignment, final damage/range balance, final weapon presentation: **NOT APPROVED / NOT TESTED**.
- Region 01 boss source-motion-to-gameplay binding, final material/VFX/sound, Minecraft-scale readability and human boss playtest: **NOT APPROVED / NOT TESTED**.

## Do not repeat or revert

- Do not recreate a second client move-intent sender or a second key-mapping layer.
- Do not bind arbitrary default keys until an approved control layout exists.
- Do not reintroduce the removed GeckoLib resource-id adapter or its dead dedicated test unless a later selected asset genuinely requires GeckoLib and the dependency/renderer decision is explicitly changed.
- Do not replace the selected Region 01 boss source simply because another asset is easier to integrate.
- Do not add more player/boss generation, reconnect, owner, exact-instance, target-admission, attack-clock, presentation-world/tick/content-generation fences without a concrete regression.
- Do not promote the vanilla iron-sword carrier into final weapon art or balance.
- Do not promote `PlayerWeaponFieldImpactProfile` numeric calibration to final balance. It exists only to make the current authoritative combat slice actually strike targets so human evidence can be collected.
- Do not derive final damage/range from Monster Hunter, Darktide, Hades or other third-party balance numbers; those references lock role/decision structure only.
- Provisional attack ticks are not final balance.
- Do not claim the successful Xvfb client smoke is a human playtest; it only proves nonfatal initialization.
- Do not confuse logical boss presentation keys with reviewed source assets. The logical profile may exist while actual animation/VFX/sound bindings remain unresolved.

## Exact next development boundary

1. Check CI for the new Region 01 production logical presentation profile first; if it fails, repair that concrete failure without reopening closed combat-authority work.
2. Human player-combat field play remains required via `docs/M3_PLAYER_COMBAT_FIELD_PLAY.md`; do not tune its numeric geometry or diagnostic damage before an observed symptom is captured.
3. Boss-side work may continue independently on evidence-backed presentation: explicitly review whether observed Dragon Evolved motions semantically fit committed strike or line displacement before binding them. Leave any unsupported role unresolved rather than guessing from clip names.
4. Arena pressure currently has no approved source motion. Either select/review a legally usable external motion consistent with the selected rig direction or author/derive one under documented reference constraints; do not fake coverage by recycling an unrelated clip.
5. The next boss-side material gate is still a real reviewed material/texture. The runtime resource directory contains the sanitized Dragon glTF but no production boss texture; do not invent an arbitrary palette or restore the stripped source Atlas.
6. Final VFX/sound similarly require documented reference/asset decisions; the new logical IDs are placeholders for those reviewed assets, not approval of an aesthetic.
7. Do not add the boss to production Region 01 encounter composition until presentation, Minecraft scale/hit geometry and authoritative combat/damage policy have evidence-backed inputs.
8. Never promote CI/client smoke to PLAYTESTED or MULTIPLAYER TESTED. Human evidence remains required for both labels.
