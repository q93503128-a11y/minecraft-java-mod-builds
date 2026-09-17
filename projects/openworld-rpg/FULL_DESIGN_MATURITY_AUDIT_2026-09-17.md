# Open-World RPG — Full Design Maturity Audit

> Date: 2026-09-17  
> Status: **FULL-CANON REVIEW COMPLETE / PRODUCTION READINESS STILL GATED**  
> Authority: this is a cross-document quality audit. It does not silently redesign gameplay canon. `PROJECT.md` remains the authority for current pre-code gates, and dedicated later canon wins over older audit wording.

---

# 1. Purpose

This pass reviews the current Open-World RPG design as a **real production game plan**, not as a list of ideas.

Questions:

1. Can an implementer build the intended game without inventing major gameplay rules while coding?
2. Are combat, progression, economy, exploration, quests, multiplayer, save/rejoin, UI and regional content connected rather than isolated features?
3. Do R01–R12 create genuinely different play rather than different biome skins on one repeated template?
4. Does the design avoid complexity inflation, currencies, menus and maintenance chores?
5. Are the documents internally synchronized with current `PROJECT.md` authority?
6. How does the document set compare with public professional RPG design material and large Minecraft RPG/mod precedents?
7. What still blocks source bootstrap because it needs **real world space, real assets or real play evidence** rather than more prose?

This audit deliberately separates:

```text
PAPER / AUTHORING COMPLETENESS
from
PRODUCTION BINDING COMPLETENESS
from
PLAY-PROVEN COMPLETENESS
```

A detailed document is not proof that a mechanic feels good.

---

# 2. Canon reviewed

The review covered the current active design set, including:

## Master / project

- `PROJECT.md`
- `GAME_DESIGN.md`
- `BRANDING.md`
- `DESIGN_COMPLETENESS_AUDIT.md`
- `REGIONS.md`
- `REGION_CROSS_AUDIT.md`
- `WORLD_STORY_CANON.md`
- `MAIN_QUEST_SCENE_PACKAGE.md`

## Combat / growth / economy

- `COMBAT_BALANCE.md`
- `CLASS_COMBAT_KITS.md`
- `CLASS_PROGRESSION.md`
- `STATUS_AND_R01_ENCOUNTERS.md`
- `LOOT_ECONOMY.md`
- `EQUIPMENT_BALANCE.md`
- `RECOVERY_PRODUCTION_APPEARANCE.md`

## Field / collection / traversal

- `GATHERING_FISHING_CAMP_HOUSING.md`
- `FISHING_COLLECTION_HOUSING_MARKET.md`
- `MOUNTS.md`

## State / multiplayer / presentation

- `QUEST_WORLD_STATE.md`
- `PARTY_MULTIPLAYER.md`
- `UI_DIRECTION.md`
- `ACCESSIBILITY_DIFFICULTY_INPUT_AUDIO.md`
- `R11_AQUATIC_ACTION_MATRIX.md`
- `M0_DEPENDENCY_AUDIT.md`

## Regional production canon

- `R01_VERTICAL_SLICE.md`
- R02–R12 `*_IMPLEMENTATION_PACKAGE.md`
- R02–R12 `*_CONTENT_BIBLE.md`

## Production binding / pre-code quality

- `PRODUCTION_ASSET_BINDING_MATRIX.md`
- `EXTERNAL_SOURCES.md` direction and asset-intake contracts
- `AZARI_SPATIAL_CLOSURE_PASS1.md`
- `BOSS_REFERENCE_DESIGN_PASS_R05_R08_2026-09-17.md`
- current R05–R08 guardian intake passes

---

# 3. Executive maturity assessment

These percentages are **review heuristics**, not measured completion claims.

