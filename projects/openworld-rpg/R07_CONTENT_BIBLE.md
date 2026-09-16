# Open-World RPG — R07 Sunscar Desert Content Bible

> Status: **DESIGN CANON — settlement, named cast, regional/main quests, well/cistern state, Deathworm hunt, dungeon evidence, rewards and rejoin behavior locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Region/system package: `R07_IMPLEMENTATION_PACKAGE.md`  
> Story: `WORLD_STORY_CANON.md`  
> Quest/state: `QUEST_WORLD_STATE.md`  
> Cross-region identity: `REGION_CROSS_AUDIT.md`  
> Rule: this file closes R07 content-authoring blanks. Existing R07 traversal/ecology/combat/dungeon contracts remain authoritative unless explicitly refined here. `GAME_DESIGN.md` wins conflicts.

R07's content is about **scarcity under centralized optimization**. Old infrastructure can preserve water, but a distant system that treats settlements as numbers can save one route by quietly starving another.

Exact external model/animation/VFX/audio filenames, hashes and Azari coordinates remain pre-code gates, never implementation discretion.

---

# 0. Player-facing language rule

No player-facing R07 UI, dialogue, quest text, map label, item text, tutorial, loading copy or system message may expose development-stage terminology, internal state IDs, asset/license/hash notes or test/implementation wording.

The player sees world language only. Missing content is a build/content failure.

---

# 1. Primary settlement — Amberwell

Final player-facing settlement name: **Amberwell**.

Amberwell is a fortified caravan oasis built around a managed well/cistern cluster where the main east-west caravan road meets a badlands pass.

Visible identity:

```text
caravan road / shade banners
→ outer animal/wagon yard
→ Amberwell gate
→ cistern court
   ├─ caravan hall / route board
   ├─ well keeper / gauge house
   ├─ smith / travel repair
   ├─ inn / food / shade court
   ├─ market / Material Vault
   ├─ guide platform
   └─ outward road to Redmile / badlands / buried fortress
```

Water jars, shade, cistern access, rope, wagon repair, pack gear and route boards must make the settlement economy legible before dialogue explains it.

---

# 2. Named R07 cast

| Character | Role | Normal anchor | Function |
|---|---|---|---|
| **Rava Sen** | caravan master | caravan hall | primary regional quest owner; route losses and trade stakes |
| **Olan Korr** | well keeper / cistern engineer | gauge house / cistern court | local water measurements and control work |
| **Tarek Vey** | dune guide / tracker | route board / outer watch | route reading, storms, Deathworm context |
| **Mira Doss** | smith / wagon repairer | repair yard | ordinary regional gear, travel equipment, mechanical support |
| **Neri Sol** | innkeeper / food merchant | Shadehouse inn | rest, food, traveler context |
| **Kesra Pell** | market / Material Vault clerk | covered market | trade/material service |
| **Ilyan Voss** | recurring Anchor scholar | joins after buried-control evidence | interprets optimization records after discovery |
| **Daren Holt** | recurring engineer/smith | joins local branch isolation work | helps separate local cistern control from remote draw |
| **Kest Arden** | rival wanderer | optional Deathworm / badlands scenes | continuity, no mandatory progression role |

No vanilla villagers are final population.

---

# 3. R07 pacing / reward benchmark

Suggested entry Lv remains **34** and the dungeon climax sits around Lv40.

Normal first-visit target:

```text
direct regional chain + first dungeon clear: ~90–125 min
regional chain + several optionals: ~140–200 min
broad first-visit completion including Deathworm: ~3–4 h
```

Mandatory content does not grind-gate the player to Lv40. Suggested levels remain risk guidance.

---

# 4. Main regional chain — The Missing Well

Final title: **The Missing Well**.  
Quest owner: **Rava Sen**.  
Category: Regional Main / Act-II eligible route.  
Permanent failure: none.

Rava's opening line:

> “Redmile didn't miss a caravan. It missed its water. The well dropped in two days, while an abandoned basin north of here started filling again.”

## 4.1 Required sequence

1. discover Amberwell;
2. travel to **Redmile Waystation** by caravan road or legal alternate route;
3. inspect the modern well gauge showing abnormal loss;
4. inspect the nearby cistern intake and confirm no ordinary surface break explains it;
5. travel to the **Old Basin** lookout/ruin;
6. inspect the recovering ancient water outlet;
7. return to Rava/Olan or trigger the field handoff if they are present.

The contrast is mandatory: water is being **redirected**, not simply disappearing.

Reward:

```text
EXP: 35% of current next-Lv requirement
Gold: 360
Class XP: 20% of current Class Rank requirement
```

Olan completion line:

> “Redmile loses pressure while a dead basin gains it. The waterworks are choosing. The question is who taught them what matters.”

Unlocks **Lines Under Sand**.

---

# 5. Main regional chain — Lines Under Sand

Final title: **Lines Under Sand**.  
Quest owners: **Olan Korr** and **Tarek Vey**.

The player investigates two buried conduit sites in either order.

## 5.1 Badlands Vent

- reached through a canyon/badlands route;
- exposed old conduit shows flow toward the abandoned basin;
- one Desert Beetle / elite pressure or environmental route problem guards the site;
- inspection records direction and pressure.

## 5.2 Dune Aqueduct Marker

- reached by open-dune cut or caravan road detour;
- buried structure shows the same remote-draw pattern from a second angle;
- sandstorm may alter ambience but is never required.

After both sites:

```text
EXP: 40% of current next-Lv requirement
Gold: 420
Class XP: 22% of current Class Rank requirement
```

Tarek completion line:

> “Two lines, same pull. If this were drought, every well would fall together. Something under us is sending water somewhere on purpose.”

Unlocks **A Route Worth Keeping**.

---

# 6. Main regional chain — A Route Worth Keeping

Final title: **A Route Worth Keeping**.  
Quest owners: **Olan Korr**, with **Daren Holt** joining after the local controller is exposed.

Purpose: prove that the region does not need to destroy all old waterworks to reject remote optimization.

## 6.1 Required sequence

1. reach the **Redmile Local Cistern Control** through the failed-waystation route;
2. clear/resolve the authored site hazard;
3. inspect the local storage branch and remote draw branch;
4. mechanically isolate the remote draw path while keeping local well/cistern operation active;
5. verify Redmile's gauge begins recovering to a stable operating band;
6. observe caravan/waystation activity return in a short world-state update.

No puzzle requires numerical plumbing calculations.

Reward:

```text
EXP: 45% of current next-Lv requirement
Gold: 460
Class XP: 25% of current Class Rank requirement
```

Daren line after isolation:

> “Local reservoir is holding. The old pipe still helps the well; it just can't be emptied because a distant model says another route is worth more.”

This unlocks the buried fortress/observatory lead and **The Buried Ledger**.

---

# 7. Optional contracts

Normally foreground at most two alongside the current main objective.

## 7.1 Salt and Shade

Giver: **Kesra Pell**.

Objective:

- gather 3 units from an accepted dryland salt/mineral node family;
- discover one shaded caravan cache/waystation location.

Reward:

```text
EXP: 18%
Gold: 230
Class XP: 10%
```

## 7.2 Armor on the Pass

Giver: **Mira Doss** or route board.

Objective: meaningfully resolve one authored Desert Beetle / Armor-of-Desert route threat after discovery.

Reward:

```text
EXP: 22%
Gold: 270
Class XP: 14%
```

No repeated elite-kill quota.

## 7.3 Oasis Catch

Giver: **Neri Sol**.

Objective: catch one ordinary valid oasis/wadi fish if the final R07 fish roster retains an ecological fishing pocket.

Reward:

```text
EXP: 12%
Gold: 180
Class XP: 7%
prepared meal x1
```

If final asset/ecology intake removes R07 fishing entirely, this contract is deleted before source bootstrap rather than replaced by a meaningless pond.

---

# 8. Ferox Deathworm hunt — Wake Under the Dunes

Final hunt title: **Wake Under the Dunes**.

Discovery grammar:

- one strong major sign (moving sand wake, violently displaced cargo, eruption scar or distant sighting) adds a broad search region;
- continuous surface disturbance and terrain signs guide the player toward the encounter basin;
- no numeric clue counter;
- finding the boss first is valid.

Tarek warning line:

> “If the sand starts moving faster than your mount, don't race it. Cut across stone and make it surface where you can see the strike.”

The Deathworm remains optional and never gates the dungeon.

First eligible defeat uses global/R07 field-boss reward rules; no new desert currency or separate quest token.

Kest optional line:

> “Open desert looks empty right up until the ground decides otherwise.”

---

# 9. Main regional dungeon — The Buried Ledger

Final title: **The Buried Ledger**.  
Category: Regional Main / Act-II evidence.  
Suggested content band: Lv37–40.  
Permanent failure: none.

First-clear route remains:

```text
half-buried fortress entrance
→ cistern galleries
→ aquifer conduit
→ observatory/control archive
→ guardian chamber
```

## 9.1 Required first-clear evidence

The player must encounter these three story facts through space/interactions:

1. **Local Storage Record** — the original works stabilized individual wells/cisterns;
2. **Balancing Expansion** — later systems added cross-route redistribution;
3. **Priority Ledger** — remote optimization explicitly ranks routes/settlements and can redirect scarce water away from lower-priority communities.

