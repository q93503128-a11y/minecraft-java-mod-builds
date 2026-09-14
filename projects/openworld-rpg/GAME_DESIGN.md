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

This includes inventory/equipment, skill HUD, stats/class screens, forge/alchemy/cooking, minimap/world map, monsters/bosses, weapons/armor/accessories, shrines, camps/tents/campfires, towns/service buildings, dungeons/ruins, VFX and sound direction.

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

Used primarily by non-skill physical actions:

- dodge/roll;
- sprinting;
- guard/block impact;
- parry attempts or related defensive actions;
- other non-skill mobility/defense.

Normal basic attacks cost no Stamina. Sprint drain must be light enough that normal exploration is comfortable. Stamina recovery is substantially more responsive than HP recovery because it is a combat-action resource.

## Rest / food / potions

- Potions are obtained mainly through purchase and appropriate RPG economy sources rather than vanilla crafting-grid dependence.
- Food exists as useful recovery/buff content rather than meaningless hunger busywork.
- Passive regeneration exists but is slow enough that rest/items matter.
- Camps, inns, shrines or other valid rest points restore resources efficiently; a camp should provide a strong journey/rest fantasy rather than maintenance labor.

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

## Skill loadout

- 4 normal active skills
- 1 ultimate
- dodge/basic attack do not consume skill slots
- most active skills consume Mana; a small concept-driven minority may use Stamina

## Ultimate

Ultimate activation is hybrid:

- combat contribution builds charge/gauge;
- after use there is an anti-spam cooldown/lockout;
- charge sources should respect role, so support/defense actions can contribute where relevant;
- only one ultimate can be equipped, so it should be distinctly stronger, more spectacular and more build-defining than normal skills.

Ultimate HUD uses an externally selected final-quality design from the first playable version.

---

# 8. Input policy

All project actions are rebindable. Default bindings must not conflict with important Minecraft or Essential controls. Low-value vanilla bindings may be repurposed if their original action is irrelevant and the user can rebind it.

Before locking defaults, audit current Minecraft defaults, Essential defaults, required companion-mod defaults and this project's full action list.

---

# 9. Class system

## 9.1 Five root classes

Use five clear, readable fantasy RPG root roles rather than vague internal labels:

1. **전사** — direct melee combat, pressure and weapon mastery.
2. **사냥꾼** — ranged/precision/mobility-oriented combat and field utility.
3. **성직자** — healing, buffs, protection and holy/support-oriented combat.
4. **마도사** — offensive magic, elemental/arcane damage and control.
5. **수호자** — defense, aggro/control, guarding allies and counter-oriented play.

Names may receive world-lore localization later, but their player-facing role must remain immediately understandable.

Weapons are broadly class-independent. Effectiveness comes from stat scaling, attack cadence, class passives/skills and equipment synergy rather than hard weapon locks.

## 9.2 Advancement depth

Canonical initial production target is about **five advancement stages** per root-class line, while architecture remains open for more later. Never label a stage as `final` because the game may expand.

Use a depth-first branch structure:

- root class;
- early/mid progression contains one major specialization branch that changes playstyle materially;
- chosen route then continues through multiple deeper advancement stages;
- special/hidden advancements may require bosses, quests, stats, items or discoveries;
- an advancement should add mechanics/passives/skill behavior/build identity, not only larger numbers.

Example principle: even a Warrior may branch toward aggressive damage pressure or defensive/tank-oriented play. Similar meaningful playstyle changes should exist across all root classes.

## 9.3 Persistent class history and switching

Progress is stored separately for every class line.

- Switching to an untrained class starts that class from its beginning.
- Switching back restores the previous class's saved advancement, class progress and unlocked content.
- Class switching costs currency/resource.
- Cost scales with player Lv but has a cap so experimentation never becomes prohibitively expensive.
- No class-switch cooldown.
- Switching does not erase learned history.

## 9.4 Skills and passives

Skill acquisition is mixed:

- core class skills come from class progress/advancement;
- world exploration, bosses, quests, NPCs and rare finds may unlock additional skills or meaningful variants.

Passives use a tree/investment structure, but **all passives actually unlocked on the currently active class apply**. There is no passive-slot loadout limit.

Critical rule: class-specific passives never remain active after switching away from that class. Each class retains its own saved passive tree/investments and restores them only when that class becomes active again. Cross-class permanent passive stacking is forbidden unless a future system explicitly introduces a separate account-wide progression layer.

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

Exact percentages/caps, insufficient-currency behavior, boss-arena handling, multiplayer revive/respawn and penalty-reduction items remain balance decisions.

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

Two-handed weapons may disable or repurpose the off-hand slot. Exact visual placement follows the selected external GUI rather than an internally improvised layout.

Accessory/Charm/Relic slots should enable build effects, not exist only for minor percentage increases.

