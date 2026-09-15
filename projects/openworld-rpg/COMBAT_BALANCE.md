# Open-World RPG — Combat Math / Timing / Feel Canon

> Status: **DESIGN CANON — combat balance baseline locked before class-kit implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Equipment math: `EQUIPMENT_BALANCE.md`  
> Technical runtime boundaries: `M0_DEPENDENCY_AUDIT.md`  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins. This file closes combat numbers that must not be invented during coding.

This pass was written only after re-reading the current master design. The intended combat is **fast open-world action RPG combat with readable defense, skills, stagger and bosses**. It is not a slow Souls clone, a long Monster Hunter hunt simulator, vanilla Minecraft combat with extra particles, or an MMO HP-sponge game.

Core feel target:

```text
read attack
→ move / dodge / guard / perfect-guard
→ punish with basic attacks or skills
→ build damage + poise pressure
→ create a stagger window
→ spend resources intelligently
→ disengage/recover when needed
→ re-enter
```

Normal fights are short. Elites create a meaningful exchange. Bosses are long enough to learn patterns but not long enough to feel like health-bar labor.

---

# 1. External benchmarks and what is adopted

External games/mods are balance references, not number packs to copy blindly.

## Guild Wars 2

Reference facts:

- baseline endurance is 100;
- a normal dodge costs 50 endurance;
- the dodge/evade lasts about 0.75 s and also repositions the character;
- its damage model uses weapon strength, offensive power, skill coefficients and target armor.

Adopted principle:

- dodge is a real resource decision rather than free spam;
- skill coefficients are explicit data;
- defensive stats must meaningfully reduce strike damage without replacing active defense.

Not adopted:

- the project does not use a 0.75 s universal evade window; that would be too forgiving for this faster Minecraft action model.

## Elden Ring / Souls precedent

Reference facts:

- Elden Ring light/medium roll has a substantially larger invulnerability window than Monster Hunter World;
- current community frame documentation lists many parries around roughly 0.17–0.20 s of active parry time, with startup/recovery surrounding it;
- enemy stance/poise rewards sustained pressure and heavy/high-stance attacks, then recovers if the player stops applying pressure;
- Dark Souls III uses a visible stamina pool where roll cost is small enough to permit several actions, with rapid regeneration once recovery begins.

Adopted principle:

- dodge should be dependable but not a near-second-long safety blanket;
- perfect guard/parry should have a short readable timing window;
- poise pressure should reward commitment and consistent pressure;
- running out of Stamina should matter, while recovery should remain responsive.

Not adopted:

- no universal slow heavy attack input;
- no extremely long routine animation commitment on ordinary weapons;
- no hidden boss stance mechanic that gives the player no readable feedback at all.

## Monster Hunter World

Reference facts:

- a normal dodge has roughly 13 i-frames at 60 FPS (~0.22 s), much shorter than a typical Souls roll;
- practical defense relies heavily on positioning, weapon behavior and getting outside lingering hitboxes rather than simply phasing through every attack.

Adopted principle:

- the dodge should physically move the player far enough that position matters after the i-frame ends;
- large lingering attacks should still reward dodging *out of* danger rather than only timing one invulnerable instant.

Not adopted:

- ordinary encounters and bosses are not tuned to Monster Hunter-length hunts.

## Better Combat

Reference facts from the current 26.2 branch:

- attacks expose explicit movement-speed, range, angle and upswing controls;
- its animation guide recommends a readable preparation → tipping point/hit → recovery structure;
- attack animation timing is scaled to weapon cooldown.

Adopted principle:

- visible tipping point and actual hit frame must align;
- each weapon family receives a deliberate movement/commitment profile rather than only different DPS.

## AcroWield

Current 26.2 Fabric releases provide dodge, just-guard/parry and configurable dash velocity. It remains reference/code-candidate material, not the project's combat authority.

Adopted principle:

- Minecraft combat benefits from directional dash distance and just-guard rather than only vanilla shield behavior.

The project keeps one server-authoritative `CombatState` as locked by `M0_DEPENDENCY_AUDIT.md`.

---

# 2. Time unit / tick contract

Minecraft server simulation is treated as 20 ticks/s for authoritative combat timing.

```text
1 tick = 0.05 s
```

Canonical windows are authored in ticks first and displayed below with seconds for readability.

Rules:

- damage state, invulnerability, guard state, poise, costs and cooldown acceptance are server-authoritative;
- the client may predict local animation/movement feedback, but prediction never authorizes damage, resource gain, item use or skill success;
- visible animation hit timing should align with server hit timing within **±1 tick**;
- do not hide large network disagreement by silently expanding attack hitboxes.

---

# 3. Primary-stat allocation baseline

The master canon already locks `VIT / END / STR / DEX / INT / WIL`. This pass closes how ordinary player-Lv stat points work.

## 3.1 Base and earned points

At Lv 1:

```text
VIT = 5
END = 5
STR = 5
DEX = 5
INT = 5
WIL = 5
```

Every combat Lv gained after Lv 1 grants **1 Attribute Point**.

