# Survival Ascension

- Mod version: `0.38.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Network protocol: `8`
- Existing-world compatibility: `mining_progress_v1`, `infrastructure_v1`, `world_ascension_v1`, `expedition_v1`, `apex_hunt_v1`, `production_v1`, `field_depots_v1`, `outpost_v1`, `field_recovery_v1`, `expedition_operations_v1`, Elite/Warband/mutation persistent NBT, affix CustomData and mining modes remain intact. 0.38 adds no SavedData field/ID and does not migrate any existing structure or item state.

## Core direction
Progression enlarges physical player actions rather than mainly inflating percentages. Bigger actions create larger throughput; infrastructure, real storage, bases, expeditions and behavior-driven enemies must consume it again. Shift remains the precision/single-action safety override.

## 0.38 Defendable Physical Outposts / 전초 방어전
### Purpose
0.30 made a real outpost and 0.37 let that outpost host a large physical warehouse cluster. The remaining weakness was that once built, the base itself rarely mattered during combat. 0.38 makes an existing active outpost an optional defendable world objective instead of introducing a separate arena, claim block or quest GUI.

### Explicit start
`ProductionService.ACTION_OUTPOST_SIEGE = "outpost_siege"` reuses the existing Industrial Works action payload.

`M -> Infrastructure -> 산업 가공소 -> 전초 방어전` requires:
- survival/non-spectator;
- completed Industrial Works;
- active owned outpost anchor within4;
- no active Regional Incident / Expedition Operation / Apex Hunt / Ascension Trial;
- one stored field-supply charge.

The first wave is spawned successfully before the supply charge is consumed. If loaded open spawn terrain is insufficient, the action aborts with no charge loss.

### Physical objective
`OutpostSiegeSystem` defines:
- `START_RADIUS = 4`
- `DEFENSE_RADIUS = 64`
- `BREACH_RADIUS = 6`
- `BREACH_LIMIT = 200`
- `SUPPLY_CHARGE_COST = 1`
- `TOTAL_WAVES = 3`
- `SIEGE_TIMEOUT_TICKS = 4800`
- `OWNER_GRACE_TICKS = 200`
- `WAVE_DELAY_TICKS = 60`
- `ENGAGE_RADIUS = 16`

A valid defense continuously requires the owner alive, same dimension, non-creative/non-spectator, inside64, and the exact outpost to keep passing `OutpostService.isRecoveryOperational`: loaded/interactable real anchor Barrel plus the physical Bed/Campfire/Crafting/Furnace structure.

### Anchor-directed attackers
Siege mobs do not simply chase a kiting player around the64-block radius. Outside the immediate16-block defense area they clear their player target and navigate toward the anchor. When the owner is actually defending inside16 and a mob is close enough, it may engage the owner directly.

This preserves the physical-base objective: leaving the anchor undefended does not drag the wave away from what it is supposed to attack.

### Breach pressure
Every five server ticks:
- living siege mobs inside radius6 count as breachers;
- pressure += `breachers * 5`;
- if no breacher exists, pressure -=10 down to0;
- pressure >=200 fails the defense immediately.

The boss bar shows wave, living siege mob count, breach pressure and total remaining time. Defenses therefore value terrain, walls, positioning, cleave and mobility rather than only total damage dealt.

### Waves / scaling
Stage1:
1. skeleton4 + spider4
2. zombie3 + pillager3 + vindicator3
3. ravager1 + pillager3 + vindicator3 + witch1 + skeleton2

Stage2:
1. skeleton4 + spider3 + zombie2 + enderman1
2. pillager4 + vindicator4 + witch2 + spider2
3. ravager2 + pillager4 + vindicator4 + witch2 + enderman1

0.38 intentionally adds no siege-only MAX_HEALTH/ATTACK_DAMAGE blanket modifier. Higher stage pressure comes from roles, ranged/melee overlap, disruptive enemies and heavier composition.

### Spawn / lifecycle safety
- siege mobs use `EntitySpawnReason.TRIGGERED`, so the outpost's NATURAL-only safe-zone cancellation cannot suppress authored waves;
- spawn search uses already-loaded positions via `level.hasChunkAt` only;
- no `getChunk`, region ticket or force-load;
- spawn ring is radius26~34;
- mobs are persistent-tagged with owner/wave;
- logout removes the runtime siege and its tracked mobs;
- a tagged mob joining without a matching active siege is canceled;
- no new SavedData is introduced.

### Encounter overlap
Starting a siege writes forward exclusions to the existing persistent ready keys for Incident/Apex/Trial through the siege timeout+200 ticks. Manual Expedition Operation starts are rejected by `ProductionService` while siege is active; completed Apex/Nexus actions also reject manual Apex/Trial starts during a siege.

Field Recovery explicitly excludes siege deaths, so a prepaid ordinary-death recovery token is not consumed by this challenge.

### Failure / rewards
Failure:
- owner death;
- invalid game mode / dimension / over64 / broken physical outpost for200 ticks;
- breach pressure200;
- total4-minute deadline.
The spent supply charge is not refunded.

Success Stage1: Combat XP350 + Diamond2 + Amethyst24 + Echo3 + vanilla XP120.
Success Stage2: Combat XP500 + Diamond4 + Echo6 + Dragon Breath2 + vanilla XP200.
Nearby surviving allies inside48 gain vanilla XP40. No flat permanent stat reward.

## 0.37 Physical Warehouse Clusters
`field_depots_v1` keeps optional `warehouse_links`. Each anchor may link max8 real Barrels inside radius6, no link supply charge. Same-dimension loaded interactable real Containers join the nearest-first logistics resolver; unloaded links remain saved and loaded-invalid links prune individually. No virtual capacity or force-load.

## 0.36 Physical Commissioning Sites
Industrial Works / Apex Tracking Post / Ascension Nexus finalizable funding requires a real bounded commissioning site. Validation occurs before final-call material consumption; already-completed worlds remain accepted.

## 0.35 / 0.34 logistics
High-volume offload scans only main inventory9..35 and preserves hotbar/equipment. Shared sink resolution remains inventory-first then nearest usable physical Barrel. Apex/Trial admission remains carried inventory-only.

## 0.33 / 0.32 expedition operations
One active operation/player, launched at active regional outpost; cross authored range, complete two validated actions, return to exact origin. New operations receive DEEP_FRONT / FORWARD_SHIFT / HOT_EXTRACTION. Death/dimension/game-mode/deadline fails without refund.

## 0.31 Field Recovery
One prepaid ordinary-death token at active outpost, same-dimension96. Incident/Apex/Trial/Siege deaths do not consume it. Safe return validates actual loaded outpost and standing space; no normal fast travel or force-load.

## 0.30 Physical Outposts
Owned registered Barrel + Bed + Campfire + Crafting Table + Furnace-family within5. Upgrade cost supply2 + Iron32 + Gold8 + Coal32. Active owner-nearby outpost extends logistics to64 and suppresses NATURAL hostiles within24 only; TRIGGERED encounter mobs remain valid.

## Production retained
METALWORKS / TIMBERWORKS / PROVISIONS / PRECISION remain. A complete four-line cycle grants supply1; buffers/supply cap3. Physical dispatch remains Gold32 + Amethyst16 + Echo2 to the player.

## Apex / Trial retained
- Apex: carried Echo8 + Amethyst32 + Gold32, nine region behavior patterns,90s.
- Trial: carried Echo32 + Amethyst64 + Dragon Breath8, four waves, Evoker excluded.
- Field challenges are mutually bounded rather than stacked simultaneously.

## Mastery / Field Mastery retained
Lv100 base: Mining11×11+vein192, Wood384, Harvest11×11, Construction49/11×11, combat6.5/16, air dash3.
After all nine regions: Quarry7×7×12, Wood448, Harvest13×13, Construction65/13×13, combat7.5/20, air dash4.

## External-source policy
0.38 adds no new external implementation or assets. Existing reference-only and packaged-license boundaries remain in `THIRD_PARTY_NOTICES.md`.