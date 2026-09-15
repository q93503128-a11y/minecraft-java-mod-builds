# Open-World RPG — Regional Content Reference

> Status: subordinate design reference  
> Master canon: `GAME_DESIGN.md`  
> Terrain basis: Azari 30k x 30k, current primary free world candidate  
> Loader: Fabric 26.2  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins.

This file expands the regional design requested by `GAME_DESIGN.md` without becoming a competing master document. Exact borders and coordinates remain provisional until the Azari world is imported and inspected in-game. Region identity, ecology, encounter roles and adjacency should survive coordinate refinement.

---

# 1. World-shape reading

The published Azari overview supports a strong authored RPG layout rather than a linear chain:

- a broad central inland sea / lake network with islands and peninsulas;
- a huge frozen northern crown;
- green western and northeastern landmasses with forests, meadows and river systems;
- high mountain / rocky terrain to the west and southwest;
- an eastern desert / oasis / badlands mass;
- a distinct southeastern volcanic-ash landmass;
- southern rocky, coastal and eroded terrain;
- dark obsidian-like pockets around the western-central interior;
- enough shoreline, reef, ocean and island space for a separate maritime layer.

The region graph must therefore be branching. Recommended Lv bands are guidance, not doors. A player can physically ride, walk or sail into a stronger region early and retreat if it is too dangerous.

Current working bands are intentionally provisional until the EXP benchmark pass. When the global curve changes, rescale all bands together rather than redesigning the world.

---

# 2. Region summary

| ID | Working terrain identity | Working Lv band | Main play identity | Signature danger |
|---|---|---:|---|---|
| R01 | central/south-central meadow, river, forest fringe | 1–8 | starting hub, basic gathering, first dungeon | Steelboar / Regalhart / Earthloong |
| R02 | western rich forest and river basin | 6–16 | dense woodland exploration, herbs, timber, old ruins | Nature Spirit / Lich |
| R03 | western/southwestern great mountains and windswept ridges | 12–24 | vertical routes, mining, exposed passes | Rocky Roller / Basalt Wyvern |
| R04 | northern ice crown, freezing taiga, ice ocean edge | 18–30 | cold expedition, visibility/weather pressure | Froststalker / Ferox Iceworm / Icebroodmother |
| R05 | northeastern jungle and bamboo-rich green belt | 24–36 | dense traversal, predators, rare plants | Tiger / Komodo / mature Earthloong |
| R06 | eastern river delta, wetland and swamp mosaic | 30–42 | water/land combat, alchemy ecology | Crocodile/Caiman / Hydra |
| R07 | eastern desert, oasis and badlands | 36–50 | heat, open sightlines, ambushes, giant-monster hunt | Desert Beetle / Armor of Desert / Ferox Deathworm |
| R08 | flower forest, fairy/whimsical forest, pollinating cliffs | 42–56 | magical ecology, vertical floral terrain, strange discoveries | Nature Spirit variants / Moonpriest / Titan Rabbit |
| R09 | southern rocky marches, dry grasslands and eroded coast | 48–62 | hard overland travel, caravan routes, impact enemies | Flamehorn / Rhino / Executioner |
| R10 | southeastern volcanic ash island and obsidian lava terrain | 56–70 | fire/impact combat, volcanic resources | Scorch Golem / Basalt Wyvern / Inferno |
| R11 | central inland sea, pirate coast, reefs and deep-ocean routes | 48–76 by subzone | naval/coastal exploration, fishing, underwater danger | Riptooth / Beast Horseshoe Crab / Abyss Fang |
| R12 | western-central obsidian spikes, overgrown caves and anomaly pockets | 72–90 | late-game abnormal ecology, rift dungeons | Farseer / Reaper-Lich tier / Terradragon |

R11 deliberately spans a wider range because coastline and shallow water should be accessible before the abyssal/deep-ocean content. R12 is not a mandatory "final biome"; it is simply the strongest currently planned open-world danger band.

---

# 3. Regional packages

## R01 — Heartland starting region

Canonical details live in `GAME_DESIGN.md`.