```text
EarnedAttributePoints(L) = L - 1
```

Therefore Lv 80 has 79 earned points.

Player-allocated value before equipment is capped at **60 in one stat**. Equipment, Mythics, temporary buffs and authored class effects may push effective value above 60.

## 3.2 Per-class saved allocation

Attribute allocation is saved per root class, like that class's passive setup.

Why:

- class switching is already a supported experimentation system;
- a Mage → Warrior switch should not require an immediate second Gold tax just to make STR usable;
- each class still starts its own class progression/history from its proper state; receiving access to the global Lv-earned attribute pool does not grant its missing skills/advancements.

Switching back restores that class's previous attribute allocation.

Baseline attribute respec at an appropriate shrine/trainer:

```text
AttributeRespecCost(L) = round_to_10(max(50, 0.40 * SwitchCost(L)))
```

No cooldown. Respec applies only to the selected class profile.

---

# 4. Shared offensive attribute curve

Weapons and damaging skills use weighted primary stats rather than raw class locks.

For weighted offensive stat `S`:

```text
x = max(0, S - 5)

AttributeDamageMultiplier(S) =
  1
  + 0.012 * min(x, 25)
  + 0.008 * min(max(x - 25, 0), 30)
  + 0.004 * max(x - 55, 0)
```

If a debuff somehow lowers weighted `S` below 5, the multiplier can fall below 1 but is floored at **0.70**.

Reference:

| Weighted stat | Damage multiplier |
|---:|---:|
| 5 | 1.00x |
| 20 | 1.18x |
| 30 | 1.30x |
| 45 | 1.42x |
| 60 | 1.54x |
| 80 | 1.62x |
| 100 | 1.70x |

This gives visible stat growth without allowing one stat to multiply endgame damage by 5–10x on top of Item Lv, affixes, skills and class effects.

## 4.1 Default weapon stat weights

| Weapon family | Weighted offensive stat |
|---|---|
| dagger | `0.15 STR + 0.85 DEX` |
| dual blades | `0.25 STR + 0.75 DEX` |
| sword | `0.50 STR + 0.50 DEX` |
| greatsword | `0.80 STR + 0.20 DEX` |
| spear / polearm | `0.40 STR + 0.60 DEX` |
| axe | `0.75 STR + 0.25 DEX` |
| hammer / mace | `0.90 STR + 0.10 DEX` |
| bow | `0.10 STR + 0.90 DEX` |
| crossbow | `0.25 STR + 0.75 DEX` |
| black-powder pistol | `0.20 STR + 0.80 DEX` |
| musket / hand cannon | `0.35 STR + 0.65 DEX` |
| staff | `0.85 INT + 0.15 WIL` |
| wand + focus | `0.75 INT + 0.25 WIL` |

A class skill may define a different weight when its fantasy genuinely requires it, e.g. WIL-heavy healing/holy skills or END-influenced Guardian techniques. That override is authored in skill data and never inferred from class name at runtime.

---

# 5. Player HP / VIT

Base player HP follows the same intentionally-flat level scale already used by equipment:

```text
BaseHP(L) = round(100 * GearScale(L))
```

VIT multiplier:

```text
x = max(0, VIT - 5)

VITMultiplier =
  1
  + 0.018 * min(x, 25)
  + 0.010 * min(max(x - 25, 0), 30)
  + 0.005 * max(x - 55, 0)

MaxHP = round(BaseHP(L) * VITMultiplier * (1 + summed_MaxHP_percent_bonus))
```

Reference before `% Max HP` gear/passives:

| Lv / VIT example | Approx. Max HP |
|---|---:|
| Lv1 / VIT5 | 100 |
| Lv8 / VIT7 | 143 |
| Lv20 / VIT10 | 223 |
| Lv44 / VIT16 | 403 |
| Lv64 / VIT21 | 575 |
| Lv80 / VIT25 | 727 |
| Lv80 / VIT60 | ~935 |

The middle examples are benchmark-build values used later for enemy-damage authoring, not forced player stat allocations.

## Natural HP recovery

- no baseline passive HP regeneration while in combat;
- after **8 s** without dealing/taking hostile damage, recover **0.30% MaxHP/s**;
- natural recovery is deliberately slow; potions, food, healing skills, camps, inns and shrines remain meaningful;
- explicit class/item effects may alter this.

---

# 6. Stamina / END

Stamina exists for active defense/movement, not for ordinary basic attacks.

For `x = max(0, END - 5)`:

```text
MaxStamina = round(
  100
  + 1.2 * min(x, 25)
  + 0.8 * min(max(x - 25, 0), 30)
  + 0.4 * max(x - 55, 0)
)

BaseStaminaRegenPerSecond =
  24
  + 0.12 * min(x, 55)
  + 0.05 * max(x - 55, 0)
```

Then apply Stamina Recovery equipment/passive bonuses.

Reference:

| END | Max Stamina | Base regen/s |
|---:|---:|---:|
| 5 | 100 | 24.0 |
| 30 | 130 | 27.0 |
| 60 | 154 | 30.6 |
| 80 | 162 | 31.6 |

