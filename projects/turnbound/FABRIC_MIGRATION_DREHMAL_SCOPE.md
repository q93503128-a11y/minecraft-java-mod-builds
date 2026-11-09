# TURNBOUND — Fabric 1.20.1 migration and Drehmal map separation

Date: 2026-11-09. Workbranch: `turnbound-content-rebuild`. This is TURNBOUND, **not** TURNBOUND: RE.

## Current user-approved gameplay contract
- Minecraft Java **1.20.1**, **Fabric**, Java 17 target, Drehmal: APOTHEOSIS v2.2.2f as an **existing authored map**.
- Independent 4-character party, fixed-point 1000-Gauge turn combat. Explorer avatar is not a combatant.
- No survival/hunger/vanilla hit points, mining, block breaking or building. Adventure-mode traversal.
- Equipment and promotion materials come from enemy defeat, not world mining.
- Gold/gems, gacha, progression, contracts, dungeon cooperation and defeat policy remain as last approved; do not quietly reset them.
- Drehmal's *landscape and authored structures* are the base; **Drehmal story, weapons, loot and combat are not TURNBOUND canon**.

## Technical migration boundary
- Original `src/main` (NeoForge 26.2), old fixtures, reference assets and historical documents remain on disk and in Git for migration review.
- Gradle now builds **only** `src/fabric/java` and `src/fabric/resources`; tests only from `src/fabricTest`.
- The initial Fabric entrypoint implements a **limited server-authoritative world safety layer**: opt-in save gate, Adventure mode enforcement, block break and common placement item prevention, incoming damage cancellation, hunger/HP restoration. It **does not** implement TURNBOUND combat sessions, character rewards, dungeon systems, UI masking or complete terrain mutation protection.
- Direct port of generic `TurnScheduler`, state/skill/stat arithmetic and target resolution is started. Historical hardcoded P06/P08 behavior is removed from this *new isolated copy*, not from the archived source. No content from old roster is restored.
- The legacy world bootstrap/downloader, NeoForge bindings and old GAME_DATA are **not** part of Fabric output. Existing old worlds remain untouched.
- The Fabric test build / JAR / in-game multiplayer test has not yet been executed until CI is checked.

## Save/world safety
- The Fabric exploration policy is disabled unless the **separate copied** world's root has a file named `turnbound-world.marker` containing exactly `TURNBOUND_DREHMAL_WORLD_V1`. The marker is not automatically created.
- This marker is deliberately NOT an original Drehmal auto-detection. Never add it to a production/legacy world.
- The original Drehmal datapacks are not yet disabled. Entering the map with a marker **before** inspecting the original datapack may trigger both games' commands. Do not call the first Fabric scaffold a ready-to-play modpack.

## Drehmal pack audit / staged separation
1. Obtain official v2.2.2f map from the source and preserve unchanged original world and resource pack.
2. On the extracted world, run:
   `python3 tools/audit_drehmal_world.py --world /path/to/Drehmal --output /path/to/drehmal-audit.json`
   The script reads datapacks/functions and produces an inventory; it does not disable anything.
3. In the original datapack, classify `load`, `tick`, scheduled functions, command block triggers, advancements and reward/storage interactions.
4. Preserve authored structures, world data, special doors/portal or redstone dependency where required; block **original story/combat/reward activations** only after verifying dependencies. Treat custom assets under a separate distribution-license decision.
5. Create a disposable world copy, add the marker **only to the copy**, then validate original special structures, survival suppression and multiplayer gameplay.
6. Only after that should a reproducible map-adapter/package be produced. World bytes must not be committed or included in TURNBOUND artifacts without permission.

## Useful source/reference material (NOT automatically reused as live content)
- `DREHMAL_MAP_REFERENCE_v1.md`: terrain topology, main roads and town/service sites, Capital Valley, Drabyel, Av'Sal.
- `WORLD_OVERHAUL_DREHMAL.md`: source-preserving placement, camera-safe battle candidates, route/settlement/encounter zones.
- `DrehmalRoutePlacementRules`, `DrehmalRouteNavigationRules` and `DrehmalAdaptiveRoutePlacement`: may provide algorithm references for placement, after removing old content/NeoForge bindings.
- `FieldEncounterRules`, `DrehmalFieldEncounterPolicy`: visible ! alert/pursuit/contact design reference.
- `DrehmalWorldProfile` / `DrehmalWorldBinding`: fail-closed save/profile binding precedent; **do not reuse 26.2 data migration or old save slots**.
- Existing map data and renderer are not proven license-cleared for packaging; no code from Drehmal itself was inspected in this migration commit.

## Missing gates
1. Fabric Gradle dependency resolution and full build/compile.
2. Fabric live login, Adventure mode + inventory/terrain mutation, health/hunger/HUD.
3. Copy-world compatibility and datapack command analysis using actual v2.2.2f world files.
4. Core battle session + network + persistence adapters, authoring catalog.
5. Visual camera/UI, GeckoLib animation, audio and multiplayer.
No claim of PLAYTESTED or MULTIPLAYER TESTED is made.
