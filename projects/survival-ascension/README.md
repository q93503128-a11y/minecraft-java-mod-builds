# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension turns progression into larger physical actions, then makes the world, loot and resource sinks scale back against that growth.

## 0.15.0-alpha.1

### Six skills
- Mining: 3x3 / 5x5 / 7x7 / 9x9, connected veins, Lv.90 Extract, Quarry Network 5x5x8 Tunnel.
- Woodcutting: 16 / 48 / 128 / 256 connected natural-tree logs. 0.15 adds Veinminer++ MIT smart-tree leaf safety and tick-drained work.
- Harvesting: 3x3 / 5x5 / 7x7 / 9x9 mature crops; Irrigation Works enables real-seed-cost replanting.
- Combat: damage growth + hostile melee cleave 2 / 4 / 8.
- Construction: line / wall / floor; Builder Foundry + Lv.90 adds material-backed 5x5x5 Volume.
- Mobility: sprint-distance progression, R dash, air dash and endgame traversal.

### Woodcutting safety
Bulk felling first gathers the connected log set. It only starts if leaves are face-adjacent to the origin or one of the gathered logs. Plain log buildings therefore stay single-block unless they are deliberately built as leaf-attached tree structures.

Large trees no longer break synchronously. Jobs drain at 12 logs/player/tick and 64 globally while every log still uses the normal player destroy controller, preserving durability, drops and per-block Woodcutting XP.

### Shared infrastructure
M -> Infrastructure is shared by the entire server world and persists in `infrastructure_v1` SavedData.
- Quarry Network: 1024 cobblestone / 256 iron / 128 redstone / 32 diamonds -> Mining Lv.90 Tunnel 5x5x8.
- Irrigation Works: 512 copper / 128 iron / 128 redstone / 128 glass / 32 slimeballs -> Harvesting Lv.30 protected seed-backed replant.
- Builder Foundry: 1024 stone bricks / 256 iron / 256 copper / 128 redstone / 64 obsidian -> Construction Lv.90 Volume 5x5x5.

Volume construction uses the existing Building Gadgets 2-inspired Survival Ascension queue: real inventory blocks, `mayInteract`, NeoForge placement hook, survival checks, max 256 pending/player and tick-distributed placement.

### UI
M main radial remains seven top-level items: Skills / Mining / Construction / Equipment / Infrastructure / Guide / Close. Guide contains Unlocks / Stats / Controls tabs.

## Third-party policy
Permissive source/patterns are adapted with runtime notices. 0.15 specifically extends the existing Veinminer++ MIT reuse to smart-tree safety and tick-drained tree work. Create remains design-reference only for infrastructure; its assets are All Rights Reserved and are not bundled. See `THIRD_PARTY_NOTICES.md`.
