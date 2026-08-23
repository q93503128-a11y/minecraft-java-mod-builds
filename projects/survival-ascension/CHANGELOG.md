# Changelog

## 0.36.0-alpha.1
- Added `InfrastructureSiteService` so the three largest Stage1/2 infrastructure projects cannot cross their final funding line as pure counters; the last finalizable funding action now requires a bounded real-world commissioning site.
- Added Industrial Works physical commissioning around any real interactable Barrel within4: radius6 must contain Stone Bricks48, Iron Blocks4, Blast Furnaces2, Stonecutter1 and Hoppers2.
- Added Apex Tracking Post commissioning around the player's own registered logistics Barrel within4: radius6 must contain Stone Bricks32, Gold Blocks4, Lodestone1, Cartography Table1 and Targets4.
- Added Ascension Nexus commissioning around the player's own registered logistics Barrel within4: radius6 must contain Obsidian32, Crying Obsidian8, Beacon1, Enchanting Table1 and Ender Chest1.
- Commissioning only runs when `canFullyFundNow` shows the current inventory + usable local logistics stock can satisfy every remaining material. `validateForFinalFunding` runs before any material is consumed by that finalizable funding call; an incomplete site therefore consumes zero project material in that call.
- Preserved existing-world compatibility: the existing completed-project branch runs before 0.36 commissioning, so existing completed Industrial Works/Apex Tracking Post/Ascension Nexus remain complete and usable without retroactive rebuilding.
- Commissioning checks only already-loaded blocks, requires `mayInteract`, validates a real Barrel Container anchor, trusts no client coordinate, and adds no chunk force-load/ticket/background scan.
- Kept Stage0 infrastructure material-only, kept the original `infrastructure_v1` funding schema unchanged, and added no new SavedData, packet, protocol bump or maintenance simulation.
- Updated Infrastructure/Industrial radials, Guide, README/PROJECT and source audit to expose exact site requirements and lock validation-before-consumption, old-complete compatibility, and all 0.35/older regressions.

## 0.35.0-alpha.1
- Added explicit `현장 일괄 적재 / High-volume Field Offload` to close the output side of the scaled-gathering logistics loop.
- Added `ProductionService.ACTION_BULK_OFFLOAD = "bulk_offload"` and routed it through the existing Industrial Works action packet; no packet schema or protocol change.
- Added a Hopper-icon `현장 일괄 적재` entry to the existing Industrial Works radial. Also corrected the old `시설 투자` detail text that still incorrectly described infrastructure funding as inventory-only after 0.34.
- Bulk offload scans only player main inventory slots9..35. Hotbar0..8, equipped gear and offhand are explicitly outside the authored range and never moved.
- Added an authored bulk-material predicate covering logs, raw ores/ingots/minerals, common stone/terrain stock, crops/seeds and current progression/infrastructure materials instead of sweeping arbitrary inventory contents.
- Offload targets use the existing nearest-first `usableContainers` path: same dimension, ordinary depot32 / active outpost64, already-loaded chunk, real vanilla Barrel Container, `mayInteract`, stale-link pruning and no chunk force-load.
- Each target Barrel fills matching existing stacks before empty slots. Transfers require `ItemStack.isSameItemSameComponents`, `Container.canPlaceItem`, and the applicable container/item stack-size limit.
- Source inventory stacks shrink only by the amount actually inserted. Full/partial Barrel capacity leaves every unaccepted remainder in the original inventory stack; changed containers and the player inventory are marked/broadcast after successful movement.
- Kept offload fully explicit: no item-pickup hook, server-tick automation, hidden toggle, virtual warehouse, cross-dimension storage or background routing.
- Updated Guide/status/README/PROJECT and source audit to lock the offload slot boundary, material whitelist, nearest physical Barrel routing, component/capacity safety and all 0.34/older regressions.

## 0.34.0-alpha.1
- Added an integrated physical logistics backbone for large stationary resource sinks instead of adding another independent progression layer.
- Generalized `FieldDepotService` with matcher-backed `countMatching` / `consumeMatching`, allowing exact items and tag-style inputs such as mixed logs to share the same real inventory + linked-Barrel resolution path.
- Industrial Works production now counts and consumes each batch from player inventory first, then currently usable linked Barrels nearest-first. Existing32-block depot and64-block active-outpost radii, same-dimension rules, loaded-chunk requirement, real-Barrel validation, `mayInteract` and stale-link pruning remain authoritative.
- Incomplete infrastructure funding now uses the same physical stock resolver, so large later projects can consume nearby stored throughput instead of requiring hundreds of items to be manually moved into player inventory.
- Equipment reforge and Mythic III awakening now use the same local physical stock resolver for their large material costs while preserving all existing item/affix validation. Salvage rewards still go to player inventory/drop.
- Industrial supply dispatch still creates physical player-carried output rather than remotely depositing rewards into linked storage.
- Apex Hunt and Ascension Trial entry materials deliberately remain inventory-only. 0.34 integrates stationary base/economy inputs without turning field-combat preparation into universal remote warehouse payment.
- No new SavedData, packet, radial page, quest GUI, remote-dimension inventory access or chunk force-loading was added. Network protocol remains8.
- Updated Guide/README/PROJECT and source audit to lock inventory-first + nearest-Barrel behavior, matcher/tag support, all physical access boundaries, deliberate Apex/Trial carry-in behavior and 0.33/older regressions.

