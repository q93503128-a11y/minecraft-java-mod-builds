# Open-World RPG — R01 Heartland Content Bible

> Status: **R01 GAMEPLAY / CONTENT DESIGN CLOSED — implementation-time gameplay choices are locked; exact ASSET_BINDING + SPATIAL_BINDING remain pre-code production gates**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Opening/first-session package: `R01_VERTICAL_SLICE.md`  
> Combat/ecology: `STATUS_AND_R01_ENCOUNTERS.md`  
> Quest/state: `QUEST_WORLD_STATE.md`  
> Loot/equipment: `LOOT_ECONOMY.md`, `EQUIPMENT_BALANCE.md`  
> Recovery/production: `RECOVERY_PRODUCTION_APPEARANCE.md`  
> Gathering/fishing/camp/housing: `GATHERING_FISHING_CAMP_HOUSING.md`, `FISHING_COLLECTION_HOUSING_MARKET.md`  
> Party/multiplayer: `PARTY_MULTIPLAYER.md`  
> UI: `UI_DIRECTION.md`  
> Story: `WORLD_STORY_CANON.md`, `MAIN_QUEST_SCENE_PACKAGE.md`  
> Cross-region quality: `REGION_CROSS_AUDIT.md`  
> Asset intake: `R01_ASSET_INTAKE.md`  
> Spatial gate: `AZARI_SPATIAL_CLOSURE_PASS1.md`  
> R01 UI flow: `R01_UI_PRODUCTION_SPEC.md`  
> R01 player-facing text: `R01_PLAYER_TEXT_SPEC.md`  
> Rule: this file is the later R01 **content-bible authority** for details it explicitly closes. It does not reopen the locked first-session spine in `R01_VERTICAL_SLICE.md`. If this file and the vertical-slice file differ on a point explicitly refined here, this file wins for that refinement; `GAME_DESIGN.md` and `PROJECT.md` remain higher authority.

This file exists so R01 is not handed to implementation as “a strong opening plus a collection of ranges and ideas.”  
An implementer must not decide R01 event structure, repeat timing, merchant stock, property count, profession opportunities, retry rules, spawn-role density, post-clear state or contextual teaching behavior while coding.

Exact external asset files/hashes and exact Azari coordinates remain dedicated **pre-code gates**. They are not implementation discretion. If a final visible asset or world placement is still unresolved, the affected player-facing source does not silently invent a substitute.

---

# 0. Player-facing language rule

No R01 player-facing UI, dialogue, quest, event, map, item, tutorial, loading text or system message may expose:

- internal IDs such as `r01_*`, `PLAYER_PROGRESS`, `ENCOUNTER_STATE`;
- `alpha`, `beta`, `prototype`, `temporary`, `placeholder`, `TODO`, `debug`, `developer`;
- asset-intake/license/hash language;
- acceptance-test language;
- production milestone names.

Internal state is translated into normal world/player language before display.

---

# 1. R01 regional identity

Final region identity: **Heartland**.  
Final hub: **Alderford**.

R01 is the player's proof that this is a complete action RPG world rather than a tutorial arena.

R01's complete play sentence is:

```text
arrive through an ordinary working road
→ survive one readable local threat
→ discover Alderford as a useful long-term hub
→ choose a first class without service-tour chores
→ follow road/quarry disturbance evidence
→ wander into gathering, fishing, housing, camp, mount and hunt opportunities
→ learn local ecology through behavior rather than enemy spam
→ discover optional danger that can be left alone
→ descend through an authored quarry dungeon
→ defeat Earthloong and recover the first undeniable old-network evidence
→ return to a visibly changed but still usable Alderford
→ open R02/R03 as peer directions while R01 remains worth revisiting
```

R01 is **not**:

- a corridor tutorial;
- a disposable starter village;
- an MMO starter zone filled with exclamation marks;
- a place that becomes empty after Earthloong;
- a mandatory completion checklist before R02/R03;
- a low-level loot landfill.

---

# 2. External reference calibration and balance discipline

External games are reference points, not values to copy literally.

## 2.1 What is being borrowed structurally

- **Elden Ring / Limgrave:** an early major threat can be visible, dangerous and fully optional; leaving and returning later is valid play. Regalhart keeps this role rather than becoming a disguised progression gate.
- **Guild Wars 2 open-world events:** local events should be discovered because something is happening in the world, permit spontaneous cooperation, grant personal participation rewards and avoid kill-steal ownership. R01's repeatable roadside event follows this structure without turning the whole region into an event chain.
- **Diablo IV open-world / dungeon reward direction:** exploration can interrupt a planned route with useful world activity, while deterministic first-clear rewards give a reliable reason to complete authored dungeons instead of depending entirely on random drops. R01 already uses both principles.
- **Monster Hunter status design:** repeated abnormal-status application becomes harder rather than allowing one status loop to dominate a boss. The exact project rules remain in `STATUS_AND_R01_ENCOUNTERS.md`.

Useful external-source notes already consistent with project direction:

- Guild Wars 2 official dynamic-events material emphasizes area-specific events, spontaneous cooperation and individual rewards.
- Blizzard's Diablo IV developer Q&A describes open-world activity as intentionally capable of pulling the player away from the planned objective and uses deterministic dungeon-linked power unlocks alongside random loot.
- Capcom's official Monster Hunter manuals explicitly describe adaptation/resistance to repeated abnormal statuses.

## 2.2 Four kinds of numbers

Every R01 number belongs to one of four categories.

### HARD_RULE
Implementation copies it exactly unless canon is deliberately revised.

Examples:
- quest objective counts;
- reward identities;
- merchant slot count;
- property count;
- camp unlock condition;
- event participation rules;
- repeat-controller state transitions.

### TUNEABLE_SEED
Implementation starts with the written value. Real Minecraft playtest may tune it after evidence.

Examples:
- enemy density;
- route travel time;
- Gold price within a preserved economy role;
- respawn delay;
- expected Lv at a pacing checkpoint;
- boss TTK inside the already-locked target band.

A tuneable seed is **not** permission to choose a random initial value during coding.

### ASSET_BINDING
Exact model/animation/VFX/audio/icon file is selected in the external-asset gate before affected player-facing implementation.

### SPATIAL_BINDING
Exact x/y/z, footprint, sightline, slope and final travel distance are selected in the Azari spatial gate before affected world implementation.

---

# 3. Full R01 subregion contract

These player-facing place names and functions are final.  
Exact coordinates are SPATIAL_BINDING.

| Subregion | Local pressure | Primary function | Required authored content |
|---|---:|---|---|
| **Alderford Approach** | Lv1–2 | arrival/readability | first Meadow Viper, gate reveal, first shrine |
| **Alderford** | safe hub | services/social | all locked service buildings, housing vacancies, route board |
| **Greenwater Ford** | Lv1–3 | river/gathering/mount | Healing Herb, fishing, Trail Stag event route |
| **Alder Meadow** | Lv2–4 | open ecology | Louxia/Gazelle/Bison, territorial signs, readable optional danger |
| **Riverwood Edge** | Lv3–5 | tighter ecology | Viper/Grizzly pressure, Hardwood, route contrast |
| **Old Quarry Road** | Lv3–5 | main disturbance route | cargo/wagon evidence, roadside event, quarry approach |
| **Rootshade Grove** | Lv6–8 | dangerous optional pocket | Steelboar/Nature Spirit pressure, Regalhart hunt boundary, Verdant Crystal |
| **Old Alderford Quarry** | Lv4–8 | dungeon arc | surface approach, lower workings, Earthloong |
| **Western Route Line** | exit warning | R02 direction | readable forestward continuation after R01 handoff |
| **High Road Line** | exit warning | R03 direction | readable uphill/highland continuation after R01 handoff |

No invisible wall separates these subregions.

## 3.1 Route-duration seeds

Before Trail Stag, intended first-visit movement targets are:

| Route | TUNEABLE_SEED |
|---|---:|
| Alderford gate → square | 20–35 s |
| square → Greenwater Ford activity edge | 45–75 s |
| square → first Alder Meadow meaningful interaction | 45–90 s |
| square → Old Quarry Road disturbance belt | 90–150 s |
| nearest practical R01 checkpoint → quarry entrance after discovery | 60–120 s |
| quarry entrance → Earthloong chamber on first clear | 12–20 min traversal/combat, plus boss |

If actual Azari geometry violates these enough to create dead travel, move the authored anchor during spatial closure rather than adding filler quests.

## 3.2 Expected Lv pacing seeds

These are observation targets, not hard gates.

