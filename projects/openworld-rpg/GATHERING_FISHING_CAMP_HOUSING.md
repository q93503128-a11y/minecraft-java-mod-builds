# Open-World RPG — Gathering / Fishing / Camp / Housing Canon

> Status: **DESIGN CANON — field gathering, fishing, temporary camps and player housing rules locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> R01 resources/equipment: `EQUIPMENT_BALANCE.md`  
> Recovery/cooking: `RECOVERY_PRODUCTION_APPEARANCE.md`  
> Opening flow: `R01_VERTICAL_SLICE.md`  
> UI: `UI_DIRECTION.md`  
> External provenance: `EXTERNAL_SOURCES.md`  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins.

This document closes the next implementation-time gaps without turning the open-world RPG into a survival chore simulator.

The shared objective is:

```text
explore
→ notice a useful place/resource
→ perform a short readable interaction
→ gain a resource, fish, rest option or property value
→ feed cooking/forge/economy/collection
→ return to exploration
```

No subsystem below introduces a new numeric currency, routine durability, mandatory hunger, long gathering grind, camp fast-travel network or construction-heavy housing simulator.

---

# 1. External precedents and adoption boundaries

## 1.1 Guild Wars 2 — readable node categories, dedicated tools

Useful precedent:

- mining, logging and harvesting are separate readable gathering categories;
- nodes are placed in environmentally sensible locations;
- dedicated tools communicate the interaction clearly;
- higher tool tiers can gate higher resource tiers.

Project adoption:

- Mining / Forestry / Herbalism use dedicated field tools;
- nodes belong to region/terrain/ecology rather than random hidden block spam;
- better tools unlock stronger nodes and shorten interaction time.

Not adopted:

- disposable tool charges;
- tools breaking after a fixed number of uses;
- ruined/junk material punishment for using the wrong tool.

Reference: `https://wiki.guildwars2.com/wiki/Gathering`

## 1.2 Genshin Impact / Stardew Valley — fishing needs one real interaction layer

Useful precedent:

- cast position and bite timing matter;
- a caught fish can require active reeling/tension management rather than one passive timer;
- fish identity can affect difficulty.

Project adoption:

- deliberate cast;
- readable bite cue + reaction window;
- common fish resolve quickly;
- stronger/rarer fish use a compact hold/release tension phase.

Not adopted:

- long mandatory fishing sessions;
- one bait type per ordinary fish species;
- frequent trash catches;
- a difficult minigame for every tiny common fish.

References:
- `https://genshin-impact.fandom.com/wiki/Fishing`
- `https://wiki.stardewvalley.net/Fish`

## 1.3 Monster Hunter Wilds — field camp as convenient infrastructure, not a second town

Useful precedent:

- a field camp is quick infrastructure for recovery/preparation;
- placement/availability is bounded;
- the camp improves long expeditions without replacing settlements.

Project adoption:

- one reusable Field Camp Kit per player;
- one active deployed camp per owner;
- rest, Recovery Belt reload and cooking;
- authored no-camp volumes protect bosses/dungeons/towns/critical routes.

Not adopted:

- camp fast travel at baseline;
- repeated currency fee on every deployment;
- camp destruction/repair busywork as a routine tax.

Reference: current Monster Hunter Wilds pop-up camp behavior was used only as structural precedent.

## 1.4 ESO housing storage — physical home, shared logical storage access

Useful precedent:

- housing storage is an explicit service distinct from ordinary inventory;
- storage furniture can expose the same logical storage rather than multiplying capacity infinitely per placed chest.

Project adoption:

- owned homes expose a personal `Home Storage` pool;
- additional homes do not multiply the pool merely by placing duplicate storage props;
- furnishing remains physical and visible.

Not adopted:

- monetized housing limits or subscription storage.

Reference: `https://help.elderscrollsonline.com/app/answers/detail/a_id/41067/`

---

# 2. External visual / motion source direction

## 2.1 Gathering tools and resource nodes

Already selected families remain authoritative:

