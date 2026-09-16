# Open-World RPG — R10 Cinderfall Implementation Package

> Status: **DESIGN CANON — R10 world/story/traversal/service/combat/reward flow is implementation-ready; exact Inferno/current volcanic-creature quality and several resource bindings remain external-intake gates**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Story spine: `WORLD_STORY_CANON.md`  
> Region graph: `REGIONS.md`  
> Combat: `COMBAT_BALANCE.md`, `STATUS_AND_R01_ENCOUNTERS.md`  
> Loot/equipment: `LOOT_ECONOMY.md`, `EQUIPMENT_BALANCE.md`  
> Mounts: `MOUNTS.md`  
> Field systems: `GATHERING_FISHING_CAMP_HOUSING.md`, `FISHING_COLLECTION_HOUSING_MARKET.md`  
> Quest/state: `QUEST_WORLD_STATE.md`  
> Quality contract: `DESIGN_COMPLETENESS_AUDIT.md`  
> External provenance: `EXTERNAL_SOURCES.md`  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins.

R10 is the first region where the main story demonstrates **both the real value and the real danger of partial Anchor restoration in one continuous playable sequence**.

Its play sentence is:

```text
approach a visually isolated volcanic ash island/peninsula whose refuge and foundries survive by exploiting dangerous heat/mineral routes
→ move between cool ash shelves, fortified industrial paths, lava cuts and vent fields without managing an always-on heat bar
→ learn why local people willingly accept the danger: high-grade metal, geothermal industry and routes that feed multiple regions depend on it
→ watch a controlled Restoration test genuinely improve forge output, stabilize one route and protect the refuge
→ hunt Basalt Wyvern on exposed basalt cliffs while Scorch Golem pressure defines heavy volcanic combat
→ discover that the restored local node is still coupled to distant demand/load balancing
→ experience a real cascade where successful remote synchronization overloads vents/forge channels and destabilizes the caldera
→ traverse a changing forge/caldera complex while isolating pressure branches
→ confront Inferno through an authored altar/core encounter only if its current model/function passes the quality gate
→ preserve useful local geothermal control while severing dangerous continental coupling
→ leave with proof that Restoration can work exactly as intended and still recreate the single-point/cascade problem
```

R10 should make the player think:

> `The system works. That is exactly why reconnecting all of it is dangerous.`

This is the clearest Act-III demonstration that both restoration advocates and their critics have legitimate evidence.

---

# 1. Locked regional identity

Preserved from `REGIONS.md`:

- terrain: volcanic ash island / ash coast / basalt shelves / caldera;
- suggested entry Lv: **58**;
- visually isolated late-game destination;
- physically reachable early by reckless traversal rather than a story-only teleport wall;
- ecology direction: Scorch Golem, Basalt Wyvern, Inferno plus only additional fire creatures that genuinely improve the region;
- no Blaze/Magma Cube population;
- settlement direction: fortified refuge / forge enclave;
- resources: volcanic glass, high-grade ore, fire-reactive mineral and ash reagent;
- high-tier smithing materials begin here but are not exclusively RNG boss drops;
- dungeon direction: caldera forge / buried volcanic foundry;
- heat is communicated through hazards/telegraphs rather than permanent maintenance UI;
- reward identity: high-tier physical/magical forge components, fire/impact control, heavy commitments and volcanic utility.

Production clarifications:

- Basalt Wyvern belongs here, correcting the older R03 placement;
- no always-on heat/thirst meter;
- no mandatory heat-resistance armor set just to enter the region;
- no vanilla Nether visit is required;
- no arbitrary Nether-native vanilla mobs are used as population;
- Inferno remains a **summoned/altar boss identity** rather than becoming a random caldera spawn;
- if current Inferno presentation still reflects its historically unfinished/remade state, the final boss must use another higher-quality external source rather than lowering the bar.

---

# 2. External game-content precedents

## 2.1 Guild Wars 2 — Ember Bay / Draconis Mons

Useful lessons from ArenaNet's volcanic maps:

