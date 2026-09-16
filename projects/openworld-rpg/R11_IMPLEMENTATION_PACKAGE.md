# Open-World RPG — R11 Inner Sea Implementation Package

> Status: **DESIGN CANON — R11 layered world/story/traversal/economy/boss flow locked; deep-water combat animation/skill compatibility and exact sea-fort boss are explicit technical/asset gates**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Story spine: `WORLD_STORY_CANON.md`  
> Region graph: `REGIONS.md`  
> Combat: `COMBAT_BALANCE.md`, `STATUS_AND_R01_ENCOUNTERS.md`  
> Loot/equipment: `LOOT_ECONOMY.md`, `EQUIPMENT_BALANCE.md`  
> Mounts: `MOUNTS.md`  
> Fishing/housing: `FISHING_COLLECTION_HOUSING_MARKET.md`  
> Quest/state: `QUEST_WORLD_STATE.md`  
> Quality contract: `DESIGN_COMPLETENESS_AUDIT.md`  
> External provenance: `EXTERNAL_SOURCES.md`  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins.

R11 is not one flat `ocean biome`. It is a **three-layer major region** that grows with the player across the campaign:

```text
Lv28 coast / harbors / shallow islands
→ Lv44 open sea / reefs / freeport routes
→ Lv64 abyssal trenches / deep temples
```

Its full play sentence is:

```text
first meet the Inner Sea as a useful coastal economy of harbors, fishing grounds and islands
→ later return with stronger traversal to treat reefs, freeports and open-water routes as a real mid/high-tier region
→ unlock Laviathan as a group-capable water specialist instead of spending minutes in empty boat travel
→ learn the sea through visible islands, lighthouse chains, wrecks, reef color, weather and creature movement rather than map-icon spam
→ obtain a permanent late-game dive utility that removes breath chores and makes deep exploration physically comfortable
→ descend into a sparse abyss where vertical distance, darkness, sound and huge creatures create tension without an oxygen-consumable treadmill
→ discover that old Anchors did not create the sea but used deep pressure/current corridors as continental transport and observation infrastructure
→ see partial reactivation improve one shipping/current route while shifting pressure toward a deep habitat and ancient temple
→ confront Abyss Fang at an authored underwater altar/temple as a true 3D late-game encounter
→ isolate the deep corridor from remote continental load balancing while preserving local harbor/navigation benefits
→ leave with the final late-game proof that one regional improvement can create consequences far away and far below
```

R11 should feel like **a region the player learns in stages**, not a one-time swimming detour.

---

# 1. Locked regional identity

Preserved from `REGIONS.md`:

- central inland sea / pirate coast / reefs / abyssal routes;
- intentionally layered suggested-entry structure rather than one misleading number;
- coast/harbor/shallow islands: **Lv28**;
- open sea/reefs/freeport routes: **Lv44**;
- abyss/trenches/deep temples: **Lv64**;
- ecology direction includes custom aquatic life from Threateningly Mobs Continued and Alex's Mobs Continued;
- **no vanilla cod, salmon, tropical fish, dolphin or squid population**;
- Threateningly candidates: Hippofish, Red Triplefish, Giant Sea Cucumber, Beast Horseshoe Crab, Riptooth, Abyss Fang;
- Alex candidates: Orca, Hammerhead Shark, Frilled Shark, Giant Squid, Cachalot Whale, Lobster, Flying Fish, Comb Jelly and curated ambience;
- settlement direction: major harbor/freeport plus small island ports;
- economy: seafood, reef reagents, medicinal sea-cucumber materials, abyssal mineral/material roles;
- dungeon archetypes: sea-fort/pirate-cove progression dungeon plus late abyssal temple;
- reward identity: water mobility, projectile control, bleed/predator interactions, fishing/resource utility and abyssal weapon/spell variants.

Production clarifications:

- no empty-ocean travel tax;
- no dedicated naval-combat progression tree at launch;
- no boat fuel/durability/cannon-upgrade grind;
- no oxygen-tank consumable grind;
- no separate aquatic weapon inventory, aquatic armor tier or underwater class progression;
- no long stretches of slow vanilla swimming as the intended late-game traversal;
- deep-water content is sparse and landmark-led rather than full of repeated submerged chests;
- `pirate/freeport` means a mixed harbor culture of traders, smugglers, privateers, fishers and sailors — not a cartoon skull-and-cannon theme applied to every building.

---

# 2. External game-content precedents

These are structural references only. No proprietary maps, story scripts, art or exact balance values are copied.

## 2.1 Subnautica — exploration, discovery and the unknown

Unknown Worlds' GDC design talk explicitly frames Subnautica around exploration, discovery and the unknown, and discusses using radio signals to add structure to a sandbox without over-directing the player.

Project adoption:

- abyss exploration is driven by silhouettes, bioluminescent life, distant sound, current/pressure effects, wreck/temple light and authored signals;
- the journal/map may provide broad depth/area guidance after discovery but does not reveal every deep POI automatically;
- story clues guide the player deeper through physical evidence instead of a permanent objective beam;
- deep traversal should create curiosity and uncertainty while remaining mechanically fair.

Explicitly not adopted:

- survival crafting as the core game;
- repeated oxygen-tank progression;
- hunger/thirst/base-building requirements;
- inventory loops centered on collecting huge amounts of raw ore underwater.

Reference:

- `https://www.gdcvault.com/play/1025745/The-Design-of-Subnautica`

