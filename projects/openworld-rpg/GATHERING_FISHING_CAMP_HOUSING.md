# Open-World RPG — Gathering / Fishing / Camp / Housing Canon

> Status: **DESIGN CANON — field gathering, fishing, temporary camps and current one-residence housing rules locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Fishing/housing refinement: `FISHING_COLLECTION_HOUSING_MARKET.md`  
> R01 resources/equipment: `EQUIPMENT_BALANCE.md`  
> Recovery/cooking: `RECOVERY_PRODUCTION_APPEARANCE.md`  
> Opening flow: `R01_VERTICAL_SLICE.md`  
> UI: `UI_DIRECTION.md`  
> External provenance: `EXTERNAL_SOURCES.md`  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins. The one-residence/trade-up/storage rules from `FISHING_COLLECTION_HOUSING_MARKET.md` are now merged here; older multi-home wording is removed and must not be restored from history.

This document closes implementation-time gaps without turning the open-world RPG into a survival chore simulator.

Shared objective:

```text
explore
→ notice a useful place/resource
→ perform a short readable interaction
→ gain a resource, fish, rest option or property value
→ feed cooking/forge/economy/collection
→ return to exploration
```

No subsystem below introduces a new numeric currency, routine durability, mandatory hunger, long gathering grind, camp fast-travel network or construction-heavy housing simulator.

---

# 1. External precedents and adoption boundaries

## 1.1 Guild Wars 2 — readable node categories, dedicated tools

Project adoption:

- Mining / Forestry / Herbalism use dedicated field tools;
- nodes belong to region/terrain/ecology rather than random hidden block spam;
- better tools unlock stronger nodes and shorten interaction time.

Not adopted:

- disposable tool charges;
- tool breakage after a fixed number of uses;
- junk-material punishment for using the wrong tool.

Reference: `https://wiki.guildwars2.com/wiki/Gathering`

## 1.2 Genshin Impact / Stardew Valley — one real fishing interaction layer

Project adoption:

- deliberate cast;
- readable bite cue + reaction window;
- common fish resolve quickly;
- stronger/rarer fish use a compact hold/release tension phase.

Not adopted:

- long mandatory fishing sessions;
- one bait type per ordinary fish species;
- frequent trash catches;
- difficult minigame for every tiny common fish.

References:
- `https://genshin-impact.fandom.com/wiki/Fishing`
- `https://wiki.stardewvalley.net/Fish`

## 1.3 Monster Hunter Wilds — field camp as convenient infrastructure

Project adoption:

- one reusable Field Camp Kit per player;
- one active deployed camp per owner;
- rest, Recovery Belt reload and cooking;
- authored no-camp volumes protect bosses/dungeons/towns/critical routes.

Not adopted:

- camp fast travel at baseline;
- repeated currency fee per deployment;
- routine destruction/repair busywork.

## 1.4 Housing storage — physical residence, one logical storage pool

Useful precedent from ESO-style explicit home storage remains valid, but the project ownership model is now stricter:

- the player owns **one residence at a time**;
- the current residence exposes one personal `Home Storage` pool whose capacity depends on residence tier;
- multiple cabinets/chests are access points to that same logical pool, not independent capacity multiplication;
- moving residence migrates storage/furniture safely as one atomic transaction;
- furnishing remains physical and visible.

Reference: `https://help.elderscrollsonline.com/app/answers/detail/a_id/41067/`

---

# 2. External visual / motion source direction

## 2.1 Gathering tools and resource nodes

Selected source families:

- KayKit `RPG Tools Bits` — pickaxe/axe/hand-tool family;
- KayKit `Resource Bits` — ore, wood and material pickup family;
- Quaternius `Stylized Nature MegaKit` — herb/plant/rock node family;
- KayKit `Forest Nature Pack` — fallback environmental node family;
- KayKit Character Animations 1.1 — `Pickaxe`, `Pickaxing`, `Chop`, `Chopping`, `Dig`, `Digging`, `Work_*`, `Working_*`.

Exact model files still pass provenance/hash/scale review before implementation.

## 2.2 Fishing ecology and motion

Preferred visible fish family:

- Quaternius `LowPoly Animated Fish` / creator-uploaded `Animated Fish` artifact;
- fish are rigged and include swimming animation;
- package-specific license/source evidence must be preserved.

R01/global fish names are locked only after actual model inspection during pre-code asset intake.

Fishing player-motion family:

```text
Fishing_Cast
Fishing_Bite
Fishing_Catch
Fishing_Idle
Fishing_Reeling
Fishing_Struggling
Fishing_Tug
```

