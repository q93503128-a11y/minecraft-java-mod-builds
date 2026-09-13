# Changelog

## 0.1.0-alpha.9

- Added two playable dedicated fishing locations so coast/deep-sea catalog content is no longer unreachable.
- Added `갈매기 항구` with a stone quay, three piers, shelter, lighthouse, lamps and dock props.
- Added `심해 수로` with an offshore platform, long fishing arms, observation structure and sea-lantern lighting.
- Added rod-tier location progression: Lakeside tier 0, Coast tier 1, Deep Sea tier 2.
- Added server-authoritative travel requests; invalid/locked destinations and travel while actively fishing are rejected server-side.
- Added Kenney-based `M` travel screen showing current location, species count and rod requirement.
- Extended vanilla bite-cycle suppression to every dedicated Fishing Game dimension.
- Added location progression tests and CI inspection for both new dimension resources and travel classes.
- Kept all current maps project-authored; community map candidates remain reference-only until redistribution rights are explicit.

## 0.1.0-alpha.8

- Expanded encounter-fish presentation from three silhouettes to five: small, tall, fat, long and dedicated angler.
- Bluegill now uses the taller/deeper-bodied silhouette instead of sharing the generic small fish body.
- Deep-sea angler now uses an adapted Sea Life `AnglerfishModel` silhouette instead of the long-fish fallback.
- Replaced the project-authored angler placeholder texture with Sea Life's MIT-licensed `anglerfish.png` and recorded the direct binary reuse.
- Added entity types, model layers and client render registration for the new silhouettes while keeping encounter fish transient, unsaved and server-session-owned.
- Expanded `FishVisualFamilyTest` so key species cannot silently regress to the wrong family.

## 0.1.0-alpha.7

- Replaced vanilla cod/salmon/tropical-fish encounter proxies with dedicated Fishing Game encounter-fish entities.
- Added three reusable encounter silhouettes — small, fat and long — adapted from Sea Life MIT geometry.
- Added synchronized species ids and species-specific textures.
- Preserved species-size scaling, curved approach, fight bursts, tension coupling and catch VFX.

## 0.1.0-alpha.6

- Suppressed vanilla `FishingHook#catchingFish` inside the dedicated lakeside while preserving cast physics, line rendering and bobbing.
- Preserved server authority for species, bite timing, tension, catch result, bag and economy.

## 0.1.0-alpha.5

- Added curved fish approach, species-size scaling, irregular pull bursts and stronger bite/catch feedback.
- Pull bursts now affect the real server-side tension value.

## 0.1.0-alpha.4

- Made the reel fight readable with the actual safe-tension band and live guidance.
- Added compact catch-result feedback.

## 0.1.0-alpha.3

- Added dedicated `fishinggame:lakeside` and physical fish presentation.

## 0.1.0-alpha.2

- Converted the project into a standalone fishing game.
- Added persistent catch bag, coins, selling, rod progression and Kenney-based fishing UI.
