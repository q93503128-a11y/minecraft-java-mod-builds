# Survival Ascension

- Mod version: `0.40.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Network protocol: `8`
- Existing-world compatibility: all existing SavedData IDs remain unchanged. 0.40 adds no migration and no new persistent structure state; breacher cooldowns live only on authored siege-mob persistent NBT while the active encounter exists.

## Core direction
Progression enlarges physical player actions rather than mainly inflating percentages. Bigger actions create larger throughput; infrastructure, real storage, bases, expeditions and behavior-driven enemies must consume it again. Shift remains the precision/single-action safety override.

## 0.40 Physical Siege Breachers / 물리 공성 파괴자
### Purpose
0.39 made player-built fortification a real pathing object, but a sufficiently thick wall could still turn the hardest bastion wave into passive pathfinding abuse. 0.40 gives only the unique final Bastion wave a tightly bounded infrastructure response: heavy/sapper attackers can physically open the qualifying wall itself.

This is not a global mob-griefing overhaul and does not change ordinary hostile mobs or the retained three-wave Outpost Defense.

### Eligibility
`OutpostSiegeBreachService` runs only while `OutpostSiegeSystem.isActive(owner)` and only considers mobs whose existing siege NBT says:
- matching `survivalascension_outpost_siege_owner`;
- `survivalascension_outpost_siege_wave >= 4`.

Because normal Outpost Defense has only three waves, the rule is Bastion-only without adding another packet or SavedData flag.

Breaker roles:
- `EntityType.RAVAGER`: break cooldown `30` ticks;
- `EntityType.VINDICATOR`: break cooldown `60` ticks.

Other siege mobs remain ordinary combat/pathing roles.

### Physical target boundary
A breaker searches only a `2`-block horizontal / Y-1..+2 local neighborhood around itself.

A candidate must:
- be inside the same physical fortification annulus radius6..12 around an owned operational outpost;
- be a vanilla `WALLS` tag block, Iron Bars or Nether Brick Fence;
- contain no block entity;
- lie generally forward from the mob toward the outpost anchor.

The service does not target the Barrel anchor, Bed, Campfire, Crafting Table, Furnace-family camp, storage barrels, terrain, doors, chests or arbitrary player builds.

### Protection chain
Before `destroyBlock(..., true, mob)` the server requires:
1. same-dimension owned outpost and owner within64;
2. `OutpostService.isRecoveryOperational` for that outpost;
3. already-loaded candidate position only;
4. `EventHooks.canEntityGrief(level, mob)`;
5. `level.mayInteract(owner, pos)`;
6. `state.canEntityDestroy(level, pos, mob)`;
7. `EventHooks.onEntityDestroyBlock(mob, pos, state)` not canceled.

The destroyed fortification drops normally. This deliberately allows immediate manual or Construction-mastery repair instead of silently deleting invested materials.

### Performance / lifecycle
- service ticks once per5 server ticks;
- scans only players with an active defense runtime;
- only nearby mobs within96 of that owner are considered;
- only Ravager/Vindicator with matching wave4 siege tags enter target search;
- each owner has max3 outposts and target search is a tiny local cube;
- no chunk load, chunk ticket or background world scan.

### Gameplay effect
The final Bastion wave now has an infrastructure-vs-counter-infrastructure loop:
- walls initially obstruct the anchor-directed wave;
- Ravagers/Vindicators that reach the wall can open holes;
- holes expose the radius6 breach-pressure objective;
- dropped blocks and existing large-scale Construction let the defender physically respond.

No breacher-only health, damage, armor, player debuff or permanent stat reward is added.

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
`OutpostSiegeSystem` has two modes:
- OUTPOST: retained 3 waves / 4800 ticks / supply1;
- BASTION: 4 waves / 6000 ticks / supply2.

Both retain owner64, real-outpost validation, anchor-directed attackers, engage16, breach6/200, loaded TRIGGERED spawns, common overlap exclusion and Field Recovery death exclusion.

Bastion mode revalidates all four physical fortification quadrants between waves. The wall's ordinary collision/pathfinding obstruction is its real combat value; there is no bastion-only HP, attack, armor or damage-reduction modifier.

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
0.40 keeps the existing permissive-code/reference-only split. The new breacher implementation is independent Survival Ascension code; no other mod's AI goal, block-breaking source, assets or configuration are bundled.
