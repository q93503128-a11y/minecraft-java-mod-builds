# TURNBOUND — Battle Outcome, Contracts and Dungeon Loop

## Confirmed three-defeat, 30-online-minute individual dungeon lockout — 2026-11-06

### User decisions superseding old open lockout duration
- A character party's **third full battle defeat in one specific generated dungeon instance** imposes a **30-minute admission lockout for that exact dungeon and player only**. This is **not a permanent ban**, and it does not affect other dungeon instances even when sharing the same theme or structure template.
- Time is measured in **cumulative time connected to the Minecraft server/world**, **not wall-clock time**. Offline time does **not** reduce the remaining lock. The remaining duration is retained across player logout/login and server restarts.
- After accumulating **30 minutes of connected server play**, the dungeon becomes accessible again and its per-player defeat count resets to **0/3**. Repeated future defeats can trigger a new independent 30-online-minute lock.
- The triggering third defeat follows existing user-confirmed loss-free defeat behavior: teleport to a valid checkpoint **outside that prohibited dungeon**, retain inventory, party equipment, XP, Gold and gems; no unearned victory or dungeon-clear rewards. Other players' own admission and defeat counts remain unaffected.
- Count only **actual party defeat outcomes** during that dungeon's battles. One fainted party member, a successful escape, voluntarily leaving a dungeon, or a successful battle does not increase defeat count by itself.
- A lock is an **entry/admission constraint** and should be enforced server-side over actual structure bounds/entrances, not only by locking the visible entrance door; otherwise mining, flying, teleportation or log-in-position exploits could bypass it. Avoid restricting unrelated overworld construction/traversal.
- Persist keyed **player identity + physical dungeon instance identifier + loss count + remaining online lock time**; time should decrement once per eligible online server tick (or equivalent trusted server-side elapsed session accounting) without client-provided timing. Count/entry state changes must be saved durably and handled correctly across world unload/relog/disconnect.
- **No separate real-time expiry, gem-paid reset, daily reset or permanent access denial** is authorized. A remaining 18-minute example after playing 12 minutes and disconnecting follows the same counter when reconnecting.
- New initial design specifics (checkpoints, dungeon structure bounds, UI, single-player pause semantics, actual time source, multiplayer interactions and playtest balancing) require implementation review. Online clock includes connection time; no AFK penalty or AFK-exclusion rule is approved.
- This is **design-only**; no running Java timer, lockout gate, persistence migration, structure registry, GUI, automated test, build or live Minecraft playtest has been performed.


## Confirmed frequent procedural dungeons, optional full clears and three-defeat instance lockout — 2026-11-06

### User-approved gameplay direction
- **Dungeon presence:** the normal generated survival world should contain **many discoverable dungeon sites**. Prioritize world coverage and rewarding exploration rather than locking in a large number of distinct handcrafted themes. The actual structure frequency, spacing/biome weights and world count remain tuning decisions, not approved numbers.
- **Procedural variants within a recognizable theme:** allow dungeon instances sharing a concept/visual identity to have meaningfully different spatial layouts, room composition, enemy/elite placements, objective path lengths, difficulty, encounter mix and loot choices. Reuse high-quality authored modules assembled with controlled generation; avoid random shape soup, unreachable rewards or generic repetitive hallways.
- **Combat is not required in every room.** There is a readable main route plus optional branches; the player may choose to defeat extra enemies, explore more chambers and **clear all available encounters/loot for maximum rewards**. Some guards or objectives may still be mandatory for a specific main-clear reward.
- **Boss mix:** small ruins/exploration sites need not have a boss; bigger/significant dungeons should feature a climactic boss and richer rewards, without fixing a taxonomy or exact number of rooms/structures.
- **Within a specific physical generated dungeon, three party-battle defeats trigger a ban on entering THAT dungeon.** Other instances, including the same theme elsewhere in the world, remain accessible. Do not impose the lock across an entire dungeon type or across the player's world.
- Regular defeat still returns the Minecraft avatar to a valid checkpoint with **no inventory, character XP, Gold, gem or equipment loss**; this dungeon-specific access loss is the intended non-monetary consequence. A defeat is not a victorious clear and gives no unearned battle rewards.
- Dungeon entry does **not** require an NPC/contract. Dungeons are worthwhile on discovery, support gem earning, and should offer useful side-room rewards besides their major clear rewards.
- The player explicitly accepted the previously suggested exploration/branching/reward framework and the small-site boss exemption / major-dungeon boss direction. Other unapproved numeric content, spawn tables and named dungeon identities remain open.

