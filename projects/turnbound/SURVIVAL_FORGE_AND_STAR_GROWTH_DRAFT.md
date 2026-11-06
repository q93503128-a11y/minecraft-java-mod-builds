# TURNBOUND — Survival Forging and Star-Growth Integration

Status: **PARTIALLY APPROVED DESIGN / NO IMPLEMENTATION**
Date: 2026-11-06
Branch: `turnbound-content-rebuild`
Project: TURNBOUND (NOT TURNBOUND: RE)

## User-confirmed forge separation from vanilla Smithing Table — 2026-11-06
- **TURNBOUND gear forging is an independently registered custom forge/blacksmith workstation and recipe interface, separate from Minecraft's vanilla Smithing Table.** No repurposing or replacement of vanilla Smithing Table recipes, templates, armor-trim/smithing functions or player-survival inventory item behavior.
- The TURNBOUND forge creates/enhances **party character equipment** from survival ores and custom monster materials. Vanilla crafting/smithing remains useful for the Minecraft explorer's ordinary survival tools and armor.
- Allowing an NPC blacksmith in towns to use the same shared forge recipes is an architectural **proposal**; the exact station location, GUI, extra modules and whether the player can craft/place a forge remain unfinalized in detail. Keep the player's ability to access normal Minecraft crafting independent of TURNBOUND's equipment loops.
- Existing ★1–★6 rank level caps and duplicate +level cap-interaction are separate decisions. This separation does not resolve those mechanics.
- Documentation checkpoint only: there is no actual custom forge block, recipe serializer, Forge GUI or registered structure yet.

## 1. User-confirmed decisions

### Character level caps by current star rank

| Current character rank | Normal base XP level ceiling |
|---|---:|
| ★1 | Lv10 |
| ★2 | Lv20 |
| ★3 | Lv30 |
| ★4 | Lv40 |
| ★5 | Lv50 |
| ★6 | Lv60 |

- Native/born gacha rarity remains ★1–★5. ★6 is a **growth/promotion rank**, not a native ★6 draw.
- A new ★3 starter chosen from three normal characters therefore begins in an XP-growth band ending at **Lv30**, unless and until a future star promotion rule raises the character's current rank. The starter is not a special rarity or unique class.
- Code and persistence must model **nativeStars** (immutable collection identity) separately from **currentStars** (promotion rank), and **baseXpLevel** separately from **duplicateBonusLevel**. Every server-authoritative increase must enforce an agreed cap and preserve save/backwards migration boundaries.
- Previous frozen legacy implementation contained a helper `CanonicalData.levelCap` for 1–6 stars, but the later v1 runtime ignored `currentStars` and used base Lv60 globally. The latest user decision is a new authoritative directive, not a claim that the current build already does this.
- Duplicate acquisitions (+1 bonus level or rarity Essence) and Awakening stay as progression pillars by earlier user decision. Whether +levels can exceed the ★1–★6 **normal/base** cap, how effective stats treat them, whether a star upgrade requires leveling to the current cap, and exact promotion materials remain **unresolved**. Do not accidentally allow a ★1 character to skip meaningful progression by receiving duplication bonuses.

### Equipment acquisition
- Most normal character weapons, armor and accessories should be **crafted from gathered survival materials + components dropped by authored TURNBOUND creatures**, rather than direct combat equipment drops.
- More valuable/specialized inputs should give better and/or different gear outcomes: rarity/quality and interesting traits, not only a bigger linear ATK bonus.
- A **forge / blacksmith station** is a natural central crafting location for this progression. The exact station rules are design proposals below, not user-approved implementation details.
- Fixed/earned first-clear loot can still include unusual finished gear or signature items if later approved, but the *ordinary* progression loop should prioritize materials and discovery.

## 2. Proposed compact forge system (NOT YET APPROVED)

Player actions:
1. **Explore/mine:** collect normal Minecraft iron/copper/gold/rare mining materials or newly authored ores only if necessary.
2. **Fight/hunt:** defeat enemies (peaceful animals only after voluntary provocation) to collect monster material (horn, claw, hide, elemental core, etc.; all examples are conceptual).
3. **Explore dungeons/complete contracts:** acquire rare components, blueprints or special crafting access.
4. **Forge:** choose a known item type and see input requirements **and exact resulting attributes/trait before consuming anything**, then confirm the craft.
5. **Equip or enhance:** put the forged physical item on one of the 4 party characters, possibly upgrade to +10 using a simple resource/cost budget; no unnecessary repeated subcrafts.

