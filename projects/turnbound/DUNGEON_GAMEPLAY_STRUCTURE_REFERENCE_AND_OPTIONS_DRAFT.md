# TURNBOUND — Dungeon Gameplay Structure: References and Design Options

## Confirmed Minecraft free movement and combat-session room exit gate — 2026-11-06

### Player-confirmed exploration/combat boundary
- **Dungeon traversal remains normal first-person/third-person Minecraft 3D world movement** on actual generated terrain. The player walks, sprints, jumps and explores rooms/corridors directly. Dungeon traversal is **NOT a menu-choice/node-by-node roguelike room selector** and is NOT an instanced separate-dimension dungeon.
- Room contents remain hidden before crossing a room entrance. A room may contain treasure, exploration, environmental interaction or enemies; **not every room forces combat**.
- **Entering a combat room initiates a turn-based party battle; after encounter start the participant cannot physically leave that room until the battle is resolved.** They cannot just sprint/dig/fly/teleport out while combat is unresolved or bypass the encounter without its intended outcome.
- The intended room exit resolutions are **enemy defeat (victory)** or **successful flee (escape)**; a party wipe resolves as a **defeat** and returns the user to the appropriate checkpoint (with the established per-dungeon loss count). A failed probabilistic flee attempt does **not** finish combat or allow leaving.
- **Winning** permits normal 3D exploration from the resolved room and awards earned encounter/room rewards. **Escaping** permits leaving the combat room without those victory/clear rewards and leaves the room uncleared. An immediate reentry must not create an uncontrollable infinite encounter loop; exact escape repositioning/retrigger grace is to be designed.
- The **choice to investigate/enter a branch** is the user's while exploring physical Minecraft space. Once a *combat* room is entered, its encounter is obligatory until victory/escape/defeat, which is separate from requiring combat in every room.
- Preserve same-world local battle presentation with 4 collectible characters fighting; player Minecraft avatar only controls party and is NOT a fifth combatant. Other players and world keep running, and protection from external actors/damage while in the battle UI remains a required technical design issue.

### Implementation boundary
- Recommended execution: server-owned participant battle-session state that denies in-battle exploration actions/position escape and changes 3D controls to turn-battle camera/UI only during combat; it must resolve end states and restore safe Minecraft-world control exactly once.
- Minecraft terrain remains editable in normal dungeon exploration; do not globally forbid mining/building in an entire dungeon merely to stop **in-battle** exits. Define server-side encounter boundaries/eligibility so alternate paths cannot evade an already-active encounter.
- **User has not selected numerical flee success chance, boss flee policy, combat-room trigger shape, default reposition after flee, avatar invulnerability implementation or multiplayer session joining.** Do not silently invent those specifics.
- Previously approved individual per-physical-dungeon **3 defeat → 30 connected-playtime-minute lockout** with loss-free checkpoint return remains in force.
- No new room trigger, UI, collision gate, combat control enforcement, code, build or playtest has been performed. This is a planning/contract change only.


## Dungeon room fog-of-war, compulsory entered-room combat and layered rewards — 2026-11-06

### User-approved direction
- Keep each generated dungeon's **unexplored room contents undisclosed**. Before entering a room, players should not receive exact lists of enemies, rewards or full room types through a map/menu. Discover room content on entry. The dungeon's broad danger/grade may remain visible for fair risk assessment, but the exact encounter is a surprise.
- **Entering a combat room initiates a battle; the player cannot simply cross it as if the combat did not exist.** That local forced encounter is distinct from the otherwise optional choice to enter that room or explore another branch. Not every room must contain combat.
- **Offer layered rewards**: individual victorious encounters, battle-room completion, treasure/reward chambers, major dungeon objective clear, and an extra full-clear reward for thoroughly clearing the dungeon. The user suggested these as reward mechanisms to design further; exact content, stacking rule and award quantities are NOT approved.
- Preserve prior user choice: players can clear the main route only or enter extra rooms and finish every battle for additional rewards; small ruins can lack a boss while major dungeons contain bosses; generated world instances vary theme modules, room graph and difficulty.
- Preserve prior 3-party-defeat limit **per player + specific physical dungeon**, followed by **30 minutes of cumulative connected playtime** lockout at that same dungeon only (not real-world wall-clock or permanent ban), and loss-free checkpoint return.

