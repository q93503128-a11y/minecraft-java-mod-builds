# Open-World RPG — R07 Sunscar Desert Implementation Package

> Status: **DESIGN CANON — R07 world/story/traversal/service/combat/reward flow is implementation-ready; exact dungeon-boss and several desert-prop bindings remain external-intake gates**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Story spine: `WORLD_STORY_CANON.md`  
> Region graph: `REGIONS.md`  
> Combat: `COMBAT_BALANCE.md`, `STATUS_AND_R01_ENCOUNTERS.md`  
> Loot/equipment: `LOOT_ECONOMY.md`, `EQUIPMENT_BALANCE.md`  
> Field systems: `GATHERING_FISHING_CAMP_HOUSING.md`, `FISHING_COLLECTION_HOUSING_MARKET.md`  
> Quest/state: `QUEST_WORLD_STATE.md`  
> Quality contract: `DESIGN_COMPLETENESS_AUDIT.md`  
> External provenance: `EXTERNAL_SOURCES.md`  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins.

R07 is the midgame desert/badlands region. Its identity is **distance, contrast and route commitment**, not survival-meter maintenance.

Its play sentence is:

```text
leave the wet eastern routes and enter a broad desert where ridges, oasis green, caravan smoke and buried towers are visible from far away
→ travel between reliable caravan stops while choosing exposed fast routes or slower canyon routes
→ learn how settlements survive through wells, cisterns and old subterranean channels instead of an infinite magical oasis
→ hunt desert predators that use sand and open sightlines as part of their combat identity
→ discover that old Anchor-era waterworks once redistributed groundwater across several routes
→ realize the present crisis is not simply “the desert is drying”: remote network commands are pulling water toward obsolete nodes
→ track Ferox Deathworm across open dunes as a native apex hunt
→ descend through a buried aquifer complex / observatory-cistern beneath a ruined desert fortress
→ sever the remote draw while preserving selected local wells and route reservoirs
→ leave with evidence that infrastructure can save lives while central optimization can also sacrifice peripheral communities
```

R07 should make the player think:

> `A system can be useful and still make cruel trade-offs when distant efficiency matters more than local survival.`

This deepens the Act-II debate rather than duplicating R04/R05/R06.

---

# 1. Locked regional identity

Preserved from `REGIONS.md`:

- terrain: eastern desert, oasis, badlands and eroded rock mass;
- suggested entry Lv: **34**;
- R07 is a midgame-harder route adjacent to R06 and later eastern/high-tier paths;
- ecology includes Desert Beetle, Armor of Desert, Ferox Deathworm and desert-fit wildlife;
- R01's rejected Rattlesnake candidate becomes valid here if current external behavior fits;
- settlement direction: caravan oasis / fortified trade town;
- resource identity: dryland herbs, salts/minerals, desert creature materials and scarce but meaningful water-linked resources;
- dungeon direction: buried aquifer / cistern / desert observatory / fortress complex;
- reward identity: anti-burst survival, impact/armor break, ranged precision, mobility across open spaces and selected heat/sand-control utility.

Production clarifications:

- **no baseline thirst meter**;
- **no always-on heat-damage meter**;
- no mandatory “drink every X minutes” loop;
- no global sand movement penalty;
- daytime/nighttime/weather can change ambience, visibility and selected encounter weights without making exploration a consumable tax;
- oasis/wells matter as world/economy/story nodes, not survival bars.

---

# 2. External game-content precedents

These are structural references only. No proprietary maps, art, scripts or balance values are copied.

## 2.1 Assassin's Creed Origins — desert contrast / landmark scale

Useful lessons:

- deserts become memorable through contrast between open dry space, Nile/oasis green, settlements and monumental landmarks;
- large landmarks help orient the player across long sightlines;
- heat, sandstorms and hallucination-like presentation can create atmosphere without requiring a constant survival-management loop;
- settlements and cities feel grounded when water and geography explain where they exist.

Project adoption:

- R07 uses strong long-distance landmarks: oasis trees, mesa silhouettes, buried towers, fortress walls and caravan smoke;
- green/water pockets are visually and economically valuable because the rest of the region is dry;
- one or two sandstorm states alter visibility and event conditions, but ordinary progression does not depend on waiting them out;
- settlements sit where water/trade routes make sense.

