# Changelog

## 0.15.0-alpha.1
- Reworked Woodcutting bulk felling around Veinminer++ MIT smart-tree safety: connected logs are gathered first and a bulk job is only created when leaves are face-adjacent to the origin or gathered log set.
- Plain player-built log structures without attached leaves now stay single-block even at high Woodcutting levels.
- Replaced synchronous 16/48/128/256-log chain destruction with tick-drained jobs: 12 logs/player/tick and 64 logs globally/tick.
- Woodcutting queued breaks retain normal `player.gameMode.destroyBlock`, tool checks, block-entity exclusion, normal drops/durability and per-block skill XP; an internal guard prevents recursive jobs.
- Added the third shared infrastructure project, Builder Foundry: 1024 stone bricks + 256 iron + 256 copper + 128 redstone + 64 obsidian.
- Completing Builder Foundry plus Construction Lv.90 unlocks Volume mode in M -> Construction.
- Volume fills a 5x5x5 cube centered on the player's placed block, excluding the already-placed origin (max 124 secondary placements).
- Volume reuses the existing material-backed protected Construction queue: real inventory blocks, mayInteract, NeoForge placement hook, survival checks, 256 pending/player cap and tick-distributed placement.
- Construction and Infrastructure radials now expose Volume and Builder Foundry respectively.
- Extended the existing Veinminer++ MIT attribution to cover smart-tree safety and tick-drained large-tree work.

## 0.14.0-alpha.1
- Added world-shared infrastructure projects persisted through a new `infrastructure_v1` SavedData; multiplayer contributions share the same server-world progress.
- Added M -> Infrastructure MineMenu-derived radial with Quarry Network / Irrigation Works / Status / Back.
- Funding is server-authoritative and consumes only currently required materials from the player's real inventory; creative/spectator funding is rejected.
- Quarry Network costs 1024 cobblestone, 256 iron, 128 redstone and 32 diamonds.
- Completing Quarry Network plus Mining Lv.90 unlocks Tunnel mining mode: a 5x5 cross-section across 8 blocks of depth.
- Tunnel work is tick-budgeted at 12 blocks per player per tick, 64 globally, with at most 256 pending targets per player.
- Tunnel secondary breaks use normal `player.gameMode.destroyBlock`, exclude block entities/unloaded chunks/unharvestable or excessively hard targets, and are guarded against recursive tunnel scheduling while still awarding normal per-block Mining XP.
- Irrigation Works costs 512 copper, 128 iron, 128 redstone, 128 glass and 32 slimeballs.
- Completing Irrigation Works plus Harvesting Lv.30 enables automatic replant for wheat, carrots, potatoes, beetroot and nether wart harvested with a hoe.
- Replant consumes one real matching seed/crop item per block and rechecks loaded chunk, replaceability, survival, `mayInteract` and the NeoForge placement hook before planting.
- Melon and pumpkin are not replanted because their stems already persist and regrow fruit.
- Simplified the M main wheel from nine entries to seven: Skills / Mining / Construction / Equipment / Infrastructure / Guide / Close. Unlocks, Stats and Controls remain as Guide tabs.
- Network protocol bumped to 7; source/JAR audits now enforce infrastructure persistence, funding safety, tunnel recursion protection and seed-backed replant rules.
- Studied Create's staged infrastructure/resource-throughput progression as a design reference. Create code is MIT and its assets are All Rights Reserved; 0.14 bundles neither Create code nor assets.

## 0.13.0-alpha.1
- Added M -> Mining nested MineMenu-derived radial with Auto / Plane / Vein / Extract / Back.
- Mining mode selection is server-authoritative, persisted on the player, and server-revalidated against Mining level.
- Auto preserves the previous behavior: valuable ore prioritizes connected vein extraction, other blocks use view-aligned area excavation.
- Plane (Lv.10) forces view-aligned excavation even on ore; Vein (Lv.30) expands only connected same-family valuable ore.
- Extract (Lv.90) performs a target-filtered bounded search for the same valuable ore family within X/Z ±12 and Y ±12, even when deposits are not connected.
- Extract only examines loaded chunks, excludes block entities and unharvestable targets, and uses the existing skill/affix vein limit as its hard destruction cap.
- All Extract targets still go through the normal player destroy controller, preserving durability, enchantment/loot behavior, events and per-block skill XP.
- Shift continues to force 1x1 precision regardless of the selected Mining mode.
- Adapted the target-filtered bounded-search idea from Mekanism's MIT-licensed Digital Miner design and packaged the Mekanism MIT notice; no Mekanism assets/machines/energy/GUI are bundled.
- Network protocol bumped to 6 and in-game guide/help text now documents Mining modes.

## 0.12.0-alpha.1
- Expanded each affix category from three possible affixes to five while preserving 0.11 CustomData compatibility.
- Elite / Ascended / Mythic gear now rolls 1 / 2 / 3 affixes from a five-affix pool, so Mythic rerolls remain meaningful instead of always owning every affix.
- Added category-specific secondary/utility affixes: Hunter/Impact, Vein/Precision, Felling/Precision, Bounty/Precision.
- Split Mining scale so excavation area and vein capacity can roll independently; Woodcutting and Harvesting can stack primary scale with secondary specialization.
- Added M -> Equipment nested MineMenu-derived radial with Reforge / Salvage / Gear Info / Back.
- Added server-authoritative EquipmentAction payload; client requests never directly alter inventory or item data.
- Reforge preserves the held item's base item and non-affix components while rerolling only the Survival Ascension affix payload.
- Reforge costs create a high-volume resource sink: Elite 16 amethyst + 8 iron, Ascended 32 amethyst + 6 diamonds, Mythic 64 amethyst + 12 diamonds + 2 netherite scraps.
- Salvage destroys the held affix gear and returns only a partial material refund; creative salvage rewards are disabled to prevent duplication.
- Network protocol bumped to 5 and source/JAR audits now require the equipment radial, action payload, reforge service and five-affix contract.

