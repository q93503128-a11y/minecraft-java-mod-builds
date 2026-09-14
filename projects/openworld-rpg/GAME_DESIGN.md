# Open-World RPG — Master Game Design Canon

> Status: DESIGN CANON / continuously maintained  
> Final game title: TBD  
> Canon priority: current GitHub `main` > this file > subordinate project docs > older conversations

This file is the single source of truth for gameplay/design decisions. Do not create a competing master design document. When a decision changes, edit the old section instead of leaving contradictory versions behind.

---

# 1. Vision

Build a large open-world action RPG on Minecraft Java with extremely low dependence on vanilla RPG/progression systems.
Minecraft supplies the block world, simulation/runtime, input foundation and multiplayer host model; the actual game identity comes from this project.

The target is not "Minecraft with RPG stats". It should feel like a standalone open-world RPG whose world happens to use Minecraft technology and blocks.

## Core loop

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

## 2.1 External-first visual rule

No player-facing feature starts with disposable AI-made or generic placeholder art, even in test builds.
From its first visible version, every important visual feature must use a deliberately selected external high-quality design, reference or usable asset.

This rule applies to:

- inventory and equipment screens;
- skill HUD and ultimate presentation;
- stats/class/advancement screens;
- forge, alchemy, cooking and other workstation screens;
- minimap and world-map presentation;
- monsters and bosses;
- weapons, shields, armor and accessories;
- shrines/checkpoints;
- camps, tents and campfires;
- towns, shops, guild-like facilities and service buildings;
- dungeons, ruins and major structures;
- important VFX and sound direction.

If an external design can legally be used directly and already looks better than a custom redesign, retain the proven design as intact as practical. Only make changes needed for this game's information, controls, resolution/GUI scale and consistency.

Do not create temporary black translucent panels, arbitrary colored borders, generic web-game cards, disposable vanilla-entity placeholders or throwaway player-facing screens.

## 2.2 Private-play target and public-repository boundary

The playable project is intended for private use, not public distribution.
However, this GitHub repository is public.

Therefore:

- assets licensed for private/local use may be used in the owner's local playable instance when their terms allow it;
- those bytes must not be committed to the public repository if redistribution is not permitted;
- the repo records provenance, source links and local integration instructions instead;
- CC0/MIT/otherwise redistributable assets may be committed when their terms allow it;
- paid, ripped, DRM-bypassed or access-control-bypassed assets are never acceptable.

## 2.3 Code hygiene

The active source tree should contain current implementation, not abandoned layers.
After a replacement is appropriately verified, remove superseded dead, duplicate, prototype and unused compatibility code immediately unless it is still required for save migration, active compatibility or another live feature.

Git history is the archive. The source tree is not.

---

# 3. Vanilla replacement policy

The project intentionally removes vanilla progression from the main RPG loop.

## Replaced or removed from the core loop

- vanilla XP and vanilla levels;
- normal vanilla XP-orb drops;
- vanilla enchantment progression as a primary growth system;
- vanilla weapons and armor as meaningful long-term RPG gear;
- vanilla monsters as the primary enemy roster;
- vanilla crafting-grid progression as the main production system;
- vanilla inventory/equipment presentation as the main character interface;
- Nether/End as mandatory progression gates.

Vanilla blocks, building interaction and environmental mechanics may remain where they improve the open-world sandbox without competing with the RPG systems.

---

# 4. Naming and player-facing notation

Fixed notation:

- Experience: `EXP`
- Level: `Lv`
- Player: `Lv <number>`
- Enemy: `Lv <number>` using the same notation

Examples:

```text
Lv 18
Lv 42 Sand Wyrm
```

The vanilla green XP/level presentation is not used for the RPG progression system.

---

# 5. Character resources

The character uses HP, Mana and Stamina.

## 5.1 HP

Primary survival resource. Exact scaling, healing and out-of-combat recovery remain balance decisions.

## 5.2 Mana

Primary active-skill resource.

Rules:

