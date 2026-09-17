# Open-World RPG — R04 Frozen Crown Implementation Package

> Status: **DESIGN CANON — R04 world/story/traversal/service/combat/reward flow is content/mechanics-closed; exact external asset file bindings remain gated where marked**  
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

R04 is written under the regional-story and POI-density rules introduced by `WORLD_STORY_CANON.md` and `DESIGN_COMPLETENESS_AUDIT.md`.

Its play sentence is:

```text
leave the temperate routes for a true expedition frontier
→ navigate by steam plumes, dark rock, waystations and coastline instead of staring at endless white terrain
→ keep outer shelters and routes functioning as storms expose weak points in an old heat network
→ hunt and gather in a cold ecosystem that is still alive rather than an empty snow biome
→ track a giant Iceworm across an exposed basin
→ descend through a newly opened glacial fissure into an old heat-exchange complex now occupied by an Icebroodmother
→ leave with hard evidence that restoring part of the Anchor network can genuinely protect ordinary people
```

R04 is the first region where the player should think:

> `Restoration is not obviously wrong. Some communities really do depend on this.`

That conclusion must come from play and visible consequences, not a lecture.

---

# 1. Locked regional identity

Preserved from `REGIONS.md`:

- terrain: northern ice crown, freezing taiga, ice-spike terrain, frozen coast and ice-ocean edge;
- suggested entry Lv: **20**;
- R04 and R05 are peer expedition-tier regions rather than one being a mandatory prerequisite for the other;
- weather and visibility matter;
- there is **no baseline always-on temperature-survival chore**;
- ecology includes Moose, Seal, Snow Leopard and Froststalker;
- major predator candidates remain Ferox Iceworm and Icebroodmother;
- settlement direction remains an expedition fort / geothermal refuge;
- dungeon direction remains a glacial fissure / frozen nest / buried facility;
- reward identity remains frost/status control, high poise, defensive accessories and anti-slow mobility;
- frozen coastline continues naturally toward R11 maritime content.

Production clarification:

- R04 is **not** a generic `cold damage region` where every enemy deals Frost and every item has Frost Resistance;
- cold shapes navigation, ecology, visual language and selected statuses, while ordinary physical wildlife remains ordinary physical wildlife;
- the region's story does not reveal that Froststalkers, Moose, Seals, Snow Leopards or Ferox Iceworms were created by Anchors.

---

# 2. External game-content precedents

These are structural references only. No proprietary art, numbers, maps or quest content are copied.

## 2.1 Monster Hunter: World — Iceborne / Hoarfrost Reach

Useful precedent:

- a harsh frozen region can still feel ecologically rich rather than empty;
- local monsters use the terrain/environment as part of their identity;
- regional materials, hunts and equipment make the locale mechanically meaningful;
- dangerous cold can strongly shape presentation without the biome becoming only white scenery.

Project adoption:

- R04 has dense **ecological identity** despite harsh terrain;
- apex creatures use snow/ice terrain as part of readable combat;
- gathering, hunting, fishing and regional gear all connect back into existing progression;
- environmental features such as ice shelves, steam vents and snow basins influence encounters.

Explicitly not adopted:

- a mandatory consumable such as `Hot Drink` for routine exploration;
- a constant cold meter that turns every detour into maintenance.

References:

- `https://news.capcomusa.com/lets/browse/monster-hunter-world-iceborne-release-date-new-monsters-new-story-and-features-revealed`
- `https://news.capcomusa.com/lets/browse/from-a-to-z-heres-every-monster-revealed-for-monster-hunter-world-iceborne-so-far`

## 2.2 Genshin Impact — Dragonspine

Useful precedent:

- heat sources, storms and strong environmental landmarks can give one region a distinct exploration language;
- special ice/geothermal-like landmarks can double as navigation and worldbuilding;
- region-specific resources/creatures/quests reinforce identity.

Project adoption:

- R04 uses geothermal plumes, waystations and sheltered route markers as visual navigation language;
- selected severe whiteouts alter visibility and encounter/event conditions;
- authored ice barriers/fissures may change route access in specific POIs.

Explicitly not adopted:

- `Sheer Cold`-style continuously accumulating survival meter;
- routine HP loss for merely existing in the region;
- repeated warming-bottle/torch maintenance;
- disabling ordinary exploration tools simply to make the area inconvenient.

Reference:

- `https://www.hoyolab.com/article/106976`

## 2.3 Horizon — Frozen Wilds / Forbidden West quest and set-piece design

Useful precedent:

- narratively driven side/regional quests can still be built around strong action/systemic gameplay;
- repeated open-world activity formats become more memorable when upgraded into distinctive set pieces rather than copied unchanged;
- environmental puzzles/large world interactions can communicate history through the place itself.

Project adoption:

- R04 contains one large **waystation/heat-route restoration sequence** and one distinctive **glacial-fissure descent**, not ten small switch puzzles;
- regional quests expose story through people, routes, machinery and combat instead of dialogue-only exposition;
- the Icebroodmother complex is a physical story about a failed heat route and newly opened habitat, not a random cave at the end of a quest marker.

References:

- `https://www.gdcvault.com/play/1029126/Game-Narrative-Summit-Pitching-and`
- `https://www.guerrilla-games.com/media/Team/Daniel%20Wewerinke%20-%20GDC24%20-%20Relic%20Ruins%20-%20Creating%20Environmental%20Puzzles.pdf`

## 2.4 The Witcher 3 / Skyrim production lessons

Useful precedent:

- off-main-quest exploration needs its own economy, loot, discovery and progression reasons;
- large open worlds require landmark-led navigation, route loops and iteration rather than terrain size alone.

Project adoption:

- R04 fishing, hunts, winter herbs, fur/hide and crystals feed existing loops;
- major routes repeatedly reconnect to refuge/waystations/shortcuts;
- POIs must satisfy worldbuilding, progression, spatial gameplay and navigation value rather than being `ice ruin + chest`.

References:

- `https://www.gdcvault.com/play/1023867/The-Living-World-of-The`
- `https://www.gdcvault.com/play/1020171/Level-Design-in-a-Day`

---

# 3. External-first source stack

## 3.1 Alex's Mobs Continued — dependency ecology

Current 26.2 Fabric continuation exists and preserves the original creature roster/behaviors.

R04 dependency candidates already aligned with the regional concept include:

- Moose;
- Seal;
- Snow Leopard;
- Froststalker.

Source behavior evidence from the original Alex's Mobs lineage identifies Froststalker as an ice-covered pack-hunting reptile associated with ice-spike terrain.

Project rules:

- use the installed/current project dependency path only after the pinned `M0_DEPENDENCY_AUDIT.md` compatibility boundary is satisfied;
- project data owns Lv, stats, spawn density, drops, encounter eligibility and progression;
- no donor vanilla-style loot table automatically becomes canonical;
- do not copy dependency assets into the public repository merely because the dependency can be installed.

Current storefront license metadata is not perfectly consistent across Modrinth/CurseForge continuation listings, so exact code/asset reuse beyond ordinary dependency use remains governed by project-local provenance review.

References:

- `https://modrinth.com/mod/alexs-mobs-continued`
- `https://www.curseforge.com/minecraft/mc-mods/alexs-mobs-continued`

## 3.2 Threateningly Mobs Continued — dependency boss candidates

Current 26.2 Fabric continuation exists.

R04 candidates:

- **Ferox Iceworm** — source lineage explicitly associates the iceworm with snowland and contact-triggered aggression;
- **Icebroodmother** — source lineage identifies it as an Ice Palace boss;
- **Ice Weaver** — source lineage identifies these as Icebroodmother minions.

Project adoption:

- preserve the strong external creature identities/model work if Minecraft-scale review passes;
- replace donor stats, rewards, progression, default spawn control and structure assumptions with project-authored rules;
- Icebroodmother/Ice Weaver are dungeon-authored, not normal natural-spawn spam;
- Ferox Iceworm becomes an authored field/world-boss hunt instead of uncontrolled giant-worm density.

The existing project policy for Threateningly Mobs licensing remains in force: normal dependency use is separate from copying continuation source/model bytes into the public repository.

References:

- `https://modrinth.com/mod/threateningly-mobs-continued`
- `https://modrinth.com/mod/threateningly-mobs/version/K8PaW2nO`
- `https://modrinth.com/mod/threateningly-mobs/version/1.0.9.9`

## 3.3 Frozen environment / settlement assets

Macro terrain remains Azari/Minecraft terrain and blocks for performance/readability.

External prop/detail candidates:

- KayKit `Forest Nature Pack` — CC0 rocks/trees/terrain-detail family;
- KayKit `Holiday Bits` — CC0 snow/seasonal prop primitives where visually appropriate, but **not** used to turn the region into a Christmas biome;
- Quaternius/KayKit accepted nature families — rock, tree, vegetation and landmark details;
- Quaternius `Medieval Village MegaKit` + `Fantasy Props MegaKit` — expedition refuge/fort architecture and interior/market/inn/work props after source-specific provenance review;
- existing accepted dungeon/ruin modular families — old heat-exchange/Anchor facility details.

Large snow banks, ice cliffs and glaciers are terrain/block composition, not thousands of Display Entities.

## 3.4 Ice Palace / dungeon shell boundary

Threateningly Mobs' donor `ice_palace` is a **candidate reference/dependency structure**, not automatically the final R04 dungeon shell.

Before adoption:

1. inspect architecture and room composition in-game;
2. remove/replace donor progression/loot assumptions;
3. confirm it visually meets the project quality bar;
4. verify it can support the glacial-fissure → old heat facility → brood nest flow below;
5. otherwise preserve Icebroodmother/Ice Weaver as dependency actors and use a stronger external modular dungeon/ruin shell.

