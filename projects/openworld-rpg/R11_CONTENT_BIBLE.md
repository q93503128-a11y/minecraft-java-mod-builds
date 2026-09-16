# Open-World RPG — R11 Inner Sea Content Bible

> Status: **DESIGN CANON — major harbor cast, coast/open-sea/deep quest flow, Laviathan unlock, Deep-Dive Harness recipe, Abyss evidence, rewards, scenes and reconnect behavior locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Region/system package: `R11_IMPLEMENTATION_PACKAGE.md`  
> Story: `WORLD_STORY_CANON.md`  
> Quest/state: `QUEST_WORLD_STATE.md`  
> Cross-region identity: `REGION_CROSS_AUDIT.md`  
> Mounts: `MOUNTS.md`  
> Rule: this file closes R11 content-authoring blanks. Existing R11 aquatic-combat, ecology, traversal and dungeon contracts remain authoritative unless explicitly refined here. `GAME_DESIGN.md` wins conflicts.

R11 is one region learned in three stages: useful coast, wide open-sea network, then late abyss. It must never become a disconnected underwater minigame or a sea filled with repeated cache icons.

Exact external fish/model/animation/VFX/audio filenames, hashes, sea-fort boss identity and aquatic animation bindings remain **pre-code technical/asset gates**, never implementation discretion.

---

# 0. Player-facing language rule

No player-facing R11 UI, dialogue, quest, map, equipment or system copy may expose development terminology, internal state IDs, asset/license/hash notes or test/implementation wording.

Underwater compatibility/fallback tags are internal only; the player sees their normal learned skill identity.

---

# 1. Primary settlement — Tidecross Freeport

Final player-facing major harbor name: **Tidecross Freeport**.

Tidecross is the launch world's largest sea-trade hub, built where mainland roads, island ferries, reef routes and deep-water pilot lanes meet.

Visible identity:

```text
main harbor approach / lighthouse line
→ outer docks and fish market
→ Tidecross quay
   ├─ Harbor Master / route board
   ├─ Reef Warden / diver station
   ├─ freeport market / Material Vault
   ├─ forge / repair hall
   ├─ Laviathan pens / handler dock
   ├─ inn / food / fish buyer
   └─ ferries / island routes / lighthouse view
```

Freeport culture is traders, pilots, fishers, salvage crews, private guards and sailors—not a cartoon pirate city.

---

# 2. Named R11 cast

| Character | Role | Normal anchor | Function |
|---|---|---|---|
| **Sella Vey** | Harbor Master / route pilot | route office | coast/open-sea primary quest owner; surface shipping evidence |
| **Neris Quill** | Reef Warden / diver | diver station | deep ecology, Deep-Dive Harness and abyss investigation |
| **Iona Marr** | Laviathan handler | large-animal dock | authored Laviathan qualification and registration |
| **Maro Pell** | Freeport factor / broker | trade arcade | import/export economy, Material Vault and regional materials |
| **Dessa Rook** | shipwright / forge master | repair hall | equipment, watercraft/route hardware and dive apparatus service |
| **Jalen Fen** | innkeeper / fish buyer | Salt Lantern inn | rest, cooking, Fish Codex economy and harbor-life context |
| **Toma Vale** | lighthouse keeper | lighthouse office | beacon/route events and coast navigation |
| **Ilyan Voss** | recurring Anchor scholar | arrives during deep-current investigation | interprets pressure-corridor evidence only after discovery |
| **Sera Wren** | recurring cartographer / ranger | route desk / island survey | surface/depth mapping support without GPS spam |
| **Kest Arden** | rival wanderer | optional reef/deep scenes | continuity, no mandatory progression role |

---

# 3. Layered pacing

R11 is intentionally revisited.

```text
Coast / Tidecross first meaningful visit: Lv28+
Open sea / reef / Laviathan: Lv44+
Abyss / late main evidence: Lv64+
```

Typical content time:

```text
coast introductory chain: ~45–70 min
open-sea chain + Laviathan qualification: ~80–120 min
late deep chain + Abyss Temple first clear: ~120–170 min
broad R11 completion across all layers: ~5–7 h
```

No layer is level-locked by an invisible wall; readiness is communicated through encounter pressure and equipment/traversal context.