The `Priority Ledger` is not a ten-page text dump. The physical display/map shows one node gaining pressure as two peripheral nodes lose it, supported by a short readable record.

Player-facing investigation text:

> “The later system does not merely share water. It ranks routes. When supply falls, lower-priority wells are drained to protect higher-priority corridors.”

If a required interaction is missed, the post-boss core records the necessary evidence to prevent softlock.

## 9.2 First-clear completion reward

In addition to boss-layer personal loot:

```text
Gold: 560
EXP: 50% of current next-Lv requirement
Class XP: 32% of current Class Rank requirement
```

Deterministic first-clear 1-of-3 gameplay roles:

1. **heavy impact / armor-break weapon role**;
2. **ranged precision / weak-point weapon role**;
3. **VIT/END anti-burst defensive accessory or shield role**.

Exact names/models/icons remain a pre-code asset gate.

---

# 10. R07 regional resolution

After the dungeon guardian is defeated:

1. the remote redistribution/priority command path is severed for R07's active local branch;
2. Amberwell/Redmile local wells and cisterns retain bounded local operating control;
3. the abandoned basin is not artificially drained dry merely to reverse the situation;
4. one caravan well/waystation stabilizes visibly;
5. **R07 — Scarcity / Remote Optimization Evidence** commits to personal state;
6. Act-II evidence progression updates if eligible.

The player does not turn the desert green.

## 10.1 Post-clear Amberwell scene

Required characters:

- Rava Sen;
- Olan Korr;
- Ilyan Voss;
- Daren Holt.

Canonical dialogue:

**Rava Sen**
> “Redmile has water again. The old basin still has some too. We didn't have to choose which road deserved to exist.”

**Olan Korr**
> “Because the wells answer locally now. Scarcity is hard enough without a distant machine hiding the choice.”

**Daren Holt**
> “The system was efficient. That's not the same as fair, and it isn't the same as safe.”

**Ilyan Voss**
> “Another reason the old network cannot simply be restored as it was.”

If this is the second qualifying R04–R07 evidence package, Act-II advances according to `WORLD_STORY_CANON.md`. Otherwise remaining evidence regions remain available.

---

# 11. Shared regional aftermath

Shared late-join-safe changes:

- Redmile Waystation visibly reopens;
- one caravan route gains regular traffic/lantern/merchant presence;
- selected local well/cistern gauges stabilize;
- merchant stock expands modestly;
- emergency `Well Failure` event weighting drops or changes into maintenance variants;
- natural desert remains desert.

Personal state:

- R07 main completion/evidence;
- Deathworm discovery/clear;
- optional contracts;
- dungeon first-clear/reward state;
- regional collection/trophy states.

Late join sees shared improvements but still receives personal evidence interactions and journal context.

---

# 12. Exact state / reconnect contract

Suggested personal states:

```text
r07_amberwell_discovered
r07_main_stage
r07_redmile_loss_seen
r07_old_basin_gain_seen
r07_badlands_conduit_seen
r07_dune_conduit_seen
r07_local_cistern_isolated
r07_deathworm_discovered
r07_deathworm_first_clear
r07_fortress_discovered
r07_priority_ledger_seen
r07_dungeon_first_clear
r07_evidence_recorded
r07_first_clear_reward_claimed
```

Shared world state separately owns Redmile reopening and route presentation.

Rules:

- defeat/disconnect preserves committed investigation steps;
- a shared reopened well does not substitute for personal evidence credit;
- scene skips preserve journal information;
- first-clear/reward commits are idempotent;
- Deathworm burrow state never owns permanent quest progress.

---

# 13. Anti-repetition contract

R07's authored grammar is distinct:

- R04: bounded restoration visibly saves people;
- R05: modern local adaptation outgrew obsolete control;
- R06: useful infrastructure is partitioned into local authority;
- **R07: scarce resources reveal the human cost of remote optimization and priority ranking.**

Do not turn R07 into another `repair 2 of 3 stations` structure.

Do not add thirst, heat meters, sand currency, mandatory resistance gear or giant empty travel gaps to manufacture desert identity.

---

# 14. Player-facing acceptance

R07 content is authoring-complete when implementation can reproduce without inventing design:

- Amberwell and named cast;
- exact four-stage regional chain and rewards;
- Redmile/Old Basin contrast;
- local-branch isolation consequence;
- optional contracts;
- Deathworm discovery grammar;
- dungeon evidence and deterministic first-clear role choices;
- post-clear Act-II scene and route/well aftermath;
- save/reconnect/late-join ownership boundaries.

Remaining unknowns are exact external asset bindings and Azari coordinates only.

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
