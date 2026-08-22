# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension turns progression into larger physical actions, then makes enemies, loot and resource sinks scale back against that growth.

## 0.16.0-alpha.1

### Six skills + infrastructure
- Mining: 3x3 / 5x5 / 7x7 / 9x9, connected veins, Lv.90 Extract, Quarry Network 5x5x8 Tunnel.
- Woodcutting: 16 / 48 / 128 / 256 natural-tree logs with Veinminer++ MIT smart-tree safety and tick-drained work.
- Harvesting: 3x3 / 5x5 / 7x7 / 9x9 mature crops; Irrigation Works enables real-seed-cost replanting.
- Combat: damage growth + hostile melee cleave 2 / 4 / 8; Combat Academy + Lv.90 upgrades a sprint melee hit into a 360-degree shockwave.
- Construction: line / wall / floor; Builder Foundry + Lv.90 adds material-backed 5x5x5 Volume.
- Mobility: sprint-distance progression, R dash, air dash and endgame traversal.

### Tactical warbands
Warband 0.16 is adapted from the MIT-licensed Warband tactical-squad model.
- When a nearby player's six-skill average is at least 30, naturally spawned hostile mobs can occasionally form a 3-6 member squad.
- Spawner-marked mobs are excluded from warband formation.
- Roles: Leader, Bruiser, Hunter and Support.
- Squad members share a player target; Bruisers lunge, Hunters reposition, Supports heal wounded squad members.
- Killing the Leader routes surviving squad members for 160 ticks (8 seconds).
- A player who kills the Leader receives 1-4 Echo Shards based on Combat level.
- Warband membership/role/rout state is stored in persistent entity data; elite ranks can coexist with warband roles.

### Combat Academy
Fourth shared infrastructure project:
- 512 iron ingots
- 256 gold ingots
- 128 emeralds
- 128 redstone
- 32 echo shards

After completion, Combat Lv.90 sprint melee attacks can trigger a shockwave every 60 ticks. It replaces the normal cleave for that hit, damages up to 12 hostile targets in a 5.5-block radius for 45% of the scaled primary damage and knocks them outward.

### Existing scaled-work safety
- Woodcutting requires leaf evidence before bulk felling and drains at 12 logs/player/tick, 64 globally.
- Tunnel mining and large Construction jobs remain tick-budgeted and use normal protected break/place paths.
- Shift remains the precision override for scaled work.

### UI
M main radial: Skills / Mining / Construction / Equipment / Infrastructure / Guide / Close. Infrastructure now includes Quarry Network / Irrigation Works / Builder Foundry / Combat Academy / Status.

## Third-party policy
Permissive source/patterns are adapted with runtime notices. 0.16 adds Warband MIT attribution for tactical-squad concepts. Create remains design-reference only for infrastructure; its assets are All Rights Reserved and are not bundled. See `THIRD_PARTY_NOTICES.md`.
