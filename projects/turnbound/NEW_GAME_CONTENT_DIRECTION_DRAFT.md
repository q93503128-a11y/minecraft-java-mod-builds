# TURNBOUND — New Player-Facing Game Content Direction (discussion draft)

Status: PARTIALLY CONFIRMED — current authoritative decisions in the sections immediately below; specific character identities, kits, numeric balance and battle presentation remain undecided.

## Approved native rarity range — 2026-11-06
- **Native / born rarity is ★1, ★2, ★3, ★4 or ★5 only.** There is no native ★6 gacha rank.
- Native rarity and any future upgrade/promotion/awakening tiers are distinct concepts. A ★6 promotion, sixth display star or Awakening path is **not approved** by today's native rarity decision; decide separately if ever needed.
- The player avatar is outside turn-based combat. The solo combat party holds up to **four playable characters**, obtained by gacha except for the first choice.
- On first start, choose **one of three ordinary ★3 characters**. No special starter-only power, extra rarity or exclusivity; all three are ordinary characters available through the normal pool.
- No rarity probabilities, duplicate conversion, star growth curves, new character identities or starter-specific role kits have been approved.
- These decisions are currently **DESIGN ONLY**. The blank production catalog remains empty and the game does not yet include the new roster or selection UI.

## Latest selected combat and starter direction — 2026-11-06 (overrides older player-plus-companions notes)
- **Solo turn-based combat is performed by a party of up to 4 gacha-acquired characters. The Minecraft player avatar explores/builds/survives but is NOT a combat party actor.**
- **On the first game start, offer a choice of exactly one of three ordinary native ★3 characters.** The selected character joins the player's collection and can be used in combat. No special starter status, exclusive abilities, bonus stats, or permanently unique power.
- The two unselected options should remain obtainable through the regular character-acquisition system, just like the chosen character; starter choices are ordinary roster characters, not a separate special tier.
- **Prior player-avatar-plus-four-companions / five-actor limit is superseded for the current design.** The present solo party limit is 4 characters; no player avatar in turn battles.
- Character identity, exact skills, initial distribution and gacha rates have **NOT** been approved. Do not recycle retired P01–P08/F01–F04 by default.
- The user has now CONFIRMED the old native rarity range **★1–★5** for the new game. A native ★6 summon rank is excluded; any upgrade/Awakening system remains a separate unresolved design question.
- These are **planning decisions only**; starter selection, party roster and gacha have not been implemented into the blank-runtime build.

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
2. **Player/party identity:** SOLVED for solo in the latest sections above: party-only combat, up to four playable units; player avatar is not a turn combatant. Exact roles, roster UI and multiplayer combining remain open.
3. **Combat encounter presentation:** seamless field-to-turn battle vs distinct contained arena; reconcile visible world foes, spectators, shared-party ownership and interaction.
4. **Progression loop:** what tangible decisions rewards enable, and what prevents farming fatigue. Do not introduce multiple overlapping currencies by default.
5. **Creative direction:** narrative tone, geographic theme, visual identity, character art source and how the world visually communicates danger/progress.

## Next collaborative decision
The world direction is vanilla-generated overworld with authored RPG locations; the latest solo combat decision is four gacha/starter characters without the avatar. Co-design the field-to-battle transition and how survival consequences work, then the in-game draw resources before specifying the first playable slice.

## User-proposed direction — 2026-11-06 (new conversation, NOT final detailed GDD)
The user proposed integrating RPG content into the normal generated Minecraft overworld, **combining survival and turn-based combat**, retiring mandatory story progression in favor of **NPC/job-board contracts**, and removing **all vanilla creatures**. Treat this as the currently preferred design direction; detailed gameplay architecture, exceptions and content still require agreement.

### Working design interpretation
- Preserve vanilla terrain generation, mining, crafting, building, farming and exploration. Add authored RPG villages, ruins, dungeons and points of interest without assuming an old imported world.
- Replace vanilla **living mobs** with authored TURNBOUND wildlife, enemies, boss encounters and civilian service NPCs. Do not accidentally remove players, nonliving utility entities, drops, projectiles, displays or vehicles. The stated "all vanilla creatures" potentially includes passive livestock, fish, villagers, golems, Nether/End mobs and bosses; audit the full list before implementation, rather than assuming only hostile spawns.
- Do not disable mobs into an empty ecosystem before replacement animals, food sources, leather/wool equivalents, progression drops, trading/service NPCs, access to Nether/End materials and vanilla recipes have viable alternatives.
- World and other players continue running in real time; only participants are managed by a turn-battle session with server-authoritative protection from outside damage, arena/world interaction exploits and cross-player contention. No global tick pause.
- Avoid a mandatory linear story. Use purposeful contract categories (hunt, gather, exploration, boss bounty) and meaningful resource or equipment rewards; avoid repetitive menu chores and disconnected currencies.
- Align one coherent economy: survival crafting must help exploration/turn combat; battle drops/contracts must help survival progress. Preserve the user choice of what to do next.
- PvE solo-party structure was amended later: **four character actors maximum**, excluding the survival player avatar. Survival HP/consequences, character equipment and multiplayer composition still require design.
- Avoid legacy authored character/boss/quest resurrection from dormant source. Reusable engine code is allowed only after separating old hardcoding.

### Relevant external reference
- Pixelmon has a server-side `allow-vanilla-mobs` option defaulting false, and uses overworld encounters with turn-based battles. Its docs explicitly note that some spawner/egg/dragon paths differ from the ordinary spawn toggle, underscoring the need to audit non-natural spawning separately. Reference for product-level behavior, NOT a drop-in NeoForge 26.2 implementation: https://pixelmonmod.com/wiki/Config
- NeoForge entity and spawning documentation for later implementation planning: https://docs.neoforged.net/docs/entities/

### Next collaborative decision
Latest decision: four playable character actors maximum (no avatar), one of three ordinary ★3 starters chosen initially; native rarity ★1–★5. Next decide field-to-turn combat rules and then draw resource sources/costs. Do not begin mass vanilla mob replacement until a substitute ecosystem can support survival.

## Superseded discussion record — former player-plus-companion concept (2026-11-06)

Approved by the user:
- SUPERSEDED: earlier hybrid player-avatar combat proposal; the current player avatar does not act as a turn combatant.
- Early gameplay is primarily solo; there is no mandatory starter companion.
- SUPERSEDED: the earlier five-person concept (one player + four companions) was replaced by the current four-character-only combat party described at the top.
- Continuing decision: characters are obtained by gacha, except for the first ordinary three-star choice.
- Survival activities, turn-based encounters, exploration and optional job-board contracts should form one coherent gameplay loop. The gacha economy should eventually connect to this loop.

Unsettled implementation/design details:
- Companion roster, classes, individual skills, summon currency, how draws are earned, costs, rates, rarity, duplicate handling, guarantees and pity.
- Whether gacha is exclusively in-game or has any optional monetization (do not assume monetization).
- The survival player's health/equipment remains relevant outside the turn battle; how character-specific gear and status effects interact with survival, and how party members appear in-world, remains to be designed.
- Cooperative battle capacity and whether each human participant brings an independent roster; the five-member limit is the solo player's active-party limit.

Implementation status: this is a **design decision only**. The empty-foundation version does not have a working 5-actor battle or gacha. Existing legacy four-ally engine caps must be adapted before shipping a full new-game party; do not silently claim this is already implemented.

Next collaborative design: define player battle actions and the draw economy/resources before committing character identities, numerical odds or new quest content.
