
# Open-World RPG — R01 UI / UX Production Specification

> Status: **DESIGN CANON — R01 player-facing screen hierarchy, state flow, interaction ownership and failure/reconnect UX locked before UI implementation**
> Master gameplay canon: GAME_DESIGN.md
> Project contract: PROJECT.md
> Shared visual grammar: UI_DIRECTION.md
> Accessibility/input: ACCESSIBILITY_DIFFICULTY_INPUT_AUDIO.md
> R01 content: R01_VERTICAL_SLICE.md, R01_CONTENT_BIBLE.md
> Exact player-facing wording: R01_PLAYER_TEXT_SPEC.md
> Quest/state: QUEST_WORLD_STATE.md
> Class: CLASS_COMBAT_KITS.md, CLASS_PROGRESSION.md
> Equipment/economy: LOOT_ECONOMY.md, EQUIPMENT_BALANCE.md
> Recovery/production: RECOVERY_PRODUCTION_APPEARANCE.md
> Gathering/fishing/housing: GATHERING_FISHING_CAMP_HOUSING.md, FISHING_COLLECTION_HOUSING_MARKET.md
> Rule: gameplay/state canon wins. This file closes how R01 exposes those rules to the player. Exact source-image filenames, final pixel crops, font file selection and real-client scale correction remain ASSET_BINDING / visual-validation work and are not permission to redesign the interaction flow during coding.

The goal is not to invent a novel UI system. The project already selected the Foozle **Lucifer RPG UI + Lucifer Equipment** family, with Kenney support assets only where the primary family needs scalable primitives. This file makes that family operational for the first complete region.

An implementer must not decide during coding:

- which R01 screens exist;
- where the primary information hierarchy lives;
- whether a screen is modal or world-overlay;
- whether an action requires confirmation;
- whether a pending reward can be closed;
- what the quest board foregrounds;
- how merchant rotation and fixed stock are separated;
- whether housing purchase includes a furnishing prompt;
- whether fishing uses a full-screen minigame;
- how class selection cancellation behaves;
- how reconnect resumes pending UI state.

---

# 0. External UI lessons used structurally

External games are references for interaction clarity, not visual assets.

- **Diablo-style action-RPG inventory comparison:** show the decision-relevant delta near the item rather than reproducing an entire character sheet inside every tooltip.
- **Guild Wars 2 open-world tracking:** foreground only a small active objective set; temporary world-event information appears near the current activity and disappears when irrelevant.
- **Elden Ring-style optional danger communication:** dangerous content uses world/boss presentation and suggested context rather than a modal “you are not allowed here” gate.
- **Monster Hunter preparation flow:** preparation/recovery information is visible enough to make a decision before combat without turning combat itself into menu management.

The project does not copy another game's UI textures, proprietary iconography or menu layout. The actual visual language remains the selected CC0 project family.

---

# 1. Global R01 UI composition rules

## 1.1 Safe area

All R01 screens use a common logical safe area.

At any supported resolution/GUI scale:

- outer interactive content keeps at least one Lucifer border-unit from the screen edge;
- no required text is placed under Minecraft chat, subtitles/captions or system toasts;
- modal content never exceeds **90% of usable width** or **88% of usable height**;
- a long Korean string wraps before shrinking below the project's minimum readable UI scale;
- critical buttons do not move to a different semantic location merely because GUI scale changes.

Exact pixels are visual-validation values, not gameplay design.

## 1.2 Shared screen skeleton

Full-screen service/character screens use one of two skeletons.

### Three-zone skeleton

~~~text
[ title / context header ]
[ left navigation ] [ primary content ] [ detail / comparison ]
[ back ]                             [ primary action ]
~~~

Default width ratio:

~~~text
left 22%
primary 50%
detail 28%
~~~

A screen may collapse the right detail zone when nothing is selected. It may not fill that space with decorative filler.

### Two-zone skeleton

~~~text
[ title / context header ]
[ primary content 64% ] [ detail / action 36% ]
[ back ]                 [ primary action ]
~~~

Used for first class selection, property purchase and first-clear reward choice.

## 1.3 World-overlay skeleton

Combat-adjacent interactions use world-first overlays rather than full screens:

- fishing tension;
- furniture placement;
- camp placement;
- contextual Dodge hint;
- dynamic event tracker;
- Trail Stag unlock presentation.

The 3D world remains visible and no opaque full-screen panel is used.

## 1.4 Modal ownership

Only one blocking modal may own player input at once.

