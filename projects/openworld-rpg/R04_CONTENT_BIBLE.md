# Open-World RPG — R04 Frozen Crown Content Bible

> Status: **DESIGN CANON — Hearthspring cast, regional quest flow, waystation restoration, dungeon evidence, rewards and rejoin behavior locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Region/system package: `R04_IMPLEMENTATION_PACKAGE.md`  
> Story: `WORLD_STORY_CANON.md`  
> Quest/state: `QUEST_WORLD_STATE.md`  
> Cross-region identity: `REGION_CROSS_AUDIT.md`  
> Rule: this file closes R04 content-authoring blanks while preserving the existing R04 ecology/combat/weather/dungeon contracts. `GAME_DESIGN.md` wins conflicts.

R04's content job is to provide the first strong playable argument that **restoring a bounded piece of old infrastructure can genuinely help ordinary people**. It must do this through visible route/shelter improvements, not a lecture.

Exact external files, hashes and Azari coordinates remain pre-code gates, not implementation choices.

---

# 0. Player-facing language

No development wording, internal IDs, asset/license notes, debug text, placeholder labels or test terminology may appear to the player.

The terms in this file such as `WORLD_PERSISTENT` or internal state keys are production-only.

---

# 1. Primary settlement — Hearthspring Refuge

Final player-facing settlement name: **Hearthspring Refuge**.

Hearthspring is a geothermal expedition refuge built around dark exposed rock, steam vents and a partially maintained heat-routing network. It is not a generic snow village.

Visible identity:

```text
steam plume / dark-rock basin
→ outer expedition gate
→ central heated court
   ├─ Hearthspring Lodge
   ├─ quartermaster / expedition board
   ├─ healer / thermal works
   ├─ merchant / Material Vault
   ├─ smith / repair shed
   ├─ hunt board
   └─ outward route markers toward three waystation corridors
```

The player understands why people can live here before any exposition: warm water, dry storage, active work, food preparation and sheltered traffic are physically visible.

---

# 2. Named R04 cast

| Character | Role | Normal anchor | Function |
|---|---|---|---|
| **Ona Brant** | refuge steward / quartermaster | expedition hall | primary local quest owner; supplies/route stakes; practical restoration case |
| **Toren Vale** | ice surveyor / route keeper | route tower / whiteout board | landmark navigation, storm reading, cautious repair perspective |
| **Selka Rime** | healer / thermal worker | bath/healer works | geothermal-water and winter-herb service; shows direct human benefit |
| **Harl Fen** | hunter / material buyer | hunt shed | Froststalker/Iceworm context, creature materials |
| **Mira Sol** | smith / repair worker | repair shed | ordinary gear/route repair support, no new profession tree |
| **Jori Pell** | innkeeper / fish buyer | Hearthspring Lodge | rest/cooking/cold-water fish economy |
| **Ilyan Voss** | recurring Anchor scholar | arrives during main investigation | interprets old heat-route evidence after it is found |
| **Daren Holt** | recurring engineer/smith | arrives for buried heat-facility investigation | distinguishes local maintained machinery from old network components |
| **Kest Arden** | rival wanderer | optional Iceworm/outer-route scenes | world continuity, no mandatory quest ownership |

Target physical population remains the R04 package baseline: 10–14 functional/expedition/guard/service NPCs + 5–8 ambient residents/travelers.

---

# 3. Main regional chain — The Last Warm Road

Final title: **The Last Warm Road**.  
Quest owner: **Ona Brant**.  
Category: Main/Regional.  
Repeatability: once per player.  
Failure: cannot permanently fail.

Ona's first line:

> “Hearthspring is warm enough. The road isn't. Windblind Waystation went dark last night, and two supply teams turned back. Find out whether the station failed or the route did.”

Sequence:

1. leave Hearthspring by the marked inland route;
2. encounter one authored whiteout front on approach to Windblind Waystation;
3. navigate using dark rock, route poles and the station's intermittent lantern/steam silhouette rather than a floating arrow;
4. clear the local Froststalker pressure around the station — solo baseline 3 Froststalkers, bounded by regional pack rules;
5. inspect the frozen local valve/intake;
6. use the manual bypass to restore **Windblind Waystation**.

No cold meter or consumable is required.

Immediate visible effect:

- station lantern/steam returns;
- interior becomes a safe weather shelter/rest interaction;
- the nearest route gets bounded expedition traffic later;
- Selka/Ona dialogue changes on return.

Reward:

```text
EXP: 35% current next-Lv requirement
Gold: 260
Class XP: 20% current Class Rank requirement
```

Ona completion line:

> “One station back is enough to move supplies again. The question is why three branches started failing in the same week.”

---

# 4. Main continuation — Lights in the White

Final title: **Lights in the White**.  
Category: Main.  
Availability: `The Last Warm Road` complete.