Not adopted:

- hallucination systems that pretend to be deep gameplay but only confuse the player;
- huge empty travel stretches justified solely by realism;
- survival damage from heat for simply walking normally.

Reference:

- `https://gamesbeat.com/how-ubisoft-created-the-art-for-the-massive-world-of-assassins-creed-origins/`

## 2.2 Zelda — Gerudo route readability

Useful lessons:

- a flat desert still works when important locations, passes and towns are strongly readable;
- main roads provide a clear route while off-road traversal remains possible;
- ruins and giant remains can work as navigation silhouettes and discovery hooks.

Project adoption:

- one obvious caravan road links the major oasis/town, but players may cut across dunes/badlands toward visible objectives;
- rock ribs, old towers, giant skeleton/ruin forms and mesa edges help form a mental map;
- dangerous shortcuts are communicated through terrain/enemy context rather than invisible walls.

Not adopted:

- universal heat/cold resistance maintenance;
- disabling the player's ordinary mount system for an entire region merely to force a special traversal gimmick;
- deep sand slow everywhere.

Reference direction:

- `https://www.gamedeveloper.com/design/breath-of-the-wild-open-world-analysis-gravity-to-go-forward`

## 2.3 Genshin Impact — Sumeru desert / buried history

Useful lessons:

- a desert can combine storms, oases, vegetation, eroded rock and large ancient ruins for strong visual diversity;
- large underground ruins can add a second spatial layer and historical depth;
- desert fantasy is stronger when geology/history visibly shape the region rather than every area being identical dunes.

Project adoption:

- R07 has desert, oasis, badlands/canyon and buried aquifer/fortress layers;
- underground content is tied to visible surface entrances/landmarks;
- the largest buried complex is one authored dungeon, not a continent-sized invisible maze;
- surface landmarks remain useful after entering/exiting underground routes.

Explicitly not adopted:

- labyrinthine multi-layer underground networks whose navigation quality depends on external fan-made maps;
- excessive clearance/key chains just to open every door;
- hiding half the region underground without readable access logic.

Reference direction:

- Sumeru environment-design statements on desert storms, oases, wind-eroded rock and sacred ruins;
- community mapping of Sumeru's desert demonstrates how quickly multi-layer underground design can become difficult to read.

## 2.4 Monster Hunter: World — Wildspire dry/wet ecology

Useful lesson:

- desert and wet areas can coexist and feed one another ecologically;
- monsters should look like they belong to the terrain and use it naturally;
- major predators can create territory rather than simply filling empty sand with mobs.

Project adoption:

- oasis/wadi areas support different ecology from exposed dunes;
- predators have authored territories;
- route value changes naturally between open dune, canyon and oasis spaces.

Reference:

- `https://news.capcomusa.com/lets/browse/new-ecosystem-revealed-for-monster-hunter-world-the-wildspire-waste`

---

# 3. External-first source stack

## 3.1 Threateningly Mobs Continued — desert identity

Current 26.2 continuation explicitly lists:

- **Desert Beetle** — deserts, badlands and savannas;
- **Armor of Desert** — deserts and badlands.

The original/current Threateningly Mobs lineage also includes **Ferox Deathworm**, documented as a desert/snowland creature with a sand/ambush identity.

Project use:

- Desert Beetle = authored heavy common/elite desert threat, not global spawn spam;
- Armor of Desert = high-tier elite/miniboss direction around old ruins/fortress routes if visual/behavior review passes;
- Ferox Deathworm = optional R07 field/world-boss hunt in open dunes/badlands;
- project data owns stats, drops, encounter state and region placement;
- donor progression/altar systems are not inherited automatically.

References:

- `https://modrinth.com/mod/threateningly-mobs-continued`
- `https://modrinth.com/mod/threateningly-mobs/version/K8PaW2nO`

## 3.2 Alex's Mobs Continued — desert ecology candidates

Use only species whose current 26.2 behavior/model supports a distinct ecological role.

Potential desert continuity includes:

- Rattlesnake — strong R07 fit after being intentionally rejected from R01;
- Roadrunner/Jerboa-style ambience where available;
- selected dryland predators/herbivores if they fill a real ecology role.

