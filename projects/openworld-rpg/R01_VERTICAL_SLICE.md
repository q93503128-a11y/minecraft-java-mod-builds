# Open-World RPG — R01 Vertical Slice / Opening 60–90 Minute Canon

> Status: **DESIGN CANON — opening settlement, named cast, first-session quest/scene flow, first mount, first dungeon, state/recovery behavior and player-facing text locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Region: `REGIONS.md`  
> Story: `WORLD_STORY_CANON.md`  
> Quest/state: `QUEST_WORLD_STATE.md`  
> Combat: `COMBAT_BALANCE.md`, `STATUS_AND_R01_ENCOUNTERS.md`  
> Classes: `CLASS_COMBAT_KITS.md`, `CLASS_PROGRESSION.md`  
> Equipment/economy: `LOOT_ECONOMY.md`, `EQUIPMENT_BALANCE.md`  
> Recovery/food: `RECOVERY_PRODUCTION_APPEARANCE.md`  
> Mounts: `MOUNTS.md`  
> UI: `UI_DIRECTION.md`  
> External provenance: `EXTERNAL_SOURCES.md`, `R01_ASSET_INTAKE.md`  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins.

This file turns the already-designed R01 systems into one implementation-ready first-session content package. The objective is that an implementer does **not** invent the opening route, settlement/service order, named cast, quest conditions, dialogue beats, reward flow, failure recovery, first mount timing, first dungeon sequence or R01-to-Act-I handoff while coding.

The target is **not a long tutorial**. The player learns the game by moving through a real region, meeting a small recurring cast, using useful services, choosing a class, following one grounded local problem, finding optional content, unlocking the first mount and clearing the first dungeon.

---

# 0. Player-facing text / development-language contract

Internal development terminology may exist in source, data, tests and logs. It must never leak into the finished player-facing game.

Forbidden in normal player-facing UI, dialogue, item text, quest text, map text, loading text, tutorial prompts and system messages:

- `P0`, `P1`, milestone labels or production-priority tags;
- `alpha`, `beta`, `prototype`, `temporary`, `placeholder`, `TODO`, `debug`, `developer`;
- implementation class/registry/data IDs such as `r01_main_stage`, `quest.r01.*`, `todo_asset` or internal asset-intake labels;
- asset-license/provenance notes;
- test instructions or acceptance-check wording;
- raw exception, missing-localization or internal enum text.

Player-facing text must use only world/game language. Examples:

- internal `r01_main_stage=QUARRY_DISCOVERED` → player sees **Roots Below Stone** and its current objective;
- internal `first_root_class_selected=false` → player sees the class-selection interaction at the Wayfarers' Hall;
- internal asset-binding failure is a build/content error, not a message saying `missing model` to the player.

A player-visible build with unresolved localization keys or internal identifiers is rejected rather than treated as acceptable temporary presentation.

---

# 1. External precedents used

## 1.1 Guild Wars 2 — exploration without NPC-chore chains

Useful precedent:

- after a brief tutorial, the player is placed in a real low-level explorable area;
- starting service areas expose useful crafting/storage/merchant functions;
- local activity can progress through useful actions without requiring a dialogue click before every action;
- POIs, events and world challenges encourage self-directed movement.

Project adoption:

- the opening settlement provides services quickly;
- the first main objective gives direction but local activity can progress through several useful action types;
- optional contracts and discoveries are visible without becoming a wall of quest markers;
- not every event starts with an NPC interaction.

Not adopted:

- no map-completion checklist requiring every icon;
- no permanent MMO-style task panel over every local area.

References:
- `https://wiki.guildwars2.com/wiki/Starting_area`
- `https://wiki.guildwars2.com/wiki/Renown_Heart`
- `https://www.guildwars2.com/en/news/explore-the-world-of-guild-wars-2/`

## 1.2 Elden Ring — visible optional danger

Useful precedent:

- an open-world field boss can be encountered early, escaped from and returned to later;
- difficult optional encounters communicate that the world is not a mandatory linear corridor.

Project adoption:

- Regalhart is discoverable during the opening route but never gates the quarry dungeon;
- hunt clues create a broad search region rather than an immediate exact GPS pin;
- the player may leave, return later or defeat it early.

Not adopted:

- R01 does not repeatedly kill a new player for failing to understand a boss placed directly on a mandatory road.

---

# 2. External motion is a production requirement

External-first applies to **animation/motion itself**, not only to character models or VFX.

The following player-visible actions require a selected external animation or external motion reference before gameplay source implementation of that action:

- idle / locomotion transitions;
- walk / jog / sprint;
- dash;
- dodge / roll;
- jump / fall / landing;
- crouch / crawl where used;
- guard / perfect-guard / parry reactions;
- weapon attack chains;
- cast / channel / heal gestures;
- hit reactions / knockdowns / get-up;
- interaction / pickup;
- mining / chopping / smithing / fishing / other visible work actions;
- revive / helping another player;
- mount / dismount;
- important class movement skills.

**Forbidden production shortcut:** moving the player several blocks with code while leaving a vanilla run pose, static body or improvised two-keyframe animation and calling that a dash.

## 2.1 Primary humanoid motion sources

### KayKit Character Animations

Primary dodge/dash/work-action source family.

Current direction:

- CC0;
- broad humanoid movement/combat/work coverage;
- legacy/free releases explicitly expose `Roll`, `Dash Front`, `Dash Back`, `Dash Right`, `Dash Left`;
- current FBX/GLTF clips are retargetable candidates.

Selection order:

1. inspect current KayKit pack for the best matching clip;
2. if exact directional coverage is better in the legal CC0 legacy pack, use that clip;
3. retarget/retime only as required for Minecraft proportions and canonical duration;
4. reject a clip if body/feet cannot visually agree with real server movement.

