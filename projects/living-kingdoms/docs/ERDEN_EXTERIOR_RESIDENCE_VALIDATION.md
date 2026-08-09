# Erden Exterior Residence Validation

## Current physical model

Erden has 74 persistent exterior households. Their household IDs and saved logical estate coordinates remain unchanged, but their generated physical homes are now a separate concern.

One Minecraft block is one metre. Every household receives one 9 x 9 metre home with:

- one complete two-block spruce door;
- three complete red beds, 222 beds across 74 households;
- one household barrel, furnace, crafting table and hanging lantern;
- foundation, enclosed floor, windows, framed walls, roof and chimney;
- a level access path;
- an interior spawn position and home target shared by founding residents and descendants.

Minecraft 26.2 does not expose the color-specific bed through the old `Blocks.RED_BED` style constant. The builder resolves `minecraft:red_bed` from the built-in block registry and validates both FOOT and HEAD halves.

## Why residence v2 exists

Residence v1 reused the workforce ledger's logical parcel coordinates as literal building coordinates. Those parcels sit at the supply-node center or only 28 metres away. The licensed supply structures are much larger than that, and an exact reconstruction audit proved that the first 18 v1 homes physically cut through all 18 central production buildings. The low-level overlap alone affected all 81 columns of every 9 x 9 footprint.

The permanent overlap audit then expanded the check to all 74 households. Physical residence coordinates are therefore no longer derived directly from the saved parcel coordinate.

## Save-compatible coordinate split

`ErdenExteriorResidenceCatalog` keeps two coordinate concepts:

- `parcelX` / `parcelZ`: the original logical household estate coordinates used by the workforce, inheritance and property ledgers;
- `physicalX` / `physicalZ`: the actual generated home anchor used by construction, spawning and home routines.

No household ID is renumbered and no saved estate coordinate is rewritten.

The physical homes form small worker hamlets outside each supply site. The hamlet baseline is 96 metres beyond the production node, with household side offsets of 0, -24, +24, -48 and +48 metres. For ordinary producers and wharves the baseline points away from the kingdom centre. Paper-mill hamlets are placed perpendicular to the mill-to-wharf freight leg so the homes do not occupy the cart road.

The final permanent geometry audit proves:

- 74 households;
- 74 distinct physical residence chunks;
- zero overlap with the real Schemcraft production structures after the same filtering and rotation rules used by the game;
- zero overlap with authored fields, paddocks, mine works, paper-mill works and wharf works;
- zero overlap with the supply freight roads;
- zero overlap between homes;
- minimum home-centre distance from its production node of about 95 metres after in-chunk footprint clamping.

## Migration and repair

`ErdenExteriorResidenceBuilder.RESIDENCE_REVISION` is 2. The separate `erden_exterior_residences` ledger therefore schedules the new physical homes even when a world already contains residence v1 data.

`ErdenKingdomExteriorBuilder.EXTERIOR_REVISION` is also 2. This is intentional. Residence v1 could overwrite parts of a licensed production building or nearby authored site geometry. Rebuilding the existing exterior anchor chunks once under revision 2 restores the authoritative production structure and worksite at the old v1 locations, while the residence ledger constructs the replacement home in the new hamlet chunk.

This repair does not recreate the whole world blindly. Exterior production chunks and residence chunks maintain separate completion conditions. A residence-only hamlet chunk is never marked as a completed production chunk.

## Streaming and ticket ownership

The exterior CI set is the union of:

- the 104 existing producer, storage and site anchor chunks; and
- 74 unique physical residence chunks.

The expected union is therefore 178 transient chunks.

At most two exterior chunks are retained in flight. They use transient `TicketType.PORTAL` tickets, not permanent forced chunks and not synchronous `level.getChunk(...)` calls.

The CI construction stress path is intentionally bounded rather than being allowed to consume almost the entire server tick:

- normal exterior write budget: 2,000 writes per tick;
- CI exterior write budget: 4,000 writes per tick;
- maximum incremental-plan CPU slice: 12 ms per server tick;
- CI retained remote chunks: at most two;
- founding-resident materialisation: at most two new villagers per spawn pass.

This does not reduce content, residence count, supply sites or validation coverage. It only spreads the same work over more ticks.

Ticket release is conditional:

- an exterior anchor may release only after exterior revision 2 is physically complete;
- a residence chunk may release only after residence revision 2 is physically validated and recorded;
- a storage anchor may release only after its authoritative barrel has actually been observed;
- the CI resident sample retains its site and physical home chunks until the household has materialized and been observed.

