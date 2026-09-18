# TURNBOUND World Overhaul — Drehmal production binding

## Status

This document is the production world-layer canon for the TURNBOUND overhaul that began after the alpha.17 Aster March playtest.

The combat, party, progression, reward, save/WAL and multiplayer-authority systems are retained. The former hand-authored Aster March physical map is no longer a production target.

Current external base:

- Source: Drehmal Team
- World: Drehmal: APOTHEOSIS v2.2.2f
- Public target version: Minecraft Java 1.20.1
- TURNBOUND target: Minecraft Java 26.2 / NeoForge 26.2.0.62
- Profile id: `turnbound:drehmal_apotheosis_2_2_2f`
- Original world/resource-pack assets: **not vendored in this repository**
- TURNBOUND binding data: semantic coordinates and source notes only

The official project provides a downloadable world and installation paths for singleplayer/multiplayer/server use, but this repository does not assume redistribution permission for the original map or resource pack. Users install the official distribution separately. TURNBOUND only stores its own profile marker, semantic anchors and gameplay state.

## Production boundary

### Preserve

These systems remain authoritative unless a later explicit overhaul changes them:

- `combat/*`: Turn Gauge threshold 1000, BattleEngine/BattleState, action/status logic
- `session/BattleSession*`: encounter lifecycle, private battle presentation and server battle authority
- `progression/*`: character ownership, growth, gacha, equipment and currencies
- `CampaignProgressStore`, `CampaignPersistence`, reward journal/WAL and reward settlement
- battle/network payloads and server-authoritative command handling
- character/enemy/reward canonical data
- battle result flow and the field-return contract
- shared progression semantics that are independent of physical Aster March blocks

### Legacy world layer — retained only as migration/reference code

The following classes are no longer called by the production tick path and must not be treated as current world canon:

- `StarterSliceBootstrap`
- `AsterMarchFoundationBuilder`
- `AsterMarchWorldShell`
- `AsterMarchContentOrchestrator`
- `AsterMarchWorldSanitizer`
- `AsterMarchVanillaSpawnGuard`
- `RadiaSafeSpawn`
- `StarterSliceWorld`
- `SouthgateChapterWorld`
- `GloamwoodChapterWorld`
- `BrokenAqueductChapterWorld`
- `EmberQuarryChapterWorld`
- `OldRelayStationWorld`
- old fixed-coordinate seam/gate/transit logic in `WorldSessionRouter`
- `AsterMarchMapData` / `AsterMarchMapScreen` / `AsterMarchMinimapLayer`
- old fixed-coordinate fast-travel projection

Do not delete these mechanically until their game-system dependencies have been migrated. They are quarantined from production first, then removed when callers have been replaced.

### Mixed classes — migrate, do not delete wholesale

- `WorldSessionRouter`: battle-return/progression hooks are useful; physical seam routing is not.
- `FieldSessionManager` and chapter session managers: encounter/progression logic is useful; calls that build or confine players to authored Aster March geometry are not.
- `TurnboundWorldSavedData`: shared progression is useful; physical block-gate writes are legacy.
- `FieldNetwork` / `FieldUiSnapshot`: keep protocol/UI state; replace Aster March coordinate projection.
- `BattleCameraController`: keep battle UX contract, replace terrain-dependent player-centered camera assumptions.

## External-world binding contract

TURNBOUND adopts the validated TURNBOUND: RE pattern rather than inventing another world integration system.

1. An arbitrary save is never auto-converted into a TURNBOUND world.
2. Production runtime activates only when the world contains `.turnbound_world_profile` with the exact profile id.
3. Manual binding is operator-only and requires the operator to stand within 192 blocks of the configured New Drabyel integration seed.
4. Binding writes only TURNBOUND metadata. It does not flatten terrain, place roads, erase structures or copy Drehmal assets.
5. A future packaged first-run installer may write the same marker only after verifying the pinned official download and completing the required 26.2 compatibility migration.
6. Per-player first arrival is stored in separate `ExternalWorldSavedData`; existing character/progression save formats are untouched.
7. Old Aster March spawn suppression and loose-item sanitizer are disabled from the production runtime because they would destructively alter the external authored world.

Current enabled integration seeds:

- Hub / New Drabyel: `502 67 1801`
- First-region seed / Stasis Facility: `778 31 668`

These are **binding seeds**, not final safe gameplay anchors. Terrain, headroom, route quality, nearby structures and encounter suitability must be inspected in the migrated 26.2 world before any seed becomes a player-facing destination.

## First implementation checkpoint

Implemented in the first overhaul unit:

- data-driven Drehmal profile resource
- exact profile marker detection
- operator manual bind command
- separate world SavedData for one-time hub arrival
- production server tick no longer calls Aster March terrain builders/sanitizers/spawn guard
- old Aster March minimap is no longer registered on the production client
- unbound arbitrary worlds fail closed instead of being rewritten
- combat/progression/save code remains in place

Operator harness:

- `/turnbound world status`
- `/turnbound world bind_drehmal`

The bind command must be run from the separately installed Drehmal Overworld near the New Drabyel integration seed.

## Next implementation boundary

The next world work must not revive the old ribbon-map pattern. It should:

1. port/adapt the validated TURNBOUND: RE v2.2.2f download/hash/26.2 migration path without redistributing the original map;
2. inspect the migrated world and promote only verified terrain-safe hub/route/encounter anchors;
3. replace `FieldSessionManager`'s `StarterSliceWorld.build` / `SouthgateChapterWorld.build` dependency with external-world semantic anchors;
4. add terrain-aware battlefield selection around encounters;
5. make battle return restore the real external-world position/session;
6. only then retire the old Aster March builders and coordinate routers physically.

## Validation state of this checkpoint

- CODE REVIEWED: YES
- TESTED: NO
- BUILD VERIFIED: NO
- JAR PRODUCED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

No build or runtime claim is implied by this document.
