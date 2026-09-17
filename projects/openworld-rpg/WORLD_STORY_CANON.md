# Open-World RPG — World / Story / Faction / Ending Canon

> Status: **DESIGN CANON — protagonist frame, world conflict, main-story spine, recurring-character roles, ending structure and region-story integration locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Main-quest implementation package: `MAIN_QUEST_SCENE_PACKAGE.md`  
> Region graph: `REGIONS.md`  
> Quest/state authority: `QUEST_WORLD_STATE.md`  
> Quality audit: `DESIGN_COMPLETENESS_AUDIT.md`  
> Rule: if this file conflicts with `GAME_DESIGN.md`, the master canon wins. Final proper nouns and exact player-facing NPC appearances remain subject to external-first asset intake where this document explicitly marks them provisional.

This document exists to keep a memorable human/narrative spine around the project's combat, progression, economy and multiplayer systems so the later regions do not become only `new biome + mobs + resources + dungeon`.

The story must support the open world rather than turn it into a linear campaign corridor.

---

# 1. Player-character frame

The player uses the approved **light-background custom protagonist** model.

Canonical rules:

- player chooses name, appearance and later root class freely;
- player personality is not pre-written into one fixed temperament;
- the protagonist does **not** begin as royalty, a prophecy figure, a reincarnated god or a uniquely chosen savior;
- every player shares one light common starting frame: they are an independent traveler / contract-seeking adventurer newly arriving in the R01 heartland;
- the opening road incident draws them into the local problem before they have any special status;
- first formal root-class selection still happens through the starting settlement guild/class facility as already canonical;
- later reputation comes from what the player actually does, not a hidden heroic destiny.

This preserves Minecraft character ownership and multiplayer compatibility while giving the main story enough common context to address the player meaningfully.

## Multiplayer protagonist rule

Every player is a legitimate adventurer in their own right.

- the host is not `the real hero`;
- a party does not need to pretend four identical chosen ones exist;
- main-story personal state remains per-player as defined by `QUEST_WORLD_STATE.md`;
- shared encounters can be completed cooperatively while dialogue choices, ending philosophy and personal epilogue remain individual.

---

# 2. Tone

Launch tone is locked to:

```text
adventure / discovery / lived-in fantasy in the early and middle game
→ increasingly strange, ancient and consequential discoveries later
→ serious stakes without becoming constant grimdark
```

Rules:

- ordinary settlements contain work, trade, food, fishing, housing, travel and local concerns worth protecting;
- the world is not already dead when the player arrives;
- humor and warmth may exist naturally through people and situations, but the game is not a parody;
- later regions may become eerie, dangerous or tragic without making every NPC miserable;
- important deaths or losses are used sparingly so they retain weight;
- regional wildlife remains ecology, not universal corruption fodder;
- visual escalation from grounded R01 toward magical/anomalous R08–R12 should make the journey feel larger over time.

---

# 3. Ancient world premise — the Anchor network

The continent contains an ancient distributed infrastructure referred to in production and ordinary readable dialogue as **Anchors** / the **Anchor network** unless later lore naming provides a genuinely better term.

An Anchor is not simply a magic crystal to collect.

The old network historically helped regulate several large-scale world processes:

- unstable magical pressure / anomalous zones;
- selected subterranean energy and geothermal routes;
- ancient transport / observation / containment facilities;
- local environmental extremes where the old builders deliberately intervened.

Important limitation:

**Anchors did not create the whole natural world and their failure does not turn every creature into a corrupted monster.**

Forests, deserts, predators, giant wildlife, regional cultures and ordinary hazards exist independently. Anchor instability only explains selected dungeons, abnormal events, awakened guardians, route changes and the main cross-region mystery.

This avoids making twelve visually different regions tell the same `purple corruption` story.

---

# 4. The central truth

The first half of the game initially supports a simple interpretation:

> Ancient Anchors once stabilized dangerous forces. Some are failing. Repairing them seems obviously good.

Later evidence complicates this.

The complete truth is:

1. the network genuinely prevented large destructive chain reactions and enabled parts of old civilization;
2. it also imposed continental-scale control over systems that regions had previously adapted to locally;
3. later generations became dependent on infrastructure they no longer fully understood;
4. a past central administration repeatedly increased network coupling for efficiency, creating a dangerous single-point-of-failure problem;
5. modern instability is therefore not simply `ancient seal broke`; it is the result of an over-connected system degrading unevenly while new societies developed around its effects;
6. restoring the exact old network, dismantling it, or splitting it into regional systems all solve some problems and create others.