Priority:

~~~text
disconnect / fatal state
> death / respawn
> irreversible transaction confirmation
> earned reward choice
> service/item confirmation
> ordinary menu
> contextual tutorial
> toast
~~~

Lower-priority presentation is queued or suppressed; it never steals focus from a higher-priority state.

## 1.5 Close / Back

- Esc / Back closes only the topmost UI layer first.
- Closing a non-committed screen never performs its primary action.
- A committed transaction is never rolled back merely because its result animation was closed.
- Screens with server-pending state show that state immediately when reopened.
- No screen uses a fake disabled close button merely to trap the player in exposition.

---

# 2. R01 HUD

## 2.1 Persistent layout

### Lower-left — player resources

One compact Lucifer-framed cluster:

1. HP;
2. Mana;
3. Stamina;
4. current loaded Recovery Belt consumable + loaded count;
5. Recovery lockout treatment when active.

No numeric Gold, EXP or inventory count lives here permanently.

### Lower-center — combat actions

~~~text
[ Skill 1 ][ Skill 2 ][ Skill 3 ][ Skill 4 ] [ ULTIMATE ]
~~~

- four ordinary skills equal-size;
- Ultimate is visually wider or more ornamented and never mistaken for Skill 5;
- current physical binding glyph/text is shown;
- cooldown and unavailable-resource states use shape/icon/timer changes in addition to color.

### Upper-center — target / boss

Ordinary target information appears only while a valid target is engaged/selected.

Boss state uses:

- boss name;
- HP;
- poise when meaningful;
- active status icons;
- phase/readability treatment without internal phase numbers such as P2.

### Upper-right — objectives

Normal free-roam:

~~~text
1 Main
+ up to 2 manually pinned optional objectives
~~~

Each row shows:

- quest title;
- current objective only;
- distance only when an exact known objective legitimately has one;
- search-area symbol instead of exact distance for broad-search content.

No reward list is permanently shown in the HUD.

### Event tracker

When Roadside Trouble is active/relevant, one compact event tracker appears directly below the objective cluster.

It shows the three real requirements:

~~~text
Clear the road threat
Brace the wheel
Return the cargo
~~~

Each completed requirement receives a clear check state.

It does not show a meaningless generic 0–100% bar.

## 2.2 Contextual prompt position

One-line contextual prompts use the lower-middle area **above** the skill row.

The first Viper Dodge hint shows:

~~~text
[Dodge binding] Dodge
~~~

No paragraph explanation appears during the attack.

## 2.3 Toast lane

Non-blocking rewards/discoveries use one toast lane at upper-left.

Examples:

- material acquired;
- Fish Codex first discovery;
- location discovered;
- mastery rank increase;
- Class Insight discovery / Passive Point gain;
- route lead opened.

Maximum visible toasts: **3**.

Additional toasts queue and coalesce identical material pickups rather than covering the screen.

---

# 3. First root-class selection

Access: interact with **Elian Rook** before a first root class is committed.

Screen: two-zone full-screen skeleton.

## 3.1 Primary zone — five class choices

Exactly five entries:

- Warrior;
- Hunter;
- Cleric;
- Mage;
- Guardian.

Each entry shows:

- accepted external character/weapon presentation;
- root-class name;
- one-sentence gameplay identity;
- recommended primary stats as guidance, never a hard requirement.

Selecting a class **previews only**. It does not mutate server state.

## 3.2 Detail zone

For the highlighted class show, in this order:

1. root mechanic name + one compact explanation;
2. four starting active skills;
3. Ultimate;
4. natural weapon synergy;
5. defensive/mobility identity.

Do not show:

- late specialization names before relevant;
- full Rank-50 tree;
- undiscovered Hidden Techniques;
- a giant stat spreadsheet.

## 3.3 Confirmation flow

Primary button:

~~~text
Begin as <Class>
~~~

First press opens one compact confirmation:

~~~text
Begin as <Class>?
Your first class is free. You can learn another path later.
[Back] [Confirm]
~~~

On Confirm:

1. client sends selection request;
2. server validates no first class already committed;
3. server commits class + starter grant atomically;
4. screen shows a short success state;
5. Dust on the Quarry Road becomes current Main;
6. screen closes to world.

If the grant transaction fails, no partial class state is committed and the screen remains open with a normal player-facing failure message.

## 3.4 Cancel

The player may close the screen without selecting.

No class, quest progression or starter item is granted.

Reopening shows the same five choices.