The final rod comes from an accepted external source. Do not retain vanilla rod presentation merely because mechanics work.

## 2.3 Camp visuals

Primary candidate: Kenney `Survival Kit` 2.0, CC0.

Use a coherent compact subset for tent/shelter, bedroll, campfire/cooking, crate/bag/sign and utility dressing. A camp must not look like a prefabricated house.

## 2.4 Housing visuals

- shells use the same accepted Medieval Village family as their settlement;
- furniture uses accepted Quaternius Fantasy Props, compatible KayKit/furniture dependencies or another coherent external family;
- shell/interior cannot look like unrelated asset-store projects.

---

# 3. Shared gathering interaction contract

Gathering is a short authored interaction, not repeated block breaking.

## 3.1 Tool Pouch

```text
Mining Pick
Woodcutter Axe
Harvest Knife / Sickle
Fishing Rod
```

Rules:

- no backpack/hotbar consumption;
- correct tool auto-equips visually for a valid interaction;
- no routine durability;
- field tools are not combat equipment;
- visuals use accepted external models/animations.

The player starts with Field versions of all four tools.

## 3.2 Interaction timing

| Node/action | Baseline time | Presentation |
|---|---:|---|
| herb / small forage | 0.65 s | one cut/pick motion |
| timber node | 1.35 s | two readable chop beats |
| ordinary ore | 1.55 s | two-to-three readable pick strikes |
| rare crystal / dense mineral | 1.80 s | stronger mining commitment |
| ordinary pickup/cache | 0.35–0.55 s | hand/pickup interaction |

Rules:

- no gathering in combat, mounted, downed, climbing or incompatible committed action;
- movement/dodge can cancel before server resolution without node consumption;
- after resolution, reward ownership is committed even if recovery animation is interrupted;
- hostile interruption before resolution cancels and leaves the personal node available;
- visible contact and server resolution align within about one tick where practical.

## 3.3 Node targeting / readability

- real external silhouettes/environmental placement;
- no permanent giant beam;
- restrained outline/marker only at useful interaction distance or accessibility setting;
- ordinary nodes not all revealed on world map;
- rich/rare authored locations may be remembered after discovery.

---

# 4. Tool progression

Three permanent tool tiers across Lv1–80:

| Tool tier | Speed | Access |
|---|---:|---|
| Field | 100% action time | ordinary/common |
| Refined | 85% | ordinary + dense regional |
| Masterwork | 75% | all ordinary/dense + authored high-tier |

Rules:

- permanent Tool-Pouch state, no charges;
- higher tier gathers lower nodes normally;
- insufficient tier does not consume node or yield junk;
- unlock through world/service/material progression, not naked level checks;
- upgrades use real materials + Gold, no separate tool currency;
- exact recipes are closed in region content before implementation.

---

# 5. Light gathering mastery

Mining, Herbalism, Forestry and Fishing each have Rank I–V automatic mastery.

No points/tree/respec/daily grind.

## 5.1 Mastery XP

```text
ordinary successful gather/catch: 1
rich/dense/rare gather/catch: 3
first discovery of a material/fish family in a major region: +10 once
relevant authored contract/discovery: +6 to +15
```

Thresholds:

```text
Rank I   0
Rank II  20
Rank III 60
Rank IV  130
Rank V   240
```

## 5.2 Mining / Herbalism / Forestry benefits

| Rank | Benefit |
|---|---|
| I | baseline |
| II | action time -5% |
| III | 5% chance +1 ordinary base material |
| IV | total action time -10% |
| V | 10% chance +1 ordinary base material; rare-secondary roll +10% relative |

No bonus duplicates boss/signature materials; mastery cannot bypass tool tier.

---

# 6. R01 node behavior

| Resource | Base yield | Personal respawn |
|---|---:|---:|
| Iron Ore | 2–4 | 7 active min |
| Hardwood | 2–3 | 5 active min |
| Healing Herb | 1–2 | 4 active min |
| Verdant Crystal | 1 | 18 active min |

Rules:

- personal active-playtime cooldowns;
- shared shell may remain visible, availability is personal;
- one player cannot deny another's harvest;
- relog/dimension change does not reset;
- server-authoritative save state;
- ordinary nodes use Field tools;
- if Verdant Crystal later requires Refined Pick for ordinary nodes, the first scripted/required R01 progression access remains non-softlocking and is documented before implementation.

---

# 7. Fishing — world interaction

Fishing remains a compact optional loop.

