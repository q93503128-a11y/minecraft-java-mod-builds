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

## Save compatibility

Physical construction is tracked in the separate `erden_exterior_residences` ledger. The existing
exterior production revision is not raised. On an old world, already completed farms, ranches, mines,
mills, wharves and storage yards remain complete; only missing residence parcel chunks are scheduled.
A residence is marked complete only after its door, beds, storage, hearth, work surface, light and
access path are observed in the loaded chunk.

The residence catalog reproduces the existing 74 household IDs and parcel coordinates exactly. The
workforce initializer now consumes that catalog instead of maintaining a second coordinate table.
Founding residents and descendants wait until their saved household's physical residence is complete,
spawn inside that same unit and navigate back to its interior home target outside working hours.

## Streaming and safety

Residence operations share the existing bounded exterior streaming queue and transient portal tickets.
Every nine-by-nine footprint is clamped inside one sixteen-by-sixteen parcel chunk. Existing production
geometry is written only when its own ledger requires it, while the residence plan can run independently
for migrated saves. Incremental writes preserve the known-shape and block-entity cleanup safeguards.

## Required regression proof

The permanent residence audit must compile with Java 25 and create a fresh realm proving:

- exactly 74 residence parcels, 18 attached quarters and 56 detached cottages;
- exactly 74 completed residence chunks and household IDs;
- 74 valid doors, 222 valid bed foot/head pairs, and 74 barrels, furnaces, crafting tables and hanging lanterns;
- level access paths without recessed one-block trenches;
- founding residents and descendants gated on physical home completion;
- resident spawning and home routines driven by the same authoritative residence catalog;
- the existing estate, lifecycle, workforce, 104-chunk exterior, ticket-release and supply-chain proofs;
- no watchdog, synchronous chunk load, invalid block entity or invalid residence errors.
