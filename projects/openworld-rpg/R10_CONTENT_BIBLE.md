# Open-World RPG — R10 Cinderfall Content Bible

> Status: **DESIGN CANON — settlement, named cast, restoration-test sequence, cascade quest flow, Masterwork Pick upgrade, rewards, scenes, Act-III evidence and reconnect behavior locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Region/system package: `R10_IMPLEMENTATION_PACKAGE.md`  
> Story: `WORLD_STORY_CANON.md`  
> Quest/state: `QUEST_WORLD_STATE.md`  
> Cross-region identity: `REGION_CROSS_AUDIT.md`  
> Mounts: `MOUNTS.md`  
> Rule: this file closes R10 content-authoring blanks. Existing R10 traversal/ecology/combat/dungeon contracts remain authoritative unless explicitly refined here. `GAME_DESIGN.md` wins conflicts.

R10 must prove both sides of the restoration argument in one uninterrupted regional story: the old local geothermal system **really can work and improve lives**, and that same successful reconnection can recreate remote coupling/cascade risk when broader automatic balancing comes online.

Exact external model/animation/VFX/audio filenames, hashes, final Inferno acceptance/replacement and Azari coordinates remain **pre-code gates**, not implementation discretion.

---

# 0. Player-facing language rule

No player-facing R10 text may expose development terminology, internal state IDs, asset/license/hash notes, acceptance language or implementation details.

Pressure, forge, restoration and emergency states are described entirely through the world's language.

---

# 1. Primary settlement — Cinderhold Refuge

Final player-facing settlement name: **Cinderhold Refuge**.

Cinderhold is a fortified forge enclave on a cool ash shelf beneath the caldera. It survives because local engineers manually control vents, cooling channels and forge heat well enough to keep a dangerous industrial settlement functioning.

Visible identity:

```text
ash-coast / basalt road
→ cooled gate bridge
→ Cinderhold Refuge
   ├─ main foundry / Forge Warden
   ├─ pressure-control house
   ├─ cooling works
   ├─ healer / recovery station
   ├─ merchant / Material Vault
   ├─ inn / shift hall
   └─ lift road toward basalt works and caldera
```

The refuge is a working community, not a safe-room lobby before a dungeon.

---

# 2. Named R10 cast

| Character | Role | Normal anchor | Function |
|---|---|---|---|
| **Maera Voss** | Forge Warden / Chief Smith | main foundry | primary local quest owner; rational supporter of reliable heat and high-tier smithing |
| **Orin Kade** | pressure engineer | pressure-control house | local vent/valve expert; identifies remote balancing and leads branch isolation |
| **Lysa Marr** | ash scout / Wyvern watcher | outer route tower | basalt routes, hazard reading and optional Wyvern hunt |
| **Toren Pell** | healer / safety officer | recovery station | worker injury/safety evidence and hazard preparation |
| **Brann Vale** | quartermaster / Material Vault merchant | supply hall | materials, mining tools and trade |
| **Sera Kelm** | innkeeper / shift steward | Ember Rest hall | food/rest/worker context |
| **Director Aven Marr** | Restoration field director | monitored test station | recurring restoration advocate; the test genuinely succeeds before the cascade |
| **Ilyan Voss** | recurring Anchor scholar | arrives once remote handshake evidence appears | interprets network-level coupling after evidence exists |
| **Daren Holt** | recurring engineer/smith | foundry support / dungeon aftermath | compares modern local safety limits with old automatic balancing |
| **Kest Arden** | rival wanderer | optional Wyvern / ash-route scene | continuity, no mandatory progression role |

No character is written as obviously right from arrival. Aven's test must be competent and beneficial before the later failure mode appears.

---

# 3. R10 pacing / reward benchmark

Suggested entry Lv remains **58**; dungeon climax is around Lv64.

```text
direct regional chain + first Coupled Forge clear: ~110–155 min
regional chain + optionals / Masterwork Pick / Wyvern: ~180–250 min
broad first-visit completion: ~4–5 h
```

The player can physically enter earlier. Recommended level is not a wall.

