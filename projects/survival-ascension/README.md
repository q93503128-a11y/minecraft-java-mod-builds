# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension turns progression into larger physical actions, then scales enemies, world stages, exploration goals and shared infrastructure back against that growth.

## 0.25.0-alpha.1 — Randomized Multi-task Field Directives
0.24 made expeditions require real in-region actions. 0.25 removes the next repetition problem: each newly discovered region now assigns one of two persistent field directives per player, and mixed directives require every task in the directive.

### 18 persistent directives
Each of the nine expedition regions has a standard directive and a mixed alternative.

- Woodland: `거목 정리` = natural-tree logs 96, or `수림 개척` = logs 64 + legitimate travel 240.
- Arid: `전초 건설` = scaled Construction placements 128, or `사막 보급로` = placements 96 + travel 240.
- Wetland: `습지 수확` = mature crops 96, or `습지 정비` = crops 64 + hostile kills 8.
- Highlands: `능선 횡단` = travel 600, or `능선 돌파` = travel 360 + successful dashes 12.
- Ocean: `해양 항로` = swimming/water/vessel travel 800, or `심해 순찰` = voyage 500 + hostile kills 8.
- Deep: `심층 채굴` = pickaxe blocks 192, or `심층 개척` = pickaxe blocks 128 + hostile kills 10.
- Frozen: `설원 횡단` = travel 600, or `빙설 돌파` = travel 360 + successful dashes 12.
- Nether: `네더 토벌` = hostile kills 24, or `네더 보급전` = hostile kills 16 + pickaxe blocks 96.
- End: `엔드 토벌` = hostile kills 32, or `공허 전진` = hostile kills 20 + legitimate travel 360.

New discoveries choose one option randomly and persist it in the existing `expedition_v1` SavedData. The directive is not rerolled by leaving/re-entering a biome or restarting the server.

### Real-action task sources
Directive progress reuses actual Survival Ascension gameplay hooks rather than inventory checks or generic button counters.
- Logs: successful smart-tree queued natural-log breaks.
- Construction: successful material-backed/protection-checked scaled placements.
- Crops: actual mature harvest events, including tick-drained secondary harvests.
- Travel: the existing legitimate sprint-distance tracker; riding, flight, elytra, swimming and teleport-like movement are excluded.
- Ocean voyage: separate water/swimming/vessel displacement tracker with sanity limits.
- Mining: valid pickaxe block breaks, including scaled/bore work through the normal destroy path.
- Combat: hostile `Enemy` kills only.
- Dash: only successful R dash activations after server cooldown/air-dash validation.

`/ascension stats` shows discovered/completed counts plus each active directive name and every task's current/required value.

### 0.24 save migration
`expedition_v1` remains the data ID.
- Existing 0.24 discovered regions are assigned their standard directive so old progress remains meaningful.
- Existing 0.24 single-objective progress migrates into the standard directive's first task.
- Existing completed regions remain completed.
- Existing 0.23/0.24 region XP reward state remains authoritative, preventing duplicate skill XP.
- A player who already owned the nine-region master milestone remains fully completed and keeps Field Mastery.

## Field Mastery remains the final Lv.100 physical layer
Complete all nine region directives at Stage 2:
- Mining Quarry tunnel `7x7x10 -> 7x7x12`, still 12 blocks/player/tick and 64 globally.
- Woodcutting `384 -> 448` natural-tree logs, still 12/player and 64/global.
- Harvesting `11x11 -> 13x13`, tick-drained 12/player and 64/global.
- Combat Academy shockwave `6.5/16 -> 7.5/20`, same 55% fraction and 50-tick cooldown.
- Construction line `49 -> 65`, plane `11x11 -> 13x13`; volume remains `7x7x7` and real-material/protection checks remain.
- Mobility Stage-2 Nexus air dashes `3 -> 4`; landing reset and normal cooldown remain.

## Existing Stage-2 loop
- Complete the Ascension Nexus, then re-select it from `M -> Infrastructure` to open an Ascension Trial.
- Entry consumes 32 Echo Shards + 64 Amethyst Shards + 8 Dragon's Breath.
- Four waves, 60 seconds per wave, 5-second setup between waves, randomized `쇄도 / 추격 / 봉쇄` doctrine and one bounded mid-wave reinforcement.
- Completion guarantees one Mythic III affix item, 2 Netherite Scraps, 4 Diamonds and 200 XP; nearby helpers receive XP without duplicating owner loot.
- Valid Mythic III gear can be awakened once into four-affix Awakened Mythic gear through the existing Equipment radial and expensive endgame materials.

### Expedition milestone rewards
- Complete four Stage-0 directives: 4 Diamonds + 16 Emeralds + 32 Amethyst Shards.
- Stage 1, seven completed including Deep and Nether: 2 Netherite Scraps + 16 Diamonds + 32 Echo Shards.
- Stage 2, all nine completed: guaranteed Mythic III + 4 Netherite Scraps + 64 Echo Shards + 16 Dragon's Breath + 500 XP + Field Mastery.

The multi-task quest structure is independently implemented after studying FTB Quests' multi-task/progression concepts and Bountiful's variable contract/reward philosophy. FTB Quests is All Rights Reserved and Bountiful is GPL-3.0; both are reference-only here. No source, assets, quest data, UI or namespaces from either project are bundled. citeturn766979search1turn766979search0

M main radial: Skills / Mining / Construction / Equipment / Infrastructure / Guide / Close. Shift remains the precision override for scaled work.
