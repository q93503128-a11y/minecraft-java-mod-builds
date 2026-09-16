# Open-World RPG — R02 Western Forest Content Bible

> Status: **DESIGN CANON — named cast, player-facing regional/main quest flow, scenes, rewards, failure/rejoin behavior and Act-I evidence handoff locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Region/system package: `R02_IMPLEMENTATION_PACKAGE.md`  
> Story: `WORLD_STORY_CANON.md`  
> Quest/state: `QUEST_WORLD_STATE.md`  
> Combat: `COMBAT_BALANCE.md`  
> Loot/equipment: `LOOT_ECONOMY.md`, `EQUIPMENT_BALANCE.md`  
> Cross-region identity: `REGION_CROSS_AUDIT.md`  
> Rule: this file closes R02 narrative/content-authoring blanks. It does not reopen R02 terrain, combat, service, dungeon or technical contracts already locked elsewhere. If it conflicts with `GAME_DESIGN.md`, the master canon wins.

This file exists so R02 is not handed to implementation as a collection of working titles and open quest ideas. An implementer must not invent the hamlet name, characters, quest objectives, scene order, rewards, rejoin behavior, regional aftermath or Act-I evidence flow while coding.

Exact external model/animation/VFX/audio filenames, hashes and exact Azari coordinates remain **pre-code gates**. They are not implementation discretion: gameplay source for the affected visible content does not begin until those gates are closed.

---

# 0. Player-facing language rule

No R02 player-facing UI, dialogue, quest, map, item, tutorial, loading text or system message may expose:

- internal IDs such as `r02_*`, `PLAYER_PROGRESS`, `ENCOUNTER_STATE`;
- `alpha`, `beta`, `prototype`, `temporary`, `placeholder`, `TODO`, `debug`, `developer`;
- asset-intake/license/hash notes;
- acceptance-test language;
- implementation or milestone terminology.

Internal state is translated into normal world/player language before display. A missing localization or unresolved model binding is a content/build failure, not a player message.

---

# 1. R02 content identity

Final settlement name: **Rillcross**.

Rillcross is a compact timber-and-river trade hamlet built where the old logging road crosses two river branches. It is visibly smaller and more specialized than Alderford. It exists because timber, fish, carts and river traffic move through it; it is not a full-service duplicate hub.

R02's content sentence is:

```text
reach Rillcross through a denser forest
→ learn that ordinary trade routes are being physically displaced
→ follow one continuous environmental disturbance rather than a clue counter
→ discover a buried route/record structure
→ enter the woodland sanctum
→ defeat the Lich that has occupied its archive
→ recover evidence that the old network stored observations and communicated with Whitecrest
```

The region must not become an undead forest. Wolves, spiders, grizzlies, Nature Spirits, fish and plants remain ordinary/local ecology. Undead presentation belongs to the authored sanctum/ruin belt.

---

# 2. Balance benchmark and pacing lock

External balance precedent is used structurally, not copied mechanically.

Useful benchmark observations:

- Guild Wars 2 awards many open-world activities as a percentage of level progress; its published wiki tables place ordinary successful events around a single-digit percentage of a level, storyline instances much higher, and dungeons much higher again.
- The Witcher 3 uses suggested quest/content levels as guidance rather than physical world gates and sharply reduces rewards for heavily over-levelled content.
- This project's global canon already uses the same broad principles: small discoveries/events, larger regional quests, major dungeon completion, no hard region gate, and anti-overlevel reward reduction.

R02 therefore uses the existing project curve rather than adding a new regional XP system.

Normal pacing target from first Rillcross arrival:

```text
direct regional/main path + first sanctum clear: ~70–100 min
main path + several optional activities: ~100–150 min
completionist first visit including guardian/fishing/contracts: ~2–3 h
```

R02 does not promise the player will reach Lv14 merely by running the mandatory path. Suggested Lv14 for the Lich is encounter guidance, not a grind requirement. A player who entered around Lv8 and explores meaningfully should usually approach the encounter around Lv10–12, with R01 leftovers/R03 overlap/optional R02 content providing additional growth.

Dynamic-event completion rewards are deliberately smaller than regional quests:

```text
small R02 event: 8% current next-Lv EXP
harder Grove Disturbance event: 10%
```

This keeps events worth joining without turning event cycling into the dominant leveling route.

---

# 3. Rillcross physical/service identity

