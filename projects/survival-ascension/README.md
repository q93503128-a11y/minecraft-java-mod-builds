# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension turns progression into larger physical actions, then makes world stages, expeditions, infrastructure, behavior-driven enemies, production, logistics and physical field bases consume that larger output again.

## 0.35.0-alpha.1 — High-volume Field Offload / 현장 일괄 적재
0.35 closes the **output side** of the high-throughput loop. 0.34 allowed large stationary sinks to consume stock already stored in nearby physical Barrels; 0.35 removes the opposite inventory-shuffling chore after 11×11 mining, 448-log felling or 13×13 harvesting.

### Explicit one-click offload
`M -> Infrastructure -> 산업 가공소 -> 현장 일괄 적재` uses the existing Industrial Works radial and existing string action packet.
- It is explicit, not automatic: no pickup event, background timer or silent inventory drain is added.
- Only the 27-slot main inventory area, slots `9..35`, is scanned.
- Hotbar slots `0..8`, equipped gear and offhand are never touched.
- The target set is intentionally limited to bulk progression materials: logs; raw ores/ingots and major minerals; common stone/terrain stock; crops/seeds; and materials used by current infrastructure, production and equipment progression.
- Tools, weapons, armor, food outside the authored crop set, potions, books, containers and arbitrary miscellaneous items are not swept into storage.

### Same physical logistics rules
Offload calls the same `usableContainers` path already used by 0.34 input logistics.
1. same dimension only;
2. ordinary linked Barrel radius32 / active physical outpost radius64;
3. chunk must already be loaded;
4. saved anchor must still be a real vanilla Barrel with a Container block entity;
5. `mayInteract` must pass;
6. missing loaded Barrels are pruned and their outpost upgrade is removed;
7. linked Barrels remain nearest-first.

Within each Barrel, matching existing stacks are filled before empty slots. Stack equality uses item + components, `Container.canPlaceItem` is respected, and the container/item maximum stack size is honored. If the available Barrels fill before the inventory stack is exhausted, only the accepted amount moves and the remainder stays in the original inventory stack.

The result is now a complete physical loop: **scaled gathering -> explicit bulk offload -> nearby real Barrel stock -> 0.34 industrial/infrastructure/reforge consumption -> outposts/operations/endgame**.

No new SavedData, packet type, protocol bump, global warehouse, cross-dimension storage or chunk force-load is introduced. Network protocol remains `8`.

## 0.34.0-alpha.1 — Integrated Logistics Backbone / 통합 물류 백본
0.34 closes the remaining gap between high-throughput skills and the physical Barrel/outpost network. Large base-side resource sinks no longer require the player to manually move hundreds of items into personal inventory when those items already exist in a nearby usable owned logistics Barrel.

### One physical stock resolver
`FieldDepotService` now exposes generic matcher-backed stock counting/consumption in addition to exact-item access. The internal shared paths are `countMatching` and `consumeMatching`, so the same real-stock path can resolve exact materials and tag-based inputs such as mixed vanilla logs.

Resolution order remains strict:
1. player inventory first;
2. currently usable linked Barrels, nearest first;
3. ordinary depot radius32, active physical outpost radius64;
4. same dimension only;
5. already-loaded chunks only;
6. the saved position must still be a real Barrel and pass `mayInteract`.

Missing loaded Barrels are still pruned. No chunk is force-loaded and no remote dimension inventory is opened.

### Large sinks connected to the network
- Industrial Works production batches now count and consume from inventory + usable linked Barrel stock. All four lines use the same rule, including tag-based log batches.
- Incomplete infrastructure projects now accept nearby usable logistics stock when the player funds them. This makes late infrastructure costs actual throughput sinks instead of inventory-shuffling chores.
- Equipment reforge and Mythic III awakening now use the same local stock resolver for their large Amethyst/Diamond/Scrap/Echo/Dragon Breath costs.
- Construction, irrigation and outpost-upgrade material use retain their existing linked-stock behavior.

The player inventory is always drained before a Barrel and linked Barrels are drained nearest-first, so the system does not silently prefer remote stock over carried materials.

### Deliberate field-combat boundary
Apex Hunt and Ascension Trial entry materials remain **player-carried**. They are field encounter preparations rather than stationary industrial transactions, so 0.34 does not turn them into remote warehouse payments. Industrial supply dispatch also still produces physical items into the player inventory/drop path.

