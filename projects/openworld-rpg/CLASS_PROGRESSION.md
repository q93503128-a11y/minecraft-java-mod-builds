# Open-World RPG — Class Progression / Advancement / Passive Tree Canon

> Status: **DESIGN CANON — class rank, specialization depth, passive economy and world-skill progression locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Root combat kits: `CLASS_COMBAT_KITS.md`  
> Combat math: `COMBAT_BALANCE.md`  
> Runtime boundaries: `M0_DEPENDENCY_AUDIT.md`  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins.

This document closes the remaining class-growth decisions that should not be improvised during coding. It deliberately favors **mechanical / strategic / access growth** over a giant passive-grid treadmill. Class progression must make the player able to do new things, not merely accumulate another layer of percentage bonuses.

---

# 1. External progression precedents

The numbers and structure below were chosen after comparing current proven RPG progression systems rather than inventing an arbitrary tree.

## Guild Wars 2

Useful precedent:

- profession specializations alter or expand the profession mechanic rather than only adding raw stats;
- a specialization has a small readable set of trait choices rather than a giant always-on web;
- Hero Points come from both leveling and world Hero Challenges;
- the world contains more challenge points than are strictly needed, so progression does not require 100% map checklist completion.

Adopted:

- class growth combines active play with world challenges;
- major advancement alters mechanics / skill families / passives / ultimate behavior;
- world progression offers **choice of which challenges** supply bonus passive points.

Not adopted:

- no expansion-style level-80 wall before the first meaningful specialization;
- no mandatory completion of every class challenge.

## Last Epoch

Useful precedent:

- passive points come from level progression and selected quests;
- Mastery changes the base class deeply;
- specialized skills have their own focused advancement rather than every modifier existing in one giant global tree;
- newly specialized skills have catch-up mechanisms so experimentation does not mean starting completely from zero.

Adopted:

- late class switching receives bounded catch-up;
- passive points are limited enough that builds must choose;
- deeper branch milestones alter a focused set of mechanics instead of adding dozens of unrelated buttons.

Not adopted:

- no separate 20-point tree for every single active skill at launch; `4 actives + 1 ultimate` already creates enough combinations with branch passives, gear and discoveries.

## Grim Dawn

Useful precedent:

- investment in mastery progression gates access to deeper skills;
- the player chooses between immediate skill power and deeper mastery access.

Adopted in simplified form:

- branch passive tiers require previous investment;
- important progression milestones require actual class use, not only player combat Lv.

Not adopted:

- no dual-class combination layer at launch; root-class switching and deep branches already fill that role.

## Minecraft RPG Series Skill Tree

Current 26.2 releases demonstrate that 100+ class nodes are technically possible. That scale is **not adopted** for this project.

Reason:

- the project already has player Lv, six attributes, 12 equipment slots, affixes, root class mechanics, active skills, branch mechanics and exploration unlocks;
- adding 100+ tiny nodes per class would increase menu labor faster than strategic depth.

Launch target is therefore a much smaller tree with high-impact nodes.

---

# 2. Class Rank

Every root class stores an independent `Class Rank`.

```text
Class Rank: 1..50
```

- first selecting an untrained root class starts that class at Rank 1;
- switching away preserves Rank, Class XP, specialization unlocks, passive allocations, world skills and completed class challenges;
- switching back restores them;
- only the currently active root class receives new Class XP;
- Class Rank does **not** replace global combat Lv;
- Class Rank does not automatically grant large hidden attack/HP multipliers; its power comes primarily from mechanics, skills and passive choices.

Launch cap is **Rank 50** per root class. Rank 50 is a cap for this release, not a player-facing `final class` label and not a promise that future content can never extend the structure.

## 2.1 Class-XP curve

For current Class Rank `R`, where `1 <= R <= 49`:

```text
ClassXP_to_next(R) = round_to_10(100 + 20R + 2R²)
```

Reference:

| Rank | XP to next |
|---:|---:|
| 1 | 120 |
| 5 | 250 |
| 10 | 500 |
| 20 | 1,300 |
| 32 | 2,790 |
| 44 | 4,850 |
| 49 | 5,880 |

Cumulative Rank 1 → 50 is approximately **110,250 Class XP**.

The visible numbers rise smoothly, but actual pacing is calibrated from percentages of the active rank requirement rather than from arbitrary enemy flat XP.

## 2.2 Target pacing

For a normally played first class:

| Rank span | Target active time / rank |
|---|---:|
| 1–10 | 5–7 min |
| 11–20 | 7–10 min |
| 21–32 | 9–12 min |
| 33–44 | 12–16 min |
| 45–50 | 16–20 min |

A focused first class should normally reach deep Rank-50 mastery in roughly **9–12 active hours**, long before every open-world objective is exhausted.

This intentionally lets a normal 20–30 hour first playthrough fully develop one class and substantially train another rather than forcing the entire playthrough into one permanent choice.

---

# 3. Class-XP sources

Let `C` be `ClassXP_to_next(current Class Rank)`.

For appropriate-level content, baseline Class-XP targets are:

| Source | Class-XP target |
|---|---:|
| ordinary enemy | ~0.8% of `C` |
| elite | ~5% |
| miniboss | ~8% |
| field/world boss first eligible defeat | ~15% |
| field/world boss repeat | ~6% |
| regional contract / normal side quest | ~15–20% |
| major regional/main milestone | ~25–30% |
| meaningful discovery / event | ~5–8% |
| dungeon first-clear completion | ~30–35% + boss |
| dungeon repeat completion | ~15% + boss |

Rules:

- combat Class XP uses the same encounter-Lv anti-farm modifier as combat EXP;
- quest/dungeon Class XP uses the same softer overlevel modifier as quest/dungeon EXP;
- idle time, merchant transactions, repeated UI actions and mass-crafting do not generate Class XP;
- support classes receive full encounter participation credit from effective healing, barriers, control and protection rather than needing to last-hit;
- Class XP is server-authoritative.

