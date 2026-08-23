# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension turns progression into larger physical actions, then makes world stages, expeditions, infrastructure, behavior-driven enemies, production and field logistics consume that larger output again.

## 0.29.0-alpha.1 — Physical Field Depots
0.29 makes the 0.28 industrial cycle affect actual world work instead of stopping at inventory rewards.

### Register a real barrel as a field depot
From `M -> Infrastructure -> 산업 가공소 -> 물류 거점 연결`:
- Stand within 4 blocks of a vanilla Barrel.
- Registering an unlinked barrel consumes one stored `현장 보급권`.
- Selecting one of your already-linked nearby barrels removes that link; the charge is not refunded.
- Each player may own at most 3 linked depots.
- A barrel already claimed by another player cannot be linked again.
- Depot ownership persists independently in `field_depots_v1`.

### Real stock, real distance
A field depot is not a virtual inventory.
- Only the actual Barrel container at the saved block position can provide materials.
- The player must be in the same dimension and within 32 blocks.
- The barrel chunk must already be loaded; Survival Ascension never force-loads depot chunks.
- `mayInteract` is checked before remote material access.
- If a loaded linked barrel no longer exists, its stale link is removed automatically.
- Player inventory is always consumed first, then usable linked barrels are searched nearest-first.

### Bulk Construction logistics
Scaled Construction keeps the existing protection and placement lifecycle, but material availability now includes nearby linked depots.
- Line / wall / floor / volume jobs may continue after the carried stack runs out if a linked barrel contains the exact BlockItem.
- A successful secondary placement consumes exactly one real item from player inventory or the depot.
- If material disappearance is detected after placement, the just-added block is rolled back rather than granting a free block.
- Existing 64 global tick budget and 512 pending/player limit remain unchanged.

### Irrigation logistics
Irrigation auto-replant uses the same material source rule.
- Wheat seeds, carrots, potatoes, beetroot seeds and nether wart are taken from the player first, then a nearby linked depot.
- Protection/place hooks still run before final replant consumption.
- A failed consumption after placement rolls the young crop back.

`/ascension stats` now reports registered depots and how many are currently usable from the player's location.

No new packet schema was added. Depot registration uses the existing `InfrastructureActionPayload(projectId, action)` path, so network protocol remains `8`.

## 0.28 — Industrial Works / Four-line Production
- Stage-1 `산업 가공소`: Stone Bricks1024 + Iron512 + Copper512 + Redstone256 + Amethyst128.
- Four atomic large-batch lines: Raw Iron/Copper/Coal, logs/cobblestone/iron, crops, and redstone/amethyst/gold/quartz.
- `production_v1` stores per-player line buffers, lifetime cycles and supply charges.
- One cycle requires one batch from all four lines. Buffers and supply charges are capped at3.
- Dispatching a charge can still produce Gold32 + Amethyst16 + Echo2.
- 0.29 adds a second use for the same charge: commissioning a physical field depot.

## 0.27 — Apex Hunts
- Stage-1 `정점 추적소`: Iron512 + Gold256 + Amethyst256 + Echo32 + Nether Star1.
- Re-select it inside a completed expedition region to start that region's 90-second Apex Hunt for Echo8 + Amethyst32 + Gold32.
- Nine region bosses use separate behavior identities: charge, reinforcements, poison/heal field, skirmish repositioning, pull, leap, frost, wither pressure and levitation/void pressure.
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
- Mining Quarry tunnel `7x7x10 -> 7x7x12`, still 12 blocks/player/tick and64 globally.
- Woodcutting `384 -> 448` natural-tree logs, 12/player and64/global.
- Harvesting `11x11 -> 13x13`, 12/player and64/global.
- Combat Academy shockwave `6.5/16 -> 7.5/20`, same55% damage fraction and50-tick cooldown.
- Construction line `49 -> 65`, plane `11x11 -> 13x13`; volume remains `7x7x7` with real materials/protection hooks.
- Mobility Stage-2 Nexus air dashes `3 -> 4`.

## Existing endgame
- World Ascension: Awakening0 -> first Wither Legendary1 -> first Ender Dragon Endgame2.
- Stage2 natural hostile subsets can gain Withered / Phase / Plague mutations; Elite ranks and Warband roles can layer through normal spawn paths.
- Complete Ascension Nexus to access the repeatable four-wave doctrine Trial.
- Mythic III gear can be awakened once from exactly3 affixes to4 affixes with large resource costs.

## External references
- Create's current repository license split remains code MIT / `src/main/resources/assets/` All Rights Reserved. 0.29 studies only the product-level logistics idea of stock-backed requests and local restocking. The field depot system is independent Survival Ascension SavedData + vanilla Barrel access; no Create source, assets, machines, package formats, GUI/data, namespace or Ponder content are copied.
- Building Gadgets 2 remains the MIT reference for material-backed protected Construction behavior. 0.29 does not copy storage/network code from it; the depot material resolver is new Survival Ascension code.
- Other permissive adaptations and reference-only projects are documented in `THIRD_PARTY_NOTICES.md` and packaged notices.

Main radial: Skills / Mining / Construction / Equipment / Infrastructure / Guide / Close. Shift remains the precision override for scaled work.
