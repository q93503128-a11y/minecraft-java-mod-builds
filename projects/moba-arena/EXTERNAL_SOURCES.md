# MOBA Arena — External Source & Donor Audit

This file records candidate and adopted third-party code, maps, UI and asset families.

It is a planning/audit ledger, not proof that every candidate has already been downloaded or integrated.

## Status vocabulary

- `ADOPTED` — selected for production use
- `PRIMARY CANDIDATE` — strongest current lead, not yet locked
- `CANDIDATE` — worth inspecting
- `REFERENCE ONLY` — may inform behavior, but is not an approved code/asset intake source
- `LOCAL-ONLY CANDIDATE` — potentially usable by the owner locally, but must not be committed to this public repository under current known terms
- `REJECTED` — not suitable for this project
- `NEEDS LICENSE AUDIT` — terms are unclear or incomplete

## Source-use vocabulary

- `dependency` — use the external mod/library directly
- `port` — adapt external source code to the target Minecraft/loader version
- `asset` — use external art/map/audio/etc. under its terms
- `local asset` — user installs original third-party bytes locally; Git stores only integration metadata/code
- `reference` — behavior/design study only; cannot become the final asset by itself

---

## 1. Map and broad MOBA donors

### Matter Overdrive — MOBA Map

- Status: `PRIMARY CANDIDATE` for an unrestricted map donor; `CANDIDATE` for minion behavior provenance
- Source: https://www.curseforge.com/minecraft/worlds/matter-overdrive-moba-map
- Version: Minecraft 1.7.10
- Map license shown by CurseForge: **Public Domain**
- Known behavior: team Android spawners produce units assigned to opposing teams; units travel from one side toward the other and attack enemy Androids/players.
- Required old runtime: Matter Overdrive + Forge 1.7.10
- Planned use:
  - inspect/import map as a possible selectable arena;
  - study its spawn/lane setup;
  - inspect Matter Overdrive Android source as a donor for lane combat behavior.
- Risk: very old world/mod version, so map conversion and code porting need proof.

### Matter Overdrive source

- Status: `CANDIDATE` code donor
- Source: https://github.com/simeonradivoev/MatterOverdrive
- License: **GPL-3.0**
- Repository state: archived legacy source
- Planned use: inspect Android/team/pathing/combat code connected to the old MOBA map and port only if it is still the strongest donor.
- Risk: major API gap from legacy Forge to Minecraft 26.2; adopting source may impose GPL obligations on derivative code.

### League of Legends - 1.12.2 - Summoner's Rift (playable map)

- Status: `CANDIDATE / NEEDS LICENSE AUDIT`
- Source: https://www.planetminecraft.com/project/league-of-legends-in-minecraft---playable-3479252/
- Known advertised mechanics: turrets, inhibitors, brushes, minions, AP-scaling abilities, shop, respawn timer; project page also describes champions, AD/AP scaling, gold, kills/assists, jungle, Dragon/Baron and a resource pack.
- Intended players: page describes 2–10 players.
- Planned use: inspect as a broad gameplay donor because it already attempted most core MOBA loops inside Minecraft.
- Restriction: do not commit/reuse its files or code until actual usage terms are verified.

### World of Champions / War of Champions

- Status: `REFERENCE ONLY / LOCAL-ONLY CANDIDATE`
- Source: https://www.curseforge.com/minecraft/worlds/world-of-champions-the-vanilla-moba
- Known scope: Minecraft vanilla MOBA with character kits and 3v3-oriented gameplay.
- License shown by CurseForge: **All Rights Reserved**
- Planned use: gameplay/UX/balance study and possibly local personal play for comparison under original download terms.
- Not approved for repository code/assets.

### Summoner's Rift Pre-Season 10 replica

- Status: `LOCAL-ONLY CANDIDATE`
- Source: https://www.planetminecraft.com/project/re-league-of-legend-summoner-s-rift-download/
- Description: 1:1-scale Summoner's Rift replica.
- Author page states editing and distributing are not allowed, while use for video/public server is described separately.
- Planned use: possible owner-local selectable arena if its actual terms permit the intended private use.
- Public Git boundary: do not commit the map or modified copies.

### Other Summoner's Rift maps

- Status: `CANDIDATE / NEEDS LICENSE AUDIT`
- Example source: https://www.planetminecraft.com/project/summoners-rift-map/
- Purpose: broaden map choice if more than one legally/technically usable arena exists.
- Rule: each candidate gets its own provenance/license entry before adoption.

---

## 2. Combat / ability code donors

### Spell Engine

- Status: `PRIMARY CANDIDATE`
- Source: https://github.com/ZsoltMolnarrr/SpellEngine
- Distribution: https://www.curseforge.com/minecraft/mc-mods/spell-engine
- License: **GPL-3.0**
- Current audit observation: Minecraft 26.2 builds exist for both Fabric and NeoForge; the 26.2 port targets Java 25.
- Useful scope: data-driven spell/casting system, targeting, delivery/impact hooks, visuals, weapon integration, HUD/GUI support.
- Planned use: direct dependency first; source port/modification only if needed and license-compatible.
- Loader implication: does not decide the loader by itself because both Fabric and NeoForge are supported.