Existing R02 service contract remains authoritative. Rillcross provides:

- shrine / fast-travel discovery;
- inn/rest/cooking;
- general regional merchant;
- tradehouse + Material Vault access;
- ranger/contract board;
- fish/food buyer;
- carpenter/property steward;
- Trail Stag hitch/convenience point;
- authored Small Cottage/Town House vacancies.

Rillcross does **not** add a second full class hall, advanced forge and advanced alchemy lab merely for convenience. Alderford remains a meaningful long-term hub.

Exact Azari coordinates wait for spatial closure, but accepted placement must make these landmarks readable from normal travel:

```text
R01 logging road entry
→ river split / timber bridge
→ Rillcross trade square
   ├─ The Split Cedar inn
   ├─ Crosswater Tradehouse
   ├─ ranger/route board
   ├─ carpenter yard / housing lane
   ├─ fish landing
   └─ shrine on raised riverbank
→ deeper logging road / root-cut trail
→ ruin belt / woodland sanctum direction
```

No compulsory service tour.

---

# 4. Named R02 cast

The following names and functions are final player-facing R02 canon. Exact face/hair/outfit/model pieces remain pre-code asset bindings only.

| Character | Role | Normal anchor | Function |
|---|---|---|---|
| **Edda Marr** | Rillcross reeve / tradehouse lead | Crosswater Tradehouse | primary local authority; regional-main quest owner; practical trade/route stakes |
| **Rowan Pike** | logging foreman / bridge worker | timber yard / damaged-route board | local work/economy voice; `The Last Survey` |
| **Cale Brinn** | innkeeper / fish buyer | The Split Cedar | rest/cooking/fish sale; `River Ledger` |
| **Pell Orin** | carpenter / property steward | carpenter yard / housing lane | furnishing and R02 property service |
| **Nell Var** | route surveyor | initially missing; later tradehouse/bridge | rescued regional NPC; visible aftermath of `The Last Survey` |
| **Sera Wren** | recurring cartographer/ranger | route board / forest approaches | continuity from R01; interprets ecology/trail without solving it for player |
| **Ilyan Voss** | recurring Anchor scholar | appears after sanctum evidence is recoverable | interprets archive/network evidence; does not know the answer in advance |
| **Kest Arden** | recurring rival wanderer | optional guardian/ruin appearances | optional world continuity; never mandatory companion/quest dispenser |
| **Edras, Last Curator** | Lich / former archive custodian | woodland sanctum inner archive | named dungeon boss whose undead state is local to the sanctum |

Target ordinary daytime population remains the R02 package target: **6–9 functional/guard NPCs + 3–5 ambient townsfolk**. Named characters use anchored/bounded behavior; the game does not run all of them as expensive free-wandering pathfinders.

---

# 5. Arrival and sequence-breaking rules

## 5.1 Normal arrival after R01

If the player completed the R01 return scene, `Western Relay` points broadly toward R02. Entering Rillcross discovers the hamlet and creates the regional/main entry **Forks Beneath the Boughs**.

Sera is available at the route board and recognizes the player from Alderford if they met her there.

Sera arrival line:

> “Alderford's road is open country. Out here, the forest hides a problem until you're standing in it. Start with the broken bridge north of the timber yard.”

Edda's first main line:

> “The road hasn't washed out. It keeps being pushed sideways. Timber crews move a marker, and the ground moves it again. Find where that pressure is coming from before we lose the river crossing.”

## 5.2 Arriving before R01 story

R02 regional content remains playable if the player physically reaches it early.

- Rillcross services work normally;
- regional quests and sanctum can be completed;
- `Western Relay Evidence` is stored personally if found;
- the evidence does **not** reveal the full continental Anchor interpretation before the player has discovered `Quarry Relay Evidence` in R01;
- after the R01 quarry evidence becomes known, the already-acquired R02 archive evidence is re-evaluated without forcing another dungeon clear.

No story wall seals the forest because the player arrived in the "wrong" order.

---

# 6. Main/regional chain — Forks Beneath the Boughs

Final title: **Forks Beneath the Boughs**.  
Quest owner: **Edda Marr**.  
Category: Main/Regional lead.  
Repeatability: once per player.  
Failure: cannot permanently fail.

This quest deliberately does **not** use R01's `any 3 distinct actions` grammar.

## Stage 1 — the broken bridge

Objective:

> “Inspect the displaced supports north of Rillcross.”

The player reaches one authored large logging/bridge anomaly. The support is bent outward from below/side pressure rather than broken by a simple impact.

Personal investigation line:

> “The timber failed after the ground shifted. Roots and stone have been forced in the same direction.”

Sera's optional contextual line if nearby/consulted:

> “Don't count marks. Follow the direction they're all pointing.”

## Stage 2 — continuous disturbance trail

The player follows a **continuous readable trail** through the forest:

- uplifted roots/soil;
- displaced stone/old road edging;
- one damaged piece of logging equipment;
- vegetation bent along the same pressure line.

These are world-navigation language, not `1/3, 2/3, 3/3` quest counters.

The HUD objective stays:

> “Follow the disturbance into the deep grove.”

The quest advances when the player enters the authored `Rootscar Clearing` destination volume after legitimately travelling from the disturbance route. Missing one individual visual trail prop never blocks progress.

## Stage 3 — route slab

At Rootscar Clearing, the player finds old fitted stone beneath newer roots. Interaction records a **Western Route Slab** journal finding.

Player-facing text:

> “The buried stone carries the same route-line language found beneath Alderford, but the surface is layered with older marks and record grooves.”

This points toward the ruin belt/old arboretum without explaining the whole network.

Completion reward:

```text
EXP: 35% current next-Lv requirement
Gold: 150
Class XP: 20% current Class Rank requirement
```

Completion unlocks:

- main continuation **The Stone That Listens**;
- broad ruin-belt search area;
- the Grovebound Warden hunt can become discoverable if the player follows the separate heavy-impact branch of the disturbance trail.

### Reconnect/death

- bridge inspection and Rootscar destination commit immediately as personal state;
- death does not reset them;
- reconnect resumes at the furthest committed stage;
- individual trail props are never consumable quest items and therefore cannot be lost/duplicated.

---

# 7. Regional human quest — The Last Survey

Final title: **The Last Survey**.  
Quest owner: **Rowan Pike**.  
Category: Regional.  
Availability: Rillcross discovered.  
Repeatability: once per player.  
Not required for main-story progression.

Rowan's offer line:

> “Nell Var went to mark a safer timber line yesterday. Her partner came back without her after the ground split between them. Find her if you can; don't drag her home through a fight.”

Sequence:

1. follow the survey stakes from the timber yard toward a side ravine;
2. discover Nell on a raised broken route/platform, alive but cut off;
3. defeat/drive off the authored threat group — solo baseline `2 Forest Wolves + 1 Forest Spider`, scaled by normal regional participant rules;
4. interact with the rope/bridge anchor to create a safe exit path;
5. speak briefly to Nell; she leaves through the safe route after the player leaves the immediate area rather than becoming a slow escort NPC.

Nell rescue line:

> “I can walk once the path is clear. Go ahead. If you babysit every surveyor in these woods, you'll never leave Rillcross.”

Reward:

```text
EXP: 25% current next-Lv requirement
Gold: 140
Class XP: 18% current Class Rank requirement
```

Visible aftermath:

- Nell appears in Rillcross near the tradehouse/bridge on later visits;
- a formerly blocked minor timber shortcut becomes a safe shared route if the world-state change passes the normal late-join rule;
- Rowan gains one new line about the route being usable again;
- no new currency/service is created.

Failure/rejoin:

- Nell cannot permanently die from ambient simulation;
- player death resets only the local threat encounter, not discovery of Nell;
- once the rescue transaction commits, disconnect cannot return Nell to the ravine or duplicate the reward.

---

# 8. Optional contract — River Ledger

Final title: **River Ledger**.  
Quest owner: **Cale Brinn**.  
Category: Contract / fishing introduction.  
Repeatability: once per player.

Cale's offer line:

> “The river changed course enough that my old catch notes are useless. Bring me one ordinary fish from a Rillcross pool and tell me where you hooked it.”

Objective:

```text
catch 1 fish from an authored R02 river/pond fishing spot
rarity allowed: Common or Uncommon
fish must be personally caught after contract acceptance
return to Cale Brinn
```

No rare/signature fish is required. The caught species' first catch simultaneously updates the Fish Codex under existing rules.

The fish is **not consumed** by turn-in; Cale only records the catch/location, so the player may sell/cook/keep it normally.

Reward:

```text
EXP: 20% current next-Lv requirement
Gold: 110
Class XP: 15% current Class Rank requirement
```

Completion line:

> “Good. One real catch tells me more than ten guesses. I'll mark the pool, not promise what's in it tomorrow.”

---

# 9. Optional contract — Under the Eaves

Final title: **Under the Eaves**.  
Quest owner: **Edda Marr** via the Rillcross board.  
Category: Contract / gathering-world-use.  
Repeatability: once per player.

Purpose: connect R02 gathering to an actual settlement need without introducing an unresolved resin item or mass-gather grind.

Objective:

```text
gather 2 Forest Mushroom from personal R02 nodes
+ gather 1 Hardwood from a personal R02 timber node
return to Crosswater Tradehouse
```

The materials are consumed on turn-in and represent food/repair stores for route crews.

Reward:

```text
EXP: 18% current next-Lv requirement
Gold: 100
Class XP: 12% current Class Rank requirement
```

No mastery rank, recipe or service is gated behind this contract.

---

# 10. Main continuation — The Stone That Listens

Final title: **The Stone That Listens**.  
Quest owner: main-story journal; Sera/Edda provide context.  
Availability: `Forks Beneath the Boughs` complete.  
Category: Main.  
Failure: cannot permanently fail.

Edda line:

> “The old foresters called those ruins an arboretum. They stopped using the name when people stopped coming back from the inner halls.”

Sera line:

> “Stay with the river until the stone arches start. The ruin is easier to find by what the forest refuses to grow over.”

Exact progression:

1. follow the main river/deep-grove route toward the ruin belt;
2. discover the first old arboretum arch landmark;
3. pass through the outer court / ruin-belt discovery volume;
4. encounter the first clearly undead/shade presentation only inside the authored ruin context;
5. interact with the outer record plinth/door mechanism;
6. exact woodland-sanctum dungeon marker and **Suggested Lv 12–14** warning become available.

Milestone reward at valid sanctum discovery:

```text
EXP: 40% current next-Lv requirement
Gold: 170
Class XP: 25% current Class Rank requirement
```

No key-item fetch is required to enter. If the selected final structure needs a door interaction, the record plinth itself supplies the meaningful opening action.

---

# 11. Grovebound Warden hunt

Final player-facing field-boss name: **Grovebound Warden**.  
Internal existing controller may retain `r02_grove_guardian`; internal ID never appears to players.  
Suggested encounter Lv: **13**.  
Main-story requirement: **optional**.

The current primary external model direction remains the distinct Quaternius guardian/golem candidate in `R02_IMPLEMENTATION_PACKAGE.md`; exact accepted model/animation is a pre-code asset gate. If that candidate fails, the planning canon is revised before implementation rather than silently changing the boss in code.

## Discovery grammar

There is **no numeric clue counter**.

Discovery starts when the player finds one large authored anomaly such as the damaged logging bridge/heavy equipment site or independently enters the Warden's wider territory.

A second, heavier branch of environmental disturbance leads away from the main route:

```text
large impact on timber/stone
→ unusually deep displaced roots/soil
→ cracked standing stones / dragged earth
→ quieter circular clearing with old growth pushed outward
→ Warden territory
```

HUD language:

> “Follow the heavier disturbance into the old grove.”

Finding the Warden first is valid and immediately creates the hunt entry.

## Kest Arden appearance

Kest may appear once near the outer Warden trail if the player has not already met him in R01.

Line:

> “The smaller tracks turn away from this clearing. Sensible creatures. I was considering copying them.”

No progression depends on Kest.

## First-defeat reward

Use existing field-boss combat/reward rules plus these locked identities:

```text
signature material: Heartwood Core
first eligible defeat: Heartwood Core x2
repeat eligible defeat: Heartwood Core x1
direct Mythic roll: 15%
EXP: 20% current next-Lv requirement
Class XP: 15% current Class Rank requirement
```

Mythic pool:

### Rootwake Maul

- Mythic Item Lv13;
- family: hammer/mace;
- authored affixes: STR / Physical Power / Poise-Stagger Resistance-equivalent offensive stagger identity using the canonical valid stat vocabulary at asset/final data binding;
- numeric authored affixes use the global Mythic deterministic/direct-drop percentile rules;
- unique: **after the wielder causes an enemy poise break, the next basic or weapon-skill hit within 4 s creates one 2.5-block ground shockwave for 45% normalized WeaponPower physical damage; 8 s internal cooldown**;
- shockwave VFX must match the real radius.