---

# 4. Guild board

World source: physical Alderford guild/route board.

Screen: compact two-zone board screen, not a generic 30-row quest list.

## 4.1 First-arrival content

Exactly:

### Featured Main
Dust on the Quarry Road

### Available Contracts
- Riverbank Remedies
- Signs in the Meadow

Before first class selection:

- Dust appears as the obvious Main lead;
- selecting it shows “Choose your first class with Elian Rook to begin”;
- no fake Accept button pretends it can begin yet.

After first class commit:

- Dust status is **Current** automatically;
- it does not require a second Accept click.

Optional contracts each expose one Accept action.

## 4.2 Board card information

Each card shows:

- category;
- title;
- 1–2 line premise;
- reward summary;
- current state: Available / Current / Completed.

No internal level range is shown unless the content itself has a useful Suggested Lv.

## 4.3 Post-Earthloong

After the briefing:

- the board visually gains the western/forest and high-road leads;
- R02/R03 appear as peer regional directions;
- neither receives a Recommended/Best choice marker.

---

# 5. Quest journal

Hotkey: J.

Full-screen three-zone skeleton.

## 5.1 Tabs

Exactly:

- Main
- Regional
- Contracts
- Discoveries

World Events never become an expired-event backlog tab.

## 5.2 Left navigation

Shows entries within current category.

Each entry displays:

- title;
- compact status icon;
- pin state where legal.

No stage IDs.

## 5.3 Primary content

Order:

1. quest title/category;
2. current objective;
3. concise location/search context;
4. 2–4 line current situation summary;
5. previously completed major steps collapsed by default;
6. reward preview when known.

## 5.4 Detail/actions

Available actions only:

- Pin / Unpin;
- Show on Map when legally known;
- Abandon only where canon allows;
- Claim/Open pending reward when applicable.

Maximum pinned optional objectives remains 2.

Trying to pin a third opens a compact replacement picker showing the two current optional pins.

## 5.5 Broad-search behavior

The Crowned Trail displays:

- clue progress when known;
- broad-area map action after 2/3 clues;
- no exact Regalhart pin.

Finding Regalhart first changes the journal to encounter/discovery state without forcing remaining clues.

---

# 6. World map

Hotkey: M.

The map surface dominates the screen; Lucifer chrome only frames controls.

## 6.1 Marker precision

Exact-point marker:
- discovered Alderford;
- activated shrine/Waystone;
- personally inspected quarry entrance;
- known service/property.

Broad-area marker:
- Regalhart search territory after clue threshold.

No marker:
- still-undiscovered hunt/POI.

## 6.2 R01 map layers

Toggle list:

- Settlements & Services
- Shrines / Travel
- Quests
- Dungeons
- Discoveries / Hunts
- Housing
- Personal Camp

Resource nodes are **not** globally revealed by default simply because one node type has been gathered.

## 6.3 Fast travel

Selecting an activated legal shrine/Waystone exposes Travel **only while the player is within 6 blocks of a personally activated origin travel anchor**.

If the player opens the map from ordinary field space, known travel nodes remain visible but Travel is disabled with `Reach a shrine or Waystone to fast travel.`

Travel cost shown in R01: **Free**.

On selection:
1. confirm destination once;
2. begin the 1.0 s travel fade/channel;
3. hostile damage/combat cancels cleanly;
4. server commits arrival only after collision-safe destination validation.

Ordinary camp does not expose Travel.

Quarry Waystone is absent from selectable travel list until personal activation.

## 6.4 Suggested Lv

Shown as secondary metadata on discovered dangerous content, never as a padlock.

Quarry entrance after inspection:

~~~text
Suggested Lv 8
~~~

The player can still enter.

---

# 7. RPG inventory / equipment

Hotkey: E.

Existing UI_DIRECTION.md equipment hierarchy remains authoritative.

R01 adds exact first-region behavior.

## 7.1 Tabs

- Equipment & Backpack
- Material Pouch
- Key Items

Wardrobe is opened from the character appearance control inside Equipment or a valid wardrobe furnishing/service; it is not a fourth material-storage tab.

## 7.2 Equipment & Backpack

Left/center:
- central character preview;
- 12 equipment slots around/adjacent.

Right:
- 4x9 general backpack at start;
- hotbar remains separately represented as the existing 9 gameplay slots.

Selecting an item opens comparison in the detail lane.

## 7.3 Comparison

Show only relevant differences:

- Item Lv / requirement;
- primary WeaponPower or Defense/MR;
- cadence/reach/guard property when relevant;
- affix delta;
- unique mechanic;
- source hint if known.

Positive/negative color is accompanied by arrows or +/- treatment.

## 7.4 Protected discard

Starter-bound, Mythic/signature, favorite or otherwise protected items require explicit discard confirmation.

No fast world Q drop remains in the default RPG control profile.

---

# 8. Material Pouch / Alderford Vault

Material Pouch is a tab in Inventory.

Alderford Vault is a world service with two tabs:

- Personal Storage
- Material Vault

## 8.1 Personal Storage

Exact R01 capacity: **36 ordinary slots per player**.

Rules:
- equipment/consumables/trade goods may be stored;
- Material Pouch materials belong in Material Vault unless manually withdrawn as ordinary stacks for a valid reason;
- Key Items never consume these slots;
- no Gold fee;
- important reward overflow may route here automatically under GAME_DESIGN.md;
- automatic overflow never evicts or rearranges locked/favorited stored items.

## 8.2 Material Vault

Category list with:

- material icon/name;
- carried amount;
- vault amount;
- 9,999 vault cap.

Actions:

- Deposit Selected;
- Withdraw Selected;
- **Deposit All Materials**.

Deposit All never moves Key Items or non-material gear.

No Gold fee.

## 8.3 Pending Reward Claims

If both backpack and Alderford Personal Storage lack legal room for an important server-owned item reward, the Vault exposes a compact **Pending Rewards** section.

Each row shows:
- reward source;
- item preview;
- grade / relevant key data;
- Earned status;
- Claim button.

Rules:
- the pending item cannot be equipped/sold/moved/traded from this view;
- Claim revalidates backpack first, then Personal Storage;
- if neither has room, Claim remains unavailable with `Make room in your backpack or Alderford Vault`;
- clearing inventory space does not auto-claim while another blocking modal owns input;
- reconnect preserves the transaction exactly once.

Journal may also expose `Claim pending reward`, opening this same claim action rather than duplicating another storage surface.

---

# 9. Recovery Belt setup

Opened from Inventory/Equipment through the Recovery Belt widget.

Layout:

~~~text
[ Slot 1 ][ Slot 2 ][ Slot 3 ][ Slot 4 ]
eligible reserve items below
~~~

Rules:

- a slot stores a consumable type assignment and current loaded dose;
- eligible R01 types are Healing Potion / Focus Draught / Cleansing Tonic;
- clicking an eligible reserve fills/replaces selected slot assignment;
- Clear Slot removes assignment without deleting reserve items;
- changing setup while in combat is disabled with an actionable reason;
- legal rest/reload uses saved assignment automatically.

The belt screen never becomes a second general inventory.

---

# 10. Nessa market

World interaction: Nessa Bell.

Three tabs:

1. Equipment
2. Household
3. Sell

## 10.1 Equipment

Exactly five current rotating slots.

Header shows:

~~~text
Stock refresh: M:SS
~~~

The timer is informative only; no paid refresh button exists.

Each row/card:
- model/icon;
- name/grade/Item Lv;
- key stats/affixes;
- price;
- compare state.

Primary action: Buy.

Buy confirmation is **not** required for ordinary Standard/Refined gear when affordable.

Confirmation **is** required for:
- Superior gear;
- any purchase that would spend more than 50% of current Gold.

## 10.2 Household

Shows the exact fixed R01 Household catalogue from R01_CONTENT_BIBLE.md.

No refresh timer.

Without residence:
- list can be browsed;
- Buy disabled with “Own a residence to buy furnishings”.

## 10.3 Sell

Two subviews:

- Items
- Materials

### Items

Shows sellable carried equipment/consumables.

Actions:
- Sell Selected;
- Sell All Unlocked Standard only after the global QoL rule permits it.

Favorite/locked/starter-bound/key/signature protection is respected.

A sale confirmation is required for:
- Superior+ gear;
- currently equipped gear;
- named/signature gear.

### Materials

Reads directly from Material Pouch; no manual withdraw chore is required.

Each row shows:
- material;
- held amount;
- exact sell value per unit;
- quantity selector;
- total Gold.

Actions:
- Sell Selected Amount;
- Sell Ordinary Surplus.

Sell Ordinary Surplus excludes:
- Regalhart Antler;
- Earthloong Scale;
- any quest-reserved quantity;
- favorite/protected material entries.