## 6.1 Baseline action costs

- dodge: **30 Stamina**;
- sprint: **5 Stamina/s** while actively sprinting;
- ordinary jump: no Stamina cost at baseline;
- ordinary basic attacks: **0 Stamina** as already locked;
- guard: no drain merely for holding guard; incoming impact costs Stamina;
- parry/perfect-guard attempt: no arbitrary upfront fee; successful/failed contact uses guard-impact rules;
- rare martial skills may use Stamina instead of Mana and define their own cost, usually 18–40.

A baseline character can perform three consecutive full-cost dodges from full Stamina but cannot endlessly chain them.

## 6.2 Stamina regeneration delays

After the listed event, regeneration waits:

| Event | Regen delay |
|---|---:|
| stop sprinting | 7 ticks / 0.35 s |
| successful perfect guard | 7 ticks / 0.35 s |
| dodge | 12 ticks / 0.60 s |
| ordinary blocked impact | 15 ticks / 0.75 s |
| guard break | 22 ticks / 1.10 s |

Taking ordinary HP damage by itself does not endlessly reset Stamina recovery unless the attack also causes a specific stagger/guard state.

---

# 7. Mana / WIL

For `x = max(0, WIL - 5)`:

```text
MaxMana = round(
  100
  + 2.5 * min(x, 25)
  + 1.5 * min(max(x - 25, 0), 30)
  + 0.75 * max(x - 55, 0)
)

BaseManaRegenPerSecond = 4.0 + 0.04 * x
```

Apply Mana Recovery gear/passive bonuses afterwards.

Reference:

| WIL | Max Mana | Base regen/s |
|---:|---:|---:|
| 5 | 100 | 4.0 |
| 30 | 163 | 5.0 |
| 60 | 208 | 6.2 |
| 80 | 223 | 7.0 |

Rules:

- spending Mana pauses natural Mana regeneration for **20 ticks / 1.0 s**;
- most normal active skills should fall roughly in the **15–35 Mana** band unless their kit specifically uses another rhythm;
- high-impact non-ultimate skills may reach roughly **35–50 Mana**;
- exact skill costs are authored during class-kit design;
- outside combat for 5 s, natural Mana regeneration is doubled so exploration does not become waiting at zero Mana;
- inns/camps/shrines restore resources much faster than natural recovery.

WIL also contributes to selected status/magical resilience through specific mechanics defined by the relevant status/class rather than a hidden universal damage-reduction multiplier.

---

# 8. Direct damage formula

`EQUIPMENT_BALANCE.md` remains authoritative for `WeaponPower`.

For a direct damaging action:

```text
WeightedStat = sum(primary_stat * authored_weight)
StatMult = AttributeDamageMultiplier(WeightedStat)

RawActionDamage = WeaponPower * ActionCoefficient * StatMult

AdditivePowerBucket =
  1
  + applicable Physical/Magic Power bonuses
  + applicable weapon-family bonuses
  + applicable authored element/status-output direct-damage bonuses

PreMitigationDamage = RawActionDamage * AdditivePowerBucket
```

Then apply, in order where applicable:

1. weak-point multiplier;
2. critical multiplier;
3. defender Defense or Magic Resistance formula;
4. authored `% damage taken` / `% damage reduction` effects;
5. final rounding.

Rules:

- bonuses in the same ordinary power bucket add together before multiplying; do not turn every affix into another multiplicative layer;
- named Mythic/skill mechanics may define an explicit separate multiplier when that is their actual mechanic;
- multi-hit skills divide their intended whole-action coefficient across visible hits; extra particle/hit events do not create free DPS;
- client visuals never determine whether damage happened.

## 8.1 ActionCoefficient

Typical starting interpretation:

- quick/basic hit component: ~0.6–1.0;
- ordinary skill total: ~1.2–2.2;
- committed/high-impact skill total: ~2.2–3.5;
- ultimate total: class-specific and normally above ordinary-skill output or produces equivalent major utility/control.

These are calibration bands, not permission to assign every skill the same coefficient. Class-kit design must define each skill exactly.

---

# 9. Defense / Magic Resistance

Use a diminishing rational formula rather than flat subtraction or a linear percentage that can easily reach immunity.

For incoming attack/content source Lv `A`:

```text
MitigationK(A) = 75 * GearScale(A)

PhysicalDamageTakenMultiplier =
  MitigationK(A) / (MitigationK(A) + EffectiveDefense)

MagicDamageTakenMultiplier =
  MitigationK(A) / (MitigationK(A) + EffectiveMagicResistance)
```

The **attacker/content Lv**, not defender Lv, sets `MitigationK`. This naturally supports the open world:

- high-Lv armor strongly protects against low-Lv enemies;
- entering a much higher-Lv region in weak armor remains dangerous;
- same-Lv armor archetype relationships stay stable as Item Lv rises.

Equal-level full-set baseline reductions from the current equipment table are approximately:

| Armor archetype | Physical reduction | Magic reduction |
|---|---:|---:|
| Light | 16.7% | 21.9% |
| Medium | 25.0% | 14.8% |
| Heavy | 30.6% | 8.5% |