- KayKit `RPG Tools Bits` — pickaxe/axe/hand-tool family;
- KayKit `Resource Bits` — ore, wood and material pickup family;
- Quaternius `Stylized Nature MegaKit` — herb/plant/rock node family;
- KayKit `Forest Nature Pack` — fallback environmental node family;
- KayKit Character Animations 1.1 — exact published work clips including `Pickaxe`, `Pickaxing`, `Chop`, `Chopping`, `Dig`, `Digging`, `Work_*`, `Working_*`.

Exact imported model files still go through the R01 asset-intake provenance/hash/scale review before implementation.

## 2.2 Fishing ecology and motion

Preferred visible fish family:

- Quaternius `LowPoly Animated Fish` / creator-uploaded `Animated Fish` artifact;
- current creator pages/source-specific publication expose the pack under CC0;
- fish are rigged and already include swimming animation.

The R01 fish species names are **not locked from text alone**. First inspect the actual fish models and choose the subset whose silhouettes suit an early river/meadow region; then assign player-facing species names/rarity/food value.

Fishing player-motion source is already strong in KayKit Character Animations 1.1:

```text
Fishing_Cast
Fishing_Bite
Fishing_Catch
Fishing_Idle
Fishing_Reeling
Fishing_Struggling
Fishing_Tug
```

The rod model must come from an accepted external source. KayKit `RPG Tools Bits` EXTRA is a legal CC0 candidate if acquired, but a free/public-safe source may replace it if it matches the character/tool language better. Do not invent a low-quality internal rod simply to avoid this intake step.

## 2.3 Camp visuals

Primary camp-prop candidate:

- Kenney `Survival Kit` 2.0 — current asset page states CC0 and contains 80 survival/nature models.

Use a coherent subset for:

- tent/shelter;
- bedroll/rest prop;
- campfire/cooking point;
- small crate/bag/sign/utility dressing where useful.

Camp appearance must remain compact enough to deploy in the authored world without looking like a prefabricated house.

## 2.4 Housing visuals

- shell/settlement language: the same accepted Quaternius Medieval Village Standard/MegaKit family used by the starting settlement;
- furniture/props: accepted Quaternius Fantasy Props, KayKit Restaurant/Dungeon furniture or another coherent accepted family;
- do not make the purchased house interior look like a different asset-store project from the settlement exterior.

---

# 3. Shared gathering interaction contract

Gathering is a **short authored interaction**, not repeated left-click block breaking.

## 3.1 Tool Pouch

The character owns a separate logical Tool Pouch containing:

```text
Mining Pick
Woodcutter Axe
Harvest Knife / Sickle
Fishing Rod
```

Rules:

- these tools do not consume general-backpack or hotbar slots;
- the correct tool auto-equips visually when a valid node/spot is used;
- tools have no routine durability;
- field tools are not combat weapons and do not occupy combat equipment slots;
- tool visuals still use actual accepted external models and animation bindings.

### Starting kit

The initial character begins with baseline **Field** versions of all four tools in the Tool Pouch.

Reason:

- R01 already allows gathering in the first open-field objective;
- forcing the player to buy four prerequisite tools from four NPCs would add friction before the core loop has started;
- progression comes from better tools and harder nodes, not from withholding basic interaction.

This does not add four visible starter-inventory items because the Tool Pouch owns them.

## 3.2 Interaction timing

Baseline Field-tool action times before mastery/tool-speed modifiers:

| Node/action | Baseline time | Presentation |
|---|---:|---|
| herb / small forage | 0.65 s | one cut/pick motion |
| timber node | 1.35 s | two readable chop beats |
| ordinary ore | 1.55 s | two-to-three readable pick strikes |
| rare crystal / dense mineral | 1.80 s | stronger mining commitment |
| ordinary pickup/cache material | 0.35–0.55 s | hand/pickup interaction rather than tool spam |

Rules:

