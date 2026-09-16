# Open-World RPG — R08 Bloomveil Content Bible

> Status: **DESIGN CANON — settlement, named cast, regional/main quests, restoration-test state, Titan Rabbit invocation, rewards, scenes, Act-III evidence and reconnect behavior locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Region/system package: `R08_IMPLEMENTATION_PACKAGE.md`  
> Story: `WORLD_STORY_CANON.md`  
> Quest/state: `QUEST_WORLD_STATE.md`  
> Cross-region identity: `REGION_CROSS_AUDIT.md`  
> Rule: this file closes R08 content-authoring blanks. Existing R08 ecology/traversal/combat/dungeon contracts remain authoritative unless explicitly refined here. `GAME_DESIGN.md` wins conflicts.

R08 is not another forest region with brighter colors. Its authored content demonstrates that some valuable modern magical ecology emerged only after historical regulation weakened, and that a restoration process can work exactly as designed while suppressing something the present world has come to depend on.

Exact external model/animation/VFX/audio filenames, hashes and Azari coordinates remain **pre-code gates**, never implementation discretion.

---

# 0. Player-facing language rule

No player-facing R08 UI, dialogue, quest, item, loading, map or system copy may expose development-stage language, internal state IDs, asset/license/hash notes, test terminology or implementation wording.

The player sees only world/game language. Missing localization or unresolved visible assets are content failures, not messages shown to the player.

---

# 1. Primary settlement — Lumenroot Sanctuary

Final player-facing settlement name: **Lumenroot Sanctuary**.

Lumenroot occupies a stable cliff shelf beneath a colossal flowering root-crown. It is a secluded scholar/artisan enclave whose architecture mixes rooted timber, pale stone, glass-like mineral accents and cultivated magical flora.

Visible identity:

```text
outer flowerwood trail
→ root arch / first sanctuary overlook
→ Lumenroot court
   ├─ Bloom Warden terrace
   ├─ Lens Archive
   ├─ pigment / catalyst workshop
   ├─ healer / alchemy garden
   ├─ Material Vault / merchant desk
   ├─ quiet inn / rest house
   └─ glass-root path toward research terraces
```

The settlement should look like it grew with the region rather than being a normal town covered in glowing plants.

---

# 2. Named R08 cast

| Character | Role | Normal anchor | Function |
|---|---|---|---|
| **Eira Vale** | Bloom Warden / ecologist | pollinator terrace | primary local quest owner; tracks bloom and pollinator cycles |
| **Orren Lume** | Archivist / Lens Keeper | Lens Archive | historical records, Knowledge Fairy/archive behavior, restoration-test evidence |
| **Mira Quen** | pigmenter / spellcraft artisan | Prism Workshop | pollen/pigment/catalyst economy, appearance and magical accessory service |
| **Sela Brin** | healer / gardener | sanctuary garden | alchemy, support flora and dangerous-surge treatment |
| **Tavin Rook** | sanctuary steward / merchant | central court | general services, Material Vault, visitor context |
| **Neris Fenn** | innkeeper / cook | Quiet Lantern rest house | rest, food and grounded sanctuary life |
| **Ilyan Voss** | recurring Anchor scholar | arrives after the first contradictory test evidence | interprets old suppression targets without deciding the moral conclusion for the player |
| **Sera Wren** | recurring cartographer / ranger | Prismroot Cliffs / route desk | spatial/ecology comparison and route context |
| **Kest Arden** | rival wanderer | optional ritual meadow / cliff scene | continuity, never required progression |
| **Director Aven Marr** | Restoration field lead | test station | competent restoration advocate; test succeeds technically and therefore matters narratively |

No named character is a disguised tutorial popup. Their services and world positions remain useful after their quests.

---

# 3. R08 pacing / reward benchmark

Suggested entry Lv remains **44**; dungeon climax is around Lv50.

Normal first-visit target:

```text
direct regional chain + first archive clear: ~100–140 min
regional chain + several optionals: ~150–220 min
broad first-visit completion including Titan Rabbit: ~3.5–4.5 h
```

Suggested levels remain guidance, not hard locks.

---

# 4. Main regional chain — A Forest Too Quiet

Final title: **A Forest Too Quiet**.  
Quest owner: **Eira Vale**.  
Category: Regional Main / Act-III eligible route.  
Permanent failure: none.

Opening line:

> “The test stopped the violent surges. It also stopped half the terrace from blooming. Safer is useful. Silent is not the same thing.”

## Required sequence

1. discover Lumenroot Sanctuary;
2. visit the active Restoration test terrace with Eira or its field marker;
3. inspect one healthy pollinator cluster outside the stabilized zone;
4. inspect one stabilized cluster where blooms remain closed and Hummingbird/Flutter activity has dropped;
5. enter the nearby archive alcove and observe one Knowledge Fairy phenomenon weakening during a stabilization pulse;
6. return to Eira and Orren.