Sources:
- `https://kaylousberg.itch.io/kaykit-character-animations`
- `https://kaylousberg.itch.io/kaykit-animations`

### Quaternius Universal Animation Library / Library 2

Use when locomotion/combat/parkour motion reads better than the KayKit alternative.

Sources:
- `https://quaternius.com/packs/universalanimationlibrary.html`
- `https://quaternius.com/packs/universalanimationlibrary2.html`

## 2.2 Server-authoritative movement versus animation root motion

The server owns real position, collision, Stamina cost and i-frame state.

Canonical universal dodge:

```text
duration: 0.45 s
movement target: ~3.2 blocks
i-frame: 0.30 s
Stamina cost: 30
```

External root motion may define the intended visual motion curve, but client root motion never owns gameplay displacement.

Acceptance:

- server movement and animation foot/body motion agree visually;
- startup, i-frame body movement and recovery agree within roughly one server tick where practical;
- collision-shortened dodges resolve without obvious wall penetration or prolonged foot sliding;
- if a clip cannot be reconciled, replace the clip rather than distorting the canonical hitbox/movement solely to save the asset.

The same rule applies to lunges, visible teleports, mount charges and knockback reactions.

---

# 3. R01 visual source family

## 3.1 Starting settlement architecture

Primary architecture/design source:

- Quaternius `Medieval Village MegaKit` family;
- use only the exact package/artifact whose source/license evidence is accepted in `R01_ASSET_INTAKE.md`;
- its roof language, wall rhythm, timber/stone balance and modular composition define the starting settlement.

Final buildings are translated into Minecraft-compatible block architecture/interiors when that improves collision/navigation/world integration. Model/display elements may provide signs, awnings, trims and props.

Secondary fallback/reference:

- Kenney `Fantasy Town Kit`, CC0, only where a missing concept cannot be solved cleanly inside the primary family and the result still harmonizes visually.

## 3.2 Props / services

Primary prop family:

- accepted Quaternius `Fantasy Props MegaKit` artifact/source;
- forge, market, guild, inn, bank/storage, potion/alchemy, quest board, carts/crates/barrels and compatible dungeon props.

## 3.3 NPC source family

Primary body/outfit direction:

- Quaternius `Universal Base Characters`;
- Quaternius `Modular Character Outfits - Fantasy`;
- exact accepted package/source/license follows `R01_ASSET_INTAKE.md`.

No vanilla villagers or unrelated flat skins are final NPC presentation.

Exact model-part bindings are a **pre-code asset-intake gate**, not an implementation decision. Names, roles and scene functions below are already locked and do not change merely because a different accepted modular part fits better.

---

# 4. Starting settlement — Alderford

The R01 starting settlement's final player-facing name is **Alderford**.

Alderford is a working river-road settlement that exists because quarry traffic, farms, travelers and river trade meet at a dependable ford. It remains useful long after the opening and must not read as a disposable tutorial village.

## 4.1 Physical topology

Exact world coordinates wait for the Azari spatial-closure pass, but the accepted placement must preserve this topology:

```text
Approach road
  ↓
Alderford Gate + first shrine
  ↓ 45–60 blocks
Market Square / The Copper Kettle
  ├─ Wayfarers' Hall
  ├─ Holt Forge
  ├─ Alderford Vault
  ├─ Greenwater Remedies
  ├─ Fordside Stables + visible Trail Stag paddock
  ├─ Guild board / route board
  └─ road exits / housing cluster
```

Spatial requirements:

- central square roughly 25–35 blocks across;
- main service doors normally 15–35 blocks from square center;
- stable visible from arrival/plaza route but offset toward the outward road;
- shrine visible immediately after gate reveal;
- forge chimney/anvil silhouette, Wayfarers' banner, inn sign, stable paddock and shrine silhouette provide physical wayfinding;
- at least two starter-home exteriors are visible from normal settlement circulation;
- no compulsory service-tour quest.

## 4.2 Functional buildings at first visit

1. Alderford Gate/watch structure;
2. gate shrine/checkpoint;
3. **The Copper Kettle** inn/tavern and cooking service;
4. market/basic merchant stall;
5. **Wayfarers' Hall** — guild/class facility;
6. **Holt Forge** — smith/forge;
7. **Alderford Vault** — personal storage / Material Vault access;
8. **Greenwater Remedies** — healer/alchemy;
9. **Fordside Stables**;
10. small housing cluster.

Buildings exist from the beginning. Progress unlocks service depth, recipes, dialogue and world context rather than magically spawning the buildings.

---

# 5. Named Alderford cast

These names and functions are final player-facing R01 canon. Asset intake may alter exact face/hair/outfit pieces only; it does not rename/rewrite the character during implementation.

| Character | Role | Normal Alderford anchor | R01 narrative/gameplay function |
|---|---|---|---|
| **Mara Venn** | Guild Pathfinder | gate/Wayfarers' Hall | primary R01 main-story guide; practical route/safety perspective; recurring early-region character |
| **Elian Rook** | Guild Steward / class trainer | Wayfarers' Hall | first root-class selection, class-service explanation |
| **Daren Holt** | Smith / engineer | Holt Forge | forge/service; recurring Engineer/Smith story role; practical machinery interpretation |
| **Lysa Fen** | healer / alchemist | Greenwater Remedies | recovery/alchemy introduction; `Riverbank Remedies` |
| **Toma Reed** | stable keeper | Fordside Stables | Trail Stag introduction and registration |
| **Brin Hale** | innkeeper / cook | The Copper Kettle | rest, food/cooking service, grounded settlement life |
| **Nessa Bell** | market merchant | market square | early consumable/material buy/sell service |
| **Oren Quill** | vault keeper | Alderford Vault | storage / Material Vault explanation on first use only |
| **Sera Wren** | cartographer / ranger | route board / west edge | recurring Cartographer/Ranger role; `Signs in the Meadow`; route/ecology perspective |
| **Ilyan Voss** | Anchor scholar | Copper Kettle guest table before clear; Wayfarers' Hall after clear | recurring Anchor Scholar; interprets quarry evidence without knowing everything in advance |
| **Kest Arden** | rival wanderer | optional field appearances | recurring Rival Wanderer; optional R01 hunt encounter; no companion AI |

