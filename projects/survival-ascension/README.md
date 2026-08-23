# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25. Network protocol `8`.

Survival Ascension makes progression increase the physical scale of player actions, then makes infrastructure, logistics, expeditions and combat consume that larger output again.

## 0.38.0-alpha.1 — Defendable Physical Outposts / 전초 방어전
0.30 made outposts physically exist, but after construction they mainly extended logistics and suppressed NATURAL hostile spawns. 0.38 makes the already-built outpost itself an optional combat objective instead of creating another abstract arena or quest screen.

Use `M -> Infrastructure -> 산업 가공소 -> 전초 방어전` while within4 blocks of an active owned outpost anchor. Starting costs one stored field-supply charge.

### Physical defense rule
- three authored waves;
- total defense deadline 4800 ticks / 4 minutes;
- 3-second regroup window between waves;
- owner must remain in the same dimension and within64 blocks;
- the actual Barrel + Bed + Campfire + Crafting Table + Furnace-family outpost structure must stay operational;
- siege mobs spawn as `TRIGGERED` entities in already-loaded open terrain around radius26~34;
- no chunk ticket or force-load is used.

The fight is not only a kill counter. Siege mobs advance toward the physical anchor. When the owner fights inside16 blocks of the anchor they engage the owner; otherwise they continue toward the base instead of following a kiting player away.

### Breach pressure
Any living siege mob inside radius6 of the anchor counts as a breacher. Every five server ticks, breach pressure rises by `breachers * 5`; when no breacher is present it falls by10. Pressure reaching200 immediately fails the defense.

This makes walls, line-of-sight, positioning, large combat cleave and mobility around the actual outpost valuable without adding a permanent flat combat stat.

### Stage compositions
Stage1 uses mixed ranged / fast / melee / heavy roles across skeletons, spiders, zombies, pillagers, vindicators, witches and a final ravager. Stage2 raises composition pressure with more role overlap, two ravagers in the last wave and endermen, rather than applying a blanket HP multiplier.

### Encounter boundaries
- cannot start during Regional Incident, Expedition Operation, Apex Hunt or Ascension Trial;
- starting a siege reserves the existing Incident/Apex/Trial ready windows through the encounter timeout;
- a new Expedition Operation is rejected while siege is active;
- manual Apex/Trial starts are rejected while siege is active;
- siege death does not consume a prepaid Field Recovery contract;
- logout cleans active siege entities/boss bar; stale tagged siege entities are rejected if loaded without a matching runtime siege.

### Rewards
Stage1 completion: Combat mastery XP350 + Diamond2 + Amethyst24 + Echo3 + vanilla XP120.
Stage2 completion: Combat mastery XP500 + Diamond4 + Echo6 + Dragon Breath2 + vanilla XP200.
Nearby surviving allies inside48 receive vanilla XP40. No new permanent stat multiplier is granted.

0.38 adds no SavedData ID, packet, virtual structure, client coordinate trust, automatic maintenance timer or chunk force-load.

## 0.37.0-alpha.1 — Physical Warehouse Clusters / 물리 창고군
Each registered depot Barrel remains an anchor and may explicitly link up to8 additional real Barrels inside radius6. `field_depots_v1` is retained with optional `warehouse_links`, so older worlds load with zero links.

All shared physical logistics consumers and 0.35 offload use usable anchor+satellite real Containers nearest-first. Unloaded satellites are skipped and preserved; loaded missing/non-Barrel satellites prune individually. Satellite linking costs no supply charge and adds no virtual capacity.

## 0.36.0-alpha.1 — Physical Commissioning Sites / 물리 준공 현장
Industrial Works, Apex Tracking Post and Ascension Nexus require a real bounded commissioning site before a finalizable funding call can cross completion. Validation occurs before final-call material consumption and old completed worlds remain compatible.

## Retained logistics / field loop
- 0.34 Integrated Logistics Backbone: stationary sinks consume inventory first, then nearest usable real logistics Barrels.
- 0.35 High-volume Field Offload: explicit main inventory slots9..35 -> nearest physical Barrel capacity; hotbar/equipment remain carried.
- 0.30 Physical Outposts: owned depot + Bed/Campfire/Crafting/Furnace; logistics64 and NATURAL-hostile safety24 while active.
- 0.31 Field Recovery: prepaid one-use ordinary-death return within96; encounter deaths stay excluded.
- 0.32/0.33 Expedition Operations: physical out-and-back regional sorties with one bounded complication per new sortie.

## Production
Industrial Works retains METALWORKS / TIMBERWORKS / PROVISIONS / PRECISION. One completed four-line set grants supply1; buffers and supply charges remain capped at3. Dispatch remains player-carried Gold32 + Amethyst16 + Echo2.

## Combat / endgame boundaries
Apex entry remains player-carried Echo8 + Amethyst32 + Gold32. Ascension Trial entry remains player-carried Echo32 + Amethyst64 + Dragon Breath8. Physical logistics never become remote encounter payment.

## Final action scale
After Lv100 + all nine expedition regions:
- Quarry tunnel 7×7×12
- Woodcutting 448 logs
- Harvest 13×13
- Combat shockwave 7.5 radius / 20 targets
- Construction line65 / plane13×13
- Mobility air dash4

Large work remains tick-budgeted and uses normal protection/material paths. Shift remains the precision override.

## External references
0.38 adds no new third-party source, data, UI or assets. Existing notices and reference-only boundaries remain in `THIRD_PARTY_NOTICES.md`.