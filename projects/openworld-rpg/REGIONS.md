# Open-World RPG — Regional Content Reference

> Status: subordinate design reference  
> Master canon: `GAME_DESIGN.md`  
> Terrain basis: Azari 30k x 30k, current primary free world candidate  
> Loader: Fabric 26.2  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins.

This file expands the regional design requested by `GAME_DESIGN.md` without becoming a competing master document. Exact borders and coordinates remain provisional until the Azari world is imported and inspected in-game. Region identity, ecology, encounter roles and adjacency should survive coordinate refinement.

---

# 1. World-shape reading

The published Azari overview supports a branching authored RPG layout rather than a linear chain:

- a broad central inland sea / lake network with islands and peninsulas;
- a huge frozen northern crown;
- green western and northeastern landmasses with forests, meadows and river systems;
- high mountain / rocky terrain to the west and southwest;
- an eastern desert / oasis / badlands mass;
- a distinct southeastern volcanic-ash landmass;
- southern rocky, coastal and eroded terrain;
- dark obsidian-like pockets around the western-central interior;
- enough shoreline, reef, ocean and island space for a separate maritime layer.

The region graph is intentionally branching. Recommended levels are guidance, not doors. A player can physically ride, walk or sail into a stronger region early and retreat if it is too dangerous.

---

# 2. Difficulty / recommended-level model

Do **not** use broad overlapping level ranges as if every region is a linear step in one ladder.

The project instead uses:

1. **Suggested entry Lv** — one clear number indicating when an average build should first feel comfortable entering a major region.
2. **Local encounter Lv** — subregions, dangerous POIs, elites, dungeons and bosses may sit above or below that region entry value.
3. **Peer regions** — two or more regions with intentionally similar difficulty may share or nearly share a suggested entry Lv, creating route choice rather than fake progression.
4. **No universal scaling** — low-Lv regions stay low-Lv and high-Lv threats remain dangerous if reached early.
5. **No level gate** — recommended Lv is communicated through enemy UI, contracts, NPC warnings, map information and encounter feel, not invisible walls.

This follows the useful part of open-world RPG precedent: The Witcher 3 uses suggested levels for areas/quests while still allowing the player to enter stronger territory, and Wynncraft deliberately has several activities/dungeons inhabiting overlapping bands instead of demanding that every location be a unique sequential level slice. The project keeps that route freedom but avoids confusing every neighboring region with a huge overlapping range.

Current numbers are **working targets**, not the final EXP curve. The later EXP benchmark pass can rescale them globally without changing region order or identity.

## Working region progression

| ID | Terrain identity | Suggested entry Lv | Difficulty role | Signature danger |
|---|---|---:|---|---|
| R01 | central/south-central meadow, river, forest fringe | 1 | starting region | Steelboar / Regalhart / Earthloong |
| R02 | western rich forest and river basin | 8 | early branch | Nature Spirit / Lich |
| R03 | western/southwestern great mountains and windswept ridges | 12 | early-harder branch | Rocky Roller / Basalt Wyvern |
| R04 | northern ice crown, freezing taiga, ice-ocean edge | 20 | expedition tier | Froststalker / Ferox Iceworm / Icebroodmother |
| R05 | northeastern jungle and bamboo-rich green belt | 20 | peer expedition route | Tiger / Komodo / mature Earthloong |
| R06 | eastern river delta, wetland and swamp mosaic | 30 | midgame route | Crocodile/Caiman / Hydra |
| R07 | eastern desert, oasis and badlands | 34 | midgame-harder route | Desert Beetle / Armor of Desert / Ferox Deathworm |
| R08 | flower forest, fairy/whimsical forest, pollinating cliffs | 44 | high-tier magical route | Nature Spirit variants / Moonpriest / Titan Rabbit |
| R09 | southern rocky marches, dry grasslands and eroded coast | 44 | peer high-tier physical route | Flamehorn / Rhino / Executioner |
| R10 | southeastern volcanic ash island and obsidian-lava terrain | 58 | late-game destination | Scorch Golem / Basalt Wyvern / Inferno |
| R11 | central inland sea, pirate coast, reefs and deep-ocean routes | 28 / 44 / 64 | layered maritime route | Riptooth / Beast Horseshoe Crab / Abyss Fang |
| R12 | western-central obsidian spikes, overgrown caves and anomaly pockets | 72 | extreme open-world zone | Farseer / Reaper-Lich tier / Terradragon |

