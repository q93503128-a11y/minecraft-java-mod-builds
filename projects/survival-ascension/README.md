# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension turns progression into larger physical actions, then makes world stages, expeditions, infrastructure, behavior-driven enemies, production, logistics and physical field bases consume that larger output again.

## 0.30.0-alpha.1 — Physical Field Outposts
0.30 upgrades the 0.29 Barrel logistics node into a real, player-built forward base instead of another invisible menu perk.

### Upgrade a real depot into an outpost
From `M -> Infrastructure -> 산업 가공소 -> 전초기지 승격`:
- Stand within 4 blocks of one of your registered Barrel field depots.
- The Barrel must have, within 5 blocks, at least one Bed, Campfire/Soul Campfire, Crafting Table and Furnace/Blast Furnace/Smoker.
- Upgrade cost: 2 stored field-supply charges + Iron Ingots32 + Gold Ingots8 + Coal32.
- Upgrade material counts combine player inventory and currently usable linked Barrel stock.
- The outpost is persisted in new independent `outpost_v1` and remains tied to that depot position.
- Unlinking the underlying field depot also removes the outpost upgrade without refund.

### Active outpost rules
An upgraded outpost is active only while all of these are true:
- owner is online, not spectator and in the same dimension;
- owner is within 64 blocks of the outpost Barrel;
- the Barrel chunk is already loaded and still contains the registered Barrel;
- `mayInteract` succeeds;
- the physical Bed/Campfire/Crafting/Furnace structure is still present.

No chunk force-loading is added. Breaking a required camp block disables the benefits immediately; rebuilding it reactivates the already-upgraded outpost.

### Extended field logistics
- Ordinary field depot: 32-block supply radius.
- Active outpost depot: 64-block supply radius.
- Bulk Construction and irrigation still consume real player/Barrel stacks, player-first and nearest-depot-first.
- Existing placement protection, material rollback, queue/tick limits and crop safety remain unchanged.

### Natural hostile safe zone
While the owner is actively using the outpost, natural hostile spawning within 24 blocks of the outpost is canceled server-side.
- Only `NATURAL` hostile spawns are suppressed.
- `TRIGGERED` spawns are deliberately untouched, so Regional Incidents, Apex Hunts and the Ascension Trial still attack normally.
- Spawners, command/event spawns and explicit encounter mobs are not converted into a free safe-zone exploit.

`/ascension stats` now reports registered/usable depots plus upgraded/active outposts.

No new packet schema was added; the existing `InfrastructureActionPayload(projectId, action)` carries the outpost upgrade action, so network protocol remains `8`.

## 0.29 — Physical Field Depots
- Register a real vanilla Barrel within4 blocks for one field-supply charge; max3/player and one owner per physical position.
- Same-dimension loaded Barrel stock supplies bulk Construction and irrigation within32 blocks.
- Player inventory is consumed first; linked Barrels are nearest-first.
- `mayInteract` is rechecked and linked chunks are never force-loaded.
- Missing loaded Barrels prune stale links. Construction/replant roll back newly placed blocks if post-place material consumption unexpectedly fails.
- `field_depots_v1` is independent SavedData.

## 0.28 — Industrial Works / Four-line Production
- Stage-1 `산업 가공소`: Stone Bricks1024 + Iron512 + Copper512 + Redstone256 + Amethyst128.
- Four atomic large-batch lines: Raw Iron/Copper/Coal, logs/cobblestone/iron, crops, and redstone/amethyst/gold/quartz.
- `production_v1` stores per-player line buffers, lifetime cycles and supply charges.
- One cycle requires one batch from all four lines. Buffers and supply charges are capped at3.
- One charge may be dispatched as Gold32 + Amethyst16 + Echo2, used to register a depot, or invested toward an outpost upgrade.

## 0.27 — Apex Hunts
- Stage-1 `정점 추적소`: Iron512 + Gold256 + Amethyst256 + Echo32 + Nether Star1.
- Re-select it inside a completed expedition region to start that region's 90-second Apex Hunt for Echo8 + Amethyst32 + Gold32.
- Nine regional bosses use separate behavior identities: charge, reinforcements, poison/heal field, skirmish repositioning, pull, leap, frost, wither pressure and levitation/void pressure.
- `apex_hunt_v1` stores first defeats and total victories per player. First defeat of all nine grants a one-time Mythic III/endgame resource package.
- Stage-2 Ascension Trial remains the stronger deterministic Mythic III source.

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
- Mythic III gear can be awakened once from exactly3 affixes to4 affixes with large resource costs.

## External references
- MineColonies is reference-only for 0.30's high-level idea that a forward settlement should combine physical facilities, supply and local defense. Current public releases are GPLv3; no MineColonies source, blueprints, citizens, building data, UI, assets, research, raid code or namespaces are copied.
- Create's current repository license split remains code MIT / `src/main/resources/assets/` All Rights Reserved. 0.28–0.30 use only high-level throughput/logistics concepts; no Create logistics implementation/assets/data are bundled.
- Building Gadgets 2 remains the MIT reference for material-backed protected Construction behavior; field depot/outpost storage resolution is independent Survival Ascension code.
- Other permissive adaptations and reference-only projects are documented in `THIRD_PARTY_NOTICES.md` and packaged notices.

Main radial: Skills / Mining / Construction / Equipment / Infrastructure / Guide / Close. Shift remains the precision override for scaled work.
