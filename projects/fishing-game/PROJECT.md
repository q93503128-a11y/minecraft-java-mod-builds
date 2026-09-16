# Fishing Game

## Identity

- Slug: fishing-game
- Mod ID: fishinggame
- Namespace: fishinggame
- Mod version: 0.1.0-alpha.21
- Minecraft: 26.2
- Java: 25
- Loader: Fabric
- Loader version: Fabric Loader >=0.19.3
- Fabric API: >=0.159.0+26.2
- Gradle: 9.5.1
- Build plugin: Fabric Loom 1.17.19
- Final JAR: build/libs/fishing-game-0.1.0-alpha.21.jar
- Required dependencies: Fabric API
- Optional external mods: Essential, connection/hosting convenience only
- Forbidden bundled dependencies: Essential
- Datagen task: none
- GameTest task: none
- Server smoke-test task: CI `./gradlew runServer` launch; pass only after dedicated server reaches ready state
- Client smoke-test task: none yet

## Game statement

Fishing Game is a standalone fishing progression game built on Minecraft, not a survival expansion.

> charge cast -> choose water -> see the fish approach -> hook -> reel -> catch -> bag/collection -> sell -> improve rod -> unlock a new fishing location -> hunt specific rarer/larger fish -> complete records -> rebirth -> accelerate the next cycle

## Non-survival rule

The player is not expected to mine, craft, fight, manage hunger, or survive nights. Player damage is disabled, hunger/health are stabilized, Adventure mode is enforced, and survival status HUD layers are removed. The vanilla hotbar and held-item tooltip remain visible while Fishing Game still uses real Minecraft inventory slots; do not hide half of the inventory affordance unless a complete replacement inventory interaction has been designed and implemented.

## Casting rule

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

- Cheongram Lakeside: the playable lake must read as an inland authored lake rather than a thin island ring in an infinite flat ocean. Alpha.20 adds a shaped lake bed, continuous exterior terrain, a raised scenic ridge, grounded conifer belt and rockwork around the existing fishing structures.
- Cheongram Lakeside suppresses stray vanilla ambient mobs inside the dedicated scenic area while explicitly preserving Fishing Game encounter fish. New chunks use the void biome to avoid introducing new ambient spawn ecology into the fishing-only map.
- Alpha.21 adds a second versioned Lakeside quality marker that repairs existing alpha.20 saves by replacing stray natural terrain/plant blocks inside the inner lake with water and removing those same debris classes above the surface. Wooden piers, posts, fences, lamps and other authored structures are deliberately excluded from cleanup.
- Gull Harbor: arrival promenade, layered shoreline rockwork, two breakwater arms with entrance beacons, three expanded fishing stations and a stronger lighthouse balcony/light silhouette.
- Deepwater Channel: three dedicated outward-facing fishing pods, hazard-guide stripes at each approach, rail-protected circulation space, submerged guide lights and a tall signal mast.
- Fishing edges remain open toward water so scenery does not fight the core interaction.
- Environment work is deterministic and server-authored; versioned quality markers upgrade existing worlds once rather than rebuilding every tick.
- Future environment revisions must use a new revision marker rather than silently relying on the original build marker.
- Third-party map candidates remain reference-only until redistribution rights are explicit; do not bundle unknown-license maps.

### Fishing hotspot rule

Alpha.16 makes authored geography affect fish hunting without turning the maps into hard-gated puzzle zones.

- The server resolves a hotspot from the bobber's actual water coordinates when the cast first becomes valid.
- Hotspots modify species selection weights only. Every species in the location remains possible from every valid water position.
- Preferred species receive a moderate weight boost and other species receive a small relative reduction, enough to make targeted hunting worthwhile without making one pier mandatory.
- Cheongram Lakeside: `서쪽 얕은 물`, `깊은 물골`, `바위 그늘`.
- Gull Harbor: `방파제 안쪽`, `항로 중앙`, `외해 끝부두`.
- Deepwater Channel: `유도등 수역`, `심해 골`, `고대 해구`.
- The first valid water contact reports the resolved hotspot and a short hint to the player.
- Undiscovered bestiary entries keep their species name hidden but show a recommended hotspot, giving the player a hunt direction without revealing the answer.
- Hotspot resolution is server-authoritative and isolated per fishing session, so multiplayer players can fish different water at the same time without sharing selection state.