R11 is intentionally layered rather than assigned one misleading number:

- coast / harbor / shallow islands: suggested Lv 28;
- open sea / reef / pirate routes: suggested Lv 44;
- abyssal trenches / deep temples: suggested Lv 64.

R04 and R05 intentionally share a suggested entry level because they are alternate expedition-tier regions with different threats, not one being a mandatory prerequisite for the other. R08 and R09 follow the same rule at a higher tier.

The recommended-level UI should therefore answer **“is this region broadly appropriate for me?”**, while elite/boss markers answer **“is this specific encounter appropriate for me?”**.

---

# 3. Regional packages

## R01 — Heartland starting region

Core identity:

- meadow / plains / river / forest-fringe start;
- approachable ordinary combat; major early difficulty spikes begin with elites, dangerous POIs and the first dungeon;
- Louxia-led non-vanilla food ecology;
- gazelle, bison, raccoon, crow and rare grizzly as curated wildlife;
- common threats include rattlesnake/cave-centipede-style enemies and readable territorial animals;
- Steelboar and Nature Spirit are early elite signals;
- Regalhart is an optional field-boss candidate;
- root-overgrown quarry is the first replayable dungeon;
- Earthloong is the current dungeon-boss candidate;
- first non-vanilla ground mount arrives early and is around sprint-speed convenience rather than a huge speed skip.

R01 foreshadows neighboring ecology. A rare Nature Spirit at the forest edge should imply that the western forest contains much stronger versions of that ecosystem rather than feeling like a random one-off monster.

Resources:

- common ore outcrops;
- starter timber nodes;
- river/forest healing herbs;
- basic foraging ingredients;
- one rarer quarry/grove material used later so the region remains relevant.

Reward identity:

- foundational weapons/armor rather than highly specialized builds;
- deterministic first-dungeon equipment choice so bad RNG cannot cripple early progression.

---

## R02 — Western rich forest / river basin

World role:

- first dense-exploration region;
- narrower roads, hidden landmarks and river forks create route choice;
- more mysterious than R01 without becoming oppressive.

Ecology:

- raccoon, crow and selected small wildlife;
- grizzly as territorial hazard, not constant aggro trash;
- Bunfungus or other compatible fantasy wildlife only where it strengthens identity;
- cave centipedes in root caves and abandoned cuts;
- Nature Spirit becomes a native major threat;
- Regalhart can appear as an uncommon hunt species;
- Lich appears only in authored ruins.

Encounter hierarchy:

- common: small predators/ambushers and territorial wildlife;
- elites: mature Nature Spirits and ancient Regalhart variants;
- field encounter: roaming forest guardian hunt;
- dungeon boss: Lich in a ruined woodland sanctum.

Resources / settlement:

- logging/trade hamlet smaller than the starting hub;
- hardwood, resin, mushrooms, healing plants, mana-active flora.

Dungeon:

- ancient woodland sanctum / collapsed arboretum;
- side branches expose lore/rare herbs instead of padding combat length.

Reward identity:

- DEX/WIL utility;
- poison/status resistance;
- forest movement;
- bow/finesse options;
- nature-linked accessories.

---

## R03 — Whitecrest highlands / windswept mountains

World role:

- first major vertical-traversal region;
- cliffs/passes matter without requiring flight;
- important mineral routes and long sightlines toward stronger lands.

Ecology:

- bison in lower upland meadows;
- highland birds where available;
- gelada monkey in rocky pockets;
- snow leopard near colder upper transitions;
- Sunbird as rare high-altitude fantasy wildlife;
- Rocky Roller in caves and exposed stone corridors;
- Basalt Wyvern as elite / field-boss candidate;
- Steelboar persists only at low density in lower passes.

Encounter hierarchy:

- common: highland wildlife, cave creatures, rolling/charge enemies;
- elites: authored Rocky Roller rockfall zones and wyvern encounters;
- field boss: Basalt Wyvern on a high exposed plateau;
- dungeon boss remains open until in-game inspection identifies a strong mountain-fit 26.2 candidate.

Resources / settlement:

- fortified mining town / cliff outpost;
- iron-family ore, dense stone, silver-like mineral, highland crystal;
- lifts/bridges/shrine shortcuts replace annoying permanent movement penalties.

Dungeon:

- collapsed cliff mine into ancient observatory/forge;
- vertical loops, lifts and one exterior cliff segment distinguish it from R01.

Reward identity:

- guard strength;
- poise damage;
- heavy weapons;
- ranged weak-point gear;
- knockback resistance.

---

## R04 — Northern frozen crown

World role:

- ice spikes, freezing taiga, ice ocean and freezing-abyss terrain;
- weather/visibility matter, but no tedious always-on temperature-management chore unless playtest proves value.

Ecology:

- moose;
- seal;
- snow leopard;
- Froststalker as a major hostile predator;
- Ferox Iceworm as field/world-boss candidate;
- Icebroodmother as dungeon-boss candidate if current behavior/animation passes inspection.

Encounter hierarchy:

- common: cold wildlife and Froststalker pockets;
- elites: large Froststalker hunts / ice-cave predators;
- field boss: Ferox Iceworm;
- dungeon boss: Icebroodmother.

Resources / settlement:

- expedition fort / geothermal refuge;
- fur/hide, frost crystal, cold-water ingredients, rare winter herbs;
- frozen coastline connects into R11.

Dungeon:

- glacial fissure / frozen nest;
- breakable ice shortcuts and wide final arena;
- slippery-floor gimmicks used sparingly.

Reward identity:

- frost/status control;
- high poise;
- defensive accessories;
- anti-slow mobility.

---

## R05 — Northeastern jungle / bamboo greenbelt

World role:

- dense biodiversity;
- navigation constrained by vegetation/rivers rather than invisible walls;
- canopy landmarks keep the region readable.

Ecology:

- gorilla, capuchin, toucan and jungle ambience;
- tiger as stalking threat;
- komodo dragon;
- anaconda where current behavior is stable;
- leafcutter ants and selected insects as ecology, not combat spam;
- mature Earthloong as native high-tier threat.

Encounter hierarchy:

- common: insects/reptiles and avoidable predators;
- elites: tiger/komodo/anaconda variants where tuning makes roles distinct;
- field boss: mature Earthloong;
- dungeon boss remains open for a strong current 26.2 jungle/temple candidate.

Resources / settlement:

- river market / canopy-edge settlement;
- rare herbs, resin, tropical food, venom materials, flexible wood/bamboo resources.

Dungeon:

- overgrown stepped temple / flooded root vault;
- exterior canopy + interior stone + root chambers.

Reward identity:

- poison/status builds;
- mobility and rapid attack;
- herbal/alchemy upgrades;
- nature/earth spell variants.

---

## R06 — Mirewater delta / swamps

World role:

- water and land interlock;
- challenge comes from positioning/line-of-sight rather than permanent movement slowdown;
- boardwalks, shallow channels, ruins and giant trees create landmarks.

Ecology:

- crocodile, caiman, snapping turtle;
- wetland ambience such as mudskipper/shoebill equivalents;
- anaconda where appropriate;
- Diplocaulus as resource creature;
- Hydra as signature major predator.

Encounter hierarchy:

- common: smaller wetland predators and territorial reptiles;
- elites: large crocodilian / venomous encounters;
- field boss: Hydra in a river basin or flooded ruin;
- dungeon boss remains open pending better swamp-fit candidate inspection.

Resources / settlement:

- raised-platform settlement / alchemist enclave;
- swamp herbs, poison reagents, clay, wetland fiber and river resources.

Dungeon:

- flooded observatory / sunken shrine;
- water depth changes routes without forcing prolonged awkward swimming combat.

Reward identity:

- poison/bleed control;
- healing amplification;
- water mobility;
- anti-grab effects;
- alchemy accessories.

---

## R07 — Sunscar desert / oasis / badlands

World role:

- wide sightlines and exposed travel;
- giant sand/earth-worm fantasy belongs here;
- danger should often be visible before it reaches the player.

Ecology:

- gazelle, jerboa, roadrunner-style ambience;
- rattlesnake;
- Guster if current behavior fits project spawn control;
- selected vultures only if visual style fits;
- Copas as food + alloy ecology;
- Desert Beetle as heavy roaming threat;
- Armor of Desert as elite/guardian;
- Ferox Deathworm as signature giant world boss.

