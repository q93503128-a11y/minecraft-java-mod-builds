# Open-World RPG — Recovery / Alchemy / Cooking / Appearance Canon

> Status: **DESIGN CANON — recovery-consumable, light production and armor/apparel presentation rules locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Combat math: `COMBAT_BALANCE.md`  
> Equipment: `EQUIPMENT_BALANCE.md`, `LOOT_ECONOMY.md`  
> Opening flow: `R01_VERTICAL_SLICE.md`  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins.

This document closes three implementation-time gaps that should not be improvised in code:

1. how HP recovery, potions and rest work without potion-spam or hunger busywork;
2. how R01 alchemy/cooking and light profession growth connect gathering back to the settlement without becoming a crafting grind; and
3. how combat armor, cosmetic appearance and NPC clothing remain external-first and visually coherent even when loot is randomized.

The design goal remains **fast open-world action RPG play with low management friction**. Recovery should create timing/resource decisions; production should give gathered materials useful destinations; appearance should make the character look authored rather than like five unrelated random loot pieces.

---

# 1. External precedents and adoption boundaries

## 1.1 Diablo IV — bounded, meaningful potion use

Current Blizzard 2.5 healing design uses a small base potion capacity and a large percentage-based heal rather than dozens of weak potion upgrades. The published 2.5 notes specify:

- base potion capacity 4;
- each potion heals 35% max Life instantly;
- potion upgrade treadmill removed.

Project adoption:

- a **4-dose quick recovery belt** at baseline;
- a healing dose restores **35% MaxHP**;
- no Healing Potion I → II → III → IV numeric-upgrade ladder.

Project differences:

- doses are backed by real purchased/crafted consumable items so Gold/alchemy remain meaningful;
- doses do not regenerate for free every 30 seconds;
- use has a visible animation/commitment and shared recovery-item lockout.

Reference: `https://news.blizzard.com/en-us/article/24244466/diablo-iv-patch-notes-2-5`

## 1.2 Elden Ring — limited between-rest recovery and rest-point refill

Useful precedent:

- Flask of Crimson Tears begins with four charges;
- resting at a Site of Grace replenishes usable charges;
- recovery is a dedicated combat action rather than food-spam from a giant inventory;
- HP and skill-resource recovery can be treated as separate choices.

Project adoption:

- only a small number of quick combat-recovery doses are available at once;
- shrines/camps/inns are natural points to prepare/reload the quick belt;
- HP and Mana recovery can compete for limited quick-belt capacity if the player chooses.

Project differences:

- rest does **not** create free consumables; it only reloads doses from actual carried reserve items;
- no Golden-Seed/Sacred-Tear style permanent flask-upgrade collectible loop at launch.

Reference: `https://eldenring.wiki.gg/wiki/Flask_of_Crimson_Tears`

## 1.3 Guild Wars 2 — one active food identity

Useful precedent:

- food applies one `Nourishment`-style preparation effect;
- eating another food replaces the previous nourishment instead of infinitely stacking meal buffs;
- common food durations are long enough that food is preparation, not a button pressed every fight.

Project adoption:

- one active meal buff at a time;
- **20 minutes of active playtime** baseline duration;
- food persists through defeat and is replaced, not stacked;
- food is optional preparation and never a hunger meter requirement.

Project differences:

- no kill-EXP food bonus baseline because combat/quest EXP pacing is already deliberately authored;
- early food bonuses remain compact enough that eating is helpful but not mandatory.

Reference: `https://wiki.guildwars2.com/wiki/Nourishment`

## 1.4 Quaternius / KayKit — actual recovery, food, outfit and work presentation

Accepted visual/production families:

- Quaternius `Modular Character Outfits - Fantasy` — CC0; 12 outfits, 62 modular parts, 3 texture variants, humanoid rig, compatible with Universal Animation Library;
- Quaternius `Universal Base Characters` — CC0 NPC/body source where a full custom humanoid NPC is appropriate;
- Quaternius `Fantasy Props MegaKit` — CC0; potion bottles, books, cauldrons, tools, furniture and market/service props;
- KayKit `Restaurant Bits` — CC0; 140+ food/kitchen models including ingredients and cooked states;
- KayKit `RPG Tools Bits` — CC0; hammer/anvil/tools/blueprints and other production props;
- KayKit `Character Animations` — CC0; 161 humanoid animations including hammering, generic work, pickaxe, fishing and other tool actions;
- Quaternius `Universal Animation Library` / `UAL2` — CC0 locomotion/combat/work-motion pool and retargeting reference.