Do not keep a weak structure merely because the boss originally spawned there.

---

# 4. Story role — the strongest early case for restoration

R04 is one of the R04–R07 Act-II evidence regions.

Its evidence package argues:

> Some modern communities genuinely rely on surviving Anchor-era infrastructure, and losing it can cause direct human harm even when no one is trying to exploit the system.

Local truth:

- the northern refuge network sits above a natural geothermal zone;
- generations ago, Anchor-era engineering redirected/stabilized part of that heat/water flow into protected route stations and a central refuge;
- modern residents understand the visible pipes/shafts/vents only partially, but they have maintained local parts of the system for years;
- recent instability is causing outer waystations and a protected travel corridor to cool/fail unevenly;
- the central refuge is **not** immediately freezing to death; the problem escalates through route loss, supply risk and isolated expeditions rather than a fake world-ending timer;
- restoring a bounded regional heat branch produces clear benefits;
- later evidence elsewhere can still show why reconnecting every branch into one continental master network is dangerous.

The Icebroodmother is not the cause of the continental crisis.

The failing heat route opens/widens a glacial fissure and changes access to a deep habitat/facility. The brood exploits that new space.

---

# 5. Local people / regional conflict

R04's primary safe hub is a **geothermal refuge / expedition fort** built around a dark-rock/steam basin rather than a generic snowy village.

Normal life should visibly include:

- expedition preparation;
- fish/meat drying and cooking;
- winter herb/alchemy work;
- route maintenance;
- sled/crate/pack handling where external props support it;
- hunters/scouts returning from outer shelters;
- ordinary residents using geothermal water/heat without acting as if every day is an apocalypse.

Core local roles:

## 5.1 Refuge Steward / Quartermaster

- coordinates supplies, shelters and safe routes;
- strongly favors restoring the old heat branch because the practical cost of failure is visible;
- not ideologically loyal to a continental restoration faction at first;
- supplies the human reason the problem matters.

## 5.2 Ice Surveyor / Route Keeper

- reads snow, crevasses, storm patterns and safe coastline routes;
- knows which failures are ordinary weather and which do not make sense;
- introduces the player to whiteout navigation without a temperature tutorial wall;
- can disagree with reckless reactivation while still supporting local repairs.

## 5.3 Healer / Thermal Worker

- ties geothermal water, winter herbs and practical treatment together;
- gives concrete evidence of what reliable heat/water access changes for the refuge;
- does not add another profession tree.

Recurring `Anchor Scholar`, `Engineer / Smith`, `Guild Pathfinder` or `Rival Wanderer` roles may appear when the main story routes through R04.

No reputation bar is required for this local conflict.

---

# 6. Spatial progression / local pressure

Suggested entry Lv remains **20**.

| Sub-area | Local pressure | Role |
|---|---:|---|
| southern frozen pass / treeline transition | Lv 19–20 | readable entry / first weather contrast |
| geothermal refuge basin | Lv 20 | safe hub / services / route orientation |
| outer taiga / first waystation belt | Lv 20–22 | Moose, Froststalker pressure, gathering |
| blue-ice shelf / frozen river cuts | Lv 21–23 | navigation / rare nodes / ambush space |
| frozen coast / seal water | Lv 21–23 | fishing / ecology / R11 foreshadowing |
| deep ice-spike / whiteout pockets | Lv 22–24 | dangerous hunt territory |
| Ferox Iceworm basin | **Lv 24** | optional field/world boss |
| glacial fissure / heat facility dungeon | Lv 23–25 | regional dungeon |
| Icebroodmother | **Lv 25** | dungeon climax / Act-II evidence |

R05 also starts at suggested Lv20, so a player can leave R04 at any time and choose the jungle route instead.

---

# 7. Cold / weather / navigation contract

R04 must feel cold **without turning into survival maintenance**.

## 7.1 No global cold meter

Baseline rules:

- no continuously accumulating `Cold` gauge;
- no passive HP loss for ordinary R04 exploration;
- no mandatory warming food/drink loop;
- no equipment slot tax requiring `cold armor` merely to enter;
- swimming in clearly dangerous freezing water may have an authored local penalty, but ordinary shoreline/fishing access remains usable.

## 7.2 Whiteout fronts

Selected weather states may create short regional whiteouts.

Whiteout effects are primarily:

- shorter visual range / stronger wind-snow presentation;
- different ambient audio;
- selected wildlife/event weighting;
- harder landmark reading in exposed zones;
- special hunt/fishing/gathering conditions where useful.

Whiteouts do **not**:

- tick unavoidable damage;
- drain a separate maintenance resource;
- disable the map entirely;
- reduce movement speed everywhere.

The player can still navigate through:

- geothermal steam columns;
- dark rock ridges;
- waystation lantern/beacon silhouettes;
- coastline orientation;
- marked route poles/stone cairns where external design supports them.

## 7.3 Slippery ice

Use sparingly.

- authored blue-ice slides/floors may appear in one POI or dungeon room;
- the entire region is not a low-friction control penalty;
- boss arenas cannot become unreadable because every movement input slides unpredictably.

---

# 8. Route restoration / regional progression

R04 has **three outer waystation/route nodes**, but the player normally stabilizes **two of three** during the core regional chain.

This creates route choice without pretending the region has branching endings.

Candidate locations:

1. inland taiga shelter;
2. ice-shelf crossing station;
3. frozen-coast supply station.

Each restoration requires reaching/solving a real place:

- hostile encounter;
- broken physical connection;
- exposed machinery;
- route navigation;
- one bounded material/service interaction where justified.

No `collect 3 Heat Tokens` abstraction.

After two are stabilized:

- refuge supply traffic visibly improves;
- one route becomes a reliable shortcut;
- regional merchant/service stock can expand modestly;
- the failing branch pattern points the main investigation toward the deep fissure.

These are strong `WORLD_PERSISTENT` candidates because they are beneficial, readable and late-join safe.

---

# 9. POI package

## Major POI A — Geothermal Crown

Functions:

- macro-navigation landmark visible by steam plume;
- explains why a permanent settlement can exist here;
- service/refuge identity;
- first physical hint of old heat-routing infrastructure.

## Major POI B — Windblind Waystation

Functions:

- whiteout navigation set piece;
- optional rescue/repair event location;
- route shortcut after restoration;
- environmental storytelling through abandoned/iced-over supply infrastructure.

## Major POI C — Blue-Ice Scar

Functions:

- exposed glacial wall / crevasse landmark;
- rare resource and wildlife pocket;
- one authored ice-route traversal moment;
- visual foreshadowing of the deeper fissure.

## Major POI D — Frozen Coast / Seal Reach

Functions:

- Seal ecology;
- cold-water fishing;
- R11 maritime sightline/foreshadowing;
- small expedition/trade story;
- no boss/chest required to justify the place.

## Major POI E — Iceworm Basin

Functions:

- optional Ferox Iceworm field-boss arena;
- visible surface tracks/ice fractures;
- high-value hunt/trophy source;
- broad terrain that supports burrow telegraphs.

## Major complex — Glacial Fissure / Heat Exchange / Brood Nest

Handled as the regional dungeon in §15.

Smaller discoveries may include:

- stranded survey camp;
- frozen waterfall overlook;
- abandoned route poles;
- thermal spring pocket;
- Snow Leopard den signs;
- Froststalker pack trail;
- old coastline marker;
- rare fishing hole / open-water steam pool.

Do not solve each discovery with the same chest.

---

# 10. Settlement / services / housing

Target daytime physical population:

```text
10–14 functional / expedition / guard / service NPCs
5–8 ambient residents / travelers
```

No vanilla villagers.

Baseline services:

- shrine / fast travel;
- inn / geothermal bath/rest presentation where the selected external architecture supports it;
- general expedition merchant;
- bank / Material Vault;
- healer / alchemy service;
- regional hunt / route board;
- cold-water fish and creature-material buyer;
- smith capable of ordinary regional gear service;
- stable hitch / mount convenience;
- property interaction for available homes.

Do not duplicate every class trainer or specialist merely because this is a major region.

## Housing

R04 can introduce the **first optional Large House-class residence** if the actual settlement shell and economy review support it.

Target authored properties:

- 1–2 Town House / insulated-cabin class properties around the existing ~9,000 Gold band;
- **1 Large Lodge-class property** around the existing ~25,000 Gold target band.

Rules:

- housing remains optional;
- one residence at a time remains canonical;
- old home trade-in/resale and safe furniture/trophy migration remain atomic;
- the Large Lodge gains more furnishing/display/storage potential, not combat power;
- do not create a cold-only storage system.

If the actual Azari/refuge geometry cannot support a good Large Lodge shell, move the first Large House opportunity to R05/R06 rather than forcing an ugly building into the fort.

---

# 11. Gathering / regional materials

R04 adds only materials with visible regional justification.

## 11.1 Existing materials retained

- Iron remains usable in ordinary expedition maintenance/crafting;
- Silver from R03 remains useful for selected regional equipment/accessories;
- Hardwood/ordinary materials can appear through merchants/imports without pretending frozen terrain produces every previous resource equally.

## 11.2 Frost-crystal role

R04 needs one visually distinctive cold-region crystal/mineral role for:

- frost/status equipment;
- selected alchemy;
- higher-tier forge recipes;
- later cross-region crafting.

Working behavior:

```text
yield: 1
personal respawn: 15–20 active min
required tool: Refined Pick
locations: blue-ice scar / dangerous fissure / rare exposed pockets
```

