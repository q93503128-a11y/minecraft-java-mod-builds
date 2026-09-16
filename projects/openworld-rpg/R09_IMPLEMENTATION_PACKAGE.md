# Open-World RPG — R09 Southstone Marches Implementation Package

> Status: **DESIGN CANON — R09 world/story/traversal/service/reward flow is implementation-ready; exact field-boss visual identity remains an external-intake gate**  
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

R09 is R08's peer at Lv44, but its identity is intentionally almost the opposite: **grounded, physical, social and infrastructural** rather than overtly magical.

Its play sentence is:

```text
enter a rocky southern march where caravans, herd routes and old forts connect dry grassland to an eroded coast
→ learn the signal towers, foundry roads and shared supply depots that local communities maintain without relying on ancient automation
→ choose between guarded trade roads, faster exposed ridge cuts and coastal routes
→ hunt heavy wildlife and armored threats where mass, charge lines, guard and poise matter
→ help restore a practical chain of bridges, beacons and depots through local labor rather than awakening a forgotten super-system
→ earn/register the Caravan Elephant as a heavy traversal mount through a real handler/caravan story
→ investigate an old fortress whose Anchor-era logistics core once centralized route control and foundry allocation
→ defeat the authored Executioner/fortress guardian only if the current external presentation passes quality review
→ leave with evidence that distributed human institutions can replace some functions once assigned to the Anchor network
```

R09 should make the player think:

> `Resilience is not only ecology or magic. People can build redundancy, agreements and physical infrastructure themselves.`

This is a grounded high-tier test of local autonomy and shared defense, not another anti-technology story.

---

# 1. Locked regional identity

Preserved from `REGIONS.md`:

- terrain: southern rocky shore, dry grassland, eroded terrain and long trade roads;
- suggested entry Lv: **44**;
- R08 and R09 are peer high-tier routes;
- caravan gameplay and heavy physical enemies prepare the player for R10;
- ecology includes Rhinoceros, Elephant, Kangaroo/Maned-Wolf-style dryland wildlife and selected ambient raptors/insects;
- Flamehorn is a signature heavy creature;
- Steelboar may persist only in limited pockets;
- Executioner appears only in authored forts/ruins;
- settlement direction: caravan fort / foundry outpost;
- resources: dense metal, clay, hardstone, hide and dry medicinal plants;
- trade routes make the region an economy connector rather than a combat-only zone;
- dungeon direction: ruined canyon fortress with battlements, foundry and collapsed lower vault;
- reward identity: heavy armor, guard/counter, impact resistance, merchant/travel utility and high-quality metal components.

Production clarifications:

- no new caravan currency;
- no mandatory escort quest where a slow NPC walks across half the region;
- no global wind/stamina survival penalty;
- R09 is not `R03 mountains but browner`;
- heavy creatures are sparse territorial/ecological actors, not constant charge spam;
- Flamehorn donor rideability does not automatically create a second permanent mount progression beside the already-canonical Caravan Elephant;
- exact field-boss visual identity is not locked until a distinct external candidate passes review.

---

# 2. External game-content precedents

## 2.1 Horizon Forbidden West — settlements as visible culture

Guerrilla's settlement design is a strong reference because people, props, occupations, animations and services all communicate how a local culture survives.

Useful lessons:

- NPCs should visibly use the world around them rather than stand near decorative workstations;
- merchants and craftspeople should make sense for their culture/economy;
- settlement architecture and behavior communicate values before dialogue does;
- narrative, quest, environment and living-world teams need the same regional intention.

Project adoption:

- Southstone's foundry workers actually move metal/coal/tools within bounded ambient routines;
- caravan handlers load packs, inspect harnesses and use depots;
- guards train/maintain signal posts because the march is physically dangerous;
- merchants specialize in travel gear, metalwork, hides and route supplies because those are local strengths;
- Elephant handlers/stables are visibly part of ordinary caravan logistics before the player unlocks the mount.

