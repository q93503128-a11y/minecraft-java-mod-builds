# Erden authored road normalization

## Problem removed

Capital roads and drainage previously inherited `WORLD_SURFACE` height at construction time. On a fresh world, the top surface can be a tree trunk, leaves or other vegetation instead of the authored continent ground. That made the royal avenue and its culvert depend on random canopy placement and could leave the culvert missing near `(0, 200)` even though other capital samples had finished.

The permanent solution is not a diagnostic-only block placement. `ErdenAuthoredRoadNormalizer` automatically subscribes to the NeoForge game event bus and runs at the lowest server-tick priority after normal capital construction.

## Persistent normalization

Loaded capital chunks are queued when their chunk event fires. A chunk is normalized only after `ErdenCapitalStreamingBuilder` records it as complete. A dedicated saved-data revision records every normalized chunk, total road columns, royal-avenue culvert cells and removed canopy blocks.

Because this ledger is independent from the older capital-build revision, existing saves receive the same normalization when affected chunks are loaded. Rebuilding the entire kingdom is unnecessary.

## Authored height rules

For every completed road column:

- the target Y is taken from `AuthoredContinentDensity.surfaceHeight(x, z)`;
- trunks, leaves and other blocks above the road corridor are cleared without drops;
- missing ground support is restored below the authored surface;
- royal roads use polished andesite on land and stone brick over fluid;
- district and local roads use packed mud;
- non-royal roads do not overwrite authored fluid crossings.

For the central royal axes (`x = 0` or `z = 0`), the same pass reasserts:

- stone-brick floor at road Y minus 4;
- water channel at road Y minus 3;
- clear channel space at road Y minus 2;
- stone-brick ceiling at road Y minus 1;
- loaded side walls perpendicular to the avenue.

## Event ordering

Both chunk-load and server-tick handlers use `EventPriority.LOWEST`. The normal capital builder therefore finishes and marks its chunk before normalization runs. The realm diagnostic checks the already normalized road instead of racing the builder.

The normalizer never force-loads additional chunks. It repairs only chunks already loaded by gameplay, world construction or deterministic CI sampling.

## Permanent regression marker

The dedicated fresh-world audit must emit:

`LK_ERDEN_AUTHORED_ROADS_PASS revision=1`

The marker must also prove:

- at least one normalized chunk;
- non-zero road columns;
- non-zero culvert cells;
- the diagnostic road exists at the authored height;
- the culvert water and ceiling exist at that same authored height;
- canopy independence is enabled;
- existing-save repair is enabled;
- culvert reassertion is enabled.

The same run must still emit the full realm diagnostic and:

`Verified Erden urban infrastructure well=true fire_cistern=true royal_culvert=true`

A watchdog timeout, event-subscriber failure or `Royal avenue culvert is incomplete` message is a hard failure.
