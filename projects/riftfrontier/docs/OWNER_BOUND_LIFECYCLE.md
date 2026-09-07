# Riftfrontier — Owner-Bound Expedition Lifecycle Contract

Status: **VERIFIED / MANUAL FIELD PLAY STILL REQUIRED**

## Verified baseline

Owner-bound lifecycle and historical field-review evidence are verified at code commit `d1830698da654102b29f36aa81881f5526e5cc76`, GitHub Actions `Build Riftfrontier` run `34131862928`.

That run passed:

- Java/Gradle toolchain verification
- clean + unit test + build
- required native GameTest gate
- dedicated server smoke
- Xvfb client smoke
- executable JAR inspection
- build report
- deliverable artifact upload
- log/report artifact upload

This verification closes an authoritative ownership bug. It does **not** close the manual Region 01 field-play, combat pacing, presentation, or M2 quality gate.

## Problem that this contract closes

M2 intentionally permits only one world-wide non-terminal expedition at a time. Before this contract, that global concurrency restriction was incorrectly also acting as player ownership: `failActive(player, ...)` selected the world's active run without proving that the event player owned it.

On a multiplayer server, an unrelated player's death or logout could therefore fail somebody else's active expedition. The same missing attribution boundary also affected recover/extract/abort/re-entry/review semantics.

World-wide concurrency and participant ownership are now separate facts.

## Authoritative run identity

A production `ExpeditionRun` now persists:

```text
sequence
region_id
contract_id
owner_uuid
content_fingerprint
status
recovered_resources
started_game_time
ended_game_time
end_reason
```

`owner_uuid` is immutable after allocation, just like sequence/region/contract. `RiftfrontierWorldData.updateExpedition` rejects owner changes.

The codec stores `owner_uuid` as an optional field. Existing saves and old test fixtures without the field decode as explicitly legacy-unowned records, so this addition does not require a persistence schema bump. New production gameplay runs are always owner-bound.

A server restart is already required to load changed mod code. Existing non-terminal legacy runs therefore pass through the established restart reconciliation and become terminal `FAILED` rather than being silently adopted by whichever player next interacts with the field.

## Player-scoped gameplay actions

The M2 world still has at most one non-terminal run, but player-originated gameplay operations use `activeFor(player, world)` and require `owner_uuid == player UUID`.

Owner-scoped operations:

- salvage recovery
- extraction
- explicit abort
- player death failure
- player logout failure
- stranded field re-entry decision
- field-play review history selection

An unrelated player's death/logout therefore produces no expedition mutation. An unrelated player cannot recover salvage, extract, or abort somebody else's run merely because it is the world's only active run.

Server restart reconciliation remains deliberately world-scoped: restart invalidates process-local encounter ownership for the one M2 active run regardless of whether its owner is online, and the terminal transition preserves that run's owner UUID.

## Re-entry semantics

Player login checks the caller's owned active run, not merely whether some expedition exists globally.

```text
caller owns active run + caller inside technical field
→ keep position

caller owns no active run + caller outside technical field
→ no movement

caller owns no active run + caller stranded inside technical field
→ explicit field exit to technical hub
```

Another player's active expedition must not suppress a stranded player's explicit field exit.

When selecting a prior terminal cause for the re-entry message, the adapter uses the caller's owner-bound history. Legacy-unowned records remain readable only as compatibility evidence and are never treated as a newly owned active run.

## Field-review attribution

`/riftfrontier expedition review` now emits `owner=<uuid>` (or `legacy-unowned` for historical compatibility) and selects the caller's expedition history.

The old pressure evidence rule (`latest successful run = current pressure - 1`) was only correct while review always meant the globally latest run. Owner-scoped history can contain an older run followed by successful runs from other players.

`ExpeditionReviewHistory` now reconstructs pressure at the target run from authoritative history:

```text
pressure_at_start
= current_region_01_pressure
- count(EXTRACTED runs whose sequence >= target sequence)
```

This is valid for the current M2 contract because Region 01 pressure advances exactly once for every successful extraction and never on failed/active runs. If persisted history and current pressure contradict that invariant, review fails loudly instead of fabricating evidence.

Pure unit tests cover cross-player historical success, failed middle runs, active runs, missing target history, and pressure/history drift.

## Test responsibility

JUnit verifies:

- owner identity is preserved by immutable transitions
- owner matching rejects another UUID
- legacy ownerless fixture compatibility
- owner survives extraction/failure
- historical pressure reconstruction across multiple owners
- invalid history fails loudly

Native GameTest verifies in actual Minecraft runtime:

- authoritative owner UUID allocation
- validated participant and persisted owner match
- owner survives SavedData updates through extraction
- restart reconciliation preserves owner UUID while failing the run
- existing encounter/restart/runtime gates continue to pass

The full CI then verifies dedicated server startup, client initialization under Xvfb, executable JAR structure and deliverable/report generation.

## Scope boundary

This is **not** multi-expedition or party ownership support. M2 still intentionally allows only one world-wide non-terminal expedition. The owner UUID exists to make that single-run model authoritative and safe, not to prematurely design M4/M5 multiplayer orchestration.

Do not add concurrent run allocation, party membership, claim systems, proximity ownership, or broad tick scans until a later milestone actually requires them.

## Exact next point

Do not revisit owner attribution, terminal causes, restart reconciliation, lure cleanup, or extraction atomicity unless a regression is observed.

The next M2 work remains manual Region 01 field-play evidence:

1. low-pressure full expedition;
2. elevated-pressure full expedition;
3. snapshot before first salvage, after each salvage, before terminal action, and immediately after terminal action;
4. normal extraction / abort / death / logout / restart re-entry passes;
5. verify the snapshot owner UUID and terminal cause match the actual participant/event;
6. judge spawn spacing, aggro/pacing, salvage hazard, and fast extraction versus patrol-clear bonus;
7. only tune numbers when a captured snapshot is paired with a concrete observed gameplay problem.

After that manual gate is actually completed, write the M3 combat/elite/boss reference dossier before creating production combat art or final enemy presentation.