Project rule:

- do not fill open sand with mobs merely because the region looks empty;
- ambience can be sparse;
- donor loot and spawn rates are normalized by project data.

## 3.3 Desert settlement / caravan external direction

Primary external-first needs:

- tents/awnings, clay/stone walls, shade structures, rugs/crates/baskets, water jars, caravan packs, market stalls and well/cistern props;
- coherent desert-town architecture rather than recolored R01 medieval roofs;
- wagons/pack-animal gear only from accepted external families that fit the project's style.

Current candidate pools include accepted Kenney/Quaternius/KayKit fantasy/desert-neutral props under their exact source-specific licenses. If no coherent free town family passes, use a strong external Minecraft schematic/build reference rather than improvised AI town architecture.

## 3.4 Dungeon-boss external gate

R07's field-boss slot already has Ferox Deathworm. The dungeon climax must be different.

Required role:

- ancient cistern/observatory/fortress guardian or desert-adapted creature with clear heavy attack language;
- capable of interacting with columns, dust lanes, exposed water channels or armor-break mechanics;
- not another worm, giant beetle clone or vanilla Husk/Golem.

Candidate source pools:

1. Threateningly `Armor of Desert` only if actual model/animation quality supports a **dungeon boss** rather than merely an elite;
2. Quaternius animated monster families after direct visual comparison;
3. another legally redistributable desert guardian model.

Status:

```text
DUNGEON BOSS FINAL IDENTITY: OPEN ASSET GATE
DUNGEON SPATIAL/STORY ROLE: LOCKED
```

---

# 4. Story role — infrastructure under scarcity

R07 is one of the optional R04–R07 Act-II evidence regions.

Its evidence package argues:

> Central optimization becomes morally and practically dangerous when a network can redirect scarce resources between distant communities without local control.

Regional history:

- old Anchor-era works linked deep aquifers, wells, cisterns and observation stations across several caravan corridors;
- the system originally stabilized water access during extreme dry cycles;
- later central operators added cross-region balancing, allowing remote nodes to pull or redirect stored water according to continental demand models;
- modern settlements no longer understand the full network but maintain local wells/cisterns and trade routes;
- recent instability causes old buried controls to resume conflicting redistribution behavior;
- one historic oasis is recovering while two modern caravan wells are unexpectedly dropping;
- simply restoring the old continental logic would improve some central routes while potentially sacrificing peripheral settlements;
- simply destroying every old conduit risks losing drought-buffer capacity;
- R07 therefore exposes the human cost of treating regions as variables in a single optimization system.

The regional solution is not to decide the final continental philosophy. It is to stop the immediate remote draw and place local wells/cisterns under explicit regional control.

---

# 5. Local people / regional conflict

Primary hub: **fortified caravan oasis / trade town** on dependable high ground around a managed well/cistern cluster.

Normal life should visibly include:

- shade markets / water-jug filling;
- caravan loading and animal tack;
- repair of cloth, rope and wheels;
- dried foods/spices/minerals;
- well/cistern inspection;
- guides/scouts reading storms and routes;
- smithing focused on travel equipment and impact-resistant gear;
- travelers arriving/departing rather than a static village population.

Key local roles:

## 5.1 Caravan Master

- cares about route reliability and travel losses;
- knows which wells historically failed/recovered;
- wants predictable water but distrusts any fix that quietly sacrifices another route;
- connects regional trade, contracts and route state.

## 5.2 Well Keeper / Cistern Engineer

- understands local plumbing/mechanics but not the buried continental logic;
- provides concrete water measurements rather than mystical exposition;
- becomes central to isolating remote control from local reservoirs.

## 5.3 Dune Guide / Tracker

- reads predator traces, wind direction, ruins and badlands passes;
- supports Ferox Deathworm hunt and optional POIs;
- represents local knowledge without becoming another full reputation system.

Recurring Anchor Scholar / Engineer / Rival Wanderer can appear when main story routes through R07.

---

# 6. Spatial progression / local pressure

Suggested entry Lv remains **34**.

