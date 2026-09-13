# Riftfrontier Automation Handoff

Recovery aid only. Current GitHub `main` plus canonical project/design documents remain authoritative.

## Current recovered state

- The M2 expedition/runtime/restart authority work is already closed enough for this stage; do not repeat it without a demonstrated regression.
- M3 player combat already has the two production weapon-family graph, server-owned ItemStack loadout component, authenticated move-id-only serverbound intent, server runtime authority/lifecycle fencing, and client-side action-slot sender.
- `RiftfrontierClientCombatInput` already resolves authored move slots from the locally published graph and sends `PlayerWeaponMoveIntentPayload(moveId)` only.
- `RiftfrontierClientKeyMappings` already exposes two configurable combat actions. They intentionally remain unbound by default because no final control layout has been approved; do not invent a final key layout merely to make the controls look complete.
- Region 01 first-boss geometry/rig direction remains the selected Quaternius CC0 `Dragon Evolved` derivation source feeding Riftfrontier's custom skinned-mesh rendering path. Do not restart candidate search or add GeckoLib merely to duplicate the working custom path.
- Production hit geometry, damage/range/resource policy, final weapon provisioning/presentation, final boss material/VFX/sound, and human field balance/readability remain evidence-gated.

## This run

- Re-read the current project canon and quality rules instead of relying on the older handoff.
- Confirmed the previous handoff was stale about concrete client combat controls: client input and key mapping code already exist.
- Removed obsolete `BossPresentationGeckoLibResourceId`: the project has no GeckoLib dependency, the current Region 01 production path uses the custom skinned-mesh renderer, and repository reference searches found no consumer of the adapter.
- Added `assets/riftfrontier/lang/ko_kr.json` for the already-existing player-facing mod/combat key strings. This changes localization only; it does not invent UI layout or final control design.
- No combat balance, hit shape, control default, model, material, VFX, sound, or final art direction was invented.

## Verification status

- Canon/source reviewed against current main: YES.
- Obsolete adapter reference search: performed before deletion; no consumer found, and `build.gradle` contains no GeckoLib dependency.
- Korean language resource structure mirrors the existing `en_us.json` keys.
- Local Gradle/build: NOT RUN in this automation environment.
- Current post-change GitHub Actions result: not yet confirmed in this handoff. Do not claim BUILD VERIFIED until a Riftfrontier workflow for the descendant commit finishes successfully.
- Human field play: NOT TESTED.
- Multiplayer field play: NOT TESTED.

## Do not repeat or revert

- Do not recreate a second client move-intent sender or a second key-mapping layer.
- Do not bind arbitrary default keys until an approved control layout exists.
- Do not reintroduce the removed GeckoLib resource-id adapter unless a later selected asset genuinely requires GeckoLib and the dependency/renderer decision is explicitly changed.
- Do not replace the selected Region 01 boss source simply because another asset is easier to integrate.
- Do not add more player/boss generation, reconnect, owner, exact-instance, target-admission, attack-clock, presentation-world/tick/content-generation fences without a concrete regression.
- Provisional attack ticks are not final balance.

## Exact next development boundary

1. Check the descendant `Build Riftfrontier` workflow. If it fails, fix the first real regression caused by this cleanup without weakening existing authority contracts.
2. Keep work centered on a genuinely playable M3 slice rather than more backend fencing.
3. Re-check whether a legitimate production ItemStack provisioning path now exists. If absent, implement the smallest player-testable provisioning path that preserves server-owned loadout authority; do not fake a finished weapon UI or final art.
4. Do not invent final control defaults. Existing unbound action slots can be assigned through Minecraft Controls for field testing until the project has an approved mapping.
5. Bind production hit geometry/damage only after an explicit evidence-backed gate; keep provisional values data-driven and clearly provisional where a test slice requires them.
6. Continue the selected Region 01 boss through actual material/animation/VFX/sound/readability work on the existing custom renderer, using external references/assets and recorded provenance rather than improvised AI art direction.
7. When a playable checkpoint is available, produce exact Minecraft test commands/JAR/reproduction steps and request human field-play evidence. Never promote CI/client smoke to PLAYTESTED.
