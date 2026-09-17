# Open-World RPG — Main Quest / Recurring Character Scene Package

> Status: **DESIGN CANON — main-route quest beats, scene ownership, route choice, rejoin points and finale handoff closed before source bootstrap**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Story spine: `WORLD_STORY_CANON.md`  
> Quest/state authority: `QUEST_WORLD_STATE.md`  
> Region authority: `REGIONS.md`, `R01_VERTICAL_SLICE.md`, `R02_CONTENT_BIBLE.md` through `R12_CONTENT_BIBLE.md`  
> Multiplayer: `PARTY_MULTIPLAYER.md`  
> Rule: this document does not duplicate regional quest chains. It closes the **cross-region main-route wrapper, recurring-character scenes, route-selection logic, evidence requirements, rejoin points and final handoff** that were previously still left as story-spine prose.

The main story exists to give the world direction without converting twelve authored regions into a mandatory checklist. Regional bibles own local quests, named regional casts, local rewards, aftermath and exact regional objective details. This file owns how those regional stories become one coherent main investigation.

The player is never treated as a chosen one. They become important because they repeatedly survive difficult field work, bring back evidence and are trusted with decisions that no single faction can settle safely on its own.

---

# 1. Main-route invariants

The launch main route is:

```text
R01 common opening
→ Act I: complete R02 OR R03 major evidence route
→ Act I synthesis / continental investigation opens
→ Act II: complete any TWO major evidence routes from R04–R07
→ Act II synthesis / three modern approaches become explicit
→ Act III-A: complete R08 OR R09 high-tier land investigation
→ Act III-B: complete R10 OR the authored late R11 investigation
→ partial reactivation consequence / cascade scene
→ R12 final truth route
→ Central Anchor crisis encounter
→ personal Restore / Release / Partition choice
→ neutral shared post-crisis world + personal epilogue state
```

Rules:

- physically entering a region never requires the main quest when geography permits it;
- the main quest may require evidence to interpret/use a specific ancient facility, but does not create invisible level walls around ordinary geography;
- completing additional non-required evidence regions remains worthwhile through their own regional rewards, extra dialogue, relationship flags and ending epilogue detail;
- no main-step asks the player to clear all twelve regions simply to make the campaign look longer;
- no region is demoted to filler because it is not required for one particular main-route permutation;
- regional dungeons/bosses remain replayable under their normal rules after their story clear;
- main-route state is personal/server-authoritative even when physical encounters are cooperative.

---

# 2. Recurring-character mapping

R01 already gives the opening cast concrete identities. These roles carry forward when the relevant character can travel plausibly.

## Mara Venn — Guild Pathfinder

Main-story function:

- practical field coordinator and the player's earliest recurring human link;
- translates continental leads into real routes, settlements and people rather than abstract lore chores;
- opens the early main investigation after R01;
- remains politically uncommitted for most of the story;
- repeatedly reminds the investigation that routes, workers and settlements are the reason the Anchor question matters.

Mara is not the player's permanent follower and does not require companion AI.

## Ilyan Voss — Anchor Scholar

Main-story function:

- interprets records, measurements and ancient interfaces;
- begins restoration-leaning because the first evidence genuinely shows that the old network prevented disasters;
- changes position only when evidence supports it;
- never knows facts the player has not plausibly recovered;
- exposition is short and paired with visible environmental/technical evidence.

## Sera Wren — Cartographer / Ranger

Main-story function:

- represents route knowledge, ecology and local adaptation;
- points out when maps, settlement behavior or wildlife contradict a purely technical Anchor explanation;
- becomes the most consistent local-autonomy voice without being written as anti-technology;
- can provide broad search areas and travel context without revealing undiscovered POIs for free.

## Daren Holt — Engineer / Smith bridge

Daren is the first concrete engineering perspective and may recur when the story needs a practical infrastructure reader.

Main-story function:

- explains machinery in terms of load, material, water, heat, lifts, gates and failure modes;
- gradually develops the regional-stewardship argument: useful infrastructure should survive, but remote authority/coupling must be bounded;
- never becomes a second magical historian.

If later asset/cultural direction requires a separate senior engineer for distant regions, Daren remains the player's first engineering anchor and the new character inherits the **role**, not his identity.

## Kest Arden — Rival Wanderer

Main-story function:

- follows a different route through the same open world;
- can surface at optional hunts, ruins or one of the non-required evidence regions;
- occasionally reaches an observation first or interprets it differently;
- sometimes cooperates in a scene/encounter but never becomes mandatory companion AI;
- demonstrates that competence exists outside the player party.

## Restoration Director — asset/name-gated recurring role

Gameplay/narrative identity is locked; final player-facing proper name and appearance remain coupled to final faction/NPC asset intake.

Function:

- coordinates large-scale restoration because delay has real human costs;
- is competent, persuasive and capable of producing genuine local improvements;
- becomes the main human opponent only when they accept system-wide reactivation risk before sufficient evidence exists;
- does not secretly desire collapse and is not converted into a cartoon villain;
- the finale does not require killing them in a humanoid HP-sponge duel.

---

# 3. Main quest state model

Suggested persistent personal state:

```text
main_act
main_step
main_route_flags[]
main_evidence_ids[]
main_required_evidence_count
main_rejoin_scene
main_scene_seen[]
main_scene_choice_flags[]
restoration_director_state
central_anchor_access_state
ending_choice
ending_completed
ending_epilogue_flags[]
```

Important semantics:

- regional completion flags remain owned by regional quest canon;
- main evidence is granted idempotently from specific authored regional completion/evidence events;
- receiving an evidence item/flag twice never duplicates main rewards;
- a player may possess optional evidence without that evidence being required for their current main route;
- main-route choices choose **which investigation supplies sufficient evidence**, not which regions continue to exist;
- no inventory item can be dropped/traded to bypass a main evidence prerequisite;
- main evidence behaves as Key Item/state, not backpack clutter.

---

# 4. Prologue / Act 0 — The Road and the Quarry

Primary authority: `R01_VERTICAL_SLICE.md`.

The cross-region main quest does not add a second tutorial layer over R01. It adopts the existing first-session flow.

## MQ-00 — arrival road incident

Player-facing purpose:

- establish the player as an ordinary traveler;
- teach movement/basic combat through a real problem;
- lead naturally to Alderford.

Rules:

- no long character-creation exposition dump;
- no prophecy language;
- no forced banker/smith/merchant errand chain;
- pre-first-shrine deaths use the already-canonical free opening recovery behavior.

Completion state:

```text
main_act = 0
main_step = ALDERFORD_REACHED
```

## MQ-01 — Alderford / first class and local problem

The player reaches Alderford, can use its services and chooses the first root class through the existing guild/class interaction.

Main objective then points toward the disturbed road/quarry situation. Optional contracts, Regalhart discovery, gathering, fishing, housing inspection and the first Trail Stag progression remain free to happen naturally.

No main objective requires visiting every settlement service before leaving town.

## MQ-02 — Roots Below Stone

This is the existing R01 quarry/dungeon sequence, not a newly duplicated quest.

Main-story trigger is the first eligible Earthloong clear and the associated deepest-chamber Anchor evidence.

On first eligible completion:

```text
main_evidence_ids += R01_QUARRY_RELAY
main_step = FIRST_ANCHOR_EVIDENCE
```

The evidence must be visibly tied to the physical site: machinery/record/measurement behavior, not merely a glowing quest item falling from the boss.

## MQ-03 — The first interpretation scene

Location preference: Alderford / Wayfarers' Hall or another already-authored safe service space, not a bespoke menu room.

Participants:

- Mara Venn;
- Ilyan Voss;
- Sera Wren where appropriate;
- Daren Holt where machinery damage/material evidence is relevant.

Scene purpose:

1. confirm the quarry evidence is old infrastructure rather than ordinary monster loot;
2. establish that similar markers point west/forestward and upward toward mountain observation routes;
3. avoid declaring the whole Anchor mystery solved;
4. offer two valid early investigation directions.

The player may ask concise clarification questions. Dialogue choices here record tone/interest only when useful; they do not secretly lock the ending.

Act transition:

```text
main_act = 1
main_step = CHOOSE_EARLY_LEAD
R02 main lead = available
R03 main lead = available
```

---

# 5. Act I — The Map Beneath the World

Required progression: **one** of R02 or R03 major evidence routes.

Both remain available before and after Act-I completion.

## Route A — R02 woodland relay evidence

Use the exact R02 regional chain and content-bible objectives.

Main evidence granted only at the authored point where the player has genuinely established that the old system stored/relayed observation or memory and communicated beyond the local ruin.

