# TURNBOUND — Dungeon Gameplay Structure: References and Design Options

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