| Layer | Current maturity | Meaning |
|---|---:|---|
| Core gameplay/system authoring | ~94–97% | rules/formulas/state ownership are unusually well specified |
| Class/combat progression authoring | ~95% | implementer invention is low; tuning remains empirical |
| Economy/loot/recovery/field-loop authoring | ~92–96% | loops are linked and anti-chore rules are strong |
| Regional R01–R12 content authoring | ~90–95% | hubs, casts, quest flows, rewards, aftermath and reconnect are mostly closed |
| Multiplayer/save/rejoin design | ~95% on paper | server authority/idempotency boundaries are exceptionally explicit; not runtime-tested |
| UI/UX behavior/direction | ~85–90% | hierarchy and visual family are bound; exact finished screens still need asset/client proof |
| Exact presentation / provenance binding | ~55–65% | many directions are known, but numerous exact files/models/VFX/SFX remain gated |
| Actual Azari spatial closure | ~20–30% | macro render windows exist; real x/y/z, routes, sightlines and travel times are not verified |
| Empirical feel/balance proof | **0% playtested** | no gameplay source/real client play proves the numerical design yet |

Do **not** average these into one fake overall percentage.

The project is highly mature as a **design-authoring package**, but it is not yet production-proven.

---

# 4. What is unusually strong

## 4.1 The systems form one game instead of a feature pile

The strongest property is connection density.

Examples:

- Recovery Belt → consumable production → gathering → Gold economy → camp/inn recovery.
- Field gathering → alchemy/forge/housing → regional resources → equipment and exploration.
- Bosses → deterministic signature materials → chosen Mythic construction, reducing pure RNG dependence.
- Mounts → real regional routes and handling challenges rather than a separate mount-level treadmill.
- Fishing → cooking/sale/codex/housing rather than a second progression game.
- Settlements → services/economy/story/world-state consequences rather than menu hubs.
- Regional quest outcomes → physical routes/shops/shortcuts/NPC state rather than only journal completion.

This matches the project quality principle that depth comes from interactions, not menu count.

## 4.2 Strong complexity discipline

Across the full design, many tempting but low-value systems are explicitly rejected:

- no biome-wide cold/thirst/heat/swamp/anomaly maintenance meters;
- no separate currency per region;
- no mount XP/breeding/genetics treadmill;
- no separate underwater class/build/equipment tree;
- no durability maintenance loop;
- no daily/weekly login treadmill;
- no prestige/paragon ladder at launch;
- no mandatory resistance suit for each biome;
- no permanent survival tax merely for existing in a region.

This is one of the most important reasons the large feature count does not automatically collapse into management labor.

## 4.3 Combat design is implementation-grade rather than thematic

`COMBAT_BALANCE.md`, class kits and progression close:

- server timing;
- dodge/guard/perfect-guard windows;
- input buffering/cancel windows;
- poise/stagger;
- damage/stat relationships;
- multiplayer boss scaling;
- class resource loops;
- specialization branches;
- Rank 10/20/32/44/50 milestones;
- world skills/Hidden Techniques;
- class fairness expectations.

This is much closer to a combat specification than a normal hobby-project GDD.

The risk is no longer `missing combat design`; it is **false precision before real playtest**.

## 4.4 Multiplayer/save thinking is far above ordinary mod planning

The design repeatedly specifies:

- server authority;
- personal vs shared state;
- late-join behavior;
- idempotent first-clear rewards;
- reconnect checkpoints;
- support participation credit;
- no last-hit ownership;
- no host-owned personal ending;
- atomic Gold/mount/service transactions.

This is especially strong because many Minecraft adventure designs leave these decisions until bugs appear.

## 4.5 Regional identity is usually gameplay identity, not only biome identity

Good examples:

- R03: understanding vertical terrain and permanently restoring real mountain routes.
- R04: whiteout landmark navigation + shelters/waystations instead of a cold meter.
- R05: river/canopy orientation and modern ecological adaptation.
- R06: interlocked dry/shallow channel routing without forced-swim punishment.
- R07: long sightlines, caravan-road vs dune/badlands route commitment without thirst.
- R08: magical ecology and bounded stabilization, not `forest but purple`.
- R09: roads/signals/depots built by modern people, not another ancient-machine biome.
- R10: a restoration system that visibly succeeds before coupling causes a cascade.
- R11: one sea learned in coast/open/deep layers rather than one flat ocean biome.
- R12: sparse anomaly rules with grounded infrastructure, not a global corruption meter.

The anti-repetition effort is real and generally successful at the **play-space level**.

## 4.6 External-first presentation gating is correct for this project

Important player-facing slots do not get temporary vanilla mobs, black-panel UI, particle-only bosses or generic placeholder dungeons.

This costs schedule speed but prevents prototype art from silently becoming production art.

The R05–R08 boss-reference contracts are particularly strong because exact anatomy is intentionally not invented before model intake.

---

# 5. Main remaining design-quality risks

## 5.1 CANON SYNC REQUIRED — stale documents now exist inside the live set

The largest documentation-quality problem is no longer missing design. It is **authority drift**.

Confirmed examples:

### Title / branding blocker conflict

Current authority in `PROJECT.md`:

- final title is **not** a gameplay-source-bootstrap gate;
- exact final logo/font/graphic remains an external-presentation gate for finished presentation.

Stale/live wording still appears in:

- `GAME_DESIGN.md` next-design/pre-code queue;
- `DESIGN_COMPLETENESS_AUDIT.md` blocker summaries;
- `MAIN_QUEST_SCENE_PACKAGE.md` remaining pre-code work;
- `BRANDING.md` final line currently calls the final title string a pre-code design gate.

These must be synchronized to the current `PROJECT.md` rule.

### Completed packages still described as future work

`REGION_CROSS_AUDIT.md` retains older wording that treats the main quest scene package and accessibility/audio work as still missing, despite:

- `MAIN_QUEST_SCENE_PACKAGE.md` now closing the cross-region route/rejoin structure;
- `ACCESSIBILITY_DIFFICULTY_INPUT_AUDIO.md` closing difficulty/assist/input/audio behavior.

### Class progression tail

`CLASS_COMBAT_KITS.md` retains old next-stage wording that is superseded by the later `CLASS_PROGRESSION.md` closure.

### R11 aquatic compatibility

The older R11 package still contains language implying underwater class/animation compatibility remains an unresolved design gate. `R11_AQUATIC_ACTION_MATRIX.md` has closed that design choice.

Remaining work is real animation/retarget/render/runtime proof, not invention of a second aquatic combat design.

### Threateningly licensing shorthand

Older dependency/region wording that flattens Threateningly Mobs Continued to `MIT` is stale for raw-byte reuse. Current binding authority records conflicting storefront metadata and requires dependency-only handling until exact upstream rights are resolved.

This includes selected old wording in dependency/R12 material.

### Decision

Do **not** create another parallel master document to solve this.

Perform a dedicated stale-canon cleanup on the original live files.

Git history is the archive.

---

# 5.2 The final-choice evidence currently risks making Partition feel empirically favored

The finale explicitly states that Restore / Release / Partition have no hidden correct answer. The wording is balanced.

However, the **experienced regional resolutions** are not equally distributed.

Approximate evidence shape:

```text
Restore-positive evidence:
- R04: bounded restoration saves people / restores useful infrastructure
- R10: restoration genuinely works before coupling becomes the danger

Release/autonomy-positive evidence:
- R05: society/ecology adapted beyond obsolete regulation
- R08: old stable target suppresses valuable modern magical ecology

Partition/local-control-positive evidence:
- R06: keep useful water infrastructure, sever remote authority
- R07: keep local wells/cisterns, sever remote draw
- R09: keep sensor information, reject remote allocation
- R10: keep local geothermal control, sever continental balancing
- R11: keep local harbor prediction, sever deep continental balancing
```

The problem is not the number of dialogue lines.

