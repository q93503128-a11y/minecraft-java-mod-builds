# Open-World RPG — R01 Vertical Slice / Opening 60–90 Minute Canon

> Status: **DESIGN CANON — opening settlement, first-region flow, first mount, first dungeon and external motion/presentation sourcing locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Region: `REGIONS.md`  
> Combat: `COMBAT_BALANCE.md`, `STATUS_AND_R01_ENCOUNTERS.md`  
> Classes: `CLASS_COMBAT_KITS.md`, `CLASS_PROGRESSION.md`  
> Equipment/economy: `LOOT_ECONOMY.md`, `EQUIPMENT_BALANCE.md`  
> Mounts: `MOUNTS.md`  
> UI: `UI_DIRECTION.md`  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins.

This file turns the already-designed R01 systems into one playable first-session slice. The objective is that implementation does not have to invent the opening route, service order, quest density, first mount timing, first dungeon room flow, or movement-animation direction.

The target is **not a long tutorial**. The player should understand the game by moving through a real region, seeing a coherent settlement, using a few services, choosing a class, taking a short objective, finding optional content, unlocking the first mount and clearing the first dungeon.

---

# 1. External precedents used

## 1.1 Guild Wars 2 — exploration without NPC-chore chains

Useful precedent:

- after a brief tutorial, the player is placed in a real low-level explorable area;
- starting service areas expose useful crafting/storage/merchant functions;
- Renown Hearts can progress from useful actions in an area without requiring the player to talk to an NPC before every activity;
- POIs, events and world challenges encourage self-directed movement.

Project adoption:

- the opening settlement provides services quickly;
- the first main objective gives direction but local activity can progress through multiple useful actions;
- optional contracts and discoveries are visible without becoming a wall of quest markers;
- not every event starts with a dialogue interaction.

Not adopted:

- no map-completion checklist requiring every icon;
- no MMO-style permanent heart/task panel over every local area.

References:
- `https://wiki.guildwars2.com/wiki/Starting_area`
- `https://wiki.guildwars2.com/wiki/Renown_Heart`
- `https://www.guildwars2.com/en/news/explore-the-world-of-guild-wars-2/`

## 1.2 Elden Ring — visible optional danger

Useful precedent:

- an open-world field boss can be encountered early, escaped from and returned to later;
- early difficult encounters signal that the world is not arranged as a mandatory linear corridor.

Project adoption:

- Regalhart is discoverable during the opening route but never gates the quarry dungeon;
- hunt clues help the player understand that something important lives nearby without placing an exact GPS pin on it immediately;
- the player may leave, return later or defeat it early.

Not adopted:

- R01 is not tuned to repeatedly kill a new player for failing to understand a boss placed directly on the mandatory road.

---

# 2. External motion is a production requirement

External-first applies to **animation/motion itself**, not only to character models or VFX.

The following player-visible actions must begin from a selected external animation or external motion reference before implementation:

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

**Forbidden production shortcut:** moving the player several blocks with code while leaving the vanilla run pose, a static body, or an improvised two-keyframe animation and calling that a dash.

## 2.1 Primary humanoid motion sources

### KayKit Character Animations — primary dodge/dash/work-action source

Current public pack direction:

- 161 humanoid animations;
- CC0;
- movement includes walking, running, jumping, crawling, sneaking and dodging;
- also includes melee, ranged, bow, magic/spellcasting and tool/work actions;
- current files are FBX / GLTF and are intended to be retargetable.

Legacy/free KayKit animation releases explicitly include:

```text
Roll
Dash Front
Dash Back
Dash Right
Dash Left
```

Therefore baseline project dodge/dash sourcing order is:

1. inspect the current KayKit pack for the best current dodge/dash clips;
2. if the current pack's clip is not superior or exact direction coverage is missing, use the still-legal CC0 legacy `Dash Front/Back/Right/Left` / `Roll` clips;
3. retarget and retime only as needed for Minecraft proportions and canonical action duration;
4. reject the clip and select another external clip if feet/body motion cannot match the real server movement convincingly.