## 2.2 Assassin's Creed Odyssey — land and sea designed as one world

Ubisoft's World Director described Odyssey as roughly a 50/50 land/ocean world and explained that islands, sea and mainland were planned together from the exploration phase.

Project adoption:

- R11 is designed together with R04/R07/R09/R10 coasts rather than as empty space between them;
- major ports naturally connect regional economies;
- islands/reefs/sea forts exist because they create routes and content, not because the map needed dots;
- leaving a harbor should immediately present readable route choices and distant landmarks.

Not adopted:

- a full naval-war upgrade loop;
- mandatory ship crew management;
- giant stretches of ocean whose only purpose is travel time.

Reference:

- `https://www.ubisoft.com/en-au/game/assassins-creed/news/2zOk4Z7OoI5COWAh6NRsfS/inside-the-studio-with-world-director-benjamin-hall`

## 2.3 Guild Wars 2 — underwater freedom, but complexity caution

Guild Wars 2 underwater mode demonstrates that 3D underwater exploration can work without breath restrictions or an underwater movement penalty.

Useful precedent:

- underwater movement is not inherently punished;
- combat can use the vertical axis;
- deeper areas can have stronger/more aggressive ecology.

Project adoption:

- late deep-water exploration has no routine breath timer once the R11 dive utility is unlocked;
- deep movement is deliberately faster/more responsive than vanilla underwater movement;
- authored attacks/encounters may use vertical space;
- depth bands affect ecology/pressure/readability.

Explicitly not adopted:

GW2 also automatically switches players to aquatic headgear, separate aquatic weapons, aquatic weapon skills and a separate underwater utility bar.

This project rejects that complexity.

- no second weapon progression;
- no second class build page;
- no underwater-only equipment rarity ladder;
- no requirement to configure a second 5-skill loadout for every class.

Reference:

- `https://wiki.guildwars2.com/wiki/Underwater_mode`

## 2.4 The Witcher 3 Skellige — explicit negative precedent

A Witcher 3 designer later acknowledged that the large number of Skellige ocean question marks/smuggler caches became a mistake: a late production pass filled the sea with repeated underwater caches that became collectible-like map clutter.

Project rule:

```text
NO sea full of repeated underwater loot icons.
```

R11 POIs must be authored destinations with identity:

- island settlement;
- reef ecology;
- wreck/event;
- sea fort;
- predator territory;
- deep trench;
- abyss temple;
- unusual fishing ground;
- current/Anchor infrastructure.

If a location's only purpose is `dive → open chest → leave`, it normally does not deserve a major map marker.

Reference:

- `https://www.nintendolife.com/news/2022/07/witcher-3-dev-admits-he-overcrowded-one-map-with-too-many-points-of-interest`

---

# 3. External-first aquatic source stack

## 3.1 Alex's Mobs Continued — current 26.2 aquatic ecology

Current Alex's Mobs Continued supports Minecraft 26.2 on Fabric and retains the full original creature roster.

Strong R11 candidates:

- Orca;
- Hammerhead Shark;
- Frilled Shark;
- Giant Squid;
- Cachalot Whale;
- Lobster;
- Flying Fish;
- Comb Jelly;
- Laviathan as the canonical specialized water/lava mount.

Important 26.2 integration evidence:

- an early 26.2 build had multipart hitbox ID crashes affecting Cachalot Whale, Giant Squid and Laviathan;
- the continuation explicitly fixed that issue;
- current 26.2 Fabric releases have continued beyond that patch.

Project consequence:

- direct current-build verification is still required for all large multipart aquatic creatures;
- their existence on 26.2 is no longer enough to call them production-safe;
- project data owns spawn density, Lv, aggression, loot and regional eligibility.

Reference:

- `https://www.curseforge.com/minecraft/mc-mods/alexs-mobs-continued`

## 3.2 Threateningly Mobs Continued — ocean threats / Abyss Fang

Current 26.2 continuation explicitly lists:

- **Beast Horseshoe Crab** — oceans and beaches;
- **Riptooth** — oceans at night;
- **Abyss Fang** — summoned at an underwater altar.

Project use:

### Beast Horseshoe Crab

- heavy shoreline/reef threat;
- low density;
- strong armor/impact identity if current model/animation review passes;
- not a generic every-beach spawn.

### Riptooth

- open-sea/night predator and optional mid/high-tier roaming boss direction;
- authored encounter area/event rather than blanket night-ocean population;
- exact boss use depends on current model/behavior quality.

### Abyss Fang

Original identity is a huge dark sperm-whale-like Ultra boss with a large maw/crimson body crack, summoned only at an underwater altar; source behavior includes `Charge` and `Devour` identities.

Project adoption:

- preserve deep-water altar/ritual boss identity;
- preserve readable charge/devour body language if current 26.2 implementation supports it;
- replace donor Ultra Summon Stone grind with a project-authored deep-temple encounter condition;
- replace donor progression/loot with project rewards;
- never spawn Abyss Fang as normal ocean population.

Reference:

- `https://modrinth.com/mod/threateningly-mobs-continued`

## 3.3 Fish-model / codex source direction

Use the already-established external animated-fish intake path.

R11 is the largest fishing region but still follows the global rule:

```text
actual accepted animated model
→ ecological depth band / silhouette
→ name / rarity / size table
→ cooking / sale / collection use
```

Do not invent ten fantasy fish names first and then recolor one model ten times.

## 3.4 Harbor / watercraft assets

Candidates remain:

- Kenney `Watercraft Kit` — CC0 small watercraft candidate;
- neutral dock/platform pieces from Kenney `Pirate Kit` where they fit;
- accepted external fantasy/harbor structure families;
- strong Minecraft ship/harbor build references/schematics when modular model kits are visually weaker.

Rules:

- public-repo assets must satisfy their real redistribution terms;
- harbor/freeport uses a coherent architectural family;
- no vanilla oak-boat fleet as final presentation;
- no random collage of pirate packs, medieval villages and tropical props.

---

# 4. Story role — consequences travel farther than people can see

R11's deep layer is a valid late Act-III investigation alternative to R10.

Its evidence package argues:

> Continental systems can move risk through routes humans cannot directly see. A surface improvement may create a distant deep-water consequence without any local actor intending harm.

Historical role of the Inner Sea Anchor network:

- it did **not** create the sea or control ordinary tides like a faucet;
- it observed and partially stabilized deep current/pressure corridors;
- these corridors supported ancient navigation, transport, observation and communication between distant regional nodes;
- surface harbors later built around the safer/predictable routes;
- after network decline, modern sailors adapted through beacons, seasonal routes, pilots and local knowledge;
- deep facilities remained mostly forgotten.

Modern conflict:

- a Restoration test reactivates part of a deep corridor;
- one surface shipping route becomes measurably faster/safer;
- fog/current uncertainty decreases around a major approach;
- however the corridor's pressure compensation shifts toward an abyssal trench and temple/habitat;
- deep creatures move, structures crack/open and Abyss Fang's altar zone becomes active;
- the effect is remote and delayed enough that surface observers initially consider the test a complete success.

Regional resolution:

- preserve local harbor prediction/beacon data and bounded route assistance;
- sever automatic cross-continental pressure balancing;
- leave deep corridors under explicit local/depth-band limits;
- document the delayed remote consequence as key evidence for the final decision.

R11 therefore complements R10:

- R10 shows an obvious **fast cascade** in one industrial region;
- R11 shows a **distant, delayed consequence** across physical depth and travel routes.

---

# 5. Layered progression model

R11 is revisited rather than consumed once.

## Layer A — Coast / Harbor / Shallow Islands

Suggested entry:

```text
Lv 28
```

Purpose:

- first major harbor discovery;
- fishing/seafood/freeport economy;
- short island routes;
- Beast Horseshoe Crab / ordinary coast threats;
- connects R04/R06/R07/R09 geography;
- does not require deep-dive utility.

## Layer B — Open Sea / Reef / Freeport Routes

Suggested entry:

```text
Lv 44
```

Purpose:

- reef/island exploration;
- Riptooth/shark threats;
- sea-fort dungeon;
- Laviathan unlock progression;
- stronger fishing/trophy catches;
- long-range route network.

## Layer C — Abyss / Trenches / Deep Temple

Suggested entry:

```text
Lv 64
```

Purpose:

- permanent dive utility;
- sparse vertical deep exploration;
- Frilled Shark/Giant Squid/Cachalot/deep ecology where appropriate;
- old current/pressure infrastructure;
- abyssal temple;
- Abyss Fang major boss;
- late Act-III evidence.

The map/journal shows these as one named region with clear depth-band danger, not three arbitrary biomes.

---

# 6. Major harbor / freeport culture

Primary hub: **large harbor / freeport trade city** located where several mainland/island routes meet.

`Freeport` does not mean every NPC is a pirate.

Normal life visibly includes:

- fish unloading/sorting/smoking;
- net/rope/sail repair;
- ferry/small-craft traffic;
- reef/diver salvage crews;
- long-distance merchants;
- legal and grey-market stalls;
- private guards/privateers;
- cart/warehouse exchange with inland caravans;
- weather/current boards;
- lighthouse/beacon maintenance;
- Laviathan handler/large-animal dock after that progression is known.

Key local roles:

## 6.1 Harbor Master / Route Pilot

- tracks surface routes, fog, reefs and shipping delays;
- supports bounded local prediction systems;
- sees the immediate benefits of restored current data;
- grounds the main story in real travel rather than abstract ocean magic.

## 6.2 Reef Warden / Diver

- understands reef ecology, wrecks and safe dive routes;
- notices pressure/creature changes before harbor administrators do;
- owns the deep utility unlock/tutorial path;
- warns against treating the deep sea as empty infrastructure space.

## 6.3 Freeport Factor / Broker

- connects seafood, salvage, R09 metals, R10 forge goods and island trade;
- makes R11 a real economy connector;
- may handle rare goods without creating a separate pirate currency.

## 6.4 Laviathan Handler

- integrates the already-canonical mount into a believable marine service;
- no random feeding/breeding grind;
- teaches passenger/turning/large-body behavior in authored water space.

Recurring Anchor Scholar, Cartographer/Ranger, Rival Wanderer and Restoration representatives can appear when the main route reaches deep investigation.

---

# 7. Surface and open-sea traversal

R11 must not contain mandatory multi-minute empty-water crossings.

## 7.1 Harbor / island ferries

For early coastal travel:

- discovered fixed ferry routes may connect major ports/islands;
- ferries are world services, not fuel systems;
- route unlocks can follow actual discovery;
- no durability/crew/cannon upgrades.

## 7.2 Small craft

A project-owned skiff/sailcraft may exist only if current Minecraft handling, collision and multiplayer authority feel good.

If vehicle controls are poor:

```text
use ferries + authored docks + Laviathan + shrine/port fast travel
```