| Sub-area | Local pressure | Role |
|---|---:|---|
| R06/eastern transition / wadi | Lv 33–34 | readable approach / first caravan route |
| oasis town / inner wells | Lv 34 | hub / services / social context |
| caravan road / low dunes | Lv 34–36 | normal travel / ecology / POIs |
| badlands canyon / buried route | Lv 35–37 | elites / resources / alternate path |
| outer ruins / failed wells | Lv 36–38 | regional investigation |
| Ferox Deathworm territory | **Lv 39** | optional field/world boss |
| buried aquifer fortress dungeon | Lv 37–40 | regional dungeon |
| dungeon guardian target | **Lv 40** | climax |

R08/R09 begin at Lv44, leaving room for R07 optional boss/dungeon to bridge toward high-tier content.

---

# 7. Desert traversal contract

R07 uses route choice, not constant penalties.

## Main caravan road

- easiest navigation;
- reliable waystations/wells;
- more patrols/events/merchants;
- somewhat longer than risky direct cuts.

## Open-dune cut

- shortest path between visible landmarks;
- exposed to Deathworm/elite territories and storms;
- no universal movement slow;
- Trail Stag/Jungle Komodo remain usable where collision allows.

## Badlands/canyon route

- shade/cover, mining/resources, ruins and narrow ambush spaces;
- visually distinct from open sand;
- reconnects to main road rather than becoming a dead maze.

## Buried passages

- short authored tunnels/aquifer access points connecting specific POIs;
- entrances tied to surface landmarks;
- no continent-wide underground maze.

---

# 8. Heat / sandstorm rules

R07 has strong environmental presentation without survival busywork.

Baseline:

- no thirst bar;
- no ambient heat damage;
- no compulsory water consumable;
- no normal Stamina drain multiplier from heat.

Day/night may alter:

- lighting/ambient sound;
- creature/event weights;
- optional rare resources/catches where sensible;
- NPC schedule/presentation.

Sandstorms may:

- reduce long-range visibility;
- change selected predator/event spawns;
- reveal or obscure a few discovery cues;
- strengthen directional audio/waystation lights.

Sandstorms may **not**:

- disable map/controls entirely;
- apply constant HP damage;
- strand the player in mandatory waiting;
- become frequent enough that clear-sky desert exploration is rare.

---

# 9. Navigation / landmarks

Open terrain demands landmark discipline.

Use at least:

1. **oasis town towers / palms / smoke**;
2. **one huge mesa / split rock**;
3. **buried observatory/fortress crown** protruding above sand;
4. **giant fossil/rib or ancient aqueduct line** if terrain/assets support it;
5. **badlands red ridge**;
6. **one remote secondary oasis or waystation lantern cluster**.

Rules:

- most intended routes expose at least one macro landmark every 45–120 seconds;
- dunes may temporarily hide low POIs, but high landmarks keep orientation;
- map icons do not replace visible geography;
- empty breathing space is allowed, but mandatory dead travel beyond the audit cadence is not.

---

# 10. POI package

## Major POI A — Caravan Oasis Town

Functions:

- hub/service/economy node;
- contrast landmark;
- regional conflict made visible through wells/cisterns/trade.

## Major POI B — Dry Well Route

Functions:

- failed modern well + caravan detour;
- concrete evidence of remote water draw;
- small combat/repair/investigation sequence;
- route becomes safer/more useful later.

## Major POI C — Broken Aqueduct / Wadi Gate

Functions:

- strong ruin landmark;
- optional shortcut/resource route;
- shows old water engineering at surface scale;
- one regional event location.

## Major POI D — Red Mesa Watch

Functions:

- panorama/map orientation;
- desert predator clues;
- rare mineral/herb pockets;
- reveals Deathworm territory and fortress crown from above.

## Major POI E — Ferox Deathworm Basin

Functions:

- optional field/world-boss hunt;
- open sand/badlands arena with safe rock islands used for readability, not cheese;
- native apex ecology / signature rewards.

## Major complex — Buried Aquifer Fortress / Observatory-Cistern

Handled in §17.

Smaller discoveries may include:

- abandoned caravan camp;
- salt/mineral shelf;
- rattlesnake den;
- half-buried survey obelisk;
- old cistern cap;
- oasis bird/wildlife point;
- wind-carved arch;
- buried trade cache with actual story/economy context rather than generic chest spam.

---

# 11. Settlement / services / housing