- two volcanic maps can feel distinct through terrain structure, sub-biomes and traversal rather than both becoming black rock + lava;
- region-specific movement tools can create memorable routes without becoming universal character maintenance systems;
- Draconis Mons was deliberately structured into distinct subfloors/biomes to balance exploration with clear travel progression;
- volcanic spaces can include living/natural pockets and constructed locations rather than being uniformly barren.

Project adoption:

- R10 uses distinct bands: ash coast/refuge, industrial basalt shelf, vent/mineral belt, high wyvern cliffs, buried forge, caldera core;
- one or two regional thermal/pressure shortcuts may exist, but there is no new mastery grind;
- repaired forge lifts/pressure routes improve repeat navigation;
- volcanic presentation varies between ash, basalt, hot vents, active lava and industrial heat.

Not adopted:

- map-specific movement currencies/mastery grinding;
- lava-walking as a general ability;
- constant bounce/propulsion gimmicks that replace normal navigation.

References:

- `https://www.guildwars2.com/en/news/guild-chat-summary-risingflames/`
- ArenaNet Draconis Mons development discussion summarized in `Flashpoint on Guild Chat`.

## 2.2 Horizon Forbidden West: Burning Shores — volcanic region + encounter journey

Useful lessons:

- a volcanic region can remain inhabited and culturally meaningful rather than being only a combat wasteland;
- large encounters become more memorable when they move through multiple spaces and visibly affect the environment;
- an epic boss should challenge several learned abilities and escalate across distinct encounter sections;
- the environment itself should communicate scale and consequences.

Project adoption:

- R10 refuge/foundry life is central, not a token safe room;
- the regional climax begins before the final boss arena through cascading vents, route changes and forge isolation tasks;
- the final dungeon/boss journey moves from industrial space toward caldera rather than loading into a disconnected arena;
- shared world-state aftermath visibly changes the refuge and route stability.

Not adopted:

- giant cinematic scale that Minecraft cannot support cleanly;
- long scripted chase sequences with little player agency;
- spectacle that breaks server authority or destroys arbitrary world terrain.

References:

- `https://blog.playstation.com/2024/05/13/horizon-forbidden-west-burning-shores-expansion-turns-one-building-the-massive-horus-battle/`
- `https://blog.playstation.com/2023/04/06/get-to-know-the-quen-of-horizon-forbidden-west-burning-shores/`

## 2.3 Heat as space, not a maintenance meter

R10 follows the project-wide principle established in R04/R07:

Adopt:

- local lava/steam/vent hazards;
- route planning around visible heat sources;
- safe industrial shelters/bridges;
- brief authored high-heat zones with obvious telegraphs;
- optional consumable/food advantages that help but are not admission tickets.

Reject:

- region-entry heat gauge;
- constant HP/Stamina drain for existing in the biome;
- mandatory water/heat potion refresh;
- gear checks that force one resistance set for hours.

---

# 3. External-first source stack

## 3.1 Threateningly Mobs Continued / original lineage

### Basalt Wyvern

Original lineage explicitly added Basalt Wyvern to **badlands and basalt biomes**, making R10 a much stronger fit than R03.

Project use:

- authored high-cliff elite / optional field-boss identity;
- not common sky spam;
- flight windows must remain melee-friendly under the same principles used for the R03 Griffin;
- final project stats/rewards/spawn state are project-owned.

Reference:

- `https://modrinth.com/mod/threateningly-mobs/version/1.0.5`

### Scorch Golem

Original wiki identity lists Scorch Golem as a high-stat hostile creature with a Nether source identity.

Project use:

- moved out of vanilla Nether dependence into authored R10 volcanic ecology/forge spaces;
- heavy elite/guardian role;
- exact attack kit waits for current 26.2 model/animation inspection;
- no random world-wide spawn.

### Inferno

Current/original listings identify Inferno as an **Ultra-tier altar-summoned boss**.

Historical changelog evidence also notes an Inferno model remake with unfinished functionality during development.

Project rule:

- preserve the authored altar/core invocation identity;
- never treat Inferno as a random natural mob;
- current 26.2 model, animations and full behavior require direct inspection before final boss acceptance;
- if quality remains below the project bar, replace the visual/boss source while preserving R10's final encounter role.

References:

- `https://www.curseforge.com/minecraft/mc-mods/threateninglly-mobs-continued`
- original Threateningly Mobs changelog history.

## 3.2 Flame-palace creature candidates

Original Threateningly lineage added creatures such as `Plague Bird` and `Dragon Flower` around a flame-palace content family.

These may be inspected as **candidate authored R10 secondary threats** only if:

- current continuation still exposes them cleanly;
- model/animation quality fits;
- their role is mechanically distinct from Scorch Golem/Wyvern;
- they do not make the region feel like the donor mod pasted wholesale.

No roster slot is guaranteed merely because a model exists.

## 3.3 Environment / forge / mining assets

R10 requires strong external direction for:

- refuge/foundry architecture;
- basalt/ash props;
- cranes/carts/smelters;
- heavy forge tools;
- pressure pipes/valves;
- old Anchor heat-control machinery;
- lava bridges/cooling structures;
- volcanic resource nodes.

Candidate pools:

- accepted Kenney/Quaternius/KayKit industrial/fantasy props under exact source-specific licenses;
- coherent external Minecraft forge/foundry builds/schematics when they outperform available model kits;
- large cliffs/lava channels remain authored world/block composition rather than entity-heavy decoration.

No vanilla furnace wall is accepted as the finished high-tier foundry.

---

# 4. Story role — restoration succeeds, then couples too much

R10 is a required late Act-III investigation option under `WORLD_STORY_CANON.md`.

Its evidence package is intentionally uncomfortable for every philosophy.

Before the player arrives:

- local forge/refuge engineers operate partial manual geothermal controls;
- productivity is limited and some mining routes remain unsafe;
- Restoration teams propose reconnecting one old Anchor heat-control branch under monitored conditions;
- local workers support the test because the practical benefits are obvious.

During regional play:

- the test **works**;
- vent pressure stabilizes in one corridor;
- forge heat becomes predictable;
- one dangerous route opens;
- high-grade production increases;
- injuries/interruptions fall.

Then the deeper problem appears:

- reactivated control handshakes with distant nodes;
- remote demand/load balancing begins moving pressure/energy according to continental priorities;
- a second node starts compensating automatically;
- the coupled response pushes the caldera/old forge toward a cascade;
- nobody intentionally sabotaged it;
- the old system is behaving according to its design.

Regional resolution:

- isolate R10 from remote automatic balancing;
- keep bounded local geothermal/forge controls active;
- install manual/local safety limits based on modern settlement needs;
- preserve the records showing both the successful benefits and the cascade risk.

This gives the finale evidence that cannot be dismissed as `restoration just failed because someone did it wrong`.

---

# 5. Local people / culture

Primary hub: **fortified volcanic refuge / forge enclave** on a cool/stable ash shelf.

Normal life visibly includes:

- ore receiving/sorting;
- smelting/forging;
- protective clothing/gear preparation;
- vent/pressure inspection;
- water/cooling management as industrial infrastructure, not thirst gameplay;
- shift changes between safe and dangerous works;
- traders from R09/R11 moving valuable material;
- injuries/recovery/healer work associated with real industrial danger.

Key local roles:

## 5.1 Forge Warden / Chief Smith

- values reliable heat because inconsistent forge temperature costs material and lives;
- initially supports the Restoration test for rational reasons;
- becomes one of the clearest witnesses that remote control is the issue, not the technology itself;
- ties high-tier smithing to story consequence.

## 5.2 Pressure Engineer

- understands local vents/valves/manual controls;
- can explain why the first test is genuinely succeeding;
- notices remote commands/load balancing that nobody locally requested;
- works with the player to isolate branches during the climax.

## 5.3 Ash Scout / Wyvern Watcher

- knows safe basalt routes and flying-predator territory;
- supports field-boss clues and traversal;
- distinguishes natural volcanic ecology from Anchor-related pressure failures.

Recurring Engineer/Smith and Restoration Director roles become particularly important here.

---

# 6. Spatial progression / local pressure

Suggested entry Lv remains **58**.