Daytime target remains **8–12 functional/service/guard NPCs + 4–8 ambient townsfolk**. The named roster does not imply all eleven characters pathfind simultaneously around the square.

Behavior rules:

- service NPCs use short local anchor zones;
- ambient townsfolk use bounded patrol/idle routes;
- visible work uses accepted external animations/props;
- normal services stay usable regardless of optional ambient schedules;
- no vanilla villagers are final population.

## 5.1 First-visit ambient character presentation

The player does not have to talk to everyone.

- Mara is encountered naturally near the gate/Wayfarers' route after the opening-road disturbance.
- Elian is the only mandatory settlement interaction before normal class-based progression.
- Ilyan is physically present at The Copper Kettle before the quarry clear, but his early dialogue does not explain Anchors or spoil the mystery.
- Daren, Lysa, Toma, Brin, Nessa, Oren and Sera are usable immediately in their roles.
- Kest is not required for R01 completion.

---

# 6. Opening loadout before class selection

Starting equipment/state:

```text
Heartland Arming Sword — Standard, Item Lv1
Watch Buckler — Standard, Item Lv1
Healing Potion x1 — loaded in the Recovery Belt
Recovery Belt loaded capacity — 4 total slots
starting Gold — 150
```

The starting Healing Potion uses the canonical recovery rule in `RECOVERY_PRODUCTION_APPEARANCE.md`:

```text
35% MaxHP
0.95 s drink action
0.72 s resolution point
6.0 s shared Recovery lockout after resolution
```

Available before class selection:

- basic weapon attack;
- sprint;
- universal dodge;
- guard / perfect guard with Watch Buckler;
- interact;
- Recovery Belt use.

Unavailable until first root-class selection:

- root-class mechanic;
- four root-class active skills;
- root-class ultimate.

## 6.1 First root-class starter grant

First root-class selection is free.

| Root class | Canonical first-selection grant |
|---|---|
| Warrior | no duplicate weapon; keep Heartland Arming Sword + Watch Buckler |
| Hunter | Riverwood Bow |
| Cleric | Initiate Staff |
| Mage | Initiate Wand |
| Guardian | no duplicate weapon; keep Heartland Arming Sword + Watch Buckler |

Every granted class starter item uses:

```text
starter_bound: true
sell_value: 0
tradeable: false
dismantle_yield: 0
```

It may be destroyed only through the normal deliberate item-discard confirmation once the player no longer wants it. Changing class never grants another sellable copy.

This is a usability grant, not a class weapon lock.

---

# 7. Opening timeline and authored content

Normal first play should usually reach Earthloong first clear in roughly **55–75 minutes**. Optional exploration, Regalhart, gathering, housing browsing or experimentation can extend the first session toward 90+ minutes.

This is a pacing target, not a mission timer.

## Phase A — 0:00–0:04 — approach road / first reveal

Sequence:

1. player begins on the Alderford approach road as an independent traveler;
2. Louxia/gazelle-style ecology is visible before combat;
3. one authored Meadow Viper threatens the road;
4. the first committed bite may trigger one compact contextual **Dodge** prompt;
5. player may defeat, guard, dodge or retreat from the Viper;
6. the ridge/road bend reveals Alderford;
7. the gate shrine activates on legitimate approach interaction.

Pre-shrine death remains economically free under existing death canon.

No forced `press W`, `press space`, `talk to three NPCs` corridor.

### Mara's gate line

After the player reaches the gate following the road disturbance, Mara may deliver this short world line without locking the camera:

> “You picked a lively road to arrive on. If you're looking for work, the Wayfarers' Hall is ahead. Quarry carts have stopped coming back on time.”

This line is skippable and is not itself a quest-completion requirement.

## Phase B — ~0:04–0:10 — Alderford / first class

On first arrival:

- shrine is usable;
- all baseline settlement services are physically present;
- Wayfarers' Hall is clearly reachable from the square;
- the guild board displays `Dust on the Quarry Road`, `Riverbank Remedies`, `Signs in the Meadow`;
- the main entry is visibly distinguished from optional contracts;
- first class selection is free.

### Elian Rook — first class interaction

Player-facing opening line:

> “Choose the discipline you want to begin with. You can learn another path later; this is where you start.”

Class selection uses the canonical class UI and immediately grants the root mechanic, four actives and ultimate defined in `CLASS_COMBAT_KITS.md`.

After the server commits first class selection and starter grant, `Dust on the Quarry Road` becomes the current main objective. The player may leave Alderford immediately; no other service interaction is mandatory.

## Phase C — ~0:10–0:25 — Dust on the Quarry Road

Final player-facing quest title: **Dust on the Quarry Road**.

Quest giver/owner: **Mara Venn**.  
Category: Main.  
Repeatability: once per player.  
Failure: cannot permanently fail.

Mara's offer line:

> “The old quarry road should be dull work. Today it isn't. Check the wrecks, help anyone still out there, and bring back enough of the picture that we know what we're dealing with.”

### Completion model

The road area contains five authored **distinct evidence/action categories**:

1. recover the lost cargo bundle from the overturned road cart;
2. defeat or meaningfully participate against the authored Meadow Viper road threat;
3. inspect the broken quarry-road marker;
4. gather one valid nearby R01 field resource from Iron Ore / Hardwood / Healing Herb;
5. meaningfully participate in the small road-assistance event if it is active.

