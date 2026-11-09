# TURNBOUND — Dungeon Exit Interaction and Multiplayer Ownership Options

## Approved co-op disconnect and third-defeat spectator continuation — 2026-11-09

**Latest user-confirmed behavior. This supersedes older suggestions that an offline or third-defeat teammate must remain physically present at the escape chamber, or that a third-defeat player must be ejected from shared rewards.**

### Participant disconnect
- When a player **leaves the game/disconnects** during a shared dungeon run, the dungeon continues with the **currently connected participants**; if one player remains, that participant's run is treated as **solo** for dungeon interaction and exit eligibility. The remaining player can finish the dungeon by themselves without waiting for the disconnected participant at the exit chamber.
- Remaining progress (already defeated shared dungeon enemies, claimed shared room progression, current exploration and previously earned rewards) **persists**; disconnect is not a run wipe, dungeon reset, forced reward reduction or instant clear. A solo survivor may continue fighting and later use the exit object to finish.
- In a group with more than two connected participants, apply the same principle to remaining active players: the departing/offline member must not strand the others at exit. Do not introduce a mandatory party-vote UI as a prerequisite to making the solo conversion work.
- The user has **not defined** what rewards an offline participant later receives, whether they automatically reclaim a place in the same active run upon reconnect, or what happens if everyone disconnects. Do not silently grant absent players all future rewards or unconditionally invalidate their previously earned rewards. Define explicit reconnect/run ownership and persisted credit rules later.

### Third party-battle defeat while teammates remain
- The player who suffers **their third dungeon defeat** still receives the already-approved **personal 30 cumulative-online-minute entry ban** for **this physical dungeon** and the normal **loss-free safe checkpoint handling** for their Minecraft avatar. This is not permanent and doesn't prevent the other team member(s) from continuing their run.
- **Instead of forcing the survivor(s) to finish alone while the defeated member disappears from the team, that third-defeat player moves into a non-interactive SPECTATOR role for the surviving teammate(s)**. Their camera can watch ongoing cooperative fights/exploration, but their avatar cannot physically reenter the forbidden dungeon, cross boundaries, attack, mine/loot in that run or initiate encounters while spectating.
- The third-defeat spectator **stays associated with the current cooperative dungeon run** and **can receive the normal shared dungeon clear and, if achieved, full-clear payout when the surviving player(s) complete the exit interaction**. Do not withhold their clear reward for not participating after defeat or require them to physically attend the exit chamber.
- Previously approved **shared same-value encounter/room reward entitlement across eligible teammates** is not silently rewritten; implementation must distinguish already-earned rewards from new event credit and resolve exactly-once settlement. User specifically reconfirmed **clear reward eligibility** for the spectator.
- **Joint-exit gathering requirement applies to connected ACTIVE EXPLORERS only.** Spectators are attached to that group's outcome and do **not** have to physically walk into the exit room. Only the surviving active explorers need to meet in the exit room and deliberately activate the authored exit; a spectator cannot initiate or force exit.
- If a remaining active explorer disconnects while a spectator is attached, the user has not yet resolved which member may continue the encounter/run; do not treat a spectator as an active solo explorer who can clear the dungeon. Handle as an explicit save/suspend/reconnect case, not an automatic reward grant.
- 30-minute online dungeon access timer must continue to obey established policy during connected spectator activity; user did not authorize a new spectator-only timer exception.
- **No access-restriction bypass:** spectator camera may view dungeon activity even while the player's own avatar is outside, but cannot be turned into a free-moving physical character that interacts with dungeon objects or rewards. Restoring controls requires the appropriate normal world/entry rules.

### Combined example (two players A and B)
- A + B split up, defeat separate enemy rooms, both eligible for equal team rewards.
- If B logs out, **A becomes a solo active dungeon explorer** and can clear through the exit object without B present.
- If B instead gets a third party defeat, **B spectates A** (no physical dungeon admission for B); A keeps exploring and when A uses the exit at the proper chamber, **both A and spectator B receive the same normal dungeon clear / full-clear reward**, as applicable.
- The remaining dungeon enemies and room progress do not respawn prematurely, and the one-hour post-exit repeat cooldown starts on actual completion; its online-only versus wall-clock basis remains undecided.

### Still to design: edge conditions
- Exactly how an online spectator watches split-up surviving teammates, switches camera target, dismisses spectating, returns to ordinary safe-world control, and how online access-ban expiry interacts with an ongoing run.
- A simultaneous total party wipe, a player who disconnects *after* three defeats while spectating, reconnecting during/after a solo conversion, repeated reconnect spam and missing-participant rewards; no user-approved detailed outcomes yet.
- Co-op combat control if two allies enter the same combat room, mid-combat disconnect handling, team-owned dungeon reset on the one-hour timer, reward log persistence across crash/restart.
- Ensure spectators are immune to environmental danger while observing and that the avatar remains securely at a safe world checkpoint; exact UI/presentation and multiplayer network validations require a real playtest.
- This is a **design contract only**: no implementation, JAR, build, or actual two-client multiplayer test has been run here.


## Confirmed cooperative dungeon run and joint exit — 2026-11-09