| Sub-area | Local pressure | Role |
|---|---:|---|
| ash-coast arrival / cool shelf | Lv 57–58 | readable entry / refuge approach |
| forge refuge | Lv 58 | hub / high-tier smithing / story |
| basalt works / mine road | Lv 58–60 | normal resources / Scorch pressure |
| vent fields / broken industrial route | Lv 59–61 | environmental hazards / events |
| high basalt cliffs | Lv 60–62 | Wyvern territory / rare resources |
| Basalt Wyvern field-boss site | **Lv 63** | optional major hunt |
| buried forge → caldera dungeon | Lv 61–64 | major late-game dungeon |
| Inferno/final guardian target | **Lv 64** | dungeon/main climax |

R11 begins at Lv64 and may be explored before R10 full clear.

---

# 7. Heat / hazard contract

R10 hazards are **local, visible and avoidable**.

## Lava

- actual lava remains dangerous;
- routes provide bridges, basalt shelves, lifts or short alternative paths;
- no routine progression requires long lava swimming.

## Steam / pressure vents

- visible buildup / sound cue;
- short burst lane/cone;
- may knock/stagger or deal damage;
- server state owns active timing;
- reused as encounter/environment language in the dungeon.

## Ashfall

- primarily atmosphere / reduced distant contrast;
- selected event state may slightly change visibility or creature weights;
- no constant suffocation/damage meter.

## High-heat forge corridors

- localized authored volumes;
- player crosses through timing/route choices, not a consumable timer;
- optional food/gear may modestly reduce hazard damage but is never mandatory admission.

---

# 8. Traversal / shortcut progression

Use:

- basalt roads/shelves;
- mine/forge lifts;
- protected industrial bridges;
- cooled channels;
- short pressure/thermal lifts only if movement quality passes;
- shrine/major fast travel.

Regional progress may permanently open:

1. refuge → basalt works lift;
2. cooled bridge across one lava cut;
3. dungeon/caldera industrial lift after first clear.

These are physical improvements, not menu unlocks.

No universal lava-walk or fire immunity ability is introduced.

---

# 9. Navigation / landmark language

Use at least:

1. **main caldera/volcano silhouette**;
2. **forge refuge smoke/stack/tower profile**;
3. **high Basalt Wyvern cliff spire**;
4. **glowing lava-cut line** visible from safe shelves;
5. **old forge crown / pressure tower**;
6. coastline/R11 sea horizon where geography permits.

Rules:

- ash may soften distance but cannot erase all anchors;
- lava/forge glow is used for hierarchy, not every surface;
- route signs/lamps are visible through moderate ashfall;
- field-boss and dungeon directions should become mentally map-able without permanent arrows.

---

# 10. POI package

## Major POI A — Cinderfall Forge Refuge

Functions:

- hub;
- high-tier smithing identity;
- Restoration test becomes visible in normal work;
- trade link to R09/R11.

## Major POI B — Cooling Works

Functions:

- local infrastructure / route shortcut;
- demonstrates successful restoration benefit;
- event/gathering/resource site.

## Major POI C — Blackglass Shelf

Functions:

- volcanic-glass/high-tier resource destination;
- dangerous but readable vent terrain;
- panorama/route choice.

## Major POI D — Pressure Spires

Functions:

- old/local regulator evidence;
- changing vent states;
- main-story investigation;
- visible marker toward dungeon.

## Major POI E — Basalt Wyvern Cliffs

Functions:

- optional field-boss arena;
- natural volcanic apex hunt;
- high-view navigation;
- signature rewards/trophy.

## Major complex — Caldera Forge / Buried Foundry

Handled in §17.

Smaller discoveries may include:

- collapsed mine camp;
- cooled lava tube;
- old pressure gauge shrine;
- ash reagent pocket;
- abandoned smith shelter;
- mineral seam overlook;
- wyvern nesting trace;
- coastal volcanic fishing pocket if actual map supports it.

---

# 11. Settlement / services / housing

Target daytime population:

```text
10–14 functional smiths/engineers/guards/traders/healers
5–8 ambient residents/workers/travelers
```

Baseline services:

- shrine/fast travel;
- inn/rest/food;
- top-tier forge/smithing service;
- Material Vault/bank;
- high-tier equipment/material merchant;
- alchemy/healing;
- regional contract/work board;
- mining/pressure engineer interaction;
- stable/mount service where terrain allows.