- most active skills consume Mana;
- Mana is not mandatory for every skill;
- a small concept-driven minority of martial/physical skills may consume Stamina instead;
- classes, equipment and passives may alter cost, regeneration and resource behavior.

## 5.3 Stamina

Stamina is primarily for non-skill physical actions.

Used by:

- dodge/roll;
- sprinting;
- guard impact / blocking;
- parry attempts or related defensive actions;
- other non-skill mobility/defensive actions where appropriate.

Rules:

- normal basic attacks cost no Stamina;
- sprinting drains Stamina slowly enough that ordinary exploration remains comfortable;
- dodge and defense must matter in combat without turning travel into resource micromanagement;
- maximum Stamina and recovery are valid progression/build axes.

---

# 6. Primary stats

Use six primary stats. Keep many derived combat stats independent so one primary stat does not become universally optimal.

## VIT — Vitality

- maximum HP;
- selected survivability scaling.

## END — Endurance

- maximum Stamina;
- Stamina recovery;
- selected guard/stability interactions.

## STR — Strength

- physical scaling for heavy/power-oriented weapons and skills;
- selected STR requirements/scaling.

## DEX — Dexterity

- physical scaling for finesse/ranged/technical weapons and skills;
- selected precision/critical interactions.

DEX does **not** automatically become the universal Attack Speed stat.

## INT — Intelligence

- magical/offensive spell scaling;
- magic-oriented weapon and skill requirements/scaling.

## WIL — Willpower

- maximum Mana;
- Mana recovery/sustain;
- selected magical/status resilience and resource-control interactions.

## Derived stats

Possible derived stats include:

- Physical Power;
- Magic Power;
- Defense;
- Magic Defense / Resistance;
- Critical Chance;
- Critical Damage;
- Attack Speed;
- Movement Speed;
- Stamina Recovery;
- Mana Recovery;
- Guard Strength / Stability;
- elemental/status modifiers;
- healing modifiers;
- cooldown modifiers where appropriate.

Exact formulas remain data-driven balance work.

---

# 7. Combat system

Combat mixes fast action-RPG flow, skill-driven builds and readable defensive play.

## 7.1 Basic attack

- Left click: weapon basic attack / combo chain.
- No Stamina cost.
- Weapon families differ in animation timing, reach, speed, hit cadence, hit count and scaling.
- Attack Speed is a serious independent build axis.
- Some classes/builds may reward hit count, on-hit frequency or rapid combo cadence.

## 7.2 No universal heavy attack

There is no generic heavy-attack input.
Right click remains available for the held item's natural behavior such as shield/guard, weapon-specific alternate use, consumable or contextual interaction.

Powerful weapon actions may exist as weapon-specific mechanics or skills, not as a global heavy attack.

## 7.3 Dodge / roll

A dedicated rebindable dodge key is mandatory.

Goals:

- directional movement based on input;
- useful for both mobility and evasion;
- consumes Stamina;
- readable invulnerability/evasion timing if playtesting supports i-frames;
- animation, visible motion and actual invulnerability timing must match.

## 7.4 Guard and parry

Guard and parry are core mechanics.

Goals:

- clear telegraph and timing;
- blocking consumes Stamina through impact or related cost;
- parry has a readable timing window and strong success feedback;
- failure is understandable rather than random;
- weapons, shields and class effects may vary guard/parry performance;
- proven external implementations should be reused or adapted where technically and legally suitable.

## 7.5 Skill loadout

Equipped combat skills are fixed to:

- 4 normal active skills;
- 1 ultimate.

Dodge and basic attack do not consume skill slots.
Most active skills consume Mana, with a small concept-driven minority allowed to consume Stamina.

## 7.6 Ultimate economy

Ultimate activation uses a **hybrid system**.

The intended structure is:

- an ultimate requires a combat-generated charge/gauge or equivalent activity-based condition;
- it also has a cooldown or anti-spam lockout;
- exact charge sources, cooldown and whether Mana participates are class/skill balance decisions.

The one equipped ultimate should be substantially stronger, more spectacular and more build-defining than a normal active skill.
The ultimate HUD must use a selected external high-quality design from the first playable implementation.

