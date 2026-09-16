# Open-World RPG — R06 Mirewater Delta Implementation Package

> Status: **DESIGN CANON — R06 world/story/traversal/service/combat/reward flow is implementation-ready; exact dungeon-boss asset remains an explicit external-intake gate**  
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

R06 is the first full midgame region where **water and land interlock continuously**. The challenge is reading channels, sightlines and predator territory — not fighting the movement system.

Its play sentence is:

```text
enter a delta where roads become boardwalks, levees and shallow channels
→ learn the raised settlement and alchemist trade network that lives with changing water
→ choose dry causeways, skiff/ferry links and shallow routes while keeping landmarks in sight
→ hunt, fish and gather from a wetland ecosystem where predators use water edges differently
→ discover that old Anchor-era waterworks keep trying to force the delta into obsolete channels
→ confront a Hydra in a flooded basin where water position matters but melee builds remain viable
→ descend into a sunken observatory / flow-regulator shrine
→ split local water control into bounded regional branches instead of fully restoring or destroying it
→ leave with evidence for Regional Stewardship: useful infrastructure can survive without one continental control system
```

R06 should make the player think:

> `The answer may not be restore everything or destroy everything. Local systems can be kept without recreating the old single point of failure.`

This is evidence for the Partition philosophy, not a hidden declaration that Partition is the best ending.

---

# 1. Locked regional identity

Preserved from `REGIONS.md`:

- terrain: eastern river delta, wetland and swamp mosaic;
- suggested entry Lv: **30**;
- water and land repeatedly interlock;
- difficulty comes from positioning, line-of-sight and route choice rather than permanent movement slowdown;
- boardwalks, shallow channels, ruins and giant trees are navigation landmarks;
- ecology includes Crocodile, Caiman, Alligator Snapping Turtle, Shoebill/Mudskipper-style ambience, Anaconda and Diplocaulus;
- Hydra is the signature major predator / field-boss direction;
- settlement direction: raised-platform wetland town / alchemist enclave;
- resources: wetland herbs, poison reagents, clay, wetland fiber and river resources;
- dungeon direction: flooded observatory / sunken shrine;
- reward identity: poison/bleed control, healing amplification, water mobility, anti-grab and alchemy accessories.

Production clarifications:

- R06 has **no baseline swamp-fatigue meter**, universal slow, disease meter or constant forced swimming;
- water depth is authored to change tactics and route choice, not to make ordinary melee combat dysfunctional;
- Crocodile/Caiman/Hydra are natural regional predators, not all explained as Anchor corruption;
- old waterworks change channels and flood pressure but do not control every raindrop in the delta;
- the exact dungeon-boss creature remains gated until a strong external animated model passes intake.

---

# 2. External game-content precedents

These are structural references only. No proprietary maps, art, scripts or balance numbers are copied.

## 2.1 Monster Hunter: World — Wildspire Waste wetlands

Capcom's Wildspire Waste is useful because one ecosystem combines swamp water and dry ground instead of treating them as unrelated biomes.

Useful lessons:

- wetland and dry routes can sit inside one readable ecosystem;
- monster behavior and body design can reflect mud/water use;
- ecological transitions create encounter variety without needing another subsystem meter;
- terrain changes which angles and spaces are safe.

Project adoption:

- R06 alternates levees, reed flats, shallow basins and raised land within one region;
- wetland predators use shorelines, shallows and cover as authored behavior spaces;
- the player repeatedly returns to dry or raised footing instead of spending long stretches swimming;
- environmental state affects encounter geometry without applying arbitrary stat penalties everywhere.

Not adopted:

- mud buildup/status on every ordinary fight;
- giant empty flats with content only at the edges;
- mandatory environmental consumables.

Reference:

- `https://news.capcomusa.com/lets/browse/new-ecosystem-revealed-for-monster-hunter-world-the-wildspire-waste`

## 2.2 The Witcher 3 — Velen / environment-production lessons

Useful production lesson:

- settlements, professions and POIs should arise from terrain and livelihood rather than looking randomly placed;
- environment, quest and level-design teams iterate locations together because gameplay, narrative and world logic all affect the same place;
- swamp mood is strongest when human life, danger and history visibly coexist.