## 0.11.0-alpha.1
- Added an Apotheosis-inspired rarity/category/affix loot loop using Minecraft 26.2 CustomData rather than replacing vanilla item classes.
- Elite I / Ascended II / Mythic III affix gear uses iron / diamond / netherite bases and carries 1 / 2 / 3 distinct affixes.
- Elite ranks gain an additional affix-gear drop chance of 25% / 65% / 100% while retaining existing material and XP rewards.
- Weapon affixes: Destruction boosts direct Combat damage, Cleave expands already-unlocked cleave targets/fraction, Mastery boosts Combat XP.
- Pickaxe affixes: Haste boosts break speed, Excavation expands already-unlocked area/vein extraction, Mastery boosts Mining XP.
- Axe affixes: Haste boosts log break speed, Chain expands already-unlocked connected-log limits, Mastery boosts Woodcutting XP.
- Hoe affixes: Haste boosts crop break speed, Area expands already-unlocked harvest area, Mastery boosts Harvesting XP.
- Scale affixes never unlock a skill's scaled action early; they only enlarge actions already unlocked by skill progression.
- Affix data is stored in item CustomData and remains visible through rarity + affix names in the item's custom display name.
- Added packaged Apotheosis MIT attribution.

## 0.10.0-alpha.1
- Upgraded elite traits from mostly passive modifiers into reactive combat patterns.
- Swift elites perform rank-scaled lateral evasions after player hits.
- Bulwark elites counter-push the attacking player instead of only stacking armor.
- Berserker elites lunge back toward attackers while below half health, in addition to low-health damage amplification.
- Vampiric elites retain real post-mitigation player-damage healing.
- Added persistent reaction cooldowns: Elite I 60 ticks, Ascended II 45, Mythic III 30.
- Added tangible rank loot: Elite I gold nuggets, Ascended II emeralds, Mythic III diamond + emerald bundle.
- Existing rank XP rewards, progression-scaled spawn odds and spawner anti-farm rules remain intact.
- Majrusz's Progressive Difficulty was studied only as a progression/difficulty-design reference because no explicit reuse license was adopted.

## 0.9.0-alpha.1
- Added progression-scaled elite hostile mobs so the world grows with player power instead of remaining static.
- Nearby players' average level across all six skills drives elite spawn chance and higher-rank odds.
- Added three persistent ranks: Elite I, Ascended II, Mythic III.
- Rank bonuses combine max health, armor, movement speed, attack damage and knockback resistance instead of health-only scaling.
- Added four persistent combat traits: Swift, Bulwark, Vampiric and Berserker.
- Berserkers gain additional low-health damage; Vampiric elites heal from actual health damage dealt to players.
- Mythic spawns announce themselves to nearby players and mythic kills grant additional vanilla XP.
- Spawner-origin mobs are excluded from elite assignment to prevent repeatable elite-reward farms.
- Elite rank/trait data uses persistent entity NBT and permanent attribute modifiers.
- Adapted rank/permanent-attribute patterns from Mob Champions 26.2 under MIT and packaged its notice.

## 0.8.0-alpha.1
- Activated Mobility as the sixth live skill.
- Added server-tracked on-foot sprint-distance XP; teleport, flight, swimming and riding do not count.
- Added modest continuous movement-speed growth through vanilla movement-speed attributes.
- Lv.10 unlocks 1-block step traversal and increased safe-fall distance.
- Lv.30 unlocks the R ground dash with a server-authoritative cooldown.
- Lv.60 unlocks one R air dash before landing and improves traversal attributes.
- Lv.90 upgrades dash power/cooldown, step height and safe-fall distance again.
- Movement impulses and cooldowns are validated on the server; the client only sends the action request.
- Studied ParCool's public parkour/action vocabulary as reference-only; no LGPL ParCool source or assets are copied or bundled.

## 0.7.0-alpha.1
- Activated Construction as the fifth live skill.
- Added M -> Construction nested radial with Single / Line / Wall / Floor / Back.
- Added level-gated build scale: line 5/9/17/33 and wall/floor 3x3/5x5/9x9.
- Added real inventory material consumption for secondary placements.
- Added server-authoritative construction-mode networking and server-side level validation.
- Added mayInteract + NeoForge placement-hook protection checks before bulk placement.
- Added a global tick budget and per-player pending cap so high-level 9x9 construction is distributed across server ticks.
- Shift now forces precision single placement for Construction too.
- Added Building Gadgets 2 MIT attribution for placement-safety/work-queue reference patterns.

## 0.6.0-alpha.1
- Replaced the direct K skills shortcut with M as the integrated menu key.
- Added the MineMenu MIT-derived radial interaction/presentation.
- Added Skills, Guide, Unlocks, Stats, Controls and Close radial entries.
- Added in-game guide pages and Skill Proficiencies MIT-derived native skill/help information architecture.

## 0.5.0-alpha.1
- Activated Combat as the fourth live skill.
- Added kill-based Combat XP, smooth damage growth and hostile-only melee cleave at Lv.30/60/90.

## 0.4.0-alpha.1
- Expanded Mining to 9x9 and added connected ore-vein extraction 24/64/128.
- Adapted Veinminer++ MIT ore matching and bounded flood-fill patterns.

## 0.3.0-alpha.1
- Activated mature-only Harvesting and added the six-skill overview foundation.

## 0.2.0-alpha.1
- Added generic skill XP storage/sync/HUD and Woodcutting progression using Skill Proficiencies MIT patterns.

## 0.1.0-alpha.1
- Initial Mining progression prototype.