| Moment | Expected ordinary first-play Lv |
|---|---:|
| reach Alderford | 1–2 |
| complete `Dust on the Quarry Road` | **2–3** |
| ordinary Trail Stag timing | **3–4** |
| inspect lower quarry entrance | **4–6** |
| enter Earthloong chamber | **5–7** |
| finish first Earthloong clear | **6–8** |

A player outside the band is not blocked. **Suggested Lv 8** at the quarry is a safety/readability recommendation, not a claim that every ordinary player must already be Lv8 before entering; a broad-path player is expected to approach Lv6–8 by the first clear, while a completionist route can arrive closer to the upper edge.

R01 intentionally front-loads **one-time** EXP/Class-XP rewards above later-region baseline percentages so the opening actually reaches these bands. Repeat events, repeat bosses and ordinary combat do not inherit that uplift.

If median playtest progression misses the band by more than about two levels, retune rewards/content density before changing region identity.

---

# 4. R01 content catalogue

## Main / regional progression

1. `Dust on the Quarry Road`
2. `Roots Below Stone`
3. Quarry relay evidence
4. Earthloong first clear
5. post-quarry Alderford briefing
6. R02/R03 peer route handoff

## Authored optional content

1. `Riverbank Remedies`
2. `Signs in the Meadow`
3. `A Stag at the Ford`
4. `Steel in the Grass`
5. `The Crowned Trail` / Regalhart
6. fishing/codex discovery
7. first-home inspection/purchase
8. Field Camp Kit unlock/craft
9. gathering/mastery discovery
10. repeatable roadside world event

No additional R01 board contract is added before playtest proves a real dead zone.

## 4.1 Substantial exploration POIs

R01 uses exactly four substantial non-hub/non-boss exploration complexes in addition to the Quarry dungeon and Regalhart encounter.

### Greenwater Ford

Existing Trail Stag/fishing/gathering space.

Required identity:
- wide readable ford;
- snapped tether post / broken ford gate route for A Stag at the Ford;
- Healing Herb bank;
- two of the five ordinary R01 fishing spots;
- one visible route back toward Alderford stables.

No separate chest is added; the mount/event/fishing/gathering loops are already the reward.

### Mosswheel Mill

Final player-facing name: **Mosswheel Mill**.

An abandoned watermill on a Greenwater branch, visibly old but not Anchor-era.

Purpose:
- memorable non-combat exploration;
- early vertical/interior movement without a dungeon;
- ordinary local history so every old structure is not part of the Anchor mystery.

Interaction:
1. discover exterior mill;
2. enter through the broken wheel-side lower door or loft route;
3. reach the dry upper loft;
4. open one personal one-time mill cache.

Discovery reward:
~~~text
EXP: **14%** current next-Lv requirement
Gold: 20
~~~

Personal mill cache:
~~~text
Gold: 30
Hardwood x3
normal authored-treasure equipment roll: 25%
~~~

The cache uses the R01 meadow/road treasure pool in §4.4.

No combat is required and no quest entry is created.

### Rootshade Grove

Existing dangerous optional pocket.

Required identity:
- darker forest-fringe silhouette;
- 2 Verdant Crystal nodes;
- Nature Spirit/Steelboar pressure under ecology caps;
- one Regalhart clue/start-boundary relationship;
- route sightline that makes deeper danger readable before entry.

The grove itself grants:
~~~text
first discovery EXP: **14%** current next-Lv requirement
~~~

No generic grove chest is added.

### Quarry Surface Works

Existing Old Quarry Road → overlook → lower entrance complex.

Required identity:
- surface ore/loading structures;
- quarry overlook discovery;
- Quarry Waystone;
- root-split lower entrance;
- visible relationship to the physical dungeon below.

Its rewards remain owned by Roots Below Stone / dungeon content; no duplicate POI reward is added.

## 4.2 Minor discoveries / landmarks — exact six

These six are normal first-visit discoveries. None receives an undiscovered map pin.

Each discovery grants:
~~~text
EXP: **10%** current next-Lv requirement
one-time per player
~~~

### Bent Roadwatch

Small ruined road/watch shelter between Alderford and the quarry road.

Additional personal cache:
~~~text
Gold 15
Healing Potion x1
~~~

### Shepherd's Overlook

A meadow rise with a strong Alderford/ford/quarry-direction sightline.

No chest. Its value is orientation + discovery EXP.

### Twin-Willow Bend

Distinct river bend framed by two large willow silhouettes.

- hosts one ordinary fishing spot;
- no additional chest;
- after discovery the name can appear on the map.

### Drover's Rest

Old livestock/trader rest shelter near a secondary road.

Personal one-time pickup:
~~~text
Trail Skewers x1
~~~

### Quarrymen's Memorial

Small carved memorial outside the active quarry works.

- no loot;
- one short environmental inscription about ordinary quarry workers;
- does not mention Anchors or foreshadow the full network before the quarry evidence.

### Root-Split Cairn

Old local waystone/cairn at the safer edge of Rootshade Grove, visibly split by newer root growth.

Personal one-time pickup:
~~~text
Healing Herb x2
~~~

It provides a visual transition into the dangerous grove but is not an Anchor artifact.

## 4.3 Exact R01 resource-node counts

Coordinates remain SPATIAL_BINDING; counts and subregion ownership are HARD_RULE starting content.

| Subregion | Iron Ore | Hardwood | Healing Herb | Verdant Crystal |
|---|---:|---:|---:|---:|
| Alderford Approach | 0 | 0 | 1 | 0 |
| Greenwater Ford | 0 | 2 | 6 | 0 |
| Alder Meadow | 0 | 2 | 3 | 0 |
| Riverwood Edge | 0 | 7 | 3 | 0 |
| Old Quarry Road | 3 | 2 | 1 | 0 |
| Rootshade Grove | 0 | 3 | 2 | 2 |
| Quarry Surface Works | 4 | 0 | 0 | 0 |
| Quarry dungeon | 4 | 0 | 0 | 2 |
| **R01 total** | **11** | **16** | **16** | **4** |

Rules:
- all use personal availability/cooldowns from gathering canon;
- decorative ore/log/herb meshes that are not gatherable must be visually distinguished by interaction highlighting only when targeted, not by giant beams;
- quest-specific personal herb availability for Riverbank Remedies uses these real Greenwater/nearby nodes rather than spawning a separate fake quest herb set;
- the two dungeon Verdant Crystal nodes are separate from the Root-Breached one-time side-cache reward.

## 4.4 R01 source-specific ordinary equipment pools

The global grade/affix tables still apply. This table only locks which ordinary R01 base families belong to each source.

### Meadow / road authored treasure

- Heartland Arming Sword
- Wayfarer Daggers
- Riverwood Bow
- Wayfarer Leathers
- River Scholar Garb
- Roadworn Band
- Wayfarer's Token

### Quarry treasure

- Quarry Maul
- River Pike
- Watch Buckler
- Ironbound Guard
- Apprentice Focus
- Quarry Seal

### Steelboar equipment roll

- Quarry Maul
- River Pike
- Watch Buckler
- Ironbound Guard

### Nature Spirit equipment roll

- Initiate Staff
- Initiate Wand
- Apprentice Focus
- River Scholar Garb
- Greenwater Pendant

### Regalhart normal equipment roll

- River Pike
- Riverwood Bow
- Wayfarer Daggers
- Wayfarer Leathers
- Greenwater Pendant
- Wayfarer's Token

The separate Hartcrown Spear signature roll is unchanged.

### Earthloong normal equipment roll

- Quarry Maul
- Watch Buckler
- Ironbound Guard
- Initiate Staff
- Apprentice Focus
- Quarry Seal

The separate Rootquake Maul / Earthscale Ward signature pool and deterministic first-clear choice remain unchanged.

No source falls back to a global bag containing every R01 item.

For every R01 ordinary equipment roll, **eligible base families inside the source pool are equal-weight by default**. Grade/category/affix selection then follows the global loot/equipment rules. A future unequal base-family weight must be written into canon/data explicitly; implementation does not invent one.

---

# 5. Contextual combat teaching — exact trigger

The first Meadow Viper can teach Dodge once.

Internal state:

```text
r01_dodge_hint_seen: boolean
r01_dodge_used_once: boolean
```

Trigger:

```text
player has not seen the hint
AND player has not successfully performed a dodge
AND player is in the authored first-road Viper encounter
AND the Viper enters its committed Coil Bite warning
```

Presentation:

- show one compact Dodge prompt **0.25 s after** the warning posture begins;
- prompt remains for **1.25 s** or until a dodge input is detected;
- gameplay never pauses;
- prompt is suppressed while another higher-priority accessibility/system prompt owns the same screen area;
- if suppressed, it is retried on the next committed Coil Bite in the same encounter;
- after one actual display, `r01_dodge_hint_seen=true` whether the dodge succeeds or fails;
- once the player performs any valid dodge, `r01_dodge_used_once=true` and the hint never appears again on that character.