Project adoption:

- R06 settlement jobs visibly depend on fishing, herb drying, ferrying, clay, raised construction and route maintenance;
- major POIs carry both local history and gameplay value;
- swamp atmosphere may be damp, dense and eerie without turning the whole game grimdark;
- unsafe flooded ruins exist alongside normal working communities so the region feels lived-in, not abandoned.

Not adopted:

- misery as the only emotional identity;
- marker saturation or repeated identical water caches;
- forcing the player through every bog to reach the main road.

Reference direction:

- `https://www.gamebanshee.com/news/116158-the-witcher-3-wild-hunt-environment-design-interview.html`

## 2.3 Zelda / open-world route readability

Useful precedent:

- water, paths, high ground and landmark silhouettes can act as natural navigation grammar;
- optional discoveries are stronger when visible from route edges or hinted through environmental cues;
- routes should reconnect so choosing one path does not create long backtracking punishment.

Project adoption:

- giant cypress-like trees, watch platforms, shrine towers, smoke/lanterns and levee silhouettes anchor orientation;
- channels function as edges/pathways rather than blue walls;
- dry and wet approaches frequently reconnect near meaningful POIs;
- regional map markers reflect legitimate discoveries instead of automatic GPS for every herb patch.

Not adopted:

- universal climb-anything physics;
- disposable micro-shrine density;
- physics-sandbox requirements unrelated to this RPG.

## 2.4 Open-world quality lesson — authored variety over icon density

The project keeps the design-audit rule that markers are a promise of varied authored content.

R06 therefore avoids the classic failure mode:

```text
30 swamp icons
→ same sunken chest
→ same two crocodiles
→ same herb bundle
```

A smaller number of strong wetland POIs is preferred over a dense copy-paste map.

---

# 3. External-first source stack

## 3.1 Alex's Mobs Continued — wetland ecology dependency

Current 26.2 Fabric continuation exists and carries the original creature roster forward.

Strong R06 dependency candidates include:

- Crocodile;
- Caiman;
- Alligator Snapping Turtle;
- Shoebill;
- Mudskipper;
- Diplocaulus;
- Anaconda where the current behavior remains stable.

Project rules:

- project data owns Lv, HP, damage, spawn density, loot and region placement;
- ambient creatures are not converted into combat targets solely for content count;
- global donor spawn rules are filtered into authored R06 ecology;
- current 26.2 behavior is inspected before exact combat timing is locked;
- continuation storefront license metadata has shown conflicting GPL/LGPL labels, so ordinary dependency use stays distinct from copying source/assets into the public repository.

Current reference:

- `https://www.curseforge.com/minecraft/mc-mods/alexs-mobs-continued`

## 3.2 Threateningly Mobs Continued — Hydra

Current 26.2 continuation explicitly lists **Hydra** in `oceans, rivers and swamps`.

This makes Hydra a strong source-fit R06 field-boss dependency rather than an invented biome transplant.

Project use:

- Hydra appears only in authored high-pressure basin/flooded-ruin encounters;
- it does not become common swamp population;
- project stats/rewards/state own the encounter;
- donor global progression/structures are not inherited automatically;
- exact source/model bytes may be reused only under the verified project provenance/license path; ordinary dependency use remains sufficient for gameplay.

Current reference:

- `https://modrinth.com/mod/threateningly-mobs-continued`

## 3.3 Wetland settlement / boat / prop direction

Use the already-accepted external-first approach:

- Kenney `Watercraft Kit` for small skiff/ferry visual candidates where scale fits;
- Kenney `Pirate Kit` only for neutral dock/raised-platform primitives, with pirate-specific symbols removed;
- Quaternius/KayKit accepted nature/prop families for reeds, plants, tools, baskets, drying racks, jars and market detail under their exact source-specific licenses;
- macro boardwalks, levees, mudbanks and giant trees remain performant world/block composition rather than thousands of Display Entities.

The settlement should look like a **working wetland town**, not a pirate reskin and not R05 river market with greener water.

## 3.4 Dungeon-boss external gate