Defense/MR affixes modify effective values before the formula.

Normal Defense/MR mitigation is hard-capped at **70%**. Ordinary stacked percentage damage reduction after armor may not push total routine mitigation above **80%**. Explicit invulnerability, perfect guard or a deliberately authored immunity mechanic is separate from ordinary mitigation.

Hybrid attacks split physical/magical portions and mitigate each portion separately.

Slash/pierce/impact remain identity/poise/status hooks at baseline rather than adding three more mandatory armor-resistance spreadsheets. A special enemy/item may care about one type explicitly.

---

# 10. Critical hits and weak points

Baseline:

```text
BaseCritChance = 5%
BaseCritMultiplier = 1.50x
```

DEX contributes modest precision without turning every DEX build into automatic crit:

```text
DEXCritBonus = min(6 percentage points, max(0, DEX - 5) * 0.08 percentage points)
```

Then add gear/class/skill Critical Chance.

Caps for ordinary persistent states:

- normal total Critical Chance cap: **60%**;
- guaranteed-crit skills may explicitly override the cap for their own hit;
- normal total Critical Damage cap: **2.25x**;
- a specific authored skill/Mythic may exceed that only for its defined event.

Default authored weak-point multiplier when an enemy has a real readable weak point:

```text
WeakPointMultiplier = 1.25x
```

Not every enemy requires a weak point. Hitbox placement must correspond to visible anatomy/model structure.

Crit and weak-point multipliers may coexist, but a Marksman/class mechanic must not turn every routine shot into permanent 3–4x damage.

---

# 11. Dodge / roll

The baseline dodge intentionally sits between Monster Hunter World's very tight evade and Guild Wars 2 / Souls-style long safety windows.

## 11.1 Canonical baseline

- Stamina cost: **30**;
- total movement/animation duration: **9 ticks / 0.45 s**;
- invulnerability: first **6 ticks / 0.30 s** after the server accepts the dodge;
- horizontal travel target on level ground: **3.2 blocks**;
- next dodge cannot begin until **11 ticks / 0.55 s** after the previous dodge began;
- directional input determines direction; no input uses a short backward evade rather than random direction;
- rooted/hard-staggered/downed characters cannot dodge.

The final 3 ticks are movement/recovery without invulnerability, so bad positioning can still place the player inside a lingering hitbox when i-frames end.

## 11.2 Dodge-cancel commitment

Basic attacks cannot always be cancelled from frame 1.

Default earliest dodge-cancel point as fraction of the current attack animation:

| Family | Earliest dodge cancel |
|---|---:|
| dagger | 60% |
| dual blades | 60% |
| wand | 60% |
| sword | 70% |
| spear | 70% |
| staff | 70% |
| bow | 70% after release / skill-specific while drawing |
| axe | 78% |
| greatsword | 80% |
| hammer / mace | 82% |
| crossbow / musket / hand cannon | 80% after discharge; reload state separately authored |

Skills define their own `dodge_cancel_start`.

This preserves weapon weight without making the whole game slow.

---

# 12. Guard / perfect guard / parry

The baseline defensive model uses **hold to guard + an initial perfect-guard window**, not a separate universal parry skill button.

Specific weapon/class skills may still have dedicated counters.

## 12.1 Guard absorption

After ordinary Defense/MR mitigation, a successful held guard reduces the remaining damage by the guard family's absorption:

| Guard type | Physical absorption | Magic absorption |
|---|---:|---:|
| weapon guard where allowed | 55% | 35% |
| buckler | 70% | 45% |
| standard shield | 85% | 60% |
| heavy/tower shield | 95% | 70% |

Element-specific shields/items may author exceptions.

Blocking a status-applying attack reduces status buildup by the same absorption percentage unless that status explicitly bypasses guard.

## 12.2 Perfect-guard window

When guard is newly pressed:

- perfect-guard active window: **4 ticks / 0.20 s**;
- if held after that, it becomes ordinary guard;
- a new perfect window cannot start until **10 ticks / 0.50 s** after the previous perfect window began;
- releasing/re-pressing inside that lockout only returns to ordinary guard and cannot be mashed for permanent perfect guard.

A successful perfect guard against a parryable/deflectable hit:

- takes **0 direct HP damage** from that hit;
- takes **0 status buildup** from that hit;
- pays only 25% of the normal calculated guard Stamina cost, minimum 2;
- cannot itself cause a guard break;
- deals strong authored poise pressure to the attacker;
- produces distinct animation/sound/VFX feedback.

This is intentionally around the same order of difficulty as tight action-game parries (~0.15–0.20 s), but Minecraft networking and 20 TPS make a clean 4-tick window more robust.

## 12.3 Guard Stamina pressure

Each enemy attack defines one guard-pressure band:

| Band | Base guard pressure |
|---|---:|
| light | 24 |
| medium | 36 |
| heavy | 54 |
| crush | 78 |

For attack source Lv `A`:

```text
GuardScale = GuardRating / (GuardRating + 12 * GearScale(A))

ENDGuardBonus = min(0.15, max(0, END - 5) * 0.002)

GuardStaminaCost = max(
  3,
  BaseGuardPressure * (1 - GuardScale) * (1 - ENDGuardBonus)
)
```

At equal Item Lv this lets a standard/heavy shield block several ordinary attacks, while repeated heavy/crush impacts still threaten the Stamina bar.

## 12.4 Guard break

If an ordinary guard impact requires more Stamina than remains:

- Stamina becomes 0;
- that impact receives only **50% of the guard type's normal absorption**;
- player enters guard-break reaction for **17 ticks / 0.85 s**;
- guard/dodge cannot restart for the first **9 ticks / 0.45 s** of that reaction;
- Stamina regeneration follows the 1.10 s guard-break delay.

No infinite shield turtle should ignore boss pressure simply by holding right-click.

## 12.5 Parryability readability

Every authored enemy attack carries explicit tags:

```text
guardable: true/false
perfect_guardable: true/false
projectile_reflectable: true/false
```

- normal melee attacks are usually guardable/perfect-guardable;
- huge grabs, floor eruptions and selected magical/environmental attacks may be unguardable;
- unguardable attacks require a distinctive telegraph/VFX/sound language established in the enemy's design;
- projectile reflection is never granted globally just because perfect guard exists.

---

# 13. Enemy poise / stagger

The project uses a visible/readable poise system for elites/bosses rather than a purely hidden stance number.

## 13.1 Player attack poise damage

For a basic attack cycle:

```text
PoiseDamage = 10 * WeaponFamilyPoiseMultiplier * ActionPoiseCoefficient
```

The family multiplier is already locked in `EQUIPMENT_BALANCE.md`.

- basic-attack whole cycle: `ActionPoiseCoefficient = 1.0`;
- a multi-hit cycle divides that 1.0 budget across its visible hits;
- skills author their own value, normally ~0.5–3.5 depending on commitment/purpose;
- class/weapon mastery may modify poise output deliberately.

Poise damage does **not** scale from ordinary damage crits. Crit builds do not accidentally become the best stagger builds.

## 13.2 Baseline enemy poise bands

| Enemy role | Typical PoiseMax |
|---|---:|
| minor / fragile common | 16–22 |
| ordinary common | 24–35 |
| sturdy/heavy common | 36–50 |
| elite | 65–90 |
| miniboss | 95–130 |
| dungeon/field boss | 150–220 |
| exceptional giant/endgame boss | 220–300 |

Individual anatomy/mechanics may alter these values.

## 13.3 Poise recovery

- minor/common: if no poise damage for 2.0 s, rapidly reset to full; no persistent UI needed;
- elite: recovery begins after **4.0 s**, at **25% PoiseMax/s**;
- miniboss/boss: recovery begins after **6.0 s**, at **12.5% PoiseMax/s**.

Any new poise damage stops recovery and restarts the delay.

## 13.4 Break result

| Role | Break window |
|---|---:|
| common | 0.70 s strong stagger |
| elite | 1.50 s |
| miniboss | 2.00 s |
| boss | 2.40 s |

During elite/miniboss/boss break:

- direct damage taken: **+15%**;
- a model-specific exposed weak point may additionally become targetable, but no generic invisible crit prompt is required for every creature;
- after recovering, target gets **1.5 s** of 50% poise-damage reduction to prevent permanent chain breaks.

Boss poise UI appears once the player meaningfully damages poise and remains visible while engaged.

## 13.5 Perfect-guard poise reward

Against a perfect-guardable attack:

- common enemy: strong immediate stagger; fragile enemies may be fully interrupted;
- elite/miniboss: `8 + 20% of target PoiseMax` poise damage;
- boss: `8 + 15% of target PoiseMax` poise damage;
- an explicitly high-commitment parryable boss attack may have a `perfect_guard_poise_multiplier` up to about 1.5x.

Therefore a boss normally requires multiple successful defensive reads, not one lucky button press, to reach a break.

---

# 14. Player poise / hit interruption

Player poise prevents every tiny hit from cancelling every attack, especially in heavier armor.

```text
PlayerPoiseMax =
  30
  + ArmorPoise
  + 0.6 * max(0, END - 5)
```

Full-set ArmorPoise baseline:

- Light: +0;
- Medium: +15;
- Heavy: +35.

Partial sets contribute proportional slot shares. Apply `Poise/Stagger Resistance` gear bonuses afterwards, respecting the existing +50% gear-contribution cap.

Incoming attack stagger-pressure starting bands:

- light contact: 16;
- medium: 28;
- heavy: 45;
- boss crush/major launch: 70+.

Rules:

- poise damage starts recovering after 1.0 s without new poise damage;
- recovery rate: 45 poise/s;
- reaching zero produces an authored hit-stagger/launch reaction appropriate to the attack;
- ordinary break reaction baseline: ~0.45–0.70 s;
- after player poise break, refill to full and grant 0.35 s stagger-immunity so a multi-hit swarm cannot permanent-lock the player;
- specific greatsword/hammer/Guardian/Warrior actions may temporarily multiply effective poise (hyperarmor), usually 1.25–2.0x, but the skill must show that weight visually.

