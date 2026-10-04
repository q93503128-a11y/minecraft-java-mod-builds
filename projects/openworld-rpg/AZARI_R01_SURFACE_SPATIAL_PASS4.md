# Open-World RPG — Azari R01 Surface Spatial Pass 4

> Status: **ACTUAL FOUR-PART R01 SLICE REOPENED / SURFACE ROUTE TERRAIN-REFINED / CLIENT ACCEPTANCE DEFERRED TO INTEGRATED R01 PLAYTEST / PRODUCTION NOT PROMOTED**
>
> Date: 2026-09-28
>
> Canon owners: `R01_VERTICAL_SLICE.md`, `R01_CONTENT_BIBLE.md`, `REGIONS.md`
>
> Earlier evidence: `AZARI_R01_SPATIAL_PASS2.md`, `AZARI_R01_QUARRY_INTERIOR_PASS3.md`
>
> Rule: this pass uses the actual creator-acquired R01 Anvil bytes. It may resolve terrain facts and improve candidate coordinates, but it does not claim gameplay-FOV, authored-structure sightline, real travel-time or final presentation acceptance.

## 1. Why this pass exists

The four persistent R01 slice archives were recovered and read directly again:

```text
Azari_R01_part_01.zip
Azari_R01_part_02.zip
Azari_R01_part_03.zip
Azari_R01_part_04.zip
```

Their SHA-256 values match the prior intake record:

```text
part 01  833a57e01e079fe5e18f035c78710b91f473066992a6bf0b68058195365c8559
part 02  3230cdf8586d47250269f9705cea4817016d0eb4b61184d96e77494506fdf285
part 03  af1700ae74b0e72de7a18c9c95a387368e3abe1a2f7e710d96c7f90c1a210c44
part 04  2704334be4d9370b9fa305a99c3b5f96a2e85c708323a8740368a1ea60b9235f
```

The pass decoded the real Anvil chunks rather than relying on the public render or guessed Y values. Surface height, top block, biome, water occupancy and local relief were sampled directly from the extracted world bytes.

No user-side Minecraft test is required for this pass.

## 2. Alderford gate correction

Pass 2 used:

```text
old gate probe = (-2280, 69, 4080)
Alderford center = (-2208, 67, 4000)
horizontal separation ≈ 107.6 blocks
```

The opening canon requires the gate → market/core relationship to be roughly **45–60 blocks**. The old probe was therefore unsuitable even though the terrain itself was valid.

Refined candidate:

```text
Alderford center = (-2208, 67, 4000)
Alderford gate / first-shrine probe = (-2240, 67, 4048)
horizontal separation ≈ 57.7 blocks
surface = grass_block
local terrain = dry, low relief
```

Direct local sampling around the new gate found approximately:

```text
small footprint: Y 66..67
broader footprint: Y 65..68
surface water in sampled footprint: none
```

This satisfies the locked topology far better while keeping the gate on natural terrain.

Status remains **candidate**.

## 3. Old Quarry Road evidence refinement

The previous evidence points were useful macro probes but had unresolved Y and were not equally good local scene terrain.

### 3.1 Broken Road Marker

Selected candidate:

```text
(-2404, 71, 4312)
surface = grass_block
local sampled relief ≈ 70..73
water = none
```

The earlier point around `(-2380, 4300)` remains valid world terrain but sits beside a more irregular mixed surface. Moving the marker to the selected grass shoulder produces a cleaner readable first-disturbance location without changing the authored quest order.

### 3.2 Roadside Trouble

Selected event center:

```text
(-2472, 71, 4388)
surface = grass_block
```

A roughly 40×40 candidate scene around the center was sampled directly:

```text
x = -2492 .. -2452
z =  4368 ..  4408
sampled ground Y ≈ 68..73
surface water = none
sampled columns = predominantly grass_block
```

This is large enough at the terrain-data level to carry the canonical scene:

- stranded wagon;
- driver/worker;
- damaged-wheel interaction;
- displaced cargo interaction;
- two-viper solo baseline;
- up to four total event threats with multiplayer scaling.

It is now represented as an explicit candidate area in the spatial JSON.

### 3.3 Lost Cargo

Selected candidate:

```text
(-2520, 73, 4480)
surface = grass_block
water = none
```

It remains a late-road evidence beat rather than a combat arena, so the selected point favors route readability and dry terrain rather than demanding a broad encounter footprint.

## 4. Refined semantic route

The candidate semantic route is now:

```text
Alderford center
(-2208, 4000)
→ gate / first shrine
(-2240, 4048)
→ Broken Road Marker
(-2404, 4312)
→ Roadside Trouble
(-2472, 4388)
→ Lost Cargo
(-2520, 4480)
→ Quarry Waystone
(-2560, 4660)
→ lower entrance
(-2600, 4700)
```

Horizontal segment lengths:

| Segment | Approx. length |
|---|---:|
| center → gate | 57.7 b |
| gate → Broken Road Marker | 310.8 b |
| marker → Roadside Trouble | 102.0 b |
| Roadside Trouble → Lost Cargo | 103.8 b |
| Lost Cargo → Quarry Waystone | 184.4 b |
| Waystone → lower entrance | 56.6 b |
| **total** | **815.2 b** |

A terrain-aware dry-path scan over the same real Anvil slice found a low-elevation land route through this corridor. The route stays roughly in the Y61–75 band before the selected lower Quarry shoulder, with no required water crossing in the sampled route.

