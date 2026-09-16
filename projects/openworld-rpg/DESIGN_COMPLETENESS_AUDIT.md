# Open-World RPG — Design Completeness & Game Quality Audit

> Date: 2026-09-16  
> Status: **DESIGN AUDIT / QUALITY CONTRACT**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Region/content references: `REGIONS.md`, `R01_VERTICAL_SLICE.md`, `R02_IMPLEMENTATION_PACKAGE.md`  
> Rule: this document audits completeness and defines how to judge finished-game quality. It does not override gameplay rules in `GAME_DESIGN.md`.

This audit exists because **design completeness, feature completeness and game quality are not the same thing**.

A project may have every system written down and still be boring, awkward, visually inconsistent or exhausting to play. Conversely, a polished vertical slice can feel excellent while the rest of the game is still mostly undefined.

The project therefore tracks two separate questions:

1. **Design closure** — how much important player-facing behavior can be implemented without inventing design during coding?
2. **Game quality** — when implemented, does the whole game actually feel like a cohesive, polished open-world action RPG?

The target remains the repository/playbook contract:

```text
not feature count
not compile success
not document length

but

one cohesive playable game
```

---

# 1. Current source basis

This audit was made after re-reading the current project canon on GitHub `main`, especially:

- `GAME_DESIGN.md`;
- `PROJECT.md`;
- `REGIONS.md`;
- `UI_DIRECTION.md`;
- `LOOT_ECONOMY.md`;
- `EQUIPMENT_BALANCE.md`;
- `MOUNTS.md`;
- `M0_DEPENDENCY_AUDIT.md`;
- `COMBAT_BALANCE.md`;
- `CLASS_COMBAT_KITS.md`;
- `CLASS_PROGRESSION.md`;
- `STATUS_AND_R01_ENCOUNTERS.md`;
- `R01_VERTICAL_SLICE.md`;
- `RECOVERY_PRODUCTION_APPEARANCE.md`;
- `R01_ASSET_INTAKE.md`;
- `GATHERING_FISHING_CAMP_HOUSING.md`;
- `FISHING_COLLECTION_HOUSING_MARKET.md`;
- `QUEST_WORLD_STATE.md`;
- `R02_IMPLEMENTATION_PACKAGE.md`;
- repository `QUALITY_STANDARD.md`;
- the project high-quality playbook.

External comparison material was selected for **structure and production lessons**, not copied content/numbers.

Primary comparison references:

- Guerrilla Games / GDC — `Horizon Zero Dawn: A Game Design Postmortem`;
- Guerrilla Games / GDC — `Balancing Action and RPG in Horizon Zero Dawn Quests`;
- Guerrilla Games / GDC — `Building Non-linear Narratives in Horizon Zero Dawn`;
- Guerrilla Games — `Horizon Zero Dawn: An Open World QA Case Study`;
- CD Projekt RED / GDC — `The Living World of The Witcher`;
- CD Projekt RED / GDC — `Witchcraft: The Alchemy of a Crafting-Based Economy`;
- BioWare / GDC — `Worlds Collide: Combining Story and Systems in Dragon Age: Inquisition`;
- Bethesda Game Studios / GDC — `Level Design in a Day: How We Used Iterative Level Design to Ship Skyrim and Fallout 3`;
- Bethesda Game Studios / GDC — `Fallout 4's Modular Level Design`;
- Obsidian / GDC 2026 — `Designing POIs (Points of Interest) for The Outer Worlds 2`;
- GDDKit `Lumenfall` worked GDD example, used only as a document-coverage cross-check.

Useful source URLs:

- https://www.guerrilla-games.com/read/horizon-zero-dawn-a-game-design-postmortem
- https://www.guerrilla-games.com/read/balancing-action-and-rpg-in-horizon-zero-dawn-quests
- https://www.guerrilla-games.com/read/building-non-linear-narratives-in-horizon-zero-dawn
- https://www.guerrilla-games.com/read/horizon-zero-dawn-an-open-world-qa-case-study
- https://www.gdcvault.com/play/1023867/The-Living-World-of-The
- https://www.gdcvault.com/play/1022800/Witchcraft-The-Alchemy-of-a
- https://www.gdcvault.com/play/1022377/Worlds-Collide-Combining-Story-and
- https://www.gdcvault.com/play/1020171/Level-Design-in-a-Day
- https://www.gdcvault.com/play/1022930/-Fallout-4-s-Modular
- https://www.gdcvault.com/play/1035724/Designing-POIs-%28Points-of-Interest%29

