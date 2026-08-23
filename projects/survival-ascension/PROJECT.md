# Survival Ascension

- Mod version: `0.30.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Network protocol: `8`
- Existing-world compatibility: `mining_progress_v1`, `infrastructure_v1`, `world_ascension_v1`, `expedition_v1`, `apex_hunt_v1`, `production_v1`, `field_depots_v1`, Elite/Warband/mutation persistent NBT, affix CustomData and mining modes remain intact. 0.30 adds independent `outpost_v1`; no prior SavedData ID or packet schema is rewritten.

## Core direction
Progression must enlarge physical actions rather than only percentages. Larger actions create larger material throughput; world stages, behavior-rich enemies, exploration goals, infrastructure, production, logistics and field bases must consume that throughput again. Shift remains the precision/single-action safety override.

## 0.30 Physical Field Outposts
### Purpose
0.29 made actual Barrels supply large-scale work. 0.30 turns selected depots into physical forward bases whose benefits depend on a real player-built camp layout instead of a virtual claim/menu flag.

### Upgrade contract
`M -> Infrastructure -> 산업 가공소 -> 전초기지 승격` uses the existing Industrial Works action payload.
- Server finds the nearest field depot already owned by the player within4 blocks; no client block position is trusted.
- Depot Barrel must still exist in a loaded chunk and be interactable.
- Within5 blocks of the Barrel the server must find at least one Bed, Campfire or Soul Campfire, Crafting Table, and Furnace/Blast Furnace/Smoker.
- Cost after validation: field-supply charges2 + Iron Ingots32 + Gold Ingots8 + Coal32.
- Iron/Gold/Coal availability combines player inventory and currently usable depot inventory; consumption remains player-first.
- Upgrade persists at that exact depot coordinate in `outpost_v1`.
- Removing/unlinking the underlying field depot also deletes its outpost upgrade without refund.
- Maximum outposts/player equals existing depot maximum3.

### `outpost_v1`
Independent per-player SavedData.
Per-player fields:
- uuid
- outposts list

Outpost entry:
- dimension string
- x/y/z anchor Barrel coordinate

Load sanitation deduplicates coordinates and caps malformed lists at3. Existing saves have no outpost record and load normally.

### Active-state contract
An upgraded outpost is functional only when every condition is true:
- owner is online and not spectator;
- owner is in the saved dimension;
- owner is within64 blocks of the anchor;
- anchor chunk is already loaded;
- anchor remains `Blocks.BARREL`;
- `level.mayInteract(owner, anchor)` succeeds;
- the four physical camp component categories remain within5 blocks.

Outposts never issue chunk tickets or force-load chunks. Missing camp blocks suspend benefits but do not erase the paid upgrade; rebuilding restores function.

### Extended logistics
- ordinary field depot supply radius remains32;
- an active upgraded outpost depot supplies to64;
- actual stock rules remain unchanged: player inventory first, then nearest usable Barrel containers;
- bulk Construction and irrigation still consume one real block/seed per successful action;
- protection checks and post-place rollback remain mandatory.

### Natural hostile safe zone
`OutpostService.onFinalizeSpawn` runs server-side.
- only entities implementing `Enemy` are candidates;
- only spawn type name exactly `NATURAL` is canceled;
- hostile spawn position must be within24 blocks of an active outpost anchor;
- active-owner and physical-structure rules are revalidated at spawn time;
- `TRIGGERED` spawns are never canceled, preserving Regional Incidents, Apex Hunts and Ascension Trials;
- spawner/command/event encounter spawns are likewise outside the NATURAL-only filter.

This safe zone is local operational security, not a permanent server-wide claim: once the owner leaves64 blocks or the camp is broken, normal natural spawning resumes.

### UI / status
The existing MineMenu-derived Industrial Works radial adds `전초기지 승격` with a Campfire icon.
- status explains structure, costs and active radii;
- `/ascension stats` reports upgraded outposts and currently active outposts next to depot status;
- no new rectangular management GUI is added.

## 0.29 Physical Field Depots retained
- nearest vanilla Barrel within4 blocks; registration cost one supply charge.
- max3/player and one owner per physical position.
- same-dimension loaded Barrel inventory within32 blocks, `mayInteract` checked.
- no force-loaded chunks or virtual stock.
- player inventory first, linked Barrels nearest-first.
- Construction and irrigation consume actual stock after normal protection/place validation and roll back on unexpected post-place consume failure.
- `field_depots_v1` remains unchanged and is the ownership source of truth for outpost anchors.

## 0.28 Industrial Works retained
`INDUSTRIAL_WORKS / 산업 가공소` is Stage1: Stone Bricks1024 + Iron512 + Copper512 + Redstone256 + Amethyst128.

Four atomic lines:
1. `METALWORKS`: Raw Iron96 + Raw Copper96 + Coal64.
2. `TIMBERWORKS`: logs192 + Cobblestone384 + Iron32.
3. `PROVISIONS`: Wheat128 + Carrot64 + Potato64 + Beetroot32.
4. `PRECISION`: Redstone128 + Amethyst64 + Gold32 + Quartz64.

`production_v1` keeps buffers0..3, lifetime cycles and charges0..3. One cycle consumes one batch from all four lines. Supply charge consumption may free storage and immediately normalize waiting complete sets. `consumeSupplyCharges(player, amount)` is the atomic multi-charge path used by 0.30 upgrade costs.

## 0.27 Apex Hunts retained
- Stage1 Apex Tracking Post: Iron512 + Gold256 + Amethyst256 + Echo32 + Nether Star1.
- completed-region hunt entry: Echo8 + Amethyst32 + Gold32.
- 90-second owner-scoped encounter and nine separate patterns CHARGE / REINFORCE / PLAGUE / SKIRMISH / PULL / LEAP / FROST / WITHER / VOID.
- `apex_hunt_v1` tracks first defeats, total victories and one-time9/9 reward.

## Expeditions / Field Mastery retained
- nine regions, two persistent directives each =18.
- 18 rare incidents, one ambush + one action rush per region.
- actual gameplay hooks only; incident bonus max20% of first unfinished task once/region.
- all nine complete at Stage2 unlock Lv100 Field Mastery.
- Field values remain Quarry7x7x12, Wood448, Harvest13x13, Academy7.5/20, Construction line65/plane13x13, air dash4.

## Endgame retained
- stage0 Awakening -> Wither Stage1 Legendary -> Dragon Stage2 Endgame.
- Stage2 Withered/Phase/Plague mutation subset, Elite ranks and Warbands.
- Ascension Nexus four-wave Trial.
- valid Mythic III 3-affix gear can awaken once to4 affixes.

## Safety contracts
- large mining/wood/farm/construction work remains tick-budgeted.
- secondary destruction uses normal `player.gameMode.destroyBlock`.
- Construction/replant preserve interaction/protection hooks and actual resource consumption.
- depot/outpost storage is local, same-dimension, loaded-chunk-only vanilla Barrel inventory; no chunk tickets or virtual stock duplication.
- outpost safe zone is owner-nearby and structure-backed, and cancels NATURAL hostile spawn only.
- TRIGGERED combat from incidents/Apex/Trial remains unaffected.
- production remains bounded: line buffers3, supply charges3; depots3; outposts3.
- no new packet schema in0.30; protocol remains8.

## External-source policy
- MineColonies: current public release pages identify GPLv3. 0.30 uses only the high-level product lesson that forward settlements combine physical facilities, supply and local defense. No MineColonies source, blueprints, citizens/workers, building definitions, research, raid code, UI, assets, data or namespace are copied.
- Create (`Creators-of-Create/Create`): code/assets split remains code MIT and `src/main/resources/assets/` ARR. Survival Ascension uses only high-level throughput/local-restocking ideas; no logistics implementation/package formats/blocks/assets/data/UI/namespaces are copied.
- Building Gadgets2 (MIT): earlier material-backed protected Construction reference only; field depot/outpost resolution is independent code.
- Existing packaged MIT/CC0 notices and all reference-only restrictions remain in `THIRD_PARTY_NOTICES.md`.
