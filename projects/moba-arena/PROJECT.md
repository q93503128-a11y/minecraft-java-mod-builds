# MOBA Arena — Project Contract

> Working project slug: `moba-arena`
> Final player-facing title: **TBD**
> Current phase: **DESIGN CANON + EXTERNAL SOURCE AUDIT**

## Repository contract

This project follows repository root `AGENTS.md`, `docs/BUILD_STANDARD.md`, `docs/QUALITY_STANDARD.md`, and the current 문승준 Minecraft high-quality playbook.

When this file or the project canon conflicts with older chat history, current GitHub `main` and these project files win.

## Technical identity

- Slug: `moba-arena`
- Provisional Mod ID: `moba_arena`
- Provisional Namespace: `moba_arena`
- Bootstrap mod version: `0.1.0-alpha.1`
- Minecraft target: **26.2**
- Java target: **25**
- Loader: **TBD — must be selected by external-code reuse audit, not habit**
- Loader/API version: **TBD**
- Gradle: **TBD after loader lock**
- Build plugin: **TBD after loader lock**
- Planned JAR: `moba-arena-0.1.0-alpha.1.jar`
- Existing-world compatibility: not promised; the game is match/map-instance oriented and depends on imported third-party arena worlds
- Required dependencies: TBD from `EXTERNAL_SOURCES.md`
- Optional external mods: TBD
- Forbidden bundled dependencies: Minecraft redistribution-prohibited files, paid/pirated assets, or any third-party code/assets whose terms do not permit inclusion in this public repository
- Datagen task: define during source bootstrap if needed
- GameTest task: define during source bootstrap
- Server smoke-test task: define during source bootstrap
- Client smoke-test task: define during source bootstrap

The loader is intentionally not locked yet. Fabric and NeoForge are both candidates. The winner is whichever allows the strongest external code, UI, animation, combat, and MOBA donor stack to be reused with the least rewriting on Minecraft 26.2.

## Product identity

This project is a Minecraft-hosted MOBA assembled primarily from existing third-party code, maps, UI, models, animation, VFX and audio rather than internally designed substitutes.

The project is deliberately **not** an exercise in inventing a new visual language or rebuilding systems that already exist externally.

Core play target:

- two teams;
- each team accepts **1 to 5 human players**;
- asymmetric sizes such as `1v2`, `2v5`, `5v1`, `4v5` are legal;
- standard symmetric sizes through `5v5` are legal;
- the selected map does **not** resize, crop, simplify or change because of player count;
- multiple external maps may be supported and selected before a match;
- player-controlled bots are **out of initial scope**;
- MOBA-native AI such as minions, neutral monsters and structure targeting remains mandatory.

## Personal-use scope

The intended gameplay build is for the owner's private personal use, not public distribution.

However, this GitHub repository is public. Therefore:

- private-use-only or non-redistributable third-party maps/assets MUST NOT be committed here;
- the repository may record source URLs, checksums, expected local paths and installation/import instructions;
- redistributable third-party code/assets may be committed only under their actual terms and with required notices;
- local-only files should remain outside Git or under ignored local intake paths once source bootstrap exists;
- access-control/DRM bypass and paid-asset piracy are forbidden;
- if public distribution is ever planned, every third-party source must be re-audited first.

## External-first hard contract

### Player-facing visuals

The following must come from external usable assets or an external mod/resource pack that supplies them:

- map/world geometry;
- HUD art;
- menu/panel/button art;
- icons;
- character/champion models;
- character/champion textures;
- animations;
- VFX;
- sound effects;
- music if used;
- tower/base/minion visual models when custom visuals are used.

**Do not create original final visual design for these categories.**

Reference-only material is not enough for a final visual if it would require us to invent/redraw the final design. A usable external asset family must be selected.

### Code

For major gameplay subsystems, the default order is:

1. use an external library/mod directly;
2. port an external open-source implementation;
3. adapt an external open-source implementation;
4. write only the minimum adapter, integration, compatibility and configuration code required to connect those pieces.

Do not create a major subsystem from scratch merely because doing so would be faster than finding or porting a donor implementation.

If no acceptable donor exists for a major subsystem, stop that subsystem and perform another source audit before authoring new architecture.

### Allowed project-owned code

Project-owned code may exist for:

- version/loader compatibility ports;
- adapters between third-party systems;
- registration and dependency wiring;
- map metadata bindings;
- spawn/waypoint/objective coordinates for an imported map;
- match state orchestration when no single donor exposes the full integration boundary;
- configuration/data conversion;
- bug fixes needed to make adopted donor code function together;
- server-authoritative validation and synchronization glue;
- tests and validation tools.

The goal is not literally zero new lines. The goal is that the **game's foundation remains externally sourced**, while original code is integration glue rather than a parallel reimplementation.

## Map contract

- Never build a new arena map for this project.
- Never redesign terrain to fit a team size.
- Never auto-scale a map based on team size.
- Never cut a 3-lane map down to 1 lane merely because the match is 1v1.
- If several usable maps are available, expose them as separate selectable maps.
- Prefer importing a map unchanged geometrically.
- Game metadata may identify existing spawn positions, lanes, tower positions, neutral objectives and base cores without rebuilding the world.
- If a map requires gameplay objects that are already present in the donor map, prefer using those rather than replacing them.

## Player bot contract

Player/champion bots are not part of the initial project.

Do not add Baritone, fake-player agents, LLM agents or custom player-bot AI as baseline dependencies just to fill empty team slots.

A match slot is initially either:

- human player; or
- empty.

If player bots are requested later, they require a new explicit audit and plan.

## Required non-player AI

The following remain in scope because the genre requires them:

- lane minions;
- neutral/jungle monsters if the selected donor ruleset/map contains them;
- tower/structure targeting or donor-equivalent automated defenses;
- objective/boss AI where present in the selected external ruleset.

These AI systems must themselves use or port external implementations wherever practical.

## Canon files

- `PROJECT.md` — technical identity and hard project constraints
- `GAME_DESIGN.md` — master gameplay and match-flow canon
- `EXTERNAL_SOURCES.md` — donor-code/map/UI/asset candidates and adoption status
- `THIRD_PARTY_ASSETS.md` — provenance and usage ledger for adopted third-party material

Do not create a competing master design document. Subordinate implementation notes may be added later, but `GAME_DESIGN.md` remains the gameplay conflict authority.

## Current gate before source bootstrap

Do not bootstrap gameplay code until the external-source audit has at least identified viable donors for:

1. arena map;
2. core MOBA mechanics or a sufficiently broad mechanics donor;
3. minion movement/combat AI;
4. player ability/combat framework;
5. player animation;
6. HUD/UI implementation and visual asset family;
7. tower/base/objective behavior;
8. economy/shop/level progression if the chosen MOBA donor does not already supply them.

The loader is selected only after this matrix is strong enough to compare Fabric vs NeoForge on actual reuse value.

## Validation state

Current state after creation of this project folder:

- `CODE REVIEWED`: **NO gameplay source exists yet**
- `TESTED`: **NOT RUN**
- `BUILD VERIFIED`: **NOT RUN**
- `JAR PRODUCED`: **NO**
- `PLAYTESTED`: **NO**
- `MULTIPLAYER TESTED`: **NO**

This is a planning/source-audit checkpoint only.
