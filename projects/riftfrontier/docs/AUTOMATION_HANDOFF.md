# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Current recovered state

- The M2 expedition/runtime/restart authority work is already closed enough for this stage; do not repeat it without a demonstrated regression.
- M3 player combat already has the two production weapon-family graph, server-owned ItemStack loadout component, authenticated move-id-only serverbound intent, server runtime authority/lifecycle fencing, and client-side action-slot sender.
- `RiftfrontierClientCombatInput` already resolves authored move slots from the locally published graph and sends `PlayerWeaponMoveIntentPayload(moveId)` only.
- `RiftfrontierClientKeyMappings` already exposes two configurable combat actions. They intentionally remain unbound by default because no final control layout has been approved; do not invent a final key layout merely to make the controls look complete.
- Region 01 first-boss geometry/rig direction remains the selected Quaternius CC0 `Dragon Evolved` derivation source feeding Riftfrontier's custom skinned-mesh rendering path. Do not restart candidate search or add GeckoLib merely to duplicate the working custom path.
- Production hit geometry, damage/range/resource policy, final weapon art/presentation, final boss material/VFX/sound, and human field balance/readability remain evidence-gated.

## Latest playable-slice work

- Re-read the current project canon and quality rules instead of relying on the older handoff.
- Confirmed concrete client combat controls already exist, so no second sender/key-mapping layer was created.
- Removed obsolete `BossPresentationGeckoLibResourceId`: the project has no GeckoLib dependency, the current Region 01 production path uses the custom skinned-mesh renderer, and repository reference searches found no production consumer of the adapter.
- Removed its now-obsolete dedicated unit test after CI proved the first cleanup pass had left that test behind. The failing build was therefore a cleanup regression, not a player-weapon provisioning regression, and the deleted adapter was not restored.
- Added `assets/riftfrontier/lang/ko_kr.json` for the already-existing player-facing mod/combat key strings. This changes localization only; it does not invent UI layout or final control design.
- Added `PlayerWeaponProvisioningCommand` and registered it on the existing command event. This closes the previous practical field-play gap where the server could authenticate a loadout but a player had no supported way to obtain a stack carrying that production identity.
- Provisioning deliberately uses a vanilla iron sword only as a temporary physical carrier. The stack receives the registered `riftfrontier:player_weapon_loadout` component; family/module identity is then revalidated through the existing current published combat catalog. No damage, reach, hit result, timing or target authority was moved into the item or command.
- Available field-play provisioning commands are `/riftfrontier weapon mobile`, `/riftfrontier weapon mobile pivot`, `/riftfrontier weapon reach`, and `/riftfrontier weapon reach pivot`. The issued item must be held in the main hand and the two existing Riftfrontier combat actions must be bound in Minecraft Controls.
- No combat balance, hit shape, final control default, model, material, VFX, sound, or final art direction was invented.

## Verification status

- Canon/source reviewed against current main: YES.
- Obsolete adapter reference search: performed before deletion; no production consumer found, and `build.gradle` contains no GeckoLib dependency.
- First post-cleanup `Build Riftfrontier` run `34734535858`, HEAD `42a5b5e283a3d790a0052b8a8f89a3a27f9093c7`: FAILED at `compileTestJava` because `BossPresentationGeckoLibResourceIdTest` still referenced the deleted obsolete adapter. Asset intake/toolchain/production compile passed before that failure.
- Minimal corrective commit `0ae324112f93d77c743df6cd4695d0b091c586be` deletes only that obsolete dedicated test; it does not weaken an active production contract or restore GeckoLib plumbing.
- Recovery `Build Riftfrontier` run `34736865658`, HEAD `0ae324112f93d77c743df6cd4695d0b091c586be`: IN PROGRESS at this handoff update. Do not claim BUILD VERIFIED until it finishes successfully.
- Local Gradle/build: NOT RUN in this automation environment.
- Human field play: NOT TESTED.
- Multiplayer field play: NOT TESTED.

## Do not repeat or revert

- Do not recreate a second client move-intent sender or a second key-mapping layer.
- Do not bind arbitrary default keys until an approved control layout exists.
- Do not reintroduce the removed GeckoLib resource-id adapter or its dead dedicated test unless a later selected asset genuinely requires GeckoLib and the dependency/renderer decision is explicitly changed.
- Do not replace the selected Region 01 boss source simply because another asset is easier to integrate.
- Do not add more player/boss generation, reconnect, owner, exact-instance, target-admission, attack-clock, presentation-world/tick/content-generation fences without a concrete regression.
- Do not promote the vanilla iron-sword carrier into final weapon art or balance. It exists only to make the current authoritative combat slice human-testable before final weapon presentation is approved.
- Provisional attack ticks are not final balance.

## Exact next development boundary

1. Check recovery `Build Riftfrontier` run `34736865658`. If it fails, fix the first real failing gate without restoring dead GeckoLib plumbing or weakening authority contracts.
2. Once CI is green, use the provisioning commands plus the existing configurable combat actions to prepare a human-testable player combat pass.
3. Do not invent production hit geometry/damage from intuition. Establish the smallest evidence-backed hit geometry/damage gate needed for actual combat feedback, keep values data-driven/provisional, and align visible attack range with authoritative candidates.
4. Continue the selected Region 01 boss through actual material/animation/VFX/sound/readability work on the existing custom renderer, using external references/assets and recorded provenance rather than improvised AI art direction.
5. When a playable checkpoint is available, provide exact Minecraft commands/reproduction steps and expected observations. Never promote CI/client smoke to PLAYTESTED.
