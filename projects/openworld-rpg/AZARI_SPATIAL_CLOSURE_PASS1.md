# Open-World RPG — Azari Spatial Closure Pass 1

> Status: **OVERHEAD-RENDER TRIAGE COMPLETE / LOCAL ARCHIVE INTAKE TOOLING READY / ACTUAL WORLD COORDINATE CLOSURE STILL REQUIRED**  
> Date: 2026-09-17  
> Spatial master: `REGIONS.md`  
> Project contract: `PROJECT.md`  
> Terrain candidate: Azari 30k × 30k by itzvmbie / vmbie  
> Rule: this pass narrows where to inspect. It does **not** pretend a 512px public render is equivalent to loading the real world.

---

# 1. Source evidence used

Current public page:

`https://www.planetminecraft.com/project/azari-30k-x-30k-world-painter-map/`

Current exact acquisition locators verified on 2026-09-18:

```text
Planet Minecraft latest page update: 2026-06-18
PMC free-download mirror path:
/project/azari-30k-x-30k-world-painter-map/download/mirror/746230/

creator-linked updated build landing page:
https://vmbiemc.gumroad.com/l/azarimap

creator instruction:
put $0 / FREE for the free download
```

The current research/browser environment can reach the Gumroad landing page but cannot complete the $0 checkout/download transaction or materialize the world archive bytes. Therefore this pass still has **no world ZIP hash and no local world load**. The acquisition path is now pinned; the remaining blocker is obtaining the archive bytes, not finding the map again.

Publicly stated facts relevant to this project:

- 30,000 × 30,000 WorldPainter world;
- 30+ custom biomes;
- 15+ custom cave variants;
- listed terrain families include plains, meadows, badlands, desert, oasis, jungle, bamboo forest, barrier reef, multiple ocean types, fairy/whimsical forests, mountains, ice spikes/ocean/abyss, freezing taiga, rich forest, obsidian spikes, volcanic ash island, pollinating cliffs and flower forest;
- the 2026-06-18 update states that underground and above-ground structures were added throughout the map;
- the creator states that not all terrain assets are their own and that some were purchased or free-to-use.

The current map therefore remains:

```text
PRIVATE PLAYABLE WORLD CANDIDATE: strong
PUBLIC-REPO WORLD BYTES: not assumed safe
PROVENANCE STATUS: VERIFY / LOCAL_ONLY
```

The public overhead render is useful for macro planning but cannot prove exact Minecraft coordinates, elevation, route slope, cave connectivity, spawn safety or actual sightline occlusion.

---

# 2. Working coordinate convention for render triage

The current public overhead render is a square full-world overview. For **search-window planning only**, this pass uses the following provisional mapping:

```text
render left   ≈ x -15000
render right  ≈ x +15000
render top    ≈ z -15000
render bottom ≈ z +15000
```

This assumes:

- the render covers the full 30k extent with little/no asymmetric padding;
- top corresponds to Minecraft north;
- the displayed continent is not rotated relative to world coordinates.

Those assumptions are **not verified yet**.

Therefore every coordinate below is a **candidate inspection window**, never a final hub/POI coordinate.

Actual world import must establish at least two obvious landmark control points before this transform is trusted.

---

# 3. Region inspection windows from the public overhead

The following values deliberately use broad radii. They say `inspect here first`, not `build the hub here`.

| Region | Render-level terrain read | Approx search center (x, z) | Search radius | Pass-1 reason |
|---|---|---:|---:|---|
| R01 Heartland | central/south-central plains-meadow-river belt | `(-650, +3500)` | 2200 | grounded central land with access toward forest/highlands/sea |
| R02 Western Forest | broad west/northwest rich-forest river mass | `(-9100, -2700)` | 2600 | strongest western dense-forest candidate belt |
| R03 Whitecrest Highlands | west/southwest major pale mountain/highland mass | `(-8800, +5200)` | 2500 | visually dominant highland/vertical terrain candidate |
| R04 Frozen Crown | northern snow/ice cap | `(0, -10900)` | 3000 | unmistakable frozen regional identity |
| R05 Jungle Greenbelt | northeast green dense belt south/east of frozen cap | `(+6100, -8000)` | 2600 | best first-pass northeast jungle/bamboo search window |
| R06 Mirewater Delta | east-central river/wetland mosaic | `(+7600, -2400)` | 2400 | dense waterways and broken-land delta geometry |
| R07 Sunscar Desert | far-east orange desert/badlands mass | `(+10200, +800)` | 2200 | unmistakable arid/oasis/badlands region |
| R08 Bloomveil | south-central magical/flower terrain search band | `(0, +8700)` | 2500 | first-pass flower/pollinating/whimsical candidate zone |
| R09 Southstone Marches | southern/southeastern rocky/grass/coastal belt before volcano | `(+3800, +9000)` | 2600 | grounded southern route/foundry/coast identity candidate |
| R10 Cinderfall | southeast volcanic ash island/peninsula | `(+10200, +9600)` | 2400 | unmistakable volcanic end of the map |
| R11 Inner Sea | central inland-sea system, reefs/islands/coasts | `(+1400, -2100)` water centroid only | 5200 | region is intentionally multi-coast/depth, not one hub point |
| R12 Obsidian Rift | west-central dark/obsidian spike pocket | `(-4700, +1700)` | 2200 | visible dark anomaly/obsidian candidate adjacent to central routes |