There is no repeated “press Dodge” spam.

---

# 6. Repeatable world event — Roadside Trouble

Final player-facing event title: **Roadside Trouble**.

Purpose:

- make Old Quarry Road feel inhabited;
- provide the fifth optional useful-action category for `Dust on the Quarry Road`;
- teach that a world event can be solved by combat **and** practical interaction;
- create spontaneous co-op without a board quest.

## 6.1 Activation

First-cycle eligibility:

```text
Alderford gate shrine activated
AND no Roadside Trouble event currently active
AND player enters the authored Old Quarry Road event volume
AND at least 4 active-play minutes have passed since shrine activation
```

Repeat eligibility after a completed/abandoned cycle:

```text
12 active-world minutes since previous cycle ended
AND event volume currently has no active Roadside Trouble instance
```

Entering the volume starts the event immediately when eligible. If an eligible incomplete player entered before the 4-minute first-cycle threshold and remains inside, the controller performs one eligibility recheck when that threshold is crossed so the player is not forced to step out and back in.  
There is no random percentage roll.

## 6.2 Physical scene

A small trade wagon has thrown a wheel and spilled cargo at a bend.

Driver start bark:

> “Wheel's gone, and the grass started moving. Clear them out and give me a hand.”

Completion bark:

> “That's enough. We'll get moving before the road finds another problem for us.”

These are ambient event lines, not a dialogue tree.

Required world actors:

- one stranded wagon;
- one driver/worker NPC;
- one damaged-wheel interaction;
- one displaced cargo crate interaction;
- a Meadow Viper threat pack.

Solo baseline threat:

```text
2 Meadow Vipers
```

Scaling:

```text
+1 Meadow Viper per additional active participant
maximum total event threat count: 4
```

## 6.3 Completion

The event completes when all three requirements are true:

1. active event threat pack defeated;
2. wheel brace interaction completed;
3. one cargo crate returned to the wagon.

Different players may perform different requirements.

One valid requirement action grants event participation:
- qualifying combat/support action;
- wheel brace;
- cargo return.

Zero-action proximity grants nothing.

## 6.4 Reward

Per eligible participant:

```text
EXP: 5% of current next-Lv requirement
Class XP: 4% of current Class Rank requirement
Gold: 20
```

If `Dust on the Quarry Road` is active and the player has not already credited the road-event category, completion credits that category once.

No unique gear/material is attached to this event.

## 6.5 Abandon/reset

If no incomplete/participating player remains within the event volume for **120 s**:

- physical event state resets;
- no completion reward is granted;
- the cycle ends;
- repeat eligibility begins its normal 12-minute timer.

Disconnect/relog does not instantly reroll the event.

---

# 7. R01 ecology / spawn seed

R01 uses authored ecology anchors, not world-wide random scanning.

Exact anchor coordinates are SPATIAL_BINDING.  
The counts and initial weights below are TUNEABLE_SEED values.

## 7.1 Group sizes

| Actor | Spawn group |
|---|---:|
| Louxia | 2–4 |
| Gazelle | 2–4 |
| Raccoon | 1 |
| Crow | 1–3 |
| Bison | 3–5 |
| Meadow Viper | 1; **20%** of valid danger-anchor cycles produce 2 |
| Grizzly | 1 |
| Steelboar | 1 |
| Nature Spirit | 1 |
| Cave Centipede | 1–2 |

## 7.2 Authored anchor counts

| Subregion | Ecology anchors |
|---|---:|
| Alderford Approach | 4 |
| Greenwater Ford | 5 |
| Alder Meadow | 8 |
| Riverwood Edge | 6 |
| Old Quarry Road exterior | 5 |
| Rootshade Grove | 5 |
| quarry cave non-dungeon pockets | 4 |

Dungeon-room actors remain encounter-controller owned and do not use these natural anchors.

## 7.3 Anchor-role weights

### Alderford Approach

```text
Louxia/Gazelle ambient: 40%
Crow/Raccoon ambient: 20%
Meadow Viper: 15%
empty/quiet: 25%
```

No Bison, Grizzly, Steelboar or Nature Spirit anchor is allowed on the first approach.

### Greenwater Ford

```text
Louxia/Gazelle: 35%
Crow/Raccoon: 20%
Meadow Viper: 20%
Bison edge herd: 10%
empty/quiet: 15%
```

No Steelboar/Nature Spirit natural anchor.

### Alder Meadow

```text
Louxia: 25%
Gazelle: 15%
Bison: 20%
Crow/Raccoon: 10%
Meadow Viper: 15%
Steelboar danger anchor: 5%
empty/quiet: 10%
```

Steelboar anchors must be outside the starter-road safety corridor.

### Riverwood Edge

```text
Louxia/Gazelle: 20%
Crow/Raccoon: 15%
Meadow Viper: 20%
Grizzly: 10%
Steelboar: 10%
Nature Spirit: 5%
empty/quiet: 20%
```

### Old Quarry Road exterior

```text
Louxia/Gazelle: 15%
Crow/Raccoon: 15%
Meadow Viper: 25%
Bison: 10%
Steelboar: 10%
empty/quiet: 25%
```

The repeatable Roadside Trouble controller is separate and does not consume a natural anchor.

### Rootshade Grove

```text
Crow/Raccoon: 10%
Meadow Viper: 20%
Grizzly: 15%
Steelboar: 20%
Nature Spirit: 10%
empty/quiet: 25%
```

## 7.4 Pressure caps

Within any 128-block local play area:

- maximum one Steelboar;
- maximum one Nature Spirit;
- maximum one Grizzly;
- Steelboar and Nature Spirit may not both naturally activate within 72 blocks of the same player;
- Regalhart never consumes the ordinary elite cap;
- event-owned threats do not cause ordinary anchors to immediately backfill additional threats.

## 7.5 Replenishment

Anchor actor/group selection uses deterministic server RNG seeded from `world_seed + authored_anchor_id + respawn_cycle_index`. The same committed cycle cannot reroll because a client reconnects, unloads a chunk or reopens a UI.

A vacated natural anchor can replenish only when:

- its previous actor/group was removed or legitimately despawned;
- **120 s** have elapsed;
- no player is within **32 blocks** of the anchor.

No R01 progression-critical actor is time/weather exclusive.  
**R01 launch time/weather does not alter ecology spawn/group weights.** Time/weather may alter accepted ambience/presentation only. Any later spawn-weight effect requires a canon/data revision first and cannot hide a required R01 quest, mount, dungeon or boss.

---

# 8. Quarry checkpoint, death and repeat-run contract

## 8.1 Quarry Waystone

Final player-facing name: **Quarry Waystone**.

It is an exterior field shrine/checkpoint, not an interior dungeon shrine.

Activation:

```text
personal lower-entrance inspection completed
→ Quarry Waystone becomes interactable
→ first legitimate interaction activates personal checkpoint + fast-travel destination
```

Spatial rule:

- outside the dungeon;
- intended 35–70 blocks of legal travel from lower entrance;
- not visible as a discovered map destination before lower-entrance inspection.

Exact coordinate is SPATIAL_BINDING.

## 8.2 Death / retry

Before Quarry Waystone activation:
- normal R01 respawn uses Alderford gate shrine.

After Quarry Waystone activation:
- death in quarry exterior/dungeon uses Quarry Waystone unless another later legal checkpoint has explicitly replaced it.

Solo ordinary-room defeat:
- current uncleared room resets to its room-entry combat state;
- previously cleared rooms in the same run remain cleared;
- opened lift shortcut remains open for that run.

Earthloong wipe:
- Earthloong HP/poise/status/phase state fully resets;
- boss-arena breakable props reset;
- the player respawns at Quarry Waystone;
- the lift shortcut remains open for the current run;
- the entire dungeon is **not** re-cleared.

Multiplayer:
- if living eligible players remain in the active encounter, normal Downed/revive rules apply;
- a defeated player who fully respawns does not force-reset the boss while other eligible participants are still legitimately fighting;
- scaling resynchronizes only at the authored safe rules in `PARTY_MULTIPLAYER.md`.

## 8.3 Run reset

A cleared quarry run can reset in either way:

1. all players leave the dungeon volume for **90 s**; or
2. after a clear, all players leave the dungeon and an eligible player uses **Reset Quarry** at the Quarry Waystone.

Reset Quarry:
- requires no player currently inside the dungeon;
- requires no active quarry encounter;
- resets room enemies, lift shortcut and Earthloong controller;
- **R01 has no repeatable authored treasure cache**: the Upper Mining Gallery and Root-Breached Workings side caches are personal one-time rewards and never reset;
- ordinary resource nodes use their own personal cooldown state and may naturally be available again;
- does not reset personal first-clear/story evidence;
- does not duplicate one-time treasure/discovery rewards.