Important current limitation:

- the current KayKit Character Animations page explicitly says eating/drinking animations are **planned**, not currently part of the guaranteed set;
- therefore a production drink/eat action must bind an actually verified external clip before implementation;
- a legally usable non-redistributable source such as Adobe Mixamo may be used **LOCAL_ONLY** for the playable build if the exact clip passes quality review, while raw animation files remain outside the public GitHub repository;
- do not substitute a hand-to-face two-keyframe placeholder merely because a free CC0 eating clip was not yet selected.

References:
- `https://quaternius.com/packs/modularcharacteroutfitsfantasy.html`
- `https://quaternius.com/packs/universalbasecharacters.html`
- `https://quaternius.com/packs/fantasypropsmegakit.html`
- `https://kaylousberg.itch.io/restaurant-bits`
- `https://kaylousberg.itch.io/rpg-tools-bits`
- `https://kaylousberg.itch.io/kaykit-character-animations`
- `https://quaternius.com/packs/universalanimationlibrary.html`
- `https://quaternius.com/packs/universalanimationlibrary2.html`

---

# 2. HP natural recovery

Natural HP recovery exists, but it must remain slow enough that rest/recovery items have purpose.

Baseline:

```text
start delay after last hostile HP damage: 8.0 s
only while not in active encounter/combat state
recovery rate: 0.40% MaxHP per second
```

Rules:

- natural recovery can eventually reach full HP; there is no arbitrary 50% cap;
- from nearly empty, waiting for full recovery takes several minutes and is intentionally inferior to normal recovery tools;
- taking hostile HP damage stops the regeneration immediately and restarts the 8 s delay;
- Barrier loss alone does not count as HP damage unless the underlying attack also damages HP;
- Poison/Burning/Bleed damage counts as hostile HP damage and prevents passive healing while ticking;
- this is not shown as a separate buff icon.

`COMBAT_BALANCE.md` remains authoritative for Mana/Stamina recovery.

---

# 3. Quick Recovery Belt

The player has a small quick-use recovery belt separate from the ordinary backpack grid.

```text
baseline loaded dose capacity: 4
```

This is **not** four free items and not another general inventory.
Each loaded dose corresponds to one real eligible consumable removed/reserved from the player's carried inventory.

## 3.1 Why the belt exists

Without a belt, the existing stack cap of 20 would let a long boss fight become repeated inventory potion spam.
The belt limits **in-combat access** while still allowing the player to carry sensible expedition reserves.

## 3.2 Loading rules

- at a shrine, inn or legal camp rest, compatible doses auto-reload from carried reserve items according to the player's saved belt setup;
- manual reload is allowed while out of combat and stationary through the inventory/quick-belt interaction;
- manual reload duration: **1.0 s** for the whole belt operation, not one second per bottle;
- no reload during an active boss encounter or while combat state is active;
- if reserve items do not exist, empty belt slots remain empty;
- rest never creates free potion items.

The belt can mix eligible recovery items. Example valid configurations:

```text
4 Healing Potion
3 Healing Potion + 1 Focus Draught
2 Healing Potion + 1 Focus Draught + 1 Cleansing Tonic
```

There is no optimal mandatory ratio enforced by class.

---

# 4. Recovery-item use language

All quick recovery items use a shared player-action presentation contract.

Baseline drink action:

```text
total action: 0.95 s
effect resolution point: 0.72 s
movement speed during action: 65%
shared Recovery lockout after successful resolution: 6.0 s
```

Rules:

- dodge/guard cannot be performed during the committed pre-resolution portion;
- before 0.72 s, a legal cancel by a dedicated high-priority state such as forced knockdown/poise break cancels the item and does not consume the dose;
- after resolution, the dose is consumed even if the player is hit during recovery frames;
- ordinary light HP damage alone does not cancel the drink unless it actually interrupts the player's action under the normal poise rules;
- no drinking while downed, mounted, climbing or in another incompatible committed animation;
- the hand/bottle animation must be an accepted external animation source and the bottle prop must be an accepted external model;
- first-person and third-person timing must agree with the server resolution point.