Source:
- `https://kaylousberg.itch.io/kaykit-character-animations`
- legacy source: `https://kaylousberg.itch.io/kaykit-animations`

### Quaternius Universal Animation Library / Library 2 — locomotion/combat/parkour source

- CC0;
- UAL: 120+ animation library covering locomotion, combat, spell-related and general actions;
- UAL2: 130+ animation library with melee/armed combos, parkour and other movement actions;
- 2026 updates provide root-motion and non-root-motion exports for locomotion/movement.

Use these when their motion reads better for a specific class/weapon/action than KayKit.

Sources:
- `https://quaternius.com/packs/universalanimationlibrary.html`
- `https://quaternius.com/packs/universalanimationlibrary2.html`

## 2.2 Server-authoritative movement versus animation root motion

The server remains authoritative for the real position, collision, stamina cost and i-frame state.

For the canonical universal dodge:

```text
duration: 0.45 s
movement target: ~3.2 blocks
i-frame: 0.30 s
Stamina cost: 30
```

External root motion may be used to derive the intended motion curve, acceleration and body pose, but **client root motion does not own gameplay displacement**.

Acceptance:

- server movement and animation contact/footwork should visually agree;
- start, i-frame body compression/evade moment and recovery should align to within about one server tick where applicable;
- no visible wall penetration because the source clip expected more distance than collision allowed;
- when collision shortens a dash, animation playback/motion must resolve gracefully instead of foot-sliding three blocks into a wall;
- if an external clip cannot be reconciled, change the clip, not the canonical hitbox/movement merely to save the asset.

The same rule applies to skill lunges, teleports with visible startup/recovery, mount charges and knockback reactions.

---

# 3. R01 visual source family

## 3.1 Starting settlement architecture

Primary architectural design source:

- **Quaternius `Medieval Village MegaKit`**;
- CC0;
- 300+ modular environment pieces;
- grid-based walls, roofs, stairs, doors/windows and related village components.

Project use:

- its silhouettes, roof language, wall rhythm, timber/stone balance and modular composition define the starting settlement;
- final world buildings are translated into Minecraft-compatible block architecture/interiors where that produces better collision, navigation and world integration;
- custom model/display elements may be used for signs, awnings, trims, props and non-block details;
- do not replace the pack's coherent design with unrelated Minecraft-build styles building-by-building.

Secondary reference/fallback only:

- Kenney `Fantasy Town Kit`, CC0, 160+ town objects;
- use only when one missing building/prop concept cannot be solved cleanly in the main Quaternius family, and visually harmonize it before admission.

## 3.2 Props / services

Primary prop source:

- **Quaternius `Fantasy Props MegaKit`**;
- CC0;
- 200+ medieval/fantasy props including tools, weapons, books, potions, market stalls, chests, furniture, cauldron and blacksmith-related objects.

Use it for:

- forge dressing;
- market stalls;
- guild interior;
- inn furniture;
- bank/storage props;
- potion/alchemy presentation;
- quest-board dressing;
- carts/crates/barrels;
- dungeon utility props where suitable.

## 3.3 NPC source family

Primary source:

- Quaternius `Universal Base Characters` — CC0, six base-character proportion variants + hairstyles, humanoid retargetable rig;
- Quaternius `Modular Character Outfits - Fantasy` — CC0, 12 outfits / 62 modular pieces, compatible with the same rig and Universal Animation Library.

The first settlement therefore uses a coherent NPC family instead of vanilla villagers or unrelated custom skins.

Important NPCs receive different silhouette/outfit/hair/prop combinations:

- guild steward / class trainer;
- smith;
- innkeeper;
- merchant;
- stable keeper;
- healer/alchemist;
- bank/storage keeper;
- quest-board/guild staff;
- selected guards / townsfolk.

NPC work animations also come from KayKit/Quaternius external motion families: hammering, interacting, carrying, writing-like interaction, idles and other accepted clips.

---

# 4. Starting settlement physical layout

The settlement remains a long-term hub rather than a disposable tutorial village.

