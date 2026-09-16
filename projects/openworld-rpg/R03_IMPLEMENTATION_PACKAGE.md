# Open-World RPG — R03 Whitecrest Highlands Implementation Package

> Status: **DESIGN CANON — R03 world/story/traversal/service/combat/reward flow is implementation-ready; exact external asset file bindings remain gated where marked**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Story spine: `WORLD_STORY_CANON.md`  
> Region graph: `REGIONS.md`  
> Combat: `COMBAT_BALANCE.md`, `STATUS_AND_R01_ENCOUNTERS.md`  
> Loot/equipment: `LOOT_ECONOMY.md`, `EQUIPMENT_BALANCE.md`  
> Field systems: `GATHERING_FISHING_CAMP_HOUSING.md`, `FISHING_COLLECTION_HOUSING_MARKET.md`  
> Quest/state: `QUEST_WORLD_STATE.md`  
> External provenance: `EXTERNAL_SOURCES.md`  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins.

R03 is the first regional package written under `WORLD_STORY_CANON.md` and `DESIGN_COMPLETENESS_AUDIT.md` rather than the older `biome + mob list` pattern.

Its play sentence is:

```text
read a mountain from below
→ choose switchback / mine / ridge approaches
→ open lifts, bridges and safe shortcuts as you climb
→ gather valuable exposed minerals without strip-mining
→ discover why old observatory machinery is tied to distant regions
→ survive a true high-altitude apex hunt
→ descend through a collapsed mine into an ancient forge-observatory
→ leave with the first physical proof that the Anchors form one continental network
```

R03 should make vertical traversal feel **conquered**, not merely annoying.

---

# 1. Locked regional identity

Preserved from `REGIONS.md`:

- terrain: western / southwestern great mountains, windswept ridges and highland meadows;
- suggested entry Lv: **12**;
- first major vertical-traversal region;
- cliffs and passes matter without requiring flight;
- fortified mining town / cliff outpost;
- mineral identity: existing Iron plus a real silver-family resource and one later highland-crystal role;
- lifts, bridges and shrine shortcuts replace permanent movement penalties;
- dungeon transitions from collapsed cliff mine into ancient observatory/forge;
- reward identity: guard strength, poise damage, heavy weapons, ranged weak-point play and knockback resistance.

Production correction:

- **Basalt Wyvern is removed from R03's field-boss slot.** Its external source identity is badlands/basalt-biome oriented and already fits later volcanic R10 much better;
- R03 instead uses a dedicated high-altitude Griffin field-boss candidate with a legally usable external animated model;
- this is a source-fit correction, not a reduction in R03 content.

---

# 2. External-first source stack

## 2.1 Mountain environment / structures

Primary direction:

- preserve Azari's actual mountain terrain as the macro silhouette;
- use coherent Quaternius/Kenney/KayKit external prop and structure families for mining, ropes, carts, ore, work areas and settlement dressing where already accepted by project provenance;
- use Quaternius modular ruins/dungeon families as details/reference for the buried observatory/forge rather than building the final dungeon from random vanilla stone-brick rooms;
- use authored Minecraft terrain/block composition for giant cliffs and roads instead of thousands of decorative model entities.

R03 needs external visual language for:

- mechanical lift/counterweight stations;
- suspension/stone bridge repair states;
- ore-processing props;
- old observatory instruments;
- ancient forge machinery;
- mine supports/carts/cranes;
- wind-exposed highland camp props.

Exact Anchor/observatory machinery is still an asset-intake blocker; `generic glowing cube` is not accepted.

## 2.2 Griffin field-boss candidate

Primary current candidate:

- VitSh `Griffin Animated` on Sketchfab;
- animated low-poly model;
- 670 triangles / 337 vertices on the current listing;
- published under Creative Commons Attribution.

Reference:

`https://sketchfab.com/3d-models/griffin-animated-a8852f113416426bb06e6bba49a525a9`

Status:

```text
STRONG EXTERNAL CANDIDATE — exact downloaded artifact / animation clip inspection still required
```

