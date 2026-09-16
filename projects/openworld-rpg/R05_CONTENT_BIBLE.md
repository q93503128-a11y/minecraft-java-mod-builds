# Open-World RPG — R05 Jungle Greenbelt Content Bible

> Status: **DESIGN CANON — settlement, named cast, regional/main quests, Jungle Komodo unlock, rewards, scenes, Act-II evidence and reconnect behavior locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Region/system package: `R05_IMPLEMENTATION_PACKAGE.md`  
> Story: `WORLD_STORY_CANON.md`  
> Quest/state: `QUEST_WORLD_STATE.md`  
> Cross-region identity: `REGION_CROSS_AUDIT.md`  
> Mounts: `MOUNTS.md`  
> Rule: this file closes R05 content-authoring blanks. Existing R05 ecology/traversal/combat/dungeon contracts remain authoritative unless explicitly refined here. `GAME_DESIGN.md` wins any conflict.

R05 must not become `R04 but warm` or `R02 but denser`. Its authored content is about a society that already learned to live with moving water, roots and seasonal change, then discovers that an old regulator is trying to force the jungle back toward a map that no longer matches reality.

Exact external model/animation/VFX/audio filenames, hashes and Azari coordinates remain **pre-code gates**. They are not implementation discretion.

---

# 0. Player-facing language rule

No player-facing R05 UI, dialogue, quest text, map text, item copy, loading text or system message may expose:

- `alpha`, `beta`, `prototype`, `temporary`, `placeholder`, `TODO`, `debug`, `developer`;
- internal quest/state keys such as `r05_*`, asset-binding IDs or encounter-controller names;
- license/hash/provenance notes;
- acceptance-test wording or implementation terminology.

Internal state always resolves into ordinary world/player language. Missing localization or model binding is a content/build failure, not a player message.

---

# 1. Primary settlement — Tanglewater Market

Final player-facing settlement name: **Tanglewater Market**.

Tanglewater sits on stable high ground where three useful river channels, raised boardwalks and root roads meet. It is a working river market, not a jungle-themed full-service capital.

Visible identity:

```text
great river / ferry approach
→ raised dock market
   ├─ Rainleaf House inn / cook
   ├─ herb & alchemy arcade
   ├─ market factor / route board
   ├─ Material Vault counter
   ├─ boatwright / Komodo paddock
   ├─ warden platform
   └─ raised homes / market roofs / outward boardwalks
```

The settlement must visibly adapt to rain and changing water through raised floors, flexible docks, flood marks, storage above ground and multiple routes.

---

# 2. Named R05 cast

| Character | Role | Normal anchor | Function |
|---|---|---|---|
| **Nara Vey** | River Factor / market coordinator | factor office / main dock | primary regional quest owner; trade-route perspective; initially open to old regulator restoration |
| **Halen Moss** | forest warden / ecologist | warden platform / outer trails | ecology, predator, seasonal-route perspective; grounded autonomy case |
| **Pera Tal** | boatwright / Komodo handler | lower dock / Komodo paddock | Jungle Komodo unlock and local transport |
| **Mira Sen** | healer / alchemist | herb arcade | alchemy, rare jungle herbs, venom/cleanse utility |
| **Tovan Reed** | smith / equipment worker | market forge | ordinary regional gear and route hardware service |
| **Jori Vale** | innkeeper / cook | Rainleaf House | rest, meals, fish buyer and grounded settlement life |
| **Sera Wren** | recurring cartographer / ranger | Crownroot Overlook / route board | helps compare old map geometry with living terrain without solving it for the player |
| **Ilyan Voss** | recurring Anchor scholar | arrives after old regulator evidence appears | interprets old network records only after discovery |
| **Kest Arden** | rival wanderer | optional Earthloong / temple-route scenes | continuity and optional world texture; never required for completion |

Target daytime population remains the existing R05 package baseline: 12–17 functional/trader/fisher/warden/service NPCs + 6–10 ambient residents/travelers.

No vanilla villagers are final population.

---

# 3. R05 pacing / reward benchmark

Suggested entry Lv remains **20**. The first regional visit should not require grinding to the dungeon's Lv27 suggestion.

Normal first-visit target from Tanglewater discovery:

```text
direct regional chain + first dungeon clear: ~80–110 min
regional chain + Komodo + several optionals: ~120–170 min
broad first-visit completion including mature Earthloong/fishing/side POIs: ~2.5–3.5 h
```

