# Open-World RPG — Azari R01 Gathering Spatial Pass 7

> Status: **ALL 47 CANONICAL R01 GATHERING NODES HAVE ACTUAL-WORLD CANDIDATE ANCHORS / PRODUCTION NOT PROMOTED**
>
> Date: 2026-09-28
>
> Canon owners: `R01_CONTENT_BIBLE.md`, `GATHERING_FISHING_CAMP_HOUSING.md`
>
> Predecessors: `AZARI_R01_FIELD_SPATIAL_PASS5.md`, `AZARI_R01_ALDERFORD_LANDMARK_PASS6.md`
>
> Rule: this pass binds the exact canonical resource-node counts to actual Azari terrain or verified Quarry solid-rock review volumes. It does not promote candidate anchors into live gameplay authority.

## 1. Canon count closure

The R01 content bible requires exactly:

| Resource | Required | Pass-7 candidate anchors |
|---|---:|---:|
| Iron Ore | 11 | 11 |
| Hardwood | 16 | 16 |
| Healing Herb | 16 | 16 |
| Verdant Crystal | 4 | 4 |
| **Total** | **47** | **47** |

Subregion distribution is unchanged:

| Subregion | Iron | Hardwood | Herb | Crystal |
|---|---:|---:|---:|---:|
| Alderford Approach | 0 | 0 | 1 | 0 |
| Greenwater Ford | 0 | 2 | 6 | 0 |
| Alder Meadow | 0 | 2 | 3 | 0 |
| Riverwood Edge | 0 | 7 | 3 | 0 |
| Old Quarry Road | 3 | 2 | 1 | 0 |
| Rootshade Grove | 0 | 3 | 2 | 2 |
| Quarry Surface Works | 4 | 0 | 0 | 0 |
| Quarry dungeon | 4 | 0 | 0 | 2 |

No new resource category, currency or extra node was introduced.

## 2. Terrain-selection rules used

Surface candidates were accepted only after direct Anvil checks.

- Hardwood: dry grass ground inside the correct ecology band, biased toward existing tree cover.
- Healing Herb: dry grass ground, with Greenwater/Riverwood candidates biased toward actual bank/water geography.
- Old Quarry Road Iron: existing exposed calcite/andesite faces.
- Quarry Surface Iron: existing exposed calcite faces on the real Quarry shoulder.
- Rootshade Verdant Crystal: the limited actual exposed rocky outcrop on the grove's eastern edge.
- Dungeon resources: original solid `stone` coordinates inside the selected Pass-3 review volumes, intended to become authored wall nodes when those rooms are excavated.

The node anchor Y is the raw terrain/block anchor. The final visible external resource mesh/block may apply its own model offset during asset binding.

## 3. All candidate anchors

