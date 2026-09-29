# TURNBOUND — Drehmal Map Reference v1

Purpose: source-backed spatial reference for production planning. This is not a substitute for Minecraft 26.2 client survey.

## 1. What can be inspected before client survey

Public sources provide enough material to understand the world at several levels:

- official Drehmal wiki: settlements, regions, POIs, layouts and coordinates;
- official/community-supported full overworld renders;
- downloadable 2D map exports, game-map exports and very high resolution Overviewer renders;
- source map's own regional/town maps.

Useful public references:
- https://wiki.drehmal.cyou/World/Overworld/
- https://wiki.drehmal.cyou/World/Regions/
- https://wiki.drehmal.cyou/World/Regions/Central_Regions/Capital_Valley/
- https://wiki.drehmal.cyou/World/Settlements/Official_Towns/Drabyel/
- https://wiki.drehmal.cyou/World/Points_of_Interest/Primal_Caverns/
- https://wiki.drehmal.cyou/Lore/Books/Explorer%27s_Guide/
- https://wiki.drehmal.cyou/World/Points_of_Interest/Av%27Sal/
- https://gist.github.com/Zottelchen/4d5d084529b28ba950a7010901314435

The public render set includes 2D images up to 79,440×39,909 and a full Overviewer export. These are reference material only and are not bundled into TURNBOUND.

## 2. Hard boundary

Source-backed coordinates describe Drehmal 2.2.x / 1.20.1 content.

They are useful to:
- identify the correct town/building/road/landmark;
- understand topology and relative placement;
- pre-plan route beats and candidate service zones.

They are not sufficient to certify:
- exact 26.2 collision;
- exact safe spawn block;
- camera clearance;
- migrated NPC/entity behavior;
- whether an original trigger fires;
- multiplayer-safe offsets.

Those remain `verifiedIn26_2=false` until actual migrated-world client inspection.

## 3. Capital Valley macro geography

Source-backed:
- central early-game region;
- mostly rolling plains, meadow and woodland;
- small cliffs/hills rather than constant severe terrain;
- red/blue/green terracotta peaks around Primal Caverns are strong visual landmarks;
- Mouth of Drehmal forms the southern water boundary;
- New Drabyel sits on a southern peninsula;
- Capital Valley Tower lies off the road between the start/Primal Caverns side and Drabyel;
- Av'Sal lies to the west/southwest and is a huge ruin complex.

This supports the R_PG-style flow: open traversal first, sparse readable enemies, then denser authored danger around ruins.

## 4. First-route source anchors

| Place | Source coordinate | Production interpretation |
|---|---:|---|
| Primal Caverns | approx. 855,65,553 (wiki pages vary by exact feature) | first major terrain landmark |
| Capital Valley Tower | 557,129,1176 | breathing landmark / possible discovery node |
| Warning Cave | approx. 454,73,1252 | optional danger POI; survey interior before battle binding |
| Explorer's Guide camp | 581,81,1501 | road-side rest / information beat |
| New Drabyel | approx. 502,67,1801 | first physical safe hub |
| Av'Sal central island | approx. -242,67,1532 | next major ruin/combat region |

The wiki explicitly places the Explorer's Guide camp on the road between Primal Caverns and Drabyel, and notes that Drehmal roads may become old/decrepit and fade out. TURNBOUND should therefore allow off-road free movement rather than enforce a corridor.

## 5. New Drabyel layout

Source-backed town identity:
- small early settlement;
- oak/spruce construction;
- many buildings partially embedded in terrain with grass roofs;
- entrance stables immediately to the right;
- Drehmal statue near the farmhouse;
- farmhouse/farmland on the southwest side;
- permanent market booths near the center;
- Adventuring Merchant;
- Runic Blacksmith / Goibhniu's Smithy opposite the village map/merchant area.

Source coordinates useful for survey seeding:
- Adventuring Merchant: about 516.5,67,1854.5;
- Runic Blacksmith: about 526,65,1839–1841;
- Coal Merchant: 532,67,1838;
- Oak Merchant: 530,67,1833;
- Wheat Merchant: 541,67,1830.

TURNBOUND zoning consequence:
- market UI belongs at the existing merchant frontage, not an invented menu plaza;
- equipment enhancement belongs at the existing smithy;
- travel guidance belongs near the entry/stables;
- story/party beat belongs around the central landmark space;
- summon access should use a suitable existing interior only after visual survey.

### 5.1 Source-backed micro layout

The current official wiki gives enough detail to narrow the in-client survey substantially:

| Feature | Source detail | TURNBOUND use |
|---|---|---|
| Town reference | approx. 502,67,1801 | approach/entry search origin only |
| Stables | immediately right on entry | TRAVEL service candidate zone; preserve horse access |
| Adventuring Merchant | 516,67,1851 | primary MARKET context; orange/red awning |
| Runic Blacksmith | 526,65,1841 | primary FORGE context; Goibhniu's Smithy / blue awning |
| Oak Merchant | 530,67,1833 | central market-booth context |
| Coal Merchant | 532,67,1838 | central market-booth context |
| Wheat Merchant | 541,67,1830 | central market-booth context |
| Drehmal statue | in front of the farmhouse | STORY landmark candidate |
| Church of the Split Deities | south side of town | source-content preservation zone |
| Nature's Rest Inn | east of Runic Blacksmith | rest/ambient context; do not displace source villagers |
| Drabyel Bookstore | far east side | lore-content preservation zone |
| Farmhouse basement Cat Map | 516,65,1861 | explicit conflict/exclusion reference, not a default summon room |

Placement implications:
- `MARKET` should be surveyed first around the Adventuring Merchant frontage, then compared with the central booth cluster for readability.
- `BLACKSMITH` should stay visually tied to Goibhniu's Smithy rather than being moved to a generic central plaza.
- `TRAVEL` should read immediately from the entrance/stables without blocking the horses or entry path.
- `STORY` can use the statue/farmhouse/church-side landmark space, but must not cover signs, graves, lore containers or original interaction points.
- `SUMMON` still has no source-backed exact room. The known farmhouse basement at 516,65,1861 already contains original collectible content and is therefore a **conflict marker**, not a placement recommendation.
- church, bookstore, inn guest rooms and other authored interiors must be inspected for original loot/lore before any TURNBOUND service occupies them.

Do not activate these source coordinates directly. They remain survey seeds until the migrated 26.2 client confirms geometry, source-content conflicts, sightlines and player flow.

## 6. Av'Sal spatial read

Source-backed:
- one of the largest ruins in the map;
- built around a lake;
- central island contains major administrative structures and a Terminus tower;
- surrounding cityscape forms outer districts;
- Mihkmari scavengers occupy the ruins;
- the Repository sits below the old capitol area.

TURNBOUND consequence:
- use outer approach for low-density patrols;
- increase pressure through ruined streets;
- preserve breathing space before the central island;
- use the central island as a high-density destination, not as random common-spawn territory;
- never flatten the ruin into a dedicated arena if a camera-safe nearby footprint exists.

## 7. Map-analysis workflow

For every new region:
1. inspect high-resolution render / official region map;
2. read official wiki settlement + POI pages;
3. record source coordinates as survey seeds;
4. classify spaces: safe hub / road / landmark / optional danger / dungeon / boss-sized clearing;
5. inspect the actual migrated 26.2 client world;
6. only then promote exact runtime positions, patrol paths and battle footprints.

This keeps world understanding detailed without pretending a web coordinate is already a verified gameplay coordinate.