Not adopted:

- large numbers of decorative NPCs that tank Minecraft performance;
- culture represented only through exposition text;
- every service duplicated in every settlement.

Reference:

- `https://blog.playstation.com/2021/11/22/horizon-forbidden-west-an-authentic-world/`

## 2.2 Dragon Age: Inquisition — story/system integration

BioWare's GDC retrospective explicitly found that discrete narrative content scaled poorly while standalone systems felt disconnected, leading to a combined approach.

Project adoption:

R09's repeatable/systemic content always gets local context:

- caravan defense exists because specific roads carry metal/food to R10-facing settlements;
- signal-tower events change route safety/readability;
- heavy wildlife events relate to herd territory or road encroachment;
- foundry supply events feed actual merchant/forge presentation;
- fortress content explains historical centralized logistics rather than being `bandits in another fort`.

Not adopted:

- open-world filler that exists only to generate map icons;
- narrative chains that require a bespoke cinematic for every minor event.

Reference:

- `https://www.gdcvault.com/play/1022377/Worlds-Collide-Combining-Story-and`

## 2.3 The Witcher 3 — living economy / off-main-route value

Useful lesson:

- open-world activities need economy, loot and regional reasons to remain worthwhile when the main quest is not being followed;
- towns, roads and merchants work better when they belong to the surrounding landscape/economy.

Project adoption:

- dense metal/hardstone/hide feed forge, housing, equipment and merchant loops;
- caravan routes create natural places for contracts/events/trade opportunities;
- roadside forts/depots are physically useful landmarks and service nodes, not only lore props;
- optional heavy hunts produce signature materials/trophies without becoming the only way to progress.

Reference:

- `https://www.gdcvault.com/play/1023867/The-Living-World-of-The`

## 2.4 Monster Hunter: World — heavy wildlife as ecology

Useful lesson:

- large animals feel better when their body mass, territory and environment explain combat behavior;
- not every large creature needs to be an always-hostile boss;
- open/dry terrain can create clear charge and positioning gameplay.

Project adoption:

- Rhino/Elephant/Flamehorn encounters use readable territory and commitment;
- charge attacks have visible lines and overshoot instead of magnetic turning;
- normal herd animals may be avoided without fighting;
- field-boss arena design uses broad terrain and line-of-sight rather than a tiny ring.

Reference direction:

- Capcom Wildspire ecology material already used in R06/R07 regional research.

---

# 3. External-first source stack

## 3.1 Alex's Mobs Continued — dryland large-animal ecology

Current 26.2 Fabric continuation is active and preserves the original creature roster/models/animations.

Strong R09 candidates:

- **Rhinoceros**;
- **Elephant**;
- **Kangaroo** where terrain/model role fits;
- **Maned Wolf** as sparse dryland predator/ambient wildlife;
- selected birds/insects as ambience.

Recent 26.2 changelog evidence specifically includes pathfinding fixes for rhinos and elephants, making current behavior review worthwhile before authoring exact regional density.

Project rules:

- project data owns Lv, stats, spawn density, loot and aggression/encounter eligibility;
- animals keep neutral/territorial roles where appropriate;
- no donor loot automatically becomes regional economy;
- current source/license metadata remains handled exactly through dependency/provenance rules.

Reference:

- `https://www.curseforge.com/minecraft/mc-mods/alexs-mobs-continued`

## 3.2 Caravan Elephant mount integration

`MOUNTS.md` already locks the Caravan Elephant as R09's heavy utility mount.

Canonical role preserved:

```text
cruise: 6.3 b/s
charge: 9.0 b/s for 2.0 s
charge cooldown: 8 s
Resolve: 2.0x player MaxHP
registration/harness after quest: 1,500 Gold
```

R09 supplies the missing authored acquisition context:

- player first sees working caravan elephants before owning one;
- handler/route quest proves the player's competence around a large animal;
- unlock is not random breeding/taming RNG;
- registration fee is paid only after the quest/world unlock;
- no giant mobile chest inventory is added.