The common incremental write path keeps `Block.UPDATE_KNOWN_SHAPE`, refuses synchronous access to unloaded chunks and clips chunk-bounded plans to their 16 x 16 metre scope.

## Resident and lifecycle integration

The workforce initializer consumes the same residence catalog that construction uses. Founding residents and later descendants:

1. retain their original household and logical estate identity;
2. wait for their physical residence to finish construction;
3. spawn at the residence builder's interior spawn position;
4. use the same household ID to return to the residence builder's home target outside work hours.

Property succession therefore transfers the same household estate record while the physical home remains attached to that household ID.

## Physical-home chunk and commute safety

The first residence-v2 fresh-world audit exposed a migration bug. The spawn code correctly calculated the new physical home position but still tested whether the old logical `homeX/homeZ` parcel chunk was loaded. Once a physical-home ticket had been released, that mismatch could allow `safeStandingY` to read blocks from an unloaded physical chunk on the server thread.

The corrected resident contract is now shared by founding residents and lifecycle descendants:

- resident materialisation is gated by the chunk containing `residentSpawnPosition(...)`, never the logical estate parcel chunk;
- spawn functions defensively refuse to read or place an entity when the physical home chunk is not loaded;
- physical commuting remains a loaded-world view of the aggregate workforce simulation;
- a commute path is submitted to Minecraft navigation only when sampled chunks along the resident-to-target corridor are already loaded;
- founding-worker routine navigation is capped at six new path requests per routine pass;
- lifecycle/descendant routine navigation is capped at four new path requests per routine pass;
- founding dependents remain controlled by the founding workforce manager rather than receiving duplicate lifecycle routine commands.

When the route is not fully loaded, the saved workforce/lifecycle state still determines attendance and production. The server does not force chunks merely to animate a worker walking through an unseen area.

## Watchdog pressure regression and sample observation

Residence-v2 stress runs originally reached roughly 130 of 178 transient exterior/residence tickets and then occasionally crossed Minecraft's 60-second watchdog threshold. Per-chunk request/start/complete tracing proved that no single 9 x 9 residence plan was stuck: the warned tick could end with that residence completing and the next chunk beginning normally.

A dedicated stress diagnostic then completed the full exterior while measuring about 54.6 seconds of accumulated server lag under the old CI pressure. After bounding the same workload to 4,000 writes per tick, a 12 ms plan slice and two retained chunks, the same diagnostic completed exterior revision 2 without an `A single server tick took` watchdog event. Its largest observed backlog fell to about 31.0 seconds. Normal gameplay is less aggressive again because its exterior write budget remains 2,000 and it does not proactively retain the entire 178-chunk CI set.

A separate v2 validation bug was also found in the resident sample observer. It still searched only the old node-centred ±96 metre AABB even though the physical home starts around 96 metres away and its interior spawn may lie a few blocks farther out. The sample residents could therefore exist while the audit reported `resident_sample_validated=false`, preventing the final sample tickets from releasing. The observer now resolves the sample household's actual `residentSpawnPosition(...)`, requires that physical chunk to be loaded and counts residents only inside a tight ±16 metre AABB around the real home.

## Permanent regression gates

A residence change is not accepted from compilation alone. The permanent audits require:

- Java 25 compilation;
- residence revision 2 and exterior revision 2;
- 74 completed physical homes and 222 complete bed pairs;
- all household fixtures and access paths physically observed;
- 178 transient exterior/residence tickets released with no permanent forced chunks;
- 18 storage yards observed;
- workforce, lifecycle, estate, inventory and kingdom-supply markers still succeeding;
- the 20-year non-persistent lifecycle projection still producing births, succession and replacement labour;
- the 74-home geometry gate reporting structure=0, worksite=0, road=0 and home=0 collisions;
- resident materialisation gated on physical-home chunks rather than logical estate chunks;
- resident sample observation centred on the real physical home;
- no long-distance navigation request across unloaded intermediate chunks;
- CI write pressure bounded to 4,000 writes, 12 ms and two retained chunks;
- no watchdog, synchronous chunk load, invalid block entity or invalid residence errors.

The historical `attached_quarters=18` counter remains in the runtime marker only for continuity with earlier audit/report consumers. Those 18 first-household homes are no longer generated inside the production building; physically they are part of the same collision-free worker-hamlet system as the other households.