## 3.1 Reward ownership / class-swap exploit prevention

- combat/event reward goes to the class active during that eligible contribution;
- dungeon completion goes to the class used for the majority of tracked eligible combat/objective participation in that run;
- quest completion uses the class responsible for the majority of tracked objective progress when such progress exists; otherwise the class active at completion receives it;
- simply switching class immediately before turning in a reward must not become the optimal power-level method.

---

# 4. Late-class catch-up

The game encourages trying multiple classes, so a Lv 60 player who starts a second class should not require another full 9–12 hours merely to reach the competence expected of that part of the world.

Expected class rank for catch-up only:

```text
ExpectedClassRank(L) =
  min(50, 1 + floor(49 * min(L - 1, 59) / 59))
```

So the benchmark reaches Rank 50 around player Lv 60.

Let:

```text
Gap = ExpectedClassRank(playerLv) - activeClassRank
```

Catch-up multiplier applied **after** anti-farm / content-level modifiers:

| Gap | Class-XP multiplier |
|---:|---:|
| <= 4 | 1.00x |
| 5–9 | 1.30x |
| 10–19 | 1.75x |
| 20+ | **2.25x** |

Rules:

- catch-up never increases ordinary combat Lv EXP or Gold;
- trivial low-Lv content remains heavily reduced before catch-up is applied;
- catch-up disappears naturally as the class approaches the expected rank;
- no paid catch-up consumable or separate boost currency at baseline.

---

# 5. Major advancement cadence

The launch class line has five meaningful advancement beats:

```text
Rank 1  — Root class identity
Rank 10 — First specialization branch
Rank 20 — Branch technique advancement
Rank 32 — Branch doctrine advancement
Rank 44 — Branch ascendant advancement
Rank 50 — Deep Mastery challenge / passive completion opportunity
```

`Rank 50` is not called `Final Advancement` in player-facing text.

Class Rank may continue accumulating even if a player delays a milestone trial. The rank is not frozen, but the associated mechanic/skill/passive tier remains locked until its challenge is completed.

## 5.1 Rank 10 specialization

At Rank 10:

- complete a short specialization trial through an appropriate guild/trainer/shrine/world facility;
- choose one of the two canonical first branches already defined in `CLASS_COMBAT_KITS.md`;
- branch mechanic, two branch actives, branch passive and branch ultimate become available as already specified there;
- root skills remain available.

First specialization trial target length: **5–8 minutes**.

## 5.2 Rank 20 technique advancement

Each active branch gains:

- one new branch active;
- its second passive-tree tier;
- one mechanical extension to the branch resource/loop.

Target challenge length: **8–12 minutes**.

## 5.3 Rank 32 doctrine advancement

Each branch chooses one of **two mutually exclusive doctrines**.

- doctrines are loadout/build choices, not permanent account choices;
- only one doctrine in that branch is active at once;
- doctrine may be switched outside combat from the class screen once both are unlocked;
- doctrine changes a mechanic or rotation rule and at least one skill family, not only +damage.

Target challenge length: **10–15 minutes**.

## 5.4 Rank 44 ascendant advancement

Each branch receives:

- a final launch-era mechanic evolution;
- capstone passive-tree access;
- a choice of two branch-ultimate augments.

Target challenge length: **12–20 minutes**, normally attached to a real world encounter/POI/branch quest rather than a menu test.

## 5.5 Rank 50 Deep Mastery challenge

Rank 50 opens one more substantial class challenge.

Reward:

- class title/cosmetic presentation where an external-quality asset exists;
- one class `Insight` completion credit if the player has not reached the five-point world-insight cap;
- no hidden +20% universal damage bonus;
- no `final class` wording.

---

# 6. Branch switching / permanence

Specialization is meaningful but not a permanent character trap.

- the first selected branch unlocks permanently after its Rank-10 trial;
- after Rank 20, the sibling branch can be unlocked by completing that branch's own Rank-10 specialization trial once;
- Class Rank is shared between both branches of one root class;
- each branch stores its own completed milestone trials, doctrine choice and passive allocation;
- inactive-branch passives and skills never remain active;
- switching branch is allowed only out of combat at an appropriate shrine/trainer/class facility.

Cost at player Lv `L`:

```text
BranchSwitchCost(L) =
  round_to_10(max(150, min(1500, 0.60 * SwitchCost(L))))
```

No cooldown.

This preserves meaningful identity without requiring the player to level the same root class twice.

---

# 7. Passive Point economy

Each root class has at most **30 spendable Passive Points** at launch.

## 7.1 Rank points

Gain 1 Passive Point at every even Class Rank:

```text
2, 4, 6, ... 50
```

Total from rank: **25**.

## 7.2 World Insight points

Each root class has eight authored `Class Insight` challenges distributed through the world.

- completing any **five of eight** grants one Passive Point each;
- after five point-granting Insights, additional Insights give their normal authored reward but no extra passive point;
- the player is therefore never required to clear all eight;
- Insights are not a separate spendable currency: completion directly grants the class Passive Point;
- Insight completion is permanent per class.

Total launch points:

```text
25 rank points + 5 Insight points = 30
```

This follows the useful part of GW2 Hero Challenges: exploration accelerates/builds progression, but exhaustive checklist completion is unnecessary.

## 7.3 Respec

Full passive respec at a shrine/trainer:

```text
PassiveRespecCost(L) =
  round_to_10(max(50, min(500, 0.20 * SwitchCost(L))))
```

- no cooldown;
- root-class and active-branch points are refunded together;
- branch-specific saved allocation updates only for the branch being edited;
- respeccing cannot be done during combat.

---

# 8. Passive-tree shape

Each equipped class build sees:

- **7 root nodes** shared by both branches;
- **8 nodes** belonging to the active branch;
- up to one optional Hidden Technique node when discovered.

This is intentionally far smaller than a 100+ node tree.

Root-tree maximum cost is 17 points. Branch-tree maximum cost is 18 points. With 30 available points the player cannot max every root and branch node simultaneously.

Branch tier requirements:

| Tier | Requirement |
|---|---|
| Branch I | Rank 10, branch unlocked, at least 5 root points spent |
| Branch II | Rank 20 milestone complete, at least 4 branch points spent |
| Branch III | Rank 32 milestone complete, at least 9 branch points spent |
| Capstone | Rank 44 milestone complete, at least 14 branch points spent |

The UI uses a compact Lucifer-derived vertical/horizontal spine rather than a giant draggable constellation. The whole current root+branch path should be understandable without zooming through a web of filler nodes.

---

# 9. Warrior passive tree

## Root nodes

| Node | Max | Effect per point / rule |
|---|---:|---|
| Steel Nerve | 3 | +3% MaxHP |
| Tireless Combatant | 3 | +4 Max Stamina and +1% Stamina recovery |
| Weapon Rhythm | 3 | while Momentum > 0, +2% Attack Speed |
| Crushing Intent | 3 | +4% Warrior poise damage |
| Held Momentum | 2 | Momentum expiry timer +1.0 s |
| Counterforce | 2 | Iron Counter direct and poise output +8% |
| Battle Temper | 1 | spending 3+ Momentum restores 10 Stamina and 8 Mana; 4 s personal ICD |

## Vanguard branch nodes

| Node | Max | Unlock | Effect |
|---|---:|---|---|
| Forward Pressure | 3 | I | movement-tagged Warrior/Vanguard skills +3% direct damage/point |
| Onslaught Rhythm | 3 | I | Onslaught Attack Speed bonus +2 percentage points/point |
| Break Lines | 3 | II | empowered Momentum spenders +5% poise damage/point |
| Rending Edge | 3 | II | Fractured Armor duration +0.5 s/point |
| Unbroken Advance | 2 | III | effective player poise during Vanguard committed movement +10%/point |
| Fuel the Assault | 2 | III | 4-Momentum spend restores 3 Mana/point |
| Overrun | 1 | Capstone | after an empowered movement skill hits an elite/boss, gain +10% combat movement for 3 s; 6 s ICD |
| Siegebreaker | 1 | Capstone | +12% direct damage against a target currently in poise-break state |

## Armsmaster branch nodes

| Node | Max | Unlock | Effect |
|---|---:|---|---|
| Keen Guard | 3 | I | successful perfect guard / Iron Counter Stamina cost -5%/point |
| Riposte Edge | 3 | I | counter-tagged direct damage +4%/point |
| Measured Blows | 3 | II | +4% poise damage and +1% crit chance on the first melee hit after perfect guard per point |
| Weapon Familiarity | 3 | II | basic-attack whole-cycle damage +3%/point |
| Calm Momentum | 2 | III | Momentum expiry +1.5 s/point after a successful perfect guard |
| Exacting Edge | 2 | III | first melee active within 2 s of perfect guard +5% direct damage/point |
| Perfect Measure | 1 | Capstone | Iron Counter success grants 1 additional Momentum if current Momentum is 0–1; 5 s ICD |
| Master at Arms | 1 | Capstone | after a Momentum spender, next basic-attack cycle gains +15% poise and cannot be interrupted by a light hit; 5 s ICD |

---

# 10. Hunter passive tree

## Root nodes

| Node | Max | Effect |
|---|---:|---|
| Keen Eye | 3 | +2% Critical Chance |
| Light Step | 3 | while holding a ranged-capable weapon, +2% combat movement speed |
| Efficient Draw | 3 | Hunter skill Mana cost -3% |
| Quarry Pressure | 3 | direct damage to current Quarry +2% |
| Focus Retention | 2 | Focus expiry +1.5 s |
| Weakpoint Study | 2 | authored weak-point multiplier +0.04 |
| Trail Sense | 1 | first damaging hit on a newly marked Quarry grants +1 Focus; once per target every 20 s |

## Ranger branch nodes

| Node | Max | Unlock | Effect |
|---|---:|---|---|
| Skirmisher's Step | 3 | I | after a ranged active, +3% movement speed/point for 2 s |
| Trapcraft | 3 | I | Ranger trap trigger radius +6% and duration +5%/point |
| Ricochet | 3 | II | secondary-target Ranger projectile damage +5%/point |
| Field Focus | 3 | II | hitting a trapped/controlled Quarry has +8% chance/point to grant 1 Focus; 1.5 s ICD |
| Mobile Quarry | 2 | III | Focus expiry pauses for 0.5 s/point after a dodge |
| Lightfoot | 2 | III | committed movement penalty while using Ranger actives reduced by 5 percentage points/point |
| Trailblazer | 1 | Capstone | dodge followed by a ranged hit within 1.2 s grants +1 Focus; 5 s ICD |
| Pack Hunt | 1 | Capstone | damage to Quarry from you grants nearby allies +5% movement toward that Quarry for 3 s; 8 s ICD |

## Marksman branch nodes

| Node | Max | Unlock | Effect |
|---|---:|---|---|
| Steady Aim | 3 | I | weak-point direct damage +3%/point |
| Longshot | 3 | I | direct ranged damage from >=12 blocks +3%/point |
| Penetrating Force | 3 | II | Marksman attacks ignore 2% EffectiveDefense/point |
| Measured Breath | 3 | II | Marksman skill Mana cost -3% and aim movement penalty reduced 3 percentage points/point |
| Focused Quarry | 2 | III | Focus expiry +1.5 s/point while current Quarry remains in line of sight |
| Critical Window | 2 | III | crit multiplier against Quarry +0.04/point |
| One Shot | 1 | Capstone | first ranged hit after 2.0 s without attacking gains +15% direct damage; 6 s ICD |
| Executioner | 1 | Capstone | +15% direct damage to poise-broken Quarry targets |