### Recommended interpretations and safety rules — subject to subsequent approval
- **Entry trigger:** do not auto-start a battle merely because a procedural room loads or a chunk appears. Trigger once when the actual exploring player intentionally crosses a recognized combat-room threshold / engages the combat-room representative, without allowing the player to run straight through uncleared guarded objectives.
- **Escape not silently abolished:** the user proposed probabilistic flee earlier, but final flee odds and whether dungeon rooms allow flee remain UNRESOLVED. If flee is permitted, a successful attempt exits combat without battle/room/full-clear rewards and does not count as a defeat; an uncleared combat room stays uncleared. If fleeing is prohibited in a room, communicate that limitation before the decision is committed. Do not treat 'combat automatically starts on entry' as blanket approval to ban every flee action.
- **Fair discovery:** hide room specifics, not broad dungeon threat. Give atmosphere/indirect danger cues where appropriate without a menu revealing exact rewards. Avoid deceptive rooms that kill a low-level party instantly with no reasonable option to avoid the dungeon itself.
- **Full-clear criterion:** recommend all reachable generated combat-room victories + main dungeon objective, rather than demanding impossible-to-find secret caches, irrelevant safe rooms or constantly respawning field mobs. Count completion at a stable generated-instance ID with stable per-room IDs. Rooms with no battle may offer exploration loot and should not block full-clear unless they carry a clear main objective.
- **Layered payout coherence:** an enemy defeat and battle-room clear can be distinct milestone events, but do not mechanically award the *same* loot twice. Gem and rare-material sources must be rationed across basic/side/full-clear rewards with server-idempotent per-player or shared-loot semantics to prevent overpayment, relog farming or PvE multiplayer disputes.
- **Traversal bypass:** Minecraft players can mine/place blocks, fly or teleport. Do not convert the whole dungeon into indestructible no-build zones by default. Server-owned guarded objectives/loot can be claimed only after encounter victory, regardless of reaching a physical reward room by an alternate path.
- **Seeded generated truth:** room type, placement, reward identity and combat selection must be generated/stored persistently per dungeon instance and not rerolled on reopen, relog, interaction spam or retry. In multiplayer the physical world/layout is shared; whether cleared-room states and major rewards are personal or shared needs further design.
- **Anti-chain-combat UX:** battle must not trigger just from standing still after battle resolution; mark room completed and provide safe breathing room. Thresholds and door behavior must support player movement and camera visibility on real world geometry. Exact interactions/visuals await actual Minecraft playtest.
- **Dungeon loss rule:** only final party defeats add 1 to that individual's 3-loss counter; successful combat, voluntarily leaving and successful escape do not. Rewards already legitimately claimed should remain after later defeats; no inventory/currency loss.

### Next design issues still open
- Definition of a valid 'room entry' in an editable survival world and the interaction between fight triggering, escape and physical exiting.
- Which specific room categories to implement; randomization weights, number of encounters, main-goal variability and how often a dungeon can be all-cleared.
- Whether major treasure chambers are reachable without fighting, whether full-clear reward can be re-earned, first-clear versus replay benefits and whether older dungeon instances replenish.
- Exact Gold/gem/material allocation across ordinary kills, room rewards and main/full-clear rewards, and quest overlap — only after content pacing is designed.
- Multiplay looting policy, per-player versus party clear credit, instance locking & checkpoint resolution.

### Status
Documentation only; no combat-room trigger, loot-room actor, procedural reveal masking, reward settlement, full-clear state, Java tests, new JAR or Minecraft playtest was run here.


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