---

# 2. Important lessons from external open-world RPG production

## 2.1 A large world is not content

Bethesda's open-world level-design material repeatedly treats player motivation, landmarks, POIs, iteration and content density as design problems rather than assuming terrain size is enough.

Project consequence:

- Azari's 30k x 30k size is **not** itself a quality advantage;
- each traveled route needs readable goals, landmarks, risk/reward and deliberate distraction;
- empty scenery may exist for breathing room, but long accidental dead travel is not acceptable;
- region design needs a measurable POI/travel cadence rather than only a biome description.

Current project status:

- R01 has a real route and content cadence;
- R02 is significantly better specified;
- R03–R12 do not yet have enough concrete density/travel contracts.

## 2.2 Story-only and system-only open-world content both fail when isolated

The Dragon Age: Inquisition GDC talk describes a production problem highly relevant here: discrete narrative content alone scaled poorly, while standalone systems felt disconnected from the world. The useful solution is to combine authored context with reusable systemic gameplay.

Project consequence:

A regional package is not complete merely because it contains:

```text
mobs + materials + boss + dungeon
```

It also needs:

```text
who lives here
what changed here
why the player cares
what local conflict or mystery gives the systems meaning
what the player can change/discover
what world or character memory remains afterward
```

R01 and R02 already partially satisfy this through route context, clues, settlement roles and dungeon identity. Later regions are not yet sufficiently authored at this level.

## 2.3 Economy should bind the world, not sit beside it

The Witcher 3 economy talks frame money/crafting/progression as a mechanism that connects world activity rather than as isolated menus.

Current project status is strong here:

- exploration feeds materials;
- materials feed forge/alchemy/cooking/camps/housing;
- combat feeds equipment/signature materials;
- Gold connects merchants, class switching, housing and services;
- fishing feeds collection, cooking, sale and display;
- boss materials feed deterministic signature crafting.

Risk to continue watching:

- later regional materials must not become isolated `R07-only token A` items;
- late-game Gold sinks must remain meaningful without becoming repair/tax chores;
- no region should introduce a new currency merely to make itself look deep.

## 2.4 Quest architecture needs non-linearity as a system, not exceptions

Horizon Zero Dawn's public quest-system material explicitly emphasizes non-linearity and a common language for simple/complex quest state.

Current project status is strong at the framework level because `QUEST_WORLD_STATE.md` already defines:

- personal progression;
- shared encounter state;
- late joining;
- contribution credit;
- split-party behavior;
- idempotent rewards;
- dialogue/choice authority.

Remaining gap:

- the project lacks enough **authored main/regional narrative content** using that framework.

The engine of the quest system is ahead of the actual story carried by it.

## 2.5 POIs should intersect worldbuilding, progression, spatial design and navigation

The Outer Worlds 2 POI talk gives a useful four-axis check:

1. worldbuilding;
2. progression;
3. spatial design;
4. navigation.

Project adoption:

Every important authored POI should be reviewed against all four axes.

A POI that is only:

```text
chest behind ruin
```

is weak even if the model is pretty.

A strong POI should answer several questions at once:

- what does this place tell me about the region?
- why is it worth reaching?
- what choice/risk/gameplay happens there?
- how did I notice/find it?
- what route or landmark relationship does it create?
- does it connect to a resource, enemy, quest, shortcut, equipment source, lore clue or later return?

## 2.6 Massive worlds require modular production without modular-looking repetition

Fallout 4 / Skyrim production talks show the value of modular kits and rapid iteration for large worlds.

Project consequence:

External modular architecture/prop families are a major advantage, but their use must be governed by **composition identity**.

Reuse:

- wall/roof/prop kits;
- encounter controller patterns;
- quest objective primitives;
- merchant/service components;
- UI components;
- data schemas.

