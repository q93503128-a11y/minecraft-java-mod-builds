# Changelog

## 0.1.0-alpha.21

- Reworked Fishing Game UI sizing around Minecraft's logical GUI dimensions instead of physical window pixels. HUD, catch bag, bestiary and travel panels now clamp to `guiWidth`/`guiHeight`, preventing 400x300/500x300 logical panels from consuming or clipping a scale-2 client window.
- Corrected the bundled Kenney CC0 panel slicing after inspecting the actual 100x100 asset: its visible frame occupies the outer 4 source pixels, not the 12 pixels used by alpha.20. This restores the intended lighter border weight without replacing the external UI language.
- Compacted the HUD to a preferred 174x62 logical surface, catch bag to 340x220, bestiary to 348x220 and travel to 320x206, with smaller buttons and denser information hierarchy.
- Restored Minecraft's hotbar and held-item tooltip because Fishing Game still uses real inventory slots. Survival-only status layers remain hidden, and cast/reel overlays were moved upward so they do not collide with the restored hotbar.
- Added deterministic UI-layout tests covering the observed 428x259 logical client size and smaller-window clamping so oversized fixed panels cannot silently return.
- Added a second one-time Cheongram Lakeside quality marker that removes stray natural terrain blocks left floating in or above the authored lake while preserving intended wooden piers, supports and fishing structures.
- Hardened initial player placement after the integrated-server shutdown error: cross-dimension placement is deferred out of the Fabric JOIN callback, valid players already in Lakeside are not redundantly teleported, pending placement is cleared on disconnect, and removed players are excluded from placement/bounds correction.
- The shutdown `DistanceManager.removePlayer` NPE is treated as a targeted lifecycle/tracking mitigation, not as fully reproduced or proven fixed; a real client join/exit test is still required.

## 0.1.0-alpha.20

- Repaired the Kenney UI composition after graphical play review exposed repeated full-panel tiles, visible seams and unreadable light-on-light text.
- Replaced the old 100x100 full-panel repetition with one reusable nine-slice panel renderer, so HUD, catch-result, bag, bestiary and travel surfaces keep Kenney corners/borders without multiplying whole panels.
- Split light-panel typography from world-overlay typography: menu/card text now uses a dark readable palette while cast/fight/notice text stays bright against the 3D world.
- Removed the broken blue-button texture path from runtime rendering after it appeared as Minecraft's magenta/black missing-texture surface; travel/progression buttons now use the same verified Kenney panel skin with explicit hover/disabled treatment.
- Re-authored Cheongram Lakeside's surrounding presentation as an inland-lake basin instead of exposing the flat-ocean generator outside the original thin shoreline ring.
- Added a sloped authored lake bed, continuous exterior terrain, raised scenic ridge, grounded conifer belt and rockwork to hide the infinite-water horizon from the normal playable area.
- Switched newly generated Lakeside chunks to the void biome and added targeted ambient-mob cleanup inside the dedicated fishing scenery while explicitly preserving Fishing Game encounter fish.
- Added an alpha.20 world-quality marker so existing alpha.19 saves receive the Lakeside presentation repair once without deleting progression or rebuilding the terrain every tick.

## 0.1.0-alpha.19

- Added the first simulator-style rebirth loop without adding a second currency or another management menu.
- Rebirth becomes available only after reaching `블루워터`, emptying the catch bag and holding the current rebirth cost; the first cost is 10,000 C and later costs rise by 1,500 C per rebirth.
- Rebirth returns the player to Cheongram Lakeside, resets coins, catch bag and rod tier, but permanently preserves the bestiary, catch counts and personal-best records.
- Each rebirth permanently increases ordinary fish sale income by 30%, so later progression cycles become meaningfully faster while first-discovery/location-completion bonuses remain one-time collection rewards.
- Kept rebirth server-authoritative: the client only requests it, while the server validates rod tier, active fishing state, bag state, coins and the location reset.
- Reused the existing catch-bag progression panel for rebirth instead of creating another screen; the panel shows rebirth count, permanent sale multiplier, next cost and readiness state.
- Added a distinct rebirth confirmation sound and persistent rod presentation: post-rebirth rods keep a glint and carry the rebirth count in the equipped item name even after the rod tier resets.
- Added backward-compatible persistent `rebirths` profile data, synchronized it to clients, and added deterministic rebirth eligibility/sale-growth tests.

## 0.1.0-alpha.18

- Added visible three-tier rod progression without replacing vanilla fishing-hook transport: each saved rod tier now selects a dedicated client item model while gameplay continues to use `minecraft:fishing_rod` as the carrier.
- Adapted the MIT-licensed Fishing Frenzy `Deluxe Fishing Rod` art into three coherent material variants for `갈대 낚싯대`, `호수 전문가`, and `블루워터`, with matching cast-state textures.
- Added a restrained animated glint to Bluewater so the highest current tier reads as a simulator-style premium upgrade without inventing a placeholder fantasy model.
- Synchronized the equipped rod presentation on login and profile upgrades, so visual tier and server-authoritative progression cannot drift during normal play.
- Added explicit 26.2 client-item fishing-rod cast dispatch resources and CI JAR checks for rod model definitions, textures and bundled MIT notice.
- Recorded the exact Fishing Frenzy source commit and upstream texture blob hashes in `THIRD_PARTY_ASSETS.md`.

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
- Hotspots bias species selection weights rather than hard-gating species; every species in the location remains possible from every valid fishing spot.
- Added Cheongram Lakeside hotspots: `서쪽 얕은 물`, `깊은 물골`, `바위 그늘`.
- Added Gull Harbor hotspots: `방파제 안쪽`, `항로 중앙`, `외해 끝부두`.
- Added Deepwater Channel hotspots: `유도등 수역`, `심해 골`, `고대 해구`.
- Added a short hotspot/ecology notice when the bobber first reaches valid water.
- Added recommended-hotspot hints to undiscovered bestiary rows while keeping the species name hidden.
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
