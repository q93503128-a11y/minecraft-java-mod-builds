# Open-World RPG — R06 Mirewater Delta Content Bible

> Status: **DESIGN CANON — settlement, named cast, regional/main quests, gate-state flow, Hydra hunt, dungeon evidence, rewards and rejoin behavior locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Region/system package: `R06_IMPLEMENTATION_PACKAGE.md`  
> Story: `WORLD_STORY_CANON.md`  
> Quest/state: `QUEST_WORLD_STATE.md`  
> Cross-region identity: `REGION_CROSS_AUDIT.md`  
> Rule: this file closes R06 content-authoring blanks. Existing R06 traversal/ecology/combat/dungeon contracts remain authoritative unless explicitly refined here. `GAME_DESIGN.md` wins conflicts.

R06 must not become another river-market region. Its authored play is about **conflicting water-control states**, people who already maintain part of the old infrastructure locally, and the decision to keep useful local control while severing dangerous remote coupling.

Exact external model/animation/VFX/audio filenames, hashes and Azari coordinates remain **pre-code gates**, not implementation discretion.

---

# 0. Player-facing language rule

No player-facing R06 UI, dialogue, quest text, map label, item copy, tutorial, loading text or system message may expose:

- development-stage words (`alpha`, `prototype`, `temporary`, `placeholder`, `TODO`, `debug`, `developer`);
- internal state IDs such as `r06_gate_east_open`;
- asset-intake, license, hash or acceptance-test notes;
- implementation terminology.

Internal state is translated into normal world/player language. Missing content is a build/content failure, not a player-facing warning.

---

# 1. Primary settlement — Siltwake

Final player-facing settlement name: **Siltwake**.

Siltwake is a raised wetland town built across several stable mounds and platforms above inner channels. It is known for ferries, wetland medicine, clay work and practical gate maintenance.

Visible identity:

```text
dry levee approach / ferry channel
→ raised lantern mast
→ central boardwalk court
   ├─ Waterwarden gatehouse
   ├─ Reedglass Remedies
   ├─ ferry pier / route board
   ├─ Kiln Row clay workshop
   ├─ Material Vault / merchant
   ├─ inn / fish buyer
   └─ raised homes / inner bridges
```

The settlement should visibly show old flood marks, repaired boardwalks, drying herbs, clay vessels, skiffs/ferries and local water gauges.

---

# 2. Named R06 cast

| Character | Role | Normal anchor | Function |
|---|---|---|---|
| **Arlen Mere** | Waterwarden / gate keeper | Waterwarden gatehouse | primary regional quest owner; local-control perspective |
| **Mae Corin** | wetland healer / alchemist | Reedglass Remedies | medicine, wetland herbs, clean-water/poison ecology |
| **Vero Ash** | ferry master / reed guide | main ferry pier | route logic, ferries/skiffs, wet/dry navigation |
| **Kelm Rook** | clay worker / repair artisan | Kiln Row | clay/fiber economy and physical gate/boardwalk maintenance |
| **Nella Thorn** | innkeeper / fish buyer | The High Reed inn | rest/cooking/fish economy and local daily-life perspective |
| **Hark Dune** | regional hunter / watch captain | predator watch platform | crocodilian/Hydra context; route safety |
| **Daren Holt** | recurring engineer/smith | arrives after contradictory gate signals are proven | helps separate mechanical failure from remote command conflict |
| **Ilyan Voss** | recurring Anchor scholar | arrives during local-controller investigation | identifies continental coupling after evidence exists |
| **Sera Wren** | recurring cartographer / ranger | optional levee/observation scenes | helps read current terrain rather than revealing quest solutions |
| **Kest Arden** | rival wanderer | optional Hydra-basin scene | world continuity; no mandatory progression role |

Population follows the existing R06 regional package. No vanilla villagers are final presentation.

---

# 3. R06 pacing / reward benchmark

Suggested entry Lv remains **30** and R07 begins around Lv34.

Normal first-visit target from Siltwake discovery:

```text
direct regional chain + first dungeon clear: ~85–120 min
regional chain + optionals/Hydra/fishing: ~130–190 min
broad first-visit completion: ~3–4 h
```

The mandatory path does not force the player to grind to Lv36 before entering the dungeon. Suggested levels remain guidance.

Regional rewards use the existing global curve: modest dynamic events, larger authored chain milestones, major first-clear dungeon reward.

---

# 4. Main regional chain — Where the Walkways End

Final title: **Where the Walkways End**.  
Quest owner: **Arlen Mere**.  
Category: Regional Main / Act-II eligible route.  
Repeatability: once per player.  
Permanent failure: none.

Arlen's opening line:

> “A broken walkway is easy. A gate opening against the river is not. Come see the east channel before someone decides the whole delta needs rebuilding.”

## 4.1 Required sequence

1. discover Siltwake;
2. reach the **East Levee Gate** by raised route;
3. inspect the gate's local manual position indicator;
4. inspect the downstream flood shelf where the actual water level contradicts that setting;
5. help secure the short route hazard/event around the gate by combat, support or interaction contribution;
6. return to Arlen or complete the field handoff interaction with him if present.

Reward:

```text
EXP: 35% of current next-Lv requirement
Gold: 300
Class XP: 20% of current Class Rank requirement
```

Arlen completion line:

> “The mechanism says closed. The channel says open. That's not wear. Something farther away is giving orders.”

Unlocks **Water Against Water**.

State persists immediately after each evidence interaction; defeat/disconnect does not reset it.

---

# 5. Main regional chain — Water Against Water

Final title: **Water Against Water**.  
Quest owners: **Arlen Mere** and **Vero Ash**.

The player investigates two contradictory local systems in either order.

## 5.1 North Reed Gate

Route identity: boardwalk / shallow reed-flat approach.

Evidence:

- local residents have intentionally kept this branch partly open for freshwater exchange;
- remote pressure is repeatedly pushing it toward closed state;
- nearby herbs/fish show the current local use is real, not theoretical.

## 5.2 Low Ferry Gate

Route identity: levee / ferry or fixed-crossing approach.

Evidence:

- local manual setting keeps the branch controlled;
- remote command pressure intermittently opens it and floods a transport shelf;
- the player can physically see the transport consequence.

Both gates must be investigated, but order is free.

Reward after both:

```text
EXP: 40% of current next-Lv requirement
Gold: 340
Class XP: 22% of current Class Rank requirement
```

Vero completion line:

> “One gate wants more water, one wants less. Same old network, opposite bad answers. That's why we steer by this delta, not a chart from somewhere else.”

Unlocks **Three Local Gates**.

---

# 6. Main regional chain — Three Local Gates

Final title: **Three Local Gates**.  
Quest owner: **Arlen Mere**.  
Recurring support: **Daren Holt** arrives after the first isolation succeeds if main-story state permits.

The goal is not to collect gate tokens. The player physically isolates local controls from conflicting remote inputs.

Three authored gateworks exist:

1. **East Levee Gate** — protects settlement-side transport shelf;
2. **North Reed Gate** — preserves freshwater/wetland exchange;
3. **Low Ferry Gate** — controls a route/ferry basin.

## 6.1 Required completion

The player must isolate **any 2 of 3** gateworks locally.

Each gate requires a different short content context:

- one route-defense / interaction sequence;
- one shallow-water mechanism approach;
- one damaged boardwalk/levee access sequence.

No gate requires a new currency or repeated crafting.

The third gate remains optional regional content and grants a small settlement convenience/stock expansion if completed later.

Required two-gate completion reward:

```text
EXP: 45% of current next-Lv requirement
Gold: 380
Class XP: 25% of current Class Rank requirement
```

Optional third-gate completion reward:

```text
EXP: 18%
Gold: 180
Class XP: 10%
```

## 6.2 Daren field line

After the first successful local isolation:

> “There. The gate still works; the distant command doesn't. We don't have to choose between keeping the machine and obeying everything connected to it.”

