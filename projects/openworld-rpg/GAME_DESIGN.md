# Open-World RPG — Master Game Design Canon

> Status: DESIGN CANON / continuously maintained
> Final game title: TBD
> Canon priority: current GitHub `main` > this file > subordinate project docs > older conversations

This file is the single source of truth for gameplay/design decisions. Do not create a competing master design document.
When a decision changes, update the old section instead of leaving contradictory versions scattered through the repository.

---

# 1. Vision

Build a large open-world action RPG that runs on Minecraft Java but has very low dependence on vanilla RPG/progression systems.
Minecraft supplies the block world, simulation/runtime and multiplayer foundation; the actual game identity comes from this project.

The target feeling is not "Minecraft with some RPG stats".
It should feel like a standalone open-world RPG whose world happens to be made from Minecraft blocks.

## Core loop

```text
Explore
→ discover region / POI / camp / dungeon / enemy encounter
→ fight / gather / complete objectives
→ gain EXP, currency, equipment, materials and skills
→ alter the character build
→ reach more dangerous areas
→ defeat elites / field bosses / dungeon bosses
→ unlock advancement, regions and new choices
→ explore again
```

The project should prefer depth and system interaction over adding disconnected menus or currencies.

---

# 2. Hard design rules

## 2.1 External-first visual rule

Player-facing design must not begin as temporary AI-made art.
This applies even to test builds.

From the first visible version, use a deliberately selected external high-quality reference or usable asset for:

- inventory;
- equipment screen;
- skill HUD;
- stats/class screens;
- forge / alchemy / cooking and other workstation screens;
- minimap/world-map presentation;
- monsters and bosses;
- weapons and armor;
- shrine;
- camps, tents and campfires;
- towns, dungeons and major structures;
- important VFX/sound direction.

If the external asset/license allows direct use, prefer keeping the proven final design instead of "improving" it into a worse custom design.
Only make the minimum changes required for this game's information, controls and consistency.

Do not use temporary black translucent panels, arbitrary colored borders, generic web-game cards, placeholder vanilla entities, or disposable player-facing art as an intermediate production stage.

## 2.2 External-source safety

The playable build is intended for private use, but the GitHub repository is public.
Therefore non-redistributable private-use assets must remain local and uncommitted. The repository records source/provenance and integration instructions instead.

## 2.3 Code hygiene

The current source tree should contain current implementation, not an archaeological layer of abandoned code.
Once a replacement is appropriately verified, remove superseded dead/duplicate/prototype code immediately.
Git history preserves the old implementation.

Exceptions: active migration, save compatibility, required compatibility shim, or a still-used feature.

---

# 3. Vanilla replacement policy

The project intentionally minimizes dependence on vanilla progression and combat content.

## Replace or remove from the core loop

- Vanilla experience points and vanilla levels
- Vanilla XP orb drops from normal gameplay
- Vanilla enchantment progression as a core system
- Vanilla weapons/armor as meaningful long-term RPG gear
- Vanilla monsters as the main enemy roster
- Vanilla crafting recipes as the main RPG production loop
- Vanilla inventory/equipment presentation as the main character screen
- Nether/End as mandatory progression gates

Vanilla blocks and environmental interactions may remain where they improve the open-world sandbox without competing with the RPG systems.

---

# 4. Naming and player-facing notation

These labels are fixed unless explicitly changed later.

- Experience: `EXP`
- Level: `Lv`
- Player level display: `Lv <number>`
- Enemy level display: `Lv <number>` using the same notation

Examples:

```text
Lv 18
Lv 42 Sand Wyrm
```

Do not use the vanilla green XP/level presentation for the RPG progression system.

---

# 5. Character resources

The character has three principal combat resources.

## HP

Health / survival resource.
Exact scaling and recovery rules are not locked yet.

## Mana

Primary skill resource.

Rules:

- most active skills consume Mana;
- Mana cost is not mandatory for every skill;
- a small number of conceptually physical/martial skills may consume Stamina instead;
- class/gear/passives may change cost, regeneration or resource behavior.