**CURRENT USER-APPROVED DUNGEON DIRECTION BELOW OVERRIDES ANY OLDER 'UNDECIDED' BOSS/BRANCH NOTES.**

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
Status: **RESEARCH + PROPOSAL ONLY; user has approved a deeper design discussion, NOT final mechanics**
No implementation, build, JAR or playtest in this step.

## A. Existing confirmed constraints (do not re-open without a player decision)

- World-generated discoverable dungeons and ruins in the normal Minecraft Java overworld: no requirement to teleport to a distinct dungeon dimension or join a separate instanced arena.
- In-world tactical encounters using the original TURNBOUND turn-gauge system; 4-character party; Minecraft avatar handles exploration and crafting, not turn battle actions.
- World and other multiplayer users continue in real time, so combat must protect avatar, battle sessions, encounter ownership and reward settlement; an actual client/multiplayer test has not established a solution.
- Dungeons should be rewarding to discover and explore independently of accepting a quest. Dungeons are a major source of summoning gems, plus valuable equipment materials/treasure. Gold and gems have distinct principal economic roles but no locked amounts.
- Defeat returns player to a checkpoint with no loss of carried items, RPG currencies, XP or equipment; no battle victory/clear loot is awarded for a lost fight. Checkpoint behavior at dungeon entrance and in multiplayer is still to design.
- Contracts may provide clues about real generated dungeons, but the precise villager versus entrance-NPC presentation is NOT yet user-confirmed.
- Mob density and encounter triggering must avoid repeatedly interrupting movement. Enemies warn (!), chase within bounds, and physical contact transitions into an actual-terrain battle.
- Dungeon sizes, number of types, roster, bosses, exact rewards, rarity/biome/size tiers, clear times and repeat policy remain unconfirmed.

## B. External mod/design references checked 2026-11-06

| Reference | Verified feature | Transferable idea | Compatibility / caution |
| --- | --- | --- | --- |
| YUNG's Better Dungeons | Catacombs, Fortresses of the Undead, Spider Caves, richer dungeon generation | Strongly recognizable themed layouts, alternate routes and layered interiors | https://modrinth.com/mod/yungs-better-dungeons ; site currently lists 26.1.x, NOT evidence of NeoForge 26.2 binary compatibility; borrow design, not jar |
| When Dungeons Arise | Dozens of imposing structures, palace/airship; certain releases use modular generated mechanical city with many randomized parts | Discovery-driven spectacle, many plausible dungeon appearances, authored modular structure pieces | https://modrinth.com/mod/when-dungeons-arise ; https://modrinth.com/mod/when-dungeons-arise/version/wiCQJUDT ; listed version availability does not prove 26.2 compatibility; author's ARR resource license does not permit copying content |
| Vault Hunters official wiki | Varied Vault Objectives including item scavenging, boss powers, lighting braziers and mixed mining/looting/combat goal | Vary gameplay verbs: hunt, gather, explore, locate, defend, collect a central treasure | https://wiki.vaulthunters.gg/Vault_Objectives ; its separate-vault-run/objective timer mechanics are not automatically suitable for permanent Minecraft overworld |

These references are inspiration only, not dependencies, finalized content or direct asset-use approvals.

## C. Proposed dungeon experience (NOT approved)

### Discovery
- Find distinctive structure through ordinary travel, caves or credible NPC clues. No mandatory contract needed. Distinct silhouettes and local world clues make it possible to spot/remember a location without teleporting.
- Overworld spawn/biome suitability and placement must not destroy old player bases, existing saved chunks or functional Minecraft vanilla infrastructure.

