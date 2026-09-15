# Open-World RPG — Master Game Design Canon

> Status: DESIGN CANON / continuously maintained  
> Final game title: TBD  
> Canon priority: current GitHub `main` > this file > subordinate project docs > older conversations

This file is the single source of truth for gameplay/design decisions. Do not create a competing master design document. When a decision changes, edit the existing section instead of leaving contradictory versions behind.

Subordinate references currently indexed by this canon:

- `REGIONS.md` — regional expansion/content details for Azari.
- `UI_DIRECTION.md` — selected external UI family, screen architecture and visual acceptance rules.

If a subordinate reference conflicts with this file, this file wins.

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
- **all vanilla mobs as world population, combat enemies, livestock, wildlife and NPC population**;
- vanilla crafting-grid progression as the main production system;
- vanilla inventory/equipment presentation as the main character interface;
- vanilla cave/strip-mining as the intended core resource-gathering loop;
- Nether/End as mandatory progression gates.

## Vanilla-mob exclusion

The authored RPG world does not naturally populate with vanilla living mobs.

This includes vanilla:

- hostile mobs;
- passive/neutral animals and livestock;
- fish and ordinary aquatic mobs;
- villagers, wandering traders and village golems as the finished NPC population;
- bats and other ambient living mobs;
- vanilla mob spawners embedded in imported structures.

Third-party maps, structures or dependencies that attempt to spawn vanilla mobs must have those spawn paths disabled, filtered or replaced before they become part of normal gameplay. Imported dungeons may keep strong architecture, but their vanilla spawners/encounters are not retained.

Food, hide/leather-like materials, bones and other creature-derived resources come from the custom/external creature ecosystem or project resource systems instead of reintroducing cows, pigs, sheep, chickens or other vanilla animals as shortcuts. Fishing likewise uses the project's custom aquatic roster/reward tables rather than depending on vanilla fish as the visible ecology.

Project/player NPCs use selected custom/external NPC presentation rather than ordinary villagers as the final population.

Vanilla blocks/building/environmental interactions may remain where they improve the open-world sandbox without competing with project systems.

---

# 4. Naming

- Experience: `EXP`
- Level: `Lv`
- Player and enemy level display use the same format: `Lv <number>`
- Vanilla green XP/level presentation is not used for RPG progression.

## Global Lv / EXP progression

### Launch cap

- launch progression has a hard **Lv 80** cap;
- there is no prestige/bonus level or infinite overflow-EXP treadmill at launch;
- the cap is data-driven so a future content expansion can raise it deliberately, but the cap is never raised merely to create more grind;
- post-cap growth comes primarily from class advancement, equipment/build refinement, discoveries, mastery and difficult content rather than hidden extra combat levels.

The current `REGIONS.md` progression already fits this cap: R12 begins at Lv 72, leaving room for extreme encounters and endgame growth before Lv 80. The Economy / EXP benchmark therefore **does not globally rescale the current region suggested-entry values**.

### EXP required per level

For current player Lv `L`, where `1 <= L <= 79`:

```text
EXP_to_next(L) = round_to_10(100 + 50L + 4L²)
```

`round_to_10` means normal rounding to the nearest 10 EXP. The smooth quadratic curve deliberately avoids an MMO-style exponential wall late in the game.

Reference anchors:

| Current Lv | EXP to next Lv | Cumulative EXP from Lv 1 |
|---:|---:|---:|
| 1 | 150 | 0 |
| 8 | 760 | 2,660 |
| 12 | 1,280 | 6,420 |
| 20 | 2,700 | 21,280 |
| 28 | 4,640 | 49,320 |
| 30 | 5,200 | 58,870 |
| 34 | 6,420 | 81,470 |
| 44 | 10,040 | 161,340 |
| 58 | 16,460 | 341,810 |
| 64 | 19,680 | 448,480 |
| 72 | 24,440 | 622,240 |
| 79 | 29,010 | 806,810 |
| 80 | cap | 835,820 |

### Target leveling tempo

These are ordinary mixed-play targets, not speedrun/grind-route guarantees:

| Lv span | Typical active time per Lv |
|---|---:|
| 1–10 | 10–15 min |
| 11–25 | 18–25 min |
| 26–45 | 28–38 min |
| 46–65 | 40–55 min |
| 66–80 | 50–70 min |

A player following varied exploration, quests, dungeons and combat should typically approach Lv 80 after roughly **55–70 hours** of a substantial first playthrough. Focused optimized play can be faster; completionist exploration can be substantially longer. Leveling must not require repetitive mob grinding to remain on the intended region curve.

### EXP reward calibration

Let `X` be `EXP_to_next` for the receiving player's current Lv. For content close to the player's Lv, use the following starting targets:

| Reward source | Base EXP target |
|---|---:|
| ordinary common enemy | ~0.35% of `X` |
| elite | ~2.5% of `X` |
| miniboss | ~5% of `X` |
| field/world boss — first eligible defeat | ~12% of `X` |
| field/world boss — repeat | ~4% of `X` |
| regional contract / normal side quest | ~8–12% of `X` |
| main/regional milestone | ~15–20% of `X` |
| discovery / meaningful dynamic event | ~2–5% of `X` |
| dungeon first-clear completion | ~20–25% of `X`, plus boss reward |
| dungeon repeat completion | ~10–12% of `X`, plus repeat boss reward |

A normal first dungeon should therefore total roughly **30–35% of one Lv** including its boss, while repeat clearing remains worthwhile without becoming the dominant leveling exploit.

Across an ordinary first playthrough, tune toward this approximate source mix rather than enforcing a literal quota:

- ordinary combat: ~20%;
- elites/minibosses: ~12%;
- bosses: ~8%;
- quests/contracts: ~25%;
- dungeons: ~25%;
- discoveries/events and other meaningful exploration rewards: ~10%.

Gathering itself primarily advances its light mastery/economy loop rather than becoming a mandatory combat-Lv grind.

