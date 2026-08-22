# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension turns survival progression into larger physical actions, then scales enemies, loot and resource sinks back against that growth.

## 0.14.0-alpha.1

### Six live skills
- Mining: 3x3 / 5x5 / 7x7 / 9x9 excavation, connected veins, Lv.90 Extract, and infrastructure-gated tunnel boring.
- Woodcutting: connected-log felling 16 / 48 / 128 / 256.
- Harvesting: mature-crop 3x3 / 5x5 / 7x7 / 9x9 plus infrastructure-gated seed-backed replanting.
- Combat: damage growth + hostile melee cleave 2 / 4 / 8 targets.
- Construction: M -> Construction -> Single / Line / Wall / Floor; line 5 / 9 / 17 / 33 and planes 3x3 / 5x5 / 9x9.
- Mobility: sprint-distance XP, Lv.30 R dash, Lv.60 air dash, Lv.90 endgame traversal.

### World-shared infrastructure
M -> Infrastructure opens a MineMenu-derived shared-project radial. Funding is persisted in server SavedData and every player contributes to the same world progress.

- Quarry Network: 1024 cobblestone + 256 iron + 128 redstone + 32 diamonds.
  - Completion + Mining Lv.90 unlocks Tunnel mode.
  - Tunnel processes a 5x5 cross-section for 8 blocks of depth.
  - Work is distributed across server ticks: 12 targets/player/tick, 64 globally, max 256 pending/player.
  - Secondary breaks still use the normal player destroy controller and cannot recursively schedule another tunnel job.
- Irrigation Works: 512 copper + 128 iron + 128 redstone + 128 glass + 32 slimeballs.
  - Completion + Harvesting Lv.30 automatically replants wheat, carrots, potatoes, beetroot and nether wart after harvest.
  - Replant consumes one real seed/crop item, checks loaded chunks, placement permission and the NeoForge placement hook, and never creates free seeds.

### Mining modes
M -> Mining: Auto / Plane / Vein / Extract / Tunnel.
- Auto: valuable ore uses connected vein extraction; normal blocks use excavation.
- Plane (Lv.10): view-aligned excavation.
- Vein (Lv.30): connected same-family valuable ore only.
- Extract (Lv.90): bounded 12-block loaded-area search for the same ore family even when deposits are disconnected.
- Tunnel (Lv.90 + Quarry Network): 5x5x8 tick-budgeted bore.
- Shift always overrides scaled mining back to one block.

### Reactive elites + affix equipment economy
- Elite I / Ascended II / Mythic III scale from player progression and use Swift/Bulwark/Vampiric/Berserker reactive patterns.
- Elite gear rolls 1 / 2 / 3 affixes from category-specific five-affix pools.
- M -> Equipment provides server-authoritative Reforge / Salvage / Gear Info.
- Mythic reforge consumes 64 amethyst + 12 diamonds + 2 netherite scraps.

### UI / controls
- M main radial is kept to seven top-level entries: Skills / Mining / Construction / Equipment / Infrastructure / Guide / Close.
- Unlocks, Stats and Controls stay inside the Guide tabs instead of crowding the main wheel.
- R: Mobility dash/action.
- Shift: precision override for scaled work.

## Third-party policy
MIT code/patterns may be adapted with notices preserved. Runtime notices cover Skill Proficiencies, Veinminer++, MineMenu, Building Gadgets 2, Mob Champions, Apotheosis and Mekanism. Create code is MIT but its assets are All Rights Reserved; 0.14 uses only high-level staged-infrastructure/resource-throughput design reference and bundles no Create code or assets. Restricted/unadopted projects remain reference-only. See `THIRD_PARTY_NOTICES.md`.
