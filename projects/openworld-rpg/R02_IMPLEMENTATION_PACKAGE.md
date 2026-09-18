# Open-World RPG — R02 Western Forest Implementation Package

> Status: **DESIGN CANON — R02 world/combat/service/reward flow is implementation-ready; exact external asset file bindings remain gated by intake where explicitly marked**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Region graph: `REGIONS.md`  
> Cross-region refinement: `REGION_CROSS_AUDIT.md`  
> Combat: `COMBAT_BALANCE.md`, `STATUS_AND_R01_ENCOUNTERS.md`  
> Loot/equipment: `LOOT_ECONOMY.md`, `EQUIPMENT_BALANCE.md`  
> Field systems: `GATHERING_FISHING_CAMP_HOUSING.md`, `FISHING_COLLECTION_HOUSING_MARKET.md`  
> Quest/state: `QUEST_WORLD_STATE.md`  
> UI: `UI_DIRECTION.md`  
> External provenance: `EXTERNAL_SOURCES.md`  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins. Later explicit anti-repetition refinements in `REGION_CROSS_AUDIT.md` are incorporated into this file and must not be reintroduced from older snapshots.

This package turns the already-canonical R02 western rich forest / river basin into a region that can be implemented without inventing its flow during coding.

The region's core play sentence is:

```text
leave the open R01 heartland
→ enter a denser forest where roads split and landmarks hide behind terrain
→ discover a compact logging/trade hamlet
→ follow river, timber and ruin evidence instead of a single corridor
→ gather/fish/hunt while learning stronger forest ecology
→ find an ancient ruined sanctum
→ defeat its Lich without turning the forest into an undead biome
→ leave with DEX/WIL/status/forest-traversal gear identity and multiple onward routes
```

R02 is **not** R01 with darker trees. It must feel more enclosed, more discovery-driven and more magical while remaining readable and comfortable to traverse.

---

# 1. Locked identity preserved from REGIONS.md

Canonical baseline remains:

- terrain: western rich forest + river basin;
- suggested entry Lv: **8**;
- first dense-exploration region;
- narrower roads, hidden landmarks and river forks;
- mysterious, not oppressive;
- Nature Spirit becomes a native major threat;
- Lich appears only in authored ruins;
- logging/trade hamlet smaller than the R01 starting hub;
- hardwood, resin, mushrooms, healing plants and mana-active flora;
- dungeon: ancient woodland sanctum / collapsed arboretum;
- reward identity: DEX/WIL utility, poison/status resistance, forest movement, bow/finesse and nature-linked accessories.

No global scaling or level gate is introduced.

---

# 2. External-first regional source stack

## 2.1 Forest/environment

Primary visual family:

- Quaternius `Stylized Nature MegaKit` Standard / accepted source-specific package;
- current creator pages describe 116 total models in the full family and 68 in the free Standard snapshot, including trees, plants, flowers, rocks, grass, bushes and mushrooms;
- creator-uploaded OpenGameArt Standard snapshot is CC0.

Currently evidenced Standard filenames useful to R02 include:

```text
Mushroom_Common.gltf
Plant_7.gltf
CommonTree_1.gltf
TwistedTree_2.gltf
Rock_Medium_2.gltf
Clover_1.gltf
Petal_1.gltf ... Petal_5.gltf
```

These filenames are candidate bindings, not automatic acceptance. Exact locally acquired package, license evidence, SHA-256, Minecraft scale and material appearance must still be recorded before committing raw bytes.

R02 uses the family mainly for readable forest nodes, mushroom/plant resource presentation, twisted/deep-forest silhouettes, ruin-overgrowth dressing and field-boss arena framing.

Do not cover Azari terrain with thousands of display entities. Translate large-scale forest language into performant world/block treatment and reserve model assets for nodes, props and landmark detail.

## 2.2 Settlement / ruins

Use the already-selected coherent families:

- Quaternius `Medieval Village MegaKit` / source-specific Standard evidence — hamlet architectural language;
- Quaternius `Fantasy Props MegaKit` — carts, trade/storage, furniture, books, tools, market/inn dressing;
- Quaternius `Ultimate Modular Ruins Pack` / `Modular Dungeon Pack` — sanctum/arboretum ruin reference and model details;
- accepted Kenney CC0 support only where a missing primitive genuinely improves the result.

The logging hamlet and ruined sanctum should look like parts of the same world, not two unrelated downloaded packs.

