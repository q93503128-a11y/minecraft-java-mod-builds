# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension turns progression into larger physical actions, then makes world stages, expeditions, infrastructure, behavior-driven enemies, production, logistics and physical field bases consume that larger output again.

## 0.31.0-alpha.1 — Death-bound Field Recovery
0.31 gives physical outposts a real expedition-recovery role without turning them into ordinary waystones or free fast travel.

### Prepaid one-use recovery contract
From `M -> Infrastructure -> 산업 가공소 -> 현장 복귀 계약`:
- Stand within 4 blocks of one of your **active** outposts.
- First arming consumes 1 stored field-supply charge in advance.
- The contract stores exactly one outpost recovery point in independent `field_recovery_v1`.
- If a paid contract is already armed, moving it to another active outpost is free; this moves the already-paid one-use token instead of creating another charge.
- Selecting the same outpost again only reports that it is already armed.

### Death qualification
A recovery is queued only when all of these are true at death time:
- the player has an armed one-use contract;
- death happens in the same dimension as the armed outpost;
- death is within 96 blocks of that outpost anchor;
- the outpost record still exists and the real Barrel/camp structure is operational and interactable;
- the player is **not** currently in a Regional Incident, Apex Hunt or Ascension Trial.

Incident/Apex/Trial deaths do not consume the contract, so field recovery cannot become a boss-fight extra life. Ordinary movement and exploration are also untouched: there is no button that teleports a living player to an arbitrary outpost.

### Respawn recovery and failure safety
After a qualifying death the prepaid token moves from `armed` to `pending`. On respawn the server attempts to return the player to a safe standing position around the target outpost.
- The destination dimension and outpost are resolved server-side from saved state.
- The destination chunk must already be loaded; no chunk ticket or force-load is added.
- Barrel, camp structure and `mayInteract` checks are repeated.
- The arrival scan requires a sturdy floor, empty body/head collision spaces and no fluid.
- The token is consumed only **after** `ServerPlayer.teleportTo` reports success.
- Failed destination validation or teleport preserves the pending token.
- A pending token can be retried. If its old target is unavailable and the player stands within 4 blocks of another active outpost, it can be rearmed there without another supply-charge cost.
- Successful recovery clears motion/fall state and increments a lifetime recovery counter.

`/ascension stats` and Industrial Works status now report `미설정 / 계약 준비 / 복귀 대기` plus lifetime successful recoveries.

No new packet schema was added; the existing string-based `InfrastructureActionPayload(projectId, action)` carries the recovery action, so network protocol remains `8`.

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
- One charge may be dispatched as Gold32 + Amethyst16 + Echo2, used to register a depot, or invested toward an outpost/recovery contract.

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
- Waystones (`TwelveIterations/Waystones`) current 26.2 branch is All Rights Reserved. 0.31 studies only the product tradeoff around return/teleport convenience and deliberately rejects always-available outpost fast travel. No Waystones code, blocks, items, menus, data, assets or namespaces are copied.
- Corpse (`denmeh/Corpse`) is LGPL-3.0. 0.31 studies only the high-level goal of reducing repetitive death-recovery travel; no Corpse source, corpse entity/container, inventory-storage logic, assets, data or namespace are copied.
- MineColonies remains GPLv3 reference-only for 0.30's physical forward-base product lesson; no implementation/content is copied.
- Create's current repository license split remains code MIT / `src/main/resources/assets/` All Rights Reserved. 0.28–0.30 use only high-level throughput/logistics concepts; no Create logistics implementation/assets/data are bundled.
- Building Gadgets 2 remains the MIT reference for material-backed protected Construction behavior; field depot/outpost storage resolution is independent Survival Ascension code.
- Other permissive adaptations and reference-only projects are documented in `THIRD_PARTY_NOTICES.md` and packaged notices.

Main radial: Skills / Mining / Construction / Equipment / Infrastructure / Guide / Close. Shift remains the precision override for scaled work.
