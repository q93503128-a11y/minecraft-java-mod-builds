# Survival Ascension — Field Boss Integration

## Goal

Endgame field encounters should feel like real bosses, not vanilla zombies or skeletons with inflated health and damage.

Core loop:

exploration -> discover a rare field boss -> read and defeat its real attack kit -> earn native boss loot + Survival Ascension progression -> improve equipment/settlement crafting -> hunt harder content

## Active source: The Birth of Steve

The current locked 26.2 NeoForge content pack already contains The Birth of Steve. Its audited major-target tag contains the original external entities:

- `tbos:hour_cantor`
- `tbos:phoenix_guardian` (The Last Curator)

Survival Ascension does not copy their model, texture, animation, sound or Java implementation. The original TBOS JAR owns all presentation and native combat. Survival only attaches tracking, multiplayer contribution credit, population caps and endgame progression through registry/tag contracts.

Natural vanilla-hostile Mythic III promotion is retired. A would-be Mythic III roll now attempts to spawn one of the loaded tagged field bosses. If optional content is absent or admission fails, the ordinary hostile falls back to Ascended II instead of becoming a stat-inflated Mythic III body.

External field bosses keep their native boss bar, animation, AI and attack patterns. Survival's generic Mythic phase buffs, trait reactions, glow and generic ring/marked attacks are not layered on top of them.

## Field-boss strength doctrine

A true field boss must be clearly harder than the old Survival-owned Mythic III, but not by setting absurd attack damage or multiplying health until the fight becomes tedious.

Current cross-system reinforcement:

- Native external attacks, animation and AI remain authoritative.
- Incoming damage receives a bounded 18% Survival-side mitigation layer.
- One hit cannot remove more than roughly 14% of maximum health before the external boss's own downstream defenses. This is an anti-burst guard, not permanent invulnerability.
- At roughly 66% and 33% health the boss enters a ward phase and summons audited content-pack escorts.
- Ward phase 1 attempts two escorts; ward phase 2 attempts three. Three or more nearby players add at most one extra escort, with a hard cap of four.
- While at least one ward escort remains, Survival-side incoming damage is reduced again to 42% of the already bounded value.
- Killing every ward escort immediately collapses the ward. Players are rewarded for switching targets instead of waiting through a fixed immunity timer.
- A ward has a 15-second upper limit so failed pathfinding or an unreachable escort cannot soft-lock the boss.
- Phase escorts come only from the already audited `expedition_reinforcements_tier_*` pools. The current high-stage pool includes TBOS enemies such as Blank Chronist, Gnomon Knight, Parallax Wraith, Meridian Sentinel and Hour Hand Wraith.
- Old phase escorts are removed before the next ward cohort is created, and surviving field-boss escorts are removed when the boss encounter ends or the server stops. This bounds accumulation.
- No world-wide mob scan or chunk force-load is used. Runtime work is limited to currently adopted field bosses and their small tracked escort sets.

Design intent: the boss is durable because the player must survive its native kit, control adds and create damage windows. It is not durable because its attack damage is set to an arbitrary triple-digit value or because it has an enormous passive health sponge.

## Reward doctrine

The most valuable repeatable rewards should come from choices and progression, not piles of diamonds/emeralds.

Current field-boss reward stack:

- native external-mod boss loot
- Survival major-target combat/expedition credit
- guaranteed rank-III Survival equipment roll for the killer
- killer-only 90 vanilla XP from the shared elite reward layer
- meaningful-contribution Mythic hunt progress for every qualified contributor
- deterministic Netherite Scrap progress every 3 ordinary hunt credits
- deterministic Tempering Seal progress every 4 ordinary hunt credits
- a qualified true field-boss kill counts as two hunt credits because the encounter is intentionally above the old Mythic III combat tier

External field bosses deliberately skip the legacy Mythic raw-material world drop and the old proximity reward bundle. They do not grant extra Survival diamonds, emeralds or echo shards merely for standing within the old reward radius. Premium progression is awarded only through the actual damage-contribution authority, with the killer included, so multiplayer participation is useful without turning the encounter into a proximity loot printer.

