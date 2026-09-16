# Open-World RPG — R09 Southstone Marches Content Bible

> Status: **DESIGN CANON — settlement, named cast, regional/main quests, signal network, Caravan Elephant qualification, rewards, scenes, Act-III evidence and reconnect behavior locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Region/system package: `R09_IMPLEMENTATION_PACKAGE.md`  
> Story: `WORLD_STORY_CANON.md`  
> Quest/state: `QUEST_WORLD_STATE.md`  
> Cross-region identity: `REGION_CROSS_AUDIT.md`  
> Mounts: `MOUNTS.md`  
> Rule: this file closes R09 content-authoring blanks. Existing R09 traversal/ecology/combat/dungeon contracts remain authoritative unless explicitly refined here. `GAME_DESIGN.md` wins conflicts.

R09 is the grounded counterpart to R08. Its evidence comes primarily from **current people-built roads, signals, depots, standards and agreements**, not from another sequence where the player discovers that an ancient ruin knew everything first.

Exact external model/animation/VFX/audio filenames, hashes, final field-boss identity and Azari coordinates remain **pre-code gates**, never implementation discretion.

---

# 0. Player-facing language rule

No player-facing R09 copy may expose development-stage terminology, internal state IDs, asset/license/hash notes, acceptance-test language or implementation wording.

The game presents roads, signals, agreements, shortages and people in world language only.

---

# 1. Primary settlement — Stoneway Fort

Final player-facing settlement name: **Stoneway Fort**.

Stoneway is a caravan fort and foundry outpost built where three major roads split toward the dry interior, rocky coast and Cinderfall-facing industrial route.

Visible identity:

```text
main caravan road
→ outer herd / wagon yard
→ Stoneway gate
→ Foundry Court
   ├─ route marshal / signal board
   ├─ foundry and smith hall
   ├─ Elephant handler yard
   ├─ merchant / Material Vault
   ├─ inn / rest house
   ├─ repair depot
   └─ three outward roads with visible signal towers
```

The fort must visibly function before dialogue explains it: loading, harness inspection, metal sorting, route boards, bells/horns, standardized repair parts and caravans moving in/out.

---

# 2. Named R09 cast

| Character | Role | Normal anchor | Function |
|---|---|---|---|
| **Kael Marr** | Route Marshal | signal hall | primary regional quest owner; roads/signals/redundancy perspective |
| **Branna Orel** | Foundry Master | main foundry | production standards, metal economy and logistics evidence |
| **Ysra Bell** | Elephant Handler / Caravan Keeper | herd yard | Caravan Elephant qualification and registration |
| **Hollis Venn** | depot engineer / road repairer | repair depot | viaduct, depots and physical shortcut work |
| **Meren Doss** | innkeeper / cook | Roadhouse Ember | rest, food, traveler context |
| **Ova Serr** | merchant / Material Vault clerk | trade arcade | regional trade/material services |
| **Sera Wren** | recurring cartographer / ranger | route tower | compares old route sensors with modern locally maintained road knowledge |
| **Daren Holt** | recurring engineer/smith | foundry/dungeon investigation | interprets old logistics hardware without displacing local workers |
| **Ilyan Voss** | recurring Anchor scholar | lower-vault investigation | establishes historical centralized allocation after physical evidence exists |
| **Kest Arden** | rival wanderer | optional ridge/hunt scenes | continuity, no mandatory progression role |

No vanilla villagers are final population.

---

# 3. R09 pacing / reward benchmark

Suggested entry Lv remains **44**; dungeon climax around Lv50.

```text
direct regional chain + first fortress clear: ~100–140 min
regional chain + Elephant qualification + optionals: ~160–230 min
broad first-visit completion including major hunt: ~3.5–4.5 h
```

Main progression never requires purchasing the Elephant.

---

# 4. Main regional chain — Three Roads South

Final title: **Three Roads South**.  
Quest owner: **Kael Marr**.  
Category: Regional Main / Act-III eligible route.  
Permanent failure: none.

Opening line:

> “Three roads leave Stoneway. None of them need a miracle. They need signals, spare parts and people who can trust the next depot is still there.”

## Required sequence

1. discover Stoneway Fort;
2. inspect the active local route board;
3. choose and visit **one of two** first-route problems:
   - Three-Road Signal Ridge, or
   - Broken Foundry Viaduct;
