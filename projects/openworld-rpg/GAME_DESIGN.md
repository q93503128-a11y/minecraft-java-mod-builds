# Open-World RPG — Master Game Design Canon

> Status: DESIGN CANON / continuously maintained  
> Final game title: **WORKING / CANDIDATE ONLY — not yet locked; not a gameplay-source-bootstrap gate; the internal `openworld-rpg` slug is never the finished player-facing title**  
> Canon priority: current GitHub `main` > `PROJECT.md` > this file > subordinate project docs > older conversations

This file is the single source of truth for gameplay/design decisions below the project-level contract. Do not create a competing master design document. When a decision changes, edit the existing section instead of leaving contradictory versions behind.

Subordinate references currently indexed by this canon:

- `REGIONS.md` — regional expansion/content details for Azari.
- `UI_DIRECTION.md` — selected external UI family, screen architecture and visual acceptance rules.
- `ACCESSIBILITY_DIFFICULTY_INPUT_AUDIO.md` — closed World Challenge, personal-assist, frequent-action input, subtitles/captions, non-audio cue, camera/VFX comfort and dynamic audio/music behavior contract.
- `LOOT_ECONOMY.md` — equipment grades, affixes, drop rates, target farming, first-clear protection and signature-material rules.
- `EQUIPMENT_BALANCE.md` — Item-Lv/base-stat curves, exact affix ranges, forge/reforge rules and R01 equipment/resource catalog.
- `MOUNTS.md` — non-vanilla mount roster, traversal balance, summon/combat rules and external visual sources.
- `M0_DEPENDENCY_AUDIT.md` — pinned Fabric 26.2 toolchain/dependency/integration boundaries.
- `COMBAT_BALANCE.md` — combat formulas, dodge/guard/parry timing, poise, TTK, encounter damage and multiplayer scaling.
- `CLASS_COMBAT_KITS.md` — five root-class combat kits, first specialization branches and external animation/VFX/icon direction.
- `CLASS_PROGRESSION.md` — Class Rank/XP, advancement beats, passive economy, deeper branch mechanics and world-discovered skills.
- `STATUS_AND_R01_ENCOUNTERS.md` — element/status rules and concrete R01 ecology/elite/field-boss/first-dungeon combat kits.
- `R01_VERTICAL_SLICE.md` — Alderford opening settlement, external player-motion bindings and first 55–75 minute playable R01 route.
- `R01_CONTENT_BIBLE.md` — full-region R01 content authority for subregions, dynamic event, ecology-density seed, quarry/Regalhart repeat controllers, exact service stock, Camp acquisition, profession opportunities, fishing mechanical slots, Alderford property roster, NPC presence and post-clear state.
- `R01_UI_PRODUCTION_SPEC.md` — exact R01 HUD/screen hierarchy, first-class/board/journal/merchant/service/housing/fishing/reward interaction flow, commit points, reconnect/error states and real-client visual acceptance.
- `RECOVERY_PRODUCTION_APPEARANCE.md` — recovery belt, potions, food, alchemy/cooking, light profession mastery and external-first armor/apparel/Wardrobe rules.
- `R01_ASSET_INTAKE.md` — current R01 exact asset/provenance intake state and unresolved presentation gates; Phase-B Pass 4 evidence is integrated but R01 is not asset-ready.
- `GATHERING_FISHING_CAMP_HOUSING.md` — Tool Pouch, gathering mastery/timing, fishing, reusable Field Camp Kit, housing/storage/furnishing and authority rules.
- `FISHING_COLLECTION_HOUSING_MARKET.md` — Fish Codex/records/trophy loop and the authoritative one-residence-at-a-time housing trade-up refinement.
- `QUEST_WORLD_STATE.md` — personal/shared quest state, objective credit, dialogue, dynamic events, late join, idempotent rewards and multiplayer quest authority.
- `PARTY_MULTIPLAYER.md` — formal party UX, participation, personal reward ownership, co-op scaling and multiplayer acceptance rules.
- `DESIGN_COMPLETENESS_AUDIT.md` — design-completeness and finished-game-quality audit with external open-world RPG production lessons and current five pre-code gates.
- `REGION_CROSS_AUDIT.md` — R01–R12 anti-repetition and regional-identity audit covering encounter/dungeon/evidence/service/resource/travel grammar; later explicit refinements in this file govern older regional-package details unless the master canon says otherwise.
- `WORLD_STORY_CANON.md` — customizable protagonist frame, Anchor-network premise, factions/recurring roles, non-linear acts, regional evidence structure, three personal endings and postgame state.
- `MAIN_QUEST_SCENE_PACKAGE.md` — cross-region main-quest route, scene/rejoin ownership, sequence-break handling and multiplayer-safe major-story progression.
- `R11_AQUATIC_ACTION_MATRIX.md` — closed R11 aquatic action compatibility, swim-base layering, 3D targeting and `AQUATIC_NATIVE / AQUATIC_ADAPTED / AQUATIC_DISABLED_WITH_FALLBACK` behavior.
- `R02_IMPLEMENTATION_PACKAGE.md` through `R12_IMPLEMENTATION_PACKAGE.md` — content/mechanics-closed regional packages that turn the broad region graph into concrete settlement, traversal, ecology, encounter, dungeon, reward, story-evidence and external-asset gates. Their older `implementation-ready` wording never waives project-wide pre-code asset/spatial gates.

If a subordinate reference conflicts with this file, this file wins unless the higher-priority `PROJECT.md` explicitly records a later project-level correction.

---

# 1. Vision

Build a large open-world action RPG on Minecraft Java with extremely low dependence on vanilla RPG/progression systems. Minecraft supplies the block world, runtime, input foundation and multiplayer host model; the actual game identity comes from this project.

Core loop:

```text
Explore
→ discover region / POI / settlement / shrine / camp / dungeon / encounter
→ fight / gather / complete objectives
→ gain EXP, Gold, equipment, materials and skills
→ alter the build or advance a class
→ challenge more dangerous areas
→ defeat elites / field bosses / dungeon bosses
→ unlock new choices, routes and progression
→ explore again
```

Depth and system interaction take priority over disconnected menus, currencies or filler systems.

## Overall fantasy identity

The current game concept is a **large open-world fantasy action RPG** rather than vanilla-plus, survival-first Minecraft or a menu-heavy MMORPG clone.

Its identity comes from:

- a huge authored continent split into distinct regions rather than procedural biome wandering;
- fast real-time combat with dodge, guard/parry, skills, stagger and bosses;
- five root classes with deep advancement and broad weapon freedom;
- exploration-led progression, dungeons, field bosses, discoveries, gathering and settlement services;
- a fully non-vanilla visible creature ecology and non-vanilla mounts;
- grounded fantasy in the early heartland that expands into mountains, ice, jungle, swamp, desert, fae/magical forests, volcanic terrain, ocean routes and anomaly/endgame zones;
- familiar readable RPG terms where invention would add no value, while region/creature/signature-item names become more specific and flavorful where identity matters.