## Endgame enchantment-completion role

Field bosses and the Fractured Archive deliberately do not collapse into the same reward table.

- Field bosses remain the main repeatable source of weighted Mythic hunt progress, Netherite Scrap progress and Tempering Seal progress.
- Late Fractured Archive major targets supply the physical `Enchantment Stone` completion resource. The Survival layer does not rewrite TBOS chests or remove native dungeon loot.
- A qualifying Archive major-target clear grants one Enchantment Stone and has a 35% chance for one Netherite Scrap.
- The tagged final Archive target (`tbos:phoenix_guardian`) grants two Enchantment Stones, one guaranteed Netherite Scrap and a 25% chance for a second scrap.
- Rewards use actual damage contribution with the killer included; already-earned rewards are persisted if a qualified contributor disconnects before payout.
- Enchantment Stones act directly on equipment held in the opposite hand. General Protection replaces Fire/Blast/Projectile Protection, and Sharpness replaces Smite/Bane of Arthropods when that item supports the canonical enchantment.
- After canonicalization, the stone adds one missing compatible curated enchantment at a useful mid level. Once no compatible curated enchantment is missing, it upgrades one existing curated enchantment by one level.
- Vanilla maximum levels and compatibility remain authoritative. The stone does not create over-level enchantments, curses or illegal incompatible combinations.
- The completion pool covers armor, melee/ranged weapons, mace/trident/spear-specific progression, tools and fishing rods so the system does not solve only one equipment family.
- If no legal improvement exists, the stone is not consumed.

Design intent: late progression should reduce enchanting-table/anvil lottery friction and make a true finished set realistically attainable, while still requiring repeated high-end content rather than granting a complete loadout in one reward.

## Candidate expansion

### World Bosses — first candidate

Current public release research (2026-09-14):

- Minecraft 26.2 supported
- NeoForge release available (`worldbosses-1.4.0.jar`, CurseForge file 8737240)
- custom models, animations, phases, skills, boss bars, atmosphere and multiplayer raid scaling
- six themed world bosses plus a final primordial boss
- boss shards, trophies, raid caches and an endgame equipment path
- All Rights Reserved

Policy: use the original JAR and original entities only. Do not copy or edit its assets into Survival Ascension.

Current blocker: `tools/build_mrpack.py` only accepts original files served by the approved Modrinth CDN. World Bosses is currently sourced through CurseForge, so adding it requires an explicit pack-source extension with pinned project/file identity, hashes, approved host and dependency verification. Do not bypass the existing source lock.

### Design references only for now

- Mowzie's Mobs — strong creature silhouettes, animation and readable boss attacks; current researched release target does not match our 26.2 pack and its custom license makes asset copying inappropriate.
- L_Ender's Cataclysm — strong raid-scale boss presentation and arena attacks; current researched releases do not match our 26.2 pack and it uses a custom license.
- Threateningly Mobs Continued — 26.2 NeoForge candidate with distinctive hostile designs; requires binary/entity audit and role-overlap check before pack admission.
- Alex's Mobs NeoForge port — 26.2 candidate with broad creature variety, but adding a large ecology mod only for a few bosses risks pack bloat; evaluate only if several creatures earn clear gameplay roles.

## Admission rules for future field-boss mods

A new content mod is added only when it provides a clear role that current bosses do not cover. Verify:

1. actual Minecraft 26.2 + NeoForge compatibility
2. client/server stability with the current locked pack
3. license and redistribution/source-link constraints
4. dependencies and update risk
5. concrete boss/entity registry IDs and native boss behavior
6. spawn/despawn safety and persistence behavior
7. whether direct spawning outside its original arena breaks scripts
8. native loot overlap with Survival rewards
9. multiplayer contribution, death, disconnect and duplicate-claim behavior
10. visual/attack identity distinct enough to justify pack weight

Do not turn every external boss into the same Survival stat template. Preserve the source boss's presentation and attack identity; Survival owns only cross-system progression, reward authority and bounded world admission.