Recommended minimal composition:
- **Base material**: sets equipment tier and primary attribute magnitude, e.g. common iron vs better metal.
- **Monster component**: sets distinctive deterministic trait or secondary stat, e.g. stronger defense vs resistance vs an action-Gauge interaction.
- **Rare catalyst/blueprint** (only for a limited number of genuinely rare recipes): unlocks unusual build choices or a high-end item and makes the dungeon/boss meaningful.

Suggested interface:
- A **placeable forge workstation block** usable from a player-built base, with NPC blacksmiths in villages using the same underlying server recipe rules so the player is not required to live by one town. Avoid two unrelated equipment-building systems.
- Small number of clear slots (recipe item and a few inputs), a robust preview of stats/traits, explicit ownership & recipe checks, no unrequested grade reroll roulette, no forced crafting timer/progress bar.
- Keep the normal inventory/recipe model whenever possible; if a dedicated forge recipe type is needed, implement it as data-driven recipes with a server-authoritative input/output transaction.

### Equipment quality and progression — proposed
- Retain legacy ordinary equipment slots: **weapon, armor, accessory**. Character-specific signature equipment is a separate special case and should not be mass-produced like generic gear without an explicit design decision.
- Retain the prior notion of equipment quality tiers **common/rare/epic/relic** as a candidate, not yet reapproved numeric or art canon. This **item tier** must be visibly distinct from characters' native ★ rarity and current ★ promotion rank.
- Better base materials generally provide a stronger baseline, but different monster components may enable viable situational tradeoffs. Avoid pure linear material inflation that obsoletes all earlier equipment instantly.
- Combat loot should primarily provide XP, coins if retained, materials and occasionally blueprints. Fully formed gear should become a special reward rather than the default.
- Keep the earlier equipment enhancement +0–+10 with no failure/destruction as proposed preserved mechanics; actual resource/Gold cost and transferability will be validated against survival gathering.
- Equipment should not be usable to trivialize the 1000-Gauge turn economy; treat any SPD, direct advance, reaction/tempo trait as an action-economy budget, not a cosmetic secondary line.

## 3. Resource-loop example (ILLUSTRATIVE, NOT CANON RECIPES)

| Action | Obtained | Progression use |
| --- | --- | --- |
| Mine common ore | base metals | craft baseline weapon/armor |
| Defeat small hostile monster | monster hide, claw, core | change item trait or upgrade specialization |
| Provoke and defeat peaceful fauna | food and useful hide or horn | survival consumables plus some equipment materials |
| Explore dangerous cave/dungeon | rare ore and difficult monster materials | better tier craft / recipe options |
| Finish a village contract | character XP, general resources, maybe summon currency | improve party and unlock further exploration |
| Beat a strong elite/boss | unusual component and possible recipe | meaningful signature/niche item options |

Crafted items should **support the four-character turn party**, while vanilla survival tools (pickaxe, axe, shovel) remain practical for gathering. Do not force players to win a long turn battle every time they need a trivial amount of leather or food.

## 4. Risks to solve before implementation

1. **Overlapping currencies:** previous Gold enhancement economy versus survival metals/materials; do not require both on every simple step merely to add friction. Determine whether Gold remains only for services/shops or for a subset of enhancements.
2. **Star cap vs duplication:** preserving duplicate bonus +levels must not undermine the new ★1–★6 normal-level progression. Resolve explicit effective level/stat and promotion eligibility semantics before shipping.
3. **Deterministic crafting:** if the same materials produce randomly rolled gear, better ingredients no longer guarantee the user can predict a better result. Prefer recipe preview and fixed primary/trait outcomes unless the player explicitly chooses an alternative.
4. **Recipes / infrastructure:** choose between ordinary crafting, a station plus menus, blacksmith services and specialized material composition without unnecessary microtransactions or menu overhead.
5. **Data-driven balance:** material quality, recipe item stats, trait IDs, monster drop sources and upgrade effects must have authoritative IDs, bounded server-side validation, and automated save/stacking tests once implementation begins.
6. **No forced survival regression:** missing vanilla cows, spiders, blazes and villagers cannot remove critical crafting paths or base building convenience without replacements.
7. **Multiplayer:** avoid item duplication or simultaneous use during forge transaction, equipment equip/unequip, world reload and disconnection.
8. **UI/asset quality:** use established production crafting interfaces and validated layout/texture/animation refs, not random black rectangles and placeholder icon spam; inspect a real Minecraft client screenshot.