Do not invent fantasy terminology just to rename universally understood concepts. `Gold`, `EXP`, `HP`, `Mana`, `Stamina`, weapon families and ordinary material names may remain straightforward. Distinctive naming effort should be spent on regions, factions, bosses, signature materials, class advancements, named gear and lore-bearing content.

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
- player/NPC robes, civilian clothing, profession apparel and cosmetic outfits;
- player locomotion/combat/work/mount animations including dash, dodge, roll, guard/parry and action transitions;
- shrines/checkpoints;
- camps, tents and campfires;
- inns, towns and service buildings;
- starting settlement;
- dungeons/ruins;
- mounts;
- resource nodes;
- important VFX/sound direction.

If an external design can legally be used directly and already looks better than a custom redesign, preserve it as intact as practical. Only alter what is required for this game's information, controls, GUI scale, Minecraft-scale rig/geometry compatibility and consistency.

Do not create temporary web-game-style panels, generic black translucent boxes, disposable vanilla-entity placeholders, recolored vanilla armor as finished RPG gear, code-only dash displacement with a vanilla running pose, or throwaway player-facing screens.

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
- Primary ordinary currency: **`Gold`**
- Player and enemy level display use the same format: `Lv <number>`
- Vanilla green XP/level presentation is not used for RPG progression.
- `Gold` is a server-authoritative numeric balance, not a physical item stack that consumes inventory space.

Material naming follows readability first:

- ordinary resources use clear names such as ore, timber, herbs, meat, hide and regional variants;
- creature/region/signature resources may use distinctive names such as a boss core, antler, scale, pollen or crystal tied to that source;
- internal labels such as `R01 Token`, `Tier-3 Material` or development-stage names are never player-facing;
- do not invent multiple pseudo-currencies when an item material or Gold already serves the loop.

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

The required-EXP curve stays the same, but ordinary reward output is intentionally much faster than the first benchmark—roughly **about three times the previous average acquisition rate** across mixed play.

These are ordinary mixed-play targets, not speedrun/grind-route guarantees:

| Lv span | Typical active time per Lv |
|---|---:|
| 1–10 | 4–6 min |
| 11–25 | 6–9 min |
| 26–45 | 9–13 min |
| 46–65 | 13–18 min |
| 66–80 | 17–24 min |

Pure leveling time is therefore much shorter than before. A player who actually explores, travels, manages gear, visits towns and completes varied content should typically approach Lv 80 after roughly **20–30 hours** of a substantial first playthrough. Completionist play can be much longer. The intended route must never require repetitive mob grinding just to stay on the region curve.

### EXP reward calibration

Let `X` be `EXP_to_next` for the receiving player's current Lv.

**Every percentage below means that the reward fills approximately that percentage of the receiving player's current next-level EXP requirement `X`. It is not a drop chance and not a percentage stat attached to the enemy.**

For content close to the player's Lv, use these faster starting targets:

| Reward source | Base EXP target |
|---|---:|
| ordinary common enemy | ~1.0% of `X` |
| elite | ~6% of `X` |
| miniboss | ~10% of `X` |
| field/world boss — first eligible defeat | ~20% of `X` |
| field/world boss — repeat | ~7% of `X` |
| regional contract / normal side quest | ~20–30% of `X` |
| main/regional milestone | ~35–45% of `X` |
| discovery / meaningful dynamic event | ~5–10% of `X` |
| dungeon first-clear completion | ~45–55% of `X`, plus boss reward |
| dungeon repeat completion | ~20–25% of `X`, plus repeat boss reward |

A normal first dungeon therefore usually contributes roughly **65–75% of one Lv** from completion + boss alone, and can reasonably produce about one Lv when its discovery/objective rewards are included. Repeat clears remain useful without becoming the sole best leveling loop.

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

Exact natural HP recovery, Recovery Belt, R01 recovery items, food and rest behavior are expanded in `RECOVERY_PRODUCTION_APPEARANCE.md`.

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
- baseline in-combat recovery access is deliberately bounded through the four-dose quick Recovery Belt defined in `RECOVERY_PRODUCTION_APPEARANCE.md` rather than letting a stack of 20 potions become one uninterrupted boss-healing pool.

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
- visible animation/motion must match actual evasion or i-frame timing;
- finished presentation uses an accepted external motion/animation source; code-only displacement with an unrelated vanilla pose is not acceptable.

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

Exact combat math/timing lives in `COMBAT_BALANCE.md`; class action kits live in `CLASS_COMBAT_KITS.md`.

---

# 8. Input policy

All project actions are rebindable.
The default frequent-action map, retired vanilla quick actions, Essential-safe collision policy, hold/toggle options and abstract action-ID requirements are **already locked** in `ACCESSIBILITY_DIFFICULTY_INPUT_AUDIO.md`.

- do not re-invent default keys during coding;
- low-value vanilla `Drop Item` / quick off-hand swap behavior follows the project control-profile rules there;
- future dependency updates that create a collision are adapter/rebind integration work, not permission to silently change the project gameplay layout;
- UI prompts always show the player's current binding rather than hard-coded key text.

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
- class switching costs Gold;
- cost scales with player `Lv` but has a cap;
- no class-switch cooldown;
- switching never erases learned history.

The player's first root-class choice in the starting settlement is free.
Later switches follow the normal paid switching rule.

Class-switch cost at player Lv `L`:

```text
SwitchCost(L) = round_to_10(min(2500, 50 + 15L + 0.4L²)) Gold
```

Reference anchors: Lv 8 = 200 Gold, Lv 20 = 510 Gold, Lv 40 = 1,290 Gold, Lv 60 = 2,390 Gold, and the cost caps at **2,500 Gold** from the late game onward.

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

Detailed root/branch skills and progression are locked in `CLASS_COMBAT_KITS.md` and `CLASS_PROGRESSION.md`.

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

Death penalty is **automatic**, not a player-choice menu.

Rules:

1. if the player has any EXP progress inside the current Lv, death removes a small amount of that current-Lv progress;
2. if there is **no removable current-Lv EXP**—including immediately after leveling or at the Lv 80 cap—death instead deducts Gold automatically;
3. Gold is allowed to become negative from the death penalty; there is no free-death exploit caused by having zero Gold;
4. an already-earned Lv can never be lost.

EXP penalty when current-Lv EXP is available:

```text
EXP_loss = min(current_Lv_EXP_progress, max(1, round(4% of EXP_to_next(current Lv))))
```

The target loss is therefore only about **4% of the current level requirement**. If the player has less progress than that, the remaining current-Lv progress is removed and no additional Gold is charged for that death.

Gold fallback at player Lv `L`:

```text
DeathGoldCost(L) = round_to_10(min(1200, 30 + 6L + 0.12L²)) Gold
```

Reference anchors: Lv 1 = 40 Gold, Lv 10 = 100 Gold, Lv 20 = 200 Gold, Lv 40 = 460 Gold, Lv 60 = 820 Gold, Lv 80 = 1,200 Gold.

Negative-Gold behavior:

- death can push Gold below zero with no hard lower floor at baseline;
- future Gold income first pays the negative balance naturally;
- purchases/services that require Gold remain unavailable until the player has enough non-negative spendable balance for that purchase;
- do not add a separate interest/debt system just because negative Gold is possible.

Additional rules:

- the opening approach before the first settlement shrine/checkpoint is activated has no economic death penalty;
- Gold is server-authoritative account/state data rather than a physical dropped stack, so banking/dropping items cannot bypass the penalty;
- boss/dungeon failure applies the same automatic rule at the nearest valid checkpoint/entrance; there is no additional equipment loss or mandatory corpse run;
- penalty-reduction items are not part of the baseline system unless later playtesting proves they add a useful choice rather than another consumable chore.