## 3.3 Threateningly Mobs Continued — Flamehorn / Executioner

### Flamehorn

Original/current lineage confirms a redesigned Flamehorn with rideable behavior and multiple skins.

Project use:

- R09 treats Flamehorn primarily as a rare heavy wild/elite identity;
- donor rideability is disabled/not exposed as a second core mount at baseline because Caravan Elephant already owns R09's mount progression;
- one distinct skin/model state may be reserved for a rare authored regional encounter only if it reads as a genuine visual identity, not a recolor boss;
- exact combat kit waits for current 26.2 model/animation inspection.

### Executioner

Executioner exists in the Threateningly Mobs heavy/high-tier roster and gallery.

Project use:

- only in authored fort/ruin content;
- primary dungeon-boss candidate if current 26.2 presentation, animations and hitbox quality pass inspection;
- never a random roadside humanoid spawn;
- if presentation fails, use another external armored humanoid/guardian rather than forcing the donor.

Normal dependency use remains separate from copying continuation bytes into the public repository.

## 3.4 Field-boss asset gate

R09 needs a distinct optional major hunt beyond normal Rhino/Flamehorn ecology.

Candidate directions after actual visual comparison:

1. a truly distinct Flamehorn high-tier skin/model state with authored behavior if it reads as more than recolor/scale;
2. another external animated giant dryland/armored creature from a redistributable pack;
3. a model-linked named Rhino/megafauna encounter only if unique armor/anatomy/animation can make it visually honest.

Status:

```text
FIELD BOSS FINAL VISUAL IDENTITY: OPEN ASSET GATE
FIELD BOSS ROLE/ARENA/REWARD SLOT: LOCKED
```

No `Rhino x2 scale` placeholder is accepted.

---

# 4. Story role — resilience built by people

R09 is one of the R08/R09 Act-III high-tier evidence routes.

Its evidence package argues:

> Some regions replaced ancient centralized functions with redundant roads, depots, local agreements and maintainable physical infrastructure. Modern resilience can be a social/engineering achievement rather than a magical inheritance.

Regional history:

- old Anchor-era logistics linked signal stations, foundry allocation, road forecasts and selected automated gates;
- the ancient system made long-distance movement efficient but concentrated routing/production decisions in distant control centers;
- after the network degraded, Southstone settlements did not wait for restoration;
- caravans established redundant roads, communities built shared depots, foundries standardized repair parts and signal keepers created local warning chains;
- the modern system is less efficient on paper but understandable, repairable and resilient to a single distant failure;
- Restoration Director teams propose reconnecting the old logistics layer to support rapidly growing R10-facing demand;
- the player discovers that the old route optimizer would close or deprioritize several modern settlements because they are inefficient compared with the central corridor;
- the danger is not a magical explosion; it is loss of local control and hidden dependency.

Regional resolution:

- preserve useful old sensors/maps where they improve safety;
- do not restore remote route/foundry allocation authority;
- connect old information into the modern local signal network instead;
- prove that technical knowledge can be reused without recreating old governance.

---

# 5. Local people / regional conflict

Primary hub: **caravan fort / foundry outpost** at a major road fork between dry grassland, rocky coast and R10-facing routes.

Normal life visibly includes:

- metal unloading/sorting;
- forge/foundry work;
- wagon/pack repairs;
- Elephant handling/harness fitting;
- hide/leather work;
- signal tower shifts;
- route guards training;
- travelers sleeping/eating/restocking;
- merchants exchanging R07/R10/R11 goods.

Key local roles:

## 5.1 Route Marshal

- coordinates road patrols, signals and caravan departures;
- values redundancy and local route knowledge;
- accepts old sensors if they provide information but rejects opaque remote control;
- owns regional route/event context.

## 5.2 Foundry Master

