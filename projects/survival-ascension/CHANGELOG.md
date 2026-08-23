# Changelog

## 0.29.0-alpha.1
- Added physical field depots backed by real vanilla Barrel inventories. `M -> Infrastructure -> 산업 가공소 -> 물류 거점 연결` finds the nearest Barrel within4 blocks and registers it for one stored industrial supply charge.
- Added independent per-player `field_depots_v1` persistence with dimension + x/y/z entries, maximum3 depots/player, duplicate sanitation and one-owner-per-physical-barrel enforcement.
- Re-selecting an already-owned nearby Barrel unlinks it without refund; another player's claimed Barrel cannot be registered.
- Field depot stock is usable only in the same dimension, within32 blocks, while the chunk is already loaded and `mayInteract` succeeds. The system never force-loads depot chunks.
- Loaded stale links are automatically pruned if the saved block is no longer a Barrel.
- Material resolution always consumes player inventory first, then usable depots nearest-first.
- Bulk Construction can now continue line/wall/floor/volume jobs from linked Barrel stock after the carried stack runs out. Existing target replacement/survival checks, NeoForge placement hook, global64 tick budget and pending512/player cap remain.
- Successful secondary Construction placements consume exactly one real block item. An unexpected post-place material disappearance rolls back the just-placed block instead of creating a free block.
- Irrigation replant now draws Wheat Seeds, Carrots, Potatoes, Beetroot Seeds and Nether Wart from player inventory then linked depots. Existing survival/protection hooks remain and failed post-place consumption rolls the young crop back.
- Production status and `/ascension stats` now expose registered depot count and currently active depot count; the production radial adds a Barrel-icon logistics action without adding a new generic GUI.
- Existing `production_v1`, `infrastructure_v1` and all older progression SavedData remain unchanged. `field_depots_v1` is additive and empty by default on old worlds.
- No packet schema changed; depot actions reuse the existing string-based `InfrastructureActionPayload`, keeping network protocol8.
- Extended Create reference-only research from 0.28 production into the high-level stock-backed request/local-restocking concept used by its modern logistics system. No Create logistics source, blocks, package formats, assets, GUI/data, namespaces or Ponder content are copied.
- Updated Guide, README/PROJECT canon, third-party policy, source audit and JAR verification for depot persistence, ownership, loaded-chunk/radius rules and material-consumption safety.

## 0.28.0-alpha.1
- Added Stage-1 shared infrastructure `산업 가공소 / Industrial Works`: Stone Bricks1024 + Iron512 + Copper512 + Redstone256 + Amethyst128.
- Added a MineMenu-style nested `산업 생산망` radial instead of a generic machine/quest rectangle. The submenu contains facility funding, four production lines, supply dispatch, status and back navigation.
- Added four large-batch production lines that deliberately consume different survival output categories: `제련 배치` Raw Iron96 + Raw Copper96 + Coal64; `구조재 배치` any logs192 + Cobblestone384 + Iron32; `식량 배치` Wheat128 + Carrot64 + Potato64 + Beetroot32; `정밀 부품 배치` Redstone128 + Amethyst64 + Gold32 + Quartz64.
- Added per-player `production_v1` SavedData with four bounded line buffers, lifetime cycle count and stored supply charges. Each line buffer is capped at3 and supply charges are capped at3.
- A production cycle requires at least one completed batch from all four lines. The system consumes exactly one from each line and creates one supply charge; a single abundant resource line cannot complete cycles by itself.
- Added queued-cycle normalization: if supply storage is full while complete four-line sets remain buffered, dispatching a charge immediately assembles waiting sets until the charge cap is reached, preventing a permanent buffer deadlock.
- Batch execution is atomic: the server checks line capacity and every input amount before consuming anything. Failed/partial-material requests do not eat resources.
- Added tangible supply dispatch: one stored supply charge produces Gold Ingots32 + Amethyst Shards16 + Echo Shards2, delivered to inventory or dropped if full.
- `/ascension stats` reports industrial lifetime cycles and stored supply charges.
- Existing `infrastructure_v1` schema is unchanged; `production_v1` is a new independent per-player store.
- No new network payload was added; protocol remains8.

## 0.27.0-alpha.1
- Added Stage-1 shared infrastructure `정점 추적소 / Apex Tracking Post`: Iron512 + Gold256 + Amethyst256 + Echo32 + Nether Star1.
- Added nine regional behavior-driven Apex Hunts with bounded lifecycle, persistent first-kill tracking and one-time9/9 mastery reward.

## 0.26.0-alpha.1
- Added18 rare regional field incidents: one bounded hostile ambush and one action-rush incident for each of the nine expedition regions.
- Added one-time per-region incident rewards, bounded lifecycle/cleanup and Trial-overlap exclusion.

## 0.25.0-alpha.1
- Replaced one fixed field objective per expedition region with two persistent directive options per region, for18 total directives across nine expedition regions.
- New region discoveries randomly select one directive per player and persist it in `expedition_v1`; standard directives preserve 0.24 behavior and legacy progress/reward migration.
- Field Mastery remains unchanged: Quarry7x7x12, Woodcut448, Harvest13x13, Academy shockwave7.5/20, Construction line65/plane13x13 and four Stage-2 Nexus air dashes.

## 0.24.0-alpha.1
- Reworked expeditions from instant biome discovery rewards into persistent `discovered -> field objective -> completed` progression with nine explicit regional objectives and legacy reward migration.

## 0.23.0-alpha.1
- Added `expedition_v1`, nine stage-gated vanilla-biome expedition regions, milestone rewards and final Stage-2 Field Mastery.

## 0.22.0-alpha.1
- Added randomized Ascension Trial doctrines `쇄도 / 추격 / 봉쇄` and 4-affix Awakened Mythic progression.

## 0.21.0-alpha.1
- Added repeatable Stage-2 four-wave Ascension Trial behind the completed Ascension Nexus.

## 0.20.0-alpha.1
- Added final Lv.100 Mastery VI across all six active skills.