R06 needs a dungeon climax distinct from the outdoor Hydra.

Required role:

- ancient water/flow guardian or wetland-adapted creature occupying the observatory/regulator core;
- visually capable of attacks that interact with lanes, gates, rising/falling shallow water or anchored platforms;
- not another multi-headed Hydra, not a giant crocodile, not a vanilla guardian and not an enlarged slime.

Candidate source pools:

1. accepted Quaternius animated monster families after direct model comparison;
2. current QAL Bestiary/Dungeon Monsters candidates if visual identity fits and animation retarget quality passes;
3. another redistributable animated aquatic/guardian model found during intake.

Status:

```text
DUNGEON BOSS VISUAL IDENTITY: OPEN ASSET GATE
DUNGEON SPATIAL/MECHANICAL ROLE: LOCKED
```

Do not lock anatomy-specific weak points or final player-facing boss name before model intake.

---

# 4. Story role — the strongest early case for regional stewardship

R06 is one of the optional R04–R07 Act-II evidence regions.

Its evidence package argues:

> Ancient infrastructure can still be useful, but reconnecting it as one continental system is not required. A region can keep bounded local control and deliberately isolate cascade risk.

Regional history:

- old Anchor-era works divided the delta into controllable channels for transport, flood protection, freshwater storage and observation;
- later administrators increasingly synchronized those gates with distant continental predictions;
- after the network degraded, residents kept only a few visible local gates/levees working and adapted around natural seasonal variation;
- recent network instability sends conflicting remote commands/pressure signals into surviving buried control chambers;
- some channels are opening when local residents need them closed and vice versa;
- full restoration would improve large-scale predictability but would reconnect R06 to the same continental cascade risk revealed elsewhere;
- full destruction would remove useful local flood protection and freshwater control;
- the regional solution is to isolate the local controller, retain selected safe gates, and disable remote coupling.

This provides a practical model for Regional Stewardship without declaring it risk-free or universally correct.

---

# 5. Local people / regional conflict

Primary hub: **raised-platform wetland settlement / alchemist enclave** built around several stable high-ground mounds and linked boardwalks.

Normal life should visibly include:

- ferry/skiff loading;
- fish cleaning/smoking;
- herb washing/drying;
- alchemy jars, presses and stills;
- clay pottery / wetland fiber weaving;
- raised homes with flood marks;
- boardwalk repair crews;
- watch platforms scanning channels for large predators;
- children/ambient residents using safe inner platforms rather than wandering in hostile water.

Key local roles:

## 5.1 Waterwarden / Gate Keeper

- maintains visible levees, boardwalks and local gate mechanisms;
- values the old works because they prevent real flood damage;
- opposes blindly re-linking remote controls they cannot verify;
- naturally represents the regional-stewardship perspective without becoming a political lecturer.

## 5.2 Alchemist / Wetland Healer

- depends on seasonal herbs, clean-water pools and creature reagents;
- notices ecological changes before engineers do;
- connects gathering/fishing/reagent loops to the regional story;
- helps distinguish natural poison/venom ecology from Anchor anomalies.

## 5.3 Ferry Master / Reed Guide

- teaches practical route logic through channels and raised ground;
- owns regional ferry/skiff service where used;
- points out safe/unsafe water lines without filling the HUD with arrows;
- provides a believable connection to R11 coastal/maritime routes later.

Recurring Engineer/Smith, Anchor Scholar, Cartographer/Ranger or Rival Wanderer may appear when the main story routes through R06.

---

# 6. Spatial progression / local pressure

Suggested entry Lv remains **30**.

| Sub-area | Local pressure | Role |
|---|---:|---|
| western approach / dry levees | Lv 29–30 | readable transition / first wetland routes |
| raised settlement / inner channels | Lv 30 | hub / services / fishing / alchemy |
| reed flats / boardwalk belt | Lv 30–32 | common ecology / gathering / route choice |
| flooded orchard / giant-tree basin | Lv 31–33 | predator pressure / discoveries |
| old gateworks / ruined levees | Lv 32–34 | regional problem / elite encounters |
| Hydra basin / flooded ruin | **Lv 35** | optional field boss |
| sunken observatory / regulator dungeon | Lv 33–36 | regional dungeon |
| dungeon guardian target | **Lv 36** | dungeon climax |