The quest completes after **any 3 distinct categories** are credited.

Rules:

- repeating one category cannot supply multiple required credits;
- if the dynamic road-assistance event is inactive/unavailable, the other four categories still make the quest completable;
- cargo/marker interactions are personal logical state even when the world prop is shared;
- combat/support participation follows `QUEST_WORLD_STATE.md`;
- no item must be physically carried back to Mara after the three-category requirement is complete.

Reward on server-authoritative completion:

```text
EXP: 35% of current next-Lv requirement
Gold: 90
Class XP: 25% of current Class Rank requirement
```

Mara's completion line:

> “That's more than bad luck. Traffic and wildlife are both being pushed off their usual lines. The quarry crew marked roots in the lower workings before they pulled out.”

Completion unlocks:

- `Roots Below Stone` as the next main objective;
- Toma Reed's explicit `A Stag at the Ford` hint if the player has not already discovered the event;
- `Steel in the Grass` remains discovery-gated rather than appearing automatically.

### Reconnect / defeat behavior

- distinct action-category credits persist immediately as personal state;
- defeat does not reset credited categories;
- disconnect does not respawn/duplicate the personal cargo reward state;
- if an authored enemy despawns, alternative categories remain sufficient.

---

# 8. First optional contracts

## 8.1 Riverbank Remedies

Quest giver: **Lysa Fen**.  
Category: Contract.  
Availability: first Alderford arrival.  
Repeatability: once per player.

Board text:

> “Greenwater Remedies needs three fresh Healing Herbs from the river edge. Bring them to Lysa Fen.”

Lysa's accept line:

> “Three fresh river herbs will do. Don't strip a whole patch; take what you need and leave the bank alive.”

Objective:

```text
gather 3 Healing Herb from the player's personal valid R01 herb nodes
return to Lysa Fen
```

Trade-acquired herbs do not satisfy this first teaching contract; the objective specifically demonstrates gathering. The herbs are consumed on turn-in.

Reward:

```text
EXP: 20% of current next-Lv requirement
Gold: 60
Class XP: 15% of current Class Rank requirement
Healing Potion x1
```

Lysa's completion line:

> “Good. That's enough for the road and enough left for the next traveler. Keep a dose ready before you go underground.”

Alchemy itself is already mechanically available when the player owns ingredients; this quest does **not** gate the service.

## 8.2 Signs in the Meadow

Quest giver: **Sera Wren**.  
Category: Contract / discovery teaching.  
Availability: first Alderford arrival.  
Repeatability: once per player.

Sera's accept line:

> “Big tracks don't mean ‘go kill the biggest thing nearby.’ Learn what made them first. Check the damaged cart and the meadow edge.”

Authored sites:

1. damaged cart with broad impact/hoof evidence;
2. churned meadow edge near a territorial route;
3. broken fence/tree scoring closer to the grove boundary.

Completion:

```text
inspect any 2 of the 3 sites
```

This contract teaches territorial ecology and foreshadows Steelboar/Regalhart. It never requires an elite or boss kill.

Reward:

```text
EXP: 20% of current next-Lv requirement
Gold: 70
Class XP: 15% of current Class Rank requirement
```

Sera's completion line:

> “Steelboar made part of it. The deeper scoring didn't. If you follow the larger trail, do it because you chose to—not because a board told you to.”

---

# 9. Service-depth triggers — exact R01 behavior

Services are useful without errand quests.

## 9.1 Forge

When the player first possesses relevant materials, Holt Forge shows currently legal recipes automatically:

- Iron Ore / Hardwood → baseline R01 forge options defined in `EQUIPMENT_BALANCE.md`;
- Verdant Crystal → first Superior R01 recipe options defined in the equipment canon;
- Earthloong Scale → signature-craft progress after first clear.

Daren does not require a `teach me what iron is` quest.

First-use line:

> “If it's in your Material Pouch, I can work from it. Bring better material and you'll see better options.”

## 9.2 Alchemy

Canonical R01 recipes are already final in `RECOVERY_PRODUCTION_APPEARANCE.md`:

```text
Healing Potion: 2 Healing Herb + 5 Gold
Focus Draught: 1 Healing Herb + 1 Louxia Glow + 8 Gold
Cleansing Tonic: 1 Healing Herb + 1 Louxia Glow + 10 Gold
```

## 9.3 Cooking

Canonical R01 meals are already final:

```text
Herbed Louxia Roast: 2 Louxia Meat + 1 Healing Herb → MaxHP +6% / 20 min
Trail Skewers: 1 Louxia Meat + 1 Healing Herb → Stamina recovery +10% / 20 min
Glow Broth: 1 Louxia Meat + 1 Louxia Glow + 1 Healing Herb → Mana recovery +10% / 20 min
```

There is no unresolved `finalize consumable data later` dependency in the R01 content flow.

---

# 10. A Stag at the Ford

Final player-facing title: **A Stag at the Ford**.  
Quest/event owner: **Toma Reed**.  
Category: Regional discovery/event.  
Target local Lv: 3–4.  
Normal timing: 25–40 minutes.  
Repeatability: unlock/reward once per player; physical rescue encounter may reset for incomplete players.

Availability:

- Fordside Stables and Toma are visible from first arrival;
- after `Dust on the Quarry Road`, Toma's direct hint becomes available;
- discovering the ford event first starts it organically without requiring the hint.

Toma's hint line:

> “One of my stags tore loose by the ford. If you find it, don't chase it. Clear the danger, then let it come to you.”

Event sequence:

1. enter the authored ford event volume;
2. locate the frightened Trail Stag near the damaged harness/cart route;
3. clear the event hazard pack — solo baseline **2 Meadow Vipers**, adding **+1 Viper-equivalent threat per additional active participant up to 4 total threats**;
4. perform the accepted calming interaction animation at close range after combat state ends;
5. mount the Stag;
6. ride the short authored ford-to-stable road segment;
7. enter the stable registration volume while mounted or leading the Stag after a legal dismount;
8. permanent Trail Stag unlock commits per eligible player.

This is not an escort quest. The player rides the animal; no slow walking AI follows the player.

Reward:

```text
Trail Stag permanent unlock
Gold: 60
EXP: 25% of current next-Lv requirement
Class XP: 20% of current Class Rank requirement
```

Toma's completion line:

> “There. It knows your hands now. Call it when the road is open enough to ride; don't ask it to fight your battles.”

Movement remains `MOUNTS.md`:

```text
cruise: 6.4 b/s
forgiving steering
no mount Stamina drain
no mount attack
```

External model direction: accepted Quaternius Stag candidate from the appropriate animated-animal source package, exact artifact/animation binding required by the pre-code asset gate.

### Failure / reconnect

- player defeat before registration resets only that player's incomplete logical step;
- event-owned Stag/hazard state may reset after 60 seconds with no active incomplete participant in the event volume;
- disconnect before registration resumes at `FORD_DISCOVERED` or `STAG_CALMED` only if the corresponding server commit occurred;
- permanent unlock/reward is atomic and cannot be farmed by replaying the physical event.

---

# 11. Roots Below Stone

Final player-facing title: **Roots Below Stone**.  
Quest owner: **Mara Venn**.  
Category: Main.  
Availability: completion of `Dust on the Quarry Road`.  
Failure: cannot permanently fail.

Mara's start line:

> “The quarry crew marked roots where there shouldn't be roots. Get eyes on the lower workings. If the entrance is open, don't assume miners opened it.”

Exact pre-dungeon stages:

1. reach the old quarry overlook discovery volume;
2. descend to the lower-workings approach by any legal route;
3. inspect the root-split masonry at the lower entrance;
4. the player receives the exact dungeon marker and **Suggested Lv 8** warning;
5. entering the quarry dungeon advances the main objective to `Reach the root-breached workings`.

Milestone reward after personal inspection of the lower entrance:

```text
EXP: 40% of current next-Lv requirement
Gold: 120
Class XP: 25% of current Class Rank requirement
```

No hard Lv8 gate exists. A skilled under-level player may enter.

The first Cave Centipede and visible ore/cave transition provide the intended encounter/readability teaching without requiring an arbitrary kill count.

---

# 12. Optional R01 major-threat content

## 12.1 Steel in the Grass

Final title: **Steel in the Grass**.  
Quest owner: **Sera Wren**.  
Availability: only after the player personally identifies a Steelboar or clear authored Steelboar territorial evidence.  
Completion: meaningfully participate in defeating one qualifying Steelboar after contract activation.  
Not required for quarry access.

Sera's offer line:

> “That plated boar has started pushing closer to the road. If you choose to hunt it, keep clear of the first charge. The armor matters less once it commits.”

Reward beyond normal elite reward:

```text
EXP: 30% of current next-Lv requirement
Gold: 100
Class XP: 15% of current Class Rank requirement
```

## 12.2 The Crowned Trail — Regalhart discovery

Final journal/discovery title: **The Crowned Trail**.

Regalhart never receives an exact first-arrival boss pin.

Three authored clue types across meadow/deep-grove boundary:

1. antler-height scoring on a large tree/wooden structure;
2. unusually deep hoof furrows and trampled vegetation;
3. a broken hunter/road marker with crown-shaped antler damage.

Discovering any **2 of 3**:

- adds a broad search region to the map;
- adds `The Crowned Trail` hunt/discovery entry;
- does not spawn Regalhart artificially if the encounter controller is already valid;
- does not gate fighting Regalhart if the player finds it first.

Finding/fighting Regalhart first immediately records boss discovery and preserves remaining clue interactions as optional world context; it does not retroactively force clue collection.

### Kest Arden optional first appearance

If Kest is present for the player's first broad-search entry or first Regalhart aftermath, use one short scene only. He is another competent wanderer, not a quest dispenser.

First-search line:

> “If you're following the crown marks, you're late by about an hour. Good news: it didn't stay where I found them.”

If the player defeats Regalhart before meeting Kest, his later R01 line changes to:

> “So you're the one who brought down the crowned hart. Saves me a long walk.”

No reward/progression depends on meeting Kest in R01.

---

# 13. First quarry dungeon

Target first-clear duration: **15–25 minutes**.

The quarry is one physical authored Azari/R01 location, not a random maze.

## 13.1 Architecture direction

Primary external design/reference bases:

- accepted Quaternius `Modular Dungeon Pack` source;
- accepted Quaternius `Ultimate Modular Ruins Pack` source;
- accepted Quaternius `Fantasy Props MegaKit` source.

DeCubed Dungeons may remain a 26.2 layout/reference/local-only source under its actual terms; vanilla spawners/loot/style are not retained as canonical R01 content.

Visual language:

```text
ordinary quarry
→ abandoned lower workings
→ root intrusion
→ old worked masonry that clearly predates the quarry
→ Earthloong chamber / buried Anchor-era relay evidence
```

Do not stitch unrelated downloaded rooms together.

## 13.2 Room / encounter sequence

### 1. Upper Mining Gallery

- abandoned quarry identity;
- Iron Ore / mining props;
- one short Cave Centipede/small-threat group;
- one visible optional side ledge/cache;
- active combat target 45–75 seconds.

### 2. Collapsed Hoist Chamber

- compact traversal around broken platforms/hoist;
- vertical centipede pressure;
- nearby mechanism opens a persistent-in-run lift shortcut toward entrance;
- no required Hardwood/material sacrifice;
- room target 2–4 minutes.