Reward structure follows the global pattern already used in R01–R04:

- tiny discoveries/dynamic events: modest progress;
- authored regional stages: meaningful but sub-level chunks;
- dungeon completion: major deterministic progress;
- no hard level gate.

R05 fixed authored quest payments below are deliberately larger than early-game R01 but remain below housing-tier inflation.

---

# 4. Main regional chain — Where the River Forks

Final title: **Where the River Forks**.  
Quest owner: **Nara Vey**.  
Category: Regional Main / Act-II eligible route.  
Repeatability: once per player.  
Permanent failure: none.

Nara's first line:

> “The river always changes. That's normal. What's happening now isn't. Two channels reversed in the same morning, and the old flood stones are humming again.”

The quest does not begin with an exposition lecture. The player is sent to **Splitwater Gardens**, where the difference between normal seasonal adaptation and current regulator pulses is physically visible.

## 4.1 Required flow

1. discover Tanglewater Market;
2. speak to Nara once or accept the board lead;
3. reach Splitwater Gardens;
4. inspect the **dry nursery inlet** where water has been forced out of a modern fish/herb pool;
5. inspect the **reopened old channel** where current pushes through a route residents stopped using generations ago;
6. return to Nara or trigger the field follow-up through Halen if he is present nearby.

Both site interactions are required because the contrast is the content: one modern route is being harmed while an obsolete route is being restored.

Reward:

```text
EXP: 35% of current next-Lv requirement
Gold: 220
Class XP: 20% of current Class Rank requirement
```

Nara completion line:

> “That old channel used to be the main route. It hasn't been useful in my lifetime. If the buried works are trying to restore it, they're restoring a map—not our river.”

Unlocks:

- **Paths Above the Water**;
- Pera Tal's **A Faster Trail** Komodo quest if not already discovered;
- Halen's optional predator/ecology contract pool.

Reconnect behavior:

- each site commit persists independently;
- defeat/disconnect never resets inspected state;
- no carried quest item is required.

---

# 5. Main regional chain — Paths Above the Water

Final title: **Paths Above the Water**.  
Quest owners: **Halen Moss** with route support from **Sera Wren** if the main story is active here.

Purpose: teach R05's actual traversal grammar instead of sending the player through a checklist of generic markers.

## 5.1 Required path

The player must complete these two authored traversal discoveries:

### Crownroot Overlook

- reach the overlook by any legal ground/root/elevated approach;
- interact with the route table / sightline point;
- the player sees the current river, Tanglewater, temple crown and old causeway geometry in one view.

### Bamboo Causeway

- traverse the causeway to its eastern marker;
- resolve or bypass the authored territorial-predator pressure;
- activate the permanent route marker/shortcut interaction.

After both, the player receives a broad temple-search area and the journal records that the old regulator lines point toward the stepped temple.

Reward:

```text
EXP: 40% of current next-Lv requirement
Gold: 250
Class XP: 22% of current Class Rank requirement
```

Halen completion line:

> “That's the part old maps miss. The jungle didn't wait for the stone roads. Roots rose, channels moved, people moved with them.”

No mandatory canopy parkour, chest or kill count is attached to this quest.

---

# 6. Main regional chain — A Map That No Longer Fits

Final title: **A Map That No Longer Fits**.  
Quest owners: **Nara Vey**, **Halen Moss**, with **Ilyan Voss** entering only after the old regulator plate is discovered.

## 6.1 Field sequence

1. reach the old causeway / outer temple regulator marker revealed by `Paths Above the Water`;
2. inspect an old carved/metal route plate showing fixed ancient channels;
3. compare it to the current river view at the nearby flood shelf;
4. trigger one bounded regulator pulse that visibly moves local water/root pressure;
5. return to Tanglewater for the short comparison scene or continue directly to the temple if the player has already seen the scene once in another personal state context.

The regulator pulse is scripted/local. It does not dynamically rewrite the entire region.

Reward:

```text
EXP: 45% of current next-Lv requirement
Gold: 300
Class XP: 25% of current Class Rank requirement
```

## 6.2 Tanglewater scene

Required characters:

- Nara Vey;
- Halen Moss;
- Ilyan Voss.

Canonical dialogue:

**Ilyan Voss**
> “The old diagram is consistent. That's the problem. It assumes the river should still be where it was centuries ago.”

**Nara Vey**
> “I wanted a stable route. Not one that dries a nursery pool so a dead road can look tidy again.”

