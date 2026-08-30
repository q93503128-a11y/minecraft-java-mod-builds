# Frontier Settlement Alpha.91 — worker navigation / local storage hardening

Version: `0.1.0-alpha.91`

## Larger physical resource search
- Lumber: 48 -> 128 horizontal blocks, vertical evidence from work Y -12 to +28.
- Quarry: 40 -> 96 horizontal blocks, vertical evidence from work Y -16 to +12.
- Mine scan: 24 -> 48 horizontal blocks and 48 -> 80 blocks downward.
- Search is still loaded-only: no chunk force-load or teleport.
- Tree/quarry targets are cached for 30 seconds and misses cool down for 5 seconds so the larger envelope is not rescanned every work tick.
- Worker lifecycle lookup/evidence bounds expand with the real lumber/quarry roaming envelope so far workers are not falsely treated as missing.

## No fence/water stupidity
- Work movement accepts only a vanilla path whose `Path.canReach()` is true.
- Coordinate-only partial-path acceptance is removed from local production movement.
- Frontier workers give water a negative path malus; standing cells and support blocks must also be dry.
- A target with no complete path is temporarily blacklisted.
- Less than 0.25-block progress for about four seconds triggers stuck recovery: navigation stops, the target is rejected for six seconds, and resource targeting is recalculated.

## Per-job physical barrels
- Lumber, farm, quarry and mine output first returns to that completed building's own barrel.
- Farm/quarry/mine use their existing blueprint barrels. Lumber receives a new barrel at local `(5,1,6)`.
- Old completed work buildings receive a missing managed barrel only if that exact loaded cell is dry, replaceable and has solid support; player blocks and block entities are never overwritten.
- Full, blocked, missing or unreachable local barrels fall back to shared physical storage. Cargo is never virtualized.
- Farm crop iteration now uses rotation-aware `BuildingRecord.localToWorld`, fixing rotated farm harvesting/replanting.

## Public storage capacity
A vanilla barrel is fixed at 27 slots. Making one barrel genuinely larger would require a custom block entity, menu/screen, inventory synchronization and save migration.
Alpha.91 instead keeps vanilla containers and manages a protected four-barrel public cluster around the original saved stockpile for up to **108 slots**.
The persisted original barrel remains authoritative; safe neighboring annex cells are backfilled without deleting solid blocks or block entities.

## Authority / safety
- No custom container protocol.
- No virtual resource ledger or item minting.
- No force-load or teleport.
- No second resident/logistics authority.
- Companion binary pins are unchanged.
