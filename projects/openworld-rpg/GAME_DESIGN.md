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

# 2. Production rules

## External-first visuals

No player-facing feature starts with disposable AI-made or generic placeholder art, even in test builds. From its first visible version, every important visual feature uses a deliberately selected external high-quality design, reference or usable asset.

This includes inventory/equipment, skill HUD, stats/class screens, forge/alchemy/cooking, minimap/world map, monsters/bosses, weapons/armor/accessories, shrines, camps/tents/campfires, inns, towns/service buildings, starting settlement, dungeons/ruins, mounts, resource nodes, VFX and sound direction.

If a design can be used directly and already looks better than a custom redesign, preserve it as intact as practical. Only alter what is required for information, controls, GUI scale and consistency. Do not create temporary web-game-style panels, disposable vanilla-entity placeholders or throwaway player-facing screens.

## Private-play target / public-repo boundary

The playable project is for private use, not public distribution. The GitHub repository is public, so non-redistributable or local/private-use asset bytes stay outside GitHub. The repository records provenance and local integration instructions instead. Redistributable CC0/MIT/etc. assets may be committed under their terms.

## Code hygiene

After a replacement is appropriately verified, remove superseded dead, duplicate, prototype and unused compatibility code immediately unless still required for save migration, active compatibility or a live feature. Git history is the archive; the source tree is not.

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
- vanilla cave-strip-mining as the intended core resource-gathering loop;
- Nether/End as mandatory progression gates.

Vanilla blocks/building/environmental interactions may remain where they improve the open-world sandbox without competing with project systems.

---

# 4. Naming

- Experience: `EXP`
- Level: `Lv`
- Player/enemy display uses the same format: `Lv <number>`
- Vanilla green XP/level presentation is not used for RPG progression.

---

# 5. Core resources and recovery

## HP

Primary survival resource. Natural recovery exists but is intentionally slow. Recovery can also come from purchased potions, food, skills/effects and rest.

## Mana

Primary active-skill resource. Most active skills consume Mana. A small concept-driven minority of martial/physical skills may use Stamina instead. Mana may recover slowly over time and more effectively through rest, items, class effects or equipment.

## Stamina

Used primarily by non-skill physical actions: dodge/roll, sprinting, guard/block impact, parry attempts and other non-skill mobility/defense. Normal basic attacks cost no Stamina. Sprint drain must be light enough that normal exploration is comfortable. Stamina recovery is substantially more responsive than HP recovery.

## Rest / food / potions

- Potions are obtained mainly through purchase and appropriate RPG economy sources rather than vanilla crafting-grid dependence.
- Food is useful recovery/buff content rather than meaningless hunger busywork.
- Passive regeneration exists but is deliberately slow enough that rest/items matter.
- Camps, inns, shrines or other valid rest points restore resources efficiently.
- Inns are valid world-service buildings if a strong external building/design source is selected.

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

Derived stats may include Physical Power, Magic Power, Defense, Magic Resistance, Critical Chance/Damage, Attack Speed, Movement Speed, Stamina/Mana Recovery, Guard Strength/Stability, healing and elemental/status modifiers. Exact formulas are data-driven balance work.

---

# 7. Combat

Combat mixes fast action-RPG flow, skill-driven builds and readable defensive play.

- Left click: weapon basic attack/combo. No Stamina cost.
- No universal heavy attack. Right click remains for shield/guard, weapon-specific alternate use, consumables and contextual item use.
- Dedicated rebindable dodge/roll key. Directional movement, Stamina cost, movement + evasion utility, visible timing aligned with actual evasion/i-frame timing.
- Guard and parry are core mechanics. Blocking costs Stamina through impact/defensive cost. Parry has readable timing and strong success feedback.
- Proven external combat implementations should be reused/adapted where technically and legally appropriate.

## Stagger / poise

Use a layered stagger system:

- normal enemies use simpler stagger resistance/reaction rules;
- elites and bosses have a meaningful poise/stagger gauge or equivalent;
- heavy weapons, impact attacks, selected skills and successful parries can reduce poise strongly;
- breaking poise creates a readable punish/damage window;
- animation and actual vulnerability state must match.

## Skill loadout

- 4 normal active skills
- 1 ultimate
- dodge/basic attack do not consume skill slots
- most active skills consume Mana; a small concept-driven minority may use Stamina

## Ultimate

Ultimate activation is hybrid:

- combat contribution builds charge/gauge;
- charge comes from role-appropriate useful actions, not damage only;
- attack, guard/parry, healing/support, control or other class-relevant contribution may charge it;
- after use there is an anti-spam cooldown/lockout;
- only one ultimate can be equipped, so it should be distinctly stronger, more spectacular and more build-defining than normal skills.

Ultimate HUD uses an externally selected final-quality design from the first playable version.

---

# 8. Input policy

All project actions are rebindable. Default bindings must not conflict with important Minecraft or Essential controls. Low-value vanilla bindings may be repurposed if their original action is irrelevant and the user can rebind it.

Before locking defaults, audit current Minecraft defaults, Essential defaults, required companion-mod defaults and this project's full action list.

---

# 9. Class system

## 9.1 Five root classes

Use five clear fantasy RPG root roles:

1. **전사** — direct melee combat, pressure and weapon mastery.
2. **사냥꾼** — ranged/precision/mobility-oriented combat, field utility and potential early-firearm specialization.
3. **성직자** — healing, buffs, protection and holy/support-oriented combat.
4. **마도사** — offensive magic, elemental/arcane damage and control.
5. **수호자** — defense, aggro/control, guarding allies and counter-oriented play.

Weapons are broadly class-independent. Effectiveness comes from stat scaling, attack cadence, class passives/skills and equipment synergy rather than hard weapon locks.

## 9.2 Advancement depth / branching

Canonical initial production target is about five advancement stages per root-class line, while architecture remains open for more later. Never label a stage as `final`.

Use a depth-first branch structure:

- root class;
- an early/mid major specialization branch materially changes playstyle;
- the chosen route continues through multiple deeper advancement stages;
- special/hidden advancements may require bosses, quests, stats, items or discoveries;
- advancement adds mechanics/passives/skill behavior/build identity, not only numbers.

Example: Warrior may branch toward damage pressure or defense/tanking. Similar meaningful playstyle changes should exist across all root classes.

## 9.3 Persistent class history and switching

Progress is stored separately for every class line.

- Switching to an untrained class starts that class from its beginning.
- Switching back restores its saved advancement, progress and unlocked content.
- Class switching costs currency/resource.
- Cost scales with player Lv but has a cap.
- No class-switch cooldown.
- Switching does not erase learned history.

## 9.4 Skills and passives

Skill acquisition is mixed:

- core class skills come from class progress/advancement;
- world exploration, bosses, quests, NPCs and rare finds may unlock additional skills or meaningful variants.

Passives use a tree/investment structure, but all passives unlocked on the currently active class apply. There is no passive-slot limit. Class-specific passives never remain active after switching away from that class. Each class retains its own saved passive tree and restores it only while active.

---

# 10. Shrines / world services

Class change, respec and advancement happen through believable in-world facilities such as shrines, sanctums, guild halls or trainers rather than a settings button.

Possible functions include class services, respawn/checkpoint, fast-travel access, build management and selected rituals. Do not force every function into one universal menu building if distributing roles creates a better world.

All structures use selected external high-quality builds/assets from the first visible implementation.

---

# 11. Death and respawn

At respawn, choose one penalty:

1. pay part of core currency; or
2. lose part of current EXP progress.

EXP loss can never reduce an already-earned Lv. The EXP floor is the start of the current Lv.

Exact percentages/caps, insufficient-currency behavior, boss-arena handling and penalty-reduction items remain balance decisions.

## Multiplayer down / revive

- In multiplayer, lethal damage can enter a temporary downed state instead of immediately forcing shrine respawn.
- A teammate can revive the downed player within the allowed rescue window.
- If rescue fails, normal shrine/checkpoint respawn and the chosen death penalty apply.
- Single-player does not need a fake waiting phase; lethal defeat may proceed directly to normal death/respawn flow.
- Revive behavior must remain server-authoritative.

---

# 12. Inventory and equipment slots

The vanilla inventory screen is replaced as the main RPG character interface by an externally selected final-quality RPG inventory/equipment design.

Canonical equipment layout target:

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

Two-handed weapons may disable or repurpose the off-hand slot. Exact visual placement follows the selected external GUI rather than an internally improvised layout. Accessory/Charm/Relic slots should enable build effects, not exist only for minor percentage increases.

Equipment has **no ordinary durability/repair chore**. Do not use vanilla-style durability as routine maintenance unless a future special mechanic has a clear gameplay purpose.

---

# 13. Weapons and item generation

## Weapon families

