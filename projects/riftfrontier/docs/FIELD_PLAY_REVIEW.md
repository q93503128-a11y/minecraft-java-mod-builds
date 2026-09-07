# Region 01 Field-Play Review Contract

Status: **EVIDENCE CAPTURE READY / OWNER + TERMINAL CAUSE VERIFIED / MANUAL FIELD PLAY STILL REQUIRED**

This document defines how Region 01 is reviewed after the automated M2-B lifecycle, ownership and restart gates. It does not replace manual Minecraft client play and must not be used to claim that combat pacing, readability, encounter spacing or reward pressure are complete.

## Verified evidence baseline

Terminal-cause persistence baseline: code commit `460a8d22cbf19ba4286e4a7f8e581692c850b186`, GitHub Actions `Build Riftfrontier` run `34129296652`.

Owner-bound lifecycle and participant-aware historical review baseline: code commit `d1830698da654102b29f36aa81881f5526e5cc76`, GitHub Actions `Build Riftfrontier` run `34131862928`.

The current baseline passed clean/unit build, required native GameTest, dedicated-server smoke, Xvfb client smoke, executable JAR inspection, build report, and deliverable/log artifact upload.

The authoritative `ExpeditionRun` records stable machine-readable participant/terminal evidence:

- `owner_uuid` for new production runs;
- `end_reason=extraction`;
- `end_reason=player_abort`;
- `end_reason=player_death`;
- `end_reason=player_logout`;
- `end_reason=server_restart`;
- `end_reason=other_failure` for future explicit failure adapters;
- `end_reason=none` only for non-terminal runs or legacy terminal records written before this field existed.

`owner_uuid` and `end_reason` are backward-compatible optional codec fields. Legacy records decode without inventing a participant or terminal cause, so these evidence additions do not require a persistence schema bump. New production gameplay runs always persist the real initiating player UUID and new terminal transitions always write an explicit reason.

Detailed ownership rules live in `OWNER_BOUND_LIFECYCLE.md`.

## Read-only evidence command

Use `/riftfrontier expedition review` during a run and immediately after a terminal result. The command does not mutate expedition, encounter, economy or content state.

The one-line snapshot records:

- authoritative run sequence;
- participant owner UUID (`legacy-unowned` only for compatibility records);
- status and stable terminal `endReason`;
- elapsed game ticks;
- recovered Region 01 salvage;
- pressure used for that historical run;
- planned hunter/scout/elite composition and active live-threat count;
- salvage hazard duration/intensity derived from that run pressure;
- current hub salvage, expedition supply and next preparation cost;
- whether the run's content fingerprint still matches the currently published content snapshot.

For an active run `endReason=none` is required. For a new terminal run, the concrete end reason must match the event that ended it. For a terminal run `liveThreats=terminal` is intentional: cleanup has already destroyed process-local encounter handles, so the review layer must not fabricate a post-cleanup threat count.

Review history is participant-aware. It selects the caller's owner-bound runs instead of silently displaying another player's latest run.

## Historical pressure reconstruction

The old bounded rule (`latest successful extraction = current pressure - 1`) was only exact for the globally latest run. Once review became participant-aware, a player's latest run can be older than successful runs completed later by another player.

Current M2 pressure advances exactly once for each successful `EXTRACTED` run and never for failed/active runs. Therefore review reconstructs the target run's encounter pressure from authoritative history:

```text
pressure_at_start
= current_region_01_pressure
- count(EXTRACTED runs whose sequence >= target sequence)
```

This works for historical extracted, failed and active runs across multiple owners. If the target is absent from authoritative history or the persisted pressure/history relationship is impossible, evidence capture fails loudly rather than emitting a plausible-looking false value.

This remains an M2 diagnostic rule tied to the current one-increment-per-success pressure contract. If later milestones add independent pressure changes, pressure-at-start must become an explicit persisted fact or gain a versioned event ledger rather than extending this equation by guesswork.