4. resolve that location's authored route problem;
5. inspect the local manual record showing how traffic was rerouted around the failure rather than stopped entirely;
6. return to Kael or complete the field handoff.

Reward:

```text
EXP: 40% of current next-Lv requirement
Gold: 540
Class XP: 22% of current Class Rank requirement
```

Kael completion line:

> “Slower road, longer shift, extra depot. We lose efficiency and keep moving. That's why there are three roads.”

Unlocks **Signals Without Masters**.

---

# 5. Main regional chain — Signals Without Masters

Final title: **Signals Without Masters**.  
Quest owners: **Kael Marr** and **Hollis Venn**.

There are three major signal nodes:

1. **Three-Road Ridge** — panorama + route warning;
2. **Coastwatch Post** — coastal shelf / storm and caravan visibility;
3. **Foundry Viaduct Signal** — industrial road / bridge status.

The player must restore **2 of 3** in any order.

Each node requires reaching a real POI and solving its distinct condition:

- Ridge: clear/resolve a heavy territorial threat and repair the signal linkage;
- Coastwatch: restore the post after a route/weather or structural failure;
- Viaduct: restore physical access/relay after the repair-route encounter.

The final activation is one concise interaction, not a tower minigame.

After 2 distinct nodes:

```text
EXP: 45% of current next-Lv requirement
Gold: 620
Class XP: 25% of current Class Rank requirement
```

World result:

- local road warnings become clearer;
- selected caravan event weighting improves;
- at least one route shortcut becomes usable;
- traveling merchant/caravan presentation increases.

Sera line:

> “The old sensors are still useful. The useful part is the reading. Stoneway decides what to do with it.”

Unlocks **The Weight of a Caravan** and the old-fortress lead.

The unchosen third signal remains optional and awards its normal POI/event reward without being required for main progression.

---

# 6. Main regional chain — The Weight of a Caravan

Final title: **The Weight of a Caravan**.  
Quest owners: **Ysra Bell** and **Kael Marr**.

This quest demonstrates the modern logistics system and grants **Elephant qualification**, but paying for personal registration is optional.

## Required sequence

1. meet the working Elephant herd at Stoneway;
2. help clear or bypass one authored blockage on the short depot-transfer route;
3. perform the handling interaction with Ysra's assigned caravan Elephant;
4. ride/operate the Elephant through a **short authored transfer route**, not a long escort;
5. use one committed charge in the training/route context to break or push through an approved obstacle/space after a clear prompt;
6. enter the destination depot volume with the transfer state intact;
7. return/control handoff completes and **Caravan Elephant Qualified** commits.

Reward:

```text
EXP: 35% of current next-Lv requirement
Gold: 420
Class XP: 20% of current Class Rank requirement
Caravan Elephant registration access: unlocked
```

Ysra line:

> “You know its width, its turn, and when not to ask for a charge. That's enough. If you want one registered to you, the harness office can do it.”

Personal registration remains exactly:

```text
Gold: 1,500
```

Registration is not required for `The Old Allocation`, Act-III progression or any regional evidence.

---

# 7. Optional contracts

## 7.1 Parts by Measure

Giver: **Branna Orel**.

Objective:

- obtain 3 valid R09 forge-material units from authored regional sources;
- inspect one standardized depot repair rack;
- deliver the material to the foundry.

Reward:

```text
EXP: 18%
Gold: 320
Class XP: 10%
```

The point is standardized local repair, not another crafting tutorial.

## 7.2 Herdstone Passage

Giver: **Ysra Bell** or route board.

Objective:

- resolve one authored large-animal road conflict without requiring a kill;
- valid solutions may include waiting/redirecting through the authored interaction, clearing a hostile pressure source or defeating an actual aggressive threat.

Reward:

```text
EXP: 18%
Gold: 300
Class XP: 10%
```

## 7.3 The Third Signal

Available only after `Signals Without Masters` completes with one node untouched.

Objective: restore the remaining signal node.

Reward:

```text
EXP: 20%
Gold: 340
Class XP: 12%
```

This produces full 3-node regional signal coverage but is not required for the main story.

---

# 8. Optional major hunt

The final field-boss creature/name remains an **asset-intake gate** because the implementation package explicitly rejects a scaled common animal.

Before source bootstrap, asset intake must lock:

- exact creature/model;
- final player-facing hunt title;
- anatomy-supported attack table;
- signature material/trophy identity.