```text
main_evidence_ids += R02_MEMORY_RELAY
main_route_flags += ACT1_R02_COMPLETE
```

The main wrapper never changes R02 into an undead-corruption explanation for the entire forest.

## Route B — R03 mountain network evidence

Use the exact R03 regional chain and content-bible objectives.

Main evidence granted at the authored observatory/forge proof that the sites were one continental network.

```text
main_evidence_ids += R03_NETWORK_GEOMETRY
main_route_flags += ACT1_R03_COMPLETE
```

Current R03 encounter canon is Griffin + accepted highland/dungeon threats. **Basalt Wyvern is not an R03 main-route requirement.**

## MQ-10 — Act-I rejoin / Continental Lines

Unlock condition:

```text
ACT1_R02_COMPLETE OR ACT1_R03_COMPLETE
```

If both are complete before the player reports, the synthesis scene acknowledges both without giving duplicate main progression.

Preferred scene composition:

- Ilyan compares the new evidence with the quarry record;
- Mara turns the abstract finding into multiple reachable continental leads;
- Sera notes that several regions have lived with old infrastructure in different ways;
- Daren points out that a network can be useful locally and dangerous when one site can load another.

Result:

- the investigation is now continental;
- R04–R07 major evidence routes are all valid candidates;
- no single NPC decides which two the player must do.

Act transition:

```text
main_act = 2
main_step = COLLECT_TWO_REGIONAL_EVIDENCE
main_required_evidence_count = 2
```

Main milestone reward follows the global main-milestone EXP/Class-XP/Gold budget. Do not duplicate a second full dungeon reward on top of the regional clear.

---

# 6. Act II — Regional Evidence

Eligible major evidence regions: **R04, R05, R06, R07**.

Required for main progression: **any two distinct evidence packages**.

The purpose is not to collect two differently colored keys. Each package must demonstrate a materially different consequence of network use, failure or adaptation.

## R04 evidence role

Canonical thesis:

- bounded restoration visibly helps ordinary people/routes;
- old infrastructure can still be worth saving.

Main wrapper records the regional result only after the authored R04 chain reaches that conclusion through play.

```text
main_evidence_ids += R04_BOUNDED_RESTORATION
```

## R05 evidence role

Canonical thesis:

- a modern ecology/society successfully adapted after old regulation faded;
- restoration is not automatically a return to a better prior state.

```text
main_evidence_ids += R05_ADAPTATION
```

## R06 evidence role

Canonical thesis:

- useful local control can remain while dangerous remote authority is severed;
- regional stewardship is technically plausible rather than only political rhetoric.

```text
main_evidence_ids += R06_LOCAL_STEWARDSHIP
```

## R07 evidence role

Canonical thesis:

- centralized optimization under scarcity can sacrifice peripheral communities even without malicious intent.

```text
main_evidence_ids += R07_ALLOCATION_COST
```

## Evidence counting rule

```text
act2_required_count = count(distinct eligible R04..R07 main evidence IDs)
advance when act2_required_count >= 2
```

Optional third/fourth evidence remains recordable later and influences dialogue/epilogue context, but does not pay the main synthesis reward again.

## MQ-20 — The Three Answers

Unlock condition: any two distinct Act-II evidence packages.

This is the first scene where the three long-term approaches become explicit in understandable language:

- coordinated continental restoration;
- controlled release/local autonomy;
- regional partition/stewardship.

Presentation rule:

- no ideology is introduced as obviously correct;
- characters argue from concrete evidence the player has actually seen;
- if the player completed specific optional regions, dialogue cites those cases;
- choices let the player question or tentatively sympathize, but **do not lock the finale choice**.

The Restoration Director enters directly or through a live operational scene only after the player understands why a restoration organization has legitimate support.

Act transition:

```text
main_act = 3
main_step = HIGH_TIER_LAND_EVIDENCE
```

---

# 7. Act III — Who Gets to Decide

Act III has two required investigation phases. They may be interleaved with optional regional content, but the late cascade scene requires both classes of evidence.

## Act III-A — one high-tier land investigation

Choose **R08 or R09**.

### R08 evidence role

- technically successful old-style stabilization can suppress a valuable modern magical ecology.

```text
main_evidence_ids += R08_SUPPRESSED_EMERGENCE
```

### R09 evidence role

- people can build redundant roads/signals/depots that replace some centralized network functions.

```text
main_evidence_ids += R09_HUMAN_REDUNDANCY
```