**CURRENT USER-APPROVED RULE; this supersedes earlier unapproved alternatives for independent room progress, solo exit during a shared run, or unequal rewards based on who landed a kill.**

### Shared physical progress and concurrent fights
- Dungeon exploration is **cooperative** for the players in that dungeon group. A physical generated dungeon has a shared active run: authored rooms, encounter defeats, treasure/room progress and all-clear enemy count are **team-wide progress**, not independently reset for every person.
- Players can **split up and battle separate rooms at the same time**, so cooperation rewards efficient division of labor; players should not be forced to stand in one combat room for another member's local encounter. Each room combat remains an on-site turn battle until its own victory, escape or defeat.
- **Kill/individual encounter awards and dungeon clear rewards are the same full reward for every active eligible participant**, not a pooled amount divided among members. A player fighting enemy A in one room and another fighting enemy B elsewhere both receive the rewards for A **and** B, regardless of who fought or landed the final hit. Similarly, battle-room clear and dungeon all-clear rewards should not be diminished by splitting the party.
- Resolve one victory on the server and **credit the same reward to each eligible participant exactly once**; sharing completed group objectives must not multiply the total payout received by one player. Don't gate normal team battle rewards behind personal damage/participation in that particular combat.
- **All clear:** defeat all finite authored dungeon enemies across the shared run, including any optional/hidden combat rooms, even if those kills were by different players. At group exit the group receives the same ordinary-clear reward plus the same all-clear bonus if eligible.
- **Personal state remains separate:** each player's dungeon-specific loss count, 3-loss / 30-connected-minute admission ban, account resources, and resulting item/currency balances remain individually tracked. The group does not inherit the defeat counter of a member.

### Group departure, not unilateral exit
- All members of the **current participating dungeon group must physically gather in the exit room** to complete the dungeon and leave together. **One member activating or reaching the exit while others are elsewhere cannot unilaterally clear/teleport the entire team.**
- The exit interactable is a world object (exact theme/asset TBD). After all present members are gathered and the group's departure is valid, perform a **single group-clear transition**. It evaluates all-clear and credits identical group outcome/reward value individually; only that departure starts the previously approved 1-hour replay cooldown for the run.
- Reaching the exit room with missing members does **not** clear the dungeon; members may leave it and backtrack for more encounters/treasure. A group waiting at exit cannot erase another member's ongoing fight or force a half-finished session to end.
- The exact departure confirmation protocol is still open: who may activate the exit when all are present, whether each member must confirm, and how to handle refusal/timeout/disconnect. **No assumption of forced teleport just because one member clicks**. Recommend short member-ready confirmation, but NOT yet approved.

### Open cases to design before implementing
- **Party membership and reward snapshot:** who is an eligible participant, when can members join/leave/reconnect, whether players in different teams can share the same physical dungeon simultaneously, and how to stop late arrivals/free-riding or phantom group members blocking departure, without contradicting the rule that separate-room fighting earns identical rewards.
- **One member suffers third defeat:** that player is checkpoint-returned and personally entry-locked for 30 online minutes while teammates may remain inside. Define a non-destructive team-membership/exit continuation policy so the other members are not stranded indefinitely; do NOT silently auto-kick, cancel the group or penalize innocent members.
- **Simultaneous battles:** map a player's four-character party, enemy ownership and turn-gauge combat sessions safely for separate rooms; how team members fight together in the SAME battle is a separate unresolved co-op combat-control question.
- **Physical loot:** equal server-awarded encounter/room/clear rewards are approved; how shared Minecraft chest contents, dropped physical items and one-off mined blocks are allocated remains open. Do not conflate natural shared-world chests with automatic personal gem/Gold transaction rewards.
- **Independent personal replay eligibility vs one shared reset:** a personal 30-online-minute loss ban, a run's one-hour replay cooldown (clock mode unspecified) and other players' continuing dungeon presence must not corrupt or prematurely respawn shared world entities.
- **Safe reward/result UI** protects its viewer from aggro/damage without pausing other players or killing other people's battle mobs.
- Preserve actual real-world cave/ruin geometry, non-menu movement, combat room camera clearance, unexplored room mystery, per-run no-respawn-before-clear, equal-value repeat rewards and escape-device-only dungeon completion.

### Validation status
**DESIGN ONLY.** No co-op reward transaction fan-out, shared-run state, group-exit coordination, parallel-room battle, protection, multiplayer test, JAR, client test or gameplay build has been implemented/verified by this documentation change.


Date: 2026-11-08
Scope: `projects/turnbound/` on `turnbound-content-rebuild`
Status: **Cooperative dungeon reward/exit rules confirmed on 2026-11-09; remaining UI and edge-case options unapproved.** No code implementation or gameplay test.