---

# 4. Masterwork Pick — exact R10 upgrade

R10 closes the final broad gathering-tool upgrade without requiring the new high-grade ore behind its own lock.

At Brann/Maera's forge service:

```text
Refined Pick
+ 8 Silver Ore
+ 4 Volcanic Glass
+ 900 Gold
→ Masterwork Pick
```

Rules:

- permanent Tool Pouch upgrade;
- keeps canonical 75% gathering-time multiplier;
- accesses authored high-tier nodes;
- does not require the R10 high-grade ore it is intended to help gather;
- cannot duplicate/refund into extra tradeable picks through class/loadout changes.

The service appears once the player owns a Refined Pick and has discovered Volcanic Glass.

---

# 5. Main regional chain — Useful Heat

Final title: **Useful Heat**.  
Quest owner: **Maera Voss**.  
Category: Regional Main / Act-III late route.  
Permanent failure: none.

Opening line:

> “We already use the mountain. The problem is that every safe shift depends on people catching pressure changes by hand. If the old controller can do that part reliably, I'm willing to test it.”

## Required sequence

1. discover Cinderhold Refuge;
2. inspect the manual pressure board with Orin;
3. visit **Cooling Works** and resolve one unstable vent/route event;
4. visit the **Basalt Works lift** and observe the manual shutdown schedule limiting access;
5. return to the monitored Restoration station;
6. approve/enter the controlled test sequence after all local safety valves are confirmed.

Reward:

```text
EXP: 40% of current next-Lv requirement
Gold: 700
Class XP: 22% of current Class Rank requirement
```

Unlocks **The Test Works**.

---

# 6. Main regional chain — The Test Works

Final title: **The Test Works**.  
Quest owners: **Director Aven Marr**, **Maera Voss**, **Orin Kade**.

The restoration branch is activated and the player must verify **all three** practical benefits so the later cascade cannot be dismissed as a failed startup.

## Verification A — Cooling Works

- previously erratic vent timing becomes stable;
- route hazard cadence visibly improves;
- workers resume the route.

## Verification B — Main Foundry

- forge temperature enters a predictable operating band;
- one high-grade production batch completes successfully;
- Maera explicitly confirms lower material waste / fewer shutdowns.

## Verification C — Basalt Lift Route

- pressure-assisted machinery stabilizes the industrial lift/bridge route;
- one previously unsafe passage opens as a persistent shared shortcut.

After all three:

```text
EXP: 45% of current next-Lv requirement
Gold: 780
Class XP: 25% of current Class Rank requirement
```

Aven line:

> “Stable vents, stable heat, open route. No hidden failure. The branch is doing exactly what the records said it would.”

Maera line:

> “Then leave it running. Carefully. This is the first week in years I can plan a full shift instead of guessing at the mountain.”

A short free-roam interval/event window follows so the improved world state is actually experienced before the next failure mode begins.

Unlocks **Load From Elsewhere** when remote balancing first appears.

---

# 7. Main regional chain — Load From Elsewhere

Final title: **Load From Elsewhere**.  
Quest owners: **Orin Kade** and **Director Aven Marr**.

Two pressure anomalies must be investigated in either order.

## Site A — Pressure Spires

- local demand is low, yet old controller commands pressure increase;
- interaction records a remote-load instruction originating beyond R10.

## Site B — Lower Foundry Bypass

- a second branch compensates automatically after Orin manually limits the first;
- physical gauges/vent behavior show the system is balancing around local intervention.

After both sites:

```text
EXP: 48% of current next-Lv requirement
Gold: 840
Class XP: 27% of current Class Rank requirement
```

Orin line:

> “Nothing here asked for more load. I close one branch and another opens to keep the continental target. That's not a broken controller. That's a controller answering to somewhere else.”

Aven line:

> “Then the test result stands. Local restoration works. What we did not isolate was the authority above it.”

Unlocks **The Coupled Forge** and begins the regional cascade state.

---

# 8. Cascade state — exact regional behavior

The cascade is a controlled authored progression state, not random grief.

