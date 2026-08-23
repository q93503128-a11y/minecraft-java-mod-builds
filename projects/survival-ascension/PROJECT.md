# Survival Ascension

- Mod version: `0.32.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Network protocol: `8`
- Existing-world compatibility: `mining_progress_v1`, `infrastructure_v1`, `world_ascension_v1`, `expedition_v1`, `apex_hunt_v1`, `production_v1`, `field_depots_v1`, `outpost_v1`, `field_recovery_v1`, Elite/Warband/mutation persistent NBT, affix CustomData and mining modes remain intact. 0.32 adds independent `expedition_operations_v1`; no prior SavedData ID or packet schema is rewritten.

## Core direction
Progression must enlarge physical actions rather than only percentages. Larger actions create larger material throughput; world stages, behavior-rich enemies, exploration goals, infrastructure, production, logistics and field bases must consume that throughput again. Shift remains the precision/single-action safety override.

## 0.32 Out-and-back Expedition Operations
### Purpose
0.23–0.26 established regional discovery/directives/incidents, 0.30 established physical outposts, and 0.31 established bounded death recovery. 0.32 connects them into repeatable sorties where a player physically leaves a completed-region outpost, works beyond its immediate support radius, and returns to the exact same base to claim the operation.

This is not another quest-book layer and not a generic counter expansion. The operation lifecycle is `launch from physical outpost -> cross range line -> perform validated regional actions away from base -> return to exact origin outpost`.

### Launch contract
`M -> Infrastructure -> 산업 가공소 -> 원정 작전` reuses the existing string action payload.
- Survival/non-spectator player only.
- Server finds the nearest active owned outpost within4 blocks; no client position is trusted.
- Server reads the biome at the saved outpost anchor and resolves one of the existing nine ExpeditionRegion values.
- The player's original `expedition_v1` directive for that region must already be complete.
- Launch costs exactly one stored `production_v1` supply charge after validation.
- One active operation/player maximum.
- Apex Hunt and Ascension Trial cannot be started while an operation is active and an operation cannot launch while either is active.
- Regional Incidents are deliberately allowed to occur during operations because they are ambient regional risk, not a second manually started boss encounter.

### Nine authored operations
Each operation has exactly two validated-action tasks plus an outbound range requirement and return requirement.

Stage0, 20 minutes:
- WOODLAND `심림 순환 벌채`: range96, LOGS_FELLED128 + TRAVEL_DISTANCE240.
- ARID `사막 보급로 개척`: range96, BLOCKS_BUILT96 + TRAVEL_DISTANCE240.
- WETLAND `습지 채집·소탕`: range96, CROPS_HARVESTED80 + HOSTILES_KILLED8.
- HIGHLANDS `능선 장거리 순찰`: range128, TRAVEL_DISTANCE600 + DASHES_USED12.
- OCEAN `외해 순항`: range128, OCEAN_VOYAGE900 + HOSTILES_KILLED8.

Stage1, 25 minutes:
- DEEP `심층 채굴 회수`: range128, BLOCKS_MINED192 + HOSTILES_KILLED10.
- FROZEN `백설 장거리 순찰`: range128, TRAVEL_DISTANCE600 + HOSTILES_KILLED10.
- NETHER `네더 전진 작전`: range160, HOSTILES_KILLED24 + BLOCKS_MINED96.

Stage2, 30 minutes:
- END `공허 외곽 소탕`: range160, HOSTILES_KILLED28 + TRAVEL_DISTANCE360.

### Outbound/work/return rules
- Range is measured from the saved origin outpost anchor.
- Stage-specific range must be reached before any operation task progress may be recorded.
- After the range line is reached, task actions count only while the player is at least48 blocks from the origin outpost and current `ExpeditionProgression.currentRegion(player)` equals the operation region.
- Operation progress reuses the same server-authoritative actions already feeding expedition directives/incidents: natural smart-tree logs, successful protected/material-backed Construction placements, mature crops, legitimate travel, ocean voyage, valid pickaxe mining, hostile kills and validated successful dash uses.
- No raw client movement, inventory click, fake placement or near-base work is accepted.
- Completing both field objectives only changes the operation to return-ready state; reward is not granted remotely.
- Player must return to within8 blocks of the exact saved origin anchor before deadline.
- On return, `OutpostService.isRecoveryOperational` revalidates outpost ownership, real loaded Barrel, `mayInteract`, Bed/Campfire/Crafting/Furnace camp structure and loaded world state.
- No chunk ticket/force-loading and no client-supplied return coordinate.

### Failure / persistence
Operation fails and clears active state with no supply refund when:
- owner dies;
- owner changes dimension;
- owner switches to creative/spectator;
- deadline expires.

Breaking or unloading the origin outpost does not silently fabricate a return. Active operation stays persistent until return/failure; return can only complete when the real origin is loaded and operational again.

`expedition_operations_v1` persists per player:
- active region or empty;
- origin dimension/x/y/z;
- absolute game-time deadline;
- range-reached flag;
- two bounded task progress counters;
- first-return completed-region bitmask;
- lifetime successful-return count;
- one-time all-nine mastery reward flag.

Malformed active region names or incomplete active records sanitize to no active operation. Progress values clamp to the authored targets. Existing worlds simply have no `expedition_operations_v1` entry.

### Rewards
Successful return always awards the operation region's skill plus vanilla resources, and then clears active state.
- Stage0: skill XP250 + experience75 + Emerald8 + Amethyst8.
- Stage1: skill XP400 + experience125 + Diamond2 + Amethyst16 + Echo2.
- Stage2: skill XP600 + experience200 + Diamond4 + Echo4 + Dragon Breath2.

First successful return in each region sets that region bit. First time all nine bits are present grants exactly one extra package:
- Netherite Scrap2
- Echo Shard16
- Amethyst Shard64
- Dragon Breath8
- experience300

No permanent flat combat/stat multiplier is attached to operation completion.

### UI/status
- Existing MineMenu-derived Industrial Works radial adds `원정 작전` with a Spyglass icon.
- Selecting while no operation is active attempts launch; selecting while active reports progress/status instead of starting or charging again.
- `/ascension stats` reports operation first-return count, lifetime returns, active region and 9/9 reward state.
- Guide explains range/work/return rules.
- No new generic rectangular quest GUI and no new packet type.

## 0.31 Death-bound Field Recovery retained
- active outpost within4; first arm consumes supply1, retargeting already-paid armed token is free.
- same-dimension ordinary death within96 and operational outpost required.
- Regional Incident/Apex/Trial death excluded.
- independent `field_recovery_v1`; pending survives failed target/arrival/teleport.
- safe arrival requires loaded/interactable outpost, sturdy floor, empty body/head collision and no fluid.
- token is consumed only after successful teleport; no ordinary living-player fast travel.

0.32 operation death deliberately fails the operation. Field recovery remains a separate ordinary-death contract: if its own 96-block qualification succeeds, it may still return the player after the failed sortie. Stage1/2 operation range requirements can exceed96, so deeper sorties are not automatically covered by recovery.

## 0.30 Physical Field Outposts retained
- nearest owned registered Barrel within4;
- interactable Bed + Campfire/Soul Campfire + Crafting Table + Furnace/Blast Furnace/Smoker all within5;
- upgrade cost supply2 + Iron32 + Gold8 + Coal32;
- exact anchor persists in `outpost_v1`, max3/player, and removing underlying depot removes its upgrade.
- owner-nearby active state gives ordinary depot32 -> outpost64 logistics and NATURAL-hostile-only safe radius24.
- TRIGGERED Regional Incident/Apex/Trial spawns remain untouched.
- no chunk force-loading.

## 0.29 Physical Field Depots retained
- nearest vanilla Barrel within4 blocks; registration cost one supply charge.
- max3/player and one owner per physical position.
- same-dimension loaded Barrel inventory within32 blocks, `mayInteract` checked.
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

`production_v1` keeps buffers0..3, lifetime cycles and charges0..3. One cycle consumes one batch from all four lines. Supply charge consumption may free storage and immediately normalize waiting complete sets.

## 0.27 Apex Hunts retained
- Stage1 Apex Tracking Post: Iron512 + Gold256 + Amethyst256 + Echo32 + Nether Star1.
- completed-region hunt entry: Echo8 + Amethyst32 + Gold32.
- 90-second owner-scoped encounter and nine separate patterns CHARGE / REINFORCE / PLAGUE / SKIRMISH / PULL / LEAP / FROST / WITHER / VOID.
- `apex_hunt_v1` tracks first defeats, total victories and one-time9/9 reward.

## Expeditions / Field Mastery retained
- nine regions, two persistent directives each =18.
-18 rare incidents, one ambush + one action rush per region.
- actual gameplay hooks only; incident bonus max20% of first unfinished task once/region.
- all nine complete at Stage2 unlock Lv100 Field Mastery.
- Field values remain Quarry7x7x12, Wood448, Harvest13x13, Academy7.5/20, Construction line65/plane13x13, air dash4.

## Endgame retained
- stage0 Awakening -> Wither Stage1 Legendary -> Dragon Stage2 Endgame.
- Stage2 Withered/Phase/Plague mutation subset, Elite ranks and Warbands.
- Ascension Nexus four-wave Trial; Evoker remains excluded.
- valid Mythic III 3-affix gear can awaken once to4 affixes.

## Safety contracts
- large mining/wood/farm/construction work remains tick-budgeted.
- secondary destruction uses normal `player.gameMode.destroyBlock`.
- Construction/replant preserve interaction/protection hooks and actual resource consumption.
- depot/outpost/recovery/operation origins are server-resolved from saved/owned coordinates; no client coordinate trust or chunk tickets.
- outpost safe zone cancels NATURAL hostile spawn only; TRIGGERED combat remains unaffected.
- operation progress only accepts preexisting validated ExpeditionAction hooks after range gate and outside48.
- operation completion requires physical return to origin within8 and outpost revalidation.
- operation cannot overlap manually started Apex/Trial encounters; Regional Incidents may coexist.
- production remains bounded: line buffers3, supply charges3; depots3; outposts3; recovery one token; operation one active/player.
- no new packet schema in0.32; protocol remains8.

## External-source policy
- Heracles (`terrarium-earth/Heracles`): current repository license MIT. 0.32 studies only product-level explicit multi-step objective/completion state. No Heracles source structures, quest data, editor/UI, assets or namespace are copied.
- Bountiful (`ejektaflex/Bountiful`): GPL-3.0 reference-only for objective/reward contract philosophy; no source/data/UI/assets copied.
- Waystones 26.2 ARR and Corpse LGPL-3.0 remain reference-only for 0.31 travel/death friction.
- MineColonies remains GPLv3 reference-only for physical forward-base product lessons.
- Create code/assets split remains MIT/ARR; no Create logistics implementation/assets/data are copied.
- Building Gadgets2 MIT reference remains limited to material-backed protected Construction behavior.
- Existing packaged MIT/CC0 notices and all reference-only restrictions remain in `THIRD_PARTY_NOTICES.md`.
