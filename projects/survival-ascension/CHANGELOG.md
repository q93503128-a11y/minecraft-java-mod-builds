# Changelog

## 0.38.0-alpha.1
- Added `Defendable Physical Outposts / 전초 방어전`: an active owned outpost can now host an explicit three-wave defense encounter instead of serving only as a logistics/safety radius.
- Added `ProductionService.ACTION_OUTPOST_SIEGE = "outpost_siege"` and a Shield entry in the existing Industrial Works radial; no packet schema or protocol bump.
- Siege start requires the owner inside4 of an active physical outpost and consumes one field-supply charge only after the first wave has been successfully spawned.
- Added a four-minute total deadline, three authored waves and a three-second regroup window between waves.
- Added physical breach pressure: living siege mobs inside6 of the anchor add `breachers * 5` pressure every five server ticks, pressure drains by10 while clear, and200 pressure fails the defense.
- Siege mobs are anchor-directed instead of being freely kiteable: outside the16-block immediate defense area they clear the distant player target and navigate toward the physical outpost anchor.
- Defense continuously revalidates owner same-dimension/within64 and the real outpost Barrel + Bed/Campfire/Crafting/Furnace structure through `OutpostService.isRecoveryOperational`; invalid state for200 ticks fails.
- Authored waves use `EntitySpawnReason.TRIGGERED` in already-loaded radius26~34 open terrain, so NATURAL-only outpost spawn suppression does not cancel them.
- Stage1/2 difficulty is raised through mixed skeleton/spider/zombie/pillager/vindicator/witch/ravager/enderman compositions rather than a siege-only blanket health/attack multiplier.
- Added bounded overlap rules: a siege cannot start during Incident/Operation/Apex/Trial; starting one reserves existing Incident/Apex/Trial ready windows; new Operation and manual Apex/Trial starts reject an active siege.
- Siege deaths now explicitly do not consume a prepaid Field Recovery contract.
- Success rewards Stage1 Combat XP350 + Diamond2 + Amethyst24 + Echo3 + vanilla XP120, Stage2 Combat XP500 + Diamond4 + Echo6 + Dragon Breath2 + vanilla XP200; nearby allies inside48 gain XP40.
- Added runtime cleanup on logout and stale tagged-mob rejection without adding siege SavedData, virtual structures, client coordinate trust or chunk force-loading.
- Updated Guide/README/PROJECT/source audit/JAR verification while retaining 0.37 warehouse clusters,0.36 commissioning,0.35 offload,0.34 integrated inputs,0.33 complications and older safety contracts.

## 0.37.0-alpha.1
- Added `Physical Warehouse Clusters / 물리 창고군` so high-throughput storage scales by building more real Barrels instead of introducing a virtual warehouse.
- Extended existing `field_depots_v1` with optional `warehouse_links`; old saves decode with no links and retain all existing depot/outpost state.
- Added max8 linked Barrels/anchor inside radius6, explicit no-supply-charge link/unlink, global physical-position ownership and loaded-only nearest-first real-Container resolution.
- Unloaded links are preserved; loaded invalid linked positions prune individually. No virtual capacity, automatic routing, cross-dimension access or force-load.

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