## 2.3 Ordinary fauna / common threats

### Forest Wolf

Candidate source:

- Quaternius animated `Wolf` model, current Poly Pizza entry marked Public Domain / CC0;
- animated low-poly silhouette is visibly distinct from vanilla Minecraft wolf presentation.

Project use:

- small forest packs as readable predators;
- no vanilla wolf model or vanilla tame-loop inheritance;
- exact final project-facing species name may remain simple if the accepted model reads clearly as a wolf.

### Forest Spider

Candidate source:

- Quaternius `LowPoly Animated Easy Enemies` Spider;
- source family is CC0 and supplies movement/attack/jump/death animation coverage.

Project use:

- low/medium-density ambush predator around roots, fallen timber and shaded cuts;
- not a vanilla giant-spider reskin.

### Existing dependency ecology

- Raccoon / Crow / Grizzly / Cave Centipede remain curated dependency ecology where their current 26.2 dependency path passes integration review;
- Cave Centipede remains underground/root-cut ecology, not a daylight forest trash mob;
- Grizzly remains rare territorial wildlife rather than an equipment farm.

### Bunfungus correction

`REGIONS.md` listed Bunfungus as a possibility only if it strengthened identity.

Production decision for baseline R02:

- **do not use Bunfungus as a normal R02 spawn**;
- its established external mushroom-field identity is too specific and would make the western forest feel like a transplanted donor biome;
- it may be reconsidered only for a future dedicated fungal pocket if the actual Azari terrain supports one.

## 2.4 Nature Spirit / Lich dependency boundary

Current R02 uses `Nature Spirit` and `Lich` from Threateningly Mobs Continued as dependency-only entities.

License evidence remains conflicted across current storefront metadata. Therefore:

- normal installed dependency use may proceed when terms/runtime allow;
- do not copy continuation model/code/assets into the public repository until exact upstream/source licensing is reconciled;
- project-owned stats, regional spawning, attacks/rewards/state remain canonical regardless of donor defaults.

The original/current source history supports Lich having a reinforcement-oriented skill identity. R02 preserves that clue rather than turning the boss into a generic projectile sponge.

## 2.5 R02 field-boss visual candidate

The field guardian must not be merely a larger/recolored R01 Nature Spirit.

Primary current candidate:

- Quaternius `Goleling Evolved` from the Ultimate Monsters family / creator model distribution;
- animated external model; current source evidence identifies the family as CC0.

Status:

```text
VISUAL CANDIDATE — NOT YET FINAL PLAYER-FACING NAME
```

Before final binding:

1. acquire exact model from a source with preserved license evidence;
2. inspect silhouette at Minecraft scale;
3. inspect available attack/run/hit/death animation clips;
4. verify the model reads as an ancient forest/stone guardian without relying on a bad recolor;
5. only then lock player-facing guardian name and exact animation bindings.

Fallback order if the model fails:

1. another distinct animated CC0 Quaternius Ultimate Monsters guardian/golem model;
2. another legally redistributable external forest-guardian model of equal/higher quality;
3. never `Nature Spirit x 1.8 scale + green particles`.

## 2.6 Lich reinforcement visual

If the dependency's own reinforcement actor cannot be normalized cleanly to project encounter rules, use an accepted external animated ghost/shade model such as a compatible Quaternius Ultimate Monsters Ghost as a project-owned Sanctum Shade presentation.

---

# 3. Spatial progression / local encounter pressure

R02 Suggested Entry Lv stays **8**.

| Sub-area | Intended local pressure | Role |
|---|---:|---|
| western transition / old logging road | Lv 8–9 | readable entry, route choice begins |
| hamlet / near-river working woods | Lv 8–10 | services, fishing, normal gathering |
| river forks / mossy woodland | Lv 9–10 | exploration, fish/resources, common threats |
| deep grove / twisted forest | Lv 10–11 | Nature Spirit territory, hidden POIs |
| root cuts / old extraction tunnels | Lv 10–12 | Cave Centipede / dangerous resource pockets |
| ruin belt / sanctum approaches | Lv 11–13 | undead signs, elite pressure, dungeon lead |
| roaming guardian hunt | Lv 13 | optional field boss |
| woodland sanctum dungeon | Lv 12–14 | compact regional dungeon |
| Lich | **Lv 14** | dungeon boss |

