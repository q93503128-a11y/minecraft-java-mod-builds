# Survival Ascension

- Mod version: `0.29.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Network protocol: `8`
- Existing-world compatibility: `mining_progress_v1`, `infrastructure_v1`, `world_ascension_v1`, `expedition_v1`, `apex_hunt_v1`, `production_v1`, Elite/Warband/mutation persistent NBT, affix CustomData and mining modes remain intact. 0.29 adds independent `field_depots_v1`; no prior SavedData ID or packet schema is rewritten.

## Core direction
Progression must enlarge physical actions rather than only percentages. Larger actions create larger material throughput; world stages, behavior-rich enemies, exploration goals, infrastructure, production and field logistics must consume that throughput again. Shift remains the precision/single-action safety override.

## 0.29 Physical Field Depots
### Purpose
0.28 made large resource throughput economically useful but the cycle still ended in player inventory. 0.29 turns industrial output into a physical worksite logistics layer: actual vanilla Barrels become supply nodes for bulk Construction and irrigation.

### Registration contract
`M -> Infrastructure -> 산업 가공소 -> 물류 거점 연결` sends the existing Industrial Works action payload.
- Find the nearest vanilla Barrel within 4 blocks; no client-supplied block position is trusted.
- Survival player only; Industrial Works must already be complete.
- Registration consumes one existing `production_v1` supply charge only after all depot validation passes.
- Re-selecting a nearby barrel already owned by the player unlinks it without refund.
- Maximum 3 depots per player.
- One physical barrel position may be owned by only one player.
- `mayInteract` is checked at registration.
- No new packet type; `InfrastructureActionPayload(projectId, action)` and protocol8 remain unchanged.

### `field_depots_v1`
New independent SavedData.
Per-player record:
- uuid
- list of depot entries

Depot entry:
- dimension string
- x/y/z block coordinates

Load sanitation:
- duplicate entries are deduplicated
- only first 3 entries per player survive malformed/oversized stored lists
- empty existing worlds simply have no depot entry

### Runtime material resolver
A depot can provide stock only if every condition holds:
- same dimension as the player
- player is within 32 blocks of the barrel
- barrel chunk is already loaded
- saved position still contains `Blocks.BARREL`
- block entity implements `Container`
- `level.mayInteract(player, depotPos)` succeeds

The system never force-loads a depot chunk. A linked barrel that is loaded but missing is pruned from `field_depots_v1`. Usable depots are nearest-first. Player inventory always has priority over depot inventory.

### Construction integration
Existing bulk Construction mode/tick/protection flow remains canonical.
- Availability check: carried inventory OR an active field depot must contain the exact block item.
- `EventHooks.onBlockPlace` and target survival/replacement checks still happen before placement.
- After `setBlockAndUpdate` succeeds, exactly one real stack item is consumed, player first then nearest depot.
- If post-placement consumption unexpectedly fails, the newly placed block is immediately rolled back and the job stops as out-of-material instead of granting a free block.
- Existing global64 block/tick and pending512/player limits remain unchanged.

### Irrigation integration
Existing irrigation eligibility, crop classification, survival checks and `EventHooks.onBlockPlace` remain.
- Seed/crop item availability uses player inventory then active depots.
- Supported resources remain Wheat Seeds / Carrot / Potato / Beetroot Seeds / Nether Wart.
- A successful replant consumes exactly one real seed/crop item.
- Unexpected post-place consume failure rolls the young crop back.
- No remote growth or crop spawning is added.

### UI / status
The existing MineMenu-derived production radial adds `물류 거점 연결` with a Barrel icon.
- Registration radius4, use radius32, max3 and charge cost are stated in UI/Guide.
- Production status prints registered/active depots and coordinates.
- `/ascension stats` reports depot count and current active count beside industrial cycles/charges.

## 0.28 Industrial Works retained
### Infrastructure gate
`INDUSTRIAL_WORKS / 산업 가공소` is a Stage1 shared project:
- Stone Bricks1024
- Iron Ingots512
- Copper Ingots512
- Redstone256
- Amethyst Shards128

### Four atomic batch lines
1. `METALWORKS`: Raw Iron96 + Raw Copper96 + Coal64.
2. `TIMBERWORKS`: any `ItemTags.LOGS`192 + Cobblestone384 + Iron32.
3. `PROVISIONS`: Wheat128 + Carrot64 + Potato64 + Beetroot32.
4. `PRECISION`: Redstone128 + Amethyst64 + Gold32 + Quartz64.

`production_v1` keeps buffers0..3, lifetime cycles and supply charges0..3. One cycle requires one batch from all four lines. Dispatch may still convert one charge into Gold32 + Amethyst16 + Echo2. Queue normalization after charge consumption remains mandatory so full line buffers cannot deadlock behind full charge storage.

## 0.27 Apex Hunts retained
- Stage1 Apex Tracking Post build: Iron512 + Gold256 + Amethyst256 + Echo32 + Nether Star1.
- Hunt entry from completed expedition region: Echo8 + Amethyst32 + Gold32.
- 90-second owner-scoped encounter, 10-second invalid-owner grace, 64-block owner radius, 48-block escort recall, 96-block hunt separation, logout/failure/orphan cleanup.
- Nine patterns: CHARGE / REINFORCE / PLAGUE / SKIRMISH / PULL / LEAP / FROST / WITHER / VOID.
- `apex_hunt_v1` tracks nine first kills, total victories and one-time 9/9 reward.
- Stage2 Trial still guarantees Mythic III and remains the strongest deterministic loot route.

## Expeditions retained
- Nine regions with two persistent directives each, 18 total.
- 18 rare incidents: one ambush + one action rush per region.
- Directive progress only comes from authoritative real gameplay hooks.
- Incident reward is once per player/region; directive bonus max20% of first unfinished task.
- 0.23/0.24/0.25 migration and reward bits remain intact.

## Mastery VI / Field Mastery retained
Base Lv100:
- Mining11x11, vein/extract192, Quarry7x7x10.
- Woodcutting384; queues12/player,64/global.
- Harvest11x11; queues12/player,64/global.
- Combat cleave10/r5/70%, Academy shockwave6.5/16/55%/50t.
- Construction line49, plane11x11, volume7x7x7.
- Mobility step2, safe fall16, dash1.80/16t, Stage2 Nexus air dash3.

After Stage2 + all nine expedition directives, at Lv100 only:
- Quarry depth12/pending640, still12/player and64/global.
- Woodcutting448.
- Harvest13x13.
- Academy shockwave7.5/20.
- Construction line65/plane13x13, volume still7^3.
- Air dash4.

## Endgame retained
- World stage0 Awakening -> first Wither Stage1 Legendary -> first Ender Dragon Stage2 Endgame.
- Stage2 mutation subset: Withered / Phase / Plague.
- Elite ranks, affixes, Warbands and Apex behavior can layer through their existing bounded contracts.
- Ascension Nexus unlocks repeatable four-wave doctrine Trial.
- Valid Mythic III with exactly3 known affixes can become four-affix Awakened Mythic once; awakened rerolls preserve4.

## Safety contracts
- Large mining/wood/farm/construction work remains tick-budgeted.
- Secondary destruction goes through normal `player.gameMode.destroyBlock`.
- Construction/replant preserve interaction/protection hooks and consume real resources.
- Field depots are local, same-dimension, loaded-chunk-only, actual Barrel inventory sources; no chunk tickets or virtual stock duplication.
- Production remains per-player, atomic and bounded: line buffers3, supply charges3.
- Field depots are per-player, bounded3, unique by physical position and persisted separately.
- No new packet schema in0.29; protocol remains8.

## External-source policy
- Create (`Creators-of-Create/Create`): current repository license split is code MIT and `src/main/resources/assets/` All Rights Reserved. 0.28 studied high-throughput multi-step production; 0.29 additionally studies only product-level stock-backed request/local-restocking concepts. The implementation is independent Survival Ascension SavedData + vanilla Barrel containers and does not copy Create logistics source, package formats, blocks, assets, data, UI, namespaces or Ponder content.
- Building Gadgets 2 (MIT): material-backed protected Construction remains the earlier reference. 0.29 field-depot storage resolution is new Survival Ascension code; no Building Gadgets storage/network code is copied.
- Existing packaged MIT/CC0 notices and reference-only restrictions remain in `THIRD_PARTY_NOTICES.md`.