Core identity:
- meadow / plains / river / forest-fringe start;
- Louxia-led non-vanilla food ecology;
- gazelle, bison, raccoon, crow and rare grizzly as curated wildlife;
- common threats include rattlesnake/cave-centipede-style enemies and readable territorial animals;
- Steelboar and Nature Spirit are early elite signals;
- Regalhart is an optional field-boss candidate;
- root-overgrown quarry is the first replayable dungeon;
- Earthloong is the current dungeon-boss candidate;
- first non-vanilla ground mount arrives early and is around sprint-speed convenience rather than a huge speed skip.

R01 foreshadows neighboring ecology. A rare Nature Spirit at the forest edge should imply that the western forest contains much stronger versions of that ecosystem rather than feeling like a random one-off monster.

---

## R02 — Western rich forest / river basin

### World role

R02 is the first region where exploration density rises sharply. Roads become narrower, landmarks are hidden by canopy and rivers create natural route choices. It should feel more mysterious than R01 without becoming oppressive.

### Ecology

Curated Alex's Mobs Continued pool:
- raccoon, crow and selected small wildlife for ambient life;
- grizzly bears as territorial hazards rather than constant aggro trash;
- bunfungus / other visually compatible fantasy wildlife only where it strengthens the forest identity;
- cave centipedes in root caves and abandoned cuts.

Threateningly Mobs Continued:
- Nature Spirit becomes a native major threat rather than a rare R01 teaser;
- Regalhart may appear as an uncommon regional species/hunt after the player has already seen the R01 field boss version;
- Lich is reserved for authored ruins instead of random global spawn.

### Encounter hierarchy

- common: small forest predators/ambushers and territorial wildlife;
- elite: mature Nature Spirit, ancient Regalhart variants;
- field encounter: roaming forest guardian hunt with multiple route/terrain approaches;
- dungeon boss: Lich in a ruined woodland sanctum, with project-authored attacks/rewards rather than donor progression.

### Services / resources

- one forest settlement or logging/trade hamlet, smaller than the starting hub;
- hardwood/timber, resin, mushrooms, healing plants and mana-active flora;
- deeper forest nodes reward route knowledge rather than simple distance from town.

### Dungeon direction

Ancient woodland sanctum / collapsed arboretum. Use a strong external ruin shell, then replace encounters and loot. Branching side rooms should expose lore/rare herbs rather than extend combat length for no reason.

### Signature reward identity

DEX/WIL utility, poison/status resistance, forest movement, bow/finesse options and nature-linked accessories rather than raw universal damage upgrades.

---

## R03 — Whitecrest highlands / windswept mountains

### World role

First major vertical-traversal region. Cliffs and passes matter, but ordinary movement must remain usable before late-game flight. The region introduces valuable mineral routes and long sightlines toward stronger lands.

### Ecology

Alex's Mobs Continued candidates:
- bison in lower upland meadows;
- bald-eagle / highland bird equivalents where available;
- gelada monkey in rocky highland pockets;
- snow leopard near the colder upper transition;
- Sunbird as rare high-altitude fantasy wildlife;
- Rocky Roller in caves and exposed stone corridors.

Threateningly Mobs Continued:
- Basalt Wyvern as a strong highland elite / field-boss candidate where its current 26.2 behavior fits;
- Steelboar can persist in lower passes but at low density.

### Encounter hierarchy

- common: highland wildlife, cave creatures, rolling/charge enemies;
- elite: Rocky Roller packs in authored rockfall zones, highland wyvern encounters;
- field boss: Basalt Wyvern on a high exposed plateau;
- dungeon boss: source remains open until in-game inspection identifies the strongest current 26.2 mountain-fit external monster. Do not force a weak thematic match merely to fill the slot.

### Services / resources

- fortified mining town / cliff outpost;
- iron-family ore, dense stone, silver-like mineral, highland crystal;
- traversal shortcuts unlock through lifts/bridges/shrine routes rather than permanent movement-stat chores.

### Dungeon direction

Collapsed cliff mine → ancient observatory/forge depth. Vertical loops, elevators/bridges and one exterior cliff segment should distinguish it from R01's quarry.

### Signature reward identity

Guard strength, poise damage, heavy weapons, ranged weak-point gear and movement-on-slope/knockback resistance.

---

## R04 — Northern frozen crown

### World role

Large northern cap using Azari's ice spikes, freezing taiga, ice ocean and freezing-abyss terrain. Weather and visibility can matter, but the player should not spend the region maintaining a tedious temperature meter unless a later playtest proves such a system actually improves the game.