R03 begins at suggested Lv12, so the western forest deliberately overlaps its upper difficulty edge. The player may leave R02 early for the mountains or stay to clear harder forest content.

No invisible wall appears at the sanctum, R03 exit or field-boss arena.

---

# 4. Navigation / landmark language

Use at least these landmark classes when Azari inspection permits:

1. main river — persistent macro-navigation spine;
2. logging road — broadest safe-ish route from R01 into hamlet;
3. split river ford / bridge — memorable decision point;
4. tall twisted-tree grove — deep forest silhouette visible through canopy gaps;
5. ruined sanctum tower/arch — glimpsed before full dungeon discovery;
6. mountain/ridge sightline — signal toward R03;
7. one dangerous high-ground/stone clearing — guardian-hunt territory.

Rules:

- no permanent floating arrows;
- Trail Stag remains usable on main routes;
- side paths may require dismount when doing so reveals real content;
- repeated fallen-log jumping is not a traversal gimmick;
- river crossings are deliberately placed so water does not become constant friction.

---

# 5. R02 logging/trade hamlet

The R02 hamlet is a secondary regional hub, smaller than Alderford.

Daytime population target:

```text
6–9 functional/guard NPCs
3–5 ambient townsfolk
```

No vanilla villagers.

## 5.1 Baseline services

Present from first discovery:

- shrine / fast-travel discovery;
- inn / rest / food;
- general regional merchant;
- tradehouse counter with Material Vault access;
- regional contract / ranger board;
- fish/food buyer or combined inn-market buyer;
- furnishing/carpenter merchant role for regional home decor and property interaction;
- stable hitch/Trail Stag convenience space, not a second mount-progression tutorial.

Not duplicated here by default:

- full class-change/advancement service;
- full advanced smith forge;
- full advanced alchemy lab;
- every R01 specialist NPC.

Basic regional recipes/orders may be handed in here when they do not require full forge/alchemy infrastructure.

## 5.2 Housing

R02 visibly introduces the first Town House-class upgrade opportunity from `FISHING_COLLECTION_HOUSING_MARKET.md`.

Target vacant authored properties:

- 1–2 Small Cottage-class regional alternatives where map space allows;
- **2 Town House-class properties** around the ~9,000 Gold baseline;
- at least one visibly offers more interior/display room than the R01 starter cottage.

The player owns **one residence at a time**. Buying another property uses the authoritative trade-up/resale/migration flow in `FISHING_COLLECTION_HOUSING_MARKET.md`; older wording suggesting simultaneous multi-property ownership is obsolete and must not be implemented.

Housing is optional.

---

# 6. R02 gathering / economy package

Do not invent a huge second material tier. R02 adds a small number of visibly justified forest resources while keeping R01 materials relevant.

## 6.1 Persistent resources

### Hardwood

```text
yield: 2–3
personal respawn: 5 active min
Field Axe valid
```

R02 uses richer/denser authored nodes rather than a `Hardwood II` item.

### Healing Herb

Existing ordinary recovery herb remains available at lower density around river/clearings. No redundant `Forest Healing Herb` exists.

## 6.2 New resource — Forest Mushroom

External candidate: `Mushroom_Common.gltf` from the source-specific Quaternius Stylized Nature MegaKit Standard snapshot.

```text
player-facing working name: Forest Mushroom
yield: 1–2
personal respawn: 5 active min
tool: Harvest Knife / Sickle
primary uses: cooking, bounded alchemy/contract uses, ordinary sale
```

Do not make every mushroom color a separate material ID.

## 6.3 Tree-resin resource role

R02 uses one resin/sap material because it connects logging to bow/finesse equipment, furnishing and selected consumables.

```text
yield target: 1–2
personal respawn: ~8 active min
source: authored resin-bearing tree/scar nodes, not every tree block
uses: bow/string/handle treatment, selected furnishing/utility recipe, bounded alchemy use
```

Final player-facing name/model remains a pre-code asset-intake gate. Internal role names are never player-facing.

## 6.4 Mana-active flora role

Use a distinct accepted plant model from the Stylized Nature family; `Plant_7.gltf` remains a current candidate pending visual acceptance.

```text
yield: 1
personal respawn: 10 active min
locations: deep grove / sanctum approaches
tool: Harvest Knife / Sickle
uses: WIL/Focus/alchemy/support regional recipes
```

Final player-facing name is closed during pre-code visual intake, not during implementation.

## 6.5 No new profession