### Matter Overdrive combat/entity code

- Status: `CANDIDATE`
- Source: https://github.com/simeonradivoev/MatterOverdrive
- License: GPL-3.0
- Useful scope: team-aware Android/minion-like entity behavior from the historical MOBA setup.
- Planned use: narrow port candidate, not a reason to import the entire old tech mod.

---

## 3. Animation/runtime donors

### GeckoLib

- Status: `PRIMARY CANDIDATE`
- Source: https://github.com/bernie-g/geckolib
- Distribution: https://www.curseforge.com/minecraft/mc-mods/geckolib
- License: **MIT**
- Current audit observation: Minecraft 26.2 builds exist for Fabric, Forge and NeoForge.
- Planned use: external model/entity/item/armor animation runtime where adopted third-party assets are compatible.
- Important: GeckoLib is runtime technology, not an art source by itself.

### Player Animation Library

- Status: `PRIMARY CANDIDATE`
- Source: https://github.com/PlayerAnimationLibrary/PlayerAnimationLibrary
- Distribution: https://www.curseforge.com/minecraft/mc-mods/player-animation-library
- License: **MIT**
- Current audit observation: Minecraft 26.2 builds exist for Fabric and NeoForge.
- Useful scope: external player animation clips, including Blockbench/GeckoLib/Bedrock-format animation ingestion.
- Planned use: direct dependency if playable characters remain player-model based or donor content requires it.

---

## 4. UI implementation and UI visual donors

### UI Lib

- Status: `PRIMARY CANDIDATE` as implementation framework
- Distribution: https://www.curseforge.com/minecraft/mc-mods/ui
- License: **Apache-2.0**
- Current audit observation: Minecraft 26.2 builds exist for Fabric and NeoForge.
- Planned use: screen/component implementation if it reduces glue code.
- Important: it does not satisfy the project's external-design requirement alone. A real external visual asset/HUD family is still required.

### RPG-HUD

- Status: `CANDIDATE` code/layout donor
- Distribution: https://modrinth.com/mod/rpg-hud
- License: **GPL-3.0-or-later**
- Current audit observation: Minecraft 26.2 is supported and source is public.
- Useful scope: externally designed RPG-style HUD behavior/layout/components.
- Planned use: inspect source/assets/terms as one candidate for actual HUD adoption or code/layout reuse.
- Risk: MOBA-specific ability/shop/score screens are not guaranteed; additional external UI sources will likely be needed.

### Production UI visual family

- Status: `UNRESOLVED — BLOCKS FINAL UI IMPLEMENTATION`
- Requirement: must be a directly usable/editable external UI asset/mod family with clear terms.
- Do not substitute AI-created panels/icons while unresolved.

---

## 5. Character/model/VFX/audio donors

Current status: `UNRESOLVED`.

Before locking a playable roster, audit actual external packages that contain enough of:

- model/texture;
- animation;
- ability visuals;
- icons;
- sound;
- source code or data-driven abilities.

Prefer coherent packs/mods over mixing unrelated visual styles.

No project-original final art may fill a missing category.

---

## 6. Loader decision matrix

Loader is still open because the strongest modern candidates currently support both sides.

### Fabric advantages to investigate

- breadth of modern lightweight libraries and open-source examples;
- compatibility with selected 26.2 donor stack;
- local/private multiplayer convenience if later desired;
- ease of porting donor code where relevant.

### NeoForge advantages to investigate

- possible easier conceptual migration from old Forge-era donor code;
- current support in Spell Engine, GeckoLib, Player Animation Library, UI Lib and other candidates;
- server/entity/event APIs that may fit legacy donor ports.

### Selection rule

Do not choose by preference alone.

Lock only after we score the final candidate set on:

1. direct code reuse amount;
2. porting effort;
3. UI/animation/content compatibility;
4. minion/tower/entity implementation compatibility;
5. network/server authority requirements;
6. dependency conflicts;
7. Minecraft 26.2 maintenance health.

---

## 7. Source-admission checklist

For every adopted source record:

- exact source URL/repository;
- author;
- exact license/usage terms;
- exact version/commit/tag/file;
- whether source is dependency, port, asset, local asset or reference;
- modification permission;
- redistribution permission;
- attribution/notice requirement;
- public-Git eligibility;
- Minecraft/loader version;
- dependencies;
- what subsystem it owns;
- what adapter code we must write;
- known conflicts with other adopted sources.

Do not mark a source `ADOPTED` until these fields are sufficiently known for its intended use.

## 8. Next audit work

Priority order:

1. download/inspect the strongest broad MOBA map/mechanics donors;
2. inspect Matter Overdrive Android/minion code paths and determine whether a narrow port is practical;
3. find at least two additional open-source minion/lane/structure donors before locking the AI architecture;
4. find actual reusable MOBA-style UI visual assets/mods, not merely UI frameworks;
5. find coherent playable-character/skill/animation packs;
6. map donor requirements against Fabric and NeoForge 26.2;
7. lock the smallest coherent external stack;
8. only then bootstrap source/build files.
