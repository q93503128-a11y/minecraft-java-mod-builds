# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension turns progression into larger physical actions, then makes world stages, expeditions, infrastructure, behavior-driven enemies and long-term production consume that larger output again.

## 0.28.0-alpha.1 — Industrial Works / Four-line Production
0.28 connects the mod's large mining, woodcutting and harvesting output to a repeatable Stage-1 economy instead of letting late-game materials simply accumulate in storage.

### Industrial Works
A new shared Stage-1 infrastructure project is available from `M -> Infrastructure -> 산업 가공소`.
- Build: Stone Bricks1024 + Iron512 + Copper512 + Redstone256 + Amethyst128.
- Before completion, the nested radial contains `시설 투자`; after completion the same radial runs production.
- Existing `infrastructure_v1` is not migrated or rewritten. Industrial Works only creates new funding keys.

### Four production lines
Every batch is server-authoritative and atomic: line capacity and all inputs are checked before any item is consumed.
- `제련 배치`: Raw Iron96 + Raw Copper96 + Coal64.
- `구조재 배치`: any vanilla/log-tag logs192 + Cobblestone384 + Iron32.
- `식량 배치`: Wheat128 + Carrot64 + Potato64 + Beetroot32.
- `정밀 부품 배치`: Redstone128 + Amethyst64 + Gold32 + Quartz64.

`production_v1` is per-player SavedData. Each line has a buffer of at most3 completed batches. One industrial cycle requires one stored batch from every line; a player cannot progress the cycle by overproducing only one category.

### Supply cycle
- One complete four-line set is consumed into one `현장 보급권`.
- Supply storage is capped at3.
- If storage is full, completed line batches may wait in their buffers. Dispatching a charge immediately normalizes any waiting complete sets into newly available charge slots, avoiding a deadlocked full buffer.
- Dispatch one charge to receive Gold Ingots32 + Amethyst Shards16 + Echo Shards2.
- Output is tangible inventory/drop loot, so it can support Apex Hunts, Ascension Trials, equipment rerolls/awakening or any other normal resource sink.
- `/ascension stats` reports lifetime industrial cycles and stored supply charges.

This loop is intentionally a throughput conversion, not a permanent percentage buff. The large inputs keep mining, forestry, farming and Nether/resource collection relevant after their mastery actions become very large.

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
- Create current repository license split was rechecked for 0.28: code is MIT while files under `src/main/resources/assets/` are All Rights Reserved. Survival Ascension studies only high-level high-throughput/multi-step processing and logistics progression for 0.28; no Create assets, recipes, machine code, data, namespaces or Ponder content are copied.
- Apotheosis official GitHub `26.1` code is MIT; packaged assets/distribution rights are treated separately and no Apotheosis assets are bundled.
- Other permissive adaptations and reference-only projects are documented in `THIRD_PARTY_NOTICES.md` and packaged notices.

Main radial: Skills / Mining / Construction / Equipment / Infrastructure / Guide / Close. Shift remains the precision override for scaled work.
