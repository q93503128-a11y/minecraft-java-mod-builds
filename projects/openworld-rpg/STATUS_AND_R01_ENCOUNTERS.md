# Open-World RPG — Status / Element / R01 Encounter Combat Canon

> Status: **DESIGN CANON — status/element rules and R01 encounter kits locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Combat math/timing: `COMBAT_BALANCE.md`  
> Class kits: `CLASS_COMBAT_KITS.md`  
> Region reference: `REGIONS.md`  
> Loot/equipment: `LOOT_ECONOMY.md`, `EQUIPMENT_BALANCE.md`  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins. Existing class-status definitions in `CLASS_COMBAT_KITS.md` are preserved unless this file only adds missing implementation detail without changing their behavior.

This pass closes two linked implementation blockers:

1. a medium-complexity elemental/status system that creates build and encounter decisions without becoming six extra resistance spreadsheets; and
2. the first region's production combat roster, including actual encounter levels, HP/Defense/MR/poise, attacks, tells, status interactions, reward hooks and external model/runtime boundaries.

The target remains a **fast readable action RPG**. Statuses support the combat loop; they do not replace direct combat with passive damage bars.

---

# 1. External precedents and adoption boundaries

## 1.1 Guild Wars 2 — distinct condition roles

Useful precedent:

- damaging conditions and control conditions have different jobs;
- poison is not merely another color of damage because it also reduces healing;
- chill is primarily a control condition;
- vulnerability-style debuffs alter a target's damage intake rather than adding another DoT.

Adopted:

- every status needs a clear tactical identity;
- movement control, damage-over-time, defense reduction, class marks and threat control remain separate concepts;
- status UI communicates the actual effect instead of only showing an elemental color.

Not adopted:

- GW2's very large chill penalties are not copied. The already-canonical `Chilled` values in `CLASS_COMBAT_KITS.md` remain much lighter and role-scaled.

Reference: `https://wiki.guildwars2.com/wiki/Condition`

## 1.2 Monster Hunter — repeated-status adaptation

Official Monster Hunter manuals state that monsters adapt to repeated abnormal status and that monsters may have natural resistance to particular statuses.

Adopted:

- stronger ailments use buildup thresholds;
- repeatedly triggering the **same** ailment on an elite/boss becomes progressively harder during the encounter;
- this applies per ailment, not as a global anti-status immunity wall.

Not adopted:

- bosses are not completely immobilized for long periods by routine paralysis/sleep loops;
- the system does not require an opaque spreadsheet of hidden monster tolerances to be playable.

Reference: `https://game.capcom.com/manual/MH_Gen/en/page-46.html`

## 1.3 Elden Ring — buildup and per-status repeat resistance

Useful precedent:

- ailments build toward a resistance threshold;
- repeated procs can increase resistance to that same ailment;
- Frostbite combines a proc payoff with a temporary vulnerability state;
- Fire can deliberately interact with active Frostbite.

Adopted in a lighter form:

- proc thresholds + per-ailment repeated-proc resistance;
- Frostbite has a short vulnerability window;
- a direct Fire hit can intentionally consume Frostbite for a small `Thermal Shock` payoff.

Not adopted:

- no 20% universal damage-vulnerability Frostbite for 30 seconds;
- no status burst that removes a huge fixed percentage of boss HP;
- no endlessly resetting Fire/Frost loop without growing resistance.

References:
- `https://eldenring.wiki.gg/wiki/Resistance_Correction`
- `https://eldenring.wiki.gg/wiki/Frost`

## 1.4 Threateningly Mobs / Continued — R01 heavy-creature source

The current 26.2 continuation provides Fabric builds and retains the visual/ecology identities used by the region design:

- Nature Spirit — forest families;
- Earthloong — forests/jungles;
- Steelboar — meadows/savannas/dark forests/badlands;
- Regalhart — meadows/taigas/forests;
- Louxia — plains identity in the original mod.

The original project lists these heavy creatures as deliberately high-threat enemies. Historical/current source material also supports:

- Steelboar as a charge-focused boar;
- Regalhart as a huge deer with a low-HP special ability;
- Nature Spirit's later revision using melee and no longer firing while in defense mode;
- an underground forest dungeon with a special Earthloong boss;
- Earthloong's lightning identity in secondary documentation.

**License boundary:** current CurseForge and Modrinth displays for the continuation disagree (`All Rights Reserved` vs `MIT`). Therefore the continuation remains **dependency-only** until exact upstream/source licensing is reconciled. No continuation assets/code are copied into the public repo merely because one store says MIT.

References:
- `https://www.curseforge.com/minecraft/mc-mods/threateninglly-mobs-continued`
- `https://modrinth.com/mod/threateningly-mobs-continued`
- `https://modrinth.com/mod/threateningly-mobs/version/1.0.9.4`
- `https://modrinth.com/mod/threateningly-mobs/version/1.0.9.7`

## 1.5 Alex's Mobs Continued — cave ecology, not R01 surface snake

Current Fabric 26.2 builds exist. Cave Centipede is a good deep-quarry enemy because its established behavior is cave-only, poison-biting and wall-climbing.

The established Rattlesnake identity, however, is specifically desert/badlands and warns before a poisonous defensive bite. It is therefore **not** used as the R01 meadow/forest surface snake merely because the model already exists. It remains a later R07 candidate.

The continuation's license display also differs across storefronts, so project use remains normal dependency/integration unless exact source licensing is resolved.

Reference: `https://www.curseforge.com/minecraft/mc-mods/alexs-mobs-continued`

## 1.6 R01 surface snake visual source

R01 uses a project-owned **Meadow Viper** built from the CC0 Quaternius `LowPoly Animated Easy Enemies` Snake asset family rather than relocating Alex's desert Rattlesnake.

The Quaternius pack supplies Snake plus movement/attack/jump/death animations under CC0 and is therefore a viable editable/direct base before the name/role is locked.

References:
- `https://quaternius.itch.io/animated-easy-enemies`
- `https://poly.pizza/m/x9x0viZs8V`

---

# 2. Damage channel versus element tag

Do **not** create a second complete damage formula for each element.

Every damaging hit still uses the existing main mitigation channel:

```text
physical
magic
```

A hit may additionally carry an element/status tag such as:

```text
fire
frost
lightning
poison
affliction-specific tags
```

The element tag can control:

- target susceptibility;
- status buildup;
- class/passive interactions;
- VFX/sound/readability;
- authored encounter mechanics.

It does **not** bypass `COMBAT_BALANCE.md` Defense/Magic Resistance unless the specific action explicitly says so.

## 2.1 Enemy elemental susceptibility

Use a compact five-state multiplier for direct element-tagged damage:

| State | Direct-damage multiplier |
|---|---:|
| weak | 1.15x |
| neutral | 1.00x |
| resistant | 0.85x |
| strongly resistant | 0.70x |
| immune | 0.00x |

Rules:

- `immune` is rare and must be visually/fantasy justified;
- normal enemies generally expose at most one notable weakness and one notable resistance;
- ordinary players do not need six mandatory elemental-resistance gear stats;
- special items/potions may provide targeted protection later, but elemental defense does not become a parallel armor spreadsheet;
- susceptibility is applied after the relevant Defense/MR calculation and before final authored generic `% damage taken` modifiers.

## 2.2 Element-output affixes

Existing `specific implemented element/status output` affixes in `EQUIPMENT_BALANCE.md` are interpreted as follows:

- an **element output** affix enters the existing additive direct-power bucket for direct damage tagged with that element;
- a **status output** affix modifies the authored damage/effect magnitude of that specific status when the item definition allows it;
- it does not secretly multiply both direct damage and buildup rate twice;
- buildup rate bonuses must be explicitly authored as buildup bonuses if introduced later.

The existing +60% per-element/status gear-contribution cap remains.

## 2.3 Status-power and status-poise reference

A buildup ailment's proc magnitude must not depend on whichever hit happened to cross the threshold last.

For **player-origin** Poisoned / Bleeding / Frostbite / Shocked damage, snapshot the strongest valid contributor from the existing previous-6-second contribution window and use that source's offensive snapshot:

```text
StatusWeightedStat =
  the same authored primary-stat weighting used by the contributing source action

StatusReferencePower =
  WeaponPower
  * AttributeDamageMultiplier(StatusWeightedStat)

RawStatusDamage =
  StatusReferencePower
  * StatusCoefficient
  * (1 + applicable specific Status Output bonuses)
```

Rules:

- status damage does not crit unless the ailment explicitly says otherwise;
- ordinary weak-point multipliers do not retroactively amplify a later status proc;
- Element Output affects direct element-tagged damage as defined above; it does not automatically double-dip into status damage;
- a named item/skill may explicitly say an element bonus also affects its status payload, but that is authored data rather than a global assumption;
- Thermal Shock uses the triggering Fire source's offensive snapshot exactly as already specified;
- if no valid offensive snapshot exists, a player-origin proc fails closed rather than inventing vanilla attack damage.

Player-origin status poise uses a neutral status reference rather than a hidden weapon-family multiplier:

```text
StatusPoiseDamage =
  10
  * PoiseCoefficient
  * (1 + applicable Poise Output bonuses)
```

For **enemy-origin** ailments, do not fabricate WeaponPower/primary stats. The encounter sheet supplies a fixed source-Lv status damage budget and, where needed, explicit player-poise pressure. Those fixed values still pass through the target's normal Defense/MR/status-duration/poise rules.

---

# 3. Two status categories

## 3.1 Direct conditions / marks

These are applied directly when the accepted skill/attack says so. They do not require a generic buildup meter unless a later source explicitly defines one.

Already canonical:

- `Snared`;
- `Chilled`;
- `Burning`;
- `Fractured Armor`;
- `Rebuked`;
- `Judged`;
- `Provoked`.

Their current `CLASS_COMBAT_KITS.md` behavior remains authoritative.

## 3.2 Buildup ailments

The launch core buildup ailments are:

- `Poisoned`;
- `Bleeding`;
- `Frostbite`;
- `Shocked`.

These are stronger payoff states. They require buildup to reach a threshold rather than being guaranteed by every tiny contact.

---

# 4. Buildup rules

Each target stores one buildup meter per buildup ailment.

```text
0 <= buildup < current_threshold
```

When accepted buildup reaches/exceeds threshold:

1. trigger the ailment;
2. reset that ailment's buildup to 0;
3. increment that ailment's encounter `proc_count`;
4. recalculate only that ailment's repeat threshold.

## 4.1 Player threshold

WIL supplies modest general ailment resistance as previously reserved by `COMBAT_BALANCE.md`.

```text
PlayerAilmentThreshold = round(
    100 + 0.70 * max(0, WIL - 5)
)
```