The finale is therefore a stewardship decision, not a morality quiz with one hidden correct answer.

---

# 5. Contemporary conflict

The main conflict is not `good kingdom versus evil cult`.

Three broad approaches emerge among people responding to the Anchor crisis. Final faction names wait for strong external visual/cultural direction, but their ideologies are canonical.

## 5.1 Continental Restoration bloc

Goal:

- reconnect and repair the Anchor network as a coordinated continental system;
- restore predictable roads, settlements, trade, containment and long-term stability.

Strengths:

- practical response to immediate disasters;
- easiest to coordinate at scale;
- protects communities already dependent on Anchor-era infrastructure.

Risks:

- recreates a central point of failure;
- requires powerful continental stewardship;
- can override regional priorities in the name of stability.

This bloc contains competent, sincere people. It is not secretly evil.

## 5.2 Local Autonomy bloc

Goal:

- safely sever/decommission the ancient network and let regions adapt through local systems instead of depending on a dead civilization.

Strengths:

- removes the continental single point of failure;
- maximizes local autonomy;
- allows natural/magical regional processes suppressed by old infrastructure to return.

Risks:

- transition is dangerous;
- some settlements lose old protections or convenient infrastructure;
- regional inequality in resources and preparedness becomes more visible.

This bloc is not anti-technology or anti-civilization. Its objection is dependency and centralized control.

## 5.3 Regional Stewardship bloc

Goal:

- divide the network into independently governed regional clusters;
- retain useful local Anchors without rebuilding one continental master system.

Strengths:

- limits cascade failure;
- lets regions choose different operating rules;
- preserves more infrastructure than full severance.

Risks:

- borders/interfaces between clusters remain difficult;
- maintenance becomes permanent distributed work;
- uneven regional choices can create political and technical friction.

This is **not** written as the automatic compromise/best ending. It trades one large system problem for many smaller coordination problems.

---

# 6. Recurring character roles

The story needs recognizable people across regions without turning the game into a companion-management RPG.

Exact final names/faces/outfits are not locked until external-first model/outfit intake. The following **roles and narrative functions are canonical**.

## 6.1 Guild Pathfinder

- practical field veteran tied to the starting adventurer/guild network;
- meets the player after the opening incident and treats them as capable help rather than a foretold hero;
- recurring point of continuity across early hubs;
- primarily cares about keeping people/routes alive;
- initially politically neutral regarding the Anchors;
- supports gameplay by connecting main leads to real locations, not by assigning endless errands.

## 6.2 Anchor Scholar

- researcher who can interpret ancient records but is not omniscient;
- starts with a restoration-leaning assumption because the known evidence says Anchors prevented disasters;
- their position can evolve as new evidence appears;
- carries much of the mystery/exposition, but important truths are also shown through places, encounters and environmental evidence so the player is not trapped in lectures.

## 6.3 Cartographer / Ranger

- world-travel expert associated with routes, POIs, wildlife and settlement knowledge;
- skeptical of solving every regional problem through a central system;
- gives the player a grounded local-autonomy perspective;
- reinforces discovery/map systems without revealing the whole map for free.

## 6.4 Engineer / Smith

- understands physical infrastructure, lifts, bridges, mining works and practical Anchor-era machinery;
- embodies the regional-stewardship position over time;
- ties high-level story questions to concrete consequences such as a lift working, a forge becoming unsafe or a settlement water system changing;
- not a magical exposition duplicate of the Scholar.

## 6.5 Rival Wanderer

- another independent adventurer encountered intermittently at hunts, roads, ruins and regional milestones;
- has their own route and does not permanently follow the player;
- sometimes cooperates, sometimes reaches a clue first, sometimes disagrees;
- demonstrates that the player is part of a wider adventuring world, not the only competent person alive;
- no mandatory companion-AI system is introduced.

## 6.6 Restoration Director

- senior organizer of the large-scale restoration effort;
- effective enough that settlements have rational reasons to support them;
- becomes the main human opposition when they conclude the network must be reactivated quickly before all evidence is available;
- does not become cartoonishly cruel simply to justify a boss fight;
- the final conflict is over unacceptable risk and authority, not proof that they wanted to destroy the world.