### 3. Root-Breached Workings

- visual transition into fantasy ecology and old masonry;
- one Nature Spirit elite using canonical Living Shell/melee identity;
- Verdant Crystal/herb side cache;
- no repeated copies of the same elite;
- active combat target 15–25 seconds, room total 2–4 minutes.

### 4. Relay Gallery / boss antechamber

This replaces an empty generic breathing room with a small but important environmental-story beat.

Visible features:

- quarry supports stop and much older fitted stone begins;
- a damaged relief/plate shows several lines radiating beyond Alderford;
- no readable exposition paragraph is placed on a wall;
- personal interaction records **Quarry Relay Evidence** as journal/key-state, not as a normal inventory item or currency;
- opened lift shortcut remains available for the current dungeon cycle;
- no permanent shrine/fast-travel node inside the dungeon.

Player-facing investigation text:

> “The stonework predates the quarry. Repeating route lines continue beyond Alderford—west through the forest and upward toward the mountains.”

### 5. Earthloong Chamber

Use `STATUS_AND_R01_ENCOUNTERS.md` exactly:

```text
Lv 8
4,900 HP solo baseline
~120–150 s active-combat target
physical phase + lightning space-control phase
no long untargetable state
only authored r01_earthloong_breakable_prop blocks break
visible lightning lane/decal range = server hit area
```

## 13.3 First-clear rewards

Canonical first-clear package:

- Earthloong Scale x2;
- guaranteed Superior+ normal boss gear;
- 15% direct Mythic roll from `Rootquake Maul / Earthscale Ward` pool;
- deterministic completion choice: `Ironroot Longsword / Riverthorn Bow / Lumenwood Staff`;
- completion EXP: 50% current next-Lv requirement + boss contribution;
- completion Class XP: 32% + boss contribution;
- Gold: 180.

Reward choice uses accepted Lucifer-family reward UI and real accepted item previews, not a vanilla chest GUI.

After first clear, Holt Forge shows Earthloong Signature Craft progress at `2 / 4 Earthloong Scales`; no immediate repeat clear is forced.

---

# 14. Earthloong aftermath / Act-I handoff

Earthloong's defeat must complete the R01 story function instead of ending as a loot screen.

## 14.1 Immediate chamber aftermath

After encounter completion and first-clear reward transaction:

1. combat music resolves;
2. the damaged relay stone/plate becomes safely interactable;
3. if `Quarry Relay Evidence` was missed in the antechamber, this interaction records it here so the main story cannot softlock;
4. the journal records that the root breach exposed an older networked structure beneath the quarry;
5. the current main objective becomes **Lines Beneath the Land — Return to Alderford**.

No mandatory long cutscene occurs in the dungeon.

## 14.2 Return scene — Wayfarers' Hall

The return is justified because several people physically interpret the evidence and establish the next open-world leads.

Required characters:

- Mara Venn;
- Ilyan Voss;
- Daren Holt.

The scene begins when the player enters the Wayfarers' Hall briefing interaction after Earthloong first clear. It is skippable line-by-line and may be skipped as a whole after the first line; skipping commits the same progression state and never removes information from the journal.

Canonical dialogue:

**Mara Venn**
> “You found the quarry problem. I was hoping it would stop at roots and bad stone.”

**Ilyan Voss**
> “It doesn't. This mark isn't a shrine seal; it's a route notation. These lines point west and uphill—forest relays and a mountain station.”

**Daren Holt**
> “And if a dead quarry is still tied to them, I want to know what happens before anyone wakes the rest.”

**Mara Venn**
> “Then we don't wake anything blind. Follow either line. See what's still connected, and keep the roads open while we learn.”

The player's journal then receives two peer leads:

- **Western Relay** — points toward R02's forest/river-basin investigation;
- **Whitecrest Station** — points toward R03's observatory/forge investigation.

Final main entry after the scene:

**Lines Beneath the Land**

Objective text:

> “Follow one of the old route lines beyond Alderford. The western relay and Whitecrest station may explain what the quarry was connected to.”

Act-I progression later requires at least one qualifying R02/R03 major lead as defined in `WORLD_STORY_CANON.md`; both remain playable.

## 14.3 Ilyan before the quarry clear

If the player talks to Ilyan at The Copper Kettle before the first dungeon clear, use only:

> “I'm here for old road records. Alderford keeps better ledgers than most places twice its size.”

He does not use the word `Anchor`, identify the quarry facility or reveal the continental network before evidence exists.

---

# 15. Quest-board density and marker rules

First arrival board state:

```text
Main: Dust on the Quarry Road
Optional: Riverbank Remedies
Optional: Signs in the Meadow
```

During first session:

- normal free-roam HUD shows at most 1 main + 2 manually pinned optional objectives;
- no more than about four unresolved R01 board/guild hooks are foregrounded by default;
- `A Stag at the Ford`, `Steel in the Grass`, `The Crowned Trail` arise from progress/discovery rather than all appearing at time zero;
- dungeon entrance gets an exact map marker only after personal lower-entrance inspection;
- optional boss exploration uses broad search areas where appropriate;
- main objectives provide guidance without turning R01 into an icon-clearing checklist.

---

# 16. Early economy check

Starting liquid balance: **150 Gold**.

Fixed authored R01 objective payments:

| Objective | Gold |
|---|---:|
| Dust on the Quarry Road | 90 |
| Riverbank Remedies | 60 |
| Signs in the Meadow | 70 |
| A Stag at the Ford | 60 |
| Roots Below Stone investigation milestone | 120 |
| Steel in the Grass | 100 |
| Earthloong first dungeon completion | 180 |