R02 does not add Carpentry or Foraging as another mastery tree.

---

# 7. R02 fishing package

R02 is the first region where fishing should feel like a real optional collection route rather than only a tutorial possibility.

Use river bends, deeper pools, old docks and shaded tributaries as authored shoal spots.

R02 contributes to the **global 36–48 species launch roster** rather than receiving a mandatory identical quota. Expected local composition after model/ecology review is roughly 4–5 identities, with shared species allowed where ecology fits.

Current external model pool contains several Quaternius animated-fish candidates. Final species names are locked only after side-by-side model inspection in the pre-code content pass.

R02 fishing uses the canonical Fish Codex, personal-best size, Trophy band, sell/cook/display loop, ordinary-fish `Grilled Catch` option and no fishing currency.

---

# 8. R02 ecology / spawn density

Normal daylight composition target:

- Raccoon/Crow: ambient/common near roads/hamlet;
- Forest Wolf: low-density predator packs;
- Forest Spider: shaded/root/fallen-log ambusher;
- Grizzly: rare territorial hazard;
- Nature Spirit: uncommon/rare elite in grove-biased zones;
- Cave Centipede: underground/root cuts only;
- Regalhart: uncommon authored traces/sighting only where variant presentation supports it;
- R02 field guardian: authored boss controller only;
- Lich: never a natural forest spawn.

No dependency global-spawn table owns this region.

## 8.1 Pack-pressure limit

- only one nearby elite-class natural threat in an ordinary exploration pocket;
- predator packs remain small;
- territorial wildlife does not join unrelated fights from huge radii;
- boss/hunt controllers suppress conflicting natural elite spawns in their immediate arena/context.

Actual density is profiler/playtest tuned.

---

# 9. Forest Spider combat kit

```text
Lv: 8–10 by placement
role: common ambusher
reference Lv8 HP: 110
Defense: 11
MR: 8
Poise: 20
same-Lv active TTK target: ~2.8–3.2 s
```

## Root Pounce

```text
wind-up: 0.50 s
committed hop/lunge: <= 3.0 blocks
post-mitigation benchmark damage: 10%
guardable: true
perfect_guardable: true
Poison buildup: 20
recovery: 0.40 s
```

## Fang Follow-up

```text
wind-up: 0.32 s
damage: 8%
Poison buildup: 25
guard pressure: light
recovery: 0.30 s
```

No invisible wall-climb teleport; no web projectile unless a later accepted enemy variant and motion/VFX support a genuinely distinct kit; common packs cannot stunlock the player.

---

# 10. Forest Wolf combat kit

```text
Lv: 9–11
role: common mobile predator
reference Lv9 HP: 145
Defense: 13
MR: 8
Poise: 28
same-Lv active TTK: ~3.5–4.0 s
pack size baseline: 1–3
```

Behavior:

- bounded patrol/territory;
- slight pack spread;
- no region-wide chase;
- a lone distant wolf may disengage after failed pursuit.

### Quick Bite

```text
wind-up: 0.32 s
damage: 9%
guardable/perfect_guardable: true
recovery: 0.28 s
```

### Bounding Lunge

```text
wind-up: 0.60 s
committed movement: 4–5 blocks
damage: 18%
guard pressure: medium
perfect_guardable: true
recovery on miss: 0.65 s
```

No synchronized three-wolf heavy lunge on the same server tick; pack scheduler offsets heavy commitments.

---

# 11. R02 Grizzly / Cave Centipede reuse

Do not create new species variants solely to increase numbers.

Grizzly Lv10 reference:

```text
HP: ~470
Defense: 30
MR: 16
Poise: 62
active fight target: ~10–12 s if provoked
```

Cave Centipede Lv10 reference:

```text
HP: ~280
Defense: 25
MR: 14
Poise: 46
```

No new token/material exists unless a real recipe later justifies a physical resource.

---

# 12. Mature Nature Spirit — native R02 elite

```text
Lv: 11
role: elite
HP: 1,000
Defense: 36
MR: 54
Poise: 88
solo active TTK: ~23–25 s
```

Preserve Rooted Swipe, Earthen Ram, Living Shell, Bloom Quake and the melee/earth identity.

## R02 addition — Root Snare

```text
telegraph: 1.00 s
area radius: 2.2 blocks
damage: 8% benchmark HP
Snared: 1.25 s if hit
guardable: false
perfect_guardable: false
recovery: 0.70 s
reuse floor: 7 s
```