Before production binding:

1. acquire the exact downloadable artifact;
2. preserve creator/license/attribution evidence;
3. record local SHA-256;
4. inspect all animation clips;
5. test wing span, collision and camera readability at Minecraft scale;
6. verify flight/landing animation supports the authored combat kit below;
7. only then lock the final player-facing regional species/title.

Do not replace failure with `large vanilla bird + particles`.

## 2.3 Rock Golem dungeon-boss candidate

Primary current candidate:

- Dm3d `Rock Golem` on OpenGameArt;
- animated low-poly dungeon monster;
- downloadable `RockGolem.blend` artifact listed by the source;
- source page lists CC-BY 3.0 and CC-BY-SA 3.0 and contains an explicit attribution chain to the original/rigging contributors.

Reference:

`https://opengameart.org/content/rock-golem`

Status:

```text
STRONG EXTERNAL CANDIDATE — conversion / attribution / animation-quality gate required
```

The public-repo integration must choose and document the exact applicable license/attribution path before committing derivative asset bytes.

If this old Blender artifact fails conversion, fallback order is:

1. another redistributable animated external stone/golem model with equivalent or better motion quality;
2. Quaternius CC0 Ultimate Monsters / Animated Monster families after an exact model is visually accepted;
3. not a scaled vanilla Iron Golem.

## 2.4 Resource visual source

KayKit `Resource Bits` is a strong direct source for ordinary R03 metals because the current CC0 pack explicitly includes:

- Iron;
- Copper;
- Silver;
- Gold;
- Stone and other resource props.

Reference:

`https://kaylousberg.itch.io/resource-bits`

R03 therefore keeps ordinary naming and uses **Silver** rather than inventing `Whitecrest Alloy Ore` solely for flavor.

---

# 3. Story role

R03 is the first region to prove that the ancient sites are a **network** rather than isolated local ruins.

Local story sentence:

```text
mountain workers need broken routes reopened
→ old machinery seems like a practical shortcut
→ repairs reveal that the lifts/observatory once exchanged measurements with distant Anchor sites
→ local people now have a real reason to debate whether ancient infrastructure should be trusted again
```

R03 must work both as:

- a complete regional story for a player who does not care about lore; and
- one valid Act-I main-story evidence route from `WORLD_STORY_CANON.md`.

The Griffin remains natural apex wildlife and is **not** revealed to be an Anchor creation.

---

# 4. Local people / regional conflict

R03's primary settlement is a fortified mining town / cliff outpost built around a lower safe shelf.

The central local problem is practical:

- a sequence of rockfalls, broken lifts and unstable high bridges has cut miners/scouts off from upper workings;
- demand for exposed metal remains high;
- the town can reopen old paths slowly by conventional means, or reactivate pieces of ancient machinery discovered below the mine;
- nobody initially knows the machinery is networked to distant facilities.

Use two recurring local perspectives without adding a reputation grind:

### Mine Foreman / Trade Lead

- wants safe access restored quickly;
- represents workers, supply and economic pressure;
- does not demand reckless sacrifice;
- gives concrete reasons why infrastructure matters.

### Ridge Warden / Route Keeper

- knows old trails, wildlife territory and avalanche/rockfall behavior;
- distrusts shortcuts that people do not understand;
- provides the strongest early warning that the old observatory reacts to events beyond R03.

The recurring `Engineer / Smith` and `Anchor Scholar` roles from `WORLD_STORY_CANON.md` can enter this region naturally during the main route.

---

# 5. Spatial progression / local pressure

Suggested entry Lv remains **12**.

| Sub-area | Local pressure | Role |
|---|---:|---|
| R02/R01 mountain foothill transition | Lv 11–12 | readable approach / lower meadow |
| mining town / lower shelves | Lv 12–13 | services / tool upgrade / first shortcuts |
| switchback quarry roads | Lv 12–14 | common wildlife / exposed ore |
| cliff mines / broken lift works | Lv 13–15 | cave threats / repair objectives |
| high ridge / wind shelves | Lv 14–16 | dangerous traversal / rare ecology |
| Griffin plateau | **Lv 17** | optional field boss |
| collapsed mine → observatory/forge dungeon | Lv 15–18 | major regional dungeon |
| Rock Golem boss role | **Lv 18** | dungeon climax |