R07 begins at suggested Lv34, so a player can branch into desert content before fully clearing R06.

---

# 7. Traversal contract — wet without being slow

R06 has four ordinary movement surfaces:

### Raised ground / levees

- fastest normal foot/mount routes;
- long sightlines and major landmarks;
- should remain viable for players who dislike water travel.

### Boardwalks / bridges

- connect settlement and key POIs;
- narrow enough to create encounter angles but not endless balance-beam walking;
- damaged sections may create optional alternate routes, not mandatory parkour chores.

### Shallow water / reed flats

- mostly knee/waist-depth authored spaces where ordinary combat remains functional;
- movement penalty, if any, is mild and spatially obvious;
- shallow lanes create flanking/line-of-sight choices rather than a universal slow debuff.

### Deep channels

- used for fishing, ferries/skiffs, predator boundaries and selected shortcuts;
- ordinary mandatory combat does not force players to swim for minutes;
- deep-water enemies may threaten crossings, but every normal progression route has a readable alternative.

## 7.1 Ferry/skiff service

If external model/implementation review passes, R06 may use short regional ferries or player-directed skiffs as **local transit**, not a new progression tree.

Rules:

- no fuel economy;
- no boat durability;
- no separate boat inventory;
- no naval combat requirement;
- disembark points connect real content/POIs;
- ferries never replace the later R11 maritime layer.

If small-boat handling proves clumsy in Minecraft geometry, fixed ferries/boardwalk routes are preferred over shipping a bad vehicle system.

---

# 8. Navigation / landmark language

Swamps easily become visually flat. R06 therefore needs repeated vertical anchors.

Use at least:

1. **raised settlement tower / lantern mast**;
2. **giant pale wetland tree or cypress silhouette**;
3. **old gatehouse / levee tower**;
4. **sunken observatory crown / broken dome visible above water**;
5. **Hydra basin ruins / stone ribs** visible from several routes;
6. one distant R07 dry ridge/desert color break where geography permits.

Rules:

- main routes repeatedly reveal one or more anchors through reed breaks;
- mist may reduce long-distance visibility temporarily but cannot erase all navigation references for long periods;
- no permanent swamp fog wall;
- boardwalk signs, colored cloth, lanterns and platform geometry teach settlement-safe routes naturally.

---

# 9. Weather / water-state rules

R06 uses authored **water-state variation**, not continuous simulation of the entire delta.

Heavy rain / event states may:

- raise selected local shallow basins visually;
- activate a few alternate fishing spots;
- change creature/event weights;
- make certain old gates leak or overflow;
- alter one or two POI approaches.

They do **not**:

- recompute the whole world hydrology every tick;
- erase quest routes unpredictably;
- flood player housing;
- force real-time waiting for a specific weather state to progress;
- apply a permanent wet/debuff tax.

Dungeon water states are authored encounter states controlled by the server, not free-running fluid puzzles.

---

# 10. POI package

## Major POI A — Raised Wetland Town

Functions:

- social/service hub;
- strong navigation anchor;
- visible alchemy/fishing/ferry economy;
- local stewardship debate grounded in real work.

## Major POI B — Broken Floodgate Causeway

Functions:

- major route split;
- visible old infrastructure;
- one repair/isolation world-state change;
- combat + environmental evidence + shortcut value.

## Major POI C — Giant-Tree Basin

Functions:

- strong natural landmark;
- Diplocaulus/amphibian/wetland ecology;
- rare herb/fishing route;
- one optional event/hunt space;
- no chest required.

## Major POI D — Drowned Orchard / Clay Shelf

Functions:

- gathering/economy source;
- shows shifting local water lines through old field walls/flood marks;
- supports clay/fiber/herb resources;
- compact predator encounter space.

## Major POI E — Hydra Basin

Functions:

- optional field-boss arena;
- half-flooded ruin with several dry islands/ledges;
- natural predator territory rather than an Anchor altar;
- signature hunt/trophy/reward source.