---

# 8. Input and keybind policy

All project actions must be rebindable.
Default bindings must respect Minecraft's important controls.

## Preserve by default

Do not overwrite high-frequency foundational controls unless there is an explicit later decision:

- WASD movement;
- jump;
- attack/use;
- inventory;
- sneak;
- sprint;
- hotbar 1–9;
- core menu/chat interaction needed for normal play.

## May repurpose when justified

Low-importance vanilla bindings may be overwritten if the project replacement makes the vanilla action irrelevant or if the user can rebind it easily.

Before locking default bindings, audit:

- current Minecraft defaults;
- Essential defaults;
- required companion-mod defaults;
- this project's full action list.

No skill or combat action may silently conflict with an important Minecraft or Essential binding.

A later input-design pass will lock defaults for dodge, 4 skills, ultimate, RPG character screen, world map, quest log and other frequent actions.

---

# 9. Class and advancement structure

Use a **deep advancement model** with relatively few root classes and many advancement stages.

Important terminology rule: do **not** label any advancement as "final". The project may expand later.

## 9.1 Principles

- player chooses an initial/root class;
- root class count remains relatively limited;
- each root class advances repeatedly across many stages;
- advancement should create new mechanics, passives, skill behavior and build identity, not only larger numbers;
- weapons remain broadly class-independent;
- practical effectiveness comes from stats, scaling, attack cadence, passives, skills and equipment synergy.

## 9.2 Class history and switching

Class progression is persistent per class line.

When the player changes to another class:

- the newly selected class begins from its own existing progress, or from the beginning if never trained;
- the previous class's advancement, class-specific progress and unlocked class content are saved;
- returning to the previous class restores that class's saved progress rather than forcing a restart;
- class switching requires an in-world service and a currency/resource cost;
- switching should not erase learned history merely to create grind.

This creates long-term multi-class collection/progression without making the current class permanent.

## 9.3 Branch topology

Canonical direction is **depth-first**:

- few root classes;
- an early meaningful branch or specialization choice may exist;
- the chosen class line then continues through multiple later advancement stages;
- hidden/special advancement routes may branch under specific world, stat, boss, quest or item conditions;
- advancement architecture must remain extensible so new stages can be added later without renaming an older stage as "final".

Exact root classes and advancement trees are not locked yet.

---

# 10. Shrines, class services and checkpoints

Class change/respec/advancement should occur through believable in-world locations such as shrines, sanctums, guild halls, trainers or other role-appropriate facilities rather than a generic settings button.

Possible services include:

- class change;
- class advancement;
- respec;
- respawn checkpoint;
- fast-travel unlock/use;
- build management;
- selected ritual/progression interactions.

Do not force every service into one universal menu building if distributed world locations create better exploration and world identity.

All such structures use external high-quality builds/assets from the first visible implementation. No generic placeholder altar or particle pillar.

---

# 11. Death and respawn

On death, the player respawns through the active checkpoint/shrine structure.

At respawn, choose one penalty:

1. pay a portion of core currency; or
2. lose a portion of current EXP progress.

Locked rule: **EXP loss can never reduce an already-earned Lv.**
The player's EXP floor is the start of the current Lv.

Still to balance:

- percentage/cap of currency loss;
- percentage/cap of EXP loss;
- insufficient-currency behavior;
- boss-arena handling;
- multiplayer revive/respawn behavior;
- whether special items can reduce the penalty.

The choice must matter without destroying hours of progression.

---

# 12. Inventory and equipment

The vanilla inventory screen is not the final RPG inventory.
Use a dedicated RPG inventory/equipment presentation based on a selected external implementation/design.

Requirements:

- equipment slots have distinct gameplay purpose;
- accessory slots create build effects, not only small percentage bonuses;
- final visual layout is selected externally before implementation;
- Minecraft GUI Scale and mouse/keyboard usability are tested from the start;
- no temporary generic RPG panel is allowed.

Exact slot layout remains undecided.

---

