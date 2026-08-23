# Survival Ascension

- Mod version: `0.28.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Network protocol: `8`
- Existing-world compatibility: `mining_progress_v1`, `infrastructure_v1`, `world_ascension_v1`, `expedition_v1`, `apex_hunt_v1`, Elite/Warband/mutation persistent NBT, affix CustomData and mining modes remain intact. 0.28 adds new independent per-player `production_v1` and a new Industrial Works funding key under the unchanged `infrastructure_v1` map.

## Core direction
Progression must enlarge physical actions rather than only percentages. Larger actions create larger material throughput; world stages, behavior-rich enemies, exploration goals, infrastructure and 0.28 production must consume that throughput again. Shift remains the precision/single-action safety override.

## 0.28 Industrial Works
### Infrastructure gate
`INDUSTRIAL_WORKS / 산업 가공소` is a Stage1 shared project.
- Stone Bricks1024
- Iron Ingots512
- Copper Ingots512
- Redstone256
- Amethyst Shards128

`M -> Infrastructure -> 산업 가공소` opens a nested MineMenu-style radial. The submenu contains facility funding, four production batches, supply dispatch, status and back navigation. No new generic rectangular machine GUI is added.

### Four batch lines
All batch requests are server validated. Capacity and every material count are checked before consumption, so a failed request cannot partially eat inputs.

1. `METALWORKS / 제련 배치`: Raw Iron96 + Raw Copper96 + Coal64.
2. `TIMBERWORKS / 구조재 배치`: any `ItemTags.LOGS`192 + Cobblestone384 + Iron Ingot32.
3. `PROVISIONS / 식량 배치`: Wheat128 + Carrot64 + Potato64 + Beetroot32.
4. `PRECISION / 정밀 부품 배치`: Redstone128 + Amethyst64 + Gold32 + Quartz64.

The lines deliberately pull from mining, woodcutting, farming and precision/Nether-resource categories. One abundant category cannot complete the cycle alone.

### `production_v1`
Per-player SavedData fields:
- uuid
- metalworks buffer
- timberworks buffer
- provisions buffer
- precision buffer
- lifetime cycles
- supply_charges

All numeric fields default0. Each line buffer is clamped0..3 and supply charges0..3. Existing worlds simply start with no production entry.

### Cycle normalization
- Adding a batch increments only its matching line after capacity/material validation.
- While supply charge storage has free space and all four buffers are >0, exactly one from every buffer is consumed, `cycles` increments and one supply charge is created.
- This uses a loop rather than a one-shot check so multiple already-complete sets normalize correctly.
- If charges are full, complete sets may wait in buffers.
- Dispatch decrements one supply charge and immediately normalizes waiting complete sets into the newly freed charge capacity. This prevents a state where all four buffers are full but no new action can free them.

### Tangible dispatch economy
One supply charge dispatch grants:
- Gold Ingots32
- Amethyst Shards16
- Echo Shards2

Inventory-full fallback drops the stack normally. Output is deliberately generic vanilla material rather than a hidden Apex-only coupon so the player decides whether to support Apex Hunts, Ascension Trials, equipment rerolls/awakening or another resource sink. The full four-line input remains substantially larger than the output, so this is throughput conversion and a renewable Echo path, not ore multiplication.

`/ascension stats` reports lifetime production cycles and currently stored supply charges.

## 0.27 Apex Hunts retained
- Stage1 Apex Tracking Post build: Iron512 + Gold256 + Amethyst256 + Echo32 + Nether Star1.
- Hunt entry from completed expedition region: Echo8 + Amethyst32 + Gold32.
- 90-second owner-scoped encounter, 10-second invalid-owner grace, 64-block owner radius, 48-block escort recall, 96-block hunt separation, logout/failure/orphan cleanup.
- Nine patterns: CHARGE / REINFORCE / PLAGUE / SKIRMISH / PULL / LEAP / FROST / WITHER / VOID.
- `apex_hunt_v1` tracks nine first kills, total victories and one-time 9/9 reward.
- Stage2 Trial still guarantees Mythic III and remains the strongest deterministic loot route.

## Expeditions retained
- Nine regions with two persistent directives each, 18 total.
- 18 rare incidents: one ambush + one action rush per region.
- Directive progress only comes from authoritative real gameplay hooks.
- Incident reward is once per player/region; directive bonus max20% of first unfinished task.
- 0.23/0.24/0.25 migration and reward bits remain intact.

## Mastery VI / Field Mastery retained
Base Lv100:
- Mining11x11, vein/extract192, Quarry7x7x10.
- Woodcutting384; queues12/player,64/global.
- Harvest11x11; queues12/player,64/global.
- Combat cleave10/r5/70%, Academy shockwave6.5/16/55%/50t.
- Construction line49, plane11x11, volume7x7x7.
- Mobility step2, safe fall16, dash1.80/16t, Stage2 Nexus air dash3.

After Stage2 + all nine expedition directives, at Lv100 only:
- Quarry depth12/pending640, still12/player and64/global.
- Woodcutting448.
- Harvest13x13.
- Academy shockwave7.5/20.
- Construction line65/plane13x13, volume still7^3.
- Air dash4.

## Endgame retained
- World stage0 Awakening -> first Wither Stage1 Legendary -> first Ender Dragon Stage2 Endgame.
- Stage2 mutation subset: Withered / Phase / Plague.
- Elite ranks, affixes, Warbands and Apex behavior can layer through their existing bounded contracts.
- Ascension Nexus unlocks repeatable four-wave doctrine Trial.
- Valid Mythic III with exactly3 known affixes can become four-affix Awakened Mythic once; awakened rerolls preserve4.

## Safety contracts
- Large mining/wood/farm/construction work remains tick-budgeted.
- Secondary destruction goes through normal `player.gameMode.destroyBlock`.
- Construction/replant consume real resources and preserve interaction/protection hooks.
- Production is per-player, atomic and bounded: line buffers3, supply charges3.
- No new packet schema in0.28; `InfrastructureActionPayload(projectId, action)` carries production action strings and protocol remains8.

## External-source policy
- Create (`Creators-of-Create/Create`): current `LICENSE.md` says code is MIT and files under `src/main/resources/assets/` are All Rights Reserved. 0.28 studies the product-level high-throughput, multi-step processing/logistics progression only. No Create assets, recipes, machines, Ponder data, namespaces or source implementation are copied into the production loop.
- Apotheosis official GitHub `26.1` code license is MIT; asset/distribution rights are treated separately.
- Existing packaged MIT/CC0 notices and reference-only restrictions remain in `THIRD_PARTY_NOTICES.md`.