**Final player-facing name and model remain gated by external asset intake.**

`Frost Crystal` is a readable working role, not permission to commit a generic recolored amethyst.

## 11.3 Winter-herb role

One rare cold herb supports:

- cleansing / frost-resistance preparations;
- cooking/tonic variants;
- regional contract/economy.

Working behavior:

```text
yield: 1–2
personal respawn: 8–12 active min
tool: Harvest Knife / Sickle
locations: thermal edges, sheltered taiga pockets, not open snow everywhere
```

Final name/model waits for exact accepted external plant asset.

## 11.4 Creature materials

Moose/Seal/Froststalker/Snow Leopard material yields are normalized by project loot tables.

- fur/hide/meat remain real materials/food, not another currency;
- rare predator parts may support one or two regional recipes/gear families;
- no creature should exist solely as a material vending machine.

---

# 12. Fishing / collection

R04 should expand fishing through **cold-water ecology**, not a new minigame.

Use:

- geothermal open-water pools;
- ice-edge river mouths;
- authored ice-fishing holes where the map supports them;
- frozen coast / dark-water inlets.

R04 Fish Codex target:

```text
4–6 regional/shared fish identities
```

Composition target:

- 2 common cold-water catches;
- 1 shared upstream/adjacent-region species;
- 1 uncommon deep/coastal catch;
- 1 rare regional trophy candidate;
- optional sixth species connecting toward R11.

Existing Fish Codex rules remain:

- discovery;
- personal best size;
- Trophy band;
- selling;
- cooking;
- housing display.

No bait tax is added for ordinary R04 fishing.

Exact species names remain gated by accepted external animated fish models.

---

# 13. Ecology / normal encounter roles

R04 must feel like a functioning cold ecosystem.

## 13.1 Moose

Role:

- large neutral herbivore in lower taiga/open snow woodland;
- readable warning/charge when crowded or attacked;
- meaningful meat/hide source but not passive target spam;
- low enough density that groups remain memorable.

## 13.2 Seal

Role:

- passive/neutral frozen-coast ecology;
- primarily visual/resource/ecological value;
- no fake combat kit is invented merely because every entity needs `content`.

## 13.3 Snow Leopard

Role:

- rare solitary territorial predator near rocky/upper transitions;
- continues the R03 foreshadowing into its stronger native habitat;
- uses stalking/pounce readability;
- not a common pack mob.

## 13.4 Froststalker

Primary hostile roaming threat.

External identity:

- Alex's Mobs lineage: ice-covered pack-hunting reptile from Ice Spikes.

Project behavior target:

- 2–4 animal packs at bounded authored/natural density;
- one member may pressure/flank while another prepares a readable rush;
- no 8-mob permanent stunlock pack;
- clear frost-crack/body posture before committed lunge;
- pack pursuit breaks when the player leaves territory far enough.

Working combat band:

```text
Lv 21–23
role: dangerous common / light elite depending pack composition
individual solo TTK: ~5–8 s
```

Exact stats wait for direct 26.2 in-game behavior inspection because donor AI/pathing strongly affects pressure.

## 13.5 Ice Weaver

Dungeon-only / authored brood minion.

- used inside the Icebroodmother complex;
- hard-capped active count;
- never becomes a region-wide spider infestation;
- exists to support boss/nest identity, not to add another natural-spawn table.

---

# 14. Ferox Iceworm field/world boss

Working role:

```text
Lv: 24
role: optional field/world boss
HP target: ~13,000–15,000
Defense: ~56
MR: ~44
Poise: 215
solo active TTK target: ~200–225 s
```

Final HP can move after actual burrow downtime is measured. Do not compensate for long untargetable behavior by simply adding HP.

## 14.1 Arena

- wide snow/ice basin with dark rock/blue-ice landmarks;
- enough open floor to read underground travel lines;
- no tiny invisible arena ring;
- edge crevasses may exist as authored danger, not random death pits around every dodge.

## 14.2 Burrow contract

The Iceworm can burrow, but melee downtime is tightly controlled.

- ordinary burrow/reposition phase should normally remain **<=4–5 s**;
- underground path must show a visible moving snow/ice disturbance;
- final emergence direction locks early enough to dodge/read;
- player is not expected to guess from audio alone;
- repeated burrows cannot chain into 20 seconds of invulnerability.

## Attack 1 — Mandible Sweep

```text
wind-up: 0.45–0.60 s
wide close sweep
benchmark damage: ~13–15%
guardable: true
perfect_guardable: true
recovery: ~0.45 s
```

## Attack 2 — Burrow Eruption

```text
visible travel line / mound
final emergence tell: >=0.85 s
impact radius: ~3.5–4.0 blocks
benchmark damage: ~28–32%
guardable: false
perfect_guardable: false
recovery: >=1.10 s
```