The player repeatedly **does Partition-like solutions successfully** before being asked whether Partition is one of three uncertain continental futures.

That can create an unintended answer:

> `We already tested Partition six times and it worked.`

### Recommended correction

Do not rewrite regions or add a fourth philosophy.

Instead, make existing local/partitioned outcomes expose **real costs that local autonomy cannot solve alone**:

- cross-region emergency coordination becomes slower;
- local optimization can push a problem across a border unless regions negotiate;
- duplicated infrastructure costs more labor/material;
- some large-scale forecasting/transport functions lose efficiency;
- incompatible regional rules can create trade or safety friction;
- at least one important late problem should require voluntary cross-region coordination even after remote authority is rejected.

Likewise, Restore should receive at least one late piece of evidence showing a problem that **cannot realistically be solved by isolated local systems** without losing substantial value.

This preserves ambiguity without reversing existing regional successes.

---

# 5.3 Regional main stories are mechanically varied but structurally too clean

A recurring high-level pattern remains:

```text
arrive at hub
→ observe local problem
→ investigate 2–3 sites
→ discover relation to old infrastructure
→ enter major dungeon/control site
→ obtain philosophical evidence
→ change regional operating state
→ visible aftermath
```

The individual play grammar is often different enough to remain enjoyable, but the **narrative production grammar** can become predictable.

R08, R09, R10 and R11 improve this:

- R08 begins with a restoration test that is technically succeeding;
- R09 centers modern human infrastructure first;
- R10 gives the player time to enjoy successful restoration before failure emerges;
- R11 delays the consequence across depth and time.

Still, the game needs a few more moments where the region matters because of **people, rivalry, loss, discovery or opportunity** without cleanly serving as a philosophy exhibit.

Recommendation:

- do not add more quest systems;
- use existing named casts and optional scenes;
- ensure several memorable quests have stakes that are personally/local important even if they contribute **zero ending evidence**.

The goal is to stop the world feeling like twelve case studies arranged for the final exam.

---

# 5.4 Optional-contract authoring is the weakest content layer

Main regional chains are generally strong.

Optional contracts are more repetitive:

- gather 2–3 local resources;
- catch one regional fish;
- resolve one elite/event;
- discover one POI;
- restore the leftover third node.

This is acceptable as **light system-facing content**, but if presented as named authored side quests across all 12 regions it can feel like checklist filler.

Recommendation:

- keep these short system contracts;
- do not inflate them into dialogue-heavy quest chains;
- for each region, ensure at least one optional authored quest is character/world-specific and cannot be summarized as `collect local thing / kill local threat / visit local point`;
- allow the rest to remain clearly lightweight contracts/events rather than pretending every task is a bespoke narrative quest.

This is a content-quality refinement, not a new subsystem.

---

# 5.5 Numerical design is strong but too easy to mistake for final truth

The project has exact-looking values for:

- frame/tick windows;
- damage coefficients;
- HP/Defense/MR;
- TTK bands;
- Gold costs;
- EXP curves;
- class progression;
- boss timings;
- resource quantities;
- gathering times;
- mount speeds;
- item curves.

Many are excellent **seed values**.

They are not play-proven.

Recommendation: adopt a documentation distinction:

```text
HARD_RULE
- identity / ownership / invariants / qualitative limits
- change only through deliberate design revision

TUNEABLE_SEED
- initial number chosen for first implementation/playtest
- expected to move from measured play
```

Examples:

- `no routine common-mob gear drops` can be a hard economy rule;
- exact `30% elite equipment drop chance` is a tuneable seed;
- `perfect guard exists and is server-authoritative` is a hard rule;
- exact tick window is tuneable after latency/feel tests;
- boss active-TTK target band is a design target;
- exact boss HP is derived/tuneable.

This prevents paper precision from becoming resistance to playtest evidence.

---

# 5.6 Project-level design pillars are present implicitly, but not compressed enough