---

# 13. Weapons and item generation

## Weapon families

Prioritize coherent external model/design packs and good animation support. Initial target families:

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
- shield and specialized off-hands.

Do not add firearms at initial launch. Firearms may be reconsidered later for a cyberpunk/advanced-tech region or a future hunter/gunner-style class if the world direction supports them.

## Item generation

Use a hybrid model:

- common field/loot equipment may roll a controlled set of random affixes;
- named, boss, dungeon and signature gear may have fixed identity, visuals and unique mechanics;
- stronger rarity/tier does not always mean strict numerical replacement;
- build interactions and unique effects matter.

## Item grade structure

Use about five grades, but avoid the tired `Common / Rare / Epic / Legendary` naming set. Exact grade names should be finalized with the world's lore/design language rather than chosen in isolation. Until then, implementation uses neutral internal tier identifiers rather than player-facing cliché labels.

---

# 14. Damage types and status effects

Use a medium-complexity system rather than either no identity or excessive elemental bookkeeping.

Physical identity may distinguish slash / pierce / impact where useful. Core magical/elemental families may include fire, frost, lightning, poison/corrosion and arcane-like effects. Status effects should be mechanically distinct and should not become many differently colored copies of the same DOT.

Exact resistance formulas and final element list remain open until enemy/region design is further defined.

---

# 15. Workstations and production

Major RPG production does not rely on the vanilla crafting-grid experience. Forge/smithing, alchemy, cooking and justified enhancement/customization systems may exist.

Each important workstation gets a purpose-built screen selected from an external proven game/mod design first. If direct use is permitted and visually strong, preserve it rather than redesigning it into a weaker UI. Physical workstation/building appearance follows the same rule.

Avoid extra production steps that only add clicks.

---

# 16. Open world

Use a high-quality external open-world RPG map/terrain solution rather than hand-authoring the entire continent from zero.

Target about **12 major regions**, adjustable to roughly 10–14 if the selected external map's geography supports a different count. Each region contains subregions/landmarks/POIs and eventually defines environmental identity, danger/Lv profile, common enemies, elites, signature threats, resources, gear identity, settlements, dungeons, camps/checkpoints, quests/events and traversal gimmicks.

Avoid universal full level scaling. Dangerous regions should remain genuinely dangerous, while skilled players may attempt them early.

---

# 17. Enemies, bosses and dungeons

Custom/project enemies dominate the combat roster. Important enemies require readable states, attack telegraphs, recovery, custom visuals/animation/VFX/sound appropriate to importance.

Accepted iconic directions include desert earth/sand-worm-style giants, dragons and region-specific large creatures/field bosses.

Bosses require readable telegraphs, visible range matching hit detection, punish windows, phase changes that alter play, and meaningful rewards.

Dungeons use high-quality external architecture/structure assets where possible. Replace/adapt enemy roster, encounters, bosses, loot and progression while retaining good architecture. Do not manufacture generic rectangular filler rooms merely to increase dungeon count.

---

# 18. Camps, housing and navigation

Travel should have a sense of journey and temporary shelter.

Planned systems include deployable/constructible camps, visually strong campfires, tents/bedrolls and possible progression toward small homes/bases using external high-quality structures rather than vanilla-recipe-only progression.

A minimap is planned, preferably by adapting a proven implementation. It may show player, party members, discovered camps/shrines, selected quest goals, dungeon entrances and major POIs, but should not automatically reveal every enemy or discovery.

---

# 19. Multiplayer / Essential

Essential-friendly multiplayer is a major usability goal. Convenience networking does not own game authority. Damage, item ownership, currency, EXP/Lv, skill cost/success, class/progression, quests, world state and saves remain server-authoritative.

Do not claim multiplayer quality until actually tested.

---

# 20. Data-driven / maintainability

Repeatedly tuned values should be data-driven where practical: player/enemy stats, Lv curves, skill parameters, Mana/Stamina costs, equipment scaling, affixes, loot, encounters, region parameters, advancement values, dungeon rewards, quests and dialogue.

Code owns rules; data owns content/tuning where feasible.

---

# 21. Current next-decision queue

Next design batch begins from the previously numbered decision 13 onward and should cover, in manageable groups rather than dozens at once:

- stagger/poise system;
- Lv scaling model;
- map discovery/fog/POI reveal;
- fast travel;
- mounts;
- quest structure;
- dynamic world events;
- dungeon replay;
- world-boss respawn/replay;
- multiplayer down/revive and loot rules;
- crafting-profession depth;
- durability;
- camp/base growth;
- settlement/building roles;
- faction/reputation scope;
- day/night/weather gameplay impact;
- inventory weight/space rules;
- final keybind audit after the full action list exists.

Do not invent answers merely to make the document look complete.