Target daytime population:

```text
9–13 functional/service/guard/trade NPCs
5–9 ambient travelers/residents
```

Baseline services:

- shrine / fast travel;
- inn/rest/food;
- caravan/general merchant;
- Material Vault/bank;
- strong smith/gear service;
- contract/route board;
- well/cistern regional interaction;
- stable/mount service;
- selected alchemy/fishing trade if local geography supports it.

R07 should feel more transient than R01/R03: people come and go with caravans.

## Housing

R07 may provide:

- 1–2 courtyard/stone **Town House-class** properties;
- 1 **Large House-class** property around the existing ~25,000 Gold target if the actual town shell supports it.

A desert house is a real authored property, not a tent item.

---

# 12. Gathering / economy package

## Existing materials

- Iron/Silver/Hardwood remain available only where geology/ecology makes sense;
- no `Desert Iron II`;
- materials from earlier regions retain cross-region recipes.

## Dryland herb role

Purpose:

- cooking/alchemy/Focus/Cleansing variants;
- regional contracts;
- ordinary trade.

Final plant/model/name waits for external intake.

## Salt / mineral deposit role

Use one readable ordinary mineral/salt identity if external source supports it.

Uses:

- cooking;
- alchemy;
- selected crafting/furnishing/trade orders.

Do not create five colored desert salts.

## Rare aquifer crystal/mineral role

R07 may contain one visually distinctive deep-aquifer mineral tied to the dungeon/high-tier crafting, but final identity waits for an external model.

## Creature materials

Desert Beetle / Deathworm / snake materials enter the project only if they support real recipes/signature crafting.

No one-material-per-mob inventory bloat.

---

# 13. Fishing / oasis collection

R07 fishing is intentionally smaller than R05/R06 but should not disappear.

Use:

- oasis pools;
- deep cistern springs where ecologically plausible;
- one wadi/seasonal water pocket if actual map supports it.

Codex target:

```text
2–4 fish identities
```

At least one fish overlaps R06 or R11 water ecology where model availability makes sense.

One rare oasis/trophy catch may support:

- Fish Codex record;
- cooking;
- sale;
- housing display.

No fish is mandatory for main desert progression.

---

# 14. Ecology / encounter roles

R07 deliberately leaves large areas quiet.

## Rattlesnake

- finally valid regional home after R01 rejection;
- warning-rattle/defensive territory identity;
- common but not carpeted across every dune;
- poison buildup readable and avoidable.

## Desert Beetle

- heavy/common-elite pressure around rocky routes/ruins;
- body mass/armor should matter to poise/impact play;
- not a routine gear dispenser.

## Armor of Desert

- rare high-tier elite/miniboss around old forts/ruins if current model/behavior passes;
- should not appear as a random road mob;
- can become the dungeon-boss visual only if actual intake proves it has enough animation/mechanical range.

## Ambient fauna

Use sparse roadrunner/jerboa/bird/reptile equivalents where available.

Open desert does not need an aggro mob every 30 blocks.

---

# 15. Ferox Deathworm field boss

Source lineage explicitly places Ferox Deathworm in desert/snowland and describes a contact-triggered aggression identity.

Working project role:

```text
Lv: 39
role: optional field/world boss
HP target: ~23,000–27,000
Defense: ~95
MR: ~66
Poise: 240
solo active TTK target: ~200–235 s
```

Exact values depend on current 26.2 model/underground-motion inspection.

## Arena

- broad dune/badland basin;
- several stable rock shelves / broken stone lines for orientation;
- enough open floor to see surface tells;
- no arena full of tiny rocks that break pathfinding;
- boss cannot remain underground/unhittable for long stretches.

## Burrow contract

The Deathworm may burrow, but:

- every burrow has visible surface wake/dust line;
- ordinary burrow duration normally <=4–5 s;
- re-emergence target area is telegraphed;
- melee builds receive real punish windows after emergence/missed charge;
- repeated underground loops are capped;
- no random instant eruption directly under a stationary player without a tell.

## Required attack roles

Exact animations wait for donor inspection, but the fight must provide:

1. close bite/body sweep;
2. committed sand charge with limited steering;
3. burrow line → eruption;
4. broad signature crossing/spiral pattern with >=1.1 s initial cue;
5. low-HP pattern escalation through sequencing, not hidden +damage steroid.

