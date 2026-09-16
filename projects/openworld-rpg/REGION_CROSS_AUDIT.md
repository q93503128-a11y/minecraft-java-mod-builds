# Open-World RPG — R01–R12 Cross-Region Quality Audit

> Date: 2026-09-16  
> Status: **DESIGN CANON — cross-region anti-repetition / identity / pacing contract**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Story spine: `WORLD_STORY_CANON.md`  
> Quality model: `DESIGN_COMPLETENESS_AUDIT.md`  
> Regional sources: `R01_VERTICAL_SLICE.md`, `R02_IMPLEMENTATION_PACKAGE.md` through `R12_IMPLEMENTATION_PACKAGE.md`  
> Rule: `GAME_DESIGN.md` remains master authority. Within the regional workstream, this later cross-region audit refines older regional-package details where it explicitly identifies a repetition correction; otherwise the individual regional package remains authoritative.

This audit was performed after re-reading the master design from top to bottom and then comparing the current R01–R12 regional packages side by side.

The purpose is not to add more features.

It is to answer:

```text
Do the twelve regions actually create twelve different play experiences?
Or did we accidentally write twelve versions of:

town → regional problem → field boss → ancient facility → dungeon boss → Anchor clue?
```

The answer is:

> The **regional identities are strong enough**, but several cross-region production patterns need explicit anti-repetition rules before implementation.

The main risk is no longer `biomes are too similar`.
The main risk is that the player learns the **content grammar** too early.

---

# 1. Audit basis / what was re-read

This pass directly checked:

- `GAME_DESIGN.md`;
- `DESIGN_COMPLETENESS_AUDIT.md`;
- `WORLD_STORY_CANON.md`;
- `FISHING_COLLECTION_HOUSING_MARKET.md` where housing/collection affects regional structure;
- `R01_VERTICAL_SLICE.md`;
- all current `R02_IMPLEMENTATION_PACKAGE.md` through `R12_IMPLEMENTATION_PACKAGE.md` files.

The master reread also found and corrected stale canon before this audit:

- reusable Field Camp Kit now replaces the obsolete per-placement material-cost wording;
- one-residence trade-up housing now replaces the obsolete broad/multiple-property wording;
- the B-style customizable protagonist / Anchor network / three personal ending structure is now represented in master canon;
- the stale design queue was replaced after R02–R12 packages were completed.

This document assumes those corrected master rules.

---

# 2. Region identity matrix

Each region needs one instantly explainable **play sentence** that is not merely its biome.

| Region | Primary play identity | Social/service identity | Main spatial language | Signature escalation |
|---|---|---|---|---|
| R01 Heartland | learn the whole RPG by playing a real region, not a tutorial corridor | full long-term starting hub | meadow / river / forest fringe / quarry | Trail Stag → Regalhart discovery → Earthloong first dungeon |
| R02 Western Forest | enclosed discovery, river forks, logging ecology and ruin mystery | compact logging/trade hamlet | shaded forest forks / river pools / woodland ruins | native Nature Spirit pressure → grove guardian → Lich sanctum |
| R03 Whitecrest | conquer vertical terrain and permanently improve the route | fortified mining town | switchback / mine / ridge / lifts / bridges | Griffin high-altitude hunt → mine/forge-observatory |
| R04 Frozen Crown | expedition logistics and readable travel through dangerous cold without survival chores | geothermal refuge / expedition fort | steam plumes / dark rock / waystations / ice coast | Iceworm tracking → glacial fissure / Icebroodmother |
| R05 Jungle Greenbelt | read a dense living jungle through river/root/canopy layers, then gain agile revisit speed | river market / handler economy | great river / roots / short canopy / temple silhouette | Jungle Komodo → mature Earthloong basin → stepped temple/root vault |
| R06 Mirewater Delta | route through interlocked land/water and manipulate bounded local flow | raised wetland/alchemist town | levee / boardwalk / shallow channel / ferry | Hydra water-space fight → local branch stewardship |
| R07 Sunscar Desert | long sightline travel, route commitment and visible water/trade infrastructure | oasis caravan town | caravan road / dune / canyon / buried aquifer | Deathworm hunt → fortress/cistern allocation crisis |
| R08 Bloomveil | explore valuable post-regulation magical ecology without turning the region into visual noise | sanctuary / scholar enclave | bloom fields / pollinating cliffs / glass-root structures | discovered ritual → Titan Rabbit → archive/over-stabilization crisis |
| R09 Southstone | grounded logistics, herd territory and human-built redundancy | caravan fort / foundry | trade road / exposed ridge / coast / fort | Elephant progression → heavy route threats → fortress logistics contrast |
| R10 Cinderfall | operate around a living volcanic industry while a successful restoration creates a real cascade | forge enclave / fortified refuge | ash shelf / industrial path / vent field / caldera | Basalt Wyvern → live cascade → caldera foundry / Inferno role |
| R11 Inner Sea | return to one maritime region at three campaign depths instead of consuming one flat ocean biome | harbor/freeport network | coast → open sea/reefs → abyssal 3D depth | Laviathan → deep-dive utility → Abyss Fang / abyss temple |
| R12 Obsidian Rift | sparse late-game anomaly exploration and synthesis rather than maximum content density | Edge Refuge | obsidian shelves / faults / split spires / excavation / Central Anchor | optional Terradragon → Sky Drake → systemic final guardian / ending |