Completing two gateworks reveals the pressure source leading toward the sunken observatory and unlocks **The Sunken Observatory**.

---

# 7. Optional regional contracts

Foreground at most two relevant optional contracts alongside the main lead.

## 7.1 Clean Water, Bitter Leaves

Giver: **Mae Corin**.

Objective:

```text
harvest 3 valid wetland-herb/antitoxin plants personally
+ inspect one clean-water source point
```

Reward:

```text
EXP: 20%
Gold: 210
Class XP: 12%
Cleansing Tonic x1
```

No mass alchemy chain follows.

## 7.2 Clay for the Walkway

Giver: **Kelm Rook**.

Objective:

```text
gather 4 Clay from authored riverbank/clay-shelf nodes
return to Kelm
```

Reward:

```text
EXP: 18%
Gold: 200
Class XP: 10%
small furnishing/utility recipe unlock
```

The clay is consumed. No new crafting profession is created.

## 7.3 Reedwater Catch

Giver: **Nella Thorn**.

Objective: catch one ordinary R06-valid fish from any discovered delta fishing spot.

Reward:

```text
EXP: 15%
Gold: 180
Class XP: 8%
prepared meal x1
```

No rare fish requirement.

---

# 8. Hydra hunt — The Basin With Many Heads

Final hunt title: **The Basin With Many Heads**.

The Hydra remains optional and is not evidence for Anchor theory.

Discovery grammar:

- one strong basin sign such as multiple directional drag/bite marks, unusually broad wake damage or a distant multi-head sighting adds a broad Hydra Basin search region;
- continuous water-edge disturbance/ruin damage leads toward the encounter;
- no numeric clue counter;
- finding the Hydra first is valid.

Hark Dune warning line:

> “If three wakes move and only one body follows, leave the deep water. Fight it where your feet still belong to you.”

First eligible defeat uses the R06 field-boss package and global reward rules. No separate `Hydra Token` or mandatory quest payment is added.

Kest optional first-sighting line:

> “I counted heads twice. Then I decided counting was less useful than finding dry ground.”

---

# 9. Main regional dungeon — The Sunken Observatory

Final title: **The Sunken Observatory**.  
Category: Regional Main / Act-II evidence.  
Suggested content band: Lv33–36.  
Permanent failure: none.

First-clear route follows the existing R06 package:

```text
drowned approach
→ outer regulator galleries
→ split-flow chamber
→ observatory archive
→ guardian basin
```

## 9.1 Split-flow chamber — exact story purpose

The player isolates three logical control channels:

```text
local settlement/levee control
local wetland/freshwater control
remote continental command path
```

The puzzle/interaction is spatial and visual. The final accepted machinery art determines exact lever/wheel/plate form, but not the logic above.

First clear requires:

- preserve both useful local channels;
- sever the remote command path.

The implementation may not reverse this into a binary `turn system on/off` choice.

## 9.2 Observatory evidence

The archive interaction records **R06 — Regional Stewardship Evidence** and shows that the local controller was later coupled to distant prediction/command systems.

Player-facing investigation text:

> “The delta gates were once local. Later routes tied their decisions to distant stations. The local controls still function after the remote path is cut.”

If the archive interaction is missed before the boss, a safe post-boss core interaction records it so progression cannot softlock.

## 9.3 First-clear completion reward

In addition to boss-layer personal loot:

```text
Gold: 500
EXP: 50% of current next-Lv requirement
Class XP: 32% of current Class Rank requirement
```

The deterministic first-clear 1-of-3 roles are locked:

1. **WIL/VIT healing/cleanse accessory role**;
2. **DEX/utility weapon role with status handling or mobility**;
3. **END/guard/anti-grab defensive role**.

Exact names/models/icons remain a pre-code asset gate.

---

# 10. R06 final regional resolution

After the dungeon guardian is defeated:

1. the player validates both local control branches;
2. the remote continental command path remains severed;
3. surviving safe local gates retain manual/regional authority;
4. one previously unreliable ferry/boardwalk route becomes dependable;
5. **R06 — Regional Stewardship Evidence** commits to personal story state;
6. Act-II evidence progression updates if eligible.