## 0.33.0-alpha.1
- Added one bounded server-chosen sortie complication to every newly launched 0.32-style out-and-back expedition operation without changing the nine regional objective catalogs, repeatable rewards or first-clear rewards.
- Added `ExpeditionComplication` with three authored rules: `DEEP_FRONT / 전선 고착`, `FORWARD_SHIFT / 전선 재전개`, and `HOT_EXTRACTION / 긴급 철수`.
- `전선 고착` requires validated operation actions to remain at or beyond the operation's authored outbound range instead of allowing fallback to the normal 48-block work zone.
- `전선 재전개` pauses remaining objective progress after the first field objective completes; progress resumes only after reaching a second server-checked line 48 blocks beyond the normal outbound range while still in the matching expedition region.
- `긴급 철수` arms a separate physical-return deadline only after both validated field objectives complete: Stage0 4:00, Stage1 3:00, Stage2 2:30. The extraction deadline is capped by the original operation deadline.
- Extended the existing `expedition_operations_v1` codec with optional `complication`, `complication_state` and `extraction_deadline` fields. Existing 0.32 active operations decode as `NONE` and retain their paid original rules instead of receiving a new modifier during migration.
- Complication identity/state persists across logout/restart, malformed complication names sanitize to `NONE`, and bounded state/deadline sanitation prevents malformed save values from expanding the authored contract.
- Complications reuse the existing validated `ExpeditionAction` plumbing and exact-origin physical outpost return. No client coordinate trust, teleport, chunk force-load, new generic quest GUI, blanket HP multiplier or permanent stat bonus was added.
- Regional Incidents remain ambient and may occur during operations; Apex Hunt and Ascension Trial remain mutually exclusive with manual operation launch. Existing death/dimension/game-mode/base-deadline failures and no-refund policy remain unchanged.
- Industrial status/system messages and Guide now expose the selected complication, forward-redeployment state and emergency extraction timer. Network protocol remains8 with no new packet type.
- Deep Rock Galactic mission mutator/extraction structure and Warframe Sortie/Deep Archimedea mission-modifier structure were used only as product-level design references. No source, data, UI, assets, audio or proprietary content was copied or bundled.

## 0.32.0-alpha.1
- Added repeatable out-and-back `원정 작전` for all nine expedition regions, staged from an active physical outpost in a region whose original directive is already complete.
- Launch from `M -> Infrastructure -> 산업 가공소 -> 원정 작전`; the server resolves the nearest active owned outpost within4 blocks and derives its expedition region from the saved anchor biome. Launch costs one stored field-supply charge.
- Added nine authored operation profiles with two validated-action objectives, stage-specific outbound ranges and20/25/30-minute deadlines.
- Operation objectives count only after the outbound range and while outside48 blocks in the matching expedition region; completion requires returning within8 of the exact launching operational outpost.
- Death, dimension exit, creative/spectator or timeout fails with no supply refund. Origin chunks are never force-loaded.
- `expedition_operations_v1` persists active state, first returns, lifetime returns and the one-time9/9 reward.
- Apex/Trial starts are mutually exclusive with operations; Regional Incidents may still occur naturally.

## 0.31.0-alpha.1
- Added prepaid one-use `현장 복귀 계약` to active physical outposts without adding ordinary fast travel.
- Same-dimension ordinary death within96 may queue recovery; Incident/Apex/Trial deaths are excluded.
- Independent `field_recovery_v1` persists armed/pending state; safe arrival is validated and the token consumes only after successful teleport.
- No chunk force-loading.

## 0.30.0-alpha.1
- Added physical field outposts that upgrade an owned registered Barrel depot.
- Structure requires Bed + Campfire/Soul Campfire + Crafting Table + Furnace/Blast Furnace/Smoker within5.
- Upgrade cost supply2 + Iron32 + Gold8 + Coal32.
- Active outpost extends logistics from32 to64 and suppresses only NATURAL hostile spawns within24; TRIGGERED encounters remain untouched.
- `outpost_v1`, max3/player, no force-load.

## 0.29.0-alpha.1
- Added physical field depots backed by real vanilla Barrel inventories, registration within4 for supply1, max3/player and one owner per physical Barrel.
- Same-dimension loaded Barrel stock supplies bulk systems inside32; active outpost version extends to64.
- `field_depots_v1` remains the ownership source of truth.

## 0.28.0-alpha.1
- Added Stage1 `산업 가공소 / Industrial Works`, four atomic large-batch production lines, bounded buffers and stored field-supply charges.

## 0.27.0-alpha.1
- Added Stage1 `정점 추적소 / Apex Tracking Post` and nine regional behavior-driven Apex Hunts with bounded lifecycle and one-time9/9 reward.

## 0.26.0-alpha.1
- Added18 rare regional field incidents: one bounded hostile ambush and one action-rush incident for each of the nine expedition regions.

## 0.25.0-alpha.1
- Replaced one fixed field objective per expedition region with two persistent directive options per region, for18 total directives across nine expedition regions.

## 0.24.0-alpha.1
- Reworked expeditions from instant biome discovery rewards into persistent `discovered -> field objective -> completed` progression.

## 0.23.0-alpha.1
- Added `expedition_v1`, nine stage-gated vanilla-biome expedition regions, milestone rewards and final Stage2 Field Mastery.

## 0.22.0-alpha.1
- Added randomized Ascension Trial doctrines and4-affix Awakened Mythic progression.

## 0.21.0-alpha.1
- Added repeatable Stage2 four-wave Ascension Trial behind the completed Ascension Nexus.

## 0.20.0-alpha.1
- Added final Lv100 Mastery VI across all six active skills.
