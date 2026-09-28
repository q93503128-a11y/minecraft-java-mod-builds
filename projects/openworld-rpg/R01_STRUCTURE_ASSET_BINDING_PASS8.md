# Open-World RPG — R01 Structure Asset Binding Pass 8

> Status: **OFFICIAL CC0 STANDARD SOURCES RE-VERIFIED / REPRODUCIBLE ARCHIVE INTAKE GATE ADDED / SERVICE-FAMILY BINDING CLOSED / EXACT PREFAB COMPOSITION STILL VISUAL-REVIEW GATED**
>
> Date: 2026-09-28
>
> Canon owners: `R01_VERTICAL_SLICE.md`, `R01_CONTENT_BIBLE.md`, `R01_ASSET_INTAKE.md`, `PRODUCTION_ASSET_BINDING_MATRIX.md`
>
> Spatial predecessors: `AZARI_R01_ALDERFORD_LANDMARK_PASS6.md`, `AZARI_R01_GATHERING_SPATIAL_PASS7.md`

## 1. Current authoritative source check

The creator-uploaded OpenGameArt pages were re-verified on 2026-09-28.

### Medieval Village MegaKit Standard

```text
source page: https://opengameart.org/content/medieval-village-megakit
uploader: quaternius
license shown on source page: CC0
archive: medieval_village_megakitstandard.zip
published Standard count: 176 models
direct archive URL:
https://opengameart.org/sites/default/files/medieval_village_megakitstandard.zip
```

The page describes a grid-snapping modular village kit with exterior+interior walls, roofs, stairs, doors, windows, vines and other village pieces.

### Fantasy Props MegaKit Standard

```text
source page: https://opengameart.org/content/fantasy-props-megakit
uploader: quaternius
license shown on source page: CC0
archive: fantasy_props_megakitstandard.zip
published Standard count: 94 models
direct archive URL:
https://opengameart.org/sites/default/files/fantasy_props_megakitstandard.zip
```

The page describes medieval/fantasy props spanning weapons, tools, vegetables, potions, market stalls, chests and furniture.

These source-specific OpenGameArt uploads remain the license authority for these exact Standard artifacts. Current central Quaternius terms are not backfilled over the source-specific CC0 snapshots.

## 2. Acquisition boundary

This execution environment could resolve the official pages and direct ZIP URLs but could not materialize the binary ZIPs into the local container.

Therefore:

```text
official source page verified: YES
direct archive URL verified: YES
license on source page verified: CC0
actual project archive bytes acquired here: NO
project-local archive SHA-256: NO
full archive 3D visual review: NO
```

No third-party mirror is promoted to license authority and no mirror hash is presented as the project's acquisition hash.

## 3. Reproducible intake gate

New tool:

```text
tools/r01_structure_asset_intake.py
```

Example after the two creator archives are available locally:

```powershell
py tools\r01_structure_asset_intake.py \
  --medieval "C:\path\to\medieval_village_megakitstandard.zip" \
  --props "C:\path\to\fantasy_props_megakitstandard.zip" \
  --output ".local\asset-intake\r01-structure-packs.json"
```

The tool:

- hashes the actual ZIP bytes with SHA-256;
- rejects unsafe ZIP member paths;
- counts unique model basenames across glTF/GLB/FBX/OBJ;
- checks the published Standard model-count floor;
- verifies the already-corroborated Medieval Village modular files;
- verifies `Potion_1.gltf` through `Potion_4.gltf` in Fantasy Props;
- emits a JSON intake report;
- returns nonzero when required files or the published model-count floor are missing.

Synthetic verification performed in this work session:

```text
Python compile: PASS
complete 176-model Medieval synthetic archive: PASS
complete 94-model Fantasy Props synthetic archive: PASS
required Medieval file set: PASS
Potion_1..4 required set: PASS
incomplete Fantasy Props archive: correctly rejected with exit 1
```

This proves the verifier logic, not the contents of the real creator ZIPs.

## 4. R01 service-family binding

The exact building composition is still gated by the real archive/3D review, but implementation no longer chooses an art family service-by-service.

