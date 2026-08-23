# Survival Ascension

- Mod version: `0.34.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Network protocol: `8`
- Existing-world compatibility: `mining_progress_v1`, `infrastructure_v1`, `world_ascension_v1`, `expedition_v1`, `apex_hunt_v1`, `production_v1`, `field_depots_v1`, `outpost_v1`, `field_recovery_v1`, `expedition_operations_v1`, Elite/Warband/mutation persistent NBT, affix CustomData and mining modes remain intact. 0.34 adds no SavedData field, ID or packet; it only routes additional stationary resource sinks through the existing physical logistics resolver. 0.33 complication migration remains unchanged and 0.32 active operations still decode with complication `NONE`.

## Core direction
Progression must enlarge physical actions rather than only percentages. Larger actions create larger material throughput; world stages, behavior-rich enemies, exploration goals, infrastructure, production, logistics and field bases must consume that throughput again. Shift remains the precision/single-action safety override.

## 0.34 Integrated Logistics Backbone / 통합 물류 백본
### Purpose
0.29 created physical Barrel depots, 0.30 upgraded some into physical outposts, and large Construction/irrigation already consumed their actual stock. But the largest stationary sinks — Industrial Works batches, unfinished infrastructure projects and equipment reforge/awakening — still required every item to be moved into player inventory first. That breaks the intended high-throughput loop once skills are processing hundreds of blocks at a time.

0.34 closes that gap without creating a virtual warehouse, global storage network or new GUI. The physical registered Barrel remains the source of truth.

### Shared physical stock resolver
`FieldDepotService` now exposes matcher-backed stock operations:
- `countMatching(ServerPlayer, Predicate<ItemStack>)`
- `consumeMatching(ServerPlayer, Predicate<ItemStack>, amount)`
- exact-item `countMaterial/consume` delegate to those generic paths.

This allows both exact materials and tag-like production requirements such as mixed logs to use the same physical inventory contract.

Resolution is deterministic:
1. player inventory is counted/consumed first;
2. eligible linked Barrels are sorted by distance and consumed nearest-first;
3. an ordinary depot is usable only inside32 blocks;
4. an active physical outpost extends only its own depot to64 blocks;
5. depot must be in the player's current dimension;
6. its chunk must already be loaded;
7. the saved block must still be a vanilla Barrel with a Container block entity;
8. `mayInteract` must pass.

A loaded missing/non-Barrel link is pruned through existing `field_depots_v1` cleanup and its outpost upgrade is removed through the existing callback. No chunk tickets, force-loads, cross-dimension access, client coordinates or virtual material balances are added.

### Integrated stationary sinks
**Industrial production**
- all four `ProductionProgram` lines count and consume from inventory + usable linked Barrel stock;
- `TIMBERWORKS` log tags work through the matcher path, so mixed vanilla logs across inventory/Barrels can satisfy the batch;
- the precheck is still performed before consumption;
- player stock is consumed before nearest linked Barrel stock;
- buffers/cycles/supply-charge caps remain unchanged.

**Infrastructure funding**
- unfinished projects use `FieldDepotService.countMaterial/consume` instead of local inventory-only loops;
- once a physical logistics network exists, large later projects such as Apex Tracking Post / Ascension Nexus can consume nearby stored throughput directly;
- `INDUSTRIAL_WORKS` itself is naturally inventory-funded on a normal new world because registering a depot requires Industrial Works to already be complete; no bootstrap shortcut is introduced.

**Equipment economy**
- Elite/Ascension/Mythic reforge costs and Mythic III awakening costs use the same local physical stock resolver;
- held gear still remains in the player's main hand and all affix validation/mutation rules remain unchanged;
- salvage output still goes to player inventory/drop and does not silently inject rewards into linked Barrels.

### Deliberate boundary: field encounter preparation
Apex Hunt and Ascension Trial entry costs intentionally remain inventory-only. These costs are field-combat preparation, not stationary base processing. A player still physically carries Echo/Amethyst/Gold for an Apex and Echo/Amethyst/Dragon Breath for the Trial.

Industrial supply dispatch likewise remains a physical output to player inventory/drop. 0.34 is therefore a **local base-side input logistics** expansion, not universal remote payment.

### Compatibility / presentation
- no new SavedData;
- no new packet or protocol bump (`8` remains);
- no new radial/menu page;
- Guide and status text explain inventory-first + nearest usable Barrel resolution and the combat-entry exception;
- all 0.33 sortie complication rules and older gameplay contracts remain unchanged.

## 0.33 Sortie Complications / 원정 작전 변수
### Purpose
0.32 established a persistent physical loop: `launch from exact outpost -> cross outbound range -> perform validated regional actions -> return to exact outpost`. 0.33 adds replay variation by changing the **rules of that route** rather than adding more objective counters, another triggered encounter, generic quest UI, blanket HP scaling or permanent stat inflation.

Every new launch receives exactly one server-chosen `ExpeditionComplication`:
- `DEEP_FRONT / 전선 고착`: after the outbound range has been crossed, operation actions count only while the player remains at or beyond that operation's authored outbound radius. The normal 48-block work radius is not sufficient for this complication.
- `FORWARD_SHIFT / 전선 재전개`: the first completed field objective pauses all remaining objective progress. The player must reach a second line at `operation.rangeTarget + 48` while still in the matching ExpeditionRegion. After that line is reached, the remaining validated actions resume.
- `HOT_EXTRACTION / 긴급 철수`: when both field objectives become complete, an extraction deadline is armed. Stage0 window 4800 ticks / 4:00, Stage1 3600 / 3:00, Stage2 3000 / 2:30. The extraction deadline is never later than the operation's original deadline.

### Selection and persistence
- Complication selection is server-side and uses server game time, player UUID and lifetime completion count only as entropy; the client never selects or supplies complication state.
- Exactly one complication per active operation/player. `NONE` exists only as migration/compatibility state for 0.32 active sorties.
- `expedition_operations_v1` keeps the same SavedData identifier and adds optional `complication`, `complication_state`, `extraction_deadline` fields.
- Missing complication fields decode as `NONE`, `0`, `0`, so an already-paid 0.32 active operation continues under its original rules instead of receiving a surprise modifier after updating.
- Malformed complication names sanitize to `NONE`. Forward-shift state is bounded; extraction state accepts only the authored armed value; extraction deadlines are capped to the original operation deadline.
- Complication state survives logout/server restart together with the original operation state.

### Runtime boundaries
- Complications reuse only the existing validated `ExpeditionAction` plumbing. They do not create an alternate client counter or inventory-based progress path.
- `DEEP_FRONT` adds a distance gate before `addProgress`.
- `FORWARD_SHIFT` locks `addProgress` while its second-line state is pending and unlocks only after a server-side distance + matching-region check.
- `HOT_EXTRACTION` starts only when both objectives are actually complete and fails through the same operation failure path if the return clock expires.
- Physical completion is still only at the exact saved origin within8 blocks after `OutpostService.isRecoveryOperational` revalidates the real loaded/interactable Barrel and camp structure.
- Death, dimension exit, creative/spectator change and base operation deadline remain failures. No supply refund.
- Regional Incidents remain ambient and may coexist. Apex Hunt and Ascension Trial remain manually mutually exclusive with operations.
- No operation teleport, no client destination, no chunk ticket/force-load, no new packet. Protocol remains8.

### Presentation
The existing Industrial Works radial action is unchanged. Launch/status system messages expose the selected complication, pending forward redeployment line or active emergency-extraction timer. The Guide explains all three. There is no new generic quest screen.

### External design references
- Deep Rock Galactic is used only for the product-level idea that a repeatable mission can carry a bounded mutator and a distinct post-objective extraction phase.
- Warframe Sorties / Deep Archimedea are used only for the product-level idea that mission modifiers should change strategy without replacing the base mission's objective identity.
- No source code, data, UI, assets, audio, namespaced content or proprietary game content from either product is copied or bundled.

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
- `WORK_RADIUS = 48`: after the range line is reached, task actions count only while the player is at least48 blocks from the origin outpost and current `ExpeditionProgression.currentRegion(player)` equals the operation region.
- Operation progress reuses the same server-authoritative actions already feeding expedition directives/incidents: natural smart-tree logs, successful protected/material-backed Construction placements, mature crops, legitimate travel, ocean voyage, valid pickaxe mining, hostile kills and validated successful dash uses.
- No raw client movement, inventory click, fake placement or near-base work is accepted.
- Completing both field objectives only changes the operation to return-ready state; reward is not granted remotely.
- Player must return to within8 blocks of the exact saved origin anchor before deadline.
- On return, `OutpostService.isRecoveryOperational` revalidates outpost ownership, real loaded Barrel, `mayInteract`, Bed/Campfire/Crafting/Furnace camp structure and loaded world state.
- No chunk ticket/force-loading and no client-supplied return coordinate.

### Failure / persistence
Operation fails and clears active state with no supply refund when owner dies, changes dimension, switches creative/spectator or exceeds the deadline. Breaking/unloading the origin does not fabricate a return; the real origin must become operational again.

`expedition_operations_v1` persists active region/origin/deadline/range flag/two task counters/first-return bitmask/lifetime returns/9-of-9 reward plus the optional 0.33 complication state. Malformed active records sanitize to bounded/no-active states.

### Rewards
- Stage0: skill XP250 + experience75 + Emerald8 + Amethyst8.
- Stage1: skill XP400 + experience125 + Diamond2 + Amethyst16 + Echo2.
- Stage2: skill XP600 + experience200 + Diamond4 + Echo4 + Dragon Breath2.
- First successful return in all nine: Netherite Scrap2 + Echo16 + Amethyst64 + Dragon Breath8 + experience300 once.

No permanent flat combat/stat multiplier is attached to operation completion.

## 0.31 Death-bound Field Recovery retained
- active outpost within4; first arm consumes supply1, retargeting already-paid armed token is free.
- same-dimension ordinary death within96 and operational outpost required.
- Regional Incident/Apex/Trial death excluded.
- independent `field_recovery_v1`; pending survives failed target/arrival/teleport.
- safe arrival requires loaded/interactable outpost, sturdy floor, empty body/head collision and no fluid.
- token is consumed only after successful teleport; no ordinary living-player fast travel.

0.32+ operation death deliberately fails the operation. Field recovery remains a separate ordinary-death contract under its own same-dimension/96-block rules.

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
- same-dimension loaded Barrel inventory within32 blocks, `mayInteract` checked; active outpost version extends to64.
- player inventory first, linked Barrels nearest-first.
- 0.34 shares the same resolver with Construction, irrigation, industrial batches, post-Industrial infrastructure funding and equipment reforge/awakening.
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
- completed-region hunt entry remains **player-carried** Echo8 + Amethyst32 + Gold32.
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
- Ascension Nexus four-wave Trial; Evoker remains excluded; Trial entry cost remains player-carried.
- valid Mythic III 3-affix gear can awaken once to4 affixes; its stationary resource cost can use local physical logistics in0.34.

## Safety contracts
- large mining/wood/farm/construction work remains tick-budgeted.
- secondary destruction uses normal `player.gameMode.destroyBlock`.
- Construction/replant preserve interaction/protection hooks and actual resource consumption.
- depot/outpost/recovery/operation origins are server-resolved from saved/owned coordinates; no client coordinate trust or chunk tickets.
- all 0.34 logistics sinks use the existing usable-depot path: current dimension,32/64 radius, loaded real Barrel, `mayInteract`, inventory-first and nearest-Barrel-first.
- matcher-backed production cannot reach an unlinked/unloaded/out-of-range/cross-dimension container.
- Apex/Trial entry costs remain inventory-only and dispatch remains player-carried; 0.34 is not universal remote payment.
- outpost safe zone cancels NATURAL hostile spawn only; TRIGGERED combat remains unaffected.
- operation progress only accepts preexisting validated ExpeditionAction hooks after range gate and outside48, then applies the selected0.33 complication gate.
- operation completion requires physical return to origin within8 and outpost revalidation.
- operation cannot overlap manually started Apex/Trial encounters; Regional Incidents may coexist.
- production remains bounded: line buffers3, supply charges3; depots3; outposts3; recovery one token; operation one active/player and one complication/operation.
- no new SavedData or packet schema in0.34; protocol remains8.

## External-source policy
- 0.34 introduces no new external-code or asset reference; it is an integration of existing Survival Ascension systems.
- Deep Rock Galactic and Warframe Sortie/Deep Archimedea are 0.33 product-level design references for bounded mission modifiers and extraction pressure. No source code, data, UI, assets, audio, namespaced content or proprietary game content is copied or bundled.
- Heracles (`terrarium-earth/Heracles`): current repository license MIT. 0.32 studies only product-level explicit multi-step objective/completion state. No Heracles source structures, quest data, editor/UI, assets or namespace are copied.
- Bountiful (`ejektaflex/Bountiful`): GPL-3.0 reference-only for objective/reward contract philosophy; no source/data/UI/assets copied.
- Waystones26.2 ARR and Corpse LGPL-3.0 remain reference-only for0.31 travel/death friction.
- MineColonies remains GPLv3 reference-only for physical forward-base product lessons.
- Create code/assets split remains MIT/ARR; no Create logistics implementation/assets/data are copied.
- Building Gadgets2 MIT reference remains limited to material-backed protected Construction behavior.
- Existing packaged MIT/CC0 notices and all reference-only restrictions remain in `THIRD_PARTY_NOTICES.md`.
