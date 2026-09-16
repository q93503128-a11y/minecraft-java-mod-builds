# Open-World RPG — R03 Whitecrest Highlands Content Bible

> Status: **DESIGN CANON — named cast, route-restoration quest flow, exact rewards, regional scenes, Act-I evidence and rejoin behavior locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Region/system package: `R03_IMPLEMENTATION_PACKAGE.md`  
> Story: `WORLD_STORY_CANON.md`  
> Quest/state: `QUEST_WORLD_STATE.md`  
> Cross-region identity: `REGION_CROSS_AUDIT.md`  
> Loot/equipment: `LOOT_ECONOMY.md`, `EQUIPMENT_BALANCE.md`  
> Rule: this file closes R03 narrative/content-authoring blanks. Existing R03 traversal/combat/dungeon contracts remain authoritative unless explicitly refined here. `GAME_DESIGN.md` wins any conflict.

R03 must not become "R02 but on a mountain." Its authored play is about understanding vertical space, reopening useful infrastructure and discovering that ancient machinery exchanges information beyond the region.

Exact external models/animations/VFX/audio and Azari coordinates remain pre-code gates, never implementation discretion.

---

# 0. Player-facing language rule

R03 follows the project-wide rule: no player-facing `alpha`, `prototype`, `TODO`, `debug`, internal state IDs, asset/license notes, test terminology or implementation wording.

Internal route flags such as `r03_bridge_open=true` are shown only as physical world state and normal quest language.

---

# 1. Settlement and regional identity

Final settlement name: **Cairnwatch**.

Cairnwatch is a fortified mining town on a broad lower mountain shelf. It survives by mining exposed veins, maintaining lift/bridge routes and controlling safe passage between the lower valleys and high ridges.

Content sentence:

```text
arrive at an isolated mining town
→ reopen one useful route by physically conquering the mountain
→ choose which second connection matters next
→ old machinery responds during restoration
→ trace that response to an ancient observatory/forge below the mountain
→ defeat its stone guardian
→ recover the first physical map proving the sites form one continental network
```

The optional Griffin remains natural apex wildlife and is never rewritten as an Anchor creation.

---

# 2. Named Cairnwatch cast

| Character | Role | Normal anchor | Function |
|---|---|---|---|
| **Bram Sorrell** | mine foreman / trade lead | mine office / ore yard | primary local quest owner; worker/supply perspective |
| **Talia Vey** | ridge warden / route keeper | route tower / upper gate | mountain navigation, wildlife, conventional-route perspective |
| **Mira Kelm** | lift engineer | lower liftworks | route restoration interactions and machinery operation |
| **Ors Fenwick** | smith / ore processor | Cairnwatch forge | Refined Pick upgrade, Silver/gear service |
| **Jessa Moor** | innkeeper / cook | The Lantern Shelf inn | rest/food/basic fish buyer |
| **Renn Calder** | regional merchant / vault clerk | trade arcade | merchant + Material Vault access |
| **Daren Holt** | recurring engineer/smith | arrives during old-machinery investigation | interprets ancient mechanisms without replacing Mira/local expertise |
| **Ilyan Voss** | recurring Anchor scholar | arrives during `Signals in the Stone` | recognizes network geometry only after evidence exists |
| **Kest Arden** | rival wanderer | optional ridge/Griffin scenes | optional continuity, never mandatory companion |

Target population remains the existing R03 package target: 8–12 functional/guard NPCs + 4–7 ambient residents/travelers.

---

# 3. Exact Refined Pick upgrade

The previous R03 package's tuning range is closed here.

At Ors Fenwick's forge:

```text
7 Iron Ore
2 Hardwood
300 Gold
→ Refined Pick
```

Rules:

- one-time permanent Tool Pouch upgrade;
- no Silver required, preventing circular progression;
- unlocks dense R03 Silver nodes;
- keeps canonical 85% gathering-time multiplier;
- recipe is visible as soon as Cairnwatch is discovered and materials/Gold are known; no separate fetch quest gates it.

---

# 4. Main regional chain — The Closed Pass

Final title: **The Closed Pass**.  
Quest owner: **Bram Sorrell**.  
Category: Main/Regional.  
Repeatability: once per player.  
Failure: cannot permanently fail.

Bram's opening line:

> “Cairnwatch isn't short on ore. We're short on ways to move it. The mid-mine lift is dead and the skybridge is unsafe. Open either one and you'll give us room to breathe.”

The first meaningful choice is route order, not a morality branch.

Player chooses one first objective:

### Route A — Mid-Mine Lift

1. enter the side mine below the lift tower;
2. cross the broken counterweight gallery;
3. clear the authored cave-threat encounter protecting/jamming the brake room;
4. release the safety brake and realign the counterweight;
5. activate the lift from the local control.

No material payment is required. The challenge is reaching and securing the mechanism.

### Route B — Broken Skybridge

1. reach the gorge from the lower switchback;
2. descend beneath the broken span via the safe side trail;
3. clear the rockfall/territorial-threat pocket below the anchor;
4. reset the bridge anchor/winch from the far support;
5. return across the restored span.

Again, no arbitrary `bring 12 logs` tax. The route itself is the content.

First-route completion reward:

```text
EXP: 30% current next-Lv requirement
Gold: 170
Class XP: 18% current Class Rank requirement
```

The repaired connection becomes shared `WORLD_PERSISTENT` infrastructure after an atomic server transaction. Personal quest credit remains per-player, so late joiners see the repaired route but can still receive context/progression through the retained interaction/journal state.

---

# 5. Main continuation — Three Heights

Final title: **Three Heights**.  
Quest owner: regional main journal; Bram/Talia/Mira provide context.  
Availability: one major route reopened.

Cairnwatch has three canonical major connections:

1. lower town → mid-mine lift;
2. Broken Skybridge;
3. upper ridge → observatory-approach lift/route.

The player must restore **two total connections** to complete `Three Heights`. The first already counts.

This is not a generic three-token checklist: each connection is a world route with different navigation/encounter language and remains permanently useful afterwards.

## Upper-ridge connection

The upper route becomes reachable after either first route is open.

Sequence:

1. reach Windwatch Eyrie by switchback/mine connection;
2. descend to the abandoned upper liftworks;
3. clear one compact highland threat/rockfall encounter;
4. reset the seized lift using Mira's visible machinery interaction;
5. ride it once to validate the route.

Completion after the second total restored connection:

```text
EXP: 35% current next-Lv requirement
Gold: 220
Class XP: 22% current Class Rank requirement
```

Restoring the third connection later awards a one-time route-completion bonus:

```text
EXP: 10% current next-Lv requirement
Gold: 80
Class XP: 6% current Class Rank requirement
```

No separate quest currency or achievement token is introduced.

---

# 6. Regional side story — The Long Way Holds

Final title: **The Long Way Holds**.  
Quest owner: **Talia Vey**.  
Category: Regional.  
Availability: Cairnwatch discovered.  
Not required for main progression.

Purpose: establish the local-autonomy/conventional-infrastructure perspective without making it a dialogue lecture.

Talia's offer line:

> “Lifts fail. Bridges fail. A good footpath still matters when every machine goes quiet. There's an old switchback above the quarry that needs clearing, not awakening.”

Sequence:

1. reach the old switchback trail;
2. clear two physically blocked points by local interaction after dealing with nearby threats;
3. discover the weather shelter/cairn near the upper end;
4. activate the route marker so the trail becomes a readable permanent alternate path.

No escort and no timed race.

Reward:

```text
EXP: 25% current next-Lv requirement
Gold: 180
Class XP: 18% current Class Rank requirement
```

Visible aftermath:

- route remains usable as a non-mechanical alternative;
- Talia's later dialogue explicitly contrasts dependable simple paths with ancient machinery without declaring one philosophy universally correct.

Completion line:

> “Not every answer needs a gear train. Remember that when the old machines start looking clever.”

---

# 7. Mining contract — Silver Cut

Final title: **Silver Cut**.  
Quest owner: **Ors Fenwick**.  
Category: Contract / tool progression.  
Repeatability: once per player.

Availability: Cairnwatch discovered.

Ors' line:

> “The bright vein above the east cut is Silver. A Field Pick will only scar it. Upgrade your pick, bring me two clean pieces, and I'll show you what this mountain is actually worth.”

Objective:

```text
own Refined Pick
gather 2 Silver Ore personally from authored R03 Silver nodes
return to Ors Fenwick
```

Silver is consumed on turn-in for demonstration/workshop use.

Reward:

```text
EXP: 20% current next-Lv requirement
Gold: 140
Class XP: 12% current Class Rank requirement
+ one Refined-grade R03-valid accessory/utility equipment roll at Item Lv 12–14
```

The equipment roll uses existing canonical loot data; no new item grade/system is created.

---

# 8. Signals in the Stone

Final title: **Signals in the Stone**.  
Category: Main.  
Availability: `Three Heights` complete.

Trigger:

When the second major route is restored, the reactivated machinery produces a brief, visible response that does **not** match the local control input. An old indicator/instrument points toward a buried high-mountain facility.

Mira Kelm:

> “That wasn't my lift answering. Something uphill sent a return signal.”

Daren Holt, after inspecting the mechanism:

> “The counterweight is ours. That index ring isn't. Alderford's quarry had the same workmanship.”

Ilyan Voss, only if R01 quarry evidence is already known:

> “Then this is the other end of a route. We need a vantage point before we decide what it's connected to.”

## Progression

1. inspect the responding lift/indicator after the second route restoration;
2. travel to **Windwatch Eyrie**;
3. use the observatory sightline/old alignment marks there to identify the buried facility approach across the next height band;
4. reach the Abandoned Liftworks / observatory-approach route;
5. discover the collapsed mine entrance that descends toward the ancient forge/observatory.

This is a perspective/terrain puzzle, not a clue counter. The player physically reads the mountain from a high viewpoint.

Milestone reward on valid dungeon-entrance discovery:

```text
EXP: 40% current next-Lv requirement
Gold: 220
Class XP: 25% current Class Rank requirement
```

Suggested dungeon warning: **Lv 15–18**. No hard gate.

---

# 9. Whitecrest Griffin hunt

Final player-facing field-boss name: **Whitecrest Griffin**.  
Suggested Lv: **17**.  
Main-story requirement: optional.

The current VitSh animated Griffin remains the primary asset direction; exact artifact/animation/scale acceptance is a pre-code gate. If it fails, planning is revised before code rather than silently replacing it.

## Discovery grammar

No `2 of 3 clues` pattern.

1. from Windwatch Eyrie, the player can witness a distant Griffin silhouette/circling pass when the authored encounter is available;
2. the journal adds a broad high-plateau search band, not an exact boss pin;
3. along the ascent, one clear landing scar/feather site confirms the route;
4. wind direction, feathers and visible plateau geography guide the last approach.

Finding the Griffin first is valid and immediately records discovery.

Kest Arden optional line near the Eyrie/plateau:

> “Good view. Bad place to look small. Something up there has been measuring us with its eyes.”

## Signature material

Final material name: **Griffin Pinion**.

```text
first eligible defeat: Griffin Pinion x2
repeat: Griffin Pinion x1
direct Mythic roll: 15%
EXP: 20% current next-Lv requirement
Class XP: 15% current Class Rank requirement
```

## Mythic pool

### Skycleaver Spear

- Mythic Item Lv17;
- family: spear/polearm;
- authored affixes: DEX / Physical Power / Attack Speed;
- unique: **a basic or weapon-skill hit that lands within 1.25 s after the wielder completes a dodge deals +35% poise damage; 6 s internal cooldown**;
- no bonus if the dodge was cancelled/interrupted before completion.

### Galecrest Charm

- Mythic Item Lv17;
- family: Charm;
- authored affixes: DEX / Movement Speed / Stamina Recovery;
- unique: **after dealing valid weak-point damage, the next dodge used within 6 s costs 30% less Stamina; 10 s internal cooldown**;
- if the final Griffin model cannot support an honest weak point, this unique is revised in canon during asset intake before implementation.

Numeric affixes follow global Mythic percentile rules.

---

# 10. Dungeon — The Observatory Below

Final main/dungeon quest title: **The Observatory Below**.  
Target first clear: **20–30 minutes**.

Existing R03 dungeon architecture remains canonical:

```text
active mine failure zone
→ exterior cliff connection
→ ancient forge
→ observatory archive
→ stone guardian chamber
```

Narrative purpose per stage:

- **mine failure zone:** current Cairnwatch problem and living infrastructure;
- **exterior cliff connection:** show scale and physically distinguish R03 from underground-only dungeons;
- **ancient forge:** reveal machinery too old/precise to belong to Cairnwatch;
- **observatory archive:** show multiple mapped route lines and distant sites;
- **guardian chamber:** protect the core chart/instrument without becoming another Earthloong-style elemental beast.