No new SavedData, packet, menu or permanent stat layer is introduced. Network protocol remains `8`.

## 0.33.0-alpha.1 — Sortie Complications / 원정 작전 변수
0.33 keeps the 0.32 out-and-back operation catalog and rewards intact, but every **new** sortie now receives exactly one bounded server-authored complication. The complication changes where or when the same validated work must happen instead of adding another quest counter, another boss wave, blanket enemy HP, or a permanent stat bonus.

### Three bounded complications
- `전선 고착 / DEEP_FRONT`: crossing the outbound line still arms the operation, but validated field actions count only while the player remains beyond that authored outbound range. Falling back to the ordinary 48-block work zone pauses progress.
- `전선 재전개 / FORWARD_SHIFT`: after the first of the two field objectives completes, remaining objective progress pauses. The player must push to a second line 48 blocks beyond the operation's normal outbound range, still inside the matching ExpeditionRegion, before progress resumes.
- `긴급 철수 / HOT_EXTRACTION`: completing both field objectives starts a separate return clock. Stage0 gets 4:00, Stage1 gets 3:00, Stage2 gets 2:30. This clock is capped by the original operation deadline and requires the same exact physical outpost return as 0.32.

The server chooses one of the three when the sortie starts. One player still has at most one active operation and therefore at most one complication. Complication identity/state and the emergency extraction deadline are persisted inside the existing `expedition_operations_v1` record.

### Existing-world / active-sortie compatibility
The new fields are optional. A 0.32 active operation loaded after updating decodes as `NONE` and continues under its original 0.32 rules; the update never silently attaches a new penalty to an already-paid sortie. New 0.33+ launches always receive one of the three authored complications.

All old failure and safety contracts remain: death, dimension exit, creative/spectator switching or deadline expiry fails the operation; no supply refund; no client coordinate trust; no operation teleport; no chunk force-load; exact-origin return still revalidates the real Barrel/camp. Regional Incidents may still happen naturally, while Apex Hunt and Ascension Trial remain mutually exclusive with manual operation starts.

No new packet schema was added; network protocol remains `8`.

## 0.32.0-alpha.1 — Out-and-back Expedition Operations
0.32 turns a completed regional expedition plus a physical outpost into a repeatable sortie instead of another stationary menu reward.

### Start from a real regional outpost
From `M -> Infrastructure -> 산업 가공소 -> 원정 작전`:
- Stand within 4 blocks of one of your **active** physical outposts.
- The biome at the saved outpost anchor determines the operation region server-side.
- That region's original expedition directive must already be completed.
- Starting consumes 1 stored field-supply charge.
- Only one operation may be active per player.
- Apex Hunts and the Ascension Trial cannot be started on top of an active operation; an operation likewise cannot start while either is already active.
- Regional Incidents may still occur naturally during a sortie.

### Leave the base before work counts
Each region has one authored operation with two real-action objectives.
- Stage0 operations require first reaching at least 96 blocks from the origin outpost.
- Stage1 operations require 128 blocks.
- Stage2 End requires 160 blocks; Nether is also 160.
- After the range line is reached, objective actions count only while the player is at least 48 blocks from the origin and physically inside the matching expedition region.
- Counts reuse the existing validated server actions: smart-tree logs, material/protection-backed Construction placements, mature crops, legitimate travel, ocean voyage, valid pickaxe mining, hostile kills and successful dash uses.
- Inventory clicks, fake client movement and actions performed beside the outpost do not count.

### Nine regional sorties
- Woodland: `심림 순환 벌채` — range96, logs128 + travel240, 20 minutes.
- Arid: `사막 보급로 개척` — range96, build96 + travel240, 20 minutes.
- Wetland: `습지 채집·소탕` — range96, crops80 + hostile kills8, 20 minutes.
- Highlands: `능선 장거리 순찰` — range128, travel600 + dashes12, 20 minutes.
- Ocean: `외해 순항` — range128, ocean voyage900 + hostile kills8, 20 minutes.
- Deep: `심층 채굴 회수` — range128, mine192 + hostile kills10, 25 minutes.
- Frozen: `백설 장거리 순찰` — range128, travel600 + hostile kills10, 25 minutes.
- Nether: `네더 전진 작전` — range160, hostile kills24 + mine96, 25 minutes.
- End: `공허 외곽 소탕` — range160, hostile kills28 + travel360, 30 minutes.