instead of shipping a bad boat system.

## 7.3 Laviathan — canonical R11 unlock

R11 is now the **primary launch unlock region** for the Laviathan.

R10 may foreshadow its lava utility, but R11 owns the actual acquisition because the creature's strongest identity is broad water/lava traversal and multiplayer ferrying.

Preserve `MOUNTS.md` values:

```text
unlock context: roughly Lv58–64
registration/tack after quest: 3,000 Gold
water/lava cruise: 8.8 b/s
land cruise: 4.6 b/s
max riders: 4
Resolve: 2.5x player MaxHP
```

Authored unlock shape:

```text
meet/observe large aquatic transport ecology
→ solve one route/creature interaction with the handler
→ prove control around reefs/open water
→ register persistent Laviathan
```

No Nether requirement.

The Laviathan does not enter the abyss temple or trivialize authored deep-interior geometry.

---

# 8. Permanent deep-dive utility

R11 deep content needs a late-game **Dive Utility** because vanilla underwater movement/breath friction is not the intended challenge.

Production/internal role name:

```text
Deep-Dive Harness
```

Final player-facing name/model waits for external asset intake.

Design:

- permanent personal utility unlock, not a normal 12-slot combat equipment item;
- visually represented with accepted external fantasy/diver apparatus where appropriate;
- automatically activates in authored deep-water state / underwater traversal;
- no durability;
- no fuel;
- no oxygen consumables;
- no separate upgrade tree;
- server owns unlock state.

Baseline functional targets after unlock:

- ordinary underwater breath restriction effectively removed for authored R11 deep exploration;
- swim movement becomes substantially more responsive than vanilla baseline;
- vertical ascent/descent controls remain readable;
- one short `Swim Burst` replaces/retargets the ordinary dodge movement underwater while preserving the same defensive commitment philosophy;
- surface/land stats are unaffected.

The utility arrives around the transition into the Lv64 deep layer, not during the first Lv28 harbor visit.

Without the utility, the player may still swim/dive normally in shallow water but deep main content is presented as **not yet practically equipped**, not blocked by an invisible level wall.

---

# 9. Underwater combat adaptation contract

R11 does **not** create a second combat game.

Canonical rule:

```text
same class
same stats
same equipment
same 4 active + ultimate progression
same Mana/Stamina/ultimate economy
same reward/build identity
```

However, not every land animation/skill can be blindly played while swimming.

## 9.1 No separate aquatic progression

Explicitly forbidden:

- aquatic weapon slots;
- underwater-only item levels;
- second class build page;
- underwater skill-tree currency;
- second configured skill bar;
- mandatory spear/harpoon weapon swap for every class.

## 9.2 Skill compatibility adaptation

Before deep-content implementation, each frequent combat action receives one of three data tags:

```text
AQUATIC_NATIVE
AQUATIC_ADAPTED
AQUATIC_DISABLED_WITH_FALLBACK
```

### AQUATIC_NATIVE

Skill already works cleanly in 3D water with accepted external animation/VFX.

### AQUATIC_ADAPTED

Same learned skill identity/economy, but movement/trajectory/animation is adjusted for water.

Examples:

- ground dash → directional swim burst;
- ground slam → short downward/forward pressure strike if the animation supports it;
- horizontal projectile → full 3D aim with bounded range;
- stationary cast → stabilized swim cast.

### AQUATIC_DISABLED_WITH_FALLBACK

Only allowed where the original skill cannot be made visually/mechanically honest.

The slot temporarily exposes a **direct aquatic variant of that same learned skill**, not a separate progression unlock.

The player does not manage or equip it separately.

## 9.3 Basic weapon rules

- sword/dagger/spear/staff/catalyst/bow/ranged families remain their owned/equipped weapons;
- exact underwater attack animation sets require external intake;
- huge ground-only hammer/greatsword swings may be shortened/retimed underwater if the model/animation supports believable drag/commitment;
- project never shows a full land combo pose while the player floats horizontally;
- hitboxes remain server-authoritative and tied to visible weapon movement.

## 9.4 Defense

- underwater dodge becomes Swim Burst, preserving the same concept of short defensive invulnerability + recovery;
- guard/parry remains available only where the weapon/off-hand and animation can present it honestly;
- if a specific guard pose fails visually, use a class/weapon-compatible aquatic defensive animation before implementation;
- no global underwater damage penalty.

This entire section is a **technical/animation gate**: design is locked, but exact per-class animation bindings must pass a dedicated R11 aquatic-combat audit before source implementation.

---

# 10. Deep navigation / readability

The abyss should feel unknown, not directionless.

Use large vertical landmarks:

1. **surface light / harbor-beacon direction** at upper depth where physically plausible;
2. **reef wall / trench lip** silhouette;
3. **bioluminescent life band** as ecological depth cue;
4. **ancient pressure pylons / current rings**;
5. **temple light / altar signature**;
6. one huge natural silhouette such as whale/squid route or rock arch.

Rules:

- darkness increases by depth but never reduces navigation to black screen + waypoint;
- important interactables have silhouette/lighting contrast;
- deep fog distance is tuned for tension without hiding lethal charges;
- vertical distance is communicated in map/journal UI where needed;
- players can retreat upward/along a known current route without following a tiny breadcrumb trail.

---

# 11. R11 POI package

Because the region spans three level bands, POIs are layered.

## Coast / harbor POIs

### Major Harbor / Freeport

- social/service/economy hub;
- fishing/port/mount progression anchor;
- later main-story return point.

