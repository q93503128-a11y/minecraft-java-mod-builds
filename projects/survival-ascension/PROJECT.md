# Survival Ascension

- Mod version: `0.39.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Network protocol: `8`
- Existing-world compatibility: all existing SavedData IDs remain unchanged. 0.39 adds no new SavedData ID or migration; physical fortification qualification is derived from currently loaded real blocks around an existing `outpost_v1` anchor.

## Core direction
Progression enlarges physical player actions rather than mainly inflating percentages. Bigger actions create larger throughput; infrastructure, real storage, bases, expeditions and behavior-driven enemies must consume it again. Shift remains the precision/single-action safety override.

## 0.39 Physical Bastion Defense / 물리 요새 방어
### Purpose
0.38 proved the physical outpost itself could be a combat objective. 0.39 closes the remaining construction gap: large construction scale now creates a defensive structure that the battle actually uses, without auto-building a second structure system or awarding a permanent generic defense stat.

### Explicit action
`ProductionService.ACTION_BASTION_SIEGE = "bastion_siege"` reuses the existing Industrial Works action payload.

`M -> Infrastructure -> 산업 가공소 -> 요새 방어전` requires:
- survival/non-spectator;
- completed Industrial Works;
- active owned outpost anchor within4;
- no active Regional Incident / Expedition Operation / Apex Hunt / Ascension Trial;
- valid distributed physical fortification ring;
- two stored field-supply charges.

The first bastion wave must spawn successfully before the two supply charges are consumed.

### Physical fortification scan
`OutpostFortificationService` defines:
- `INNER_RADIUS = 6`
- `OUTER_RADIUS = 12`
- `VERTICAL_DOWN = 3`
- `VERTICAL_UP = 4`
- `MIN_COLUMNS_PER_QUADRANT = 12`
- `MIN_TOTAL_COLUMNS = 48`

Accepted defensive blocks:
- any block in `BlockTags.WALLS`;
- `Blocks.IRON_BARS`;
- `Blocks.NETHER_BRICK_FENCE`.

The annulus is split into NE/NW/SE/SW. A fortified x/z coordinate counts once even if the wall is several blocks tall, so a few tall pillars cannot inflate the requirement. Each quadrant independently needs12 columns.

Scanning is server-side and already-loaded-only via `level.hasChunkAt`. There is no `getChunk`, ticket or force-load.

### Why no fortification SavedData
The player's wall itself is the truth. 0.39 stores no `fortified=true` bit and gives no passive armor percentage. If the player tears the wall down, later validation sees that immediately. This avoids stale virtual state and keeps the build visible in the world.

### Bastion encounter
`OutpostSiegeSystem` now has two modes:
- OUTPOST: retained 3 waves / 4800 ticks / supply1;
- BASTION: new 4 waves / 6000 ticks / supply2.

Both retain:
- owner inside64;
- real outpost operational through `OutpostService.isRecoveryOperational`;
- anchor-directed attackers;
- engage radius16;
- breach radius6;
- breach limit200;
- breach pressure `+ breachers * 5` every five ticks and `-10` while clear;
- loaded radius26~34 `TRIGGERED` spawns;
- 3-second regroup window;
- common Incident/Operation/Apex/Trial overlap exclusion and Field Recovery death exclusion.

Bastion mode revalidates all four physical fortification quadrants between waves. The wall's ordinary collision/pathfinding obstruction is its real combat value; there is no bastion-only HP, attack, armor or damage-reduction modifier.

### Authored bastion pressure
Stage1 moves from mixed undead/ranged pressure into pillager/vindicator/witch overlap and then multiple Ravagers in the final wave.

Stage2 increases simultaneous role overlap further and ends with three Ravagers plus ranged/melee/disruption pressure. Evokers remain absent so the Ascension Trial keeps its own encounter identity and Vex spam does not bypass the intended wall geometry.

### Rewards
Stage1: Combat XP650 + Construction XP250 + Diamond4 + Amethyst32 + Echo6 + vanilla XP220.

Stage2: Combat XP900 + Construction XP350 + Diamond6 + Echo10 + Dragon Breath4 + Netherite Scrap1 + vanilla XP320.

Nearby surviving allies within48 gain vanilla XP70. No permanent generic combat stat is awarded.

## 0.38 Defendable Physical Outposts retained
Normal three-wave defense remains supply1 / 4 minutes. Siege mobs advance toward the actual anchor rather than following a distant kiting player. Owner death, invalid mode/dimension, >64 distance, broken real outpost, breach pressure200 or timeout fails without refund.

## 0.37 Physical Warehouse Clusters retained
`field_depots_v1` keeps optional `warehouse_links`. Each anchor may link max8 real Barrels inside6, no link supply charge. Loaded interactable real Containers join the nearest-first resolver; no virtual capacity or force-load.

## 0.36 Physical Commissioning Sites retained
Industrial Works / Apex Tracking Post / Ascension Nexus finalizable funding requires a real bounded commissioning site. Validation occurs before final-call material consumption; already-completed worlds remain accepted.

## 0.35 / 0.34 logistics retained
High-volume offload scans only main inventory9..35 and preserves hotbar/equipment. Shared sink resolution remains inventory-first then nearest usable physical Barrel. Apex/Trial admission stays player-carried.

## 0.33 / 0.32 expedition operations retained
One active operation/player, launched at active regional outpost; cross authored range, complete two validated actions, return to exact origin. New operations receive DEEP_FRONT / FORWARD_SHIFT / HOT_EXTRACTION. Death/dimension/game-mode/deadline fails without refund.

## 0.31 Field Recovery retained
One prepaid ordinary-death token at active outpost, same-dimension96. Incident/Apex/Trial/Outpost/Bastion defense deaths do not consume it. Safe return validates actual loaded outpost and standing space; no ordinary fast travel or force-load.

## 0.30 Physical Outposts retained
Owned registered Barrel + Bed + Campfire + Crafting Table + Furnace-family within5. Upgrade cost supply2 + Iron32 + Gold8 + Coal32. Active owner-nearby outpost extends logistics to64 and suppresses NATURAL hostiles within24 only; TRIGGERED encounter mobs remain valid.

## Production retained
METALWORKS / TIMBERWORKS / PROVISIONS / PRECISION remain. A complete four-line cycle grants supply1; buffers/supply cap3. Physical dispatch remains Gold32 + Amethyst16 + Echo2 to the player.

## Mastery / Field Mastery retained
Lv100 base: Mining11×11+vein192, Wood384, Harvest11×11, Construction49/11×11, combat6.5/16, air dash3.
After all nine regions: Quarry7×7×12, Wood448, Harvest13×13, Construction65/13×13, combat7.5/20, air dash4.

## External-source policy
0.39 adds no new external implementation or assets. Existing reference-only and packaged-license boundaries remain in `THIRD_PARTY_NOTICES.md`.