- no active gathering while the player is in combat state;
- no gathering while mounted, downed, climbing or in an incompatible committed action;
- movement/dodge can cancel before the server resolution point with no node consumption;
- after the resolution point, reward ownership is committed even if the recovery animation is interrupted;
- hostile forced interruption before resolution cancels the attempt and leaves the personal node available;
- important event frames and tool contact must align with the server resolution within about one tick where practical.

## 3.3 Node targeting / readability

- nodes use real external models/silhouettes and environmental placement;
- no giant permanent glowing beam over every herb/ore node;
- a restrained outline/interaction marker appears only when the player is within practical interaction range or uses a valid accessibility/highlight setting;
- world map does not reveal every ordinary node;
- discovered rich/rare authored locations may be remembered on the map when that supports route planning.

---

# 4. Tool progression

Use **three broad permanent tool tiers** across the full Lv 1–80 game rather than a six-tier material treadmill.

| Tool tier | Role | Speed modifier | Node access |
|---|---|---:|---|
| Field | starting baseline | 100% action time | ordinary/common nodes |
| Refined | early-mid progression | 85% action time | ordinary + dense regional nodes |
| Masterwork | mid-late progression | 75% action time | all ordinary/dense + authored high-tier nodes |

Rules:

- upgrades are permanent items/Tool-Pouch state, not disposable charges;
- a better tool can gather lower-tier nodes normally;
- a tool below the required tier simply reports that the node is too hard/advanced and does **not** consume the node or produce junk;
- tool tier is unlocked through world/service/material progression, not a naked `Lv 20 required` menu check;
- tool upgrades may use real regional materials + Gold, but do not create a separate tool-upgrade currency;
- final exact recipes are region-content work and must use already-admitted external materials.

The speed gain is intentionally useful but not so large that the player feels punished for exploring before obtaining the next tool tier.

---

# 5. Light gathering mastery

Mining, Herbalism, Forestry and Fishing each use a **five-rank automatic mastery track**.

There are:

- no mastery skill points;
- no mastery tree;
- no respec;
- no mandatory daily task;
- no mass-craft equivalent.

## 5.1 Mastery XP

Baseline mastery XP events:

```text
ordinary successful gather/catch: 1
rich/dense/rare successful gather/catch: 3
first discovery of a material/fish family in a major region: +10 once
relevant authored gathering/fishing contract or discovery: +6 to +15
```

Rank thresholds:

```text
Rank I   = 0
Rank II  = 20
Rank III = 60
Rank IV  = 130
Rank V   = 240
```

This means repeated gathering can progress mastery, but exploration and new regional resources accelerate it naturally.

## 5.2 Mining / Herbalism / Forestry rank benefits

| Rank | Benefit |
|---|---|
| I | baseline interaction |
| II | gathering action time -5% |
| III | 5% chance for +1 ordinary base material |
| IV | total gathering action time -10% |
| V | 10% chance for +1 ordinary base material; rare-secondary roll chance +10% relative |

Restrictions:

- bonus yield never duplicates boss/signature materials;
- rare-secondary bonus applies only to a node's already-defined secondary table;
- mastery cannot bypass minimum tool tier.

The economy impact stays small enough that gathering remains a route/activity choice rather than the only rational Gold strategy.

---

# 6. R01 node behavior

The existing R01 resource catalog remains unchanged:

| Resource | Base yield | Personal respawn |
|---|---:|---:|
| Iron Ore | 2–4 | 7 active min |
| Hardwood | 2–3 | 5 active min |
| Healing Herb | 1–2 | 4 active min |
| Verdant Crystal | 1 | 18 active min |

Additional rules:

- respawn timers are per-player and advance only while that player/world state is actively loaded according to the existing active-playtime convention;
- the node's shared physical shell may remain visible, but interaction availability/feedback is personal;
- one player's harvest cannot deny another player's harvest;
- changing dimension/relogging does not reset the node;
- node availability is server-authoritative save state;
- ordinary R01 nodes use Field tools;
- Verdant Crystal may require the Refined Pick after the first scripted/introductory crystal access if visual playtesting shows that the tool upgrade creates useful access progression. The first required R01 progression path must never softlock on this rule.

