# Erden Realm Construction Validation

## Scope

The basic Erden kingdom must build as a physical Minecraft world without turning unloaded chunk access into a synchronous server-thread load. One Minecraft block is one metre. The initial royal citadel, streamed capital cells, exterior production settlements and household residences all share the same nonblocking construction safety rules.

## Regression closed

A previous safety change correctly stopped `IncrementalWorldEditPlan` from synchronously reaching into an unloaded chunk, but the initial unbounded citadel plan had no owner keeping its next target chunk loaded after `GenerationTask` finished. The plan therefore prepared 69,805 writes and then remained permanently at `0/69805`.

The corrected contract is split by plan type:

- unbounded realm plans request only the missing target chunks they actually encounter, using transient `TicketType.PORTAL` tickets;
- those transient tickets are released when the unbounded plan completes, and are also released if plan execution throws;
- chunk-bounded streamed plans never self-request neighboring chunks;
- each streamed Erden capital cell uses `new IncrementalWorldEditPlan(chunk)`, so builders cannot spill writes outside the loaded 16 x 16 metre cell;
- explicit capital-cell requests use transient asynchronous tickets instead of `setChunkForced` or synchronous `level.getChunk(...)`;
- construction writes retain `Block.UPDATE_KNOWN_SHAPE` so placing walls, doors and other shape-sensitive blocks does not synchronously wake adjacent chunks through neighbor-shape propagation.

## Required runtime proof

`.github/workflows/audit-living-kingdoms-realm-construction.yml` is the permanent focused regression gate. A fresh Java 25 server must:

1. prepare the 961 construction chunks without generation errors;
2. prepare the cleaned Erden citadel plan with 69,805 scheduled writes;
3. escape the historical `0/69805` state and record non-zero construction progress;
4. avoid watchdog, single-tick timeout and incremental-homeland failure messages;
5. preserve the no-forced-chunk and no-synchronous-capital-load source invariants.

The first focused audit after the repair observed the citadel move from `0/69805` to `9555/69805`, `18555/69805` and `27555/69805` without a watchdog failure. This focused proof is necessary but does not replace the longer residence, lifecycle, estate, workforce, exterior, supply-chain and full-build audits.

## Cross-system requirement

All longer Living Kingdoms audits must run again after this repair because their earlier failures shared the same stalled initial kingdom construction. A system is not considered revalidated merely because its source compiles or because the focused construction audit succeeds. Its own permanent fresh-world marker must succeed on a commit containing these construction invariants.
