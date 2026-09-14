# Open-World RPG — Master Game Design Canon

> Status: DESIGN CANON / continuously maintained  
> Final game title: TBD  
> Canon priority: current GitHub `main` > this file > subordinate project docs > older conversations

This file is the single source of truth for gameplay/design decisions. Do not create a competing master design document. When a decision changes, edit the existing section instead of leaving contradictory versions behind.

---

# 1. Vision

Build a large open-world action RPG on Minecraft Java with extremely low dependence on vanilla RPG/progression systems. Minecraft supplies the block world, runtime, input foundation and multiplayer host model; the actual game identity comes from this project.

Core loop:

```text
Explore
→ discover region / POI / settlement / shrine / camp / dungeon / encounter
→ fight / gather / complete objectives
→ gain EXP, currency, equipment, materials and skills
→ alter the build or advance a class
→ challenge more dangerous areas
→ defeat elites / field bosses / dungeon bosses
→ unlock new choices, routes and progression
→ explore again
```

Depth and system interaction take priority over disconnected menus, currencies or filler systems.

---

# 2. Non-negotiable production rules

## 2.1 External-first visuals

No player-facing feature starts with disposable AI-made or generic placeholder art, even in test builds.
From the first visible implementation, important visuals use a deliberately selected external high-quality design, reference or usable asset.

This applies to:

- inventory/equipment;
- skill HUD and ultimate presentation;
- stats/class/advancement screens;
- forge, alchemy, cooking and other workstation screens;
- minimap/world map;
- monsters and bosses;
- weapons, armor and accessories;
- shrines/checkpoints;
- camps, tents and campfires;
- inns, towns and service buildings;
- starting settlement;
- dungeons/ruins;
- mounts;
- resource nodes;
- important VFX/sound direction.

If an external design can legally be used directly and already looks better than a custom redesign, preserve it as intact as practical. Only alter what is required for this game's information, controls, GUI scale and consistency.

Do not create temporary web-game-style panels, generic black translucent boxes, disposable vanilla-entity placeholders or throwaway player-facing screens.

## 2.2 Private-play target / public-repo boundary

The playable project is for private use, not public distribution. The GitHub repository is public.

Therefore:

- local/private-use assets may be used in the owner's local playable instance when their terms permit it;
- non-redistributable asset bytes stay outside GitHub;
- the repo records provenance and local integration instructions instead;
- CC0/MIT/otherwise redistributable assets may be committed under their terms;
- no DRM/access-control bypass or paid-asset piracy.

## 2.3 Code hygiene

After a replacement is appropriately verified, remove superseded dead, duplicate, prototype and unused compatibility code immediately unless still required for save migration, active compatibility or a live feature.

Git history is the archive; the active source tree is not.

---

# 3. Vanilla replacement policy

Replace/remove from the core RPG loop:

- vanilla XP and levels;
- normal vanilla XP-orb drops;
- vanilla enchantment progression as a primary growth system;
- vanilla weapons/armor as meaningful long-term gear;
- vanilla monsters as the primary enemy ecosystem;
- vanilla crafting-grid progression as the main production system;
- vanilla inventory/equipment presentation as the main character interface;
- vanilla cave/strip-mining as the intended core resource-gathering loop;
- Nether/End as mandatory progression gates.

Vanilla blocks/building/environmental interactions may remain where they improve the open-world sandbox without competing with project systems.

---

# 4. Naming

- Experience: `EXP`
- Level: `Lv`
- Player and enemy level display use the same format: `Lv <number>`
- Vanilla green XP/level presentation is not used for RPG progression.

---

# 5. Core resources and recovery

## HP

Primary survival resource. Natural recovery exists but is intentionally slow. Recovery can also come from purchased potions, food, skills/effects and rest.

## Mana

Primary active-skill resource.

- most active skills consume Mana;
- a small concept-driven minority of physical/martial skills may use Stamina instead;
- class/equipment/passives may alter cost, recovery and resource behavior;
- Mana can regenerate naturally, but rest and dedicated effects are much more efficient.