Completing every listed optional objective yields **680 Gold** in fixed objective payments plus ordinary loot/material/combat income. A direct player earns less.

The first Trail Stag is free after its event. The 2,400-Gold starter house remains a real multi-hour savings target rather than first-session free property.

---

# 17. Audio source direction

No important R01 sound defaults to `whatever vanilla sound is closest`.

Primary redistributable source families:

- Kenney RPG Audio;
- Kenney Impact Sounds;
- Kenney UI Audio / Interface Sounds;
- selected exact CC0 Freesound/OpenGameArt ambience with provenance recorded.

Current ambience direction:

- Alderford: restrained people/work/forge layers;
- meadow: wind + sparse wildlife;
- riverwood: water + bird/leaf ambience;
- quarry: stone/wood creak/low cave air;
- root sections: subtle tonal layer, not generic horror drone;
- Earthloong: dedicated body impact, stone/root, electrical charge/strike layers.

Every accepted production sound gets exact source/license/filename/hash in bindings before the source-bootstrap gate opens for the relevant content.

---

# 18. VFX / signage / interaction presentation

- guild board is a physical coherent prop/structure with restrained interaction highlight;
- forge uses accepted furnace/anvil/tool props and smith motion;
- stable uses paddock/tack/feeding props;
- shrine uses a distinctive external-source silhouette/VFX family;
- Trail Stag unlock uses real Stag/tack/registration presentation, not a text-only toast;
- resource nodes use accepted external models;
- Earthloong telegraphs match real server hit areas;
- important rewards display real equipment previews;
- Lucifer-family UI remains the overlay language for dialogue/reward/inventory/service screens.

No player-facing R01 screen uses a temporary black developer panel while waiting for later art.

---

# 19. Multiplayer behavior

Progression is per player unless explicitly world-shared.

## 19.1 Settlement / services

- shrine discovery, class selection, inventory/bank state, Trail Stag unlock, quest completion and first-clear rewards are personal/server-authoritative;
- buildings physically exist for everyone;
- one player's class/story state does not overwrite another's.

## 19.2 Field objectives

- natural enemies/events are shared physical entities;
- eligible participants receive personal rewards/progress;
- personal gathering remains personal;
- investigation sites record personal logical interaction even when the physical prop is shared.

## 19.3 Trail Stag

- nearby eligible incomplete players may resolve the rescue together;
- each receives their own permanent unlock exactly once;
- a player who missed it may later trigger the incomplete-player version;
- completed players cannot farm repeat Gold/EXP/unlock rewards.

## 19.4 Quarry dungeon

- physical authored dungeon, not mandatory per-party copies at launch;
- encounter controller owns current run state;
- boss scales by canonical participant HP/poise rules;
- outgoing boss damage does not scale upward with player count;
- first-clear/completion/signature loot is personal;
- shortcut is encounter-cycle state;
- late joiners must meet normal contribution eligibility;
- reward transaction is idempotent.

## 19.5 Disconnect-critical transactions

- if Earthloong is defeated but a player's deterministic reward choice is not committed before disconnect, store `reward_choice_pending=true`; reopen the exact choice on reconnect before granting another copy;
- if the post-quarry briefing is interrupted, reopen from the first uncommitted presentation beat or allow whole-scene skip; story progression commits only once;
- quest reward claim and story-stage transitions use separate idempotent transaction keys.

---

# 20. Data / state contract

Canonical data ownership layout may use equivalent final paths, but these logical records must exist as data-driven content rather than hard-coded quest branches:

```text
opening/
  starting_loadout
  settlement_services
  r01_opening_flow

quests/r01/
  dust_on_quarry_road
  riverbank_remedies
  signs_in_meadow
  stag_at_ford
  roots_below_stone
  steel_in_grass
  crowned_trail
  lines_beneath_land

dungeons/r01_quarry/
  rooms
  encounters
  shortcut
  relay_evidence
  completion_rewards

presentation/
  motion_bindings
  npc_bindings
  settlement_bindings
  dialogue_r01
  audio_bindings
```

## 20.1 R01 main-stage enum

Internal only; never player-facing:

```text
ARRIVAL_ROAD
ALDERFORD_REACHED
FIRST_CLASS_SELECTED
QUARRY_ROAD_ACTIVE
QUARRY_ROAD_COMPLETE
QUARRY_ENTRANCE_DISCOVERED
QUARRY_DUNGEON_ACTIVE
EARTHLOONG_CLEARED
POST_QUARRY_BRIEFING_PENDING
ACT1_LEADS_OPEN
```

Required persistent personal fields:

```text
first_shrine_activated
first_root_class_selected
starter_package_claimed
r01_main_stage
quarry_road_action_bits
optional_contract_states
trail_stag_state
trail_stag_unlocked
regalhart_clues_seen
regalhart_discovered
quarry_discovered
quarry_relay_evidence_seen
quarry_first_clear
first_clear_reward_claimed
reward_choice_pending
post_quarry_briefing_seen
act1_western_relay_lead_known
act1_whitecrest_station_lead_known
```

All progression-changing fields are server-authoritative.

---

# 21. Exact pre-code presentation-asset gate

Before gameplay source implementation of the R01 slice begins, bind exact accepted files/sources for at least:

## Player / NPC motion

```text
universal_idle
walk
run
sprint
dash_forward
dash_back
dash_left
dash_right
roll_if_used
jump
fall
land
guard
perfect_guard_reaction
one_hand_attack_chain
two_hand_attack_chain
bow_draw_release
spell_cast
heal_cast
interaction
pickup
smith_hammer
revive
mount
dismount
drink
eat
stag_calm_interaction
```

Each binding records:

```text
source_pack
source_clip_filename
license / local-only boundary
exact artifact identity/hash where available
retarget notes
playback speed
root-motion source yes/no
server-movement profile if applicable
```