Reference:

| WIL | Threshold |
|---:|---:|
| 5 | 100 |
| 30 | 118 |
| 60 | 139 |
| 80 | 153 |
| 100 | 167 |

This affects **frequency of proc**, not the raw damage of the ailment once active.

Existing `negative-status duration reduction` gear then shortens eligible status duration after a proc.

Ordinary total negative-status duration reduction is hard-capped at **50%** unless a named immunity mechanic explicitly says otherwise.

## 4.2 Enemy role threshold

```text
EnemyBaseAilmentThreshold =
    100
    * EncounterRoleFactor
    * AilmentResistanceFactor
```

Encounter role factors:

| Role | Factor |
|---|---:|
| common | 1.00 |
| sturdy common | 1.15 |
| elite | 1.35 |
| miniboss | 1.65 |
| dungeon / field boss | 2.15 |
| major / world boss | 2.60 |

Ailment-resistance factors:

| Ailment relation | Factor |
|---|---:|
| weak | 0.80 |
| neutral | 1.00 |
| resistant | 1.25 |
| strongly resistant | 1.60 |
| immune | no buildup accepted |

## 4.3 Repeat resistance

After the same buildup ailment has already triggered during the encounter:

```text
RepeatFactor = 1 + 0.35 * min(proc_count, 4)
CurrentThreshold = round(EnemyBaseAilmentThreshold * RepeatFactor)
```

Reference after each prior proc:

```text
0 prior: 1.00x
1 prior: 1.35x
2 prior: 1.70x
3 prior: 2.05x
4+ prior: 2.40x
```

Rules:

- proc of Poison does not raise Frostbite resistance;
- common enemies normally die before this matters, but the same rule is safe globally;
- boss proc counts reset on full encounter reset/despawn, not merely because the party kites for five seconds;
- ordinary non-boss targets reset proc count after leaving combat for 15 s and returning to their normal ecology state.

## 4.4 Buildup decay

If no new buildup of that ailment arrives for **3.0 s**:

```text
BuildupDecayPerSecond = 20% of EnemyBaseAilmentThreshold
```

Player buildup uses 20 points/s after the same 3 s delay.

A new buildup packet immediately stops decay and restarts the delay.

## 4.5 Multiplayer ownership

Each target has **one shared meter per ailment**, not one meter per player.

- all eligible players can contribute buildup;
- the proc magnitude snapshots the strongest valid contributor's status power among contributors from the previous 6 s;
- all meaningful contributors receive encounter participation credit;
- a low-power final poke cannot steal a high-power status proc;
- the target still has only one active Poison/Bleed/Frostbite/Shocked state at a time, preventing four-player status multiplication from bypassing boss HP scaling.

---

# 5. Core ailment definitions

## 5.1 Poisoned

Role: long DoT + anti-healing.

Player-origin baseline proc:

```text
duration: 8.0 s
total StatusCoefficient: 0.75
tick interval: 1.0 s
damage channel: magic/toxin
critical: false
healing received: -20%
```

Rules:

- normal Magic Resistance mitigates poison damage because the project intentionally has no separate mandatory Poison Defense stat;
- a stronger Poison replaces a weaker active Poison; otherwise re-proc refreshes duration;
- no intensity stack count;
- player/enemy cleansing can remove it when the source permits cleanse;
- enemy-applied Poison uses an authored source-Lv damage budget rather than `% target MaxHP` runtime damage. R01 enemy values are specified in the encounter section.

## 5.2 Bleeding

Role: shorter physical attrition ailment for cutting/puncture builds.

Player-origin baseline proc:

```text
duration: 6.0 s
total StatusCoefficient: 0.70
tick interval: 1.0 s
damage channel: physical
critical: false
```

For Bleeding ticks only:

```text
EffectiveDefenseForBleed = 50% of normal EffectiveDefense
```

Rules:

- Bleeding does not become a giant percentage-MaxHP boss nuke;
- stronger magnitude wins, duration refreshes;
- sprinting/moving does not arbitrarily increase bleed damage;
- constructs/creatures without a plausible bleeding anatomy may be resistant or immune, but immunity must be data-authored rather than inferred from entity class names.

## 5.3 Frostbite

Role: proc burst + short vulnerability window.

On proc:

```text
immediate StatusCoefficient: 0.35 magic/frost
Frostbite duration: 5.0 s
```

During Frostbite:

- common/elite direct damage taken: **+8%**;
- miniboss/boss direct damage taken: **+5%**;
- the bonus uses the existing final authored damage-taken layer and respects global mitigation/damage-taken safeguards;
- Frostbite itself does not slow movement. `Chilled` remains the movement-control status.

### Thermal Shock

A **direct** accepted Fire-tag hit against an actively Frostbitten target:

- removes Frostbite;
- adds `0.30` StatusCoefficient using the Fire source's offensive snapshot;
- adds `PoiseCoefficient 1.25` worth of status poise pressure;
- per-target Thermal Shock ICD: 1.0 s;
- Burning DoT ticks do not trigger Thermal Shock;
- removing Frostbite does not erase current Frost buildup and does not reset its repeat-resistance proc count.

This creates deliberate Fire/Frost sequencing without an infinite free-reset loop.

## 5.4 Shocked

Role: lightning proc + poise synergy, **not long paralysis**.

On proc:

```text
immediate StatusCoefficient: 0.50 magic/lightning
immediate PoiseCoefficient: 1.60
Conductive duration: 4.0 s
```

While Conductive:

- common/elite Lightning-tagged direct hits deal +15% poise damage;
- miniboss/boss Lightning-tagged direct hits deal +10% poise damage;
- no movement lock or universal stun;
- no automatic chain lightning unless the applying skill explicitly owns a chain mechanic.

This gives Lightning an identity without allowing a party to hard-stun a boss repeatedly.

---

# 6. Existing class statuses — compatibility details

These rules only clarify integration; they do not redefine the class canon.

## Chilled

- movement reduction only;
- strongest magnitude wins;
- duration refreshes;
- does not automatically apply Frost buildup;
- an individual Frost skill may apply both Chilled and Frost buildup if its data says so.

## Burning

- direct timed fire/magic damage;
- no crit;
- normal MR;
- strongest existing magnitude wins, duration refreshes;
- Burning does not require buildup;
- Burning ticks do not trigger Thermal Shock.

## Snared

- movement reduction only unless source skill says more;
- bosses are never globally rooted by Snared.

## Fractured Armor

- EffectiveDefense reduction only;
- strongest magnitude wins and same source does not stack.

## Rebuked

- outgoing direct-damage reduction only;
- environmental/self-inflicted damage unaffected.

## Judged

- source-player-specific Inquisitor mark;
- not a generic dispellable world ailment.

## Provoked

- AI threat weighting only;
- no PvP forced control;
- encounter scripts may override.

---

# 7. Cleanse categories

`Saint / Greater Mend` already distinguishes minor and major cleanse.

Launch **minor dispellable** states:

- Burning;
- Chilled;
- Snared;
- Poisoned;
- Bleeding;
- Fractured Armor;
- Rebuked;
- Conductive portion of Shocked.

`Frostbite` counts as **major dispellable** because it carries a burst/vulnerability payoff.

Future encounter curses/hard-control effects may be added as major statuses later.

`Judged`, `Provoked`, boss phase marks and scripted encounter mechanics are not automatically removed by generic cleanse unless explicitly tagged dispellable.

---

# 8. Status UI / readability

Status complexity stays medium by showing only what the player can act on.

Player HUD:

- active negative status icon + remaining-duration ring;
- when buildup exceeds 35% threshold, show a restrained buildup ring around that ailment icon;
- no permanent empty row of every possible ailment.

Enemy/target UI:

- active status icons near target/boss frame;
- elite/boss buildup only becomes visible after the player has contributed meaningful buildup to that ailment;
- boss status-resistance increase is communicated by a subtle thicker buildup threshold marker after a proc, not hidden entirely;
- no giant center-screen `POISON!` popup on every application.

World readability:

- Burning: attached shaped flame/heat treatment, not generic particle spam;
- Chilled/Frostbite: frost rim/crystal or ground-contact treatment;
- Poison: compact toxic trail/aura + icon;
- Shocked/Conductive: short electrical arcs tied to body/model anchors;
- status VFX cannot hide attack telegraphs or weak points.

---

# 9. R01 roster — external source and ecology role

| Project role | Production identity | External source | Adoption mode | R01 use |
|---|---|---|---|---|
| food/passive ecology | Louxia | Threateningly Mobs Continued | dependency-only | plains/meadow herds |
| surface small threat | **Meadow Viper** | Quaternius LowPoly Animated Easy Enemies — Snake | CC0 editable/direct base | grass/river/forest-edge defensive predator |
| deep quarry threat | Cave Centipede | Alex's Mobs Continued | dependency/integration | deep quarry/cave pockets only |
| herd hazard/resource | Bison | Alex's Mobs Continued | dependency/integration | open meadow herds |
| rare territorial wildlife | Grizzly | Alex's Mobs Continued | dependency/integration | forest fringe / rare |
| elite | Steelboar | Threateningly Mobs Continued | dependency-only | meadow/dark-wood pockets |
| rare elite | Nature Spirit | Threateningly Mobs Continued | dependency-only | forest fringe/grove |
| optional field boss | Regalhart | Threateningly Mobs Continued | dependency-only | authored meadow/forest hunt |
| first dungeon boss | Earthloong | Threateningly Mobs Continued | dependency-only | root-overgrown quarry boss |

Gazelle/Raccoon/Crow remain ecological life and do not need full hostile combat kits in this pass.

## 9.1 Rattlesnake correction

The earlier `REGIONS.md` wording said `rattlesnake/cave-centipede-style enemies` as an early threat direction.

Production decision:

- Alex's exact Rattlesnake is **not** an R01 spawn because its established external identity is desert/badlands;
- the CC0 Quaternius Snake is the accepted external base for R01 `Meadow Viper`;
- Alex's Rattlesnake remains a later **R07 candidate** where its warning-rattle/desert behavior actually fits.

This is a refinement of a candidate, not a reversal of the region's intended small-snake combat role.

---

# 10. R01 ecology/spawn contract

R01 ordinary world population must still feel like ecology, not a combat arena every 20 blocks.

- Louxia/Gazelle/Raccoon/Crow: mostly passive/ambient;
- Bison: neutral herd; attacks only when provoked or close calf/herd defense requires it;
- Meadow Viper: defensive threat with readable pre-attack posture rather than constant chase;
- Grizzly: rare territorial hazard, not a common aggro mob;
- Cave Centipede: deep quarry/cave only; never a daylight meadow spawn;
- Steelboar: sparse elite/territorial spawn;
- Nature Spirit: very rare grove/forest-edge elite;
- Regalhart: authored field-boss encounter/roaming hunt, not ordinary natural-population density;
- Earthloong: authored R01 dungeon boss; normal mature Earthloong belongs later in R05.