Public Subnautica design material explicitly describes starting from and adhering to specific design pillars, then testing the intended emotions through prototypes.

Openworld RPG contains strong principles everywhere, but its top-level identity is spread through many pages.

A new implementer can understand the game, but must read a lot before knowing which four ideas win when two good features conflict.

Existing canon can be compressed without redesign into roughly:

1. **Authored world over checklist world** — geography, settlements, ecology, quests and consequences have purpose.
2. **Fast readable action RPG combat with build depth** — commitment and mastery without sponge/maintenance combat.
3. **Progression expands choices instead of chores** — no currency/menu/treadmill inflation.
4. **Persistent co-op world with production-grade presentation** — server truth, external-first visuals and visible consequences.

This should eventually appear as a short north-star section in the master canon, not as another new document hierarchy.

---

# 5.7 The largest actual blocker is no longer writing: it is 3D space

The strongest professional comparison is Guerrilla's published description of *Horizon Forbidden West* quest production.

For a main quest they describe:

```text
narrative story summary
→ quest design document with story beats/objectives/world map/area descriptions
→ in-engine 3D layout as soon as possible
→ iterate flow/level design in the real space
```

Openworld RPG already has unusually strong equivalents of the first two steps.

It does **not** yet have the third step for its chosen Azari world.

`AZARI_SPATIAL_CLOSURE_PASS1.md` correctly refuses to fake this from the public overhead render.

Until actual Azari bytes are loaded, the following remain hypotheses:

- hub footprints;
- dungeon entrance locations;
- road joins;
- real route length;
- slope/collision quality;
- sightline occlusion;
- landmark cadence;
- actual travel times;
- NPC/pathfinding suitability;
- R11 depth routes;
- R12 flight/no-fly geometry;
- whether 12 authored regions physically fit the world as intended.

**No additional 20-page prose pass can substitute for this.**

---

# 5.8 Production scope remains very large even though system complexity is controlled

The launch design includes:

- 5 root classes with deep Rank-50 progression;
- 12 large regions;
- dozens of named NPCs;
- regional quest chains and world-state variants;
- multiple mounts;
- housing/camp/fishing/gathering;
- 36–48 fish target range;
- many external/custom creatures;
- high-quality bosses/dungeons;
- multiplayer/reconnect correctness;
- full external-first UI/model/animation/VFX/audio expectations.

The design wisely avoids adding dozens of independent currencies/menus, but **content production volume itself is still AAA/modpack-scale**.

The correct response is not to cut core identity blindly.

It is to prove the production pipeline on one real vertical slice before mass authoring assets for all 12 regions.

R01 is already designed for that role.

---

# 6. External comparison

## 6.1 Horizon Forbidden West — quest/world production

Sources:

- https://blog.playstation.com/2021/11/22/horizon-forbidden-west-an-authentic-world/
- https://blog.playstation.com/2022/08/10/how-guerrilla-created-vegas-in-horizon-forbidden-west/

Observed public production principles:

- settlements are designed around believable culture, work, props, behavior and services;
- progression systems must operate in dialogue with the whole game;
- main-quest design documents contain story beats, objectives, world map/flow and detailed area descriptions;
- designers move into 3D layout early and iterate the real space with art/narrative/environment teams.

Openworld RPG comparison:

**Strong / comparable on paper**

- settlement function/culture is unusually explicit;
- regional resources and services connect to actual livelihoods;
- quest beats/objectives/rewards/state/rejoin are detailed;
- region packages integrate combat/traversal/story/economy.

**Behind production reality**

- no actual Azari 3D/blockout iteration yet;
- NPC work loops are specified but not witnessed in-client;
- visual family is directionally bound but not yet embodied across whole settlements.

## 6.2 Van Buren / Black Isle regional design documents

Publicly leaked historical source:

- Denver design document, 83 pages, Chris Avellone & Sean K. Reynolds;
- covers overview/map flow, background, drama/themes, locations, characters and quests.
- https://fallout.fandom.com/wiki/Denver_design_document

Openworld RPG comparison:

A modern Rxx implementation package + content bible is already similar in **regional-document scope**:

- regional thesis;
- locations/POIs;
- NPC cast;
- quest stages;
- rewards;
- dungeon;
- world aftermath;
- ties to larger game themes.

Openworld RPG goes further in several modern implementation concerns:

- multiplayer ownership;
- reconnect/idempotency;
- performance constraints;
- data-driven contracts;
- explicit external-asset provenance gates.

But the old Denver package exposes an important comparison weakness: **map flow/physical area representation is a first-class artifact**. Openworld RPG's text is ahead of its actual spatial proof.

## 6.3 The Witcher 3 — living world outside the main quest

Source:

- https://www.gdcvault.com/play/1023867/The-Living-World-of-The

CD Projekt RED's public GDC description frames the challenge as making economy, loot and progression create a rewarding open-world RPG even when the player is not following the primary quest.

Openworld RPG is strong here:

- hunting, gathering, fishing, mounts, housing, contracts and dungeons feed common progression/economy;
- most side systems do not create isolated currencies;
- regional resources have cross-system uses.

Risk:

- lightweight optional contracts must not degrade into map-icon chores merely because the underlying systems are well integrated.

## 6.4 Guild Wars 2: Heart of Maguuma — terrain/story/gameplay co-design

Source:

- https://www.guildwars2.com/en/news/gameplay-features-making-the-most-of-maguuma/

ArenaNet publicly describes gameplay, story and terrain as conceived together, with vertical layers containing distinct opportunities and open-world state affecting characters/content.

Openworld RPG matches the **design intent** well:

- R03 vertical highlands;
- R05 canopy/river layers;
- R08 cliffs/roots;
- R11 coast/open/deep layers;
- physical post-quest state changes.

Again, the difference is empirical: those relationships still need the real Azari world.

## 6.5 Subnautica — pillars, emotion and prototype proof

Source:

- https://gdcvault.com/play/1026273/The-Design-of-Subnautica

The public design talk emphasizes:

- explicit design pillars;
- desired emotion;
- prototypes;
- exploration/discovery/the unknown;
- adding structure without over-directing the sandbox.

Openworld RPG already strongly rejects GPS/icon/checklist design and uses landmarks/discovery grammar.

Its weakness is that **design pillars are distributed instead of compressed**, and no playable prototype has yet tested whether the intended feelings survive Minecraft movement/camera/combat.

## 6.6 Wynncraft — closest shipped Minecraft RPG scale comparison

Current public Wynncraft documentation demonstrates a large Minecraft RPG with:

- five primary classes and ability trees;
- quests and questlines;
- professions/gathering/crafting;
- dungeons/raids;
- world events;
- level-banded regional content;
- a central content-book/navigation layer.

Sources:

- https://wynncraft.wiki.gg/wiki/Classes
- https://wynncraft.wiki.gg/wiki/Quest
- https://wynncraft.wiki.gg/wiki/Professions
- https://wynncraft.wiki.gg/wiki/World_Events
- https://wynncraft.wiki.gg/wiki/Raid

Openworld RPG differs deliberately:

- less MMO chore structure;
- no daily objective baseline;
- much lighter professions;
- no separate grind ladder for every activity;
- personal/co-op state rules are more explicitly specified in the design docs;
- region/world consequences are more authored.

But Wynncraft is a warning about **content volume**: a Minecraft RPG of this breadth is not only a code problem. It requires enormous quest, environment, enemy, item, presentation and iteration throughput.

## 6.7 Minecraft boss mods — fight implementation benchmark

### Bosses of Mass Destruction

- public repository: https://github.com/barribob/bosses-of-mass-destruction
- explicit boss implementation/state and custom presentation exist in code;
- boss-specific materials/rewards and structure handling are integrated.

