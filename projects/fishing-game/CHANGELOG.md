# Changelog

## 0.1.0-alpha.8

- Expanded encounter-fish presentation from three silhouettes to five: small, tall, fat, long and dedicated angler.
- Bluegill now uses the taller/deeper-bodied silhouette instead of sharing the generic small fish body.
- Deep-sea angler now uses an adapted Sea Life `AnglerfishModel` silhouette instead of the long-fish fallback.
- Replaced the project-authored angler placeholder texture with Sea Life's MIT-licensed `anglerfish.png` and recorded the direct binary reuse.
- Added entity types, model layers and client render registration for the new silhouettes while keeping encounter fish transient, unsaved and server-session-owned.
- Expanded `FishVisualFamilyTest` so bluegill, mackerel, angler, catfish, oarfish and ancient sturgeon cannot silently regress to the wrong family.
- Kept the user-test gate: this visual refactor still requires actual Minecraft client review before a JAR is called playtest-ready.

## 0.1.0-alpha.7

- Replaced vanilla cod/salmon/tropical-fish encounter proxies with dedicated Fishing Game encounter-fish entities.
- Added three reusable encounter silhouettes — small, fat and long — adapted from Sea Life's MIT-licensed fish model geometry with bundled attribution/license.
- Added synchronized species ids so the client renders the selected catch with a species-specific texture instead of a generic vanilla fish appearance.
- Added species textures for the current catalog; UV-compatible largemouth/carp/catfish/perch/tuna textures reuse Sea Life MIT assets, while the remaining alpha.7 textures are project-authored.
- Kept encounter fish transient: no AI, no save serialization, no loot, and cleanup on the existing fishing-session lifecycle.
- Preserved species-size scaling, curved approach, fight bursts, tension coupling and catch VFX from the existing fishing presentation.
- Added `FishVisualFamilyTest` to lock purposeful body-family routing for key species.
- Kept the playtest gate: build success does not replace actual Minecraft client review of fish proportions/textures and the dedicated lake.

## 0.1.0-alpha.6

- Removed the hidden second fishing system from the dedicated lakeside: vanilla `FishingHook#catchingFish` is suppressed there while Fishing Game owns bite timing and catch flow.
- Kept vanilla cast physics, line rendering and water bobbing instead of replacing the entire fishing hook.
- Scoped the mixin to server-owned hooks in `fishinggame:lakeside` so unrelated vanilla dimensions are not globally altered.
- Added required mixin configuration to the Fabric metadata and made CI inspect both the mixin config and mixin class in the playable JAR.
- Preserved server authority for species selection, bite timing, tension, catch result, bag and economy.

## 0.1.0-alpha.5

- Replaced the obvious circular hooked-fish motion with a curved final approach and irregular fight motion.
- Added species-size scaling for temporary fish proxies so small freshwater fish and large catches no longer read at the same physical size.
- Added short fish pull bursts during reeling; bursts visibly extend the fish and apply a server-authoritative tension impulse.
- Added bubble trails on final approach plus stronger bite and successful-catch splash/audio feedback.
- Added deterministic presentation math tests.

## 0.1.0-alpha.4

- Made the reel fight readable by showing the actual safe-tension band.
- Added live reel/release/maintain guidance.
- Added a compact catch-result card with species, rarity, weight, length and value.

## 0.1.0-alpha.3

- Added dedicated `fishinggame:lakeside` and its authored fishing environment.
- Added physical fish presentation before and during the bite.
- Added cleanup for temporary hooked-fish visuals.

## 0.1.0-alpha.2

- Converted the project from survival-adjacent mod to standalone fishing game.
- Added persistent catch bag, coins, selling and three-tier rod progression.
- Added dedicated fishing HUD/catch-bag screen using Kenney CC0 UI assets.
- Kept Essential optional and external to game-state authority.