---

# 15. Attack timing / commitment / Better Combat integration

The runtime already supports explicit upswing and movement multipliers. These values are a starting combat-language baseline.

## 15.1 Hit timing fraction

Target tipping-point / hit time as fraction of the full basic-attack cycle:

| Family | Hit fraction |
|---|---:|
| dagger | 0.42 |
| dual blades | 0.45 |
| wand | 0.43 |
| sword | 0.52 |
| spear | 0.53 |
| staff | 0.50 |
| bow | release-dependent, normally ~0.55 of full-shot cycle |
| axe | 0.56 |
| greatsword | 0.58 |
| hammer / mace | 0.60 |
| crossbow | discharge ~0.55; reload authored separately |
| black-powder firearm | discharge ~0.48; reload authored separately |

Actual animation tipping point and attack event must remain within ±1 server tick.

## 15.2 Movement while attacking

Default Better-Combat-style movement-speed multiplier during committed portion:

| Family | Movement multiplier |
|---|---:|
| dagger | 0.95 |
| dual blades | 0.90 |
| wand | 0.90 |
| sword | 0.85 |
| spear | 0.80 |
| staff | 0.80 |
| bow | 0.70 while committed/drawing |
| axe | 0.70 |
| greatsword | 0.60 |
| hammer / mace | 0.55 |
| crossbow | 0.70 |
| black-powder firearm | 0.65–0.75 by weapon |

Skills can override deliberately.

## 15.3 Input buffering

- basic-attack/combo input buffer: **3 ticks / 0.15 s** before next legal transition;
- dodge input may buffer for **2 ticks / 0.10 s** when a current non-cancellable action is about to become cancellable;
- guard input should become active as soon as the current action legally allows it;
- do not queue an action for so long that the character executes an unwanted input after the player has mentally moved on.

---

# 16. Hit feel budget

Numbers alone do not make impact. The playbook explicitly requires timing, reaction, animation, VFX, sound and camera to agree.

Client-side presentation baseline:

- light confirmed hit: ~20–30 ms micro hit-hold/impact emphasis;
- medium: ~35–45 ms;
- heavy: ~50–65 ms;
- poise break / major boss weak-point hit: ~70–90 ms;
- these are **client presentation effects only** and never pause server simulation or other players;
- repeated multi-hit skills reduce per-hit emphasis so 10 hits do not become visual stutter;
- camera response remains restrained in first-person/close Minecraft framing and must be user-adjustable if motion sickness becomes an issue;
- hit sound, enemy reaction and visible VFX must occur with the confirmed hit rather than noticeably after it.

No generic particle explosion is accepted as a substitute for an actual impact animation/effect on important attacks.

---

# 17. Benchmark DPS used for enemy HP authoring

Enemy HP is authored from a neutral same-Lv benchmark rather than arbitrary large numbers.

`WeaponBudget(L)` remains from `EQUIPMENT_BALANCE.md`.

Define an authoring-only benchmark:

```text
BenchmarkDPS(L) = WeaponBudget(L) * (1.15 + 0.0075 * (L - 1))
```

This approximates a competent same-Lv build using sensible stat allocation, relevant gear and normal skills, **excluding ultimate burst**, after ordinary target mitigation.

Reference:

| Lv | Benchmark DPS |
|---:|---:|
| 1 | ~25 |
| 8 | ~37 |
| 20 | ~58 |
| 44 | ~109 |
| 64 | ~159 |
| 80 | ~205 |

This formula is a content-authoring benchmark, not a hidden replacement for the real player damage formula.

---

# 18. Enemy role TTK targets

TTK means a reasonably built, same-Lv solo player actively fighting correctly. It is not a speedrun and excludes long forced travel/cutscene time.

| Encounter role | Target active-combat TTK |
|---|---:|
| tiny/swarm threat | 1.5–2.5 s |
| ordinary common enemy | **2.5–4.0 s** |
| sturdy/heavy common | 4–6 s |
| elite | **15–25 s** |
| miniboss | **40–70 s** |
| R01 first dungeon boss | **120–150 s** |
| normal later dungeon boss | **150–210 s** |
| field boss | **180–240 s** |
| major/world/endgame boss | **240–330 s** |

Authoring baseline:

```text
EnemyHP ~= BenchmarkDPS(encounterLv) * target_active_combat_seconds
```

Then adjust only for real encounter traits such as long untargetable movement, adds, shield phases or weak-point exposure.

Do **not** add HP merely because a boss model is large.

Examples:

- Lv8 first-dungeon boss at 120–150 s ⇒ roughly **4,400–5,500 HP** before encounter-specific adjustment;
- Lv80 world boss at 240–330 s ⇒ roughly **49,000–68,000 HP**.

Wall-clock encounter time may be ~15–30% longer than active-damage TTK because the player is dodging/repositioning, but a boss should not become a 10-minute sponge by default.

---

