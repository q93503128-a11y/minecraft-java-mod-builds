# Open-World RPG — Five Root-Class Combat Kits

> Status: **DESIGN CANON — root-class combat kits and first specialization branches locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Deeper class progression authority: `CLASS_PROGRESSION.md`  
> Combat math/timing: `COMBAT_BALANCE.md`  
> Runtime boundaries: `M0_DEPENDENCY_AUDIT.md`  
> Visual/UI rules: `UI_DIRECTION.md`  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins. Later progression details explicitly closed in `CLASS_PROGRESSION.md` supersede this file's former future-work notes.

This document closes the class decisions that must not be invented while coding. It defines the five root classes, their root mechanic, four starting actives, one root ultimate, passive identity, exact costs/cooldowns/action coefficients/poise values, class-specific ultimate-charge rules, first specialization branches, and external animation/VFX/icon directions.

The combat target remains the master direction: **fast open-world action RPG combat**, not a slow Souls clone, not a long MMO rotation spreadsheet, and not vanilla Minecraft attacks with particles added on top.

---

# 1. External precedents used

External work is used as a quality/interaction reference and, when license/runtime fit allows, as an implementation base. Proprietary/ARR assets are not copied into the public repository.

## 1.1 Guild Wars 2 — profession mechanics

Useful precedent:

- each profession has a distinct, easy-to-read mechanic rather than only different skill names;
- Warrior builds Adrenaline through combat and spends it on bursts;
- Ranger uses a unique field-combat identity rather than merely being `bow warrior`;
- Guardian combines proactive defense and ally support;
- Elementalist changes combat behavior through elemental identity rather than only larger spell numbers;
- specialization changes a profession mechanic/play pattern rather than merely adding +damage.

Project adoption:

- every root class receives **one primary mechanic**;
- the mechanic is deliberately small enough to understand from the HUD;
- first specialization branches alter the mechanic and add real active/passive/ultimate choices.

Not adopted:

- no additional ten-button profession bar;
- no class receives a second full resource system that duplicates Mana/Stamina/Ultimate Gauge without strong value.

## 1.2 Current Minecraft RPG Series — reference only

Current public listings observed during this design pass:

- `Rogues & Warriors (RPG Series)` supports Minecraft 26.2 and focuses Warrior skills on weakening/smashing while Rogue skills emphasize evasion/trickery;
- `Archers (RPG Series)` supports 26.2, works with bows/crossbows through Spell Engine + Ranged Weapon API, and provides a proven active-archery presentation direction;
- `Wizards (RPG Series)` supports 26.2 and clearly separates Arcane focused damage, Fire area damage, and Frost damage/control/shielding;
- `Paladins & Priests (RPG Series)` supports 26.2 and is a strong visual/behavior reference for healing/protection/holy combat;
- `Skill Tree (RPG Series)` supports 26.2 and demonstrates a large class-node structure, but the project does not inherit its tree wholesale.

These content mods are ARR and also pull progression/content dependencies the project does not want as baseline. Therefore:

- they remain **REFERENCE / OPTIONAL LOCAL CONTENT**, not required project dependencies;
- do not copy their source/assets into this public repository;
- project skills are authored against the already-locked Spell Engine / Better Combat / project CombatState boundaries;
- donor spell books, village structures, Runes progression and donor loot rules are not inherited.

## 1.3 Animation source direction

Primary external animation/reference families:

- **Better Combat** — required runtime foundation for player weapon animation/cadence and attack presentation;
- **Quaternius Universal Animation Library 2** — CC0, 130+ animations including melee/armed combos and movement; direct editable/reference source for custom player actions where Better Combat does not already provide a suitable motion;
- Player Animation Library — runtime bridge for accepted player animations.

The project may retarget or simplify external CC0 motion to the Minecraft player skeleton, but it must preserve the external motion idea and hit timing rather than inventing a weak placeholder animation.

## 1.4 VFX source direction

Primary reusable visual source families:

- **Kenney Particle Pack** — CC0, 80 particle/light/shader sprites including magic, fire, sparks and electricity;
- **OpenGameArt CC0 animated particle/effect sheets** — selected fire/magic/electricity/impact/shield/ring effects;
- Spell Engine visual-effect primitives for delivery/projectile synchronization;
- accepted creature/item/model geometry when a skill effect is physically tied to an equipped weapon or external prop.

Important skills are not accepted as `vanilla particle cloud`. Sprite/effect assets are composited into shaped trails, rings, impact planes, decals or projectiles whose visible area matches the server hit area.

## 1.5 Skill icons

- primary source family: **Game-icons.net**, CC BY 3.0, with author attribution recorded per icon;
- icons are recolored/outlined/pixel-treated into the Lucifer UI language;
- one skill never receives a random unrelated icon merely because it is available first;
- exact SVG/PNG source name and author are recorded during asset intake.

---

# 2. Shared class-kit contract

## 2.1 Starting access

When a root class is selected for the first time:

- its root mechanic becomes active immediately;
- its **four root actives** are unlocked immediately;
- its **root ultimate** is unlocked immediately;
- all five fit the canonical `4 active + 1 ultimate` loadout without a tutorial lock sequence;
- later class progression expands choices rather than withholding the basic identity for hours.

A specialization later adds branch-specific active choices/passives/ultimate choices, but the player still equips only four ordinary actives and one ultimate at a time.

## 2.2 Weapon freedom

The master broad-weapon rule remains intact.

- class selection never removes the ability to equip ordinary weapons;
- a skill may require a physical behavior tag when that action cannot logically exist without it;
- `melee_attack_capable`, `ranged_weapon`, and `guard_capable` are behavior tags, not class locks;
- Mage/Cleric magic may be cast while holding non-magic weapons, but magical equipment naturally has better INT/WIL/Magic-Power/Healing synergy;
- Warrior/Hunter/Guardian skills that physically require melee/ranged/guard behavior may be unavailable with an incompatible held setup.

## 2.3 Skill damage

Use `COMBAT_BALANCE.md` direct-damage formula.

Every coefficient below is the **whole visible action coefficient**. Multi-hit skills divide it across visible hits. Extra VFX contacts do not create hidden extra damage.

## 2.4 Healing reference

For Cleric and future support skills:

```text
HealingWeightedStat = 0.80 * WIL + 0.20 * INT
HealingStatMult = AttributeDamageMultiplier(HealingWeightedStat)
HealingReference = BaseHP(casterLv) * HealingStatMult

FinalHeal =
  HealingReference
  * HealCoefficient
  * (1 + HealingDoneBonuses)
  * (1 + targetHealingReceivedBonuses)
```

Rules:

- healing does not critically hit at baseline;
- overhealing is discarded unless a specific passive converts part of it to barrier;
- healing is server-authoritative;
- healing the same full-health target does not count as `effective healing` for class-resource/ultimate generation.

## 2.5 Barrier reference

```text
BarrierWeightedStat = 0.65 * WIL + 0.35 * END
BarrierStatMult = AttributeDamageMultiplier(BarrierWeightedStat)
BarrierReference = BaseHP(casterLv) * BarrierStatMult

BarrierAmount =
  BarrierReference
  * BarrierCoefficient
  * (1 + applicableHealingOrBarrierBonuses)
```

Unless a skill explicitly says otherwise:

- barrier duration: 6 s;
- barriers from the same skill replace/refresh rather than stack infinitely;
- different barrier sources stack only up to **40% of the recipient's MaxHP** at baseline;
- barrier does not count as healing for effects that require restored HP.

## 2.6 Ultimate Gauge