## Re-entry boundary

Restart or logout failure can leave a player's persisted position inside the bounded technical Region 01 cell after that player's authoritative expedition has already become terminal. On `PlayerLoggedInEvent`, Riftfrontier evaluates a pure re-entry decision from two caller-specific facts: whether that player owns the active expedition, and whether the player's actual login position is inside the technical Region 01 cell.

- caller owns active expedition + technical field position: do not move the player;
- caller owns no active expedition + outside technical field: do not move the player;
- caller owns no active expedition + still inside technical field: explicit field exit to the technical hub.

Another player's active expedition must not suppress the stranded caller's field exit. The return message uses the caller's owner-bound terminal history when available and includes the exact persisted cause. This lets restart and logout re-entry be reviewed as different lifecycle outcomes instead of both appearing as an undifferentiated `FAILED` run.

This remains event-driven. It does not scan the world, infer entity ownership from proximity, resurrect a reconciled run, refund supply, or teleport players who are not stranded in the technical field cell.

The adapter closes automatically verifiable lifecycle/position/participant/evidence inconsistencies, but whether the re-entry message and transition feel good in actual play remains part of the manual field-play gate.

## Automated cross-player isolation boundary

M2 still permits only one world-wide non-terminal expedition. That concurrency rule is not participant ownership.

New production runs persist the initiating player's UUID. Player-originated recover/extract/abort/death/logout/re-entry/review operations require that owner. An unrelated player's death or logout therefore cannot fail the active owner's run, and an unrelated player cannot recover or extract that run merely because it is the world's only active expedition.

Server restart remains intentionally world-scoped: it invalidates the one active run regardless of whether its owner is online, while preserving the owner UUID on the resulting `FAILED/server_restart` record.

## Manual review passes

Capture at least one review line before the first salvage, after each salvage recovery, before extraction, and after extraction/failure. Repeat across low and elevated pressure.

Every snapshot for a new production run must show the UUID of the player actually performing the run. For lifecycle-edge passes, verify both participant and terminal cause in the post-event review line:

```text
normal extraction → correct owner + endReason=extraction
manual abort      → correct owner + endReason=player_abort
death             → correct owner + endReason=player_death
logout            → correct owner + endReason=player_logout
server restart    → preserved owner + endReason=server_restart
```

When a second player is available during multiplayer review, also verify that the non-owner can die/logout without changing the owner's active run.

The human reviewer must still judge:

1. spawn spacing and whether hunter/scout/elite roles are legible in motion;
2. aggro and combat pacing rather than only total enemy count;
3. whether salvage slowness produces a meaningful but fair risk window;
4. whether fast extraction versus patrol-clear bonus is a real choice;
5. death, logout, abort, extraction and restart re-entry UX, with exact persisted owner/cause evidence;
6. whether technical proxy behaviour is good enough to inform M3 without mistaking proxy art for final design.

## Evidence rule

Do not change pressure scaling, hazard duration, reward bonus or role composition merely because a number looks high or low. A tuning change needs a captured run snapshot plus an observed gameplay problem. Record both the before/after snapshot and the concrete symptom.

Automated tests may verify snapshot invariants, owner isolation, historical pressure reconstruction, terminal-cause persistence, GameTest integration and command/build integration, but **cannot close this manual field-play gate**.

## Exact next point

Do not revisit owner attribution, terminal causes, restart reconciliation, lure cleanup or extraction atomicity without a demonstrated regression.

The next work is an actual Minecraft client field-play pass at low and elevated pressure. Capture owner-aware review snapshots before/after salvage and terminal actions, exercise extraction/abort/death/logout/restart, and judge spawn spacing, aggro/pacing, hazard fairness and fast-extract versus patrol-clear reward pressure. Only evidence-backed problems should change numbers. After that manual gate is genuinely complete, prepare the M3 combat/elite/boss reference dossier before final enemy art or presentation work.