## Species fight identity

Alpha.17 keeps the one-button reel system but stops different fish from feeling like the same fight with a different resistance number.

- Overall force/difficulty still comes from each species' existing resistance and the equipped rod's strength/control.
- Each species is assigned one server-owned fight style: `꾸준한 힘싸움`, `연속 질주`, `깊은 잠수`, `묵직한 버팀`, or `불규칙 난동`.
- Fight style changes burst cadence, burst duration, burst force and the visible fish's lateral/orbit/dive motion; it does not add another stamina bar, skill button, currency or equipment layer.
- The first hooked notice identifies the behavior in player-facing language so a player can immediately react without opening another screen.
- Fast fish make shorter, more frequent lateral runs; divers pull downward; bruisers create longer/heavier pressure windows; erratic fish move quickly and irregularly; steady fish favor slower readable pressure.
- The visible encounter fish uses the same style that drives server tension bursts, so animation/motion and real catch pressure do not contradict each other.
- Fight-style calculation is deterministic data keyed by species identity while burst timing remains per-session/randomized and isolated per player.

## Rod presentation rule

Alpha.18 makes existing rod progression visible without replacing the stable fishing transport, and alpha.19 carries that presentation into permanent progression.

- `minecraft:fishing_rod` remains the physical item and bobber/line carrier; rod tier is still server-owned profile progression rather than item-owned authority.
- The equipped stack receives a tier-specific `ITEM_MODEL`, and each client item dispatches between normal and cast art with the vanilla `minecraft:fishing_rod/cast` condition.
- `갈대 낚싯대`, `호수 전문가`, and `블루워터` share a coherent silhouette adapted from the MIT-licensed Fishing Frenzy Deluxe Fishing Rod instead of using placeholder geometry.
- Tier identity comes from material/palette treatment: natural reed/brass, graphite/freshwater teal, then deep-ocean blue/cyan.
- Bluewater adds Minecraft's animated enchantment glint so the highest current tier visibly reads as premium; lower tiers stay clean on the first progression cycle.
- Login restores the saved tier presentation, and profile upgrades refresh the equipped presentation immediately.
- After the first rebirth, every equipped rod keeps a glint and its item name includes the saved rebirth count. This makes permanent progression visible even when the current rod tier has reset to the starter tier.
- Future higher-grade rod art should keep using clearly licensed external bases or purpose-built final assets; do not ship placeholder rods merely to fill a prestige tier.

## Rebirth / long-term progression rule

Alpha.19 adds one simulator-style meta-progression layer without adding another currency, skill tree or screen.

- Rebirth requires the maximum current rod tier (`블루워터`), an empty catch bag, no active fishing and enough coins for the current target.
- First rebirth target: 10,000 C.
- Each completed rebirth increases the next target by 1,500 C.
- A successful rebirth returns the player to Cheongram Lakeside and resets coins, catch bag and rod tier.
- Species discoveries, catch counts, personal-best weight/length and the rebirth count are permanent and survive the reset.
- Ordinary fish sale income permanently gains +30% per rebirth. This multiplier applies to selling the catch bag, not to one-time discovery/location-completion rewards.
- Discovery and location-completion rewards never reset, so rebirth cannot repeatedly print collection rewards.
- The progression is intentionally accelerating: later cycles should reach the existing rod/location content faster, while the rising coin target keeps the reset from becoming immediate.
- The existing `B` catch-bag progression panel becomes the rebirth surface at maximum rod tier; a separate prestige menu is forbidden unless future gameplay needs enough choices to justify one.
- Client input only requests rebirth. The server validates all requirements, performs the location reset and commits persistent state.

## Collection / bestiary rule

The temporary catch bag and permanent collection records are separate systems.

