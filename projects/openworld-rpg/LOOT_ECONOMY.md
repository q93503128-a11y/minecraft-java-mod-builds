# Open-World RPG — Loot / Equipment Economy Reference

> Status: subordinate design reference  
> Master canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Scope: equipment generation, affixes, source drop rates, signature loot, bad-luck protection and loot presentation.  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins.

This file expands the loot-economy work explicitly queued by `GAME_DESIGN.md`. It is intentionally detailed enough that implementation should translate tables/rules into data and code rather than inventing balance or content behavior while coding.

---

# 1. External precedents and what is actually adopted

The project does not copy another game's numbers or proprietary art. It reuses proven structural solutions where they solve the same problem.

## Grim Dawn

Useful precedent:

- common bases can roll prefixes/suffixes;
- rare affixes create meaningful combinations instead of every higher color being a strict replacement;
- Monster Infrequents make specific enemies worth targeting;
- one-time exploration chests can guarantee valuable rewards.

Project adoption:

- controlled affix pools;
- specific bosses/regions own identifiable target-farm pools;
- deterministic first-clear rewards coexist with repeatable random gear.

## Diablo IV

Useful precedent:

- condensed affix pools reduce junk stats;
- Unique items are target-farmable from defined bosses;
- higher-end items can use stronger affix-quality presentation without requiring dozens of rarity tiers.

Project adoption:

- no giant filler-affix pool;
- every major signature item has a known source;
- signature loot has a direct-drop path and a visible deterministic fallback.

The project does **not** adopt seasonal reset structure, giant item showers, or many overlapping upgrade currencies.

## Current Minecraft 26.2 references

### DungeonRPG

Current Fabric 26.2 reference for:

- randomized equipment;
- named unique items;
- dungeon/boss-focused loot;
- salvage/recycle UX.

The project does not adopt its durability/repair loop or its exact rarity names/numbers.

### ToolTiers

Current Fabric 26.2 MIT reference for:

- equipment quality/tier state;
- attribute reroll/reforge concepts;
- data-driven quality effects.

Use as implementation reference only where its architecture remains simpler than project-owned code. Do not inherit its Common/Rare/Legendary/Mythic presentation unchanged.

### Relics (RPG Series)

Current 26.2 reference for:

- distinct trinkets with individual perks/artwork;
- boss/dungeon acquisition;
- multiplayer-oriented RPG accessory rewards.

ARR content remains dependency/reference material, not a source-art donor for the public repository.

---

# 2. Visual-source admission rule

**No new player-facing item, material, resource node, weapon, armor piece, accessory, boss drop or consumable becomes canonical only because a mechanic needs a name.**

Before an item family is locked, record a viable visual source:

1. directly usable external asset;
2. editable external base;
3. installed dependency whose normal use supplies the model/icon;
4. strong external reference that can legally guide a project-owned adaptation;
5. only when none of the above works, a deliberately commissioned/custom solution consistent with the established visual family.

AI-improvised item art is not the default fallback.

Every production item definition must eventually carry or resolve to:

```text
id
player-facing name
item family / slot
source region / encounter
item Lv rules
grade rules
affix pool
unique mechanic if any
3D/world model source
inventory icon source
VFX / ground presentation source if important
sound source/direction if important
license / public-repo status
```

If the model/icon source is still unknown, the entry is a **candidate**, not production-canonical content.

## Current reusable visual families

- `Foozle Lucifer - Equipment` (CC0) — slot frames, rarity/background language and equipment presentation.
- `Foozle Lucifer - RPG UI` (CC0) — loot tooltip/HUD/UI visual grammar.
- `Quaternius Modular Weapons Pack` (CC0) — primary editable/direct-use weapon-model family.
- `Quaternius Fantasy Props MegaKit` (CC0) — potions, tools, chests, books, market props and many ordinary fantasy item/world-prop bases.
- `KayKit Resource Bits` (CC0) — especially useful for readable wood/stone/iron/copper/silver/gold/resource-chunk silhouettes.
- `Quaternius Stylized Nature MegaKit` (CC0), `Kenney Nature Kit` (CC0), `KayKit Forest Nature Pack` (CC0) — plant, herb, rock and world-node visual bases.
- creature/boss dependency assets — source-linked meat/hide/scale/antler/core drops should visually derive from the actual creature identity where permitted rather than becoming unrelated generic icons.