### Return to the same physical base
Finishing the two field objectives is not enough. The player must return to within 8 blocks of the exact outpost that launched the sortie.
- The origin outpost/camp is revalidated at return time.
- No chunk is force-loaded and no destination is client-supplied.
- Death, dimension exit, creative/spectator switching or timeout fails the sortie with no supply refund.
- 0.31 field recovery is separate: an operation death fails the operation, while the prepaid recovery contract still uses its own ordinary-death/96-block rules.
- Active operation state, progress, origin and deadline persist through logout/restart in independent `expedition_operations_v1`.

### Rewards and first-clear tracking
Every successful return grants repeatable skill XP/experience plus stage-scaled vanilla resources. `expedition_operations_v1` also tracks first successful return per region and lifetime successful sorties.
- Stage0 return: region skill XP250 + experience75 + Emerald8 + Amethyst8.
- Stage1 return: region skill XP400 + experience125 + Diamond2 + Amethyst16 + Echo2.
- Stage2 return: region skill XP600 + experience200 + Diamond4 + Echo4 + Dragon Breath2.
- First successful return in all nine regions grants one non-repeatable logistics-endgame package: Netherite Scrap2 + Echo16 + Amethyst64 + Dragon Breath8 + experience300.

`/ascension stats`, Industrial Works status and the Guide report operation first-clears, lifetime returns and active sortie state.

No new packet schema was added; the existing string-based `InfrastructureActionPayload(projectId, action)` carries the operation action, so network protocol remains `8`.

## 0.31 — Death-bound Field Recovery
- Active outpost within4; first arming costs one field-supply charge.
- Independent `field_recovery_v1` stores exactly one prepaid armed/pending recovery token and lifetime successes.
- Qualifying ordinary death must be same-dimension and within96 of the saved operational outpost.
- Incident/Apex/Trial deaths do not consume the token.
- Safe post-respawn return revalidates loaded Barrel/camp, `mayInteract`, sturdy floor, collision and fluid.
- Token is consumed only after successful server teleport. Failed validation preserves it.
- There is no ordinary living-player outpost fast travel.

## 0.30 — Physical Field Outposts
- Upgrade an owned registered Barrel depot while within4 blocks.
- Physical camp within5 requires an interactable Bed, Campfire/Soul Campfire, Crafting Table and Furnace/Blast Furnace/Smoker.
- Cost: supply charges2 + Iron32 + Gold8 + Coal32.
- `outpost_v1` is independent SavedData and remains tied to the exact depot coordinate.
- Active only while owner is same-dimension/within64, anchor chunk is loaded, Barrel exists/interactable and the real camp structure remains valid.
- Ordinary depot supply radius32; active outpost depot radius64.
- Active outpost suppresses only `NATURAL` hostile spawns within24; `TRIGGERED` Incident/Apex/Trial spawns remain untouched.
- No force-loaded chunks.

## 0.29 — Physical Field Depots
- Register a real vanilla Barrel within4 blocks for one field-supply charge; max3/player and one owner per physical position.
- Same-dimension loaded Barrel stock supplies bulk Construction, irrigation, Industrial Works batches, post-Industrial infrastructure funding and equipment reforge/awakening within the applicable32/64 logistics radius.
- 0.35 adds explicit nearest-first offload of authored bulk resources from main inventory slots9..35 into that same real stock, while preserving hotbar/equipment/offhand.
- Player inventory is consumed first by resource sinks; linked Barrels are nearest-first for both input consumption and offload target order.
- `mayInteract` is rechecked and linked chunks are never force-loaded.
- Missing loaded Barrels prune stale links. Construction/replant roll back newly placed blocks if post-place material consumption unexpectedly fails.
- `field_depots_v1` is independent SavedData.

## 0.28 — Industrial Works / Four-line Production
- Stage-1 `산업 가공소`: Stone Bricks1024 + Iron512 + Copper512 + Redstone256 + Amethyst128.
- Four atomic large-batch lines: Raw Iron/Copper/Coal, logs/cobblestone/iron, crops, and redstone/amethyst/gold/quartz.
- `production_v1` stores per-player line buffers, lifetime cycles and supply charges.
- One cycle requires one batch from all four lines. Buffers and supply charges are capped at3.
- One charge may be dispatched as Gold32 + Amethyst16 + Echo2, used to register a depot, arm recovery, or launch a completed-region sortie.

