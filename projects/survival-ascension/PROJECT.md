# Survival Ascension

- Mod version: `0.31.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Network protocol: `8`
- Existing-world compatibility: `mining_progress_v1`, `infrastructure_v1`, `world_ascension_v1`, `expedition_v1`, `apex_hunt_v1`, `production_v1`, `field_depots_v1`, `outpost_v1`, Elite/Warband/mutation persistent NBT, affix CustomData and mining modes remain intact. 0.31 adds independent `field_recovery_v1`; no prior SavedData ID or packet schema is rewritten.

## Core direction
Progression must enlarge physical actions rather than only percentages. Larger actions create larger material throughput; world stages, behavior-rich enemies, exploration goals, infrastructure, production, logistics and field bases must consume that throughput again. Shift remains the precision/single-action safety override.

## 0.31 Death-bound Field Recovery
### Purpose
0.30 made outposts valuable while the owner is physically operating around them. 0.31 adds a bounded death-recovery role so distant expeditions do not become repeated long corpse runs, without adding ordinary fast travel that would bypass Mobility or exploration.

### Arming contract
`M -> Infrastructure -> 산업 가공소 -> 현장 복귀 계약` reuses the existing Industrial Works action payload.
- Player must be Survival/non-spectator and within4 blocks of an **active** owned outpost.
- First arm consumes exactly one existing `production_v1` supply charge in advance.
- The paid one-use token is stored in new independent `field_recovery_v1` as one `armed` recovery point.
- A currently armed paid token may be retargeted to another active outpost for no additional charge; this moves the existing token rather than minting another.
- Re-selecting the same armed outpost does not consume another charge.
- No normal/manual outpost teleport exists. `configure()` may only teleport when a previous qualifying death is already in `pending` state.

### Death qualification contract
`FieldRecoveryService.onLivingDeath` may move `armed -> pending` only when all conditions hold:
- dead entity is the owning `ServerPlayer`;
- a paid `armed` point exists and no `pending` point already exists;
- player is not currently in a Regional Incident, Apex Hunt or Ascension Trial;
- death dimension exactly matches the armed outpost dimension;
- death position is within96 blocks of the saved outpost anchor;
- the anchor is still recorded in `outpost_v1`;
- the target outpost is recovery-operational: loaded real Barrel, interactable anchor, and the interactable Bed/Campfire/Crafting/Furnace camp structure remains complete.

Challenge deaths explicitly do not queue or consume the recovery contract. The existing Incident/Apex/Trial failure rules therefore remain authoritative and field recovery cannot become an extra life inside those encounters.

### `field_recovery_v1`
Independent per-player SavedData.
Per-player fields:
- uuid
- optional one-entry `armed` list
- optional one-entry `pending` list
- lifetime successful `recoveries`

Recovery point:
- dimension string
- x/y/z outpost anchor

Load sanitation takes at most the first armed and first pending entry. Existing worlds have no entry and load with no recovery contract.

State transitions:
- first paid setup -> `armed`
- qualifying death -> `pending`, while `armed` is cleared
- successful recovery -> `pending` cleared and lifetime recovery count incremented
- failed recovery -> `pending` retained
- manual pending recovery retry may use the same target; if it remains unusable while the player stands beside another active outpost, `rearmPending` moves the prepaid token to that outpost without another supply charge.

### Respawn recovery
`FieldRecoveryService.onPlayerRespawn` schedules a server-side recovery attempt after vanilla respawn processing.
- The target `ServerLevel` is resolved from the saved dimension string.
- No chunk loading/forcing is requested; the outpost anchor and candidate arrival blocks must already be loaded.
- `OutpostService.isRecoveryOperational` revalidates outpost record, real Barrel, `mayInteract`, and the real four-part camp layout.
- Safe arrival scan stays within the physical camp radius and requires:
  - loaded candidate;
  - `mayInteract` at feet;
  - sturdy floor below;
  - empty body and head collision shapes;
  - no fluid in body/head cells.
- `ServerPlayer.teleportTo` must report success before the token is consumed.
- On success movement is zeroed and fall distance cleared.
- If target/arrival/teleport fails, the pending token remains available rather than being lost.

### UI / status
The existing MineMenu-derived Industrial Works radial adds `현장 복귀 계약` with a Compass icon.
- text states active-outpost requirement, supply1 cost,96-block general-death scope and one-use behavior;
- Industrial status reports armed/pending/unset state and lifetime successful recoveries;
- `/ascension stats` reports the same compact state;
- no new generic rectangular GUI and no new packet type are introduced.

## 0.30 Physical Field Outposts retained
### Upgrade contract
- nearest owned registered Barrel within4;
- interactable Bed + Campfire/Soul Campfire + Crafting Table + Furnace/Blast Furnace/Smoker all within5;
- upgrade cost supply2 + Iron32 + Gold8 + Coal32;
- physical camp component blocks and Barrel must pass `mayInteract` checks;
- exact anchor persists in `outpost_v1`, max3/player, and removing underlying depot removes its upgrade.

### Active-state / benefits
- owner online/non-spectator, same dimension, within64;
- anchor and camp chunks already loaded; no force loading;
- Barrel and interactable camp still physically present;
- ordinary depot radius32, active outpost depot radius64;
- NATURAL hostile spawns only are suppressed within24;
- TRIGGERED Regional Incident/Apex/Trial spawns remain untouched.

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

`production_v1` keeps buffers0..3, lifetime cycles and charges0..3. One cycle consumes one batch from all four lines. Supply charge consumption may free storage and immediately normalize waiting complete sets. `consumeSupplyCharges(player, amount)` remains the atomic multi-charge path for outpost upgrades; single-charge consumption is used for recovery contracts.

## 0.27 Apex Hunts retained
- Stage1 Apex Tracking Post: Iron512 + Gold256 + Amethyst256 + Echo32 + Nether Star1.
- completed-region hunt entry: Echo8 + Amethyst32 + Gold32.
-90-second owner-scoped encounter and nine separate patterns CHARGE / REINFORCE / PLAGUE / SKIRMISH / PULL / LEAP / FROST / WITHER / VOID.
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
- Ascension Nexus four-wave Trial; `AscensionTrialSystem.isActive(player)` is a read-only overlap check added for recovery exclusion and does not alter Trial lifecycle.
- valid Mythic III 3-affix gear can awaken once to4 affixes.

## Safety contracts
- large mining/wood/farm/construction work remains tick-budgeted.
- secondary destruction uses normal `player.gameMode.destroyBlock`.
- Construction/replant preserve interaction/protection hooks and actual resource consumption.
- depot/outpost/recovery targets are server-resolved from saved/owned coordinates, loaded-chunk-only and `mayInteract` revalidated; no client coordinate trust or chunk tickets.
- outpost safe zone is owner-nearby and structure-backed, canceling NATURAL hostile spawn only.
- TRIGGERED combat from incidents/Apex/Trial remains unaffected.
- recovery cannot queue during those encounters and cannot be invoked as normal fast travel.
- pending recovery is consumed only after successful teleport.
- production remains bounded: line buffers3, supply charges3; depots3; outposts3; recovery has at most one armed and one pending point.
- no new packet schema in0.31; protocol remains8.

## External-source policy
- Waystones (`TwelveIterations/Waystones`): current 26.2 branch license is All Rights Reserved. 0.31 uses only the product-level comparison around teleport convenience and intentionally does not implement always-available outpost fast travel. No source, blocks/items, warp/return mechanics, menus, data, assets or namespace are copied.
- Corpse (`denmeh/Corpse`): LGPL-3.0. 0.31 uses only the high-level product goal of reducing repetitive death-recovery travel. No corpse entity/container, inventory-storage/transfer implementation, source, data, assets or namespace are copied.
- MineColonies remains GPLv3 reference-only for physical forward-base product lessons.
- Create (`Creators-of-Create/Create`): code/assets split remains code MIT and `src/main/resources/assets/` ARR. Survival Ascension uses only high-level throughput/local-restocking ideas; no logistics implementation/package formats/blocks/assets/data/UI/namespaces are copied.
- Building Gadgets2 (MIT): earlier material-backed protected Construction reference only; field depot/outpost resolution is independent code.
- Existing packaged MIT/CC0 notices and all reference-only restrictions remain in `THIRD_PARTY_NOTICES.md`.
