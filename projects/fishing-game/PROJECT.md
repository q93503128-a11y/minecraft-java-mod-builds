# Fishing Game

## Identity

- Slug: fishing-game
- Mod ID: fishinggame
- Namespace: fishinggame
- Mod version: 0.1.0-alpha.3
- Minecraft: 26.2
- Java: 25
- Loader: Fabric
- Loader version: Fabric Loader >=0.19.3
- Fabric API: >=0.159.0+26.2
- Gradle: 9.5.1
- Build plugin: Fabric Loom 1.17.19
- Final JAR: build/libs/fishing-game-0.1.0-alpha.3.jar
- Existing-world compatibility: fishing profiles remain compatible; the dedicated fishing dimension is added without replacing vanilla dimensions
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

Do not invent the visual language ad hoc. The first UI system uses Kenney CC0 UI assets, with information architecture informed by proven fishing games such as Fish It / Fisch. External assets and their licenses are tracked in `THIRD_PARTY_ASSETS.md`.

## World direction

Use dedicated fishing locations/maps rather than ordinary survival roaming. Prefer existing high-quality fishing maps or environment assets when their redistribution/use terms are explicit. Unknown-license maps may be used only as references or local editing bases until permission is clear.

Location architecture:
- Cheongram Lakeside / freshwater — first authored dedicated location, generated in `fishinggame:lakeside`
- Gull Harbor / coast — planned
- Deepwater Channel / deep sea — planned

The first lakeside is authored inside the mod because no redistributable third-party fishing map with sufficiently clear rights was accepted for bundling. This does not relax the external-reference rule for later visual iteration.

## Catch presentation direction

A hooked fish must be spatially visible. Alpha.3 uses a temporary pool of real vanilla fish entities as server-controlled visual proxies that approach the bobber and fight around it. These are not the final species models; later asset/model work should replace them without returning to invisible numeric-only catches.

## User-test gate

Do not hand the user another JAR for a tiny technical check. A user-facing test build must have, at minimum:
- dedicated non-survival HUD
- catch bag UI
- persistent catches and coins
- selling
- meaningful rod progression
- visible/credible fish presentation during the catch
- at least one dedicated fishing location with acceptable visual quality
- complete cast -> catch -> sell -> upgrade loop

Build success alone does not satisfy this gate. The first dedicated location and visible fish proxy move alpha.3 close to the gate, but actual Minecraft visual review is still required before calling it ready for the user's playtest.