### Ecology

Alex's Mobs Continued:
- moose;
- seals near the ice-ocean edge;
- snow leopard;
- Froststalker as a primary hostile fantasy predator.

Threateningly Mobs Continued candidates:
- Ferox Iceworm as a major field/world-boss candidate;
- Icebroodmother as a dungeon-boss candidate if the current 26.2 port retains acceptable behavior/animation.

### Encounter hierarchy

- common: cold-adapted wildlife + Froststalker pockets;
- elite: large Froststalker hunts / ice-cave predators;
- field boss: Ferox Iceworm breaking through snow/ice routes;
- dungeon boss: Icebroodmother in a glacial cavern or frozen nest.

### Services / resources

- expedition fort / geothermal refuge rather than a giant city;
- fur/hide, frost crystal, cold-water ingredients, rare winter herbs;
- frozen shoreline connects into R11 maritime content.

### Dungeon direction

Glacial fissure / frozen nest with shifting sightlines, breakable ice shortcuts and a final wide arena. Avoid slippery-floor gimmicks in every room.

### Signature reward identity

Frost/status control, high poise, defensive accessories, cold-element spell/weapon variants and anti-slow mobility.

---

## R05 — Northeastern jungle / bamboo greenbelt

### World role

Dense high-biodiversity land. Navigation is constrained by vegetation and rivers rather than invisible walls. Vertical canopy landmarks should make the region readable from a distance.

### Ecology

Alex's Mobs Continued:
- gorilla;
- capuchin monkey;
- toucan and other jungle ambience;
- tiger as a dangerous stalker;
- komodo dragon;
- anaconda where the current roster/behavior is stable;
- leafcutter ants and selected insects for ecological density rather than combat spam.

Threateningly Mobs Continued:
- mature Earthloong becomes a native high-tier jungle/forest threat. R01's dungeon encounter functions as the player's early introduction to the species, not its only appearance.

### Encounter hierarchy

- common: insects/reptiles and avoidable predator encounters;
- elite: tiger/komodo/anaconda variants where project Lv tuning makes them mechanically distinct;
- field boss: mature Earthloong in an ancient grove/river-temple approach;
- dungeon boss: keep open for a high-quality external jungle/temple monster after current 26.2 dependency inspection; do not recycle another biome's boss purely for completeness.

### Services / resources

- river market / canopy-edge settlement;
- rare herbs, resin, tropical food, venom materials, flexible wood/bamboo-like crafting resources;
- some plants are alchemy ingredients unavailable in R01/R02, giving real trade value.

### Dungeon direction

Overgrown stepped temple / flooded root vault. Routes alternate between exterior canopy, interior stone and root-filled chambers.

### Signature reward identity

Poison/status builds, mobility, rapid attack, herbal/alchemy upgrades and nature/earth spell variants.

---

## R06 — Mirewater delta / swamps

### World role

Water and land interlock. The challenge is positioning and line-of-sight, not constant slow movement. Boardwalks, shallow channels, ruined towers and large trees create navigation landmarks.

### Ecology

Alex's Mobs Continued:
- crocodile and caiman;
- alligator snapping turtle;
- mudskipper / shoebill-style wetland ambience;
- anaconda where appropriate.

Threateningly Mobs Continued:
- Diplocaulus as valuable passive/breedable meat/leather/bone ecology;
- Hydra as the region's signature major predator.

### Encounter hierarchy

- common: smaller wetland predators and territorial reptiles;
- elite: large crocodilian / venomous wetland encounters;
- field boss: Hydra in a large river basin or flooded ruin;
- dungeon boss: currently open pending inspection of external swamp-fit candidates; quality beats filling every slot early.

### Services / resources

- raised-platform settlement / alchemist enclave;
- medicinal fluids/materials from Diplocaulus under normalized project loot;
- swamp herbs, poison reagents, clay, wetland fiber and river resources.

### Dungeon direction

Flooded observatory / sunken shrine. Water depth changes routes but must not turn combat into awkward swimming for the entire dungeon.

### Signature reward identity

Poison/bleed control, healing amplification, water mobility, anti-grab effects and alchemy-oriented accessories.

---

## R07 — Sunscar desert / oasis / badlands

### World role

