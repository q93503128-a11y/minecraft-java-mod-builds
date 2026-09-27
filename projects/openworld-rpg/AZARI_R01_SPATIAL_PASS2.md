# Open-World RPG — Azari R01 Spatial Pass 2

> Status: **ACTUAL R01 WORLD-SLICE NBT PARSED / SURFACE MACRO CANDIDATES LOCKED FOR IN-GAME REVIEW / FINAL COORDINATE BINDING NOT YET APPROVED**
>
> Date: 2026-09-27
>
> Canon owners: `R01_VERTICAL_SLICE.md`, `R01_CONTENT_BIBLE.md`, `REGIONS.md`
>
> Rule: this pass is based on the actual extracted Azari world bytes, not the public overview render. It may narrow or reject coordinates. It does **not** claim gameplay-FOV sightline or travel-time acceptance until the slice is opened in a real client.

## 1. Actual-world evidence

The inspected source was extracted from the creator-downloaded file:

```text
source file: AzariNEW4252026.zip
world root: AzariNEW4252026/
level.dat version: 1.21.11
DataVersion: 4671
world spawn recorded by level.dat: (0, 79, 0)
```

The R01 intake slice covers:

```text
region X: -8 .. 4
region Z:  0 .. 12
approx block X: -4096 .. 2559
approx block Z:     0 .. 6655
```

Parsed slice inventory:

```text
Anvil region files: 169
entity region files: 87
POI region files: 102
total selected entries: 358
parts: 4
```

Local evidence hashes used for this pass:

```text
manifest  9556ac50579fbc8cc932835929247c33be0e34b5077b3f224d8b4672ccb44cc3
part 01   833a57e01e079fe5e18f035c78710b91f473066992a6bf0b68058195365c8559
part 02   3230cdf8586d47250269f9705cea4817016d0eb4b61184d96e77494506fdf285
part 03   af1700ae74b0e72de7a18c9c95a387368e3abe1a2f7e710d96c7f90c1a210c44
part 04   2704334be4d9370b9fa305a99c3b5f96a2e85c708323a8740368a1ea60b9235f
```

These are hashes of the transferred R01 slice artifacts, not the untouched 19.2 GB creator ZIP. The full creator-ZIP hash is still not available in this workspace.

The pass decoded actual Anvil chunk NBT, `WORLD_SURFACE` / `MOTION_BLOCKING_NO_LEAVES` heightmaps, surface block-state palettes and biome palettes. It did not infer the terrain from screenshots.

## 2. Rejected early candidates

The old public-render R01 center `(-650, +3500)` remains only a search-window hint.

Actual slice inspection rejected several tempting points:

- `(-1464, 2184)`: plains/grass, but local geometry reads as a small peninsula/island rather than a dependable long-term river-road hub;
- `(-2400, 1184)`: very flat plains beside river/ocean water, but the candidate footprint is effectively island/peninsula terrain and pushes R01 too far from the intended central Heartland window;
- `(-1952, 2912)`: meadow/river geometry is attractive, but the surface slice contains a high density of creator-added fence/wood/cobble-style structure blocks through the candidate core, making overwrite/reuse assumptions unsafe without a dedicated structure review;
- `(-824, 3784)`: grass/cherry terrain exists, but local height/coverage is less stable and the area is less suitable than the selected candidate for a grounded working hub.

No rejected coordinate becomes a fallback automatically.

## 3. Alderford candidate

### 3.1 Candidate center

Current raw-world candidate:

```text
Alderford center candidate
x = -2208
y = 67
z = 4000
surface = grass_block
surface biome at center = plains
```

Reason:

- center itself is natural plains/grass;
- the surrounding area has a readable river/road/coast relationship without forcing the hub onto an island;
- a broad buildable core exists without flattening a signature mountain;
- the southern/southwestern route naturally transitions toward the selected rocky Quarry foothill;
- the candidate remains inside the original broad R01 Heartland search intent rather than moving the start to a distant regional edge.

### 3.2 Core footprint candidate

Current terrain-safe core review box:

```text
x = -2288 .. -2112
z =  3968 ..  4080
```

Raw-data observations across this box:

- no sampled surface-water occupancy;
- no detected final-building-style surface block footprint that requires preserving a creator settlement;
- surface elevation roughly `64..78`, with the central 90% roughly `65..74`;
- majority surface is `grass_block`;
- biome mix is plains into cherry-grove fringe.

This box is large enough for the central service cluster, but exact Wayfarers' Hall / forge / inn / vault / stable / five housing-shell coordinates remain pending in-game camera and road review.

### 3.3 Gate / first shrine

Do not lock the exact gate or shrine point from NBT alone.

The current preferred gate side is the **southwestern/outward-road edge** of the core, because it preserves a natural continuation into the Old Quarry Road candidate corridor. A representative terrain-safe probe is near:

```text
(-2280, 69, 4080)
```

This is a candidate probe, not a final shrine coordinate.

## 4. Old Quarry Road candidate corridor

The following polyline is the current raw-terrain route skeleton:

```text
Alderford core
(-2208, 4000)
→ (-2260, 4100)
→ (-2320, 4200)
→ (-2380, 4300)
→ (-2440, 4400)
→ (-2500, 4500)
→ (-2560, 4600)
→ Quarry Waystone belt
(-2560, 4660)
→ lower-entrance belt
(-2600, 4700)
```

Approximate polyline length to the lower-entrance belt is **~812 blocks** before final road smoothing, authored bends or structure placement.

That is suitable as a candidate because:

- the route remains on valid land at the sampled points;
- it moves from low grass/plains/cherry terrain into exposed rock rather than teleporting between identities;
- the middle belt around `(-2440, 4400)` is far enough from the hub to host Roadside Trouble / marker / cargo evidence without reading as a town event;
- the final approach reaches a real rocky foothill.

This does **not** yet prove the 90–150 s / 60–120 s targets. Those are measured in-client along the final navigable road, not from Euclidean or probe-polyline distance.

### Candidate evidence belt

Current non-final placement belt:

```text
Broken Road Marker / first disturbance candidate: around (-2380, 4300)
Roadside Trouble candidate center: around (-2440, 4400)
Lost Cargo / late-road evidence candidate: around (-2500, 4500)
```

The order may move during in-game readability review. The quest rules do not change.

## 5. Quarry Surface Works candidate

The raw slice exposes a strong natural quarry transition on the southern rocky shoulder.

### 5.1 Quarry Waystone belt

Current preferred exterior checkpoint probe:

```text
(-2560, 67, 4660)
surface = grass_block
biome = cherry_grove
```

It sits before the main rock face and can support an exterior field checkpoint without becoming an interior dungeon shrine.

### 5.2 Quarry overlook

Current preferred elevated overlook probe:

```text
(-2628, 96, 4764)
surface = calcite
biome = plains
```

This is an actual exposed rocky point. It is high enough to serve as the first Quarry identity/reveal candidate while remaining close enough to the lower entrance for the authored overlook → Waystone/entrance sequence to be shaped around the terrain.

### 5.3 Lower entrance belt

Current preferred lower-entrance probe:

```text
(-2600, 64, 4700)
surface = grass_block
biome = cherry_grove
```

The nearby slope rises sharply into calcite/rock, providing a plausible place to cut/bind the authored lower quarry entrance without pretending the whole mountain is already a finished dungeon.

The exact entrance mouth, support structure, cart/hoist placement and camera-facing direction remain pending client inspection.

## 6. Dungeon interior status

Do **not** bind Upper Gallery / Collapsed Hoist / Root-Breached / Relay / Earthloong chamber coordinates yet.

The surface route is now materially narrowed, but the interior still needs:

1. actual cave/solid-volume inspection under the selected Quarry shoulder;
2. room/corridor footprint check against creator underground structures;
3. boss-camera clearance;
4. multiplayer choke-width review;
5. actual entrance-to-boss travel-time measurement.

The already-implemented semantic room ids and encounter authority remain correct. This pass only chooses where their physical shell is likely to attach.

## 7. What is now closed vs still open

### Closed enough to carry into in-game review

The current Pass-2 candidates are now mirrored in the bundled data file `data/openworld_rpg/world/r01_spatial_candidates.json` and validated at mod startup. Every anchor/area/route remains `candidate`; normal gameplay accessors return no production coordinate until a later client-review pass explicitly promotes the affected entries. This prevents raw-world analysis coordinates from silently becoming live quest/spawn authority.

- actual R01 world-slice bytes are available and parsed;
- public-render-only placement is no longer the active method;
- the old rough R01 center is no longer treated as a hub coordinate;
- one primary Alderford terrain candidate is selected;
- one primary Alderford → Quarry surface corridor is selected;
- one primary Quarry exterior/overlook/entrance terrain cluster is selected;
- rejected early candidates are recorded so later work does not silently oscillate back to them.

### Still requires real Minecraft client inspection

- exact approach-road start and 2–4 minute reveal;
- final Alderford gate + first shrine;
- exact service-building and housing-shell footprints;
- exact road mesh/path smoothing;
- exact Lost Cargo / marker / Roadside Trouble volumes;
- final gathering-node coordinates;
- real sightlines at gameplay FOV;
- actual on-foot / Trail Stag travel time;
- Quarry entrance facing and support-structure footprint;
- all dungeon interior room/boss coordinates;
- lighting/camera/performance acceptance.

Verification:

```text
ACTUAL R01 AZARI SLICE BYTES PARSED: YES
ANVIL HEIGHTMAP/BLOCK/BIOME DATA REVIEWED: YES
ALDERFORD PRIMARY TERRAIN CANDIDATE SELECTED: YES
QUARRY SURFACE PRIMARY TERRAIN CANDIDATE SELECTED: YES
R01 SURFACE ROUTE CANDIDATE SELECTED: YES
FULL 19.2 GB CREATOR ZIP HASHED IN THIS WORKSPACE: NO
ACTUAL MINECRAFT CLIENT WORLD LOADED: NO
GAMEPLAY-FOV SIGHTLINES VERIFIED: NO
TRAVEL TIMES MEASURED IN CLIENT: NO
FINAL SPATIAL_BINDING COMPLETE: NO
PLAYTESTED: NO
```
