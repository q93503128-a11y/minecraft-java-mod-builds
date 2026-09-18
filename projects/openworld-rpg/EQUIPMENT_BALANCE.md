# Open-World RPG — Equipment Scaling / Forge / R01 Visual Catalog

> Status: subordinate design reference  
> Master canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Loot rules: `LOOT_ECONOMY.md`  
> UI rules: `UI_DIRECTION.md`  
> Scope: base equipment scaling, concrete affix values, forge behavior, R01 resources, R01 equipment families and their external visual sources.  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins.

This file closes the equipment-value and first-region content decisions that must not be invented during implementation. Code should translate these formulas/tables into data-driven runtime rules. If a hard implementation constraint invalidates one of these rules, update the canon first instead of silently redesigning it in Java.

---

# 1. External precedents used

The project uses precedent structurally rather than copying proprietary art or numbers.

## Grim Dawn

Useful parts:

- a blacksmith is primarily a reliable gap-filling/crafting service rather than an endless mandatory `+1 -> +20` treadmill;
- monster-specific materials and Monster Infrequents make enemy identity matter to loot;
- rerolling and crafting can improve a build without making random world loot irrelevant.

Project adoption:

- regional materials feed deterministic forge recipes;
- boss-derived materials visibly protect against bad luck;
- one bounded affix-replacement action exists on high-grade random gear;
- there is no universal reinforcement-level grind.

## Diablo IV

Useful part:

- recent itemization explicitly moved Tempering toward choosing a desired affix rather than repeatedly accepting random unwanted results.

Project adoption:

- the player chooses which existing affix slot to replace;
- the forge never rerolls every good line because one line is bad;
- no multi-layer Tempering/Masterworking currency stack is copied.

## Guild Wars 2

Useful parts:

- gear is separated into visible equipment slots around a character presentation;
- item quality, level and slot determine stat magnitude;
- upgrades can create build identity without requiring every item to climb a long numeric enhancement ladder.

Project adoption:

- Item Lv and base archetype own core power;
- grade mainly controls affix count/quality rather than multiplying all base stats;
- later build depth comes from affixes, named gear and class synergy rather than generic enhancement spam.

---

# 2. Locked external visual families

These are design inputs, not loose inspiration.

| Use | Source | License/status | Project role |
|---|---|---|---|
| ordinary martial weapons / shields | Quaternius `Modular Weapons Pack` — https://quaternius.com/packs/medievalweapons.html | CC0 | primary R01 grounded weapon models |
| additional fantasy weapons, staves, wands | KayKit `Fantasy Weapons Bits` — https://kaylousberg.itch.io/fantasy-weapons-bits | CC0 | primary R01 magic/fantasy weapon supplement |
| resource chunks, logs, ores, crystals, textiles | KayKit `Resource Bits` — https://kaylousberg.itch.io/resource-bits | CC0 | primary R01 resource pickup/model family |
| herbs / plants / rocks / nature nodes | Quaternius `Stylized Nature MegaKit` — https://quaternius.com/packs/stylizednaturemegakit.html | CC0 | primary R01 herb/node visual family |
| fallback forest rocks/bushes/grass | KayKit `Forest Nature Pack` — https://kaylousberg.itch.io/kaykit-forest | CC0 | secondary R01 node/environment family |
| light/medium/heavy player armor silhouettes | Quaternius `Modular Character Outfits - Fantasy` — https://quaternius.com/packs/modularcharacteroutfitsfantasy.html | CC0 | primary editable base for R01 worn armor families |
| ordinary fantasy props / potions / smith props | Quaternius `Fantasy Props MegaKit` — https://quaternius.com/packs/fantasypropsmegakit.html | CC0 | consumable/forge/world-prop supplement |
| equipment frame / rarity / inventory presentation | Foozle `Lucifer - Equipment` | CC0 | icon framing and rarity language |
| inventory/tooltips/forge screen visual language | Foozle `Lucifer - RPG UI` | CC0 | shared screen grammar |
| Louxia / Regalhart / Earthloong identity | `Threateningly Mobs Continued` 26.2 dependency | MIT/current dependency candidate | creature-derived visual/material basis |

Rules:

- an R01 weapon/armor/resource may not silently switch to an unrelated art pack during coding because it is easier;
- exact source-model filenames/hashes are acquisition metadata and must be recorded during the pre-code asset-intake pass;
- model selection itself is already constrained here by family, silhouette and role; the importer is not allowed to redesign the item;
- inventory icons for 3D equipment/resources should normally be orthographic renders of the same accepted source model, reduced/reworked into the Lucifer pixel language where necessary, rather than unrelated AI-generated icons;
- creature-derived signature materials should be adapted from the creature/dependency geometry or texture identity where license/technical format permits.