No vanilla animals/hostiles fill gaps around these tables.

---

# 11. R01 combat-stat summary

These values use the existing same-Lv TTK targets from `COMBAT_BALANCE.md`.

| Encounter | Lv | Role | HP | Defense | MR | Poise | Approx. solo active TTK |
|---|---:|---|---:|---:|---:|---:|---:|
| Meadow Viper | 2 | common | **75** | 7 | 4 | 18 | ~2.8 s |
| Cave Centipede | 4 | sturdy common | **165** | 19 | 10 | 42 | ~5.5 s |
| Bison | 3 | sturdy neutral wildlife | **155** | 23 | 7 | 50 | ~5.4 s if fought |
| Grizzly | 6 | dangerous territorial wildlife | **320** | 24 | 11 | 58 | ~9–11 s |
| Steelboar | 6 | elite | **680** | 37 | 11 | 82 | ~20 s |
| Nature Spirit | 7 | elite | **790** | 25 | 39 | 78 | ~22–23 s |
| Regalhart | 8 | field boss | **6,600** | 29 | 23 | 180 | ~180 s |
| Earthloong | 8 | first dungeon boss | **4,900** | 45 | 35 | 190 | ~134 s |

Notes:

- these are project-normalized RPG stats; donor-mod vanilla HP values are not imported;
- field/dungeon boss multiplayer HP/poise scaling remains exactly `COMBAT_BALANCE.md`;
- HP is not increased because a donor model is physically large;
- exact world movement speed/pathfinding is validated after importing the actual entity/model because Minecraft geometry can change practical pressure substantially.

---

# 12. Louxia — passive R01 food ecology

## Identity

- Lv1 ecology creature;
- passive herd animal;
- luminous body organ is visible and explains `Louxia Glow`;
- no fake hostile moveset is invented just to make every entity combat content.

Behavior:

- flees when damaged;
- short panic sprint, then attempts to regroup with nearby Louxia;
- does not body-block the player aggressively;
- breeding/taming behavior from donor dependency is not required for the project's baseline loop unless later husbandry design intentionally adopts it.

Drops remain `EQUIPMENT_BALANCE.md`:

- Louxia Meat 1–2 guaranteed per eligible kill;
- Louxia Glow 35%;
- Gold: 0.

No equipment drop.

---

# 13. Meadow Viper — surface common threat

## External presentation

- exact model/motion family: Quaternius `LowPoly Animated Easy Enemies` Snake, CC0;
- use its existing snake movement/attack language as the base;
- Minecraft conversion may adjust proportions/texture treatment but must not replace the source silhouette with an improvised vanilla silverfish/small-slime substitute.

## Behavior

Defensive, not permanently hostile.

- warning zone: 3.5 blocks;
- when a player remains inside warning space, Viper coils/faces target for at least 0.45 s;
- leaving warning space before attack de-escalates it;
- direct attack against the Viper immediately engages it;
- after a successful or missed bite, it tries to create ~2 blocks of space rather than glueing itself to the player's feet.

## Attack — Coil Bite

```text
wind-up: 0.45 s
active/lunge: ~0.15 s
recovery: 0.55 s
range: ~2.2 blocks
benchmark post-mitigation damage: 9% same-Lv benchmark HP
guard pressure: light
guardable: true
perfect_guardable: true
poise pressure: light
Poison buildup: 40
```

Three poorly handled bites can poison a baseline WIL character, while one bite teaches the buildup UI without immediately forcing an antidote.

If Poisoned by Meadow Viper:

```text
duration: 8 s
total fixed source-Lv budget: ~9% Lv2 benchmark HP before target-specific mitigation
healing received: -20%
```

## Status relations

- Poison buildup: strongly resistant (`1.60x` threshold);
- other launch ailments: neutral;
- no arbitrary elemental weakness.

## Rewards

- normal common-enemy EXP target ~1% of current next-Lv requirement;
- Class XP ~0.8% current Class Rank requirement;
- Gold: **2 at 60%**;
- no random consumable drop at baseline;
- **no new venom material** is invented solely because the snake exists.

---

# 14. Cave Centipede — deep quarry sturdy common

## External presentation

Use Alex's Mobs Continued dependency entity/animation. Preserve:

- long segmented body;
- wall-climbing identity;
- poison bite;
- deep-cave ecology.

Project spawn override confines it to quarry/deep-cave pockets appropriate to R01 rather than letting donor global spawn rules own region design.

## Attack 1 — Scuttle Bite

```text
wind-up: 0.35 s
recovery: 0.40 s
benchmark damage: 11% same-Lv benchmark HP
guard pressure: light
guardable: true
perfect_guardable: true
Poison buildup: 30
```

## Attack 2 — Body Rake

The segmented body visibly sweeps across the player's space.

```text
wind-up: 0.65 s
active sweep: 0.25 s
recovery: 0.65 s
benchmark damage: 18%
guard pressure: medium
guardable: true
perfect_guardable: true
player poise pressure: medium
Poison buildup: 0
```

## Attack 3 — Ceiling Drop

Only valid while the centipede is meaningfully above the target on climbable quarry geometry.

```text
warning: 0.70 s visible body movement + ground shadow/dust cue
landing radius: 2.3 blocks
benchmark damage: 16%
guard pressure: medium
guardable: true
perfect_guardable: true
Poison buildup: 20
recovery: 0.75 s
```

No teleport-to-ceiling behavior is invented; the attack requires a physically valid climbed position.

## Status relations

- Poison buildup: strongly resistant;
- other statuses neutral.

## Rewards

- no ordinary equipment roll;
- common/sturdy EXP target ~1% current Lv requirement;
- Class XP ~0.8%;
- Gold: **3 at 70%**;
- donor `Cave Centipede Leg` is **not** automatically admitted into project loot. It remains disabled from the project-normalized loot table unless later alchemy design gives it a real non-overlapping use and the dependency/license boundary is rechecked.

---

# 15. Bison — neutral herd hazard

Bison is not a farmable hostile pack.

## Behavior

- neutral herd movement;
- aggro when attacked;
- nearby adult bison can assist if a herd member/calf is attacked, with a short group-response cap so a whole loaded meadow does not pathfind to one player;
- disengages after the player leaves herd territory and no recent damage exists.

## Attacks

### Headbutt

```text
wind-up: 0.45 s
damage: 10%
guard pressure: medium
guardable/perfect_guardable: true
```

### Herd Charge

```text
wind-up: 0.75 s with hoof scrape/head-lower tell
damage: 18%
guard pressure: heavy
guardable/perfect_guardable: true
recovery after miss: 0.80 s
```

## Reward

Use canonical Tough Hide sourcing:

- Tough Hide 1–2 at 70%;
- no ordinary food drop in R01; Louxia remains the intended early creature-food source;
- Gold: 0;
- no equipment drop.

---

# 16. Grizzly — rare territorial wildlife

Grizzly remains a **hazard**, not a full elite economy source.

Behavior:

- warns/stands/roars before aggro where donor animation supports it;
- attacks when territory is violated or harmed;
- does not pursue across huge distances merely because the player rendered it.

Attacks:

### Paw Swipe

```text
wind-up: 0.40 s
damage: 12%
guardable/perfect_guardable: true
```

### Maul Sequence

```text
initial tell: 0.65 s
2-hit total damage budget: 20%
guard pressure: medium + medium
follow-up is readable from first swing
recovery: 0.75 s
```

### Warning Roar

- no HP damage;
- short intimidation/space cue only;
- does not apply an arbitrary fear hard-control status at R01.

Drops:

- Tough Hide 2–3 guaranteed;
- Gold: 0;
- no normal equipment roll.

---

# 17. Steelboar — R01 elite

## External identity preserved

- heavy steel-tusk boar silhouette;
- neutral/territorial rather than constant map-wide aggro;
- strong headlong charge is the center of the kit.

## Attack 1 — Iron Tusk

```text
wind-up: 0.40 s
recovery: 0.35 s
benchmark damage: 13%
guard pressure: medium
guardable/perfect_guardable: true
poise pressure: medium
```

## Attack 2 — Shoulder Hook

```text
wind-up: 0.60 s
wide frontal arc
benchmark damage: 20%
guard pressure: heavy
guardable/perfect_guardable: true
poise pressure: heavy
recovery: 0.70 s
```

## Attack 3 — Iron Rush

Core signature attack.

```text
pre-tell: 0.85 s snort + hoof scrape + head lower
charge path: up to 12 blocks
benchmark damage: 28%
guard pressure: heavy
player poise pressure: 70
perfect_guardable: true
perfect_guard_poise_multiplier: 1.40
recovery after miss/end: 1.20 s
```

Rules:

- charge steering is limited after commitment;
- if the player sidesteps/dodges, Steelboar must visibly overshoot instead of magnetically turning 90 degrees;
- a successful perfect guard produces a major poise reward but does not launch the elite across the arena;
- collision with tagged heavy obstacle can add **18 self-poise damage**, with a per-charge one-time limit.

## Low-HP behavior

Below 35% HP, `Furious Route` is deterministic rather than an implementation-chosen random chance:

- when its 14 s Furious Route cooldown is ready, the **next** otherwise-valid `Iron Rush` opportunity at 6–12 blocks with a clear committed line becomes a two-charge `Furious Route`;
- second charge receives its own >=0.60 s pivot/tell;
- if a second legal charge line cannot be established after the pivot tell, the sequence ends after charge one and still consumes the Furious Route cooldown;
- after the second charge, recovery is 1.40 s;
- no permanent attack-speed/damage steroid.

### Runtime action-selection authority

Code state `23c7fec95ed14c4f6ed6c80f774fa17ecac3b739` adds the server-owned, data-backed
Steelboar selection controller without fabricating unresolved impact semantics:

- the bundled identity is exact `threateningly_mobs:steelboar`, content Lv6, with the global
  **2-tick / 0.10 s** post-recovery decision delay recorded in encounter data;
- close range `<=3.0` uses deterministic server RNG over **Iron Tusk 60 / Shoulder Hook 40**,
  with Shoulder Hook removed while its exact **60-tick / 3.0 s** cooldown is active;
- the general R01 anti-repeat contract is enforced: if the same ordinary close attack has committed
  twice in a row and the other close attack is legal, the repeated attack is removed from the next
  candidate set;