Verdict:

- **terrain/visual identity:** strong;
- **traversal identity:** strong;
- **settlement fantasy:** mostly strong;
- **story evidence identity:** strong in concept, but delivery method can repeat;
- **boss/dungeon content grammar:** biggest repetition risk;
- **resource/economy identity:** healthy if new material IDs remain controlled;
- **actual density/pacing:** cannot be fully closed until Azari is imported and measured in travel time.

---

# 3. Forest / dry / water / industrial overlap audit

Several regions share a broad environmental family. They remain acceptable only if their **navigation and decision language** stays different.

## 3.1 R02 vs R05 vs R08 — three forested regions

### R02

Must remain:

- enclosed;
- grounded;
- river-fork / logging / ruin discovery focused;
- modestly magical;
- readable through small local landmarks and trails.

### R05

Must remain:

- tropical/living;
- visibly layered but not maze-like;
- anchored by a great river, giant canopy and short elevated routes;
- strongly tied to Komodo revisit speed and local ecological adaptation.

### R08

Must remain:

- more open and luminous in important spaces;
- cliff/bloom/sanctuary oriented rather than another dense-forest maze;
- magical because ecology and architecture interact, not because foliage density increases;
- visually hierarchical: ordinary earth/wood/stone remains so magic still has contrast.

**Cross-region rule:** R08 may not solve exploration difficulty by copying R05's root/canopy navigation. Its magic identity must change what the player observes/interacts with, not merely add another vertical forest layer.

## 3.2 R07 vs R09 — two dry regions

R07 = **distance + water infrastructure + oasis/caravan contrast**.

R09 = **physical logistics + herd territory + roads/forts/foundry**.

Cross-region rules:

- R07 keeps long sightlines and big destination silhouettes;
- R09 keeps more frequent human infrastructure and route decisions;
- do not give both regions the same sand/dust storm navigation mechanic;
- R09 heavy wildlife is not just R07 predators with more armor;
- R09's regional value comes from functioning human systems, not scarcity of water.

## 3.3 R06 vs R11 — two water-heavy regions

R06 = **shallow/edge water as route geometry**.

R11 = **maritime scale and late 3D depth**.

Cross-region rules:

- R06 repeatedly returns the player to dry/raised footing;
- R11 deliberately grows from coast to sea to abyss across the campaign;
- do not introduce deep-dive mechanics in R06;
- do not turn R11 coast into R06-style boardwalk/swamp routing;
- aquatic creature reuse must respect depth/habitat instead of treating all water as one spawn table.

## 3.4 R03 vs R09 vs R10 — mining/industry adjacency

R03 = **vertical extraction + old observation/engineering**.

R09 = **human-maintained logistics/foundry + distributed redundancy**.

R10 = **live thermal industry + dangerous system coupling/cascade**.

Cross-region rules:

- R03's forge/observatory is historical discovery, not an operating industrial city;
- R09's foundry exists because current people maintain trade and production;
- R10's foundry is a high-risk active system whose state changes during the story;
- do not reuse the same furnace room / ore-cart / switch-puzzle composition across all three.

