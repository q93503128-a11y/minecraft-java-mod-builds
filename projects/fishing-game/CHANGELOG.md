# Changelog

## 0.1.0-alpha.17

- Added five species fight styles so hooked fish no longer differ only by a resistance scalar: `꾸준한 힘싸움`, `연속 질주`, `깊은 잠수`, `묵직한 버팀`, `불규칙 난동`.
- Kept the existing one-button hold/release reel control; fight styles change burst cadence, duration, force and visible movement without adding another meter, skill or currency.
- Coupled server-side tension bursts and encounter-fish movement to the same fight style so the visible run/dive behavior matches the real pressure applied to the line.
- Added distinct lateral/orbit/dive motion profiles for fast, diving, heavy and erratic fish while preserving rod strength/control as the progression counterplay.
- Added a short player-facing fight-style cue at the hook moment so the player can react immediately without opening a menu.
- Added deterministic fight-style tests covering cadence, strength/duration, dive bias and representative species assignments.

## 0.1.0-alpha.16

- Added three server-resolved fishing hotspots to each dedicated location so map sub-areas affect target hunting instead of acting as scenery only.
- Hotspots are resolved from the bobber's actual water coordinates when a cast becomes valid.
- Hotspots bias species selection weights rather than hard-gating species; every species in the current location remains possible from every valid fishing spot.
- Added Cheongram Lakeside hotspots: `서쪽 얕은 물`, `깊은 물골`, `바위 그늘`.
- Added Gull Harbor hotspots: `방파제 안쪽`, `항로 중앙`, `외해 끝부두`.
- Added Deepwater Channel hotspots: `유도등 수역`, `심해 골`, `고대 해구`.
- Added a short hotspot/ecology notice when the bobber first reaches valid water.
- Added recommended-hotspot hints to undiscovered bestiary rows while keeping undiscovered species names hidden.
- Added deterministic hotspot classification/bias tests, including an assertion that non-preferred species remain catchable.

## 0.1.0-alpha.15

- Added one-time first-discovery coin rewards that scale with existing fish rarity instead of introducing another currency.
- Added one-time location collection-completion rewards for Cheongram Lakeside, Gull Harbor and Deepwater Channel.
- Made collection rewards server-authoritative by deriving them from the transition between previous and updated permanent species records.
- Prevented repeat catches, selling, reconnecting and legacy-save migration from duplicating discovery/completion rewards.
- Added collection reward and location-completion callouts to catch-result feedback.
- Added current-location collection progress to the HUD and collection progress/completion targets to bestiary and travel screens without revealing undiscovered species names.
- Added deterministic profile tests for first-discovery rewards, no duplicate rewards, one-time location completion and legacy migration behavior.

## 0.1.0-alpha.14

- Unified HUD, catch bag, bestiary and travel screens behind one Kenney-based Fishing Game UI theme instead of maintaining separate ad-hoc color/layout rules.
- Widened the always-on fishing HUD so coin, bag, rod, location and B/J/M navigation no longer compete inside a 100px panel.
- Reworked bag information hierarchy into summary, catch list and rod-progression sections; unaffordable rod upgrades are now visibly disabled instead of inviting a failed request.
- Added size-grade visibility to bag rows and consistent rarity/record colors shared with catch-result and bestiary presentation.
- Added lightweight audio cues for full cast charge, strong hooked-fish pulls, trophy/rare catches, first discoveries, personal records, selling and rod upgrades while preserving the existing bite/catch sounds for ordinary events.
- Kept all new audio presentation client-side/cosmetic; species, catch result, records, money and purchases remain server-authoritative.
- Added CI JAR inspection for the shared UI theme and client audio classes.

## 0.1.0-alpha.13

- Replaced immediate vanilla-speed casting with hold-and-release charge casting.
- Added a compact Kenney-language cast meter; full charge is reached after 18 ticks and overcharging gives no extra distance.
- Made cast duration server-authoritative: the initial rod use starts a server charge and the client sends only release/cancel transitions.
- Scaled the vanilla fishing-hook launch velocity from the validated charge while keeping species rarity, fish size, value and luck completely independent from cast power.
- Preserved the same right-click hold/release input for line-tension control once a fish is actually hooked.
- Added deterministic charge normalization / throw-speed tests and CI JAR inspection for the new cast payload and math helper.

## 0.1.0-alpha.12

- Added a versioned environment-quality pass so existing authored fishing worlds can receive later visual upgrades without deleting saves or rebuilding every tick.
- Upgraded Gull Harbor with an arrival promenade, shoreline rockwork, breakwater arms, harbor-entry beacons and three expanded fishing stations.
- Strengthened the lighthouse silhouette with a wider balcony and multi-light crown.
- Upgraded Deepwater Channel with three outward-facing fishing pods, protected circulation rails, hazard guide stripes, submerged guide lights and a signal mast.
- Kept fishing-facing platform edges open so scenery improves navigation without obstructing casting.
- Added alpha.12 environment builder inspection to the Fishing Game CI artifact gate.

## 0.1.0-alpha.11

- Added catch-size grades (`일반`, `대형`, `트로피`, `괴물급`) derived from each species' configured weight/length range.
- Expanded the catch-result card inside the existing Kenney CC0 UI language so rarity, size grade, weight, length and sale value are readable together.
- Added immediate first-discovery feedback for newly registered species.
- Added immediate personal-best feedback when a catch beats prior weight and/or length records.
- Kept all rewards and records authoritative on the server; the client derives presentation only from synchronized profile state.
- Added deterministic size-grade tests and JAR inspection for the new presentation classes.

## 0.1.0-alpha.10

- Separated the temporary catch bag from permanent species records.
- Added persistent per-species caught count, best weight and best length.
- Added `J` bestiary screen with hidden names for undiscovered species and per-location collection progress.
- Selling fish now clears only the bag while preserving collection history.
- Added legacy-save migration behavior that seeds records from fish still present in an old catch bag.
- Added profile tests covering records, personal bests, selling and migration.

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