- a clear committed line at **5.5–12.0 blocks** gives ready Iron Rush priority over ordinary melee;
- **3.0–5.5 blocks** with no legal Rush returns reposition rather than inventing extra reach or a
  hidden lunge;
- Iron Rush records its exact **140-tick / 7.0 s** cooldown, 17-tick tell, 24-tick recovery,
  12-block commitment, 28% benchmark damage, heavy guard-pressure band, player-poise pressure 70,
  perfect-guardability, `1.40x` perfect-guard poise multiplier and one-time heavy-obstacle self-poise
  value 18 as canonical data;
- below, not at, **35% HP**, a ready Furious Route deterministically replaces the next otherwise
  legal Iron Rush only in its exact **6.0–12.0 block** window;
- committing Furious Route consumes its exact **280-tick / 14 s** override cooldown immediately,
  preserves the normal Iron Rush cooldown, records the second-charge pivot tell as a **minimum
  12 ticks / >=0.60 s**, and records **28 ticks / 1.40 s** recovery after charge two;
- if the target is only 5.5–<6.0 blocks away, low HP does not steal the ordinary Iron Rush with a
  Furious Route;
- deterministic selection is seeded from encounter/spawn instance + actor UUID + action counter.

One source gap is intentionally preserved instead of guessed: current Steelboar canon states
`perfect_guardable: true` for Iron Rush but does **not** state its ordinary `guardable` tag.
The data model therefore stores Iron Rush `guardable = null` and explicitly reports its impact
contract as not yet closed. Iron Tusk and Shoulder Hook retain their explicit
`guardable/perfect_guardable: true` values.

Build Openworld RPG run `37094461580` is **SUCCESS**: tests/build, pinned creature/dependency
inspection, core/gameplay dedicated-server smoke, gameplay client startup, both verification JARs,
mrpack packaging and artifact upload all passed. Artifact:
`openworld-rpg-m0-23c7fec95ed14c4f6ed6c80f774fa17ecac3b739`
(`11262949211`, SHA-256
`f8645f5e15519b1b27ec594584a3efd5142afb61471e421eaad9d509c526d227`).

This closes Steelboar **selection/cooldown/Furious Route decision authority** only. Charge steering,
second-charge execution, exact impact geometry, obstacle collision and production presentation/spawn
remain separate gates.

### Runtime closed-melee execution and impact authority

Code state `7a4a06781ac9ceaecc21cff748a79464e5dbcf73` binds the two Steelboar melee attacks
whose impact contracts are fully closed:

- the shared player-poise canon maps incoming `medium` stagger pressure to **28** and `heavy`
  to **45**; those values are therefore not local Steelboar inventions;
- Iron Tusk commits an exact **8-tick / 0.40 s** wind-up, one server impact frame, then
  **7-tick / 0.35 s** recovery;
- Iron Tusk impact resolves **13% same-Lv benchmark physical damage**, medium guard pressure,
  `guardable=true`, `perfect_guardable=true`, and player-poise pressure **28** through the
  central project player-damage/defense/poise runtimes;
- Shoulder Hook commits an exact **12-tick / 0.60 s** wind-up, one server impact frame, then
  **14-tick / 0.70 s** recovery;
- Shoulder Hook impact resolves **20% same-Lv benchmark physical damage**, heavy guard pressure,
  `guardable=true`, `perfect_guardable=true`, and player-poise pressure **45** through the same
  central authority;
- one committed action-counter may consume each target at most once and only on the exact impact
  tick; early, late and duplicate callbacks fail closed;
- a second melee execution cannot begin while the current one is in wind-up/impact/recovery;
- exact authored Steelboar registry + project `authored_spawn` are required before runtime
  impact authority can apply;
- donor natural spawns and raw verification entities do not inherit these production combat rules;
- Iron Rush and Furious Route are explicitly rejected by this execution/impact path. Their unresolved
  ordinary `guardable` tag therefore cannot be bypassed through the now-working melee authority.

Build Openworld RPG run `37095783113` is **SUCCESS**: tests/build, pinned creature/dependency
inspection, core/gameplay dedicated-server smoke, gameplay client startup, both verification JARs,
mrpack packaging and artifact upload all passed. Artifact:
`openworld-rpg-m0-7a4a06781ac9ceaecc21cff748a79464e5dbcf73`
(`11264875569`, SHA-256
`22e67a3282418ecfa8af19fed35699a572da2b57ac778f068d43088c907a4eb8`).

This closes Iron Tusk / Shoulder Hook **backend timing + impact authority**. Their final visible
frontal contact geometry/animation is still a presentation binding, and Steelboar production spawn
remains closed. Iron Rush/Furious Route remain blocked from impact execution until their remaining
canon/presentation gates are closed.

## Status relations

- Bleeding buildup: resistant;
- Poison buildup: resistant;
- Lightning direct damage: weak (1.15x) only because the accepted steel/metal body language supports conductivity;
- other direct elements neutral.

## Rewards

Apply elite loot canon:

- 30% one ordinary equipment roll;
- Iron Ore 1–3 at 60%;
- Tough Hide 1 at 35%;
- Gold: **18 guaranteed**;
- EXP ~6% current next-Lv requirement;
- Class XP ~5% current Class Rank requirement.

No separate `Steel Fang Currency` is introduced.

---

# 18. Nature Spirit — rare R01 forest-edge elite

## External identity preserved

The donor identity is a heavy nature/rock creature and its later update explicitly moved it toward melee while removing defense-mode shooting.

Therefore this project **does not** turn it back into a generic ranged caster.

## Attack 1 — Rooted Swipe

```text
wind-up: 0.45 s
frontal arc
benchmark damage: 13%
guard pressure: medium
guardable/perfect_guardable: true
```

## Attack 2 — Earthen Ram

```text
wind-up: 0.75 s
short 3.0-block committed body ram
benchmark damage: 22%
guard pressure: heavy
guardable/perfect_guardable: true
poise pressure: heavy
recovery: 0.85 s
```

## Defense behavior — Living Shell

```text
duration: up to 2.5 s
movement: nearly stationary
direct damage taken: -35%
poise damage taken: +25%
minimum reuse: 12 s
```

Rules:

- the model visibly closes/braces into its existing defensive identity;
- it does **not** fire projectiles while defending;
- the correct answer is to reposition, use heavy poise pressure or wait for the opening rather than hit an invisible damage-reduction buff.

If poise-broken during Living Shell, the stance ends immediately.

### Runtime Living Shell authority

Code state `ed91f8ff3af99db5a6d2372ed2bc6a175d2c8e17` binds the canon-closed Living Shell
mechanics into the shared external-actor damage/poise authority without opening Nature Spirit
production spawning:

- the server stores only **post-mitigation canonical HP actually lost** from hostile player damage;
- the trigger window is exactly the prior **80 ticks / 4.0 s**;
- the shell may be selected at the next explicit Nature Spirit decision when that rolling damage is
  at least **20% MaxHP** or current poise is at or below **40%**;
- while active, the shared project target snapshot applies **0.65x direct damage taken** and the
  shared project poise application applies **1.25x poise damage taken**;
- the ordinary elite poise-break damage window still composes through the same central snapshot
  rather than being replaced by a donor rule;
- an actual project poise break immediately calls the Nature Spirit controller and terminates the
  shell without granting the natural-exit forced Bloom Quake;
- natural expiry preserves the existing controller contract: on the next legal decision, an eligible
  target within **4.0 blocks** forces Bloom Quake;
- donor natural spawns and raw verification fixtures do not receive this authority because the
  runtime requires the exact `threateningly_mobs:nature_hamony` registry plus project
  `authored_spawn` tag.

Build Openworld RPG run `37016332713` is **SUCCESS**: tests/build, pinned creature/dependency
inspection, core/gameplay dedicated-server smoke, gameplay client startup, both verification JARs,
mrpack packaging and artifact upload all passed. Artifact:
`openworld-rpg-m0-ed91f8ff3af99db5a6d2372ed2bc6a175d2c8e17`
(`11230471423`, SHA-256
`a7c5d216631be9ef74eba8a245ec68c677b4d93a1dd28ff2c2991797b9ffda47`).

This is **backend authority closure**, not an in-world Nature Spirit encounter playtest. Rooted Swipe,
Earthen Ram and Bloom Quake movement/telegraph/contact presentation remain production-gated.

### Runtime attack-impact authority

Code state `beed315775ae3589086d4a57ea49f22ef3cf817e` binds all three canon-closed Nature
Spirit impact contracts behind one presentation-confirmed server authority:

- only exact `threateningly_mobs:nature_hamony` with the project `authored_spawn` tag can use it;
- donor natural spawns and raw verification fixtures are rejected even though their registry/stat
  profile is known;
- `Rooted Swipe` resolves the locked 13% same-Lv benchmark physical hit with medium guard pressure
  and normal guard/perfect-guard handling;
- `Earthen Ram` resolves the locked 22% physical hit with heavy guard pressure plus authored player
  poise pressure **45**;
- `Bloom Quake` resolves the locked 25% physical hit as unguardable/un-perfect-guardable plus
  authored player poise pressure **45**;
- Cave Centipede action ids are rejected by this authority rather than being accepted by enum shape
  alone;
- presentation code still owns whether the visible frontal arc / 3-block committed ram / 4-block
  quake ring actually contacted a player. Donor damage magnitude never crosses the project boundary.

The authority intentionally reuses the same player incoming-damage and player-poise runtimes already
used by the rest of project combat. No separate Nature Spirit armor, dodge or poise interpretation is
invented.

Build Openworld RPG run `37085239391` is **SUCCESS**: tests/build, pinned creature/dependency
inspection, core/gameplay dedicated-server smoke, gameplay client startup, both verification JARs,
mrpack packaging and artifact upload passed. Artifact:
`openworld-rpg-m0-beed315775ae3589086d4a57ea49f22ef3cf817e`
(`11260552298`, SHA-256
`fad3605f152aaf85654196d79362d08fe97eee851a5b0c67758acbe31f03fbee`).

This closes the three attacks' **impact authority**, not their visible geometry/movement/presentation
binding and not Nature Spirit production spawning.

### Runtime action-execution authority

Code state `c935da82645ed9b0d007ecb66ffd850b21ca5263` adds the server-owned execution gate
between action selection and impact authority:

- selecting a Nature Spirit attack now commits one server action-counter instance; another decision
  cannot be selected while that action is in wind-up / impact / authored recovery;
- Rooted Swipe opens its one contact frame exactly **9 ticks** after selection;
- Earthen Ram opens its contact frame exactly **15 ticks** after selection, preserves the canon-locked
  **3.0-block committed movement requirement** as presentation metadata, and holds its **17-tick**
  authored recovery before another decision;