Do not visibly reuse:

- the same room layout with different stone;
- the same boss pattern with a different model;
- the same three-POI composition in every region;
- the same settlement service layout with palette swaps.

Production reuse should be invisible to the player where possible.

## 2.7 Open-world QA is a design concern

Guerrilla's open-world QA case study emphasizes risk management, exploratory testing, telemetry and automated support for a huge state space.

Project consequence:

`build succeeds` cannot be the main completion proof.

The final project needs local/dev-only telemetry and targeted scenario testing for:

- route completion;
- sequence breaking;
- quest state;
- reward duplication;
- player split/regroup;
- save/load during events;
- chunk unload/reload;
- encounter reset;
- economy exploits;
- inventory overflow;
- travel dead time;
- performance hotspots;
- multiplayer state disagreement.

No external analytics service is required; local debug/session summaries are sufficient for private play.

---

# 3. Design maturity scale

Use this only to measure **decision closure**, not fun.

## D0 — absent

The player-facing need is not defined.

## D1 — direction only

Fantasy/intent/reference exists, but implementation would still require major design decisions.

## D2 — system rules

Main behavior is defined, but content, exceptions, numbers, presentation or state ownership still has meaningful gaps.

## D3 — implementation-ready

A competent implementer can build the subsystem without inventing core gameplay rules.

Expected:

- inputs/outputs;
- numbers/defaults;
- important edge cases;
- multiplayer/server authority where relevant;
- UI/presentation direction;
- data ownership;
- acceptance criteria.

## D4 — production-bound design

D3 plus the actual player-visible content required for the planned slice is substantially bound:

- selected external model/UI/animation/audio sources where relevant;
- exact content roster/data;
- source/license status;
- final world placement/composition direction;
- testable acceptance cases.

D4 still does **not** mean implemented or playtested.

---

# 4. Current planning-completeness audit

The current project is unusually detailed in systems, but much less complete in whole-game authored content.

Approximate maturity is intentionally given as a range, not false precision.

## 4.1 Strong / near-implementation-ready areas

| Area | Current maturity | Notes |
|---|---|---|
| product identity / core loop | D4 | strong, consistent, easy to explain |
| combat math / dodge / guard / poise | D4 | exact timing/formulas/TTK/authority exist |
| root classes / first branches / class progression | D4 | unusually detailed before code |
| loot / gear / affix / signature protection | D4 | source ownership and anti-confetti philosophy clear |
| inventory / material pouch / key items | D3–D4 | capacity and overflow rules defined |
| recovery / food / alchemy / cooking | D3–D4 | R01 concrete, later content still expands |
| mounts | D3–D4 | rules/progression/source direction strong |
| quest/world-state framework | D4 | personal/shared/encounter authority well defined |
| gathering / fishing / camp / housing rules | D3–D4 | new fishing collection and one-home trade-up direction closes major gaps |
| multiplayer authority model | D4 design | actual multiplayer is still untested by definition |
| R01 gameplay route | D4 design | strongest content package; asset intake remains incomplete |
| R02 regional flow | D3–D4 | implementation package exists; exact asset acceptance still pending |

## 4.2 Partially closed areas

| Area | Current maturity | Main gap |
|---|---|---|
| UI/UX | D3 | visual family and architecture exist; not every major screen has final mockup/real-client proof |
| world map / navigation | D2–D3 | general rules exist; whole-world landmark/POI density and route-time budget missing |
| factions/reputation | D1–D2 | rule says `selected meaningful factions`; actual factions/relationships mostly absent |
| day/night/weather | D1–D2 | principle exists; concrete per-region content/weather tables missing |
| global audio/music | D1–D2 | R01 audio direction exists; whole-game adaptive music/ambience system absent |
| external asset closure | D2 | strong source research, but many exact model/hash/visual acceptance gates remain |
| later-region economy/content | D1–D2 | broad identities exist; actual resources, shops, gear pools and service differences mostly absent |
| open-world QA strategy | D2 | standards exist, but project-specific risk matrix/telemetry/route test plan is not yet a canon artifact |