---

# 11. Cleric passive tree

## Root nodes

| Node | Max | Effect |
|---|---:|---|
| Wellspring | 3 | +5 Max Mana |
| Mercy | 3 | effective healing +3% |
| Sacred Guard | 3 | barrier amount +3% |
| Resolute Faith | 3 | Magic Resistance contribution from equipment +4% |
| Lingering Grace | 2 | Grace expiry +1.5 s |
| Balanced Service | 2 | after damaging an eligible enemy, next heal/barrier skill within 4 s costs 5% less Mana/point; after effective healing, next damaging Cleric skill gets the same reduction |
| Living Doctrine | 1 | reaching maximum Grace restores 8 Mana; 6 s ICD |

## Saint branch nodes

| Node | Max | Unlock | Effect |
|---|---:|---|---|
| Gentle Hands | 3 | I | Saint healing +3%/point |
| Overflowing Light | 3 | I | Saint overheal-to-barrier conversion +3 percentage points/point where that conversion is allowed |
| Sanctuary Keeper | 3 | II | ground support radius +4% and duration +4%/point |
| Aegis | 3 | II | Saint barrier +3%/point |
| Purifying Grace | 2 | III | cleanse-enabled skills cooldown -5%/point |
| Benediction | 2 | III | allies effectively healed by Saint gain +4% Mana/Stamina recovery for 3 s/point; does not stack from same Saint |
| Saving Grace | 1 | Capstone | effective heal on ally below 25% HP gets +20% healing; 12 s target ICD |
| Shared Light | 1 | Capstone | when healing another ally, self receives healing equal to 20% of effective amount; cannot recursively trigger |

## Inquisitor branch nodes

| Node | Max | Unlock | Effect |
|---|---:|---|---|
| Judgment | 3 | I | direct damage to Judged targets +3%/point |
| Smitecraft | 3 | I | Inquisitor poise damage +4%/point |
| Battle Mercy | 3 | II | authored damage-to-healing conversion +1 percentage point/point |
| Lasting Brand | 3 | II | Judged duration +0.5 s/point |
| Condemn | 2 | III | Judged targets lose 3% Magic Resistance/point, boss effect halved |
| Zeal | 2 | III | damaging a Judged elite/boss has +10% chance/point to grant 1 Grace; 2 s ICD |
| Verdict | 1 | Capstone | consuming Judged on elite/boss grants +5 Ultimate Charge; 6 s ICD |
| Penance | 1 | Capstone | offensive Cleric skill that produces effective ally healing also grants that ally a 5% MaxHP barrier; 8 s target ICD |

---

# 12. Mage passive tree

## Root nodes

| Node | Max | Effect |
|---|---:|---|
| Deep Well | 3 | +6 Max Mana |
| Arcane Efficiency | 3 | Mage skill Mana cost -3% |
| Spell Edge | 3 | +2% Magic Power |
| Quick Sigils | 3 | +2% cast speed |
| Weave Memory | 2 | Arcane Weave sequence expiry +1.5 s |
| Triune Study | 2 | spell empowered by completed Weave gains +5% direct/utility magnitude/point |
| Resonant Mind | 1 | completing a valid Weave restores 8 Mana; 5 s ICD |

## Elementalist branch nodes

| Node | Max | Unlock | Effect |
|---|---:|---|---|
| Firecraft | 3 | I | Burning output +5%/point |
| Frostcraft | 3 | I | Chilled duration/control magnitude +4%/point within global caps |
| Stormcraft | 3 | II | Lightning-tagged direct damage +3% and poise +3%/point |
| Conduction | 3 | II | elemental spell Mana cost -3%/point |
| Elemental Memory | 2 | III | multi-element sequence timer +1.0 s/point |
| Confluence | 2 | III | a spell using a different element from previous spell gains +4% direct/status output/point |
| Triad | 1 | Capstone | completing Fire/Frost/Lightning sequence triggers a 0.80-coefficient visual pulse on the primary target; 6 s ICD |
| Elemental Heart | 1 | Capstone | for 5 s after a three-element sequence, +15% Mana recovery |

## Arcanist branch nodes

| Node | Max | Unlock | Effect |
|---|---:|---|---|
| Arcane Power | 3 | I | Arcane-tagged direct damage +3%/point |
| Phasecraft | 3 | I | Phase Step cooldown -4%/point |
| Binding Formula | 3 | II | non-boss bind/control duration +5%/point; boss control contribution converts to poise as defined by skill |
| Orbital Theory | 3 | II | orbit/projectile-manipulation skill direct damage +3%/point |
| Weave Precision | 2 | III | enhanced spell after Weave +5% direct/utility magnitude/point |
| Rift Efficiency | 2 | III | teleport/control skill Mana cost -4%/point |
| Continuum | 1 | Capstone | Phase Step causes the next accepted Mage skill within 3 s to reduce its own cooldown by 1.0 s; 6 s ICD |
| Singularity | 1 | Capstone | enemies under an Arcanist hard/soft control effect take +10% Arcane direct damage from that Arcanist |

---

# 13. Guardian passive tree

## Root nodes

| Node | Max | Effect |
|---|---:|---|
| Bulwark | 3 | +3% MaxHP |
| Enduring Guard | 3 | +4 Max Stamina and +1% Stamina recovery |
| Shieldcraft | 3 | guard impact Stamina cost -4% |
| Protective Force | 3 | Guardian barrier +3% |
| Resolve Keeper | 2 | Resolve expiry +1.5 s |
| Defiant Retort | 2 | perfect-guard poise output +8% |
| Stand Together | 1 | reaching max Resolve grants nearest ally within 6 blocks a 5% MaxHP barrier; in solo grants self 3%; 10 s ICD |

