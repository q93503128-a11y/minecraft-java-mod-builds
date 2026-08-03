# Erden exterior workforce validation

Erden's rural production belt is no longer an automatic background resource generator. Its farms,
ranches, collieries, iron mines, paper mills and river wharves share one persistent population and
attendance ledger with the kingdom supply system.

## Authoritative population

- 18 exterior supply settlements
- 74 households
- 216 persistent residents
- 142 assigned workers
- 74 children or elders
- 124 production-site workers
- 18 wharf workers
- unique resident IDs, household homes and node assignments
- permanent death records; dead workers are not silently replaced

Loaded villagers are materialised views of the saved residents. Unloaded settlements continue to
use the same identities and saved death state through aggregate simulation.

## Attendance and production

Each worker has a role, shift and distributed weekly rest day. Deterministic illness and hazardous
mine absence can create additional missed work. Every day the ledger records required, living,
attending, absent and dead workers for each node.

Daily output is calculated from both the seasonal production percentage and the node's actual
attendance percentage. Zero labour can therefore produce zero goods. A node below half staffing
cannot dispatch cargo. Paper-mill cargo also requires its transfer wharf to remain operational.
Long offline catch-up recalculates attendance for each historic day instead of reusing the latest
attendance snapshot.

## Streaming and physical residents

Exterior CI construction retains at most three distant chunks simultaneously, ignores incidental
route chunks outside the authoritative audit set and uses a larger write budget only in CI. Normal
player-driven streaming retains the lower runtime budget. Household samples use the same five
anchor chunks that prove each site's physical construction, avoiding synchronous or unbounded
chunk loading.

The old `ErdenPhysicalEconomyManager` fixed-import implementation has been deleted. Economy
constants, diagnostic entrances and all active processing now belong to
`ErdenAuthoritativeEconomyManager`. The population diagnostic retainer requests and forces sample
chunks without synchronously calling `getChunk`.

## Required regression markers

A fresh-world validation is successful only when it emits all of the following without a watchdog,
level-load exception or realm diagnostic failure:

- `LK_ERDEN_EXTERIOR_WORKFORCE_PASS`
- `LK_ERDEN_KINGDOM_EXTERIOR_PASS`
- `LK_ERDEN_KINGDOM_SUPPLY_PASS`
- `LK_ERDEN_LIVING_ECONOMY_PASS`

The workforce marker must prove attendance, scheduled rest, persistent death accounting and a
loaded resident sample. The supply marker must prove `workforce_linked=true`,
`staffed_production=true` and `wharf_labor=true` while retaining fixed-import removal and shipment
escrow.

The permanent workflow is `.github/workflows/audit-living-kingdoms-exterior-workforce.yml`.
