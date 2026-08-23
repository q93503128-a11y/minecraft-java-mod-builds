# Changelog

## 0.37.0-alpha.1
- Added `Physical Warehouse Clusters / 물리 창고군` so high-throughput storage scales by building more real Barrels instead of introducing a virtual warehouse.
- Extended existing `field_depots_v1` with optional `warehouse_links`; old saves decode with no links and retain all existing depot/outpost state.
- Added `MAX_LINKED_BARRELS_PER_DEPOT = 8` and `MAX_LINK_RADIUS = 6` plus load-time sanitation for missing anchors, over-range links, per-anchor caps and globally double-claimed physical positions.
- Added `ProductionService.ACTION_WAREHOUSE_TOGGLE = "toggle_warehouse_barrel"` and a `창고 배럴 연결` entry to the existing Industrial Works radial.
- Linking resolves the target real Barrel within4 server-side, requires `mayInteract`, rejects registered anchors and other players' links, then resolves an owned loaded/interactable anchor within6. Re-selecting the player's linked Barrel unlinks it.
- Satellite Barrel linking costs no supply charge. Crafted physical Barrel capacity is the cost; unlinking never deletes the Barrel or its contents.
- Removing or pruning a depot anchor also removes only that anchor's stored warehouse link records; Outpost removal behavior remains unchanged.
- Expanded the shared logistics resolver so a usable depot contributes its anchor plus currently usable linked real Barrel Containers.
- Linked Barrel chunks are never force-loaded. An unloaded link is skipped and preserved; a loaded missing/non-Barrel/non-Container link is pruned individually.
- Resolved anchor + satellite Containers are sorted nearest-first by physical Barrel position before input consumption or offload insertion.
- Preserved inventory-first sink ordering and 0.35 merge-before-empty / component-equality / `canPlaceItem` / finite-capacity / source-remainder behavior.
- Split status meaning so `activeDepotCount` remains usable anchor count and `activeStorageBarrelCount` reports actual usable anchor+satellite Containers.
- Added no new packet, protocol bump, automatic pickup routing, cross-dimension access, global inventory, background scan or chunk force-load.
- Updated Guide/README/PROJECT/source audit to lock migration, ownership, link limits, loaded-only behavior, nearest-first real-stock use and all 0.36/older regressions.

## 0.36.0-alpha.1
- Added `InfrastructureSiteService` and bounded real-world commissioning for Industrial Works, Apex Tracking Post and Ascension Nexus before finalizable funding can cross completion.
- Final site validation runs before any project material is consumed in that final call; existing completed projects remain compatible.
- Industrial profile: Barrel + Stone Bricks48 + Iron Blocks4 + Blast Furnaces2 + Stonecutter1 + Hoppers2 in radius6.
- Apex profile: owned registered Barrel + Stone Bricks32 + Gold Blocks4 + Lodestone1 + Cartography Table1 + Targets4.
- Nexus profile: owned registered Barrel + Obsidian32 + Crying Obsidian8 + Beacon1 + Enchanting Table1 + Ender Chest1.
- No new SavedData, packet, background maintenance scan or chunk force-load.

## 0.35.0-alpha.1
- Added explicit High-volume Field Offload from main inventory slots9..35 into nearest usable real Barrel stock.
- Hotbar0..8, equipment and offhand remain untouched.
- Matching stacks fill before empty slots; item components, `canPlaceItem`, real capacity and unaccepted remainders are preserved.
- No item-pickup hook or automatic routing.

## 0.34.0-alpha.1
- Added one shared inventory-first + nearest real-Barrel resolver for industrial batches, unfinished infrastructure and equipment reforge/awakening.
- Apex/Trial entry remained physically player-carried.

## 0.33.0-alpha.1
- Added exactly one bounded sortie complication to each new operation: DEEP_FRONT, FORWARD_SHIFT or HOT_EXTRACTION.
- Existing0.32 active operations may migrate as NONE.

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