The exact adopted asset for every material is selected during regional item/resource catalog work before implementation.

---

# 3. Equipment identity

Every ordinary equipment item has:

- `Item Lv` from 1–80;
- one base archetype/model identity;
- one equipment slot/family;
- one grade;
- zero or more canonical affixes according to grade;
- source tags for region/encounter/merchant/crafting targeting.

## Item Lv generation

For a normal equipment roll from source encounter level `S`:

```text
ItemLv = clamp(S + delta, 1, 80)
```

`delta` uses this weighted distribution:

| delta | weight |
|---:|---:|
| -2 | 10% |
| -1 | 20% |
| 0 | 40% |
| +1 | 20% |
| +2 | 10% |

Boss first-clear curated rewards use at least the boss/content Lv and do not roll below it.

Unless a specific named item states otherwise, required player Lv equals `Item Lv`. A player who sequence-breaks into high-level content may obtain the reward early, but cannot bypass the global equipment progression merely through multiplayer carrying.

Base-stat curves by weapon/armor family are locked in `EQUIPMENT_BALANCE.md`. Loot code must read those canonical data values; it must not invent family curves internally.

---

# 4. Five equipment grades

Player-facing launch grades:

1. **Standard**
2. **Refined**
3. **Superior**
4. **Exalted**
5. **Mythic**

These avoid the complete `Common / Rare / Epic / Legendary` cliché ladder while remaining immediately understandable.

Visual rule:

- frame geometry and pixel treatment derive from `Lucifer - Equipment`;
- project recoloring must remain inside the shared dark-stone/iron/bronze UI language;
- grade can use color, but color is never the only signal: border shape/detail and tooltip label also change;
- no unrelated rarity-frame pack may be mixed into the inventory just for higher tiers.

## Affix count / quality floor

| Grade | Normal affixes | Affix value percentile floor | Role |
|---|---:|---:|---|
| Standard | 1 | 60% | readable baseline gear |
| Refined | 2 | 65% | early build direction |
| Superior | 3 | 70% | serious build piece |
| Exalted | 4 | 75% | high-end random gear |
| Mythic | 3 authored identities + 1 unique power | 85% | named/signature build-defining gear |

`percentile floor` means the final rolled value lies between that percentile of the affix's allowed Item-Lv range and its maximum. Exact stat min/max curves are data, not hard-coded per rarity.

A lower-grade item with the right affixes can beat a higher-grade item with poor synergy. Grade increases options/roll quality, but does not replace item identity with a single global power multiplier.

Mythic items do not use a generic four-random-affix generator. Their affix identities and unique mechanic are authored around the model/source/boss and only their allowed numeric ranges roll.

---

# 5. Affix pool rules

Affixes are deliberately condensed. Do not create filler such as tiny isolated resistances or dozens of near-duplicates only to inflate loot variety.

Core categories:

- **Primary** — VIT / END / STR / DEX / INT / WIL and selected broad-stat combinations.
- **Offense** — Physical/Magic Power, Critical Chance/Damage, Attack Speed, weapon/skill-family scaling, status/elemental output where valid.
- **Defense** — Defense, Magic Resistance, max HP, guard/stability, poise/stagger resistance and justified mitigation.
- **Resource** — Mana/Stamina maximum, recovery, selected skill-cost efficiency and ultimate/resource interactions.
- **Utility** — movement, healing/support, status handling, parry/dodge/guard utility and other build-changing quality effects.

## Slot category weights

Weights are used when choosing an affix category, after invalid categories/affixes are filtered out.

After the category is chosen, **eligible affix identities inside that category are equal-weight by default**. A specific affix may use a non-equal data weight only when its canonical data explicitly records that weight; implementation must not invent hidden rarity weights.

