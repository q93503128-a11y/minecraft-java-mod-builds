# Open-World RPG — Quest / Dialogue / World-State / Multiplayer Progression Canon

> Status: **DESIGN CANON — quest state, objective credit, dialogue choices, dynamic events, shared-world boundaries and co-op progression locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Party/co-op refinement: `PARTY_MULTIPLAYER.md`  
> Opening: `R01_VERTICAL_SLICE.md`  
> Combat/rewards: `COMBAT_BALANCE.md`, `LOOT_ECONOMY.md`  
> Gathering/camp/housing: `GATHERING_FISHING_CAMP_HOUSING.md`  
> UI: `UI_DIRECTION.md`  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins. `PARTY_MULTIPLAYER.md` is the later explicit authority for combat-reward participation thresholds: **one valid damage or support action is enough**. `R01_PLAYER_TEXT_SPEC.md` owns exact R01 board/HUD/journal wording without changing the state/credit rules in this file.

This document closes the rules that should not be invented while implementing quests or multiplayer saves.

The goal is not to make a Minecraft MMO quest log. The goal is to let players discover and complete meaningful regional content together or separately without kill stealing, host-owned progression, duplicated rewards, forced NPC errand chains or a permanently crowded HUD.

---

# 1. External precedents and adoption boundaries

## 1.1 Guild Wars 2 — participation instead of kill ownership

Useful precedent:

- dynamic events can be discovered by entering the relevant area rather than accepting every activity from an NPC first;
- multiple players can contribute without a traditional first-tag/kill-steal race;
- rewards are personal to qualifying participants;
- events may scale around active participation;
- story progression remains distinct from open-world events.

Project adoption:

- authored regional events are world-shared encounters with personal participation rewards;
- combat/support/objective contribution can all establish eligibility;
- last hit never owns an event or boss reward;
- discovery/event play can begin in the world rather than through a board click.

Not adopted:

- a dense MMO event chain covering every square of the map;
- universal downscaling of every encounter to whoever enters;
- map-completion checklist pressure.

References:
- `https://wiki.guildwars2.com/wiki/Event`
- `https://wiki.guildwars2.com/wiki/Personal_story`

## 1.2 Wynncraft — Minecraft can support authored world events without vanilla quest structure

Useful precedent:

- large authored Minecraft RPG worlds can combine quests, discoveries, dungeons and world events;
- world events can produce a self-contained encounter/reward loop rather than acting only as another NPC task.

Project adoption:

- selected regional events exist as authored encounter controllers;
- event rewards remain project-owned and personal;
- discoveries can expose content/journal information without becoming mandatory quests.

Not adopted:

- donor progression, currencies, UI or content names.

Reference: `https://wynncraft.wiki.gg/wiki/World_Events`

---

# 2. State ownership model

Every quest/world record belongs to exactly one authority class.

## 2.1 `PLAYER_PROGRESS`

Personal, persistent, server-authoritative state.

Examples:

- main/regional quest step;
- contract completion;
- clue/discovery flags;
- first boss/dungeon clear;
- personal reward claim;
- class/mount/service unlock;
- dialogue choice / personal relationship consequence;
- personal map markers;
- personal resource/fishing state.

One player cannot advance or overwrite another player's `PLAYER_PROGRESS` merely because they are party leader or host.

## 2.2 `WORLD_PERSISTENT`

Shared server-world facts that physically need one truth for everyone.

Use sparingly.

Valid examples:

- an authored bridge/route permanently repaired for this world;
- a settlement-wide structure or service that deliberately changes for every player;
- a one-time world milestone whose shared physical result is the intended game.

Do **not** put personal narrative choices, first-clear rewards, quest completion or individual unlocks here merely because Minecraft has one physical world.

A shared permanent world change must remain safe for a player who joins later.

## 2.3 `ENCOUNTER_STATE`

Shared but resettable runtime/save state.

Examples:

- field boss alive/defeated/cooldown;
- dynamic event phase;
- dungeon run encounter phase;
- temporary shortcut/opened gate for the current dungeon cycle;
- spawned objective actors/props.

Encounter reset never deletes personal first-clear/completion history.

## 2.4 `LOCAL_PRESENTATION`

Client presentation only.

Examples:

- which journal entry is expanded;
- pinned quest HUD entries;
- dialogue text reveal speed;
- map filter state;
- local interaction highlight.

No Gold/item/EXP/quest success is awarded from client-only state.

---

# 3. Quest/content categories

Use a small readable set.

