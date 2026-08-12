# Erden source-native interior topology phase

The urban interior overhaul now has three independent safety layers:

- the runtime preservation pass restores imported architectural detail only when entrance-to-core traversal remains valid;
- `ErdenUrbanSourceTopologyCatalog` reads the three licensed schematic sources directly and records broad source topology without touching world chunks or functional plot counts;
- `ErdenUrbanPlacedTopologyCatalog` consumes the exact cropped fragments and exact rotations that back the 233 deterministic urban lots, validates their real footprint/entrance metadata, and classifies each retained fragment conservatively before any compatibility geometry can be removed.

`ExternalUrbanFabricBuilder` is now the single source of truth for this diagnostic metadata. It exposes read-only snapshots derived from the same retained placement objects used by construction, so the classifier does not duplicate or approximate the placement algorithm. Source analysis and exact-placement classification are bootstrapped before realm diagnostics so dedicated-server audits capture their topology markers.

This phase is diagnostic infrastructure for replacing synthetic upper floors incrementally. It does not by itself disable the current compatibility interior or alter any city block.

## Invariants

1. The 233 deterministic urban functional plots and their role counts remain unchanged.
2. Source-topology and exact-fragment analysis are source-only and may not synchronously load a world chunk.
3. Exact placement metadata must reconcile every entrance to a known cropped fragment, rotation, dimensions and world footprint before it can be considered for conversion.
4. A fragment can be classified only as `AUTHORED_MULTILEVEL`, `AUTHORED_GROUND_ONLY`, or `FALLBACK`; ambiguity always resolves toward fallback.
5. Source-native floors are adopted only after a per-building runtime path also proves entrance, retained stairs and a usable upper floor.
6. The current synthetic ground/upper-floor geometry remains the compatibility fallback whenever imported topology is incomplete or unsafe.
7. The 273-entry traversal audit remains a hard regression gate after every behavioral interior change.

## Current conversion gate

The exact placed-fragment exposure and 233-building source classification layer are now present. The next gate is runtime correlation rather than another guessed geometry pass.

For each urban entrance, correlate `ErdenUrbanPlacedTopologyCatalog` with the pre-conversion `ErdenUrbanAuthoredInteriorSurvey`. A building may bypass the synthetic upper-floor converter only when its exact retained fragment is `AUTHORED_MULTILEVEL` **and** the finished-world pre-conversion survey independently proves a path from the actual entrance through retained stair geometry to a usable upper-floor area. `AUTHORED_GROUND_ONLY`, `FALLBACK`, missing survey data, incomplete chunks, broken stairs or failed traversal must retain the compatibility path.

Role fixtures can move into source-native room cells only after this dual source/runtime proof is stable. Exterior facade, roof, entrance, access path, functional plot identity, economy/workforce counts and the 273-entry traversal gate must remain unchanged throughout that migration.