## Stamina

Primary non-skill action resource.

Stamina is used by physical actions such as:

- dodge/roll;
- sprinting;
- guard impact/blocking;
- parry attempts or related defensive actions;
- other non-skill mobility/defensive actions where appropriate.

Rules:

- normal basic attacks consume **no Stamina**;
- sprinting consumes Stamina slowly enough that ordinary open-world traversal does not feel irritating;
- dodge/defense should matter in combat without making exploration a stamina-management chore;
- maximum Stamina and recovery can be improved through stats, class effects, equipment and other progression.

Exact costs/regeneration timings remain a balance item.

---

# 6. Primary stat system

Use six primary stats. Keep generic derived combat stats separate so builds can specialize through equipment/class effects without one primary stat becoming mandatory for everything.

## VIT — Vitality

Primary role:

- maximum HP;
- survivability-oriented scaling where appropriate.

Do not automatically attach every defensive stat to VIT.

## END — Endurance

Primary role:

- maximum Stamina;
- Stamina recovery scaling;
- selected guard/stability interactions.

## STR — Strength

Primary role:

- physical scaling for heavy/power-oriented weapons and skills;
- STR requirements/scaling where a weapon or class concept needs it.

## DEX — Dexterity

Primary role:

- physical scaling for finesse/ranged/technical weapons and skills;
- selected precision/critical-oriented interactions.

Important: DEX does **not** automatically become the universal Attack Speed stat. Attack Speed must remain a meaningful independent build lever through equipment, class/passives and weapon properties.

## INT — Intelligence

Primary role:

- magical/offensive spell scaling;
- magic-focused weapon and skill requirements/scaling.

## WIL — Willpower

Primary role:

- maximum Mana;
- Mana recovery/sustain;
- selected magical/status resilience or resource-control interactions when appropriate.

## Derived stats

Examples include, but are not limited to:

- Physical Power
- Magic Power
- Defense
- Magic Defense / resistance if the final combat model needs it
- Critical Chance
- Critical Damage
- Attack Speed
- Movement Speed
- Stamina Recovery
- Mana Recovery
- Guard Strength / Stability
- elemental/status modifiers

Derived stat formulas are not final yet and should be data-driven where practical.

---

# 7. Combat system

The combat identity mixes fast action-RPG combat, skill-driven combat and readable defensive play.

## 7.1 Basic attack

- Left click: weapon basic attack / combo chain.
- No Stamina cost.
- Weapon families have different animation timing, reach, speed, hit cadence and scaling.
- Attack Speed is a meaningful build dimension.
- Some classes/builds may care about number of hits, proc frequency or combo cadence, making Attack Speed equipment valuable.

## 7.2 No generic heavy-attack input

There is no universal right-click heavy attack.
Right click remains available for the held item's natural use, such as:

- shield/guard;
- weapon-specific alternate behavior;
- consumable/use item;
- other contextual item interaction.

If a weapon concept needs a powerful special move, implement it as a weapon-specific mechanic or skill rather than a universal heavy-attack system.

## 7.3 Dodge / roll

Add a dedicated rebindable dodge key.

Goals:

- useful as both movement and evasion;
- directional according to movement input;
- consumes Stamina;
- includes a tuned invulnerability/evasion window if playtesting supports it;
- animation and actual invulnerability timing must match visibly.

External combat implementations should be studied/reused where terms permit instead of rebuilding a worse version from scratch.

## 7.4 Guard and parry

Guard and parry are core combat mechanics.

Goals:

- clear telegraph and timing;
- blocking costs Stamina through impact or related defensive cost;
- parry has a readable timing window and strong success feedback;
- failure should remain understandable rather than random;
- suitable weapons/shields/classes may have different guard/parry performance.

Use proven external implementation/code/reference wherever practical and legally usable.

## 7.5 Skills

Equipped active-skill layout is fixed to:

- 4 normal active skills
- 1 ultimate skill