| Category | Purpose | Acceptance | Repeatability |
|---|---|---|---|
| Main | major world/region guidance | automatic/current story lead or authored trigger | normally once per player |
| Regional | region identity / meaningful local chain | world discovery, NPC or regional state | normally once per player |
| Contract | concise optional objective with useful reward | board/NPC/discovery | authored one-shot or explicitly repeatable |
| Discovery | clues, POIs, lore-bearing findings, boss traces | automatic on meaningful discovery | recorded once; content itself may remain revisit-able |
| World Event | shared physical event | proximity/participation; no pre-accept required | resettable/cooldown-based |

Do not add separate Daily/Weekly/Bounty currency systems at baseline.

A future repeatable contract is allowed only when it gives a real reason to revisit a route/creature/material. It must not become a daily-login chore.

---

# 4. Acceptance, tracking and HUD density

## 4.1 Main story

- only one current main-story lead is foregrounded at a time;
- reaching a legitimate trigger may create/advance the main entry automatically;
- the player is never required to open the journal merely to make a discovered main event start counting.

## 4.2 Regional quests / contracts

- a board/NPC may offer a contract that the player explicitly accepts;
- a discovery-triggered regional objective may appear as an offer rather than silently filling the quest log;
- accepting an objective never teleports the player or forces party members to accept it.

## 4.3 Discoveries

- meaningful POI/clue discoveries record automatically;
- they do not occupy the normal active-contract limit;
- a discovery becomes a quest only if it develops into a real multi-step objective.

## 4.4 HUD tracking

The journal may hold many known entries, but the normal free-roam HUD shows at most:

```text
1 Main objective
+ up to 2 manually pinned optional objectives
```

World events may temporarily show one compact event tracker while the player is actively participating.

This preserves low foreground quest density and prevents an MMO checklist wall.

The player can pin/unpin eligible objectives freely. Pin state is presentation, not progression state.

---

# 5. Objective-credit rules

The server awards objective progress from explicit authored actions. There is no generic `nearby party member did something, therefore everyone progressed` rule.

## 5.1 Combat objectives — one valid action qualifies

For a kill/elite/miniboss/boss combat objective, each player qualifies after **one valid encounter-linked action**.

Any one of these is sufficient:

- one server-accepted damaging hit against the objective enemy;
- one valid poise/stagger/control/debuff action against it;
- one heal that restores actual missing HP to an ally actively engaged with it;
- one valid barrier/protection/support buff applied to an ally actively engaged with it;
- one successful revive during the encounter;
- one authored encounter-support/objective interaction explicitly marked as combat participation.

Rules:

- last hit has no special ownership;
- there is **no damage-share percentage threshold**;
- there is **no minimum contribution score after the first valid action**;
- one small legitimate hit is enough even on an elite or boss;
- one legitimate heal/support action is enough for a healer/support build;
- arriving near the end is fine if the player actually performs one valid action before resolution;
- zero-action proximity is not enough;
- no-op abuse such as pure healing a full-HP ally solely to manufacture participation does not count as a valid heal, while a real support buff/protection effect may count on valid application as defined in `PARTY_MULTIPLAYER.md`;
- once granted, participation eligibility persists for the remainder of that encounter instance even if the player is later Downed.

This intentionally favors frictionless private co-op over contribution policing.

## 5.2 Gathering / collection objectives

- personal resource-node collection counts only for the player who gathers that personal node/reward;
- party members can gather their own copy from the same physical node;
- tradeable items may satisfy a delivery requirement only if the quest explicitly allows acquired/traded items;
- quest-critical personal evidence items use Key Item state and cannot be stolen/dropped/sold.

## 5.3 Investigation / interaction objectives

- clue, examine, talk and personal switch interactions record per player;
- a nearby party member does not silently mark another player's clue as seen;
- if an interaction changes a shared physical encounter object, other players can still perform/receive their personal logical interaction through a retained prompt/state representation.

## 5.4 Traversal / location objectives

Location discovery is personal and server-validated by entering the authored volume/condition.

One player revealing a POI does not automatically reveal it on every other player's map.

---

# 6. Party cooperation without host-owned progression

Players are allowed to be on different steps of the same quest.

## 6.1 Matching step

If players have the same combat quest step active:

- one shared physical kill/event can credit every player who performs at least one valid contribution under §5.1;
- personal interactions/gathering still record separately where appropriate;
- party leader has no extra progression authority.

