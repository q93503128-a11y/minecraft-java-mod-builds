# TURNBOUND — Contract Grade, World Generation and Dungeon Discovery

Date: 2026-11-06
Project: TURNBOUND `projects/turnbound/`
Branch: `turnbound-content-rebuild`
Status: Partial user-approved direction; proposals explicitly separated; NO CODE IMPLEMENTED

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
