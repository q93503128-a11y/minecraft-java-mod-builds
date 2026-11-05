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

## User-proposed direction — 2026-11-06 (new conversation, NOT final detailed GDD)
The user proposed integrating RPG content into the normal generated Minecraft overworld, **combining survival and turn-based combat**, retiring mandatory story progression in favor of **NPC/job-board contracts**, and removing **all vanilla creatures**. Treat this as the currently preferred design direction; detailed gameplay architecture, exceptions and content still require agreement.

### Working design interpretation
- Preserve vanilla terrain generation, mining, crafting, building, farming and exploration. Add authored RPG villages, ruins, dungeons and points of interest without assuming an old imported world.
- Replace vanilla **living mobs** with authored TURNBOUND wildlife, enemies, boss encounters and civilian service NPCs. Do not accidentally remove players, nonliving utility entities, drops, projectiles, displays or vehicles. The stated "all vanilla creatures" potentially includes passive livestock, fish, villagers, golems, Nether/End mobs and bosses; audit the full list before implementation, rather than assuming only hostile spawns.
- Do not disable mobs into an empty ecosystem before replacement animals, food sources, leather/wool equivalents, progression drops, trading/service NPCs, access to Nether/End materials and vanilla recipes have viable alternatives.
- World and other players continue running in real time; only participants are managed by a turn-battle session with server-authoritative protection from outside damage, arena/world interaction exploits and cross-player contention. No global tick pause.
- Avoid a mandatory linear story. Use purposeful contract categories (hunt, gather, exploration, boss bounty) and meaningful resource or equipment rewards; avoid repetitive menu chores and disconnected currencies.
- Align one coherent economy: survival crafting must help exploration/turn combat; battle drops/contracts must help survival progress. Preserve the user choice of what to do next.
- PvE player role/party identity is **not settled**: player fights directly vs player collects/recruits a party vs hybrid. This determines how survival HP, held weapons, armor, buffs, companions and turn-based skills interact. Do not implement these as an assumed default.
- Avoid legacy authored character/boss/quest resurrection from dormant source. Reusable engine code is allowed only after separating old hardcoding.

### Relevant external reference
- Pixelmon has a server-side `allow-vanilla-mobs` option defaulting false, and uses overworld encounters with turn-based battles. Its docs explicitly note that some spawner/egg/dragon paths differ from the ordinary spawn toggle, underscoring the need to audit non-natural spawning separately. Reference for product-level behavior, NOT a drop-in NeoForge 26.2 implementation: https://pixelmonmod.com/wiki/Config
- NeoForge entity and spawning documentation for later implementation planning: https://docs.neoforged.net/docs/entities/

### Next single collaborative decision
Choose who takes actions in combat: **the player avatar**, **collected/recruited party members**, or **both**. Only after this decide first creatures, equipment relationships, combat presentation, contracts, and the smallest playable slice. Do not begin a mass spawn replacement before a sufficient substitute ecosystem exists.
