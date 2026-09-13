# Fishing Game

## Identity

- Slug: fishing-game
- Mod ID: fishinggame
- Namespace: fishinggame
- Mod version: 0.1.0-alpha.8
- Minecraft: 26.2
- Java: 25
- Loader: Fabric
- Loader version: Fabric Loader >=0.19.3
- Fabric API: >=0.159.0+26.2
- Gradle: 9.5.1
- Build plugin: Fabric Loom 1.17.19
- Final JAR: build/libs/fishing-game-0.1.0-alpha.8.jar
- Existing-world compatibility: fishing profiles remain compatible; encounter-fish entities are transient and unsaved
- Required dependencies: Fabric API
- Optional external mods: Essential, connection/hosting convenience only
- Forbidden bundled dependencies: Essential
- Datagen task: none
- GameTest task: none
- Server smoke-test task: CI `./gradlew runServer` launch; pass only after dedicated server reaches ready state
- Client smoke-test task: none yet

## Game statement

Fishing Game is a standalone fishing progression game built on Minecraft, not a survival expansion.

Core loop:

> cast -> see the fish approach -> hook -> reel -> catch -> bag/collection -> sell -> improve rod -> reach better fishing locations -> catch rarer/larger fish

## Non-survival rule

The player is not expected to mine, craft, fight, manage hunger, or survive nights. Player damage is disabled, hunger/health are stabilized, Adventure mode is enforced, and survival HUD layers are removed. Minecraft is the runtime/world renderer for a dedicated fishing game.

## UI / HUD rule

Do not invent the visual language ad hoc. The UI system uses Kenney CC0 UI assets, with information architecture informed by proven fishing games such as Fish It / Fisch. External assets and licenses are tracked in `THIRD_PARTY_ASSETS.md`.

Presentation rules:
- show the real safe-tension band
- guidance reacts to current tension: reel in, release, or maintain rhythm
- successful catch result shows species, rarity, weight, length and value
- HUD stays compact enough that the fishing scene remains visually dominant

## World direction

Use dedicated fishing locations/maps rather than ordinary survival roaming. Prefer existing high-quality fishing maps or environment assets when redistribution/use terms are explicit. Unknown-license maps remain reference-only until permission is clear.

Location architecture:
- Cheongram Lakeside / freshwater — first authored dedicated location, generated in `fishinggame:lakeside`
- Gull Harbor / coast — planned
- Deepwater Channel / deep sea — planned

## Catch presentation direction

A hooked fish must be spatially visible, and visible motion should agree with actual mechanics. Alpha.5 established curved approach, species-size scaling, irregular pull bursts, bubble/splash feedback and convergence toward the hook. A pull burst also applies a small server-side tension impulse.

Alpha.7 replaced vanilla encounter proxies with dedicated transient Fishing Game fish and three reusable body families. Alpha.8 expands that visual system to five purposeful silhouettes — small, tall, fat, long and dedicated angler. Bluegill is routed to a taller/deeper-bodied shape and the deep-sea angler receives its own anglerfish geometry and licensed Sea Life texture rather than sharing the long-fish fallback.

The encounter entities exist only for the active fishing session, are not saved, have no AI and are discarded on cancel/catch/disconnect. This preserves the physical-fish feel without turning the lake into a persistent-entity simulation.

The adapted model geometry and directly reused compatible textures are tracked in `THIRD_PARTY_ASSETS.md`, and the full Sea Life MIT notice is bundled in the JAR.

## Fishing-hook ownership rule

Fishing Game uses the vanilla fishing hook only as the cast/line/bobber transport and visual anchor. Dedicated fishing locations must not also run vanilla's independent lure/nibble/bite/loot presentation on the same hook.

Alpha.6 suppresses vanilla `FishingHook#catchingFish` only for server-owned hooks inside `fishinggame:lakeside`. Cast physics, line rendering and water bobbing remain vanilla. Custom fish approach, bite timing, tension, catch result and economy remain server authoritative.

## User-test gate

Do not hand the user another JAR for a tiny technical check. A user-facing test build must have, at minimum:
- dedicated non-survival HUD
- catch bag UI
- persistent catches and coins
- selling
- meaningful rod progression
- visible/credible species presentation during the catch
- readable tension control with the safe band visible
- clear catch-result feedback
- no competing vanilla bite/loot presentation inside dedicated Fishing Game locations
- at least one dedicated fishing location with acceptable visual quality
- complete cast -> catch -> sell -> upgrade loop

Build success alone does not satisfy this gate. Alpha.8 improves species silhouette identity, but actual Minecraft client review of fish proportions/textures, lake composition and overall screen quality is still required before calling the build playtest-ready.