### Entry / exploration / climactic reward / exit
1. **Spot and assess** threat; signal approximate danger without overwhelming numbers-heavy UI or invisible boss triggers.
2. **Enter real geometry** with exploration resources and current party; optionally reach a local checkpoint, not a separate dungeon dimension.
3. **Explore a purposeful room graph** with a readable route to a major goal plus optional side branches and alternate paths. Never require a full maze of empty identical hallways.
4. **Encounter varied but finite meaningful activities:** an alerted hostile pocket, safe exploration/viewpoint, material pocket, side cache, environmental interaction or a distinct tactical elite. Do not require a separate puzzle minigame in every dungeon.
5. **Meaningful culmination:** combatant/boss, rare treasure, investigation or specialist challenge according to the structure's identity. **Whether every dungeon must have a boss is not decided.**
6. **Claim progression rewards**, with one-time major gem/treasure credit tied to server-owned world/structure and player ownership. Exit or retrace without repeated unavoidable encounters.

### A compact functional room vocabulary
- **Approach / orientation:** clear physical entrance and threat cues.
- **Encounter:** a battle zone with party and enemy placements, attack/camera visibility and retreat space; not a monster-spawner tunnel.
- **Optional risk / cache:** side path that exchanges more danger/attention for rare ores, crafting materials or a valuable chest.
- **Exploration/interaction:** discover clues, switches, power conduits, hidden shortcuts, or visually discoverable room interactions; only if grounded in the structure theme.
- **Anchor / culmination:** rare artifact, miniboss, boss or final treasure, not always the same key-door-boss box.

All are optional design roles, not a required per-dungeon room count.

## D. Player choice and world interaction

- **Example branch:** visible elite guards high-value ore/cache on left, quieter longer route with less side treasure on right; player can decide whether to invest time/turn combat for extra reward.
- **Do not use block-breaking bans as the universal fix** for survival players digging, placing blocks, flying, using pearls or breaking doors. The major quest/first-clear reward can require server-confirmed objective completion rather than mere chest reach; exploratory materials can remain accessible through Minecraft play.
- Bypassable unnecessary battles are acceptable to a point. If the main reward requires a guardian/objective, communicate this in-world; do not claim a player can skip the guardian and still claim that same completion.
- The world is persistent: no blanket structural rollback that destroys player changes after a run.

## E. Reward and replay strategy (proposal, not payout contract)

- **Exploration rewards along the way:** ores, monster parts and useful materials so the visit is worth it even before reaching the final chamber.
- **One-time meaningful clear/discovery reward:** proportionate gems, Gold, rare material or a recipe as content warrants. Gem award should not be duplicated by re-opening a chest, re-logging or multiple contract providers.
- **Repeat value through a changing exploration world**, finding new generated dungeon structures, not necessarily endlessly resetting the exact same permanent site. Whether existing dungeon encounters/spawners replenish and with what reward cap needs explicit agreement.
- Multiplayer needs explicit per-player versus per-party eligibility and server idempotence; physical chest content should not be confused with personal milestone rewards.
- This interacts with stochastic contract availability; keep long-run gem supply sustainable without one trivial dungeon becoming an unlimited gem source.

## F. Outstanding decisions to discuss IN ORDER

1. **Must every dungeon end with a boss, or should only substantial/rare dungeons demand a boss while smaller ruins reward exploration/miniboss/treasure?** This determines room budgets, pacing and how many battles a discovery implies.
2. Do players want meaningful optional branches and hidden loot, or primarily short reliable combat-to-boss runs? Keep a coherent exploration experience; do not create obligatory long puzzles.
3. How should dungeon entrance/interior checkpoints and immediate defeat replay work, given the approved no-loss defeat rule and anti-fast-travel concern?
4. What does 'clear' mean (one-time physical structure, per-player accomplishment) and whether an explored dungeon ever respawns or changes?
5. How to generate modular rooms/structures with battle-camera headroom while remaining safe for mining, building and server multiplayer.
6. Only after the game loop is approved: structure types, environmental themes, unique mechanics, monster kits, bosses, specific reward pools, gem quantities and drop probabilities.

**This document is NOT an approval to implement any particular number of dungeons, tiers, bosses or reward values.**