All classes use the same visible ultimate scale:

```text
UltimateCharge: 0..100
```

Rules:

- ultimate use resets charge to 0;
- after use, a **35 s Ultimate Lockout** prevents another activation even if charge somehow reaches 100;
- charge may still accumulate during lockout, but activation remains blocked;
- global ultimate-charge gain cap: **12 charge/s** before explicitly authored encounter mechanics;
- class switching resets current ultimate charge to 0;
- resting at shrine/inn/camp resets current ultimate charge to 0;
- after 45 s completely out of combat, stored charge decays at 5/s until 0;
- low-level farming uses the same encounter-level multiplier as combat EXP: very under-level enemies give sharply reduced charge;
- support charge is based on the active encounter level when healing/protecting allies rather than the ally's character Lv.

Each class below defines its role-aware event gains.

## 2.7 Shared cast acceptance

- resource cost is paid only after the server accepts the cast;
- cooldown begins on accepted cast unless the skill explicitly begins on successful counter/trigger;
- no cooldown/resource refund exploit from changing class, slot or weapon mid-cast;
- all hitboxes, projectile launches, movement and i-frame exceptions are explicit in skill data;
- an active skill does not silently receive dodge i-frames merely because its animation moves the player.

---

# 3. Warrior / 전사

## 3.1 Root identity

Combat identity:

- direct melee pressure;
- weapon mastery;
- meaningful poise destruction;
- one precision counter without becoming the party tank;
- strongest when staying engaged and converting continued pressure into empowered attacks.

Primary suggested stats, not hard requirements:

- STR;
- VIT / END;
- DEX depending on weapon family.

## 3.2 Root mechanic — Momentum

```text
Momentum: 0..3 pips
```

Gain:

- land the final hit/event of a melee basic-attack cycle on an eligible enemy: +1; personal ICD 0.80 s;
- successful `Iron Counter`: +1;
- personally cause an elite/miniboss/boss poise break: +2.

Rules:

- Momentum expires completely after 7 s without landing hostile melee damage or a successful perfect-guard/counter event;
- at 1/2/3 pips, Warrior poise damage is increased by **+5% / +10% / +15%**;
- when at 3 pips, the next root Warrior offensive active marked `Momentum spender` consumes all 3 and applies that skill's empowered rule;
- ordinary basic attacks do not consume Momentum.

This is three small pips on the class-mechanic HUD, not another 0–100 resource bar.

## 3.3 Root passive — Combat Temper

- while Momentum > 0, hit-stagger duration taken from ordinary non-launch hits is reduced by **10%**;
- this does not grant immunity, alter guard break or bypass player-poise rules;
- effect ends immediately when Momentum reaches 0.

## 3.4 Active 1 — Driving Slash

```text
id: warrior_driving_slash
requirement: melee_attack_capable
cost: 20 Mana
cooldown: 6.0 s
movement: forward 2.2 blocks maximum, collision-safe
ActionCoefficient: 1.65
PoiseCoefficient: 1.30
hit profile: 3.5 block frontal arc
wind-up: 0.30–0.40 s by accepted animation
recovery: ~0.35 s
dodge_cancel_start: 70% of action
Momentum spender: yes
```

Empowered at 3 Momentum:

- coefficient becomes **1.90**;
- PoiseCoefficient becomes **1.80**;
- forward movement may extend to 2.6 blocks only if collision/path remains valid.

Purpose: short gap-close/punish tool, not an iframe dash.

## 3.5 Active 2 — Iron Counter

```text
id: warrior_iron_counter
requirement: melee_attack_capable
cost: 18 Stamina
cooldown: 10.0 s
counter stance: 10 ticks / 0.50 s
successful counter coefficient: 1.55
successful counter poise coefficient: 2.00
```

Rules:

- the first `perfect_guardable` melee hit contacting the Warrior during the stance is resolved as a successful perfect guard;
- on success, the Warrior immediately performs the visible counter strike;
- successful counter gives +1 Momentum;
- if no valid hit arrives, the stance ends with 0.35 s recovery;
- Iron Counter does not consume Momentum;
- it cannot counter grabs, floor eruptions or attacks explicitly tagged unguardable.

This is a more committed class action than the universal 0.20 s perfect-guard tap.

## 3.6 Active 3 — Cyclone Cut

```text
id: warrior_cyclone_cut
requirement: melee_attack_capable
cost: 28 Mana
cooldown: 11.0 s
radius: 3.3 blocks
total ActionCoefficient: 2.10
hits: 2 visible hits = 0.90 + 1.20
whole-action PoiseCoefficient: 1.50
movement multiplier during action: 0.75
dodge_cancel_start: 72%
Momentum spender: yes
```

Empowered:

- total coefficient: **2.30**;
- PoiseCoefficient: **2.00**;
- second hit lightly pulls normal enemies up to 0.6 blocks toward the Warrior; elites/bosses are not displaced.

## 3.7 Active 4 — Breaker Slam

```text
id: warrior_breaker_slam
requirement: melee_attack_capable
cost: 36 Mana
cooldown: 15.0 s
wind-up: 0.75 s
total ActionCoefficient: 2.90
PoiseCoefficient: 3.00
hit profile: 4.2 block frontal impact arc
hyperarmor: effective player poise x1.60 during committed wind-up/impact
recovery: 0.70 s
dodge_cancel_start: 82%
Momentum spender: yes
```

Empowered:

- coefficient: **3.35**;
- PoiseCoefficient: **4.00**;
- normal enemies hit at the visible center of impact receive a strong stagger/knockdown reaction where their animation supports it.

## 3.8 Root ultimate — Earthshatter

```text
id: warrior_earthshatter
UltimateCharge cost: 100
cast/wind-up: 1.00 s
total ActionCoefficient: 6.00
PoiseCoefficient: 6.00
range: 7.0 block ground rupture / frontal sector
hyperarmor: effective player poise x2.0 during committed cast
recovery: 1.00 s
```

Rules:

- visible ground rupture and server hit area must match;
- cannot pass through solid walls simply because the VFX continues visually;
- normal enemies may be launched/knocked down;
- bosses take poise pressure but are not displaced unless authored by that boss.

### Warrior ultimate-charge events

- melee basic cycle landed: +2;
- Warrior active hits its primary target: +3;
- successful perfect guard / Iron Counter: +5;
- personally causes elite/miniboss/boss poise break: +10;
- global 12/s cap still applies.

---

# 4. Warrior first specialization branches

The first Warrior specialization changes the mechanic and immediately unlocks two branch actives, one branch passive and one branch ultimate choice. Root skills remain usable.

## 4.1 Vanguard / 공세 계열

Identity: continuous aggression, movement pressure, poise destruction and close-range burst.

### Mechanic change — Onslaught Momentum

```text
Momentum max: 4 pips
```

- gain rules remain the same;
- each pip grants +4% Warrior poise damage and +2% movement speed while in combat;
- at 4 pips, the next Vanguard/Warrior offensive active consumes all 4 and gains **+20% direct damage and +35% poise damage**;
- spending 4 Momentum grants `Onslaught` for 3 s: +10% Attack Speed and +10% combat movement speed.

### Passive — No Retreat

During `Onslaught`:

- ordinary hit-stagger duration taken is reduced by an additional 15%;
- being guard-broken ends Onslaught immediately.

### Branch active — Rending Advance

```text
cost: 22 Mana
cooldown: 6.0 s
ActionCoefficient: 1.90
PoiseCoefficient: 1.50
movement: 3.0 block forward slash
```

Applies `Fractured Armor` for 5 s:

- normal/elite target EffectiveDefense -10%;
- miniboss/boss EffectiveDefense -5%;
- does not stack with itself; refreshes duration.

### Branch active — Relentless Breaker

```text
cost: 38 Mana
cooldown: 14.0 s
ActionCoefficient: 3.20
PoiseCoefficient: 3.60
wind-up: 0.70 s
hyperarmor: x1.80 effective player poise
```

If this hit personally causes a poise break:

- refund 18 Mana;
- Driving Slash/Rending Advance remaining cooldown is reduced by 3 s.

### Branch ultimate — Warpath

Duration: 8 s.

During Warpath:

- +15% Attack Speed;
- +10% movement speed;
- first successful Warrior/basic hit against each target every 1.0 s releases a 2.5-block impact wave around that target;
- impact wave coefficient: 0.45;
- impact-wave poise coefficient: 0.40;
- wave cannot independently generate Momentum or Ultimate Charge.

The VFX is a restrained weapon/ground-impact pulse, not a generic permanent aura explosion.

## 4.2 Armsmaster / 무기 숙련 계열

Identity: precise defense, counter attacks, deliberate weapon-category mastery and personal offensive technique. It must remain distinct from Guardian's ally-protection/denial role.

### Mechanic change — Technique

Momentum is replaced by:

```text
Technique: 0..3 pips
```

Gain:

- melee basic combo finisher: +1, ICD 0.8 s;
- universal perfect guard: +2;
- Master's Riposte success: +2.

At 3 pips, the next Warrior active consumes all 3 and gains one category bonus:

- **fast** (`dagger`, `dual blades`): after cast, +12% Attack Speed for 2.5 s;
- **balanced** (`sword`, `spear`, `axe`): that active gains +15 percentage points Critical Chance and +20% poise damage;
- **heavy** (`greatsword`, `hammer/mace`): that active gains +40% poise damage and x1.50 action hyperarmor where the skill has a committed phase.

Technique expires after 10 s without melee damage/perfect guard.

### Passive — Perfect Form

Successful perfect guard:

- restores 8 Stamina;
- reduces remaining cooldown of each non-ultimate Warrior active by 1.0 s;
- personal ICD 1.0 s.

### Branch active — Master's Riposte

```text
cost: 16 Stamina
cooldown: 8.0 s
active counter window: 6 ticks / 0.30 s
counter ActionCoefficient: 2.20
PoiseCoefficient: 3.00
```

Success:

- resolves incoming valid hit as perfect guard;
- restores additional 10 Stamina after the counter connects;
- grants +2 Technique.

Miss:

- 0.45 s recovery.

### Branch active — Measured Assault

```text
cost: 24 Mana
cooldown: 10.0 s
total ActionCoefficient: 2.40
hits: 3 visible weapon hits = 0.65 + 0.75 + 1.00
whole-action PoiseCoefficient: 1.80
dodge_cancel_start: 65% after second hit
```

Attack motion uses an accepted external three-hit armed combo adjusted to the weapon family rather than three identical vanilla swings.

### Branch ultimate — Grandmaster's Sequence

```text
4-hit committed sequence
total ActionCoefficient: 5.80
whole-action PoiseCoefficient: 4.50
target redirection: <=30 degrees between hits
```

Final-hit category effect:

- fast: final two hits occur 15% faster;
- balanced: final hit +25 percentage points Crit Chance;
- heavy: final hit PoiseCoefficient x1.50.

No hit may auto-track through walls or turn 180 degrees mid-swing.

---

# 5. Hunter / 사냥꾼

## 5.1 Root identity

Combat identity:

- ranged precision;
- mobile repositioning;
- quarry focus;
- field control;
- bow/crossbow baseline with future black-powder support through the same ranged tags.

Suggested stats:

- DEX;
- END;
- STR secondary for some heavy ranged families.

## 5.2 Root mechanic — Quarry + Focus

One hostile target may be the Hunter's **Quarry**.

Quarry assignment:

- first valid ranged basic/skill hit marks that target for 8 s;
- hitting a new target replaces the old Quarry unless a specialization says otherwise;
- striking the current Quarry refreshes duration to 8 s.

```text
Focus: 0..3 pips
```

Gain Focus when hitting Quarry:

- from >=7 blocks: +1, ICD 0.75 s;
- a valid anatomical weak-point hit grants +1 additional Focus, but no more than once per 1.5 s.

Taking direct HP damage removes 1 Focus.
Focus clears when Quarry expires or the Hunter leaves combat for 8 s.

At 3 Focus, the next Hunter active tagged `Focus spender` consumes all and applies its empowered rule.

## 5.3 Root passive — Hunter's Measure

Against Quarry:

- projectile speed +8%;
- weak-point UI may subtly highlight an already-authored weak point after the Hunter has hit it once;
- this never invents a weak point on enemies that do not have one.

## 5.4 Active 1 — Quickstep Volley

```text
requirement: ranged_weapon
cost: 18 Mana
cooldown: 7.0 s
dash: 3.0 blocks in current movement direction
iframes: none
projectiles: 3
total same-target coefficient cap: 1.65 (0.55 each)
whole-action PoiseCoefficient: 0.70
Focus spender: yes
```

Empowered:

- dash becomes 4.0 blocks;
- arrows may pierce one normal enemy, but the same target cannot take more than the normal three-hit coefficient cap.

## 5.5 Active 2 — Pinning Shot

```text
requirement: ranged_weapon
cost: 18 Mana
cooldown: 8.0 s
ActionCoefficient: 1.55
PoiseCoefficient: 1.00
Focus spender: yes
```

Applies `Snared`:

- common/elite: -35% movement for 3.0 s;
- miniboss: -20% for 2.5 s;
- boss: -12% for 2.0 s;
- no full root on bosses.

Empowered:

- duration +1.5 s on non-bosses, +0.5 s on bosses;
- PoiseCoefficient 1.50.

## 5.6 Active 3 — Fan of Arrows

```text
requirement: ranged_weapon
cost: 26 Mana
cooldown: 10.0 s
projectiles: 5 in readable cone
per-target hit cap: 2 arrows
maximum per-target ActionCoefficient: 1.90
whole-action PoiseCoefficient: 1.00
Focus spender: yes
```

Empowered:

- projectile count 7;
- cone widens modestly;
- same-target coefficient cap increases only to **2.15**, preventing point-blank shotgun abuse.

## 5.7 Active 4 — Power Shot

```text
requirement: ranged_weapon
cost: 34 Mana
cooldown: 14.0 s
charge/wind-up: 0.70 s
ActionCoefficient: 3.00
PoiseCoefficient: 1.60
projectile speed: high
special weak-point multiplier for this shot: 1.40x
Focus spender: yes
```

Empowered:

- ActionCoefficient 3.35;
- weak-point multiplier 1.55x.

## 5.8 Root ultimate — Skyfall

Ground-targeted arrow storm:

```text
radius: 7.0 blocks
duration: 4.5 s
maximum total coefficient per target: 5.50
PoiseCoefficient total: 3.00
slow while inside: 25% normal/elite, 10% boss
```

Rules:

- repeated visual arrows are grouped into server damage pulses; they are not dozens of independent hidden hits;
- terrain/ceiling check prevents arrows visibly raining through solid roofs in indoor dungeons;
- effect density must remain readable in multiplayer.

### Hunter ultimate-charge events

- ranged basic hit on Quarry: +2;
- same hit from >=7 blocks: +1 additional;
- valid weak-point hit: +3, ICD 1.0 s;
- Hunter active hits primary Quarry: +3;
- personally causes ranged poise break: +6.