Prioritize coherent external model/design packs and good animation support. Candidate families:

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
- **early / black-powder firearm family** such as hand cannon, matchlock/flintlock pistol or musket when the Hunter line and external design quality justify it.

Modern/automatic firearms are not part of the current fantasy baseline. More advanced firearms may be reconsidered only for a future cyberpunk/advanced-tech direction or dedicated later class/region.

## Item generation / drops

Use a hybrid item model and probabilistic loot tables:

- regular enemy, elite, dungeon and boss drop tables use weighted/probability-based drops rather than giving every item every clear;
- common equipment may roll controlled affixes;
- named/boss/signature gear may have fixed identity, visuals and unique mechanics but can still be probabilistic drops;
- progression-critical quest/key items should not create softlocks solely through bad RNG;
- first-clear progression rewards may be deterministic even when farmable equipment remains probabilistic;
- stronger grade does not always mean strict numeric replacement.

## Multiplayer loot ownership

Use **personal loot for all dropped rewards in multiplayer**. A player's combat/loot rewards are not a shared floor-race where another player can take them first. Shared-world resource-node behavior is a separate gathering decision and is not automatically inferred from personal combat loot.

## Item grade structure

Use about five grades, but avoid the tired `Common / Rare / Epic / Legendary` naming set. Exact player-facing grade names are finalized with world lore/design language. Until then implementation uses neutral internal tier identifiers.

---

# 14. Damage types and status effects

Use a medium-complexity system. Physical identity may distinguish slash / pierce / impact where useful. Core magical/elemental families may include fire, frost, lightning, poison/corrosion and arcane-like effects. Status effects must be mechanically distinct rather than differently colored copies of the same DOT.

---

# 15. Workstations / professions / production

Major RPG production does not rely on the vanilla crafting-grid experience. Forge/smithing, alchemy, cooking and justified enhancement/customization systems may exist.

Use **light profession mastery** rather than giant mandatory profession grinds. Smithing, alchemy and cooking may improve through use and unlock useful recipes/quality/options, but progression must not require repetitive mass-crafting for dozens of hours.

Each important workstation gets a purpose-built screen selected from an external proven game/mod design first. If direct use is permitted and visually strong, preserve it rather than redesigning it into a weaker UI. Physical workstation/building appearance follows the same rule.

Avoid extra production steps that only add clicks.

---

# 16. Open world / settlements

Use a high-quality external open-world RPG map/terrain solution rather than hand-authoring the entire continent from zero.

Target about **12 major regions**, adjustable to roughly 10–14 if the selected external map's geography supports a different count. Each region contains subregions/landmarks/POIs and defines environmental identity, danger/Lv profile, enemies, elites, signature threats, resources, gear identity, settlements, dungeons, camps/checkpoints, quests/events and traversal gimmicks.

## Enemy Lv model

Use **regional Lv bands with only narrow contextual adjustment**. Do not scale every enemy to the player. A low-Lv region remains low-Lv later; a dangerous region remains genuinely dangerous when entered early.

## Starting settlement

The game should have a memorable starting village/settlement that acts as the player's first safe social/service hub. Its final name and world lore are decided later; do not use a generic vanilla village as the finished result.

The starting settlement may contain selected services such as:

- inn / tavern;
- forge / smith;
- merchant/market;
- alchemy/healer service;
- adventurer/quest guild;
- class/trainer/guild access where appropriate;
- storage/bank;
- stable/mount access;
- nearby shrine/checkpoint;
- quest/NPC hooks.

The village layout and buildings are sourced from a coherent external high-quality village/build family or map/schematic. Avoid stitching together unrelated building styles when a coherent pack/source is available.

## Town / service-building baseline

Default service palette, adjusted per settlement rather than copied mechanically everywhere:

- inn/tavern — rest, food, local information and selected checkpoint/time services;
- forge/smithy — weapon/armor production and related upgrade services;
- alchemy/healer — potions, treatment and alchemy;
- market/merchant district — general buying/selling and regional goods;
- adventurer/quest guild — contracts, regional jobs and world hooks;
- shrine/sanctum — checkpoint, selected travel/class/ritual services;
- stable — mount purchase/management/travel services;
- bank/storage — player storage and economy support;
- class hall/trainer where appropriate — advancement/training/class services;
- region-specific special facilities when they create real gameplay.

All important buildings are external-first designs/assets.

## Factions / reputation

