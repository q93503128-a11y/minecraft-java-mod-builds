# M2-B In-World Hub Loop Field Check

This checklist validates the first connected post-extraction hub loop. The smithing table and lodestone used here are **technical vertical-slice affordances only**. They are not approved final hub art, UI, station design, naming, or interaction language.

## Build under test

Use a JAR produced by the `Build Riftfrontier` workflow for the checkpoint that contains `ExpeditionHubTerminal`.

Do not mark this document PLAYTESTED until a human actually performs these steps in Minecraft. CI, GameTest, dedicated-server smoke and Xvfb client smoke do not count as human play.

## Setup

1. Install the checkpoint JAR on the Minecraft 26.2 NeoForge test client/server used for Riftfrontier field checks.
2. Enter a disposable test world with commands available.
3. Run `/riftfrontier expedition status` and record starting `hubSalvage`, `supply`, `regionPressure`, and `nextSupplyCost`.
4. Bootstrap the first run with `/riftfrontier expedition start`. This command remains a diagnostic/bootstrap surface until final contract/hub UI is reference-reviewed; this checklist does not promote it to final UX.

## Field -> extraction -> hub

1. Recover at least three Region 01 amethyst salvage nodes by right-clicking them.
2. Use the field lodestone extraction relay at the far edge of the technical Region 01 cell.
3. Expected: authoritative extraction succeeds and the player returns to the technical hub.
4. Expected: a smithing table is present two blocks west of hub center and a lodestone is present two blocks east of hub center.
5. Expected: the extraction result message still reports retained salvage, patrol bonus, stored salvage, region pressure, and next supply cost. The new stations must not replace or recompute those values.

## Provision station

1. Record `/riftfrontier expedition status` after extraction.
2. Right-click the hub smithing table once.
3. Expected when at least one secured salvage is available: exactly the existing authoritative provision operation occurs — one secured salvage is consumed and two expedition supply are added.
4. Run `/riftfrontier expedition status` again and compare the values.
5. Right-click again when storage is insufficient.
6. Expected: the existing provision rule rejects the action, the station reports the rejection, and no storage/supply value is duplicated or partially mutated.

## Deployment station

1. Right-click the hub lodestone.
2. Expected: the existing authoritative `ExpeditionGameplayService.start(...)` path begins the next Region 01 expedition, spends the current pressure-scaled supply cost, persists a new run/start context, spawns the planned encounter, and teleports the player into the Region 01 technical cell.
3. Expected: no separate station-owned expedition state exists; `/riftfrontier expedition status` must describe the same run that the command path would have created.
4. Complete three salvage recoveries and extract through the field relay again.
5. Expected: the player returns to the hub and both technical stations are restored for another cycle.

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
- The hub stations do not make the temporary vanilla blocks final art. Final hub presentation still requires the project reference/design gate.
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