The player may enter R04 or other stronger regions before completing R03. No regional wall is created.

---

# 6. Vertical traversal language

R03 explicitly avoids a new always-on climbing stamina system.

Use the world itself:

- switchback trails;
- narrow but readable ledges;
- ladders/scaffolds only where believable;
- mine tunnels that reconnect height bands;
- suspension/stone bridges;
- mechanical lifts;
- one or two deliberate short parkour/traversal moments, not constant block-jump chores;
- shrine/major shortcuts after meaningful route discovery.

## 6.1 Shortcut progression

The player can open **three major convenience connections** during normal regional play:

1. lower-town → mid-mine lift;
2. broken gorge crossing / bridge route;
3. upper ridge → observatory approach lift or equivalent shortcut.

These are strong candidates for `WORLD_PERSISTENT` shared changes because they are beneficial, readable and late-join safe.

They must be:

- server-authoritative;
- idempotent;
- permanently saved for the world once repaired;
- still explainable to a player who joins later.

Repair is not a construction minigame. It uses one concise interaction after the player solves the actual route/encounter/material requirement.

---

# 7. POI package

R03's POIs follow the 4-axis rule in `WORLD_STORY_CANON.md`.

## Major POI A — Broken Skybridge

Functions:

- visible navigation landmark across a gorge;
- early route-choice problem;
- small combat/resource pocket below;
- later permanent shortcut after repair.

## Major POI B — Silver Cut

Functions:

- first obvious Refined-tool mining payoff;
- dangerous side mine rather than strip-mining;
- ore, contract and combat value;
- teaches that valuable nodes are authored world destinations.

## Major POI C — Windwatch Eyrie

Functions:

- major panorama / map orientation;
- wildlife observation and Griffin clue source;
- rare gathering/fishing-weather clue where relevant;
- no chest required to justify the visit.

## Major POI D — Abandoned Liftworks

Functions:

- visible industrial story;
- repair/shortcut loop;
- first explicit hint that some machinery is older than the town;
- links local infrastructure to main-story investigation.

## Major POI E — High Plateau / Griffin Hunt

Functions:

- optional field-boss arena;
- visible from multiple lower sightlines;
- high-value hunt and trophy source;
- natural apex ecology, not an Anchor facility.

## Major complex — Observatory Forge

Handled as the regional dungeon in §13.

Smaller discoveries can include:

- old survey cairns;
- collapsed prospect camps;
- highland springs;
- mineral seams visible from route overlooks;
- abandoned rope stations;
- weather shelters;
- rare wildlife nesting points.

Do not fill these with identical chests.

---

# 8. Settlement / services

Target daytime functional population:

```text
8–12 workers / guards / service NPCs
4–7 ambient residents / travelers
```

No vanilla villagers.

Baseline services:

- shrine / fast travel;
- inn / rest / food;
- strong smith / ore-processing service;
- bank / Material Vault;
- regional merchant;
- mining / route contract board;
- lift engineer / repair interaction;
- stable hitch / mount convenience;
- basic fish/food sale through inn/merchant where needed.

R03 intentionally **does not** duplicate every class/alchemy specialist.

## Housing

R03 may offer 1–2 stone/highland **Town House-class** alternatives around the existing regional price band.

It does **not** introduce the next Large House tier merely because another region appeared. Housing progression should feel economically meaningful, not automatic every zone.

---

# 9. Gathering / tool progression

## 9.1 Iron Ore

Existing resource remains relevant.

R03 uses richer exposed nodes and mine pockets, not `Iron Ore II`.

## 9.2 Silver Ore

First strong new metal identity for R03.

External visual source:

- KayKit Resource Bits Silver family, CC0.