## 5. Technical feasibility / background references

- NeoForge official custom recipe architecture defines recipe input, `Recipe`, `RecipeType`, `RecipeSerializer`; confirm exact 26.2 signatures before coding:
  https://github.com/neoforged/Documentation/blob/main/docs/resources/server/recipes/custom.md
- NeoForge official server recipe/data model as a data-driven starting point:
  https://github.com/neoforged/Documentation/blob/main/docs/resources/server/recipes/index.md
- Silent Gear uses materials and components to influence equipment strength/traits; useful high-level design reference, **not a dependency or permission to copy assets**:
  https://www.curseforge.com/minecraft/mc-mods/silent-gear
  https://modrinth.com/project/silent-gear
- Direct porting/reusing Tinkers'/Silent Gear code is NOT assumed compatible with the current Minecraft 26.2 NeoForge runtime; review license/API if implementing dependency later.

## 6. Approval and implementation boundaries

**Approved now:** ★1–★6 current-rank normal level limits at +10 increments; native rarity remains ★1–★5; gear primarily obtained via materials and forging, with monster drops contributing; better material should yield higher quality or valuable traits.

**Proposed, NOT approved:** exact forge machine configuration, one blacksmith per village, numeric material tiers, recipe list, forge UI/slots, specific resource names and monsters, cost formulas, upgrade conditions and balance.

**Not implemented:** new six-rank level-cap enforcement, star-promotion flow, material recipe engine, forge block, forge UI, monster component loot, integration tests or any new JAR.

Next collaborative unit: approve forge workflow and resource quality model; then separately resolve star-promotion and duplicate effective level behavior. Do not begin producing first playable route or visual assets for an unapproved crafting system.

## Confirmed duplicate-level option A — 2026-11-06
- Latest user choice: a character's current star rank limits only base XP level: star 1=Lv10, 2=Lv20, 3=Lv30, 4=Lv40, 5=Lv50, 6=Lv60. Native rarity remains 1–5 stars; rank 6 is progression-only.
- Existing duplicate reward: choose either +1 bonus level or native-rarity Star Essence per copy. Bonus levels cap at +10 and accumulate separately from base XP.
- Effective combat level = base XP level + duplicate bonus level, even if that exceeds the current-star base XP cap. Example: star 1, base Lv10 + bonus 3 gives effective Lv13; star 6, base Lv60 + bonus 10 gives effective Lv70.
- Bonus levels do not independently promote star rank, increase the XP cap or substitute for star-promotion requirements. Promotion prerequisites, costs, exact stat curve and final UI display remain undecided.
- This supersedes earlier statements marking bonus-level/cap interaction unresolved, but does not approve any new runtime feature or assert testing. Documentation only.

## User-approved sequential star promotion — 2026-11-06
- **Promotion is available when a character reaches the CURRENT STAR RANK'S BASE XP LEVEL CAP** and the player spends the required **promotion materials obtained through monster encounters and dungeons**. This is an AND condition, not either/or.
- Rank grows **one star at a time** (★1→★2→★3→★4→★5→★6). All native ★1–★5 characters are eligible to grow eventually to ★6. No native ★6 draws.
- Each current star rank has an independent normal base XP ceiling: ★1 Lv10, ★2 Lv20, ★3 Lv30, ★4 Lv40, ★5 Lv50, ★6 Lv60. Example: ★3 Lv30 (+5 duplicate levels) may ascend to ★4 after paying the appropriate materials; its normal base XP ceiling then becomes Lv40.
- **Duplicate bonus levels do NOT satisfy the base XP cap requirement:** ★3 Lv25(+5) has effective combat level 30 but still cannot promote until its actual base XP level reaches 30.
- Promotion raises `currentStars` and the **base XP cap** but must not change immutable `nativeStars`, remove already-earned +bonus levels, or automatically restore old character assets/old balance.
- **No specific costs are approved yet**: promotion-material names, quantities, the split between ordinary monsters, elites and dungeons, any optional Gold component, future star stat changes, level/XP carry behavior and relation to once-only Awakening need separate design.
- Promotion should create meaningful mining/monster/dungeon gameplay without overwhelming inventory with one new currency per star or requiring repetitive mandatory dungeon grinding. Proposed UX/recipe implementation is not an approved final design.
- **DESIGN ONLY:** no actual rank-promotion screen, save schema or verified playable progression exists in the current empty-content build; no code/build/CI work was requested here.
