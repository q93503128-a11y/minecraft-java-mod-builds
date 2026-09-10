# Fishing Game

## Identity

- Slug: fishing-game
- Mod ID: fishinggame
- Namespace: fishinggame
- Mod version: 0.1.0-alpha.6
- Minecraft: 26.2
- Java: 25
- Loader: Fabric
- Loader version: Fabric Loader >=0.19.3
- Fabric API: >=0.159.0+26.2
- Gradle: 9.5.1
- Build plugin: Fabric Loom 1.17.19
- Final JAR: build/libs/fishing-game-0.1.0-alpha.6.jar
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

Do not invent the visual language ad hoc. The UI system uses Kenney CC0 UI assets, with information architecture informed by proven fishing games such as Fish It / Fisch. External assets and their licenses are tracked in `THIRD_PARTY_ASSETS.md`.

Presentation rules:
- the tension mechanic must show its actual safe band instead of asking the player to infer an invisible threshold
- reeling guidance must react to current tension: reel in, release, or maintain rhythm
- a successful catch must get a compact result card with species, rarity, weight, length and value
- HUD feedback must stay compact enough to preserve the fishing scene as the main visual focus

## World direction

Use dedicated fishing locations/maps rather than ordinary survival roaming. Prefer existing high-quality fishing maps or environment assets when their redistribution/use terms are explicit. Unknown-license maps may be used only as references or local editing bases until permission is clear.

Location architecture:
- Cheongram Lakeside / freshwater — first authored dedicated location, generated in `fishinggame:lakeside`
- Gull Harbor / coast — planned
- Deepwater Channel / deep sea — planned

The first lakeside is authored inside the mod because no redistributable third-party fishing map with sufficiently clear rights was accepted for bundling. This does not relax the external-reference rule for later visual iteration.

## Catch presentation direction

A hooked fish must be spatially visible. Alpha.3 established visible fish proxies; alpha.5 replaced the obvious orbiting motion with an S-curve approach, species-size scaling, irregular pull bursts, bubble/splash feedback and fight motion that converges toward the hook as catch progress rises.

The current fish are still Minecraft-native visual proxies, not final per-species models. The movement direction is informed by the MIT Simple Fishing Overhaul concept of moving real fish toward the hook, but the presentation code is project-authored rather than copied from that mod.

Visible motion and actual mechanics should agree: a pull burst also applies a small server-side tension impulse, so the fish visibly surging away is not cosmetic-only feedback.

## Fishing-hook ownership rule

Fishing Game uses the vanilla fishing hook only as the cast/line/bobber transport and visual anchor. The dedicated fishing locations must not also run vanilla's independent lure/nibble/bite/loot presentation on the same hook.

Alpha.6 suppresses vanilla `FishingHook#catchingFish` only for server-owned hooks inside `fishinggame:lakeside`. Cast physics, line rendering and water bobbing remain vanilla. Custom fish approach, bite timing, tension, catch result and economy remain server authoritative in Fishing Game.

This suppression is deliberately scoped to dedicated Fishing Game locations instead of globally changing vanilla fishing in unrelated dimensions. Each future dedicated location must be added to the managed-location rule when it is implemented.

## User-test gate

Do not hand the user another JAR for a tiny technical check. A user-facing test build must have, at minimum:
- dedicated non-survival HUD
- catch bag UI
- persistent catches and coins
- selling
- meaningful rod progression
- visible/credible fish presentation during the catch
- readable tension control with the safe band visible
- clear catch-result feedback
- no competing vanilla bite/loot presentation inside dedicated Fishing Game locations
- at least one dedicated fishing location with acceptable visual quality
- complete cast -> catch -> sell -> upgrade loop

Build success alone does not satisfy this gate. Alpha.6 closes the double-fishing-system conflict, but actual Minecraft visual review of the lake, fish motion and screen composition is still required before calling the build playtest-ready.
