# Fishing Game

## Identity

- Slug: fishing-game
- Mod ID: fishinggame
- Namespace: fishinggame
- Mod version: 0.1.0-alpha.10
- Minecraft: 26.2
- Java: 25
- Loader: Fabric
- Loader version: Fabric Loader >=0.19.3
- Fabric API: >=0.159.0+26.2
- Gradle: 9.5.1
- Build plugin: Fabric Loom 1.17.19
- Final JAR: build/libs/fishing-game-0.1.0-alpha.10.jar
- Required dependencies: Fabric API
- Optional external mods: Essential, connection/hosting convenience only
- Forbidden bundled dependencies: Essential
- Datagen task: none
- GameTest task: none
- Server smoke-test task: CI `./gradlew runServer` launch; pass only after dedicated server reaches ready state
- Client smoke-test task: none yet

## Game statement

Fishing Game is a standalone fishing progression game built on Minecraft, not a survival expansion.

> cast -> see the fish approach -> hook -> reel -> catch -> bag/collection -> sell -> improve rod -> unlock a new fishing location -> catch rarer/larger fish -> complete records

## Non-survival rule

The player is not expected to mine, craft, fight, manage hunger, or survive nights. Player damage is disabled, hunger/health are stabilized, Adventure mode is enforced, and survival HUD layers are removed.

## Location progression

The fish catalog is connected to a three-location progression rather than leaving coast/deep-sea species unreachable.

- `청람 호수` / `fishinggame:lakeside`: available from the start; freshwater catalog.
- `갈매기 항구` / `fishinggame:coast`: requires rod tier 1 (`호수 전문가`); coast catalog.
- `심해 수로` / `fishinggame:deep_sea`: requires rod tier 2 (`블루워터`); deep-sea catalog.

Travel is requested from the client but unlocked/validated by the server. Active fishing blocks travel. The player opens the Kenney-based travel screen with `M`; the screen shows current location, fish count and required rod rather than adding another currency or arbitrary stage key.

All three current locations are dedicated fishing spaces rather than ordinary survival terrain. Cheongram Lakeside remains the authored lake. Gull Harbor has a stone quay, three piers, shelter and lighthouse; Deepwater Channel is an offshore fishing platform with fishing arms, observation structure and lights. Their clocks are independent so each location can keep its own visual time. Third-party map candidates remain reference-only until redistribution rights are explicit.

## Collection / bestiary rule

Alpha.10 separates the temporary catch bag from permanent collection records.

- `B`: catch bag, selling and rod progression.
- `J`: fish collection / bestiary.
- `M`: fishing-location travel.
- Selling clears only the catch bag. Species records survive and remain server-authoritative persistent player data.
- Each discovered species records total catch count, personal-best weight and personal-best length.
- Undiscovered species hide their name in the collection screen.
- The collection screen shows per-location discovery progress as well as total discovery progress.
- Existing saves without a `records` field remain loadable. Fish still present in an old catch bag seed their initial records on migration; catches sold before this record system existed cannot be reconstructed.

The bestiary exists to make rare-fish hunting, replaying old locations and personal-record chasing part of the core loop without adding another currency or maintenance chore.

## UI / HUD rule

Do not invent the visual language ad hoc. HUD, bag, bestiary and travel screens reuse the Kenney CC0 UI language already bundled with the project. External assets and licenses are tracked in `THIRD_PARTY_ASSETS.md`.

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
- permanent collection/personal-best records that survive selling
- complete cast -> catch -> sell -> upgrade -> travel -> collect loop
- acceptable actual Minecraft screen quality

Build success alone is not the final gate. Alpha.10 completes the first permanent collection loop, but the authored environments and HUD/menu composition still require actual graphical client review before PLAYTESTED is claimed.