When it begins:

- selected vent fields change timing/state;
- one industrial route closes and an alternate remains usable;
- workers evacuate specified unsafe work volumes;
- refuge core services remain available;
- smith/merchant/healer remain functional;
- no arbitrary player housing or normal terrain is destroyed;
- the main dungeon/forge approach becomes the safest route to isolate the system.

Reconnect during cascade resumes from the latest durable pressure checkpoint. The world never rolls a new random layout.

---

# 9. Optional contracts

## 9.1 Blackglass Work

Giver: **Brann Vale**.

Objective:

- gather 3 Volcanic Glass from valid personal R10 nodes;
- inspect one Blackglass Shelf worksite.

Reward:

```text
EXP: 18%
Gold: 360
Class XP: 10%
```

## 9.2 Safe Shift

Giver: **Toren Pell**.

Objective:

- resolve one Pressure Spike or Foundry Overload event;
- interact with the post-event safety station after the route is stable.

Reward:

```text
EXP: 22%
Gold: 420
Class XP: 14%
prepared recovery/support item x1
```

## 9.3 Ashline Survey

Giver: **Lysa Marr**.

Objective:

- discover Blackglass Shelf;
- discover one safe basalt overlook;
- identify one Wyvern territorial trace without requiring a kill.

Reward:

```text
EXP: 18%
Gold: 340
Class XP: 10%
```

---

# 10. Basalt Wyvern hunt — Crown Over Cinder

Final hunt title: **Crown Over Cinder**.

Discovery grammar:

- a major aerial sighting, nesting trace or recent route strike creates a broad cliff-search region;
- visible high-cliff signs and flight passes guide the player;
- no numeric clue counter;
- finding/fighting the Wyvern first is valid.

Lysa warning line:

> “It owns the high shelf, not the whole mountain. Watch where it turns before the dive. Once it commits, the cliff gives you more room than it gives the Wyvern.”

The boss remains optional and never gates the cascade/dungeon.

First eligible defeat follows global/R10 field-boss reward rules.

---

# 11. Main regional dungeon — The Coupled Forge

Final title: **The Coupled Forge**.  
Category: Regional Main / Act-III evidence.  
Suggested content band: Lv61–64.  
Permanent failure: none.

First-clear route:

```text
working industrial approach under evacuation
→ pressure galleries
→ coupling chamber
→ caldera transition
→ core altar / final guardian
```

## Required first-clear state/evidence

The player must encounter these four facts through the dungeon state:

1. **Local Benefit Record** — the restored branch genuinely reduced local instability and improved production/safety;
2. **Remote Handshake** — reactivation automatically re-established communication with distant nodes;
3. **Compensation Logic** — limiting one branch caused another to compensate to preserve a continental target;
4. **Cascade Risk** — multiple correct automatic responses can collectively destabilize the regional system.

Player-facing investigation text:

> “The local controller is stable. The overload comes from several linked nodes correcting for one another. Each response is valid on its own; together they drive the forge beyond local limits.”

## Branch-isolation sequence

During the first clear, the player isolates **three functional layers** in this order:

1. local refuge safety control — **KEEP ACTIVE**;
2. local foundry/geothermal production control — **KEEP ACTIVE under local limits**;
3. continental automatic balancing/remote authority — **DISCONNECT**.

This is not a dialogue choice and does not decide the final game ending. It is R10's practical regional solution.

If a required evidence interaction is missed, the post-boss core records the minimum evidence needed to prevent softlock.

## First-clear completion reward

In addition to boss-layer personal loot:

```text
Gold: 920
EXP: 55% of current next-Lv requirement
Class XP: 36% of current Class Rank requirement
```

Deterministic 1-of-3 role choice:

1. **high-tier heavy physical / impact weapon role**;
2. **fire/heat-linked magical catalyst or staff role**;
3. **END/VIT guard-stability defensive role**.

Exact names/models/icons are pre-code asset bindings.

---

# 12. Final guardian / Inferno gate

R10's final encounter role is fixed, but **current Inferno is not automatically accepted**.