The visible snow/ice eruption must match the server hit radius.

## Attack 3 — Body Crush / Tail Reversal

Only lock the exact body part after donor animation review.

```text
telegraph: ~0.70 s
damage: ~22–25%
knockback: moderate
guardable: true
perfect_guardable: true
recovery: ~0.75 s
```

Purpose: punish sitting permanently on one safe body angle.

## Optional ranged/frost action

Do **not** invent a projectile/breath attack unless current donor animation/model inspection provides a visually credible base.

If no such animation exists, the boss remains a high-quality burrowing/body-pressure encounter instead of gaining generic blue particles to check a `ranged attack` box.

## High-pressure state

Below ~40% HP:

- one burrow pattern may create a second delayed surface trace;
- emergence remains readable;
- no permanent haste/damage steroid;
- grounded punish windows stay real.

## Rewards

First eligible defeat follows field-boss canon:

- guaranteed Superior+ R04 equipment roll;
- 2 model-linked signature materials after exact anatomy/model intake;
- 15% direct Mythic/signature roll;
- EXP ~20% current next-Lv requirement;
- Class XP ~15% current Class Rank requirement.

Repeat follows normal field-boss rules.

No final signature-material name before direct model inspection.

---

# 15. Regional dungeon — Glacial Fissure → Heat Exchange → Brood Nest

Target first-clear wall-clock length:

```text
~22–32 minutes
```

This dungeon must not be `R01 cave but blue`.

## Stage 1 — Newly opened fissure

- broken surface route / rescue aftermath;
- visible glacier layers and recent ice movement;
- one optional cold-resource branch;
- route alternates between narrow crevasse and one broader chamber.

## Stage 2 — Geothermal under-ice river

- visible steam/water proves heat still moves below the glacier;
- traversal around bridges/ice shelves, not prolonged awkward swimming;
- environmental evidence connects the natural geothermal zone to constructed old conduits.

## Stage 3 — Old heat-exchange facility

- first clear practical Anchor-era infrastructure;
- valves/shafts/manifolds are visual storytelling, not a giant technical UI puzzle;
- one major authored interaction opens a safer route and demonstrates that restoring a local branch has an immediate physical effect.

## Stage 4 — Brood boundary

- facility damage/new fissures expose the nest;
- Ice Weavers appear here, not throughout the whole dungeon;
- egg/web/nest presentation uses the actual accepted dependency model/VFX language where possible;
- no generic cobweb-filled vanilla mineshaft substitute.

## Stage 5 — Icebroodmother chamber

- broad natural/constructed hybrid arena;
- enough floor for add management without constant wall collision;
- thermal vents/old machinery may provide visual phase cues but are not gimmick buttons required every 20 seconds.

## Shortcut / repeat rule

First clear opens one central heat-service lift/tunnel/route back toward the entrance.

Repeat clears:

- preserve the major shortcut;
- remove long first-clear investigation interactions;
- maintain combat/resource reasons to return without replaying exposition.

---

# 16. Icebroodmother dungeon boss

Working role:

```text
Lv: 25
role: dungeon boss
HP target: ~11,500–13,000
Defense: ~50
MR: ~62
Poise: 210
solo active TTK target: ~170–195 s
```

Exact values wait for direct 26.2 donor-animation and minion-pressure review.

Combat identity:

- brood control / territorial pressure;
- deliberate add windows rather than constant spawn spam;
- clear large-body melee tells;
- one visually honest frost/web space-control pattern if the donor model/VFX can support it;
- meaningful solo and co-op readability.

## Attack 1 — Foreleg / Mandible Combo

Final limb naming follows actual model anatomy.

```text
initial tell: 0.45–0.60 s
1–2 hit readable sequence
total benchmark damage budget: <=22%
guardable: true
perfect_guardable: true
```

## Attack 2 — Brood Call

Source lineage already establishes Ice Weaver minions.

Project rule:

```text
spawn: bounded small wave
active Ice Weaver hard cap: 3–4 solo, modestly higher in multiplayer only if readability remains good
next call unavailable while previous wave is mostly alive
```

Boss does not become invulnerable during the entire add wave.

Adds provide target-priority pressure, not a mandatory trash-clear intermission.

## Attack 3 — Frozen Line / Web Zone

Only finalize after model/VFX review.

Target design if supported:

- one or two clearly drawn floor lanes/patches;
- buildup/slow or bounded Frostbite interaction rather than huge damage;
- visible lifetime and boundary;
- no nearly invisible white-on-white hitbox.

If the accepted asset cannot sell web/frost casting, replace this action with a physical rush/body-slam pattern that the animation can honestly support.

## Attack 4 — Broodmother Crush

```text
telegraph: >=1.00 s
large committed body impact
benchmark damage: ~30–34%
guard pressure: heavy
perfect_guardable: true if the final animation has a clear impact frame
recovery: >=1.20 s
```