Selling Regalhart Antler or Earthloong Scale always opens a confirmation showing:
- quantity being sold;
- remaining quantity;
- known signature-craft requirement of 4.

Direct R01 material sell values come from R01_CONTENT_BIBLE.md.

---

# 11. Holt Forge

World interaction: Daren Holt.

Three-zone screen.

## 11.1 Categories

- Weapons
- Off-hands
- Superior
- Signature

Categories with no known recipe remain hidden rather than appearing as fake Coming Soon content.

## 11.2 Recipe list

Shows known R01 recipes only.

Each row:
- output model/icon;
- grade/Item Lv;
- combined owned/required materials from **Material Pouch + Material Vault**;
- Gold.

Unavailable learned recipes show exact missing requirement.

## 11.3 Detail

Show:
- actual output preview;
- fixed affix;
- random valid affix count;
- compare vs currently equipped compatible item;
- cost summary.

Primary action: Forge.

One confirmation only.

Signature panel shows progress such as:

~~~text
Earthloong Scale 2 / 4
~~~

It never implies an immediate repeat clear is mandatory.

---

# 12. Greenwater Remedies — Alchemy

World interaction: Lysa Fen / alchemy station.

Tabs:
- Craft
- Buy

## 12.1 Craft

R01 exact recipes:
- Healing Potion
- Focus Draught
- Cleansing Tonic

Detail shows:
- output;
- actual effect;
- belt eligibility;
- use time/shared Recovery lockout;
- combined owned/required ingredients from **Material Pouch + Material Vault**;
- service fee.

Buttons:
- Craft 1
- Craft 5
- Craft Max

Craft Max opens one confirmation summarizing exact output count/materials/Gold.

## 12.2 Buy

Fixed three-item **unlimited** stock, exact prices.

No stock counter, sold-out state or rotating timer.

Settlement service cost detail may expand an owned count into `Pouch N + Vault M`; the player is never required to withdraw materials before crafting.

---

# 13. The Copper Kettle — Cooking

World interaction: Brin Hale / kitchen.

Tabs:
- Cook
- Prepared Meals

## 13.1 Cook

R01:
- Herbed Louxia Roast
- Trail Skewers
- Glow Broth
- Grilled Catch when a grillable fish is owned/known.

Detail:
- combined settlement-owned ingredients from **Material Pouch + Material Vault**;
- immediate out-of-combat heal;
- Nourishment effect/duration where applicable;
- currently active meal and replacement warning.

If another meal is active, crafting is allowed; **eating** a replacement carries the replacement warning, not crafting.

Buttons:
- Cook 1
- Cook 5
- Cook Max with confirmation summary.

## 13.2 Prepared Meals

Exact Brin fixed prices with **unlimited** R01 prepared-meal availability.

No stock counter, sold-out state or random daily menu.

---

# 14. Trail Stag unlock presentation

The physical calming/mount/return sequence carries the reward.

Upon successful stable registration:

1. world control remains visible;
2. short accepted Stag/tack registration animation plays;
3. a compact unlock panel appears.

Panel:

~~~text
Trail Stag
Mount unlocked

Cruise: 6.4 b/s
Combat attack: None

[Mount summon binding] Summon / Dismiss
~~~

The panel closes automatically after **6 s** or immediately on input.

It is not a full-screen reward chest.

Gold/EXP/Class XP use the normal toast lane.

---

# 15. Property inspection / purchase

World interaction: physical property sign/door steward trigger at a vacant house.

Screen: two-zone.

## 15.1 Property zone

Show:
- real exterior preview or retained world view;
- property name;
- tier;
- room/usable-footprint summary;
- Home Storage capacity;
- exact price.

Do not show a fake marketing render unrelated to the physical shell.

## 15.2 Purchase zone

Show:
- current Gold;
- property price;
- optional **Starter Furnishing Package +750 Gold** toggle;
- total.

Primary action: Purchase Residence.

Always requires one final confirmation.

## 15.3 Successful first purchase

Server commits property transaction first.

Then:
- success presentation;
- furnishing package items deposited if selected;
- Home Storage becomes available;
- property sign changes ownership state.

If another player bought it first:
- no Gold removed;
- screen returns unavailable with “This residence is no longer vacant.”

## 15.4 Move / Trade Residence

When a residence is already owned, the same screen changes action to Move / Trade Residence.

Show exact:

~~~text
New price
Old residence credit (80%)
Required Gold difference
Furniture/storage migration: Included
~~~

One confirmation.