Before source bootstrap, asset intake must either:

1. accept the current Inferno model/animation/function after Minecraft-scale review; or
2. bind a stronger external volcanic guardian while preserving the core/altar/cascade encounter role.

Only after that gate may the exact player-facing boss name, anatomy-specific weak points and final animation-bound attack table be committed.

Implementation may not create a temporary fire boss or particle-only substitute.

Regardless of final model, the boss must satisfy the existing R10 package's Lv64 / high-tier TTK, melee uptime, visible range, no-long-invulnerability and no-world-grief contracts.

---

# 13. Regional resolution / post-clear scene

After the final guardian/core encounter:

1. continental automatic balancing is disconnected from R10's operational branch;
2. local refuge safety control remains active;
3. local forge/geothermal control remains active with locally authored safety limits;
4. the cooled bridge/lift route remains open;
5. foundry production remains better than pre-test baseline but below uncontrolled peak throughput;
6. **R10 — Successful Restoration / Coupling Cascade Evidence** commits to personal state;
7. Act-III progression updates.

Required post-clear characters:

- Maera Voss;
- Orin Kade;
- Director Aven Marr;
- Ilyan Voss;
- Daren Holt.

Canonical scene:

**Maera Voss**
> “The forge is still better than it was. Stable heat, safer shift, less waste. I don't want to lose that.”

**Orin Kade**
> “You won't. The local controls stay. What is gone is the part that could raise our pressure because some distant node asked for more.”

**Aven Marr**
> “Then restoration was not the mistake. Coupling authority we did not intend to restore was.”

**Daren Holt**
> “Which means the old network's strength and its danger are the same thing: everything can answer everything else.”

**Ilyan Voss**
> “And now we have proof from a system that worked.”

Skipping commits identical state and journal evidence.

---

# 14. Laviathan decision

R10 does **not** unlock Laviathan at launch.

Canonical launch placement:

```text
Laviathan authored unlock: R11 Inner Sea
registration after R11 quest: 3,000 Gold
```

R10 may foreshadow the creature/specialist transport role through coastal sightings or trade dialogue, but it cannot be registered here.

This removes the old `R10 or R11` implementation-time choice and gives the water/lava specialist mount to the region where its full maritime movement role can actually be demonstrated.

---

# 15. Shared and personal state

Shared/world-persistent:

- successful local restoration benefits after `The Test Works`;
- cascade physical state while active;
- post-clear local-only control configuration;
- cooled bridge/lift shortcuts;
- improved bounded foundry ambient state.

Personal:

- R10 evidence;
- quest rewards;
- Wyvern discovery/clear;
- optional contracts;
- dungeon first-clear/reward ownership;
- dialogue flags.

Late join sees the current shared physical state and can still obtain personal evidence through preserved/condensed interactions.

---

# 16. Reconnect / transaction safety

- each test verification commits independently;
- cascade checkpoints are deterministic server state;
- shared route changes do not skip personal quest credit;
- branch isolation interactions are idempotent;
- disconnect during final reward transaction cannot duplicate or lose rewards;
- final local/remote control state survives chunk unload/server restart;
- late join can complete evidence after the world has already been stabilized;
- Masterwork Pick service is atomic and cannot duplicate consumed materials/Gold.

---

# 17. What is closed / remains gated

Closed:

- Cinderhold Refuge identity and named cast;
- exact Masterwork Pick recipe/cost;
- successful restoration-test sequence;
- exact three benefit checks;
- remote-load investigation;
- cascade world-state behavior;
- optional contracts and Wyvern discovery grammar;
- dungeon isolation order and reward amounts;
- post-clear local-control configuration and dialogue;
- Laviathan launch unlock moved decisively to R11.

Pre-code gates still required:

- Inferno accept/replace decision and exact final boss name/attack table;
- exact high-grade ore/fire-reactive mineral models and final identities;
- deterministic reward item models;
- forge/refuge/Anchor machinery assets;
- VFX/SFX/BGM bindings;
- Azari coordinates/volumes/travel-time closure.

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
