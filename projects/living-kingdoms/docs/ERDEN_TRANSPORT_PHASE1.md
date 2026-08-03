# Erden physical transport phase 1

## Why this layer exists

Erden's first physical-economy pass made warehouses, workshops, shops, households and wages persistent, but a stock transfer still crossed the capital instantly in the ledger. The transport layer exists to make distance, roads and obstruction matter without keeping the entire 2.4 x 1.8 km capital loaded.

The governing rule is unchanged:

- The whole capital remains a persistent aggregate simulation.
- Only activity near a player is materialised as entities.
- Materialised transport must follow authored roads rather than teleport through buildings.
- Unloaded districts never force-load solely to animate background commerce.

## Implemented

### Persistent delivery state

`ErdenTransportSavedData` stores:

- source and destination worksite IDs
- cargo resource and amount
- creation and phase ticks
- loading, moving, unloading, completed and failed states
- compressed road waypoints
- current waypoint and retry count
- porter and cart entity UUIDs
- modeled travel time
- cumulative manifest, physicalisation, completion, blockage and failure totals

The schema is save-backed and survives server restarts.

### Road routing

`ErdenTransportManager` plans routes over the same procedural road classification used to build the capital:

- royal roads are preferred
- district roads follow
- local streets remain valid
- building entrances are connected to the nearest authored road
- loaded road blocks are checked for a solid floor and two blocks of headroom
- unloaded roads use the authored network without forcing chunks to load

Routes are compressed into turning points and regular progress points so saved jobs remain bounded.

### Loading, movement and unloading

Player-near jobs can materialise as:

- a persistent villager porter named `왕도 짐꾼`
- a cargo-cart proxy for heavy loads

The job waits during loading, follows route waypoints, waits during unloading and then records completion. Interacting with the porter or cart displays cargo, quantity, source, destination, route length and current state in Korean.

### Obstruction and retry

A porter that stops making progress does not teleport through the obstruction.

- the route is recalculated from the nearest road
- blockage and delay metrics are recorded
- up to three reroutes are attempted
- an unresolved route becomes a failed delivery record

### Streaming boundary

At most 18 jobs are materialised around players. Jobs outside the materialisation radius discard their temporary entities while retaining their saved route state. The rest of the capital remains aggregate.

## Verified result

Build 474 tested commit `b0583858bf235f7dca0c6b486016f1ba30b453fe` with Java 25, NeoForge 26.2.0.38-beta, a fresh dedicated-server world and the graphical client test.

The fresh world reported:

- 303 road manifests
- 211,229 modeled travel ticks
- road route planning enabled
- loaded-road obstruction checks enabled
- persistent delivery jobs enabled
- physicalisation radius 224 blocks
- maximum 18 simultaneous materialised jobs
- unloaded routes retained as aggregate simulation

The same run also retained the previous economy result:

- 156 worksites
- 15 warehouses
- 77 household wallets
- 289 stock deliveries
- 890 production/service units
- 77/77 households supplied
- 42 bread reserved across 21 bakeries

The standard Living Kingdoms build workflow now permanently requires both the physical-economy marker and the transport marker, and verifies that the transport manager and saved-data classes are packaged.

## Deliberate limits of phase 1

This phase makes delivery routes, time, obstruction and nearby movement persistent and visible. It does **not yet** delay the authoritative target inventory until the visible cart finishes unloading. The existing daily economy still settles stock in aggregate, and the transport job records and materialises the matching logistics layer.

The current cargo cart is also a Minecraft chest-minecart proxy moved behind the porter. It is a functional transport placeholder, not the final custom medieval wagon model.

## Next phase

1. Move cargo into an in-transit escrow when a delivery departs.
2. Credit the destination only after successful unloading.
3. Return, reroute or lose cargo according to a blocked or failed route.
4. Replace the chest-minecart proxy with a purpose-built medieval handcart and wagon entity.
5. Add road congestion, gate opening schedules, porter shifts and guarded high-value convoys.
6. Test live player-caused road obstruction and recovery in a retained world.
