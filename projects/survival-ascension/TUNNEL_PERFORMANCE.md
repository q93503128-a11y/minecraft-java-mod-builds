# Tunnel bulk-mining server-tick performance

## Reported integrated-server baseline

Minecraft Java 26.2 / NeoForge 26.2 / Java 25, shaders off: ordinary play averaged about 8 ms with a roughly 16 ms max tick. A 7x7x10 tunnel job averaged about 62 ms and showed a roughly 790 ms max server tick while client FPS stayed near 60. These are human F3 measurements and are the acceptance baseline, not CI-generated numbers.

## Current-main root cause audit

The target queue was already a deque and geometry was generated once. The defect was cost control: one job could still start twelve `ServerPlayerGameMode.destroyBlock` pipelines in one tick regardless of the time spent by preceding calls. Each pipeline deliberately includes vanilla/NeoForge break event, loot/enchantment, drop/entity, block/neighbor/light/fluid, stat/advancement and client-sync work.

Frontier Settlement multiplied that cost because its global BreakBlockEvent listeners could rebuild complete active building/road/outpost plans, and the civic-core listener rebuilt all tier plans, even for unrelated tunnel blocks. This was avoidable CPU/allocation/GC pressure layered on top of the necessary break pipeline.

## 0.61.17 / Alpha.113 controls

- 6 ms global and 4 ms per-job soft bore budgets measured with `System.nanoTime()`.
- EWMA prediction avoids knowingly starting another full destroy pipeline when the remaining slice is smaller than recent pipeline cost; one target is always allowed to prevent starvation.
- Existing loaded-chunk admission remains fail-closed. No force-load/generation path was added.
- The manual-equivalent `gameMode.destroyBlock` path remains intact. There is no raw AIR fast path and no loot/enchantment/event bypass.
- Pending-count bookkeeping is batched once per scheduler slice rather than once per target.
- Frontier listeners use block-type or conservative physical envelopes before exact plan protection. Exact cancellation still runs for candidate positions.

## Runtime profiler

After a job completes, run `/ascension borestats`; the log also emits `[bore-profile]`. Buckets: target generation, validation, reduced-wear bookkeeping, complete vanilla destroy pipeline p95/p99/max, and scheduler-slice p95/p99/max. The destroy bucket deliberately includes downstream NeoForge subscribers and Minecraft internals; splitting it further would require invasive instrumentation and is not used as a production shortcut.

## Required real-play acceptance

The original integrated-server pack must be re-run because build/CI success cannot establish F3 MSPT. Exercise ordinary stone, ores/mixed blocks, fluid and gravity adjacency, block entities, chunk boundaries, enchanted/Silk Touch/Fortune tools, full inventory, LAN, player movement/logout/death, save/rejoin, and overlapping requests. Compare F3 avg/max and `/ascension borestats` against the reported ~8/16 ms idle and ~62/790 ms bore baseline. Human performance acceptance is pending until repeated >50 ms and hundreds-of-ms spikes are absent in that environment.
