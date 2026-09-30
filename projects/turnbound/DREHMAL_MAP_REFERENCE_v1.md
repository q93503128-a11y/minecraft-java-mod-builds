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

Do not use these source coordinates as fixed NPC blocks. TURNBOUND may use them as search origins for the live-world auto-resolver, which chooses a nearby standable block while rejecting road centers, block-entity/source-content conflicts and existing villagers/traders. Static coordinates remain unpromoted; client playtest is the final visual/readability check rather than a manual placement workflow.

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

## 6.5 Structured world-derived reference

A public community map project, `zachaa/DrehmalMap`, has already extracted structured data from the Drehmal 2.2.2 world for:
- roads/path GeoJSON
- signs with text
- traders and trades
- storage/block entities
- item frames / armor stands
- named entities and locations

TURNBOUND uses this as **reference-only** because no redistributable license was identified. Raw upstream JSON/map tiles are not copied into the project.

For the New Drabyel micro-area, an automated query of the upstream structured data found 73 relevant source objects in the surveyed town window:
- 33 signs
- 21 storage entries
- 5 traders
- 11 item frames
- 2 Primal Cache/block-entity entries
- 1 armor stand

Useful exact source evidence includes:
- welcome sign: 502,70,1804
- Verdant Saddle sign: 506,68,1836
- Drabyel Guardhouse sign: 520,68,1823
- Runic Blacksmith trader: 526,65,1839
- Goibhniu's Smithy sign: 527,67,1844
- Oak / Coal / Wheat traders: 530,67,1830 / 535,67,1838 / 541,67,1833
- Adventuring Merchant: 516,67,1854
- church sign: 527,68,1854
- Nature's Rest sign: 538,69,1844
- bookstore sign: 561,68,1844
- shrine/offerings sign: 511,67,1850
- farmhouse basement map frame: 516,65,1861
- nearby Primal Caches: 525,67,1854 and 550,70,1841

This is detailed enough to stop treating hub placement as a manual coordinate hunt.

### Runtime placement rule

For New Drabyel services the preferred workflow is now:
1. use the extracted sign/trader/road data to define the semantic service zone;
2. when a player actually approaches New Drabyel in the bound 26.2 world, scan the live blocks around that source anchor;
3. reject liquid, blocked headroom, unstable 3×3 ground, roof-height drift, source block entities and existing villagers/traders;
4. reject the authored road centerline and prefer a nearby readable shoulder;
5. choose the lowest-scoring valid point deterministically and face the actor toward the nearest road;
6. spawn no service for a role if no safe point exists.

The admin survey commands remain diagnostics only. Normal development/playtesting must not require the user to type one command per NPC.

Source/provenance: `THIRD_PARTY/drehmal-map-reference/SOURCE.md`.

### 6.6 Capital Valley route extraction

The same structured source is now used beyond the hub. The first-route corridor query covers the Stasis/Primal side through Temple Ruins, Capital Valley Tower, Warning Cave branch, Explorer's Campsite and the New Drabyel approach.

The extracted route window contains source-authored signs, storage, item frames, armor stands, named entities/traders and the road GeoJSON. Important preservation examples include:
- Drabyel direction sign near 581,66,831;
- Temple Ruins lore signs around 559,65~69,820~822;
- Capital Valley Tower title signs around 556~558,131,1175~1177;
- Warning Cave source storage including Bryde's Warning around 454,73,1252;
- Explorer's Guide camp storage around 581,81,1501;
- Drabyel/Av'Sal crossroads signs around 585~586,82~83,1437~1443;
- Village of Drabyel direction signs around 484,70~71,1537.

Runtime route placement therefore does not choose the first merely-flat block. Encounter and non-service NPC sites are scored toward readable road shoulders, source block/entity conflicts are rejected, battle candidates receive a wider source-content clearance check, and materialization repeats the source-content check when the player actually approaches the area.

This covers all **7 first-route combat groups** (6 Common + 1 optional Elite) and all **3 field NPCs**. If a site or arena cannot satisfy the same-zone safety rules, it remains absent instead of being moved to a semantically unrelated location.

## 7. Map-analysis workflow

For every new region:
1. inspect high-resolution render / official region map;
2. read official wiki settlement + POI pages;
3. record source coordinates as survey seeds;
4. classify spaces: safe hub / road / landmark / optional danger / dungeon / boss-sized clearing;
5. let the bound 26.2 runtime derive safe exact positions from those semantic zones and reject source-content conflicts;
6. inspect the resulting route in normal client play for sightline, camera, pathfinding and visual feel;
7. freeze a static coordinate only when there is a concrete reason to replace the adaptive resolver.

This keeps world understanding detailed without pretending a web coordinate is already a verified gameplay coordinate, while also avoiding manual coordinate authoring.


## 8. Structured-map scale and future region release plan — 2026-09-30

Pinned zachaa/DrehmalMap@72d82180cbe3f950f068cf2d8e8668c6b09d5c58 confirms that TURNBOUND is not limited to one large road.

data/locations.json:
- 376 structured locations total
- 285 in the overworld
- 79 in Lo'Dahr
- 9 in true_end
- 2 in space
- 1 in end
- overworld location-point bounds span about 31,409 blocks east-west and 11,122 blocks north-south
- major location categories include 68 buildings, 62 other locations, 38 abandoned towns, 26 campsites, 24 Avsohm facilities, 19 abandoned buildings, 17 small towns and 16 towns

data/paths.geojson:
- 644 path features
- 11,070 path coordinate points
- the main path network spans roughly 10.6k × 10.5k blocks

Near New Drabyel, named source landmarks are already hundreds of blocks apart: Explorer's Campsite is about 354 blocks straight-line, while Av'Sal center is about 772 blocks away. Therefore TURNBOUND early-game content should use compact live-resolved micro-objectives around the hub rather than forcing the player to commute between named macro landmarks.

Long-term production implication:
- treat major territories as unlockable regions/chapters;
- reveal distant map regions gradually instead of exposing every far destination as an immediate objective;
- connect unlocked regions through discovered fast travel, shortcuts and later mount support;
- preserve large untouched spaces for future characters, enemies, dungeons, hidden quests, bosses and region-specific systems.


### Physical-service boundary

TURNBOUND's global management menu is intentionally not a remote town-service toolbar.
- E menu may manage party, equipped ownership/loadout, quests, codex, records and settings.
- purchase/sale, enhancement, summon/Star Essence exchange and fast travel remain physical NPC/facility interactions.
- the global Archive page is read-only summon history/rates; exchange actions exist only in the physical summon facility.
- server gates remain authoritative even if a client attempts to send a facility command directly.