### Lighthouse Chain

- navigation/worldbuilding;
- selected beacons may reopen as safe local world-state changes;
- not a Ubisoft-style tower checklist.

### Beast Horseshoe Beach

- authored heavy coastal threat / ecology;
- gathering/fishing/resource value;
- no mandatory boss.

## Open-sea POIs

### Living Reef

- fishing/collection/resource destination;
- strong color/silhouette landmark;
- predator/prey ecology;
- later pressure changes can be visible here.

### Wreck Field / Storm Route

- one or two substantial authored wreck/event spaces;
- salvage + story + navigation;
- **not dozens of repeated underwater crates**.

### Sea Fort / Freebooter Keep

- mid/high-tier dungeon;
- island/reef/fort traversal rather than fully underwater;
- links trade conflict and regional route safety.

### Riptooth Night Route

- optional roaming/field-boss encounter space;
- clue-based discovery;
- night/ocean source identity preserved without forcing all night travel into boss combat.

## Abyss POIs

### Trench Lip Observatory

- first major deep landmark;
- dive utility test / route understanding;
- old pressure/current infrastructure visible.

### Whale Fall / Deep Ecology Site

- natural large-scale ecology landmark if accepted assets/behavior support it;
- no chest required;
- rare materials/creatures/fish possible.

### Pressure Corridor

- visible evidence of partial reactivation;
- main-story route;
- current/lighting/creature changes show remote consequence.

### Abyss Temple / Underwater Altar

- late dungeon/main investigation;
- Abyss Fang encounter.

---

# 12. Settlement / services / housing

## Major harbor services

Baseline:

- shrine/fast travel;
- inn/rest/food;
- Material Vault/bank;
- seafood/fishing market;
- general/import merchant;
- forge/smith appropriate to a major trade hub;
- alchemy/healer;
- dock/ferry services;
- Laviathan handler after progression;
- dive utility / reef-watcher service;
- contract/route board.

The harbor can be one of the game's largest settlements but Minecraft NPC density remains bounded.

Target daytime physical population:

```text
12–18 functional/service/guard/dock NPCs
8–12 ambient residents/travelers
```

Use schedules/visibility culling rather than 100 simulated citizens.

## Small island ports

- 2–4 useful service nodes across the region;
- not full duplicate towns;
- may provide shrine/ferry/food/fishing merchant/contract functions;
- each needs visual/economic identity.

## Housing

R11 may support late housing progression strongly:

- 1–2 Large House-class waterfront properties around the existing ~25,000 Gold band;
- **one optional Prestige House-class harbor property around the 65,000+ Gold band** if the selected map/build family provides a convincing authored shell;
- prestige property is a Gold sink/display/home goal only;
- no passive merchant income, dock tax or property automation;
- no boat-house inventory exploit.

This is a natural late-game place to offer the first true prestige residence because R12 is not intended to become a normal residential capital.

---

# 13. Fishing / Fish Codex

R11 is the launch game's largest fishing/collection region.

Total R11 codex target:

```text
7–10 fish identities across all depth bands
```

This count includes shared/overlapping species; it is not 10 wholly unique fish by force.

Composition target:

- 2–3 coast/shallow species;
- 2–3 open-sea/reef species;
- 2–3 deep/abyss species;
- 1–2 species overlapping adjacent coasts where ecology fits.

At most a few deep catches should be overtly magical/abyssal.

Use cases:

- sale;
- cooking/Grilled Catch where valid;
- selected alchemy/medicine;
- personal-best/trophy records;
- housing display;
- regional collection/contract;
- deep species may unlock lore/ecology records, not combat equipment drops.

R11 must not create a second fishing currency.

---

# 14. Gathering / aquatic resource economy

R11 resource identities remain bounded.

## Giant Sea Cucumber medicinal role

If current donor creature/model and terms fit:

- medicinal/alchemy reagent source;
- not a required slaughter farm;
- project-normalized drops;
- regional healer/alchemist use.

## Reef reagent role

One or two accepted external coral/reef material identities maximum.

Uses:

- alchemy;
- selected furnishing/appearance;
- magical/weapon accessories.

No coral-color material spam.

## Abyssal mineral role

One high-tier deep mineral/resource may exist if exact external visual source passes.

Uses:

- late weapon/catalyst/accessory crafting;
- R12 bridge recipes;
- selected deep utility.

It cannot be obtainable only from Abyss Fang RNG.

## Seafood

Fish/creature-food economy feeds cooking and sale without recreating vanilla cod/salmon items.

---

# 15. Ecology by depth

## Coast / shallow

- lobster/flying fish/selected passive fish;
- Beast Horseshoe Crab heavy coast threat;
- occasional shark boundary depending on exact current behavior;
- seabirds/harbor ambience from accepted sources where appropriate.

## Open sea / reef

- Orca as rare large wildlife;
- Hammerhead Shark as predator;
- Giant Squid/Cachalot routes at low density where current multipart behavior passes;
- Riptooth authored night predator/boss direction;
- Comb Jelly and fish schools as bounded ambience.

## Deep / abyss

- Frilled Shark as deep predator;
- selected Giant Squid/Cachalot if depth/behavior fits;
- deep fish / comb-jelly-like ambience;
- Abyss Fang only at authored altar encounter.

Rules:

- no predator every 20 blocks;
- huge creatures have large territory footprints and low density;
- ambient schools are lightweight presentation, not hundreds of pathfinding entities;
- predator aggro distance/vertical pursuit has strict disengage rules;
- surface players are not repeatedly attacked from unseen abyss depths.