- Bloom Quake opens its area-contact frame exactly **20 ticks** after selection, preserves the
  canon-locked **4.0-block radius** as presentation metadata, and holds its **18-tick** authored
  recovery;
- Rooted Swipe has no additional authored recovery in the data; its impact frame is still a distinct
  server tick, preventing pre-tell contact from resolving;
- presentation binders can consume a target only on the exact committed impact tick and only once for
  that target/action-counter pair;
- area attacks may confirm multiple different players on the same impact tick, so Bloom Quake does
  not collapse into a single-target shortcut;
- the old direct impact entry point is no longer the production seam: runtime callers must pass
  through the scheduled action instance before `R01NatureSpiritImpactAuthority` can resolve damage;
- no ram interpolation, frontal-arc shape, ring VFX or donor animation is fabricated by this backend.
  Those visible pieces must later match the already-locked timing/space metadata.

Build Openworld RPG run `37092771895` is **SUCCESS**: tests/build, pinned creature/dependency
inspection, core/gameplay dedicated-server smoke, gameplay client startup, both verification JARs,
mrpack packaging and artifact upload all passed. Artifact:
`openworld-rpg-m0-c935da82645ed9b0d007ecb66ffd850b21ca5263`
(`11263037838`, SHA-256
`849d014d0a410dd618da52a012f8a933f3a24a07abd2cda46ac03b864f519572`).

This closes **selection -> wind-up -> scheduled impact -> recovery** backend authority. Actual
player-facing movement/telegraph/contact geometry and Nature Spirit production spawning remain gated.

### Bloom Quake server area authority

Code state `f920385919b7f7b752fa3e6ffea20b6cfc91e54b` additionally closes the part of
Bloom Quake geometry that is already exact in canon:

- the impact frame still comes only from the committed action-counter execution state;
- the server area is an exact **4.0-block horizontal radius** centered on the Nature Spirit at impact;
- points exactly on the 4.0-block boundary are included; points beyond it are rejected;
- only candidate players supplied by the encounter/presentation binder are examined, so unrelated
  nearby players are not auto-enrolled into an encounter merely because they stand near the actor;
- dead, spectator, cross-level or out-of-radius candidates are rejected;
- each accepted candidate still passes through the scheduled-impact target dedupe and the existing
  Nature Spirit impact authority before damage/poise resolution;
- this does not fabricate the visible ground/root ring. Final presentation must render a ring that
  matches this same 4.0-block server radius.

Build Openworld RPG run `37093321135` is **SUCCESS**: tests/build, pinned creature/dependency
inspection, core/gameplay dedicated-server smoke, gameplay client startup, both verification JARs,
mrpack packaging and artifact upload all passed. Artifact:
`openworld-rpg-m0-f920385919b7f7b752fa3e6ffea20b6cfc91e54b`
(`11263683026`, SHA-256
`24de3a4a26d7dd6dbb10a954f2e9952d39457ca30dabfafaf6e0f451a8d566e3`).

Bloom Quake's server timing + radius + impact authority are therefore closed. Its visible ring/VFX and
Nature Spirit production spawning remain gated.

## Attack 3 — Bloom Quake

Used mainly after leaving Living Shell or when surrounded.

```text
telegraph: 1.00 s visible ground/root ring
radius: 4.0 blocks
benchmark damage: 25%
guardable: false
perfect_guardable: false
poise pressure: heavy
recovery: 0.90 s
```

The server hit area must match the visible external ground decal/ring. This is an earth/root burst, not a ranged projectile.

## Status relations

- Poison buildup: strongly resistant;
- Fire direct damage: weak (1.15x);
- Magic Resistance is deliberately higher than physical Defense;
- no blanket immunity to Chilled/Bleed/Frostbite.

## Rewards

- elite 30% gear roll;
- Healing Herb 1–2 at 60%;
- Verdant Crystal 1 at 35%;
- Gold: **20 guaranteed**;
- EXP ~6%;
- Class XP ~5%.

No new `Nature Shell` currency is admitted in R01.

### Runtime reward-authority state

The server-owned personal reward bridge is implemented at
`199d82437a854603a7443ea9ad7d9a00fc10b09f`.

- one accepted contribution fixes the player's reward class for that exact Nature Spirit instance;
- each eligible player receives an independent persisted reward plan;
- equipment/material RNG is committed before delivery, so reconnect cannot reroll it;
- the equipment base family is equal-weight across Initiate Staff / Initiate Wand /
  Apprentice Focus / River Scholar Garb / Greenwater Pendant;
- River Scholar Garb resolves Head / Chest / Legs / Gloves / Boots at equal 1/5;
- ordinary grade and source-Lv ±2 Item-Lv rules remain the global `LOOT_ECONOMY.md` rules;
- EXP/Class XP/Gold use the existing idempotent personal reward transaction;
- Healing Herb / Verdant Crystal delivery is reconnect-safe through Material Pouch receipts;
- a successful equipment roll remains pending instead of inventing a sell/materialization value while
  the ordinary Exalted equipment economy contract is still unclosed.

This does **not** make Nature Spirit production-ready. Reward recording/finalization is accepted only
for an authored project spawn, and no Nature Spirit authored production spawn has been promoted yet.
Its attack-specific presentation also remains blocked because the pinned donor exposes no accepted
Rooted Swipe / Earthen Ram / Bloom Quake animation surface.

Build Openworld RPG run `36975176112` is SUCCESS for this backend state and produced artifact
`openworld-rpg-m0-199d82437a854603a7443ea9ad7d9a00fc10b09f`
(`11213159221`). This is not an in-world Nature Spirit playtest.

The encounter-linked support bridge is additionally bound at
`efb63e028f5db5f0a802ff499b4457a693dfd668`:

- one real skill heal qualifies only after restoring missing HP to another actively engaged player
  through an explicit Nature Spirit encounter actor;
- one barrier qualifies on a positive effective grant to another actively engaged player through the
  explicit Nature Spirit encounter-linked barrier path; it does not need to wait for later damage
  absorption, matching `PARTY_MULTIPLAYER.md`;
- self-heal/self-barrier, full-HP no-op heal, zero-effective barrier, party membership and proximity
  alone do not qualify;
- the Nature Spirit reward service still independently rejects non-Nature-Spirit actors and
  non-authored spawns;
- the same explicit barrier API also closes the previously missing Earthloong barrier-support
  participation boundary without changing its existing reward ownership rules;
- control/debuff/revive participation stays open until those concrete runtime actions exist.

Build Openworld RPG run `37003375371` is **SUCCESS** for this support bridge and produced artifact
`openworld-rpg-m0-efb63e028f5db5f0a802ff499b4457a693dfd668`
(`11224598410`). This still is not an in-world Nature Spirit encounter playtest and does not open
Nature Spirit production spawning.

A concrete non-damaging control contribution path is additionally bound at corrected code state
`6fe9862b489d154cb36c96464cd7d8ca5f2b0dd1`:

- Mage Phase Step's Weave field records support only after the Phase-field slow is actually accepted
  and is not suppressed by a stronger movement control;
- one Weave field publishes at most once per target, so its two-tick refresh does not manufacture
  repeated participation;
- Guardian `Provoked` records control support only on a first accepted controller or a controller
  ownership change; same-Guardian refreshes do not republish;
- exact target routing is fail-closed: Earthloong uses its exact registry, while Nature Spirit requires
  both `threateningly_mobs:nature_hamony` and the accepted project authored-spawn tag;
- damaging skills that also carry a debuff still qualify normally through their real damage path; no
  separate fake contribution is required;
- revive participation remains open because no concrete encounter-linked revive caller is admitted yet.

The first CI for this pass, run `37007134482`, stopped at `compileJava` because the bridge referenced
a non-existent Phase-field accessor. The correction uses the already-existing
`suppressedByStrongerControl()` result contract and changes no authored combat value. Build Openworld
RPG run `37007506464` is **SUCCESS** for the corrected state and produced artifact
`openworld-rpg-m0-6fe9862b489d154cb36c96464cd7d8ca5f2b0dd1`
(`11226493360`). Tests/build, R01 creature/dependency inspection, core/gameplay server smoke, gameplay
client startup, both verification JARs and mrpack packaging passed. This does not promote Nature Spirit
production spawning or resolve its attack-presentation gate.

---

# 19. Regalhart — optional R01 field boss

## Encounter target

```text
Lv: 8
HP: 6,600
Defense: 29
MR: 23
Poise: 180
solo active TTK: ~180 s
```

Regalhart is an optional skill-check/hunt, not required for first-dungeon progression.

External identity preserved:

- huge deer silhouette;
- antlers are a core readable weapon/weak-point feature;
- donor history explicitly supports a special low-HP ability.

## Weak point

Head/antler region:

```text
WeakPointMultiplier: 1.25x
```

The weak point is easiest to punish after a missed charge or major committed attack, not while standing safely at range forever.

## Attack 1 — Antler Sweep

```text
wind-up: 0.45 s
wide frontal/side arc
benchmark damage: 11%
guard pressure: medium
guardable/perfect_guardable: true
recovery: 0.40 s
```

Antler Sweep follow-up is deterministic: above 40% HP, **every third** Antler Sweep chains a second mirrored sweep when the target remains in a legal follow-up arc; at <=40% HP, **every second** Antler Sweep does so. A skipped follow-up because no legal target exists still advances the sequence counter. Total two-hit combo budget stays <=22% benchmark HP.

## Attack 2 — Crown Charge

```text
pre-tell: 0.90 s
path: up to 16 blocks
damage: 28%
guard pressure: heavy
player poise pressure: 70
perfect_guardable: true
perfect_guard_poise_multiplier: 1.25
recovery on miss: 1.20 s
```

Steering becomes strongly limited after commitment.

## Attack 3 — Rear Kick

Anti-backside camping tool.

```text
wind-up: 0.40 s
rear cone
damage: 12%
guardable/perfect_guardable: true
recovery: 0.35 s
```

## Attack 4 — Royal Bound

```text
jump/landing tell: >=1.10 s
landing radius: 4.5 blocks
damage: 30%
guardable: false
perfect_guardable: false
recovery: 1.10 s
```

The landing marker appears only after the physical leap begins; Regalhart does not teleport.

## Sovereign state — <=40% HP

One-time transition:

```text
duration: 1.50 s roar/antler-light tell
invulnerability: none
ordinary damage reduction during transition: 50% only
```

After transition:

- movement speed +10%;
- every **second** Sovereign-state `Crown Charge` attempts one second charge when a legal >=6-block committed line exists after the pivot;
- if no legal line exists, the chain is skipped but the sequence counter still advances;
- second charge has its own >=0.65 s turn/tell;
- chained charge ends with 1.40 s recovery;
- Antler Sweep follow-up timing becomes slightly tighter, but no hidden damage multiplier is added.