## Current approved game contract: preserve unchanged
- Minecraft-style physical dungeon movement, not menu/node navigation, and room content concealed before entry.
- Entering a combat room starts an on-site turn-based battle; the player cannot physically leave until victory, successful escape or defeat resolves the encounter. Numeric escape odds are still open.
- Player's party has four collectible characters; the Minecraft avatar explores and does not join turn combat as a fifth character.
- Exit/clear only occurs after **intentional interaction with an authored escape object and actual departure**, not reaching the chamber, killing a boss or obtaining treasure. The player can backtrack and continue exploration even after discovering the exit.
- All-clear bonus at exit requires **all finite dungeon-authored enemies** defeated in that run. Ordinary exit clear otherwise pays ordinary rewards.
- Individual encounter, battle-room and treasure rewards may already be earned; main clear and all-clear rewards are settled at exit, exactly once.
- Pre-clear enemies and loot do not regenerate. Following clear, a **one-hour replay cooldown** applies and a subsequent eligible full run pays equal-value rewards (no repeat nerfs); whether this timer is wall-clock or online-only is not yet decided.
- Three actual party defeats by one player in one physical dungeon produce that player's **30-online-minute lockout** to that dungeon only; no items or currency are lost.
- When combat-result, reward or dungeon-clear UI is open, local enemies do not approach or damage that player; do not globally pause the server or other players.

## Proposed physical exit and screen workflow — UNAPPROVED DETAILS
1. The generated dungeon places a tangible, themed exit interactable in its escape room, with art/animation tied to structure theme. Crystal, stone marker, portal, etc. are possible assets, NOT selected visuals.
2. Interaction opens a short, readable **Leave / Continue Exploring** confirmation (subject to real Minecraft UI validation). Merely entering the room opens nothing and starts no timer.
3. Offer minimal status such as **All-Clear Eligible / Not Yet All-Clear** without spoiling undiscovered room placement, enemy kinds, treasure locations or total hidden enemy count.
4. On successful leave, validate participant's actual dungeon ID/run and eligibility server-side, record ordinary/full-clear outcome, deliver rewards idempotently, start the one-hour repeat cooldown and return player to a safe world position. Don't claim implementation order/atomicity resolved until real persistence/network design.
5. Present a concise battle/dungeon result UI after safe exit, with reward summary and optional room/clear information. In-game look, layout, icons, copy and animations require commercial-game reference research + actual client screenshot and play testing before acceptance.
6. Safe UI state is player-scoped; suppress hostile targeting, contact-triggered encounters and incoming damage for the protected player, including during dismissal transitions, without freezing other players' monsters and survival. Ensure no indefinite freely-toggleable invulnerability exploit.

## Historical multiplayer comparison (superseded by approved cooperative mode above)
**Option A — Shared physical dungeon run (RECOMMENDED, NOT APPROVED):**
- Players intentionally participating together enter one physical, server-owned dungeon run. Layout, enemies and room clears are shared, matching visible Minecraft world state.
- Each player has **individual** losses/30-minute access ban, individual clear reward settlement, replay eligibility and progression record. Collective combat room success must not grant duplicates for one player or force others to leave the world.
- Players can decide individually when to use exit; an early exit does not automatically teleport/finish other players and cannot erase enemies/progress they need. A reset may not occur while eligible participating players remain active in the dungeon.
- Participation/credit must be defined for join-in-progress, partial participation, early exit, another player defeating a boss while a teammate is elsewhere, disconnect/reconnect and players intentionally joining different parties.
- Cooperative turn battle actor control requires a specific separate decision: do players command separate 4-character parties, collectively command 4 actors, or a chosen allotment? The legacy engine's multiplayer/session code is not evidence the new mode is already implemented.
- The physical run reset timing and player-specific 1h cooldown interaction require a coherent policy: do not automatically reset shared mobs simply because one player reaches eligibility.

**Option B — Per-player progression in a shared physical structure (UNAPPROVED):**
- Individually credited combat-room/treasure states are kept separate, but the same server world must present different active enemies and room outcomes to separate players without unfair collisions, invisible hits, duplicated physical loot or players stealing each others' enemies.
- This is more complex for a survival world with editable blocks and may require extensive visibility, encounter-claim and rendering infrastructure. Avoid promising easy instancing or assigning hidden client-only state as authoritative.

## Later questions / implementation risks
- Who owns a particular shared run and who may participate; whether two unrelated groups may start the same structure simultaneously; how to handle a player locked out after 3 defeats while friends remain.
- Participation requirements for shared rewards, per-player vs per-team boss clear/all clear and exactly-once gem rewards.
- Whether departing player sees the all-clear state at the time **they** leave and how to prevent a late joiner claiming all rewards without participating.
- Physical chest contents, mining and other survival interactions versus abstract personal clear payouts: keep them coherent and don't overwrite real builds on repeat reset.
- Explicit 1h cooldown clock basis; reset eligibility when different people depart at different times.
- How a reward or escape UI responds to incoming attacks already in flight, another player's active combat, logout/crash and concurrent exit actions; test server-authoritative transitions.
- Asset/UI design should follow verified game visuals, not temporary card/mockup UI copied directly into Minecraft.
- **No Java resource changes, builds, functional multiplayer or playtests were performed.**

## Updated next decision
Cooperative shared physical progress, equal team combat/clear rewards and joint physical exit are approved; 2026-11-09 rules now resolve disconnect-to-remaining-solo and third-defeat spectating with preserved clear reward. Next design pass should resolve same-room collaborative combat, reconnect behavior, spectator controls and simultaneous wipe.