## Bastion branch nodes

| Node | Max | Unlock | Effect |
|---|---:|---|---|
| Wallcraft | 3 | I | Bastion barrier +3%/point |
| Guarded Ground | 3 | I | while inside own Bastion ground zone, damage taken -2%/point |
| Heavy Stance | 3 | II | player poise +5%/point while guarding or channeling Bastion skill |
| Provocation | 3 | II | Provoked normal-enemy duration +8%/point; boss threat contribution +8%/point without forced AI override |
| Shared Shield | 2 | III | barrier placed on ally also grants self barrier equal to 10% of applied amount/point |
| Resolve Fortification | 2 | III | each Resolve pip grants +2% guard efficiency/point |
| Living Wall | 1 | Capstone | when a barrier you placed on another ally breaks from hostile damage, gain +1 Resolve; 4 s ICD |
| Citadel | 1 | Capstone | while at maximum Resolve, take 8% less direct damage and gain +10% guard efficiency |

## Sentinel branch nodes

| Node | Max | Unlock | Effect |
|---|---:|---|---|
| Retaliation | 3 | I | counter-tagged direct damage +4%/point |
| Denial | 3 | I | perfect-guard poise output +5%/point |
| Intercept | 3 | II | Sentinel movement/control active range +4%/point |
| Vigilance | 3 | II | eligible perfect guard has +10% chance/point to grant an extra Resolve; 2 s ICD |
| Guard Rhythm | 2 | III | Sentinel counter skill cooldown -5%/point after perfect guard |
| Zone Control | 2 | III | Sentinel slows/knockback-control magnitude +6%/point within boss/control caps |
| Watchful | 1 | Capstone | first perfect guard every 8 s grants nearest ally within 6 blocks a 5% MaxHP barrier |
| No Passage | 1 | Capstone | targets you perfect-guard become `Disrupted` for 3 s: +15% poise damage taken from you; bosses retain the poise effect but ignore displacement |

---

# 14. Rank-20 branch actives

These become additional choices; the player still equips only four normal actives.

| Branch | Skill | Cost / CD | Core numbers / role |
|---|---|---|---|
| Vanguard | **Crashing Pursuit** | 28 Mana / 9 s | 4.0-block collision-safe rush; ActionCoeff 2.40; PoiseCoeff 2.30; if it lands during Onslaught, extend current Onslaught by 1.0 s once per cast |
| Armsmaster | **Masterstroke** | 24 Mana / 10 s | ActionCoeff 2.15; PoiseCoeff 2.10; when used within 1.2 s after perfect guard/Iron Counter, becomes 2.70 / 3.00 |
| Ranger | **Hunting Net** | 24 Mana / 12 s | 3.5-block cone; ActionCoeff 1.40; normal targets Snared 2.0 s, elites 0.8 s, bosses receive only authored 10% movement slow for 2.0 s |
| Marksman | **Heartpiercer** | 34 Mana / 12 s | 0.90 s committed aim; ActionCoeff 3.40; PoiseCoeff 1.10; weak-point multiplier +0.15 for this shot only |
| Saint | **Renewal** | 34 Mana / 16 s | HealCoeff 0.34; remove one cleanse-eligible negative effect; if target began below 40% HP, additionally BarrierCoeff 0.08 |
| Inquisitor | **Judgment Chain** | 30 Mana / 11 s | ActionCoeff 2.40; applies Judged; hitting already-Judged target adds 0.60 coefficient and produces HealCoeff 0.08 to lowest-HP eligible ally within 6 blocks |
| Elementalist | **Chain Lightning** | 30 Mana / 10 s | up to 3 visible jumps; total ActionCoeff 2.40 distributed 1.10/0.75/0.55; each target only once per cast |
| Arcanist | **Gravity Well** | 34 Mana / 14 s | 4.0-block field, 3 s; total ActionCoeff 1.80; pulls normal targets toward center, elites slowed; bosses take added poise pressure instead of forced displacement |
| Bastion | **Anchor Slam** | 30 Mana / 12 s | ActionCoeff 2.20; PoiseCoeff 2.50; creates 4.0-block zone for 4 s granting allies +10% guard efficiency |
| Sentinel | **Interdict** | 26 Mana / 9 s | 2.8-block collision-safe intercept; ActionCoeff 1.80; PoiseCoeff 2.20; hit target becomes Disrupted for 3 s, delaying its poise recovery start by +1.0 s |

No new active above receives hidden i-frames unless explicitly added by later canonical revision.

---

# 15. Rank-32 doctrines

Only one doctrine for the current branch is active at once. Switching doctrine outside combat is free after both are unlocked.

## Vanguard

- **Relentless Tempo** — Onslaught gains +5 percentage points Attack Speed; Warrior/Vanguard active dodge-cancel threshold occurs 5 percentage points earlier, never earlier than 60% of the action.
- **Siegebreaker Doctrine** — empowered Momentum spenders deal +25% poise damage; direct damage unchanged.

## Armsmaster

- **Duelist's Measure** — first melee hit within 2 s after perfect guard gains +12% direct damage and +10% crit chance; 3 s ICD.
- **Weapon Savant** — basic-attack whole cycles gain +10% poise and +6% direct damage; Momentum generated by basic cycles lasts 2 s longer.

## Ranger

- **Skirmisher** — moving at least 3 blocks since the previous ranged hit makes the next Quarry hit grant +1 Focus; 3 s ICD.
- **Field Controller** — Ranger traps/ground zones gain +20% duration and hitting a controlled Quarry deals +10% poise damage.

## Marksman

- **Patient Aim** — remaining nearly stationary for 0.75 s before a committed ranged skill grants that shot +12% direct damage and +0.08 weak-point multiplier; taking hostile damage or dodging cancels the preparation.
- **Execution Window** — +10% direct damage against poise-broken Quarry targets; full-Focus weak-point hit extends the current poise-break vulnerability by 0.15 s, once per break.

