# TURNBOUND — New Player-Facing Game Content Direction (discussion draft)

Status: DISCUSSION DRAFT — no proposed setting, character, ability, quest or map style is approved canon.
Date: 2026-11-05. Branch: turnbound-content-rebuild.
Project: existing TURNBOUND; NOT TURNBOUND: RE.

## Approved constraints carried forward
- Rebuild from zero **authored** playable characters, enemies/bosses, narrative, quests, regions, equipment and old Drehmal gameplay. Do not restore retired P01–P08/F01–F04 or their lore by default.
- Game goal: Minecraft exploration + tactical turn battles + collection/progression, with a cohesive player-facing loop. Avoid endless farming and story-only rushing.
- Preserve generic technical machinery (1000-Gauge scheduling, action-target validation, save isolation, multiplayer/server authority) where proven useful; do not inherit historic numeric balance or old authored combat rules automatically.
- User and assistant decide content together. Assistant must not settle character identities, abilities, rarities, narrative beats or quest assignments unilaterally.
- Gameplay/UI art, VFX, models and audio are first-class, not late polish. Design references and asset licensing must be checked before asset production.
- Starting world shown in first real client screenshot was a functional **empty-content** vanilla-world test; it is NOT approval of vanilla terrain as the final authored map.

## Current verification and risk acceptance
- The user observed that the new-build client entered a Minecraft world and that no old TURNBOUND HUD/content appeared in that screenshot.
- Build TURNBOUND #1065 passed unit/build, NeoForge dedicated-server boot, JAR and Modrinth pack checks.
- TURNBOUND Client Boot Smoke #2 passed Linux Xvfb window creation.
- Save/reconnect/death and 2-client multiplayer were not tested. The user chose to start new-content planning now and defer those acceptance checks. Mark them NOT TESTED until directly observed; do not silently claim the full client game is playtested.

## Proposed first *playable slice* (scope only; pending agreement)
One small, visually coherent traversal-to-combat loop that demonstrates why players leave a settlement and come back:
1. Discover a reachable destination in the world using readable environmental cues.
2. Encounter a visible enemy with a consistent world-to-battle transition.
3. Make meaningful decisions in a short turn-gauge combat and see matching effects, animations and impact.
4. Obtain a useful reward that influences the player's next exploration or combat choice.
5. Return or continue along a clearly visible new route.

Do not implement this proposal until the game's actual world style, party/collection identity and first encounter concept are discussed and agreed.

## Open design decisions — explicitly NOT SETTLED
1. **World topology:** (A) expanded vanilla overworld with authored locations, (B) hand-built authored overworld, (C) hub-connected crafted regions; assess travel feel, build/art assets and multiplayer.
2. **Player/party identity:** playable roster acquisition vs persistent main character plus recruited companions, and active party controls. Do not reuse abandoned roster design.
3. **Combat encounter presentation:** seamless field-to-turn battle vs distinct contained arena; reconcile visible world foes, spectators, shared-party ownership and interaction.
4. **Progression loop:** what tangible decisions rewards enable, and what prevents farming fatigue. Do not introduce multiple overlapping currencies by default.
5. **Creative direction:** narrative tone, geographic theme, visual identity, character art source and how the world visually communicates danger/progress.

## Next collaborative decision
Choose the **world topology and party fantasy** before creating names, kits, rarity tables, dungeon lists or story beats. Once decided, document a small vertical slice with measurable art/UX, mechanical, save and multiplayer acceptance, and then delegate suitable visual-production tasks to Codex.
