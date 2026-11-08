# TURNBOUND — Dungeon Exit Interaction and Multiplayer Ownership Options

Date: 2026-11-08
Scope: `projects/turnbound/` on `turnbound-content-rebuild`
Status: **ASSISTANT DESIGN PROPOSALS, NOT YET USER-APPROVED.** No code implementation or gameplay test.

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

## Multiplayer architecture choice still needed
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

## Recommended next decision
Select the multiplayer ownership model first: **shared physical dungeon progress and individual player rewards/losses** (Option A) versus individually separate room progress (Option B), before specifying final exit UI and implementation.