That last clause prevents tool progression from contradicting the already-authored first-dungeon/first-Superior flow.

---

# 7. Fishing — world interaction

Fishing is a real small gameplay loop, but it must not become a separate full game that interrupts the action-RPG pace.

## 7.1 Fishing spots

Use authored/region-generated **shoal spots** in suitable water rather than allowing identical reward tables from every one-block puddle.

A spot communicates itself through restrained world presentation:

- water ripple / disturbed surface;
- occasional accepted external fish silhouette below the surface where technically/visually practical;
- environmental placement near river bends, pools, banks, docks, reefs or region-specific water features.

No giant floating fishing icon is permanently visible in the world.

### Personal availability

R01/common fishing spot baseline:

```text
personal catches before depletion: 2
personal respawn after depletion: 6 active minutes
```

Rare/special authored spot baseline:

```text
personal catches: 1
personal respawn: 15–20 active minutes or authored event condition
```

The exact regional table can override these values when a location is intentionally richer.

## 7.2 Casting

1. interact/use Fishing Rod near a valid fishing spot;
2. character transitions into accepted `Fishing_Idle`/cast stance;
3. hold input briefly to aim cast direction/distance;
4. release to cast;
5. landing too far outside the valid shoal simply produces no bite and can be recast immediately after the line returns;
6. ordinary casting does not consume bait at baseline.

The system does not require one bait item per common fish species.

Special bait may exist later only for a rare hunt/target-fishing role that justifies another item.

## 7.3 Bite timing and hook

Field Rod baseline bite delay:

```text
minimum: 1.5 s
maximum: 5.0 s
```

A bite produces:

- rod/line motion;
- clear external/accepted sound;
- small contextual hook cue near the crosshair/character, not a large modal.

Hook reaction windows:

| Fish difficulty | Hook window |
|---|---:|
| common | 0.90 s |
| uncommon | 0.75 s |
| rare | 0.65 s |
| trophy/signature | 0.55 s |

Missing the hook does not consume the personal fishing-spot charge.

## 7.4 Catch resolution

### Common fish

A successful hook on an ordinary common fish uses a short automatic reel/catch finish:

```text
~1.0–1.5 s after hook
```

Do **not** force a tension minigame for every small fish.

### Uncommon / rare / trophy fish

Use one compact hold/release tension mechanic:

- holding increases line tension;
- releasing decreases tension;
- progress rises while tension is inside the fish's moving valid zone;
- progress slowly decays outside it;
- extreme high/low tension for too long breaks the attempt;
- intended duration is about 2.5–6 s, with exceptional trophy fish capped around 8 s.

Use `Fishing_Reeling`, `Fishing_Struggling`, `Fishing_Tug` and `Fishing_Catch` motion states as appropriate.

This is the recommended middle ground between a one-click timer and a long Stardew-style minigame.

## 7.5 Fishing mastery benefits

| Rank | Fishing benefit |
|---|---|
| I | baseline |
| II | maximum bite wait -8% |
| III | hook windows +0.08 s |
| IV | maximum bite wait -15% total |
| V | tension valid-zone width +10%; trophy-size/value roll gets one extra bounded roll |

Fishing mastery does not directly multiply rare-fish drop chance enough to invalidate exploration/location/time conditions.

## 7.6 Fish rewards

Ordinary fishing output feeds existing systems:

- cooking ingredients;
- selected regional contracts;
- collection/trophy records;
- a small number of region-specific crafting materials where visually/logically justified.

Rules:

- ordinary fishing does not directly drop random equipment;
- no large trash/junk table at baseline;
- fish are not a separate currency;
- common fish remain obtainable under ordinary conditions;
- time/weather may improve weights or expose special catches, but baseline progression never requires waiting idly for a narrow clock/weather window.

Player-facing R01 fish names/stat values wait for exact model intake from the external fish family.

---

# 8. Field Camp Kit

A camp is temporary **field infrastructure**, not a portable settlement.

## 8.1 Unlock / recipe

R01 baseline reusable kit recipe/service target:

```text
4 Hardwood
+ 2 Tough Hide
+ 40 Gold service fee
→ Field Camp Kit
```

The kit is permanent after creation. Repeated deployment does not consume another 4 Hardwood + 2 Tough Hide.

Reason:

- material input connects gathering/ecology to exploration;
- repeated per-placement material tax would become inventory/route friction rather than a meaningful choice.

The recipe may become visible after the player has obtained the required materials; it does not require a separate tutorial quest.

## 8.2 Deployment

Baseline deployment:

```text
commit time: 2.5 s
maximum active camps per owner: 1
```

Legal placement requires:

- out of combat;
- sufficient roughly flat footprint;
- not submerged;
- not inside settlements, dungeons, boss arenas, authored encounter volumes or protected structures;
- not blocking a major road/door/critical traversal route;
- not within 24 blocks of a shrine/major settlement service center;
- not within 12 blocks of another active camp unless the game is deliberately treating them as one party camp zone.

If placement fails, no state/resource is lost.

## 8.3 Camp functions

A deployed camp provides:

- full HP/Mana/Stamina rest under the already-canonical camp-rest rules;
- Recovery Belt reload from carried reserves;
- cooking from known recipes/materials;
- safe equipment/inventory management while not in combat;
- visible tent/bedroll/campfire field presentation.

It does **not** provide at baseline:

- fast travel;
- death checkpoint/respawn ownership;
- bank/Material Vault access;
- forge;
- alchemy lab;
- class switching/advancement;
- merchant stock.

This preserves settlements/shrines as meaningful world infrastructure.

## 8.4 Persistence / multiplayer

- camp placement is server-authoritative and saved;
- the owner's active camp may persist across normal save/reload until packed/redeployed;
- nearby eligible players may rest/cook at the physical camp without owning a separate copy;
- using another player's camp does not grant ownership or duplicate the kit;
- party members can share the camp's services, but personal inventories/recovery reserves remain personal;
- redeploying the kit removes the owner's previous camp after a successful new placement;
- no camp can be duplicated through relog/disconnect races.

Camp destruction by random ambient monsters is **not** a baseline maintenance mechanic. An authored event may temporarily threaten a camp only if the encounter itself is the content.

---

# 9. Housing — ownership model

Housing is a physical settlement property system, not an instanced menu-only room and not a full colony builder.

## 9.1 Physical properties

- authored settlements contain explicit purchasable houses;
- the exterior remains part of the real world;
- each property has a stable server-side `property_id`;
- ownership is world/server authoritative;
- a property has one primary owner and optional trusted/co-owner permissions;
- guests may be allowed entry/use by owner permissions;
- the authored shell is protected from destructive structural edits at baseline.

The starting settlement should expose **at least four purchasable starter-house shells** even though the opening visual rule only requires at least two to be visible. This avoids immediate multiplayer scarcity in the intended small-friend-group use case.

No artificial story/reputation permission gate is added to the starter property.

## 9.2 Starter price

Existing economy canon remains:

```text
starter house: 2,400 Gold
starter furnishing/storage package target: 750 Gold
```

The furnishing package sits inside the already-canonical 600–900 Gold band.

The shell can be bought without buying the furnishing package immediately.

## 9.3 Multiple homes / moving

A player may own multiple properties later, but exactly one is marked the **Primary Residence** for UI sorting/house-related convenience.

Owning a second home does not multiply personal storage capacity automatically.

There is no launch need for a real-estate speculation/resale economy. If property resale is later added, it must be a bounded convenience operation and cannot generate profit.

---

# 10. Housing functions

The house should create a satisfying ownership/collection goal without replacing settlement services.

Baseline useful functions:

- rest;
- personal ordinary-item storage;
- furnishing/decor placement;
- boss/collection trophy display;
- Wardrobe/appearance access through an appropriate furniture object if desired;
- optional basic cooking access after installing a kitchen/cooking furnishing.

Not baseline home functions:

- forge;
- full alchemy lab;
- class change/advancement;
- merchant;
- shrine fast travel;
- death checkpoint override;
- material-production automation.