## Major complex — Sunken Observatory / Flow-Regulator Shrine

Handled in §17.

Smaller discoveries may include:

- abandoned reed-fisher huts;
- flood-height stones;
- snapping-turtle nesting shelf;
- shoebill feeding ground;
- collapsed local medicine dock;
- old ferry bells;
- half-submerged survey marker;
- rare fish pool;
- isolated clean-water spring.

Do not convert them into a repetitive `loot chest in water` checklist.

---

# 11. Settlement / services / housing

Target daytime physical population:

```text
8–12 functional/service/guard NPCs
5–8 ambient residents / fishers / alchemists / travelers
```

Baseline services:

- shrine / fast travel;
- inn/rest/food;
- regional merchant;
- Material Vault/bank access;
- strong alchemy/healing service;
- fishing/ingredient buyer;
- wetland contract / route board;
- ferry/skiff service where accepted;
- basic smith repair/commerce, but not necessarily the strongest forge in the game.

R06 is the first region where alchemy presentation should feel culturally central, but it does **not** introduce another profession tree.

## Housing

R06 may offer:

- 1–2 raised **Town House-class** wetland properties;
- optionally the first visually spacious **Large House-class** shell only if the actual settlement/map provides a convincing high-ground property and the economy supports the existing ~25,000 Gold band.

No floating/boat home baseline.

Property exterior/support structure remains protected; interior furnishing follows the existing housing canon.

---

# 12. Gathering / production package

R06 adds a small number of justified materials rather than a swamp-token economy.

## Existing materials retained

- Healing Herb persists where ecologically sensible;
- Hardwood/resin may appear around giant-tree belts at lower density;
- ordinary fish continue across regional boundaries where species models fit.

## New role — Wetland Fiber

Purpose:

- light armor/furnishing/utility crafting;
- selected bow/grip/bandage-style production;
- local trade good.

Baseline:

```text
yield: 1–2
personal respawn: ~6 active min
tool: Harvest Knife/Sickle
source: authored reed/fiber stands, not every grass block
```

Final player-facing plant/model waits for external node intake.

## New role — Mire Herb / Antitoxin plant

Purpose:

- alchemy / cleanse / poison-management recipes;
- regional contracts;
- modest sale value.

No separate mastery. Uses Herbalism.

Final name/model remains unlocked until an exact external plant source passes intake.

## Clay

Clay is a normal readable material, not magical mud currency.

Use:

- pottery/furnishing orders;
- selected alchemy containers/workstation upgrades;
- small regional contracts.

Mining strip pits are not required; use authored riverbank/clay-shelf nodes.

## Creature reagents

Venom/scute/reptile materials are admitted only when they have an actual recipe/use and donor terms permit the chosen integration path.

Do not add one material per reptile merely because the model exists.

---

# 13. Fishing / Fish Codex

R06 should be one of the stronger fishing regions because its water network naturally supports the collection loop.

R06 codex target:

```text
5–7 fish identities
```

Composition target:

- 2 common delta/river fish;
- 1 shallow-reed specialist;
- 1 deeper-channel uncommon fish;
- 1 rare regional/trophy fish;
- 1–2 overlaps with R05/R11 where ecology/model availability makes sense.

Uses:

- ordinary sale;
- `Grilled Catch` where valid;
- selected alchemy/cooking recipes;
- Fish Codex personal best / Trophy display;
- one regional contract or discovery chain.

No ordinary equipment drops from fishing.

Weather/time may change weights, but no main progression waits on a narrow rare-fish window.

Final species names wait for accepted external animated fish models.

---

# 14. Ecology / encounter roles

R06 should feel alive without becoming a reptile gauntlet.

## Crocodile

Role:

- shoreline/deep-channel ambush predator;
- clear resting/body silhouette before aggression;
- short committed lunge rather than permanent high-speed land chase;
- disengages outside territory.

## Caiman

Role:

- smaller/quicker wetland predator where current dependency behavior is distinct enough;
- if its actual behavior overlaps Crocodile too heavily, reduce density or use it as ecology rather than pretending two identical combat roles are different.

