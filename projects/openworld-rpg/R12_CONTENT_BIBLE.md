# Open-World RPG — R12 Obsidian Rift Content Bible

> Status: **DESIGN CANON — Riftwatch cast, R12 ground-route authoring, Sky Drake unlock, Terradragon invocation, Central Anchor finale, personal ending execution, rewards, scenes and reconnect behavior locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Region/system package: `R12_IMPLEMENTATION_PACKAGE.md`  
> Story: `WORLD_STORY_CANON.md`  
> Quest/state: `QUEST_WORLD_STATE.md`  
> Cross-region identity: `REGION_CROSS_AUDIT.md`  
> Mounts: `MOUNTS.md`  
> Rule: this file closes R12 content-authoring blanks. Existing R12 anomaly, traversal, encounter and finale contracts remain authoritative unless explicitly refined here. `GAME_DESIGN.md` wins conflicts.

R12 is the place where the player sees the continent's earlier evidence become one physical system. It is not allowed to introduce a secret fourth philosophy, an arbitrary corruption meter, a prestige treadmill or a surprise explanation that invalidates the earlier regions.

Exact final-guardian model/name/anatomy attacks, selected anomaly-creature models, VFX/audio bindings and Azari coordinates remain **pre-code asset/spatial gates**, never implementation discretion.

---

# 0. Player-facing language rule

No player-facing R12 UI, dialogue, quest, item, ending, loading or system copy may expose development-stage terminology, internal state IDs, asset/license/hash notes, test wording or implementation terminology.

Internal names such as `Central Guardian`, `Phase Seam state` or `ending_restore=true` are never shown as raw strings.

---

# 1. Primary hub — Riftwatch Refuge

Final player-facing hub name: **Riftwatch Refuge**.

Riftwatch is a compact research/rescue camp on stable ground outside the strongest anomaly band. It is intentionally not another full city.

Visible identity:

```text
fractured-border approach
→ rescue beacon / rope bridge
→ Riftwatch Refuge
   ├─ expedition desk / map wall
   ├─ survey / instrument shelter
   ├─ healer / recovery tent
   ├─ forge / Material Vault supply point
   ├─ Sky Drake handler perch
   ├─ rest / mess shelter
   └─ outward routes toward Glass Garden, Split Spires and Excavation
```

The camp should look like people are actively surviving and studying a dangerous place, not like a quest board surrounded by tents.

---

# 2. Named R12 cast

| Character | Role | Normal anchor | Function |
|---|---|---|---|
| **Tessa Vorn** | refuge lead / expedition coordinator | expedition desk | primary R12 regional quest owner; rescue and route stakes |
| **Elen Quill** | anomaly surveyor / systems researcher | instrument shelter | consistent anomaly rules, Phase Seam/Echo Signal interpretation |
| **Rook Vale** | rescue captain / route scout | outer beacon | field safety, lost expedition events and ground-route knowledge |
| **Nara Sol** | Sky Drake handler / high-route researcher | handler perch | Sky Drake qualification and registration |
| **Mira Pell** | healer / recovery officer | recovery tent | injuries, expedition consequences and ordinary camp life |
| **Dessa Korr** | smith / supply keeper | forge/supply point | high-tier service, materials and equipment support |
| **Ilyan Voss** | Anchor scholar | archive/excavation route | synthesizes earlier evidence without choosing the ending for the player |
| **Daren Holt** | engineer/smith | Central Anchor approach | physical network/coupling interpretation and emergency isolation work |
| **Director Aven Marr** | Restoration Director | excavation / Central Anchor | strongest Restore advocate; competent and consequential rather than villainized |
| **Sera Wren** | cartographer/ranger | survey route | relates R12 geometry to earlier regional lines |
| **Kest Arden** | rival wanderer | optional high-spire/Terradragon scenes | final-world continuity; no ending authority |

No NPC can make the player's final philosophical choice.

---

# 3. R12 pacing

Suggested entry remains **Lv72**; final guardian is Lv80.

```text
Riftwatch + first ground-route chain: ~70–100 min
regional investigation + Sky Drake optionals: ~120–180 min
Obsidian Cathedral / Central Anchor finale: ~90–130 min
broad R12 first completion with Terradragon: ~4–6 h
```

The region is physically open before main-story readiness. Only the deep Central Anchor interface/finale requires accumulated investigation state.

---

# 4. Main regional chain — At the Edge of the Rift

Final title: **At the Edge of the Rift**.  
Quest owner: **Tessa Vorn**.  
Category: R12 Regional Main / Act-IV.  
Permanent failure: none.

Opening line:

