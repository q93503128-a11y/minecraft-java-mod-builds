# Erden kingdom exterior construction validation

## Scope

The 18 saved supply nodes are no longer abstract coordinates only. Exterior chunks now stream authored production sites and their approach routes as those chunks load. One block is treated as one metre throughout the layout.

This phase establishes the physical geography and worksite shell for:

- 4 grain estates
- 3 ranches
- 3 collieries
- 2 iron mines
- 3 paper mills
- 3 river wharves

The economic supply ledger and exterior builder consume the same authoritative node catalog, preventing a simulated producer from drifting away from its visible worksite.

## Catalog integrity

`ErdenKingdomSupplyCatalog` is the sole authority for node ID, metre coordinates, role, site radius, licensed building style and facing. The economy creates saved producer records from this catalog and the exterior builder reads the same entries. A second handwritten coordinate table is not permitted.

The catalog must always contain exactly 18 unique IDs, 15 producing nodes and 3 river wharves. A catalog revision that moves or changes a site must also advance the exterior authored revision so previously built chunks cannot silently preserve obsolete geometry.

## Streamed construction

Exterior construction uses the same bounded incremental world-edit plan as the capital. Each 16 x 16 metre cell is saved at exterior revision 1 after completion and is not rebuilt until the authored revision changes.

Normal play does not permanently force-load the kingdom. A site and its roads are built when the player loads the relevant chunks. Temporary force-loading exists only for deterministic CI anchors and is released as each cell finishes.

CI anchor preparation is non-blocking. The unique exterior anchor list is prepared without loading chunks, at most one distant chunk is force-requested per server tick, and construction begins only after the chunk load event confirms availability. The server thread must never call synchronous `getChunk` after forcing an exterior CI chunk. This prevents the former 90-chunk single-tick request from tripping the 60-second server watchdog.

## Licensed architecture and functional additions

Each node uses one of the existing attributed external house, manor or castle-house templates as its architectural anchor. Procedural additions are role-specific rather than generic decoration:

- grain estates: two mature irrigated wheat fields, perimeter fencing and barn
- ranches: divided paddocks, water troughs, hay storage and barn
- collieries: framed mine portal, descending rail tunnel, coal faces, headframe and spoil pile
- iron mines: reinforced deepslate portal, descending rail tunnel, iron faces, headframe and spoil pile
- paper mills: mill building, water channel, reed banks, mill wheel and storage barn
- river wharves: stone quay, timber pier, mooring fence posts and loading crane

Every node also receives a stone loading yard, barrel and secondary chest. Approach roads use five-metre packed-mud carriageways and connect to the nearest capital gate. Paper mills connect to the nearest wharf.

## Authoritative producer inventories

The 15 producing sites use their loading-yard barrel as the visible form of the saved producer stock. River wharves are transfer sites and are intentionally excluded from production inventory materialization.

The tick order is fixed:

1. Capture player changes from already materialized producer barrels.
2. Run exterior production and dispatch.
3. Settle capital-bound cargo through shipment escrow.
4. Materialize the resulting producer stock back into the barrels.
5. Run the remaining living economy and transport systems.

An empty newly constructed barrel cannot overwrite saved production. A separate materialization ledger records which producer barrels have received their initial saved stock. After that first write, removing wheat, leather, hay, coal, iron or paper from a barrel reduces the corresponding saved node stock on the next synchronization tick.

Daily dispatch preserves working reserves at the site instead of removing every unit:

- grain estate: 12 wheat
- ranch: 8 leather and 6 hay
- colliery: 8 coal
- iron mine: 4 iron
- paper mill: 10 paper

Only stock above the reserve enters shipment escrow. Dispatch immediately reduces the producer barrel on the next materialization pass, while the capital warehouse is credited only after arrival.

Right-clicking a producer barrel reports the actual stored stock, cargo currently in transit, cumulative production and route-delay days in Korean.

## Terrain integration

The central work yard is flattened only inside a bounded role-specific radius. Fields, fences and route surfaces follow authored continent height samples outside the central pad. Construction suppresses block drops and runs the existing streamed-chunk debris cleaner when each cell completes.

## Persistent validation

The exterior saved data records:

- authored revision
- completed exterior chunks
- completed five-chunk node anchors
- cumulative applied writes

The permanent fresh-world construction audit must emit:

`LK_ERDEN_KINGDOM_EXTERIOR_PASS revision=1 nodes=18 producers=15 wharves=3`

The same marker must prove:

- at least 70 distinct exterior anchor chunks completed
- all 18 five-cell node anchors completed
- non-zero world-edit writes
- a barrel exists at every loading yard
- licensed external buildings were used
- fields, paddocks, mines, mills, docks and roads were generated
- construction debris remained zero
- CI requests were staggered and no synchronous exterior `getChunk` path ran

The permanent producer-inventory audit must additionally emit:

`LK_ERDEN_EXTERIOR_INVENTORY_PASS revision=1 nodes=15 containers=15`

It must prove all six resources are visible, capture and write passes both occurred, player removal is authoritative, dispatch reduces barrels and local reserves remain. The kingdom-supply marker must include `local_reserves=true`.

## Remaining exterior work

This phase does not yet claim a complete living countryside. The following remain before the kingdom completion test:

- farm, ranch, mine, mill and port workers with shifts and homes
- ranch livestock and production dependency on animal health
- visible wagon and barge entities following the stored shipment records
- tool, labor, weather, route and damage effects on daily production
- complete settlements and services around the isolated work compounds
- bridges, customs, river traffic and long-distance road stations