- manages high-quality metal output and repair standards;
- cares about throughput but knows a forge that only works when a distant machine approves allocations is fragile;
- ties dungeon logistics evidence to real equipment production.

## 5.3 Elephant Handler / Caravan Keeper

- manages caravan elephants and working animals;
- runs the authored mount unlock;
- teaches handling/charge space through a real route problem rather than a menu tutorial;
- no breeding-stat treadmill.

Recurring Engineer/Smith, Cartographer/Ranger, Rival Wanderer and Restoration representatives can appear as main-story routing demands.

---

# 6. Spatial progression / local pressure

Suggested entry Lv remains **44**.

| Sub-area | Local pressure | Role |
|---|---:|---|
| R07/R06 road transition | Lv 43–44 | route entry / caravans |
| caravan fort / foundry | Lv 44 | hub / services / mount lead |
| dry grassland herd routes | Lv 44–46 | ecology / gathering / open combat |
| eroded coast / stone shelves | Lv 45–47 | resources / alternate route |
| signal ridges / ruined forts | Lv 46–48 | elites / regional story |
| field-boss territory | **Lv 49** | optional major hunt |
| canyon fortress / foundry vault dungeon | Lv 47–50 | regional dungeon |
| Executioner/guardian target | **Lv 50** | dungeon climax |

R10 begins at Lv58, so R09 has room for optional high-tier contracts/hunts and acts as a physical preparation bridge.

---

# 7. Route/traversal contract

R09 is about **roads that matter** without becoming a delivery simulator.

### Guarded trade road

- safest long route;
- waystations/signal posts;
- more events/merchants/caravans;
- suitable for Trail Stag/Komodo/Elephant.

### Ridge cut

- shorter/direct;
- heavy wildlife/elite risk;
- strong vistas and mineral nodes;
- no invisible boundary preventing early use.

### Coastal shelf

- slower, resource/fishing/discovery route;
- rocky coves and eroded formations;
- connections toward R11 where geography supports it.

### Old military road / fortress line

- ruins and historical logistics;
- opens shortcuts after regional progress;
- feeds the dungeon rather than becoming endless fort copies.

No route applies permanent speed debuffs.

---

# 8. Signal-network world system

R09's signature regional world system is a **simple physical signal chain**, not a minigame UI.

Selected towers/waystations can be restored during regional play.

Benefits may include:

- safer/readable road presentation;
- changed dynamic-event pool;
- additional traveling merchant/caravan appearances;
- one route shortcut/gate available;
- clearer warning for a major wildlife event.

Rules:

- no tower-capture checklist covering the whole region;
- target 3 major signal nodes, not 20;
- each node sits at a meaningful POI and has its own route/combat/story reason;
- activation is a concise final interaction after the actual gameplay challenge;
- state is server-authoritative and late-join safe.

---

# 9. POI package

## Major POI A — Southstone Caravan Fort

Functions:

- primary hub;
- economy/culture landmark;
- foundry + route + Elephant systems intersect here.

## Major POI B — Three-Road Signal Ridge

Functions:

- panorama/navigation;
- signal restoration;
- route choice made visually clear;
- optional elite/resource encounter.

## Major POI C — Herdstone Flats

Functions:

- Rhino/Elephant dryland ecology;
- handler/caravan quest context;
- open combat and gathering without a mandatory boss.

## Major POI D — Broken Foundry Viaduct

Functions:

- metal/economy route;
- physical shortcut repair;
- visible logistics history;
- one regional event family.

## Major POI E — Field-boss Range

Functions:

- optional major hunt after external model selection;
- broad terrain for readable heavy-combat identity;
- trophy/signature material source.

## Major complex — Canyon Fortress / Foundry Vault

Handled in §16.

Smaller discoveries may include:

- abandoned signal hut;
- hardstone quarry shelf;
- elephant watering stop;
- old road-marker line;
- wrecked caravan with actual event/history context;
- coastal cave/fishing cove;
- Flamehorn territory marks;
- small memorial/road shrine.

---

