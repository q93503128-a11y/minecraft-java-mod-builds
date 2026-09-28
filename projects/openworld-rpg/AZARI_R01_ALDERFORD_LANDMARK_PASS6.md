# Open-World RPG — Azari R01 Alderford / Landmark Spatial Pass 6

> Status: **ACTUAL ANVIL ALDERFORD SERVICE/HOUSING CENTERS SELECTED / SIX LANDMARK CANDIDATES SELECTED / ASSET FOOTPRINT + CLIENT ACCEPTANCE STILL OPEN**
>
> Date: 2026-09-28
>
> Canon owners: \`R01_VERTICAL_SLICE.md\`, \`R01_CONTENT_BIBLE.md\`, \`FISHING_COLLECTION_HOUSING_MARKET.md\`
>
> Predecessors: \`AZARI_R01_SURFACE_SPATIAL_PASS4.md\`, \`AZARI_R01_FIELD_SPATIAL_PASS5.md\`
>
> Rule: these are terrain-level shell/landmark centers from the actual R01 Anvil slice. They do not force a prefab size, door orientation or final camera composition before the accepted external asset is bound.

## 1. Why this pass exists

Pass 4 closed the Alderford gate and Quarry Road surface route. Pass 5 closed the field subregions and exact 5+1+1 fishing spatial set.

The next unresolved spatial questions were the starting settlement's service/housing distribution and the six exact minor discoveries. Those do not require the user to launch Minecraft merely to discover terrain height or broad flatness.

The actual R01 Anvil slice was sampled directly again.

## 2. Alderford service topology

Canonical center:

~~~text
Alderford center = (-2208, 67, 4000)
~~~

The selected service shell/prop centers are:

| Service | Candidate | Center distance | Raw-terrain note |
|---|---|---:|---|
| Wayfarers' Hall | \`(-2240,66,4000)\` | 32.0 b | dry grass, local relief ~1 |
| The Copper Kettle | \`(-2208,67,3968)\` | 32.0 b | dry grass, local relief ~3 |
| Holt Forge | \`(-2176,67,4000)\` | 32.0 b | dry grass, local relief ~2 |
| Alderford Vault | \`(-2192,67,4024)\` | 28.8 b | dry grass, local relief ~3 |
| Greenwater Remedies | \`(-2184,66,3976)\` | 33.9 b | dry grass, local relief ~1 |
| market/basic merchant | \`(-2224,66,3984)\` | 22.6 b | dry grass, local relief ~1 |
| Fordside Stables | \`(-2232,65,4024)\` | 33.9 b | dry grass, outward-road side |
| guild/route board | \`(-2224,66,4016)\` | 22.6 b | dry grass, square/road circulation edge |

Every normal service candidate is within the locked **15–35 block** service relationship from the square center.

The already-selected gate / first-shrine candidate remains:

~~~text
(-2240, 67, 4048)
center separation ≈ 57.7 blocks
~~~

That preserves the locked 45–60 block gate → market/core relationship.

## 3. Five exact Alderford housing shells

The launch roster remains exactly four Small Cottages plus one Market House.

Terrain-level shell centers:

| Property | Candidate | Terrain role |
|---|---|---|
| Gate Cottage | \`(-2264,67,4056)\` | extremely flat gate-side grass patch |
| Paddock Cottage | \`(-2264,67,4000)\` | western outer ring near stable/paddock side |
| Riverside Cottage | \`(-2152,67,3968)\` | river-facing outer side |
| Quarry-Road Cottage | \`(-2272,67,4024)\` | outward-road residential edge |
| Market House | \`(-2160,67,4016)\` | eastern outer ring / visible upgrade home |

These candidates deliberately sit outside the service square rather than consuming the central 25–35 block plaza.

This pass does **not** decide exact building width/depth or door placement. The selected external shell family still owns those dimensions. If an accepted shell cannot fit a candidate without destructive terrain work, move that shell center within the same reserved Alderford core instead of distorting the asset.

## 4. Six exact minor discoveries

The content bible defines exactly six ordinary first-visit landmarks. Pass 6 now gives each one a real-terrain candidate.

| Landmark | Candidate | Direct terrain reading |
|---|---|---|
| Bent Roadwatch | \`(-2304,75,4152)\` | dry raised grass shoulder, local relief ~2 |
| Shepherd's Overlook | \`(-2100,70,4266)\` | slight dry Meadow rise, local relief ~2 |
| Twin-Willow Bend | \`(-2141,63,3925)\` | mixed sand/grass bank ~5 b from the bound ordinary fishing water |
| Drover's Rest | \`(-2140,70,4152)\` | dry flat secondary-route shoulder, local relief ~1 |
| Quarrymen's Memorial | \`(-2544,71,4588)\` | dry flat approach shoulder before the Quarry Waystone belt |
| Root-Split Cairn | \`(-2600,76,4296)\` | dry flat eastern/safe edge of Rootshade Grove |

The semantic rewards and functions are unchanged:

- Bent Roadwatch keeps its Gold 15 + Healing Potion cache;
- Shepherd's Overlook remains orientation/discovery only;
- Twin-Willow Bend owns the existing ordinary fishing spot;
- Drover's Rest keeps Trail Skewers x1;
- Quarrymen's Memorial remains a no-loot ordinary-worker memorial;
- Root-Split Cairn keeps Healing Herb x2.

## 5. What is closed here

Direct-world evidence now closes:

- the 8 non-gate Alderford service/board terrain centers;
- all 5 launch housing shell terrain centers;
- all 6 exact minor-landmark terrain centers;
- service-center distance compatibility with the locked plaza topology;
- dry/flat-enough raw terrain for the chosen shell centers.

## 6. What remains asset/client work

Still unclaimed:

- exact prefab width/depth/rotation;
- exact service door coordinates;
- gate → shrine → plaza sightline at gameplay FOV;
- forge chimney / hall banner / inn sign / stable-paddock silhouette readability;
- whether at least two finished home exteriors read clearly from normal circulation;
- Shepherd's Overlook's final authored directional view after structures/foliage exist;
- Twin-Willow willow-model placement;
- landmark prop dressing and lighting;
- first/third-person circulation feel.

These are deferred to the integrated R01 first-complete test rather than a partial user test.

## 7. Binding state

All Pass-6 entries are bundled in \`r01_spatial_candidates.json\` with:

~~~text
status = candidate
~~~

No \`client_verified\` or \`production\` promotion occurs.

Verification:

~~~text
ACTUAL R01 ANVIL TERRAIN USED: YES
ALDERFORD LOCKED SERVICE ROSTER HAS TERRAIN CENTERS: YES
ALL FIVE LAUNCH HOUSING SHELLS HAVE TERRAIN CENTERS: YES
ALL SIX MINOR LANDMARKS HAVE TERRAIN CENTERS: YES
SERVICE DISTANCE TOPOLOGY 15–35 BLOCKS: YES
GATE TO CORE 45–60 BLOCK RELATIONSHIP: YES
ASSET FOOTPRINT/ORIENTATION VERIFIED: NO
ACTUAL CLIENT VISUAL ACCEPTANCE: NO
R01 SPATIAL_BINDING COMPLETE: NO
R01 IMPLEMENTED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
~~~