Exact orientation follows imported Azari terrain, but the first accepted layout must preserve this readable topology:

```text
Approach road
  ↓
Gate / first shrine
  ↓ 45–60 blocks
Inn + central market square
  ├─ Guild / class hall
  ├─ Forge / smith
  ├─ Bank / Material Vault
  ├─ Healer / alchemy
  ├─ Stable and visible Trail Stag paddock
  └─ road exits / quest board
```

Target spatial rules:

- central square: roughly 25–35 blocks across;
- main service doors should normally sit within about 15–35 blocks of the square center;
- the stable is visible from the arrival/plaza route, but slightly toward the outward road so mounts do not crowd the central square;
- the shrine/checkpoint is visible immediately after the gate reveal;
- forge chimney/anvil silhouette, guild banner, inn sign, stable paddock and shrine vertical motif provide visual wayfinding without floating tutorial arrows;
- at least two starter homes are visible as purchasable housing examples from the beginning;
- no compulsory service tour quest.

## 4.1 Functional building set at first visit

Present from the beginning:

1. gate/watch structure;
2. shrine/checkpoint;
3. inn/tavern;
4. market/basic merchant;
5. adventurer guild/class facility;
6. forge/smith;
7. bank/storage / Material Vault;
8. healer/alchemy shop;
9. stable;
10. small housing cluster.

Buildings do not magically spawn after quest completion. Progress unlocks service depth, recipes or NPC dialogue inside already-existing spaces.

---

# 5. NPC density / performance

Starter-hub target at ordinary daytime population:

- **8–12 functional/service/guard NPCs**;
- **4–8 ambient townsfolk** near square/inn/roads;
- do not spawn 30 pathfinding NPCs merely to make the square look busy.

Behavior:

- service NPCs use short local anchor zones rather than full-village wandering AI;
- ambient NPCs use bounded patrol/idle routes;
- smith animation runs at the forge, stable keeper works near paddock, merchant interacts with stall, etc.;
- NPC actions are visually supported by external animations/props;
- night schedule may move selected NPCs indoors later, but is not required to block service use in the opening slice.

No vanilla villagers are used as final NPCs.

---

# 6. Opening loadout before class selection

The approach road occurs before the guild class selection, so the player needs a small universal combat vocabulary without inventing a temporary class.

Starting equipment:

- Standard Item Lv1 `Heartland Arming Sword`;
- Standard Item Lv1 `Watch Buckler`;
- one basic healing consumable once the consumable system is finalized;
- starting Gold remains the canonical ~150 Gold.

Available before class selection:

- basic weapon attack;
- sprint;
- universal dodge;
- guard / perfect guard with the buckler;
- interact.

Unavailable until class selection:

- class mechanic;
- four class actives;
- ultimate.

On the first free root-class selection, the guild provides **one non-sellable-for-profit starter package** appropriate to making that class immediately functional:

| Root class | Recommended free starter equipment |
|---|---|
| Warrior | keep Heartland Arming Sword + Watch Buckler; no duplicate Gold-value grant |
| Hunter | Riverwood Bow |
| Cleric | Initiate Staff |
| Mage | Initiate Wand |
| Guardian | keep sword + Watch Buckler |

Rules:

- this is a usability grant, not a class weapon lock;
- it occurs only for the player's first-ever root-class selection;
- switching classes later does not create infinitely sellable starter gear;
- granted gear cannot be sold until replaced or may have zero sell value; implementation chooses the cleanest anti-exploit representation without changing its combat stats.

---

# 7. Opening timeline target

Normal first-play flow should usually reach the Earthloong first clear in roughly **55–75 minutes**. Optional exploration, Regalhart, extra gathering, inventory inspection, housing browsing or experimentation can naturally extend the same first session toward **90+ minutes**.

This is a pacing target, not a speedrun timer or forced mission clock.

## Phase A — 0:00–0:04 — approach road / first reveal

Goals:

- establish movement and camera;
- demonstrate that creatures are non-vanilla;
- teach one real combat read without a tutorial arena;
- reveal the settlement quickly.

Sequence:

1. player begins on a short approach road/outskirts route;
2. Louxia/gazelle-style ecology is visible at a distance before combat;
3. a single authored Meadow Viper encounter demonstrates its coil/warning tell;
4. one compact context prompt may show `Dodge` only when the Viper commits to its first bite;
5. player can kill it, guard it, dodge it or simply back away;
6. crest/gate reveal shows the settlement;
7. first shrine/checkpoint activates at the gate/inside approach.

The opening pre-shrine death remains economically free as already canonical.

Animation requirements:

- run/sprint from external locomotion source;
- dodge from accepted KayKit/Quaternius external clip;
- sword/basic guard from Better Combat/external animation family;
- Viper uses its accepted external Snake animation family.

No forced `press W`, `press space`, `talk to three NPCs` corridor.

## Phase B — ~0:04–0:10 — settlement / class choice

On first arrival:

- shrine is usable;
- guild/class facility is immediately reachable;
- forge, market, bank, inn, stable and healer are visible/usable at their baseline level;
- quest board shows **1 main objective + 2 optional contracts**;
- first root class selection is free.

Class selection immediately activates its root mechanic, four actives and root ultimate under `CLASS_COMBAT_KITS.md`.

The player is free to leave town immediately after choosing a class.

## Phase C — ~0:10–0:25 — first open-field loop

Main objective: **Dust on the Quarry Road**.

Narrative function:

- traffic toward the old quarry has become unsafe;
- do not explain the full Earthloong/root problem yet.

The first road area uses a flexible local objective rather than a linear checklist. Progress can come from several nearby useful actions, for example:

- recover lost road cargo;
- drive off/defeat a Meadow Viper threat;
- inspect one damaged wagon/road marker;
- gather one nearby Hardwood/Iron/Healing Herb node relevant to repair/supplies;
- help resolve a small local event if it is active.

A player should not need to do every possible action.

Reward target:

```text
EXP: ~35% of current next-Lv requirement
Gold: 90
Class XP: ~25% of current Class Rank requirement
```

First optional contracts:

### Riverbank Remedies

- gather a small amount of Healing Herb from a marked broad river/forest-edge area;
- the nodes themselves remain personal;
- no requirement to craft ten potions afterwards.

Reward:

```text
EXP: ~20%
Gold: 60
Class XP: ~15%
```

### Signs in the Meadow

- investigate a damaged cart / hoof-scarred roadside site;
- teaches bison/large-creature territory and foreshadows Steelboar/Regalhart rather than immediately demanding an elite kill.

Reward:

```text
EXP: ~20%
Gold: 70
Class XP: ~15%
```

## 7.1 Service depth unlocks are material-driven, not errand-driven

When the player first obtains the relevant material and returns/opens the service:

- Iron Ore / Hardwood makes the basic forge crafting options meaningful/visible;
- Healing Herb makes basic alchemy options meaningful/visible;
- Louxia Meat/food materials expose the inn/cooking interaction when that consumable data is finalized;
- Verdant Crystal later reveals the first Superior R01 recipe tier.

There is no separate `talk to smith so he teaches you what iron is` quest.

## Phase D — ~0:25–0:40 — first Trail Stag

Quest/event: **A Stag at the Ford**.

Availability:

- stable and keeper are visible from first arrival;
- event becomes directly hinted after the road objective;
- the player can also discover the ford first and trigger the event organically;
- target local Lv: ~3–4;
- normal timing: first 25–40 minutes.

Event flow:

1. find a Trail Stag near a damaged riverside harness/cart route;
2. resolve 1–2 small threats / unsafe approach conditions, normally Meadow Vipers or a local territorial hazard;
3. use a short external interaction/calming animation rather than a menu-only `claim mount` button;
4. mount the Stag;
5. ride it back toward the stable along a short road segment, naturally teaching steering/jump/dismount;
6. stable registration becomes permanent and free.

This is **not an escort quest**: the player rides the animal; the AI does not slowly walk beside the player.

Reward:

```text
Trail Stag permanent unlock
Gold: 60
EXP: ~25%
Class XP: ~20%
```