The player still has reasons to walk into the settlement.

## 10.1 Home Storage

Starter furnished home unlocks:

```text
Home Storage: 54 ordinary-item slots
```

Later furniture/service expansion target:

```text
Home Storage expansion: 108 ordinary-item slots total
```

Rules:

- Home Storage is personal server-authoritative storage;
- storage furniture in any owned home accesses the same logical pool;
- placing ten chests does not create ten independent 54-slot pools;
- Home Storage is separate from the settlement Material Vault;
- materials inside Home Storage are not automatically preferred over the Material Pouch/Vault for service crafting unless a future explicit rule says so;
- key/quest items never need Home Storage.

This prevents housing from turning into an infinite chest multiplication exploit while still giving the home a real function.

---

# 11. Furnishing interaction

Housing customization is deliberately lighter than Minecraft creative building.

## 11.1 Protected shell

Baseline owner editing permits:

- place/remove accepted furniture/prop objects in owned interior/property volumes;
- rotate supported furnishings;
- move existing placed furnishings;
- choose from authored material/color variants when the accepted asset family supports them.

Baseline owner editing does **not** freely delete:

- load-bearing walls;
- roof;
- settlement road;
- neighboring building geometry;
- service infrastructure.

This preserves the coherent external settlement architecture and prevents multiplayer grief/visual collapse.

## 11.2 Placement UX

Use direct world placement with a restrained placement overlay:

- ghost preview of the real accepted furniture model;
- valid/invalid placement feedback;
- grid/surface snapping by default;
- 90-degree rotation as the simple baseline, with finer rotation only for furnishings that genuinely benefit;
- cancel returns the furnishing without loss;
- no separate construction currency.

Furniture belongs to normal item/material/economy sources or authored housing packages.

## 11.3 Starter furnishing package

The 750-Gold starter package should visibly complete an otherwise empty starter shell with a coherent minimal set such as:

- bed/rest point;
- storage chest/cabinet access point;
- table + 2 chairs;
- lighting;
- simple shelf/cabinet;
- one trophy/display surface;
- small decor set matching the settlement visual family.

Exact model filenames are asset-intake work, not design invention during coding.

---

# 12. Trophy / collection use

Housing provides a destination for memorable world rewards without adding another combat-stat progression layer.

Examples:

- Regalhart antler display after qualifying discovery/defeat;
- Earthloong trophy/display piece;
- large/rare fish records or mounted/displayed model where an accepted asset supports it;
- region keepsakes.

Rules:

- trophy display is cosmetic/collection-first;
- no hidden +5% damage for placing the correct boss head;
- no mandatory trophy checklist for region completion;
- unlocked trophy appearance/state is server-authoritative and cannot be duplicated for economy profit.

---

# 13. Multiplayer authority / edge cases

## Gathering

Server owns:

- personal node availability;
- resolution time/result;
- yield;
- mastery XP;
- tool-tier validity.

Two players can harvest the same personal node independently.

## Fishing

Server owns:

- spot personal charge/cooldown;
- selected fish/result table;
- hook success validation window;
- tension result;
- reward/mastery.

Client presentation may predict rod/line motion but cannot award fish.

## Camp

Server owns placement legality, owner identity, world position, persistence and service availability.

## Housing

Server owns property ownership, co-owner permissions, furniture state, Home Storage and trophy unlock state.

Never trust client-side `I own this house/node/fish/camp` state for item or Gold changes.

---

# 14. Performance rules

## Resource nodes

- no every-tick global scan for nearby nodes;
- use chunk/region-local authored node data and event-driven interaction;
- personal cooldown checks happen on interaction/visibility update boundaries, not by iterating every player's every node each tick.

## Fishing

- fishing spots are lightweight region objects/markers;
- do not keep large schools of fully pathfinding fish entities active only to provide loot visuals;
- if submerged fish are shown, use bounded/simple client visuals or a small number of ambient entities appropriate to the area.

## Camps / housing