---

# 7. Main-story structure

The main story is **non-linear inside acts** and uses region choices so open-world freedom remains real.

The player may physically enter stronger regions early. Main-story recommendations and facility access may require discovered evidence, but normal region geography is not sealed behind invisible story walls.

The implementation-level route/rejoin rules, sequence-break recognition and multiplayer-safe scene ownership are closed in `MAIN_QUEST_SCENE_PACKAGE.md`. This document owns the story meaning; that package owns the cross-region playable handoff structure.

## Prologue / Act 0 — The Road and the Quarry

Primary region: R01.

Flow:

```text
arrive as unknown traveler
→ opening road disturbance
→ reach starting settlement
→ choose first class / begin ordinary regional work
→ quarry disturbances and wildlife/route clues converge
→ Earthloong dungeon clear exposes first undeniable Anchor-era facility/evidence
```

Story function:

- establishes ordinary life before continental stakes;
- proves the current disturbance is larger than one monster;
- gives the player a reason to investigate without declaring them chosen.

Regalhart remains optional and should not secretly become a mandatory prophecy beast.

## Act I — The Map Beneath the World

Primary regions: R02 and R03.

The player investigates **at least one** major early lead after R01; completing both gives more context and regional rewards but is not required simply to advance the main plot.

### R02 contribution

The woodland sanctum reveals:

- the old network stored observations/memory as well as raw power;
- the Lich/ruin crisis is an authored local consequence around a damaged relay, not proof that all of R02 is undead/corrupted;
- historical records show communication with mountain observation sites.

### R03 contribution

The mountain observatory/forge reveals:

- Anchors were geographically networked rather than isolated shrines;
- route/lift/mineral infrastructure depended on that network in practical ways;
- surviving maps point toward multiple distant regional nodes.

Act-I completion unlocks the **continental investigation** as a clear main-story objective while leaving the route open.

## Act II — Regional Evidence

Primary difficulty band: R04–R07.

The player is asked to obtain enough independent regional evidence to determine why the network is failing.

Main progression requires **two major regional evidence packages** from R04–R07, not all four regions.

Each remaining region still has complete regional story/content and can be finished before or after main progression.

Purpose:

- support peer-route freedom;
- prevent the main story from becoming a checklist forcing every region in level order;
- make optional exploration produce additional context and epilogue detail rather than meaningless filler.

Regional findings must disagree in useful ways. For example:

- a settlement genuinely depends on an Anchor-era heat/water route;
- another region is being harmed because an old regulator suppresses a natural cycle;
- one failed node creates a dangerous anomaly;
- another region proves local adaptation can function without old infrastructure.

By the end of Act II, the three modern philosophies in §5 are understandable.

## Act III — Who Gets to Decide

Primary difficulty band: R08–R11.

The technical question becomes political and practical.

The Restoration Director begins coordinated reactivation because continued delay also carries real risk.

Main progression requires:

- one high-tier land-region investigation from the R08/R09 peer tier;
- one major late investigation from R10 or the appropriate R11 layer;
- a confrontation with the consequences of partial reactivation.

Again, the unchosen regions remain fully playable and contribute optional evidence, relationships and ending epilogue states.

Act III must show both sides being partly right:

- restored infrastructure genuinely saves something;
- over-coupling also causes a dangerous cascade or anomaly that proves blindly recreating the old system is unacceptable.

This event exposes/opens the final truth path toward R12.

## Act IV — R12 / The Central Anchor

R12 remains physically discoverable as an extreme open-world region, but the deepest central facility/main finale requires the evidence/keys produced by the main investigation.

Flow:

```text
enter anomaly zone
→ reconcile conflicting historical records with the actual system
→ learn why the network became over-coupled
→ stop the immediate cascade / defeat the final ancient-system encounter
→ confront the Restoration Director's final attempt or disagreement without reducing the entire story to killing them
→ choose the future operating model
```

Exact final boss model/mechanics are **not locked here**. They require external-first boss intake and the asset-gated final-guardian closure defined by `PROJECT.md`.

The final boss should be an ancient/systemic guardian or crisis-form encounter whose mechanics visibly express the Anchor network. The Restoration Director may be present/opposed without requiring a generic humanoid HP-sponge duel.

---

# 8. Regional-story contract for R03–R12