Movement remains `MOUNTS.md`:

- 6.4 b/s cruise;
- forgiving steering;
- no mount stamina drain;
- no mount attack.

External source:

- Quaternius `Ultimate Animated Animal Pack` Stag, CC0.

## Phase E — ~0:35–0:55 — quarry approach / optional major threats

Main objective: **Roots Below Stone**.

Flow:

- travel toward the abandoned quarry;
- encounter visible ore nodes and the first deep/cave transition;
- first Cave Centipede teaches vertical awareness;
- inspect the old lower-workings entrance/root intrusion;
- reveal the dungeon entrance and Suggested Lv 8 warning.

Reward for the investigation milestone before entering the dungeon:

```text
EXP: ~40%
Gold: 120
Class XP: ~25%
```

No hard Lv8 gate. A skilled under-level player may enter.

### Optional Steelboar contract — Steel in the Grass

The contract appears only after the player has seen/identified a Steelboar or its clear territorial evidence.

Reward beyond the elite's normal loot/EXP:

```text
EXP: ~30%
Gold: 100
Class XP: ~15%
```

It is not required for dungeon access.

### Regalhart discovery — the Crowned Trail

Regalhart is not shown as a precise boss pin on first arrival.

Three authored clue types exist across the meadow/deep-grove boundary:

1. antler-height scoring on a large tree / wooden structure;
2. unusually deep hoof furrows / trampled vegetation;
3. a broken hunter/road marker with visible crown-shaped antler damage.

Discovering any **2 of 3**:

- adds a broad search region to the map;
- adds a small field-boss/hunt entry to the journal;
- does **not** spawn Regalhart artificially if its encounter controller is already valid;
- does not gate fighting it if the player finds the boss before finding clues.

This keeps the boss optional and discoverable rather than turning exploration into GPS following.

## Phase F — ~0:45–0:70 — first quarry dungeon

Target first-clear duration remains **15–25 minutes**.

The dungeon is a physical authored location in Azari/R01, not a random maze.

### External architecture direction

Primary external design/reference bases:

- Quaternius `Modular Dungeon Pack`, CC0;
- Quaternius `Ultimate Modular Ruins Pack`, CC0;
- Quaternius `Fantasy Props MegaKit`, CC0.

DeCubed Dungeons may remain a current 26.2 **layout/reference/local-only** source under its CC-BY-NC-SA terms, but its vanilla spawners/loot/style are not copied into the public project as the canonical R01 dungeon.

The final block structure should preserve a coherent quarry → ruined workings → root-overgrown chamber language rather than stitching unrelated downloaded rooms together.

### Room / encounter sequence

#### 1. Upper Mining Gallery

Purpose:

- establish abandoned industrial/quarry identity;
- expose Iron Ore and mining props;
- short first combat group using Cave Centipede / small accepted threats;
- one visible optional side ledge/cache.

Target active combat: ~45–75 seconds.

#### 2. Collapsed Hoist Chamber

Purpose:

- one compact traversal problem around broken platforms/hoist rather than a puzzle menu;
- a second combat space with vertical centipede pressure;
- nearby mechanism opens a **persistent-in-run lift shortcut** back toward the entrance.

No mandatory material sacrifice is required to repair the hoist. The player should not be softlocked for failing to carry Hardwood.

Target room time: ~2–4 minutes.

#### 3. Root-Breached Workings

Purpose:

- visual transition from ordinary quarry into fantasy ecology;
- one **Nature Spirit elite** encounter using its existing Living Shell / melee identity;
- Verdant Crystal / herb side cache shows why the roots matter to the region's materials.

Target active combat: ~15–25 seconds plus positioning/recovery; room total ~2–4 minutes.

The Nature Spirit is the one major pre-boss elite. Do not pad the dungeon with three copies of it.

#### 4. Boss Antechamber / opened lift shortcut

- short breathing/readability space;
- shortcut ensures a boss death does not require replaying the entire dungeon trash route;
- no long cutscene;
- no new permanent shrine/fast-travel node inside the dungeon.