---

# 6. Hunter first specialization branches

## 6.1 Ranger / 레인저 계열

Identity: mobile shooting, multiple marked targets, traps and flexible field pressure.

### Mechanic change — Trail Marks

- up to **3 Quarry-marked targets** may coexist;
- marking a fourth removes the oldest mark;
- Focus is shared globally at max 3;
- eligible ranged hits against any marked target can generate Focus from >=5 blocks instead of 7;
- direct HP damage still removes 1 Focus.

### Passive — Trailblazer

After moving at least 4 blocks since the previous ranged hit:

- next hit against a marked target gains +10% projectile speed;
- it also grants +1 Focus;
- ICD 2.0 s.

### Branch active — Snare Trap

```text
cost: 20 Mana
cooldown: 12.0 s
maximum simultaneously active: 2
placement range: 5 blocks
lifetime: 20 s
trigger radius: 1.8 blocks
ActionCoefficient on trigger: 1.40
PoiseCoefficient: 1.00
```

Snare on trigger:

- common/elite: -45% movement for 4 s;
- miniboss: -25% for 2.5 s;
- boss: -15% for 2 s.

Trap is visible/readable and cannot be stacked 10 times under a stationary boss.

### Branch active — Ricochet Shot

```text
cost: 24 Mana
cooldown: 8.0 s
primary coefficient: 1.35
up to 3 additional marked/nearby targets: 0.75 each
PoiseCoefficient primary: 0.90
```

Bounce priority favors Trail-Marked enemies before unmarked enemies.
One target cannot be hit twice by the same cast.

### Branch ultimate — Wild Hunt

Duration: 8 s.

- Ranger movement speed +15%;
- each currently Trail-Marked enemy is attacked by one spectral/energy arrow pulse every 1 s;
- total automatic coefficient cap per marked target over the full ultimate: **5.00**;
- these automatic hits cannot generate Focus or Ultimate Charge;
- if no target is marked, the first valid ranged hit marks normally and joins the remaining duration.

## 6.2 Marksman / 명사수 계열

Identity: one target, distance discipline, weak points and deliberate high-risk shots.

### Mechanic change — Steady Focus

- Quarry remains limited to one target;
- Focus maximum becomes **5**;
- after 0.75 s while movement speed is <=1 block/s and the player is not sprinting, `Steady Aim` activates;
- while Steady Aim is active, each Focus grants +4% weak-point damage;
- direct HP damage removes 2 Focus and breaks Steady Aim;
- normal movement breaks Steady Aim but does not delete stored Focus.

### Passive — Long Sight

Against Quarry at >=12 blocks:

- direct ranged damage +8%;
- projectile gravity/drop compensation may be improved only within the accepted Ranged Weapon API physics; no hitscan conversion for ordinary bows.

### Branch active — Heartpiercer

```text
cost: 38 Mana
cooldown: 16.0 s
charge: 0.90 s
ActionCoefficient: 3.60
PoiseCoefficient: 1.80
weak-point multiplier: 1.65x
Focus cost: all available, minimum 3 required
```

For each Focus above 3:

- +5% Heartpiercer direct damage, max +10% at 5.

Weak-point hit:

- reduce Heartpiercer remaining cooldown by 4 s after the cast completes.

### Branch active — Deadeye Reposition

```text
cost: 18 Mana
cooldown: 9.0 s
movement: 3.0 block backward/side step
iframes: none
```

If the Hunter ends at least 8 blocks from current Quarry:

- gain +2 Focus;
- next ranged hit within 3 s gains +20% projectile speed.

### Branch ultimate — Final Shot

```text
single-target committed shot
aim time: 1.20 s
ActionCoefficient: 6.50
PoiseCoefficient: 3.00
bonus Crit Chance for this shot: +30 percentage points
weak-point multiplier: 1.75x
maximum targeting range: 30 blocks with line of sight
```

Rules:

- Final Shot is not hitscan if the equipped ranged family is normally projectile-based;
- aim can be cancelled before release, but Ultimate Charge is consumed once release is server-accepted;
- shot cannot pierce walls;
- a missed shot is a real miss.

---

# 7. Cleric / 성직자

## 7.1 Root identity

Combat identity:

- active healing/protection rather than passive MMO heal-bot play;
- holy ranged/offensive pressure sufficient for solo play;
- alternation between helping allies and pressuring enemies;
- small number of strong readable support decisions.

Suggested stats:

- WIL;
- INT;
- VIT / END as needed.

## 7.2 Root mechanic — Grace

```text
Grace: 0..3 pips
```

Gain:

- Cleric damaging active hits an eligible hostile: +1, ICD 1.0 s;
- one Cleric heal restores at least 6% of recipient MaxHP effectively: +1, per-recipient ICD 2.0 s;
- one Cleric barrier is actually consumed by hostile damage for at least 6% recipient MaxHP: +1, per-recipient ICD 2.0 s.

At 3 Grace, next Cleric active tagged `Grace spender` consumes all and applies its empowered effect.

Grace expires after 10 s outside combat.

## 7.3 Root passive — Balanced Doctrine

After using a damaging Cleric active, the next healing/protection Cleric active within 6 s receives +10% Healing/Barrier output.
After using a healing/protection Cleric active, the next damaging Cleric active within 6 s receives +8% direct damage.

Only one side of the doctrine may be primed at once; it alternates rather than stacking.

## 7.4 Active 1 — Radiant Lance

```text
cost: 14 Mana
cooldown: 4.0 s
range: 20 blocks
ActionCoefficient: 1.35 magic
PoiseCoefficient: 0.60
Grace spender: yes
```

Empowered:

- primary coefficient remains 1.35;
- the lance chains to up to 2 additional enemies within 4 blocks for 0.55 coefficient each;
- if no second enemy exists, it instead heals the lowest-health valid ally/self within 8 blocks for `HealCoefficient 0.08`.

## 7.5 Active 2 — Mend

```text
cost: 22 Mana
cooldown: 8.0 s
cast: 0.45 s
range: 12 blocks / self allowed
HealCoefficient: 0.30
Grace spender: yes
```

Empowered:

- HealCoefficient: **0.40**;
- cleanse one project-tagged `minor dispellable` negative status.

## 7.6 Active 3 — Consecrated Ground

```text
cost: 32 Mana
cooldown: 14.0 s
radius: 5.0 blocks
duration: 5.0 s
total ally HealCoefficient: 0.30
total enemy ActionCoefficient: 1.35 magic
Grace spender: yes
```

Empowered:

- allies/self in area receive an initial barrier with `BarrierCoefficient 0.12`;
- barrier follows normal 6 s duration/cap rules.

## 7.7 Active 4 — Rebuke

```text
cost: 24 Mana
cooldown: 10.0 s
hit profile: 4.5 block frontal holy burst
ActionCoefficient: 1.80 magic
PoiseCoefficient: 1.60
Grace spender: yes
```

Applies `Rebuked`:

- normal/elite outgoing direct damage -15% for 4 s;
- miniboss/boss outgoing direct damage -8% for 3 s;
- does not stack with itself.

Empowered:

- PoiseCoefficient: 2.20;
- Rebuked duration +1 s.

## 7.8 Root ultimate — Sanctuary

```text
radius: 7.0 blocks
duration: 8.0 s
initial BarrierCoefficient: 0.25
total HealCoefficient: 0.55
total enemy magic ActionCoefficient while remaining in zone: 2.00
```

Allies in Sanctuary also receive:

- -20% negative-status duration for statuses applied while inside.