### Level-difference EXP modifiers

Combat reward modifier by `encounter Lv - player Lv`:

- `+6 or more` — 120%;
- `+3 to +5` — 110%;
- `-2 to +2` — 100%;
- player is `3–5` Lv above — 75%;
- player is `6–10` Lv above — 40%;
- player is `11+` Lv above — 10%.

The base reward still uses the receiving player's `X`, so being carried through a much higher-Lv encounter cannot skip huge sections of the level curve. Under-level danger is rewarded modestly, not exponentially.

For quests/dungeon completion, compare player Lv with the content's recommended Lv and use a softer anti-overlevel curve:

- up to 5 Lv under or near intended level — 100%;
- 6–10 Lv over — 75%;
- 11–20 Lv over — 50%;
- 21+ Lv over — 25%.

Do not grant an extra under-level quest multiplier. Sequence-breaking remains allowed because the world is open, but it is not a power-level shortcut.

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
- Creature-based cooking ingredients come from the custom ecology; starting-region examples include Louxia meat and selected non-vanilla wildlife meat.

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

Ultimate HUD uses the externally selected final-quality project UI family from the first playable version.

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

Weapon-freedom rule:

- classes and advancements do not hard-lock ordinary weapon families unless a specific skill physically requires a weapon behavior;
- poor combinations remain possible, but lose the scaling, passive, animation or skill synergy that makes a specialization effective;
- a Marksman can technically carry a sword and a Warrior can carry a bow, but class mechanics naturally make their intended weapon patterns stronger;
- external mods' weapon locks are not inherited automatically if they conflict with this rule.

## 9.2 Advancement depth / branching

- initial production target: about five advancement stages per root-class line;
- architecture remains open for more later;
- never label a stage as `final`;
- use a depth-first structure;
- an early/mid major specialization branch changes playstyle materially;
- the chosen route then continues through multiple deeper stages;
- hidden/special advancements may require bosses, quests, stats, items or discoveries;
- advancement adds mechanics/passives/skill behavior/build identity, not only numbers.

Canonical first major specialization directions:

- **전사**: `공세 계열` / `무기 숙련 계열`
  - 공세 계열 — continuous melee pressure, poise destruction, close-range burst and aggressive tempo.
  - 무기 숙련 계열 — precise guard/parry, counters and weapon-family mastery; personal offensive mastery rather than party tanking.
- **사냥꾼**: `레인저 계열` / `명사수 계열`
  - 레인저 계열 — mobile shooting, multi-target pressure, traps/field utility and flexible mid-range combat.
  - 명사수 계열 — weak-point play, long-range precision and high-risk/high-reward single-target shots.
- **성직자**: `성인 계열` / `심판관 계열`
  - 성인 계열 — healing, cleansing, protection and support buffs.
  - 심판관 계열 — holy marks/judgement, offensive support and combat-healing so solo play remains active.
- **마도사**: `원소술사 계열` / `비전술사 계열`
  - 원소술사 계열 — fire/frost/lightning interaction, elemental combinations and area damage.
  - 비전술사 계열 — teleportation, barriers, binding/control, projectile manipulation and spell-shaping.
- **수호자**: `보루 계열` / `파수꾼 계열`
  - 보루 계열 — aggro, barriers, ally damage mitigation and holding space.
  - 파수꾼 계열 — blocking/parrying, retaliation, attack denial and battlefield control.

Warrior weapon-mastery and Guardian sentinel/counter gameplay must remain distinct:

- Warrior converts personal defensive precision into stronger personal offense and weapon mastery.
- Guardian converts defensive precision into enemy denial, positioning control and party protection.

A major advancement should change at least three of the following when applicable:

1. a core mechanic or combat rule;
2. available active-skill family;
3. passive-tree identity;
4. ultimate candidates or ultimate behavior.

Pure `+X% damage` promotions are not sufficient advancement identity.

Working specialization names may be replaced later by stronger world/lore names without reopening their gameplay identity.

## 9.3 Persistent class history and switching

Progress is stored separately for every class line.

- an untrained class starts from its beginning;
- switching back restores saved advancement/progress/unlocked content;
- class switching costs the core currency/resource;
- cost scales with player `Lv` but has a cap;
- no class-switch cooldown;
- switching never erases learned history.

The player's first root-class choice in the starting settlement is free.
Later switches follow the normal paid switching rule.

Class-switch cost at player Lv `L`:

```text
SwitchCost(L) = round_to_10(min(2500, 50 + 15L + 0.4L²))
```

Reference anchors: Lv 8 = 200, Lv 20 = 510, Lv 40 = 1,290, Lv 60 = 2,390, and the cost caps at **2,500** from the late game onward.

The purpose is to make switching a real economy choice without turning experimentation into a long re-grind. A late-game switch should usually cost on the order of tens of minutes of normal income, not hours.

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

## 9.5 External class / skill reuse policy

Class and skill production is **external-first** in the same way as visual production.
Do not rebuild a lower-quality copy merely to make the mechanic internally original.

- if a current mod already supplies a polished skill, animation, targeting pattern, spell effect, weapon behavior or support mechanic that fits a canonical class, prefer direct dependency/local use or legally permitted selective adaptation;
- ARR content such as RPG Series class mods can be used as dependencies/local content/reference, but their source/assets are not copied into this public repository;
- permissively licensed code such as MIT class/skill implementations may be selectively ported/adapted with required notices;
- Spell Engine is the leading dependency candidate for data-driven active skills and weapon skills because it currently supports 26.2 and provides a complete spell runtime;
- external class names do not automatically replace the five root classes or their advancement identities; good external skills may be mapped into the project's class branches;
- the project-owned EXP/Lv, class history, advancement depth, switch economy, ultimate rules and server authority remain canonical even when external skill content is reused;
- do not inherit vanilla-XP progression, arbitrary crafting requirements, weapon hard locks or other donor-mod rules that conflict with this game;
- every reused skill must still meet the project's visible-range = hitbox, telegraph, animation, resource-cost and multiplayer-authority standards.