The 6 s lockout is shared by Healing Potion / Focus Draught / Cleansing Tonic so the player cannot drink three categories back-to-back in two seconds.

---

# 5. Baseline combat consumables

## 5.1 Healing Potion

R01 and all ordinary settlements reliably stock the basic Healing Potion.

```text
restore: 35% MaxHP
heal timing: at 0.72 s drink resolution
critical: no
R01 price: **30 Gold**
stack cap in backpack: 20
quick-belt eligible: yes
```

Rules:

- percentage healing avoids low-level/full-endgame potion versions and matches the decision to avoid an upgrade treadmill;
- Healing Received modifiers apply to the restored amount;
- the resulting heal cannot exceed MaxHP;
- no HoT is added at baseline.

R01 alchemy recipe:

```text
2 Healing Herb
+ 5 Gold service fee
→ 1 Healing Potion
```

This keeps merchant purchase convenient while making gathered herbs economically useful.

## 5.2 Focus Draught

Role: controlled Mana recovery for skill-heavy builds.

```text
restore: 40% MaxMana total
25% MaxMana at resolution
+ 15% MaxMana over the following 3.0 s
R01 price: **35 Gold**
stack cap: 20
quick-belt eligible: yes
```

The 3 s tail stops the item from being a full instant-combo reset while still making it useful in boss combat.

R01 recipe:

```text
1 Healing Herb
+ 1 Louxia Glow
+ 8 Gold service fee
→ 1 Focus Draught
```

## 5.3 Cleansing Tonic

Role: active answer to R01 poison/burn/bleed/control without creating one antidote item per status color.

```text
on resolution: remove all currently active statuses tagged minor_dispellable
major statuses: unaffected
R01 price: **40 Gold**
stack cap: 20
quick-belt eligible: yes
```

After cleansing:

```text
negative buildup received: -20%
duration: 10 s
```

This reduction affects buildup gain only; it is not blanket status immunity.

R01 recipe:

```text
1 Healing Herb
+ 1 Louxia Glow
+ 10 Gold service fee
→ 1 Cleansing Tonic
```

`STATUS_AND_R01_ENCOUNTERS.md` remains authoritative for what counts as minor/major dispellable.

## 5.4 No baseline potion families beyond real need

Do not create early-game stacks of:

- Fire Potion;
- Frost Potion;
- Lightning Potion;
- Minor Poison Potion;
- Greater Poison Potion;
- Potion of +3% STR;
- Potion of +3% DEX;
- Potion of +3% INT.

Add a new combat consumable only when a real encounter/build creates a distinct use case that is not already served by gear, food, class skills or the three baseline recovery items above.

---

# 6. Rest behavior

Rest points restore character resources efficiently but do not manufacture economic items.

## Shrine/checkpoint rest

- fully restore HP;
- fully restore Mana;
- fully restore Stamina;
- clear ordinary dispellable negative statuses;
- reload Recovery Belt from carried reserve consumables;
- activate/update checkpoint state;
- expose valid fast-travel/class/ritual options defined elsewhere.

## Inn rest

- fully restore HP/Mana/Stamina;
- clear ordinary dispellable negative statuses;
- reload belt from carried reserves;
- may offer paid prepared meals through the inn/food service;
- does not create free potions.

## Camp rest

- only at a legally deployed camp outside combat/no-camp authored volumes;
- fully restore HP/Mana/Stamina;
- reload belt from carried reserves;
- allow camp cooking from known recipes;
- no fast travel baseline;
- no free alchemy lab at baseline; portable alchemy is a later explicit unlock if it earns its complexity.

Dungeon/boss checkpoint design decides whether a particular rest interaction resets nearby encounters. Do not silently turn every heal point into enemy-respawn farming behavior.

---

# 7. Food / Cooking

Food is preparation and world flavor, not hunger maintenance.

## 7.1 Nourishment rule

```text
maximum active meal buffs: 1
baseline duration: 20 active minutes
```

Rules:

- consuming another meal replaces the previous meal buff;
- duration counts only while the character is actively loaded/playing;
- nourishment persists through defeat/respawn;
- meal effects do not stack with themselves by eating multiple copies;
- no Hunger/Saturation meter is introduced;
- meals are not usable during active combat;
- ordinary prepared meals restore **15% MaxHP** immediately when eaten out of combat, then apply their nourishment effect;
- meal healing is preparation QoL, not a combat-potion replacement.

Baseline meal-use action:

```text
action: 1.20 s
combat state required: false
```

The eating animation must be an accepted external clip; do not use vanilla food bobbing as the final third-person presentation.

## 7.2 R01 launch meals

R01 begins with only three useful recipes.

### Herbed Louxia Roast

```text
2 Louxia Meat
+ 1 Healing Herb
```

Nourishment:

```text
MaxHP +6%
duration: 20 min
```

### Trail Skewers

```text
1 Louxia Meat
+ 1 Healing Herb
```

Nourishment:

```text
Stamina recovery +10%
duration: 20 min
```

### Glow Broth

```text
1 Louxia Meat
+ 1 Louxia Glow
+ 1 Healing Herb
```

Nourishment:

```text
Mana recovery +10%
duration: 20 min
```

These use current canonical R01 creature/gathering materials instead of introducing three new seasoning currencies.

Exact food model bindings come from the CC0 KayKit Restaurant Bits / compatible Quaternius food-prop family before implementation.

## 7.3 Meal balance rules

- early meals normally modify one main preparation axis by about **6–10%**;
- meals never multiply final damage by 25% at launch;
- a meal should help a build/route, not be required for the build to function;
- do not attach bonus EXP/Gold to ordinary food at baseline;
- food should not need to be refreshed every five minutes.

---

# 8. Alchemy service

R01 healer/alchemist becomes mechanically relevant as soon as the player first obtains Healing Herb.

Service screen:

- uses the existing Lucifer-family project UI grammar;
- lists known recipes, output, owned ingredients and resulting belt/recovery category clearly;
- consumes eligible materials from **Material Pouch first, then same-player Material Vault for any remainder** when used at a settlement alchemy service; portable/non-settlement alchemy uses Material Pouch only;
- supports `Craft 1`, `Craft 5` where affordable, and `Craft Max` with a confirmation summary;
- never requires moving ingredients one-by-one into arbitrary cauldron slots merely to simulate crafting labor.

The physical station uses an external cauldron/table/bottle/tool composition from Quaternius Fantasy Props MegaKit or another accepted coherent source.
NPC working motion uses a real external work animation.

---

# 9. Cooking service

Cooking is available through:

- inn/tavern kitchen/service;
- a proper home kitchen if later furnished;
- deployed camp cooking where the camp supports it.

Rules:

- known recipes are selected directly;
- settlement cooking consumes eligible materials from **Material Pouch first, then same-player Material Vault for any remainder**; home/camp/portable cooking uses Material Pouch only;
- batch cooking is allowed;
- no timing minigame at baseline;
- no chance to burn/fail ordinary food;
- restaurant/kitchen physical props and food models use external asset families;
- cooking is optional support/economy content, not a gate for combat progression.

---

# 10. Light profession mastery

Smithing, Alchemy and Cooking use the same **small five-rank mastery shape**.

```text
Mastery Rank: 1..5
```

This is not a separate combat Lv and does not require repetitive mass crafting.

## 10.1 Mastery progress source

Mastery progress comes from **distinct practice**, not item volume.

Accepted progress events:

- first successful craft of a newly learned recipe;
- first completion of an authored profession order/contract;
- learning a regional technique from an NPC/book/discovery;
- first successful use of a new region's major ingredient/material category in that profession.

Repeatedly crafting the same cheap recipe after its first successful learning use gives **no mastery progress**.

There is no `craft 500 weak potions` optimal path.

## 10.2 Rank thresholds

Internal `Mastery Insight` count is not an inventory item/currency.

```text
Rank 1: 0
Rank 2: 4 Insights
Rank 3: 9 Insights
Rank 4: 15 Insights
Rank 5: 22 Insights
```

Enough Insight opportunities exist that the player does not need every regional recipe/order.

## 10.3 What mastery may grant

Mastery can unlock:

- recipes;
- region-specific processing options;
- batch convenience;
- small service-fee reduction;
- authored higher-quality recipe variants.

