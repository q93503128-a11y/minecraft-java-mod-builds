# MOBA Arena — Project Contract

> Working project slug: `moba-arena`
> Final player-facing title: **TBD**
> Current phase: **DONOR STACK CONDITIONALLY LOCKED / RUNTIME PREFLIGHT NEXT**

## Repository contract

This project follows repository root `AGENTS.md`, `docs/BUILD_STANDARD.md`, `docs/QUALITY_STANDARD.md`, the current 문승준 Minecraft high-quality playbook, and this project's canon.

When older chat history conflicts with current GitHub `main`, current `main` wins.

## Technical identity

- Slug: `moba-arena`
- Provisional Mod ID: `moba_arena`
- Provisional Namespace: `moba_arena`
- Bootstrap mod version: `0.1.0-alpha.1`
- Minecraft target: **1.19.2 — conditional donor lock**
- Java target: **17**
- Loader: **Forge**
- Forge compatibility target: **43.3.13 provisional; must pass Anime Assembly dependency smoke test before source bootstrap**
- Build plugin / Gradle: lock during M0 after the dependency profile boots cleanly
- Planned JAR: `moba-arena-0.1.0-alpha.1.jar`
- Existing-world compatibility: not promised; matches use imported arena worlds and resettable match state

The project intentionally prefers the version with the strongest complete external game stack over the newest Minecraft version. The current first-slice choice is Forge 1.19.2 because Anime Assembly 1.1.4 already supplies a coherent player-facing MOBA combat layer: character selection, 22 playable kits, four character abilities plus an additional skill, health bars, team assignment helpers, all-player Ready/Start flow, an M-key equipment shop, NPC combat facilities and a MOBA minimap.

Minecraft 26.2 remains a researched alternative, not the initial target. Anime Limitless 1.1.0 on Fabric 26.2 has a much larger character/technique roster, but is All Rights Reserved and does not currently document an equivalent integrated MOBA team/ready/shop/minimap match layer. Choosing it first would force this project to author substantially more game architecture, which violates the external-first goal.

## Required first-slice runtime stack

The exact intake rules and versions are normative in `IMPLEMENTATION_BLUEPRINT.md` and `EXTERNAL_SOURCES.md`.

Current planned runtime:

- Anime Assembly 1.1.4 — player characters, abilities, animations/VFX, health bars, character selection, MOBA Ready/Start, shop and minimap;
- GeckoLib 3.1.40 for Forge 1.19.2 — required Anime Assembly animation runtime;
- playerAnimator / `player-animation-lib-forge-1.0.2.jar` — CurseForge project 658587, file 4418149, MIT; required Anime Assembly player animation runtime;
- Pehkui 3.8.2 for Forge 1.19.2 — required Anime Assembly scaling runtime;
- Kleiders Custom Renderer API 6.0.0 for Forge 1.19.2 — CurseForge project 682065, file 5083496, All Rights Reserved; required Anime Assembly renderer and its CurseForge relations page declares no dependencies;
- SmartBrainLib 1.9 / 1.19.2 branch — minion sensing, targeting, path movement and combat behavior runtime.

The former Kleider version-identification blocker is closed. That does **not** mean the dependency profile is tested: the exact JAR bytes still need to be obtained from their original sources, fingerprinted and smoke-tested together.

Third-party runtime JARs are dependencies, not files to copy casually into this public repository. Their licenses, download identity, checksum and redistribution boundary must be recorded before packaging. In particular, the All Rights Reserved Kleider JAR is a local/direct runtime dependency and is not to be rehosted in this repository.

## Product identity

This is a Minecraft-hosted MOBA assembled primarily from existing third-party code, maps, UI, models, animation, VFX and audio rather than internally designed substitutes.

Core play target:

- two teams;
- each team accepts **1 to 5 human players**;
- asymmetric sizes such as `1v2`, `2v5`, `5v1`, `4v5` are legal;
- standard symmetric sizes through `5v5` are legal;
- selected map geometry never resizes/crops/simplifies because of team size;
- multiple external maps may eventually be selectable;
- player/champion bots are **out of initial scope**;
- MOBA-native AI such as lane minions, neutral monsters and automated structures remains mandatory.

## External-first hard contract

### Player-facing layer

Do not build a parallel replacement for functionality already supplied by the adopted runtime.

For the first slice, Anime Assembly owns by default:

- character roster and character-selection presentation;
- character combat kits and ability execution;
- character animations and bundled skill presentation;
- character health bars;
- MOBA Ready/Start interaction;
- the visible in-match equipment shop when technically bridgeable;
- the visible MOBA minimap when technically bridgeable;
- its own player/NPC rendering dependencies.

The project may wrap or validate these systems but must not silently replace them with a second custom character engine, second shop, second cooldown HUD or project-created final art.

### Major gameplay code

Default order:

1. use an external mod/library directly;
2. port a permissively licensed external implementation;
3. adapt that implementation only where Minecraft/loader/API integration requires it;
4. write the minimum adapter/orchestration code needed to connect the external pieces.

The currently selected donor split is:

- player combat/presentation: Anime Assembly direct dependency;
- lane wave/reward lifecycle: SimpleLaneWars (`c0mbit/mc-dota`, MIT) narrow port;
- minion AI executor/pathing: SmartBrainLib direct dependency;
- tower/inhibitor/final-core state skeleton: `cadox8/LoM` (Apache-2.0) narrow port;
- match/team bridge: Anime Assembly state first, LoM team/game state shapes only where a missing boundary must be filled;
- visible shop/economy: Anime Assembly first; LoM shop code only as a fallback backend if Anime Assembly cannot expose a reliable server-authoritative transaction path.

`lol-minecraft` is **reference only** because no repository license was found. No implementation code may be copied from it.

## Allowed project-owned code

Project-owned code is limited mainly to:

- Forge registration and compatibility glue;
- `AnimeAssemblyBridge` and version fingerprinting;
- map metadata and local-map binding;
- match phase orchestration not exposed by the dependency;
- server-authoritative validation/synchronization;
- lane waypoint memory/predicates connecting SmartBrainLib to map metadata;
- ports of explicitly admitted donor files;
- result/reset flow;
- tests, validators and provenance tooling.

The goal is not zero new lines. The goal is that project code connects external foundations rather than recreating them.

## Map contract

- Never build a new arena map for this project.
- Never modify map geometry because of player count.
- Anime Assembly's modified MOBA map is the first local-runtime candidate because it is the map its MOBA mode was designed around.
- Anime Assembly credits the parent as Shinkiroo's `League of Legends Summoner's Rift (Pre-Season 10)` map. The original Planet Minecraft page explicitly says editing and distributing are not allowed, and no separate redistribution/edit permission for Anime Assembly's modified Google Drive copy has been verified.
- Therefore the parent and modified map bytes are **local-only** for this project unless a separate permission grant is later proven. Do not commit, redistribute or package them from this public repository.
- Git may store source identity, the modified-copy Drive file ID, metadata, coordinates, expected folder identity and checksums; it must not store restricted map bytes.
- The modified copy's world-folder identity and SHA-256 remain M0 intake work because the bytes have not yet been acquired in this environment.
- Backup candidates remain documented in `EXTERNAL_SOURCES.md`.

## Public repository / private-use boundary

Private personal play does not turn third-party content into redistributable content.

Therefore:

- do not commit third-party JARs/maps/assets unless redistribution permission is established;
- do not assume Anime Assembly's AFL-3.0 license grants rights to every underlying franchise character, trademark or externally sourced asset contained in or depicted by the mod;
- preserve MIT/Apache/MPL notices and modification notices where applicable;
- treat All Rights Reserved runtime dependencies as dependency references/local bytes rather than repo-bundled artifacts unless explicit redistribution permission is proven;
- do not bypass paid access, DRM or access controls;
- re-audit every adopted source before any public game distribution.

## Canon files

- `PROJECT.md` — technical identity and hard constraints
- `GAME_DESIGN.md` — gameplay and match-flow conflict authority
- `IMPLEMENTATION_BLUEPRINT.md` — exact donor ownership, port map, adapter boundaries and ordered implementation plan
- `EXTERNAL_SOURCES.md` — source/license/version research and candidate/adoption status
- `THIRD_PARTY_ASSETS.md` — provenance ledger for material actually entering the project/runtime

`IMPLEMENTATION_BLUEPRINT.md` is a technical annex, not a competing gameplay design document. If implementation mapping conflicts with older exploratory notes, the blueprint wins. If it conflicts with gameplay intent in `GAME_DESIGN.md`, fix the blueprint rather than changing the game silently.

## Preflight gate before gameplay source bootstrap

Do **not** bootstrap gameplay source until all of these are closed:

1. obtain the exact pinned Anime Assembly, GeckoLib, playerAnimator, Pehkui and Kleider JAR bytes from their original sources and record SHA-256 fingerprints;
2. Anime Assembly 1.1.4 + required dependencies boots on the selected Forge 1.19.2 profile;
3. the same profile is checked for multiplayer/dedicated-server viability to the extent the dependency supports it;
4. Anime Assembly's actual JAR symbols/resources are inventoried and `AnimeAssemblyBridge` is designed from real symbols, not guessed class names;
5. the local-only MOBA map's modified-copy identity, world-folder name and checksum are recorded without committing or redistributing the map bytes;
6. character select, abilities, health bars, team assignment, Ready/Start, M shop and minimap are manually demonstrated in the donor-only profile;
7. the missing-screen UI asset family for map/team/result screens has a legally usable external basis.

If Anime Assembly fails a critical compatibility gate, stop and re-evaluate the donor stack. Do not respond by writing a parallel home-grown character/skill engine.

## Validation state

- `CODE REVIEWED`: **planning/donor documents reviewed; no gameplay source exists yet**
- `TESTED`: **NOT RUN**
- `BUILD VERIFIED`: **NOT RUN**
- `JAR PRODUCED`: **NO**
- `PLAYTESTED`: **NO**
- `MULTIPLAYER TESTED`: **NO**

This remains a design/source-audit checkpoint.