## 7.1 Fishing spots

Use authored/region-generated shoal spots at sensible river bends, pools, banks, docks, reefs and region waters.

Presentation:

- water ripple/disturbance;
- occasional accepted submerged fish silhouette where practical;
- no permanent giant fishing icon.

R01/common spot:

```text
personal catches before depletion: 2
personal respawn: 6 active min
```

Rare/special spot:

```text
personal catches: 1
personal respawn: 15–20 active min or authored condition
```

## 7.2 Casting

1. use Fishing Rod near valid spot;
2. accepted idle/cast stance;
3. brief aim hold;
4. release cast;
5. invalid landing returns line without consuming spot;
6. ordinary fishing uses no bait.

## 7.3 Bite / hook

```text
bite delay: 1.5–5.0 s
```

| Difficulty | Hook window |
|---|---:|
| common | 0.90 s |
| uncommon | 0.75 s |
| rare | 0.65 s |
| trophy/signature | 0.55 s |

Missed hook does not consume spot charge.

## 7.4 Catch resolution

Common fish: ~1.0–1.5 s short automatic finish after hook.

Uncommon/rare/trophy: compact hold/release tension system, 2.5–6 s normally and exceptional trophy catch capped around 8 s.

## 7.5 Fishing mastery

| Rank | Benefit |
|---|---|
| I | baseline |
| II | maximum bite wait -8% |
| III | hook windows +0.08 s |
| IV | maximum bite wait -15% total |
| V | tension valid-zone width +10%; one extra bounded trophy-size/value roll |

Mastery cannot replace exploration/location conditions with brute-force rarity farming.

## 7.6 Fish rewards

Fish feed cooking, contracts, collection/trophies, Gold and only visually/logically justified regional material roles.

No equipment drops, large junk table or fishing currency.

Launch collection scale and current one-residence trophy/display rules are canonical in `FISHING_COLLECTION_HOUSING_MARKET.md`.

---

# 8. Field Camp Kit

A camp is temporary field infrastructure, not a portable settlement.

## 8.1 Unlock / recipe

```text
4 Hardwood
+ 2 Tough Hide
+ 40 Gold service fee
→ Field Camp Kit
```

Permanent reusable kit; deployment does not consume another recipe.

## 8.2 Deployment

```text
commit time: 2.5 s
maximum active camps per owner: 1
```

Requires out of combat, suitable footprint, not submerged, outside settlements/dungeons/boss/event/protected volumes, not blocking critical travel, >=24 blocks from shrine/major service center and >=12 blocks from another camp unless intentionally sharing a party camp zone.

Failed placement loses nothing.

## 8.3 Functions

Provides:

- full HP/Mana/Stamina rest;
- Recovery Belt reload from carried reserves;
- cooking;
- safe inventory/equipment management out of combat;
- physical tent/bedroll/campfire presentation.

Does not provide:

- fast travel;
- death checkpoint;
- Material Vault;
- forge/alchemy/class service/merchant.

## 8.4 Persistence / multiplayer

- server-authoritative placement/owner/save;
- one persisted active camp per owner;
- nearby eligible players may rest/cook without gaining ownership;
- personal inventory/reserves remain personal;
- successful redeploy removes previous camp;
- no relog/disconnect duplication;
- random ambient camp destruction is not a baseline maintenance mechanic.

---

# 9. Housing — one-residence ownership model

Housing is a physical settlement property system, not an instanced menu room and not a colony builder.

## 9.1 Physical properties

- settlements contain authored purchasable houses;
- exterior remains in the real world;
- every property has stable server-side `property_id`;
- property ownership is server-authoritative;
- a physical property has one primary owner;
- optional trusted/guest permissions do not create co-ownership of the property economy;
- authored shell is protected from destructive structural edits.

Starting settlement target:

- at least **3–4 Small Cottage-class vacancies** for intended small multiplayer;
- at least one visibly larger future-upgrade residence.

No story/reputation permission gate for the first residence; Gold is the practical gate.

## 9.2 Property tiers / price anchors

| Tier | Baseline purchase target | Role |
|---|---:|---|
| Small Cottage | **2,400 Gold** | first residence |
| Town House | **9,000 Gold** | midgame move, larger display/storage |
| Large Residence | **25,000 Gold** | major furnishing/trophy space |
| Prestige Estate | **65,000+ Gold** | optional late luxury/collection sink |

Equivalent shells should not have wildly different prices. The optional first furnishing package remains approximately **750 Gold**.

## 9.3 One residence / moving