### Oldgrowth Ward

- Mythic Item Lv13;
- family: relic/charm;
- authored affixes: WIL / Max HP / negative-status duration reduction;
- unique: **when a negative status on the wearer is cleansed or naturally expires, gain 8% incoming-damage reduction for 4 s; 12 s internal cooldown**;
- does not trigger repeatedly from every tick of one status.

Exact 3D/icon binding for Heartwood Core and both Mythics is mandatory before player-visible implementation.

---

# 12. Woodland Sanctum dungeon — story layer

The room/combat architecture in `R02_IMPLEMENTATION_PACKAGE.md` remains canonical:

```text
forest exterior
→ collapsed arboretum court
→ root-breached archive
→ broken cloister / river channel
→ sepulchral gallery
→ inner sanctum
```

First-clear target: **18–24 minutes**.

The narrative escalation is:

1. outer arboretum proves this was once an ordered observation/growing space, not a tomb;
2. root archive contains record shelves/stone memory surfaces mixed with living overgrowth;
3. broken cloister shows the site once managed water/route observation;
4. sepulchral gallery reveals that Edras and his shades are a later occupation/transformation of the archive;
5. inner sanctum contains the relay record needed for Act I.

Do not add generic exposition notes to every room. Most history is shown through physical structure and one or two short interactable records.

---

# 13. Edras, Last Curator

Final player-facing boss name: **Edras, Last Curator**.  
Creature/runtime base: the R02 Lich dependency direction already defined in `R02_IMPLEMENTATION_PACKAGE.md`.  
Suggested Lv: **14**.  
Combat stats/attacks/reinforcement thresholds: unchanged from the R02 implementation package.

Story identity:

- Edras was an archive custodian/research role tied to the local observation records, not the ruler of R02;
- his current undead state is confined to the sanctum history and does not explain the whole forest ecology;
- he is not a speaking exposition machine during combat;
- one pre-fight line/echo is enough to imply obsessive preservation.

Pre-fight echo:

> “Records remain. Roads vanish. Names vanish. The record remains.”

No dialogue choice pauses combat.

## Signature material

Final material name: **Curator Seal**.

```text
first eligible clear: Curator Seal x2
repeat eligible clear: Curator Seal x1
```

## Mythic pool

### Edras' Archive Staff

- Mythic Item Lv14;
- family: staff;
- authored affixes: INT / Magic Power / Mana Recovery;
- unique: **every fifth damaging skill hit on the same target within 8 s marks the target with `Recorded`; the next damaging skill consumes Recorded to deal 50% normalized WeaponPower bonus magic damage; one target cannot trigger this more than once every 8 s**;
- `Recorded` is a boss-item combat marker, not a new global status/stacking subsystem and has a compact existing-style icon/VFX.

### Curator's Seal

- Mythic Item Lv14;
- family: relic;
- authored affixes: WIL / Max Mana / negative-status duration reduction;
- unique: **when the wearer successfully applies a project-valid control/status to an enemy, restore 3% MaxMana; 4 s internal cooldown**;
- cannot trigger from environmental/non-owned status ticks.

Exact model/icon/VFX bindings remain mandatory pre-code asset gates.

---

# 14. First-clear curated choice — exact identities

R02 dungeon first clear offers one **Superior Item Lv14** choice. All three use deterministic **85th-percentile** values of their canonical affix ranges, matching the reliable R01 first-dungeon reward philosophy.

## 14.1 Briarstring Bow

- family: bow;
- fixed affixes: `DEX`, `Critical Chance`, `weak-point damage`;
- role: precision/finesse reward without Hunter lock;
- external model: accepted grounded forest/fantasy bow from the project's existing external weapon families;
- not Mythic; no unique effect.

## 14.2 Mossweave Mantle

- slot/family: Light Chest;
- fixed affixes: `WIL`, `Max Mana`, `negative-status duration reduction`;
- role: caster/support/status-handling option;
- external model: accepted R02-compatible cloth/ranger-scholar piece from the established humanoid outfit family;
- not Mythic; no unique effect.

## 14.3 Wayroot Charm

- slot/family: Charm;
- fixed affixes: `DEX`, `Movement Speed`, `Stamina Recovery`;
- role: exploration/melee/ranged utility that remains useful outside one class;
- external icon/model: accepted external charm/jewelry direction within the existing equipment visual language;
- not Mythic; no unique effect.