Three outer stations exist:

1. **Windblind Waystation** — inland taiga / whiteout route;
2. **Blue-Ice Crossing** — glacial shelf / crevasse crossing;
3. **Seal Reach Station** — frozen coast / supply landing.

Windblind is already restored by the opening quest. The player chooses **one of the remaining two** to stabilize for main progression. The third remains optional and useful.

This is route choice, not a hidden best answer.

## 4.1 Blue-Ice Crossing

Sequence:

1. follow the blue-ice scar landmark;
2. cross one authored crevasse/ice-route traversal sequence;
3. reach the station's exposed conduit;
4. clear a small local threat or environmental blockage depending on active encounter state;
5. rotate/open the local bypass manifold;
6. verify steam/flow reaches the crossing shelter.

No material tax.

## 4.2 Seal Reach Station

Sequence:

1. follow coastline markers toward the frozen landing;
2. pass through Seal ecology without forcing combat;
3. deal with one authored route problem — normally Froststalker pressure or a frozen intake obstruction;
4. open the local thermal intake and station valve;
5. verify the supply landing returns to service.

## Main completion reward after a second total station is active

```text
EXP: 30% current next-Lv requirement
Gold: 220
Class XP: 20% current Class Rank requirement
```

Restoring the third station later awards:

```text
EXP: 10%
Gold: 100
Class XP: 6%
```

After two stations:

- Hearthspring supply props/traffic improve modestly;
- the old heat branch's inconsistent pattern becomes clear;
- `Heat Beneath Ice` begins.

Shared station state is server-authoritative and late-join safe; personal quest credit is separately retained.

---

# 5. Regional side quest — Shelter in the Storm

Final title: **Shelter in the Storm**.  
Quest owner: **Toren Vale**.  
Category: Regional.  
Not required for main progression.

Purpose: make whiteout navigation a short authored skill instead of generic survival punishment.

Toren's line:

> “Anyone can follow a pole line in clear weather. I need to know the old emergency cairns still work when the mountain disappears.”

Sequence:

1. start from a known route marker near Hearthspring;
2. enter an authored whiteout search area;
3. use sound/steam/dark-rock/cairn silhouettes to locate the emergency shelter;
4. relight/open its mechanical beacon from inside;
5. return is optional; quest completes remotely because the visible beacon proves success.

Reward:

```text
EXP: 20%
Gold: 160
Class XP: 14%
```

No escort, no timer and no failure penalty beyond restarting the event/search state if the player leaves.

---

# 6. Optional cold-water contract — Dark Water, Bright Scale

Final title: **Dark Water, Bright Scale**.  
Quest owner: **Jori Pell**.  
Category: Contract / fishing.  
Repeatability: once per player.

Objective:

```text
catch 1 Common or Uncommon R04 fish personally
from Seal Reach / geothermal open water / valid frozen-coast pool
return to Jori Pell
```

The fish is not consumed; the quest records the catch and updates the Fish Codex normally.

Reward:

```text
EXP: 18%
Gold: 140
Class XP: 12%
```

No rare weather-only fish is required.

---

# 7. Main investigation — Heat Beneath Ice

Final title: **Heat Beneath Ice**.  
Category: Main / Act-II evidence setup.  
Availability: two outer stations active.

Ona:

> “Two stations are holding, but the flow between them doesn't make sense. We're feeding heat into one branch and losing it somewhere under the glacier.”

Daren Holt, after examining the refuge manifold:

> “Your local pipes aren't the problem. They end at an older trunk line. Whatever built that line expected to control heat far below us.”

Ilyan Voss:

> “Then we test the local branch, not the continent. If it helps Hearthspring, that's evidence. If it causes something else, that's evidence too.”

Progression:

1. inspect Hearthspring's old trunk-line interface;
2. follow geothermal vents and the Blue-Ice Scar toward the deep fissure;
3. discover the newly widened glacial opening;
4. enter the regional dungeon **The Opened Fissure**.

Discovery reward:

```text
EXP: 40%
Gold: 260
Class XP: 25%
```

Suggested dungeon range: Lv23–25. No hard gate.

---

# 8. Ferox Iceworm hunt

Final player-facing name remains **Ferox Iceworm** because the accepted dependency creature already owns a strong readable identity.  
Suggested Lv: **24**.  
Main-story requirement: optional.

## Discovery

No clue counter.

1. the player first sees an abnormally long moving fracture/mound crossing Iceworm Basin or discovers the surface trench after an encounter window;
2. journal adds a broad basin hunt area;
3. repeated surface motion and broken ice naturally lead to the encounter zone;
4. finding the Iceworm first is valid.

Kest optional line:

> “I thought it was a crack in the ice until the crack turned around.”

## Signature material

Final name: **Iceworm Plate**.