Baseline design:

```text
yield: 1–2
personal respawn: 9 active min
required tool: Refined Pick
locations: authored exposed veins / Silver Cut / dangerous high mine pockets
uses: regional equipment, selected accessories, later cross-region crafting, ordinary sale
```

Silver is not a currency.

## 9.3 Refined Pick availability

R03 must not softlock its own Silver behind a tool only obtainable elsewhere.

The mining-town smith can upgrade the Field Pick into Refined quality using **existing early materials + Gold**, with no Silver requirement.

Working authoring target:

```text
6–8 Iron Ore
2 Hardwood
~250–350 Gold service cost
```

Exact cost remains tuneable against actual R01/R02 income, but implementation must not invent a circular Silver requirement.

The Refined Pick keeps the already-canonical 85% gathering-time multiplier and dense-regional-node access.

## 9.4 Highland crystal role

R03 still needs one visually distinctive highland crystal/mineral for later forge/magic crossover.

However the final player-facing name/model remains **UNLOCKED** until an exact external model passes intake.

Do not lock `Highland Crystal` as production content solely because a design table wants another material.

---

# 10. Fishing / collection

Fishing remains optional and lighter than R02.

Use:

- mountain streams;
- sheltered tarns;
- mine-fed spring pools only where ecology makes sense.

R03 Fish Codex target:

```text
2–4 regional/shared fish identities
```

At least one fish should overlap with adjacent lower waters so the world feels ecological rather than every border resetting the species table.

One uncommon/trophy highland catch can support:

- selling;
- cooking;
- Fish Codex personal best;
- housing trophy display.

Final species names wait for actual accepted external fish models.

---

# 11. Ecology / encounter roles

R03 should feel windswept and spacious. It does not need hostile trash every 20 blocks.

## 11.1 Lower-upland bison / sparse Steelboar

Existing creatures may persist at **low density** in lower transitional shelves.

They are ecology continuity, not R03's new identity.

## 11.2 Gelada troop role

Rocky-pocket/highland troop wildlife.

Project behavior target:

- usually neutral at range;
- group warning/display if the player pushes into a troop area;
- selected adults can throw a rock or rush when provoked;
- hard cap group responders so one mountain does not pathfind at the player.

Exact current dependency/model integration must pass the existing 26.2 external dependency gate before implementation.

## 11.3 Snow Leopard role

Rare upper-transition territorial predator.

Behavior target:

- stalks from cover/height where navigation permits;
- clear crouch/pounce tell;
- disengages after territory is escaped;
- rare enough that spotting one is meaningful.

It foreshadows R04 rather than filling all R03 ridges.

## 11.4 Cave threat

R03 mine interiors require a distinct cave pressure source, but **the earlier `Rocky Roller` name/identity is not considered production-locked until its exact external source is proven**.

Allowed implementation order:

1. use an accepted current dependency cave creature if its model/behavior fits the mine and license/runtime boundary;
2. select a distinct redistributable external animated stone/cave creature;
3. only then lock the player-facing species and exact combat kit.

Do not solve this by duplicating R01 Cave Centipede in every mine or by scaling the dungeon boss down into a common mob.

This is one explicit remaining asset-intake gate inside an otherwise closed region flow.

---

# 12. Griffin field boss

Working role:

```text
Lv: 17
role: optional field boss
HP target: ~10,000–10,800
Defense: ~52
MR: ~39
Poise: 195
solo active TTK target: ~190–215 s
```

Final numbers may move modestly after the accepted model's actual movement/airtime is profiled.

## Arena

- broad high plateau / saddle with cliff silhouettes but enough safe floor for readable combat;
- arena boundary comes from terrain/encounter disengage, not an invisible tiny circle;
- player must not be knocked off a lethal cliff by every ordinary wing touch;
- intentional edge danger can exist in specific telegraphed attacks.

## Flight contract

The Griffin is a flying boss but must remain fun for melee builds.