---

# 3. Equipment power philosophy

Core rules:

- `Item Lv` and base archetype determine base damage/Defense/Magic Resistance;
- **grade does not multiply base weapon/armor values**;
- grade determines affix count and affix quality floor as defined in `LOOT_ECONOMY.md`;
- therefore a well-synergized lower-grade item can remain useful while a higher-grade item has more build potential;
- there is no routine durability;
- there is no universal `+1 ... +20` equipment enhancement ladder;
- there is no separate enhancement-stone currency;
- ordinary duplicate gear is sold, not automatically converted into another permanent salvage currency.

This keeps the equipment loop focused on `find / compare / build / target farm`, not `upgrade every slot again after every drop`.

---

# 4. Shared Item-Lv scale

For `1 <= L <= 80`:

```text
GearScale(L) = 1 + 0.055 * (L - 1)
```

Reference values:

| Item Lv | GearScale |
|---:|---:|
| 1 | 1.000 |
| 8 | 1.385 |
| 20 | 2.045 |
| 44 | 3.365 |
| 64 | 4.465 |
| 80 | 5.345 |

This is intentionally much flatter than exponential MMO inflation. A few Item Lv matter, but finding a synergistic piece remains relevant.

`Item Lv` generation, ±2 source variance and content-level restrictions remain defined by `LOOT_ECONOMY.md`.

---

# 5. Weapon base scaling

## 5.1 Normalized Weapon Power

Weapons use a normalized `WeaponPower` budget so animation speed does not secretly decide which weapon family is mathematically best.

```text
WeaponBudget(L) = 22 * GearScale(L)
WeaponPower(L, family) = round(WeaponBudget(L) * FamilyPowerFactor)
```

`WeaponPower` is the normalized base offensive budget used by skill scaling and basic-attack calculation. It is **not** simply the visible damage number of one individual swing.

Reference base budget before family factor:

| Item Lv | WeaponBudget |
|---:|---:|
| 1 | 22 |
| 8 | 30 |
| 20 | 45 |
| 44 | 74 |
| 64 | 98 |
| 80 | 118 |

## 5.2 Family cadence / power / stagger identity

| Family | Hand use | FamilyPowerFactor | Target basic cadence | Poise/stagger multiplier | Core feel |
|---|---|---:|---:|---:|---|
| dagger | 1H | 0.88 | 1.85 attacks/s | 0.55 | very fast precision |
| dual blades | 2H pair | 1.03 | 1.35 two-hit cycles/s | 0.65 | sustained close pressure |
| sword | 1H | 0.95 | 1.25 attacks/s | 1.00 | balanced |
| greatsword | 2H | 1.10 | 0.78 attacks/s | 1.60 | reach / commitment / impact |
| spear / polearm | 2H baseline | 1.03 | 1.05 attacks/s | 1.10 | reach / spacing |
| axe | 1H | 0.97 | 0.90 attacks/s | 1.35 | slower high impact |
| hammer / mace | 2H baseline | 1.10 | 0.70 attacks/s | 1.80 | maximum poise pressure |
| bow | 2H | 1.02 | 1.00 full-shot cycles/s | 0.80 | ranged precision |
| crossbow | 2H | 1.07 | 0.72 shots/s | 1.10 | deliberate ranged burst |
| staff | 2H | 1.00 | 1.15 basic casts/s | 0.85 | magic baseline / skill synergy |
| wand | 1H | 0.82 | 1.45 basic casts/s | 0.50 | fast casting + off-hand focus |
| black-powder pistol | 1H | 0.95 | 0.85 shots/s | 0.90 | later Hunter burst/utility |
| musket / hand cannon | 2H | 1.10 | 0.55 shots/s | 1.25 | later slow high-impact shot |

Black-powder families are included in the data contract now, but their first actual region/item is locked only after the M0 ranged/combat dependency audit and accepted external model selection.

## 5.3 Basic hit conversion

For a single-hit basic attack family:

```text
BaseHitDamage = WeaponPower / basic_attacks_per_second
```

For multi-hit animations, the whole attack-cycle hit coefficients must sum to the same cycle budget. Example for dual blades:

```text
CycleDamage = WeaponPower / cycles_per_second
HitA = CycleDamage * 0.55
HitB = CycleDamage * 0.45
```

Longer combo animations may redistribute damage across their animation, but their sustained baseline cannot silently exceed the family WeaponPower budget because the animation happened to contain more hit events.

Skills scale from normalized `WeaponPower`, not from the single-swing damage of a slow weapon. Skill definitions may intentionally add weapon-family coefficients when a skill concept requires it.

## 5.4 Wand + focus