Current priority source pool is maintained in `EXTERNAL_SOURCES.md`; important candidates include Spell Engine, RPG Series combat/class modules, RPG Class Selection, Archetypes and Ranged Weapon API.

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

Currency option at player Lv `L`:

```text
DeathCurrencyCost(L) = round_to_10(min(1200, 30 + 6L + 0.12L²))
```

Reference anchors: Lv 1 = 40, Lv 10 = 100, Lv 20 = 200, Lv 40 = 460, Lv 60 = 820, Lv 80 = 1,200.

EXP option:

```text
EXP_loss = min(10% of EXP_to_next(current Lv), 25% of EXP currently earned inside the current Lv)
```

This means a death near the beginning of a Lv loses little EXP, a death near the end of a Lv is capped at 10% of that level's requirement, and an earned Lv can never be lost.

Additional rules:

- the opening approach before the first settlement shrine/checkpoint is activated has no economic death penalty;
- if the player cannot afford the currency option, that option is visibly disabled and shows the shortfall; the EXP option remains available;
- the core currency is server-authoritative account/state data rather than a physical dropped stack, so banking/dropping items cannot bypass the penalty;
- boss/dungeon failure applies the same one-choice penalty at the nearest valid checkpoint/entrance; there is no additional equipment loss or mandatory corpse run;
- penalty-reduction items are not part of the baseline system unless later playtesting proves they add a useful choice rather than another consumable chore.

## Multiplayer down / revive

- lethal damage can enter a temporary downed state;
- teammates can revive the downed player within the rescue window;
- failed rescue leads to normal shrine/checkpoint respawn and the chosen death penalty;
- single-player does not need a fake waiting phase and may proceed directly to defeat/respawn;
- revive behavior is server-authoritative.

---

# 12. Inventory and equipment

The vanilla inventory screen is replaced as the main RPG character interface by the selected external RPG UI language documented in `UI_DIRECTION.md`.

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

- **ordinary/common enemies do not drop equipment as routine loot**; their rewards focus on EXP, currency, creature/material drops and selected consumables;
- equipment drops are concentrated in elites, minibosses, field/world bosses, dungeons, authored treasure, quests and selected merchant/crafting rewards;
- common equipment obtained from valid equipment sources may roll controlled affixes;
- named/boss/signature gear may have fixed identity, visuals and unique mechanics while still being probabilistic drops;
- progression-critical quest/key items must not softlock progress through bad RNG;
- first-clear progression rewards may be deterministic while farmable gear remains probabilistic;
- stronger grade does not always mean strict numeric replacement;
- loot density must not create a constant inventory-cleanup chore.

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

Each important workstation gets a purpose-built screen from the shared external UI language documented in `UI_DIRECTION.md`. Physical workstation/building appearance follows the same external-first rule.

Avoid production steps that only add clicks.

---

# 16. Open world / settlements

Use a high-quality external open-world RPG map/terrain solution rather than hand-authoring the entire continent from zero.

**Azari 30k x 30k is the current primary terrain candidate and region planning proceeds against it.** Its world bytes remain local/private while usage terms are still being verified. If its import or terms fail, preserve the same gameplay/region principles on the strongest free fallback rather than reopening every system decision.

Target about **12 major regions**, adjustable to roughly 10–14 if the selected external map's geography strongly supports a different count.
Each region contains subregions/landmarks/POIs and defines environmental identity, danger/Lv profile, enemies, elites, signature threats, resources, gear identity, settlements, dungeons, camps/checkpoints, quests/events and traversal gimmicks.

## Enemy Lv model

Use **one suggested-entry Lv per major region plus local encounter Lv** rather than broad overlapping region ranges.

- each major region has one clear suggested-entry Lv indicating when an average build should first feel comfortable there;
- subregions, dangerous POIs, elites, dungeons and bosses can sit above or below the region's entry recommendation;
- peer regions may intentionally share the same or nearly the same recommended Lv when they are alternate routes with similar overall difficulty;
- do not universally scale every enemy to the player;
- a low-Lv region remains low-Lv later and a dangerous region remains dangerous when entered early;
- recommended Lv is guidance, not a hard gate; high-Lv areas remain physically enterable.

The concrete values and region graph are maintained in `REGIONS.md`. The Economy / EXP benchmark confirmed that the current progression already fits the Lv 80 launch cap, so the existing suggested-entry values are retained without a global rescale.

Early difficulty direction:

- the starting region is approachable rather than punishing;
- ordinary enemies still require basic positioning and combat attention, but should not repeatedly kill a new player who is learning the systems;
- major difficulty spikes begin with elites, dangerous POIs, minibosses, field bosses and dungeons;
- high-Lv areas are never blocked by invisible walls or arbitrary story gates merely because the player is under-levelled.

## Starting settlement

The game has a memorable starting village/settlement as the first safe social/service hub.
Its final name and lore are decided later; do not use a generic vanilla village as the finished result.

The starting settlement is a real long-term hub, not a disposable tutorial town.
Its buildings exist coherently from the beginning; progression primarily unlocks advanced services or access rather than making arbitrary buildings appear out of nowhere.

### Opening / first reveal

- start on an approach road or settlement outskirts rather than spawning directly in the central plaza;
- use a short roughly 2–4 minute playable approach/encounter to establish movement/combat/context without a long forced tutorial;
- reaching the gate/entrance gives the first clear reveal of the settlement and safe hub;
- do not turn the opening into a chain of NPC errands before the player is allowed to explore.

### Immediately usable services

From the first settlement visit, provide at least:

- nearby shrine/checkpoint;
- inn/tavern rest;
- basic merchant/market access;
- bank/storage;
- adventurer/quest guild;
- basic forge/smith service.

The first root-class selection happens through the guild/class facility and is free.

