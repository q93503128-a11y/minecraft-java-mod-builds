# M2-B In-World Hub Loop Field Check

This checklist validates the connected first-expedition and post-extraction hub loop. The smithing table and lodestone used here are **technical vertical-slice affordances only**. They are not approved final hub art, UI, station design, naming, or interaction language.

## Build under test

Use a JAR produced by the `Build Riftfrontier` workflow for the checkpoint that contains the fresh-world `ExpeditionHubTerminal` bootstrap.

Do not mark this document PLAYTESTED until a human actually performs these steps in Minecraft. CI, GameTest, dedicated-server smoke and Xvfb client smoke do not count as human play.

## Fresh-world setup — no command bootstrap

1. Install the checkpoint JAR on the Minecraft 26.2 NeoForge test client/server used for Riftfrontier field checks.
2. Create and enter a **disposable fresh world** that has no Riftfrontier expedition history.
3. Expected on first server login: the player is routed to the bounded technical hub near `(0, 100, 0)` and receives the technical-hub message.
4. Expected: a smithing table is present two blocks west of hub center and a lodestone is present two blocks east of hub center.
5. Optionally run `/riftfrontier expedition status` and record starting `hubSalvage`, `supply`, `regionPressure`, and `nextSupplyCost`. The command is diagnostic only; it is no longer required to begin the first run.
6. Right-click the hub lodestone.
7. Expected: the existing authoritative `ExpeditionGameplayService.start(...)` path starts the first Region 01 expedition, spends the current preparation supply cost, persists the run/start context, spawns the planned encounter, and teleports the player into the technical Region 01 cell.
8. Expected: the station owns no separate expedition state and invents no balance value. `/riftfrontier expedition status` should describe the same authoritative run.

`/riftfrontier expedition start` remains available as a diagnostic/fallback command; this checklist deliberately verifies that ordinary first-slice traversal no longer depends on it.

## Field -> extraction -> hub

1. Recover at least three Region 01 amethyst salvage nodes by right-clicking them.
2. Use the field lodestone extraction relay at the far edge of the technical Region 01 cell.
3. Expected: authoritative extraction succeeds and the player returns to the technical hub.
4. Expected: the smithing table and lodestone are present again at their hub positions.
5. Expected: the extraction result message still reports retained salvage, patrol bonus, stored salvage, region pressure, and next supply cost. The stations must not replace or recompute those values.

## Provision station

1. Record `/riftfrontier expedition status` after extraction.
2. Right-click the hub smithing table once.
3. Expected when at least one secured salvage is available: exactly the existing authoritative provision operation occurs — one secured salvage is consumed and two expedition supply are added.
4. Run `/riftfrontier expedition status` again and compare the values.
5. Right-click again when storage is insufficient.
6. Expected: the existing provision rule rejects the action, the station reports the rejection, and no storage/supply value is duplicated or partially mutated.

## Deployment station — repeat cycle

1. Right-click the hub lodestone.
2. Expected: the same authoritative `ExpeditionGameplayService.start(...)` path begins the next Region 01 expedition using the current pressure-scaled supply cost.
3. Complete three salvage recoveries and extract through the field relay again.
4. Expected: the player returns to the hub and both technical stations are restored for another cycle.

## Fresh-world bootstrap must not reset established state

1. After at least one expedition has been recorded, note the current expedition sequence, storage, supply and Region 01 pressure.
2. Log out and log back in while no expedition is active.
3. Expected: the fresh-world bootstrap does **not** run again merely because the player logged in. Existing expedition history remains the discriminator.
4. Expected: no expedition sequence, storage, supply, pressure or terminal run state is reset or recreated.
5. If the previous logout/restart invalidated an active expedition, the existing reconciliation policy remains authoritative; the fresh bootstrap must not disguise that failure as a new clean world.

## World-response continuity

Across at least two successful extractions, record:

- expedition sequence;
- secured salvage before/after provisioning;
- expedition supply before/after provisioning and deployment;
- Region 01 pressure;
- next preparation supply cost.

Expected: the physical hub loop preserves the existing causal chain `field result -> secured salvage -> provisioned supply -> next deployment -> pressure-scaled preparation cost`. Station interaction must never invent its own balance values.

## Regression checks

- `/riftfrontier expedition start`, `provision`, `extract`, `abort`, `status`, and review commands remain diagnostic/fallback surfaces.
- Early field extraction is still rejected by the existing extraction gate.
- Death/logout/restart reconciliation behavior is unchanged.
- A world with existing expedition history is never treated as fresh merely because there is currently no active run.
- The hub stations and technical platform do not become final art. Final hub presentation still requires the project reference/design gate.
- No claim of multiplayer correctness is made until an actual multiplayer field session is performed.

## Result labels

Record separately:

- `CODE REVIEWED`
- `TESTED`
- `BUILD VERIFIED`
- `JAR PRODUCED`
- `PLAYTESTED`
- `MULTIPLAYER TESTED`

Never infer the last two from automation.