### Mowzie's Mobs

- public source exposes animation-timed body/reach hit logic;
- current public license is All Rights Reserved unless explicitly stated, so project use remains reference-oriented unless terms permit more.
- https://github.com/BobMowzie/MowziesMobs-Public

### L_Ender's Cataclysm

- purpose-built large structures/arenas and high-impact bosses reinforce one another;
- public code exposes explicit structures such as Ruined Citadel, Burning Arena, Sunken City and Cursed Pyramid in supported source generations.

Openworld RPG's boss **design contracts** compare well because they demand:

- readable body-first tells;
- arena relationship;
- real vulnerable states;
- phase geometry changes;
- multiplayer target/state ownership;
- visible hitbox truth.

But the comparison is not won on paper.

Those mods have actual animated bodies and arenas players can fight.

Openworld RPG still needs accepted exact models and Minecraft-scale play proof.

---

# 7. Overall design verdict

## Authoring quality

**VERY HIGH.**

The project has moved beyond ordinary hobby GDD territory. Major systems and regional content are detailed enough that the normal failure mode is no longer `developer must invent the game while coding`.

## Cohesion

**HIGH.**

The project repeatedly connects exploration, combat, economy, progression, settlements and world state. It also shows strong discipline against adding currencies, menus and maintenance loops merely to increase feature count.

## Regional/content variety

**HIGH at gameplay-space level; MEDIUM-HIGH at quest macro-structure level.**

Regions usually play differently, but too many main regional stories still fit the same investigation → infrastructure truth → dungeon → evidence → aftermath macro skeleton.

## Narrative decision neutrality

**GOOD IN TEXT / NEEDS COUNTERWEIGHT IN EXPERIENCED EVIDENCE.**

The final choice wording is fair, but repeated successful local-partition solutions risk making Partition feel pre-validated.

## Production readiness

**NOT YET SOURCE-READY UNDER CURRENT PROJECT CONTRACT.**

The blockers are correctly narrow now:

1. exact external presentation/provenance;
2. actual Azari spatial closure;
3. asset-gated final encounter sheets;
4. stale-document/hidden-choice cleanup.

## Play-proven quality

**NOT TESTED.**

No amount of documentation changes this status.

---

# 8. Recommended next sequence

Do not add another large system or another broad planning layer.

Use this order:

```text
1. stale-canon cleanup in the existing authoritative files
2. obtain/load actual Azari world bytes
3. calibrate real map orientation + exact R01 spatial closure
4. finish R01 exact production asset bindings
5. build one real R01 vertical slice using final visual language
6. client/playtest combat + travel + settlement + quest + economy loop
7. retune seed numbers from observed play
8. prove multiplayer/save authority on the vertical slice
9. only then expand the proven production pipeline across R02–R12
```

In parallel, exact-model intake can continue for true `OPEN_MODEL_SELECTION` boss rows when Azari bytes are unavailable.

---

# 9. What should NOT happen next

Do not respond to this audit by adding:

- more classes;
- more regions;
- more currencies;
- a reputation tree;
- survival meters;
- another crafting profession;
- another giant lore document;
- a separate underwater progression game;
- a prestige/endless-level system;
- temporary player-facing visuals merely to start coding.

The project has enough design.

The next major quality gain comes from **making the existing design collide with real Minecraft space, real models and real hands-on play**.

---

# 10. Verification state

```text
CURRENT MAIN REVIEWED: YES
ACTIVE DESIGN CANON CROSS-READ: YES
R01–R12 REGIONAL AUTHORING CROSS-READ: YES
EXTERNAL PROFESSIONAL / MINECRAFT PRECEDENT REVIEWED: YES
STALE-CANON CONFLICTS FOUND: YES
DESIGN REDESIGNED: NO
CODE REVIEWED: N/A
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

Docs/research audit only. Build/CI is not warranted for this pass.