This is the project adaptation of the donor's low-HP special behavior: more pattern pressure, not an HP-sponge stat steroid.

## Status relations

- Poison buildup: resistant;
- Bleeding buildup: neutral;
- Frostbite/Shocked: neutral;
- direct elements neutral.

## Rewards

R01 first-defeat boss EXP/Class-XP is intentionally elevated above the later-region baseline as part of the one-time opening progression ramp. Repeat rewards stay on the normal global band.

First eligible defeat:

- 2 Regalhart Antlers;
- guaranteed Superior+ normal gear;
- 15% direct Mythic roll from the exact one-item R01 launch signature pool: `Hartcrown Spear`;
- Gold: **70**;
- EXP **40%** current next-Lv requirement;
- Class XP **30%** current Class Rank requirement.

Repeat:

- 1 Regalhart Antler;
- guaranteed normal gear;
- 25% second normal gear roll;
- 15% direct Mythic roll;
- Gold: **35**;
- EXP ~7%;
- Class XP ~6%.

Personal loot rules remain canonical even though the boss entity/HP is shared in multiplayer.

---

# 20. Earthloong — R01 first dungeon boss

## Encounter target

```text
Lv: 8
HP: 4,900
Defense: 45
MR: 35
Poise: 190
solo active TTK: ~134 s
```

This stays inside the canonical 120–150 s active-damage target for the first dungeon boss.

External identity preserved:

- Earthloong's long earth-dragon/reptilian body;
- forest/earth identity;
- established lightning attack identity;
- donor history includes a special underground forest-dungeon Earthloong;
- donor block-breaking spectacle is **not** allowed to grief the authored RPG world indiscriminately.

## Arena rule — authored breakables only

Any Earthloong attack with environment-breaking spectacle may break only:

```text
r01_earthloong_breakable_prop
```

tagged arena props.

It may **not** destroy arbitrary Azari terrain, player housing, storage or dungeon-critical navigation blocks.

## Phase 1 — 100% to 55%

### Claw Sweep

```text
wind-up: 0.45 s / 9 ticks
damage: 10%
guard pressure: medium
guardable/perfect_guardable: true
horizontal attack arc: absolute facing angle 0°..120°
```

### Tail Scythe

```text
wind-up: 0.65 s / 13 ticks
wide rear/side arc
damage: 20%
guard pressure: heavy
guardable/perfect_guardable: true
recovery: 0.65 s / 13 ticks
horizontal attack arc: absolute facing angle 60°..180°
```

Angle convention for these two physical arcs is horizontal: `0° = directly forward`, `90° = side`, `180° = directly rear`. The **60°..120° flank overlap is intentional**. On the side of the body both attacks may be spatially legal and the locked weighted selector decides which committed action occurs; there is no artificial angle seam where neither move is valid.

### Quarry Rush

```text
pre-tell: 0.80 s / 16 ticks
forward path: 9 blocks
start legality: target 5.0..9.0 blocks + clear committed line
damage: 24%
guard pressure: heavy
perfect_guardable: true
recovery: 0.90 s / 18 ticks
```

### Lightning Furrow

Signature ranged/space-control attack.

```text
telegraph: 1.20 s luminous body/horn cue + ground lane decals
lanes: 3
lane width: 1.4 blocks
lane length: up to 12 blocks
damage per player per wave: 26% benchmark HP max
channel: magic/lightning
guardable: false
perfect_guardable: false
Shock buildup: 35
recovery: 1.00 s
```

A player cannot be hit by overlapping lanes from the same wave more than once.

Exact Phase-1 lane geometry is HARD_RULE:

```text
commit forward axis:
  horizontal normalized vector from Earthloong center to the selected current target
  sampled once when Lightning Furrow commits

3-lane lateral center offsets:
  -2.50 / 0.00 / +2.50 blocks

lane half-width:
  0.70 blocks

longitudinal hit interval:
  0.00 .. 12.00 blocks from Earthloong horizontal center along the committed axis
```

The target may move after commit; the lane axis does **not** retarget.

For the Phase-2 4-lane pattern, keep the same 2.50-block center spacing without adding a center lane:

```text
4-lane lateral center offsets:
  -3.75 / -1.25 / +1.25 / +3.75 blocks
```

Ground-lane binding rules:

- server hit geometry and visible ground decals use the same committed axis/offsets/width;
- a solid arena wall/closed collision barrier terminates the lane beyond that obstruction; the damage rectangle does not pass invisibly through walls;
- each visible lane segment is ground-projected to the local traversable arena surface;
- a player is eligible for a lane hit only when their feet are within **1.25 blocks vertically** of that local projected surface;
- a local floor discontinuity greater than **1.5 blocks** between adjacent projected samples breaks that segment rather than bridging a vertical cliff/ledge;
- visual sampling may be finer than gameplay sampling, but it may not visually imply a safe gap where the server still damages.

These values create real dodge corridors between 1.4-block lanes while preserving a compact first-boss arena pattern. Playtest may revise the canon values later, but source code does not choose different spacing first.

### Root Breaker

```text
telegraph: 1.00 s
radius: 4.5 blocks
damage: 30%
guardable: false
perfect_guardable: false
player poise pressure: 75
recovery: 1.00 s
```

Breaks only tagged arena props.

## Phase transition — 55% HP

`Stormshed`:

```text
duration: 1.40 s
invulnerability: none
damage reduction during transformation: 50%
```

Visible lightning moves across the accepted Earthloong body anchors before phase-2 attacks become legal.

No cinematic untargetable wait.

Phase-boundary ownership is exact:

- crossing <=55% HP during a committed attack does **not** cancel that attack, its impact or its recovery;
- set a pending Stormshed transition immediately when the threshold is crossed;
- after the current committed action reaches its normal recovery end, enter Stormshed before another attack decision;
- Stormshed lasts exactly **28 ticks**;
- no new attack is selected during those 28 ticks;
- Earthloong remains targetable and takes the listed 50% damage reduction;
- threat, cooldown clocks, `action_counter` and anti-repeat history continue and are not reset;
- Phase 2 becomes attack-legal only after the 28th transition tick completes.

## Phase 2 additions

### Forked Heaven

```text
telegraph: 1.10 s / 22 ticks per target marker
marker radius: 1.8 blocks
marker creation offsets: 0 / 8 / 16 ticks after cast commit
impact offsets: 22 / 30 / 38 ticks after cast commit
recovery after third impact: 1.00 s / 20 ticks
3 lightning impacts distributed around engaged player positions
damage per impact: 18%
channel: magic/lightning
Shock buildup: 25
same-player same-cast hit cap: 1 unless the player intentionally moves into a later separately telegraphed marker
```

Forked Heaven marker ownership/timing is exact:

- marker assignments are chosen from valid engaged participants sorted by current SelectionThreat, with UUID lexical order only as the deterministic tie-break;
- 1 player: assignment order `P1 / P1 / P1`;
- 2 players: assignment order `higher threat / lower threat / higher threat`;
- 3+ players: the first three current SelectionThreat players receive markers 1/2/3 respectively;
- each marker samples its assigned player's current horizontal position **when that marker is created**, ground-projects there, and then stays fixed until impact;
- a marker is not silently retargeted after creation;
- if its assigned participant becomes invalid before that marker is created, choose the next currently valid participant by SelectionThreat; if none exists, skip that marker;
- after a player was already hit by this cast, a later marker may hit them again only when they were outside that later marker when it was telegraphed and then moved into it before impact;
- marker/player vertical tolerance is **1.25 blocks**;
- the 20-tick recovery begins after the third authored impact time even when a marker was skipped.

### Earthline Surge

Two-part readable sequence:

1. physical ground/body surge;
2. delayed lightning trace along the visible same line.

```text
commit telegraph: 0.80 s / 16 ticks
committed line width: 1.8 blocks
committed line length: 9.0 blocks
physical hit offset: tick 16
physical hit: 14%
physical guardable/perfect_guardable: false/false
lightning follow-up offset: tick 27
lightning follow-up: 16%
lightning guardable/perfect_guardable: false/false
second-hit delay: 0.55 s / 11 ticks
Shock buildup from lightning: 20
recovery after lightning: 0.90 s / 18 ticks
```

Earthline geometry is HARD_RULE:

- commit one horizontal axis from Earthloong center toward the selected current target;
- the target may move after commit; neither physical nor lightning line retargets;
- the same visible/projected line owns both hit checks;
- use the same local-ground projection rules as Lightning Furrow: 1.25-block player vertical tolerance and break the segment across >1.5-block floor discontinuity;
- solid arena collision terminates the line beyond that obstruction;
- both hits resolve their own dodge window; landing/dodging the first does not erase the delayed second hit;
- Shock buildup is added only when the lightning follow-up itself produces an accepted contact;
- there is no guard/perfect-guard shortcut for remaining inside the line.

The point is to dodge **out of the line**, not iframe one event while standing in the hazard.

### Lightning Furrow phase-2 change

- the **first** phase-2 Lightning Furrow uses 4 lanes;
- later phase-2 uses alternate **3 lanes → 4 lanes → 3 lanes → 4 lanes**;
- lane width/damage do not increase;
- recovery remains a real punish window;
- overlapping-lane single-wave hit cap remains.

## Weak-point window

During `Lightning Furrow` and `Forked Heaven` charge-up, the visibly luminous head/crest is a weak point:

```text
WeakPointMultiplier: 1.25x
poise damage received from weak-point hits: +20%
```

This rewards aggressive positioning instead of only waiting outside every spell.

## Status relations

Direct:

- Lightning: strongly resistant (0.70x);
- Frost: weak (1.15x);
- Fire: neutral.

Buildup:

- Shocked: strongly resistant (`1.60x` threshold);
- Poisoned: strongly resistant;
- Bleeding: resistant;
- Frostbite: neutral.

Earthloong is **not immune** to its own element's status system by a hidden blanket rule; it is simply hard to proc and resistant to direct Lightning.

## Rewards

The first Earthloong clear uses the R01 one-time progression ramp. Repeat clears use ordinary repeat percentages.

First eligible dungeon clear:

Boss kill layer:

- guaranteed Superior+ normal gear;
- Earthloong Scale x2;
- 15% direct Mythic roll from `Rootquake Maul / Earthscale Ward` pool;
- boss EXP **40%** current next-Lv requirement;
- boss Class XP **20%** current Class Rank requirement.

Dungeon-completion layer:

- choose one Superior Item Lv8: `Ironroot Longsword / Riverthorn Bow / Lumenwood Staff`;
- EXP: **100%** current next-Lv requirement from completion + the exact 40% boss layer above;
- Class XP: **64%** current Class Rank requirement from completion + the exact 20% boss layer above;
- Gold: **180 total first-clear Gold**; no separate extra Earthloong kill Gold.

Repeat:

- completion guaranteed normal gear;
- boss 60% additional normal gear;
- Earthloong Scale x1;
- 15% Mythic roll;
- repeat completion EXP **22%** + **7%** boss contribution;
- Class XP **15%** completion + **6%** boss;
- Gold: **90**.

These values remain subject to actual playtest feel, but implementation starts from these numbers rather than inventing them.

---

# 21. R01 enemy attack readability contract

All attacks above inherit `COMBAT_BALANCE.md` telegraph rules.

Additional R01 teaching goals:

- Meadow Viper teaches **warning → dodge/step away → buildup**;
- Cave Centipede teaches vertical awareness and poison without a boss-sized HP bar;
- Steelboar teaches committed charge steering and perfect-guard payoff;
- Nature Spirit teaches that defense can be beaten by poise/positioning rather than DPS spam;
- Regalhart teaches weak points + phase pattern escalation;
- Earthloong combines physical tells, ground danger, lightning spacing and boss poise without long untargetable phases.

R01 must not introduce all endgame mechanics at once.

## 21.1 Exact R01 action-selection controller

R01 attack lists are not a bag of moves for implementation to choose arbitrarily. The server owns action selection.

At the end of an attack/recovery state:

1. wait **0.10 s** decision delay;
2. validate current target through `COMBAT_BALANCE.md` threat rules;
3. build only the actions whose range/angle/cooldown/terrain conditions are legal;
4. apply the actor rules below;
5. when more than one weighted action remains, choose through deterministic server RNG seeded by `encounter_or_spawn_instance_id + actor_id + action_counter`;
6. increment `action_counter` once after a committed action.

General anti-repeat rule:

- the same ordinary attack cannot be selected more than **2 committed actions in a row** when another legal attack exists;
- a signature movement/space-control action cannot be chosen twice in a row unless the second use is an explicitly authored combo/follow-up;
- if only one legal attack exists, it may repeat;
- a committed telegraph is never cancelled merely because another action became preferable.

Distance checks use horizontal target distance unless an attack explicitly requires vertical geometry.

### Meadow Viper

Only `Coil Bite` exists.

- if target is inside the 3.5-block warning zone but outside 2.2-block bite reach, Viper faces/holds/coils and closes only enough to establish legal bite reach;
- after >=0.45 s warning and legal <=2.2-block reach, Coil Bite commits;
- after recovery, Viper creates the authored ~2-block spacing before another bite decision;
- no random alternate attack is invented.

### Cave Centipede

Cooldown:
```text
Ceiling Drop: 8.0 s
```

Eligibility/weight:

| Condition | Action | Weight |
|---|---|---:|
| physically above target on valid climb geometry, horizontal distance <=2.3, Ceiling Drop ready | Ceiling Drop | 100 |
| ground/wall melee distance <=2.3 | Scuttle Bite | 60 |
| ground/wall distance <=3.2 | Body Rake | 40 |

If Ceiling Drop is legal, its row joins the weighted set rather than teleporting/forcing an invalid drop. If no attack is legal, the Centipede navigates to a legal local attack position.

### Bison

Cooldown:
```text
Herd Charge: 7.0 s
```

- <=2.6 blocks: Headbutt;
- 4.0–10.0 blocks + clear committed line + Charge ready: Herd Charge;
- between these bands or while Charge is unavailable: close/reposition;
- a Bison does not circle-strafe or repeatedly back away to manufacture charge distance.

### Grizzly

On first territorial engagement:

- if aggression began only from territory violation, perform Warning Roar once before the first damaging attack;
- if the player directly damaged the Grizzly first, Warning Roar is skipped for that engagement.

Melee selection <=3.2 blocks:

```text
Paw Swipe: weight 65
Maul Sequence: weight 35
```

Maul Sequence:
- cooldown **5.0 s**;
- cannot be chosen twice consecutively.

Outside 3.2 blocks the Grizzly closes distance; it does not gain an unlisted leap/charge.

### Steelboar

Cooldowns:
```text
Iron Rush: 7.0 s
Shoulder Hook: 3.0 s
Furious Route override: 14.0 s as already defined
```

Selection:

- 5.5–12.0 blocks + clear line + Iron Rush ready: Iron Rush has priority over ordinary melee;
- <=3.0 blocks:
  - Iron Tusk weight 60;
  - Shoulder Hook weight 40 when ready;
- 3.0–5.5 blocks with no legal Rush: close/reposition.

Below 35% HP, the existing Furious Route rule replaces the next legal Iron Rush when its override cooldown is ready. It does not create a separate random roll.

### Nature Spirit

Living Shell trigger is exact:

```text
if Living Shell reuse ready
AND (
  received >=20% MaxHP as hostile post-mitigation damage during the previous 4.0 s
  OR current Poise <=40% PoiseMax
)
→ next legal decision enters Living Shell
```

Living Shell lasts **2.5 s** unless poise-broken earlier.

After a natural 2.5 s Living Shell end:
- if an eligible target is within 4.0 blocks, the next action is Bloom Quake;
- otherwise normal selection resumes.

Cooldowns:
```text
Living Shell reuse: 12.0 s
Bloom Quake after non-Shell use: 8.0 s
Earthen Ram: 4.0 s
```

Normal selection:

| Condition | Action | Weight |
|---|---|---:|
| <=3.2 blocks | Rooted Swipe | 60 |
| 2.5–5.0 blocks + Ram ready | Earthen Ram | 40 |
| <=4.0 blocks + Bloom Quake ready and not forced by Shell exit | Bloom Quake | 20 |

If Bloom Quake is selected, it cannot be the next action again even if only its cooldown was externally reset.

### Regalhart

Cooldowns:
```text
Rear Kick: 3.0 s
Crown Charge: 7.0 s
Royal Bound: 9.0 s
```

Rear protection:
- target inside validated rear attack arc and <=3.5 blocks + Rear Kick ready → Rear Kick has priority.

Front/side close:
- <=4.5 blocks → Antler Sweep unless rear-protection rule owns the action.

Mid/far selection when both are geometrically legal:

| Target distance | Crown Charge | Royal Bound |
|---|---:|---:|
| 4.5–7.0 | 45 | 55 |
| 7.0–12.0 | 60 | 40 |
| 12.0–16.0 | 75 | 25 |

**Exact-boundary note:** the current table text overlaps at exactly `7.0` and `12.0` blocks
without stating which adjacent row owns those two exact values. Until that tiny canon boundary is
explicitly normalized, the runtime must fail closed/reposition at exactly those two floating-point
distances rather than silently choosing one probability table. This does not affect the open
intervals on either side.

An unavailable/cooling action is removed and weights are renormalized.

Royal Bound requires a legal landing volume within its authored movement envelope. Crown Charge requires a clear committed path. If neither is legal, Regalhart closes/repositions instead of teleporting.

Sovereign-state internal second-charge and Antler-Sweep follow-up counters remain the deterministic rules already defined above; they are combo internals and do not roll a second action choice.

#### Runtime Regalhart selection / Sovereign authority

Code state `1291553781db71966e14858ac84c8ac1d4ff4c4c` binds the canon-closed server
selection rules without opening Regalhart production spawning:

- exact dependency identity remains `threateningly_mobs:the_regalhart`, content Lv8;
- rear protection owns the decision first: legal rear arc + <=3.5 blocks + Rear Kick ready selects
  Rear Kick before close-front logic;
- front/side <=4.5 blocks selects Antler Sweep when rear protection does not own the action;
- Crown Charge uses the exact **140-tick / 7.0 s** cooldown and requires the caller to confirm a clear
  committed path;
- Royal Bound uses the exact **180-tick / 9.0 s** cooldown and requires the caller to confirm a legal
  landing volume;
- mid/far weights remain exactly **45/55**, **60/40**, **75/25** in the authored distance bands;
  cooling/illegal actions are removed before deterministic server RNG;
- exact 7.0 and 12.0 blocks remain intentionally fail-closed/reposition because the current table
  overlaps those two endpoints and does not yet assign them to one adjacent row;
- Crown Charge and Royal Bound are signature movement/space-control actions and cannot repeat
  immediately when the other signature action is also legal; if only one is legal, the general
  single-legal-action exception still permits repetition;
- ordinary anti-repeat remains max two consecutive committed actions when another legal ordinary
  action exists;
- the <=40% HP Sovereign transition begins exactly once and locks attack selection for the full
  **30 ticks / 1.50 s**;
- the transition data preserves **0.50x ordinary damage taken** and post-transition
  **1.10x movement speed** for later runtime binding; this selection pass does not pretend those
  combat/movement multipliers are already applied;
- Antler Sweep sequence cadence is deterministic: every third above 40% HP and every second in
  Sovereign state. A due follow-up with no legal mirrored arc is skipped but the sweep counter still
  advances;
- every second Sovereign-state Crown Charge marks its internal second-charge opportunity due. The
  later charge executor must still establish the authored >=6-block legal line after its
  >=13-tick / 0.65 s pivot tell;
- weak-point multiplier **1.25x** is stored as canon data only; head/antler hit geometry is not
  fabricated in this pass.

This pass also preserves real impact gaps rather than guessing them. Antler Sweep does **not** invent
player-poise pressure merely from its medium guard band; Crown Charge ordinary `guardable` is still
unstated; Rear Kick's guard-pressure/player-poise values are unstated. Therefore this is selection /
phase authority, not a claim that all Regalhart impacts are source-ready.

Build Openworld RPG run `37096585105` is **SUCCESS**: tests/build, pinned creature/dependency
inspection, core/gameplay dedicated-server smoke, gameplay client startup, both verification JARs,
mrpack packaging and artifact upload all passed. Artifact:
`openworld-rpg-m0-1291553781db71966e14858ac84c8ac1d4ff4c4c`
(`11264443479`, SHA-256
`90be1d6a68845b60a53c3de47d6868a91ea15b5f79f26a64153b33d469511890`).

#### Runtime Regalhart closed-impact authority

Code state `2a182a9708f968e13751d0fe72927c2b19c27323` advances only the Regalhart impacts
whose current canon is complete:

- non-combo Antler Sweep owns the exact **9-tick / 0.45 s** wind-up, one server impact frame and
  **8-tick / 0.40 s** recovery;
- its first hit resolves **11% same-Lv benchmark physical damage**, medium guard pressure and
  `guardable/perfect_guardable=true` through the central project player-defense pipeline;