Important uncertainty:

- R08/R09 internal border is the least trustworthy from the public render alone;
- R01 exact heartland hub placement depends on local road/river geometry, not the rough central search point;
- R11 `center` is meaningless for service placement: Tidecross must sit on an actual useful coast/route junction;
- R12 must be confirmed against the exact obsidian-spike/overgrown-cave terrain in the world before any Central Anchor placement is committed.

---

# 4. Region-hub placement rule

Do **not** place a settlement at the mathematical center of its biome/region.

A hub candidate passes only if the real world shows:

1. at least two useful approach routes;
2. one visually readable long-range landmark or terrain silhouette;
3. enough buildable footprint without flattening the signature terrain;
4. a nearby short-loop field area for first quests/services;
5. at least one outward route toward the region's dungeon/major POI;
6. no constant cliff/water/pathfinding fight for ordinary NPC routines;
7. route geometry that can support later multiplayer without one-block chokepoints.

Preferred hub-to-first-meaningful-content rhythm:

```text
hub exit
→ 30–60 s readable transition
→ first useful node/event/choice
→ another 45–90 s maximum before a landmark/route decision/POI interaction
```

Large scenery gaps are allowed only when the view itself is a strong orientation/spectacle beat and the player has suitable traversal.

---

# 5. Travel-time targets for actual coordinate closure

Canonical movement references from `MOUNTS.md`:

```text
player sprint: ~5.61 b/s
Trail Stag: 6.4 b/s
Jungle Komodo: 8.4 b/s cruise
Caravan Elephant: 6.3 b/s cruise
Laviathan: 8.8 b/s water/lava
Sky Drake: 10.5 b/s cruise / 16.5 b/s dive
```

The 30k world must not turn scale into dead travel.

### Within a region

Target ordinary first-visit authored spacing:

- hub → first major field POI: roughly **1.5–4 min** at the region's expected traversal state;
- major POI → next meaningful route decision/POI: roughly **1–3.5 min**;
- dungeon approach from the nearest unlocked practical service/shrine: roughly **2–5 min** on first visit;
- repeat route after shortcut/shrine: materially shorter.

### Between neighboring regional hubs

First-time overland journey target:

- usually **5–10 min** at the traversal tier expected when that connection first matters;
- journeys longer than ~10–12 min require meaningful sub-POIs/events/settlements/route changes, not empty road;
- cross-map returns rely on discovered shrine/fast-travel network rather than repeatedly riding 8,000+ blocks.

The rough render-center distances are **not** accepted travel distances. Hubs should be placed toward useful shared route edges, while the middle of each region remains exploration space.

---

# 6. Required macro sightlines

The actual world pass must preserve these visual-navigation relationships where terrain permits.

## R01

- Alderford or its near approach should expose at least two future-direction silhouettes/route cues;
- the player should understand `forest / highlands / inland-sea or major central landmark` without a permanent objective arrow.

## R03

- Windwatch Eyrie must genuinely reveal route relationships and the observatory approach;
- if the real terrain cannot support that sightline, move the authored Eyrie/route, not the mountain.

## R04

- Hearthspring should be findable through dark rock/steam/light contrast from at least one primary approach;
- whiteout areas must still have authored recovery landmarks.

## R05

- giant canopy/river/temple cues must reconnect frequently enough to prevent a 3D jungle maze;
- at least one canopy overlook must reveal a useful route relationship.

## R07

- oasis/settlement/buried-tower/badlands silhouettes should support long-distance desert orientation;
- open visibility is part of the region's identity, not empty-space permission.