A wand is intentionally below a two-handed staff by itself.

An ordinary magical focus/off-hand contributes:

```text
FocusPower(L) = WeaponBudget(L) * 0.18
```

For compatible magic skills:

```text
EffectiveMagicWeaponPower = WandWeaponPower + FocusPower
```

A staff occupies both hands and receives its full budget directly. This keeps `wand + focus` and `staff` as different play patterns rather than one being an obvious mathematical mistake.

---

# 6. Armor base scaling

Armor has three broad visual/stat archetypes with no class hard lock.

- **Light** — cloth/robe/light fantasy outfit: lowest physical Defense, highest Magic Resistance/resource-affix bias.
- **Medium** — leather/traveler/ranger outfit: balanced.
- **Heavy** — plate/guard outfit: highest physical Defense/poise-affix bias, lowest base Magic Resistance.

There is **no automatic movement-speed penalty merely for wearing Heavy armor** at baseline. Build tradeoffs come from base Defense/Magic Resistance and affix tendencies rather than permanent movement annoyance.

For each slot:

```text
Defense = round(Lv1Defense * GearScale(ItemLv))
MagicResistance = round(Lv1MR * GearScale(ItemLv))
```

## 6.1 Lv1 slot baselines

| Archetype / slot | Head DEF/MR | Chest DEF/MR | Legs DEF/MR | Gloves DEF/MR | Boots DEF/MR | Full set DEF/MR |
|---|---:|---:|---:|---:|---:|---:|
| Light | 2 / 3 | 5 / 7 | 4 / 5 | 2 / 3 | 2 / 3 | **15 / 21** |
| Medium | 4 / 2 | 8 / 4 | 6 / 3 | 3 / 2 | 4 / 2 | **25 / 13** |
| Heavy | 5 / 1 | 11 / 2 | 8 / 2 | 4 / 1 | 5 / 1 | **33 / 7** |

Full-set reference totals after scaling:

| Item Lv | Light DEF/MR | Medium DEF/MR | Heavy DEF/MR |
|---:|---:|---:|---:|
| 1 | 15 / 21 | 25 / 13 | 33 / 7 |
| 8 | 21 / 29 | 35 / 18 | 46 / 10 |
| 20 | 31 / 43 | 51 / 27 | 67 / 14 |
| 44 | 50 / 71 | 84 / 44 | 111 / 24 |
| 64 | 67 / 94 | 112 / 58 | 147 / 31 |
| 80 | 80 / 112 | 134 / 69 | 176 / 37 |

Chest/legs carry most base protection so glove/boot upgrades do not feel as important as a full chest replacement.

## 6.2 Armor affix tendency override

`LOOT_ECONOMY.md` provides generic armor weights. The following archetype overrides are canonical when choosing categories:

| Armor archetype | Primary | Offense | Defense | Resource | Utility |
|---|---:|---:|---:|---:|---:|
| Light | 25 | 20 | 15 | 30 | 10 |
| Medium | 25 | 25 | 25 | 10 | 15 |
| Heavy | 25 | 10 | 45 | 5 | 15 |

This changes probability only. It does not ban a valid unusual build.

---

# 7. Shield and defensive off-hand baseline

A shield has no independent DPS budget.

```text
GuardRating(L) = round(20 * GearScale(L) * shield_family_factor)
```

Baseline shield-family factors:

- buckler: `0.80` — best parry/lowest guard;
- standard shield: `1.00` — balanced;
- tower/heavy shield: `1.25` — strongest guard, slower defensive handling where animation supports it.

Blocked-hit Stamina damage is already resolved by the canonical guard formula in `COMBAT_BALANCE.md` §12.3; shield item data supplies this `GuardRating` directly. This line is not a future balance decision.

---

# 8. Exact affix value rules

`LOOT_ECONOMY.md` already determines affix count and grade percentile floor.
This section closes the actual value ranges.

## 8.1 Roll procedure

For an affix with allowed `[min, max]` and grade percentile floor `f`:

```text
r = random(f, 1.0)
value = lerp(min, max, r)
```

Rounding:

- flat primary stats: whole integers;
- Crit Chance and Movement Speed: nearest `0.1%`;
- all other percentages: nearest `0.5%`.

## 8.2 Flat primary-stat scaling

For VIT / END / STR / DEX / INT / WIL:

```text
AffixFlatScale(L) = 1 + 0.045 * (L - 1)
PrimaryMin(L) = max(1, round(1.0 * AffixFlatScale(L)))
PrimaryMax(L) = max(PrimaryMin + 1, round(2.5 * AffixFlatScale(L)))
```

Reference raw ranges before grade percentile floor:

| Item Lv | Single-primary range |
|---:|---:|
| 1 | 1–2 |
| 8 | 1–3 |
| 20 | 2–5 |
| 44 | 3–7 |
| 64 | 4–10 |
| 80 | 5–11 |

Dual-primary affixes are not part of the baseline random pool. If a named item needs two stats, they are authored individually so generic loot does not become unreadable.

## 8.3 Percentage affix ranges

These raw min/max values are intentionally compact because up to 12 equipment slots can contribute affixes.

### Offense

| Affix | Raw range |
|---|---:|
| Physical Power | +2.5% to +7.5% |
| Magic Power | +2.5% to +7.5% |
| specific weapon-family power | +4% to +10% |
| specific implemented element/status output | +4% to +12% |
| Critical Chance | +1.0 to +3.5 percentage points |
| Critical Damage | +5% to +15% |
| Attack Speed | +2% to +6% |
| weak-point damage, where supported | +5% to +15% |

### Defense

| Affix | Raw range |
|---|---:|
| Defense | +3% to +9% |
| Magic Resistance | +3% to +9% |
| Max HP | +2.5% to +7% |
| Guard Strength | +5% to +15% |
| Poise/Stagger Resistance | +5% to +15% |

### Resource

| Affix | Raw range |
|---|---:|
| Max Mana | +3% to +9% |
| Max Stamina | +3% to +9% |
| Mana Recovery | +4% to +12% |
| Stamina Recovery | +4% to +12% |
| skill Mana-cost reduction | 2% to 6% |
| dodge/sprint Stamina-cost reduction | 3% to 9% |
| Ultimate charge gain | +4% to +10% |

### Utility

| Affix | Raw range |
|---|---:|
| Movement Speed | +1.0% to +3.5% |
| Healing Done | +4% to +12% |
| Healing Received | +4% to +12% |
| negative-status duration reduction | 5% to 15% |
| potion/food effect strength | +4% to +12% |

Do not add tiny filler variants such as `+1.2% fire resistance` merely to grow the list.

## 8.4 Aggregate caps from gear

Caps apply after summing all equipped gear. They exist to preserve combat feel and prevent one stat from invalidating animation/resource systems.

| Stat | Gear-contribution cap |
|---|---:|
| Critical Chance | +30 percentage points |
| Attack Speed | +35% |
| Movement Speed | +15% |
| skill Mana-cost reduction | 20% |
| dodge/sprint Stamina-cost reduction | 25% |
| Ultimate charge gain | +30% |
| Healing Done | +40% |
| Healing Received | +40% |
| Guard Strength | +50% |
| Poise/Stagger Resistance | +50% |
| one element/status output | +60% |
| one weapon-family power | +60% |

Primary stats and base Defense/Magic Resistance are not capped here; their later combat formulas handle diminishing/appropriate scaling where needed.

---

# 9. Forge / smithing rules

The forge is a meaningful world service, not an enhancement treadmill.

Baseline forge tabs/actions:

1. **Craft** — deterministic known recipes that fill equipment gaps.
2. **Reforge Affix** — one bounded repair of a bad random affix on high-grade ordinary gear.
3. **Signature Craft** — craft a boss Mythic from its visible signature materials.

There is no baseline `Enhance +1`, `Masterwork Level 20`, repair durability, or salvage-shard loop.

## 9.1 Craft

Regional recipes define:

```text
output_base_id
output_item_lv
output_grade
fixed_affix_ids
random_affix_count
materials
gold_cost
required_service_tier / discovered_recipe
```

Ordinary regional smith recipe baseline:

- output grade: **Refined**;
- one fixed thematic affix identity;
- one random valid affix;
- values roll using normal Refined percentile floor.

Rare regional recipe baseline:

- output grade: **Superior**;
- one fixed thematic affix identity;
- two random valid affixes;
- requires the region's rare material or authored encounter material.

Exalted crafting is not a routine menu option. An Exalted recipe requires an authored rare blueprint/quest/source and does not become the expected way to fill every slot.

## 9.2 Reforge Affix

Only **Superior** and **Exalted** non-Mythic gear may use baseline Reforge.

Rules:

- the player chooses exactly one existing affix to replace;
- all other affixes remain untouched;
- the replacement identity is randomly selected from the item's valid pool after excluding current identities and the removed identity for that roll;
- the new value uses the item's existing grade percentile floor;
- **each individual item may be reforged only once** at baseline;
- Mythic authored affix identities cannot be reforged;
- there is no paid `unlock another reroll` escalation loop.

Gold cost:

```text
ReforgeBase(L) = round_to_10(60 + 3L + 0.08L²)
SuperiorCost = ReforgeBase(L)
ExaltedCost = round_to_10(ReforgeBase(L) * 1.6)
```