## Multiplayer down / revive

- lethal damage can enter a temporary downed state;
- teammates can revive the downed player within the rescue window;
- failed rescue leads to normal shrine/checkpoint respawn and the automatic death penalty above;
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

All visible armor/robes/apparel use accepted external 3D design/model families. Exact appearance/Wardrobe rules and the preferred 26.2 Armor Model API path are expanded in `RECOVERY_PRODUCTION_APPEARANCE.md`.

## Inventory capacity

There is **no inventory-weight system**.

The portable inventory uses one general backpack plus the existing nine-slot quickbar/hotbar concept. The backpack is intentionally larger than vanilla from the beginning, while materials and key items are separated so exploration does not become inventory housekeeping.

### General backpack capacity

| Stage | General backpack | Hotbar | Total ordinary carried slots |
|---|---:|---:|---:|
| start | 36 (`4 x 9`) | 9 | **45** |
| expansion I | 45 (`5 x 9`) | 9 | **54** |
| expansion II | 54 (`6 x 9`) | 9 | **63** |
| expansion III | 63 (`7 x 9`) | 9 | **72** |

Each expansion adds exactly one nine-slot row so the UI remains readable and the upgrade is immediately understandable.

Working upgrade economy:

- expansion I — about **1,200 Gold** plus a simple early-region bag/material component;
- expansion II — about **4,000 Gold** plus a midgame material component;
- expansion III — about **12,000 Gold** plus a high-tier material component.

These are service/crafting upgrades tied to world progression and available settlements, not hard `Lv X required` menu locks. Costs may be tuned after real item-density playtests, but the 45 → 54 → 63 → 72 total-slot progression is the current canonical target.

### Material Pouch

Crafting/gathering materials use a dedicated **Material Pouch** from the start.

- eligible gathered/crafting materials auto-route into the pouch on pickup;
- the pouch is a category/catalog keyed by material type rather than a small fixed grid that fills with different material IDs;
- pouch contents do not consume general-backpack slots;
- field capacity is **999 of each material type**;
- crafting, forge, alchemy, cooking and other valid service UIs may consume directly from the pouch without forcing manual withdrawal;
- players can manually withdraw/deposit for trade or organization where relevant;
- if a material reaches 999 in the field pouch, additional copies can enter the general backpack instead of being silently deleted;
- settlement bank/storage provides a Material Vault baseline of **9,999 per material type** and a `Deposit Materials` action, preserving a reason to return to town without constant sorting chores.

The pouch is for materials, not a hidden second general inventory. Equipment, normal consumables and arbitrary miscellaneous items cannot be stuffed into it.

### Key / quest items

Progression-critical keys, quest flags and equivalent non-tradeable progression items use a separate **Key Items** category/state and do not consume general inventory slots.
They cannot be accidentally sold, dropped or lost through death.

### Stack caps

Use category-specific caps rather than vanilla's universal-feeling 64 convention:

| Item category | Baseline stack cap |
|---|---:|
| equipment / individually rolled gear | 1 |
| potions / bombs / direct combat consumables | 20 |
| food / ordinary utility consumables | 50 |
| normal stackable loot / trade goods | 99 |
| crafting/gathering materials in Material Pouch | 999 per material |
| sandbox building/decor blocks where retained | 256 |
| Gold | numeric balance; no item stack |
| key/quest progression items | separate state; no backpack stack |

A later ammunition implementation may use a large dedicated reserve instead of consuming ordinary backpack slots; do not lock an ammo stack rule before the Hunter/firearm implementation is actually selected.

### Sorting / protection / overflow

The final inventory supports:

- one-action sort;
- category/filter/search tools when item count justifies them;
- favorite/locked slots or items that sorting and ordinary sell-all actions cannot move/sell;
- clear `new`/recent-loot feedback without permanent visual noise;
- comparison against currently equipped gear only when relevant.

Portable-container nesting cannot create infinite storage. Project backpacks/pouches cannot be placed inside equivalent portable storage recursively.

Important rewards are never silently deleted because the general backpack is full. Quest/key items bypass it; deterministic boss/dungeon/progression rewards remain claimable through their reward interaction until space exists or use a small non-storage overflow handoff. The overflow mechanism must not become a free second permanent backpack.

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

- **ordinary/common enemies do not drop equipment as routine loot**; their rewards focus on EXP, Gold, creature/material drops and selected consumables;
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

Use the grade structure and loot rules locked in `LOOT_ECONOMY.md` rather than re-inventing them during implementation.

---

# 14. Damage types and status effects

Use medium complexity.

Physical identity may distinguish slash / pierce / impact where useful.
Core magical/elemental families may include fire, frost, lightning, poison/corrosion and arcane-like effects.
Status effects must be mechanically distinct rather than differently colored copies of the same DOT.

Exact launch status/buildup relationships are defined in `STATUS_AND_R01_ENCOUNTERS.md`.

---

# 15. Workstations / professions / production

Major RPG production does not rely on the vanilla crafting grid.
Potential systems include forge/smithing, alchemy, cooking and justified enhancement/customization.

Use **light profession mastery** rather than giant mandatory profession grinds.
Smithing, alchemy and cooking may improve through use and unlock recipes/quality/options, but progression must not require repetitive mass-crafting for dozens of hours.

Each important workstation gets a purpose-built screen from the shared external UI language documented in `UI_DIRECTION.md`. Physical workstation/building appearance follows the same external-first rule.

Avoid production steps that only add clicks.

The current five-rank non-grindy mastery shape and R01 alchemy/cooking behavior are defined in `RECOVERY_PRODUCTION_APPEARANCE.md`; forge/reforge behavior is defined in `EQUIPMENT_BALANCE.md`.

---

# 16. Open world / settlements

Use a high-quality external open-world RPG map/terrain solution rather than hand-authoring the entire continent from zero.

**Azari 30k x 30k is the current primary terrain candidate and region planning proceeds against it.** Its world bytes remain local/private while usage terms are still being verified. If its import or terms fail, preserve the same gameplay/region principles on the strongest free fallback rather than reopening every system decision.

Target about **12 major regions**, adjustable to roughly 10–14 if the selected external map's geography strongly supports a different count.
Each region contains subregions/landmarks/POIs and defines environmental identity, danger/Lv profile, enemies, elites, signature threats, resources, gear identity, settlements, dungeons, camps/checkpoints, quests/events and traversal gimmicks.

## Enemy Lv model

Use **one suggested-entry Lv per major region plus local encounter Lv** rather than broad overlapping region ranges.

- each major region has one clear suggested-entry Lv indicating when an average build should first feel comfortable entering a major region;
- subregions, dangerous POIs, elites, dungeons and bosses can sit above or below that region entry recommendation;
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

## Starting settlement — Alderford

The game's first safe social/service hub is **Alderford**. Its player-facing name, functional identity and opening topology are locked in `R01_VERTICAL_SLICE.md`; do not reopen the name during implementation and do not substitute a generic vanilla village as the finished result.

Alderford is a real long-term hub, not a disposable tutorial town. Its buildings exist coherently from the beginning; progression primarily unlocks advanced services or access rather than making arbitrary buildings appear out of nowhere.