The central lift shortcut remains a first-clear traversal reward and repeat-run convenience.

---

# 11. Dungeon boss — Forgewake Colossus

Final player-facing boss name: **Forgewake Colossus**.  
Runtime/model direction: the Rock Golem candidate in `R03_IMPLEMENTATION_PACKAGE.md`, pending conversion/license/animation gate.  
Suggested Lv: **18**.

Combat numbers/attacks remain the existing R03 Rock Golem contract.

The phase-2 fracture must be visible on the final accepted asset; if the imported model cannot visibly support it, the phase presentation is revised in planning before implementation.

## Signature material

Final material name: **Resonant Core**.

```text
first eligible clear: Resonant Core x2
repeat: Resonant Core x1
direct Mythic roll: 15%
```

## Mythic pool

### Faultline Hammer

- Mythic Item Lv18;
- family: hammer/mace;
- authored affixes: STR / Physical Power / hammer-family power;
- unique: **the first heavy/committed weapon hit after 8 s without triggering this effect creates a straight 5-block fissure dealing 60% normalized WeaponPower physical damage to enemies in the visible line; 8 s internal cooldown**;
- visible fissure = server hit lane.

### Observatory Bulwark

- Mythic Item Lv18;
- family: standard/heavy shield;
- authored affixes: END / Guard Strength / Poise-Stagger Resistance;
- unique: **a successful perfect guard against an attack tagged heavy grants +20% poise damage dealt for 5 s; 8 s internal cooldown**;
- does not trigger from trivial environmental contacts.

Exact models/icons/VFX are mandatory pre-code asset bindings.

---

# 12. First-clear curated choice

After first Forgewake Colossus clear, choose one **Superior Item Lv18** item. All three fixed affixes use deterministic **85th-percentile** canonical values.

## 12.1 Whitecrest Maul

- family: hammer/mace;
- fixed affixes: `STR`, `Physical Power`, `hammer-family power`;
- role: heavy commitment/poise reward;
- no unique effect.

## 12.2 Cliffguard Shield

- family: standard shield;
- fixed affixes: `END`, `Guard Strength`, `Poise/Stagger Resistance`;
- role: defensive/guard reward;
- no unique effect.

## 12.3 Windpiercer Bow

- family: bow;
- fixed affixes: `DEX`, `Critical Chance`, `weak-point damage`;
- role: ranged precision/elevation reward;
- no unique effect.

Exact external model/icon selection is a pre-code gate, not an implementation decision.

---

# 13. Dungeon first-clear transaction

Each eligible player receives:

```text
guaranteed Superior+ normal R03 equipment roll
Resonant Core x2
15% direct Mythic roll from Faultline Hammer / Observatory Bulwark
choose 1: Whitecrest Maul / Cliffguard Shield / Windpiercer Bow
completion EXP: 50% current next-Lv requirement + boss contribution
completion Class XP: 32% current Class Rank requirement + boss contribution
Gold: 340
```

Repeat follows global dungeon rules and does not duplicate deterministic rewards.

Reward state is server-authoritative/idempotent across death, disconnect and UI reopen.

---

# 14. Whitecrest Network Chart — Act-I evidence

After the guardian falls, the observatory instrument/chart becomes safely interactable.

Personal journal/key-state: **Whitecrest Network Chart**.

Player-facing investigation text:

> “The observatory maps Alderford's buried route and the western forest site as parts of a wider lattice. Lines continue north, east and toward the inland sea. These places were not isolated facilities.”

This is R03's qualifying Act-I evidence.

If R01 Quarry Relay Evidence is known, it immediately becomes `Whitecrest Network Evidence`.

If R01 was sequence-broken, the chart remains stored and is reinterpreted when the R01 prerequisite later exists.

---

# 15. Cairnwatch return scene / Act-I handoff

After first dungeon clear, a short scene becomes available at the mine office/route overlook.

Required participants:

- Bram Sorrell;
- Talia Vey;
- Mira Kelm;
- Daren Holt;
- Ilyan Voss if R01 evidence is known.

**Bram Sorrell**
> “We opened two roads and found a map underneath both of them. That's not the kind of efficiency I asked for.”