Reference:

| Item Lv | Superior | Exalted |
|---:|---:|---:|
| 8 | 90 Gold | 140 Gold |
| 20 | 150 Gold | 240 Gold |
| 44 | 350 Gold | 560 Gold |
| 64 | 580 Gold | 930 Gold |
| 80 | 810 Gold | 1,300 Gold |

Material cost:

- Superior: 2 units of the current/source region's ordinary forge material;
- Exalted: 2 units of that region's rare forge material;
- do **not** introduce a global `Reforge Crystal` currency when existing real materials can serve the function.

## 9.3 Signature / Mythic craft

`LOOT_ECONOMY.md` remains authoritative: 4 boss signature materials craft one chosen Mythic from that boss's known pool.

Gold fee:

```text
MythicCraftFee(L) = round_to_10(200 + 8L + 0.15L²)
```

Examples:

| Mythic Item Lv | Fee |
|---:|---:|
| 8 | 270 Gold |
| 20 | 420 Gold |
| 44 | 840 Gold |
| 64 | 1,330 Gold |
| 80 | 1,800 Gold |

Crafted Mythic numeric authored affixes use a deterministic **90th-percentile value** of their allowed range. Direct drops still roll inside the normal Mythic `85%–100%` window, so a lucky direct item may be slightly better numerically without making crafted bad-luck protection feel like a consolation-prize item.

## 9.4 No Item-Lv raising service

An early Mythic does not scale to Lv80 merely because the player loves it.

Reasons:

- new regions need real reward identity;
- permanent early Mythics would crowd out later visual/content work;
- the collection/housing/trophy systems preserve sentimental value without forcing every early item into endgame balance.

If a future higher-tier version of a signature concept exists, it is a separately authored item/source, not a hidden infinite level-up button.

## 9.5 No salvage currency at baseline

Ordinary unwanted gear is sold under existing sell-back rules.

Do not add salvage just because other ARPGs have it. Add salvage only if a future system has a concrete non-overlapping need that cannot be met by Gold and real regional materials.

---

# 10. R01 production resource catalog

This is the first region's canonical resource set. It is intentionally small.

## 10.1 Field/node resources

| Resource | Source / location | Yield | Personal respawn | Visual source | Main uses |
|---|---|---:|---:|---|---|
| **Iron Ore** | quarry / rocky outcrops | 2–4 | 7 active min | KayKit Resource Bits iron/ore model family | starter forge weapons, shields, heavy armor/service recipes |
| **Hardwood** | woodland timber nodes | 2–3 | 5 active min | KayKit Resource Bits logs/wood bundle | bows, shafts, camps, furnishing, some weapon recipes |
| **Healing Herb** | river/forest-edge herb nodes | 1–2 | 4 active min | Quaternius Stylized Nature MegaKit low herb/flower model | basic potion/healing food/alchemy |
| **Verdant Crystal** | dangerous deep-quarry / grove pockets only | 1 | 18 active min | KayKit Resource Bits crystal/gem-cluster family | R01 Superior recipes, magic equipment, later revisit value |

Node state is personal per player as already canonical.

The exact free-pack file chosen for Healing Herb must be a low readable green/flowering plant silhouette from the selected Quaternius family and recorded during asset intake before gameplay coding. This is an asset-file binding task, not permission to invent a different herb concept later.

## 10.2 Creature resources

| Resource | Source | Project-normalized reward | Visual source | Main use |
|---|---|---|---|---|
| **Louxia Meat** | Louxia | 1–2 guaranteed per eligible kill | Threateningly Mobs Continued existing creature/item identity | food/cooking |
| **Louxia Glow** | Louxia luminous organ | 1 at 35% | derive from Louxia luminous-organ / existing `glowing swell` identity | light/utility props, basic magic/forge recipe |
| **Tough Hide** | bison / grizzly primarily; gazelle secondary | bison 1–2 at 70%, grizzly 2–3 guaranteed, gazelle 1 at 35% | KayKit Resource Bits textile/leather-like model adapted to brown hide, or creature-derived legal texture where cleaner | medium armor, bow grips/straps, camp utility |

`Louxia Glow` is the clean player-facing project name for the dependency's luminous-organ concept; the game does not expose awkward machine-translated `glowing swell` wording if the dependency allows normal item-remapping/translation.

## 10.3 Signature boss materials

| Material | Boss | First eligible defeat | Repeat | Visual derivation | Purpose |
|---|---|---:|---:|---|---|
| **Regalhart Antler** | Regalhart | 2 | 1 | simplified/adapted antler geometry from Regalhart identity | Hartcrown signature craft |
| **Earthloong Scale** | Earthloong | 2 | 1 | simplified armored scale/plate form derived from Earthloong visual identity | Earthloong signature craft |