Completion of either route is enough to satisfy the high-tier land requirement.

## MQ-30 — Reactivation Notice

After one R08/R09 evidence package, the player learns that the Restoration Director is beginning coordinated reactivation in selected sites.

The scene must show why waiting is also dangerous:

- a route, settlement service, containment problem or logistics chain is genuinely improved by restoration;
- the Director's decision is not irrational;
- Ilyan may initially understand/support part of the rationale;
- Mara focuses on the speed/risk tradeoff rather than ideology;
- Sera/Daren challenge the assumption that success at one site proves safe continental coupling.

No combat with the Director occurs here.

## Act III-B — one late consequence investigation

Choose either:

- **R10** authored late investigation; or
- the **appropriate late R11 investigation layer** defined by R11 regional canon.

R10 and R11 remain fully playable independently of which one supplies the required main evidence.

### R10 evidence role

- restoration works locally;
- remote load balancing produces a fast cascade elsewhere.

```text
main_evidence_ids += R10_FAST_CASCADE
```

### R11 evidence role

- a successful surface improvement creates a delayed distant/deep consequence.

```text
main_evidence_ids += R11_DELAYED_DEEP_COST
```

## MQ-31 — The Cascade

Unlock condition:

```text
(one of R08/R09 evidence)
AND
(one of R10/R11 late evidence)
```

This is the decisive Act-III rejoin scene/encounter package.

Required beats:

1. an already-active restoration action produces a real beneficial result;
2. measurements then show harmful coupling outside the immediate site;
3. the player experiences or directly verifies the consequence rather than hearing only a report;
4. the Restoration Director refuses to abandon all restoration but must acknowledge the network is more coupled than expected;
5. the evidence points to the Central Anchor as the only place where the coupling history/controls can be resolved safely.

The Director may oppose the player's attempt to delay/enter the Central Anchor, but the conflict remains about authority and risk.

Act transition:

```text
main_act = 4
main_step = CENTRAL_ANCHOR_ROUTE_OPEN
central_anchor_access_state = EVIDENCE_READY
```

---

# 8. Act IV — R12 / Where the Lines Meet

R12 geography remains explorable before Act IV. Only the deepest Central Anchor main route requires the investigation state above.

## MQ-40 — Enter the anomaly zone

The player uses R12's established traversal/anomaly grammar and regional story rather than walking through a separate linear campaign tunnel.

Main-specific goals:

- verify conflicting historical records against physical system evidence;
- establish that over-coupling was a historical administrative/engineering decision, not the natural default state of the whole continent;
- locate the Central Anchor access path;
- preserve optional R12 exploration, Terradragon and side content as separate content rather than mandatory finale padding.

## MQ-41 — Central Anchor truth sequence

Required revelation order:

1. the network genuinely prevented destructive chain reactions and supported civilization;
2. regional dependence increased over generations;
3. central authorities repeatedly coupled formerly separable systems for efficiency/control;
4. this improved coordination while creating catastrophic shared failure modes;
5. modern societies now differ enough that returning to one old operating model would impose real costs.

This sequence is delivered through environment, machinery behavior, short records and character interpretation. No single lore terminal may carry the entire truth.

## MQ-42 — Immediate crisis encounter

Exact final guardian name/model/anatomy/attack sheet remains an external-asset gate.

Locked encounter purpose:

- stop the immediate Central Anchor cascade;
- mechanically express coupled systems, rerouting, overload, isolated failure and controlled separation;
- use readable telegraphs and the project's normal combat language rather than becoming a puzzle-only finale;
- permit cooperative combat under normal server authority;
- avoid a generic giant HP bar with no relationship to the Anchor system.

The Restoration Director can participate in the scene as ally/opponent/interfering authority depending on authored staging, but killing the Director is not required to make the philosophical choice possible.

On first eligible clear:

```text
main_step = FINAL_CHOICE_PENDING
ending_completed = false
```

No ending reward is granted yet.

## MQ-43 — Restore / Release / Partition

After the immediate crisis is contained, each eligible player independently chooses:

- Restore;
- Release;
- Partition.

Rules:

- choice is explicit, never inferred from earlier dialogue;
- earlier sympathy flags alter framing/response lines only;
- no option is labelled good/bad/true/canon;
- multiplayer choice is personal;
- disconnect before server commit returns the player to `FINAL_CHOICE_PENDING` without consuming the choice;
- successful commit is idempotent and cannot duplicate rewards.

