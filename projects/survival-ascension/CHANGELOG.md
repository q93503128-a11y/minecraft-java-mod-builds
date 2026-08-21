# Changelog

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
