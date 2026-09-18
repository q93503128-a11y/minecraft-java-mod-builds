# Open-World RPG — Fishing Collection / Housing Market Canon

> Status: **DESIGN CANON — user-directed refinement of fishing collection and property progression**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Field systems baseline: `GATHERING_FISHING_CAMP_HOUSING.md`  
> UI: `UI_DIRECTION.md`  
> Economy: `GAME_DESIGN.md`, `LOOT_ECONOMY.md`  
> External provenance: `EXTERNAL_SOURCES.md` plus the source review recorded in this file  
> Rule: if this file conflicts with the older fishing/housing details in `GATHERING_FISHING_CAMP_HOUSING.md`, **this file is the authoritative refinement for fishing collection, property ownership/resale and furnishing progression**. `GAME_DESIGN.md` still wins over both.

This pass follows the user's explicit direction:

- fishing is a small but satisfying optional collection activity;
- caught fish belong in a real codex/collection;
- fish have practical uses such as cooking and selling;
- rod presentation, fish models, fishing motion and codex UI remain external-first;
- settlements contain multiple authored empty houses of different sizes;
- the player owns one residence at a time, sells the old house and moves into a larger one later;
- interior furnishing uses external high-quality furniture/system work where possible rather than an improvised internal decoration set.

The result must remain a **side loop that enriches exploration**, not a second progression game that players must grind.

---

# 1. External-first implementation direction

## 1.1 Fishing mechanics / code candidates

### Simple Fishing Overhaul

Current review on 2026-09-15:

- Minecraft 26.2;
- Fabric + NeoForge;
- client/server;
- MIT;
- source available;
- core concept: the fishing rod interacts with actual fish present in the water instead of inventing an invisible loot-only fish.

Project status:

- **strong `CODE_CANDIDATE`** for hook/target/physical-fish interaction architecture;
- inspect and selectively reuse/port compatible MIT logic where it reduces risk;
- do not inherit vanilla fish ecology or loot tables, because this project owns the visible non-vanilla aquatic ecosystem.

Source:
`https://modrinth.com/mod/simple-fishing-overhaul`

### Better Fishing

Current review on 2026-09-15:

- Minecraft 26.2 Fabric release exists;
- interactive tension/progress minigame;
- physical fish rendering on the hook;
- simulated 3D fishing line;
- procedural rod/arm presentation with its companion setup;
- ARR.

Project status:

- **`REFERENCE / LOCAL_ONLY DEPENDENCY CANDIDATE`**;
- excellent presentation benchmark for line physics, tension feedback and a fish visibly coming out of the water;
- do not copy its code/assets into the public repository;
- direct local dependency use is allowed only if integration with the project-owned fish tables, external fish models and server authority proves clean in a real 26.2 test;
- if it fights the canonical fish/state model, keep it as reference and use permissive code instead.

Source:
`https://modrinth.com/mod/better-fishing-system`

### Robin's Fishery

Current review on 2026-09-15:

- Minecraft 26.2 Fabric;
- MIT;
- adds biome-dependent fish;
- deliberately cooks multiple fish into a generic cooked-fish result to reduce inventory bloat.

Project status:

- **`CODE_CANDIDATE / REFERENCE`** for biome/region fish-table architecture and anti-inventory-bloat decisions;
- its species/content names are not automatically adopted;
- project fish remain external-visual-first and region-authored.

Source:
`https://modrinth.com/mod/fishery`

Do not install three overlapping fishing-overhaul mods at once. The production build chooses one ownership path after a narrow technical comparison.

## 1.2 Rod / humanoid fishing presentation

Already accepted direction remains:

- KayKit Character Animations 1.1 fishing motion family:
  - `Fishing_Cast`
  - `Fishing_Bite`
  - `Fishing_Catch`
  - `Fishing_Idle`
  - `Fishing_Reeling`
  - `Fishing_Struggling`
  - `Fishing_Tug`
- KayKit `RPG Tools Bits` is CC0; its paid EXTRA tier explicitly adds fishing assets and is a legal private-project candidate if purchased normally;
- a free external rod of equal quality may replace the paid EXTRA model after actual visual comparison.