Canonical launch rule:

```text
maximum owned residential properties per player: 1
```

Progression:

```text
save Gold
→ buy one vacant residence
→ furnish/use it
→ later select another vacant residence
→ atomic Move / Trade Residence transaction
→ old property sold
→ furniture/storage migrated safely
→ own only the new residence
```

The old multi-property rule is deleted. There is no `Primary Residence` plus extra owned homes model at launch.

## 9.4 First purchase

If no residence is owned:

```text
validate vacancy + Gold
→ pay full price
→ commit ownership
```

## 9.5 Move / trade-in

Do not require selling first.

```text
required Gold difference
= new purchase price
- old-home sale credit
```

Baseline sale credit:

```text
80% of old property's normal purchase price
```

Rules:

- merchant/reputation discounts do not increase resale credit;
- normal resale cannot generate profit;
- furnishings are not sold with shell;
- unavailable/newly occupied target aborts transaction with no changes;
- special quest-granted property would require an explicit authored rule.

## 9.6 Safe furniture/storage migration

Before ownership/Gold changes, server builds a durable Moving Inventory containing:

- all owner movable furniture;
- all ordinary owner display/trophy items;
- current Home Storage contents.

Successful move:

1. validate target vacancy, owner, Gold and migration state;
2. reserve transaction server-side;
3. create durable migration snapshot;
4. apply Gold delta;
5. transfer ownership;
6. migrate furniture/storage;
7. reset old property to authored default;
8. finalize idempotent transaction.

If validation/migration fails, transaction aborts before irreversible ownership/Gold changes.

Moving into a smaller residence is allowed. Overflow movable objects enter a temporary `Moving` section until withdrawn/placed; it cannot be used as infinite permanent storage.

---

# 10. Housing functions

Baseline functions:

- rest;
- Home Storage;
- furnishing/decor placement;
- boss/collection trophy display;
- Wardrobe/appearance access through appropriate furniture;
- basic cooking after installing suitable kitchen/cooking furnishing.

Not baseline:

- forge;
- full alchemy lab;
- class change/advancement;
- merchant;
- shrine fast travel;
- death-checkpoint override;
- material automation.

## 10.1 Home Storage by residence tier

| Residence | Home Storage |
|---|---:|
| Small Cottage | 54 ordinary slots |
| Town House | 72 ordinary slots |
| Large Residence | 108 ordinary slots |
| Prestige Estate | 144 ordinary slots |

Rules:

- one logical pool for the currently owned residence;
- ten cabinets do not multiply capacity;
- accepted storage furniture is an access point/visual object;
- Material Vault remains separate with its own material role;
- move/sale never deletes contents;
- migration snapshot protects contents before old property reset;
- key/quest items never require Home Storage.

---

# 11. Furnishing interaction

Housing customization is lighter than unrestricted survival building.

## 11.1 Protected shell

Owner may:

- place/remove/move accepted furniture in owned interior/property volumes;
- rotate supported furnishings;
- choose authored material/color variants.

Owner may not freely delete:

- load-bearing walls;
- roof;
- settlement roads;
- neighboring geometry;
- service infrastructure.

## 11.2 Placement UX

- ghost preview of real accepted furnishing;
- valid/invalid placement feedback;
- grid/surface snapping default;
- 90-degree rotation baseline, finer only where valuable;
- cancel returns item without loss;
- no construction currency.

## 11.3 Starter furnishing package

~750 Gold target, coherent minimal set:

- bed/rest point;
- Home Storage access chest/cabinet;
- table + 2 chairs;
- lighting;
- simple shelf/cabinet;
- one trophy/display surface;
- small matching decor set.

Exact external models are closed in asset intake before implementation.

---

# 12. Trophy / collection use

Housing provides a destination for memorable rewards without combat-stat stacking.

Examples:

- Regalhart antler display;
- Earthloong trophy/display;
- trophy fish where accepted asset supports it;
- region keepsakes.

Rules:

- cosmetic/collection-first;
- no hidden combat stat bonus;
- no mandatory trophy checklist for region completion;
- server-authoritative unlock/item identity;
- migration preserves trophies when moving residence.

---

# 13. Multiplayer authority / edge cases

## Gathering

Server owns personal node availability, resolution/result, yield, mastery XP and tool-tier validity.

## Fishing

Server owns spot charge/cooldown, result selection, hook validation, tension result, reward/mastery.

## Camp

Server owns placement legality, owner, position, persistence and service flags.

## Housing

Server owns:

- current residence property ID;
- vacancy/ownership;
- trusted permissions;
- furniture state;
- Home Storage;
- moving transaction;
- trophy state;
- Gold changes.

Two players cannot buy the same vacancy simultaneously. A player cannot own a second residence while the first ownership remains active.

---

# 14. Performance rules

## Resource nodes

- no every-tick global scan;
- chunk/region-local authored data + event-driven interaction;
- cooldown checks on interaction/visibility boundaries.

## Fishing

- lightweight region spot objects;
- no huge schools of pathfinding fish solely for loot visuals;
- bounded client/ambient fish visuals where useful.

## Camps / housing

- static/bounded rendering strategies appropriate to asset backend;
- no decorative-furniture pathfinding or per-tick UI calculation;
- furnishing-count limits based on measured performance, not arbitrary tiny caps.

---

# 15. UI requirements

All systems use Lucifer-family grammar.

## Tool Pouch / mastery

Compact four-tool view with tool tier + Rank I–V indicator; no gathering mastery tree screen.

## Fishing

Common catch uses almost no modal UI; tension phase uses one compact combat-safe tension/progress element.

## Camp

World ghost placement + small validity reason; rest/cook reuses existing service language.

## Housing

Purchase/move screen shows:

- property name/location;
- real-world/exterior preview;
- tier;
- purchase price;
- room/usable-footprint summary;
- Home Storage capacity;
- furnishing package status;
- current old-home 80% trade-in credit;
- exact Gold difference for Move / Trade Residence.

Furnishing mode is world-first and compact. Home Storage reuses inventory components.

---

# 16. Data contract

Suggested ownership:

```text
gathering/
  tool_tiers
  mastery
  nodes/*
  regions/*

fishing/
  spots/*
  fish/*
  regional_tables/*

camp/
  field_camp
  placement_rules

housing/
  properties/*
  furnishing_catalog/*
  storage
  moving_transactions
```

## Node schema

```text
id
category
region_tags
required_tool_tier
interaction_time
yield_table
personal_respawn
model_source_id
animation_binding
secondary_rolls
```

## Fish schema

```text
id
player_facing_name
region_tags
water_tags
rarity
hook_window
tension_profile
food_or_material_outputs
model_source_id
icon_source_id
conditions
```

## Camp schema

```text
kit_id
owner
position
rotation
deployed
visual_set_id
service_flags
placement_rule_set
```

## Housing property schema

```text
property_id
settlement_id
property_tier
normal_purchase_price
owner_uuid
trusted_decorators[]
guest_permissions
furnishing_volume
furnishings[]
home_storage_profile
trophy_state[]
```

## Player housing state

```text
current_residence_property_id | null
moving_transaction_id | null
```

There is no owned-properties array at launch.

---

# 17. R01 acceptance targets

Before these systems are source-ready for R01:

1. exact R01 herb/ore/wood node models bound and scale-reviewed;
2. Pick/Axe/Harvest Knife/Rod models accepted;
3. work/fishing animations retarget-tested;
4. at least the canonical early R01 fish model set selected and assigned player-facing identities;
5. fishing tension UI accepted in Lucifer family;
6. camp model family acquired/hashed/inspected;
7. Small Cottage exterior/interior visually composed from accepted settlement/furniture families;
8. starter furnishing package models pinned;
9. personal node/fishing/camp/housing state has explicit server save ownership;
10. property buy/move/80%-resale/migration transaction is specified exactly as §9 before implementation;
11. actual Minecraft playtest later checks gathering speed, fishing fatigue, camp usefulness, housing travel friction and migration UX.

---

# 18. What this closes

Closed as design:

- Tool Pouch and starting Field tools;
- gathering timing/cancellation;
- three tool tiers;
- five-rank non-grindy gathering/fishing mastery;
- R01 node timing;
- fishing spot/cast/hook/tension loop;
- reusable Field Camp Kit and server authority;
- **one residence at a time**;
- property tiers: 2,400 / 9,000 / 25,000 / 65,000+ Gold;
- **80% resale/trade-in**;
- atomic Move / Trade Residence;
- durable furniture/storage migration;
- Home Storage tiers: 54 / 72 / 108 / 144 ordinary slots;
- furnishing/trophy rules;
- multiplayer ownership/transaction safety;
- performance and data ownership boundaries.

Remaining work is external asset intake, exact fish identity/model binding, exact property-shell/furniture binding, UI visual acceptance, implementation and evidence-driven playtest tuning — **not permission to reintroduce multi-home ownership or invent a different housing economy during coding**.