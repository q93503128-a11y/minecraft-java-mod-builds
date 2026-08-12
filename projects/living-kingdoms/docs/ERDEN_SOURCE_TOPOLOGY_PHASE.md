# Erden source-native interior topology phase

The urban interior overhaul now has two independent safety layers:

- the runtime preservation pass restores imported architectural detail only when entrance-to-core traversal remains valid;
- `ErdenUrbanSourceTopologyCatalog` reads the three licensed schematic sources directly and records door-connected walkable levels and stair topology without touching world chunks or functional plot counts.

The source catalog is bootstrapped before realm diagnostics so dedicated-server audits capture its topology markers. It is diagnostic infrastructure for replacing synthetic upper floors incrementally, not permission to remove the current fallback.

## Invariants

1. The 233 deterministic urban functional plots and role counts remain unchanged.
2. No source-topology analysis may synchronously load a world chunk.
3. Source-native floors are adopted only after a per-building runtime path proves entrance, stairs and usable floor connectivity.
4. The current synthetic ground/upper-floor geometry remains the fallback whenever imported topology is incomplete or unsafe.
5. The 273-entry traversal audit remains a hard regression gate.

## Next conversion gate

Expose or derive the exact placed fragment footprint for each of the 233 urban entrances, correlate it with the source catalog, then classify each building as source-native or fallback. A source-native classification must identify at least one door-connected upper floor and preserve the real stair route before role fixtures are migrated into verified room cells.