#### 5. Earthloong Chamber

Use the exact `STATUS_AND_R01_ENCOUNTERS.md` boss kit:

- Lv8;
- 4,900 HP solo baseline;
- ~120–150 s active-combat target;
- physical phase plus lightning space-control phase;
- no long untargetable state;
- break only authored `r01_earthloong_breakable_prop` blocks;
- visible lane/decal range matches server hit area.

### Dungeon first-clear rewards

Keep existing canon:

- Earthloong Scale x2;
- guaranteed Superior+ normal boss gear;
- 15% direct Mythic roll from `Rootquake Maul / Earthscale Ward` pool;
- completion choice: `Ironroot Longsword / Riverthorn Bow / Lumenwood Staff`;
- completion EXP: 50% current next-Lv requirement + boss contribution;
- completion Class XP: 32% + boss contribution.

Add first-clear Gold baseline:

```text
Gold: 180
```

The deterministic three-item choice appears in the accepted Lucifer reward panel with real accepted model/icon previews. It is not a vanilla chest GUI.

After the first clear, returning to the smith exposes the known Earthloong Signature Craft progress (`2 / 4 Earthloong Scales`) without forcing an immediate repeat clear.

## Phase G — ~0:65–0:90+ — free continuation

After the first dungeon clear, the game stops behaving like a tutorial.

Natural options:

- return to settlement, compare/re-equip reward and use forge;
- continue saving toward first home;
- pursue Regalhart;
- hunt Steelboar/Nature Spirit;
- explore unfinished R01 POIs/resources;
- investigate R02/R03 route signals;
- complete first specialization/class challenge if Class Rank has reached the relevant threshold;
- replay the quarry if desired.

No mandatory `now talk to six NPCs because the tutorial is over` sequence.

---

# 8. Quest-board density and marker rules

On first arrival:

```text
Main objectives visible: 1
Optional contracts visible: 2
```

During the first session:

- no more than about 4 unresolved R01 board/guild hooks should be simultaneously foregrounded by default;
- additional contracts appear through discovery/progress rather than all being dumped at time zero;
- discovered POIs may appear on the map;
- optional bosses use clue/search areas before exact markers where that improves exploration;
- dungeon entrance may receive an exact marker after it has been physically discovered/investigated;
- quest UI never covers the screen with an MMO checklist while free-roaming.

Main objectives remain roughly the 30% guidance side of the project's 30/70 guidance/exploration target.

---

# 9. Gold / early economy check

Starting liquid balance remains approximately **150 Gold**.

Fixed first-session authored rewards in this slice:

| Objective | Gold |
|---|---:|
| Dust on the Quarry Road | 90 |
| Riverbank Remedies | 60 |
| Signs in the Meadow | 70 |
| A Stag at the Ford | 60 |
| Roots Below Stone | 120 |
| Steel in the Grass | 100 |
| Earthloong first dungeon completion | 180 |

A player who completes every optional listed activity earns **680 Gold** from these objective payments, plus ordinary combat/material/loot income. A more direct player earns less.

This remains consistent with the early gross-income target of roughly 500–700 Gold/hour and keeps the 2,400-Gold starter home a real multi-hour savings target instead of a first-session freebie.

The first Trail Stag itself is free after the event, so basic traversal is not delayed by Gold grinding.

---

# 10. Audio source direction for the slice

No important R01 sound is accepted as `use whatever vanilla sound is closest` by default.

Primary redistributable source families:

- **Kenney RPG Audio** — CC0, 50 footsteps/weapon/RPG Foley sounds;
- **Kenney Impact Sounds** — CC0, 130 impact/Foley sounds;
- **Kenney UI Audio / Interface Sounds** — CC0, 50/100 interface sounds;
- selected **CC0 Freesound/OpenGameArt ambience** with exact source recorded.

Current ambient candidates:

- Freesound `Forest birds - ambient seamless loop` by Magnesus — CC0, 27-second seamless forest-bird loop;
- OpenGameArt `Park ambiences` — CC0 recordings for birds/river/wind;
- use only recordings without obvious modern contamination after listening in context.