This does **not** mean the final road is a straight polyline. Authored bends, width, props, sightline control and small slope smoothing still belong to world authoring.

## 5. Canon consistency

### Alderford gate topology

```text
locked target: 45–60 blocks
candidate: ~57.7 blocks
result: distance-compatible
```

This is a distance/topography check, not a final gameplay-FOV acceptance.

### Old Quarry Road disturbance pacing

The center → Roadside Trouble candidate path is roughly 470.5 horizontal blocks through the semantic route.

That is consistent enough to keep the current candidate for the locked **90–150 s** square → disturbance pacing target once authored road bends, actual movement state and naturally encountered interactions are included.

**Travel time is not verified here.**

### Quarry Waystone

Canonical rule:

```text
Quarry Waystone must be 35–70 blocks of legal travel from the lower entrance.
```

Current candidate separation:

```text
(-2560, 4660) → (-2600, 4700)
horizontal separation ≈ 56.6 blocks
```

The current Waystone and lower-entrance probes therefore remain spatially compatible and are not moved by Pass 4.

## 6. What direct world analysis can close without asking the user to test

Direct Anvil analysis is sufficient to establish:

- candidate terrain Y;
- surface material and biome;
- water versus land;
- broad slope/relief;
- whether a route requires obvious water crossing;
- approximate horizontal distances;
- whether an encounter footprint is grossly obstructed by terrain;
- whether a quarry carve volume is solid/open at raw-world level.

This evidence should be consumed during implementation instead of asking the user to manually discover Y coordinates.

## 7. What still belongs to the integrated R01 client acceptance

The following are visual/feel questions and remain deliberately unclaimed:

- first-person and third-person sightlines after authored structures exist;
- exact gate-shrine reveal composition;
- service-building silhouettes and plaza readability;
- road dressing and evidence visibility at gameplay FOV;
- real normal-movement and Trail Stag travel times;
- final Quarry entrance facing/hoist composition;
- final dungeon traversal feel;
- Earthloong donor-model camera and arena sizing;
- final lighting/performance;
- multiplayer choke/revive feel.

These are deferred to the integrated R01 first-complete test rather than interrupting development with a partial user playtest.

## 8. Binding consequence

The bundled spatial data is updated with the refined gate/evidence candidates and the Roadside Trouble scene area.

All affected entries remain:

```text
status = candidate
```

No entry is promoted to `client_verified` or `production`.

The normal runtime production accessors therefore continue to return no live coordinate for these candidates.

## 9. Verification state

```text
ACTUAL FOUR R01 SLICE PARTS AVAILABLE: YES
PART HASHES MATCH PRIOR INTAKE: YES
DIRECT ANVIL SURFACE Y REVIEW: YES
ALDERFORD GATE TOPOLOGY CORRECTED: YES
ROAD EVENT TERRAIN CANDIDATES REFINED: YES
ROADSIDE TROUBLE SCENE FOOTPRINT SELECTED: YES
DRY SURFACE ROUTE PLAUSIBILITY CHECKED: YES
QUARRY WAYSTONE 35–70 BLOCK RULE SATISFIED: YES
ACTUAL MINECRAFT CLIENT REVIEW: NO
TRAVEL TIME MEASURED IN CLIENT: NO
PRODUCTION SPATIAL BINDING: NO
R01 SPATIAL_BINDING COMPLETE: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```


## 10. 2026-10-04 integrated-client surface review transport

The production gate above is unchanged: no candidate is promoted from raw-world evidence alone.
Code state `3ea496637f549b14226ef3146bccd14cd77e0802` instead adds a test-artifact-only transport so the
remaining real-client review can be performed without manually rediscovering coordinates.

The R01 integration playtest JAR exposes:

```text
/owr_r01_surface_first
/owr_r01_surface_next
/owr_r01_surface_prev
/owr_r01_surface_current
/owr_r01_surface_jump <1-based-index>
```

The plan has **101 surface checkpoints**:
- 85 spatial anchors;
- 9 candidate-area center points;
- 7 authored route waypoints.

The first eight points are the progression-critical Alderford -> Quarry surface sequence. Remaining
anchors/areas are deterministic, so repeated review sessions use the same ordering.

Safety boundary:
- explicit anchor Y is respected with a one-block standing offset;
- area centers/route waypoints use `MOTION_BLOCKING_NO_LEAVES` to resolve surface height;
- the five Quarry interior review volumes are **not** auto-teleported because their candidate
  geometry can intentionally be solid or only partially open;
- commands exist only when the R01 integration verification marker is active;
- the harness never changes candidate/client_verified/production status, quest state or rewards.

Build Openworld RPG #351 / run `37200048471`: **SUCCESS**.

Updated verification state:

```text
R01 SURFACE REVIEW HARNESS: IMPLEMENTED
R01 SURFACE REVIEW PLAN: 101 CHECKPOINTS
NORMAL GAMEPLAY COMMAND EXPOSURE: NO
QUARRY INTERIOR AUTO-TELEPORT: NO
ACTUAL MINECRAFT CLIENT REVIEW: NO
TRAVEL TIME MEASURED IN CLIENT: NO
PRODUCTION SPATIAL BINDING: NO
R01 SPATIAL_BINDING COMPLETE: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
