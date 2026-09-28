# Open-World RPG — Azari R01 Field Spatial Pass 5

> Status: **ACTUAL ANVIL FIELD/FOREST/RIVER CANDIDATES REFINED / EXACT R01 FISHING SPOT COUNT PLACED / PRODUCTION NOT PROMOTED**
>
> Date: 2026-09-28
>
> Canon owners: `R01_CONTENT_BIBLE.md`, `R01_VERTICAL_SLICE.md`, `GATHERING_FISHING_CAMP_HOUSING.md`, `FISHING_COLLECTION_HOUSING_MARKET.md`
>
> Predecessor: `AZARI_R01_SURFACE_SPATIAL_PASS4.md`
>
> Rule: direct Anvil facts may close raw terrain/water questions. Final authored-model composition, sightlines and movement feel remain integrated-client acceptance work.

## 1. Purpose

Pass 4 corrected the Alderford → Quarry surface route. Pass 5 uses the same real four-part R01 Azari slice to close the next spatial layer:

- Greenwater Ford;
- Alder Meadow;
- Riverwood Edge;
- Rootshade Grove;
- Mosswheel Mill footprint;
- exactly five ordinary fishing spots;
- exactly one uncommon/deeper pool;
- exactly one rare fishing spot.

This is still candidate binding. No entry becomes normal gameplay authority.

## 2. Greenwater Ford

Selected crossing probe:

```text
(-2440, 61, 4104)
```

Direct cross-section at `z=4104`:

```text
main water run: approximately x=-2448 .. -2434
width: ~16 blocks
water surface: Y=61
mean sampled depth: ~1.9 blocks
maximum sampled depth: 3 blocks
```

Alderford center → selected ford is approximately **254 blocks horizontal**. This is distance-compatible with the locked **45–75 s** square → Greenwater activity target, but real travel time remains unclaimed until the authored route exists.

Candidate area:

```text
x -2520 .. -2390
z  3970 ..  4160
```

Coarse direct sampling of that area is roughly:

```text
median surface Y: ~68
water columns: ~10%
dominant surface: grass_block
secondary identity: cherry-grove bank + river/lush-cave water corridor
```

This gives enough raw geography for the locked identity:

- ford crossing;
- snapped tether/broken-gate Trail Stag event;
- Healing Herb banks;
- two ordinary fishing spots;
- route back toward Alderford/Fordside Stables.

The exact Stag prop/model placement and stable-facing sightline remain asset/client binding.

## 3. Alder Meadow

First-meaningful-interaction probe:

```text
(-2000, 68, 4150)
surface: grass_block
horizontal distance from Alderford center: ~256 blocks
```

Candidate area:

```text
x -2080 .. -1900
z  4080 ..  4320
```

The area is mostly dry open grass/cherry terrain with low water occupancy. It is suitable at raw-terrain level for the locked meadow ecology:

- Louxia/Gazelle/Bison activity;
- Signs in the Meadow evidence;
- readable optional-danger direction;
- early Hardwood/Healing Herb placements.

Final actor anchor density and Shepherd's Overlook sightline remain later authored-spatial work.

## 4. Riverwood Edge

Reference probe:

```text
(-2060, 67, 4260)
surface: grass_block
```

Candidate area:

```text
x -2040 .. -1880
z  4020 ..  4330
```

The selected strip intentionally follows the transition from cherry terrain into riverbank/plains rather than drawing an invisible biome wall. It can support the locked role:

- tighter ecology than Alder Meadow;
- Viper/Grizzly pressure;
- concentrated Hardwood;
- riverbank contrast;
- the uncommon/deeper fishing pool toward the eastern end.

The boundary is a content-density transition, not a collision barrier.

## 5. Rootshade Grove

Reference probe:

```text
(-2680, 68, 4240)
surface: grass_block
```

Candidate area:

```text
x -2760 .. -2600
z  4160 ..  4340
```

Direct coarse sampling is predominantly dry cherry-grove ground with more trunk/wood columns than the selected open-meadow candidate. It is also displaced west of the ordinary Quarry Road line instead of sitting directly on top of the Lv3–5 route.

That separation is important because Rootshade owns:

- Lv6–8 optional pressure;
- Steelboar/Nature Spirit ecology;
- two Verdant Crystal nodes;
- Regalhart clue/start-boundary relationship.

The accepted darker silhouette still requires authored foliage/root dressing; raw Azari terrain alone is not claimed to provide final art direction.

## 6. Mosswheel Mill

Candidate footprint:

```text
x -2400 .. -2360
z  4160 ..  4200
```

This footprint straddles the Greenwater branch bank. The nearby water channel is real; a dry grass shoulder exists immediately east of it. The final mill shell/orientation remains asset binding because a waterwheel model has to meet the actual bank/water elevation rather than being forced into an arbitrary rectangle.

No combat encounter is added.

## 7. Exact R01 fishing spatial set

The content canon requires exactly:

```text
5 ordinary
1 uncommon/deeper pool
1 rare
```

Pass 5 places exactly that count.

| Slot | Candidate water coordinate | Direct depth | Spatial role |
|---|---|---:|---|
| Ordinary 1 | `(-2440, 61, 4104)` | 3 | Greenwater Ford |
| Ordinary 2 | `(-2486, 61, 3992)` | 3 | lower Greenwater branch |
| Ordinary 3 | `(-2140, 61, 3920)` | 2 | Twin-Willow Bend candidate |
| Ordinary 4 | `(-2394, 61, 4180)` | 1 | Mosswheel branch |
| Ordinary 5 | `(-2050, 61, 4230)` | 1 | Riverwood bank |
| Uncommon | `(-1900, 61, 4290)` | 5 | deeper Riverwood pool |
| Rare | `(-1886, 61, 3886)` | 10 | quiet eastern deep-water spot |

Additional direct checks:

- uncommon pool has an accessible solid bank within roughly 10 blocks;
- rare pool has an accessible sand bank within roughly 10 blocks;
- both Greenwater-required ordinary spots are inside the selected Greenwater candidate geography;
- Twin-Willow Bend owns one ordinary slot as required;
- final fish names/models remain ASSET_BINDING and are not invented by this pass.

These coordinates describe water-surface centers. They do not imply a player stands at Y=61 inside the water.

## 8. Why no user test is requested

The facts closed here are encoded in the world bytes:

- water surface Y;
- depth;
- dry/wet footprint;
- approximate relief;
- horizontal distance;
- broad biome/terrain transition.

Asking the user to manually inspect those now would duplicate work.

The integrated R01 first-complete client test will instead judge what raw NBT cannot:

- whether the ford reads as a ford after props are authored;
- Stag event readability and ride-back feel;
- fishing cast readability and bank animation space;
- meadow/forest threat readability;
- Rootshade silhouette and danger telegraph;
- final Mosswheel model fit;
- real movement time at player and mount speeds.

## 9. Binding state

All new entries remain:

```text
candidate
```

No `client_verified` or `production` promotion occurs.

```text
R01 SPATIAL_BINDING COMPLETE: NO
R01 IMPLEMENTED: NO
R01 PLAYTESTED: NO
R01 MULTIPLAYER TESTED: NO
```