There is no arbitrary multi-hour dungeon lockout.

---

# 9. Regalhart repeat controller

Regalhart remains optional.

## 9.1 First discovery

Existing `The Crowned Trail` rules remain:
- two of three clue types reveal broad territory;
- boss may be found before clues;
- clue completion is never required to damage/defeat Regalhart.

## 9.2 Territory controller

After a valid defeat:

```text
repeat_eligible_after = 20 active-world minutes
```

Reactivation additionally requires:
- Regalhart territory has had no active boss instance;
- no player has remained inside the core boss arena for at least **60 s** after eligibility.

When valid, the controller chooses one of **3 spatially-authored territory start anchors** from a deterministic world/cycle seed.

It never materializes directly in a player's camera or within 24 blocks of a player.

## 9.3 Disengage/reset

If no eligible engaged player remains within Regalhart territory for **25 s**:

- active combat ends;
- Regalhart returns/resets to its controller start state;
- HP/poise/status reset;
- no reward granted.

## 9.4 Repeat reward

Repeat defeat uses the canonical field/world-boss repeat package:
- guaranteed normal equipment roll;
- 25% second normal equipment roll;
- Regalhart Antler x1;
- 15% signature/Mythic roll;
- repeat boss EXP target from global canon.

The R01 discovery/quest reward and first-defeat bonuses do not repeat.

---

# 10. Alderford service catalogue

Buildings exist from first arrival.  
No service tour is required.

## 10.1 Nessa Bell — market equipment rotation

Nessa owns **5 rotating personal equipment slots**.

Refresh:
```text
10 active-world minutes
deterministic personal merchant cycle
relog/UI reopen does not reroll
```

The personal merchant epoch starts when the player's Alderford gate shrine first commits. Merchant active time advances only while that player is legitimately loaded in the server world; offline time does not generate extra refreshes.

```text
cycle_index = floor((personal_active_world_time - alderford_shrine_activation_time) / 10 active minutes)
```

Cycle RNG is deterministic server-side from:

```text
world_seed
+ player_uuid
+ merchant_id("nessa_bell")
+ cycle_index
```

Implementation may choose a stable hash/PRNG primitive, but the same inputs must reproduce the same cycle and client reconnect/UI reopen cannot reroll it.

Slots:

1. weapon;
2. weapon/off-hand;
3. armor;
4. armor;
5. accessory.

### Exact slot-base generation

**Slot 1 — Weapon**

Equal-weight one base from:

- Heartland Arming Sword
- Wayfarer Daggers
- Quarry Maul
- River Pike
- Riverwood Bow
- Initiate Staff
- Initiate Wand

**Slot 2 — Weapon / Off-hand**

First choose category:

```text
weapon: 50%
off-hand: 50%
```

If weapon, choose equal-weight from the seven Slot-1 weapon bases **excluding Slot 1's exact base**.

If off-hand, choose equal-weight:
- Watch Buckler
- Apprentice Focus

**Slots 3 and 4 — Armor**

First choose two **distinct** armor equipment slots from:

```text
Head
Chest
Legs
Gloves
Boots
```

All 10 unordered slot pairs are equal-weight.

For each chosen slot, choose one armor family equal-weight:

```text
River Scholar Garb
Wayfarer Leathers
Ironbound Guard
```

Therefore Slots 3 and 4 can never be the exact same armor piece in one cycle.

**Slot 5 — Accessory**

Equal-weight:
- Greenwater Pendant — Necklace
- Roadworn Band — Ring
- Wayfarer's Token — Charm
- Quarry Seal — Relic

### Grade generation

R01 rotating grade weights per generated slot:

```text
Standard: 50%
Refined: 40%
Superior: 10%
Exalted: 0%
Mythic: 0%
```

At most **1 Superior** appears in a five-slot cycle.

Generation is evaluated in slot order 1 → 5:
- the first Superior result is kept;
- if a later slot also rolls Superior, reroll that later grade using only Standard/Refined at normalized 5:4 weight.

Fixed R01 Item Lv by generated grade:

```text
Standard: Item Lv2
Refined: Item Lv4
Superior: Item Lv6
```

Affix identities/values are generated once from the same cycle + slot seed and persist for that entire cycle.

### Purchase state

Each slot is a **single personal stock item per cycle**.

- one successful purchase marks that slot SOLD for that player until the next cycle;
- selling the purchased item back does not restore the merchant slot;
- reconnect does not restore SOLD stock;
- a cycle refresh replaces all unpurchased/sold slots with the next deterministic cycle;
- if the 10-minute boundary passes while the market UI is open, the server rejects any stale old-cycle Buy request and the client refreshes to the new cycle;
- there is no paid/manual reroll.

This prevents infinite duplicate purchases from one favorable roll while preserving the intended frequent early stock refresh.

### R01 merchant base prices

| Category | Standard | Refined | Superior |
|---|---:|---:|---:|
| weapon | 150 | 240 | 500 |
| off-hand | 130 | 210 | 450 |
| armor piece | 120 | 200 | 420 |
| accessory | 110 | 180 | 380 |

Affixes follow normal grade rules.  
Sell-back remains the global 25% equipment baseline.

Nessa does not sell Mythic/signature boss gear.

## 10.2 Lysa Fen — recovery/alchemy

Reliable purchase stock:

| Item | Price |
|---|---:|
| Healing Potion | 30 Gold |
| Focus Draught | 35 Gold |
| Cleansing Tonic | 40 Gold |

These do not rotate out and use **unlimited fixed personal purchase availability** in R01. There is no stock count, daily reset or buyout timer; Gold and legal destination space are the purchase constraints.

Alchemy service recipes remain:

```text
Healing Potion = 2 Healing Herb + 5 Gold
Focus Draught = 1 Healing Herb + 1 Louxia Glow + 8 Gold
Cleansing Tonic = 1 Healing Herb + 1 Louxia Glow + 10 Gold
```

## 10.3 Brin Hale — prepared food

Reliable prepared-meal stock:

| Meal | Price |
|---|---:|
| Herbed Louxia Roast | 25 Gold |
| Trail Skewers | 20 Gold |
| Glow Broth | 25 Gold |

Prepared meals use **unlimited fixed personal purchase availability** in R01. There is no stock count/daily reset. Cooking from owned ingredients remains available through the normal service and does not require buying prepared food.

## 10.4 Daren Holt — forge

R01 recipe set is exactly the recipe table in `EQUIPMENT_BALANCE.md`.

Daren does not sell unlimited Iron Ore, Hardwood, Verdant Crystal, Regalhart Antler or Earthloong Scale.  
Those materials must retain world-source value.

## 10.5 Oren Quill — vault

- Material Vault access: free;
- Alderford forge/alchemy/cooking may spend same-player Material Vault materials after Material Pouch quantities under the global atomic sourcing rule;
- ordinary Personal Storage access: free;
- R01 Personal Storage capacity: **36 ordinary slots per player**;
- important reward overflow uses backpack → Personal Storage → Pending Reward Claim as defined in `GAME_DESIGN.md`;
- no deposit/withdraw fee.

## 10.6 Elian Rook — class service

- first root class selection: free;
- later class switching uses the global Lv-scaled/capped class-switch formula;
- no R01-exclusive switch surcharge.

## 10.7 Toma Reed — stable

- Trail Stag first unlock remains free through `A Stag at the Ford`;
- no R01 mandatory registration/tack fee after the event;
- R01 does not sell a faster paid mount that invalidates Trail Stag immediately.

---

# 11. R01 tool-tier boundary

R01 starts and ends with the **Field** tier for Pick, Axe, Harvest Knife/Sickle and Fishing Rod.

- no Refined or Masterwork gathering-tool upgrade recipe is sold, crafted or rewarded in R01;
- all ordinary R01 nodes, including Verdant Crystal, are intentionally Field-tool accessible;
- later regions may introduce the first Refined upgrade only through their own explicit regional canon;
- implementation must not invent an Alderford “better pickaxe” shop because three global tool tiers exist.

---

# 12. Field Camp Kit — exact R01 acquisition

The Field Camp Kit is not a starter item and not a quest reward.

Recipe discovery trigger:

```text
player has obtained at least 1 Hardwood
AND player has obtained at least 1 Tough Hide
→ Field Camp Kit recipe becomes permanently known
```

The player-facing recipe becomes visible at Holt Forge immediately.

Craft:

```text
4 Hardwood
+ 2 Tough Hide
+ 40 Gold
→ permanent Field Camp Kit unlock
```

