# TITANBREAK 0.1.0-alpha.53 Implementation Notes

## Purpose

Alpha.53 completes the first structural visual-remodel pass that was left uncommitted when the previous development chat ended. This batch is intentionally presentation-only: it preserves gameplay, renderer and animation contracts while replacing remaining weak silhouettes and retiring the alpha.52-only visual CI.

## Geometry batch

- B02 `gravemarch_colossus.geo.json`: heavier power/berserker silhouette; reinforced upper body and joints, dorsal impact armor and a more readable shock-heart.
- B03 `bastion_walker.geo.json`: fortified quadruped silhouette; four supports/eight plates retained; upper defense-node/power-core route reinforced with asymmetric turret masses, ram and buttresses.
- B07 `storm_leviathan.geo.json`: more organic wandering-body silhouette; four wing membranes and six electric sacs retained, with sensor crest, dorsal charge spine and tail control surfaces.
- B08 `ash_titan.geo.json`: thermal guardian silhouette; six cooling plates/radiation arms retained, with stronger radiant-heart frame, heat vents and head-sensor cowl.
- `bulwark.geo.json`: front-dominant moving rampart.
- `howler.geo.json`: oversized acoustic head/horn, throat bellows and resonance rings.

Existing animation-referenced bones are retained. New bones are presentation structures, not new gameplay state.

## Verification changes

Added `tools/verify_visual_assets.py`:
- parses all GeckoLib entity geometry;
- rejects duplicate bones, invalid/missing parents, cycles and malformed cube vectors;
- checks matching animation bone references;
- checks ordinary model-to-texture stem mapping;
- protects the six alpha.53 remodels with persistent signature-bone and cube-count contracts.

The renderer-specific `hollow_colossus` mapping remains an explicit verifier exception.

The stale `.github/workflows/titanbreak-alpha52-visual-ci.yml` is removed and replaced with `.github/workflows/titanbreak-visual-regression-ci.yml`. The new gate is version-tolerant, runs the verifier, performs a Java 25 clean build, validates the runtime JAR name from `gradle.properties`, and uploads the built JAR.

## Version

`mod_version=0.1.0-alpha.53`

Build target remains the values in the current Gradle properties:
- Minecraft 26.2
- NeoForge 26.2.0.38-beta
- GeckoLib 5.5.3
- Java 25

## Manual test focus after CI

1. B02 foot/head weakpoint readability and attack animation pivots.
2. B03 upper defense-node/power-core route, turret visibility and four-leg ground contact.
3. B07 body/fin/tail clipping during aerial motion.
4. B08 chest/core visibility, vent silhouette and melee arm pivots.
5. Bulwark front-wall collision/readability at normal combat range.
6. Howler mouth/resonator readability during its sonic attack.
7. Culling, scale, texture stretching and multipart alignment for all six.