Dodge does not occupy a skill slot.
Basic attack does not occupy a skill slot.

Most skills consume Mana. A small minority may consume Stamina when the class/skill concept strongly supports it.

## 7.6 Ultimate

Only one ultimate can be equipped at a time.
Therefore ultimates should be noticeably stronger, more spectacular and more build-defining than ordinary active skills.

The exact ultimate activation economy (cooldown, charge, Mana, special gauge, hybrid) is not yet locked.

The ultimate HUD must use an externally selected high-quality game/mod design rather than an improvised custom panel.

---

# 8. Class and advancement structure

Class structure uses selection + repeated advancement.

## Principles

- the player selects an initial class;
- there are relatively few root classes;
- each root class has multiple advancement stages;
- advancement should happen several times during a long playthrough, not only once;
- later advancements should create stronger identity and mechanics rather than only larger numbers;
- weapons are **not** hard-locked by class;
- any class may equip any weapon unless a specific exceptional rule is intentionally designed;
- practical effectiveness depends on the weapon's physical/magical scaling, attack cadence and the class/build's stats/passives/skills.

Example design consequence:

A rapid-hit class can choose a fast weapon and Attack Speed gear to maximize on-hit interactions, while another class may use a slower high-scaling weapon. Neither requires a global class-based weapon lock.

Exact root classes, advancement tiers and branch topology are the next major design task and are not yet canonized.

---

# 9. Class reset / shrine

Class reset/respec should happen through an in-world shrine or equivalent landmark rather than a generic settings button.

The shrine can eventually support selected functions such as:

- class reset / respec;
- class advancement;
- respawn;
- fast travel;
- build management;
- other progression rituals if they fit the world.

Not every function must be combined if doing so creates a menu hub instead of a believable world location.

Visual rule: use an external high-quality shrine/structure design or usable structure asset. Do not create a generic pillar-and-particle shrine as a placeholder.

---

# 10. Death and respawn

On death, the player respawns through the appropriate shrine/checkpoint system.

At respawn, the player chooses one of two penalties:

1. pay a portion of core currency; or
2. lose a portion of EXP.

Exact percentages/scaling are not locked yet.

Future balance decisions must define:

- whether EXP loss can reduce an already-earned Lv;
- minimum/maximum loss;
- insufficient-currency behavior;
- boss-arena handling;
- multiplayer death/respawn behavior.

The choice should create a real decision without making repeated deaths destroy a long play session.

---

# 11. Inventory and equipment

The vanilla inventory screen is not the final RPG inventory.
Use a dedicated RPG inventory/equipment presentation based on a selected external implementation/design.

Expected categories include weapon/armor/accessory/special equipment, but the exact slot set is not yet locked.

Requirements:

- equipment slots must have distinct gameplay purpose rather than existing only to increase slot count;
- accessory slots should enable build changes, not only small percentage bonuses;
- inventory/equipment UI is designed from an external final-quality reference from the beginning;
- interaction must remain practical under Minecraft GUI Scale and mouse/keyboard use.

---

# 12. Equipment and weapon philosophy

The game uses its own RPG equipment ecosystem.
Vanilla gear is not intended as long-term progression equipment.

Weapons differ by:

- physical/magical scaling;
- attack speed;
- reach;
- animation cadence;
- combo behavior;
- special effects;
- synergy with classes, stats and accessories.

A higher-tier item should not always be a pure numeric replacement for every lower-tier item.
Distinct mechanics and build interactions are valuable.

Attack Speed is explicitly allowed as a serious equipment/build axis because some classes/mechanics may reward hit count or proc frequency.

---

# 13. Workstations and crafting

Major RPG production is not built around the vanilla crafting-grid experience.

Potential stations include:

- forge / smithing;
- alchemy;
- cooking;
- equipment enhancement/customization where justified;
- other profession-specific stations only when they connect to the main loop.

Rules:

- each major workstation gets a purpose-built screen;
- do not reuse a generic project GUI simply because it already exists;
- select a proven external game/mod UI design for that station type;
- if direct use is permitted and the design works, retain it with minimal necessary adaptation;
- station block/model/structure also follows the external-first visual rule.

Avoid creating many workstations that only add extra clicks between materials and reward.

---

# 14. Open world

The world should be based on a high-quality external open-world RPG map/terrain solution rather than asking this project to hand-author an entire continent from scratch.

## Region-density decision

Target **roughly 10–14 major regions**, with exact count determined by the selected external map.
This is intentionally denser than a few giant biomes but not fragmented into dozens of shallow zones.

Each major region can contain several micro-regions/POIs.

Exact region names and concepts are not canon until the map is selected, because region design should follow the actual terrain rather than forcing terrain into a prewritten list.

## Region content package

Each major region should eventually define:

- environmental identity;
- recommended Lv band or danger profile;
- common enemy families;
- elite/rare enemies;
- at least one memorable major threat where appropriate;
- resources/materials;
- item/gear identity;
- POIs;
- dungeon opportunities;
- camps/shrines;
- quests/events;
- traversal or environmental gimmicks where valuable.

Avoid universal full level-scaling that makes every area feel identical. Dangerous regions should genuinely be dangerous, while skilled players may attempt them early.

---

# 15. Enemy ecosystem

The main enemy roster is project-owned/custom rather than vanilla monsters carrying larger HP numbers.

Enemies should have readable states, attack telegraphs, recovery and reactions.

Important enemies require custom model/animation/VFX/sound quality appropriate to their role.

## Iconic regional monsters

The project explicitly wants classic high-fantasy/open-world regional threats where they fit.
Examples already accepted in principle:

- giant earth/sand worm type monster in a desert region;
- dragons as major high-tier predators/bosses;
- region-specific giant creatures and field bosses.

These should be integrated into the ecology and regional progression rather than added as isolated spectacle mobs.

A desert worm should, for example, have meaningful underground movement/emergence/telegraph behavior rather than simply being a reskinned surface melee mob.

---

# 16. Bosses

Bosses include dungeon bosses, regional/field bosses and potentially world-class threats.

Boss rules:

- readable telegraphs;
- attacks with visible range matching actual hit detection;
- meaningful recovery/punish windows;
- phase changes that alter play, not just damage values;
- custom animation/model/VFX/sound appropriate to importance;
- rewards that open build/progression options.

Dragons should be treated as proper aerial/ground combatants, not vanilla Ender Dragon recolors.

---

# 17. Dungeons and structures

Dungeons should appear often enough to reward exploration but not so frequently that the world becomes a dungeon-icon checklist.

Use external high-quality dungeon structures/maps/structure packs where usage terms permit.
Do not hand-build generic rectangular rooms merely to satisfy a dungeon count.

Dungeon integration should replace or adapt:

- enemy roster;
- encounter logic;
- boss;
- loot/reward logic;
- progression hooks;

while retaining good external architecture when possible.

Potential tiers:

- small ruin/cave encounters;
- medium dungeons;
- rare major dungeons with unique bosses and reward families.

---

# 18. Camps, campfire, tents and building

Open-world travel should have a sense of journey and temporary shelter.

Planned direction:

- deployable or constructible camp;
- visually strong campfire;
- tent / bedroll / shelter;
- possible small house/base construction through project-specific systems rather than relying only on vanilla recipes.

The camp can support selected functions such as:

- rest/recovery;
- cooking;
- skill/equipment adjustment if balance permits;
- time passing;
- temporary respawn or travel support;
- buffs/comfort mechanics if they add gameplay instead of maintenance chores.

Do not turn camping into repetitive survival busywork.

Visual rule: campfire/tent/house kit designs come from selected external high-quality assets/references from the first playable version.

---

# 19. Minimap and navigation

A minimap is planned.
Prefer a reusable/open implementation that can be adapted rather than building a low-quality minimap from zero.

Expected map information may include:

- player;
- party members;
- discovered shrine/camp;
- quest destination;
- dungeon entrance;
- selected bosses/major POIs.

Do not automatically expose every enemy through radar; discovery and danger should remain meaningful.

Minimap visual design also follows the external-first rule.

---

# 20. Multiplayer / Essential direction

Essential-friendly multiplayer is a high-priority usability goal.
The loader/dependency decision should preserve this if technically practical.

Essential or another connection convenience layer does not own game authority.
Important game state remains server-authoritative, including:

- damage;
- item ownership;
- currency;
- EXP/Lv;
- skill success/cost;
- class/progression;
- quest state;
- world/structure state;
- saves.

Do not claim multiplayer quality until it is actually tested.

---

# 21. Data-driven requirements

Values expected to be tuned repeatedly should be data-driven where practical:

- player/enemy stats;
- level curves;
- skill parameters;
- Mana/Stamina cost;
- equipment stats/scaling;
- loot;
- enemy encounter tables;
- region parameters;
- class advancement values;
- dungeon rewards;
- quests/dialogue.

Code owns rules; data owns content/tuning where feasible.

---

# 22. Current locked decisions

As of 2026-09-14, the following are explicitly locked unless the user later changes them:

1. Large open-world RPG direction with very low vanilla-system dependence.
2. Private-play target; not intended for public release.
3. Public GitHub still must not contain non-redistributable assets.
4. EXP is written `EXP`; level is written `Lv` for both player and enemies.
5. Vanilla XP/level progression and normal XP drops are removed from the core game.
6. Player-facing inventory/equipment is a custom RPG presentation.
7. No temporary player-facing visual design, including test implementations.
8. External final-quality designs/assets/references are used from the first visible implementation.
9. Major workstation screens do not reuse generic project UI by default.
10. Dodge/roll gets its own key and consumes Stamina.
11. Sprint consumes a small/comfortable amount of Stamina.
12. Basic attacks consume no Stamina.
13. Most active skills consume Mana; a small concept-driven minority may use Stamina.
14. Guard and parry are core combat mechanics.
15. No universal heavy attack.
16. 4 normal active skill slots + 1 ultimate slot.
17. Ultimate is intentionally high-impact because only one can be equipped.
18. Six primary stats: VIT, END, STR, DEX, INT, WIL.
19. Attack Speed remains an independent build axis rather than being automatically dictated by DEX.
20. Classes use initial selection plus repeated multi-stage advancement.
21. Root class count stays relatively limited while advancement depth is large.
22. Weapons are broadly class-independent; effectiveness comes from scaling/cadence/build synergy.
23. Class reset/respec happens through an in-world shrine-like structure.
24. Death respawn offers a choice to pay some currency or lose some EXP.
25. Open-world region count targets roughly 10–14 major regions, finalized after an external map is selected.
26. Custom monsters dominate the main enemy ecosystem.
27. Desert worm-type giant monster and dragons are accepted monster directions.
28. External dungeons/structures are preferred over generic self-made filler.
29. Campfire/tent/camp/base-building direction is accepted.
30. Minimap is planned, preferably by adapting a proven external implementation.
31. Dead/superseded/duplicate code is removed after safe replacement rather than accumulated indefinitely.
32. Essential-friendly multiplayer is a major technical goal.

---

# 23. Next design decisions

Do not skip directly to mass content production. Next decisions should be made roughly in this order:

1. Root classes and advancement-tree topology
2. Weapon families and physical/magical scaling identities
3. Combat resource timing: Stamina/Mana recovery, guard/parry/dodge costs
4. Ultimate activation economy
5. Exact equipment slot layout and accessory philosophy
6. External inventory/skill-HUD/workstation UI design selection
7. External open-world map selection
8. Region list derived from the selected map
9. Shrine design/source and shrine functionality split
10. Death-penalty percentages and Lv-loss rule
11. First region vertical slice: enemies → elite → boss → dungeon → rewards → camp/shrine → UI loop

Do not invent answers for undecided items merely to make the document look complete.