Shared world result:

```text
Central Anchor immediate crisis = resolved
final combat space = safe/replay-compatible
world geometry = neutral shared post-crisis state
```

Personal result:

```text
ending_choice = RESTORE | RELEASE | PARTITION
ending_completed = true
ending_epilogue_flags = derived from personal regional outcomes/evidence
```

---

# 9. Optional evidence and epilogue use

Non-required evidence is not fake completionism.

Optional regional completion may alter:

- final pre-choice dialogue examples;
- which communities/people appear in the epilogue;
- short journal summaries;
- recurring-character responses;
- cosmetic/title presentation where accepted external assets exist;
- postgame dialogue or selected event pools.

Optional evidence may **not**:

- secretly unlock the only good ending;
- increase ending reward power enough to force completionism;
- make an unvisited region disappear from the world;
- turn every region into a hidden mandatory prerequisite.

---

# 10. Scene design / dialogue contract

Main scenes should normally last long enough to communicate one decision or revelation, not become cutscene walls.

Rules:

- ordinary information scene target: roughly 30–90 seconds when voiced/read at normal pace;
- major act-rejoin scene may run longer if movement/environment interaction breaks up exposition;
- player can advance/skip non-interactive dialogue under `QUEST_WORLD_STATE.md` rules;
- critical state changes are committed independently of client subtitle timing;
- dialogue choices appear when they express curiosity, stance or a real local decision; do not force three fake response buttons after every paragraph;
- no quest says `collect 2/4 philosophies`; the player sees real regional objectives and understandable investigation leads;
- journals summarize recovered evidence in world language, never internal `R04_BOUNDED_RESTORATION` IDs;
- map/journal distinguishes an exact destination from a broad search region.

---

# 11. Rejoin / sequence-break rules

Open-world freedom creates predictable edge cases; they are designed here rather than improvised later.

## Region completed before main lead

If a player finishes an eligible regional story before the main quest formally points there:

- regional completion remains valid;
- when the main investigation later reaches that act, already-owned eligible evidence is recognized immediately;
- the player gets the appropriate synthesis/rejoin scene, not a demand to replay the whole region;
- main rewards are granted once only.

## Player has enough evidence before report scene

The next rejoin scene becomes available at a valid safe/world interaction point. The game does not force immediate teleport or interrupt exploration with a long cutscene.

## Player enters R12 early

- ordinary R12 exploration/content works under its own rules;
- the deepest Central Anchor main interaction remains unavailable until `central_anchor_access_state = EVIDENCE_READY`;
- the block is represented by believable unreadable/inactive system state, not an invisible wall around the whole region.

## Regional boss already defeated on repeat cycle

Main evidence depends on the authored personal regional state/first eligible story condition, not whichever repeat boss kill happened most recently.

---

# 12. Multiplayer / split-party rules

Main story remains valid when players travel together, split up or are at different stages.

- each player stores personal main-act/main-evidence/ending state;
- a shared regional encounter may award evidence to every eligible participant whose personal prerequisites permit that evidence;
- a player without prerequisites may still help friends fight but does not skip their own missing investigation chain;
- party leader/host status never owns main progress for other players;
- physical shared-world route changes may be visible to everyone when the regional canon says the change is genuinely shared;
- seeing an opened bridge/door does not automatically grant the opener's personal story evidence;
- late joiners evaluate current shared world facts plus their personal quest state through `QUEST_WORLD_STATE.md` reconciliation rules;
- reward transactions are idempotent;
- support contribution counts for eligible encounter participation;
- AFK proximity does not.

Different players can make different final ending choices after the same cooperative finale.

---

# 13. Failure / disconnect / recovery

- ordinary death uses the normal checkpoint/death-penalty rules; main scenes do not introduce a separate punishment economy;
- failing a story encounter resets only the encounter-owned transient state necessary for a clean retry;
- committed dialogue/evidence flags are not rolled back because a later fight failed;
- disconnect during a scene resumes from the last committed state, not from a duplicate reward transaction;
- disconnect during the final choice returns to pending choice unless the server already committed the chosen ending;
- no main quest can be permanently failed because an NPC despawned, a client closed, another player clicked first or the general backpack was full.

---

# 14. Reward contract

Regional quests/dungeons/bosses keep their regional rewards. Main-route wrapper scenes do not duplicate a second full reward package simply for reporting the same clear.