# 13. Weapons and equipment philosophy

The game uses its own RPG equipment ecosystem.
Vanilla gear is not meaningful long-term progression gear.

## 13.1 Weapon diversity

Use a broad weapon roster, but prioritize families for which strong external models/designs and suitable animation references/assets are actually available.

Potential families to evaluate include:

- sword;
- greatsword;
- dagger / dual blades;
- spear / polearm;
- axe;
- hammer / mace;
- bow;
- crossbow or mechanical ranged weapon if visually supported;
- staff;
- wand / catalyst;
- spellblade / magic melee family;
- shield as defensive equipment;
- additional exotic families only when external design quality and gameplay identity justify them.

A family is not adopted merely to increase the count.

Weapons may differ by:

- physical/magical scaling;
- attack speed;
- reach;
- hit count;
- combo behavior;
- animation cadence;
- guard properties;
- special effects;
- class/passive/accessory synergy.

Higher tier does not always mean strict numeric replacement. Build identity and mechanics matter.

## 13.2 External weapon design rule

Weapon appearance should come from selected external usable assets or high-quality references. Prefer coherent packs/visual families over mixing unrelated art styles.

---

# 14. Workstations and production

Major RPG production does not rely on the vanilla crafting-grid experience.

Potential systems:

- forge / smithing;
- alchemy;
- cooking;
- equipment enhancement/customization when justified;
- specialized production only when it connects to the main loop.

Rules:

- each important workstation gets a purpose-built screen;
- do not reuse a generic project GUI merely because it exists;
- select an external proven game/mod UI design first;
- if direct use is permitted and it already works visually, preserve it rather than redesigning it into a weaker UI;
- physical workstation/building appearance follows the same external-first rule;
- avoid extra production steps that only add clicks.

---

# 15. Open world and region density

Use a high-quality external open-world RPG map/terrain solution rather than hand-authoring the entire continent from zero.

## 15.1 Region-density target

Canonical target: **about 12 major regions**, adjustable to roughly 10–14 if the selected external map's geography strongly supports a different count.

Each major region should usually contain several subregions, landmarks and POIs rather than behaving as one uniform biome.

Do not prewrite all region concepts before map selection. Region identity should follow the actual terrain so mountains, coasts, deserts, forests and ruins feel naturally placed.

## 15.2 Region package

Each major region eventually defines:

- environmental identity;
- danger/Lv profile;
- common enemy families;
- elite/rare enemies;
- signature large threat or field boss where appropriate;
- materials/resources;
- item/gear identity;
- POIs and settlements;
- dungeon opportunities;
- camps/checkpoints/shrines;
- quests/events;
- traversal/environment gimmicks where valuable.

Avoid universal full level scaling. High-danger regions should truly be dangerous, though skilled players may attempt them early.

---

# 16. Enemy ecosystem

Custom/project enemies dominate the main combat roster.
Vanilla mobs are not the primary progression ecosystem.

Enemies need readable states, attack telegraphs, recovery and reactions.
Important enemies require model, animation, VFX and sound quality appropriate to their role.

Accepted regional monster directions include:

- giant earth/sand worm in desert terrain;
- dragons as major predators/bosses;
- region-specific giants, beasts, magical creatures and field bosses.

These threats belong to the ecology and progression rather than existing only as spectacle.
A desert worm, for example, needs meaningful subterranean movement, emergence telegraphs and encounter logic rather than behaving like a reskinned surface melee mob.

---

# 17. Bosses

Boss categories may include:

- dungeon bosses;
- regional/field bosses;
- rare roaming threats;
- major story/world threats.

Boss standards:

- readable telegraphs;
- visible range aligned with hit detection;
- meaningful recovery/punish windows;
- phase changes that alter play rather than only increase numbers;
- custom model/animation/VFX/sound appropriate to importance;
- rewards that unlock build or progression choices.

Dragons are proper aerial/ground combatants, not vanilla Ender Dragon recolors.

---

# 18. Dungeons, ruins and structures