| Item family | Primary | Offense | Defense | Resource | Utility |
|---|---:|---:|---:|---:|---:|
| normal weapon | 20 | 50 | 0 | 20 | 10 |
| armor | 25 | 20 | 35 | 10 | 10 |
| shield / defensive off-hand | 15 | 10 | 35 | 20 | 20 |
| necklace / rings / charm / relic | 20 | 25 | 10 | 25 | 20 |
| magical focus / catalyst | 20 | 35 | 5 | 30 | 10 |

Weapon-family and slot filters override these weights. A bow does not roll shield-only guard affixes; heavy armor does not roll bow-projectile affixes merely because both are `Offense`/`Utility` categories.

Rules:

- never roll the same exact affix twice on one item;
- an item may have at most two affixes from the same category;
- impossible/useless combinations are removed before random selection rather than allowed as junk rolls;
- class names do not hard-lock affixes; broad weapon freedom remains canonical;
- class-specific skill affixes exist only when they refer to an actual implemented skill family and remain readable after class switching;
- the affix pool and values are data-driven and server-authoritative.

---

# 6. General grade probability by equipment-roll source

Mythic is **not** part of the generic table. It uses named/signature pools described later.

Base distribution for content around the player's intended progression:

| Equipment-roll source | Standard | Refined | Superior | Exalted |
|---|---:|---:|---:|---:|
| elite | 50% | 35% | 13% | 2% |
| miniboss | 25% | 45% | 25% | 5% |
| normal authored treasure / dungeon chest | 30% | 45% | 21% | 4% |
| rare treasure / major POI chest | 15% | 40% | 35% | 10% |
| field/world boss | 10% | 35% | 40% | 15% |
| dungeon boss | 5% | 30% | 45% | 20% |

Late-content quality shift:

- source Lv 44–64: move 10 percentage points from Standard into Superior;
- source Lv 65–80: after the previous shift, move 5 percentage points from Refined into Exalted;
- clamp categories at zero if a special source overrides the base table.

This means higher regions naturally stop showering the player with low-value gear without making early Exalted drops impossible.

---

# 7. How often equipment actually drops

The game deliberately avoids ARPG floor-confetti. Common enemies do **not** roll normal equipment.

## Ordinary/common enemies

- equipment chance: **0%** baseline;
- reward: EXP, Gold, creature/resource materials and selected consumables;
- authored exceptions require a specific named item/source identity and are not routine random gear.

## Elite

- 30% chance to create one equipment roll;
- no second random equipment roll at baseline;
- elite-specific materials may drop separately.

## Miniboss

- 75% chance to create one equipment roll;
- 10% chance for one additional equipment roll;
- important authored minibosses may own a narrow named-drop pool.

## Normal authored treasure / POI chest

- 25% chance to create one normal equipment roll;
- the rest of the value comes from Gold/materials/consumables/world rewards.

## Rare treasure / dangerous POI reward chest

- one guaranteed equipment roll;
- source uses the rare-treasure grade table;
- one-time discovery chests may additionally guarantee `Superior+` when the placement effort/danger justifies it.

## Field/world boss

First eligible defeat per player:

- one guaranteed `Superior+` normal equipment roll;
- one curated first-clear reward choice when that boss is a major regional milestone;
- two units of that boss's signature material;
- one direct Mythic/signature roll at 15%.

Repeat eligible defeat:

- one guaranteed normal equipment roll;
- 25% chance for a second normal equipment roll;
- one unit of signature material;
- one direct Mythic/signature roll at 15%.

## Dungeon first clear

For an ordinary 15–25 minute dungeon:

- dungeon boss: one guaranteed `Superior+` equipment roll;
- completion reward: choose **1 of 3 distinct** curated gear candidates appropriate to the dungeon/region;
- first-clear candidates cannot all occupy the same slot unless the dungeon is deliberately weapon-specific;
- signature material from the dungeon boss: 2;
- direct Mythic/signature roll: 15%;
- other chests/materials follow their own tables but should not push a normal first clear into an inventory-cleanup event.