## Rewards

First defeat:

- guaranteed Superior+ R07 equipment;
- 2 signature materials after exact anatomy/model intake;
- 15% direct Mythic/signature roll;
- EXP ~20% current next-Lv requirement;
- Class XP ~15%;
- optional trophy furnishing if visual source supports it.

Repeat follows normal field-boss rules.

---

# 16. Dynamic events

## Caravan Under Pressure

- short authored defense/escort intersection, not a ten-minute NPC walk;
- caravan can be stationary/repairing while threats arrive;
- support/combat/objective contribution all count.

## Well Failure

- investigate one falling well / clear obstruction / operate local mechanism;
- reveals regional story and gives useful trade/consumable rewards;
- not a repeatable daily chore.

## Sandstorm Lost Route

- optional storm event where guide signals / lanterns / sound reveal a stranded group or hidden ruin;
- storm creates discovery opportunity rather than punishment.

## Beetle Territory

- heavy Desert Beetle controls a canyon resource/route;
- player may fight or use alternate path;
- no arbitrary quest acceptance needed if event active.

---

# 17. Regional dungeon — Buried Aquifer Fortress / Observatory-Cistern

Target first-clear wall-clock:

```text
~24–34 minutes
```

The dungeon's identity is **desert-scale engineering beneath a sparse surface**.

## Stage 1 — Half-buried fortress entrance

- readable surface landmark;
- recent caravan/well evidence;
- wind/sand exterior encounter;
- one optional side route/resource cache with real historical context.

## Stage 2 — Cistern galleries

- dry stone halls reveal massive old water storage;
- visible high/low water marks explain historical operation;
- limited shallow-water sections create contrast but do not copy R06 dungeon.

## Stage 3 — Aquifer conduit

- large pipes/channels/shafts show water redistribution at regional scale;
- one route branches toward a failed peripheral well, making the cost visible;
- no complex plumbing spreadsheet puzzle.

## Stage 4 — Observatory/control archive

- records/maps reveal cross-region optimization and remote draw priorities;
- the player can see that one settlement's restored pressure corresponds to another route losing water;
- Act-II evidence package is obtained here.

## Stage 5 — Guardian chamber

- dry, broad arena around central cistern/control core;
- boss mechanics emphasize armor/impact, sand/dust lanes or water-channel denial depending on final model;
- no second Deathworm.

## Repeat shortcut

After first clear:

- one service stair/lift/door reconnects entrance to lower cistern level;
- story-only valve/control steps are skipped on repeats;
- dungeon remains replayable for boss/loot without repeating exposition chores.

---

# 18. Dungeon-boss mechanical contract

Target:

```text
Lv: 40
role: dungeon boss
solo active TTK: ~170–205 s
Poise: ~210–250
```

Required encounter properties:

- readable heavy commitment;
- one armor/exposure or stance mechanic that creates real punish windows;
- one long-line or column-based spatial attack suited to the cistern arena;
- one phase transition that changes behavior/space rather than only stats;
- melee uptime remains reasonable;
- exact hitboxes/weak points wait for external model intake.

If Armor of Desert is selected after inspection, its existing identity should be preserved rather than overwritten with unrelated magic. If not, choose a better external guardian.

---

# 19. Regional quest / discovery flow

## `The Missing Well` role

- caravan route arrives late / one stop has failed;
- investigate real water loss rather than generic monster attack;
- establishes local stakes.

## `Lines Under Sand` role

- inspect two old conduit/well sites in flexible order;
- discover pressure is being redirected rather than simply disappearing;
- one path can be direct dune, one badlands/canyon.

## `A Route Worth Keeping` role

- local people debate whether old works should be reactivated;
- player restores/islands one useful local branch without re-enabling remote control;
- visible route service improves.

## `The Buried Ledger` role

- regional dungeon / Act-II evidence;
- reveals central optimization logic and its human cost.

## Ferox Deathworm hunt

- begins through surface wakes, lost cargo, shed material, seismic signs or distant sightings supported by the actual model;
- optional;
- never blocks main progression.

---

# 20. Regional outcome / memory

Shared late-join-safe consequences may include:

- one caravan well/cistern stabilizes;
- a waystation reopens;
- one formerly abandoned shortcut becomes safe enough for normal traffic;
- merchant/caravan presence increases;
- another natural/low-value area is deliberately not over-engineered.

Personal consequences:

- R07 evidence package;
- local NPC dialogue/relationship flags;
- Deathworm hunt state;
- discoveries/trophy/fishing records;
- first-clear/reward state.

The desert should feel **more connected and safer**, not suddenly lush or climate-transformed.

---

# 21. Reward identity

Regional equipment emphasis:

- impact/poise damage;
- armor break / committed-hit payoff;
- ranged precision/weak-point utility;
- anti-burst / VIT/END defensive options;
- movement/skill recovery after dodge/commitment;
- selected Poison resistance from Rattlesnake/desert ecology without making it a poison region clone;
- dryland/fire/sand-themed skill variants only when external VFX support fits.

No desert currency.

No mandatory region-specific resistance set.

---

# 22. Audio / presentation

Desert ambience needs contrast and space.

Use:

- wide low wind;
- fabric/rope/wood at caravan hubs;
- sparse birds/insects/reptiles;
- sand movement and distant rock resonance;
- silence reduction before major buried threat cues;
- storm audio that preserves directional signals.

Dungeon:

- wind falls away quickly;
- huge dry cistern reverb / distant drip / stone movement;
- old control machinery becomes audible near the archive/core.

Deathworm surface wake and eruption cues must be readable through music and storm layers.

---

# 23. Multiplayer / authority

Server owns:

- quest/evidence state;
- well/cistern/route world state;
- Deathworm burrow path and hit validation;
- dungeon control state;
- personal loot/rewards;
- personal gathering/fishing state;
- event contribution.

Required tests later:

- Deathworm underground location never desyncs from visible wake;
- support roles qualify in caravan/boss events;
- shared well repair does not skip personal story flags;
- late join sees reopened route but can still obtain personal evidence;
- dungeon control state remains deterministic after reconnect/chunk unload;
- no duplicated first-clear reward.

`MULTIPLAYER TESTED` remains NO until real-client verification.

---

# 24. Performance constraints

- no per-tick region-wide sandstorm scan;
- no simulated underground aquifer network;
- Deathworm only runs expensive burrow/path logic while its encounter is active;
- ambient desert population remains sparse;
- long sightlines require careful entity/render-distance profiling;
- large settlements/markets use block/world composition with bounded model entities;
- buried dungeon effects stop when unloaded.

---

# 25. Asset-intake blockers

R07 is not asset-ready until:

1. current 26.2 Desert Beetle/Armor of Desert/Ferox Deathworm model/animation review;
2. Rattlesnake/current desert wildlife dependency review;
3. exact caravan-town/market/awning/well/cistern architecture assets;
4. exact dryland herb/salt/mineral node models;
5. exact R07 equipment and weapon visual families;
6. Ferox Deathworm trophy/signature material visual mapping;
7. final dungeon-boss external model;
8. cistern/Anchor machinery source family;
9. sandstorm/dust/impact VFX sources;
10. desert ambience/music/sound sources;
11. oasis fish-model assignments where retained.

No vanilla Husk, scaled Iron Golem or particle-only worm is accepted as a finished substitute.

---

# 26. R07 quality acceptance

Real play must prove:

- open desert feels spacious without becoming dead travel;
- macro landmarks support navigation;
- caravan road, dunes and badlands offer meaningful route trade-offs;
- no thirst/heat chore is needed to make the region feel hot/dry;
- oasis/town economy visually depends on water and trade;
- sparse ecology feels intentional, not unfinished;
- Deathworm burrow combat remains readable and gives melee real uptime;
- dungeon teaches the resource-allocation story through place and consequence;
- underground layers remain easy to mentally map;
- post-clear route/well changes are visible;
- regional rewards matter elsewhere;
- multiplayer/save state does not duplicate or desync;
- performance holds under long sightlines and storm effects.

Verification state:

```text
DESIGN REVIEWED: YES
EXTERNAL REFERENCE REVIEWED: YES
EXACT ASSET INTAKE: PARTIAL / REQUIRED
CODE REVIEWED: N/A
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