Actual server area matches visible roots. Root Snare cannot chain into an unavoidable Bloom Quake without a legal movement window. Living Shell continues to increase incoming poise damage.

Rewards follow elite equipment rules; no abstract Nature currency.

---

# 13. Regalhart in R02

- no common natural Regalhart spawn;
- authored tracks/sightings may occur;
- an ancient/mature hunt variant becomes production content only if the dependency/external asset path supports clear visual differentiation;
- if visual differentiation is weak, keep Regalhart as rare ecology/traces and do not create a stat-scaled repeat boss.

---

# 14. R02 field guardian hunt

Internal production ID:

```text
r02_grove_guardian
```

Never expose the internal ID to players.

Current visual candidate: Quaternius `Goleling Evolved`.

Reference combat target if the candidate passes visual/animation review:

```text
Lv: 13
role: field boss
HP: 6,800
Defense: 52
MR: 38
Poise: 205
solo active TTK target: ~145–155 s
```

Gameplay roles are locked:

1. quick close-range strike — ~10–12% HP, guardable;
2. wide committed double-arm/sweep sequence — 20–24% total;
3. forward committed rush/body movement — ~26%, strong miss punish;
4. ground-line/ground-ring signature — ~30%, >=1.10 s visible tell, unguardable and matched to external ground VFX;
5. <=45% pattern transition adds aftershock/sequence pressure, not flat +damage/+HP.

Constraints:

- no teleport;
- no random full-arena unavoidable AoE;
- model animation must support actual attack body language;
- if the final accepted model cannot support these roles convincingly, revise the kit in canon before implementation rather than faking motion.

## 14.1 Hunt discovery — continuous environmental trail

The old numeric `2 of 3 clues` discovery rule is **removed**. Do not restore it from earlier snapshots.

Discovery now uses one continuous environmental-reading sequence that is deliberately different from R01 Regalhart:

1. the player encounters one **major disturbance anchor** in the working forest — a damaged logging site, bridge support or large route obstruction visibly beyond ordinary wildlife damage;
2. that anchor exposes a physically continuous trail of displaced roots, broken soil, shifted stone and intermittent heavy impacts leading deeper from the working woods;
3. the trail may branch around terrain, but each legitimate continuation must be readable from the previous segment by world geometry/environmental damage rather than a quest counter;
4. entering the guardian territory records the hunt location and broad map search area;
5. finding the guardian before the disturbance/trail sequence is always valid and records discovery immediately.

There is **no numeric clue count** and no requirement to click arbitrary evidence props. The player is reading a sustained physical path through the forest.

The map may show a broad search area after the major disturbance is found. It never reveals an exact boss GPS pin before territory/boss discovery.

Internal personal state may use semantic flags such as:

```text
guardian_disturbance_found
guardian_trail_followed
guardian_territory_found
guardian_discovered
```

These are never player-facing.

## 14.2 Reward contract

First eligible defeat:

- one guaranteed Superior+ normal equipment roll;
- 2 signature materials tied to the accepted final model identity;
- 15% direct Mythic roll from the final guardian signature pool;
- EXP ~20% current next-Lv requirement;
- Class XP ~15% current Class Rank requirement.

Repeat follows global field-boss rules.

Signature material/Mythic player-facing names remain a pre-code visual-identity gate and cannot ship as generic `Guardian Token`/internal labels.

---

# 15. R02 woodland sanctum dungeon

Target first-clear active length:

```text
18–24 minutes
```

Architecture:

```text
forest exterior
→ collapsed arboretum court
→ root-breached archive / ritual galleries
→ opened shortcut
→ inner sanctum
```

Primary direction: accepted Quaternius ruins/dungeon architecture, Stylized Nature overgrowth, Fantasy Props where appropriate, and no imported vanilla spawner encounters.

## Room 1 — Arboretum Court

- living forest → abandoned ordered garden/sanctum transition;
- one common group + environmental clue;
- optional mushroom/flora cache;
- target 2–3 min.

## Room 2 — Root Archive

- shelves/ruins split by roots;
- one Mature Nature Spirit elite or equivalent authored guardian;
- optional lore + rare regional resource side branch instead of another trash pack;
- target 3–4 min.

## Room 3 — Broken Cloister / River Channel

- shallow-water/bridge visual break;
- short traversal + compact combat;
- persistent-in-run shortcut toward entrance;
- no prolonged swimming combat.