From this point forward, a regional package is incomplete unless it answers all of the following.

## 8.1 People

- who lives/works/travels here?
- what does normal life look like when there is no quest happening?
- what service/economy connects them to the region?

## 8.2 Current regional problem

- what changed recently or what persistent local conflict deserves attention?
- why does it matter to residents rather than only to the player's loot table?

## 8.3 Main-story relationship

Every region must explicitly be one of:

- major Anchor evidence region;
- optional corroborating/contradicting evidence region;
- mostly independent regional story whose events show how ordinary people live under the wider crisis.

Do not make every boss an Anchor guardian.

## 8.4 Regional change / memory

At least one meaningful personal or safe shared consequence should remain after the regional climax:

- dialogue/service change;
- opened route/shortcut;
- settlement visual improvement where late-join safe;
- new merchant/recipe/hunt;
- changed event pool;
- new NPC presence;
- personal journal/epilogue flag.

A region should remember that something happened.

## 8.5 Distinctive play sentence

Each region needs a one-sentence gameplay identity that is not simply its biome.

Examples of shape:

- `read the mountain, open lifts and master exposed vertical routes`;
- `navigate river/canopy landmarks while dangerous wildlife controls shortcuts`;
- `move between land and channels while water routes reshape encounter angles`.

---

# 9. Current regional narrative hooks

These are **directional hooks**, not permission to lock unverified visual assets/names.

## R01 — Heartland

Narrative role:

- prove ordinary local trouble connects to something old and continental.

Local human concern:

- roads, quarry work, trade and settlement safety.

Anchor relationship:

- buried facility/evidence beneath the quarry route.

## R02 — Western forest

Narrative role:

- show the network preserved records/communication as well as power.

Local human concern:

- logging/trade routes, forest boundaries and newly exposed ruins.

Important rule:

- Nature Spirit/forest wildlife remain native ecology;
- the Lich belongs to the authored ruined sanctum and does not convert the whole forest into an undead zone.

## R03 — Whitecrest highlands

Narrative role:

- reveal continental network geometry through an old observatory/forge complex.

Local human concern:

- safe mining routes, lifts, bridges and whether dangerous old machinery should be reopened.

Regional change:

- player actions permanently restore selected safe shortcuts/lifts where late-join safe.

## R04 — Frozen crown

Narrative direction:

- a refuge/expedition network depends partly on geothermal or protected routes that are becoming unreliable;
- cold wildlife remains natural;
- the player sees a strong case for restoration because losing old protection has real human cost.

## R05 — Jungle greenbelt

Narrative direction:

- river/canopy settlements have adapted around dense natural cycles rather than ancient central control;
- old temple/root facilities show that some Anchor intervention previously constrained or redirected those cycles;
- presents a stronger autonomy argument without declaring technology evil.

## R06 — Mirewater delta

Narrative direction:

- old water-control infrastructure and natural wetland needs conflict;
- restoring every ancient channel could help roads/settlements while harming the living delta;
- regional outcome should affect routes/services, not become a global morality flag.

## R07 — Sunscar desert

Narrative direction:

- oasis/caravan survival makes water predictability valuable;
- ancient subterranean regulation may explain why some routes persisted while others died;
- giant desert predators remain native hazards rather than Anchor creations.

## R08 — Magical / fae route

Narrative direction:

- reveals a region where loosening old regulation allowed genuinely valuable magical ecology/culture to flourish;
- makes full restoration visibly costly even when technically stable.

## R09 — Southern rocky marches

Narrative direction:

- local communities favor practical self-reliance and physically maintained routes/fortifications;
- provides a grounded non-magical test of regional autonomy and shared defense.

## R10 — Volcanic ash region

Narrative direction:

- high-value heat/mineral infrastructure makes Anchor control economically attractive and physically dangerous;
- partial restoration here can produce one of Act III's clearest `it works, but the coupled risk is real` demonstrations.

## R11 — Maritime route

Narrative direction:

- currents, harbor safety, reefs and deep routes show how continental systems affect travel without making the sea itself artificial;
- different depth bands can reveal different eras of infrastructure and evidence.

## R12 — Anomaly zone

Narrative role:

- source of the clearest evidence about network coupling and the final operating choice;
- anomalies become legitimately strange here because this is where the system failed most severely, not because every earlier biome needed the same effect.

---

# 10. POI and travel-density contract