## Stamina

Stamina is primarily a non-skill action resource.

Used by:

- dodge/roll;
- sprinting;
- guard/block impact;
- parry attempts or related defensive actions;
- other non-skill mobility/defensive actions where appropriate.

Rules:

- normal basic attacks cost no Stamina;
- sprinting drains Stamina slowly enough that ordinary exploration remains comfortable;
- Stamina recovery is substantially faster/more responsive than HP recovery;
- maximum Stamina and recovery are valid build axes.

## Rest / food / potions

- Potions are obtained mainly through purchase and appropriate RPG economy sources rather than vanilla crafting-grid dependence.
- Food is useful recovery/buff content rather than hunger busywork.
- Passive regeneration exists but remains slow enough that rest/items matter.
- Camps, inns, shrines or other valid rest points restore resources efficiently.
- Inns are valid world-service buildings when a strong external design/build is selected.

---

# 6. Primary stats

Use six primary stats:

- `VIT` — maximum HP and selected survivability scaling.
- `END` — maximum Stamina, Stamina recovery and selected guard/stability interactions.
- `STR` — heavy/power-oriented physical scaling.
- `DEX` — finesse/ranged/technical physical scaling and selected precision/critical interactions.
- `INT` — offensive magic / magical weapon and skill scaling.
- `WIL` — maximum Mana, Mana sustain/recovery and selected magical/status resilience.

Attack Speed remains an independent build axis rather than being universally dictated by DEX.

Derived stats may include Physical Power, Magic Power, Defense, Magic Resistance, Critical Chance/Damage, Attack Speed, Movement Speed, Stamina/Mana Recovery, Guard Strength/Stability, healing and elemental/status modifiers.

Exact formulas are data-driven balance work.

---

# 7. Combat

Combat mixes fast action-RPG flow, skill-driven builds and readable defensive play.

## Basic attack

- Left click: weapon basic attack/combo.
- No Stamina cost.
- Weapon families differ in animation timing, reach, speed, hit cadence, hit count and scaling.
- Attack Speed is a serious independent build axis.

## No universal heavy attack

There is no generic heavy-attack input.
Right click remains for held-item behavior such as shield/guard, weapon-specific alternate use, consumables and contextual interactions.

## Dodge / roll

- dedicated rebindable key;
- directional movement;
- consumes Stamina;
- useful for both mobility and evasion;
- visible animation/motion must match actual evasion or i-frame timing.

## Guard / parry

- core combat mechanics;
- blocking consumes Stamina through impact/defensive cost;
- parry has readable timing and strong success feedback;
- suitable weapons, shields and class effects may differ in guard/parry performance;
- proven external implementations should be reused/adapted where technically and legally suitable.

## Stagger / poise

Use a layered system:

- normal enemies use simpler stagger resistance/reaction rules;
- elites and bosses have meaningful poise/stagger gauges or equivalent;
- heavy weapons, impact attacks, selected skills and successful parries can reduce poise strongly;
- breaking poise creates a readable punish/damage window;
- animation and actual vulnerability state must match.

## Skill loadout

- 4 normal active skills
- 1 ultimate
- dodge/basic attack do not consume skill slots
- most active skills consume Mana; a small concept-driven minority may use Stamina

## Ultimate

Ultimate activation uses a hybrid model:

- useful combat contribution builds charge/gauge;
- charge sources are role-aware, not damage-only;
- attacking, guarding/parrying, healing/support, control and other relevant actions may contribute;
- after use there is an anti-spam cooldown/lockout;
- only one ultimate can be equipped, so it should be distinctly stronger, more spectacular and more build-defining than normal skills.

Ultimate HUD uses an externally selected final-quality design from the first playable version.

---

# 8. Input policy

All project actions are rebindable.
Default bindings must not conflict with important Minecraft or Essential controls.
Low-value vanilla bindings may be repurposed if their original action is irrelevant and the user can rebind it.

Before locking defaults, audit current Minecraft defaults, Essential defaults, required companion-mod defaults and the project's complete action list.

---