## Room 4 — Sepulchral Gallery

- first explicit undead/sanctum presentation;
- 1–2 weak shade actors/equivalent;
- previews Lich reinforcement language without spawning Liches as trash.

## Room 5 — Inner Sanctum

Lich arena with open central movement space, 3–4 orientation pillars/roots, readable external ground-sigil telegraphs, visible authored reinforcement anchors and no arbitrary terrain destruction.

---

# 16. Lich — R02 dungeon boss

Dependency visual identity: Threateningly Mobs Continued `Lich`, dependency-only.

```text
Lv: 14
HP: 6,800
Defense: 33
MR: 60
Poise: 170
solo active TTK target: ~140–150 s
```

The Lich is a caster/controller with bounded reinforcement.

## Soul Bolt

```text
wind-up: 0.45–0.55 s
projectile damage: 10% benchmark HP
channel: magic
guardable: true
perfect_guardable: true
projectile_reflectable: only if final runtime/visual supports it cleanly
recovery: ~0.30 s
```

## Grave Line

```text
telegraph: 0.90 s visible floor lane
lane width: ~1.5 blocks
length: 10–12 blocks
damage: 20%
channel: magic
guardable: false
perfect_guardable: false
recovery: 0.70 s
```

## Withered Sigil

```text
telegraph: 1.20 s ground circle
radius: 3.0 blocks
damage: 26%
channel: magic
guardable: false
perfect_guardable: false
negative effect: brief authored Snared/slow-space pressure only if accepted VFX clearly communicates it
recovery: 0.80 s
```

## Sanctum Call

Trigger points:

```text
first cast: <=70% HP
second cast: <=35% HP
```

Each cast:

- >=1.20 s visible channel;
- at most 2 Sanctum Shade-role adds from authored anchors;
- if 2 valid shades already live, use another attack instead of exceeding cap;
- Lich is not invulnerable during channel.

Shade target:

```text
Lv: 13–14
HP: 120–150
Poise: ~18–22
role: fragile add
```

No vanilla zombie/skeleton reinforcement.

## <=45% HP pattern change

- Grave Line may gain one separately telegraphed offset follow-up;
- Soul Bolt may become a short two-shot rhythm with readable interval;
- Withered Sigil may predict movement more aggressively;
- no flat stat steroid.

Status relations:

- Poison buildup immune;
- Bleeding strongly resistant;
- Frostbite resistant;
- Shocked neutral;
- Fire direct damage weak 1.15x;
- normal Defense/MR still applies.

First-clear boss layer:

- guaranteed Superior+ normal equipment;
- 2 Lich signature materials;
- 15% direct Mythic/signature roll;
- boss Class XP ~10% current Class Rank requirement.

Dungeon completion layer:

- choose 1 of 3 curated Superior Item Lv14 rewards;
- completion EXP 50% current next-Lv + boss contribution;
- completion Class XP 32% + boss contribution;
- Gold 260.

Repeat:

- global dungeon-repeat equipment rules;
- 1 signature material;
- 15% Mythic roll;
- repeat EXP 22% + boss contribution;
- Class XP 15% completion + boss contribution.

No generic `Lich Token` ships.

---

# 17. R02 dungeon first-clear choice

Gameplay roles are locked; exact item identities/models are selected in pre-code R02 asset intake:

1. bow/finesse weapon — Superior Item Lv14, DEX + Critical/weak-point or Attack Speed identity;
2. WIL-focused light equipment/accessory — Superior Item Lv14, WIL + status handling / Mana sustain;
3. nature/forest utility off-hand/accessory — Superior Item Lv14, movement/status/guard/support utility rather than another pure DPS weapon.

Rules:

- at least two different equipment slots/families across the three;
- no class-exclusive choice labels;
- exact names bind only after accepted model/icon intake, before implementation.

---

# 18. R02 ordinary loot identity

Weight regional pools toward:

- bows / daggers / light swords / spear-finesse where accepted models exist;
- medium armor / ranger-traveler visual language;
- light/WIL clothing/accessories;
- poison/status handling;
- modest movement utility;
- Mana/resource utility;
- forest/nature-linked accessory visuals.

No universal green `Forest Set`.

Global source rules remain:

- ordinary enemies: no routine equipment;
- elites: 30% one roll;
- dangerous POI/rare chest: targeted regional gear;
- field/dungeon bosses: guaranteed normal gear + signature path;
- merchants: regional rotating subsets.