## 4.3 Major under-authored areas

| Area | Current maturity | Why it matters |
|---|---|---|
| main narrative spine | D0–D1 | the player has systems and regions, but no concrete whole-game purpose/arc |
| core NPC cast / character relationships | D0–D1 | settlements risk feeling like service kiosks rather than a world |
| actual factions / political-social geography | D0–D1 | reputation framework exists without enough authored targets |
| region narrative capsules R03–R12 | D1 | later regions currently risk becoming biome + monster + dungeon packages |
| implementation-ready R03–R12 | D1–D2 | R03 is in progress; most later regions remain broad |
| whole-world POI density / travel pacing budget | D0–D1 | Azari size could create long dead travel or uneven density |
| final act / climax / ending | D0 | no whole-game closure target |
| Lv80 / post-final-boss endgame role | D0–D1 | level cap exists but endgame purpose is not yet clearly authored |
| accessibility / difficulty / input-assist canon | D0–D1 | no clear final player-options contract |
| complete keybind/control map | D1 | explicitly deferred, correctly, but still unresolved |
| save/version migration / corrupted-state recovery design | D1 | important for a long private world and multiplayer persistence |

---

# 5. Overall current design-completeness verdict

## System design closure

**Approximately 85–90%.**

The project is already far past ordinary concept-stage design in combat, progression, inventory, economy, classes, field systems and server authority.

## R01 vertical-slice design closure

**Approximately 85%.**

The remaining gap is dominated by:

- exact asset acquisition/binding;
- final visual conversion;
- final UI mockup/client validation;
- actual implementation/playtest.

The gameplay rules themselves are mostly closed.

## Whole-game authored-content closure

**Approximately 35–45%.**

Reason:

- R01 is detailed;
- R02 now has an implementation package;
- R03 is still in planning;
- R04–R12 remain largely regional direction rather than final production content;
- narrative/faction/endgame content is substantially under-authored.

## Whole-project planning maturity

**Approximately 65–75%, with ~70% as the practical center estimate.**

This number should never be read as `the game is 70% complete`.

It means roughly:

> most reusable rules are closed, but a large amount of authored world content and final presentation binding still needs design work.

A useful mental model:

```text
systems: ahead
R01/R02: healthy
later world content: behind
narrative/characters/endgame: clearly behind
presentation asset closure: behind systems
actual implementation/playtest: not started / not proven
```

---

# 6. The largest missing design decisions

## 6.1 World narrative spine

Before writing ten more independent region packages, define the minimum whole-game narrative spine.

Need:

- world premise/current crisis or central change;
- why the player begins in R01;
- what keeps the player moving beyond personal power gain;
- what the player gradually learns about the continent;
- how R01–R12 relate to the central problem;
- what changes at major act boundaries;
- what the final confrontation/problem actually is;
- what `finishing the game` means;
- what remains explorable afterward.

The main story should remain compatible with the 30/70 guided/exploration direction. It does **not** need to become a linear cinematic RPG.

## 6.2 Player-avatar identity

One significant user-level choice remains unresolved:

```text
A. mostly blank/custom adventurer whose identity comes from the player's actions
B. lightly authored protagonist with a defined background but broad role-play freedom
C. strongly authored named protagonist
```

The current Minecraft/custom-class structure naturally supports A or B better than C, but this should be deliberately locked because it affects dialogue, voice, relationships and quest writing.

## 6.3 Core NPC/faction cast

Need a small memorable cast before dozens of service NPCs are authored.

At minimum define:

- R01 recurring anchor NPCs;
- major faction or institution representatives;
- one or more recurring rivals/allies/mentors where useful;
- which NPCs travel or reappear across regions;
- what changes in their behavior based on major choices/progress;
- which NPCs are service-only and deliberately not pretending to be major characters.

Do not create 40 reputation bars.

## 6.4 Regional narrative capsule

Every R03+ implementation package must include:

```text
central local tension / mystery
2–4 named local characters or role anchors
one major regional chain
one optional narrative/discovery chain
one world-state or relationship consequence where appropriate
how the dungeon/boss relates to the region rather than merely living there
what the player learns about the larger world
```