A broad exposed region where sightlines are long and major threats can be seen before they reach the player. The giant sand/earth-worm fantasy belongs here.

### Ecology

Alex's Mobs Continued:
- gazelle, jerboa and roadrunner-style prey/ambient wildlife;
- rattlesnake;
- Guster as a supernatural sand threat if its current behavior remains stable under project spawn control;
- selected vultures only when they visually fit the non-vanilla identity.

Threateningly Mobs Continued:
- Copas as passive/breedable food + alloy ecology in badland/hill edges;
- Desert Beetle as heavy roaming threat;
- Armor of Desert as elite/guardian;
- Ferox Deathworm as the signature giant desert world-boss candidate.

### Encounter hierarchy

- common: snakes, desert spirits and smaller predators;
- elite: Desert Beetle / Armor of Desert;
- field/world boss: Ferox Deathworm roaming or surfacing in a marked sand basin;
- dungeon boss: Armor of Desert-type guardian in a buried ruin, with a different authored moveset/reward role from roaming elites.

### Services / resources

- oasis trade town / caravan hub;
- Copas meat/alloy, dry herbs, glass/silica resources, desert mineral nodes;
- caravan events connect the region to R09 and R01 economy.

### Dungeon direction

Buried palace / collapsed aqueduct beneath an oasis. Sandfall and vertical shafts should change traversal without turning into repetitive falling-block traps.

### Signature reward identity

Impact/armor penetration, heat/fire preparation, stamina efficiency over long fights, black-powder/ranged materials and desert guardian gear.

---

## R08 — Bloomveil fairy forests / pollinating cliffs

### World role

A visually fantastical region using Azari's fairy forest, whimsical forest, great flower forest and pollinating-cliff families. It should look dramatically different from the grounded R01/R02 forests and justify higher magical density.

### Ecology

Alex's Mobs Continued:
- hummingbird;
- Flutter;
- Bunfungus and selected non-hostile fantastical creatures;
- Sunbird may appear as a very rare sky encounter if it does not overcrowd R03's identity.

Threateningly Mobs Continued:
- Knowledge Fairy for authored library/ruin encounters rather than random bookshelf farming;
- Nature Spirit variants as magical ecology;
- Moonpriest as a high-tier humanoid/magical encounter candidate;
- Titan Rabbit as the deliberately strange field/world-boss candidate.

### Encounter hierarchy

- common: magical wildlife and low-density aggressive flora/fae threats;
- elite: Nature Spirit variants / Moonpriest groups in authored spaces;
- field boss: Titan Rabbit in a huge flowering basin where its scale feels intentional;
- dungeon boss: Moonpriest or another externally sourced magical caster after animation review.

### Services / resources

- secluded sanctuary / scholar enclave rather than another full trade city;
- mana flora, rare pigments, magical pollen, light materials, spellcraft ingredients;
- Knowledge Fairy mechanics are adapted to project skill/progression rewards rather than vanilla enchanted-book economy.

### Dungeon direction

Fae archive / glass-and-root observatory / overgrown library. Puzzle density stays light; the dungeon is still an action-RPG space, not a menu puzzle collection.

### Signature reward identity

Mana sustain, spell-shaping, support/healing interactions, status conversion and visually distinctive magical accessories.

---

## R09 — Southstone marches / dry rocky belt

### World role

A harsh transition belt around Azari's south: rocky shores, dry grassland, eroded land and trade roads. It supports caravans and harder physical enemies before the volcanic island.

### Ecology

Alex's Mobs Continued:
- rhinoceros;
- elephant where terrain width supports herds;
- kangaroo / maned-wolf-style dryland wildlife where appropriate;
- selected raptors/insects for ambience.

Threateningly Mobs Continued:
- Flamehorn as signature hostile/neutral heavy creature;
- Steelboar persists in limited pockets;
- Executioner can occupy authored forts/ruins rather than spawn everywhere.

### Encounter hierarchy

- common: dryland wildlife and smaller predators;
- elite: Flamehorn / armored charge enemies;
- field boss: a high-stat Flamehorn herd leader or externally sourced regional giant after in-game quality check;
- dungeon boss: Executioner in a ruined road fortress if its current 26.2 presentation is strong enough.

### Services / resources

