# Open-World RPG — Azari R01 Quarry Interior Pass 3

> Status: **ACTUAL QUARRY SUBSURFACE NBT SCANNED / 3D REVIEW VOLUMES SELECTED / FINAL ROOM GEOMETRY STILL REQUIRES CLIENT REVIEW**
>
> Date: 2026-09-27
>
> Canon owners: `R01_VERTICAL_SLICE.md` §13.2, `R01_CONTENT_BIBLE.md`, `STATUS_AND_R01_ENCOUNTERS.md`
>
> This pass does not redesign the dungeon. The authored sequence remains Upper Mining Gallery → Collapsed Hoist Chamber → Root-Breached Workings → Relay Gallery → Earthloong Chamber.

## 1. Evidence window

The scan used the already-acquired real Azari R01 slice and inspected the Quarry neighborhood:

```text
x = -2720 .. -2480
z =  4620 ..  4870
y =   -16 ..    96
```

The scan streamed only intersecting chunks from the real Anvil region files rather than loading the whole 30k map.

Results:

```text
Quarry-neighborhood chunks scanned: 418
missing chunks in the scan window: 0
subchunks with meaningful underground air: 830
usable underground connected components >= 500 air voxels: 66
```

The largest nearby cave systems are irregular vertical/narrow networks rather than ready-made dungeon rooms.

A floor-clearance check across the same window did not find a naturally flat chamber suitable for treating the Earthloong arena as already finished terrain. Even with only three blocks of vertical clearance, the best contiguous natural floor disk was roughly 3 blocks in radius. Therefore the correct use of the map is:

```text
real Azari cave seams / rock identity
+ authored quarry excavation and supports
+ authored boss chamber carved into verified solid mountain
```

not dropping the encounters into arbitrary natural caves.

## 2. Upper Mining Gallery review shell

Candidate review volume:

```text
x = -2576 .. -2541
y =    32 ..    47
z =  4722 ..  4753
mode = natural_seam
```

Raw-volume observations:

- approximately 7.5% natural air;
- approximately 92.4% solid rock;
- sampled minimum overburden: about 20 blocks;
- close enough to the lower-entrance probe to support an authored entrance tunnel without a long dead corridor.

Interpretation:

- retain visible natural cave cuts where useful;
- excavate the authored mining-gallery footprint around them;
- ore carts/supports/side-cache ledge remain authored;
- exact entrance orientation and second-wave threshold stay client-review items.

This volume is not the final room boundary.

## 3. Collapsed Hoist review shell

Candidate review volume:

```text
x = -2612 .. -2573
y =    24 ..    47
z =  4770 ..  4809
mode = natural_seam
```

Raw-volume observations:

- approximately 6% natural air;
- approximately 94% solid rock;
- sampled minimum overburden: about 42 blocks;
- nearby cave topology varies strongly in Y, which is useful for the authored broken-platform/hoist traversal identity.

Use the real vertical cave seam as negative space while authored hoist/platform geometry controls readable traversal. The persistent lift shortcut remains a project mechanic, not a natural-cave assumption.

## 4. Root-Breached review shell

Candidate review volume:

```text
x = -2636 .. -2597
y =    22 ..    41
z =  4802 ..  4841
mode = natural_seam
```

Raw-volume observations:

- approximately 7% natural air;
- approximately 93% solid rock;
- sampled minimum overburden: about 49 blocks;
- this sits inside the larger southern underground cave network and gives enough rock mass to author the ecology/masonry transition.

This is the preferred place to review Nature Spirit sightline, root growth, one-time side cache and old-masonry reveal together.

## 5. Relay Gallery transition probe

Candidate review volume:

```text
x = -2596 .. -2575
y =    22 ..    33
z =  4818 ..  4841
mode = transition_probe
```

This sampled volume is effectively solid and has at least about 63 blocks of overhead in the extracted slice.

Purpose:

- bridge Root-Breached natural seam into the boss pocket;
- host the older fitted-stone/relay treatment without forcing that story beat into a random cave;
- preserve the canonical damaged plate interaction;
- keep the route short enough that the antechamber remains a story beat rather than another combat room.

Final relief orientation/camera/readability still requires the client.

## 6. Earthloong chamber carve probe

Candidate sanity volume:

```text
x = -2586 .. -2555
y =    14 ..    25
z =  4818 ..  4849
mode = solid_carve_probe
size = 32 x 12 x 32 blocks
```

Observed in the extracted slice:

- 100% solid/non-water across the sampled sanity box;
- minimum sampled overburden: about 68 blocks;
- no natural open chamber is being falsely treated as a finished boss arena.

This is deliberately a carve probe, not a final arena-size decision. The final chamber must still be checked against Earthloong body dimensions/camera distance, 12-block Lightning Furrow length, four-lane lateral spread, 4.5-block Root Breaker radius, dodge corridors, co-op spacing/revive access, authored breakable-prop placement and entrance/aftermath sightline.

No production code may use this candidate volume until client review explicitly promotes it.

## 7. Data binding

The five Pass-3 volumes are bundled in `data/openworld_rpg/world/r01_spatial_candidates.json`.

Spatial schema version is now 2.

Runtime states remain:

```text
candidate
→ client_verified
→ production
```

Normal gameplay can obtain only `production` anchors/areas/volumes. Pass-3 data therefore cannot silently become live spawn/quest/boss authority.

## 8. What remains before interior production binding

1. load the real world slice/world in a Minecraft client;
2. inspect the lower entrance facing and first tunnel;
3. walk the candidate room sequence at gameplay FOV;
4. validate the Hoist vertical readability in first/third person;
5. verify Nature Spirit and Cave Centipede spawn anchors against walls/ceilings;
6. size the Earthloong chamber from the actual donor model and camera;
7. verify lightning decal projection over the final arena floor;
8. test multiplayer choke width and revive space;
9. measure entrance → boss travel time;
10. only then promote accepted entries to `client_verified` / `production`.

Verification:

```text
ACTUAL QUARRY SUBSURFACE NBT SCANNED: YES
MISSING CHUNKS IN REVIEW WINDOW: 0
NATURAL CAVE TOPOLOGY REVIEWED: YES
UPPER GALLERY REVIEW VOLUME SELECTED: YES
COLLAPSED HOIST REVIEW VOLUME SELECTED: YES
ROOT-BREACHED REVIEW VOLUME SELECTED: YES
RELAY REVIEW VOLUME SELECTED: YES
EARTHLOONG SOLID CARVE POCKET SELECTED: YES
ACTUAL MINECRAFT CLIENT REVIEW: NO
FINAL ROOM GEOMETRY: NO
PRODUCTION SPATIAL BINDING: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