| Candidate | Coordinate | Evidence |
|---|---:|---|
| `alderford_approach/healing_herb_01` | `(-2254, 72, 4122)` | healing herb 1/1 alderford approach terrain candidate |
| `greenwater_ford/hardwood_01` | `(-2392, 67, 4098)` | hardwood 1/2 greenwater ford terrain candidate |
| `greenwater_ford/hardwood_02` | `(-2432, 66, 4010)` | hardwood 2/2 greenwater ford terrain candidate |
| `greenwater_ford/healing_herb_01` | `(-2448, 66, 4066)` | healing herb 1/6 greenwater ford terrain candidate |
| `greenwater_ford/healing_herb_02` | `(-2456, 66, 4042)` | healing herb 2/6 greenwater ford terrain candidate |
| `greenwater_ford/healing_herb_03` | `(-2472, 70, 4098)` | healing herb 3/6 greenwater ford terrain candidate |
| `greenwater_ford/healing_herb_04` | `(-2456, 69, 4122)` | healing herb 4/6 greenwater ford terrain candidate |
| `greenwater_ford/healing_herb_05` | `(-2408, 68, 4122)` | healing herb 5/6 greenwater ford terrain candidate |
| `greenwater_ford/healing_herb_06` | `(-2400, 69, 4130)` | healing herb 6/6 greenwater ford terrain candidate |
| `alder_meadow/hardwood_01` | `(-2008, 69, 4272)` | hardwood 1/2 alder meadow terrain candidate |
| `alder_meadow/hardwood_02` | `(-2040, 67, 4104)` | hardwood 2/2 alder meadow terrain candidate |
| `alder_meadow/healing_herb_01` | `(-2080, 68, 4240)` | healing herb 1/3 alder meadow terrain candidate |
| `alder_meadow/healing_herb_02` | `(-2040, 67, 4216)` | healing herb 2/3 alder meadow terrain candidate |
| `alder_meadow/healing_herb_03` | `(-1960, 65, 4208)` | healing herb 3/3 alder meadow terrain candidate |
| `riverwood_edge/hardwood_01` | `(-2008, 68, 4116)` | hardwood 1/7 riverwood edge terrain candidate |
| `riverwood_edge/hardwood_02` | `(-2008, 69, 4300)` | hardwood 2/7 riverwood edge terrain candidate |
| `riverwood_edge/hardwood_03` | `(-2016, 68, 4084)` | hardwood 3/7 riverwood edge terrain candidate |
| `riverwood_edge/hardwood_04` | `(-2040, 66, 4196)` | hardwood 4/7 riverwood edge terrain candidate |
| `riverwood_edge/hardwood_05` | `(-2000, 69, 4252)` | hardwood 5/7 riverwood edge terrain candidate |
| `riverwood_edge/hardwood_06` | `(-1968, 66, 4148)` | hardwood 6/7 riverwood edge terrain candidate |
| `riverwood_edge/hardwood_07` | `(-2032, 68, 4308)` | hardwood 7/7 riverwood edge terrain candidate |
| `riverwood_edge/healing_herb_01` | `(-1952, 64, 4228)` | healing herb 1/3 riverwood edge terrain candidate |
| `riverwood_edge/healing_herb_02` | `(-1984, 66, 4236)` | healing herb 2/3 riverwood edge terrain candidate |
| `riverwood_edge/healing_herb_03` | `(-2040, 67, 4212)` | healing herb 3/3 riverwood edge terrain candidate |
| `old_quarry_road/iron_ore_01` | `(-2452, 73, 4268)` | iron ore 1/3 old quarry road terrain candidate |
| `old_quarry_road/iron_ore_02` | `(-2436, 72, 4528)` | iron ore 2/3 old quarry road terrain candidate |
| `old_quarry_road/iron_ore_03` | `(-2400, 71, 4512)` | iron ore 3/3 old quarry road terrain candidate |
| `old_quarry_road/hardwood_01` | `(-2504, 71, 4352)` | hardwood 1/2 old quarry road terrain candidate |
| `old_quarry_road/hardwood_02` | `(-2400, 74, 4456)` | hardwood 2/2 old quarry road terrain candidate |
| `old_quarry_road/healing_herb_01` | `(-2384, 65, 4544)` | healing herb 1/1 old quarry road terrain candidate |
| `rootshade_grove/hardwood_01` | `(-2712, 66, 4296)` | hardwood 1/3 rootshade grove terrain candidate |
| `rootshade_grove/hardwood_02` | `(-2696, 65, 4192)` | hardwood 2/3 rootshade grove terrain candidate |
| `rootshade_grove/hardwood_03` | `(-2672, 66, 4176)` | hardwood 3/3 rootshade grove terrain candidate |
| `rootshade_grove/healing_herb_01` | `(-2744, 63, 4184)` | healing herb 1/2 rootshade grove terrain candidate |
| `rootshade_grove/healing_herb_02` | `(-2728, 63, 4208)` | healing herb 2/2 rootshade grove terrain candidate |
| `rootshade_grove/verdant_crystal_01` | `(-2624, 72, 4232)` | verdant crystal 1/2 rootshade grove terrain candidate |
| `rootshade_grove/verdant_crystal_02` | `(-2624, 72, 4252)` | verdant crystal 2/2 rootshade grove terrain candidate |
| `quarry_surface_works/iron_ore_01` | `(-2560, 67, 4712)` | iron ore 1/4 quarry surface works terrain candidate |
| `quarry_surface_works/iron_ore_02` | `(-2584, 71, 4728)` | iron ore 2/4 quarry surface works terrain candidate |
| `quarry_surface_works/iron_ore_03` | `(-2644, 71, 4764)` | iron ore 3/4 quarry surface works terrain candidate |
| `quarry_surface_works/iron_ore_04` | `(-2688, 73, 4796)` | iron ore 4/4 quarry surface works terrain candidate |
| `quarry_dungeon/iron_ore_01` | `(-2568, 40, 4730)` | iron ore 1/4 Quarry dungeon authored-wall candidate in Upper Mining Gallery review shell |
| `quarry_dungeon/iron_ore_02` | `(-2598, 32, 4786)` | iron ore 2/4 Quarry dungeon authored-wall candidate in Collapsed Hoist review shell |
| `quarry_dungeon/iron_ore_03` | `(-2628, 30, 4810)` | iron ore 3/4 Quarry dungeon authored-wall candidate in Root-Breached review shell |
| `quarry_dungeon/iron_ore_04` | `(-2588, 28, 4824)` | iron ore 4/4 Quarry dungeon authored-wall candidate in Relay Gallery review shell |
| `quarry_dungeon/verdant_crystal_01` | `(-2624, 30, 4816)` | verdant crystal 1/2 Quarry dungeon authored-wall candidate in Root-Breached review shell |
| `quarry_dungeon/verdant_crystal_02` | `(-2608, 28, 4832)` | verdant crystal 2/2 Quarry dungeon authored-wall candidate in Root-Breached review shell |