# 9. Class system

## 9.1 Five root classes

Use five clear fantasy RPG root roles:

1. **전사** — direct melee pressure, weapon mastery and aggressive/defensive melee branches.
2. **사냥꾼** — ranged precision, mobility, field utility and possible early-firearm specialization.
3. **성직자** — healing, buffs, protection and holy/support combat.
4. **마도사** — offensive magic, elemental/arcane damage and control.
5. **수호자** — defense, aggro/control, ally protection and counter-oriented play.

Weapons are broadly class-independent. Effectiveness comes from stat scaling, cadence, class passives/skills and equipment synergy rather than hard weapon locks.

## 9.2 Advancement depth / branching

- initial production target: about five advancement stages per root-class line;
- architecture remains open for more later;
- never label a stage as `final`;
- use a depth-first structure;
- an early/mid major specialization branch changes playstyle materially;
- the chosen route then continues through multiple deeper stages;
- hidden/special advancements may require bosses, quests, stats, items or discoveries;
- advancement adds mechanics/passives/skill behavior/build identity, not only numbers.

Example principle: Warrior can branch toward damage pressure or defensive/tank play.

## 9.3 Persistent class history and switching

Progress is stored separately for every class line.

- an untrained class starts from its beginning;
- switching back restores saved advancement/progress/unlocked content;
- class switching costs the core currency/resource;
- cost scales with player `Lv` but has a cap;
- no class-switch cooldown;
- switching never erases learned history.

## 9.4 Skills and passives

Skill acquisition is mixed:

- core class skills come from class progression/advancement;
- exploration, bosses, quests, NPCs and rare finds may unlock additional skills or meaningful variants.

Passives:

- use a tree/investment structure;
- all passives unlocked on the currently active class apply;
- no passive-slot limit;
- class-specific passives never remain active after switching away from that class;
- each class retains its own saved passive tree and restores it only while active;
- cross-class passive stacking is forbidden unless a future separate global-progression system explicitly introduces it.

---

# 10. Shrines / world services

Class change, respec and advancement happen through believable world facilities such as shrines, sanctums, guild halls or trainers rather than a settings button.

Possible services include:

- class change;
- class advancement;
- respec;
- respawn checkpoint;
- fast travel;
- build management;
- selected rituals/progression interactions.

Do not force every service into one universal menu building if distributed locations create a better world.
All important structures use selected external high-quality builds/assets from the first visible implementation.

---

# 11. Death and respawn

At respawn, choose one penalty:

1. pay part of core currency; or
2. lose part of current EXP progress.

EXP loss can never reduce an already-earned `Lv`. The EXP floor is the start of the current `Lv`.

Exact percentages/caps, insufficient-currency behavior, boss-arena handling and penalty-reduction items remain balance decisions.

## Multiplayer down / revive

- lethal damage can enter a temporary downed state;
- teammates can revive the downed player within the rescue window;
- failed rescue leads to normal shrine/checkpoint respawn and the chosen death penalty;
- single-player does not need a fake waiting phase and may proceed directly to defeat/respawn;
- revive behavior is server-authoritative.

---

# 12. Inventory and equipment

The vanilla inventory screen is replaced as the main RPG character interface by an externally selected final-quality RPG inventory/equipment design.

Canonical equipment target:

- Main Weapon
- Off-hand
- Head
- Chest
- Legs
- Gloves
- Boots
- Necklace
- Ring 1
- Ring 2
- Charm
- Relic

Two-handed weapons may disable or repurpose the off-hand slot.
Exact visual placement follows the selected external GUI rather than an internally improvised layout.
Accessory/Charm/Relic slots should create build effects, not exist only for tiny percentage bonuses.

## Inventory capacity

- no inventory-weight system;
- inventory uses a larger RPG-oriented slot capacity than vanilla;
- capacity can be expanded through progression/upgrades;
- materials should have a dedicated material pouch/category or equivalent organization so gathering does not constantly fill the main inventory;
- item stacks are not globally limited to vanilla's 64-item cap;
- stackable materials/consumables may use substantially larger stack caps appropriate to RPG play;
- exact starting slot count, expansion milestones and per-category stack caps remain balance decisions.