---

# 4. Coast chain — Harbor of Many Roads

Final title: **Harbor of Many Roads**.  
Quest owner: **Sella Vey**.  
Category: Regional Main / Coast introduction.  
Permanent failure: none.

Opening line:

> “Every road around the Inner Sea ends at water eventually. Learn the lights and currents first; after that, the sea stops looking empty.”

## Required sequence

1. discover Tidecross Freeport;
2. inspect the main route/current board;
3. visit the nearest lighthouse/beacon station;
4. complete one short harbor-route problem: stalled ferry, damaged beacon or reef-warning event;
5. discover one shallow-island or reef-edge POI;
6. return/field handoff to Sella.

Reward:

```text
EXP: 35% of current next-Lv requirement
Gold: 300
Class XP: 18% of current Class Rank requirement
```

Unlocks freeport ferries/route contracts and later **Routes Between Islands** once the player reaches or naturally engages the open-sea content band.

---

# 5. Open-sea chain — Routes Between Islands

Final title: **Routes Between Islands**.  
Quest owners: **Sella Vey**, **Toma Vale**, **Neris Quill**.

The player completes **2 of 3** route objectives in any order:

1. **Living Reef Route** — verify safe passage through a reef corridor and resolve one predator/pressure event;
2. **Storm-Wreck Route** — locate the authored wreck field and recover/activate its route signal;
3. **Sea-Fort Route** — reach the fort approach and restore/secure its navigation signal without requiring the full dungeon clear.

After 2 distinct objectives:

```text
EXP: 42% of current next-Lv requirement
Gold: 520
Class XP: 23% of current Class Rank requirement
```

World result:

- one ferry/open-sea route becomes more reliable;
- route board gains additional safe/fast connections;
- Laviathan qualification quest becomes available.

Sella line:

> “The sea is never one road. That's why the lights matter. Lose a route and you use the next one instead of pretending the water disappeared.”

The third route remains optional.

---

# 6. Laviathan unlock — A Wake for Four

Final title: **A Wake for Four**.  
Quest owner: **Iona Marr**.  
Category: Regional traversal / permanent mount unlock.  
Permanent failure: none.

This is the **only launch authored Laviathan unlock region**. R10 only foreshadows it.

## Required sequence

1. observe a working Laviathan at Tidecross before ownership;
2. travel with Iona to a broad-water handling area;
3. clear/resolve one route/ecology problem that prevents safe handling;
4. mount the assigned training Laviathan;
5. steer through three broad markers demonstrating turn radius and ascent/surface handling;
6. carry at least one NPC/party passenger seat during the final short route when solo scripting permits an NPC passenger, otherwise use an occupied training dummy/handler state;
7. return to handler dock;
8. **Laviathan Qualified** commits.

Qualification reward:

```text
EXP: 30% of current next-Lv requirement
Gold: 260
Class XP: 18% of current Class Rank requirement
registration access: unlocked
```

Personal registration:

```text
Gold: 3,000
```

Registration is optional for main-story progress but strongly improves open-water travel.

Iona line:

> “It isn't a boat and it won't turn like one. Give it room, let the body follow the head, and four riders can cross water faster than any ferry schedule.”

No breeding, feeding grind or mount XP.

---

# 7. Sea-Fort regional dungeon

Final player-facing dungeon title: **The Breakwater Keep**.

Suggested band: Lv46–52.  
Role: optional/regional open-sea dungeon, not required for late Act-III evidence.

First-clear route remains the implementation package flow:

```text
reef/dock approach
→ outer fort / warehouse yard
→ signal tower
→ command/storehouse interior
→ final commander/guardian
```

The final boss identity/model remains a pre-code asset gate.

First-clear completion reward in addition to boss loot:

```text
EXP: 45% of current next-Lv requirement
Gold: 620
Class XP: 28% of current Class Rank requirement
```

Deterministic 1-of-3 role choice:

1. projectile/ranged-control weapon role;
2. mobility/bleed accessory role;
3. defensive water/knockback-control role useful outside R11.

Completing the dungeon permanently improves one open-sea signal/ferry route and unlocks its repeat shortcut.

---

# 8. Deep-Dive Harness — exact unlock

