# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25.

Survival Ascension turns progression into larger physical actions, then makes enemies, loot, infrastructure and the world itself scale back against that growth.

## 0.17.0-alpha.1

### Boss-driven world ascension
Adapted from Hostiles Are Too Easy's CC0 dynamic-difficulty progression concept.
- Stage 0 `Awakening / 각성`: new world default.
- Kill the Wither -> Stage 1 `Legendary / 전설`.
- Kill the Ender Dragon -> Stage 2 `Endgame / 종말`.
- The stage is server-world shared and persists in `world_ascension_v1` SavedData.
- Boss advancement broadcasts to all online players.
- M -> Infrastructure -> Status shows the canonical current stage.

World stage changes organization rather than only inflating health:
- Elite chance adds +4 percentage points per stage, while keeping a hard 28% cap.
- Mythic and Ascended rank odds also increase per stage.
- Tactical-warband formation chance adds +8 percentage points per stage.
- Warband size grows from 3-6 at Stage 0, to 4-7 after the Wither, to 5-8 after the Ender Dragon.
- Existing Swift/Bulwark/Vampiric/Berserker elite traits and Leader/Bruiser/Hunter/Support squad roles remain active, so both systems can stack.

### Existing late-game loop
- Mining: up to 9x9, Extract and Quarry Network 5x5x8 Tunnel.
- Woodcutting: up to 256 natural-tree logs with smart-tree safety and tick-drained work.
- Harvesting: up to 9x9 plus real-seed-cost replant after Irrigation Works.
- Combat: cleave plus Combat Academy Lv.90 sprint shockwave.
- Construction: line/wall/floor plus Builder Foundry Lv.90 5x5x5 Volume.
- Mobility: sprint progression, R dash, air dash and endgame traversal.
- Tactical warband leaders drop Echo Shards used by Combat Academy.
- Affix equipment, reforge/salvage and shared infrastructure remain the primary resource sinks.

### UI
M main radial: Skills / Mining / Construction / Equipment / Infrastructure / Guide / Close.
Guide documents world stages; Infrastructure Status reports the actual server stage and project funding.

## Third-party policy
Permissive source/patterns are adapted with runtime notices. 0.17 adds Hostiles Are Too Easy CC0 attribution for boss-driven dynamic difficulty. No legacy HATE mixins/assets/namespaces are bundled. See `THIRD_PARTY_NOTICES.md`.