- routine airborne sequence should usually last **<=5–6 s** before a punishable ground/low-hover window;
- no repeated 20-second unreachable circles;
- melee players can meaningfully punish every major dive/landing cycle;
- ranged players gain safer chip opportunities but not exclusive access to the fight;
- flight path is server-owned; client animation is presentation.

## Attack 1 — Talon Sweep

```text
wind-up: 0.40 s
1–2 readable close hits
total benchmark damage budget: <=20%
guardable/perfect_guardable: true
recovery: 0.40 s
```

## Attack 2 — Wing Gale

```text
wind-up: 0.70 s
wide frontal/side gust
damage: ~14%
knockback: moderate, bounded
guardable: true
perfect_guardable: true
recovery: 0.65 s
```

This is space control, not a giant invisible damage cone.

## Attack 3 — Ridge Dive

Signature attack.

```text
airborne setup / tell: >=1.10 s
committed dive line
damage: ~32%
guard pressure: heavy
perfect_guardable: true
perfect_guard_poise_multiplier: 1.35
recovery / grounded punish: 1.30–1.60 s
```

Steering becomes sharply limited after final commitment.

## Attack 4 — Crosswind Landing

```text
visible landing tell: >=1.00 s
landing radius: ~4.0 blocks
benchmark damage: ~26%
guardable: false
perfect_guardable: false
recovery: ~1.10 s
```

The visible gust/dust/ground treatment must match the server radius.

## High-pressure state

Below ~40% HP:

- may chain one second short reposition/dive;
- every chained dive receives its own readable turn/tell;
- no generic permanent +damage/+speed steroid;
- landing punish window remains real.

## Weak-point rule

Do **not** lock a wing/head weak point until the imported model's anatomy and animation clips are inspected.

A weak point may become active during post-dive recovery if the accepted model provides a visually honest target.

## Rewards

Use normal field-boss personal reward rules:

First eligible defeat:

- guaranteed Superior+ normal R03 equipment roll;
- 2 model-linked signature materials after exact Griffin anatomy/model intake;
- 15% direct Mythic/signature roll;
- EXP ~20% current next-Lv requirement;
- Class XP ~15% current Class Rank requirement.

Repeat follows normal field-boss canon.

The signature material name is not invented before the model is accepted.

---

# 13. Regional dungeon — Collapsed Mine → Observatory Forge

Target first-clear wall-clock length:

```text
~20–30 minutes
```

The dungeon must not feel like R01 quarry with different stone.

## Stage 1 — Active mine failure zone

- recognizable recent mining works;
- vertical shaft / broken lift problem;
- one optional Silver/resource branch;
- local cave pressure;
- shows that the current crisis affects living infrastructure.

## Stage 2 — Exterior cliff connection

- short authored exterior ledge/bridge segment;
- broad mountain sightline;
- route loops back into older masonry;
- this is the signature spatial break from underground monotony.

## Stage 3 — Ancient forge

- old heavy machinery / counterweights / heat scars;
- combat room designed around space and heavy impacts rather than many small mobs;
- first clear visual evidence that the machinery was not built by the current mining town.

## Stage 4 — Observatory archive

- maps/instruments/records make the continental connection readable;
- the main-story evidence can be obtained without reading a ten-page lore dump;
- environment should visibly point to multiple distant nodes/regions.

## Stage 5 — Rock Golem chamber

- large observatory/forge guardian arena;
- mechanically distinct from Earthloong;
- emphasizes impact, guard/poise and punish windows.

## Shortcut

A central lift becomes usable after the observatory is reached and returns the player near the dungeon entrance / town-side route.

Repeat clear should not require the full first-clear repair route every time.

---

# 14. Rock Golem dungeon boss

Working role:

```text
Lv: 18
role: dungeon boss
HP target: ~9,000–10,000
Defense: ~68
MR: ~45
Poise: 210
solo active TTK target: ~165–185 s
```

Exact values wait for converted model movement/animation review within these bands.

Combat identity:

- slow enough to read;
- not so slow that the player simply walks behind it forever;
- high physical solidity / poise;
- major attacks create punish windows;
- phase change alters exposed behavior rather than only adding damage.