### Physical first-use flow

Prefer a readable world layout such as:

```text
Gate / arrival
→ inn / central square
→ adventurer guild / class access
→ forge / smith
→ quest board / exits
```

The architecture/signage/NPC placement should teach the route visually.
Avoid a compulsory sequence of trivial `talk to banker → talk to smith → talk to merchant` tutorial errands.

### Service growth

- the forge building is usable immediately for basic commerce/service, while deeper crafting/enhancement can unlock after the player actually gathers early ore/timber resources;
- alchemy/healer access can introduce deeper alchemy after the player first gathers relevant herbs;
- these unlocks exist to connect `explore/gather → return → produce/upgrade → explore again`, not to add menu chores;
- service unlock conditions must remain short, legible and impossible to miss permanently.

### Quest board / guild density

At the beginning, expose roughly:

- 1 main objective;
- 2–3 regional contracts/side objectives;
- additional work revealed through exploration, discoveries and regional progress.

Do not fill the starting hub with a wall of MMO-style chores on first arrival.

### Stable / first mount

- the stable and mount NPC should be visible from the beginning so mount progression is an obvious future goal;
- the first usable ground mount is unlocked through an early first-region event/quest rather than given immediately;
- the first mount arrives quickly, before the first region is exhausted;
- its travel speed is around ordinary player sprinting speed rather than a huge early skip, while sustained travel and convenience make it worthwhile;
- the first visible mount is a **non-vanilla creature/model**; vanilla horses/camels/etc. do not return as the world's visible mount population;
- later mounts provide the meaningful speed/handling/combat/flight progression.

### Housing

- starting-settlement homes may be inspected and purchased from the beginning;
- there is **no story, boss-clear or reputation permission gate for the right to buy the first home**;
- normal price/economy is the gate: starting funds do not trivially buy a home;
- the first normal starter home costs **2,400 core-currency units** at baseline;
- a starter furnishing/storage package should cost roughly **600–900** additional units, so owning the shell and fully furnishing it are separate early goals;
- with the target R01 income curve, a savings-focused player can normally reach the first home in roughly **5–7 hours** without dedicated currency grinding;
- later ordinary homes may occupy roughly 7,500–12,000 and 20,000–35,000 bands, while 60,000+ prestige properties are optional late-game sinks rather than progression requirements.

### Exits / open-world signal

The settlement should connect to at least three meaningful directions when the selected external map permits it:

1. a main road toward first-region core content;
2. a secondary route toward gathering/small POIs/exploration;
3. a visibly riskier route toward a higher-Lv area or dangerous encounter.

Use recommended-Lv danger rather than invisible walls to communicate that the world extends beyond the intended first path.

The village layout and buildings come from a coherent external high-quality village/build family or map/schematic.
Avoid stitching together unrelated building styles when a coherent pack/source exists.

## R01 — first-region content package

`R01` is an internal production identifier. Final player-facing names wait until the actual Azari terrain is imported and lore naming is coherent with the world.

### Terrain / role

Use Azari's central or south-central **meadow / plains / river / forest-fringe** geography for the first region.

The region should teach the world by contrast rather than tutorials:

- open meadow around the starting route and settlement;
- river/woodland transition for exploration and herbs;
- an old quarry/cave complex for the first dungeon route;
- a deeper grove or rugged edge for elite/field-boss encounters;
- at least one obvious route leading into a visibly more dangerous neighboring region.

**R01 suggested entry Lv is 1.** Local encounter pressure rises through roughly Lv 1–8 across the opening road, meadow, forest edge, quarry, dangerous grove, elites and first dungeon. This `1–8` spread describes encounter progression inside R01; it is **not** a broad regional recommendation band.

Working local pressure:

- arrival road / settlement outskirts — roughly Lv 1–2;
- open meadow and foraging routes — roughly Lv 1–4;
- riverwood / forest fringe — roughly Lv 3–5;
- quarry / cave approach — roughly Lv 4–6;
- deep grove / dangerous ridge / first major threats — roughly Lv 6–8;
- neighboring-region exits may immediately expose stronger enemies beyond the intended early route.

### Wildlife / food ecology

No vanilla livestock or animals spawn here.

Primary early ecology uses selected external creatures rather than filling every biome with every installed mob:

- **Louxia** — common passive herd creature in plains/meadows; main early creature-food source and a candidate for later controlled breeding/farming; its luminous organ/material can also feed early utility/crafting loops;
- **gazelle** — optional/common open-meadow prey/wildlife from Alex's Mobs Continued; useful for meat/hide-style resources after project loot normalization;
- **bison** — uncommon herd wildlife; neutral until threatened and substantially more dangerous than a basic food animal; useful as higher-yield meat/hide ecology rather than a cow replacement reskin;
- **raccoon / crow** — low-impact ambient/foraging wildlife near forest edge, roads and settlement outskirts;
- **grizzly bear** — rare territorial forest-edge wildlife that can punish careless early exploration without being treated as a routine trash mob.

External default loot is not automatically canonical. Project loot tables normalize food/material yields so one dependency does not dominate the economy.

### Common combat threats

The first region is not zombie/skeleton replacement spam. Common danger comes from understandable wildlife/creature behavior:

- rattlesnake-style ambush/area-denial creature in rocky grass and warm banks;
- cave centipede-style close-range threat in quarry/caves;
- territorial grizzly/bison encounters where aggression is readable and avoidable;
- additional small non-vanilla creature threats may be selected only after their 26.2 behavior is inspected in-game.

Common threats provide EXP, currency/materials and creature resources, **not routine equipment drops**.

### Elites / field boss

Use Threateningly Mobs Continued selectively rather than accepting its default world-wide spawn rules:

- **Steelboar** — first-region elite hunt candidate; armored charge/impact identity, placed in specific meadow/woodland pockets rather than common spawn spam;
- **Nature Spirit** — rare magical elite in the deeper grove; teaches that regional ecology can shift from natural wildlife into fantasy threats;
- **Regalhart** — first-region optional field-boss candidate, discoverable through exploration/hunt clues rather than mandatory story gating.