Formal parties support **2–4 players**, with 4 as the maximum rather than a required size. Solo remains fully supported. Exact party UX/reward rules live in `PARTY_MULTIPLAYER.md`.

## 6.2 Different steps

If players are on different steps:

- each receives credit only for actions relevant to their own current step;
- a later-step player's presence cannot skip prerequisites for the earlier player;
- an earlier player may still help fight an encounter and receive normal eligible combat rewards without being granted later quest completion.

## 6.3 Helping without accepting

A player may help another player's quest encounter without accepting that quest.

They may receive normal combat/event loot/EXP if eligible, but they do not receive:

- the quest completion;
- one-time quest reward;
- story choice;
- progression unlock tied to that quest.

This keeps co-op permissive without turning party membership into automatic story completion.

## 6.4 Catch-up

Do not build a universal `sync everyone to leader` button at baseline.

Instead:

- short regional chains use checkpoints that are easy to catch up independently;
- shared boss/dungeon content remains replayable/helpable;
- previously completed helpers can assist without duplicating one-time rewards;
- if later playtesting finds a specific long chain painful to catch up, solve that chain deliberately rather than introducing a global skip system.

---

# 7. Dialogue interaction

Dialogue supports world identity and choices, but normal play should not become stationary text-box reading.

## 7.1 Presentation

Baseline dialogue rules:

- use the Lucifer-family dialogue/service visual grammar;
- important NPC portrait/model/name/context is visually clear;
- ordinary interaction is skippable/advanceable immediately;
- the player can exit non-critical dialogue without losing progress;
- no long typewriter delay that blocks fast readers;
- no mandatory full-screen fade for routine merchant/contract dialogue;
- camera lock/cinematic framing is reserved for short major moments where it materially improves presentation.

NPCs still exist physically in the world; dialogue is not a disembodied menu kiosk.

## 7.2 Choice types

Use choices only when they create a consequence or useful role-play expression.

Allowed baseline consequence types:

- different dialogue/context;
- modest relationship/reputation change for a meaningful faction;
- alternate but roughly equivalent reward form;
- information/clue revealed through a different route;
- later NPC availability/response;
- personal quest branch that rejoins before content scope explodes.

Avoid fake three-button choices where every answer immediately produces the same line.

## 7.3 Irreversible choices

A choice that permanently changes the player's personal progression must:

- explain the consequence before confirmation;
- use a deliberate confirmation interaction;
- never be decided by another party member;
- never rely on a 3-second timed response.

Shared `WORLD_PERSISTENT` choices are rare and must not be placed in ordinary personal dialogue. One friend should not permanently rewrite the whole server world for everyone through a casual NPC response.

---

# 8. Turn-in and reward friction

Do not force a return trip only because old MMO quest design expects `turn in to NPC`.

## Auto-complete / remote-complete is valid when:

- the success is self-evident and the NPC does not need to react physically;
- the reward is numeric/personal and no world-service interaction is being introduced;
- returning would only add dead travel.

## Physical return is preferred when:

- the return itself closes a story beat;
- a service/recipe/class/mount/property is unlocked through the encounter;
- the player needs to choose a reward;
- the NPC/world visibly changes;
- the route intentionally returns the player to a settlement service loop.

One-time rewards are server-authoritative and idempotent. Reopening dialogue, relogging or replaying an encounter cannot duplicate them.

No quest-specific token currency is added merely to hand out rewards.

---

# 9. Failure / abandonment / missable-content policy

## Main / Regional

- cannot be permanently failed by ordinary combat defeat;
- failure resets the relevant encounter/checkpoint, not the whole chain;
- critical NPC death/despawn cannot permanently brick progress;
- key state/items cannot be sold/dropped/lost.

## Contracts

- may be abandoned and reaccepted unless explicitly authored as a timed world event;
- abandonment clears only that player's active contract state, not global world objects;
- reaccepting cannot duplicate already-claimed one-time rewards.

## World events

- can succeed or fail as a shared encounter;
- failure is a world outcome for that event cycle, not a character punishment;
- ordinary failure does not delete Gold/EXP beyond whatever normal player deaths occurred;
- event can return on its authored cooldown/condition.

Do not use real-world daily expiration for normal story/contract content.

---

# 10. Dynamic world events

Dynamic events are **discovered content**, not a second quest-board layer.

## 10.1 Activation

An event controller may activate from:

- region/time/weather condition;
- player proximity;
- authored world-state condition;
- prior event outcome;
- rare encounter roll with cooldown.

Events do not require every nearby player to click `Accept`.