Ownership:

- one permanent utility unlock per player;
- stored as project utility/Tool-Pouch state, not an ordinary consumable stack;
- not sellable;
- not tradeable;
- not droppable;
- crafting a second copy is impossible while the unlock exists.

No separate camp tutorial quest is added.

## 12.1 Exact R01 Camp placement

R01 uses the following exact placement-rule set:

```text
horizontal footprint: 5.0 x 5.0 blocks
maximum support-surface height variance across footprint: 0.75 block
minimum distance from activated shrine or authored major-service-center anchor: 24 blocks
minimum distance from any other active player camp anchor: 12 blocks
minimum distance from active boss-arena boundary: 24 blocks
```

Additional legality:
- owner must be out of combat;
- footprint may not intersect settlement/dungeon/event/protected/no-camp volumes;
- footprint may not intersect liquid or unsupported air columns needed by the camp's solid footprint;
- at least **80% of the 5x5 support sample area** must resolve to stable walkable support; the remaining uneven edge may be handled by the accepted visual ground dressing but cannot float the functional camp;
- camp entrance/service interaction side must have a **1.5-block-wide** clear approach lane;
- no R01 party-camp spacing exception exists: formal party members still use the same 12-block minimum;
- roads/critical navigation use authored no-camp volumes rather than an expensive runtime full-road scan.

Redeploy is atomic:
1. validate new position while old camp remains authoritative;
2. commit new camp position/service state;
3. remove old camp presentation;
4. on failure, old camp remains untouched.

The final accepted camp asset/composition must fit this rule set. Asset binding does not get to silently enlarge the gameplay footprint.

First legal deployment uses the normal world placement preview and one compact contextual explanation of rest/cooking/redeploy behavior.

---

# 13. R01 profession and gathering progression

## 13.1 Smithing Mastery Insight — R01 maximum 4

R01 can award exactly these Smithing Insight flags:

1. first successful craft of any baseline R01 Refined weapon/off-hand;
2. first successful craft of a **second distinct** baseline R01 Refined weapon/off-hand;
3. first successful R01 Superior recipe;
4. first successful Regalhart/Earthloong signature craft.

Crafting more baseline copies gives no extra Insight.

## 13.2 Alchemy Mastery Insight — R01 maximum 3

1. first Healing Potion craft;
2. first Focus Draught craft;
3. first Cleansing Tonic craft.

`Riverbank Remedies` itself does not award a separate Alchemy Insight because it teaches gathering rather than potion production.

## 13.3 Cooking Mastery Insight — R01 maximum 4

1. first Herbed Louxia Roast;
2. first Trail Skewers;
3. first Glow Broth;
4. first Grilled Catch.

## 13.4 Gathering Mastery exact R01 contract bonuses

Normal gather XP remains global.

One-time authored bonuses:

- first Iron Ore material-family discovery: Mining +10 Mastery XP;
- first Verdant Crystal discovery: Mining +10;
- first Hardwood material-family discovery: Forestry +10;
- first Healing Herb material-family discovery: Herbalism +10;
- `Riverbank Remedies` completion: Herbalism +10;
- each first R01 fish identity follows the normal first-discovery Fishing bonus.

No mastery reward is duplicated by relogging.

---

# 14. R01 fishing package — mechanics locked before final model naming

The final player-facing fish names are ASSET_BINDING because the project refuses to name species first and force weak mismatched models afterward.

**Everything except final name/model/icon binding is closed here.**

## 14.1 R01 fish identities

| Internal content slot | Rarity | Habitat | Min | Normal band | Max | Base sell | Grillable | Catch role |
|---|---|---|---:|---:|---:|---:|---|---|
| `r01_river_common_a` | Common | moving river / ford | 16 cm | 24–34 cm | 42 cm | 4 Gold | yes | easiest first catch |
| `r01_river_common_b` | Common | riverbank / slow bend | 12 cm | 18–28 cm | 34 cm | 5 Gold | yes | small ordinary catch |
| `r01_pool_uncommon` | Uncommon | deeper slow pool / Riverwood edge | 24 cm | 32–46 cm | 54 cm | 7 Gold | yes | longer tension introduction |
| `r01_ford_rare` | Rare | authored quiet/deep R01 spot | 32 cm | 44–64 cm | 76 cm | 14 Gold | yes | R01 record/trophy chase |

For each slot, final accepted external fish model determines the ordinary player-facing species name before gameplay source for that slot is authored.

## 14.2 Spot count

Spatial closure must place exactly:

- **5 ordinary R01 fishing spots**;
- **1 uncommon/deeper pool spot**;
- **1 rare spot**.

Ordinary spot:
```text
2 personal catches
6 active-minute personal respawn
```

Uncommon pool:
```text
2 personal catches
8 active-minute personal respawn
```

Rare spot:
```text
1 personal catch
18 active-minute personal respawn
```

## 14.3 Species pool

Ordinary spot:
```text
common_a 55%
common_b 35%
pool_uncommon 10%
```

Uncommon pool:
```text
common_a 25%
common_b 25%
pool_uncommon 45%
ford_rare 5%
```

Rare spot:
```text
common_a 10%
common_b 10%
pool_uncommon 45%
ford_rare 35%
```

R01 has no weather-only or midnight-only fish required for the four-species regional set.

## 14.4 Catch RNG / reroll protection

Each personal fishing spot maintains:

```text
spot_cycle_index
catch_ordinal_in_cycle
pending_candidate | null
```

Candidate RNG is deterministic server-side from:

```text
world_seed
+ player_uuid
+ authored_spot_id
+ spot_cycle_index
+ catch_ordinal_in_cycle
```

The candidate contains species + size roll and is created when the first valid cast at that catch ordinal reaches bite generation.

Rules:
- reopening UI, chunk unload or reconnect does not reroll the pending candidate;
- missing the Hook window does **not** consume the spot charge and does **not** replace the pending candidate;
- cancelling/reeling in before a bite does not consume the charge and does not create a new candidate;
- a **successful Hook commit consumes one personal spot charge immediately**;
- after a successful Hook, tension failure/disconnect yields no fish and the consumed charge remains consumed;
- after resolution, increment catch ordinal and clear pending candidate;
- spot respawn starts a new spot cycle only after the authored active-time cooldown.

This removes intentional miss/relog fishing rerolls.

## 14.5 Exact R01 size distribution

Generate a base uniform percentile `u` in [0, 1).

Map it piecewise:

```text
u 0.00–0.10:
  linear min_size → normal_size_low

u 0.10–0.90:
  linear normal_size_low → normal_size_high

u 0.90–1.00:
  linear normal_size_high → max_size
```

Therefore 80% of baseline R01 catches land inside the authored normal band while true extremes remain uncommon.

Fishing Mastery Rank V:
- generate **one additional independent size percentile** from the same candidate seed using a separate sub-roll;
- keep the larger resulting size;
- species does not reroll;
- this is the exact meaning of the global “one extra bounded trophy-size/value roll”.

Trophy threshold:

```text
trophy_threshold =
  max_size - 0.05 * (max_size - min_size)
```

A fish is Trophy if `size >= trophy_threshold`.

This defines the upper 5% **size band**, not a guaranteed 5% catch probability.

## 14.6 Exact R01 sale-value multiplier

For non-Trophy catches, linearly interpolate by these size anchors:

```text
min_size                 → 0.85x
normal_size_low          → 0.95x
midpoint(normal band)    → 1.00x
normal_size_high         → 1.10x
trophy_threshold         → 1.25x
```

Trophy:
```text
1.50x
```

Final raw-fish sale value:

```text
max(1, round(base_sell_value * size_multiplier))
```

The Trophy 1.50x replaces the non-Trophy interpolation rather than stacking on top of 1.25x.

## 14.7 Exact R01 bite / Hook timing

Bite delay uses a uniform server-side roll:

```text
min: 1.50 s
base max: 5.00 s
```

Fishing Mastery:
- Rank II: max = 4.60 s;
- Rank IV+: max = 4.25 s.

Hook windows:
- Common: 0.90 s;
- Uncommon: 0.75 s;
- Rare: 0.65 s;
- Trophy candidate: 0.55 s;
- Rank III+: add +0.08 s to the applicable window.

The server already knows whether the pending candidate is Trophy; the client receives only the appropriate readable bite presentation/window, not hidden size data.

## 14.8 Exact R01 catch resolution

Common non-Trophy:
- successful Hook → **1.20 s** accepted reel/catch presentation → reward;
- no tension minigame.

Any Trophy candidate, plus all Uncommon/Rare:
- enter tension.

Normalized tension:
```text
range: 0.00–1.00
start: 0.45
baseline valid zone: 0.25–0.75
Rank V valid zone: 0.225–0.775
```