Per-area direction:

- settlement: restrained people/work/forge layers, not constant loud crowd loop;
- meadow: wind + sparse wildlife;
- riverwood: water + bird/leaf ambience;
- quarry: stone/wood creak/low cave air, less wildlife;
- Nature Spirit/root sections: subtle tonal layer, not generic horror drone;
- Earthloong: dedicated body impact, stone/root, electricity charge/strike layers selected during asset intake.

Every accepted sound receives source URL/license/exact filename/hash in source bindings before release-quality implementation.

---

# 11. VFX / signage / interaction presentation

Use existing external-first visual rules.

- quest board is a real external prop/model/block structure with restrained interaction highlight;
- forge uses real furnace/anvil/tool props and smith animation rather than a floating menu kiosk;
- stable has real paddock/tack/feeding props;
- shrine has a distinctive external-source vertical silhouette and VFX family;
- mount unlock uses Stag animation + tack/registration presentation, not a text-only unlock toast;
- resource nodes use their accepted external models;
- dungeon danger decals and Earthloong lightning lanes use external VFX sprites/geometry and match hitboxes exactly;
- important rewards show real equipment model/icon preview.

Lucifer-family UI remains the overlay language for dialogue/reward/inventory/service screens.

---

# 12. Multiplayer behavior for the opening slice

Progression is per player unless a state is explicitly world-shared.

## Settlement / services

- shrine discovery, class selection, inventory/bank state, mount unlock, quest completion and first-clear rewards are personal/server-authoritative;
- service buildings physically exist for everyone;
- one player's class choice does not change another player's available class.

## Field objectives

- natural enemies and world events are shared physical entities;
- eligible players receive personal EXP/loot/progress according to contribution rules;
- personal gathering nodes remain personal;
- a player may complete `Riverbank Remedies` without requiring the other player to gather the same nodes.

## Trail Stag event

- eligible nearby players may complete the rescue together;
- each eligible participant receives their own permanent Trail Stag unlock;
- a player who missed the event can trigger/replay the personal unlock state later without deleting another player's mount;
- event implementation may respawn/reset its authored Stag interaction for incomplete players after the area is idle; completed players cannot farm rewards.

## Quarry dungeon

R01 quarry remains a physical authored dungeon rather than requiring dynamic per-party world copies at launch.

- encounter controller tracks currently engaged eligible players;
- boss scales by existing participant HP/poise rules;
- outgoing boss damage does not increase with player count;
- first-clear/completion/signature loot is personal;
- internal shortcut state resets with the dungeon encounter cycle as appropriate;
- players joining after boss engagement do not receive first-clear reward without meeting normal contribution/eligibility rules;
- no player can duplicate first-clear reward by relogging or changing participant count.

---

# 13. R01 presentation asset manifest requirements

Before the slice is considered asset-ready, bind exact files for at least:

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
```

Every entry records:

```text
source_pack
source_clip_filename
license
retarget_notes
playback_speed
root_motion_source: yes/no
server_movement_profile_if_applicable
```

## Settlement

- gate/watch design source;
- inn;
- guild;
- forge;
- bank/storage;
- healer/alchemy;
- stable;
- shrine;
- market stalls;
- housing shells;
- major signs / banners;
- major interior prop families.

## Dungeon

- quarry block palette/reference;
- mine supports/carts/hoist props;
- root-overgrowth treatment;
- side-cache props;
- breakable arena props;
- Earthloong telegraph VFX and sound assets.

No manifest entry may be `todo_asset`, `use vanilla for now` or `make AI placeholder` in a player-visible build.

---

# 14. Data / state contract

Suggested ownership:

```text
opening/
  starting_loadout.json
  settlement_services.json
  r01_opening_flow.json

quests/r01/
  dust_on_quarry_road.json
  riverbank_remedies.json
  signs_in_meadow.json
  stag_at_ford.json
  roots_below_stone.json
  steel_in_grass.json
  crowned_trail.json