Sanctuary is a visible holy ward/decal/vertical light structure whose footprint matches the real radius.

### Cleric ultimate-charge events

- damaging Cleric active hits eligible primary hostile: +2;
- every 5% recipient MaxHP of effective healing: +2, max +6 per cast/recipient;
- every 5% recipient MaxHP of Cleric barrier actually consumed by hostile damage: +2, max +6 per source/recipient;
- cleanse one meaningful negative status: +3;
- no charge from overhealing a full-health ally.

---

# 8. Cleric first specialization branches

## 8.1 Saint / 성인 계열

Identity: stronger healing, cleansing, protection and rescue utility without becoming passive.

### Mechanic change — Benediction

Grace is renamed/modified to `Benediction`:

- effective healing/protection event gives +2 pips instead of +1;
- damaging active still gives +1;
- max remains 3;
- spending 3 on a healing/protection skill refunds **30% of that skill's Mana cost** after successful resolution.

### Passive — Overflowing Grace

- up to 20% of otherwise-wasted overheal becomes barrier;
- barrier created this way is capped at **12% of target MaxHP**;
- repeated overheal refreshes/replaces that passive barrier, not stacks it.

### Branch active — Greater Mend

```text
cost: 24 Mana
cooldown: 8.0 s
HealCoefficient: 0.42
cast: 0.50 s
```

If Benediction-empowered:

- HealCoefficient: 0.55;
- cleanse up to 2 `minor dispellable` statuses or 1 `major dispellable` status.

### Branch active — Hallowed Ground

```text
cost: 34 Mana
cooldown: 16.0 s
radius: 6.0 blocks
duration: 6.0 s
total HealCoefficient: 0.40
total enemy ActionCoefficient: 1.00 magic
```

Allies inside:

- 10% ordinary damage reduction;
- this respects global mitigation caps.

### Branch ultimate — Miracle

Instant arrival pulse in 7-block radius:

- HealCoefficient 0.55;
- BarrierCoefficient 0.25.

Then for 6 s:

- additional total HealCoefficient 0.20.

Downed allies inside:

- rescue window extends by 5 s once;
- revive channel against them is reduced from 2.5 s to 1.5 s while Miracle remains active;
- Miracle does **not** auto-revive.

## 8.2 Inquisitor / 심판관 계열

Identity: holy pressure, marks/judgment and combat-healing so solo and aggressive support remain active.

### Mechanic change — Judgment

Grace becomes `Judgment`:

- damaging Cleric active: +2 pips;
- effective heal/protection event: +1;
- max 3;
- next damaging Inquisitor/Cleric active at 3 consumes all and gains +15% poise damage plus its normal empowered effect.

### Passive — Battle Litany

Damaging a `Judged` target with Cleric/Inquisitor direct damage:

- restore 1 Mana per valid hit;
- maximum 4 Mana/s.

### Branch active — Judgment Bolt

```text
cost: 16 Mana
cooldown: 4.0 s
ActionCoefficient: 1.80 magic
PoiseCoefficient: 0.70
range: 20 blocks
```

Applies `Judged` for 6 s.

While target is Judged:

- Cleric/Inquisitor direct damage heals the lowest-health valid ally/self within 10 blocks for **20% of post-mitigation damage dealt**;
- per-hit healing is capped at `HealCoefficient 0.08` worth of HealingReference;
- Judged does not stack; duration refreshes.

### Branch active — Sentence

```text
cost: 28 Mana
cooldown: 10.0 s
ActionCoefficient: 2.30 magic
PoiseCoefficient: 2.00
```

If target is Judged:

- consume Judged;
- add a second visible detonation with ActionCoefficient 0.80;
- detonation PoiseCoefficient 0.80.

### Branch ultimate — Final Judgment

```text
cast: 1.0 s
radius: 6.0 blocks
duration: 4.0 s
total enemy ActionCoefficient: 5.50 magic
total ally HealCoefficient: 0.30
PoiseCoefficient total: 3.50
```

Enemies remain fully targetable; this is not an invulnerability cinematic.

---

# 9. Mage / 마도사

## 9.1 Root identity

Combat identity:

- offensive magic;
- area/control options;
- active positioning;
- variety rewarded through spell sequencing;
- later branch into elemental interactions or arcane shaping/control.

Suggested stats:

- INT;
- WIL;
- END/VIT as needed.

## 9.2 Root mechanic — Arcane Weave

Arcane Weave tracks **different Mage active IDs** used within a short sequence.

```text
Weave sigils: 0..3 distinct skill IDs
sequence window: 8 s
```

Rules:

- casting a Mage active whose ID is not already in the current sequence adds one sigil;
- casting a duplicate skill refreshes the sequence timer but does not add another sigil;
- at 3 distinct sigils, gain `Weave Ready` for 8 s and clear stored IDs;
- next Mage active tagged `Weave consumer` consumes Weave Ready and applies that skill's weave effect;
- Phase Step participates as a distinct skill cast but deals no damage by itself.

No extra resource is spent; the mechanic rewards varied spell use.

## 9.3 Root passive — Arcane Memory

While Weave sequence is at 2 sigils:

- Mage movement speed +5%;
- taking direct HP damage does not clear the sequence but shortens remaining sequence time by 2 s.

## 9.4 Active 1 — Arc Bolt

```text
cost: 12 Mana
cooldown: 3.0 s
range: 22 blocks
ActionCoefficient: 1.20 magic
PoiseCoefficient: 0.50
Weave consumer: yes
```

Weave effect:

- primary hit unchanged;
- forks to up to 2 nearby enemies within 4 blocks for 0.50 coefficient each.

## 9.5 Active 2 — Phase Step

```text
cost: 16 Mana
cooldown: 8.0 s
teleport distance: up to 5.0 blocks
server path/collision validation: required
invulnerability: 2 ticks / 0.10 s during accepted displacement only
Weave consumer: yes
```

Rules:

- cannot teleport through solid walls;
- if destination is invalid, use nearest safe position along path or reject without cost;
- no vertical teleport beyond ordinary one-block traversal unless a specialization explicitly changes it.

Weave effect:

- origin leaves a 2.5-block arcane field for 3 s;
- enemies inside: -25% movement; bosses -10%;
- field deals no damage.

## 9.6 Active 3 — Frost Ring

```text
cost: 25 Mana
cooldown: 10.0 s
radius: 4.2 blocks
ActionCoefficient: 1.45 magic
PoiseCoefficient: 1.20
Weave consumer: yes
```

Applies `Chilled`:

- common/elite movement -35% for 3.5 s;
- miniboss -20% for 2.5 s;
- boss -15% for 2.0 s;
- ordinary attack/cast-speed penalties are not added at root baseline.

Weave effect:

- caster receives `BarrierCoefficient 0.10` using Mage's BarrierReference.

## 9.7 Active 4 — Flame Burst

```text
cost: 30 Mana
cooldown: 12.0 s
ground-target radius: 3.5 blocks
duration: 2.5 s
total direct ActionCoefficient: 2.30 magic
PoiseCoefficient: 1.00
Weave consumer: yes
```

Applies `Burning`:

- additional total ActionCoefficient 0.40 over 4 s;
- Burning cannot crit;
- Burning is magic/fire and uses normal Magic Resistance;
- reapplication refreshes duration and keeps the stronger existing per-tick magnitude rather than infinitely stacking.

Weave effect:

- direct coefficient increases to 2.60;
- Burning total coefficient increases to 0.50.

## 9.8 Root ultimate — Astral Convergence