## Durability

Equipment has no ordinary vanilla-style durability/repair chore.
A future special mechanic may use wear only if it creates real gameplay value.

---

# 13. Weapons and item generation

## Weapon families

Prioritize coherent external model/design packs and good animation support.
Candidate families:

- sword;
- greatsword;
- dagger / dual blades;
- spear / polearm;
- axe;
- hammer / mace;
- bow;
- crossbow;
- staff;
- catalyst / grimoire / wand-type magical focus;
- shield and specialized off-hands;
- early black-powder firearms such as hand cannon, matchlock/flintlock pistol or musket when the Hunter line and external design quality justify it.

Modern/automatic firearms are not part of the current fantasy baseline. More advanced firearms may be reconsidered only for a future advanced-tech/cyberpunk region or dedicated later class.

## Item generation / drops

Use a hybrid item model and probabilistic loot tables:

- normal enemies, elites, dungeons and bosses use weighted/probability-based drops;
- common field equipment may roll controlled affixes;
- named/boss/signature gear may have fixed identity, visuals and unique mechanics while still being probabilistic drops;
- progression-critical quest/key items must not softlock progress through bad RNG;
- first-clear progression rewards may be deterministic while farmable gear remains probabilistic;
- stronger grade does not always mean strict numeric replacement.

## Multiplayer loot ownership

All dropped combat/loot rewards are **personal per player** in multiplayer.
No shared-floor race where another player can take the reward first.

## Item grade structure

Use about five grades, but avoid the tired `Common / Rare / Epic / Legendary` naming set.
Player-facing names are chosen with the world's lore/design language later.
Until then use neutral internal tier IDs.

---

# 14. Damage types and status effects

Use medium complexity.

Physical identity may distinguish slash / pierce / impact where useful.
Core magical/elemental families may include fire, frost, lightning, poison/corrosion and arcane-like effects.
Status effects must be mechanically distinct rather than differently colored copies of the same DOT.

---

# 15. Workstations / professions / production

Major RPG production does not rely on the vanilla crafting grid.
Potential systems include forge/smithing, alchemy, cooking and justified enhancement/customization.

Use **light profession mastery** rather than giant mandatory profession grinds.
Smithing, alchemy and cooking may improve through use and unlock recipes/quality/options, but progression must not require repetitive mass-crafting for dozens of hours.

Each important workstation gets a purpose-built screen selected from an external proven game/mod design first. If a directly usable design is visually strong, preserve it rather than redesigning it into a weaker UI.
Physical workstation/building appearance follows the same external-first rule.

Avoid production steps that only add clicks.

---

# 16. Open world / settlements

Use a high-quality external open-world RPG map/terrain solution rather than hand-authoring the entire continent from zero.

Target about **12 major regions**, adjustable to roughly 10–14 if the selected external map's geography strongly supports a different count.
Each region contains subregions/landmarks/POIs and defines environmental identity, danger/Lv profile, enemies, elites, signature threats, resources, gear identity, settlements, dungeons, camps/checkpoints, quests/events and traversal gimmicks.

## Enemy Lv model

Use **regional Lv bands with only narrow contextual adjustment**.
Do not scale every enemy to the player.
A low-Lv region remains low-Lv later; a dangerous region remains dangerous when entered early.

## Starting settlement

The game has a memorable starting village/settlement as the first safe social/service hub.
Its final name and lore are decided later; do not use a generic vanilla village as the finished result.

Possible services:

- inn / tavern;
- forge / smith;
- merchant / market;
- alchemy / healer;
- adventurer / quest guild;
- class/trainer/guild access where appropriate;
- bank/storage;
- stable/mount access;
- nearby shrine/checkpoint;
- quest/NPC hooks.

The village layout and buildings come from a coherent external high-quality village/build family or map/schematic.
Avoid stitching together unrelated building styles when a coherent pack/source exists.

## Town / service-building baseline