All three use real accepted previews in the Lucifer-family reward panel. No implementation-time renaming or affix selection is allowed.

---

# 15. Dungeon first-clear reward transaction

After Edras defeat, each eligible player receives the existing R02 dungeon package:

```text
guaranteed Superior+ normal boss equipment roll
Curator Seal x2
15% direct Mythic roll from Edras' Archive Staff / Curator's Seal
choose 1: Briarstring Bow / Mossweave Mantle / Wayroot Charm
completion EXP: 50% current next-Lv requirement + boss contribution
completion Class XP: 32% current Class Rank requirement + boss contribution
Gold: 260
```

Repeat clear follows existing global/R02 repeat rules and does not duplicate deterministic first-clear choice.

Reward transaction is server-authoritative and idempotent. Disconnect after boss death cannot lose the unclaimed choice or create a second copy of already committed deterministic rewards.

---

# 16. Western Relay evidence

After Edras first clear, the inner archive becomes safely interactable.

The player records **Western Relay Record** as personal journal/key-state, not an inventory item/currency.

Player-facing investigation text:

> “The archive did more than store power readings. It compared river levels, forest movement and route conditions with reports arriving from a high mountain station. One destination repeats beside the same route symbol: Whitecrest.”

This is the canonical R02 Act-I evidence package.

If the player has already discovered R01 Quarry Relay Evidence, this becomes `Western Relay Evidence` immediately.

If R01 has not yet been completed, the record is stored without explaining the continental network; it upgrades automatically once the R01 prerequisite is known.

---

# 17. Rillcross return scene / Act-I handoff

After first sanctum clear and valid Western Relay Evidence, a short return interaction becomes available in Crosswater Tradehouse.

Required characters:

- Edda Marr;
- Sera Wren;
- Ilyan Voss, if the player has R01 Quarry Relay Evidence.

The scene is line-by-line skippable. Skipping commits the same journal/progression state.

**Edda Marr**
> “So the ruin under our road wasn't watching only Rillcross.”

**Sera Wren**
> “It watched the river, the forest and every route people cut through them. That's a lot of attention for a dead machine.”

**Ilyan Voss**
> “And it traded those observations with Whitecrest. The quarry mark in Alderford was a route. This archive proves the route carried records as well as power.”

**Edda Marr**
> “Then write down one thing clearly: whatever the old network was for, people here learned to live after it stopped answering.”

**Ilyan Voss**
> “That's evidence too.”

Progress effects:

- `Western Relay Evidence` committed;
- if the player has not completed the R03 major evidence path, `Whitecrest Station` remains a live peer lead rather than a mandatory immediate order;
- if R03 qualifying evidence is already known, Act-I completion logic may resolve immediately under `WORLD_STORY_CANON.md`;
- no quest requires the player to exhaust every Rillcross side activity before leaving.

If the player sequence-broke R02 before R01, this full scene becomes available only after R01 Quarry Relay Evidence is known; the dungeon is not repeated.

---

# 18. Regional aftermath

R02 remembers completion without heavy per-player geometry phasing.

After `The Last Survey`:

- Nell Var is present in Rillcross;
- the minor timber shortcut is usable if the shared route change is active;
- related ambient dialogue changes.

After Edras first clear / Western Relay Evidence:

- ruin-belt ordinary undead/shade ambient pressure outside the replayable dungeon is reduced/removed;
- Nature Spirit/forest wildlife ecology remains unchanged;
- Edda/Ilyan/Sera gain aftermath lines;
- the sanctum remains replayable under encounter reset rules;
- one Rillcross merchant/inn rotation may add an authored region recipe/decor item, but no new currency/service tree appears.

The forest does not suddenly become bright, purified or empty. The local story was about routes, records and one occupied archive, not cleansing an entire biome.

---

# 19. Dynamic event families — exact first-pass rewards

Events are discovered shared content; no board acceptance is required.

## 19.1 Fallen Timber Route

- clear a compact wildlife/route hazard and interact with the obstruction;
- completion reward per eligible participant:

```text
EXP: 8% current next-Lv requirement
Gold: 40
Class XP: 5% current Class Rank requirement
```

## 19.2 River Cargo Trouble

- secure/recover cargo around a ford/dock with combat + interaction credit;
- reward:

```text
EXP: 8%
Gold: 45
Class XP: 5%
```

## 19.3 Grove Disturbance

- short Nature Spirit/forest-threat event, distinct from Grovebound Warden;
- reward:

```text
EXP: 10%
Gold: 55
Class XP: 6%
```

Event cooldowns are authored per controller in the **10–16 active-minute** band after completion/failure; exact controller value is data, not real-world daily reset. Event cycling must not outpace meaningful regional quests/dungeons as the best progression route.

---

# 20. Quest/rejoin state contract

Required personal state includes at minimum:

```text
r02_rillcross_discovered
r02_forks_stage
r02_bridge_inspected
r02_rootscar_reached
r02_route_slab_recorded
r02_last_survey_state
r02_nell_rescued
r02_river_ledger_state
r02_under_eaves_state
r02_sanctum_discovered
r02_sanctum_first_clear
r02_first_clear_choice_claimed
r02_western_route_recorded
r02_western_relay_evidence
r02_return_scene_seen
r02_grovebound_warden_discovered
r02_grovebound_warden_first_clear
```

These identifiers are internal only.

Rules:

- quest advancement, boss clear, rewards and evidence are server-authoritative;
- party leader/host cannot overwrite another player's stage;
- helpers receive normal eligible encounter rewards but no one-time quest/story reward unless their own state qualifies;
- shared physical props retain personal logical interaction when another player has already used them;
- reconnect resumes from durable committed state, not from client screen state;
- no reward grants from dialogue UI alone before server transaction commit.

---

# 21. Multiplayer scene/quest behavior

- two players may be on different R02 stages;
- the disturbance trail can be traversed together, but each player's Rootscar/route-slab discovery commits personally;
- `The Last Survey` physical encounter can be shared; Nell rescue reward/progress commits separately for eligible players with the quest active;
- Edras encounter scales by existing participant HP/poise rules; outgoing damage does not increase merely because players joined;
- first-clear choice and evidence are personal/idempotent;
- a player who already cleared may help another without receiving another deterministic reward;
- return-scene skip/advance is personal; one player cannot choose another player's dialogue/progression state.

---

# 22. Pre-code gates remaining for R02

R02 **content authoring is closed by this file**, but gameplay source for player-visible R02 content must not begin until the applicable gates below are closed in planning/provenance docs:

1. exact accepted Grovebound Warden model/animations and Heartwood Core/Mythic visuals;
2. exact Lich/Edras runtime-model/animation verification and Curator Seal/Mythic visuals;
3. exact Forest Mushroom/resource node model binding;
4. resin/mana-flora roles in the older package either receive accepted models + final player-facing names or are removed from launch R02 before implementation;
5. exact 4–5 fish roster/models/names/value tables;
6. exact Rillcross building compositions, housing shells and NPC outfit bindings;
7. exact sanctum architecture/prop/VFX/audio bindings;
8. exact three first-clear item visual bindings;
9. actual Azari coordinates, sightlines, travel times, encounter volumes and route density;
10. actual audio/VFX acceptance in Minecraft scale.

These are planning/intake tasks. None may be delegated to Java implementation as `pick something that works`.

---

# 23. R02 content-closure acceptance

R02 may be marked **CONTENT DESIGN CLOSED** when all of the following statements are true:

- Rillcross name/role/service identity is fixed;
- named cast and recurring-character participation are fixed;
- the main chain has exact objectives, rewards, failure and reconnect behavior;
- optional human/fishing/gathering content is exact enough to data-author without inventing mechanics;
- Grovebound Warden discovery no longer uses a copied R01 numeric clue grammar;
- field-boss/dungeon-boss reward identities are fixed;
- first-clear curated reward identities/affixes are fixed;
- Western Relay evidence and Act-I handoff are fixed;
- regional aftermath is fixed and late-join safe;
- multiplayer ownership/reward semantics are fixed;
- player-facing development language is forbidden;
- remaining unknowns are explicit **pre-code asset/spatial gates**, not implementation choices.

Current verification state after this pass:

```text
CONTENT DESIGN REVIEWED: YES
R02 QUEST/NPC/SCENE AUTHORING AT R01 CLOSURE LEVEL: YES
BALANCE CHECKED AGAINST PROJECT GLOBAL CURVES + EXTERNAL OPEN-WORLD RPG PRECEDENT: YES
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