# 19. Enemy damage authoring

Enemy damage is generated around a benchmark same-Lv player, not as a direct percentage-MaxHP attack at runtime.

Benchmark VIT for tuning only:

```text
BenchmarkVIT(L) = 5 + round(0.25 * (L - 1))
BenchmarkHP(L) = MaxHP(L, BenchmarkVIT(L), no HP% bonus)
```

Reference benchmark HP:

| Lv | Benchmark VIT | Benchmark HP |
|---:|---:|---:|
| 1 | 5 | 100 |
| 8 | 7 | 143 |
| 20 | 10 | 223 |
| 44 | 16 | 403 |
| 64 | 21 | 575 |
| 80 | 25 | 727 |

For raw attack-data generation, use **25% expected physical mitigation** as the neutral medium-armor benchmark, then let the real Defense formula determine actual received damage.

Target **post-mitigation** damage share against that benchmark player:

| Attack role | Benchmark HP share |
|---|---:|
| minor quick poke | 6–8% |
| ordinary normal attack | **9–12%** |
| ordinary committed/heavy | **16–22%** |
| elite normal attack | 11–15% |
| elite heavy | **22–30%** |
| boss quick/light | 8–12% |
| boss full combo total | **18–28%** |
| boss committed heavy | **22–32%** |
| boss signature/high-risk attack | **35–45%** |
| exceptional catastrophic mechanic | **55–65%** |

Equal-Lv ordinary combat therefore teaches mechanics without two-shotting a new player. Boss signature attacks hurt enough to matter without routine full-health one-shots.

Rules:

- a same-Lv ordinary/boss attack should not routinely exceed 65% benchmark HP after mitigation;
- true one-shot mechanics are exceptional encounter rules, never normal stat inflation;
- if an equal-Lv attack exceeds ~50%, it requires a strong, long, unmistakable telegraph or positional failure condition;
- entering a much higher-Lv region may naturally create near-one-shots because the attacker's fixed Lv budget is much larger. That is allowed and is part of open-world danger.

---

# 20. Telegraph / punish timing

Attacks are tuned around human readability and Minecraft networking, not only damage values.

Starting bands:

| Attack type | Readable wind-up / tell | Typical punish/recovery |
|---|---:|---:|
| common quick | 0.30–0.45 s | 0.20–0.35 s |
| common heavy | 0.55–0.85 s | 0.50–0.80 s |
| elite normal | 0.40–0.70 s | 0.30–0.60 s |
| elite heavy | 0.70–1.10 s | 0.70–1.00 s |
| boss quick | 0.30–0.50 s | 0.20–0.40 s |
| boss committed | 0.65–1.10 s | 0.60–1.00 s |
| boss signature | **1.00–1.60 s** | **1.00–1.80 s** |

A boss may use faster follow-ups **inside an already-readable combo**, but the beginning of a lethal sequence must tell the player what is happening.

Damage/readability safeguards:

- an attack expected to deal >35% benchmark HP should normally have at least ~0.65 s of readable initial cue unless it is an established combo follow-up;
- >50% attacks normally need at least ~1.1 s or an equivalent long positional/environmental warning;
- visual attack area and server damage area must match;
- recovery must be long enough that correctly reading a major attack gives a real punish opportunity.

---

# 21. Multiplayer combat scaling

Ordinary enemies do **not** gain generic HP merely because a friend joined. The game already supports players splitting up; global sponge scaling would punish co-op exploration.

For authored elite/miniboss/boss encounters, let `N` be eligible engaged players.

## 21.1 Boss HP

For `1 <= N <= 4`:

```text
BossHPScale(N) = 1 + 0.65 * (N - 1)
```

Reference:

- 1 player: 1.00x;
- 2: 1.65x;
- 3: 2.30x;
- 4: 2.95x.

For players beyond 4, add only +0.45x per additional eligible player unless a specific encounter is authored for larger groups.

This means co-op is somewhat faster than solo but does not delete the boss instantly.

## 21.2 Boss poise

For `1 <= N <= 4`:

```text
BossPoiseScale(N) = 1 + 0.40 * (N - 1)
```

Beyond 4, +0.30x per additional eligible player.

This prevents four players from permanently staggering a boss while preserving the value of coordinated poise pressure.

## 21.3 Damage

Boss outgoing damage does **not** scale upward merely because more players joined.

Difficulty comes from:

- more target switching;
- area control;
- mechanics that ask players to spread/position;
- encounter-specific adds where appropriate;
- revive pressure.

Do not multiply boss damage and HP at the same time just to claim co-op is harder.

---

# 22. Multiplayer down / revive timing

Master canon already requires down/revive. Baseline numbers:

- multiplayer lethal hit enters Downed if at least one eligible living teammate is participating in the same encounter/nearby play context;
- downed rescue window: **15 s**;
- downed player movement: crawl/very slow reposition only;
- no normal attacks/skills while downed;
- revive channel: **2.5 s**, interrupted by meaningful hostile damage/control;
- successful revive: **35% MaxHP**, **50% Stamina**, **25% Mana**;
- revived player receives **20 s Rescue Fatigue**;
- lethal damage during Rescue Fatigue skips a second down and proceeds to normal defeat/respawn.