These are materials, not currencies. They sit in Material Pouch, use the normal 999 cap, and can have additional small authored forge/display uses after their primary Mythic purpose.

---

# 11. R01 ordinary equipment visual catalog

R01 teaches the weapon/armor language. It deliberately uses grounded forms rather than immediately filling the region with exotic neon fantasy gear.

## 11.1 Weapon/off-hand bases

| Base family | Gameplay family | External model direction | R01 source ownership |
|---|---|---|---|
| **Heartland Arming Sword** | sword | Quaternius Modular Weapons: clean straight medieval sword silhouette | starting merchant / forge / R01 treasure |
| **Wayfarer Daggers** | dual blades / dagger | Quaternius Modular Weapons paired simple daggers | elites / merchant / treasure |
| **Quarry Maul** | hammer | Quaternius Modular Weapons / Fantasy Props heavy hammer silhouette | quarry / smith / Steelboar-adjacent rewards |
| **River Pike** | spear | Quaternius Modular Weapons long spear/polearm silhouette | forge / river-road contracts |
| **Riverwood Bow** | bow | KayKit Fantasy Weapons Bits or Quaternius grounded bow silhouette | hunter-style contracts / merchant / forge |
| **Initiate Staff** | staff | KayKit Fantasy Weapons Bits staff with clear magical head silhouette | guild/merchant / Nature-Spirit-side content |
| **Initiate Wand** | wand | KayKit Fantasy Weapons Bits compact wand | merchant / magic service rewards |
| **Watch Buckler** | buckler | Quaternius round shield | merchant / forge / road-defense contract |
| **Apprentice Focus** | magical focus | KayKit fantasy handheld magical accessory or Quaternius prop-book/gem basis | magic service / treasure |

Rules:

- these are item-base identities, not one fixed final item each; Item Lv/grade/affixes still vary by source;
- source pools use subsets, not a global equally weighted bag;
- model recolors/material variations may communicate Item Lv band/region only when they preserve the original external silhouette and do not turn every grade into a rainbow reskin.

## 11.2 Armor families

Use Quaternius `Modular Character Outfits - Fantasy` as the editable CC0 base family.

| Armor family | Archetype | Visual direction | Source role |
|---|---|---|---|
| **River Scholar Garb** | Light | cloth/robe outfit with restrained blue/green/neutral palette | guild, magic services, dungeon/treasure |
| **Wayfarer Leathers** | Medium | leather/traveler/ranger silhouette, brown/green neutral palette | merchant, contracts, exploration rewards |
| **Ironbound Guard** | Heavy | grounded silver/iron plate silhouette | smith, quarry, elite/dungeon rewards |

Each family supplies Head / Chest / Legs / Gloves / Boots pieces while keeping the pack's coherent outfit language.

Implementation must adapt the source silhouettes to the chosen Minecraft armor rendering backend without replacing them with generic vanilla armor textures. The M0 audit decides the safest AzureLib/armor-model path; it does not reopen the visual family.

## 11.3 Accessories

R01 ordinary Necklace / Ring / Charm / Relic visuals use `Lucifer - Equipment` CC0 equipment sprites where a suitable readable icon exists. If a 3D world presentation is required, use a compact external CC0 jewelry/prop model, but do not create a separate unrelated icon design.

R01 accessories remain mechanically modest compared with later named Relics; their job is to introduce build affixes without making early inventory management dense.

---

# 12. R01 deterministic first-dungeon choice

The first root-overgrown quarry dungeon boss is currently Earthloong at local Lv ~8. The first eligible clear gives one **Superior Item Lv 8** choice among the following three accepted visual directions.

All three have fixed affix identities and deterministic **85th-percentile** affix values so the reward is reliably useful.

## 12.1 Ironroot Longsword

- family: sword;
- external model: Quaternius Modular Weapons — broad clean straight sword / restrained fantasy guard;
- fixed affixes: `STR`, `Physical Power`, `Guard Strength`;
- role: balanced melee option that works for Warrior, Guardian or any weapon-flex build;
- not Mythic; no unique effect.

## 12.2 Riverthorn Bow

- family: bow;
- external model: KayKit Fantasy Weapons Bits — grounded fantasy bow silhouette;
- fixed affixes: `DEX`, `Critical Chance`, `Attack Speed`;
- role: precision/ranged option without hard Hunter lock;
- not Mythic; no unique effect.

## 12.3 Lumenwood Staff

- family: staff;
- external model: KayKit Fantasy Weapons Bits — readable orb/gem-headed staff silhouette;
- fixed affixes: `INT`, `Max Mana`, `Magic Power`;
- role: Mage/Cleric-compatible magic option without class lock;
- not Mythic; no unique effect.