### Opening / first reveal

- start on an approach road or settlement outskirts rather than spawning directly in the central plaza;
- use a short roughly 2–4 minute playable approach/encounter to establish movement/combat/context without a long forced tutorial;
- reaching the gate/entrance gives the first clear reveal of Alderford and the safe hub;
- do not turn the opening into a chain of NPC errands before the player is allowed to explore.

The detailed first 55–75 minute opening route is locked in `R01_VERTICAL_SLICE.md`.

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

Exact Trail Stag and later mount rules live in `MOUNTS.md`.

### Housing

- Alderford homes may be inspected and purchased from the beginning;
- there is **no story, boss-clear or reputation permission gate for the right to buy the first home**;
- normal price/economy is the gate: starting funds do not trivially buy a home;
- the first normal starter home costs **2,400 Gold** at baseline;
- a starter furnishing/storage package costs **750 Gold** at baseline and remains separate from buying the shell;
- with the target R01 income curve, a savings-focused player can normally reach the first home in roughly **5–7 hours** without dedicated Gold grinding;
- a player owns **one active residence at a time** at baseline; later houses are upgrades/trade-ups rather than storage-multiplying extra properties;
- canonical vacant-house price anchors are **Small 2,400 / Town House 9,000 / Large 25,000 / Prestige 65,000+ Gold**;
- selling/trading up returns **80% of the old house's standard purchase value**;
- moving house must atomically preserve owned furniture, trophies and home-storage contents; a failed move cannot delete or duplicate them;
- Home Storage grows by residence tier and remains one logical personal pool rather than multiplying with every placed chest.

Exact housing-market, furnishing, storage and external-system direction lives in `FISHING_COLLECTION_HOUSING_MARKET.md`.

### Exits / open-world signal

Alderford should connect to at least three meaningful directions when the selected external map permits it:

1. a main road toward first-region core content;
2. a secondary route toward gathering/small POIs/exploration;
3. a visibly riskier route toward a higher-Lv area or dangerous encounter.

Use recommended-Lv danger rather than invisible walls to communicate that the world extends beyond the intended first path.

The village layout and buildings come from a coherent external high-quality village/build family or map/schematic.
Avoid stitching together unrelated building styles when a coherent pack/source exists.

## R01 — first-region content package

`R01` is an internal production identifier. Alderford is already locked as the starting settlement name; broader player-facing region naming/terrain placement follows the current region canon and actual Azari spatial closure rather than being invented during implementation.

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

The first region is not zombie/skeleton replacement spam. Common danger comes from understandable wildlife/creature behavior.
The production R01 threat roster and the correction from the earlier rattlesnake-style candidate to the external CC0 Meadow Viper are defined in `STATUS_AND_R01_ENCOUNTERS.md`.

Common threats provide EXP, Gold/materials and creature resources, **not routine equipment drops**.

### Elites / field boss

Use Threateningly Mobs Continued selectively rather than accepting its default world-wide spawn rules.
The exact Steelboar, Nature Spirit, Regalhart and Earthloong project-normalized encounter kits are defined in `STATUS_AND_R01_ENCOUNTERS.md`.

Their original mod stats/loot/spawn rates are reference inputs only. Project data owns Lv, HP/damage, stagger, rewards, respawn and placement.

### First dungeon

Working concept: **an abandoned quarry / root-overgrown underground complex** connecting the region's mining/resource loop to its fantasy ecology.

- target first-clear length: short-to-medium, roughly 15–25 minutes rather than a huge early maze;
- use an external high-quality dungeon/structure shell where terms permit;
- any imported vanilla mob spawners are removed/replaced;
- 2–3 meaningful combat spaces plus traversal/side-cache choices are preferable to many copy-pasted rooms;
- quarry layers first teach gathering/resource visuals, then transition into root/magic corruption deeper inside;
- **Earthloong** is the deepest-chamber boss and its exact phases/attacks are defined in `STATUS_AND_R01_ENCOUNTERS.md`;
- first-clear reward includes a deterministic meaningful equipment choice through the settlement/smith flow so bad RNG cannot leave a new player without useful progression;
- repeat clears focus on regional materials and a controlled chance at signature Earthloong-themed items rather than flooding the inventory with random gear.

DeCubed Dungeons is a current free 26.2 Fabric/datapack architecture candidate, but its vanilla spawners/loot are not adopted unchanged. Final shell selection happens during map integration/asset intake.

The room-by-room opening flow and shortcut expectations are defined in `R01_VERTICAL_SLICE.md`.

### First-region resources

Keep the first gathering loop simple and immediately connected to settlement services.
The actual R01 resource catalog is locked in `EQUIPMENT_BALANCE.md` and its current alchemy/cooking uses are expanded in `RECOVERY_PRODUCTION_APPEARANCE.md`.

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

The current concrete first-play route is `R01_VERTICAL_SLICE.md`.

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

All important buildings, NPC clothes, props and visible work animations are external-first designs/assets.

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

Exact launch mount roster/progression/speeds/handling live in `MOUNTS.md`.

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

# 20. Quests / narrative / world state

Use a mixed structure weighted toward exploration:

- main narrative for world/system introduction and major progression;
- regional quest chains that give each area identity;
- free exploration/discovery without constant quest-marker following.

Target overall feel is approximately **30% guided objectives / 70% free exploration and self-directed discovery**, not a rigid numerical quota.
The main story guides without turning the open world into a linear corridor.

Canonical protagonist/story structure:

- player name, appearance, class and moment-to-moment roleplay remain freely customizable;
- the shared background is deliberately light: an independent outsider/adventurer arriving into the continent's road/settlement network, not a prewritten chosen one, monarch or reincarnated hero;
- the main mystery concerns the ancient **Anchor network**, whose regional branches historically measured, routed, contained or stabilized selected large-scale processes without making the natural world itself artificial;
- modern regional societies adapted differently as Anchor infrastructure weakened, so restoration, release and regional partition each have real benefits and costs;
- R01 is the common opening, while later acts permit alternate evidence routes rather than forcing all 12 regions into a mandatory checklist;
- the launch finale resolves the immediate Central Anchor cascade first, then presents the player-personal **Restore / Release / Partition** decision;
- multiplayer players keep their own story choice/ending state rather than a host choosing the moral result for everyone;
- postgame preserves a stable explorable world and continues through bosses, dungeons, Mythics, hidden techniques, collection/fishing/housing and difficult regional content rather than rolling the save back before the ending.

The cross-region main route is locked as:

```text
R01 common opening
→ at least one of R02 / R03
→ any two major evidence packages from R04–R07
→ one of R08 / R09
→ R10 or the authored late-R11 investigation
→ R12 Central Anchor finale
→ personal Restore / Release / Partition choice
```

The regions not used as mandatory main-route evidence remain complete playable regions with their own progression, rewards, optional evidence and epilogue consequences. Completing eligible regional content before the main quest formally asks for it must be recognized from durable state rather than forcing a fake replay.

Quest/world-state ownership, objective credit, split-party behavior, dynamic events, dialogue and idempotent rewards are defined in `QUEST_WORLD_STATE.md`; the story premise/acts/recurring roles are defined in `WORLD_STORY_CANON.md`; the implementation-level cross-region route, sequence-break and rejoin behavior is defined in `MAIN_QUEST_SCENE_PACKAGE.md`.

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