- `B`: catch bag, selling, rod progression and rebirth.
- `J`: fish collection / bestiary.
- `M`: fishing-location travel.
- Selling clears only the catch bag. Species records survive and remain server-authoritative persistent player data.
- Rebirth also preserves species records, catch counts and personal-best values.
- Each discovered species records total catch count, personal-best weight and personal-best length.
- Undiscovered species hide their name in the collection screen.
- The collection screen shows per-location discovery progress as well as total discovery progress.
- Existing saves without a `records` or `rebirths` field remain loadable. Fish still present in an old catch bag seed their initial records on migration; catches sold before this record system existed cannot be reconstructed.

### Collection reward loop

Collection feeds the same coin economy instead of adding another currency or menu.

- The first catch of a species awards a one-time discovery bonus based on rarity: Common 12 C, Uncommon 20 C, Rare 35 C, Epic 65 C, Legendary 120 C.
- Completing every species record in a location awards one one-time completion bonus: Cheongram Lakeside 180 C, Gull Harbor 300 C, Deepwater Channel 500 C.
- Rewards are determined from the server-owned transition from previous species records to updated records. Re-catching a known species or revisiting a completed location never repeats the reward.
- Legacy save migration never retroactively prints coins. Existing records and migrated bag records are treated as already discovered.
- Reward coins use the existing coin economy and intentionally remain smaller than the long-term value of ordinary selling, so collection supplements rather than replaces fishing/selling.
- Rebirth sale multipliers do not multiply discovery/completion bonuses.
- HUD, bestiary and travel UI show location collection progress and completion reward information without exposing hidden species names.

## Catch quality / record feedback

Every catch is graded from its configured species weight/length ranges: `일반`, `대형`, `트로피`, `괴물급`. The catch-result card shows rarity, size grade, weight, length and base sale value, and calls out first discoveries, collection rewards, location completion or new personal records. The catch bag shows the actual boosted Sell All total after rebirth. Presentation is derived from server-authoritative profile snapshots.

## UI / HUD rule

Do not invent the visual language ad hoc. HUD, cast meter, bag, bestiary and travel screens reuse the Kenney CC0 UI language already bundled with the project. External assets and licenses are tracked in `THIRD_PARTY_ASSETS.md`.

- The bundled Kenney grey panel is a 100x100 framed surface whose visible frame is the outer 4 source pixels. Nine-slice composition must preserve that source frame instead of sampling an arbitrary thicker border.
- Screen dimensions are Minecraft logical GUI units, not physical window pixels. Major panels must derive from `guiWidth`/`guiHeight` and fit inside explicit margins; fixed sizes larger than the active logical GUI are forbidden.
- Current preferred logical sizes are HUD 174x62, catch bag 340x220, bestiary 348x220 and travel 320x206, with smaller-window clamping.
- Light Kenney surfaces use a dark panel-text palette; world-space cast/fight/notice overlays use a separate bright palette so one color scheme is not forced onto opposite backgrounds.
- Buttons use the same verified Kenney panel skin and explicit accent/hover/disabled states. A missing/broken texture must never be allowed to render as Minecraft's magenta/black fallback.
- The vanilla hotbar and held-item tooltip stay visible while the game relies on actual inventory slots. If a future fishing-specific inventory replaces them, the replacement must cover the complete item-selection/inventory interaction rather than hiding only the hotbar.
- Cast and reel overlays reserve the bottom hotbar region and render above it.
- Bag, bestiary and travel screens share title/subtitle hierarchy, separators, section labels, text colors and disabled-state treatment.
- Unaffordable rod upgrades are visibly disabled client-side while the server remains the authority for the actual purchase.
- At maximum rod tier, the existing progression button switches to `환생하기` and the same rod panel shows rebirth count, permanent sale multiplier, next cost and whether the bag must be sold first.
- Catch bag rows expose rarity, size grade, weight, length and base value without creating more menus; the top sale total includes the rebirth multiplier.
- Location collection counts stay visible in the HUD/travel view and collection-completion targets stay inside the bestiary rather than creating a quest screen.
- Hotspot hunt hints stay in existing notices/bestiary rows instead of adding another map or hunting menu.
- UI changes must still be judged in a real Minecraft client at supported GUI scales; code/build success does not certify screen composition.