---

# 4. Main-story evidence delivery must vary

The Anchor story is allowed to connect regions. The **delivery method may not become formulaic**.

Bad repeated pattern:

```text
regional problem
→ enter old facility
→ read ancient log
→ boss
→ receive philosophy lesson
```

Canonical evidence-delivery distinction:

| Region | What the player should primarily *experience*, not merely read |
|---|---|
| R01 | ordinary local disturbances unexpectedly connect to a deeper old facility; mystery begins |
| R02 | old records/relay history broaden the mystery, but the forest/Lich crisis remains a local ruin story rather than an ideology lecture |
| R03 | physical maps, lifts, machinery and sightline relationships prove a continental network existed |
| R04 | restoring a bounded heat branch visibly keeps real routes/waystations/community activity working |
| R05 | current ecology and local society visibly function without central reconnection; regulator interference is the exception |
| R06 | the player physically divides local water control into bounded branches and sees channels/routes stabilize |
| R07 | consequences are distributed across **multiple surface communities/wells/routes**; the aquifer complex explains why, but the evidence is visible before the final room |
| R08 | scholars/records + visible magical ecology demonstrate that over-stabilization suppresses something genuinely valuable |
| R09 | the strongest evidence is the **working human-built road/beacon/depot system itself**; the old logistics core is contrast, not the sole proof |
| R10 | a restoration test visibly succeeds first, then a live cascade demonstrates why success can recreate systemic risk |
| R11 | a useful shipping/current improvement produces a delayed consequence far away and below, proving remote coupling through distance |
| R12 | architecture, prior regional evidence and the active Central Anchor finally synthesize the whole system; no final lore-dump room should carry the plot alone |

Rules:

- no more than a minority of important truths should be delivered only through readable records;
- recurring NPC dialogue explains interpretation, not facts the player was already able to see;
- a region's ideological implication is never displayed as `Restore +1 / Release +1 / Partition +1`;
- optional regions add context/epilogue state without making the player feel they missed the only correct ending evidence.

---

# 5. Dungeon grammar audit

The current dungeon concepts are diverse enough **if their play structure is preserved**.

| Region | Dungeon/major-complex grammar that must stay distinct |
|---|---|
| R01 | short quarry → roots; first readable replayable dungeon, minimal branching |
| R02 | compact woodland sanctum/arboretum; stronger room identity and bounded caster reinforcement |
| R03 | collapsed mine route transitions into vertical forge/observatory; traversal unlocks matter as much as rooms |
| R04 | expedition through glacial fissure into heat infrastructure / brood habitat; natural-to-built transition |
| R05 | stepped temple + root vault; readable layer reconnection rather than long corridor rooms |
| R06 | partially flooded regulator/shrine; water state changes combat/route geometry without long swimming |
| R07 | surface fortress + shafts/sandfall + cistern/aquifer; vertical dry-to-water contrast |
| R08 | archive/observatory with knowledge/ecology spaces; not a stone corridor dungeon with magical wallpaper |
| R09 | **surface-forward fortress/foundry** with battlements, route pressure and current human history; do not over-emphasize another deep ancient vault |
| R10 | live industrial emergency across changing foundry/caldera spaces; story climax starts before boss arena |
| R11 | two different archetypes: exposed sea fort/pirate-cove and later fully 3D abyssal temple |
| R12 | sparse excavation/cathedral → Central Anchor finale; exploration branches exist but final route remains readable |

## 5.1 Locked anti-repeat rule

Do not implement all dungeons from one modular room grammar such as:

```text
entry room
→ combat room
→ puzzle switch
→ combat room
→ shortcut
→ boss arena
```

Shared code/data primitives are encouraged.
Visible pacing/composition must differ.

## 5.2 Ancient-facility saturation rule

Not every regional dungeon needs to secretly be an Anchor facility.

- R02 remains primarily a woodland sanctum/Lich location;
- R09 remains primarily a current/historical fortress-logistics location;
- natural lairs and cultural structures retain their own history;
- Anchor infrastructure may intersect a location only when the regional story needs it.

The world must feel older and larger than one ancient organization.

---

# 6. Signature encounter / field-boss diversity