```text
cast: 1.0 s
radius: 6.0 blocks
duration: 5.0 s
total ActionCoefficient per target: 5.80 magic
PoiseCoefficient total: 4.00
```

Each pulse:

- normal enemies may be pulled up to 0.4 blocks toward center;
- elites pull at half that strength;
- miniboss/boss position is not forcibly moved unless encounter data allows it.

If Weave Ready existed before ultimate activation:

- it is consumed;
- final detonation gains +15% direct damage and +25% poise damage.

### Mage ultimate-charge events

- Mage damaging active hits primary eligible target: +3;
- each additional enemy hit by same cast: +0.5, max +3 additional per cast;
- completing a 3-sigil Arcane Weave: +6;
- applying meaningful root/slow/control to an eligible elite/boss for the first time in 4 s: +2.

---

# 10. Mage first specialization branches

## 10.1 Elementalist / 원소술사 계열

Identity: Fire/Frost/Lightning alternation, elemental combinations and area pressure.

### Mechanic change — Elemental Sequence

Arcane Weave becomes a per-target elemental sequence.

- Elementalist damaging spells carry `fire`, `frost` or `lightning` element tags;
- hitting the same eligible target with **three different elements within 6 s** triggers `Elemental Break`;
- target-specific ICD: 10 s.

Elemental Break:

```text
ActionCoefficient: 1.20 magic
PoiseCoefficient: 2.00
```

It is a visible tri-element burst and cannot recursively trigger itself.

### Passive — Elemental Momentum

Triggering Elemental Break:

- restores 15 Mana;
- grants +10% movement speed for 4 s.

### Branch active — Chain Lightning

```text
cost: 15 Mana
cooldown: 4.5 s
primary ActionCoefficient: 1.35
up to 2 nearby additional targets: 0.55 each
primary PoiseCoefficient: 0.70
element: lightning
```

One enemy cannot be hit twice by the same chain.

### Branch active — Glacial Wave

```text
cost: 26 Mana
cooldown: 10.0 s
7-block frontal cone
ActionCoefficient: 1.80
PoiseCoefficient: 1.50
element: frost
```

Chilled:

- normal/elite -40% movement for 3.5 s;
- miniboss -22% for 2.5 s;
- boss -15% for 2 s.

### Branch ultimate — Primal Tempest

```text
radius: 7 blocks
duration: 6 s
total ActionCoefficient: 6.60
total PoiseCoefficient: 4.50
```

Pulse order visibly rotates Fire → Frost → Lightning.
The same target may trigger at most one Elemental Break from Primal Tempest itself.

## 10.2 Arcanist / 비전술사 계열

Identity: teleportation, binding, projectile shaping and controlled spell economy.

### Mechanic change — Deep Weave

Arcane Weave remains, but Weave Ready changes:

- next Mage/Arcanist ordinary active receives +20% ActionCoefficient where it deals direct damage;
- its Mana cost is reduced by 25%;
- non-damaging actions instead receive their listed stronger Weave utility;
- Weave Ready lasts 10 s.

### Passive — Arcane Reserve

Completing a 3-sigil weave restores **12 Mana**.

### Branch active — Binding Prism

```text
cost: 24 Mana
cooldown: 11.0 s
radius: 4.0 blocks
ActionCoefficient: 1.40
PoiseCoefficient: 1.20
```

Control:

- common: rooted 1.5 s;
- elite: rooted 0.75 s;
- miniboss: -25% movement for 2 s;
- boss: -20% movement for 2 s;
- root immunity/recent-control protection may shorten repeated roots but must be explicit in enemy status data.

### Branch active — Arcane Orbit

```text
cost: 30 Mana
cooldown: 14.0 s
duration: 8.0 s
orbiting projectiles: 3
```

Each time the caster successfully uses a non-ultimate Mage/Arcanist active while at least one orb remains:

- one orb launches at nearest valid hostile within 12 blocks;
- orb ActionCoefficient 0.75;
- orb PoiseCoefficient 0.35;
- maximum one orb launches per skill cast;
- orbs cannot generate Weave sigils or Ultimate Charge themselves.

### Branch ultimate — Singularity

```text
radius: 6 blocks
duration: 5 s
total ActionCoefficient: 5.80
total PoiseCoefficient: 5.00
```

Control:

- common enemies pulled 0.6 blocks per pulse toward center;
- elites 0.3;
- bosses not displaced unless encounter explicitly permits it.

After detonation:

- caster receives BarrierCoefficient 0.15.

---

# 11. Guardian / 수호자

## 11.1 Root identity

Combat identity:

- defense;
- ally protection;
- threat/space control;
- counters and attack denial;
- stronger when reading incoming attacks, not by passive invulnerability.

Suggested stats:

- END;
- VIT;
- STR/WIL depending on offensive/support build.

## 11.2 Root mechanic — Resolve

```text
Resolve: 0..3 pips
```

Gain:

- successful perfect guard: +1;
- ordinary guarded hit whose final Stamina cost is >=18: +1, ICD 2 s;
- Guardian barrier actually absorbs >=8% recipient MaxHP from hostile damage: +1, per-recipient ICD 4 s.

Rules:

- Resolve expires after 10 s fully outside combat;
- Resolve is not consumed by ordinary guard;
- `Aegis Field` consumes available Resolve to scale its barrier.

## 11.3 Root passive — Stand Firm

While actively guarding:

- PlayerPoiseMax +25%;
- incoming knockback from ordinary non-launch attacks -30%;
- does not stop guard break or explicitly unguardable launch/grab mechanics.

## 11.4 Active 1 — Bulwark Rush

```text
requirement: guard_capable setup
cost: 20 Stamina
cooldown: 8.0 s
forward movement: up to 3.8 blocks
ActionCoefficient: 1.45 physical
PoiseCoefficient: 1.80
```

During the committed rush:

- frontal incoming guardable damage receives 50% of current guard setup's normal absorption;
- this is not a perfect guard and cannot reflect projectiles;
- collision with first substantial enemy ends forward movement and performs the impact.

## 11.5 Active 2 — Warding Strike

```text
cost: 20 Mana
cooldown: 7.0 s
ActionCoefficient: 1.60 physical/magic according to equipped skill profile
PoiseCoefficient: 1.40
range: melee / 3.5 block arc
```

Applies `Provoked` for 5 s to AI-controlled targets:

- common/elite target weighting toward Guardian x4;
- miniboss/boss weighting x2;
- encounter-script target locks/mechanics may override;
- Provoked has no PvP mind-control behavior.

## 11.6 Active 3 — Aegis Field

```text
cost: 30 Mana
cooldown: 15.0 s
radius: 5.0 blocks
base BarrierCoefficient: 0.18
Resolve consumed: all available
```

Per Resolve consumed:

- +0.04 BarrierCoefficient.

Therefore at 3 Resolve:

```text
BarrierCoefficient = 0.30
```

Barrier duration follows shared 6 s baseline.

## 11.7 Active 4 — Counterwall

```text
cost: 22 Stamina
cooldown: 12.0 s
stance duration: 15 ticks / 0.75 s
```

First valid `perfect_guardable` hit during stance:

- resolves as perfect guard;
- releases a 4-block frontal counter wave;
- ActionCoefficient 1.70;
- PoiseCoefficient 2.20;
- successful trigger grants +2 Resolve.

No valid hit:

- 0.55 s recovery.

Counterwall is easier/longer than the universal perfect-guard tap, but costs a skill slot/cooldown and is not permanently available.

## 11.8 Root ultimate — Unbroken Line

Duration: 8 s, 7-block moving aura centered on Guardian.