## Alligator Snapping Turtle

Role:

- slow territorial hazard / armored resource creature;
- high frontal solidity, clear bite commitment;
- not a high-speed pursuer.

## Shoebill / Mudskipper

Ambient ecology first.

Do not invent combat kits unless the actual external creature behavior makes that worthwhile.

## Diplocaulus

Useful amphibious resource/ecology creature.

- remains part of wetland food/resource chain;
- no forced hostility;
- exact drops normalized by project economy.

## Anaconda

Use at low density where actual current 26.2 behavior is stable and the encounter role differs from R05.

No grab attack may ignore multiplayer/server authority or animation readability.

---

# 15. Hydra field boss

Working role:

```text
Lv: 35
role: optional field boss
HP target: ~17,500–20,000
Defense: ~82
MR: ~68
Poise: 230
solo active TTK target: ~190–225 s
```

Exact values may shift after real donor-model movement/head-hitbox review.

## Arena

- flooded ruin/basin with several broad dry or ankle-deep platforms;
- melee builds always have a route to the body/head attack windows;
- deep water is boundary/positioning space, not the only floor;
- no tiny islands that make multiplayer body-blocking miserable;
- visible reeds/ruins cannot hide lethal telegraphs.

## Multi-head contract

Hydra's external body/head presentation should matter without becoming a tedious whack-a-mole.

Baseline design direction:

- shared boss HP;
- heads can have temporary stagger/interrupt states rather than separate permanent HP bars;
- attacks originate from visibly committed heads;
- breaking/interrupting one head creates a short local punish window and reduces the next combined pattern;
- no endless head-regrowth mechanic that resets progress;
- no requirement to use a specific damage element just to prevent regeneration unless the donor model/mechanics provide exceptionally strong visual justification and playtest proves it fun.

## Attack role set

Exact animation bindings wait for in-game dependency inspection, but the fight requires these readable roles:

1. **Bite/Sweep head attack** — close-range guardable pressure;
2. **Committed double-head cross** — two visible attack lanes with a central/outer dodge choice;
3. **Water-line surge** — shallow lane/arc that changes position without filling the arena permanently;
4. **Signature three-head sequence** — >=1.1 s initial tell, large but readable combined attack, followed by a real punish window;
5. **rear/body anti-camp response** only if model supports it visually.

Damage follows `COMBAT_BALANCE.md`; signature attacks may sit around the 30–42% benchmark-HP band but require unmistakable telegraphing.

Hydra does not get long untargetable swims.

## Rewards

First eligible defeat:

- guaranteed Superior+ R06 equipment roll;
- 2 model-linked signature materials after exact intake;
- 15% direct Mythic/signature roll;
- EXP ~20% current next-Lv requirement;
- Class XP ~15% current Class Rank requirement;
- trophy/house display unlock if the accepted model supports a meaningful visual trophy.

Repeat follows normal field-boss rules.

No `Hydra Token` currency.

---

# 16. Elite / event encounter package

R06 needs encounter variety beyond Hydra.

Examples:

## Levee Break event

- temporary local water-state controller;
- defend/clear one dangerous area while Waterwarden closes a gate;
- player contribution may be combat, interaction or support;
- event resets safely and does not permanently grief the world.

## Crocodilian Nest / Territory event

- route is threatened by a large authored predator group/elite;
- avoid making babies/nests a morality gimmick;
- objective is securing a route, luring adults away or defeating a territorial elite depending on content context.

## Medicine Run event

- dangerous herb shelf becomes accessible during a short regional condition;
- not a daily chore;
- reward feeds alchemy/cooking/economy rather than another token.

## Lost Ferry / Rescue event

- short navigation + combat/support encounter on a side channel;
- creates human context without becoming an escort mission lasting ten minutes;
- rescued NPCs are not allowed to pathfind through impossible swamp geometry for long distances.

---

# 17. Regional dungeon — Sunken Observatory / Flow-Regulator Shrine

Target first-clear wall-clock length:

```text
~22–32 minutes
```

R06 dungeon identity is **water-route control and alternating dry/shallow combat spaces**, not underwater combat.