## Dungeon repeat clear

- completion: one guaranteed normal dungeon equipment roll;
- boss: 60% chance for one additional normal equipment roll;
- signature material: 1;
- direct Mythic/signature roll: 15%.

A normal repeat dungeon should therefore average around 1.6 ordinary equipment pieces plus its signature chance, not 15 disposable pieces.

## Quests/contracts

- routine contracts primarily give EXP/Gold/materials, not random gear every time;
- main/regional milestones may give a deterministic curated equipment reward;
- gear rewards must come from an actual external visual/model family already accepted for that region/content.

---

# 8. Target farming and source ownership

Every important gear family has identifiable acquisition sources.

A region/dungeon/boss loot pool should normally contain:

- 4–8 ordinary equipment families strongly associated with that source/region;
- appropriate regional material drops;
- 1–3 signature/Mythic items for a major boss where the visual/model quality justifies them.

Do not put every weapon and every armor model into one global random table.
A player looking for a specific build should be able to learn where that style of item comes from.

No class-exclusive smart-loot filter is used at baseline because the project deliberately permits broad weapon freedom. Targeted geography/encounters, merchants and deterministic first-clear choices provide direction without silently deciding the player's class for them.

---

# 9. Mythic / signature gear

A Mythic item must earn its existence through all of the following:

- a strong external model/icon/design source;
- a specific source encounter/quest/location;
- a named identity tied to that source;
- a unique mechanic that materially changes play/build behavior;
- authored affix identities that support that mechanic;
- dedicated tooltip/ground-drop presentation consistent with the selected UI family.

A generic sword with +15% more damage is not Mythic.

## Direct-drop chance

Each eligible field/world/dungeon boss signature roll has a baseline **15%** direct chance to drop one item from its signature pool.

If a boss has multiple Mythics, the boss definition owns their relative weights. Do not silently add unrelated global Mythics to dilute a player's target farm.

## Visible bad-luck protection — signature materials

There is no hidden infinite pity counter.

Every eligible boss clear grants a visually/source-linked signature material:

- first eligible clear: **2** materials;
- repeat clear: **1** material.

At the appropriate smith/forge, **4 boss signature materials** can be used to craft one chosen Mythic from that boss's known pool, plus a modest Gold service fee defined by the item's tier/region.

Consequences:

- a player can be lucky and obtain the direct Mythic immediately;
- a player who is unlucky still reaches a chosen signature item after at most a small number of clears;
- materials remain useful after a lucky direct drop when that boss has multiple signature items;
- the protection resource is a real boss-derived material with a model/icon and crafting role, not an abstract `Boss Token` currency.

Boss material names and visuals are not invented before the boss's actual model/source is inspected. Examples of shape are `Regalhart Antler` or `Earthloong Core`, not final names unless their visual source supports them.

---

# 10. First-clear protection

Bad RNG must never make a first-time player leave a meaningful dungeon with no useful progression.

Rules:

- every major first dungeon/region milestone has at least one deterministic or choose-one gear reward;
- the reward uses content-level Item Lv and an accepted external item model;
- three-choice rewards present distinct identities, not three numerical copies of the same sword;
- quest/key progression items are always deterministic;
- first-clear protection is per player in multiplayer;
- repeat farming reverts to the normal random/source tables plus visible signature-material protection.

R01's first quarry dungeon therefore must ship with its actual three-choice reward models/icons selected before coding begins.

---

# 11. Duplicate handling / inventory pressure

The loot system is deliberately low-density enough that inventory management remains a decision instead of janitorial work.

- favorite/locked items are protected from bulk sell actions;
- identical rolled equipment does not stack;
- the inventory marks genuinely new gear but does not leave permanent `NEW` clutter;
- normal merchants may provide `Sell all unlocked Standard` and later `Sell all unlocked below <grade>` QoL once the player has seen those items;
- bulk actions always preview expected Gold and number of items;
- Mythic and favorited items are never included in default bulk actions.

