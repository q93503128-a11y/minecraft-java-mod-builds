# Fishing Game

## Identity

- Slug: fishing-game
- Mod ID: fishinggame
- Namespace: fishinggame
- Mod version: 0.1.0-alpha.9
- Minecraft: 26.2
- Java: 25
- Loader: Fabric
- Loader version: Fabric Loader >=0.19.3
- Fabric API: >=0.159.0+26.2
- Gradle: 9.5.1
- Build plugin: Fabric Loom 1.17.19
- Final JAR: build/libs/fishing-game-0.1.0-alpha.9.jar
- Required dependencies: Fabric API
- Optional external mods: Essential, connection/hosting convenience only
- Forbidden bundled dependencies: Essential
- Datagen task: none
- GameTest task: none
- Server smoke-test task: CI `./gradlew runServer` launch; pass only after dedicated server reaches ready state
- Client smoke-test task: none yet

## Game statement

Fishing Game is a standalone fishing progression game built on Minecraft, not a survival expansion.

> cast -> see the fish approach -> hook -> reel -> catch -> bag/collection -> sell -> improve rod -> unlock a new fishing location -> catch rarer/larger fish

## Non-survival rule

The player is not expected to mine, craft, fight, manage hunger, or survive nights. Player damage is disabled, hunger/health are stabilized, Adventure mode is enforced, and survival HUD layers are removed.

## Location progression

Alpha.9 turns the existing fish catalog into an actual three-location progression instead of leaving coast/deep-sea species unreachable.

- `청람 호수` / `fishinggame:lakeside`: available from the start; freshwater catalog.
- `갈매기 항구` / `fishinggame:coast`: requires rod tier 1 (`호수 전문가`); coast catalog.
- `심해 수로` / `fishinggame:deep_sea`: requires rod tier 2 (`블루워터`); deep-sea catalog.

Travel is requested from the client but unlocked/validated by the server. Active fishing blocks travel. The player opens the Kenney-based travel screen with `M`; the screen shows current location, fish count and required rod rather than adding another currency or arbitrary stage key.

All three current locations are dedicated fishing spaces rather than ordinary survival terrain. Cheongram Lakeside remains the existing authored lake. Alpha.9 adds a project-authored harbor with stone quay, three piers, shelter and lighthouse, plus an offshore deep-sea platform with fishing arms, observation structure and lights. Third-party map candidates remain reference-only until redistribution rights are explicit.

## UI / HUD rule

Do not invent the visual language ad hoc. HUD, bag and travel screens reuse the Kenney CC0 UI language already bundled with the project. External assets and licenses are tracked in `THIRD_PARTY_ASSETS.md`.

## Catch presentation direction

Fishing encounters use five transient silhouettes — small, tall, fat, long and angler — with species-specific textures. They are session-only entities: unsaved, no AI, no loot, cleaned up on the fishing lifecycle. Visible approach and burst motion must agree with server-side bite/tension behavior.

## Fishing-hook ownership rule

Vanilla fishing hook is only cast/line/bobber transport and visual anchor in all dedicated Fishing Game dimensions. Vanilla's independent lure/nibble/bite cycle is suppressed there; Fishing Game owns species, bite timing, reel fight, catch result and economy.

## User-test gate

Do not hand the user a JAR for a tiny technical check. A user-facing test build must have:
- dedicated non-survival HUD
- catch bag, persistence, coins and selling
- meaningful rod progression
- visible species presentation and readable reel control
- clear catch result
- no competing vanilla bite/loot presentation
- multiple dedicated fishing locations connected to progression
- complete cast -> catch -> sell -> upgrade -> travel loop
- acceptable actual Minecraft screen quality

Build success alone is not the final gate. Alpha.9 completes the planned three-location progression structure, but the authored harbor/deep-sea environments and the overall HUD/map composition still require actual graphical client review before PLAYTESTED is claimed.