---

# 16. Riptooth optional field-boss direction

Current source identity: ocean predator active at night.

Working project role:

```text
Lv: 51
role: optional open-sea field boss
HP target: ~28,000–33,000
Defense: ~118
MR: ~83
Poise: ~235
solo active TTK target: ~195–225 s
```

Exact stats wait for current 26.2 model/movement inspection.

Arena/route:

- broad reef/open-water territory near one visible island/rock/ship landmark;
- enough surface/shallows for players who have not entered deep-dive progression;
- boss can move through water freely but cannot spend long unreachable periods far below melee players;
- current/reef geometry cannot hide lethal lunges.

Required attack roles after model inspection:

- close bite;
- committed lateral pass/charge;
- one breach/surface or reef-angle reposition;
- signature predator rush with strong audio/visual tell;
- high-pressure sequencing below ~40% without hidden stat steroid.

If current Riptooth presentation does not support fair open-water combat, use it as elite ecology and select a stronger field-boss model rather than forcing the slot.

---

# 17. Mid-tier dungeon — Sea Fort / Freebooter Keep

Suggested layer:

```text
Lv 46–52
```

Target first-clear:

```text
~20–30 minutes
```

Identity:

- mostly surface/fort/island combat;
- short dock/reef approaches;
- not a full naval-combat dungeon;
- demonstrates harbor politics/trade without turning the game into pirate faction management.

Flow:

1. reef/dock approach with two routes;
2. outer fort / warehouse yard;
3. signal/route-control tower;
4. interior storehouse/command space;
5. final commander/guardian arena.

Final boss:

```text
SEA-FORT BOSS VISUAL IDENTITY: OPEN EXTERNAL ASSET GATE
```

Possible direction:

- high-quality external corsair/armored humanoid with accepted player-scale combat animations;
- or a distinct external fort guardian if humanoid quality is weaker.

No vanilla pillager captain or flat-skin boss.

Rewards:

- open-sea equipment/materials;
- route/ferry/freeport improvements;
- optional named weapon/accessory after external model selection;
- no pirate token currency.

---

# 18. Laviathan unlock chain

Working sequence:

```text
open-sea route problem / large-creature observation
→ work with harbor handler / reef warden
→ reach a safe broad-water interaction area
→ demonstrate steering / passenger / large-body handling
→ solve one ecological/route encounter
→ register persistent Laviathan for 3,000 Gold
```

Rules:

- no random tame chance;
- no repeated feeding stack;
- no breeding;
- no mount level;
- one handler sequence, then permanent unlock;
- multiplayer registration state personal;
- up to 4 riders with server-owned controller seat.

The unlock should occur **before** the player begins routine abyss travel so R11's large water spaces become more convenient as the region deepens.

---

# 19. Deep-dive unlock chain

The Deep-Dive Harness role is introduced through the Reef Warden/Diver and old pressure-route investigation.

Working sequence:

```text
find evidence below normal comfortable dive depth
→ inspect one shallow old pressure station / wreck
→ acquire/assemble accepted permanent dive apparatus through ordinary regional materials + service
→ test movement at trench lip
→ unlock deep route
```

Economic rule:

- cost/material requirement should be meaningful but modest for Lv60+;
- no rare boss RNG gate;
- no consumable refill;
- no second upgrade tree.

Exact recipe waits for accepted model/material catalog.

This is equipment-readiness gating through the world, not an invisible `Lv64 required` wall.

---

# 20. Late dungeon — Abyss Temple / Pressure Observatory

Suggested layer:

```text
Lv 65–69
```

Target first-clear wall-clock:

```text
~28–38 minutes
```

The dungeon alternates true-water traversal with large air/dry/pressure chambers so Minecraft combat never spends 35 continuous minutes underwater.

## Stage 1 — Trench descent

- true 3D deep traversal;
- strong landmark/light route;
- sparse predator pressure;
- demonstrates Deep-Dive Harness movement without a tutorial popup wall.

## Stage 2 — Pressure pylons

- exterior/partly flooded old structures;
- visible current corridor effects;
- player sees modern reactivation shifting pressure downward;
- one optional resource/ecology branch.

## Stage 3 — Air/maintenance chamber

- dry or air-pocket interior break;
- combat/resource/reset space;
- old maps show sea routes and distant Anchor nodes;
- prevents underwater combat fatigue.

## Stage 4 — Deep observatory

- records/physical instruments reveal ancient current/pressure corridor purpose;
- direct evidence links surface route improvement to deep displacement;
- late Act-III evidence package recorded.

## Stage 5 — Underwater altar / Abyss Fang chamber

- broad 3D arena around a strong altar/pressure-ring landmark;
- enough visual reference points to judge vertical distance;
- no tiny cave walls that cause camera/hitbox chaos;
- Abyss Fang invoked through current story state rather than donor summon-stone grind.

## Repeat shortcut

After first clear:

- pressure/current lift or temple gate provides a faster trench-to-core route;
- story-only observation steps are skipped;
- repeat route keeps deep identity but avoids long empty descent.

---

# 21. Abyss Fang major boss

Source identity:

- underwater altar summoned;
- huge dark whale-like body;
- prominent maw / crimson crack visual;
- `Charge` and `Devour` behavior identity.

Working project target:

```text
Lv: 69
role: late major dungeon / Act-III boss
HP target: ~55,000–65,000
Defense: ~150
MR: ~140
Poise: ~285
solo active TTK target: ~235–290 s
```

Exact values depend on current model scale, movement speed and vulnerable uptime.

## 3D arena contract

- arena has visible upper/lower rings/altar/rock silhouettes;
- boss can attack vertically but cannot leave useful player range for extended periods;
- movement path server-authoritative;
- no instant turn-around 180° body hits;
- giant-body collision/hitbox must be tested with multiple clients;
- camera must remain readable around the model.

## Attack identity

### Maw Bite

- close readable commitment;
- broad but honest mouth hitbox;
- guard/defense handling decided after animation review.

### Abyss Charge

- strong initial orientation/tell;
- limited steering after commitment;
- visible pressure/wake line;
- miss creates real reposition/punish window.

### Devour

Source identity may be preserved only if the current animation/hitbox can make a grab readable.

Project safeguards:

- >=1.0 s unmistakable tell for a major grab;
- server-authoritative target capture;
- no client-only teleport into mouth;
- ally/support counterplay or short escape/interrupt condition where visually reasonable;
- no unavoidable instant kill;
- boss cannot chain Devour repeatedly.

If donor Devour cannot satisfy these conditions, replace the exact mechanic rather than preserving bad behavior for fidelity.

### Pressure Ring / Current Sweep

Project-authored environmental attack only if VFX can clearly show the 3D volume.

- uses one horizontal/depth plane or expanding visible ring;
- not invisible sphere damage;
- gives vertical repositioning purpose.

## Phase escalation

Below ~50%:

- current corridor/altar becomes visibly unstable;
- boss may chain charge into a different vertical angle;
- safe navigation references remain;
- no long invulnerability;
- no arena-wide permanent damage field.

## Rewards

First eligible defeat:

- guaranteed Superior+ deep R11 gear;
- 2 model-linked signature materials after intake;
- 15% Mythic/signature roll;
- EXP ~20% current next-Lv requirement;
- Class XP ~15%;
- major trophy/display unlock if accepted model supports it;
- Act-III evidence completion where applicable.

No Abyss Token.

---

# 22. Main-story / regional quest flow

R11 content appears over multiple visits.

## Coast chain — `Harbor of Many Roads`

- discover major harbor/freeport;
- learn surface routes, fish/economy, ferry network;
- low/mid-tier regional content independent of Anchor main story.

## Open-sea chain — `Routes Between Islands`

- reef/fort/wreck routes;
- Laviathan progression;
- stronger predator/fishing content;
- sea-fort optional/regional dungeon.

## Late chain — `The Current Below`

- Restoration surface route test shows real benefit;
- Reef Warden reports deep ecological/pressure anomalies;
- Deep-Dive Harness unlocked;
- investigate trench pressure stations;
- surface evidence alone cannot explain the effect.

## `What the Harbor Cannot See`

- abyss temple/main dungeon;
- proves delayed remote consequence;
- isolate automatic deep corridor balancing;
- preserve bounded local route prediction/navigation support.

Abyss Fang is the climax of the deep investigation, not a random monster blocking the story door.

---

# 23. Regional outcome / memory

Shared late-join-safe consequences may include:

- harbor approach beacon/current prediction remains improved;
- one ferry/open-sea route becomes more reliable;
- one deep pressure corridor visibly goes dark/is isolated;
- reef ambience/event pool recovers in one affected area;
- Laviathan handler service expands;
- deep temple repeat shortcut remains available.

Personal consequences:

- coastal/open/deep discoveries;
- Laviathan unlock/registration;
- Deep-Dive Harness unlock;
- R11 late evidence package;
- Riptooth/Abyss Fang first-clear state;
- Fish Codex/trophy records;
- local NPC/dialogue flags.

The sea should remain alive after the story; it is not `fixed` into a static safe lake.

---

# 24. Dynamic event package

## Reef Disturbance

- predator/pressure/ecology event around a specific reef;
- support/combat/objective contribution all valid;
- useful fishing/resource reward, no token.

## Storm-Wreck Signal

- weather/visibility reveals or threatens one authored wreck/event route;
- short exploration/rescue/salvage content;
- not repeated cache spam.

## Harbor Convoy

- compact departure/arrival defense/repair event near route nodes;
- avoids 10-minute escort AI.

## Deep Echo

- late-game pressure/sonar-like signal points toward a large creature or infrastructure event;
- creates Subnautica-style curiosity without objective-beam dependence.

---

# 25. Reward identity

R11 regional rewards span layers.

Coast/open sea:

- bleed/predator interaction;
- ranged/projectile control;
- mobility around water/knockback;
- fishing/resource utility;
- light/medium equipment and accessories;
- seafood/alchemy/cooking value.

Deep:

- WIL/INT/DEX abyssal accessory options;
- pressure/current-themed skills where external VFX supports them;
- deep weapon/catalyst materials;
- defensive/recovery tools for 3D combat;
- selected bleed/weak-point/predator Mythics.

Rules:

- no mandatory water-resistance set;
- no separate aquatic equipment grade;
- gear remains useful outside R11;
- fishing utility never becomes best-in-slot combat power.

---

# 26. Audio / presentation

R11 needs the strongest audio-depth transition in the game so far.

## Harbor/coast

- gull/sea-life equivalents from accepted sources;
- ropes, docks, sails, markets, bells;
- waves and port work;
- music supports busy social space.

## Open sea

- more wind/water, less constant settlement noise;
- distant creature/wreck/storm cues;
- long quiet intervals allowed.