It does **not** grant a universal stacking `+5% all stats per rank` power layer.

Baseline fee reduction:

```text
Rank 1: 0%
Rank 2: 2%
Rank 3: 4%
Rank 4: 6%
Rank 5: 8%
```

This is meaningful over time but not strong enough to make profession grind mandatory.

---

# 11. Armor / apparel visual canon

All player-visible armor, robes, clothes and NPC outfits are external-first exactly like weapons, UI and animation.

This includes:

- combat light armor;
- medium/leather/traveler armor;
- heavy armor;
- mage/cleric robes;
- class/trainer outfits;
- civilian/tavern/merchant clothing;
- smith/alchemist/stable/guard profession clothing;
- named boss/signature armor;
- cosmetic clothing/appearance overrides.

Forbidden as finished presentation:

- recolored vanilla iron/diamond/netherite armor pretending to be project gear;
- flat Minecraft skin recolors used as the entire visible robe/plate solution;
- one generic internally invented robe reused for every caster;
- placeholder armor with correct stats but no accepted 3D appearance source.

## 11.1 R01 armor source family

Primary R01 source:

**Quaternius `Modular Character Outfits - Fantasy`**.

Current published properties:

- 12 game-ready outfits;
- 62 modular parts;
- 3 texture variations per outfit;
- humanoid rig;
- compatible with Universal Base Characters;
- compatible with Universal Animation Library;
- CC0.

R01 already-canonical armor archetypes remain:

- `River Scholar Garb` — Light;
- `Wayfarer Leathers` — Medium;
- `Ironbound Guard` — Heavy.

Asset intake selects exact modular parts from the pack for each slot/family before implementation.
The names/stat identities do not authorize inventing a visually unrelated model.

## 11.2 Rendering path

Current preferred runtime:

- `Armor Model API` on Fabric 26.2;
- current listing supports custom `.geo.json` armor geometry including robes, pauldrons, hats, skirts and per-slot rendering;
- MIT license according to current Modrinth listing.

Project adaptation:

- preserve accepted external silhouette/design as much as Minecraft player proportions/readability allow;
- convert/retarget into the supported armor skeleton/geometry pipeline;
- test walk/sprint/dodge/guard/cast/jump/sit/mount animations for clipping before an armor family is accepted;
- do not run a second competing armor rendering stack unless a hard blocker is proven.

Reference: `https://modrinth.com/mod/armor-model-api`

---

# 12. Equipment appearance and grade presentation

Randomized loot may change stats without turning the character into a rainbow patchwork.

Rules:

- item **base family** owns the actual model/silhouette;
- grade does not replace the whole model with a random unrelated shape;
- Standard → Refined → Superior → Exalted can use restrained accepted material/trim/detail variants where the external source supports them;
- color alone is never the only grade signal;
- Mythic/signature gear may have its own external model/source because its identity is authored;
- the inventory icon is rendered/derived from the same accepted external model family shown on the character.

No `purple rarity = entire armor suddenly neon purple` rule.

---

# 13. Wardrobe / appearance override

A lightweight wardrobe prevents good-stat loot from forcing an ugly mixed outfit.

## 13.1 Unlock

When the player first legitimately owns an eligible visible armor/apparel item:

- that item's accepted appearance ID becomes permanently available in the player's Wardrobe collection;
- the physical item may later be sold without deleting the unlocked appearance;
- salvage is not required because salvage is not a baseline economy system.

The unlock is server-authoritative persistent progression.

## 13.2 Applying appearance

Visible appearance override slots:

- Head;
- Chest;
- Legs;
- Gloves;
- Boots.

Rules:

- appearance changes stats **never**;
- overrides can be changed while out of combat from the Inventory `Appearance` tab;
- town/home wardrobe furniture opens the same UI as a world-flavor shortcut rather than a mandatory trip;
- applying an already-unlocked appearance is free;
- no Transmog Token currency;
- `Hide Helmet` is a free toggle;
- clearing an override shows the actual equipped item's appearance again.

## 13.3 Weapon/off-hand appearance

Weapon appearance override is stricter because visuals communicate attack reach/timing.

Allowed only when:

- same weapon family;
- compatible handedness;
- compatible reach profile;
- animation/hitbox presentation remains truthful.