## Phase behavior around ~55%

- nest activity/arena treatment changes visibly;
- Brood Call gains a different composition or position rather than simply more HP/damage;
- one thermal/facility element can expose a temporary punish lane or reduce brood pressure;
- no long invulnerable cinematic phase.

The environment may help communicate the fight, but victory does not require cycling identical switches repeatedly.

## Status direction

- Frost-related control should not make the boss immune to every frost build; use high buildup threshold/resistance rather than blanket immunity unless model/lore truly requires it;
- Poison/Bleed resistance is decided after anatomy/model review;
- Fire does not automatically receive a giant `ice boss = fire weakness` multiplier;
- exact weaknesses remain data-authoring work after visual/encounter inspection.

## Rewards

First eligible clear:

- guaranteed Superior+ R04 equipment roll;
- 2 model-linked signature materials;
- 15% direct Mythic/signature roll;
- standard first-clear dungeon EXP/Class XP;
- choose 1 of 3 curated Superior candidates appropriate to R04.

Completion-choice role spread:

1. defensive / anti-stagger or shield-oriented option;
2. frost/status-control or WIL/INT-support option;
3. mobility / anti-slow / precision option.

Exact names/models wait for accepted external equipment assets.

---

# 17. Regional quest / discovery flow

Quest names below are working player-facing candidates and may improve during narrative polish without changing their functions.

## `The Last Warm Road`

Purpose:

- arrive at the refuge;
- see that one outer shelter/route has failed;
- meet local people before the continental ideology arrives;
- establish the practical cost of losing heat-route infrastructure.

No long tutorial chain.

## `Lights in the White`

Purpose:

- investigate/stabilize two of three outer waystations in flexible order;
- use real route navigation, encounter and repair contexts;
- make whiteout landmarks meaningful.

## `Heat Beneath Ice`

Purpose:

- show that failing route heat is connected to an older buried system;
- Scholar/Engineer can recognize Anchor-era design;
- the player sees a local branch restoration help the refuge immediately.

## `The Opened Fissure`

Purpose:

- regional dungeon;
- discover why the branch failed and what the old system actually did;
- defeat Icebroodmother;
- complete one valid R04–R07 Act-II evidence package.

## Iceworm hunt discovery

Purpose:

- optional field-boss route;
- begins from surface tracks/fractures/sightings;
- never blocks the regional dungeon or main-story progression.

Foreground HUD still follows:

```text
1 main objective + up to 2 manually pinned optional objectives
```

---

# 18. Dynamic events

Use a restrained pool. R04 is not an MMO snow-event conveyor belt.

Candidate event families:

## Stranded Expedition

- triggered at selected route locations;
- rescue/escort distance remains short and authored;
- contribution includes combat/support/interactions;
- after enough regional restoration, event weighting can shift to supply/return variants rather than pretending nothing changed.

## Froststalker Crossing

- pack pressure near a travel route;
- can be bypassed, fought or approached from another route;
- not a mandatory kill-everything popup.

## Whiteout Signal

- short navigation/search event around a known waystation radius;
- landmark/sound/track reading rather than following a giant floating arrow;
- failure simply resets/cools down, no extra punishment.

No real-world daily schedule or event currency.

---

# 19. Regional outcome / memory

After the regional chain/dungeon:

Shared late-join-safe consequences may include:

- two stabilized waystations remain lit/active;
- one protected supply route becomes visibly occupied/maintained;
- refuge ambient NPC traffic/supply props improve modestly;
- selected merchant/food/fish stock expands;
- the opened dungeon shortcut remains available according to normal dungeon reset rules.

Personal consequences:

- R04 evidence package recorded;
- local NPC dialogue flags;
- Iceworm discovery/clear state;
- Icebroodmother first-clear/reward state;
- regional discoveries and Fish Codex records.

The region remains cold and dangerous afterward. The player improves **human access and reliability**, not the climate itself.

---

# 20. Reward identity

No R04-specific token currency.

Regional item/affix emphasis:

- Frostbite / chilled / slow handling;
- status buildup resistance and cleanse utility;
- poise / stagger resistance;
- defensive accessories;
- guard stability;
- anti-slow / movement recovery;
- selected WIL/END sustain;
- bounded frost/status offense.

Avoid making every item only useful inside R04.

A strong R04 reward should remain relevant in later control-heavy encounters, R11 cold-water/deep routes or frost/status builds.

---

# 21. Audio / presentation contract

R04 is the first later-region package to explicitly lock an audio-state structure because the design audit identified global music/sound coverage as a remaining gap.

Required regional audio states:

1. calm exposed snowfield / taiga ambience;
2. whiteout / high-wind ambience;
3. geothermal refuge warmth/interior ambience;
4. glacial fissure / ice-groan ambience;
5. old heat-facility mechanical/steam ambience;
6. Ferox Iceworm encounter music/state;
7. Icebroodmother dungeon/boss music/state.