## Deep

- surface/wind disappears;
- low pressure/creature calls/structural resonance;
- sparse high-frequency bioluminescent/ecology detail;
- directional boss/creature cues remain distinct;
- music can become minimal so unknown space matters.

Abyss Fang:

- charge/devour cues must cut through ambience;
- huge-body sounds communicate direction/depth, not just loudness.

No constant sonar ping unless tied to an actual player action/POI.

---

# 27. UI / map considerations

R11 is the first region where depth matters enough to require explicit UI support.

Map/journal rules:

- layer/depth indicator for deep discovered POIs;
- avoid stacking five icons at the same X/Z with no depth context;
- surface/open/deep filters become available after relevant discoveries;
- Fish Codex can show habitat/depth bands after a species is caught;
- Dive Utility state is shown only when relevant, not as a permanent HUD meter;
- no oxygen bar after permanent deep utility unlock because there is no breath-management loop.

Lucifer visual family remains canonical.

---

# 28. Multiplayer / authority

Server owns:

- ferry/port route unlocks;
- Laviathan controller/passenger/Resolve state;
- Dive Utility unlock;
- underwater movement validation;
- aquatic skill adaptation state;
- quest/evidence state;
- predator/boss AI and 3D positions;
- dungeon pressure/current state;
- personal loot/reward claims;
- personal fishing/gathering state.

Required multiplayer tests later:

- Laviathan four-rider seats/controller remain deterministic after reconnect/dismount;
- large multipart sea-creature hitboxes do not crash/desync;
- two players at different depth/progression states retain personal quest correctness;
- Deep-Dive Harness activation and swim speed agree server/client;
- Swim Burst i-frame/movement timing aligns with animation;
- aquatic adapted skills preserve server hit timing;
- Abyss Fang giant-body charge/devour sync for multiple players;
- deep dungeon current state identical for all clients;
- no altar/reward duplication on relog;
- support contribution qualifies underwater.

`MULTIPLAYER TESTED` remains NO until real-client verification.

---

# 29. Performance constraints

- no full-ocean entity simulation;
- aquatic spawn density controlled by loaded region/depth bands;
- ambient fish schools use lightweight presentation where possible;
- Cachalot/Giant Squid/Laviathan multipart entities receive explicit profiling after current 26.2 fixes;
- deep fog/lighting used to bound render load as well as mood;
- no giant particle clouds simulating water currents;
- boss/large predator AI only active near encounter/loaded routes;
- island/harbor NPC counts bounded;
- repeated wrecks/reef models use static/block composition where possible;
- no per-tick global scan for players below a depth threshold; use region/state hooks.

---

# 30. Asset / technical blockers before implementation

R11 is not implementation-ready at the **deep combat presentation** level until all of these close:

1. current 26.2 Fabric Alex aquatic roster in-game review, especially Cachalot/Giant Squid/Laviathan multipart behavior;
2. current Riptooth/Abyss Fang model, animation, hitbox and attack review;
3. exact fish model assignments for 7–10 codex roles;
4. coherent harbor/freeport/island-port architecture;
5. accepted watercraft/ferry model + multiplayer behavior decision;
6. Laviathan mount integration and seating visual review;
7. **Deep-Dive Harness external model / player animation family**;
8. external swim locomotion, Swim Burst, underwater attack/cast/guard animation sources;
9. per-class `AQUATIC_NATIVE / ADAPTED / DISABLED_WITH_FALLBACK` audit;
10. sea-fort final boss external model;
11. abyssal temple architecture / pressure machinery family;
12. abyssal mineral/reef/medicinal resource models;
13. R11 equipment/weapon/accessory visual families;
14. current/fog/pressure/bioluminescent VFX sources;
15. harbor/open-sea/deep audio/music sources;
16. Abyss Fang/Riptooth trophy/signature-material visuals.

No vanilla boat fleet, Guardian/Elder Guardian boss, Drowned enemy, vanilla fish population, static Steve swim pose or particle-only current effect is accepted as the finished solution.

---

# 31. R11 quality acceptance

Real play must prove:

- coast/open/deep feel like three meaningful layers of one region rather than three disconnected maps;
- the harbor and islands make ocean travel worthwhile before late-game diving exists;
- no mandatory route contains minutes of empty water with no decision/landmark/content;
- Laviathan materially improves group/open-water travel without becoming a universal land mount;
- the Deep-Dive Harness removes breath/movement chores without adding a second progression treadmill;
- underwater combat still feels like the player's class, not an unrelated minigame;
- aquatic adapted skills have final-quality animation/VFX and honest hitboxes;
- deep navigation creates mystery without directionless darkness;
- R11 avoids the Skellige repeated-underwater-cache failure completely;
- fishing is the richest collection region without becoming material grind;
- large predators are sparse, readable and impressive;
- Abyss Fang supports true 3D combat without camera/hitbox chaos;
- the delayed remote consequence of Restoration is understood through play/environment, not only exposition;
- post-clear harbor/deep states visibly change;
- multiplayer giant-entity/dive/boss authority is stable;
- performance remains acceptable across harbor density, open water and deep effects.

Verification state:

```text
DESIGN REVIEWED: YES
EXTERNAL REFERENCE REVIEWED: YES
EXACT ASSET INTAKE: PARTIAL / REQUIRED
DEEP COMBAT TECHNICAL/ANIMATION GATE: REQUIRED
CODE REVIEWED: N/A
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
