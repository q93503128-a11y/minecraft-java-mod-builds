# Open-World RPG — Party / Multiplayer Canon

> Status: **DESIGN CANON — co-op party, personal combat rewards, shared encounter participation and multiplayer UX rules locked before implementation**  
> Master gameplay canon: `GAME_DESIGN.md`  
> Project contract: `PROJECT.md`  
> Quest/world-state authority: `QUEST_WORLD_STATE.md`  
> Combat authority: `COMBAT_BALANCE.md`  
> Class progression: `CLASS_PROGRESSION.md`  
> Loot/economy: `LOOT_ECONOMY.md`  
> Rule: this file closes multiplayer/party details that the existing master and quest-state canon already imply. It does not grant party leaders progression authority and does not override personal quest/end-state rules.

The multiplayer goal is **play together without turning cooperation into reward competition or host-owned progression**.

A friend joining should make the game more social and usually somewhat easier/faster, not reduce the EXP each player earns, steal loot, force quest syncing or make one player the `real protagonist`.

---

# 1. Launch party model

Formal party membership is a convenience layer over the server-authoritative world.

Launch baseline:

```text
formal party size cap: 4 players
party membership required for normal co-op: NO
party leader progression authority: NONE
party friendly fire: OFF
```

Players outside the same formal party may still participate in public/authored encounters when the encounter permits it. Reward eligibility always comes from actual participation, not from the party roster alone.

The four-player cap is chosen because:

- core boss HP/poise scaling is explicitly tuned for 1–4 engaged players;
- HUD readability remains manageable;
- the intended private-play use is small-group co-op;
- encounters can still support additional non-party eligible players using the existing `>4` scaling rules when necessary.

Party leader may:

- invite;
- remove a member;
- promote a new leader;
- place/clear one party waypoint or contextual ping where allowed.

Party leader may **not**:

- accept quests for another player;
- choose dialogue/endings for another player;
- claim another player's loot/reward;
- reveal another player's personal discoveries automatically;
- change another player's class/build;
- force fast travel/teleport;
- decide shared permanent world changes unless the authored encounter itself defines a server-world outcome.

Leaving/dissolving a party never deletes personal progression or already-earned participation rewards.

---

# 2. Shared enemy kill EXP — canonical rule

**Combat EXP is never divided by the number of party members.**

When one enemy dies, every eligible participating player receives a separate personal EXP award.

For eligible player `P`:

```text
PersonalKillEXP(P)
= normal solo EXP value for that enemy/content role
  evaluated against P's current EXP_to_next
  × P's own encounter-Lv modifier
```

Therefore:

```text
2 eligible players kill one common enemy
→ player A receives 100% of A's normal eligible kill EXP
→ player B receives 100% of B's normal eligible kill EXP
→ no 50/50 split
```

Likewise for 3–4 players: each qualifying player gets their own full normal personal award.

There is:

- no shared EXP pool;
- no `party EXP penalty`;
- no last-hit bonus;
- no damage-rank multiplier that gives the top DPS more EXP;
- no requirement that all eligible players belong to the same formal party.

The receiving player's existing level-difference modifier still applies individually. A low-Lv player being helped by a high-Lv friend does not receive the high-Lv player's absolute EXP budget; the reward is always evaluated from the receiver's own level curve and encounter-level modifier.

This deliberately makes playing with friends comfortable while preserving the global anti-power-level curve.

---

# 3. Class XP follows the same non-split principle

For combat sources, each eligible player also receives their own normal **Class XP** award for the active class that earned participation.

```text
PersonalCombatClassXP(P)
= normal solo Class-XP value
  × P's own encounter-Lv modifier
```

It is not divided by party size.

Existing anti-swap ownership remains:

- combat/event Class XP goes to the class active during the eligible contribution;
- changing class just before the enemy dies does not steal the reward onto the new class;
- dungeon/quest majority-participation rules remain as defined in `CLASS_PROGRESSION.md`.

Support-oriented classes can qualify through real support contribution and receive the same personal Class-XP entitlement as a damage build.

---

# 4. Combat reward eligibility

Party membership by itself never grants combat EXP/loot.

The server keeps an encounter-participation ledger.

Valid participation can include:

- direct effective damage;
- poise/stagger contribution;
- effective healing to an engaged eligible ally;
- barrier/protection that actually absorbs or prevents hostile damage;
- authored control/debuff/support that materially affects the engaged target;
- successful revive during the encounter;
- required encounter/objective interaction.

## Ordinary common enemies

Eligibility is intentionally lenient.

A player qualifies through either:

- at least one legitimate damaging hit; or
- one meaningful encounter-linked support contribution to an ally who is actively fighting that enemy.

Reason: two friends should not fight over tags on a 3-second common mob.

## Elite / miniboss / boss / world event

Use a broader participation window and threshold so that:

- a support player can qualify without damage racing;
- one token hit at the final 1% does not automatically grant a major first-clear reward;
- late arrivals can still earn normal eligible rewards when they genuinely join the fight before it is effectively over.

Exact weights are data-driven by encounter role, but **DPS share is never the sole qualifier**.

AFK proximity, following a party around without contributing, or standing outside the fight gives no combat reward.

---

# 5. Downed / defeated / disconnect behavior

Existing down/revive rules from `COMBAT_BALANCE.md` remain authoritative.

Reward handling:

- becoming Downed does not erase participation already earned;
- a player who is revived and continues fighting retains one continuous participation record;
- a player who is defeated but remains in the encounter/revive context can still receive a major encounter reward if the threshold was already legitimately met;
- voluntarily leaving the encounter and remaining inactive long enough may expire normal participation eligibility;
- reconnect never duplicates a committed reward.