- caravan fort / foundry outpost;
- dense metal, clay, hardstone, hide, dry medicinal plants;
- trade routes make this a currency/source-sink connector rather than a region that only exists for combat.

### Dungeon direction

Ruined canyon fortress with exterior battlements, interior foundry and collapsed lower vault. Enemy placement should make use of height without ranged spam.

### Signature reward identity

Heavy armor, guard/counter builds, impact resistance, merchant/travel utility and high-quality metal components.

---

## R10 — Cinderfall volcanic ash island

### World role

Azari's southeastern red volcanic landmass is visually isolated enough to feel like a major destination. It is a late-game region, but it should be reachable early by a reckless player via coast/route rather than story teleport only.

### Ecology

Threateningly Mobs Continued is the primary current source:
- Scorch Golem as a heavy normal/elite enemy depending project tuning;
- Basalt Wyvern in cliffs/ash skies;
- Inferno as the signature world/dungeon boss candidate.

Additional fire creatures should be added only from current Fabric 26.2 sources that visually outperform these; no vanilla Blaze/Magma Cube substitute population is allowed.

### Encounter hierarchy

- common: ashland creatures and smaller externally sourced fire threats still to be selected after M0 dependency inspection;
- elite: Scorch Golem / Basalt Wyvern;
- field boss: Basalt Wyvern alpha or equivalent cliff encounter;
- major boss: Inferno in a volcanic forge/caldera dungeon.

### Services / resources

- one fortified refuge / forge enclave rather than natural farmland;
- volcanic glass, high-grade ore, fire-reactive mineral, ash reagent;
- highest-tier smithing materials begin here but are not exclusively RNG boss drops.

### Dungeon direction

Caldera forge / buried volcanic foundry. Heat is communicated through hazards/telegraphs rather than a constant UI maintenance bar.

### Signature reward identity

Fire/impact builds, high-grade smithing, charged weapon effects, explosive/black-powder advancement materials and high-poise equipment.

---

## R11 — Inner Sea, pirate coast and abyssal routes

### World role

The huge central water network is not empty travel space. Shallows, islands, reef, pirate cove and deep-water routes form a maritime region layered across multiple Lv bands.

Subzones:
- coast/shallow islands: roughly midgame-accessible;
- open sea / reef: stronger encounters;
- deep trenches / freezing-abyss-linked waters: late-game danger.

### Ecology

Threateningly Mobs Continued:
- Hippofish and Red Triplefish for breedable/resource fish ecology;
- Giant Sea Cucumber for medicinal/material ecology;
- Beast Horseshoe Crab as heavy coast/ocean threat;
- Riptooth as dangerous night/ocean predator;
- Abyss Fang as late deep-water boss.

Alex's Mobs Continued:
- orca;
- hammerhead shark;
- frilled shark;
- giant squid;
- cachalot whale;
- lobster, flying fish, comb jelly and other curated marine ambience.

No vanilla cod/salmon/tropical fish/dolphin/squid population is required. If a dependency's taming/breeding recipe expects vanilla fish, project recipes/loot should be redirected to custom aquatic food where technically possible rather than bringing vanilla fish back as visible ecology.

### Encounter hierarchy

- common: passive fish/sea life + low-density predators;
- elite: hammerhead/frilled-shark encounters and Beast Horseshoe Crab;
- field boss: Riptooth night hunt or large roaming sea predator;
- deep boss: Abyss Fang in an underwater altar/cavern reauthored around project progression.

### Services / resources

- one major harbor/pirate-trade settlement plus small island ports;
- fish/seafood economy, coral/reef reagents, medicinal sea cucumber materials, abyssal minerals;
- fishing becomes a meaningful side economy because the visible aquatic roster is custom rather than vanilla fish filler.

### Dungeon direction

Two reusable archetypes:
1. pirate-cove / sea-fort dungeon for ordinary maritime progression;
2. deep reef / abyssal temple for late-game underwater content.

Underwater combat sections should be designed for project movement and ranged/melee viability rather than forcing prolonged vanilla swimming combat.

### Signature reward identity

Water mobility, projectile control, bleed/predator effects, fishing/resource utility and abyssal spell/weapon variants.

---

## R12 — Obsidian Rift / overgrown anomaly

### World role

Use Azari's dark western-central pockets, obsidian-spike terrain and overgrown-cave systems as the strongest current open-world anomaly. It should feel abnormal without requiring Nether/End progression.