> “The refuge is safe because we stopped pretending the whole region follows one set of rules. Learn the seams first. Then decide how deep you want to go.”

## Required sequence

1. discover Riftwatch Refuge;
2. visit the nearby **Phase Seam bridge** and observe its two authored route states;
3. follow one **Echo Signal** to a lost survey marker;
4. inspect one severed conduit where ordinary stone/maintenance infrastructure meets anomaly growth;
5. return/field handoff to Tessa and Elen.

Reward:

```text
EXP: 40% of current next-Lv requirement
Gold: 900
Class XP: 22% of current Class Rank requirement
```

Elen line:

> “Different symptoms, same underlying network. The seams are not random magic. They are damaged relationships between systems that used to coordinate.”

Unlocks **Lines That Should Not Meet**.

---

# 5. Main regional chain — Lines That Should Not Meet

Final title: **Lines That Should Not Meet**.  
Quest owners: **Elen Quill**, **Sera Wren**, **Daren Holt**.

The player investigates **3 of 4** major ground evidence sites in any order:

1. **Partition Gate** — old physical isolation hardware that once separated branches;
2. **Echo Quarry** — repeated signal pattern shows later cross-linking bypassed older partitions;
3. **Glass Garden Fault** — natural/ecological overgrowth interacting with active network energy, proving not everything present is artificial;
4. **Severed Relay Trench** — maintenance evidence shows one local failure once propagated outward through linked conduits.

The fourth site remains optional and gives context/reward.

After 3 distinct sites:

```text
EXP: 48% of current next-Lv requirement
Gold: 1,020
Class XP: 27% of current Class Rank requirement
```

Daren line:

> “The early system had partitions everywhere. Later work bridged around them for efficiency. They didn't remove the safety idea—they outgrew their tolerance for it.”

Unlocks the Obsidian Cathedral investigation and Sky Drake qualification if its ground-route prerequisites are also met.

---

# 6. Sky Drake unlock — Above the Split Spires

Final title: **Above the Split Spires**.  
Quest owner: **Nara Sol**.  
Category: optional major traversal unlock.  
Permanent failure: none.

Prerequisites:

- discover Riftwatch Refuge;
- complete `At the Edge of the Rift`;
- discover at least **one** major R12 ground-route POI beyond the refuge.

This ensures the player learns R12 at ground scale before permanent flight.

## Required sequence

1. inspect high-perch/drake signs from a ground-accessible overlook;
2. reach the lower Split Spires shelter through normal traversal;
3. resolve one authored rescue/territory/anomaly problem affecting the handler route;
4. perform the accepted approach/bonding/registration interaction with the assigned Sky Drake candidate;
5. complete a short controlled flight course after the permanent unlock transaction becomes eligible;
6. return to the handler perch or finish the flight at the authored landing shelf;
7. **Sky Drake Qualified** commits.

Qualification reward:

```text
EXP: 30% of current next-Lv requirement
Gold: 500
Class XP: 18% of current Class Rank requirement
registration access: unlocked
```

Registration remains:

```text
Gold: 6,000
```

Nara line:

> “You learned the ground first. Good. From the air you will see more, but you will understand less if every strange line becomes just another shape below you.”

No breeding, feeding grind, mount XP or random tame chance.

---

# 7. Terradragon optional world boss — The Mountain That Moves

Final hunt title: **The Mountain That Moves**.

Terradragon remains optional and never gates the Central Anchor or endings.

## Discovery

- discover the altar basin through long-range terrain spectacle, Riftwatch records or high-route observation;
- complete one authored altar-access sequence;
- first discovery permanently unlocks deliberate summon interaction.

## Summon requirement

Each successful encounter activation consumes:

```text
6 accepted R12 rift-crystal materials
+ 2 Volcanic Glass
```

Rules:

- the final player-facing rift-crystal material name/model is fixed during external asset intake before source bootstrap;
- materials are consumed only when server-authoritative encounter activation succeeds;
- first-clear reward is personal/non-repeatable;
- repeat summon cooldown remains **45 active minutes**;
- relog/restart does not reset cooldown;
- no Ultra Summon Stone or R12 token currency.

Kest optional line:

> “I've seen cliffs that looked alive. This is the first one I expect to answer back.”

---

# 8. Optional regional contracts

## 8.1 Lost Signal

Giver: **Rook Vale**.

Objective:

- follow one authored Echo Signal;
- recover or rescue the expedition state at its endpoint;
- return/field handoff.

Reward:

```text
EXP: 18%
Gold: 420
Class XP: 10%
```

## 8.2 Glass Garden Survey

Giver: **Elen Quill**.

Objective:

- discover Glass Garden;
- interact with two distinct ecological/anomaly observation points;
- gather one accepted late overgrowth/resource component if the final external model catalog retains it.

Reward:

```text
EXP: 20%
Gold: 460
Class XP: 12%
```

## 8.3 The Fourth Line

Available when `Lines That Should Not Meet` completed with one site unvisited.

Objective: inspect the remaining major ground evidence site.

Reward:

```text
EXP: 20%
Gold: 480
Class XP: 12%
```

---

# 9. Obsidian Cathedral chain — The Partitions Below

Final title: **The Partitions Below**.  
Quest owners: **Tessa Vorn**, **Daren Holt**, **Ilyan Voss**.  
Category: Act-IV Main / finale approach.  
Permanent failure: none.

Prerequisite: accumulated main-story evidence/authorization state defined in `WORLD_STORY_CANON.md`. The exterior complex remains discoverable before this state.

First-clear route:

```text
current expedition layer
→ Partition Galleries
→ Conduit Descent
→ Network Archive
→ Central Anchor approach
```

## Required evidence

The player must encounter these four facts:

1. **Early Partition Architecture** — the network originally expected regional branches to be isolatable;
2. **Later Coupling Bypasses** — administrators linked around safety partitions for greater efficiency/control;
3. **Regional Truth Map** — R04/R05/R06/R07/R08/R09/R10/R11 evidence fits one system without invalidating any region's local outcome;
4. **Current Cascade Path** — modern instability is propagating because enough cross-links still function.

Player-facing investigation text:

> “The network was not built as one indivisible machine. Earlier sections could be isolated. Later links made the whole system more efficient—and made distant failures able to reach one another.”

Reward on reaching the Central Anchor approach before the final guardian:

```text
EXP: 45% of current next-Lv requirement
Gold: 1,100
Class XP: 25% of current Class Rank requirement
```

This reward commits before the finale so a defeat/reconnect cannot erase the entire approach progression.

---

# 10. Final crisis — One System, Many Wounds

Final title: **One System, Many Wounds**.  
Category: Launch Main Finale.  
Permanent failure: none.

The immediate crisis occurs **before** the ending choice.

## Pre-boss sequence

1. Central Anchor enters a cascading failure state;
2. three visible regional-sector interfaces become active;
3. player and NPCs establish a temporary emergency partition to prevent immediate continent-wide propagation;
4. Restoration Director Aven Marr argues for preserving the maximum viable system but does not receive unilateral control;
5. final systemic guardian/crisis form activates;
6. player defeats/contains it using the already-authored Network Link / Cascade Line / Partition Window / Reconfiguration mechanics.

No player selects Restore/Release/Partition during active combat.

## Final guardian reward

First eligible victory grants, before ending selection:

```text
Gold: 1,500
EXP: enough to complete remaining Lv80 progress but never grant post-cap prestige XP
Class XP: 45% of current Class Rank requirement, capped by normal class progression
```

Deterministic finale choice reward — choose **1 of 3** high-end roles:

1. build-defining **Relic/Charm** role with conditional utility;
2. high-end **weapon/catalyst** role tied to resource/positioning interaction;
3. **defensive/support Relic** role tied to guard/healing/poise or recovery.

Exact item names, models and final numeric effects are pre-code asset/equipment bindings and must be closed before implementation. The finale is never reward-RNG-only.

## Final guardian asset gate

`Central Guardian / Cascade Guardian` is internal only.

Before source bootstrap, external intake must lock:

- final model;
- final player-facing name;
- anatomy-supported attack bindings;
- VFX/SFX;
- camera/hitbox acceptance.

Implementation cannot invent a temporary final boss.

---

# 11. Post-boss decision — What Remains Connected

Final title: **What Remains Connected**.

This decision happens only after:

- the immediate cascade is contained;
- the guardian is defeated;
- the player has access to a concise evidence summary;
- no combat timer is active.

The player is presented three explicit philosophies exactly as canon:

## Restore

Player-facing summary:

> **Restore coordinated continental operation under modern stewardship.** Keep the network connected, but rebuild oversight and safeguards around what the present world has learned.

Acknowledged evidence:

- R04/R10 prove restoration can produce real benefits;
- coordinated systems can protect routes, heat, water and infrastructure;
- risk remains in concentrated authority and renewed cascade paths.

## Release

Player-facing summary:

> **Sever and decommission the continental network in a controlled way.** Preserve local societies and ecosystems by ending dependence on central coordination.

Acknowledged evidence:

- R05/R08 show modern systems that flourish outside historical regulation;
- hidden remote coupling can impose costs on places that no longer need it;
- loss of useful large-scale coordination remains a real cost.

## Partition

