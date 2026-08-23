# Changelog

## 0.40.0-alpha.1
- Added `Physical Siege Breachers / 물리 공성 파괴자` so the unique fourth Bastion wave can physically answer the player-built wall instead of only increasing enemy statistics.
- Added `OutpostSiegeBreachService` and registered it on the server tick bus.
- Breaching is Bastion-only by reusing the existing siege owner/wave NBT and requiring wave4; normal three-wave Outpost Defense never enters the block-breaking path.
- Only Ravagers and Vindicators may breach: Ravager successful-break cooldown30 ticks, Vindicator60 ticks.
- Breakers search only a tiny local area around themselves and only target the same qualifying fortification materials: vanilla `WALLS`, Iron Bars and Nether Brick Fence inside the outpost annulus radius6..12.
- Candidate blocks must lie generally forward toward the outpost anchor, preventing arbitrary scenery behind the attacker from being selected.
- Added full protection chain: active siege owner, same-dimension operational owned outpost, loaded position, `EventHooks.canEntityGrief`, owner `mayInteract`, `state.canEntityDestroy`, `EventHooks.onEntityDestroyBlock`, and no block entity.
- Fortification destruction uses normal item drops, letting the defender recover material and rebuild with the existing Construction system.
- Added no arbitrary terrain destruction, storage/anchor destruction, new packet, new SavedData ID, client coordinate trust, custom block/item, `getChunk`, region ticket or force-load.
- Updated Guide/README/PROJECT/source audit/JAR verification while retaining 0.39 Bastion qualification/rewards and every older logistics/expedition/endgame boundary.

## 0.39.0-alpha.1
- Added `Physical Bastion Defense / 물리 요새 방어` so large player-built fortifications around an existing outpost become optional combat infrastructure instead of a passive number bonus.
- Added `OutpostFortificationService` with a loaded-only annulus scan at radius6..12 and vertical allowance Y-3..+4.
- Fortification accepts vanilla `WALLS` tag blocks, Iron Bars and Nether Brick Fence.
- Split the annulus into NE/NW/SE/SW and require at least12 fortified x/z columns in each quadrant; stacked blocks in one x/z coordinate count only once, preventing tall-pillar count inflation.
- Added no fortified SavedData flag: the currently loaded real blocks are the source of truth. Removing the wall makes subsequent validation fail.
- Added `ProductionService.ACTION_BASTION_SIEGE = "bastion_siege"` and a Stone Brick Wall entry to the existing Industrial Works radial; no packet schema or protocol bump.
- Bastion start requires an active outpost within4, a valid four-quadrant fortification ring and two supply charges. The first wave must spawn before the two charges are consumed.
- Extended `OutpostSiegeSystem` to retain normal OUTPOST mode at3 waves/4800 ticks/supply1 and add BASTION mode at4 waves/6000 ticks/supply2.
- Bastion mode revalidates the physical fortification ring between waves; normal outpost structure validation, owner64 radius, breach radius6/limit200 and anchor-directed attackers remain shared.
- Added harder Stage1/Stage2 bastion compositions through more simultaneous ranged/melee/disruption roles and multiple Ravagers rather than adding bastion-only health, attack, armor or damage-reduction multipliers.
- Added Stage1 bastion rewards: Combat XP650 + Construction XP250 + Diamond4 + Amethyst32 + Echo6 + vanilla XP220.
- Added Stage2 bastion rewards: Combat XP900 + Construction XP350 + Diamond6 + Echo10 + Dragon Breath4 + Netherite Scrap1 + vanilla XP320. Nearby surviving allies inside48 gain XP70.
- Retained the existing Incident/Operation/Apex/Trial mutual-exclusion behavior and Field Recovery death exclusion for both defense modes.
- Added no auto-builder, new SavedData ID, client coordinate trust, background maintenance loop or chunk force-load.
- Updated Guide/README/PROJECT/source audit/JAR verification while retaining 0.38 normal defense,0.37 warehouse clusters,0.36 commissioning,0.35 offload,0.34 integrated inputs,0.33 complications and older safety contracts.