dungeons/r01_quarry/
  rooms.json
  encounters.json
  shortcut.json
  completion_rewards.json

presentation/
  motion_bindings.json
  npc_bindings.json
  settlement_bindings.json
  audio_bindings.json
```

Opening state must separately store:

```text
first_shrine_activated
first_root_class_selected
starter_package_claimed
trail_stag_unlocked
r01_main_stage
optional_contract_states
regalhart_clues_seen
quarry_discovered
quarry_first_clear
first_clear_reward_claimed
```

All progression-changing fields are server-authoritative.

---

# 15. First playable acceptance checklist

When this vertical slice is implemented, acceptance requires more than build success.

## Motion

1. dodge/dash uses an accepted external animation clip;
2. no vanilla running/static pose during real dash displacement;
3. animation and 3.2-block/0.45-second server movement remain visually synchronized;
4. collision-shortened dodges do not create obvious foot skating/wall penetration;
5. weapon/cast/guard hit timing matches gameplay events within about one server tick where applicable.

## Opening flow

6. new player reaches settlement reveal in roughly 2–4 minutes without a long forced tutorial;
7. first class can be chosen and used immediately;
8. first board presents 1 main + 2 optional objectives, not a quest wall;
9. player can leave town without talking to every service NPC;
10. basic gather → return → service loop is understandable without compulsory fetch-chain dialogue;
11. Trail Stag normally unlocks around 25–40 minutes and is free;
12. Trail Stag visually uses accepted external Stag animation/model and feels like 6.4 b/s rather than a reskinned vanilla horse;
13. Regalhart can be found/fought without completing its clue sequence and never gates the dungeon;
14. first quarry run lasts roughly 15–25 minutes on normal first play;
15. opened shortcut prevents full trash rerun after a boss failure;
16. first Earthloong clear usually lands around 55–75 minutes of a normal first session;
17. completion reward choice uses real item art/models and the Lucifer reward UI;
18. the first session does not normally provide enough Gold to trivialize the 2,400-Gold starter house goal.

## Presentation

19. no vanilla villagers are visible as final settlement NPCs;
20. settlement architecture reads as one coherent external design family;
21. forge/stable/guild are identifiable from world silhouette/signage before opening the map;
22. NPC work/interaction actions use external animations rather than static entities;
23. important audio/VFX sources are external and provenance-recorded;
24. dungeon rooms do not contain vanilla spawners/vanilla-mob fallback content;
25. Earthloong lightning visuals match server hit areas.

## Multiplayer

26. two players may split gathering/contracts without blocking each other's permanent progress;
27. both eligible players can earn Trail Stag unlock;
28. quarry rewards are personal;
29. boss scaling follows existing HP/poise participant rules and does not raise outgoing damage;
30. first-clear rewards cannot be duplicated through relog/re-entry.

---

# 16. What this pass closes

Closed for implementation:

- external-animation requirement for dash/dodge/roll/locomotion and other player-facing motion;
- primary KayKit / Quaternius motion families;
- root-motion versus server-authority boundary;
- starting settlement coherent external architecture/prop/NPC families;
- settlement physical service topology and NPC-density target;
- pre-class opening loadout and first-class usability grant;
- opening 0–90 minute flow;
- first quest/contract density and initial reward values;
- Trail Stag event timing and interaction flow;
- Regalhart clue/discovery behavior;
- quarry dungeon room/shortcut/encounter sequence;
- Earthloong first-clear placement in the opening flow;
- early Gold pacing check;
- R01 audio-source direction;
- multiplayer state behavior for the first slice;
- motion/settlement/dungeon asset manifest requirements;
- first playable acceptance checklist.

Still intentionally requires later passes before all gameplay coding is design-complete:

- exact consumable/healing-food/alchemy/cooking stat tables;
- exact dialogue/lore/final settlement and NPC names after Azari terrain/lore naming is locked;
- exact downloaded model/animation/audio filenames and hashes during asset intake;
- R02+ vertical content;
- actual Azari coordinate placement after local map import and inspection;
- final tuning after real Minecraft playtest.