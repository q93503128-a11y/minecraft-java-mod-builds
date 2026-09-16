# EARTH TO STARS — Fabric Ship Authority Bridge

This document records the first server-authoritative ship integration unit after the Fabric 26.2 standalone/content rebase. It does not claim that a player-facing starter craft, live flight or multiplayer playtest exists yet.

## Verified source

- implementation commit: `1f93680b8076aa1022b67dcd944ce40f5eeeb1cd`
- Minecraft 26.2 API-alignment commit: `06ca1ecda7d42e8b6451ab5b434147458cc020d0`
- Minecraft: `26.2`
- Fabric Loader: `0.19.5`
- Fabric API: `0.160.0+26.2`
- Java: `25`
- Mod version: `0.3.0-alpha.1`

## Implemented authority contract

The Fabric artifact now exposes a real authority bridge around the retained loader-neutral ship kernel instead of creating a second Fabric-only ship state.

### Network contracts

`ShipControlInputPayload` is client-to-server input only. It carries:

- stable `ShipId`
- a server-issued control-session UUID
- monotonic input sequence
- throttle / yaw / lift axes

`ShipControlSessionPayload` is server-to-client session state. It tells the client whether a control session for a specific ship is active and, when active, which server-issued session UUID is valid.

The client has no packet that can create a ship, grant itself `PILOT`, mint a valid control session or directly mutate ETS logical ship state.

### Server authority

`EarthToStarsFabricShipAuthority` owns the Fabric runtime bridge:

- one authoritative `ShipRepository`
- active physical-flight runtime bindings by `ShipId`
- controller-to-ship bindings by player UUID
- lifecycle load/reset on server start/stop
- persistent add/update/remove operations
- server-side control grant/release
- C2S input validation before forwarding to the existing `ShipFlightRuntime`

The existing kernel remains responsible for important control invariants: `PILOT` permission, one active controller, random session UUID, monotonic sequence rejection, 40-tick lease expiry and neutral input after expiry.

A future physical craft implementation must validate that the player is interacting with the correct real craft, seat, range and world before calling `grantControl`. The bridge deliberately does not create a fake craft or provide a client request that bypasses those physical checks.

### Persistence

`EarthToStarsFabricShipSavedData` persists authoritative logical ships with the existing binary `ShipStateCodec`, Base64-wrapped in Fabric/vanilla `SavedData` storage.

Important properties:

- stable `ShipId` is preserved;
- the existing ship schema/migration codec remains the source of truth;
- corrupt payloads fail closed instead of silently resetting a ship;
- persisted map key and decoded `ShipId` must agree;
- server start reconstructs the authoritative repository from persisted states;
- server stop clears static runtime bindings to avoid leaking world state between integrated/dedicated server lifetimes.

## Failure found and corrected

The first authority build, `Build earth-to-stars Fabric 26.2` run `35050092286`, passed the standalone contract validator but failed during Java compilation.

The first real error was the `SavedDataType` constructor in `EarthToStarsFabricShipSavedData`: Minecraft 26.2 uses the current four-argument constructor, while the initial bridge used the older three-argument shape.

Commit `06ca1ecda7d42e8b6451ab5b434147458cc020d0` added the current DataFixer argument position without changing the ETS save key or `ShipStateCodec`. No ship feature was deleted, stubbed or excluded to obtain a green build.

## Verification

`Build earth-to-stars Fabric 26.2` run `35050302544` / run number `#40`: **PASS**

- standalone Fabric contract validator: PASS
- loader-neutral ship kernel tests: PASS
- clean test/build: PASS
- production JAR verify: PASS
- dedicated Fabric server boot: PASS
- production JAR: `earth_to_stars-0.3.0-alpha.1.jar`
- SHA-256: `40034f8310fcf9654cf78c19a84c6c218e8ee74888314643da4a174573425109`

`Smoke earth-to-stars Fabric client` run `35050302541` / run number `#15`: **PASS**

- Xvfb: PASS
- Fabric client preparation: PASS
- `runClient` smoke: PASS

Current status:

- `CODE REVIEWED`: YES
- `TESTED`: YES for automated kernel/contract/build/runtime-smoke gates
- `BUILD VERIFIED`: YES
- `JAR PRODUCED`: YES
- `DEDICATED FABRIC BOOT`: YES
- `CLIENT FABRIC SMOKE`: YES
- `PLAYTESTED`: NO
- `LIVE FLIGHT FEEL`: NOT RUN
- `EARTH→SPACE CONTINUITY`: NOT TESTED
- `MULTIPLAYER TESTED`: NO

The smoke gates demonstrate that the bridge compiles and initializes on server/client. They do not demonstrate that a physical craft moves, collides, looks good or feels good to fly.

## Next gate — physical starter craft

Do not expand planets or combat content yet. The next unit is:

```text
verify exact permissive vehicle/entity source + license + commit/version
→ implement server-authoritative launch/deploy transaction
→ bind one real physical starter-craft entity to stable ShipId
→ render the approved Kenney CC0 starter-craft production asset through a Fabric-compatible path
→ validate real interaction/seat/range/world state before control grant
→ wire client control input + camera
→ implement/tune collision and movement
→ consume actual ETS power/propellant/oxygen during flight
→ live client acceptance test
```

The logical `ShipState` remains authoritative and the physical entity should carry only the stable identity/runtime data needed to represent that ship in the Minecraft world. Do not fork a second economy, module inventory, fuel store or ownership model into the entity.

Physical movement/collision should be informed by current permissively licensed Fabric/current-Minecraft vehicle implementations before original low-level behavior is invented. Candidate projects are research leads only until their exact source revision and license obligations are verified and recorded.