# 10. Settlement / services / housing

Target daytime population:

```text
10–14 functional workers/guards/handlers/merchants
6–10 ambient residents/travelers
```

Baseline services:

- shrine/fast travel;
- inn/rest/food;
- strong forge/foundry service;
- Material Vault/bank;
- caravan/general merchant;
- route/contract board;
- stable + Elephant handler;
- equipment merchant emphasizing physical/guard builds;
- basic alchemy/healer access but not R06-level specialization.

## Housing

R09 is a good candidate for:

- 1–2 Large House-class stone/courtyard properties around the ~25,000 Gold band;
- practical workshop/garage-like furnishing space if external assets fit;
- no economic income/automation from owning a foundry-style home.

Housing remains one residence at a time.

---

# 11. Gathering / economy package

R09 should enrich forge/housing loops without creating `Southstone Ore` spam.

## Dense metal role

Use existing Iron/Silver/other accepted metals where geology permits plus **one high-quality metal component/alloy input role** only if external model/recipe direction is coherent.

Do not create an ore solely because R09 is higher level.

## Hardstone

May exist as an authored quarry/contract/furnishing material role if it has real use.

- not a generic block-mining grind;
- gathered from specific quarry/stone nodes;
- used in furnishing/route/forge orders.

## Clay

Continues from R06/R07 where geography fits; not a new ID.

## Hide / heavy-animal material

Rhino/Elephant ordinary hunting is **not** encouraged as the main progression loop.

- Elephant mount ecology should discourage turning working/tame herds into material farms;
- hide primarily comes from hostile/eligible regional creatures or authored trade sources;
- no mandatory slaughter loop.

## Dry medicinal plant

One accepted external plant role for recovery/alchemy/trade; final name waits for intake.

---

# 12. Fishing / coast collection

R09 uses coastal coves/rock pools/river mouths only where actual Azari geography supports them.

Fish Codex target:

```text
2–4 regional/shared identities
```

This is a bridge toward R11 rather than a major fishing region.

One rocky-coast trophy species may appear if an accepted animated fish model fits.

---

# 13. Ecology / heavy-combat roles

## Rhinoceros

- neutral/territorial heavy wildlife;
- clear warning/head-lower tell;
- committed charge with limited steering;
- broad disengage territory;
- not routine hostile spam.

## Elephant

- working/domestic and wild contexts clearly separated;
- ordinary wild adults neutral/defensive;
- project avoids farming them for loot;
- handler quest uses behavior/space rather than random feeding RNG.

## Kangaroo / Maned Wolf

- sparse ecology/secondary threat roles only if current model behavior adds diversity;
- not mandatory roster slots.

## Flamehorn

- rare heavy creature / elite identity;
- current external model/animation review required;
- donor rideability is not exposed as a second permanent R09 mount baseline;
- preserve strong horn/charge/fire body language only if visible and readable.

## Steelboar

- low-density continuity only;
- not a second R01-style main elite.

---

# 14. Caravan Elephant unlock

This regional chain closes the mount design already locked in `MOUNTS.md`.

Working sequence:

```text
meet working caravan herd at fort
→ help resolve a route blockage / predator or signal failure without simply killing random animals
→ learn heavy-mount handling in a short authored space
→ accompany/operate one short caravan transfer with the Elephant rather than walking behind an NPC for ten minutes
→ register personal Caravan Elephant for 1,500 Gold
```

The trial should demonstrate:

- slower acceleration;
- wide turning radius;
- high Resolve;
- charge line commitment;
- visible body width/collision.

No mount-level XP, bond meter or breeding grind.

---

# 15. Dynamic event package

## Signal Down

- restore/defend one major signal node;
- route safety/event pool changes temporarily or permanently as authored;
- not a repetitive tower checklist.

## Caravan Repair

- caravan is stopped, not walking slowly across the map;
- player can gather/repair/defend or solve a nearby threat;
- success returns visible movement to the road.