## R10

- volcanic mass/forge/refuge should advertise the region from approach routes;
- Basalt Wyvern territory can use high shelves visible from below.

## R11

- islands/lighthouses/reef color/harbor silhouettes form a chain of navigation references;
- deep areas require visible vertical reference rings/rock/altar forms rather than featureless water.

## R12

- obsidian/Central Anchor/Cathedral silhouettes must be visible in stages rather than appearing only at the entrance;
- Sky Drake unlock occurs after the player has already learned the ground-scale landmark network.

---

# 7. R11 special spatial closure

R11 cannot be validated as a single surface polygon.

Actual world intake must record:

```text
surface coastline polygons
harbor/island nodes
reef/open-sea travel corridors
safe Laviathan turning widths
trench-lip coordinates
abyss floor/depth band
Deep-Dive Harness test route
air/maintenance chamber position relative to underwater path
Abyss Fang arena center + vertical bounds
```

Any area that becomes 3+ minutes of featureless Laviathan travel without meaningful navigation/content should be shortened by route placement, ferry/fast-travel structure or POI density rather than defended as `realistic ocean scale`.

---

# 8. R12 special spatial closure

R12 actual intake must record separately:

- ground-route graph before Sky Drake;
- Riftwatch footprint;
- Phase Seam / anomaly POI bounds;
- Terradragon basin size and safe camera perimeter;
- Cathedral entrance / repeat shortcut;
- Central Anchor approach and final arena bounds;
- Sky Drake launch/landing clearances;
- dungeon/interior/no-fly volumes;
- aerial sightlines that become useful after unlock without making ground exploration obsolete.

Do not use invisible blanket anti-flight walls outdoors after Sky Drake unlock.

---

# 9. Actual-world capture sheet

For every final hub/POI/dungeon/boss location, the world inspection pass records:

```text
id
x
y
z
region
terrain/biome read
usable footprint
primary approach direction
alternate approach direction
nearest landmark
landmark visible distance
nearest meaningful content distance
on-foot travel time
expected-tier mount travel time
fast-travel/shrine relationship
vertical range
camera risk
multiplayer choke risk
performance risk
world-edit amount required
accept / move / reject
notes
```

Coordinates are not accepted by looking at the map image alone.

---

# 10. Actual-world inspection sequence

When the current Azari world archive is available locally (use the creator-linked 2026-06-18 updated build unless a newer verified update supersedes it), first run `tools/azari_world_intake.py` as defined in `AZARI_WORLD_INTAKE.md`, then:

1. preserve archive/source/version/date and hash;
2. load an untouched inspection copy;
3. identify world coordinate orientation and exact 30k bounds;
4. verify at least two render-to-world landmark control points;
5. fly/spectate through all 12 search windows;
6. reject any region-window mismatch before placing content;
7. capture candidate hub coordinates/footprints;
8. trace major route joins and measure real path distance, not Euclidean distance;
9. place candidate POI/dungeon/boss anchors only after sightline review;
10. measure expected-tier travel times;
11. move/compress/reroute content where the 30k scale creates dead time;
12. run a final R01→R12 macro route audit and R11/R12 special traversal audit.

Useful inspection commands in an actual dev instance may include spectator mode, `/tp`, coordinate display and deliberately temporary **developer-only inspection tooling**. None of those labels/messages enter normal gameplay.

---

# 11. Pass-1 decision

This pass closes:

- which macro terrain windows should be inspected first for R01–R12;
- the coordinate-capture schema;
- hub placement criteria;
- sightline criteria;
- first-pass travel-time budgets;
- R11 and R12 special spatial requirements.

This pass does **not** close:

- exact x/y/z;
- exact region polygons;
- exact roads;
- settlement footprints;
- dungeon/boss arena coordinates;
- vertical/depth bands;
- real sightline distances;
- real travel times.

Those require the actual current Azari world archive, not only the public render.

Verification:

```text
DESIGN/SPATIAL CRITERIA REVIEWED: YES
PUBLIC OVERHEAD REVIEWED: YES
AZARI LATEST DOWNLOAD LOCATOR PINNED: YES
AZARI WORLD ARCHIVE BYTES ACQUIRED IN THIS WORKSPACE: NO
ACTUAL AZARI WORLD LOADED: NO
EXACT COORDINATES VERIFIED: NO
TRAVEL TIMES MEASURED IN WORLD: NO
PLAYTESTED: NO
```

No build/CI is warranted for this docs-only pass.