## 0.27 — Apex Hunts
- Stage-1 `정점 추적소`: Iron512 + Gold256 + Amethyst256 + Echo32 + Nether Star1.
- Re-select it inside a completed expedition region to start that region's 90-second Apex Hunt for **player-carried** Echo8 + Amethyst32 + Gold32.
- Nine regional bosses use separate behavior identities: charge, reinforcements, poison/heal field, skirmish repositioning, pull, leap, frost, wither pressure and levitation/void pressure.
- `apex_hunt_v1` stores first defeats and total victories per player. First defeat of all nine grants a one-time Mythic III/endgame resource package.
- Stage-2 Ascension Trial remains the stronger deterministic Mythic III source; its entry materials are also deliberately player-carried.

## 0.25–0.26 Expeditions
- Nine vanilla-biome expedition regions have two persistent directives each, 18 total.
- Directive tasks reuse real smart-tree felling, protected/material-backed construction, mature harvests, legitimate movement/voyage, valid mining, hostile kills and validated dash uses.
- Each region also has one ambush and one action-rush incident, 18 incidents total.
- Incident rewards are once per player/region and can add at most20% to the first unfinished directive task.
- Complete all nine regions at Stage2 to unlock Field Mastery.

## Field Mastery
At Lv.100 and after the nine-region completion:
- Mining Quarry tunnel `7x7x10 -> 7x7x12`, still12 blocks/player/tick and64 globally.
- Woodcutting `384 -> 448` natural-tree logs,12/player and64/global.
- Harvesting `11x11 -> 13x13`,12/player and64/global.
- Combat Academy shockwave `6.5/16 -> 7.5/20`, same55% damage fraction and50-tick cooldown.
- Construction line `49 -> 65`, plane `11x11 -> 13x13`; volume remains `7x7x7` with real materials/protection hooks.
- Mobility Stage-2 Nexus air dashes `3 -> 4`.

## Existing endgame
- World Ascension: Awakening0 -> first Wither Legendary1 -> first Ender Dragon Endgame2.
- Stage2 natural hostile subsets can gain Withered / Phase / Plague mutations; Elite ranks and Warband roles can layer through normal spawn paths.
- Complete Ascension Nexus to access the repeatable four-wave doctrine Trial.
- Mythic III gear can be awakened once from exactly3 affixes to4 affixes with large resource costs; 0.34 allows those stationary upgrade costs to draw from the local physical logistics network.

## External references
- 0.35 introduces no new third-party implementation or asset source; it extends Survival Ascension's existing Barrel logistics with vanilla/NeoForge Container contracts.
- Deep Rock Galactic mission mutators/extraction and Warframe Sortie/Deep Archimedea mission modifiers are 0.33 product-level design references only. Survival Ascension copies no source, data, UI, assets, names, audio or proprietary content from either game.
- Heracles (`terrarium-earth/Heracles`) is MIT. 0.32 studies only the product-level idea that repeatable multi-step tasks should have explicit objective/completion state; the out-and-back physical-base loop, persistence, rewards and action hooks are independent Survival Ascension code. No Heracles quest data, UI, source structures, assets or namespace are copied.
- Bountiful remains GPL-3.0 reference-only for objective/reward contract philosophy; no source/data/UI/assets are copied.
- Waystones 26.2 remains All Rights Reserved and Corpse LGPL-3.0; both remain reference-only for 0.31 travel/death friction.
- MineColonies remains GPLv3 reference-only for 0.30's physical forward-base product lesson.
- Create's repository license split remains code MIT / `src/main/resources/assets/` All Rights Reserved. 0.28–0.30 use only high-level throughput/logistics concepts.
- Building Gadgets 2 remains the MIT reference for material-backed protected Construction behavior.
- Other permissive adaptations and reference-only projects are documented in `THIRD_PARTY_NOTICES.md` and packaged notices.

Main radial: Skills / Mining / Construction / Equipment / Infrastructure / Guide / Close. Shift remains the precision override for scaled work.
