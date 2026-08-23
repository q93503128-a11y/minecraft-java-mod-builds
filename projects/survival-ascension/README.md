# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25. Network protocol `8`.

Survival Ascension makes progression increase the physical scale of player actions, then makes infrastructure, logistics, expeditions and combat consume that larger output again.

## 0.41.0-alpha.1 — Civil Works Causeways / 토목 공사소·도로 교량 시공
0.41 moves back from combat escalation to the core loop: large gathering and physical storage now feed a large engineering sink that leaves useful infrastructure in the world.

### Civil Works project
New Stage1 infrastructure project `CIVIL_WORKS / 토목 공사소`:
- Stone Bricks `2048`
- Cobblestone `1536`
- Gravel `1536`
- Iron Ingots `256`
- Copper Ingots `256`

Funding uses the retained 0.34 resolver: player inventory first, then nearest usable physical logistics Barrels. No virtual warehouse or remote universal payment is added.

The finalizable funding call also reuses the 0.36 physical commissioning contract. Within4 of the player there must be an owned registered Barrel, and inside radius6 of that anchor the loaded world must contain:
- Stone Bricks48
- Scaffolding16
- Iron Blocks4
- Stonecutters2
- Crafting Table1

The site is checked before the final material call consumes anything. As with existing commissioning sites, this is one-time proof of a real yard; dismantling it later does not disable the completed project.

### Road / bridge construction mode
After Civil Works is complete, Construction Lv60 unlocks `도로/교량` in the existing Construction radial.

Place the first ordinary BlockItem while the mode is selected. The same block is then queued forward in the player's horizontal look direction as a flat three-wide deck:
- Lv60: `3 × 17`
- Lv90: `3 × 33`
- Lv100: `3 × 49`
- Lv100 + all nine expedition regions / Field Mastery: `3 × 65`

The mode is deliberately forward-only instead of centering around the first block, so repeated placements naturally extend roads, raised causeways and bridges across real terrain. It does not auto-level terrain, delete obstacles, create supports from nowhere or choose another block palette.

### Existing Construction engine only
0.41 does not add a second builder engine. Causeway targets enter the same `ConstructionProgression` queue and retain:
- global budget64 placed attempts/tick;
- player-local budget8/tick;
- max512 queued blocks/player;
- player inventory + usable real physical logistics material consumption;
- `level.mayInteract` protection;
- NeoForge `EventHooks.onBlockPlace`;
- block survival/replacement checks;
- Shift precision override.

0.41 also makes bulk placement explicitly loaded-only with `level.hasChunkAt(target)` before block-state/protection work. Unloaded road segments are skipped; no `getChunk`, ticket or force-load is used.

### External reference boundary
The causeway work extends the already-noticed Building Gadgets 2 MIT design/code-study boundary: material-backed bulk placement, protection checks and tick-distributed work. Survival Ascension's Civil Works funding, physical commissioning yard, three-wide forward geometry and field-logistics integration are its own code. No Building Gadgets assets, item models, GUI assets, templates or namespaces are bundled.

## 0.40.0-alpha.1 — Physical Siege Breachers / 물리 공성 파괴자
0.39 made a player-built wall matter through collision and pathing. 0.40 lets the unique final bastion wave answer that infrastructure physically instead of merely receiving more health or damage.

Only siege mobs tagged as wave4 are eligible, so normal three-wave Outpost Defense does not gain block destruction. Ravagers may break one eligible fortification at most every30 ticks and Vindicators every60 ticks. They target only vanilla `WALLS`, Iron Bars or Nether Brick Fence inside the physical fortification annulus radius6..12 and generally forward toward the anchor.

Every break still requires loaded terrain, `EventHooks.canEntityGrief`, owner `mayInteract`, block entity-destroy permission and `EventHooks.onEntityDestroyBlock`. Destroyed fortification drops normally for physical repair. No arbitrary terrain/storage/anchor destruction or force-load is added.

## 0.39.0-alpha.1 — Physical Bastion Defense / 물리 요새 방어
A distributed wall ring around an active outpost admits the optional four-wave Bastion defense. The server scans radius6..12, Y-3..+4 and requires at least12 fortified x/z columns in each NE/NW/SE/SW quadrant. Valid materials are `WALLS`, Iron Bars and Nether Brick Fence. Same-column vertical stacking counts once.

Bastion costs supply2, lasts6000 ticks, revalidates the physical ring between waves and retains breach radius6/limit200. Normal Outpost Defense remains supply1/3 waves/4800 ticks. Difficulty is composition-driven rather than a bastion-only blanket HP/attack multiplier.

## 0.38.0-alpha.1 — Defendable Physical Outposts / 전초 방어전
Active real outposts can host a three-wave defense. Siege mobs advance toward the real anchor; enemies inside6 generate breach pressure and200 fails the defense. The owner must remain within64 and keep the actual outpost structure operational.

## 0.37.0-alpha.1 — Physical Warehouse Clusters / 물리 창고군
Each registered Barrel remains an anchor and may explicitly link up to8 additional real Barrels inside radius6. `field_depots_v1` keeps optional `warehouse_links`; unloaded satellites are skipped/preserved and loaded-invalid links prune individually.

## 0.36.0-alpha.1 — Physical Commissioning Sites / 물리 준공 현장
Industrial Works, Apex Tracking Post and Ascension Nexus require a real bounded commissioning site before a finalizable funding call can cross completion. 0.41 extends this existing engine to Civil Works. Existing completed projects remain compatible.

## Retained field loop
- 0.34 Integrated Logistics Backbone: stationary sinks consume inventory first, then nearest usable real logistics Barrels.
- 0.35 High-volume Field Offload: explicit main inventory slots9..35 -> nearest physical Barrel capacity; hotbar/equipment remain carried.
- 0.30 Physical Outposts: owned depot + Bed/Campfire/Crafting/Furnace; logistics64 and NATURAL-hostile safety24 while active.
- 0.31 Field Recovery: prepaid one-use ordinary-death return within96; authored encounter deaths stay excluded.
- 0.32/0.33 Expedition Operations: physical out-and-back regional sorties with one bounded complication per new sortie.

## Combat / endgame boundaries
Apex entry remains player-carried Echo8 + Amethyst32 + Gold32. Ascension Trial entry remains player-carried Echo32 + Amethyst64 + Dragon Breath8. Physical logistics never become remote encounter payment.

## Final action scale
After Lv100 + all nine expedition regions:
- Quarry tunnel 7×7×12
- Woodcutting 448 logs
- Harvest 13×13
- Combat shockwave 7.5 radius / 20 targets
- Construction line65 / plane13×13 / road-bridge deck3×65
- Mobility air dash4

Large work remains tick-budgeted and uses normal protection/material paths. Shift remains the precision override.

## External references
Permissive-code and reference-only boundaries are documented in `THIRD_PARTY_NOTICES.md`. 0.41 adds no third-party assets or dependency and keeps Building Gadgets 2 under its existing MIT notice.