- camp props and furnishings use bounded static rendering/normal block-entity/display strategies appropriate to the chosen asset backend;
- no continuous pathfinding or per-tick UI calculation for decorative furniture;
- enforce reasonable furnishing-count limits based on actual performance measurements rather than an arbitrary tiny cap.

Profiler/playtest decides final density limits.

---

# 15. UI requirements

All four systems reuse the Lucifer-family UI grammar.

## Tool Pouch / mastery

- compact category view for Pick / Axe / Harvest Knife / Rod;
- show current tool tier and one simple Rank I–V mastery indicator per category;
- no skill-tree screen for gathering mastery.

## Fishing

- ordinary common catch requires almost no modal UI;
- rare tension phase uses one compact tension/progress element near the center/bottom combat-safe area;
- no giant opaque minigame panel covering the world.

## Camp

- world ghost placement + small validity reason;
- rest/cook interaction uses existing service components, not a new camp-only design language.

## Housing

- property purchase uses the existing merchant/service component language;
- furnishing mode is world-first with compact controls;
- Home Storage reuses inventory components;
- trophy/collection placement avoids a separate collectible-currency screen.

---

# 16. Data contract

Suggested data ownership:

```text
gathering/
  tool_tiers.json
  mastery.json
  nodes/*.json
  regions/*.json

fishing/
  spots/*.json
  fish/*.json
  regional_tables/*.json

camp/
  field_camp.json
  placement_rules.json

housing/
  properties/*.json
  furnishing_catalog/*.json
  storage.json
```

## Node schema

```text
id
category
region_tags
required_tool_tier
interaction_time
yield_table
personal_respawn
model_source_id
animation_binding
secondary_rolls
```

## Fish schema

```text
id
player_facing_name
region_tags
water_tags
rarity
hook_window
tension_profile
food_or_material_outputs
model_source_id
icon_source_id
conditions
```

## Camp schema

```text
kit_id
owner
position
rotation
deployed
visual_set_id
service_flags
placement_rule_set
```

## Housing schema

```text
property_id
settlement_id
price
owner_uuid
coowners[]
primary_residence
furnishing_volume
furnishings[]
home_storage_profile
trophy_state[]
```

---

# 17. R01 acceptance targets

Before these systems are considered implementation-ready for R01:

1. exact R01 herb/ore/wood node models are bound and scale-reviewed;
2. Pick/Axe/Harvest Knife/Rod models are accepted;
3. `Pickaxing`, `Chopping` and fishing animation chains are retarget-tested against the selected player body/outfit;
4. at least 3 suitable early-river fish models are selected from a legal external fish family and assigned real player-facing identities;
5. the fishing tension UI receives a Lucifer-family visual mock/implementation direction;
6. Kenney Survival Kit exact camp models are acquired/hashed/inspected or replaced with a stronger coherent accepted camp family;
7. one starter-house exterior/interior is visually composed from the accepted settlement/furniture families;
8. starter furniture package models are pinned;
9. all personal node/fishing/camp/housing state has explicit server save ownership;
10. actual Minecraft playtest checks gathering speed, fishing fatigue, camp usefulness and house travel friction before the subsystem is called complete.

---

# 18. What this closes

This document closes design-time rules for:

- Tool Pouch and starting Field tools;
- gathering interaction timing/cancellation;
- three permanent gathering-tool tiers;
- five-rank non-grindy Mining/Herbalism/Forestry/Fishing mastery;
- R01 node timing integration;
- authored fishing spots, personal depletion/respawn and cast/hook/tension loop;
- ordinary vs rare-fish interaction cost;
- no mandatory ordinary bait/junk loop;
- reusable material-built Field Camp Kit;
- camp placement/services/persistence/multiplayer boundaries;
- physical property ownership;
- starter-house price/furnishing package;
- shared logical Home Storage across owned homes;
- lightweight furnishing/trophy rules;
- server authority and performance boundaries.

Remaining work is primarily **external asset intake / R01 fish identity selection / exact source bindings / UI visual acceptance / implementation/playtest**, not permission to invent a different gathering/camp/housing game during coding.
