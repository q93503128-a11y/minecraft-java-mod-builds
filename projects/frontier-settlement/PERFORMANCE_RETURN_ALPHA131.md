# Alpha.131 — Deep-work return recovery and performance smoothing

## Real-play symptom

Quarry residents can progressively work below the original terrain surface, then spend too long or fail to return to their local profession barrel. The same play session also reports recurring integrated-server hitches despite the settlement game being smaller in feature count than many commercial games/modpacks.

## Bounded gameplay fix

- Quarry and mine residents always attempt ordinary physical return navigation first.
- Only while carrying real cargo toward an available local profession barrel, a 240-tick (12-second) return timer runs.
- When that timer expires, the same physical resident and unchanged MAINHAND ItemStack move to a safe walkable cell at that resident's own workplace.
- The timer does not apply to outbound gathering, farms/lumber, or central-storage overflow.
- No resource is duplicated, deleted, virtualized or force-loaded.

## Static performance findings

This patch intentionally separates code evidence from profiling proof. Current worker code has several burst-shaped costs:

- a lumber miss can inspect up to 129 x 129 surface columns and several vertical candidates;
- a quarry miss searches the bounded radius-40 surface/overburden envelope;
- a mine miss can inspect a radius-32 horizontal envelope through up to 80 blocks of depth;
- candidate validation repeatedly checks settlement protection state;
- route acquisition can test multiple vanilla paths around one target.

Alpha.129 already bounded/cached these searches, but a no-target result retried after exactly 100 ticks for every worker. Multiple residents could therefore align their large miss scans on the same integrated-server tick. Alpha.131 changes only the miss cadence: 200 ticks plus a deterministic 0-190 tick per-worker offset. Average no-target rescanning drops and the remaining work is spread across time.

## What this does not claim

This is not a profiler result. It removes one confirmed burst pattern and one observed return-path failure mode, but the dominant remaining hitch in the user's real world must be measured. If real play still hitches, capture Java Flight Recorder or spark data during the symptom and compare `SettlementWorkerService`, vanilla pathfinding/navigation, entity queries, storage scans and world/block updates before changing more systems.

## Acceptance

1. Let a quarry resident excavate below its workplace and fill/part-fill cargo.
2. Confirm a normal short return still walks.
3. If the resident cannot finish the local return within about 12 seconds, confirm it reappears on a safe workplace cell with the exact same carried stack, then deposits normally.
4. With an exhausted/no-target production site, observe that large target reacquisition does not recur every five seconds in lockstep across workers.
5. For deeper performance conclusions, collect a profiler capture from the same world.

Canonical Alpha.131 validation trigger: 2026-09-13.
