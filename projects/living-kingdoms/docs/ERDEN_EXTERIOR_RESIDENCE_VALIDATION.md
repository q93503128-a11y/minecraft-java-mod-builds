# Erden Exterior Residence Validation

## Physical scope

Erden's exterior workforce now owns 74 real residence units at the original authoritative household
parcels. One Minecraft block is one metre. The first household at each of the 18 supply nodes uses
attached worker quarters inside a corner of the central production complex; the remaining 56
households use detached cottages. No household ID or saved parcel coordinate changes.

Each nine-by-nine-metre residence contains:

- one two-block spruce door and a level exterior access path;
- three complete red beds, for 222 bed fixtures across the exterior settlements;
- one household barrel, furnace, crafting table and hanging lantern;
- a weatherproof foundation, enclosed floor, windows, framed walls, roof and chimney;
- a safe interior spawn position and a shared interior home target used by resident routines.

Minecraft 26.2 no longer exposes a color-specific bed through the `Blocks` constants used by earlier
mappings. The residence builder resolves the stable `minecraft:red_bed` identifier through the built-in
block registry and then applies the normal direction and bed-part state properties. A missing registry
entry is an explicit construction error rather than a silent fallback.

## Save compatibility

Physical construction is tracked in the separate `erden_exterior_residences` ledger. The existing
exterior production revision is not raised. On an old world, already completed farms, ranches, mines,
mills, wharves and storage yards remain complete; only missing residence parcel chunks are scheduled.
A residence is marked complete only after its door, complete bed foot/head pairs, storage, hearth,
work surface, hanging light and every planned access-path cell are observed in the loaded chunk.

The residence catalog reproduces the existing 74 household IDs and parcel coordinates exactly. The
workforce initializer now consumes that catalog instead of maintaining a second coordinate table.
Founding residents and descendants wait until their saved household's physical residence is complete,
spawn inside that same unit and navigate back to its interior home target outside working hours.

## Streaming and safety

Residence operations share the existing bounded exterior streaming queue and transient portal tickets.
Every nine-by-nine footprint is clamped inside one sixteen-by-sixteen parcel chunk. Existing production
geometry is written only when its own ledger requires it, while the residence plan can run independently
for migrated saves. A residence parcel ticket is retained until its separate construction ledger records
the completed unit, even when the older production-site ledger was already complete. Incremental writes
preserve the known-shape and block-entity cleanup safeguards.

Physical storage yards use the same rule. A storage-anchor ticket cannot be released until the barrel
at that node's authoritative storage coordinate has actually been observed in the loaded world. The
exterior completion marker no longer requires all 18 storage chunks to remain loaded simultaneously;
instead it consumes the authoritative observation result recorded during the bounded ticket lifetime.
The permanent ticket audit still requires all 18 storage yards to have been physically observed before
all 104 transient exterior tickets may be released.

A coordinate audit also found that straight household access paths could physically cross storage-yard
fixtures. The concrete failure was `erden_paper_mill_01`: household 55's second path cell and the node's
barrel both occupied `(-1532, 76, 250)`, explaining the historical 17/18 storage observation result.
The same geometry scan found straight-path conflicts with storage chests or hay at three additional
settlements. Access paths are therefore storage-aware: a straight route is used only when every cell is
clear of the authoritative barrel, hay and chest line; otherwise the path bends one metre to a free side
inside the same parcel chunk and continues toward the work site. The physical residence validator checks
every resulting path cell and rejects any route that still occupies a storage fixture. Household parcel
coordinates and storage coordinates remain unchanged, preserving existing saves and supply ownership.

Capital authored-road normalization follows the same cross-chunk safety rule as streamed construction.
Its direct block writes use client updates, known-shape suppression and drop suppression together, so a
road or culvert cell at a chunk edge cannot trigger neighbor-shape resolution that synchronously loads an
unloaded adjacent chunk. This rule is enforced by the permanent authored-road audit after a watchdog
proved that ordinary neighbor updates could otherwise block the server thread for a full minute.

Exterior external-building planning is also nonblocking. A second watchdog showed the server thread
waiting inside `RealmSitePlanner.surfaceY()` while an exterior schematic fragment requested a chunk at
full status during plan creation. External building foundations now read `IncrementalWorldEditPlan`'s
planned surface height: it starts from the deterministic authored continent and automatically reuses any
same-plan site flattening already scheduled for that column. `RealmSitePlanner.surfaceY()` itself is now
an authored-terrain lookup with no `getChunk` or `getHeight` call, so construction planning cannot promote
or synchronously load an unloaded chunk through that helper.

A third watchdog exposed the final lower-level hole: an incremental write could call `getBlockState()`
on a coordinate outside the currently retained exterior chunk, which caused `ServerChunkCache.getChunk()`
to block the server thread. Exterior plans are now explicitly constructed with their `ChunkPos`. Set
operations outside that 16-by-16 X/Z scope are rejected at planning time and fill operations are clipped
to the scope; adjacent structure pieces are generated by that adjacent chunk's own plan. The common write
path also checks `level.hasChunk()` before any block-state read. If a required chunk is temporarily absent,
the operation remains pending instead of being marked complete or synchronously loading the chunk. Thus
this safeguard neither creates silent holes nor permits blocking chunk promotion. The permanent exterior
audit enforces the bound-plan constructor, scope guard, loaded-chunk precondition and known-shape update
flags before starting its fresh-world runtime proof.

## Required regression proof

The permanent residence audit must compile with Java 25 and create a fresh realm proving:

- exactly 74 residence parcels, 18 attached quarters and 56 detached cottages;
- exactly 74 completed residence chunks and household IDs;
- 74 valid doors, 222 valid bed foot/head pairs, and 74 barrels, furnaces, crafting tables and hanging lanterns;
- complete level access paths without recessed trenches or storage-fixture overlap;
- founding residents and descendants gated on physical home completion;
- resident spawning and home routines driven by the same authoritative residence catalog;
- migrated residence chunks retained until the new residence ledger is complete;
- each storage-anchor chunk retained until its physical storage barrel has been observed;
- all 18 physical storage yards observed before all 104 transient exterior tickets are released;
- exterior completion based on persisted construction ledgers plus authoritative physical observations, not simultaneous chunk residency;
- authored road and culvert repair without synchronous cross-chunk neighbor loads;
- external schematic planning without synchronous surface chunk loads;
- chunk-scoped incremental writes with no synchronous block-state read of an unloaded chunk;
- the existing estate, lifecycle, workforce, 104-chunk exterior, ticket-release and supply-chain proofs;
- no watchdog, synchronous chunk load, invalid block entity, invalid residence or storage-path collision errors.