## Saint

- **Mercy Doctrine** — effective healing on a target below 40% HP +12%; no increase on overheal.
- **Aegis Doctrine** — Barrier amount +12%; barriers you create have +2 s duration, still respecting the global barrier cap.

## Inquisitor

- **Condemnation** — consuming/detonating Judged gains +15% direct damage and +20% poise output.
- **Penance Doctrine** — damage-to-healing effects +35% relative healing output, but their direct damage is not increased.

## Elementalist

- **Confluence** — casting three different elemental tags in sequence causes the next different-element spell within 5 s to gain +15% direct/status output.
- **Dominion** — choose Fire, Frost or Lightning as a dominant element outside combat; dominant-element output +12%, other elements unchanged. Changing dominance requires being out of combat for 5 s.

## Arcanist

- **Riftwalker** — Phase Step / teleport-tagged skills cost 20% less Mana and grant +15% combat movement for 2 s.
- **Spellshaper** — orbit/projectile-manipulation skills gain +12% direct damage and +15% projectile velocity/range where the visual still matches collision.

## Bastion

- **Bodyguard** — barrier placed on another ally +15%; moving toward an ally below 50% HP grants +10% movement speed, capped to 4 s and 8 s ICD.
- **Stronghold** — while inside your own Bastion zone, +15% player poise and +10% guard efficiency.

## Sentinel

- **Counterguard** — successful perfect guard causes next counter-tagged skill within 3 s to gain +20% poise and +10% direct damage.
- **Warden** — Sentinel control zones +20% radius and +20% duration, but no extra boss displacement.

---

# 16. Rank-44 branch ascendant rules

Rank 44 does not introduce another resource bar. It sharpens the existing branch.

## Vanguard

Mechanic evolution:

- after an empowered 4-Momentum spender, retain **1 Momentum** instead of always returning to 0; 6 s ICD.

Ultimate augment choice:

- **Rupture** — equipped Vanguard ultimate +30% poise output and applies Fractured Armor for 8 s;
- **Overrun** — after Vanguard ultimate resolves, gain Onslaught for 6 s and restore 20 Stamina.

## Armsmaster

Mechanic evolution:

- successful perfect guard grants `Mastery` for 4 s: next melee active +12% direct damage and +20% poise; 6 s ICD.

Ultimate augment:

- **Perfect Sequence** — first successful perfect guard during the branch ultimate resets Iron Counter once;
- **Weapon Finale** — branch ultimate +20% direct damage and +1.0 whole-action PoiseCoefficient.

## Ranger

Mechanic evolution:

- when a Quarry dies, Quarry may transfer to the nearest eligible hostile within 10 blocks and preserve up to 2 Focus; 3 s ICD.

Ultimate augment:

- **Wide Hunt** — secondary valid Quarry targets receive 70% of primary ultimate output;
- **Pursuit** — single primary Quarry receives +20% ultimate direct damage and Ranger gains +15% combat movement for 6 s afterwards.

## Marksman

Mechanic evolution:

- full-Focus weak-point hit reduces Heartpiercer remaining cooldown by 20%; 2 s ICD.

Ultimate augment:

- **Dead Center** — branch ultimate weak-point multiplier +0.20 and ignores 15% EffectiveDefense/MagicResistance as appropriate;
- **Clean Exit** — after branch ultimate resolves, set Focus to at least 3 and gain +20% movement for 4 s.

## Saint

Mechanic evolution:

- while at max Grace, first effective heal on an ally below 35% HP consumes 1 Grace and also grants BarrierCoeff 0.08; 8 s target ICD.

Ultimate augment:

- **Miracle** — if a downed ally is inside the valid ultimate area, revive one nearest downed ally at 20% MaxHP; still applies Rescue Fatigue; if no downed ally exists, ultimate healing +20%;
- **Radiant Shelter** — branch ultimate healing/barrier +15% and persistent support field duration +3 s where applicable.

## Inquisitor

Mechanic evolution:

- consuming Judged on an elite/boss grants +1 Grace; 4 s ICD.

Ultimate augment:

- **Final Verdict** — branch ultimate +20% direct and +25% poise output;
- **Redemptive Verdict** — 25% of branch-ultimate post-mitigation damage dealt to eligible targets is converted into distributed effective healing, respecting normal anti-recursion rules.

## Elementalist

Mechanic evolution:

- completing a Fire/Frost/Lightning sequence grants `Triune` for 6 s; next elemental spell gains +12% direct/status output.

Ultimate augment:

- **Cataclysm** — +20% ultimate direct output, +20% area, +25% poise output;
- **Perfect Storm** — after ultimate, +50% Mana recovery and +20% cooldown-recovery speed for elemental skills for 6 s.

## Arcanist

Mechanic evolution:

- completing a valid Arcane Weave reduces Phase Step remaining cooldown by 1 s and restores 4 Mana; 5 s ICD.

Ultimate augment:

- **Event Horizon** — Singularity/control ultimate radius +20% and poise output +40%;
- **Collapse** — final visible detonation +30% direct damage, without increasing control duration.

## Bastion

Mechanic evolution:

- reaching maximum Resolve grants `Fortified` for 5 s: +10% guard efficiency; refreshing max Resolve refreshes duration but does not stack.

Ultimate augment:

- **Citadel** — branch ultimate creates an 8 s stationary protection zone; allies inside take 15% less direct damage, respecting total mitigation caps;
- **Marching Wall** — protection follows the Guardian for 8 s but reduction is 10% instead of 15%.

## Sentinel

Mechanic evolution:

- perfect guarding an elite/boss while at max Resolve applies Disrupted for 4 s instead of 3 s and deals +10% additional poise pressure; 4 s ICD.

Ultimate augment:

- **No Passage** — ultimate control/poise output +30%, area +15%;
- **Reversal** — during the ultimate window, each successful perfect guard triggers a 1.00-coefficient visible counter pulse, max once every 0.8 s.

---

# 17. World-discovered class skills

Exploration/bosses may unlock additional actives without making them mandatory for baseline class viability. These are permanent root-class unlocks and can be slotted alongside root/branch actives if the active class can use them.

Every skill below still requires the external animation/VFX asset-intake gate from `CLASS_COMBAT_KITS.md` before implementation.

## Warrior

### Seismic Lunge — R03 mountain combat discovery

- source: Rocky Roller / Basalt-Wyvern highland technique quest;
- 30 Mana / 12 s;
- 4.5-block collision-safe line advance;
- ActionCoeff 2.35;
- PoiseCoeff 2.75;
- no i-frames.

### Executioner's Arc — R09 fortress discovery

- source: Executioner/fortress combat chain;
- 34 Mana / 13 s;
- wide committed arc;
- ActionCoeff 2.70;
- PoiseCoeff 1.80;
- +15% direct damage if the target is already poise-broken.

## Hunter

### Briar Snare — R05 jungle discovery

- 26 Mana / 14 s;
- one placed snare at a time;
- 12 s field lifetime;
- 2.6-block trigger area;
- ActionCoeff 1.50 on trigger;
- normal Snared 2 s, elite 0.75 s, boss 10% slow for 2 s only.

### Piercing Volley — R07 caravan/desert ranged technique

- 32 Mana / 13 s;
- requires `ranged_weapon`;
- three-projectile sequence total ActionCoeff 2.70;
- first projectile may pierce one additional normal/elite target at 65% output;
- boss hit remains one target per projectile.

## Cleric

### Cleansing Light — R02 Lich/sanctum discovery

- 28 Mana / 14 s;
- HealCoeff 0.20;
- removes one cleanse-eligible negative status;
- if cast on self with no removable status, still performs healing but gains no fake cleanse charge/reward.

### Lunar Benediction — R08 Moonpriest discovery

- 36 Mana / 20 s;
- 5-block support pulse;
- BarrierCoeff 0.16;
- restores 8 Mana to affected allies once per cast, including caster if eligible;
- does not create infinite Mana loops through duplicate Clerics.

## Mage

### Frost Lance — R04 frozen-crown discovery

- 26 Mana / 8 s;
- ActionCoeff 2.05;
- narrow fast projectile;
- applies canonical Chilled buildup/effect as defined for Mage skills.

### Magma Orb — R10 volcanic discovery

- 40 Mana / 16 s;
- ActionCoeff 3.20 total across impact/explosion;
- 3.5-block explosion;
- applies the canonical Burning effect;
- projectile cannot phase through terrain.

## Guardian

### Stoneward — R03 highland defensive technique

- 28 Mana / 15 s;
- BarrierCoeff 0.18 to self and nearest eligible ally within 6 blocks;
- +15% guard efficiency for 5 s;
- solo use still functions on self.

### Obsidian Retort — R10 Scorch-Golem technique

- 30 Mana / 12 s;
- ActionCoeff 2.20;
- PoiseCoeff 2.80;
- if used within 2 s after a successful guard/perfect guard, also grants BarrierCoeff 0.10 to self.

---

# 18. Hidden Techniques

Hidden Techniques are optional world discoveries, not mandatory sixth advancement branches. Each root class has one launch Hidden Technique node.

- requires Class Rank 44+;
- requires the authored world encounter/discovery;
- costs **1 normal Passive Point** once unlocked;
- inactive-class hidden nodes never apply;
- they are intentionally strong build options, not required baseline balance assumptions.

## Warrior — Mountain's Answer

Source: R03 Basalt-Wyvern/major highland mastery challenge.

- perfect-guarding a heavy/crush attack grants +1 Momentum and causes the next Warrior skill within 4 s to deal +20% poise damage;
- 8 s ICD.

## Hunter — Apex Instinct

Source: late hunt chain using a major regional predator/field boss.

- full-Focus weak-point hit preserves 1 Focus after the normal spend/consumption behavior and grants +6 Ultimate Charge;
- 8 s ICD.

## Cleric — Moon Covenant

Source: R08 Moonpriest/sanctuary chain.

- effective healing or barrier application to an ally below 35% HP also grants a 8% target-MaxHP barrier;
- 12 s target ICD.

## Mage — Anomaly Lens

Source: R12 Farseer/anomaly research chain.

- completing a valid three-tag Weave restores 10 Mana and causes the next spell within 5 s to ignore 8% relevant Magic Resistance;
- 8 s ICD.

## Guardian — Obsidian Oath

Source: R10 Scorch-Golem/volcanic defense challenge.

- perfect-guarding a heavy/crush attack grants allies within 5 blocks BarrierCoeff 0.06; in solo self receives BarrierCoeff 0.04;
- 8 s ICD.

---

# 19. Class Insight challenge sets

Exact coordinates wait for Azari import, but the challenge identities/regions are fixed enough that implementation is not allowed to invent a generic checklist later.

Only the first five completed Insights per root class grant Passive Points.

## Warrior candidate Insights

1. R01 Steelboar impact/poise challenge.
2. R03 Rocky Roller break challenge.
3. R03 Basalt Wyvern highland duel.
4. R07 Armor of Desert guard-break challenge.
5. R09 Flamehorn impact hunt.
6. R09 fortress Executioner challenge.
7. R10 Scorch Golem poise challenge.
8. R12 Terradragon-tier mastery encounter.

## Hunter candidate Insights

1. R01 Regalhart weak-point hunt.
2. R02 forest guardian tracking challenge.
3. R04 Iceworm hunt.
4. R05 jungle predator/Komodo tracking challenge.
5. R07 Deathworm observation/hunt objective.
6. R08 Titan Rabbit precision challenge.
7. R11 abyssal hunt.
8. R12 Farseer/anomaly target challenge.