Use reputation only for selected meaningful factions/regions. Do not create a dozen reputation bars by default. Reputation should unlock/change relationships, services, quests, prices, access or rewards where it actually matters.

## Day / night / weather

Use moderate gameplay impact:

- selected enemies, rare hunts, resources, events and boss behaviors may depend on time/weather;
- weather can alter region atmosphere and selected encounters;
- do not make ordinary play depend on tedious waiting for the correct clock/weather state.

---

# 17. Map discovery / navigation / fast travel

Use a middle-ground discovery model:

- general terrain/world shape can be visible enough for navigation;
- detailed POIs, dungeons, shrines, special bosses and discoveries must be found through exploration;
- minimap does not reveal every enemy or undiscovered reward.

Fast travel is available between discovered **shrines and major settlements/hubs**. Ordinary camps and every minor POI are not universal teleport nodes. Combat prevents fast travel.

Minimap/world-map visuals and implementation are external-first.

---

# 18. Mounts and traversal

Use ground mounts as a real progression/travel system.

Rules:

- different mounts can have meaningfully different travel speed, acceleration/handling, toughness and other role-relevant properties;
- some mounts may support mounted combat or combat utility;
- mounts should use external high-quality models/animations and proven mount implementations/code where suitable;
- do not make every mount a cosmetic reskin with identical stats;
- later progression should unlock substantially faster traversal options;
- late-game traversal may include flying mounts or another high-speed traversal system if it preserves world design and progression;
- flight should not be available so early that terrain, danger and dungeon approaches become irrelevant.

---

# 19. Enemies, bosses, world events and dungeons

Custom/project enemies dominate the combat roster. Important enemies require readable states, attack telegraphs, recovery, custom visuals/animation/VFX/sound appropriate to importance.

Accepted iconic directions include desert earth/sand-worm-style giants, dragons and region-specific large creatures/field bosses.

## Dynamic world events

Use region-specific dynamic events at a controlled frequency. Examples may include caravan attacks, rare dragon appearances, night creatures, magical weather/incursions or roaming major monsters. Events should feel discovered in the world, not like constant MMORPG chores.

## Dungeon replay

Dungeons are replayable. First clear provides unique/progression-significant rewards where appropriate; later clears may provide probabilistic gear, materials and rare drops. External dungeon architecture is retained where strong while encounters, enemies, bosses and rewards are project-specific.

## Field/world bosses

Field/world bosses can reappear under suitable timers/conditions. First defeat can have unique progression rewards, while repeat defeats focus on probabilistic rare drops/materials. Avoid rapid repetitive respawn loops.

---

# 20. Quests

Use a mixed structure:

- main narrative for world/system introduction and major progression;
- regional quest chains that give each area identity;
- free exploration and discovery that do not require constant quest-marker following.

The main story should guide without turning the open world into a linear corridor.

---

# 21. Gathering / resources

The intended gathering loop is **open-world RPG gathering**, not vanilla cave mining or strip-mining.

## Resource-node direction

- ore veins/mineral deposits, herbs, timber/wood resources, food ingredients and rare materials should appear as recognizable world gathering nodes or resource points;
- nodes are placed/generated according to region/terrain/ecology rather than hidden randomly behind thousands of ordinary stone blocks;
- gathering should involve discovery, route choice and region knowledge;
- common nodes may regenerate after a suitable time/condition so the world does not become permanently exhausted;
- rare resources may use longer respawn, special conditions, events, bosses or dangerous locations;
- node visuals/models/interactions are external-first where good assets/designs exist;
- node system design should study proven external implementations rather than reinventing resource regeneration poorly;
- vanilla block breaking can remain possible as a sandbox action, but it is not the intended progression source for core RPG ores/materials.

Exact shared-vs-personal node ownership in multiplayer, gathering tool requirements, gathering mastery and node respawn rules are still open decisions.

---

# 22. Camps / housing / rest

Travel should have a sense of journey and temporary shelter.

## Camps

Camps are **quick-build temporary field infrastructure**:

- player spends defined materials/resources to deploy/build a camp rapidly rather than manually constructing it block by block;
- camp visuals use an external final-quality tent/campfire/shelter design from the first implementation;
- camp can provide strong rest/resource recovery and selected travel/utility functions;
- camp construction should support exploration rather than become a construction grind;
- ordinary camps are not universal fast-travel nodes unless later design explicitly adds a limited exception.

## Housing

Permanent player housing is separate from camps.