## Resource naming

Use readable ordinary names for ordinary materials and save invented names for materials that deserve identity.

Examples of naming shape, not a locked R01 item list:

- ordinary: Iron Ore, Hardwood, River Herb, Louxia Meat, Tough Hide;
- source-linked: Regalhart Antler, Earthloong Core, Frost Crystal, Moon Pollen;
- avoid generic development language such as `Region Material A` or unnecessary extra currency tokens.

## Multiplayer ownership

Resource nodes use **personal gathering state** in multiplayer.
One player gathering a node does not deny the same node to another player simply because they arrived a moment later.

## Gathering tools

Use dedicated gathering tools such as pickaxe, axe, harvesting knife/sickle and fishing rod as appropriate.

- tools are project/RPG tools, not the core vanilla mining progression;
- routine tool durability is not used;
- tools do not occupy normal combat equipment slots during ordinary play;
- the Tool Pouch/context system automatically presents the appropriate accepted external tool at valid nodes;
- baseline Field tools are available from the start, with Refined and Masterwork permanent tiers providing bounded speed/access growth rather than consumable charges.

## Regeneration

- common nodes regenerate after a suitable active-world-time interval;
- rare nodes use longer regeneration and/or special conditions, events, dangerous locations or boss-linked access;
- rare resource design should create reasons to explore rather than simply wait on a short timer;
- personal respawn/save state is server-authoritative and relog/dimension changes do not refresh it.

Exact interaction times, mastery ranks, R01 node timers/yields and tool-tier rules are locked in `GATHERING_FISHING_CAMP_HOUSING.md`.

Node visuals/models/interactions are external-first when good assets/designs exist.
External node-system implementations should be studied instead of reinventing regeneration poorly.

---

# 22. Fishing / camps / housing / rest

Travel should have a sense of journey and temporary shelter, while fishing/housing remain optional-but-useful side loops rather than survival chores.

## Fishing

Fishing is a compact collection/economy/cooking activity rather than a long mandatory minigame.

- use custom/external visible fish, rod and fishing animations rather than vanilla-fish presentation;
- common catches resolve quickly; uncommon/rare/trophy catches may use a short readable tension interaction;
- no ordinary bait-per-fish tax at baseline;
- authored fishing spots/shoals provide regional species and controlled depletion/respawn instead of one reward table for every puddle;
- Fish Codex tracks discovered species, useful source/location information and personal records/trophy catches;
- fish can feed selling, cooking, collections, contracts and home trophy/display loops without becoming another currency;
- narrow weather/time catches remain optional collection advantages, not progression gates.

Exact fishing timings, mastery and spot rules live in `GATHERING_FISHING_CAMP_HOUSING.md`; collection/records/cooking/market/UI direction lives in `FISHING_COLLECTION_HOUSING_MARKET.md`.

## Camps

Camps are **quick-deploy reusable field infrastructure**.

- the player crafts/acquires one reusable **Field Camp Kit** rather than paying a material cost every placement;
- deployment is rapid and placement is server-validated against combat, terrain, settlement/dungeon/boss/protected-route restrictions;
- at baseline one active camp exists per owner; successful redeploy packs/replaces the previous camp;
- camps provide full HP/Mana/Stamina rest, Recovery Belt reload from carried reserve, cooking and out-of-combat inventory/equipment management;
- camps do **not** baseline provide fast travel, a death checkpoint, Material Vault/bank, forge, full alchemy lab, merchant or class advancement;
- nearby players may use appropriate rest/cooking functions while ownership/state remains authoritative;
- random routine camp destruction and repair chores are not part of the baseline.

Exact placement dimensions/rules, persistence and external camp candidates live in `GATHERING_FISHING_CAMP_HOUSING.md`.

## Housing

Permanent player housing is separate from camps and uses actual vacant settlement properties.

- towns/settlements contain external-first empty/purchasable house shells in multiple sizes;
- the right to buy the first available home exists from the beginning rather than being unlocked by story/boss progression;
- a player owns one active residence at a time at baseline and **trades up** rather than accumulating houses for multiplied storage;
- canonical price anchors are Small 2,400 / Town House 9,000 / Large 25,000 / Prestige 65,000+ Gold;
- selling the old home returns 80% of its standard purchase value;
- furniture, trophies and logical Home Storage migrate safely/atomically during a move;
- house functions include rest, Home Storage, furnishing/decor, wardrobe access where suitable, cooking after appropriate furnishing and trophy/collection display;
- placing extra chests/furniture does not multiply logical storage pools;
- the house does not replace town forge/full alchemy/class/merchant/shrine services;
- exterior/structural shell is protected at baseline; player customization focuses on interior furniture, accepted variants and trophy display rather than unrestricted demolition of roads/walls/neighbors.

Exact housing market, storage tiers, furnishing and external 26.2 furniture/property-system candidates live in `FISHING_COLLECTION_HOUSING_MARKET.md`.

---

# 23. Multiplayer / Essential

Essential-friendly multiplayer is a major usability goal.
Connection convenience does not own game authority.

Server-authoritative state includes damage, item ownership, Gold, EXP/Lv, skill cost/success, class/progression, quests, world state and saves.

Combat/loot rewards are personal per player.
Resource gathering is also personal per player at the node-availability level.

Players can travel together or pursue separate exploration/content and regroup later. Ordinary progression must not require constant party proximity.

Formal party UX, participation eligibility, per-player reward ownership, co-op encounter scaling, reconnect behavior and exploit cases are closed in `PARTY_MULTIPLAYER.md`.

Do not claim multiplayer quality until actually tested with real clients.

---

# 24. Economy and merchants

## Gold

Use **Gold** as the one primary numeric currency for the ordinary economy.

- player-facing name is simply `Gold` unless later lore work discovers a genuinely better reason to change it;
- do not use copper/silver/gold denomination conversion at baseline;
- Gold is a numeric account/state value and does not occupy inventory;
- add special currencies/tokens only when a specific activity genuinely needs a distinct reward loop;
- do not multiply currencies merely to make the game look larger.

Gold supports at least class switching, automatic death fallback, merchants, housing and selected services.
Starting liquid currency target is roughly **150 Gold**: enough for basic supplies and a mistake, nowhere near enough to trivialize the first house.

## Economy feel / income curve

Early and midgame should feel **slightly money-constrained but not grind-starved**: the player usually has several attractive uses for Gold and must choose priorities, while normal play still funds essential recovery and progression. Later progression becomes more financially comfortable rather than maintaining artificial scarcity forever.

Target ordinary gross and routine-spend-adjusted net income:

| Lv span | Gross Gold / active hour | Typical net after routine consumables/services |
|---|---:|---:|
| 1–9 | 500–700 | 350–500 |
| 10–24 | 900–1,400 | 600–900 |
| 25–44 | 2,000–3,200 | 1,200–2,000 |
| 45–64 | 4,000–6,500 | 2,500–4,500 |
| 65–80 | 7,000–11,000 | 4,500–8,000 |

Routine unavoidable spending should normally remain below roughly one third of gross income. Larger optional purchases create the meaningful tradeoffs; essential recovery should not force a Gold grind.