Examples:

- sword → another sword: normally allowed;
- greatsword → dagger: forbidden;
- tower shield → tiny focus orb: forbidden;
- bow → crossbow: forbidden.

A cosmetic weapon may never make the visible range materially disagree with the server hitbox.

## 13.4 Color variants

Use authored source-pack color/material variants.

- Quaternius outfit pack's existing texture variations are valid appearance options after intake;
- there is no baseline dye-resource economy;
- if future dyeing is added, it must be based on actual compatible external material/texture workflow rather than arbitrary RGB sliders that destroy the art direction.

---

# 14. NPC clothing / social readability

NPC apparel must communicate role before floating UI text does.

Starting settlement baseline role families:

- guard;
- smith;
- guild/trainer;
- inn/tavern worker;
- alchemist/healer;
- merchant;
- stable handler;
- civilian/traveler.

Use modular external outfit parts/props to create coherent variants while keeping the same settlement visual family.

Rules:

- not every smith is an identical clone;
- variation comes from accepted modular parts, hair/head choices and authored texture variants rather than random unrelated skins;
- role silhouettes remain readable at normal Minecraft camera distance;
- NPC work animations use external motion sources when visible;
- profession props match the action: smith hammer/anvil, alchemist bottle/cauldron, stable tack/brush, etc.

---

# 15. Player motion compatibility with apparel

Armor acceptance is not complete just because the idle pose looks good.

Every visible armor family must be checked against at least:

- walk;
- sprint;
- directional dodge/roll/dash;
- jump;
- landing;
- guard;
- perfect-guard/parry reaction;
- representative light/heavy weapon attacks;
- spell cast;
- drink/eat;
- down/revive;
- mount/dismount.

If a robe/skirt/pauldron clips catastrophically during the accepted external dodge animation, solve the geometry/rig/animation binding or select another external piece. Do not hide the problem by reverting to a vanilla pose.

---

# 16. R01 service economy anchors

R01 prices remain inside the existing master economy bands.

| Service/item | Baseline |
|---|---:|
| Healing Potion purchase | 30 Gold |
| Focus Draught purchase | 35 Gold |
| Cleansing Tonic purchase | 40 Gold |
| Healing Potion craft fee | 5 Gold |
| Focus Draught craft fee | 8 Gold |
| Cleansing Tonic craft fee | 10 Gold |
| basic prepared inn meal | 15–25 Gold |
| appearance override | free |
| Hide Helmet | free |
| basic shrine rest | free |

These keep essential recovery below the existing target of roughly one third of gross income while still making herbs, Louxia materials and merchants relevant.

No inn-rest tax, wardrobe tax or equipment-repair tax is added merely to create Gold sinks.

---

# 17. UI requirements

All service/appearance UI uses `UI_DIRECTION.md` Lucifer-family grammar.

Recovery HUD:

- one compact Recovery Belt indicator with 4 dose pips;
- selected consumable icon and remaining loaded count;
- 6 s lockout shown by radial/tint treatment;
- no giant potion menu over combat.

Alchemy/Cooking:

- left/category area only when enough recipes justify it;
- recipe visual + outcome in main area;
- owned/required ingredients shown together;
- `Craft 1 / 5 / Max` controls;
- clear Material Pouch consumption;
- no vanilla crafting-grid reskin.

Appearance:

- character preview is primary;
- five visible armor override slots plus helmet toggle;
- owned appearance catalog/filter;
- actual equipped stats remain in Equipment tab, not duplicated into Wardrobe;
- source model preview must match in-world geometry.

---

# 18. Data contract

Suggested data ownership:

```text
consumables/
  recovery/*.json
food/
  meals/*.json
professions/
  alchemy.json
  cooking.json
  smithing.json
recipes/
  alchemy/r01/*.json
  cooking/r01/*.json
appearance/
  armor_families/*.json
  wardrobe/*.json
  npc_outfits/*.json
assets/
  source_bindings/*.json
```

Recovery item minimum:

```text
id
category
stack_cap
quick_belt_eligible
use_duration
resolve_time
shared_lockout
hp_restore_percent
mana_restore_profile
cleanse_tags
model_source_id
icon_source_id
animation_source_id
sound_source_id
```