Their original mod stats/loot/spawn rates are reference inputs only. Project data owns Lv, HP/damage, stagger, rewards, respawn and placement.

### First dungeon

Working concept: **an abandoned quarry / root-overgrown underground complex** connecting the region's mining/resource loop to its fantasy ecology.

- target first-clear length: short-to-medium, roughly 15–25 minutes rather than a huge early maze;
- use an external high-quality dungeon/structure shell where terms permit;
- any imported vanilla mob spawners are removed/replaced;
- 2–3 meaningful combat spaces plus traversal/side-cache choices are preferable to many copy-pasted rooms;
- quarry layers first teach gathering/resource visuals, then transition into root/magic corruption deeper inside;
- **Earthloong** is the current external boss candidate for the deepest chamber because its forest/earth identity fits the region and gives the first dungeon a non-vanilla silhouette;
- first-clear reward includes a deterministic meaningful equipment choice through the settlement/smith flow so bad RNG cannot leave a new player without useful progression;
- repeat clears focus on regional materials and a controlled chance at a signature Earthloong-themed item rather than flooding the inventory with random gear.

DeCubed Dungeons is a current free 26.2 Fabric/datapack architecture candidate, but its vanilla spawners/loot are not adopted unchanged. Final shell selection happens during map integration.

### First-region resources

Keep the first gathering loop simple and immediately connected to settlement services:

- common ore/mineral outcrops for the forge;
- timber nodes along woodland routes;
- common healing/alchemy herbs around river/forest transitions;
- food/foraging ingredients in meadow and riverside areas;
- one rarer quarry/grove material that gives the player a reason to revisit the region later.

The first hour should demonstrate `explore → gather → return to settlement → improve/craft → go back out` without making crafting mandatory busywork.

### Quest / discovery flow

Do not front-load NPC chores.

Recommended first-region progression:

1. short approach encounter → settlement reveal;
2. free first root-class selection and access to basic services;
3. main objective points the player toward a disturbed road/quarry situation without forcing every side service tutorial;
4. 2–3 optional regional contracts expose hunting, gathering and a dangerous POI;
5. an early regional event/quest unlocks the first non-vanilla ground mount before the region is mostly complete;
6. quarry investigation becomes the first replayable dungeon;
7. Regalhart remains an optional field-boss discovery rather than a gate;
8. completing the region strongly suggests neighboring routes but never locks the player inside R01.

### Multiplayer feel

R01 must work both together and separately:

- players may split between gathering, contracts and exploration without losing each other's permanent progression;
- personal combat loot and personal node state remain active;
- dungeon/field-boss participation rewards each eligible player individually;
- no party-presence requirement for ordinary exploration.

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

Minimap/world-map visuals use the selected project UI language documented in `UI_DIRECTION.md`.

---

# 18. Mounts and traversal

Use **non-vanilla visible mounts** as a real progression/travel system.

- the first mount arrives early in R01 and mainly improves sustained travel convenience, with speed around ordinary sprinting rather than instantly trivializing the map;
- later mounts can differ meaningfully in speed, acceleration/handling, toughness and role;
- some mounts may support mounted combat or combat utility;
- use external high-quality models/animations and proven mount implementations/code where suitable;
- vanilla horse/camel/etc. visuals are not used as the finished mount population;
- mounts are not cosmetic reskins with identical stats;
- later progression unlocks substantially faster traversal;
- late-game traversal may include flying mounts or another high-speed system;
- flight must not arrive so early that terrain, danger and dungeon approaches become irrelevant.

---

# 19. Enemies, bosses, world events and dungeons

The visible world ecology and combat roster use **non-vanilla creatures/NPCs**.
Important enemies require readable states, attack telegraphs, recovery, custom visuals/animation/VFX/sound appropriate to importance.

Creature sourcing is external-first but curated by region:

- do not enable every dependency's global default spawns;
- region definitions decide which wildlife/enemies can appear, at what density and in what role;
- ordinary wildlife, normal threats, elites, minibosses, field bosses and dungeon bosses are distinct content tiers;
- default dependency stats, loot, dimensions and progression are not automatically inherited;
- vanilla-derived reskins/variants are low priority and cannot become the primary ecology under the no-vanilla-mob rule.

Food/resource ecology is part of monster design. Passive/neutral custom creatures must provide enough meat/hide/bone-like resources that removing vanilla livestock does not create a survival-resource hole.

Accepted iconic directions include:

- desert earth/sand-worm-style giant monsters;
- dragons;
- region-specific giant creatures and field bosses.

## Spawn ownership / natural generation

The project owns the authored RPG ecology instead of accepting every dependency's global defaults.

Implementation direction on Fabric 26.2:

- use Minecraft/Fabric's normal loaded-chunk natural-spawn systems rather than scanning the whole world every tick;
- remove vanilla/default spawn entries from authored RPG biomes and add only approved regional custom creatures;
- ordinary wildlife and common threats may use natural spawning with project-owned region/biome weights and group sizes;
- elites, field/world bosses, dungeon bosses, quest mobs and event encounters use authored encounter controllers/locations/conditions rather than uncontrolled natural-spawn spam;
- separately suppress vanilla spawn paths that do not come only from biome tables, including imported spawners and special vanilla systems as applicable;
- a final server-side safety filter may reject forbidden vanilla entities if another dependency leaks them into authored gameplay;
- do not simulate the entire 30k world ecology while unloaded; offscreen predator/prey simulation is deliberately bounded for performance and reliability;
- validate density and spawn performance with profiler/playtest rather than guessing.

Exact implementation may reuse permissive external spawn-filter/code solutions where they reduce risk, but project region rules remain authoritative.

## Dynamic world events