## 0.38.0-alpha.1
- Added `Defendable Physical Outposts / 전초 방어전`: an active owned outpost can host an explicit three-wave defense encounter instead of serving only as a logistics/safety radius.
- Siege start requires the owner inside4 of an active physical outpost and consumes one field-supply charge only after the first wave has been successfully spawned.
- Added a four-minute total deadline, three authored waves and a three-second regroup window between waves.
- Added physical breach pressure: living siege mobs inside6 of the anchor add `breachers * 5` pressure every five server ticks, pressure drains by10 while clear, and200 pressure fails the defense.
- Siege mobs are anchor-directed instead of being freely kiteable; outside the16-block immediate defense area they navigate toward the physical outpost anchor.
- Defense continuously revalidates owner same-dimension/within64 and the real outpost Barrel + Bed/Campfire/Crafting/Furnace structure.
- Authored waves use `EntitySpawnReason.TRIGGERED` in already-loaded radius26~34 terrain. Stage pressure comes from compositions rather than a siege-only blanket health/attack multiplier.
- Added bounded encounter overlap, Field Recovery exclusion, success rewards, logout cleanup and stale tagged-mob rejection without siege SavedData or force-load.

## 0.37.0-alpha.1
- Added `Physical Warehouse Clusters / 물리 창고군`: each depot anchor may link max8 additional real Barrels inside radius6, with optional `warehouse_links` in `field_depots_v1`.
- Unloaded links are preserved; loaded invalid links prune individually. No virtual capacity, automatic routing, cross-dimension access or force-load.

## 0.36.0-alpha.1
- Added bounded real-world commissioning for Industrial Works, Apex Tracking Post and Ascension Nexus before finalizable funding can cross completion.
- Final site validation runs before any project material is consumed in that final call; existing completed projects remain compatible.

## 0.35.0-alpha.1
- Added explicit High-volume Field Offload from main inventory slots9..35 into nearest usable real Barrel stock; hotbar/equipment remain untouched.

## 0.34.0-alpha.1
- Added one shared inventory-first + nearest real-Barrel resolver for industrial batches, unfinished infrastructure and equipment reforge/awakening.
- Apex/Trial entry remained physically player-carried.

## 0.33.0-alpha.1
- Added exactly one bounded sortie complication to each new operation: DEEP_FRONT, FORWARD_SHIFT or HOT_EXTRACTION.

## 0.32.0-alpha.1
- Added nine repeatable physical out-and-back expedition operations staged from active regional outposts.

## 0.31.0-alpha.1
- Added prepaid one-use ordinary-death field recovery at active outposts; no ordinary fast travel.

## 0.30.0-alpha.1
- Added physical field outposts with Bed/Campfire/Crafting/Furnace structure, logistics64 and NATURAL-hostile safety24.

## 0.29.0-alpha.1
- Added real vanilla Barrel field depots, max3/player, one owner per physical position, no force-load.

## 0.28.0-alpha.1
- Added Stage1 Industrial Works, four production lines, bounded buffers and field-supply charges.

## 0.27.0-alpha.1
- Added Stage1 Apex Tracking Post and nine behavior-driven Apex Hunts.

## 0.26.0-alpha.1
- Added18 regional field incidents.

## 0.25.0-alpha.1
- Added two persistent directive options per expedition region.

## 0.24.0-alpha.1
- Reworked expedition discovery into persistent physical field objectives.

## 0.23.0-alpha.1
- Added nine expedition regions and Field Mastery progression.

## 0.22.0-alpha.1
- Added randomized Ascension Trial doctrines and4-affix Awakened Mythic progression.

## 0.21.0-alpha.1
- Added repeatable Stage2 four-wave Ascension Trial.

## 0.20.0-alpha.1
- Added final Lv100 Mastery VI across all six active skills.