## Player placement / tracking lifecycle rule

- Initial placement into the dedicated Lakeside dimension must not cross-dimension teleport directly inside `ServerPlayConnectionEvents.JOIN`; the player may still be completing vanilla chunk-tracker registration at that point.
- Alpha.21 queues initial placement for a later server tick, clears pending placement on disconnect, skips removed players, and avoids re-teleporting a player whose saved position is already valid in Lakeside.
- Explicit travel and rebirth are different: those actions intentionally move the player and continue to use the normal server-authoritative travel path.
- The alpha.20 integrated-server shutdown `DistanceManager.removePlayer` NPE is not considered proven fixed until a real client join -> play -> exit reproducer is clean. Automated dedicated-server startup alone cannot certify this lifecycle regression.

## Audio feedback rule

- Existing bobber splash remains the bite cue; ordinary catches retain the existing catch sound.
- Full cast charge gets one light confirmation cue rather than repeated charging noise.
- Large tension jumps during the hooked fight can trigger a restrained fish-pull cue with a client cooldown.
- Trophy/rare catches receive an extra reward layer; first discoveries, personal records, monster-size catches, legendary fish and location completion receive a stronger confirmation layer.
- Successful selling, rod upgrades and rebirth each have distinct confirmation cues.
- Added cues are cosmetic client feedback only. They do not own catch state, records, currency, progression, rebirth or purchase success.

## Catch presentation direction

Fishing encounters use nine transient morphology families — small, tall, fat, long, angler, cyprinid, pelagic, bream and catfish — with species-specific textures. The added families deliberately separate carp/crucian depth, salmonid/tuna streamlining, sea-bream compression and catfish head/whisker identity instead of routing most of the catalog through one generic fat model. They are session-only entities: unsaved, no AI, no loot, cleaned up on the fishing lifecycle. Visible approach and burst motion must agree with server-side bite/tension behavior.

## Fishing-hook ownership rule

Vanilla fishing hook is only line/bobber transport and visual anchor in all dedicated Fishing Game dimensions. Fishing Game owns charge/release timing, hotspot resolution, species, bite timing, reel fight, catch result and economy. Vanilla's independent lure/nibble/bite cycle is suppressed there.

## User-test gate

Do not hand the user a JAR for a tiny technical check. A user-facing test build must have:
- dedicated non-survival HUD that fits the actual logical GUI without crowding the playfield
- a usable hotbar/inventory affordance unless a complete fishing-specific replacement has been implemented
- readable charge casting with meaningful distance response
- catch bag, persistence, coins and selling
- meaningful rod progression with held-item visuals that match the saved tier
- visible species presentation and readable reel control
- species fights whose visible motion and tension cadence are meaningfully different
- clear catch result with rarity, size grade, records and collection reward feedback
- no competing vanilla bite/loot presentation
- multiple dedicated fishing locations connected to progression
- permanent collection/personal-best records that survive selling and rebirth
- collection goals that feed the existing economy without another chore currency
- map sub-areas that meaningfully affect target hunting without hard-gating species
- server-authoritative Bluewater -> rebirth -> starter reset -> faster selling loop with no duplicate collection rewards
- complete cast -> target water -> catch -> sell -> upgrade -> travel -> collect -> rebirth loop
- acceptable actual Minecraft screen and world quality

Alpha.20 passed automated build/server checks but failed the next real graphical review: major screens were oversized at the user's GUI scale, the hotbar removal was incoherent with the retained inventory, natural terrain debris remained suspended in the lake, and integrated-server shutdown logged a player/chunk-tracking NPE. Alpha.21 is the corrective slice. It must pass automated CI and then be re-tested in a real client for UI scale, lake cleanup, rod hold/release animation, encounter-fish morphology and join/exit shutdown behavior before PLAYTESTED, GRAPHICAL CLIENT REVIEWED or shutdown-regression-verified status is claimed.