- towns/settlements can contain empty/purchasable houses;
- player can buy a residence rather than automatically upgrading a field camp into a full base;
- house shells/interiors should use coherent external building designs/assets;
- exact furnishing, storage, cosmetic and gameplay functions remain to be designed.

---

# 23. Multiplayer / Essential

Essential-friendly multiplayer is a major usability goal. Convenience networking does not own game authority. Damage, item ownership, currency, EXP/Lv, skill cost/success, class/progression, quests, world state and saves remain server-authoritative.

Combat/loot rewards are personal per player. Downed/revive behavior is defined in the death section.

Do not claim multiplayer quality until actually tested.

---

# 24. Economy

A core currency is required for at least class switching, death-penalty choice, merchants, housing and selected services.

The exact **player-facing currency name and unit are not locked yet**. Choose them together with the world's lore rather than forcing a generic name too early.

Preferred design direction to decide next:

- one primary numeric currency for ordinary economy;
- avoid unnecessary copper/silver/gold denomination conversion unless it adds real value;
- add special currencies/tokens only when a specific activity needs a distinct reward loop;
- do not multiply currencies merely to make the game look larger.

---

# 25. Data-driven / maintainability

Repeatedly tuned values should be data-driven where practical: player/enemy stats, Lv curves, skill parameters, Mana/Stamina costs, equipment scaling, affixes, drop probabilities, loot, encounters, resource nodes, region parameters, advancement values, dungeon rewards, quests and dialogue.

Code owns rules; data owns content/tuning where feasible.

---

# 26. Current locked decisions

Major locked decisions as of 2026-09-14 include:

- private-use large open-world action RPG with very low vanilla progression dependence;
- external-first visuals/assets from the first visible/test implementation;
- `EXP` / `Lv` notation and removal of vanilla XP progression/drop loop;
- HP + Mana + Stamina; Stamina is primarily non-skill action resource; basic attack costs no Stamina;
- dodge, guard, parry and layered stagger/poise combat;
- 4 active skills + 1 high-impact hybrid-charge ultimate;
- VIT / END / STR / DEX / INT / WIL primary stats;
- five root classes: 전사 / 사냥꾼 / 성직자 / 마도사 / 수호자;
- deep ~5-stage initial advancement target, one major playstyle-changing branch, no `final` terminology;
- persistent per-class progression with paid Lv-scaled/capped switching and no switch cooldown;
- all unlocked passives of the active class apply; inactive-class passives never leak across;
- broad weapon freedom, including future Hunter-compatible black-powder firearms but no modern firearms baseline;
- 12-slot RPG equipment target and no routine durability chore;
- hybrid random-affix + named/signature equipment; roughly five non-cliché player-facing grades later;
- medium damage/status complexity;
- light smithing/alchemy/cooking mastery;
- regional Lv bands, roughly 12 major regions, discovered POIs, shrine/major-hub fast travel;
- ground mounts with varied speed/handling/combat roles and later high-speed/flying traversal;
- mixed main/regional/free-exploration quests, dynamic region events, replayable dungeons, respawning field/world bosses;
- probabilistic drops with deterministic protection for progression-critical items;
- multiplayer down/revive and fully personal combat loot;
- quick-build material-cost camps; permanent houses are purchased separately in settlements;
- selected faction/reputation systems only where meaningful;
- moderate day/night/weather gameplay effects;
- RPG field resource nodes instead of cave/strip-mining as the core gathering loop;
- dead/superseded/duplicate code removed after safe replacement;
- Essential-friendly server-authoritative multiplayer target.

---

# 27. Next decision queue

Keep batches manageable. Next choices should cover:

1. resource-node ownership in multiplayer: shared depletion vs personal gathering state;
2. gathering categories/mastery: mining, herbalism, forestry, fishing/foraging and how light the progression stays;
3. gathering tools/interactions and whether tools are required or simply improve yield/speed;
4. resource-node regeneration timing and rare-node rules;
5. core currency structure/name timing and whether one primary currency is accepted;
6. merchant economy: buy/sell rules, rotating stock and rare goods;
7. inventory capacity: slot count, material pouch and whether weight is completely absent;
8. purchased-house functions and furnishing/storage depth;
9. starting-settlement role/unlocks and whether services expand through story/quests;
10. exact class advancement branches after external character/weapon design research;
11. world-map/terrain candidate selection and region derivation;
12. final keybind audit only after the full action list exists.

Do not invent answers merely to make the document look complete.
