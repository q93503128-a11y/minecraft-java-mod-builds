# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Current recovered state

- The M2 expedition/runtime/restart authority work is already closed enough for this stage; do not repeat it without a demonstrated regression.
- M3 player combat already has the two production weapon-family graph, server-owned ItemStack loadout component, authenticated move-id-only serverbound intent, server runtime authority/lifecycle fencing, client-side action-slot sender, and a supported command provisioning path.
- `RiftfrontierClientCombatInput` resolves authored move slots from the locally published graph and sends `PlayerWeaponMoveIntentPayload(moveId)` only.
- `RiftfrontierClientKeyMappings` exposes two configurable combat actions. They intentionally remain unbound by default because no final control layout has been approved; do not invent a final key layout merely to make the controls look complete.
- Region 01 first-boss geometry/rig direction remains the selected Quaternius CC0 `Dragon Evolved` derivation source feeding Riftfrontier's custom skinned-mesh rendering path. Do not restart candidate search or add GeckoLib merely to duplicate the working custom path.
- Final damage/range/resource policy, final weapon art/presentation, final boss material/VFX/sound, and human field balance/readability remain evidence-gated.

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

## Verification status

- Canon/source reviewed against current main: YES.
- Previous recovery run `34736865658`: BUILD VERIFIED / SUCCESS for the pre-impact baseline.
- Latest field-impact test commit: `2d94c54cfddcb8d81d0ae7567a4d90adb0cd0dca`.
- Latest `Build Riftfrontier` run `34739314980`: IN PROGRESS at handoff write. Do not claim this new field-impact checkpoint BUILD VERIFIED until that run or a descendant finishes successfully.
- Local Gradle/build: NOT RUN in this automation environment.
- Human player-weapon field play with the new impact geometry: NOT TESTED.
- Multiplayer field play: NOT TESTED.
- Final hitbox/visual alignment, final damage/range balance, final weapon presentation: NOT APPROVED / NOT TESTED.

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

## Exact next development boundary

1. Check `Build Riftfrontier` run `34739314980`. If it fails, fix the first actual compile/test/runtime gate without weakening the ACTIVE-only/dedup/server-authority path.
2. If green, use the provisioning commands plus the two existing configurable combat actions for a real Minecraft player-combat pass against ordinary hostile mobs. Verify: attacks outside facing geometry miss; mobile entry has materially shorter practical reach than reach commitment; one target receives at most one diagnostic hit per execution; TELEGRAPH/RECOVERY never damage; loadout swap/unequip still invalidates the execution.
3. Do not tune the numeric geometry or damage from intuition after this point. Capture the observed symptom first. Once repeated tuning begins, move the calibration values into the appropriate data-driven production policy rather than growing hard-coded values.
4. Next visible-quality work after this combat checkpoint should be player attack presentation/hit feedback or the selected Region 01 boss material/animation/VFX/sound/readability path, using existing external references/assets and provenance rather than improvised final art.
5. Never promote CI/client smoke to PLAYTESTED. If human play is needed, provide exact commands, control setup, target placement and expected observations.
