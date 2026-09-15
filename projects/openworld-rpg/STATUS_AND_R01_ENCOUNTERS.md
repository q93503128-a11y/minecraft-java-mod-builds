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
- Louxia Glow 35%.

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
- ordinary low Gold/consumable table only;
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
- ordinary creature food only if accepted by later cooking catalog;
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

Below 35% HP, once per 14 s maximum:

- Steelboar may perform a two-charge `Furious Route`;
- second charge receives its own >=0.60 s pivot/tell;
- after the second charge, recovery is 1.40 s;
- no permanent attack-speed/damage steroid.

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
- EXP ~6%;
- Class XP ~5%.

No new `Nature Shell` currency is admitted in R01.

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

May chain a second mirrored sweep with a visible body turn; total two-hit combo budget stays <=22% benchmark HP.

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
- `Crown Charge` may chain one second charge;
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

First eligible defeat:

- 2 Regalhart Antlers;
- guaranteed Superior+ normal gear;
- 15% direct Mythic roll from known Regalhart signature pool (`Hartcrown Spear` currently);
- EXP ~20% current next-Lv requirement;
- Class XP ~15% current Class Rank requirement.

Repeat:

- 1 Regalhart Antler;
- guaranteed normal gear;
- 25% second normal gear roll;
- 15% direct Mythic roll;
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
wind-up: 0.45 s
damage: 10%
guard pressure: medium
guardable/perfect_guardable: true
```

### Tail Scythe

```text
wind-up: 0.65 s
wide rear/side arc
damage: 20%
guard pressure: heavy
guardable/perfect_guardable: true
recovery: 0.65 s
```

### Quarry Rush

```text
pre-tell: 0.80 s
forward path: 9 blocks
damage: 24%
guard pressure: heavy
perfect_guardable: true
recovery: 0.90 s
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

## Phase 2 additions

### Forked Heaven

```text
telegraph: 1.10 s per target marker
3 lightning impacts distributed around engaged player positions
damage per impact: 18%
channel: magic/lightning
Shock buildup: 25
same-player same-cast hit cap: 1 unless the player intentionally moves into a later separately telegraphed marker
```

### Earthline Surge

Two-part readable sequence:

1. physical ground/body surge;
2. delayed lightning trace along the visible same line.

```text
physical hit: 14%
lightning follow-up: 16%
second-hit delay: 0.55 s
Shock buildup from lightning: 20
```

The point is to dodge **out of the line**, not iframe one event while standing in the hazard.

### Lightning Furrow phase-2 change

- may use 4 lanes instead of 3;
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

First eligible dungeon clear:

Boss kill layer:

- guaranteed Superior+ normal gear;
- Earthloong Scale x2;
- 15% direct Mythic roll from `Rootquake Maul / Earthscale Ward` pool;
- boss Class XP ~10% current Class Rank requirement.

Dungeon-completion layer:

- choose one Superior Item Lv8: `Ironroot Longsword / Riverthorn Bow / Lumenwood Staff`;
- EXP: **50%** current next-Lv requirement from completion + boss contribution;
- Class XP: **32%** from completion + boss contribution.

Repeat:

- completion guaranteed normal gear;
- boss 60% additional normal gear;
- Earthloong Scale x1;
- 15% Mythic roll;
- repeat completion EXP **22%** + boss contribution;
- Class XP **15%** completion + ~6% boss.

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
- first implementation acceptance checks.

Still intentionally requires later design/asset work:

- exact current dependency model/entity/animation IDs after local asset-intake inspection;
- exact sound/VFX filenames and hashes;
- R02+ enemy combat kits;
- later special statuses such as sleep/paralysis/curse if a real encounter/build needs them;
- PvP behavior, because PvP is not a baseline product pillar;
- final tuning after real client playtest.