Fish pull per second:
```text
Uncommon: +0.08
Rare: +0.12
Trophy: +0.16
```

Player input:
```text
Hold / reel: +0.28 tension per second
Release:     -0.32 tension per second
```

Net tension movement = fish pull + current player-input term.

Catch progress:
- increases at 1.0 progress-second per real second **only while Hold is active and current tension is inside the valid zone**;
- pauses outside the valid zone;
- never becomes negative.

Required catch progress:
```text
Uncommon non-Trophy: 3.2 s
Rare non-Trophy:     4.4 s
Any Trophy:          5.8 s
```

Failure:
- tension >=0.95 continuously for 0.40 s → line breaks;
- tension <=0.05 continuously for 0.60 s → fish escapes;
- either failure ends the already-consumed hooked attempt with no fish reward.

The values are TUNEABLE_SEED only after real Minecraft fishing feel evidence exists; implementation begins exactly here.

The low raw sale values are intentional: fishing supports collection/cooking/economy but must not beat meaningful quest/dungeon income.

---

# 15. Alderford housing roster

Alderford contains exactly **5 authored purchasable residential shells** at launch.

## 15.1 Small Cottage vacancies — 4

All four:

```text
tier: Small Cottage
price: 2,400 Gold
Home Storage: 54 slots
```

Final player-facing property labels:

1. **Gate Cottage**
2. **Paddock Cottage**
3. **Riverside Cottage**
4. **Quarry-Road Cottage**

Exact shell model/coordinates are ASSET_BINDING + SPATIAL_BINDING.

## 15.2 Visible upgrade home — 1

Final player-facing property label: **Market House**.

```text
tier: Town House
price: 9,000 Gold
Home Storage: 72 slots
```

It is purchasable from the beginning if the player legitimately has enough Gold.  
It is not story-gated.

Alderford has no Large Residence or Prestige Estate property at launch; later settlements provide those tiers.

## 15.3 Exact starter furnishing package — 750 Gold

| Furnishing | Package value |
|---|---:|
| bed/rest furnishing | 180 |
| Home Storage access cabinet | 140 |
| table | 90 |
| chair x2 | 120 |
| matching light/lantern pair | 70 |
| shelf/cabinet | 60 |
| trophy/display stand | 90 |
| **Total** | **750** |

The package is optional.

Buying the package grants the furnishing items into the house's Home Storage/Moving-safe furnishing inventory; it does not auto-place them into an arbitrary layout.

## 15.4 Exact R01 furnishing catalogue

R01 does not leave a generic “add furniture later” catalogue to implementation.

Nessa Bell exposes a fixed **Household** catalogue to players who own a residence. This catalogue does **not** use the 10-minute equipment rotation.

| Furnishing | Price | Function |
|---|---:|---|
| Alderford Bed | 180 Gold | home rest access |
| Storage Cabinet | 140 Gold | Home Storage access point only; does not add capacity |
| Plain Table | 90 Gold | decor / placement surface |
| Alderford Chair | 60 Gold | sittable |
| Iron Lantern | 35 Gold | interior light |
| Wall Shelf | 60 Gold | decor/display surface |
| Trophy Stand | 90 Gold | one eligible trophy/display |
| Wardrobe | 160 Gold | Wardrobe/appearance access |
| Cooking Hearth | 220 Gold | enables home cooking service |
| Woven Rug | 70 Gold | decor |
| Wooden Bench | 85 Gold | sittable decor |
| Side Table | 50 Gold | decor / placement surface |

Rules:

- this is the **entire baseline purchasable R01 furniture catalogue**; implementation does not add random extra functional furniture;
- accepted external models/material variants may supply visual variants, but variants do not change price/function unless canon is revised first;
- the 750-Gold starter package is a one-time purchase option attached to the first successful residence purchase and grants exactly its listed pieces;
- declining the package does not remove later access to the individual catalogue;
- furnishing purchases go directly to the owned residence's Home Storage/furnishing inventory;
- beds, chairs, benches, cabinets, wardrobe and hearth use their real accepted external interaction/model presentation rather than decorative fake blocks.

## 15.5 Exact R01 furnishing placement

R01 placement intentionally stays simpler than a construction game.

Position:
- horizontal anchor snaps to a **0.25-block grid**;
- vertical position snaps to the validated supporting surface;
- rotation is exactly **0° / 90° / 180° / 270°**;
- no free-angle/finer rotation in R01.

Surface classes:
- Alderford Bed / Storage Cabinet / Plain Table / Chairs / Wardrobe / Cooking Hearth / Bench / Side Table / Trophy Stand → floor-supported;
- Iron Lantern → floor/table/shelf-supported;
- Woven Rug → floor-surface only;
- Wall Shelf → authored wall-surface only.

Validation:
- furniture's accepted placement collision box, inflated horizontally by **0.05 block**, must remain inside the owned furnishing volume;
- solid placement collision may not overlap another solid furnishing or protected shell collision;
- decorative non-solid extents may visually approach walls only where the accepted model's authored placement box permits it;
- authored doorway/critical-interaction clearance volumes are no-furniture volumes;
- a functional furnishing must preserve its authored interaction approach point.

Move remains atomic: original placement is not removed until the new placement validates/commits.

No R01 furnishing uses physics stacking, freeform scaling or arbitrary RGB recoloring.

---

# 16. Alderford NPC presence contract

NPC schedules are presentation, not service lockouts.

**Service/quest relevance overrides ambient schedule.**

Exact R01 rule:

- if any player enters within **12 blocks** of a named NPC's primary service interaction anchor and that service is legally available, the NPC uses/returns to the authored on-duty service anchor;
- if a personal accepted quest is in a return/turn-in state for that NPC, entering the settlement/service relevance volume also forces the NPC to the authored turn-in/service anchor;
- the override persists until no relevant player remains for **20 s**, then ordinary ambient schedule may resume;
- a committed authored story/event scene can temporarily supersede service position, but the service resumes immediately after the scene reaches its stable committed state;
- service UI access never depends on guessing where an off-duty NPC wandered;
- under the 15-pathfinder core cap, activation priority is **required scene/quest/service named NPC → Alderford Watch guards → other nearby named ambient NPC → unnamed townsfolk**. Lower-priority ambient actors yield their loaded pathfinder slot first.

Thus Daren may visit The Copper Kettle, Lysa may work near herbs, Nessa may move under the covered market and Sera may patrol, but those ambient beats never turn normal service/turn-in into NPC hunting. Shops do not close because the player arrived at night.

World time bands:

```text
Morning: 06:00–10:00
Day: 10:00–18:00
Evening: 18:00–22:00
Night: 22:00–06:00
```

## 16.1 Named presence

- **Mara Venn** — gate/route activity in Morning, Wayfarers' Hall Day, Copper Kettle/Wayfarers interior Evening/Night;
- **Elian Rook** — Wayfarers' Hall all bands, with different work/sit anchors;
- **Daren Holt** — Forge Morning/Day, Forge exterior or Copper Kettle Evening, Forge interior Night;
- **Lysa Fen** — Remedies Day, river-herb/garden edge Morning, Remedies interior Evening/Night;
- **Toma Reed** — stable/paddock Morning/Day, stable interior Evening/Night;
- **Brin Hale** — Copper Kettle all bands;
- **Nessa Bell** — market stall Day, nearby stock/covered-market anchor Morning/Evening/Night;
- **Oren Quill** — Vault all bands;
- **Sera Wren** — route board Morning, Riverwood/road-edge authored field anchor Day, Copper Kettle/route board Evening, no forced Night field patrol; **while an accepted Sera contract is in a return/turn-in state, quest relevance overrides the ambient schedule and Sera is available at the route board until that personal turn-in completes**;
- **Ilyan Voss** — pre-clear Copper Kettle guest anchor; post-clear Wayfarers' Hall Day and Copper Kettle Evening;
- **Kest Arden** — event/hunt-owned only; never loops visibly around Alderford as a service NPC.

## 16.2 Unnamed population

Logical R01 settlement population additionally includes:

- 2 Alderford Watch guards;
- 4 ambient townsfolk.

At most **15 pathfinding humanoid NPCs** may be active in Alderford's loaded core at once.

If all logical actors would exceed this cap, apply the relevance priority above. Unnamed townsfolk yield first; then non-relevant off-duty named ambient actors use their authored off-core schedule state. A required service/quest NPC is never removed merely to preserve ambient population.

No vanilla villager/golem fills population gaps.

---

# 17. Required first-use service lines

These lines are player-facing canon. They may be localized, but implementation must not invent substitute exposition.

## Nessa Bell — market