---

# 19. R02 regional quest flow

R02 remains discoverable without a board quest.

On first hamlet arrival, foreground at most:

```text
1 regional/main lead
+ 2 optional contracts
```

## Regional lead — Forks Beneath the Boughs

Purpose:

- establish unsafe/missing forest routes;
- lead naturally from hamlet → deep grove → ruin signs → sanctum without a straight corridor.

First phase completes by any 3 distinct actions from its authored set, such as:

- inspect an abandoned logging marker;
- resolve a wolf/spider hazard near a route;
- discover the split river crossing;
- inspect Nature Spirit damage/trace;
- recover a route ledger/cargo cache;
- find the first ruined-sanctum marker.

Repeating one category does not count multiple times.

Reward target:

```text
EXP: ~25–30% current next-Lv requirement
Gold: 130–160
Class XP: ~18–22%
```

## Optional contract — River Ledger

- discover one R02 fishing pool and catch an allowed ordinary regional fish category;
- first catch can simultaneously unlock its Codex entry;
- no rare/signature fish requirement.

```text
EXP: ~20%
Gold: ~110
Class XP: ~12–15%
```

## Optional contract — Resin and Rot

- gather/inspect a small amount of the accepted resin role and Forest Mushroom/plant material;
- reveals an actual production/trade use;
- no mass gathering requirement;
- does not ship until resin visual/name intake is complete.

## Guardian hunt discovery

Uses the **continuous disturbance → environmental trail → guardian territory** flow in §14.1. There is no clue counter and no `2 of 3` requirement.

## Sanctum discovery

The dungeon receives an exact marker only after the player physically discovers the ruin approach or reaches the appropriate regional-investigation step. No arbitrary key item gates the door unless the accepted structure gives the interaction a real world function.

---

# 20. Dynamic events

Use 2–3 small event families rather than MMO spam.

### Fallen Timber Route

Shared physical obstruction/wildlife pressure around a road; threat/interactions contribute; personal participation rewards.

### River Cargo Trouble

Trade cargo around a ford/dock; supports combat + interaction participation.

### Grove Disturbance

Temporary Nature Spirit/forest-threat event in an authored pocket; elite cap prevents accidental raid density; not the field-boss encounter.

Events follow `QUEST_WORLD_STATE.md` and require no pre-accept.

---

# 21. R02 housing/fishing collection integration

Fishing:

- local fish enter Fish Codex;
- regional rare fish may become a trophy/display candidate after exact model intake;
- buyer/inn gives immediate Gold/cooking use;
- no fisherman currency.

Housing:

- Town House-class properties provide visible midgame aspiration;
- regional decor uses accepted external assets;
- decor uses Gold/material/collection routes, not another currency;
- one-residence-at-a-time ownership remains authoritative;
- property purchase is never required for regional completion.

A displayed trophy fish grants no combat stat.

---

# 22. R02 sound / VFX direction

External/verified layers are required for broad forest wind/leaves, river water, sparse wildlife, logging/hamlet work, deep-grove tonal shift and sanctum room tone.

Combat:

- Wolf/Spider get creature-specific attack/hit/death sounds;
- Nature Spirit uses heavier body/earth/root impacts;
- field guardian gets dedicated signature set after final model acceptance;
- Lich gets distinct cast onset, lane/sigil telegraphs and reinforcement cue;
- unguardable ground attacks must warn differently from ordinary Soul Bolt.

VFX hit areas match server hit areas. No generic green cloud=`nature` or purple cloud=`lich` shortcut.

---

# 23. Data/state ownership

Suggested data layout:

```text
regions/r02/
  region
  subareas
  spawn_tables
  landmarks
  fishing_spots
  resources
  merchants
  housing

encounters/r02/
  forest_spider
  forest_wolf
  grizzly
  cave_centipede
  mature_nature_spirit
  grove_guardian
  lich
  sanctum_shade

dungeons/r02_sanctum/
  rooms
  encounters
  shortcut
  rewards

quests/r02/
  regional_chain
  river_contract
  resource_contract
  guardian_hunt

assets/source_bindings/
  r02_*
```

Server authority owns encounter levels/stats, spawns, hit validation, resources/fishing state, quest progress, rewards, Gold/items and property ownership. Client presentation never awards/advances these states.

---

