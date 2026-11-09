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
- The initial Fabric entrypoint implements a **limited server-authoritative world safety layer**: opt-in save gate, Adventure mode enforcement, block break and common placement item prevention, incoming damage cancellation, hunger/HP restoration. It **does not** implement TURNBOUND combat sessions, character rewards, dungeon systems or complete terrain mutation protection. Vanilla survival bars are now suppressed on clients after the server sends the opt-in world mode; the actual on-screen result still requires a Minecraft client test.
- Direct port of generic `TurnScheduler`, state/skill/stat arithmetic and target resolution is started. Historical hardcoded P06/P08 behavior is removed from this *new isolated copy*, not from the archived source. No content from old roster is restored.
- The legacy world bootstrap/downloader, NeoForge bindings and old GAME_DATA are **not** part of Fabric output. Existing old worlds remain untouched.
- **Verified build checkpoint (2026-11-09)**: [Fabric Build #1067](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42564964448) PASS (Gradle tests, compile/build and Fabric JAR inspection) at `4bbade5e0effe78a85c138bd6ab703deb01d6052`. [Fabric Client Boot Smoke](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42564964435) PASS (headless Xvfb graphical Minecraft startup). These results do **not** test in-world interaction, multiplayer, Drehmal map integration or visual HUD inside a world.

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
1. Fabric Gradle dependency/build and graphical client startup: **VERIFIED** as above; no repeated CI necessary for this documentation change.
2. Fabric live login into activated world, Adventure mode + inventory/terrain mutation, health/hunger/HUD.
3. Copy-world compatibility and datapack command analysis using actual v2.2.2f world files.
4. Core battle session + network + persistence adapters, authoring catalog.
5. Visual camera/UI, GeckoLib animation, audio and multiplayer.
No claim of PLAYTESTED or MULTIPLAYER TESTED is made.

## Additional map features found in official documentation (2026-11-09)

These are **official/reference behavior, not verified in the provided v2.2.2f binaries**. They are not proof of dependency-free reuse.

- **Terminus Network:** existing fast-travel towers, central hub and already-used tower unlocking are spatially valuable. Their commands, original security-clearance/story milestones and original UI should **not** be silently carried over. Source: https://www.wiki.drehmal.cyou/Story_and_Features/Terminus_Network/
- **Critical exploration conflict:** some towers were designed to be climbed by **placing blocks/pillaring**. TURNBOUND forbids placement. Preserve original geometry and provide an alternate traversal affordance or non-destructive interaction, verified in-client. Do not invent a universal teleport bypass.
- **Original regional maps and merchant buildings:** source map helps guide exploration and existing town layouts fit TURNBOUND services. Source: https://www.wiki.drehmal.cyou/Help/Getting_Started/
- **Holotexts / CIT Resewn:** some map visual text depends on an external client resource pack / CIT Resewn. Treat this as an optional visual preservation dependency after screen inspection, not permission to redistribute the resource pack. Source: https://wiki.drehmal.cyou/Help/FAQ/
- **World datapack:** upstream's FAQ explicitly warns that separating terrain from story/weapons requires significant datapack surgery; do not delete the whole pack. **The actual world datapack remains NOT INSPECTED in this task**, because no official map archive was supplied to the runtime and remote binary world extraction was unavailable.
- **Publication permissions:** adapting the map for an independent RPG server or redistributing map assets requires contacting Drehmal creators; the wiki also states creator credit and limitations on monetization. Source: https://wiki.drehmal.cyou/Help/FAQ/

The audit tool and source-preserving migration boundary are ready to use on an **isolated map copy**. No original Drehmal functions have yet been disabled on disk or demonstrated safe to disable.

## 2026-11-09 subsequent separation and combat unit

See [DREHMAL_DATA_PACK_SEPARATION.md](DREHMAL_DATA_PACK_SEPARATION.md). Added a non-destructive datapack call-graph inspector/copy stager and a loader-independent common action resolver. The actual official world files were **not** analyzed or disabled, and the combat resolver is not yet connected to player-facing world encounters. Keep old source/saves safe.

## Fabric separation checkpoint (2026-11-09)
- [Drehmal separation status and inspector](DREHMAL_DATA_PACK_SEPARATION.md).
- Build/test [TURNBOUND #1068](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42566636024): core-logic Python/JDK17 + Gradle Fabric tests/build/JAR verification PASS, source SHA `3b65df702332741e264eaac160c01607be5b1ced`.
- Actual v2.2.2f world binaries, original datapack disabling, multiplayer and in-world play remain NOT TESTED. No world copy has been created or modified remotely.