This prevents infinite chain-revive loops without removing clutch rescue play.

Single-player still proceeds directly to defeat/respawn as already locked.

---

# 23. Boss combat numerical rules

Bosses are not enlarged common enemies.

Baseline constraints:

- same-Lv first-clear boss should generally survive at least ~2 minutes of competent active damage unless intentionally designed as a short duel;
- routine invulnerability/untargetable transitions should not exceed **2.5 s** repeatedly; longer spectacle must be rare and justified;
- phase transitions change patterns/space/decision-making, not only `damage +20%`;
- one phase should not erase all previously learned tells without giving a new readable cue language;
- major punish windows generally allow at least one meaningful skill or one heavy weapon cycle;
- repeated teleport/flee behavior must not inflate wall-clock fight duration without interaction;
- no boss attack is accepted only because its DPS spreadsheet is balanced: animation, active frame, VFX, sound, hitbox and recovery must all agree.

---

# 24. Data contract

Combat numbers belong in data where practical.

Suggested definitions:

```text
combat/
  player_resource_rules.json
  damage_rules.json
  defense_rules.json
  dodge_guard_rules.json
  poise_rules.json
  weapon_commitment.json
  encounter_role_targets.json
  enemies/*.json
  bosses/*.json
```

## 24.1 Attack definition minimum

```text
id
source_level_rule
damage_type_split
action_coefficient
offensive_stat_weights
crit_allowed
weakpoint_allowed
poise_coefficient
guard_pressure_band
guardable
perfect_guardable
projectile_reflectable
telegraph_ticks
active_ticks
recovery_ticks
hitbox_profile
movement_profile
animation_id
vfx_binding
sound_binding
```

## 24.2 Enemy combat minimum

```text
id
encounter_level
role
max_hp
physical_defense
magic_resistance
poise_max
poise_recovery_delay
poise_recovery_rate
attacks[]
phase_rules[]
multiplayer_hp_scaling
multiplayer_poise_scaling
```

No `TODO_DAMAGE`, `temporary_iframe`, or code-local mystery constants in a player-visible implementation.

---

# 25. First implementation acceptance targets

When source bootstrap reaches combat, verify in this order rather than tuning by compile success:

1. sword same-Lv common target dies in the 2.5–4.0 s band with a benchmark build;
2. heavy weapon has similar sustained budget but visibly stronger poise and commitment;
3. dodge moves ~3.2 blocks, provides exactly 6 authoritative i-frame ticks and is vulnerable in its recovery tail;
4. three immediate baseline dodges are possible, a fourth is not without regeneration/investment;
5. held shield guard consumes Stamina on impact and can guard-break;
6. 4-tick perfect guard is reliable at local low latency and still timing-dependent;
7. visible attack hit frame and actual server hit agree within ±1 tick;
8. ordinary enemy cannot permanently stagger-lock the player;
9. heavy/poise-focused play breaks elites/bosses materially faster than dagger spam without automatically dealing the best HP DPS;
10. first Earthloong-style dungeon-boss prototype lands near 120–150 s active solo TTK at intended Lv/gear;
11. 2-player boss fight is faster than solo but not half the duration; boss HP uses 1.65x and poise 1.40x baseline;
12. Essential-hosted/remote test is eventually required before declaring dodge/parry multiplayer feel successful.

If combat feels slow/weak/floaty despite matching these numbers, inspect wind-up, movement lock, hit frame, recovery, sound, hit reaction, VFX and camera before blindly increasing damage/attack speed, following the project playbook.

---

# 26. What this pass closes

Closed for implementation:

- per-class primary-stat point baseline;
- actual STR/DEX/INT/WIL offensive scaling curve;
- weapon stat-weight defaults;
- VIT → HP formula;
- END → Stamina / Stamina regen formula;
- WIL → Mana / Mana regen formula;
- direct damage order and coefficient model;
- Defense/Magic Resistance formula;
- crit/weak-point baseline;
- exact baseline dodge cost/distance/i-frame/lockout;
- guard absorption and Stamina pressure;
- exact baseline perfect-guard window and anti-mash lockout;
- enemy and player poise rules;
- basic weapon timing/commitment language;
- encounter TTK bands;
- enemy damage bands and telegraph requirements;
- boss multiplayer HP/poise scaling;
- multiplayer down/revive timing;
- first combat implementation acceptance targets.

Still intentionally deferred to the next design work because it requires actual class/skill identity rather than generic formulas:

- exact five root-class starting active skills/passives/ultimates;
- advancement-stage class mechanics and skill variants;
- exact skill Mana/Stamina costs and coefficients;
- class-specific ultimate-charge sources/rates;
- exact elemental/status-effect roster and buildup/effect values;
- individual enemy/boss attack kits beyond the global authoring bands.

The next combat-design batch should therefore build the **five root-class complete starting kits and first specialization branches on top of these numbers**, using external animation/VFX/skill references before locking each skill.