For major bosses/dungeons/events, if a player met reward eligibility before a brief disconnect and the server resolves the encounter while their reward transaction is pending, the idempotent personal reward may be delivered on reconnect.

Ordinary common-enemy kill EXP is not queued indefinitely for an offline player.

---

# 6. Loot and Gold in co-op

Eligible combat loot is personal unless a specific authored world object is intentionally shared.

Baseline:

```text
personal equipment roll
personal material roll
personal signature-material state
personal first-clear reward
personal EXP / Class XP
personal ordinary combat Gold where that source awards Gold
```

No need/greed roll window.
No ground-loot race.
No party leader loot ownership.

A Mythic/signature drop for one player does not consume another player's roll.

Boss first-clear / repeat status is evaluated independently per player.

---

# 7. Ordinary enemy and boss scaling

Existing combat canon remains:

- ordinary enemies do **not** gain generic HP because another player joined;
- elite/miniboss/boss scaling uses **eligible engaged players**, not total online players and not total formal party membership;
- 1–4 player boss HP scale: `1 + 0.65 × (N - 1)`;
- 1–4 player boss poise scale: `1 + 0.40 × (N - 1)`;
- outgoing boss damage does not increase merely because more players joined.

A friend exploring on the other side of the map must not make the local boss tankier.

An inactive/AFK party member must not count toward encounter scaling.

When an additional player legitimately joins an active scalable encounter, scaling may increase at an authored safe synchronization point rather than instantly healing/warping the boss in an unreadable way. The exact controller transition must not delete already-dealt damage percentage unfairly.

---

# 8. Quest progression in a party

Quest progression remains personal.

If party members share the same objective:

- one physical encounter may credit each qualifying player;
- each player's logical quest step advances separately;
- reward transactions remain personal.

If party members are on different steps:

- each receives only progress valid for their own step;
- a later-step player cannot skip prerequisites for an earlier player;
- the earlier player can still help and receive ordinary combat/event rewards.

Helping a quest you have not accepted can grant normal eligible combat/event rewards, but not its one-time quest completion, unlock or story choice.

No global `sync everyone to leader` button at launch.

---

# 9. Discovery / gathering / fishing / merchants / housing

Co-op must not create resource competition by default.

- gathering-node availability is personal;
- fishing-spot depletion/availability follows the existing personal rules;
- personal rotating merchant stock cannot be emptied by another player;
- one player's Fish Codex/discovery does not auto-complete another's;
- housing ownership/storage is personal;
- a party member may use explicitly shareable camp/rest/cooking functions without taking ownership of the camp/home;
- personal Key Items and quest evidence cannot be stolen/traded unless the quest explicitly permits a tradable ordinary item.

---

# 10. Party HUD / communication UX

Party UI follows the existing Lucifer-family UI language and must remain compact.

Normal HUD shows, for each other party member:

- name;
- compact HP state;
- Downed/defeated indicator;
- approximate direction/range state when useful;
- one restrained class/role icon if readable.

Do not display every buff/debuff/gear stat on the party HUD.

Party communication baseline:

- one contextual world/map ping;
- one shared party waypoint at a time;
- optional direction/range marker;
- pings expire automatically and cannot spam the HUD.

Party waypoint/pings are navigation communication only. They do not reveal undiscovered POIs or grant quest/discovery credit.

---

# 11. Friendly-fire / PvP baseline

The authored RPG launch experience is co-op first.

Baseline:

- direct player-vs-player damage: disabled;
- harmful player-applied statuses on other players: disabled;
- party members cannot body-block a revive interaction intentionally through combat collision rules;
- environmental hazards and authored enemy attacks still affect each player normally.

A future optional PvP mode would require a separate deliberate design. It is not implicitly created by enabling Minecraft's vanilla PvP behavior.

---

# 12. Multiplayer endings / personal story

The host is not the canonical protagonist.

- every player owns their own main-story progression;
- Restore / Release / Partition is a personal ending choice;
- one player's ending cannot overwrite another's;
- shared postgame world remains the stabilized interim world-state already defined in R12 canon;
- dialogue/epilogue presentation follows the player's own choice and optional regional evidence.

This is required for Essential-hosted/private co-op to remain coherent when friends progress at different speeds.

---

# 13. Multiplayer acceptance matrix

Do not declare multiplayer successful until actual multi-client play covers at least:

1. two same-Lv players kill one common enemy and **both independently receive full normal personal EXP/Class XP**;
2. mixed-Lv players kill one enemy and each receives reward from their own level/modifier calculation;
3. a support-heavy player qualifies through healing/barrier/control without damage racing;
4. AFK nearby party member receives nothing;
5. last hit changes no reward ownership;
6. two players on the same quest step both gain valid combat credit;
7. players on different quest steps do not incorrectly sync/skip prerequisites;
8. helper without the quest receives normal encounter rewards but not quest completion;
9. boss scales from actually engaged players, not remote party roster;
10. Downed → revive → boss clear preserves eligibility correctly;
11. eligible boss participant briefly disconnects/rejoins without duplicate/lost committed reward;
12. personal loot/signature drops do not steal another player's roll;
13. personal gathering/fishing/merchant state cannot be consumed by a friend;
14. split party can explore separate regions without changing each other's ordinary enemy HP;
15. independent R12 ending selections coexist in the same shared postgame world.

Verification labels remain strict:

```text
DESIGN REVIEWED != MULTIPLAYER TESTED
BUILD VERIFIED != MULTIPLAYER TESTED
```

Actual Essential-hosted and ordinary dedicated/integrated-server client testing is required when implementation exists.