## 10.2 Participation

Combat participation uses the same generous one-action rule as §5.1 and `PARTY_MULTIPLAYER.md`:

- one legitimate hit; or
- one valid heal/protection/buff/control/revive/support interaction

is enough to qualify for the combat/event reward layer for that encounter instance.

For non-combat events, one successful authored objective action such as a repair/carry/rescue/interaction may establish participation where the event defines it.

Zero-action proximity gives no reward. A reward-eligible player does not lose eligibility merely for becoming inactive later in the same short encounter, but **boss/event scaling** may stop counting someone who is no longer actually engaged so one tagged-and-departed player cannot keep the encounter inflated.

There is no hidden participation score used to reduce personal reward after qualification.

## 10.3 Scaling

Event scaling may adjust:

- number/composition of ordinary enemies;
- elite presence;
- objective amount;
- boss HP/poise using existing multiplayer boss rules.

Do **not** increase outgoing boss damage just because more players joined; this remains consistent with the existing combat multiplayer contract.

Reward eligibility and scaling engagement are separate states: a player may retain their earned reward eligibility while no longer counting toward live scaling after genuinely leaving the encounter.

## 10.4 Rewards

- personal to each eligible participant;
- combat EXP/Class XP is not split between participants;
- no shared floor-loot race;
- first-discovery/first-clear bonuses remain personal one-time state;
- repeat rewards use the event's repeat table;
- failure may give no event-completion reward or a deliberately smaller participation reward, but cannot be exploited by intentional failure farming.

---

# 11. World-state changes and late joiners

Minecraft presents one physical world, so permanent story phasing is deliberately limited.

## 11.1 Prefer personal logical consequences

Where possible, a personal quest choice changes:

- dialogue;
- journal;
- available personal service/reward;
- map knowledge;
- personal NPC interaction options;
- future personal quest branch.

It does **not** require two players standing in the same plaza to see mutually incompatible buildings.

## 11.2 Shared physical changes

Use a shared permanent change only when:

- it is beneficial/readable for all players;
- late joiners are not softlocked;
- no personal reward is accidentally granted to everyone;
- the change has one sensible world truth.

Examples:

- repaired public bridge;
- opened public route;
- settlement-wide authored upgrade after a true server milestone.

A late joiner encountering the changed world receives the appropriate context/journal information and can still complete any personal prerequisite/reward flow that remains relevant.

## 11.3 No per-player duplicate world geometry baseline

Do not build a heavy phasing system that renders whole alternative towns per player merely to support dialogue branches.

If a future major story requires mutually exclusive large world geometry, design that specific content around an instance or another explicit technical solution instead of making every normal quest pay the cost.

---

# 12. Dungeon / boss progression

Physical encounters may be shared/replayable; progression rewards are personal.

For a dungeon/boss:

- encounter controller owns current shared run state;
- every eligible player has separate `first_clear`, reward-claim and quest-step state;
- a helper who already cleared may fight again without receiving duplicate deterministic first-clear rewards;
- **one valid damage/support contribution is sufficient for encounter reward eligibility even if the player joined late**;
- first-clear choice rewards are committed atomically before the UI is considered complete;
- disconnect/reopen cannot create a second choice reward.

Run-scoped shortcut state can reset with the encounter cycle without deleting the personal first-clear record.

---

# 13. R01 exact quest-state mapping

This section closes the first-region implementation rules already outlined by `R01_VERTICAL_SLICE.md`.

## 13.1 `Dust on the Quarry Road`

State: personal Main objective with shared physical world actors.

The local objective has five authored useful-action categories:

1. recover one lost cargo interaction;
2. participate in clearing the authored Meadow Viper threat under the one-action combat-credit rule;
3. inspect the damaged wagon/road marker;
4. personally gather one relevant nearby R01 node;
5. participate in the repeatable **`Roadside Trouble`** world event defined in `R01_CONTENT_BIBLE.md` if it is active.

Completion requires:

```text
3 distinct categories of 5
```

No category can be spammed repeatedly for extra quest progress.

If `Roadside Trouble` is not active, the other four categories are sufficient. The event's activation, participation, reward, abandon and repeat cycle are owned by `R01_CONTENT_BIBLE.md`; this quest file does not invent a second event definition.

Reward stays canonical:

```text
EXP ~35% current next-Lv requirement
Gold 90
Class XP ~25% current Class Rank requirement
```

Completion is remote/self-evident; no mandatory walk back to the guild just to claim the numeric reward.