# 24. Performance constraints

- no every-tick whole-forest search;
- loaded-chunk spawn tables only;
- ambient wildlife bounded;
- decorative models selective, not one display entity per grass tuft;
- fishing spots use lightweight authored markers/state;
- field-boss controller activates near authored hunt area/eligible players only;
- dungeon controller scopes logic to active run/nearby players;
- Lich adds hard-capped;
- hamlet NPCs use short anchor/patrol zones.

Profiler decides final density limits.

---

# 25. R02 asset-intake gates

Before R02 player-visible implementation, close at least:

1. acquire/hash/visually accept Wolf candidate;
2. acquire/hash/visually accept Spider candidate;
3. acquire/hash/inspect `Goleling Evolved` or replace with a superior guardian candidate;
4. inspect exact guardian animation clips before final attack binding;
5. confirm Threateningly dependency/license/runtime boundary;
6. inspect Lich model/animation/reinforcement presentation;
7. acquire/hash selected Stylized Nature files;
8. accept resin model and lock its player-facing name;
9. accept mana-flora model and lock its player-facing name;
10. choose exact R02/global fish models applicable here and lock species/size/value tables;
11. accept hamlet compositions and two Town House-class shells;
12. accept sanctum room/prop family;
13. accept field-guardian/Lich VFX + sound families;
14. accept the three dungeon first-clear item model/icon bindings;
15. accept signature-material/Mythic visual identities before naming them.

Missing external bindings remain source-bootstrap blockers, not implementation-time choices.

---

# 26. First R02 playtest acceptance

## Exploration

- river/road/landmarks orient without arrow spam;
- forest feels denser than R01 without constant Trail Stag collision on main routes;
- side routes reveal meaningful content rather than dead ends;
- guardian discovery reads as a continuous physical trail and never as a numeric clue checklist.

## Combat

- Forest Spider dies quickly but can punish ignored poison pressure;
- Wolf packs pressure without synchronized unavoidable lunges;
- Nature Spirit visibly extends the R01 learned kit;
- field guardian reads as a new important creature rather than scaled Nature Spirit;
- Lich reinforcement adds a tactical decision without turning the fight into prolonged add cleanup;
- Lich telegraphs/hitboxes match.

## Economy / side loops

- gathering feeds real recipes/orders;
- fishing catches update Codex and sell/cook correctly;
- hamlet reduces unnecessary travel without replacing Alderford;
- Town House preview communicates the next housing step and never implies simultaneous residence ownership.

## Dungeon

- first clear ~18–24 minutes;
- shortcut prevents full trash replay after boss death;
- first-clear choice useful and visually distinct;
- no vanilla spawners/UI/chests undermine presentation.

## Multiplayer

- personal node/fishing state independent;
- participation supports non-DPS roles;
- first-clear rewards personal/idempotent;
- physical housing vacancy cannot be double-purchased;
- no party-leader quest ownership.

---

# 27. Current verification state

- DESIGN REVIEWED AGAINST CURRENT MASTER CANON: YES
- REGION_CROSS_AUDIT R02 GUARDIAN-DISCOVERY REFINEMENT MERGED INTO THIS FILE: YES
- OLD R02 `2 OF 3 CLUES` RULE: REMOVED / NON-CANONICAL
- ONE-RESIDENCE HOUSING RULE EXPLICITLY REASSERTED: YES
- EXTERNAL SOURCE RESEARCH: YES
- THREATENINGLY CONTINUATION SOURCE/LICENSING CONFLICT RECORDED: YES
- R02 SPATIAL / SERVICE / COMBAT / DUNGEON MACRO FLOW LOCKED: YES
- R02 QUEST/NPC/SCENE AUTHORING AT R01 CLOSURE LEVEL: YES — CLOSED BY `R02_CONTENT_BIBLE.md`
- EXACT ALL R02 ASSET FILES HASHED/ACCEPTED: NO
- FIELD GUARDIAN FINAL MODEL: CANDIDATE ONLY (`Goleling Evolved`)
- R02 FISH FINAL ROSTER: NO — model-first intake required
- RESIN / MANA-FLORA PLAYER-FACING ASSET BINDING: NOT YET
- CODE REVIEWED: NOT APPLICABLE — no gameplay code changed
- TESTED: NO
- BUILD VERIFIED: NO
- JAR PRODUCED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

This is a design/provenance package. No build/CI is warranted for this docs-only work.