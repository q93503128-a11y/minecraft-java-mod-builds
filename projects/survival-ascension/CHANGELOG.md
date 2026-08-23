# Changelog

## 0.41.0-alpha.1
- Added `Civil Works Causeways / 토목 공사소·도로 교량 시공` to reconnect large resource throughput with persistent world engineering instead of adding another combat tier.
- Added Stage1 `CIVIL_WORKS` infrastructure project: Stone Bricks2048 + Cobblestone1536 + Gravel1536 + Iron256 + Copper256.
- Civil Works uses existing inventory-first + physical logistics Barrel funding and stores progress inside existing `infrastructure_v1`; no new SavedData ID or migration.
- Extended `InfrastructureSiteService` with a registered-Barrel Civil Works yard: radius6 Stone Bricks48 + Scaffolding16 + Iron Blocks4 + Stonecutters2 + Crafting Table1.
- Finalizable Civil Works funding validates that real loaded commissioning yard before consuming any material in the final call; later dismantling does not disable the completed project.
- Added `ConstructionMode.CAUSEWAY`, server-gated by Construction Lv60 and completed Civil Works.
- Added a `도로/교량` entry to the existing Construction radial and a `토목 공사소` entry to the Infrastructure radial; no packet schema/protocol bump.
- Causeway places the player's actual chosen BlockItem as a flat three-wide deck extending forward in dominant horizontal look direction: Lv60 3×17, Lv90 3×33, Lv100 3×49, Field Mastery 3×65.
- Causeway uses the same Construction queue, max512 pending targets/player, global64/tick and local8/tick work budget instead of a second auto-builder.
- Added explicit loaded-only bulk construction target check with `level.hasChunkAt(target)` before block-state/protection work. Unloaded segments skip; no `getChunk`, ticket or force-load.
- Retained player/physical-depot material consumption, `mayInteract`, replaceability, block survival, NeoForge `EventHooks.onBlockPlace`, rollback on material race and Shift precision override.
- Causeway does not erase terrain, auto-level ground, generate free support pillars or choose decorative palettes; the resulting real deck/bridge is the world consequence.
- Building Gadgets 2 remains covered by the existing MIT notice; 0.41 extends the already-adapted material/protection/tick-distribution principles without bundling external assets or a new dependency.
- Retained 0.40 physical breachers, 0.39 Bastion defense, 0.38 Outpost defense, 0.37 warehouse clusters and older logistics/expedition/endgame contracts.

## 0.40.0-alpha.1
- Added `Physical Siege Breachers / 물리 공성 파괴자` so the unique fourth Bastion wave can physically answer the player-built wall instead of only increasing enemy statistics.
- Added `OutpostSiegeBreachService` and registered it on the server tick bus.
- Breaching is Bastion-only by reusing the existing siege owner/wave NBT and requiring wave4; normal three-wave Outpost Defense never enters the block-breaking path.
- Only Ravagers and Vindicators may breach: Ravager successful-break cooldown30 ticks, Vindicator60 ticks.
- Breakers search only a tiny local area around themselves and only target vanilla `WALLS`, Iron Bars and Nether Brick Fence inside the outpost annulus radius6..12.
- Added full protection chain: active siege owner, same-dimension operational owned outpost, loaded position, `EventHooks.canEntityGrief`, owner `mayInteract`, `state.canEntityDestroy`, `EventHooks.onEntityDestroyBlock`, and no block entity.
- Fortification destruction uses normal item drops, letting the defender recover material and rebuild with the existing Construction system.
- Added no arbitrary terrain destruction, storage/anchor destruction, new packet, new SavedData ID, client coordinate trust, custom block/item, `getChunk`, region ticket or force-load.

## 0.39.0-alpha.1
- Added `Physical Bastion Defense / 물리 요새 방어`: radius6..12 physical wall ring, four quadrants each12 unique fortified x/z columns, supply2 /4 waves /6000 ticks.
- Bastion revalidates the physical ring between waves and scales through authored role overlap rather than a blanket HP/attack multiplier.

## 0.38.0-alpha.1
- Added `Defendable Physical Outposts / 전초 방어전`: active outpost, supply1, three waves, anchor-directed mobs, breach radius6/limit200 and owner64 physical-defense requirement.

## 0.37.0-alpha.1
- Added `Physical Warehouse Clusters / 물리 창고군`: each depot anchor may link max8 additional real Barrels inside radius6, with optional `warehouse_links` in `field_depots_v1`.

## 0.36.0-alpha.1
- Added bounded real-world commissioning for Industrial Works, Apex Tracking Post and Ascension Nexus before finalizable funding can cross completion.

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