## 13.2 `Riverbank Remedies`

State: personal optional Contract.

Objective:

```text
gather 3 Healing Herb units from valid R01 herb nodes
```

- personal nodes mean party members do not compete;
- traded/pre-owned herb does not retroactively satisfy the first tutorial contract: this contract specifically teaches gathering;
- after this one introductory contract, later normal ingredient-delivery contracts may explicitly allow inventory-owned/traded materials where appropriate.

Reward stays:

```text
EXP ~20%
Gold 60
Class XP ~15%
```

No forced potion-crafting follow-up.

## 13.3 `Signs in the Meadow`

State: personal optional Contract / discovery bridge.

Objective:

- investigate the damaged cart site;
- inspect one additional authored large-creature territory sign nearby.

Completion records the region's large-creature warning knowledge and can seed later Steelboar/Regalhart discovery context.

Reward remains:

```text
EXP ~20%
Gold 70
Class XP ~15%
```

## 13.4 `A Stag at the Ford`

State: personal permanent Trail Stag unlock using shared physical event actors.

- nearby players can cooperate on threats;
- each player must complete/receive the personal calming/registration interaction;
- each eligible incomplete participant receives their own permanent mount unlock;
- already-completed helpers receive normal encounter rewards only and cannot duplicate the first unlock reward;
- an incomplete player can trigger the authored event again later if they missed the first shared occurrence.

## 13.5 `Roots Below Stone`

State: personal Main objective + shared dungeon discovery/encounter.

- quarry entrance discovery is personal;
- entering/clearing the dungeon uses shared physical encounter state;
- Earthloong first-clear and completion reward choice are personal and idempotent;
- no hard Lv8 gate.

## 13.6 `Steel in the Grass`

State: personal Contract revealed by Steelboar sighting/evidence.

Kill credit uses the global one-action rule: one valid hit or one valid encounter-linked heal/protection/support/control action is sufficient. Last hit has no special ownership.

## 13.7 `The Crowned Trail`

State: personal Discovery/hunt entry.

Existing rule remains:

```text
find any 2 of 3 authored Regalhart clue types
→ reveal broad search region + journal hunt entry
```

Finding/fighting Regalhart before the clues remains legal.

## 13.8 Earthloong / first dungeon reward

Shared boss; personal progression.

Each eligible first-clear player independently receives:

- Earthloong Scale x2;
- guaranteed Superior+ boss gear;
- direct Mythic-roll resolution;
- one deterministic choice from Ironroot Longsword / Riverthorn Bow / Lumenwood Staff;
- first-clear completion EXP / Class XP / Gold.

Choice UI timing does not hold the whole server/boss controller hostage. The server stores a pending personal reward-choice record until that player successfully selects/receives one option.

---

# 14. Dialogue / quest save transaction rules

Important progress changes use idempotent server transactions.

At minimum record stable identifiers such as:

```text
quest_id
step_id
objective_progress
completed_steps
choice_flags
reward_claim_ids
first_clear_ids
discovery_flags
updated_at_world_tick
```

Rules:

- advance quest step + mark one-time reward claim atomically where they belong to the same completion;
- a retry after disconnect must see the committed reward ID and not grant again;
- do not use client toast appearance as proof that a reward was committed;
- save personal progression at meaningful mutations rather than only graceful logout;
- malformed/missing optional presentation data must not silently erase canonical progression;
- save migration/versioning begins before the first preserved long-term test world.

---

# 15. Disconnect / reconnect behavior

## Personal quest progress

Already committed objective/step progress survives reconnect.

## Active event

- once a valid encounter participation action is recorded, that encounter's reward eligibility remains recorded through a brief disconnect where the encounter supports reconnect-safe reward transactions;
- reconnect does not require a second hit/support action merely to re-earn already-recorded eligibility;
- already committed event reward remains committed;
- ordinary common-enemy rewards are not queued indefinitely for someone who is offline at resolution.

## Dialogue / reward choice

- closing/disconnecting during ordinary dialogue simply reopens at the current stable step;
- if a deterministic reward choice has been earned but not chosen, it remains a pending personal claim;
- disconnect cannot reroll the offered deterministic set or duplicate the reward.

## Dungeon

- personal first-clear/quest state survives;
- current shared encounter may reset according to dungeon controller rules if nobody remains;
- reconnect-safe major encounter rewards may resolve for a player whose one-action eligibility was already recorded before disconnect;
- reconnect never duplicates completion or rewards.

---

# 16. UI / UX requirements