No sell-first workflow.

---

# 16. Furnishing mode

Entry: while inside owned residence, choose Furnish from Home Storage/furnishing access.

World-overlay only.

## 16.1 Layout

Bottom strip:
- owned placeable furniture categories/items.

Near reticle/object:
- item name;
- current rotation;
- valid/invalid reason.

Control legend:
- Place;
- Rotate;
- Move;
- Pick Up;
- Cancel.

Current bindings come from action IDs; do not hard-code letters.

## 16.2 Placement

- ghost uses the **real accepted furnishing model**;
- valid/invalid presentation uses shape/border + concise reason, not color alone;
- grid/surface snapping default;
- 90-degree rotation baseline.

No Gold/material is consumed on placement; the player already owns the furnishing.

## 16.3 Move / pickup

Moving is atomic:
- original remains authoritative until new placement validates;
- cancellation restores original state;
- disconnect cannot duplicate it.

---

# 17. Camp placement / first deployment

The permanent Field Camp Kit appears under project utilities after unlock.

Using it enters world-overlay placement.

Show:
- real accepted camp ghost;
- footprint;
- compact validity reason.

Invalid reasons:
- In combat
- Protected settlement area
- Dungeon / boss area
- Too close to shrine
- Too close to another camp
- No valid ground

Successful first deployment gives a one-time compact explanation:

~~~text
Field Camp
Rest • Reload Recovery Belt • Cook • Manage gear
Redeploying moves your camp.
~~~

No tutorial quest is created.

---

# 18. Fishing interaction UI

Fishing is intentionally not a full-screen minigame.

## 18.1 Cast

World reticle/aim retained.

While aiming:
- one subtle landing-valid indicator;
- no large power meter.

Invalid landing returns line and consumes no spot charge.

## 18.2 Bite

On bite:
- world/rod/fish presentation leads;
- compact hook prompt near center;
- hook window uses canonical difficulty timing.

No giant QTE banner.

## 18.3 Tension phase

Only uncommon/rare/trophy-class catches enter tension.

One compact horizontal tension element above the lower-center combat row.

Show:
- current tension indicator;
- valid-zone bounds;
- remaining catch progress.

Do not show hidden RNG percentages.

## 18.4 Catch result

Common:
- compact toast with model/icon, name, size, rarity.

First discovery / personal best / Trophy:
- expanded catch card for up to **4 s**;
- New label on first discovery;
- old → new size for record;
- Trophy text plus visual treatment, not color alone.

The card does not prevent movement.

---

# 19. Fish Codex

Reached from the parent collection/inventory UI after first fishing discovery.

Three-zone screen.

## 19.1 Left

Region/water-package filters.

## 19.2 Primary list

R01 discovered/known slots.

Unknown behavior:
- hidden silhouette/question;
- slot exists only after relevant water package discovery.

## 19.3 Detail

Discovered fish:
- real accepted model/icon;
- player-facing name;
- rarity;
- habitat/learned conditions;
- base sale value;
- size-range information actually learned;
- total caught;
- personal best;
- Trophy record;
- cooking use.

Unknown conditions are not spoiled.

---

# 20. Boss HUD — Regalhart / Earthloong

Boss frame appears after encounter engagement.

Required:
- boss name;
- HP;
- poise;
- status icons;
- weak-point feedback only when legitimately exposed/learned.

No exact numerical HP is required by default.

Regalhart Sovereign:
- restrained frame emphasis;
- no PHASE 2 text.

Earthloong lightning phase:
- world tell/VFX remains primary;
- HUD does not replace spatial reading with warning paragraphs.

---

# 21. Earthloong first-clear reward

After server records eligible first clear, the player owns a personal pending choice.

Screen: two-zone full-screen reward screen.

## 21.1 Three choices

Exactly:
- Ironroot Longsword
- Riverthorn Bow
- Lumenwood Staff

All three visible simultaneously.

Each:
- real accepted model/icon preview;
- Superior / Item Lv8;
- fixed affixes;
- compare-to-equipped summary.

No choice preselected.

## 21.2 Detail

Selecting a card shows:
- full relevant stats;
- affix values;
- compatible family/use;
- no fake class restriction.

Primary action:
Choose <Item>.

## 21.3 Confirmation

~~~text
Choose <Item> as your first quarry-clear reward?
This choice grants one item.
[Back] [Confirm]
~~~

Server grants exactly one item and clears pending state atomically.

## 21.4 Closing without choosing

