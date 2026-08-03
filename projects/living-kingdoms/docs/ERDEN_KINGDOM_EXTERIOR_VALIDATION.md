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

## Streamed construction

Exterior construction uses the same bounded incremental world-edit plan as the capital. Each 16 x 16 metre cell is saved at exterior revision 1 after completion and is not rebuilt until the authored revision changes.

Normal play does not permanently force-load the kingdom. A site and its roads are built when the player loads the relevant chunks. Temporary force-loading exists only for deterministic CI anchors and is released as each cell finishes.

## Licensed architecture and functional additions

Each node uses one of the existing attributed external house, manor or castle-house templates as its architectural anchor. Procedural additions are role-specific rather than generic decoration:

- grain estates: two mature irrigated wheat fields, perimeter fencing and barn
- ranches: divided paddocks, water troughs, hay storage and barn
- collieries: framed mine portal, descending rail tunnel, coal faces, headframe and spoil pile
- iron mines: reinforced deepslate portal, descending rail tunnel, iron faces, headframe and spoil pile
- paper mills: mill building, water channel, reed banks, mill wheel and storage barn
- river wharves: stone quay, timber pier, mooring fence posts and loading crane

Every node also receives a stone loading yard, barrel and secondary chest. Approach roads use five-metre packed-mud carriageways and connect to the nearest capital gate. Paper mills connect to the nearest wharf.

## Terrain integration

The central work yard is flattened only inside a bounded role-specific radius. Fields, fences and route surfaces follow authored continent height samples outside the central pad. Construction suppresses block drops and runs the existing streamed-chunk debris cleaner when each cell completes.

## Persistent validation

The exterior saved data records:

- authored revision
- completed exterior chunks
- completed five-chunk node anchors
- cumulative applied writes

The permanent fresh-world audit must emit:

`LK_ERDEN_KINGDOM_EXTERIOR_PASS revision=1 nodes=18 producers=15 wharves=3`

The same marker must prove:

- at least 70 distinct exterior anchor chunks completed
- all 18 five-cell node anchors completed
- non-zero world-edit writes
- a barrel exists at every loading yard
- licensed external buildings were used
- fields, paddocks, mines, mills, docks and roads were generated
- construction debris remained zero

## Remaining exterior work

This phase does not yet claim a complete living countryside. The following remain before the kingdom completion test:

- physical producer inventory synchronization with every loading-yard barrel
- farm, ranch, mine, mill and port workers with shifts and homes
- ranch livestock and production dependency on animal health
- visible wagon and barge entities following the stored shipment records
- tool, labor, weather, route and damage effects on daily production
- complete settlements and services around the isolated work compounds
- bridges, customs, river traffic and long-distance road stations
