# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension turns progression into larger physical actions, then scales enemies, world stages, exploration goals and shared infrastructure back against that growth.

## 0.27.0-alpha.1 — Apex Tracking Post + Behavior-driven Apex Hunts
0.27 fills the main remaining combat gap after 0.26: the player had large-scale progression, tactical warbands and repeatable trials, but no region-linked mini-boss hunt loop with clearly different behavior patterns.

### Apex Tracking Post
A new Stage-1 shared infrastructure project is available from `M -> Infrastructure`.
- Build cost: Iron 512 + Gold 256 + Amethyst 256 + Echo Shards 32 + Nether Star 1.
- Once complete, re-select the post while standing inside an already completed expedition region to start that region's Apex Hunt.
- Repeat hunt cost: Echo Shards 8 + Amethyst Shards 32 + Gold Ingots 32.
- Hunts cannot be started by creative/spectator players.
- One hunt lasts 90 seconds, fails after 10 seconds of owner death/region/distance invalidity, uses a boss bar, recalls wandering escorts and cleans tagged mobs on logout/restart.
- Active hunts are separated by 96 blocks. Starting a hunt also pushes the player's field-incident ready time past the hunt window, and the Ascension Nexus refuses to open a Trial while that player owns an Apex Hunt.

### Nine region Apex archetypes
Each expedition region has one persistent first-kill slot and one behavior identity.
- Woodland `수림 파쇄자`: Ravager-led charge hunt. A visible one-second windup precedes a high-speed charge and impact knockback.
- Arid `황야 지휘관`: Husk commander. Calls bounded reinforcements at 70% and 35% health.
- Wetland `늪지 역병핵`: Zombie plague-heart. Periodic close-range poison field also heals the boss.
- Highlands `능선 사냥꾼`: Stray skirmisher. Repeated lateral/reposition impulses punish static melee chasing.
- Ocean `심해 압제자`: Elder Guardian with Guardian escorts. Periodically pulls the owner back into beam range.
- Deep `심층 추적자`: Spider stalker. Repeated long leaps close medium distance.
- Frozen `빙설 감시자`: Stray control hunt. Periodic cold field applies strong short Slowness.
- Nether `네더 약탈자`: Wither Skeleton with Blaze/Wither Skeleton escorts. Periodic close-range Wither pulse and displacement.
- End `공허 전조자`: Enderman with Shulker/Enderman escorts. Periodic Levitation pulse while the boss aggressively closes distance.

Boss durability/armor/attack additions are archetype-specific rather than one blanket multiplier. The point of the tier is telegraphed behavior + escort composition, not only a larger health number.

### Rewards and long-term sink
- Every victory gives an affix item and materials while consuming the repeat-entry resources above.
- Stage 1: Ascended II gear + Diamonds 2 + Echo Shards 4 + XP 120.
- Stage 2: Ascended II gear, with 20% Mythic III chance + Diamonds 3 + Echo Shards 6 + Netherite Scrap 1 + XP 180.
- Nearby helpers inside 48 blocks receive XP 50 without duplicating owner loot.
- `apex_hunt_v1` stores per-player first defeats and total victories.
- First defeat of all nine regional Apex archetypes grants guaranteed Mythic III + Netherite Scrap 4 + Echo Shards 32 + Dragon's Breath 16 + XP 500.
- `/ascension stats` reports `Apex first defeats /9` and total hunt victories.

The repeat reward is deliberately below the Stage-2 Ascension Trial's guaranteed Mythic III so the Trial remains the stronger deterministic endgame loot source.

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

`/ascension stats` reports discoveries, completed directives, resolved incidents, active directive task progress, Apex first defeats and total Apex victories.

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
- Silent Gear: current CurseForge project is MIT and was studied for the high-level idea that long-lived gear progression should retain identity while resources continue to matter. 0.27 does not copy its material/part/blueprint implementation; Survival Ascension instead uses a hunt-driven affix reward loop.
- Apotheosis: the official `Shadows-of-Fire/Apotheosis` GitHub `26.1` branch contains an MIT LICENSE for code. CurseForge currently labels the distribution page All Rights Reserved, so Survival Ascension treats source-code adaptation and packaged assets/distribution rights separately; no Apotheosis assets are bundled.
- Enhanced Celestials Tweaks (MIT): event lifecycle reference only for 0.26.
- Majrusz's Progressive Difficulty: rare forced encounter pacing reference-only; no code/assets copied without a clear reusable license.
- FTB Quests and Bountiful remain reference-only for persistent multi-task/contract structure.
- Lootr remains a design reference for per-player exploration-reward fairness.
- Gateways to Eternity, Skill Proficiencies, Veinminer++, MineMenu, Building Gadgets 2, Mob Champions, Mekanism and Warband retain their existing packaged notices where code patterns were permissively adapted.

M main radial: Skills / Mining / Construction / Equipment / Infrastructure / Guide / Close. Shift remains the precision override for scaled work.
