# Changelog

## 0.4.0-alpha.1
- Expanded Mining terrain excavation to 9x9 at level 90.
- Added level-gated connected ore-vein extraction: 24 blocks at Lv.30, 64 at Lv.60, and 128 at Lv.90.
- Valuable ores now prefer vein extraction over flat area excavation so high-level mining changes behavior instead of only increasing radius.
- Added ore-family equivalence so touching stone/deepslate variants of major ores can be treated as the same vein.
- Kept precision sneak mode as an explicit 1x1 override for both area and vein mining.
- Kept all secondary ore breaks on `ServerPlayerGameMode.destroyBlock` for normal drops, durability and event handling.
- Adapted MIT-licensed Veinminer++ ore matching and bounded flood-fill patterns with packaged attribution.
- Updated the K skill screen and `/ascension stats` to show current vein capacity.

## 0.3.0-alpha.1
- Activated Harvesting progression with mature-crop-only XP to prevent immature crop XP farming.
- Added hoe-scoped harvest speed scaling and 3x3/5x5/7x7/9x9 area harvesting at levels 10/30/60/90.
- Kept every secondary harvested block on the normal `ServerPlayerGameMode.destroyBlock` pipeline.
- Added the K-key six-skill overview screen with level, XP progress, current action scale and speed.
- Added shared mastery tiers I-V as the foundation for later tool-tier, enchantment and content unlocks.
- Added `/ascension harvesting setlevel` and Harvesting to `/ascension stats`.
- Extended the retained Skill Proficiencies MIT attribution to the mature-crop classifier and multi-skill screen architecture.

## 0.2.0-alpha.1
- Replaced the mining-only save model with a generic per-skill XP map while preserving alpha.1 mining save migration.
- Adapted MIT-licensed Skill Proficiencies architecture for skill storage, clientbound payloads, and recent-skill XP HUD behavior.
- Added a synchronized skill XP HUD and level-up feedback.
- Fixed mining speed so the bonus only applies to pickaxe mining instead of globally modifying block-break speed.
- Added Woodcutting progression with tool-specific speed scaling and connected-log limits of 16/48/128/256 at levels 10/30/60/90.
- Preserved normal `ServerPlayerGameMode.destroyBlock` handling for all secondary mining/log breaks.
- Added third-party notices and explicit ARR reference-only policy.

## 0.1.0-alpha.1
- Initial mining progression prototype.
- Added levels 0-100, mining speed scaling, 3x3/5x5/7x7 area mining, precision sneak mode, commands and canonical verification.