Use region-specific dynamic events at controlled frequency.
Examples may include caravan attacks, rare dragon appearances, night creatures, magical weather/incursions or roaming major monsters.
Events should feel discovered in the world, not like constant MMORPG chores.

## Dungeon replay

Dungeons are replayable.
First clear provides unique/progression-significant rewards where appropriate; later clears provide probabilistic gear, materials and rare drops.
External dungeon architecture is retained where strong while encounters, enemies, bosses and rewards are project-specific.
Imported vanilla spawners/encounters are always replaced.

## Field/world bosses

Field/world bosses can reappear under suitable timers/conditions.
First defeat may have unique progression rewards; repeats focus on probabilistic rare drops/materials.
Avoid rapid repetitive respawn loops.

---

# 20. Quests

Use a mixed structure weighted toward exploration:

- main narrative for world/system introduction and major progression;
- regional quest chains that give each area identity;
- free exploration/discovery without constant quest-marker following.

Target overall feel is approximately **30% guided objectives / 70% free exploration and self-directed discovery**, not a rigid numerical quota.
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
- the right to buy the first available home is present from the beginning rather than unlocked by story/boss progression;
- price and available funds create the practical early-game gate instead of an arbitrary permission gate;
- player buys a residence rather than turning a field camp into a full permanent base;
- house shells/interiors use coherent external building designs/assets;
- house functions include rest, storage, furnishing/decor and trophy/collection display;
- the house does not automatically replace every town service such as forge/alchemy, because those service buildings need to remain meaningful world locations;
- baseline house price bands are defined in the starting-settlement housing section and economy tuning, not independently per UI.

---

# 23. Multiplayer / Essential

Essential-friendly multiplayer is a major usability goal.
Connection convenience does not own game authority.

Server-authoritative state includes damage, item ownership, currency, EXP/Lv, skill cost/success, class/progression, quests, world state and saves.

Combat/loot rewards are personal per player.
Resource gathering is also personal per player at the node-availability level.

Players can travel together or pursue separate exploration/content and regroup later. Ordinary progression must not require constant party proximity.

Do not claim multiplayer quality until actually tested.

---

# 24. Economy and merchants

## Core currency

Use **one primary numeric currency** for the ordinary economy.

- player-facing currency name/icon are not locked yet and should be chosen with world lore;
- documentation may refer to raw numeric values as `core-currency units`; that phrase is not a player-facing currency name;
- avoid copper/silver/gold denomination conversion unless it later proves valuable;
- add special currencies/tokens only when a specific activity genuinely needs a distinct reward loop;
- do not multiply currencies merely to make the game look larger.

The core currency supports at least class switching, death-penalty choice, merchants, housing and selected services.
Starting liquid currency target is roughly **150 units**: enough for basic supplies and a mistake, nowhere near enough to trivialize the first house.

## Economy feel / income curve

Early and midgame should feel **slightly money-constrained but not grind-starved**: the player usually has several attractive uses for currency and must choose priorities, while normal play still funds essential recovery and progression. Later progression becomes more financially comfortable rather than maintaining artificial scarcity forever.

Target ordinary gross and routine-spend-adjusted net income:

| Lv span | Gross currency / active hour | Typical net after routine consumables/services |
|---|---:|---:|
| 1–9 | 500–700 | 350–500 |
| 10–24 | 900–1,400 | 600–900 |
| 25–44 | 2,000–3,200 | 1,200–2,000 |
| 45–64 | 4,000–6,500 | 2,500–4,500 |
| 65–80 | 7,000–11,000 | 4,500–8,000 |

Routine unavoidable spending should normally remain below roughly one third of gross income. Larger optional purchases create the meaningful tradeoffs; essential recovery should not force a currency grind.

### Primary currency sources

Tune first-play gross income roughly around these roles rather than a rigid exact split:

- quests/contracts and first-clear objective payments — largest reliable source;
- normal combat — small steady currency, with elites/bosses paying noticeably more;
- dungeon clears/chests and world events — major burst income;
- selling unwanted equipment and genuine material surplus — useful secondary source, not the dominant optimal loop;
- selected gathering/crafting orders or regional trade opportunities — bounded supplemental income.

Do not make repetitive slaughter of the easiest common mob, relogging a chest or merchant arbitrage the best currency strategy.

### Primary sinks

Core sinks are:

- potions, food and selected consumables;
- class switching and any future justified respec service;
- merchant equipment/special rotating stock;
- forge/alchemy/crafting service fees and materials;
- housing, furnishing, trophy/display and storage expansion where appropriate;
- later mounts/stable and selected travel/service purchases;
- chosen currency death penalty.

Fast travel does not need a routine tax merely to delete money. Add a travel fee only if later playtesting proves it creates an actual route/economy choice rather than friction.

## Merchant stock / refresh

Merchant inventory is **mostly randomized/rotating**, but essential items and explicitly designated goods remain fixed.

Fixed/reliably available stock includes:

- core potions/recovery necessities;
- required basic supplies;
- explicitly progression-safe goods that should not disappear because of RNG.

Rotating equipment, regional goods, special materials and rare finds refresh every **60 minutes of active server/world playtime** by default.

Refresh rules:

- sleeping, reopening the UI, relogging or changing the Minecraft clock does not force a reroll;
- stock generation is deterministic from stable world/merchant identity plus refresh-cycle state so simple save/reload abuse does not reroll it;
- the server owns the current catalog and cycle timing;
- ordinary rotating purchase availability is personal per player in multiplayer so one player cannot empty another player's normal shopping opportunity;
- intentionally unique world-event lots may be shared only when scarcity itself is the authored mechanic.

Do not add separate refresh timers for every merchant tier at baseline. Keep one readable cadence until real playtesting proves a second cadence materially improves the economy.

## Merchant pricing structure

Price items by their own tier/identity and regional economy, not by silently scaling the exact same item to the buyer's Lv.

Target affordability relative to same-tier gross income:

- basic recovery/consumable: roughly 1–3 minutes of income;
- ordinary current-tier gear purchase or clear upgrade: roughly 10–20 minutes;
- strong rotating higher-grade gear: roughly 30–60 minutes;
- rare/signature merchant item: roughly 1.5–3 hours and therefore a deliberate savings target.

R01 starting anchors before the later loot-economy pass:

- food/basic utility consumable: ~10–25;
- basic potion: ~25–40;
- ordinary starter weapon/armor shop item: ~120–250;
- strong R01 rotating gear: ~350–600;
- rare/signature R01 merchant item: ~900–1,500.

Sell-back baselines:

- equipment: **25%** of standard buy value;
- materials: **35%**;
- consumables: **20%**.

Specific authored trade goods may override these values, but buy/sell tables must never permit deterministic merchant-to-merchant arbitrage.

Meaningful reputation may alter prices by at most roughly **±10–15%** unless a rare authored faction rule explicitly justifies more. Reputation is not allowed to turn the ordinary economy into a mandatory grind.

Housing baseline remains 2,400 for the first starter home, with later price bands as defined in the settlement section. Class switching and death-cost formulas are defined in their own canonical sections so merchant tuning cannot silently change them.

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

# 26. UI / UX canon

The UI visual direction is now selected and is no longer an open user-choice question.
Detailed screen rules live in `UI_DIRECTION.md`.

Canonical direction:

- **Foozle `Lucifer - RPG UI`** is the primary free CC0 visual family;
- **Foozle `Lucifer - Equipment`** is the matching equipment/inventory companion;
- **Kenney `Fantasy UI Borders`** supplies scalable CC0 9-slice support where the primary family lacks a suitable resizable frame;
- **Kenney `UI Pack - Adventure`** may supply missing low-level CC0 controls only, not a competing visual theme;
- preserve the external family's pixel density, framing, ornament language and component consistency while adapting the donor pack's heavy red/infernal palette to the project's broader fantasy world;
- dark stone/iron/charcoal surfaces with restrained bronze/gold trim form the baseline; saturated red is reserved for danger/high-impact states instead of every control;
- HP/Mana/Stamina and 4-skill + ultimate presentation use the same visual language;
- Spell Engine or another runtime may own skill behavior, but its default UI does not override project visual canon;
- inventory is a purpose-built RPG character screen with the canonical 12 slots, larger backpack, material category/pouch and contextual comparison—not a vanilla inventory reskin;
- forge, alchemy, cooking, class/advancement, map, death/respawn and other important screens reuse the same component/spacing grammar;
- no generic black translucent panel phase, no unrelated UI-pack collage, no temporary vanilla buttons;
- real Minecraft-client screenshot review at multiple GUI scales/resolutions is required before a screen is visually accepted.

Backend/UI-library candidates are not visual canon. Current candidates include a current 26.2 Fabric accessory backend such as Trinkets Updated where it fits; older RPG Inventory architecture remains reference/code material unless 26.2 compatibility is established.

---

# 27. Current locked decisions

Major locked decisions as of 2026-09-15:

- private-use large open-world action RPG with very low vanilla progression dependence;
- Fabric is the locked mod loader for this project unless a future hard technical blocker forces a deliberate migration review;
- Azari 30k x 30k is the primary free terrain candidate and region planning proceeds against it while local-use/import terms are verified;
- external-first visuals/assets from the first visible/test implementation;
- UI visual language is the free CC0 Foozle Lucifer RPG UI + Lucifer Equipment family, with Kenney Fantasy UI Borders/Adventure only as supporting scalable/control primitives; details live in `UI_DIRECTION.md`;
- no temporary player-facing design;
- dead/superseded/duplicate code removed after safe replacement;
- `EXP` / `Lv` notation and removal of vanilla XP progression/drop loop;
- launch combat-Lv cap is Lv 80 with a smooth quadratic EXP curve, no launch prestige/overflow-Lv treadmill, and typical varied first-play progression toward cap in roughly 55–70 hours;
- current R01–R12 suggested-entry values remain unchanged after the EXP benchmark; R12 begins at Lv 72 and leaves endgame headroom before the Lv 80 cap;
- ordinary combat is only a minority of total EXP; quests, dungeons, elites/bosses and exploration rewards keep leveling tied to the whole game loop rather than mob grinding;
- **no vanilla mobs as normal world population**: hostile mobs, animals/livestock, aquatic mobs and ordinary villager/golem population are replaced by the custom/external ecosystem and NPC roster;
- creature-derived food/materials come from non-vanilla wildlife/livestock equivalents; imported vanilla spawners are replaced;
- project region/spawn rules own normal ecology; ordinary creatures may use natural spawning while elites/bosses/events use authored encounter logic;
- HP + Mana + Stamina; Stamina is primarily non-skill action resource; basic attack costs no Stamina;
- dodge, guard, parry and layered stagger/poise combat;
- 4 active skills + 1 high-impact hybrid-charge ultimate;
- VIT / END / STR / DEX / INT / WIL primary stats;
- five root classes: 전사 / 사냥꾼 / 성직자 / 마도사 / 수호자;
- deep ~5-stage initial advancement target, one major playstyle-changing branch, no `final` terminology;
- first major specialization directions are 전사 공세/무기숙련, 사냥꾼 레인저/명사수, 성직자 성인/심판관, 마도사 원소술사/비전술사, 수호자 보루/파수꾼;
- major advancements must change mechanics/skills/passives/ultimate identity rather than only numeric stats;
- class/skill production is external-first: reuse high-quality current external skills/dependencies or legally reusable code when they fit instead of rebuilding weaker copies;
- Spell Engine and the current 26.2 RPG Series ecosystem are priority dependency/content candidates; MIT sources such as RPG Class Selection and Archetypes are selective code-port candidates;
- persistent per-class progression with paid Lv-scaled/capped switching and no switch cooldown; first class free; switch cost caps at 2,500 core-currency units;
- first root-class selection in the starting settlement is free;
- all unlocked passives of the active class apply; inactive-class passives never leak across;
- broad weapon freedom: classes create natural weapon synergy rather than ordinary hard weapon locks; Hunter-compatible black-powder firearms remain allowed and modern firearms excluded from baseline;
- 12-slot RPG equipment target and no routine durability chore;
- larger expandable RPG inventory, no weight system, material pouch/category and stack sizes above vanilla 64 where appropriate;
- ordinary enemies do not routinely drop equipment; equipment farming centers on elites/bosses/dungeons/authored rewards/merchants/crafting;
- hybrid random-affix + named/signature equipment; about five non-cliché grades later;
- medium damage/status complexity;
- light smithing/alchemy/cooking mastery;
- roughly 12 major regions with **one suggested-entry Lv plus local encounter Lv**, peer regions allowed at equal difficulty, no universal scaling, no level-gate walls;
- discovered POIs and shrine/major-hub fast travel;
- starting region suggested entry Lv 1, with local early encounter pressure rising roughly through Lv 8 rather than treating 1–8 as a broad region recommendation band;
- starting region is approachable while elites/POIs/bosses provide the first major difficulty spikes;
- high-Lv regions remain physically enterable rather than being blocked by invisible/story walls;
- ground mounts use non-vanilla visible creatures/models; first mount arrives early at about sprint-speed convenience, later mounts provide meaningful speed/handling/combat/flight progression;
- mixed main/regional/free-exploration quests with roughly 30/70 guided-vs-free-exploration feel, dynamic region events, replayable dungeons, respawning field/world bosses;
- probabilistic drops with deterministic protection for progression-critical items;
- multiplayer down/revive and fully personal loot;
- death offers a capped Lv-scaled currency payment or current-Lv-only EXP loss; earned Lv never decreases, opening pre-shrine deaths are free, and no extra corpse/equipment-loss layer is added;
- multiplayer allows players to explore together or separately and regroup without ordinary progression requiring party proximity;
- RPG field resource nodes instead of cave/strip-mining as the core gathering loop;
- personal node gathering state in multiplayer;
- mining/herbalism/forestry/fishing-foraging categories with light mastery;
- dedicated no-routine-durability gathering tools, separate from combat slots;
- common node regeneration and slower/conditional rare-node regeneration;
- one primary currency; player-facing name chosen later with world lore;
- economy is slightly constrained in early/midgame and becomes more comfortable later; ordinary routine spending should not consume most income;
- merchants use fixed essentials plus rotating stock on a 60-minute active-world-time cadence; reopen/relog/sleep cannot reroll them;
- baseline sell-back ratios are 25% equipment / 35% materials / 20% consumables and deterministic arbitrage is forbidden;
- quick-build material-cost camps;
- permanent houses are purchased separately in settlements and provide rest/storage/decor/trophy functions;
- first-home purchase permission exists from the beginning; the starter house baseline is 2,400 core-currency units and functions as roughly a 5–7 hour savings goal rather than a story gate;
- starting settlement begins with a short approach/reveal, then immediate shrine/inn/basic merchant/bank/guild/basic-smith access;
- starting settlement uses a visually taught gate → square/inn → guild → smith → board/exits flow rather than mandatory NPC errand chains;
- forge/alchemy deeper functionality is introduced through early gathering so exploration → gathering → return → production forms an immediate loop;
- starting quest density is intentionally low: about 1 main objective plus 2–3 regional contracts before discoveries add more;
- the stable is visible immediately but the first non-vanilla ground mount arrives through early first-region progression;
- starting settlement should expose multiple routes including an intentionally dangerous higher-Lv direction when geography supports it;
- R01 is an Azari central/south-central meadow/river/forest-fringe region with suggested entry Lv 1, Louxia-led custom food ecology, curated non-vanilla wildlife, Steelboar/Nature Spirit elites, Regalhart field-boss candidate and Earthloong first-dungeon boss candidate;
- R02–R12 terrain identities, creature roles, bosses, resources, dungeon directions and suggested-entry values are expanded in `REGIONS.md`;
- selected faction/reputation systems only where meaningful;
- moderate day/night/weather gameplay effects;
- starting settlement and all important buildings use coherent external architecture/designs;
- Essential-friendly server-authoritative multiplayer target.

---

# 28. Next design queue

Do not re-decide the locked systems above. Continue from here without asking the user to reselect details that can be solved through research, external assets or normal balance work.

Completed/advanced design work that should **not** be restarted from zero:

- Azari provisional R01–R12 regional expansion;
- regional creature/ecology sourcing and no-vanilla spawn architecture;
- external UI family selection and screen-language direction;
- global Lv 80 / EXP reward benchmark and no-rescale confirmation for current region entry levels;
- core-currency income/sink targets, class-switch cost, death penalty, starter housing prices and merchant refresh/pricing structure.

Recommended next batch:

1. **Inventory numbers** — set starting slot count, expansion steps, material-pouch behavior and practical stack caps above 64 from real item density and the selected UI rather than re-asking the user.
2. **Loot economy** — set affix count/ranges, grade probabilities, elite/boss drop rates, deterministic first-clear protections and targeted bad-luck protection while keeping ordinary enemies equipment-free.
3. **Non-vanilla mount sourcing** — select actual free/current custom-creature mount candidates for the first ground mount and later traversal tiers while retaining useful permissive riding/QoL code where appropriate.
4. **M0 Fabric dependency audit** — verify exact current 26.2 integration boundaries for Azari import/spawn filtering, creature mods, Spell Engine, RPG Series modules, class/accessory/inventory candidates, ranged/combat libraries and Essential compatibility before source bootstrap.
5. **Final keybind audit** only after the complete frequent-action list is known; important Minecraft/Essential keys must not conflict.

When design direction becomes unclear, research real open-world RPGs, open-source RPGs and large Minecraft RPG mods before inventing filler systems.