### Recommended implementation and safety constraints (not extra rules the user explicitly finalized)
- Assign each generated physical dungeon a persistent **instance UUID or stable world/dimension + generated-structure identifier**, independent of the visual theme. Keep a **per-player, per-instance defeat count** (0–3+) and an admission flag; persist across relogs and server restarts so reentering cannot reset the count. Avoid counting every character faint as a party defeat; count only a final lost battle in that structure.
- Minecraft is a destructible shared world, so an intended 'entry ban' needs **server-authoritative region/admission enforcement**, including digging, flying, teleporting, alternate entrances and reconnect positions; a mere locked entrance door is not sufficient. Avoid permanently preventing players from modifying other parts of the world.
- On the **third defeat**, select a safe checkpoint *outside the now-forbidden instance*; do not return the player to an interior checkpoint and trap them. Previously approved loss-free checkpoint behavior remains, and no additional death penalty should be invented.
- **Duration/unlock semantics are unresolved:** the user required denial after 3 defeats but did not explicitly select permanent lockout versus a future way to regain access. Do not quietly assume a timed cooldown, paid reset, or permanent account-level loss. Clarify at a later design decision before implementing.
- Other players' own defeat counters should remain independent by default; actual multiplayer instance ownership, looting, bosses and dungeon mutation require explicit server-world policies.
- Distinguish **dungeon entry** from **combat entry/exit**. Leaving voluntarily should not automatically count as a defeat, and intentionally retreating through a normal exit should not spend a defeat unless the user later approves it. Fleeing inside a battle remains a separate pending design.
- Worldgen should use authored reusable rooms/templates and weighted variation with graph/path validity checks. Java Jigsaw/template-pool worldgen is a plausible basis. Real-world generated chunks must remain stable for a given seed and saved terrain; do not regenerate on login or player deaths. Worldgen must avoid overwriting saved builds/chunks and significant terrain/cave damage.
- Balance high structure availability against exploration surprise, visual clutter, server chunk generation cost and loot inflation: no arbitrary claim that a numerical spacing/density is already tuned.
- Track major first-clear and side loot independently, so optional combat rewards can be collected once as designed while duplicated gems on relog, chest replacement, or team contention are prevented. Exact repeat/refill rules and per-player/per-party first-clear status are unsettled.
- Room/battle-geometry validators should check reachability, lack of accidental seals, exit routes, no liquid/lava softlocks, camera headroom and four-actor party placement on actual terrain.
- References for technical concept only (not audited NeoForge 26.2 APIs):
  https://minecraft.wiki/w/Jigsaw_structure
  https://minecraft.wiki/w/Template_pool
  https://minecraft.wiki/w/Tutorial:Custom_structures

### Still open (do not assume final design)
- Whether 3-loss dungeon exclusion is permanent or recoverable; what can reset it, if anything.
- How generated instance bounds and multiple entrances are represented and enforced, how checkpoints interact with nearby structures, and how multiplayer physical loot works.
- Exact distribution, density, biome-specific themes, difficulty parameters and whether depth/biome determines enemy strength; no fixed dungeon count or genre-specific labels.
- Rules for side-room loot, optional rare battles, clearing all rooms, previously looted sites and re-encounter spawning.
- Exact dungeon list, bosses, rewards, gems, durations, structural art, loot, NPC leads and crafting-material identities.

### Status
**DOCUMENTED ONLY.** No worldgen/room generator, lockout persistence, access gate, checkpoint behavior, loot attribution or actual playable dungeon has been implemented or tested as a consequence of this decision.


Date: 2026-11-06
Project: `projects/turnbound/` on `turnbound-content-rebuild`
Status: CONFIRMED high-level direction + design proposals, NOT IMPLEMENTED

## Field battle failure, possible flee, contracts and dungeons — 2026-11-06
### User-confirmed gameplay outcomes
- **Party defeat:** return the Minecraft explorer avatar to the most recent applicable checkpoint. There is **no defeat penalty**: no inventory loss, dropped tools, Gold/gacha-gem deduction, XP/character level loss or equipment destruction. Defeat is a battle outcome, **not a vanilla player-death/drop event**.
- A failed battle grants no victory loot, contract kill credit, dungeon-clear status or one-time clear bonus. This distinction is not a punishment or preexisting resource deduction; it simply does not award a victory.
- Checkpoint activation, initial fallback, boss-specific checkpoint placement, recovery of the defeated party's HP and the non-teleporting behavior in multiplayer require design before implementation. No loss of progress or overworld building is intended.
- **Contracts:** the player wants contract difficulty to influence **Gold and gem payouts**. Gems are principally for character summoning, and Gold is principally for other RPG services/equipment/transactions, but the exact currency uses and award rules remain **flexible, not fully fixed**. No amount or difficulty rating has been chosen.
- **Dungeons:** discovery should be intrinsically rewarding, with worthwhile loot; they are one of the major gem-earning content sources. Dungeon layout, duration, repeatability, difficulty, resource drops and payout numbers have not been fixed.

