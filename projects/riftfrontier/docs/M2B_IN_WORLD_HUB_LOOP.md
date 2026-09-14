# M2-B In-World Hub Loop Field Check

This checklist validates the connected first-expedition and post-extraction hub loop. The smithing table and lodestone used here are **technical vertical-slice affordances only**. They are not approved final hub art, UI, station design, naming, or interaction language. The localized actionbar added at the latest checkpoint is likewise a Minecraft-native readability aid, not final Riftfrontier HUD design.

## Build under test

Use the `riftfrontier-0.1.0-alpha.1-deliverables` artifact from `Build Riftfrontier` workflow `34821369460`, successful attempt 2, commit `fded37cbe91668af213af477ae0b0238ac455693`.

Expected artifact digest:

`sha256:1ffbf1ef086a348052c697bfc983c404da283f3d2ce54db5d32d82063e607659`

Do not mark this document PLAYTESTED until a human actually performs these steps in Minecraft. CI, GameTest, dedicated-server smoke and Xvfb client smoke do not count as human play.

## Fresh-world setup — no command bootstrap

1. Install the checkpoint JAR on the Minecraft 26.2 NeoForge test client/server used for Riftfrontier field checks.
2. Create and enter a **disposable fresh world** that has no Riftfrontier expedition history.
3. Expected on first server login: the player is routed to the bounded technical hub near `(0, 100, 0)` and receives the technical-hub chat message.
4. Expected actionbar: `Lodestone: deploy • Smithing table: provision` in English, or the matching localized Korean text when the client language is Korean.
5. Expected: a smithing table is present two blocks west of hub center and a lodestone is present two blocks east of hub center.
6. Optionally run `/riftfrontier expedition status` and record starting `hubSalvage`, `supply`, `regionPressure`, and `nextSupplyCost`. The command is diagnostic only; it is no longer required to begin the first run.
7. Right-click the hub lodestone.
8. Expected: the existing authoritative `ExpeditionGameplayService.start(...)` path starts the first Region 01 expedition, spends the current preparation supply cost, persists the run/start context, spawns the planned encounter, and teleports the player into the technical Region 01 cell.
9. Expected actionbar after deployment: Region 01, current salvage objective progress (`0/3` initially), and the authoritative pressure captured for the run.
10. Expected: the station and actionbar own no separate expedition state and invent no balance value. `/riftfrontier expedition status` should describe the same authoritative run.

`/riftfrontier expedition start` remains available as a diagnostic/fallback command; this checklist deliberately verifies that ordinary first-slice traversal no longer depends on it.

## Field -> extraction -> hub

1. Recover Region 01 amethyst salvage nodes by right-clicking them.
2. After each successful authoritative recovery, expected actionbar: current salvage progress, current live patrol threat count, and the reminder that clearing the patrol grants the existing +1 bonus.
3. Expected: the first field interaction materializes the temporary field lodestone relay when its location is free. Its actionbar only announces relay availability; it must not mutate objective progress or extraction eligibility.
4. Recover at least three Region 01 amethyst salvage nodes total.
5. Use the field lodestone extraction relay at the far edge of the technical Region 01 cell.
6. Expected: authoritative extraction succeeds and the player returns to the technical hub.
7. Expected: the smithing table and lodestone are present again at their hub positions.
8. Expected: existing detailed chat still reports retained salvage, patrol bonus, stored salvage, region pressure, and next supply cost.
9. Expected actionbar after extraction: stored salvage, authoritative Region 01 pressure and next preparation supply cost. These values must agree with `/riftfrontier expedition status`; the actionbar must never recompute them independently.

## Provision station

1. Record `/riftfrontier expedition status` after extraction.
2. Right-click the hub smithing table once.
3. Expected when at least one secured salvage is available: exactly the existing authoritative provision operation occurs — one secured salvage is consumed and two expedition supply are added.
4. Expected actionbar: current expedition supply, remaining stored salvage, and next deployment cost.
5. Run `/riftfrontier expedition status` again and compare the values.
6. Right-click again when storage is insufficient.
7. Expected: the existing provision rule rejects the action, the station reports the rejection, and no storage/supply value is duplicated or partially mutated.

## Deployment station — repeat cycle

1. Right-click the hub lodestone.
2. Expected: the same authoritative `ExpeditionGameplayService.start(...)` path begins the next Region 01 expedition using the current pressure-scaled supply cost.
3. Expected actionbar: the new run begins at salvage `0/3` with that run's authoritative pressure.
4. Complete three salvage recoveries and extract through the field relay again.
5. Expected: the player returns to the hub and both technical stations are restored for another cycle.

## Fresh-world bootstrap must not reset established state

1. After at least one expedition has been recorded, note the current expedition sequence, storage, supply and Region 01 pressure.
2. Log out and log back in while no expedition is active.
3. Expected: the fresh-world bootstrap does **not** run again merely because the player logged in. Existing expedition history remains the discriminator.
4. Expected: no expedition sequence, storage, supply, pressure or terminal run state is reset or recreated.
5. If the previous logout/restart invalidated an active expedition, the existing reconciliation policy remains authoritative; the fresh bootstrap must not disguise that failure as a new clean world.

## Actionbar readability / localization checks

1. Verify at least one complete cycle with `en_us` and, when practical, one with `ko_kr`.
2. Confirm the actionbar is transient and readable without opening a menu; it must not create a permanent custom overlay or hide the existing boss bar during boss review.
3. Confirm chat retains the more detailed diagnostic/result messages. The actionbar is concise state feedback, not a replacement source of authority.
4. Confirm successive recovery interactions replace the previous actionbar state instead of stacking stale lines.
5. Confirm rejected provision/extraction/start actions do not display a false success-state actionbar.
6. Any mismatch between actionbar numbers and `/riftfrontier expedition status` is a failure even if the underlying gameplay result is correct.

## World-response continuity

Across at least two successful extractions, record:

- expedition sequence;
- secured salvage before/after provisioning;
- expedition supply before/after provisioning and deployment;
- Region 01 pressure;
- next preparation supply cost.

Expected: the physical hub loop preserves the existing causal chain `field result -> secured salvage -> provisioned supply -> next deployment -> pressure-scaled preparation cost`. Station interaction and actionbar feedback must never invent their own balance values.

## Regression checks

- `/riftfrontier expedition start`, `provision`, `extract`, `abort`, `status`, and review commands remain diagnostic/fallback surfaces.
- Early field extraction is still rejected by the existing extraction gate.
- Death/logout/restart reconciliation behavior is unchanged.
- A world with existing expedition history is never treated as fresh merely because there is currently no active run.
- The hub stations, technical platform and actionbar wording do not become final art/UI. Final hub presentation still requires the project reference/design gate.
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
