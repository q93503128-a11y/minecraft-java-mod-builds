# Erden kingdom supply-chain validation

## Scope

The capital no longer receives raw materials from a fixed daily inventory injection. Rural production and shipment escrow are now the only automatic source of wheat, coal, leather, paper, iron and hay entering the 15 capital warehouses.

This is the first kingdom-scale supply phase. It establishes authoritative production, dispatch, transit and warehouse settlement before the exterior sites themselves are built as complete player-visible settlements.

## Persistent supply network

The saved world state contains 18 exterior supply nodes:

- 4 grain estates
- 3 ranches
- 3 collieries
- 2 iron mines
- 3 paper mills
- 3 river wharves

The 15 producing sites retain their own stock, last production day, cumulative output and blocked-route count. The three wharves are transfer anchors used by paper-mill barge routes.

## Resources and daily output

Normal output is generated at the producer, not at the capital warehouse:

- wheat: 48 per grain estate
- leather: 40 per ranch
- hay: 24 per ranch
- coal: 28 per colliery
- iron: 16 per mine
- paper: 40 per paper mill

A bounded seven-day productivity cycle changes daily output between 85% and 110%. Route blocks are deterministic and delay stock at the producer instead of deleting or duplicating it.

## Opening reserves

A new or migrated world receives 18 recorded opening convoys representing production completed before the player's arrival. The cargo is removed from exterior-node stock, placed in shipment escrow and settled into a selected warehouse through the same arrival path used by later shipments.

Opening reserves are not repeated. After initialization, warehouse supply depends exclusively on newly produced and delivered cargo.

## Shipment escrow

Every shipment stores:

- unique shipment ID
- source node and destination warehouse
- resource and amount
- departure and arrival ticks
- transport mode
- modeled route length in metres
- status
- whether it belongs to the one-time opening convoy

Wagon routes use the nearest capital gate. Paper-mill shipments use a river wharf and barge mode. Travel duration is derived from modeled route length and cannot be shorter than the minimum loading and travel window.

Cargo is deducted from the producer when the shipment is dispatched. It is credited to the warehouse only when the arrival tick is reached. Settled shipments are retained for seven in-game days for audit and then pruned.

## Capital integration

The execution order is fixed:

1. Capture player changes from loaded warehouse containers.
2. Settle exterior cargo that has reached its destination.
3. Produce and dispatch missing supply days.
4. Run the capital workshop and household economy.
5. Materialize the resulting warehouse and workshop stocks back into containers.

Warehouse interaction now reports cumulative exterior receipts separately from internal capital transfers.

The removed system previously added the following bundle to every warehouse every day: wheat 96, coal 32, leather 24, paper 32, iron 20 and hay 40. The method and its `imports` metric no longer exist.

## Permanent regression marker

A fresh-world supply audit must emit:

`LK_ERDEN_KINGDOM_SUPPLY_PASS revision=1 nodes=18 producers=15 wharves=3 resources=6 opening_convoys=18`

The same marker must also prove:

- produced cargo is non-zero
- dispatched cargo is non-zero
- received cargo is non-zero
- at least four capital warehouses received exterior cargo
- normal shipments remain in transit after opening reserves settle
- fixed daily imports are disabled
- shipment escrow is enabled
- wagon and barge routes are both present

The physical-economy pass marker must include `kingdom_supply=true`, and all 77 household purchase outcomes, bakery reserve conservation and authoritative internal transport must still pass.

## Remaining exterior work

This phase does not claim that the kingdom exterior is complete. The next stage is to materialize these saved supply nodes as full farms, ranches, mines, paper mills and river ports with:

- authored terrain and structures
- fields, livestock and extractable resource faces
- resident workers and work shifts
- loading yards and warehouse containers
- roads, gates, bridges and river lanes matching the stored routes
- visible wagon and barge loading, travel and unloading
- production loss when workers, tools, animals or routes are unavailable

The kingdom is not ready for the user's completion test until that exterior layer and the remaining world, law, ecology and interior work are finished.