### Recommendations under discussion — NOT yet user-approved details
- **Probabilistic flee:** the user raised escape chance because free, guaranteed fleeing would trivialize world navigation. Recommend a chance-based escape attempt: on success, exit at the encounter location; on failure, the attempt costs an action/opportunity and the enemy can pressure the party. Do not set a numeric percentage until tested. Whether bosses disallow fleeing or modify odds remains open.
- **Retry-loop protection:** a small escape probability alone cannot prevent unlimited escape attempts, so failed attempts should create actual time/turn pressure; escaping should not award clear rewards or allow a bypass of required boss/encounter victories.
- **Combined NPC + quest-board workflow:** local boards list repeatable regular jobs, while villagers/other named NPCs can offer memorable or specialist tasks. Both use the same underlying server-owned contract records, journal and payouts to avoid duplicated systems. This is preferred over making the player browse several disconnected interfaces.
- **Reward directions:** first dungeon clears should feel valuable; later repeats can still provide materials and useful progress, but do not lock all gems behind one endlessly repeated easy dungeon. Exact first-clear and repeat payouts remain to be designed, not an approved economy.
- **Protect the overworld avatar during battle:** since the Minecraft world does not globally pause and the player is directing four characters rather than fighting as an avatar, define the avatar's physical vulnerability to mobs, hazards, other players, world changes, disconnect and death; server authority must prevent exploits and double settlements.
- **Checkpoint exploit consideration:** loss-free defeat returning to a checkpoint might be used as free fast travel. Assess its impact in real play; do not introduce an unrequested item or currency death penalty as a workaround.

### Existing technical reference and scope
- On current source, `BattleSessionManager.command` treats `FLEE` as an unconditional session end when `fleeAllowed()`, not a probabilistic retreat action. `BattleOutcome` currently has `RUNNING`, `ALLY_VICTORY`, `ENEMY_VICTORY`; fleeing is not a modeled outcome. Adding chance/failure costs requires real gameplay changes, not merely a value tweak.
- Old `RegionalContractService` demonstrates NPC-delivered repeatable region tasks with Gold/party XP, but it is legacy authored gameplay and is NOT active new-game content. Its architectural ideas may be reused after review, without importing its old map, NPCs, payout values or forced story requirements.
- This checkpoint records **content direction only**. No battle defeat checkpoint teleport, flee probability, new NPC or noticeboard, dungeon spawn/rewards, or gem settlement is implemented/tested in the rebuilt empty content.

### Next co-design unit
- Pick a **contract acquisition UX** (proposed combined noticeboard + NPC), then detail dungeon exploration decisions and checkpoint placements before setting reward amounts or summon-currency pacing.


## Confirmed unified contract acquisition — 2026-11-06
- **Both village noticeboards and resident NPCs issue contracts.** Noticeboards are the principal access point for routine/open contracts; residents/NPCs can offer personal, place-specific or special contracts.
- They share **one contract system**: common objective tracking, acceptance/completion state, player journal and authoritative reward settlement. Do not create parallel unrelated quest economies or duplicate credit just because two sources expose a contract.
- Main story is not required; contracts remain optional activities in the Minecraft survival + turn-battle loop.
- Reward direction already chosen: difficulty-sensitive Gold and summon gems; exact payout amounts, difficulty brackets and available job count remain open. Gold primarily serves other progression/services, gems primarily serve character summons, without permanently freezing every possible use.
- **Unresolved content design:** job categories and objectives, contract generation/availability, delivery and hand-in rules, limits on concurrent/recurring jobs, cancellation, dungeon integration, NPC interaction details and any exceptional reward cases. Do not manufacture a fixed initial contract count.
- **Design documentation only**: NPC/board contract UI, world registrations, new rewards and multiplayer state are not implemented or playtested.

## Graded stochastic contracts and dungeon quest discovery — 2026-11-06

### User-chosen direction and user proposals
- **Replace reward stereotypes by objective type with an independent quest grade/difficulty axis.** Gold and summoning gems are awarded according to contract difficulty/grade and actual challenge; killing, collection, exploration and dungeon tasks are not permanently Gold-only or gem-only categories. Exact grade labels, bracket counts, drop tables and pay amounts are not fixed.
- **Ordinary contracts** may originate from either village noticeboards **or suitable villager NPCs**; **special contracts** originate only from NPCs or special world interactions/events, not the ordinary noticeboard listing.
- **Both ordinary and special contracts should have probabilistic occurrence/availability.** Villagers that offer a contract should vary across world-generated villages; no universal permanently fixed quest-giver list, no requirement that every villager offers work.
- User prefers **not** to create a separate category of identically repeatable contracts. Distinguish each completed/consumed individual contract from future newly generated contracts. Infinite copies of a single trivial request should not become the primary gems economy.
- **Dungeons are worthwhile independently of quests**, and they are a primary source of summoning gems. Whether a dungeon-related quest points toward its location from a resident or begins via an NPC/event at the entrance is a **question still under co-design**, not a resolved user requirement.
- The player's previous acceptance of unified quest tracking and NPC/noticeboard acquisition still applies; the newer special-contract exception supersedes earlier blanket statements implying every quest may appear on either surface.