### Primary Gold sources

Tune first-play gross income roughly around these roles rather than a rigid exact split:

- quests/contracts and first-clear objective payments — largest reliable source;
- normal combat — small steady Gold, with elites/bosses paying noticeably more;
- dungeon clears/chests and world events — major burst income;
- selling unwanted equipment and genuine material surplus — useful secondary source, not the dominant optimal loop;
- selected gathering/crafting orders or regional trade opportunities — bounded supplemental income.

Do not make repetitive slaughter of the easiest common mob, relogging a chest or merchant arbitrage the best Gold strategy.

### Primary sinks

Core sinks are:

- potions, food and selected consumables;
- class switching and any future justified respec service;
- merchant equipment/special rotating stock;
- forge/alchemy/crafting service fees and materials;
- housing, furnishing, trophy/display and storage expansion where appropriate;
- later mounts/stable and selected travel/service purchases;
- automatic Gold death fallback when current-Lv EXP cannot be removed.

Fast travel does not need a routine tax merely to delete Gold. Add a travel fee only if later playtesting proves it creates an actual route/economy choice rather than friction.

## Merchant stock / refresh

Merchant inventory is **mostly randomized/rotating**, but essential items and explicitly designated goods remain fixed.

Fixed/reliably available stock includes:

- core potions/recovery necessities;
- required basic supplies;
- explicitly progression-safe goods that should not disappear because of RNG.

Rotating equipment, regional goods, special materials and rare finds refresh every **10 minutes of active server/world playtime** by default.

Refresh rules:

- sleeping, reopening the UI, relogging or changing the Minecraft clock does not force a reroll;
- stock generation is deterministic from stable world/merchant identity plus refresh-cycle state so simple save/reload abuse does not reroll it;
- the server owns the current catalog and cycle timing;
- ordinary rotating purchase availability is personal per player in multiplayer so one player cannot empty another player's normal shopping opportunity;
- intentionally unique world-event lots may be shared only when scarcity itself is the authored mechanic.

Do not add separate refresh timers for every merchant tier at baseline. Keep one readable 10-minute cadence until real playtesting proves a second cadence materially improves the economy.

## Merchant pricing structure

Price items by their own tier/identity and regional economy, not by silently scaling the exact same item to the buyer's Lv.

Target affordability relative to same-tier gross income:

- basic recovery/consumable: roughly 1–3 minutes of income;
- ordinary current-tier gear purchase or clear upgrade: roughly 10–20 minutes;
- strong rotating higher-grade gear: roughly 30–60 minutes;
- rare/signature merchant item: roughly 1.5–3 hours and therefore a deliberate savings target.

R01 starting anchors before the later loot-economy pass:

- food/basic utility consumable: ~10–25 Gold;
- basic potion: ~25–40 Gold;
- ordinary starter weapon/armor shop item: ~120–250 Gold;
- strong R01 rotating gear: ~350–600 Gold;
- rare/signature R01 merchant item: ~900–1,500 Gold.

Sell-back baselines:

- equipment: **25%** of standard buy value;
- materials: **35%**;
- consumables: **20%**.

Specific authored trade goods may override these values, but buy/sell tables must never permit deterministic merchant-to-merchant arbitrage.

Meaningful reputation may alter prices by at most roughly **±10–15%** unless a rare authored faction rule explicitly justifies more. Reputation is not allowed to turn the ordinary economy into a mandatory grind.

Housing follows the fixed trade-up market in §16/§22: 2,400 / 9,000 / 25,000 / 65,000+ Gold house anchors with 80% old-house resale. Class switching and death-cost formulas are defined in their own canonical sections so merchant tuning cannot silently change them.

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
- quests/dialogue;
- recovery items/food recipes/profession mastery;
- armor/apparel appearance bindings and NPC outfit role data.

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
- inventory is a purpose-built RPG character screen with the canonical 12 slots, 45 starting ordinary carried slots, expandable to 72, Material Pouch, Key Items and contextual comparison—not a vanilla inventory reskin;
- forge, alchemy, cooking, class/advancement, map, death/respawn, Wardrobe/Appearance and other important screens reuse the same component/spacing grammar;
- fishing/Fish Codex, housing/property/furnishing and quest-journal screens must use the same external visual grammar rather than becoming unrelated minigame UIs;
- no generic black translucent panel phase, no unrelated UI-pack collage, no temporary vanilla buttons;
- real Minecraft-client screenshot review at multiple GUI scales/resolutions is required before a screen is visually accepted.

Accessibility, World Challenge, personal assists, final frequent-action defaults, subtitles/captions, non-audio cues, camera/VFX comfort and dynamic audio behavior are closed in `ACCESSIBILITY_DIFFICULTY_INPUT_AUDIO.md`. Exact audio files and real-client usability remain external-asset/validation work, not open UI design decisions.

Backend/UI-library candidates are not visual canon. Current candidates include a current 26.2 Fabric accessory backend such as Trinkets Updated where it fits; older RPG Inventory architecture remains reference/code material unless 26.2 compatibility is established.

---

# 27. Current locked decisions

Major locked decisions as of **2026-09-17**:

- private-use large open-world fantasy action RPG with very low vanilla progression dependence;
- core identity is exploration + fast action combat + character/build growth across a large authored regional world rather than vanilla survival or an MMO chore list;
- protagonist is customizable in name/appearance/class with only a light shared outsider/adventurer starting frame; no strongly fixed chosen-one identity;
- main story centers on the ancient Anchor network and three legitimate long-term stewardship directions rather than one obvious good/evil answer;
- cross-region main route is R01 → at least one of R02/R03 → any two of R04–R07 → R08 or R09 → R10 or late-R11 investigation → R12, with already-completed eligible regional evidence recognized rather than replay-forced;
- launch ending choice is personal **Restore / Release / Partition** after the immediate Central Anchor crisis is contained; multiplayer players may choose independently and postgame remains explorable;
- ordinary universal terms stay readable; primary currency is **Gold**, while distinctive naming effort goes to regions/factions/bosses/signature materials/gear;
- Fabric is the locked mod loader for this project unless a future hard technical blocker forces a deliberate migration review;
- Azari 30k x 30k is the primary free terrain candidate and region planning proceeds against it while local-use/import terms are verified;
- external-first visuals/assets from the first visible/test implementation;
- player/NPC armor, robes, clothing and profession outfits use accepted external 3D model/design families rather than recolored vanilla armor/flat-skin substitutes;
- player locomotion/combat/work/mount motions including dash/dodge/roll are external-first and must match actual server movement/hit/i-frame timing;
- UI visual language is the free CC0 Foozle Lucifer RPG UI + Lucifer Equipment family, with Kenney Fantasy UI Borders/Adventure only as supporting scalable/control primitives; details live in `UI_DIRECTION.md`;
- World Challenge, personal assists, frequent-action input defaults, subtitles/captions, non-audio combat information, camera/VFX comfort and music/audio state behavior are closed at design level in `ACCESSIBILITY_DIFFICULTY_INPUT_AUDIO.md`;
- R11 aquatic action compatibility is closed at design level in `R11_AQUATIC_ACTION_MATRIX.md`; runtime retarget/render/playtest remains validation work rather than a second underwater design pass;
- no temporary player-facing design;
- dead/superseded/duplicate code removed after safe replacement;
- `EXP` / `Lv` notation and removal of vanilla XP progression/drop loop;
- launch combat-Lv cap is Lv 80 with a smooth quadratic EXP curve, no launch prestige/overflow-Lv treadmill;
- average mixed-play EXP acquisition is retuned to roughly three times the first benchmark, targeting about 20–30 hours of substantial normal play to approach Lv 80 rather than 55–70 hours;
- EXP reward percentages mean the approximate share of the receiving player's current next-Lv requirement filled by that reward;
- current R01–R12 suggested-entry values remain unchanged after the EXP benchmark; R12 begins at Lv 72 and leaves endgame headroom before the Lv 80 cap;
- ordinary combat is only a minority of total EXP; quests, dungeons, elites/bosses and exploration rewards keep leveling tied to the whole game loop rather than mob grinding;
- **no vanilla mobs as normal world population**: hostile mobs, animals/livestock, aquatic mobs and ordinary villager/golem population are replaced by the custom/external ecosystem and NPC roster;
- creature-derived food/materials come from non-vanilla wildlife/livestock equivalents; imported vanilla spawners are replaced;
- project region/spawn rules own normal ecology; ordinary creatures may use natural spawning while elites/bosses/events use authored encounter logic;
- HP + Mana + Stamina; Stamina is primarily non-skill action resource; basic attack costs no Stamina;
- natural HP recovery is deliberately slow; combat recovery is bounded by a four-dose quick Recovery Belt loaded from real consumable reserves;
- baseline Healing Potion restores 35% MaxHP; Focus Draught and Cleansing Tonic provide bounded Mana/cleanse alternatives and all use a shared recovery lockout;
- food is one-at-a-time 20-minute Nourishment preparation, never a hunger-management requirement;
- dodge, guard, parry and layered stagger/poise combat;
- exact combat formulas/TTK/defensive windows are locked in `COMBAT_BALANCE.md`;
- 4 active skills + 1 high-impact hybrid-charge ultimate;
- VIT / END / STR / DEX / INT / WIL primary stats;
- five root classes: 전사 / 사냥꾼 / 성직자 / 마도사 / 수호자;
- root class mechanics, starting skills, first specializations and deeper Rank-50 class progression/passive economies are already specified in `CLASS_COMBAT_KITS.md` / `CLASS_PROGRESSION.md`;
- deep ~5-stage initial advancement target, one major playstyle-changing branch, no `final` terminology;
- first major specialization directions are 전사 공세/무기숙련, 사냥꾼 레인저/명사수, 성직자 성인/심판관, 마도사 원소술사/비전술사, 수호자 보루/파수꾼;
- major advancements must change mechanics/skills/passives/ultimate identity rather than only numeric stats;
- class/skill production is external-first: reuse high-quality current external skills/dependencies or legally reusable code when they fit instead of rebuilding weaker copies;
- persistent per-class progression with paid Lv-scaled/capped switching and no switch cooldown; first class free; switch cost caps at 2,500 Gold;
- all unlocked passives of the active class apply; inactive-class passives never leak across;
- broad weapon freedom: classes create natural weapon synergy rather than ordinary hard weapon locks; Hunter-compatible black-powder firearms remain allowed and modern firearms excluded from baseline;
- 12-slot RPG equipment target and no routine durability chore;
- equipment Item-Lv/base curves, five grades, affix ranges, forge/reforge rules and boss-signature bad-luck protection are already defined in loot/equipment subordinate canon;
- starting general backpack is 36 slots plus 9 hotbar slots = 45 ordinary carried slots; three 9-slot expansions raise the total to 54 / 63 / 72;
- Material Pouch auto-routes eligible materials, stores up to 999 of each material type without consuming general slots, and town Material Vault stores 9,999 per material type;
- Key Items do not consume ordinary backpack capacity; category stack caps replace blanket vanilla 64 behavior;
- sorting/favorite-lock/search behavior is part of the inventory target and portable-storage recursion cannot create infinite nested storage;
- lightweight Wardrobe unlocks legitimately acquired visual appearances, provides free Hide Helmet, changes no stats and restricts weapon overrides to compatible family/reach/handedness;
- ordinary enemies do not routinely drop equipment; equipment farming centers on elites/bosses/dungeons/authored rewards/merchants/crafting;
- medium damage/status complexity with direct conditions plus bounded Poison/Bleed/Frostbite/Shock buildup and repeat resistance;
- five-rank light smithing/alchemy/cooking mastery is non-grindy and advances through distinct recipe/order/technique experiences rather than mass-crafting one cheap recipe;
- roughly 12 major regions with **one suggested-entry Lv plus local encounter Lv**, peer regions allowed at equal difficulty, no universal scaling, no level-gate walls;
- R02–R12 each have content/mechanics-closed regional packages tying world problem/NPC roles/traversal/POIs/ecology/resources/dungeon/boss/reward/story evidence together rather than leaving them as biome lists; exact gated presentation/spatial placement still follows `PROJECT.md` pre-code gates;
- R01–R12 are governed by `REGION_CROSS_AUDIT.md` anti-repetition rules so later implementation must vary evidence delivery, dungeon grammar, settlement identity, signature-encounter discovery and resource/reward mechanics rather than merely reskinning one regional formula;
- discovered POIs and shrine/major-hub fast travel;
- starting region suggested entry Lv 1, with local early encounter pressure rising roughly through Lv 8 rather than treating 1–8 as a broad region recommendation band;
- **Alderford** is the final player-facing starting settlement name and its opening topology/named cast/service order are locked in `R01_VERTICAL_SLICE.md`;
- R01 has concrete Louxia/Meadow Viper/Cave Centipede/Bison/Grizzly/Steelboar/Nature Spirit/Regalhart/Earthloong combat/ecology roles and first-dungeon boss numbers;
- starting region is approachable while elites/POIs/bosses provide the first major difficulty spikes;
- high-Lv regions remain physically enterable rather than being blocked by invisible/story walls; R12 exploration can be entered early while only the deep Central Anchor finale requires main-investigation state;
- ground mounts use non-vanilla visible creatures/models; Trail Stag arrives early and later Komodo/Elephant/Laviathan/Sky Drake roles are specified in `MOUNTS.md`;
- mixed main/regional/free-exploration quests with roughly 30/70 guided-vs-free-exploration feel, dynamic region events, replayable dungeons, respawning field/world bosses;
- probabilistic drops with deterministic protection for progression-critical items;
- multiplayer down/revive and fully personal loot;
- quest/main-story progress and important reward claims are personal/server-authoritative unless a physical world fact genuinely requires one shared truth;
- death automatically removes a small amount of current-Lv EXP; if no current-Lv EXP can be removed, it automatically charges Lv-scaled Gold, and death can drive Gold negative;
- earned Lv never decreases; opening pre-shrine deaths are free and there is no extra corpse/equipment-loss layer;
- multiplayer allows players to explore together or separately and regroup without ordinary progression requiring party proximity;
- RPG field resource nodes instead of cave/strip-mining as the core gathering loop;
- personal node gathering state in multiplayer;
- mining/herbalism/forestry/fishing-foraging categories with five-rank light mastery;
- dedicated Field/Refined/Masterwork no-routine-durability gathering tools in Tool Pouch/context use, separate from combat slots;
- fishing is an optional collection/cooking/selling/record/trophy loop with external fish/rod/animation/Codex UI and short rare/trophy tension play rather than a long universal minigame;
- economy is slightly constrained in early/midgame and becomes more comfortable later; ordinary routine spending should not consume most income;
- merchants use fixed essentials plus rotating stock on a **10-minute active-world-time cadence**; reopen/relog/sleep cannot reroll them;
- baseline sell-back ratios are 25% equipment / 35% materials / 20% consumables and deterministic arbitrage is forbidden;
- camps use a **reusable Field Camp Kit**; ordinary placement does not consume materials and camps do not replace fast travel/town services;
- permanent houses are fixed vacant settlement properties with **one active residence at a time**, 2,400 / 9,000 / 25,000 / 65,000+ Gold trade-up tiers, 80% resale and safe furniture/storage migration;
- first-home purchase permission exists from the beginning; the starter house baseline is 2,400 Gold and functions as roughly a 5–7 hour savings goal rather than a story gate;
- starting settlement begins with a short approach/reveal, then immediate shrine/inn/basic merchant/bank/guild/basic-smith access;
- starting settlement uses a coherent external village/prop/NPC clothing family and a visually taught gate → square/inn → guild → smith → board/exits flow rather than mandatory NPC errand chains;
- the first 55–75 minute R01 route, first Trail Stag event, Regalhart clue discovery and quarry room sequence are already defined in `R01_VERTICAL_SLICE.md`;
- forge/alchemy/cooking deeper functionality is introduced through early gathering so exploration → gathering → return → production forms an immediate loop;
- starting quest density is intentionally low: about 1 main objective plus 2–3 regional contracts before discoveries add more;
- the stable is visible immediately but the first non-vanilla ground mount arrives through early first-region progression;
- starting settlement should expose multiple routes including an intentionally dangerous higher-Lv direction when geography supports it;
- selected faction/reputation systems only where meaningful;
- moderate day/night/weather gameplay effects;
- starting settlement and all important buildings use coherent external architecture/designs;
- Essential-friendly server-authoritative multiplayer target;
- Terradragon is R12 optional altar-summoned world-boss spectacle, **not** the main-story final guardian; the Central Anchor finale keeps a separate external-model/anatomy asset gate.