All quest/dialogue/event UI follows `UI_DIRECTION.md` and the Lucifer family.

## Journal

Tabs/groups:

```text
Main
Regional
Contracts
Discoveries
```

World Events appear as temporary world/event context, not a permanent backlog tab full of expired tasks.

Each quest entry prioritizes:

- current objective;
- location/search-area context when known;
- meaningful reward preview when appropriate;
- concise prior-step summary;
- abandon option only where legal.

Do not dump hidden debug IDs, internal stage labels or giant lore walls into the player-facing journal.

## Map

Use three marker precision levels:

1. exact point — known service/entrance/objective whose exact location is legitimately known;
2. broad search area — hunt/clue/exploration content;
3. no marker — discovery intended to emerge from environment/lore.

The quest system does not automatically promote every objective to GPS precision.

## Event tracker

- appears only while event is relevant/nearby/participated;
- disappears cleanly after leaving/completion;
- shows the real event objective, not a generic progress bar when the action itself is readable in-world.

---

# 17. Performance rules

- quest objectives are event-driven from kills/interactions/region-entry/gather/reward hooks; no per-tick scan of every quest against the whole world;
- region/event controllers activate only around loaded relevant areas;
- shared event state is one controller per authored event instance/area, not one duplicated world simulation per player;
- personal progress lookup uses indexed quest/objective IDs rather than scanning full journal history on every combat hit;
- map marker recalculation occurs on relevant progress/discovery mutations, not continuously every tick;
- dialogue clients receive only the authoritative branch/state needed for the interaction;
- combat participation records use compact per-encounter eligible-player state and are cleared/reset with the encounter lifecycle.

Profiler/playtest determines whether a specific large event needs stronger optimization.

---

# 18. Multiplayer acceptance tests before calling this system complete

At minimum test:

1. a **2-player party** forms normally; both hit one enemy once and both receive full independent combat EXP/Class XP;
2. 3-player and 4-player parties work; 4 is the maximum, not a required size;
3. one player hitting an elite/boss exactly once qualifies for personal combat reward and relevant kill-objective credit;
4. one valid heal on an engaged injured ally qualifies the healer;
5. one valid protection/buff/control action qualifies the support player;
6. a nearby AFK player with zero valid action receives no event/boss reward;
7. last hit changes no reward ownership;
8. two players can gather the same physical herb node independently and progress their own contract;
9. a player ahead in `Roots Below Stone` cannot drag a new player past missing prerequisites merely by partying;
10. a previously-cleared helper can assist Earthloong without duplicating deterministic first-clear rewards;
11. disconnect during earned-but-unselected dungeon reward preserves exactly one pending choice;
12. `Dust on the Quarry Road` completes from any valid 3-of-5 category combination;
13. `The Crowned Trail` accepts any 2-of-3 clues and still permits finding Regalhart first;
14. Trail Stag shared cooperation grants only each incomplete eligible player's personal unlock;
15. dialogue choices by player A do not silently select player B's personal response;
16. permanent shared world-state changes remain safe for a late-joining player;
17. quest HUD never exceeds the intended 1 Main + 2 pinned optional foreground density outside temporary event context;
18. relog/reopen cannot duplicate Gold, EXP, item, mount or first-clear rewards;
19. a player who tags a scalable boss once and then leaves the encounter keeps earned reward eligibility only as authored, but no longer inflates live boss scaling indefinitely;
20. split parties can pursue separate regions/quest steps without remote progression or ordinary-enemy scaling interference.

`MULTIPLAYER TESTED` remains **NO** until these are exercised with real separate clients.

---

# 19. What this closes

This document closes design-time rules for:

- personal vs shared vs encounter quest/world state;
- main/regional/contract/discovery/world-event categories;
- quest acceptance and HUD density;
- one-action combat/support objective credit;
- gather/investigate/location objective credit;
- split-party progression and helper behavior;
- dialogue and irreversible personal choices;
- return-to-NPC versus remote completion;
- failure/abandon/repeat rules;
- dynamic event activation/participation/scaling/rewards;
- shared permanent world changes and late joiners;
- dungeon/boss personal first-clear state;
- exact R01 quest-state mapping;
- idempotent save/reward transactions;
- disconnect/reconnect behavior;
- journal/map/event UI information rules;
- multiplayer acceptance tests.

Remaining work is primarily **presentation/asset acceptance, implementation data authoring, and real multiplayer playtesting**, not permission to invent a host-owned, kill-steal or contribution-percentage quest architecture during coding.