On activation:

- self/allies receive BarrierCoefficient 0.22.

While inside aura:

- allies/self ordinary damage taken -20%;
- guard Stamina cost -20%;
- Guardian effective player poise x1.75;
- every 2 s, engaged AI targets inside receive a Provoked refresh toward Guardian.

Mitigation caps still apply. This ultimate does not make anyone invulnerable.

### Guardian ultimate-charge events

- ordinary guarded hit with final Stamina cost >=12: +2;
- successful perfect guard: +6;
- every 5% recipient MaxHP of Guardian barrier consumed by hostile damage: +2, max +6 per barrier source/recipient;
- hit from a currently Provoked target that the Guardian guards or receives: +2, ICD 2 s per target.

---

# 12. Guardian first specialization branches

## 12.1 Bastion / 보루 계열

Identity: barriers, ally mitigation, threat control and holding space.

### Mechanic change — Fortification

Resolve becomes `Fortification`, max 3.

Gain rules remain, but:

- each Fortification stack increases Guardian barrier output +5%;
- each stack reduces guard Stamina cost by 3%;
- these bonuses disappear when stacks are consumed;
- `Citadel Field` consumes all Fortification.

### Passive — Fortified Presence

While Fortification > 0:

- PlayerPoiseMax +15 flat;
- AI threat generated by Guardian damage/guard actions +50%.

### Branch active — Bulwark Beacon

A visible external-first standard/ward prop is placed on the ground.

```text
cost: 28 Mana
cooldown: 16.0 s
radius: 6.0 blocks
duration: 8.0 s
maximum active: 1
```

Effects:

- allies inside: 10% ordinary damage reduction;
- engaged AI enemies inside: threat weighting toward Guardian x4;
- spending 1 Fortification on cast extends duration to 10 s;
- Beacon does not block movement/collision.

### Branch active — Citadel Field

```text
cost: 34 Mana
cooldown: 18.0 s
radius: 6.0 blocks
base BarrierCoefficient: 0.25
Fortification consumed: all
```

Each consumed stack:

- +0.05 BarrierCoefficient.

Max at 3 stacks:

```text
BarrierCoefficient = 0.40
```

### Branch ultimate — Citadel

Creates an anchored 7-block field for 8 s.

On activation:

- BarrierCoefficient 0.30 to self/allies.

Inside field:

- 25% ordinary damage reduction;
- Guardian threat generation +100%;
- Guardian effective player poise x2.0;
- allies receive 20% guard-Stamina-cost reduction.

The field does not follow the Guardian. Positioning is the tradeoff versus root Unbroken Line.

## 12.2 Sentinel / 파수꾼 계열

Identity: perfect-guard mastery, retaliation, attack denial and battlefield interruption. Sentinel protects mainly by **stopping attacks**, whereas Armsmaster converts precision into personal weapon offense.

### Mechanic change — Watch

Resolve becomes `Watch`, max 3.

Gain:

- universal perfect guard: +1;
- Counterwall/Interdict success: +1;
- ordinary block does not generate Watch.

Each Watch stack:

- +10% perfect-guard poise damage;
- +5% Sentinel counter-skill direct damage.

Watch expires after 8 s without a perfect guard/counter success.

### Passive — Read the Blow

Successful perfect guard:

- restore 12 Stamina;
- reduce remaining cooldown of Sentinel branch actives by 1.0 s;
- ICD 1.0 s.

### Branch active — Interdict

```text
cost: 18 Stamina
cooldown: 9.0 s
stance: 9 ticks / 0.45 s
counter ActionCoefficient: 2.00
PoiseCoefficient: 2.80
```

On first valid hit:

- resolves as perfect guard;
- reflect projectile only if that projectile is explicitly `projectile_reflectable`;
- emits visible counter wave matching 4-block frontal area.

### Branch active — Lockdown Strike

```text
cost: 20 Mana
cooldown: 8.0 s
ActionCoefficient: 1.80
PoiseCoefficient: 2.40
```

If target is currently in an authored attack-recovery state within 0.8 s after a committed/heavy attack:

- PoiseCoefficient becomes 3.60;
- normal enemies receive strong interruption;
- bosses still follow poise system, never arbitrary hard stun immunity bypass.

### Branch ultimate — No Passage

Duration: 6 s.

While active:

- universal perfect-guard window becomes 6 ticks / 0.30 s;
- ordinary guard physical/magic absorption each gain +5 percentage points, still respecting mitigation caps;
- every successful perfect guard emits a 4-block retaliation wave:
  - ActionCoefficient 1.00;
  - PoiseCoefficient 1.50;
  - per-target wave ICD 1.0 s;
- player must still press/release guard; this is not automatic invulnerability.

---

# 13. Cross-class status definitions introduced by these kits

This section closes only statuses required by the current root/first-branch class kits. Later world/enemy status design may add more without redefining these silently.

## Snared

- movement-speed reduction with values/durations authored by source skill;
- no forced root on bosses unless explicit encounter rule allows it;
- strongest active Snared magnitude wins; duration refreshes.

## Chilled

- movement-speed reduction only at this stage;
- no universal attack-speed/cast-speed penalty unless a later authored system adds it;
- strongest magnitude wins; duration refreshes.

## Burning

- timed magic/fire damage;
- does not crit;
- normal Magic Resistance applies;
- stronger existing Burning remains when weaker Burning is reapplied; duration refreshes;
- no infinite stack count.

## Fractured Armor

- reduces EffectiveDefense by the authored percentage;
- same source does not stack with itself;
- strongest magnitude wins, duration refreshes.

## Rebuked

- reduces outgoing direct damage by authored percentage;
- does not reduce environmental/self-inflicted damage;
- strongest magnitude wins.

## Judged

- Inquisitor class mark;
- one Inquisitor per source player may maintain its own mark on a target;
- Sentence consumes only the caster's own Judged mark.

## Provoked

- AI threat-weight modifier only;
- encounter target-lock/script mechanics can override;
- no PvP control.

---

# 14. Class UI/HUD requirements

All class HUD components use the existing Lucifer-family design language.

Required elements:

- 4 active slots + 1 ultimate slot;
- cooldown wipe/radial mask;
- Mana/Stamina cost failure feedback without giant text spam;
- Ultimate Gauge;
- one compact class mechanic widget:
  - Warrior Momentum/Technique/Onslaught pips;
  - Hunter Focus pips + current Quarry indicator;
  - Cleric Grace/Benediction/Judgment pips;
  - Mage Weave sigils / Weave Ready;
  - Guardian Resolve/Fortification/Watch pips.

Rules:

- class mechanic widget belongs near skills/resources, not in a separate large panel;
- no extra MMO action bar for specialization mechanics;
- Quarry/Judged/Provoked target markers are restrained and world-readable;
- important empowered-ready state changes icon/frame treatment, not a giant central popup every rotation.

---

# 15. Skill icon / animation / VFX admission rules

Before a skill is considered asset-ready:

1. select exact external animation/reference source;
2. select exact icon source file/author or a model-render-derived icon where appropriate;
3. select exact VFX sprite/geometry/reference family;
4. verify source license/adoption mode;
5. prototype at Minecraft scale;
6. verify visible range against server hitbox;
7. verify GUI icon remains readable at normal Minecraft GUI scales;
8. record source binding.

No production skill may ship with:

- `todo_icon`;
- generic enchanted-glint-only effect;
- a vanilla particle burst standing in for an important spell;
- an animation whose weapon never reaches the actual hit frame;
- a copied ARR RPG-Series asset committed into the public repository.

