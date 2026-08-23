# Changelog

## 0.32.0-alpha.1
- Added repeatable out-and-back `원정 작전` for all nine expedition regions, staged from an active physical outpost in a region whose original directive is already complete.
- Launch from `M -> Infrastructure -> 산업 가공소 -> 원정 작전`; the server resolves the nearest active owned outpost within4 blocks and derives its expedition region from the saved anchor biome. Launch costs one stored field-supply charge.
- Added nine authored operation profiles: Woodland/Arid/Wetland range96, Highlands/Ocean/Deep/Frozen range128, Nether/End range160, each with exactly two existing validated ExpeditionAction objectives and a20/25/30-minute deadline by stage.
- Operation objectives do not count until the outbound range line has been crossed, then count only outside48 blocks of the origin and while the player is physically inside the matching expedition region.
- Reused the same real server-authoritative action hooks as directives/incidents: smart-tree logs, successful material/protection-backed Construction placements, mature crops, legitimate movement, ocean voyage, valid pickaxe mining, hostile kills and successful dash uses.
- Completing field objectives does not grant rewards remotely; the player must return to within8 blocks of the exact launching outpost, which is revalidated as a real loaded/interactable Barrel + four-part camp before completion.
- Added independent `expedition_operations_v1` persistence for active region, origin dimension/position, deadline, range-reached state, two bounded progress counters, first-return region mask, lifetime return count and one-time9/9 reward flag. Active operations survive logout/restart.
- Death, dimension exit, creative/spectator switching or timeout fails the active operation with no supply refund. Origin chunks are never force-loaded.
- 0.31 field recovery remains independent: an operation death fails the sortie, while the prepaid recovery contract may still qualify under its own same-dimension/96-block ordinary-death rules.
- Apex Hunt and Ascension Trial manual starts are mutually exclusive with active operations. Regional Incidents may still occur naturally during a sortie.
- Repeatable return rewards scale by world stage: Stage0 skillXP250/XP75/Emerald8/Amethyst8; Stage1 skillXP400/XP125/Diamond2/Amethyst16/Echo2; Stage2 skillXP600/XP200/Diamond4/Echo4/DragonBreath2.
- First successful return in all nine regions grants one extra logistics-endgame package: Netherite Scrap2 + Echo16 + Amethyst64 + Dragon Breath8 + XP300. No permanent flat-stat multiplier is added.
- Industrial radial, Production status, `/ascension stats` and Guide now expose operation launch/progress/first-clear/lifetime-return state. No new packet schema; protocol remains8.
- Heracles is MIT and is used only as a product-level reference for explicit multi-step objective/completion state. Bountiful remains GPL-3.0 reference-only. No quest data/UI/source structures/assets/namespaces are copied.
- Updated README/PROJECT canon, third-party policy, source audit and JAR verification for `expedition_operations_v1`, exact operation catalog, range/work/return gates, persistence, failure rules, challenge overlap and old-system regressions.

## 0.31.0-alpha.1
- Added prepaid, one-use `현장 복귀 계약` to active physical outposts without adding ordinary waystone-style fast travel.
- `M -> Infrastructure -> 산업 가공소 -> 현장 복귀 계약` requires an active outpost within4 blocks; first arming consumes one stored field-supply charge in advance.
- An already-paid armed contract may be retargeted to another active outpost without another charge; selecting the same target does not double-charge.
- Added independent `field_recovery_v1` SavedData with optional armed point, optional pending point and lifetime successful recovery count per player. Older worlds start unset.
- Recovery qualifies only for same-dimension general deaths within96 blocks of the armed outpost while the real loaded/interactable Barrel + Bed/Campfire/Crafting/Furnace structure remains operational.
- Regional Incident, Apex Hunt and Ascension Trial deaths explicitly do not queue or consume field recovery, preventing encounter extra-life abuse.
- A qualifying death atomically moves the prepaid state from armed to pending. Respawn then attempts a server-side return to a safe standing position around the outpost.
- Safe arrival requires loaded blocks, `mayInteract`, sturdy floor, empty feet/head collision and no fluid. No chunk force-loading is added.
- Pending recovery is consumed only after `ServerPlayer.teleportTo` succeeds. Failed target validation, arrival search or teleport preserves the pending contract.
- A pending contract can be retried. If its original target is unavailable, standing by another active outpost allows the prepaid token to be rearmed there without another supply charge.
- Successful recovery clears movement/fall state and increments lifetime recovery count.
- Added `AscensionTrialSystem.isActive(player)` as a read-only overlap check while retaining the full four-wave Trial lifecycle and rewards.
- Production radial, Industrial status, `/ascension stats` and Guide now expose recovery setup/state/counter. No new packet schema was added; protocol remains8.
- Waystones 26.2 is All Rights Reserved and Corpse is LGPL-3.0; both are reference-only for product-level death/travel friction. No source, blocks/entities, inventory-transfer logic, assets, data or namespaces are copied.
- Updated README/PROJECT canon, third-party policy, source audit and JAR verification for recovery persistence, death qualification, challenge exclusion, safe arrival and no-fast-travel/no-force-load contracts.

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

## 0.29.0-alpha.1
- Added physical field depots backed by real vanilla Barrel inventories. `M -> Infrastructure -> 산업 가공소 -> 물류 거점 연결` finds the nearest Barrel within4 blocks and registers it for one stored industrial supply charge.
- Added independent per-player `field_depots_v1` persistence with dimension + x/y/z entries, maximum3 depots/player, duplicate sanitation and one-owner-per-physical-barrel enforcement.
- Same-dimension loaded Barrel stock supplies bulk Construction and irrigation within32 blocks; player inventory is consumed first and chunks are never force-loaded.

## 0.28.0-alpha.1
- Added Stage-1 shared infrastructure `산업 가공소 / Industrial Works`, four atomic large-batch production lines, bounded buffers and stored field-supply charges.

## 0.27.0-alpha.1
- Added Stage-1 `정점 추적소 / Apex Tracking Post` and nine regional behavior-driven Apex Hunts with bounded lifecycle and one-time9/9 reward.

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