## Attack 1 — Stone Fist

```text
wind-up: 0.45 s
damage: ~11%
guard pressure: medium
guardable/perfect_guardable: true
```

May alternate sides as a readable two-hit sequence with total <=22% benchmark HP.

## Attack 2 — Forge Hammer Slam

Uses both arms/body if supported by the accepted animation.

```text
telegraph: 0.95 s
frontal impact + short ground shock
damage: ~27%
guard pressure: heavy
perfect_guardable: true
recovery: 1.10 s
```

The ground effect cannot extend beyond its visible crack/dust treatment.

## Attack 3 — Fault Line

```text
telegraph: 1.15 s
one or more clearly marked straight ground lanes
damage: ~28%
guardable: false
perfect_guardable: false
recovery: 1.00 s
```

This teaches leaving the line, not iframe-spamming inside a lingering hitbox.

## Attack 4 — Boulder Rush

Only if the imported rig/animation can support a believable short committed locomotion attack.

```text
pre-tell: >=0.85 s
short forward commitment
damage: ~24%
perfect_guardable: true
miss recovery: >=1.00 s
```

If the model cannot sell this attack, replace the attack during intake rather than forcing bad animation.

## Phase 2 — Fractured Shell at ~55%

Intended behavior change:

- outer armor/stance visibly cracks or opens using model-compatible treatment;
- Defense falls modestly;
- movement/turning becomes somewhat faster;
- one attack gains a second delayed ground trace;
- no long invulnerable transformation.

The exact visible fracture/core treatment remains gated by the imported model. Do not add an invisible `phase 2 buff` with no visual support.

## Status relation direction

- high physical Defense is already part of identity;
- do not grant blanket magic immunity;
- Poison/Bleed may be resistant or strongly resistant only if the accepted anatomy/presentation supports it;
- Lightning/Frost/Fire weakness is **not** invented until the final model/material language is visible.

## Rewards

First eligible clear follows dungeon canon:

Boss layer:

- guaranteed Superior+ normal R03 equipment;
- 2 model-linked signature materials after asset intake;
- 15% Mythic/signature roll;
- boss Class XP contribution.

Completion choice target:

Choose 1 of 3 distinct Item-Lv18 Superior candidates using already-supported external weapon families:

- heavy impact weapon role;
- defensive shield/off-hand role;
- ranged weak-point weapon role.

Exact player-facing names/models are locked only after the Quaternius/accepted external weapon models are selected.

Completion EXP/Class XP uses the existing normal first-clear dungeon percentages rather than a new R03-only economy.

---

# 15. Regional quest / discovery flow

Working regional chain structure:

## `The Closed Pass` role

- discover town and current route failure;
- inspect one broken connection;
- choose which accessible route to reopen first;
- introduces local people, not the continental mystery first.

## `Three Heights` role

- progress through lift/bridge/high-route restoration;
- each repair requires reaching the site through actual exploration/combat, not collecting three arbitrary UI tokens;
- at least two connections can be opened in flexible order.

## `Signals in the Stone` role

- old machinery responds unexpectedly during repairs;
- Scholar/Engineer main-story roles recognize that the signals are not local;
- opens observatory investigation without forcing every regional side quest.

## `The Observatory Below` role

- regional dungeon / main evidence;
- can satisfy the Act-I main-story evidence requirement from `WORLD_STORY_CANON.md`.

## Griffin hunt discovery

- begins from feathers/scratches/nest signs / distant sightings only after actual external model intake can support the clues;
- optional;
- never blocks the dungeon or R04 access.

Foreground quest density remains low: one main lead plus manually pinned optionals.

---

# 16. Regional outcome / memory

After the main regional chain/dungeon:

Shared late-join-safe world consequences may include:

- repaired selected lifts/bridges remain active;
- one upper route becomes a normal trade/travel path;
- smith/merchant stock reflects restored Silver access;
- town ambient dialogue changes from isolation to reopened-route concerns.