The UI presents all three in the same Lucifer-family reward panel with real model/icon previews and comparison against currently equipped gear.

---

# 13. R01 signature / Mythic items

These are intentionally few. Their models must be selected from the external families above before asset intake is marked complete.

## 13.1 Hartcrown Spear

- source: Regalhart field boss;
- required Lv / Item Lv: 8 baseline;
- family: spear;
- model direction: Quaternius ornate spear/polearm with an antler/crown-compatible silhouette; use a source model as intact as practical rather than drawing a new weapon from zero;
- authored affixes: `DEX`, `Movement Speed`, `Physical Power`;
- unique: **Hart's Momentum**.

`Hart's Momentum`:

```text
After moving at sprint speed for >= 1.25 s,
the next melee basic attack made within 2.0 s
lunges up to 1.5 blocks toward its valid target
and deals +35% poise/stagger damage.
Internal cooldown: 5.0 s.
```

The lunge cannot pass through solid collision and cannot extend the actual hitbox beyond the visible/validated attack reach.

Craft path: 4 Regalhart Antlers + MythicCraftFee(ItemLv).

## 13.2 Rootquake Maul

- source: Earthloong dungeon boss;
- required Lv / Item Lv: 8 baseline;
- family: hammer;
- model direction: KayKit/Quaternius heavy fantasy hammer with earth/stone-compatible mass and silhouette;
- authored affixes: `STR`, `Physical Power`, `Guard/Poise pressure`;
- unique: **Rootquake**.

`Rootquake`:

```text
When the wielder personally causes an elite/boss poise break,
release a ground shockwave centered on that enemy.
Radius: 4.0 blocks.
Damage: 40% of WeaponPower as physical/impact damage.
Internal cooldown: 8.0 s.
Normal enemies hit by the shockwave receive strong stagger,
not a guaranteed chain poise break on bosses/elites.
```

The VFX uses a ground/earth crack or ring treatment matched to the actual 4-block radius; no generic particle cloud.

Craft path: 4 Earthloong Scales + MythicCraftFee(ItemLv).

## 13.3 Earthscale Ward

- source: Earthloong dungeon boss;
- required Lv / Item Lv: 8 baseline;
- family: standard/heavy shield;
- model direction: Quaternius ornate/solid shield with an earth/plate-compatible silhouette;
- authored affixes: `VIT`, `Guard Strength`, `Max HP`;
- unique: **Earthen Reprieve**.

`Earthen Reprieve`:

```text
Successful perfect guard grants a barrier equal to 8% of max HP.
Barrier duration: 4.0 s.
Barrier does not stack with itself; a new trigger refreshes/replaces it.
Internal cooldown: 10.0 s.
```

Craft path: 4 Earthloong Scales + MythicCraftFee(ItemLv).

Earthloong therefore has a two-item known signature pool. The direct 15% signature roll chooses between those two at equal baseline weight unless future visual/playtest evidence justifies changing that specific boss pool.

---

# 14. R01 forge recipes

These initial recipes give materials immediate purpose without turning gathering into compulsory mass crafting.

| Recipe | Output | Materials | Gold | Affix rule |
|---|---|---|---:|---|
| Heartland Sword | Refined Item Lv 4 Heartland Arming Sword | 6 Iron Ore + 2 Hardwood | 60 | fixed STR + 1 random valid |
| Quarry Maul | Refined Item Lv 5 Quarry Maul | 8 Iron Ore + 2 Hardwood | 70 | fixed Physical Power + 1 random valid |
| River Pike | Refined Item Lv 4 River Pike | 5 Iron Ore + 3 Hardwood | 60 | fixed END + 1 random valid |
| Riverwood Bow | Refined Item Lv 4 Riverwood Bow | 4 Hardwood + 2 Tough Hide | 50 | fixed DEX + 1 random valid |
| Initiate Staff | Refined Item Lv 4 Initiate Staff | 4 Hardwood + 2 Louxia Glow | 60 | fixed INT + 1 random valid |
| Watch Buckler | Refined Item Lv 4 Watch Buckler | 4 Hardwood + 3 Iron Ore | 50 | fixed Guard Strength + 1 random valid |

Early Superior recipes unlock after the first player encounters Verdant Crystal rather than via arbitrary menu level gates. They reuse the exact normal Refined materials, add **2 Verdant Crystal**, and use the following exact R01 Gold costs:

| Superior output | Refined materials reused | Verdant Crystal | Gold | Affix rule |
|---|---|---:|---:|---|
| Superior Item Lv6 Heartland Arming Sword | 6 Iron Ore + 2 Hardwood | 2 | **150** | fixed STR + 2 random valid |
| Superior Item Lv6 Quarry Maul | 8 Iron Ore + 2 Hardwood | 2 | **180** | fixed Physical Power + 2 random valid |
| Superior Item Lv6 River Pike | 5 Iron Ore + 3 Hardwood | 2 | **150** | fixed END + 2 random valid |
| Superior Item Lv6 Riverwood Bow | 4 Hardwood + 2 Tough Hide | 2 | **130** | fixed DEX + 2 random valid |
| Superior Item Lv6 Initiate Staff | 4 Hardwood + 2 Louxia Glow | 2 | **150** | fixed INT + 2 random valid |
| Superior Item Lv6 Watch Buckler | 4 Hardwood + 3 Iron Ore | 2 | **130** | fixed Guard Strength + 2 random valid |

These are HARD_RULE starting costs. Real playtest may revise the canon if the entire R01 economy is demonstrably off; implementation does not choose its own rounded 2.5x value.

Do not require the player to craft these items to progress. They are a reliable gap-filling path beside loot/merchants/quests.

---

# 15. Item comparison / UI data requirements

The inventory comparison layer must be able to show, without a giant stat wall:

- Item Lv;
- grade;
- base WeaponPower or Defense/Magic Resistance difference;
- attack cadence/family for weapons;
- guard rating for shields;
- affix differences;
- unique Mythic mechanic in a separate authored block;
- source hint for signature items when discovered;
- `Reforged` state when the one allowed reforge was consumed.

The item icon/model shown in the UI must correspond to the actual accepted external model family, not a generic placeholder icon.

---

# 16. Data contract

Suggested data ownership:

```text
equipment/
  base_families/
    weapons/*.json
    armor/*.json
    offhands/*.json
  affixes/*.json
  signature/*.json
  recipes/*.json
resources/
  r01/*.json
loot/
  r01/*.json
assets/
  source_bindings/*.json
```

## 16.1 Weapon base schema

```text
id
family
hand_use
item_lv
family_power_factor
basic_cadence
poise_multiplier
reach_profile
model_source_id
icon_source_id
valid_affix_pool
source_tags
```

## 16.2 Armor base schema

```text
id
slot
archetype: light | medium | heavy
item_lv
lv1_defense
lv1_magic_resistance
model_source_id
icon_source_id
valid_affix_pool
source_tags
```

## 16.3 Resource schema

```text
id
player_facing_name
category
node_or_drop_source
yield_rule
personal_respawn_if_node
material_pouch_cap
model_source_id
icon_source_id
license_status
uses[]
```

## 16.4 Source-binding schema

Every production visual binding records at least:

```text
project_item_id
external_source_name
source_url
license
adoption_mode: direct | editable_base | dependency | reference
exact_source_file
source_sha256_if_local_file
conversion_notes
public_repo_safe
```

No content registry entry may fall back to `todo_model` or a vanilla placeholder in a player-visible build.

---

# 17. Pre-code asset-intake checkpoint for this subsystem

Before gameplay implementation of R01 equipment/resources begins:

1. download the selected free CC0 Quaternius/KayKit packs from their canonical pages;
2. record exact pack/file hashes and individual model filenames used by every R01 entry above;
3. inspect the models in Blender/Blockbench and confirm they survive Minecraft scale/silhouette conversion;
4. produce one actual Minecraft-scale sample of each visual family: martial weapon, magic weapon, armor, ore/resource, herb node;
5. render matching inventory icons from those accepted models into the Lucifer UI grammar;
6. verify the three first-dungeon reward models side-by-side so they are visually distinct;
7. verify Regalhart Antler and Earthloong Scale can be derived cleanly from the dependency visual identity under the MIT/public-repo boundary;
8. if one exact source model fails technically, replace it with another model **inside the already selected external family/role** and update the source binding before coding that item;
9. do not replace the whole art direction with an improvised internal design during implementation.

This checkpoint is asset preparation/validation, not a later gameplay-design phase.

---

# 18. What this pass closes

This file closes the previous loot-economy blockers for:

- weapon base-stat scaling;
- armor base-stat scaling;
- weapon cadence/power identity;
- exact baseline affix value ranges and gear caps;
- forge/craft/re-roll policy;
- decision to omit generic enhancement levels and salvage currency;
- R01 canonical field/creature/signature resource list;
- R01 ordinary weapon/off-hand families;
- R01 three armor visual families;
- R01 deterministic first-dungeon reward identities;
- R01 initial Mythic identities/mechanics;
- R01 initial forge recipes;
- the external model families those systems are designed around.

Remaining work before source bootstrap is not permission to invent these values during coding. The next planning batches should continue closing actual world-visible content and exact dependency/asset integration.