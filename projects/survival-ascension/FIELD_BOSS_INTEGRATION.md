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

## Reward doctrine

The most valuable repeatable rewards should come from choices and progression, not piles of diamonds/emeralds.

Existing field-boss hooks currently include:

- native external-mod boss loot
- Survival major-target combat/expedition credit
- guaranteed rank-III Survival equipment roll for the killer
- meaningful-contribution Mythic hunt progress for every qualified contributor
- deterministic Netherite Scrap progress every 3 qualified Mythic kills
- deterministic Tempering Seal progress every 4 qualified Mythic kills

Before final playtest, audit legacy raw diamond/emerald/XP rewards together with native boss loot so the encounter does not become a loot printer. Premium progression must remain per-qualified-player to reduce loot-stealing problems.

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