The region is sparse rather than crowded: fewer creatures, larger encounter footprints and more environmental storytelling.

### Ecology

Alex's Mobs Continued candidates:
- Farseer as an abnormal ranged/interdimensional threat;
- Murmur in deep cave systems;
- selected fantastical entities only if their source behavior can be detached from Nether/End assumptions cleanly.

Threateningly Mobs Continued:
- Reaper;
- Lich / Moonpriest / Executioner only in authored ruins where reuse does not dilute their earlier regional identities;
- Terradragon as the current high-end world-boss candidate.

Avoid Skreecher unless its Warden-summon behavior can be cleanly disabled/replaced, because vanilla Wardens are excluded from the ecology.

### Encounter hierarchy

- common: low-density anomaly creatures; ordinary enemies are individually more meaningful rather than numerous;
- elite: Farseer / Reaper-tier encounters;
- field/world boss: Terradragon in the largest open obsidian basin;
- dungeon boss: separate rift/cathedral boss candidate remains open if Terradragon works better as the region's open-world spectacle.

### Services / resources

- no normal large city inside the anomaly;
- one edge refuge / research camp connected to a nearby safer region;
- rift crystal, obsidian-glass material, arcane ore and late spellcraft components;
- resources should reward dangerous expeditions without making all earlier materials obsolete.

### Dungeon direction

Obsidian cathedral / rift excavation descending into overgrown caves. Combat spaces should emphasize telegraphed high-impact attacks and vertical sightlines, not simply inflated HP.

### Signature reward identity

Late build-defining relics, class-skill variants, high-end arcane/status effects and unusual mechanics rather than only top numeric stats.

---

# 4. Adjacency / traversal graph

The world should not play as R01 → R02 → R03 → ... in a straight line.

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

This graph is conceptual. Actual roads/coastlines must follow imported terrain.

Design intent:
- R01 has at least two sensible progression routes plus one obviously dangerous route;
- R02/R03 offer a western/northern progression branch;
- R05/R06/R07/R08 create a broad eastern loop;
- R09 connects the southern mainland to the volcano and desert trade routes;
- R11 acts as a cross-map maritime connector once appropriate travel is available;
- R12 can be discovered from multiple western/central approaches rather than behind one mandatory story door.

---

# 5. Spawn / ecology implementation contract

Regional content depends on strict spawn ownership.

- Natural vanilla mob population is disabled globally inside authored RPG gameplay.
- External dependencies do not own global spawn balance; project region data does.
- A creature can be globally disabled and manually/regionally re-enabled through project rules if the dependency allows it.
- Imported structures have vanilla spawners replaced.
- Donor-mod dimensions, summon progression, crafting recipes and default loot do not become canon automatically.
- Passive creature density is tuned so the world feels alive without creating entity-count/performance problems on a 30k map.
- Predators do not constantly wipe out food ecology offscreen; ecosystem simulation is deliberately bounded for performance and reliability.
- Normal creatures do not routinely drop equipment.
- Elites/bosses have authored reward pools and deterministic first-clear protection where appropriate.

---

# 6. External creature source mapping

Current principal dependencies/candidates already tracked in `EXTERNAL_SOURCES.md`:

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
- at least 2–3 authored elite/hunt patterns across its subregions;
- one field-boss/world-boss direction where appropriate;
- at least one dungeon/major interior or a deliberate alternate signature activity;
- regional gathering/resource identity;
- at least one meaningful reward family/build hook;
- events/quests that use the region's geography rather than generic kill counters;
- return reasons after first completion without daily-chore structure.

Do not satisfy these counts by duplicating the same encounter with a different name.

---

# 8. Next regional work

Before source bootstrap:

1. import the actual Azari world locally and confirm which published biome/terrain family corresponds to each provisional region;
2. draw exact R01–R12 borders from rivers, mountain chains, coastlines and biome transitions;
3. inspect all selected current Fabric 26.2 creatures in-game for model/animation/AI quality;
4. reject or replace candidates that leak vanilla entities, require unsuitable donor progression, or have poor visual quality;
5. select the actual external structures/dungeon shells and settlement architecture per region;
6. then convert the working Lv bands into the final EXP/Lv curve during the benchmark pass.