Encounter hierarchy:

- common: snakes, sand spirits and smaller predators;
- elites: Desert Beetle / Armor of Desert;
- world boss: Ferox Deathworm in a major sand basin;
- dungeon boss: authored guardian variant in buried ruins.

Resources / settlement:

- oasis trade town / caravan hub;
- Copas meat/alloy, dry herbs, silica/glass resources and desert minerals;
- caravans connect R07 to R09 and R01 economy.

Dungeon:

- buried palace / collapsed aqueduct beneath oasis;
- sandfall and vertical shafts alter traversal without repetitive traps.

Reward identity:

- impact/armor penetration;
- stamina efficiency;
- black-powder/ranged materials;
- desert guardian equipment.

---

## R08 — Bloomveil fairy forests / pollinating cliffs

World role:

- visually fantastical fairy forest / great flower forest / pollinating cliffs;
- deliberately more magical than grounded R01/R02 woodland.

Ecology:

- hummingbird;
- Flutter;
- Bunfungus and selected non-hostile fantastical creatures;
- rare Sunbird only if it does not dilute R03;
- Knowledge Fairy in authored library/ruin encounters;
- Nature Spirit variants;
- Moonpriest as high-tier magical encounter;
- Titan Rabbit as strange field/world boss.

Encounter hierarchy:

- common: magical wildlife and sparse aggressive flora/fae threats;
- elites: Nature Spirit variants / Moonpriest groups in authored spaces;
- field boss: Titan Rabbit;
- dungeon boss: Moonpriest or better current external caster after animation review.

Resources / settlement:

- secluded sanctuary / scholar enclave;
- mana flora, rare pigments, magical pollen, light materials, spellcraft ingredients.

Dungeon:

- fae archive / glass-and-root observatory / overgrown library;
- puzzles stay light enough that combat/exploration remains primary.

Reward identity:

- Mana sustain;
- spell shaping;
- support/healing interactions;
- status conversion;
- magical accessories.

---

## R09 — Southstone marches / dry rocky belt

World role:

- harsh southern transition belt of rocky shore, dry grassland, eroded terrain and trade roads;
- caravan gameplay and heavy physical enemies prepare the player for R10.

Ecology:

- rhinoceros;
- elephant where terrain supports herds;
- kangaroo/maned-wolf-style dryland wildlife where appropriate;
- selected raptors/insects for ambience;
- Flamehorn as signature heavy creature;
- Steelboar in limited pockets;
- Executioner only in authored forts/ruins.

Encounter hierarchy:

- common: dryland wildlife and smaller predators;
- elites: Flamehorn / armored charge enemies;
- field boss: high-stat herd leader or stronger regional giant after quality check;
- dungeon boss: Executioner if 26.2 presentation passes review.

Resources / settlement:

- caravan fort / foundry outpost;
- dense metal, clay, hardstone, hide, dry medicinal plants;
- trade routes make the region an economy connector rather than combat-only zone.

Dungeon:

- ruined canyon fortress with battlements, foundry and collapsed lower vault.

Reward identity:

- heavy armor;
- guard/counter builds;
- impact resistance;
- merchant/travel utility;
- high-quality metal components.

---

## R10 — Cinderfall volcanic ash island

World role:

- visually isolated late-game volcanic destination;
- reachable early by reckless traversal rather than story-only teleport.

Ecology:

- Scorch Golem;
- Basalt Wyvern;
- Inferno as signature boss candidate;
- further fire creatures only if they outperform these visually and mechanically;
- no Blaze/Magma Cube population.

Encounter hierarchy:

- common: curated ashland creatures still to be finalized after dependency inspection;
- elites: Scorch Golem / Basalt Wyvern;
- field boss: wyvern alpha or equivalent cliff encounter;
- major boss: Inferno in a volcanic forge/caldera.

Resources / settlement:

- fortified refuge / forge enclave;
- volcanic glass, high-grade ore, fire-reactive mineral, ash reagent;
- high-tier smithing materials begin here but are not exclusively RNG boss drops.

Dungeon:

- caldera forge / buried volcanic foundry;
- heat communicated through hazards/telegraphs rather than permanent maintenance UI.

Reward identity:

- fire/impact builds;
- high-grade smithing;
- charged weapon effects;
- explosive/black-powder advancement;
- high-poise gear.