Personal consequences:

- observatory evidence recorded;
- regional completion/reward state;
- discovered Griffin hunt state;
- local NPC dialogue/relationship flags.

The region should visibly feel easier to **navigate** afterward, without being emptied of all threats/content.

---

# 17. R03 reward identity

Do not create a separate mountain currency.

Regional equipment/affix emphasis:

- guard efficiency / guard stability;
- poise damage;
- stagger/knockback resistance;
- heavy weapon commitment payoff;
- ranged weak-point / elevation-friendly precision;
- selected Stamina/END utility.

This does not mean every R03 item is heavy armor.

The region should contain useful choices for Hunter/Mage/Cleric players too, particularly weak-point, resource and defensive accessories.

---

# 18. Multiplayer / authority

Server owns:

- lift/bridge world state;
- quest/evidence state;
- boss encounter state;
- field-boss flight path/hit validation;
- gathering node ownership;
- first-clear/repeat reward eligibility;
- dungeon shortcut state;
- personal loot.

Specific tests later required:

- one player repairing a shared lift does not invalidate another player's personal quest interaction;
- late join sees repaired physical route but can still complete personal story/evidence;
- two players at different R03 quest steps can share boss/dungeon encounters safely;
- Griffin cannot desync between flying client animation and server hit position;
- fall/death during lift travel cannot duplicate or strand state;
- dungeon shortcut persists/reset rules behave after chunk unload/relog.

`MULTIPLAYER TESTED` remains NO until real-client verification.

---

# 19. Performance constraints

- no global search for broken lifts/POIs every tick;
- route state is explicit saved data + local loaded-chunk presentation;
- Griffin tracking/flight logic only evaluates the active encounter area;
- mine/observatory effects do not run when the area is unloaded;
- high-altitude ambience uses bounded entity/effect density;
- resource nodes remain region/chunk/event driven;
- decorative ropes/props use efficient static/block/model composition rather than large always-ticking entity populations.

---

# 20. Acceptance before R03 can be called asset-ready

Required external intake:

1. Griffin exact artifact + license/attribution + SHA-256 + animation list;
2. Rock Golem exact artifact + chosen license/attribution + SHA-256 + conversion test;
3. one accepted R03 cave-threat source to replace the unresolved Rocky Roller role;
4. exact Silver ore/node world presentation from KayKit or accepted coherent source;
5. highland-crystal visual source or explicit deletion of that material role;
6. mining-town architecture/props and lift/bridge final presentation;
7. ancient observatory/forge machinery source language;
8. R03 NPC outfit bindings;
9. dungeon/field-boss VFX and sound sources;
10. R03 fish exact model/species mapping.

Gameplay acceptance later requires:

- traversal feels vertical but not laborious;
- reopening shortcuts is clearly useful;
- no mandatory climb repeatedly exceeds the player's tolerance without discovery/action;
- Refined Pick/Silver progression is understandable and non-circular;
- Griffin melee uptime is healthy;
- Rock Golem lands inside the 150–210 s later-dungeon target without becoming a defense sponge;
- regional story can be understood without reading optional lore text;
- observatory revelation clearly establishes continental network geometry;
- R03 remains enjoyable even for a player who already completed R02.

---

# 21. What this pass changes relative to old REGIONS.md

Refined/closed:

- Basalt Wyvern **removed from R03** and reserved as a better R10 volcanic candidate;
- dedicated animated Griffin candidate introduced for R03 field-boss role;
- Rock Golem external candidate introduced for the previously-open dungeon boss slot;
- exact regional human problem and Act-I story function added;
- three persistent traversal shortcuts added;
- POI package and content-density targets added;
- Silver / Refined Pick progression connected without a circular recipe;
- fishing and housing kept present but deliberately not allowed to dominate every region;
- regional aftermath/state memory added;
- unresolved Rocky Roller candidate explicitly demoted until its external source is verified.

The next region package must preserve this combined **people + place + system + story + encounter + aftermath** standard.