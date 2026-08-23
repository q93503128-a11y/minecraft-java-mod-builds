# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension turns progression into larger physical actions, then scales enemies, world stages, exploration goals and shared infrastructure back against that growth.

## 0.26.0-alpha.1 — Rare Regional Field Incidents
0.25 made each expedition region use one of two persistent multi-task directives. 0.26 adds short rare incidents so an expedition can change while the player is actually doing it instead of remaining only a checklist.

### Incident cadence and safety
- Every 30 seconds, an eligible player inside a discovered expedition region gets a 10% rare-incident roll.
- A successful incident reward is permanent and limited to once per player per region through optional `incident_rewards` bits inside the existing `expedition_v1` SavedData.
- Failed incidents do not erase directive progress and may occur again after cooldown.
- One incident lasts 45–60 seconds and uses a boss bar for remaining enemies/action progress/time.
- Leaving the incident region or moving more than 48 blocks from its start for 10 seconds fails it.
- Creative/spectator players cannot start or progress incidents.
- Incident checks are excluded around the Ascension Trial start window so the two encounter systems do not stack on top of each other.

### 18 region incidents
Each region has one ambush and one action-rush incident.

- Woodland: `수림 습격` or `벌목 비상` (24 natural logs).
- Arid: `약탈대 급습` or `긴급 보급선` (24 scaled placements).
- Wetland: `늪지 습격` or `긴급 수확` (20 mature crops).
- Highlands: `능선 매복` or `능선 돌파` (4 successful dashes).
- Ocean: `익사자 습격` or `폭풍 항해` (180 water/vessel travel).
- Deep: `심층 군집` or `붕괴 전 채굴` (48 valid pickaxe blocks).
- Frozen: `설원 습격` or `빙설 강행군` (180 legitimate travel).
- Nether: `네더 급습` or `열기 속 채굴` (48 valid pickaxe blocks).
- End: `공허 습격` or `공허 추적` (180 legitimate travel).

Ambushes use bounded triggered vanilla spawns near the player. Ocean ambushes use water spawn slots. Incident mobs are cleaned on failure/logout rather than becoming a permanent event army.

### One-time incident rewards
Resolving a region's incident for the first time grants:
- Stage 0 region: relevant skill XP +100, Emeralds 4, Amethyst Shards 8.
- Stage 1 region: relevant skill XP +150, Diamonds 2, Echo Shards 4.
- Stage 2 region: relevant skill XP +200, Diamonds 4, Echo Shards 8.
- If the region directive is still incomplete, the first unfinished directive task also gains up to 20% of that task's target.

The 20% bonus is a one-time event reward, not a repeatable shortcut. Normal region completion XP and milestone rewards remain independently one-time.

## 0.25 expedition directives
Each of the nine expedition regions has two persistent directives; a new discovery randomly receives one option and keeps it across re-entry/restart. Existing 0.24 discoveries migrate to their standard directive.

Examples:
- Woodland: logs96, or logs64 + travel240.
- Highlands/Frozen: travel600, or travel360 + successful dashes12.
- Deep: mining192, or mining128 + hostile kills10.
- Nether: hostile kills24, or hostile kills16 + mining96.
- End: hostile kills32, or hostile kills20 + travel360.

Every task reuses actual Survival Ascension action hooks: successful smart-tree work, protected/material-backed scaled construction, mature crop breaks, legitimate traversal, water/vessel voyage, valid pickaxe destruction, hostile `Enemy` kills and successful server-validated dashes.

`/ascension stats` reports discoveries, completed directives, resolved incidents and active directive task progress.

## Field Mastery remains the final Lv.100 physical layer
Complete all nine region directives at Stage 2:
- Mining Quarry tunnel `7x7x10 -> 7x7x12`, still 12 blocks/player/tick and 64 globally.
- Woodcutting `384 -> 448` natural-tree logs, still 12/player and 64/global.
- Harvesting `11x11 -> 13x13`, tick-drained 12/player and 64/global.
- Combat Academy shockwave `6.5/16 -> 7.5/20`, same 55% fraction and 50-tick cooldown.
- Construction line `49 -> 65`, plane `11x11 -> 13x13`; volume remains `7x7x7` with real materials/protection hooks.
- Mobility Stage-2 Nexus air dashes `3 -> 4`; landing reset and normal cooldown remain.

## Existing Stage-2 loop
- World Ascension: Awakening 0 -> first Wither Legendary 1 -> first Ender Dragon Endgame 2.
- Stage 2 natural hostile subsets can gain Withered / Phase / Plague mutations; Elite ranks and Warband roles can stack independently.
- Complete the Ascension Nexus, then re-select it from `M -> Infrastructure` to open an Ascension Trial.
- Trial entry: 32 Echo Shards + 64 Amethyst Shards + 8 Dragon's Breath.
- Four waves, 60 seconds each, randomized `쇄도 / 추격 / 봉쇄` doctrine and one bounded mid-wave reinforcement.
- Trial completion guarantees Mythic III gear; valid Mythic III can be awakened once into four-affix Awakened Mythic gear through the Equipment radial.

## External design references
- Enhanced Celestials Tweaks (MIT): studied the high-level idea that one temporary event can bundle spawn changes, duration, rewards and lifecycle rules. 0.26 uses its own region incidents, vanilla mobs, boss bar, persistence and reward logic; no source/assets/config data are bundled.
- Majrusz's Progressive Difficulty: Undead Army-style rare forced encounter pacing is reference-only. The current public repository root does not expose a license file, so no source/assets/data are copied.
- FTB Quests and Bountiful remain reference-only for persistent multi-task/contract structure.
- Lootr remains a design reference for per-player exploration-reward fairness.
- Gateways to Eternity, Skill Proficiencies, Veinminer++, MineMenu, Building Gadgets 2, Mob Champions, Apotheosis, Mekanism and Warband retain their existing packaged notices where code patterns were permissively adapted.

M main radial: Skills / Mining / Construction / Equipment / Infrastructure / Guide / Close. Shift remains the precision override for scaled work.