R10 is allowed to be the strongest launch smithing hub but must not invalidate all earlier smiths; earlier hubs still support ordinary forge/reforge services.

## Housing

R10 may offer one authored Large House-class refuge property if architecture/map supports it.

Do not put a prestige mansion beside lava solely because late-game players have Gold.

---

# 12. Gathering / production package

## Volcanic Glass

Readable ordinary name is acceptable if accepted external presentation fits.

Uses:

- high-tier weapon/accessory components;
- selected alchemy/catalyst recipes;
- furnishing/trophy/glass detail;
- ordinary trade.

Gathered from authored cooled shelves/nodes, not every obsidian block.

## High-grade ore role

R10 should introduce one meaningful high-tier metal/ore **only after exact external model and equipment recipe family are selected**.

Rules:

- not boss-exclusive;
- rare world nodes + dungeon/contract + merchant/crafting sources;
- Masterwork Pick may be required for the richest nodes, but acquisition cannot require the same ore in a circular lock.

## Fire-reactive mineral role

One special material may support:

- fire/heat VFX-linked weapons;
- forge upgrades;
- high-tier alchemy.

Final name/model waits for intake.

## Ash reagent

Use a bounded alchemy/crafting role rather than every ash block becoming loot.

No `Cinder Token` currency.

---

# 13. Fishing / coastal collection

R10 fishing is small but can use geothermal/coastal contrast.

Potential spots:

- cooled coastal pools;
- deep ash-coast water;
- one warm spring only if ecology/model supports it.

Fish Codex target:

```text
2–4 identities
```

Do not invent lava fish unless an excellent external animated model exists and the ecology is visually believable.

---

# 14. Ecology / encounter hierarchy

## Scorch Golem

- high-poise volcanic/forge heavy elite;
- authored near mineral/old forge spaces;
- not blanket map population;
- exact current attacks/status relations after 26.2 inspection.

## Basalt Wyvern

- high-cliff volcanic flyer;
- sparse elite and optional field-boss identity;
- routine flight windows remain short enough for melee;
- natural volcanic creature, not an Anchor machine.

## Secondary creatures

Use only if they add distinct roles after review:

- Plague Bird / Dragon Flower from original flame-palace content family;
- another redistributable animated ash/volcanic creature.

No Blaze/Magma Cube fallback.

---

# 15. Basalt Wyvern field boss

Working role:

```text
Lv: 63
role: optional field boss
HP target: ~42,000–48,000
Defense: ~145
MR: ~105
Poise: ~265
solo active TTK target: ~205–240 s
```

Exact values depend on current model/flight profile.

## Arena

- broad basalt shelf / cliff saddle;
- visible lava hazard only at specific edges/channels;
- enough stable melee floor;
- wind/ash does not hide dive telegraphs;
- no tiny ledges causing constant fall deaths.

## Required behavior

- short airborne reposition/dive cycles;
- grounded bite/claw/wing opportunities;
- one basalt/fire breath or lane attack only if external animation/VFX supports it;
- signature dive with strong tell and punish;
- no repeated 15–20s unreachable circles;
- flight path server-owned.

## Rewards

First defeat:

- guaranteed Superior+ R10 gear;
- 2 model-linked signature materials after intake;
- 15% direct Mythic/signature roll;
- EXP ~20% current next-Lv requirement;
- Class XP ~15%;
- trophy if accepted model supports it.

---

# 16. Dynamic event package

## Pressure Spike

- local vent/forge controller becomes unstable;
- player helps clear threat / operate manual branch / defend engineer;
- visibly changes one route for event duration;
- no event currency.

## Foundry Overload

- high production during Restoration test creates real risk;
- event can reward high-tier crafting materials;
- reinforces main-story theme before climax.

## Wyvern Pass

- flying threat crosses a transport route;
- short defense/avoidance/hunt event;
- not mandatory boss spawn.

## Ashfall Rescue

- compact rescue/route event using landmarks/sound;
- no long escort pathfinding.

---

# 17. Regional dungeon — Caldera Forge / Buried Volcanic Foundry

Target first-clear wall-clock:

```text
~28–38 minutes
```

This is the first dungeon whose **regional state visibly changes during the run** because the coupled system is actively cascading.

## Stage 1 — Working industrial approach

- starts in a functional/partly restored forge extension;
- workers/engineers retreat as pressure rises;
- player sees the benefit and then the failure, not an already-dead ruin.

## Stage 2 — Pressure galleries

- valves/vents/cooling lines;
- local hazards change paths;
- combat uses visible vent timing and heavy enemies;
- one resource/forge side branch.

## Stage 3 — Coupling chamber

- old node visibly handshakes with distant routes;
- player isolates one or two branches while the system compensates elsewhere;
- shows cascade behavior through environment, not a text diagram.

## Stage 4 — Caldera exterior/interior transition

- short exposed segment overlooking active caldera;
- major spectacle/landmark moment;
- route remains readable and physically safe enough for normal play;
- pressure event escalates.

## Stage 5 — Core altar / Inferno chamber

- old summoning/core architecture;
- final boss/guardian invoked as part of the stabilization sequence if accepted external source fits;
- combat arena broad enough for high-tier patterns.

## Repeat shortcut

After first clear:

- industrial lift/bridge connects entrance to lower/core wing;
- first-clear coupling story interactions are skipped;
- repeat run keeps meaningful combat/resource/boss flow.

---

# 18. Inferno final-boss candidate

Inferno source identity is Ultra-tier and altar-summoned, which fits an authored R10 core encounter better than random spawning.

Working target **only if current 26.2 presentation passes**:

```text
Lv: 64
role: major dungeon/main-story boss
HP target: ~48,000–58,000
Defense: ~150
MR: ~125
Poise: ~280
solo active TTK target: ~220–270 s
```

Required encounter properties:

- visible physical/magical body identity stronger than generic fire particles;
- close-range attacks for melee interaction;
- one or two fire/pressure patterns matching external animation/VFX;
- phase change tied to the active core/pressure state;
- no blanket fire floor covering the whole arena;
- no long invulnerability;
- authored breakable props only, no world grief;
- final weak points/status relations determined after model inspection.

If current Inferno is incomplete/low-quality:

```text
REJECT CURRENT PRESENTATION
→ select higher-quality external volcanic boss model
→ preserve altar/core/cascade encounter role
→ revise exact attacks before implementation
```

Do not ship a weak boss because the donor name is convenient.

---

# 19. Main-story / regional quest flow

## `Useful Heat` role

- arrive at refuge/forge;
- see manual instability and real economic/safety limits;
- meet Restoration test team;
- establish why reactivation is appealing.

## `The Test Works` role

- activate/observe bounded restoration branch;
- route stabilizes, forge output improves, workers benefit;
- player participates in successful practical tasks rather than being told it worked.

## `Load From Elsewhere` role

- unexpected remote balancing begins;
- investigate two pressure sites in flexible order;
- discover system is responding to distant demand, not sabotage.

## `The Coupled Forge` role

- dungeon/main Act-III evidence;
- isolate regional controls during cascade;
- final boss/core sequence;
- preserve working local regulation while cutting remote automatic coupling.

Basalt Wyvern hunt remains optional.

---

# 20. Regional outcome / memory

Shared late-join-safe changes may include:

- stable local forge/pressure branch remains active;
- one cooled route/bridge stays open;
- foundry visibly runs at improved but bounded capacity;
- merchant/smith stock expands;
- one remote-coupling structure remains physically disabled/isolated;
- workers return to selected areas.

Personal consequences:

- R10 Act-III evidence package;
- local/restoration dialogue flags;
- Wyvern hunt state;
- first-clear/reward/discovery state.

The region should feel **safer and productive**, not magically extinguished.

---

# 21. Reward identity

Regional equipment emphasis:

- high-tier forge/weapon base quality;
- fire/status resistance as useful option, not mandatory set;
- impact/poise/committed attack payoff;
- guard stability / END/VIT options;
- fire/heat-linked spell variants where external VFX fits;
- catalyst/weapon materials from world/dungeon/forge, not boss RNG only;
- selected mining/production utility bounded so combat gear remains combat gear.

