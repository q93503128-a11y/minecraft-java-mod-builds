# Open-World RPG — R01 Alderford Structure Binding Pass 9

> Status: **STRUCTURE/SERVICE/PROPERTY BINDING INFRASTRUCTURE IMPLEMENTED / EXACT PREFAB COMPOSITION STILL VISUAL-REVIEW GATED / PRODUCTION SPATIAL PROMOTION NOT PERFORMED**
>
> Date: 2026-09-28
>
> Canon owners: `R01_VERTICAL_SLICE.md`, `R01_CONTENT_BIBLE.md`, `FISHING_COLLECTION_HOUSING_MARKET.md`, `R01_ASSET_INTAKE.md`, `PRODUCTION_ASSET_BINDING_MATRIX.md`
>
> Predecessors: `AZARI_R01_ALDERFORD_LANDMARK_PASS6.md`, `R01_STRUCTURE_ASSET_BINDING_PASS8.md`

## 1. What this pass implements

Pass 8 fixed the Alderford architecture/ordinary-prop families but deliberately left exact wall/roof/pivot composition blocked on the real Quaternius Standard archives and visual review.

Pass 9 converts the already-closed part into runtime-readable implementation infrastructure without inventing those missing visuals.

New bundled data:

```text
data/openworld_rpg/world/r01_alderford_structure_bindings.json
```

New runtime contract:

```text
R01StructureBindingData
R01StructureBindingLoader
```

The bundle contains exactly:

```text
15 structure identities
  2 gate/shrine compositions
  6 building services
  1 market composition
  1 route-board composition
  5 housing shells

9 service bindings
5 property bindings
```

## 2. Deterministic composition contract

Every structure has a stable namespaced composition ID and:

```text
selection_mode = authored_fixed
state = visual_review_gated
exact_prefab_id = null
```

This deliberately prevents runtime random kitbashing or an implementation-time AI/vanilla placeholder from becoming the settlement design.

An exact prefab ID is illegal while the composition is still `visual_review_gated`. A structure marked `production` is illegal unless its composition is `accepted` and has an exact prefab ID.

No Pass-9 structure is promoted to production.

## 3. Spatial separation

Each shell references the terrain-level Pass-6 spatial anchor already present in `r01_spatial_candidates.json`.

The loader validates every structure reference against the current bundled spatial dataset.

A production-ready structure cannot bind a non-production spatial anchor.

Gate/watch and gate shrine intentionally share the same terrain candidate because Pass 6 selected a combined gate/shrine terrain probe, but they use separate composition IDs so later accepted geometry is not forced into one prefab.

## 4. Service interaction separation

Service interaction is no longer implied to occur at the building's terrain shell center.

Each service owns a semantic `interaction_socket_id`, for example:

```text
openworld_rpg:r01/socket/alderford/copper_kettle/service
openworld_rpg:r01/socket/alderford/holt_forge/service
openworld_rpg:r01/socket/alderford/route_board/face
```

The final world transform belongs to the accepted authored composition. This keeps door/counter/board interaction placement gated with the actual prefab pivot/orientation instead of baking guessed coordinates into gameplay authority.

## 5. Housing shell identity

The exact five launch properties are bound to their distinct authored shell identities without inventing final dimensions:

| Property | Tier | Price | Home Storage | Sale credit |
|---|---|---:|---:|---:|
| Gate Cottage | Small Cottage | 2,400 Gold | 54 | 80% |
| Paddock Cottage | Small Cottage | 2,400 Gold | 54 | 80% |
| Riverside Cottage | Small Cottage | 2,400 Gold | 54 | 80% |
| Quarry-Road Cottage | Small Cottage | 2,400 Gold | 54 | 80% |
| Market House | Town House | 9,000 Gold | 72 | 80% |

The property record points to the authored shell identity. Protected/interior volumes remain unresolved until the exact accepted shell dimensions exist.

## 6. Asset-family boundary

The data fixes only families already closed by Pass 8:

```text
Quaternius Medieval Village MegaKit Standard
Quaternius Fantasy Props MegaKit Standard
KayKit RPG Tools for Holt Forge ordinary tool/forge details
```

The route board is a prop composition and therefore does not falsely claim a Medieval Village building shell.

No exact Medieval wall/roof module arrangement, shrine detail, stable layout, Market House exterior, door facing or pivot is selected here.

## 7. Verification boundary

Pre-commit checks completed before repository integration:

```text
JSON syntax/contract check: PASS
Java main/test source syntax check with minimal compile stubs: PASS
real project Gradle unit tests: NOT VERIFIED IN THIS SESSION
real project build: NOT VERIFIED IN THIS SESSION
Minecraft client: NOT RUN
multiplayer: NOT RUN
```

The available GitHub connector does not expose push-triggered Actions runs for this commit, and the local container cannot resolve github.com for a git checkout. Therefore the Gradle/build labels remain unverified rather than being inferred from the local syntax checks.

## 8. State after Pass 9

```text
ALDERFORD STRUCTURE FAMILY BINDING INFRASTRUCTURE: IMPLEMENTED
ALDERFORD SERVICE SOCKET CONTRACT: IMPLEMENTED
ALDERFORD PROPERTY SHELL IDENTITY CONTRACT: IMPLEMENTED
EXACT PREFAB COMPOSITION: NO
REAL CREATOR STRUCTURE ARCHIVE ACQUIRED: NO
PRODUCTION SPATIAL PROMOTION: NO
R01 ASSET_BINDING COMPLETE: NO
R01 SPATIAL_BINDING COMPLETE: NO
R01 SOURCE READY: NO
R01 IMPLEMENTED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