The project likes large memorable creatures, but a static `boss icon → circular arena → respawn timer` template repeated twelve times would destroy that advantage.

Target diversity:

| Region | Signature encounter discovery/form |
|---|---|
| R01 | Regalhart clue hunt with broad search area; player may find boss first |
| R02 | **regional environmental disturbance / grove incident**, not another `2 of 3 clues` counter |
| R03 | visible high-altitude territory and approach choice; Griffin is noticed from the mountain before a UI hunt list |
| R04 | multi-site Iceworm tracking across exposed snow/ice terrain |
| R05 | mature Earthloong territorial basin; ecology/terrain signs, no formal clue checklist required |
| R06 | Hydra encounter tied to a changing flooded basin / local water event state |
| R07 | long-distance Deathworm signs and moving dune territory; spectacle is visible in open terrain |
| R08 | deliberate optional ritual invocation of Titan Rabbit after discovering the ritual site |
| R09 | roaming herd/road-crisis heavy threat; should intersect logistics/route content rather than live permanently in one arena |
| R10 | Basalt Wyvern cliff/air-space hunt; Inferno belongs to the live caldera crisis rather than ordinary field-boss spawn logic |
| R11 | open-sea predator territory for field threat; Abyss Fang is a deep authored temple/altar encounter |
| R12 | Terradragon is a deliberately optional altar-summoned ultra boss; the main-story final guardian is separate |

### Explicit R02 repetition correction

The older R02 package currently describes the grove guardian through a `2 of 3 clues → broad search area` structure that is intentionally similar to R01 Regalhart.

That similarity is no longer desired after the whole-game comparison.

**Refinement:** R02 grove guardian discovery should use an authored **forest disturbance escalation** instead:

```text
player encounters one major grove/logging/bridge disturbance
→ world/NPC/environment indicates a moving pressure source deeper in the forest
→ disturbed roots/stone/soil act as continuous environmental trail language, not counted checklist clues
→ player reaches the broad guardian territory naturally
```

Finding the guardian directly remains valid.
No exact GPS pin is required.
No `2/3` objective counter is shown.

This cross-audit refinement supersedes only the R02 field-guardian **discovery method**, not its combat/reward contract.

---

# 7. Altar / ritual encounter saturation

R08 Titan Rabbit, R10 Inferno, R11 Abyss Fang and R12 Terradragon all have some donor/source relationship to ritual/altar/summoning presentation.

They must not feel like four copies of `place item on altar → boss appears`.

Distinct production roles:

- **R08 Titan Rabbit:** optional discovered magical/ecological ritual; whimsical-strange hunt identity;
- **R10 Inferno:** the caldera crisis/forge state causes the encounter to become active; preserve altar/core visual identity without making the player perform the same optional summoning interaction as R08/R12;
- **R11 Abyss Fang:** deep-temple investigation/lure/activation in 3D water space; the underwater approach is the encounter setup;
- **R12 Terradragon:** deliberate optional ultra-boss summoning remains the clearest true `altar summon` of the late game.

If implementation produces the same interaction/animation/UI prompt for all four, it fails the regional-identity audit even if each boss model is different.

---

# 8. Settlement/service identity audit

The towns must not become twelve reskinned service menus.

A settlement may share basic rest/merchant/storage conveniences when needed for travel quality, but each important hub has a **headline function** and deliberate omissions.

| Region | Headline settlement role | Services/identity that should dominate |
|---|---|---|
| R01 | full long-term starting hub | guild/class access, basic forge, bank, stable, housing, broad baseline services |
| R02 | logging/trade hamlet | timber/resin/fish trade, basic supplies, compact inn/storage, early housing option |
| R03 | fortified mining town | smithing/mining, lift/route engineering, ore trade, heavy-equipment identity |
| R04 | expedition/geothermal refuge | recovery, supply, healer/expedition logistics; no need for a normal residential market |
| R05 | river market | fishing, herbs/alchemy trade, Komodo handler, river commerce |
| R06 | raised wetland/alchemist town | alchemy/healing, ferry/water-route services, wetland materials |
| R07 | caravan oasis town | caravan trade, travel/ranged supplies, cistern/well economy, route information |
| R08 | scholar sanctuary | research/spellcraft-facing services, pigments/pollen/magical materials, quiet social space |
| R09 | caravan fort/foundry | foundry/metal, logistics contracts, Elephant handlers, high-value route trade |
| R10 | forge enclave/refuge | highest thermal/high-tier smithing identity, safe recovery and supply; limited ordinary town breadth |
| R11 | harbor/freeport network | fish/sea trade, Laviathan, dive utility, maritime storage/services; strongest maritime hub |
| R12 | Edge Refuge | rescue/research/high-tier supply/journal; deliberately no normal housing district or giant market |

