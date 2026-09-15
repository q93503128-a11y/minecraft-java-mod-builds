# Open-World RPG — R02 Western Forest Implementation Package

> Status: **DESIGN CANON — R02 world/combat/service/reward flow is implementation-ready; exact external asset file bindings remain gated by intake where explicitly marked**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Region graph: `REGIONS.md`  
> Combat: `COMBAT_BALANCE.md`, `STATUS_AND_R01_ENCOUNTERS.md`  
> Loot/equipment: `LOOT_ECONOMY.md`, `EQUIPMENT_BALANCE.md`  
> Field systems: `GATHERING_FISHING_CAMP_HOUSING.md`, `FISHING_COLLECTION_HOUSING_MARKET.md`  
> Quest/state: `QUEST_WORLD_STATE.md`  
> UI: `UI_DIRECTION.md`  
> External provenance: `EXTERNAL_SOURCES.md`  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins. It refines the existing R02 section of `REGIONS.md`; it does not reopen R02's Lv/terrain/role decisions.

This package turns the already-canonical R02 western rich forest / river basin into a region that can be implemented without inventing its flow during coding.

The region's core play sentence is:

```text
leave the open R01 heartland
→ enter a denser forest where roads split and landmarks hide behind terrain
→ discover a compact logging/trade hamlet
→ follow river, timber and ruin clues instead of a single corridor
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

These filenames are **candidate bindings**, not automatic acceptance. Exact locally acquired package, license evidence, SHA-256, Minecraft scale and material appearance must still be recorded before committing raw bytes.

R02 uses the family mainly for:

- readable forest nodes;
- mushroom/plant resource presentation;
- twisted/deep-forest silhouettes;
- ruin-overgrowth dressing;
- field-boss arena framing.

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
- it may be reconsidered for a future dedicated mushroom/fungal pocket if the actual Azari terrain supports one.

This is candidate rejection, not content deletion.

## 2.4 Nature Spirit / Lich dependency boundary

Current R02 uses `Nature Spirit` and `Lich` from Threateningly Mobs Continued as **dependency-only** entities.

License evidence remains conflicted across current storefront metadata:

- continuation CurseForge listing displays All Rights Reserved;
- other metadata/pages have displayed MIT lineage/status;
- the original older project has MIT listings.

Therefore:

- normal installed dependency use may proceed when terms/runtime allow;
- do not copy continuation model/code/assets into the public repository until exact upstream/source licensing is reconciled;
- project-owned stats, regional spawning, attacks/rewards/state remain canonical regardless of donor defaults.

The original/current source history supports Lich having a reinforcement-oriented skill identity. R02 deliberately preserves that design clue instead of turning the boss into a generic projectile sponge.

## 2.5 R02 field-boss visual candidate

The field guardian must not be merely a larger/recolored R01 Nature Spirit.

Primary current candidate:

- Quaternius **`Goleling Evolved`** from the Ultimate Monsters family / creator model distribution;
- animated external model; current downstream/source evidence identifies it as a Quaternius model and the Ultimate Monsters family is published as CC0.

Status:

```text
VISUAL CANDIDATE — NOT YET FINAL PLAYER-FACING NAME
```

Before final binding:

1. acquire exact model from a source with preserved license evidence;
2. inspect silhouette at Minecraft scale;
3. inspect available attack/run/hit/death animation clips;
4. verify the model reads as an ancient forest/stone guardian without relying on a bad recolor;
5. only then lock the player-facing guardian name and exact attack animation bindings.

Fallback order if the model fails:

1. another distinct animated CC0 Quaternius Ultimate Monsters guardian/golem model;
2. another legally redistributable external forest-guardian model of equal/higher quality;
3. **not** `Nature Spirit x 1.8 scale + green particles`.

## 2.6 Lich reinforcement visual

If the dependency's own reinforcement actor cannot be cleanly normalized to project encounter rules, use an accepted external animated ghost/shade model such as Quaternius Ultimate Monsters `Ghost` as a project-owned **Sanctum Shade** presentation.

This is a fallback, not permission to mix random undead packs.

---

# 3. Spatial progression / local encounter pressure

R02 Suggested Entry Lv stays **8**.

Use local pressure instead of treating the region as a flat Lv8 zone:

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

Dense forest can become annoying if every direction looks the same. R02 requires authored visual orientation.

Use at least these landmark classes when Azari inspection permits:

1. **main river** — persistent macro-navigation spine;
2. **logging road** — broadest safe-ish route from R01 into hamlet;
3. **split river ford / bridge** — memorable decision point;
4. **tall twisted-tree grove** — deep forest silhouette visible through canopy gaps;
5. **ruined sanctum tower/arch** — glimpsed before full dungeon discovery;
6. **mountain/ridge sightline** — signal toward R03;
7. **one dangerous high-ground/stone clearing** — guardian-hunt territory.

Rules:

- do not solve navigation with permanent floating arrows;
- paths can be narrower than R01, but Trail Stag remains usable on the main routes;
- side paths may require dismount/foot exploration when that reveals real content;
- repeated fallen-log jumping is not a traversal gimmick;
- river crossings are deliberately placed so water does not become constant movement friction.

---

# 5. R02 logging/trade hamlet

The R02 hamlet is a **secondary regional hub**, smaller than the starting settlement.

Target physical population at daytime:

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

Reason:

- the hamlet must be useful enough that visiting it matters;
- the starting settlement must still remain a meaningful long-term hub.

Basic regional recipes/orders may be handed in here when they do not require full forge/alchemy infrastructure.

## 5.2 Housing

R02 visibly introduces the **first Town House-class upgrade opportunity** from `FISHING_COLLECTION_HOUSING_MARKET.md`.

Target vacant authored properties:

- 1–2 Small Cottage-class regional alternatives where map space allows;
- **2 Town House-class properties** around the ~9,000 Gold baseline;
- at least one should visually offer more interior/display room than the R01 starter cottage.

The player still owns only one residence at a time and uses atomic trade-in/move behavior.

Housing is optional. The player does not need to buy a forest house to use the hamlet or finish the region.

---

# 6. R02 gathering / economy package

Do not invent a huge second material tier. R02 adds a small number of visibly justified forest resources while keeping R01 materials relevant.

## 6.1 Persistent resources

### Hardwood

Existing R01 resource remains common and useful.

```text
yield: 2–3
personal respawn: 5 active min
Field Axe valid
```

R02 contains richer/denser authored nodes rather than a new `Hardwood II` item.

### Healing Herb

Existing ordinary recovery herb remains available at lower density around river/clearings.

No redundant `Forest Healing Herb` is introduced merely for region color.

## 6.2 New resource — Forest Mushroom

Production direction:

- external exact candidate: `Mushroom_Common.gltf` from the source-specific Quaternius Stylized Nature MegaKit Standard snapshot;
- readable ordinary name is acceptable because the resource is intentionally ordinary.

Baseline:

```text
player-facing working name: Forest Mushroom
yield: 1–2
personal respawn: 5 active min
tool: Harvest Knife / Sickle
primary uses: cooking, bounded alchemy/contract uses, ordinary sale
```

Do not make every mushroom color a separate material ID.

Final model/texture acceptance still requires intake review.

## 6.3 New resource role — tree resin

R02 needs one resin/sap material because it connects logging to bow/finesse equipment, furnishing and selected consumables.

Design role:

```text
yield target: 1–2
personal respawn: ~8 active min
source: authored resin-bearing tree/scar nodes, not every tree block
uses: bow/string/handle treatment, selected furnishing/utility recipe, bounded alchemy use
```

**Player-facing final name/model is not locked yet.**

Reason:

- the exact external resin/bottle/glob model has not yet passed intake;
- external-first rules prohibit locking an item just because the mechanic needs a noun.

Internal data may temporarily call the role `r02_tree_resin_role`, never player-facing.

## 6.4 New resource role — mana-active flora

Use a distinct accepted plant model from the Stylized Nature family; `Plant_7.gltf` is a current exact filename candidate but has not been visually accepted.

Target behavior:

```text
yield: 1
personal respawn: 10 active min
locations: deep grove / sanctum approaches, not roadside spam
tool: Harvest Knife / Sickle
uses: WIL/Focus/alchemy/support-related regional recipes
```

Final player-facing name waits for visual review.

## 6.5 No new profession

R02 does **not** add Carpentry or Foraging as another giant mastery tree.

Furniture/trade orders can consume wood/resin through existing economy/service rules without inventing another profession bar.

---

# 7. R02 fishing package

R02 is the first region where fishing should feel like a real optional collection route rather than only a tutorial possibility.

Use river bends, deeper pools, old docks and shaded tributaries as authored shoal spots.

R02 codex target:

```text
4–5 fish identities
```

Composition target:

- 2 common river/pond catches;
- 1 uncommon deep-pool/tributary catch;
- 1 rare regional catch;
- optional fifth shared fish overlapping R01/R03 water where ecology fits.

Current external model pool already contains several Quaternius CC0 animated fish candidates, including generic Fish variants, Goldfish/Blue Goldfish and other distinct silhouettes. **Do not lock the final R02 species names until the actual models are inspected side-by-side.**

R02 fishing uses the canonical:

- Fish Codex;
- personal-best size;
- Trophy band;
- sell/cook/display loop;
- ordinary fish → `Grilled Catch` option;
- no fishing currency.

Rare R02 catch conditions may prefer shaded/deep forest pools or a broad time/weather weight, but no required main progression waits for a narrow weather window.

---

# 8. R02 ecology / spawn density

The forest must still read as ecology, not continuous combat.

Normal daylight composition target:

- Raccoon/Crow: ambient/common near roads/hamlet;
- Forest Wolf: low-density predator packs, primarily deeper from services;
- Forest Spider: shaded/root/fallen-log ambusher;
- Grizzly: rare territorial hazard;
- Nature Spirit: uncommon/rare elite in authored grove-biased zones;
- Cave Centipede: underground/root cuts only;
- Regalhart: uncommon authored hunt/trace occurrence only where external variant presentation actually supports it;
- R02 field guardian: authored boss controller only;
- Lich: **never a natural forest spawn**; sanctum/ruin encounter only.

No default dependency global-spawn table owns this region.

## 8.1 Pack pressure limit

Ordinary loaded forest should avoid accidental `wolf pack + spider + Nature Spirit + grizzly` dogpiles.

Regional spawn controller should enforce local role pressure such as:

- only one nearby elite-class natural threat in an ordinary exploration pocket;
- predator packs remain small;
- territorial wildlife does not join unrelated fights from huge radii;
- boss/hunt controllers suppress conflicting natural elite spawns inside their immediate arena/context.

Actual density is profiler/playtest tuned.

---

# 9. Forest Spider combat kit

Working project identity: **Forest Spider** until the external model is inspected; a stronger lore name is not required for an ordinary animal-scale enemy.

```text
Lv: 8–10 by placement
role: common ambusher
reference Lv8 HP: 110
Defense: 11
MR: 8
Poise: 20
same-Lv active TTK target: ~2.8–3.2 s
```

## Attack — Root Pounce

Use the external jump/attack animation rather than inventing ranged web spam.

```text
wind-up: 0.50 s
committed hop/lunge: <= 3.0 blocks
post-mitigation benchmark damage: 10%
guardable: true
perfect_guardable: true
Poison buildup: 20
recovery: 0.40 s
```

## Attack — Fang Follow-up

Only after reaching close range.

```text
wind-up: 0.32 s
damage: 8%
Poison buildup: 25
guard pressure: light
recovery: 0.30 s
```

Rules:

- no invisible wall-climb teleport;
- no web projectile unless later external motion/VFX inspection justifies a distinct enemy variant;
- two/three spiders may pressure positioning, but common packs cannot stunlock the player.

Rewards:

- ordinary common EXP/Gold budget;
- no routine equipment;
- no new `Spider Fang` material unless a later real recipe needs it.

---

# 10. Forest Wolf combat kit

External source candidate: Quaternius animated Wolf CC0.

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

- patrol/roam in bounded forest territory;
- packs spread slightly rather than occupy exactly one hitbox;
- do not chase through the entire region;
- if only one wolf remains far from territory/pack after a failed pursuit, it may disengage instead of behaving like a zombie.

## Quick Bite

```text
wind-up: 0.32 s
damage: 9%
guardable/perfect_guardable: true
recovery: 0.28 s
```

## Bounding Lunge

```text
wind-up: 0.60 s
committed movement: 4–5 blocks
damage: 18%
guard pressure: medium
perfect_guardable: true
recovery on miss: 0.65 s
```

Pack rule:

- no synchronized three-wolf lunge on the same server tick;
- local pack attack scheduler offsets heavy commitments so the player can actually read them.

Rewards:

- common EXP/Gold/material budget only;
- no ordinary equipment;
- creature resource drops only if existing cooking/material design gives them a real use.

---

# 11. R02 Grizzly / Cave Centipede reuse

Do not create new species variants solely to increase numbers.

## Grizzly

R02 may place stronger territorial Grizzlies around Lv10–11 using the established R01 kit with source-Lv-scaled attack data.

Reference target at Lv10:

```text
HP: ~470
Defense: 30
MR: 16
Poise: 62
active fight target: ~10–12 s if provoked
```

This remains wildlife, not an elite loot pinata.

## Cave Centipede

R02 root cuts use the R01 physical/wall-climb kit at Lv10–12.

Reference target Lv10:

```text
HP: ~280
Defense: 25
MR: 14
Poise: 46
```

Do not add new poison-leg currency or global surface spawning.

---

# 12. Mature Nature Spirit — native R02 elite

R01 taught the basic identity. R02 makes Nature Spirit a native elite with one additional area-control decision rather than only higher HP.

Reference target:

```text
Lv: 11
role: elite
HP: 1,000
Defense: 36
MR: 54
Poise: 88
solo active TTK: ~23–25 s
```

Preserve:

- Rooted Swipe;
- Earthen Ram;
- Living Shell;
- Bloom Quake;
- melee/earth identity;
- no generic projectile spam during defense.

## R02 addition — Root Snare

The Spirit braces and causes a clearly visible external root/ground VFX at target ground.

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

Rules:

- area appears before resolution;
- actual server area matches visible roots;
- one Spirit cannot chain Snare directly into an unavoidable Bloom Quake before the player receives a legal movement window;
- Living Shell still increases incoming poise damage, preserving the learned R01 answer.

Status relations remain broadly R01-like:

- Poison strongly resistant;
- Fire direct damage weak;
- MR higher than Defense.

Rewards:

- elite 30% equipment-roll rule;
- regional mushroom/flora/material output only when source tables justify it;
- no new abstract Nature currency.

---

# 13. Regalhart in R02

R02 should acknowledge that Regalhart is a forest-capable species without making the R01 boss meaningless.

Baseline:

- no common natural Regalhart spawn;
- occasional authored tracks/sightings may appear;
- a stronger `ancient/mature` hunt variant becomes production content **only if the dependency or accepted external asset path supports clear visual differentiation** such as genuinely different antler/body presentation;
- if visual differentiation is weak, keep Regalhart as rare ecological sighting/traces and do not create a second stat-scaled boss.

This decision is deliberately quality-gated.

---

# 14. R02 field guardian hunt

Internal production ID:

```text
r02_grove_guardian
```

Do not expose this internal name to players.

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

Exact attack names/player-facing boss name wait for model/animation inspection, but gameplay roles are locked:

1. **quick close-range strike** — ~10–12% HP, guardable;
2. **wide committed double-arm/sweep sequence** — 20–24% total, readable follow-up;
3. **forward committed rush/body movement** — ~26%, strong miss punish;
4. **ground-line/ground-ring signature** — ~30%, >=1.10 s visible tell, unguardable and matched to external ground VFX;
5. **<=45% pattern transition** — adds aftershock/sequence pressure, **not** flat +damage/+HP.

Constraints:

- no teleport;
- no random full-arena unavoidable AoE;
- model animation must support the actual attack body language;
- if the model's available animations cannot support these roles convincingly, redesign the kit around its real clips **before implementation** rather than faking motions.

## Hunt discovery

The field boss is discovered through **2 of 3** clue types, structurally similar to Regalhart but with different physical language:

- split/broken old stone marker or ruin segment;
- deeply displaced roots/soil around a clearing;
- heavy impact marks on abandoned logging equipment/bridge support.

Any 2 clues reveal a broad search area, not a precise GPS pin.

Finding the boss first remains valid.

## Reward contract

First eligible defeat:

- one guaranteed Superior+ normal equipment roll;
- 2 signature materials tied to the **accepted final model identity**;
- 15% direct Mythic roll from the final guardian signature pool;
- EXP ~20% current next-Lv requirement;
- Class XP ~15% current Class Rank requirement.

Repeat follows global field-boss rules.

**Do not name the signature material or Mythic before the accepted model gives it a real visual identity.**

---

# 15. R02 woodland sanctum dungeon

Target first-clear active length:

```text
18–24 minutes
```

It should be modestly denser/more branching than R01 but not become a maze.

Architecture:

```text
forest exterior
→ collapsed arboretum court
→ root-breached archive / ritual galleries
→ opened shortcut
→ inner sanctum
```

Primary external visual direction:

- Quaternius Ultimate Modular Ruins / Modular Dungeon architecture;
- Stylized Nature MegaKit overgrowth;
- Fantasy Props books/statues/candles/chests/furniture where accepted;
- no imported vanilla spawner encounters.

## Room 1 — Arboretum Court

Purpose:

- transition from living forest to abandoned ordered garden/sanctum;
- one common group + environmental clue;
- optional mushroom/flora cache.

Target room time: ~2–3 min.

## Room 2 — Root Archive

Purpose:

- shelves/ruins split by tree roots;
- one Mature Nature Spirit elite or equivalent authored guardian;
- optional side branch with lore + rare regional plant/resource rather than another trash pack.

Target room time: ~3–4 min.

## Room 3 — Broken Cloister / River Channel

Purpose:

- shallow water/bridge path gives a visual break;
- short traversal and one compact combat setup;
- opens a persistent-in-run shortcut back toward the entrance.

Do not require prolonged swimming combat.

## Room 4 — Sepulchral Gallery

Purpose:

- first explicit undead/sanctum corruption presentation;
- 1–2 weak shade actors or equivalent external/dependency enemies;
- introduces the Lich's reinforcement visual language before the boss without spawning Liches as trash.

## Room 5 — Inner Sanctum

Lich arena.

Arena supports:

- open central movement space;
- 3–4 readable pillars/roots for orientation, not permanent projectile cheese;
- external ground-sigil telegraph readability;
- reinforcement spawns from visible authored anchors;
- no arbitrary terrain destruction.

---

# 16. Lich — R02 dungeon boss

Dependency visual identity: Threateningly Mobs Continued `Lich`, dependency-only.

Project-normalized target:

```text
Lv: 14
HP: 6,800
Defense: 33
MR: 60
Poise: 170
solo active TTK target: ~140–150 s
```

The Lich is a **caster/controller with bounded reinforcement**, not a stationary projectile turret.

## Attack 1 — Soul Bolt

Working gameplay label; final VFX/name may be refined after dependency animation inspection.

```text
wind-up: 0.45–0.55 s
projectile damage: 10% benchmark HP
channel: magic
guardable: true
perfect_guardable: true
projectile_reflectable: only if final runtime/visual supports it cleanly
recovery: ~0.30 s
```

Projectile speed must allow a real side-step/dodge read at ordinary arena distance.

## Attack 2 — Grave Line

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

No hidden extension beyond the decal.

## Attack 3 — Withered Sigil

```text
telegraph: 1.20 s ground circle
radius: 3.0 blocks
damage: 26%
channel: magic
guardable: false
perfect_guardable: false
negative effect: brief authored Snared/slow-space pressure only if the accepted VFX clearly communicates it
recovery: 0.80 s
```

This is movement pressure, not long hard crowd control.

## Reinforcement — Sanctum Call

Preserve the donor Lich's reinforcement identity in a bounded project form.

Trigger points:

```text
first cast: at/below 70% HP
second cast: at/below 35% HP
```

Each cast:

- >=1.20 s visible channel;
- spawns at most 2 `Sanctum Shade`-role adds from authored anchors;
- if 2 valid shades are already alive, the summon is skipped/replaced by a non-summon attack rather than exceeding the cap;
- Lich is **not invulnerable** during the channel;
- killing shades is useful but not mandatory if the player can manage space.

Shade target:

```text
Lv: 13–14
HP: 120–150
role: fragile common/add
Poise: ~18–22
```

Use dependency reinforcement actor if its presentation fits. Otherwise bind one coherent accepted external animated ghost/shade model.

No vanilla zombie/skeleton reinforcement.

## Phase change — <=45% HP

No stat steroid.

Pattern changes:

- Grave Line may be followed by one separately telegraphed offset lane;
- Soul Bolt may appear as a short 2-shot rhythm with a readable interval;
- Withered Sigil may target predicted movement more aggressively;
- second reinforcement threshold remains bounded as above.

No permanent +20% damage/haste simply because HP reached 45%.

## Status relations

Visually justified undead resistance:

- Poison buildup: immune;
- Bleeding buildup: strongly resistant;
- Frostbite: resistant;
- Shocked: neutral;
- Fire direct damage: weak (1.15x);
- physical/magic mitigation still uses normal Defense/MR.

Hard immunity is used only for Poison because the accepted undead identity genuinely lacks a normal toxin biology.

## Reward contract

First eligible clear — boss layer:

- guaranteed Superior+ normal equipment;
- 2 Lich signature materials;
- 15% direct Mythic/signature roll;
- boss Class XP ~10% current Class Rank requirement.

Dungeon completion layer:

- choose one of **3 curated Superior Item Lv14** region/dungeon rewards;
- completion EXP: **50%** current next-Lv requirement + boss contribution;
- completion Class XP: **32%** + boss contribution;
- Gold baseline target: **260 Gold**.

Repeat:

- global dungeon-repeat equipment rules;
- 1 signature material;
- 15% Mythic roll;
- repeat EXP 22% + boss contribution;
- Class XP 15% completion + boss contribution.

The signature material's final name/model remains blocked on exact external/dependency visual intake. Do not ship `Lich Token`.

---

# 17. R02 dungeon first-clear choice

Gameplay roles are locked; exact player-facing item identities/model files are selected in R02 asset intake before implementation.

The three candidates must be visually distinct and support the region's reward identity:

1. **bow/finesse weapon candidate**
   - Superior Item Lv14;
   - DEX + Critical/weak-point or Attack Speed identity;
   - accepted external grounded forest/fantasy bow model;
2. **WIL-focused light equipment/accessory candidate**
   - Superior Item Lv14;
   - WIL + status handling / Mana sustain;
   - accepted external apparel/accessory model/icon;
3. **nature/forest utility off-hand or accessory candidate**
   - Superior Item Lv14;
   - movement/status/guard or support utility rather than another pure DPS weapon;
   - accepted external model/icon.

Rules:

- at least two different equipment slots/families across the three;
- broad weapon freedom remains intact;
- no class-exclusive choice labels;
- exact names are locked only after model/icon intake.

This is intentionally stricter than inventing three cool names first and searching for art later.

---

# 18. R02 ordinary loot identity

Regional ordinary equipment pools should weight toward:

- bows / daggers / light swords / spear-finesse options where accepted models exist;
- medium armor / ranger-traveler visual language;
- light/WIL clothing/accessories;
- poison/status handling;
- modest movement utility;
- Mana/resource utility;
- forest/nature-linked accessory visuals.

Do not create a new universal `Forest Set` where every piece is simply green.

Target pool follows global source rules:

- ordinary enemies: no routine equipment;
- elites: 30% one roll;
- dangerous POI/rare chest: targeted regional gear;
- field boss/dungeon boss: guaranteed normal gear + signature path;
- merchants: regional rotating subsets rather than global pool.

---

# 19. R02 regional quest flow

R02 should be discoverable even if the player arrives without a board quest.

On first hamlet arrival, foreground at most:

```text
1 regional/main lead
+ 2 optional contracts
```

following the existing HUD/quest-density canon.

## Regional lead — working title: Forks Beneath the Boughs

Purpose:

- establish missing/unsafe forest routes;
- lead naturally from hamlet → deep grove → ruin signs → sanctum without a straight quest corridor.

First phase completes by any **3 distinct actions from an authored set**, for example:

- inspect an abandoned logging marker;
- resolve a wolf/spider hazard near a route;
- discover the split river crossing;
- inspect Nature Spirit damage/trace;
- recover a route ledger/cargo cache;
- find first ruined sanctum marker.

Do not require every action.

Reward target for the first regional milestone:

```text
EXP: ~25–30% current next-Lv requirement
Gold: 130–160
Class XP: ~18–22%
```

## Optional contract — River Ledger

Fishing/trade-facing contract:

- discover one R02 fishing pool and catch an allowed ordinary regional fish category;
- first catch can simultaneously unlock its Codex entry;
- no requirement for rare/signature fish.

Reward target:

```text
EXP: ~20%
Gold: ~110
Class XP: ~12–15%
```

## Optional contract — Resin and Rot

Gathering/exploration-facing contract:

- gather/inspect a small amount of the accepted resin-role and Forest Mushroom/plant material;
- may reveal an alchemy/cooking or furnishing/trade use;
- no mass-gather requirement.

If resin visual intake is not complete, do not ship this contract until the real item is admitted.

## Guardian hunt discovery

Uses the 2-of-3 clue rule in §14 and becomes a broad hunt entry only after meaningful discovery.

## Sanctum discovery

The dungeon receives an exact map marker only after the player physically discovers the ruin approach or reaches the appropriate regional investigation step.

No key item is required merely to open the door unless the selected external structure physically needs a meaningful short interaction.

---

# 20. Dynamic events

Use **2–3 small event families**, not constant MMO spam.

Candidate event roles:

### Fallen Timber Route

- shared physical obstruction / wildlife pressure around a road;
- players clear threat/interact with the route;
- personal participation rewards;
- no permanent world repair unless the authored event is explicitly a one-time world change.

### River Cargo Trouble

- recover/secure trade cargo around a ford/dock;
- supports combat + interaction participation;
- fishing/gathering is not required.

### Grove Disturbance

- temporary Nature Spirit / forest-threat event in an authored pocket;
- elite cap prevents accidental raid density;
- not the field-boss encounter.

Events use `QUEST_WORLD_STATE.md` participation/reward rules and do not require pre-accept.

---

# 21. R02 housing/fishing collection integration

R02 is where the side systems begin to feel like a persistent RPG world rather than isolated mechanics.

Fishing:

- local fish appear in the Fish Codex;
- one regional rare fish can become a trophy/display candidate after exact model intake;
- fish buyer/inn gives immediate Gold/cooking use;
- no separate fisherman currency.

Housing:

- Town House-class physical properties provide visible midgame aspiration;
- forest furniture variants may use accepted wood/plant/furniture external assets;
- regional decor is purchased/earned through Gold/material/collection routes, not a new currency;
- buying property is never required for regional completion.

A trophy fish displayed in an R02 house is a nice cross-system reward; it grants no combat stat.

---

# 22. R02 sound / VFX direction

Do not default important sounds to vanilla.

## Forest ambience

Need externally sourced/verified layers for:

- broad forest wind/leaves;
- river water;
- sparse bird/wildlife;
- logging/hamlet work;
- deep-grove tonal shift;
- sanctum stone/wood/room tone.

Keep layers restrained. Dense forest does not mean constant loud birds.

## Combat

- Wolf/Spider: creature-specific attacks/hit/death sounds matched to external visuals;
- Nature Spirit: body/earth/root impacts heavier than R01 common combat;
- field guardian: dedicated stone/wood/ground signature set after final model acceptance;
- Lich: distinct cast onset, lane/sigil telegraphs and reinforcement cue;
- an unguardable Lich ground attack must have a recognizably different warning from ordinary Soul Bolt.

VFX must use exact hit areas.

No generic green particle cloud = `nature`; no purple cloud = `lich` shortcut.

---

# 23. Data/state ownership

Suggested data layout:

```text
regions/r02/
  region.json
  subareas.json
  spawn_tables.json
  landmarks.json
  fishing_spots.json
  resources.json
  merchants.json
  housing.json