The player **may close** the screen.

Closing:
- does not auto-select;
- does not lose the reward;
- leaves “Quarry Reward — Choice Pending” under Main/journal claim action;
- one non-spam reminder appears on first return to Alderford or next journal open.

Reconnect:
- pending choice remains;
- it may reopen once after world load when no higher-priority modal is active;
- closing again is allowed.

The boss/world controller never waits for another player's UI choice.

---

# 22. Post-quarry briefing / route handoff UI

Dialogue remains world/NPC presentation, not a visual novel covering the world.

## 22.1 Dialogue frame

Compact lower/side frame:
- speaker name;
- current line;
- advance;
- Skip Scene after first line.

Skipping:
- commits identical story information/state;
- journal summary is populated.

## 22.2 Route update

After briefing:
- no forced full-screen map;
- compact New Routes Available presentation:
  - Western Forest route
  - Whitecrest high road
- player may Open Map or close.

Neither route is labeled better/correct.

---

# 23. Dynamic event UX — Roadside Trouble

On eligible active-volume entry:

- event title appears for **2.5 s**;
- three-condition tracker enters below normal objective list;
- driver bark is subtitled according to accessibility settings.

Participation is not shown as a hidden score.

On completion:
- all three requirements check;
- compact completion header;
- EXP/Class XP/Gold use normal reward presentation;
- tracker disappears after **4 s**.

On abandonment/reset:
- tracker fades after leaving relevance;
- no failure modal.

---

# 24. Death / respawn in R01

Global death UI from UI_DIRECTION.md remains exact.

R01 adds checkpoint names.

Before Quarry Waystone:
~~~text
Respawn: Alderford Shrine
~~~

After Quarry Waystone qualifies:
~~~text
Respawn: Quarry Waystone
~~~

Earthloong wipe never presents Restart Dungeon because run/shortcut state already defines recovery.

Single-player does not wait in fake Downed state.

---

# 25. NPC/service interaction focus

World interact highlight uses one common treatment:

~~~text
<Interaction binding> <Name / Service>
~~~

Examples:
- Elian Rook — Class Service
- Nessa Bell — Market
- Holt Forge
- Greenwater Remedies
- Alderford Vault
- Property Sign

Do not add a permanent floating icon over every service NPC.

World signage/silhouette remains primary wayfinding.

---

# 26. Empty / error / race-condition states

## Merchant stock changed

~~~text
That stock has changed. The market list has been refreshed.
~~~

No Gold removed.

## Property race

~~~text
This residence is no longer vacant.
~~~

No Gold removed.

## Craft materials changed

~~~text
You no longer have the required materials.
~~~

Refresh counts.

## Reward already claimed

~~~text
That reward has already been claimed.
~~~

Close stale reward screen. Never duplicate reward.

## Lost service actor / client desync

Close safely and preserve server state. Never expose exception/internal packet text.

---

# 27. Accessibility requirements

All R01 screens:

- show current rebindable action glyph/text;
- support keyboard focus independently of mouse;
- do not encode grade/validity/status only by color;
- respect UI scale;
- preserve subtitles/captions outside modal overlap;
- do not remove required boss telegraphs at Low VFX;
- never reveal Regalhart or undiscovered POIs through accessibility outlines.

Focus order:

~~~text
navigation → primary content → detail → secondary action → primary action
~~~

Back returns in reverse screen-stack order.

---

# 28. Screen transition rules

Opening a full service screen:
- allowed only while not in an incompatible committed combat/action state;
- ordinary nearby ambient world continues;
- player movement is locally locked while screen owns input.

If hostile combat becomes authoritative while a non-combat service screen is open:
- close the service screen;
- no transaction commits unless server already accepted it.

World overlays such as fishing/furniture/camp use their own action restrictions.

No service UI gives invulnerability.

---

# 29. R01 UI data/state contract

Client receives server-authoritative presentation data for at least:

~~~text
hud/
  resources
  skills
  recovery_belt
  objective_tracker
  event_tracker
  target_boss

class_first_select/
  available_roots
  preview_data
  first_selection_state
  starter_grant_state

quest/
  visible_entries
  objective_state
  pin_state
  map_precision
  pending_claims

merchant/
  cycle_id
  next_refresh_active_time
  rotating_slots
  fixed_household_catalog
  price
  affordability

service/
  known_recipes
  owned_material_counts
  output_preview
  exact_cost

housing/
  property_state
  price
  sale_credit
  furnishing_package
  owned_furniture
  placement_validation