Use external high-quality dungeon/structure builds where usage terms permit.
Do not satisfy a dungeon count with generic self-made rectangular rooms.

Integration may replace/adapt:

- enemy roster;
- encounter logic;
- boss;
- loot/reward logic;
- progression hooks;

while preserving strong external architecture.

Potential dungeon density:

- frequent small ruins/caves/mini-encounters;
- less frequent medium dungeons;
- rare major dungeons with unique bosses and reward families.

The exact count follows the selected map and external structure pool.

---

# 19. Buildings and world services

External buildings are not decorative filler; assign gameplay roles to strong external structures where appropriate.

Candidate world-service roles include:

- forge/smith;
- alchemist;
- cook/tavern;
- class trainer or advancement hall;
- class-change shrine/sanctum;
- merchant market;
- storage/bank;
- quest/adventurer hall;
- fast-travel hub;
- mount stable if mounts are adopted;
- inn/rest location;
- dungeon entrance/guardian structure;
- special crafting/research facility when justified.

Prefer adapting available high-quality structures to roles over inventing low-quality buildings from scratch.

---

# 20. Camps, campfires, tents and player shelter

Open-world travel should create a sense of journey and temporary shelter.

Accepted direction:

- deployable or constructible camp;
- visually strong campfire;
- tent / bedroll / shelter;
- possible project-specific house/base placement or construction beyond vanilla recipes.

Possible camp functions:

- rest/recovery;
- cooking;
- time passing;
- temporary respawn/travel support;
- selected equipment/skill management if balance allows;
- comfort/buff effects only when they add decisions rather than chores.

Do not turn camping into repetitive survival busywork.
Camp, tent, campfire and house-kit visuals follow the external-first rule from the first implementation.

---

# 21. Minimap, world map and discovery

A minimap is planned.
Prefer adapting a proven reusable/open implementation over building a weak minimap from zero.

Potential markers:

- player;
- party members;
- discovered shrine/camp;
- quest destination;
- dungeon entrance;
- selected bosses and major POIs.

Do not reveal every enemy by default. Discovery and danger remain meaningful.
The minimap/world-map visual design must be externally selected before implementation.

Fog-of-war, discovery rules, marker density and fast-travel presentation remain design decisions.

---

# 22. Multiplayer / Essential direction

Essential-friendly multiplayer is a high-priority usability goal.
Loader/dependency choices should preserve this when technically practical.

Essential or another connection layer does not own gameplay authority.
Server authority remains final for:

- damage;
- items;
- currency;
- EXP/Lv;
- skill success/cost;
- class and class-history progression;
- quests;
- world/structure state;
- saves.

Do not claim multiplayer quality until actually tested.

---

# 23. Data-driven requirements

Repeatedly tuned values should be data-driven where practical:

- player/enemy stats;
- level/EXP curves;
- skill parameters;
- Mana/Stamina costs;
- equipment stats/scaling;
- class advancement data;
- class-switch costs;
- loot;
- encounter tables;
- region parameters;
- dungeon rewards;
- quests/dialogue;
- death penalties;
- world-service configuration.

Code owns rules; data owns content/tuning where practical.

---

# 24. Locked decisions as of 2026-09-14