The player-facing rod cannot remain the vanilla rod merely because the mechanics are finished.

## 1.3 Fish model / icon sources

Primary current 3D family:

### Quaternius Animated Fish / LowPoly Animated Fish

Creator-controlled/current and creator-uploaded historical pages both expose the pack as CC0.

- 7 animated aquatic species;
- rigged;
- swim animation;
- low-poly style compatible with the project's stylized external creature direction.

Sources:
- `https://quaternius.com/packs/animatedfish.html`
- `https://opengameart.org/content/animated-fish`

License handling still follows the project's package-specific Quaternius rule. Preserve the exact acquisition source/license evidence and local hash before committing raw files.

Additional fish families may be admitted only when their style, license and rigging survive the same intake process. Do not create dozens of weak recolors only to inflate the codex count.

For codex/inventory icons, preferred order is:

1. render the accepted 3D fish model into a readable orthographic icon;
2. adapt that render into the shared Lucifer pixel/UI language;
3. use a separate external icon only when it depicts the exact accepted fish identity better.

## 1.4 Fish codex UI

Visual asset family remains:

- Foozle `Lucifer - RPG UI` — CC0;
- supporting `Lucifer - Equipment` / Kenney border primitives only when useful.

Layout/reference direction:

- DREDGE encyclopedia: one fish gets a readable detail entry with catch count, location, size/value and variant/rarity information;
- Stardew Valley collection: a fast grid makes overall completion visible at a glance.

These games are **layout/information references only**. Their proprietary art is not copied.

Production structure:

```text
Fish Codex
├─ collection grid / silhouette overview
├─ region-water filters
└─ selected species detail
   ├─ accepted fish render/icon
   ├─ rarity
   ├─ known habitat/region
   ├─ catch conditions learned so far
   ├─ times caught
   ├─ personal best size
   ├─ typical/base sell value
   ├─ cooking uses
   └─ trophy/display status
```

No black debug panel, vanilla recipe-book reskin or unrelated scrapbook art family is accepted.

---

# 2. Fishing's role in the full RPG

Fishing serves five connected purposes:

1. **collection** — discover and complete the Fish Codex;
2. **exploration** — regions/water types contain different catches;
3. **economy** — sell useful surplus catches for modest Gold;
4. **cooking** — fish can be grilled or used in regional recipes;
5. **housing/trophies** — exceptional catches can become cosmetic home displays.

It does **not** become:

- mandatory combat-Lv progression;
- the best Gold farm in the game;
- a daily quest treadmill;
- a separate fishing currency shop;
- equipment loot spam;
- a bait-inventory-management simulator.

---

# 3. Launch collection scale

The codex should feel substantial without demanding hundreds of nearly identical fish.

Launch target:

```text
roughly 4–6 catchable fish identities per major region/water package
roughly 36–48 unique fish identities across the launch world after overlap
```

Rules:

- neighboring regions may share sensible common fish;
- each major aquatic identity should normally contribute at least one region-specific catch;
- not every region needs exactly the same count;
- each major region may have approximately one rare/signature/trophy chase fish where the external model pool supports a genuinely distinct identity;
- exact species names are not locked until the external model/icon candidate exists.

R01 uses exactly **four mechanical fish identities** before final visual naming: two Common, one Uncommon and one Rare. Their size ranges, values, spot pools and 5+1+1 spot counts are locked in `R01_CONTENT_BIBLE.md`. Do not invent the four final player-facing species names before the actual external model/icon set is visually reviewed; those names are ASSET_BINDING and must be closed before gameplay source for the fish slots is authored.

---

# 4. Fish rarity and catch identity

Use four readable collection bands:

- Common
- Uncommon
- Rare
- Signature

`Signature` is a collection/hunt identity, not a new equipment-grade system.

Rarity affects:

- how frequently the species appears in valid spots;
- hook/tension difficulty;
- ordinary sale value band;
- codex presentation;
- trophy/display desirability.

Rarity does **not** automatically mean:

- better combat food;
- a mandatory crafting material;
- exclusive weather waiting;
- exponentially higher Gold/hour.

---

# 5. Fish size and personal records

Every catch receives a server-authoritative size roll inside that species' authored range.

Each species data entry defines at least:

```text
min_size
normal_size_low
normal_size_high
max_size
base_sell_value
```

The codex records:

- first catch date/region state where useful;
- total caught;
- current personal best size;
- whether a trophy-size individual has ever been caught.

## 5.1 Size value modifier

Use a bounded multiplier so record hunting is rewarding but cannot destroy the economy.

Baseline shape:

```text
very small legal catch: ~0.85x base value
ordinary catch: ~1.00x
large catch: up to ~1.25x
Trophy catch: 1.50x value baseline
```

Exact continuous interpolation is implementation data, but it must remain bounded inside this shape.

## 5.2 Trophy threshold

A species' upper roughly 5% authored size band is its baseline **Trophy** band.

Trophy catch behavior:

- stronger catch presentation;
- codex marks the species trophy record;
- catch can be sold normally at the bounded bonus;
- or retained and converted/placed as an eligible housing display if an accepted display asset exists;
- no combat stat bonus for owning/displaying it.

A new larger catch replaces the recorded personal-best number but never deletes the old physical item if the player kept it.

---

# 6. Fish Codex discovery rules

## Unknown species

Before first catch:

- hidden silhouette/question state;
- region filter may show an undiscovered slot only after the player has visited/discovered the relevant water package;
- exact sell price, size range and special condition remain hidden.

## First catch

First successful catch reveals:

- name;
- accepted model/icon;
- rarity;
- caught location/region;
- basic description;
- base sale-value band;
- basic cooking use;
- that catch's size.

## Learned condition detail

Conditions become more explicit through actual play rather than a wiki dump.

Examples:

- catching the fish at night can reveal `Night` as a preferred condition;
- catching it in rain can reveal the weather association;
- an NPC/book/discovery can reveal a broad habitat hint before the first catch.

Do not require five catches just to display information the player has already directly proven.

---

# 7. Codex completion rewards

Collection rewards should feed identity/housing, not mandatory combat power.

Baseline milestone direction:

| Launch codex completion | Reward direction |
|---:|---|
| 25% | small fishing-themed home decor / plaque set |
| 50% | rod cosmetic or better fishing-display furniture appearance |
| 75% | larger aquarium/trophy-display or regional angler decor set if a suitable external asset is accepted |
| 100% | prestige fishing title + distinctive home trophy/display cosmetic |

Rules:

- no permanent +damage/+HP reward for fish completion;
- no unique mandatory combat item behind 100%;
- rewards use accepted external UI/model/furniture assets;
- if the final external asset family cannot support an aquarium cleanly, use wall/plaque/display-table trophies instead of forcing a poor aquarium implementation.

Regional mini-completion may award a small cosmetic/display without creating twelve more currencies.

---

# 8. Cooking and fish utility

The player can actually eat/use catches rather than collecting meaningless icons.

## 8.1 Field grilling

Any ordinary edible fish tagged `grillable` can be cooked at a legal camp or cooking service into a simple **Grilled Catch** food result.

Baseline:

```text
1 grillable fish
→ 1 Grilled Catch
```

`Grilled Catch`:

- ordinary out-of-combat food;
- restores approximately 15% MaxHP when eaten, consistent with the existing prepared-food recovery language;
- does **not** apply an additional long-duration Nourishment buff at baseline;
- exists as a convenient use for surplus ordinary fish without making every species require its own recipe.

This prevents the codex from creating 40 separate nearly identical cooked-fish inventory items.

## 8.2 Regional recipes

Selected fish may also be ingredients in authored regional meals.

A regional fish recipe may provide the normal one-at-a-time 20-minute Nourishment effect already defined in `RECOVERY_PRODUCTION_APPEARANCE.md`.

Rules:

- only fish whose actual visual/ecological identity fits the meal become recipe-specific ingredients;
- no recipe is added solely because every fish "needs a recipe";
- one species may be economically better sold while another is better cooked, creating small choices without spreadsheets.