**Mira Kelm**
> “The lifts don't need the old network to move now. That's important. We know what belongs to us.”

**Ilyan Voss**
> “And we know what didn't. The chart links sites across the continent. The old builders treated distance like one system.”

**Talia Vey**
> “A map tells you what they connected. It doesn't tell you whether they should have.”

**Daren Holt**
> “Then we keep the routes we've repaired and learn the rest before we touch another ancient control.”

Progress effects:

- `Whitecrest Network Evidence` committed;
- if R02 qualifying Western Relay Evidence is already known, Act-I completion can resolve immediately;
- if R02 is not complete, its western lead remains available but not forced as an immediate order;
- later regional-evidence progression unlocks according to `WORLD_STORY_CANON.md`.

The player never has to finish Griffin, Silver Cut or every third shortcut to advance the main story.

---

# 16. Regional aftermath

Shared late-join-safe changes:

- repaired lifts/bridge remain active;
- the optional conventional switchback from `The Long Way Holds` remains usable;
- one upper trade/travel path gains ambient worker/traveler presence at bounded density;
- smith/merchant stock may include authored Silver-access options already allowed by economy canon.

Personal state:

- evidence/journal completion;
- boss/dungeon first-clear rewards;
- Griffin discovery/defeat;
- route quest completion and NPC aftermath dialogue.

Cairnwatch becomes easier to traverse but the mountain remains dangerous and explorable.

---

# 17. Multiplayer / rejoin

Required personal state includes at minimum:

```text
r03_cairnwatch_discovered
r03_closed_pass_first_route
r03_mid_mine_lift_personal_credit
r03_skybridge_personal_credit
r03_upper_lift_personal_credit
r03_three_heights_complete
r03_long_way_holds_complete
r03_silver_cut_state
r03_signals_stage
r03_dungeon_discovered
r03_dungeon_first_clear
r03_first_clear_choice_claimed
r03_whitecrest_chart_recorded
r03_whitecrest_network_evidence
r03_return_scene_seen
r03_griffin_discovered
r03_griffin_first_clear
```

Shared route state is separately world-authoritative.

Rules:

- if another player repairs a route first, an incomplete player uses a retained route-control/context interaction to receive personal quest credit without breaking the shared route;
- route transactions are idempotent and cannot duplicate material/Gold because route repairs have no personal material fee;
- death during lift travel respawns under normal death/checkpoint rules and does not revert the lift;
- two players on different quest stages can share Griffin/dungeon encounters;
- first-clear rewards/evidence remain personal;
- return-scene progression cannot be advanced for another player by the host/party leader.

---

# 18. Remaining R03 pre-code gates

R03 **content authoring is closed**, but affected source implementation must wait for:

1. exact accepted Whitecrest Griffin artifact, attribution, hash, animations, weak-point viability;
2. exact Forgewake Colossus/Rock Golem artifact, license path, conversion, fracture presentation;
3. one accepted distinct cave-threat model/species + combat kit, or explicit redesign/removal before code;
4. exact highland crystal model/name/use or explicit deletion of that role before code;
5. exact 2–4 fish roster/models/names/values;
6. exact Cairnwatch architecture, lifts, bridge, machinery and NPC outfits;
7. observatory/forge machinery source language and VFX/audio;
8. exact model/icon bindings for all R03 named rewards;
9. actual Azari coordinates/height bands/travel times/sightlines/route volumes;
10. real Minecraft traversal/camera performance acceptance.

No Java implementer is authorized to choose these ad hoc.

---

# 19. Current closure status

```text
CONTENT DESIGN REVIEWED: YES
R03 QUEST/NPC/SCENE AUTHORING AT R01/R02 CLOSURE LEVEL: YES
REFINED PICK COST LOCKED: YES
FIELD BOSS PLAYER-FACING IDENTITY/REWARDS LOCKED: YES, subject to pre-code asset acceptance
DUNGEON BOSS PLAYER-FACING IDENTITY/REWARDS LOCKED: YES, subject to pre-code asset acceptance
ACT-I EVIDENCE/HANDOFF LOCKED: YES
EXACT ASSET INTAKE CLOSED: NO
AZARI SPATIAL CLOSURE: NO
CODE REVIEWED: NOT APPLICABLE
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

No build/CI is warranted for this docs-only content-authoring pass.
