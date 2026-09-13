# Fishing Game

## Identity

- Slug: fishing-game
- Mod ID: fishinggame
- Namespace: fishinggame
- Mod version: 0.1.0-alpha.14
- Minecraft: 26.2
- Java: 25
- Loader: Fabric
- Loader version: Fabric Loader >=0.19.3
- Fabric API: >=0.159.0+26.2
- Gradle: 9.5.1
- Build plugin: Fabric Loom 1.17.19
- Final JAR: build/libs/fishing-game-0.1.0-alpha.14.jar
- Required dependencies: Fabric API
- Optional external mods: Essential, connection/hosting convenience only
- Forbidden bundled dependencies: Essential
- Datagen task: none
- GameTest task: none
- Server smoke-test task: CI `./gradlew runServer` launch; pass only after dedicated server reaches ready state
- Client smoke-test task: none yet

## Game statement

Fishing Game is a standalone fishing progression game built on Minecraft, not a survival expansion.

> charge cast -> see the fish approach -> hook -> reel -> catch -> bag/collection -> sell -> improve rod -> unlock a new fishing location -> catch rarer/larger fish -> complete records

## Non-survival rule

The player is not expected to mine, craft, fight, manage hunger, or survive nights. Player damage is disabled, hunger/health are stabilized, Adventure mode is enforced, and survival HUD layers are removed.

## Casting rule

Casting is part of the fishing interaction, not an instant vanilla transport step.

- Press/hold right click to build cast charge; release to throw.
- Full charge is reached after 18 server ticks. Holding longer gives no additional distance.
- Charge affects hook launch speed/distance only. It never increases species rarity, fish size, sale value or bite luck.
- The client shows a compact charge meter, but the authoritative charge duration is measured from server game time between accepted press and release.
- Release/cancel requests are transition packets rather than per-frame charge spam.
- Once a fish is hooked, the same right-click hold/release input returns to line-tension control.

## Location progression

- `청람 호수` / `fishinggame:lakeside`: available from the start; freshwater catalog.
- `갈매기 항구` / `fishinggame:coast`: requires rod tier 1 (`호수 전문가`); coast catalog.
- `심해 수로` / `fishinggame:deep_sea`: requires rod tier 2 (`블루워터`); deep-sea catalog.

Travel is requested from the client but unlocked/validated by the server. Active fishing blocks travel. `M` opens the Kenney-based travel screen.

### Environment quality contract

Alpha.12 gives the two progression locations readable authored silhouettes instead of leaving them as flat functional platforms.

- Gull Harbor: arrival promenade, layered shoreline rockwork, two breakwater arms with entrance beacons, three expanded fishing stations and a stronger lighthouse balcony/light silhouette.
- Deepwater Channel: three dedicated outward-facing fishing pods, hazard-guide stripes at each approach, rail-protected circulation space, submerged guide lights and a tall signal mast.
- Fishing edges remain open toward water so scenery does not fight the core interaction.
- New environment work is deterministic and server-authored; it runs only once per dimension revision rather than rebuilding every tick.
- Existing alpha.9+ worlds are upgraded through a separate alpha.12 quality marker after the original base-build marker is detected. New worlds receive the same pass after their base structures are authored.
- Future environment revisions must use a new revision marker rather than silently relying on the original build marker.
- Third-party map candidates remain reference-only until redistribution rights are explicit; do not bundle unknown-license maps.

## Collection / bestiary rule

The temporary catch bag and permanent collection records are separate systems.

- `B`: catch bag, selling and rod progression.
- `J`: fish collection / bestiary.
- `M`: fishing-location travel.
- Selling clears only the catch bag. Species records survive and remain server-authoritative persistent player data.
- Each discovered species records total catch count, personal-best weight and personal-best length.
- Undiscovered species hide their name in the collection screen.
- The collection screen shows per-location discovery progress as well as total discovery progress.
- Existing saves without a `records` field remain loadable. Fish still present in an old catch bag seed their initial records on migration; catches sold before this record system existed cannot be reconstructed.

## Catch quality / record feedback

Every catch is graded from its configured species weight/length ranges: `일반`, `대형`, `트로피`, `괴물급`. The catch-result card shows rarity, size grade, weight, length and sale value, and calls out first discoveries or new personal records. Presentation is derived from server-authoritative profile snapshots.

## UI / HUD rule

Do not invent the visual language ad hoc. HUD, cast meter, bag, bestiary and travel screens reuse the Kenney CC0 UI language already bundled with the project. External assets and licenses are tracked in `THIRD_PARTY_ASSETS.md`.

Alpha.14 centralizes that visual language in one Fishing Game theme instead of duplicating panel/color rules per screen.

- The always-on HUD uses a 200px-wide two-tile Kenney panel so coin, bag, rod, location and B/J/M navigation remain readable instead of overflowing a 100px card.
- Bag, bestiary and travel screens share title/subtitle hierarchy, separators, section labels, text colors and disabled-state treatment.
- Unaffordable rod upgrades are visibly disabled client-side while the server remains the authority for the actual purchase.
- Catch bag rows expose rarity, size grade, weight, length and value without creating more menus.
- UI changes must still be judged in a real Minecraft client at supported GUI scales; code/build success does not certify screen composition.

## Audio feedback rule

Alpha.14 separates ordinary fishing audio from reward/interaction cues.

- Existing bobber splash remains the bite cue; ordinary catches retain the existing catch sound.
- Full cast charge gets one light confirmation cue rather than repeated charging noise.
- Large tension jumps during the hooked fight can trigger a restrained fish-pull cue with a client cooldown.
- Trophy/rare catches receive an extra reward layer; first discoveries, personal records, monster-size catches and legendary fish receive a stronger confirmation layer.
- Successful selling and rod upgrades have distinct reward cues.
- These added cues are cosmetic client feedback only. They do not own catch state, records, currency, progression or purchase success.

## Catch presentation direction

Fishing encounters use five transient silhouettes — small, tall, fat, long and angler — with species-specific textures. They are session-only entities: unsaved, no AI, no loot, cleaned up on the fishing lifecycle. Visible approach and burst motion must agree with server-side bite/tension behavior.

## Fishing-hook ownership rule

Vanilla fishing hook is only line/bobber transport and visual anchor in all dedicated Fishing Game dimensions. Fishing Game owns charge/release timing, species, bite timing, reel fight, catch result and economy. Vanilla's independent lure/nibble/bite cycle is suppressed there.

## User-test gate

Do not hand the user a JAR for a tiny technical check. A user-facing test build must have:
- dedicated non-survival HUD
- readable charge casting with meaningful distance response
- catch bag, persistence, coins and selling
- meaningful rod progression
- visible species presentation and readable reel control
- clear catch result with rarity, size grade and record feedback
- no competing vanilla bite/loot presentation
- multiple dedicated fishing locations connected to progression
- permanent collection/personal-best records that survive selling
- complete cast -> catch -> sell -> upgrade -> travel -> collect loop
- acceptable actual Minecraft screen quality

Build success alone is not the final gate. Alpha.14 closes the first shared UI/audio feedback pass, but the actual Minecraft client still needs graphical/play review before PLAYTESTED or GRAPHICAL CLIENT REVIEWED is claimed.
