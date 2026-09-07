# Region 01 Field-Play Review Contract

Status: **EVIDENCE CAPTURE READY / MANUAL FIELD PLAY STILL REQUIRED**

This document defines how Region 01 is reviewed after the automated M2-B lifecycle and restart gates. It does not replace manual Minecraft client play and must not be used to claim that combat pacing, readability, encounter spacing or reward pressure are complete.

## Read-only evidence command

Use `/riftfrontier expedition review` during a run and immediately after a terminal result. The command does not mutate expedition, encounter, economy or content state.

The one-line snapshot records:
- authoritative run sequence and status;
- elapsed game ticks;
- recovered Region 01 salvage;
- pressure used for the latest run;
- planned hunter/scout/elite composition and active live-threat count;
- salvage hazard duration/intensity derived from that run pressure;
- current hub salvage, expedition supply and next preparation cost;
- whether the run's content fingerprint still matches the currently published content snapshot.

For a terminal run `liveThreats=terminal` is intentional. Cleanup has already destroyed process-local encounter handles, so the review layer must not fabricate a post-cleanup threat count.

For the latest successful extraction, the run pressure is reconstructed as `current region pressure - 1`, because Region 01 advances pressure exactly once on successful settlement. Failed and non-terminal runs use current pressure. This is a bounded M2 diagnostic rule, not a general historical telemetry system.

## Re-entry boundary

Restart or logout failure can leave a player's persisted position inside the bounded technical Region 01 cell after the authoritative expedition has already become terminal. On `PlayerLoggedInEvent`, Riftfrontier now evaluates a pure re-entry decision from two facts only: whether an expedition is still active, and whether the player's actual login position is inside the technical Region 01 cell.

- active expedition + technical field position: do not move the player;
- no active expedition + outside technical field: do not move the player;
- no active expedition + still inside technical field: explicit field exit to the technical hub with a message explaining that the previous expedition is no longer active and spent preparation supply remains consumed.

This is intentionally event-driven. It does not scan the world, infer entity ownership from proximity, resurrect a reconciled run, refund supply, or teleport players who are not stranded in the technical field cell.

The adapter closes an automatically verifiable lifecycle/position inconsistency, but whether the re-entry message and transition feel good in actual play remains part of the manual field-play gate.

## Manual review passes

Capture at least one review line before the first salvage, after each salvage recovery, before extraction, and after extraction/failure. Repeat across low and elevated pressure.

The human reviewer must still judge:
1. spawn spacing and whether hunter/scout/elite roles are legible in motion;
2. aggro and combat pacing rather than only total enemy count;
3. whether salvage slowness produces a meaningful but fair risk window;
4. whether fast extraction versus patrol-clear bonus is a real choice;
5. death, logout, abort, extraction and restart re-entry UX, including whether the explicit login field-exit message is understandable;
6. whether technical proxy behaviour is good enough to inform M3 without mistaking proxy art for final design.

## Evidence rule

Do not change pressure scaling, hazard duration, reward bonus or role composition merely because a number looks high or low. A tuning change needs a captured run snapshot plus an observed gameplay problem. Record both the before/after snapshot and the concrete symptom.

Automated tests may verify snapshot invariants and command/build integration, but **cannot close this manual field-play gate**.
