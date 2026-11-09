# TURNBOUND — Dungeon Exit Interaction and Multiplayer Ownership Options

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
Cooperative shared physical progress, equal team combat/clear rewards and joint physical exit are now approved; next design pass should resolve party roster, member absence/third defeat at exit, and reward eligibility without breaking parallel-room fights.