## Named NPCs

Every named R01 NPC receives before implementation:

- exact accepted body/proportion variant;
- outfit modular-part set;
- hair/head/accessory treatment;
- profession/story prop if any;
- idle/work animation set;
- color/material palette compatible with Alderford;
- source/license/artifact provenance.

No NPC binding may be `generic ranger for now`, `temporary villager`, or an internal placeholder in a player-visible build.

## Settlement / dungeon

Bind exact accepted sources for:

- Alderford gate/watch;
- Copper Kettle;
- Wayfarers' Hall;
- Holt Forge;
- Alderford Vault;
- Greenwater Remedies;
- Fordside Stables;
- shrine;
- market/sign/banner/interior prop families;
- quarry supports/carts/hoist;
- root-overgrowth treatment;
- old relay masonry/plate treatment;
- side-cache props;
- Earthloong telegraph VFX/sound.

This is a **source-bootstrap blocker**. Missing exact player-visible bindings are not deferred to implementation.

---

# 22. Azari spatial-closure gate

R01 content logic is locked here, but exact world coordinates must be authored from the real imported Azari terrain before source implementation of world placement.

The spatial pass must record, at minimum:

- Alderford center/gate/shrine/service-building coordinates;
- approach-road start and settlement reveal sightline;
- quarry overlook/entrance/chamber coordinates;
- ford event volume and stable-return route;
- Riverbank Remedies gather-area bounds;
- Signs in the Meadow evidence-site coordinates;
- Steelboar discovery/hunt territory;
- Regalhart clue/search/boss territory;
- R02/R03 outgoing road/visual lead directions;
- measured ordinary first-play travel times between major R01 beats.

The spatial pass may move a POI to fit terrain, sightlines and pacing. It may **not** redesign its quest function, reward, NPC owner, story meaning or content rules without first revising this canon.

---

# 23. First playable acceptance checklist

## Motion / combat presentation

1. dodge/dash uses accepted external motion;
2. no vanilla running/static pose during real dash displacement;
3. animation and server displacement remain visually synchronized;
4. collision-shortened dodge resolves cleanly;
5. weapon/cast/guard event timing agrees with gameplay within about one server tick where practical.

## Opening / content

6. normal new player sees Alderford within roughly 2–4 minutes;
7. first class is immediately usable after selection;
8. first board shows exactly 1 main + 2 optional contracts;
9. player can leave without visiting every service NPC;
10. `Dust on the Quarry Road` completes from any three distinct legal action categories;
11. basic gather → service loop is understandable without compulsory fetch-chain dialogue;
12. Trail Stag normally unlocks around 25–40 minutes and is free;
13. Regalhart can be found/fought without clue completion and never gates quarry;
14. first quarry run lasts roughly 15–25 minutes;
15. shortcut prevents a full trash rerun after boss failure;
16. Earthloong first clear normally lands around 55–75 minutes;
17. post-quarry scene opens both R02/R03 Act-I leads without pretending one is mandatory;
18. first-session fixed Gold does not trivialize the 2,400-Gold starter home.

## Narrative / player-facing quality

19. Alderford, all named NPCs and quest titles match this canon;
20. Ilyan does not reveal Anchor-network truth before quarry evidence;
21. Mara/Daren/Ilyan post-quarry scene delivers the Act-I handoff even when skipped, through journal state;
22. no player-facing `P0`, `alpha`, `temporary`, `TODO`, debug ID, raw localization key or implementation term is visible;
23. no mandatory dialogue is a long exposition dump; routine scenes remain immediately advanceable/skippable;
24. Kest remains optional in R01 and does not gate main progression.

## Presentation

25. no vanilla villagers as final Alderford NPCs;
26. settlement architecture reads as one coherent family;
27. forge/stable/guild are identifiable from world silhouette/signage;
28. NPC work/interactions use accepted motion;
29. important audio/VFX provenance is recorded;
30. dungeon contains no vanilla-spawner/vanilla-mob fallback content;
31. Earthloong lightning visuals match server hit areas;
32. reward choice uses accepted Lucifer-family UI and real item previews.

## Multiplayer / persistence

33. two players can split personal gathering/contracts without blocking each other;
34. eligible incomplete players can each unlock Trail Stag;
35. quarry first-clear rewards are personal;
36. boss scaling follows canonical participant rules without increased outgoing damage;
37. first-clear reward cannot duplicate through disconnect/relog/re-entry;
38. interrupted post-quarry briefing resumes/skips without losing or duplicating story state.

---

# 24. R01 design-closure status

This pass closes for implementation **as design**, not merely as a concept:

- final starting-settlement player-facing name and service names;
- named R01/recurring cast and their R01 functions;
- first-class grant anti-exploit representation;
- exact starting recovery item;
- exact main/optional quest ownership, objective counts and completion logic;
- exact first-session key dialogue lines and post-quarry briefing;
- Trail Stag encounter scaling/reset/reconnect behavior;
- quarry relay-evidence story beat;
- Earthloong-to-Act-I handoff;
- exact R01 persistent state expectations;
- player-facing developer-language prohibition;
- exact distinction between design decisions and remaining pre-code asset/spatial gates.

Still required **before gameplay source bootstrap for the relevant R01 content**:

1. exact external model/animation/VFX/audio artifact bindings and provenance/hash where applicable under §21;
2. exact Azari coordinates, volumes, sightlines and measured travel times under §22.

Those two items are not permission for an implementer to improvise design. They are dedicated pre-code planning/verification gates. If either gate exposes a hard conflict, update this canon first and only then implement the revised rule.

Real playtesting may later tune already-defined numbers such as travel time, enemy density or reward pacing when evidence shows a feel/balance problem; it must not silently invent missing gameplay systems or rewrite story/content inside code.