Rules:

- do not copy the R01 physical service layout into later towns;
- every settlement does not need a class trainer, full forge, full alchemy, stable, bank, housing and guild all in one plaza;
- missing advanced services are acceptable when a nearby major hub/fast travel relationship keeps the omission from becoming travel punishment;
- NPC ambient work must match the settlement's actual economy;
- settlement silhouettes/signage/props must communicate role before the player opens menus.

## 8.1 Housing distribution

Housing is **not** a linear `new region = next house tier` unlock track.

Canonical cross-region rule:

- R01 provides several Small Cottage vacancies and at least one visible future upgrade;
- later safe settlements may provide alternative Town House / Large / Prestige shells according to actual architecture and geography;
- R04/R10/R12 expedition/refuge-style hubs do not need purchasable homes merely to satisfy a checklist;
- buying a larger house remains an economy/collection choice, not mandatory regional progression;
- multiple visual/architectural choices within a tier are preferable to exactly one house per tier if the external build pool supports them.

Final property placement waits for the actual Azari settlement composition audit.

---

# 9. Resource / crafting anti-bloat audit

The regional packages generally follow the correct direction: keep old materials relevant and add only visually/economically justified resources.

Cross-region admission rule for every new material:

A normal regional material must satisfy at least one of:

1. it has **multiple real sinks** across equipment/consumable/furnishing/production/trade;
2. it has one strong signature sink whose identity is worth the inventory/material ID;
3. it is needed for a clearly different external visual/ecological collectible role.

Reject:

- `Iron II` / `Hardwood II` / `Healing Herb II`;
- one-use filler materials whose only purpose is one quest turn-in;
- one new currency per region;
- color variants as separate IDs without gameplay reason;
- forcing every boss to drop a unique craft token if its signature item can use a direct named material/reward instead.

R02's continued use of Hardwood/Healing Herb and its refusal to create mushroom-color IDs is the correct pattern to preserve.

Fishing remains a **shared world collection system**, not 12 separate regional fishing economies. The 36–48 launch-species target already allows overlap; do not force `4–6 brand-new fish` for every region.

---

# 10. Reward-identity overlap audit

Regional rewards should change build options, not only apply a local elemental resistance.

Current broad identity remains healthy:

- R02: DEX/WIL, finesse, nature/status utility;
- R03: guard/poise/heavy/weak-point;
- R04: frost/control/anti-slow/defensive expedition utility;
- R05: poison/status, mobility/rapid attack, herbal/nature-earth variants;
- R06: poison/bleed control, healing, anti-grab, water mobility;
- R07: anti-burst, armor break/impact, ranged/open-space utility;
- R08: Mana, spell shaping, support/healing, status conversion;
- R09: heavy armor, guard/counter, impact resistance, travel/merchant utility;
- R10: high-tier forge, fire/impact, charged/committed effects;
- R11: water mobility, projectile control, predator/bleed, abyss variants;
- R12: build-defining relic/skill variants/unusual late effects rather than only largest numbers.

Risk pairs that require item-level review later:

- R03 vs R09 — guard/heavy/impact;
- R05 vs R06 — poison/status/herbal utility;
- R08 vs R12 — magical/arcane build-defining effects;
- R04 vs R11 — movement/control utility near water/cold environments.

Rule for later equipment intake:

> If two regions reward the same build axis, they need different **mechanics**, not merely stronger percentages.

Example shape:

- R03 heavy item may reward successful poise break;
- R09 heavy item may reward guard/counter or charge-line control;
- R12 magic item may alter a skill rule entirely rather than adding more Magic Power.

Do not finalize duplicate affix families just because the region summary uses different flavor text.

---