## Herd Crossing

- large neutral animals occupy a road/flat;
- may become a navigation/behavior encounter without combat;
- demonstrates ecology and road culture.

## Foundry Supply

- brief event involving ore/road/elite threat;
- reward feeds forge/economy rather than tokens.

---

# 16. Regional dungeon — Canyon Fortress / Foundry Vault

Target first-clear wall-clock:

```text
~25–35 minutes
```

R09 dungeon identity is **fortification, industrial space and physical combat**, not another magical ruin.

## Stage 1 — Battlement approach

- visible fortress landmark from regional routes;
- exterior assault/infiltration has at least two approaches;
- ranged/cover and heavy guard pressure introduced.

## Stage 2 — Caravan yard / logistics hall

- old route allocation infrastructure;
- storage, gates, carts and signal devices show fortress purpose;
- environment tells the logistics story before archive text.

## Stage 3 — Foundry

- large industrial room / forge machinery;
- heavy enemies with space to commit attacks;
- optional high-quality metal/material branch.

## Stage 4 — Lower vault / control archive

- evidence reveals old remote allocation logic and modern route incompatibility;
- one local information feed can be preserved while authority is disconnected;
- Act-III evidence package obtained here.

## Stage 5 — Executioner / guardian chamber

- broad physical arena;
- pillars/gates/forge props support positioning;
- no endless add waves;
- final boss uses external animation/hitbox quality as hard gate.

## Repeat shortcut

After first clear:

- gate/lift/stair connects entrance yard to lower foundry/vault;
- repeat run removes story-only logistics interactions;
- target repeat route remains combat/loot focused.

---

# 17. Executioner dungeon-boss candidate

Working target if the current external model passes:

```text
Lv: 50
role: dungeon boss
solo active TTK: ~180–215 s
Poise: ~240–270
```

Required identity:

- heavy humanoid commitment;
- readable weapon reach matching visible model;
- guard/counter/poise interplay;
- no humanoid boss skating while swinging;
- one anti-turtle/space-control move;
- phase change based on stance/weapon behavior, not only stat buffs;
- no generic dark-magic projectile spam unless the external source visibly supports it.

If current Executioner presentation fails review, replace with a higher-quality external armored humanoid/guardian. Do not lower the quality bar merely because the name is already in `REGIONS.md`.

---

# 18. Field-boss mechanical slot

Final visual identity is still open, but the role is locked:

```text
Lv: 49
role: optional major dryland hunt
solo active TTK: ~195–230 s
Poise: ~240–270
```

The boss must:

- be visibly distinct from common Rhino/Elephant/Flamehorn;
- use broad open terrain;
- emphasize committed movement, impact and positioning;
- remain melee-accessible;
- not be a scaled normal animal with a boss bar.

Asset intake must close this before implementation.

---

# 19. Main-story / regional quest flow

## `Three Roads South` role

- arrive at the fort and learn current road/signal problems;
- player chooses first route/signal issue to investigate;
- regional culture appears before Anchor exposition.

## `Signals Without Masters` role

- restore two signal nodes in flexible order;
- discover old sensors still work while central control is unreliable;
- locals already built a manual network around them.

## `The Weight of a Caravan` role

- integrates Elephant unlock and route logistics;
- demonstrates distributed supply rather than a fetch-quest lecture.

## `The Old Allocation` role

- fortress dungeon / Act-III evidence;
- reveals historical optimizer would deprioritize modern side settlements/roads;
- player preserves information but rejects remote allocation authority.

Optional field-boss hunt remains separate from main evidence.

---

# 20. Regional outcome / memory

Shared late-join-safe changes may include:

- 2–3 major signal nodes active;
- one viaduct/gate shortcut repaired;
- more caravans/travelers appear on key roads;
- foundry merchant stock expands;
- Elephant handler/stable visibly operates expanded routes.

Personal consequences:

- R09 high-tier evidence package;
- Caravan Elephant unlock/registration state;
- local NPC dialogue/relationship flags;
- field-boss hunt state;
- first-clear/reward/discovery state.

The region should feel **better connected by people**, not transformed by magical infrastructure.

---

# 21. Reward identity

Regional equipment emphasis:

- heavy armor / Defense / END;
- guard efficiency / perfect-guard payoff;
- poise damage and stagger resistance;
- committed heavy-weapon payoff;
- impact/knockback resistance;
- merchant/travel utility kept modest so it does not become mandatory economy gear;
- high-quality physical weapon components;
- selected Hunter precision/weak-point pieces so the region is not melee-exclusive.

No caravan token, signal currency or Elephant XP.

---

# 22. Audio / presentation

R09 sound should feel physical and exposed:

- wind across dry grass/stone;
- wagon/rope/harness creak;
- hammer/foundry rhythms near hub;
- herd footfalls at distance;
- signal bell/horn motifs;
- coast/waves where relevant.

Dungeon:

- exterior wind transitions into stone/metal resonance;
- foundry has low mechanical/forge layers;
- Executioner/heavy boss footsteps and weapon movement must communicate commitment.

Do not over-score normal caravan movement with constant heroic music.

---

# 23. Multiplayer / authority

Server owns:

- signal/road shared world states;
- quest/evidence state;
- Elephant unlock/summon/Resolve/charge hit validation;
- dungeon encounter state;
- field-boss state;
- personal loot/reward claims;
- event contribution.

Required multiplayer tests later:

- one player activating signal world state does not skip another's personal quest interaction;
- late join sees restored roads while retaining personal evidence flow;
- Elephant charge and passenger/collision state remain server-consistent;
- support contribution qualifies in events/bosses;
- dungeon shortcut/evidence survives reconnect;
- no duplicate mount registration/reward.

`MULTIPLAYER TESTED` remains NO until real-client verification.

---

# 24. Performance constraints

- bounded caravan/ambient NPC counts;
- no region-wide per-tick road scanning;
- signal state is explicit saved data;
- large-animal pathfinding density capped aggressively;
- Rhino/Elephant herd responders use local caps;
- foundry ambient animations use bounded schedules;
- field-boss AI only active in encounter zone;
- coast/long sightlines profiled with target entity/render distance.

---

# 25. Asset-intake blockers

R09 is not asset-ready until:

1. current 26.2 Rhino/Elephant/Kangaroo/Maned Wolf model/behavior review;
2. current Flamehorn model/skin/animation review;
3. current Executioner model/animation/hitbox review;
4. final distinct field-boss external model;
5. coherent caravan-fort/foundry architecture/prop family;
6. Elephant harness/tack/player seating visual acceptance;
7. exact dense-metal/hardstone/dry-herb node models;
8. R09 armor/weapon/accessory external visual families;
9. signal tower/road-marker visual family;
10. dungeon/foundry machinery assets;
11. impact/forge/VFX/audio sources;
12. coastal fish/trophy model assignments where used.

No scaled vanilla Ravager/Iron Golem, horse caravan or temporary flat-skin NPC is accepted as finished presentation.

---

# 26. R09 quality acceptance

Real play must prove:

- R09 feels high-tier despite being visually grounded;
- roads, signals, caravans and foundry economy create a distinct regional identity;
- settlement NPCs visibly interact with their work rather than stand as menus;
- route choices matter without turning travel into escort labor;
- heavy wildlife feels territorial and readable rather than spammy;
- Caravan Elephant is a meaningful handling/Resolve/charge unlock rather than only a reskinned horse;
- the region communicates distributed human resilience through play/world state;
- fortress dungeon differs strongly from magical/natural prior dungeons;
- field boss is visually distinct once intake closes;
- Executioner/guardian combat respects visible reach and commitment;
- post-clear caravans/signals/merchant state visibly change;
- multiplayer/save authority remains correct;
- performance survives large animals + NPC hub + long sightlines.

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