Reward:

```text
EXP: 40% of current next-Lv requirement
Gold: 520
Class XP: 22% of current Class Rank requirement
```

Orren completion line:

> “Nothing is broken. That is what worries me. The old target is being reached, and the archive is losing phenomena we have studied for generations.”

Unlocks **What the Bloom Remembers**.

---

# 5. Main regional chain — What the Bloom Remembers

Final title: **What the Bloom Remembers**.  
Quest owners: **Orren Lume** and **Sera Wren**.

The player investigates **2 of 3** authored evidence sites in any order. The third remains available as optional context.

## Site A — Pollinator Terraces

- compare old engraved growth markers with current bloom spread;
- evidence shows modern pollination ranges exceed historical regulated boundaries.

## Site B — Quiet Library Grove

- observe Knowledge Fairy/archive behavior under naturally varying magical pressure;
- recover one short historical record noting deliberate suppression of unstable knowledge phenomena.

## Site C — Prismroot Cliffs

- compare old regulator lens alignment with current magical-root growth;
- physical sightline shows the historic system was designed to flatten pressure variation across a much wider zone.

After 2 distinct sites:

```text
EXP: 45% of current next-Lv requirement
Gold: 580
Class XP: 25% of current Class Rank requirement
```

Ilyan line:

> “The old network did exactly what its builders asked: fewer spikes, fewer surprises, fewer emergent systems. The question is whether the present world still wants that bargain.”

Unlocks **Safe According to Whom**.

---

# 6. Main regional chain — Safe According to Whom

Final title: **Safe According to Whom**.  
Quest owners: **Eira Vale**, **Director Aven Marr**, **Sela Brin**.

Purpose: prevent R08 from becoming a simplistic `nature good, regulation bad` story.

## Required sequence

1. a real magical surge forms near a sanctuary research route;
2. player stabilizes the surge through one authored combat/interaction event;
3. Sela treats/assesses the affected area, proving uncontrolled spikes can cause real harm;
4. Aven demonstrates the historical constant-suppression profile;
5. Eira proposes a bounded local safety profile;
6. player activates the local profile at the test station;
7. observe the result: dangerous spike control remains active around settlement/archive infrastructure, while ordinary background variation and pollination return outside those safety volumes.

Canonical regional control state after success:

```text
settlement/archive dangerous-spike limiter: ON
continuous region-wide suppression target: OFF
remote continental control authority: OFF
normal local magical variation: ALLOWED
```

There is no ideology dialogue wheel. The state is established through the practical regional solution.

Reward:

```text
EXP: 50% of current next-Lv requirement
Gold: 650
Class XP: 28% of current Class Rank requirement
```

Aven line:

> “The old profile is cleaner. The local one is messier. But it keeps people safe without flattening everything we came here to protect.”

Unlocks **The Glass-Root Archive**.

---

# 7. Optional contracts

Foreground at most two alongside the current main objective.

## 7.1 Colors That Live

Giver: **Mira Quen**.

Objective:

- gather 3 valid Magical Pollen from personal R08 nodes;
- gather 2 units from the accepted common pigment-flora family;
- return to Mira.

Reward:

```text
EXP: 18%
Gold: 300
Class XP: 10%
1 cosmetic pigment unlock from the accepted R08 palette
```

The reward is appearance-only and uses the Wardrobe/appearance system, not combat stats.

## 7.2 The Quiet Shelf

Giver: **Orren Lume**.

Objective:

- reach Quiet Library Grove;
- complete one Archive Echo interaction;
- resolve one authored Moonpriest/guardian pressure encounter if active.

Reward:

```text
EXP: 22%
Gold: 340
Class XP: 14%
```

## 7.3 A Spring Worth Keeping

Giver: **Sela Brin**.

Objective:

- discover the luminous spring;
- gather one valid mana-active flora node;
- clear or stabilize one local magical-pressure hazard.

Reward:

```text
EXP: 16%
Gold: 260
Class XP: 10%
prepared support consumable x1
```

---

# 8. Titan Rabbit ritual hunt — The Meadow That Answers

Final hunt title: **The Meadow That Answers**.

Discovery grammar:

- discover the ritual meadow through visible stone/flower geometry, local story or old archive symbol;
- no random boss spawn and no numeric clue counter;
- the invocation method becomes journal-readable after one legitimate ritual-record interaction.

First and repeat invocation requirement:

```text
4 Magical Pollen
+ 2 accepted R08 mana-flora materials
```

Rules:

- materials are consumed only when the encounter successfully enters server-authoritative active state;
- crash/disconnect before activation cannot consume the offering permanently;
- first clear and repeat rewards follow normal world-boss ownership rules;
- the encounter has an authored reset/cooldown, not an UltraSummonStone grind.

Eira warning line:

> “It is not a guardian and it is not tame. If the meadow answers, give it room to land.”

Kest optional line:

> “I've learned not to laugh at ritual warnings when the warning is rabbit-shaped.”

Titan Rabbit never gates the archive or Act-III evidence.

---

# 9. Main regional dungeon — The Glass-Root Archive

Final title: **The Glass-Root Archive**.  
Category: Regional Main / Act-III evidence.  
Suggested content band: Lv47–50.  
Permanent failure: none.

First-clear route remains:

```text
living approach
→ glass-root galleries
→ pollination lens / regulator chamber
→ memory archive
→ final guardian chamber
```

## Required evidence

The player must encounter these three facts:

1. **Suppression Target** — historical operation intentionally flattened regional magical-pressure variation;
2. **Emergence Record** — modern ecology/crafts/archive phenomena arose after that constant suppression weakened;
3. **Successful Modern Test** — the current restoration test was functioning correctly while suppressing those modern systems.

Player-facing investigation text:

> “The archive records fewer anomalies under full regulation—and far fewer bloom cycles, pollinator events and living-memory phenomena. The loss was intentional, not accidental.”

If an evidence interaction is missed, the post-boss archive core records the minimum required evidence to prevent softlock.

## First-clear completion reward

In addition to boss-layer personal loot:

```text
Gold: 720
EXP: 52% of current next-Lv requirement
Class XP: 34% of current Class Rank requirement
```

Deterministic 1-of-3 role choice:

1. **Mana sustain / catalyst or staff role**;
2. **healing/support/status-conversion accessory role**;
3. **precision magical/ranged hybrid role**.

Exact item names/models/icons are closed during the asset-binding gate before source bootstrap.

---

# 10. Regional resolution / post-clear scene

After the dungeon guardian is defeated:

1. remote historical suppression authority is disabled for the R08 branch;
2. bounded settlement/archive spike protection remains active;
3. selected pollinator terraces visibly reactivate;
4. Knowledge Fairy/archive phenomena return in safe authored spaces;
5. **R08 — Emergence / Cost of Historical Stability Evidence** commits to personal state;
6. Act-III progression updates if eligible.

Required post-clear characters:

- Eira Vale;
- Orren Lume;
- Director Aven Marr;
- Ilyan Voss.

Canonical scene:

**Eira Vale**
> “The terrace is moving again. Not perfectly. Living things rarely do.”

**Aven Marr**
> “And the limiter still catches the dangerous peaks. I came here expecting restoration or failure. This is neither.”

**Orren Lume**
> “It is a boundary. Keep the protection we need. Stop preserving an old definition of normal.”

**Ilyan Voss**
> “That distinction will matter when someone tries to make one answer fit the entire continent.”

Skipping the scene commits identical progression and journal information.

---

# 11. Shared and personal state

Shared/world-persistent candidates:

- bounded local safety profile active;
- one bloom bridge / route reopens;
- selected pollinator terrace visibly recovers;
- archive shortcut remains available.

Personal state:

- R08 evidence package;
- quest rewards;
- Titan Rabbit discovery/clear;
- optional-contract flags;
- local dialogue flags;
- first-clear reward ownership.

Late join sees the current regional physical state but can still obtain personal evidence through preserved interactions/condensed archive playback.

---

# 12. Reconnect / failure contract

- site evidence commits immediately on legitimate interaction;
- optional 2-of-3 evidence never resets on defeat;
- surge failure resets only the active encounter, not prior main progress;
- bounded regulator activation is idempotent;
- Titan Rabbit offering is transactional and cannot double-consume;
- dungeon evidence/reward commits are independently recoverable after disconnect;
- no first-clear duplication through relog/chunk unload;
- shared post-clear world state never prevents a late joiner from completing personal narrative state.

---

# 13. What is closed / what remains gated

Closed before implementation:

- Lumenroot Sanctuary identity;
- named cast and scene ownership;
- exact main quest order and completion rules;
- bounded-regulation regional solution;
- optional contracts;
- Titan Rabbit invocation resource quantities;
- reward percentages/Gold;
- dungeon evidence and deterministic reward roles;
- post-clear dialogue/state;
- reconnect/late-join behavior.

Still mandatory **pre-code asset gates**, not implementation discretion:

- exact magical-flora models and final material display names where the model determines identity;
- exact final dungeon guardian model/attack table;
- exact item models for deterministic dungeon choices;
- exact VFX/SFX/BGM bindings;
- exact Azari coordinates/volumes/sightlines.

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