| R01 place | Architecture family | Prop/detail family | Current binding |
|---|---|---|---|
| Alderford Gate/watch | Medieval Village Standard | Fantasy Props where suitable | family bound; exact composition pending |
| first shrine | Medieval Village-compatible authored masonry/wood composition | separate accepted shrine details required if Standard kit lacks a convincing shrine silhouette | exact shrine still gated |
| The Copper Kettle | Medieval Village Standard | Fantasy Props + already accepted food/kitchen families | family bound |
| Wayfarers' Hall | Medieval Village Standard | Fantasy Props for ordinary interior/service dressing | family bound |
| Holt Forge | Medieval Village Standard | KayKit RPG Tools + compatible Fantasy Props | family bound |
| Alderford Vault | Medieval Village Standard | Fantasy Props storage/chest/furniture candidates | family bound |
| Greenwater Remedies | Medieval Village Standard | Fantasy Props potion/furniture candidates | family bound |
| Fordside Stables | Medieval Village Standard | Fantasy Props ordinary stable/market dressing where the real archive supports it | family bound |
| market/basic merchant | Medieval Village Standard canopy/adjacent shell language | Fantasy Props market-stall/ordinary market props | family bound |
| Gate/Paddock/Riverside/Quarry-Road Cottages | Medieval Village Standard | restrained coherent interior family | family bound |
| Market House | Medieval Village Standard, larger/taller authored composition than Small Cottage | same coherent interior family | family bound |
| Mosswheel Mill | Medieval Village Standard is first architecture candidate | Fantasy Props ordinary sacks/crates/tools where suitable | family bound, wheel/bank fit pending |

This is intentionally one coherent settlement language, not thirteen unrelated downloaded buildings.

## 5. Quarry presentation boundary

The Medieval Village kit does **not** become the Quarry dungeon.

Quarry composition remains:

```text
real Azari mountain/cave identity
+ authored excavation
+ Minecraft-integrated stone/timber structure
+ accepted ordinary quarry props/tools
+ project-owned encounter geometry
```

Fantasy Props and KayKit RPG Tools may provide carts/crates/tools/worksite details where exact archive review supports them.

Do not paste a complete medieval house underground and call it a mining room.

## 6. Exact modular vocabulary already corroborated

Existing evidence retained from `R01_ASSET_INTAKE.md`:

```text
glTF/Wall_Plaster_Straight.gltf
glTF/Wall_Plaster_Window_Wide_Round.gltf
glTF/Wall_Plaster_Door_Round.gltf
glTF/Wall_UnevenBrick_Straight.gltf
glTF/Roof_RoundTiles_6x8.gltf
glTF/Roof_Front_Brick6.gltf
glTF/Prop_Chimney.gltf
glTF/Balcony_Cross_Straight.gltf
glTF/Prop_Vine1.gltf
glTF/Prop_Vine4.gltf
```

Fantasy Props potion candidate universe remains:

```text
Potion_1.gltf
Potion_2.gltf
Potion_3.gltf
Potion_4.gltf
```

Filename evidence is not enough to choose the final potion assignment or exact building layout.

## 7. What may now proceed

The project may now implement non-visual structure-binding infrastructure around these fixed family IDs without inventing temporary art.

Allowed:

- prefab/composition data schema;
- spatial anchor → structure-family binding;
- server-owned service IDs/interaction locations;
- deterministic structure placement pipeline;
- asset conversion tooling once creator ZIPs are present;
- collision/navigation validation around accepted converted pieces.

Still blocked until real archive/visual review:

- exact wall/roof module layout per building;
- final door facing/pivot;
- final shrine silhouette;
- final stable/paddock prop set;
- final Market House exterior composition;
- exact Mosswheel wheel/model fit;
- player-facing screenshots claimed as accepted production presentation.

## 8. State

```text
R01 STRUCTURE FAMILY DIRECTION: BOUND
OFFICIAL OPEN_GAME_ART SOURCE RE-VERIFIED: YES
SOURCE-SPECIFIC LICENSE: CC0
REPRODUCIBLE ARCHIVE VERIFIER: TESTED
REAL CREATOR ARCHIVES ACQUIRED IN THIS ENVIRONMENT: NO
REAL ARCHIVE SHA-256: NO
EXACT PREFAB COMPOSITION: NO
MINECRAFT VISUAL ACCEPTANCE: NO
R01 ASSET_BINDING COMPLETE: NO
R01 SPATIAL_BINDING COMPLETE: NO
R01 IMPLEMENTED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