encounters/r02/
  forest_spider.json
  forest_wolf.json
  grizzly.json
  cave_centipede.json
  mature_nature_spirit.json
  grove_guardian.json
  lich.json
  sanctum_shade.json

dungeons/r02_sanctum/
  rooms.json
  encounters.json
  shortcut.json
  rewards.json

quests/r02/
  regional_chain.json
  river_contract.json
  resource_contract.json
  guardian_hunt.json

assets/source_bindings/
  r02_*.json
```

Server authority owns:

- encounter Lv/stats;
- spawns;
- boss phases/hit validation;
- resource/fish personal state;
- quest progress;
- dungeon first-clear/reward claim;
- Gold/items;
- property availability/ownership.

Client-only presentation never awards or advances these states.

---

# 24. Performance constraints

R02's density cannot be achieved through brute-force entities.

- no every-tick whole-forest search;
- loaded-chunk spawn tables only;
- ambient wildlife density remains bounded;
- external decorative flora models are used selectively for landmarks/nodes, not one display entity per grass tuft;
- fishing spots use lightweight authored markers/state, not hundreds of simulated fish entities;
- field boss controller activates only near its authored hunt area/eligible players;
- dungeon controller scopes logic to active run/nearby tracking players;
- Lich reinforcement hard-caps active adds;
- hamlet NPCs use short anchor/patrol zones rather than full-village pathfinding.

Profiler decides final density limits.

---

# 25. R02 asset-intake gates

Before R02 player-visible implementation, close at least:

1. acquire/hash visually accept Quaternius Wolf candidate;
2. acquire/hash visually accept Easy Enemies Spider candidate;
3. acquire/hash inspect `Goleling Evolved` or replace it with a clearly superior guardian candidate;
4. inspect exact field-guardian animation clips before final attack animation binding;
5. confirm Threateningly Mobs Continued dependency/license/runtime boundary for Nature Spirit/Lich normal dependency use;
6. inspect Lich actual model/animation/reinforcement presentation;
7. acquire/hash selected Stylized Nature Standard files including mushroom/plant/tree candidates;
8. accept actual resin model before locking resin player-facing name;
9. accept actual mana-flora model before locking player-facing name;
10. choose exact 4–5 R02 fish models and then lock their species names/size/value tables;
11. accept hamlet building compositions and two Town House-class shells;
12. accept sanctum room/prop family;
13. accept field-guardian/Lich VFX + sound families;
14. accept the three dungeon first-clear item model/icon bindings;
15. accept signature-material/Mythic visual identities before naming them.

A missing external binding stays marked unresolved. Do not fill it with a temporary vanilla/AI asset just to start coding.

---

# 26. First R02 playtest acceptance

When eventually implemented, a real client playtest must verify:

## Exploration

- player can orient using river/road/landmarks without map-arrow spam;
- forest feels denser than R01 but Trail Stag does not constantly collide with scenery on main routes;
- side routes reveal meaningful resource/POI content rather than dead ends.

## Combat

- Forest Spider dies quickly but can punish ignored poison pressure;
- Wolf packs pressure positioning without synchronized unavoidable lunges;
- Nature Spirit visibly extends the R01 learned kit rather than only gaining HP;
- field guardian feels visually like a new important creature, not scaled Nature Spirit;
- Lich reinforcement adds a tactical decision without turning 145 s into 4 minutes of add cleanup;
- Lich ground telegraphs/hitboxes match.

## Economy/side loops

- R02 gathering feeds real recipes/orders;
- fishing catches update Codex and can be sold/cooked;
- hamlet services reduce unnecessary travel without replacing the R01 hub;
- Town House preview clearly communicates the next housing step.

## Dungeon

- first clear lands around 18–24 minutes;
- shortcut prevents full trash replay after boss death;
- first-clear choice is useful and visually distinct;
- no vanilla spawners/UI/chests undermine presentation.

## Multiplayer

- personal node/fishing state independent;
- boss/event participation supports non-DPS roles;
- first-clear rewards personal/idempotent;
- physical housing vacancy cannot be double-purchased;
- no party-leader quest ownership.

---

# 27. Current verification state

- DESIGN REVIEWED AGAINST CURRENT MASTER CANON: YES
- R02 EXISTING REGIONS.md DIRECTION PRESERVED: YES
- EXTERNAL SOURCE RESEARCH: YES
- SOURCE/LICENSING CONFLICT FOR THREATENINGLY CONTINUATION RECORDED: YES
- R02 SPATIAL / SERVICE / COMBAT / QUEST / DUNGEON FLOW LOCKED: YES
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