fishing/
  spot_state
  hook_window_state
  tension_state
  catch_result
  codex_record

rewards/
  earthloong_pending_choice
  claimed_state
~~~

Client may animate/predict presentation.

It does not author Gold, item ownership, quest progression, property ownership, fish result, reward choice or class commit.

---

# 30. R01 screen acceptance matrix

| Screen/state | Blocking? | Can close without commit? | Server commit point |
|---|---|---|---|
| HUD | no | n/a | none |
| contextual Dodge hint | no | n/a | hint-seen after actual display |
| first class select | yes | yes | Confirm class |
| guild board | yes | yes | optional contract Accept |
| journal | yes | yes | Pin/Abandon/Claim |
| map | yes | yes | Travel request |
| inventory | yes | yes | item/equip transaction |
| Recovery Belt setup | yes | yes | assignment/reload |
| market | yes | yes | Buy/Sell |
| forge | yes | yes | Forge confirm |
| alchemy/cooking | yes | yes | Craft transaction |
| Trail Stag unlock | no | automatic | unlock already committed |
| property purchase | yes | yes | Purchase/Move confirm |
| furnishing mode | overlay | yes | each validated place/move |
| camp placement | overlay | yes | validated placement |
| fishing | overlay | according to fishing action state | server catch resolution |
| Fish Codex | yes | yes | read-only |
| boss HUD | no | n/a | combat server-owned |
| Earthloong 3-choice | yes | **yes, remains pending** | item-choice Confirm |
| route update | no | yes | route state already committed |
| death | yes | no fake penalty choice | death/respawn transaction |

---

# 31. Exact presentation states that remain ASSET_BINDING

This document closes interaction/information architecture. It does **not** authorize improvised final art.

Before R01 UI is presentation-ready, bind/validate:

- exact Lucifer frame/button/tab/sprite files;
- exact Lucifer Equipment slot/background pieces;
- Kenney support pieces only where needed;
- final project font(s) with Korean coverage and license;
- exact R01 skill/item/service/quest icons;
- exact model-render pipeline for equipment previews;
- exact map marker sprites;
- exact status icons;
- exact property/furniture/fish preview assets.

If the chosen pack lacks one component, reconstruct it from the admitted family grammar or an admitted support family rather than introducing an unrelated theme.

---

# 32. Real-client visual acceptance

R01 UI is not finished until actual Minecraft screenshots/video verify:

1. 1920x1080 normal GUI scale;
2. smaller laptop-class resolution;
3. at least two other GUI scales;
4. long Korean labels;
5. smallest/longest believable Gold and stat values;
6. 36-slot initial backpack without unreadable slots;
7. 4-skill + Ultimate HUD over bright meadow and dark quarry;
8. objective + Roadside Trouble tracker without covering combat reads;
9. first class selection readable without a wall of text;
10. five-slot Nessa stock visibly distinct from Household fixed stock;
11. three Earthloong reward choices compare cleanly;
12. property total + furnishing toggle cannot be misunderstood;
13. fishing tension does not obscure world/fish presentation;
14. subtitles do not collide with prompts/service dialogs;
15. no stretched/blurry Lucifer frames.

Visual failure updates this specification or asset binding before propagation to every screen.

---

# 33. What this specification closes

Closed for R01 implementation:

- HUD ownership/layout;
- objective/event density;
- first-class selection preview/confirm/cancel;
- guild-board behavior;
- journal/map hierarchy;
- inventory/R01 comparison behavior;
- Material Vault UX;
- Recovery Belt setup;
- market tab separation and confirmation policy;
- forge/alchemy/cooking sequence;
- Trail Stag unlock presentation;
- property purchase/move UX;
- furnishing/Camp overlays;
- fishing hook/tension/catch presentation;
- Fish Codex hierarchy;
- boss frame behavior;
- Earthloong pending 3-choice behavior;
- post-quarry route update;
- Roadside Trouble tracker;
- death checkpoint naming;
- error/race states;
- focus/accessibility rules;
- server/client ownership.

Still not complete until external UI/icon/model bindings and actual Minecraft visual review are done.

Current verification:

~~~text
R01 UI INTERACTION DESIGN REVIEWED: YES
R01 UI SOURCE ASSET BINDINGS: NOT COMPLETE
R01 UI IMPLEMENTED: NO
R01 UI SCREENSHOT REVIEWED: NO
TESTED: NO
BUILD VERIFIED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
~~~