## 4. Dungeon authored-wall candidates

The six dungeon resources are deliberately not claimed as naturally exposed resources in the untouched map.

Current raw blocks:

```text
Upper Gallery Iron       (-2568,40,4730) = stone
Collapsed Hoist Iron     (-2598,32,4786) = stone
Root-Breached Iron       (-2628,30,4810) = stone
Relay Gallery Iron       (-2588,28,4824) = stone
Root-Breached Crystal 1  (-2624,30,4816) = stone
Root-Breached Crystal 2  (-2608,28,4832) = stone
```

This is intentional: the authored quarry rooms will expose these nodes on the final wall/floor geometry while retaining the real mountain mass and the locked resource counts.

If final room carving moves a wall enough to invalidate one exact block, the node may move **inside the same canonical room** during final authored-room binding. The count and room ownership do not change silently.

## 5. User-test policy

No user test is requested for this pass.

Direct Anvil evidence is sufficient for:

- ground Y;
- dry/wet status;
- surface block;
- exposed rock identity;
- resource-count placement;
- whether dungeon candidate blocks are solid stone.

The integrated R01 first-complete test remains responsible for:

- whether nodes are visible but not glowing clutter;
- gathering animation/model alignment;
- approach/readability during ordinary exploration;
- interaction range and collision;
- respawn/personal-availability feel;
- final dungeon-wall placement after authored rooms exist.

## 6. Binding state

Every Pass-7 anchor remains:

```text
status = candidate
```

Normal production accessors therefore continue to expose none of these coordinates.

Verification:

```text
ACTUAL AZARI ANVIL BYTES USED: YES
GATHERING NODE COUNT: 47/47
IRON ORE: 11/11
HARDWOOD: 16/16
HEALING HERB: 16/16
VERDANT CRYSTAL: 4/4
SURFACE NODE RAW BLOCK/Y CHECKED: YES
DUNGEON NODE ORIGINAL BLOCK = STONE: 6/6
PRODUCTION PROMOTION: NO
USER PLAYTEST REQUIRED NOW: NO
R01 SPATIAL_BINDING COMPLETE: NO
R01 IMPLEMENTED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