Default palette, adjusted per settlement rather than copied everywhere:

- inn/tavern — rest, food, local information, selected time/checkpoint services;
- forge/smithy — weapon/armor production and upgrades;
- alchemy/healer — potions, treatment and alchemy;
- market/merchant district — buying/selling and regional goods;
- adventurer/quest guild — contracts, jobs and world hooks;
- shrine/sanctum — checkpoint, selected travel/class/ritual services;
- stable — mount purchase/management;
- bank/storage — player storage and economy support;
- class hall/trainer where appropriate;
- region-specific special facilities when they create real gameplay.

All important buildings are external-first designs/assets.

## Factions / reputation

Use reputation only for selected meaningful factions/regions.
Do not create many reputation bars by default.
Reputation should affect relationships, services, quests, prices, access or rewards where it matters.

## Day / night / weather

Use moderate gameplay impact:

- selected enemies, rare hunts, resources, events and boss behavior may depend on time/weather;
- weather may alter region atmosphere and selected encounters;
- ordinary play must not require tedious waiting for the correct clock/weather state.

---

# 17. Map discovery / navigation / fast travel

Use a middle-ground discovery model:

- general terrain/world shape is visible enough for navigation;
- detailed POIs, dungeons, shrines, special bosses and discoveries are revealed through exploration;
- minimap does not reveal every enemy or undiscovered reward.

Fast travel is available between discovered **shrines and major settlements/hubs**.
Ordinary camps and minor POIs are not universal teleport nodes.
Combat prevents fast travel.

Minimap/world-map visuals and implementation are external-first.

---

# 18. Mounts and traversal

Use ground mounts as a real progression/travel system.

- mounts can differ meaningfully in speed, acceleration/handling, toughness and role;
- some mounts may support mounted combat or combat utility;
- use external high-quality models/animations and proven mount implementations/code where suitable;
- mounts are not cosmetic reskins with identical stats;
- later progression unlocks substantially faster traversal;
- late-game traversal may include flying mounts or another high-speed system;
- flight must not arrive so early that terrain, danger and dungeon approaches become irrelevant.

---

# 19. Enemies, bosses, world events and dungeons

Custom/project enemies dominate the combat roster.
Important enemies require readable states, attack telegraphs, recovery, custom visuals/animation/VFX/sound appropriate to importance.

Accepted iconic directions include:

- desert earth/sand-worm-style giant monsters;
- dragons;
- region-specific giant creatures and field bosses.

## Dynamic world events

Use region-specific dynamic events at controlled frequency.
Examples may include caravan attacks, rare dragon appearances, night creatures, magical weather/incursions or roaming major monsters.
Events should feel discovered in the world, not like constant MMORPG chores.

## Dungeon replay

Dungeons are replayable.
First clear provides unique/progression-significant rewards where appropriate; later clears provide probabilistic gear, materials and rare drops.
External dungeon architecture is retained where strong while encounters, enemies, bosses and rewards are project-specific.

## Field/world bosses

Field/world bosses can reappear under suitable timers/conditions.
First defeat may have unique progression rewards; repeats focus on probabilistic rare drops/materials.
Avoid rapid repetitive respawn loops.

---

# 20. Quests

Use a mixed structure:

- main narrative for world/system introduction and major progression;
- regional quest chains that give each area identity;
- free exploration/discovery without constant quest-marker following.

The main story guides without turning the open world into a linear corridor.

---

# 21. Gathering / resources

The intended gathering loop is **open-world RPG gathering**, not vanilla cave mining or strip-mining.

## Gathering categories

Use a manageable set:

- mining;
- herbalism / plant gathering;
- forestry / timber gathering;
- fishing / foraging-style resource gathering.

These may have light mastery/progression, but gathering is not a massive mandatory profession grind.

## Resource nodes

- ore veins/mineral deposits, herbs, timber resources, food ingredients and rare materials appear as recognizable world gathering nodes/resource points;
- nodes are placed/generated according to region, terrain and ecology rather than hidden behind thousands of ordinary stone blocks;
- gathering emphasizes discovery, route choice and region knowledge;
- vanilla block breaking can remain possible as sandbox interaction, but it is not the intended source of core RPG resources.

