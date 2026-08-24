# Changelog

## 0.42.1-alpha.1
- Reworked the in-game guide into a scrollable gameplay reference: body scissor clipping, mouse-wheel scrolling and a visible scrollbar replace the old hard bottom cutoff.
- Removed patch-note wording such as `0.42부터` from `GuideScreen`; release history remains in this file instead.
- Added explicit World Ascension guidance: world start Stage0 `각성`, first Wither kill Stage1 `전설`, first Ender Dragon kill Stage2 `종말`, plus current consequences/unlocks.
- Accelerated early mastery progression with a smooth discount that fades out by Lv60. Approximate cumulative XP: Lv10 1190->453, Lv30 17520->11610, Lv60 121890->107010, Lv90 394110->379230, Lv100 536150->521270.
- Existing saved total XP is preserved; because level is derived from total XP, old characters may resolve slightly upward under the new thresholds.
- Replaced flat `valuable_ores = 20 XP` mining valuation with material tiers while keeping the same tag for vein/extract eligibility.
- Key mining values: Copper7/8, Iron9/10, Gold12/13, Diamond18/20, Emerald20/22, Ancient Debris24, Obsidian16, Crying Obsidian18. This removes the old copper20 > obsidian6 inversion.
- Clarified player-facing `배럴` terminology as Minecraft's normal `통 (Barrel)` block in the guide, production radial and field-logistics messages.
- Reversed shared logistics-backed material consumption order: nearest usable physical logistics `통` Containers first, then player inventory only for the shortfall.
- The storage-first order automatically affects existing systems already using `FieldDepotService.consume*` such as Construction, Infrastructure and replant/material sinks; no virtual storage or new resolver is added.
- Vanilla crafting-table grids remain vanilla and do not remotely pull storage. Apex/Ascension Trial entry also remains carried-only.
- No new SavedData, packet/protocol, custom block/item/entity, force-load or third-party asset/dependency.

## 0.42.0-alpha.1
- Added `Physical Freight Relay / 물리 화물 중계` so actual logistics stock can move between physical outposts instead of roads/rails remaining decorative infrastructure.
- Added `FreightService` using vanilla Chest Minecarts, rails, real Barrel Containers and existing `FieldDepotData`/warehouse links; no new custom entity/block/item or dependency.
- Added `ProductionService.ACTION_FREIGHT = "physical_freight"` and `물리 화물 수레` to the existing Industrial Works radial; existing network protocol8 and InfrastructureActionPayload are reused.
- Freight requires both completed Industrial Works and Civil Works, an active owned outpost within4, a Chest Minecart within4 and already-loaded rail at/below the cart.
- Departure loading requires a completely empty cart and moves only the existing `FieldDepotService.isBulkMaterial` whitelist from that exact outpost's registered Barrel anchor + its linked warehouse Barrels.
- Loaded freight stores only owner UUID and origin outpost dimension/x/y/z on the Chest Minecart's persistent NBT; no global route/freight SavedData is added.
- The same physical cart must arrive at a different active owned outpost in the same dimension before unloading is allowed.
- Destination unloading inserts only into that destination's actual anchor + linked warehouse Barrels; partial unload is allowed and remainder stays in the cart.
- Freight insertion keeps component-equal merge-first behavior, `Container.canPlaceItem`, real stack/container limits and empty-slot fallback; source stacks shrink only by actually accepted quantities.
- Freight has no generated reward and no supply-charge cost. There is therefore no short-route reward exploit; the useful result is only physical relocation of existing stock.
- Added no cart spawn/auto-drive, cart/player teleport, abstract route registration, universal remote storage, `getChunk`, region ticket, force-load or cross-dimension transport.
- Create remains design-reference-only for the product lesson of physical stock movement. No Create train/contraption/package/Stock Link/routing source or assets are bundled.
- Retained 0.41 Civil Works causeways, 0.40 physical breachers, 0.39 Bastion defense, 0.38 Outpost defense, 0.37 warehouse clusters and older logistics/expedition/endgame contracts.

## 0.41.0-alpha.1
- Added `Civil Works Causeways / 토목 공사소·도로 교량 시공` to reconnect large resource throughput with persistent world engineering instead of adding another combat tier.
- Added Stage1 `CIVIL_WORKS`: Stone Bricks2048 + Cobblestone1536 + Gravel1536 + Iron256 + Copper256, funded through existing physical logistics.
- Added registered-Barrel Civil Works commissioning yard: radius6 Stone Bricks48 + Scaffolding16 + Iron Blocks4 + Stonecutters2 + Crafting Table1.
- Added `ConstructionMode.CAUSEWAY`: actual selected BlockItem as flat 3-wide forward deck at 17/33/49/65 length using the existing Construction queue.
- Added explicit `level.hasChunkAt(target)` bulk-placement boundary; no chunk tickets or force-load.

## 0.40.0-alpha.1
- Added Bastion-final-wave physical Ravager/Vindicator breachers targeting only qualifying fortification with full grief/protection hooks and ordinary block drops.

## 0.39.0-alpha.1
- Added `Physical Bastion Defense`: radius6..12 real fortification, four quadrants each12 unique fortified columns, supply2 /4 waves /6000 ticks.

## 0.38.0-alpha.1
- Added `Defendable Physical Outposts`: active outpost, supply1, three waves, anchor-directed mobs, breach radius6/limit200 and owner64 requirement.

## 0.37.0-alpha.1
- Added `Physical Warehouse Clusters`: each depot anchor may link max8 additional real Barrels inside radius6 in `field_depots_v1`.

## 0.36.0-alpha.1
- Added bounded real-world commissioning before finalizable late-project funding.

## 0.35.0-alpha.1
- Added explicit High-volume Field Offload from main inventory slots9..35 into nearest usable real Barrel stock.

## 0.34.0-alpha.1
- Added shared material resolver for industrial batches, unfinished infrastructure and equipment spending. 0.42.1 changes its consumption order to nearest usable physical logistics storage first, then player inventory. Apex/Trial entry stays player-carried.

## 0.33.0-alpha.1
- Added one bounded sortie complication to each new operation: DEEP_FRONT, FORWARD_SHIFT or HOT_EXTRACTION.

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