- no player-poise damage is invented for Antler Sweep because current canon does not state one;
- an Antler Sweep decision whose deterministic mirrored follow-up is due is rejected by this executor
  until the second hit's exact timing is canon-closed, preventing a half-implemented combo;
- Royal Bound's impact contract is independently closed as **30% physical**, unguardable and
  un-perfect-guardable with an exact **4.5-block horizontal landing radius**;
- Royal Bound does **not** receive a fake fixed landing tick: the authored tell is `>=1.10 s` and the
  physical leap must supply the actual landing frame later;
- Royal Bound candidate membership remains caller-owned, while dead/spectator/cross-level/out-of-radius
  targets are excluded by the server area resolver;
- Crown Charge and Rear Kick remain rejected from impact authority because their current impact
  contracts are incomplete;
- exact `threateningly_mobs:the_regalhart` + project `authored_spawn` are required, so natural
  donor spawns and raw verification entities cannot accidentally inherit production combat rules.

Build Openworld RPG run `37098727691` is **SUCCESS**: tests/build, pinned creature/dependency
inspection, core/gameplay dedicated-server smoke, gameplay client startup, both verification JARs,
mrpack packaging and artifact upload all passed. Artifact:
`openworld-rpg-m0-2a182a9708f968e13751d0fe72927c2b19c27323`
(`11265038798`, SHA-256
`74ee7d355b09b148f305a494905df29a6a2f9fc360348641a93740d740868576`).

### Earthloong — shared cooldowns

```text
Quarry Rush: 7.0 s
Lightning Furrow: 8.0 s
Root Breaker: 9.0 s
Forked Heaven: 10.0 s
Earthline Surge: 7.0 s
```

Root Breaker becomes eligible when either:
- at least 2 engaged players are within 4.5 blocks; or
- the current target has remained within 3.0 blocks continuously for >=2.0 s.

Phase 1 weighted set:

| Legal condition | Action | Weight |
|---|---|---:|
| front/side <=3.5 + absolute facing angle 0°..120° | Claw Sweep | 50 |
| validated rear/side arc <=4.5 + absolute facing angle 60°..180° | Tail Scythe | 55 |
| 5.0–9.0 + clear committed line + ready | Quarry Rush | 45 |
| 5.0–12.0 + ready | Lightning Furrow | 35 |
| Root Breaker condition + ready | Root Breaker | 45 |

Phase 2 keeps all legal Phase-1 actions and adds:

| Legal condition | Action | Weight |
|---|---|---:|
| at least one engaged player 4.0–12.0 + ready | Forked Heaven | 40 |
| current target 3.0–9.0 + legal visible line + ready | Earthline Surge | 45 |

Space-control class:
```text
Lightning Furrow
Root Breaker
Forked Heaven
Earthline Surge
```

Anti-spam:
- after **2 consecutive** space-control-class actions, the next committed attack must be Claw Sweep, Tail Scythe or Quarry Rush if one is legal;
- if no physical action is legal, Earthloong repositions until one becomes legal rather than chaining a third full hazard cast;
- phase transition does not reset attack cooldowns or anti-repeat history;
- Lightning Furrow 3/4-lane phase-2 alternation remains exactly as defined above.

Forked Heaven impact ownership:
- 1 engaged player: all 3 authored markers are arranged around that player, but same-cast hit cap remains 1 unless the player intentionally enters a later marker;
- 2 players: marker distribution is higher-threat / lower-threat / higher-threat;
- 3+ players: first 3 valid participants by current SelectionThreat each receive one marker;
- no marker targets a Downed/invalid/out-of-arena player.

These selection rules are HARD_RULE initial behavior. Real playtest may revise canon if a pattern is unfair/repetitive; gameplay code does not invent different weights/cooldowns first.

---

# 22. Spawn / performance constraints

- no every-tick world-wide searches;
- normal R01 mobs use loaded-chunk regional spawn tables and normal entity AI bounds;
- Cave Centipede wall/ceiling logic only evaluates local navigation/context around the entity;
- elite spawn density is capped per local area so two Steelboars + Nature Spirit do not randomly create an accidental raid on the starter road;
- Regalhart uses an authored field-boss controller/eligible respawn rule rather than high-weight natural spawn;
- Earthloong exists only inside an active dungeon run/encounter context in R01;
- boss VFX/event logic is scoped to engaged encounter participants and nearby tracking clients;
- no boss attack searches all players in a dimension.

---

# 23. Multiplayer authority

Server owns:

- spawn/aggro state;
- target selection/threat;
- attack phase;
- hit validation;
- damage/status buildup;
- status proc count/threshold;
- boss HP/poise scaling;
- first-clear state;
- personal reward eligibility;
- signature material and gear reward delivery.

Client may predict animation/telegraph presentation only.

Boss player-count scaling uses the already-locked `COMBAT_BALANCE.md` formulas. R01 enemy outgoing damage never scales upward merely because more players joined.

---

# 24. Data contract

Suggested data ownership:

```text
status/
  rules.json
  burning.json
  chilled.json
  poison.json
  bleeding.json
  frostbite.json
  shocked.json
  class_debuffs.json

elements/
  susceptibility.json

encounters/r01/
  meadow_viper.json
  cave_centipede.json
  bison.json
  grizzly.json
  steelboar.json
  nature_spirit.json
  regalhart.json
  earthloong.json

loot/r01/
  common_creatures.json
  elites.json
  regalhart.json
  earthloong.json
```

## 24.1 Status definition minimum

```text
id
category: direct | buildup
buildup_base_if_any
proc_duration
source_power_rule
damage_channel
element_tag
total_status_coefficient
cleanse_category
stack_policy
refresh_policy
vfx_binding
icon_binding
```

## 24.2 Enemy definition minimum

```text
id
external_source_binding
encounter_level
role
spawn_profile
aggro_profile
max_hp
defense
magic_resistance
poise_max
element_susceptibility
ailment_resistance
weakpoints[]
attacks[]
phase_rules[]
loot_profile
exp_profile
class_xp_profile
multiplayer_scaling_profile
```

## 24.3 Enemy attack minimum

Use all existing `COMBAT_BALANCE.md` attack fields plus:

```text
element_tag
status_buildup[]
same_cast_hit_cap
breakable_prop_tags[]
phase_requirements
```

No player-visible implementation may contain `todo_attack`, generic invisible AoE, or a damage event that does not correspond to an accepted animation/VFX tell.

---

# 25. Pre-code external intake for R01 encounters

Before gameplay implementation of these encounters is marked asset-ready:

1. launch the exact current 26.2 Fabric builds of Threateningly Mobs Continued and Alex's Mobs Continued in an isolated test instance;
2. inspect Louxia, Cave Centipede, Steelboar, Nature Spirit, Regalhart and Earthloong actual current model/animation/entity IDs;
3. verify continuation licenses/source repositories because storefront displays currently conflict;
4. do not copy dependency-only textures/models into the public repo if rights remain unclear;
5. download the CC0 Quaternius Snake and record exact asset filename + SHA-256;
6. convert/test Meadow Viper at Minecraft scale, including attack reach and ground contact;
7. record exact attack-animation availability for Steelboar/Nature Spirit/Regalhart/Earthloong;
8. if the dependency's current animation cannot visually support one authored attack, replace that attack with another move that **the accepted external model can actually show**, then revise canon before coding;
9. bind exact VFX/sound sources for Lightning Furrow, Bloom Quake, Regalhart sovereign transition, Poison/Chill/Burning/Shock before calling those encounters presentation-complete;
10. take real Minecraft screenshots/video at gameplay FOV and verify hitbox-to-visual agreement.

This intake may refine technical binding. It may not silently replace external-first creature designs with vanilla mobs or improvised placeholder models.

---

# 26. First implementation acceptance targets

When these systems are eventually coded, verify at minimum:

1. baseline WIL player takes three Meadow Viper bites in a short window to reach Poison, not one accidental graze;
2. buildup visibly decays after 3 s without new contribution;
3. repeated same ailment on a boss requires progressively more buildup and only that ailment's threshold changes;
4. Burning remains direct/refresh behavior exactly as class canon; it is not accidentally converted to buildup;
5. Chilled remains movement-only;
6. Frostbite + direct Fire produces one Thermal Shock and clears active Frostbite without clearing repeat resistance;
7. Shocked never hard-stuns Earthloong/Regalhart;
8. Cave Centipede remains deep quarry/cave and can use wall movement without global scan/pathfinding spikes;
9. Steelboar charge cannot magnetically turn through a 90-degree dodge;
10. Nature Spirit never resurrects its old generic defense-mode projectile behavior;
11. Regalhart phase transition is targetable and adds pattern pressure rather than a large raw-damage steroid;
12. Earthloong lands near 120–150 s solo active TTK at Lv8 benchmark;
13. Earthloong lightning lanes visibly match server hit areas and same-wave overlaps do not multihit one player;
14. Earthloong breaks only tagged arena props;
15. 2-player bosses use 1.65x HP / 1.40x poise and still keep unchanged outgoing damage;
16. personal first-clear/signature rewards cannot be duplicated by relogging or participant-count changes;
17. no vanilla mob leaks into the finished R01 encounter ecology.

---

# 27. What this pass closes

Closed for implementation:

- element tag versus physical/magic mitigation relationship;
- compact enemy element-susceptibility model;
- player/enemy buildup thresholds;
- WIL ailment-resistance role;
- buildup decay;
- repeated-proc resistance;
- multiplayer shared-buildup ownership;
- Poisoned / Bleeding / Frostbite / Shocked exact baseline behavior;
- compatibility with existing Burning / Chilled / class debuffs;
- cleanse categories;
- R01 surface snake selection and removal of Alex's desert Rattlesnake from R01;
- R01 Louxia / Meadow Viper / Cave Centipede / Bison / Grizzly / Steelboar / Nature Spirit / Regalhart / Earthloong combat roles;
- exact R01 enemy Lv/HP/Defense/MR/poise starting stats;
- full signature attack/tell/guard/status behavior for R01 combat threats;
- Regalhart field-boss phase/reward baseline;
- Earthloong first-dungeon boss phases/weak point/reward baseline;
- R01 external dependency/license boundaries;
- exact R01 server-side action-selection weights/cooldowns/anti-repeat rules;
- first implementation acceptance checks.

Still intentionally requires later design/asset work:

- exact current dependency model/entity/animation IDs after local asset-intake inspection;
- exact sound/VFX filenames and hashes;
- R02+ enemy combat kits;
- later special statuses such as sleep/paralysis/curse if a real encounter/build needs them;
- PvP behavior, because PvP is not a baseline product pillar;
- final tuning after real client playtest.