A salvage system is **not part of the launch canon**. `EQUIPMENT_BALANCE.md` closes the forge/reforge loop without a salvage-material sink, so ordinary unwanted gear is sold. Salvage should be reconsidered only if a future explicitly authored system creates a real need rather than being added because another RPG has it.

---

# 12. Loot ground / pickup presentation

Loot rarity must be readable in the 3D world without turning every fight into particle spam.

Baseline:

- Gold/material pickup feedback is compact and may auto-route to Gold/Material Pouch where appropriate;
- Standard/Refined equipment uses subtle ground presentation;
- Superior gets a short distinct highlight;
- Exalted gets a stronger beam/marker and higher-impact pickup sound;
- Mythic gets a clearly unique pillar/marker, sound and loot toast;
- important presentation is not color-only.

`Loot Rarity` (current Fabric 26.2, MIT) is a useful code/behavior reference for ground beams, but its default colors/effects are not automatically the project's final visuals. Final shapes/colors must match `Lucifer` UI rarity language and actual Minecraft screenshot readability.

---

# 13. Merchant interaction with gear economy

Merchants complement target farming; they do not invalidate bosses/dungeons.

General rotating-stock expectations:

- Standard/Refined current-region gear appears regularly;
- Superior appears occasionally and is a meaningful purchase;
- Exalted is uncommon and expensive;
- Mythic boss/signature gear is not placed into ordinary 10-minute merchant rotation unless a specific authored merchant/quest explicitly exists for it.

Relative price intent:

- Standard — roughly 10–15 minutes of same-tier gross Gold income;
- Refined — roughly 15–25 minutes;
- Superior — roughly 30–45 minutes;
- Exalted — roughly 60–90 minutes;
- Mythic — source-specific; normal merchants do not define its baseline acquisition.

Existing `GAME_DESIGN.md` Gold bands remain authoritative where they are more specific.

---

# 14. Data contract for implementation

Loot content is data-driven.

Suggested data split:

```text
items/
  bases/
  mythics/
affixes/
  primary/
  offense/
  defense/
  resource/
  utility/
loot/
  regions/
  elites/
  minibosses/
  field_bosses/
  dungeons/
  treasures/
merchants/
materials/
```

Runtime code owns:

- validated roll procedure;
- server-authoritative reward ownership;
- weighted selection;
- duplicate-affix prevention;
- multiplayer personal loot;
- deterministic first-clear state;
- signature-material protection;
- inventory/overflow delivery.

Data owns:

- item/model/icon IDs;
- Item Lv/source parameters;
- affix pools and values;
- source drop probabilities;
- grade distributions;
- boss signature pools;
- regional equipment pools;
- first-clear reward candidates.

No Java switch statement should contain the final list of every boss drop or affix merely because it was faster to prototype.

---

# 15. Remaining pre-code content / presentation bindings

Loot-system rules and numerical equipment/forge rules are already closed between this file and `EQUIPMENT_BALANCE.md`. Gameplay source must not reopen them.

The remaining pre-code work is exact content/presentation binding that implementation must not invent:

1. region-by-region equipment/model/icon catalogs beyond the already-authored R01 mechanical catalog;
2. exact source file/model/hash binding for R01 items/resources as tracked by `R01_ASSET_INTAKE.md`;
3. every regional first-clear reward's real model/icon/source;
4. final Mythic identities only after each boss/item visual source passes quality review;
5. exact boss signature-material names/models derived from accepted boss anatomy/presentation;
6. later-region resource-node/item models, icons and provenance.

The following are **not open design tasks anymore**:

- weapon/armor base-stat curves by Item Lv and family;
- exact affix min/max curves by Item Lv;
- forge/reforge behavior and Gold costs;
- launch salvage policy;
- R01 mechanical equipment/resource catalog.

Those are canonical in `EQUIPMENT_BALANCE.md`. If a hard implementation constraint invalidates one of them, revise the canon first rather than silently reopening the decision in code.