---

# 28. Next design queue

Do not re-decide the locked systems above. Continue from here without asking the user to reselect details that can be solved through research, external assets or normal balance work.

Completed/advanced design work that should **not** be restarted from zero:

- broad R01–R12 region graph plus content/mechanics-closed R02–R12 regional packages;
- world/protagonist/Anchor story spine, regional evidence structure, recurring-role framework and Restore/Release/Partition endings;
- cross-region main-quest scene/rejoin route in `MAIN_QUEST_SCENE_PACKAGE.md`;
- quest/world-state/multiplayer progression authority and formal party/reward rules;
- gathering tools/mastery/node loop, fishing loop/Fish Codex direction, reusable camp and one-residence housing market;
- R01–R12 cross-region anti-repetition/density/settlement/dungeon/evidence audit and its later regional refinements;
- regional creature/ecology sourcing and no-vanilla spawn architecture;
- external UI family selection and screen-language direction;
- World Challenge, accessibility/assist behavior, frequent-action input defaults, subtitles/non-audio cues, camera/VFX comfort and dynamic audio/music behavior;
- R11 aquatic action compatibility and swim-base combat/cast/guard adaptation matrix;
- global Lv 80 curve and fast ~3x reward benchmark with no-rescale confirmation for current region entry levels;
- Gold economy, class-switch cost, automatic death penalty, housing trade-up anchors and 10-minute merchant refresh/pricing structure;
- inventory capacity, Material Pouch/Vault, Key Items and stack-cap rules;
- loot economy, five grades, affixes, targeted boss farming, signature-material bad-luck protection and R01 equipment/resource catalog;
- exact equipment stat/affix/forge/reforge rules;
- non-vanilla mount roster/progression/balance;
- M0 Fabric 26.2 dependency/toolchain audit;
- combat formulas, TTK, dodge/guard/parry, poise and multiplayer boss scaling;
- all five root combat kits, first ten specializations and Rank-50 class/passive advancement structure;
- launch status/buildup rules and complete R01 combat roster including Regalhart/Earthloong;
- R01 opening vertical slice from approach road through first Trail Stag and Earthloong clear;
- external-first player motion rule including dash/dodge/roll/work/mount actions;
- four-dose Recovery Belt, baseline R01 potions, food, alchemy/cooking and five-rank light profession mastery;
- external-first armor/apparel/NPC-clothing pipeline, Armor Model API direction and lightweight Wardrobe rules;
- R01 asset intake evidence through Phase-B Pass 4, including Wizard River Scholar, Knight Ironbound and Kenney Trail Skewers exact candidate families while preserving unresolved acceptance gates;
- finished-game quality model and current design-completeness audit criteria.

The remaining gameplay-source-bootstrap gates are exactly the current project-level gates in `PROJECT.md`:

1. **Exact external asset binding / provenance** — finish R01 acceptance and expand to production-critical creature/boss/NPC/outfit/item/structure/Anchor machinery/VFX/animation/SFX/BGM sources, including acquisition/license/hash and required 3D/Minecraft acceptance.
2. **Azari spatial closure** — bind settlements, roads, route joins, shrines, POIs, dungeon entrances/exits, boss arenas, landmarks/sightlines and travel/content-density targets to actual coordinates; compress/reroute empty scale instead of filling it with copy-paste.
3. **Asset-gated boss / final-guardian closure** — after accepted models exist, lock exact player-facing identities, anatomy-supported attacks, weak points, signature materials and presentation for still-gated regional bosses and the R12 systemic final guardian.
4. **Final stale-document / hidden-choice audit** — remove obsolete alternatives and any remaining gameplay-affecting `decide during coding` choice, then run the pre-bootstrap acceptance check once.

**Final player-facing title selection is not one of these gameplay-source-bootstrap gates.** `BRANDING.md` owns the later title lock, while logo/font/graphic bytes remain part of the ordinary external-presentation/provenance gate.

Recommended next batches therefore are:

1. **External asset intake completion, beginning with R01** — perform acquisition/hash + actual 3D review for the already narrowed candidates rather than restarting broad scouting; continue only genuinely unresolved motion/model slots, then potion/weapon/VFX/audio acceptance.
2. **Azari spatial closure** — import/audit the real terrain and author the coordinate/sightline/travel-density package. Use the existing POI cadence targets rather than arbitrary icon quotas.
3. **Asset-gated boss/final guardian completion** — bind boss anatomy and mechanics to accepted visuals instead of designing attacks the model cannot express.
4. **Final canon/stale audit and source-bootstrap decision** — once gates 1–3 are materially closed, perform one final hidden-choice/stale-text pass and only then create the gameplay source/resource/data skeleton.

Exact music/SFX file selection remains part of the external-asset gate; the audio **behavior** contract is already closed. R11 animation retarget quality remains implementation/visual-validation work; the aquatic **design** contract is already closed. Do not reopen either as generic design work.

When design direction becomes unclear, research real open-world RPGs, open-source RPGs and large Minecraft RPG mods before inventing filler systems.