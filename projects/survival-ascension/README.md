# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension turns progression into larger physical actions, then scales enemies, world stages and shared infrastructure back against that growth.

## 0.19.0-alpha.1

### Endgame hostile mutations
World Ascension Stage 2 now adds a second hostile-combat layer adapted from Hostiles Are Too Easy's CC0 Celestial Type vocabulary.

Natural spawns only, 18% mutation roll on eligible mobs; spawner-origin mobs and babies are excluded.
- Skeletons can become `Withered`: successful player hits apply Wither for 80 ticks.
- Zombies can become `Phase`: after player health damage they have a 55% chance, while off a 45-tick reaction cooldown, to evade laterally/backward.
- Zombies can become `Plague`: successful player hits apply Poison for 120 ticks.

Mutation is stored in persistent entity NBT and can coexist with Elite I / Ascended II / Mythic III rank and Leader / Bruiser / Hunter / Support warband role. Player mutation kills grant +10 vanilla XP and have a 35% chance to drop one Echo Shard.

### World progression and endgame infrastructure
- Stage 0 Awakening: default.
- Wither kill -> Stage 1 Legendary.
- Ender Dragon kill -> Stage 2 Endgame.
- Stage increases elite frequency/rank odds and tactical warband size/frequency.
- M -> Infrastructure -> Status shows the canonical server stage.
- Stage 2 allows funding Ascension Nexus: 4 Nether Stars / 64 Dragon's Breath / 512 Obsidian / 512 Amethyst Shards / 64 Echo Shards.
- Ascension Nexus + Mobility Lv.90 upgrades air dash from one to two uses before landing while retaining the normal dash cooldown.

### Current late-game action scale
- Mining: 9x9, Extract and 5x5x8 Tunnel.
- Woodcutting: up to 256 natural-tree logs with smart-tree protection and tick draining.
- Harvesting: 9x9 plus real-seed-cost replant.
- Combat: 8-target cleave plus 5.5-radius/12-target sprint shockwave.
- Construction: 9x9 planes plus 5x5x5 Volume.
- Mobility: dash, air dash, and after Ascension Nexus two air dashes per airtime.
- Enemies: elite rank + tactical role + Stage-2 mutation layers can coexist.

M main radial remains Skills / Mining / Construction / Equipment / Infrastructure / Guide / Close.