### Design recommendations (NOT user-approved binding specifics)
- Represent grade independently from mission kind; calculate grade using actual threat, travel/risk, dungeon difficulty and required effort. Higher grade should improve the expected total value of Gold and gems, not necessarily require identical rewards for every mission. Do not substitute a fixed drop value based solely on objective type.
- For grade naming, E/D/C/B/A/S is only an illustrative UI scheme; **no letters or tier counts are approved**.
- Create ordinary contracts as **persisted instances** with objective, region, giver/source, grade, designated targets, completion and claim state. Generate new instances from region-appropriate event/templates when world conditions allow; never re-roll an existing offer simply by clicking an NPC repeatedly, rejoining or breaking/replacing a board.
- A special contract may appear with a low but nonzero chance after an eligible world occurrence (expedition clue, rare encounter, dungeon lead, NPC incident), rather than on every random villager conversation. Keep sufficient authored variety and meaningful stages so mere random parameter swaps do not masquerade as content.
- Avoid a dedicated daily/weekly contract reset or requiring the exact same completed job over and over. **Economic caveat:** if one-off jobs are finite, a primary summoning currency runs out. Continually generated *new distinct contract instances* are a candidate solution, but their quality and supply need playtesting. No specific respawn timer, generation rate, cap or guaranteed minimum is approved.
- **Dungeon-related lead distribution:** recommended majority mode is a villager/contract informant describing a rough region, landmarks or route to a **real existing world-generated dungeon**, allowing exploration and discovery. A rarer entrance-site clue or distressed NPC can initiate a **special event/quest** at the dungeon itself. Avoid one generic NPC standing in front of every dungeon. A dungeon remains enterable and rewarding if found without a quest. These are suggestions, **not yet agreed user decisions**.
- Quest-to-dungeon pointers MUST resolve to a real generated structure rather than pointing to a nonexistent or already exhausted dungeon; per-player multiplayer clear semantics and worldgen chunk availability need implementation design.
- Reward stability: no gem duplication from multiple quest-giver surfaces, overlapping objectives, board refreshing, villager multiplication, reconnection or repeatedly completing the same persisted instance. Per-player completion and multiplayer target ownership require explicit rules.
- Design the objective verbs and actual player choices before finalizing numerical gem/hour expectations or assigning fixed counts to quest types, NPCs or dungeons.

### Supporting game comparisons, not implementation copies
- *Monster Hunter: World* investigation quests can be generated from monster traces/activities, relate to a field area and provide better rewards with difficulty. Japanese/Korean game tutorial transcription: https://mhw.poedb.tw/kor/panel_tutorial ; community investigation overview: https://monsterhunterwiki.org/wiki/MHWorld/Quests .
- *The Witcher 3* links noticeboard contracts to clients and discoveries; notices can direct players to locations. Reference catalog: https://witcher-games.fandom.com/wiki/Notice_board and https://witcher-games.fandom.com/wiki/The_Witcher_3_contracts .
- These are **design references**, not approvals to copy private code/content or assets.

### Implementation state and next gate
This is a **design document change only**. No procedural NPC offers, board generation, special events, quest grade/reward engine, dungeon lead system, new state persistence or gem settlement exists in the new-game empty runtime yet.
Before implementing, co-design **the contract lifecycle**, **frequency/eligibility by region** and **dungeon discovery** through actual in-game choices; keep all numerical payouts and probabilities open.


## Dungeon exploration design reference checkpoint — 2026-11-06
- Deeper dungeon design research and **unapproved proposed gameplay structure** recorded in `DUNGEON_GAMEPLAY_STRUCTURE_REFERENCE_AND_OPTIONS_DRAFT.md`.
- External inspirations: YUNG's Better Dungeons (recognizable layered themed structures), When Dungeons Arise (discoverable spectacle with authored modular pieces), Vault Hunters (varied dungeon objectives); do not copy code/assets or claim current TURNBOUND 26.2 compatibility.
- **No new dungeon mechanic has yet been user-approved by this research alone.** Next co-design pivot: whether every dungeon needs a boss or only the major ones; preserve dungeon discoverability without mandatory quests, meaningful gems and reward ownership, loss-free defeat, and same-world party turn battles.
- Design/documentation work only, no Java changes or tests/build/playtest.
