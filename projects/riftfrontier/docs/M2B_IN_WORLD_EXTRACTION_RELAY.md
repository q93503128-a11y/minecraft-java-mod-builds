# M2-B In-World Extraction Relay Field Check

This is a human field-play checklist for the first Riftfrontier expedition loop. The lodestone relay is a **technical vertical-slice affordance**, not final Region 01 art, UI, portal, or extraction-device language.

## Goal

Prove that the playable loop can now close inside the Minecraft world:

`prepare/start -> deploy -> fight/recover -> choose extraction -> return to hub -> retain salvage -> provision -> next expedition`

The existing `/riftfrontier expedition extract` command remains a diagnostic/fallback surface. Normal field verification should use the in-world relay.

## Setup

Use the CI-produced Riftfrontier test JAR from a successful `Build Riftfrontier` run. Start a clean single-player/dev world or dedicated-server test world with commands available.

1. Run `/riftfrontier expedition status` and record supply, secured salvage and Region 01 pressure.
2. Ensure enough supply exists for a run; use the already-supported vertical-slice provisioning path if required.
3. Run `/riftfrontier expedition start`.
4. Confirm deployment to the technical Region 01 cell and the expected encounter population.

## Relay appearance

1. Right-click one Region 01 salvage amethyst node with the main hand.
2. Expected: normal salvage recovery still occurs.
3. Expected: a lodestone appears at the far `+Z` edge of the 11x11 technical field cell (`technicalRegionCenter + (0, 0, 5)`).
4. Expected: the player receives a message explaining that the lodestone is the temporary extraction relay.
5. Expected: the relay does not replace any of the five salvage nodes.

## Early extraction rejection

After recovering fewer than the contract-required three salvage units:

1. Right-click the lodestone with the main hand.
2. Expected: extraction is rejected with the authoritative contract-gate reason.
3. Expected: the player remains deployed in Region 01.
4. Expected: no extraction reward, hub salvage, patrol bonus, pressure change, or terminal extraction evidence is granted by the rejected attempt.

## Successful in-world extraction

1. Recover at least three salvage units.
2. Option A: leave patrol threats alive to exercise fast/risky extraction.
3. Option B: clear the patrol to exercise the existing patrol bonus path.
4. Right-click the lodestone.
5. Expected: extraction resolves through the same `ExpeditionGameplayService.extract(...)` authority used by the command path.
6. Expected: the run becomes terminal, the encounter is cleaned, the player returns to the hub, retained salvage is settled, pressure/supply-cost consequence is reported, and patrol bonus matches whether live threats remained.
7. Run `/riftfrontier expedition status` and verify the persisted result.

## Repeat-run reset

1. Start another expedition after satisfying its supply cost.
2. Expected: technical-cell preparation clears the old field state, including the previous relay.
3. On the first field block interaction of the new deployed run, the lodestone relay should materialize again at the same technical position.
4. No relay interaction from an inactive or non-owned run may grant extraction.

## Evidence labels

Passing CI/GameTest/server/client smoke proves automation only. Mark this feature `PLAYTESTED` only after a person performs the checks above in Minecraft. Mark `MULTIPLAYER TESTED` only after the same ownership/rejection/success paths are exercised with multiple connected players. Record observed failures instead of tuning around them without evidence.
