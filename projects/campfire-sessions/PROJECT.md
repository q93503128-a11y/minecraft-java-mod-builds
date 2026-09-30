# Campfire Sessions

Status: ALPHA.6 MUSIC FOUNDATION / ISLAND-LIFE DESIGN PHASE

- Mod ID: campfiresessions
- Version: 0.6.0-alpha.1
- Minecraft: 26.2
- Java: 25
- Loader: NeoForge 26.2.0.87
- Distribution target: private/personal play via Modrinth .mrpack with all required gameplay assets/config/world template provisioned; no manual map install
- Current product direction: Cozy Multiplayer Island Life Sim
- Canonical game design: `GAME_DESIGN.md`
- External asset/dependency planning: `ASSET_PLAN.md`
- Actually adopted third-party assets: `THIRD_PARTY_ASSETS.md`

## Current foundation

Alpha.6 remains the current implemented foundation:
1. Acoustic Guitar with external Minecraft-native model.
2. Wooden Chair with external furniture model and seating behavior.
3. 15 verified CC0 bundled tracks.
4. build-generated real OGG duration/BPM metadata.
5. inertial song-selection carousel.
6. Kenney external UI artwork.
7. PREV / PLAY-STOP / NEXT / REPEAT-AUTO controls.
8. outside-UI B/N/R key mappings.
9. BPM-driven note feedback.
10. local-player third-person guitar arm-pose prototype; final full-body instrument performance animation and multiplayer sync are NOT implemented yet.
11. up to 32 local custom OGG tracks through `config/campfiresessions/music/`.
12. repeat-one and auto-next.
13. Build Campfire Sessions CI success and produced 0.6.0-alpha.1 JAR.

## Product pivot

Campfire Sessions is no longer planned as only a compact music-and-rest mod.

The existing music system becomes one lifestyle pillar inside a larger island-life game:
- persistent island village.
- animal residents.
- multiplayer households.
- fixed authored buildings.
- housing debt and expansion.
- shops and catalog.
- museum and collecting.
- fishing, bugs, sea life, fossils, flora and mushrooms.
- cooking.
- seasons/weather/calendar.
- pier-based travel.
- exploration and limited combat.
- resident relationships, moving, gifts, mail and visits.
- contests, birthdays, seasonal events and a multi-day major festival.

The complete accepted direction is maintained in `GAME_DESIGN.md`.
Do not re-design these systems from scratch in future chats without an explicit user change.

## Visual production rule

Final player-facing design must be driven by high-quality external assets/reference implementations.
Do not default to improvised AI-authored:
- UI.
- buildings.
- furniture.
- resident appearance.
- tools.
- boats.
- clothing.
- festival props.

Use `ASSET_PLAN.md` for candidate tracking and `THIRD_PARTY_ASSETS.md` for adopted assets.

## World rule

Canonical base world: **Geming400 — Island map | 1024×1024 / Island - No WorldBorder.zip**.

Status: **SELECTED / REAL WORLD INSPECTED / 26.2 SERVER LOAD VERIFIED**.

The selected world is MIT licensed, directly obtainable, inspected from its actual region files and legally suitable for bundling in the Campfire Sessions Modrinth modpack.

The user should not need to manually:
- download a separate map ZIP.
- extract/copy a world into `saves/`.
- install schematics.
- repair version/loader-specific world files.

The modpack ships the canonical world as a protected template/config asset and Campfire provisions the playable save automatically without overwriting an existing progressed world during pack updates.

Map-layout direction:
- the selected world provides one dominant main landmass and multiple meaningful secondary islands.
- the main village uses the existing flatter central area plus targeted authored terrain grading.
- do not flatten the whole island; preserve coastlines, hills and natural identity.
- secondary islands remain available for travel, collection, exploration and event content.

Major building exterior positions are fixed and managed by the game.
Players decorate permitted interiors/yards/public decoration zones but do not freely destroy or rebuild critical village structures.

## Multiplayer authority

Server-authoritative state includes at minimum:
- money.
- loans.
- household membership.
- personal collection encyclopedia.
- houses.
- residents and moves.
- donations.
- shop state.
- public projects.
- calendar/seasons/weather.
- event state.
- protected-world state.

Do not claim multiplayer success until actually tested.

## Scope boundary

- No public-release assumption.
- No bypass of paid access/DRM/access restrictions.
- No player-facing debug/test/prototype/TODO/developer residue.
- No test resources masquerading as final game content.
- No unnecessary duplicate systems or throwaway code.
- No spontaneous AI visual design replacing available external quality assets.

## Current validation state

Implemented alpha.6 music foundation:
- CODE REVIEWED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- PLAYTESTED: NO
- CLIENT VISUAL TESTED: NO
- MULTIPLAYER TESTED: NO

Island-life expansion:
- DESIGN IN PROGRESS
- CANONICAL BASE WORLD SELECTED: YES
- WORLD REGION DATA INSPECTED: YES
- WORLD LOAD VERIFIED ON MINECRAFT 26.2: YES
- IMPLEMENTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

## Next planning rule

Continue planning in batches of roughly eight genuinely new decisions.
Already-decided topics should be referenced briefly when needed, not recycled merely to fill the batch.
Before implementation, finish the major unresolved items listed in `GAME_DESIGN.md`, especially:
- final directly obtainable map and real-world inspection.
- building/house prefab set.
- resident model/rig set.
- furniture/UI/tool/boat asset selections.
- player/resident animation stack validation, including the Player Animation Library candidate and final full-body guitar performance.
- economy numbers.
- concrete save serialization details and remaining Household implementation edge cases.