```text
first eligible defeat: Iceworm Plate x2
repeat: x1
direct Mythic roll: 15%
EXP: 20%
Class XP: 15%
```

## Mythic pool

### Rimebreaker Pike

- Mythic Item Lv24;
- family: spear/polearm;
- authored affixes: STR / Physical Power / specific weapon-family power;
- unique: **after hitting an enemy emerging from movement/charge/burrow-type committed motion, deal +30% poise damage for that hit; 6 s internal cooldown**;
- runtime tag is authored on encounters, not inferred from client animation.

### Whitewake Charm

- Mythic Item Lv24;
- family: Charm;
- authored affixes: END / Movement Speed / negative-status duration reduction;
- unique: **when the wearer becomes Slowed/Chilled by a valid hostile status, immediately reduce that status's remaining duration by an additional 25%; 12 s internal cooldown**;
- does not grant blanket immunity.

Exact visuals remain pre-code bindings.

---

# 9. Dungeon — The Opened Fissure

Final quest/dungeon title: **The Opened Fissure**.  
Target first-clear duration: **22–32 min**.

Existing dungeon sequence remains canonical:

```text
new glacial fissure
→ geothermal under-ice river
→ old heat-exchange facility
→ brood boundary
→ Icebroodmother chamber
```

## Critical story interaction — bounded local restoration

In the old heat-exchange facility, before the brood boundary, the player reaches the local branch regulator.

The player does **not** press a generic `Restore Network` button.

Sequence:

1. Daren/Ilyan identify the branch as the one serving Hearthspring's outer stations;
2. player clears the immediate facility encounter/hazard;
3. player opens the local bypass and isolates the branch from the damaged deeper trunk;
4. heat/steam visibly returns through the facility and to the surface branch;
5. server commits the bounded regional restoration.

Immediate world effects before the boss is defeated:

- the two restored waystations become more stable/visibly active;
- Hearthspring thermal flow improves;
- one previously unreliable sheltered route becomes consistently usable;
- Selka later reports practical treatment/water benefit.

This is crucial: **the benefit happens before Icebroodmother dies**, proving the boss was not the magical cause of every problem.

The restoration also opens/warms a path deeper through the fissure, exposing the brood boundary.

---

# 10. Icebroodmother

Final player-facing boss name: **Icebroodmother**.  
Suggested Lv: **25**.  
Combat kit/minion caps remain the R04 implementation-package contract.

## Signature material

Final name: **Broodmother Carapace**.

```text
first eligible clear: x2
repeat: x1
direct Mythic roll: 15%
```

## Mythic pool

### Frostweaver Staff

- Mythic Item Lv25;
- family: staff;
- authored affixes: WIL / Magic Power / implemented frost-status output;
- unique: **when the wielder applies a valid Slow/Chill/Frostbite-type status to a target already affected by one such project status, extend the newly applied effect's duration by 20%; 8 s internal cooldown per target**;
- does not create a new status family.

### Broodwinter Aegis

- Mythic Item Lv25;
- family: shield;
- authored affixes: END / Guard Strength / negative-status duration reduction;
- unique: **after a successful perfect guard, hostile status buildup received is reduced by 25% for 5 s; 10 s internal cooldown**.

Exact visuals/VFX remain pre-code asset gates.

---

# 11. First-clear curated choice

All three are **Superior Item Lv25** with fixed affixes at deterministic **85th-percentile** canonical values.

## 11.1 Hearthguard Shield

- standard/heavy shield;
- fixed affixes: `END`, `Guard Strength`, `Poise/Stagger Resistance`;
- defensive/high-poise role.

## 11.2 Rimebind Focus

- magical focus;
- fixed affixes: `WIL`, `Max Mana`, `implemented frost/status output`;
- control/support role without class lock.

## 11.3 Snowstep Boots

- Medium Boots;
- fixed affixes: `DEX`, `Movement Speed`, `negative-status duration reduction`;
- mobility/anti-slow/precision role.

External model/icon bindings are mandatory before implementation.

---

# 12. Dungeon completion reward

Each eligible player's first clear grants:

```text
guaranteed Superior+ normal R04 equipment roll
Broodmother Carapace x2
15% direct Mythic roll: Frostweaver Staff / Broodwinter Aegis
choose 1: Hearthguard Shield / Rimebind Focus / Snowstep Boots
completion EXP: 50% current next-Lv requirement + boss contribution
completion Class XP: 32% current Class Rank requirement + boss contribution
Gold: 480
```

Repeat follows global dungeon rules; deterministic first-clear reward cannot duplicate.

---

# 13. Act-II evidence — Hearthspring Heat Record

After the local branch is isolated/restored and the dungeon first clear completes, the player receives personal evidence state **Hearthspring Heat Record**.

Player-facing journal text:

> “A local heat branch was isolated from the damaged trunk and restored without reconnecting the wider network. Hearthspring's shelters, water and supply route improved immediately. The old infrastructure can still protect people when its scope is understood and bounded.”