# 11. Traversal-progression cadence

The current traversal cadence is one of the strongest whole-game structures and should be preserved.

```text
R01 Trail Stag — sustained basic travel convenience
R03 lifts/bridges — regional permanent route mastery
R04 waystations / safe expedition routes — harsh-region readability
R05 Jungle Komodo — agile/faster ground revisit
R06 ferries/levees/local water routes — regional network mastery
R07 caravan-road shortcuts — long-distance route confidence
R09 Caravan Elephant — heavy/group/logistics traversal identity
R10 forge lifts/pressure paths — industrial route recovery
R11 Laviathan + Deep-Dive Utility — sea/group/depth traversal
R12 Sky Drake — late flight only after ground-scale learning
```

This avoids the bad structure where every region needs a new mount.

Rules:

- later mounts cannot make all previous routes meaningless;
- regional shortcuts remain useful after faster mounts;
- Sky Drake arrives late enough that first-read terrain still matters;
- fast travel remains discovered shrine/major-hub travel and does not replace route mastery during first exploration.

---

# 12. Density / travel-pacing bands

The exact world budget waits for Azari import and coordinate measurement.
Do **not** convert this section into arbitrary POI counts before then.

Use the existing audit's playtest targets as a starting measurement language:

- major visual landmark / meaningful route decision: usually within ~30–90 s of ordinary travel where geography permits;
- meaningful optional interaction/POI opportunity on content-bearing primary exploration routes: roughly every ~1–3 min;
- substantial authored POIs remain several minutes apart so they keep identity;
- intentionally sparse regions may stretch the interaction cadence when the empty interval creates anticipation, spectacle, danger or destination readability.

Relative density intent:

### Higher / compact

- R01;
- R02;
- settlement/market portions of R05/R08.

### Medium / varied

- R03;
- R05 overall;
- R06;
- R09.

### Intentionally sparse in major stretches

- R04 expedition routes;
- R07 open desert;
- R10 ash/volcanic approaches;
- R11 open sea / abyss;
- R12 anomaly zone.

Sparse does **not** mean dead.

A sparse interval must provide at least one of:

- destination sightline;
- navigation choice;
- ecological spectacle;
- threat anticipation;
- environmental story;
- resource opportunity;
- audio/weather/scale contrast.

If it provides none, shorten or repurpose the route.

R12's existing ~2–5 min content-bearing route decision / ~5–8 min substantial POI target is a valid intentionally sparse exception, not a template for the whole world.

---

# 13. Recurring NPC / faction distribution

The recurring cast should connect the world, but every major NPC appearing in every region would make the continent feel staged around the player.

Rules:

- normally use **1–2 recurring roles** in a regional main-story visit, plus local NPC anchors;
- the Guild Pathfinder dominates early field continuity, not every late scientific scene;
- Anchor Scholar appears where interpretation matters;
- Engineer/Smith appears where infrastructure mechanics matter;
- Cartographer/Ranger appears where ecology/routes/discovery matter;
- Rival Wanderer remains intermittent and unpredictable rather than an obligatory pre-boss cameo;
- Restoration Director should become more visible as coordinated reactivation becomes politically relevant, not stand in the starting village giving exposition.

Local characters remain necessary so each region is not populated only by touring main-cast NPCs.

---

# 14. Visual/audio distinction rules

Several region pairs can collapse visually if external assets are admitted carelessly.

## R02 / R05 / R08

- R02: grounded greens/browns, shade, river/ruin mystery;
- R05: tropical water/root/canopy abundance;
- R08: bloom/cliff/glass-root contrast with restrained luminous accents.

## R03 / R09 / R10

- R03: exposed stone, wind, mining/observatory machinery;
- R09: warm dry road/fort/foundry and human logistics;
- R10: ash/basalt/active thermal industry.

## R04 / R12

Both can be sparse/high-contrast.

- R04 communicates weather, cold and expedition distance;
- R12 communicates structural/anomalous rule failure and old network convergence.

R12 must not become `cold region but black/purple`.

## Audio

Regional music/ambience must not be only one biome loop + one combat loop.

At implementation planning, each region needs at least:

- travel/exploration ambience identity;
- settlement/refuge identity;
- danger/combat transition language;
- dungeon/major-complex treatment;
- signature boss treatment when importance warrants it;
- enough silence/breathing room that telegraphs and environmental sound remain readable.

Exact global music system remains a later queued design batch.

---

# 15. Whole-game repetition gates

Before accepting any implemented region, compare it to the previous two implemented regions and ask:

1. Did its primary route challenge change?
2. Did its settlement feel like a different place to live/work, not a palette swap?
3. Did its signature encounter start differently?
4. Did its dungeon use a different spatial rhythm?
5. Did it communicate its story evidence differently?
6. Did its reward mechanics create a different build decision?
7. Did it add/reuse materials without inventory bloat?
8. Did it provide at least one memorable non-combat discovery?
9. Did its first-clear world state visibly change anything worth noticing?
10. Did the region introduce novelty without a new chore meter/currency/menu?

If several answers are `no`, do not solve it by adding more content. Change the content grammar.

---

# 16. Current design maturity refresh

The older `DESIGN_COMPLETENESS_AUDIT.md` maturity snapshot was written **before** `WORLD_STORY_CANON.md` and the full R03–R12 package pass. Its quality model and gates remain valid, but its old `later regions/narrative are largely absent` snapshot is no longer current.

Current post-region-pass estimate:

| Area | Current design maturity | Main remaining blocker |
|---|---|---|
| core systems/combat/progression/economy | D4 | implementation/playtest rather than design |
| quest/world-state framework | D4 | actual scene/dialogue content |
| world story / ending structure | D3–D4 | exact scene package, proper nouns, final guardian binding |
| R01 route/content | D4 design | exact asset intake/client validation |
| R02–R11 regional structure | D3–D4 | exact asset bindings, placement and some boss identities |
| R12 structure/finale role | D3 | exact final guardian/model/mechanics and final encounter pass |
| whole-world spatial density | D1–D2 | Azari import / coordinates / measured traversal |
| external asset closure | D2–D3 | exact files/hashes/visual acceptance across regions |
| global audio/music | D1–D2 | whole-game system/source binding |
| accessibility/difficulty/keybinds | D1–D2 | final control/assist decisions after real combat feel tests |
| save migration/recovery + open-world QA harness | D1–D2 | implementation-specific design/telemetry plan |

Practical whole-project planning maturity is now roughly **80–85%**, not the earlier ~70% snapshot.

This still does **not** mean the game is 80–85% complete.

It means:

```text
most core rules: closed
most regional fantasy/flow: closed
whole story structure: closed enough to write scenes
exact assets/world placement/audio/accessibility/final encounter: still open
actual implementation and play quality: not proven
```

---

# 17. Next design work after this audit

Priority order:

1. **Exact external asset intake expansion** — finish R01 unresolved bindings, then region-critical bosses/resources/architecture/VFX/audio, including the R12 final guardian.
2. **Main quest / recurring-character scene package** — turn the already-locked story spine into actual scene/quest beats without forcing all regions.
3. **Azari terrain import / coordinate audit when technically available** — only then lock exact POI counts, roads, shrine positions and measured travel cadence.
4. **Global audio/music direction** — use external licensed/usable sources and define mix/crossfade/telegraph priority.
5. **Accessibility/difficulty/control closure** — finalize only after combat/UI can be judged in a real client where feel-sensitive decisions can be tested.
6. **Pre-bootstrap closure audit** — verify remaining TBDs are genuine asset/terrain/implementation gates rather than hidden game-design invention.

No new side system should jump ahead of these simply because it is easy to invent.

---

# 18. Verification state

For this audit batch:

- DESIGN REVIEWED: **YES**
- MASTER CANON RE-READ: **YES**
- R01–R12 CROSS-REGION REVIEW: **YES**
- EXTERNAL PRECEDENT RE-RESEARCH: **NOT REQUIRED FOR THIS PASS** — this pass compared the already researched/canonical regional precedents; no new external factual claim is needed to change the cross-region rules.
- CODE REVIEWED: **N/A**
- TESTED: **NO**
- BUILD VERIFIED: **NO**
- JAR PRODUCED: **NO**
- PLAYTESTED: **NO**
- MULTIPLAYER TESTED: **NO**

No build/CI should be run for this docs-only audit.