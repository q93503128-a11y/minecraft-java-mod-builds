# Changelog

## 0.30.0-alpha.1
- Added physical field outposts that upgrade an existing registered Barrel depot into a forward base instead of introducing a virtual claim or generic management GUI.
- `M -> Infrastructure -> 산업 가공소 -> 전초기지 승격` finds the nearest owned depot within4 blocks server-side; no client coordinate is trusted.
- Outpost structure requires Bed + Campfire/Soul Campfire + Crafting Table + Furnace/Blast Furnace/Smoker within5 blocks of the anchor Barrel.
- Upgrade cost after full validation: field-supply charges2 + Iron32 + Gold8 + Coal32. Material counts/consumption combine player inventory and currently usable linked Barrel stock, player-first.
- Added independent `outpost_v1` SavedData keyed by player and exact dimension/x/y/z anchor, deduplicated and capped at3 entries/player. Existing `field_depots_v1` remains the ownership source of truth.
- Unlinking or auto-pruning the underlying field depot also removes its outpost upgrade without refund.
- Active outpost requires owner online, same dimension, within64 blocks, loaded/interactable anchor Barrel and all four physical camp component categories still present. No chunk tickets or force-loading are added.
- Active outpost extends only its own depot supply radius from32 to64 while preserving real-stock, player-first, nearest-depot material resolution.
- Added a24-block local safety zone that cancels only `NATURAL` hostile `FinalizeSpawnEvent`s around an active outpost. `TRIGGERED` encounter spawns remain untouched so Regional Incidents, Apex Hunts and Ascension Trials cannot be disabled by an outpost.
- Added atomic multi-charge production consumption for the two-charge outpost cost while retaining queued four-line cycle normalization.
- Production status, `/ascension stats`, Industrial Works radial and Guide now report/operate upgraded and active outposts.
- No packet schema changed; the outpost action reuses the existing string-based Industrial Works payload and network protocol remains8.
- MineColonies is reference-only for the product-level idea that a forward settlement combines physical facilities, supply and local defense. Current public release pages identify GPLv3; no MineColonies source, blueprints, citizens, building data, UI, assets, research, raid code or namespaces are copied.
- Updated README/PROJECT canon, third-party policy, source audit and JAR verification for physical structure checks, costs, outpost persistence,64-block logistics,24-block NATURAL-only safe zone and old-system regressions.

## 0.29.0-alpha.1
- Added physical field depots backed by real vanilla Barrel inventories. `M -> Infrastructure -> 산업 가공소 -> 물류 거점 연결` finds the nearest Barrel within4 blocks and registers it for one stored industrial supply charge.
- Added independent per-player `field_depots_v1` persistence with dimension + x/y/z entries, maximum3 depots/player, duplicate sanitation and one-owner-per-physical-barrel enforcement.
- Re-selecting an already-owned nearby Barrel unlinks it without refund; another player's claimed Barrel cannot be registered.
- Field depot stock is usable only in the same dimension, within32 blocks, while the chunk is already loaded and `mayInteract` succeeds. The system never force-loads depot chunks.
- Loaded stale links are automatically pruned if the saved block is no longer a Barrel.
- Material resolution always consumes player inventory first, then usable depots nearest-first.
- Bulk Construction can continue line/wall/floor/volume jobs from linked Barrel stock after the carried stack runs out while preserving protection hooks and tick limits.
- Irrigation replant draws its real seed/crop items from player inventory then linked depots with rollback on unexpected post-place consume failure.
- Production status and `/ascension stats` expose registered/active depots; protocol remains8.

## 0.28.0-alpha.1
- Added Stage-1 shared infrastructure `산업 가공소 / Industrial Works`: Stone Bricks1024 + Iron512 + Copper512 + Redstone256 + Amethyst128.
- Added a MineMenu-style nested industrial radial and four atomic large-batch production lines.
- Added `production_v1` with four bounded line buffers, lifetime cycle count and stored supply charges, all capped/normalized to prevent deadlock.
- One four-line cycle creates one supply charge; dispatch may create Gold32 + Amethyst16 + Echo2.

## 0.27.0-alpha.1
- Added Stage-1 `정점 추적소 / Apex Tracking Post` and nine regional behavior-driven Apex Hunts with bounded lifecycle and one-time9/9 reward.

## 0.26.0-alpha.1
- Added18 rare regional field incidents: one bounded hostile ambush and one action-rush incident for each of the nine expedition regions.

## 0.25.0-alpha.1
- Replaced one fixed field objective per expedition region with two persistent directive options per region, for18 total directives across nine expedition regions.
- Field Mastery remains Quarry7x7x12, Wood448, Harvest13x13, Academy7.5/20, Construction65/13x13 and four Stage2 air dashes.

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
