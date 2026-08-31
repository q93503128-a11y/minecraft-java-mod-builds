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
- Farm/quarry/mine use their existing blueprint barrels. Lumber receives a barrel at local `(5,1,6)`.
- Old completed work buildings receive a missing managed barrel only if that exact loaded cell is dry, replaceable and has solid support; player blocks and block entities are never overwritten.
- Full, blocked, missing or unreachable local barrels fall back to shared physical storage. Cargo is never virtualized.
- Farm crop iteration uses rotation-aware `BuildingRecord.localToWorld`, fixing rotated farm harvesting/replanting.

## Shared public storage
The old Alpha.91 public four-barrel cluster has been superseded by the combined Frontier + Survival shared-economy design.
A newly founded settlement receives exactly one dedicated **공용 보급고 / shared supply depot** as its authoritative starter stockpile.
The depot has **54 physical ItemStack slots** and is registered with the shared-depot registry so both Frontier settlement spending and Survival Ascension shared-resource spending can consume the same physical inventory.
Additional shared depots are player-crafted/placed rather than automatically granted; profession worksite barrels remain ordinary local barrels.

For save compatibility, legacy Alpha.91 public vanilla barrels in the former stockpile cluster are upgraded in place to shared supply depots while preserving their physical ItemStacks. This is a one-way compatibility migration, not a reason to create free annex depots in new settlements.
The authoritative starter depot remains managed/protected like the old founding stockpile; player-crafted shared depots outside managed positions remain normal removable blocks.

## Authority / safety
- Shared resources are still backed by physical Minecraft ItemStacks; no virtual resource ledger or item minting.
- No force-load or teleport.
- No second resident/logistics authority.
- Frontier and Survival share resource categories through the dedicated depot while exact rare/progression catalysts may remain exact items.
- Companion binary pins are unchanged.