Player-facing summary:

> **Split the network into independently governed regional clusters.** Keep useful infrastructure while preventing one authority or failure from controlling the whole continent.

Acknowledged evidence:

- R06/R07/R09/R10/R11 show local control, redundancy and bounded systems can work;
- coordination between clusters becomes harder and less efficient;
- regional governance does not eliminate every risk or conflict.

Rules:

- no option is visually labelled good/true/best;
- order is stable and not randomized;
- choice requires deliberate confirmation;
- one player's selection cannot choose for another player;
- no hidden morality score overrides the selected ending.

---

# 12. Ending confirmation and personal epilogue

After selection, show one confirmation describing the actual action in plain world language.

### Restore confirmation

> “Rebuild the continental links under new safeguards and shared stewardship?”

### Release confirmation

> “Begin the controlled severance of the old continental links?”

### Partition confirmation

> “Separate the network into independently governed regional clusters?”

After confirmation:

- ending state commits personally and atomically;
- personal journal/epilogue/dialogue flags update;
- optional-region participation changes epilogue details where authored;
- shared world remains the **post-crisis stabilized interim state**;
- no other player's world geometry is forcibly rewritten to match this personal philosophy.

If disconnect occurs before confirmation, no ending choice commits. If disconnect occurs after durable commit but before presentation finishes, the epilogue resumes without asking the player to choose again.

---

# 13. Ending scene ownership

Required central participants in the finale aftermath:

- Tessa Vorn;
- Ilyan Voss;
- Daren Holt;
- Director Aven Marr.

Additional regional representatives may appear through concise communication/epilogue segments based on completed evidence regions, but the finale does not become a 30-minute roll call.

Core pre-choice dialogue:

**Aven Marr**
> “We have enough of it alive to rebuild. After what we saw in Cinderfall and Hearthspring, abandoning that capacity has a cost.”

**Ilyan Voss**
> “And rebuilding every connection recreates the paths that carried the failures. We finally know what those paths cost too.”

**Daren Holt**
> “There isn't a clean machine waiting for us to pick the right button. There are useful parts, dangerous links, and people who will live with what comes next.”

**Tessa Vorn**
> “The emergency is over. This part is a choice.”

The decision UI follows immediately after, with no countdown.

---

# 14. Regional resolution / postgame state

Shared post-crisis R12 state:

- immediate cascade stopped;
- Central Anchor physically stabilized but not presented as fully restored/dead/partitioned for all players;
- main approach and selected shortcuts remain open;
- Riftwatch operates as a postgame research/rescue hub;
- Sky Drake and Terradragon content remain available;
- optional anomaly events/complexes continue;
- world exploration remains possible.

Personal state:

- chosen ending;
- R12 evidence/quest flags;
- finale reward;
- Sky Drake qualification/registration;
- Terradragon discovery/clear/cooldown eligibility;
- optional contracts;
- epilogue/faction response flags.

No prestige level, reset or New Game+ is forced.

---

# 15. Reconnect / multiplayer safety

- 3-of-4 ground evidence persists immediately;
- Sky Drake 6,000-Gold registration is atomic;
- Terradragon material consumption/cooldown is server-authoritative and transaction-safe;
- Central Anchor approach evidence commits before final boss;
- guardian victory commits before ending decision;
- ending selection is personal, explicit and atomic;
- a player joining after another's finale sees the stable shared post-crisis world and can still run their own main evidence/finale path;
- no shared event can overwrite another player's ending;
- final reward cannot duplicate on relog;
- world-state recovery uses deterministic checkpoints after crashes/disconnects.

---

# 16. What is closed / remains gated

Closed before implementation:

- Riftwatch Refuge identity and named cast;
- exact R12 ground-route evidence requirements;
- Sky Drake qualification and 6,000 Gold registration;
- Terradragon summon quantities and 45-minute cooldown;
- Obsidian Cathedral evidence structure;
- final crisis ordering;
- pre-ending guardian reward structure;
- exact Restore/Release/Partition player-facing summaries and confirmations;
- shared-vs-personal postgame state;
- reconnect/multiplayer ending safety.

Pre-code gates still required:

- final guardian external model/name/anatomy attack bindings;
- exact R12 ordinary/elite anomaly creature models;
- final rift-crystal/overgrowth/late-material visual identities;
- Sky Drake model/animation acceptance;
- Terradragon current model/animation/hitbox acceptance;
- finale reward item models/effects within existing equipment balance;
- Anchor machinery/Cathedral/Riftwatch visual assets;
- final VFX/SFX/BGM;
- Azari coordinates, volumes, flight/no-fly areas and final travel-time audit.

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