> “Road's been odd, but the shelves aren't empty. If you need a replacement or want to sell what you won't use, start here.”

## Oren Quill — vault

> “Materials stay under your name. Put them away here and the town services can still see what belongs to you.”

## Brin Hale — cooking

> “Hot food is for the road, not another chore. Bring ingredients or buy a plate and get moving while it's still warm.”

## Daren Holt — forge

Existing first-use line remains authoritative:

> “If it's in your Material Pouch, I can work from it. Bring better material and you'll see better options.”

## Lysa Fen — alchemy

> “I can sell the basics. If you bring the plants and glow-organs yourself, I can make the same doses for less.”

## Toma Reed — stable before Stag unlock

> “The paddock's for trained stock. If you want one to answer your call, earn its trust first.”

## Elian Rook — class service

Existing first-selection line remains authoritative:

> “Choose the discipline you want to begin with. You can learn another path later; this is where you start.”

No mandatory “talk to every service NPC” objective is created.

---

# 18. R01 contract abandonment / reaccept

R01 one-shot optional contracts follow the global ability to abandon, but their exact behavior is closed here.

## Riverbank Remedies

On Abandon:
- active contract tracking clears;
- any quest-reserved Healing Herb quantity is released back to ordinary material ownership;
- already claimed reward/completion can never be reset.

On Reaccept before completion:
- gather counter starts at **0 / 3**;
- only valid Healing Herbs gathered after the reaccept commit count toward the renewed contract;
- older inventory herbs may still be used normally, but do not retroactively satisfy the teaching objective.

## Signs in the Meadow

On Abandon:
- active contract counter clears;
- world discovery knowledge remains.

On Reaccept:
- counter starts at **0 / 2**;
- the three authored sites become legally re-inspectable for contract credit even if their discovery text was seen before;
- each site can credit the renewed contract once.

## Steel in the Grass

On Abandon:
- active hunt contract clears;
- Steelboar/world discovery knowledge remains.

On Reaccept:
- the player must meaningfully participate in a **new qualifying Steelboar defeat after the reaccept commit**;
- an older defeat never retroactively completes the renewed contract.

## Content that does not use Abandon

- Dust on the Quarry Road — Main;
- Roots Below Stone / Lines Beneath the Land — Main;
- A Stag at the Ford — Regional discovery/unlock;
- The Crowned Trail — Discovery/Hunt record;
- Roadside Trouble — World Event.

These may be unpinned/ignored where UI permits, but they are not converted into abandon/reaccept contracts.

---

# 19. Post-Earthloong R01 aftermath

Earthloong first clear changes R01 presentation without deleting its repeatable content.

Immediately committed per eligible player:

- quarry story evidence recorded;
- first-clear reward transaction recorded/pending safely;
- post-quarry briefing becomes available;
- R02/R03 peer lead state opens through existing story rules.

Visible Alderford changes after briefing:

1. Ilyan's daytime anchor moves to Wayfarers' Hall;
2. Daren exposes Earthloong signature-craft progress;
3. Mara's primary dialogue becomes the R02/R03 route lead instead of quarry urgency;
4. Sera can comment on both outgoing route lines;
5. route board visually gains the west/forest and high-road regional leads;
6. ordinary quarry-road trade ambience resumes in a restrained form.

The region does **not** become permanently threat-free.

Still available:
- Roadside Trouble;
- gathering;
- fishing;
- housing;
- Trail Stag;
- Steelboar/Nature Spirit ecology;
- Regalhart;
- repeat quarry/Earthloong;
- forge/alchemy/cooking;
- class service.

Quarry Relay Evidence is never granted again as a duplicate inventory object.

---

# 20. R01 UI state inventory

R01 may use only the already-selected Lucifer-family visual grammar.

Player-facing R01 needs these final screen/state families:

1. first class selection;
2. quest board;
3. journal/objective detail;
4. inventory/equipment comparison;
5. Nessa market buy/sell;
6. forge recipe/result;
7. alchemy recipe/result;
8. cooking recipe/result;
9. Material Vault/storage;
10. Recovery Belt setup;
11. Trail Stag unlock/registration feedback;
12. property inspection/purchase;
13. housing furnishing mode;
14. fishing tension/catch result;
15. Fish Codex entry;
16. boss frame/status presentation;
17. Earthloong first-clear reward choice;
18. post-quarry route handoff/map update;
19. death/respawn;
20. settings/accessibility prompts used by R01.

No R01 screen may be implemented first as a black developer panel or vanilla chest/menu with a promise to reskin it later.

Exact art-piece file bindings and final pixel-perfect screenshot acceptance remain ASSET_BINDING / real-client validation.

---

# 21. R01 state additions

The following logical states are now required in addition to existing vertical-slice states:

```text
r01_dodge_hint_seen
r01_dodge_used_once

r01_roadside_event_cycle
r01_roadside_event_participation
r01_roadside_event_last_end_active_time

r01_quarry_waystone_discovered
r01_quarry_waystone_activated
r01_quarry_run_id
r01_quarry_run_state
r01_quarry_lift_open

r01_regalhart_cycle
r01_regalhart_last_defeat_active_time

field_camp_recipe_known
field_camp_permanent_unlock

r01_property_inspected[]
r01_property_owner_state

r01_profession_insight_flags

r01_merchant_epoch_active_time
r01_merchant_cycle_index
r01_merchant_cycle_sold_slots[]

r01_fishing_spot_cycle_index[]
r01_fishing_catch_ordinal[]
r01_fishing_pending_candidate[]
r01_fish_discovery_flags

r01_service_first_use_flags
r01_npc_aftermath_comment_flags

r01_contract_reaccept_generation[]
r01_pending_reward_claim_ids[]
```

All progression/economy ownership is server-authoritative.

---

# 22. Reconnect / idempotency additions

## Roadside Trouble

- credited event participation for the active event instance is not duplicated after reconnect;
- disconnect before completion grants no completion reward unless the server had already committed the completion transaction;
- the world event itself continues/reset according to controller state rather than per-client presence.

## Quarry

- Waystone discovery/activation persists personally;
- current physical run may continue while other players remain;
- first-clear reward/evidence remains personal and idempotent;
- a pending deterministic reward choice reopens exactly once.

## Regalhart

- repeat timer is world/controller state, not client time;
- reconnect does not respawn the boss;
- first-defeat reward flags remain personal.

## Camp

- permanent unlock cannot duplicate through craft/reconnect races.

## Housing

- uses the existing durable atomic property transaction contract.

---

# 23. R01 material sell values

These direct values override the generic material-percentage rule because most R01 field/signature materials have no ordinary unlimited merchant buy price.

| Material | Sell value / unit |
|---|---:|
| Iron Ore | 5 Gold |
| Hardwood | 4 Gold |
| Healing Herb | 5 Gold |
| Verdant Crystal | 24 Gold |
| Louxia Meat | 3 Gold |
| Louxia Glow | 8 Gold |
| Tough Hide | 6 Gold |
| Regalhart Antler | 45 Gold |
| Earthloong Scale | 45 Gold |

Rules:

- materials can be sold directly from Material Pouch through Nessa's Sell → Materials view;
- selling Regalhart Antler or Earthloong Scale always requires a confirmation showing current held amount and known signature-craft requirement;
- bulk-sell never includes boss signature materials;
- quest-reserved Healing Herbs for an active Riverbank Remedies completion transaction are not consumed by a bulk sale;
- there is no merchant who sells unlimited Verdant Crystal, Regalhart Antler or Earthloong Scale back to the player.

The values keep surplus gathering useful without making passive wildlife/resource loops the dominant early Gold route.

---

# 24. R01 combat Gold table

R01 combat Gold is deliberately small compared with authored objective/dungeon income. These are HARD_RULE starting values.

Gold is granted as a server-owned combat reward transaction; implementation does not need to render literal coin items inside animal bodies.

| Encounter | Gold |
|---|---:|
| Louxia | 0 |
| Meadow Viper | 2 Gold at 60% |
| Cave Centipede | 3 Gold at 70% |
| Bison | 0 |
| Grizzly | 0 |
| Steelboar | 18 Gold guaranteed |
| Nature Spirit | 20 Gold guaranteed |
| Regalhart — first eligible defeat | 70 Gold |
| Regalhart — repeat eligible defeat | 35 Gold |
| Earthloong — first eligible dungeon clear | **180 Gold total**, already owned by dungeon completion; no extra boss Gold |
| Earthloong — repeat clear | 90 Gold |

Rules:

- passive/neutral wildlife is not the best direct Gold farm;
- no ordinary R01 creature drops random potions at baseline;
- material sale value remains separate from the combat Gold transaction;
- first-clear quest/objective Gold is not duplicated by the table above;
- if playtest shows common-mob farming beating authored R01 play, reduce common combat Gold before increasing housing/consumable prices.