No volcanic currency.

---

# 22. Laviathan progression connection

`MOUNTS.md` allows Laviathan unlock through R10 or R11.

R10 may **introduce** the idea through hazardous water/lava transport lore/creature sightings, but the exact unlock region is chosen during R10/R11 asset/playflow integration.

If R10 unlock is used:

- it must be a dedicated authored route/creature interaction;
- no Nether requirement;
- 3,000 Gold registration after quest remains canonical;
- role stays water/lava specialist, slow on land;
- does not bypass the R10 dungeon or caldera before intended discovery.

Do not force the unlock into R10 merely because lava exists if R11 provides a better high-quality encounter.

---

# 23. Audio / presentation

R10 needs strong dynamic range:

Safe/refuge:

- forge rhythm;
- tools/metal;
- low vent rumble;
- workers/trade.

Field:

- ash wind;
- distant lava/rock movement;
- pressure hiss localized to hazards;
- large empty spaces allowed to become quiet.

Cascade:

- pressure/forge layers escalate gradually;
- alarms/signals remain diegetic/local rather than global siren spam;
- music intensity follows actual state.

Boss:

- physical body/attack sounds must remain distinct from background lava;
- lethal telegraphs cannot be hidden by continuous roaring ambience.

---

# 24. Multiplayer / authority

Server owns:

- restoration/coupling/pressure world states;
- quest/evidence progression;
- vent/hazard timing;
- forge shortcut state;
- Wyvern/Inferno encounter state;
- boss flight/hit validation;
- personal loot/reward claims;
- gathering nodes.

Required multiplayer tests later:

- pressure state identical for all clients;
- one player's personal story state not skipped by shared branch isolation;
- late join sees post-clear physical state but can still obtain evidence;
- Wyvern flight does not desync;
- disconnect during cascade resolves to deterministic valid checkpoint;
- no Inferno altar double-consume / reward duplication;
- support contribution qualifies.

`MULTIPLAYER TESTED` remains NO until real-client verification.

---

# 25. Performance constraints

- no simulated region-wide heat/pressure fluid network;
- local state-machine controllers only;
- VFX density bounded aggressively around lava/forge;
- no thousands of ash particles per player;
- Wyvern/Inferno expensive AI only in active encounter;
- foundry ambient animation counts bounded;
- dynamic lights/transparent heat effects profiled on RTX3050-class target client and weaker fallback settings;
- inactive dungeon sections stop ticking expensive effects.

---

# 26. Asset-intake blockers

R10 is not asset-ready until:

1. current 26.2 Basalt Wyvern model/flight/animation review;
2. current Scorch Golem model/attack review;
3. current Inferno model/function/animation quality review;
4. secondary volcanic-creature decision after actual comparison;
5. coherent forge/refuge architecture/prop family;
6. volcanic-glass/high-tier-ore/fire-mineral/ash node models;
7. R10 equipment/weapon/accessory visual families;
8. Anchor heat-control / pressure machinery assets;
9. lava/steam/ash/fire VFX sources;
10. forge/volcanic ambience/music/sound sources;
11. Wyvern/Inferno trophy/signature material models;
12. Laviathan R10-vs-R11 unlock decision after direct playflow/asset review.

No Blaze, Magma Cube, scaled vanilla dragon substitute or particle-only Inferno is accepted as finished presentation.

---

# 27. R10 quality acceptance

Real play must prove:

- the volcanic region is dangerous because of readable spaces/hazards, not a maintenance meter;
- refuge/foundry life makes the risk economically and socially understandable;
- the Restoration test visibly produces genuine benefits before the cascade;
- the player can understand that the cascade is a coupling/design problem, not villain sabotage;
- Basalt Wyvern flight remains melee-friendly and readable;
- Scorch Golem/secondary mobs add distinct combat roles;
- the dungeon escalates through a journey rather than teleporting to a boss arena;
- post-clear forge/route improvement is obvious;
- high-tier materials have world sources and do not require boss grind;
- visuals/audio communicate heat without hiding telegraphs;
- multiplayer pressure/boss/save state is deterministic;
- performance remains acceptable under forge + lava + ash + multiplayer load.

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