The authored gameplay slot is already fixed:

- Lv49 major dryland hunt;
- broad open terrain;
- committed movement/impact identity;
- no main-story gate;
- global field-boss first/repeat reward rules.

Implementation may not choose or improvise this boss.

---

# 9. Main regional dungeon — The Old Allocation

Final title: **The Old Allocation**.  
Category: Regional Main / Act-III evidence.  
Suggested content band: Lv47–50.  
Permanent failure: none.

First-clear route:

```text
battlement approach
→ caravan yard / logistics hall
→ foundry
→ lower vault / control archive
→ Executioner / guardian chamber
```

## Required evidence

The player must encounter:

1. **Sensor Ledger** — ancient sensors/maps could accurately report road and production state;
2. **Allocation Authority** — old central logic assigned foundry output and route priority remotely;
3. **Modern Comparison** — present Stoneway roads/depots deliberately use redundancy and local agreements instead of one optimal route.

Player-facing investigation text:

> “The old system could close a road or redirect foundry output from afar when another corridor ranked higher. Stoneway's modern network keeps the readings and leaves the decision with the people maintaining the route.”

The core interaction after the boss guarantees any missed evidence required for progression.

## First-clear completion reward

In addition to boss-layer loot:

```text
Gold: 760
EXP: 52% of current next-Lv requirement
Class XP: 34% of current Class Rank requirement
```

Deterministic 1-of-3 role choice:

1. **heavy guard/counter weapon or shield role**;
2. **poise/impact heavy-weapon role**;
3. **ranged precision / weak-point role**.

Exact names/models are bound at the pre-code asset gate.

---

# 10. Regional resolution / post-clear scene

After the guardian is defeated:

1. remote route/foundry allocation authority is disabled for the R09 branch;
2. old sensor information remains connected to Stoneway's manual/local signal network;
3. restored signals and modern depots remain the operational backbone;
4. foundry output improves modestly through better information, not remote command;
5. **R09 — Human Redundancy / Local Institutions Evidence** commits to personal state;
6. Act-III progression updates if eligible.

Required post-clear characters:

- Kael Marr;
- Branna Orel;
- Daren Holt;
- Ilyan Voss.

Canonical scene:

**Branna Orel**
> “The old sensors are better than ours. Fine. We'll use the readings.”

**Kael Marr**
> “But no distant ledger closes my western road because another road looks cheaper on paper.”

**Daren Holt**
> “That's the useful distinction. Reuse the instrument. Don't quietly restore the authority attached to it.”

**Ilyan Voss**
> “Southstone replaced a perfect chain with several imperfect ones. One link can fail without taking the region with it.”

Skipping commits the same state/journal information.

---

# 11. Shared and personal state

Shared/world-persistent:

- every restored signal remains active;
- repaired viaduct/gate shortcuts remain available;
- post-clear sensor feed can enhance local signals;
- selected caravan/foundry ambient state increases.

Personal:

- R09 evidence;
- Elephant qualification and personal registration ownership;
- optional-contract flags;
- field-boss discovery/clear;
- dungeon first-clear/reward state;
- local dialogue flags.

Late join sees physical route improvements but retains personal evidence interactions.

---

# 12. Reconnect / transaction safety

- signal activation is idempotent and independently saved;
- 2-of-3 main credit persists across defeat/reconnect;
- the Elephant transfer trial can restart from its last safe quest checkpoint but cannot duplicate registration eligibility or Gold;
- 1,500 Gold registration is an atomic server transaction;
- shared road repairs never erase personal story interactions;
- dungeon evidence and rewards commit independently;
- no first-clear duplication through relog/chunk unload.

---

# 13. What is closed / remains gated

Closed:

- Stoneway Fort identity and named cast;
- main quests and exact objective logic;
- 2-of-3 signal progression;
- Elephant qualification vs optional 1,500-Gold registration;
- optional contracts;
- dungeon evidence, reward amounts and post-clear state;
- dialogue/rejoin/multiplayer ownership rules.

Pre-code gates still required:

- exact field-boss model/name/attack table;
- final Executioner acceptance or replacement model;
- exact deterministic reward models;
- NPC/outfit/fort/foundry assets;
- VFX/SFX/BGM bindings;
- Azari coordinates/route travel-time closure.

Verification at authoring time:

```text
DESIGN REVIEWED: YES
CODE REVIEWED: N/A
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