1. Large open-world action RPG with very low vanilla-system dependence.
2. Private-play target; not intended for public distribution.
3. Public GitHub must still exclude non-redistributable assets.
4. `EXP` and `Lv` notation is fixed for player and enemies.
5. Vanilla XP/level progression and ordinary XP drops leave the core game.
6. Dedicated RPG inventory/equipment presentation replaces vanilla as the main character UI.
7. No temporary player-facing visual design, including test implementations.
8. External final-quality design/assets/references are used from the first visible implementation.
9. Workstation screens do not reuse generic project UI by default.
10. Dodge/roll has a dedicated rebindable key and consumes Stamina.
11. Sprint consumes a small/comfortable amount of Stamina.
12. Basic attacks cost no Stamina.
13. Most active skills use Mana; a small concept-driven minority may use Stamina.
14. Guard and parry are core combat mechanics.
15. No universal heavy attack.
16. 4 normal active skills + 1 ultimate.
17. Ultimate uses hybrid charge + cooldown/lockout logic and is deliberately high-impact.
18. Six primary stats: VIT, END, STR, DEX, INT, WIL.
19. Attack Speed remains an independent build axis rather than a direct DEX consequence.
20. Default project keybinds must not conflict with important Minecraft/Essential controls; low-importance vanilla bindings may be repurposed when justified.
21. Classes use relatively few root classes with deep repeated advancement.
22. No advancement stage is named or treated as permanently "final".
23. Class progress/history is saved separately per class line.
24. Changing to a new/untrained class starts that class from its own beginning, while returning to an old class restores its saved progress.
25. Class switching costs currency/resource and occurs through an in-world service.
26. Weapons remain broadly class-independent; effectiveness comes from scaling, cadence, build and class synergy.
27. Weapon families should be diverse but selected around strong external designs/assets and real gameplay identity.
28. Death lets the player choose currency loss or EXP loss.
29. EXP death loss may never reduce an already-earned Lv.
30. Region target is about 12 major regions, adjusted to the chosen external map.
31. Custom monsters dominate the enemy ecosystem.
32. Desert worm-type giant monster and dragons are accepted monster directions.
33. External dungeons/structures are preferred over generic self-made filler.
34. Strong external buildings should be reused as functional world locations when practical.
35. Campfire/tent/camp/base-building direction is accepted.
36. Minimap is planned, preferably by adapting a proven implementation.
37. Dead/superseded/duplicate code is removed after safe replacement rather than accumulated indefinitely.
38. Essential-friendly multiplayer remains a major technical goal.

---

# 25. Large next-decision batch

The next design pass should let the user decide many connected items together rather than answering one tiny question at a time.
Do not invent answers for these merely to make the document look complete.

Decisions still needed:

1. root-class count and class themes;
2. number of advancement stages targeted for the first production scope;
3. hidden/special advancement rules;
4. class-switch cost curve and where switching is allowed;
5. exact default keybind layout;
6. exact weapon-family roster;
7. exact equipment-slot layout;
8. item rarity/quality model;
9. fixed items vs randomized affixes vs hybrid loot;
10. skill acquisition: class level, quests, tomes, bosses, trainers, or hybrid;
11. passive-skill structure and whether passives consume a loadout budget;
12. stat-point acquisition and respec rules;
13. elemental/status system;
14. potion/consumable limits and healing model;
15. Stamina/Mana recovery timings;
16. guard-break/poise/stagger model;
17. map fog/discovery and marker rules;
18. fast-travel rules;
19. mounts or alternative traversal systems;
20. quest structure and how much main-story gating exists;
21. dynamic world events / rare encounters;
22. dungeon reset/respawn behavior;
23. world-boss respawn behavior;
24. loot ownership and party distribution in multiplayer;
25. downed/revive behavior in multiplayer;
26. crafting depth and whether gathering/crafting gets separate proficiency levels;
27. camp functions and limitations;
28. base/house construction scope;
29. settlement/shop/service-building roles;
30. faction/reputation system or deliberate omission;
31. enemy level placement and limited scaling rules;
32. first external open-world map selection;
33. first external UI design family selection;
34. first external weapon/model asset families;
35. first external structure/dungeon asset families;
36. first region vertical slice after map selection.

---

# 26. Research-derived design cautions

External open-world RPG precedents support several useful cautions that should guide later decisions without automatically becoming canon:

- Separate optional crafting/gathering progression can create depth, but it should not become mandatory grind for combat progression.
- Deep class/ability progression works best when it creates mechanical choices, not only stat increments.
- Horizontal progression and build variety can reduce the need for endless vertical stat inflation.
- Towns and world services feel better when they exist as actual places rather than only menu buttons.
- Dungeons, raids, field encounters and exploration rewards should have distinct reward identities.
- Mounts/traversal can matter greatly in a large voxel open world, but they should be added only if the selected map's scale justifies them.

These are research notes, not locked systems until explicitly selected.