---

# 16. Recommended external binding by class

## Warrior

Animation direction:

- Better Combat heavy/balanced melee sequences;
- Quaternius UAL2 armed 3/4-hit combos, heavy strike and movement actions.

VFX:

- Kenney sparks/impact sprites;
- CC0 ground crack/ring effect textures for Breaker/Earthshatter;
- weapon trail geometry/sprite aligned to actual blade/weapon path.

RPG Series reference:

- Rogues & Warriors Warrior Codex's weakening/smashing identity only as behavior/presentation reference.

## Hunter

Animation direction:

- Ranged Weapon API draw/release behavior;
- Archers RPG Series as reference for active archery presentation;
- external dodge/parkour movement from UAL2 for Quickstep/Deadeye Reposition.

VFX:

- actual arrow/projectile models where possible;
- restrained trail sprites;
- Skyfall uses authored projectile/rain representation rather than generic particles.

## Cleric

Animation direction:

- Paladins & Priests reference for readable healing/protection gestures;
- UAL2 casting/armed stance references where suitable.

VFX:

- Game-icons holy/shield/cross/radiance icon families;
- Kenney/OpenGameArt rings, light bursts, shield planes;
- ground decals/rune geometry sized to actual zone radius.

## Mage

Animation direction:

- Wizards RPG Series Arcane/Fire/Frost presentation reference;
- Spell Engine delivery primitives;
- UAL2 casting motion where it can be cleanly retargeted.

VFX:

- Kenney electricity/magic/fire sprites;
- OpenGameArt CC0 fire/spell/animated-particle sheets;
- actual projectile meshes/planes for Arc Bolt/Chain Lightning/Arcane Orbit when cleaner than particles.

## Guardian

Animation direction:

- Better Combat shield/weapon handling;
- UAL2 shield/impact/armed motion reference;
- Horse/weapon combat mods are not required for this subsystem.

VFX:

- external shield/ring/light impact sprites;
- Quaternius Fantasy Props standard/banner family for Bulwark Beacon if the exact model passes Minecraft-scale review;
- visible counter arcs matched to the 4-block counter region.

---

# 17. Data schema

Suggested root data ownership:

```text
classes/
  warrior.json
  hunter.json
  cleric.json
  mage.json
  guardian.json

skills/
  warrior/*.json
  hunter/*.json
  cleric/*.json
  mage/*.json
  guardian/*.json

specializations/
  warrior_vanguard.json
  warrior_armsmaster.json
  hunter_ranger.json
  hunter_marksman.json
  cleric_saint.json
  cleric_inquisitor.json
  mage_elementalist.json
  mage_arcanist.json
  guardian_bastion.json
  guardian_sentinel.json

class_statuses/*.json
assets/source_bindings/classes/*.json
```

## 17.1 Class definition minimum

```text
id
player_facing_name
root_mechanic_id
root_passive_id
starting_active_ids[4]
starting_ultimate_id
ultimate_charge_rules
suggested_stats
specialization_ids[]
ui_mechanic_widget
```

## 17.2 Skill definition minimum

```text
id
class_id
skill_type: active | ultimate
resource_type
resource_cost
cooldown_ticks
cast_ticks
active_ticks
recovery_ticks
action_coefficient
heal_coefficient
barrier_coefficient
poise_coefficient
offensive_stat_weights
range_profile
hitbox_profile
movement_profile
dodge_cancel_start
weapon_behavior_requirements[]
status_applications[]
class_mechanic_gain_or_spend
ultimate_charge_gain_rules
animation_binding
vfx_binding
sound_binding
icon_binding
server_authority_flags
```

Specialization modifiers are explicit data; do not duplicate whole root classes in Java conditionals.

---

# 18. First implementation acceptance targets

When class implementation begins, validate a meaningful slice rather than all five at once.

## Warrior acceptance

- Momentum gain/expiry/empower works server-authoritatively;
- Iron Counter accepts only valid guardable/perfect-guardable attacks;
- Breaker visibly produces much more poise pressure than dagger spam;
- Earthshatter VFX footprint equals real ground hit area.

## Hunter acceptance

- Quarry swaps/refreshes correctly;
- Focus from distance/weak point cannot double-fire beyond ICD;
- Quickstep has zero hidden i-frames;
- Fan of Arrows same-target hit cap prevents point-blank multiplicative abuse;
- Final Shot is a real projectile/line-of-sight action, not a wall-penetrating scripted hit.

## Cleric acceptance

- effective healing/barrier absorption, not raw cast spam, generates Grace/ultimate;
- healing formula scales sanely from early to Lv80 benchmark HP;
- Sanctuary radius matches visuals;
- Inquisitor damage-heal cap prevents multi-hit abuse.

## Mage acceptance

- duplicate spell casts do not add Arcane-Weave sigils;
- Phase Step cannot cross walls or invalid spaces;
- Burning refresh/strongest-value rule prevents infinite stacking;
- Elemental Break target ICD works;
- Singularity cannot drag bosses that encounter data marks immovable.

## Guardian acceptance

- Resolve generation uses actual guard/barrier events;
- Counterwall does not counter unguardable attacks;
- Provoked changes AI preference but cannot override scripted boss mechanics;
- Aegis/Fortification barrier caps are respected;
- Sentinel perfect-guard ultimate still requires player timing.

## Multiplayer acceptance

For every class before claiming multiplayer success:

- resource/class mechanic state is authoritative on server;
- personal target marks do not leak/overwrite another player's marks;
- one player's barrier/heal attribution cannot give duplicate charge to every support player;
- projectile/hit VFX do not produce duplicate server damage;
- Essential-hosted remote play eventually verifies class mechanic latency/feedback.

---

# 19. What this pass closes

Closed for implementation in this root/first-specialization document:

- all five root-class starting combat identities;
- exact root class mechanic for Warrior/Hunter/Cleric/Mage/Guardian;
- exact four root actives per class;
- exact root ultimate per class;
- root passive behavior;
- Mana/Stamina costs;
- cooldowns;
- damage/heal/barrier/poise coefficients;
- relevant movement/control timing;
- class-specific Ultimate Charge sources;
- first two specialization branches for every root class;
- each branch's mechanic change;
- two branch actives;
- branch passive;
- branch ultimate;
- minimal status definitions required by these kits;
- external animation/VFX/icon source families and adoption rules;
- data schema and first implementation acceptance criteria.

Closed by later dedicated canon and therefore **not future design work here**:

- Rank 20/32/44/50 advancement stages and milestone rules — `CLASS_PROGRESSION.md`;
- passive-tree layouts, Passive Point economy, World Insight points and respec — `CLASS_PROGRESSION.md`;
- Class Rank / Class XP / catch-up / branch-switch progression and unlock conditions — `CLASS_PROGRESSION.md`;
- world-discovered optional skills and Hidden Techniques — `CLASS_PROGRESSION.md`;
- frequent-action keybind map and input-conflict policy — `ACCESSIBILITY_DIFFICULTY_INPUT_AUDIO.md`.

Still separate production/content work:

- global enemy/world status roster beyond the statuses explicitly required here, only where later dedicated status/encounter canon has not already closed the interaction;
- exact external animation/VFX/icon/SFX filenames, hashes, provenance and Minecraft-scale bindings during asset intake;
- runtime tuning of timings/costs/coefficients when real play demonstrates a feel or balance problem;
- actual client and multiplayer validation.

Those remaining production tasks are not permission to redesign the root kits during coding. Implementation must use this file together with `CLASS_PROGRESSION.md` as the class-combat baseline.