This is one valid R04–R07 Act-II evidence package.

It does **not** state that continental restoration is therefore correct.

---

# 14. Hearthspring return scene

After first clear, a short scene becomes available at the central thermal works.

Participants:

- Ona Brant;
- Selka Rime;
- Toren Vale;
- Daren Holt;
- Ilyan Voss when the player's main story has reached the relevant investigation.

**Selka Rime**
> “The treatment pools are holding temperature again. We moved the outer patients inside without losing the water line.”

**Ona Brant**
> “That's the first week in months I can send supplies north without planning for a dead shelter.”

**Toren Vale**
> “Good. We repaired one branch we can see. That doesn't mean we hand the whole mountain to a machine we can't.”

**Daren Holt**
> “Agreed. Local controls first. Anything beyond that waits for evidence.”

**Ilyan Voss**
> “And this is evidence: restoration can work. The question is how far the connection should reach.”

Progress effects:

- R04 Act-II evidence committed;
- main story counts it toward the required two R04–R07 evidence packages;
- no ending/faction choice is forced here.

---

# 15. Dynamic event rewards

## Stranded Expedition

```text
EXP: 9%
Gold: 90
Class XP: 6%
```

Short rescue/secure-area event; no long escort.

## Froststalker Crossing

```text
EXP: 8%
Gold: 80
Class XP: 5%
```

Can be fought or bypassed depending on event objective context.

## Whiteout Signal

```text
EXP: 8%
Gold: 75
Class XP: 5%
```

Navigation/search interaction event. Failure has no extra punishment.

Event cooldowns use authored active-time windows, normally 12–18 minutes, never daily-login schedules.

---

# 16. Regional aftermath

After two stations + local branch restoration + first dungeon clear:

- stabilized waystations remain lit/active;
- one supply route gains bounded ambient traffic;
- Hearthspring's thermal presentation becomes slightly more active;
- selected merchant/food/fish stock expands within existing systems;
- Selka/Ona/Toren dialogue reflects actual improvement;
- region remains cold, stormy and dangerous;
- Froststalkers/Iceworm/ecology do not vanish;
- the player improved human reliability, not climate.

---

# 17. State / reconnect / multiplayer

Required personal state includes at minimum:

```text
r04_hearthspring_discovered
r04_last_warm_road_state
r04_windblind_personal_credit
r04_blue_ice_personal_credit
r04_seal_reach_personal_credit
r04_lights_in_white_complete
r04_shelter_storm_complete
r04_fishing_contract_state
r04_heat_beneath_ice_stage
r04_local_branch_restored_personal_credit
r04_dungeon_first_clear
r04_first_clear_choice_claimed
r04_hearthspring_heat_record
r04_return_scene_seen
r04_iceworm_discovered
r04_iceworm_first_clear
```

Shared station/route/local-branch physical state is stored separately as world-authoritative state.

Rules:

- a late joiner sees restored infrastructure but can still perform retained contextual interaction/get personal story credit;
- split party can work on different stations;
- first player to repair a station does not consume another player's reward;
- disconnect during whiteout resumes from durable quest stage, not temporary client presentation;
- dungeon reward/evidence transactions are idempotent;
- Iceworm burrow and Icebroodmother add state remain server-authoritative.

---

# 18. Remaining pre-code gates

R04 content authoring is closed, but implementation waits for:

1. direct 26.2 review of Moose/Seal/Snow Leopard/Froststalker models/AI/scale;
2. Ferox Iceworm exact model/animation/hit-volume and Iceworm Plate/Mythic visual bindings;
3. Icebroodmother/Ice Weaver exact model/animation/VFX and Broodmother reward visuals;
4. final dungeon shell decision after donor Ice Palace inspection;
5. exact cold crystal and winter herb models/names/values or explicit removal before code;
6. exact R04 fish roster/models/names/value tables;
7. Hearthspring architecture, waystation props and NPC outfits;
8. geothermal/whiteout/boss VFX and regional audio bindings;
9. exact named reward models/icons;
10. Azari coordinates, station routes, travel times, storm volumes and dungeon placement.

None may be decided casually in gameplay code.

---

# 19. Current closure status

```text
CONTENT DESIGN REVIEWED: YES
R04 QUEST/NPC/SCENE AUTHORING AT CLOSURE LEVEL: YES
RESTORATION BENEFIT / ACT-II EVIDENCE LOCKED: YES
FIELD/DUNGEON BOSS REWARD IDENTITIES LOCKED: YES, subject to pre-code asset acceptance
EXACT ASSET INTAKE CLOSED: NO
AZARI SPATIAL CLOSURE: NO
CODE REVIEWED: NOT APPLICABLE
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

No build/CI is warranted for this docs-only pass.
