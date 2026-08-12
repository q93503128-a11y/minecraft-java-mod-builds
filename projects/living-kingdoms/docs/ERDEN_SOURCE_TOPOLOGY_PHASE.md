# Erden source-native interior topology phase

The urban interior overhaul now has four independent safety layers:

- the runtime preservation pass restores imported architectural detail only when entrance-to-core traversal remains valid;
- `ErdenUrbanSourceTopologyCatalog` reads the three licensed schematic sources directly and records broad source topology without touching world chunks or functional plot counts;
- `ErdenUrbanPlacedTopologyCatalog` consumes the exact cropped fragments and exact rotations that back the 233 deterministic urban lots, validates their real footprint/entrance metadata, and classifies each retained fragment conservatively before any compatibility geometry can be removed;
- `ErdenUrbanTopologyCorrelationAudit` correlates those exact source classifications with the finished-world pre-conversion survey without changing blocks, letting a future converter require independent source and runtime proof rather than trusting either model alone.

`ExternalUrbanFabricBuilder` is the single source of truth for diagnostic placement metadata. It exposes read-only snapshots derived from the same retained placement objects used by construction, so the classifier does not duplicate or approximate the placement algorithm. Source analysis and exact-placement classification are bootstrapped before realm diagnostics so dedicated-server audits capture their topology markers.

This phase is diagnostic infrastructure for replacing synthetic interiors incrementally. It does not by itself disable the current compatibility interior or alter any city block.

## Measured exact-fragment result

The fresh dedicated-server audit of the exact retained fragments found:

- `all_in_one_house.schem#0`: `AUTHORED_GROUND_ONLY`, 713 reachable cells, one meaningful ground band, vertical span 1;
- `medieval_manor.schem#0`: `AUTHORED_GROUND_ONLY`, 705 reachable cells, one meaningful ground band, vertical span 2;
- `fantasy_castle_house.schem#0`: `FALLBACK`, only 11 reachable cells from the retained entrance under the conservative walkability model;
- across all 233 deterministic urban lots: 156 `AUTHORED_GROUND_ONLY`, 77 `FALLBACK`, and **0 `AUTHORED_MULTILEVEL`**.

The source-native upper-floor gate is therefore closed for the current three cropped facade kits. Existing stairs in the schematic data are not sufficient evidence of a retained doorway-to-upper-floor route. No building is allowed to lose its compatibility upper floor merely because stair blocks exist in the imported source.

## Invariants

1. The 233 deterministic urban functional plots and their role counts remain unchanged.
2. Source-topology, exact-fragment analysis and source/runtime correlation are read-only and may not synchronously load a world chunk.
3. Exact placement metadata must reconcile every entrance to a known cropped fragment, rotation, dimensions and world footprint before it can be considered for conversion.
4. A fragment can be classified only as `AUTHORED_MULTILEVEL`, `AUTHORED_GROUND_ONLY`, or `FALLBACK`; ambiguity always resolves toward fallback.
5. A genuine source-native upper floor may be adopted only when source classification and finished-world runtime traversal both independently prove the route.
6. The current synthetic ground/upper-floor geometry remains the compatibility fallback whenever imported topology is incomplete or unsafe.
7. Exterior facade, roof silhouette, real entrance, access path, functional plot identity and economy/workforce counts may not drift during interior work.
8. The 273-entry traversal audit remains a hard regression gate after every behavioral interior change.

## Safe pivot: source-shell-constrained authored interiors

Because the current retained fragments contain no proven source-native upper route, the next implementation must not keep pretending that a hidden imported second floor can simply be switched on. The next phase is a **source-shell-constrained authored interior planner**.

For each exact placed fragment, derive interior buildable volumes from the actual retained shell rather than a fixed guessed box. The planner must identify facade/roof/perimeter blocks as immutable, locate enclosed or safely enclosable void cells, identify floor-support bands, preserve the real doorway corridor, and reserve a staircase shaft wholly inside verified shell space. Only then may it propose additional rooms or an authored upper floor. Any cell whose structural role is ambiguous remains untouched.

The first implementation remains read-only: emit per-fragment candidate room volumes, usable floor area, safe stair-shaft candidates and protected structural cells. A later streamed converter may materialize only those verified plans and must pass the 273-entry traversal audit plus dedicated-server/client regression checks before compatibility geometry can be retired.