---

## R11 — Inner Sea, pirate coast and abyssal routes

World role:

The central water network is a layered region rather than empty travel space.

Subzones:

- Lv 28 coast / harbors / shallow islands;
- Lv 44 open sea / reefs / pirate routes;
- Lv 64 abyssal trenches / deep temples.

Ecology:

Threateningly Mobs Continued candidates:

- Hippofish and Red Triplefish as food/resource fish;
- Giant Sea Cucumber as medicinal/material ecology;
- Beast Horseshoe Crab as heavy coast/ocean threat;
- Riptooth as night/ocean predator;
- Abyss Fang as deep-water boss.

Alex's Mobs Continued candidates:

- orca;
- hammerhead shark;
- frilled shark;
- giant squid;
- cachalot whale;
- lobster, flying fish, comb jelly and curated ambience.

No vanilla cod/salmon/tropical fish/dolphin/squid population. If a dependency expects vanilla fish in recipes, project recipes/loot should redirect to custom aquatic food where feasible.

Encounter hierarchy:

- common: passive sea life and low-density predators;
- elites: sharks / Beast Horseshoe Crab;
- field boss: Riptooth or large roaming predator;
- deep boss: Abyss Fang.

Resources / settlement:

- major harbor/pirate-trade settlement plus small island ports;
- seafood economy, reef reagents, medicinal sea-cucumber materials, abyssal minerals.

Dungeon archetypes:

1. pirate-cove / sea-fort progression dungeon;
2. deep reef / abyssal temple late-game dungeon.

Reward identity:

- water mobility;
- projectile control;
- bleed/predator effects;
- fishing/resource utility;
- abyssal spell/weapon variants.

---

## R12 — Obsidian Rift / overgrown anomaly

World role:

- strongest currently planned open-world anomaly;
- sparse rather than crowded;
- fewer creatures with larger encounter footprints and stronger environmental storytelling;
- not presented as a mandatory “final biome.”

Ecology:

- Farseer;
- Murmur;
- selected fantastical entities only if Nether/End assumptions can be detached cleanly;
- Reaper;
- Lich/Moonpriest/Executioner only in authored ruins where reuse does not dilute earlier identities;
- Terradragon as current high-end world-boss candidate;
- avoid Skreecher unless Warden-summon behavior can be disabled/replaced because vanilla Wardens are excluded.

Encounter hierarchy:

- common: low-density anomaly creatures;
- elites: Farseer / Reaper-tier encounters;
- field/world boss: Terradragon;
- separate rift/cathedral boss remains open if Terradragon works better as an open-world spectacle.

Resources / settlement:

- no large ordinary city inside anomaly;
- one edge refuge / research camp;
- rift crystal, obsidian-glass material, arcane ore and late spellcraft components;
- earlier materials remain useful.

Dungeon:

- obsidian cathedral / rift excavation descending into overgrown caves;
- telegraphed high-impact attacks and vertical sightlines instead of inflated HP.

Reward identity:

- build-defining relics;
- class-skill variants;
- high-end arcane/status mechanics;
- unusual effects rather than only highest numbers.

---

# 4. Adjacency / traversal graph

The world does not play as R01 → R02 → R03 → ... in a straight line.

Preferred broad graph before coordinate audit:

```text
                 R04 Frozen Crown
                  /          \
             R03 Highlands   R05 Jungle/Bamboo
               |   \          |      \
               |    R02 Forest|       R08 Bloomveil
               |       \      |       /
               |        R01 Heartland
               |        /   \      \
             R12 Rift  R09 Marches -- R06 Mirewater -- R07 Desert
                \       \                \              |
                 \------- R11 Inner Sea -----------------+
                           \             /
                            ---- R10 Cinderfall
```

Design intent:

- R01 offers at least two sensible routes plus one obviously dangerous direction;
- R02/R03 form an early western/northern branch;
- R04/R05 are peer expedition-tier destinations rather than sequential requirements;
- R06/R07 form a midgame eastern branch;
- R08/R09 are peer high-tier routes with very different combat/ecology;
- R11 acts as a cross-map connector with its own layered difficulty;
- R12 is discoverable from multiple approaches rather than behind one mandatory story door.

Actual roads/coastlines must follow the imported terrain.