This is not full restoration and not full destruction.

## 10.1 Post-clear Siltwake scene

Required characters:

- Arlen Mere;
- Mae Corin;
- Daren Holt;
- Ilyan Voss.

Canonical dialogue:

**Arlen Mere**
> “The gates answer here now. If we open one, we know why. If we close one, the delta doesn't wait on a command from half a continent away.”

**Mae Corin**
> “And the reed pools are moving again without flooding the herb beds.”

**Daren Holt**
> “Local machine, local responsibility. More maintenance, fewer cascades.”

**Ilyan Voss**
> “Which means the old network can be divided. Useful parts do not require one master system.”

If this is the player's second qualifying R04–R07 evidence package, Act-II advances under `WORLD_STORY_CANON.md`. Otherwise, remaining evidence routes stay available.

---

# 11. Regional shared aftermath

Shared late-join-safe changes:

- the two required isolated gateworks remain visibly stable;
- optional third gate reflects its own completion state if done;
- one ferry/boardwalk shortcut becomes reliably available;
- selected alchemy/fishing/merchant stock expands;
- emergency `Levee Break` weighting falls and may be replaced with ordinary maintenance variants;
- natural wetland areas deliberately left uncontrolled remain visibly wetland rather than being drained.

Personal state:

- regional main completion;
- R06 stewardship evidence;
- Hydra discovery/clear;
- optional contracts;
- fish/trophy progress;
- dungeon first-clear/reward claim.

Late join sees physical shared improvements but retains access to personal evidence interactions through preserved state and journal handoff.

---

# 12. Exact state / reconnect contract

Suggested personal states:

```text
r06_siltwake_discovered
r06_main_stage
r06_east_gate_evidence_seen
r06_north_gate_evidence_seen
r06_low_ferry_evidence_seen
r06_gate_east_isolated
r06_gate_north_isolated
r06_gate_ferry_isolated
r06_required_gate_count
r06_hydra_discovered
r06_hydra_first_clear
r06_observatory_discovered
r06_observatory_evidence_recorded
r06_observatory_first_clear
r06_first_clear_reward_claimed
```

All progression-changing fields are server-authoritative.

Rules:

- shared gate state and personal story credit are separate;
- one player's repair/isolation cannot silently complete another player's personal evidence stage;
- defeat/disconnect preserves committed interactions;
- water-state transitions recover deterministically after reload;
- first-clear/reward transactions are idempotent;
- no quest requires a ferry NPC to pathfind across the whole delta.

---

# 13. Balance / repetition safeguards

R06 intentionally differs from earlier regions:

- R02 = follow environmental disturbance through forest;
- R03 = conquer vertical infrastructure;
- R04 = restore bounded heat routes and see direct humanitarian benefit;
- R05 = prove modern ecology/society adapted beyond obsolete control;
- **R06 = keep useful infrastructure but partition authority into local branches.**

Do not rewrite `Three Local Gates` into three identical switch rooms. Each gate's route, threat and local reason differ.

Do not add a swamp currency, disease meter, universal slow, repeated valve minigame or required Hydra kill.

---

# 14. Player-facing acceptance

R06 content is authoring-complete when implementation can reproduce without design invention:

- Siltwake identity and named cast;
- exact main-chain objectives and rewards;
- two-of-three local gate isolation with optional third gate;
- optional contracts;
- Hydra discovery grammar;
- split-flow chamber's exact conceptual solution;
- Act-II stewardship evidence;
- post-clear dialogue and shared aftermath;
- multiplayer/reconnect/late-join state boundaries.

Remaining unknowns are exact external assets and Azari coordinates only.

Verification at this authoring pass:

```text
DESIGN REVIEWED: YES
CONTENT AUTHORING CLOSED: YES
EXACT EXTERNAL ASSET BINDING: NO — PRE-CODE GATE
AZARI COORDINATES: NO — PRE-CODE GATE
CODE REVIEWED: N/A
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