Final player-facing equipment/service name: **Deep-Dive Harness**.

The Harness is a permanent traversal apparatus/state, not a consumable oxygen tank and not a combat equipment slot replacement.

## Unlock quest — Below the Light

Quest owner: **Neris Quill**.

Required sequence:

1. discover the Trench Lip Observatory or another authored deep-limit clue;
2. inspect one shallow old pressure station/wreck;
3. return to Neris/Dessa with the recorded pressure data;
4. craft/fit the Harness through the service;
5. test it in the authored trench-lip movement course;
6. permanent deep-dive state commits.

Exact service recipe:

```text
6 Silver Ore
+ 4 Volcanic Glass
+ 4 accepted R11 reef-fiber/reagent materials
+ 1,200 Gold
→ Deep-Dive Harness
```

The exact player-facing identity/model of the local reef material is finalized during asset intake before source bootstrap; the quantity and mechanical requirement are already fixed.

Harness effects remain the implementation-package canon:

- routine deep breath restriction removed for authored deep content;
- substantially improved underwater movement;
- readable ascent/descent;
- Swim Burst replaces/retargets ordinary dodge underwater;
- no surface stat bonus;
- no refill or upgrade tree.

Quest completion reward:

```text
EXP: 30% of current next-Lv requirement
Gold: 300
Class XP: 18% of current Class Rank requirement
Deep-Dive Harness: permanent unlock
```

---

# 9. Late chain — The Current Below

Final title: **The Current Below**.  
Quest owners: **Sella Vey**, **Neris Quill**, with Restoration/Anchor personnel joining after evidence appears.  
Category: Late Regional Main / Act-III route.  
Permanent failure: none.

The quest must first prove the surface improvement.

## Required sequence

1. observe/participate in a bounded Restoration current-route test near Tidecross;
2. verify one harbor approach now has clearer current prediction / safer travel time;
3. complete one successful surface convoy/ferry use under the improved state;
4. receive Neris's report that deep pressure/creature behavior changed after the same activation;
5. use the Deep-Dive Harness to inspect **two of three** deep anomaly sites:
   - Trench Lip pressure pylon;
   - affected reef/deep ecology zone;
   - old corridor marker near the temple route;
6. compare deep pressure direction with the surface route test.

Reward:

```text
EXP: 48% of current next-Lv requirement
Gold: 820
Class XP: 27% of current Class Rank requirement
```

Neris line:

> “The harbor sees calmer water. Down here the same correction has to go somewhere, and the trench is taking it.”

Unlocks **What the Harbor Cannot See**.

---

# 10. Main late dungeon — What the Harbor Cannot See

Final title: **What the Harbor Cannot See**.  
Category: Regional Main / Act-III evidence.  
Suggested band: Lv65–69.  
Permanent failure: none.

First-clear route:

```text
trench descent
→ pressure pylons
→ air / maintenance chamber
→ deep observatory
→ underwater altar / Abyss Fang
```

## Required evidence

The player must encounter:

1. **Surface Benefit Record** — reactivation improved a real harbor/current route;
2. **Deep Compensation Path** — the same network shifted pressure toward a distant deep corridor;
3. **Habitat/Temple Consequence** — deep ecology and infrastructure changed after the surface improvement;
4. **Remote Balancing Authority** — the corridor would continue adjusting without local/depth-band limits.

Player-facing investigation text:

> “The safer harbor route and the damaged trench are the same correction seen from two places. The network moves pressure farther than the people benefiting from it can see.”

If evidence is missed, the post-boss observatory commits the required summary.

## First-clear completion reward

In addition to Abyss Fang boss-layer loot:

```text
Gold: 980
EXP: 55% of current next-Lv requirement
Class XP: 36% of current Class Rank requirement
```

Deterministic 1-of-3 role choice:

1. abyssal DEX/precision weapon role;
2. WIL/INT current/pressure catalyst role;
3. defensive/recovery accessory for 3D movement and heavy predator encounters.

Exact names/models are pre-code asset bindings.

---

# 11. Abyss Fang encounter activation

Abyss Fang remains the authored climax of the deep investigation.

Activation rules:

- no donor summon-stone currency;
- the altar becomes operable only after the first-clear story state reaches the temple core;
- the first story activation requires **no additional farmed key item** beyond reaching the core legitimately;
- repeat activations use the dungeon's normal reset/eligibility state rather than a separate rare summon currency;
- first-clear reward ownership is personal and non-repeatable;
- repeat boss materials/gear follow global loot rules.

This keeps the altar identity without inserting an unrelated grind immediately before the story climax.

---

# 12. Regional resolution / post-clear scene

After Abyss Fang and core isolation:

1. automatic continental pressure balancing is disconnected from the R11 deep corridor;
2. Tidecross retains bounded local route prediction/beacon assistance;
3. deep corridor control remains local/depth-limited;
4. one affected reef/deep ambience state visibly recovers;
5. **R11 — Distant / Delayed Coupling Consequence Evidence** commits to personal state;
6. Act-III progression updates.

Required characters:

- Sella Vey;
- Neris Quill;
- Ilyan Voss;
- local Restoration representative or recorded test lead.

Canonical scene:

**Sella Vey**
> “The approach is still safer. I don't want to throw that away because we finally saw the bill.”

**Neris Quill**
> “Then don't. Keep the harbor aid local. Stop asking the trench to absorb changes it never agreed to.”

**Ilyan Voss**
> “R10 showed a cascade people could watch happen. This one travelled too far and too deep for the beneficiaries to notice.”

Skipping commits identical progression and journal state.

---

# 13. Optional content contracts

## 13.1 Reef Ledger

Giver: **Maro Pell**.

Objective: discover two authored reef-resource/fishing nodes and return one accepted reef reagent.

Reward:

```text
EXP: 16%
Gold: 280
Class XP: 9%
```

## 13.2 Lights Out

Giver: **Toma Vale**.

Objective: restore one optional lighthouse/beacon after a storm/event.

Reward:

```text
EXP: 18%
Gold: 300
Class XP: 10%
```

## 13.3 Riptooth hunt

Final hunt name: **Red Wake at Night**.

Discovery starts from one strong night-route sign/sighting and a broad search area. No clue counter. Finding Riptooth first is valid. It never gates Laviathan, Harness or the deep main story.

---

# 14. Shared and personal state

Shared/world-persistent:

- restored lighthouse/ferry route states;
- Breakwater Keep route improvement;
- surface-current test benefit while active;
- post-clear local-only prediction assistance;
- isolated deep corridor presentation;
- abyss temple repeat shortcut.

Personal:

- coast/open/deep quests;
- Laviathan qualification/registration;
- Deep-Dive Harness;
- R11 evidence;
- Riptooth/Abyss Fang clear state;
- Fish Codex/trophies;
- deterministic first-clear rewards.

Late join receives access to preserved personal evidence interactions even if shared routes are already improved.

---

# 15. Reconnect / multiplayer transaction safety

- route-objective credits persist immediately;
- Laviathan 3,000-Gold registration is atomic;
- Harness crafting is atomic and cannot duplicate materials/Gold;
- surface/deep restoration state is server-authoritative;
- Abyss Fang first activation cannot double-trigger or double-reward;
- disconnect underwater resumes at a safe deterministic state/checkpoint, not inside invalid geometry;
- shared corridor isolation cannot erase another player's personal evidence progression;
- aquatic combat compatibility is data-owned and identical server/client.

---

# 16. What is closed / remains gated

Closed:

- Tidecross Freeport identity and named cast;
- coast/open/deep quest structure and exact rewards;
- Laviathan authored qualification and 3,000 Gold registration;
- Deep-Dive Harness exact recipe and unlock sequence;
- surface-benefit/deep-consequence story sequence;
- Abyss Fang first story activation without summon-stone grind;
- post-clear local/deep control state;
- reconnect/late-join ownership.

Pre-code technical/asset gates still required:

- exact aquatic animation set and per-skill `AQUATIC_NATIVE/ADAPTED/FALLBACK` binding;
- current Laviathan/Giant Squid/Cachalot multipart verification;
- exact sea-fort boss;
- exact reef reagent/fish/resource models;
- Deep-Dive Harness visible model;
- Abyss Fang current-model/hitbox acceptance;
- harbor/island/temple architecture, VFX/SFX/BGM;
- Azari coordinates/depth bands/routes.

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