---

# 5. Spawn / ecology implementation contract

Regional content depends on strict spawn ownership.

## 5.1 Principle

The project does **not** replace Minecraft spawning with a completely custom every-tick entity search unless profiling proves that necessary. Reuse Minecraft/Fabric's existing spawn machinery where it is good, then put project ownership around it.

Goals:

- no natural vanilla living-mob population;
- no donor mod globally dumping its entire roster into Azari;
- common ecology uses efficient natural spawning;
- elites, bosses, events and dungeon encounters are authored systems rather than random biome-spawn noise;
- multiplayer server owns spawn/encounter truth.

## 5.2 Fabric 26.2 biome spawn layer

Fabric API 26.2 exposes `BiomeModificationContext.MobSpawnSettingsContext` with:

- `getMobs(...)`;
- `addSpawn(...)`;
- `removeSpawns(...)`;
- `removeSpawnsOfEntityType(...)`;
- `clearSpawns(category)`;
- `clearSpawns()`.

Fabric's modification phases run in this order:

1. `ADDITIONS`
2. `REMOVALS`
3. `REPLACEMENTS`
4. `POST_PROCESSING`

For this project, the preferred M0 architecture is:

1. mark every biome actually used by the authored Azari world with project data/tags after import audit;
2. in a late project-owned biome modification, clear ordinary biome spawn entries for those authored biomes;
3. rebuild only the approved custom creature spawn table for each terrain/biome family;
4. preserve the original third-party entity spawn-placement rules unless they conflict with the project;
5. use region/biome-specific weights, group sizes and density instead of enabling donor defaults globally.

Because the project needs to remove spawn additions from dependencies as well as vanilla entries, a late `REPLACEMENTS` or `POST_PROCESSING` project modifier is safer than adding approved mobs first and then clearing in `REMOVALS`. Exact ordering is verified against the final dependency set during M0 rather than guessed.

## 5.3 Natural ecology vs authored encounters

Use Minecraft's natural-spawn machinery mainly for:

- passive/neutral wildlife;
- ordinary predators;
- low-tier hostile creatures;
- ambient aquatic/air creatures.

Do **not** put the following into ordinary uncontrolled biome spawning by default:

- named elites;
- field/world bosses;
- dungeon bosses;
- quest targets;
- event monsters;
- rare signature creatures whose appearance should matter.

Those use project encounter controllers, authored locations, event conditions or explicit spawn anchors so rarity, telegraphing, respawn and rewards remain reliable.

## 5.4 Vanilla special-spawn paths

Clearing biome spawn lists alone is not enough to satisfy the no-vanilla-mob rule.

Minecraft 26.2 has separate game rules for ordinary mob spawning and special systems including patrols, phantoms, wandering traders, Wardens and spawner blocks. The project disables the relevant vanilla special spawn systems in the authored RPG world and removes/replaces imported vanilla spawner blocks.

Do **not** simply turn `spawn_mobs` off globally if the custom ecology still relies on normal mob spawning; doing so would suppress the project creatures too. Instead:

- clear/rebuild biome spawn tables;
- disable vanilla-only special spawn systems individually;
- replace structure spawners;
- audit dependency-specific custom spawners/event spawners;
- add a narrow server-side safety gate only for vanilla EntityTypes that still leak through an unhandled path.

The safety gate is a last line of defense, not the primary ecosystem implementation. It should reject only forbidden vanilla living entities in the authored world and must not interfere with project NPCs, custom mobs, explicit developer test summons or legitimate scripted encounters.

## 5.5 Imported structures / map conversion

During Azari and structure import:

- scan/remove vanilla mob spawner blocks from adopted dungeons;
- remove vanilla villagers/animals/entities saved into imported structures;
- replace vanilla encounter markers with project anchors/data;
- keep architecture when strong, not the donor encounter table;
- validate that newly loaded chunks do not repopulate vanilla mobs through donor datapacks or structures.

## 5.6 Dependency spawn ownership

For Alex's Mobs Continued, Threateningly Mobs Continued and later creature dependencies:

- prefer dependency config/data options to disable global default spawning when available;
- if not sufficient, strip their biome entries in the project late biome-modification pass and re-add only approved entities;
- do not fork/copy a large dependency merely to change spawn weights unless its license and maintenance cost justify it;
- donor structures, dimensions, recipes and loot remain non-canonical unless separately adopted.