---

# 25. R01 economy sanity targets

These are TUNEABLE_SEED acceptance bands.

During an ordinary first 55–75 minute Earthloong run:

```text
gross Gold from authored rewards + ordinary world play:
target 500–850 Gold before major optional spending
```

A direct player who skips optional contracts/hunts should normally remain below the all-content total.

The first 2,400-Gold cottage must remain a multi-hour savings goal, not an automatic first-session purchase.

Recovery spending should remain a choice but should not consume more than roughly one third of gross early income during competent play.

Ordinary fishing by itself must not beat quest/dungeon Gold/hour.

Regalhart/Earthloong farming must remain attractive for their known drops without making ordinary R01 merchant gear irrelevant.

---

# 26. R01 hidden-choice closure table

| Question an implementer must not answer | Canonical answer |
|---|---|
| Does the first Viper always show dodge help? | Only under the exact one-time trigger in §5 |
| What is the undefined road event? | `Roadside Trouble`, §6 |
| Which substantial R01 POIs exist? | exact four in §4.1 |
| Which minor discoveries exist? | exact six in §4.2 |
| How many R01 resource nodes exist? | exact counts in §4.3 |
| Which ordinary items can each elite/boss/treasure source roll? | exact source pools in §4.4 |
| How are R01 enemy/boss attacks selected? | exact cooldown / legality / weight / anti-repeat controller in `STATUS_AND_R01_ENCOUNTERS.md` |
| How does Guardian threat actually affect AI targets? | exact server-side threat + 1.25x switch hysteresis in `COMBAT_BALANCE.md` |
| How many rotating Nessa slots? | exactly 5, one personal item each |
| When does Nessa refresh? | every 10 active-world min from personal Alderford shrine epoch |
| Can relog/UI reopen reroll Nessa? | no; deterministic world/player/merchant/cycle seed |
| Can one favorable Nessa item be bought repeatedly? | no; one purchase marks that personal slot SOLD until next cycle |
| Can a Nessa cycle contain several Superior items? | no; at most 1 |
| Can R01 merchant sell Mythics? | no |
| What grades/Item Lv does normal R01 rotation use? | exact table/controller in §10.1 |
| Are Lysa/Brin basics finite stock? | no; unlimited fixed R01 stock, Gold + legal storage are constraints |
| What does each R01 material sell for? | exact table in §23 |
| What Gold does each R01 combat actor award? | exact combat Gold table in §24 |
| How much ordinary Alderford bank storage exists? | 36 Personal Storage slots/player + 9,999/material Material Vault |
| What happens when an important reward has no backpack room? | backpack → Alderford Personal Storage → Pending Reward Claim |
| Can Pending Reward Claim act as free storage? | no; item unusable/tradeable until legally claimed |
| How do optional contracts behave after Abandon/Reaccept? | exact reset/reinspection/new-defeat semantics in §18 |
| When does the Camp recipe appear? | first owned Hardwood + Tough Hide |
| Is the Camp Kit consumable/tradeable? | permanent personal utility unlock, no |
| What exact R01 Camp footprint/rules are used? | 5×5 footprint, 0.75 height variance, 24-block shrine/service, 12-block camp spacing, §12.1 |
| Can party members ignore Camp spacing? | no in R01 |
| How does R01 furniture snap/rotate? | 0.25-block position grid + 90° rotations only, §15.5 |
| How many R01 property vacancies? | 4 Small Cottage + 1 Town House |
| Is the starter furnishing package 700 or 800 Gold? | exactly 750 Gold |
| What does the package contain? | exact seven-line package in §15.3 |
| What individual R01 furniture can be bought? | exact 12-item Household catalogue in §15.4 |
| Where do early profession Insights come from? | exact flags in §13 |
| Does R01 require weather/time fishing? | no |
| Do R01 time/weather conditions alter launch spawn weights? | no; ambience/presentation only |
| How many R01 fishing spots? | 5 ordinary + 1 uncommon + 1 rare |
| Can missed hook/relog reroll the fish candidate? | no; deterministic pending candidate in §14.4 |
| What is the R01 fish-size distribution? | exact piecewise 10% / 80% / 10% distribution in §14.5 |
| What is Trophy threshold/value? | upper 5% size band; exact interpolation/Trophy multiplier in §14.5–14.6 |
| What are Hook/tension values? | exact windows/pull/reel/progress/failure seeds in §14.7–14.8 |
| What are the four final fish species names? | **not gameplay discretion**; blocked until the four accepted external models are bound |
| How is Trail Stag summoned/dismounted? | exact candidate-position search + safe dismount controller in `MOUNTS.md` |
| What are Trail Stag steering/jump seeds? | exact R01 TUNEABLE_SEED values in `MOUNTS.md`; revise only from real play evidence |
| Is fast travel anywhere-on-map? | no; node-to-node only |
| What is R01 fast-travel cost/cooldown/channel? | free / 0 cooldown / 1.0 s channel; requires legal origin anchor within 6 blocks |
| Does fast travel drag the party? | no; personal action |
| What happens after quarry death? | §8 |
| Does the whole dungeon reset on Earthloong wipe? | no |
| How does a repeat quarry reset? | 90 s empty or explicit exterior Reset Quarry |
| How long until Regalhart can return? | 20 active-world min + empty-territory condition |
| Do shops close at night? | no |
| How many extra ambient Alderford NPCs? | 2 guards + 4 townsfolk |
| Who writes HUD/objective/board/NPC fallback text? | exact R01 wording/state authority is `R01_PLAYER_TEXT_SPEC.md`; implementation does not improvise it |
| Who decides R01 screen flow/confirm/commit behavior? | `R01_UI_PRODUCTION_SPEC.md` |
| Does Earthloong clear delete R01 activity? | no; exact aftermath in §19 |

---

# 27. Remaining gates that are not design discretion

After this content-bible pass, the remaining R01 blockers are deliberately narrow.

## ASSET_BINDING

Must still select/validate:

- exact player/NPC outfit parts;
- exact weapon/off-hand/armor files;
- exact potion/meal/prop files;
- exact Meadow Viper model conversion;
- exact dependency entity/model/animation IDs;
- exact eat/drink/revive/mount clips;
- exact VFX/SFX/BGM;
- exact service-building compositions;
- exact furniture models;
- exact four fish model/icon identities and therefore their final player-facing species names.

## SPATIAL_BINDING

Must still load the actual Azari world and record:

- exact Alderford layout coordinates;
- every R01 subregion boundary;
- exact node/ecology anchors;
- exact fishing spots;
- property shell coordinates/footprints;
- Roadside Trouble volume;
- Trail Stag event route;
- quarry entrance/Waystone/dungeon/boss coordinates;
- Regalhart clue/territory/start anchors;
- route sightlines and measured travel times.

If either gate exposes a hard conflict, update canon **before** coding the affected player-facing content.

---

# 28. R01 content-closure acceptance

The R01 **gameplay/content design** is closed. This does **not** mean source-ready or play-proven.

Use the following status split:

```text
R01 GAMEPLAY / CONTENT DESIGN CLOSED: YES
R01 IMPLEMENTATION-TIME GAMEPLAY CHOICES REMAIN: NO
R01 ASSET_BINDING COMPLETE: NO
R01 SPATIAL_BINDING COMPLETE: NO
R01 SOURCE READY: NO
R01 IMPLEMENTED: NO
R01 PLAYTESTED: NO
R01 MULTIPLAYER TESTED: NO
```

R01 becomes source-ready only when the external presentation and actual-world spatial gates below are also true:

## Content

- no unnamed required quest/event remains;
- no required objective has an undefined count;
- no merchant/service leaves stock/price behavior to implementation;
- no repeat boss/dungeon leaves reset timing to implementation;
- no first-session contextual tutorial leaves trigger behavior to implementation;
- no house/camp/fishing/profession rule leaves a gameplay choice to implementation;
- substantial/minor POI roster, R01 resource-node counts and ordinary equipment source pools are fixed.

## Presentation

- every required visible actor/item/screen has an accepted external binding;
- no player-visible placeholder is needed;
- fish names match real accepted fish models rather than speculative labels.

## Spatial

- actual Azari R01 loaded;
- route/POI/property/event coordinates recorded;
- travel times measured;
- sightlines reviewed at gameplay FOV;
- no required route depends on an impossible slope/chokepoint.

## Validation status language

Until actual implementation/testing happens:

```text
DESIGN REVIEWED: YES
R01 CONTENT BIBLE AUTHORED: YES
ASSET READY: NO
SPATIAL READY: NO
CODE REVIEWED: N/A
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