Use the global EXP/Class-XP/Gold economy:

- act-rejoin/synthesis milestones may grant a normal **main/regional milestone** reward;
- combat/dungeon rewards remain owned by the encounter that produced them;
- optional extra evidence mostly pays through its region's own rewards plus narrative/epilogue value;
- finale gives one authored completion package, not three different power tiers based on ending philosophy;
- no ending-specific best-in-slot weapon makes one philosophy the mechanical best choice;
- if Lv80 prevents normal EXP gain, finale reward value shifts to Gold/authored equipment/cosmetic/collection outcomes rather than hidden overflow levels.

---

# 15. Journal / UI structure

The Quest Journal should present the main route as a readable investigation, not an act-number checklist.

Required hierarchy:

```text
Main Investigation
  current objective
  current known leads
  evidence summary
  completed major discoveries

Regional Stories
  per-region authored chains

Contracts / Optional
  normal regional work
```

The player-facing journal does not expose `Act II requires 2/4` as a naked system counter. Instead it can say that **two independent regional cases are needed before the group can draw a reliable conclusion**, then list currently known leads.

After enough evidence exists, the journal changes to the rejoin/report objective rather than continuing to pressure the player to clear every remaining candidate region.

---

# 16. Asset / presentation gates

This document closes main-quest logic. It does not waive external-first presentation.

Still gated before visible implementation where applicable:

- Restoration Director final proper name/face/outfit/faction visual language;
- recurring NPC final travel/outfit variants beyond already accepted R01 presentation;
- Anchor machinery/interface/facility visual language;
- R12 systemic final guardian exact model/anatomy/weak points/signature material;
- scene-specific animations, gestures, props, VFX and SFX/BGM;
- exact cinematic camera work if a scene requires more than ordinary gameplay framing.

Failure to find a good asset means the visible scene remains blocked; it does not authorize a vanilla villager, glowing cube or static mannequin placeholder as final presentation.

---

# 17. Main-route acceptance matrix

Before implementation is called design-faithful, verify at minimum:

1. R01 always produces the first Anchor evidence without making Regalhart mandatory;
2. Act I advances from R02 **or** R03 and recognizes both if both are already complete;
3. Act II requires two distinct R04–R07 evidence packages, not the same route twice;
4. completing three/four Act-II regions does not duplicate the synthesis reward;
5. Act III requires one R08/R09 investigation plus one R10/R11 late-consequence investigation;
6. unchosen evidence regions remain fully playable before and after main progression;
7. early completion of an eligible region is recognized later without forced replay;
8. R12 geography is explorable early while the deepest Central Anchor interaction respects personal evidence state;
9. multiplayer participants can hold different main stages on one world without host overwrite;
10. support players receive legitimate encounter participation credit without last-hit requirements;
11. final choice is personal and idempotent;
12. earlier dialogue sympathy never silently locks Restore/Release/Partition;
13. no ending grants a mechanically superior reward tier;
14. optional evidence changes context/epilogue without becoming a hidden mandatory checklist;
15. no player-facing internal IDs, act labels, asset gates or development terminology leak into the game.

---

# 18. What this closes

Closed before source bootstrap:

- exact main-route region requirement structure;
- Act 0 → Act IV transition logic;
- Act-I, Act-II and Act-III rejoin points;
- evidence counting and already-completed-region reconciliation;
- recurring R01 character functions across the main story;
- Restoration Director narrative function;
- sequence-break behavior;
- split-party/main-progress ownership;
- final-choice commit/reconnect behavior;
- wrapper reward ownership;
- journal hierarchy and player-facing investigation presentation;
- explicit boundary between regional quests and the cross-region main quest.

Still separate production/closure work:

- **gameplay-source-bootstrap blocker:** exact external presentation bindings/provenance for the gated visible content above;
- **gameplay-source-bootstrap blocker:** actual Azari coordinates, scene locations, sightlines and travel-time validation;
- **gameplay-source-bootstrap blocker:** stale-document cleanup where older files still describe already-superseded alternatives;
- **not a gameplay-source-bootstrap blocker:** final branding/title string; it must be closed before player-facing branded release/presentation, while the internal production slug may remain during gameplay bootstrap.

`MAIN QUEST SCENE PACKAGE = DESIGN CLOSED` does **not** mean the quest has been implemented, visually accepted or playtested.
