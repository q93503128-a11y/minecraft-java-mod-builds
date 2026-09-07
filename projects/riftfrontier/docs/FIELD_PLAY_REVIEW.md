# Region 01 Field-Play Review Contract

Status: **PERSISTED START CONTEXT VERIFIED / MANUAL FIELD PLAY STILL REQUIRED**

This document defines how Region 01 is reviewed after the automated M2-B lifecycle, ownership, restart and evidence-integrity gates. It does not replace manual Minecraft client play and must not be used to claim that combat pacing, readability, encounter spacing or reward pressure are complete.

## Verified evidence baseline

Terminal-cause persistence baseline: code commit `460a8d22cbf19ba4286e4a7f8e581692c850b186`, GitHub Actions `Build Riftfrontier` run `34129296652`.

Owner-bound lifecycle and participant-aware historical review baseline: code commit `d1830698da654102b29f36aa81881f5526e5cc76`, GitHub Actions `Build Riftfrontier` run `34131862928`.

Persisted expedition start-context baseline: code commit `7ca0d83d7aa295317e87a9cad2a96f9dc8f9e647`, GitHub Actions `Build Riftfrontier` run `34137132672`.

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

New production runs also persist an immutable `start_context` containing the exact Region 01 pressure, preparation-supply cost, planned hunter/scout/elite counts, salvage-hazard duration and hazard amplifier that authored that run. The runtime uses the captured pressure for encounter startup and salvage hazard rather than silently consulting a later world-pressure value.

`owner_uuid`, `end_reason` and `start_context` are backward-compatible optional codec fields. Legacy records decode without inventing a participant, terminal cause or historical tuning state, so these evidence additions do not require a persistence schema bump. New production gameplay runs always persist the real initiating player UUID, explicit terminal reason and start context.

Detailed ownership rules live in `OWNER_BOUND_LIFECYCLE.md`.

## Read-only evidence command

Use `/riftfrontier expedition review` during a run and immediately after a terminal result. The command does not mutate expedition, encounter, economy or content state.

The one-line snapshot records:

- authoritative run sequence;
- participant owner UUID (`legacy-unowned` only for compatibility records);
- status and stable terminal `endReason`;
- elapsed game ticks;
- recovered Region 01 salvage;
- pressure used for that run;
- planned hunter/scout/elite composition and active live-threat count;
- salvage hazard duration/intensity for that run;
- current hub salvage, expedition supply and next preparation cost;
- `startContext=persisted` for new exact evidence or `startContext=legacy-reconstructed` for compatibility evidence;
- whether the run's content fingerprint still matches the currently published content snapshot.

For an active run `endReason=none` is required. For a new terminal run, the concrete end reason must match the event that ended it. For a terminal run `liveThreats=terminal` is intentional: cleanup has already destroyed process-local encounter handles, so the review layer must not fabricate a post-cleanup threat count.

Review history is participant-aware. It selects the caller's owner-bound runs instead of silently displaying another player's latest run.

## Historical start-context rule

For every new production run, review must use the persisted `start_context` as the authoritative source of historical pressure, preparation cost, encounter composition and hazard tuning. It must not recompute those values from the current world pressure or whatever formulas happen to exist when the review command is run later.

This is important because field-play evidence may be inspected after additional successful runs, after another player's run, or after later tuning changes. A snapshot must describe the run that actually happened, not a plausible reconstruction using today's formulas.

Legacy runs written before `start_context` remain readable. For those records only, the M2 compatibility path reconstructs pressure from authoritative expedition history using the bounded one-increment-per-success contract, then labels the result `startContext=legacy-reconstructed`. Reconstructed evidence must never be presented as exact persisted evidence.

The legacy reconstruction rule remains:

```text
pressure_at_start
= current_region_01_pressure
- count(EXTRACTED runs whose sequence >= target sequence)
```

If the target is absent from authoritative history or the persisted pressure/history relationship is impossible, evidence capture fails loudly rather than emitting a plausible-looking false value. New production records must not depend on this equation.

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

Server restart remains intentionally world-scoped: it invalidates the one active run regardless of whether its owner is online, while preserving the owner UUID and persisted start context on the resulting `FAILED/server_restart` record.

## Manual review passes

Capture at least one review line before the first salvage, after each salvage recovery, before extraction, and after extraction/failure. Repeat across low and elevated pressure.

Every snapshot for a new production run must show the UUID of the player actually performing the run and `startContext=persisted`. For lifecycle-edge passes, verify both participant and terminal cause in the post-event review line:

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

Automated tests may verify snapshot invariants, owner isolation, exact start-context preservation for new runs, explicit legacy reconstruction, terminal-cause persistence, GameTest integration and command/build integration, but **cannot close this manual field-play gate**.

## Exact next point

Do not revisit owner attribution, terminal causes, restart reconciliation, lure cleanup, extraction atomicity or start-context persistence without a demonstrated regression.

The next work is an actual Minecraft client field-play pass at low and elevated pressure. Capture `startContext=persisted` owner-aware review snapshots before/after salvage and terminal actions, exercise extraction/abort/death/logout/restart, and judge spawn spacing, aggro/pacing, hazard fairness and fast-extract versus patrol-clear reward pressure. Only evidence-backed problems should change numbers. After that manual gate is genuinely complete, prepare the M3 combat/elite/boss reference dossier before final enemy art or presentation work.