## Cleric candidate Insights

1. R02 Lich cleanse/sanctum objective.
2. R04 expedition rescue objective.
3. R06 Hydra-region field support event.
4. R08 Moonpriest encounter.
5. R08 sanctuary protection discovery.
6. R10 volcanic refugee/protection event.
7. R11 maritime rescue event.
8. R12 Reaper-Lich/anomaly purification objective.

## Mage candidate Insights

1. R02 ruined-sanctum arcane discovery.
2. R04 frost phenomenon research.
3. R05 mature Earthloong elemental interaction.
4. R06 Hydra multi-element encounter.
5. R08 Moonpriest magical archive.
6. R10 Inferno/volcanic spell challenge.
7. R11 abyssal magic discovery.
8. R12 Farseer anomaly research.

## Guardian candidate Insights

1. R01 Earthloong defensive clear.
2. R03 highland convoy/wyvern defense.
3. R04 expedition rescue holdout.
4. R06 flooded-shrine defense event.
5. R07 caravan defense.
6. R09 fortress defense encounter.
7. R10 Scorch-Golem holdout.
8. R11 harbor/ship defense event.

Insight reward ownership is personal in multiplayer. One player completing an eligible challenge cannot consume another player's opportunity.

---

# 20. Class progression UI / UX

The class screen uses the established Lucifer UI language.

Required information hierarchy:

1. active root class + Class Rank / XP bar;
2. current root mechanic summary;
3. current specialization and next advancement milestone;
4. compact root + active-branch passive spine;
5. 4-active + 1-ultimate current loadout;
6. world skills / Hidden Technique entries only after discovered or clearly hinted;
7. respec/switch cost before confirmation.

Do not show:

- a 100-node zoom canvas;
- multiple fake currencies;
- giant permanent stat wall;
- undiscovered hidden-technique exact location spoilers by default;
- developer/internal IDs.

At normal 1920x1080 and ordinary GUI scale, the current root+branch tree should fit as one coherent screen or one short vertical scroll, not require panning around a constellation.

---

# 21. Data contract

Suggested data layout:

```text
classes/
  warrior/
    progression.json
    root_passives.json
    vanguard_passives.json
    armsmaster_passives.json
    insights.json
    world_skills.json
  hunter/...
  cleric/...
  mage/...
  guardian/...
```

Minimum progression definition:

```text
class_id
rank_cap
rank_xp_formula
catchup_rules
passive_point_rank_schedule
insight_point_cap
advancement_milestones[]
branch_switch_cost_rule
passive_respec_cost_rule
```

Minimum passive node:

```text
id
class_id
branch_id | root
max_rank
point_cost_per_rank
unlock_rank
prerequisites[]
effects[]
icon_binding
```

Minimum advancement milestone:

```text
id
class_id
branch_id
required_class_rank
required_trial
unlocks[]
mechanic_changes[]
doctrine_choices[]
ultimate_augments[]
```

Progression, Class XP, Passive Points, branch unlocks, doctrine state and Insight completion are server-authoritative saved data.

---

# 22. Multiplayer / exploit rules

- Class XP and Passive Points are personal progression;
- party members do not reduce each other's Class XP reward merely by participating;
- support contribution counts toward encounter eligibility;
- AFK proximity does not count as contribution;
- an Insight is personal and cannot be consumed by another player;
- branch/passive changes require out-of-combat server acceptance;
- disconnect/relog does not refund a paid respec while keeping the new allocation;
- no client packet may directly set Rank, XP, passive point count or branch state.

---

# 23. First implementation acceptance

When this subsystem is eventually implemented, verify at minimum:

1. new root class begins at Rank 1 with its root kit already available;
2. Rank progression uses the defined curve and active-class ownership;
3. Rank 10 branch selection does not erase root skills;
4. sibling branch can later be unlocked without re-leveling the root class;
5. inactive-branch passives cannot leak;
6. 25 rank Passive Points + first five of eight Insights produce exactly 30 maximum points;
7. tree prerequisites prevent capstone skipping;
8. full tree cannot be maxed with 30 points;
9. passive respec and branch switch preserve saved history correctly;
10. late-class catch-up reaches the intended multiplier and cannot turn trivial R01 farming into the optimal route;
11. class switching immediately before quest turn-in does not steal all quest Class XP for an unused class;
12. support contribution earns boss/dungeon Class XP without damage last-hits;
13. Rank-20/32/44 branch mechanics actually alter combat behavior, not only tooltip numbers;
14. Hidden Technique remains optional and does not become a mandatory balance assumption;
15. multiplayer progression remains server authoritative.

---

# 24. What this pass closes

Closed before coding:

- per-root Class Rank cap and XP curve;
- Class-XP reward pacing;
- late-class catch-up;
- five major advancement beats;
- sibling-branch unlock/switch behavior;
- exact Passive Point economy;
- compact tree size/prerequisites;
- all five root passive trees;
- all ten first-branch passive trees;
- Rank-20 branch active for all ten branches;
- Rank-32 doctrine choice for all ten branches;
- Rank-44 mechanic/ultimate augmentation for all ten branches;
- ten world-discovered root-class skills;
- five optional Hidden Techniques;
- Class Insight structure and region/source identities;
- respec costs and server-authority rules;
- progression data contract.

Still intentionally separate design work:

- exact visual asset filename/hash intake for each new world skill/icon/VFX;
- later advancement-stage player-facing lore names if the world narrative produces better names;
- individual enemy/boss kits required to serve the class trials;
- complete status-effect global roster beyond the statuses already required by current class kits;
- final keybind audit after every frequent action is known.

The next design batch should therefore leave class progression and move to **status/element interaction rules + enemy combat archetypes / first-region encounter kits**, while keeping every visible enemy external-first.