Rules:

- wind must not be a permanently loud broadband hiss that exhausts the player;
- refuge should sound materially warmer/denser than exposed ice terrain;
- ice crack/groan cues may support navigation/telegraphs but cannot be the only cue for lethal attacks;
- important boss impacts need dedicated sound layers synchronized to server-confirmed attack events;
- external-first BGM/SFX sourcing remains required before implementation acceptance.

No player-facing temporary stock-beep/debug sound is accepted as final presentation.

---

# 22. Multiplayer / authority

Server owns:

- waystation restoration state;
- route/shelter world state;
- whiteout event state relevant to gameplay;
- boss encounter state;
- Iceworm burrow position/hit validation;
- Icebroodmother minion caps/spawn authority;
- dungeon shortcut state;
- gathering/fishing personal state;
- first-clear/repeat reward eligibility;
- all items/EXP/Gold/progression.

Later multiplayer tests must include:

- one player stabilizing a shared waystation does not erase another player's personal interaction/quest credit;
- late join sees restored route but can still complete personal evidence flow;
- split-party players can work on different waystations safely;
- Iceworm underground position cannot desync from visible mound/eruption;
- Icebroodmother minion counts remain bounded with two or more players;
- disconnect during first-clear pending reward does not duplicate or lose reward;
- whiteout event state remains coherent across clients.

`MULTIPLAYER TESTED` remains NO until real clients are used.

---

# 23. Performance constraints

- no full-region weather/waystation scans every tick;
- whiteout controllers operate by region/weather state and loaded areas;
- distant geothermal steam is bounded visual presentation, not hundreds of ticking particle emitters;
- Froststalker pack logic uses normal local entity AI, not global pack searches;
- Ferox Iceworm boss AI only runs inside active encounter scope;
- Ice Weaver minions have strict active caps;
- frozen-coast aquatic/fish presentation must not create large permanent pathfinding schools;
- glacial facility effects stop when unloaded;
- use profiler/playtest to set final ambient entity/VFX density.

---

# 24. R04 implementation / intake gates

R04 is design-closed enough that implementation should not invent its gameplay loop, but production still requires external-first intake for specific visible assets.

Before player-facing implementation is accepted:

- inspect current 26.2 Moose / Seal / Snow Leopard / Froststalker models, scales, AI and animations;
- inspect Ferox Iceworm model, hit volume, burrow motion and available animations;
- inspect Icebroodmother / Ice Weaver model/animation/VFX support;
- inspect donor Ice Palace structure before deciding whether any of it is retained;
- select exact frost-crystal and winter-herb models/icons;
- select exact R04 fish models;
- bind expedition/refuge NPC outfits and architecture;
- bind waystation/route props;
- bind geothermal/ice/whiteout VFX;
- bind regional ambient/boss audio sources;
- record source-specific license/provenance and hashes for any committed external bytes;
- verify all important visuals at Minecraft scale.

Do not fill a missing asset gate with a vanilla polar bear, vanilla spider, generic blue crystal, recolored leather armor or particle-only boss substitute.

---

# 25. Acceptance targets

A later playable R04 is not accepted because the quests technically complete.

Required playtest questions:

## Exploration

- can the player orient in snow using real landmarks without constant map staring?
- does whiteout create atmosphere/choice without becoming irritating?
- are meaningful discoveries encountered at the cadence defined by `WORLD_STORY_CANON.md`?
- does the region feel ecologically alive rather than empty white terrain?

## Regional story

- does the player understand why residents value restoring the heat branch before the Scholar explains the ideology?
- does the local repair visibly improve life/navigation?
- does R04 provide a sincere restoration argument without declaring Restore the correct ending?

## Combat

- are Froststalker packs readable rather than stunlock swarms?
- is Ferox Iceworm reachable/punishable often enough for melee builds?
- do Iceworm burrow trails match server position?
- does Icebroodmother add pressure stay bounded?
- do white/blue VFX remain visible on snow/ice backgrounds?

## Economy / side loops

- do fishing, herbs, creature materials and crystal routes feed existing systems?
- are R04 materials useful beyond one regional recipe?
- is the optional Large Lodge a plausible savings target rather than a mandatory tax?

## Presentation

- does the refuge feel visually/audibly warm compared with the exterior?
- do weather/audio changes improve atmosphere without fatigue?
- are external models/architecture/UI coherent rather than a collage?

## Technical

- save/load preserves restored waystations and personal evidence;
- late join remains safe;
- no reward duplication;
- no chunk-unload boss/route corruption;
- regional weather/VFX/entity load meets profiler targets.

Verification state at design authoring time:

```text
DESIGN REVIEWED: YES
EXTERNAL REFERENCE REVIEWED: YES
CODE REVIEWED: N/A
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
