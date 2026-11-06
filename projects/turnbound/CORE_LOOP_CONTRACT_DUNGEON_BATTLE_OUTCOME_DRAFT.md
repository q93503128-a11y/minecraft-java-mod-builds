# TURNBOUND — Battle Outcome, Contracts and Dungeon Loop

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