Exact counts may vary. The requirement is **meaning**, not quota fulfillment.

## 6.5 World density / travel pacing

Need a dedicated spatial budget after Azari is actually imported.

Measure in **travel time**, not only blocks.

Initial playtest targets to test, not blindly enforce:

```text
major visual landmark or route decision: usually visible/encountered within ~30–90 s of ordinary travel
meaningful optional interaction/POI opportunity on primary exploration routes: roughly every ~1–3 min
major authored POI / settlement / dungeon / field boss / major event: enough spacing to feel distinct, usually several minutes apart
```

The correct result may differ per region:

- R01 should be readable and relatively dense;
- R02 can hide more content behind forks;
- R03 can use long sightlines and vertical detours;
- desert/ocean regions may intentionally create longer stretches, but those stretches need navigation spectacle, risk or destination readability.

Do not make every 60 seconds contain a chest/encounter. Density is rhythm, not clutter.

## 6.6 Endgame and closure

Before implementation gets far, define:

- final major encounter/content sequence;
- expected Lv/Item Lv at first completion;
- whether the world remains playable afterward;
- repeatable endgame targets;
- what Class Rank/build progression remains relevant after Lv80;
- what optional world bosses/dungeons remain aspirational after story completion;
- whether there is any NG+ concept at all.

Default recommendation unless a later design proves otherwise:

- world remains playable after ending;
- no mandatory NG+ at launch;
- endgame consists mainly of hard optional bosses/dungeons, build completion, collection/housing/fishing/codex and class mastery;
- no endless numeric prestige treadmill.

## 6.7 Accessibility / difficulty / control settings

Need explicit player options because action combat contains tight timing.

At minimum evaluate:

- remappable project keybinds;
- hold/toggle options for sprint/guard/aim where relevant;
- camera shake strength;
- hit-stop/camera impact reduction;
- subtitle/dialogue readability;
- UI scale compatibility;
- color-independent rarity/status communication;
- optional stronger attack-telegraph visibility;
- optional perfect-guard/dodge timing assistance only if it can be implemented without breaking authoritative multiplayer;
- difficulty presets or selected assists versus a single fixed difficulty.

Do not finalize arbitrary assists before combat playtest. First define what can safely be adjusted.

## 6.8 Global audio/music system

R01 has audio directions, but the whole game needs a music/ambience contract.

Need:

- exploration layers by region;
- settlement layer;
- danger/combat escalation;
- elite/boss music ownership;
- dungeon transition;
- discovery/major reward stingers;
- night/weather variation where useful;
- crossfade rules;
- silence/breathing-space policy;
- sound-priority/mix rules so telegraphs remain audible.

External audio remains provenance-tracked.

---

# 7. Required regional implementation package from R03 onward

Every region does **not** need identical amounts of content, but an implementation-ready package must answer the same categories.

## Identity

- one-sentence player fantasy;
- suggested entry Lv;
- why the region is not a reskin of a previous region;
- visual/terrain source direction;
- 3–5 macro landmarks/navigation anchors.

## Traversal

- primary movement challenge/opportunity;
- mounts usable/not usable and why;
- shortcuts/unlocks;
- what prevents terrain from becoming annoying rather than interesting.

## Settlement/social layer

- settlement role and size;
- essential services;
- services deliberately absent;
- housing tier/opportunities where relevant;
- NPC/faction anchors;
- local economy identity.

## Ecology/combat

- passive/ambient ecology;
- common threats with distinct behavior roles;
- sturdy/elite roles;
- optional hunt/field boss;
- dungeon boss;
- actual external model/dependency direction;
- no enemy exists solely because a model was available.

## Resources/fishing/production

- reused prior resources;
- only a small number of justified new resources;
- fish where water ecology supports it;
- recipes/services/rewards that make those materials useful;
- no dead regional material.

## Exploration/POIs

For each important POI, cover:

```text
worldbuilding
progression/reward
spatial gameplay
navigation/discovery
```

Region should include more than combat POIs.

Possible roles:

- dangerous resource pocket;
- environmental story site;
- puzzle/traversal space;
- hunt clue;
- mini encounter;
- rare fishing location;
- hidden shrine/shortcut;
- social/trade site;
- lore/skill discovery;
- treasure with a meaningful source identity.

## Narrative

- local tension/mystery;
- regional quest chain;
- key NPC roles;
- relation to main narrative;
- discoverable environmental storytelling;
- at least one optional story/discovery beat;
- relevant choice/consequence where it adds value.

## Rewards/growth

- equipment identity;
- skill/class/discovery relevance;
- first-clear deterministic protection where needed;
- boss signature path;
- onward-region reason.

## Presentation

- architecture kit;
- creature/model sources;
- animation/VFX/audio direction;
- region ambience/music;
- UI-specific needs if any.

## Runtime / multiplayer

- personal/shared states;
- encounter reset behavior;
- sequence-breaking safety;
- chunk unload/reload behavior;
- performance risks;
- multiplayer participation/reward rules.

## Acceptance

- minimum actual play route;
- expected active time;
- intended memorable moments;
- obvious fail symptoms;
- test commands/setup when implementation begins.

---

# 8. Whole-game quality model

The project should not declare completion from a single number, but a weighted score helps expose weak areas.

Use eight quality dimensions after implementation.

| Dimension | Weight | What it asks |
|---|---:|---|
| Core feel / controls / moment-to-moment feedback | 18 | Is simply moving, fighting, interacting and looting satisfying? |
| World / exploration / spatial pacing | 15 | Does travel create curiosity, readable goals and meaningful discovery rather than dead distance? |
| Combat / enemy / boss quality | 15 | Are enemy roles, tells, hitboxes, reactions and encounter decisions strong? |
| Progression / economy / build decisions | 10 | Do rewards create new choices without grind/currency clutter? |
| Content / narrative / regional identity | 12 | Are regions memorable and meaningful rather than asset-filled biomes? |
| Presentation — UI/art/animation/VFX/audio | 15 | Does it look/sound like one intentional game? |
| UX / onboarding / accessibility | 5 | Can players learn, read and comfortably control the game? |
| Technical / stability / save / performance / multiplayer | 10 | Does the game remain trustworthy under real play? |

Total: 100.

## Important: weighted score cannot hide a fatal weakness

A 90 in art cannot compensate for broken save data.
A 95 in combat cannot compensate for an empty world.
A 90 average cannot compensate for multiplayer duplicating boss rewards.

Therefore use **quality gates** as well as the score.

### Candidate high-quality completion gate

Before calling the game broadly complete:

- no critical dimension is below 70/100;
- Core Feel, World/Exploration, Combat and Presentation should target **85+** because they define this project's identity;
- the main route is playable from start to end without developer intervention;
- no mandatory progression state can softlock;
- no player-facing placeholder assets/UI remain;
- major external-source/license state is resolved for the actual playable build/repo boundary;
- save/load works across major quest/dungeon/boss/housing states;
- multiplayer reward/progression authority is actually tested before claiming it works;
- worst-case performance is measured in the heaviest real region/settlement/boss scenarios;
- actual playtests confirm that the designed loops are fun rather than merely numerically compliant.

These are targets, not marketing scores.

---

# 9. Quality must be judged at several time scales

An open-world RPG can feel good for ten minutes and become dull after ten hours. Review quality at multiple horizons.

## 9.1 Ten-second quality

Ask during ordinary play:

- movement responsive?
- attack/guard/dodge readable and satisfying?
- camera stable?
- hit reaction/sound/VFX convincing?
- interaction prompt obvious without visual clutter?
- pickup/reward feedback clear?

If the answer is no, adding another region does not help.

## 9.2 Ten-minute quality

Within a normal ten-minute slice:

- did the player make at least one meaningful choice?
- did something visually/gameplay-wise change?
- was there a reason to deviate from the road?
- did the player obtain/use a meaningful reward or discovery?
- was there avoidable dead travel/menu management?

Not every ten minutes requires combat or loot, but the player should remain intentionally engaged.

## 9.3 One-hour quality

A representative hour should naturally touch several connected loops, for example:

```text
travel/exploration
→ encounter or discovery
→ resource / quest / dungeon progress
→ reward
→ build/service choice
→ different next destination
```

Audit:

- too many repeated enemy groups?
- too much travel with no decisions?
- too many town returns?
- inventory cleanup too frequent?
- quests feel like errands?
- rewards actually affect choices?
- fishing/housing/gathering remain optional pleasures rather than obligations?

## 9.4 Five-to-ten-hour quality

This horizon exposes shallow systems.

Check:

- do classes/builds materially diverge?
- do new regions change tactics/traversal/ecology?
- are earlier resources/services still relevant?
- have bosses become pattern variants of the same fight?
- are new items mostly numeric upgrades or new decisions?
- is map exploration still curious rather than checklist cleanup?

## 9.5 Full-playthrough quality

Check:

- pacing rises/falls intentionally;
- the main narrative provides direction without suffocating exploration;
- late regions still introduce real novelty;
- Lv80 feels earned rather than grindy;
- the final act/final encounter provides closure;
- optional systems remain relevant but not mandatory;
- there is a satisfying reason to continue after the ending, or a satisfying reason to stop.

---

# 10. Feature completion ladder

A feature is not `complete` just because code exists.

Use this ladder internally:

## S — SPECIFIED

Rules/content/presentation/state/acceptance are defined.

## I — IMPLEMENTED

Core code/data exists and is code-reviewed.

## G — INTEGRATED

Works with relevant neighboring systems.

Examples:

- fishing integrates inventory/cooking/sale/codex/housing;
- boss integrates quest/reward/loot/signature craft/multiplayer;
- housing integrates economy/storage/furniture/permissions.

## P — PLAYTESTED

Actually used in Minecraft under realistic conditions.

## Q — POLISHED

Feel, visual, audio, UX, edge cases and performance meet the project standard.

A whole subsystem is only broadly complete when its important paths reach at least `Q`.

Use existing verification vocabulary alongside this ladder:

- CODE REVIEWED;
- TESTED;
- BUILD VERIFIED;
- JAR PRODUCED;
- PLAYTESTED;
- MULTIPLAYER TESTED.

Never collapse these into one fake `done` state.

---

# 11. Project-local quality telemetry / observations

No external analytics platform is required.
During development/playtest, local debug/session summaries can measure:

## Travel/exploration

- minutes between meaningful decisions/interactions;
- time spent only traveling;
- POIs visited per hour;
- route backtracking;
- fast-travel frequency;
- regions/routes players repeatedly avoid.

## Combat

- ordinary/elite/boss encounter duration;
- damage/death cause distribution;
- dodge/guard/perfect-guard usage;
- poise break frequency;
- healing-belt usage;
- attacks that miss players visually but still hit, or visually hit but fail.

## Progression/economy

- EXP sources by category;
- Gold income/sinks;
- items immediately sold/discarded;
- materials that accumulate with no use;
- time to meaningful equipment improvement;
- forge/merchant/crafting usage;
- class-switch/respec frequency.

## UX

- inventory-full events;
- time in inventory/menus;
- abandoned contracts;
- missed tutorial concepts;
- repeated failed interaction attempts;
- accidental key conflicts.

## Technical

- server tick time hotspots;
- entity counts by region;
- display/VFX counts;
- chunk load spikes;
- save/load failures;
- duplicate reward attempts;
- multiplayer state mismatches.

Metrics inform diagnosis. They do not replace observation of actual feel.

---

# 12. Whole-game content-density rules

Avoid both empty-world and icon-map extremes.

Each region should intentionally mix:

- calm traversal;
- visual destination;
- spontaneous ecology;
- meaningful optional detour;
- authored quest/discovery;
- dangerous encounter;
- settlement/service recovery;
- dungeon/major challenge.

Do not require equal density everywhere.

A mountain ridge, desert, ocean or haunted ruin may use emptiness intentionally, but the emptiness must create:

- anticipation;
- scale;
- navigation choice;
- spectacle;
- danger;
- resource opportunity;
- destination visibility.

If it creates none of these, it is dead travel.

---

# 13. Avoiding region formula fatigue