## Stage 1 — Drowned approach

- broken observation platforms / old levee structures;
- visible evidence of contradictory water directions;
- one optional fish/herb/loot side shelf;
- local predators establish risk.

## Stage 2 — Outer regulator galleries

- player reaches a dry/semi-dry old control structure;
- large mechanical gates/channels show that this was infrastructure, not a temple built only for a boss;
- one or two server-controlled water-state changes alter routes;
- every changed route has clear before/after feedback.

## Stage 3 — Split-flow chamber

- central puzzle/combat space where two or three local branches can be isolated;
- solution is spatial/readable, not a wall of rune math;
- party members can operate separate interactions without desync;
- solved state persists for the current run and first-clear story state.

## Stage 4 — Observatory archive

- records show the local delta controller was later coupled to distant continental prediction/command nodes;
- environmental map/relief makes this visible without a long lore dump;
- player obtains the Act-II evidence package here.

## Stage 5 — Guardian basin

- broad central platform network with shallow channels;
- dungeon boss uses controlled water lanes/platform pressure;
- mechanics must correspond to final imported model.

## Repeat shortcut

After first clear:

- one upper gate/lift/boardwalk route opens from near entrance to the core wing;
- repeat runs skip most first-clear flow-isolation story interactions;
- boss/loot route remains meaningful without 25 minutes of repeated switch work.

---

# 18. Dungeon-boss mechanical contract

Exact visual identity remains open, but the encounter must satisfy:

```text
Lv: 36 target
role: dungeon boss
solo active TTK: ~165–200 s
poise band: ~200–240
```

Required identity:

- controls space through **visible water lanes / platform pressure / anchored attacks**;
- never becomes unreachable underwater for long periods;
- no permanent arena-wide slow;
- no random grab with poor telegraph;
- one phase transition changes route/attack geometry rather than just +damage/+speed;
- final animation/hitbox acceptance is required before exact attacks are locked.

The implementation team must stop and revise this section after asset intake if the selected external model cannot sell these mechanics visually.

---

# 19. Regional quest / discovery flow

Working regional chain:

## `Where the Walkways End` role

- enter settlement;
- inspect one route/gate failure;
- learn how locals actually travel and work;
- establishes the wetland before the Anchor debate.

## `Water Against Water` role

- investigate contradictory gate/flow behavior at two sites in flexible order;
- one may be reached via boardwalk, another via shallow/ferry route;
- no `collect 10 swamp tokens` filler.

## `Three Local Gates` role

- Waterwarden proposes isolating key regional branches;
- player reaches/repairs/isolates selected gateworks through actual POIs/encounters;
- at least two can be done in flexible order;
- visible travel/settlement safety improves.

## `The Basin With Many Heads` role

- Hydra clues reveal a natural apex predator has occupied a recently changed basin;
- optional hunt;
- never required to prove Anchor theory.

## `The Sunken Observatory` role

- dungeon / Act-II evidence;
- reveals that local waterworks were later remotely coupled;
- regional resolution keeps useful local control while cutting remote authority.

Foreground HUD remains one main lead + manually pinned optionals.

---

# 20. Regional outcome / memory

After the regional climax:

Shared late-join-safe consequences may include:

- selected gateworks remain visibly stabilized;
- one boardwalk/ferry route becomes reliably available;
- settlement ambient activity shifts from emergency repair to maintenance/trade;
- selected alchemy/fishing stock expands;
- one formerly flooded route becomes a normal shortcut while another natural wetland is deliberately left untouched.

Personal consequences:

- R06 evidence package recorded;
- local stewardship dialogue flags;
- Hydra hunt state;
- codex/discovery/trophy progress;
- personal first-clear/reward state.

The region should feel **better managed**, not artificially drained into dry land.

---

# 21. Reward identity

Regional equipment/affix emphasis:

- Poison/Bleed buildup control or resistance;
- healing amplification / cleanse utility;
- anti-grab / recovery / stagger-resistance tools;
- movement/skill handling in shallow-water or slowed states without creating a mandatory `Swamp Set`;
- WIL/VIT/END support accessories;
- selected DEX/alchemy utility;
- water/venom/nature spell variants where external VFX/skill support fits.