**Halen Moss**
> “Keep what protects people. Cut what tries to command the whole jungle.”

**Ilyan Voss**
> “Then the temple is where we separate those two things.”

This scene unlocks **Roots Beneath the Steps**.

---

# 7. Jungle Komodo quest — A Faster Trail

Final title: **A Faster Trail**.  
Quest owner: **Pera Tal**.  
Category: Optional Regional / Mount.  
Availability: Tanglewater discovery; direct hint after `Where the River Forks`.  
Reward/registration: once per player.

Pera's opening line:

> “Stags are good on roads. These aren't roads. If you want something that turns between roots instead of fighting them, come see how the Komodos choose a path.”

## 7.1 Exact objective

1. meet Pera at the paddock;
2. follow the broad field lead to the **Rootloop Trail**;
3. observe one authored wild-Komodo route marker / basking site;
4. clear or avoid one territorial hazard blocking the handler's approach;
5. complete a short handling trial on the accepted registration Komodo: ride through **three large route gates** in sequence, with no speed-rank requirement and no fail-on-one-mistake timer;
6. return to Tanglewater;
7. pay the canonical **600 Gold registration/tack fee**;
8. Jungle Komodo permanent unlock commits.

There is no random food-taming chance and no repeated `feed until hearts` loop.

Canonical movement remains `MOUNTS.md`:

```text
cruise: 8.4 b/s
dash: 10.5 b/s for 2.5 s
very high turning response
Resolve: 1.20x player MaxHP
```

Quest reward in addition to the mount unlock:

```text
EXP: 30% of current next-Lv requirement
Class XP: 15% of current Class Rank requirement
```

The 600 Gold is the only Gold cost; the quest does not also charge a hidden handling fee.

Pera completion line:

> “Don't drive it like a stag. Let it cut the corner. That's what it was built for.”

Failure/reconnect:

- route-gate progress may reset if the player abandons the handling trial, but no Gold is lost before final registration;
- registration is atomic;
- disconnect after payment cannot duplicate or lose ownership.

---

# 8. Optional contracts

R05 board foreground remains low-density. Normally show at most two relevant optional contracts alongside the main objective.

## 8.1 Rainleaf Stock

Giver: **Mira Sen**.

Objective:

```text
harvest 2 accepted rare-jungle-herb nodes personally
+ obtain 1 shared venom reagent from a valid regional source OR turn in 1 existing carried reagent
```

The herb requirement teaches regional gathering; the venom component does not force a rare-predator grind.

Reward:

```text
EXP: 20%
Gold: 170
Class XP: 12%
Cleansing Tonic x1
```

## 8.2 A Clear Crossing

Giver: **Halen Moss**.

Objective: secure one authored trade-path predator incident by meaningful participation. The actual encounter may be Tiger, Anaconda or another admitted regional predator according to the authored site.

Reward:

```text
EXP: 22%
Gold: 190
Class XP: 14%
```

No repeatable kill-count farm is attached.

## 8.3 River Table

Giver: **Jori Vale**.

Objective:

- discover any valid R05 fishing spot;
- catch one ordinary R05-eligible fish;
- return or simply register the catch depending on service UX.

Reward:

```text
EXP: 15%
Gold: 150
Class XP: 8%
prepared regional meal x1
```

No rare/weather fish requirement.

---

# 9. Mature Earthloong hunt — The Deep Root Trembles

Final hunt title: **The Deep Root Trembles**.

Discovery sources may include:

- large fresh ground displacement;
- cracked root arches;
- distant sighting;
- Halen/Kest witness context.

There is **no numeric clue counter requirement**.

Once the player discovers one strong major sign, the journal gains a broad Earthloong Basin search area. Following continuous terrain damage/ground marks leads naturally toward the basin. Finding the boss first is always valid.

This hunt never gates the temple.

First eligible defeat rewards follow the R05 package and global field-boss rules; no extra quest Gold is added on top of the boss package unless the player had an authored hunt contract active.

Kest optional line after first sighting:

> “Same kind you saw near Alderford? Maybe. Bigger lesson either way: don't assume you've seen the adult just because you've seen the species.”

---

# 10. Main regional dungeon — Roots Beneath the Steps

Final quest title: **Roots Beneath the Steps**.  
Category: Regional Main / Act-II evidence.  
Suggested content band: Lv24–27.  
Permanent failure: none.

The player enters the already-designed stepped temple / root-vault dungeon.

First-clear sequence remains the R05 package:

```text
exterior stepped temple
→ root court
→ flooded archive / water-control level
→ deep root vault
→ regulator chamber / final guardian
```

## 10.1 First-clear story interactions

The player must encounter three story-state interactions during first clear:

1. **Fixed Channel Relief** — proves the regulator expected fixed ancient channels;
2. **Reclaimed Root Vault** — shows long-term natural/local adaptation after control declined;
3. **Continental Coupling Junction** — identifies the dangerous remote-control path that can be isolated from useful local structure.

If one interaction is missed before the boss, the safe post-boss regulator interaction records the missing required evidence so the main story cannot softlock.

## 10.2 Dungeon completion reward

In addition to boss-layer personal loot:

```text
Gold: 420
EXP: 50% of current next-Lv requirement
Class XP: 32% of current Class Rank requirement
```

The first-clear deterministic 1-of-3 equipment choice remains model-first. The three gameplay roles are locked now:

1. **DEX / rapid-attack weapon role**;
2. **WIL/INT status-support accessory or focus role**;
3. **mobility / cleanse / anti-grab defensive utility role**.

Exact names/icons/models are a pre-code asset gate; implementation cannot invent temporary names/art.

---

# 11. R05 final regional resolution

After the dungeon guardian is defeated:

1. the player accesses the regulator core;
2. the continental control/coupling path is isolated;
3. useful local passive/structural functions remain intact where safe;
4. one modern side channel/crossing visibly stabilizes/reopens;
5. a journal evidence package is committed: **R05 — Local Adaptation Evidence**;
6. the player's Act-II evidence count updates if eligible.

The region does **not** restore the ancient river map.

## 11.1 Post-clear Tanglewater scene

Required characters:

- Nara Vey;
- Halen Moss;
- Ilyan Voss.

Canonical dialogue:

**Nara Vey**
> “The market's routes are steady again. Not the old routes. Ours.”

**Halen Moss**
> “Good. A river isn't broken because it moved.”

**Ilyan Voss**
> “And a system isn't useful merely because it can force the world back into its records.”

If R05 is the player's second qualifying R04–R07 evidence package, the broader Act-II progression advances according to `WORLD_STORY_CANON.md`. If not, the journal points only to the remaining eligible evidence regions without making one mandatory route.

---

# 12. Regional aftermath / shared world memory

Shared late-join-safe outcome:

- one previously unstable crossing/side channel becomes a dependable travel route;
- regulator-pulse dynamic events become less frequent or change into harmless maintenance variants;
- market dock/ferry activity expands modestly;
- selected merchant/alchemy/fish stock expands;
- the dungeon repeat shortcut remains available under normal dungeon rules.

Personal state:

- main chain completion;
- R05 evidence package;
- Jungle Komodo unlock/registration;
- Earthloong discovery/clear;
- optional contracts;
- Fish Codex/trophy states;
- first-clear/reward claim.

Late join sees the shared route improvements but can still perform the full personal evidence interactions through preserved interaction states/journal logic.

---

# 13. Exact state/reconnect contract

Suggested personal states:

```text
r05_market_discovered
r05_main_stage
r05_splitwater_dry_inlet_seen
r05_splitwater_old_channel_seen
r05_crownroot_seen
r05_causeway_crossed
r05_old_map_seen
r05_regulator_pulse_seen
r05_komodo_trial_state
r05_komodo_registered
r05_earthloong_discovered
r05_earthloong_first_clear
r05_temple_discovered
r05_temple_first_clear
r05_evidence_recorded
r05_first_clear_reward_claimed
```

All progression-changing fields are server-authoritative.

Rules:

- defeat never erases completed investigation state;
- disconnect during a scene skips no required information because journal text mirrors committed evidence;
- rewards commit atomically;
- shared route changes never substitute for another player's personal quest credit;
- no quest depends on one transient ambient NPC pathing correctly across the jungle.

---

# 14. Player-facing acceptance

R05 is content-complete only when implementation can reproduce all of the following without inventing design:

- Tanglewater Market identity and named cast;
- exact four-stage regional chain;
- exact Komodo acquisition and 600 Gold registration;
- optional contract goals/rewards;
- Earthloong discovery grammar;
- dungeon evidence interactions and first-clear rewards;
- autonomy evidence resolution and post-clear dialogue;
- shared/personal state boundaries;
- late-join/reconnect behavior.

Remaining unknowns are asset/coordinate validation gates only, not permission to redesign the content during coding.

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