The new regional-package contract is a coverage checklist, **not** a mandate that every region contains the same content count.

Avoid:

```text
1 town
3 quests
4 common mobs
1 elite
1 field boss
1 dungeon
repeat x12
```

Instead deliberately vary region structure.

Examples:

- R03 can center vertical route mastery and a mining town;
- R04 can feel like an expedition with one strong refuge and dangerous long-distance ice travel;
- R06 can use river/boardwalk movement and partially flooded locations;
- R07 can make visible giant threats and oasis/caravan routes more important than dense POI count;
- R11 can be layered maritime progression with ports/islands/reefs/abyss;
- R12 can intentionally reduce ordinary services and focus on extreme anomaly/exploration pressure.

The player should recognize the shared game's rules but not predict the exact region template.

---

# 14. Explicitly acceptable omissions

A complete game does **not** need every common RPG feature.

The following are optional unless later playtest proves they strengthen the core:

- romance system;
- permanent AI party companions;
- giant settlement-building system;
- farming simulator;
- survival hunger/thirst;
- durability/repair grind;
- daily/weekly quests;
- battle pass/live-service progression;
- prestige/infinite Lv treadmill;
- procedural random dungeons;
- dozens of reputation bars;
- universal loot salvage currency;
- mandatory NG+;
- PvP.

Absence is better than a shallow disconnected version.

---

# 15. Revised planning order after this audit

The previous queue was too region-forward relative to the missing narrative/world-density foundation.

Recommended order now:

## A. World narrative spine + player identity

Create a concise but production-usable document covering:

- player identity model;
- opening premise;
- main-world conflict/mystery;
- act progression;
- core recurring cast/factions;
- how major regions feed the central arc;
- climax/ending/postgame state.

Do this **before** fully authoring R04–R12 so later regions do not become disconnected content islands.

## B. World density / POI / traversal pacing contract

After/alongside actual Azari import inspection:

- landmark strategy;
- travel-time bands;
- POI roles;
- density measurement;
- calm-space rules;
- mount/fast-travel interaction;
- route readability.

## C. Finish R03 using the new regional package contract

R03 remains the next implementation-ready regional package, but now it must include its narrative/social/POI layer, not only enemies/resources/dungeon.

The already identified correction remains sound:

- do not force Basalt Wyvern into R03 merely because a model exists;
- keep basalt/volcanic identity for a region where it visually/ecologically fits better;
- choose R03 field/dungeon boss only after external-source/model/animation review.

## D. R04–R12 implementation packages

Build them in manageable batches and reuse proven production schemas without copy-pasting region gameplay.

## E. Global audio/music + accessibility/control contract

Close before broad implementation makes retrofitting expensive.

## F. Endgame/completion contract

Lock before high-level regions and final bosses are implemented.

## G. Project-specific QA / telemetry / save-state risk matrix

Create before content implementation expands enough to create an unmanageable state space.

---

# 16. User decisions that should not be guessed

Most gaps can be solved through research and project logic. A small number materially affect the game's identity and deserve explicit user choice.

The first important one is **player-avatar authorship**:

- blank/custom adventurer;
- lightly authored background;
- strongly authored protagonist.

A second later choice may be the tone/importance of major narrative choices:

- mostly exploratory story with limited branch consequences;
- moderate personal/regional choices;
- heavily branching story with major mutually exclusive outcomes.

Do not ask these repeatedly. Once locked, record them in the master narrative canon.

---

# 17. Audit conclusion

The project is **not under-designed in mechanics**.

Its current weakness is the opposite:

> reusable systems are approaching production readiness faster than the whole world has acquired authored meaning, density and closure.

The next improvement in quality will not come from inventing another progression menu.

It will come from:

- giving the continent a narrative reason to exist;
- giving regions recurring people, local problems and consequences;
- measuring actual travel/content rhythm;
- completing later-region packages with the same depth as R01/R02;
- locking endgame/ending;
- closing final presentation/audio/accessibility gaps;
- then validating all of it through real Minecraft play rather than document confidence.

The project should therefore preserve its strong system canon while shifting planning effort toward **world meaning + content density + completion criteria** before broad source implementation.