Meal minimum:

```text
id
ingredients[]
out_of_combat_heal_percent
nourishment_id
nourishment_magnitude
duration_active_seconds
food_model_source_id
consume_animation_source_id
```

Appearance binding minimum:

```text
appearance_id
visible_slot
source_pack
exact_source_file
license
adoption_mode
texture_variant
converted_geo_file
icon_render_source
compatible_motion_profiles[]
public_repo_safe
```

NPC outfit minimum:

```text
outfit_id
role_tags[]
base_character_source
modular_parts[]
texture_variant
prop_bindings[]
work_animation_bindings[]
```

---

# 19. Pre-code asset-intake acceptance

Before this subsystem is marked asset-ready:

1. select exact Quaternius outfit part files for R01 Light / Medium / Heavy families;
2. convert one full set through Armor Model API and inspect it in the actual 26.2 Fabric client;
3. verify the set under sprint, dodge, heavy attack, cast and Trail Stag mount poses;
4. select exact potion bottle models and inventory icons from the accepted external prop family;
5. select exact R01 meal models from KayKit Restaurant Bits or another coherent CC0 pack;
6. select exact external drink and eat animation clips — KayKit's current page does not yet guarantee them, so no placeholder is accepted;
7. bind smith/alchemist/cook visible work actions to real external tool/work animations;
8. verify third-person action timing against server heal/use resolution;
9. verify Wardrobe override sync with a second client before claiming multiplayer-tested appearance;
10. take real Minecraft screenshots of Light / Medium / Heavy / civilian / profession outfits at normal gameplay FOV and GUI scale.

---

# 20. First implementation acceptance targets

1. player cannot access more than 4 loaded recovery doses during one uninterrupted boss encounter;
2. belt reload consumes real reserve items and never creates free bottles;
3. Healing Potion restores exactly 35% MaxHP before Healing Received modifiers;
4. recovery action resolve point and visible drinking animation differ by at most one server tick;
5. light damage does not randomly cancel the potion unless the normal poise/action rules actually interrupt the player;
6. all three R01 consumables share the 6 s Recovery lockout;
7. food cannot be consumed in active combat;
8. one meal replaces the previous Nourishment rather than stacking;
9. repeated crafting of the same cheap recipe cannot grind profession mastery indefinitely;
10. Material Pouch feeds alchemy/cooking directly;
11. R01 armor uses accepted external 3D geometry rather than vanilla recolor presentation;
12. randomized affixes do not change the armor base silhouette arbitrarily;
13. Wardrobe changes visuals only and never stats;
14. weapon appearance overrides cannot invalidate visible reach/handedness;
15. Hide Helmet is free and does not unequip the real item;
16. armor does not catastrophically clip during the canonical external dodge/roll animation;
17. NPC profession clothing/props/work motions are external-source bound rather than generic villager presentation.

---

# 21. What this pass closes

Closed for implementation:

- slow natural HP recovery;
- 4-dose Recovery Belt behavior;
- potion use timing/commitment/shared lockout;
- Healing Potion / Focus Draught / Cleansing Tonic behavior, R01 prices and recipes;
- shrine/inn/camp recovery boundaries;
- one-at-a-time food Nourishment model and 20-minute duration;
- three R01 meals and their exact early buffs;
- non-grindy 5-rank light profession mastery shape;
- batch-friendly alchemy/cooking UI behavior;
- external-first armor/apparel/NPC clothing rule;
- R01 outfit family source and Armor Model API rendering direction;
- lightweight Wardrobe/Hide Helmet rules;
- same-family weapon appearance restrictions;
- player-motion compatibility acceptance for armor;
- exact external-asset intake requirements for consume/work/outfit presentation.

Still intentionally later:

- R02+ regional meals/potions/outfit families;
- exact drink/eat animation filename after asset intake;
- higher-region profession recipes;
- later named cosmetic rewards;
- actual tuning after client playtest.

R01 Camp acquisition/deployment and the baseline Alderford furnishing catalogue are **not later design work anymore**: `GATHERING_FISHING_CAMP_HOUSING.md` + `R01_CONTENT_BIBLE.md` own those exact rules. Remaining Camp/furniture work is external model binding, world placement, implementation and playtest.