## 5.7 Density / performance

A 30k authored map does not mean all chunks simulate ecology at once. Normal Minecraft chunk/player proximity rules remain useful.

Additional rules:

- avoid persistent mobs unless gameplay requires persistence;
- keep ambient packs small enough to avoid pathfinding/entity-count spikes;
- do not simulate predator/prey ecology globally offscreen;
- boss/event controllers sleep when no relevant player is nearby;
- no every-tick scan of the whole world or every loaded entity;
- profile before changing spawn radius/caps/pathfinding budgets.

---

# 6. External creature source mapping

Current principal dependencies/candidates already tracked in `EXTERNAL_SOURCES.md`.

## Alex's Mobs Continued

Use primarily for diverse wildlife, predators and selected fantasy creatures:

- R01/R02: gazelle, bison, raccoon, crow, grizzly, cave centipede;
- R03: highland wildlife, Sunbird, Rocky Roller;
- R04: moose, seal, snow leopard, Froststalker;
- R05: gorilla, capuchin, toucan, tiger, komodo, anaconda, leafcutter ants;
- R06: crocodile, caiman, snapping turtle, wetland wildlife;
- R07: jerboa, roadrunner, rattlesnake, Guster;
- R08: hummingbird, Flutter, Bunfungus;
- R09: rhinoceros, elephant and dryland wildlife;
- R11: orca, sharks, giant squid, cachalot, lobster/flying-fish/comb-jelly ecology;
- R12: Farseer, Murmur and selectively adapted fantastical entities.

This is a dependency roster, not permission to enable its full default spawn list.

## Threateningly Mobs Continued

Use primarily for distinct high-tier monsters, bosses and replacement resource creatures:

- R01: Louxia, Steelboar, Regalhart, Earthloong introduction;
- R02: Nature Spirit, Lich authored encounter;
- R03: Basalt Wyvern candidate;
- R04: Ferox Iceworm, Icebroodmother candidates;
- R05: mature Earthloong;
- R06: Diplocaulus, Hydra;
- R07: Copas, Desert Beetle, Armor of Desert, Ferox Deathworm;
- R08: Knowledge Fairy, Nature Spirit variants, Moonpriest, Titan Rabbit;
- R09: Flamehorn, Executioner;
- R10: Scorch Golem, Basalt Wyvern, Inferno;
- R11: Hippofish, Red Triplefish, Giant Sea Cucumber, Beast Horseshoe Crab, Riptooth, Abyss Fang;
- R12: Reaper and Terradragon candidates.

Every candidate remains subject to real 26.2 visual/behavior inspection. If an external mob looks or plays worse than the surrounding game, replace the candidate rather than preserving it because it appears in this planning document.

---

# 7. Content-density rules

Each major region eventually needs, at minimum:

- one safe service node or a deliberate reason to have none;
- several recognizable traversal landmarks;
- multiple common ecology roles, not only hostile mobs;
- at least 2–3 authored elite/hunt patterns across subregions;
- one field/world-boss direction where appropriate;
- at least one dungeon/major interior or deliberate alternate signature activity;
- regional gathering/resource identity;
- at least one meaningful reward family/build hook;
- events/quests that use geography rather than generic kill counters;
- return reasons after first completion without daily-chore structure.

Do not satisfy these counts by duplicating the same encounter with a different name.

---

# 8. Next regional work

Before source bootstrap:

1. import the actual Azari world locally and confirm which published biome/terrain family corresponds to each provisional region;
2. draw exact R01–R12 borders from rivers, mountain chains, coastlines and biome transitions;
3. inspect selected current Fabric 26.2 creatures in-game for model/animation/AI quality;
4. inspect each creature dependency's config/spawn registration path, then decide whether config suppression or the Fabric late biome-rebuild layer owns its natural spawning;
5. reject or replace candidates that leak vanilla entities, require unsuitable donor progression, or have poor visual quality;
6. select actual external structures/dungeon shells and settlement architecture per region;
7. run the economy/EXP benchmark pass and rescale suggested entry levels together rather than reopening region identities;
8. only after the real spawn table is implemented, profile loaded-entity counts/pathfinding under representative multiplayer conditions before changing caps or spawn frequency.