## 8.3 Raw selling

Every ordinary catch has a normal Gold sale value.

- fish can be sold directly to ordinary relevant merchants/inn/fishmonger service;
- no separate fishing token;
- size modifies value within the bounded rule above;
- regional/signature fish may have a higher base value, but Fishing must remain a supplemental economy source rather than the dominant Gold/hour route.

Economy target:

```text
ordinary casual fishing route:
roughly comparable to other optional gathering activities of the same region,
not better than completing meaningful quests/dungeons/events over time.
```

Actual Gold values wait until the first real R01 fish roster/model set is accepted.

---

# 9. Catch inventory / anti-clutter rule

Caught fish are real inventory items because they can be cooked, sold or displayed.

To avoid a dedicated inventory subsystem for one side activity:

- fish use ordinary food/trade-item stacking rules;
- same species stacks together when no unique retained metadata is required;
- an ordinary catch does not need to store a unique per-item size after its value is realized unless it is the personal record/trophy class or the player explicitly preserves it;
- trophy/record-capable retained catches are individual items (`stack 1`) because their size/display identity matters;
- generic cooked `Grilled Catch` collapses species bloat after cooking.

Do not add a separate Fishing Bag currency/inventory unless playtesting demonstrates a real inventory problem after the actual 36–48 species roster exists.

---

# 10. Fishing presentation acceptance

Fishing is not asset-ready until a representative end-to-end catch shows all of the following in actual Minecraft:

1. accepted external rod visible in first and third person;
2. external cast/reel/tug/catch animation retargeted cleanly;
3. line/bobber or equivalent world presentation without obvious vanilla-placeholder feel;
4. a real accepted external fish model/identity involved in the catch presentation;
5. compact hook/tension feedback matching server state;
6. fish emerges/reels in without teleport-looking presentation;
7. result toast/codex update uses Lucifer-family UI;
8. first catch visibly unlocks the codex entry;
9. selling/cooking the same species works;
10. trophy-size catch can reach the housing-display path when that display is implemented.

`BUILD VERIFIED` is not visual acceptance.

---

# 11. Housing ownership revision — one residence at a time

This section **replaces** the older baseline statement that a player may own multiple properties later.

Launch rule:

```text
maximum owned residential properties per player: 1
```

The world contains multiple authored **vacant houses** at several size/price bands.
The progression is:

```text
save Gold
→ buy an available empty house
→ furnish/use it
→ later choose a larger house
→ trade/sell the old house
→ move furnishings safely
→ own the new house
```

Reasons:

- matches the desired "move up in the world" feeling;
- keeps physical settlement housing readable;
- prevents one player from buying every visible house in small multiplayer;
- avoids multiplying storage/property complexity;
- gives larger houses a real aspirational purpose.

---

# 12. Property tiers and price anchors

Keep the existing economy bands but turn them into clear physical property tiers.

| Property tier | Baseline purchase target | Role |
|---|---:|---|
| Small Cottage | **2,400 Gold** | first residence, early savings goal |
| Town House | **9,000 Gold** baseline | larger rooms/display/storage, midgame move |
| Large Residence | **25,000 Gold** baseline | major furnishing/trophy space |
| Prestige Estate | **65,000+ Gold** | optional late-game luxury/collection sink |

Exact individual shells may vary around their tier when location/layout justifies it, but do not create wildly different prices for visually equivalent houses.

The R01 starting-settlement roster is now exact in `R01_CONTENT_BIBLE.md`: **4 Small Cottage vacancies** and **1 Town House** visible/purchasable from the beginning. Later settlements provide their own property counts.

Later settlements supply additional tier/architecture choices.

The first home remains available from the beginning with **no story permission gate**; Gold is the practical gate.

---

# 13. Buying / upgrading UX

Buying uses a physical property sign/steward/realtor-style service attached to the actual vacant house or settlement service.

The purchase screen shows:

- property name/location;
- exterior preview/real-world view;
- tier;
- purchase price;
- interior usable footprint/room count summary;
- Home Storage capacity;
- whether optional starter furnishing package is included or separate;
- current residence trade-in credit when applicable.

No auction/bidding system is needed for ordinary NPC-owned houses.

## 13.1 First purchase

If the player owns no home:

```text
pay full property price
→ ownership transaction commits
→ house becomes the player's residence
```

The baseline Small Cottage remains 2,400 Gold.
The R01 optional starter furnishing package is exactly **750 Gold** and uses the item/value breakdown in `R01_CONTENT_BIBLE.md`.

## 13.2 Moving to a larger/different home

Do **not** force the player to sell first and risk ending up homeless after a mistaken transaction.

Use one atomic **Move / Trade Residence** operation:

```text
new purchase price
- old-home sale credit
= required Gold difference
```

The server validates the whole transaction first, then commits ownership/move together.

If the player cannot afford the difference, nothing changes.

---

# 14. Selling / trade-in

Baseline resale credit:

```text
80% of that property's normal purchase price
```

Rules:

- resale never includes temporary merchant/reputation discounts as a profit exploit;
- furnishings are not sold with the shell unless a future explicit furnished-property system says so;
- ordinary resale cannot generate profit;
- quest/event-granted special property would need its own authored rule rather than abusing the normal percentage;
- after sale, the property resets to an authored empty/default state and can become vacant again.

Why 80%:

- upgrading feels affordable rather than punitive;
- buying/selling repeatedly still loses value, so housing cannot become free money/arbitrage;
- the main economic sink remains the size upgrade and furnishings, not a 50% moving tax.

---

# 15. Safe moving of furniture and stored items

A housing upgrade must never destroy the player's collection.

Before a move commits, the server builds a **Moving Inventory** transaction containing:

- all player-owned movable furniture placed in the old residence;
- all ordinary display/trophy items owned by the player;
- current Home Storage contents.

On successful purchase:

- old placed furnishings are removed from the old property;
- their items become available in the new residence's Home Storage / temporary `Moving` section;
- trophies retain their unlock/item identity;
- old property resets only after this migration is safely recorded.

If migration cannot fit the new storage representation or fails validation, the transaction aborts before Gold/ownership changes.

Selling a large house and moving into a smaller one is allowed; overflow furniture remains in the temporary Moving section until placed/withdrawn and cannot be used as infinite permanent storage.

---

# 16. Home Storage by residence size

Housing storage should improve with the home without making the house mandatory for combat progression.

Baseline target:

| Residence | Home Storage |
|---|---:|
| Small Cottage | 54 ordinary slots |
| Town House | 72 ordinary slots |
| Large Residence | 108 ordinary slots |
| Prestige Estate | 144 ordinary slots |

Rules:

- Home Storage is one logical pool for the currently owned residence;
- placing ten cabinets does not multiply capacity ten times;
- accepted storage furniture acts as access points and visual dressing;
- Material Vault remains a separate settlement system with its existing 9,999-per-material role;
- selling/moving never deletes Home Storage contents;
- house size is useful convenience/collection space, not a combat-stat requirement.

---

# 17. Furniture / interior external-first stack

## 17.1 Adorn — primary functional furniture candidate

Current review 2026-09-15:

- Minecraft 26.2;
- Fabric supported;
- MIT;
- source available;
- includes chairs, sofas, drawers, kitchen counters/cupboards/sinks and other home decoration/storage.

Project status:

- **strong `DEPENDENCY / CODE_CANDIDATE`**;
- first candidate for proven seating, storage-furniture and placement behavior;
- direct visible blocks are accepted only if actual screenshot review shows their style works with the selected settlement/interior art direction;
- if visual style clashes, keep/reuse permitted behavior patterns and bind higher-quality accepted external fantasy furniture models instead.

Source:
`https://modrinth.com/mod/adorn`

## 17.2 Skniro's Furniture — broad secondary candidate

Current review 2026-09-15:

- Minecraft 26.2;
- Fabric/Forge/NeoForge;
- MIT;
- broad furniture set.

Project status:

- **secondary `DEPENDENCY / CODE_CANDIDATE`** for furnishing breadth;
- compare actual visuals/performance with Adorn before adding another dependency;
- do not ship both large furniture mods merely because they exist.

Source:
`https://modrinth.com/mod/skniros-furniture`

## 17.3 Property protection / permissions

### Safe Zone Claims

Current review 2026-09-15:

- Minecraft 26.2 Fabric;
- Java 25;
- MIT;
- server-side claim/protection system with trust/admin behavior.

Project status:

- **`CODE_CANDIDATE / REFERENCE`** for robust block/interact/explosion/property permission enforcement;
- project housing uses fixed authored `property_id` volumes, not player-drawn generic land claims;
- do not expose generic claim-wand/chat-command UX as the housing system if selective reuse can give us the protection logic cleanly.

Source:
`https://modrinth.com/plugin/safe-zone-claims`

Generic Open Parties and Claims is not the first choice because the RPG already owns party/progression semantics and does not need a second visible generic claim-management game.

---

# 18. Furniture visual language

Functional code does not get to dictate ugly/incorrect art.

Preferred visual order:

1. accepted furniture models/blocks from the selected 26.2 furniture dependency **if** they harmonize with the settlement;
2. accepted Quaternius Fantasy Props / house-interior furniture under the project's package-specific license boundary;
3. other external redistributable fantasy furniture families that visually match;
4. only then a project adaptation based on those external designs.

The house shell and furnishing must look like one game.

Do not mix:

- modern TV/microwave furniture into the grounded fantasy starting settlement;
- five unrelated furniture packs in one room;
- vanilla oak-chair approximations beside detailed low-poly fantasy props without a deliberate style bridge.

---

# 19. Interior editing rules

The house is delivered as an **empty authored shell**.

Player may:

- place/remove/move owned furniture and decor;
- rotate compatible furnishings;
- place rugs, lights, shelving, tables, chairs, beds, cabinets, display/trophy props and accepted decorative objects;
- select approved visual/material variants offered by the admitted external furniture family;
- use functional furniture such as bed/rest, storage access and basic kitchen/cooking where canon permits.

Player may not at baseline:

- destroy exterior walls/roof;
- excavate through neighboring houses;
- remove settlement roads/structural service pieces;
- convert the authored town into an unrestricted survival claim.

This is **interior furnishing**, not a second free-build base game.

If later playtesting shows surface customization is valuable, floor/wall finish presets can be added through accepted external texture/material variants without allowing structural demolition.

---

# 20. Furniture acquisition and economy

Furniture uses the existing Gold/material economy.

Sources may include:

- furnishing merchant/carpenter stock;
- the 750-Gold starter package;
- regional furniture variants;
- quest/collection trophies;
- crafted decor when the recipe creates a meaningful use for an existing real material.

No `Housing Token`, `Furniture Coin` or separate decoration stamina is introduced.

Avoid dozens of tiny assembly recipes.
A chair does not need five manual intermediate crafting steps merely because Minecraft has crafting.

---

# 21. Fishing × housing connection

Fishing and housing should reinforce each other without making either mandatory.

Supported connections:

- trophy-size fish wall/plaque/display;
- codex milestone decorations;
- regional fishing keepsakes;
- optional fish-themed kitchen/angler furniture;
- personal-best size visible on the trophy tooltip/display plaque.

If an aquarium system is adopted:

- use an external proven aquarium/fish-display solution/model;
- keep entity count bounded;
- aquarium fish are display state, not full pathfinding mobs simulated every tick;
- removing the display safely returns/retains the owned catch state.

Do not implement an aquarium just because it sounds nice if the only available result is visually weak or performance-heavy.

---

# 22. Multiplayer property rules

Because houses are physical authored vacancies:

- a physical property has only one primary owner at a time;
- each player can own at most one residence;
- an owned house is unavailable for another player's purchase until sold/released;
- starting settlement provides enough Small Cottage vacancies for the intended small friend-group baseline;
- later settlements expand available choices.

Permission roles:

```text
Owner
Trusted Decorator (optional)
Guest
```

Baseline:

- Owner: purchase/sell/move/furnish/storage authority;
- Trusted Decorator: may place/move approved decor if owner grants it, but cannot sell house or access private storage unless separately granted;
- Guest: enter/sit/use non-private furniture only.

Personal Home Storage remains private by default even when friends can enter the house.

Ownership, furniture mutations and Gold transactions are server-authoritative.

---

# 23. Failure / transaction safety

Housing purchase/sale/move must be atomic.

Never allow:

- Gold removed but ownership not granted;
- old house reset before furniture migration succeeded;
- disconnect halfway through creating duplicate furniture;
- two players purchasing the same vacancy simultaneously;
- resale exploit using discounts/reputation;
- guest interaction modifying owner state without permission.

Canonical transaction order:

```text
validate availability + owner + Gold + migration state
→ reserve property transaction server-side
→ create durable migration snapshot
→ apply Gold delta
→ transfer ownership
→ migrate furniture/storage
→ finalize old property reset
→ mark transaction complete/idempotent
```

Recovery after server interruption must replay or roll back from durable transaction state rather than guessing from client UI.

---

# 24. Data contracts

## Fish species

```text
id
player_facing_name
model_source_id
icon_source_id
rarity
region_tags[]
water_tags[]
condition_weights
min_size
normal_size_low
normal_size_high
max_size
base_sell_value
grillable
recipe_tags[]
trophy_display_source_id
codex_description
```

## Player fish record

```text
fish_id
discovered
first_catch_context
total_caught
best_size
trophy_caught
revealed_condition_flags[]
```

## Property

```text
property_id
settlement_id
tier
purchase_price
sale_credit_rate
protected_volume
interior_volume
storage_capacity
shell_source_id
furnishing_style_id
owner_player_id | null
```

## Furniture placement

```text
property_id
furniture_instance_id
owner_player_id
furniture_type_id
position
rotation
variant
stored_item_state_if_applicable
```

## Housing transaction

```text
transaction_id
player_id
old_property_id | null
new_property_id | null
price
sale_credit
gold_delta
migration_snapshot_id
state: PREPARED | COMMITTED | ROLLED_BACK
```

---

# 25. Acceptance gates before implementation is called finished

## Fishing

- one R01 common fish and one R01 rare fish use accepted external model/icon sources;
- accepted external rod + KayKit fishing motion is visible in Minecraft;
- selected permissive fishing code path or clean local dependency integration is technically proven;
- hook/tension server authority works;
- first-catch codex reveal works;
- personal best/trophy size works;
- raw sell works;
- camp cooking to Grilled Catch works;
- one trophy/display route works;
- UI looks like the established Lucifer family, not a custom debug screen.

## Housing

- at least two different-size authored property shells exist in a test settlement;
- one player can buy the small property;
- another player cannot buy the same occupied property;
- furnishing placement uses accepted external furniture presentation;
- storage is persistent/server-authoritative;
- the owner can trade into a larger vacant house in one atomic operation;
- 80% sale credit is applied once;
- every placed furnishing survives the move or enters the Moving section;
- old property returns to empty/vacant state only after successful migration;
- permission/grief protection survives relog/restart;
- no duplicate furniture/Gold on disconnect/retry.

Visual review and real multiplayer tests are still required before either subsystem is called complete.

---

# 26. Current verification state

- DESIGN REVIEWED: YES
- USER DIRECTION INTEGRATED: YES
- CURRENT 26.2 EXTERNAL CANDIDATES REVIEWED: YES
- LICENSE METADATA REVIEWED: YES for the candidate classifications recorded above
- EXACT FISH ROSTER/MODEL BINDINGS: NOT YET — external model intake required first
- FISHING CODE CANDIDATE TECHNICALLY TESTED: NO
- FURNITURE DEPENDENCY TECHNICALLY TESTED: NO
- PROPERTY PROTECTION CODE TECHNICALLY TESTED: NO
- BUILD VERIFIED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

This is design/provenance work. No build/CI is justified by this document-only pass.