R06 does not introduce a new currency.

Useful items must remain relevant outside R06.

---

# 22. Audio / presentation

R06 cannot rely on `green fog + frog sound`.

Outdoor layers:

- broad wetland insects/birds/frogs at restrained density;
- water movement, boardwalk creak, distant ferry bell, reeds/wind;
- predator areas become quieter before major attacks instead of simply getting louder;
- settlement adds work rhythms, pottery/alchemy/fishing sounds.

Dungeon transition:

- outdoor organic ambience falls away;
- heavy water resonance, old gates, dripping chambers and mechanical rumble increase;
- control-room audio becomes clearer/drier as the player reaches functional old machinery.

Hydra:

- distinct head/body cues must support attack readability;
- roar layering cannot hide the start of lethal tells.

All audio requires external/licensed source intake appropriate to project standards before final acceptance.

---

# 23. Multiplayer / authority

Server owns:

- water/gate world states;
- quest/evidence state;
- ferry/skiff authority if used;
- Hydra/guardian encounter state;
- personal loot;
- personal gathering/fishing state;
- dungeon branch/shortcut state;
- first-clear/repeat reward eligibility.

Required multiplayer tests later:

- one player changing a shared gate does not skip another player's personal interaction/story flag;
- late join sees stable physical water route but can still complete evidence;
- support contribution qualifies during Hydra/event encounters;
- no kill ownership/last-hit race;
- ferry/controller authority cannot be stolen/desynced;
- dungeon water state remains identical for all clients;
- disconnect during water-state transition restores a valid deterministic state;
- no boss/reward duplication through relog/chunk unload.

`MULTIPLAYER TESTED` remains NO until real-client verification.

---

# 24. Performance constraints

- no global water simulation;
- no per-tick scanning of every gate/boardwalk/node in the region;
- authored local water-state controllers only;
- ambient wetland mobs use bounded density and simplified distant behavior;
- crocodilian/anaconda pathfinding activates only in relevant loaded areas;
- Hydra logic exists only for the active encounter zone;
- deep channels do not spawn huge schools of fully pathfinding fish;
- large reed/prop fields favor block/world dressing over entity-heavy decoration;
- profile settlement + wetland + rain + event + multiplayer combinations before acceptance.

---

# 25. Asset-intake blockers before implementation

R06 is not asset-ready until these are resolved:

1. current 26.2 behavior inspection for Crocodile/Caiman/Snapping Turtle/Diplocaulus/Anaconda;
2. Hydra in-game model/animation/head-hitbox inspection;
3. exact wetland plant/fiber/herb node models + source/license/hash;
4. exact fish-model assignments for the R06 codex;
5. exact raised-settlement/dock/boardwalk structure composition;
6. skiff/ferry model and whether local vehicle behavior passes Minecraft playtesting;
7. exact dungeon-boss model/animation source;
8. old water-gate / Anchor machinery visual family;
9. R06 equipment/weapons/accessories exact external model families;
10. alchemy settlement props/UI bindings;
11. Hydra and dungeon VFX/sound sources;
12. trophy/furnishing model compatibility.

No placeholder vanilla entity/boat/guardian is accepted as the finished answer for these gates.

---

# 26. R06 quality acceptance

R06 is not complete because every table exists.

Actual play must prove:

- wetland travel feels different without feeling slower for its own sake;
- the player can orient using landmarks rather than the map every 20 seconds;
- dry/shallow/deep routes each create useful choices;
- no normal route requires prolonged awkward swimming combat;
- the settlement visibly belongs to the wetland economy;
- ecology contains ambient/non-hostile life as well as predators;
- Hydra is readable for melee and ranged builds;
- the dungeon water-state mechanics are understandable in multiplayer;
- the stewardship story is learned through consequences, not only dialogue;
- fishing/foraging/alchemy loops are useful without becoming chores;
- post-clear route/world changes are noticeable;
- visuals/audio meet the external-first quality bar;
- performance remains acceptable at target multiplayer density.

Verification state at design completion:

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