## Multiplayer ownership

Resource nodes use **personal gathering state** in multiplayer.
One player gathering a node does not deny the same node to another player simply because they arrived a moment later.

## Gathering tools

Use dedicated gathering tools such as pickaxe, axe, harvesting knife/sickle and fishing rod as appropriate.

- tools are project/RPG tools, not the core vanilla mining progression;
- routine tool durability is not used;
- tools should not occupy normal combat equipment slots during ordinary play;
- a tool pouch/equipment category or context-sensitive interaction is preferred;
- tool quality may affect speed/yield/access when it creates meaningful progression.

## Regeneration

- common nodes regenerate after a suitable time/condition;
- rare nodes use longer regeneration and/or special conditions, events, dangerous locations or boss-linked access;
- rare resource design should create reasons to explore rather than simply wait on a short timer;
- exact timers remain balance work.

Node visuals/models/interactions are external-first when good assets/designs exist.
External node-system implementations should be studied instead of reinventing regeneration poorly.

---

# 22. Camps / housing / rest

Travel should have a sense of journey and temporary shelter.

## Camps

Camps are **quick-build temporary field infrastructure**.

- player spends defined materials/resources to deploy/build a camp rapidly rather than manually constructing it block by block;
- visuals use an external final-quality tent/campfire/shelter design from the first implementation;
- camps provide strong rest/resource recovery and selected field utility;
- camp construction supports exploration rather than becoming a construction grind;
- ordinary camps are not universal fast-travel nodes.

## Housing

Permanent player housing is separate from camps.

- towns/settlements can contain empty/purchasable houses;
- player buys a residence rather than turning a field camp into a full permanent base;
- house shells/interiors use coherent external building designs/assets;
- house functions include rest, storage, furnishing/decor and trophy/collection display;
- the house does not automatically replace every town service such as forge/alchemy, because those service buildings need to remain meaningful world locations.

---

# 23. Multiplayer / Essential

Essential-friendly multiplayer is a major usability goal.
Connection convenience does not own game authority.

Server-authoritative state includes damage, item ownership, currency, EXP/Lv, skill cost/success, class/progression, quests, world state and saves.

Combat/loot rewards are personal per player.
Resource gathering is also personal per player at the node-availability level.

Do not claim multiplayer quality until actually tested.

---

# 24. Economy and merchants

## Core currency

Use **one primary numeric currency** for the ordinary economy.

- player-facing currency name/icon are not locked yet and should be chosen with world lore;
- avoid copper/silver/gold denomination conversion unless it later proves valuable;
- add special currencies/tokens only when a specific activity genuinely needs a distinct reward loop;
- do not multiply currencies merely to make the game look larger.

The core currency supports at least class switching, death-penalty choice, merchants, housing and selected services.

## Merchant stock

Merchant inventory is **mostly randomized/rotating**, but essential items and explicitly designated goods remain fixed.

Examples of fixed or reliably available stock may include:

- core potions/recovery necessities;
- required basic supplies;
- specific progression-safe items that should not disappear because of RNG.

Other equipment, regional goods, rare materials or special finds may rotate or randomize according to merchant/region rules.
Exact refresh cadence and pool composition remain balance decisions.

---

# 25. Data-driven / maintainability

Repeatedly tuned values should be data-driven where practical:

- player/enemy stats;
- Lv curves;
- skill parameters;
- Mana/Stamina costs;
- equipment scaling;
- affixes and drop probabilities;
- merchant pools/refresh data;
- inventory capacity/stack caps;
- loot/encounters;
- resource nodes and respawn rules;
- region parameters;
- advancement values;
- dungeon rewards;
- quests/dialogue.

Code owns rules; data owns content/tuning where feasible.

---

# 26. Current locked decisions

Major locked decisions as of 2026-09-14:

- private-use large open-world action RPG with very low vanilla progression dependence;
- external-first visuals/assets from the first visible/test implementation;
- no temporary player-facing design;
- dead/superseded/duplicate code removed after safe replacement;
- `EXP` / `Lv` notation and removal of vanilla XP progression/drop loop;
- HP + Mana + Stamina; Stamina is primarily non-skill action resource; basic attack costs no Stamina;
- dodge, guard, parry and layered stagger/poise combat;
- 4 active skills + 1 high-impact hybrid-charge ultimate;
- VIT / END / STR / DEX / INT / WIL primary stats;
- five root classes: 전사 / 사냥꾼 / 성직자 / 마도사 / 수호자;
- deep ~5-stage initial advancement target, one major playstyle-changing branch, no `final` terminology;
- persistent per-class progression with paid Lv-scaled/capped switching and no switch cooldown;
- all unlocked passives of the active class apply; inactive-class passives never leak across;
- broad weapon freedom including Hunter-compatible black-powder firearms; modern firearms excluded from baseline;
- 12-slot RPG equipment target and no routine durability chore;
- larger expandable RPG inventory, no weight system, material pouch/category and stack sizes above vanilla 64 where appropriate;
- hybrid random-affix + named/signature equipment; about five non-cliché grades later;
- medium damage/status complexity;
- light smithing/alchemy/cooking mastery;
- regional Lv bands, roughly 12 major regions, discovered POIs, shrine/major-hub fast travel;
- ground mounts with varied speed/handling/combat roles and later high-speed/flying traversal;
- mixed main/regional/free-exploration quests, dynamic region events, replayable dungeons, respawning field/world bosses;
- probabilistic drops with deterministic protection for progression-critical items;
- multiplayer down/revive and fully personal loot;
- RPG field resource nodes instead of cave/strip-mining as the core gathering loop;
- personal node gathering state in multiplayer;
- mining/herbalism/forestry/fishing-foraging categories with light mastery;
- dedicated no-routine-durability gathering tools, separate from combat slots;
- common node regeneration and slower/conditional rare-node regeneration;
- one primary currency; player-facing name chosen later with world lore;
- merchants use mostly randomized stock while potions/essentials/designated items remain reliably available;
- quick-build material-cost camps;
- permanent houses are purchased separately in settlements and provide rest/storage/decor/trophy functions;
- selected faction/reputation systems only where meaningful;
- moderate day/night/weather gameplay effects;
- starting settlement and all important buildings use coherent external architecture/designs;
- Essential-friendly server-authoritative multiplayer target.

---

# 27. Next design queue for the next chat

Do not re-decide the locked systems above. Continue from here.

Recommended next batch:

1. **Starting settlement gameplay design** — which services exist immediately, what unlocks later, first NPC/service flow, house purchase timing.
2. **Five root-class advancement trees** — actual first major branch for 전사 / 사냥꾼 / 성직자 / 마도사 / 수호자, then deeper advancement direction without using `final` terminology.
3. **External open-world map selection** — shortlist actual usable/downloadable RPG maps/terrain and choose the map before naming all 12 regions.
4. **First-region content package** — once the map is chosen: Lv band, common mobs, elite, field boss, dungeon, resources, quests, shrine/camp and signature rewards.
5. **External UI selection** — inventory/equipment, skill HUD, forge, alchemy, cooking, class/advancement and death/respawn screens; choose proven final designs before implementation.
6. **Economy balance** — currency sources/sinks, class-switch cost curve, death penalty %, house prices, shop refresh cadence.
7. **Inventory numbers** — starting slot count, expansion steps, material-pouch behavior and practical stack caps above 64.
8. **Loot economy** — affix count/ranges, grade probabilities, boss-drop rates, bad-luck protection only where needed.
9. **Mount progression** — first ground mount, speed classes, combat mounts, later high-speed/flying unlock conditions.
10. **Final keybind audit** only after the complete frequent-action list is known; important Minecraft/Essential keys must not conflict.

When design direction becomes unclear, research real open-world RPGs, open-source RPGs and large Minecraft RPG mods before inventing filler systems.
