# Changelog

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