Azari's 30k x 30k scale only matters if travel repeatedly produces curiosity and decisions.

## 10.1 Interest cadence

On intended on-foot / early-mount routes, target roughly:

```text
visual orientation / interesting sightline: every 45–120 seconds
meaningful route choice, encounter, resource pocket or discovery: every 2–4 minutes
substantial authored POI: normally within 4–7 minutes of active exploration on content-bearing routes
```

These are **playtest targets**, not a requirement to place a chest every N blocks.

Breathing-room scenery is valid. Long dead mandatory travel is not.

As mounts become faster, spacing can grow physically while preserving similar perceived cadence.

## 10.2 Major POI quality check

Every important authored POI is reviewed on four axes:

1. worldbuilding;
2. progression/reward;
3. spatial/gameplay design;
4. navigation/landmark value.

A major POI should normally satisfy at least **3/4 strongly** and ideally all four.

`ruin + chest` is not enough.

## 10.3 Regional density floor

This is a content-planning floor, not a quota to fill with garbage.

A normal full major region should usually support:

- one primary settlement/hub or a deliberate wild-region alternative;
- one major dungeon or equivalent authored multi-stage complex;
- one field/world-boss experience;
- roughly 2–4 other substantial authored POIs/complexes;
- roughly 4–8 smaller discoveries/landmarks with actual identity;
- selected dynamic-event locations;
- gathering/fishing/hunt routes that overlap the above rather than existing as disconnected map dots.

If a region cannot support this without repetition, make it smaller or merge content instead of filling emptiness with copy-paste.

---

# 11. Main-story pacing target

The story should leave room for the rest of the RPG.

Working first-play targets:

- focused main-route player: roughly **15–22 active hours** depending on combat/travel skill and optional detours;
- ordinary mixed first play: roughly **25–40+ hours** with regional quests, exploration, housing, fishing, class/equipment experimentation and optional bosses;
- completionist play may go substantially longer without requiring repetitive mandatory grind.

These are not marketing promises. They are pacing targets used to detect a main story that is either a six-hour skeleton or a 60-hour forced checklist.

Main quest should normally occupy only part of a play session. The player must be free to see a side road, fish, hunt, gather, change gear, buy a home or clear a regional story without feeling that the world-ending timer makes all of that narratively absurd.

Therefore the crisis escalates through evidence and localized failures, not a literal `the world ends tomorrow` countdown.

---

# 12. Final choice and endings

After the final immediate cascade is stopped, each player makes a deliberate personal choice.

There are three launch ending philosophies.

## Ending A — Restore

Choose to rebuild a coordinated continental Anchor network under modern stewardship.

Epilogue character:

- safer predictable infrastructure;
- strong inter-region coordination/trade;
- reduced anomaly risk in many settled areas;
- continuing debate over who holds continental authority and how dependency is prevented from recurring.

## Ending B — Release

Choose controlled dismantling/severance of the old network.

Epilogue character:

- highest regional autonomy;
- natural/magical systems adapt without continental regulation;
- difficult transition for places that depended heavily on old infrastructure;
- local engineering/social solutions become more important.

## Ending C — Partition

Choose to break the network into independently governed regional clusters.

Epilogue character:

- cascade risk is reduced;
- useful local infrastructure survives;
- different regions adopt different operating rules;
- interfaces, maintenance and politics remain an ongoing challenge.

No ending is labelled `good`, `bad`, `true` or `canon` in player-facing UI.

Optional regional completion changes **epilogue details**, not which philosophical choice the game secretly judges correct.

---

# 13. Multiplayer ending / postgame world

The final dungeon/encounter can be cooperative, but the ending choice is personal.

Server rules:

- each eligible player records their own ending choice;
- one player's choice never overwrites another player's story history;
- personal epilogue dialogue/journal/cosmetic/title rewards can reflect that choice;
- the shared physical server world enters one **neutral post-crisis state**: the immediate catastrophic cascade is stopped and the final combat space is safe/replay-compatible;
- do not permanently transform the whole shared map into three mutually incompatible geometries based on whoever clicked first.

This preserves co-op without turning story agency into host authority.

---

# 14. Postgame / endgame role

Finishing the main story does not unlock an infinite numerical treadmill.

Postgame emphasizes content already central to the game:

- R12 extreme encounters/anomaly hunts;
- R11 deep-sea/high-tier content;
- optional regional stories not completed during the main route;
- field/world-boss target farming and Mythic collections;
- difficult dungeon variants/challenges only where mechanics justify them;
- hidden techniques / advanced class challenges;
- Fish Codex/trophy collection;
- housing/trophy/display completion;
- rare discoveries and late optional bosses;
- build experimentation across saved class histories.

No launch requirement for:

- infinite Paragon/prestige levels;
- daily login chores;
- seasonal reset;
- mandatory endless dungeon keys;
- separate endgame currency inflation.

---

# 15. Narrative presentation rules

- gameplay and environment carry as much story as dialogue;
- avoid long mandatory exposition scenes before the player understands why the information matters;
- important dialogue is skippable/advanceable according to `QUEST_WORLD_STATE.md`;
- no silent protagonist option needs to force completely blank dialogue: use concise player response choices where useful without pretending every line creates a branching game;
- do not make every regional NPC know the entire Anchor mystery;
- ordinary people should talk primarily about their own lives and local problems;
- journals/records should be short enough to read voluntarily and never required merely to understand the basic main objective;
- major revelations should be supported by location/visual evidence rather than a single lore dump.

Final localized dialogue prose is content writing governed by the locked scene intent/objectives/rejoin state, not a license to change quest logic during implementation.

---

# 16. External-first narrative presentation

Recurring NPCs, faction uniforms, important story props, Anchor machinery, final facility architecture and cinematics follow the same external-first contract as combat content.

Before locking exact final NPC proper names/visual identities:

1. select a coherent external NPC/apparel/model family;
2. verify license/dependency boundary;
3. map role and regional culture to that visual;
4. then lock exact appearance/name presentation.

Story design is not permission to fill the game with vanilla villagers wearing renamed skins.

Important Anchor devices/facilities likewise require actual external design/model/structure references before visible implementation. `generic glowing cube` is not an accepted final ancient-tech language.

---

# 17. Save / choice data

In addition to `QUEST_WORLD_STATE.md`, the story needs stable personal flags such as:

```text
main_act
main_step
anchor_evidence_ids[]
regional_story_completion[]
regional_outcome_flags[]
recurring_npc_relationship_flags[]
ending_choice
ending_completed
ending_epilogue_flags[]
```

Rules:

- ending choice is explicit and idempotent;
- disconnect during final choice does not reroll or duplicate rewards;
- regional optional flags may alter epilogue/dialogue but never erase unrelated progression;
- migration/versioning is required before any persistent test world becomes valuable.

`MAIN_QUEST_SCENE_PACKAGE.md` refines the exact route/rejoin/sequence-break ownership that uses these durable states. A player who completed an eligible region before the main quest requests it must not be forced to replay the content merely to set a later flag.

---

# 18. What this closes

Closed before implementation:

- customizable protagonist with light shared background;
- early-adventure → late-strange serious-but-not-grimdark tone;
- Anchor network as the cross-region mystery/infrastructure;
- no universal corruption explanation;
- three legitimate modern approaches to the network;
- recurring character-role ensemble without companion-management scope;
- non-linear four-act main-story structure;
- R01–R12 narrative function directions;
- region-story minimum contract;
- POI/travel cadence quality targets;
- three personal ending philosophies;
- multiplayer-safe personal endings and neutral shared postgame;
- postgame role without infinite progression treadmill;
- cross-region main-route selection requirements, scene/rejoin ownership and sequence-break behavior through `MAIN_QUEST_SCENE_PACKAGE.md`;
- R03–R12 regional story/outcome content through their current implementation-package + content-bible pairs.

Remaining work is presentation/spatial binding rather than another broad story-design pass:

- exact final visual models/outfits and asset-dependent proper-name presentation for recurring NPCs/factions where still gated;
- actual Azari coordinates, staging, sightlines and travel relationships for major story scenes/locations;
- final Anchor machinery/facility external asset language and accepted provenance;
- final systemic guardian exact external model, anatomy-supported phases/weak points and rewards;
- exact region/main-scene music and SFX asset selection; behavior/state priority is already closed in `ACCESSIBILITY_DIFFICULTY_INPUT_AUDIO.md`;
- epilogue/cinematic presentation assets and final localized prose built from the locked scene intents.

Do not return to `biome + mob list` planning or reopen the main route. The next story-facing work must bind this canon to accepted assets and actual world space.